// JVM 调优工具与 OOM 排查实战学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：进程内体检——PID、堆读数、线程数（jps/jstat 的进程内视角）
//   第 2 段：线程快照——jstack 在进程内能拿到什么
//   第 3 段：制造一次内存泄漏（受控；小堆下亲眼看 OOM 现场）
//   第 4 段：进程外命令速查（jps/jstat/jmap/jstack/jcmd/JFR/Arthas）
//
// 实战玩法（亲眼看 OOM 现场与堆转储）：
//   java -Xmx64m -XX:+HeapDumpOnOutOfMemoryError "-Dstdout.encoding=UTF-8" 示例代码.java

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;

class TroubleshootingDemo {

    // 模拟泄漏的【只进不出】集合：static 意味着它和应用同寿命，永远有 GC Roots 指着它
    static final List<byte[]> LEAK = new ArrayList<>();

    public static void main(String[] args) {
        demoVitals();
        demoThreads();
        demoLeak();
        demoCheatSheet();
    }

    // ------------------------------------------------------------------
    // 第 1 段：进程内体检——外部工具看到的第一屏数据
    // ------------------------------------------------------------------
    static void demoVitals() {
        System.out.println("== 第 1 段：进程内体检 ==");
        System.out.println("  进程号 PID: " + ProcessHandle.current().pid()
                + "（jps/jcmd 就是靠 PID 找到进程）");
        Runtime rt = Runtime.getRuntime();
        System.out.println("  堆: 已用 " + mb(rt.totalMemory() - rt.freeMemory())
                + " / 已申请 " + mb(rt.totalMemory())
                + " / 上限 " + mb(rt.maxMemory()));
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        System.out.println("  堆明细:   " + mem.getHeapMemoryUsage());
        System.out.println("  非堆明细: " + mem.getNonHeapMemoryUsage() + "（元空间在这里面）");
        System.out.println("  活跃线程数: " + Thread.activeCount() + "（jstack 看每个线程的明细）");
        System.out.println("  对照命令：jstat -gc <pid> 1000 → 每秒一屏同样的数字");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：线程快照——jstack 的进程内视角
    // ------------------------------------------------------------------
    static void demoThreads() {
        System.out.println("== 第 2 段：线程快照 ==");
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            System.out.println("  线程: " + t.getName() + "（" + t.getState() + "）");
        }
        System.out.println("  CPU 飙高的套路：top 定位线程号 → 转 16 进制 → jstack 里找 nid=0x... 的栈");
        System.out.println("  死锁排查：jstack <pid> 会直接打印 Found one Java-level deadlock");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：制造泄漏——static 集合只进不出
    // ------------------------------------------------------------------
    static void demoLeak() {
        System.out.println("== 第 3 段：泄漏现场 ==");
        long max = Runtime.getRuntime().maxMemory();

        if (max <= 256L * 1024 * 1024) {
            // 小堆模式（如 -Xmx64m）：一路塞到 OOM，看现场长什么样
            try {
                for (int i = 1; ; i++) {
                    LEAK.add(new byte[1024 * 1024]);           // 1MB 1MB 地塞
                    if (i % 16 == 0) System.out.println("  已塞入 " + i + " MB...");
                }
            } catch (OutOfMemoryError e) {
                System.out.println("  OutOfMemoryError: " + e.getMessage());
                System.out.println("  生产正确姿势：启动就预埋 -XX:+HeapDumpOnOutOfMemoryError，");
                System.out.println("  OOM 时自动生成 java_pid<pid>.hprof → MAT/VisualVM 打开看支配树");
            }
        } else {
            // 默认大堆：演示泄漏雏形，不真撑爆机器
            for (int i = 0; i < 32; i++) LEAK.add(new byte[1024 * 1024]);
            System.out.println("  static List 已塞入 32MB 且永远无法回收（只进不出的泄漏雏形）");
            System.out.println("  当前堆已用: " + mb(Runtime.getRuntime().totalMemory()
                    - Runtime.getRuntime().freeMemory()));
            System.out.println("  想看真正的 OOM 现场（约 30 秒内必现）：");
            System.out.println("    java -Xmx64m -XX:+HeapDumpOnOutOfMemoryError 示例代码.java");
        }
        System.out.println("  排查思路：内存泄漏必有一个【被 static/缓存/监听器持有】且只增不减的集合；");
        System.out.println("  jmap -histo 先看哪类对象最多 → 堆转储 + MAT 支配树定位【谁引用着它不放手】");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：进程外命令速查
    // ------------------------------------------------------------------
    static void demoCheatSheet() {
        System.out.println("== 第 4 段：命令速查 ==");
        System.out.println("  jps -l                  找到 Java 进程号（第一个命令永远是它）");
        System.out.println("  jstat -gc <pid> 1000    每秒看 GC：各区容量/使用、YGC/FGC 次数与耗时");
        System.out.println("  jmap -histo <pid>       按类统计对象数量与大小（轻量，排查第一步）");
        System.out.println("  jmap -dump:format=b,file=heap.hprof <pid>   导出堆转储（重操作，避开高峰）");
        System.out.println("  jstack <pid>            线程快照：死锁、CPU 飙高、线程卡死");
        System.out.println("  jcmd <pid> help         万能入口（GC.heap_dump / JFR.start 都靠它）");
        System.out.println("  JFR（飞行记录器）: JDK 11+ 自带，开销 <1%，生产可常开，事件级回放");
        System.out.println("  Arthas: 线上诊断神器，dashboard/thread/heapdump，免重启 attach");
        System.out.println("  常用启动参数: -Xms/-Xmx 堆、-Xss 栈、-XX:MaxMetaspaceSize、");
        System.out.println("               -XX:+HeapDumpOnOutOfMemoryError、-Xlog:gc（GC 日志）");
    }

    /** 把字节数格式化成 MB。 */
    static String mb(long bytes) {
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
