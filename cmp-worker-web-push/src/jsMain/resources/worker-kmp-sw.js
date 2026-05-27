// worker-kmp Service Worker template — Phase 9 of v3.0.0 epic
// Copy to your web app's HTTP origin at /worker-kmp-sw.js (or path configured via
// WebPushConfig.serviceWorkerScript). Listens for push events from the consumer's
// server + dispatches to pending work in IndexedDB.
//
// alpha06 scaffold: the push event handler is wired but the IndexedDB read +
// work dispatch are stub no-ops. Real implementation lands in alpha06.X follow-up.
//
// CSP: no eval, no document.write, no innerHTML (per SECURITY.md T13-T18).

self.addEventListener('push', event => {
    if (!event.data) return;
    let payload;
    try { payload = event.data.json(); } catch (_) { return; }
    if (payload.type !== 'WORKER_KMP_TRIGGER') return;
    event.waitUntil(processPendingWork(payload.scope || ''));
});

self.addEventListener('periodicsync', event => {
    if (event.tag.startsWith('worker-kmp:')) {
        event.waitUntil(processPendingWork(event.tag));
    }
});

async function processPendingWork(scope) {
    // alpha06.X delivers:
    //   1. Open IndexedDB (existing WebWorkPersistence from 2.0.0)
    //   2. Read all ENQUEUED work entries matching scope
    //   3. For each: run via WebWorkManager.runOne() (works in SW context)
    //   4. Write state back
    //   5. BroadcastChannel('worker-kmp').postMessage(...) to notify open tabs
    console.log('worker-kmp SW: push received for scope', scope, '— alpha06.X impl pending');
}
