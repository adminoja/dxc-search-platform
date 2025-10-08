package th.go.dxc.platform.search.application.report.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

public final class QrUtil {

  public static String qrPngBase64(String text, int size, int margin) {
    try {
      Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // L|M|Q|H
      hints.put(EncodeHintType.MARGIN, margin); // quiet zone (modules)

      BitMatrix matrix = new MultiFormatWriter()
          .encode(text, BarcodeFormat.QR_CODE, size, size, hints);

      BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix, new MatrixToImageConfig());
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(img, "png", baos);
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (Exception e) {
      throw new RuntimeException("QR encode failed", e);
    }
  }
}
