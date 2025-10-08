package th.go.dxc.platform.search.adapter.out.report.storage;

import io.minio.*;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

import th.go.dxc.platform.search.application.report.port.out.storage.ObjectStorePort;
import th.go.dxc.platform.search.config.ReportProperties;

@Component
public class MinioObjectStorage implements ObjectStorePort {
  private final MinioClient minio;
  private final ReportProperties props;

  public MinioObjectStorage(MinioClient minioClient, ReportProperties props) {
    this.minio = minioClient;
    this.props = props;
    ensureBucket();
  }

  @Override
  public PutResult putAndPresign(byte[] data, String objectKey, String contentType, Duration presignTtl) {
    try (var bais = new ByteArrayInputStream(data)) {
      minio.putObject(PutObjectArgs.builder()
          .bucket(props.minio().bucket())
          .object(objectKey)
          .contentType(contentType)
          .stream(bais, data.length, -1)
          .build());
      String url = minio.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
          .bucket(props.minio().bucket())
          .object(objectKey)
          .method(Method.GET)
          .expiry(presignTtl.toSecondsPart(), TimeUnit.SECONDS)
          .build());
      return new PutResult(objectKey, url, data.length);
    } catch (Exception e) {
      throw new RuntimeException("MinIO put/presign failed", e);
    }
  }

  private void ensureBucket() {
    try {
      String b = props.minio().bucket();
      if (!minio.bucketExists(BucketExistsArgs.builder().bucket(b).build())) {
        minio.makeBucket(MakeBucketArgs.builder().bucket(b).build());
      }
    } catch (Exception e) {
      throw new RuntimeException("MinIO bucket init failed", e);
    }
  }
}
