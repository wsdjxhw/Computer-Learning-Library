-- 演示7【间隙锁】会话B（RR）：往“空隙”里插一行，被拦下

USE learn_tx;
SET SESSION innodb_lock_wait_timeout = 3;

SELECT SLEEP(1.5);

SELECT '【B1】INSERT id=7——它落在 A 锁定的间隙 (5,10) 里' AS step;
INSERT INTO t_gap VALUES (7, 70);
-- ↑ 阻塞约 3 秒后 ERROR 1205：间隙被锁，插不进去。
--   5 和 7 之间没有任何“已存在的行”，锁的是“位置”——这就是间隙锁。

SELECT '【B2】改用 RC 再看同一幕 → 演示8' AS step;
