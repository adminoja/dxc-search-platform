package th.go.dxc.platform.search.domain.report.model;

import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonValue;

public record ReportToken(@JsonValue UUID value) {

  // compact constructor for validation
  public ReportToken {
    if (value == null) {
      throw new IllegalArgumentException("report token must be non-blank");
    }
  }

  // Jackson: treat the single string as the whole object
  @JsonCreator(mode = Mode.DELEGATING)
  public static ReportToken of(String raw) {
    try {
      return new ReportToken(UUID.fromString(raw)); // strict parse
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Invalid ReportToken", ex);
    }
  }

  public static ReportToken random() {
    return new ReportToken(UUID.randomUUID());
  }

  public static ReportToken none() {
    return new ReportToken(new UUID(0L, 0L));
  }

  /** explicit accessor for business logic */
  public String asString() {
    return value.toString();
  }

  /** masked for logs/debug (e.g., abcd…wxyz) */
  public String masked() {
    String s = value.toString();
    return s.substring(0, 4) + "…" + s.substring(s.length() - 4);
  }

  public String scoped(String scopeHash) {
    return scopeHash + ":" + value;
  }

  public String shortId() {
    return Integer.toHexString(value.hashCode());
  }

}
