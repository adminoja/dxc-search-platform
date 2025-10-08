package th.go.dxc.platform.search.domain.catalog.model;

import java.util.Map;

public record DatasetRoute(
    String path,
    String serviceId,
    Map<String, String> headers
) {

}
