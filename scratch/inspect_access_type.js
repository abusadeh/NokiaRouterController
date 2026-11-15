const fs = require('fs');

const c380 = fs.readFileSync('chunk_380.js', 'utf8');

// Find all matches for "accessType="
let matches = c380.match(/accessType=[^&"'`\s]+/g) || [];
console.log('accessType matches:', matches);

// Find code around saveGuestDetails
let pos = c380.indexOf('saveGuestDetails');
console.log(c380.slice(pos - 200, pos + 1000));
