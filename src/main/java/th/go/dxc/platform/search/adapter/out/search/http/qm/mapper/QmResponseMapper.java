package th.go.dxc.platform.search.adapter.out.search.http.qm.mapper;
// adapter/out/qm/mapper/QmResponseMapper.java
import com.fasterxml.jackson.databind.JsonNode;

import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

public interface QmResponseMapper {
  /** A unique id that you will reference from dataset config (e.g., "springPage", "arrayTotal", "cursor"). */
  public String id();

  /** Convert arbitrary QM JSON into your unified page result. */
  // public DomainPageResult<Map<String,Object>> toPageResult(JsonNode body, DomainPageRequest req);
  public DomainPageResult<JsonNode> toPageResult(JsonNode body, DomainPageRequest req,Dataset dataset);
}
