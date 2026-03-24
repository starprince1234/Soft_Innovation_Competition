#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

const projectRoot = process.cwd();

const forbiddenRootEntries = [
  'App.vue',
  'main.js',
  'manifest.json',
  'pages.json',
  'common',
  'components',
  'config',
  'pages',
  'static',
  'stores'
];

const requiredSrcEntries = [
  'src/App.vue',
  'src/main.js',
  'src/manifest.json',
  'src/pages.json',
  'src/common',
  'src/components',
  'src/config',
  'src/pages',
  'src/static',
  'src/stores'
];

const existingForbidden = forbiddenRootEntries.filter((rel) =>
  fs.existsSync(path.join(projectRoot, rel))
);

const missingRequired = requiredSrcEntries.filter((rel) =>
  !fs.existsSync(path.join(projectRoot, rel))
);

if (existingForbidden.length === 0 && missingRequired.length === 0) {
  console.log('OK: 源码结构合规（仅 src 为业务源码目录），构建继续。');
  process.exit(0);
}

console.error('ERROR: 源码结构不合规，请保持 src 为唯一业务源码目录。');

if (existingForbidden.length > 0) {
  console.error('\n检测到不应存在的 root 源码:');
  existingForbidden.forEach((item) => console.error(`- ${item}`));
}

if (missingRequired.length > 0) {
  console.error('\n检测到缺失的 src 核心目录或入口:');
  missingRequired.forEach((item) => console.error(`- ${item}`));
}

process.exit(1);
