// 文件与 Path 学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：File 老类的用法与痛点
//   第 2 段：Path——现代路径表达
//   第 3 段：Files——一个类打点一切文件操作
//   第 4 段：walk 遍历目录树（含清理）

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

class PathFilesDemo {

    public static void main(String[] args) throws Exception {
        demoLegacyFile();
        demoPath();
        demoFilesApi();
        demoWalk();
    }

    // ------------------------------------------------------------------
    // 第 1 段：File 老类——认识它，但别再首选它
    // ------------------------------------------------------------------
    static void demoLegacyFile() {
        System.out.println("== 第 1 段：File 老类 ==");

        File self = new File("示例代码.java");       // 相对路径基准是 user.dir（启动 java 的目录）

        System.out.println("exists=" + self.exists()
                + ", isFile=" + self.isFile()
                + ", length=" + self.length() + " 字节");

        // 痛点演示：mkdir() 失败只返回 false，不告诉你为什么（没权限？已存在？父目录缺失？）
        File impossible = new File("Z:/不存在的盘/子目录");
        System.out.println("mkdir() 失败只得到: " + impossible.mkdir()
                + " ← 没有异常、没有原因（这就是 File 被淘汰的理由之一）");
        System.out.println("File 的其他毛病：大多方法返回 boolean 而非抛异常、元数据获取慢、跨平台行为不一");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：Path——路径的正确表达
    // ------------------------------------------------------------------
    static void demoPath() {
        System.out.println("== 第 2 段：Path ==");

        // Path.of（Java 11+）：路径对象本身只是"字符串的语义化"，不代表文件存在
        Path messy = Path.of("data", "..", "logs", "app.log");
        System.out.println("原始: " + messy);
        System.out.println("normalize 去掉 ..: " + messy.normalize());   // data/../logs/app.log → logs/app.log

        Path base = Path.of("D:", "study");
        Path full = base.resolve("notes/a.txt");      // resolve：base 之下拼子路径
        System.out.println("resolve 拼接: " + full);
        System.out.println("getFileName: " + full.getFileName()
                + "，parent: " + full.getParent()
                + "，绝对路径: " + Path.of("notes.txt").toAbsolutePath()
                + "（基准是 user.dir）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：Files——一个工具类打点一切
    // ------------------------------------------------------------------
    static void demoFilesApi() throws IOException {
        System.out.println("== 第 3 段：Files ==");

        // 建目录（多级）/ 写 / 读：失败抛 IOException 并带原因，比 File 的 boolean 体面得多
        Path dir = Files.createTempDirectory("study-nio");
        Path file = dir.resolve("配置.txt");

        Files.writeString(file, "name=学习库\nlang=java", StandardCharsets.UTF_8); // 显式编码，不赌默认值
        System.out.println("写入后 size = " + Files.size(file) + " 字节");

        String content = Files.readString(file);      // 读小文件一步到位（默认 UTF-8）
        System.out.println("readString 读回:\n" + content.replace("\n", " / "));

        Path copy = dir.resolve("配置-副本.txt");
        Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING);   // 已存在时覆盖而非抛异常

        // Files.list 返回的 Stream 持有目录句柄——【必须关闭】，否则 Windows 下连目录都删不掉
        long fileCount;
        try (Stream<Path> ls = Files.list(dir)) {
            fileCount = ls.count();
        }
        System.out.println("copy 完成，目录内文件数 = " + fileCount);

        Files.delete(copy);                           // 删除单个文件
        Files.delete(file);                           // 两个文件都删，目录才是空的
        Files.delete(dir);                            // 目录非空时 delete 直接抛 DirectoryNotEmptyException
        System.out.println("delete 清理完毕，exists = " + Files.exists(dir));
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：walk 遍历目录树——Stream 时代的目录递归
    // ------------------------------------------------------------------
    static void demoWalk() throws IOException {
        System.out.println("== 第 4 段：walk 目录树 ==");

        // 造一棵测试目录树：project/{src/{Main.java, Util.java}, doc/说明.txt, pom.xml}
        Path root = Files.createTempDirectory("study-tree");
        Files.createDirectories(root.resolve("src"));
        Files.createDirectories(root.resolve("doc"));
        Files.writeString(root.resolve("src/Main.java"), "class Main {}");
        Files.writeString(root.resolve("src/Util.java"), "class Util {}");
        Files.writeString(root.resolve("doc/说明.txt"), "目录树演示");
        Files.writeString(root.resolve("pom.xml"), "<project/>");

        // walk 一次吐出整棵树的 Stream<Path>（深度优先）——替代手写递归
        try (Stream<Path> tree = Files.walk(root)) {
            System.out.println("整棵树（相对路径）:");
            tree.sorted()                             // 排序只为输出稳定，walk 本身顺序不保证
                    .map(p -> root.relativize(p))     // relativize：算出相对 root 的路径
                    .forEach(p -> System.out.println("  " + p));
        }

        // 组合拳：树里筛 .java 文件并统计行数——老写法要十行递归，现在三行
        try (Stream<Path> tree = Files.walk(root)) {
            long javaLines = tree.filter(p -> p.toString().endsWith(".java"))
                    .mapToLong(p -> {
                        try { return Files.lines(p).count(); }
                        catch (IOException e) { return 0; }
                    })
                    .sum();
            System.out.println(".java 文件总行数 = " + javaLines);
        }

        // 递归删除：walk 出的路径【倒序】删——先删文件再删目录，否则非空目录删不掉
        try (Stream<Path> tree = Files.walk(root)) {
            tree.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); }
                        catch (IOException e) { /* 演示代码从简 */ }
                    });
        }
        System.out.println("递归删除后 exists = " + Files.exists(root));
        System.out.println("（JDK 21 起可直接用 Files.walkFileTree 或第三方更好地处理删除失败）");
        System.out.println();
    }
}
