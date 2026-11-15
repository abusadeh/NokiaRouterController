const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

// Search for act=del or add_wlan in quotes
let matches = content.match(/["'][^"']*(?:add_wlan|del_wlan|act=del|add_ethernet|macfilter)[^"']*["']/gi) || [];
console.log('Matches:', matches.slice(0, 30));
