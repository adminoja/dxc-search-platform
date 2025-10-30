package th.go.dxc.platform.search.application.catalog.service;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import th.go.dxc.platform.search.application.catalog.port.in.ListDomainsByIdsUseCase;
import th.go.dxc.platform.search.application.catalog.port.out.DomainCatalogRepository;

@Component
@RequiredArgsConstructor
public class DomainService implements ListDomainsByIdsUseCase {
    private final DomainCatalogRepository repository;

    @Override
    public reactor.core.publisher.Flux<th.go.dxc.platform.search.domain.catalog.model.Domain> execute(
            ListDomainsByIdsUseCase.Input input) {
        return repository.findDomainsByIds(input.domainIds());
    }

}
