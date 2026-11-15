const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

// Search for SET_WIRELESS_2INFO
let pos = 0;
while (true) {
  pos = content.indexOf('SET_WIRELESS_2INFO', pos);
  if (pos === -1) break;
  console.log('SET_WIRELESS_2INFO at:', pos);
  console.log(content.slice(Math.max(0, pos - 150), Math.min(content.length, pos + 250)));
  pos += 18;
}

// Search for ssidselect= in string literals
let pos2 = 0;
while (true) {
  pos2 = content.indexOf('ssidselect', pos2);
  if (pos2 === -1) break;
  let snippet = content.slice(Math.max(0, pos2 - 50), Math.min(content.length, pos2 + 150));
  if (snippet.includes('&') || snippet.includes('=')) {
    console.log('\nssidselect param at:', pos2);
    console.log(snippet);
  }
  pos2 += 10;
}
