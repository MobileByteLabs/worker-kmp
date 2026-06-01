#!/usr/bin/env python3
"""
Pre-process docs/ for GitHub Wiki sync.

Copy docs/ → .wiki-build/, rewriting internal `.md` links to absolute
`/wiki/{basename}` URLs that GitHub Wiki actually resolves.

Why this exists
---------------
GitHub Wiki indexes every `.md` file by its **basename** — the source
subdirectory context is dropped when the wiki action mirrors `docs/`.
That means relative links in the source docs break on the wiki:

  ](getting-started/installation.md)  →  404, redirects to raw.githubusercontent.com
  ](installation.md)                   →  200 but SILENT redirect to wiki Home

This script rewrites those targets to absolute `/wiki/installation` URLs
that resolve correctly on the wiki. The source `docs/` tree stays
untouched, so in-repo browsing of `docs/` continues to use relative
links naturally.

Scope rules
-----------
* Only rewrite if the resolved link target lives inside the source tree
  (i.e., is a doc that will actually be published to the wiki). Out-of-tree
  references like `../../build-logic/.../KoverConventionPlugin.kt` (.kt is
  ignored anyway) or `../../../../plan-layer/.../GOAL.md` (lives outside
  docs/) stay untouched.
* Skip code fences (```…```) and inline code (`…`) so we don't rewrite
  link-like substrings inside code samples.
* Skip absolute URLs (http://, https://) — never rewrite an existing
  external link.
* Skip pure-anchor links like `(#section)` — anchor-only links stay
  intact (they navigate within the current page).

Usage: python3 rewrite-wiki-links.py SRC DST
"""

from __future__ import annotations

import re
import shutil
import sys
from pathlib import Path

WIKI = "https://github.com/MobileByteLabs/worker-kmp/wiki"

LINK_RE = re.compile(
    r"""
    \]\(                              # opening of the link target
      (?!https?://)                   # bail on absolute URLs
      (?P<path>(?:[^)/\s]+/)*)        # optional relative path segments
      (?P<basename>[A-Za-z0-9._-]+)   # filename without extension
      \.md                            # require .md
      (?P<anchor>\#[^)\s]+)?          # optional #anchor
    \)
    """,
    re.VERBOSE,
)

# Spans we MUST NOT rewrite: code fences + inline code.
PROTECT_RE = re.compile(r"```[\s\S]*?```|`[^`\n]+`")


def rewrite(content: str, source_file: Path, tree_root: Path) -> str:
    """Return `content` with in-tree .md links rewritten to wiki URLs."""
    stash: list[str] = []

    def _stash(m: re.Match) -> str:
        stash.append(m.group(0))
        return f"\0PROTECT{len(stash) - 1}\0"

    stashed = PROTECT_RE.sub(_stash, content)

    tree_root_abs = tree_root.resolve(strict=False)

    def _rewrite(m: re.Match) -> str:
        path = m.group("path") or ""
        basename = m.group("basename")
        anchor = m.group("anchor") or ""

        target_abs = (source_file.parent / f"{path}{basename}.md").resolve(strict=False)
        try:
            target_abs.relative_to(tree_root_abs)
        except ValueError:
            # Outside the docs tree — leave the link as-is so repo browsing still works.
            return m.group(0)

        return f"]({WIKI}/{basename}{anchor})"

    rewritten = LINK_RE.sub(_rewrite, stashed)

    for i, original in enumerate(stash):
        rewritten = rewritten.replace(f"\0PROTECT{i}\0", original, 1)

    return rewritten


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: rewrite-wiki-links.py SRC DST", file=sys.stderr)
        return 1

    src = Path(sys.argv[1])
    dst = Path(sys.argv[2])

    if not src.is_dir():
        print(f"Source dir not found: {src}", file=sys.stderr)
        return 1

    if dst.exists():
        shutil.rmtree(dst)
    shutil.copytree(src, dst)

    changed = 0
    for md_file in dst.rglob("*.md"):
        before = md_file.read_text()
        after = rewrite(before, md_file, dst)
        if after != before:
            md_file.write_text(after)
            changed += 1
            print(f"rewrote: {md_file.relative_to(dst)}")

    print(f"\nDone. {changed} file(s) had links rewritten.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
