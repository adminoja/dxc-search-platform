package th.go.dxc.platform.search.application.search.service.special;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.catalog.port.in.ListDatasetLocalFieldsBySearchFieldsUseCase;
import th.go.dxc.platform.search.application.catalog.port.in.ListSpecializedReportDatasetIdsUseCase;
import th.go.dxc.platform.search.application.search.port.in.SearchGlobalSearchUseCase;
import th.go.dxc.platform.search.application.search.port.in.SearchSpecializedReportUseCase;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchRunIndexPort;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRequest;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRun;
import th.go.dxc.platform.search.domain.search.model.LocalSearchRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSpecializedReportExecutor implements SearchSpecializedReportUseCase {

        private final SearchGlobalSearchUseCase globalSearchUseCase;
        private final ListSpecializedReportDatasetIdsUseCase listDatasetIds;
        private final ListDatasetLocalFieldsBySearchFieldsUseCase listDatasetLocalFields;
        private final GlobalSearchRunIndexPort runIndexPort;

        @Override
        public Mono<String> execute(SearchSpecializedReportUseCase.Input input) {
                Instant requestedAt = Instant.now();
                log.debug("execute SearchSpecializedReportUseCase: {}", input);
                if (input == null || input.req() == null)
                        throw new IllegalArgumentException("Request can't be empty");
                log.debug("toLocalSearchRequests: {}", input.req().criteria());
                List<LocalSearchRequest> lRequestList = toLocalSearchRequests(input.req().criteria(),
                                input.req().pageRequest());
                log.debug("LocalSearchRequest List size={}", lRequestList == null ? null : lRequestList.size());
                return listDatasetIds.execute(
                                ListSpecializedReportDatasetIdsUseCase.Input.of(input.req().reportId())).collectList()
                                .flatMap(datasetIds -> globalSearchUseCase.execute(SearchGlobalSearchUseCase.Input
                                                .of(
                                                                GlobalSearchRequest.of(input.req().criteria(), lRequestList),
                                                                input.userContext(), input.invocationContext())))
                                // Step 2: now you have the runId (from globalSearchUseCase)
                                .flatMap(runId -> {
                                        String userId = input.userContext().userId();
                                        String username = input.userContext().username();
                                        String reportId = input.req().reportId().value();
                                        String subjectNin = (String) input.req().criteria().getOrDefault("person.citizen_id",
                                                        "");

                                        GlobalSearchRun run = GlobalSearchRun.start(runId, userId, username, reportId,
                                                        subjectNin, requestedAt);
                                        return runIndexPort.save(run).thenReturn(runId);
                                });
        }

        private List<LocalSearchRequest> toLocalSearchRequests(Map<String, Object> criteria,
                        DomainPageRequest pageRequest) {
                List<LocalSearchRequest> requestList = new ArrayList<>();
                Map<Dataset.Id, Map<String, String>> datasetFieldMap = listDatasetLocalFields
                                .execute(ListDatasetLocalFieldsBySearchFieldsUseCase.Input
                                                .of(new ArrayList<String>(criteria.keySet())));
                log.debug("Dataset Field Map = {}", datasetFieldMap);
                for (Dataset.Id datasetId : datasetFieldMap.keySet()) {
                        Map<String, String> globalLocalFieldMap = datasetFieldMap.get(datasetId);
                        Map<String, Object> localCriteria = new HashMap<>();
                        for (String gKey : criteria.keySet()) {
                                localCriteria.put(globalLocalFieldMap.get(gKey), criteria.get(gKey));
                        }
                        log.debug("Global Fiels: {}, localCriteria: {}", globalLocalFieldMap, localCriteria);
                        LocalSearchRequest request = LocalSearchRequest.of(Instant.now(), datasetId, localCriteria,
                                        pageRequest);
                        requestList.add(request);
                }

                return requestList;
        }
}
