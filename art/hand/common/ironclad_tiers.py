"""Ironclad shared tier trims (ART-STYLE-GUIDE 10.3, the Hearthguard ladder): tier II clean iron with brass rivets and brass
rims on the signature plate; tier III brass rims on all plate, brass edging, one horned or crested part, and the ember kept as
a burning line (the House accent art_hue_check wants on every skin).
Import: sys.path.insert(0, <art/hand/common>); import ironclad_tiers as IT.
"""
import random
from handlib import ramp, blank, clamp, LIT, lightx, per_tier

IRON  = ramp("0d0e14", "1a1c25", "2c303b", "444a58", "62697a", "8a92a2", "bcc3cf")
EMBER = ramp("1f0a06", "4a1608", "7a2a0c", "993f12", "c8966f", "ecc9a6")
BRASS = ramp("3e2b12", "664a20", "85642a", "997530", "c8a870")          # guide 10.3
HORN  = ramp("2e2820", "574d3e", "857862", "b3a68a", "d9cfb4")           # tier III bone horns
SIDES = ("front", "back", "left", "right")

def _r(*k): return random.Random(repr(k))
def _lit(face, x, w, base): return base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)

def brass_up(p, tier, top=True, foot=False, rivets=True, every=False):
    """Wrap a tier I painter: brass rivets (lone bright iron texels) at tier II and up; a brass top rim (top) and foot rim
    (foot) on the side faces; at tier III both rims always (every plate is rimmed when every=True)."""
    if tier < 2: return p
    def paint(face, w, h):
        g = [list(r) for r in p(face, w, h)]
        if face in ("top", "bottom"): return g   # brass only on the side faces: the tops stay blackened iron
        if rivets:
            for y in range(h):
                for x in range(w):
                    c = g[y][x]
                    lone = c in (IRON[5], IRON[6]) and (x == 0 or g[y][x - 1] != c) and (x == w - 1 or g[y][x + 1] != c)
                    below = g[y + 1][x] if y + 1 < h else None
                    if lone and below in IRON and IRON.index(below) <= IRON.index(c) - 2:   # rivet(): a head over a dark shadow
                        g[y][x] = BRASS[4]; g[y + 1][x] = BRASS[1]
        rim_t = top or (tier == 3 and every)
        rim_f = (foot and h >= 3) or (tier == 3 and every and h >= 3)
        for x in range(w):
            if rim_t and g[0][x] is not None: g[0][x] = BRASS[3 + (LIT[face] > 0)] if (x % 3) else BRASS[4]
            if rim_f and g[h - 1][x] is not None: g[h - 1][x] = BRASS[2 if x % 2 else 1]
        return g
    return paint

def T(p, **kw):
    """The painter dict for a tier I plate: I as drawn, II and III brass-trimmed."""
    return {1: p, 2: brass_up(p, 2, **kw), 3: brass_up(p, 3, **kw)}

def plate_t(tier, seed="ip", base=3, ember=False):
    """New tier plate: clean riveted iron with a lit hammered top, a brass foot rim (II) or brass rims top and foot (III);
    ember=True runs a burning ember line just above the foot rim (tier III)."""
    def paint(face, w, h):
        r = _r(seed, tier, face, w, h); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = _lit(face, x, w, base) + (1 if y == 0 else 0) + r.choice((-1, 0, 0, 0, 0, 1))
                g[y][x] = clamp(IRON, v)
        if face in ("top", "bottom"): return g
        if h >= 2:
            for x in range(w): g[h - 1][x] = BRASS[2 if x % 2 else 1]
        if tier == 3:
            for x in range(w): g[0][x] = BRASS[4 if x % 3 == 0 else 3]
            if ember and h >= 4:
                for x in range(w): g[h - 2][x] = EMBER[3 if x % 2 else 2]
        if face in ("front", "back") and w >= 4 and h >= 3:
            g[1][1] = g[1][w - 2] = BRASS[4]
        return g
    return paint

def P(seed, base=3, ember=False):
    return per_tier(lambda t: plate_t(t, seed, base, ember), (2, 3))

def brass(base=3, seed="br"):
    """Cast brass: lit top, shaded foot, a glint."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(BRASS, _lit(face, x, w, base) + (1 if y == 0 and face in SIDES else 0) - (1 if y == h - 1 and h > 2 else 0))
        if face in ("front", "right") and w > 1 and h > 1: g[0][0] = BRASS[4]
        return g
    return paint

def spike(R=IRON, base=3, tip=2):
    """A spike, horn or crown point: dark at the root (the last row), pale at the tip (the first row)."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                t = ((h - 1 - y) * (tip + 1)) // max(1, h)
                g[y][x] = clamp(R, _lit(face, x, w, base) - t + (1 if face == "top" else 0))
        if face in SIDES and h > 1: g[0] = [clamp(R, base + tip)] * w
        return g
    return paint

def horn(): return spike(HORN, 2, 2)

def ember_line(R=IRON, base=2):
    """A dark iron strap with a burning ember seam down its middle row: glow-like, never bright orange."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = clamp(R, _lit(face, x, w, base))
        if face in SIDES:
            m = h // 2
            for x in range(w): g[m][x] = EMBER[3 if x % 2 else 2]
        return g
    return paint

def crew_tiers(B, seed, bevor=True, comb=True):
    """Tier II/III armour for the siege-kit crew (ballista and mortar crews). Tier II: a brass cap band, a fourth spaulder
    lame with a brass boss, bracers, knee plates and (bevor=True) a riveted bevor. Tier III: spaulder spikes, greaves and
    (comb=True) an ember-seamed comb on the cap."""
    from handlib import Bone, Box
    B.append(Bone("cap_band", "head", tag="helm", stages=[2, 3], boxes=[Box((-4.9, -5.5, -4.9), (9.8, 1, 9.8), brass(3, seed + "cb"))]))
    if bevor:
        B.append(Bone("bevor", "head", tag="helm", stages=[2, 3], boxes=[Box((-4.4, -2, -4.7), (8.8, 3, 1), P(seed + "bv", 3), dil=0.05)]))
    if comb:
        B.append(Bone("comb", "head", tag="helm", stages=[3], boxes=[Box((-0.5, -11.5, -3.5), (1, 1.5, 7), ember_line(IRON, 3))]))
    for side, c in (("right", -1), ("left", 1)):
        sp = f"{side}_spaulder"
        B.append(Bone(f"{side}_lame4", sp, stages=[2, 3], boxes=[Box((-3 + 1.5 * c, 4.5, -2.5), (6, 1, 5), P(seed + "l4" + side, 3), dil=0.1)]))
        B.append(Bone(f"{side}_sboss", sp, stages=[2, 3], boxes=[Box((-1.5, -2.5, -1.5), (3, 1, 3), brass(3, seed + "sb" + side))]))
        B.append(Bone(f"{side}_sspikes", sp, stages=[3], boxes=[Box((-0.5, -4.5, dz - 0.5), (1, 2, 1), spike(IRON, 3, 2)) for dz in (-2.5, 2.5)]))
        B.append(Bone(f"{side}_bracer", f"{side}_arm", stages=[2, 3], boxes=[Box((c - 2.2, 3, -2.2), (4.4, 3, 4.4), P(seed + "br" + side, 3, ember=True), dil=0.05)]))
        B.append(Bone(f"{side}_kneeplate", f"{side}_leg", stages=[2, 3], boxes=[Box((-2.5, 4, -2.9), (5, 2.5, 1), P(seed + "kn" + side, 4), dil=0.05)]))
        B.append(Bone(f"{side}_greave", f"{side}_leg", stages=[3], boxes=[Box((-2.4, 6, -2.4), (4.8, 3, 4.8), P(seed + "gv" + side, 3), dil=0.05)]))
    return B
