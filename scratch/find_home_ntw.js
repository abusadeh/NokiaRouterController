const fs = require('fs');
const content = fs.readFileSync('C:\\Users\\Abusadeh\\AppData\\Local\\Programs\\antigravity\\router_main.js', 'utf8');

function findUsages(needle) {
    let pos = 0;
    console.log(`\n=================== Usages of ${needle} ===================`);
    let count = 0;
    while (count < 5) {
        const idx = content.indexOf(needle, pos);
        if (idx === -1) break;
        const start = Math.max(0, idx - 150);
        const end = Math.min(content.length, idx + 400);
        console.log(`\nMatch at ${idx}:\n` + content.substring(start, end));
        pos = idx + needle.length;
        count++;
    }
}

findUsages('.home_ntw_status');
findUsages('home_ntw_status');
findUsages('getConnectedDevices');
