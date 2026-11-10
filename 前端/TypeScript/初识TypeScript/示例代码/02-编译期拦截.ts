// ============================================================
// 02 · 编译期能拦住什么——故意制造 4 个错误，看 tsc 报错长什么样
// 编译（用 tsconfig 一次编译全部）：npx -p typescript@5 tsc
// 现象：tsc 报 4 个错误，但【默认仍会产出 .js】（noEmitOnError 默认 false）
// 再跑：node 02-编译期拦截.js —— 观察哪些错误运行时立刻炸、哪些静默出错值
// ============================================================

function withTax2(amount: number, rate: number): number {
  return amount * (1 + rate);
}

// 错误1 · 类型不匹配：number 塞不进 string
// Java 对照：incompatible types: double cannot be converted to String
const label2: string = withTax2(100, 0.13);

// 错误2 · 属性拼错：JS 里只得到 undefined，静默失败；重构改名后最高发的错误
const order2 = { id: 7, amount: 250 };
const amountCopy2: number = order2.amout;      // TS2551: 'amout' does not exist. Did you mean 'amount'?

// 错误3 · 参数个数不对
// Java 对照：method withTax2(...) is not applicable for arguments (int)
const bad2: number = withTax2(100);            // TS2554: Expected 2 arguments, but got 1

// 上面三个错误编译期被拦，但产物"能跑"——跑出来的是错的值（静默出错，比崩溃更危险）：
console.log('错误1 的产物值 label2 =', label2);          // 112.99999999999999：number 被当 string 用，运行时无感（浮点误差让 113 也对不上）
console.log('错误2 的产物值 amountCopy2 =', amountCopy2); // undefined：拼错的属性静默变 undefined
console.log('错误3 的产物值 bad2 =', bad2);               // NaN：rate 没传是 undefined，100 * (1+undefined)

// 错误4 · 调用不存在的方法：四个错误里唯一会在运行时【立刻炸】的
const upper2: number = 'hello'.toUppercase2(); // TS2551: 'toUppercase2' does not exist. Did you mean 'toUpperCase'?
console.log('到不了这一行');
