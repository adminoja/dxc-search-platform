package th.go.dxc.platform.search.application.report.port.in;

import java.util.Map;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.domain.common.value.UserContext;

public interface GeneratePdfFromReportIdUseCase {
  Mono<Result> generate(Query query);

  record Query(String reportId, UserContext userContext, String suggestedFileName) {}
  record Result(String objectKey, String presignedUrl, int sizeBytes, Map<String,Object> modelUsed) {}
}
