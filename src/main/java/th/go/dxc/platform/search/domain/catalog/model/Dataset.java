package th.go.dxc.platform.search.domain.catalog.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)                  // name() instead of getName()
@RequiredArgsConstructor                   // ctor for final @NonNull fields
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Dataset {
    @EqualsAndHashCode.Include
    private final Dataset.Id id;
    private final String name;
    private final String description;
    private final Organization.Id ownerOrgId;
    private final DatasetRoute route;
    private final DatasetFieldMapping mapping;
    public record Id(String value) { }
}
