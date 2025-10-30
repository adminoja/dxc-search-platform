package th.go.dxc.platform.search.application.report.port.out.token;

import th.go.dxc.platform.search.domain.report.model.ReportToken;

public interface ReportTokenStorePort {
    public ReportToken nextToken();
}
