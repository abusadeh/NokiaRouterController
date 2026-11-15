const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

let pos = content.indexOf('getEncryptModeValue2');
console.log('Class around getEncryptModeValue2 at', pos);
console.log(content.slice(Math.max(0, pos - 1000), Math.min(content.length, pos + 1000)));
