-- 演示8【间隙锁】会话A（RC）：同样锁范围，RC 下不加间隙锁

USE learn_tx;
SET SESSION transaction_isolation = 'READ-COMMITTED';

-- 重置数据
DELETE FROM t_gap WHERE id NOT IN (1, 5, 10);

SELECT '【A1】当前读锁定范围 2~9（RC 下只锁命中的记录 5）' AS step;
BEGIN;
SELECT * FROM t_gap WHERE id BETWEEN 2 AND 9 FOR UPDATE;
-- 只锁真实存在的记录 5；间隙 (1,5)、(5,10) 不锁

SELECT SLEEP(6);

COMMIT;
