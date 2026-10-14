// HashSet 与 LinkedHashSet 学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：Set 的价值——O(1) 成员判断
//   第 2 段：去重的本质是 equals + hashCode
//   第 3 段：LinkedHashSet——保插入顺序的 Set
//   第 4 段：三家 Set 选型

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class HashSetDemo {

    public static void main(String[] args) {
        demoWhySet();
        demoEqualsHashCode();
        demoLinkedHashSet();
        demoChoose();
    }

    // ------------------------------------------------------------------
    // 第 1 段：Set 的价值——判断【在不在】比 List 快得多
    // ------------------------------------------------------------------
    static void demoWhySet() {
        System.out.println("== 第 1 段：Set 的价值 ==");

        // 同样 10 万个元素装进 List 和 Set
        List<Integer> list = new ArrayList<>();
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            list.add(i);
            set.add(i);
        }

        // 各查 100 次不存在的元素：List 是从头扫到尾 O(n)，Set 是定位桶 O(1)
        long t1 = System.nanoTime();
        for (int i = 0; i < 100; i++) list.contains(999_999);
        long t2 = System.nanoTime();
        for (int i = 0; i < 100; i++) set.contains(999_999);
        long t3 = System.nanoTime();
        // 用微秒打印：Set 的百次查询常常不足 1 毫秒，按毫秒会显示成 0
        System.out.println("  List.contains 100 次耗时: " + (t2 - t1) / 1_000 + " 微秒（数值每次不同）");
        System.out.println("  Set.contains  100 次耗时: " + (t3 - t2) / 1_000 + " 微秒（数量级差距稳定）");

        // add 重复元素自动被忽略：返回 false，不报错
        Set<String> tags = new HashSet<>();
        System.out.println("  第一次 add(热门) = " + tags.add("热门"));
        System.out.println("  第二次 add(热门) = " + tags.add("热门") + "（重复直接拒收）");
        System.out.println("  去重后的元素数 = " + tags.size());
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：去重的本质——equals 说相等才算重复，hashCode 决定去哪个桶找
    // ------------------------------------------------------------------
    /** 没重写 equals/hashCode：用的是 Object 默认的【内存地址】比较。 */
    static class BadKey {
        final String name;
        BadKey(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    /** 重写了 equals/hashCode：按【内容】判等——Set 这才能认出两个对象是同一个。 */
    static class GoodKey {
        final String name;
        GoodKey(String name) { this.name = name; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GoodKey)) return false;
            return name.equals(((GoodKey) o).name);      // 相等的依据：name 相同
        }
        @Override public int hashCode() { return name.hashCode(); }   // 必须与 equals 同一依据
        @Override public String toString() { return name; }
    }

    static void demoEqualsHashCode() {
        System.out.println("== 第 2 段：去重的本质 ==");

        // 不重写：两个内容相同的对象被当成两个人 → 去重失效
        Set<BadKey> badSet = new HashSet<>();
        badSet.add(new BadKey("张三"));
        badSet.add(new BadKey("张三"));                   // 内容一样，但地址不同
        System.out.println("  不重写 equals: 两个张三都进了集合，size = " + badSet.size() + "（去重失效）");

        // 重写：内容相同即同一个人 → 正确去重
        Set<GoodKey> goodSet = new HashSet<>();
        goodSet.add(new GoodKey("张三"));
        goodSet.add(new GoodKey("张三"));
        System.out.println("  重写 equals+hashCode: size = " + goodSet.size() + "（只认一个张三）");

        // 更隐蔽的坑：contains 也靠同一套规则——不重写就连【查】都查不到
        System.out.println("  badSet.contains(new 张三) = "
                + badSet.contains(new BadKey("张三")) + "（查不到，因为equals按地址比）");
        System.out.println("  goodSet.contains(new 张三) = " + goodSet.contains(new GoodKey("张三")));
        System.out.println("结论：放进 Set/Map 键的自定义类，equals 与 hashCode 必须【成对】重写");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：LinkedHashSet——多一条链表记住插入顺序
    // ------------------------------------------------------------------
    static void demoLinkedHashSet() {
        System.out.println("== 第 3 段：LinkedHashSet ==");

        Set<String> hash = new HashSet<>();
        Set<String> linked = new LinkedHashSet<>();
        for (String s : new String[]{"香蕉", "苹果", "橘子", "葡萄"}) {
            hash.add(s);
            linked.add(s);
        }
        System.out.println("  HashSet 遍历:      " + hash + "（顺序由 hash 决定，看着乱且不稳定）");
        System.out.println("  LinkedHashSet 遍历: " + linked + "（严格按插入顺序）");
        System.out.println("LinkedHashSet = HashSet + 双向链表：去重能力不变，多花一份链表内存换稳定顺序");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：Set 三兄弟选型
    // ------------------------------------------------------------------
    static void demoChoose() {
        System.out.println("== 第 4 段：选型 ==");

        // TreeSet 放在下一篇细讲，这里只看一眼：它保证【排序后】的顺序
        Set<Integer> tree = new java.util.TreeSet<>();
        for (int v : new int[]{42, 7, 19, 3}) tree.add(v);
        System.out.println("  TreeSet 遍历: " + tree + "（自动排序——代价见下一篇）");

        System.out.println("  选型口诀：");
        System.out.println("    只要去重、不在乎顺序        → HashSet（最快最省）");
        System.out.println("    去重 + 保持插入顺序          → LinkedHashSet");
        System.out.println("    去重 + 自动排序/范围查询     → TreeSet（红黑树，见下篇）");
        System.out.println("    去重本质回顾：HashSet 底层就是一个 HashMap（元素当 key，value 是占位对象）");
        System.out.println();
    }
}
