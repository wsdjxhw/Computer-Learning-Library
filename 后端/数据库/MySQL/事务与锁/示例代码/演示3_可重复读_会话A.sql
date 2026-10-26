-- 演示3【可重复读】会话A：时间线与演示2完全相同
-- 区别只在 B 的隔离级别是 READ-REPEATABLE（InnoDB 默认）

USE learn_tx;

-- 重置数据
UPDATE account SET balance = 1000 WHERE id <= 2;
DELETE FROM account WHERE id > 2;

SELECT SLEEP(2);

SELECT '【A1】提交：Alice +500，并插入 Carol' AS step;
BEGIN;
UPDATE account SET balance = balance + 500 WHERE id = 1;
INSERT INTO account VALUES (3, 'Carol', 1000);
COMMIT;
