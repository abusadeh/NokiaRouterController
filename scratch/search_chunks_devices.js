const chunks = {
  25: "25.c04965d5fca1901b.js",
  30: "30.3ceaabb0af7ddbdc.js",
  76: "common.e3ef1010f1363c60.js",
  136: "136.c411fae9b3b49abc.js",
  189: "189.905fbd3cff9f3f36.js",
  224: "224.0a4733de8e1f0303.js",
  332: "332.88e47058f0fe5512.js",
  380: "380.d1a6641915f92ffd.js",
  402: "402.1a99c795461ff3c8.js",
  452: "452.c919cb6b3b19fbca.js",
  478: "478.465e41acf9209134.js",
  579: "579.080a1c5577dfc267.js",
  590: "590.80af0ddda37be2e8.js",
  611: "611.b7211842c8b4cef4.js",
  810: "810.a8388af6ee25409e.js",
  819: "819.6c6689d16d5df725.js"
};

async function searchInChunks() {
  for (const [id, filename] of Object.entries(chunks)) {
    try {
      const res = await fetch(`http://192.168.1.254/web_whw/${filename}`);
      if (res.ok) {
        const text = await res.text();
        const keywords = ['device_home_network_status', 'device_home_nw_client_status', 'lan_status_web_app.cgi?wlan', 'dashboard_device_status', 'home_ntw_status', 'Connected Devices', 'connected_devices'];
        const matches = keywords.filter(k => text.includes(k));
        if (matches.length > 0) {
          console.log(`Chunk ${id} (${filename}): matches ->`, matches);
          for (const m of matches) {
            let pos = text.indexOf(m);
            console.log(`  Snippet for ${m} in Chunk ${id}:`);
            console.log(text.slice(Math.max(0, pos - 100), Math.min(text.length, pos + 300)));
          }
        }
      }
    } catch (e) {
      console.error(`Error with ${filename}:`, e.message);
    }
  }
}

searchInChunks();
