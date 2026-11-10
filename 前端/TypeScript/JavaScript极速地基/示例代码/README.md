# 示例代码 · JavaScript 极速地基

## 怎么跑

环境：Node 18+（`node -v` 检查）。在**本目录**下执行：

```powershell
node 01-变量与相等.js        # 也可以直接右键"使用 Code Runner"（如装了插件）
node 02-函数与this.js
node 03-对象数组与引用.js
node 04-异步与事件循环.js
node 06-模块与导入.mjs        # 模块示例：入口是 06，它去 import 05
```

或一键全跑：

```powershell
powershell -ExecutionPolicy Bypass -File .\运行全部.ps1
```

## 文件清单

| 文件 | 主题 | 关键现象 |
| --- | --- | --- |
| `01-变量与相等.js` | 动态类型、let/const、null/undefined、`===` | 同一变量连装三种类型；`typeof null === 'object'` |
| `02-函数与this.js` | 函数是一等公民、this 由调用方式决定 | 方法拆下来裸调用直接报错，三种修复姿势 |
| `03-对象数组与引用.js` | 对象字面量、解构/展开、浅拷贝的坑、map/filter/reduce | 浅拷贝改嵌套字段，原始对象跟着变 |
| `04-异步与事件循环.js` | 单线程、宏任务/微任务、async/await | `4` 排在 `then2` 之前的 FIFO 细节 |
| `05-工具库.mjs` | 被导入方：export 三种形态 | 不导出的内容即模块私有 |
| `06-模块与导入.mjs` | 导入方：默认/命名/重命名/命名空间导入 | `50 * 1.13 = 56.499...` 浮点坑 |

## 两个约定

1. **模块文件用 `.mjs`**：没有 `package.json` 时 `.js` 默认按 CommonJS 解析，写 `import/export` 会报错；`.mjs` 明确声明用 ES 模块。
2. **输出以真跑为准**：各文件注释里标注的预期输出均为本机（Node 18.20.4）实测结果，动手改代码后重新运行对照。
