package th.go.dxc.platform.search.domain.audit.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.*;

/**
 * Audited actions. Start small; add only when you truly need distinct analytics.
 * If you expect many domain-specific actions later, keep the enum lean and
 * store domain details in AuditEvent.detailsJson.
 */
public enum Action {

  // ---- Security/session actions ----
  LOGIN("LOGIN"),
  LOGOUT("LOGOUT"),
  ACCESS_DENIED("ACCESS_DENIED"),

  // ---- Search & data access ----
  SEARCH_QUERY("SEARCH_QUERY"),
  DATA_READ("DATA_READ"),

  // ---- Report lifecycle ----
  REPORT_GENERATE("REPORT_GENERATE"),
  REPORT_DOWNLOAD("REPORT_DOWNLOAD"),

  // ---- Data export (CSV/XLS/PDF outside report templates) ----
  DATA_EXPORT("DATA_EXPORT");

  private final String code;

  Action(String code) {
    this.code = code;
  }

  @JsonValue
  public String code() {
    return code;
  }

  // ---------- Parsing with aliases ----------

  private static final Map<String, Action> LOOKUP;
  static {
    Map<String, Action> m = new HashMap<>();
    // canonical names
    for (Action a : values()) {
      m.put(norm(a.code), a);
      m.put(norm(a.name()), a);
    }
    // aliases
    alias(m, SEARCH_QUERY, "SEARCH", "SEARCH_EXECUTE", "QUERY");
    alias(m, DATA_READ, "READ", "VIEW");
    alias(m, DATA_EXPORT, "EXPORT", "DOWNLOAD_DATA");
    alias(m, REPORT_GENERATE, "REPORT_CREATE", "REPORT_PRINT");
    alias(m, REPORT_DOWNLOAD, "DOWNLOAD_REPORT");
    alias(m, ACCESS_DENIED, "FORBIDDEN");
    LOOKUP = Collections.unmodifiableMap(m);
  }

  @JsonCreator
  public static Action from(String v) {
    if (v == null || v.isBlank()) return null;
    return LOOKUP.getOrDefault(norm(v), null);
  }

  private static void alias(Map<String, Action> map, Action a, String... names) {
    for (String n : names) {
      map.put(norm(n), a);
    }
  }

  private static String norm(String s) {
    return s.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
  }
}
