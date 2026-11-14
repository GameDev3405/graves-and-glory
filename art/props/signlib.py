"""Shared builder for the signpost props (signpost_1, _2, _3): a stained-timber post on a rubble footing with 1-3 blank
weathered arm boards. A board points east (the viewer's left on the front) or west, is mounted on the front (north) or the
back of the post, and may carry a carved arrow on both faces. No lettering. Nothing here is a prop of its own.
Frame: 1x3x1 cells, the prop stored as one part shifted down 16 px; the post is x 6..10, z 6..10.
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, WOOD, IRON, pat_grain   # noqa: F401

SHIFT = (0, 16, 0)
GRAIN = pat_grain("x")


def board_mat(x_tip, y_mid, east, carve=True):
    """A weathered arm board: grain along x, a darker split now and then and, if `carve`, an arrow engraved along the
    middle row of both big faces with a triangular head toward the tip. The tip x is world x, so front and back line up."""
    sgn = 1 if east else -1

    def pat(face, P, c, r, w, h):
        x, y, z = int(math.floor(P[0])), int(math.floor(P[1])), int(math.floor(P[2]))
        if face in ("north", "south"):
            d = (x_tip - 1 - x) * sgn                  # texels back from the last texel of the tip
            dy = abs(y - y_mid)
            if carve and ((dy == 0 and 4 <= d <= 12) or (d == 5 and dy == 1) or (d == 6 and dy == 2)):
                return -2
        return GRAIN(face, P, c, r, w, h) - (1 if h3(x, y, z, 17) < 0.07 else 0)
    return Mat(WOOD, 3, pat)


def board(side, east, y0, name="arm", carve=True, h=7):
    """One arm board with a stepped point, `h` tall (odd), at y0..y0+h, 14 px long. Returns a list of E."""
    ym = y0 + h // 2
    z0, z1 = (4, 6) if side == "front" else (10, 12)
    skip_face = "south" if side == "front" else "north"
    a, b = (2, 16) if east else (0, 14)
    m = board_mat(b if east else a, ym, east, carve)
    if east:
        parts = [((a, y0, z0), (b - 3, y0 + h, z1)), ((b - 3, y0 + 1, z0), (b - 2, y0 + h - 1, z1)),
                 ((b - 2, y0 + 2, z0), (b - 1, y0 + h - 2, z1)), ((b - 1, ym, z0), (b, ym + 1, z1))]
    else:
        parts = [((a + 3, y0, z0), (b, y0 + h, z1)), ((a + 2, y0 + 1, z0), (a + 3, y0 + h - 1, z1)),
                 ((a + 1, y0 + 2, z0), (a + 2, y0 + h - 2, z1)), ((a, ym, z0), (a + 1, ym + 1, z1))]
    names = (name, name + " step", name + " step", name + " point")
    return [E(f, t, m, skip=skip_face, name=nm) for (f, t), nm in zip(parts, names)]


def sign(pid, boards, extra=()):
    els = [
        E((4, 0, 4), (12, 3, 12), "stone", skip="down", name="footing"),
        E((2, 0, 7), (5, 2, 11), "stone_top", skip="down", name="rubble"),
        E((11, 0, 5), (14, 2, 8), "stone_top", skip="down", name="rubble"),
        E((6, 3, 6), (10, 42, 10), "swood_y", name="post"),
        E((5, 42, 5), (11, 43, 11), "swood_x", name="post cap"),
        E((7, 43, 7), (9, 45, 9), "swood_y", name="post finial"),
    ]
    for side, east, y0 in boards:
        els += board(side, east, y0)
    els += list(extra)
    return Prop(pid, {pid: els}, shift={pid: SHIFT})
