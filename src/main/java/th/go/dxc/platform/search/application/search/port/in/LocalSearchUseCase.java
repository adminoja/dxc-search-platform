package th.go.dxc.platform.search.application.search.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.search.model.SearchRequest;
import th.go.dxc.platform.search.domain.search.model.SearchResult;

public interface LocalSearchUseCase {
  Mono<SearchResult> query(Query query);
  record Query( SearchRequest request,UserContext userContext) {}
}