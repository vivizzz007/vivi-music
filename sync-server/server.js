'use strict';

/**
 * VIVI Music — device sync relay
 *
 * A minimal WebSocket server that lets two devices (the Android app and the
 * desktop edition) pair without an account and exchange sync snapshots.
 *
 * The server does NOT read the data it relays (beyond routing metadata).
 * TLS is terminated by the host (Render / Hugging Face) in front of this
 * process, so the client connects via `wss://`.
 *
 * State is kept in memory for speed and persisted best-effort to `data.json`
 * so pairs/mailboxes survive a restart or a free-tier sleep. On a free-tier
 * redeploy the filesystem is reset and pairs must be re-established.
 */

const { WebSocketServer, WebSocket } = require('ws');
const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = process.env.PORT || 8080;
const DATA_FILE = process.env.DATA_FILE || path.join(__dirname, 'data.json');
const PAIR_CODE_TTL_MS = 5 * 60 * 1000;

// ---------------------------------------------------------------------------
// Persistent state
//   devices   : deviceId -> { name, pairId }
//   pairs     : pairId   -> { a, b, createdAt }
//   mailboxes : deviceId -> last SyncSnapshot pushed by that device
// ---------------------------------------------------------------------------
let state = { devices: {}, pairs: {}, mailboxes: {} };
try {
  const raw = fs.readFileSync(DATA_FILE, 'utf8');
  if (raw) state = JSON.parse(raw);
} catch (_) {
  /* first run — start empty */
}

// Runtime-only state (never persisted)
const sockets = {};       // deviceId -> ws
const pendingPairs = {};  // code -> { pairId, deviceId, name, createdAt }
const pendingDisconnects = {}; // deviceId -> timeout handle (grace before unpairing)
const UNPAIR_GRACE_MS = 15 * 1000;

function persist() {
  try {
    fs.writeFileSync(DATA_FILE, JSON.stringify(state, null, 2));
  } catch (err) {
    console.error('persist failed:', err.message);
  }
}

function send(ws, obj) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(obj));
  }
}

function peerOf(deviceId) {
  const device = state.devices[deviceId];
  if (!device || !device.pairId) return null;
  const pair = state.pairs[device.pairId];
  if (!pair) return null;
  return pair.a === deviceId ? pair.b : pair.a;
}

function generateCode() {
  let code;
  do {
    code = String(Math.floor(100000 + Math.random() * 900000));
  } while (pendingPairs[code]);
  return code;
}

// ---------------------------------------------------------------------------
// Message handlers
// ---------------------------------------------------------------------------

function onMessage(ws, raw) {
  let msg;
  try {
    msg = JSON.parse(raw);
  } catch (_) {
    return;
  }
  if (!msg || !msg.deviceId) return;

  // Remember which socket belongs to this device (lazy registration).
  sockets[msg.deviceId] = ws;
  ws._deviceId = msg.deviceId;
  // A live message means the device is (back) online: cancel any pending unpair.
  if (pendingDisconnects[msg.deviceId]) {
    clearTimeout(pendingDisconnects[msg.deviceId]);
    delete pendingDisconnects[msg.deviceId];
  }
  if (!state.devices[msg.deviceId]) {
    state.devices[msg.deviceId] = { name: msg.deviceName || 'Device', pairId: null };
    persist();
  } else if (msg.deviceName) {
    state.devices[msg.deviceId].name = msg.deviceName;
  }

  switch (msg.type) {
    case 'ping':        return send(ws, { type: 'pong', timestampMs: Date.now(), echoTimestampMs: msg.timestampMs || null });
    case 'pair_request': return handlePairRequest(ws, msg);
    case 'pair_join':    return handlePairJoin(ws, msg);
    case 'sync_push':    return handleSyncPush(ws, msg);
    case 'sync_pull':    return handleSyncPull(ws, msg);
    case 'unpair':       return handleUnpair(ws, msg);
    default:             return send(ws, { type: 'pair_error', message: 'Unknown message type' });
  }
}

function handlePairRequest(ws, msg) {
  const code = generateCode();
  const pairId = crypto.randomUUID();
  pendingPairs[code] = {
    pairId,
    deviceId: msg.deviceId,
    name: msg.deviceName || 'Device',
    createdAt: Date.now(),
  };
  send(ws, { type: 'pair_code', code, pairId });
}

function handlePairJoin(ws, msg) {
  const pending = pendingPairs[msg.code];
  if (!pending || Date.now() - pending.createdAt > PAIR_CODE_TTL_MS) {
    delete pendingPairs[msg.code];
    return send(ws, { type: 'pair_error', message: 'Invalid or expired code' });
  }
  if (pending.deviceId === msg.deviceId) {
    return send(ws, { type: 'pair_error', message: 'Cannot pair with yourself' });
  }
  delete pendingPairs[msg.code];

  const a = pending.deviceId; // requester
  const b = msg.deviceId;     // joiner

  // Break any previous pairing for both devices (a device belongs to one pair).
  for (const id of [a, b]) {
    const old = state.devices[id] && state.devices[id].pairId;
    if (old) delete state.pairs[old];
  }

  const pairId = pending.pairId;
  state.pairs[pairId] = { a, b, createdAt: Date.now() };
  state.devices[a] = state.devices[a] || { name: pending.name, pairId: null };
  state.devices[b] = state.devices[b] || { name: msg.deviceName || 'Device', pairId: null };
  state.devices[a].pairId = pairId;
  state.devices[b].pairId = pairId;
  persist();

  send(sockets[a], {
    type: 'pair_joined',
    pairId,
    peerDeviceId: b,
    peerDeviceName: state.devices[b].name,
  });
  send(ws, {
    type: 'pair_joined',
    pairId,
    peerDeviceId: a,
    peerDeviceName: state.devices[a].name,
  });
}

function handleSyncPush(ws, msg) {
  if (!msg.snapshot) return;
  state.mailboxes[msg.deviceId] = msg.snapshot;
  persist();

  const peer = peerOf(msg.deviceId);
  if (peer && sockets[peer]) {
    send(sockets[peer], { type: 'sync', fromDeviceId: msg.deviceId, snapshot: msg.snapshot });
  }
}

function handleSyncPull(ws, msg) {
  const peer = peerOf(msg.deviceId);
  if (!peer) return send(ws, { type: 'pair_error', message: 'Not paired' });
  const snapshot = state.mailboxes[peer];
  if (snapshot) {
    send(ws, { type: 'sync', fromDeviceId: peer, snapshot });
  } else {
    send(ws, { type: 'no_snapshot' });
  }
}

function handleUnpair(ws, msg) {
  unpair(msg.deviceId);
}

// Clear the pair for [deviceId] and tell the still-connected peer it is gone.
function unpair(deviceId) {
  const device = state.devices[deviceId];
  if (!device || !device.pairId) return;
  const pairId = device.pairId;
  const pair = state.pairs[pairId];
  if (!pair) return;
  const peer = pair.a === deviceId ? pair.b : pair.a;
  delete state.pairs[pairId];
  if (state.devices[deviceId]) state.devices[deviceId].pairId = null;
  if (state.devices[peer]) state.devices[peer].pairId = null;
  delete state.mailboxes[deviceId];
  delete state.mailboxes[peer];
  send(sockets[peer], { type: 'pair_error', message: 'Device was unpaired' });
  persist();
}

// A device's socket closed (e.g. the app was closed). Give it a short grace
// period to reconnect before unpairing, so a transient network blip doesn't
// tear down a healthy pairing. Only the device's live socket matters: if a
// newer socket already replaced this one, do nothing.
function handleDisconnect(ws) {
  const deviceId = ws._deviceId;
  if (!deviceId) return;
  if (sockets[deviceId] !== ws) return;
  delete sockets[deviceId];
  if (pendingDisconnects[deviceId]) clearTimeout(pendingDisconnects[deviceId]);
  pendingDisconnects[deviceId] = setTimeout(() => {
    delete pendingDisconnects[deviceId];
    if (sockets[deviceId]) return; // reconnected during the grace period
    unpair(deviceId);
  }, UNPAIR_GRACE_MS);
}

// ---------------------------------------------------------------------------
// Bootstrap
// ---------------------------------------------------------------------------

// A plain HTTP server answers health checks (Render pings `/health` to decide
// whether the deploy is live); the WebSocket server is attached to it so the
// same port serves both `GET /health` and `wss://` upgrades.
const server = http.createServer((req, res) => {
  if (req.method === 'GET' && (req.url === '/' || req.url === '/health')) {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('ok');
    return;
  }
  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

const wss = new WebSocketServer({ server });
wss.on('connection', (ws) => {
  ws.on('message', (raw) => onMessage(ws, raw.toString()));
  ws.on('close', () => handleDisconnect(ws));
});

server.listen(PORT, () => {
  console.log(`VIVI Music sync relay listening on :${PORT} (http + ws)`);
});
