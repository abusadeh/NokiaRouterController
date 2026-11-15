const fs = require('fs');
const content = fs.readFileSync('C:\\Users\\Abusadeh\\AppData\\Local\\Programs\\antigravity\\router_main.js', 'utf8');

function searchAll(needle) {
    let pos = 0;
    while (true) {
        const idx = content.indexOf(needle, pos);
        if (idx === -1) break;
        console.log(`\n=== Match at ${idx} ===`);
        console.log(content.slice(Math.max(0, idx - 100), Math.min(content.length, idx + 400)));
        pos = idx + needle.length;
    }
}
searchAll('getConnectedDevicesGET(');
searchAll('getConnectedDevices(');
searchAll('getHomeNetworkStatus(');
