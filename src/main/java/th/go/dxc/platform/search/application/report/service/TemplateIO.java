package th.go.dxc.platform.search.application.report.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import th.go.dxc.platform.search.application.report.port.out.template.FileResourcePort;
import th.go.dxc.platform.search.config.ReportProperties;

@Component
public class TemplateIO {

  private final FileResourcePort files;
  private final ReportProperties props;

  public TemplateIO(FileResourcePort files, ReportProperties props) {
    this.files = files;
    this.props = props;
  }

  public Mono<String> readText(String location) {
    return files.getInputStream(location)
        .flatMap(is -> Mono.fromCallable(() -> new String(is.readAllBytes(), StandardCharsets.UTF_8))
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
    return files.getInputStream(location)
        .flatMap(is -> Mono.fromCallable(is::readAllBytes).subscribeOn(Schedulers.boundedElastic()))
        .map(bytes -> "data:%s;base64,%s".formatted(mime, Base64.getEncoder().encodeToString(bytes)));
  }

  /** Shortcut to build dataset-relative locations: */
  public String loc(String datasetId, String relative) {
    String base = props.template().location();
    String path = (base == null || base.isBlank()) ? datasetId + "/" + relative
        : base + "/" + datasetId + "/" + relative;
    return "classpath:" + path; // default to classpath; caller can still pass gitlab:/... explicitly
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
}
