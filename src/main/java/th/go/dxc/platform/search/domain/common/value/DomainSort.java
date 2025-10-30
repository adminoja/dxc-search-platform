package th.go.dxc.platform.search.domain.common.value;


import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
  

  public static DomainSort of(String sortSpec) {
        if (sortSpec == null || sortSpec.isBlank())
            return DomainSort.unsortedSort();

        String[] specs = sortSpec.split("[;|]");
        List<DomainSort.Order> orders = new ArrayList<>();

        for (String raw : specs) {
            String s = raw.trim();
            if (s.isEmpty())
                continue;

            DomainSort.Direction dir = DomainSort.Direction.ASC;
            String prop = s;

            // prefix style: -field / +field
            if (prop.startsWith("-")) {
                dir = DomainSort.Direction.DESC;
                prop = prop.substring(1).trim();
            } else if (prop.startsWith("+")) {
                prop = prop.substring(1).trim();
            } else {
                // delimiter style: field,ASC or field:DESC (case-insensitive)
                int comma = prop.indexOf(',');
                int colon = prop.indexOf(':');
                int sep = (comma >= 0 && colon >= 0) ? Math.min(comma, colon) : Math.max(comma, colon);
                if (sep >= 0) {
                    String field = prop.substring(0, sep).trim();
                    String d = prop.substring(sep + 1).trim().toUpperCase(Locale.ROOT);
                    if (!field.isEmpty()) {
                        prop = field;
                        dir = "DESC".equals(d) ? DomainSort.Direction.DESC : DomainSort.Direction.ASC;
                    }
                }
            }

            if (!prop.isEmpty()) {
                orders.add(DomainSort.Order.builder().property(prop).direction(dir).build());
            }
        }

        return orders.isEmpty() ? DomainSort.unsortedSort() : DomainSort.builder().orders(List.copyOf(orders)).build();
    }

}
