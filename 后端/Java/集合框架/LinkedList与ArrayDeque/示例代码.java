// LinkedList 与 ArrayDeque 学习示例（List 的另一面：双端队列）
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：LinkedList 的正确打开方式——Deque（当队列和栈用）
//   第 2 段：头部插入对比——链表的主场
//   第 3 段：随机访问对比——链表的滑铁卢
//   第 4 段：ArrayDeque——官方推荐的双端队列
//
// 计时段的毫秒数每次运行不同，但数量级差距稳定。

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

class LinkedListDemo {

    public static void main(String[] args) {
        demoDequeApi();
        demoHeadInsert();
        demoRandomAccess();
        demoArrayDeque();
    }

    // ------------------------------------------------------------------
    // 第 1 段：LinkedList 实现了 Deque——队列与栈两副面孔
    // ------------------------------------------------------------------
    static void demoDequeApi() {
        System.out.println("== 第 1 段：Deque 的两副面孔 ==");

        // 面孔一：先进先出队列（FIFO）——offer 从队尾进，poll 从队头出
        Deque<String> queue = new LinkedList<>();
        queue.offer("第1位"); queue.offer("第2位"); queue.offer("第3位");
        System.out.println("  队列出队: " + queue.poll() + "，" + queue.poll() + "（先来的先走）");

        // 面孔二：后进先出栈（LIFO）——push 压栈顶，pop 弹栈顶（栈顶=队头）
        Deque<Integer> stack = new LinkedList<>();
        stack.push(1); stack.push(2); stack.push(3);
        System.out.println("  栈弹栈: " + stack.pop() + "，" + stack.pop() + "（后进来的先走）");

        // 两头操作都是 O(1)——这是链表结构的天赋
        Deque<String> both = new LinkedList<>();
        both.addFirst("头"); both.addLast("尾");
        System.out.println("  两头同时操作: " + both + "（addFirst/addLast 均 O(1)）");
        System.out.println("  注意：Stack 类早已不推荐用（继承 Vector、全方法同步），Deque 才是正解");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：头部插入——链表的主场
    // ------------------------------------------------------------------
    static void demoHeadInsert() {
        System.out.println("== 第 2 段：头部插入 ==");

        int n = 50_000;
        List<Integer> array = new ArrayList<>();
        List<Integer> linked = new LinkedList<>();

        long t1 = System.nanoTime();
        for (int i = 0; i < n; i++) array.add(0, i);       // ArrayList：每次头插要把现有元素整体后移
        long t2 = System.nanoTime();
        for (int i = 0; i < n; i++) linked.add(0, i);      // LinkedList：改两个节点的指针就完事
        long t3 = System.nanoTime();

        System.out.println("  头插 " + n + " 次：ArrayList " + (t2 - t1) / 1_000_000
                + " ms vs LinkedList " + (t3 - t2) / 1_000_000 + " ms（数值每次不同）");
        System.out.println("  ArrayList 头插是 O(n)（整体挪窝），LinkedList 是 O(1)（接个节点）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：随机访问——链表的滑铁卢
    // ------------------------------------------------------------------
    static void demoRandomAccess() {
        System.out.println("== 第 3 段：随机访问 ==");

        int n = 20_000;
        List<Integer> array = new ArrayList<>();
        List<Integer> linked = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            array.add(i);
            linked.add(i);
        }

        // 反复取中间元素：ArrayList 直接下标寻址；LinkedList 只能从一头数过去
        long t1 = System.nanoTime();
        for (int i = 0; i < 2_000; i++) array.get(n / 2);
        long t2 = System.nanoTime();
        for (int i = 0; i < 2_000; i++) linked.get(n / 2);
        long t3 = System.nanoTime();

        System.out.println("  取中间元素 2000 次：ArrayList " + (t2 - t1) / 1_000_000
                + " ms vs LinkedList " + (t3 - t2) / 1_000_000 + " ms（数值每次不同）");
        System.out.println("  ArrayList.get 是 O(1)，LinkedList.get 是 O(n)（两头找较近的一头数）");
        System.out.println("  所以 LinkedList 当【List】用几乎全面落败——它的价值只在队列/栈");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：ArrayDeque——比 LinkedList 更好的双端队列
    // ------------------------------------------------------------------
    static void demoArrayDeque() {
        System.out.println("== 第 4 段：ArrayDeque ==");

        // 同一套 Deque API（offer/poll/push/pop），底层是循环数组而非链表
        Deque<String> deque = new ArrayDeque<>();
        deque.offer("a"); deque.offer("b"); deque.offerFirst("first");
        System.out.println("  队列内容: " + deque + "（offerFirst 插到了队头）");
        System.out.println("  poll 出队: " + deque.poll() + "，peek 看一眼队头: " + deque.peek());

        // ArrayDeque 的限制：不允许 null 元素（null 是 poll 空队列的专用返回值，会混淆语义）
        try {
            deque.add(null);
        } catch (NullPointerException e) {
            System.out.println("  add(null) → NullPointerException（null 被留作【队列空了】的信号）");
        }

        System.out.println("  为什么官方推荐它当栈/队列：");
        System.out.println("    连续内存缓存友好、无节点对象开销 → 大多数场景比 LinkedList 快");
        System.out.println("  选型总结：随机访问 → ArrayList；队列/栈 → ArrayDeque；");
        System.out.println("            LinkedList 只在【又要下标又要频繁两头增删】的缝隙场景考虑");
        System.out.println();
    }
}
