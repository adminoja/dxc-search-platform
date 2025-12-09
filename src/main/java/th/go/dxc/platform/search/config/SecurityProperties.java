package th.go.dxc.platform.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@ConfigurationProperties(prefix = "platform.security")
@Slf4j
public record SecurityProperties(
    List<String> csrfIgnorePaths,
    Swagger swagger,
    Cookies cookies,
    String hmacSecretB64, // base64 of 32+ random bytes
    String scopeVersion   // e.g., "v1"
) {

  public record Swagger(Boolean enabled, List<String> roles) {}
  public record Cookies(String sameSite, Boolean secure) {}

  // --------------------------------------------------------------------------------------------
  // ❗ INITIALIZER BLOCK FOR RECORD — runs after canonical constructor
  // This replaces the old hardcoded secret ("CHANGE_ME_BASE64").
  // - If no secret supplied, generate a secure random key.
  // - Warn developers so they know SIT/dev is using a temporary secret.
  // - Production systems should always supply their own secret externally.
  // --------------------------------------------------------------------------------------------
  public SecurityProperties {
    // Handle HMAC secret defaulting
    if (hmacSecretB64 == null || hmacSecretB64.isBlank()) {

      // ❗ Generate 32-byte random key (256-bit)
      byte[] randomKey = new byte[32];
      new SecureRandom().nextBytes(randomKey);

      hmacSecretB64 = Base64.getEncoder().encodeToString(randomKey);

      // ❗ Emit warning so SIT/dev teams know the secret changes on every restart
      log.warn("""
          WARNING: platform.security.hmacSecretB64 is not configured.
          A temporary random HMAC secret was generated for non-production environment.
          DO NOT use this behavior in production. Provide a stable Base64 secret via
          Config Server, Vault, or environment variable.
          """);
    }

    // Normalize scopeVersion
    if (scopeVersion == null || scopeVersion.isBlank()) {
      scopeVersion = "v1";
    }
  }

  // Convert base64 to byte[]
  public byte[] hmacSecret() {
    return Base64.getDecoder().decode(hmacSecretB64);
  }
}
