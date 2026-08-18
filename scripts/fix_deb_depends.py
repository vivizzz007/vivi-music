#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Patch a .deb so its `Depends` field works on Debian as well as Ubuntu.

jpackage builds the .deb on ubuntu-latest and auto-detects dependencies with
`dpkg -S`, which returns Ubuntu 24.04's *t64-renamed* package names
(e.g. `libasound2t64`, `libglib2.0-0t64`, `libgtk-3-0t64`). Those names do not
exist on Debian Bookworm (which still ships `libasound2`, `libglib2.0-0`, …),
so `apt`/`dpkg` reports "dependency not found".

We rewrite every t64-suffixed package as `<base> | <base>t64` so the package
manager picks whichever name the target distro actually provides:
- Debian Bookworm          -> `<base>`        (no t64 suffix)
- Debian Trixie / Ubuntu 24.04+ -> `<base>t64`
"""

import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

TOKEN_RE = re.compile(r"[A-Za-z0-9][A-Za-z0-9.+\-]*t64")


def fix_depends_line(line: str) -> str:
    prefix, sep, value = line.partition(":")
    if not sep:
        return line
    fixed = []
    for item in value.split(","):
        item = item.strip()
        if TOKEN_RE.fullmatch(item):
            base = item[:-3]
            fixed.append(f"{base} | {item}")
        else:
            fixed.append(item)
    return prefix + ": " + ", ".join(fixed)


def main() -> None:
    if len(sys.argv) != 2:
        print("usage: fix_deb_depends.py <file.deb>", file=sys.stderr)
        sys.exit(2)

    deb = Path(sys.argv[1]).resolve()
    if not deb.is_file():
        print(f"not found: {deb}", file=sys.stderr)
        sys.exit(1)

    workdir = Path(tempfile.mkdtemp(prefix="vivideb-"))
    try:
        subprocess.run(["dpkg-deb", "-R", str(deb), str(workdir)], check=True)

        control = workdir / "DEBIAN" / "control"
        text = control.read_text(encoding="utf-8")
        new_lines = []
        changed = False
        for line in text.splitlines(keepends=True):
            if line.startswith("Depends:"):
                new_line = fix_depends_line(line.rstrip("\n")) + "\n"
                if new_line != line:
                    changed = True
                new_lines.append(new_line)
            else:
                new_lines.append(line)
        control.write_text("".join(new_lines), encoding="utf-8")

        if not changed:
            print("No t64 dependencies found; leaving package unchanged.")
        else:
            print("Rewrote Depends to include Debian-compatible alternatives.")
            print(control.read_text(encoding="utf-8"))

        os.remove(deb)
        subprocess.run(
            ["dpkg-deb", "--build", "--root-owner-group", str(workdir), str(deb)],
            check=True,
        )
        print(f"Repacked: {deb}")
    finally:
        shutil.rmtree(workdir, ignore_errors=True)


if __name__ == "__main__":
    main()
