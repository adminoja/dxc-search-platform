// src/main/java/th/go/dxc/platform/search/application/report/model/ReportModel.java
package th.go.dxc.platform.search.application.report.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportModel(
    ReportToken token,                 // useful for downstream actions (render/presign)
    String datasetId,
    Instant createdAt,
    Map<String, Object> data,
    Map<String, Object> metadata       // optional
) {
  public ReportModel {
    Objects.requireNonNull(token, "token is required");
    Objects.requireNonNull(datasetId, "datasetId is required");
    Objects.requireNonNull(createdAt, "createdAt is required");
    Objects.requireNonNull(data, "data is required");
  }

  public static ReportModel from(ReportToken token, ReportDataSnapshot s) {
    Objects.requireNonNull(s, "snapshot is required");
    return new ReportModel(
        token,
        s.datasetId(),
        s.createdAt(),
        s.data(),
        null
    );
  }
}
