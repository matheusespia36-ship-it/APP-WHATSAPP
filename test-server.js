const http = require('http');

const options = {
  hostname: '127.0.0.1',
  port: 3000,
  path: '/status',
  method: 'GET',
  headers: {
    'x-token': 'minhasenha'
  }
};

const req = http.request(options, (res) => {
  console.log(`STATUS: ${res.statusCode}`);
  let data = '';
  res.on('data', (chunk) => { data += chunk; });
  res.on('end', () => {
    console.log('RESPOSTA:', data);
  });
});

req.on('error', (e) => {
  console.error(`Problema com a requisição: ${e.message}`);
});

req.end();
