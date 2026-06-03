# Docs authoring guide (project-specific)

Project-specific extensions to the canonical authoring guide that lives in
[`DEVELOPMENT-TEMPLATE.md`](DEVELOPMENT-TEMPLATE.md). Read the template
file first — the conventions below assume that baseline.

This file is **owned by this repo**. It is NOT sync'd from
`mbl-library-template-kmp` (only `DEVELOPMENT-TEMPLATE.md` is). Edit it
freely to capture conventions specific to your library.

## When to put something here vs in DEVELOPMENT-TEMPLATE.md

| Belongs in `DEVELOPMENT-TEMPLATE.md` (generic) | Belongs in `DEVELOPMENT.md` (project) |
|------------------------------------------------|----------------------------------------|
| How to add a page, section, image | Cookbook recipe enforcement (line caps, mandatory langs, frontmatter fields) |
| Style guide (code blocks, admonitions, tables, links) | Per-module page conventions (README-embed flavor vs placeholder) |
| Local preview command | Custom validators (e.g. "every `cmp-*` module has a `docs/modules/` page") |
| Pipeline architecture explainer | Legacy directory migration plan |
| Generic agent recipes / invariants / validations | Library-specific recipes / invariants / scaling cues |
| `Home.md` ↔ `index.md` dual-surface rule | Custom mkdocs plugins or macros |

## Conventions for this library

(Add sections here as your library's docs/ tree grows. Examples below are
placeholders — delete or replace.)

### Cookbook recipe format

(If your library has a cookbook, document the enforced shape here — line
cap, required frontmatter fields, required code-block languages, related
links pattern.)

### Per-module page conventions

(If your library has multiple modules with one page each, document the
include-markdown pattern or placeholder fallback here.)

### Library-specific recipes

(Add agent-actionable recipes that aren't covered by the generic guide.)

### Library-specific invariants

(When X changes in your library, what else must change?)

### Library-specific validation

(CLI snippets that audit conventions specific to this library.)
