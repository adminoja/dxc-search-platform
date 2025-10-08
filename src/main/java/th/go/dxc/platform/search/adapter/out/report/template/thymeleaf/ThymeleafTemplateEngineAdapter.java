package th.go.dxc.platform.search.adapter.out.report.template.thymeleaf;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import th.go.dxc.platform.search.application.report.port.out.template.TemplateEnginePort;
import th.go.dxc.platform.search.config.ReportProperties;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * Renders HTML from Thymeleaf templates using the location defined in ReportProperties:
 *   - classpath:  e.g. "classpath:report-templates"
 *   - filesystem: e.g. "file:/opt/dxc/templates" or "/opt/dxc/templates"
 */
public class ThymeleafTemplateEngineAdapter implements TemplateEnginePort {

  private final TemplateEngine engine;
  private static final String SUFFIX = ".html";

  public ThymeleafTemplateEngineAdapter(ReportProperties props) {
    var t = props.templates();

    var resolver = t.location().startsWith("classpath:")
        ? classpathResolver(t.location(), t.cacheSeconds())
        : fileResolver(t.location(), t.cacheSeconds());

    this.engine = new TemplateEngine();
    this.engine.setTemplateResolver(resolver);
  }

  @Override
  public byte[] renderHtml(String templateName, Map<String, Object> model) {
    try {
      var ctx = new Context(Locale.getDefault());
      if (model != null) model.forEach(ctx::setVariable);
      String html = engine.process(templateName, ctx);       // pass name WITHOUT ".html"
      return html.getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Thymeleaf render failed for template: " + templateName + SUFFIX, e);
    }
  }

  // ---------- resolvers ----------

  private static ClassLoaderTemplateResolver classpathResolver(String location, int cacheSeconds) {
    // e.g. "classpath:report-templates" -> "report-templates/"
    String prefix = location.substring("classpath:".length());
    if (!prefix.endsWith("/")) prefix += "/";
    var r = new ClassLoaderTemplateResolver();
    r.setPrefix(prefix);
    r.setSuffix(SUFFIX);
    r.setTemplateMode(TemplateMode.HTML);
    r.setCharacterEncoding(StandardCharsets.UTF_8.name());
    r.setCheckExistence(true);
    if (cacheSeconds > 0) {
      r.setCacheable(true);
      r.setCacheTTLMs(cacheSeconds * 1000L);
    } else {
      r.setCacheable(false);
    }
    return r;
  }

  private static FileTemplateResolver fileResolver(String location, int cacheSeconds) {
    // supports "file:/abs/dir" or plain "/abs/dir"
    String prefix = location.startsWith("file:") ? location.substring("file:".length()) : location;
    if (!prefix.endsWith("/")) prefix += "/";
    var r = new FileTemplateResolver();
    r.setPrefix(prefix);
    r.setSuffix(SUFFIX);
    r.setTemplateMode(TemplateMode.HTML);
    r.setCharacterEncoding(StandardCharsets.UTF_8.name());
    r.setCheckExistence(true);
    if (cacheSeconds > 0) {
      r.setCacheable(true);
      r.setCacheTTLMs(cacheSeconds * 1000L);
    } else {
      r.setCacheable(false);
    }
    return r;
  }
}
