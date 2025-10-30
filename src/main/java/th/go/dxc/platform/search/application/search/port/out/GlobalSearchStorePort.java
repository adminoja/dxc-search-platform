package th.go.dxc.platform.search.application.search.port.out;

import java.util.List;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchResult;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchState;

public interface GlobalSearchStorePort {
  Mono<String> initRun(List<String> datasetIds);     // returns runId
  Mono<GlobalSearchState> get(String runId);
  Mono<Void> update(String runId, Consumer<GlobalSearchState> mutator);
  Mono<Void> expire(String runId);                   // mark EXPIRED (or let cache evict)
  Mono<Boolean> cancel(String runId);
  Mono<Void> storeResult(String runId, GlobalSearchResult result);
  Mono<GlobalSearchResult> getResult(String runId);
}
