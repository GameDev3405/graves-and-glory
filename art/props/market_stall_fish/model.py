"""Market Stall, fish (`market_stall_fish`): the base `market_stall` model plus fish. 3x3x2 cells, anchor cell (1,0,0).
Brief: art/props/market_stall_fish/brief.md. One long ice tray with two rows of silver-grey and blue-white fish laid side by side.
Build:  python art/props/build_props.py market_stall_fish      Card:  python tools/prop_card.py market_stall_fish
"""
import importlib.util, math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, cylinder   # noqa: F401
_spec = importlib.util.spec_from_file_location("market_stall_base", os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "market_stall", "model.py"))
_base = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(_base)

ICE = Mat(make_ramp("A9C8D4", 0.14), 2, lambda f, P, c, r, w, h: -1 if h3(*P, 71) < 0.12 else 0, edges=False)
SILVER = make_ramp("A3ABB3", 0.20)     # silver-grey
BLUEWHITE = make_ramp("B4CDE6", 0.16)  # blue-white
BACK = make_ramp("3E5878", 0.20)       # the dark blue back stripe and the eye


def fish_mat(ramp, x0, head):
    """Flat 8x2x3 fish: a darker back stripe down the top, a light belly, an eye near the head end (head -1 west, +1 east)."""
    ex = x0 + 1.5 if head < 0 else x0 + 6.5
    def p(face, P, c, r, w, h):
        if face == "up": return BACK[2] if r == 1 else 1
        if face in ("north", "south"):
            if abs(P[0] - ex) < 0.6 and r == 0: return BACK[1]
            return -1 if r == 0 else (1 if r == h - 1 else 0)
        return 0
    m = Mat(ramp, 2, p, edges=False); m.ramps = (BACK,)
    return m


def fish(x, z, head, ramp, y=17):
    """A fish lying along x with its tail on the far end: an 8x2x3 body and a notched (forked) tail of two 2x1x1 prongs."""
    if head < 0:   # head at the west end, tail east
        bx, tx = x, x + 8
    else:
        bx, tx = x + 2, x
    m = fish_mat(ramp, bx, head)
    return [E((bx, y, z), (bx + 8, y + 2, z + 3), m, name="fish"),
            E((tx, y, z), (tx + 2, y + 1, z + 1), m, name="fish tail"),
            E((tx, y, z + 2), (tx + 2, y + 1, z + 3), m, name="fish tail")]


def build():
    # a display tray: ice stepped up toward the back so the back row shows over the front row; the front rim is low
    goods = [E((-12, 15, 4), (20, 17, 9), ICE, name="ice bed"),
             E((-12, 15, 9), (20, 19, 13), ICE, name="ice riser"),
             E((-13, 15, 3), (21, 17, 4), "plank_x", name="tray rim"),
             E((-13, 15, 13), (21, 20, 14), "plank_x", name="tray rim"),
             E((-13, 15, 4), (-12, 19, 13), "plank_z", name="tray rim"),
             E((20, 15, 4), (21, 19, 13), "plank_z", name="tray rim")]
    # two rows of three fish, 10 px each (8 body + 2 tail), alternating colour and direction
    for i, x in enumerate((-11, 0, 10)):
        goods += fish(x, 4, -1 if i % 2 == 0 else 1, SILVER if i % 2 == 0 else BLUEWHITE, y=17)
        goods += fish(x, 9, 1 if i % 2 == 0 else -1, BLUEWHITE if i % 2 == 0 else SILVER, y=19)
    return _base.variant("market_stall_fish", goods)
