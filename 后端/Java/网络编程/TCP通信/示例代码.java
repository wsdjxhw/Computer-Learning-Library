// TCP Socket 编程学习示例（BIO 模型：ServerSocket / Socket）
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：InetAddress——IP 与主机名
//   第 2 段：最简 TCP 回显服务（单连接，看懂 accept/read/write 生命周期）
//   第 3 段：多客户端——accept 循环 + 线程池（一连接一任务）
//   第 4 段：SO_TIMEOUT——给阻塞读上个闹钟
//
// 全部示例只连 localhost（回环地址），端口用 0 由系统自动分配，离线可运行、绝不冲突。

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class TcpDemo {

    public static void main(String[] args) throws Exception {
        demoInetAddress();
        demoEchoServer();
        demoMultiClient();
        demoTimeout();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ------------------------------------------------------------------
    // 第 1 段：InetAddress——网络上的一台机器
    // ------------------------------------------------------------------
    static void demoInetAddress() throws IOException {
        System.out.println("== 第 1 段：InetAddress ==");

        InetAddress loopback = InetAddress.getLoopbackAddress();
        System.out.println("  回环地址: " + loopback.getHostAddress() + "（localhost，本机自连不走网卡）");

        InetAddress byName = InetAddress.getByName("localhost");   // 域名/IP → InetAddress
        System.out.println("  localhost 解析为: " + byName.getHostAddress());

        // getByName(域名) 会触发 DNS 解析——可能阻塞、可能失败，生产代码要意识到这点
        try {
            InetAddress local = InetAddress.getLocalHost();
            System.out.println("  本机: " + local.getHostName() + " / " + local.getHostAddress());
        } catch (IOException e) {
            System.out.println("  本机名解析失败（真实环境也会发生）: " + e.getClass().getSimpleName());
        }
        System.out.println("IP 定位机器，端口定位机器上的进程；1024 以下要管理员权限，别硬占");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：最简 TCP 回显——服务端与客户端的完整生命周期
    // ------------------------------------------------------------------
    static void demoEchoServer() throws Exception {
        System.out.println("== 第 2 段：最简 TCP 回显 ==");

        // 只绑回环地址：外面连不进来，示例更安全；端口 0 = 系统自动分配
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            int port = server.getLocalPort();

            Thread serverThread = new Thread(() -> {
                try (Socket socket = server.accept()) {      // 阻塞等人来连（BIO 的 B）
                    BufferedReader in = reader(socket);
                    PrintWriter out = writer(socket);
                    String line;
                    // TCP 是【无边界的字节流】——按行读的前提是双方约定了换行作分隔（应用层协议）
                    while ((line = in.readLine()) != null) {
                        out.println("回显: " + line);        // println 自带的换行就是协议的一部分
                        if ("bye".equals(line)) break;       // 约定的结束信号
                    }
                } catch (IOException e) {
                    System.out.println("  [server] 连接结束");
                }
            }, "echo-server");
            serverThread.start();

            try (Socket socket = new Socket("localhost", port)) {   // 三次握手发生在 connect 里
                PrintWriter out = writer(socket);
                BufferedReader in = reader(socket);
                for (String msg : new String[]{"你好", "第二条消息", "bye"}) {
                    out.println(msg);                        // 写一行
                    System.out.println("  客户端收到: " + in.readLine());   // 等一行回显
                }
            }
            serverThread.join();
        }
        System.out.println("accept 建立连接 → 流式读写 → 约定信号关连接：TCP 服务端的最小骨架");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：多客户端——accept 循环 + 线程池
    // ------------------------------------------------------------------
    static void demoMultiClient() throws Exception {
        System.out.println("== 第 3 段：多客户端 ==");

        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            int port = server.getLocalPort();

            // 一连接一任务：accept 循环只管接客，处理交给线程池（复用《线程池》篇的结论）
            AtomicInteger seq = new AtomicInteger();
            ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "tcp-worker-" + seq.incrementAndGet());
                return t;
            });

            Thread acceptor = new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {           // 收满 3 个连接就打烊
                        Socket socket = server.accept();
                        pool.submit(() -> handleOneClient(socket));
                    }
                } catch (IOException e) {
                    System.out.println("  [server] accept 结束");
                }
            }, "acceptor");
            acceptor.start();

            // 3 个客户端同时连上来（各自的输出顺序不保证）
            Thread[] clients = new Thread[3];
            for (int i = 0; i < 3; i++) {
                final int id = i + 1;
                clients[i] = new Thread(() -> {
                    try (Socket socket = new Socket("localhost", port)) {
                        PrintWriter out = writer(socket);
                        BufferedReader in = reader(socket);
                        out.println("客户端" + id + " 的请求");
                        System.out.println("  客户端" + id + " 收到: " + in.readLine());
                    } catch (IOException e) {
                        System.out.println("  客户端" + id + " 出错: " + e.getClass().getSimpleName());
                    }
                });
                clients[i].start();
            }
            for (Thread c : clients) c.join();
            acceptor.join();
            pool.shutdown();                                // 自己建的池自己收尾
        }
        System.out.println("这就是 BIO 的天花板：连接数 = 线程数，几千连接就把线程耗光——");
        System.out.println("海量连接要靠 NIO 多路复用（见《NIO基础》第 4 段的 Selector）");
        System.out.println();
    }

    /** 线程池里的任务：读一行、回一行、关连接。 */
    static void handleOneClient(Socket socket) {
        try (Socket s = socket;                              // 接管关闭责任
             BufferedReader in = reader(s);
             PrintWriter out = writer(s)) {
            String line = in.readLine();
            out.println("由 " + Thread.currentThread().getName() + " 处理: " + line);
        } catch (IOException e) {
            System.out.println("  [worker] 处理连接出错: " + e.getClass().getSimpleName());
        }
    }

    // ------------------------------------------------------------------
    // 第 4 段：SO_TIMEOUT——不能信任对面一定会回话
    // ------------------------------------------------------------------
    static void demoTimeout() throws Exception {
        System.out.println("== 第 4 段：SO_TIMEOUT ==");

        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            int port = server.getLocalPort();

            // 装死服务器：接受了连接，但一言不发
            Thread ghost = new Thread(() -> {
                try (Socket ignored = server.accept()) {
                    sleep(1200);                             // 模拟对端卡死/网络黑洞
                } catch (IOException e) {
                    System.out.println("  [ghost] 结束");
                }
            }, "ghost-server");
            ghost.start();

            try (Socket socket = new Socket("localhost", port)) {
                socket.setSoTimeout(500);                    // 读操作最多等 500ms
                BufferedReader in = reader(socket);
                try {
                    in.readLine();                           // 对面装死，这里会卡……
                } catch (SocketTimeoutException e) {
                    System.out.println("  500ms 没等到数据 → SocketTimeoutException（而不是永久卡死）");
                }
            }
            ghost.join();
        }
        System.out.println("生产铁律：一切网络读都要有超时（SO_TIMEOUT / 连接超时 / NIO 的 Selector 轮询上限）");
        System.out.println();
    }

    // 流的编解码必须显式指定字符集——乱码的根源在《字节流与字符流》讲过
    static BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    static PrintWriter writer(Socket socket) throws IOException {
        // 第二个参数 true = println 后自动 flush，对端立刻能看到
        return new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }
}
