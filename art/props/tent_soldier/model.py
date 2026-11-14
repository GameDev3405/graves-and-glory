"""Soldier's Tent (`tent_soldier`): 5x4x7 cells, anchor cell (2,0,3). Brief: art/props/tent_soldier/brief.md.

A BIG prop: every element is modelled in the real prop frame (art/props/README.md: model px, 16 = 1 block, origin = min
corner of the anchor cell, x east, y up, z south, FRONT FACES NORTH (-z)). The footprint runs x -32..48, y 0..64,
z -48..64 px. Prop.split cuts the elements into 3-cell regions (parts tent_soldier_<n>); region boundaries fall at
x=8 (the ridge), y=48, z=-16 and z=32. After changing which regions hold elements run
python tools/new_prop.py tent_soldier --sync  so the def lists the same parts.

A ridge soldier's tent: low canvas side walls, a roof that is steep near the ridge and flares shallower at the eaves,
the middle bay sagging a pixel between its seams, a ridge pole, guy ropes to pegs, a closed back gable and a front
door whose flap is rolled up and tied open. Neutral undyed canvas (team colours come later).
Build:  python art/props/build_props.py tent_soldier      Card:  python tools/prop_card.py tent_soldier
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, WOOD, pat_grain   # noqa: F401

SIZE = (5, 4, 7)
CX = 8            # ridge line (x): the centre of the three body cells and the region boundary

CANVAS = make_ramp("BCB08E", 0.20)     # undyed canvas
HEMP = make_ramp("8E7448", 0.20)       # guy rope
GROUND = make_ramp("5E5240", 0.18)     # groundsheet inside
WOOL = make_ramp("6A5B4C", 0.18)       # bedroll blanket


def seams(period, axis):
    """Canvas strips: a stitched seam one tone down every `period` px along `axis` (world aligned)."""
    def p(face, P, c, r, w, h):
        a = P[0] if axis == "x" else P[2]
        if face in ("east", "west") and axis == "x": a = P[2]
        if face in ("north", "south") and axis == "z": a = P[0]
        return -1 if math.floor(a) % period == 0 else 0
    return p


def pat_weave(face, P, c, r, w, h):
    return -1 if h3(*P, 61) < 0.025 else 0


ROOF = Mat(CANVAS, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[2]) % 16 == 0 else 0)
_wall_seam = lambda f, P, c, r, w, h: -1 if math.floor(P[2] if f in ("east", "west") else P[0]) % 16 == 0 else 0
WALL = Mat(CANVAS, 3, _wall_seam)
GABLE = Mat(CANVAS, 3, _wall_seam, edges=False)
POLE = Mat(make_ramp("9C805A"), 2, pat_grain("z"))
INSIDE = Mat(CANVAS, 1, lambda f, P, c, r, w, h: 0, edges=False, lit=False)
FOLD = Mat(CANVAS, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[0]) % 4 == 0 else 0)   # the rolled flap
ROPE = Mat(HEMP, 2, edges=False)
FLOOR = Mat(GROUND, 2, pat_weave, edges=False)
BLANKET = Mat(WOOL, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[0]) % 5 == 0 else 0)


def panel(side, ox, oy, length, angle, z0, z1, name, thick=2, mat=ROOF, faces=None):
    """A roof slab hanging from its outer top corner (ox, oy), pitched down away from the ridge: side +1 east, -1 west."""
    if side > 0:
        return E((ox, oy - thick, z0), (ox + length, oy, z1), mat, rot=("z", -angle, (ox, oy, z0)), name=name, faces=faces)
    return E((ox - length, oy - thick, z0), (ox, oy, z1), mat, rot=("z", angle, (ox, oy, z0)), name=name, faces=faces)


def roof_lower(dx):
    """Underside height of the roof at distance dx from the ridge (steep run, then the flared eave skirt)."""
    if dx <= 16.26: return 48 - dx - 2.83
    return 32 - (dx - 16.0) * math.tan(math.radians(22.5)) - 2.17


def gable_half(y0):
    """Half width of a gable row whose bottom is y0: as wide as the roof underside allows, tucked 1 px under it."""
    dx = 0.0
    while roof_lower(dx + 0.1) >= y0 and dx < 26: dx += 0.1
    return int(dx) - 1


def build():
    els = []
    # ---- roof: three bays along z, the middle one sags 1 px between the end bays -----------------------------------
    bays = [(-32, -16, 0), (-16, 32, -1), (32, 50, 0)]
    for z0, z1, sag in bays:
        for side in (1, -1):
            # steep upper pitch, then the flared eave skirt; the second starts where the first ends (whole px)
            els.append(panel(side, CX, 48 + sag, 23, 45, z0, z1, "roof pitch"))
            els.append(panel(side, CX + side * 16, 32 + sag, 9, 22.5, z0, z1, "roof eave skirt"))
    # ---- ridge pole and rear post ---------------------------------------------------------------------------------
    els.append(E((6, 47, -34), (10, 51, 52), POLE, name="ridge pole"))
    els.append(E((6, 0, 48), (10, 47, 52), "wood_y", skip="down", name="rear post"))
    # ---- side walls (low, vertical) with a rope tab each side per guy -----------------------------------------------
    els.append(E((30, 0, -30), (32, 28, 48), WALL, skip="down", faces={"west": INSIDE}, name="east wall"))
    els.append(E((-16, 0, -30), (-14, 28, 48), WALL, skip="down", faces={"east": INSIDE}, name="west wall"))
    for zc in (-20, 8, 36):
        for side in (1, -1):
            tx = 32 if side > 0 else -18
            els.append(E((tx, 17, zc - 1), (tx + 2, 20, zc + 1), ROPE, name="guy tab"))
            if side > 0:
                els.append(E((34, 18, zc - 1), (54, 19, zc + 1), ROPE, rot=("z", -45, (34, 18, zc)), name="guy rope"))
                els.append(E((47, 0, zc - 1), (49, 3, zc + 1), "wood_y", skip="down", name="peg"))
            else:
                els.append(E((-38, 18, zc - 1), (-18, 19, zc + 1), ROPE, rot=("z", 45, (-18, 18, zc)), name="guy rope"))
                els.append(E((-33, 0, zc - 1), (-31, 3, zc + 1), "wood_y", skip="down", name="peg"))
    # ---- back gable: wall to the eave, then rows following the underside of the roof ------------------------------------
    els.append(E((-14, 0, 46), (30, 28, 48), WALL, skip="down", faces={"north": INSIDE}, name="back wall"))
    y0 = 28
    while y0 < 48:
        hw = gable_half(y0)
        if hw >= 2:
            els.append(E((CX - hw, y0, 46), (CX + hw, y0 + 3, 48), GABLE, faces={"north": INSIDE}, name="back gable"))
        y0 += 3
    # ---- front gable: canvas either side of the door and over it; the door is x -1..17, to y 32 -------------------------
    els.append(E((-14, 0, -30), (-1, 28, -28), WALL, skip="down", name="front wall left"))
    els.append(E((17, 0, -30), (30, 28, -28), WALL, skip="down", name="front wall right"))
    y0 = 28
    while y0 < 48:
        hw = gable_half(y0)
        if hw >= 2:
            if y0 < 31:   # beside the door
                els.append(E((CX - hw, y0, -30), (-1, y0 + 3, -28), GABLE, name="front gable left"))
                els.append(E((17, y0, -30), (CX + hw, y0 + 3, -28), GABLE, name="front gable right"))
            else:
                els.append(E((CX - hw, y0, -30), (CX + hw, y0 + 3, -28), GABLE, name="front gable"))
        y0 += 3
    # door frame, the rolled-up flap and its ties
    els.append(E((-4, 0, -33), (-1, 32, -30), "wood_y", skip="down", name="door post"))
    els.append(E((17, 0, -33), (20, 32, -30), "wood_y", skip="down", name="door post"))
    els.append(E((-1, 27, -32), (17, 31, -28), FOLD, name="rolled flap"))
    for x in (3, 11):
        els.append(E((x, 26, -33), (x + 1, 32, -27), ROPE, name="flap tie"))
    # front guy ropes from the door posts
    for px in (-4, 17):
        els.append(E((px, 15, -34), (px + 3, 18, -29), ROPE, name="guy tab"))
        els.append(E((px + 1, 16, -52), (px + 2, 17, -34), ROPE, rot=("x", -45, (px + 1, 16, -34)), name="guy rope"))
        els.append(E((px, 0, -48), (px + 2, 3, -46), "wood_y", skip="down", name="peg"))
    # rear guy rope from the rear post
    els.append(E((5, 15, 48), (11, 18, 53), ROPE, name="guy tab"))
    els.append(E((8, 16, 53), (9, 17, 70), ROPE, rot=("x", 45, (8, 16, 53)), name="guy rope"))
    els.append(E((7, 0, 64), (9, 3, 66), "wood_y", skip="down", name="peg"))
    # ---- inside: groundsheet and a rolled bedroll against the back wall ---------------------------------------------------
    els.append(E((-14, 0, -28), (30, 1, 46), FLOOR, skip="down", name="groundsheet"))
    els.append(E((-10, 1, 34), (26, 6, 46), BLANKET, name="bedroll"))
    glow = []
    return Prop.split("tent_soldier", els, SIZE, glow=glow)
