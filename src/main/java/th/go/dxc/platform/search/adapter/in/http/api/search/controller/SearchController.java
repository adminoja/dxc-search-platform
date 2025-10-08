package th.go.dxc.platform.search.adapter.in.http.api.search.controller;

import java.util.Locale;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.SearchRequestDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.SearchResultDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.mapper.SearchApiMapper;
import th.go.dxc.platform.search.application.search.port.in.LocalSearchUseCase;
import th.go.dxc.platform.search.domain.common.value.UserContext;

@RestController
@RequestMapping(path = "/api/search", produces = MediaType.APPLICATION_JSON_VALUE)
public class SearchController {

    private final LocalSearchUseCase localSearch;

    public SearchController(LocalSearchUseCase localSearch) {
        this.localSearch = localSearch;
    }

    @PostMapping(path = "/{datasetId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<SearchResultDto> search(@PathVariable String datasetId,
            @RequestBody SearchRequestDto body,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId,
            // Authentication authentication,
            // @AuthenticationPrincipal Jwt jwt,
            @AuthenticationPrincipal UserContext user, // <-- principal is UserContext now            
            Locale requestLocale) {

        // 1) Build UserContext from JWT + Authentication (Keycloak/RH-SSO)
        // UserContext user = UserContextMapper.fromJwt(jwt);

        // 2) Map DTO -> Domain request (criteria is Map<String,Object>)
        var domainReq = SearchApiMapper.toDomain(datasetId, body);

        // 3) Call use case and map Domain -> DTO (current SearchResultDto fields only)
        return localSearch.query(new LocalSearchUseCase.Query(domainReq, user))
                .map(SearchApiMapper::toDto);
    }


}
