package th.go.dxc.platform.search.application.report.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.common.usecase.CommandUseCase;
import th.go.dxc.platform.search.application.report.model.ReportToken;
import th.go.dxc.platform.search.domain.common.value.UserContext;

public interface RenderHtmlReportUseCase extends CommandUseCase<RenderHtmlReportUseCase.Input,RenderHtmlReportUseCase.Output>{
    public Mono<Output> execute(Input input);
    public record Input(String scope, ReportToken token,UserContext user){}
    public record Output(String html){}

}
