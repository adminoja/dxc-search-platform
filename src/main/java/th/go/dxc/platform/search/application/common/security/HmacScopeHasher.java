package th.go.dxc.platform.search.application.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public final class HmacScopeHasher implements ScopeHasher {
  private final byte[] secret;   // primary key bytes
  private final String version;  // e.g. "v1"

  public HmacScopeHasher(byte[] secret, String version) {
    log.trace("HmacScopeHasher: secret={}, version={}",secret,version);
    this.secret = secret;
    this.version = version;
  }

  @Override
  public String scopeFor(String userId, String tenant, String realm) {
    log.trace("scopeFor: userId={}, tenant={}, realm={}",userId,tenant,realm);
    String data = userId + "|" + tenant + "|" + realm;
    log.trace("data: {}",data);
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
      String scope =  version + ":" + b64;           // ex: v1:8m6y...
      log.trace("scope: {}", scope);
      return scope;
    } catch (Exception e) {
      throw new IllegalStateException("HMAC error", e);
    }
  }
}
