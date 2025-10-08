package th.go.dxc.platform.search.domain.common.value;

import java.time.Instant;

// request telemetry (per request)
public record RequestContext(
    String correlationId,
    String clientIp,
    String userAgent,
    Instant requestStart
) {}