// src/main/java/th/go/dxc/platform/search/adapter/in/http/api/mapper/SearchApiMapper.java
package th.go.dxc.platform.search.adapter.in.http.api.search.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import th.go.dxc.platform.search.adapter.in.http.api.search.dto.LocalSearchPageResultDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.LocalSearchRequestDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.LocalSearchResultDto;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainSort;
import th.go.dxc.platform.search.domain.search.model.LocalSearchRecord;
import th.go.dxc.platform.search.domain.search.model.LocalSearchRequest;
import th.go.dxc.platform.search.domain.search.model.LocalSearchResult;

/**
 * Maps API DTOs <-> domain types for Search.
 */
public final class LocalSearchApiMapper {

    private LocalSearchApiMapper() {
    }

    /* ===================== DTO -> DOMAIN ===================== */

    /** Build a domain SearchRequest from API dto + path variable datasetId. */
    public static LocalSearchRequest toDomain(String datasetId, LocalSearchRequestDto dto) {
        int page = dto.page() != null ? Math.max(0, dto.page()) : 0;
        int size = dto.size() != null ? Math.max(1, dto.size()) : DomainPageRequest.DEFAULT_SIZE;

        DomainSort sort = parseSort(dto.sort());
        Map<String, Object> criteria = dto.criteria() != null ? dto.criteria() : Map.of();

        DomainPageRequest pageReq = DomainPageRequest.of(page, size, sort.unsorted() ? null : sort);
        return new LocalSearchRequest(Instant.now(),new Dataset.Id(datasetId), criteria, pageReq);
    }

    /**
     * Parse sort strings like:
     * - "name" -> name ASC
     * - "-createdAt" -> createdAt DESC
     * - "updatedAt,desc" -> updatedAt DESC
     * - "name,ASC;createdAt,DESC" (multiple; also supports "|" as separator)
     */
    public static DomainSort parseSort(String sortSpec) {
        if (sortSpec == null || sortSpec.isBlank())
            return DomainSort.unsortedSort();

        String[] specs = sortSpec.split("[;|]");
        List<DomainSort.Order> orders = new ArrayList<>();

        for (String raw : specs) {
            String s = raw.trim();
            if (s.isEmpty())
                continue;

            DomainSort.Direction dir = DomainSort.Direction.ASC;
            String prop = s;

            // prefix style: -field / +field
            if (prop.startsWith("-")) {
                dir = DomainSort.Direction.DESC;
                prop = prop.substring(1).trim();
            } else if (prop.startsWith("+")) {
                prop = prop.substring(1).trim();
            } else {
                // delimiter style: field,ASC or field:DESC (case-insensitive)
                int comma = prop.indexOf(',');
                int colon = prop.indexOf(':');
                int sep = (comma >= 0 && colon >= 0) ? Math.min(comma, colon) : Math.max(comma, colon);
                if (sep >= 0) {
                    String field = prop.substring(0, sep).trim();
                    String d = prop.substring(sep + 1).trim().toUpperCase(Locale.ROOT);
                    if (!field.isEmpty()) {
                        prop = field;
                        dir = "DESC".equals(d) ? DomainSort.Direction.DESC : DomainSort.Direction.ASC;
                    }
                }
            }

            if (!prop.isEmpty()) {
                orders.add(DomainSort.Order.builder().property(prop).direction(dir).build());
            }
        }

        return orders.isEmpty() ? DomainSort.unsortedSort() : DomainSort.builder().orders(List.copyOf(orders)).build();
    }

    /* ===================== DOMAIN -> DTO ===================== */

    /**
     * Map domain SearchResult (runId + DomainPageResult<DataRecordSummary>) to API
     * SearchResultDto.
     * If you later want to expose runId to the UI, add a runId field in
     * SearchResultDto and set it here.
     */
    public static LocalSearchPageResultDto toPageResultDto(LocalSearchResult r) {
        var page = r.pageResult(); // DomainPageResult<DataRecordSummary>

        var items = page.content().stream()
                .map(LocalSearchApiMapper::toDtoItem)
                .toList();

        return LocalSearchPageResultDto.of(items,page.number(),page.size(),page.totalElements());
        
    }

    public static LocalSearchResultDto toDto(LocalSearchResult r) {
        var page = r.pageResult(); // DomainPageResult<DataRecordSummary>

        var items = page.content().stream()
                .map(LocalSearchApiMapper::toDtoItem)
                .toList();

        return LocalSearchResultDto.of(
                r.datasetId(),
                r.runId(),
                r.status(),
                LocalSearchPageResultDto.of(items,page.number(),page.size(),page.totalElements()),
                r.failure(),
                r.startedAt(),
                r.durationMs());
    }
    // private static LocalSearchPageResultDto.Item toDtoItem(LocalSearchRecord s) {
    //     return new LocalSearchPageResultDto.Item(
    //             s.reportToken(),
    //             s.data() // Map<String,Object>
    //     );
    // }
    private static LocalSearchPageResultDto.Item toDtoItem(LocalSearchRecord s) {
        return new LocalSearchPageResultDto.Item(
                s.reportToken().asString(),
                s.dataRecord().data() // Map<String,Object>
        );
    }

    
}


