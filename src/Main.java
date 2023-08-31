import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

public class Main {
    public static int port = 8887; // start from 8888, since is (++ port)
    public static Socket socket2C;
    public static void main(String[] args) {
        try {
            socket2C = new Socket("localhost", Integer.parseInt(args[0]));
        } catch (Exception e){throw new RuntimeException("caused by calling: " + e.getMessage());}
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

        QR localIPqr;
        try {
            localIPqr = new QR("http://" + getLocalHostExactAddress().getHostAddress() + ":" + port + "/index.html");
        } catch (Exception e) {
            return;
        }
        localIPqr.showQRCode();

        try {
            while (true) {
                Socket client = serverSocket.accept();
                localIPqr.close();
                (new GetResponse(client, args[1])).start();
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