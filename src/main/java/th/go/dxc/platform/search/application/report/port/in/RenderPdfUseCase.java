package th.go.dxc.platform.search.application.report.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.common.usecase.CommandUseCase;
import th.go.dxc.platform.search.application.report.model.ReportToken;
import th.go.dxc.platform.search.domain.common.value.UserContext;

public interface RenderPdfUseCase extends CommandUseCase<RenderPdfUseCase.Input, RenderPdfUseCase.Output> {

  Mono<Output> execute(Input input);

  /**
   * Input:
   *  - scope: your scope hash used everywhere else
   *  - token: the report token (same as HTML step)
   *  - filename: optional filename for the PDF
   *  - options: Chromium tuning (all optional; sensible defaults used if null)
   */
  record Input(String scope, ReportToken token, String filename, Options options,UserContext user) {}

  record Options(
      Boolean printBackground,
      Boolean preferCssPageSize,
      Boolean landscape,
      Double  scale,
      String  marginTop,
      String  marginBottom,
      String  marginLeft,
      String  marginRight,
      String  waitDelay,         // e.g. "1500ms" or "2s"
      String  waitForExpression  // e.g. "window._ready === true"
  ) {}

  /** Output PDF bytes (+ metadata for controller headers) */
  record Output(String filename, String contentType, byte[] pdfBytes) {}
}
