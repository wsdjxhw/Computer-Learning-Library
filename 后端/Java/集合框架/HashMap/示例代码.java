// HashMap 学习示例
// 运行方式：java 示例代码.java （UTF-8 终端可加 -Dstdout.encoding=UTF-8）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：基本操作与"同键覆盖"
//   第 2 段：三种遍历方式
//   第 3 段：getOrDefault / putIfAbsent / merge 与词频统计实战
//   第 4 段：null 键、顺序问题与选型
//
// 底层原理（哈希函数、冲突链、负载因子扩容）已在
// 数据结构与算法/数据结构/哈希表/ 完整讲解，本篇聚焦 Java 用法，两边互链不重复。

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

class HashMapDemo {

    public static void main(String[] args) {
        demoBasic();
        demoIteration();
        demoUtilityMethods();
        demoOrderAndNull();
    }

    // ------------------------------------------------------------------
    // 第 1 段：基本操作
    // ------------------------------------------------------------------
    static void demoBasic() {
        System.out.println("== 第 1 段：基本操作 ==");

        // 两个泛型参数：键类型、值类型。键一般用 String/Integer 等不可变类型
        Map<String, Integer> scores = new HashMap<>();

        scores.put("Alice", 90);                   // 插入/更新，平均 O(1)
        scores.put("Bob", 75);
        scores.put("Carol", 85);

        System.out.println("map = " + scores);
        System.out.println("get(\"Alice\") = " + scores.get("Alice"));
        System.out.println("get(\"Dave\") = " + scores.get("Dave") + "  ← 不存在返回 null，不抛异常");
        System.out.println("containsKey(\"Bob\") = " + scores.containsKey("Bob"));
        System.out.println("size() = " + scores.size());

        // 同键再 put 是"覆盖"，并且 put 会返回被覆盖的旧值 —— 很适合用来观察覆盖行为
        Integer oldValue = scores.put("Alice", 95);
        System.out.println("put(\"Alice\", 95) 返回旧值: " + oldValue + "，现在 get = " + scores.get("Alice"));

        scores.remove("Bob");                      // 按键删除，同样返回被删的值
        System.out.println("remove(\"Bob\") 后 size = " + scores.size());
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：遍历
    // ------------------------------------------------------------------
    static void demoIteration() {
        System.out.println("== 第 2 段：遍历 ==");
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 90);
        scores.put("Bob", 75);
        scores.put("Carol", 85);

        // 方式一（最常用）：entrySet 遍历"键值对"，一次拿到键和值，不用再查一次 map
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            System.out.println("  entrySet: " + entry.getKey() + " = " + entry.getValue());
        }

        // 方式二：keySet 只拿键，需要值时再 get（多一次查找，只在不需要值时用它）
        for (String name : scores.keySet()) {
            System.out.println("  keySet: " + name);
        }

        // 方式三：values 只拿值
        int total = 0;
        for (int score : scores.values()) {
            total += score;
        }
        System.out.println("  values 求和: " + total);
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：实用方法与词频统计
    // ------------------------------------------------------------------
    static void demoUtilityMethods() {
        System.out.println("== 第 3 段：实用方法与词频统计 ==");

        Map<String, Integer> stock = new HashMap<>();
        stock.put("苹果", 10);

        // getOrDefault：不存在时返回默认值，避免先 containsKey 再 get 的两步写法
        System.out.println("  苹果库存: " + stock.getOrDefault("苹果", 0)
                + "，橙子库存: " + stock.getOrDefault("橙子", 0));

        // putIfAbsent：键不存在才放入 —— 实现"没有就初始化"
        stock.putIfAbsent("橙子", 0);
        System.out.println("  putIfAbsent(橙子,0) 后: " + stock);

        // 实战：统计每个单词出现次数（词频统计是 HashMap 最经典的应用）
        String[] words = {"apple", "banana", "apple", "cherry", "banana", "apple"};
        Map<String, Integer> count = new HashMap<>();
        for (String word : words) {
            // merge：键不存在 → 放入 1；存在 → 用旧值算出新值放回（这里 +1）
            count.merge(word, 1, Integer::sum);
        }
        System.out.println("  词频统计: " + count);
        // 若不用 merge，等价的传统写法是 if/get/put 三行 —— merge 一行搞定且只查一次 map
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：null 键与顺序
    // ------------------------------------------------------------------
    static void demoOrderAndNull() {
        System.out.println("== 第 4 段：null 键与顺序 ==");

        // HashMap 允许至多一个 null 键（hashCode 为 0 单独处理），允许多个 null 值
        Map<String, Integer> m = new HashMap<>();
        m.put(null, 0);
        System.out.println("  null 键的值: " + m.get(null));

        // HashMap 不保证任何顺序：遍历顺序既不是插入序也不是排序序，还可能随扩容变化
        Map<String, Integer> inserted = new HashMap<>();
        inserted.put("first", 1);
        inserted.put("second", 2);
        inserted.put("third", 3);
        System.out.println("  HashMap 遍历序: " + inserted + "  ← 与插入顺序无关");

        // 需要顺序时的两个替代品：
        Map<String, Integer> linked = new LinkedHashMap<>();   // 保持插入顺序
        linked.put("first", 1);
        linked.put("second", 2);
        linked.put("third", 3);
        System.out.println("  LinkedHashMap: " + linked);

        Map<String, Integer> tree = new TreeMap<>();           // 按键排序
        tree.put("banana", 2);
        tree.put("apple", 1);
        tree.put("cherry", 3);
        System.out.println("  TreeMap: " + tree + "  ← 键有序");

        // 选型口诀：要快用 HashMap，要插入序用 LinkedHashMap，要按键排序用 TreeMap
        System.out.println();
    }
}
