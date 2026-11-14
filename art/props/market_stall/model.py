"""Market Stall (`market_stall`): 3x3x2 cells, anchor cell (1,0,0). Brief: art/props/market_stall/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..48, z 0..32 px. The prop is exactly 3 cells tall, so it stays
one part stored shifted down 16 px (shift={id: (0, 16, 0)}).

A wooden counter in front, four posts, a striped cloth awning pitched down to the front with a scalloped valance, a back
shelf and a price slate. The four variants (market_stall_fish/_bread/_cloth/_pots) import `base_elements()` from here
and add their goods (counter top is y 15, goods sit in x -13..29, z 4..13 and hang from the awning).
Build:  python art/props/build_props.py market_stall      Card:  python tools/prop_card.py market_stall
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, WOOD, SWOOD, LINEN, pat_plank, pat_grain   # noqa: F401

SHIFT = (0, 16, 0)
AWN_RED = make_ramp("A33A2E", 0.20)
AWN_CREAM = make_ramp("D2C29A", 0.16)


class Stripe:
    """Awning cloth: stripes 4 px wide across x, red and cream, lit like a Mat (top +1, front 0, back -1, underside -2)."""
    ramp = AWN_RED
    ramps = (AWN_RED, AWN_CREAM)
    LIT = {"up": 1, "north": 0, "east": 0, "south": -1, "west": -1, "down": -2}

    def __call__(s, face, P, c, r, w, h):
        ramp = AWN_RED if (math.floor(P[0]) // 4) % 2 == 0 else AWN_CREAM
        t = 2 + s.LIT[face]
        if face in ("north", "south") and r == h - 1: t -= 1
        return ramp[max(0, min(4, t))]


AWNING = Stripe()
PLANK_Y = Mat(WOOD, 2, pat_plank("y", 5))


def base_elements():
    els = [
        # counter: plank-fronted body and an overhanging top board
        E((-13, 0, 3), (29, 13, 13), "plank_x", skip="down", faces={"north": PLANK_Y}, name="counter body"),
        E((-15, 13, 1), (31, 15, 15), "swood_x", name="counter top"),
        E((-13, 4, 2), (29, 6, 3), "wood_x", name="counter brace"),
        # four posts, tall at the back so the cloth pitches down to the customers
        E((-14, 0, 1), (-11, 34, 4), "wood_y", skip="down", name="front post"),
        E((27, 0, 1), (30, 34, 4), "wood_y", skip="down", name="front post"),
        E((-14, 0, 27), (-11, 45, 30), "wood_y", skip="down", name="back post"),
        E((27, 0, 27), (30, 45, 30), "wood_y", skip="down", name="back post"),
        # rafters under the awning, back beam, back shelf
        E((-15, 32, 0), (-13, 34, 30), "wood_z", rot=("x", -22.5, (-15, 34, 0)), name="rafter"),
        E((29, 32, 0), (31, 34, 30), "wood_z", rot=("x", -22.5, (29, 34, 0)), name="rafter"),
        E((-11, 34, 27), (27, 37, 30), "wood_x", name="back beam"),
        E((-11, 20, 26), (27, 22, 30), "swood_x", name="back shelf"),
        E((-11, 12, 28), (-9, 20, 30), "wood_y", name="shelf bracket"),
        E((25, 12, 28), (27, 20, 30), "wood_y", name="shelf bracket"),
        # striped awning pitched 22.5 degrees, rising to the back
        E((-16, 33, -1), (32, 35, 31), AWNING, rot=("x", -22.5, (-16, 35, -1)), name="awning"),
    ]
    # scalloped valance hanging from the awning's front edge, stripes continuing
    for i in range(12):
        x = -16 + 4 * i
        els.append(E((x, 29 if i % 2 else 28, 0), (x + 4, 33, 2), AWNING, name="valance"))
    # price slate leaning at the back right of the counter top
    els.append(E((21, 15, 11), (27, 16, 13), "swood_x", name="slate foot"))
    els.append(E((21, 16, 12), (27, 22, 13), Mat(make_ramp("3B4A4A"), 2, lambda f, P, c, r, w, h: 1 if (f == "north" and r == 2 and 1 < c < w - 2) else 0), name="price slate"))
    return els


def build():
    return Prop("market_stall", {"market_stall": base_elements()}, shift={"market_stall": SHIFT})


def variant(pid, goods):
    """A stall variant: the base stall plus `goods` (a list of E), stored like the base."""
    return Prop(pid, {pid: base_elements() + goods}, shift={pid: SHIFT})
