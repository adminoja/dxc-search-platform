package th.go.dxc.platform.search.application.report.service;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

public final class QrSvgUtil {

  public static String qrSvg(String text, int size, int margin) {
    try {
      Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
      hints.put(EncodeHintType.MARGIN, margin);

      BitMatrix m = new MultiFormatWriter()
          .encode(text, BarcodeFormat.QR_CODE, size, size, hints);

      int w = m.getWidth();
      int h = m.getHeight();

      StringBuilder path = new StringBuilder();
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          if (m.get(x, y)) {
            // draw a 1x1 square at (x,y)
            path.append("M").append(x).append(' ').append(y).append("h1v1h-1z");
          }
        }
      }

      // crispEdges and viewBox make it scale perfectly in PDF/Chromium
      return """
          <svg xmlns="http://www.w3.org/2000/svg"
               shape-rendering="crispEdges"
               viewBox="0 0 %d %d"
               width="%d" height="%d">
            <rect width="100%%" height="100%%" fill="white"/>
            <path d="%s" fill="black"/>
          </svg>
          """.formatted(w, h, w, h, path.toString());
    } catch (Exception e) {
      throw new RuntimeException("QR SVG encode failed", e);
    }
  }
}
