package th.go.dxc.platform.search.adapter.in.http.api.report.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.common.security.ScopeHasher;
import th.go.dxc.platform.search.application.report.model.ReportToken;
import th.go.dxc.platform.search.application.report.port.in.RenderPdfUseCase;
import th.go.dxc.platform.search.application.report.port.in.RenderPdfUseCase.Options;
// ⬇️ adjust these to your actual packages
import th.go.dxc.platform.search.domain.common.value.UserContext;

@Slf4j
@RestController
@RequestMapping("/api/reports")
public class ReportPdfController {

  private final RenderPdfUseCase renderPdf;
  private final ScopeHasher scopeHasher;

  public ReportPdfController(RenderPdfUseCase renderPdf, ScopeHasher scopeHasher) {
    this.renderPdf = renderPdf;
    this.scopeHasher = scopeHasher;
  }

  /**
   * GET /reports/{token}/pdf
   * Uses only the report token and the UserContext to generate & stream the PDF.
   */
  @GetMapping(path = "/{token}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public Mono<ResponseEntity<byte[]>> pdf(
      @PathVariable("token") @NotBlank String token,
      @AuthenticationPrincipal @NonNull UserContext user
  ) {
    final String scopeHash = scopeHasher.scopeFor(user.userId(), user.tenantId(), user.realm());


    // Sensible defaults; no extra query params or headers needed.
    Options options = new Options(
        true,   // printBackground
        true,   // preferCssPageSize
        null,   // landscape
        null,   // scale
        "25mm", "20mm", "14mm", "14mm", // margins
        "1500ms", // waitDelay (gives time for any QR/JS rendering; safe if unused)
        null      // waitForExpression
    );

    var input = new RenderPdfUseCase.Input(
        scopeHash,
        new ReportToken(token),
        defaultFilename(token),
        options
    ,user);
    log.debug("pdf: input = {}",input);
    return renderPdf.execute(input)
        .map(out -> ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, out.contentType())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + out.filename() + "\"")
            .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
            .body(out.pdfBytes()));
  }

  // --- helpers --------------------------------------------------------------

  private static String defaultFilename(String token) {
    String base = token == null ? "" : token;
    // keep only safe chars for Content-Disposition filename
    base = base.replaceAll("[^A-Za-z0-9._-]", "");
    if (base.isBlank()) base = "report";
    if (!base.toLowerCase().endsWith(".pdf")) base = base + ".pdf";
    return base;
  }
}
