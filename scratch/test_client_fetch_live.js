const crypto = require('crypto');

function base64urlEscape(str) {
    return str.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '.');
}

function rsaEncrypt(pubkeyB64, buffer) {
    const clean = pubkeyB64.replace(/[\r\n\-]/g, '').replace('BEGIN PUBLIC KEY', '').replace('END PUBLIC KEY', '').trim();
    const chunked = clean.match(/.{1,64}/g).join('\n');
    const pem = `-----BEGIN PUBLIC KEY-----\n${chunked}\n-----END PUBLIC KEY-----\n`;
    return crypto.publicEncrypt({
        key: pem,
        padding: crypto.constants.RSA_PKCS1_PADDING
    }, buffer);
}

function aesEncrypt(key, iv, data) {
    const cipher = crypto.createCipheriv('aes-128-cbc', key, iv);
    let encrypted = cipher.update(data, 'utf-8');
    encrypted = Buffer.concat([encrypted, cipher.final()]);
    return encrypted;
}

function aesDecrypt(key, iv, encryptedBytes) {
    const decipher = crypto.createDecipheriv('aes-128-cbc', key, iv);
    let decrypted = decipher.update(encryptedBytes);
    decrypted = Buffer.concat([decrypted, decipher.final()]);
    return decrypted.toString('utf-8');
}

function sha256Hex(text) {
    return crypto.createHash('sha256').update(text).digest('hex');
}

async function main() {
    const nonceRes = await fetch('http://192.168.1.254/login_web_app.cgi?nonce', { method: 'POST' });
    const nonceData = await nonceRes.json();

    const decKeyBytes = crypto.randomBytes(16);
    const decIvBytes = crypto.randomBytes(16);
    const decKeyStr = decKeyBytes.toString('hex');
    const decIvStr = decIvBytes.toString('hex');

    const username = 'admin';
    const password = 'Password@123';
    const userhash = sha256Hex(username);
    const passhash = sha256Hex(password);
    const response = sha256Hex(`${userhash}:${nonceData.nonce}:${passhash}`);
    const randomKeyhash = sha256Hex(`${nonceData.randomKey}:${passhash}`);

    const qStr = `userhash=${userhash}&RandomKeyhash=${randomKeyhash}&response=${response}&nonce=${encodeURIComponent(nonceData.nonce)}&enckey=${decKeyStr}&enciv=${decIvStr}&nohash=1`;
    const ephKey = crypto.randomBytes(16);
    const ephIv = crypto.randomBytes(16);
    const encQ = aesEncrypt(ephKey, ephIv, qStr);
    const keyBlock = Buffer.concat([ephKey, ephIv]);
    const encKeyBlock = rsaEncrypt(nonceData.pubkey, keyBlock);

    const postPayload = `enckey=${base64urlEscape(encKeyBlock.toString('base64'))}&enctext=${base64urlEscape(encQ.toString('base64'))}`;
    
    // In node fetch, cookies are in res.headers.getSetCookie()
    const loginRes = await fetch('http://192.168.1.254/login_web_app.cgi', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: postPayload
    });

    const setCookies = loginRes.headers.getSetCookie ? loginRes.headers.getSetCookie() : [loginRes.headers.get('set-cookie')];
    console.log("Set-Cookies:", setCookies);
    let cookie = '';
    for (const c of setCookies) {
        if (c && c.includes('sid=')) {
            cookie = c.split(';')[0];
        }
    }
    console.log("Using cookie:", cookie);

    const endpoints = [
        'lan_status_web_app.cgi?wlan',
        'device_home_nw_client_status_web_app.cgi',
        'sta_info_web_app.cgi'
    ];

    for (const ep of endpoints) {
        console.log(`\n=================== Fetching ${ep} ===================`);
        const res = await fetch('http://192.168.1.254/' + ep, {
            headers: {
                'Cookie': cookie
            }
        });
        console.log("Status:", res.status);
        const text = await res.text();
        console.log("Response text length:", text.length);
        
        let plain = text;
        if (text.startsWith('encryp_') || text.includes('encrypted_data') || /^[A-Za-z0-9+/=_-]{40,}$/.test(text.trim())) {
            try {
                // Try AES decrypt
                const clean = text.trim();
                const b64 = clean.replace(/-/g, '+').replace(/_/g, '/');
                const buf = Buffer.from(b64, 'base64');
                plain = aesDecrypt(decKeyBytes, decIvBytes, buf);
                console.log("Decrypted successfully!");
            } catch(e) {
                console.log("Decryption attempt:", e.message);
            }
        }
        
        console.log("Sample content (first 800 chars):");
        console.log(plain.substring(0, 800));
        
        try {
            const parsed = JSON.parse(plain);
            console.log("Keys in JSON:", Object.keys(parsed));
            if (parsed.device_cfg) {
                console.log(`device_cfg has ${parsed.device_cfg.length} items:`);
                console.log(JSON.stringify(parsed.device_cfg, null, 2).substring(0, 1000));
            }
            if (parsed.aps) {
                console.log(`aps has ${parsed.aps.length} items:`);
                console.log(JSON.stringify(parsed.aps, null, 2).substring(0, 1000));
            }
        } catch (e) {
            console.log("Not JSON or JSON parse error:", e.message);
        }
    }
}

main().catch(console.error);
