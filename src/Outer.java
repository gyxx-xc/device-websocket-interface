import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

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
                if(b[len] == 0) break;
            }
            System.out.println(new String(Arrays.copyOfRange(b, 0, len)));
        }
    }
}
