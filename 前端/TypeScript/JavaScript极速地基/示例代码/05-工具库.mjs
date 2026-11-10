// ============================================================
// 05 · 工具库（被导入方）—— export 的三种形态
// 本文件不直接运行，由 06 导入。
// 为什么是 .mjs：没有 package.json 的项目里 .js 默认按 CommonJS 解析，
// 写 import/export 会报错；.mjs 明确声明"我是 ES 模块（ESM）"
// ============================================================

export const TAX_RATE = 0.13;                  // 命名导出：常量

export function withTax(amount) {              // 命名导出：函数
  return Math.round(amount * (1 + TAX_RATE));
}

const discountTable = { vip: 0.9, normal: 1 }; // 不 export 的内容就是模块私有

export function discounted(amount, level) {
  // ?? 空值合并：level 为 null/undefined 时取 1（比 Java 的三目运算符省事）
  return Math.round(amount * (discountTable[level] ?? 1));
}

export default function summarize(amount, level) {   // 默认导出：一个模块只能有一个
  return `原价 ${amount}，折后 ${discounted(amount, level)}，含税 ${withTax(amount)}`;
}

// Java 对照：package + import 只能导"类"；ESM 可以导出任意值（变量/函数/类）。
// 可见性也不一样：Java 靠 private 修饰符，ESM 靠"不导出就是私有"。
