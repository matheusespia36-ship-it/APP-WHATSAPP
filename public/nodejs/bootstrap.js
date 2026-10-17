const http = require('http');
const path = require('path');
let up = false;
function fallback(err) {
  if (up) return; up = true;
  const msg = String((err && err.stack) || err);
  http.createServer((q, r) => {
    r.writeHead(500, {'Content-Type':'text/plain; charset=utf-8','Access-Control-Allow-Origin':'*'});
    r.end('Falha no server.js:\n\n' + msg);
  }).listen(3000, '127.0.0.1');
}
process.on('uncaughtException', fallback);
process.on('unhandledRejection', fallback);
try { require(path.join(__dirname, 'server.js')); } catch (e) { fallback(e); }
