package th.go.dxc.platform.search.domain.search.model;

import java.time.Instant;
import java.util.Map;

import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;

public record SpecializedReportRequest(
    Instant requestedAt,
    SpecializedReport.Id reportId,
    Map<String,Object> criteria,
    DomainPageRequest pageRequest
) {
    public static SpecializedReportRequest of(
            Instant requestedAt,
    SpecializedReport.Id reportId,
    Map<String,Object> criteria,
    DomainPageRequest pageRequest
    ){
        return new SpecializedReportRequest(requestedAt, reportId, criteria, pageRequest);
    }

}
