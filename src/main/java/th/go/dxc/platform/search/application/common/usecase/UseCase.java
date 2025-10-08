package th.go.dxc.platform.search.application.common.usecase;

import reactor.core.publisher.Mono;

public interface UseCase<I,O> { Mono<O> execute(I input); }
