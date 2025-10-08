package th.go.dxc.platform.search.adapter.in.http.api.report.controller;

import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.adapter.in.http.api.common.api.mapper.RequestContextMapper;
import th.go.dxc.platform.search.adapter.in.security.mapper.TokenContextMapper;
import th.go.dxc.platform.search.adapter.in.security.mapper.UserContextMapper;
import th.go.dxc.platform.search.application.report.port.in.GeneratePdfFromReportIdUseCase;
import th.go.dxc.platform.search.domain.common.value.RequestContext;
import th.go.dxc.platform.search.domain.common.value.TokenContext;
import th.go.dxc.platform.search.domain.common.value.UserContext;

@Slf4j
@RestController
@RequestMapping(path = "/api/reports", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ReportController {
  private final GeneratePdfFromReportIdUseCase reportUseCase;

  @PostMapping("/{reportId}/pdf")
  public Mono<GeneratePdfFromReportIdUseCase.Result> generate(
      @PathVariable String reportId,
      @RequestParam(name = "filename", required = false) String filename,
      @AuthenticationPrincipal Jwt jwt,
      ServerHttpRequest request
  ) {
    UserContext uctx   = UserContextMapper.fromJwt(jwt);           // identity/roles/tenant
    TokenContext tctx  = TokenContextMapper.fromJwt(jwt);          // jti, session_state, iat/auth_time, acr/amr
    RequestContext rctx= RequestContextMapper.fromRequest(request);// correlationId, clientIp, userAgent, requestStart

    // ---- Structured audit log ----
    log.info("Report PDF request: reportId={} correlationId={} userId={} clientId={} tenantId={} tokenId={} ip={} ua={}",
        reportId, rctx.correlationId(), uctx.userId(), uctx.clientId(), uctx.tenantId(),
        tctx.tokenId(), rctx.clientIp(), rctx.userAgent());

    var q = new GeneratePdfFromReportIdUseCase.Query(reportId, uctx, filename);
    return reportUseCase.generate(q);
  }
}
