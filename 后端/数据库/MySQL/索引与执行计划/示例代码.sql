-- =====================================================================
-- 索引与执行计划：B+ 树、最左前缀、回表与覆盖索引、EXPLAIN、索引失效
-- 运行方式（MySQL 8.0+）：
--   mysql --default-character-set=utf8mb4 -t -u root -p < 示例代码.sql
-- 说明：
--   1. 本脚本自建数据库 learn_index（10 万行用户数据），可反复执行。
--   2. EXPLAIN 的 rows 是“估算值”，不同机器/版本可能略有出入，属正常；
--      关键看 type、key、key_len、Extra 这几列的定性变化。
--   3. 建索引的语句穿插在演示中间：为了让你看到“建索引前后”执行计划的
--      对比，而不是一口气把索引全建好。
-- =====================================================================

DROP DATABASE IF EXISTS learn_index;
CREATE DATABASE learn_index DEFAULT CHARACTER SET utf8mb4;
USE learn_index;

-- ---------------------------------------------------------------------
-- 第 0 节：建表 + 造 10 万行数据
-- ---------------------------------------------------------------------

-- 0-9 的数字表：靠它“交叉相乘”拼出 0~99999 的连续编号，不用存储过程。
CREATE TABLE digits (i INT NOT NULL PRIMARY KEY);
INSERT INTO digits VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

-- 主表：id 自增主键（聚簇索引）；其余列先一律不加索引。
-- remark 列故意给 60 字节，模拟“行里有大字段”，让回表的代价更真实。
CREATE TABLE t_user (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    city       VARCHAR(20)  NOT NULL,
    age        TINYINT UNSIGNED NOT NULL,
    created_at DATETIME     NOT NULL,
    remark     VARCHAR(60)  NOT NULL
) ENGINE=InnoDB;

-- 5 张数字表交叉连接 => 10^5 = 100000 行。
-- 每列的取值都是编号 n 的确定性函数（不用随机数，保证每次可复现）：
--   username   user_000000 ~ user_099999，全局唯一，区分度最高
--   city       10 个城市各约 1 万行（区分度 1/10）
--   age        18~63 共 46 种，各约 2174 行
--   created_at 均匀散布在 2024-01-01 ~ 2025-12-31 两年间
INSERT INTO t_user (username, city, age, created_at, remark)
SELECT CONCAT('user_', LPAD(n, 6, '0')),
       ELT(1 + (n MOD 10), '北京','上海','广州','深圳','杭州','成都','武汉','西安','南京','重庆'),
       18 + (n * 7) MOD 46,
       DATE_ADD('2024-01-01', INTERVAL (n * 37) MOD 730 DAY)
         + INTERVAL (n * 11) MOD 86400 SECOND,
       RPAD('备注内容-', 60, 'x')
FROM (
    SELECT d4.i * 10000 + d3.i * 1000 + d2.i * 100 + d1.i * 10 + d0.i AS n
    FROM digits d0 CROSS JOIN digits d1 CROSS JOIN digits d2
         CROSS JOIN digits d3 CROSS JOIN digits d4
) seq;

SELECT COUNT(*) AS 总行数, COUNT(DISTINCT username) AS 用户名去重,
       COUNT(DISTINCT city) AS 城市数
FROM t_user;

-- 此时表上只有主键索引。
SHOW INDEX FROM t_user;

-- ---------------------------------------------------------------------
-- 第 1 节：没有索引 vs 有索引 —— type 从 ALL 到 ref
-- ---------------------------------------------------------------------

-- 1.1 按用户名精确查一个人。username 上没有索引：
--     type=ALL 全表扫描，估算要扫约 10 万行才能找到 1 行。
EXPLAIN SELECT * FROM t_user WHERE username = 'user_042857';

-- 1.2 给 username 建索引后再看：
--     type=ref（非唯一索引等值查找），key=idx_username，
--     估算扫描行数直接掉到 1。
ALTER TABLE t_user ADD INDEX idx_username (username);
EXPLAIN SELECT * FROM t_user WHERE username = 'user_042857';

-- 1.3 直观感受一次代价（时间受缓存影响，定性看差距即可，数值每次不同）。
--     username 走索引：3~4 层 B+ 树直达，0.00 秒。
--     remark 没有索引也没有别的条件可借力：扫完 10 万行才敢说“没有”。
SELECT * FROM t_user WHERE username = 'user_042857';
SELECT * FROM t_user WHERE remark = '不存在的备注xyz';

-- ---------------------------------------------------------------------
-- 第 2 节：回表与覆盖索引
-- ---------------------------------------------------------------------

-- 2.1 建一个联合索引，本节和后面几节都围绕它展开。
--     列顺序 (city, age, created_at)：等值查最常用的列在前（原因见第 4 节）。
ALTER TABLE t_user ADD INDEX idx_city_age_created (city, age, created_at);

-- 2.2 SELECT * 需要索引里没有的列（remark）：
--     在 idx 里找到 1 万行主键后，还得拿主键回聚簇索引逐行取整行数据
--     ——这就是“回表”，Extra 里没有任何“Using index”字样。
EXPLAIN SELECT * FROM t_user WHERE city = '北京';

-- 2.3 只查索引里就有的列（主键 id 存在于所有二级索引的叶子结点里）：
--     Extra 出现 Using index —— 覆盖索引，一次都不用回表。
EXPLAIN SELECT id, city, age, created_at FROM t_user WHERE city = '北京';

-- 2.4 顺带认识“估算”：EXPLAIN 的 rows 基于采样统计，city='北京' 被估成
--     约 1.9 万行，实际正好 1 万。rows 看量级、别当精确值；要精确就实测。
SELECT COUNT(*) AS 北京真实行数 FROM t_user WHERE city = '北京';

-- ---------------------------------------------------------------------
-- 第 3 节：最左前缀原则
-- ---------------------------------------------------------------------

-- 3.1 匹配连续前缀：city 等值 + age 等值，两列都用上，
--     key_len = 83 = city 82 字节 + age 1 字节（两列都 NOT NULL，没有
--     NULL 标志位；字节怎么算的见知识详解第五节）。
EXPLAIN SELECT id FROM t_user WHERE city = '北京' AND age = 25;

-- 3.2 跳过第一列直接查 age：按教科书结论“违反最左前缀，走不了索引”，
--     但 MySQL 8.0.13+ 有 Skip Scan 优化：第一列 city 只有 10 个值，优化器
--     干脆枚举 10 个 city 分别钻进索引，Extra 出现 Using index for skip scan。
--     注意代价：估算仍要摸约 1 万条索引记录——救了命，但没白捡。
--     结论：能用不代表该依赖，写 SQL 仍应带上前缀列（区分度也更高）。
EXPLAIN SELECT id FROM t_user WHERE age = 25;

-- 3.3 前导模糊匹配：'北%' 能确定从索引哪一段开始扫（type=range）；
--     '%京' 开头不确定，没法定位，两条查询对照看 type 与 rows：
--       查所有列(*)  → 只能全表扫描（type=ALL，约 10 万行）
--       只查 id      → 优化器退而求其次：整棵索引从头扫到尾（type=index，
--                      Extra: Using index——索引比整表窄，扫它更便宜）
--     共同点：都失去了“直接定位”，都要看约 10 万条记录。
EXPLAIN SELECT * FROM t_user WHERE city LIKE '%京';
EXPLAIN SELECT id FROM t_user WHERE city LIKE '%京';
EXPLAIN SELECT id FROM t_user WHERE city LIKE '北%';

-- 3.4 范围之后断链：city 等值、age 用了范围(>)。
--     定位只用到 city + age 两列（key_len = 83，age 作为范围端点参与），
--     第三列 created_at 不再参与定位，只能在扫描时“边走边筛”：
--       只查 id（覆盖）   → Extra: Using where; Using index，索引里筛完即止
--       查所有列（不覆盖）→ Extra: Using index condition = 索引下推 ICP：
--                           created_at 的过滤下推到索引层完成，
--                           不满足的行连回表都省了
EXPLAIN SELECT id FROM t_user
WHERE city = '北京' AND age > 30 AND created_at < '2025-01-01';
EXPLAIN SELECT * FROM t_user
WHERE city = '北京' AND age > 30 AND created_at < '2025-01-01';

-- 3.5 对照：age 改用等值后，created_at 可以接着做范围定位，
--     key_len 变成 88（82 + 1 + created_at 5 字节），三列都参与定位。
EXPLAIN SELECT id FROM t_user
WHERE city = '北京' AND age = 25 AND created_at < '2025-01-01';

-- ---------------------------------------------------------------------
-- 第 4 节：索引失效的四种经典写法
-- ---------------------------------------------------------------------

-- 4.1 对索引列用函数：索引存的是原始值，LEFT() 之后 optimizer 对不上号。
--     （等价改写：username LIKE 'user_0428%' 就能走 idx_username。）
EXPLAIN SELECT * FROM t_user WHERE LEFT(username, 9) = 'user_0428';

-- 4.2 隐式类型转换：username 是 VARCHAR，却拿数字 42857 来比。
--     MySQL 把每一行的 username 转成数字再比（'user_042857' -> 0），
--     转换发生在列上 => 索引失效（type=ALL），而且结果全错（空集）。
--     下方 SHOW WARNINGS 能看到 1292 截断警告。
EXPLAIN SELECT * FROM t_user WHERE username = 42857;
SELECT * FROM t_user WHERE username = 42857;
SHOW WARNINGS;

-- 4.3 OR 两侧不是都有索引：city 有索引、age 单独没有索引。
--     OR 要求“两边都能走索引”才可能免于全表扫描，一边塌了整体就塌，
--     type=ALL。（真有这种查询：给 age 也建索引，或改写成 UNION ALL。）
EXPLAIN SELECT * FROM t_user WHERE city = '北京' OR age = 25;

-- 4.4 否定条件：<> 命中 10 个城市中的 9 个、约 9 万行，
--     顺着索引找到 9 万个主键再回表 9 万次，还不如直接顺序扫全表，
--     优化器果断放弃索引（type=ALL）。索引不是万能的：
--     选择性太低时，索引就不再是捷径。
EXPLAIN SELECT * FROM t_user WHERE city <> '北京';

-- ---------------------------------------------------------------------
-- 第 5 节：ORDER BY 与 filesort
-- ---------------------------------------------------------------------

-- 5.1 created_at 上没有索引：按时间排序取前 5，
--     Extra 出现 Using filesort——把符合条件的行取出来单独排序。
EXPLAIN SELECT * FROM t_user ORDER BY created_at LIMIT 5;

-- 5.2 建索引后：B+ 树叶子本身就是按 created_at 有序的，
--     顺着索引读前 5 行即得，filesort 消失（type=index、rows=5）。
--     ※ 真实踩坑现场：如果刚建完索引 EXPLAIN 仍是老计划，多半是
--       统计信息还没刷新——ANALYZE TABLE 让优化器重新采样一次再看。
ALTER TABLE t_user ADD INDEX idx_created (created_at);
ANALYZE TABLE t_user;
EXPLAIN SELECT * FROM t_user ORDER BY created_at LIMIT 5;

-- 5.3 联合索引内的排序：WHERE city 定位到一段后，段内 age 本来就有序，
--     ORDER BY age 不需要额外排序；一旦排序方向/顺序与索引不一致
--     （先 created_at 后 age），就又出现 filesort。
EXPLAIN SELECT id FROM t_user WHERE city = '北京' ORDER BY age;
EXPLAIN SELECT id FROM t_user WHERE city = '北京' ORDER BY created_at, age;

-- ---------------------------------------------------------------------
-- 第 6 节：最终的家底与速查
-- ---------------------------------------------------------------------

-- 四个索引：主键（聚簇）+ 三个二级索引。
SHOW INDEX FROM t_user;

-- 估算表数据量：二级索引有 3 份，插入/更新都要维护，这就是索引的代价。
SELECT COUNT(*) AS 行数 FROM t_user;

SELECT '索引与执行计划 演示结束' AS done;
