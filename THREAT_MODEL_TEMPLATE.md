# Threat Model Template

> Consumer-extension template — copy to your fork's THREAT_MODEL.md and extend with
> deployment-specific threats. worker-kmp's library-level threat model is at SECURITY.md.

---

## Your deployment-specific context

- App name: <fill in>
- User data sensitivity: <e.g. medical / financial / consumer-grade>
- Compliance requirements: <e.g. GDPR, HIPAA, none>
- Threat actor model: <e.g. opportunistic / state-level / insider>

---

## Consumer-extension STRIDE rows

Add rows for threats SPECIFIC to your app on top of worker-kmp's library threat model:

| ID | Surface | Threat class | Status | Mitigation |
|----|---------|--------------|:------:|------------|
| C1 | <your surface> | <STRIDE class> | <MITIGATED / ACCEPTED> | <code reference or rationale> |

## Server-side threats (Web Push)

If you ship a Web Push server (per Phase 9), extend with rows for:

- Server-side subscription storage (encryption at rest, access controls)
- /push/subscribe rate-limiting + bot defense
- Cross-user subscription tampering
- Server compromise → push storm

## Custom worker threats

If your workers handle sensitive data or external network:

- Input validation on WorkData parameters
- TLS pinning for outbound network from doWork()
- Worker output sanitization before persisting

---

## Auditing your extension

1. Review SECURITY_ASSUMPTIONS.md and confirm each item is true in your deployment.
2. Add deployment-specific assumptions to your own ASSUMPTIONS.md.
3. Re-audit quarterly or after material architecture changes.
