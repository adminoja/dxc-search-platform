package th.go.dxc.platform.search.adapter.in.http.api.common.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "cacheToken" }) // show token first
public record CachedItemDto<T>(
        String cacheToken,
        @JsonUnwrapped T data
) {}