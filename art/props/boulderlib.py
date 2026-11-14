"""Shared ground for the nine boulders (`boulder_<small|medium|large>_<grey|dark|mossy>`): the three stones and the lobe
shapes. Each prop's model.py only names its size and stone. Frame and rules: art/props/README.md.
"""
import os, sys, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from proplib import E, Prop, Mat, make_ramp, h3, blob, LIT   # noqa: F401

GREY = make_ramp("6C6E74", 0.20)    # weathered field stone, mid grey, a little cool (8E9094 rendered nearly white in game)
DARK = make_ramp("55535C", 0.22)    # basalt / old slag: darker, violet-leaning
MOSS = make_ramp("5F7A3A", 0.18)    # the same moss as the stone hand

def _crack(salt):
    """World-aligned weathering: a few long hairline cracks, one faint bedding line, rare chips; no speckle field."""
    def pat(face, P, c, r, w, h):
        x, y, z = P
        a = (x if face in ("north", "south", "up", "down") else z)
        k = (a * 0.7 + y) % 19.0
        if k < 1.0 and h3(round(a / 3), round(y / 3), salt) < 0.5: return -1
        if face not in ("up", "down") and y % 11 == 0 and h3(round(a / 4), y, salt + 1) < 0.6: return -1
        if h3(x, y, z, salt + 2) < 0.03: return -1
        if face not in ("up", "down") and h3(x, y, z, salt + 3) < 0.02: return 1
        return 0
    return pat


class Rock(Mat):
    """Stone with optional moss: on the tops (mostly) and in the cracks only: the same hairline cracks and bedding line
    the stone pattern draws are filled with moss on the sides, so the moss reads as growing in the joints."""
    def __init__(s, ramp, salt, moss=None, topcover=0.0):
        super().__init__(ramp, 2, _crack(salt))
        s.moss, s.topcover, s.salt = moss, topcover, salt
        s.ramps = (ramp,) + ((moss,) if moss else ())

    def __call__(s, face, P, c, r, w, h):
        if s.moss:
            x, y, z = P
            if face == "up": on = h3(x // 2, z // 2, s.salt + 9) < s.topcover
            elif face == "down": on = False
            else: on = super().__call__(face, P, c, r, w, h) is not None and s.pattern(face, P, c, r, w, h) == -1 and h3(x, y, z, s.salt + 17) < 0.85
            if on:
                t = 2 + (1 if face == "up" else LIT[face])
                if h3(x, y, z, s.salt + 13) < 0.10: t -= 1
                return s.moss[max(0, min(4, t))]
        return super().__call__(face, P, c, r, w, h)


STONES = {
    "grey": lambda: Rock(GREY, 41),
    "dark": lambda: Rock(DARK, 53),
    "mossy": lambda: Rock(GREY, 67, MOSS, topcover=0.7),
}
SEEDS = {"grey": 1, "dark": 2, "mossy": 3}
FLIP = {"grey": (False, False), "dark": (True, False), "mossy": (False, True)}   # mirrored so a field of them does not repeat

# Rock masses (chamfered, rounded blocks), several turned 22.5 or 45 degrees about their own vertical axis (rotation origin =
# the centre of the mass's bottom face). Each mass is an octagonal prism made of boxes: two crossed boxes (a plus) plus a 45 degree
# diamond filling each notched corner (the chamfer), with a narrower top step (the rounded top). A chamfer of `c` px uses diamonds
# `2*round(c*0.707)` px wide. Sketch frame 0..N px; (x0, z0, x1, z1, y0, y1, angle, c); c = 0 is a plain box (pebbles).
# Cluster layout: tall stone off-centre, lower bodies beside it overlapping its height range, pebbles; no cap, all heights differ.
MASSES = {
    "small": (16, [
        (5, 2, 13, 10, 0, 10, 22.5, 2),   # the tall stone, off to the north-east
        (1, 5, 9, 13, 0, 6, 0, 2),        # a wide low body beside it
        (4, 9, 12, 14, 0, 4, 45, 2),      # a turned chunk at the front
        (11, 10, 15, 14, 0, 3, 22.5, 0),  # pebble
        (2, 1, 6, 5, 0, 3, 45, 0),        # pebble
    ]),
    "medium": (32, [
        (12, 5, 24, 17, 0, 24, 22.5, 3),  # the tall stone
        (2, 10, 17, 28, 0, 12, 0, 3),     # west body
        (13, 16, 29, 30, 0, 15, 22.5, 3), # south-east body, turned
        (23, 1, 31, 9, 0, 8, 45, 2),      # low shoulder at the north-east
        (1, 1, 7, 7, 0, 5, 22.5, 0),      # pebble at the foot
    ]),
    "large": (48, [                        # low and wide: 3 cells across, about 2 high
        (14, 6, 36, 26, 0, 28, 22.5, 4),  # the highest mass, off to the back
        (2, 16, 22, 40, 0, 19, 0, 4),     # west body
        (20, 22, 44, 44, 0, 15, 22.5, 4), # south-east body, turned
        (30, 2, 46, 16, 0, 10, 45, 3),    # low shoulder at the north-east
        (4, 2, 14, 12, 0, 7, 45, 3),      # rubble at the north-west
    ]),
}
ROT_SIGN = 1   # which way a positive y rotation turns, for placing the diamonds


def _mass(x0, z0, x1, z1, y0, y1, ang, c, mat, name):
    """The elements of one chamfered mass (see MASSES). Returns a list of E, all turned by `ang` about the mass centre."""
    ox, oz = int(round((x0 + x1) / 2)), int(round((z0 + z1) / 2)); th = math.radians(ang) * ROT_SIGN
    def rot(a): return {"rot": ("y", a, (ox, y0, oz))} if a else {}
    def own(bx0, by0, bz0, bx1, by1, bz1, nm):       # a box turned about the mass origin
        kw = {"rot": ("y", ang, (ox, by0, oz))} if ang else {}
        return E((bx0, by0, bz0), (bx1, by1, bz1), mat, skip="down", name=nm, **kw)
    h = y1 - y0
    if c == 0:
        return [own(x0, y0, z0, x1, y1, z1, name)]
    t = 3 if h >= 14 else (2 if h >= 8 else 0); yb = y1 - t; i = c // 2 + 1
    d = 2 * int(round(c * 0.707))
    els = [own(x0, y0, z0 + c, x1, yb, z1 - c, name + " plus"), own(x0 + c, y0, z0, x1 - c, yb, z1, name + " plus")]
    # diamonds: the turn of the mass plus 45 degrees (a square repeats every 90), centre moved with the mass turn
    ang_d = ang + 45
    if ang_d > 45: ang_d -= 90
    for (ix, iz) in ((x0 + c, z0 + c), (x1 - c, z0 + c), (x0 + c, z1 - c), (x1 - c, z1 - c)):
        dx, dz = ix - ox, iz - oz
        rx = dx * math.cos(th) + dz * math.sin(th); rz = -dx * math.sin(th) + dz * math.cos(th)
        cx, cz = int(round(ox + rx)), int(round(oz + rz))
        kw = {"rot": ("y", ang_d, (cx, y0, cz))} if ang_d else {}
        els.append(E((cx - d // 2, y0, cz - d // 2), (cx + d // 2, yb, cz + d // 2), mat, skip="down", name=name + " chamfer", **kw))
    if t:
        els.append(own(x0 + i, yb, z0 + i, x1 - i, y1, z1 - i, name + " top"))
    return els


def boulder(size, stone):
    """Elements for one boulder (small 1x1x1, medium 2x2x2, large 3x3x3 cells): a cluster of 4-5 chamfered masses and pebbles,
    shapes differ by size, mirrored per stone. A 3x3 prop's anchor is its middle cell, so its footprint runs -16..32 on x and z."""
    mat = STONES[stone](); N, masses = MASSES[size]; fx, fz = FLIP[stone]; O = -16 if size == "large" else 0
    els = []
    for k, (x0, z0, x1, z1, y0, y1, ang, c) in enumerate(masses):
        if fx: x0, x1 = N - x1, N - x0
        if fz: z0, z1 = N - z1, N - z0
        if fx != fz: ang = -ang
        els += _mass(x0 + O, z0 + O, x1 + O, z1 + O, y0, y1, ang, c, mat, "boulder %d" % (k + 1))
    return els
