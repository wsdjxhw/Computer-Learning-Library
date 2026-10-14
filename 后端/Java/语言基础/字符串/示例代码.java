// 字符串（String）学习示例
// 运行方式：java 示例代码.java （UTF-8 终端可加 -Dstdout.encoding=UTF-8）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：String 不可变
//   第 2 段：字符串池与 == 陷阱
//   第 3 段：常用方法与文本块
//   第 4 段：循环拼接与 StringBuilder

class StringDemo {

    public static void main(String[] args) {
        demoImmutability();
        demoPool();
        demoCommonMethods();
        demoStringBuilder();
    }

    // ------------------------------------------------------------------
    // 第 1 段：String 不可变
    // ------------------------------------------------------------------
    static void demoImmutability() {
        System.out.println("== 第 1 段：String 不可变 ==");
        String s = "abc";

        // concat/replace/toUpperCase 等"修改"方法都返回新字符串，原串纹丝不动
        s.concat("def");
        System.out.println("s.concat(\"def\") 后 s = " + s + "  ← 原串没变");

        // 接住返回值才出现"变化"的假象：其实是 s 改指向了一个新对象
        s = s.concat("def");
        System.out.println("接住返回值后 s = " + s);

        // 引用赋值同理：t 和 s 指向同一对象，之后 s 换了目标，t 不受影响
        String t = s;
        s = s.toUpperCase();
        System.out.println("s=" + s + ", t=" + t + "  ← t 仍指向旧对象");
        // 不可变的意义：可以放心共享（字符串池）、线程安全、hashCode 可缓存（做 HashMap 键很合适）
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：字符串池与 == 陷阱
    // ------------------------------------------------------------------
    static void demoPool() {
        System.out.println("== 第 2 段：字符串池 ==");

        String a = "hello";              // 字面量：先查字符串池，有同内容对象就直接复用
        String b = "hello";              // → b 复用了 a 那个对象
        String c = new String("hello");  // new：强制在堆里新建对象，无视池

        System.out.println("a == b ? " + (a == b));         // true：池中复用，同一对象
        System.out.println("a == c ? " + (a == c));         // false：堆里新建，不同对象
        System.out.println("a.equals(c) ? " + a.equals(c)); // true：equals 比较内容
        // 结论：比较字符串内容一律用 equals，== 比较的是"是否同一个对象"（面试高频）
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：常用方法与文本块
    // ------------------------------------------------------------------
    static void demoCommonMethods() {
        System.out.println("== 第 3 段：常用方法 ==");
        String s = "  Hello, Java World  ";

        System.out.println("length()      = " + s.length());          // 长度（含空格）
        System.out.println("strip()       = \"" + s.strip() + "\"");  // 去首尾空白（Java 11+，比 trim 更懂 Unicode）
        System.out.println("charAt(2)     = " + s.charAt(2));         // 按下标取字符
        System.out.println("substring(2,9) = \"" + s.substring(2, 9) + "\""); // 含头不含尾 [2, 9)
        System.out.println("indexOf(\"Java\") = " + s.indexOf("Java"));       // 首次出现位置，找不到返回 -1
        System.out.println("contains(\"World\") = " + s.contains("World"));   // 是否包含
        System.out.println("replace(\"Java\",\"Python\") = " + s.replace("Java", "Python"));
        System.out.println("toUpperCase() = " + s.strip().toUpperCase());
        System.out.println("equalsIgnoreCase(\"  hello, java world  \") = "
                + s.equalsIgnoreCase("  hello, java world  "));       // 忽略大小写比较

        // split 按分隔符切开后得到数组，常配合循环使用
        for (String part : "apple,banana,cherry".split(",")) {
            System.out.println("  水果: " + part);
        }
        // String.join 反向拼接
        System.out.println("join(\"-\", ...) = " + String.join("-", "2026", "10", "14"));

        // 文本块（Java 15+）：三引号保留换行缩进，写多行文本不用一堆 \n 和 +
        String json = """
                {
                  "name": "Alice",
                  "age": 30
                }""";
        System.out.println("文本块:\n" + json);
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：循环拼接与 StringBuilder
    // ------------------------------------------------------------------
    static void demoStringBuilder() {
        System.out.println("== 第 4 段：循环拼接的性能 ==");

        int n = 10_000;
        // 方式一：循环里 += 。String 不可变 → 每轮都生成一个新串并整体复制，
        // n 轮累计复制 O(n^2) 个字符，规模一大性能崩塌
        long start = System.nanoTime();
        String s = "";
        for (int i = 0; i < n; i++) {
            s += "a";
        }
        long plusTime = System.nanoTime() - start;

        // 方式二：StringBuilder 内部维护可变字符数组，append 只在末尾追加，
        // 需要时才扩容（机制同动态数组），总代价 O(n)
        start = System.nanoTime();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append("a");
        }
        String result = sb.toString();   // 拼完一次性转成不可变的 String
        long builderTime = System.nanoTime() - start;

        System.out.println("拼接 " + n + " 次：");
        System.out.println("  String +=       : " + plusTime / 1_000_000 + " 毫秒");
        System.out.println("  StringBuilder   : " + builderTime / 1_000_000 + " 毫秒");
        System.out.println("  两者结果相同? " + s.equals(result));
        System.out.println();
        // 单行拼接直接用 + 就好（编译器会优化）；循环/多次拼接才需要 StringBuilder
    }
}
