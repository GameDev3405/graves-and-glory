"""Armour Stand (`armour_stand`): 1x2x1 cells, anchor cell (0,0,0). Brief: art/props/armour_stand/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..16, y 0..32, z 0..16 px (the crossbar tips poke 1 px out each side).
A timber cross-foot and post carry a full harness: sabatons, greaves with knee cops, cuisses, a mail skirt, a belt, a
plate cuirass with a ridge, pauldrons, hanging vambraces slung over a crossbar, a gorget and a great helm with a
T-slit and a faded plume. Plate is dull iron (the `iron` ramp), steel only on rims and the ridge; mail is a 1 px
staggered check on the same ramp.
Build:  python art/props/build_props.py armour_stand      Card:  python tools/prop_card.py armour_stand
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, IRON, LEATHER   # noqa: F401

PLUME = make_ramp("7A2A24")          # faded red: plume and tabard


def pat_mail(face, P, c, r, w, h):
    """Mail: a 1 px staggered check, one tone down on every other texel; the lit column keeps its tone."""
    x, y, z = P
    a = x if face in ("north", "south", "up", "down") else z
    b = y if face not in ("up", "down") else z
    return -1 if (int(a) + int(b)) % 2 == 0 else 0


MAT["mail"] = Mat(IRON, 1, pat_mail)
MAT["plate"] = Mat(IRON, 2, lambda f, P, c, r, w, h: (1 if f in ("north", "east") and c == 0 and w >= 3 else (-1 if h3(*P, 6) < 0.05 else 0)))
MAT["cloth"] = Mat(PLUME, 2)


def build():
    b = [
        # cross-foot: one beam along x, the other along z split around it; the post between the legs
        E((1, 0, 6), (15, 2, 10), "plank_x", skip="down", name="foot beam"),
        E((6, 0, 1), (10, 2, 6), "plank_z", skip="down", name="foot beam front"),
        E((6, 0, 10), (10, 2, 15), "plank_z", skip="down", name="foot beam back"),
        E((7, 2, 7), (9, 16, 9), "wood_y", name="post"),
        # legs: sabatons, greaves, knee cops (proud, steel), cuisses
        E((3, 2, 3), (7, 4, 10), "plate", skip="down", name="sabaton west"),
        E((9, 2, 3), (13, 4, 10), "plate", skip="down", name="sabaton east"),
        E((4, 4, 6), (7, 8, 10), "plate", name="greave west"), E((9, 4, 6), (12, 8, 10), "plate", name="greave east"),
        E((4, 7, 5), (7, 9, 10), "steel", name="knee cop west"), E((9, 7, 5), (12, 9, 10), "steel", name="knee cop east"),
        E((4, 9, 6), (7, 13, 10), "plate", name="cuisse west"), E((9, 9, 6), (12, 13, 10), "plate", name="cuisse east"),
        # mail skirt, belt, a faded tabard strip hanging over the front
        E((4, 12, 5), (12, 15, 11), "mail", name="mail skirt"),
        E((4, 15, 4), (12, 16, 12), "leather", name="belt"),
        E((7, 15, 3), (9, 16, 4), "gilt", name="buckle"),
        E((6, 8, 4), (10, 15, 6), "cloth", name="tabard"),
        # cuirass with a ridge, pauldrons, gorget
        E((5, 16, 5), (11, 23, 11), "plate", name="cuirass"),
        E((7, 17, 4), (9, 22, 5), "steel", name="chest ridge"),
        E((1, 21, 4), (5, 23, 12), "plate", name="pauldron west"), E((11, 21, 4), (15, 23, 12), "plate", name="pauldron east"),
        E((2, 23, 5), (5, 25, 11), "plate", name="pauldron cap west"), E((11, 23, 5), (14, 25, 11), "plate", name="pauldron cap east"),
        E((6, 23, 6), (10, 25, 10), "steel", name="gorget"),
        # the crossbar the harness hangs on, tips showing past the arms
        E((-1, 18, 7), (17, 20, 9), "wood_x", name="crossbar"),
        # arms hanging at the sides: mail sleeve, elbow cop, vambrace, gauntlet
        E((1, 16, 6), (4, 21, 10), "mail", name="sleeve west"), E((12, 16, 6), (15, 21, 10), "mail", name="sleeve east"),
        E((1, 14, 5), (4, 16, 11), "steel", name="elbow cop west"), E((12, 14, 5), (15, 16, 11), "steel", name="elbow cop east"),
        E((1, 11, 6), (4, 14, 10), "plate", name="vambrace west"), E((12, 11, 6), (15, 14, 10), "plate", name="vambrace east"),
        E((1, 9, 6), (4, 11, 10), "leather", name="gauntlet west"), E((12, 9, 6), (15, 11, 10), "leather", name="gauntlet east"),
        # great helm: a T of slit and bar on the face, domed crown, a plume
        E((5, 25, 5), (11, 29, 11), "plate", name="helm"),
        E((6, 29, 6), (10, 30, 10), "plate", name="crown"),
        E((5, 27, 4), (7, 28, 5), "soot", name="slit west"), E((9, 27, 4), (11, 28, 5), "soot", name="slit east"),
        E((7, 25, 4), (9, 29, 5), "steel", name="nose bar"),
        E((7, 30, 5), (9, 32, 11), "cloth", name="plume"),
    ]
    return Prop("armour_stand", {"armour_stand": b})
