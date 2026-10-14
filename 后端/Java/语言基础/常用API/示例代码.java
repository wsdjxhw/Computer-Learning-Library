// 常用 API 学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：Object 的核心方法与 equals/hashCode 重写约定
//   第 2 段：Objects 工具类
//   第 3 段：日期时间 API（LocalDate / LocalDateTime / Duration）
//   第 4 段：BigDecimal 精确计算
//   第 5 段：System / Math / 格式化

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class CommonApiDemo {

    public static void main(String[] args) {
        demoObjectMethods();
        demoObjectsUtils();
        demoDateTime();
        demoBigDecimal();
        demoSystemAndMath();
    }

    // ------------------------------------------------------------------
    // 第 1 段：Object 的核心方法
    // ------------------------------------------------------------------
    static void demoObjectMethods() {
        System.out.println("== 第 1 段：Object 的核心方法 ==");

        // 所有类都隐式继承 Object。不重写 toString 时打印"类名@十六进制哈希"，没法看
        Object anything = new Object();
        System.out.println("Object 默认 toString: " + anything + "  ← 类名@哈希，没有信息量");
        Student plain = new Student("小明", 18);
        System.out.println("Student 重写后 toString(): " + plain);

        // == 比较引用：两个内容相同的学生是不同对象
        Student a = new Student("小明", 18);
        Student b = new Student("小明", 18);
        System.out.println("== 比较: " + (a == b) + "，equals 比较: " + a.equals(b));

        // 重写了 equals + hashCode 后，HashSet 能正确去重（只重写 equals 会出现两个"小明"）
        Set<Student> set = new HashSet<>();
        set.add(a);
        set.add(b);                    // hashCode 相同 → 同一桶 → equals 判等 → 拒绝重复
        System.out.println("HashSet 去重后 size = " + set.size() + "（equals/hashCode 成对重写的功劳）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：Objects 工具类
    // ------------------------------------------------------------------
    static void demoObjectsUtils() {
        System.out.println("== 第 2 段：Objects 工具 ==");

        String s = null;
        // Objects.equals(a, b)：先判空再比较，两个 null 也算相等，不会 NPE
        System.out.println("Objects.equals(null, null) = " + Objects.equals(s, null));
        System.out.println("s.equals(null) 会 NPE，Objects.equals 不会");

        // requireNonNull：入口快速失败——把 NPE 提前到参数检查处，堆栈更有意义
        try {
            Objects.requireNonNull(s, "name 不能为 null");
        } catch (NullPointerException e) {
            System.out.println("requireNonNull 抛出: " + e.getMessage());
        }

        // hash：把多个字段拼成一个 hashCode，比手写 31 累乘可靠（第 1 段 Student 就用了它）
        System.out.println("Objects.hash(\"小明\", 18) = " + Objects.hash("小明", 18));
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：日期时间 API（java.time，Java 8+）
    // ------------------------------------------------------------------
    static void demoDateTime() {
        System.out.println("== 第 3 段：日期时间 ==");

        // LocalDate 只有日期、LocalDateTime 带时间，都是【不可变】对象——
        // 所有"修改"方法返回新对象（和 String 一个思路），天然线程安全
        LocalDate today = LocalDate.now();               // 运行当天
        LocalDate birthday = LocalDate.of(2000, 6, 15);
        System.out.println("今天: " + today + "，生日: " + birthday);

        // Period 算日期差；plusDays 等运算返回新对象，原对象不变
        Period age = Period.between(birthday, today);
        LocalDate nextWeek = today.plusDays(7);
        System.out.println("年龄: " + age.getYears() + " 岁零 " + age.getMonths() + " 个月"
                + "；today 加 7 天 = " + nextWeek + "，today 仍是 " + today);

        // 格式化与解析：DateTimeFormatter 线程安全（老 API 的 SimpleDateFormat 不是）。
        // 注意模式只到分钟——格式化丢掉了秒，解析回来的对象和 now 不相等，截断到分钟才相等
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
        LocalDateTime now = LocalDateTime.now();
        String text = now.format(fmt);
        LocalDateTime parsed = LocalDateTime.parse(text, fmt);
        System.out.println("格式化: " + text + "；解析回来 == now ? " + now.equals(parsed)
                + "，截断到分钟后 == ? " + now.truncatedTo(java.time.temporal.ChronoUnit.MINUTES).equals(parsed));

        // Duration 算时间量（毫秒/秒），常用于计时
        Duration d = Duration.between(now, now.plusHours(2).plusMinutes(30));
        System.out.println("2 小时 30 分后相差: " + d.toMinutes() + " 分钟");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：BigDecimal 精确计算
    // ------------------------------------------------------------------
    static void demoBigDecimal() {
        System.out.println("== 第 4 段：BigDecimal ==");

        // 双精度构造把 double 的误差原样带进来；字符串构造才是精确的 0.1
        BigDecimal fromDouble = new BigDecimal(0.1);
        BigDecimal fromString = new BigDecimal("0.1");
        System.out.println("new BigDecimal(0.1)     = " + fromDouble);
        System.out.println("new BigDecimal(\"0.1\")  = " + fromString);

        // 加减乘没有歧义；除法除不尽时必须指定位数和舍入模式，否则抛异常
        BigDecimal price = new BigDecimal("19.99");
        BigDecimal qty = new BigDecimal("3");
        System.out.println("19.99 × 3 = " + price.multiply(qty));
        try {
            BigDecimal r = new BigDecimal("1").divide(new BigDecimal("3")); // 1/3 除不尽
        } catch (ArithmeticException e) {
            System.out.println("1/3 不指定舍入抛出: ArithmeticException（Non-terminating decimal expansion）");
        }
        System.out.println("1/3 保留 4 位 = " + new BigDecimal("1")
                .divide(new BigDecimal("3"), 4, RoundingMode.HALF_UP));

        // equals 连精度一起比，compareTo 才是比数值——经典陷阱
        BigDecimal x = new BigDecimal("1.0");
        BigDecimal y = new BigDecimal("1.00");
        System.out.println("1.0 equals 1.00 ? " + x.equals(y) + "；compareTo == 0 ? " + (x.compareTo(y) == 0));
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 5 段：System / Math / 格式化
    // ------------------------------------------------------------------
    static void demoSystemAndMath() {
        System.out.println("== 第 5 段：System 与 Math ==");

        // 计时标准姿势：nanoTime 单调递增（currentTimeMillis 受系统时钟调整影响）
        long t1 = System.nanoTime();
        int sum = 0;
        for (int i = 0; i < 1_000_000; i++) { sum += i; }
        long t2 = System.nanoTime();
        System.out.println("百万次累加耗时: " + (t2 - t1) / 1_000 + " 微秒（sum=" + sum + "）");

        // Math 常用：注意 round 是"四舍五入到最近整数"、-2.5 会舍到 -2（向正无穷方向）
        System.out.println("Math.round(2.5)=" + Math.round(2.5) + ", round(-2.5)=" + Math.round(-2.5)
                + ", floor(3.9)=" + (int) Math.floor(3.9) + ", ceil(3.1)=" + (int) Math.ceil(3.1)
                + ", sqrt(2)=" + Math.sqrt(2));

        // String.format / printf：%.2f 保留两位小数（四舍五入）、%d 整数、%s 字符串
        System.out.println(String.format("单价 %.2f 元 × %d 件 = %.2f 元", 19.999, 3, 19.999 * 3));
        System.out.println();
    }
}

// ------------------------------------------------------------------
// 演示用学生类：演示 equals/hashCode 的成对重写
// ------------------------------------------------------------------
class Student {

    private String name;
    private int age;

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    /** equals 五约定：自反/对称/传递/一致/对 null 返回 false——IDE 生成的模板就满足。 */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                    // 同一对象直接相等（快捷路径）
        if (!(o instanceof Student)) return false;     // 类型不符（含 null）直接不等
        Student other = (Student) o;
        return age == other.age && Objects.equals(name, other.name); // 字段逐个比较
    }

    /** hashCode 必须与 equals 一致：equals 相等 → hashCode 必须相同。 */
    @Override
    public int hashCode() {
        return Objects.hash(name, age);                // 让工具类替你算，别手写容易出错的 31 累乘
    }

    /** 打印友好：日志里看到"小明(18)"而不是"Student@1b6d3586"。 */
    @Override
    public String toString() {
        return name + "(" + age + ")";
    }
}
