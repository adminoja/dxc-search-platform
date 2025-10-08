package th.go.dxc.platform.search.adapter.out.search.http.qm.mapper;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// adapter/out/qm/mapper/Json.java
final class Json {
  static Map<String,Object> toMap(JsonNode node) {
    return new ObjectMapper().convertValue(node, new TypeReference<Map<String,Object>>(){});
  }
  private Json() {}
}
