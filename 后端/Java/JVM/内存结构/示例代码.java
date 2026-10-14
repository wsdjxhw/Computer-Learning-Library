// JVM 内存结构（运行时数据区）学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：虚拟机栈——递归爆栈现场（StackOverflowError）
//   第 2 段：堆——对象的家与 Runtime 内存读数
//   第 3 段：方法区/元空间与字符串常量池
//   第 4 段：程序计数器与本地方法栈 + 全景总结
//
// 进阶玩法（体会参数影响，爆栈深度会明显变化）：
//   java -Xss256k "-Dstdout.encoding=UTF-8" 示例代码.java   ← 栈减到 256KB，几百层就爆

class MemoryStructureDemo {

    public static void main(String[] args) {
        demoStack();
        demoHeap();
        demoMethodArea();
        demoOverview();
    }

    // ------------------------------------------------------------------
    // 第 1 段：虚拟机栈——每调用一个方法压一个栈帧，递归不返回就只压不弹
    // ------------------------------------------------------------------
    static int depth = 0;                 // 记录递归到过多少层（static 变量存在堆里）

    static void recurse() {
        depth++;                          // 每进一层 +1
        recurse();                        // 无限递归：栈帧只增不减，直到栈被塞满
    }

    static void demoStack() {
        System.out.println("== 第 1 段：虚拟机栈 ==");
        try {
            recurse();
        } catch (StackOverflowError e) {
            // 教学 Catch：生产代码绝不该捕获 Error——这里只为【亲眼看到】爆栈发生在第几层
            System.out.println("  递归到第 " + depth + " 层爆栈：StackOverflowError（深度每次不同）");
        }
        System.out.println("  每次方法调用压入一个【栈帧】（存局部变量表、操作数栈、返回地址）");
        System.out.println("  栈是线程私有的：一个线程一条栈——爆栈只炸自己，不影响别的线程");
        System.out.println("  调 -Xss 调小栈 → 深度变小；方法的局部变量越多 → 每帧越大 → 深度也变小");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：堆——所有 new 出来的对象住在这里（线程共享）
    // ------------------------------------------------------------------
    static void demoHeap() {
        System.out.println("== 第 2 段：堆 ==");
        Runtime rt = Runtime.getRuntime();
        System.out.println("  堆上限 maxMemory:    " + mb(rt.maxMemory())
                + "（≈ -Xmx，默认约为物理内存 1/4）");
        System.out.println("  已申请 totalMemory: " + mb(rt.totalMemory())
                + "（从 -Xms 起步，按需向操作系统要）");
        System.out.println("  空闲 freeMemory:    " + mb(rt.freeMemory())
                + "（已申请但还没被对象占用）");
        System.out.println("  已用 ≈ total - free = " + mb(rt.totalMemory() - rt.freeMemory()));

        byte[] block = new byte[10 * 1024 * 1024];      // new 一个 10MB 的对象 → 落在堆上
        System.out.println("  new 了 10MB byte[] 后，已用 ≈ "
                + mb(rt.totalMemory() - rt.freeMemory()) + "（GC 时机不同读数会漂）");
        System.out.println("  block 只是个【栈上的引用】（4~8 字节），真正的 10MB 数据在堆里——");
        System.out.println("  经典考点：引用在栈，对象在堆；基本类型局部变量直接存栈帧里");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：方法区（元空间）与字符串常量池——JDK 8 起的版图变化
    // ------------------------------------------------------------------
    static void demoMethodArea() {
        System.out.println("== 第 3 段：方法区/元空间 ==");
        // 类的元数据（类名、字段、方法字节码）在 Metaspace——JDK 8 起用【本地内存】实现，
        // 取代了堆里的永久代；但 static 变量与 Class 对象本身仍在堆中
        Class<?> c = MemoryStructureDemo.class;
        System.out.println("  " + c.getSimpleName() + " 的类元数据在元空间；Class 实例和 static 变量在堆");

        // 字符串常量池：JDK 7 起也从永久代搬到了堆里
        String s1 = "学习库";                  // 字面量 → 池中只存一份，所有字面量共享它
        String s2 = new String("学习库");      // new 强制在堆里再造一个【新】对象
        String s3 = s2.intern();               // intern() 返回池中那一份
        System.out.println("  s1 == s2: " + (s1 == s2) + "（new 造了新对象，地址不同）");
        System.out.println("  s1 == s3: " + (s1 == s3) + "（intern 拿回池中共享的那份，地址相同）");
        System.out.println("  结论：比较内容永远用 equals；循环里 new String 是白白制造垃圾的坏味道");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：程序计数器与本地方法栈 + 全景图
    // ------------------------------------------------------------------
    static void demoOverview() {
        System.out.println("== 第 4 段：全景图 ==");
        System.out.println("  线程共享：堆（对象/数组/Class实例/static） + 方法区（类元数据，元空间实现）");
        System.out.println("  线程私有：虚拟机栈（栈帧） + 程序计数器 + 本地方法栈（服务 native 方法）");
        System.out.println("  程序计数器：记录当前线程执行到哪条字节码——线程切换回来能接着干，全 JVM 唯一不会 OOM 的区");
        System.out.println("  栈会 StackOverflowError（第 1 段）；堆和元空间会 OutOfMemoryError");
        System.out.println("  热身问题：一个 static 大集合无限变大，最后撑爆的是哪个区？——堆（对象在堆）");
        System.out.println("  变形题：无限递归爆的是哪个区？——虚拟机栈（第 1 段的现场）");
    }

    /** 把字节数格式化成 MB，读数友好。 */
    static String mb(long bytes) {
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
