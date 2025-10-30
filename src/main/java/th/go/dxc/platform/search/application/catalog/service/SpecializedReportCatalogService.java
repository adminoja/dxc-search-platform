package th.go.dxc.platform.search.application.catalog.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.catalog.port.in.GetSpecializedReportByIdUseCase;
import th.go.dxc.platform.search.application.catalog.port.in.ListDatasetFieldRuleMapByDomainCanonicalKeyUseCase;
import th.go.dxc.platform.search.application.catalog.port.in.ListDatasetLocalFieldsBySearchFieldsUseCase;
import th.go.dxc.platform.search.application.catalog.port.out.SpecializedReportCatalogRepository;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.search.model.SpecializedReport;

@RequiredArgsConstructor
@Component
public class SpecializedReportCatalogService
        implements GetSpecializedReportByIdUseCase, ListDatasetFieldRuleMapByDomainCanonicalKeyUseCase,ListDatasetLocalFieldsBySearchFieldsUseCase {
    private final SpecializedReportCatalogRepository repository;

    @Override
    public Mono<SpecializedReport> execute(GetSpecializedReportByIdUseCase.Input input) {
        return repository.findSpecializedReportById(new SpecializedReport.Id(input.reportId()));
    }

    @Override
    public Map<Dataset.Id, Dataset.FieldRule> execute(ListDatasetFieldRuleMapByDomainCanonicalKeyUseCase.Input input) {
        return repository.findDatasetIdFieldRule(input.domainId(), input.canonicalKey());
    }

    @Override
    public Map<Dataset.Id,Map<String,String>> execute(ListDatasetLocalFieldsBySearchFieldsUseCase.Input input){
        return repository.findDatasetIdLocalFields(input.searchFields());
    }
}
