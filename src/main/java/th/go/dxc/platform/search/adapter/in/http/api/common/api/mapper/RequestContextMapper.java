package th.go.dxc.platform.search.adapter.in.http.api.common.api.mapper;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import th.go.dxc.platform.search.domain.common.value.RequestContext;

/**
 * Builds RequestContext (per-request telemetry) from HTTP request/headers.
 */
public final class RequestContextMapper {
  private RequestContextMapper() {}

  /** Build from the reactive request (preferred). */
  public static RequestContext fromRequest(ServerHttpRequest req) {
    HttpHeaders h = req.getHeaders();
    InetSocketAddress remote = req.getRemoteAddress();
    return fromHeaders(h, remote);
  }

  /** Build from headers + optional remote address (useful in tests). */
  public static RequestContext fromHeaders(HttpHeaders headers, InetSocketAddress remoteAddress) {
    String correlationId = firstNonBlank(
        headers.getFirst("X-Correlation-ID"),
        headers.getFirst("X-Request-ID"),
        synthesizeFromTraceparent(headers.getFirst("traceparent"))
    );
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }

    // client IP: prefer proxy headers, then remoteAddress
    String clientIp = firstNonBlank(
        // X-Forwarded-For may contain a list; take the first hop
        firstIpFromXForwardedFor(headers.getFirst("X-Forwarded-For")),
        headers.getFirst("CF-Connecting-IP"),
        headers.getFirst("X-Real-IP"),
        remoteAddress != null && remoteAddress.getAddress() != null
            ? remoteAddress.getAddress().getHostAddress() : null
    );

    // user agent (trim to a safe length)
    String userAgent = safeUserAgent(headers.getFirst("User-Agent"));

    // request start: try standard-ish headers, else now()
    Instant requestStart = firstInstant(
        parseRequestStartHeader(headers.getFirst("X-Request-Start")),
        parseRequestStartHeader(headers.getFirst("X-Queue-Start")),
        Instant.now()
    );

    return new RequestContext(correlationId, clientIp, userAgent, requestStart);
  }

  // ---------------- helpers ----------------

  private static String firstNonBlank(String... vals) {
    if (vals == null) return null;
    for (String v : vals) if (v != null && !v.isBlank()) return v;
    return null;
  }

  private static Instant firstInstant(Instant... vals) {
    if (vals == null) return null;
    for (Instant v : vals) if (v != null) return v;
    return null;
  }

  private static String firstIpFromXForwardedFor(String xff) {
    if (xff == null || xff.isBlank()) return null;
    int comma = xff.indexOf(',');
    String first = (comma >= 0 ? xff.substring(0, comma) : xff).trim();
    return first.isEmpty() ? null : first;
  }

  private static String synthesizeFromTraceparent(String tp) {
    // W3C traceparent: "00-<traceId>-<spanId>-<flags>"
    if (tp == null) return null;
    String[] parts = tp.split("-");
    if (parts.length >= 2 && parts[1].length() == 32) return parts[1];
    return null;
  }

  private static String safeUserAgent(String ua) {
    if (ua == null) return null;
    // avoid huge UA strings ending up in logs/DB
    int MAX = 512;
    return ua.length() <= MAX ? ua : ua.substring(0, MAX);
  }

  /**
   * Parse "X-Request-Start"/"X-Queue-Start" formats:
   * - "t=1600000000.123" (seconds with fraction),
   * - "1600000000123" (milliseconds),
   * - "1600000000" (seconds).
   */
  private static Instant parseRequestStartHeader(String v) {
    if (v == null || v.isBlank()) return null;
    String s = v.trim();
    if (s.startsWith("t=")) s = s.substring(2);

    // try millis/seconds integer
    try {
      if (s.indexOf('.') < 0) {
        long num = Long.parseLong(s);
        // heuristic: 13+ digits → millis
        return (s.length() >= 13)
            ? Instant.ofEpochMilli(num)
            : Instant.ofEpochSecond(num);
      }
    } catch (NumberFormatException ignore) { /* fall through */ }

    // try fractional seconds
    try {
      double secs = Double.parseDouble(s);
      long millis = (long) Math.floor(secs * 1000d);
      return Instant.ofEpochMilli(millis);
    } catch (NumberFormatException ignore) {
      return null;
    }
  }
}
