import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Paths;

public class Outer {
    public static int port = 1000;
    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            // run java -jar interface.jar "your path" "pot number"
            new Thread(() -> Main.main(new String[]{Integer.toString(Outer.port), Paths.get(".", "src", "html").toString()})).start(); // call inner

            Socket socket = serverSocket.accept();
            InputStream in = socket.getInputStream();
            byte[] b = new byte[1024];
            while (true) {
                int len;
                if (in.read(b, 0, 2) == -1) return;
                len = ((b[0] & 255) << 8) | (b[1] & 255);
                if (in.read(b, 2, len) == -1) return;

                String value = Integer.toString(((b[3] & 255) << 8) | (b[4] & 255));
                ByteBuffer byteBuffer = ByteBuffer.allocate(4).putInt(value.length());
                socket.getOutputStream().write(GetResponse.joinByteArray(byteBuffer.array(), value.getBytes()));
                socket.getOutputStream().flush();
            }
        }
    }
}
