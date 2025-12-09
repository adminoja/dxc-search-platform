package th.go.dxc.platform.search.application.report.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import th.go.dxc.platform.search.application.report.port.out.template.FileResourcePort;
import th.go.dxc.platform.search.config.ReportProperties;

@Component
public class TemplateIO {

  private final FileResourcePort files;
  private final ReportProperties props;

  // Optional: baseDir for any local file scheme (used by FileResourcePort impl)
  private final Path baseDir;

  public TemplateIO(FileResourcePort files, ReportProperties props) {
    this.files = Objects.requireNonNull(files, "files");
    this.props = Objects.requireNonNull(props, "props");

    String loc = props.template().location();
    if (loc != null && !loc.isBlank()) {
      String trimmed = loc.trim();

      // If it starts with a known non-file scheme, don't try to make it a Path
      if (trimmed.startsWith("classpath:") || trimmed.startsWith("gitlab:")) {
        this.baseDir = null; // baseDir only used for file: scheme
      } else if (!trimmed.contains(":")) {
        // plain filesystem path like "report/template/pdf"
        this.baseDir = Paths.get(trimmed).toAbsolutePath().normalize();
      } else if (trimmed.startsWith("file:")) {
        // optional: support "file:/some/dir" in config
        // (on Windows you may want URI → Path)
        this.baseDir = Paths.get(java.net.URI.create(trimmed)).toAbsolutePath().normalize();
      } else {
        // Unknown scheme => do not use as baseDir
        this.baseDir = null;
      }
    } else {
      this.baseDir = null;
    }
  }

  public Mono<String> readText(String location) {
    String safeLocation = sanitizeLocation(location);
    return files.getInputStream(safeLocation)
        .flatMap(is -> Mono.fromCallable(() -> readAllUtf8(is))
            .subscribeOn(Schedulers.boundedElastic()));
  }

  /**
   * e.g.,
   * dataUri("gitlab:/report/templates/pdf/_common/img/logo.svg","image/svg+xml")
   */
  public Mono<String> dataUri(String location, String mime) {
    if (location == null || location.isBlank())
      return Mono.empty();
    if (location.startsWith("data:"))
      return Mono.just(location);
    String safeLocation = sanitizeLocation(location);
    return files.getInputStream(safeLocation)
        .flatMap(is -> Mono.fromCallable(is::readAllBytes).subscribeOn(Schedulers.boundedElastic()))
        .map(bytes -> "data:%s;base64,%s".formatted(mime, Base64.getEncoder().encodeToString(bytes)));
  }

  /** Shortcut to build dataset-relative locations: */
  public String loc(String datasetId, String relative) {
    String safeDatasetId = sanitizeSegment(datasetId, "datasetId");
    String safeRelative = sanitizeRelativePath(relative, "relative");

    String rawBase = props.template().location();
    String basePath = stripScheme(rawBase);

    String path = (basePath == null || basePath.isBlank())
        ? safeDatasetId + "/" + safeRelative
        : basePath + "/" + safeDatasetId + "/" + safeRelative;

    String location = "classpath:" + path;
    return sanitizeLocation(location);
  }

  public String readTextSync(String location) {
    return readTextSync(location, Duration.ofSeconds(5));
  }

  public String readTextSync(String location, Duration timeout) {
    return readText(location).blockOptional(timeout).orElse("");
  }

  // private String dataUriSync(String location, String mime) {
  // return io.dataUri(location, mime).blockOptional(IO_TIMEOUT).orElse("");
  // }
  public String dataUriOrEmpty(String location, String mime) {
    return dataUriOrEmpty(location, mime, Duration.ofSeconds(5));
  }

  public String dataUriOrEmpty(String location, String mime, Duration timeout) {
    if (location == null || location.isBlank())
      return "";
    return dataUri(location, mime).blockOptional(timeout).orElse("");
  }
  // ---- helpers -----------------------------------------------------------

  private String readAllUtf8(InputStream is) throws Exception {
    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
  }

  /**
   * High-level sanitizer for resource location strings.
   * Supports schemes like classpath:, gitlab:, file: (if you use them).
   */
  private String sanitizeLocation(String location) {
    if (location == null || location.isBlank()) {
      throw new IllegalArgumentException("location must not be blank");
    }
    String trimmed = location.trim();

    int idx = trimmed.indexOf(':');
    if (idx <= 0) {
      throw new IllegalArgumentException("location must have scheme, e.g. 'classpath:...'");
    }
    String scheme = trimmed.substring(0, idx);
    String pathPart = trimmed.substring(idx + 1);

    pathPart = pathPart.replace('\\', '/');

    if (pathPart.contains("..")) {
      throw new IllegalArgumentException("Path traversal ('..') not allowed in template location");
    }

    switch (scheme) {
      case "classpath":
        // enforce relative for classpath
        if (pathPart.startsWith("/")) {
          pathPart = pathPart.substring(1);
        }
        return "classpath:" + pathPart;

      case "gitlab":
        // keep leading '/' if caller provided it (no need to strip)
        return "gitlab:" + pathPart;

      case "file":
        // you may or may not want to strip leading '/' here, depending on how you use
        // baseDir
        if (baseDir != null) {
          Path candidate = baseDir.resolve(pathPart).normalize();
          if (!candidate.startsWith(baseDir)) {
            throw new IllegalArgumentException("Template file escapes base directory");
          }
          return "file:" + candidate.toString();
        } else {
          Path candidate = Paths.get(pathPart).normalize();
          if (candidate.isAbsolute()) {
            throw new IllegalArgumentException("Absolute file paths not allowed for templates");
          }
          return "file:" + candidate.toString();
        }

      default:
        throw new IllegalArgumentException("Unsupported template location scheme: " + scheme);
    }
  }

  private String sanitizeSegment(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    String v = value.trim();
    // Only allow a conservative set of characters, no slashes
    if (!v.matches("^[A-Za-z0-9._-]{1,100}$")) {
      throw new IllegalArgumentException("Invalid " + label + " for template path");
    }
    return v;
  }

  private String sanitizeRelativePath(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    String v = value.trim().replace('\\', '/');
    if (v.contains("..")) {
      throw new IllegalArgumentException("Path traversal ('..') not allowed in " + label);
    }
    // allow subdirectories but no weird chars
    if (!v.matches("^[A-Za-z0-9._/ -]{1,200}$")) {
      throw new IllegalArgumentException("Invalid " + label + " for template path");
    }
    return v;
  }

  private String stripScheme(String loc) {
    if (loc == null)
      return null;
    String trimmed = loc.trim();
    int idx = trimmed.indexOf(':');
    if (idx <= 0)
      return trimmed; // no scheme
    String scheme = trimmed.substring(0, idx);
    if ("classpath".equals(scheme) || "file".equals(scheme) || "gitlab".equals(scheme)) {
      return trimmed.substring(idx + 1); // drop 'classpath:'
    }
    return trimmed; // unknown scheme, return as-is
  }

}
