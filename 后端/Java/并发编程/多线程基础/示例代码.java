// 多线程基础学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：创建与启动线程（Thread vs Runnable）
//   第 2 段：竞态条件与 synchronized
//   第 3 段：wait/notify 生产者-消费者
//   第 4 段：volatile 标志位与线程中断
//   第 5 段：线程状态观察
//
// 说明：多线程输出天然存在调度不确定性——凡顺序可能变化的输出，注释里都标了"顺序不保证"。

class ThreadBasicDemo {

    public static void main(String[] args) throws InterruptedException {
        demoCreateAndStart();
        demoRaceAndSync();
        demoWaitNotify();
        demoVolatileAndInterrupt();
        demoThreadState();
    }

    // ------------------------------------------------------------------
    // 第 1 段：创建与启动线程
    // ------------------------------------------------------------------
    static void demoCreateAndStart() throws InterruptedException {
        System.out.println("== 第 1 段：创建与启动 ==");

        // 方式 1：继承 Thread，重写 run() 写任务逻辑
        Thread t1 = new Thread() {
            @Override
            public void run() {
                for (int i = 1; i <= 3; i++) {
                    System.out.println("  [子线程A] 第 " + i + " 次（顺序不保证）");
                }
            }
        };

        // 方式 2：实现 Runnable，丢给 Thread——推荐：不占用继承名额、任务与线程解耦
        // Runnable 是函数式接口，lambda 一行搞定
        Thread t2 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                System.out.println("  [子线程B] 第 " + i + " 次（顺序不保证）");
            }
        }, "worker-B");                     // 顺手给线程起名，排查问题时救命

        t1.start();                         // start() 才会开新线程执行 run()
        t2.start();
        // 注意：直接调 t1.run() 不开新线程——那就是普通方法调用，在 main 里同步执行！

        t1.join();                          // join()：main 在这里等 t1 做完
        t2.join();                          // 不等的话 main 先跑完，进程直接结束
        System.out.println("main: 两个子线程都已结束（join 的功劳）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：竞态条件与 synchronized
    // ------------------------------------------------------------------
    static void demoRaceAndSync() throws InterruptedException {
        System.out.println("== 第 2 段：竞态与 synchronized ==");

        // counter++ 不是一步到位：读→加 1→写回 三步，两个线程交错执行就会丢更新
        Object lock = new Object();
        final int LOOP = 100_000;
        Runnable unsafeTask = () -> {
            for (int i = 0; i < LOOP; i++) { counter++; }
        };
        Thread a = new Thread(unsafeTask);
        Thread b = new Thread(unsafeTask);
        a.start(); b.start(); a.join(); b.join();
        System.out.println("无锁累加 " + (LOOP * 2) + " 次 = " + counter
                + "（几乎总是小于期望——丢更新了）");

        // synchronized(锁对象)：同一时刻只有一个线程能进代码块——三步变成不可分割的原子操作
        counter = 0;
        Runnable safeTask = () -> {
            for (int i = 0; i < LOOP; i++) {
                synchronized (lock) { counter++; }
            }
        };
        Thread c = new Thread(safeTask);
        Thread d = new Thread(safeTask);
        c.start(); d.start(); c.join(); d.join();
        System.out.println("加锁累加 " + (LOOP * 2) + " 次 = " + counter + "（每次都精确）");
        System.out.println();
    }

    static int counter = 0;                 // 被多线程共享的静态字段（竞态演示用）

    // ------------------------------------------------------------------
    // 第 3 段：wait/notify 生产者-消费者
    // ------------------------------------------------------------------
    /** 单槽缓冲区：put 放不进就等，take 取不到就等——线程间"接力"的最小模型。 */
    static class OneSlot {
        private Integer item = null;        // null 表示槽是空的

        synchronized void put(int value) throws InterruptedException {
            while (item != null) {          // 槽满 → 等消费者取走（用 while 防"虚假唤醒"）
                wait();                     // 释放锁并沉睡，直到被 notify 唤醒
            }
            item = value;
            System.out.println("  生产者放入: " + value);
            notifyAll();                    // 唤醒等着取货的消费者
        }

        synchronized int take() throws InterruptedException {
            while (item == null) {          // 槽空 → 等生产者放东西
                wait();
            }
            int value = item;
            item = null;
            System.out.println("  消费者取走: " + value);
            notifyAll();                    // 唤醒等着放货的生产者
            return value;
        }
    }

    static void demoWaitNotify() throws InterruptedException {
        System.out.println("== 第 3 段：wait/notify ==");

        OneSlot buffer = new OneSlot();
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) { buffer.put(i); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "producer");
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) { buffer.take(); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "consumer");

        producer.start(); consumer.start();
        producer.join(); consumer.join();
        System.out.println("单槽强制交替：放一个才能取一个（输出顺序确定）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：volatile 标志位与线程中断
    // ------------------------------------------------------------------
    static volatile boolean running = true; // volatile：一个线程改了，另一个线程立刻可见

    static void demoVolatileAndInterrupt() throws InterruptedException {
        System.out.println("== 第 4 段：volatile 与中断 ==");

        // 不加 volatile 的话，工作线程可能把 running 缓存成"永真的 true"，永远停不下来
        Thread worker = new Thread(() -> {
            long count = 0;
            while (running) { count++; }    // 每轮都重新读主内存里的 running
            System.out.println("  worker 收到停止信号，共循环 " + count + " 次");
        });
        worker.start();

        Thread.sleep(50);                   // 让 worker 先跑一会儿
        running = false;                    // main 翻转开关（对 worker 立刻可见）
        worker.join();

        // interrupt：给"正在睡觉/阻塞"的线程发通知——sleep/.join/wait 会抛 InterruptedException
        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                System.out.println("  sleeper 被叫醒: InterruptedException");
                Thread.currentThread().interrupt();   // 惯例：把中断标记补回去，让上层还能感知
            }
        });
        sleeper.start();
        Thread.sleep(100);
        sleeper.interrupt();
        sleeper.join();
        System.out.println("停线程三件套：volatile 开关（轮询场景）/ interrupt（阻塞场景）/ 状态判断");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 5 段：线程状态观察
    // ------------------------------------------------------------------
    static void demoThreadState() throws InterruptedException {
        System.out.println("== 第 5 段：线程状态 ==");

        Thread t = new Thread(() -> { });   // 什么也不做，启动后立即结束

        System.out.println("start 前: " + t.getState());       // NEW（只是对象，未开工）
        t.start();
        t.join();                                            // 等它彻底结束
        System.out.println("join 后: " + t.getState());       // TERMINATED（终态）

        System.out.println("六种状态: NEW → RUNNABLE ↔ BLOCKED/WAITING/TIMED_WAITING → TERMINATED");
        System.out.println("（详见《知识详解.md》状态图；BLOCKED 是等锁，WAITING 是 wait/join 无期限）");
        System.out.println();
    }
}
