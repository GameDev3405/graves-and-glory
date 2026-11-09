#!/usr/bin/env python3
"""Render every troop at every tier, close up and at game distance, for the unit texture audit.

Builds each troop with its `art/hand/<id>/build_<id>.py preview` command (the same builders that ship the
textures; `tools/build_skins.py --verify` proves they match), then renders the preview geo with GeckoLib's
offline renderer `tools/anim/render_geo.py` (on `anim/geckolib-rebuild`; pass its path with --renderer).

  python tools/art/texture_audit_render.py --renderer <render_geo.py> --out <dir> [--side crimson] [ids...]

Writes <out>/<id>/t<N>_near.png, t<N>_far.png and one contact sheet per House: <out>/house_<house>.png.
"""
import argparse, json, os, subprocess, sys
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
BG = (40, 42, 50)


def troops():
    d = json.load(open(os.path.join(ROOT, "sheets", "design.json"), encoding="utf-8"))
    return [(t["id"], t["house"], t["name"]) for t in d["troops"]]


def run(cmd):
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode:
        raise SystemExit("failed: %s\n%s%s" % (" ".join(cmd), r.stdout, r.stderr))


def render_troop(tid, renderer, out, side):
    pv = os.path.join(out, "_preview", tid)
    run([sys.executable, os.path.join(ROOT, "art", "hand", tid, "build_%s.py" % tid), "preview", pv, side])
    tiers = sorted(int(f[len(tid) + 2:-9]) for f in os.listdir(pv) if f.startswith(tid + "_t") and f.endswith(".geo.json"))
    td = os.path.join(out, tid); os.makedirs(td, exist_ok=True)
    for t in tiers:
        geo, tex = os.path.join(pv, "%s_t%d.geo.json" % (tid, t)), os.path.join(pv, "%s_t%d.png" % (tid, t))
        for kind, extra in (("near", ["--views", "front,three_quarter,back"]),
                            ("far", ["--views", "front,three_quarter", "--distance", "game16"])):
            rd = os.path.join(out, "_r", "%s_t%d_%s" % (tid, t, kind))
            run([sys.executable, renderer, "--geo", geo, "--texture", tex, "--out", rd, "--no-gif"] + extra)
            Image.open(os.path.join(rd, "frame_0000_t0.png")).save(os.path.join(td, "t%d_%s.png" % (t, kind)))
    return tiers


def house_sheet(house, rows, out):
    """rows: [(tid, name, tiers)]. One row per troop: per tier the near render at half size, then the
    game-distance render at its true pixel size and at 3x nearest (to see what survives at 16 blocks)."""
    cells = []
    for tid, name, tiers in rows:
        row = []
        for t in tiers:
            near = Image.open(os.path.join(out, tid, "t%d_near.png" % t)).convert("RGB")
            near = near.resize((near.width // 2, near.height // 2), Image.LANCZOS)
            far = Image.open(os.path.join(out, tid, "t%d_far.png" % t)).convert("RGB")
            row.append((t, near, far, far.resize((far.width * 2, far.height * 2), Image.NEAREST)))
        cells.append((tid, name, row))
    pad, lab = 8, 18
    widths = [sum(n.width + f.width + f3.width + 3 * pad for _, n, f, f3 in r) for _, _, r in cells]
    heights = [max(max(n.height, f3.height) for _, n, _, f3 in r) + lab + pad for _, _, r in cells]
    img = Image.new("RGB", (max(widths) + pad, sum(heights) + lab + pad), BG)
    d = ImageDraw.Draw(img)
    d.text((pad, 3), "House %s  |  per tier: close (front, 3/4, back) | 16 blocks true size | 16 blocks x2 nearest" % house, fill=(230, 230, 230))
    y = lab
    for (tid, name, r), h in zip(cells, heights):
        d.text((pad, y + 2), "%s  (%s)" % (name, tid), fill=(255, 220, 140))
        x = pad
        for t, n, f, f3 in r:
            d.text((x, y + lab - 2), "T%d" % t, fill=(200, 200, 200))
            img.paste(n, (x, y + lab + 8)); x += n.width + pad
            img.paste(f, (x, y + lab + 8)); x += f.width + pad
            img.paste(f3, (x, y + lab + 8)); x += f3.width + pad * 2
        y += h
    img.save(os.path.join(out, "house_%s.png" % house))


def troop_sheet(tid, name, tiers, out):
    """Compact grading sheet: one row per tier, close renders at 0.6 then the 16-block front and 3/4 at 2x nearest."""
    rows = []
    for t in tiers:
        near = Image.open(os.path.join(out, tid, "t%d_near.png" % t)).convert("RGB")
        near = near.resize((int(near.width * 0.6), int(near.height * 0.6)), Image.LANCZOS)
        far = Image.open(os.path.join(out, tid, "t%d_far.png" % t)).convert("RGB")
        far = far.resize((far.width * 2, far.height * 2), Image.NEAREST)
        rows.append((t, near, far))
    W = max(n.width + f.width for _, n, f in rows) + 30
    H = sum(max(n.height, f.height) + 4 for _, n, f in rows) + 16
    img = Image.new("RGB", (W, H), BG); d = ImageDraw.Draw(img)
    d.text((4, 2), "%s (%s)  close | 16 blocks x2" % (name, tid), fill=(255, 220, 140)); y = 16
    for t, n, f in rows:
        d.text((4, y + 4), "T%d" % t, fill=(220, 220, 220))
        img.paste(n, (26, y)); img.paste(f, (30 + n.width, y)); y += max(n.height, f.height) + 4
    img.save(os.path.join(out, "troop_%s.png" % tid))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--renderer", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--side", default="crimson")
    ap.add_argument("--sheets-only", action="store_true", help="reuse existing renders")
    ap.add_argument("ids", nargs="*")
    a = ap.parse_args()
    by_house = {}
    for tid, house, name in troops():
        if a.ids and tid not in a.ids:
            continue
        tiers = [1, 2, 3] if a.sheets_only else render_troop(tid, a.renderer, a.out, a.side)
        by_house.setdefault(house, []).append((tid, name, tiers))
        troop_sheet(tid, name, tiers, a.out)
        print(tid, "tiers", tiers, flush=True)
    for house, rows in by_house.items():
        house_sheet(house, rows, a.out)
        print("sheet", house)


if __name__ == "__main__":
    main()
