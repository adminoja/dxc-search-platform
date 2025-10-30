package th.go.dxc.platform.search.application.search.port.in;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.search.model.GlobalSearchResult;

public interface GetGlobalSearchResultUseCase {

    public Mono<GlobalSearchResult> execute(Input input);
    public record Input(String runId) {
        public static Input of(String runId) {
            return new Input(runId);
        }
    }
}
