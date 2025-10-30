package th.go.dxc.platform.search.adapter.in.http.api.search.dto;

import java.time.Instant;
import java.util.List;

public record GlobalSearchResultDto(
    String runId,
    List<LocalSearchResultDto> results,
    Instant startedAt,                     // when request dispatched
    Long durationMs                        // millis between start and finish
    ) {


    public static GlobalSearchResultDto of(String runId,List<LocalSearchResultDto> results, Instant startedAt, long durationMs) {
        runId = runId == null ? "" :  runId;
        results = results == null ? List.of() : results;
        startedAt = startedAt == null ? Instant.EPOCH : startedAt;
        durationMs = durationMs < 0 ? 0 : durationMs;
        return new GlobalSearchResultDto(runId, results, startedAt, durationMs);
    }
    
}
