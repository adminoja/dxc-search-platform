package th.go.dxc.platform.search.application.common.report;

import java.time.LocalDate;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ThaiFormatUtil {
  public String dateBe(Object value, String pattern) {
    if (value == null) return "-";
    LocalDate d = (value instanceof LocalDate ld) ? ld : LocalDate.parse(value.toString());
    return DateTimeFormatter.ofPattern(pattern).format(ThaiBuddhistDate.from(d));
  }
  public String address(Object... parts) {
    var sb = new StringBuilder();
    for (Object p : parts) {
      var s = Objects.toString(p, "").trim();
      if (!s.isEmpty()) { if (sb.length() > 0) sb.append(' '); sb.append(s); }
    }
    return sb.length() == 0 ? "-" : sb.toString();
  }
}
