package th.go.dxc.platform.search.application.catalog.port.in;

import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

public interface ListDatasetUseCase {
    DomainPageResult<Dataset> query(Query query);
    record Query(String keyword,DomainPageRequest pageRequest) {}
}
