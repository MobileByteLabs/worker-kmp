# KMP Library Template — Claude Instructions

> This template provides the foundation for creating Kotlin Multiplatform libraries with best-in-class tooling.

## Template Purpose

This is the **single source of truth** for KMP library infrastructure:
- Scripts: verify.sh, release.sh, pre-commit.sh, pre-push.sh, setup-hooks.sh
- CI/CD: gradle.yml (multi-platform testing), publish.yml (Maven Central publishing)
- Quality: detekt config, editorconfig, spotless

## Sync Contract

See `.claude/SYNC.md` for the sync contract. When this template is updated,
child library projects (e.g., KmpToolkit) run `/lib-template-sync` to pull
the latest generic infrastructure.

## Branch Strategy

- `development` — default branch, PRs target here, CI triggers
- `main` — release-only, merged from development during `release.sh`

## Module Structure

Libraries use the `cmp-{name}/` module pattern:
- `cmp-library/` — default template module (rename via customizer.sh)
- Each module has its own `build.gradle.kts` with `mavenPublishing` plugin
- Modules are auto-discovered by scripts and CI workflows

## Adding a New Module

1. Create `cmp-{name}/` directory with standard KMP source sets
2. Add `mavenPublishing` plugin to `build.gradle.kts`
3. Add module to `settings.gradle.kts`
4. Scripts and CI auto-discover it (no config changes needed)

## Key Commands (via claude-product-cycle framework)

| Command | Purpose |
|---------|---------|
| `/lib-template-sync` | Sync this template's infrastructure to child projects |
| `/lib-release` | Release library to Maven Central |
| `/release` | Interactive release matrix |
| `/lib-test` | Run tests with TDD verification |

## Quality Standards

- **Formatting**: ktlint via Spotless (120 char line, 4-space indent)
- **Static Analysis**: Detekt with project baseline
- **Testing**: JVM tests required, iOS/macOS/Linux tests when applicable
- **CI**: All checks must pass before merge
