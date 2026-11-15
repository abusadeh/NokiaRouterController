const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

// Search for methods in class L
let chunk = content.slice(145500, 160000);
let methods = chunk.match(/[a-zA-Z0-9_]+\s*\([^)]*\)\s*\{/g) || [];
console.log('Methods in class L:');
methods.forEach(m => console.log(' ', m));
