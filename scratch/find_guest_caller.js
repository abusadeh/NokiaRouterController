const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

let pos = 0;
while (true) {
  pos = content.indexOf('setGuestNetworkData', pos);
  if (pos === -1) break;
  console.log('Pos:', pos);
  console.log(content.slice(Math.max(0, pos - 200), Math.min(content.length, pos + 400)));
  pos += 20;
}

// Also search for "guest" in component files or relativeUrls
let pos2 = 0;
while (true) {
  pos2 = content.indexOf('ConfigWhwGuest', pos2);
  if (pos2 === -1) break;
  console.log('\nConfigWhwGuest at pos:', pos2);
  console.log(content.slice(Math.max(0, pos2 - 100), Math.min(content.length, pos2 + 300)));
  pos2 += 15;
}
