package th.go.dxc.platform.search.adapter.out.report.pdf;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.report.port.out.pdf.PdfRendererPort;
import th.go.dxc.platform.search.config.ReportProperties;

public class GotenbergPdfRendererAdapter implements PdfRendererPort {

  private final WebClient client;
  private final String endpoint;
  private final Duration timeout;

  public GotenbergPdfRendererAdapter(ReportProperties props) {
    var g = props.gotenberg();
    this.client = WebClient.builder()
        .baseUrl(g.baseUrl())
        .defaultHeader(HttpHeaders.USER_AGENT, "dxc-report/1.0")
        .filter(logOnError()) // small helper below
        .build();
    this.endpoint = g.endpoint();               // e.g. /forms/chromium/convert/html
    this.timeout = g.timeout();
  }

  @Override
  public byte[] fromHtml(byte[] htmlUtf8) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

    // Required main document named "index.html"
    parts.add("files", filePart("index.html", htmlUtf8, MediaType.TEXT_HTML));

    // Sensible A4 defaults (you can expose these via properties later if needed)
    parts.add("paperWidth", "8.27");    // A4 width (inches)
    parts.add("paperHeight", "11.69");  // A4 height (inches)
    parts.add("marginTop", "0.5");
    parts.add("marginBottom", "0.5");
    parts.add("marginLeft", "0.5");
    parts.add("marginRight", "0.5");
    // parts.add("preferCssPageSize", "true");   // if you prefer @page CSS over numbers

    return client.post()
        .uri(endpoint)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .retrieve()
        .toEntity(byte[].class)
        .timeout(timeout)
        .map(this::requirePdfBody)
        .block(timeout);
  }

  // -------- Optional convenience: header/footer variant (not part of the port) --------
  /** Render with optional header/footer HTML. Safe to use if you call this class directly. */
  public byte[] fromHtml(byte[] htmlUtf8, byte[] headerHtmlUtf8, byte[] footerHtmlUtf8) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

    parts.add("files", filePart("index.html", htmlUtf8, MediaType.TEXT_HTML));
    if (headerHtmlUtf8 != null && headerHtmlUtf8.length > 0) {
      parts.add("files", filePart("header.html", headerHtmlUtf8, MediaType.TEXT_HTML));
      parts.add("headerTemplate", "header.html");
    }
    if (footerHtmlUtf8 != null && footerHtmlUtf8.length > 0) {
      parts.add("files", filePart("footer.html", footerHtmlUtf8, MediaType.TEXT_HTML));
      parts.add("footerTemplate", "footer.html");
    }

    parts.add("paperWidth", "8.27");
    parts.add("paperHeight", "11.69");
    parts.add("marginTop", "0.5");
    parts.add("marginBottom", "0.5");
    parts.add("marginLeft", "0.5");
    parts.add("marginRight", "0.5");

    return client.post()
        .uri(endpoint)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .retrieve()
        .toEntity(byte[].class)
        .timeout(timeout)
        .map(this::requirePdfBody)
        .block(timeout);
  }

  // -------- helpers --------

//   private HttpHeaders htmlPartHeaders(String filename) {
//     HttpHeaders h = new HttpHeaders();
//     h.setContentType(MediaType.TEXT_HTML);
//     h.setContentDisposition(ContentDisposition
//         .builder("form-data")
//         .name("files")
//         .filename(filename, StandardCharsets.UTF_8)
//         .build());
//     return h;
//   }

  private org.springframework.http.HttpEntity<ByteArrayResource> filePart(String filename, byte[] bytes, MediaType mt) {
    var resource = new ByteArrayResource(bytes) {
      @Override public String getFilename() { return filename; }
    };
    HttpHeaders h = new HttpHeaders();
    h.setContentType(mt);
    h.setContentLength(bytes.length);
    h.setContentDisposition(ContentDisposition
        .builder("form-data")
        .name("files")
        .filename(filename, StandardCharsets.UTF_8)
        .build());
    return new org.springframework.http.HttpEntity<>(resource, h);
  }

private byte[] requirePdfBody(ResponseEntity<byte[]> resp) {
  // Spring 6: use getStatusCode().value()
  if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
    int status = resp.getStatusCode().value();
    String msg = "Gotenberg render failed: status=" + status;
    byte[] body = resp.getBody();
    if (body != null && body.length > 0) {
      msg += ", body=" + new String(body, java.nio.charset.StandardCharsets.UTF_8);
    }
    throw new IllegalStateException(msg);
  }
  return resp.getBody();
}


  private static ExchangeFilterFunction logOnError() {
    return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
      if (clientResponse.statusCode().isError()) {
        return clientResponse.bodyToMono(String.class)
            .defaultIfEmpty("<empty>")
            .flatMap(body -> Mono.error(new IllegalStateException(
                "Gotenberg HTTP " + clientResponse.statusCode().value() + ": " + body)));
      }
      return Mono.just(clientResponse);
    });
  }
}
