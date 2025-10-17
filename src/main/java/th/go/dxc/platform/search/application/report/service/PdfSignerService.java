package th.go.dxc.platform.search.application.report.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.Optional;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import th.go.dxc.platform.search.application.report.port.out.template.FileResourcePort;
import th.go.dxc.platform.search.config.ReportProperties;
@Slf4j
@Service
public class PdfSignerService {

  private final ReportProperties.Signing cfg;
  private final PrivateKey privateKey;     // null when disabled
  private final Certificate[] chain;       // null when disabled

  public PdfSignerService(ReportProperties reportProps, ResourceLoader resourceLoader,FileResourcePort io) {
    this.cfg = reportProps.sign();
    if (cfg == null || Boolean.FALSE.equals(cfg.enabled())) {
      this.privateKey = null;
      this.chain = null;
      return;
    }
    try {
      Security.addProvider(new BouncyCastleProvider());

      // Resource resource = resourceLoader.getResource(Objects.requireNonNull(cfg.keystorePath(),
      //     "platform.report.sign.keystorePath is required"));
      // if (!resource.exists()) {
      //   throw new IllegalStateException("Keystore not found: " + cfg.keystorePath());
      // }
      String location = Objects.requireNonNull(cfg.keystorePath(),
          "platform.report.sign.keystorePath is required");
      InputStream is = io.getInputStream(location).block(Duration.ofSeconds(5));
      if(is==null)
      {
        throw new IllegalStateException("Keystore not found or unreadable: " + location);
      }
      KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(is, toChars(cfg.storePassword()));
      
      // try (is = resource.getInputStream()) {
      //   ks.load(is, toChars(cfg.storePassword()));
      // }

      String alias = Optional.ofNullable(cfg.alias())
          .filter(a -> !a.isBlank())
          .orElseGet(() -> firstAlias(ks));

      this.privateKey = (PrivateKey) ks.getKey(alias, toChars(cfg.keyPassword()));
      this.chain = ks.getCertificateChain(alias);
      if (this.chain == null || this.chain.length == 0) {
        throw new IllegalStateException("No certificate chain for alias: " + alias);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize PdfSignerService", e);
    }
  }

  public boolean isEnabled() {
    return cfg != null && Boolean.TRUE.equals(cfg.enabled()) && privateKey != null && chain != null;
  }

  /** No-op if disabled; otherwise returns signed PDF bytes. */
  public Mono<byte[]> sign(byte[] unsignedPdf) {
    log.trace("sign: isEabled={}",isEnabled());
    if (!isEnabled()) return Mono.just(unsignedPdf);
    return Mono.fromCallable(() -> doSign(unsignedPdf));
  }

  private byte[] doSign(byte[] unsignedPdf) throws Exception {
    log.trace("doSiign: {}",unsignedPdf==null?null:unsignedPdf.length);
    try (PDDocument doc = Loader.loadPDF(unsignedPdf);
         ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      PDSignature sig = new PDSignature();
      sig.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
      sig.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
      sig.setName(defaultIfBlank(cfg.signerName(), "DXC Search Platform"));
      sig.setLocation(defaultIfBlank(cfg.signerLocation(), "Bangkok, Thailand"));
      sig.setReason(defaultIfBlank(cfg.signerReason(), "Official Report Verification"));
      sig.setSignDate(GregorianCalendar.from(
          java.time.ZonedDateTime.now(ZoneId.of("Asia/Bangkok"))));
      doc.addSignature(sig);

      try (SignatureOptions options = new SignatureOptions()) {
        // visible signature can be attached via options if needed later
        ExternalSigningSupport ext = doc.saveIncrementalForExternalSigning(out);
        byte[] cms = signCMS(ext.getContent().readAllBytes(), privateKey, chain);
        ext.setSignature(cms);
      }
      log.trace("out: {}", out);
      return out.toByteArray();
    }
  }

  private static byte[] signCMS(byte[] data, PrivateKey key, Certificate[] chain) throws Exception {
    CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
    gen.addCertificates(new JcaCertStore(Arrays.asList(chain)));

    X509Certificate signerCert = (X509Certificate) chain[0];
    ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(key);

    gen.addSignerInfoGenerator(
        new org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder(
            new JcaDigestCalculatorProviderBuilder().build())
            .build(signer, signerCert)
    );

    CMSSignedData sd = gen.generate(new CMSProcessableByteArray(data), false);
    return sd.getEncoded();
  }

  private static String defaultIfBlank(String s, String def) {
    return (s == null || s.isBlank()) ? def : s;
  }

  private static char[] toChars(String s) {
    return s == null ? new char[0] : s.toCharArray();
  }

  private static String firstAlias(KeyStore ks) {
    try {
      var e = ks.aliases();
      if (!e.hasMoreElements()) throw new IllegalStateException("No alias in keystore");
      return e.nextElement();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to enumerate keystore aliases", ex);
    }
  }
}
