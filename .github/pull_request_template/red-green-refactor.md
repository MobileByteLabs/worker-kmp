Use this template for hotfixes, docs-only changes, or other small PRs that aren't tied to a tracked sub-plan. For plan-driven feature work, use the main `PULL_REQUEST_TEMPLATE.md` instead.

## What changed

<one-line summary>

## RED test commit (still required for non-trivial changes)

- [ ] First commit is `test:` with a failing test, OR
- [ ] N/A — explain why: <reason>

## Quality review

- [ ] `./gradlew check` GREEN
- [ ] `./gradlew spotlessCheck detekt` clean
- [ ] kdoc updated if public API touched
- [ ] @reviewer signs off → `LGTM`
