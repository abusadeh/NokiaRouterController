const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

// Find getWifi2_4Details
let pos = content.indexOf('getWifi2_4Details');
console.log('=== getWifi2_4Details at', pos, '===');
if (pos !== -1) {
  console.log(content.slice(pos, pos + 2500));
}

// Find get_guest_wifi parsing
pos = content.indexOf('getGuestNetworkData');
console.log('\n=== getGuestNetworkData at', pos, '===');
if (pos !== -1) {
  console.log(content.slice(Math.max(0, pos - 200), Math.min(content.length, pos + 1500)));
}
