package th.go.dxc.platform.search.application.report.service;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.common.security.ScopeHasher;
import th.go.dxc.platform.search.application.report.model.ReportModel;
import th.go.dxc.platform.search.application.report.port.in.ReadReportModelUseCase;
import th.go.dxc.platform.search.application.report.port.out.snapshot.SnapshotCachePort;

@Service
@AllArgsConstructor
public class ReadReportModelUseCaseImpl implements ReadReportModelUseCase {
    private final ScopeHasher scopeHasher;
    private final SnapshotCachePort cache;

    @Override
    public Mono<ReadReportModelUseCase.Output> execute(ReadReportModelUseCase.Input in) {
        return Mono.defer(() -> {
            var u = in.user();
            String scopeHash = scopeHasher.scopeFor(u.userId(), u.tenantId(), u.realm());
            return Mono.justOrEmpty(cache.get(scopeHash, in.token()))
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Report snapshot not found")))
                    .map(snap -> new ReadReportModelUseCase.Output(ReportModel.from(in.token(), snap)));
        });
    }
}
