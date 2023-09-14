import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Arrays;

public class Outer {
    public static int port = 1000;
    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            // for your program, open a thread and run:
            // java -jar interface.jar "html folder path" "pot number"
            new Thread(() -> Main.main(new String[]{Integer.toString(Outer.port), Paths.get(".", "src", "html").toString()})).start(); // call inner

            Socket socket = serverSocket.accept();
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            while (true) {
                // transfer format:
                // int(4 bytes):len + bytes:info
                int len;
                byte[] b = new byte[1024];
                if (in.read(b, 0, 4) != 4) return; // do error copping here
                len = ByteBuffer.wrap(b).getInt();
                if (in.read(b, 4, len) != len) return;

                // do something for the received data

                // send data to device
                // you can jump this step if you have no data to send
                // for this demo, we echo back
                out.write(Arrays.copyOfRange(b, 0, len+4));
                out.flush();
            }
        }
    }
}
