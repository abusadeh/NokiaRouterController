const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

console.log(content.slice(144500, 146000));
