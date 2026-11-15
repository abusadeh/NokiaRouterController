const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

// Find class M{constructor
let pos = content.indexOf('class M{constructor');
console.log('class M at:', pos);
if (pos !== -1) {
  console.log(content.slice(pos, pos + 500));
}

// Find class R{constructor
pos = content.indexOf('class R{constructor');
console.log('class R at:', pos);
if (pos !== -1) {
  console.log(content.slice(pos, pos + 500));
}
