package th.go.dxc.platform.search.adapter.in.security.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.oauth2.jwt.Jwt;

import th.go.dxc.platform.search.domain.common.value.TokenContext;

/**
 * Maps Spring Security Jwt → TokenContext (token/session facts).
 * Keep this separate from UserContext (identity/authorization).
 */
public final class TokenContextMapper {
  private TokenContextMapper() {}

  public static TokenContext fromJwt(Jwt jwt) {
    Objects.requireNonNull(jwt, "jwt is required");

    String tokenId   = jwt.getId();                          // jti (may be null)
    String sessionId = jwt.getClaimAsString("session_state");

    // Issued-at (iat) is exposed as Jwt#getIssuedAt() when available
    Instant issuedAt = jwt.getIssuedAt();
    if (issuedAt == null) {
      issuedAt = parseInstantClaim(jwt.getClaim("iat"));     // fallback if provider omitted std field
    }

    // OIDC "auth_time" (seconds since epoch). Fallback to iat if absent.
    Instant authTime = parseInstantClaim(jwt.getClaim("auth_time"));
    if (authTime == null) authTime = issuedAt;

    String acr = jwt.getClaimAsString("acr");                // assurance level, optional
    List<String> amr = toStringList(jwt.getClaim("amr"));    // authentication methods, optional

    return new TokenContext(tokenId, sessionId, issuedAt, authTime, acr, amr);
  }

  // -------- helpers --------

  private static Instant parseInstantClaim(Object v) {
    if (v == null) return null;
    if (v instanceof Instant i) return i;
    if (v instanceof Number n)  return Instant.ofEpochSecond(n.longValue());
    if (v instanceof String s) {
      // try epoch seconds first, then ISO-8601
      try { return Instant.ofEpochSecond(Long.parseLong(s)); } catch (NumberFormatException ignore) {}
      try { return Instant.parse(s); } catch (Exception ignore) {}
    }
    return null;
  }

  private static List<String> toStringList(Object v) {
    if (!(v instanceof Collection<?> c)) return List.of();
    List<String> out = new ArrayList<>(c.size());
    for (Object o : c) if (o != null) out.add(o.toString());
    out.sort(String::compareTo);
    return out;
  }
}
