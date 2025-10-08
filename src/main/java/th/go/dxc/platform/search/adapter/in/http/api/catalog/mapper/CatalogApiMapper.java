package th.go.dxc.platform.search.adapter.in.http.api.catalog.mapper;

import th.go.dxc.platform.search.adapter.in.http.api.catalog.dto.DatasetDto;
import th.go.dxc.platform.search.adapter.in.http.api.catalog.dto.OrganizationDto;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.catalog.model.Organization;

public class CatalogApiMapper {
    public static OrganizationDto toDto(Organization org) {
        return new OrganizationDto(
            org.id().value(),
            org.name(),
            org.description()
        );
    }
    public static Organization toDomain(OrganizationDto dto) {
        return new Organization(
            new Organization.Id(dto.id()),
            dto.name(),
            dto.description()
        );
    }
    public static Organization.Id toDomainId(String id) {
        return new Organization.Id(id);
    }   
    public static DatasetDto toDto(Dataset dataset) {
        return new DatasetDto(
            dataset.id().value(),
            dataset.name(),
            dataset.description()
        );
    }   
    // public static Dataset toDomain(DatasetDto dto) {
    //     return new Dataset(
    //         new Dataset.Id(dto.id()),
    //         dto.title(),
    //         dto.description()
    //     );
    // }
}
