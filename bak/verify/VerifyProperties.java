// application/report/verify/VerifyProperties.java
package th.go.dxc.platform.search.application.report.verify;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "platform.report.verify")
public record VerifyProperties(
    String baseUrl,            // leave blank to use Forwarded headers
    String endpointPath,       // default "/verify"
    Integer tokenTtlMinutes,   // default 1440
    Qr qr, Barcode barcode
) {
  public VerifyProperties {
    if (endpointPath == null || endpointPath.isBlank()) endpointPath = "/verify";
    if (tokenTtlMinutes == null || tokenTtlMinutes <= 0) tokenTtlMinutes = 1440;
    if (qr == null || qr.size == null || qr.size <= 0) qr = new Qr(240);
    if (barcode == null || barcode.enabled == null) barcode = new Barcode(true);
    if (baseUrl != null && baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length()-1);
  }
  public record Qr(Integer size) {}
  public record Barcode(Boolean enabled) {}
}
