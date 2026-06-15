const fs = require('fs');
const path = require('path');

const keys = new Set();
const tPattern = /t\(\s*['"]([a-zA-Z0-9_-]+)['"]/g;

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
console.log(JSON.stringify(Array.from(keys).sort(), null, 2));
