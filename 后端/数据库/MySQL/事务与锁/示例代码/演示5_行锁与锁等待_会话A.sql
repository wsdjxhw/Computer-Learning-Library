-- 演示5【行锁与锁等待】会话A：锁住 id=1 不放

USE learn_tx;

-- 重置数据
UPDATE account SET balance = 1000 WHERE id <= 2;

SELECT '【A1】开启事务，UPDATE id=1（拿到 id=1 的行锁）' AS step;
BEGIN;
UPDATE account SET balance = balance + 100 WHERE id = 1;

SELECT SLEEP(6);            -- 拿着锁 6 秒不撒手

COMMIT;
SELECT '【A2】提交，释放锁' AS step;
