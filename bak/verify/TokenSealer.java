// application/report/verify/TokenSealer.java
package th.go.dxc.platform.search.application.report.verify;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

final class TokenSealer {
  private static final byte[] AAD = "DXC:VERIFY:v1".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  private final Map<Integer, SecretKeySpec> keys;
  private final int activeKid;
  private final ObjectMapper om = new ObjectMapper();
  private final SecureRandom rnd;

  TokenSealer(VerifyProperties p) {
    this.activeKid = p.keys().activeKid();
    this.keys = p.keys().material().entrySet().stream()
        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
            e -> new SecretKeySpec(Base64.getDecoder().decode(e.getValue()), "AES")));
    try { this.rnd = SecureRandom.getInstanceStrong(); } catch (Exception e) { throw new IllegalStateException(e); }
  }

  String seal(String rt, String scopeHash, Instant ts, Instant exp) {
    try {
      var json = om.writeValueAsBytes(Map.of("v",1,"kid",activeKid,"rt",rt,"sc",scopeHash,"ts",ts.toString(),"exp",exp.toString()));
      var iv = new byte[12]; rnd.nextBytes(iv);
      var c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.ENCRYPT_MODE, keys.get(activeKid), new GCMParameterSpec(128, iv));
      c.updateAAD(AAD);
      var ct = c.doFinal(json);
      var enc = Base64.getUrlEncoder().withoutPadding();
      return "v1."+activeKid+"."+enc.encodeToString(iv)+"."+enc.encodeToString(ct);
    } catch (Exception e) { throw new IllegalStateException("seal failed", e); }
  }

  Payload open(String vt) {
    try {
      var parts = vt.split("\\.",4);
      if (parts.length != 4 || !"v1".equals(parts[0])) throw new IllegalArgumentException("bad token");
      int kid = Integer.parseInt(parts[1]);
      var iv  = Base64.getUrlDecoder().decode(parts[2]);
      var ct  = Base64.getUrlDecoder().decode(parts[3]);
      var c = Cipher.getInstance("AES/GCM/NoPadding");
      var key = keys.get(kid);
      if (key == null) throw new IllegalArgumentException("unknown kid");
      c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
      c.updateAAD(AAD);
      var json = c.doFinal(ct);
      var n = new ObjectMapper().readTree(json);
      return new Payload(n.get("rt").asText(), n.get("sc").asText(),
          Instant.parse(n.get("ts").asText()), Instant.parse(n.get("exp").asText()), kid);
    } catch (Exception e) { throw new IllegalArgumentException("open failed", e); }
  }

  record Payload(String rt, String sc, Instant ts, Instant exp, int kid) {}
}
