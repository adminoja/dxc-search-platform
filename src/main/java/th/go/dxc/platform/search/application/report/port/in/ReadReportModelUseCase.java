package th.go.dxc.platform.search.application.report.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.report.model.ReportModel;
import th.go.dxc.platform.search.domain.report.model.ReportToken;

public interface ReadReportModelUseCase {
  record Input(UserContext user, ReportToken token) {}
  record Output(ReportModel report) {}
  Mono<Output> execute(Input input);
}
