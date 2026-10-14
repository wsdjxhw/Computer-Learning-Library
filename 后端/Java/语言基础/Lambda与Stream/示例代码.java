// Lambda 与 Stream 学习示例
// 运行方式：java 示例代码.java （需 JDK 16+（用到 record），本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：函数式接口与 Lambda
//   第 2 段：方法引用的四种形式
//   第 3 段：Stream 中间操作（惰性求值）
//   第 4 段：终端操作与 collect 分组统计
//   第 5 段：Optional

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class LambdaStreamDemo {

    public static void main(String[] args) {
        demoFunctional();
        demoMethodRef();
        demoIntermediate();
        demoTerminal();
        demoOptional();
    }

    // ------------------------------------------------------------------
    // 第 1 段：函数式接口与 Lambda
    // ------------------------------------------------------------------
    /** 函数式接口：有且只有一个抽象方法（@FunctionalInterface 让编译器帮你把关）。 */
    @FunctionalInterface
    interface Calc {
        int op(int a, int b);
    }

    static void demoFunctional() {
        System.out.println("== 第 1 段：函数式接口与 Lambda ==");

        // Lambda 就是"匿名方法"：(参数) -> 表达式/语句块
        // 它能赋给 Calc 这种"单抽象方法接口"——类型对得上就行，不用写匿名类那一坨
        Calc add = (a, b) -> a + b;
        Calc max = (a, b) -> Math.max(a, b);   // 多语句时用 { ...; return x; }
        System.out.println("add.op(3, 5) = " + add.op(3, 5) + "，max.op(3, 5) = " + max.op(3, 5));

        // 对照：匿名内部类的老写法——Lambda 之前只能这样把"行为"当参数传
        Comparator<String> oldStyle = new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return Integer.compare(s1.length(), s2.length());
            }
        };
        Comparator<String> newStyle = (s1, s2) -> Integer.compare(s1.length(), s2.length());
        List<String> words = new ArrayList<>(List.of("banana", "fig", "apple"));
        words.sort(newStyle);
        System.out.println("按长度排序: " + words + "（两种写法效果相同，老写法要 5 行）");

        // JDK 自带大批函数式接口：Runnable、Comparator、Function、Predicate、Consumer……
        Runnable task = () -> System.out.println("Runnable 也是函数式接口，lambda 直接给它");
        task.run();
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：方法引用的四种形式
    // ------------------------------------------------------------------
    static void demoMethodRef() {
        System.out.println("== 第 2 段：方法引用 ==");

        List<String> names = List.of("charlie", "alice", "bob");

        // 1. 类名::静态方法 —— s -> Integer.parseInt(s) 的缩写
        List<Integer> lens = names.stream().map(String::length)   // 2. 类名::实例方法（参数当接收者）
                .collect(Collectors.toList());
        System.out.println("String::length 取长度: " + lens);

        // 3. 对象::方法 —— 借用某个具体对象的方法
        String prefix = "员工: ";
        List<String> labeled = names.stream().map(prefix::concat).collect(Collectors.toList());
        System.out.println("prefix::concat: " + labeled);

        // 4. 类名::new —— 构造器引用，造对象也一行
        names.stream().map(StringBuilder::new)                    // s -> new StringBuilder(s)
                .forEach(sb -> System.out.println("  构造器引用造出: " + sb));

        // 判断标准：lambda 体只是"转发调用一个已有方法"时，都能改成方法引用
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：中间操作——惰性求值
    // ------------------------------------------------------------------
    // record 是 Java 16+ 的紧凑类写法：自动生成构造器、getter、equals/hashCode/toString
    record Employee(String name, String dept, int salary) { }

    static void demoIntermediate() {
        System.out.println("== 第 3 段：中间操作（惰性） ==");

        List<Employee> staff = List.of(
                new Employee("Alice", "技术", 20000),
                new Employee("Bob", "销售", 12000),
                new Employee("Carol", "技术", 25000),
                new Employee("Dave", "人事", 10000),
                new Employee("Eve", "销售", 15000));

        // 中间操作(filter/map/sorted...)只是"登记流水线"，不触发执行——
        // 没有终端操作，下面这行的 peek 什么都不会打印
        staff.stream()
                .peek(e -> System.out.println("  peek 看不到我: " + e.name()))  // 不会执行！
                .filter(e -> e.salary() >= 12000);

        // 接上终端操作，整条流水线才开闸
        List<String> highPaid = staff.stream()
                .filter(e -> e.salary() >= 12000)          // 过滤
                .map(Employee::name)                       // 变换：Employee → 姓名
                .sorted()                                  // 排序
                .limit(3)                                  // 截断
                .collect(Collectors.toList());             // 终端：开闸执行
        System.out.println("月薪 ≥12000 的前三人: " + highPaid);
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：终端操作与 collect 分组统计
    // ------------------------------------------------------------------
    static void demoTerminal() {
        System.out.println("== 第 4 段：终端与 collect ==");

        List<Employee> staff = List.of(
                new Employee("Alice", "技术", 20000),
                new Employee("Bob", "销售", 12000),
                new Employee("Carol", "技术", 25000),
                new Employee("Eve", "销售", 15000));

        // count / forEach 是终端操作
        long count = staff.stream().filter(e -> e.salary() > 10000).count();
        System.out.println("月薪过万人数: " + count);

        // groupingBy：按部门分组——替代手写 Map + 循环判空的传统三行
        Map<String, List<Employee>> byDept = staff.stream()
                .collect(Collectors.groupingBy(Employee::dept));
        System.out.println("按部门分组: " + byDept.keySet() + " → 技术部 " + byDept.get("技术").size() + " 人");

        // 下游收集器：分组后再统计/求平均，一行完成"各部门平均薪资"
        Map<String, Double> avgByDept = staff.stream()
                .collect(Collectors.groupingBy(Employee::dept, Collectors.averagingInt(Employee::salary)));
        System.out.println("各部门平均薪资: " + avgByDept);

        // joining：拼接字符串；reduce：把流折叠成一个值（这里求总薪资）
        String nameList = staff.stream().map(Employee::name).collect(Collectors.joining(", ", "[", "]"));
        int total = staff.stream().map(Employee::salary).reduce(0, Integer::sum);
        System.out.println("名单: " + nameList + "，总薪资: " + total);
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 5 段：Optional
    // ------------------------------------------------------------------
    static void demoOptional() {
        System.out.println("== 第 5 段：Optional ==");

        // Optional<T> 是"可能没有值"的显式容器——用类型系统逼调用方处理空的情况
        Optional<Employee> found = findByName("Alice");
        Optional<Employee> missing = findByName("Zack");

        // 链式处理：map 变换 + orElse 给默认值，全程无 if-null
        String name = found.map(Employee::name).orElse("查无此人");
        String fallback = missing.map(Employee::name).orElse("查无此人");
        System.out.println("Alice → " + name + "；Zack → " + fallback);

        // orElseGet 惰性求值：默认值很贵（查库/新建大对象）时用它，避免白算
        Employee e = missing.orElseGet(() -> new Employee("默认", "无", 0));
        System.out.println("orElseGet 兜底: " + e);

        // orElseThrow：明确"没有就该报错"的场景，异常信息比 NPE 有意义
        try {
            Employee must = findByName("Zack").orElseThrow(() -> new IllegalStateException("员工不存在: Zack"));
        } catch (IllegalStateException ex) {
            System.out.println("orElseThrow 抛出: " + ex.getMessage());
        }
        System.out.println();
    }

    /** 模拟一个可能查不到的查询：返回 Optional 而不是 null——方法签名就表达了"可能没有"。 */
    static Optional<Employee> findByName(String name) {
        List<Employee> staff = List.of(new Employee("Alice", "技术", 20000));
        return staff.stream().filter(e -> e.name().equals(name)).findFirst();
    }
}
