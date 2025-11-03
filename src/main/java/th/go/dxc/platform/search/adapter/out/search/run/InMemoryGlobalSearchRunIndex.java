package th.go.dxc.platform.search.adapter.out.search.run;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchRunIndexPort;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRun;

@AllArgsConstructor
@Component
public class InMemoryGlobalSearchRunIndex implements GlobalSearchRunIndexPort {

  // runId → GlobalSearchRun
  @Qualifier("globalSearchRunCache")
  private final Cache<String, GlobalSearchRun> runCache;

  // userId → Deque of runIds (most recent first)
  @Qualifier("userRunIndexCache")
  private final Cache<String, Deque<String>> userIndexCache;

  private static final int MAX_RUNS_PER_USER = 500;

  @Override
  public Mono<Void> save(GlobalSearchRun run) {
    return Mono.fromRunnable(() -> {
      runCache.put(run.runId(), run);

      userIndexCache.asMap().compute(run.userId(), (k, existing) -> {
        Deque<String> deque = (existing != null) ? existing : new ConcurrentLinkedDeque<>();
        // dedupe: remove if already present, then addFirst
        deque.remove(run.runId());
        deque.addFirst(run.runId());
        while (deque.size() > MAX_RUNS_PER_USER)
          deque.removeLast();
        return deque;
      });
    });
  }

  @Override
  public Mono<Void> update(GlobalSearchRun run) {
    return Mono.fromRunnable(() -> runCache.put(run.runId(), run));
  }

  @Override
  public Mono<GlobalSearchRun> get(String runId) {
    return Mono.fromSupplier(() -> Optional.ofNullable(runCache.getIfPresent(runId))
        .orElseThrow(() -> new NoSuchElementException("run not found")));
  }

  @Override
  public Flux<GlobalSearchRun> listByUser(String userId, int limit, int offset) {
    return Mono.fromSupplier(() -> {
      Deque<String> deque = userIndexCache.getIfPresent(userId);
      if (deque == null)
        return List.<GlobalSearchRun>of();

      return deque.stream()
          .skip(offset)
          .limit(limit)
          .map(runCache::getIfPresent)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }).flatMapMany(Flux::fromIterable);
  }

  @Override
  public Mono<Void> delete(String runId) {
    return Mono.fromRunnable(() -> {
      GlobalSearchRun run = runCache.getIfPresent(runId);
      if (run != null) {
        runCache.invalidate(runId);
        Deque<String> deque = userIndexCache.getIfPresent(run.userId());
        if (deque != null)
          deque.remove(runId);
      }
    });
  }
}
