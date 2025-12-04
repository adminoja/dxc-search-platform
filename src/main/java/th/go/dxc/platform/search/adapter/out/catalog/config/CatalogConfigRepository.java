package th.go.dxc.platform.search.adapter.out.catalog.config;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.catalog.port.out.DatasetRepository;
import th.go.dxc.platform.search.application.catalog.port.out.DomainCatalogRepository;
import th.go.dxc.platform.search.application.catalog.port.out.OrganizationRepository;
import th.go.dxc.platform.search.application.catalog.port.out.SpecializedReportCatalogRepository;
import th.go.dxc.platform.search.config.CatalogProperties;
import th.go.dxc.platform.search.config.CatalogProperties.DatasetProps;
import th.go.dxc.platform.search.config.CatalogProperties.DatasetProps.FieldRuleProps;
import th.go.dxc.platform.search.config.CatalogProperties.DatasetProps.FieldRuleProps.TransformRuleProps;
import th.go.dxc.platform.search.config.CatalogProperties.DatasetProps.MappingProps;
import th.go.dxc.platform.search.config.CatalogProperties.DatasetProps.RouteProps;
import th.go.dxc.platform.search.config.CatalogProperties.DatasetProps.RouteProps.DataProps;
import th.go.dxc.platform.search.config.CatalogProperties.DatasetProps.RouteProps.DataProps.PointerProps;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.catalog.model.Dataset.FieldMapping;
import th.go.dxc.platform.search.domain.catalog.model.Dataset.FieldRule;
import th.go.dxc.platform.search.domain.catalog.model.Dataset.FieldRule.TransformRule;
import th.go.dxc.platform.search.domain.catalog.model.Dataset.FieldRule.TransformType;
import th.go.dxc.platform.search.domain.catalog.model.Dataset.Route;
import th.go.dxc.platform.search.domain.catalog.model.Dataset.Route.Data;
import th.go.dxc.platform.search.domain.catalog.model.Dataset.Route.Data.Pointer;
import th.go.dxc.platform.search.domain.catalog.model.Domain;
import th.go.dxc.platform.search.domain.catalog.model.Organization;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;
import th.go.dxc.platform.search.domain.search.model.SpecializedReport;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CatalogConfigRepository
    implements OrganizationRepository, DatasetRepository, DomainCatalogRepository, SpecializedReportCatalogRepository {
  private final CatalogProperties properties;
  private final ObjectMapper objectMapper = new ObjectMapper();

  // Implementation details would go here
  @Override
  public DomainPageResult<Organization> searchOrganizationByKeyword(String keyword, DomainPageRequest pageRequest) {
    final String q = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    final boolean hasQ = !q.isEmpty();

    // Filter + map from config items to domain
    List<Organization> filtered = properties.organizations().stream()
        .filter(it -> {
          if (!hasQ)
            return true;
          return contains(it.id(), q)
              || contains(it.name(), q) // if you use single-language name
              || contains(it.description(), q);
        })
        .map(this::toDomain)
        // optional: stable sort for nice UX; prefer Thai collation if present
        .sorted(Comparator.comparing(
            Organization::name, // adjust if your domain uses LocalizedText
            Comparator.nullsLast(Collator.getInstance(Locale.of("th")))))
        .toList();

    // Pagination
    int from = Math.min(pageRequest.offset().intValue(), filtered.size());
    int to = Math.min(from + pageRequest.pageSize(), filtered.size());
    List<Organization> slice = filtered.subList(from, to);

    return DomainPageResult.of(slice, pageRequest, filtered.size());
  }

  private static boolean contains(String source, String needleLower) {
    return source != null && source.toLowerCase(Locale.ROOT).contains(needleLower);
  }

  // --- mapping helper ---

  private Organization toDomain(CatalogProperties.OrganizationProps it) {
    // If your domain has a single String name:
    return new Organization(new Organization.Id(it.id()), it.name(), it.description());

    // If you adopted LocalizedText (TH/EN), use:
    // var name = new LocalizedText(it.getNameTh(), it.getNameEn());
    // return new Organization(new OrganizationId(it.getId()), name, it.isActive());
  }

  private Dataset toDomain(CatalogProperties.DatasetProps datasetProps) {
    // If your domain has a single String name:
    return new Dataset(
        new Dataset.Id(datasetProps.id()),
        datasetProps.name(),
        datasetProps.description(),
        new Organization.Id(datasetProps.ownerOrgId()),
        toDomain(datasetProps.route()),
        toDomain(datasetProps.mapping()),
        toDomains(datasetProps));

    // If you adopted LocalizedText (TH/EN), use:
    // var name = new LocalizedText(it.getNameTh(), it.getNameEn());
    // return new Organization(new OrganizationId(it.getId()), name, it.isActive());
  }

  private Route toDomain(RouteProps routeProps) {
    return routeProps == null ? null
        : Route.of(routeProps.path(), routeProps.serviceId(), routeProps.headers(),routeProps.querys(), toDomain(routeProps.data()));
  }

  private Data toDomain(DataProps dataProps) {
    return dataProps == null ? null : Data.of(toDomain(dataProps.pointers()), dataProps.isArray());
  }

  private Pointer toDomain(PointerProps pointerProps) {
    return pointerProps == null ? null
        : Pointer.of(pointerProps.content(), pointerProps.pageNumber(), pointerProps.pageSize(),
            pointerProps.numberOfElements(), pointerProps.totalElements());
  }

  private FieldMapping toDomain(MappingProps mappingProps)
  {
    return new FieldMapping(
            mappingProps != null && mappingProps.searchFields() != null
                ? mappingProps.searchFields()
                : List.of(),
            mappingProps != null && mappingProps.canonicalSearchFields() != null
                ? mappingProps.canonicalSearchFields()
                : Map.of(),
            mappingProps != null && mappingProps.summaryFields() != null
                ? mappingProps.summaryFields()
                : List.of(),
            mappingProps != null && mappingProps.naturalKeyFields() != null
                ? mappingProps.naturalKeyFields()
                : List.of());
  }
  @Override
  public Optional<Organization> findOrganizationById(Organization.Id id) {
    return properties.organizations().stream().filter(o -> o.id().equalsIgnoreCase(id.value())).findAny()
        .map(this::toDomain);
  }

  @Override
  public DomainPageResult<Dataset> searchDatasetByKeyword(String keyword, DomainPageRequest pageRequest) {
    final String q = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    final boolean hasQ = !q.isEmpty();

    // Filter + map from config items to domain
    List<Dataset> filtered = properties.datasets().stream()
        .filter(it -> {
          if (!hasQ)
            return true;
          return contains(it.id(), q)
              || contains(it.name(), q) // if you use single-language name
              || contains(it.description(), q);
        })
        .map(this::toDomain)
        // optional: stable sort for nice UX; prefer Thai collation if present
        .sorted(Comparator.comparing(
            Dataset::name, // adjust if your domain uses LocalizedText
            Comparator.nullsLast(Collator.getInstance(Locale.of("th")))))
        .toList();

    // Pagination
    int from = Math.min(pageRequest.offset().intValue(), filtered.size());
    int to = Math.min(from + pageRequest.pageSize(), filtered.size());
    List<Dataset> slice = filtered.subList(from, to);

    return DomainPageResult.of(slice, pageRequest, filtered.size());

  }

  @Override
  public Optional<Dataset> findDatasetById(Dataset.Id id) {
    return properties.datasets().stream()
        .filter(d -> d.id() != null && d.id().equalsIgnoreCase(id.value()))
        .findFirst()
        .map(this::toDomain);
  }

  @Override
  public Flux<Domain> findDomainsByIds(List<Domain.Id> domainIds) {
    if (domainIds == null || domainIds.isEmpty())
      return Flux.empty();

    // 1) Null-safe read of properties.domains()
    List<CatalogProperties.DomainProps> props = properties.domains() == null ? Collections.emptyList()
        : properties.domains();

    // 2) Convert DomainProps -> Domain, then index by id
    Map<String, Domain> byId = props.stream()
        .map(this::toDomain) // make sure the method signature matches exactly (see below)
        .collect(Collectors.toMap(
            d -> d.id().value(),
            Function.identity(),
            (a, b) -> a,
            LinkedHashMap::new));

    // 3) Emit in the same order as requested, skipping missing
    return Flux.fromIterable(domainIds)
        .map(Domain.Id::value)
        .distinct()
        .map(byId::get)
        .filter(Objects::nonNull);
  }

  @Override
  public Mono<SpecializedReport> findSpecializedReportById(SpecializedReport.Id id) {
    Objects.requireNonNull(id, "id is required");

    return Mono.fromCallable(() -> properties.specializedReports().stream()
        .filter(r -> r.id().equals(id.value()))
        .findFirst()
        .map(this::toDomain)
        .orElse(null)).flatMap(Mono::justOrEmpty);
  }

  private SpecializedReport toDomain(CatalogProperties.SpecializedReportProps p) {
    // Adjust constructors/factory methods to match your domain classes
    return new SpecializedReport(
        SpecializedReport.Id.of(p.id()),
        p.name(),
        Organization.Id.of(p.ownerOrgId()),
        p.domainIds().stream().map(Domain.Id::of).collect(Collectors.toList()));
  }

  @Override
  public Flux<Dataset.Id> findDatasetIds(SpecializedReport.Id reportId) {

    // 1) Find the specialized report and get its domain-ids (empty if not found)
    List<String> domainIds = properties.specializedReports().stream()
        .filter(r -> r.id().equals(reportId.value())) // adjust if your id type differs
        .findFirst()
        .map(CatalogProperties.SpecializedReportProps::domainIds)
        .orElse(List.of());

    if (domainIds.isEmpty()) {
      return Flux.empty();
    }

    Set<String> wanted = Set.copyOf(domainIds);

    // 2) List all datasets that have ANY of those domain ids under datasets.domains
    return Flux.fromIterable(properties.datasets())
        .filter(ds -> ds.domains() != null && !ds.domains().isEmpty())
        .filter(ds -> {
          // domains() is assumed to be Map<String, ?>
          // Match on the map's keys (domain ids)
          var keys = ds.domains().keySet();
          return keys.stream().anyMatch(wanted::contains);
        })
        .map(ds -> new Dataset.Id(ds.id())); // if you have Dataset.Id.of(String), use that instead
  }

  @Override
  public Map<Dataset.Id, Map<String, String>> findDatasetIdLocalFields(List<String> canonicalKeyList) {
    log.debug("findDatasetIdLocalFields: {}", canonicalKeyList);
    Map<Dataset.Id, Map<String, String>> datasetIdLocalFields = new HashMap<>();

    for (DatasetProps datasetProps : properties.datasets()) {
      MappingProps mappingProps = datasetProps.mapping();
      log.debug("mappingProps.canonicalSearchField: {}",
          mappingProps == null ? null : mappingProps.canonicalSearchFields());
      if (mappingProps != null && mappingProps.canonicalSearchFields() != null
          && !mappingProps.canonicalSearchFields().isEmpty()) {
        Map<String, String> searchFieldsProp = mappingProps.canonicalSearchFields();
        // chcek if contain all keys
        log.debug("hasAllKey: searchFieldProp={}, canonicalKeyList={}", searchFieldsProp, canonicalKeyList);
        if (hasAllKey(searchFieldsProp, canonicalKeyList)) {
          Map<String, String> fieldMap = new HashMap<>();
          for (String key : canonicalKeyList) {
            fieldMap.put(key, searchFieldsProp.get(key));
          }
          log.debug("save key: {}", fieldMap);
          datasetIdLocalFields.put(Dataset.Id.of(datasetProps.id()), fieldMap);
        }
      }
    }
    return datasetIdLocalFields;
  }

  public static boolean hasAllKey(Map<String, String> toCheck, List<String> keys) {
    return toCheck != null && (keys == null || keys.stream().allMatch(toCheck::containsKey));
  }

  /**
   * Map CatalogProperties.DomainProps -> Domain. Adjust to your actual API/types.
   */
  private Domain toDomain(CatalogProperties.DomainProps p) {
    // Examples — pick the correct one for your model:

    // If p.id() returns String:
    // return new Domain(new Domain.Id(p.id()), p.name(), p.description(),
    // p.canonicalKeys());

    // If p.id() already returns Domain.Id:
    // return new Domain(p.id(), p.name(), p.description(), p.canonicalKeys());

    // Or, if you have a builder/factory:
    return Domain.of(
        new Domain.Id(p.id()), // or just p.id() if it's already Domain.Id
        p.name(),
        p.description(),
        p.canonicalKeys());
  }

  private Map<String, Map<String, Dataset.FieldRule>> toDomains(CatalogProperties.DatasetProps datasetProps) {
    if (datasetProps == null || datasetProps.domains() == null)
      return Map.of();

    Map<String, Map<String, FieldRuleProps>> source = datasetProps.domains();

    // Jackson will convert by matching property names; enum values are matched by
    // name.
    return objectMapper.convertValue(
        source,
        new TypeReference<Map<String, Map<String, Dataset.FieldRule>>>() {
        });
  }

  public Map<Dataset.Id, Dataset.FieldRule> findDatasetIdFieldRule(Domain.Id domainId, String canonicalKey) {
    Objects.requireNonNull(domainId, "domainId is required");
    Objects.requireNonNull(canonicalKey, "canonicalKey is required");

    Map<Dataset.Id, Dataset.FieldRule> out = new LinkedHashMap<>();

    // Iterate all datasets in catalog
    for (CatalogProperties.DatasetProps ds : properties.datasets()) {
      Map<String, Map<String, FieldRuleProps>> domains = ds.domains();
      if (domains == null || domains.isEmpty())
        continue;

      // Domain-level map: canonicalKey -> FieldRuleProps
      Map<String, FieldRuleProps> rules = domains.get(domainId.value());
      if (rules == null || rules.isEmpty())
        continue;

      FieldRuleProps ruleProps = rules.get(canonicalKey);
      if (ruleProps == null)
        continue;

      // Map props → domain model
      FieldRule rule = toDomain(ruleProps);
      out.put(Dataset.Id.of(ds.id()), rule); // use constructor if your Id has one
    }

    return out;
  }

  private FieldRule toDomain(FieldRuleProps p) {
    return new FieldRule(
        p.pointer(),
        p.coalesce() == null ? List.of() : p.coalesce(),
        p.compose(),
        (p.transform() == null ? List.<TransformRuleProps>of() : p.transform())
            .stream().map(this::toDomain).toList());
  }

  private TransformRule toDomain(TransformRuleProps t) {
    // Assuming enum names match; adjust mapping if needed
    TransformType type = TransformType.valueOf(t.type().name());
    return new TransformRule(type, t.args() == null ? List.of() : t.args());
  }

}
