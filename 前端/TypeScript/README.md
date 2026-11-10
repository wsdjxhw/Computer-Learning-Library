# TypeScript

存放 TypeScript 主线学习内容（11 个知识点，分 4 小批推进，规划见[前端总 README](../README.md)）。

## 怎么跑示例

- **JS 地基示例**：Node 18+，在 [JavaScript极速地基/示例代码/](JavaScript极速地基/示例代码/) 目录 `node 01-变量与相等.js`（或一键 `powershell -ExecutionPolicy Bypass -File .\运行全部.ps1`）。
- **TS 示例**：在 [初识TypeScript/示例代码/](初识TypeScript/示例代码/) 目录用 `npx -p typescript@5 tsc` 编译（免全局安装，本机实测 5.9.3），再用 `node` 运行产物 `.js`；详细顺序见该目录 README。

## 与 JavaScript 的关系

TS 是 JS 的超集：先以 1 个知识点补齐「JavaScript 极速地基（Java 视角）」再进 TS 主线——TS 的类型描述的是 JS 的行为，且编译产物就是 JS（类型擦除）。

## 知识点清单（小批一 2/11 已完成）

- ✅ `JavaScript极速地基/`：动态类型、let/const/var、null/undefined、`===` 与隐式转换、函数一等公民与 this 四规则、引用语义与浅拷贝、map/filter/reduce、事件循环（宏/微任务）、async/await、ESM 模块
- ✅ `初识TypeScript/`：超集关系、tsc 两件事（检查 + 转译）、类型擦除（enum 例外）、报错三段式与高频错误码、`noEmitOnError` 默认行为、any 传染与 unknown 防线、编译期能力的边界
- ⬜ 小批二（类型系统主干）：基础类型与收窄、函数与对象类型、类型推导与断言
- ⬜ 小批三（进阶类型）：泛型、类型体操入门、类与面向对象
- ⬜ 小批四（工程与实战）：声明文件与第三方类型、tsconfig 与工程化、综合实战

## 八股追问板块（沿用后端目录的约定）

TS 与 JS 基础同为前端面试重点，每个知识点的 `知识详解.md` 均含**「五、八股追问」**栏目（结论 + 为什么 + 常见追问），格式与 `后端/` 一致。

新增知识点时，先判断是否与已有主题共享稳定的中间分类，再决定目录层级。
