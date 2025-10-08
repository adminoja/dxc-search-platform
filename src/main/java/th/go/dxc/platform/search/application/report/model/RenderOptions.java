package th.go.dxc.platform.search.application.report.model;

import java.util.Locale;
import java.time.ZoneId;

public record RenderOptions(
    Locale locale,
    ZoneId zoneId,
    String templateName,      // required for HTML/PDF templating; ignore for raw CSV builders if not templated
    String filenameHint,      // e.g., dataset or title; used for content-disposition
    boolean singleUse         // consume token on success
) {
  public static RenderOptions of(String template, String filename) {
    return new RenderOptions(Locale.getDefault(), ZoneId.systemDefault(), template, filename, true);
  }
}
