'use strict';
/* ================================================================
 * TypeScript 系列 · 跨知识点导航（共享脚本，单一数据源）
 *
 * 用法：系列内每个页面（<知识点目录>/<页面>.html）做两件事——
 *   1. <body data-tsnav="知识点id">      （见下方 SERIES 的 id）
 *   2. </body> 前引入 <script src="../系列导航.js"></script>
 * 脚本自动在页首插进度条（系列位次 + 圆点）、页尾插上一页/下一页卡片，
 * 并注册 ← / → 键盘翻页（输入框内不触发）。
 *
 * 维护：新增知识点时，只需在 SERIES 里补一条（dir + file + name），
 * 系列内所有页面同步点亮——不要在各页面里复制清单。
 * 所有页面都在 前端/TypeScript/<知识点>/ 同一层，故互相之间的相对路径
 * 统一是 '../<dir>/<file>'。
 * ================================================================ */
var TSNav = (function () {

  /* ---------- 系列数据（唯一维护点） ---------- */
  var SERIES = [
    { id: 'js-basics',    no: 1,  name: 'JavaScript 极速地基', dir: 'JavaScript极速地基', file: '互动演示.html', legacy: true },
    { id: 'ts-intro',     no: 2,  name: '初识 TypeScript',    dir: '初识TypeScript',     file: '互动演示.html', legacy: true },
    { id: 'basic-types',  no: 3,  name: '基础类型与收窄',      dir: '基础类型与收窄',     file: '学习页.html' },
    { id: 'fn-obj',       no: 4,  name: '函数与对象类型' },
    { id: 'inference',    no: 5,  name: '类型推导与断言' },
    { id: 'generics',     no: 6,  name: '泛型' },
    { id: 'type-gym',     no: 7,  name: '类型体操入门' },
    { id: 'oop',          no: 8,  name: '类与面向对象' },
    { id: 'dts',          no: 9,  name: '声明文件与第三方类型' },
    { id: 'tsconfig',     no: 10, name: 'tsconfig 与工程化' },
    { id: 'capstone',     no: 11, name: '综合实战' }
  ];

  function href(item) {
    if (!item || !item.dir || !item.file) return null;
    return '../' + item.dir + '/' + item.file;
  }
  function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  /* ---------- 样式（一次性注入，全部 .tsnav- 前缀，不碰宿主页面） ---------- */
  var CSS = ''
    + '.tsnav-strip{background:#fff;border-bottom:1px solid #e5e7eb;font-size:12.5px;}'
    + '.tsnav-strip .tsnav-in{max-width:1180px;margin:0 auto;padding:7px 16px;display:flex;align-items:center;gap:10px;flex-wrap:wrap;}'
    + '.tsnav-label{font-weight:700;color:#1f2937;white-space:nowrap;}'
    + '.tsnav-count{color:#6b7280;white-space:nowrap;}'
    + '.tsnav-dots{display:flex;gap:6px;align-items:center;flex:1;min-width:0;}'
    + '.tsnav-dot{width:9px;height:9px;border-radius:50%;border:1.5px solid #cbd5e1;display:inline-block;flex:none;}'
    + 'a.tsnav-dot{border-color:#2563eb;background:#dbeafe;}'
    + 'a.tsnav-dot:hover{background:#2563eb;}'
    + '.tsnav-dot.tsnav-cur{border-color:#2563eb;background:#2563eb;box-shadow:0 0 0 3px #dbeafe;}'
    + '.tsnav-arrow{color:#2563eb;text-decoration:none;white-space:nowrap;padding:2px 8px;border:1px solid #bfdbfe;border-radius:99px;}'
    + '.tsnav-arrow:hover{background:#eff6ff;}'
    + '.tsnav-arrow.tsnav-off{color:#cbd5e1;border-color:#e5e7eb;cursor:default;}'
    + '.tsnav-cards{max-width:1180px;margin:0 auto 30px;padding:0 16px;display:grid;grid-template-columns:1fr 1fr;gap:12px;}'
    + '.tsnav-card{border:1px solid #e5e7eb;background:#fff;border-radius:10px;padding:12px 14px;text-decoration:none;color:inherit;display:block;}'
    + 'a.tsnav-card:hover{border-color:#2563eb;box-shadow:0 1px 6px rgba(37,99,235,.15);}'
    + '.tsnav-card.tsnav-next{text-align:right;}'
    + '.tsnav-card.tsnav-off{background:#f8fafc;color:#94a3b8;}'
    + '.tsnav-cap{font-size:11.5px;color:#6b7280;margin-bottom:3px;}'
    + '.tsnav-nm{font-size:14px;font-weight:700;}'
    + '.tsnav-badge{font-size:11px;padding:1px 7px;border-radius:99px;border:1px solid #e5e7eb;margin-left:6px;white-space:nowrap;font-weight:400;}'
    + '.tsnav-badge.tsnav-legacy{color:#b45309;border-color:#fcd34d;background:#fffbeb;}'
    + '.tsnav-badge.tsnav-plan{color:#94a3b8;}'
    + '.tsnav-hint{font-size:11.5px;color:#94a3b8;margin-top:4px;}'
    + '@media (max-width:640px){.tsnav-cards{grid-template-columns:1fr;}.tsnav-card.tsnav-next{text-align:left;}}';
  var styled = false;
  function ensureStyle(doc) {
    if (styled) return;
    var s = doc.createElement('style');
    s.textContent = CSS;
    (doc.head || doc.body).appendChild(s);
    styled = true;
  }

  /* ---------- 渲染 ---------- */
  function dotHTML(item, currentId) {
    var tip = item.name + (item.legacy ? '（旧格式页）' : '') + (href(item) ? '' : '（待建）');
    if (item.id === currentId) return '<span class="tsnav-dot tsnav-cur" title="' + esc(item.name) + '（当前页）"></span>';
    if (href(item)) return '<a class="tsnav-dot" href="' + esc(href(item)) + '" title="' + esc(tip) + '"></a>';
    return '<span class="tsnav-dot" title="' + esc(tip) + '"></span>';
  }
  function arrowHTML(item, dir) {
    var label = dir === 'prev' ? '‹ 上一页' : '下一页 ›';
    if (href(item)) return '<a class="tsnav-arrow" href="' + esc(href(item)) + '" title="' + esc(item.name) + '（快捷键 ← / →）">' + label + '</a>';
    return '<span class="tsnav-arrow tsnav-off" title="待建">' + label + '</span>';
  }
  function cardHTML(item, dir) {
    var cap = dir === 'prev' ? '上一页' : '下一页';
    var cls = 'tsnav-card' + (dir === 'next' ? ' tsnav-next' : '');
    var badge = '';
    if (item && item.legacy) badge = '<span class="tsnav-badge tsnav-legacy">旧格式</span>';
    if (!href(item)) badge = '<span class="tsnav-badge tsnav-plan">待建</span>';
    if (href(item)) {
      return '<a class="' + cls + '" href="' + esc(href(item)) + '">'
        + '<div class="tsnav-cap">' + cap + '</div>'
        + '<div class="tsnav-nm">' + esc(item.no + ' · ' + item.name) + badge + '</div>'
        + (item.legacy ? '<div class="tsnav-hint">旧格式知识点：讲解在 知识详解.md，此入口为互动演示</div>' : '')
        + '</a>';
    }
    return '<div class="' + cls + ' tsnav-off">'
      + '<div class="tsnav-cap">' + cap + '</div>'
      + '<div class="tsnav-nm">' + esc(item.no + ' · ' + item.name) + badge + '</div>'
      + '<div class="tsnav-hint">小批推进中，建成即在此点亮</div>'
      + '</div>';
  }

  /* ---------- 挂载 ---------- */
  function mount(currentId) {
    var doc = document;
    var idx = -1, i;
    for (i = 0; i < SERIES.length; i++) if (SERIES[i].id === currentId) idx = i;
    if (idx < 0 || !doc.body) return false;
    ensureStyle(doc);

    var prev = idx > 0 ? SERIES[idx - 1] : null;
    var next = idx < SERIES.length - 1 ? SERIES[idx + 1] : null;
    var cur = SERIES[idx];

    /* 页首：系列位次条 */
    var dots = '';
    for (i = 0; i < SERIES.length; i++) dots += dotHTML(SERIES[i], currentId);
    var strip = doc.createElement('div');
    strip.className = 'tsnav-strip';
    strip.innerHTML = '<div class="tsnav-in">'
      + '<span class="tsnav-label">TypeScript 系列</span>'
      + '<span class="tsnav-count">' + cur.no + ' / ' + SERIES.length + ' · ' + esc(cur.name) + '</span>'
      + '<span class="tsnav-dots">' + dots + '</span>'
      + arrowHTML(prev, 'prev')
      + arrowHTML(next, 'next')
      + '</div>';
    doc.body.insertBefore(strip, doc.body.firstChild);

    /* 页尾：上一页 / 下一页卡片 */
    var cards = doc.createElement('div');
    cards.className = 'tsnav-cards';
    cards.innerHTML =
      (prev ? cardHTML(prev, 'prev') : '<div class="tsnav-card tsnav-off"><div class="tsnav-cap">上一页</div><div class="tsnav-nm">已是系列第一个知识点</div></div>')
      + (next ? cardHTML(next, 'next') : '<div class="tsnav-card tsnav-card tsnav-next tsnav-off"><div class="tsnav-cap">下一页</div><div class="tsnav-nm">已是系列最后一个知识点</div></div>');
    doc.body.appendChild(cards);

    /* 键盘翻页：← / →（输入框内不触发；带修饰键不劫持） */
    doc.addEventListener('keydown', function (e) {
      if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
      if (e.altKey || e.ctrlKey || e.metaKey) return;
      var t = e.target;
      var tag = t && t.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || (t && t.isContentEditable)) return;
      var it = e.key === 'ArrowLeft' ? prev : next;
      var h = href(it);
      if (h) { e.preventDefault(); location.href = h; }
    });
    return true;
  }

  /* 自动挂载：真实浏览器里 <body data-tsnav="..."> 即生效 */
  if (typeof document !== 'undefined' && document.body
      && typeof document.body.getAttribute === 'function'
      && document.body.getAttribute('data-tsnav')) {
    mount(document.body.getAttribute('data-tsnav'));
  }

  return { mount: mount, SERIES: SERIES, href: href };
})();
