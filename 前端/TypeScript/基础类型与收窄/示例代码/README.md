# 示例代码 · 基础类型与收窄

三个文件对应学习页的三条主线，均可用一条命令体检（无需全局安装，本机实测 tsc 5.9.3）：

```powershell
npx --yes -p typescript@5 tsc --strict --noEmit 01-收窄六式.ts
```

| 文件 | 预期结果 | 学什么 |
| --- | --- | --- |
| [01-收窄六式.ts](01-收窄六式.ts) | **零错误**；去掉 `--noEmit` 编译后 `node 01-收窄六式.js` 可运行 | typeof / 真值 / 相等性 / in / instanceof / 判别联合+never，六种收窄一次看全 |
| [02-穷尽检查的威力.ts](02-穷尽检查的威力.ts) | **预期 1 个错误**：`(20,13): error TS2322: Type 'Squiggle' is not assignable to type 'never'.` | 故意的：联合新增成员而 switch 漏写分支，never 穷尽检查当场点名 |
| [03-unknown的正确打开方式.ts](03-unknown的正确打开方式.ts) | 零错误（打开第 9 行注释则报 `TS18046`） | 外部数据入口用 unknown，一层层收窄证明后才用；对照 any 的一路放行 |

## 运行 01 的产物

```powershell
npx --yes -p typescript@5 tsc 01-收窄六式.ts   # 产出 01-收窄六式.js
node 01-收窄六式.js
```

实测输出（本机 node 18.20.4）：

```text
hi 3.14
TS 匿名 0
12.57 9
2026-01-01T00:00:00.000Z
2026-01-01
```

> 学习页（[../学习页.html](../学习页.html)）里的报错文本、参考答案和数值结论，全部来自上表的真实运行结果——浏览器里跑不了 tsc，动手练习的验证以这里的本地命令为准。
