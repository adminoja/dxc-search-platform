package th.go.dxc.platform.search.application.report.port.out.template;

import java.util.Map;

public interface TemplateEnginePort {
  byte[] renderHtml(String templateName, Map<String,Object> model); // returns UTF-8 HTML bytes
}
