"""Map Table (`map_table`): 2x2x1 cells, anchor cell (0,0,0). Brief: art/props/map_table/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..32, y 0..32, z 0..16 px; the table fills y 0..14 (the upper cell holds the candle
and its light block).
A field commander's trestle table: plank top on two A-frame trestles (legs splayed along z), a spread map pinned by iron
weights and daggers at the corners, a rolled scroll and a candle stub whose flame is a glow part (map_table_flame).
Build:  python art/props/build_props.py map_table      Card:  python tools/prop_card.py map_table
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOT, WATER, STONE, LEATHER, LINEN   # noqa: F401

PARCH = make_ramp("CDB98A", 0.18)    # old parchment
RED = make_ramp("982418")            # route and X
FIELD = make_ramp("6B7F45")          # fields
INK = LEATHER[1]
WAX = make_ramp("D9CFAE", 0.14)
MAT["wax"] = Mat(WAX, 2)
FLAME = Mat(HOT, 3, edges=False, lit=False)
FLAME_CORE = Mat(HOT, 4, edges=False, lit=False)

W, H = 19, 8   # the map's inner size (the sheet is 21 x 10 with a 1 px border)
GRID = [[" "] * W for _ in range(H)]
def put(pts, ch):
    for x, y in pts:
        if 0 <= x < W and 0 <= y < H: GRID[y][x] = ch
put([(x, y) for x in range(0, 3) for y in range(0, 3)], "h"); put([(0, 3), (3, 1), (1, 3)], "h")
put([(x, y) for x in range(4, 8) for y in range(0, 3)], "g"); put([(4, 3), (5, 3)], "g")
put([(x, y) for x in range(14, 18) for y in range(1, 4)], "g"); put([(15, 4)], "g")
put([(x, y) for x in range(1, 6) for y in range(5, 8)], "g")
put([(18, 0), (17, 0), (16, 0)], "h"); put([(18, 1)], "h")
put([(12, 0), (11, 1), (11, 2), (10, 3), (10, 4), (9, 4), (9, 5), (8, 6), (7, 7)], "w")
put([(12, 1), (12, 2), (11, 3), (11, 4), (10, 5), (9, 6), (8, 7)], "w")
put([(2, 6), (3, 6), (4, 5), (5, 4), (6, 4), (7, 3), (8, 3), (13, 3), (14, 2), (15, 2)], "r")
GRID[3][8] = "r"; GRID[3][9] = "r"; GRID[3][10] = "r"
put([(3, 3)], "k"); put([(4, 3)], "k"); put([(3, 4)], "k"); put([(13, 5), (14, 5)], "k")
X_MARK = {(15, 4), (17, 4), (16, 5), (15, 6), (17, 6)}


class MapPaint:
    """The spread map: up face painted as seen from above (column 0 west, row 0 north), edges bare parchment."""
    ramps = (PARCH, RED, FIELD, WATER, STONE, WAX)

    def __call__(s, face, P, c, r, w, h):
        if face != "up":
            return PARCH[1] if face in ("south", "west") else PARCH[2]
        if c == 0 or r == 0 or c == w - 1 or r == h - 1:
            return PARCH[1] if (c + r) % 5 else PARCH[2]
        ix, iy = c - 1, r - 1
        if (ix, iy) in X_MARK: return RED[3]
        ch = GRID[iy][ix]
        return {"h": STONE[3], "g": FIELD[2], "w": WATER[3], "r": RED[2], "k": INK}.get(ch, PARCH[3] if (ix * 3 + iy * 5) % 17 else PARCH[2])


def build():
    t = [
        # plank top and the two cross battens under it
        E((1, 12, 2), (31, 14, 14), "plank_x", name="table top"),
        E((2, 10, 3), (7, 12, 13), "swood_z", name="trestle beam west"),
        E((25, 10, 3), (30, 12, 13), "swood_z", name="trestle beam east"),
    ]
    # two A-frame trestles: legs splayed north and south
    for cx in (3, 26):
        t.append(E((cx, 0, 1), (cx + 3, 11, 3), "wood_y", skip="down", rot=("x", 22.5, (cx, 0, 3)), name="trestle leg front"))
        t.append(E((cx, 0, 13), (cx + 3, 11, 15), "wood_y", skip="down", rot=("x", -22.5, (cx, 0, 13)), name="trestle leg back"))
    t += [
        E((7, 4, 7), (26, 6, 9), "swood_x", name="stretcher"),
        # the spread map, pinned
        E((5, 14, 3), (26, 15, 13), MapPaint(), skip="down", name="map"),
        E((4, 15, 3), (7, 17, 6), "iron", name="weight north-west"),
        E((4, 15, 10), (7, 17, 13), "stone", name="weight south-west"),
        E((21, 15, 10), (24, 17, 13), "iron", name="weight south-east"),
        # a dagger lying at the north-east corner
        E((19, 15, 4), (24, 16, 5), "steel", name="dagger blade"),
        E((24, 15, 3), (25, 17, 6), "iron", name="dagger guard"),
        E((25, 15, 4), (28, 16, 5), "leather", name="dagger grip"),
        E((28, 15, 4), (29, 16, 5), "gilt", name="dagger pommel"),
        # a rolled spare scroll
        E((9, 15, 11), (19, 18, 14), "linen", name="rolled scroll"),
        E((13, 15, 11), (14, 18, 14), "leather", name="scroll tie"),
        E((15, 15, 11), (16, 18, 14), "leather", name="scroll tie"),
        # candle: an iron dish, a wax stub with a drip
        E((26, 14, 9), (31, 15, 14), "iron", name="candle dish"),
        E((27, 15, 10), (30, 19, 13), "wax", name="candle"),
        E((27, 15, 9), (28, 17, 10), "wax", name="wax drip"),
    ]
    flame = [
        E((28, 19, 10), (29, 22, 13), FLAME, name="flame"),
        E((27, 19, 11), (30, 22, 12), FLAME, name="flame"),
        E((28, 22, 11), (29, 24, 12), FLAME_CORE, name="flame tip"),
    ]
    return Prop("map_table", {"map_table": t, "map_table_flame": flame}, glow=("map_table_flame",))
