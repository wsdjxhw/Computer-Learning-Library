# 按顺序运行本目录全部示例（模块示例的入口是 06）
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

node 01-变量与相等.js
node 02-函数与this.js
node 03-对象数组与引用.js
node 04-异步与事件循环.js
node 06-模块与导入.mjs
