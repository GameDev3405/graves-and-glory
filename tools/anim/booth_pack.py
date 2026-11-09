"""Review booth: pack one clip's frames into one WebP sheet.

Sections are stacked top to bottom: reference, previous, new; within a viewer near then game16. Each section is a
row-major grid; the returned metadata is what the page needs to cut a frame out again.
"""
from __future__ import annotations

import math
import os

from PIL import Image

from booth_config import BoothError
from booth_render import DISTANCES, VIEWERS

WEBP_MAX = 16383
FILL = (226, 226, 226)       # the renderer's sky colour


def pack_sheet(render, out_path, near_cell_h=240, cols=12, quality=82):
    plan = []
    y = 0
    width = 0
    for viewer in VIEWERS:
        if viewer not in render.frames:
            continue
        for dist in DISTANCES:
            frames = render.frames[viewer][dist]
            if not frames:
                continue
            h, w = frames[0].shape[:2]
            cell_w, cell_h = w, h
            if dist == "near" and h > near_cell_h:
                cell_h = near_cell_h
                cell_w = max(1, int(round(w * near_cell_h / h)))
            rows = int(math.ceil(len(frames) / cols))
            plan.append(({"viewer": viewer, "distance": dist, "y": y, "cell_w": cell_w, "cell_h": cell_h,
                          "cols": cols, "frames": len(frames)}, frames))
            y += rows * cell_h
            width = max(width, cols * cell_w)
    if not plan:
        raise BoothError("nothing to pack: the clip has no frames")
    if width > WEBP_MAX or y > WEBP_MAX:
        raise BoothError("sheet would be %dx%d px, above the %d px limit; the clip is too long for one sheet"
                         % (width, y, WEBP_MAX))
    sheet = Image.new("RGB", (width, y), FILL)
    for sec, frames in plan:
        size = (sec["cell_w"], sec["cell_h"])
        for i, f in enumerate(frames):
            im = Image.fromarray(f)
            if im.size != size:
                im = im.resize(size, Image.LANCZOS)
            sheet.paste(im, ((i % cols) * sec["cell_w"], sec["y"] + (i // cols) * sec["cell_h"]))
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    sheet.save(out_path, "WEBP", quality=quality, method=4)
    return [sec for sec, _ in plan]


def total_bytes(paths):
    return sum(os.path.getsize(p) for p in paths)
