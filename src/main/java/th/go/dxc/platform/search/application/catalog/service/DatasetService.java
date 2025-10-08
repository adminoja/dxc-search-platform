package th.go.dxc.platform.search.application.catalog.service;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.catalog.port.in.GetDatasetUseCase;
import th.go.dxc.platform.search.application.catalog.port.in.ListDatasetUseCase;
import th.go.dxc.platform.search.application.catalog.port.out.DatasetRepository;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

@Service
@AllArgsConstructor
public class DatasetService  implements ListDatasetUseCase,GetDatasetUseCase {
    private final DatasetRepository datasetRepository;
    @Override
    public DomainPageResult<Dataset> query(ListDatasetUseCase.Query query) {
        return datasetRepository.searchDatasetByKeyword(query.keyword(),query.pageRequest());
    }
    @Override
    public Mono<GetDatasetUseCase.Result> execute(GetDatasetUseCase.Input input) {
        return Mono.just(new Result(datasetRepository.findDatasetById(input.id()))) ;
    }

}
