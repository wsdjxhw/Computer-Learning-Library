-- 演示8【间隙锁】会话B（RC）：同样的 INSERT，这次畅通

USE learn_tx;
SET SESSION transaction_isolation = 'READ-COMMITTED';

SELECT SLEEP(1.5);

SELECT '【B1】INSERT id=7——间隙没锁，立即成功' AS step;
INSERT INTO t_gap VALUES (7, 70);   -- 秒过

SELECT id, v FROM t_gap ORDER BY id;
-- 代价也很明显：RC 放弃间隙锁，幻读防线就少了一半（当前读挡不住别的事务插入）。
-- 对比结论见《知识详解.md》：RR 用间隙锁挡住幻读的“当前读”路径。
