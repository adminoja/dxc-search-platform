package th.go.dxc.platform.search.domain.catalog.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)                  // name() instead of getName()
@RequiredArgsConstructor                   // ctor for final @NonNull fields
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Organization {
    @EqualsAndHashCode.Include
    private final Organization.Id id;
    private final String name;
    private final String description;

    public record Id(String value) { }  
}
