package th.go.dxc.platform.search.adapter.out.search.http.qm.mapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

// adapter/out/qm/mapper/QmResponseMapperRegistry.java
@Component
public class QmMapperRegistry {
  private final Map<String, QmResponseMapper> byId;

  public QmMapperRegistry(List<QmResponseMapper> mappers) {
    this.byId = mappers.stream().collect(Collectors.toUnmodifiableMap(
        QmResponseMapper::id, Function.identity(), (a,b) -> a));
  }

  public QmResponseMapper getOrDefault(String id) {
    return byId.getOrDefault(
        (id == null || id.isBlank()) ? "springPage" : id,
        Optional.ofNullable(byId.get("springPage"))
            .orElseThrow(() -> new IllegalStateException("Missing default mapper 'springPage'")));
  }
}
