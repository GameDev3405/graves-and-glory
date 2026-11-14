"""Soldier's Tent (Large, Weathered) (`tent_soldier_large_weathered`): `tent_soldier_large` in dirty, sun-faded canvas: mud-stained hem, darker grimy lower walls, patches, bleached roof. Same geometry, collision and solid cells as the source
(art/props/tent_soldier_large/model.py, which takes the palette); only the ramps, team trim and weathering differ.
Brief: art/props/tent_soldier_large_weathered/brief.md.
Build:  python art/props/build_props.py tent_soldier_large_weathered      Card:  python tools/prop_card.py tent_soldier_large_weathered
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import make_ramp   # noqa: F401
import tentlib
from tentlib import MUD, PATCH_NEW, PATCH_TAR   # noqa: F401

SRC = tentlib.load_model("tent_soldier_large")

PATCH_PALE = make_ramp("E2D8B6", 0.12)                 # fresh canvas: bright against the dirty wall
PATCH_DARK = make_ramp("65583F", 0.16)                 # tarred
PATCH_MID = make_ramp("8A7A5A", 0.16)                  # on the bleached roof

PAL = SRC.Palette(
    canvas=make_ramp("A99C7B", 0.18),           # wall canvas, dirtied
    roof=make_ramp("CDC4A6", 0.12),             # the roof takes the sun: bleached
    hemp=make_ramp("7E6840", 0.20),
    wear=dict(mud=MUD, mud_lo=4, mud_span=6, dirt_lo=14, dirt_span=7, roof_stain=0.3,
              wall_patches=[("east", 6, 18, 18, 28, PATCH_PALE), ("west", -22, -8, 24, 34, PATCH_DARK), ("south", 22, 32, 14, 24, PATCH_DARK)],
              front_patches=[("north", -38, -28, 22, 32, PATCH_PALE)],
              roof_patches=[(1, 14, 26, 6, 20, PATCH_DARK), (-1, 20, 31, -34, -22, PATCH_MID)]))

def build():
    return SRC.build("tent_soldier_large_weathered", PAL)
