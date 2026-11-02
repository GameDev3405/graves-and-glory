"""Ironclad kit for the hand-authored House troops (Shieldbreaker, Arbalist): ramps, fur, layered plate, brigandine,
rivets, ember trim and worn-metal scuffs, drawn at one texel per model pixel on top of art/hand/common/handlib.py.

Ironclad is blackened iron: a cool gunmetal ramp with a bright hammered rim, riveted lames, mail under plate, and
ember (a burnt orange) on shield, weapon, tabard bordure, trim and plume. Ember sits inside the team tint rule: it is
dark and saturated in its shadows and goes pale peach in its lights, so no texel is both bright and orange.
"""
import os, random, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import *   # noqa: F401,F403
import handlib as H

IRON = ramp("0d0e14", "1a1c25", "2c303b", "444a58", "62697a", "8a92a2", "bcc3cf")
EMBER = ramp("1f0a06", "4a1608", "7a2a0c", "993f12", "c8966f", "ecc9a6")   # tones 4 and 5 are pale: the tint rule
FUR = ramp("17130f", "2e2720", "4a4034", "6b5e4c", "8e7f68", "b3a68c")
OILED = ramp("120d0a", "2a1a10", "4a2f1b", "6a4528", "8a5f3a")           # worn leather, a shade darker than handlib's

def pal_ic(main=IRON, **extra):
    p = {str(i): c for i, c in enumerate(main)}
    p.update({ch: EMBER[i] for i, ch in enumerate("pqrstu")})
    p.update({ch: FUR[i] for i, ch in enumerate("vwxyzZ")})
    p.update({ch: OILED[i] for i, ch in enumerate("ABCDE")})
    p.update({"k": INK, "m": MAIL[1], "n": MAIL[3], "o": MAIL[4], "l": MAIL[2]})
    p.update(extra)
    return p

TEAM_IC = H.TEAM

def rng(*key): return random.Random(hash(key) & 0xFFFF if False else sum((i + 3) * ord(c) for i, c in enumerate("|".join(map(str, key)))))

def shade(c, R, step):
    return R[max(0, min(len(R) - 1, R.index(c) + step))] if c in R else c

def scuff(g, seed, n, R=IRON, skip_top=True):
    """Scatter n worn texels (a bright edge catch or a dark pit) by hand-ish seeded luck, never on a grid."""
    r = rng(seed); h = len(g); w = len(g[0]) if h else 0
    for _ in range(n):
        x, y = r.randrange(w), r.randrange(h)
        c = g[y][x]
        if c is None or c not in R: continue
        g[y][x] = shade(c, R, 1 if r.random() < 0.35 else -1)
    return g

def rivet(g, x, y, R=IRON, tone=5):
    if 0 <= y < len(g) and 0 <= x < len(g[0]) and g[y][x] is not None: g[y][x] = R[tone]
    if 0 <= y + 1 < len(g) and 0 <= x < len(g[0]) and g[y + 1][x] is not None and g[y + 1][x] in R: g[y + 1][x] = shade(g[y + 1][x], R, -2)

def lames(bands, R=IRON, base=3, seed="l", wear=3, rivets=True, ember_rows=(), keep_top=True):
    """Overlapping plates stacked down the side faces. bands: heights top to bottom. Each lame catches light on its top
    row, drops into a dark lip, carries a rivet at each end and takes wear. ember_rows: band indexes edged in ember."""
    def paint(face, w, h):
        if face in ("top", "bottom"): return plate(R=R, base=base, spec=False)(face, w, h)
        g = blank(w, h); y0 = 0
        for bi, bh in enumerate(bands):
            for dy in range(bh):
                y = y0 + dy
                if y >= h: break
                for x in range(w):
                    v = base + LIT[face] + lightx(face, x, w)
                    if dy == 0: v += 1
                    if dy == bh - 1 and bh > 2: v -= 2
                    elif dy == bh - 2 and bh > 3: v -= 1
                    if face == "front" and w >= 5 and x == w // 2 and dy not in (0, bh - 1): v += 1   # the ridge
                    g[y][x] = clamp(R, v)
                if bi in ember_rows and dy == bh - 1:
                    for x in range(w): g[y][x] = EMBER[2 if x % 2 else 3]
            if rivets and bh >= 3 and face in ("front", "back"):
                for x in ((1, w - 2) if w >= 5 else (0, w - 1)):
                    rivet(g, x, y0 + 1, R)
            y0 += bh
        return scuff(g, (seed, face), wear, R)
    return paint

def fur(R=FUR, hang=True, seed="f"):
    """A pelt: strands in broken runs, a ragged lower edge, a lit crown."""
    def paint(face, w, h):
        g = blank(w, h); r = rng(seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (1 if y == 0 else 0) + r.choice((-1, 0, 0, 1))
                if (x + y // 2) % 3 == 0: v -= 1
                g[y][x] = clamp(R, v)
        if face not in ("top", "bottom") and hang:
            for x in range(w):
                if r.random() < 0.45: g[h - 1][x] = None
                elif r.random() < 0.5: g[h - 1][x] = clamp(R, 1)
        return g
    return paint

def leather_ic(base=2, stitch=False, ember_stitch=False):
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if y == 0 and face not in ("top", "bottom"): v += 1
                if y == h - 1 and h > 2 and face not in ("top", "bottom"): v -= 1
                g[y][x] = clamp(OILED, v)
        if stitch and face not in ("top", "bottom") and h > 2:
            for x in range(1, w, 2): g[1][x] = EMBER[2] if ember_stitch else clamp(OILED, base + 2)
        return scuff(g, ("lea", face, w, h, base), max(1, w * h // 14), OILED)
    return paint

def mail_ic(base=3):
    return mail(base=base)

def ember_band(R_base=IRON, y_rows=(0,), seed="e"):
    """A plain painter that draws the base plate and an ember bordure along the given rows (a trim strip)."""
    def paint(face, w, h):
        g = plate(R=R_base, base=3)(face, w, h)
        if face in ("top", "bottom"): return g
        for y in y_rows:
            for x in range(w):
                g[y][x] = EMBER[3 if (x + y) % 2 == 0 else 2]
            if face == "front" and w > 3: g[y][0] = EMBER[4]
        return g
    return paint

def ember_plume(seed="pl", tail=False):
    """Horsehair dyed ember: strands run front to back; the tips go pale."""
    def paint(face, w, h):
        g = blank(w, h); r = rng(seed, face, w, h)
        for y in range(h):
            for x in range(w):
                along = y if tail else x
                v = 2 + (1 if face == "top" else 0) + (1 if along % 3 == 0 else (-1 if along % 3 == 2 else 0)) + r.choice((0, 0, 1)) + LIT[face] // 2
                g[y][x] = clamp(EMBER, v)
        if face not in ("top", "bottom"):
            for x in range(w):
                if r.random() < 0.4: g[h - 1][x] = None
        return g
    return paint

def fold(grid_rows):
    """Mirror a half-width grid text into a full one."""
    return "\n".join(r + r[::-1] for r in grid_rows.strip("\n").splitlines())

def mirror(g): return [list(reversed(r)) for r in g]

def lit_side(g):
    """The wearer's lit side of a left-hand face grid: one tone up on iron."""
    out = [list(r) for r in mirror(g)]
    for r in out:
        for i, c in enumerate(r):
            if c in IRON[:-1]: r[i] = IRON[IRON.index(c) + 1]
    return out
