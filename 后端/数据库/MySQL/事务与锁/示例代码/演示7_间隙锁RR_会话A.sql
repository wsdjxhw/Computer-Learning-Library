-- 演示7【间隙锁】会话A（RR）：锁住 (1,10) 之间的记录和“空隙”

USE learn_tx;

-- 重置数据（t_gap 主键 1、5、10）
DELETE FROM t_gap WHERE id NOT IN (1, 5, 10);

SELECT '【A1】当前读锁定范围 2~9（RR 下 = Next-Key Lock：记录+间隙）' AS step;
BEGIN;
SELECT * FROM t_gap WHERE id BETWEEN 2 AND 9 FOR UPDATE;
-- 锁住：记录 5，以及间隙 (1,5)、(5,10)——不存在的 id=7 也在射程内

SELECT SLEEP(6);

COMMIT;
