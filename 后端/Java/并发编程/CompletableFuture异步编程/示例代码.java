// CompletableFuture 与异步编程学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，orTimeout 需 JDK 9+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：Future 的局限——CompletableFuture 解决什么问题
//   第 2 段：回调链 thenApply / thenAccept
//   第 3 段：thenApply vs thenCompose vs thenCombine
//   第 4 段：allOf / anyOf 并行编排
//   第 5 段：异常传播与 exceptionally / handle / whenComplete
//   第 6 段：自定义线程池与超时

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class AsyncDemo {

    public static void main(String[] args) throws Exception {
        demoFutureLimit();
        demoChain();
        demoComposeAndCombine();
        demoAllOfAnyOf();
        demoException();
        demoPoolAndTimeout();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ------------------------------------------------------------------
    // 第 1 段：Future 的局限——拿结果只能傻等或轮询
    // ------------------------------------------------------------------
    static void demoFutureLimit() throws Exception {
        System.out.println("== 第 1 段：Future 的局限 ==");

        ExecutorService pool = Executors.newFixedThreadPool(1);
        Future<String> future = pool.submit(() -> {
            sleep(300);                        // 模拟慢任务（如远程调用）
            return "订单数据";
        });

        // Future 拿结果只有两条路：get() 阻塞干等，或 isDone() 轮询——都没有"完成时通知我"
        while (!future.isDone()) {
            System.out.println("  主线程：好了吗？没好。（只能反复问，或干脆 get() 卡死等）");
            sleep(150);
        }
        System.out.println("  future.get() = " + future.get());
        pool.shutdown();

        System.out.println("痛点①：取结果必须阻塞或轮询；痛点②：A 完成后接着做 B、");
        System.out.println("A 和 B 都完成后合并——这类编排 Future 写不出来（或者说写出来没法看）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：回调链——完成时自动触发下一步
    // ------------------------------------------------------------------
    static void demoChain() {
        System.out.println("== 第 2 段：回调链 ==");

        CompletableFuture<Void> chain = CompletableFuture
            .supplyAsync(() -> {                       // 异步起跑：提交到线程池立刻返回
                System.out.println("  [1] 查询订单（" + Thread.currentThread().getName() + "）");
                sleep(100);
                return "订单#1";
            })
            .thenApply(order -> {                      // 上一步结果 → 加工后继续传
                System.out.println("  [2] 查物流（" + Thread.currentThread().getName() + "）");
                sleep(100);
                return order + " 已发货";
            })
            .thenApply(logistics -> logistics + "，预计明天送达")
            .thenAccept(result -> {                   // 终点：消费结果（无返回值）
                System.out.println("  [3] 最终结果: " + result);
            });

        // 主线程提交完立刻回来干别的——不用等，链跑完了自然会执行 [3]
        System.out.println("  主线程：链已提交，我先干别的（这行先于/后于回调都正常）");
        chain.join();                                 // 演示用：join 等链走完再进下一段
        System.out.println("整条链没有一个 get() 阻塞调用——完成事件自己往下流");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：thenApply vs thenCompose vs thenCombine
    // ------------------------------------------------------------------
    static void demoComposeAndCombine() {
        System.out.println("== 第 3 段：三个组合方法 ==");

        // thenCompose：下一步【本身也是异步的】——函数返回 CompletableFuture（像 flatMap）
        // 场景：先查用户（异步）→ 再拿用户 id 查订单（又一个异步调用）
        CompletableFuture<String> orders = CompletableFuture
            .supplyAsync(() -> { System.out.println("  查用户 id=42"); return 42; })
            .thenCompose(userId -> CompletableFuture.supplyAsync(() -> {
                System.out.println("  用 id=" + userId + " 查订单（依赖上一步结果的异步调用）");
                sleep(100);
                return "3 笔订单";
            }));

        // thenCombine：合并两条【互不依赖】的异步链
        CompletableFuture<String> points = CompletableFuture.supplyAsync(() -> {
            sleep(150);                               // 另一条独立链：查积分
            return "积分 1200";
        });
        CompletableFuture<String> merged = orders.thenCombine(points,
                (o, p) -> "汇总：用户有 " + o + "，" + p);
        System.out.println("  " + merged.join());     // 两条链并行跑，都好了才合并

        System.out.println("记忆法：thenApply 同步加工；thenCompose 依赖型异步（flatMap）；");
        System.out.println("thenCombine 汇合型合并（两条链互不相识，最后碰头）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：allOf / anyOf——并行编排
    // ------------------------------------------------------------------
    static void demoAllOfAnyOf() {
        System.out.println("== 第 4 段：allOf / anyOf ==");

        // 场景：并行向 3 个渠道比价，allOf 等全部返回
        CompletableFuture<String> a = priceQuery("渠道A", 150);
        CompletableFuture<String> b = priceQuery("渠道B", 100);
        CompletableFuture<String> c = priceQuery("渠道C", 50);
        CompletableFuture.allOf(a, b, c).join();       // 等三条全部完成
        System.out.println("  allOf 全部完成: " + a.join() + "；" + b.join() + "；" + c.join());

        // anyOf：谁先回来用谁（多活一：同时请求多个副本，最快者胜）
        Object fastest = CompletableFuture.anyOf(
                priceQuery("副本1", 120), priceQuery("副本2", 40), priceQuery("副本3", 200)).join();
        System.out.println("  anyOf 最先返回: " + fastest);

        System.out.println("总耗时 ≈ 最慢一条（allOf）/ 最快一条（anyOf），而不是三条相加");
        System.out.println();
    }

    static CompletableFuture<String> priceQuery(String name, long costMs) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(costMs);                            // 模拟各渠道响应速度不同
            return name + " 报价完成（耗时 " + costMs + "ms）";
        });
    }

    // ------------------------------------------------------------------
    // 第 5 段：异常会沿链传播，exceptionally / handle 负责兜底
    // ------------------------------------------------------------------
    static void demoException() {
        System.out.println("== 第 5 段：异常处理 ==");

        // 中间某步抛异常：后面的 thenApply 全部跳过，直达异常处理器——不会丢、不会静默吞掉
        // 注：<String> 是显式类型见证：lambda 只抛异常不返回值，编译器推不出 T，必须人工指明
        CompletableFuture<String> rescued = CompletableFuture
            .<String>supplyAsync(() -> { throw new RuntimeException("库存服务挂了"); })
            .thenApply(v -> { System.out.println("  这行不会执行（异常跳过了它）"); return v; })
            .exceptionally(ex -> "兜底数据：走本地缓存");        // 只管异常，返回替代值
        System.out.println("  exceptionally 兜底: " + rescued.join());

        // handle：正常/异常都要处理（像 try-catch-finally 的合体，两个分支都给返回值）
        CompletableFuture<String> handled = CompletableFuture
            .supplyAsync(() -> { throw new RuntimeException("风控服务超时"); })
            .handle((result, ex) -> {
                if (ex != null) return "降级：放行并记录告警";
                return "正常结果: " + result;
            });
        System.out.println("  handle 降级: " + handled.join());

        // whenComplete：只旁观不修改（打日志、埋点用）；异常原样继续往下传
        CompletableFuture<String> observed = CompletableFuture
            .supplyAsync(() -> "正常数据")
            .whenComplete((result, ex) -> System.out.println(
                    "  whenComplete 看到: " + result + " / 异常=" + (ex == null ? "无" : ex.getMessage())));
        System.out.println("  结果原样透传: " + observed.join());

        System.out.println("注意：join() 会把链内异常包成 CompletionException 抛出——不兜底它不会消失");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 6 段：自定义线程池与超时——生产代码的必修课
    // ------------------------------------------------------------------
    static void demoPoolAndTimeout() {
        System.out.println("== 第 6 段：线程池与超时 ==");

        // 不传 Executor 时用 ForkJoinPool.commonPool()——全 JVM 共享的一口锅
        CompletableFuture.supplyAsync(() -> "默认池的线程: " + Thread.currentThread().getName())
                .thenAccept(s -> System.out.println("  " + s));

        // 生产建议：传自己的池（隔离、可控、可监控——道理同《线程池》篇的规约）
        AtomicInteger seq = new AtomicInteger();
        ExecutorService myPool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "my-async-" + seq.incrementAndGet());
            return t;
        });
        CompletableFuture.supplyAsync(() -> "自定义池的线程: " + Thread.currentThread().getName(), myPool)
                .thenAccept(s -> System.out.println("  " + s));

        // orTimeout：给异步任务上闹钟（JDK 9+）；completeOnTimeout：超时给默认值而不是抛异常
        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
            sleep(300);                               // 模拟一个磨蹭的下游
            return "慢慢腾腾的结果";
        }).orTimeout(150, TimeUnit.MILLISECONDS)        // 150ms 等不到就以 TimeoutException 结束
          .exceptionally(ex -> "超时兜底（" + ex.getClass().getSimpleName() + "）");
        System.out.println("  orTimeout 结果: " + slow.join());

        myPool.shutdown();                             // 自己建的池自己收尾（commonPool 不用管）
        System.out.println("一句话总结：supplyAsync 默认蹭公共池，生产必须传自己的池 + 配上超时");
        System.out.println();
    }
}
