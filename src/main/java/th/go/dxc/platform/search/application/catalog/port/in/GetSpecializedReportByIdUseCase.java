package th.go.dxc.platform.search.application.catalog.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.search.model.SpecializedReport;

public interface GetSpecializedReportByIdUseCase {
    Mono<SpecializedReport> execute(Input input);
    public record Input(String reportId) {
        public static Input of(String reportId) {
            return new Input(reportId);
        }
    }
}
