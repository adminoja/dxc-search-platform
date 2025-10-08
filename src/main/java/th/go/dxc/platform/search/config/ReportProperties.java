package th.go.dxc.platform.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "platform.report")
public record ReportProperties(
    Templates templates,
    Gotenberg gotenberg,
    Minio minio,
    Snapshot snapshot
) {
  public ReportProperties {
    if (templates == null) {
      templates = new Templates("classpath:report-templates", 300,
          List.of(
              "report.css",
              "fonts/NotoSansThai-Regular.ttf",
              "fonts/NotoSansThai-Bold.ttf"
          ));
    }
    if (gotenberg == null) gotenberg = new Gotenberg("http://gotenberg:3000", "/forms/chromium/convert/html", 20_000);
    if (minio == null) minio = new Minio("http://minio:9000", "minioadmin", "minioadmin", "dxc-reports", 300);
    if (snapshot == null) snapshot = new Snapshot(Duration.ofMinutes(30), 200_000L, "v1", "detail");
  }

  /**
   * Template files root. Use either:
   *  - {@code classpath:report-templates} (recommended)
   *  - or {@code file:/opt/dxc/templates}
   */
  public static record Templates(
      String location,
      int cacheSeconds,
      List<String> assets        // relative paths to attach to Gotenberg (e.g. CSS, fonts)
  ) {
    public Templates {
      if (location == null || location.isBlank()) location = "classpath:report-templates";
      if (assets == null) assets = List.of("report.css");
      if (cacheSeconds < 0) cacheSeconds = 0;
    }
  }

  public static record Gotenberg(
      String baseUrl,
      String endpoint,
      int timeoutMs
  ) {
    public Gotenberg {
      if (baseUrl == null || baseUrl.isBlank()) baseUrl = "http://gotenberg:3000";
      if (endpoint == null || endpoint.isBlank()) endpoint = "/forms/chromium/convert/html";
      if (timeoutMs <= 0) timeoutMs = 20_000;
    }
  }

  public static record Minio(
      String endpoint,
      String accessKey,
      String secretKey,
      String bucket,
      int presignSeconds
  ) {
    public Minio {
      if (endpoint == null || endpoint.isBlank()) endpoint = "http://minio:9000";
      if (accessKey == null) accessKey = "minioadmin";
      if (secretKey == null) secretKey = "minioadmin";
      if (bucket == null || bucket.isBlank()) bucket = "dxc-reports";
      if (presignSeconds <= 0) presignSeconds = 300;
    }
  }

  /** Settings for the snapshot token cache (Caffeine now, Redis later). */
  public static record Snapshot(
      Duration ttl,
      long caffeineMaxSize,
      String keyVersion,
      String keyNamespace
  ) {
    public Snapshot {
      if (ttl == null) ttl = Duration.ofMinutes(30);
      if (caffeineMaxSize <= 0) caffeineMaxSize = 200_000L;
      if (keyVersion == null || keyVersion.isBlank()) keyVersion = "v1";
      if (keyNamespace == null || keyNamespace.isBlank()) keyNamespace = "detail";
    }
  }
}
