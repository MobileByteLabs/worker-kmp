// Minimal reference worker-kmp Web Push server.
//
// Added in worker-kmp v3.0.0-alpha06.X (Phase 9 alpha06.X).
//
// Endpoints:
//   POST /push/subscribe   — accepts {endpoint, p256dh, auth} JSON; stores in SQLite
//   GET  /push/subscribers — admin: count current subscriptions
//
// Cron: every hour, send a worker-kmp trigger push to all subscriptions.
//
// VAPID keys: read from env VAPID_PUBLIC_KEY + VAPID_PRIVATE_KEY.
// Generate via: npx web-push generate-vapid-keys
//
// Per SECURITY.md T7-T15 (worker-kmp v3.0.0 epic Phase 10):
//   * MUST NOT log raw endpoint URLs (hashed below).
//   * SHOULD encrypt subscription rows at rest (NOT done in this minimal sample).
//   * SHOULD rate-limit /push/subscribe per IP (NOT done — add for production).

const express = require('express');
const Database = require('better-sqlite3');
const cron = require('node-cron');
const webPush = require('web-push');
const crypto = require('crypto');

const VAPID_PUBLIC = process.env.VAPID_PUBLIC_KEY;
const VAPID_PRIVATE = process.env.VAPID_PRIVATE_KEY;
const VAPID_SUBJECT = process.env.VAPID_SUBJECT || 'mailto:admin@example.com';

if (!VAPID_PUBLIC || !VAPID_PRIVATE) {
    console.error('Set VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY env vars.');
    console.error('Generate via: npx web-push generate-vapid-keys');
    process.exit(1);
}

webPush.setVapidDetails(VAPID_SUBJECT, VAPID_PUBLIC, VAPID_PRIVATE);

const db = new Database('subscriptions.db');
db.exec(`CREATE TABLE IF NOT EXISTS subs (
    endpoint TEXT PRIMARY KEY,
    p256dh   TEXT NOT NULL,
    auth     TEXT NOT NULL,
    created  INTEGER NOT NULL
)`);

const app = express();
app.use(express.json());

function hashEndpoint(endpoint) {
    return 'sha256:' + crypto.createHash('sha256').update(endpoint).digest('hex').slice(0, 8);
}

app.post('/push/subscribe', function (req, res) {
    const { endpoint, p256dh, auth } = req.body || {};
    if (!endpoint || !p256dh || !auth) {
        return res.status(400).json({ error: 'missing endpoint / p256dh / auth' });
    }
    db.prepare('INSERT OR REPLACE INTO subs (endpoint, p256dh, auth, created) VALUES (?, ?, ?, ?)')
        .run(endpoint, p256dh, auth, Date.now());
    console.log('Subscribed:', hashEndpoint(endpoint));
    res.json({ ok: true });
});

app.get('/push/subscribers', function (req, res) {
    const row = db.prepare('SELECT COUNT(*) AS n FROM subs').get();
    res.json({ count: row.n });
});

cron.schedule('0 * * * *', async function () {
    const subs = db.prepare('SELECT * FROM subs').all();
    const payload = JSON.stringify({ type: 'WORKER_KMP_TRIGGER', scope: 'cron' });
    for (const sub of subs) {
        try {
            await webPush.sendNotification(
                { endpoint: sub.endpoint, keys: { p256dh: sub.p256dh, auth: sub.auth } },
                payload
            );
            console.log('Pushed to', hashEndpoint(sub.endpoint));
        } catch (e) {
            console.error('Push failed for', hashEndpoint(sub.endpoint), ':', e.statusCode || e.message);
            if (e.statusCode === 410 || e.statusCode === 404) {
                // Subscription expired — drop it
                db.prepare('DELETE FROM subs WHERE endpoint = ?').run(sub.endpoint);
            }
        }
    }
});

const PORT = process.env.PORT || 8787;
app.listen(PORT, function () {
    console.log('worker-kmp push server listening on :' + PORT);
});
