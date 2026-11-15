async function testAll() {
  const endpoints = [
    'device_home_nw_client_status_web_app.cgi',
    'device_home_network_status_web_app.cgi',
    'lan_status_web_app.cgi?wlan',
    'sta_info_web_app.cgi',
    'lan_status_web_app.cgi?lan',
    'dashboard_device_status_web_app.cgi',
    'wlan_config_status_web_app.cgi'
  ];
  for (const ep of endpoints) {
    try {
      const res = await fetch('http://192.168.1.254/' + ep);
      console.log(ep, 'Status:', res.status);
      const text = await res.text();
      console.log('Length:', text.length, 'Sample:', text.slice(0, 150));
    } catch(e) {
      console.error(ep, 'Error:', e.message);
    }
  }
}
testAll();
