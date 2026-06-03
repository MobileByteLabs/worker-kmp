---
title: "Web push server"
description: "Server-side push delivery integration for worker-kmp jobs that need to trigger off browser-side events."
---

# Web Push Server Guide

> Documentation for consumer push servers that work with cmp-worker-web-push.
> Lands at v3.0.0-alpha06.X — this file is a scaffold.

## RFC 8030 protocol

Consumer's server sends Web Push messages to subscribers' registered endpoints
using VAPID JWT auth. Library ships the client side (subscription registration +
Service Worker push handler); consumer ships:

1. **VAPID key generation** (one-time): `./gradlew :cmp-worker-web-push:generateVapidKeys`
   (alpha06.X) writes public + private key to file. Private key goes in
   framework vault per RULE-SECRETS-VAULT-001 (`/secrets push --generate vapid`).
2. **Subscription registration endpoint**: HTTP POST receiving `{endpoint, p256dh, auth}` JSON.
   Store in encrypted-at-rest DB. Rate-limit /push/subscribe per IP (recommended: 10/hr).
3. **Cron job**: every N minutes, fan out pushes to all subscriptions.
4. **Push payload**: `{ "type": "WORKER_KMP_TRIGGER", "scope": "<tag>" }` — payload size <4KB.

## Server obligations (per [operations/security.md](../operations/security.md) T7-T15)

- MUST NOT log raw subscription endpoint URLs — hash them (e.g. `sha256:first8chars`)
- MUST encrypt subscription rows at rest
- MUST rate-limit /push/subscribe per IP
- MUST validate VAPID JWT `sub` claim on every push (defense-in-depth)

## Reference servers (alpha06.X)

- `samples/web-push-server-node/` — minimal Node.js using `web-push` npm package
- `samples/web-push-server-ktor/` — Kotlin Ktor using `nl.martijndwars:web-push`

## See also

- Phase 9 sub-plan
- [operations/security.md](../operations/security.md) T7-T18 (Web Push attack surfaces)
- [platform-support/true-background-matrix.md](../platform-support/true-background-matrix.md) (per-browser support)
