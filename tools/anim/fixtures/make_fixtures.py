#!/usr/bin/env python3
"""Deterministically regenerate rig.png, rig.geo.json and walk.animation.json in this folder."""
import json
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))

FACE_COL = {  # per-face base colours so every face is recognisable
    "west": (230, 60, 60), "east": (60, 200, 80), "north": (60, 90, 230),
    "south": (240, 200, 50), "up": (240, 240, 240), "down": (90, 40, 130),
}

# name, parent, pivot, cube origin, size, uv, tint
BONES = [
    ("root", None, [0, 0, 0], None, None, None, 0),
    ("body", "root", [0, 12, 0], [-4, 12, -2], [8, 12, 4], [16, 16], 0),
    ("head", "body", [0, 24, 0], [-4, 24, -4], [8, 8, 8], [0, 0], 0),
    ("arm_left", "body", [5, 22, 0], [4, 12, -2], [4, 12, 4], [40, 16], 1),
    ("arm_right", "body", [-5, 22, 0], [-8, 12, -2], [4, 12, 4], [40, 32], -1),
    ("leg_left", "root", [2, 12, 0], [0, 6, -2], [4, 6, 4], [0, 16], 1),
    ("foot_left", "leg_left", [2, 6, 0], [0, 0, -3], [4, 6, 6], [0, 48], 1),
    ("leg_right", "root", [-2, 12, 0], [-4, 6, -2], [4, 6, 4], [16, 32], -1),
    ("foot_right", "leg_right", [-2, 6, 0], [-4, 0, -3], [4, 6, 6], [24, 48], -1),
]


def tint(c, k):
    return tuple(max(0, min(255, int(v * (1 + 0.12 * k)))) for v in c)


def paint_box(draw, uv, size, k):
    u, v = uv
    sx, sy, sz = size
    rects = {
        "west": (u + sz + sx, v + sz, sz, sy), "east": (u, v + sz, sz, sy),
        "north": (u + sz, v + sz, sx, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy),
        "up": (u + sz, v, sx, sz), "down": (u + sz + sx, v, sx, sz),
    }
    for f, (x, y, w, h) in rects.items():
        draw.rectangle([x, y, x + w - 1, y + h - 1], fill=tint(FACE_COL[f], k) + (255,))
        # asymmetric marker: dark pixel row along the top edge of the region
        draw.line([x, y, x + w - 1, y], fill=(20, 20, 20, 255))


def make_png():
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for name, _, _, _, size, uv, k in BONES:
        if size:
            paint_box(d, uv, size, k)
    im.save(os.path.join(HERE, "rig.png"))


def make_geo():
    bones = []
    for name, parent, pivot, origin, size, uv, _ in BONES:
        b = {"name": name, "pivot": pivot}
        if parent:
            b["parent"] = parent
        if origin:
            b["cubes"] = [{"origin": origin, "size": size, "uv": uv}]
        bones.append(b)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.fixture_rig", "texture_width": 64, "texture_height": 64},
        "bones": bones}]}
    with open(os.path.join(HERE, "rig.geo.json"), "w") as fh:
        json.dump(geo, fh, indent=1)


def feet(travel):
    """Foot position keyframes (pixels). Stance pulls the foot back relative to the body by `travel`
    px over 10 ticks; 16 * 0.05 blocks/tick * 10 ticks = 8 px keeps the foot planted at --speed 0.05."""
    t, h = travel, travel / 2.0
    left = {"0": [0, 0, 0], "0.5": [0, 0, t], "0.75": [0, 4, h], "1.0": [0, 0, 0]}
    right = {"0": [0, 0, t], "0.25": [0, 4, h], "0.5": [0, 0, 0], "1.0": [0, 0, t]}
    return left, right


def make_anim():
    pl, pr = feet(8)
    sl, sr = feet(4)
    anims = {
        "walk_planted": {"loop": True, "animation_length": 1.0, "bones": {
            "foot_left": {"position": pl}, "foot_right": {"position": pr}}},
        "walk_sliding": {"loop": True, "animation_length": 1.0, "bones": {
            "foot_left": {"position": sl}, "foot_right": {"position": sr}}},
        "arm_swing": {"loop": True, "animation_length": 1.0, "bones": {
            "arm_left": {"rotation": {"0": [-30, 0, 0], "0.5": {"vector": [30, 0, 0], "easing": "easeInOutSine"},
                                      "1.0": [-30, 0, 0]}},
            "arm_right": {"rotation": {"0": [30, 0, 0], "0.5": {"vector": [-30, 0, 0], "easing": "easeInOutSine"},
                                       "1.0": [30, 0, 0]}}}},
        "head_bob": {"loop": True, "animation_length": 1.0, "bones": {
            "head": {"position": {"0": [0, 0, 0], "0.5": [0, 1, 0], "1.0": [0, 0, 0]}}}},
        "head_look": {"loop": True, "animation_length": 2.0, "bones": {
            "head": {"rotation": {"0": [0, 0, 0], "1.0": [0, 40, 0], "2.0": [0, 0, 0]}}}},
        "spin_test": {"loop": "hold_on_last_frame", "animation_length": 1.0, "bones": {
            "body": {"rotation": {"0": [0, 0, 0], "1.0": {"vector": [0, 90, 0], "easing": "catmullrom"}}}}},
    }
    with open(os.path.join(HERE, "walk.animation.json"), "w") as fh:
        json.dump({"format_version": "1.8.0", "animations": anims}, fh, indent=1)


if __name__ == "__main__":
    make_png()
    make_geo()
    make_anim()
    print("fixtures written to", HERE)
