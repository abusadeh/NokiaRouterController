const fs = require('fs');
const c380 = fs.readFileSync('chunk_380.js', 'utf8');

let pos = c380.indexOf('setWireless2Info');
console.log(c380.slice(pos - 600, pos + 800));
