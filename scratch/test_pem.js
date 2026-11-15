const crypto = require('crypto');
const http = require('http');

function rsaEncrypt(pubkeyB64, buffer) {
  const clean = pubkeyB64.replace(/[\r\n\-]/g, '').replace('BEGIN PUBLIC KEY', '').replace('END PUBLIC KEY', '').trim();
  const chunked = clean.match(/.{1,64}/g).join('\n');
  const pem = `-----BEGIN PUBLIC KEY-----\n${chunked}\n-----END PUBLIC KEY-----\n`;
  return crypto.publicEncrypt({
    key: pem,
    padding: crypto.constants.RSA_PKCS1_PADDING
  }, buffer);
}

// Test with nonce
async function check() {
  const res = await fetch('http://192.168.1.254/login_web_app.cgi?nonce', { method: 'POST' });
  const data = await res.json();
  console.log('Got nonce:', data.nonce);
  console.log('Pubkey length:', data.pubkey.length);
  try {
    const enc = rsaEncrypt(data.pubkey, Buffer.from('hello'));
    console.log('Encryption SUCCESS, length:', enc.length);
  } catch(e) {
    console.error('RSA Encrypt failed:', e);
  }
}
check();
