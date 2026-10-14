// 并发工具类学习示例（JUC 协作工具：CountDownLatch / CyclicBarrier / Semaphore / BlockingQueue）
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：CountDownLatch——倒计时门闩
//   第 2 段：CyclicBarrier——循环栅栏
//   第 3 段：Semaphore——信号量限流
//   第 4 段：BlockingQueue——生产者消费者的工程化

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

class SyncToolsDemo {

    public static void main(String[] args) throws Exception {
        demoCountDownLatch();
        demoCyclicBarrier();
        demoSemaphore();
        demoBlockingQueue();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ------------------------------------------------------------------
    // 第 1 段：CountDownLatch——"等待 N 件事完成"
    // ------------------------------------------------------------------
    static void demoCountDownLatch() throws InterruptedException {
        System.out.println("== 第 1 段：CountDownLatch ==");

        // 场景一：主线程等 3 个 worker 全部完工（等 Everyone）。
        // countDown 是"划掉一格"，await 是"数完没？没数完我就堵在这"
        int workers = 3;
        CountDownLatch done = new CountDownLatch(workers);
        for (int i = 1; i <= workers; i++) {
            final int id = i;
            new Thread(() -> {
                sleep(50 * id);                    // 工期各不相同（顺序不保证）
                System.out.println("  worker-" + id + " 完工，划掉一格");
                done.countDown();
            }).start();
        }
        done.await();                              // 主线程在这等到计数归零
        System.out.println("主线程：全部完工，汇总结果");

        // 场景二：发令枪（Everyone 等 1）——3 个线程 await 等同一声枪响同时起跑
        CountDownLatch gun = new CountDownLatch(1);
        for (int i = 1; i <= 3; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    gun.await();                   // 各自等枪响
                    System.out.println("  选手 " + id + " 起跑（顺序不保证）");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }).start();
        }
        sleep(100);                                // 确保选手都就位了
        System.out.println("  发令枪响！");
        gun.countDown();
        sleep(100);                                // 给选手线程打印时间
        System.out.println("计数归零后闩就废了（不可重置）——要循环用请看第 2 段");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：CyclicBarrier——"人到齐了一起过门"
    // ------------------------------------------------------------------
    static void demoCyclicBarrier() throws InterruptedException {
        System.out.println("== 第 2 段：CyclicBarrier ==");

        int parties = 3;
        // 人到齐后先执行这个回调（此时大家都还堵在 await 上，回调由最后到的人执行）
        CyclicBarrier barrier = new CyclicBarrier(parties,
                () -> System.out.println("  —— 3 人到齐，开会！——"));

        Thread[] threads = new Thread[parties];
        for (int i = 0; i < parties; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    sleep(40 * id);                // 先各自干活（顺序不保证）
                    System.out.println("  成员 " + id + " 到达集合点，等待其他人");
                    barrier.await();               // 人没到齐就堵在这
                    System.out.println("  成员 " + id + " 开始第二阶段（顺序不保证）");
                } catch (Exception e) { Thread.currentThread().interrupt(); }
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        // 与 CountDownLatch 的核心区别：CyclicBarrier 可【循环复用】
        System.out.println("同一个人工重置都不用——直接下一轮 await（Cyclic 的含义）");
        System.out.println("选型：等事件完成用 Latch；多线程分阶段会合用 Barrier");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：Semaphore——限流：3 个车位 5 辆车
    // ------------------------------------------------------------------
    static void demoSemaphore() throws InterruptedException {
        System.out.println("== 第 3 段：Semaphore ==");

        Semaphore parking = new Semaphore(3);      // 3 个车位
        // "当前在场"计数：acquire 之后才 +1，所以打印出的数字永远 ≤ 3——
        // 证据在数字里，不在打印顺序里（线程打印顺序本来就不保证）
        AtomicInteger inPark = new AtomicInteger(0);
        Thread[] cars = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int id = i + 1;
            cars[i] = new Thread(() -> {
                try {
                    parking.acquire();             // 抢车位：没车位就堵在这等
                    System.out.println("  车 " + id + " 停入，当前在场 "
                            + inPark.incrementAndGet() + " 辆（顺序不保证）");
                    sleep(150);                    // 停一会儿
                    inPark.decrementAndGet();
                    parking.release();             // 让出车位
                    System.out.println("  车 " + id + " 离开");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            cars[i].start();
        }
        for (Thread t : cars) t.join();

        // 结果特征：打印出的"当前在场"永远不会超过 3（5 辆车串成了两批）
        System.out.println("5 辆车、3 车位 → 在场数从未超过 3：限流/资源池的标准做法");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：BlockingQueue——生产者消费者的工程化
    // ------------------------------------------------------------------
    static void demoBlockingQueue() throws InterruptedException {
        System.out.println("== 第 4 段：BlockingQueue ==");

        // 容量 2 的阻塞队列：put 满了自动等，take 空了自动等——
        // 手写 wait/notify 版本（见《多线程基础》第 3 段）它替你全包了
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);

        Thread producer = new Thread(() -> {
            try {
                String[] goods = {"A", "B", "C", "D"};
                for (String g : goods) {
                    queue.put(g);                  // 队列满就阻塞等待，不用写一行锁
                    System.out.println("  生产 " + g + "（队列现有 " + queue.size() + " 个）");
                }
                queue.put("END");                  // 毒丸：约定的结束信号
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        Thread consumer = new Thread(() -> {
            try {
                String g;
                while (!"END".equals(g = queue.take())) {   // 队列空就阻塞等待
                    sleep(100);                    // 消费得比生产慢 → 队列会顶满 → 生产者被挡
                    System.out.println("  消费 " + g);
                }
                System.out.println("  收到 END，消费者退出");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        producer.start(); consumer.start();
        producer.join(); consumer.join();
        System.out.println("线程池的 workQueue、线程间任务传递（Executor 框架的地基）都是它");
        System.out.println();
    }
}
