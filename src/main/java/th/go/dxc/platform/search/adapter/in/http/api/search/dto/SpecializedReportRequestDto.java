package th.go.dxc.platform.search.adapter.in.http.api.search.dto;

import java.time.Instant;
import java.util.Map;

import th.go.dxc.platform.search.adapter.in.http.api.common.api.dto.PageRequestDto;

public record SpecializedReportRequestDto(Instant requestedAt,
        String reportId,
        Map<String, Object> criteria,
        PageRequestDto pageRequest) {
    
}
