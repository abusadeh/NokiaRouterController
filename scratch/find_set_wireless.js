const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

let pos = 0;
while (true) {
  pos = content.indexOf('SET_WIRELESS_2INFO', pos);
  if (pos === -1) break;
  console.log('Pos:', pos);
  console.log(content.slice(Math.max(0, pos - 200), Math.min(content.length, pos + 400)));
  pos += 18;
}

// Search for 'do_config_glb'
pos = 0;
while (true) {
  pos = content.indexOf('do_config_glb', pos);
  if (pos === -1) break;
  console.log('\ndo_config_glb Pos:', pos);
  console.log(content.slice(Math.max(0, pos - 200), Math.min(content.length, pos + 400)));
  pos += 13;
}
