// Test query to wlan_config_status_web_app.cgi
async function test() {
  try {
    const res = await fetch('http://192.168.1.254/wlan_config_status_web_app.cgi');
    console.log('Status:', res.status);
    const text = await res.text();
    console.log('Body length:', text.length);
    console.log('Sample:', text.slice(0, 300));
  } catch (e) {
    console.error(e);
  }
}
test();
