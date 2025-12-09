package th.go.dxc.platform.search.adapter.in.http.api.report.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.common.security.ScopeHasher;
import th.go.dxc.platform.search.application.report.port.in.ReadReportModelUseCase;
import th.go.dxc.platform.search.application.report.port.in.RenderHtmlReportUseCase;
import th.go.dxc.platform.search.application.report.port.in.RenderPdfReportUseCase;
import th.go.dxc.platform.search.application.report.port.in.RenderPdfReportUseCase.Options;
// ⬇️ adjust these to your actual packages
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.report.model.ReportModel;
import th.go.dxc.platform.search.domain.report.model.ReportToken;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/search/report")
public class ReportApi {

    private final RenderHtmlReportUseCase renderHtml;
    private final RenderPdfReportUseCase renderPdf;
    private final ReadReportModelUseCase readReportModel;
    private final ScopeHasher scopeHasher;

    /** Mapped model view (uses application port). */
    @GetMapping(value = "/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ReportModel>> readModel(
            @PathVariable("token") @NotBlank String token,
            @AuthenticationPrincipal UserContext user) {
        return readReportModel.execute(new ReadReportModelUseCase.Input(user, ReportToken.of(token)))
                .map(out -> ResponseEntity.ok(out.report()))
                .onErrorResume(IllegalArgumentException.class, e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping(value = "/{token}/html", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<ResponseEntity<String>> html(
            @PathVariable String token,
            @AuthenticationPrincipal UserContext user) {

        final ReportToken reportToken;
        try {
            reportToken = ReportToken.of(token); // validation here
        } catch (IllegalArgumentException ex) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        final String scopeHash = scopeHasher.scopeFor(user.userId(), user.tenantId(), user.realm());

        return renderHtml.execute(
                new RenderHtmlReportUseCase.Input(scopeHash, reportToken, user))
                .map(out -> ResponseEntity.ok()
                        .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                        .body(out.html()))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(IllegalArgumentException.class,
                        e -> Mono.just(ResponseEntity.notFound().build()));
    }

    /**
     * GET /reports/{token}/pdf
     * Uses only the report token and the UserContext to generate & stream the PDF.
     */
    @GetMapping(path = "/{token}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public Mono<ResponseEntity<byte[]>> pdf(
            @PathVariable("token") String token,
            @AuthenticationPrincipal UserContext user) {

        final ReportToken reportToken;
        try {
            reportToken = ReportToken.of(token);
        } catch (IllegalArgumentException ex) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        final String scopeHash = scopeHasher.scopeFor(user.userId(), user.tenantId(), user.realm());

        Options options = new Options(
                true, true, null, null,
                "25mm", "20mm", "14mm", "14mm",
                "1500ms", null);

        String filename = defaultFilename(reportToken.asString());

        var input = new RenderPdfReportUseCase.Input(
                scopeHash,
                reportToken,
                filename,
                options,
                user);

        return renderPdf.execute(input)
                .map(out -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, out.contentType())
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + out.filename() + "\"")
                        .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                        .body(out.pdfBytes()))
                .onErrorResume(IllegalArgumentException.class,
                        e -> Mono.just(ResponseEntity.notFound().build()));
    }
    // --- helpers --------------------------------------------------------------

    private static String defaultFilename(String token) {
        String base = token == null ? "" : token;
        // keep only safe chars for Content-Disposition filename
        base = base.replaceAll("[^A-Za-z0-9._-]", "");
        if (base.isBlank())
            base = "report";
        if (!base.toLowerCase().endsWith(".pdf"))
            base = base + ".pdf";
        return base;
    }
}
