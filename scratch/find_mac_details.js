const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

let pos = 0;
while (true) {
  pos = content.indexOf('del_ssid_mac_addr', pos);
  if (pos === -1) break;
  console.log('del_ssid_mac_addr at:', pos);
  console.log(content.slice(Math.max(0, pos - 200), Math.min(content.length, pos + 300)));
  pos += 17;
}

// Search for mac address regex or format in the code
let pos2 = 0;
while (true) {
  pos2 = content.indexOf('macfilter_web_app.cgi', pos2);
  if (pos2 === -1) break;
  console.log('\nmacfilter_web_app.cgi at:', pos2);
  console.log(content.slice(Math.max(0, pos2 - 100), Math.min(content.length, pos2 + 250)));
  pos2 += 21;
}
