import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
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
import java.util.Enumeration;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        QR localIPqr;
        try {
            localIPqr = new QR("http://" + getLocalHostExactAddress().getHostAddress()+"/");
        } catch (Exception e) {
            return;
        }
        localIPqr.showQRCode();
        try {
            ServerSocket serverSocket = new ServerSocket(80);
            Path file = Paths.get(".", "src", "html/main.html");
            String html;
            html = String.join("\n", Files.readAllLines(file));
            html = html.replaceAll("localhost", getLocalHostExactAddress().getHostAddress());
            while (true) {
                Socket client = serverSocket.accept();
                OutputStream clientOutStream = client.getOutputStream();
                InputStream in = client.getInputStream();
                Scanner s = new Scanner(in, StandardCharsets.UTF_8);
                String data = s.useDelimiter("\\r\\n\\r\\n").next();
                Matcher get = Pattern.compile("^GET (.*) HTTP/").matcher(data);
                if (get.find()) { // get command
                    Matcher connectionM = Pattern.compile("Connection: (.*)").matcher(data);
                    String[] connection = connectionM.group(1).split(", ");
                    if (connection.length == 1 && connection[0].equals("keep-alive"));
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
                                if (in.read(response, 6 + i, 1) != -1)
                                    d[i] = (byte) (response[i + 6] ^ mask[i & 3] & 255);
                                else
                                    throw new RuntimeException("the data is not receive comprehensively");
                            }
                            System.out.println(new String(d));
                        }
                    } else {
                        localIPqr.close();
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

    public static InetAddress getLocalHostExactAddress() {
        try {
            InetAddress candidateAddress = null;

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                // 该网卡接口下的ip会有多个，也需要一个个的遍历，找到自己所需要的
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    // 排除loopback回环类型地址（不管是IPv4还是IPv6 只要是回环地址都会返回true）
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了 就是我们要找的
                            // ~~~~~~~~~~~~~绝大部分情况下都会在此处返回你的ip地址值~~~~~~~~~~~~~
                            return inetAddr;
                        }

                        // 若不是site-local地址 那就记录下该地址当作候选
                        if (candidateAddress == null) {
                            candidateAddress = inetAddr;
                        }

                    }
                }
            }

            // 如果出去loopback回环地之外无其它地址了，那就回退到原始方案吧
            return candidateAddress == null ? InetAddress.getLocalHost() : candidateAddress;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}