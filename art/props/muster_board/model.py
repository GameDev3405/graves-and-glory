"""Muster Board (`muster_board`): 2x3x1 cells, anchor cell (0,0,0). Brief: art/props/muster_board/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..32, y 0..48, z 0..16 px.
A standing timber notice board: two posts, a plank board with a map, a contract and notices pinned to it, a roof beam,
a ledge with an ink pot and a rolled scroll.
Build:  python art/props/build_props.py muster_board      Card:  python tools/prop_card.py muster_board
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, LINEN, LEATHER, STEEL, WATER, STONE, WOOD  # noqa: F401

RED = make_ramp("982418")      # wax seal, route ink, marker
FIELD = make_ramp("6B7F45")    # map: fields
INK = LEATHER[1]

MAT["ink_pot"] = Mat(make_ramp("2C2A3A"), 2)


class Paper:
    """A pinned sheet: face painted as seen from the front (gx 0 = viewer's left). `art(gx, gy, w, h)` -> colour or None."""
    ramps = (RED, FIELD)   # prop_card's palette check only accepts ramps a Mat/painter declares in .ramp / .ramps

    def __init__(s, art, torn=False):
        s.art, s.torn = art, torn

    def __call__(s, face, P, c, r, w, h):
        if face != "north":
            return LINEN[3] if face == "up" else LINEN[2] if face == "east" else LINEN[1]
        gx, gy = c, r
        if s.torn and gy == h - 1 and gx % 3 != 1:
            return LINEN[1]
        if gx in (0, w - 1) or gy in (0, h - 1):
            return LINEN[3] if gy == 0 else LINEN[1]
        col = s.art(gx, gy, w, h)
        if col: return col
        return LINEN[2] if (gx + 2 * gy) % 11 else LINEN[3]


def text_lines(rows):
    """rows: {gy: (start, length)} -> ink on those rows."""
    def art(gx, gy, w, h):
        if gy in rows:
            a, n = rows[gy]
            if a <= gx < a + n: return INK
        return None
    return art


MAP = [  # inner 10 x 12, h hills, g fields, w river, k village
    ".hh...gg..",
    "hhh.ggg...",
    ".h..gggg..",
    "..k..gg...",
    "..........",
    "wwwwwww...",
    "...wwwwwww",
    "..........",
    "........gg",
    ".....ggggg",
    "....gggg..",
    "...ggg....",
]
# dashed route, start bottom-left, fording the river at x=5, ending in the red X (grid x, y; inner coords)
ROUTE = [(2, 10), (3, 10), (3, 9), (4, 8), (5, 7), (5, 6), (5, 5), (6, 4), (7, 4), (8, 3)]
ROUTE_ON = {pt for i, pt in enumerate(ROUTE) if i % 4 != 3}
MARK_X = {(7, 0), (9, 0), (8, 1), (7, 2), (9, 2)}


def map_art(gx, gy, w, h):
    ix, iy = gx - 1, gy - 1
    if (ix, iy) in MARK_X: return RED[2]
    if (ix, iy) in ROUTE_ON: return RED[2]
    ch = MAP[iy][ix]
    return {"h": STONE[3], "g": FIELD[2], "w": WATER[3], "k": INK}.get(ch)


SEAL = [".XXX.", "XXXXX", "XXXXX", ".XXX."]


def seal(gx, gy, w, h):
    lx, ly = gx - 2, gy - 9
    if 0 <= ly < 4 and 0 <= lx < 5 and SEAL[ly][lx] == "X":
        if (lx, ly) == (1, 1): return RED[3]
        return RED[1] if (ly == 3 or lx == 4) else RED[2]
    return None


CONTRACT_ROWS = {2: (2, 5), 3: (2, 5), 5: (1, 7), 7: (1, 6)}
NOTICE_ROWS = {2: (1, 4), 4: (1, 3), 5: (1, 4), 7: (1, 3)}
SCRAP_ROWS = {2: (1, 5), 4: (1, 3)}


def contract_art(gx, gy, w, h):
    return seal(gx, gy, w, h) or text_lines(CONTRACT_ROWS)(gx, gy, w, h)


def build():
    z0, z1 = 3, 4      # pinned paper layer, in front of the board face at z=4
    m = [
        # feet and posts
        E((0, 0, 2), (6, 2, 14), "swood_z", skip="down", name="foot west"),
        E((26, 0, 2), (32, 2, 14), "swood_z", skip="down", name="foot east"),
        E((1, 2, 6), (5, 44, 10), "wood_y", name="post west"),
        E((27, 2, 6), (31, 44, 10), "wood_y", name="post east"),
        E((5, 4, 7), (27, 6, 9), "swood_x", name="back stay"),
        # board, rails
        E((3, 12, 4), (29, 40, 6), "plank_x", name="board"),
        E((3, 10, 4), (29, 12, 7), "swood_x", name="bottom rail"),
        # roof beam and cap
        E((0, 40, 4), (32, 44, 11), "swood_x", name="top beam"),
        E((0, 44, 3), (32, 46, 13), "plank_x", name="roof plank"),
        E((6, 38, 3), (7, 40, 4), "iron", name="beam stud"), E((25, 38, 3), (26, 40, 4), "iron", name="beam stud"),
        # pinned papers
        E((5, 22, z0), (17, 36, z1), Paper(map_art), skip="south down", name="map of the marches"),
        E((19, 24, z0), (28, 38, z1), Paper(contract_art), skip="south down", name="contract"),
        E((19, 14, z0), (25, 23, z1), Paper(text_lines(NOTICE_ROWS), torn=True), skip="south down", name="notice"),
        E((7, 14, z0), (13, 20, z1), Paper(text_lines(SCRAP_ROWS), torn=True), skip="south down", name="scrap",
          rot=("z", -22.5, (10, 17, 3))),
        # pins
        E((6, 35, 2), (7, 36, 3), "iron", name="pin"), E((15, 35, 2), (16, 36, 3), "iron", name="pin"),
        E((6, 23, 2), (7, 24, 3), "iron", name="pin"), E((15, 23, 2), (16, 24, 3), "iron", name="pin"),
        E((20, 36, 2), (21, 37, 3), "iron", name="pin"),
        E((20, 15, 2), (21, 16, 3), "iron", name="pin"), E((23, 21, 2), (24, 22, 3), "iron", name="pin"),
        E((9, 18, 2), (10, 19, 3), "iron", name="pin"),
        # dagger driven through the contract
        E((26, 29, 2), (27, 34, 3), "steel", name="dagger blade"), E((26, 28, 2), (27, 29, 3), "steel", name="dagger point"),
        E((24, 34, 2), (29, 35, 3), "iron", name="dagger guard"), E((26, 35, 2), (27, 38, 3), "leather", name="dagger grip"),
        E((25, 38, 2), (28, 39, 3), "gilt", name="dagger pommel"),
        # ledge with ink pot and a rolled scroll
        E((17, 9, 1), (30, 10, 7), "plank_z", name="ledge"),
        E((26, 10, 2), (29, 14, 5), "ink_pot", name="ink pot"), E((26, 14, 3), (28, 15, 4), "ink_pot", name="ink pot lip"),
        E((19, 10, 2), (25, 13, 5), "linen", name="rolled scroll"),
        E((21, 10, 2), (22, 13, 5), "leather", name="scroll tie"),
    ]
    return Prop("muster_board", {"muster_board": m}, shift={"muster_board": (0, 16, 0)})   # y 0..48 stored as -16..32
