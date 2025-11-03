package th.go.dxc.platform.search.application.search.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import th.go.dxc.platform.search.application.search.port.in.GetGlobalSearchResultUseCase;
import th.go.dxc.platform.search.application.search.port.in.SearchGlobalSearchUseCase;
import th.go.dxc.platform.search.application.search.port.in.SearchLocalSearchUseCase;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchRunIndexPort;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchStorePort;
import th.go.dxc.platform.search.domain.common.value.InvocationContext;
import th.go.dxc.platform.search.domain.common.value.InvocationContext.FeatureType;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.search.exception.DomainSearchException;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRequest;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchResult;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRun;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchStatus;
import th.go.dxc.platform.search.domain.search.model.LocalSearchRequest;
import th.go.dxc.platform.search.domain.search.model.LocalSearchResult;
import th.go.dxc.platform.search.domain.search.model.LocalSearchStatus;
import th.go.dxc.platform.search.domain.search.model.LocalSearchTaskStatus;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalSearchService implements SearchGlobalSearchUseCase, GetGlobalSearchResultUseCase {
  private final GlobalSearchStorePort store;
  private final SearchLocalSearchUseCase local; // you already have it
  private final GlobalSearchRunIndexPort runIndexPort;
  // cap concurrency so you don’t melt downstreams
  private final int maxConcurrency = 8;

  // @Override
  // public Mono<String> execute(SearchGlobalSearchUseCase.Input input) {
  // return startRunNew(input.req(), input.userContext(),
  // input.invocationContext());
  // }

  // GlobalSearchService.execute(...)
  @Override
  public Mono<String> execute(SearchGlobalSearchUseCase.Input input) {
    return ReactiveSecurityContextHolder.getContext()
        .map(sc -> sc.getAuthentication())
        .flatMap(auth -> startRunNew(input.req(), input.userContext(), input.invocationContext(), auth))
        .switchIfEmpty(startRunNew(input.req(), input.userContext(), input.invocationContext(), null));
  }

  @Override
  public Mono<GlobalSearchResult> execute(GetGlobalSearchResultUseCase.Input input) {
    return store.getResult(input.runId());
  }

  // private Mono<String> startRunNew(GlobalSearchRequest req, UserContext user,
  // InvocationContext invo) {
  // var dsIds = req.requests().stream().map(r -> r.datasetId().value()).toList();

  // return store.initRun(dsIds)
  // .doOnSuccess(runId -> {
  // // Fire-and-forget background pipeline
  // launchNew(runId, req, user, invo)
  // // needed only if your store/IO are blocking
  // .subscribeOn(Schedulers.boundedElastic())
  // // make sure failures mark the run as FAILED (bridge Mono<Void> properly)
  // .onErrorResume(ex ->
  // store.update(runId, st -> {
  // st.status = GlobalSearchStatus.FAILED;
  // st.finishedAt = Instant.now();
  // })
  // .then(Mono.error(ex))
  // )
  // .subscribe(
  // ignore -> {},
  // err -> log.error("GlobalSearch background failed runId={}", runId, err)
  // );
  // });
  // }
  private Mono<String> startRunNew(GlobalSearchRequest req,
      UserContext user,
      InvocationContext invo,
      @Nullable Authentication auth) {

    var dsIds = req.requests().stream().map(r -> r.datasetId().value()).toList();

    return store.initRun(dsIds)
        .doOnSuccess(runId -> {

          // NEW: record the run in the index for listing
          GlobalSearchRun runIdx = buildRunForIndex(runId, req, user, invo);
          runIndexPort.save(runIdx)
              // ensure the write happens even if the pipeline fails later
              .onErrorResume(e -> {
                log.error("Failed to save run index for runId={}", runId, e);
                return Mono.empty();
              })
              .subscribe();

          Mono<GlobalSearchResult> bg = launchNew(runId, req, user, invo)
              // if you have blocking I/O
              .subscribeOn(Schedulers.boundedElastic());

          // Re-attach SecurityContext so WebClient can see the JWT
          if (auth != null) {
            bg = bg.contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
          }

          bg.onErrorResume(ex -> store.update(runId, st -> {
            st.status = GlobalSearchStatus.FAILED;
            st.finishedAt = Instant.now();
          })
              .then(Mono.error(ex)))
              .subscribe(
                  ignore -> {
                  },
                  err -> log.error("GlobalSearch background failed runId={}", runId, err));
        });
  }

  private Mono<GlobalSearchResult> launchNew(String runId, GlobalSearchRequest req, UserContext user,
      InvocationContext invo) {
    var locals = req.requests();
    return store.update(runId, st -> {
      st.status = GlobalSearchStatus.RUNNING;
      st.startedAt = Instant.now();
    })

        .then(
            runIndexPort.get(runId)
                .flatMap(existing -> runIndexPort.update(existing.withStatus(GlobalSearchStatus.RUNNING, null)))
                .onErrorResume(e -> {
                  log.warn("Could not update run index to RUNNING for runId={}", runId, e);
                  return Mono.empty();
                }))

        .thenMany(Flux.fromIterable(locals)
            .flatMap(localReq -> executeOne(runId, localReq, user, invo), maxConcurrency))
        .collectList()
        .flatMap(localsResults -> finalizeRun(runId, req.requestedAt(), localsResults, invo));
  }

  private Mono<GlobalSearchResult> finalizeRun(String runId, Instant startedAt, List<LocalSearchResult> locals,
      InvocationContext invo) {

    Instant finishedAt = Instant.now();
    GlobalSearchStatus status = deriveRunStatus(locals); // SUCCEEDED / PARTIAL / FAILED

    GlobalSearchResult global = new GlobalSearchResult(
        runId,
        invo.productFeatureId(),

        locals,
        startedAt,
        Duration.between(startedAt, finishedAt).toMillis());
    return store.storeResult(runId, global)
        .then(
            store.update(runId, st -> {
              st.status = status;
              st.finishedAt = finishedAt; // or persist elsewhere as you prefer
            }))
        // NEW: mirror terminal status to run index
        .then(
            runIndexPort.get(runId)
                .flatMap(existing -> runIndexPort.update(existing.withStatus(status, finishedAt)))
                .onErrorResume(e -> {
                  log.error("Failed to update run index after finalize for runId={}", runId, e);
                  return Mono.empty();
                }))
        .thenReturn(global);
  }

  private Mono<LocalSearchResult> executeOne(String runId, LocalSearchRequest localReq, UserContext user,
      InvocationContext invo) {
    String dsId = localReq.datasetId().value();

    return store.get(runId).flatMap(st -> {
      if (st.cancelled) {
        st.perDataset.put(dsId,
            new LocalSearchTaskStatus(dsId, LocalSearchStatus.CANCELLED, "cancelled", null, null, false));
        st.cancelledCnt.incrementAndGet();
        st.completedOrFailed.incrementAndGet();

        // Also emit a LocalSearchResult representing cancellation so it ends up in the
        // global result list
        return Mono.just(LocalSearchResult.cancelled(dsId, runId, localReq.requestedAt(), System.currentTimeMillis()
            - localReq.requestedAt().toEpochMilli()));
      }

      st.perDataset.put(dsId, new LocalSearchTaskStatus(dsId, LocalSearchStatus.IN_PROGRESS, null, null, null, false));
      st.inProgress.incrementAndGet();

      var start = System.nanoTime();

      InvocationContext localSearchInvo = invo.withCurrent(FeatureType.LOCAL_SEARCH, localReq.datasetId().value());
      Boolean isReport = false;
      SearchLocalSearchUseCase.Input localQuery = SearchLocalSearchUseCase.Input.of(localReq, isReport, user,
          localSearchInvo);

      return local.execute(localQuery)
          // If your LocalSearchUseCase returns a page, adapt to your factory:
          // .map(page -> LocalSearchResult.success(dsId, runId, page,
          // localReq.requestedAt(), ...))
          .onErrorResume(e -> {
            var cause = reactor.core.Exceptions.unwrap(e);

            LocalSearchStatus status = (cause instanceof DomainSearchException dse)
                ? dse.status()
                : (cause instanceof java.util.concurrent.TimeoutException)
                    ? LocalSearchStatus.TIMEOUT
                    : (cause instanceof java.util.concurrent.CancellationException)
                        ? LocalSearchStatus.CANCELLED
                        : LocalSearchStatus.INTERNAL_ERROR;

            var msg = (cause.getMessage() == null || cause.getMessage().isBlank())
                ? cause.getClass().getSimpleName()
                : cause.getMessage();

            Integer httpStatus = (cause instanceof DomainSearchException dse) ? dse.httpStatus() : null;
            String upstreamCode = (cause instanceof DomainSearchException dse) ? dse.code() : null;

            log.error("Local search failed for dataset {} with status {}: {}", dsId, status, msg);

            return Mono.just(LocalSearchResult.failure(
                dsId,
                runId,
                status,
                "Search failed: " + msg,
                httpStatus,
                upstreamCode,
                localQuery.request().requestedAt(),
                System.currentTimeMillis() - localQuery.request().requestedAt().toEpochMilli()));
          })
          .flatMap(result -> {
            long ms = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            var terminal = result.status();
            var msg = (result.failure() != null) ? result.failure().message() : null;
            var http = (result.failure() != null) ? result.failure().httpStatus() : null;

            return store.update(runId, cur -> {
              cur.perDataset.put(dsId,
                  new LocalSearchTaskStatus(dsId, terminal, msg, http, ms,
                      result.status() == LocalSearchStatus.PARTIAL));
              cur.inProgress.decrementAndGet();
              cur.completedOrFailed.incrementAndGet();
              if (isFailure(terminal)) {
                cur.failed.incrementAndGet();
              }
            }).thenReturn(result);
          });
    });
  }

  private static boolean isFailure(LocalSearchStatus s) {
    return s.isFailure();
  }

  private GlobalSearchStatus deriveRunStatus(List<LocalSearchResult> locals) {
    boolean anyOk = locals.stream().anyMatch(LocalSearchResult::isSuccess);
    boolean anyFail = locals.stream().anyMatch(LocalSearchResult::isFailure);
    if (anyOk && anyFail)
      return GlobalSearchStatus.PARTIAL;
    if (anyOk)
      return GlobalSearchStatus.COMPLETED;
    return GlobalSearchStatus.FAILED;
  }

  private GlobalSearchRun buildRunForIndex(
      String runId,
      GlobalSearchRequest req,
      UserContext user,
      InvocationContext invo) {
    String userId = user.userId();
    String username = user.username();
    String reportId = invo.productFeatureId(); // you already set this on result
    String subjectNin = (String) (req.sharedCriteria()==null?null:req.sharedCriteria().getOrDefault("citizen_id", ""));
    return GlobalSearchRun.start(runId, userId, username, reportId, subjectNin, req.requestedAt());
  }
}
