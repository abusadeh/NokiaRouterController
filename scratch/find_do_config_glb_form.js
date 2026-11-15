const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

// Search for do_config_glb
let pos = 0;
while (true) {
  pos = content.indexOf('do_config_glb', pos);
  if (pos === -1) break;
  console.log('do_config_glb at', pos);
  console.log(content.slice(Math.max(0, pos - 50), Math.min(content.length, pos + 250)));
  pos += 13;
}

// Find form fields for wireless save
let regex = /ssidenable=|ssidname=|enableWireless=/gi;
let m;
while ((m = regex.exec(content)) !== null) {
  console.log('Param match at', m.index);
  console.log(content.slice(Math.max(0, m.index - 50), Math.min(content.length, m.index + 150)));
}
