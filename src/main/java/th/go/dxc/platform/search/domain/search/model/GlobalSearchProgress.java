package th.go.dxc.platform.search.domain.search.model;

public record GlobalSearchProgress(int total, int done, int failed, int inProgress, int cancelled, int percent) {
  public static GlobalSearchProgress of(int total, int done, int failed, int inProg, int cancelled) {
    int pct = total == 0 ? 100 : Math.min(100, Math.round(100f * (done + failed + cancelled) / total));
    return new GlobalSearchProgress(total, done + failed + cancelled, failed, inProg, cancelled, pct);
  }
}
