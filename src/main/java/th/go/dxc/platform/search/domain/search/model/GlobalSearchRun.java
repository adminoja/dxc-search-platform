package th.go.dxc.platform.search.domain.search.model;

import java.time.Instant;

public record GlobalSearchRun(
    String runId,             // correlationId, PK
    String userId,            // who triggered
    String username,          // display name (optional)
    String reportId,          // SpecializedReport.id
    String subjectNin,        // citizen id or key field from criteria
    Instant requestedAt,      // from SpecializedReportRequest
    Instant startedAt,        // when actual search started
    Instant finishedAt,       // when finished/finalized
    GlobalSearchStatus status, // mirrors GlobalSearchState.status
    Integer resultCount,      // optional total record count
    String pdfObjectKey,      // optional path/key in object storage
    String errorMessage       // optional, for FAILED runs
) {

    public static GlobalSearchRun start(
        String runId, String userId, String username,
        String reportId, String subjectNin, Instant requestedAt
    ) {
        return new GlobalSearchRun(
            runId, userId, username, reportId, subjectNin,
            requestedAt, null, null, GlobalSearchStatus.QUEUED,
            null, null, null
        );
    }

    public GlobalSearchRun withStatus(GlobalSearchStatus newStatus, Instant finishedAt) {
        return new GlobalSearchRun(
            runId, userId, username, reportId, subjectNin,
            requestedAt, startedAt, finishedAt, newStatus,
            resultCount, pdfObjectKey, errorMessage
        );
    }
}
