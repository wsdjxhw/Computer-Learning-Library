// NIO 基础学习示例（Buffer / Channel / Selector）
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：Buffer——position/limit/capacity 三指针与 flip
//   第 2 段：FileChannel 读写文件
//   第 3 段：transferTo——零拷贝式的文件复制
//   第 4 段：Selector——一个线程管多条连接

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Set;

class NioDemo {

    public static void main(String[] args) throws Exception {
        demoBuffer();
        Path tmp = demoFileChannel();
        demoTransferTo(tmp);
        demoSelector();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    static void printState(String label, ByteBuffer buf) {
        System.out.println("  " + label + "：position=" + buf.position()
                + "，limit=" + buf.limit() + "，capacity=" + buf.capacity());
    }

    // ------------------------------------------------------------------
    // 第 1 段：Buffer——NIO 数据的搬运容器，先搞懂三个指针
    // ------------------------------------------------------------------
    static void demoBuffer() {
        System.out.println("== 第 1 段：Buffer 三指针 ==");

        ByteBuffer buf = ByteBuffer.allocate(8);       // 分配 8 字节：capacity=8 且不可变
        printState("allocate 后", buf);

        String data = "Hello";
        for (byte b : data.getBytes(StandardCharsets.UTF_8)) {
            buf.put(b);                                // 写一个字节，position 前进一格
        }
        printState("put 5 字节后", buf);               // position=5：已写 5 格，还能再写 3 格

        buf.flip();                                    // 写→读切换：limit=position(5)，position=0
        printState("flip 后", buf);

        System.out.print("  依次读出: ");
        while (buf.hasRemaining()) {                   // position < limit 就还有数据
            System.out.print((char) buf.get());        // 读一个字节，position 前进一格
        }
        System.out.println();
        printState("读完", buf);                       // position 追上 limit：读空了

        buf.clear();                                   // 指针复位（旧数据并没擦掉，只是可以被覆盖）
        printState("clear 后", buf);
        System.out.println("口诀：put 完 flip 再 get，用完 clear 复位——忘了 flip 读出的是空");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：FileChannel——Channel 只认 Buffer，不认数组
    // ------------------------------------------------------------------
    static Path demoFileChannel() throws Exception {
        System.out.println("== 第 2 段：FileChannel ==");

        Path tmp = Files.createTempFile("nio-demo", ".txt");   // 临时文件，退出前自动删
        tmp.toFile().deleteOnExit();

        // 写：数据先装进 Buffer，再整块交给 Channel（对流式 API 说再见）
        try (FileChannel out = FileChannel.open(tmp,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            ByteBuffer writeBuf = ByteBuffer.wrap("你好，NIO！".getBytes(StandardCharsets.UTF_8));
            out.write(writeBuf);
        }
        System.out.println("  写入 " + Files.size(tmp) + " 字节到 " + tmp.getFileName());

        // 读：Channel 把数据灌进 Buffer，flip 切读模式后才能取
        try (FileChannel in = FileChannel.open(tmp, StandardOpenOption.READ)) {
            ByteBuffer readBuf = ByteBuffer.allocate((int) Files.size(tmp));
            in.read(readBuf);
            readBuf.flip();
            System.out.println("  读回: " + StandardCharsets.UTF_8.decode(readBuf));
        }
        System.out.println();
        return tmp;                                    // 留给第 3 段当复制源
    }

    // ------------------------------------------------------------------
    // 第 3 段：transferTo——把复制这件事整个下沉给内核
    // ------------------------------------------------------------------
    static void demoTransferTo(Path src) throws Exception {
        System.out.println("== 第 3 段：transferTo 零拷贝 ==");

        Path dst = Files.createTempFile("nio-copy", ".txt");
        dst.toFile().deleteOnExit();

        // 传统复制：磁盘→内核缓冲→JVM 堆→再写回内核→磁盘（数据在用户态/内核态来回倒 4 次）
        // transferTo：内核里直接从源文件搬到目标文件，JVM 堆根本不沾手（零拷贝思想）
        try (FileChannel srcCh = FileChannel.open(src, StandardOpenOption.READ);
             FileChannel dstCh = FileChannel.open(dst, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            long copied = srcCh.transferTo(0, srcCh.size(), dstCh);
            System.out.println("  transferTo 搬运了 " + copied + " 字节（源与目标大小一致）");
        }
        System.out.println("Kafka/RocketMQ 大文件传输快，靠的就是这类【内核态直搬】的思路");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：Selector——一个线程同时盯多条连接
    // ------------------------------------------------------------------
    static void demoSelector() throws Exception {
        System.out.println("== 第 4 段：Selector ==");

        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("localhost", 0));   // 端口 0：系统自动分配，绝不冲突
        server.configureBlocking(false);              // 非阻塞是注册 Selector 的前提
        server.register(selector, SelectionKey.OP_ACCEPT);    // 先只关心【有人来连】
        int port = server.socket().getLocalPort();

        Thread serverThread = new Thread(() -> runEchoServer(selector), "nio-server");
        serverThread.setDaemon(true);                 // 兜底：万一没收摊成功也让 JVM 能退出
        serverThread.start();

        // 客户端用普通阻塞式 SocketChannel 即可（本段演示的主角是服务端的多路复用）
        try (SocketChannel client = SocketChannel.open()) {
            client.connect(new InetSocketAddress("localhost", port));
            for (String msg : new String[]{"hello", "nio"}) {
                client.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
                ByteBuffer echo = ByteBuffer.allocate(64);
                client.read(echo);                    // 阻塞等服务端回显
                echo.flip();
                System.out.println("  客户端收到回显: " + StandardCharsets.UTF_8.decode(echo));
            }
        }                                             // close 后服务端 read 返回 -1，自动收摊
        serverThread.join(3000);
        System.out.println("select() 一个调用同时盯着 ACCEPT/READ 多类事件——连接多但都闲着");
        System.out.println("（长连接网关、聊天室）时不用一线程一连接地硬扛，这就是多路复用");
        System.out.println();
    }

    /** 单线程 Selector 回显服务器：有连接就收，有数据就原样弹回。 */
    static void runEchoServer(Selector selector) {
        try {
            int rounds = 0;
            while (rounds++ < 150 && selector.isOpen()) {  // 轮数上限是兜底，正常几轮就退
                selector.select(200);                 // 等 200ms：有事件就返回，没事件超时返回
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();                      // 必须手动移除，否则下轮重复处理同一事件
                    if (key.isAcceptable()) {         // 事件 1：有人来连
                        SocketChannel ch = ((ServerSocketChannel) key.channel()).accept();
                        ch.configureBlocking(false);
                        ch.register(selector, SelectionKey.OP_READ);  // 改关心【有数据可读】
                        System.out.println("  [server] 接受新连接");
                    } else if (key.isReadable()) {    // 事件 2：某连接发来数据
                        SocketChannel ch = (SocketChannel) key.channel();
                        ByteBuffer buf = ByteBuffer.allocate(64);
                        int n = ch.read(buf);
                        if (n == -1) {                // 客户端关闭的信号
                            ch.close();
                            System.out.println("  [server] 客户端断开，收摊");
                            return;
                        }
                        buf.flip();
                        ch.write(buf);                // 原样写回 = 回显
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("  [server] 退出: " + e.getClass().getSimpleName());
        }
    }
}
