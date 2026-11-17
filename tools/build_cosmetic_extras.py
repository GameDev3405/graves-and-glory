#!/usr/bin/env python3
"""Builds the one texture of the modded cosmetic extras (cosmetics render plan 6): textures/entity/cosmetic/extras.png, 16x16.

python tools/build_cosmetic_extras.py          write it
python tools/build_cosmetic_extras.py --check  fail if the committed file differs from a fresh build

The sheet is a 4x4 grid of 4x4 swatches; CosmeticExtras draws each box with one swatch (its cell index, row-major). Each swatch is its colour
with a lit top row and a shaded bottom row, so a thin box reads as cloth or metal rather than a flat fill. The colours are placeholders
until Kort's art review (plan 9.5).
"""
from __future__ import annotations

import io
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/gravesandglory/textures/entity/cosmetic/extras.png"
# cell index -> (name, rgb); CosmeticExtras names the same indices
SWATCHES = [
    ("crimson", (176, 32, 42)),     # 0 Renown pennant streamer
    ("gold", (224, 176, 64)),       # 1 Founders streamer, Renown trim fringe
    ("iron", (58, 58, 66)),         # 2 Renown frame rails
    ("gilt", (242, 208, 96)),       # 3 Founders frame rails, standard finial
    ("brass", (192, 138, 58)),      # 4 Mastery III brass rails
    ("linen", (232, 232, 222)),     # 5 spare
    ("dark_gold", (160, 120, 32)),  # 6 standard crossbar
    ("oak", (110, 80, 48)),         # 7 spare
]


def shade(rgb, f):
    return tuple(max(0, min(255, int(c * f))) for c in rgb) + (255,)


def build() -> bytes:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i, (_, rgb) in enumerate(SWATCHES):
        x0, y0 = (i % 4) * 4, (i // 4) * 4
        for y in range(4):
            f = 1.18 if y == 0 else 0.78 if y == 3 else 1.0
            for x in range(4):
                g = f * (0.94 if (x + y) % 2 else 1.0)  # a faint weave
                img.putpixel((x0 + x, y0 + y), shade(rgb, g))
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=False)
    return buf.getvalue()


def main(argv) -> int:
    data = build()
    if "--check" in argv:
        if not OUT.exists():
            print("build_cosmetic_extras: extras.png is missing")
            return 1
        have = Image.open(OUT).convert("RGBA")
        want = Image.open(io.BytesIO(data)).convert("RGBA")
        if have.size != want.size or have.tobytes() != want.tobytes():
            print("build_cosmetic_extras: extras.png differs from a fresh build")
            return 1
        print("build_cosmetic_extras: extras.png ok")
        return 0
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(data)
    print("build_cosmetic_extras: wrote", OUT.relative_to(ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
