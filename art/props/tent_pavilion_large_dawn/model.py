"""Commander's Great Pavilion (Dawn) (`tent_pavilion_large_dawn`): the `tent_pavilion_large` pavilion in the Dawn team's teal (UI #5fb3ad, ink #1b4a47) alternating with unbleached linen, slightly weathered. Same geometry, collision and solid cells as the
source (art/props/tent_pavilion_large/model.py, which takes the palette); only the ramps and the weathering differ.
Brief: art/props/tent_pavilion_large_dawn/brief.md.
Build:  python art/props/build_props.py tent_pavilion_large_dawn      Card:  python tools/prop_card.py tent_pavilion_large_dawn
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import make_ramp   # noqa: F401
import tentlib
from tentlib import MUD, PATCH_NEW, PATCH_TAR   # noqa: F401

SRC = tentlib.load_model("tent_pavilion_large")

PAL = SRC.Palette(
    light=tentlib.LINEN,
    dark=tentlib.DAWN,
    rope=make_ramp("9B8D72", 0.16),
    wear=dict(mud=MUD, mud_lo=3, mud_span=4, dirt=3, valance_dirt=38,
              patches=[(45, 19, 5, 18, 26, PATCH_NEW)]))


def build():
    return SRC.build("tent_pavilion_large_dawn", PAL)
