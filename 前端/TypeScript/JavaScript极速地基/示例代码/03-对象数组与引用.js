'use strict';
// ============================================================
// 03 · 对象、数组、引用与解构（Java 视角）
// 运行：node 03-对象数组与引用.js
// ============================================================

// ---------- 1. 对象字面量：不写类，直接造对象 ----------
const product = { id: 1, name: '键盘', price: 299 };
product.stock = 50;              // 动态加属性；delete product.stock 可删——对象像个"可变的袋子"
console.log(product);            // { id: 1, name: '键盘', price: 299, stock: 50 }
// Java 对照：必须先 class Product 再 new；JS 的字面量更像 Map，随取随放
// 这份"自由"正是后面要学 TypeScript 的原因——没人检查你放进去什么

// ---------- 2. 赋值传引用（对象/数组），原始类型才拷贝值 ----------
const a = { name: '原始对象' };
const b = a;                     // 两个变量指向同一个对象（≈ Java 的对象引用）
b.name = '被改了';
console.log(a.name);             // 被改了：a、b 是同一份

// ---------- 3. 解构与展开 ----------
const { name, price } = product;         // 按属性名"拆包"，一次取多个
const { name: productName } = product;   // 重命名解构
const cheaper = { ...product, price: 199 };   // 浅拷贝 + 覆盖个别字段（不可变更新的常用姿势）
console.log(name, productName);          // 键盘 键盘
console.log(cheaper);                    // { id: 1, name: '键盘', price: 199, stock: 50 }
console.log([1, 2, 3, 4].slice(0, 2));   // [ 1, 2 ]：数组切片（≈ subList，但返回新数组）

// ---------- 4. 浅拷贝的坑：嵌套对象仍然共享 ----------
const original = { info: { stock: 10 } };
const shallow = { ...original };         // ... 只拷贝第一层
shallow.info.stock = 999;                // 改"拷贝"的嵌套字段
console.log(original.info.stock);        // 999：原始也被改了——第二层还是同一个对象
const deep = structuredClone(original);  // 深拷贝（Node 17+ / 现代浏览器内置）
deep.info.stock = 1;
console.log(original.info.stock);        // 999：深拷贝后彻底分离

// ---------- 5. map / filter / reduce：对标 Java Stream ----------
const orders = [
  { id: 1, amount: 100, paid: true },
  { id: 2, amount: 250, paid: false },
  { id: 3, amount: 80, paid: true },
];
const paidSum = orders
  .filter((o) => o.paid)          // ↔ stream().filter(o -> o.isPaid())
  .map((o) => o.amount)           // ↔ .map(o -> o.getAmount())
  .reduce((acc, n) => acc + n, 0);// ↔ .reduce(0, Integer::sum)
console.log(paidSum);             // 180
// 差别：JS 数组方法每次调用都立即遍历（非惰性）；Java Stream 要到终结操作才真正执行
