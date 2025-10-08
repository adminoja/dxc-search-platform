package th.go.dxc.platform.search.domain.report.model;

/** Points from a reportId to the cached row snapshot */
public record ReportPointer(
    String datasetId,
    String dataId,
    String runId,
    String scopeHash   // user-bound to prevent cross-user reads
) {}
