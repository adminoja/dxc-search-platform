package th.go.dxc.platform.search.domain.common.value;


import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Builder(toBuilder = true)
@Accessors(fluent = true)                // pageNumber(), pageSize(), sort(), ...
public class DomainPageRequest {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 200; // tune as you wish
  int pageNumber;                        // zero-based
  int pageSize;                          // <= 0 means "unpaged"
  DomainSort sort;                       // may be null -> unsorted

  // -------- Derived flags/values --------
  public Boolean unpaged() { return pageSize <= 0; }
  public Boolean paged()   { return !unpaged(); }
  public Long    offset()  { return (long) Math.max(pageNumber, 0) * Math.max(pageSize, 0); }

  // -------- Basic navigation (size/sort preserved) --------
  /** Next page (no bounds check). */
  public DomainPageRequest next() {
    return withPage(pageNumber + 1);
  }
  /** Previous page, clamped at 0. */
  public DomainPageRequest prev() {
    return withPage(Math.max(pageNumber - 1, 0));
  }
  /** Previous page or first (alias of prev()). */
  public DomainPageRequest previousOrFirst() {
    return prev();
  }
  /** First page (0). */
  public DomainPageRequest first() {
    return withPage(0);
  }
  /** Replace only the page number (keeps size/sort). */
  public DomainPageRequest withPage(int newPage) {
    return this.toBuilder().pageNumber(Math.max(newPage, 0)).build();
  }

  // -------- Navigation that needs bounds information --------
  /** Last page by known total pages (clamped). totalPages may be 0. */
  public DomainPageRequest lastByTotalPages(int totalPages) {
    final int last = Math.max(0, totalPages - 1);
    return withPage(last);
  }
  /** Next page or last page if out of bounds. */
  public DomainPageRequest nextOrLastByTotalPages(int totalPages) {
    final int last = Math.max(0, totalPages - 1);
    return withPage(Math.min(pageNumber + 1, last));
  }

  // -------- “Has next/previous” helpers --------
  /** Has previous page (purely local). */
  public boolean hasPrevious() {
    return pageNumber > 0;
  }
  /** Has next page using known total pages. */
  public boolean hasNextByTotalPages(int totalPages) {
    if (unpaged()) return false;
    if (totalPages <= 0) return false;
    return pageNumber < (totalPages - 1);
  }
  /** Has next page using total elements (avoids precomputing totalPages). */
  public boolean hasNextByTotalElements(long totalElements) {
    if (unpaged() || pageSize <= 0) return false;
    // next page exists if next page's first element index < totalElements
    long nextFirstIndex = ((long) (pageNumber + 1)) * (long) pageSize;
    return nextFirstIndex < Math.max(totalElements, 0);
  }

  // -------- Convenience factories --------
  public static DomainPageRequest of(int pageNumber, int pageSize, DomainSort sort) {
    return DomainPageRequest.builder().pageNumber(Math.max(pageNumber, 0)).pageSize(pageSize).sort(sort).build();
  }
  public static DomainPageRequest of(int pageNumber, int pageSize) {
    return of(pageNumber, pageSize, null);
  }
  public static DomainPageRequest unpagedRequest(DomainSort sort) {
    return of(0, 0, sort);
  }
  public static DomainPageRequest unpagedRequest() {
    return unpagedRequest(null);
  }
}

