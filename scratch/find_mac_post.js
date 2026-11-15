const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

let pos = 0;
while (true) {
  pos = content.indexOf('add_wlan', pos);
  if (pos === -1) break;
  console.log('add_wlan at:', pos);
  console.log(content.slice(Math.max(0, pos - 150), Math.min(content.length, pos + 250)));
  pos += 8;
}

// Find form submission for MAC filter
let pos2 = 0;
while (true) {
  pos2 = content.indexOf('addSsidMacAddress', pos2);
  if (pos2 === -1) break;
  console.log('\naddSsidMacAddress at:', pos2);
  console.log(content.slice(Math.max(0, pos2 - 300), Math.min(content.length, pos2 + 300)));
  pos2 += 17;
}
