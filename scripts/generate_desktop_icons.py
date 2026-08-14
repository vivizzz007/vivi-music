#!/usr/bin/env python3
"""Generate OS-specific app icons for the VIVI Music desktop edition.

Reads the source logo (a square-ish PNG) and produces:
  - logo_vmde.ico  (Windows)
  - logo_vmde.icns (macOS)
  - logo_vmde.png  (Linux — used as-is, kept here as the source too)

Requires Pillow:  pip install pillow
"""

import io
import os
import struct

from PIL import Image

SRC = os.path.join(os.path.dirname(__file__), "..", "desktop", "icons", "logo_vmde.png")
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "desktop", "icons")


def square(img: Image.Image) -> Image.Image:
    """Center the image on a transparent square canvas (avoids distortion)."""
    w, h = img.size
    side = max(w, h)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(img, ((side - w) // 2, (side - h) // 2), img)
    return canvas


def build_ico(img: Image.Image, path: str) -> None:
    sizes = [(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]
    img.save(path, format="ICO", sizes=sizes)


def build_icns(img: Image.Image, path: str) -> None:
    # Modern ICNS: PNG data stored in standard slot types. macOS 10.15+ reads
    # PNG-based icns fine, so we do not need legacy JPEG2000/PNG fallbacks.
    entries = [
        ("ic07", 128),
        ("ic08", 256),
        ("ic09", 512),
        ("ic10", 1024),
        ("ic11", 32),
        ("ic12", 64),
        ("ic13", 256),
        ("ic14", 512),
    ]
    chunks = b""
    for slot, size in entries:
        buf = io.BytesIO()
        img.resize((size, size), Image.Resampling.LANCZOS).save(buf, format="PNG")
        data = buf.getvalue()
        chunks += slot.encode("ascii") + struct.pack(">I", len(data) + 8) + data

    with open(path, "wb") as f:
        f.write(b"icns" + struct.pack(">I", 8 + len(chunks)) + chunks)


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    img = square(Image.open(SRC).convert("RGBA"))

    build_ico(img, os.path.join(OUT_DIR, "logo_vmde.ico"))
    build_icns(img, os.path.join(OUT_DIR, "logo_vmde.icns"))
    print(f"Wrote {OUT_DIR}/logo_vmde.ico and logo_vmde.icns")


if __name__ == "__main__":
    main()
