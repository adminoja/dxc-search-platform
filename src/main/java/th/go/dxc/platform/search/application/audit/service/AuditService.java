package th.go.dxc.platform.search.application.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import th.go.dxc.platform.search.application.audit.port.out.AuditLogPort;
import th.go.dxc.platform.search.domain.audit.value.*;
import th.go.dxc.platform.search.domain.common.value.RequestContext;
import th.go.dxc.platform.search.domain.common.value.TokenContext;
import th.go.dxc.platform.search.domain.common.value.UserContext;

@Service
@RequiredArgsConstructor
public class AuditService {

  private final AuditLogPort port;
  private final PiiRedactor redactor;

  /** Generic logger. Prefer the typed helpers below when possible. */
  public Mono<Void> log(AuditEvent e) { return port.log(e); }

  /** Convenience: log successful report generation. */
  public Mono<Void> reportGenerateSuccess(
      UserContext u, TokenContext t, RequestContext r,
      String reportId, String objectKey, String datasetId, String templateId, int latencyMs
  ) {
    String details = redactor.toSafeJson(Map.of("datasetId", datasetId, "templateId", templateId));
    return port.log(new AuditEvent(
        Instant.now(),
        u.tenantId(), u.userId(), u.username(), u.clientId(),
        t == null ? null : t.tokenId(),
        t == null ? null : t.sessionId(),
        Action.REPORT_GENERATE, "REPORT", reportId + "->" + objectKey,
        Outcome.SUCCESS,
        r.correlationId(), r.clientIp(), r.userAgent(),
        latencyMs,
        details,
        1
    ));
  }

  /** Convenience: log denied/error for report generation. */
  public Mono<Void> reportGenerateDeniedOrError(
      UserContext u, TokenContext t, RequestContext r,
      String reportId, Outcome outcome, String errorCode, String errorMessage, int latencyMs
  ) {
    String details = redactor.toSafeJson(Map.of("datasetId", "", "templateId", "", "count", ""));
    // Keep error details minimal and non-PII; or move to a separate error store.
    return port.log(new AuditEvent(
        Instant.now(),
        u.tenantId(), u.userId(), u.username(), u.clientId(),
        t == null ? null : t.tokenId(),
        t == null ? null : t.sessionId(),
        Action.REPORT_GENERATE, "REPORT", reportId,
        outcome, // DENIED or ERROR
        r.correlationId(), r.clientIp(), r.userAgent(),
        latencyMs,
        details,
        1
    ));
  }
}
