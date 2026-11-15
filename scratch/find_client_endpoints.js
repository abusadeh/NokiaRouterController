const fs = require('fs');

const content = fs.readFileSync('router_main.js', 'utf8');

const clientEndpoints = [
  'device_home_network_status_web_app.cgi',
  'device_home_nw_client_status_web_app.cgi',
  'dashboard_device_status_web_app.cgi',
  'dashboard_ntwtopo_status_web_app.cgi',
  'lan_status_web_app.cgi?lan',
  'lan_status_web_app.cgi?wlan',
  'sta_info_web_app.cgi'
];

clientEndpoints.forEach(ep => {
  let pos = content.indexOf(ep);
  console.log(`\n=== Endpoint: ${ep} at ${pos} ===`);
  if (pos !== -1) {
    console.log(content.slice(Math.max(0, pos - 150), Math.min(content.length, pos + 300)));
  }
});
