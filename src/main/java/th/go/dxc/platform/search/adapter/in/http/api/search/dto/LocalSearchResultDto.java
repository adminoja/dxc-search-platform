package th.go.dxc.platform.search.adapter.in.http.api.search.dto;

import java.time.Instant;

import th.go.dxc.platform.search.domain.search.model.LocalSearchResult.FailureInfo;
import th.go.dxc.platform.search.domain.search.model.LocalSearchStatus;

public record LocalSearchResultDto(
        String datasetid,
        String runId,
        LocalSearchStatus status,
        LocalSearchPageResultDto pageResult, // nullable on error
        FailureInfo failure, // present on failures; null on success
        Instant startedAt, // when request dispatched
        Long durationMs // millis between start and finish
) {
    public static LocalSearchResultDto of(
            String datasetid,
            String runId,
            LocalSearchStatus status,
            LocalSearchPageResultDto pageResult, // nullable on error
            FailureInfo failure, // present on failures; null on success
            Instant startedAt, // when request dispatched
            Long durationMs // millis between start and finish
    ) {
        return new LocalSearchResultDto(
                datasetid,
                runId,
                status,
                pageResult,
                failure,
                startedAt,
                durationMs);
    }
}
