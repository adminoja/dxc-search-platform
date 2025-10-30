package th.go.dxc.platform.search.domain.search.model;

import java.time.Instant;
import java.util.List;


public record GlobalSearchResult(
    String runId,
    String productFeatureId,
    List<LocalSearchResult> results,
    Instant startedAt,                     // when request dispatched
    Long durationMs                        // millis between start and finish
    
    ) {


    public static GlobalSearchResult of(String runId,String productFeatureId, List<LocalSearchResult> results, Instant startedAt, long durationMs) {
        runId = runId == null ? "" :  runId;
        results = results == null ? List.of() : results;
        startedAt = startedAt == null ? Instant.EPOCH : startedAt;
        durationMs = durationMs < 0 ? 0 : durationMs;
        return new GlobalSearchResult(runId,productFeatureId, results, startedAt, durationMs);
    }
    
    
}
