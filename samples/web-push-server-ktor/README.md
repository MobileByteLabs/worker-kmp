# worker-kmp Web Push Server — Ktor reference

Minimal Web Push server consumers can fork. Added in worker-kmp v3.0.0-alpha06.X
(Phase 9 alpha06.X — Web Push real impls).

This is a STANDALONE Gradle project — it is NOT included in the root
`settings.gradle.kts`. Build and run from this directory.

## Setup

```bash
# Generate VAPID keys (uses Node's web-push CLI; openssl also works — see below)
npx web-push generate-vapid-keys

export VAPID_PUBLIC_KEY=BC4-...
export VAPID_PRIVATE_KEY=secret-private-key
export VAPID_SUBJECT=mailto:admin@example.com

./gradlew run
```

### OpenSSL alternative for VAPID

```bash
openssl ecparam -genkey -name prime256v1 -noout -out vapid.pem
openssl ec -in vapid.pem -pubout -out vapid.pub
# then BASE64URL-encode both keys (raw 32-byte secret + 65-byte uncompressed pub)
```

## Endpoints

- `POST /push/subscribe` — accepts `{endpoint, p256dh, auth}` JSON; stores in SQLite.
- `GET  /push/subscribers` — admin: count current subscriptions.
- `GET  /health` — liveness probe.

The hourly background loop sends a `WORKER_KMP_TRIGGER` push payload to every subscription.

## Hardening checklist (do these before production)

Per worker-kmp v3.0.0 epic Phase 10 SECURITY.md T7-T15:

- [ ] Rate-limit `/push/subscribe` per IP (e.g. Ktor's `RateLimit` plugin).
- [ ] Encrypt subscription rows at rest (TLS + DB-level encryption).
- [ ] Move VAPID private key into worker-kmp's `/secrets` vault — never commit it to git
      or paste it into chat (`/secrets push --generate vapid`).
- [ ] Add a `/push/unsubscribe` endpoint for user-driven opt-out.
- [ ] Rotate VAPID keys annually.

See the worker-kmp library's `docs/features/web-push-server.md` (lands alongside
Phase 10 docs) and `docs/operations/security.md` for the full threat model.
