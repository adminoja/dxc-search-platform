// domain/audit/value/AuditEvent.java
package th.go.dxc.platform.search.domain.audit.value;

import java.time.Instant;

public record AuditEvent(
    Instant ts,
    String tenantId,
    String userId,
    String username,
    String clientId,
    String tokenId,
    String sessionId,
    Action action,          // enum (was String)
    String resourceType,
    String resourceId,
    Outcome outcome,        // enum (was String)
    String correlationId,
    String ip,
    String userAgent,
    Integer latencyMs,
    String detailsJson,
    Integer version         // optional: event schema version
) {}
