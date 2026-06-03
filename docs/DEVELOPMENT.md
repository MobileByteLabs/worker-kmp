# Docs authoring guide

How to write, structure, and ship the documentation that lives under `docs/`.
This guide is itself an example of the conventions it documents.

## Two surfaces, one source

Files in `docs/` feed **two** published surfaces:

| Surface | Pipeline | Lands at |
|---------|----------|----------|
| **mkdocs site** (canonical) | `.github/workflows/docs-publish.yml` → `mbl-actionhub/docs-publish-mkdocs.yml` | `https://<org>.github.io/<repo>/` |
| **GitHub Wiki** | `.github/workflows/sync-docs-to-wiki.yml` → `mbl-actionhub-docshub` composite | `https://github.com/<org>/<repo>/wiki/<basename>` |

Both pipelines trigger on push to `development` whenever `docs/**` or `mkdocs.yml`
changes. There is no "write once, sync later" step — the merge is the deploy.

## Files with special meaning

| File | Used by | Purpose |
|------|---------|---------|
| `index.md` | mkdocs | Root URL of the site (`/`). Required — without it the root returns 404. |
| `Home.md` | wiki | Wiki's home page. GitHub Wiki indexes by basename; this name is hard-coded. Excluded from the mkdocs build (`exclude_docs:`) to avoid a duplicate `/Home/` page. |
| `_Sidebar.md` | wiki | Wiki sidebar nav. Excluded from the mkdocs build. |
| `requirements.txt` | docs-publish workflow | Pinned mkdocs deps — change a version here, not in the workflow. |
| `stylesheets/mbs-brand.css` | mkdocs | Brand polish. Override colors for your library here. |

`index.md` and `Home.md` are duplicates by design: mkdocs needs `index.md`,
wiki needs `Home.md`, and they should always show the same landing content.
When you edit one, edit the other.

## Adding a new page (3-step)

1. **Write the markdown** under the appropriate section directory
   (`docs/<section>/<page-slug>.md`). Use kebab-case for filenames.
2. **Register it in `mkdocs.yml`** under `nav:`. The section heading you put it
   under is what appears in the site's tab bar.
3. **Push to `development`** — the docs-publish workflow rebuilds and
   redeploys the site within ~30 seconds.

Wiki picks up the new file automatically (no nav registration needed —
wiki uses `_Sidebar.md` for nav).

## Section conventions

The starter `mkdocs.yml` declares these sections. Use them or replace them,
but keep the structure shallow (≤2 levels of nesting):

| Section | What goes here |
|---------|----------------|
| **Home** | Library overview, badges, "why this exists" — same content as `index.md`/`Home.md`. |
| **Getting started** | Install, first usage, key concepts, migration guides. Each page should be runnable end-to-end. |
| **Features** | One page per top-level capability. Title each page as a noun phrase ("Foreground tasks"), not a verb ("How to run foreground tasks"). |
| **Platform support** | Per-platform pages + matrix tables. Capture quirks, capabilities, and not-yet-supported APIs. |
| **Operations** | Performance, security, audits, threat models — anything ops-oriented for production deployers. |
| **Release** | Release process, postmortem template, deprecation policy. |

## Style guide

### Code blocks

Always declare the language. mkdocs-material renders Kotlin, Swift, Bash, YAML,
JSON, and TOML out of the box. Never leave a code block unlabeled.

````markdown
```kotlin
val worker = MyWorker()
```
````

Inline `code` for symbols + flag names. Use `**bold**` for first-mention
emphasis of a concept (sparingly).

### Admonitions

Use admonitions (`!!! note`, `!!! warning`, `!!! tip`) for callouts that break
the flow but are important. Don't use them for body text.

```markdown
!!! warning "iOS background time is finite"
    BGTaskScheduler grants ~30s of execution. Long work needs
    `URLSession.uploadTask` instead.
```

### Tables

Use tables for any comparison with ≥3 dimensions (platforms × features,
versions × behaviors, options × tradeoffs). Inline lists are fine for
≤2 dimensions.

### Links

- **Internal** (within the docs/ tree): use relative paths
  (`[Quick start](getting-started/quick-start.md)`). mkdocs validates these on
  build; broken links surface as INFO-level warnings.
- **External**: full URLs.
- **Cross-repo source** (`workspaces/mbs/...`): the warning is INFO-level and
  expected — the link works on GitHub source view but not on the docs site.
- **API reference**: link to the published Dokka HTML (bundled inside each
  module's `-javadoc.jar`) or to Maven Central.

### Per-platform caveats

When a feature behaves differently across platforms, structure as:

```markdown
## Per-platform notes

- **Android:** ... 
- **iOS:** ...
- **JVM Desktop:** ...
- **JS / wasmJs:** ...
```

This pattern is grep-friendly and reads consistently across the site.

## Test locally before pushing

```bash
pip install -r docs/requirements.txt
mkdocs serve
# open http://127.0.0.1:8000
```

`mkdocs build --strict` is what CI runs. If strict fails locally, it'll fail
in CI. Most common cause: a nav entry references a file that doesn't exist,
or a relative link points outside `docs/`.

## What NOT to do

- **Don't hand-author files under `site/`** — that directory is the mkdocs
  build output, regenerated on every deploy.
- **Don't commit `.cache/` or generated assets** — `.gitignore` already
  excludes them; if you see one in `git status`, fix the ignore instead of
  staging it.
- **Don't edit the workflow** to change build behavior. The build logic
  lives in `mbl-actionhub/docs-publish-mkdocs.yml`. Bump the `@vX.Y.Z` pin
  in `.github/workflows/docs-publish.yml` to upgrade.
- **Don't add a `docs/CNAME` file** to set a custom domain — configure it
  via the repo's Pages settings (UI or `gh api ... -f cname=...`).
- **Don't author Liquid `{% ... %}` syntax** in markdown. The mkdocs site
  doesn't process Liquid, but if Pages is ever misconfigured back to legacy
  Jekyll it will fail to render those files.

## Adding a new section to the nav

Edit `mkdocs.yml` → `nav:` block. Each entry is `Section Title:` followed by
either a single `page.md` or a nested list of pages. Keep section titles
≤3 words; the navigation tab bar truncates long titles.

```yaml
nav:
  - Home: index.md
  - Getting started:
      - Installation: getting-started/installation.md
      - Quick start: getting-started/quick-start.md
  - New section:
      - First page: new-section/first-page.md
```

## Updating the brand

`docs/stylesheets/mbs-brand.css` declares primary + accent colors via
`--md-primary-fg-color` and `--md-accent-fg-color`. Change these to match
your library's brand. Tweak typography spacing in the same file — keep
changes small; mkdocs-material's defaults are well-considered.

For the palette toggle (light/dark mode), edit `mkdocs.yml` → `theme.palette`.

## When the site breaks

| Symptom | Cause | Fix |
|---------|-------|-----|
| `/` returns 404 | `docs/index.md` missing | Add it (duplicate of `Home.md`). |
| Build fails: "unrecognized relative link" | A `*.md` link points outside `docs/` | Either fix the link or accept it (it'll surface as INFO, not error). |
| Build fails: "nav references file that doesn't exist" | `mkdocs.yml` nav has a stale entry | Remove the entry or create the file. |
| Pages deploy succeeds but site shows old content | CDN cache | Hard refresh (Cmd-Shift-R). Usually clears within 1-2 minutes. |
| `configure-pages` errors with "Get Pages site failed" | Repo's Pages source not set to "GitHub Actions" | Bump caller workflow pin to `v1.9.1+` (auto-enables) OR flip Settings → Pages → Source manually. |

## Wiki-specific notes

- Wiki sync requires the wiki to be initialized (Settings → Features → Wikis
  → enabled, then create any first page via the Wiki UI). Without this, the
  composite errors with "Repository not found".
- `_Sidebar.md` in `docs/` becomes the wiki's left nav. If you want wiki
  auto-sidebar generation, set `sidebar-mode: auto` in the workflow inputs
  (the default).
- Wiki indexes by basename — two files named the same in different subdirs
  collide. The docshub composite resolves by subdir-prefixing the slug, but
  it's cleaner to avoid the collision in the first place.

## Pipeline architecture (one-paragraph version)

The build + deploy logic lives **once** in
[`mbl-actionhub/docs-publish-mkdocs.yml`](https://github.com/MobileByteLabs/mbl-actionhub/blob/main/.github/workflows/docs-publish-mkdocs.yml).
This repo's `.github/workflows/docs-publish.yml` is a 5-line caller that
pins to a specific version (`@v1.9.1` at time of writing). Upgrades happen
in one place; consumers bump the pin to opt in.
