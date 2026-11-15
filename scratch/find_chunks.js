const fs = require('fs');
const content = fs.readFileSync('router_main.js', 'utf8');

// Find chunk files
const chunkMatches = content.match(/"[0-9]+\.[a-f0-9]+\.js"/g) || [];
console.log('Chunk files found:', chunkMatches);

// Search for guest network components or strings
const guestMatches = content.match(/"[^"]*guest[^"]*"/gi) || [];
console.log('\nGuest strings count:', guestMatches.length);
console.log(Array.from(new Set(guestMatches)).slice(0, 30));
