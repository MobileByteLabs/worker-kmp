---
title: "Security assumptions"
description: "Trust boundaries, attacker model, and assumptions worker-kmp's design relies on — read before threat-modeling your usage."
---

# Security Assumptions

> Explicit trust assumptions worker-kmp v3 makes. If any of these is invalid in your
> deployment, please file a security advisory (see [security.md](./security.md) § Reporting a vulnerability).

---

## Consumer obligations

1. **Consumer signs their installer + the daemon JAR** with a code-signing certificate (Windows SmartScreen, macOS Gatekeeper, Linux distro-appropriate). worker-kmp produces unsigned artifacts.
2. **Consumer secures their Web Push server** per [features/web-push-server.md](../features/web-push-server.md) (RFC 8030 + VAPID + encrypted subscriptions at rest + rate-limit subscribe endpoint + log redaction).
3. **Consumer protects their VAPID private key** — stored in framework vault per RULE-SECRETS-VAULT-001; never committed/emailed/pasted; rotated annually; rotated immediately on suspected leak.
4. **Consumer's app process is trusted**. Workers, observers, persistence files all run at consumer-app user privilege. If the consumer-app process is compromised, worker-kmp's protections do not apply.

## Platform-level trust

5. **OS scheduler integrity** — Windows Task Scheduler, macOS launchd, Linux systemd/cron are trusted. If the user is unprivileged, scheduler tampering is out of scope.
6. **Browser trust** — Service Worker code served from the consumer's HTTP origin is trusted to the extent the origin is trusted. SRI hashes (T13) defend against CDN compromise.
7. **OS file ACLs** — `~/.worker-kmp/` and `~/Library/LaunchAgents/` are protected by OS file permissions (mode 600/700). Cross-process attacks at same user privilege are outside our threat model.

## Daemon privilege boundary

8. **Daemon runs as consumer-app user (not system service)** — alternative requires admin/root at install (out of scope per Phase 8). Risks are bounded to user's own data. If consumer needs system-wide background, they ship a separate LaunchDaemon / Windows Service themselves.

## Worker privilege boundary

9. **Registered workers run in consumer-app process at consumer-app privilege** — Koin factory injection cannot grant more privileges than the host process has.

## What is OUT of scope

- Formal third-party security audit (planned v3.x or pre-v4 depending on adoption)
- Side-channel analysis (timing, power)
- Anti-debugging / obfuscation (consumer's responsibility via ProGuard/R8)
- Hardware-backed key storage (consumer's HSM integration)
- Pen-testing the consumer's server infrastructure
