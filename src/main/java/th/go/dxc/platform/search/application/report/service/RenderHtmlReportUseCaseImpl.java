package th.go.dxc.platform.search.application.report.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import th.go.dxc.platform.search.application.common.report.ThaiFormatUtil;
import th.go.dxc.platform.search.application.report.model.ReportDataSnapshot;
import th.go.dxc.platform.search.application.report.port.in.RenderHtmlReportUseCase;
import th.go.dxc.platform.search.application.report.port.out.snapshot.SnapshotCachePort;
import th.go.dxc.platform.search.application.report.port.out.template.HtmlTemplateRendererPort;

@Service
public class RenderHtmlReportUseCaseImpl implements RenderHtmlReportUseCase {

    private final SnapshotCachePort snapshots;
    private final HtmlTemplateRendererPort templates;
    private final ThaiFormatUtil thai;
    private final ResourceLoader resources;

    public RenderHtmlReportUseCaseImpl(SnapshotCachePort snapshots,
                                       HtmlTemplateRendererPort templates,
                                       ThaiFormatUtil thai,
                                       ResourceLoader resources) {
        this.snapshots = snapshots;
        this.templates = templates;
        this.thai = thai;
        this.resources = resources;
    }

    @Override
    public Mono<Output> execute(Input input) {
        return Mono.fromCallable(() -> {
                    ReportDataSnapshot snap = snapshots.get(input.scope(), input.token());
                    if (snap == null) return null;

                    Map<String, Object> model = new HashMap<>();
                    model.put("doc", snap.data());
                    model.put("thai", thai);

                    // watermark controls (template reads wmText/wmAngle)
                    model.put("wmText", "DXC • INTERNAL USE ONLY • CONFIDENTIAL");
                    model.put("wmAngle", -45);

                    // footer meta (Map access via meta['key'] in template)
                    Map<String, Object> meta = new HashMap<>();
                    String qrText = "https://search.dxc.go.th/verify?vt="+input.token().value(); // or any string you want to encode
                    String qrBase64 = QrUtil.qrPngBase64(qrText, 256, 1);
                    meta.put("qrBase64", qrBase64);
                    try {
                        var printed = snap.createdAt().atZone(ZoneId.systemDefault()).toLocalDate();
                        meta.put("printedAtBe", thai.dateBe(printed, "d/MM/yyyy"));
                    } catch (Exception e) {
                        meta.put("printedAtBe", "-");
                    }
                    meta.put("printedBy", "-");
                    model.put("meta", meta);

                    // single-file HTML: inline fonts + css + logo (Gotenberg-safe)
                    model.put("embedAssets", true);
                    model.put("inlineStyle", buildInlineStyle());
                    model.put("inlineLogo",
                            dataUri("classpath:/report/templates/pdf/_common/img/dxc-logo.svg", "image/svg+xml"));

                    // optional: helps when you preview via a controller with assets served
                    model.put("assetBase", "/report/templates/pdf/");

                    String templateBase = snap.datasetId(); // e.g. "dop.probationers"
                    String html = templates.render(templateBase + "/index", model);
                    return new Output(html);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(o -> o == null ? Mono.empty() : Mono.just(o));
    }

    /* ============================== helpers ============================== */

    /** Build the inline <style>: shared print.css + inlined @font-face for TH Sarabun New. */
    private String buildInlineStyle() throws IOException {
        String printCss = readText("classpath:/report/templates/pdf/_common/print.css");

        String regular = dataUri("classpath:/report/templates/pdf/_common/fonts/thsarabunnew/THSarabunNew.woff2", "font/woff2");
        String bold    = dataUri("classpath:/report/templates/pdf/_common/fonts/thsarabunnew/THSarabunNew Bold.woff2", "font/woff2");
        String italic  = dataUri("classpath:/report/templates/pdf/_common/fonts/thsarabunnew/THSarabunNew Italic.woff2", "font/woff2");
        String bi      = dataUri("classpath:/report/templates/pdf/_common/fonts/thsarabunnew/THSarabunNew BoldItalic.woff2", "font/woff2");

        StringBuilder faces = new StringBuilder();
        if (regular != null) faces.append(fontFace("TH Sarabun New", "normal", "400", regular, "woff2"));
        if (bold    != null) faces.append(fontFace("TH Sarabun New", "normal", "700", bold, "woff2"));
        if (italic  != null) faces.append(fontFace("TH Sarabun New", "italic", "400", italic, "woff2"));
        if (bi      != null) faces.append(fontFace("TH Sarabun New", "italic", "700", bi, "woff2"));

        // base family rule (harmless duplicate, keeps stack if print.css is edited)
        String baseFamily = """
            body, table, th, td, div, span, p, h1, h2, h3, h4, h5, h6 {
              font-family: 'TH Sarabun New','THSarabunNew','Sarabun','Noto Sans Thai','Tahoma',sans-serif;
            }
            """;

        // IMPORTANT: put @font-face AFTER print.css so data-URI sources override relative ones
        return baseFamily + "\n" + printCss + "\n/* inlined TH Sarabun New */\n" + faces;
    }

    private String fontFace(String family, String style, String weight, String dataUrl, String format) {
        return """
            @font-face{
              font-family:'%s';
              font-style:%s;
              font-weight:%s;
              font-display:swap;
              src:url('%s') format('%s');
            }
            """.formatted(family, style, weight, dataUrl, format);
    }

    /** Convert a classpath resource to a data URI (base64). Returns null if not found. */
    private String dataUri(String location, String mime) throws IOException {
        Resource r = resources.getResource(location);
        if (!r.exists()) return null;
        try (InputStream in = r.getInputStream()) {
            byte[] bytes = StreamUtils.copyToByteArray(in);
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
    }

    private String readText(String location) throws IOException {
        Resource r = resources.getResource(location);
        try (InputStream in = r.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }
}
