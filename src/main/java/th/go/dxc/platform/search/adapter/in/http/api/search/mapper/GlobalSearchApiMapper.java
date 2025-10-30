package th.go.dxc.platform.search.adapter.in.http.api.search.mapper;

// package th.go.dxc.platform.search.adapter.in.http.mapper;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import th.go.dxc.platform.search.adapter.in.http.api.search.dto.GlobalSearchRequestDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.GlobalSearchResultDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.LocalSearchRequestDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.LocalSearchResultDto;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRequest;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchResult;
import th.go.dxc.platform.search.domain.search.model.LocalSearchRequest;

public final class GlobalSearchApiMapper {

  private GlobalSearchApiMapper() {
  }

  public static GlobalSearchRequest toDomain(GlobalSearchRequestDto dto) {
    Objects.requireNonNull(dto, "dto");
    if (dto.requests() == null || dto.requests().isEmpty()) {
      throw new IllegalArgumentException("'requests' must not be empty");
    }

    var now = Instant.now();

    // Shared defaults (safe copies)
    LocalSearchRequestDto sharedPage = dto.sharedPageRequest();
    Map<String, Object> sharedCriteria = safeMap(sharedPage.criteria());

    // Build LocalSearchRequest list with merged defaults
    List<LocalSearchRequest> locals = dto.requests().stream().map(item -> {
      var effectiveCriteria = merge(sharedCriteria, item.pageRequest().criteria()); // shared -> item (item wins)
      LocalSearchRequestDto itemPage = item.pageRequest();
      LocalSearchRequestDto effectivePage = (itemPage != null) ? new LocalSearchRequestDto(effectiveCriteria, itemPage.page(),itemPage.size(),itemPage.sort()) : sharedPage;
      return LocalSearchApiMapper.toDomain(item.datasetId(),effectivePage);
    }).toList();

    var aggregation = parseEnum(dto.aggregation(), GlobalSearchRequest.Aggregation.NONE,
        GlobalSearchRequest.Aggregation::valueOf);
    var grouping = parseEnum(dto.grouping(), GlobalSearchRequest.Grouping.BY_DATASET,
        GlobalSearchRequest.Grouping::valueOf);
    var ordering = parseEnum(dto.ordering(), GlobalSearchRequest.Ordering.BY_TIME,
        GlobalSearchRequest.Ordering::valueOf);
    var timeout = Duration
        .ofMillis(dto.perDatasetTimeoutMs() == null ? 10_000 : Math.max(1, dto.perDatasetTimeoutMs()));
    var failFast = dto.failFast() != null && dto.failFast();

    // Pass sharedCriteria through because your domain model includes it
    return new GlobalSearchRequest(
        now,
        locals,
        aggregation,
        grouping,
        ordering,
        timeout,
        failFast,
        sharedCriteria);
  }



  public static GlobalSearchResultDto toDto(GlobalSearchResult domain) {
    Objects.requireNonNull(domain, "domain");

    List<LocalSearchResultDto> dtoItems = domain.results().stream()
        .map(LocalSearchApiMapper::toDto)
        .toList();

    return new GlobalSearchResultDto(
        domain.runId(),
        dtoItems,
        domain.startedAt(),
        domain.durationMs());
  }
  // ---- helpers ----


  private static Map<String, Object> merge(Map<String, Object> shared, Map<String, Object> item) {
    if (shared == null || shared.isEmpty())
      return safeMap(item);
    if (item == null || item.isEmpty())
      return Map.copyOf(shared);
    // preserve order deterministically
    var out = new LinkedHashMap<String, Object>(shared.size() + item.size());
    out.putAll(shared);
    out.putAll(item); // item overrides shared
    return Map.copyOf(out);
  }

  private static Map<String, Object> safeMap(Map<String, Object> m) {
    return (m == null) ? Map.of() : Map.copyOf(m);
  }

  private static <E> E parseEnum(String raw, E def, java.util.function.Function<String, E> f) {
    if (raw == null || raw.isBlank())
      return def;
    try {
      return f.apply(raw.trim().toUpperCase());
    } catch (Exception ignore) {
      return def;
    }
  }
}
