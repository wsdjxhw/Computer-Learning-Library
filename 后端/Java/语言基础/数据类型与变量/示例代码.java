// 数据类型与变量学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：八大基本类型
//   第 2 段：整型溢出与浮点精度陷阱
//   第 3 段：类型转换（自动提升与强制转换）
//   第 4 段：包装类与自动装箱/拆箱
//   第 5 段：Integer 缓存陷阱（== 与 equals）

import java.util.HashMap;
import java.util.Map;

class DataTypeDemo {

    public static void main(String[] args) {
        demoPrimitiveTypes();
        demoOverflowAndPrecision();
        demoConversion();
        demoWrapperAndBoxing();
        demoIntegerCache();
    }

    // ------------------------------------------------------------------
    // 第 1 段：八大基本类型
    // ------------------------------------------------------------------
    static void demoPrimitiveTypes() {
        System.out.println("== 第 1 段：八大基本类型 ==");

        // Java 是静态强类型语言：变量类型声明后不可变，编译期就检查类型匹配
        // 4 种整型：全部有符号，位数决定能表示的范围
        byte tiny = 127;              // 8 位，-128 ~ 127
        short small = 32_000;         // 16 位；数字里的下划线只为可读，编译时忽略
        int count = 2_000_000_000;    // 32 位，整数字面量的默认类型
        long big = 9_000_000_000L;    // 64 位；字面量超出 int 范围必须加 L

        // 2 种浮点型：按 IEEE 754 标准存储，无法精确表示大部分小数
        float f = 3.14f;              // 32 位，字面量带 f
        double d = 3.141592653589793; // 64 位，小数字面量的默认类型

        // 1 种字符型 + 1 种布尔型
        char ch = '中';               // 16 位无符号，存一个 UTF-16 码元，装得下汉字
        boolean ok = true;            // 只有 true / false，与整数 0/1 无关（区别于 C）

        System.out.println("整型: byte=" + tiny + ", short=" + small + ", int=" + count + ", long=" + big);
        System.out.println("浮点: float=" + f + ", double=" + d);
        System.out.println("char='" + ch + "', boolean=" + ok);

        // 每种基本类型都有配套包装类，其中的常量记录了类型边界——比死记硬背可靠
        System.out.println("int  范围: " + Integer.MIN_VALUE + " ~ " + Integer.MAX_VALUE);
        System.out.println("byte 范围: " + Byte.MIN_VALUE + " ~ " + Byte.MAX_VALUE);
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：溢出与精度
    // ------------------------------------------------------------------
    static void demoOverflowAndPrecision() {
        System.out.println("== 第 2 段：溢出与精度陷阱 ==");

        // 整型溢出：超出范围不会报错，而是"绕回"到另一端（像里程表归零）
        int max = Integer.MAX_VALUE;
        System.out.println("Integer.MAX_VALUE + 1 = " + (max + 1) + "  ← 溢出成最小值！");

        // 溢出大多来自"先乘后除"的中间结果，即使最终值在范围内也会错
        int million = 1_000_000;
        System.out.println("million * million / million = " + (million * million / million)
                + "  ← 中间结果 10^12 已溢出，期望 1000000");

        // 大数值应换 long（或 java.math.BigInteger）
        long correct = (long) million * million / million;
        System.out.println("先转 long 再算 = " + correct);

        // 浮点精度：二进制无法精确表示 0.1、0.2 这类十进制小数
        System.out.println("0.1 + 0.2 = " + (0.1 + 0.2) + "  ← 不等于 0.3，别用 == 比较浮点");
        // 涉及金额等精确计算应使用 BigDecimal，详见《知识详解.md》误区部分
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：类型转换
    // ------------------------------------------------------------------
    static void demoConversion() {
        System.out.println("== 第 3 段：类型转换 ==");

        // 自动提升：小容量 → 大容量，永远安全，编译器自动完成
        int i = 100;
        long l = i;        // int → long：扩宽，无信息丢失
        double d = l;      // long → double：自动提升
        System.out.println("int 100 → long → double: " + i + " → " + l + " → " + d);

        // 强制转换：大容量 → 小容量必须显式写 (目标类型)，可能丢数据
        double pi = 3.99;
        int truncated = (int) pi;   // 浮点 → 整型：直接截断小数，不是四舍五入
        System.out.println("(int) 3.99 = " + truncated + "  ← 截断，不是 4");

        // byte 的 128：先溢出再截断
        byte wrapped = (byte) 128;
        System.out.println("(byte) 128 = " + wrapped + "  ← 8 位装不下 128，绕回 -128");

        // char 本质是无符号 16 位整数，可与 int 互转
        char c = 'A';
        System.out.println("'A' + 1 转回 char = " + (char) (c + 1) + "（A 的编码是 " + (int) c + "）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：包装类与自动装箱/拆箱
    // ------------------------------------------------------------------
    static void demoWrapperAndBoxing() {
        System.out.println("== 第 4 段：包装类与装箱拆箱 ==");

        // 基本类型不是对象：不能为 null、不能放进集合、没有方法。
        // 包装类把它们包成对象。Java 5 起支持自动装箱/拆箱，两种写法可互相赋值：
        Integer boxed = 5;      // 自动装箱：int 5 → Integer.valueOf(5)
        int unboxed = boxed;    // 自动拆箱：Integer → intValue()

        // 互转的常用工具方法
        String s = "123";
        int parsed = Integer.parseInt(s);      // 字符串 → int（内容不是数字会抛 NumberFormatException）
        String back = String.valueOf(parsed);  // int → 字符串
        System.out.println("parseInt(\"123\")=" + parsed + ", String.valueOf(123)=\"" + back + "\""
                + "，拆箱后参与运算: " + (unboxed + 1));

        // 拆箱的隐患（生产环境 NPE 重灾区）：Map 取不存在的键返回 null，直接当 int 用即炸
        Map<String, Integer> stock = new HashMap<>();   // 故意什么都没放
        Integer count = stock.get("苹果");               // get 不存在的键 → null
        try {
            int value = count;   // 对 null 自动拆箱 → NullPointerException
            System.out.println("苹果库存: " + value);
        } catch (NullPointerException e) {
            System.out.println("对 null 拆箱抛出: NullPointerException（真实场景：map.get 结果直接赋给 int）");
        }
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 5 段：Integer 缓存陷阱
    // ------------------------------------------------------------------
    static void demoIntegerCache() {
        System.out.println("== 第 5 段：Integer 缓存 ==");

        // 自动装箱走 Integer.valueOf()，它缓存了 -128 ~ 127 的对象
        Integer a = 127, b = 127;
        Integer c = 128, d = 128;

        // == 比较引用（是否同一个对象）；equals 比较内容
        System.out.println("127 == 127 ? " + (a == b)          // 同一缓存对象 → true
                + "；128 == 128 ? " + (c == d));                // 超出缓存，两个新对象 → false
        System.out.println("128 equals 128 ? " + c.equals(d)); // equals 比较数值 → true

        // 结论：包装类型一律用 equals 判断相等，绝不用 ==
        // 面试高频追问：为什么 127 true、128 false？——见《知识详解.md》原理部分
        System.out.println();
    }
}
