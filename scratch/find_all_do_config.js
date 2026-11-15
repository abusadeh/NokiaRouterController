const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

// Find all occurrences of "do_config_glb"
let pos = 0;
while (true) {
  pos = content.indexOf('do_config_glb', pos);
  if (pos === -1) break;
  console.log('Pos:', pos);
  console.log(content.slice(Math.max(0, pos - 150), Math.min(content.length, pos + 250)));
  pos += 13;
}
