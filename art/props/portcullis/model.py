"""Portcullis (`portcullis`): 1x1x1 cells, anchor cell (0,0,0). Brief: art/props/portcullis/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..16, z 0..16 px.
One cell of a plain gate grille: four full-height 2 px iron bars and one crossbar behind them.
Shared by portcullis, portcullis_bottom and portcullis_raised: bars 2 px wide at x 1, 5, 9, 13 (z 5..9), one iron
crossbar behind them (z 9..11) at y 6..8 and 14..16 of every cell, so bars and crossbars repeat every 4 px / 8 px across
cell edges, side by side and stacked.
Build:  python art/props/build_props.py portcullis      Card:  python tools/prop_card.py portcullis
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE   # noqa: F401

XS = (1, 5, 9, 13)


def build():
    els = [E((x, 0, 5), (x + 2, 16, 9), "iron", skip="up down", name="bar") for x in XS]
    els += [E((0, y, 9), (16, y + 2, 11), "iron", name="crossbar") for y in (6, 14)]
    return Prop("portcullis", {"portcullis": els})
