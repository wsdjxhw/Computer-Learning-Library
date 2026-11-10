#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const vm = require('vm');

const target = process.argv[2];
if (!target) {
  console.error('用法：node verify_page_runtime.js <学习页.html>');
  process.exit(2);
}
const file = path.resolve(target);
const html = fs.readFileSync(file, 'utf8');

const VOID_TAGS = new Set(['area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input', 'link', 'meta', 'param', 'source', 'track', 'wbr']);
const TAG_RE = /<\/?([a-zA-Z][\w:-]*)([^>]*)>/g;
const ATTR_RE = /([a-zA-Z_:][-\w:.]*)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'<>`]+)))?/g;

function parseAttrs(raw) {
  const attrs = {};
  let match;
  ATTR_RE.lastIndex = 0;
  while ((match = ATTR_RE.exec(raw))) {
    attrs[match[1].toLowerCase()] = match[2] !== undefined ? match[2] : (match[3] !== undefined ? match[3] : (match[4] !== undefined ? match[4] : ''));
  }
  return attrs;
}

class MiniDocument {
  constructor() {
    this.ids = new Map();
    this.root = this.createElement('html');
    this.body = null;
    this.documentElement = this.root;
  }
  createElement(tag) {
    return new MiniElement(tag.toLowerCase(), {}, this);
  }
  createTextNode(text) {
    return { nodeType: 3, text: String(text), parentNode: null };
  }
  appendChild(node) { return this.root.appendChild(node); }
  removeChild(node) { return this.root.removeChild(node); }
  getElementById(id) { return this.ids.get(id) || null; }
  querySelector(selector) { return queryAll(this.root, selector)[0] || null; }
  querySelectorAll(selector) { return queryAll(this.root, selector); }
  execCommand() { return true; }
}

class MiniElement {
  constructor(tagName, attrs, doc) {
    this.nodeType = 1;
    this.tagName = tagName;
    this.attrs = attrs;
    this.doc = doc;
    this.childNodes = [];
    this.parentNode = null;
    this.listeners = {};
    this.style = {};
    this.hidden = Object.prototype.hasOwnProperty.call(attrs, 'hidden');
    this.disabled = Object.prototype.hasOwnProperty.call(attrs, 'disabled');
    this.checked = false;
    this.value = '';
    if (this.tagName === 'input' || this.tagName === 'textarea' || this.tagName === 'select') { this.value = attrs.value || ''; }
    if (attrs.id) doc.ids.set(attrs.id, this);
  }
  get id() { return this.attrs.id || ''; }
  set id(value) { this.attrs.id = value; this.doc.ids.set(value, this); }
  get className() { return this.attrs.class || ''; }
  set className(value) { this.attrs.class = value; }
  get classList() {
    const self = this;
    return {
      contains(token) { return self.className.split(/\s+/).indexOf(token) >= 0; },
      add() {
        const tokens = self.className.split(/\s+/).filter(Boolean);
        Array.prototype.forEach.call(arguments, function (token) { if (tokens.indexOf(token) < 0) tokens.push(token); });
        self.className = tokens.join(' ');
      },
      remove() {
        const removeTokens = Array.prototype.slice.call(arguments);
        self.className = self.className.split(/\s+/).filter(function (token) { return token && removeTokens.indexOf(token) < 0; }).join(' ');
      },
      toggle(token, force) {
        const has = this.contains(token);
        const shouldAdd = force === undefined ? !has : !!force;
        if (shouldAdd && !has) this.add(token);
        if (!shouldAdd && has) this.remove(token);
        return shouldAdd;
      }
    };
  }
  get children() { return this.childNodes.filter(function (node) { return node.nodeType === 1; }); }
  get nextSibling() {
    if (!this.parentNode) return null;
    const index = this.parentNode.childNodes.indexOf(this);
    return index >= 0 && index + 1 < this.parentNode.childNodes.length ? this.parentNode.childNodes[index + 1] : null;
  }
  get parentNode() { return this._parentNode || null; }
  set parentNode(value) { this._parentNode = value; }
  get textContent() {
    return this.childNodes.map(function (node) { return node.nodeType === 3 ? node.text : node.textContent; }).join('');
  }
  set textContent(value) {
    this.childNodes.slice().forEach(function (node) {
      if (node.parentNode) node.parentNode.removeChild(node);
    });
    if (value !== '') this.appendChild(this.doc.createTextNode(value));
  }
  get innerHTML() { return this.childNodes.map(serializeNode).join(''); }
  set innerHTML(value) {
    this.childNodes.slice().forEach(function (node) {
      if (node.parentNode) node.parentNode.removeChild(node);
    });
    parseInto(this, String(value), false);
  }
  setAttribute(name, value) {
    const key = name.toLowerCase();
    this.attrs[key] = String(value);
    if (key === 'id') this.doc.ids.set(String(value), this);
  }
  getAttribute(name) { const key = name.toLowerCase(); return Object.prototype.hasOwnProperty.call(this.attrs, key) ? this.attrs[key] : null; }
  removeAttribute(name) {
    const key = name.toLowerCase();
    if (key === 'id' && this.attrs.id) this.doc.ids.delete(this.attrs.id);
    delete this.attrs[key];
  }
  hasAttribute(name) { return Object.prototype.hasOwnProperty.call(this.attrs, name.toLowerCase()); }
  appendChild(node) {
    if (node.parentNode) node.parentNode.removeChild(node);
    node.parentNode = this;
    this.childNodes.push(node);
    return node;
  }
  insertBefore(node, ref) {
    if (node.parentNode) node.parentNode.removeChild(node);
    const index = ref ? this.childNodes.indexOf(ref) : -1;
    node.parentNode = this;
    if (index < 0) this.childNodes.push(node);
    else this.childNodes.splice(index, 0, node);
    return node;
  }
  removeChild(node) {
    const index = this.childNodes.indexOf(node);
    if (index >= 0) this.childNodes.splice(index, 1);
    node.parentNode = null;
    return node;
  }
  addEventListener(type, handler) {
    if (!this.listeners[type]) this.listeners[type] = [];
    this.listeners[type].push(handler);
  }
  removeEventListener(type, handler) {
    if (!this.listeners[type]) return;
    this.listeners[type] = this.listeners[type].filter(function (item) { return item !== handler; });
  }
  dispatch(type, event) {
    (this.listeners[type] || []).slice().forEach(function (handler) { handler.call(this, event || {}); }, this);
  }
  click() { this.dispatch('click'); }
  focus() {}
  blur() {}
  select() {}
  scrollIntoView() {}
  querySelector(selector) { return queryAll(this, selector)[0] || null; }
  querySelectorAll(selector) { return queryAll(this, selector); }
  closest(selector) {
    let node = this;
    while (node) {
      if (node.nodeType === 1 && matchesSelector(node, selector)) return node;
      node = node.parentNode;
    }
    return null;
  }
}

function serializeNode(node) {
  if (node.nodeType === 3) return String(node.text);
  const attrs = Object.keys(node.attrs).map(function (key) { return ' ' + key + '="' + String(node.attrs[key]).replace(/"/g, '&quot;') + '"'; }).join('');
  const inner = node.childNodes.map(serializeNode).join('');
  return '<' + node.tagName + attrs + '>' + inner + '</' + node.tagName + '>';
}

function parseInto(parent, source, registerBody) {
  const stack = [parent];
  let position = 0;
  let parsedTags = 0;
  TAG_RE.lastIndex = 0;
  let match;
  while (position < source.length) {
    TAG_RE.lastIndex = position;
    match = TAG_RE.exec(source);
    if (!match) {
      appendText(stack[stack.length - 1], source.slice(position));
      break;
    }
    parsedTags++;
    if (process.env.DEBUG_MINI_DOM && parsedTags % 500 === 0) {
      console.error('mini-dom tag #' + parsedTags + ' pos=' + position + '/' + source.length + ' tag=' + match[0].slice(0, 40));
    }
    if (match.index > position) appendText(stack[stack.length - 1], source.slice(position, match.index));
    const isClosing = match[0].charAt(1) === '/';
    const tag = match[1].toLowerCase();
    const attrs = parseAttrs(match[2]);
    const selfClosing = match[2].trim().endsWith('/') || VOID_TAGS.has(tag);
    let nextPosition = TAG_RE.lastIndex;
    if (isClosing) {
      for (let i = stack.length - 1; i > 0; i--) {
        if (stack[i].tagName === tag) { stack.length = i; break; }
      }
    } else {
      const owner = stack[stack.length - 1].nodeType === 1 ? stack[stack.length - 1].doc : stack[stack.length - 1];
      const element = new MiniElement(tag, attrs, owner);
      stack[stack.length - 1].appendChild(element);
      if (registerBody && tag === 'body') owner.body = element;
      if (!selfClosing) stack.push(element);
      if (tag === 'script') {
        const closeIndex = source.indexOf('</script>', TAG_RE.lastIndex);
        if (closeIndex >= 0) nextPosition = closeIndex;
      }
    }
    position = nextPosition;
  }
}
function appendText(parent, text) {
  if (!text) return;
  const owner = parent.nodeType === 1 ? parent.doc : parent;
  const node = owner.createTextNode(text);
  parent.appendChild(node);
}
function parseSimpleSelector(part) {
  const token = { tag: null, id: null, classes: [], attrs: [] };
  const tagMatch = part.match(/^[a-zA-Z][\w:-]*/);
  if (tagMatch) token.tag = tagMatch[0].toLowerCase();
  let rest = part;
  if (tagMatch) rest = rest.slice(tagMatch[0].length);
  const idMatch = rest.match(/#([\w-]+)/);
  if (idMatch) { token.id = idMatch[1]; rest = rest.replace('#' + idMatch[1], ''); }
  const classMatches = rest.match(/\.[\w-]+/g) || [];
  token.classes = classMatches.map(function (item) { return item.slice(1); });
  const attrMatches = rest.match(/\[[^\]]+\]/g) || [];
  token.attrs = attrMatches.map(function (item) {
    const body = item.slice(1, -1);
    const attrMatch = body.match(/^([\w:-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=]+)))?$/);
    if (!attrMatch) return { name: body, value: null };
    return { name: attrMatch[1], value: attrMatch[2] !== undefined ? attrMatch[2] : (attrMatch[3] !== undefined ? attrMatch[3] : (attrMatch[4] !== undefined ? attrMatch[4] : null)) };
  });
  return token;
}
function matchesSelector(node, selector) {
  const parts = selector.split(/\s+/).filter(Boolean).map(parseSimpleSelector);
  if (!parts.length) return false;
  if (!elementMatchesToken(node, parts[parts.length - 1])) return false;
  let current = node.parentNode;
  for (let i = parts.length - 2; i >= 0; i--) {
    let matched = false;
    while (current && current.nodeType === 1) {
      if (elementMatchesToken(current, parts[i])) { matched = true; break; }
      current = current.parentNode;
    }
    if (!matched) return false;
    current = current && current.parentNode;
  }
  return true;
}
function elementMatchesToken(node, token) {
  if (token.tag && node.tagName !== token.tag) return false;
  if (token.id && node.id !== token.id) return false;
  const classes = node.className.split(/\s+/);
  if (token.classes.some(function (item) { return classes.indexOf(item) < 0; })) return false;
  return token.attrs.every(function (attr) {
    const value = node.getAttribute(attr.name);
    if (value === null) return false;
    if (attr.value === null || attr.value === '') return true;
    return value === attr.value;
  });
}
function queryAll(root, selector) {
  const results = [];
  const selectorParts = selector.split(',').map(function (item) { return item.trim(); }).filter(Boolean);
  function walk(node) {
    node.childNodes.forEach(function (child) {
      if (child.nodeType !== 1) return;
      if (selectorParts.some(function (part) { return matchesSimplePath(child, part); })) results.push(child);
      walk(child);
    });
  }
  function matchesSimplePath(node, selector) {
    const parts = selector.split(/\s+/).filter(Boolean).map(parseSimpleSelector);
    if (!parts.length) return false;
    if (!elementMatchesToken(node, parts[parts.length - 1])) return false;
    let current = node.parentNode;
    for (let i = parts.length - 2; i >= 0; i--) {
      let found = false;
      while (current && current.nodeType === 1) {
        if (elementMatchesToken(current, parts[i])) { found = true; break; }
        current = current.parentNode;
      }
      if (!found) return false;
      current = current && current.parentNode;
    }
    return true;
  }
  if (root.nodeType === 1 && selectorParts.some(function (part) { return matchesSimplePath(root, part); })) results.push(root);
  walk(root);
  return results;
}

function extractInlineScripts(source) {
  const scripts = [];
  const pattern = /<script\b([^>]*)>([\s\S]*?)<\/script>/gi;
  let match;
  while ((match = pattern.exec(source))) {
    if (!/\bsrc=/i.test(match[1])) scripts.push(match[2]);
  }
  return scripts;
}

function createStorage(shouldFail) {
  const data = new Map();
  return {
    getItem(key) { if (shouldFail) throw new Error('storage denied'); return data.has(key) ? data.get(key) : null; },
    setItem(key, value) { if (shouldFail) throw new Error('storage denied'); data.set(key, String(value)); },
    removeItem(key) { if (shouldFail) throw new Error('storage denied'); data.delete(key); }
  };
}

function instrumentPageCode(code) {
  const markers = [
    'Object.keys(SNIPPETS).forEach',
    'renderTrack();',
    'renderRouter();',
    'renderExercises();',
    'renderQuiz();',
    'initNavigation();',
    'initSectionActions();',
    'initResume();',
    'initBaseControls();',
    'wrapTables();',
    'enhanceCodeBlocks();',
    'updateReadProgress();',
    'updateProgress();'
  ];
  markers.forEach(function (marker) {
    code = code.split(marker).join("console.error('page-step: " + marker + "'); " + marker);
  });
  code = code.replace(
    '  sections.forEach(function (section, i) {',
    "  sections.forEach(function (section, i) { console.error('page-section: ' + section.id);"
  );
  return code;
}

function runPage(source, shouldFail) {
  if (process.env.DEBUG_MINI_DOM) console.error('mini-dom: start parse');
  const doc = new MiniDocument();
  parseInto(doc, source, true);
  if (process.env.DEBUG_MINI_DOM) console.error('mini-dom: parse done, nodes=' + doc.root.childNodes.length);
  if (!doc.body) throw new Error('页面缺少 body');
  const storage = createStorage(shouldFail);
  const sandbox = {
    document: doc,
    navigator: {},
    window: {
      localStorage: storage,
      isSecureContext: false,
      confirm: function () { return true; },
      setTimeout: function () { return 0; },
      scrollTo: function () {},
      getComputedStyle: function () { return {}; }
    },
    console
  };
  sandbox.window.window = sandbox.window;
  sandbox.globalThis = sandbox;
  const context = vm.createContext(sandbox);
  const scripts = extractInlineScripts(source);
  scripts.forEach(function (code, index) {
    if (process.env.DEBUG_MINI_DOM) console.error('mini-dom: run inline script ' + (index + 1));
    const executable = process.env.DEBUG_MINI_DOM ? instrumentPageCode(code) : code;
    new vm.Script(executable, { filename: file + '#inline-' + (index + 1) }).runInContext(context);
    if (process.env.DEBUG_MINI_DOM) console.error('mini-dom: inline script ' + (index + 1) + ' done');
  });
  return { doc, sandbox };
}

let failures = 0;
function assert(condition, message) {
  if (condition) console.log('[PASS] ' + message);
  else { failures++; console.log('[FAIL] ' + message); }
}
function text(node) { return node ? node.textContent : ''; }
function count(nodes) { return nodes ? nodes.length : 0; }

const normal = runPage(html, false);
const doc = normal.doc;
const sections = doc.querySelectorAll('[data-learning-section]');
const sideLinks = doc.querySelectorAll('nav.side a');
const mobileLinks = doc.querySelectorAll('.mobile-nav a');
const completeButtons = doc.querySelectorAll('[data-section-complete]');
const quizzes = doc.querySelectorAll('.quiz');
const textareas = doc.querySelectorAll('textarea');
const pres = doc.querySelectorAll('pre');
const wrappedPres = doc.querySelectorAll('.code-block pre');
const tables = doc.querySelectorAll('table');
const wrappedTables = doc.querySelectorAll('.table-wrap table');
console.log('[INFO] 章节=' + sections.length + '，练习=' + quizzes.length + '，草稿框=' + textareas.length + '，代码块=' + pres.length);

assert(doc.querySelectorAll('h1').length === 1, '页面只有一个主标题');
assert(sections.length > 0, '页面初始化出学习章节');
assert(completeButtons.length === sections.length, '每个学习章节都有主动完成按钮');
if (sideLinks.length && mobileLinks.length) {
  assert(sideLinks.length === mobileLinks.length, '移动端目录与桌面目录一致');
}
if (pres.length) {
  assert(wrappedPres.length === pres.length, '所有代码块都获得复制入口');
}
if (tables.length) {
  assert(wrappedTables.length === tables.length, '所有表格都进入横向滚动容器');
}
if (doc.getElementById('storageNote')) {
  assert(doc.getElementById('storageNote').hidden === true, '正常存储时不显示降级提示');
}

const completeButton = doc.querySelector('[data-section-complete]');
if (completeButton) {
  completeButton.click();
  assert(completeButton.getAttribute('aria-pressed') === 'true', '章节完成按钮可标记');
  completeButton.click();
  assert(completeButton.getAttribute('aria-pressed') === 'false', '章节完成状态可撤销');
}

const firstQuiz = doc.querySelector('.quiz');
const quizOption = firstQuiz ? firstQuiz.querySelector('button.opt') : null;
if (firstQuiz && quizOption) {
  quizOption.click();
  const status = firstQuiz.querySelector('.status') || firstQuiz.querySelector('[aria-live]');
  assert(!!status && text(status).indexOf('未做') < 0, '首个固定答案题作答后有状态反馈');
}

const draft = doc.querySelector('textarea');
if (draft) {
  draft.value = 'runtime draft';
  draft.dispatch('input');
  assert(draft.value === 'runtime draft', '代码草稿输入不报错且保留在控件中');
}

if (doc.getElementById('btnResetProgress')) {
  doc.getElementById('btnResetProgress').click();
  assert(true, '练习重置链路可执行');
}
if (doc.getElementById('btnResetRead')) {
  doc.getElementById('btnResetRead').click();
  assert(true, '阅读重置链路可执行');
}

const denied = runPage(html, true);
const deniedSections = denied.doc.querySelectorAll('[data-learning-section]');
assert(deniedSections.length === sections.length, '禁用存储时页面仍完整初始化');
if (denied.doc.getElementById('storageNote')) {
  assert(denied.doc.getElementById('storageNote').hidden === false, '禁用存储时显示降级说明');
}
const deniedDraft = denied.doc.querySelector('textarea');
if (deniedDraft) {
  deniedDraft.value = 'draft in memory';
  deniedDraft.dispatch('input');
  assert(deniedDraft.value === 'draft in memory', '禁用存储时练习仍可使用');
}

console.log('\n运行时验证：' + (failures ? '发现 ' + failures + ' 项失败。' : '全部通用断言通过。'));
console.log('此脚本验证通用初始化与交互链路；页面专属答案、演示步骤和内容一致性仍需按 Skill 要求人工或专项验证。');
process.exitCode = failures ? 1 : 0;
