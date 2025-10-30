// src/main/java/th/go/dxc/platform/search/application/report/model/ReportDataSnapshot.java
package th.go.dxc.platform.search.domain.report.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record ReportDataSnapshot(
  
    ReportToken reportToken,
    String datasetId,
    Instant createdAt,
    JsonNode data
) {
  public ReportDataSnapshot {
    Objects.requireNonNull(datasetId, "datasetId is required");
    Objects.requireNonNull(createdAt, "createdAt is required");
    Objects.requireNonNull(data, "data is required");
  }

  public Map<String, Object> dataAsMap() {
    return data != null && data.isObject() ? 
      new ObjectMapper().convertValue(data, new TypeReference<Map<String, Object>>() {}) : 
      Map.of();
  }

  public static ReportDataSnapshot of(ReportToken token,String datasetId, JsonNode data) {
    return new ReportDataSnapshot(token,datasetId, Instant.now(), data);
  }

  
}
