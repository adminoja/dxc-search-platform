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
import th.go.dxc.platform.search.application.search.port.in.CancelSpecializedRunUseCase;
import th.go.dxc.platform.search.application.search.port.in.GetSpecializedReportResultUseCase;
import th.go.dxc.platform.search.application.search.port.in.GetSpecializedRunUseCase;
import th.go.dxc.platform.search.application.search.port.in.ListSpecializedRunsUseCase;
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



  private final ListSpecializedRunsUseCase listRuns;
  private final GetSpecializedRunUseCase getRun;
  private final CancelSpecializedRunUseCase cancelRun;

  // GET /api/report/specialized/runs?limit=50&offset=0&status=COMPLETED&reportId=...&q=3102*
  @GetMapping("/runs")
  public Mono<ListSpecializedRunsUseCase.Result> listRuns(
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String reportId,
      @RequestParam(required = false, name = "q") String query,
     @AuthenticationPrincipal UserContext user
  ) {
    // adapt how you get the userId from auth
    String userId = user.userId();
    return listRuns.execute(ListSpecializedRunsUseCase.Input.of(userId, limit, offset, status, reportId, query));
  }

  // GET /api/report/specialized/runs/{runId}
  @GetMapping("/runs/{runId}")
  public Mono<ListSpecializedRunsUseCase.RunRow> getRun(@PathVariable String runId) {
    return getRun.execute(GetSpecializedRunUseCase.Input.of(runId));
  }

  // POST /api/report/specialized/runs/{runId}:cancel
  @PostMapping("/runs/{runId}:cancel")
  public Mono<Boolean> cancel(@PathVariable String runId) {
    return cancelRun.execute(CancelSpecializedRunUseCase.Input.of(runId));
  }


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
