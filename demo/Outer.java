import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.security.spec.ECField;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Outer {
    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {

            // for your program, open a thread and run:
            // java -jar interface.jar "pot number" "html folder path"
            new Thread(() -> {
                try {
                    Process cmdProc = Runtime.getRuntime().exec("java -jar ./interface.jar " + serverSocket.getLocalPort() + " html");
                    BufferedReader stdoutReader = new BufferedReader(
                            new InputStreamReader(cmdProc.getInputStream()));
                    String line;
                    while ((line = stdoutReader.readLine()) != null) {
                        System.out.println(line);
                    }

                    BufferedReader stderrReader = new BufferedReader(
                            new InputStreamReader(cmdProc.getErrorStream()));
                    while ((line = stderrReader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (Exception e) {e.printStackTrace();}
            }).start();

            Socket socket = serverSocket.accept();
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            while (true) {
                // transfer format:
                // int:len + bytes:info (int is 32 bits)
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
