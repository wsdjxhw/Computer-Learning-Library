-- =====================================================================
-- SQL 进阶：JOIN、子查询、GROUP BY、窗口函数、CTE 与 UNION
-- 运行方式（MySQL 8.0+）：
--   mysql --default-character-set=utf8mb4 -t -u root -p < 示例代码.sql
-- 说明：
--   1. 本脚本自建数据库 learn_sql，可反复执行（开头会先删掉旧库）。
--   2. 数据取自经典的 scott 示例库（14 名员工、4 个部门、5 档工资级别），
--      数据量小但覆盖了所有教学场景：外键无匹配(40 部门)、自引用(经理)、
--      并列值(两对同薪)、NULL(无经理的 KING)。
--   3. 第 5 节包含一条“预期会报错”的语句（ONLY_FULL_GROUP_BY），
--      用 --force 参数运行可继续往下执行；交互式粘贴则会中断，属正常。
-- =====================================================================

DROP DATABASE IF EXISTS learn_sql;
CREATE DATABASE learn_sql DEFAULT CHARACTER SET utf8mb4;
USE learn_sql;

-- ---------------------------------------------------------------------
-- 第 0 节：建表与数据
-- ---------------------------------------------------------------------

-- 部门表：40 号部门 OPERATIONS 故意没有任何员工，专门用来演示外连接
CREATE TABLE dept (
    deptno INT PRIMARY KEY,
    dname  VARCHAR(14) NOT NULL,
    loc    VARCHAR(13) NOT NULL
);

-- 员工表：mgr 指向本表 empno（自引用），PRESIDENT KING 没有经理(mgr 为 NULL)
CREATE TABLE emp (
    empno   INT PRIMARY KEY,
    ename   VARCHAR(10) NOT NULL,
    job     VARCHAR(9)  NOT NULL,
    mgr     INT,                    -- 可为 NULL：KING 之上无人
    hiredate DATE NOT NULL,
    sal     DECIMAL(7,2) NOT NULL,
    deptno  INT NOT NULL
);

-- 工资级别表：用来演示“非等值连接”（BETWEEN 而不是 =）
CREATE TABLE salgrade (
    grade INT PRIMARY KEY,
    losal INT NOT NULL,
    hisal INT NOT NULL
);

INSERT INTO dept VALUES
(10,'ACCOUNTING','NEW YORK'),
(20,'RESEARCH','DALLAS'),
(30,'SALES','CHICAGO'),
(40,'OPERATIONS','BOSTON');

INSERT INTO emp VALUES
(7369,'SMITH','CLERK',7902,'1980-12-17', 800.00,20),
(7499,'ALLEN','SALESMAN',7698,'1981-02-20',1600.00,30),
(7521,'WARD','SALESMAN',7698,'1981-02-22',1250.00,30),
(7566,'JONES','MANAGER',7839,'1981-04-02',2975.00,20),
(7654,'MARTIN','SALESMAN',7698,'1981-09-28',1250.00,30),
(7698,'BLAKE','MANAGER',7839,'1981-05-01',2850.00,30),
(7782,'CLARK','MANAGER',7839,'1981-06-09',2450.00,10),
(7788,'SCOTT','ANALYST',7566,'1987-04-19',3000.00,20),
(7839,'KING','PRESIDENT',NULL,'1981-11-17',5000.00,10),
(7844,'TURNER','SALESMAN',7698,'1981-09-08',1500.00,30),
(7876,'ADAMS','CLERK',7788,'1987-05-23',1100.00,20),
(7900,'JAMES','CLERK',7698,'1981-12-03', 950.00,30),
(7902,'FORD','ANALYST',7566,'1981-12-03',3000.00,20),
(7934,'MILLER','CLERK',7782,'1982-01-23',1300.00,10);

INSERT INTO salgrade VALUES
(1,700,1200),(2,1201,1400),(3,1401,2000),(4,2001,3000),(5,3001,9999);

-- ---------------------------------------------------------------------
-- 第 1 节：JOIN 的四种面孔
-- ---------------------------------------------------------------------

-- 1.1 内连接：只保留两表都能匹配上的行。
--     40 号部门没有员工，emp 中也没有 40 的员工行，所以结果里看不到 40。
SELECT d.dname, e.ename, e.sal
FROM dept d JOIN emp e ON d.deptno = e.deptno
ORDER BY d.dname, e.empno;

-- 1.2 左连接：保留左表全部行，右表匹配不上的补 NULL。
--     最后一行 OPERATIONS + NULL 就是“40 部门没有员工”被显式暴露出来。
SELECT d.dname, e.ename, e.sal
FROM dept d LEFT JOIN emp e ON d.deptno = e.deptno
ORDER BY d.deptno, e.empno;

-- 1.3 右连接：与左连接对称，保留右表全部行（效果上等价于把两表换个位置做左连接）。
SELECT d.dname, e.ename
FROM emp e RIGHT JOIN dept d ON d.deptno = e.deptno
ORDER BY d.deptno, e.empno;

-- 1.4 自连接：一张表当两张表用。emp.mgr 指向 emp.empno，
--     “查每个员工及其经理”就是把 emp 和 emp 自己连一次。
--     KING 没有经理，内连接下他被“丢掉”——共 13 行而不是 14 行。
SELECT e.ename AS 员工, m.ename AS 领导
FROM emp e JOIN emp m ON e.mgr = m.empno
ORDER BY e.empno;

-- 1.5 非等值连接：连接条件不必是等号。
--     每个人的工资落在 salgrade 的哪个区间，就得到哪个级别。
SELECT e.ename, e.sal, s.grade
FROM emp e JOIN salgrade s ON e.sal BETWEEN s.losal AND s.hisal
ORDER BY e.sal;

-- ---------------------------------------------------------------------
-- 第 2 节：LEFT JOIN 的经典陷阱——条件写在 ON 还是 WHERE
-- ---------------------------------------------------------------------

-- 2.1 条件写在 ON 里：先按“部门=部门 且 工资>2000”配对，
--     配不上的左表行照样保留（部门 40 的 NULL 行还在，共 7 行）。
--     语义：先决定“怎么连”，再展示全部左表行。
SELECT d.dname, e.ename, e.sal
FROM dept d LEFT JOIN emp e
     ON d.deptno = e.deptno AND e.sal > 2000
ORDER BY d.deptno, e.empno;

-- 2.2 同样的条件写在 WHERE 里：连接照常发生，再对连接结果过滤。
--     NULL > 2000 的结果是 UNKNOWN（不是 TRUE），整行被过滤掉，
--     部门 40 消失，左连接“退化”成内连接——共 6 行。
--     语义：先连完，再对“最终结果”过滤。
SELECT d.dname, e.ename, e.sal
FROM dept d LEFT JOIN emp e ON d.deptno = e.deptno
WHERE e.sal > 2000
ORDER BY d.deptno, e.empno;

-- ---------------------------------------------------------------------
-- 第 3 节：子查询
-- ---------------------------------------------------------------------

-- 3.1 标量子查询：子查询只返回一行一列，可以当一个“算出来的常量”用。
--     全公司平均工资是 2073.21，拿它和每个人的工资做对比。
SELECT ename, sal,
       ROUND((SELECT AVG(sal) FROM emp), 2) AS 全公司平均,
       sal - (SELECT AVG(sal) FROM emp)     AS 与平均差值
FROM emp
ORDER BY sal DESC
LIMIT 5;

-- 3.2 相关子查询：子查询里引用了外层的 e.deptno，
--     相当于对每一行员工都问一遍“你比你们部门的平均高吗？”
--     MySQL 8.x 会把它优化成类似 EXISTS 的执行方式。
SELECT e.ename, e.deptno, e.sal,
       (SELECT ROUND(AVG(sal),2) FROM emp WHERE deptno = e.deptno) AS 部门平均
FROM emp e
WHERE e.sal > (SELECT AVG(sal) FROM emp WHERE deptno = e.deptno)
ORDER BY e.deptno, e.sal DESC;

-- 3.3 IN：子查询结果当集合用——“在……里面”。
--     谁的工资正好等于本部门的最高工资？
SELECT ename, deptno, sal
FROM emp
WHERE sal IN (SELECT MAX(sal) FROM emp GROUP BY deptno)
ORDER BY deptno;

-- 3.4 NOT IN 的 NULL 陷阱（重点！）
--     “找出没有下属的员工”，直觉写法：empno 不在 mgr 集合里。
--     但 mgr 集合里含有 NULL（KING 没有经理），
--     x NOT IN (..., NULL) 等价于 x<>a AND x<>b AND x<>NULL，
--     只要与 NULL 比较就得到 UNKNOWN，永远凑不出 TRUE —— 结果是空集！
SELECT ename
FROM emp
WHERE empno NOT IN (SELECT mgr FROM emp);

-- 3.5 正确姿势：NOT EXISTS 逐行判断“是否存在”，不涉及集合比较，
--     因此不受 NULL 影响 —— 8 名没有下属的“光杆”员工被正确找出
--     （KING 不在其中：JONES/BLAKE/CLARK 都向他汇报，他是有下属的）。
SELECT e.ename
FROM emp e
WHERE NOT EXISTS (SELECT 1 FROM emp m WHERE m.mgr = e.empno);

-- ---------------------------------------------------------------------
-- 第 4 节：GROUP BY、聚合与 WHERE/HAVING
-- ---------------------------------------------------------------------

-- 4.1 五个聚合函数一把梭：分组后每组出一行。
SELECT deptno,
       COUNT(*)      AS 人数,
       ROUND(AVG(sal),2) AS 平均工资,
       MAX(sal)      AS 最高,
       MIN(sal)      AS 最低,
       SUM(sal)      AS 工资总额
FROM emp
GROUP BY deptno
ORDER BY deptno;

-- 4.2 WHERE 与 HAVING 的分工：
--     WHERE 在分组前过滤原始行（不能碰聚合结果）；
--     HAVING 在分组后过滤“组”（可以用聚合结果）。
--     下面：先丢掉 CLERK，再只留平均工资 > 2000 的部门。
SELECT deptno, COUNT(*) AS 人数, ROUND(AVG(sal),2) AS 平均工资
FROM emp
WHERE job <> 'CLERK'
GROUP BY deptno
HAVING AVG(sal) > 2000
ORDER BY deptno;

-- 4.3 ONLY_FULL_GROUP_BY（MySQL 5.7.5+ 默认开启）：
--     SELECT 里出现的非聚合列必须出现在 GROUP BY 里。
--     下面这条在老版本里能跑且“随机挑一个 ename”，现在是直接报错。
--     ※ 预期报错：ERROR 1055 —— 用 mysql --force 运行本脚本可继续。
SELECT deptno, ename FROM emp GROUP BY deptno;

-- 4.4 想要“每组工资最高的人”，正确做法见第 5 节窗口函数，或相关子查询。
SELECT e.deptno, e.ename, e.sal
FROM emp e
WHERE e.sal = (SELECT MAX(sal) FROM emp WHERE deptno = e.deptno)
ORDER BY e.deptno;

-- ---------------------------------------------------------------------
-- 第 5 节：窗口函数——分组但不折叠
-- ---------------------------------------------------------------------

-- 5.1 三兄弟对比：ROW_NUMBER / RANK / DENSE_RANK 都按 sal 降序排名，
--     区别只在并列(3000 与 3000、1250 与 1250)怎么处理：
--       ROW_NUMBER：硬排出 1、2、3……并列也分先后（这里用 empno 决胜）
--       RANK：并列同名次，占用名次号（1、2、2、4、……）
--       DENSE_RANK：并列同名次，不占用名次号（1、2、2、3、……）
--     注意别名 row_number/rank/dense_rank 中的 row_number 和 rank 是
--     MySQL 8 的保留字（窗口函数名），当别名用必须加反引号。
SELECT ename, sal,
       ROW_NUMBER() OVER (ORDER BY sal DESC, empno) AS `row_number`,
       RANK()       OVER (ORDER BY sal DESC)        AS `rank`,
       DENSE_RANK() OVER (ORDER BY sal DESC)        AS `dense_rank`
FROM emp
ORDER BY sal DESC, empno;

-- 5.2 PARTITION BY：先按部门“分区”，每个区各自排名——
--     一条语句同时拿到三个部门的工资榜首。
SELECT deptno, ename, sal,
       ROW_NUMBER() OVER (PARTITION BY deptno ORDER BY sal DESC, empno) AS 部门内名次
FROM emp
ORDER BY deptno, 部门内名次;

-- 经典应用：取每组第一名 —— 排名后在外层套一层查询过滤名次 = 1。
SELECT *
FROM (SELECT deptno, ename, sal,
             ROW_NUMBER() OVER (PARTITION BY deptno ORDER BY sal DESC, empno) AS rn
      FROM emp) t
WHERE rn = 1
ORDER BY deptno;

-- 5.3 累计求和：默认帧是“从分区头到当前行（含并列行）”，
--     注意 JAMES 与 FORD 同一天入职：默认帧是 RANGE，“同位”的行一起算，
--     两人显示的累计值相同，都是 23625.00（19675 + 950 + 3000）。
SELECT ename, hiredate, sal,
       SUM(sal) OVER (ORDER BY hiredate) AS 累计工资
FROM emp
ORDER BY hiredate, ename;

-- 5.4 LAG：不连接、不分组，直接取“按入职顺序的上一行”，做环比/同比很方便。
--     细节：窗口内 ORDER BY 只有 hiredate 时，同一天的 FORD/JAMES 先后顺序
--     是未定义的（输出里 FORD 的“上一人”是 JAMES 的 950，正是这个原因）；
--     需要确定性结果时，窗口排序应补上第二列，如 ORDER BY hiredate, empno。
SELECT ename, hiredate, sal,
       LAG(sal) OVER (ORDER BY hiredate) AS 上一人工资,
       sal - LAG(sal) OVER (ORDER BY hiredate) AS 差值
FROM emp
ORDER BY hiredate, ename;

-- ---------------------------------------------------------------------
-- 第 6 节：CTE（WITH 子句）与 UNION
-- ---------------------------------------------------------------------

-- 6.1 CTE 把“中间结果”命名复用，长查询不再一层层套娃：
--     先算每个部门的平均工资，再回头逐人对比。
WITH dept_avg AS (
    SELECT deptno, AVG(sal) AS avg_sal
    FROM emp
    GROUP BY deptno
)
SELECT e.ename, e.deptno, e.sal, ROUND(a.avg_sal, 2) AS 部门平均
FROM emp e JOIN dept_avg a ON e.deptno = a.deptno
WHERE e.sal > a.avg_sal
ORDER BY e.deptno, e.sal DESC;

-- 6.2 UNION 去重、UNION ALL 保留全部：
--     10 号部门 3 人、20 号部门 5 人：UNION 去掉重复后剩 4 种岗位；
--     UNION ALL 则 8 行原样拼接（结果行数一目了然地暴露了“谁被去掉”）。
SELECT job FROM emp WHERE deptno = 10
UNION
SELECT job FROM emp WHERE deptno = 20;

SELECT job FROM emp WHERE deptno = 10
UNION ALL
SELECT job FROM emp WHERE deptno = 20;

-- ---------------------------------------------------------------------
-- 第 7 节：NULL 的三值逻辑与 COUNT 的三种数法
-- ---------------------------------------------------------------------

-- 7.1 NULL 与任何值比较都得到 UNKNOWN，不是 FALSE：
--     所以“mgr = NULL”永远查不出 KING，必须用 IS NULL。
SELECT NULL = NULL AS 等号判断,
       NULL IS NULL AS is_null,
       NULL <=> NULL AS 安全等于,
       0 = NULL AS 与0比较;

SELECT ename, mgr FROM emp WHERE mgr = NULL;   -- 错误姿势：永远空集
SELECT ename, mgr FROM emp WHERE mgr IS NULL;  -- 正确姿势：KING

-- 7.2 COUNT 的口径：
--     COUNT(*)    数行数（14）
--     COUNT(mgr)  数这一列“非 NULL”的个数（13，KING 的 mgr 是 NULL）
--     COUNT(DISTINCT job) 数去重后的非 NULL 值（5 种岗位）
SELECT COUNT(*) AS 全部行数,
       COUNT(mgr) AS mgr非空数,
       COUNT(DISTINCT job) AS 岗位数
FROM emp;

SELECT 'SQL进阶 演示结束' AS done;
