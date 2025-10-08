package th.go.dxc.platform.search.adapter.in.http.api.common.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CachedPageDto<T>(
        List<T> content,
        PageableDto pageable,
        int totalPages,
        long totalElements,
        boolean last,
        int number,
        int size,
        int numberOfElements,
        SortDto sort,
        boolean first,
        boolean empty
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PageableDto(
            SortDto sort, int pageSize, int pageNumber, long offset, boolean paged, boolean unpaged
    ) {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SortDto(boolean sorted, boolean unsorted, boolean empty) {}
}
