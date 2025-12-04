// src/main/java/th/go/dxc/platform/search/application/report/model/ReportModel.java
package th.go.dxc.platform.search.domain.report.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportModel(
    ReportToken token,                 // useful for downstream actions (render/presign)
    String datasetId,
    Instant createdAt,
    JsonNode data,
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
