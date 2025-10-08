package th.go.dxc.platform.search.adapter.out.report.pdf;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.report.port.out.PdfRendererPort;
import th.go.dxc.platform.search.config.ReportProperties;

@Component
public class GotenbergPdfRenderer implements PdfRendererPort {
  private final WebClient client;
  private final ReportProperties props;

  public GotenbergPdfRenderer(WebClient gotenbergClient, ReportProperties props) {
    this.client = gotenbergClient;
    this.props = props;
  }

  @Override
  public Mono<byte[]> render(Template template, Map<String,Object> model) {
    // model is already baked into HTML by service; this adapter just posts bytes.
    byte[] indexBytes = template.indexHtml().getBytes(StandardCharsets.UTF_8);
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("files", asPart(indexBytes, "index.html", "text/html"));
    if (template.headerHtml() != null) form.add("files", asPart(template.headerHtml().getBytes(StandardCharsets.UTF_8), "header.html", "text/html"));
    if (template.footerHtml() != null) form.add("files", asPart(template.footerHtml().getBytes(StandardCharsets.UTF_8), "footer.html", "text/html"));

    return client.post()
        .uri(props.getGotenberg().endpoint())
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .bodyValue(form)
        .retrieve()
        .bodyToMono(byte[].class)
        .timeout(Duration.ofMillis(props.getGotenberg().timeoutMs()));
  }

  private org.springframework.http.HttpEntity<ByteArrayResource> asPart(byte[] data, String filename, String contentType) {
    var resource = new ByteArrayResource(data) { @Override public String getFilename() { return filename; } };
    var headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(contentType));
    return new org.springframework.http.HttpEntity<>(resource, headers);
  }
}
