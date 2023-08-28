import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.swing.*;
import java.awt.*;

public class QR extends JPanel {
    public JFrame frame;
    public final int SIZE;
    private final BitMatrix bitMatrix;
    public QR(String content) throws WriterException {
        super();
        bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0);
        SIZE = bitMatrix.getWidth();
        frame = new JFrame("QRCode");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int i = 0; i < SIZE; i ++) {
            for (int j = 0; j < SIZE; j ++){
                if (bitMatrix.get(i, j)) {
                    g.fillRect(i*10, j*10, 10, 10);
                }
            }
        }
    }

    public void showQRCode() {
        frame.setSize((SIZE+10)*10, (SIZE+10)*10);
        frame.add(this);
        frame.setVisible(true);
    }

    public void close(){
        frame.dispose();
    }
}
