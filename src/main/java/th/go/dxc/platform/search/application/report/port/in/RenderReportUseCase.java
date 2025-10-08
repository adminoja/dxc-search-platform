// src/main/java/th/go/dxc/platform/search/application/report/port/in/RenderReportUseCase.java
package th.go.dxc.platform.search.application.report.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.report.model.RenderOptions;
import th.go.dxc.platform.search.application.report.model.RenderedReport;
import th.go.dxc.platform.search.application.report.model.ReportFormat;
import th.go.dxc.platform.search.application.report.model.ReportToken;
import th.go.dxc.platform.search.domain.common.value.UserContext;

public interface RenderReportUseCase {
  Mono<RenderedReport> render(UserContext user,
                              ReportToken token,
                              ReportFormat format,
                              RenderOptions options);
}
