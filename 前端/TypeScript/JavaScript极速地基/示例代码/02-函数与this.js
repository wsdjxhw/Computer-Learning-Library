'use strict';
// ============================================================
// 02 · 函数是一等公民与 this（Java 视角）
// 运行：node 02-函数与this.js
// 一句话对照：Java 的 this 永远指向当前实例；JS 的 this 由"怎么调用"决定
// ============================================================

// ---------- 1. 函数是值：能赋给变量、能当参数传递 ----------
function double(x) {
  return x * 2;
}
const ref = double;              // 函数本身是个值（Java 里方法不是值，只能包成 Lambda）
console.log(ref(21));            // 42：换了个名字调用同一函数
function apply(fn, value) {      // 函数当参数——一切"回调"的基石
  return fn(value);
}
console.log(apply(double, 8));   // 16：≈ Java 的 Function<Integer,Integer>，但没有类型仪式

// ---------- 2. this 由"怎么调用"决定，不由"在哪里定义"决定 ----------
const user = {
  name: '小明',
  greet() {                      // 方法简写，等价于 greet: function () {...}
    return `你好，我是 ${this.name}`;
  },
};
console.log(user.greet());       // 你好，我是小明：obj.method() 调用 → this = obj

const detached = user.greet;     // 把方法"拆下来"（因为函数是值）
try {
  console.log(detached());       // 裸调用 fn() → this = undefined（严格模式下）
} catch (e) {
  console.log('裸调用报错：', e.message);   // Cannot read properties of undefined (reading 'name')
}
// Java 对照：把 user.getGreeting() 赋给变量再调用，还是绑定 user；JS 拆下来绑定就丢了
// 这就是 DOM 回调 / 定时器回调里 this 变 undefined 的根源

// ---------- 3. 三种修复：call / bind / 箭头函数 ----------
console.log(detached.call(user));   // 你好，我是小明：call 在"这一次调用"里显式指定 this
const bound = detached.bind(user);  // bind 生成一个永久绑定 this 的新函数
console.log(bound());               // 你好，我是小明
const timer = {
  label: '计时器',
  start() {
    // 箭头函数没有自己的 this，捕获"定义处外层"（即 start 方法）的 this
    const trigger = () => `触发者：${this.label}`;
    return trigger();              // 即使 trigger 被拆出去再调用，this 也还是 timer
  },
};
console.log(timer.start());         // 触发者：计时器

// ---------- 4. 实战选择：回调用箭头函数 ----------
const report = {
  title: '订单',
  total(orders) {
    // 回调不碰 this → 箭头函数最省心；这里箭头捕获外层（total）的 this 用于最后拼标题
    const sum = orders.reduce((acc, o) => acc + o.amount, 0);
    return `[${this.title}] 合计 ${sum}`;
  },
};
console.log(report.total([{ amount: 100 }, { amount: 250 }]));   // [订单] 合计 350
