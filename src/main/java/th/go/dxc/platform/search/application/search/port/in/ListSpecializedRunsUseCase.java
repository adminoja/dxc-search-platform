package th.go.dxc.platform.search.application.search.port.in;

import reactor.core.publisher.Mono;

public interface ListSpecializedRunsUseCase {
  Mono<Result> execute(Input input);

  record Input(String userId, int limit, int offset,
               String statusFilter, String reportIdFilter, String query) {
    public static Input of(String userId, int limit, int offset,
                           String statusFilter, String reportIdFilter, String query) {
      return new Input(userId, limit, offset, statusFilter, reportIdFilter, query);
    }
  }

  record Result(java.util.List<RunRow> items, long total) {}

  // compact row DTO for the table
  record RunRow(
      String runId,
      String reportId,
      String subjectNin,
      java.time.Instant requestedAt,
      java.time.Instant startedAt,
      java.time.Instant finishedAt,
      th.go.dxc.platform.search.domain.search.model.GlobalSearchStatus status,
      Long durationMs,
      Integer resultCount,
      Progress progress,
      String viewUrl,
      String pdfUrl
  ) {
    public record Progress(Integer completed, Integer failed, Integer inProgress, Integer total) {}
  }
}

