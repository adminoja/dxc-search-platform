package th.go.dxc.platform.search.domain.common.value;


import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;

@Value
@Builder(toBuilder = true)
@Accessors(fluent = true) // -> orders(), sorted(), unsorted(), empty()
public class DomainSort {

  @Singular
  List<Order> orders;

  // Derived flags
  public boolean sorted()   { return orders != null && !orders.isEmpty(); }
  public boolean unsorted() { return !sorted(); }
  public boolean empty()    { return orders == null || orders.isEmpty(); }

  @Value
  @Builder
  @Accessors(fluent = true) // -> property(), direction()
  public static class Order {
    String property;
    Direction direction;
  }

  public enum Direction { ASC, DESC }
  // Convenience factory
  public static DomainSort unsortedSort() {
    return DomainSort.builder().build();
  }
}
