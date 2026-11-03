"""Graveborn shared ramps (ART-STYLE-GUIDE 10.6), darkest first, derived from the spec's Look bases; plus a per-skin
violet check. Import: sys.path.insert(0, <art/hand/common>); import graveborn as GB.

The House accent is VIOLET (pale violet, guide 10.1). tools/art_hue_check.py counts only texels with saturation >= 0.30
and value >= 0.12; of VIOLET only tones 0 to 3 count (tone 4 and 5 are too grey), so put tones 1 to 3 where they show:
hems, bindings, wraps, cords, grips, mask trim. Moss and rust are saturated too and compete with it.
"""
import colorsys
from PIL import Image
import random
from handlib import ramp, blank, clamp, LIT, lightx

VIOLET = ramp("#1E1528", "#362645", "#54406A", "#7A6494", "#A28FBA", "#D2C6D8")   # House accent (fixed paint)
BONE   = ramp("#3A3328", "#5E5544", "#857B66", "#A89F87", "#C9C1A8", "#E4DECB")   # bare bone, skulls
MOSS   = ramp("#161A12", "#262E1E", "#36402A", "#4F5A3C", "#6A7552", "#87906A")   # grave-moss rags
RUST   = ramp("#1E130D", "#34221A", "#4E3324", "#6B4A33", "#8A6444", "#A8805A")   # rusted mail and coif
GIRON  = ramp("#0C0B0C", "#171516", "#221F1E", "#2B2826", "#3D3936", "#55504B", "#746D66")   # black grave-iron
FROST  = ramp("#26323A", "#3E4E57", "#5C6F78", "#7A8E96", "#93A7AE", "#B8C8CC", "#DCE3E2")   # frost grey to rime
SOUL   = ramp("#3E5A6E", "#7FA8C4", "#CFEFFF", "#F2FCFF")                             # soul-light (glow only)
GILT   = ramp("#2E1E0E", "#482E16", "#6F5026", "#96783A", "#BDAA6A")                  # funeral gilt (mask, jaw wire)
ROT    = ramp("#141210", "#221E1A", "#332C24", "#4A4134", "#625746", "#7A6E5A")      # rotted burial cloth
GSKIN  = ramp("#1E231D", "#2A3028", "#3E463A", "#56604F", "#6F7A66", "#8A9480")      # Risen grey-green flesh

# ---- tier trims (II salvaged iron, bone and violet binding; III black plate, funeral gilt and the crown-level part) ----
def _rng(*k): return random.Random(repr(k))
def _lit(face, x, w, base): return base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)

def gb_plate(tier, seed="gp", worn=0.12):
    """Grave-iron plate: tier 2 pitted salvaged iron piped in violet along its foot, tier 3 black plate chased in gilt."""
    base = 3 if tier == 2 else 2
    def paint(face, w, h):
        r = _rng(seed, face, w, h); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = _lit(face, x, w, base + 1) + r.choice((-1, 0, 0, 1))
                if r.random() < worn: v -= 2
                g[y][x] = clamp(GIRON, v)
        if face not in ("top", "bottom"):
            for x in range(w): g[0][x] = clamp(GIRON, base + 3 + LIT[face])                      # the lit rolled edge
            if h >= 3:
                for x in range(w):
                    g[h - 1][x] = (VIOLET[2 + (x % 2)] if tier == 2 else GILT[2 + (x % 2)]) if face != "left" or x % 2 == 0 else g[h - 1][x]
            if face in ("front", "back") and w >= 4 and h >= 4:                                   # rivets, a diagonal glint
                g[1][1] = g[1][w - 2] = GIRON[6] if tier == 2 else GILT[4]
                for i in range(min(w, h) - 2): g[h - 3 - i][1 + i] = GIRON[5] if tier == 2 else GIRON[4]
        else:
            for x in range(w): g[0][x] = clamp(GIRON, base + 2)
        return g
    return paint

def gb_bone(seed="gbn", base=3):
    """A slab of bare bone: pale on top, darker toward the foot, a few cracks."""
    def paint(face, w, h):
        r = _rng(seed, face, w, h); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) - (y * 2 // max(1, h)) + r.choice((0, 0, 0, 1))
                g[y][x] = clamp(BONE, v)
        if face not in ("top", "bottom") and h >= 3:
            for _ in range(max(1, w * h // 9)): g[1 + r.randrange(h - 1)][r.randrange(w)] = BONE[1]
        return g
    return paint

def gb_spike(R=BONE, base=3, tip=2):
    """A spike or tine: dark at the root, brightening to a pale tip (the last row)."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                t = (y * (tip + 1)) // max(1, h)
                v = base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) - (tip - t)
                g[y][x] = clamp(R, v)
        if face not in ("top", "bottom") and h > 1: g[h - 1] = [clamp(R, base + tip + 1)] * w
        return g
    return paint

def gb_band(R=VIOLET, lo=1, bead=BONE):
    """A wrap or cord: a violet band, a bead of `bead` set in its front."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = R[lo + ((x + y) % 3 == 0) + (face in ("front", "right") and (x + y) % 2 == 0)]
        if face == "front" and bead: g[h // 2][w // 2] = bead[4]
        return g
    return paint

def gb_gilt(seed="ggl"):
    """Funeral gilt: dull gold, lit on top, a scratched surface."""
    def paint(face, w, h):
        r = _rng(seed, face, w, h); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(GILT, _lit(face, x, w, 3) - (1 if y == h - 1 and h > 2 else 0) + r.choice((-1, 0, 0, 0, 1)))
        return g
    return paint

def gb_skull(seed="gsk"):
    """A small trophy skull: bone, two dark sockets and a nasal notch on the front."""
    def paint(face, w, h):
        g = gb_bone(seed, 4)(face, w, h)
        if face == "front" and w >= 3 and h >= 3:
            g[1][0] = g[1][w - 1] = BONE[0]; g[2][w // 2] = BONE[1]
            if h >= 4:
                for x in range(1, w - 1): g[h - 1][x] = BONE[2]
        return g
    return paint

def gb_rag(R=ROT, cut=2, key="grg", base=2):
    """A hanging rag strip: folds, a violet binding two rows above a ragged foot."""
    def paint(face, w, h):
        r = _rng(key, face, w, h); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(R, _lit(face, x, w, base) + (1 if (x % 3 == 0) else 0))
        if face in ("front", "back") and h >= 4:
            for x in range(w): g[h - 2][x] = VIOLET[1 + (x % 2) + (face == "front")]
            for x in range(w):
                for y in range(h - r.randrange(cut + 1), h): g[y][x] = None
        return g
    return paint

def violet_share(png, centre=269, band=22):
    """Share of a skin's saturated weight inside the violet band (art_hue_check counts >= 0.10 over the House)."""
    tot = inb = 0.0
    for r, g, b, a in Image.open(png).convert("RGBA").getdata():
        if a < 200: continue
        h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
        if s < 0.30 or v < 0.12: continue
        w = s * v; tot += w
        d = abs(int(h * 360) - centre) % 360
        if min(d, 360 - d) <= band: inb += w
    return inb / tot if tot else 0.0

if __name__ == "__main__":
    import sys
    for p in sys.argv[1:]: print("%-40s violet %3d%%" % (p, violet_share(p) * 100))
