package th.go.dxc.platform.search.domain.search.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.catalog.model.Domain;

@Builder
public record SpecializedReportResult(
                String runId,
                SpecializedReport.Id reportId,
                // Map<Domain.Id, JsonNode> domainResults,
                Map<Domain.Id, Map<Dataset.Id,List<Map<String,String>>>> domainResults,
                Instant startedAt,
                Long durationMs) {

        public static SpecializedReportResult of(String runId, SpecializedReport.Id reportId,
                        // Map<Domain.Id, JsonNode> domainResults, 
                        Map<Domain.Id, Map<Dataset.Id,List<Map<String,String>>>> domainResults,
                        Instant startedAt, long durationMs) {
                runId = runId == null ? "" : runId;
                reportId = reportId == null ? SpecializedReport.Id.empty() : reportId;
                domainResults = domainResults == null ? Map.of() : domainResults;
                startedAt = startedAt == null ? Instant.EPOCH : startedAt;
                durationMs = durationMs < 0 ? 0 : durationMs;
                // return new SpecializedReportResult(runId, reportId, domainResults, startedAt, durationMs);
                return new SpecializedReportResult(runId, reportId, domainResults, startedAt, durationMs);
        }
        public record Item(String key,String value,Instant ts){}
}
