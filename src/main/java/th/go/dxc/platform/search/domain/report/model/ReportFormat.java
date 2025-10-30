package th.go.dxc.platform.search.domain.report.model;

public enum ReportFormat {
  PDF("application/pdf", "pdf"),
  HTML("text/html; charset=UTF-8", "html"),
  // CSV("text/csv; charset=UTF-8", "csv"),
  // XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx")
  ;

  public final String contentType;
  public final String ext;
  ReportFormat(String contentType, String ext) { this.contentType = contentType; this.ext = ext; }
}
