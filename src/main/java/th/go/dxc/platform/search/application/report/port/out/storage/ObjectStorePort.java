package th.go.dxc.platform.search.application.report.port.out.storage;

import java.time.Duration;

/** Outbound port to store a rendered artifact and return a presigned GET URL. */
public interface ObjectStorePort {

  /**
   * Store an object and return a presigned GET URL.
   * @param bytes       object bytes (PDF/HTML/CSV/XLSX)
   * @param objectName  path/key in the bucket, e.g. "pdf/report-123.pdf"
   * @param contentType MIME type, e.g. "application/pdf"
   * @param presignTtl  URL validity window
   * @return            absolute URL string
   */
  public PutResult putAndPresign(byte[] bytes, String objectName, String contentType, Duration presignTtl);

  public record PutResult(
    String objectKey,
    String url,
    Integer length    
  ){}
}
