// ============================================================
// 01 · 收窄的六种手段（本文件零错误，可 --noEmit 体检）
// 检查：npx -p typescript@5 tsc --strict --noEmit 01-收窄六式.ts
// 运行：先去掉类型编译（tsc 01-*.ts）再 node 产物；或直接看注释推演
// ============================================================

// ---------- 1. typeof 收窄：原始类型的分岔 ----------
function format(value: string | number): string {
  if (typeof value === 'string') {
    return value.trim();          // 这里 value 已经是 string——能调 trim
  }
  return value.toFixed(2);        // 走到这说明是 number——能调 toFixed
}
// 没有 if 会怎样？value.trim() 直接报 TS2339（union 上不存在两类型共有的方法之外的方法）

// ---------- 2. 真值收窄：if(x) 排除 null/undefined ----------
function greet(name: string | null): string {
  if (name) {
    return name.toUpperCase();    // name: string（null 已被排除）
  }
  return '匿名';
}
// 边界：if(name) 在【运行时】还会滤掉空串 ''（falsy），但类型系统仍按 string 处理
// ——类型不区分"非空字符串"，这是类型收窄的能力边界

// ---------- 3. 相等性收窄：=== null 一刀切走 null ----------
function textLen(x: string | null): number {
  if (x === null) return 0;
  return x.length;                // x: string
}

// ---------- 4. in 收窄：按"有没有某属性"分岔 ----------
type Fish = { swim: () => void };
type Bird = { fly: () => void };
function move(pet: Fish | Bird): void {
  if ('swim' in pet) {
    pet.swim();                   // pet: Fish
  } else {
    pet.fly();                    // pet: Bird
  }
}

// ---------- 5. instanceof 收窄 ----------
function logTime(t: Date | string): void {
  if (t instanceof Date) {
    console.log(t.toISOString()); // t: Date
  } else {
    console.log(t.toUpperCase()); // t: string
  }
}

// ---------- 6. 判别联合 + never 穷尽检查 ----------
type Circle = { kind: 'circle'; r: number };
type Square = { kind: 'square'; w: number };
type Shape = Circle | Square;
function area(s: Shape): number {
  switch (s.kind) {
    case 'circle':
      return Math.PI * s.r ** 2;  // s: Circle
    case 'square':
      return s.w ** 2;            // s: Square
    default: {
      const _exhaustive: never = s;  // 漏写分支/新增成员时，这行立刻报错（见 02）
      return _exhaustive;
    }
  }
}

console.log(format('  hi  '), format(3.14159));
console.log(greet('ts'), greet(null), textLen(null));
console.log(area({ kind: 'circle', r: 2 }).toFixed(2), area({ kind: 'square', w: 3 }));
logTime(new Date('2026-01-01'));
logTime('2026-01-01');
