package th.go.dxc.platform.search.domain.catalog.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Dataset(
        Dataset.Id id,
        String name,
        String description,
        Organization.Id ownerOrgId,
        Route route,
        FieldMapping mapping,
        Map<String, Map<String, FieldRule>> domains) {
    public Dataset {
        id = Objects.requireNonNull(id, "dataset.id");
        name = nonBlankOrNull(name);
        description = defaultIfBlank(description, name);
        ownerOrgId = Organization.Id.of("unknown"); // optional? keep as-is or requireNonNull if mandatory
        route = Objects.requireNonNull(route, "dataset.route");
        mapping = mapping == null ? FieldMapping.of(List.of(), Map.of(), List.of(), List.of()) : mapping;
        // Default to empty immutable map to avoid NPE/ConcurrentModification
        domains = domains == null ? Map.of() : deepCopy2(domains);
    }

    public record Id(String value) {
        public Id {
            value = Objects.requireNonNull(value, "dataset.id.value").trim();
        }

        public static Id of(String value) {
            return new Id(value);
        }
    }

    public record Route(
            String path,
            String serviceId,
            Map<String, String> headers,
            Map<String, String> querys,
            Data data) {
        public Route {
            path = Objects.requireNonNull(path, "route.path").trim();
            serviceId = nonBlankOrNull(serviceId); // allow null if not used
            headers = headers == null ? Map.of() : Map.copyOf(headers);
            querys = querys == null ? Map.of() : Map.copyOf(querys);
            data = data == null ? Data.of(null, null) : data;
        }

        public static Route of(String path, String serviceId, Map<String, String> headers, Map<String, String> querys,
                Data data) {
            return new Route(path, serviceId, headers, querys, data);
        }

        public record Data(
                Pointer pointers,
                Boolean isArray) {
            public Data {
                pointers = pointers == null ? Pointer.of() : pointers;
                // Prefer primitive boolean; if you must keep Boolean, normalize:
                isArray = isArray == null ? true : isArray;
            }

            public static Data of(Pointer pointers, Boolean isArray) {
                return new Data(pointers, isArray);
            }

            public record Pointer(
                    String content,
                    String pageNumber,
                    String pageSize,
                    String numberOfElements,
                    String totalElements) {
                public Pointer {
                    // For pointers, either require non-null or set safe defaults
                    content = normalizeJsonPointer(defaultIfBlank(content, "/content"));
                    pageNumber = normalizeJsonPointer(defaultIfBlank(pageNumber, "/page")); // customize your defaults
                    pageSize = normalizeJsonPointer(defaultIfBlank(pageSize, "/size"));
                    numberOfElements = normalizeJsonPointer(defaultIfBlank(numberOfElements, "/numberOfElements"));
                    totalElements = normalizeJsonPointer(defaultIfBlank(totalElements, "/totalElements"));
                }

                public static Pointer of() {
                    return new Pointer(null, null, null, null, null);
                }

                public static Pointer of(String content, String pageNumber, String pageSize,
                        String numberOfElements, String totalElements) {
                    return new Pointer(content, pageNumber, pageSize, numberOfElements, totalElements);
                }
            }
        }
    }

    public record FieldMapping(
            List<String> searchFields,
            Map<String, String> canonicalSearchFields,
            List<String> summaryFields,
            List<String> naturalKeyFields) {
        public FieldMapping {
            searchFields = copyListOrEmpty(searchFields);
            canonicalSearchFields = canonicalSearchFields == null ? Map.of() : Map.copyOf(canonicalSearchFields);
            summaryFields = copyListOrEmpty(summaryFields);
            naturalKeyFields = copyListOrEmpty(naturalKeyFields);
        }

        public static FieldMapping of(List<String> searchFields,
                Map<String, String> canonicalSearchFields,
                List<String> summaryFields,
                List<String> naturalKeyFields) {
            return new FieldMapping(searchFields, canonicalSearchFields, summaryFields, naturalKeyFields);
        }
    }

    public record FieldRule(
            String pointer, // "/province_id"
            List<String> coalesce, // ["/prov_code","/province_code"]
            String compose, // "${/house_no} ${/road} จ.${/province_name}"
            List<TransformRule> transform // [{type:toDate}, {type:trim}]
    ) {
        public FieldRule {
            pointer = nonBlankOrNull(pointer); // allow null if using coalesce/compose instead
            coalesce = copyListOrEmpty(coalesce);
            compose = nonBlankOrNull(compose);
            transform = transform == null ? List.of() : List.copyOf(transform);
        }

        public static FieldRule of(String pointer, List<String> coalesce, String compose,
                List<TransformRule> transform) {
            return new FieldRule(pointer, coalesce, compose, transform);
        }

        public record TransformRule(
                TransformType type,
                List<String> args) {
            public TransformRule {
                type = Objects.requireNonNull(type, "fieldRule.transform.type");
                args = args == null ? List.of() : List.copyOf(args);
            }
        }

        public enum TransformType {
            toDate, trim, squashWhitespace, padLeft, toDecimal, mapCode, upper, lower
        }
    }

    // ---------- small helpers ----------
    private static String nonBlankOrNull(String s) {
        return s == null ? null : (s.isBlank() ? null : s.trim());
    }

    private static String defaultIfBlank(String s, String d) {
        return (s == null || s.isBlank()) ? d : s;
    }

    private static List<String> copyListOrEmpty(List<String> in) {
        return in == null ? List.of() : List.copyOf(in);
    }

    private static Map<String, Map<String, FieldRule>> deepCopy2(Map<String, Map<String, FieldRule>> in) {
        if (in == null || in.isEmpty())
            return Map.of();
        return in.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        e -> e.getKey(),
                        e -> e.getValue() == null ? Map.of() : Map.copyOf(e.getValue())));
    }

    /**
     * Convert various path flavors (field, dot, JSONPath, pointer) into a strict
     * RFC-6901 JSON Pointer.
     */
    private static String normalizeJsonPointer(String raw) {
        if (raw == null)
            return "/";
        String s = raw.trim();
        if (s.isEmpty())
            return "/";

        // 1) Fragment form "#/a/b" -> "/a/b"
        if (s.startsWith("#/")) {
            return canonicalizePointer(s.substring(1));
        }

        // 2) Already a JSON Pointer "/a/b" (tolerate accidental multiple slashes &
        // trailing slash)
        if (s.startsWith("/")) {
            return canonicalizePointer(s);
        }

        // 3) JSONPath "$...." -> tokenize JSONPath then build pointer
        if (s.startsWith("$")) {
            List<String> tokens = tokenizeJsonPath(s);
            return buildPointer(tokens);
        }

        // 4) Dot / bracket hybrid like "a.b[0].c" or "a['b/c']"
        if (looksLikeDotOrBracketPath(s)) {
            List<String> tokens = tokenizeDotBracketPath(s);
            return buildPointer(tokens);
        }

        // 5) Plain single field name
        return buildPointer(List.of(s));
    }

    /**
     * Append a child path (any flavor) to a base JSON Pointer, returning a
     * normalized JSON Pointer.
     */
    // private static String append(String basePointer, String childPath) {
    // String base = canonicalizePointer(
    // basePointer == null || basePointer.isBlank() ? "/" : basePointer.trim());
    // String child = normalizeJsonPointer(childPath);
    // if ("/".equals(base)) return child;
    // if ("/".equals(child)) return base;
    // // strip leading slash from child and concatenate
    // return base + (child.startsWith("/") ? child : "/" + child);
    // }

    // -------- internals --------

    private static String canonicalizePointer(String p) {
        // collapse multiple slashes, remove trailing slash (except root), keep empty
        // tokens out
        String s = p;
        // strip fragment symbol if someone passed "#/.." by mistake
        if (s.startsWith("#/"))
            s = s.substring(1);
        // ensure starts with "/"
        if (!s.startsWith("/"))
            s = "/" + s;
        // split and re-join to eliminate duplicate slashes
        String[] parts = s.split("/+");
        List<String> toks = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) { // skip leading empty before first slash
            if (!parts[i].isEmpty()) {
                toks.add(escapeToken(unescapePointerToken(parts[i]))); // normalize escapes
            }
        }
        return buildPointer(toks);
    }

    private static String buildPointer(List<String> tokens) {
        if (tokens == null || tokens.isEmpty())
            return "/";
        StringBuilder sb = new StringBuilder();
        for (String t : tokens) {
            sb.append('/').append(escapeToken(t));
        }
        return sb.toString();
    }

    /** RFC-6901 escape: "~" -> "~0", "/" -> "~1" */
    private static String escapeToken(String t) {
        if (t == null)
            return "";
        return t.replace("~", "~0").replace("/", "~1");
    }

    /** Best-effort unescape if someone fed already-escaped parts (idempotent). */
    private static String unescapePointerToken(String t) {
        if (t == null)
            return "";
        // order matters: "~1" -> "/", then "~0" -> "~"
        return t.replace("~1", "/").replace("~0", "~");
    }

    private static boolean looksLikeDotOrBracketPath(String s) {
        // Heuristics: has dot not at ends, or bracket characters
        return s.contains(".") || (s.indexOf('[') >= 0 && s.indexOf(']') > s.indexOf('['));
    }

    /** Tokenize JSONPath like $.a.b[0]['c/d'] into ["a","b","0","c/d"] */
    private static List<String> tokenizeJsonPath(String s) {
        // Strip leading "$" or "$."
        int i = 0;
        if (s.charAt(0) == '$')
            i++;
        if (i < s.length() && s.charAt(i) == '.')
            i++;

        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();

        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '.') {
                if (cur.length() > 0) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                }
                i++;
            } else if (c == '[') {
                // flush current
                if (cur.length() > 0) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                }
                i++;
                // bracket expression: [123] or ['name'] or ["name"]
                if (i < s.length() && (s.charAt(i) == '\'' || s.charAt(i) == '"')) {
                    char quote = s.charAt(i++);
                    StringBuilder q = new StringBuilder();
                    while (i < s.length()) {
                        char ch = s.charAt(i++);
                        if (ch == '\\' && i < s.length()) {
                            q.append(s.charAt(i++)); // simple escape
                        } else if (ch == quote) {
                            break;
                        } else {
                            q.append(ch);
                        }
                    }
                    tokens.add(q.toString());
                    // skip optional ]
                    while (i < s.length() && s.charAt(i) != ']')
                        i++;
                    if (i < s.length() && s.charAt(i) == ']')
                        i++;
                } else {
                    // numeric index
                    StringBuilder num = new StringBuilder();
                    while (i < s.length() && Character.isDigit(s.charAt(i))) {
                        num.append(s.charAt(i++));
                    }
                    tokens.add(num.toString());
                    // skip to ]
                    while (i < s.length() && s.charAt(i) != ']')
                        i++;
                    if (i < s.length() && s.charAt(i) == ']')
                        i++;
                }
            } else {
                cur.append(c);
                i++;
            }
        }
        if (cur.length() > 0)
            tokens.add(cur.toString());
        // Clean tokens: trim and drop empties
        return tokens.stream().filter(t -> t != null && !t.isEmpty()).toList();
    }

    /** Tokenize dot/bracket hybrid: a.b[0].c a['x/y'] a["b~c"] */
    private static List<String> tokenizeDotBracketPath(String s) {
        // Reuse JSONPath tokenizer by pretending it starts with "$."
        return tokenizeJsonPath(s.startsWith("$.") || s.startsWith("$[") ? s : "$." + s);
    }
}
