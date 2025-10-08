package th.go.dxc.platform.search.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "platform.search.cache")
public class SearchCacheProperties {

  /**
   * TTL for cached record details (e.g., item rows used to render PDFs).
   * Examples in YAML: "30m", "PT30M", "1800s".
   */
  @NotNull
  private Duration detailTtl = Duration.ofMinutes(30); // default

  public Duration getDetailTtl() { return detailTtl; }
  public void setDetailTtl(Duration detailTtl) { this.detailTtl = detailTtl; }
}
