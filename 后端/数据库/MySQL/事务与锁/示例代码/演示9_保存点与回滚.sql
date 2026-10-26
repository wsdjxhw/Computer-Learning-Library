-- 演示9【SAVEPOINT】单会话：事务内的“存档点”
-- 运行：单个连接执行即可，不需要 B

USE learn_tx;

-- 重置
UPDATE account SET balance = 1000 WHERE id = 1;

SELECT '【1】开启事务：先花 300' AS step;
BEGIN;
UPDATE account SET balance = balance - 300 WHERE id = 1;

SELECT '【2】设置存档点 sp1，再花 700（花光了）' AS step;
SAVEPOINT sp1;
UPDATE account SET balance = balance - 700 WHERE id = 1;
SELECT balance AS 花光了吗 FROM account WHERE id = 1;     -- 0

SELECT '【3】ROLLBACK TO sp1：只回到存档点，事务没结束' AS step;
ROLLBACK TO sp1;
SELECT balance AS 回到存档点 FROM account WHERE id = 1;   -- 700（第一次消费保留）

SELECT '【4】整体 ROLLBACK：全部撤销' AS step;
ROLLBACK;
SELECT balance AS 全部撤销 FROM account WHERE id = 1;     -- 1000
-- 结论：ROLLBACK TO 只撤到存档点、事务继续；ROLLBACK 撤销整个事务。
