-- 演示6【死锁】会话B：先锁 id=2，再要 id=1——和 A 反着来

USE learn_tx;

SELECT SLEEP(1);

SELECT '【B1】锁 id=2' AS step;
BEGIN;
UPDATE account SET balance = balance + 1 WHERE id = 2;

SELECT SLEEP(3);

SELECT '【B2】再要 id=1（A 正拿着）' AS step;
UPDATE account SET balance = balance + 1 WHERE id = 1;
-- ↑ 死锁形成：A 等 B 的 id=2，B 等 A 的 id=1
--   InnoDB 检测到环，主动回滚其中一方：ERROR 1213 Deadlock found

ROLLBACK;
-- 结论：死锁不需要人工干预，InnoDB 秒级检测并牺牲一方；
--   应用要做的只是“捕获 1213 重试一次”，以及按相同顺序加锁来预防。
