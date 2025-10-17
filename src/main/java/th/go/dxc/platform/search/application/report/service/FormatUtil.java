package th.go.dxc.platform.search.application.report.service;

import java.time.LocalDate;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import th.go.dxc.platform.search.config.ReportProperties;

@Component("format")
public class FormatUtil {

  private static final Locale TH = Locale.of("th", "TH");
  private final ReportProperties props;

  public FormatUtil(ReportProperties props) {
    this.props = props;
    
  }

  // ---------- Date ----------

  /** Raw text → Thai BE default ("TH_BE:d MMMM uuuu") */
  public String date(String raw) {
    LocalDate ld = parseToDate(raw, null);
    return ld != null ? formatOut(ld, "TH_BE:d MMMM uuuu") : "-";
  }

  /** Raw text → custom output (prefix-aware: TH_BE:/TH_CE:/EN_CE:) */
  public String date(String raw, String outputFormat) {
    LocalDate ld = parseToDate(raw, null);
    return ld != null ? formatOut(ld, outputFormat) : "-";
  }

  /** Raw text with explicit input + output formats (prefix-aware on both) */
  public String date(String raw, String inputFormat, String outputFormat) {
    LocalDate ld = parseToDate(raw, inputFormat);
    return ld != null ? formatOut(ld, outputFormat) : "-";
  }

  /** LocalDate → Thai BE default */
  public String date(LocalDate date) { return date(date, "TH_BE:d MMMM uuuu"); }

  /** LocalDate → custom output (prefix-aware) */
  public String date(LocalDate date, String outputFormat) {
    if (date == null) return "-";
    return formatOut(date, outputFormat);
  }

  public String date(LocalDate date, String inputFormat, String outputFormat) {
    return date(date, outputFormat);
  }

  // ---------- Code (flat keys) ----------

  /**
   * Map code via flat-key tables:
   * platform.report.templates.code.<key> = { RAW: "Label", ... }
   * Fallback: if <key> contains '-', try "common-" + rightPart.
   * Example: codeKey("MALE","dop-sex") → (dop-sex) else (common-sex) → "ชาย"
   */
  public String codeKey(String rawCode, String key) {
    if (key == null || key.isBlank()) return "-";
    if (rawCode == null) return "-";

    Map<String, Map<String, String>> all = props.template().code();
    if (all == null || all.isEmpty()) return rawCode;

    String trimmedKey = key.trim();
    String label = lookupFlat(all, trimmedKey, rawCode);
    if (label != null) return label;

    int dash = trimmedKey.indexOf('-');
    if (dash > 0) {
      String type = trimmedKey.substring(dash + 1);
      String commonKey = "common-" + type;
      label = lookupFlat(all, commonKey, rawCode);
      if (label != null) return label;
    }
    return rawCode;
  }

  private String lookupFlat(Map<String, Map<String, String>> all, String key, String raw) {
    Map<String, String> table = all.get(key);
    if (table == null) return null;
    String v = table.get(raw);
    if (v != null) return v;
    v = table.get(raw.toUpperCase(Locale.ROOT));
    if (v != null) return v;
    return table.get(raw.toLowerCase(Locale.ROOT));
  }

  // ---------- Strings ----------

  /** Mask string while keeping left/right characters visible. */
  public String mask(String raw, String maskChar, int skipLeft, int skipRight) {
    if (raw == null) return "-";
    String s = raw;
    String m = (maskChar == null || maskChar.isEmpty()) ? "*" : maskChar;
    int n = s.length();
    int left = Math.max(0, Math.min(skipLeft, n));
    int right = Math.max(0, Math.min(skipRight, n - left));
    int mid = Math.max(0, n - left - right);
    return s.substring(0, left) + m.repeat(mid) + s.substring(n - right);
  }

  /** Limit string length with ellipsis. */
  public String limit(String raw, Integer maxChar) {
    if (raw == null) return "-";
    if (maxChar == null || maxChar <= 0 || raw.length() <= maxChar) return raw;
    return raw.substring(0, Math.max(0, maxChar - 1)).trim() + "…";
  }

  /** Robust Thai address builder (skips blanks, adds labels, normalizes spaces). */
  public String address(Object houseNo, Object soi, Object street,
                        Object subdist, Object dist, Object prov) {
    List<String> parts = new ArrayList<>(6);
    add(parts, houseNo, "");
    add(parts, street, "ถนน ");
    add(parts, soi, "ซ. ");
    add(parts, subdist, "แขวง ");
    add(parts, dist, "เขต ");
    add(parts, prov, "จังหวัด ");
    return parts.isEmpty() ? "-" : String.join(" ", parts);
  }

  private void add(List<String> parts, Object v, String prefix) {
    if (v == null) return;
    String s = v.toString().trim();
    if (s.isEmpty()) return;
    parts.add(prefix + s.replaceAll("\\s+", " "));
  }

  // ---------- Internals (date parse/format) ----------

  private LocalDate parseToDate(String raw, String inputFormat) {
    if (raw == null) return null;
    String txt = raw.trim();
    if (txt.isEmpty()) return null;

    String fmt = inputFormat == null ? "" : inputFormat.trim();

    try {
      // Prefixed input patterns
      if (fmt.startsWith("TH_BE:")) {
        String pat = fmt.substring("TH_BE:".length());
        var f = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pat)
            .toFormatter(TH).withResolverStyle(ResolverStyle.STRICT);
        LocalDate d = LocalDate.from(f.parse(txt));
        return d.withYear(d.getYear() - 543); // BE → CE
      }
      if (fmt.startsWith("TH_CE:") || fmt.startsWith("TH:")) {
        String pat = fmt.substring(fmt.indexOf(':') + 1);
        var f = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pat)
            .toFormatter(TH).withResolverStyle(ResolverStyle.STRICT);
        return LocalDate.from(f.parse(txt)); // CE
      }
      if (fmt.startsWith("EN_CE:") || fmt.startsWith("EN:")) {
        String pat = fmt.substring(fmt.indexOf(':') + 1);
        var f = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pat)
            .toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
        return LocalDate.from(f.parse(txt)); // CE
      }

      // Plain explicit pattern → guess locale; fix 25xx as BE
      if (!fmt.isEmpty()) {
        Locale loc = rawLocaleGuess(txt);
        var f = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(fmt)
            .toFormatter(loc).withResolverStyle(ResolverStyle.STRICT);
        LocalDate d = LocalDate.from(f.parse(txt));
        if (d.getYear() >= 2400) d = d.withYear(d.getYear() - 543);
        return d;
      }

      // Auto-detect shapes
      return autoParse(txt);
    } catch (Exception ignore) {
      return autoParse(txt);
    }
  }

  private LocalDate autoParse(String s) {
    var candidates = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE, // 2025-10-10
        DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT),
        // English month names
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM uuuu")
            .toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMMM uuuu")
            .toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT),
        // Thai month names (normalize BE below)
        DateTimeFormatter.ofPattern("d MMM uuuu", TH).withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("d MMMM uuuu", TH).withResolverStyle(ResolverStyle.STRICT)
    );

    for (DateTimeFormatter f : candidates) {
      try {
        LocalDate d = LocalDate.from(f.parse(s));
        if (d.getYear() >= 2400) d = d.withYear(d.getYear() - 543);
        return d;
      } catch (Exception ignore) {}
    }
    return null;
  }

  private String formatOut(LocalDate date, String outputFormat) {
    String fmt = (outputFormat == null || outputFormat.isBlank())
        ? "TH_BE:d MMMM uuuu" : outputFormat;

    if (fmt.startsWith("TH_BE:")) {
      String pat = fmt.substring("TH_BE:".length());
      var be = ThaiBuddhistDate.from(date);
      return DateTimeFormatter.ofPattern(pat, TH).format(be);
    }
    if (fmt.startsWith("TH_CE:") || fmt.startsWith("TH:")) {
      String pat = fmt.substring(fmt.indexOf(':') + 1);
      return DateTimeFormatter.ofPattern(pat, TH).format(date);
    }
    if (fmt.startsWith("EN_CE:") || fmt.startsWith("EN:")) {
      String pat = fmt.substring("EN_CE:".length());
      return DateTimeFormatter.ofPattern(pat, Locale.ENGLISH).format(date);
    }
    return DateTimeFormatter.ofPattern(fmt).format(date); // default CE, JVM locale
  }

  private Locale rawLocaleGuess(String raw) {
    boolean hasThai = raw.codePoints().anyMatch(
        cp -> Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.THAI);
    return hasThai ? TH : Locale.ENGLISH;
  }
}
