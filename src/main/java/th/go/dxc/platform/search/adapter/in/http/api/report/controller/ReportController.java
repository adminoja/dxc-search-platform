// src/main/java/th/go/dxc/platform/search/adapter/in/http/api/report/controller/ReportController.java
package th.go.dxc.platform.search.adapter.in.http.api.report.controller;

import java.time.ZoneId;
import java.util.Locale;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.report.model.PresignedUrl;
import th.go.dxc.platform.search.application.report.model.RenderOptions;
import th.go.dxc.platform.search.application.report.model.ReportFormat;
import th.go.dxc.platform.search.application.report.model.ReportToken;
import th.go.dxc.platform.search.application.report.port.in.PresignReportUseCase;
import th.go.dxc.platform.search.application.report.port.in.RenderReportUseCase;
import th.go.dxc.platform.search.domain.common.value.UserContext;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

  private final RenderReportUseCase renderReport;
  private final PresignReportUseCase presignReport;

  public ReportController(RenderReportUseCase renderReport, PresignReportUseCase presignReport) {
    this.renderReport = renderReport;
    this.presignReport = presignReport;
  }

  @PostMapping("/{token}")
  public Mono<ResponseEntity<byte[]>> download(
      @PathVariable String token,
      @RequestParam String template,
      @RequestParam(defaultValue = "report") String filename,
      @RequestParam(name = "format", required = false) String formatOverride,
      @RequestParam(defaultValue = "attachment") String disposition,
      @RequestHeader HttpHeaders headers,
      @AuthenticationPrincipal UserContext user) {

    ReportFormat fmt = decideFormat(formatOverride, headers);
    RenderOptions opts = new RenderOptions(Locale.getDefault(), ZoneId.systemDefault(), template, filename, true);

    return renderReport.render(user, ReportToken.of(token), fmt, opts)
        .map(out -> ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, out.contentType())
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.builder(("inline".equalsIgnoreCase(disposition) ? "inline" : "attachment"))
                    .filename(out.filename()).build().toString())
            .body(out.bytes()));
  }

  @PostMapping("/{token}/url")
  public Mono<ResponseEntity<PresignedUrl>> presign(
      @PathVariable String token,
      @RequestParam String template,
      @RequestParam(defaultValue = "report") String filename,
      @RequestParam(name = "format", required = false) String formatOverride,
      @RequestHeader HttpHeaders headers,
      @AuthenticationPrincipal UserContext user) {

    ReportFormat fmt = decideFormat(formatOverride, headers);
    RenderOptions opts = new RenderOptions(Locale.getDefault(), ZoneId.systemDefault(), template, filename, true);

    return presignReport.renderAndPresign(user, ReportToken.of(token), fmt, opts)
        .map(ResponseEntity::ok);
  }

  private static ReportFormat decideFormat(String override, HttpHeaders headers) {
    if (override != null && !override.isBlank()) return fromExt(override);
    for (MediaType mt : headers.getAccept()) {
      if (mt.includes(MediaType.APPLICATION_PDF)) return ReportFormat.PDF;
      if (mt.includes(MediaType.TEXT_HTML))       return ReportFormat.HTML;
      // if ("text".equals(mt.getType()) && "csv".equalsIgnoreCase(mt.getSubtype())) return ReportFormat.CSV;
      // if ("application".equals(mt.getType()) &&
      //     "vnd.openxmlformats-officedocument.spreadsheetml.sheet".equalsIgnoreCase(mt.getSubtype())) {
      //   return ReportFormat.XLSX;
      // }
    }
    return ReportFormat.PDF;
  }
  private static ReportFormat fromExt(String ext) {
    return switch (ext.toLowerCase()) {
      case "pdf"  -> ReportFormat.PDF;
      case "html" -> ReportFormat.HTML;
      // case "csv"  -> ReportFormat.CSV;
      // case "xlsx" -> ReportFormat.XLSX;
      default     -> throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported format: " + ext);
    };
  }
}
