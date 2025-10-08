package th.go.dxc.platform.search.domain.search.model;

import java.util.Map;
import java.util.Objects;

import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;

public record SearchRequest(
    Dataset.Id datasetId,
    Map<String,Object> criteria,
    DomainPageRequest pageRequest
) {
  public SearchRequest {
    Objects.requireNonNull(datasetId, "datasetId");
    criteria = (criteria == null) ? Map.of() : criteria;
    pageRequest = (pageRequest == null) ? DomainPageRequest.of(0, DomainPageRequest.DEFAULT_SIZE) : pageRequest;
  }
}
