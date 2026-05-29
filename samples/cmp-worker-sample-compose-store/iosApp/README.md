# iosApp — Xcode wrapper for cmp-worker-sample-compose-store

Generate the `.xcodeproj` on demand from `project.yml`:

```bash
brew install xcodegen   # one-time
cd samples/cmp-worker-sample-compose-store/iosApp
xcodegen generate       # creates iosApp.xcodeproj (gitignored)
```

See `../README.md` for the full per-target run guide.

After `worker-kmp-app-plugin` adoption (follow-up to the launcher epics), the
contents of this directory will be regenerated from the `@WorkerKmpApp`
annotation in commonMain — no hand-authored Xcode wrapper required.
