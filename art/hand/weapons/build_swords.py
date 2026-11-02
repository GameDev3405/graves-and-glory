#!/usr/bin/env python3
"""The knight's arming sword, tiers I to III, hand-authored at one texel per model pixel to match the 1x knight.

Every cube is placed by hand on whole pixels and every face is painted from the knight's own ramps (steel, gold,
leather), so sword and wearer read as one piece. The sources land in art/weapons/ as Blockbench projects with the
texture embedded, a colour sheet, a material key map and the weapon json; tools/build_skins.py compiles them.

Frame: Blockbench, y up, the grip's centre at the origin (the fist covers y -2..+2), the blade up +y, its flats across
z (north is the front). Light rules: the honed edges are the brightest steel, the ridge or fuller runs down the middle,
tops catch light, bottoms sit in shadow.

  python build_swords.py preview <png>   a contact sheet of the three tiers, front, three-quarter and edge on
  python build_swords.py install         the sources into art/weapons/, then tools/build_skins.py
"""
import base64, io, json, math, os, sys, uuid
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
sys.path.insert(0, os.path.join(HERE, "..", "knight"))
from build_knight import STEEL, GOLD, LEATH, ramp   # noqa: E402  the knight's own ramps

# The knight's gold with its two middle lights eased under the weapon tint rule (saturation 0.45 or value 0.60 near a side
# colour, tools/build_skins.py tint): the shadows are the knight's own, the lights the paler gilt the old swords used.
GILT = GOLD[:3] + ramp("96783a", "bdaa6a", "e4d780") + GOLD[6:]

SHEET_W = 32
KEY = {"steel": (255, 0, 0), "iron": (170, 0, 0), "gilt": (255, 170, 0), "leather": (0x55, 0x33, 0x22)}
FACES = ("north", "east", "south", "west", "up", "down")
GRIP = {"offset": [0, 0, 0], "rotation": [90, 45, 90],
        "note": "the grip's centre is the model origin (the fist box covers y -2..+2); the model is y up with its big faces "
                "across z, and the converter turns it by a half turn about x into the weapon frame; the sword is turned flat "
                "to the side and tilted 45 degrees forward, as the sprite sword was"}


class Cube:
    """A box on whole pixels: lo and hi corners, a material, a painter per face (face, w, h) -> rows of colours, top row
    first as seen from outside; a face left out of `skip` is drawn. `turn` is a rotation about z at `pivot`, degrees."""

    def __init__(self, name, lo, hi, mat, paint, skip=(), turn=0, pivot=None, lit=()):
        self.name, self.lo, self.hi, self.mat, self.paint, self.skip = name, lo, hi, mat, paint, set(skip)
        self.lit = set(lit)   # faces that glow whole (the _e sheet); a 1x texel counts 4 against the glow cap
        self.turn, self.pivot = turn, pivot or [(a + b) / 2 for a, b in zip(lo, hi)]

    def size(self, face):
        dx, dy, dz = (int(round(b - a)) for a, b in zip(self.lo, self.hi))
        return {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}[face]


def flat(c):
    return lambda face, w, h: [[c] * w for _ in range(h)]


def by_face(**f):
    """A painter that picks one painter a face; `rest` paints the faces not named."""
    return lambda face, w, h: (f.get(face) or f["rest"])(face, w, h)


def taper(cols, rows=None, dark=None):
    """A blade flat: `across` with its top row's outer columns stepped down to `dark`, so the point reads as tapering."""
    def r(face, y, h):
        o = dict(rows(face, y, h)) if rows else {}
        if y == 0 and dark: o.setdefault(0, dark); o.setdefault(len(cols) - 1, dark)
        return o
    return across(cols, r)


def across(cols, rows=None):
    """Columns given left to right along +x (the wearer's view of the flat); the north face sees them mirrored. `rows`
    (face, y from the top, h) -> {column: colour} overrides single texels."""
    def paint(face, w, h):
        g = []
        for y in range(h):
            line = list(cols if face == "south" else cols[::-1])
            for x, c in (rows(face, y, h) if rows else {}).items():
                line[x if face == "south" else w - 1 - x] = c
            g.append(line)
        return g
    return paint


def wrap(a, b, gild=None):
    """A leather grip wound on the bias: alternate bands, a gilt wire in every third band when `gild` is given."""
    def paint(face, w, h):
        g = []
        for y in range(h):
            row = []
            for x in range(w):
                k = (y + x) % 3
                row.append(gild if (gild and k == 0) else (a if k != 2 else b))
            g.append(row)
        return g
    return paint


def ring(edge, body, hub):
    """A round boss on a square face: dark corners, a lit rim at the top, `hub` in the middle."""
    def paint(face, w, h):
        g = [[body] * w for _ in range(h)]
        for y in range(h):
            for x in range(w):
                if (x in (0, w - 1)) and (y in (0, h - 1)):
                    g[y][x] = edge
        if w >= 3 and h >= 3:
            g[h // 2][w // 2] = hub
        return g
    return paint


def bar(top, mid, low, ends=None):
    """A guard bar's flat: light along the top row, shadow along the bottom, darker end texels."""
    def paint(face, w, h):
        g = []
        for y in range(h):
            c = top if y == 0 and h > 1 else (low if y == h - 1 and h > 1 else mid)
            row = [c] * w
            if ends and w > 2:
                row[0] = row[-1] = ends
            g.append(row)
        return g
    return paint


# ---- the three swords ----

def tier1():
    """The Squire's iron sword: a plain diamond-section blade with two nicks, a straight bar, a block pommel."""
    S = STEEL
    nicks = lambda face, y, h: {0: S[2]} if y == 4 else ({2: S[2]} if y == 7 else {})
    return [
        Cube("pommel", [-1, -4, -1], [1, -2, 1], "iron", by_face(up=flat(S[4]), down=flat(S[1]), rest=bar(S[4], S[3], S[2], S[2]))),
        Cube("grip", [-0.5, -2, -0.5], [0.5, 2, 0.5], "leather", wrap(LEATH[3], LEATH[1]), skip=("up", "down")),
        Cube("guard", [-3.5, 2, -1], [3.5, 3, 1], "iron", by_face(up=flat(S[4]), down=flat(S[1]), rest=bar(S[3], S[3], S[3], S[2]))),
        Cube("blade", [-1.5, 3, -0.5], [1.5, 14, 0.5], "iron",
             by_face(north=taper([S[4], S[5], S[3]], nicks, S[2]), south=taper([S[4], S[5], S[3]], nicks, S[2]),
                     up=flat(S[4]), rest=flat(S[5])), skip=("down",)),
        Cube("tip", [-0.5, 14, -0.5], [0.5, 16, 0.5], "iron",
             by_face(north=across([S[5]], lambda f, y, h: {0: S[6]} if y == 0 else {}),
                     south=across([S[5]], lambda f, y, h: {0: S[6]} if y == 0 else {}), rest=flat(S[5])), skip=("down",)),
    ]


def tier2():
    """The sergeant's steel sword: a fullered blade, a centre block, quillons turned up toward the blade, a wheel pommel."""
    S = STEEL
    def fuller(face, y, h):   # the groove runs two thirds up, then the ridge rises to the point
        return {1: S[3] if y >= h // 3 else S[6]} | ({1: S[4]} if y == h // 3 else {})
    blade = taper([S[5], S[3], S[4]], fuller, S[3])
    return [
        Cube("pommel", [-1.5, -5, -0.5], [1.5, -2, 0.5], "steel", by_face(north=ring(S[2], S[4], S[6]), south=ring(S[2], S[4], S[6]),
             up=flat(S[5]), down=flat(S[1]), rest=flat(S[3]))),
        Cube("rivet", [-0.5, -4, -1], [0.5, -3, 1], "steel", by_face(north=flat(S[6]), south=flat(S[6]), rest=flat(S[4]))),
        Cube("grip", [-0.5, -2, -0.5], [0.5, 2, 0.5], "leather", wrap(LEATH[3], LEATH[2]), skip=("up", "down")),
        Cube("guard", [-3.5, 2, -0.5], [3.5, 3, 0.5], "steel", by_face(up=flat(S[5]), down=flat(S[1]), rest=flat(S[3]))),
        Cube("quillon_l", [-4.5, 2, -0.5], [-3.5, 4, 0.5], "steel", by_face(up=flat(S[6]), down=flat(S[1]), rest=bar(S[6], S[4], S[2]))),
        Cube("quillon_r", [3.5, 2, -0.5], [4.5, 4, 0.5], "steel", by_face(up=flat(S[6]), down=flat(S[1]), rest=bar(S[6], S[4], S[2]))),
        Cube("block", [-1.5, 1, -1], [1.5, 4, 1], "steel", by_face(up=flat(S[6]), down=flat(S[1]), rest=bar(S[5], S[4], S[2], S[3]))),
        Cube("blade", [-1.5, 4, -0.5], [1.5, 16, 0.5], "steel",
             by_face(north=blade, south=blade, up=flat(S[5]), rest=flat(S[6])), skip=("down",)),
        Cube("tip", [-0.5, 16, -0.5], [0.5, 18, 0.5], "steel", by_face(up=flat(S[6]), rest=flat(S[6])), skip=("down",)),
    ]


def tier3():
    """The knight's gilded sword: the steel blade with a gilt thread in its fuller and a gilt ricasso, a gilt cross with
    diamond terminals over a gilt centre block, a gilt-wired grip and a gilt star pommel with a steel boss."""
    S, G = STEEL, GILT
    def fuller(face, y, h):
        if y >= h - 3: return {1: (G[5], G[4], G[2])[y - (h - 3)]}   # the gilt ricasso at the blade root
        if y >= h // 3: return {1: G[3]}                              # the fuller, inlaid with gilt
        return {1: S[6]}
    blade = taper([S[5], G[3], S[4]], fuller, S[3])
    PY = -2 - 1.5 * math.sqrt(2)   # the diamond pommel's top point just meets the grip's end
    star = lambda face, w, h: [[G[1] if (x in (0, w - 1)) and (y in (0, h - 1)) else (G[5] if y == 0 else G[3]) for x in range(w)] for y in range(h)]
    return [
        Cube("pommel", [-1.5, PY - 1.5, -0.5], [1.5, PY + 1.5, 0.5], "gilt", by_face(north=star, south=star, up=flat(G[5]), down=flat(G[1]), rest=flat(G[3])),
             turn=45),
        Cube("boss", [-0.5, PY - 0.5, -1], [0.5, PY + 0.5, 1], "steel", by_face(north=flat(S[6]), south=flat(S[6]), rest=flat(S[4]))),
        Cube("grip", [-0.5, -2, -0.5], [0.5, 2, 0.5], "leather", wrap(LEATH[3], LEATH[1], G[4]), skip=("up", "down")),
        Cube("guard", [-3.5, 2, -0.5], [3.5, 3, 0.5], "gilt", by_face(up=flat(G[5]), down=flat(G[1]), rest=bar(G[4], G[3], G[2]))),
        Cube("terminal_l", [-5.4, 1.5, -1], [-3.4, 3.5, 1], "gilt", by_face(north=star, south=star, rest=flat(G[3])), turn=45),
        Cube("terminal_r", [3.4, 1.5, -1], [5.4, 3.5, 1], "gilt", by_face(north=star, south=star, rest=flat(G[3])), turn=45),
        Cube("block", [-1.5, 1, -1], [1.5, 4, 1], "gilt", by_face(up=flat(G[5]), down=flat(G[1]), rest=bar(G[5], G[3], G[2], G[2]))),
        Cube("blade", [-1.5, 4, -0.5], [1.5, 17, 0.5], "steel",
             by_face(north=blade, south=blade, up=flat(S[5]), rest=flat(S[6])), skip=("down",)),
        Cube("tip", [-0.5, 17, -0.5], [0.5, 19, 0.5], "steel", by_face(up=flat(S[6]), rest=flat(S[6])), skip=("down",)),
    ]


TIERS = {
    "arming_sword": (tier1, "The Squire's arming sword, tier I (plain), hand-authored at 1 texel a px in the knight's ramps: a 3 px "
                     "iron blade of diamond section (11 px and a 2 px point) with two nicks in its edges, a straight 7 px iron bar, "
                     "a bias-wound leather grip and a 2 px block pommel; no gilt; 20 px long."),
    "arming_sword_t2": (tier2, "The Squire's arming sword, tier II (upgraded), hand-authored at 1 texel a px in the knight's ramps: a 3 px "
                        "steel blade with a grey fuller two thirds up and a bright ridge to the point (12 px and 2), a steel centre "
                        "block and bar whose quillons turn up toward the blade, a leather grip and a 3 px wheel pommel with a "
                        "bright rivet; 23 px long."),
    "arming_sword_t3": (tier3, "The Squire's arming sword, tier III (elite), hand-authored at 1 texel a px in the knight's ramps: a 3 px "
                        "polished steel blade with a gilt thread in its fuller and a gilt ricasso (13 px and 2), a gilt bar with "
                        "diamond terminals over a gilt centre block, a gilt-wired leather grip and a gilt star pommel round a "
                        "steel boss; 24 px long."),
}


# ---- packing and files ----

def pack(cubes):
    """Paints every face and shelf-packs it on a SHEET_W wide sheet. Returns (colour, key, {(cube, face): uv}); pack.glow is
    the _e sheet, or None when no cube lights a face."""
    patches = []
    for c in cubes:
        for f in FACES:
            if f in c.skip: continue
            w, h = c.size(f)
            if w and h: patches.append((c, f, w, h, c.paint(f, w, h)))
    order = sorted(patches, key=lambda p: (-p[3], -p[2]))
    x = y = shelf = 0; at = {}
    for c, f, w, h, _ in order:
        if x + w > SHEET_W: x, y, shelf = 0, y + shelf, 0
        at[(c.name, f)] = (x, y); x += w; shelf = max(shelf, h)
    H = y + shelf
    H = ((H + 7) // 8) * 8
    colour = Image.new("RGBA", (SHEET_W, H), (0, 0, 0, 0)); key = Image.new("RGBA", (SHEET_W, H), (0, 0, 0, 0))
    glow = Image.new("RGBA", (SHEET_W, H), (0, 0, 0, 0)) if any(c.lit for c in cubes) else None
    uv = {}
    for c, f, w, h, g in patches:
        x0, y0 = at[(c.name, f)]
        assert len(g) == h and all(len(r) == w for r in g), (c.name, f, w, h)
        for yy in range(h):
            for xx in range(w):
                if g[yy][xx] is None: continue
                colour.putpixel((x0 + xx, y0 + yy), g[yy][xx]); key.putpixel((x0 + xx, y0 + yy), KEY[c.mat] + (255,))
                if f in c.lit: glow.putpixel((x0 + xx, y0 + yy), tuple(g[yy][xx][:3]) + (255,))
        uv[(c.name, f)] = [x0, y0, x0 + w, y0 + h]
    pack.glow = glow
    return colour, key, uv


def uid(*parts): return str(uuid.uuid5(uuid.NAMESPACE_URL, "gravesandglory/" + "/".join(parts)))


def project(wid, cubes, colour, uv):
    buf = io.BytesIO(); colour.save(buf, "PNG")
    elements = []
    for c in cubes:
        e = {"name": c.name, "box_uv": False, "render_order": "default", "locked": False, "export": True,
             "from": c.lo, "to": c.hi, "autouv": 0, "color": 0, "origin": c.pivot,
             "faces": {f: {"uv": uv[(c.name, f)], "texture": 0} for f in FACES if (c.name, f) in uv},
             "type": "cube", "uuid": uid(wid, c.name)}
        if c.turn: e["rotation"] = [0, 0, c.turn]
        elements.append(e)
    rest = uid(wid, "rest")
    W, H = colour.size
    return {"meta": {"format_version": "5.0", "model_format": "free", "box_uv": False}, "name": wid, "model_identifier": "",
            "resolution": {"width": 16, "height": 16}, "elements": elements,
            "groups": [{"name": "rest", "uuid": rest, "export": True, "origin": [0, 0, 0], "rotation": [0, 0, 0], "children": []}],
            "outliner": [{"uuid": rest, "isOpen": False, "children": [e["uuid"] for e in elements]}],
            "textures": [{"name": wid, "id": "0", "width": W, "height": H, "uv_width": W, "uv_height": H, "internal": True,
                          "uuid": uid(wid, "texture"), "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()}]}


def outputs(tiers=None):
    """{path under art/weapons/: PIL image, or a JSON object} for the three tiers (another builder passes its own)."""
    out = {}
    for wid, (make, note) in (tiers or TIERS).items():
        cubes = make()
        colour, key, uv = pack(cubes)
        out[wid + ".bbmodel"] = project(wid, cubes, colour, uv)
        out[wid + ".png"], out[wid + "_m.png"] = colour, key
        if pack.glow: out[wid + "_e.png"] = pack.glow
        out[wid + ".json"] = {"weapon": wid, "density": 1, "thickness": 2, "grip": GRIP, "note": note}
    return out


# ---- preview ----

def rot(rx, ry, rz):
    a, b, c = (math.radians(v) for v in (rx, ry, rz))
    Rx = [[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]]
    Ry = [[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]]
    Rz = [[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]]
    m = lambda A, B: [[sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return m(Rz, m(Ry, Rx))

def app(M, p): return [sum(M[i][k] * p[k] for k in range(3)) for i in range(3)]

CORNER = {   # (s, t) in the patch, s left to right, t top to bottom, as seen from outside -> picks of lo/hi
    "north": lambda s, t: (1 - s, 1 - t, 0), "south": lambda s, t: (s, 1 - t, 1), "east": lambda s, t: (1, 1 - t, 1 - s),
    "west": lambda s, t: (0, 1 - t, s), "up": lambda s, t: (s, 1, t), "down": lambda s, t: (s, 0, 1 - t)}
NORMAL = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}

def render(cubes, yaw, pitch, px=10, bg=(40, 42, 50, 255)):
    from PIL import ImageDraw
    cam = rot(pitch, yaw, 0); light = [-0.35, 0.85, -0.45]; quads = []
    for c in cubes:
        R = rot(0, 0, c.turn)
        def world(p): q = app(R, [p[i] - c.pivot[i] for i in range(3)]); return [q[i] + c.pivot[i] for i in range(3)]
        for f in FACES:
            if f in c.skip: continue
            w, h = c.size(f)
            if not (w and h): continue
            g = c.paint(f, w, h); n = app(R, NORMAL[f])
            shade = 0.62 + 0.38 * max(0.0, sum(n[i] * light[i] for i in range(3)))
            for ty in range(h):
                for tx in range(w):
                    col = g[ty][tx]
                    if col is None: continue
                    pts = []
                    for s, t in ((tx / w, ty / h), ((tx + 1) / w, ty / h), ((tx + 1) / w, (ty + 1) / h), (tx / w, (ty + 1) / h)):
                        pick = CORNER[f](s, t)
                        p = world([c.lo[i] + (c.hi[i] - c.lo[i]) * pick[i] for i in range(3)])
                        q = app(cam, p); pts.append((q[0], -q[1], q[2]))
                    quads.append((sum(p[2] for p in pts) / 4, [(p[0], p[1]) for p in pts], tuple(int(col[i] * shade) for i in range(3)) + (255,)))
    xs = [x for _, ps, _ in quads for x, _ in ps]; ys = [y for _, ps, _ in quads for _, y in ps]
    mx, my = min(xs) - 1, min(ys) - 1
    img = Image.new("RGBA", (int((max(xs) - mx + 1) * px), int((max(ys) - my + 1) * px)), bg); d = ImageDraw.Draw(img)
    for _, ps, col in sorted(quads, key=lambda q: -q[0]):
        d.polygon([((x - mx) * px, (y - my) * px) for x, y in ps], fill=col, outline=col)
    return img


def main(tiers=None, doc=__doc__):
    tiers = tiers or TIERS
    if len(sys.argv) < 2: print(doc); return
    if sys.argv[1] == "preview":
        tiles = []
        for wid, (make, _) in tiers.items():
            cubes = make()
            tiles += [render(cubes, y, p) for y, p in ((0, 0), (35, 15), (90, 0), (180, 0))]
        H = max(t.height for t in tiles); W = sum(t.width for t in tiles) + 12 * (len(tiles) + 1)
        sheet = Image.new("RGBA", (W, H + 24), (40, 42, 50, 255)); x = 12
        for t in tiles: sheet.paste(t, (x, 12 + H - t.height)); x += t.width + 12
        sheet.save(sys.argv[2]); print("preview", sys.argv[2])
    elif sys.argv[1] == "install":
        import subprocess
        sys.path.insert(0, os.path.join(REPO, "tools"))
        import bbmodel
        for name, thing in outputs(tiers).items():
            path = os.path.join(REPO, "art", "weapons", name)
            if name.endswith(".bbmodel"): bbmodel.write(thing, path)
            elif isinstance(thing, dict):
                with open(path, "w", encoding="utf-8", newline="\n") as f: f.write(json.dumps(thing, indent=1) + "\n")
            else: thing.save(path)
        sys.exit(subprocess.call([sys.executable, os.path.join(REPO, "tools", "build_skins.py")], cwd=REPO))


if __name__ == "__main__":
    main()
