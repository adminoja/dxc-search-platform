package th.go.dxc.platform.search.application.catalog.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import th.go.dxc.platform.search.application.catalog.port.in.GetOrganizationUseCase;
import th.go.dxc.platform.search.application.catalog.port.in.ListOrganizationUseCase;
import th.go.dxc.platform.search.application.catalog.port.out.OrganizationRepository;
import th.go.dxc.platform.search.domain.catalog.model.Organization;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

@Service
@RequiredArgsConstructor
public class OrganizationService implements ListOrganizationUseCase,GetOrganizationUseCase {

    private final OrganizationRepository organizationRepository;

    @Override
    public DomainPageResult<Organization> query(ListOrganizationUseCase.Query query) {
        return organizationRepository.searchOrganizationByKeyword(query.keyword(), query.pageRequest());
    
    }
    @Override
    public Optional<Organization> query(GetOrganizationQuery query) {
        return organizationRepository.findOrganizationById(query.id());   
    }
}
