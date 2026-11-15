const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

let pos = content.indexOf('getWifi2_4Details(');
console.log('getWifi2_4Details at', pos);
if (pos !== -1) {
  console.log(content.slice(pos, pos + 2500));
}
