// 锁与原子类学习示例（JUC：java.util.concurrent.locks / atomic）
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：ReentrantLock 基础与可重入性
//   第 2 段：tryLock——拿不到锁的正确姿势
//   第 3 段：Condition——多个等待队列的精细唤醒
//   第 4 段：ReadWriteLock——读读并发、读写互斥
//   第 5 段：AtomicInteger 与 CAS

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class LockAtomicDemo {

    public static void main(String[] args) throws Exception {
        demoReentrantLock();
        demoTryLock();
        demoCondition();
        demoReadWriteLock();
        demoAtomicAndCas();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ------------------------------------------------------------------
    // 第 1 段：ReentrantLock 基础与可重入
    // ------------------------------------------------------------------
    static final ReentrantLock lock = new ReentrantLock();

    static void demoReentrantLock() {
        System.out.println("== 第 1 段：ReentrantLock 与可重入 ==");

        // 标准三件套：lock() → try 干活 → finally unlock()（unlock 漏了就是死锁事故）
        lock.lock();
        try {
            System.out.println("outer 拿到锁");
            inner();                  // 同一线程再次 lock 同一把锁：可重入，不会把自己锁死
        } finally {
            lock.unlock();            // 加了几次就要解几次——这里一共解两次
        }
        System.out.println("可重入：同一线程可重复加锁，synchronized 也一样有这个性质");
        System.out.println();
    }

    static void inner() {
        lock.lock();
        try {
            System.out.println("inner 再次拿到同一把锁（可重入成功）");
        } finally {
            lock.unlock();
        }
    }

    // ------------------------------------------------------------------
    // 第 2 段：tryLock——"抢不到就算了"
    // ------------------------------------------------------------------
    static void demoTryLock() throws InterruptedException {
        System.out.println("== 第 2 段：tryLock ==");

        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("holder 占住锁 300ms");
                sleep(300);
            } finally {
                lock.unlock();
            }
        });
        holder.start();
        sleep(50);                     // 让 holder 先把锁拿到手

        // synchronized 做不到的事：抢不到锁不傻等，立刻知道失败（或限时等待）
        if (lock.tryLock()) {          // 立刻返回，绝不等待
            try { System.out.println("立刻 tryLock 成功（不该发生）"); }
            finally { lock.unlock(); }
        } else {
            System.out.println("立刻 tryLock 失败 → 不等待，先去干别的");
        }

        if (lock.tryLock(1, TimeUnit.SECONDS)) {   // 限时等待：1 秒内等到就成功
            try { System.out.println("限时 tryLock 成功（等到了 holder 释放）"); }
            finally { lock.unlock(); }
        }
        holder.join();
        System.out.println("tryLock 是 synchronized 办不到的能力：避免无限排队、防死锁探测");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：Condition——一把锁配多个"等待室"
    // ------------------------------------------------------------------
    /** 两条件队列版单槽缓冲：notFull 给生产者等，notEmpty 给消费者等——唤醒精确制导。 */
    static class TwoConditionBuffer {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();   // 等待室 1：生产者
        private final Condition notEmpty = lock.newCondition();  // 等待室 2：消费者
        private Integer item = null;

        void put(int v) throws InterruptedException {
            lock.lock();
            try {
                while (item != null) notFull.await();  // 槽满：进"生产者等待室"
                item = v;
                System.out.println("  生产者放入: " + v);
                notEmpty.signal();                     // 只叫醒消费者（notifyAll 是全村广播）
            } finally { lock.unlock(); }
        }

        int take() throws InterruptedException {
            lock.lock();
            try {
                while (item == null) notEmpty.await(); // 槽空：进"消费者等待室"
                int v = item;
                item = null;
                System.out.println("  消费者取走: " + v);
                notFull.signal();                      // 只叫醒生产者
                return v;
            } finally { lock.unlock(); }
        }
    }

    static void demoCondition() throws InterruptedException {
        System.out.println("== 第 3 段：Condition ==");

        TwoConditionBuffer buffer = new TwoConditionBuffer();
        Thread p = new Thread(() -> {
            try { for (int i = 1; i <= 3; i++) buffer.put(i); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread c = new Thread(() -> {
            try { for (int i = 1; i <= 3; i++) buffer.take(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        p.start(); c.start(); p.join(); c.join();
        System.out.println("await/signal 对应 wait/notify，但一把锁能有多个条件队列——");
        System.out.println("synchronized 只有一个等待室，notifyAll 只能全村广播（还可能叫错人）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：ReadWriteLock——读读并发、读写互斥
    // ------------------------------------------------------------------
    static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    static String config = "v1";

    static void demoReadWriteLock() throws InterruptedException {
        System.out.println("== 第 4 段：ReadWriteLock ==");

        // 两个读线程同时进入读锁——读不改数据，并发读安全且高效
        Runnable reader = () -> {
            rwLock.readLock().lock();
            try {
                System.out.println("  " + Thread.currentThread().getName() + " 正在读: " + config);
                sleep(100);            // 模拟读耗时——期间另一个读线程也在里面
            } finally { rwLock.readLock().unlock(); }
        };
        Thread r1 = new Thread(reader, "读者A");
        Thread r2 = new Thread(reader, "读者B");
        r1.start(); r2.start(); r1.join(); r2.join();
        System.out.println("两个读线程【同时】持有读锁（总耗时 ≈ 一次读，不是两次）");

        // 写锁与一切互斥：写的时候谁都不许进（读也不行），防止读到写了一半的数据
        Thread writer = new Thread(() -> {
            rwLock.writeLock().lock();
            try {
                config = "v2";
                System.out.println("  写者更新为 " + config + "（期间读写全被挡在外面）");
            } finally { rwLock.writeLock().unlock(); }
        }, "写者");
        writer.start(); writer.join();
        System.out.println("适合读多写少：配置、缓存、白名单这类数据");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 5 段：AtomicInteger 与 CAS
    // ------------------------------------------------------------------
    static final AtomicInteger atomicCount = new AtomicInteger(0);

    static void demoAtomicAndCas() throws InterruptedException {
        System.out.println("== 第 5 段：AtomicInteger 与 CAS ==");

        // 无锁线程安全：两个线程各加 100000 次，结果必然精确——没有 synchronized 也没有锁
        Runnable task = () -> {
            for (int i = 0; i < 100_000; i++) atomicCount.incrementAndGet();
        };
        Thread a = new Thread(task), b = new Thread(task);
        a.start(); b.start(); a.join(); b.join();
        System.out.println("AtomicInteger 累加 200000 次 = " + atomicCount.get() + "（无锁也精确）");

        // 原理是 CAS（比较并交换）：旧值没被人动过才写入，动过就重试——乐观策略
        // 手写一个"CAS 循环版 ++"看看 incrementAndGet 的内部逻辑长什么样：
        AtomicInteger demo = new AtomicInteger(5);
        casIncrement(demo);
        System.out.println("手写 CAS 循环把 5 加成了 " + demo.get());
        System.out.println("CAS 三操作数：期望值(内存里现在的) / 新值 / 内存地址；不匹配就失败重试");
        System.out.println("代价：高竞争下自旋重试空转（LongAdder 分段解决）、ABA 问题（见八股追问）");
        System.out.println();
    }

    /** CAS 循环：读取现值 → 算新值 → compareAndSet 不成功（说明有人抢先）就重来。 */
    static void casIncrement(AtomicInteger target) {
        int oldValue, newValue;
        do {
            oldValue = target.get();          // 步骤 1：读当前值
            newValue = oldValue + 1;          // 步骤 2：基于旧值算新值
        } while (!target.compareAndSet(oldValue, newValue));  // 步骤 3：没被人改过才生效
    }
}
