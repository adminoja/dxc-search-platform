package th.go.dxc.platform.search.domain.audit.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** Result of an audited action. Keep this small and stable. */
public enum Outcome {
  SUCCESS,
  DENIED,
  ERROR;

  @JsonValue
  public String json() {
    return name();
  }

  @JsonCreator
  public static Outcome from(String v) {
    if (v == null || v.isBlank()) return null;
    String n = normalize(v);
    for (Outcome o : values()) {
      if (o.name().equals(n)) return o;
    }
    // Unrecognized values map to ERROR (or return null if you prefer strictness)
    return ERROR;
  }

  private static String normalize(String s) {
    return s.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
  }
}
