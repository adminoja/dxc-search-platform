package th.go.dxc.platform.search.adapter.in.http.api.catalog.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import th.go.dxc.platform.search.adapter.in.http.api.catalog.dto.DatasetDto;
import th.go.dxc.platform.search.adapter.in.http.api.catalog.dto.OrganizationDto;
import th.go.dxc.platform.search.adapter.in.http.api.catalog.mapper.CatalogApiMapper;
import th.go.dxc.platform.search.adapter.in.http.api.common.api.dto.DtoPageResult;
import th.go.dxc.platform.search.adapter.in.http.api.common.api.dto.PageRequestDto;
import th.go.dxc.platform.search.adapter.in.http.api.common.api.mapper.ApiPageMapper;
import th.go.dxc.platform.search.adapter.in.http.api.common.api.mapper.PageResultDtoMapper;
import th.go.dxc.platform.search.application.catalog.port.in.ListDatasetUseCase;
import th.go.dxc.platform.search.application.catalog.port.in.ListOrganizationUseCase;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogApi {
    private final ListOrganizationUseCase listOrganizationUsecase;
    private final ListDatasetUseCase listDatasetUsecase;
    private final PageResultDtoMapper pageResultDtoMapper;

    @GetMapping("/organizations")
    public DtoPageResult<OrganizationDto> organizations( 
        @RequestParam(required = false) String keyword,
        @Valid @ParameterObject PageRequestDto pageRequestDto) {
    ListOrganizationUseCase.Query query = new ListOrganizationUseCase.Query(keyword,ApiPageMapper.toDomainPageRequest(pageRequestDto));
    return pageResultDtoMapper.toDto(listOrganizationUsecase.query(query),CatalogApiMapper::toDto);    
    }

    @GetMapping("/datasets")
    public DtoPageResult<DatasetDto> datasets( 
        @RequestParam(required = false) String keyword,
        @Valid @ParameterObject PageRequestDto pageRequestDto) {
    ListDatasetUseCase.Query query = new ListDatasetUseCase.Query(keyword,ApiPageMapper.toDomainPageRequest(pageRequestDto));
    return pageResultDtoMapper.toDto(listDatasetUsecase.query(query),CatalogApiMapper::toDto);    
    }
}
