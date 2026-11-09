"""Stake Barricade (`stake_barricade`): 3x2x1 cells, anchor cell (1,0,0). Brief: art/props/stake_barricade/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..32, z 0..16 px.

Field cover: sharpened stakes lashed to two rails on a split-log sill, points leaning out to the front (the enemy side).
Build:  python art/props/build_props.py stake_barricade      Card:  python tools/prop_card.py stake_barricade
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, WOOD, LINEN   # noqa: F401
from proplib import pat_grain

# freshly cut stakes: the bark is the dark wood ramp, the sharpened points show pale heartwood
HEART = make_ramp("B49A72", 0.16)
POINT = Mat(HEART, 2, pat_grain("y"))
ROPE = Mat(LINEN, 1, lambda f, P, c, r, w, h: -1 if (r + c) % 2 else 0)


def build():
    els = [
        # split-log sill on the ground, the rails' feet
        E((-16, 0, 9), (32, 5, 15), "stump", skip="down", faces={"east": "endgrain", "west": "endgrain"}, name="sill log"),
        E((-14, 0, 15), (-10, 3, 16), "swood_y", skip="down", name="sill chock"),
        E((26, 0, 15), (30, 3, 16), "swood_y", skip="down", name="sill chock"),
        # two back rails the stakes are lashed to, set where the leaning stakes pass
        E((-16, 10, 11), (32, 13, 14), "wood_x", name="low rail"),
        E((-16, 19, 7), (32, 22, 10), "wood_x", name="high rail"),
        # props behind, bracing the wall against a charge: from the ground behind up under the high rail
        E((-11, 0, 13), (-8, 12, 16), "wood_y", rot=("x", -22.5, (-11, 0, 13)), name="brace"),
        E((24, 0, 13), (27, 12, 16), "wood_y", rot=("x", -22.5, (24, 0, 13)), name="brace"),
    ]
    # stakes leaning to the front: 4 px thick, alternating heights, a pale sharpened point on each
    xs = [-15, -9, -3, 3, 9, 15, 21, 27]
    for i, x in enumerate(xs):
        h = 23 if i % 2 else 27
        org = (x, 0, 11)
        els.append(E((x, 0, 11), (x + 4, h, 15), "wood_y", rot=("x", -22.5, org), name="stake"))
        els.append(E((x + 1, h, 12), (x + 3, h + 4, 14), POINT, rot=("x", -22.5, org), name="point"))
    # rope lashings binding pairs of stakes, on their front faces just above the low rail
    for x in (-15, 3, 21):
        els.append(E((x, 12, 10), (x + 10, 14, 11), ROPE, rot=("x", -22.5, (x, 0, 11)), name="lashing"))
    return Prop("stake_barricade", {"stake_barricade": els})
