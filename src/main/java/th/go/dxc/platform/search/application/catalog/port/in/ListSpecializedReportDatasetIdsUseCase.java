package th.go.dxc.platform.search.application.catalog.port.in;

import reactor.core.publisher.Flux;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.search.model.SpecializedReport;

public interface ListSpecializedReportDatasetIdsUseCase {
    public Flux<Dataset.Id> execute(Input input);
    public record Input(
         SpecializedReport.Id reportId
    ) {
        public static Input of(
             SpecializedReport.Id reportId
        ) {
            return new Input(reportId);
        }
    }   
}
