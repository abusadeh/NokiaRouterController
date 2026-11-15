const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

let pos = 0;
while (true) {
  pos = content.indexOf('"setWireless2Info"', pos);
  if (pos === -1) break;
  console.log('"setWireless2Info" at:', pos);
  console.log(content.slice(Math.max(0, pos - 500), Math.min(content.length, pos + 500)));
  pos += 18;
}
