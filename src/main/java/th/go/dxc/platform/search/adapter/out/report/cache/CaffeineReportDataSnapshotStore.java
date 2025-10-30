package th.go.dxc.platform.search.adapter.out.report.cache;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;
import th.go.dxc.platform.search.application.report.port.out.snapshot.ReportDataSnapshotStorePort;
import th.go.dxc.platform.search.config.ReportProperties;
import th.go.dxc.platform.search.domain.report.model.ReportDataSnapshot;
import th.go.dxc.platform.search.domain.report.model.ReportToken;

@Slf4j
public final class CaffeineReportDataSnapshotStore implements ReportDataSnapshotStorePort {
  private final com.github.benmanes.caffeine.cache.Cache<String, ReportDataSnapshot> cache;
  private final String ver, ns;

  public CaffeineReportDataSnapshotStore(ReportProperties.Snapshot cfg) {
    this.cache = Caffeine.newBuilder()
        .maximumSize(cfg.caffeineMaxSize())
        .expireAfterWrite(cfg.ttl()) // per-cache TTL
        .build();
    this.ver = cfg.keyVersion();
    this.ns = cfg.keyNamespace();
  }

  private String k(String scope, ReportToken token) {
    return ver + ":" + ns + ":" + scope + ":" + token.value();
  }

  @Override
  public void put(String scope, ReportToken token, ReportDataSnapshot snap, Duration ttl) {
    String cacheKey = k(scope, token);
    cache.put(cacheKey, snap);
  }

  @Override
  public ReportDataSnapshot get(String scope, ReportToken token) {
    String cacheKey = k(scope, token);
    return cache.getIfPresent(cacheKey);
  }

  @Override
  public ReportDataSnapshot getAndEvict(String scope, ReportToken token) {
    String cacheKey = k(scope, token);
    ReportDataSnapshot v = cache.getIfPresent(cacheKey);
    if (v != null)
      cache.invalidate(cacheKey);
    return v;
  }
}
