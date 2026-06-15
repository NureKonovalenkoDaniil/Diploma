const fs = require('fs');
const path = require('path');

// Extract keys used in t()
const keys = new Set();
const tPattern = /\bt\(\s*['"]([a-zA-Z0-9_-]+)['"]/g;

function walk(dir) {
  const list = fs.readdirSync(dir);
  list.forEach(file => {
    const fullPath = path.join(dir, file);
    const stat = fs.statSync(fullPath);
    if (stat.isDirectory()) {
      if (file !== 'node_modules' && file !== 'dist' && file !== '.git') {
        walk(fullPath);
      }
    } else if (file.endsWith('.tsx') || file.endsWith('.ts')) {
      const content = fs.readFileSync(fullPath, 'utf8');
      let match;
      while ((match = tPattern.exec(content)) !== null) {
        keys.add(match[1]);
      }
    }
  });
}

walk(path.join(__dirname, 'src'));
const extractedKeys = Array.from(keys);

// Read LocaleContext.tsx and parse uk/en keys
const localeContent = fs.readFileSync(path.join(__dirname, 'src', 'contexts', 'LocaleContext.tsx'), 'utf8');

// Find uk: { ... } object content
const ukMatch = localeContent.match(/uk:\s*\{([\s\S]*?)\},\s*en:/);
if (!ukMatch) {
  console.error('Could not find uk dictionary in LocaleContext.tsx');
  process.exit(1);
}

const ukDictText = ukMatch[1];
const definedKeys = [];
const keyPattern = /([a-zA-Z0-9_-]+)\s*:/g;
let m;
while ((m = keyPattern.exec(ukDictText)) !== null) {
  definedKeys.push(m[1]);
}

const missing = extractedKeys.filter(k => !definedKeys.includes(k));
console.log('Defined keys count:', definedKeys.length);
console.log('Extracted keys count:', extractedKeys.length);
console.log('Missing keys:', missing);
