package th.go.dxc.platform.search.adapter.in.http.api.common.api.mapper;

import java.util.ArrayList;
import java.util.List;

import th.go.dxc.platform.search.adapter.in.http.api.common.api.dto.PageRequestDto;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainSort;

public final class ApiPageMapper {
  private ApiPageMapper() {}

  public static DomainPageRequest toDomainPageRequest(PageRequestDto dto) {
    int p = dto.page() == null ? 0 : Math.max(0, dto.page());
    int s = dto.size() == null ? DomainPageRequest.DEFAULT_SIZE : Math.max(1, dto.size());
    DomainSort sort = SortMapper.parse(dto.sort()); // your HTTP-layer parser
    return DomainPageRequest.of(p, s, sort);
  }

    public static DomainSort toDomainSort(String sort) {
    if (sort == null || sort.isBlank()) return DomainSort.unsortedSort();
    String[] parts = sort.split("[;]");
    List<DomainSort.Order> orders = new ArrayList<>(parts.length);
    for (String p : parts) {
      String s = p.trim();
      if (s.isEmpty()) continue;
      String[] kv = s.split(",", 2);
      String prop = kv[0].trim();
      if (prop.isEmpty()) continue;
      DomainSort.Direction dir = DomainSort.Direction.ASC;
      if (kv.length > 1) {
        String d = kv[1].trim().toUpperCase();
        if ("DESC".equals(d)) dir = DomainSort.Direction.DESC;
      }
      orders.add(DomainSort.Order.builder().property(prop).direction(dir).build());
    }
    return orders.isEmpty() ? DomainSort.unsortedSort() : DomainSort.builder().orders(orders).build();
  }

  public static String toDtoSort(DomainSort sort) {
    if (sort == null || sort.unsorted()) return "";
    return sort.orders().stream()
        .map(o -> o.property() + "," + o.direction().name().toLowerCase())
        .reduce((a, b) -> a + ";" + b)
        .orElse("");
  }
}