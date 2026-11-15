const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

let pos = content.indexOf('getWifi2_4Details(');
let defPos = content.indexOf('getWifi2_4Details(', pos + 10);
console.log('Definition at:', defPos);
if (defPos !== -1) {
  console.log(content.slice(defPos, defPos + 2500));
}
