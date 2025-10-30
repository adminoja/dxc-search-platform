package th.go.dxc.platform.search.application.search.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.search.model.SpecializedReportResult;

public interface GetSpecializedReportResultUseCase {

    public Mono<SpecializedReportResult> execute(Input input);
    public record Input(String runId) {
        public static Input of(String runId) {
            return new Input(runId);
        }
    }
}
