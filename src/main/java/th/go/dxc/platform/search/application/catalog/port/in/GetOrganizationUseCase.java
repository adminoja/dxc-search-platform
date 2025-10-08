package th.go.dxc.platform.search.application.catalog.port.in;

import java.util.Optional;

import th.go.dxc.platform.search.domain.catalog.model.Organization;

public interface GetOrganizationUseCase {
    Optional<Organization> query(GetOrganizationQuery query);
    record GetOrganizationQuery(Organization.Id id) {}
}
