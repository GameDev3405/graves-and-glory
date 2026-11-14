"""Wall Weapon Rack (`weapon_rack_wall`): 2x2x1 cells, anchor cell (0,0,0). Brief: art/props/weapon_rack_wall/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..32, y 0..32, z 0..16 px. The back (south, z 16) sits flush on a wall.
The hung twin of the free-standing `weapon_rack` (same four weapons, same timber): a plank back panel, two posts and
three rails, the weapons resting against the rails.
Build:  python art/props/build_props.py weapon_rack_wall      Card:  python tools/prop_card.py weapon_rack_wall
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE   # noqa: F401


def build():
    r = [
        # back panel against the wall, two posts, rails across them
        E((0, 2, 15), (32, 30, 16), "plank_x", skip="south", name="back panel"),
        E((0, 2, 12), (3, 30, 15), "wood_y", name="post"), E((29, 2, 12), (32, 30, 15), "wood_y", name="post"),
        E((0, 24, 11), (32, 27, 15), "swood_x", name="top rail"),
        E((3, 10, 11), (29, 12, 15), "swood_x", name="mid rail"),
        E((3, 2, 11), (29, 3, 13), "swood_x", name="foot rail"),
        # iron pegs through the top rail, nails in the posts
        E((8, 28, 12), (10, 29, 14), "iron", name="peg"), E((22, 28, 12), (24, 29, 14), "iron", name="peg"),
    ]
    # arming sword, point down (Gilded squire)
    r += [E((6, 3, 10), (8, 20, 11), "steel", name="arming sword blade"), E((6, 2, 10), (7, 3, 11), "steel", name="point"),
          E((3, 20, 9), (11, 21, 12), "iron", name="arming sword bar"), E((6, 21, 10), (8, 25, 11), "leather", name="grip"),
          E((5, 25, 9), (9, 27, 12), "iron", name="pommel")]
    # boar spear, head up
    r += [E((12, 2, 10), (13, 26, 11), "wood_y", name="boar spear shaft"),
          E((10, 25, 9), (15, 26, 12), "iron", name="spear lugs"),
          E((11, 26, 10), (14, 30, 11), "steel", name="spear head"), E((12, 30, 10), (13, 32, 11), "steel", name="spear tip")]
    # Ironclad hearth axe, head up
    r += [E((18, 2, 10), (19, 28, 11), "wood_y", name="hearth axe haft"),
          E((19, 21, 9), (21, 27, 12), "iron", name="axe cheek"), E((21, 18, 10), (24, 29, 11), "steel", name="axe blade"),
          E((16, 23, 10), (18, 25, 11), "iron", name="axe spike"), E((18, 28, 10), (19, 29, 11), "iron", name="cap")]
    # Gilded longsword, point down, gilt guard
    r += [E((24, 3, 10), (26, 22, 11), "steel", name="longsword blade"), E((24, 2, 10), (25, 3, 11), "steel", name="point"),
          E((21, 22, 9), (29, 23, 12), "gilt", name="longsword guard"), E((24, 23, 10), (26, 28, 11), "leather", name="grip"),
          E((23, 28, 9), (27, 30, 12), "gilt", name="pommel")]
    return Prop("weapon_rack_wall", {"weapon_rack_wall": r})
