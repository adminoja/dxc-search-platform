// src/main/java/th/go/dxc/platform/search/application/report/model/ReportDataSnapshot.java
package th.go.dxc.platform.search.application.report.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ReportDataSnapshot(
    String datasetId,
    Instant createdAt,
    Map<String, Object> data
) {
  public ReportDataSnapshot {
    Objects.requireNonNull(datasetId, "datasetId is required");
    Objects.requireNonNull(createdAt, "createdAt is required");
    Objects.requireNonNull(data, "data is required");
  }

  public static ReportDataSnapshot of(String datasetId, Map<String, Object> data) {
    return new ReportDataSnapshot(datasetId, Instant.now(), data);
  }
}
