package th.go.dxc.platform.search.application.report.verify;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import th.go.dxc.platform.search.application.report.model.ReportDataSnapshot;
import th.go.dxc.platform.search.application.report.model.ReportToken;
import th.go.dxc.platform.search.application.report.port.out.snapshot.SnapshotCachePort;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class VerifyService {

  private final VerifyProperties props;
  private final SnapshotCachePort cache;   // blocking port you showed
  private final TokenSealer sealer;

  public VerifyService(VerifyProperties props, SnapshotCachePort cache) {
    this.props = props;
    this.cache = cache;
    this.sealer = new TokenSealer(props);
  }

  /** Build sealed verify token at snapshot time (scopeHash + token + ts + exp). */
  public String buildVerifyToken(String scopeHash, ReportToken token, ReportDataSnapshot snap) {
    Instant ts = snap.createdAt();
    Instant exp = ts.plusSeconds(props.tokenTtlMinutes() * 60L);
    return sealer.seal(token.value(), scopeHash, ts, exp);
  }

  /** Footer meta for Thymeleaf (QR → /verify?vt=..., BE timestamps, short data hash). */
  public Map<String,Object> buildFooterMeta(ServerHttpRequest req, String vt, ReportDataSnapshot snap, String printedBy) {
    String verifyUrl = VerifySupport.buildVerifyUrl(props, req, vt);
    String qrBase64  = VerifySupport.qrBase64Png(verifyUrl, props.qr().size());
    String dataHash  = VerifySupport.stableSha256(snap.data());

    var meta = new LinkedHashMap<String,Object>();
    meta.put("qrBase64", qrBase64);
    meta.put("verifyShort", verifyUrl.replaceFirst("^https?://",""));
    meta.put("createdAtBe", VerifySupport.be(snap.createdAt()));
    meta.put("printedAtBe", VerifySupport.be(Instant.now()));
    meta.put("printedBy", printedBy);
    meta.put("dataHash", dataHash.substring(0, 8));
    return meta;
  }

  /** Verify from sealed token; wraps blocking cache calls in boundedElastic. */
  public Mono<VerifyResult> verifyByVt(String vt) {
    return Mono.fromCallable(() -> sealer.open(vt)) // AES-GCM decrypt (CPU only)
        .publishOn(Schedulers.parallel())
        .flatMap(p -> {
          if (Instant.now().isAfter(p.exp())) return Mono.just(VerifyResult.expired());

          // ⚠️ SnapshotCachePort.get(scopeHash, ReportToken) is blocking → wrap it:
          return Mono.fromCallable(() -> cache.get(p.sc(), new ReportToken(p.rt())))
              .subscribeOn(Schedulers.boundedElastic())
              .map(snap -> {
                if (snap == null) return VerifyResult.expired();
                if (!p.ts().equals(snap.createdAt())) return VerifyResult.badTs();

                String hash = VerifySupport.stableSha256(snap.data());
                Map<String,Object> summary = Map.of(
                    "caseRegistrationNumber", snap.data().getOrDefault("caseRegistrationNumber", "-"),
                    "officeName",            snap.data().getOrDefault("officeName", "-")
                );
                return VerifyResult.valid(hash, summary);
              })
              .defaultIfEmpty(VerifyResult.expired());
        })
        .onErrorResume(e -> Mono.just(VerifyResult.invalid()));
  }

  public record VerifyResult(String status, String dataHash, Map<String,Object> summary) {
    static VerifyResult valid(String h, Map<String,Object> s){ return new VerifyResult("VALID",   h, s); }
    static VerifyResult expired(){ return new VerifyResult("EXPIRED", null, null); }
    static VerifyResult badTs(){ return new VerifyResult("TIMESTAMP_MISMATCH", null, null); }
    static VerifyResult invalid(){ return new VerifyResult("INVALID_TOKEN", null, null); }
  }
}
