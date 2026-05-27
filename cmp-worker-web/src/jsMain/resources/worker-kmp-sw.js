// worker-kmp Service Worker — Background Sync support
// Copy this file to your web server root at /worker-kmp-sw.js
// or serve it dynamically using backgroundSyncServiceWorkerScript() in Kotlin.

self.addEventListener('sync', function (event) {
    if (event.tag && event.tag.startsWith('worker-kmp-')) {
        event.waitUntil(
            self.clients
                .matchAll({ type: 'window', includeUncontrolled: true })
                .then(function (clients) {
                    clients.forEach(function (client) {
                        client.postMessage({ type: 'WORKER_KMP_SYNC', tag: event.tag });
                    });
                })
        );
    }
});

self.addEventListener('message', function (event) {
    if (event.data && event.data.type === 'WORKER_KMP_PING') {
        event.source.postMessage({ type: 'WORKER_KMP_PONG' });
    }
});
