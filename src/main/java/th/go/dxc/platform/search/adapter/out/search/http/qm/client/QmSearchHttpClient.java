package th.go.dxc.platform.search.adapter.out.search.http.qm.client;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;

@Component
public class QmSearchHttpClient {

  private static final Set<String> HOP_BY_HOP = Set.of(
      "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
      "te", "trailer", "transfer-encoding", "upgrade", "host", "content-length"
  );

  private final WebClient client;   // should be load-balanced (Spring Cloud)
  private final ObjectMapper objectMapper;

  public QmSearchHttpClient(@LoadBalanced WebClient.Builder builder,
                            ObjectMapper objectMapper) {
    this.client = builder.build();        // <- load-balancer aware
    this.objectMapper = objectMapper;
  }

  public Mono<JsonNode> invoke(
      Dataset ds,
      Map<String, Object> filters,
      DomainPageRequest req,
      HttpHeaders headers // <-- now HttpHeaders
  ) {
    var route = ds.route();

    // 1) Collect inputs
    Map<String, Object> params = (filters == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(filters);

    // 2) Extract required path variables from the route template
    UriTemplate template = new UriTemplate(route.path());          // e.g. "/api/v1/persons/{citizen_id}"
    var requiredVars = template.getVariableNames();                // e.g. ["citizen_id"]

    Map<String, Object> pathVars = new LinkedHashMap<>();
    for (String v : requiredVars) {
      Object val = params.remove(v);
      if (val == null || (val instanceof String s && s.isBlank())) {
        return Mono.error(new IllegalArgumentException(
            "Missing required path variable '" + v + "' for dataset " + ds.id().value()));
      }
      pathVars.put(v, val);
    }

    // 3) Expand the path
    String expandedPath = template.expand(pathVars).toString();

    // 4) Build the lb:// URI and attach remaining filters as query params
    //    NOTE: This requires Spring Cloud's LoadBalancerExchangeFilterFunction configured for WebClient.
    UriComponentsBuilder ub = UriComponentsBuilder
        .fromUriString("lb://" + route.serviceId())
        .path(expandedPath);

    // leftover filters => query params
    params.forEach((k, v) -> {
      if (v instanceof Iterable<?> it) {
        it.forEach(item -> ub.queryParam(k, item));
      } else if (v != null && v.getClass().isArray()) {
        Object[] arr = (Object[]) v;
        for (Object item : arr) ub.queryParam(k, item);
      } else if (v != null) {
        ub.queryParam(k, v);
      }
    });

    // paging (and sort if you have it)
    ub.queryParam("page", req.pageNumber());
    ub.queryParam("size", req.pageSize());
    if (req.sort() != null && !req.sort().unsorted()) {
      // e.g. "field,ASC;createdAt,DESC"
      ub.queryParam("sort", req.sort());
    }

    var uri = ub.build(true).toUri();

    // Prepare outbound headers: copy, sanitize, and ensure a correlation id
    HttpHeaders outbound = copyHeaders(headers);
    sanitizeHopByHop(outbound);
    ensureCorrelationId(outbound);

    // 5) Reactive HTTP call (no blocking) + robust error handling + timeout
    return client
        .get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .headers(h -> h.addAll(outbound)) // <-- apply sanitized headers
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, resp ->
            resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new IllegalArgumentException(
                    "QM 4xx for dataset " + ds.id().value() + ": " + resp.statusCode() + " " + body)))
        .onStatus(HttpStatusCode::is5xxServerError, resp ->
            resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new IllegalStateException(
                    "QM 5xx for dataset " + ds.id().value() + ": " + resp.statusCode() + " " + body)))
        .bodyToMono(String.class)
        .timeout(Duration.ofSeconds(60))
        .map(body -> {
          try {
            return objectMapper.readTree(body);
          } catch (IOException e) {
            throw new IllegalStateException(
                "QM returned invalid JSON for dataset " + ds.id().value(), e);
          }
        });
  }

  private static HttpHeaders copyHeaders(HttpHeaders in) {
    HttpHeaders out = new HttpHeaders();
    if (in != null && !in.isEmpty()) {
      out.addAll(in);
    }
    return out;
  }

  private static void sanitizeHopByHop(HttpHeaders headers) {
    if (headers == null || headers.isEmpty()) return;
    // HttpHeaders is case-insensitive; removing lower-case names is enough.
    HOP_BY_HOP.forEach(headers::remove);
  }

  private static void ensureCorrelationId(HttpHeaders headers) {
    if (headers == null) return;
    if (!headers.containsKey("X-Request-Id") && !headers.containsKey("X-Correlation-Id")) {
      headers.add("X-Request-Id", UUID.randomUUID().toString());
    }
  }
}
