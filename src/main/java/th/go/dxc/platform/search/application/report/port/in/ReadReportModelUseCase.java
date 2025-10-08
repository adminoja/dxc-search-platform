package th.go.dxc.platform.search.application.report.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.report.model.ReportModel;
import th.go.dxc.platform.search.application.report.model.ReportToken;
import th.go.dxc.platform.search.domain.common.value.UserContext;

public interface ReadReportModelUseCase {
  record Input(UserContext user, ReportToken token) {}
  record Output(ReportModel report) {}
  Mono<Output> execute(Input input);
}
