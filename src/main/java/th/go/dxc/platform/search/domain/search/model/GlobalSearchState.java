package th.go.dxc.platform.search.domain.search.model;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class GlobalSearchState {
  public final String runId;
  public volatile GlobalSearchStatus status = GlobalSearchStatus.QUEUED;
  public final Instant requestedAt = Instant.now();
  public volatile Instant startedAt;
  public volatile Instant updatedAt = Instant.now();
  public volatile Instant finishedAt;
  public volatile boolean cancelled = false;
  public final List<String> datasetIds;
  public final ConcurrentMap<String, LocalSearchTaskStatus> perDataset = new ConcurrentHashMap<>();
  public final AtomicInteger inProgress = new AtomicInteger();
  public final AtomicInteger completedOrFailed = new AtomicInteger();
  public final AtomicInteger failed = new AtomicInteger();
  public final AtomicInteger cancelledCnt = new AtomicInteger();

  public GlobalSearchState(String runId, List<String> datasetIds) {
    this.runId = runId;
    this.datasetIds = List.copyOf(datasetIds);
    datasetIds.forEach(ds -> perDataset.put(ds, new LocalSearchTaskStatus(ds, LocalSearchStatus.PENDING, null, null, null, false)));
  }

  public GlobalSearchProgress progress() {
    int total = datasetIds.size();
    int inProg = inProgress.get();
    int doneFailCancel = completedOrFailed.get();
    int canc = cancelledCnt.get();
    return GlobalSearchProgress.of(total, doneFailCancel - failed.get() - canc, failed.get(), inProg, canc);
  }
}