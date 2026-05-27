# Security

> Threat model + mitigations for worker-kmp v3+. Per RULE-SECRETS-VAULT-001 + the
> worker-kmp v3.0.0 epic Phase 10 (security threat model).

Internal STRIDE audit: 2026-05-27.

---

## Reporting a vulnerability

Use GitHub Security Advisories (https://github.com/MobileByteLabs/worker-kmp/security/advisories/new)
for private disclosure. We respond within 7 days.

---

## STRIDE matrix

The following 28-row STRIDE table covers every attack surface introduced by worker-kmp
v3.0.0. Each row is either MITIGATED (with code reference) or ACCEPTED (with explicit
rationale referencing SECURITY_ASSUMPTIONS.md).

### Desktop daemon JAR (Phase 8)

| ID | Surface | Threat class | Status | Mitigation / Rationale |
|----|---------|--------------|:------:|------------------------|
| T1 | daemon JAR | Spoofing (unsigned JAR substitution) | TBD | Phase 8 T4-T5: SHA-256 integrity check at startup; mismatch → exit non-zero. Defends against post-install substitution. |
| T2 | daemon persistence files | Tampering (other-process file edits) | TBD | Phase 8 T7-T8: HMAC-SHA256 per .properties file using per-install ephemeral key (mode 600). Daemon refuses invalid HMAC. |
| T3 | daemon logs | Repudiation (log destruction) | TBD | Phase 10 T32-T34: log dir mode 700; rotating 1MB × 3; security events go to sticky security.log (never rotated/deleted). |
| T4 | daemon stack traces | Info disclosure (path leaks) | TBD | Phase 10 T32-T34: log entries strip input/output by default (`<redacted, N bytes>`); opt-in --verbose-log. |
| T5 | daemon lock file | DoS (lock removal → multi-daemon) | TBD | Phase 8 T9: FileChannel.lock + PID; stale-lock detection compares PID to running processes. |
| T6 | daemon process privilege | Elevation (runs as consumer-app user) | ACCEPTED | Documented in SECURITY_ASSUMPTIONS.md §"Daemon privilege boundary". Alternative is system service requiring sudo at install (out of scope per Phase 8). Risks bounded to user's own data. |

### Web Push subscription endpoint (Phase 9)

| ID | Surface | Threat class | Status | Mitigation / Rationale |
|----|---------|--------------|:------:|------------------------|
| T7 | endpoint URL | Spoofing (URL guessed) | ACCEPTED | RFC 8030 + VAPID key (only key holder can send pushes anyway). Defense-in-depth via T14's server obligations. |
| T8 | subscription | Tampering (replay over long horizons) | TBD | Phase 9 T13: subscriptionExpiryDays default 90; subscriptions auto-refresh. |
| T9 | server logs | Repudiation (log disclosure) | TBD | Phase 10 T11-T12: PushSubscriptionLogger — never logs raw endpoint URL; hashes endpoint to sha256:first8chars. |
| T10 | push endpoint = device identifier | **Info disclosure: PII** | TBD | Phase 10 T11-T15: endpoint hashed in logs; server-side: encrypt subscription rows at rest; rate-limit /push/subscribe. |
| T11 | subscription table | DoS (unbounded growth) | TBD | Phase 9 T13: 90-day auto-refresh + server rate-limit (T14). |
| T12 | server compromise | Elevation (push storm) | TBD | Document server hardening in WEB_PUSH_SERVER_GUIDE.md (T14); VAPID key rotation annually. |

### Service Worker push handler (Phase 9)

| ID | Surface | Threat class | Status | Mitigation / Rationale |
|----|---------|--------------|:------:|------------------------|
| T13 | SW script source | Spoofing (compromised CDN) | TBD | Phase 10 T18: WebPushConfig.serviceWorkerIntegrityHash + Integrity-Match server header; refuses if mismatch. |
| T14 | IndexedDB | Tampering by tab JS | TBD | Phase 10 T20: WebWorkPersistence schema-validates each entry; deserialize throws IntegrityException on tamper. |
| T15 | SW push event log | Repudiation (push logged with PII) | TBD | Phase 10 T11-T12: PushSubscriptionLogger applies in SW context too; redacts payloads. |
| T16 | push payload → DOM | Info disclosure (payload echoes) | TBD | Phase 10 T17-T19: ESLint config forbids `document.write` / `innerHTML` / `eval` in worker-kmp-sw.js. |
| T17 | SW kills self | DoS (bad payload) | TBD | Phase 10 T19: SW handler strictly validates `payload.type === 'WORKER_KMP_TRIGGER' && typeof payload.scope === 'string'`; anything else dropped silently. |
| T18 | SW imports | Elevation (compromised import) | TBD | Phase 10 T17: SW carries `// CSP: no eval, no document.write, no innerHTML` top-comment + ESLint enforces. |

### VAPID key lifecycle (Phase 9)

| ID | Surface | Threat class | Status | Mitigation / Rationale |
|----|---------|--------------|:------:|------------------------|
| T19 | VAPID private key | Tampering (committed to source) | TBD | Phase 10 T21: /secrets push --generate vapid integrates with framework vault per RULE-SECRETS-VAULT-001. Key NEVER echoed; stored in SOPS+age-encrypted vault. |
| T20 | VAPID private key on disk | Info disclosure | TBD | Phase 10 T22: WEB_PUSH_SERVER_GUIDE.md mandates: never commit / email / paste in chat. Annual rotation. |
| T21 | leaked VAPID key | Elevation (push to all subscribers) | TBD | Phase 10 T22: rotation procedure documented. /secrets rotate vapid_private_key immediate response. |

### Koin factory injection (Phase 0)

| ID | Surface | Threat class | Status | Mitigation / Rationale |
|----|---------|--------------|:------:|------------------------|
| T22 | WorkerRegistry | Spoofing (fake worker registered) | TBD | Phase 10 T30: workKoinModule rejects worker class names containing `..`, `/`, or null bytes. |
| T23 | WorkerRegistry post-start | Tampering | TBD | Phase 10 T29: registry becomes immutable after workKoinModule loads into Koin; subsequent register<T>() throws WorkerRegistryAlreadyLoadedException. |
| T24 | registered worker | Elevation (more privileges than expected) | ACCEPTED | Workers run in consumer-app process at consumer-app privilege. Documented in SECURITY_ASSUMPTIONS.md §"Worker privilege boundary". |

### Notifications permission (Phase 7)

| ID | Surface | Threat class | Status | Mitigation / Rationale |
|----|---------|--------------|:------:|------------------------|
| T25 | Notification.requestPermission | Spoofing (perma-grant abused) | TBD | Phase 10 T24-T25: requireNotificationPermission requires consumer-side explicit opt-in via WebPushConfig.notificationPermissionAutoRequest = true OR explicit subscriber.requestPushPermission() call. Library never auto-prompts. |
| T26 | notification body | Info disclosure (work content) | TBD | Phase 10 T26-T27: ForegroundInfo.Builder.setContentSafe(title, message) sanitizes strings; raw setter @Internal. FOREGROUND_TASKS.md warns about lock-screen visibility. |
| T27 | permission deny | DoS (user-permanent-deny) | ACCEPTED | UX is consumer responsibility. PRIVACY.md documents soft-ask pattern. |

### Foreground service notification (Phase 1)

| ID | Surface | Threat class | Status | Mitigation / Rationale |
|----|---------|--------------|:------:|------------------------|
| T28 | foreground notification | Info disclosure (lock-screen visibility) | TBD | Phase 10 T27: FOREGROUND_TASKS.md warns consumers about sensitive content; recommends Notification.VISIBILITY_PRIVATE. |

---

## Auditing dependencies

- OWASP dependency-check runs on every PR (.github/workflows/security-scan.yml)
- npm audit on samples/web-push-server-node/
- gitleaks on every push
- SBOM (CycloneDX) generated per release; attached to GitHub Release

---

## Audit history

| Date | Audit type | Outcome |
|------|-----------|---------|
| 2026-05-27 | Internal STRIDE (Phase 10 of worker-kmp v3.0.0 epic) | 28-row matrix initial draft; mitigations TBD as Phases 0/7/8/9 land |

---

## See also

- [`SECURITY_ASSUMPTIONS.md`](SECURITY_ASSUMPTIONS.md) — trust assumptions list
- [`THREAT_MODEL_TEMPLATE.md`](THREAT_MODEL_TEMPLATE.md) — consumer-extension template
- Framework rule: RULE-SECRETS-VAULT-001
- Phase 10 sub-plan: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/10-security-threat-model.md`
