package th.go.dxc.platform.search.application.report.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.common.usecase.CommandUseCase;
import th.go.dxc.platform.search.application.report.model.ReportToken;

public interface RenderHtmlReportUseCase extends CommandUseCase<RenderHtmlReportUseCase.Input,RenderHtmlReportUseCase.Output>{
    public Mono<Output> execute(Input input);
    public record Input(String scope, ReportToken token){}
    public record Output(String html){}

}
