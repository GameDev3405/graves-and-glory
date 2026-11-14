"""Giant's Offering Bowl (`offering_bowl`): 3x3x3 cells, anchor cell (1,0,1). Brief: art/props/offering_bowl/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..48, z -16..32 px. The prop is exactly 3 cells tall, so each
part stays one part stored shifted down 16 px (y -16..32).

A giant's carved stone bowl on a short stem, its rim man-high, a carved band round it and rainwater in it. People
still come: tallow candles on the plinth, on the ground and two on the rim, a few coins, a posy of poppies and tansy.
The candle flames are their own full-bright part.
Build:  python art/props/build_props.py offering_bowl      Card:  python tools/prop_card.py offering_bowl
"""
import os, sys, math
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, h3, make_ramp   # noqa: F401
from giant_bones import octagon
from giant_relics import (stone, octo_ring_y, candle, lift, tone, GSTONE, WAX, COIN, POPPY, TANSY, STEM)

POOL = make_ramp("3E5552", 0.18)   # dark still rainwater, gone green with age


def pat_pool(face, P, c, r, w, h):
    x, y, z = P
    return 1 if face == "up" and h3(math.floor(x / 3), z, 51) < 0.08 else 0


C = (8, 8)          # the bowl's axis (x, z)
RIM = 34            # top of the rim, px


def carved(base):
    """The bowl wall: the stone, plus a carved band between two grooves (y 26 and 31) with a zigzag running round it."""
    def p(face, P, c, r, w, h):
        x, y, z = P
        if face not in ("up", "down"):
            a = x + z
            if 26 <= y < 27 or 31 <= y < 32: return tone(GSTONE, face, -1)
            if 27 <= y < 31 and (math.floor(a) % 6) == (math.floor(y) - 27 if math.floor(a / 6) % 2 == 0 else 3 - (math.floor(y) - 27)) % 6:
                return tone(GSTONE, face, -1)
        return base.pattern(face, P, c, r, w, h)
    m = Mat(GSTONE, 2, p, top=1)
    m.ramps = base.ramps
    return m


def build():
    S = stone(moss_h=4, top_moss=0.4, seed=2)
    T = stone(moss_h=0, top_moss=0.25, seed=3)
    W = carved(stone(moss_h=0, top_moss=0.3, seed=4))
    els = [E((-6, 0, -6), (22, 3, 22), S, skip="down", name="plinth")]
    els += octagon(C, 9, "y", 3, 9, S, name="foot")
    els += octagon(C, 6, "y", 9, 15, T, name="stem")
    els += octagon(C, 14, "y", 15, 19, T, name="bowl underside")
    els += octagon(C, 19, "y", 19, 24, T, name="bowl belly")
    els += octo_ring_y(C[0], C[1], 21, 4, 24, RIM, W, name="bowl wall")
    els += octagon(C, 17, "y", 24, 29, Mat(POOL, 1, pat_pool, edges=False), name="old rainwater")
    glow = []
    # candles: (x, y, z, height) on the plinth, on the ground in front, and two on the rim (north and east walls)
    for x, y, z, hgt in ((-4, 3, -4, 5), (18, 3, -4, 4), (-4, 3, 18, 3), (1, 0, -12, 4), (13, 0, -14, 6), (5, RIM, -12, 3),
                         (26, RIM, 5, 4)):
        wax, flame = candle(x, z, hgt)
        els += lift([wax], y); glow += lift([flame], y)
    # wax run down from the rim candles, and puddled round the ground ones
    els += [E((5, RIM - 3, -14), (7, RIM, -13), WAX, name="wax run"),
            E((0, 0, -13), (4, 1, -9), WAX, skip="down", name="wax puddle"),
            E((12, 0, -15), (16, 1, -12), WAX, skip="down", name="wax puddle")]
    # coins on the plinth's front step
    els += [E((3, 3, -5), (5, 4, -3), COIN, skip="down", name="coin"), E((8, 3, -4), (10, 4, -2), COIN, skip="down", name="coin"),
            E((10, 3, -6), (12, 4, -4), COIN, skip="down", name="coin")]
    # a posy laid on the plinth: stems, poppies and a head of tansy
    st, po, ta = Mat(STEM, 2, edges=False), Mat(POPPY, 2, edges=False), Mat(TANSY, 2, edges=False)
    els += [E((14, 3, -4), (20, 4, -2), st, skip="down", name="posy stems"),
            E((11, 3, -5), (14, 5, -2), po, skip="down", name="poppies"),
            E((12, 4, -2), (14, 6, 0), ta, skip="down", name="tansy"),
            E((-12, 0, -4), (-9, 2, -1), ta, skip="down", name="tansy on the ground")]
    return Prop("offering_bowl", {"offering_bowl": els, "offering_bowl_flames": glow}, glow=("offering_bowl_flames",),
                shift={"offering_bowl": (0, 16, 0), "offering_bowl_flames": (0, 16, 0)})
