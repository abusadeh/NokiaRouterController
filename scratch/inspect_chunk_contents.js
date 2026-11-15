const fs = require('fs');

const c30 = fs.readFileSync('chunk_30.js', 'utf8');
console.log('c30 first 300 chars:', c30.slice(0, 300));
console.log('c30 search for "guest":', c30.indexOf('guest'));
console.log('c30 search for "wifi":', c30.indexOf('wifi'));
console.log('c30 search for "wireless":', c30.indexOf('wireless'));

const c380 = fs.readFileSync('chunk_380.js', 'utf8');
console.log('\nc380 first 300 chars:', c380.slice(0, 300));
let pos = c380.indexOf('setWireless2Info');
console.log('c380 setWireless2Info:', pos);
if (pos !== -1) {
  console.log(c380.slice(Math.max(0, pos - 300), Math.min(c380.length, pos + 800)));
}

let pos2 = c380.indexOf('ConfigWhwGuest');
console.log('c380 ConfigWhwGuest:', pos2);
if (pos2 !== -1) {
  console.log(c380.slice(Math.max(0, pos2 - 300), Math.min(c380.length, pos2 + 800)));
}
