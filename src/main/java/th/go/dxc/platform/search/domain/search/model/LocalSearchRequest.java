package th.go.dxc.platform.search.domain.search.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;

public record LocalSearchRequest(
    Instant requestedAt,
    Dataset.Id datasetId,
    Map<String, Object> criteria,
    DomainPageRequest pageRequest) {
  public LocalSearchRequest {
    Objects.requireNonNull(requestedAt, "requestedAt");
    Objects.requireNonNull(datasetId, "datasetId");
    criteria = (criteria == null) ? Map.of() : criteria;
    pageRequest = (pageRequest == null) ? DomainPageRequest.of(0, DomainPageRequest.DEFAULT_SIZE) : pageRequest;
  }

  public static LocalSearchRequest of(Instant requestedAt,
      Dataset.Id datasetId,
      Map<String, Object> criteria,
      DomainPageRequest pageRequest) {
    return new LocalSearchRequest(requestedAt, datasetId, criteria, pageRequest);
  }
}
