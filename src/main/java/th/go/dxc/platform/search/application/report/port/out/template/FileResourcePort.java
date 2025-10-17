package th.go.dxc.platform.search.application.report.port.out.template;

import java.io.InputStream;

import reactor.core.publisher.Mono;

public interface FileResourcePort {
  /** Unified, scheme-aware fetch: classpath:/, file:/, gitlab:/ or gitlab://<ref>/ */
  Mono<InputStream> getInputStream(String location);

  /** Optional helper for dataset-based paths, keeps your old call sites short. */
  default Mono<InputStream> getInputStream(String datasetId, String relativePath) {
    // Build a classpath-like location by default; caller can still pass gitlab:/... to override.
    return getInputStream("classpath:" + relativeToDataset(datasetId, relativePath));
  }

  private static String relativeToDataset(String datasetId, String file) {
    if (datasetId == null || datasetId.isBlank()) return file;
    return datasetId.endsWith("/") ? datasetId + file : datasetId + "/" + file;
  }
}
