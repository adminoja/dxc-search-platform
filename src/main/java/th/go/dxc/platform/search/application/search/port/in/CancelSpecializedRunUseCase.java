package th.go.dxc.platform.search.application.search.port.in;

// application/search/port/in/CancelSpecializedRunUseCase.java

import reactor.core.publisher.Mono;

public interface CancelSpecializedRunUseCase {
  Mono<Boolean> execute(Input input);
  record Input(String runId) { public static Input of(String runId){ return new Input(runId);} }
}

