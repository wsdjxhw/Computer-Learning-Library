// 迭代器与 fail-fast 学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：迭代器三件套 hasNext/next/remove
//   第 2 段：fail-fast——边遍历边删除的爆炸现场
//   第 3 段：三种正确删除姿势
//   第 4 段：经典陷阱——删倒数第二个元素居然不抛异常
//   第 5 段：fail-safe——CopyOnWriteArrayList 的快照世界

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

class IteratorDemo {

    public static void main(String[] args) {
        demoIteratorBasics();
        demoFailFast();
        demoCorrectRemove();
        demoLastSecondTrap();
        demoFailSafe();
    }

    static List<String> fruits() {
        List<String> list = new ArrayList<>();
        list.add("苹果"); list.add("香蕉"); list.add("橘子"); list.add("葡萄");
        return list;
    }

    // ------------------------------------------------------------------
    // 第 1 段：迭代器——遍历的统一协议
    // ------------------------------------------------------------------
    static void demoIteratorBasics() {
        System.out.println("== 第 1 段：迭代器基础 ==");

        // for-each 就是编译器生成的迭代器写法（Iterable 的集合都能 for-each）
        List<String> list = fruits();
        for (String s : list) {                  // 糖衣之下：Iterator + hasNext + next
            System.out.print("  " + s);
        }
        System.out.println(" ← for-each 遍历");

        // 手写迭代器：能多做的事是【边遍历边安全删除】
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {                   // 还有下一个吗
            String s = it.next();                // 前进并取出
            if ("香蕉".equals(s)) {
                it.remove();                     // 删除【刚取出的】这个元素——唯一的合法删除方式
            }
        }
        System.out.println("  it.remove 删除香蕉后: " + list);

        // ListIterator 是 List 专属增强版：能倒着走、能改值、能知道下标
        ListIterator<String> li = list.listIterator(list.size());   // 光标放队尾
        System.out.print("  倒着走: ");
        while (li.hasPrevious()) {
            System.out.print(li.previous() + " ");
        }
        System.out.println();
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：fail-fast——爆炸现场
    // ------------------------------------------------------------------
    static void demoFailFast() {
        System.out.println("== 第 2 段：fail-fast ==");

        List<String> list = fruits();
        try {
            for (String s : list) {
                if ("香蕉".equals(s)) {
                    list.remove(s);              // 遍历途中用【集合自己的】remove → 踩雷
                }
            }
            System.out.println("  没抛异常？（本例大概率会抛，见第 4 段的例外）");
        } catch (ConcurrentModificationException e) {
            System.out.println("  ConcurrentModificationException！迭代器发现集合被【外人】动了");
        }
        System.out.println("  机制：ArrayList 里有个修改计数 modCount；迭代器创建时抄一份");
        System.out.println("  （expectedModCount）；每次 next/remove 都核对两份计数——不一致立刻掀桌");
        System.out.println("  注意：这是【尽力而为】的故障检测，不是保证——别依赖它做正确性");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：三种正确删除姿势
    // ------------------------------------------------------------------
    static void demoCorrectRemove() {
        System.out.println("== 第 3 段：正确姿势 ==");

        // 用中英混编的列表：两字中文被删、英文保留——看得到【部分删除】的效果
        // 姿势一：迭代器的 remove（同步更新两份计数，最经典）
        List<String> a = new ArrayList<>(List.of("苹果", "banana", "橘子", "grape"));
        for (Iterator<String> it = a.iterator(); it.hasNext(); ) {
            if (it.next().length() == 2) it.remove();   // 删除两字中文项
        }
        System.out.println("  ① 迭代器remove: " + a);

        // 姿势二：removeIf（JDK 8+，内部就是迭代器删除，最推荐）
        List<String> b = new ArrayList<>(List.of("苹果", "banana", "橘子", "grape"));
        b.removeIf(s -> s.length() == 2);
        System.out.println("  ② removeIf:      " + b);

        // 姿势三：下标倒序删（只有 for 循环可用；正序删会漏元素——删一个全体前移，i 却 +1 了）
        List<String> c = new ArrayList<>(List.of("苹果", "banana", "橘子", "grape"));
        for (int i = c.size() - 1; i >= 0; i--) {
            if (c.get(i).length() == 2) c.remove(i);
        }
        System.out.println("  ③ 倒序下标删:    " + c);
        System.out.println("  日常写代码：removeIf 一行搞定；面试：三种都要会说，还要会解释为何正序下标删会漏");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：经典陷阱——删倒数第二个元素，居然不抛异常
    // ------------------------------------------------------------------
    static void demoLastSecondTrap() {
        System.out.println("== 第 4 段：倒数第二陷阱 ==");

        // 场景：[苹果, 香蕉, 橘子]，条件命中【倒数第二个】香蕉
        List<String> list = fruits();
        list.remove("葡萄");                      // 先构造三元素场景（离线构造，不在遍历中删）
        try {
            for (String s : list) {              // 遍历 [苹果, 香蕉, 橘子]
                if ("香蕉".equals(s)) {
                    list.remove(s);              // 删掉了倒数第二个元素
                }
            }
            System.out.println("  竟然没抛异常！结果: " + list + "（香蕉确实被删了）");
        } catch (ConcurrentModificationException e) {
            System.out.println("  抛了异常（理论上不该走到这）: " + list);
        }

        // 为什么：删除后 size 变 2，恰好等于迭代器的光标位置 → hasNext 判断【没有下一个了】
        // → 循环自然结束 → next() 根本没被再次调用 → 计数核对没机会执行 → 异常没抛
        // 危险性：同样的代码，删别的元素当场爆炸，删倒数第二个却【静默通过】——
        // 也就是说 fail-fast 漏检是真实存在的，它只是尽力而为
        System.out.println("  原理：删除后 hasNext 提前返回 false，next 不再执行，核对被跳过");
        System.out.println("  教训：别赌 fail-fast 会救你——遍历中删除，永远用第 3 段的姿势");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 5 段：fail-safe——CopyOnWriteArrayList 的快照世界
    // ------------------------------------------------------------------
    static void demoFailSafe() {
        System.out.println("== 第 5 段：fail-safe ==");

        // CopyOnWriteArrayList：写时复制——每次增删都复制一个新数组，读/迭代走旧快照
        CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<>();
        cow.add("监听器A"); cow.add("监听器B");

        for (String s : cow) {                   // 迭代开始，拿到当前数组快照
            System.out.println("  遍历到: " + s);
            if ("监听器A".equals(s)) {
                cow.add("监听器C");              // 遍历中修改——不抛异常！
                System.out.println("    遍历中 add 了 监听器C（无异常）");
            }
        }
        System.out.println("  迭代结束再看集合: " + cow + "（C 在，但刚才的遍历看不见它）");
        System.out.println("  代价：每次写复制整个数组 → 写多读少就是灾难；适合读多写少的监听器列表");
        System.out.println("  对照：ConcurrentHashMap 的迭代器是【弱一致】——不抛异常，尽量反映最新状态");
        System.out.println();
    }
}
