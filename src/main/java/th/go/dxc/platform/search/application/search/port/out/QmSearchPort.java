package th.go.dxc.platform.search.application.search.port.out;


import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.catalog.model.DatasetRoute;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.search.model.DataRecord;
import th.go.dxc.platform.search.domain.search.model.SearchRequest;

public interface QmSearchPort {
    Mono<DomainPageResult<DataRecord>> search(DatasetRoute route, SearchRequest request,UserContext userContext);
}
