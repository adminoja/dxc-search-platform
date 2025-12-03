package th.go.dxc.platform.search.domain.report.model;

import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonValue;

public record ReportToken(@JsonValue String value) {
  private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");

  // compact constructor for validation
  public ReportToken {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("report token must be non-blank");
    }
    if (!SAFE.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid ReportToken");
    }
  }

  // Jackson: treat the single string as the whole object
  @JsonCreator(mode = Mode.DELEGATING)
  public static ReportToken of(String v) {
    return new ReportToken(v);
  }

  public static ReportToken random() {
    return new ReportToken(UUID.randomUUID().toString());
  }

  public static ReportToken none() {
    return new ReportToken("NONE");
  }

  /** explicit accessor for business logic */
  public String asString() {
    return value;
  }

  /** masked for logs/debug (e.g., abcd…wxyz) */
  public String masked() {
    if (value.length() <= 8)
      return "****";
    return value.substring(0, 4) + "…" + value.substring(value.length() - 4);
  }

  /** optional: short form for metrics/tags */
  public String shortId() {
    return Integer.toHexString(value.hashCode());
  }

  public String scoped(String scopeHash) {
    return scopeHash + ":" + value;
  } // cache key helper

}
