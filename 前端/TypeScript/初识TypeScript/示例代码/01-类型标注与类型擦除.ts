// ============================================================
// 01 · 类型标注与类型擦除——亲手编译一次，看类型去了哪
// 编译（在本目录执行）：
//   npx -p typescript@5 tsc --target ES2020 01-类型标注与类型擦除.ts
// 运行产物：node 01-类型标注与类型擦除.js
// ============================================================

// ---------- 1. 变量标注：类型写在名字后面（Java 写在前面） ----------
const productName: string = '机械键盘';
const price: number = 299;
const tags: string[] = ['外设', '办公'];      // 数组类型：string[]（Java 是 String[]）

// 字面量能看出类型时，实战更多靠"推导"——不写也行
const derived = 3.14;                         // 自动推导为 number，不必写 : number

// ---------- 2. 函数标注：参数 + 返回值 ----------
// Java: double withTax(double amount, double rate)
function withTax(amount: number, rate: number): number {
  return amount * (1 + rate);
}
const msg: string = `含税价 ${withTax(price, 0.13)}`;
console.log(productName, tags, msg);
// 编译完成后打开同目录的 01-类型标注与类型擦除.js：
// 所有 ": 类型" 都消失了，其余代码一模一样——这就是"类型擦除"

// ---------- 3. 自定义形状：type 给对象起类型名 ----------
type Product = {
  id: number;
  name: string;
  price: number;
};
const p: Product = { id: 1, name: productName, price };
console.log(p);

// 拼错属性名，tsc 直接点名——这是 JS 给不了的体验：
// p.prcie = 100;   // error TS2551: Property 'prcie' does not exist on type 'Product'. Did you mean 'price'?
