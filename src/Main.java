import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

public class Main {
    public static int port; // start from 8888, since is (++ port)
    public static Socket socket2C;
    public static void main(String[] args) {
        try {
            socket2C = new Socket("localhost", Integer.parseInt(args[0]));
        } catch (Exception e){throw new RuntimeException("caused by calling: " + e.getMessage());}
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException ignore) {
            // port unavailable
            throw new RuntimeException("socket are unavailable");
        }
        port = serverSocket.getLocalPort();
        System.out.println("server start on "+getLocalHostExactAddress().getHostAddress()+":"+port);

        QR localIPqr;
        try {
            localIPqr = new QR("http://" + getLocalHostExactAddress().getHostAddress() + ":" + port + "/index.html");
        } catch (Exception e) {
            return;
        }
        localIPqr.showQRCode();

        while (true) {
            try {
                Socket client = serverSocket.accept();
                localIPqr.close();
                (new GetResponse(client, args[1])).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static InetAddress getLocalHostExactAddress() {
        try {
            InetAddress candidateAddress = null;

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                // 该网卡接口下的ip会有多个，也需要一个个的遍历，找到自己所需要的
                for (Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses(); inetAddresses.hasMoreElements(); ) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    // 排除loopback回环类型地址（不管是IPv4还是IPv6 只要是回环地址都会返回true）
                    if (!inetAddress.isLoopbackAddress()) {
                        if (inetAddress.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了 就是我们要找的
                            // ~~~~~~~~~~~~~绝大部分情况下都会在此处返回你的ip地址值~~~~~~~~~~~~~
                            return inetAddress;
                        }

                        // 若不是site-local地址 那就记录下该地址当作候选
                        if (candidateAddress == null) {
                            candidateAddress = inetAddress;
                        }

                    }
                }
            }

            // 如果出去loopback回环地之外无其它地址了，那就回退到原始方案吧
            return candidateAddress == null ? InetAddress.getLocalHost() : candidateAddress;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("acquire address failed");
    }
}