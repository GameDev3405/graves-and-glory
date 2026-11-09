"""Prop card: the one image a prop is reviewed on (docs/props/WORKFLOW.md, steps 3-5).

    python tools/prop_card.py <id> [--no-build] [--shots DIR] [--out DIR]

Builds the prop (art/props/build_props.py <id>), then writes work/props/cards/<id>/card.png and card.json:
  - 1x beside the Knight (game scale, 16 px a block) and a 4x turnaround: front, 3/4 front-east, 3/4 front-west,
    east side, back, top
  - a collision map: every cell of the footprint, layer by layer, coloured by how much model fills it and whether the
    def makes it solid
  - the in-game shots, if tools/prop_shot.py has put them in work/props/shots/<id>/
  - the numbers and the automatic checks

Checks (FAIL = exit 1, the prop is not ready for review; WARN = the reviewer must look at it):
  def        the def exists, its part models are exactly the exported part files, size matches
  density    every face 1 texel per model px (tools/prop_sheet.py)
  frame      every stored element inside -16..32
  footprint  the assembled model inside the footprint, at most 2 px overhang, nothing below the floor
  palette    every texel colour comes from a proplib ramp (named ramps, make_ramp, House colours)
  budget     elements <= 64 (WARN above, FAIL above 96); sheet <= 128 px (256 allowed over 8 cells)
  collision  WARN on a solid cell the model hardly fills (invisible wall) or an open cell it mostly fills (walk-through)
  brief      art/props/<id>/brief.md exists with no TODO left
"""
import argparse, importlib.util, json, math, os, re, sys
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART = os.path.join(ROOT, "art", "props")
DEFS = os.path.join(ROOT, "src", "main", "resources", "data", "gravesandglory", "gg_props")
sys.path.insert(0, os.path.join(ROOT, "tools")); sys.path.insert(0, ART)
import prop_sheet as ps   # noqa: E402
import proplib            # noqa: E402

BG = (36, 38, 46, 255); FG = (225, 225, 225, 255); DIM = (150, 155, 165, 255)
OKC = (110, 200, 120, 255); WARNC = (235, 190, 80, 255); FAILC = (240, 100, 90, 255)

def font(n):
    for f in ("arial.ttf", "DejaVuSans.ttf"):
        try: return ImageFont.truetype(f, n)
        except OSError: pass
    return ImageFont.load_default()

def load_builders():
    spec = importlib.util.spec_from_file_location("build_props", os.path.join(ART, "build_props.py"))
    m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m

def all_ramp_colours(prop):
    """Every colour a proplib ramp can give: module ramps, House colours, and every Mat the prop's elements use."""
    cols = set()
    def add(r):
        for c in r: cols.add(tuple(c[:3]))
    for v in vars(proplib).values():
        if isinstance(v, list) and len(v) == 5 and all(isinstance(c, tuple) and len(c) == 4 for c in v): add(v)
    for h in proplib.HOUSE.values():
        for r in h.values(): add(r)
    for m in proplib.MAT.values(): add(m.ramp)
    for els in prop.parts.values():
        for e in els:
            for f in proplib.FACES:
                m = e.mat_for(f)
                if hasattr(m, "ramp"): add(m.ramp)
                for r in getattr(m, "ramps", ()): add(r)
    return cols

def cell_fill(els, d):
    """Fraction of each footprint cell (x,y,z in def cells) filled by element boxes (rotations ignored, overlaps capped)."""
    sx, sy, sz = d["size"]; px, pz = (sx - 1) // 2, (sz - 1) // 2
    fill = {}
    for cx in range(sx):
        for cy in range(sy):
            for cz in range(sz):
                lo = ((cx - px) * 16, cy * 16, (cz - pz) * 16); v = 0.0
                for frm, to, *_ in els:
                    o = 1.0
                    for i in range(3):
                        o *= max(0.0, min(to[i], lo[i] + 16) - max(frm[i], lo[i]))
                    v += o
                fill[(cx, cy, cz)] = min(1.0, v / 4096.0)
    return fill

def collision_map(d, fill, k=24):
    sx, sy, sz = d["size"]; solid = {tuple(c) for c in d["solid"]}
    lights = {(l["x"], l["y"], l["z"]) for l in d.get("lights", [])}
    f = font(11); tiles = []
    for y in range(sy):
        W, H = sx * k + 2, sz * k + 2 + 14
        t = Image.new("RGBA", (W, H), BG); g = ImageDraw.Draw(t)
        g.text((1, 0), "y=%d" % y, fill=DIM, font=f)
        for x in range(sx):
            for z in range(sz):
                v = fill[(x, y, z)]; s = (x, y, z) in solid
                base = int(40 + 180 * v)
                col = (base, base, base, 255)
                x0, y0 = 1 + x * k, 15 + z * k
                g.rectangle([x0, y0, x0 + k - 2, y0 + k - 2], fill=col)
                if s: g.rectangle([x0, y0, x0 + k - 2, y0 + k - 2], outline=(220, 70, 60, 255), width=2)
                if (x, y, z) in lights: g.ellipse([x0 + 4, y0 + 4, x0 + k - 6, y0 + k - 6], fill=(255, 210, 80, 255))
        tiles.append(t)
    note = Image.new("RGBA", (190, 46), BG); g = ImageDraw.Draw(note)
    g.text((0, 0), "top-down per layer, north up\ngrey = model fill, red = solid\n(barrier), dot = light", fill=DIM, font=f)
    return ps.compose(tiles + [note], label="collision")

def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("id"); ap.add_argument("--no-build", action="store_true")
    ap.add_argument("--shots", default=None); ap.add_argument("--out", default=None)
    a = ap.parse_args(argv); pid = a.id
    out = a.out or os.path.join(ROOT, "work", "props", "cards", pid); os.makedirs(out, exist_ok=True)
    shots = a.shots or os.path.join(ROOT, "work", "props", "shots", pid)
    checks = []   # (name, level OK/WARN/FAIL, text)
    def chk(name, level, text): checks.append((name, level, text))

    B = load_builders(); build = dict(B.builders()).get(pid)
    if not build: print("no builder for %s (art/props/%s/model.py with build())" % (pid, pid)); return 1
    prop = build()
    if not a.no_build: proplib.write(prop, ART)

    dpath = os.path.join(DEFS, pid + ".json")
    if not os.path.exists(dpath): print("no def", dpath); return 1
    d = json.load(open(dpath, encoding="utf8"))
    tex, parts = ps.load_prop(ART, pid)
    files = {n for n, _ in parts}; defparts = [p["model"] for p in d["parts"]]
    if set(defparts) != files or set(prop.parts) != files:
        chk("def", "FAIL", "def parts %s, exported %s, model.py %s" % (sorted(defparts), sorted(files), sorted(prop.parts)))
    else: chk("def", "OK", "parts %s, size %s" % (",".join(defparts), d["size"]))
    defglow = {p["model"] for p in d["parts"] if p.get("glow")}
    if defglow != set(prop.glow): chk("def", "FAIL", "glow parts: def %s, model.py %s" % (sorted(defglow), sorted(prop.glow)))

    bad = []
    for n, m in parts: bad += ps.check_density("%s/%s" % (pid, n), m, tex.size)
    chk("density", "FAIL" if bad else "OK", "; ".join(bad[:4]) if bad else "1 texel per model px on every face")

    out_frame = []
    for n, m in parts:
        for i, e in enumerate(m["elements"]):
            if min(e["from"]) < -16 or max(e["to"]) > 32: out_frame.append("%s#%d" % (n, i))
    chk("frame", "FAIL" if out_frame else "OK", ("outside -16..32: " + ", ".join(out_frame[:6])) if out_frame else "all parts inside -16..32")

    els = ps.elements_of(parts)
    sx, sy, sz = d["size"]; px, pz = (sx - 1) // 2, (sz - 1) // 2
    box = ((-16 * px, 0, -16 * pz), (16 * (sx - px), 16 * sy, 16 * (sz - pz)))
    lo = [min(e[0][i] for e in els) for i in range(3)]; hi = [max(e[1][i] for e in els) for i in range(3)]
    over = [max(box[0][i] - lo[i], hi[i] - box[1][i]) for i in range(3)]
    if lo[1] < 0 or max(over) > 2:
        chk("footprint", "FAIL", "model x %g..%g y %g..%g z %g..%g vs footprint %s..%s" % (lo[0], hi[0], lo[1], hi[1], lo[2], hi[2], box[0], box[1]))
    else: chk("footprint", "OK", "model x %g..%g y %g..%g z %g..%g in footprint (overhang %g px)" % (lo[0], hi[0], lo[1], hi[1], lo[2], hi[2], max(0, max(over))))

    allowed = all_ramp_colours(prop)
    import numpy as np
    A = np.asarray(tex); A = A[A[..., 3] > 0][:, :3]
    used = {tuple(int(v) for v in c) for c in np.unique(A, axis=0)}
    off = [c for c in used if c not in allowed]
    chk("palette", "FAIL" if off else "OK", ("%d off-ramp colours, e.g. %s" % (len(off), ["#%02X%02X%02X" % c for c in off[:4]])) if off
        else "%d colours, all from proplib ramps" % len(used))

    n_el = sum(len(m["elements"]) for _, m in parts); cells = sx * sy * sz
    chk("budget", "FAIL" if n_el > 96 else ("WARN" if n_el > 64 else "OK"), "%d elements (64 target, 96 limit)" % n_el)
    sheet_cap = 256 if cells > 8 else 128
    chk("budget", "FAIL" if tex.size[0] > sheet_cap else "OK", "sheet %dx%d (cap %d)" % (tex.size[0], tex.size[1], sheet_cap))

    fill = cell_fill(els, d); solid = {tuple(c) for c in d["solid"]}
    wall = [c for c in solid if fill[c] < 0.08]; walk = [c for c, v in fill.items() if v > 0.5 and c not in solid]
    msg = []
    if wall: msg.append("solid but nearly empty (invisible wall): %s" % wall)
    if walk: msg.append("mostly filled but open (walk-through): %s" % walk)
    chk("collision", "WARN" if msg else "OK", "; ".join(msg) if msg else "%d solid cells match the model" % len(solid))

    bpath = os.path.join(ART, pid, "brief.md"); use = "?"
    if not os.path.exists(bpath): chk("brief", "FAIL", "no art/props/%s/brief.md" % pid)
    else:
        bt = open(bpath, encoding="utf8").read()
        m = re.search(r"\*\*Use:\*\*\s*([a-z]+)", bt); use = m.group(1) if m else "?"
        todo = bt.count("TODO")
        chk("brief", "FAIL" if todo else "OK", ("%d TODO left" % todo) if todo else "filled")

    # ---- the card image ---------------------------------------------------------------------------------------
    kn1, kn4 = ps.knight_front(1), ps.knight_front(4)
    r1 = [ps.on_swatch(kn1, "grass", 1)] if kn1 else []
    r1 += [ps.on_swatch(ps.render(els, tex, 0, 0, 1), "grass", 1), ps.on_swatch(ps.render(els, tex, 35, 25, 1), "stone", 1)]
    views = [("front", 0, 0), ("3/4 front-east", 35, 25), ("3/4 front-west", -35, 25), ("east side", 90, 10), ("back", 180, 15), ("top", 0, 89)]
    r4 = ([ps.on_swatch(kn4, "grass", 4)] if kn4 else []) + [ps.on_swatch(ps.render(els, tex, y, p, 4), "stone" if i else "grass", 4) for i, (_, y, p) in enumerate(views)]
    rows = [ps.compose([ps.compose(r1, label="1x game scale"), ps.compose(r4, label="4x: " + ", ".join(v[0] for v in views))])]
    rows.append(collision_map(d, fill))
    shot_files = sorted(f for f in (os.listdir(shots) if os.path.isdir(shots) else []) if f.endswith(".png"))
    if shot_files:
        ims = []
        for f in shot_files[:4]:
            im = Image.open(os.path.join(shots, f)).convert("RGBA"); im = im.resize((480, int(480 * im.height / im.width)), Image.BILINEAR); ims.append(im)
        rows.append(ps.compose(ims, label="in game: " + ", ".join(os.path.splitext(f)[0].replace("p1-prop-" + pid + "-", "") for f in shot_files[:4])))
    else:
        chk("in-game", "WARN", "no shots in %s (run tools/prop_shot.py %s)" % (os.path.relpath(shots, ROOT), pid))

    knight_h = kn1.height if kn1 else 32
    numbers = ["cells %dx%dx%d (x*y*z), anchor (%d,0,%d), front north" % (sx, sy, sz, px, pz),
               "height %g px = %.2f blocks = %.2f x the Knight (%d px)" % (hi[1], hi[1] / 16, hi[1] / knight_h, knight_h),
               "elements %d in %d part(s); sheet %dx%d; %d colours; solid %d cells; lights %d; effects %d" %
               (n_el, len(parts), tex.size[0], tex.size[1], len(used), len(solid), len(d.get("lights", [])), len(d.get("effects", []))),
               "cmd %s" % ", ".join("%s=%d" % (p["model"], p["cmd"]) for p in d["parts"])]
    fb, fs = font(18), font(13)
    W = max(r.width for r in rows); hh = 34 + 18 * len(numbers) + 17 * len(checks) + 10
    head = Image.new("RGBA", (max(W, 900), hh), BG); g = ImageDraw.Draw(head)
    g.text((8, 6), "%s  (%s)  use: %s  category: %s" % (d["name"], pid, use, d.get("category", "?")), fill=FG, font=fb)
    y = 32
    for line in numbers: g.text((8, y), line, fill=DIM, font=fs); y += 18
    for name, lvl, text in checks:
        g.text((8, y), lvl, fill={"OK": OKC, "WARN": WARNC, "FAIL": FAILC}[lvl], font=fs)
        g.text((62, y), "%-10s %s" % (name, text), fill=FG, font=fs); y += 17
    rows.insert(0, head)
    W = max(r.width for r in rows); H = sum(r.height for r in rows)
    card = Image.new("RGBA", (W, H), BG); y = 0
    for r in rows: card.alpha_composite(r, (0, y)); y += r.height
    card.save(os.path.join(out, "card.png"))
    res = {"id": pid, "name": d["name"], "use": use, "size": d["size"], "elements": n_el, "sheet": list(tex.size), "colours": len(used),
           "height_px": hi[1], "knight_px": knight_h, "checks": [{"check": n, "level": l, "text": t} for n, l, t in checks],
           "shots": shot_files}
    json.dump(res, open(os.path.join(out, "card.json"), "w"), indent=1)
    for n, l, t in checks: print("%-4s %-10s %s" % (l, n, t))
    print("card", os.path.join(out, "card.png"), card.size)
    return 1 if any(l == "FAIL" for _, l, _ in checks) else 0

if __name__ == "__main__":
    sys.exit(main())
