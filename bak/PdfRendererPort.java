package th.go.dxc.platform.search.application.report.port.out;

import reactor.core.publisher.Mono;
import java.util.Map;

public interface PdfRendererPort {
  Mono<byte[]> render(Template template, Map<String,Object> model);

  record Template(String indexHtml, String headerHtml, String footerHtml) {}
}
