# worker-kmp Web Push Server — Node.js reference

Minimal Web Push server consumers can fork. Added in worker-kmp v3.0.0-alpha06.X
(Phase 9 alpha06.X — Web Push real impls).

## Setup

```bash
npm install
npx web-push generate-vapid-keys
export VAPID_PUBLIC_KEY=BC4-...
export VAPID_PRIVATE_KEY=secret-private-key
export VAPID_SUBJECT=mailto:admin@example.com
npm start
```

## Endpoints

- `POST /push/subscribe` — accepts `{endpoint, p256dh, auth}` JSON; stores in SQLite.
- `GET  /push/subscribers` — admin: count current subscriptions.

The hourly cron sends a `WORKER_KMP_TRIGGER` push payload to every subscription.

## Hardening checklist (do these before production)

Per worker-kmp v3.0.0 epic Phase 10 SECURITY.md T7-T15:

- [ ] Rate-limit `/push/subscribe` per IP (e.g. `express-rate-limit`).
- [ ] Encrypt subscription rows at rest (TLS + DB-level encryption).
- [ ] Move VAPID private key into worker-kmp's `/secrets` vault — never commit it to git
      or paste it into chat (`/secrets push --generate vapid`).
- [ ] Add a `/push/unsubscribe` endpoint for user-driven opt-out.
- [ ] Rotate VAPID keys annually.

See the worker-kmp library's `docs/features/web-push-server.md` (lands alongside
Phase 10 docs) and `docs/operations/security.md` for the full threat model.
