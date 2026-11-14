"""Market Stall, bread (`market_stall_bread`): the base `market_stall` model plus bread. 3x3x2 cells, anchor cell (1,0,0).
Brief: art/props/market_stall_bread/brief.md. A wicker basket of round loaves, a board of baguettes and loaves on the back shelf.
Build:  python art/props/build_props.py market_stall_bread      Card:  python tools/prop_card.py market_stall_bread
"""
import importlib.util, math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, cylinder   # noqa: F401
_spec = importlib.util.spec_from_file_location("market_stall_base", os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "market_stall", "model.py"))
_base = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(_base)

CRUST = fixed("6E4318", "A06A22", "CF9230", "E8B550", "F6D58A")    # golden crust, lighter than the counter wood
WICKER = make_ramp("8E6C3A", 0.18)
DOUGH = fixed("A67B3C", "D0A55A", "EBC77A", "F6DD9E", "FFF0C6")    # the pale scored top

LOAF = Mat(CRUST, 2, lambda f, P, c, r, w, h: -1 if (f == "up" and (c + r) % 4 == 0) else (1 if (f == "up" and (c + r) % 4 == 2) else 0), edges=False)
BAGUETTE = Mat(CRUST, 2, lambda f, P, c, r, w, h: -1 if (f == "up" and (c + 2 * r) % 5 in (0, 1)) else (1 if f == "up" and r == 0 else 0), edges=False)
WEAVE = Mat(WICKER, 2, lambda f, P, c, r, w, h: -1 if (c + r) % 2 else 0, edges=False)
BOULE_TOP = Mat(DOUGH, 2, lambda f, P, c, r, w, h: -1 if (f == "up" and (c + r) % 3 == 0) else 0, edges=False)


def boule(x, y, z, w=6, d=5):
    """A round loaf: a body and a narrower top with a pale scored crown."""
    return [E((x, y, z), (x + w, y + 4, z + d), LOAF, name="loaf"),
            E((x + 1, y + 4, z + 1), (x + w - 1, y + 6, z + d - 1), BOULE_TOP, name="loaf crown")]


def build():
    goods = []
    # wicker basket on the left of the counter (x -13..5): four walls, a floor, loaves heaped above the rim
    goods += [E((-13, 15, 4), (5, 17, 13), WEAVE, name="basket floor"),
              E((-13, 17, 4), (5, 20, 5), WEAVE, name="basket wall"), E((-13, 17, 12), (5, 20, 13), WEAVE, name="basket wall"),
              E((-13, 17, 5), (-12, 20, 12), WEAVE, name="basket wall"), E((4, 17, 5), (5, 20, 12), WEAVE, name="basket wall")]
    goods += boule(-12, 17, 5, 8, 7) + boule(-3, 17, 5, 8, 7)
    # board of baguettes
    goods.append(E((8, 15, 4), (20, 16, 13), "plank_x", name="bread board"))
    goods += [E((8, 16, 5), (20, 19, 8), BAGUETTE, name="baguette"), E((9, 16, 9), (20, 19, 12), BAGUETTE, name="baguette"),
              E((10, 19, 6), (19, 21, 11), BAGUETTE, name="baguette")]
    # back shelf loaves (shelf top y 22)
    goods += [E((-9, 22, 26), (-1, 27, 30), LOAF, name="shelf loaf"), E((1, 22, 26), (9, 27, 30), LOAF, name="shelf loaf"),
              E((11, 22, 26), (19, 27, 30), LOAF, name="shelf loaf")]
    return _base.variant("market_stall_bread", goods)
