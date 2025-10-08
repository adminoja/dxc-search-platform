package th.go.dxc.platform.search.application.report.port.out.pdf;

public interface PdfRendererPort {
  byte[] fromHtml(byte[] htmlUtf8);
}
