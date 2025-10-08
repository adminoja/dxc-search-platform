package th.go.dxc.platform.search.application.catalog.port.out;

import java.util.Optional;

import th.go.dxc.platform.search.domain.catalog.model.Organization;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

public interface OrganizationRepository {
    DomainPageResult<Organization> searchOrganizationByKeyword(String keyword, DomainPageRequest pageRequest);
    Optional<Organization> findOrganizationById(Organization.Id id);
}
