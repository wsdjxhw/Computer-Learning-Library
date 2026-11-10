'use strict';
// ============================================================
// 04 · 单线程、事件循环与 async/await（Java 视角）
// 运行：node 04-异步与事件循环.js
// 核心模型：JS 只有一个调用栈（单线程）；"等 IO"的活交给运行时，
// 完成后把回调排进队列，栈空了再依次执行——没有 Java 那种多线程抢占
// ============================================================

// ---------- 1. 最小模型：同步 → 微任务 → 宏任务 ----------
console.log('A 同步：脚本顺序执行，立刻打印');

setTimeout(() => {
  console.log('D 宏任务：setTimeout 0ms 也要等栈空 + 微任务清完');
}, 0);

Promise.resolve().then(() => {
  console.log('C 微任务：Promise 回调，优先于所有宏任务');
});

console.log('B 同步：脚本本身没跑完，任何回调都插不进来');
// 单看这四行，顺序是 A → B → C → D

// ---------- 2. async/await 是 Promise 的语法糖 ----------
async function fakeFetch(id) {
  return { id, name: '商品' + id };    // async 函数的返回值自动包成 Promise
}
async function main() {
  console.log('1 await 之前：同步执行');
  const data = await fakeFetch(7);     // await = 挂起等 Promise，把控制权还给事件循环
  console.log('3 await 之后：等价于 .then(res => ...) 回调里的代码');
  return data;
}
main().then((d) => console.log('4 main 完成的 Promise', d.name));
console.log('2 main() 调用后：await 让出控制权，脚本继续往下走');

// ---------- 3. 微任务优先的经典交错 ----------
setTimeout(() => console.log('宏任务 timeout1'), 0);
Promise.resolve()
  .then(() => console.log('微任务 then1'))
  .then(() => console.log('微任务 then2（链式第二跳仍是微任务）'));

// 每轮宏任务之前，微任务队列会被"清空到底"才轮到下一个宏任务。
// 全文件完整输出（本机实测，建议先自己推一遍再运行核对）：
// A B 1 2 C 3 微任务then1 4 微任务then2 D 宏任务timeout1
// 注意"4"排在"微任务then2"之前：队列是先进先出，每个微任务完成时产生的新微任务
// 排到队尾——then1 完成把 then2 排到队尾时，"main 完成的回调"已经排在它前面了
