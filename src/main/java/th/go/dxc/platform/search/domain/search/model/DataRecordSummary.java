// domain/search/model/SearchResultItem.java
package th.go.dxc.platform.search.domain.search.model;

import java.util.Map;
import java.util.Objects;

public record DataRecordSummary(
    // Id recordId,
    String reportToken, // snapshot handle for this row (dataId+runId signed)
    Map<String, Object> data
) {
  public DataRecordSummary {
    // Objects.requireNonNull(recordId, "recordId");
    Objects.requireNonNull(reportToken, "reportId");
    if (data == null || data.isEmpty()) {
      data = java.util.Map.of(); // no nulls here
    } else {
      // allow null values; forbid null keys (filter them out)
      var m = new java.util.LinkedHashMap<String, Object>();
      data.forEach((k, v) -> {
        if (k != null) m.put(k, v); // keep null values if present
      });
      data = java.util.Collections.unmodifiableMap(m);
    }
  }

  public record Id(String value) {
    public Id {
      if (value == null || value.isBlank())
        throw new IllegalArgumentException("dataId empty");
    }

    public static Id of(String v) {
      return new Id(v);
    }
  }

}
