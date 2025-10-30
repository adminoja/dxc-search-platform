package th.go.dxc.platform.search.application.catalog.port.in;

import java.util.List;

import reactor.core.publisher.Flux;
import th.go.dxc.platform.search.domain.catalog.model.Domain;

public interface ListDomainsByIdsUseCase {
    Flux<Domain> execute(ListDomainsByIdsUseCase.Input input);

    record Input(List<Domain.Id> domainIds) {
        public static Input of(List<Domain.Id> domainIds) {
            return new Input(domainIds);
        }
    }
}
