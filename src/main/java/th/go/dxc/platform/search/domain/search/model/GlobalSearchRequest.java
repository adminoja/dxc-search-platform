package th.go.dxc.platform.search.domain.search.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.DomainPageRequest;

public record GlobalSearchRequest(
    Instant requestedAt,
    List<LocalSearchRequest> requests,

    // Optional knobs (not required by the runner, but useful to carry along)
    Aggregation aggregation,
    Grouping grouping,
    Ordering ordering,
    Duration perDatasetTimeout,
    boolean failFast,
    Map<String,Object> sharedCriteria
) {
    public enum Aggregation { NONE, UNION, INTERSECT }
  public enum Grouping { BY_DATASET, BY_DOMAIN }
  public enum Ordering { BY_TIME, BY_SCORE }
public GlobalSearchRequest {
    Objects.requireNonNull(requestedAt, "requestedAt");
    Objects.requireNonNull(requests, "requests");
    if (requests.isEmpty()) throw new IllegalArgumentException("requests must not be empty");
    // Make immutable defensive copy
    requests = List.copyOf(requests);

    // Defaults for optional knobs
    aggregation = (aggregation == null) ? Aggregation.NONE : aggregation;
    grouping = (grouping == null) ? Grouping.BY_DATASET : grouping;
    ordering = (ordering == null) ? Ordering.BY_TIME : ordering;
    perDatasetTimeout = (perDatasetTimeout == null) ? Duration.ofSeconds(10) : perDatasetTimeout;
  }

  /** Convenience: same criteria/page across many datasets, with defaults. */
  public static GlobalSearchRequest simpleNow(Collection<Dataset.Id> datasetIds,
                                              Map<String,Object> sharedCriteria,
                                              DomainPageRequest pageRequest) {
    var now = Instant.now();
    var locals = datasetIds.stream()
        .map(id -> new LocalSearchRequest(now, id, sharedCriteria, pageRequest))
        .toList();
    return new GlobalSearchRequest(now, locals,
        Aggregation.NONE, Grouping.BY_DATASET, Ordering.BY_TIME,
        Duration.ofSeconds(10), false,
        sharedCriteria);
  }

  /** Convenience: full control factory. */
  public static GlobalSearchRequest of(Instant requestedAt,
                                       Collection<Dataset.Id> datasetIds,
                                       Map<String,Object> sharedCriteria,
                                       DomainPageRequest pageRequest,
                                       Aggregation aggregation,
                                       Grouping grouping,
                                       Ordering ordering,
                                       Duration perDatasetTimeout,
                                       boolean failFast) {
    var locals = datasetIds.stream()
        .map(id -> new LocalSearchRequest(requestedAt, id, sharedCriteria, pageRequest))
        .toList();
    return new GlobalSearchRequest(requestedAt, locals, aggregation, grouping, ordering, perDatasetTimeout, failFast, sharedCriteria);
  }

  public static GlobalSearchRequest of(List<LocalSearchRequest> requests){
    return new GlobalSearchRequest(Instant.now(), requests, null, null, null, null, false, null);
  }
}
