-- 演示1【脏读】会话A：改了余额但不提交，最后回滚
-- 隔离级别：READ-UNCOMMITTED（读未提交，最低级）
-- 配对文件：演示1_脏读_会话B.sql（两个窗口/两个连接同时跑）

USE learn_tx;
SET SESSION transaction_isolation = 'READ-UNCOMMITTED';

-- 重置数据（autocommit 立即生效），保证演示可反复运行
UPDATE account SET balance = 1000 WHERE id = 1;

SELECT '【A1】开启事务，给 Alice +500（不提交）' AS step;
BEGIN;
UPDATE account SET balance = balance + 500 WHERE id = 1;

SELECT SLEEP(7);            -- 等 7 秒，期间 B 会来读

SELECT '【A2】回滚——那 +500 从来没发生过' AS step;
ROLLBACK;
