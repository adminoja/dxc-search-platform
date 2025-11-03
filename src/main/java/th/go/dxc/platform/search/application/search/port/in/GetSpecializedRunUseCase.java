package th.go.dxc.platform.search.application.search.port.in;

import reactor.core.publisher.Mono;

public interface GetSpecializedRunUseCase {
  Mono<ListSpecializedRunsUseCase.RunRow> execute(Input input);
  record Input(String runId) { public static Input of(String runId){ return new Input(runId);} }
}
