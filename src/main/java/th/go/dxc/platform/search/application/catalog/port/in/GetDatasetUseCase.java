package th.go.dxc.platform.search.application.catalog.port.in;


import java.util.Optional;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.common.usecase.QueryUseCase;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;

public interface GetDatasetUseCase extends QueryUseCase<GetDatasetUseCase.Input,GetDatasetUseCase.Result>{
    Mono<Result> execute(Input input);
    record Input(Dataset.Id id) {}
    record Result(Optional<Dataset> dataset) {}
}
