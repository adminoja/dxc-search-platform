package th.go.dxc.platform.search.domain.search.model;

import java.time.Instant;
import java.util.Objects;

import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

public record LocalSearchResult(
        String datasetId,
        String runId,
        LocalSearchStatus status,
        DomainPageResult<LocalSearchRecord> pageResult, // nullable on error
        // DomainPageResult<Map<String,Object>> pageResult, // nullable on error
        FailureInfo failure, // present on failures; null on success
        Instant startedAt, // when request dispatched
        Long durationMs // millis between start and finish

) {
    public static Boolean isSuccess(LocalSearchResult result) {
        Objects.requireNonNull(result, "result");
       return result.status==null?false:result.status.isSuccess();
    }
    public static Boolean isFailure(LocalSearchResult result) {
        Objects.requireNonNull(result, "result");
       return result.status==null?false:!result.status.isSuccess();
    }
    /** Success path; status auto-computed (EMPTY vs SUCCESS). */
    public static LocalSearchResult success(String datasetId,
            String runId,
            DomainPageResult<LocalSearchRecord> page,
            // DomainPageResult<Map<String,Object>> page,
            Instant startedAt,
            Long durationMs) {
        Objects.requireNonNull(datasetId, "datasetId");
        Objects.requireNonNull(page, "pageResult");
        var status = computeSuccessStatus(page, /* truncated */ false);
        return new LocalSearchResult(datasetId, runId, status, page, null, startedAt, durationMs);
    }

    /** Success path with truncation signal → PARTIAL. */
    public static LocalSearchResult successTruncated(String datasetId,
            String runId,
            DomainPageResult<LocalSearchRecord> page,
            // DomainPageResult<Map<String,Object>> page,
            Instant startedAt,
            Long durationMs) {
        Objects.requireNonNull(datasetId, "datasetId");
        Objects.requireNonNull(page, "pageResult");
        var status = computeSuccessStatus(page, /* truncated */ true);
        return new LocalSearchResult(datasetId, runId, status, page, null, startedAt, durationMs);
    }

    /** Failure path with structured info (status REQUIRED). */
    public static LocalSearchResult failure(String datasetId,
            String runId,
            LocalSearchStatus status,
            String message,
            Integer httpStatus,
            String upstreamCode,
            Instant startedAt,
            Long durationMs) {
        Objects.requireNonNull(datasetId, "datasetId");
        Objects.requireNonNull(status, "status");
        return new LocalSearchResult(
                datasetId, runId, status, null,
                new FailureInfo(httpStatus, upstreamCode, message),
                startedAt, durationMs);
    }
    public static LocalSearchResult cancelled(String datasetId,
            String runId,
            Instant startedAt,
            Long durationMs) {
        Objects.requireNonNull(datasetId, "datasetId");
        return new LocalSearchResult(
                datasetId, runId, LocalSearchStatus.CANCELLED, null,
                null,
                startedAt, durationMs);
    }
    // --- Helpers ---------------------------------------------------------------

    private static LocalSearchStatus computeSuccessStatus(DomainPageResult<?> page, boolean truncated) {
        if (truncated)
            return LocalSearchStatus.PARTIAL;
        long total = page.totalElements(); // adjust if your API differs
        return (total == 0) ? LocalSearchStatus.EMPTY : LocalSearchStatus.SUCCESS;
    }

    // --- Nested types ----------------------------------------------------------

    /** Extra failure diagnostics; safe to expose or log. */
    public record FailureInfo(
            Integer httpStatus, // nullable (transport may be non-HTTP)
            String upstreamCode, // nullable (vendor/app-specific)
            String message // short, user-friendly reason
    ) {
    }
}
