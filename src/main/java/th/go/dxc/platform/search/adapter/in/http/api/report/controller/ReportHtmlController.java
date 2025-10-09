package th.go.dxc.platform.search.adapter.in.http.api.report.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.common.security.ScopeHasher;
import th.go.dxc.platform.search.application.report.model.ReportToken;
import th.go.dxc.platform.search.application.report.port.in.RenderHtmlReportUseCase;
import th.go.dxc.platform.search.domain.common.value.UserContext;

@RestController
@RequestMapping("/api/reports")
public class ReportHtmlController {

  private final RenderHtmlReportUseCase useCase;
  private final ScopeHasher scopeHasher;

  public ReportHtmlController(RenderHtmlReportUseCase useCase, ScopeHasher scopeHasher) {
    this.useCase = useCase;
    this.scopeHasher = scopeHasher;
  }

  @GetMapping(value = "/{token}/pdf/html", produces = MediaType.TEXT_HTML_VALUE)
  public Mono<ResponseEntity<String>> html(@PathVariable String token,
                                           @AuthenticationPrincipal @NonNull UserContext user) {
    final String scopeHash = scopeHasher.scopeFor(user.userId(), user.tenantId(), user.realm());

    return useCase.execute(new RenderHtmlReportUseCase.Input(scopeHash, new ReportToken(token),user))
        .map(out -> ResponseEntity.ok()
            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
            .body(out.html()))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
  }
}
