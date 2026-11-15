const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

// Search for 'set_wireless2_info'
let pos = 0;
while (true) {
  pos = content.indexOf('set_wireless2_info', pos);
  if (pos === -1) break;
  console.log('Pos:', pos);
  console.log(content.slice(Math.max(0, pos - 100), Math.min(content.length, pos + 250)));
  pos += 18;
}

// Search for relativeUrls['set_wireless2_info'] or similar
pos = 0;
while (true) {
  pos = content.indexOf('wireless2_info', pos);
  if (pos === -1) break;
  console.log('\nwireless2_info Pos:', pos);
  console.log(content.slice(Math.max(0, pos - 100), Math.min(content.length, pos + 250)));
  pos += 14;
}
