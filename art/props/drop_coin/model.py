"""Drop Coin (`drop_coin`): 1x1x1 cells, anchor cell (0,0,0). Brief: art/props/drop_coin/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..16, z 0..16 px.

The giant coin the drop altar (art/props/drop_altar) drops: a bronze disc 10 px across (radius 5 = 0.3125 block) and
3 px thick (0.1875 block), its faces looking north and south, with a glimpur crystal core 6 px across (shade off) and a
rune on each face. Model only: the game renders it itself (no collision).
Centre: x 8, y 8 (the cell centre), z 8.5 (the disc spans z 7..10; whole-px faces and an odd thickness cannot sit on
z 8). The footprint check forbids geometry below the floor, so the disc is centred on the cell, not on the origin.
Build:  python art/props/build_props.py drop_coin      Card:  python tools/prop_card.py drop_coin
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, cylinder   # noqa: F401  (materials: proplib.MAT keys)

BRONZE = make_ramp("8a6a3e")                                   # giant bronze, dulled (same as the altar)
GLIM = fixed("3a2a66", "6a52b0", "a08ae6", "d6ccf8", "f6f2ff")  # glimpur crystal, violet to white (same as the altar)

C = (8, 8)          # disc centre (x, y)
Z0, Z1 = 7, 10      # disc thickness

# the face rune on the crystal core (6x6, row 0 = top, symmetric so it reads the same on both faces): a giant's eye-rune
RUNE = ["..##..",
        ".#..#.",
        "#.##.#",
        "#.##.#",
        ".#..#.",
        "..##.."]


class CoinPaint:
    """Bronze ring: a lit upper rim, four engraved notches (top, bottom, sides), shaded lower rim, edge faces lit by
    the troop light. Core: glimpur, white-violet with the eye-rune in deep violet."""
    ramps = (BRONZE, GLIM)

    def __init__(s, core):
        s.core = core

    def __call__(s, face, P, c, r, w, h):
        x, y, z = P
        dx, dy = x - C[0], y - C[1]
        d = math.hypot(dx, dy)
        if s.core:
            if face in ("north", "south"):
                gx, gy = int(math.floor(x - (C[0] - 3))), int(math.floor((C[1] + 3) - y))
                if 0 <= gx < 6 and 0 <= gy < 6 and RUNE[gy][gx] == "#": return GLIM[1]
                return GLIM[4] if gy == 0 or gx == 0 else GLIM[3]
            return GLIM[2]
        if face in ("north", "south"):
            ax, ay = abs(dx), abs(dy)
            if (ax < 1 and ay > 3.5) or (ay < 1 and ax > 3.5): return BRONZE[0]       # engraved notches
            if d > 4.2: return BRONZE[3] if dy > 0 else BRONZE[1]                       # rim: lit above, shaded below
            return BRONZE[2]
        return BRONZE[{"up": 3, "east": 2, "west": 1, "down": 0}.get(face, 2)]


def build():
    coin = cylinder((C[0], C[1], Z0), 5, Z1 - Z0, "z", CoinPaint(False), name="coin rim", hollow=3)
    coin += cylinder((C[0], C[1], Z0), 3, Z1 - Z0, "z", CoinPaint(True), name="glimpur core", shade=False)
    return Prop("drop_coin", {"drop_coin": coin})
