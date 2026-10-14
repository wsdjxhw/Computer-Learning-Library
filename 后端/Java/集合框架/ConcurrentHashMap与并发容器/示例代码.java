// ConcurrentHashMap 与并发容器学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：为什么不用 synchronizedMap——全表锁 vs 分段锁
//   第 2 段：原子复合操作——putIfAbsent / computeIfAbsent / merge
//   第 3 段：ConcurrentHashMap 的 null 禁令
//   第 4 段：并发容器全家福与选型
//
// 计时段的毫秒数每次运行不同，但数量级差距稳定。

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

class ConcurrentCollectionsDemo {

    public static void main(String[] args) throws Exception {
        demoLockGranularity();
        demoAtomicOps();
        demoNullBan();
        demoFamily();
    }

    // ------------------------------------------------------------------
    // 第 1 段：synchronizedMap（一把大锁）vs ConcurrentHashMap（细粒度锁）
    // ------------------------------------------------------------------
    static void demoLockGranularity() throws Exception {
        System.out.println("== 第 1 段：锁粒度 ==");

        int threads = 4, perThread = 500_000;

        // 方案一：Collections.synchronizedMap——所有读写共用一把锁，同一时刻一个线程
        Map<Integer, Integer> syncMap =
                Collections.synchronizedMap(new HashMap<>());
        long t1 = System.nanoTime();
        runPuts(syncMap, threads, perThread);
        long t2 = System.nanoTime();

        // 方案二：ConcurrentHashMap——JDK8 起【桶级 CAS + synchronized】，不同桶并行
        Map<Integer, Integer> chm = new ConcurrentHashMap<>();
        long t3 = System.nanoTime();
        runPuts(chm, threads, perThread);
        long t4 = System.nanoTime();

        System.out.println("  " + threads + " 线程各写 " + perThread + " 条：");
        System.out.println("  synchronizedMap: " + (t2 - t1) / 1_000_000 + " ms（数值每次不同）");
        System.out.println("  ConcurrentHashMap: " + (t4 - t3) / 1_000_000 + " ms（数值每次不同）");
        System.out.println("  两者 size 相同: " + (syncMap.size() == chm.size()) + "（都线程安全，差在吞吐）");
        System.out.println("  追加一句：HashMap 直接并发写不仅丢数据，JDK7 还可能成环死循环——永不许裸奔");
        System.out.println();
    }

    /** 多线程并发往 map 写各不相同的键（键不冲突，纯比锁的开销）。 */
    static void runPuts(Map<Integer, Integer> map, int threads, int perThread)
            throws Exception {
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            final int base = t * perThread;
            new Thread(() -> {
                for (int i = 0; i < perThread; i++) map.put(base + i, i);
                done.countDown();
            }).start();
        }
        done.await();                              // 等全部线程写完
    }

    // ------------------------------------------------------------------
    // 第 2 段：原子复合操作——并发 Map 的真正杀手锏
    // ------------------------------------------------------------------
    static void demoAtomicOps() {
        System.out.println("== 第 2 段：原子复合操作 ==");

        // 病根：先查再放（check-then-act）在并发下是两步，中间能插进别人
        // ConcurrentHashMap 把【查+算+放】打包成一步：
        Map<String, Integer> counter = new ConcurrentHashMap<>();
        String[] words = {"apple", "banana", "apple", "cherry", "apple", "banana"};

        // merge：键存在就把旧值和新值按函数合并；不存在就直接放入
        for (String w : words) {
            counter.merge(w, 1, Integer::sum);     // 词频统计的经典一行
        }
        System.out.println("  merge 词频统计: " + counter);

        // computeIfAbsent：没有才计算放入（幂等初始化）——返回的一定是【已经存在】的值
        Map<String, List<Integer>> index = new ConcurrentHashMap<>();
        List<Integer> list = index.computeIfAbsent("apple", k -> new java.util.ArrayList<>());
        list.add(1);
        index.computeIfAbsent("apple", k -> new java.util.ArrayList<>()).add(2);  // 不会新建第二个列表
        System.out.println("  computeIfAbsent 复用同一个列表: " + index);

        // putIfAbsent：存在则不动、返回旧值；不存在则放入、返回 null
        Map<String, String> config = new ConcurrentHashMap<>();
        System.out.println("  首次 putIfAbsent 返回: " + config.putIfAbsent("mode", "prod"));
        System.out.println("  再次 putIfAbsent 返回: " + config.putIfAbsent("mode", "dev")
                + "（旧值 prod，且没被覆盖）");
        System.out.println("  这些方法都是【原子的】——比先 get 再 put 的手写版又快又对");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：null 禁令——CHM 与 HashMap 的一条重要分界线
    // ------------------------------------------------------------------
    static void demoNullBan() {
        System.out.println("== 第 3 段：null 禁令 ==");

        // HashMap 允许 null 键 null 值（null 键有专门的位置）
        Map<String, String> hm = new HashMap<>();
        hm.put("备注", null);
        System.out.println("  HashMap.put(值=null) 没问题: " + hm);

        // ConcurrentHashMap 两样都禁止
        Map<String, String> chm = new ConcurrentHashMap<>();
        try {
            chm.put("备注", null);
        } catch (NullPointerException e) {
            System.out.println("  CHM.put(值=null) → NullPointerException");
        }
        try {
            chm.put(null, "x");
        } catch (NullPointerException e) {
            System.out.println("  CHM.put(键=null) → NullPointerException");
        }
        System.out.println("  为什么：get 返回 null 无法区分【不存在】还是【值就是 null】——");
        System.out.println("  单线程 HashMap 靠 containsKey 补查能分清；并发下两次调用之间");
        System.out.println("  状态可能被别人改掉，歧义无法消除 → 干脆禁止，逼你用无歧义的表达");
        System.out.println("  记法：并发容器（CHM/ArrayDeque/BlockingQueue）普遍禁 null；HashMap 允许");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：并发容器全家福
    // ------------------------------------------------------------------
    static void demoFamily() {
        System.out.println("== 第 4 段：全家福 ==");

        CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<>();
        cow.add("a"); cow.add("b");
        System.out.println("  CopyOnWriteArrayList: 读无锁、写复制整个数组 → 读多写少的监听器列表");
        System.out.println("    （迭代时改集合不抛异常——fail-safe 的细节见《迭代器与fail-fast》第 5 段）");

        System.out.println("  选型速查：");
        System.out.println("    Map    → ConcurrentHashMap（几乎唯一正解）");
        System.out.println("    List   → 读多写少 CopyOnWriteArrayList；否则想想是不是该用别的结构");
        System.out.println("    Set    → ConcurrentSkipListSet（有序）/ CopyOnWriteArraySet（读多写少）");
        System.out.println("    队列   → BlockingQueue 家族（生产者消费者，见《并发工具类》第 4 段）");
        System.out.println("    有序Map → ConcurrentSkipListMap（跳表实现，支持范围查询，对应并发的 TreeMap）");
        System.out.println("  注意：并发容器保证的是【每个操作】线程安全；");
        System.out.println("        多个操作的组合（先 get 再 put）仍需原子方法或外部锁");
        System.out.println();
    }
}
