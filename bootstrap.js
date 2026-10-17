const http = require('http');
const fs = require('fs');
const path = require('path');

const dataDir = path.join(__dirname, '..', 'nodejs-data');
const tmpDir = path.join(dataDir, 'tmp');
try { fs.mkdirSync(tmpDir, { recursive: true }); } catch (e) {}

process.env.DATA_DIR = dataDir;
process.env.PORT = '3000';
process.env.TMPDIR = tmpDir;
process.env.TMP = tmpDir;
process.env.TEMP = tmpDir;

let up = false;
function fallback(err) {
  if (up) return; up = true;
  const msg = String((err && err.stack) || err);
  http.createServer((q, r) => {
    r.writeHead(500, {'Content-Type':'text/plain; charset=utf-8','Access-Control-Allow-Origin':'*'});
    r.end('Falha no server.js:\n\n' + msg);
  }).listen(3000, '127.0.0.1');
}
process.on('uncaughtException', (e) => { if (!up) fallback(e); else console.error(e); });
process.on('unhandledRejection', (e) => { console.error(e); });
try { require(path.join(__dirname, 'server.js')); } catch (e) { fallback(e); }
