package th.go.dxc.platform.search.adapter.out.search.http.qm.mapper;

import java.util.ArrayList;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

// adapter/out/qm/mapper/SpringPageMapper.java
@Component("springPage")
public class SpringPageMapper implements QmResponseMapper {
  @Override public String id() { return "springPage"; }

  // @Override
  // public DomainPageResult<Map<String,Object>> toPageResult(JsonNode body, DomainPageRequest req) {
  //   var content = new ArrayList<Map<String,Object>>();
  //   body.path("content").forEach(n -> content.add(Json.toMap(n))); // helper below
  //   // int page = body.path("number").asInt(req.pageNumber());
  //   // int size = body.path("size").asInt(req.pageSize());
  //   long total = body.path("totalElements").asLong(content.size());
  //   return DomainPageResult.of(content, req, total);
  // }

    @Override
  public DomainPageResult<JsonNode> toPageResult(JsonNode body, DomainPageRequest req) {
    var content = new ArrayList<JsonNode>();
    body.path("content").forEach(n -> content.add(n)); // helper below
    // int page = body.path("number").asInt(req.pageNumber());
    // int size = body.path("size").asInt(req.pageSize());
    long total = body.path("totalElements").asLong(content.size());
    return DomainPageResult.of(content, req, total);
  }
}
