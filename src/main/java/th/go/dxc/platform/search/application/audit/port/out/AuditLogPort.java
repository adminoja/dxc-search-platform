package th.go.dxc.platform.search.application.audit.port.out;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.audit.value.AuditEvent;

public interface AuditLogPort {
  Mono<Void> log(AuditEvent event);
}
