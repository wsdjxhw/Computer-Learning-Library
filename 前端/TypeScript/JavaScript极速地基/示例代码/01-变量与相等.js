'use strict';
// ============================================================
// 01 · 变量、动态类型与相等判断（Java 视角）
// 运行：node 01-变量与相等.js
// 一句话对照：Java 是"变量有类型"；JS 是"变量没类型，值才有类型"
// ============================================================

// ---------- 1. 动态类型：同一个变量先后装不同类型的值 ----------
let box = 42;                    // number：JS 只有一种数字类型（不分 int/long/double）
console.log(box, typeof box);    // 42 number
box = 'hello';                   // 换成 string 也合法——类型跟着值走
console.log(box, typeof box);    // hello string
box = { id: 1 };                 // 再换成 object：这三行在 Java 里连编译都过不了
console.log(box, typeof box);    // { id: 1 } object

// ---------- 2. let / const / var ----------
const MAX = 100;                 // const：绑定不可重新赋值（≈ Java 的 final）
// MAX = 200;                    // 打开会报错：TypeError: Assignment to constant variable
let count = 1;                   // let：块级作用域，最接近 Java 的局部变量
{
  let inner = '块内';
  console.log(inner);            // 块内：let 只在这个 { } 里可见
}
// console.log(inner);           // 打开报错：ReferenceError: inner is not defined
// var 是旧写法：函数级作用域 + 变量提升（声明被"搬"到函数开头），
// 且 var a = 1; var a = 2; 重复声明不报错——现代代码一律用 let/const，var 只在读老代码时见到

// ---------- 3. null 与 undefined ----------
let notAssigned;                 // 声明未赋值 → undefined（"系统：从没给过值"）
const deliberatelyEmpty = null;  // 显式置空 → null（"程序员：故意放空"）
console.log(notAssigned, deliberatelyEmpty);   // undefined null
console.log(typeof deliberatelyEmpty);         // object ← 历史包袱：typeof null === 'object'
// Java 只有 null；JS 多出的 undefined 表示"缺省"，null 表示"主动置空"
// typeof null 返回 'object' 是 1995 年的实现遗留，为兼容至今不能修

// ---------- 4. === 与 == ----------
console.log('5' == 5);           // true ：== 会隐式转换（字符串转成数字再比）
console.log('5' === 5);          // false：=== 类型不同直接不等，不做转换
console.log(null == undefined);  // true ：特例，null 与 undefined 仅彼此宽松相等
console.log(null === undefined); // false
console.log(0 == '');            // true ：'' 隐式转成数字 0
console.log(0 === '');           // false
console.log(NaN === NaN);        // false：NaN 连自己都不等（同 Java 的 Double.NaN == Double.NaN 为 false）
// 团队规约：一律用 ===；== 唯一合理用途是 x == null（同时判断 null 和 undefined）

// ---------- 5. 模板字符串 ----------
const course = 'MySQL';
const points = 4;
console.log(`已学完 ${course}，共 ${points} 个知识点，按两周一个约 ${points * 2} 周`);
// 反引号 + ${表达式}：Java 的 String.format(...) 位置直接内插，任意类型自动转字符串
