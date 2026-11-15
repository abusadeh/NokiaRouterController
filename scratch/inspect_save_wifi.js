const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

// Find where setWireless2Info is called in components
let pos = content.indexOf('saveWifiDetails');
if (pos === -1) pos = content.indexOf('saveWifi');
if (pos === -1) pos = content.indexOf('onSave');
console.log('Save match:', pos);

// Let's search for "setWireless2Info" usage
let pos2 = 0;
while (true) {
  pos2 = content.indexOf('setWireless2Info', pos2);
  if (pos2 === -1) break;
  console.log('\nsetWireless2Info at:', pos2);
  console.log(content.slice(Math.max(0, pos2 - 150), Math.min(content.length, pos2 + 400)));
  pos2 += 16;
}
