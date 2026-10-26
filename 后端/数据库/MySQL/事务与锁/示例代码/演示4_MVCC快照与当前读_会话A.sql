-- 演示4【快照读 vs 当前读】会话A：在 B 的事务中间提交一次修改

USE learn_tx;

-- 重置数据
UPDATE account SET balance = 1000 WHERE id = 1;

SELECT SLEEP(2);            -- 等 B 建立快照

SELECT '【A1】提交：Alice +500' AS step;
UPDATE account SET balance = balance + 500 WHERE id = 1;   -- autocommit
