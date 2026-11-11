// ============================================================
// 03 · unknown 的正确打开方式（含一个预期报错）
// 检查：npx -p typescript@5 tsc --strict --noEmit 03-unknown的正确打开方式.ts
// ============================================================

const data: unknown = JSON.parse('{"port":3306}');

// 直接用 → 编译期拒绝（对照 any 的"静默放行"）：
// console.log(data + 1);
// ↑ 打开注释，报错：error TS18046: 'data' is of type 'unknown'.

// 正确姿势：一层层收窄，证明可用再用
if (typeof data === 'object' && data !== null && 'port' in data) {
  const port = data.port;          // in 收窄后 port: unknown（属性类型仍未知）
  if (typeof port === 'number') {
    console.log('端口号 + 1 =', port + 1);   // port: number，放行
  } else {
    console.log('port 不是数字，拒绝使用');
  }
}
// 对照初识TypeScript示例03：any 版本一路放行、运行时输出错值 3307；
// unknown 版本在编译期就要求你"证明数据长什么样"——防线前移
