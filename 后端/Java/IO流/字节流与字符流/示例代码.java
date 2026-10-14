// 字节流与字符流学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：字节流读写文件
//   第 2 段：字符流与编码
//   第 3 段：缓冲流（装饰器模式）
//   第 4 段：经典文件复制实战
//   第 5 段：一行顶一节的工具方法（Files）

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class IoStreamDemo {

    public static void main(String[] args) throws Exception {
        demoByteStream();
        demoCharStream();
        demoBuffered();
        demoCopy();
        demoFilesHelpers();
    }

    // ------------------------------------------------------------------
    // 第 1 段：字节流——万物皆字节
    // ------------------------------------------------------------------
    static void demoByteStream() throws Exception {
        System.out.println("== 第 1 段：字节流 ==");

        // InputStream/OutputStream 是字节流抽象基类：按 0~255 的字节搬运，什么文件都能处理
        // （图片、音频、zip……以及"不知道内容是什么"的场合）
        Path file = Files.createTempFile("study-byte", ".bin");
        file.toFile().deleteOnExit();                    // 退出时清理临时文件

        try (OutputStream out = new FileOutputStream(file.toFile())) {
            out.write("Hello, 字节流!".getBytes(StandardCharsets.UTF_8)); // 字符串 → UTF-8 字节
            out.write(10);                               // 写单个字节（\n）
        }                                                 // try-with-resources 自动 close

        try (InputStream in = new FileInputStream(file.toFile())) {
            // read() 返回 int 而不是 byte：要用 -1 表示"读完了"，byte 装不下这个哨兵值；
            // 且 int 的低 8 位是无符号字节（0~255），避免 byte 的负数被误判
            int byteCount = 0;
            while (in.read() != -1) {                    // 一个字节一个字节地读
                byteCount++;
            }
            System.out.println("文件共 " + byteCount + " 字节（\"Hello, 字节流!\" 的 UTF-8 编码 + 换行）");
        }
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：字符流与编码——文本的专属通道
    // ------------------------------------------------------------------
    static void demoCharStream() throws Exception {
        System.out.println("== 第 2 段：字符流与编码 ==");

        Path file = Files.createTempFile("study-char", ".txt");
        file.toFile().deleteOnExit();

        // Writer/Reader 按"字符"读写，但字符最终仍是字节——必须明确指定编码！
        // OutputStreamWriter 是"字节流 → 字符流"的桥梁：把字符按 UTF-8 编码成字节写出
        try (Writer w = new OutputStreamWriter(
                new FileOutputStream(file.toFile()), StandardCharsets.UTF_8)) {
            w.write("工资：20000 元\n");
            w.write("姓名：小明\n");
        }

        // 用正确的编码读回来：InputStreamReader（字节 → 字符）+ BufferedReader 逐行读
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(file.toFile()), StandardCharsets.UTF_8))) {
            System.out.println("UTF-8 正确解码:");
            String line;
            while ((line = r.readLine()) != null) {      // readLine 去掉换行符，结束时返回 null
                System.out.println("  " + line);
            }
        }

        // 故意用 GBK 解码 UTF-8 的字节 → 乱码：编码不一致是一切乱码的根源
        byte[] bytes = Files.readAllBytes(file);
        String mojibake = new String(bytes, Charset.forName("GBK"));
        System.out.println("GBK 硬解 UTF-8 → " + mojibake.replace("\n", " / ").trim());
        System.out.println("  ← 每个汉字在 UTF-8 里占 3 字节，按 GBK 两个一切，全都对不上号");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：缓冲流——装饰器模式的教科书
    // ------------------------------------------------------------------
    static void demoBuffered() throws Exception {
        System.out.println("== 第 3 段：缓冲流 ==");

        Path file = Files.createTempFile("study-buf", ".txt");
        file.toFile().deleteOnExit();

        // BufferedXxx 不替换流，而是"包"在流外面加一层内存缓冲（默认 8KB）：
        // 没 buffer 时每 write 一次就系统调用一次；有 buffer 则攒一批再写，快几个数量级
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file.toFile()), StandardCharsets.UTF_8))) {
            bw.write("第一行：缓冲流在写");
            bw.newLine();                                 // 跨平台换行（Windows \r\n、Linux \n）
            bw.write("第二行： BufferedWriter.newLine");
        }                                                 // close 会先 flush 缓冲，数据落盘

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file.toFile()), StandardCharsets.UTF_8))) {
            System.out.println("readLine 读回:");
            br.lines().forEach(l -> System.out.println("  " + l));
        }
        System.out.println("装饰器模式：new BufferedWriter(new OutputStreamWriter(new FileOutputStream(...)))");
        System.out.println("—— 一层包一层，每层只加一个能力（缓冲 / 编码转换 / 文件落地）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：经典文件复制（面试与工作的保留节目）
    // ------------------------------------------------------------------
    static void demoCopy() throws Exception {
        System.out.println("== 第 4 段：文件复制 ==");

        Path src = Files.createTempFile("study-src", ".txt");
        src.toFile().deleteOnExit();
        Path dst = Files.createTempFile("study-dst", ".txt");
        dst.toFile().deleteOnExit();
        Files.write(src, "复制我这一行就够了".getBytes(StandardCharsets.UTF_8));

        // 标准套路：try-with-resources 管两个流 + 缓冲数组批量搬 + read 返回实际长度
        try (InputStream in = new FileInputStream(src.toFile());
             OutputStream out = new FileOutputStream(dst.toFile())) {
            byte[] buf = new byte[8192];                 // 8KB 缓冲：单字节循环复制慢百倍
            int len;
            while ((len = in.read(buf)) != -1) {         // read 返回这次实际读了多少字节
                out.write(buf, 0, len);                  // 只写实际读到的部分——最后一段往往不满
            }
        }
        System.out.println("复制后内容: " + Files.readString(dst));   // readString 默认 UTF-8
        System.out.println("注意 write(buf, 0, len) 的 len：最后一段不足 8192，写整段会带脏数据");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 5 段：现代工具一行搞定（详见《文件与Path》篇）
    // ------------------------------------------------------------------
    static void demoFilesHelpers() throws Exception {
        System.out.println("== 第 5 段：Files 工具 ==");

        Path from = Files.createTempFile("study-f", ".txt");
        from.toFile().deleteOnExit();
        Path to = Files.createTempFile("study-t", ".txt");
        to.toFile().deleteOnExit();
        Files.writeString(from, "Files.copy 一行 = 第 4 段整个循环", StandardCharsets.UTF_8);

        Files.copy(from, to, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Files.copy 复制: " + Files.readString(to));

        // Files.lines 返回 Stream<String>——逐行处理文件的现代姿势（配合 Lambda 与 Stream）
        try (var lines = Files.lines(from, StandardCharsets.UTF_8)) {   // Stream 也占资源，要关闭
            lines.map(String::trim).forEach(l -> System.out.println("  行: " + l));
        }
        System.out.println();
    }
}
