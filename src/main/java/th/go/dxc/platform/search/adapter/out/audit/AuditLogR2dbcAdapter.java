package th.go.dxc.platform.search.adapter.out.audit;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.audit.port.out.AuditLogPort;
import th.go.dxc.platform.search.domain.audit.value.AuditEvent;

@Repository
@RequiredArgsConstructor
public class AuditLogR2dbcAdapter implements AuditLogPort {

  private final DatabaseClient db;

  @Override
  public Mono<Void> log(AuditEvent e) {

    GenericExecuteSpec spec = db.sql("""
      INSERT INTO audit_log(
        ts, tenant_id, user_id, username, client_id,
        token_id, session_id,
        action, resource_type, resource_id,
        outcome,
        correlation_id, ip, user_agent, latency_ms,
        details_json, version
      ) VALUES (
        :ts, :tenant, :uid, :uname, :cid,
        :tid, :sid,
        :act, :rtype, :rid,
        :outc,
        :corr, :ip, :ua, :lat,
        :det, :ver
      )
      """);

    // required / non-null
    spec = spec.bind("ts", e.ts());
    spec = spec.bind("ver", e.version() == null ? 1 : e.version());

    // nullable strings
    spec = bind(spec, "tenant",  e.tenantId(),   String.class);
    spec = bind(spec, "uid",     e.userId(),     String.class);
    spec = bind(spec, "uname",   e.username(),   String.class);
    spec = bind(spec, "cid",     e.clientId(),   String.class);
    spec = bind(spec, "tid",     e.tokenId(),    String.class);
    spec = bind(spec, "sid",     e.sessionId(),  String.class);
    spec = bind(spec, "rtype",   e.resourceType(), String.class);
    spec = bind(spec, "rid",     e.resourceId(), String.class);
    spec = bind(spec, "corr",    e.correlationId(), String.class);
    spec = bind(spec, "ip",      e.ip(),         String.class);
    spec = bind(spec, "ua",      e.userAgent(),  String.class);
    spec = bind(spec, "det",     e.detailsJson(), String.class);

    // enums (as uppercase strings)
    spec = bindEnum(spec, "act",  e.action());
    spec = bindEnum(spec, "outc", e.outcome());

    // nullable integer
    spec = bind(spec, "lat", e.latencyMs(), Integer.class);

    return spec.then();
  }

  // ---------- helpers ----------

  private static <T> GenericExecuteSpec bind(GenericExecuteSpec spec, String name, T value, Class<?> type) {
    return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
  }

  private static <E extends Enum<E>> GenericExecuteSpec bindEnum(GenericExecuteSpec spec, String name, E value) {
    return value == null ? spec.bindNull(name, String.class) : spec.bind(name, value.name());
  }
}
