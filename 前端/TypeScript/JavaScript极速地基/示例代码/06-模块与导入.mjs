// ============================================================
// 06 · 模块与导入（入口文件）
// 运行：node 06-模块与导入.mjs
// ============================================================

import summarize from './05-工具库.mjs';                    // 默认导出：名字随便起，不用 {}
import { TAX_RATE, withTax } from './05-工具库.mjs';        // 命名导出：名字必须对上，用 {}
import { discounted as priceAfterDiscount } from './05-工具库.mjs';  // as 重命名
import * as taxUtils from './05-工具库.mjs';                // 命名空间导入：全收进一个对象

console.log(summarize(200, 'vip'));           // 原价 200，折后 180，含税 226
console.log(TAX_RATE, withTax(100));          // 0.13 113
console.log(priceAfterDiscount(100, 'vip'));  // 90
console.log(taxUtils.withTax(50));            // 56：命名空间导入 ≈ 把整个模块当一个静态工具类用
// 为什么不是 57？50 * 1.13 = 56.49999999999999——二进制浮点存不下 0.13（同 Java 的 double），
// 四舍五入后是 56。涉及钱的计算要先乘到整数分，别直接用小数乘

// 坑位提示：CommonJS（.js 默认）与 ESM（.mjs）不能混用——
// .mjs 里用 require 报错，.js 里用 import（不配 package.json）也报错。
// 这两套模块体系并存是 Node 的历史包袱，TypeScript 工程化（知识点10）再讲怎么统一。
