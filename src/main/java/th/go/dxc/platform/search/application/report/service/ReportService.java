// src/main/java/th/go/dxc/platform/search/application/report/service/ReportService.java
package th.go.dxc.platform.search.application.report.service;

import java.time.Duration;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import th.go.dxc.platform.search.application.common.security.ScopeHasher;
import th.go.dxc.platform.search.application.report.model.PresignedUrl;
import th.go.dxc.platform.search.application.report.model.RenderOptions;
import th.go.dxc.platform.search.application.report.model.RenderedReport;
import th.go.dxc.platform.search.application.report.model.ReportDataSnapshot;
import th.go.dxc.platform.search.application.report.model.ReportFormat;
import th.go.dxc.platform.search.application.report.model.ReportModel;
import th.go.dxc.platform.search.application.report.model.ReportToken;
import th.go.dxc.platform.search.application.report.port.in.PresignReportUseCase;
import th.go.dxc.platform.search.application.report.port.in.ReadReportModelUseCase;
import th.go.dxc.platform.search.application.report.port.in.RenderReportUseCase;
import th.go.dxc.platform.search.application.report.port.out.pdf.PdfRendererPort;
import th.go.dxc.platform.search.application.report.port.out.snapshot.SnapshotCachePort;
import th.go.dxc.platform.search.application.report.port.out.storage.ObjectStorePort;
import th.go.dxc.platform.search.application.report.port.out.storage.ObjectStorePort.PutResult;
import th.go.dxc.platform.search.application.report.port.out.template.TemplateEnginePort;
import th.go.dxc.platform.search.config.ReportProperties;
import th.go.dxc.platform.search.domain.common.value.UserContext;

public class ReportService implements RenderReportUseCase, PresignReportUseCase, ReadReportModelUseCase {

  private final SnapshotCachePort cache;
  private final ScopeHasher scopeHasher;
  private final TemplateEnginePort template;
  private final PdfRendererPort pdf;
  // private final CsvRendererPort csv;
  // private final ExcelRendererPort xlsx;
  private final ObjectStorePort objectStore; // for presign use case
  private final ReportProperties props; // to read minio.presignSeconds

  public ReportService(SnapshotCachePort cache,
      ScopeHasher scopeHasher,
      TemplateEnginePort template,
      PdfRendererPort pdf,
      // CsvRendererPort csv,
      // ExcelRendererPort xlsx,
      ObjectStorePort objectStore,
      ReportProperties props) {
    this.cache = cache;
    this.scopeHasher = scopeHasher;
    this.template = template;
    this.pdf = pdf;
    // this.csv = csv;
    // this.xlsx = xlsx;
    this.objectStore = objectStore;
    this.props = props;
  }

  @Override
  public Mono<RenderedReport> render(UserContext user,
      ReportToken token,
      ReportFormat format,
      RenderOptions opts) {
    return Mono.fromCallable(() -> {
      String scope = scopeHasher.scopeFor(
          user.userId(),
          (user.tenantId() == null || user.tenantId().isBlank()) ? "default" : user.tenantId(),
          firstNonBlank(user.realm(), user.issuer(), "local"));

      ReportDataSnapshot snap = opts.singleUse()
          ? cache.getAndEvict(scope, token)
          : cache.get(scope, token);

      if (snap == null) {
        throw new IllegalArgumentException("Token expired or invalid");
      }

      return switch (format) {
        case HTML -> {
          byte[] html = template.renderHtml(opts.templateName(), snap.data());
          yield new RenderedReport(safeName(opts.filenameHint(), "html"), ReportFormat.HTML.contentType, html);
        }
        case PDF -> {
          byte[] html = template.renderHtml(opts.templateName(), snap.data());
          byte[] pdfBytes = pdf.fromHtml(html);
          yield new RenderedReport(safeName(opts.filenameHint(), "pdf"), ReportFormat.PDF.contentType, pdfBytes);
        }
        // case CSV -> {
        // byte[] csvBytes = csv.fromSnapshot(snap);
        // yield new RenderedReport(safeName(opts.filenameHint(), "csv"),
        // ReportFormat.CSV.contentType, csvBytes);
        // }
        // case XLSX -> {
        // byte[] xlsxBytes = xlsx.fromSnapshot(snap);
        // yield new RenderedReport(safeName(opts.filenameHint(), "xlsx"),
        // ReportFormat.XLSX.contentType, xlsxBytes);
        // }
      };
    }).subscribeOn(Schedulers.boundedElastic());
  }

  @Override
  public Mono<PresignedUrl> renderAndPresign(UserContext user,
      ReportToken token,
      ReportFormat format,
      RenderOptions opts) {
    return render(user, token, format, opts)
        .map(out -> {
          String objectName = format.name().toLowerCase() + "/" +
              fileBase(opts.filenameHint()) + "-" + token.value() + "." + format.ext;
          Duration ttl = Duration.ofSeconds(props.minio().presignSeconds());
          PutResult putResult = objectStore.putAndPresign(out.bytes(), objectName, out.contentType(), ttl);
          String url = putResult.url();
          return new PresignedUrl(url, objectName, out.contentType(), (int) ttl.getSeconds());
        });
  }

  @Override
  public Mono<ReadReportModelUseCase.Output> execute(ReadReportModelUseCase.Input in) {
    return Mono.defer(() -> {
      var u = in.user();
      String scopeHash = scopeHasher.scopeFor(u.userId(), u.tenantId(), u.realm());
      return Mono.justOrEmpty(cache.get(scopeHash, in.token()))
          .switchIfEmpty(Mono.error(new IllegalArgumentException("Report snapshot not found")))
          .map(snap -> new ReadReportModelUseCase.Output(ReportModel.from(in.token(), snap)));
    });
  }
  private static String safeName(String hint, String ext) {
    return fileBase(hint) + "." + ext;
  }

  private static String fileBase(String hint) {
    String base = (hint == null || hint.isBlank()) ? "report" : hint.toLowerCase().replaceAll("[^a-z0-9._-]+", "-");
    return base;
  }

  private static String firstNonBlank(String... vals) {
    for (String v : vals)
      if (v != null && !v.isBlank())
        return v;
    return null;
  }
}
