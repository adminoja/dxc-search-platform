package th.go.dxc.platform.search.application.search.port.out;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRun;

public interface GlobalSearchRunIndexPort {

    /** Insert new run entry at the beginning of a search */
    Mono<Void> save(GlobalSearchRun run);

    /** Update run status, finishedAt, or result count */
    Mono<Void> update(GlobalSearchRun run);

    /** Retrieve one run (optional for detail view) */
    Mono<GlobalSearchRun> get(String runId);

    /** List runs for a given user, sorted by requestedAt desc */
    Flux<GlobalSearchRun> listByUser(String userId, int limit, int offset);

    /** Optional: delete old or failed runs */
    Mono<Void> delete(String runId);
}
