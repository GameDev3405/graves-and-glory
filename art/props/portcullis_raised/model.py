"""Raised Portcullis (`portcullis_raised`): 1x1x1 cells, anchor cell (0,0,0). Brief: art/props/portcullis_raised/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..16, z 0..16 px.
The bottom edge of a gate drawn up into its arch: bar stubs and tips hanging from the top of the cell. No collision.
Shared by portcullis, portcullis_bottom and portcullis_raised: bars 2 px wide at x 1, 5, 9, 13 (z 5..9), one iron
crossbar behind them (z 9..11) at y 6..8 and 14..16 of every cell, so bars and crossbars repeat every 4 px / 8 px across
cell edges, side by side and stacked.
Build:  python art/props/build_props.py portcullis_raised      Card:  python tools/prop_card.py portcullis_raised
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE   # noqa: F401

XS = (1, 5, 9, 13)


def tips(dy):
    """Same tips as portcullis_bottom: bars end at y = dy + 4."""
    out = []
    for x in XS:
        out += [E((x, dy + 2, 6), (x + 2, dy + 4, 8), "steel", skip="up", name="tip"),
                E((x, dy + 1, 7), (x + 1, dy + 2, 8), "steel", skip="up", name="tip point"),
                E((x, dy, 7), (x + 1, dy + 1, 8), "steel", skip="up", name="tip point")]
    return out


def build():
    # portcullis_bottom lifted by 6 px: its bar ends, ferrule bands, tips and the lowest crossbar, cut off at the cell top
    els = [E((x, 10, 5), (x + 2, 16, 9), "iron", skip="up", name="bar stub") for x in XS]
    els += [E((x, 10, 5), (x + 2, 11, 9), "steel", skip="up", name="ferrule band") for x in XS]
    els += tips(6)
    els.append(E((0, 12, 9), (16, 14, 11), "iron", name="crossbar"))
    return Prop("portcullis_raised", {"portcullis_raised": els})
