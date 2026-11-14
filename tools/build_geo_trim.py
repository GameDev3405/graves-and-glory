#!/usr/bin/env python3
"""Builds the veteran trim overlays of the GeckoLib troops (C15-F4: a Proven, Mastery II, troop shows its trim on the Geo rig too).

python tools/build_geo_trim.py [troop ...]      build every art/geo_trim/<troop>.json (or the named ones)
python tools/build_geo_trim.py --check [troop]  rebuild in memory and fail if a committed overlay differs (verify fast runs this)

Input, one recipe per troop: art/geo_trim/<troop>.json
  colours  {"light": hex, "dark": hex}: the trim's two brass tones (the legacy knight_veteran.png's palette).
  marks    [{bone, cube, pattern, rows, tiers}]: a band on the side faces (east, north, west, south) of that box-UV cube.
           pattern "rope": every texel of the row, light and dark alternating (a twisted edging); "studs": every other
           column, dark (stud heads). rows: indices into the cube's side height, negative from the bottom. tiers: the
           armour tiers the mark is painted for.
The geometry and the tier sheets are the rig exporter's (art/anim/common/export.py): geo/troop/<troop>.geo.json,
troop_rig/<troop>.json and textures/entity/troop/<troop>_geo/t<tier>_<side>.png. Nothing here writes or changes them.

Output: textures/entity/troop/<troop>_geo/veteran_t<tier>.png, one per tier the recipe paints, the sheet's size, alpha 0 or
255. GeoTroopRenderer draws it over the tier sheet (cutout, same geometry) when the Look is a veteran; a troop or tier without
the file draws plain, so the next Geo troops gain a trim by adding a recipe and running this, no code.

Rules (the legacy trim_check's, on the Geo sheet), each a build failure:
  - every trim texel lies on a texel every side's tier sheet paints, and on a side face of the named cube only: no other cube
    whose bone can show at that tier (or any weapon or draw-frame bone) uses that texel, so the brass lands nowhere else;
  - the marked bone shows at that armour tier and is not a team bone (so one overlay serves every side);
  - at most TRIM_SHARE of the tier sheet's opaque texels.
"""
from __future__ import annotations

import io
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/gravesandglory"
RECIPES = ROOT / "art/geo_trim"
TRIM_SHARE = 0.20       # as tools/build_skins.py: gilt kept rare
SIDES = ("gold", "violet", "crimson", "teal", "company", "linen")   # GeoTroopRenderer.SIDES


class TrimError(Exception):
    pass


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def box_rects(cube):
    """Every face rectangle (x0, y0, w, h) of a box-UV cube, sizes floored as GeckoLib does; per-face UV cubes give theirs."""
    uv = cube.get("uv", [0, 0])
    if isinstance(uv, dict):
        out = []
        for f in uv.values():
            (u, v), (w, h) = f.get("uv", [0, 0]), f.get("uv_size", [0, 0])
            x0, x1 = sorted((u, u + w)); y0, y1 = sorted((v, v + h))
            out.append((int(x0), int(y0), int(round(x1 - x0)) or 1, int(round(y1 - y0)) or 1))
        return out
    u, v = int(uv[0]), int(uv[1])
    sx, sy, sz = (int(s) for s in cube["size"])
    return [(u + sz, v, sx, sz), (u + sz + sx, v, sx, sz),                  # up, down
            (u, v + sz, 2 * sz + 2 * sx, sy)]                                # east, north, west, south as one strip


def side_strip(cube):
    """The side faces of a box-UV cube as one strip: x0, y0, width, height."""
    uv = cube.get("uv")
    if not isinstance(uv, list):
        raise TrimError("trim marks need a box-UV cube")
    u, v = int(uv[0]), int(uv[1])
    sx, sy, sz = (int(s) for s in cube["size"])
    return u, v + sz, 2 * sz + 2 * sx, sy


def load(troop):
    recipe = json.loads((RECIPES / f"{troop}.json").read_text(encoding="utf-8"))
    geo = json.loads((ASSETS / f"geo/troop/{troop}.geo.json").read_text(encoding="utf-8"))["minecraft:geometry"][0]
    rig = json.loads((ASSETS / f"troop_rig/{troop}.json").read_text(encoding="utf-8"))["bones"]
    return recipe, geo, rig


def can_show(rule, tier):
    """Whether a bone may show at this armour tier: armour-staged bones by their stages; weapon, draw and shield bones always
    may (their stage is the weapon tier or the off hand, not the armour tier)."""
    if rule.get("tag") in ("weapon", "draw", "shield"):
        return True
    st = rule.get("stages") or []
    return not st or tier in st


def build(troop):
    """{tier: Image} of the troop's overlays, checked."""
    recipe, geo, rig = load(troop)
    light, dark = hexrgb(recipe["colours"]["light"]), hexrgb(recipe["colours"]["dark"])
    tw, th = geo["description"]["texture_width"], geo["description"]["texture_height"]
    bones = {b["name"]: b for b in geo["bones"]}
    tiers = sorted({t for m in recipe["marks"] for t in m["tiers"]})
    out = {}
    for tier in tiers:
        sheets = []
        for side in SIDES:
            p = ASSETS / f"textures/entity/troop/{troop}_geo/t{tier}_{side}.png"
            if not p.exists():
                raise TrimError(f"{troop}: no tier sheet {p.name}")
            im = Image.open(p).convert("RGBA")
            if im.size != (tw, th):
                raise TrimError(f"{troop}: {p.name} is {im.size}, the geo says {(tw, th)}")
            sheets.append(im.load())
        # Who uses each texel at this tier: (bone, cube index) of every cube that can show.
        owners = {}
        for b in geo["bones"]:
            if not can_show(rig.get(b["name"], {}), tier):
                continue
            for ci, c in enumerate(b.get("cubes", [])):
                for x0, y0, w, h in box_rects(c):
                    for y in range(y0, y0 + h):
                        for x in range(x0, x0 + w):
                            owners.setdefault((x, y), set()).add((b["name"], ci))
        img = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
        px = img.load()
        for m in recipe["marks"]:
            if tier not in m["tiers"]:
                continue
            name, ci = m["bone"], m.get("cube", 0)
            if name not in bones:
                raise TrimError(f"{troop}: no bone {name}")
            rule = rig.get(name, {})
            if rule.get("team"):
                raise TrimError(f"{troop}: {name} is a team bone; a trim must serve every side")
            if not can_show(rule, tier):
                raise TrimError(f"{troop}: {name} does not show at armour tier {tier}")
            x0, y0, w, h = side_strip(bones[name]["cubes"][ci])
            for r in m["rows"]:
                y = y0 + (r if r >= 0 else h + r)
                if not y0 <= y < y0 + h:
                    raise TrimError(f"{troop}: {name} row {r} is outside its {h} rows")
                for i in range(w):
                    x = x0 + i
                    if m["pattern"] == "rope":
                        c = light if i % 2 == 0 else dark
                    elif m["pattern"] == "studs":
                        if i % 2:
                            continue
                        c = dark
                    else:
                        raise TrimError(f"{troop}: unknown pattern {m['pattern']}")
                    if owners.get((x, y), set()) != {(name, ci)}:
                        raise TrimError(f"{troop}: t{tier}: texel ({x}, {y}) of {name} is also used by {sorted(owners.get((x, y), set()) - {(name, ci)})}")
                    for s, sp in zip(SIDES, sheets):
                        if sp[x, y][3] == 0:
                            raise TrimError(f"{troop}: t{tier}: texel ({x}, {y}) lies on a texel the {s} sheet leaves clear")
                    px[x, y] = c
        n = sum(1 for y in range(th) for x in range(tw) if px[x, y][3])
        base = sum(1 for y in range(th) for x in range(tw) if sheets[-1][x, y][3])
        if n == 0:
            raise TrimError(f"{troop}: t{tier}: empty trim")
        if n > TRIM_SHARE * base:
            raise TrimError(f"{troop}: t{tier}: {n} trim texels, over {TRIM_SHARE:.0%} of the sheet's {base}")
        out[tier] = img
    return out


def target(troop, tier):
    return ASSETS / f"textures/entity/troop/{troop}_geo/veteran_t{tier}.png"


def png_bytes(img):
    b = io.BytesIO()
    img.save(b, "PNG", optimize=False)
    return b.getvalue()


def pixels_equal(a, b):
    return a.size == b.size and a.convert("RGBA").tobytes() == b.convert("RGBA").tobytes()


def main(argv):
    check = "--check" in argv
    names = [a for a in argv if not a.startswith("--")] or sorted(p.stem for p in RECIPES.glob("*.json"))
    bad = 0
    for troop in names:
        try:
            built = build(troop)
        except TrimError as e:
            print(f"build_geo_trim: FAIL {e}")
            bad += 1
            continue
        for tier, img in built.items():
            t = target(troop, tier)
            if check:
                if not t.exists() or not pixels_equal(Image.open(t), img):
                    print(f"build_geo_trim: FAIL {t.relative_to(ROOT)} is missing or not what art/geo_trim/{troop}.json builds; run tools/build_geo_trim.py {troop}")
                    bad += 1
            else:
                t.write_bytes(png_bytes(img))
            n = sum(1 for a in img.getchannel("A").tobytes() if a)
            print(f"build_geo_trim: {troop} t{tier}: {n} trim texels{' ok' if check else ' written'}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
