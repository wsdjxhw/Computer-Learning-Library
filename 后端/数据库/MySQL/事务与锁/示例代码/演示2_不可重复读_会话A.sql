-- 演示2【不可重复读 + 幻读】会话A：在 B 的两次读之间提交一次修改和一次插入
-- 隔离级别：READ-COMMITTED（读已提交，B 用这个级别观察）

USE learn_tx;

-- 重置数据
UPDATE account SET balance = 1000 WHERE id <= 2;
DELETE FROM account WHERE id > 2;

SELECT SLEEP(2);            -- 等 B 完成第一次读

SELECT '【A1】提交：Alice +500，并插入新用户 Carol' AS step;
BEGIN;
UPDATE account SET balance = balance + 500 WHERE id = 1;
INSERT INTO account VALUES (3, 'Carol', 1000);
COMMIT;
