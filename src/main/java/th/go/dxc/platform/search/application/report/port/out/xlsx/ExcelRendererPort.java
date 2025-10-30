package th.go.dxc.platform.search.application.report.port.out.xlsx;

import th.go.dxc.platform.search.domain.report.model.ReportDataSnapshot;

public interface ExcelRendererPort {
  byte[] fromSnapshot(ReportDataSnapshot snapshot); // adapters can use Apache POI/JXLS
}
