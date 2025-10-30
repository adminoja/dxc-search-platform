package th.go.dxc.platform.search.application.catalog.port.out;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.catalog.model.Domain;
import th.go.dxc.platform.search.domain.search.model.SpecializedReport;

public interface SpecializedReportCatalogRepository {
    public Mono<SpecializedReport> findSpecializedReportById(SpecializedReport.Id id);
    public Flux<Dataset.Id> findDatasetIds(SpecializedReport.Id id);
    public Map<Dataset.Id, Dataset.FieldRule> findDatasetIdFieldRule(Domain.Id domainId,String canonicalKey);
    public Map<Dataset.Id,Map<String,String>> findDatasetIdLocalFields(List<String> searchField);
}
