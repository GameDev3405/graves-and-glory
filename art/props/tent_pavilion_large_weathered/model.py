"""Commander's Great Pavilion (Weathered) (`tent_pavilion_large_weathered`): the `tent_pavilion_large` pavilion in dull, sun-faded canvas: faded oxblood and unbleached linen stripes, mud and grime at the hem, a few patches. Same geometry, collision and solid cells as the
source (art/props/tent_pavilion_large/model.py, which takes the palette); only the ramps and the weathering differ.
Brief: art/props/tent_pavilion_large_weathered/brief.md.
Build:  python art/props/build_props.py tent_pavilion_large_weathered      Card:  python tools/prop_card.py tent_pavilion_large_weathered
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import make_ramp   # noqa: F401
import tentlib
from tentlib import MUD, PATCH_NEW, PATCH_TAR   # noqa: F401

SRC = tentlib.load_model("tent_pavilion_large")

DUN = make_ramp("857656", 0.16)                      # a mid-brown patch, visible on the linen stripes

PAL = SRC.Palette(
    light=make_ramp("CBBF9C", 0.15),            # unbleached linen, sun-dulled
    dark=make_ramp("9A8A6E", 0.14),             # sun-bleached khaki-brown: tone on tone with the linen, no circus stripes
    roof_light=make_ramp("CFC5A8", 0.11),       # the roof takes the most sun: paler, flatter
    roof_dark=make_ramp("A99C80", 0.11),
    rope=make_ramp("8E8068", 0.16),
    wear=dict(mud=MUD, mud_lo=5, mud_span=7, dirt=5, valance_dirt=41,
              patches=[(45, 19, 7, 14, 26, PATCH_NEW), (135, 0, 7, 24, 36, DUN), (180, 0, 7, 16, 28, PATCH_NEW)],
              roof_patches=[(90, -26, 7, 40, 52, PATCH_NEW), (45, 16, 7, 55, 66, PATCH_NEW)]))


def build():
    return SRC.build("tent_pavilion_large_weathered", PAL)
