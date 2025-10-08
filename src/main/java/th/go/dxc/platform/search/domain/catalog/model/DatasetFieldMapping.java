package th.go.dxc.platform.search.domain.catalog.model;

import java.util.List;

public record DatasetFieldMapping(
    List<String> searchFields,
    List<String> summaryFields,
    List<String> naturalKeyFields
) {

}
