import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetResponse extends Thread {
    Socket client;

    public static final List<String> TEXT_EXTENSION = Arrays.asList("html", "css", "js", "txt");

    public GetResponse(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            OutputStream clientOutStream = client.getOutputStream();
            InputStream in = client.getInputStream();
            Scanner s = new Scanner(in, StandardCharsets.UTF_8);
            String data = s.useDelimiter("\\r\\n\\r\\n").next();
            System.out.println(data);
            Matcher get = Pattern.compile("^GET (.*) HTTP/").matcher(data);
            if (get.find()) { // get command
                Path file = Paths.get(".", "src", "html", get.group(1));
                if (Files.isDirectory(file)) file = Paths.get(file.toString(), "index.html");
                String fileName = file.getFileName().toString();
                String extension = fileName.substring(fileName.lastIndexOf(".")+1);
                byte[] source;
                try {
                    if (TEXT_EXTENSION.contains(extension)) {
                        String content = Files.readString(file);
                        content = content.replaceAll("###HOST###",
                                Main.getLocalHostExactAddress().getHostAddress() + ":" + Main.port);
                        source = content.getBytes();
                    } else {
                        source = Files.readAllBytes(file);
                    }
                } catch (Exception e) {
                    clientOutStream.write(
                            ("""
                                    HTTP/1.1 404
                                    
                                    """).getBytes()
                    );
                    clientOutStream.flush();
                    clientOutStream.close();
                    return;
                }

                Matcher upgrade = Pattern.compile("Upgrade: (.*)").matcher(data);
                if (upgrade.find()) {
                    Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
                    if (match.find()) {
                        byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                                + "Connection: Upgrade\r\n"
                                + "Upgrade: websocket\r\n"
                                + "Sec-WebSocket-Accept: "
                                + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(
                                (match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)))
                                + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
                        clientOutStream.write(response);
                        clientOutStream.flush();
                        response = new byte[1024];
                        byte[] d = new byte[1024];
                        while (in.read(response, 0, 6) != -1) {
                            int len = response[1] & 127;
                            byte[] mask = Arrays.copyOfRange(response, 2, 6);
                            for (int i = 0; i < len; i++) {
                                if (in.read(response, 6 + i, 1) != -1)
                                    d[i] = (byte) (response[i + 6] ^ mask[i & 3] & 255);
                                else
                                    throw new RuntimeException("the data is not receive comprehensively");
                            }
                            System.out.println(new String(Arrays.copyOfRange(d, 0, len)));
                        }
                    }
                } else {
                    clientOutStream.write(
                            joinByteArray(("""
                                    HTTP/1.1 200
                                    
                                    """).getBytes(), source)
                    );
                    clientOutStream.flush();
                }
                clientOutStream.close();
            }
        } catch (IOException | NoSuchAlgorithmException ignore) {
        }
    }

    // copy https://mkyong.com/java/java-how-to-join-and-split-byte-arrays-byte/
    public static byte[] joinByteArray(byte[] byte1, byte[] byte2) {

        byte[] result = new byte[byte1.length + byte2.length];

        System.arraycopy(byte1, 0, result, 0, byte1.length);
        System.arraycopy(byte2, 0, result, byte1.length, byte2.length);

        return result;

    }
}
