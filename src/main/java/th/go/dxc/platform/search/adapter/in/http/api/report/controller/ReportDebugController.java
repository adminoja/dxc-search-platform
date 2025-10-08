package th.go.dxc.platform.search.adapter.in.http.api.report.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.common.security.ScopeHasher;
import th.go.dxc.platform.search.application.report.model.ReportDataSnapshot;
import th.go.dxc.platform.search.application.report.model.ReportModel;
import th.go.dxc.platform.search.application.report.model.ReportToken;
import th.go.dxc.platform.search.application.report.port.in.ReadReportModelUseCase;
import th.go.dxc.platform.search.application.report.port.out.snapshot.SnapshotCachePort;
import th.go.dxc.platform.search.domain.common.value.UserContext;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(path = "/api/reports")
@Validated
public class ReportDebugController {

  private final ReadReportModelUseCase readReportModel;
  private final SnapshotCachePort cache;
  private final ScopeHasher scopeHasher;

  public ReportDebugController(ReadReportModelUseCase readReportModel,
                               SnapshotCachePort cache,
                               ScopeHasher scopeHasher) {
    this.readReportModel = readReportModel;
    this.cache = cache;
    this.scopeHasher = scopeHasher;
  }

  /** Mapped model view (uses application port). */
  @GetMapping(value = "/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<ReportModel>> readModel(
      @PathVariable("token") @NotBlank String token,
      @AuthenticationPrincipal UserContext user
  ) {
    return readReportModel.execute(new ReadReportModelUseCase.Input(user, ReportToken.of(token)))
        .map(out -> ResponseEntity.ok(out.report()))
        .onErrorResume(IllegalArgumentException.class, e -> Mono.just(ResponseEntity.notFound().build()));
  }

  /** Raw snapshot object from cache (no eviction). */
  @GetMapping(value = "/{token}/raw", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<ReportDataSnapshot>> readRaw(
      @PathVariable("token") @NotBlank String token,
      @AuthenticationPrincipal UserContext user
  ) {
    log.debug("raw for {} for {}",token,user);
    final String scopeHash = scopeHasher.scopeFor(user.userId(), user.tenantId(), user.realm());
     log.debug("readRaw: token = {} , scopeHash = {}",token,scopeHash);
    return Mono.defer(() -> Mono.justOrEmpty(cache.get(scopeHash, ReportToken.of(token))))
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  /** Raw snapshot payload (Map only) (no eviction). */
  @GetMapping(value = "/{token}/raw/data", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Map<String, Object>>> readRawData(
      @PathVariable("token") @NotBlank String token,
      @AuthenticationPrincipal UserContext user
  ) {
    final String scopeHash = scopeHasher.scopeFor(user.userId(), user.tenantId(), user.realm());
    return Mono.defer(() -> Mono.justOrEmpty(cache.get(scopeHash, ReportToken.of(token))))
        .map(snap -> ResponseEntity.ok(snap.data()))
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  /** Evict snapshot by token. Returns 204 if evicted, 404 if not found. */
  @DeleteMapping("/{token}")
  public Mono<ResponseEntity<Void>> evict(
      @PathVariable("token") @NotBlank String token,
      @AuthenticationPrincipal UserContext user
  ) {
    final String scopeHash = scopeHasher.scopeFor(user.userId(), user.tenantId(), user.realm());
    final ResponseEntity<Void> NO_CONTENT =
        ResponseEntity.noContent().header("X-Report-Evicted", "true").build();

    return Mono.defer(() -> Mono.justOrEmpty(cache.getAndEvict(scopeHash, ReportToken.of(token))))
        .map(s -> NO_CONTENT) // keep type as ResponseEntity<Void>
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }
}
