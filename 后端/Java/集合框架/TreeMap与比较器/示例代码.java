// TreeMap 与比较器学习示例（Comparable / Comparator）
// 运行方式：java 示例代码.java （需 JDK 11+，record 语法需 JDK 16+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：TreeMap——自动排序的 Map 与导航方法
//   第 2 段：Comparable——类自己的【自然排序】
//   第 3 段：Comparator——外置的【定制排序】
//   第 4 段：范围视图 headMap/tailMap/subMap
//   第 5 段：没有比较器的代价——ClassCastException

import java.util.Comparator;
import java.util.TreeMap;

class TreeMapDemo {

    public static void main(String[] args) {
        demoNaturalOrder();
        demoComparable();
        demoComparator();
        demoRangeView();
        demoNoComparator();
    }

    // ------------------------------------------------------------------
    // 第 1 段：自然排序与导航方法
    // ------------------------------------------------------------------
    static void demoNaturalOrder() {
        System.out.println("== 第 1 段：自然排序与导航 ==");

        // 乱序放入，遍历自动按键升序——TreeMap 内部是红黑树，put 时就站好了位
        TreeMap<Integer, String> tm = new TreeMap<>();
        tm.put(88, "张三");
        tm.put(72, "李四");
        tm.put(95, "王五");
        tm.put(60, "赵六");
        System.out.println("  遍历（自动升序）: " + tm);

        // 导航方法：TreeMap 独有的【按大小提问】能力
        System.out.println("  最小/最大键: " + tm.firstKey() + " / " + tm.lastKey());
        System.out.println("  ≥80 的最小键 (ceilingKey 80): " + tm.ceilingKey(80));
        System.out.println("  <88 的最大键 (floorKey 88):   " + tm.floorKey(88));
        System.out.println("  严格大于 72 的最小键:          " + tm.higherKey(72));
        System.out.println("HashMap 做不到这些——它只认 hash 不认大小");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：Comparable——类自带自然排序
    // ------------------------------------------------------------------
    /** 学生按分数排；record 不自动实现 Comparable，要自己声明。 */
    record Student(String name, int score) implements Comparable<Student> {
        @Override
        public int compareTo(Student o) {
            return Integer.compare(score, o.score);   // 负=我排前面；0=相等；正=我排后面
        }
        @Override public String toString() { return name + ":" + score; }
    }

    static void demoComparable() {
        System.out.println("== 第 2 段：Comparable ==");

        TreeMap<Student, String> map = new TreeMap<>();   // 不传比较器 → 用 Student 自己的 compareTo
        map.put(new Student("张三", 88), "一班");
        map.put(new Student("王五", 95), "二班");
        map.put(new Student("李四", 72), "一班");
        System.out.println("  按分数升序: " + map.keySet());
        System.out.println("  compareTo 写在类内部 = 这个类型【天生】的排序规则（自然排序）");
        System.out.println("  Integer/String 都实现了它，所以第 1 段不用传比较器也能排");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：Comparator——排序规则外置，随时可换
    // ------------------------------------------------------------------
    static void demoComparator() {
        System.out.println("== 第 3 段：Comparator ==");

        // 同一个 Student 类，不改一行代码，换一种排法：分数降序
        TreeMap<Student, String> desc = new TreeMap<>(Comparator.reverseOrder());
        desc.put(new Student("张三", 88), "一班");
        desc.put(new Student("王五", 95), "二班");
        desc.put(new Student("李四", 72), "一班");
        System.out.println("  分数降序: " + desc.keySet());

        // 还能按完全无关的字段排：按姓名排（String 自身可比较）
        TreeMap<Student, String> byName = new TreeMap<>(Comparator.comparing(Student::name));
        byName.put(new Student("张三", 88), "一班");
        byName.put(new Student("王五", 95), "二班");
        byName.put(new Student("李四", 72), "一班");
        System.out.println("  按姓名排:  " + byName.keySet());
        System.out.println("  Comparable vs Comparator：一个钉死在类里，一个随用随传、可组合可反转");
        System.out.println("  经验：类有【唯一公认】顺序才实现 Comparable（如时间、编号）；");
        System.out.println("        业务排序（按价格/按销量/按评分…）一律用 Comparator");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：范围视图——TreeMap 的杀手锏
    // ------------------------------------------------------------------
    static void demoRangeView() {
        System.out.println("== 第 4 段：范围视图 ==");

        TreeMap<Integer, String> tm = new TreeMap<>();
        tm.put(60, "及格"); tm.put(72, "中"); tm.put(88, "良"); tm.put(95, "优");

        // subMap/headMap/tailMap 返回的是【视图】：不复制数据，随原 Map 联动
        System.out.println("  60~90 之间: " + tm.subMap(60, 90));        // 含头不含尾
        System.out.println("  <88 的:     " + tm.headMap(88));          // 键小于 88 的前缀
        System.out.println("  ≥88 的:     " + tm.tailMap(88));          // 键 ≥88 的后缀

        tm.put(80, "中上");                              // 视图联动：原 Map 改了，视图跟着变
        System.out.println("  原 Map 加了 80 后再看 60~90: " + tm.subMap(60, 90));
        System.out.println("  范围查询 O(log n + k)——排行榜取前 100、日志按时间段取，都是它");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 5 段：没比较器就 put——当场翻车
    // ------------------------------------------------------------------
    /** 普通类：既没实现 Comparable，构造 TreeMap 时也没传 Comparator。 */
    static class PlainKey {
        final String id;
        PlainKey(String id) { this.id = id; }
        @Override public String toString() { return id; }
    }

    static void demoNoComparator() {
        System.out.println("== 第 5 段：没有比较器 ==");

        TreeMap<PlainKey, String> tm = new TreeMap<>();  // 编译器不拦你
        try {
            tm.put(new PlainKey("k1"), "v1");            // put 要比较大小决定站哪 → 转型 Comparable 失败
            System.out.println("  不该执行到这里");
        } catch (ClassCastException e) {
            System.out.println("  put 当场抛 ClassCastException：" +
                    "红黑树靠比较定位，没有比较规则寸步难行");
        }
        System.out.println("  HashMap 没这个问题（它靠 hashCode 定位）——两类 Map 的根本分野");
        System.out.println("  顺带一提：TreeMap 的键也不允许 null（无法与 null 比较大小）");
        System.out.println();
    }
}
