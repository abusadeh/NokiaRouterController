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
    
    const loginRes = await fetch('http://192.168.1.254/login_web_app.cgi', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: postPayload
    });

    const loginData = await loginRes.json();
    console.log("Login result:", loginData);

    const cookie = `sid=${loginData.sid}`;
    const token = loginData.token;

    const endpoints = [
        'device_home_nw_client_status_web_app.cgi',
        'device_home_network_status_web_app.cgi',
        'lan_status_web_app.cgi?wlan',
        'sta_info_web_app.cgi',
        'lan_status_web_app.cgi?lan',
        'dashboard_device_status_web_app.cgi'
    ];

    for (const ep of endpoints) {
        console.log(`\n=================== ${ep} ===================`);
        try {
            const res = await fetch('http://192.168.1.254/' + ep, {
                headers: {
                    'Cookie': cookie,
                    'X-Auth-Token': token
                }
            });
            console.log(`Status: ${res.status}`);
            const text = await res.text();
            console.log(`Length: ${text.length}`);
            if (text.length < 3000) {
                console.log(text);
            } else {
                console.log(text.substring(0, 1500) + '\n... [TRUNCATED]');
            }
        } catch (e) {
            console.error(e.message);
        }
    }
}

main().catch(console.error);
