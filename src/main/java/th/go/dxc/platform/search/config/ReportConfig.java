package th.go.dxc.platform.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import th.go.dxc.platform.search.adapter.out.cache.caffeine.CaffeineSnapshotCache;
import th.go.dxc.platform.search.adapter.out.report.pdf.GotenbergPdfRendererAdapter;
import th.go.dxc.platform.search.application.report.port.out.pdf.PdfRendererPort;
import th.go.dxc.platform.search.application.report.port.out.snapshot.SnapshotCachePort;

@Configuration
public class ReportConfig {

  @Bean
  public WebClient gotenbergClient(ReportProperties props) {
    return WebClient.builder()
        .baseUrl(props.gotenberg().baseUrl())
        .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
        .build();
  }

  // @Bean
  // public MinioClient minioClient(ReportProperties props) {
  //   return MinioClient.builder()
  //       .endpoint(props.minio().endpoint())
  //       .credentials(props.minio().accessKey(), props.minio().secretKey())
  //       .build();
  // }

  // @Bean
  // public ReportService reportService(SnapshotCachePort cache,
  //     ScopeHasher scopeHasher,
  //     TemplateEnginePort template,
  //     PdfRendererPort pdf,
  //     // CsvRendererPort csv,
  //     // ExcelRendererPort xlsx,
  //     ObjectStorePort objectStore,
  //     ReportProperties props) {
  //   return new ReportService(cache, scopeHasher, template, pdf
  //   // , csv, xlsx
  //       , objectStore, props);
  // }

  @Bean
  public SnapshotCachePort snapshotCachePort(ReportProperties cfg) {
    return new CaffeineSnapshotCache(cfg.snapshot());
  }

  // @Bean
  // public TemplateEnginePort templateEnginePort(ReportProperties props) {
  //   return new PebbleTemplateEngineAdapter(props);
  // }

  @Bean
  public PdfRendererPort pdfRendererPort(ReportProperties props) {
    return new GotenbergPdfRendererAdapter(props);
  }

  // @Bean
  // ThaiFormatUtil thaiFormatUtil() {
  //   return new ThaiFormatUtil();
  // }
  @Bean
  public StringTemplateResolver stringTemplateResolver() {
    StringTemplateResolver r = new StringTemplateResolver();
    r.setTemplateMode(TemplateMode.HTML); // important
    r.setCacheable(false);                // set true in prod + add your own cache policy
    r.setOrder(1);                        // ensure it runs before classpath/file resolvers
    return r;
  }
}
