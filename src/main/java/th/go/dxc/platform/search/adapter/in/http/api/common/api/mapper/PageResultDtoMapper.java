package th.go.dxc.platform.search.adapter.in.http.api.common.api.mapper;

import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import th.go.dxc.platform.search.adapter.in.http.api.common.api.dto.DtoPageResult;
import th.go.dxc.platform.search.domain.common.value.DomainPageResult;

@Component
public class PageResultDtoMapper {

  /** Map with element conversion: S(domain) -> T(dto). */
  public <S, T> DtoPageResult<T> toDto(DomainPageResult<S> src, Function<S, T> elementMapper) {
    if (src == null) {
      return new DtoPageResult<>(List.of(), 0, 0, 0L, 0, 0, false, false, true, true, true);
    }

    // adjust accessors if your DomainPageResult uses getters (getPage(), getSize(), ...)
    int page = src.number();
    int size = src.size();
    long totalElements = src.totalElements();

    List<T> content = src.content() == null ? List.of()
        : src.content().stream().map(elementMapper).toList();

    int numberOfElements = content.size();

    // ceil(totalElements / size) with guards
    int totalPages = (size <= 0) ? 0 : (int) ((totalElements + (long) size - 1) / size);

    boolean hasPrevious = page > 0;
    boolean hasNext = totalPages > 0 && (page + 1) < totalPages;
    boolean first = page == 0;
    boolean last = totalPages == 0 || (page + 1) == totalPages;
    boolean empty = numberOfElements == 0;

    return new DtoPageResult<>(
        content, page, size, totalElements, totalPages, numberOfElements,
        hasNext, hasPrevious, first, last, empty
    );
  }

  /** Convenience: identity element mapping when domain == dto element type. */
  public <T> DtoPageResult<T> toDto(DomainPageResult<T> src) {
    return toDto(src, Function.identity());
  }
}
