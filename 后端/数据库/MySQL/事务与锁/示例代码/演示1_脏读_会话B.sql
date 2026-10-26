-- 演示1【脏读】会话B：两次读余额
-- 隔离级别：READ-UNCOMMITTED——允许读到别人未提交的数据

USE learn_tx;
SET SESSION transaction_isolation = 'READ-UNCOMMITTED';

SELECT SLEEP(1);            -- 等 A 先改完

SELECT balance AS B1_读到的余额 FROM account WHERE id = 1;
-- ↑ 1500：A 还没提交，B 却读到了。这就是“脏读”：
--   拿着这个数去做转账/报表，A 一回滚，一切都是错的。

SELECT SLEEP(7);            -- 等 A 回滚

SELECT balance AS B2_A回滚后再读 FROM account WHERE id = 1;
-- ↑ 1000：凭空出现又凭空消失的 500。
-- 结论：READ-UNCOMMITTED 几乎不可用，InnoDB 默认 READ-REPEATABLE。
