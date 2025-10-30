package th.go.dxc.platform.search.adapter.in.http.api.search.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.catalog.model.Domain;
import th.go.dxc.platform.search.domain.search.model.SpecializedReport;

public record SpecializedReportResultDto(String runId,
        String reportId,
        // Map<String, JsonNode> domainResults,
        Map<String, Map<String, List<Map<String, String>>>> domainResults,
        Instant startedAt,
        Long durationMs) {

    public static SpecializedReportResultDto of(String runId, SpecializedReport.Id reportId,
            // Map<Domain.Id, JsonNode> domainResults,
            Map<Domain.Id, Map<Dataset.Id, List<Map<String, String>>>> domainResults,
            Instant startedAt, Long durationMs) {
        runId = runId == null ? "" : runId;
        reportId = reportId == null ? SpecializedReport.Id.empty() : reportId;
        domainResults = domainResults == null ? Map.of() : domainResults;
        startedAt = startedAt == null ? Instant.EPOCH : startedAt;
        durationMs = durationMs == null || durationMs < 0 ? 0 : durationMs;
        return new SpecializedReportResultDto(runId, reportId.value(), toStringKeyMap(domainResults), startedAt,
                durationMs);
    }

    // public static Map<String, JsonNode> toStringKeyMap(Map<Domain.Id, JsonNode>
    // domainResults) {
    // if (domainResults == null || domainResults.isEmpty())
    // return Collections.emptyMap();

    // Map<String, JsonNode> out = new LinkedHashMap<>(domainResults.size());
    // domainResults.forEach((k, v) -> out.put(k.value(), v));
    // return out;
    // }

    public static Map<String, Map<String, List<Map<String, String>>>> toStringKeyMap(
            Map<Domain.Id, Map<Dataset.Id, List<Map<String, String>>>> domainResults) {
        if (domainResults == null || domainResults.isEmpty())
            return Collections.emptyMap();

        if (domainResults == null || domainResults.isEmpty())
            return Map.of();

        Map<String, Map<String, List<Map<String, String>>>> dto = new LinkedHashMap<>();

        domainResults.forEach((domainId, datasetMap) -> {
            String domainKey = domainId.value();
            Map<String, List<Map<String, String>>> inner = dto.computeIfAbsent(domainKey, k -> new LinkedHashMap<>());

            datasetMap.forEach((datasetId, rows) -> inner.put(datasetId.value(), rows));
        });

        return dto;
    }
}
