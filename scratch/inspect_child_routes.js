const fs = require('fs');
const content = fs.readFileSync('C:\\Users\\Abusadeh\\AppData\\Local\\Programs\\antigravity\\router_main.js', 'utf8');

const idx = content.indexOf('path:"",canActivate:');
console.log(content.slice(idx, idx + 2000));
