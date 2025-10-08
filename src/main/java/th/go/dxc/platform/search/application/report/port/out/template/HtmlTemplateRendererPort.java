package th.go.dxc.platform.search.application.report.port.out.template;

import java.util.Map;

public interface HtmlTemplateRendererPort {
  String render(String templateName, Map<String, Object> model);
}
