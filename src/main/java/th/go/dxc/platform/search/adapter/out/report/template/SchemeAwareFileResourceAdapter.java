package th.go.dxc.platform.search.adapter.out.report.template;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import th.go.dxc.platform.search.application.report.port.out.template.FileResourcePort;
import th.go.dxc.platform.search.config.ReportProperties;

@Slf4j
@Component
public class SchemeAwareFileResourceAdapter implements FileResourcePort {

  private final ReportProperties props;
  private final WebClient gitlabWebClient;

  public SchemeAwareFileResourceAdapter(ReportProperties props, WebClient gitlabWebClient) {
    this.props = Objects.requireNonNull(props);
    this.gitlabWebClient = Objects.requireNonNull(gitlabWebClient);
  }

  @Override
  public Mono<InputStream> getInputStream(String location) {
    if (location == null || location.isBlank())
      return Mono.empty();

    if (location.startsWith("data:")) {
      log.debug("getInputStream from data:");
      // already inlined
      byte[] bytes = location.getBytes(StandardCharsets.UTF_8);
      return Mono.just(new ByteArrayInputStream(bytes));
    }
    if (location.startsWith("classpath:")) {
      String cp = location.substring("classpath:".length()).replaceFirst("^/+", "");
      log.trace("Getting Classpath Resource from {}", cp);
      var res = new ClassPathResource(cp);
      return Mono.fromCallable(() -> (InputStream) res.getInputStream())
          .subscribeOn(Schedulers.boundedElastic());
    }
    if (location.startsWith("file:")) {
      log.debug("getInputStream from file:");
      Path p = Path.of(location.substring("file:".length()));
      return Mono.fromCallable(() -> (InputStream) new FileInputStream(p.toFile()))
          .subscribeOn(Schedulers.boundedElastic());
    }
    if (location.startsWith("http://") || location.startsWith("https://")) {
      return httpToStream(location);
    }
    if (location.startsWith("gitlab:")) {
      log.debug("getInputStream from gitlab:");
      return gitlabToStream(location);
    }

    // No scheme: treat as classpath relative to datasetBasePath (for backward
    // compat)
    String base = props.template().location();
    String cp = (base == null || base.isBlank()) ? location : base + "/" + location;
    var res = new ClassPathResource(cp);
    return Mono.fromCallable(() -> (InputStream) res.getInputStream())
        .subscribeOn(Schedulers.boundedElastic());
  }

  // --- helpers ---

  private Mono<InputStream> httpToStream(String url) {
    return gitlabWebClient.get()
        .uri(url)
        .retrieve()
        .bodyToMono(byte[].class)
        .map(ByteArrayInputStream::new);
  }

  /**
   * Supports:
   * gitlab:/path/inside/repo.txt -> uses defaultRef
   * gitlab://feature-branch/path/inside/repo.txt -> uses explicit ref
   */
  private Mono<InputStream> gitlabToStream(String location) {
    log.debug("gitlab resource: {}", location);
    // String basePath = props.template().basePath();
    var g = props.template().gitlab();
    // String base = rstrip(g.baseRawUrl(), "/");
    // String defaultRef = rstrip(g.ref(), "/");

    // final Pattern p = Pattern.compile("^gitlab:(?://([^/]+))?(.*)$");
    // Matcher m = p.matcher(location);
    // if (!m.find())
    // return Mono.error(new IllegalArgumentException("Invalid gitlab: location: " +
    // location));

    // String ref = (m.group(1) != null && !m.group(1).isBlank()) ? m.group(1) :
    // defaultRef;
    // String repoPath = m.group(2);
    // log.debug("repoPath = {}", repoPath);
    // String encodedRepoPath = URLEncoder.encode(repoPath, StandardCharsets.UTF_8);
    // String url = base +"/"+ encodedRepoPath+ "/raw?ref="+g.ref();

    String base = g.baseUrl();
    String apiVersion = g.apiVersion();
    int projectId = g.projectId();
    String filePath = location.replace("gitlab:", "");
    String ref = g.ref();
    log.debug("filePath={}", filePath);
    // encode ONLY the file path
    String encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8).replace("+", "%20");
    log.debug("encodedPath={}", encodedPath);
    String url = String.format(
        "%s/api/%s/projects/%d/repository/files/%s/raw?ref=%s",
        base, apiVersion, projectId, encodedPath, ref);
    log.debug("uri: {}", url);
    URI uri = URI.create(url);
    WebClient.RequestHeadersSpec<?> req = gitlabWebClient.get().uri(uri);
    if (g.useDeployToken()) {
      log.debug("use Deploy Token: username={}, password={}", g.deployToken().username(),
          "*".repeat(g.deployToken().password().length()));
      // req = req.headers(h -> h.setBasicAuth(g.deployToken().username(),
      // g.deployToken().password()));
      req = req.header("Deploy-Token", g.deployToken().password());
    } else if (g.usePat()) {
      log.debug("use Private Token: header={}, token={}", g.tokenHeader(), "*".repeat(g.token().length()));
      // req = req.headers(h -> h.setBearerAuth(g.token()));
      req = req.headers(h -> h.add(g.tokenHeader(), g.token()));
    }
    return req.retrieve()
        .bodyToMono(byte[].class)
        .map(ByteArrayInputStream::new);
  }

  // private static String rstrip(String s, String suffix) {
  //   return (s != null && s.endsWith(suffix)) ? s.substring(0, s.length() - suffix.length()) : s;
  // }
}
