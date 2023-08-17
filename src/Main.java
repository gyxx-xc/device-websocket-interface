import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        try {
            ServerSocket serverSocket = new ServerSocket(80);
            Path file = Paths.get(".", "src", "main.html");
            String html;
            html = String.join("\n", Files.readAllLines(file));
            while (true) {
                Socket client = serverSocket.accept();
                OutputStream clientOutStream = client.getOutputStream();
                InputStream in = client.getInputStream();
                Scanner s = new Scanner(in, StandardCharsets.UTF_8);
                String data = s.useDelimiter("\\r\\n\\r\\n").next();
                System.out.println(data);
                System.out.println();
                Matcher get = Pattern.compile("^GET").matcher(data);
                if (get.find()) {
                    Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
                    if (match.find()){
                        byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                                + "Connection: Upgrade\r\n"
                                + "Upgrade: websocket\r\n"
                                + "Sec-WebSocket-Accept: "
                                + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(
                                        (match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)))
                                + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
                        clientOutStream.write(response);
                    } else {
                        clientOutStream.write(
                                ("""
                                        HTTP/1.1 200
                                        Content-Type: text/html
                                                                        
                                        """ + html).getBytes()
                        );
                    }
                    clientOutStream.flush();
                    clientOutStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}