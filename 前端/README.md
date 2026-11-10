# 前端

存放浏览器与用户界面开发相关学习内容，例如：HTML、CSS、JavaScript、TypeScript、前端框架、工程化、浏览器原理、性能优化和前端面试知识。

## 内容规划

### 进行中：TypeScript 主线（11 个知识点，分 4 小批；小批一已完成）

当前方向从 TypeScript 切入（TS 是 JS 的超集，也是现代前端框架与 Node 工程的默认语言）。规划参考了社区口碑最好的教程：[TypeScript 官方 Handbook（中文）](https://www.tslang.cn/zh/docs/handbook/intro.html)、[《TypeScript 入门教程》（xcatliu）](https://ts.xcatliu.com/)、[《深入理解 TypeScript》](https://jkchao.github.io/typescript-book-chinese/)、[Total TypeScript 免费教程](https://www.totaltypescript.com/tutorials)（Matt Pocock，社区公认 TS 教学第一人）、Udemy《Understanding TypeScript》（Max Schwarzmüller，4.6 分约 6 万评价，章节结构参考）。

学习路线：先以 1 个知识点补齐「Java 视角的 JS 极速地基」（主流 TS 教程一致要求先会 JS），再进 TS 主线；示例代码用 `tsc` 编译 + `node` 运行产物真跑验证。

| 小批 | 知识点 |
| --- | --- |
| 一 地基与初识 ✅ | ① JavaScript 极速地基（Java 视角）② 初识 TypeScript —— 说明见 [TypeScript/README.md](TypeScript/README.md) |
| 二 类型系统主干 | ③ 基础类型与收窄 ④ 函数与对象类型 ⑤ 类型推导与断言 |
| 三 进阶类型 | ⑥ 泛型 ⑦ 类型体操入门 ⑧ 类与面向对象（结构化 vs 名义类型） |
| 四 工程与实战 | ⑨ 声明文件与第三方类型 ⑩ tsconfig 与工程化 ⑪ 综合实战：类型安全小项目 |

### 后续路线（规划中——落地时才建目录）

TypeScript → 浏览器与 DOM → CSS → 前端框架（Vue / React，届时再选）→ 前端工程化（Vite、包管理）→ 性能与前端面试专题。

新增知识点时，先判断是否与已有主题共享稳定的中间分类，再决定目录层级。
