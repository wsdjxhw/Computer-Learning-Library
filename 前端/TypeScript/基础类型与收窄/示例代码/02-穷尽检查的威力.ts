// ============================================================
// 02 · 穷尽检查的威力——故意带着一个错误（预期报错，详见文末）
// 场景：Shape 新增了成员 Squiggle，但 area 忘了加分支
// 检查：npx -p typescript@5 tsc --strict --noEmit 02-穷尽检查的威力.ts
// ============================================================

type Circle = { kind: 'circle'; r: number };
type Square = { kind: 'square'; w: number };
type Squiggle = { kind: 'squiggle'; len: number };   // ← 新增的成员
type Shape = Circle | Square | Squiggle;

function area(s: Shape): number {
  switch (s.kind) {
    case 'circle':
      return Math.PI * s.r ** 2;
    case 'square':
      return s.w ** 2;
    default: {
      // Squiggle 没被处理，流到这里时 s 的类型是 Squiggle（不是 never）
      const _exhaustive: never = s;   // ← 报错位置！Squiggle 塞不进 never
      return 0;
    }
  }
}

// 真实报错（本机 tsc 5.9.3）：
// 02-穷尽检查的威力.ts(20,13): error TS2322: Type 'Squiggle' is not assignable to type 'never'.
// 收获：新增联合成员后，所有"穷尽式 switch"的遗漏点被编译器一一点名——
// 这就是判别联合 + never 的组合拳：改类型定义，编译器替你找出所有要改的分支
