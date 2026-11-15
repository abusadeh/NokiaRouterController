const fs = require('fs');

async function inspectChunk402() {
  const res = await fetch(`http://192.168.1.254/web_whw/402.1a99c795461ff3c8.js`);
  const text = await res.text();
  fs.writeFileSync('chunk_402.js', text);
  console.log('Saved chunk_402.js, size:', text.length);

  // Search for how it fetches devices
  const searches = ['home_ntw_status', 'get_connected_devices', 'client_list', 'Hosts', 'wlan', 'lan_status', 'IPAddress', 'MACAddress', 'HostName'];
  for (const s of searches) {
    let pos = 0;
    let found = 0;
    while (found < 3) {
      const idx = text.indexOf(s, pos);
      if (idx === -1) break;
      console.log(`\n[${s} at ${idx}]:`);
      console.log(text.slice(Math.max(0, idx - 100), Math.min(text.length, idx + 350)));
      pos = idx + s.length;
      found++;
    }
  }
}
inspectChunk402();
