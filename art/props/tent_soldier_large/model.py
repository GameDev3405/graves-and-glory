"""Large Soldier's Tent (`tent_soldier_large`): 9x6x9 cells, anchor cell (4,0,4). Brief: art/props/tent_soldier_large/brief.md.

The big sibling of `tent_soldier` (same look, kept unchanged as the small tent), sized against the map's block tent
(compare fixture tent_soldier.nbt: a 7x7-block body, 5 blocks high). This one: a 7x7-cell body (112 px), walls 43 px high
so a Knight (35 px) walks in through the 32 px door, ridge at 91 px.

A BIG prop: every element is modelled in the real prop frame (model px, 16 = 1 block, origin = min corner of the anchor
cell, x east, y up, z south, FRONT FACES NORTH (-z)). The footprint runs x -64..80, y 0..96, z -64..80 px. Prop.split cuts
the elements into 3-cell regions (parts tent_soldier_large_<n>); region boundaries fall at x=-16 and 32, y=48, z=-16 and 32.
Rotated roof slabs may not cross an x or y boundary, so each roof side is cut at x=-16 / x=32: the piece ending on the
boundary turns about its END (its unrotated box then stays inside the region), the others about their start.
After changing which regions hold elements run  python tools/new_prop.py tent_soldier_large --sync.
Build:  python art/props/build_props.py tent_soldier_large      Card:  python tools/prop_card.py tent_soldier_large
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, WOOD, pat_grain, cylinder   # noqa: F401

SIZE = (9, 6, 9)
CX = 8            # ridge line (x)
R = 91            # ridge height (outer canvas)
WALL_H = 43       # side walls
DOOR_X0, DOOR_X1 = -8, 24      # door opening, x
DOOR_H = 38

CANVAS = make_ramp("BCB08E", 0.20)     # undyed canvas
HEMP = make_ramp("8E7448", 0.20)       # guy rope
GROUND = make_ramp("5E5240", 0.18)     # groundsheet inside
WOOL = make_ramp("6A5B4C", 0.18)       # bedroll blanket
HAY = make_ramp("B89A44", 0.18)        # hay bale


def pat_weave(face, P, c, r, w, h):
    return -1 if h3(*P, 61) < 0.025 else 0


ROOF = Mat(CANVAS, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[2]) % 16 == 0 else 0)
_wall_seam = lambda f, P, c, r, w, h: -1 if math.floor(P[2] if f in ("east", "west") else P[0]) % 16 == 0 else 0
WALL = Mat(CANVAS, 3, _wall_seam)
GABLE = Mat(CANVAS, 3, _wall_seam, edges=False)
FRONT = Mat(CANVAS, 3, _wall_seam, edges=False)   # the front wall and lintel: no top edge, or it draws a pale seam across the gable
POLE = Mat(make_ramp("9C805A"), 2, pat_grain("z"))
INSIDE = Mat(CANVAS, 1, lambda f, P, c, r, w, h: 0, edges=False, lit=False)
FOLD = Mat(CANVAS, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[0]) % 5 == 0 else 0)   # the rolled flap
ROPE = Mat(HEMP, 2, edges=False)
FLOOR = Mat(GROUND, 2, pat_weave, edges=False)
BLANKET = Mat(WOOL, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[0]) % 5 == 0 else 0)
BALE = Mat(HAY, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[2] if f in ("east", "west") else P[0]) % 3 == 0 else 0, edges=False)


def piece_start(side, ox, oy, length, angle, z0, z1, name, thick=2):
    """A roof slab turning about its outer top START corner (ox, oy); it pitches down away from the ridge."""
    if side > 0:
        return E((ox, oy - thick, z0), (ox + length, oy, z1), ROOF, rot=("z", -angle, (ox, oy, z0)), name=name)
    return E((ox - length, oy - thick, z0), (ox, oy, z1), ROOF, rot=("z", angle, (ox, oy, z0)), name=name)


def piece_end(side, ex, ey, length, angle, z0, z1, name, thick=2):
    """A roof slab turning about its outer top END corner (ex, ey): its unrotated box lies back toward the ridge."""
    if side > 0:
        return E((ex - length, ey - thick, z0), (ex, ey, z1), ROOF, rot=("z", -angle, (ex, ey, z0)), name=name)
    return E((ex, ey - thick, z0), (ex + length, ey, z1), ROOF, rot=("z", angle, (ex, ey, z0)), name=name)


def roof_lower(dx):
    """Underside height of the roof at distance dx from the ridge (steep run, then the flared eave skirt)."""
    if dx <= 40: return R - dx - 2.83
    return (R - 40) - (dx - 40) * math.tan(math.radians(22.5)) - 2.17


def gable_half(y0):
    """Half width of a gable row whose bottom is y0: as wide as the roof underside allows, tucked 1 px under it."""
    dx = 0.0
    while roof_lower(dx + 0.1) >= y0 and dx < 60: dx += 0.1
    return int(dx) - 1


def build():
    els = []
    # ---- roof: three bays along z (cuts at z=-16, 32), the middle one sags 1 px --------------------------------------
    for z0, z1, sag in [(-50, -16, 0), (-16, 32, -1), (32, 66, 0)]:
        e = R + sag
        # east of the ridge: steep slab ending on the x=32 boundary (turns about its end), then the rest, then the skirt
        els.append(piece_end(1, 32, e - 24, 34, 45, z0, z1, "roof pitch"))
        els.append(piece_start(1, 32, e - 24, 23, 45, z0, z1, "roof pitch"))
        els.append(piece_start(1, 48, e - 40, 17, 22.5, z0, z1, "roof eave skirt"))
        # west: mirrored about x=8, boundary x=-16
        els.append(piece_end(-1, -16, e - 24, 34, 45, z0, z1, "roof pitch"))
        els.append(piece_start(-1, -16, e - 24, 23, 45, z0, z1, "roof pitch"))
        els.append(piece_start(-1, -32, e - 40, 17, 22.5, z0, z1, "roof eave skirt"))
    # ---- ridge pole and rear post ------------------------------------------------------------------------------------------
    els.append(E((5, R - 2, -52), (11, R + 3, 68), POLE, name="ridge pole"))
    els.append(E((6, 0, 62), (10, R - 2, 66), "wood_y", skip="down", name="rear post"))
    # ---- side walls and guy ropes ---------------------------------------------------------------------------------------------
    els.append(E((62, 0, -46), (64, WALL_H, 62), WALL, skip="down", faces={"west": INSIDE}, name="east wall"))
    els.append(E((-48, 0, -46), (-46, WALL_H, 62), WALL, skip="down", faces={"east": INSIDE}, name="west wall"))
    for zc in (-34, -10, 14, 38):
        els.append(E((64, 18, zc - 1), (66, 22, zc + 1), ROPE, name="guy tab"))
        els.append(E((60, 5, zc - 1), (80, 6, zc + 1), ROPE, rot=("z", -45, (80, 6, zc)), name="guy rope"))
        els.append(E((78, 0, zc - 1), (80, 6, zc + 1), "wood_y", skip="down", name="peg"))
        els.append(E((-50, 18, zc - 1), (-48, 22, zc + 1), ROPE, name="guy tab"))
        els.append(E((-64, 5, zc - 1), (-44, 6, zc + 1), ROPE, rot=("z", 45, (-64, 6, zc)), name="guy rope"))
        els.append(E((-64, 0, zc - 1), (-62, 6, zc + 1), "wood_y", skip="down", name="peg"))
    # ---- back gable: wall to the eave, then rows following the underside of the roof ----------------------------------------------
    els.append(E((-46, 0, 60), (62, WALL_H, 62), WALL, skip="down", faces={"north": INSIDE}, name="back wall"))
    y0 = WALL_H
    while y0 < R - 3:
        hw = gable_half(y0)
        if hw >= 2:
            els.append(E((CX - hw, y0, 60), (CX + hw, y0 + 4, 62), GABLE, faces={"north": INSIDE}, name="back gable"))
        y0 += 4
    # ---- front gable: pillars either side of the door (x -8..24, 38 high), canvas over it ---------------------------------------------
    els.append(E((-46, 0, -46), (DOOR_X0, WALL_H, -44), FRONT, skip="down", name="front wall left"))
    els.append(E((DOOR_X1, 0, -46), (62, WALL_H, -44), FRONT, skip="down", name="front wall right"))
    els.append(E((DOOR_X0, DOOR_H, -46), (DOOR_X1, WALL_H, -44), FRONT, name="front lintel"))
    y0 = WALL_H
    while y0 < R - 3:
        hw = gable_half(y0)
        if hw >= 2:
            els.append(E((CX - hw, y0, -46), (CX + hw, y0 + 4, -44), GABLE, name="front gable"))
        y0 += 4
    # door posts, the flap rolled up across the top of the door and its two hemp ties
    els.append(E((-12, 0, -50), (DOOR_X0, 42, -46), "wood_y", skip="down", name="door post"))
    els.append(E((DOOR_X1, 0, -50), (28, 42, -46), "wood_y", skip="down", name="door post"))
    els.append(E((DOOR_X0, DOOR_H, -50), (DOOR_X1, DOOR_H + 6, -44), FOLD, name="rolled flap"))
    for x in (2, 12):
        els.append(E((x, DOOR_H - 1, -51), (x + 2, DOOR_H + 7, -43), ROPE, name="flap tie"))
    # front guy ropes from the door posts
    for px in (-12, 24):
        els.append(E((px, 18, -50), (px + 4, 22, -45), ROPE, name="guy tab"))
        els.append(E((px + 1, 5, -64), (px + 3, 6, -44), ROPE, rot=("x", -45, (px + 1, 6, -64)), name="guy rope"))
        els.append(E((px + 1, 0, -64), (px + 3, 6, -62), "wood_y", skip="down", name="peg"))
    # rear guy rope from the rear post
    els.append(E((5, 18, 62), (11, 22, 67), ROPE, name="guy tab"))
    els.append(E((8, 5, 60), (9, 6, 80), ROPE, rot=("x", 45, (8, 6, 80)), name="guy rope"))
    els.append(E((7, 0, 78), (9, 6, 80), "wood_y", skip="down", name="peg"))
    # ---- inside: groundsheet, two bedrolls, hay bales, a chest and a barrel (as the block tent's) ---------------------------------------
    els.append(E((-46, 0, -44), (62, 1, 60), FLOOR, skip="down", name="groundsheet"))
    els.append(E((-44, 1, 14), (-28, 6, 52), BLANKET, name="bedroll"))
    els.append(E((-24, 1, 30), (-10, 6, 56), BLANKET, name="bedroll"))
    els.append(E((38, 1, 44), (60, 9, 58), BALE, name="hay bale"))
    els.append(E((40, 9, 46), (58, 17, 56), BALE, name="hay bale"))
    els.append(E((44, 1, 12), (60, 9, 26), "swood_x", name="chest"))
    els.append(E((44, 9, 12), (60, 11, 26), "wood_x", name="chest lid"))
    els.extend(cylinder((40, 1, 32), 6, 14, "y", "swood_y", name="barrel"))
    glow = []
    return Prop.split("tent_soldier_large", els, SIZE, glow=glow)
