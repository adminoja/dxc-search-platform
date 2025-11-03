package th.go.dxc.platform.search.application.search.service.special;
// application/search/service/CancelSpecializedRunExecutor.java

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.search.port.in.CancelSpecializedRunUseCase;
import th.go.dxc.platform.search.application.search.port.out.GlobalSearchStorePort;

@Component
@RequiredArgsConstructor
public class CancelSpecializedRunExecutor implements CancelSpecializedRunUseCase {
  private final GlobalSearchStorePort store;
  @Override public Mono<Boolean> execute(Input input) { return store.cancel(input.runId()); }
}
