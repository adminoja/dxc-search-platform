package th.go.dxc.platform.search.application.report.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.report.port.in.RenderHtmlReportUseCase;
import th.go.dxc.platform.search.application.report.port.in.RenderPdfUseCase;
import th.go.dxc.platform.search.config.ReportProperties;
import th.go.dxc.platform.search.domain.common.value.UserContext;

@Slf4j
@Service
public class RenderPdfUseCaseImpl implements RenderPdfUseCase {

    private final RenderHtmlReportUseCase htmlUseCase;
    private final WebClient webClient;
    private final ReportProperties props;
    // 👇 ADD field
    private final PdfSignerService pdfSignerService;
    private final TemplateIO io;

    public RenderPdfUseCaseImpl(
            RenderHtmlReportUseCase htmlUseCase,
            ReportProperties props, PdfSignerService pdfSignerService, TemplateIO io) {
        this.htmlUseCase = Objects.requireNonNull(htmlUseCase, "htmlUseCase");
        this.props = props;
        var strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024)) // 32MB
                .build();

        // HttpClient http = HttpClient.create()
        //         .wiretap( // <— full request/response line+headers+body (human readable)
        //                 "reactor.netty.http.client",
        //                 LogLevel.trace,
        //                 AdvancedByteBufFormat.SIMPLE);

        this.webClient = WebClient.builder()
                .baseUrl(props.gotenberg().baseUrl())
                // .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_PDF_VALUE)
                .filter(logRequest()) // <— custom request logger
                .filter(logResponse()) // <— custom response logger
                .build();

        this.pdfSignerService = Objects.requireNonNull(pdfSignerService); // <— added
        this.io = io;
    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            log.trace("➡️  {} {}", req.method(), req.url());
            req.headers().forEach((n, v) -> log.trace("➡️  {}: {}", n, String.join(",", v)));
            return Mono.just(req);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(res -> {
            log.trace("⬅️  HTTP {}", res.statusCode());
            res.headers().asHttpHeaders().forEach((n, v) -> log.trace("⬅️  {}: {}", n, String.join(",", v)));
            return Mono.just(res);
        });
    }

    @Override
    public Mono<Output> execute(Input in) {
        log.trace("execute: Input={}", in);
        Objects.requireNonNull(in, "input");
        Objects.requireNonNull(in.scope(), "scope");
        Objects.requireNonNull(in.token(), "token");

        // 1) First get the rendered HTML from your existing use case
        log.trace("1) First get the rendered HTML from your existing use case");
        return htmlUseCase.execute(new RenderHtmlReportUseCase.Input(in.scope(), in.token(), in.user()))
                .flatMap(htmlOut -> {
                    log.trace("htmlOut = {}", htmlOut);
                    String html = htmlOut.html();
                    if (html == null || html.isBlank()) {
                        log.trace("null HTML");
                        return Mono.error(new IllegalStateException("Rendered HTML is empty"));
                    }
                    log.trace("html length = {}", html.length());
                    // 2) Prepare Gotenberg multipart form
                    log.trace("2) Prepare Gotenberg multipart form");
                    MultipartBodyBuilder mb = new MultipartBodyBuilder();

                    // "files" must contain an entry named index.html
                    byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
                    mb.part("files", new NamedByteArrayResource("index.html", htmlBytes))
                            .filename("index.html")
                            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8));
                    // ---- header.html (from classpath) ----
                    // Resource header = headerWithLogo(in.user());
                    String header = headerWithLogoHtml(in.user());
                    
                    if (
                        // header.exists()
                        header !=null && !header.isBlank()
                        ) {
                        mb.part("files", header).filename("header.html")
                                .contentType(MediaType.TEXT_HTML);
                    } else {
                        log.warn("No Header File");
                    }

                    // ---- footer.html (from classpath) ----
                    // Resource footer = footerBytes();
                    String footer = footerHtml();
                    if (
                        // footer.exists()
                        footer!=null && !footer.isBlank()
                        ) {
                        mb.part("files", footer).filename("footer.html")
                                .contentType(MediaType.TEXT_HTML);
                    } else {
                        log.warn("No footer file");
                    }
                    // Chromium options (defaults)
                    RenderPdfUseCase.Options o = in.options();
                    boolean printBackground = o == null || o.printBackground() == null ? true : o.printBackground();
                    boolean preferCssPageSize = o == null || o.preferCssPageSize() == null ? true
                            : o.preferCssPageSize();
                    mb.part("printBackground", printBackground ? "true" : "false");
                    mb.part("preferCssPageSize", preferCssPageSize ? "true" : "false");

                    if (o != null) {
                        if (o.landscape() != null)
                            mb.part("landscape", o.landscape() ? "true" : "false");
                        if (o.scale() != null)
                            mb.part("scale", o.scale().toString());
                        if (o.marginTop() != null)
                            mb.part("marginTop", o.marginTop());
                        if (o.marginBottom() != null)
                            mb.part("marginBottom", o.marginBottom());
                        if (o.marginLeft() != null)
                            mb.part("marginLeft", o.marginLeft());
                        if (o.marginRight() != null)
                            mb.part("marginRight", o.marginRight());
                        if (o.waitDelay() != null)
                            mb.part("waitDelay", o.waitDelay());
                        else if (o.waitForExpression() != null)
                            mb.part("waitForExpression", o.waitForExpression());
                    }

                    final String filename = ensurePdfSuffix(
                            in.filename() == null || in.filename().isBlank() ? "report.pdf" : in.filename());
                    log.trace("Multiplart = {}", mb);
                    // 3) Call Gotenberg
                    log.trace("3) Call Gotenberg");
                    return webClient.post()
                            .uri("/forms/chromium/convert/html")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(BodyInserters.fromMultipartData(mb.build()))
                            // If Gotenberg returns non-2xx, read the error payload as text so you can see
                            // it
                            .exchangeToMono(res -> {
                                if (res.statusCode().is2xxSuccessful()) {
                                    // return res.bodyToMono(byte[].class)
                                    // .switchIfEmpty(Mono.fromRunnable(
                                    // () -> log.warn("⚠️ 200 OK with EMPTY BODY from Gotenberg")))
                                    // .doOnNext(b -> log.trace("📄 PDF bytes received: {}", b.length))
                                    // .map(bytes -> new Output(filename,
                                    // res.headers().contentType().map(MediaType::toString)
                                    // .orElse(MediaType.APPLICATION_PDF_VALUE),
                                    // bytes));
                                    return res.bodyToMono(byte[].class)
                                            .switchIfEmpty(Mono.fromRunnable(
                                                    () -> log.warn("⚠️  200 OK with EMPTY BODY from Gotenberg")))
                                            .doOnNext(b -> log.trace("📄 PDF bytes received: {}", b.length))
                                            // 🔐 sign here (no-op if disabled)
                                            .flatMap(pdfSignerService::sign)
                                            .map(signed -> new Output(
                                                    filename,
                                                    res.headers().contentType().map(MediaType::toString)
                                                            .orElse(MediaType.APPLICATION_PDF_VALUE),
                                                    signed));
                                } else {
                                    return res.bodyToMono(String.class)
                                            .defaultIfEmpty("<no error body>")
                                            .flatMap(err -> {
                                                int code = res.statusCode().value();
                                                String reason = java.util.Optional
                                                        .ofNullable(org.springframework.http.HttpStatus.resolve(code))
                                                        .map(org.springframework.http.HttpStatus::getReasonPhrase)
                                                        .orElse("");
                                                log.error("❌ Gotenberg {} {} error:\n{}", code, reason, err);
                                                return reactor.core.publisher.Mono.error(
                                                        new IllegalStateException("Gotenberg error " + code
                                                                + (reason.isBlank() ? "" : " " + reason)));
                                            });
                                }
                            })
                            .timeout(props.gotenberg().timeout());
                });
    }

    private static String ensurePdfSuffix(String name) {
        return name.toLowerCase().endsWith(".pdf") ? name : name + ".pdf";
    }

    /** ByteArrayResource with fixed filename for multipart "files". */
    static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(String filename, byte[] bytes) {
            super(bytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private String headerWithLogoHtml(UserContext user) {
        String html = io.readTextSync(props.template().resolve(props.template().header()));
        log.trace("logo from {} , mime={}",props.template().resolve(props.template().logo().path()),props.template().logo().mime());
        String logoB64 = io.dataUriOrEmpty(props.template().resolve(props.template().logo().path()),
                props.template().logo().mime());
        // replace marker
        log.trace("logoB64 = {}",left(logoB64,150));
        html = html.replace("{{LOGO_BASE64}}", logoB64);
        log.trace("html = {}", left(html,800));
        // add printed_by, printed_date_time
        html = html.replace("{{PRINTED_BY}}", user == null ? "Anonymous" : user.username());
        html = html.replace("{{PRINTED_DATE_TIME}}", prettyDateTimeBe(Instant.now()));
        return html;
    }

    private String left(String str,Integer count)
    {
        String left = "";
        if(str!=null && !str.isBlank())left = str.substring(0,(count>=str.length()?str.length():count));
        return left;
    }

    private String footerHtml(){
        String html = io.readTextSync(props.template().resolve(props.template().footer()));
        return html;
    }

    private String prettyDateTimeBe(Instant instant) {
        ZonedDateTime zdt = instant.atZone(ZoneId.of("Asia/Bangkok"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEEที่ d MMMM พ.ศ. yyyy เวลา HH:mm น.",
                Locale.of("th", "TH")).withChronology(ThaiBuddhistChronology.INSTANCE);
        return formatter.format(zdt);
    }
}
