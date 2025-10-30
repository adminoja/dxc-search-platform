package th.go.dxc.platform.search.adapter.out.report.token;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import th.go.dxc.platform.search.application.report.port.out.token.ReportTokenStorePort;
import th.go.dxc.platform.search.domain.report.model.ReportToken;

@AllArgsConstructor
@Component
public class InMemoryReportTokenStore implements ReportTokenStorePort {

    @Override
    public synchronized ReportToken nextToken() {
        return ReportToken.random();
    }

}
