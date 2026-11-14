"""Market Stall, cloth (`market_stall_cloth`): the base `market_stall` model plus cloth. 3x3x2 cells, anchor cell (1,0,0).
Brief: art/props/market_stall_cloth/brief.md. Folded cloth stacks and a bolt on the counter, three cloths hung from the awning.
Build:  python art/props/build_props.py market_stall_cloth      Card:  python tools/prop_card.py market_stall_cloth
"""
import importlib.util, math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, cylinder   # noqa: F401
_spec = importlib.util.spec_from_file_location("market_stall_base", os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "market_stall", "model.py"))
_base = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(_base)

RED = make_ramp("C4362B", 0.18)
BLUE = make_ramp("2E63B8", 0.18)
GREEN = make_ramp("3E943A", 0.18)


def folded(ramp):
    """A bolt of folded cloth: a 1 px light fold edge along the top of each face, a dark fold line every 3 px down the sides."""
    def p(f, P, c, r, w, h):
        if f in ("up", "down"): return 1 if (c == 0 or r == 0) else 0
        if r == 0: return 2
        return -1 if math.floor(P[1]) % 3 == 0 else 0
    return Mat(ramp, 2, p, edges=False)


def hanging(ramp):
    """A hung cloth seen from the front: a woven band near the hem, a lighter border down each side."""
    def p(f, P, c, r, w, h):
        if f not in ("north", "south"): return 0
        if r >= h - 3: return -1 if r == h - 1 else 1
        if r in (h - 5, h - 6): return 2
        if c in (0, w - 1): return 1
        return 0
    return Mat(ramp, 2, p, edges=False)


def build():
    YELLOW = make_ramp("E0B52A", 0.16)
    goods = [E((-12, 15, 5), (-2, 21, 12), folded(RED), name="cloth stack"),
             E((0, 15, 5), (10, 20, 12), folded(BLUE), name="cloth stack"),
             E((12, 15, 5), (20, 19, 12), folded(YELLOW), name="cloth stack")]
    # a bolt on end at the right of the counter, and the top fold of the red stack
    goods += cylinder((24, 15, 8), 2, 12, "y", folded(GREEN), name="bolt of cloth")
    goods.append(E((-11, 21, 6), (-3, 22, 11), folded(RED), name="top fold"))
    # cloths hung from the awning's front edge
    goods += [E((-12, 24, 4), (-4, 34, 5), hanging(RED), name="hung cloth"),
              E((6, 24, 5), (14, 34, 6), hanging(BLUE), name="hung cloth"),
              E((20, 24, 4), (28, 34, 5), hanging(GREEN), name="hung cloth")]
    return _base.variant("market_stall_cloth", goods)
