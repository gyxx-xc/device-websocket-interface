import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Outer {
    public static int port = 1000;
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket;
        while (true) {
            try {
                serverSocket = new ServerSocket(++ port);
                break;
            } catch (IOException ignore) {
                continue; // port unavailable
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Port resource are used up");
            }
        }
        new Thread(() -> Main.main(new String[]{Integer.toString(Outer.port)})).start(); // call inner
        Socket socket = serverSocket.accept();
        InputStream in = socket.getInputStream();
        byte[] b = new byte[1024];
        while (true) {
            int len;
            for (len = 0; in.read(b, len, 1) != -1; len++) {
                if(len >= 3) break;
            }
            // System.out.println(new String(Arrays.copyOfRange(b, 0, len)));
//            System.out.println((b[1]));
            if (b[0] != -1) {
                System.out.println(((b[1] & 255)<<8) + (b[2] & 255));
            }
        }
    }
}
