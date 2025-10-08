package th.go.dxc.platform.search.application.report.port.out.csv;

import th.go.dxc.platform.search.application.report.model.ReportDataSnapshot;

public interface CsvRendererPort {
  byte[] fromSnapshot(ReportDataSnapshot snapshot); // or overload with options
}
