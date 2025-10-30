package th.go.dxc.platform.search.adapter.out.search.run;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchStorePort;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchResult;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchState;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchStatus;

@AllArgsConstructor
@Component
public class InMemoryRunStore implements GlobalSearchStorePort {
  @Qualifier("globalSearchStatusCache")
  private final Cache<String, GlobalSearchState> statusCache;
  @Qualifier("globalSearchResultCache")
  private final Cache<String,GlobalSearchResult> resultCache;
  // public InMemoryRunStore() {
  //   this.cache = Caffeine.newBuilder()
  //       .expireAfterWrite(Duration.ofHours(24)) // TTL
  //       .maximumSize(10_000)
  //       .build();
  // }

  @Override
  public Mono<String> initRun(List<String> dsIds) {
    return Mono.fromSupplier(() -> {
      String runId = UUID.randomUUID().toString();
      statusCache.put(runId, new GlobalSearchState(runId, dsIds));
      return runId;
    });
  }

  @Override
  public Mono<GlobalSearchState> get(String runId) {
    return Mono.fromSupplier(() -> statusCache.getIfPresent(runId))
        .switchIfEmpty(Mono.error(new NoSuchElementException("run not found")));
  }

  @Override
  public Mono<Void> update(String runId, Consumer<GlobalSearchState> mutator) {
    return get(runId).doOnNext(st -> {
      mutator.accept(st);
      st.updatedAt = Instant.now();
    }).then();
  }

  @Override
  public Mono<Void> expire(String runId) {
    return Mono.fromRunnable(() -> statusCache.invalidate(runId));
  }

  @Override
  public Mono<Boolean> cancel(String runId) {
    return get(runId).map(st -> {
      st.cancelled = true;
      st.status = GlobalSearchStatus.CANCELLED;
      return true;
    })
        .onErrorReturn(false);
  }

  @Override
  public Mono<GlobalSearchResult> getResult(String runId) {
    // In-memory store does not keep results; implement as needed.
    return Mono.fromSupplier(() -> resultCache.getIfPresent(runId))
        .switchIfEmpty(Mono.error(new NoSuchElementException("result not found")));
  }
  @Override
  public Mono<Void> storeResult(String runId, GlobalSearchResult result) {
    return Mono.fromRunnable(() -> resultCache.put(runId, result));
  }
}
