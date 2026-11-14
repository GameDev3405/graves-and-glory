"""Market Stall, pots (`market_stall_pots`): the base `market_stall` model plus clay pots. 3x3x2 cells, anchor cell (1,0,0).
Brief: art/props/market_stall_pots/brief.md. Round pots and stacked bowls on the counter, pots on the shelf, two hung pots, an amphora.
Build:  python art/props/build_props.py market_stall_pots      Card:  python tools/prop_card.py market_stall_pots
"""
import importlib.util, math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, cylinder   # noqa: F401
_spec = importlib.util.spec_from_file_location("market_stall_base", os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "market_stall", "model.py"))
_base = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(_base)

TERRA = make_ramp("B5653B", 0.20)
GLAZE = make_ramp("3F7A80", 0.20)

POT = Mat(TERRA, 2, lambda f, P, c, r, w, h: -1 if (f not in ("up", "down") and math.floor(P[1]) % 4 == 0) else 0, edges=False)
GLAZED = Mat(GLAZE, 2, lambda f, P, c, r, w, h: 1 if (f == "up") else 0, edges=False)
BAND = Mat(TERRA, 1, edges=False)


def pot(cx, cz, r=4, h=5, mat=POT):
    """A round pot: a cylinder body with a narrower neck and a dark rim band."""
    return (cylinder((cx, 15, cz), r, h, "y", mat, name="pot")
            + [E((cx - 2, 15 + h, cz - 2), (cx + 2, 15 + h + 2, cz + 2), mat, name="pot neck"),
               E((cx - 2, 15 + h + 2, cz - 2), (cx + 2, 15 + h + 3, cz + 2), BAND, name="pot rim")])


def build():
    goods = pot(-8, 8, 4, 5) + pot(2, 9, 3, 4) + pot(9, 8, 3, 6, GLAZED)
    # stacked bowls at the right of the counter
    goods += [E((14, 15, 5), (21, 16, 12), POT, name="bowl"), E((15, 16, 6), (20, 17, 11), POT, name="bowl"),
              E((16, 17, 7), (19, 19, 10), GLAZED, name="bowl")]
    # pots on the back shelf (shelf top y 22)
    goods += [E((-8, 22, 26), (-3, 27, 30), POT, name="shelf pot"), E((0, 22, 26), (6, 26, 30), GLAZED, name="shelf pot"),
              E((9, 22, 26), (14, 28, 30), POT, name="shelf pot"), E((17, 22, 26), (22, 26, 30), POT, name="shelf pot")]
    # two small pots hung from the awning edge
    for x in (-3, 14):
        goods += [E((x + 1, 28, 5), (x + 2, 34, 6), "linen", name="string"), E((x - 1, 22, 4), (x + 4, 28, 7), POT, name="hung pot")]
    # a tall amphora on the floor behind the counter's left end
    goods += cylinder((-6, 0, 20), 3, 10, "y", POT, skip="down", name="amphora") + [E((-8, 10, 18), (-4, 14, 22), POT, name="amphora neck")]
    return _base.variant("market_stall_pots", goods)
