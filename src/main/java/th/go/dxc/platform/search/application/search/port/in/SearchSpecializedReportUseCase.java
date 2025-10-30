package th.go.dxc.platform.search.application.search.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.common.value.InvocationContext;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.search.model.SpecializedReportRequest;

public interface SearchSpecializedReportUseCase {
    public Mono<String> execute(Input input);
    record Input(SpecializedReportRequest req, UserContext userContext,InvocationContext invocationContext){
        public static Input of(SpecializedReportRequest req, UserContext userContext,InvocationContext invocationContext) {
            return new Input(req,userContext,invocationContext);
        }
    }
}
