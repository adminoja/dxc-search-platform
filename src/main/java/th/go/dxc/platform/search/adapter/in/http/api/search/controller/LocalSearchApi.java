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
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.LocalSearchPageResultDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.dto.LocalSearchRequestDto;
import th.go.dxc.platform.search.adapter.in.http.api.search.mapper.LocalSearchApiMapper;
import th.go.dxc.platform.search.application.search.port.in.SearchLocalSearchUseCase;
import th.go.dxc.platform.search.domain.common.value.InvocationContext;
import th.go.dxc.platform.search.domain.common.value.UserContext;

@RestController
@RequestMapping(path = "/api/search/local", produces = MediaType.APPLICATION_JSON_VALUE)
public class LocalSearchApi {

    private final SearchLocalSearchUseCase localSearch;

    public LocalSearchApi(SearchLocalSearchUseCase localSearch) {
        this.localSearch = localSearch;
    }

    @PostMapping(path = "/{datasetId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<LocalSearchPageResultDto> search(@PathVariable String datasetId,
            @RequestBody LocalSearchRequestDto body,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId,
            @AuthenticationPrincipal UserContext user, // <-- principal is UserContext now            
            Locale requestLocale) {

        // 1) Build UserContext from JWT + Authentication (Keycloak/RH-SSO)
        // UserContext user = UserContextMapper.fromJwt(jwt);

        // 2) Map DTO -> Domain request (criteria is Map<String,Object>)
        var domainReq = LocalSearchApiMapper.toDomain(datasetId, body);
        InvocationContext invo = InvocationContext.userTopLevel(
                InvocationContext.FeatureType.LOCAL_SEARCH,
                datasetId,
                correlationId != null ? correlationId : "",
                "");
        Boolean isReport = true;
        // 3) Call use case and map Domain -> DTO (current SearchResultDto fields only)
        return localSearch.execute(new SearchLocalSearchUseCase.Input(domainReq, isReport, user,invo))
                .map(LocalSearchApiMapper::toPageResultDto);
    }


}
