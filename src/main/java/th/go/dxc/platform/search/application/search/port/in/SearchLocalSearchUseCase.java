package th.go.dxc.platform.search.application.search.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.common.value.InvocationContext;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.search.model.LocalSearchRequest;
import th.go.dxc.platform.search.domain.search.model.LocalSearchResult;

public interface SearchLocalSearchUseCase {
  Mono<LocalSearchResult> execute(Input query);

  record Input(LocalSearchRequest request, Boolean isReport, UserContext userContext,
      InvocationContext invocationContext) {
    public static Input of(LocalSearchRequest request, Boolean isReport, UserContext userContext,
        InvocationContext invocationContext) {
      return new Input(request, isReport, userContext, invocationContext);
    }

    public static Input of(LocalSearchRequest request, UserContext userContext, InvocationContext invocationContext) {
      return new Input(request, false, userContext, invocationContext);
    }
  }
}