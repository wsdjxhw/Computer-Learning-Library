-- 演示5【行锁与锁等待】会话B：改别的行畅通无阻，改同一行被卡住

USE learn_tx;
-- 锁等待超时设短（默认 50 秒，演示等不了那么久；单位秒）
SET SESSION innodb_lock_wait_timeout = 3;

SELECT SLEEP(1.5);          -- 等 A 先拿到 id=1 的锁

SELECT '【B1】改 id=2：和 A 锁的不是同一行，立即成功' AS step;
UPDATE account SET balance = balance + 100 WHERE id = 2;   -- 秒过：行锁只锁命中行

SELECT '【B2】改 id=1：和 A 抢同一行，被阻塞' AS step;
UPDATE account SET balance = balance + 100 WHERE id = 1;
-- ↑ 卡住约 3 秒后报错：ERROR 1205 Lock wait timeout exceeded
--   （A 在第 6 秒才提交，B 只肯等 3 秒）

ROLLBACK;                  -- 本演示不留下改动
-- 结论：InnoDB 行锁粒度到“行”；锁不到就排队，排队超时就报 1205。
