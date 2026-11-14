"""Shared materials for the drill-yard props (training_dummy, archery_target): straw, burlap sacking and rope.
Straw reads as straw by its strands: short dark and pale dashes one px wide that run along the bundle, each column
with its own phase, on a warm yellow ramp. Nothing here is a prop of its own (the build only looks at art/props/<id>/model.py)."""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from proplib import Mat, make_ramp, h3, E, fixed   # noqa: F401

# fixed (not make_ramp, which washes bright bases to a pale cream): dark golden-brown strands up to pale gold
STRAW_R = fixed("6A4C1E", "93702B", "C29B3E", "DDBC62", "F0DA94")
BURLAP_R = make_ramp("8A7860", 0.16)     # greyer, duller sacking so the straw stands out against it
HEMP_R = make_ramp("8E7448", 0.20)       # rope


def _uv(face, P, axis):
    """(strand index, position along the strand) for a face of a bundle whose strands run along `axis`."""
    x, y, z = P
    if axis == "y":
        return (x, y) if face in ("north", "south") else (z, y) if face in ("east", "west") else (x, z)
    if axis == "x":
        return (y, x) if face in ("north", "south") else (z, x) if face in ("up", "down") else (y, z)
    return (x, z) if face in ("up", "down") else (y, z) if face in ("east", "west") else (x, y)


def straw_pattern(axis="y", dark=0.48, light=0.12, seg=3):
    """Strands: each column has its own phase and, in places, a shorter dash length, so the dark runs never line up."""
    def p(face, P, c, r, w, h):
        k, a = _uv(face, P, axis)
        k, a = int(math.floor(k)), int(math.floor(a))
        sl = 2 if h3(k, 7, 5) < 0.4 else seg
        ph = int(h3(k, 0, 3) * 5)
        s = (a + ph) // sl
        d = h3(k, s, 11)
        if k % 3 == 0 and d < 0.25: return -2          # a deep gap between strands now and then
        if d < dark: return -1
        if d > 1 - light: return 1
        return 0
    return p


STRAW_Y = Mat(STRAW_R, 2, straw_pattern("y"), edges=False, top=0)
STRAW_X = Mat(STRAW_R, 2, straw_pattern("x"), edges=False, top=0)
STRAW_Z = Mat(STRAW_R, 2, straw_pattern("z"), edges=False, top=0)


def pat_burlap(face, P, c, r, w, h):
    """Coarse weave: a thread row every other px, offset columns, an odd dark knot."""
    x, y, z = P
    u = x if face in ("north", "south", "up", "down") else z
    v = y if face not in ("up", "down") else z
    u, v = int(math.floor(u)), int(math.floor(v))
    if v % 2 == 0 and (u + v // 2) % 2 == 0: return -1
    if h3(u, v, 5) < 0.04: return 1
    return 0


BURLAP = Mat(BURLAP_R, 2, pat_burlap)
ROPE = Mat(HEMP_R, 2, lambda f, P, c, r, w, h: -1 if (c + r) % 2 else 0, edges=False)
