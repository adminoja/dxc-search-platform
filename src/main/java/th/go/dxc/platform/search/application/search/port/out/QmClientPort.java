package th.go.dxc.platform.search.application.search.port.out;


import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.search.model.DataRecord;
import th.go.dxc.platform.search.domain.search.model.LocalSearchRequest;

public interface QmClientPort {
    Mono<DomainPageResult<DataRecord>> search(Dataset.Route route, LocalSearchRequest request,UserContext userContext);
    // Mono<DomainPageResult<Map<String,Object>>> search(DatasetRoute route, LocalSearchRequest request,UserContext userContext);
    
}
