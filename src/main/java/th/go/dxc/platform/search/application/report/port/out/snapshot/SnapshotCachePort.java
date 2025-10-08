// src/main/java/th/go/dxc/platform/search/application/report/port/out/snapshot/SnapshotCachePort.java
package th.go.dxc.platform.search.application.report.port.out.snapshot;

import java.time.Duration;
import th.go.dxc.platform.search.application.report.model.ReportDataSnapshot;
import th.go.dxc.platform.search.application.report.model.ReportToken;

public interface SnapshotCachePort {
  void put(String scopeHash, ReportToken token, ReportDataSnapshot snapshot, Duration ttl);
  ReportDataSnapshot get(String scopeHash, ReportToken token);
  ReportDataSnapshot getAndEvict(String scopeHash, ReportToken token);
  default String key(String scope, ReportToken token) { return "v1:detail:" + scope + ":" + token.value(); }
}
