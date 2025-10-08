package th.go.dxc.platform.search.adapter.out.report.template.pebble;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.loader.FileLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import th.go.dxc.platform.search.application.report.port.out.template.TemplateEnginePort;
import th.go.dxc.platform.search.config.ReportProperties;

/** Pebble-based TemplateEnginePort; supports classpath: or file: template roots via ReportProperties. */
public class PebbleTemplateEngineAdapter implements TemplateEnginePort {

  private final PebbleEngine engine;

  public PebbleTemplateEngineAdapter(ReportProperties props) {
    var t = props.templates();

    var builder = new PebbleEngine.Builder()
        .cacheActive(t.cacheSeconds() > 0)
        .strictVariables(true);                 // fail fast on missing fields

    if (t.location().startsWith("classpath:")) {
      // e.g. classpath:report-templates  ->  prefix "report-templates/"
      String prefix = t.location().substring("classpath:".length());
      if (!prefix.endsWith("/")) prefix += "/";
      ClasspathLoader loader = new ClasspathLoader();
      loader.setPrefix(prefix);
      loader.setSuffix(".html");               // include/extends can omit .html
      builder.loader(loader);
    } else {
      // supports "file:/abs/dir" or plain "/abs/dir"
      String prefix = t.location().startsWith("file:")
          ? t.location().substring("file:".length())
          : t.location();
      if (!prefix.endsWith("/")) prefix += "/";
      FileLoader loader = new FileLoader();
      loader.setPrefix(prefix);
      loader.setSuffix(".html");
      builder.loader(loader);
    }

    this.engine = builder.build();
  }

  @Override
  public byte[] renderHtml(String templateName, Map<String, Object> model) {
    try (var out = new StringWriter()) {
      // Pass name WITHOUT ".html" (suffix is set on the loader)
      PebbleTemplate tpl = engine.getTemplate(templateName);
      tpl.evaluate(out, model == null ? Map.of() : model);
      return out.toString().getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Pebble render failed for template: " + templateName + ".html", e);
    }
  }
}
