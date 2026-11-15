const http = require('http');
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

function httpReq(options, body) {
    return new Promise((resolve, reject) => {
        const req = http.request(options, res => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, body: data }));
        });
        req.on('error', reject);
        if (body) req.write(body);
        req.end();
    });
}

async function main() {
    const nonceRes = await httpReq({
        hostname: '192.168.1.254',
        port: 80,
        path: '/login_web_app.cgi?nonce',
        method: 'POST',
        headers: { 'Content-Length': 0 }
    });
    const nonceData = JSON.parse(nonceRes.body);

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

    const loginRes = await httpReq({
        hostname: '192.168.1.254',
        port: 80,
        path: '/login_web_app.cgi',
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(postPayload)
        }
    }, postPayload);

    console.log("Login headers:", loginRes.headers);
    console.log("Login body:", loginRes.body);
    const cookies = loginRes.headers['set-cookie'];
    console.log("Cookies:", cookies);
    let cookie = '';
    if (cookies) {
        cookie = cookies.map(c => c.split(';')[0]).join('; ');
    }
    console.log("Merged cookie:", cookie);

    const endpoints = [
        '/lan_status_web_app.cgi?wlan',
        '/device_home_nw_client_status_web_app.cgi',
        '/sta_info_web_app.cgi',
        '/dashboard_device_status_web_app.cgi'
    ];

    for (const ep of endpoints) {
        console.log(`\n=================== Fetching ${ep} ===================`);
        const res = await httpReq({
            hostname: '192.168.1.254',
            port: 80,
            path: ep,
            method: 'GET',
            headers: {
                'Cookie': cookie
            }
        });
        console.log("Status:", res.status);
        console.log("Body length:", res.body.length);
        let plain = res.body;
        if (!plain.startsWith('<!doctype') && !plain.startsWith('{') && plain.length > 50) {
            try {
                const b64 = plain.trim().replace(/-/g, '+').replace(/_/g, '/');
                const buf = Buffer.from(b64, 'base64');
                plain = aesDecrypt(decKeyBytes, decIvBytes, buf);
                console.log("Decrypted successfully!");
            } catch(e) {
                console.log("Decryption note:", e.message);
            }
        }
        console.log(plain.substring(0, 1000));
        try {
            const parsed = JSON.parse(plain);
            console.log("\nParsed keys:", Object.keys(parsed));
            if (parsed.device_cfg) {
                console.log(`device_cfg length: ${parsed.device_cfg.length}`);
                console.log(JSON.stringify(parsed.device_cfg, null, 2));
            }
            if (parsed.aps) {
                console.log(`aps length: ${parsed.aps.length}`);
                console.log(JSON.stringify(parsed.aps, null, 2));
            }
        } catch (e) {}
    }
}

main().catch(console.error);
