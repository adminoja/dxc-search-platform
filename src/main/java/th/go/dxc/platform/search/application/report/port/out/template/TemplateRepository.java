package th.go.dxc.platform.search.application.report.port.out.template;

public interface TemplateRepository {
  TemplateBundle load(String datasetId);
  record TemplateBundle(byte[] indexHtml, byte[] headerHtml, byte[] footerHtml) {}
}
