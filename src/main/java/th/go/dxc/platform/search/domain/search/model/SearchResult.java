package th.go.dxc.platform.search.domain.search.model;

import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

public record SearchResult(
    String runId,
    DomainPageResult<DataRecordSummary> pageResult
) {

}
