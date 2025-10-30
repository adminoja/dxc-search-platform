package th.go.dxc.platform.search.application.search.service.special;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import th.go.dxc.platform.search.application.catalog.port.in.GetSpecializedReportByIdUseCase;
import th.go.dxc.platform.search.application.catalog.port.in.ListDatasetFieldRuleMapByDomainCanonicalKeyUseCase;
import th.go.dxc.platform.search.application.catalog.port.in.ListDomainsByIdsUseCase;
import th.go.dxc.platform.search.application.search.port.in.GetGlobalSearchResultUseCase;
import th.go.dxc.platform.search.application.search.port.in.GetSpecializedReportResultUseCase;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.catalog.model.Domain;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchResult;
import th.go.dxc.platform.search.domain.search.model.LocalSearchRecord;
import th.go.dxc.platform.search.domain.search.model.LocalSearchResult;
import th.go.dxc.platform.search.domain.search.model.SpecializedReportResult;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetSpecializedReportResultExecutor
                implements
                GetSpecializedReportResultUseCase {
        private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\$\\{([^}]+)}");

        private final ObjectMapper mapper;
        private final GetGlobalSearchResultUseCase getGlobalResult;
        private final GetSpecializedReportByIdUseCase getSpecializedReport;
        private final ListDomainsByIdsUseCase listDomains;
        private final ListDatasetFieldRuleMapByDomainCanonicalKeyUseCase listFieldRuleMap;

        @Override
        public Mono<SpecializedReportResult> execute(GetSpecializedReportResultUseCase.Input input) {
                return getGlobalResult.execute(
                                GetGlobalSearchResultUseCase.Input.of(input.runId()))
                                .flatMap(globalResult -> toSpecializedReportResult(globalResult, input.runId()));
        }

        private Mono<SpecializedReportResult> toSpecializedReportResult(
                        GlobalSearchResult globalResult, String runId) {

                return getSpecializedReport
                                .execute(GetSpecializedReportByIdUseCase.Input.of(globalResult.productFeatureId())) // Mono<SpecializedReport>
                                .flatMapMany(report -> listDomains
                                                .execute(ListDomainsByIdsUseCase.Input.of(report.domainIds()))) // Flux<Domain>
                                .map(domain -> toDomainResultsAllKeys(domain.id(), domain.canonicalKeys(),
                                                globalResult)) // Map<Domain.Id, Map<Dataset.Id,
                                                               // List<Map<String,String>>>>
                                .reduce(new LinkedHashMap<Domain.Id, Map<Dataset.Id, List<Map<String, String>>>>(),
                                                this::mergeDomainResultMaps)
                                .map(merged -> SpecializedReportResult.builder()
                                                .runId(runId)
                                                .domainResults(merged)
                                                .build());
        }

        // private Mono<SpecializedReportResult> toSpecializedReportResult(
        // GlobalSearchResult globalResult, String runId) {

        // return getSpecializedReport
        // .execute(GetSpecializedReportByIdUseCase.Input.of(globalResult.productFeatureId()))
        // // Mono<SpecializedReport>
        // .flatMapMany(report -> listDomains
        // .execute(ListDomainsByIdsUseCase.Input.of(report.domainIds()))) //
        // Flux<Domain>
        // .flatMap(domain -> Flux.fromIterable(domain.canonicalKeys())
        // .map(ck -> Tuples.of(domain.id(), ck))) // Flux<Tuple2<Domain.Id,String>>
        // .map(t -> toDomainResults(t.getT1(), t.getT2(), globalResult)) //
        // Map<Domain.Id,
        // // Map<Dataset.Id,
        // // List<Map<String,String>>>>
        // .reduce(new LinkedHashMap<Domain.Id, Map<Dataset.Id, List<Map<String,
        // String>>>>(),
        // this::mergeDomainResultMaps) // merged map
        // .map(merged -> SpecializedReportResult.builder()
        // .runId(runId)
        // .domainResults(merged)
        // .build());
        // }

        public Map<Domain.Id, Map<Dataset.Id, List<Map<String, String>>>> toDomainResultsAllKeys(
                        Domain.Id domainId,
                        List<String> canonicalKeys,
                        GlobalSearchResult globalResult) {

                // 1) Collect FieldRule per (canonicalKey, datasetId)
                // rulesByKey: canonicalKey -> (datasetId -> FieldRule)
                Map<String, Map<Dataset.Id, Dataset.FieldRule>> rulesByKey = new LinkedHashMap<>();
                for (String ck : canonicalKeys) {
                        Map<Dataset.Id, Dataset.FieldRule> rulesForKey = listFieldRuleMap.execute(
                                        new ListDatasetFieldRuleMapByDomainCanonicalKeyUseCase.Input(domainId, ck));
                        if (rulesForKey != null && !rulesForKey.isEmpty()) {
                                rulesByKey.put(ck, rulesForKey);
                        }
                }
                if (rulesByKey.isEmpty()) {
                        return Collections.singletonMap(domainId, Map.of());
                }

                // 2) Source data grouped by dataset
                Map<Dataset.Id, List<JsonNode>> dataByDataset = toDatasetJsonMap(globalResult, mapper);

                // 3) For each dataset, build rows. Each row aggregates all canonicalKey ->
                // valueText
                Map<Dataset.Id, List<Map<String, String>>> byDataset = new LinkedHashMap<>();

                dataByDataset.forEach((datasetId, dataNodeList) -> {
                        // Pre-extract the applicable rules for this dataset across all canonical keys
                        // applicable: canonicalKey -> FieldRule (only those that exist for this
                        // dataset)
                        Map<String, Dataset.FieldRule> applicable = new LinkedHashMap<>();
                        for (Map.Entry<String, Map<Dataset.Id, Dataset.FieldRule>> e : rulesByKey.entrySet()) {
                                Dataset.FieldRule r = e.getValue().get(datasetId);
                                if (r != null) {
                                        applicable.put(e.getKey(), r);
                                }
                        }
                        if (applicable.isEmpty()) {
                                return; // no rules for this dataset; skip
                        }

                        List<Map<String, String>> rows = new java.util.ArrayList<>();

                        // For each record node in this dataset, create one row map and fill all
                        // canonical keys
                        for (JsonNode dataNode : dataNodeList) {
                                Map<String, String> row = new LinkedHashMap<>();

                                for (Map.Entry<String, Dataset.FieldRule> e : applicable.entrySet()) {
                                        String canonicalKey = e.getKey();
                                        Dataset.FieldRule rule = e.getValue();

                                        JsonNode valueNode = evaluateValue(dataNode, rule);
                                        String valueText = (valueNode == null || valueNode.isNull()) ? null
                                                        : valueNode.asText(null);

                                        // Include only non-null values; if you want nulls kept, remove this guard
                                        if (valueText != null) {
                                                row.put(canonicalKey, valueText);
                                        }
                                }

                                // If at least one canonical key produced a value, keep the row
                                if (!row.isEmpty()) {
                                        rows.add(row);
                                }
                        }

                        if (!rows.isEmpty()) {
                                byDataset.put(datasetId, rows);
                        }
                });

                return Collections.singletonMap(domainId, byDataset);
        }

        // private Map<Domain.Id, Map<Dataset.Id, List<Map<String, String>>>> toDomainResults(
        //                 Domain.Id domainId,
        //                 String canonicalKey,
        //                 GlobalSearchResult globalResult) {

        //         Map<Dataset.Id, Dataset.FieldRule> rules = listFieldRuleMap.execute(
        //                         new ListDatasetFieldRuleMapByDomainCanonicalKeyUseCase.Input(domainId, canonicalKey));

        //         Map<Dataset.Id, List<JsonNode>> dataByDataset = toDatasetJsonMap(globalResult, mapper);
        //         Instant now = Instant.now();

        //         Map<Dataset.Id, List<Map<String, String>>> byDataset = new LinkedHashMap<>();

        //         dataByDataset.forEach((datasetId, dataNodeList) -> {
        //                 Dataset.FieldRule rule = rules.get(datasetId);
        //                 if (rule == null)
        //                         return; // no mapping for this dataset; skip

        //                 List<Map<String, String>> rows = byDataset.computeIfAbsent(datasetId,
        //                                 k -> new java.util.ArrayList<>());

        //                 for (JsonNode dataNode : dataNodeList) {
        //                         JsonNode valueNode = evaluateValue(dataNode, rule);
        //                         String valueText = (valueNode == null || valueNode.isNull()) ? null
        //                                         : valueNode.asText(null);

        //                         // All String keys/values (value may be null; Map<String,String> allows null)
        //                         Map<String, String> row = new LinkedHashMap<>();
        //                         row.put("key", canonicalKey);
        //                         row.put("value", valueText);
        //                         row.put("timestamp", now.toString());
        //                         // Optional: keep datasetId in the row too (redundant since it's the outer key)
        //                         row.put("datasetId", datasetId.value());

        //                         rows.add(row);
        //                 }
        //         });

        //         return Collections.singletonMap(domainId, byDataset);
        // }

        private Map<Domain.Id, Map<Dataset.Id, List<Map<String, String>>>> mergeDomainResultMaps(
                        Map<Domain.Id, Map<Dataset.Id, List<Map<String, String>>>> acc,
                        Map<Domain.Id, Map<Dataset.Id, List<Map<String, String>>>> inc) {

                inc.forEach((domainId, datasetMap) -> {
                        Map<Dataset.Id, List<Map<String, String>>> target = acc.computeIfAbsent(domainId,
                                        k -> new LinkedHashMap<>());
                        datasetMap.forEach((datasetId, rows) -> {
                                target.merge(datasetId, new java.util.ArrayList<>(rows), (oldV, newV) -> {
                                        oldV.addAll(newV);
                                        return oldV;
                                });
                        });
                });
                return acc;
        }

        static String normalizePointer(String p) {
                return (p == null || p.isBlank()) ? "" : (p.startsWith("/") ? p : "/" + p);
        }

        public static Map<Dataset.Id, List<JsonNode>> toDatasetJsonMap(
                        GlobalSearchResult global,
                        ObjectMapper mapper) {
                Objects.requireNonNull(mapper, "mapper is required");

                Map<Dataset.Id, List<JsonNode>> out = new LinkedHashMap<>();

                if (global == null)
                        return out;
                List<LocalSearchResult> locals = global.results();
                if (locals == null || locals.isEmpty())
                        return out;

                for (LocalSearchResult lsr : locals) {
                        if (lsr == null)
                                continue;

                        // --- Resolve Dataset.Id from LocalSearchResult ---
                        // Change one of the lines below to match your actual LocalSearchResult API.

                        // Case A: LocalSearchResult stores dataset id as a String:
                        Dataset.Id datasetId = new Dataset.Id(lsr.datasetId()); // adjust if the accessor differs
                        log.debug("toDataJsonMap for {}", datasetId);
                        // Case B (alternate): LocalSearchResult already has Dataset.Id:
                        // Dataset.Id datasetId = lsr.datasetId();

                        if (datasetId == null || datasetId.value() == null || datasetId.value().isBlank())
                                continue;

                        // --- Choose what to serialize as the value ---
                        // Option 1: whole LocalSearchResult
                        // JsonNode value = mapper.valueToTree(lsr);

                        // Option 2 (alternate): only the data/page inside LocalSearchResult
                        if (lsr.pageResult() == null || lsr.pageResult().empty())
                                continue;
                        else
                                log.info("Empty pageResult for {}", datasetId);

                        log.debug("convert each data item to jsonNode. size={}", lsr.pageResult().content().size());
                        List<JsonNode> dataList = new ArrayList<>();
                        for (LocalSearchRecord rec : lsr.pageResult().content()) {
                                JsonNode data = rec.dataRecord().data();
                                log.debug("data = {}", data == null ? null : data.toString());
                                JsonNode value = mapper.valueToTree(data); // or lsr.data()
                                log.debug("JsonNode = {}", value == null ? null : value);
                                dataList.add(value);
                        }
                        out.put(datasetId, dataList);
                }

                return out;
        }

        /**
         * Evaluate a Dataset.FieldRule against a JsonNode.
         * Supports:
         * - pointer: single Jackson pointer (e.g. "/province_id")
         * - coalesce: list of pointers; first non-null/non-empty wins
         * - compose: template like "${/house_no} ${/road} จ.${/province_name}"
         * Optional transforms (basic subset shown):
         * - trim, upper, lower, squashWhitespace
         */
        private JsonNode evaluateValue(JsonNode data, Dataset.FieldRule rule) {
                // 1) pointer
                if (notBlank(rule.pointer())) {
                        String pointer = normalizePointer(rule.pointer());
                        log.debug("Evaluate pointer {} with {}", pointer, data.asText());
                        JsonNode v = getByPointer(data, pointer);
                        log.debug("Got {}", v.asText());
                        return applyTransforms(v, rule);
                }
                // 2) coalesce
                if (rule.coalesce() != null && !rule.coalesce().isEmpty()) {
                        for (String ptr : rule.coalesce()) {
                                JsonNode v = getByPointer(data, ptr);
                                if (hasText(v)) {
                                        return applyTransforms(v, rule);
                                }
                        }
                        // all empty => null
                        return NullNode.getInstance();
                }
                // 3) compose
                if (notBlank(rule.compose())) {
                        String composed = composeTemplate(rule.compose(), data);
                        JsonNode v = composed == null ? NullNode.getInstance()
                                        : mapper.getNodeFactory().textNode(composed);
                        return applyTransforms(v, rule);
                }
                // nothing specified
                return NullNode.getInstance();
        }

        private JsonNode getByPointer(JsonNode root, String pointer) {
                // Accept both "/a/b" and "a/b" by normalizing
                String normalized = pointer.startsWith("/") ? pointer : "/" + pointer;
                JsonPointer jp = JsonPointer.compile(normalized);
                JsonNode node = root.at(jp);
                return node.isMissingNode() ? NullNode.getInstance() : node;
        }

        private String composeTemplate(String template, JsonNode data) {
                if (template == null)
                        return null;
                Matcher m = TEMPLATE_TOKEN.matcher(template);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                        String path = m.group(1).trim();
                        JsonNode v = getByPointer(data, path);
                        String replacement = hasText(v) ? Matcher.quoteReplacement(v.asText()) : "";
                        m.appendReplacement(sb, replacement);
                }
                m.appendTail(sb);
                String out = sb.toString();
                // collapse if entirely blank
                return out.isBlank() ? null : out;
        }

        private JsonNode applyTransforms(JsonNode value, Dataset.FieldRule rule) {

                log.debug("applyTransform rule {} with {}", rule, asText(value));
                if (rule.transform() == null || rule.transform().isEmpty()) {
                        log.debug("No Transform set return");
                        return value == null ? NullNode.getInstance() : value;
                }
                String s = value == null || value.isNull() ? null : value.asText(null);

                for (Dataset.TransformRule tr : rule.transform()) {
                        if (s == null)
                                break;
                        Dataset.TransformType type = tr.type();
                        if (type == null)
                                continue;

                        switch (type) {
                                case trim -> s = s.trim();
                                case upper -> s = s.toUpperCase(Locale.ROOT);
                                case lower -> s = s.toLowerCase(Locale.ROOT);
                                case squashWhitespace -> s = s.replaceAll("\\s+", " ").trim();
                                // Add your other transforms here, e.g. toDate, toDecimal, padLeft, mapCode,
                                // etc.
                                default -> {
                                        /* no-op for unimplemented ones */ }
                        }
                }
                return s == null ? NullNode.getInstance() : mapper.getNodeFactory().textNode(s);
        }

        private static boolean hasText(JsonNode n) {
                return n != null && !n.isNull() && !n.isMissingNode() && !n.asText("").isBlank();
        }

        private static boolean notBlank(String s) {
                return s != null && !s.isBlank();
        }

        private String asText(JsonNode json) {
                String text = null;
                if (json != null) {
                        try {
                                text = mapper.writeValueAsString(json);
                        } catch (JsonProcessingException e) {
                                e.printStackTrace();
                        }
                }
                return text;
        }
}
