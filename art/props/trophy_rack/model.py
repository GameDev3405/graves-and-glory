"""Trophy Rack (`trophy_rack`): 2x2x1 cells, anchor cell (0,0,0). Brief: art/props/trophy_rack/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..32, y 0..32, z 0..16 px. The back (south, z 16) sits flush on a wall.
A timber trophy board: plank panel between two posts and a stepped crown, a spear and a sword crossed behind a battered
enemy great helm (dented crown, rust, snapped crest, mail collar), two captured round shields low in the corners (dark green
with a pale slash, oxblood with a pale cross).
Build:  python art/props/build_props.py trophy_rack      Card:  python tools/prop_card.py trophy_rack
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, IRON, LINEN, cylinder, SWOOD, pat_plank   # noqa: F401

ENEMY = make_ramp("5E8A4C")          # captured colours: bright moss green
ENEMY2 = make_ramp("A03C34")         # a second captured colour: brick red
RUST = make_ramp("8A4B2A", 0.18)     # rust on the helm


def shield_pat(cx, cy, kind):
    """Round shield face (centre cx, cy): `slash` = a pale diagonal across the field, `cross` = a pale cross; a chipped texel now and then."""
    def pat(face, P, c, r, w, h):
        x, y, z = P
        if face != "north": return 0
        if kind == "slash":
            if -1.6 <= (x - cx) - (y - cy) < 1.6: return LINEN[3]
        else:
            if abs(x - cx) < 1.2 or abs(y - cy) < 1.2: return LINEN[2]
        return -1 if h3(x, y, 3) < 0.08 else 0
    return pat


MAT["shield_a"] = Mat(ENEMY, 2, shield_pat(26, 10, "slash"))
MAT["shield_b"] = Mat(ENEMY2, 2, shield_pat(6, 10, "cross"))
MAT["dark_plank"] = Mat(SWOOD, 1, pat_plank("x"))   # stained board: the weapons and helm stand out
MAT["rust"] = Mat(RUST, 2)


def build():
    b = [
        # the board: plank panel on the wall, two posts, a stepped crown, a foot rail
        E((3, 2, 14), (29, 28, 16), "dark_plank", skip="south", name="back panel"),
        E((0, 1, 12), (4, 30, 16), "wood_y", skip="south", name="post west"), E((28, 1, 12), (32, 30, 16), "wood_y", skip="south", name="post east"),
        E((4, 27, 12), (28, 30, 16), "swood_x", skip="south", name="top rail"),
        E((8, 30, 13), (24, 32, 16), "swood_x", skip="south", name="crown"),
        E((4, 1, 12), (28, 3, 16), "swood_x", skip="south", name="foot rail"),
        # spear and sword crossed behind the helm (rotated 45 degrees about the crossing)
        E((15, 3, 13), (17, 29, 14), "wood_y", skip="south", rot=("z", 45, (16, 16, 13)), name="spear shaft"),
        E((13, 28, 13), (19, 32, 14), "steel", skip="south", rot=("z", 45, (16, 16, 13)), name="spear head"),
        E((14, 5, 11), (17, 21, 13), "steel", rot=("z", -45, (16, 16, 13)), name="sword blade"),
        E((9, 21, 11), (23, 22, 13), "iron", rot=("z", -45, (16, 16, 13)), name="sword guard"),
        E((15, 22, 11), (17, 27, 13), "leather", rot=("z", -45, (16, 16, 13)), name="sword grip"),
        E((14, 27, 11), (18, 29, 13), "gilt", rot=("z", -45, (16, 16, 13)), name="sword pommel"),
        # the battered enemy helm: collar, dented dome, slit and nose bar, rust, a snapped crest
        E((10, 9, 8), (22, 12, 12), "iron", name="helm collar"),
        E((11, 12, 7), (21, 19, 12), "iron", name="helm"),
        E((12, 19, 8), (18, 22, 12), "iron", name="crown"), E((18, 19, 8), (20, 21, 12), "iron", name="crown dent"),
        E((11, 15, 6), (15, 16, 7), "soot", name="eye slit"), E((17, 15, 6), (21, 16, 7), "soot", name="eye slit"),
        E((15, 12, 6), (17, 19, 7), "steel", name="nose bar"),
        E((12, 17, 6), (14, 19, 7), "rust", name="rust patch"), E((18, 12, 6), (20, 14, 7), "rust", name="rust patch"),
        E((15, 22, 9), (17, 25, 11), "leather", name="crest stump"), E((17, 24, 9), (19, 25, 11), "leather", name="crest snap"),
    ]
    # two captured round shields low in the corners, bosses proud
    b += cylinder((26, 10, 10), 4, 4, "z", "shield_a", name="captured shield", skip="south")
    b += cylinder((6, 10, 10), 4, 4, "z", "shield_b", name="captured shield", skip="south")
    b += [E((25, 9, 8), (27, 11, 10), "iron", name="shield boss"), E((5, 9, 8), (7, 11, 10), "iron", name="shield boss")]
    return Prop("trophy_rack", {"trophy_rack": b})
