package th.go.dxc.platform.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Base64;
import java.util.List;

@ConfigurationProperties(prefix = "platform.security")
public record SecurityProperties(
    List<String> csrfIgnorePaths,
    Swagger swagger,
    Cookies cookies,
    String hmacSecretB64, // base64 of 32+ random bytes
    String scopeVersion // e.g., "v1"
) {
  public record Swagger(Boolean enabled, List<String> roles) {
  }

  public record Cookies(String sameSite, Boolean secure) {
  }

  // optional defaults / normalization
  public SecurityProperties {
    if (hmacSecretB64 == null || hmacSecretB64.isBlank()) {
      hmacSecretB64 = "CHANGE_ME_BASE64";
    }
    if (scopeVersion == null || scopeVersion.isBlank()) {
      scopeVersion = "v1";
    }
  }

  public byte[] hmacSecret() {
    return Base64.getDecoder().decode(hmacSecretB64);
  }
}
