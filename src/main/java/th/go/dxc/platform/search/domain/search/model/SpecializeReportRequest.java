package th.go.dxc.platform.search.domain.search.model;

import java.util.Map;

public record SpecializeReportRequest(
    SpecializedReport.Id reportId,
    Map<String, Object> parameters
) {

}
