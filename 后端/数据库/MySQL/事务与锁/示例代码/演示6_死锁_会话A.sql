-- 演示6【死锁】会话A：先锁 id=1，再要 id=2

USE learn_tx;

-- 重置数据
UPDATE account SET balance = 1000 WHERE id <= 2;

SELECT '【A1】锁 id=1' AS step;
BEGIN;
UPDATE account SET balance = balance + 1 WHERE id = 1;

SELECT SLEEP(3);            -- 等 B 也锁上 id=2

SELECT '【A2】再要 id=2（B 正拿着）' AS step;
UPDATE account SET balance = balance + 1 WHERE id = 2;
-- ↑ 与 B 互相等待 → InnoDB 死锁检测立即介入，回滚代价小的一方

COMMIT;
SELECT '【A3】存活方提交成功' AS step;
