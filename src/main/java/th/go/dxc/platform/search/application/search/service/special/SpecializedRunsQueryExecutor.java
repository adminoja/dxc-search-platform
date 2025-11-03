package th.go.dxc.platform.search.application.search.service.special;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.search.port.in.ListSpecializedRunsUseCase;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchRunIndexPort;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchStorePort;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRun;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchState;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchStatus;

@Component
@RequiredArgsConstructor
public class SpecializedRunsQueryExecutor implements ListSpecializedRunsUseCase {

        private final GlobalSearchRunIndexPort runIndex;
        private final GlobalSearchStorePort store;

        @Override
        public Mono<Result> execute(Input input) {
                return runIndex.listByUser(input.userId(), input.limit(), input.offset())
                                .flatMap(run ->
                                // try live state; if missing/evicted, still return a row from the index
                                store.get(run.runId())
                                                .map(st -> toRow(run, st))
                                                .onErrorResume(e -> Mono.just(toRow(run, null))), 8)

                                .distinct(ListSpecializedRunsUseCase.RunRow::runId)
                                .filter(row -> input.statusFilter() == null
                                                || row.status().name().equalsIgnoreCase(input.statusFilter()))
                                .filter(row -> input.reportIdFilter() == null
                                                || input.reportIdFilter().equals(row.reportId()))
                                .filter(row -> {
                                        if (input.query() == null || input.query().isBlank())
                                                return true;
                                        return row.subjectNin() != null && row.subjectNin().contains(input.query());
                                })
                                .collectList()
                                // If you later persist the index, replace list.size() with a real count.
                                .map(list -> new Result(list, list.size()));
        }

        private ListSpecializedRunsUseCase.RunRow toRow(GlobalSearchRun run, GlobalSearchState st) {
                GlobalSearchStatus status = (st != null && st.status != null) ? st.status : run.status();
                Instant requestedAt = run.requestedAt();
                Instant startedAt = (st != null && st.startedAt != null) ? st.startedAt : run.startedAt();
                Instant finishedAt = (st != null && st.finishedAt != null) ? st.finishedAt : run.finishedAt();

                Long durationMs = (startedAt != null && finishedAt != null)
                                ? Duration.between(startedAt, finishedAt).toMillis()
                                : null;

                ListSpecializedRunsUseCase.RunRow.Progress prog = (st == null) ? null
                                : new ListSpecializedRunsUseCase.RunRow.Progress(
                                                Math.max(0, st.completedOrFailed.get() - st.failed.get()
                                                                - st.cancelledCnt.get()),
                                                st.failed.get(),
                                                st.inProgress.get(),
                                                st.datasetIds.size());

                String viewUrl = "/api/report/specialized/" + run.runId();
                String pdfUrl = (run.pdfObjectKey() != null)
                                ? "/api/report/specialized/" + run.runId() + ".pdf"
                                : null;

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
