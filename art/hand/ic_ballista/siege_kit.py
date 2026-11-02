"""Ironclad siege kit for the Ballista Crew, the Mortar and Brassjaw, at one texel per model pixel.

Builds on art/hand/ic_breaker/ironclad.py (the House palette: gunmetal iron, ember, fur, oiled leather) and adds what
the siege troops need: seasoned oak, scorched brass, tarred rope, long-axis grain, hoops and the crewman's harness.
Light follows handlib: top front, the wearer's right, a bright top rim, a dark bottom seam.
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "ic_breaker"))
from ironclad import *   # noqa: F401,F403
from handlib import Troop

OAK = ramp("140d08", "2b1c0f", "46301a", "644826", "846236", "a58250")          # seasoned oak, plum shadows, sand lights
BRASS = ramp("1c1208", "3a2610", "5e3f16", "8a5e22", "b4863c", "d9b465", "f0dc9c")   # scorched brass, lights go pale
ROPE = ramp("1b150e", "372c1f", "58472f", "7d6746", "a08a64", "c0ae86")         # tarred hemp

def axes(face, x, y, w, h, along):
    """(across, pos) of texel (x, y) of a face for a box whose long axis is x, y or z: pos runs along it."""
    if along == "y":
        return (x, y) if face in ("front", "back", "left", "right") else (x, y)
    if along == "x":
        return (y, x)
    # z
    if face in ("left", "right"): return (y, x)
    return (x, y)

def grain(R=OAK, along="x", base=3, seed="g", knots=1):
    """Wood: lit and dark runs along the long axis, a knot or two placed by hand, never cross bands."""
    def paint(face, w, h):
        g = blank(w, h); r = rng(seed, face, w, h)
        runs = {}
        for y in range(h):
            for x in range(w):
                a, p = axes(face, x, y, w, h, along)
                key = a
                if key not in runs: runs[key] = r.choice((-1, 0, 0, 1))
                v = base + LIT[face] + runs[key]
                if face not in ("top", "bottom"):
                    v += lightx(face, x, w) if along != "x" else 0
                    if along == "x" and y == 0: v += 1
                    if along == "x" and y == h - 1 and h > 2: v -= 1
                g[y][x] = clamp(R, v)
        for _ in range(knots):
            x, y = r.randrange(w), r.randrange(h)
            if face not in ("top", "bottom") and w > 2 and h > 2:
                g[y][x] = clamp(R, base - 2)
                if x + 1 < w: g[y][x + 1] = clamp(R, base - 1)
        return g
    return paint

def hooped(inner, hoops, along="x", R=IRON, trim=None, base=3):
    """A beam or barrel: `inner` painter with hoops of iron (and a brass lip) at the listed positions along the long axis."""
    def paint(face, w, h):
        g = inner(face, w, h)
        for pos, ln in hoops:
            for y in range(h):
                for x in range(w):
                    a, p = axes(face, x, y, w, h, along)
                    if pos <= p < pos + ln:
                        t = 3 + LIT[face] + (1 if p == pos else 0) + (-1 if p == pos + ln - 1 and ln > 2 else 0)
                        g[y][x] = clamp(R, t)
                        if trim and p == pos + (ln // 2) and ln >= 3: g[y][x] = clamp(trim, 4 + LIT[face])
        return scuff(g, ("hoop", face, along, w, h), 2, R)
    return paint

def brass(base=4, spec=True, seed="b"):
    p = plate(BRASS, base, spec=False, seam=False)
    def paint(face, w, h):
        g = p(face, w, h)
        if face not in ("top", "bottom") and w > 1 and h > 1:
            g[0][0] = clamp(BRASS, base + 2)
            if spec and w > 2: g[h - 1][w - 1] = clamp(BRASS, base - 2)
        return scuff(g, (seed, face, w, h), max(1, w * h // 20), BRASS)
    return paint

def rope(along="x", base=3, seed="r"):
    """Twisted hemp: a diagonal lay along the long axis."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                a, p = axes(face, x, y, w, h, along)
                v = base + LIT[face] + (1 if (a + p) % 3 == 0 else (-1 if (a + p) % 3 == 2 else 0))
                g[y][x] = clamp(ROPE, v)
        return g
    return paint

def iron_plain(base=3, seed="i", wear=3):
    p = plate(IRON, base, spec=False)
    def paint(face, w, h): return scuff(p(face, w, h), (seed, face, w, h), wear, IRON)
    return paint

def ember_core(R=EMBER, hot=5):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(R, hot - (1 if (x + y) % 2 else 0) + (1 if face == "top" else 0) + LIT[face] // 2)
        return g
    return paint

# ---- the crewman's harness, shared by the Ballista Crew and the Mortar ----
CAP_FRONT = """
344444443
3444544443|brow
q rrrrrr q
"""

def cap_painter(goggles=False):
    """The iron cap: a bowl of lames with an ember brow band; the front is open from the brow down with cheek guards."""
    base = lames([3, 2, 2], base=3, seed="cap", wear=3, rivets=False)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "bottom": return blank(w, h)
        if face in ("front", "right", "left", "back"):
            for x in range(w): g[2][x] = EMBER[2 if x % 2 else 3]            # the brow band, round the whole cap
        if face == "front":
            for y in range(3, h):
                for x in range(2, w - 2): g[y][x] = None                     # the face opening
            for y in range(3, h):
                for x in (0, 1, w - 2, w - 1): g[y][x] = clamp(IRON, 3 - (1 if x >= w - 2 else 0) + (1 if x == 0 else 0))
            for x in range(2, w - 2): g[3][x] = IRON[1]                      # the shadow under the brow
        if face == "back":
            for y in range(1, h): g[y][w // 2] = IRON[2]
        return g
    return paint

def coif():
    return mail_ic(3)

def crew_body_painter(seed="cb"):
    return rows([(0, lames([3, 3, 3], base=3, seed=seed, wear=6, rivets=True)), (9, leather_ic(base=2, stitch=True))])

def crew_belt():
    def paint(face, w, h):
        g = leather_ic(base=1)(face, w, h)
        if face == "front":
            for y in range(h):
                for x in (3, 4): g[y][x] = BRASS[4 if y == 0 else 2]
            g[0][3] = g[0][4] = EMBER[4]
        return g
    return paint

def crew_arm():
    return rows([(0, mail_ic(3)), (3, lames([3, 3, 2], base=3, seed="carm", wear=3)), (9, leather_ic(base=2, stitch=True))])

def crew_leg():
    return rows([(0, mail_ic(2)), (4, lames([2, 2, 2], base=2, seed="cleg", wear=3, rivets=False)), (8, leather_ic(base=1))])

def spaulder_lame(i):
    def paint(face, w, h):
        g = lames([h], base=3 + (1 if i == 0 else 0), seed=("sp", i), wear=2, rivets=(w >= 6))(face, w, h)
        if i == 1 and face not in ("top", "bottom"):
            for x in range(w): g[h - 1][x] = EMBER[2 if x % 2 else 3]
        return g
    return paint

COG = """
..L..
.LmL.
LmdmL
.LmL.
..L.."""

def crew_tabard(T):
    """Team tabard from collar to belt, the House cog in brass on the chest, an ember hem; open at the arms."""
    pcog = {"L": BRASS[5], "m": BRASS[3], "d": IRON[0]}
    def paint(face, w, h):
        g = blank(w, h)
        if face == "bottom": return g
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if face in ("front", "back") and x in (0, w - 1): v -= 1
                if y == 0 and face != "top": v += 1
                g[y][x] = clamp(T, v)
        if face in ("right", "left"):
            for y in range(0, 7):
                for x in range(w): g[y][x] = None
        if face in ("front", "back"):
            g[h - 1] = [EMBER[3] if x % 2 else EMBER[2] for x in range(w)]
            g[h - 2] = [EMBER[4]] * w
            if face == "front": over(g, grid(COG, pcog), 1, 3)
        return g
    return paint

def crew_flap(T, inner_x):
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(T, 3 + LIT[face] + (-1 if x == inner_x else 0) + (-1 if y >= h - 3 else 0))
        g[h - 1] = [EMBER[2]] * w
        g[h - 2] = [EMBER[4] if x % 2 else EMBER[3] for x in range(w)]
        return g
    return paint

def crew_bones(plume=False, goggles=False):
    """Root, body, a head with four faces, arms, legs, cap, coif, belt, tabard, spaulders, boots and skirts."""
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), crew_body_painter())]))
    B.append(Bone("head", "body", (0, -12, 0)))
    for k in range(4):
        B.append(Bone(f"face_v{k}", "head", boxes=[Box((-4, -8, -4), (8, 8, 8), face_painter(k))], variants=[k]))
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), crew_arm())]))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), crew_arm())]))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), crew_leg())]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), crew_leg())]))
    B.append(Bone("cap", "head", tag="helm", boxes=[
        Box((-4.5, -9, -4.5), (9, 5, 9), cap_painter()),
        Box((-3.5, -10, -3.5), (7, 1, 7), iron_plain(4, "capcrown", 2)),
        Box((-0.5, -5, -5), (1, 3, 1), iron_plain(4, "nasal", 1))]))
    B.append(Bone("coif", "head", tag="helm", boxes=[Box((-4.5, -5, 1), (9, 6, 3.5), coif())]))
    if plume:
        B.append(Bone("plume", "head", tag="helm", boxes=[Box((-0.5, -12, -4), (1, 3, 9), ember_plume("pl"))]))
    B.append(Bone("belt", "body", boxes=[Box((-4, -3, -2), (8, 2, 4), crew_belt(), dil=0.45)]))
    B.append(Bone("tabard", "body", team=True, boxes=[Box((-4, -12, -2), (8, 12, 4), "tabard", dil=0.3, sheet="cloth")]))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"{side}_spaulder", f"{side}_arm", (c, -2.5, 0), rot=(0, 0, 12 * c), boxes=[
            Box((-4, -1.5, -4), (8, 2, 8), spaulder_lame(0)),
            Box((-4 + 0.5 * c, 0.5, -3.5), (8, 2, 7), spaulder_lame(1), dil=0.15),
            Box((-3.5 + 1.0 * c, 2.5, -3), (7, 2, 6), spaulder_lame(2), dil=0.15)]))
        B.append(Bone(f"{side}_cuff", f"{side}_arm", boxes=[Box((c - 2.5, 6, -2.5), (5, 2, 5), lames([2], base=3, rivets=False, wear=1))]))
        B.append(Bone(f"{side}_boot", f"{side}_leg", boxes=[Box((-2.5, 9, -3), (5, 3, 5), rows([(0, leather_ic(base=2)), (1, leather_ic(base=1, stitch=True)), (2, lames([1], base=2, rivets=False, wear=0))]))]))
        inner = 3 if side == "right" else 0
        B.append(Bone(f"{side}_skirt", f"{side}_leg", team=True, boxes=[
            Box((-2, 0, -2.45), (4, 7, 0), ("flap", inner), sheet="cloth"),
            Box((-2, 0, 2.45), (4, 7, 0), ("flap", 3 - inner), sheet="cloth")]))
    return B

def crew_cloth(spec, side):
    T, M = TEAM_IC[side]
    if spec == "tabard": return crew_tabard(T)
    return crew_flap(T, spec[1])
