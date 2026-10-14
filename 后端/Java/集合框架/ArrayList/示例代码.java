// ArrayList 学习示例
// 运行方式：java 示例代码.java （UTF-8 终端可加 -Dstdout.encoding=UTF-8）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：创建与基本操作
//   第 2 段：remove 的重载陷阱
//   第 3 段：三种遍历方式
//   第 4 段：排序
//   第 5 段：不可变列表与定长列表
//
// 底层原理（动态数组扩容、O(1) 随机访问、O(n) 中间插入删除）
// 已在 数据结构与算法/数据结构/数组/ 完整讲解，本篇聚焦 Java 用法，两边互链不重复。

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class ArrayListDemo {

    public static void main(String[] args) {
        demoBasic();
        demoRemoveTrap();
        demoIteration();
        demoSort();
        demoImmutable();
    }

    // ------------------------------------------------------------------
    // 第 1 段：创建与基本操作
    // ------------------------------------------------------------------
    static void demoBasic() {
        System.out.println("== 第 1 段：基本操作 ==");

        // 泛型指定元素类型；引用一边可省略 <> 里的类型（钻石操作符）
        List<String> fruits = new ArrayList<>();

        fruits.add("苹果");                        // 尾部追加，平均 O(1)（偶尔触发扩容）
        fruits.add("香蕉");
        fruits.add("樱桃");
        fruits.add(1, "葡萄");                     // 指定下标插入，后面元素整体后移 O(n)

        System.out.println("列表: " + fruits + "，长度 size() = " + fruits.size());
        System.out.println("get(0) = " + fruits.get(0) + "  ← 按下标访问 O(1)，底层就是数组");
        System.out.println("indexOf(\"樱桃\") = " + fruits.indexOf("樱桃") + "  ← 按值查找 O(n)");
        System.out.println("contains(\"香蕉\") = " + fruits.contains("香蕉"));

        fruits.set(0, "西瓜");                     // 替换指定下标的元素
        System.out.println("set(0, \"西瓜\") 后: " + fruits);
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：remove 重载陷阱
    // ------------------------------------------------------------------
    static void demoRemoveTrap() {
        System.out.println("== 第 2 段：remove 陷阱 ==");

        List<Integer> nums = new ArrayList<>(List.of(10, 20, 30));

        // remove 有两个重载：remove(int index) 按 下标 删；remove(Object o) 按 值 删
        nums.remove(1);                            // 传 int 字面量 → 匹配 remove(int)：删下标 1
        System.out.println("remove(1) 删的是下标 1 → " + nums);

        nums = new ArrayList<>(List.of(10, 20, 30));
        nums.remove(Integer.valueOf(1));           // 传 Integer 对象 → 匹配 remove(Object)：删值为 1 的元素
        System.out.println("remove(Integer.valueOf(1)) → " + nums + "（没有值为 1 的元素，列表不变）");

        // List.of(10,20,30) 里没有 1，所以第二个什么都没删 —— 正好说明两者语义完全不同。
        // 高频 bug：想按值删 Integer 元素却写 list.remove(1)，删掉的是下标 1。
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：遍历
    // ------------------------------------------------------------------
    static void demoIteration() {
        System.out.println("== 第 3 段：遍历 ==");
        List<String> fruits = new ArrayList<>(List.of("苹果", "香蕉", "樱桃"));

        // 方式一：传统 for + 下标。需要下标信息（比如要改写元素）时用它
        for (int i = 0; i < fruits.size(); i++) {
            System.out.println("  for-i  [" + i + "] " + fruits.get(i));
        }

        // 方式二：增强 for（for-each）。只读遍历的首选，代码最简
        for (String fruit : fruits) {
            System.out.println("  for-each " + fruit);
        }

        // 遍历时删除的陷阱：for-each 中直接 remove 会抛 ConcurrentModificationException
        // （迭代器发现列表被外部改动）。安全做法：removeIf（内部用迭代器删除）
        fruits.removeIf(f -> f.equals("香蕉"));
        System.out.println("  removeIf 删除香蕉后: " + fruits);
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：排序
    // ------------------------------------------------------------------
    static void demoSort() {
        System.out.println("== 第 4 段：排序 ==");

        List<Integer> nums = new ArrayList<>(Arrays.asList(42, 7, 19, 3));
        nums.sort(Comparator.naturalOrder());      // 升序
        System.out.println("升序: " + nums);
        nums.sort(Comparator.reverseOrder());      // 降序
        System.out.println("降序: " + nums);

        // 对象列表按某个字段排序：Comparator 组合出"先按长度，再按字典序"
        List<String> words = new ArrayList<>(List.of("banana", "fig", "apple"));
        words.sort(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
        System.out.println("先按长度再按字典序: " + words);
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 5 段：不可变列表与定长列表
    // ------------------------------------------------------------------
    static void demoImmutable() {
        System.out.println("== 第 5 段：三种创建方式的区别 ==");

        // ArrayList：完全可变，最常用
        List<String> mutable = new ArrayList<>();
        mutable.add("可以增删改");

        // List.of（Java 9+）：不可变列表，增删改都抛 UnsupportedOperationException
        List<String> immutable = List.of("固定", "内容");
        try {
            immutable.add("试试添加");
        } catch (UnsupportedOperationException e) {
            System.out.println("  List.of 添加元素: UnsupportedOperationException（不可变）");
        }

        // Arrays.asList：定长视图 —— 能改元素（set），不能增删（还是引用原数组的坑）
        List<String> fixed = Arrays.asList("a", "b");
        try {
            fixed.add("c");
        } catch (UnsupportedOperationException e) {
            System.out.println("  Arrays.asList 添加元素: UnsupportedOperationException（定长）");
        }
        fixed.set(0, "A");                          // set 允许
        System.out.println("  Arrays.asList set(0,\"A\") 后: " + fixed);
        System.out.println();
    }
}
