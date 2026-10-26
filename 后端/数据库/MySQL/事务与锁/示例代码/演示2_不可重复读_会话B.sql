-- 演示2【不可重复读 + 幻读】会话B：一个事务里读两次，前后不一样
-- 隔离级别：READ-COMMITTED——每次读都看“最新已提交”，事务中途结果会变

USE learn_tx;
SET SESSION transaction_isolation = 'READ-COMMITTED';

SELECT '【B1】开启事务，第一次读' AS step;
BEGIN;
SELECT balance AS 第一次_余额 FROM account WHERE id = 1;   -- 1000
SELECT COUNT(*) AS 第一次_总人数 FROM account;             -- 2

SELECT SLEEP(6);            -- 期间 A 提交了修改和插入

SELECT '【B2】同一个事务里，第二次读' AS step;
SELECT balance AS 第二次_余额 FROM account WHERE id = 1;   -- 1500：不可重复读
SELECT COUNT(*) AS 第二次_总人数 FROM account;             -- 3：幻读
COMMIT;

-- 结论：RC 解决了脏读（只读已提交），但同一事务内两次读可以不一致：
--   改了值 → 不可重复读；多了行 → 幻读。
