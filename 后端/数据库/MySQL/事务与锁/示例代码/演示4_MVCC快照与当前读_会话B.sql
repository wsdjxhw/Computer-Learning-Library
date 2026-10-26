-- 演示4【快照读 vs 当前读】会话B：RR 下的三种读，结果竟然不同
-- 这是理解 MVCC 的关键实验：普通 SELECT 读快照，SELECT ... FOR UPDATE 读最新

USE learn_tx;
SET SESSION transaction_isolation = 'REPEATABLE-READ';

SELECT '【B1】开启事务，第一次普通读（建立快照）' AS step;
BEGIN;
SELECT balance AS B1_普通读 FROM account WHERE id = 1;     -- 1000

SELECT SLEEP(6);            -- 期间 A 提交了 +500

SELECT '【B2】A 已提交。普通 SELECT（快照读）' AS step;
SELECT balance AS B2_普通读_快照 FROM account WHERE id = 1; -- 仍是 1000！

SELECT '【B3】SELECT ... FOR UPDATE（当前读）' AS step;
SELECT balance AS B3_当前读 FROM account WHERE id = 1 FOR UPDATE; -- 1500！
-- ↑ 当前读：直接读最新版本并加行锁。UPDATE/DELETE/LOCK IN SHARE MODE 同理。

SELECT '【B4】FOR UPDATE 之后再普通读' AS step;
SELECT balance AS B4_普通读 FROM account WHERE id = 1;
-- ↑ 快照没换：仍是 1000（快照在 B1 就定了，当前读不影响它）

COMMIT;
SELECT '【B5】提交后新快照' AS step;
SELECT balance AS B5_提交后 FROM account WHERE id = 1;     -- 1500
