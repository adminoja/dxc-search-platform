package th.go.dxc.platform.search.domain.common.value;

import java.util.List;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Builder(toBuilder = true)
@Accessors(fluent = true) // -> content(), totalElements(), totalPages()...
public class DomainPageResult<T> {

  @Singular("content")
  List<T> content;

  DomainPageRequest pageable;

  int  totalPages;
  long totalElements;

  int  number;             // page number (alias of pageable.pageNumber())
  int  size;               // page size   (alias of pageable.pageSize())
  int  numberOfElements;   // content.size()

  boolean first;
  boolean last;
  boolean empty;

  // ---------- Factory: minimal inputs; everything else auto-derived ----------
  public static <T> DomainPageResult<T> of(
      List<T> content,
      DomainPageRequest pageRequest,
      long totalElements
  ) {
    final List<T> safe = content == null ? List.of() : List.copyOf(content);
    final long safeTotal = Math.max(totalElements, 0);
    final int page = Math.max(pageRequest.pageNumber(), 0);
    final int size = Math.max(pageRequest.pageSize(), 0);
    final boolean paged = pageRequest.paged();

    final int totalPages;
    if (!paged) {
      totalPages = safeTotal > 0 ? 1 : 0;
    } else if (safeTotal == 0 || size == 0) {
      totalPages = 0;
    } else {
      totalPages = (int) ((safeTotal + size - 1) / size); // ceil
    }

    final int numberOfElements = safe.size();
    final boolean first = page <= 0;
    final boolean last  = totalPages == 0 || page >= (totalPages - 1);
    final boolean empty = numberOfElements == 0;

    return DomainPageResult.<T>builder()
        .content(safe)
        .pageable(pageRequest)
        .totalPages(totalPages)
        .totalElements(safeTotal)
        .number(page)
        .size(size)
        .numberOfElements(numberOfElements)
        .first(first)
        .last(last)
        .empty(empty)
        .build();
  }

  // Convenience: unpaged result
  public static <T> DomainPageResult<T> unpaged(List<T> content) {
    return of(content, DomainPageRequest.unpagedRequest(), content == null ? 0 : content.size());
  }

  // Sort passthrough (handy in mappers)
  public DomainSort sort() {
    return pageable != null ? pageable.sort() : DomainSort.unsortedSort();
  }
}
