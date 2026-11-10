// ============================================================
// 03 · any：把检查网剪开一个洞，会传染
// 编译：npx -p typescript@5 tsc（本文件贡献 2 个错误，正常现象）
// ============================================================

// ---------- 1. 显式 any：一行失守，层层失守 ----------
// JSON.parse 的返回类型就是 any——"外面来的数据"天然是 any
const config: any = JSON.parse('{"host":"localhost","port":3306}');
console.log(config.host);       // 合法：碰巧存在
console.log(config.prt);        // 也"合法"：拼错了也不报错，运行时是 undefined —— 静默！
const portText: string = config.port;   // any 赋给谁都不报错：3306（number）被当 string 用
console.log(portText + 1);      // 运行时输出 3307：portText 实际是 number，": string" 标注形同虚设——any 在编译期已放行

// ---------- 2. 隐式 any：strict 模式下参数不标注会直接报错 ----------
function greet2(who) {          // error TS7006: Parameter 'who' implicitly has an 'any' type
  return `你好，${who}`;
}
console.log(greet2('TypeScript'));

// ---------- 3. 标注之后的防线（对照） ----------
function greet3(who: string): string {
  return `你好，${who}`;
}
console.log(greet3('TypeScript'));
console.log(greet3(42));        // error TS2345: number 不能传给 string 参数——编译期就拦下
// 注：无视错误硬跑产物的话，这行照样执行（打印"你好，42"）——
// 类型检查只存在于编译期，运行时没有任何保护
