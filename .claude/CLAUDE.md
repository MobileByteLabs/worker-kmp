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

## Plan-driven development (claude-product-cycle framework)

This repo is consumed by the [claude-product-cycle](https://github.com/MobileByteLabs/claude-product-cycle) framework at `workspaces/mbs/worker-kmp/`. All v3+ feature work is plan-driven through the framework.

- **Plans live at:** `plan-layer/project-plans/mbs/worker-kmp/active/`
- **Start work:** invoke the framework's `/idea-plan` → `/gap-planning-project` → `/gap-implement-project` workflow from the framework root.
- **Methodology:** [superpowers](https://github.com/obra/superpowers) — design-first, RED→GREEN→REFACTOR, two-stage review (spec compliance + code quality), evidence before "done."
- **PR discipline:** the PR template at `.github/PULL_REQUEST_TEMPLATE.md` enforces the methodology. Every feature PR must link to a sub-plan, declare a RED test commit, and pass both Spec + Quality reviews.

See `workspaces/mbs/worker-kmp/idea-layer/idea-plan.yaml` for the canonical feature roster + release-plan.

### How to start a new sub-plan

1. From the framework root, ensure the session is bound: `/context-start mbs-worker-kmp`
2. Pick the next sub-plan per the master `sub_plans:` dependency order in [`PLAN.md`](../../../../../../plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/PLAN.md).
3. Lock the sub-plan: edit its frontmatter `status: draft → locked`.
4. Open an isolated worktree off this repo:
   ```bash
   bash scripts/start-subplan.sh <full-sub-plan-slug>
   # e.g. bash scripts/start-subplan.sh worker-kmp-v3-foreground-storeflow-01-foreground-tasks
   ```
   This creates a worktree at `../worker-kmp-{slug}/` on branch `feat/{slug}`.
5. `cd` into the worktree. Execute the sub-plan's tasks in order (each task = one RED commit + one GREEN commit + optional REFACTOR).
6. When all tasks complete: push the branch, open a PR using `.github/PULL_REQUEST_TEMPLATE.md`. Request Spec review THEN Quality review.
7. After both reviews LGTM: merge, then mark the sub-plan frontmatter `status: locked → complete`.

(The `scripts/start-subplan.sh` convenience script lands in Phase 6 Tier D.)
