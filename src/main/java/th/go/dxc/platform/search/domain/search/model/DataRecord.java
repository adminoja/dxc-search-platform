// domain/search/model/RecordDetail.java
package th.go.dxc.platform.search.domain.search.model;

import java.util.Map;
import java.util.Objects;

public record DataRecord(
    Map<String, Object> data
    ) {
  public DataRecord {
    data = Objects.requireNonNullElseGet(data, Map::of);
  }

}
