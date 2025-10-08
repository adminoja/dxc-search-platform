package th.go.dxc.platform.search.adapter.in.security.mapper;

import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;

import th.go.dxc.platform.search.domain.common.value.UserContext;

/**
 * Maps Spring Security Jwt → domain UserContext.
 * Keep this in an inbound security adapter package (framework → domain).
 */
public final class UserContextMapper {
    private UserContextMapper() {
    }

    /** Simple usage: derive everything from the JWT. */
    public static UserContext fromJwt(Jwt jwt) {

        return fromJwt(jwt, null, null, null,  null, null);
    }

    /**
     * Extended usage: allow overrides/extras for
     * tenant/client/authorities/correlation/locale/timezone.
     */
public static UserContext fromJwt(
        Jwt jwt,
        String overrideTenantId,
        String overrideClientId,
        Collection<String> extraAuthorities,
        Locale locale,
        ZoneId timeZone) {

    Objects.requireNonNull(jwt, "jwt is required");

    // ----- identifiers -----
    String username = firstNonBlank(
            jwt.getClaimAsString("preferred_username"),
            jwt.getClaimAsString("upn"),
            jwt.getClaimAsString("email"),
            jwt.getSubject());
    String userId = jwt.getSubject();

    String clientId = firstNonBlank(
            overrideClientId,
            jwt.getClaimAsString("azp"),
            jwt.getClaimAsString("client_id"));

    // issuer & realm (Keycloak/RH-SSO: iss = https://.../realms/<REALM>)
    String issuer = (jwt.getIssuer() != null)
            ? jwt.getIssuer().toString()
            : nullIfBlank(jwt.getClaimAsString("iss"));
    String realm  = (jwt.getIssuer() != null)
            ? realmFromIssuer(jwt.getIssuer())
            : realmFromIssuer(jwt.getClaimAsString("iss")); // null-safe

    // tenant: prefer explicit tenant claims/override; optionally fall back to realm
    String tenantId = firstNonBlank(
            overrideTenantId,
            jwt.getClaimAsString("tenant_id"),
            jwt.getClaimAsString("tenant"),
            jwt.getClaimAsString("organization"),
            realm // <— keep this fallback only if realm==tenant in your setup
    );

    // National ID / Citizen ID (PII — avoid logging)
    String nin = firstNonBlank(
            jwt.getClaimAsString("nin"),
            jwt.getClaimAsString("thai_cid"),
            jwt.getClaimAsString("citizen_id"),
            jwt.getClaimAsString("pid"),
            jwt.getClaimAsString("citizen_no"));

    // ----- locale & timezone -----
    if (locale == null) {
        String loc = firstNonBlank(jwt.getClaimAsString("locale"), jwt.getClaimAsString("ui_locale"));
        locale = (loc == null || loc.isBlank()) ? Locale.forLanguageTag("th-TH") : Locale.forLanguageTag(loc);
    }
    if (timeZone == null) {
        String zone = firstNonBlank(jwt.getClaimAsString("zoneinfo"), jwt.getClaimAsString("timezone"));
        timeZone = (zone == null || zone.isBlank()) ? ZoneId.of("Asia/Bangkok") : ZoneId.of(zone);
    }

    // ----- authorities (canonicalized) -----
    Set<String> authorities = new java.util.TreeSet<>();
    authorities.addAll(extractRealmRoles(jwt));
    authorities.addAll(extractResourceRoles(jwt, clientId));
    authorities.addAll(extractScopes(jwt));
    if (extraAuthorities != null) authorities.addAll(extraAuthorities);

    // Build record with new fields (issuer, realm)
    return new UserContext(
            nullIfBlank(userId),
            nullIfBlank(clientId),
            authorities,
            nullIfBlank(tenantId),
            nullIfBlank(username),
            nullIfBlank(nin),
            locale,
            timeZone,
            nullIfBlank(issuer),
            nullIfBlank(realm)
    );
}


    // ===== helpers =====

    @SuppressWarnings("unchecked")
    private static List<String> extractRealmRoles(Jwt jwt) {
        Object ra = jwt.getClaim("realm_access");
        if (!(ra instanceof Map))
            return List.of();
        Object roles = ((Map<String, Object>) ra).get("roles");
        if (!(roles instanceof Collection<?> col))
            return List.of();
        return col.stream().filter(Objects::nonNull).map(Object::toString).sorted().collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractResourceRoles(Jwt jwt, String clientId) {
        Object resAccess = jwt.getClaim("resource_access");
        if (!(resAccess instanceof Map))
            return List.of();

        List<String> out = new ArrayList<>();
        ((Map<String, Object>) resAccess).forEach((client, val) -> {
            if (val instanceof Map<?, ?> m) {
                Object roles = m.get("roles");
                if (roles instanceof Collection<?> col) {
                    for (Object r : col) {
                        if (r == null)
                            continue;
                        String role = r.toString();
                        // namespaced role
                        out.add(client + ":" + role);
                        // convenience: also add plain role for the active client
                        if (clientId != null && clientId.equals(client))
                            out.add(role);
                    }
                }
            }
        });
        Collections.sort(out);
        return out;
    }

    private static List<String> extractScopes(Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");
        if (scope == null || scope.isBlank())
            return List.of();
        return Arrays.stream(scope.split("\\s+"))
                .filter(s -> !s.isBlank())
                .map(s -> "SCOPE_" + s)
                .sorted()
                .collect(Collectors.toList());
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals)
            if (v != null && !v.isBlank())
                return v;
        return null;
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // ----- issuer → realm helpers (accept URL, URI, or String) -----

    private static String realmFromIssuer(URL iss) {
        if (iss == null)
            return null;
        try {
            return realmFromIssuer(iss.toURI());
        } catch (URISyntaxException e) {
            // Fallback to plain string parse
            return realmFromIssuer(iss.toString());
        }
    }

    private static String realmFromIssuer(String iss) {
        if (iss == null || iss.isBlank())
            return null;
        try {
            return realmFromIssuer(URI.create(iss));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String realmFromIssuer(URI iss) {
        if (iss == null)
            return null;
        String path = iss.getPath();
        if (path == null)
            return null;
        int i = path.indexOf("/realms/");
        if (i < 0)
            return null;
        String realm = path.substring(i + "/realms/".length());
        return realm.isBlank() ? null : realm;
    }
}
