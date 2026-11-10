#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const vm = require('vm');

const target = process.argv[2];
if (!target) {
  console.error('用法：node validate_learning_page.js <学习页.html>');
  process.exit(2);
}

const file = path.resolve(target);
const results = [];
function record(level, name, detail) {
  results.push({ level, name, detail: detail || '' });
}
function pass(name, detail) { record('PASS', name, detail); }
function warn(name, detail) { record('WARN', name, detail); }
function fail(name, detail) { record('FAIL', name, detail); }
function check(condition, name, success, failure) {
  if (condition) pass(name, success);
  else fail(name, failure);
}
function softCheck(condition, name, success, failure) {
  if (condition) pass(name, success);
  else warn(name, failure);
}

let html;
try {
  html = fs.readFileSync(file, 'utf8');
} catch (error) {
  console.error('无法读取文件：' + error.message);
  process.exit(2);
}

check(/<!DOCTYPE html>/i.test(html), '文档类型', '存在 HTML5 doctype', '缺少 HTML5 doctype');
check(/<html[^>]+lang=["']zh-CN["']/i.test(html), '页面语言', '已声明 zh-CN', '缺少 lang="zh-CN"');
check(/<meta[^>]+charset=["']?UTF-8/i.test(html), '字符编码', '已声明 UTF-8', '缺少 UTF-8 声明');
check(/<meta[^>]+name=["']viewport["']/i.test(html), '移动端视口', '存在 viewport', '缺少 viewport meta');
const h1Count = (html.match(/<h1(?:\s|>)/gi) || []).length;
check(h1Count === 1, '主标题层级', '页面只有一个 h1', 'h1 数量为 ' + h1Count + '，应为 1');

const ids = [];
const idPattern = /\sid=["']([^"']+)["']/gi;
let match;
while ((match = idPattern.exec(html))) ids.push(match[1]);
const duplicateIds = ids.filter((id, index) => ids.indexOf(id) !== index);
check(duplicateIds.length === 0, 'ID 唯一性', duplicateIds.length === 0 ? ids.length + ' 个 ID 无重复' : '', '重复 ID：' + Array.from(new Set(duplicateIds)).join(', '));

const externalRuntime = [];
const resourcePattern = /<(?:script|img|iframe|audio|video|source)\b[^>]*\bsrc=["'](https?:)?\/\/([^"']+)["'][^>]*>/gi;
while ((match = resourcePattern.exec(html))) externalRuntime.push(match[0]);
const externalCss = /<link\b[^>]*rel=["']stylesheet["'][^>]*href=["'](?:https?:)?\/\//gi;
const cssImports = /@import\s+url\(\s*["']?(?:https?:)?\/\//gi;
check(!externalRuntime.length && !externalCss.test(html) && !cssImports.test(html), '离线运行资源', '未发现外部脚本、样式或媒体依赖', '发现外部运行依赖：' + externalRuntime.join('；'));

const inlineScripts = [];
const localScripts = [];
const scriptPattern = /<script\b([^>]*)>([\s\S]*?)<\/script>/gi;
while ((match = scriptPattern.exec(html))) {
  const attrs = match[1];
  const code = match[2];
  const srcMatch = attrs.match(/\bsrc=["']([^"']+)["']/i);
  if (srcMatch) localScripts.push(srcMatch[1]);
  else inlineScripts.push(code);
}
let scriptErrors = 0;
inlineScripts.forEach((code, index) => {
  try { new vm.Script(code, { filename: file + '#inline-' + (index + 1) }); }
  catch (error) { scriptErrors++; fail('内联脚本语法 ' + (index + 1), error.message); }
});
if (!scriptErrors) pass('内联脚本语法', inlineScripts.length + ' 段脚本均可解析');

localScripts.forEach((src) => {
  if (/^(?:https?:)?\/\//i.test(src)) return;
  const clean = decodeURIComponent(src.split('#')[0].split('?')[0]);
  const localFile = path.resolve(path.dirname(file), clean);
  if (!fs.existsSync(localFile)) fail('本地脚本路径', '不存在：' + src);
  else {
    try {
      new vm.Script(fs.readFileSync(localFile, 'utf8'), { filename: localFile });
      pass('本地脚本路径', src + ' 存在且语法可解析');
    } catch (error) { fail('本地脚本语法', src + '：' + error.message); }
  }
});

const localRefs = [];
const refPattern = /\b(?:href|src)=["']([^"']+)["']/gi;
while ((match = refPattern.exec(html))) localRefs.push(match[1]);
const missingRefs = [];
const missingAnchors = [];
localRefs.forEach((ref) => {
  if (/^(?:[a-z]+:|\/\/|#)/i.test(ref)) {
    if (ref.charAt(0) === '#' && ref.length > 1 && ids.indexOf(ref.slice(1)) < 0) missingAnchors.push(ref);
    return;
  }
  const cleanRef = ref.split('#')[0].split('?')[0];
  if (!cleanRef) return;
  let decoded;
  try { decoded = decodeURIComponent(cleanRef); } catch (error) { decoded = cleanRef; }
  const localFile = path.resolve(path.dirname(file), decoded);
  if (!fs.existsSync(localFile)) missingRefs.push(ref);
});
check(missingRefs.length === 0, '本地链接与资源', missingRefs.length === 0 ? '所有本地相对路径均存在' : '', '不存在的路径：' + missingRefs.join('；'));
check(missingAnchors.length === 0, '页内锚点', missingAnchors.length === 0 ? '所有页内锚点均有目标' : '', '无效锚点：' + missingAnchors.join('；'));

const storageAdapter = /function\s+(?:lsGet|storageGet|safeGet)[\s\S]*?try\s*{[\s\S]*?localStorage[\s\S]*?}\s*catch\s*\(/i.test(html);
const catchesStorage = /localStorage[\s\S]{0,500}catch\s*\(/i.test(html) || storageAdapter;
check(/localStorage/.test(html) ? catchesStorage : true, '存储降级', /localStorage/.test(html) ? '检测到 localStorage 异常捕获' : '页面未使用持久化存储', '检测到 localStorage，但未发现邻近的 try/catch 降级');

const svgBlocks = html.match(/<svg\b[\s\S]*?<\/svg>/gi) || [];
let svgBalanceError = '';
svgBlocks.forEach((svg, svgIndex) => {
  const stack = [];
  const tagPattern = /<\/?([a-zA-Z][\w:-]*)(?:\s[^<>]*?)?\s*\/?>/g;
  let tag;
  while ((tag = tagPattern.exec(svg))) {
    const raw = tag[0];
    const name = tag[1].toLowerCase();
    if (raw.startsWith('</')) {
      const open = stack.pop();
      if (open !== name) { svgBalanceError = 'SVG ' + (svgIndex + 1) + '：期望闭合 ' + open + '，实际闭合 ' + name; break; }
    } else if (!/\/>$/.test(raw)) stack.push(name);
  }
  if (!svgBalanceError && stack.length) svgBalanceError = 'SVG ' + (svgIndex + 1) + '：未闭合 ' + stack.join(', ');
});
check(svgBalanceError === '', 'SVG 标签配平', svgBlocks.length + ' 张内联 SVG 标签配平', svgBalanceError);
const svgWithoutName = svgBlocks.filter((svg) => !/role=["']img["']/i.test(svg) || !/aria-label=["'][^"']+["']/i.test(svg));
softCheck(svgWithoutName.length === 0, 'SVG 可访问名称', svgBlocks.length + ' 张 SVG 均有 role 与 aria-label', svgWithoutName.length + ' 张 SVG 缺少 role="img" 或 aria-label');

softCheck(/@media\s*\(max-width:\s*(?:[3-9]\d{2}|1\d{3})px\)/i.test(html), '移动端样式', '检测到移动端媒体查询', '未检测到移动端媒体查询');
softCheck(/scroll-margin-top\s*:/i.test(html), '锚点偏移', '检测到 scroll-margin-top', '缺少粘性顶栏下的锚点偏移');
softCheck(/prefers-reduced-motion/i.test(html), '减少动画', '支持 prefers-reduced-motion', '未支持减少动画偏好');
softCheck(/@media\s+print/i.test(html), '打印样式', '存在打印样式', '缺少打印样式');
softCheck(/:focus-visible/i.test(html), '键盘焦点', '存在可见焦点样式', '缺少 :focus-visible 样式');
softCheck(/aria-live=["']polite["']/i.test(html), '动态反馈', '存在 aria-live 动态结果区域', '未检测到 aria-live 动态反馈');
softCheck(/<(?:nav|details)[\s\S]*?(?:目录|章节)/i.test(html), '章节导航', '检测到目录或章节导航', '未检测到章节导航');

const fontMatch = html.match(/body\s*{[^}]*font-size:\s*(\d+(?:\.\d+)?)px/i);
softCheck(!!fontMatch && Number(fontMatch[1]) >= 16, '正文字号', fontMatch ? 'body 正文字号为 ' + fontMatch[1] + 'px' : '', fontMatch ? 'body 正文字号仅 ' + fontMatch[1] + 'px，建议至少 16px' : '未明确 body 正文字号，建议至少 16px');
softCheck(/data-learning-section/i.test(html), '章节完成机制', '检测到 data-learning-section', '未检测到章节完成标记；旧页面可作为体验改造建议');
softCheck(/data-section-complete/i.test(html), '主动学习状态', '检测到主动章节完成按钮', '未检测到主动章节完成按钮');
softCheck(/继续上次学习|继续学习/i.test(html), '继续学习', '提供继续上次学习', '未检测到继续上次学习入口');
softCheck(/复制代码|copy-code/i.test(html), '代码复制', '提供代码复制', '未检测到代码复制功能');
softCheck(/错题|重做/i.test(html), '错题反馈', '提供错题或重做机制', '未检测到错题重做机制');
softCheck(/闭卷|回忆/i.test(html), '回忆与复习', '提供回忆或复习闭环', '未检测到闭卷回忆环节');

const buttonPattern = /<button\b([^>]*)>([\s\S]*?)<\/button>/gi;
let unnamedButtons = 0;
while ((match = buttonPattern.exec(html))) {
  const text = match[2].replace(/<[^>]+>/g, '').trim();
  if (!text && !/aria-label=["'][^"']+["']/i.test(match[1])) unnamedButtons++;
}
softCheck(unnamedButtons === 0, '按钮可访问名称', '静态按钮均有文字或 aria-label', unnamedButtons + ' 个静态按钮缺少可访问名称');

const passed = results.filter((item) => item.level === 'PASS').length;
const warnings = results.filter((item) => item.level === 'WARN').length;
const failures = results.filter((item) => item.level === 'FAIL').length;
results.forEach((item) => {
  console.log('[' + item.level + '] ' + item.name + (item.detail ? '：' + item.detail : ''));
});
console.log('\n汇总：' + passed + ' 通过，' + warnings + ' 提醒，' + failures + ' 失败；共 ' + results.length + ' 项。');
console.log('提醒项需要人工判断是否适用于当前主题；自动检查不能替代真实浏览器体验验收。');
process.exitCode = failures ? 1 : 0;
