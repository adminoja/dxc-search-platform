package th.go.dxc.platform.search.adapter.in.http.api.search.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.GlobalSearchRequestDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.GlobalSearchResultDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.mapper.GlobalSearchApiMapper;
import th.go.dxc.platform.search.application.search.port.in.SearchGlobalSearchUseCase;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchStorePort;
import th.go.dxc.platform.search.domain.common.value.InvocationContext;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search/global")
public class GlobalSearchApi {
  private final SearchGlobalSearchUseCase runs;
  private final GlobalSearchStorePort store;

  @PostMapping
  public Mono<Map<String,Object>> create(@RequestBody GlobalSearchRequestDto dto,
    @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId,
    @AuthenticationPrincipal UserContext user) {
    String runId = UUID.randomUUID().toString();
    GlobalSearchRequest req = GlobalSearchApiMapper.toDomain(dto);
    InvocationContext invo = InvocationContext.userTopLevel(
            InvocationContext.FeatureType.GLOBAL_SEARCH,
            "GLOBAL_SEARCH",
            correlationId != null ? correlationId : "",
            runId);
    return runs.execute(new SearchGlobalSearchUseCase.Input( req, user,invo))
        .map(id -> Map.of("runId", id, "status", "QUEUED"));
  }


  
  @GetMapping("/{runId}")
  public Mono<Map<String, Object>> status(@PathVariable String runId) {
    return store.get(runId).map(st -> Map.of(
        "runId", st.runId,
        "status", st.status.name(),
        "requestedAt", st.requestedAt,
        "startedAt", st.startedAt,
        "updatedAt", st.updatedAt,
        "progress", st.progress(),
        "datasets", st.perDataset.values()));
  }

  @DeleteMapping("/{runId}")
  public Mono<Void> cancel(@PathVariable String runId) {
    return store.cancel(runId).then();
  }

  @GetMapping("/{runId}/result")
  public Mono<GlobalSearchResultDto> getResult(@PathVariable String runId) {
    return store.getResult(runId).map(GlobalSearchApiMapper::toDto);
  }
}
