-- 演示3【可重复读】会话B：同一个事务里再读两次，这次结果不变
-- 隔离级别：REPEATABLE-READ（InnoDB 默认）——靠 MVCC 快照实现

USE learn_tx;
SET SESSION transaction_isolation = 'REPEATABLE-READ';

SELECT '【B1】开启事务，第一次读（此刻建立快照）' AS step;
BEGIN;
SELECT balance AS 第一次_余额 FROM account WHERE id = 1;   -- 1000
SELECT COUNT(*) AS 第一次_总人数 FROM account;             -- 2

SELECT SLEEP(6);            -- 期间 A 提交了同样的修改和插入

SELECT '【B2】同一个事务里，第二次读（还是旧快照）' AS step;
SELECT balance AS 第二次_余额 FROM account WHERE id = 1;   -- 仍是 1000
SELECT COUNT(*) AS 第二次_总人数 FROM account;             -- 仍是 2
COMMIT;

SELECT '【B3】提交后再读（新事务 = 新快照）' AS step;
SELECT COUNT(*) AS 提交后_总人数 FROM account;             -- 3

-- 结论：RR 下“普通 SELECT”读的是事务开始时的快照（MVCC），
--   A 的提交对 B 不可见——可重复读 + 快照读不出现幻读。
