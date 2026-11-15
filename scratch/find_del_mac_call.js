const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

let pos = content.indexOf('deleteSsidMacAddress');
while (pos !== -1) {
  console.log('deleteSsidMacAddress at', pos);
  console.log(content.slice(Math.max(0, pos - 200), Math.min(content.length, pos + 300)));
  pos = content.indexOf('deleteSsidMacAddress', pos + 20);
}

// Find any call to addSsidMacAddress
let pos2 = 0;
while (true) {
  pos2 = content.indexOf('"addSsidMacAddress"', pos2);
  if (pos2 === -1) break;
  console.log('"addSsidMacAddress" at', pos2);
  console.log(content.slice(Math.max(0, pos2 - 200), Math.min(content.length, pos2 + 300)));
  pos2 += 19;
}
