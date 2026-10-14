// LinkedHashMap 与 LRU 缓存学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：LinkedHashMap——HashMap + 一条记住顺序的链
//   第 2 段：accessOrder——访问也能改变顺序
//   第 3 段：removeEldestEntry——10 行实现 LRU 缓存

import java.util.LinkedHashMap;
import java.util.Map;

class LinkedHashMapDemo {

    public static void main(String[] args) {
        demoInsertOrder();
        demoAccessOrder();
        demoLru();
    }

    // ------------------------------------------------------------------
    // 第 1 段：插入顺序版——LinkedHashMap 的默认形态
    // ------------------------------------------------------------------
    static void demoInsertOrder() {
        System.out.println("== 第 1 段：插入顺序 ==");

        // 构造参数（容量, 负载因子, accessOrder=false）→ 按【插入】顺序排链
        Map<String, Integer> cart = new LinkedHashMap<>(16, 0.75f, false);
        cart.put("苹果", 3);
        cart.put("牛奶", 1);
        cart.put("面包", 2);
        System.out.println("  遍历: " + cart + "（严格按放入顺序）");
        System.out.println("  本质：HashMap 的桶数组 + 一条贯穿所有条目的双向链表");
        System.out.println("  HashMap 遍历要扫空桶，LinkedHashMap 顺链走——大表遍历也更快");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：accessOrder=true——谁被访问（get/put）谁排到链尾
    // ------------------------------------------------------------------
    static void demoAccessOrder() {
        System.out.println("== 第 2 段：访问顺序 ==");

        Map<String, Integer> m = new LinkedHashMap<>(16, 0.75f, true);  // 关键：true
        m.put("A", 1); m.put("B", 2); m.put("C", 3);
        System.out.println("  初始: " + m.keySet() + "（A B C）");

        m.get("A");            // 访问 A → A 被挪到链尾（最近使用）
        System.out.println("  get(A) 后: " + m.keySet() + "（A 挪到了最后）");

        m.put("B", 20);        // put 已存在的键也算一次访问
        System.out.println("  put(B) 后: " + m.keySet() + "（B 也挪到最后）");
        System.out.println("  链头=最久未用，链尾=刚刚用过——这正是 LRU 需要的顺序！");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：removeEldestEntry——10 行实现 LRU 缓存
    // ------------------------------------------------------------------

    /**
     * LRU（Least Recently Used，最近最少使用）缓存：
     * 容量满时淘汰【最久没被用过】的条目。
     * 实现只需继承 LinkedHashMap 并重写 removeEldestEntry：
     * 每次 put 后 JDK 会回调这个方法，返回 true 就自动删掉链头（最久未用）。
     */
    static class LruCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        LruCache(int capacity) {
            super(16, 0.75f, true);        // accessOrder 必须 true：按访问排链
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;      // 超容量 → 删链头（最久未用的那个）
        }
    }

    static void demoLru() {
        System.out.println("== 第 3 段：LRU 缓存 ==");

        LruCache<String, Integer> cache = new LruCache<>(3);
        cache.put("页面A", 1);
        cache.put("页面B", 2);
        cache.put("页面C", 3);
        System.out.println("  装满 3 个: " + cache.keySet());

        cache.get("页面A");                // A 刚被用过 → 从最旧变成最新
        System.out.println("  get(页面A) 后顺序: " + cache.keySet() + "（A 变最新）");

        cache.put("页面D", 4);             // 超容量 → 自动淘汰最久未用的 B（不是 A！）
        System.out.println("  put(页面D) 后: " + cache.keySet() + "（页面B 被淘汰）");
        System.out.println("  LRU 语义验证：被访问过的 A 活了下来，一直没动的 B 被踢走");

        System.out.println("  这道题的另一副面孔：LeetCode 146 要求 O(1)——");
        System.out.println("  手写版本 = HashMap 定位 + 双向链表维护顺序（LinkedHashMap 内部正是这个结构）");
        System.out.println("  生产环境用 Caffeine/Guava Cache（加了过期、容量 weighting、命中率统计）");
        System.out.println();
    }
}
