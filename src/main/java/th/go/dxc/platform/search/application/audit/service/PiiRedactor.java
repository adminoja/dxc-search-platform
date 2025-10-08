package th.go.dxc.platform.search.application.audit.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class PiiRedactor {
  private final String keyId;
  private final byte[] key;
  private final boolean outBase64;

  public PiiRedactor(String keyId, String hmacKeyB64, String output) {
    this.keyId = (keyId == null || keyId.isBlank()) ? "v1" : keyId;
    this.key = Base64.getDecoder().decode(hmacKeyB64);
    this.outBase64 = "base64".equalsIgnoreCase(output);
  }

  /** Deterministic keyed hash (HMAC-SHA256). */
  public String hashStable(String value) {
    if (value == null) return null;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      byte[] h = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
      String body = outBase64
          ? Base64.getUrlEncoder().withoutPadding().encodeToString(h)
          : toHex(h);
      return keyId + ":" + body;
    } catch (Exception e) {
      return null; // or rethrow as unchecked if you prefer
    }
  }

  public String toSafeJson(java.util.Map<String, ?> details) {
    if (details == null || details.isEmpty()) return null;
    Object datasetId = details.get("datasetId");
    Object templateId = details.get("templateId");
    Object count = details.get("count");
    return "{\"datasetId\":\"" + safe(datasetId) + "\","
         + "\"templateId\":\"" + safe(templateId) + "\","
         + "\"count\":\"" + safe(count) + "\"}";
  }

  private static String toHex(byte[] b) {
    StringBuilder sb = new StringBuilder(b.length * 2);
    for (byte x : b) sb.append(String.format("%02x", x));
    return sb.toString();
  }
  private static String safe(Object o) { return o == null ? "" : String.valueOf(o).replace("\"", "\\\""); }
}
