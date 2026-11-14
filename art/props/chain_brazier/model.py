"""Chain Brazier (`chain_brazier`): 1x4x1 cells, anchor cell (0,0,0). Brief: art/props/chain_brazier/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..64, z 0..16 px; everything hangs in y 32..64 (the two lowest
cells are open air, walk-under height), so every part is modelled in the real frame and stored shifted by 32.
A ceiling plate at y 62..64, two interlocked links, a block, a stepped bail arching over the fire, and a deep iron bowl
with a banded belly and a rim hoop. Three parts, the last two full-bright:
  chain_brazier        the iron: plate, chain, bail, bowl
  chain_brazier_coals  the coal bed and heap in the bowl (glow)
  chain_brazier_flame  the static flame, its own part so a live animated flame can replace it later (glow)
Build:  python art/props/build_props.py chain_brazier      Card:  python tools/prop_card.py chain_brazier
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOT   # noqa: F401

FLAME = Mat(HOT, 3, edges=False, lit=False)
FLAME_HOT = Mat(HOT, 4, edges=False, lit=False)
FLAME_EDGE = Mat(HOT, 2, edges=False, lit=False)


def build():
    b = [
        # ceiling plate and the chain: an x-facing link, a z-facing link passing through it
        E((4, 62, 4), (12, 64, 12), "iron", skip="up", name="ceiling plate"),
        E((7, 58, 7), (9, 59, 9), "iron", name="link z foot"),
        E((7, 59, 6), (9, 62, 7), "iron", name="link z bar"), E((7, 59, 9), (9, 62, 10), "iron", name="link z bar"),
        E((6, 55, 7), (7, 60, 9), "iron", name="link x bar"), E((9, 55, 7), (10, 60, 9), "iron", name="link x bar"),
        E((7, 59, 7), (9, 60, 9), "iron", name="link x cap"),
        E((6, 54, 6), (10, 56, 10), "iron", name="bail block"),
        # stepped bail from the rim posts up over the fire
        E((2, 43, 7), (4, 50, 9), "iron", name="bail post west"), E((12, 43, 7), (14, 50, 9), "iron", name="bail post east"),
        E((3, 50, 7), (6, 52, 9), "iron", name="bail step west"), E((10, 50, 7), (13, 52, 9), "iron", name="bail step east"),
        E((5, 52, 7), (8, 54, 9), "iron", name="bail step west"), E((8, 52, 7), (11, 54, 9), "iron", name="bail step east"),
        # the bowl: base, belly, shoulder, a steel-bright band, a rim hoop
        E((5, 33, 5), (11, 34, 11), "iron", skip="down", name="bowl base"),
        E((4, 34, 4), (12, 37, 12), "iron", name="bowl belly"),
        E((3, 37, 3), (13, 41, 13), "iron", name="bowl shoulder"),
        E((2, 38, 2), (14, 40, 14), "steel", name="bowl band"),
        E((2, 41, 2), (14, 43, 4), "iron", name="rim"), E((2, 41, 12), (14, 43, 14), "iron", name="rim"),
        E((2, 41, 4), (4, 43, 12), "iron", name="rim"), E((12, 41, 4), (14, 43, 12), "iron", name="rim"),
    ]
    c = [
        E((4, 41, 4), (12, 43, 12), "coal", skip="down", name="coal bed"),
        E((5, 43, 5), (11, 45, 11), "coal", skip="down", name="coal heap"),
        E((6, 45, 6), (10, 46, 10), "coal", skip="down", name="coal crest"),
    ]
    f = [
        E((6, 46, 6), (10, 50, 10), FLAME, skip="down", name="flame body"),
        E((7, 50, 7), (9, 52, 9), FLAME_HOT, name="flame tip"),
        E((4, 46, 6), (6, 49, 9), FLAME_EDGE, skip="down", name="flame lick west"),
        E((10, 46, 7), (12, 48, 10), FLAME_EDGE, skip="down", name="flame lick east"),
        E((7, 46, 10), (9, 49, 12), FLAME_EDGE, skip="down", name="flame lick back"),
    ]
    shift = {"chain_brazier": (0, 32, 0), "chain_brazier_coals": (0, 32, 0), "chain_brazier_flame": (0, 32, 0)}
    return Prop("chain_brazier", {"chain_brazier": b, "chain_brazier_coals": c, "chain_brazier_flame": f},
                glow=("chain_brazier_coals", "chain_brazier_flame"), shift=shift)
