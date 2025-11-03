package th.go.dxc.platform.search.application.search.service.special;

import java.time.Duration;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.search.port.in.GetSpecializedRunUseCase;
import th.go.dxc.platform.search.application.search.port.in.ListSpecializedRunsUseCase;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchRunIndexPort;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchStorePort;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRun;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchState;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchStatus;

@Component
@RequiredArgsConstructor
public class GetSpecializedRunExecutor implements GetSpecializedRunUseCase {

  private final GlobalSearchRunIndexPort runIndex;
  private final GlobalSearchStorePort store;

  @Override
  public Mono<ListSpecializedRunsUseCase.RunRow> execute(Input input) {
    return runIndex.get(input.runId())
        .flatMap(run -> store.get(input.runId())
            .onErrorResume(e -> Mono.empty())
            .map(st -> toRow(run, st))
            .switchIfEmpty(Mono.just(toRow(run, null))));
  }

  /** Map (run index + optional live state) -> table row */
  private ListSpecializedRunsUseCase.RunRow toRow(GlobalSearchRun run, GlobalSearchState st) {
    GlobalSearchStatus status = (st != null && st.status != null) ? st.status : run.status();
    Instant requestedAt = run.requestedAt();
    Instant startedAt = (st != null && st.startedAt != null) ? st.startedAt : run.startedAt();
    Instant finishedAt = (st != null && st.finishedAt != null) ? st.finishedAt : run.finishedAt();

    Long durationMs = (startedAt != null && finishedAt != null)
        ? Duration.between(startedAt, finishedAt).toMillis()
        : null;

    // use progress() helper if you added it; otherwise compute from fields
    ListSpecializedRunsUseCase.RunRow.Progress prog = null;
    if (st != null) {
      // If you have GlobalSearchProgress record:
      // var p = st.progress();
      // prog = new ListSpecializedRunsUseCase.RunRow.Progress(p.completed(),
      // p.failed(), p.inProgress(), p.total());

      // Or compute inline without GlobalSearchProgress:
      int completed = Math.max(0, st.completedOrFailed.get() - st.failed.get() - st.cancelledCnt.get());
      prog = new ListSpecializedRunsUseCase.RunRow.Progress(
          completed,
          st.failed.get(),
          st.inProgress.get(),
          st.datasetIds.size());
    }

    String viewUrl = "/api/report/specialized/" + run.runId();
    String pdfUrl = (run.pdfObjectKey() != null) ? "/api/report/specialized/" + run.runId() + ".pdf" : null;

    return new ListSpecializedRunsUseCase.RunRow(
        run.runId(),
        run.reportId(),
        run.subjectNin(),
        requestedAt,
        startedAt,
        finishedAt,
        status,
        durationMs,
        run.resultCount(),
        prog,
        viewUrl,
        pdfUrl);
  }
}
