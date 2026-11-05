"""Tidecallers shared tier trims (ART-STYLE-GUIDE 10.5): tier II sea-blue rims on bronze plate and more barnacles, tier III deep
sea-blue plate chased in warm bronze with pearl rivets, and the sea taking more each tier (barnacle, kelp, shell, coral).
Import: sys.path.insert(0, <art/hand/common>); import tidecallers as TC.

The House accent is SEA (sea blue, guide 10.1); tools/art_hue_check.py wants a share of it on every skin, so the tier III
plate is sea blue itself and the tier II plate carries a sea-blue foot rim. Coral belongs to the Trident Guard only.
"""
import random
from handlib import ramp, blank, clamp, LIT, lightx, Bone, Box, per_tier

SEA    = ramp("0a1426", "11264a", "1a3f6a", "2a5e8a", "5e88a6", "9db8c4")              # House accent (guide 10.1)
DEEP   = ramp("060c18", "0c1a33", "13294f", "1c3a68", "2a5384", "3f6f9e", "6f97ba")     # tier III abyssal plate
BRONZE = ramp("2a1608", "553014", "86521e", "b57a30", "d9a455", "f0cf8c")              # sea-worn bronze
WARM   = ramp("3a2010", "6a3e18", "9a6224", "c48a36", "e2b060", "f6dc9c")              # tier III warm bronze chasing
BARN   = ramp("2c3234", "52595a", "7e8582", "a9aea6", "cfd2c8")                        # barnacle crust
PEARL  = ramp("6a706c", "8e948f", "b4bab4", "d8dcd4", "eef1ec")                        # pearl (guide: #D8DCD4)
CORAL  = ramp("3f3230", "6f5d57", "a08d82", "cdbcad", "e8dccd", "faf4e8")              # bleached coral (the Trident Guard's)
KELP   = ramp("0d1a12", "16301e", "21452a", "2f5c38", "467a4c")                        # wet kelp

def _rng(*k): return random.Random(repr(k))
def _lit(face, x, w, base): return base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)

def tc_plate(tier, seed="tp", barn=0.06):
    """Tier II: bronze plate, a lit rolled edge, a sea-blue foot rim and barnacles. Tier III: deep sea-blue plate chased in warm
    bronze top and foot, pearl rivets and a diagonal glint."""
    R = BRONZE if tier == 2 else DEEP
    base = 2 if tier == 2 else 3
    def paint(face, w, h):
        r = _rng(seed, tier, face, w, h); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(R, _lit(face, x, w, base) + r.choice((-1, 0, 0, 0, 1)))
        if face in ("top", "bottom"):
            for x in range(w): g[0][x] = clamp(R, base + 2)
            return g
        for x in range(w): g[0][x] = clamp(R, base + 2 + LIT[face]) if tier == 2 else WARM[3 + (x % 2)]
        if h >= 3:
            for x in range(w): g[h - 1][x] = SEA[3 + (x % 2)] if tier == 2 else WARM[2 + (x % 2)]
        if face in ("front", "back") and w >= 4 and h >= 4:
            g[1][1] = g[1][w - 2] = BRONZE[5] if tier == 2 else PEARL[4]
            if tier == 3:
                for i in range(min(w, h) - 3): g[h - 3 - i][2 + i] = DEEP[6]
        for _ in range(int(w * h * barn * (1 if tier == 2 else 0.5))):
            g[1 + r.randrange(max(1, h - 2))][r.randrange(w)] = BARN[1 + r.randrange(4)]
        return g
    return paint

def tc_band(tier, seed="tb"):
    """A sea-blue binding: bronze beads (tier II) or pearls (tier III) set along its front."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(SEA, 3 + LIT[face] + (1 if (x + y) % 3 == 0 else 0) - (1 if y == h - 1 else 0))
        if face in ("front", "back", "left", "right"):
            for x in range(1, w, 3): g[h // 2][x] = BRONZE[4] if tier == 2 else PEARL[4]
        return g
    return paint

def tc_spike(R, base=3, tip=2):
    """A spine, tooth or prong: dark at the root, pale at the tip (the last row)."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                t = (y * (tip + 1)) // max(1, h)
                g[y][x] = clamp(R, _lit(face, x, w, base) - (tip - t))
        if face not in ("top", "bottom") and h > 1: g[h - 1] = [clamp(R, base + tip + 1)] * w
        return g
    return paint

def tc_barnacles(seed="tbn"):
    """A crust of barnacles: grey cones with dark mouths."""
    def paint(face, w, h):
        r = _rng(seed, face, w, h); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(BARN, 2 + LIT[face] + r.choice((-1, 0, 1)))
        for _ in range(max(1, w * h // 5)):
            x, y = r.randrange(w), r.randrange(h); g[y][x] = BARN[0]
        return g
    return paint

def tc_coral(seed="tco"):
    """A bleached coral sprig, pitted and ribbed."""
    def paint(face, w, h):
        r = _rng("tco", seed, face, w, h); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(CORAL, 3 + (face == "top") + LIT[face] // 2 + r.choice((-1, 0, 0, 1)) - ((x + y // 2) % 3 == 0))
        return g
    return paint

def tc_pearl():
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = clamp(PEARL, 3 + LIT[face] + (1 if x == 0 and y == 0 else 0))
        return g
    return paint

def P(seed, barn=0.06):
    """The tier II/III plate painter dict."""
    return per_tier(lambda t: tc_plate(t, seed, barn), (2, 3))

def band(seed="tb"):
    return per_tier(lambda t: tc_band(t, seed), (2, 3))

def limb_plates(B, arm_box, leg_box, arm_dx=0.0, legs=True, arms=True, tag="tc"):
    """Bracers and greaves on both sides at tiers II and III. arm_box/leg_box: (origin, size) for the right side, mirrored by
    arm_dx for the left arm (the existing bracer offset convention)."""
    for side, c in (("right", 0), ("left", 1)):
        if arms:
            (ox, oy, oz), sz = arm_box
            B.append(Bone(f"{side}_t_bracer", f"{side}_arm", stages=[2, 3], boxes=[Box((ox + c * arm_dx, oy, oz), sz, P(tag + "br" + side, 0.04), dil=0.1)]))
        if legs:
            o, sz = leg_box
            B.append(Bone(f"{side}_greave", f"{side}_leg", stages=[2, 3], boxes=[Box(o, sz, P(tag + "gv" + side), dil=0.1)]))
