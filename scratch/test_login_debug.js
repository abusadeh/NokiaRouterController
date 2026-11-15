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
    console.log("Got nonce:", nonceData.nonce);

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

    console.log("Login status:", loginRes.status);
    console.log("Login headers:", Object.fromEntries(loginRes.headers.entries()));
    const loginText = await loginRes.text();
    console.log("Login body:", loginText);
}

main().catch(console.error);
