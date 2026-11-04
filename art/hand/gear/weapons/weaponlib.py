"""The commander gear weapons (docs/superpowers/plans/2026-11-03-gear-art.md, Task 7), on the sword builder's frame.

A weapon is the build_swords Cube list: Blockbench, y up, the grip's centre at the origin (the fist covers y -2..+2),
one texel per model px (the 10-30 standard every shipped weapon follows), every frame one `rest` group. Each item
module defines weapon() -> Weapon, whose make(rarity, side) returns the cubes at that rarity in that side's cloth.

Rarity reads on the parts the fist does not cover (guard, pommel, collars, head, tips), in the gear's ladder:
Common plain worn metal; Uncommon a green enamel bead; Rare bright steel and blue enamel; Epic a lit purple gem;
Legendary gold fittings, a lit sun stone and a larger silhouette. Gear is house-neutral; banners take the side cloth.
Each step up also changes the build (a wrap, guard, plate, spike or fitting), so adjacent tiers part without their
borders: test_tiers_change_shape holds every weapon to it.

Held on the mannequin: a `blade` hold points the weapon forward from the right fist (edge down), a `pole` hold stands
it upright (string or flag trailing back), `shield` sits before the left forearm, `hip` hangs at the right hip. A
two-handed weapon names its second grip; the left arm reaches for it by a search over the arm's angles.

  python weaponlib.py sheet <png>     the contact sheet of every weapon (art/hand/gear/weapons/*.py)
"""
import importlib, math, os, sys
from PIL import Image, ImageDraw
HERE = os.path.dirname(os.path.abspath(__file__))
GEAR = os.path.join(HERE, "..")
for p in (os.path.join(GEAR, "..", "weapons"), GEAR, os.path.join(GEAR, "sets"), os.path.join(GEAR, "..", "common")):
    sys.path.insert(0, os.path.abspath(p))
import build_swords as W
from build_swords import Cube, by_face, flat
from handlib import ramp, clamp, STEEL, GOLD, LEATH, LINEN, TEAM, SIDES
from marchwarden import BRIGHT, PIPE, ENAMEL, GEM, SUN
import gearlib, render, mannequin

WORN = ramp("141418", "22232a", "34363f", "4a4d57", "626672", "80848f", "a2a6ae")   # Common: dull, pitted iron
W.KEY.update({"wood": (0x8B, 0x45, 0x13), "bone": (0xEE, 0xEE, 0xDD), "cloth": (0x30, 0x60, 0xC0), "hide": (0x66, 0x44, 0x22),
              "gem": (0x80, 0x00, 0xFF), "brass": (0xCC, 0x99, 0x33)})
LITW = {"up": 1, "south": 0, "east": 0, "north": 0, "west": -1, "down": -2}
LEGEND, EPIC, RARE, UNCOMMON, COMMON = 5, 4, 3, 2, 1
CAP = {1: 16, 2: 16, 3: 18, 4: 18, 5: 18}   # ART-STYLE-GUIDE 7.1, tiers I-II and III; a bow's string stays out
GILT = {tuple(c) for c in GOLD}
BRASS = ramp("2a1e0c", "4e3818", "765626", "9c7634", "be9648", "d8b468")   # haft bands: duller and browner than gold
YEW = ramp("2a140c", "4a2414", "6e3a1e", "92542c", "b2723e", "cc9358")
OAK = ramp("1e140c", "352416", "4e3620", "684a2c", "84603a", "9e784c")
ASHW = ramp("4a4234", "70664e", "988c6e", "bcb192", "dcd3b6", "f0ead4")   # pale ash wood

def metal_of(r):
    """The blade or head metal by rarity: worn iron, steel, then polished steel."""
    return WORN if r == COMMON else STEEL if r == UNCOMMON else BRIGHT

def fit_of(r):
    """The fittings (guard, pommel, collars, caps): the blade metal, gold at Legendary."""
    return GOLD if r == LEGEND else metal_of(r)

def accent(r):
    """(ramp, lit) of the rarity bead or stone, or None at Common."""
    return {UNCOMMON: (PIPE, False), RARE: (ENAMEL, False), EPIC: (GEM, True), LEGEND: (SUN, True)}.get(r)

# ---- painters (face, w, h) -> rows, top first as seen from outside ----
def tone(R, base=3, lead=True):
    """A forged or turned piece: lit by face, a brighter leading column on the broad faces."""
    def paint(face, w, h):
        v = base + LITW[face]
        return [[clamp(R, v + (1 if lead and x == 0 and w > 2 and face in ("south", "north") else 0)) for x in range(w)] for y in range(h)]
    return paint

def grain(R, base=2, salt=0, knot=None):
    """Wood: one grain line along the length, a darker fleck now and then, `knot` (y from the top) a dark ring."""
    def paint(face, w, h):
        g = []
        for y in range(h):
            row = []
            for x in range(w):
                v = base + LITW[face]
                k = (y * 5 + x * 3 + salt) % 9
                if face not in ("up", "down") and x == w // 2 and w > 1: v -= 1
                if k == 0: v -= 1
                if knot is not None and abs(y - knot) < 1: v -= 1
                row.append(clamp(R, v))
            g.append(row)
        return g
    return paint

def wound(R, base=2, rub=None):
    """A bias-wound grip: bands two tones apart; `rub` (y from the top) a rubbed-pale patch where the thumb sits."""
    def paint(face, w, h):
        return [[clamp(R, base + LITW[face] + (1 if (x + y) % 3 != 2 else -1) + (1 if rub is not None and abs(y - rub) < 1 and x == 0 else 0))
                 for x in range(w)] for y in range(h)]
    return paint

def bands(R, base=2, every=2, B=None, bbase=3):
    """Lashing or wire: rows alternating with a second ramp `B` (sinew on a limb, wire on a grip)."""
    B = B or R
    def paint(face, w, h):
        return [[clamp(B, bbase + LITW[face]) if y % every == 0 else clamp(R, base + LITW[face]) for x in range(w)] for y in range(h)]
    return paint

def blade(R, base=4, fuller=False, nicks=(), dark_edge=False):
    """A blade's flats: the bevel column bright, the core a step down, a fuller groove two down in the middle column;
    `nicks` (y from the top) step the outer edge texel dark. The edges and up face are the bright bevel."""
    def paint(face, w, h):
        if face in ("north", "south"):
            g = []
            for y in range(h):
                row = []
                for x in range(w):
                    xx = x if face == "south" else w - 1 - x
                    v = base if 0 < xx < w - 1 else base + 1
                    if fuller and xx == w // 2 and 1 < y < h - 2: v = base - 2
                    if dark_edge and xx == w - 1: v = base - 1
                    if xx == w - 1 and y in nicks: v = base - 3
                    row.append(clamp(R, v))
                g.append(row)
            return g
        return [[clamp(R, base + 1 + LITW[face]) for _ in range(w)] for _ in range(h)]
    return paint

def stone(R):
    """A set stone: a lit upper corner, a deep lower one."""
    def paint(face, w, h):
        g = [[R[1 + (face == "up")]] * w for _ in range(h)]
        if w > 1 and h > 1: g[0][0] = R[-1]; g[-1][-1] = R[0]
        elif face != "down": g[0][0] = R[2]
        return g
    return paint

def cloth(side, base=2, hem=None, cut=None, device=None):
    """Banner cloth in the side's colour: weave shading by column, a dark hem row, `cut(x, y, w, h)` -> True leaves the
    texel out (a swallow tail, a tatter), `device(x, y, w, h)` -> colour or None paints a sign on the broad faces."""
    R = TEAM[side][0]
    def paint(face, w, h):
        g = []
        for y in range(h):
            row = []
            for x in range(w):
                xx = x if face == "south" else w - 1 - x
                if cut and face in ("north", "south") and cut(xx, y, w, h): row.append(None); continue
                v = base + LITW[face] + (1 if (x // 2 + y) % 4 == 0 else 0)
                c = clamp(R, v)
                if hem and (y == h - 1 or y == 0): c = hem
                if device and face in ("north", "south"):
                    d = device(xx, y, w, h)
                    if d: c = d
                row.append(c)
            g.append(row)
        return g
    return paint

# ---- shared parts ----
def bead(name, lo, hi, r, turn=0, pivot=None):
    """The rarity bead or stone on a fitting: nothing at Common; lit at Epic and Legendary."""
    a = accent(r)
    if not a: return []
    R, lit = a
    return [Cube(name, lo, hi, "gem", stone(R), turn=turn, pivot=pivot, lit=W.FACES if lit else ())]

def chain(prefix, start, lengths, leans, widths, depth, mat, paint, mirror=False, overlap=0.05):
    """Staves joined end to end from `start` (x, y): each `lean` degrees from +y toward +x, each long enough to run past its
    joint by half its width times the sine of the turn plus 0.05 px (no notch, no seam). `mirror` runs it down -y."""
    x, y = start; out = []
    for i, (L, a, wd) in enumerate(zip(lengths, leans, widths)):
        t = math.radians(a); dx, dy = math.sin(t) * L, math.cos(t) * L
        cx, cy = x + dx / 2, y + dy / 2
        prev = leans[i - 1] if i else 0
        ext = wd / 2 * abs(math.sin(math.radians(a - prev))) + overlap
        lo, hi = [cx - wd / 2, cy - L / 2 - ext, -depth / 2], [cx + wd / 2, cy + L / 2 + (overlap if i < len(lengths) - 1 else 0), depth / 2]
        turn = -a
        if mirror:
            lo, hi = [lo[0], -hi[1], lo[2]], [hi[0], -lo[1], hi[2]]; cy = -cy; turn = a
        p = paint[i] if isinstance(paint, (list, tuple)) else paint
        m = mat[i] if isinstance(mat, (list, tuple)) else mat
        out.append(Cube("%s%d" % (prefix, i), lo, hi, m, p, turn=turn, pivot=[cx, cy, 0]))
        x, y = x + dx, y + dy
    chain.end = (x, y if not mirror else -y)
    return out

# ---- the weapon ----
class Weapon:
    """make(r, side) -> [Cube]; grips: the y of each fist on the weapon (0 first); hold: blade, pole, shield or hip."""
    def __init__(s, item_id, make, hold="blade", grips=(0,), note="", show_side=False):
        s.item_id, s.make, s.hold, s.grips, s.note, s.show_side = item_id, make, hold, tuple(grips), note, show_side
        s.hands = len(s.grips)

    def cubes(s, r, side="crimson"): return s.make(r, side)

    def counted(s, r):
        return [c for c in s.cubes(r) if not c.name.startswith("string")]

    def haft_of(s, y, r=COMMON):
        """The cubes the fist at weapon height y closes on: the y range covers the 4 px fist and x 0 lies inside them."""
        return [c for c in s.cubes(r) if not c.turn and c.lo[1] <= y - 2 + 1e-6 and c.hi[1] >= y + 2 - 1e-6 and c.lo[0] <= 0 <= c.hi[0]]

# ---- held on the mannequin ----
HOLDS = {   # parent bone, weapon point -> parent frame (y down, -z forward), offset in the parent frame
    "blade":  ("right_hand", lambda p: (p[2], -p[0], -p[1]), (0, 0, 0)),
    "pole":   ("right_hand", lambda p: (p[2], -p[1], p[0]), (0, 0, 0)),
    "shield": ("left_hand", lambda p: (-p[0], -p[1], p[2]), (0.5, -2, -2.65)),
    "hip":    ("body", None, (-5.4, -0.5, 1.2)),
}
HIP = render.rot(0, 0, -22)
POSES = {"idle": {}, "attack": {"right_arm": (-30, 0, 0)}}   # BipedClips attack at TroopClips.PEAKS["attack"] (0.25 s)
TWO_HAND = {"idle": (-45, -22, 0), "attack": (-30, -22, 0)}    # a two-handed grip brings the fist in to the centre line
LIGHT = [0.35, -0.85, -0.45]

def hold_point(hold, p):
    parent, f, off = HOLDS[hold]
    q = f(p) if f else render.app(HIP, (-p[0], -p[1], p[2]))
    return [q[i] + off[i] for i in range(3)]

def frames(bones, pose):
    mats, offs = {}, {}
    for bn in bones:
        pm, po = mats.get(bn.parent, render.rot(0, 0, 0)), offs.get(bn.parent, [0, 0, 0])
        o = [po[i] + render.app(pm, bn.pivot)[i] for i in range(3)]
        mats[bn.name], offs[bn.name] = render.mul(pm, render.rot(*pose.get(bn.name, bn.rot))), o
    return mats, offs

def reach(w, pose):
    """The left arm's angles that bring the left hand nearest the weapon's second grip, by a coarse then a fine search."""
    bones = mannequin.bones(); parent = HOLDS[w.hold][0]
    target_local = hold_point(w.hold, (0, w.grips[1], 0))
    mats, offs = frames(bones, pose)
    T = [offs[parent][i] + render.app(mats[parent], target_local)[i] for i in range(3)]
    def err(a):
        m, o = frames(bones, dict(pose, left_arm=a)); h = o["left_hand"]
        return sum((h[i] - T[i]) ** 2 for i in range(3))
    best = min(((p, y, rl) for p in range(-150, 31, 10) for y in range(-90, 91, 15) for rl in range(-90, 91, 30)), key=err)
    best = min(((best[0] + dp, best[1] + dy, best[2] + dr) for dp in range(-8, 9, 2) for dy in range(-12, 13, 3) for dr in range(-24, 25, 6)), key=err)
    reach.miss = math.sqrt(err(best))
    return best

_POSED = {}
def posed(w, clip):
    """A two-handed weapon's pose: the right arm keeps its clip pitch within 30 degrees and turns in until the left hand
    can close on the second grip; the right arm with the smallest miss wins."""
    pose = dict(POSES[clip])
    if w.hands == 2:
        key = (w.item_id, clip)
        if key not in _POSED:
            p0, y0, r0 = TWO_HAND[clip]
            tries = []
            for dp in (-30, -15, 0, 15):
                for y in range(y0, -91, -8):
                    p = dict(pose, right_arm=(p0 + dp, y, r0)); left = reach(w, p)
                    tries.append((reach.miss + 0.02 * abs(y - y0) + 0.02 * abs(dp), reach.miss, p["right_arm"], left))
            _, miss, right, left = min(tries)
            _POSED[key] = (right, left, miss)
        right, left, reach.miss = _POSED[key]
        pose["right_arm"], pose["left_arm"] = right, left
    return pose

def weapon_quads(w, r, side, mats, parent):
    """(quads of 20 numbers in the parent frame, sheet) groups by face shade, so render.render's flat weapon light is
    replaced by the troop's own face light."""
    cubes = w.cubes(r, side)
    colour, _, uv = W.pack(cubes); glow = W.pack.glow
    if glow: colour = Image.alpha_composite(colour, glow)
    lit = {(c.name, f) for c in cubes for f in c.lit}
    groups = {}
    for c in cubes:
        R = W.rot(0, 0, c.turn)
        for f in W.FACES:
            if (c.name, f) not in uv: continue
            x0, y0, x1, y1 = uv[(c.name, f)]
            P = []
            for s, t in ((0, 0), (1, 0), (1, 1), (0, 1)):
                pick = W.CORNER[f](s, t)
                p = [c.lo[i] + (c.hi[i] - c.lo[i]) * pick[i] for i in range(3)]
                q = W.app(R, [p[i] - c.pivot[i] for i in range(3)])
                P.append(hold_point(w.hold, [q[i] + c.pivot[i] for i in range(3)]))
            n = W.app(R, W.NORMAL[f]); n0 = hold_point(w.hold, (0, 0, 0)); n1 = hold_point(w.hold, n)
            nw = render.app(mats[parent], [n1[i] - n0[i] for i in range(3)])
            shade = 1.0 if (c.name, f) in lit else 0.62 + 0.38 * max(0.0, sum(nw[i] * LIGHT[i] for i in range(3)))
            if f == "up" and nw[1] < -0.9: shade = max(shade, 0.98)
            k = round(shade / 0.85, 2)
            groups.setdefault(k, []).append([v for pt in P for v in pt] + [x0, y0, x1, y0, x1, y1, x0, y1])
    out = []
    for k, qs in groups.items():
        img = colour.copy(); px = img.load()
        for yy in range(img.height):
            for xx in range(img.width):
                c = px[xx, yy]
                if c[3]: px[xx, yy] = tuple(min(255, int(c[i] * k)) for i in range(3)) + (c[3],)
        out.append((parent, (0, 0, 0), (0, 0, 0), qs, img))
    return out

def held(w, r, side, clip, yaw=-35, pitch=12, px=4):
    bones = mannequin.bones(); pose = posed(w, clip)
    for bn in bones:
        if bn.name in pose: bn.rot = list(pose[bn.name])
    mats, _ = frames(bones, {})
    parent = HOLDS[w.hold][0]
    sheets = {"skin": mannequin.skin(), "cloth": mannequin.skin()}
    return render.render(bones, lambda b: True, sheets, yaw, pitch, px=px, extra=weapon_quads(w, r, side, mats, parent))

def bare(w, r, side, px=5):
    cubes = w.cubes(r, side)
    img = W.render(cubes, 35, 15, px=px)
    return img

# ---- in-game export ----
ARM = {"right_hand": ("right_arm", (-1, 8, 0)), "left_hand": ("left_arm", (1, 8, 0)), "body": ("body", (0, 12, 0))}   # the mannequin frame -> the vanilla biped part's

def model_quads(w, cubes, uv):
    """Every face as a 20-number quad (four corners in the vanilla biped part's frame, model px with y down, then the texel
    of each corner): weapon_quads' corners taken the other way round, so (corner 1 - corner 0) x (corner 3 - corner 0)
    points out of the cube as TroopMesh lights it."""
    part, off = ARM[HOLDS[w.hold][0]]
    out = []
    for c in cubes:
        R = W.rot(0, 0, c.turn)
        for f in W.FACES:
            if (c.name, f) not in uv: continue
            x0, y0, x1, y1 = uv[(c.name, f)]; P = []
            for s_, t_ in ((0, 0), (0, 1), (1, 1), (1, 0)):
                pick = W.CORNER[f](s_, t_)
                p = [c.lo[i] + (c.hi[i] - c.lo[i]) * pick[i] for i in range(3)]
                q = W.app(R, [p[i] - c.pivot[i] for i in range(3)])
                h = hold_point(w.hold, [q[i] + c.pivot[i] for i in range(3)])
                P += [round(h[i] + off[i], 4) for i in range(3)]
            out.append(P + [x0, y0, x0, y1, x1, y1, x1, y0])
    return part, out

def outputs(w):
    """{path under the assets: PIL image or JSON} for one weapon: models/gear/weapon/<id>.json and, per rarity, its sheet
    textures/entity/gear/weapon/<id>/<id>_r<n>.png (a banner's per side, _r<n>_<side>.png) and the lit sheet _e<n>.png. The
    model holds each rarity's quads in the vanilla biped part's frame, and a two-handed weapon its idle and attack arm angles."""
    d = "textures/entity/gear/weapon/%s/" % w.item_id; ns = "gravesandglory:"
    out = {}; rar = {}
    for r in gearlib.RARITIES:
        cubes = w.cubes(r)
        colour, _, uv = W.pack(cubes); glow = W.pack.glow
        part, quads = model_quads(w, cubes, uv)
        row = {"sheet": list(colour.size), "quads": quads}
        if w.show_side:
            for side in SIDES:
                sc, _, suv = W.pack(w.cubes(r, side)); assert suv == uv and sc.size == colour.size, (w.item_id, r, side)
                out[d + "%s_r%d_%s.png" % (w.item_id, r, side)] = sc
            row["texture"] = ns + d + "%s_r%d_" % (w.item_id, r)
        else:
            out[d + "%s_r%d.png" % (w.item_id, r)] = colour; row["texture"] = ns + d + "%s_r%d.png" % (w.item_id, r)
        if glow:
            out[d + "%s_e%d.png" % (w.item_id, r)] = glow; row["glow"] = ns + d + "%s_e%d.png" % (w.item_id, r)
        rar[str(r)] = row
    j = {"format": 1, "item": w.item_id, "hold": w.hold, "parent": part, "hands": w.hands, "side": w.show_side, "rarities": rar}
    if w.hands == 2:
        j["pose"] = {clip: {b: list(posed(w, clip)[b]) for b in ("right_arm", "left_arm")} for clip in POSES}
    out["models/gear/weapon/%s.json" % w.item_id] = j
    return out

# ---- the contact sheet ----
def items():
    out = []
    for f in sorted(os.listdir(HERE)):
        if f.endswith(".py") and f != "weaponlib.py" and not f.startswith("_"):
            out.append(importlib.import_module(f[:-3]).weapon())
    order = [i["id"] for i in gearlib.ids()["items"] if i["slot"] in ("main", "off")]
    return sorted(out, key=lambda w: order.index(w.item_id) if w.item_id in order else 99)

def row(w, side="crimson"):
    names = {i["id"]: i["name"] for i in gearlib.ids()["items"]}
    tiles = []
    for r in gearlib.RARITIES:
        a, b, c = bare(w, r, side), held(w, r, side, "idle"), held(w, r, side, "attack", yaw=-70, pitch=8)
        H = max(t.height for t in (a, b, c)); blk = Image.new("RGBA", (a.width + b.width + c.width + 16, H + 18), gearlib.BG)
        x = 0
        for t in (a, b, c): blk.paste(t, (x, H - t.height)); x += t.width + 8
        d = ImageDraw.Draw(blk); d.rectangle([0, H + 6, blk.width - 8, H + 9], fill=gearlib.hx(gearlib.BORDERS[r]))
        d.text((2, H + 8), "%s  %d cubes" % (gearlib.RARITY_NAMES[r - 1], len(w.counted(r))), fill=(200, 196, 186, 255))
        tiles.append(blk)
    img = render.sheet_of(tiles)
    return gearlib.label(img, "%s / %s" % (names.get(w.item_id, w.item_id), side), gearlib.BORDERS[5])

def sheet(out):
    ws = items(); rows_ = [row(w) for w in ws]
    rows_ += [row(w, "teal") for w in ws if w.show_side]
    img = gearlib.stack(rows_); os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True); img.save(out)
    print("weapons sheet", out, img.size)

if __name__ == "__main__":
    if len(sys.argv) > 2 and sys.argv[1] == "sheet": sheet(sys.argv[2])
    else: print(__doc__)
