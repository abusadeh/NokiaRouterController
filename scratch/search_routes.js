const fs = require('fs');
const content = fs.readFileSync('C:\\Users\\Abusadeh\\AppData\\Local\\Programs\\antigravity\\router_main.js', 'utf8');

function searchRoutes() {
    let pos = 0;
    while (true) {
        const idx = content.indexOf('path:"', pos);
        if (idx === -1) break;
        const slice = content.slice(idx, idx + 40);
        console.log(slice);
        pos = idx + 6;
    }
}
searchRoutes();
