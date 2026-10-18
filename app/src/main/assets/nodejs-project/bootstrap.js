process.env.DATA_DIR = process.env.DATA_DIR || require('path').join(__dirname, 'data');
process.env.PUBLIC_DIR = process.env.PUBLIC_DIR || require('path').join(__dirname, 'public');

process.on('uncaughtException', (err) => {
  console.error('[BOOTSTRAP] Exceção capturada:', err);
});

process.on('unhandledRejection', (reason) => {
  console.error('[BOOTSTRAP] Rejeição capturada:', reason);
});

try {
  require('./server.js');
} catch (e) {
  console.error('[BOOTSTRAP] Erro ao carregar server.js:', e);
}
