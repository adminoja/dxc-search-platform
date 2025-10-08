package th.go.dxc.platform.search.application.report.service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import th.go.dxc.platform.search.application.common.cache.port.out.CachePort;
import th.go.dxc.platform.search.application.report.port.in.GeneratePdfFromReportIdUseCase;
import th.go.dxc.platform.search.application.report.port.out.ObjectStoragePort;
import th.go.dxc.platform.search.application.report.port.out.PdfRendererPort;
import th.go.dxc.platform.search.application.report.port.out.TemplateRepository;
import th.go.dxc.platform.search.config.ReportProperties;
import th.go.dxc.platform.search.domain.common.value.UserContext;
import th.go.dxc.platform.search.domain.report.model.ReportPointer;

@Service
@RequiredArgsConstructor
public class ReportPdfService implements GeneratePdfFromReportIdUseCase {

  private final CachePort cache;
  private final TemplateRepository templates;
  private final PdfRendererPort renderer;
  private final ObjectStoragePort storage;
  private final ReportProperties props;

  @Override
  public Mono<Result> generate(Query q) {
    Objects.requireNonNull(q.reportId(), "reportId");
    return Mono.fromCallable(() -> {
      // 1) Resolve reportId -> pointer
      String ptrKey = reportPointerKey(q.reportId());
      ReportPointer ptr = cache.get(ptrKey, ReportPointer.class);
      if (ptr == null) throw new IllegalArgumentException("Report expired or not found: " + q.reportId());

      // 2) Enforce user scoping
      String scopeHash = scopeHash(q.userContext());
      if (!scopeHash.equals(ptr.scopeHash()))
        throw new SecurityException("Report belongs to a different user scope");

      // 3) Load cached row data
      String detailKey = detailKey(ptr.datasetId(), ptr.dataId(), ptr.scopeHash(), ptr.runId());
      @SuppressWarnings("unchecked")
      Map<String,Object> row = cache.get(detailKey, Map.class);
      if (row == null) throw new IllegalArgumentException("Row snapshot expired for reportId=" + q.reportId());

      // 4) Load template bundle (<dataset-id>.html, optional header/footer)
      var tb = templates.load(ptr.datasetId());

      // 5) Render: apply variables (very small set to keep stable)
      String index = new String(tb.indexHtml(), StandardCharsets.UTF_8);
      String header = tb.headerHtml() == null ? null : new String(tb.headerHtml(), StandardCharsets.UTF_8);
      String footer = tb.footerHtml() == null ? null : new String(tb.footerHtml(), StandardCharsets.UTF_8);

      Map<String,Object> model = new HashMap<>();
      model.putAll(row);                       // fields from snapshot (e.g., case_no, person_name, etc.)
      model.put("generatedAt", OffsetDateTime.now().toString());
      model.put("datasetId", ptr.datasetId());
      model.put("reportId", q.reportId());

      // naive ${var} replacement to avoid coupling the domain to Thymeleaf tags.
      // if you prefer Thymeleaf proper, inject SpringTemplateEngine here.
      index  = simpleReplace(index,  model);
      header = header == null ? null : simpleReplace(header, model);
      footer = footer == null ? null : simpleReplace(footer, model);

      var pdfMono = renderer.render(new PdfRendererPort.Template(index, header, footer), model);
      byte[] pdf = pdfMono.block(); // safe: we're already on boundedElastic

      // 6) Store → presign
      String fileBase = (q.suggestedFileName() == null || q.suggestedFileName().isBlank())
          ? (ptr.datasetId() + "-" + q.reportId())
          : q.suggestedFileName().replaceAll("[^a-zA-Z0-9._-]", "_");
      String objectKey = "reports/" + fileBase + ".pdf";
      var put = storage.putAndPresign(objectKey, pdf, "application/pdf", props.getMinio().getPresignSeconds());

      return new Result(put.objectKey(), put.presignedUrl(), put.sizeBytes(), row);
    }).subscribeOn(Schedulers.boundedElastic());
  }

  private static String reportPointerKey(String reportId) {
    return "report:" + reportId;
  }
  private static String detailKey(String datasetId, String dataId, String scopeHash, String runId) {
    return String.join(":", "detail", datasetId, dataId, scopeHash, runId);
  }

  /** Reuse the same scoping logic as LocalSearchService (must match!) */
  private static String scopeHash(UserContext u) {
    var sb = new StringBuilder();
    sb.append(u.userId() == null ? "" : u.userId());
    sb.append("|").append(u.clientId() == null ? "" : u.clientId());
    sb.append("|").append(u.authorities() == null ? "" : String.join(",", u.authorities()));
    sb.append("|").append(u.tenantId() == null ? "" : u.tenantId());
    return sha256(sb.toString());
  }

  private static String sha256(String s) {
    try {
      var md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      var sb = new StringBuilder(dig.length * 2);
      for (byte b : dig) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String simpleReplace(String html, Map<String,Object> model) {
    String out = html;
    for (var e : model.entrySet()) {
      String key = e.getKey();
      String val = e.getValue() == null ? "" : String.valueOf(e.getValue());
      out = out.replace("${" + key + "}", escapeHtml(val));
    }
    return out;
  }
  private static String escapeHtml(String s) {
    return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
            .replace("\"","&quot;").replace("'","&#39;");
  }
}
