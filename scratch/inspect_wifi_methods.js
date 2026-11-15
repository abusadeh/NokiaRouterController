const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

console.log(content.slice(154500, 160000));
