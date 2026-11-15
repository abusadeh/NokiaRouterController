const fs = require('fs');
const content = fs.readFileSync('C:\\Users\\Abusadeh\\AppData\\Local\\Programs\\antigravity\\router_main.js', 'utf8');

// Let's find class V or getHomeNetworkStatus calls
function findHomeNtwCalls() {
    let pos = 0;
    while (true) {
        const idx = content.indexOf('getHomeNetworkStatus', pos);
        if (idx === -1) break;
        console.log(`\n=== getHomeNetworkStatus at ${idx} ===`);
        console.log(content.slice(Math.max(0, idx - 100), Math.min(content.length, idx + 400)));
        pos = idx + 20;
    }
}
findHomeNtwCalls();
