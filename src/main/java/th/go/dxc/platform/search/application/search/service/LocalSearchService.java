package th.go.dxc.platform.search.application.search.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.catalog.port.in.GetDatasetUseCase;
import th.go.dxc.platform.search.application.common.security.ScopeHasher;
import th.go.dxc.platform.search.application.report.model.ReportDataSnapshot;
import th.go.dxc.platform.search.application.report.model.ReportToken;
import th.go.dxc.platform.search.application.report.port.out.snapshot.SnapshotCachePort;
import th.go.dxc.platform.search.application.search.port.in.LocalSearchUseCase;
import th.go.dxc.platform.search.application.search.port.out.QmSearchPort;
import th.go.dxc.platform.search.config.ReportProperties;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;
import th.go.dxc.platform.search.domain.common.value.DomainSort;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.search.model.DataRecord;
import th.go.dxc.platform.search.domain.search.model.DataRecordSummary;
import th.go.dxc.platform.search.domain.search.model.SearchResult;

@Slf4j
@AllArgsConstructor
@Service
public class LocalSearchService implements LocalSearchUseCase {

        private final GetDatasetUseCase getDatasetUseCase;
        private final QmSearchPort qmSearchPort;
        // private final CachePort cache;
        // private final SearchCacheProperties ttlConfig;
        private final ScopeHasher scopeHasher;
        private final SnapshotCachePort snapshotCache;
        private final ReportProperties reportProperties;

        @Override
        public Mono<SearchResult> query(LocalSearchUseCase.Query query) {
                return getDatasetUseCase
                                .execute(new GetDatasetUseCase.Input(query.request().datasetId()))
                                // Result -> Optional<Dataset> -> Mono<Dataset>
                                .flatMap(r -> Mono.justOrEmpty(r.dataset()))
                                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                                "Dataset not found: " + query.request().datasetId().value())))
                                .flatMap(dataset -> qmSearchPort.search(dataset.route(), query.request(),
                                                query.userContext()))
                                .flatMap(page -> cacheAndAttachReportIds(query, page));
        }

        private Mono<SearchResult> cacheAndAttachReportIds(LocalSearchUseCase.Query q,
                        DomainPageResult<DataRecord> page) {
                log.debug("cacheAndAttachReportIds: query={} ", q);
                final String datasetId = q.request().datasetId().value();
                final String runId = UUID.randomUUID().toString();
                ;
                // final String scopeHash = scopeHash(q.userContext()); // prevent cross-user
                // reads

                // Build items + cache in one step off the event loop (if large pages)
                return Mono.fromCallable(() -> {
                        // var now = Instant.now();
                        List<DataRecordSummary> items = new ArrayList<DataRecordSummary>(page.content().size());
                        for (DataRecord rec : page.content()) {
                                Map<String, Object> row = rec.data();

                                ReportToken token = ReportToken.random();

                                // If you added the factory:
                                ReportDataSnapshot snapshot = ReportDataSnapshot.of(datasetId, row);

                                // If you didn't add the factory, use this instead:
                                // ReportDataSnapshot snapshot = new ReportDataSnapshot(datasetId,
                                // Instant.now(), row);

                                UserContext userContext = q.userContext();
                                String scopeHash = scopeHasher.scopeFor(userContext.userId(), userContext.tenantId(),
                                                userContext.realm());
                                log.debug("cacheAndAttachReportIds: token = {} , scopeHash = {}", token.value(),
                                                scopeHash);

                                snapshotCache.put(scopeHash, token, snapshot, reportProperties.snapshot().ttl());

                                // return token.value() to UI as `reportToken`

                                items.add(new DataRecordSummary(
                                                // new DataRecordSummary.Id(dataId),
                                                token.value(), // or token.recordDetailToken()
                                                row));
                        }
                        return new SearchResult(
                                        runId,
                                        DomainPageResult.of(
                                                        items,
                                                        DomainPageRequest.of(page.number(), page.size(),
                                                                        DomainSort.unsortedSort()),
                                                        page.totalElements()));
                });
        }

        // private String cacheKeyForDetail(String datasetId, String dataId, String
        // scopeHash, String runId) {
        // // Keep it explicit and flat for observability
        // return String.join(":", "detail", datasetId, dataId, scopeHash, runId);
        // }

        // private String scopeHash(UserContext uctx) {
        // // Hash only the minimum needed to scope cache; HMAC with a server secret if
        // you
        // // prefer.
        // var basis = new StringBuilder()
        // .append(Objects.toString(uctx.userId(), ""))
        // .append("|")
        // .append(uctx.authorities() == null ? ""
        // : uctx.authorities().stream().sorted().collect(Collectors.joining(",")))
        // .append("|")
        // .append(Objects.toString(uctx.clientId(), ""));
        // return sha256Hex(basis.toString());
        // }

        // private static String sha256Hex(String s) {
        // try {
        // var md = MessageDigest.getInstance("SHA-256");
        // byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
        // var sb = new StringBuilder(dig.length * 2);
        // for (byte b : dig)
        // sb.append(String.format("%02x", b));
        // return sb.toString();
        // } catch (NoSuchAlgorithmException e) {
        // throw new IllegalStateException(e);
        // }
        // }

}
