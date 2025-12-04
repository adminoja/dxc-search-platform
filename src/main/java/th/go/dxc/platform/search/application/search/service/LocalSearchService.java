package th.go.dxc.platform.search.application.search.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.catalog.port.in.GetDatasetUseCase;
import th.go.dxc.platform.search.application.common.security.ScopeHasher;
import th.go.dxc.platform.search.application.report.port.out.snapshot.ReportDataSnapshotStorePort;
import th.go.dxc.platform.search.application.report.port.out.token.ReportTokenStorePort;
import th.go.dxc.platform.search.application.search.port.in.SearchLocalSearchUseCase;
import th.go.dxc.platform.search.application.search.port.out.QmClientPort;
import th.go.dxc.platform.search.config.ReportProperties;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.report.model.ReportDataSnapshot;
import th.go.dxc.platform.search.domain.report.model.ReportToken;
import th.go.dxc.platform.search.domain.search.model.DataRecord;
import th.go.dxc.platform.search.domain.search.model.LocalSearchRecord;
import th.go.dxc.platform.search.domain.search.model.LocalSearchResult;

@Slf4j
@AllArgsConstructor
@Service
public class LocalSearchService implements SearchLocalSearchUseCase {

        private final GetDatasetUseCase getDatasetUseCase;
        private final QmClientPort qmSearchPort;
        private final ScopeHasher scopeHasher;
        private final ReportDataSnapshotStorePort reportDataSnapshotStore;
        private final ReportProperties reportProperties;
        private final ReportTokenStorePort reportTokenStore;

        @Override
        public Mono<LocalSearchResult> execute(SearchLocalSearchUseCase.Input input) {
                return getDatasetUseCase
                                .execute(new GetDatasetUseCase.Input(input.request().datasetId()))
                                // Result -> Optional<Dataset> -> Mono<Dataset>
                                .flatMap(r -> Mono.justOrEmpty(r.dataset()))
                                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                                "Dataset not found: " + input.request().datasetId().value())))
                                .flatMap(dataset -> qmSearchPort.search(dataset.route(), input.request(),
                                                input.userContext()))
                                .flatMap(page -> toCachedLocalSearchRecordPage(page, input.userContext(),
                                                input.request().datasetId().value(), Instant.now(),
                                                input.isReport()))
                                .map(enrichedAndCachedPage -> {
                                        return LocalSearchResult.success(
                                                        input.request().datasetId().value(),
                                                        UUID.randomUUID().toString(),
                                                        enrichedAndCachedPage,
                                                        input.request().requestedAt(),
                                                        System.currentTimeMillis()
                                                                        - input.request().requestedAt().toEpochMilli());
                                });
        }

        private LocalSearchRecord cachedLocalSearchRecord(JsonNode data, UserContext userContext, String datasetId,
                        Instant createdAt, Boolean isReport) {
                if (isReport) {
                        ReportToken token = reportTokenStore.nextToken();
                        ReportDataSnapshot snapshot = ReportDataSnapshot.of(token, datasetId, data);
                        String scopeHash = scopeHasher.scopeFor(userContext.userId(), userContext.tenantId(),
                                        userContext.realm());
                        log.debug("cacheAndAttachReportIds: token = {} , scopeHash = {}", token.value(),
                                        scopeHash);

                        reportDataSnapshotStore.put(scopeHash, token, snapshot,
                                        reportProperties.snapshot().ttl());
                        return LocalSearchRecord.of(token, DataRecord.of(
                                        data, datasetId, datasetId, createdAt));
                }else {
                        return LocalSearchRecord.of(ReportToken.none(), DataRecord.of(
                                        data, datasetId, datasetId, createdAt));
                }
                // return snapshot;
        }

        private Mono<DomainPageResult<LocalSearchRecord>> toCachedLocalSearchRecordPage(
                        DomainPageResult<DataRecord> page, UserContext userContext, String datasetId, Instant createdAt,
                        Boolean isReport) {

                List<LocalSearchRecord> newContent = page.content().stream().map(dr -> {
                        return cachedLocalSearchRecord(dr.data(), userContext, datasetId, createdAt,isReport);
                }).toList();
                return Mono.just(DomainPageResult.of(
                                newContent,
                                DomainPageRequest.of(page.number(), page.size(), page.sort()),
                                page.totalElements()));
        }
}
