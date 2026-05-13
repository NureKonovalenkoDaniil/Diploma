#!/usr/bin/env node
const fs = require('fs');
const http = require('http');
const https = require('https');
const { argv } = require('process');

function loadConfig(path) {
  const raw = fs.readFileSync(path, 'utf8');
  return JSON.parse(raw);
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

function postJson(url, data) {
  return new Promise((resolve, reject) => {
    try {
      const parsed = new URL(url);
      const isHttps = parsed.protocol === 'https:';
      const payload = JSON.stringify(data);
      const opts = {
        hostname: parsed.hostname,
        port: parsed.port || (isHttps ? 443 : 80),
        path: parsed.pathname + (parsed.search || ''),
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(payload)
        }
      };

      const req = (isHttps ? https : http).request(opts, (res) => {
        let body = '';
        res.on('data', c => body += c);
        res.on('end', () => resolve({ status: res.statusCode, body }));
      });
      req.on('error', reject);
      req.write(payload);
      req.end();
    } catch (e) { reject(e); }
  });
}

async function run(cfg) {
  console.log(`[${cfg.deviceId}] Starting emulator. Sending to ${cfg.serverBaseUrl}`);

  // Attempt claim -> login -> periodic send
  async function claim() {
    try {
      const url = cfg.serverBaseUrl.replace(/\/$/, '') + '/api/iotdevice/claim';
      const res = await postJson(url, { deviceId: cfg.deviceId });
      console.log(`[${cfg.deviceId}] claim status ${res.status}`);
      return true;
    } catch (e) { console.error(`[${cfg.deviceId}] claim error`, e.message || e); return false; }
  }

  async function login() {
    try {
      const url = cfg.serverBaseUrl.replace(/\/$/, '') + '/api/auth/device-login';
      const res = await postJson(url, { deviceId: cfg.deviceId, deviceSecret: cfg.deviceSecret || '' });
      console.log(`[${cfg.deviceId}] login status ${res.status}`);
      if (res.status === 200) {
        try { const obj = JSON.parse(res.body); return obj.token || obj.Token || null; } catch { return null; }
      }
      return null;
    } catch (e) { console.error(`[${cfg.deviceId}] login error`, e.message || e); return null; }
  }

  let jwt = null;
  if (cfg.autoClaim) await claim();
  jwt = await login();

  const sendUrl = cfg.serverBaseUrl.replace(/\/$/, '') + '/api/storagecondition';

  while (true) {
    const temp = +(cfg.initialTemp + (Math.random() - 0.5) * cfg.variationTemp).toFixed(2);
    const hum  = +(cfg.initialHumidity + (Math.random() - 0.5) * cfg.variationHumidity).toFixed(2);

    const payload = {
      Temperature: temp,
      Humidity: hum,
      DeviceID: cfg.deviceId,
      Timestamp: new Date().toISOString()
    };

    try {
      const parsed = new URL(sendUrl);
      // attach token if present
      if (jwt) {
        // naive approach: include token in payload header if endpoint accepts
      }
      const res = await postJson(sendUrl, payload);
      console.log(`[${cfg.deviceId}] send status ${res.status} payload T:${temp} H:${hum}`);
    } catch (e) {
      console.error(`[${cfg.deviceId}] send error`, e.message || e);
    }

    await sleep(cfg.samplingMs || 5000);
  }
}

function printUsage() {
  console.log('Usage: node emulator.js --config <path-to-json>');
}

(async () => {
  const idx = argv.indexOf('--config');
  if (idx === -1 || !argv[idx+1]) { printUsage(); process.exit(1); }
  const cfgPath = argv[idx+1];
  const cfg = loadConfig(cfgPath);
  await run(cfg);
})();
