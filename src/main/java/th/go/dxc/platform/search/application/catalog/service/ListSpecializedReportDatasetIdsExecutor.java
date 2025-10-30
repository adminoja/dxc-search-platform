package th.go.dxc.platform.search.application.catalog.service;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import th.go.dxc.platform.search.application.catalog.port.in.ListSpecializedReportDatasetIdsUseCase;
import th.go.dxc.platform.search.application.catalog.port.out.SpecializedReportCatalogRepository;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
@Slf4j
@Component
@RequiredArgsConstructor
public class ListSpecializedReportDatasetIdsExecutor implements ListSpecializedReportDatasetIdsUseCase{
    private final SpecializedReportCatalogRepository repo;

    @Override
    public Flux<Dataset.Id> execute(Input input) {
       return repo.findDatasetIds(input.reportId());
    }
}