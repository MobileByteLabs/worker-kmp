---
sync_version: "1.0.0"
template_repo: "MobileByteLabs/mbl-library-template-kmp"
template_branch: "development"
description: "Sync contract for KMP library template. /lib-template-sync reads this file to know what to sync and how."
last_updated: "2026-04-19"

syncable_files:
  - path: scripts/setup-hooks.sh
    strategy: replace
    executable: true
    description: "Install git pre-commit and pre-push hooks"
  - path: scripts/pre-commit.sh
    strategy: replace
    executable: true
    description: "Pre-commit hook — spotless auto-fix + detekt static analysis"
  - path: scripts/pre-push.sh
    strategy: replace
    executable: true
    description: "Pre-push hook — format check + detekt + jvmTest + apiCheck"
  - path: scripts/verify.sh
    strategy: replace
    executable: true
    description: "CI mirror (--fix/--check/--quick/--ci/--full) + --platforms + --local"
  - path: scripts/release.sh
    strategy: replace
    executable: true
    description: "8-step release pipeline — quality, platforms, maven-local, version, merge, tag, release"
  - path: .github/workflows/gradle.yml
    strategy: merge
    preserve_local:
      - "on.push.branches"
      - "on.pull_request.branches"
    description: "CI workflow — quality + multi-platform build + build-all"
  - path: .github/workflows/publish.yml
    strategy: merge
    preserve_local:
      - "on.release"
      - "env secrets"
    description: "Maven Central publish — multi-module auto-discovery + parallel"
  - path: config/detekt/detekt.yml
    strategy: replace
    description: "Detekt static analysis configuration"
  - path: config/detekt/baseline.xml
    strategy: skip
    description: "Project-specific detekt baseline — never overwrite"
  - path: .editorconfig
    strategy: replace
    description: "Editor configuration (indent, charset, line endings)"
  - path: .claude/SYNC.md
    strategy: replace
    description: "This file itself — always pull latest sync contract"

never_sync:
  - "cmp-*/"
  - "sample-app/"
  - "settings.gradle.kts"
  - "gradle/libs.versions.toml"
  - "gradle.properties"
  - "customizer.sh"
  - ".github/workflows/template-customize.yml"
  - "TEMPLATE.md"
  - "README.md"
  - "LICENSE"
  - "docs/"
  - ".claude/CLAUDE.md"

post_sync_verify:
  - command: "./scripts/verify.sh --check"
    description: "Verify formatting + static analysis pass"
    required: true
  - command: "./scripts/setup-hooks.sh"
    description: "Reinstall git hooks with updated scripts"
    required: true
  - command: "./scripts/verify.sh --quick"
    description: "Full quality verification (optional)"
    required: false
---

# Library Template Sync Contract

This file defines what /lib-template-sync should sync from this template to child library projects.

## How It Works

1. Child project runs /lib-template-sync
2. Framework fetches THIS file from template repo (MobileByteLabs/mbl-library-template-kmp@development)
3. Parses syncable_files list above
4. For each file: replace (overwrite), merge (preserve local sections), or skip
5. Make scripts executable (chmod +x)
6. Run post_sync_verify commands
7. Show summary of changes

## Adding New Syncable Files

When you add a new generic file to this template:
1. Add the file to this template repo
2. Add an entry to syncable_files above with correct strategy
3. Commit and push to development
4. Child projects run /lib-template-sync and automatically get the new file

No framework changes needed. SYNC.md IS the contract.

## Sync Strategies

| Strategy | When | Behavior |
|----------|------|----------|
| replace | Scripts, quality config | Template version replaces local entirely |
| merge | CI workflows | Template structure merged, local branches/secrets preserved |
| skip | Baselines, project config | Never touched — project-specific |

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-04-19 | Initial sync contract — 5 scripts, 2 workflows, 2 configs |
