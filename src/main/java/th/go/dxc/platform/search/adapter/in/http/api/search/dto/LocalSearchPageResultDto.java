package th.go.dxc.platform.search.adapter.in.http.api.search.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;
import th.go.dxc.platform.search.domain.common.value.DomainSort;

/**
 * SearchResultDto
 * - Minimal inputs, all other fields auto-derived
 * - Integrates with DomainPageRequest + DomainSort
 *
 * Required inputs (primary factory):
 *   - List<Item> content
 *   - DomainPageRequest pageRequest
 *   - long totalElements
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocalSearchPageResultDto(
  
    List<Item> content,

    // pageable container (Spring-like)
    Pageable pageable,

    // top-level page metadata
    int totalPages,
    long totalElements,
    boolean last,
    int number,              // current page (0-based)
    int size,                // page size
    int numberOfElements,    // content.size()
    Sort sort,               // duplicate of pageable.sort
    boolean first,
    boolean empty
) {

  // ===== Primary factory (Domain-first) =====
  public static LocalSearchPageResultDto of(
      List<Item> content,
      DomainPageRequest pageRequest,
      long totalElements
  ) {
    final List<Item> safeContent = content == null ? List.of() : List.copyOf(content);
    final long safeTotal = Math.max(totalElements, 0);

    // Pull page/size/sort/paged flags from DomainPageRequest
    final int page = Math.max(pageNumber(pageRequest), 0);
    final int size = Math.max(pageSize(pageRequest), 0);
    final boolean unpaged = isUnpaged(pageRequest);
    final boolean paged = !unpaged;

    // Build Sort from DomainSort (derive sorted/unsorted/empty)
    final Sort sort = fromDomain(sort(pageRequest));

    // totalPages
    final int totalPages;
    if (!paged) {
      totalPages = (safeTotal > 0) ? 1 : 0;
    } else {
      totalPages = (safeTotal == 0 || size == 0) ? 0 : (int) ((safeTotal + size - 1) / size);
    }

    final int numberOfElements = safeContent.size();
    final boolean first = page <= 0;
    final boolean last = totalPages == 0 || page >= (totalPages - 1);
    final boolean empty = numberOfElements == 0;

    final Pageable pageable = new Pageable(
        sort,
        size,
        page,
        (long) page * (long) size,
        paged,
        unpaged
    );

    return new LocalSearchPageResultDto(
        safeContent,
        pageable,
        totalPages,
        safeTotal,
        last,
        page,
        size,
        numberOfElements,
        sort,
        first,
        empty
    );
  }

  // ===== Convenience factory (raw numbers) =====
  public static LocalSearchPageResultDto of(
      List<Item> content,
      int page,
      int size,
      long totalElements
  ) {
    // Create a minimal DomainPageRequest shim when you don’t have the real one handy
    return of(content, DomainPageRequest.of(page, size, null), totalElements);
  }

  // ===== Nested DTOs =====

  public record Pageable(
      Sort sort,
      int pageSize,
      int pageNumber,
      long offset,
      boolean paged,
      boolean unpaged
  ) {}

  public record Sort(
      boolean sorted,
      boolean unsorted,
      boolean empty
  ) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonPropertyOrder({ "reportToken", "data" })
  public record Item(
      String reportToken,
      JsonNode data // required to be an object for most UIs, but kept generic JsonNode
  ) {
    public Item {
      if (data == null) {
        data = NullNode.getInstance();
      }
    }

    public static Item of(String reportToken, JsonNode data) {
      return new Item(reportToken, data);
    }
  }

  // @JsonInclude(JsonInclude.Include.NON_NULL)
  // @JsonPropertyOrder({ "reportToken" }) // show cacheToken first; rest (map) flattens in
  // public record Item(
  //     String reportToken,
  //     @JsonIgnore Map<String, Object> data
  // ) {
  //   public Item {
  //     data = data == null ? Collections.emptyMap() : data;
  //   }

  //   @JsonAnyGetter
  //   public Map<String, Object> flatData() {
  //     return data;
  //   }

  //   public static Item of(String cacheToken, Map<String, Object> data) {
  //     return new Item(cacheToken, data);
  //   }
  // }

  // ===== Domain adapters (adjust names here if your API differs) =====

  private static int pageNumber(DomainPageRequest pr) {
    // Prefer pr.pageNumber(); else try pr.getPageNumber() or pr.page()
    try { return pr.pageNumber(); } catch (NoSuchMethodError | UnsupportedOperationException e) {}
    try { return (int) pr.getClass().getMethod("getPageNumber").invoke(pr); } catch (Exception ignored) {}
    try { return (int) pr.getClass().getMethod("page").invoke(pr); } catch (Exception ignored) {}
    return 0;
  }

  private static int pageSize(DomainPageRequest pr) {
    try { return pr.pageSize(); } catch (NoSuchMethodError | UnsupportedOperationException e) {}
    try { return (int) pr.getClass().getMethod("getPageSize").invoke(pr); } catch (Exception ignored) {}
    try { return (int) pr.getClass().getMethod("size").invoke(pr); } catch (Exception ignored) {}
    return 0;
  }

  private static boolean isUnpaged(DomainPageRequest pr) {
    try { return pr.unpaged(); } catch (NoSuchMethodError | UnsupportedOperationException e) {}
    try { return (boolean) pr.getClass().getMethod("isUnpaged").invoke(pr); } catch (Exception ignored) {}
    return pageSize(pr) <= 0; // fallback heuristic
  }

  private static DomainSort sort(DomainPageRequest pr) {
    try { return pr.sort(); } catch (NoSuchMethodError | UnsupportedOperationException e) {}
    try { return (DomainSort) pr.getClass().getMethod("getSort").invoke(pr); } catch (Exception ignored) {}
    return null;
  }

  private static Sort fromDomain(DomainSort ds) {
    // Default derivation if DomainSort exposes booleans
    try {
      boolean sorted = ds != null && (boolean) ds.getClass().getMethod("isSorted").invoke(ds);
      boolean empty = ds == null || (boolean) ds.getClass().getMethod("isEmpty").invoke(ds);
      boolean unsorted = !sorted;
      // If DomainSort also has isUnsorted(), prefer it:
      try { unsorted = ds == null || (boolean) ds.getClass().getMethod("isUnsorted").invoke(ds); } catch (Exception ignored) {}
      return new Sort(sorted, unsorted, empty);
    } catch (Exception ignored) {
      // Fallback: derive from presence of orders list, if available
      boolean hasOrders = false;
      if (ds != null) {
        try {
          Object orders = ds.getClass().getMethod("orders").invoke(ds);
          if (orders instanceof List<?> list) hasOrders = !list.isEmpty();
        } catch (Exception ignored2) {}
      }
      boolean sorted = hasOrders;
      boolean unsorted = !hasOrders;
      boolean empty = !hasOrders;
      return new Sort(sorted, unsorted, empty);
    }
  }


}
