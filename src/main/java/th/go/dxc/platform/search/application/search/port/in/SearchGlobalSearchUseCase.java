package th.go.dxc.platform.search.application.search.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.common.value.InvocationContext;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchRequest;

public interface SearchGlobalSearchUseCase {
    public Mono<String> execute(Input input);
    record Input(GlobalSearchRequest req, UserContext userContext,InvocationContext invocationContext){
        public static Input of(GlobalSearchRequest req, UserContext userContext,InvocationContext invocationContext) {
            return new Input(req,userContext,invocationContext);
        }
    }
}
