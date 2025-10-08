package th.go.dxc.platform.search.domain.common.value;

import java.time.Instant;
import java.util.List;

public record TokenContext(
    String tokenId,        // jti
    String sessionId,      // session_state
    Instant issuedAt,      // iat
    Instant authTime,      // when user authenticated
    String acr,            // e.g., "urn:mace:incommon:iap:silver" or realm-specific
    List<String> amr       // e.g., ["pwd","otp","webauthn"]
) {}