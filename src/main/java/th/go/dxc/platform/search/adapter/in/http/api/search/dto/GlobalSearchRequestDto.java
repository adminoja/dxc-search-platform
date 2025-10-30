package th.go.dxc.platform.search.adapter.in.http.api.search.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotEmpty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GlobalSearchRequestDto(
        @NotEmpty List<DatasetSearchRequestDto> requests,

        // Shared defaults applied to every request (item-level overrides win)
        LocalSearchRequestDto sharedPageRequest,

        // Optional global knobs (strings = lenient at the edge)
        String aggregation, // NONE|UNION|INTERSECT
        String grouping, // BY_DATASET|BY_DOMAIN
        String ordering, // BY_TIME|BY_SCORE
        Long perDatasetTimeoutMs,
        Boolean failFast) {
    public record DatasetSearchRequestDto(
            String datasetId,
            LocalSearchRequestDto pageRequest) {
    }
}
