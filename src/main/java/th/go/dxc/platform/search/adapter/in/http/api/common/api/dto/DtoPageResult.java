// adapter/in/http/api/shared/paging/DtoPageResult.java
package th.go.dxc.platform.search.adapter.in.http.api.common.api.dto;

import java.util.List;

public record DtoPageResult<T>(
    List<T> content,
    int page,                // 0-based
    int size,
    long totalElements,
    int totalPages,
    int numberOfElements,
    boolean hasNext,
    boolean hasPrevious,
    boolean first,
    boolean last,
    boolean empty
) {}
