// UDP 数据报编程学习示例（DatagramSocket / DatagramPacket）
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：数据报收发——每个包都是独立的、有边界的
//   第 2 段：UDP 的一问一答（无连接的请求-响应）
//
// 全部示例走 localhost 回环，端口用 0 由系统自动分配，离线可运行。

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

class UdpDemo {

    public static void main(String[] args) throws Exception {
        demoSendReceive();
        demoRequestReply();
    }

    // ------------------------------------------------------------------
    // 第 1 段：数据报收发——send 一个包，receive 一个包
    // ------------------------------------------------------------------
    static void demoSendReceive() throws IOException {
        System.out.println("== 第 1 段：数据报收发 ==");

        // 收件人：绑一个端口等包裹；寄件人：不绑也行（系统随机分配出口端口）
        try (DatagramSocket receiver = new DatagramSocket(0, InetAddress.getLoopbackAddress());
             DatagramSocket sender = new DatagramSocket()) {
            int port = receiver.getLocalPort();

            // UDP 每个数据报都是独立包裹：内容 + 收件地址全在包里，包与包互不搭理
            for (String msg : new String[]{"第一条", "第二条", "第三条"}) {
                byte[] data = msg.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(
                        data, data.length, InetAddress.getLoopbackAddress(), port);
                sender.send(packet);
            }

            // 不需要另开线程：receive 阻塞到包裹到达为止，发完了自己收正好
            byte[] buf = new byte[256];
            for (int i = 0; i < 3; i++) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                receiver.receive(packet);
                // 注意用 getLength() 而不是 buf.length：包里只装了这么些字节
                String msg = new String(buf, 0, packet.getLength(), StandardCharsets.UTF_8);
                System.out.println("  收到: " + msg + "（来自 "
                        + packet.getAddress().getHostAddress() + ":" + packet.getPort() + "）");
            }
        }
        System.out.println("对照 TCP：那边是无边界的字节流（要自己定界），这边一次 send 对应一次 receive");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：一问一答——UDP 也能做请求-响应，只是没有【连接】这回事
    // ------------------------------------------------------------------
    static void demoRequestReply() throws Exception {
        System.out.println("== 第 2 段：UDP 一问一答 ==");

        try (DatagramSocket server = new DatagramSocket(0, InetAddress.getLoopbackAddress());
             DatagramSocket client = new DatagramSocket()) {
            int port = server.getLocalPort();

            Thread serverThread = new Thread(() -> {
                try {
                    byte[] buf = new byte[256];
                    DatagramPacket req = new DatagramPacket(buf, buf.length);
                    server.receive(req);                    // 谁发的、从哪个端口发来的——全在包里
                    String msg = new String(buf, 0, req.getLength(), StandardCharsets.UTF_8);
                    byte[] resp = ("大写回信: " + msg.toUpperCase()).getBytes(StandardCharsets.UTF_8);
                    // 按包上写的【发件人地址】原路回信——没有连接，每次都要带地址
                    server.send(new DatagramPacket(resp, resp.length,
                            req.getAddress(), req.getPort()));
                    System.out.println("  [server] 已按发件人地址回信（无连接状态，回完就两清）");
                } catch (IOException e) {
                    System.out.println("  [server] 出错: " + e.getClass().getSimpleName());
                }
            }, "udp-server");
            serverThread.start();

            byte[] data = "ping".getBytes(StandardCharsets.UTF_8);
            client.send(new DatagramPacket(data, data.length,
                    InetAddress.getLoopbackAddress(), port));
            byte[] buf = new byte[256];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            client.receive(resp);                            // 等回信（生产代码要考虑丢包重试）
            System.out.println("  [client] 收到: "
                    + new String(buf, 0, resp.getLength(), StandardCharsets.UTF_8));
            serverThread.join();
        }
        System.out.println("UDP 交易模型：发包 → （可能）收包。丢了、乱了、重复了都 possible——");
        System.out.println("可靠性要自己在应用层补（重传/编号/校验），这正是 TCP 帮你做掉的事");
        System.out.println();
    }
}
