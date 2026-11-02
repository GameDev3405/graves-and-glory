"""The Wildmoot kit for hand-authored troops at one texel per model pixel (ART-STYLE-GUIDE 10.4).

Fur, hide, boiled leather, bone and tusk, clay warpaint; ochre is the House accent (10.1) and sits on the skin sheet as
stitching, bindings and toggles so the House reads ochre in every team colour. Team colour stays on the cloth sheet only.
Souls-like read: dark worn hides, bone trophies, a strong dark-to-light step between layers. No flags, pennants or
standards on any back (Kort, 2026-10-29); back gear such as quivers and totem staffs is fine.

Shared by build_wm_hunter, build_wm_seer, build_wm_hound, build_wm_stalker and build_wm_oldhorn. wm_tusk keeps its own
copies of these painters so its installed outputs never move.
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "ic_breaker"))
from handlib import *   # noqa: F401,F403
from handlib import blank, clamp, LIT, lightx, plate, leather, ramp, LEATH, INK, TEAM
from ironclad import scuff, rng, fur, shade   # noqa: F401

# ---- palettes (darkest first) ----
PIG = ramp("2c1612", "5e332a", "8c5a48", "b3806a", "d6a994")          # boar-kin flesh, as the Tusk
SNOUT = ramp("2a1a14", "3a2620", "5e4034", "7e5a4a", "9c786a")        # guide 10.4 snout ramp
HIDE = ramp("1b120c", "3a2619", "5c4029", "82603c", "a98258", "cfae80")
FUR = ramp("24170e", "43291a", "673f25", "885836", "a97650")          # russet coat (guide 10.4)
GREYFUR = ramp("2a2622", "4a443c", "6a6258", "7d7468", "9a9284", "bab3a4")  # the Seer's and Old Horn's grey (spec #7D7468, #8A8378)
BARK = ramp("16100b", "2c2017", "4a3626", "634a35", "7e6249")         # spec bark #4A3626
BONE = ramp("33291f", "675b47", "9a8d71", "c9bd9f", "e6dfc8", "f7f3e4")
HORN = ramp("0f0b09", "231915", "3b2c24", "58443a", "7a6252")
WOOD = ramp("1a120c", "36241a", "563a26", "7a5436", "9c7248")
OCHRE = ramp("3e2208", "683e14", "8e5c20", "b07c36", "cc9c5e")        # House accent (10.1)
CLAY = ramp("4a4030", "7a6c52", "a8987a", "c8b48a", "e2d4b0")         # pale clay warpaint (spec #C8B48A)
CHITIN = ramp("0c0e0b", "161a13", "23291f", "343c2e", "4a5440", "64705a")  # spec chitin #23291F
SILK = ramp("6e6a60", "9a958a", "c4c0b4", "e2dfd6", "f6f4ee")
BLOOD = ramp("2a0e0b", "4a1a15", "6e2a22", "8e3a2e")                  # dried blood (spec #6E2A22)
GLOWCLAY = ramp("8a6a30", "d8b060", "ffe0a0")                          # clay-glow eyes (the _e sheet)

def tone(R, base=3, seed=None, wear=2):
    """A flat plate painter of ramp R with a lit rim and dark seam, then seeded wear."""
    def paint(face, w, h):
        g = plate(R=R, base=base, spec=False)(face, w, h)
        return scuff(g, (seed, face, w, h), wear, R) if seed else g
    return paint

def flat(R, base=3, seed=None, wear=2):
    """Face-lit flat colour with the wearer's-right light falloff, no rim (flesh, chitin, smooth hide)."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(R, base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0))
        return scuff(g, (seed, face, w, h), wear, R) if seed else g
    return paint

def leath(base=2, stitch=False, seed="wl", ochre=False):
    """Boiled leather; ochre=True runs an ochre stitch line along the lower edge of the side faces."""
    def paint(face, w, h):
        g = leather(base=base, stitch=stitch)(face, w, h)
        g = scuff(g, (seed, face, w, h), max(1, w * h // 14), LEATH)
        if ochre and face not in ("top", "bottom") and h >= 3:
            for x in range(0, w, 2): g[h - 2][x] = OCHRE[3]
        return g
    return paint

def pelt(R=HIDE, seed="wm-pelt", hang=True):
    """A shaggy pelt (strands, ragged lower edge)."""
    return fur(R=R, seed=seed, hang=hang)

def coat(R=FUR, seed="wm-coat", stripe=None, stripe_faces=("top",), stripe_w=2):
    """A beast coat: broken strands, darker toward the belly; stripe=ramp paints a centre stripe on stripe_faces."""
    def paint(face, w, h):
        g = blank(w, h); r = rng(seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + r.choice((-1, 0, 0, 0, 1))
                if (x + y // 2) % 3 == 0: v -= 1
                if face not in ("top", "bottom") and y >= h - 2: v -= 1
                g[y][x] = clamp(R, v)
        if stripe and face in stripe_faces:
            c0 = (w - stripe_w) // 2
            for y in range(h):
                for x in range(c0, c0 + stripe_w): g[y][x] = clamp(stripe, 3 + (1 if (y % 3 == 0) else 0))
        return g
    return paint

def bone_paint(base=4, seed=None):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(BONE, base + LIT[face] + (1 if y == 0 else 0))
        if face not in ("top", "bottom") and h > 1:
            for x in range(w): g[h - 1][x] = clamp(BONE, base - 1 + LIT[face])
        return scuff(g, (seed, face, w, h), 1, BONE) if seed else g
    return paint

def antler(base=3, seed="ant"):
    """Antler: bone-brown with paler tips (the top rows and the top face)."""
    R = ramp("2e2418", "524230", "7a6648", "a08a66", "c8b48e", "e8dcc0")
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                tip = 1 if (y == 0 or face == "top") else 0
                g[y][x] = clamp(R, base + LIT[face] + tip + (1 if (x + y) % 4 == 0 else 0) - (1 if (x * 3 + y) % 5 == 0 else 0))
        return scuff(g, (seed, face, w, h), 2, R)
    return paint

def lashing(R=OCHRE, base=3):
    """Ochre-dyed sinew lashing: alternating bands, the House accent on the skin sheet."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(R, base + LIT[face] + (0 if (y + x // 2) % 2 == 0 else -1))
        return g
    return paint

def clay_stripes(g, rows_, R=CLAY, tone_=3):
    """Paint clay warpaint texels at (x, y) cells onto a painted face grid."""
    for x, y in rows_:
        if 0 <= y < len(g) and 0 <= x < len(g[0]) and g[y][x] is not None: g[y][x] = R[tone_]
    return g

# ---- team cloth painters (cloth sheet only) ----
def cloth_band(T, base=3, ragged=False, seed="wm-cloth", barry=False):
    """A team cloth face: a lit field, barry=True alternates two-row stripes; ragged=True frays the lower edge."""
    def paint(face, w, h):
        g = blank(w, h)
        if face in ("top", "bottom") and ragged: return g
        r = rng(seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = (base if not barry or (y // 2) % 2 == 0 else base - 2) + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                g[y][x] = clamp(T, v)
        if ragged and face not in ("top", "bottom"):
            for x in range(w):
                if r.random() < 0.4: g[h - 1][x] = None
        return g
    return paint
