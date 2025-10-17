package th.go.dxc.platform.search.application.report.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import th.go.dxc.platform.search.application.catalog.port.out.DatasetRepository;
// import th.go.dxc.platform.search.application.common.report.ThaiFormatUtil;
import th.go.dxc.platform.search.application.report.model.ReportDataSnapshot;
import th.go.dxc.platform.search.application.report.port.in.RenderHtmlReportUseCase;
import th.go.dxc.platform.search.application.report.port.out.snapshot.SnapshotCachePort;
import th.go.dxc.platform.search.config.ReportProperties;
import th.go.dxc.platform.search.domain.catalog.model.Dataset;
import th.go.dxc.platform.search.domain.common.value.UserContext;

@AllArgsConstructor
@Slf4j
@Service
public class RenderHtmlReportUseCaseImpl implements RenderHtmlReportUseCase {
    private static final Duration IO_TIMEOUT = Duration.ofSeconds(5);
    private final DatasetRepository datasetRepository;
    private final TemplateIO io;
    private final SnapshotCachePort snapshots;
    // private final HtmlTemplateRendererPort templates;
    private final FormatUtil format;
    private final ReportProperties props;
    private final SpringWebFluxTemplateEngine engine;

    @Override
    public Mono<Output> execute(Input input) {
        log.trace("RenderHtmlReportUseCaseImpl.execute: Input={}", input);

        return Mono.fromCallable(() -> {
            log.trace("Start: input={}", input);
            ReportProperties.Template t = props.template();
            ReportDataSnapshot snap = snapshots.get(input.scope(), input.token());
            log.trace("snap={}", snap);
            if (snap == null)
                return null;

            Map<String, Object> model = new HashMap<>();
            model.put("doc", snap.data());
            // model.put("thai", thai);
            model.put("format", format);
            // watermark controls (template reads wmText/wmAngle)
            log.trace("watermark controls (template reads wmText/wmAngle)");

            model.put("wmText", watermarkText(input.user()));
            model.put("wmAngle", -45);

            // footer meta (Map access via meta['key'] in template)
            log.trace("footer meta (Map access via meta['key'] in template)");
            Map<String, Object> meta = new HashMap<>();
            String qrText = "https://search.dxc.go.th/verify?vt=" + input.token().value(); // or any string you want to
                                                                                           // encode
            String qrBase64 = QrUtil.qrPngBase64(qrText, 256, 1);
            meta.put("qrBase64", qrBase64);
            try {
                var printed = snap.createdAt().atZone(ZoneId.systemDefault()).toLocalDate();
                // meta.put("printedAtBe", thai.dateBe(printed, "d/MM/yyyy"));
                meta.put("printedAtBe", format.date(printed, "d/MM/yyyy"));
            } catch (Exception e) {
                meta.put("printedAtBe", "-");
            }
            meta.put("printedBy", input.user().username());
            Dataset dataset = datasetRepository.findDatasetById(new Dataset.Id(snap.datasetId())).orElseThrow();
            meta.put("dataset", dataset);
            model.put("meta", meta);

            // single-file HTML: inline fonts + css + logo (Gotenberg-safe)
            log.trace("single-file HTML: inline fonts + css + logo (Gotenberg-safe)");
            model.put("embedAssets", true);
            model.put("inlineStyle", buildInlineStyle());
            model.put("inlineLogo", io.dataUriOrEmpty(t.resolve(t.logo().path()), t.logo().mime(), IO_TIMEOUT));

            // optional: helps when you preview via a controller with assets served
            model.put("assetBase", t.basePath());
            String templateLocation = props.template().resolve(snap.datasetId());
            log.debug("templateLocation = {}", templateLocation);
            String htmlTemplate = io.readTextSync(templateLocation + "/index.html", IO_TIMEOUT);
            log.trace("templates.render {}, model = {}", templateLocation, model == null ? null : model.size());
            // String html = templates.render(templateBase + "/index", model);
            String html = render(htmlTemplate, Locale.of("th", "TH"), model);
            log.trace("return {}", html == null ? null : html.length());
            return new Output(html);
        })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(o -> o == null ? Mono.empty() : Mono.just(o));
    }

    /* ============================== helpers ============================== */

    /**
     * Build the inline <style>: shared print.css + inlined @font-face for TH
     * Sarabun New.
     */
    private String buildInlineStyle() {
        var t = props.template(); // inject your root props and get TemplateProps

        // 1) print.css (relative to base location)
        String printCss = io.readTextSync(t.resolve(t.css()), IO_TIMEOUT);

        // 2) font family + mime
        var f = t.font();
        String family = f.family();
        String mime = f.mime();
        String format = mimeToFormat(mime);

        // 3) data URIs for fonts (relative paths resolved to base; absolute pass
        // through)
        String regular = io.dataUriOrEmpty(t.resolve(f.regular()), mime);
        String bold = io.dataUriOrEmpty(t.resolve(f.bold()), mime);
        String italic = io.dataUriOrEmpty(t.resolve(f.italic()), mime);
        String bi = io.dataUriOrEmpty(t.resolve(f.boldItalic()), mime);

        StringBuilder faces = new StringBuilder();
        if (!regular.isBlank())
            faces.append(fontFace(family, "normal", "400", regular, format));
        if (!bold.isBlank())
            faces.append(fontFace(family, "normal", "700", bold, format));
        if (!italic.isBlank())
            faces.append(fontFace(family, "italic", "400", italic, format));
        if (!bi.isBlank())
            faces.append(fontFace(family, "italic", "700", bi, format));

        String baseFamily = """
                body, table, th, td, div, span, p, h1, h2, h3, h4, h5, h6 {
                  font-family: '%s','THSarabunNew','Sarabun','Noto Sans Thai','Tahoma',sans-serif;
                }
                """.formatted(family);

        return baseFamily + "\n" + printCss + (faces.length() > 0 ? "\n/* inlined fonts */\n" + faces : "");
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

    private String mimeToFormat(String mime) {
        String m = mime == null ? "" : mime.toLowerCase();
        if (m.contains("woff2"))
            return "woff2";
        if (m.contains("woff"))
            return "woff";
        if (m.contains("truetype") || m.contains("ttf"))
            return "truetype";
        if (m.contains("opentype") || m.contains("otf"))
            return "opentype";
        return "woff2";
    }

    public String render(String htmlTemplate, Locale locale, Map<String, Object> model) {

        var spec = new TemplateSpec(htmlTemplate, TemplateMode.HTML);
        var ctx = new Context(locale);
        ctx.setVariables(model); // Map<String,Object>

        String out = engine.process(spec, ctx);

        return out; // blocking: run on boundedElastic if inside reactive chain
    }

    private String watermarkText(UserContext user) {
        return "DXC • " + (user == null ? "anonymous" : (user.fullName() + " (" + user.username() + ")"))
                 + " •  " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
                // + " • CONFIDENTIAL"
                ;
    }
}
