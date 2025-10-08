package th.go.dxc.platform.search.application.catalog.port.out;

import java.util.Optional;

import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

public interface DatasetRepository {
    DomainPageResult<Dataset> searchDatasetByKeyword(String keyword, DomainPageRequest pageRequest);
    Optional<Dataset> findDatasetById(Dataset.Id id);
}
