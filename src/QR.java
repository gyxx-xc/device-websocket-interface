import javax.swing.*;
import java.awt.*;

public class QR extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.fillRect(150, 150, 100, 100);
    }
}
