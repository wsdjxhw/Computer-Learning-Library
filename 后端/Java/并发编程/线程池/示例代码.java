// 线程池学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：Executors 工厂快速上手
//   第 2 段：ThreadPoolExecutor 七参数与执行流程（含拒绝）
//   第 3 段：Future——拿回任务的返回值
//   第 4 段：为什么生产不建议用 Executors 工厂
//
// 说明：任务调度顺序有轻微不确定性，注释已标注；完成数量等结果不受影响。

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ThreadPoolDemo {

    public static void main(String[] args) throws Exception {
        demoExecutorsFactory();
        demoSevenParams();
        demoFuture();
        demoWhyNotExecutors();
    }

    /** 关停线程池并等任务跑完：shutdown 不接新任务但跑完存量；awaitTermination 等待收尾。 */
    static void shutdownAndWait(ExecutorService pool) throws InterruptedException {
        pool.shutdown();
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {  // 最多等 10 秒
            pool.shutdownNow();                              // 超时强制停（interrupt 正在跑的任务）
        }
    }

    // ------------------------------------------------------------------
    // 第 1 段：Executors 工厂——最省事的用法
    // ------------------------------------------------------------------
    static void demoExecutorsFactory() throws Exception {
        System.out.println("== 第 1 段：Executors 工厂 ==");

        // 固定 3 个线程的池：提交 5 个任务，只有 3 个"工位"，其余排队
        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (int i = 1; i <= 5; i++) {
            final int id = i;
            pool.execute(() -> {                     // execute：提交无返回值的任务
                System.out.println("  任务 " + id + " 由 " + Thread.currentThread().getName() + " 执行（顺序不保证）");
            });
        }
        shutdownAndWait(pool);
        System.out.println("线程复用：5 个任务只用了 pool-1-thread-1 ~ 3 三个线程，不新建不销毁");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：ThreadPoolExecutor 七参数与执行流程（本篇核心）
    // ------------------------------------------------------------------
    static void demoSevenParams() throws Exception {
        System.out.println("== 第 2 段：七参数与执行流程 ==");

        // 七个参数一次配齐（含义逐个对应正文表格）：
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2,                                      // corePoolSize：常驻线程数（正式工）
                3,                                      // maximumPoolSize：最大线程数（正式工+临时工）
                1, TimeUnit.SECONDS,                    // keepAliveTime + 单位：临时工空闲多久裁掉
                new ArrayBlockingQueue<>(2),            // workQueue：任务排队区（容量 2）
                r -> {                                  // threadFactory：定制线程（起名，排查用）
                    Thread t = new Thread(r, "my-pool-" + (threadSeq++));
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()   // handler：满了怎么办（默认直接抛异常）
        );

        // 数学题：2 正式 + 2 排队 + 1 临时 = 5 个容纳能力，提交 6 个 → 第 6 个被拒！
        for (int i = 1; i <= 6; i++) {
            final int id = i;
            try {
                pool.execute(() -> {
                    System.out.println("  任务 " + id + " 进入 " + Thread.currentThread().getName());
                    sleep(300);                         // 占住工位 300ms，制造"拥挤"现场
                });
                System.out.println("任务 " + id + " 提交成功");
            } catch (RejectedExecutionException e) {
                System.out.println("任务 " + id + " 被拒绝: RejectedExecutionException（线程满 + 队列满）");
            }
        }

        shutdownAndWait(pool);
        System.out.println("执行流程口诀：核心满 → 进队列；队列满 → 开临时线程；到最大还满 → 走拒绝策略");
        System.out.println("完成数 = " + pool.getCompletedTaskCount()
                + "，历史峰值线程数 = " + pool.getLargestPoolSize() + "（2 正式 + 1 临时）");
        System.out.println();
    }

    static int threadSeq = 1;

    // ------------------------------------------------------------------
    // 第 3 段：Future——拿回任务的结果
    // ------------------------------------------------------------------
    static void demoFuture() throws Exception {
        System.out.println("== 第 3 段：Future ==");

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // Callable 有返回值、能抛受检异常（Runnable 两者都不行）
            Callable<Integer> sumTask = () -> {
                sleep(200);                             // 假装在辛苦计算
                return 1 + 2 + 3;
            };

            // submit 立刻返回 Future"取货凭证"，main 可以先干别的
            Future<Integer> future = pool.submit(sumTask);
            System.out.println("提交后立刻返回，isDone() = " + future.isDone());

            System.out.println("main 先干点别的……");
            int result = future.get();                  // get() 阻塞等结果
            System.out.println("future.get() = " + result + "，此时 isDone() = " + future.isDone());

            // get(超时)：最多等多久，等不到抛 TimeoutException——防止无限等待
            Future<Integer> slow = pool.submit(() -> { sleep(3000); return 99; });
            try {
                slow.get(200, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                System.out.println("200ms 内没等到: TimeoutException（slow 任务稍后仍会算完）");
            }
            slow.cancel(true);                          // 不等了：interrupt 正在执行的任务
        } finally {
            shutdownAndWait(pool);
        }
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：为什么生产不建议用 Executors 工厂
    // ------------------------------------------------------------------
    static void demoWhyNotExecutors() throws Exception {
        System.out.println("== 第 4 段：工厂方法的隐患 ==");

        // 隐患 1：newFixedThreadPool 的队列是【无界】LinkedBlockingQueue——
        //        任务堆积速度超过消费速度时队列无限增长，最终 OOM
        // 隐患 2：newCachedThreadPool 的 max 是 Integer.MAX_VALUE——
        //        请求突增时无限开新线程，同样 OOM（线程是昂贵资源：栈内存 + 调度成本）
        System.out.println("newFixedThreadPool → 无界队列 OOM；newCachedThreadPool → 无限线程 OOM");
        System.out.println("阿里巴巴规约：强制用 new ThreadPoolExecutor(...) 显式声明全部参数");

        // 线程数怎么定：CPU 密集 ≈ 核数 + 1；IO 密集 ≈ 核数 × 2 起（等待比例越高可越大）
        System.out.println("本机 CPU 核数 = " + Runtime.getRuntime().availableProcessors()
                + "（CPU 密集任务就开这么多，开多了反而互相抢核）");
        System.out.println();
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();         // 保持中断标记，不悄悄吞掉
        }
    }
}
