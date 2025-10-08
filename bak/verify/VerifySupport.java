package th.go.dxc.platform.search.application.report.verify;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;

final class VerifySupport {

  private VerifySupport() {}

  // ======= Stable SHA-256 of snapshot JSON (deterministic ordering) =======
  private static final ObjectMapper OM = new ObjectMapper()
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);

  static String stableSha256(Object snapshot) {
    try {
      byte[] json = OM.writeValueAsBytes(snapshot);
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
      return java.util.HexFormat.of().formatHex(digest);
    } catch (Exception e) {
      throw new IllegalStateException("hashing failed", e);
    }
  }

  // ======= Build Verify URL (uses forwarded headers or fixed base-url) =======
  static String buildVerifyUrlForVt(VerifyProperties props, @Nullable ServerHttpRequest req, String vt) {
    String base = (props.baseUrl() != null && !props.baseUrl().isBlank())
        ? trimTrailingSlash(props.baseUrl())
        : externalBase(req); // req must be non-null if no base-url override
    return base + props.endpointPath() + "?vt=" + URLEncoder.encode(vt, StandardCharsets.UTF_8);
  }

  /** Resolve external base URL from WebFlux request using Forwarded / X-Forwarded-* (no deprecated APIs). */
  static String externalBase(ServerHttpRequest request) {
    URI u = request.getURI();
    HttpHeaders h = request.getHeaders();

    // Prefer RFC 7239 Forwarded header if present (take the first entry before comma)
    String fwd = first(h.getFirst("Forwarded"));
    String proto = null;
    String hostPort = null; // may contain "host:port" or "[v6]:port"

    if (fwd != null) {
      String firstElem = fwd.split(",", 2)[0].trim();
      for (String part : firstElem.split(";")) {
        String p = part.trim();
        if (p.regionMatches(true, 0, "proto=", 0, 6)) proto = stripQuotes(p.substring(6));
        else if (p.regionMatches(true, 0, "host=", 0, 5)) hostPort = stripQuotes(p.substring(5));
      }
    }

    // Fallbacks to X-Forwarded-* if Forwarded was absent/partial
    if (proto == null) proto = first(h.getFirst("X-Forwarded-Proto"));
    if (hostPort == null) hostPort = first(h.getFirst("X-Forwarded-Host"));

    // If X-Forwarded-Port exists and hostPort has no explicit port, apply it
    String xfPort = first(h.getFirst("X-Forwarded-Port"));

    // Defaults from the actual request URI
    String scheme = proto != null ? proto : u.getScheme();
    String host = u.getHost();
    int port = u.getPort(); // -1 if unknown

    if (hostPort != null && !hostPort.isBlank()) {
      // hostPort may be "example.com", "example.com:8443", or "[2001:db8::1]:8443"
      if (hostPort.startsWith("[")) { // IPv6 in brackets
        int end = hostPort.indexOf(']');
        host = hostPort.substring(1, end);
        int colon = hostPort.indexOf(':', end);
        if (colon > 0) port = parsePort(hostPort.substring(colon + 1), port);
        else if (xfPort != null) port = parsePort(xfPort, port);
        else if (port == -1) port = defaultPortFor(scheme);
      } else {
        int colon = hostPort.lastIndexOf(':'); // safe even if no port
        if (colon > 0 && hostPort.indexOf(':') == colon) { // single colon (IPv4/hostname with port)
          host = hostPort.substring(0, colon);
          port = parsePort(hostPort.substring(colon + 1), port);
        } else {
          host = hostPort;
          if (xfPort != null) port = parsePort(xfPort, port);
          else if (port == -1) port = defaultPortFor(scheme);
        }
      }
    } else if (xfPort != null) {
      port = parsePort(xfPort, port);
    }

    StringBuilder base = new StringBuilder();
    base.append(scheme).append("://");
    // Re-wrap IPv6 literal
    if (host.contains(":")) base.append('[').append(host).append(']');
    else base.append(host);

    // Append port only if it is non-default for the scheme
    if (port != -1 && !(scheme.equalsIgnoreCase("https") && port == 443)
        && !(scheme.equalsIgnoreCase("http") && port == 80)) {
      base.append(':').append(port);
    }

    // Optional prefix (e.g. /report)
    String prefix = first(h.getFirst("X-Forwarded-Prefix"));
    if (prefix != null && !prefix.isBlank()) {
      if (!prefix.startsWith("/")) prefix = "/" + prefix;
      if (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
      base.append(prefix);
    }
    return base.toString();
  }

  private static int defaultPortFor(String scheme) {
    return "https".equalsIgnoreCase(scheme) ? 443 : ("http".equalsIgnoreCase(scheme) ? 80 : -1);
  }
  private static int parsePort(String s, int fallback) {
    try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; }
  }
  private static String stripQuotes(String s) {
    if (s == null) return null;
    s = s.trim();
    if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }
  private static String first(String v) {
    if (v == null) return null;
    int idx = v.indexOf(',');
    return idx >= 0 ? v.substring(0, idx).trim() : v.trim();
  }
  private static String trimTrailingSlash(String s) {
    return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
  }

  // ======= QR as Base64 PNG (ZXing) =======
  static String qrBase64Png(String text, int size) {
    try {
      var matrix = new QRCodeWriter().encode(
          text, BarcodeFormat.QR_CODE, size, size, Map.of(EncodeHintType.MARGIN, 1));
      BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
      for (int x = 0; x < size; x++) {
        for (int y = 0; y < size; y++) {
          img.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
        }
      }
      var baos = new java.io.ByteArrayOutputStream();
      ImageIO.write(img, "png", baos);
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (Exception e) {
      throw new IllegalStateException("QR generation failed", e);
    }
  }

  // ======= Thai BE timestamp helper =======
  static String be(Instant instant) {
    var z = instant.atZone(ZoneId.of("Asia/Bangkok"));
    int beYear = z.getYear() + 543;
    String[] m = {"ม.ค.","ก.พ.","มี.ค.","เม.ย.","พ.ค.","มิ.ย.","ก.ค.","ส.ค.","ก.ย.","ต.ค.","พ.ย.","ธ.ค."};
    return String.format("%02d %s %d %02d:%02d น.",
        z.getDayOfMonth(), m[z.getMonthValue() - 1], beYear, z.getHour(), z.getMinute());
  }
}
