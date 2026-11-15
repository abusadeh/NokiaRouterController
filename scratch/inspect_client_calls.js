const fs = require('fs');
const path = 'C:\\Users\\Abusadeh\\AppData\\Local\\Programs\\antigravity\\router_main.js';
const content = fs.readFileSync(path, 'utf8');

function search(needle) {
    let pos = 0;
    console.log(`\n========================================\n=== Searching for: ${needle} ===`);
    let count = 0;
    while (count < 5) {
        const idx = content.indexOf(needle, pos);
        if (idx === -1) break;
        const start = Math.max(0, idx - 200);
        const end = Math.min(content.length, idx + 500);
        console.log(`\nMatch at ${idx}:\n` + content.substring(start, end));
        pos = idx + needle.length;
        count++;
    }
}

search('get_connected_devices');
search('device_home_nw_client_status');
search('dashboard_device_status');
search('sta_info');
search('wlan_host_list');
search('client_list');
search('device_status');
