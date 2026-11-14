"""Tool Rack (`tool_rack`): 2x2x1 cells, anchor cell (0,0,0). Brief: art/props/tool_rack/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x 0..32, y 0..32, z 0..16 px. The back (south, z 16) sits flush on a wall.
A plank board with a top rail and a deep foot shelf. Hung on it, seen from the front (higher x is on the viewer's left):
a shovel, a cross-peen hammer, a pair of tongs, a sledge, and a two-man crosscut saw lying on the shelf.
Build:  python art/props/build_props.py tool_rack      Card:  python tools/prop_card.py tool_rack
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, SWOOD, pat_plank   # noqa: F401

MAT["dark_plank"] = Mat(SWOOD, 1, pat_plank("x"))   # stained, darker than the tool hafts so they stand out


def build():
    b = [
        # board, top rail with iron pegs, foot shelf
        E((0, 3, 14), (32, 27, 16), "dark_plank", skip="south", name="back panel"),
        E((0, 27, 12), (32, 30, 16), "swood_x", skip="south", name="top rail"),
        E((0, 1, 10), (32, 3, 16), "swood_x", skip="south", name="foot shelf"),
        E((4, 27, 10), (6, 29, 12), "iron", name="peg"), E((12, 27, 10), (14, 29, 12), "iron", name="peg"),
        E((20, 27, 10), (22, 29, 12), "iron", name="peg"), E((28, 27, 10), (30, 29, 12), "iron", name="peg"),
    ]
    # sledge (x 1..10): ash haft, a heavy iron head with steel strike faces
    b += [E((4, 8, 12), (6, 26, 14), "wood_y", name="sledge haft"),
          E((1, 19, 11), (9, 25, 14), "iron", name="sledge head"),
          E((0, 20, 12), (1, 24, 13), "steel", name="sledge face"), E((9, 20, 12), (10, 24, 13), "steel", name="sledge face"),
          E((3, 8, 11), (7, 10, 14), "leather", name="sledge wrap")]
    # tongs (x 10..16): spread reins, a steel pivot, closed jaws
    b += [E((10, 18, 12), (12, 26, 14), "iron", name="tong rein"), E((14, 18, 12), (16, 26, 14), "iron", name="tong rein"),
          E((11, 15, 12), (15, 18, 14), "steel", name="tong pivot"),
          E((12, 6, 12), (14, 15, 13), "iron", name="tong jaws"), E((11, 6, 12), (12, 9, 13), "iron", name="tong lip"),
          E((14, 6, 12), (15, 9, 13), "iron", name="tong lip")]
    # cross-peen hammer (x 17..26): haft, iron head, steel face and a peen wedge
    b += [E((20, 10, 12), (22, 26, 14), "wood_y", name="hammer haft"),
          E((17, 22, 11), (25, 26, 14), "iron", name="hammer head"),
          E((25, 22, 12), (26, 26, 13), "steel", name="hammer face"), E((16, 23, 12), (17, 25, 13), "iron", name="hammer peen")]
    # shovel (x 26..32): handle with a T grip, steel socket and blade
    b += [E((28, 15, 12), (30, 26, 14), "wood_y", name="shovel handle"),
          E((26, 24, 11), (32, 26, 14), "wood_x", name="shovel grip"),
          E((28, 12, 12), (30, 15, 14), "iron", name="shovel socket"),
          E((26, 7, 12), (32, 12, 13), "steel", name="shovel blade"), E((27, 5, 12), (31, 7, 13), "steel", name="shovel tip")]
    # two-man crosscut saw lying on the shelf: blade, a row of teeth, a handle at each end
    b += [E((3, 4, 13), (22, 8, 14), "steel", name="saw blade")]
    b += [E((3 + 4 * k, 3, 13), (5 + 4 * k, 4, 14), "steel", name="saw tooth") for k in range(4)]
    b += [E((1, 3, 12), (3, 12, 14), "wood_y", name="saw handle"), E((22, 3, 12), (24, 12, 14), "wood_y", name="saw handle")]
    return Prop("tool_rack", {"tool_rack": b})
