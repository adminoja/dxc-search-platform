package th.go.dxc.platform.search.adapter.in.http.api.common.api.dto;

public record PageRequestDto(Integer page, Integer size, String sort) {
    public PageRequestDto() { this(0, 20, null); } // defaults if not provided
 }