package th.go.dxc.platform.search.adapter.out.common.jpa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainSort;

public final class PageableMapper {
  private PageableMapper() {}

  public static Pageable toSpring(DomainPageRequest dpr) {
    Sort s = toSpringSort(dpr.sort());
    return PageRequest.of(dpr.pageNumber(), dpr.pageSize(), s);
  }

  public static DomainPageRequest toDomain(Pageable pageable) {
    if (pageable == null) {
      return DomainPageRequest.of(0, DomainPageRequest.DEFAULT_SIZE);
    }
    DomainSort ds = toDomainSort(pageable.getSort());
    return DomainPageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ds);
  }

  public static Sort toSpringSort(DomainSort ds) {
    if (ds == null || ds.unsorted()) return Sort.unsorted();
    return Sort.by(ds.orders().stream()
        .map(o -> new Sort.Order(
            o.direction() == DomainSort.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC,
            o.property()))
        .toList());
  }

  public static DomainSort toDomainSort(Sort sort) {
    if (sort == null || sort.isUnsorted()) return DomainSort.unsortedSort();
    return DomainSort.builder()
    .orders(sort.stream()
        .map(o -> 
          DomainSort.Order.builder()
          .property(o.getProperty())
          .direction( o.isAscending() ? DomainSort.Direction.ASC : DomainSort.Direction.DESC)
          .build()
          )
        .toList()
      ).build();
  }
}
