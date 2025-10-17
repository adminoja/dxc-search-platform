package th.go.dxc.platform.search.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "platform.report")
public record ReportProperties(
    Template template,
    Gotenberg gotenberg,
    // Minio minio,
    Snapshot snapshot,
    Signing sign) {
  public ReportProperties {
    if (template == null) {
      template = new Template("classpath:report-templates", null, null, null, null, null, null,
          new HashMap<String, Map<String, String>>());
    }
    if (gotenberg == null)
      gotenberg = new Gotenberg("http://gotenberg:3000", "/forms/chromium/convert/html", Duration.ofSeconds(60));
    // if (minio == null) minio = new Minio("http://minio:9000", "minioadmin",
    // "minioadmin", "dxc-reports", 300);
    if (snapshot == null)
      snapshot = new Snapshot(Duration.ofMinutes(30), 200_000L, "v1", "detail");
  }

  /**
   * Template files root. Use either:
   * - {@code classpath:report-templates} (recommended)
   * - or {@code file:/opt/dxc/templates}
   */
  public static record Template(
      String location,
      String css, // relative or absolute
      FontProps font,
      ImageProps logo,
      String header,
      String footer,
      GitLab gitlab,
      Map<String, Map<String, String>> code) {
    public Template {
      if (location == null || location.isBlank())
        location = "classpath:report-templates";
      if (css == null || css.isBlank())
        css = "print.css";
      if (font == null)
        font = new FontProps(null, null, null, null, null, null);
      if (logo == null)
        logo = new ImageProps(null, null);
      if (header == null)
        header = "header.html";
      if (footer == null)
        footer = "footer.html";
      if (gitlab == null)
        gitlab = new GitLab(null,null,null, null, null, null, null, null, null);
      if (code == null)
        code = new HashMap<>();
    }

    public String scheme() {
      int i = location.indexOf(':');
      return i >= 0 ? location.substring(0, i) : "";
    }

    public String basePath() {
      int i = location.indexOf(':');
      return i >= 0 ? location.substring(i + 1) : location;
    }

    /**
     * Join `location` and a possibly-relative child. Absolute values pass through.
     */
    public String resolve(String child) {
      if (child == null || child.isBlank())
        return child;
      // treat anything with "<scheme>:" as absolute (classpath:, file:, gitlab:,
      // http:, etc.)
      int colon = child.indexOf(':');
      if (colon > 0 && colon < child.indexOf('/'))
        return child;

      // relative: join with base, preserving the scheme
      String base = location;
      if (!base.endsWith("/"))
        base = base + "/";
      String rel = child.startsWith("/") ? child.substring(1) : child;
      return base + rel;
    }

    public static record FontProps(
        String family,
        String mime,
        String regular,
        String bold,
        String italic,
        String boldItalic) {
      public FontProps {
        if (mime == null || mime.isBlank())
          mime = "font/woff2";
        if (family == null || family.isBlank())
          family = "TH Sarabun New";
      }
    }

    public static record ImageProps(
        String path,
        String mime) {
      public ImageProps {
        if (path == null)
          path = "logo.svg";
        if (mime == null)
          mime = "image/svg+xml";
      }
    }
  }

  public record GitLab(
      String baseUrl,
      String apiVersion,
      Integer projectId,
      // String baseRawUrl, // e.g. https://gitlab.example.com/group/project/-/raw
      String ref, // e.g. master
      DeployToken deployToken, // optional: basic auth for raw/API
      String tokenHeader, // optional: e.g. "PRIVATE-TOKEN"
      String token, // optional: value for tokenHeader
      Integer connectTimeoutMs,
      Integer readTimeoutMs) {
    public record DeployToken(String username, String password) {
    }

    // Compact canonical constructor: apply defaults / normalization
    public GitLab {
      baseUrl = Objects.requireNonNullElse(baseUrl, "");
      apiVersion = (apiVersion==null||apiVersion.isBlank())?"v4":apiVersion;
      projectId = Objects.requireNonNullElse(projectId, 0);
      ref = (ref == null || ref.isBlank()) ? "master" : ref;
      // baseRawUrl = Objects.requireNonNullElse(baseRawUrl, "");
      tokenHeader = (tokenHeader==null||tokenHeader.isBlank())?"PRIVATE-TOKEN": tokenHeader;
      // sensible timeouts if not provided
      connectTimeoutMs = (connectTimeoutMs == null || connectTimeoutMs <= 0) ? 3000 : connectTimeoutMs;
      readTimeoutMs = (readTimeoutMs == null || readTimeoutMs <= 0) ? 5000 : readTimeoutMs;

      // leave deployToken and personalAccessToken nullable (optional auth)
      // no change needed: deployToken = deployToken; personalAccessToken =
      // personalAccessToken;
    }

    public boolean useDeployToken() {
      return deployToken != null
          && deployToken.username() != null
          && !deployToken.username().isBlank();
    }

    public boolean usePat() {
      return token != null && !token.isBlank();
    }

  }

  public static record Gotenberg(
      String baseUrl,
      String endpoint,
      Duration timeout) {
    public Gotenberg {
      if (baseUrl == null || baseUrl.isBlank())
        baseUrl = "http://gotenberg:3000";
      if (endpoint == null || endpoint.isBlank())
        endpoint = "/forms/chromium/convert/html";
      if (timeout == null)
        timeout = Duration.ofSeconds(60);
    }
  }

  // public static record Minio(
  // String endpoint,
  // String accessKey,
  // String secretKey,
  // String bucket,
  // int presignSeconds) {
  // public Minio {
  // if (endpoint == null || endpoint.isBlank())
  // endpoint = "http://minio:9000";
  // if (accessKey == null)
  // accessKey = "minioadmin";
  // if (secretKey == null)
  // secretKey = "minioadmin";
  // if (bucket == null || bucket.isBlank())
  // bucket = "dxc-reports";
  // if (presignSeconds <= 0)
  // presignSeconds = 300;
  // }
  // }

  /** Settings for the snapshot token cache (Caffeine now, Redis later). */
  public static record Snapshot(
      Duration ttl,
      long caffeineMaxSize,
      String keyVersion,
      String keyNamespace) {
    public Snapshot {
      if (ttl == null)
        ttl = Duration.ofMinutes(30);
      if (caffeineMaxSize <= 0)
        caffeineMaxSize = 200_000L;
      if (keyVersion == null || keyVersion.isBlank())
        keyVersion = "v1";
      if (keyNamespace == null || keyNamespace.isBlank())
        keyNamespace = "detail";
    }
  }

  public record Signing(
      Boolean enabled, // default true
      String keystorePath, // /opt/keys/domain-signing-cert.p12
      String storePassword, // changeit
      String keyPassword, // changeit
      String alias, // optional; auto-pick first if blank
      String signerName, // "DXC Search Platform"
      String signerLocation, // "Bangkok, Thailand"
      String signerReason // "Official Report Verification"
  ) {
    public Signing {
      if (enabled == null)
        enabled = Boolean.TRUE;
    }
  }

}
