This template enforces the planning-rigor methodology used by the worker-kmp v3 epic: every change is tied to a sub-plan, every fix starts with a failing test (RED→GREEN→REFACTOR), and review happens in two stages — spec first (does the diff match the plan?), quality second (is the code shippable?). Hotfixes and docs-only PRs that don't fit this shape should use `.github/pull_request_template/red-green-refactor.md` instead.

## Linked plan / sub-plan

- [ ] `plan-layer/project-plans/mbs/worker-kmp/active/{epic}/{NN}-{name}.md`
- [ ] Task IDs covered: `T#-T#`

## RED test commit

- [ ] First commit on this branch is a `test:` commit with a failing test for the change
- [ ] Test name: `<class>.<method>`
- [ ] Commit SHA: `<paste>`

## Spec review (gate 1 — required before quality review)

- [ ] Diff matches the sub-plan body
- [ ] No scope creep beyond listed tasks
- [ ] @reviewer signs off → `LGTM-SPEC`

## Quality review (gate 2 — required before merge)

- [ ] `./gradlew check` GREEN locally
- [ ] `./gradlew spotlessCheck detekt` clean
- [ ] kdoc complete on new public API
- [ ] BCV snapshot updated if public API changed
- [ ] @reviewer signs off → `LGTM-QUALITY`

## Risk acknowledgments

- [ ] Read the sub-plan's `## Risk + mitigation` section; no unmitigated risks introduced
