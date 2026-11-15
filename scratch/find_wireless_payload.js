const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

let pos = content.indexOf('set_wireless2_info');
console.log('set_wireless2_info at:', pos);

// Let's find where the form for set_wireless2_info is submitted
let pos2 = 0;
while (true) {
  pos2 = content.indexOf('setWireless2Info', pos2);
  if (pos2 === -1) break;
  console.log('\nsetWireless2Info at:', pos2);
  console.log(content.slice(Math.max(0, pos2 - 300), Math.min(content.length, pos2 + 300)));
  pos2 += 16;
}
