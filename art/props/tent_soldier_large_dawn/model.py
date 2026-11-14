"""Soldier's Tent (Large, Dawn) (`tent_soldier_large_dawn`): `tent_soldier_large` in linen canvas with the Dawn team's teal (UI #5fb3ad, ink #1b4a47) as two roof stripes, eave trim and the door flap, lightly weathered. Same geometry, collision and solid cells as the source
(art/props/tent_soldier_large/model.py, which takes the palette); only the ramps, team trim and weathering differ.
Brief: art/props/tent_soldier_large_dawn/brief.md.
Build:  python art/props/build_props.py tent_soldier_large_dawn      Card:  python tools/prop_card.py tent_soldier_large_dawn
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import make_ramp   # noqa: F401
import tentlib
from tentlib import MUD, PATCH_NEW, PATCH_TAR   # noqa: F401

SRC = tentlib.load_model("tent_soldier_large")

PAL = SRC.Palette(canvas=tentlib.LINEN, roof=make_ramp("D0C5A2", 0.12), team=tentlib.DAWN, stripes=(-3, 3), hemp=make_ramp("8E7448", 0.20),
                  wear=dict(mud=MUD, mud_lo=3, mud_span=3, dirt_lo=8, dirt_span=4, roof_stain=0.2,
                            wall_patches=[("west", -22, -8, 20, 30, PATCH_NEW)]))

def build():
    return SRC.build("tent_soldier_large_dawn", PAL)
