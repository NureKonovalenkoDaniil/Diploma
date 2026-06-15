const fs = require('fs');
const path = require('path');

const cyrillicPattern = /[а-яА-ЯёЁіІїЇєЄґҐ]/;
const report = {};

function checkDir(dir) {
  const list = fs.readdirSync(dir);
  list.forEach(file => {
    const fullPath = path.join(dir, file);
    const stat = fs.statSync(fullPath);
    if (stat.isDirectory()) {
      if (file !== 'node_modules' && file !== 'dist' && file !== '.git') {
        checkDir(fullPath);
      }
    } else if (file.endsWith('.tsx') && !file.endsWith('.test.tsx')) {
      const content = fs.readFileSync(fullPath, 'utf8');
      const lines = content.split('\n');
      const matches = [];
      lines.forEach((line, idx) => {
        if (cyrillicPattern.test(line)) {
          matches.push({ lineNum: idx + 1, text: line.trim() });
        }
      });
      if (matches.length > 0) {
        const relative = path.relative(path.join(__dirname, 'src'), fullPath);
        report[relative] = matches;
      }
    }
  });
}

checkDir(path.join(__dirname, 'src'));
fs.writeFileSync(path.join(__dirname, 'cyrillic_report.json'), JSON.stringify(report, null, 2));
console.log('Report written successfully!');
