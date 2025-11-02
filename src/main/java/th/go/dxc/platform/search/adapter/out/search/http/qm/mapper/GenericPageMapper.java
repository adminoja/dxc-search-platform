package th.go.dxc.platform.search.adapter.out.search.http.qm.mapper;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.catalog.model.Dataset.Route.Data;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

// adapter/out/qm/mapper/SpringPageMapper.java
@Slf4j
@Component("genericPage")
@RequiredArgsConstructor
public class GenericPageMapper implements QmResponseMapper {

  @Override
  public String id() {
    return "genericPage";
  }

  @Override
  public DomainPageResult<JsonNode> toPageResult(JsonNode body, DomainPageRequest req, Dataset ds) {
    log.trace("toPageResult: req={}, ds={}", req, ds);
    var content = new ArrayList<JsonNode>();
    Data data = ds.route().data();
    Integer page = 0;
    Integer size = req.pageSize();
    Long  total = 0L;
    String contentPointer = data.pointers().content();
    log.trace("contentPointer = {}", contentPointer);
    // JsonNode contentJsonNode = body.path(contentPointer);
    JsonNode contentJsonNode = body.at(contentPointer);
    log.debug("isArray={}", data.isArray());
    if (data.isArray()) {
      log.trace("add List contentJsonNode: {}", contentJsonNode);
      contentJsonNode.forEach(n -> content.add(n)); // helper below
      page = body.at(data.pointers().pageNumber()).asInt(req.pageNumber());
      size = body.at(data.pointers().pageSize()).asInt(req.pageSize());
      total = body.at(data.pointers().numberOfElements()).asLong(content.size());
    } else {
      log.trace("add Single Content: {}", contentJsonNode);
      page = 0;
      size = 1;
      total = 1L;
      content.add(contentJsonNode);
    }
    log.trace("content={}, page={}, size={}, total={}",content, page,size,total);
    return DomainPageResult.of(content, DomainPageRequest.of(page, size, req.sort()), total);
  }
}
