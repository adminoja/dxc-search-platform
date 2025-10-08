package th.go.dxc.platform.search.adapter.in.http.api.search.dto;

import java.util.Map;

public record SearchRequestDto(
        Map<String, Object> criteria, // free-form filters from UI
        Integer page, // 0-based; default 0
        Integer size, // page size; default 20
        String sort // e.g. "field,ASC" or "field,DESC"; optional
) {

}
