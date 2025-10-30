package th.go.dxc.platform.search.adapter.in.http.api.search.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.GlobalSearchResultDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.SpecializedReportRequestDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.SpecializedReportResultDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.mapper.GlobalSearchApiMapper;
import th.go.dxc.platform.search.adapter.in.http.api.search.mapper.SpecializedReportApiMapper;
import th.go.dxc.platform.search.application.search.port.in.GetSpecializedReportResultUseCase;
import th.go.dxc.platform.search.application.search.port.in.SearchSpecializedReportUseCase;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchStorePort;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.InvocationContext;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.search.model.SpecializedReport;
import th.go.dxc.platform.search.domain.search.model.SpecializedReportRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search/special")
public class SpecializedReportApi {
  private final SearchSpecializedReportUseCase runs;
  private final GetSpecializedReportResultUseCase getResult;
  private final GlobalSearchStorePort store;

  @PostMapping
  public Mono<Map<String, Object>> create(@RequestBody SpecializedReportRequestDto dto,
      @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId,
      @AuthenticationPrincipal UserContext user) {
    String runId = UUID.randomUUID().toString();
    SpecializedReportRequest req = SpecializedReportApiMapper.toDomain(dto);
    InvocationContext invo = InvocationContext.userTopLevel(
        InvocationContext.FeatureType.SPECIALIZED_REPORT,
        dto.reportId(),
        correlationId != null ? correlationId : "",
        runId);
    return runs.execute(new SearchSpecializedReportUseCase.Input(req, user, invo))
        .map(id -> Map.of("runId", id, "status", "QUEUED"));
  }

  @GetMapping("/reports/{reportId}")
  public Mono<Map<String, Object>> createByPreset(@PathVariable String reportId,
      @RequestParam Map<String, String> params,
      @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId,
      @AuthenticationPrincipal UserContext user) {
    String runId = UUID.randomUUID().toString();
    Map<String,Object> criteria = params==null?Map.of():new LinkedHashMap<>(params);
    Integer page = params!=null && params.containsKey("page")?Integer.valueOf(params.get("page")):0;
    Integer size = params!=null && params.containsKey("size")?Integer.valueOf(params.get("size")):5;
    SpecializedReportRequest req = SpecializedReportRequest.of(Instant.now(), SpecializedReport.Id.of(reportId),
        criteria, DomainPageRequest.of(page, size));
    InvocationContext invo = InvocationContext.userTopLevel(
        InvocationContext.FeatureType.SPECIALIZED_REPORT,
        reportId,
        correlationId != null ? correlationId : "",
        runId);
    return runs.execute(new SearchSpecializedReportUseCase.Input(req, user, invo))
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

  @GetMapping("/{runId}/result")
  public Mono<SpecializedReportResultDto> getResult(@PathVariable String runId) {
    return getResult.execute(GetSpecializedReportResultUseCase.Input.of(runId)).map(SpecializedReportApiMapper::toDto);
  }

  @GetMapping("/{runId}/result/raw")
  public Mono<GlobalSearchResultDto> getResultRaw(@PathVariable String runId) {
    return store.getResult(runId).map(GlobalSearchApiMapper::toDto);
  }
}
