package th.go.dxc.platform.search.adapter.out.report.template.thymeleaf;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import th.go.dxc.platform.search.application.report.port.out.template.HtmlTemplateRendererPort;

import java.util.Locale;
import java.util.Map;

@Component
public class ThymeleafTemplateRenderer implements HtmlTemplateRendererPort {

  private final SpringTemplateEngine engine;

  public ThymeleafTemplateRenderer(SpringTemplateEngine engine) {
    this.engine = engine;
  }

  @Override
  public String render(String templateName, Map<String, Object> model) {
    var ctx = new Context(Locale.forLanguageTag("th-TH"));
    ctx.setVariables(model);
    return engine.process(templateName, ctx);
  }
}
