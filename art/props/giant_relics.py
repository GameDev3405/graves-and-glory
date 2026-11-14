"""Shared ground for the small giant relics (rune_stone, cairn, offering_bowl, giant_chain_link, helmet_grave_marker).

Kort, 2026-11: "Giant relics, small". No bones: worked stone and old iron left by giants long gone, weathered and mossed
like the stone hand. Stone uses the stone hand's pale weathered ramp (one world); iron is a giant's iron, gone to rust.
Every pattern is world-aligned and chunky (2-3 px cells), no speckle.
"""
import math
from proplib import E, Mat, make_ramp, h3, LIT, fixed, HOT, GILT, LINEN

GSTONE = make_ramp("8A8478", 0.20)    # the stone hand's weathered stone
DSTONE = make_ramp("6E6B66", 0.20)    # a darker field stone for a cairn's mix
MOSS = make_ramp("5F7A3A", 0.18)
LICHEN = make_ramp("8E9256", 0.16)
RUST = make_ramp("7A4630", 0.20)      # rust over a giant's iron
OLDIRON = make_ramp("4C4A4E", 0.20)   # the dark iron under the rust
RUNE = make_ramp("79BDAE", 0.14)      # the runes' faint cold light (full-bright part)
DARK = fixed("0E0C10", "16131A", "1E1A22", "28232C", "322C36")   # the inside of a helmet seen through its eyes
POPPY = make_ramp("A8402F", 0.18)
TANSY = make_ramp("C9AE48", 0.16)
STEM = make_ramp("4E6A30", 0.18)


def tone(ramp, face, d=0, base=2):
    return ramp[max(0, min(4, base + LIT[face] + d))]


def _moss_lichen(face, P, moss_h, top_moss, seed):
    """Moss up to a ragged `moss_h` px, moss patches on up faces, lichen spots on the sides; None for bare surface."""
    x, y, z = P
    if moss_h > 0:
        line = moss_h + int(h3(math.floor(x / 3), math.floor(z / 3), 9 + seed) * 4)
        if y < line:
            return tone(MOSS, face, 1 if h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 11) < 0.25 else 0)
    if face == "up" and h3(math.floor(x / 3), math.floor(z / 3), 19 + seed) < top_moss:
        return tone(MOSS, face, -1 if h3(math.floor(x / 2), math.floor(z / 2), 21) < 0.2 else 0)
    if face not in ("up", "down"):
        cell = (math.floor(x / 4), math.floor(y / 3), math.floor(z / 4))
        if h3(*cell, 31 + seed) < 0.04:
            return tone(LICHEN, face, 1 if h3(x, y, z, 35) < 0.25 else 0)
    return None


def stone(moss_h=6, top_moss=0.45, seed=0, ramp=GSTONE):
    """Weathered giant stone: big flat planes, a few long slanted cracks, rare 2x2 chips; mossy foot and tops."""
    def p(face, P, c, r, w, h):
        g = _moss_lichen(face, P, moss_h, top_moss, seed)
        if g is not None: return g
        x, y, z = P
        a = x if face in ("north", "south", "up", "down") else z
        k = (a * 0.7 + y) % 23.0
        if k < 1.0 and h3(round(a / 3), round(y / 3), 7 + seed) < 0.55: return -1
        if h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 13 + seed) < 0.03: return -1
        return 0
    m = Mat(ramp, 2, p, top=1)
    m.ramps = (MOSS, LICHEN)
    return m


def rust(seed=0, iron=0.18, moss_h=0, top_moss=0.0):
    """A giant's iron gone to rust: rust in 3 px blooms with patches of the dark iron showing, rust runs streaking down
    the side faces, lichen now and then; a mossy foot where it meets the earth."""
    def p(face, P, c, r, w, h):
        g = _moss_lichen(face, P, moss_h, top_moss, seed)
        if g is not None: return g
        x, y, z = P
        if h3(math.floor(x / 3), math.floor(y / 3), math.floor(z / 3), 41 + seed) < iron:
            return tone(OLDIRON, face, 1 if face in ("north", "east") and r == 0 else 0)
        if face not in ("up", "down"):
            a = x if face in ("north", "south") else z
            if h3(math.floor(a / 2), 43 + seed) < 0.22 and h3(math.floor(a / 2), math.floor(y / 6), 45 + seed) < 0.6: return -1
        k = h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 47 + seed)
        return 1 if k < 0.08 else 0
    m = Mat(RUST, 2, p, top=1)
    m.ramps = (OLDIRON, MOSS, LICHEN)
    return m


def pat_earth(face, P, c, r, w, h):
    x, y, z = P
    return -1 if h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 17) < 0.14 else 0


WAX = Mat(LINEN, 1, edges=False)
COIN = Mat(GILT, 2, edges=False)
FLAME = Mat(HOT, 3, lambda f, P, c, r, w, h: 1 if r == 0 else 0, edges=False, lit=False)


def candle(x, z, h, name="candle"):
    """A 2x2 tallow candle `h` px tall standing on y0 (pass y via the returned list's shift); returns (wax, flame)."""
    return E((x, 0, z), (x + 2, h, z + 2), WAX, skip="down", name=name), E((x, h, z), (x + 2, h + 2, z + 2), FLAME, skip="down", name=name + " flame")


def lift(els, dy):
    """Move elements up by dy px (their rotation origins too)."""
    for e in els:
        e.frm[1] += dy; e.to[1] += dy
        if e.rot: e.rot = (e.rot[0], e.rot[1], (e.rot[2][0], e.rot[2][1] + dy, e.rot[2][2]))
    return els


def octo_ring_y(cx, cz, R, t, y0, y1, mat, name="ring", faces=None):
    """An octagonal ring standing on the ground plane (axis y): outer inradius R, walls `t` thick, centre (cx, cz).
    Four straight walls and four 45-degree walls, those 1 px lower at each end so no two tops share a plane."""
    L = int(round(R * math.tan(math.radians(22.5))))
    o = (cx, (y0 + y1) / 2, cz)
    out = [E((cx - L, y0, cz - R), (cx + L, y1, cz - R + t), mat, name=name, faces=faces),
           E((cx - L, y0, cz + R - t), (cx + L, y1, cz + R), mat, name=name, faces=faces),
           E((cx - R, y0, cz - L), (cx - R + t, y1, cz + L), mat, name=name, faces=faces),
           E((cx + R - t, y0, cz - L), (cx + R, y1, cz + L), mat, name=name, faces=faces)]
    for ang in (45, -45):
        out.append(E((cx - L, y0 + 1, cz - R), (cx + L, y1 - 1, cz - R + t), mat, name=name, faces=faces, rot=("y", ang, o)))
        out.append(E((cx - L, y0 + 1, cz + R - t), (cx + L, y1 - 1, cz + R), mat, name=name, faces=faces, rot=("y", ang, o)))
    return out


def old_iron(seed=0, bloom=0.3, moss_h=0, top_moss=0.0, top=1):
    """Dark old iron with big rust blooms (4 px cells) and rust runs down the sides: calmer than `rust`, for a piece
    that should still read as worked iron at a distance."""
    def p(face, P, c, r, w, h):
        g = _moss_lichen(face, P, moss_h, top_moss, seed)
        if g is not None: return g
        x, y, z = P
        if h3(math.floor(x / 4), math.floor(y / 4), math.floor(z / 4), 61 + seed) < bloom:
            return tone(RUST, face, 1 if h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 63) < 0.15 else 0)
        if face not in ("up", "down"):
            a = x if face in ("north", "south") else z
            if h3(math.floor(a / 2), 65 + seed) < 0.18 and h3(math.floor(a / 2), math.floor(y / 8), 67 + seed) < 0.5:
                return tone(RUST, face, -1)
        return 0
    m = Mat(OLDIRON, 2, p, top=top)
    m.ramps = (RUST, MOSS, LICHEN)
    return m
