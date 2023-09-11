import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetResponse extends Thread {
    private final Socket client;
    private final String path;
    private static volatile boolean threadLock;

    public GetResponse(Socket client, String path) {
        this.client = client;
        this.path = path;
    }

    public static final List<String> TEXT_EXTENSION = Arrays.asList("html", "css", "js", "txt", "php");

    @Override
    public void run() {
        try {
            OutputStream clientOutStream = client.getOutputStream();
            InputStream clientInStream = client.getInputStream();
            OutputStream outerWrite = Main.socket2C.getOutputStream();
            InputStream outerIn = Main.socket2C.getInputStream();

            Scanner s = new Scanner(clientInStream, StandardCharsets.UTF_8);
            String request = s.useDelimiter("\\r\\n\\r\\n").next();
            Matcher get = Pattern.compile("^GET (.*) HTTP").matcher(request);
            if (get.find()) { // get command
                Matcher upgrade = Pattern.compile("Upgrade: (.*)").matcher(request);
                if (upgrade.find()) {
                    connectWebSocket(request, clientOutStream, clientInStream, outerWrite, outerIn);
                } else {
                    responseHttp(get.group(1), clientOutStream);
                }
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // copy https://mkyong.com/java/java-how-to-join-and-split-byte-arrays-byte/
    public static byte[] joinByteArray(byte[] byte1, byte[] byte2) {

        byte[] result = new byte[byte1.length + byte2.length];

        System.arraycopy(byte1, 0, result, 0, byte1.length);
        System.arraycopy(byte2, 0, result, byte1.length, byte2.length);

        return result;

    }

    protected void responseHttp(String filePath, OutputStream clientOutStream) throws IOException {
        Path file = Paths.get(path, filePath);
        if (!Files.exists(file)) {
            clientOutStream.write(
                    ("""
                                    HTTP/1.1 404
                                    
                                    """).getBytes()
            );
            clientOutStream.flush();
            clientOutStream.close();
            client.close();
            return;
        }
        if (Files.isDirectory(file)) file = Paths.get(file.toString(), "index.html");
        String fileName = file.getFileName().toString();
        String extension = fileName.substring(fileName.lastIndexOf(".")+1);
        byte[] source;
        if (TEXT_EXTENSION.contains(extension)) {
            String content = Files.readString(file);
            content = content.replaceAll("###HOST###",
                    Main.getLocalHostExactAddress().getHostAddress() + ":" + Main.port);
            source = content.getBytes();
        } else {
            source = Files.readAllBytes(file);
        }
        clientOutStream.write(
                joinByteArray(("""
                                    HTTP/1.1 200
                                    
                                    """).getBytes(), source)
        );
        clientOutStream.flush();
        clientOutStream.close();
        client.close();
    }

    protected void connectWebSocket(
            String request,
            OutputStream clientOutStream,
            InputStream clientInStream,
            OutputStream outerWrite,
            InputStream outerIn)
            throws IOException, NoSuchAlgorithmException {
        Matcher match = Pattern.compile("Host: (.*)").matcher(request);
        if (!match.find()) {
            badRequest(clientOutStream, "{\"error\": {\"message\": \"missing host\"}}");
            return;
        }
        match = Pattern.compile("Sec-WebSocket-Version: 13").matcher(request);
        if (!match.find()) {
            badRequest(clientOutStream, "{\"error\": {\"message\": \"WebSocket version not acceptable\"}}");
            return;
        }
        match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(request);
        if (!match.find()) {
            badRequest(clientOutStream, "{\"error\": {\"message\": \"Missing WebSocket Key\"}}");
            return;
        }
        Matcher get = Pattern.compile("^GET (.*) HTTP").matcher(request);
        int selector; // determinate which socket it will use
        if (!get.find()) return;
        // TODO: modularize the different socket
        if (get.group(1).equals("/in")){
            selector = 1;
        } else if (get.group(1).equals("/out")){
            selector = 2;
        } else {
            badRequest(clientOutStream, "{\"error\": {\"message\": \"request socket not found\"}}");
            return;
        }
        byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                + "Connection: Upgrade\r\n"
                + "Upgrade: websocket\r\n"
                + "Sec-WebSocket-Accept: "
                + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(
                (match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)))
                + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        clientOutStream.write(response);
        clientOutStream.flush();
        if (selector == 1) { // read
            byte[] frame = new byte[10];
            byte[] d = new byte[1024];
            while (clientInStream.read(frame, 0, 2) != -1) {
                if ((frame[1] & 128) == 0) { // client not masked
                    endSocket(clientOutStream, (short) 1002);
                    return;
                }
                int len = frame[1] & 127;
                if (len == 126) {
                    if (clientInStream.read(frame, 2, 2) != -1) {
                        len = (frame[2] & 255) << 8 | (frame[3] & 255);
                    } else {
                        endSocket(clientOutStream, (short) 1002);
                        return;
                    }
                } else if (len == 127) {
                    // only the last two bits will be used for len
                    // if the len is longer than 16 bits...(2^16 = 2 TiB)
                    // just end the socket, since it is too big
                    if (clientInStream.read(frame, 2, 4) != -1) {
                        len = (frame[8] & 255) << 8 | (frame[9] & 255);
                    } else {
                        endSocket(clientOutStream, (short) 1002);
                        return;
                    }
                    if (frame[2] != 0 &&
                            frame[3] != 0 &&
                            frame[4] != 0 &&
                            frame[5] != 0 &&
                            frame[6] != 0 &&
                            frame[7] != 0) {
                        endSocket(clientOutStream, (short) 1002);
                        return;
                    }
                }
                byte[] mask = new byte[4];
                if (clientInStream.read(mask, 0, 4) == -1) {
                    endSocket(clientOutStream, (short) 1002);
                    return;
                }
                for (int i = 0; i < len; i++) {
                    // It's not too slow, so I just leave it like this
                    if (clientInStream.read(d, i, 1) != -1)
                        d[i] = (byte) (d[i] ^ mask[i & 3] & 255);
                    else {
                        endSocket(clientOutStream, (short) 1002);
                        return;
                    }
                }
                if ((frame[0] & 15) == 8) {
                    if (d[0] == 3 && d[1] == (byte) 0xE8) { // 1000 ( statue code = (d[0] << 8 | d[1]) )
                        endSocket(clientOutStream, (short) 1000); // quited from client, so this program closed
                        System.exit(0);
                    } else if (d[0] == 3 && d[1] == (byte) 0xE9) { // 1001
                        endSocket(clientOutStream, (short) 1001);
                        return;
                    } else {
                        endSocket(clientOutStream, (short) (d[0] << 8 | d[1]));
                        return;
                    }
                }
                if ((frame[0] & 15) == 9) { // replay ping
                    ByteBuffer byteBuffer = ByteBuffer
                            .allocate(len < 125 ? 2 : 4 + len)
                            .put((byte) 0x8A);
                    if (len < 125)
                        byteBuffer.put((byte) len);
                    else
                        byteBuffer.put((byte) 126).putShort((short) len);
                    clientOutStream.write(joinByteArray(byteBuffer.array(), Arrays.copyOfRange(d, 0, len)));
                    clientOutStream.flush();
                }
                if ((frame[0] & 15) == 1) {
                    outerWrite.write(joinByteArray(new byte[]{(byte) (len >> 8), (byte) len}, Arrays.copyOfRange(d, 0, len)));
                    outerWrite.flush();
                }
            }
        }
        else {
            byte[] d = new byte[1024];
            while (outerIn.read(d, 0, 4) != -1) {
                int len = ByteBuffer.wrap(d).getInt(0);
                if (len > 1024) return;
                if (outerIn.read(d, 0, len) == -1) return;
                ByteBuffer byteBuffer;
                if (len > 125) {
                    byteBuffer = ByteBuffer
                            .allocate(4)
                            .putShort((short) 0x817E)
                            .putShort((short) len);
                } else {
                    byteBuffer = ByteBuffer
                            .allocate(2)
                            .put((byte) 0x81)
                            .put((byte) len);
                }
                clientOutStream.write(joinByteArray(byteBuffer.array(), Arrays.copyOfRange(d, 0, len)));
                clientOutStream.flush();
            }
        }
    }

    private void badRequest(OutputStream clientOutStream, String message) throws IOException {
        clientOutStream.write(
                ("""
                                 HTTP/1.1 400

                                 """ + message).getBytes()
        );
        clientOutStream.flush();
        clientOutStream.close();
        client.close();
    }

    private void endSocket(OutputStream out, short statueCode) throws IOException {
        System.out.println(statueCode);
        ByteBuffer byteBuffer = ByteBuffer
                .allocate(4)
                .putShort((short) 0x8802)
                .putShort(statueCode);
        out.write(byteBuffer.array());
        out.close();
        client.close();
    }

}
