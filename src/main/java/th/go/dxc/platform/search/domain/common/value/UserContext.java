package th.go.dxc.platform.search.domain.common.value;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;


/**
 * Identity/authorization context of the caller.
 * Keep this stable (who the user is and what they can do).
 * Do NOT include request-scoped telemetry like correlationId here.
 */
public record UserContext(
    String userId,           // subject / sub (stable identifier)
    String clientId,         // OAuth client / app id (optional)
    Set<String> authorities, // canonicalized roles/permissions
    String tenantId,         // realm/org/tenant (optional if single-tenant)
    String username,         // human-friendly login/display name
    String nin,              // sensitive national id (optional; avoid logging)
    Locale locale,           // user preference if available
    ZoneId timeZone,          // user preference if available
    String issuer,          // <- new: jwt.iss as String (e.g. https://sso/.../realms/DXC)
    String realm            // <- optional: parsed from issuer; may be null
) {}
