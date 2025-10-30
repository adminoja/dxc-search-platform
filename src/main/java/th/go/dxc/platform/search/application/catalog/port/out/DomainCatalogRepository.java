package th.go.dxc.platform.search.application.catalog.port.out;

import java.util.List;

import reactor.core.publisher.Flux;
import th.go.dxc.platform.search.domain.catalog.model.Domain;

public interface DomainCatalogRepository {
    Flux<Domain> findDomainsByIds(List<Domain.Id> domainIds);
}
