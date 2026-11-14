"""Street Lamp (`street_lamp`): 1x3x1 cells, anchor cell (0,0,0). Brief: art/props/street_lamp/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..48, z 0..16 px. The prop is exactly 3 cells tall, so it stays
one part per layer, each stored shifted down 16 px (stored y -16..32), like the README says for a 3-cell prop.
A medieval lamp-post (rework 2026-11-14, Kort: "heavy rework"): a stained post on a two-step stone footing at the back of
the cell, three iron bands, a capped top with a braced iron crook arm reaching north; a lantern (proplib `lantern`:
corner posts, amber glass on all four faces, pyramid cap, ring) hangs from the arm's hook on a short chain, leaning
toward the viewer. The glass and its lit sill are a glow part (full-bright, slow candle flicker).
Build:  python art/props/build_props.py street_lamp      Card:  python tools/prop_card.py street_lamp
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, Anim, LAMP_FLICKER, lantern   # noqa: F401


def build():
    b = [
        # two-step stone footing under the post (back half of the cell), two weathered corner chips
        E((3, 0, 6), (13, 3, 16), "stone", skip="down", name="footing low"),
        E((4, 3, 7), (12, 5, 15), "stone", skip="down", name="footing high"),
        E((2, 0, 6), (3, 1, 8), "stone", skip="down", name="chip"),
        E((13, 0, 13), (14, 2, 16), "stone", skip="down", name="chip"),
        # stained post with three iron bands, capped
        E((6, 5, 11), (10, 44, 15), "swood_y", skip="down", name="post"),
        E((5, 8, 10), (11, 10, 16), "iron", name="band low"),
        E((5, 17, 10), (11, 19, 16), "iron", name="band mid"),
        E((5, 26, 10), (11, 28, 16), "iron", name="band high"),
        E((5, 44, 10), (11, 46, 16), "iron", name="post cap"),
        E((6, 46, 11), (10, 47, 15), "steel", skip="down", name="cap stud"),
        # the crook: an arm reaching north off the cap, its down-turned hook, a stepped brace from the post
        E((7, 46, 3), (9, 48, 13), "iron", faces={"up": "steel"}, name="arm"),
        E((7, 44, 3), (9, 46, 5), "iron", name="hook"),
        E((7, 40, 9), (9, 42, 11), "iron", name="brace"),
        E((7, 42, 8), (9, 44, 10), "iron", name="brace"),
        E((7, 44, 7), (9, 46, 9), "iron", name="brace"),
        # chain from the hook to the lantern's ring: two links, alternating tones
        E((7, 43, 3), (9, 44, 5), "steel", name="chain link"),
        E((7, 42, 3), (9, 43, 5), "iron", name="chain link"),
        # a steel finial under the lantern
        E((7, 27, 3), (9, 28, 5), "steel", name="finial"),
    ]
    body, glow = lantern(3, 28, -1, 10, 10, 8, crossbar=True)
    return Prop("street_lamp", {"street_lamp": b + body, "street_lamp_glass": glow}, glow=("street_lamp_glass",),
                anim={"street_lamp_glass": Anim(**LAMP_FLICKER)},
                shift={"street_lamp": (0, 16, 0), "street_lamp_glass": (0, 16, 0)})
