// GC 与垃圾收集器学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：可达性分析——强引用挡回收，弱引用被回收
//   第 2 段：四种引用强度——强/软/弱/虚
//   第 3 段：程序自己观察 GC（收集器名字就藏在 MXBean 里）
//   第 4 段：收集器演进速查（Serial → Parallel → CMS → G1 → ZGC）
//
// 想亲眼看 GC 日志（强烈推荐）：
//   java "-Xlog:gc" "-Dstdout.encoding=UTF-8" 示例代码.java

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

class GcDemo {

    /** 1MB 的对象，让 GC 的动作【看得见】。 */
    static class Big {
        byte[] data = new byte[1024 * 1024];
    }

    public static void main(String[] args) throws Exception {
        demoReachability();
        demoReferenceStrength();
        demoGcBeans();
        demoCollectors();
    }

    // ------------------------------------------------------------------
    // 第 1 段：可达性分析——判定垃圾的标准是【从 GC Roots 不可达】
    // ------------------------------------------------------------------
    static void demoReachability() throws InterruptedException {
        System.out.println("== 第 1 段：可达性分析 ==");

        Big strong = new Big();                                // 强引用：GC 永远不敢动
        WeakReference<Big> weak = new WeakReference<>(new Big());  // 弱引用：下次 GC 就回收

        System.out.println("  GC 前，弱引用拿得到对象吗: " + (weak.get() != null));
        for (int i = 1; i <= 5 && weak.get() != null; i++) {   // 最多试 5 轮，通常 1~2 轮就清
            System.gc();                        // 【建议】JVM 做一次 Full GC（不是命令）
            Thread.sleep(100);                  // 给收集器一点完成时间
        }
        System.out.println("  GC 后：强引用还在 " + (strong != null)
                + " / 弱引用对象被回收 " + (weak.get() == null));

        strong = null;                          // 切断强引用 → 那个 Big 同样变成垃圾
        System.out.println("  判定垃圾不是数引用（引用计数有循环引用难题），");
        System.out.println("  而是【从 GC Roots 出发走不到】——GC Roots：栈上局部变量、");
        System.out.println("  static 字段、常量、JNI 引用等这些【肯定还活着】的起点");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：四种引用强度——回收的【优先级】不同
    // ------------------------------------------------------------------
    static void demoReferenceStrength() throws InterruptedException {
        System.out.println("== 第 2 段：四种引用 ==");
        System.gc();                            // 先清场，让下面的观察更干净

        SoftReference<Big> soft = new SoftReference<>(new Big());
        Thread.sleep(100);
        System.out.println("  软引用（内存不够才回收）GC 后通常还在: " + (soft.get() != null)
                + "（不保证——取决于内存压力，这正是它的语义）");
        System.out.println("  强引用：不回收——宁可 OOM 也不动（第 1 段的 strong）");
        System.out.println("  弱引用：活不过下一次 GC（第 1 段的 weak）");
        System.out.println("  虚引用：形同虚设，只收【回收通知】——JDK 9+ 用 Cleaner 替代 finalize 做资源清理");
        System.out.println("  用途速记：软引用=内存敏感的缓存；弱引用=不阻碍回收的关联（WeakHashMap）；");
        System.out.println("            虚引用=堆外内存的释放通知（DirectByteBuffer 的清理机制）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：程序自己观察 GC——收集器的名字就写在 MXBean 里
    // ------------------------------------------------------------------
    static void demoGcBeans() throws InterruptedException {
        System.out.println("== 第 3 段：程序自己看 GC ==");
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.println("  收集器: " + gc.getName() + "，已收集 " + gc.getCollectionCount()
                    + " 次，累计耗时 " + gc.getCollectionTime() + " ms");
        }
        System.out.println("  ↑ 名字里直接写着当前用的收集器——JDK 9+ 默认就是 G1（本库 JDK 21）");

        long before = totalCount();
        List<byte[]> junk = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            junk.add(new byte[1024 * 1024]);    // 快速制造 64MB 临时对象，逼 GC 出手
        }
        junk = null;                            // 切断引用，全部变成垃圾
        System.gc();
        Thread.sleep(200);
        System.out.println("  制造并丢弃 64MB 对象后，收集次数: " + before + " → " + totalCount()
                + "（只增不减；具体加几次每次不同）");
        System.out.println("  外部工具等价物：jstat -gc <pid> 1000 每秒看同一组数字");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：收集器演进速查
    // ------------------------------------------------------------------
    static void demoCollectors() {
        System.out.println("== 第 4 段：收集器速查 ==");
        System.out.println("  Serial   单线程，小内存/客户端（-XX:+UseSerialGC）");
        System.out.println("  Parallel 多线程并行，吞吐量优先——JDK 8 的默认（-XX:+UseParallelGC）");
        System.out.println("  CMS      并发标记清除，低停顿先驱——JDK 9 废弃、14 移除（历史考点）");
        System.out.println("  G1       Region 化分代 + 停顿目标可控——JDK 9+ 默认（-XX:+UseG1GC）");
        System.out.println("  ZGC      着色指针 + 读屏障，停顿 <1ms——JDK 15 转正（-XX:+UseZGC）");
        System.out.println("  算法主线：新生代用复制算法（对象朝生夕死，复制最划算）、");
        System.out.println("            老年代用标记-清除/标记-整理；CMS 的碎片问题正是它被淘汰的原因之一");
    }

    /** 所有收集器的收集次数之和（jstat 里的 YGC+FGC 就是这组数字）。 */
    static long totalCount() {
        long sum = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = gc.getCollectionCount();
            if (c > 0) sum += c;
        }
        return sum;
    }
}
