// domain/search/model/SearchResultItem.java
package th.go.dxc.platform.search.domain.search.model;

import java.util.Objects;

import th.go.dxc.platform.search.domain.report.model.ReportToken;

public record LocalSearchRecord(
    ReportToken reportToken, // snapshot handle for this row (dataId+runId signed)
    DataRecord dataRecord
) {
  public static LocalSearchRecord of(
      ReportToken reportToken,
      DataRecord dataRecord) {
    return new LocalSearchRecord(
        reportToken,
        dataRecord);
  }
  // public static LocalSearchRecord of(
  //     String reportTokenValue,
  //     DataRecord dataRecord) {
  //   return new LocalSearchRecord(
  //       reportTokenValue == null ? null : new ReportToken(reportTokenValue),
  //       dataRecord);
  // }
  public LocalSearchRecord {
    // Objects.requireNonNull(recordId, "recordId");
    Objects.requireNonNull(reportToken, "reportId");
    if (dataRecord == null || dataRecord.isEmpty()) {
      dataRecord = DataRecord.empty(); // no nulls here
    } 
  }


}
