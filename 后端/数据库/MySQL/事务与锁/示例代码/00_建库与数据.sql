-- =====================================================================
-- 事务与锁：并发演示的建库脚本（先跑这一个）
-- 运行方式（MySQL 8.0+）：
--   mysql --default-character-set=utf8mb4 -t -u root -p < 00_建库与数据.sql
-- 内容：
--   learn_tx.account  账户表（余额），演示脏读/不可重复读/MVCC/行锁/死锁
--   learn_tx.t_gap    三个稀疏主键，演示间隙锁
-- 说明：
--   每个演示的“会话A”文件开头都会把数据重置回初始状态，可反复运行。
-- =====================================================================

DROP DATABASE IF EXISTS learn_tx;
CREATE DATABASE learn_tx DEFAULT CHARACTER SET utf8mb4;
USE learn_tx;

CREATE TABLE account (
    id      INT PRIMARY KEY,
    name    VARCHAR(20) NOT NULL,
    balance INT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE t_gap (
    id INT PRIMARY KEY,
    v  INT NOT NULL
) ENGINE=InnoDB;

INSERT INTO account VALUES (1, 'Alice', 1000), (2, 'Bob', 1000);

-- 主键故意留洞：1、5、10，中间是两个“间隙”(1,5) 和 (5,10)
INSERT INTO t_gap VALUES (1, 10), (5, 50), (10, 100);

SELECT '建库完成：account 2 行，t_gap 3 行(1/5/10)' AS done;
