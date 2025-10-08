package th.go.dxc.platform.search.adapter.out.catalog.config;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import th.go.dxc.platform.search.application.catalog.port.out.DatasetRepository;
import th.go.dxc.platform.search.application.catalog.port.out.OrganizationRepository;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.catalog.model.DatasetFieldMapping;
import th.go.dxc.platform.search.domain.catalog.model.DatasetRoute;
import th.go.dxc.platform.search.domain.catalog.model.Organization;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

@Repository
@RequiredArgsConstructor
public class CatalogConfigRepository implements OrganizationRepository, DatasetRepository {
  private final CatalogProperties properties;

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
        new DatasetRoute(datasetProps.route().path(), datasetProps.route().serviceId(), datasetProps.route().headers()),
        new DatasetFieldMapping(
            datasetProps.mapping() != null && datasetProps.mapping().searchFields() != null
                ? datasetProps.mapping().searchFields()
                : List.of(),
            datasetProps.mapping() != null && datasetProps.mapping().summaryFields() != null
                ? datasetProps.mapping().summaryFields()
                : List.of(),
            datasetProps.mapping() != null && datasetProps.mapping().naturalKeyFields() != null
                ? datasetProps.mapping().naturalKeyFields()
                : List.of()));

    // If you adopted LocalizedText (TH/EN), use:
    // var name = new LocalizedText(it.getNameTh(), it.getNameEn());
    // return new Organization(new OrganizationId(it.getId()), name, it.isActive());
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
    return 
        properties.datasets().stream()
            .filter(d -> d.id() != null && d.id().equalsIgnoreCase(id.value()))
            .findFirst()
            .map(this::toDomain);
  }

}
