// src/main/java/th/go/dxc/platform/search/adapter/out/search/http/qm/QmSearchHttpAdapter.java
package th.go.dxc.platform.search.adapter.out.search.http.qm;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.adapter.out.catalog.config.CatalogConfigRepository;
import th.go.dxc.platform.search.adapter.out.search.http.qm.client.QmSearchHttpClient;
import th.go.dxc.platform.search.adapter.out.search.http.qm.mapper.QmResponseMapper;
import th.go.dxc.platform.search.application.search.port.out.QmSearchPort;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.catalog.model.DatasetRoute;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.search.model.DataRecord;
import th.go.dxc.platform.search.domain.search.model.SearchRequest;

/**
 * Outbound HTTP adapter that calls Query Manager (QM).
 * Supports route headers with placeholders, e.g.:
 * X-User-Nin: "{user.nin}"
 * X-Citizen-Id: "{citizen_id}"
 * X-Nested: "{criteria.person.citizen_id}" or simply "{person.citizen_id}"
 *
 * Placeholders resolution order:
 * 1) user.* -> from UserContext (e.g., user.nin, user.tenantId, user.username,
 * etc.)
 * 2) criteria.* or plain keys -> from request.criteria() map (dot-path
 * supported)
 *
 * Assumes a load-balanced WebClient so "http://{serviceId}" resolves via
 * discovery/DNS.
 */
@AllArgsConstructor
@Component
public class QmSearchHttpAdapter implements QmSearchPort {
  private final CatalogConfigRepository catalog;
  private final QmSearchHttpClient http;
  private final Map<String, QmResponseMapper> mapperRegistry;
  private final QmResponseMapper defaultMapper;

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

  @Override
  public Mono<DomainPageResult<DataRecord>> search(
      DatasetRoute route,
      SearchRequest request,
      UserContext userContext) {

    // sync config lookup (defensive: we use the id present in the request)
    Dataset ds = catalog.findDatasetById(request.datasetId())
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown datasetId: " + request.datasetId().value()));

    var mapper = mapperRegistry.getOrDefault("springPage", defaultMapper);

    // Build headers with template resolution
    HttpHeaders headers = resolveHeaders(route.headers(), request, userContext);

    // http.invoke returns Mono<JsonNode>
    return http.invoke(ds, request.criteria(), request.pageRequest(), headers)
        // mapper is sync -> use map
        .map(body -> mapper.toPageResult(body, request.pageRequest())) // DomainPageResult<Map<String,Object>>
        // convert page content Map<String,Object> -> DataRecord
        .map(this::mapPageToDataRecord);
  }

  // ---- helpers ----

  private DomainPageResult<DataRecord> mapPageToDataRecord(
      DomainPageResult<Map<String, Object>> src) {
    List<DataRecord> items = src.content().stream()
        .map(this::toDataRecord)
        .toList();

    return DomainPageResult.of(
        items,
        DomainPageRequest.of(src.number(), src.size()),
        src.totalElements()
    // If you add sort later: , src.getSort()
    );
  }

  private DataRecord toDataRecord(Map<String, Object> row) {
    // Adapt to your actual DataRecord implementation
    return new DataRecord(row);
  }

  /**
   * Resolve a map of header templates into concrete HttpHeaders.
   * Supports values like "{user.nin}" and "{citizen_id}".
   */
  private HttpHeaders resolveHeaders(Map<String, String> headerTemplates,
      SearchRequest request,
      UserContext userContext) {
    HttpHeaders headers = new HttpHeaders();
    if (headerTemplates == null || headerTemplates.isEmpty()) {
      return headers;
    }

    for (Map.Entry<String, String> e : headerTemplates.entrySet()) {
      String name = e.getKey();
      String template = e.getValue();
      if (name == null || name.isBlank() || template == null)
        continue;

      String resolved = resolveTemplate(template, request, userContext);
      // Only add non-null, non-empty values
      if (resolved != null && !resolved.isBlank()) {
        headers.add(name, resolved);
      }
    }
    return headers;
  }

  /**
   * Resolve a single template string with placeholders like {user.nin} or
   * {citizen_id}.
   * If a placeholder cannot be resolved, it is kept as-is (so you notice
   * misconfig).
   */
  private String resolveTemplate(String template,
      SearchRequest request,
      UserContext userContext) {
    Matcher m = PLACEHOLDER.matcher(template);
    StringBuffer sb = new StringBuffer(template.length());
    while (m.find()) {
      String key = m.group(1).trim(); // e.g. "user.nin" or "citizen_id" or "person.citizen_id"
      Object value = null;

      if (key.startsWith("user.")) {
        value = extractFromUser(userContext, key.substring("user.".length()));
      } else if (key.startsWith("criteria.")) {
        value = extractFromCriteria(request.criteria(), key.substring("criteria.".length()));
      } else {
        // Try criteria first for plain keys
        value = extractFromCriteria(request.criteria(), key);
        // (No implicit fallback to user for plain keys to avoid surprises)
      }

      String replacement = value == null ? m.group(0) : Objects.toString(value);
      // Avoid backrefs issue in appendReplacement by quoting replacement
      m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    m.appendTail(sb);
    return sb.toString();
  }

  /**
   * Extract a value from UserContext by a simple dot path (nin, tenantId,
   * username, roles[0], etc.)
   * This method is defensive: it tries common getters and, if available,
   * an attributes() map. Adjust as needed if your UserContext changes.
   */
  private Object extractFromUser(UserContext userContext, String path) {
    if (userContext == null || path == null || path.isBlank())
      return null;
    String p = path.trim();

    // Common, explicit known fields first (avoid reflection):
    // Adjust these to your actual UserContext API
    try {
      switch (p) {
        case "nin" -> {
          try {
            return userContext.nin();
          } catch (Throwable ignored) {
          }
        }
        case "tenantId" -> {
          try {
            return userContext.tenantId();
          } catch (Throwable ignored) {
          }
        }
        case "username" -> {
          try {
            return userContext.username();
          } catch (Throwable ignored) {
          }
        }
        case "userId" -> {
          try {
            return userContext.userId();
          } catch (Throwable ignored) {
          }
        }
        default -> {
          // Fall through to attributes map or nested map
        }
      }
    } catch (Throwable ignored) {
      /* continue attempts */ }

    // If you keep arbitrary attributes:
    try {
      var attrsMethod = userContext.getClass().getMethod("attributes");
      Object attrsObj = attrsMethod.invoke(userContext);
      if (attrsObj instanceof Map<?, ?> m) {
        @SuppressWarnings("unchecked")
        Map<String, Object> attrs = (Map<String, Object>) m;
        return getByDotPath(attrs, p);
      }
    } catch (Throwable ignored) {
      /* no attributes() or inaccessible */ }

    return null;
  }

  /**
   * Extract a value from criteria map using dot-paths.
   */
  private Object extractFromCriteria(Map<String, Object> criteria, String path) {
    if (criteria == null || path == null || path.isBlank())
      return null;
    return getByDotPath(criteria, path.trim());
  }

  /**
   * Dot-path lookup into nested Map/List structures.
   * Supports array indices like "items.0.id".
   */
  private Object getByDotPath(Object root, String path) {
    if (root == null || path == null || path.isBlank())
      return null;
    String[] parts = path.split("\\.");
    Object cur = root;

    for (String part : parts) {
      if (cur == null)
        return null;

      if (cur instanceof Map<?, ?> map) {
        cur = map.get(part);
      } else if (cur instanceof List<?> list) {
        try {
          int idx = Integer.parseInt(part);
          cur = (idx >= 0 && idx < list.size()) ? list.get(idx) : null;
        } catch (NumberFormatException nfe) {
          return null;
        }
      } else {
        // Non-container encountered before path end
        return null;
      }
    }
    return cur;
  }
}
