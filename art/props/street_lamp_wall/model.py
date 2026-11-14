"""Wall Lamp (`street_lamp_wall`): 1x1x1 cells, anchor cell (0,0,0). Brief: art/props/street_lamp_wall/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..16, z 0..16 px. The back (south, z 16) sits flush on a wall.
Rework 2026-11-14 (Kort: "heavy rework"): three parts that read apart from the side. A riveted iron wall plate with a
boss; a braced iron arm reaching north; a two-link chain from the arm's tip to the lantern (proplib `lantern`, the same
anatomy as the street lamp's: corner posts, amber glass on all four faces, pyramid cap, ring). The glass and its lit sill
are a glow part (full-bright, slow candle flicker on its own order so it never blinks in step with the street lamps).
Build:  python art/props/build_props.py street_lamp_wall      Card:  python tools/prop_card.py street_lamp_wall
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, Anim, LAMP_FLICKER_WALL, lantern   # noqa: F401


def build():
    b = [
        # steel wall plate (never seen from behind; lighter than the lantern iron so the bracket separates), four dark 2x2 rivets, a boss where the arm grows out
        E((2, 3, 15), (14, 16, 16), "steel", skip="south", name="wall plate"),
        E((3, 4, 14), (5, 6, 15), "iron", skip="south", name="rivet"), E((11, 4, 14), (13, 6, 15), "iron", skip="south", name="rivet"),
        E((3, 13, 14), (5, 15, 15), "iron", skip="south", name="rivet"), E((11, 13, 14), (13, 15, 15), "iron", skip="south", name="rivet"),
        E((6, 11, 14), (10, 16, 15), "steel", skip="south", name="boss"),
        # the steel arm and its 45-degree stepped brace down to the plate
        E((7, 14, 5), (9, 16, 14), "steel", name="arm"),
        E((7, 7, 13), (9, 10, 15), "steel", name="brace"),
        E((7, 10, 11), (9, 12, 13), "steel", name="brace"),
        E((7, 12, 9), (9, 14, 11), "steel", name="brace"),
        # two chain links from the arm tip down to the lantern's ring, alternating tones
        E((7, 13, 5), (9, 14, 7), "steel", name="chain link"),
        E((7, 12, 5), (9, 13, 7), "iron", name="chain link"),
    ]
    body, glow = lantern(3, 0, 1, 10, 10, 6, crossbar=False)
    return Prop("street_lamp_wall", {"street_lamp_wall": b + body, "street_lamp_wall_glass": glow}, glow=("street_lamp_wall_glass",),
                anim={"street_lamp_wall_glass": Anim(**LAMP_FLICKER_WALL)})
