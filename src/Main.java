import javax.swing.*;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        JFrame frame = new JFrame("test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 1000);
        frame.add(new QR());
        frame.setVisible(true);
        boolean wb = false;
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
                        clientOutStream.flush();
                        response = new byte[1024];
                        byte[] d = new byte[1024];
                        while (in.read(response, 0, 6) != -1) {
                            int len = response[1] & 127;
                            byte[] mask = Arrays.copyOfRange(response, 2, 6);
                            for (int i = 0; i < len; i++) {
                                in.read(response, 6 + i, 1);
                                d[i] = (byte) (response[i + 6] ^ mask[i & 3] & 255);
                            }
                            System.out.println(Arrays.toString(d));
                            clientOutStream.write(response, 0, len+6);
                            clientOutStream.flush();
                        }
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