const fs = require('fs');

console.log('=== CHUNK 30 ===');
const c30 = fs.readFileSync('chunk_30.js', 'utf8');
let pos = c30.indexOf('setGuestNetworkData');
console.log('setGuestNetworkData in c30:', pos);
if (pos !== -1) {
  console.log(c30.slice(Math.max(0, pos - 300), Math.min(c30.length, pos + 1000)));
}

pos = c30.indexOf('ConfigWhwGuest');
console.log('ConfigWhwGuest in c30:', pos);
if (pos !== -1) {
  console.log(c30.slice(Math.max(0, pos - 300), Math.min(c30.length, pos + 1000)));
}
