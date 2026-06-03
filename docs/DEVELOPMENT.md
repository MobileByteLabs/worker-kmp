---
title: Docs guide (project)
description: Per-project authoring extensions on top of the canonical DEVELOPMENT-TEMPLATE.md blueprint.
---

# Docs guide (project)

!!! abstract "What this file is"
    Per-project extensions on top of the canonical blueprint in
    [`DEVELOPMENT-TEMPLATE.md`](DEVELOPMENT-TEMPLATE.md) — **read that first.**

    Unlike the template file, this one is **owned by your repo**. It is NOT
    sync'd from `mbl-library-template-kmp`. Edit it freely to capture
    conventions specific to your library.

!!! tip "When in doubt, default to the template"
    If a convention applies to "any KMP library docs/," it belongs in
    `DEVELOPMENT-TEMPLATE.md` (and should be PR'd against
    `mbl-library-template-kmp`). Only library-specific overrides or
    extensions go here.

---

## What goes here vs there

| Belongs in `DEVELOPMENT-TEMPLATE.md` (generic) | Belongs in `DEVELOPMENT.md` (project) |
|---|---|
| How to add a page, section, image | Cookbook recipe enforcement (line caps, mandatory langs, frontmatter fields) |
| Style guide (code blocks, admonitions, tables, links) | Per-module page conventions (README-embed flavor vs placeholder) |
| Local preview + Material features | Library-specific validators (cross-checks specific to this repo) |
| Pipeline architecture explainer | Legacy directory migration plan (per-repo) |
| Generic agent recipes / invariants / validations | Library-specific recipes / invariants / scaling cues |
| `Home.md` ↔ `index.md` dual-surface rule | Custom mkdocs plugins or macros |

---

## Conventions for this library

!!! info "This is a starter stub"
    The sections below are placeholders for conventions that emerge as
    your library grows. Delete the ones that don't apply; expand the
    ones that do.

### Cookbook recipe format

??? note "If your library has a cookbook"
    Document the enforced shape:

    - Line cap (e.g. ≤ 80 lines)
    - Required frontmatter fields (e.g. `reviewed_by.date`, `version`)
    - Required code-block languages (e.g. ≥ 1 ` ```kotlin ` block)
    - Title pattern (e.g. "How do I X?")
    - Related-links structure

    Reference the recipe template (typically `docs/_partials/cookbook-recipe-template.md`).

### Per-module page conventions

??? note "If your library has multiple modules with one page each"
    Document:

    - Where the per-module pages live (`docs/modules/cmp-*.md`)
    - The README-embed pattern (via `mkdocs-include-markdown-plugin`)
    - The placeholder fallback for modules without a README yet
    - The migration rule (when a module gains a README, convert its
      placeholder in the same PR)

### Library-specific recipes

??? note "Agent-actionable recipes not covered by the generic guide"
    Examples:

    - "Add a new module": (1) create the Gradle subproject (2) write
      `cmp-<name>/README.md` (3) create `docs/modules/cmp-<name>.md`
      (4) add to mkdocs nav (5) apply the Dokka convention plugin (6)
      update CHANGELOG.

    - "Migrate a legacy `docs/<module>/` subdir": (1) lift content
      into `cmp-<module>/README.md` or cookbook recipes (2) delete the
      legacy subdir (3) remove its line from `mkdocs.yml` →
      `exclude_docs:`.

### Library-specific invariants

??? note "Cross-file edit obligations specific to this repo"
    Examples:

    - **New `cmp-<name>/` Gradle module** → 5 places must update in
      the same PR: docs/modules/ landing page + mkdocs nav + Gradle
      plugin + per-module CHANGELOG + root CHANGELOG.
    - **Cookbook recipe verified against new release** → bump
      `reviewed_by.date` + `version` in frontmatter.
    - **Legacy subdir migrated** → remove its line from `exclude_docs:`.

### Library-specific validation

??? note "CLI snippets that audit conventions specific to this library"
    Examples:

    ```bash
    # Every cmp-* Gradle module has a docs/modules/ landing page
    diff <(ls -1d cmp-*/) <(ls docs/modules/cmp-*.md | xargs -n1 basename)

    # Every cookbook recipe ≤ 80 lines
    for f in docs/cookbook/**/*.md; do
      lines=$(wc -l < "$f")
      [ "$lines" -gt 80 ] && echo "OVERLONG ($lines): $f"
    done
    ```

---

## Local preview

The local preview command is the same as the generic guide:

```bash
pip install -r docs/requirements.txt
mkdocs serve
```

If your library adds plugins beyond `mkdocs-material` (e.g.
`mkdocs-include-markdown-plugin`, `mkdocs-macros-plugin`), pin them in
`docs/requirements.txt` so contributors get the same versions CI runs.
