# 示例代码：事务与锁（双会话并发演示）

事务的问题只在**并发**时暴露，所以本知识点的示例由「会话A + 会话B」成对组成：两个 mysql 连接同时执行，靠 `SELECT SLEEP()` 把时间线对齐，每个演示约 8 秒。

## 目录结构

| 文件 | 内容 |
| --- | --- |
| `00_建库与数据.sql` | 建 `learn_tx` 库（account 账户表 + t_gap 间隙表），先跑 |
| `演示1_脏读_会话A/B.sql` | READ-UNCOMMITTED 下读到别人未提交的数据 |
| `演示2_不可重复读_会话A/B.sql` | READ-COMMITTED 下同一事务两次读结果不同（含幻读 count） |
| `演示3_可重复读_会话A/B.sql` | REPEATABLE-READ（默认）下两次读一致——MVCC 快照 |
| `演示4_MVCC快照与当前读_会话A/B.sql` | 同一行数据：普通 SELECT 读旧值、FOR UPDATE 读新值 |
| `演示5_行锁与锁等待_会话A/B.sql` | 行锁粒度到行；锁不到就排队，超时 ERROR 1205 |
| `演示6_死锁_会话A/B.sql` | 交叉加锁形成环，InnoDB 检测并回滚一方 ERROR 1213 |
| `演示7_间隙锁RR_会话A/B.sql` | RR 下往被锁的"空隙"插入 → 被阻塞 |
| `演示8_间隙锁RC_会话A/B.sql` | 同样一幕在 RC 下畅通（不锁间隙） |
| `演示9_保存点与回滚.sql` | SAVEPOINT 存档点，单会话 |
| `运行演示.ps1` | 一键驱动：并发执行 A/B 并合并输出 |

## 运行方法

**方式一：一键跑（推荐）**

```powershell
cd 后端/数据库/MySQL/事务与锁/示例代码
mysql --default-character-set=utf8mb4 -t -u root -p < 00_建库与数据.sql   # 先建库
.\运行演示.ps1 -Demo 1 -User root -Password 你的密码                      # 跑单个演示
.\运行演示.ps1 -Demo All -User root -Password 你的密码                    # 跑全部
```

**方式二：手动开两个终端**（更直观，能看到"卡住"的效果）

终端 1：`mysql --default-character-set=utf8mb4 -t -u root -p learn_tx`，粘贴执行 会话A.sql；
终端 2：同样再开一个，粘贴 会话B.sql。各自按文件里的顺序执行即可。

## 预期现象速查

| 演示 | 关键现象 | 错误码 |
| --- | --- | --- |
| 1 脏读 | B 读到 1500，A 回滚后变回 1000 | — |
| 2 RC | B 两次读：1000→1500，2 人→3 人 | — |
| 3 RR | B 两次读都是 1000 / 2 人；提交后才看到 3 人 | — |
| 4 MVCC | 普通 SELECT 1000；FOR UPDATE 1500；再普通读仍是 1000 | — |
| 5 行锁 | 改 id=2 秒过；改 id=1 卡 3 秒后失败 | 1205 |
| 6 死锁 | 一方报错回滚，另一方成功 | 1213 |
| 7 间隙锁 RR | INSERT id=7 卡 3 秒后失败 | 1205 |
| 8 间隙锁 RC | INSERT id=7 立即成功 | — |
| 9 保存点 | ROLLBACK TO sp1 回到 700；ROLLBACK 回到 1000 | — |

每个演示的会话A开头都会重置数据，可反复运行。时间线与输出的逐句解读见《[知识详解.md](../知识详解.md)》第三节。
