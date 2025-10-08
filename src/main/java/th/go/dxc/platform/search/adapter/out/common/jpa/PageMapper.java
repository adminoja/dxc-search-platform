// adapter/common/jpa/PageAdapters.java
package th.go.dxc.platform.search.adapter.out.common.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;
import th.go.dxc.platform.search.domain.common.value.DomainSort;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PageMapper {
  private PageMapper() {}

  // Spring Page<E> -> DomainPageResult<T>
  public static <E,T> DomainPageResult<T> toDomain(Page<E> page, Function<E,T> map) {
    List<T> content = page.getContent().stream().map(map).collect(Collectors.toList());
    return DomainPageResult.of(
        content,
        DomainPageRequest.of(page.getNumber(), page.getSize(),mapSort(page.getSort())),
        page.getTotalElements()
        // if you added sort to DomainPageResult, pass mapSort(page.getSort())
    );
  }

  // DomainPageResult<T> -> Spring Page<T> (when an adapter must return Page)
  public static <T> Page<T> toSpring(DomainPageResult<T> dpr, Pageable pageable) {
    return new PageImpl<>(dpr.content(), pageable, dpr.totalElements());
  }

  // Spring Sort -> DomainSort
  public static DomainSort mapSort(Sort sort) {
    if (sort == null || sort.isUnsorted()) return DomainSort.unsortedSort();
    var orders = sort.stream()
        .map(o -> DomainSort.Order.builder().property(o.getProperty()).direction(o.isAscending() ? DomainSort.Direction.ASC : DomainSort.Direction.DESC).build())
        .collect(Collectors.toList());
    return DomainSort.builder().orders(orders).build();
  }

  // DomainSort -> Spring Sort
  public static Sort mapSort(DomainSort ds) {
    if (ds == null || ds.unsorted()) return Sort.unsorted();
    var orders = ds.orders().stream()
        .map(o -> new Sort.Order(
            o.direction() == DomainSort.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC,
            o.property()))
        .toList();
    return Sort.by(orders);
  }
}
