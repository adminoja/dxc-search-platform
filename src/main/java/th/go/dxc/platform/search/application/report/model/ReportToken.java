package th.go.dxc.platform.search.application.report.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonValue;

public record ReportToken(@JsonValue String value) {

  // compact constructor for validation
  public ReportToken {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("report token must be non-blank");
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

  @Override public String toString() { return value; }
}
