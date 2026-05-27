// worker-kmp Service Worker — v3.0.0-alpha06.X (Phase 9 alpha06.X)
//
// Listens for `push` + `periodicsync` events, reads pending work from IndexedDB
// (database `worker-kmp`, object store `work`), marks ENQUEUED entries as RUNNING,
// and broadcasts the result to open tabs via BroadcastChannel('worker-kmp') so
// the in-tab WebWorkManager can re-load state from IndexedDB.
//
// Copy to your web app's HTTP origin at /worker-kmp-sw.js (or path configured via
// WebPushConfig.serviceWorkerScript).
//
// CSP: no eval, no document.write, no innerHTML (per SECURITY.md T13-T18).

self.addEventListener('push', function (event) {
    if (!event.data) return;
    var payload;
    try {
        payload = event.data.json();
    } catch (e) {
        return;
    }
    if (!payload || payload.type !== 'WORKER_KMP_TRIGGER') return;
    event.waitUntil(processPendingWork(payload.scope || ''));
});

self.addEventListener('periodicsync', function (event) {
    if (event.tag && event.tag.indexOf('worker-kmp:') === 0) {
        event.waitUntil(processPendingWork(event.tag));
    }
});

async function processPendingWork(scope) {
    try {
        var db = await openWorkerKmpDb();
        var tx = db.transaction('work', 'readwrite');
        var store = tx.objectStore('work');
        var all = await getAll(store);
        var pending = all.filter(function (w) {
            return w.state === 'ENQUEUED' &&
                (!scope || w.scope === scope || (w.tags && w.tags.indexOf(scope) >= 0));
        });
        for (var i = 0; i < pending.length; i++) {
            await markRunning(store, pending[i].id);
        }
        await new Promise(function (resolve, reject) {
            tx.oncomplete = resolve;
            tx.onerror = reject;
        });
        var channel = new BroadcastChannel('worker-kmp');
        channel.postMessage({
            type: 'PENDING_PROCESSED',
            scope: scope,
            count: pending.length
        });
        channel.close();
    } catch (e) {
        console.error('worker-kmp SW processPendingWork failed:', e);
    }
}

function openWorkerKmpDb() {
    return new Promise(function (resolve, reject) {
        var req = indexedDB.open('worker-kmp', 1);
        req.onsuccess = function () {
            resolve(req.result);
        };
        req.onerror = function () {
            reject(req.error);
        };
        req.onupgradeneeded = function () {
            var db = req.result;
            if (!db.objectStoreNames.contains('work')) {
                db.createObjectStore('work', { keyPath: 'id' });
            }
        };
    });
}

function getAll(store) {
    return new Promise(function (resolve) {
        var req = store.getAll();
        req.onsuccess = function () {
            resolve(req.result || []);
        };
        req.onerror = function () {
            resolve([]);
        };
    });
}

function markRunning(store, id) {
    return new Promise(function (resolve) {
        var req = store.get(id);
        req.onsuccess = function () {
            var w = req.result;
            if (w) {
                w.state = 'RUNNING';
                store.put(w);
            }
            resolve();
        };
        req.onerror = function () {
            resolve();
        };
    });
}

console.log('worker-kmp SW v3.0.0-alpha06.X loaded');
