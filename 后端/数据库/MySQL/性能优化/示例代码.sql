-- =====================================================================
-- 性能优化：慢查询定位、深分页、COUNT、批量写入
-- 运行方式（MySQL 8.0+）：
--   mysql --default-character-set=utf8mb4 -t -u root -p < 示例代码.sql
-- 说明：
--   1. 自建 learn_perf 库（10 万行），可反复执行。
--   2. 涉及 SET GLOBAL（慢日志开关）：需要高权限账号；生产环境这类
--      参数由 DBA 统一管理，不要在业务代码里改。
--   3. 所有时间均为真实运行输出，受机器与缓存影响，每次会有出入；
--      看量级和相对差距即可。
-- =====================================================================

DROP DATABASE IF EXISTS learn_perf;
CREATE DATABASE learn_perf DEFAULT CHARACTER SET utf8mb4;
USE learn_perf;

-- ---------------------------------------------------------------------
-- 第 0 节：造 10 万行数据（与“索引与执行计划”同套路：数字表交叉相乘）
-- ---------------------------------------------------------------------
CREATE TABLE digits (i INT NOT NULL PRIMARY KEY);
INSERT INTO digits VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

CREATE TABLE t_user (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) NOT NULL,
    city       VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    remark     VARCHAR(60) NOT NULL
) ENGINE=InnoDB;

INSERT INTO t_user (username, city, created_at, remark)
SELECT CONCAT('user_', LPAD(n, 6, '0')),
       ELT(1 + (n MOD 10), '北京','上海','广州','深圳','杭州','成都','武汉','西安','南京','重庆'),
       DATE_ADD('2024-01-01', INTERVAL (n * 37) MOD 730 DAY)
         + INTERVAL (n * 11) MOD 86400 SECOND,
       RPAD('备注内容-', 60, 'x')
FROM (
    SELECT d4.i * 10000 + d3.i * 1000 + d2.i * 100 + d1.i * 10 + d0.i AS n
    FROM digits d0 CROSS JOIN digits d1 CROSS JOIN digits d2
         CROSS JOIN digits d3 CROSS JOIN digits d4
) seq;

ALTER TABLE t_user ADD INDEX idx_created (created_at);
ANALYZE TABLE t_user;
SELECT COUNT(*) AS 总行数 FROM t_user;

-- ---------------------------------------------------------------------
-- 第 1 节：慢查询日志——找到“谁在慢”
-- ---------------------------------------------------------------------

-- 1.1 打开慢日志，阈值 0.1 秒（生产常用 1~3 秒；演示调低方便捕捉）
--     日志默认写到 datadir/主机名-slow.log
SET GLOBAL slow_query_log = ON;
-- 注意：SET GLOBAL 只对“之后新建的连接”生效，本脚本在同一个连接里跑完，
-- 所以还要 SET SESSION 让当前连接立即生效（生产上也是如此：改完全局，
-- 老连接用的仍是旧阈值）。
SET GLOBAL long_query_time = 0.1;
SET SESSION long_query_time = 0.1;
SHOW VARIABLES LIKE 'slow_query_log%';
SHOW VARIABLES LIKE 'long_query_time';
SELECT @@datadir AS 日志所在目录;

-- 1.2 现场制造一条慢查询：SLEEP(0.2) 本身耗时 0.2 秒 > 0.1 秒，进日志。
--     （第 0 节造数的那条 INSERT 虽然也慢，但它执行在开启慢日志之前——
--       慢日志只记录开启之后执行的语句，这也是排查时“日志里怎么没有”的常见原因）
SELECT SLEEP(0.2) AS 制造一条慢查询;

-- 慢日志是文本，可直接打开看：结尾附了本机实测的日志节选。
-- 生产排查顺序：慢日志找 SQL → EXPLAIN 看计划 → 对症下药（见第 2~4 节）。

-- ---------------------------------------------------------------------
-- 第 2 节：深分页——LIMIT 100000, 20 为什么慢，怎么救
-- ---------------------------------------------------------------------

-- 2.1 直觉写法：翻到第 5000 页。
--     它必须老老实实“越过”前 100000 行才能取 20 行——
--     扫描 100020 行、丢弃 100000 行，浪费全部在 offset 上。
EXPLAIN SELECT * FROM t_user ORDER BY id LIMIT 100000, 20;
SET @t0 = NOW(6);
SELECT * FROM t_user ORDER BY id LIMIT 100000, 20;
SELECT ROUND(TIMESTAMPDIFF(MICROSECOND, @t0, NOW(6)) / 1000000, 4) AS 写法1_直觉写法_耗时_秒;

-- 2.2 书签法（记下上一页末尾的 id，下一页从它继续）。
--     WHERE id > 100000 让 B+ 树直接定位到第 100001 行：只扫 20 行。
--     代价：要求排序键连续/有序且无其他过滤条件，且不能跳页。
--     （EXPLAIN 的 rows 是估算值，这里估成 1；看 type=range、key=PRIMARY 即可）
EXPLAIN SELECT * FROM t_user WHERE id > 100000 ORDER BY id LIMIT 20;
SET @t0 = NOW(6);
SELECT * FROM t_user WHERE id > 100000 ORDER BY id LIMIT 20;
SELECT ROUND(TIMESTAMPDIFF(MICROSECOND, @t0, NOW(6)) / 1000000, 4) AS 写法2_书签法_耗时_秒;

-- 2.3 延迟关联（通用解）：先用覆盖索引把目标 20 个主键找出来，
--     再用这 20 个主键回表取整行——offset 仍然要扫，但扫的是
--     “只有主键的窄索引”，比扫整行便宜一个量级。
EXPLAIN
SELECT u.* FROM t_user u
JOIN (SELECT id FROM t_user ORDER BY id LIMIT 100000, 20) t ON u.id = t.id;
SET @t0 = NOW(6);
SELECT u.* FROM t_user u
JOIN (SELECT id FROM t_user ORDER BY id LIMIT 100000, 20) t ON u.id = t.id;
SELECT ROUND(TIMESTAMPDIFF(MICROSECOND, @t0, NOW(6)) / 1000000, 4) AS 写法3_延迟关联_耗时_秒;

-- 结论：offset 小（前几页）直觉写法没问题；越往后越要用 2.2/2.3。
-- 10 万行的表差距还不吓人，表到千万行、offset 到百万级时，
-- 直觉写法要几秒，书签法/延迟关联仍是毫秒级——恶化和 offset 成正比。

-- ---------------------------------------------------------------------
-- 第 3 节：COUNT 一张大表
-- ---------------------------------------------------------------------

-- 3.1 精确 COUNT(*)：InnoDB 没存“总行数”（事务各看各的），
--     只能把一棵索引树从头扫到尾。10 万行尚快，亿级表就是灾难。
SELECT COUNT(*) AS 精确计数 FROM t_user;

-- 3.2 只要量级不要精确时，用统计信息：瞬间返回，误差可能百分之几。
SELECT TABLE_ROWS AS 统计行数 FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'learn_perf' AND TABLE_NAME = 't_user';
-- （EXPLAIN 的 rows 列也是这个来源的估算，见“索引与执行计划”篇 2.4）

-- ---------------------------------------------------------------------
-- 第 4 节：批量写入的三种姿势（5000 行对比）
-- ---------------------------------------------------------------------
DELIMITER ;;
-- 姿势1：循环 5000 次单条 INSERT，autocommit 默认开——每条一个事务、
--        每次事务提交都要等 redo log 落盘（fsync），磁盘成为瓶颈。
CREATE PROCEDURE p_insert_one_by_one()
BEGIN
    DECLARE i INT DEFAULT 0;
    WHILE i < 5000 DO
        INSERT INTO t_loadtest (v) VALUES (i);
        SET i = i + 1;
    END WHILE;
END;;
-- 姿势2：同样 5000 条，但包在一个事务里——只有最后一次提交落盘。
CREATE PROCEDURE p_insert_one_tx()
BEGIN
    DECLARE i INT DEFAULT 0;
    START TRANSACTION;
    WHILE i < 5000 DO
        INSERT INTO t_loadtest (v) VALUES (i);
        SET i = i + 1;
    END WHILE;
    COMMIT;
END;;
DELIMITER ;

CREATE TABLE t_loadtest (id BIGINT AUTO_INCREMENT PRIMARY KEY, v INT) ENGINE=InnoDB;

-- 三档真实计时（秒）
SET @t0 = NOW(6);
CALL p_insert_one_by_one();
SELECT ROUND(TIMESTAMPDIFF(MICROSECOND, @t0, NOW(6)) / 1000000, 3) AS 姿势1_逐条autocommit_秒;

TRUNCATE t_loadtest;
SET @t0 = NOW(6);
CALL p_insert_one_tx();
SELECT ROUND(TIMESTAMPDIFF(MICROSECOND, @t0, NOW(6)) / 1000000, 3) AS 姿势2_单事务5000条_秒;

TRUNCATE t_loadtest;
SET @t0 = NOW(6);
INSERT INTO t_loadtest (v)
SELECT d3.i * 1000 + d2.i * 100 + d1.i * 10 + d0.i
FROM digits d0 CROSS JOIN digits d1 CROSS JOIN digits d2 CROSS JOIN digits d3
WHERE (d3.i * 1000 + d2.i * 100 + d1.i * 10 + d0.i) < 5000;
SELECT ROUND(TIMESTAMPDIFF(MICROSECOND, @t0, NOW(6)) / 1000000, 3) AS 姿势3_一条多值5000行_秒,
       COUNT(*) AS 行数 FROM t_loadtest;

-- 结论：逐条提交 vs 合并事务 vs 多值单条，差距通常在 1~2 个数量级。
-- 应用层对应：别在循环里逐条 save()；MyBatis/JDBC 都有 batch 写法；
-- 超大批量导入可临时 SET unique_checks=0 / foreign_key_checks=0 再恢复。

-- ---------------------------------------------------------------------
-- 第 5 节：收尾
-- ---------------------------------------------------------------------
-- （演示结束；慢日志保持开启着也无妨，测试实例随手关：）
SET GLOBAL slow_query_log = OFF;
SELECT '性能优化 演示结束' AS done;

-- ---------------------------------------------------------------------
-- 附：本机实测的慢日志节选（tianxuan-slow.log，节选自末尾运行）
--     两条正好对应 1.2 的 SLEEP 和第 4 节的姿势1：
--     Query_time 总耗时、Lock_time 等锁时间、Rows_examined 扫描行数；
--     存储过程按整条 CALL 计时（4.8 秒 = 5000 次逐条提交的总和）。
-- ---------------------------------------------------------------------
-- # Time: 2026-10-26T07:52:13.530192Z
-- # User@Host: root[root] @ localhost [127.0.0.1]  Id:    31
-- # Query_time: 0.204397  Lock_time: 0.000000 Rows_sent: 1  Rows_examined: 1
-- SET timestamp=1793001133;
-- SELECT SLEEP(0.2) AS 制造一条慢查询;
-- # Time: 2026-10-26T07:52:18.414392Z
-- # User@Host: root[root] @ localhost [127.0.0.1]  Id:    31
-- # Query_time: 4.823975  Lock_time: 0.000003 Rows_sent: 0  Rows_examined: 0
-- SET timestamp=1793001138;
-- CALL p_insert_one_by_one();
