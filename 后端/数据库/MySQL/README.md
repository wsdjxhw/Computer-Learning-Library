# MySQL

存放 MySQL 数据库的学习内容。示例统一为可直接执行的 `.sql` 脚本（自建 `learn_sql` / `learn_index` 演示库，可反复执行），在 MySQL 8.0+ 上运行：

```powershell
mysql --default-character-set=utf8mb4 -t -u root -p < 知识点目录/示例代码.sql
```

- 需要本机可连接的 MySQL 8.0（窗口函数、CTE 需要 8.0+）；
- 含预期报错的脚本加 `--force` 参数可跑完（详见各知识详解"八、运行示例"）。

## 与 Java 模块的衔接

JDBC / MyBatis / Spring 事务等框架主题属"框架"方向（规划见[后端总 README](../../README.md)）；本目录聚焦数据库本身的原理与用法。写好 SQL 是用好一切 ORM 的前提。

## 八股追问板块（沿用 Java 目录的约定）

数据库同为面试八股重镇，本目录每个知识点的 `知识详解.md` 均含**「五、八股追问」**栏目（结论 + 为什么 + 常见追问），格式与 `后端/Java/` 一致。

## 内容规划

### 已完成（2 个知识点）

- `SQL进阶/`：JOIN 四形态、ON vs WHERE、子查询与 NOT IN 的 NULL 陷阱、GROUP BY 与 ONLY_FULL_GROUP_BY、窗口函数三兄弟、CTE 与 UNION
- `索引与执行计划/`：B+ 树与树高估算、聚簇/二级索引与回表、覆盖索引、最左前缀与 Skip Scan、key_len 计算、EXPLAIN 逐列精读、四大失效场景、filesort 与 ANALYZE TABLE

### 规划中（小批二）

- `事务与锁/`：ACID、隔离级别与 MVCC、行锁/间隙锁、死锁排查
- `性能优化/`：慢查询定位、深分页、SQL 改写实战

新增知识点时，先判断是否与已有主题共享稳定的中间分类，再决定目录层级。
