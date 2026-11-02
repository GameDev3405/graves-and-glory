#!/usr/bin/env python3
"""The Order Archer's recurve and the Longbowman's longbow, tiers I to III, hand-authored at one texel per model pixel
to match the 1x troops.

Both bows are animated: the rest frame and the three draw frames (pull_0 to pull_2) are top-level groups of one
Blockbench project, the limbs flex a little more in each, and the string runs from nock to nock at rest and from each nock
to the drawing hand's point once drawn. Cubes are whole pixels (a limb segment is 1 or 2 px thick, 1 or 2 px deep and a
whole number of px long, turned to its angle); only the string is under a pixel. A frame's limbs are the same cubes turned
a little further, so every frame reads the rest frame's painted faces and the sheet holds each patch once.

Frame: Blockbench, y up, the grip's centre at the origin (the fist covers y -2..+2), the string to the left (-x), the
limbs curving toward it in the x-y plane, their flats across z. Light rules: tops catch light, the back of the stave
(+x) is lighter than its belly (-x), bottoms sit in shadow.

  The Order Archer's recurve  order_bow, order_bow_t2, order_bow_t3: five staves a limb, the tips turned back away from
    the string. I plain dark wood, leather risers, self nocks. II walnut, horn nocks, steel collars on a wrapped grip.
    III gilt-capped horn nocks, a gilt-wired grip, gilt knee bands and a riser plate set with the Order's gilt cross.
  The Longbowman's tall self bow  longbow, longbow_t2, longbow_t3: a shallow D of yew, light sapwood back and darker
    heartwood belly. II is tipped with horn, III bound with gilt bands and hung with a white feather at the grip.

  python build_bows.py preview <png>   every bow: rest and the three draw frames in profile, then a three-quarter view
  python build_bows.py install         the sources into art/weapons/, then tools/build_skins.py
"""
import math, os, sys
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import build_swords as W
from build_swords import Cube, flat, by_face, wrap, bar, STEEL as S, GILT as G, LEATH as L, ramp, FACES
from build_knight import LINEN as H   # the horn's ramp

W.KEY.update({"wood": (0x8B, 0x45, 0x13), "bone": (0xEE, 0xEE, 0xDD), "linen": (255, 255, 255)})

# Woods and horn. A saturated light tone near the sides' hues fails tools/build_skins.py tint, so the lights stay pale and
# the saturated ones stay dark.
DK = ramp("1f140e", "33221a", "4a3324", "63462f", "7d5b3f", "957354")          # the plain dark wood
WN = ramp("2b1a11", "4a2e1c", "6b4429", "8a5c3a", "a07a5c", "bf9b78")          # walnut: richer and brighter
SAP = ramp("8f7550", "a98e63", "c4a77a", "d9be92", "e8d3ab", "f3e3c5")         # yew sapwood, the pale back of the stave
HEART = ramp("2e1a12", "452818", "5e371f", "6a3e22", "7e4a2c", "8c5634")       # yew heartwood, the dark belly
STRING = (217, 211, 193, 255)
FEATHER = ramp("958262", "bfac86", "ddcfaa", "f5edd8")

GRIP = {"offset": [0, 0, 0], "rotation": [0, 0, 0],
        "note": "the grip's centre is the model origin (the fist box covers y -2..+2); the model is y up with its big faces "
                "across z, and the converter turns it by a half turn about x into the weapon frame"}
PULL = [0.15, 0.32, 0.48]
ZSTEP = 0.25   # every other limb segment sits a quarter px across z so the flats of neighbours are never coplanar
FRAMES = ("rest", "pull_0", "pull_1", "pull_2")


def r4(v): return round(float(v) + 0.0, 4)


def rv(v): return [r4(a) for a in v]


class Thread(Cube):
    """The string: a box under a pixel thick from p0 to p1 in the x-y plane. Its faces read the shared string column of
    the sheet by hand (see project); the preview paints each face as one texel."""

    def __init__(self, name, p0, p1, w, hz):
        dx, dy = p1[0] - p0[0], p1[1] - p0[1]
        n = math.hypot(dx, dy)
        super().__init__(name, rv([p0[0] - w / 2, p0[1], -hz]), rv([p0[0] + w / 2, p0[1] + n, hz]), "linen", flat(STRING),
                         turn=r4(math.degrees(math.atan2(-dx, dy))), pivot=rv([p0[0], p0[1], 0]))
        self.w, self.hz, self.n = w, hz, r4(n)

    def size(self, face): return (1, 1)


# ---- painters ----

def tone(cols, over=None):
    """cols(face, w) -> the colours of a row, left to right as seen from outside; over(face, y, h, w) -> {x: colour}."""
    def paint(face, w, h):
        g = [list(cols(face, w)) for _ in range(h)]
        for y in range(h):
            for x, c in (over(face, y, h, w) if over else {}).items():
                g[y][x] = c
        return g
    return paint


def blend(a, b): return tuple((p + q) // 2 for p, q in zip(a, b))


def stave(B, K, i, upper):
    """A limb segment. B is the ramp of the back (+x, lit), K that of the belly (-x). East is the back and west the belly;
    on a two texel flat the near column is the grain line; a pale fleck rides segment i's second row from its outer end."""
    def cols(face, w):
        if face == "east": return [B[2], B[3]][-w:] if w <= 2 else [B[3]] * w
        if face == "west": return [K[1], K[2]][-w:] if w <= 2 else [K[2]] * w
        if face in ("north", "south"):
            c = [B[3], K[2]] if face == "north" else [K[2], B[3]]
            return c if w == 2 else [blend(B[2], K[3])] * w
        return [B[4] if face == "up" else K[0]] * w

    def over(face, y, h, w):
        d = (h - 1 - y) if upper else y   # texels from the stave's base
        if i % 2 == 0 and d == h - 2 and face in ("east", "west"): return {w - 1: B[4] if face == "east" else K[3]}
        return {}
    return tone(cols, over)


def horn(R, upper):
    """A horn tip: lighter toward the point, a pale cap."""
    def paint(face, w, h):
        g = []
        for y in range(h):
            d = (h - 1 - y) if upper else y
            c = R[3] if d == 0 else (R[4] if d < h - 1 else R[5])
            if face == "down": c = R[2]
            if face == "up": c = R[5]
            g.append([c] * w)
        return g
    return paint


def cap(upper):
    return by_face(up=flat(G[5]), down=flat(G[2]), rest=bar(G[5], G[4], G[3]))


def sun(face, w, h):
    """The Order's mark on a riser plate: a gilt cross with a bright heart on a dark gilt field."""
    g = [[G[1]] * w for _ in range(h)]
    for i in range(w): g[h // 2][i] = G[4]
    for j in range(h): g[j][w // 2] = G[4]
    g[h // 2][w // 2] = G[6]
    return g


def feather(face, w, h):
    """A white feather in profile: a grey shaft on the inside, the vane trimmed to a point."""
    shaft, vane, shade = FEATHER[1], FEATHER[3], FEATHER[2]
    g = []
    for y in range(h):
        row = [shaft, vane if y < h - 1 else None]
        if y == h - 2: row[1] = shade
        g.append(row if face == "south" else row[::-1])
    return g


# ---- the bows ----
# A segment: (length, angle degrees at rest, thick across x, deep across z, flex weight). The upper limb is built, the lower
# is its mirror. `nock` is the cube at the tip that carries the string; `bands` are cubes set across a joint.

class Spec:
    def __init__(self, wid, note, root, segs, wood, nock, grip, risers, extras=(), bands=(), draw=(3.2, 5.65)):
        self.wid, self.note, self.root, self.segs, self.wood = wid, note, root, segs, wood
        self.nock, self.grip, self.risers, self.extras, self.bands = nock, grip, risers, extras, bands
        self.draw = draw

    def limb(self, bend, s):
        """The cubes of one limb (s 'up' or 'lo') bent by `bend` x weight degrees, and its string point."""
        out = []
        x, y = 0.0, float(self.root)
        up = s == "up"
        place = (lambda P, n, t, d, th, z=0: (rv([P[0] - t / 2, P[1], z - d / 2]), rv([P[0] + t / 2, P[1] + n, z + d / 2]), r4(th), rv([P[0], P[1], 0]))) if up else \
                (lambda P, n, t, d, th, z=0: (rv([P[0] - t / 2, -P[1] - n, z - d / 2]), rv([P[0] + t / 2, -P[1], z + d / 2]), r4(-th), rv([P[0], -P[1], 0])))
        B, K = self.wood
        last = len(self.segs) - 1
        th = 0.0
        for i, (n, a, t, d, wt) in enumerate(self.segs):
            th = a + bend * wt
            lo, hi, turn, piv = place((x, y), n, t, d, th, ZSTEP * ((i + 1) % 2))
            skip = ("down", "up") if i < last else ("down",)
            out.append(Cube("seg%d_%s" % (i, s), lo, hi, "wood", by_face(**{f: stave(B, K, i, up) for f in FACES}), skip=skip, turn=turn, pivot=piv))
            for (bi, mat, mk) in self.bands:
                if bi == i:
                    px, py = x + 0.5 * math.sin(math.radians(th)), y - 0.5 * math.cos(math.radians(th))
                    bl, bh, bt, bp = place((px, py), 1, t + 1, d + 1, th)
                    out.append(Cube("band%d_%s" % (i, s), bl, bh, mat, mk, turn=bt, pivot=bp))
            x, y = x - n * math.sin(math.radians(th)), y + n * math.cos(math.radians(th))
        nk = self.nock
        nth = th + nk["rel"] + bend * nk["wt"]
        lo, hi, turn, piv = place((x, y), nk["n"], 1, nk["d"], nth, ZSTEP * ((len(self.segs) + 1) % 2))
        out.append(Cube("nock_%s" % s, lo, hi, nk["mat"], nk["mk"](up), skip=("down",), turn=turn, pivot=piv))
        dx, dy = -math.sin(math.radians(nth)), math.cos(math.radians(nth))
        at = nk["at"]
        point = (x + dx * at, y + dy * at)
        if nk.get("cap"):
            cm, ck = nk["cap"]
            ex, ey = x + dx * nk["n"], y + dy * nk["n"]
            lo, hi, turn, piv = place((ex, ey), 1, 1, nk["d"], nth, ZSTEP * (len(self.segs) % 2))
            out.append(Cube("cap_%s" % s, lo, hi, cm, ck, skip=("down",), turn=turn, pivot=piv))
        return out, point

    def attach_x(self, bend): return self.limb(bend, "up")[1][0]

    def bend_for(self, target):
        lo, hi = 0.0, 80.0
        for _ in range(60):
            mid = (lo + hi) / 2
            if self.attach_x(mid) > target: lo = mid
            else: hi = mid
        return (lo + hi) / 2

    def frames(self):
        """{frame: [cubes]} and the four string centres."""
        out, nocks = {}, []
        _, p0 = self.limb(0, "up")
        xs = r4(p0[0] - 0.1)
        # the drawn string reaches draw[1]; the nocks follow 85 percent of the way
        full = self.draw[1]
        steps = [xs - (xs + full) * (k + 1) / 3 if False else xs + (-full - xs) * (k + 1) / 3 for k in range(3)]
        for fi, name in enumerate(FRAMES):
            if fi == 0: bend = 0.0
            else: bend = self.bend_for(p0[0] + 0.85 * (steps[fi - 1] - xs))
            up, pu = self.limb(bend, "up")
            lo, pl = self.limb(bend, "lo")
            cubes = self.core() + up + lo
            if fi == 0:
                ya = pu[1]
                cubes.append(Thread("string", (pu[0], -ya), (pu[0], ya), 0.4, 0.15))
                nocks.append([r4(pu[0]), 0])
            else:
                d = (steps[fi - 1], 0.0)
                cubes.append(Thread("string_hi", d, pu, 0.5, 0.15))
                cubes.append(Thread("string_lo", d, (pl[0], -pu[1]), 0.5, 0.15))
                nocks.append([r4(d[0]), 0])
            out[name] = [self._named(c, name) for c in cubes]
        return out, nocks

    @staticmethod
    def _named(c, frame):
        c.name = frame + "/" + c.name
        return c

    def core(self):
        return [Cube(*a, **k) for a, k in self.grip + self.risers + self.extras]


def make(wid, note, **kw): return Spec(wid, note, **kw)


def grip_cubes(paint):
    return [(("grip", [-1, -2, -1], [1, 2, 1], "leather", paint), {"skip": ("up", "down")})]


def order_bow(tier):
    t3, t2 = tier == 3, tier >= 2
    wood = (WN, WN) if t2 else (DK, DK)
    segs = [(2, 5, 1, 2, 0.35), (2, 15, 1, 2, 0.65), (2, 27, 1, 2, 1.0), (2, 40, 1, 1, 1.2), (2, 10, 1, 1, 1.0)]
    if tier == 1:
        nock = {"n": 1, "d": 1, "rel": 0, "wt": 1.0, "mat": "wood", "mk": lambda up: by_face(up=flat(DK[4]), down=flat(DK[1]), rest=flat(DK[3])), "at": 0.5}
        grip = grip_cubes(wrap(L[3], L[1]))
        riser = wrap(L[2], L[1])
        risers = [(("riser_hi", [-1, 2, -1], [1, 3, 1], "leather", by_face(up=flat(L[2]), down=flat(L[1]), rest=riser)), {}),
                  (("riser_lo", [-1, -3, -1], [1, -2, 1], "leather", by_face(up=flat(L[2]), down=flat(L[1]), rest=riser)), {})]
        extras, bands = [], []
    else:
        steel = by_face(up=flat(S[5]), down=flat(S[1]), rest=bar(S[5], S[4], S[2], S[3]))
        gilt = by_face(up=flat(G[5]), down=flat(G[2]), rest=bar(G[5], G[4], G[2], G[3]))
        if t3:
            nock = {"n": 1, "d": 1, "rel": -6, "wt": 1.0, "mat": "bone", "mk": lambda up: horn(H, up), "at": 0.5, "cap": ("gilt", cap(True))}
            grip = grip_cubes(wrap(L[3], L[1], G[4]))
            risers = [(("riser_hi", [-1.5, 2, -1.5], [1.5, 5, 1.5], "gilt", by_face(north=sun, south=sun, east=sun, west=gilt, up=flat(G[5]), down=flat(G[1]))), {}),
                      (("riser_lo", [-1, -3, -1], [1, -2, 1], "gilt", gilt), {})]
            bands = [(2, "gilt", by_face(up=flat(G[5]), down=flat(G[2]), rest=bar(G[5], G[4], G[3])))]
        else:
            nock = {"n": 2, "d": 1, "rel": -6, "wt": 1.0, "mat": "bone", "mk": lambda up: horn(H, up), "at": 0.8}
            grip = grip_cubes(wrap(L[3], L[1]))
            risers = [(("riser_hi", [-1, 2, -1], [1, 3, 1], "steel", steel), {}), (("riser_lo", [-1, -3, -1], [1, -2, 1], "steel", steel), {})]
            bands = []
        extras = []
    names = {1: "I (plain)", 2: "II (upgraded)", 3: "III (elite)"}
    wid = "order_bow" + ("" if tier == 1 else "_t%d" % tier)
    return make(wid, NOTES[wid], root=3, segs=segs, wood=wood, nock=nock, grip=grip, risers=risers, extras=extras, bands=bands, draw=(3.2, 5.65))


def longbow(tier):
    t3, t2 = tier == 3, tier >= 2
    wood = (SAP, HEART)
    segs = [(3, 2, 2, 2, 0.5), (3, 6, 2, 2, 0.8), (3, 11, 1, 2, 1.0), (3, 17, 1, 1, 1.0), (2, 25, 1, 1, 1.0)]
    wid = "longbow" + ("" if tier == 1 else "_t%d" % tier)
    steel = by_face(up=flat(S[5]), down=flat(S[1]), rest=bar(S[5], S[4], S[2], S[3]))
    gilt = by_face(up=flat(G[5]), down=flat(G[2]), rest=bar(G[5], G[4], G[2], G[3]))
    bands, extras = [], []
    if tier == 1:
        nock = {"n": 1, "d": 1, "rel": 0, "wt": 1.0, "mat": "wood", "mk": lambda up: by_face(up=flat(HEART[4]), down=flat(HEART[1]), rest=flat(HEART[3])), "at": 0.5}
        grip = grip_cubes(wrap(L[3], L[1]))
        rw = wrap(L[2], L[1])
        risers = [(("riser_hi", [-1, 2, -1], [1, 3, 1], "leather", by_face(up=flat(L[2]), down=flat(L[1]), rest=rw)), {}),
                  (("riser_lo", [-1, -3, -1], [1, -2, 1], "leather", by_face(up=flat(L[2]), down=flat(L[1]), rest=rw)), {})]
    else:
        nock = {"n": 2, "d": 1, "rel": 0, "wt": 1.0, "mat": "bone", "mk": lambda up: horn(H, up), "at": 0.8}
        if t3: nock = {"n": 1, "d": 1, "rel": 0, "wt": 1.0, "mat": "bone", "mk": lambda up: horn(H, up), "at": 0.5, "cap": ("gilt", cap(True))}
        grip = grip_cubes(wrap(L[3], L[1], G[4]) if t3 else wrap(L[3], L[1]))
        riser_mat, rp = ("gilt", gilt) if t3 else ("steel", steel)
        risers = [(("riser_hi", [-1, 2, -1], [1, 3, 1], riser_mat, rp), {}), (("riser_lo", [-1, -3, -1], [1, -2, 1], riser_mat, rp), {})]
        if t3:
            band = by_face(up=flat(G[5]), down=flat(G[2]), rest=bar(G[5], G[4], G[3]))
            bands = [(1, "gilt", band), (3, "gilt", band)]
            extras = [(("clasp", [1, 0, -0.5], [2, 1, 0.5], "gilt", gilt), {}),
                      (("feather", [1, -5, -0.25], [3, 0, 0.75], "bone", by_face(north=feather, south=feather, up=flat(H[5]), down=flat(H[3]), east=flat(H[4]), west=flat(H[4]))), {"turn": 14, "pivot": [1.5, 0, 0]})]
    return make(wid, NOTES[wid], root=3, segs=segs, wood=wood, nock=nock, grip=grip, risers=risers, extras=extras, bands=bands, draw=(3.4, 5.65))


NOTES = {}
TIERS = {}
for _w, _f, _t in (("order_bow", order_bow, 1), ("order_bow_t2", order_bow, 2), ("order_bow_t3", order_bow, 3),
                   ("longbow", longbow, 1), ("longbow_t2", longbow, 2), ("longbow_t3", longbow, 3)):
    NOTES[_w] = _w
    TIERS[_w] = (lambda f=_f, t=_t: f(t), _w)


# ---- project and outputs ----

def project(spec, frames):
    """The Blockbench project: one group a frame; each frame's solid cubes read the rest frame's patches, its strings the
    shared string column."""
    wid = spec.wid
    solid = [c for c in frames["rest"] if not isinstance(c, Thread)]
    longest = max(c.n for cs in frames.values() for c in cs if isinstance(c, Thread))
    col = Cube("stringcol", [0, 0, 0], [1, int(math.ceil(longest)), 1], "linen", flat(STRING), skip=[f for f in FACES if f != "north"])
    colour, key, uv = W.pack([Cube(c.name.split("/")[1], c.lo, c.hi, c.mat, c.paint, c.skip, c.turn, c.pivot) for c in solid] + [col])
    ux, uy = uv[("stringcol", "north")][:2]
    elements, groups, outliner = [], [], []
    for frame in FRAMES:
        gid = W.uid(wid, frame)
        ids = []
        for c in frames[frame]:
            base = c.name.split("/")[1]
            e = {"name": c.name, "box_uv": False, "render_order": "default", "locked": False, "export": True,
                 "from": c.lo, "to": c.hi, "autouv": 0, "color": 0, "origin": c.pivot, "type": "cube", "uuid": W.uid(wid, c.name)}
            if frame != "rest": e["visibility"] = False
            if c.turn: e["rotation"] = [0, 0, c.turn]
            if isinstance(c, Thread):
                dims = {"north": (c.w, c.n), "south": (c.w, c.n), "east": (2 * c.hz, c.n), "west": (2 * c.hz, c.n),
                        "up": (c.w, 2 * c.hz), "down": (c.w, 2 * c.hz)}
                e["faces"] = {f: {"uv": [ux, uy, r4(ux + dims[f][0]), r4(uy + dims[f][1])], "texture": 0} for f in FACES}
            else:
                e["faces"] = {}
                for f in FACES:
                    if f in c.skip: continue
                    got = uv[(base, f)]
                    assert (got[2] - got[0], got[3] - got[1]) == c.size(f), (c.name, f)
                    e["faces"][f] = {"uv": got, "texture": 0}
            elements.append(e); ids.append(e["uuid"])
        groups.append({"name": frame, "uuid": gid, "export": True, "visibility": frame == "rest", "origin": [0, 0, 0], "rotation": [0, 0, 0], "children": []})
        outliner.append({"uuid": gid, "isOpen": False, "children": ids})
    proj = W.project(wid, [], colour, uv)
    proj["elements"], proj["groups"], proj["outliner"] = elements, groups, outliner
    return proj, colour, key


def outputs(tiers=None):
    out = {}
    for wid, (make_, note) in (tiers or TIERS).items():
        spec = make_()
        frames, nocks = spec.frames()
        proj, colour, key = project(spec, frames)
        out[wid + ".bbmodel"] = proj
        out[wid + ".png"], out[wid + "_m.png"] = colour, key
        out[wid + ".json"] = {"weapon": wid, "density": 1, "thickness": 2, "grip": GRIP, "pull": PULL, "nocks": nocks, "note": spec.note}
    return out


# ---- preview ----

def preview(tiers, path):
    rows = []
    for wid, (make_, _) in tiers.items():
        spec = make_()
        frames, _ = spec.frames()
        tiles = [W.render(frames[f], 0, 0, 8) for f in FRAMES] + [W.render(frames["rest"], 35, 15, 8), W.render(frames["rest"], 0, 0, 3)]
        H = max(t.height for t in tiles); Wd = sum(t.width for t in tiles) + 12 * (len(tiles) + 1)
        row = Image.new("RGBA", (Wd, H + 12), (40, 42, 50, 255)); x = 12
        for t in tiles: row.paste(t, (x, 6 + H - t.height)); x += t.width + 12
        rows.append(row)
    Wd = max(r.width for r in rows); sheet = Image.new("RGBA", (Wd, sum(r.height for r in rows)), (40, 42, 50, 255)); y = 0
    for r in rows: sheet.paste(r, (0, y)); y += r.height
    sheet.save(path); print("preview", path)


def main():
    if len(sys.argv) > 2 and sys.argv[1] == "preview": preview(TIERS, sys.argv[2])
    else:
        W.outputs = outputs   # install writes these files, not the swords'
        W.main(TIERS, __doc__)


if __name__ == "__main__":
    main()
