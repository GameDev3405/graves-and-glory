"""Giant's Stone Hand (`stone_hand`): 3x4x2 cells, anchor cell (1,0,0). Brief: art/props/stone_hand/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..64, z 0..32 px. Elements stay inside -16..32 per part;
the fingers part is stored shifted down 32 px (shift={"fingers": (0, 32, 0)}), like the forge hood.

A giant's weathered stone hand rising out of the ground, palm to the front, fingers half-curled, one finger broken.
Build:  python art/props/build_props.py stone_hand      Card:  python tools/prop_card.py stone_hand
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3   # noqa: F401

# Old weathered stone, warmer and paler than the forge's bricks, so it reads as carved bedrock, not masonry.
GSTONE = make_ramp("8A8478", 0.20)
MOSS = make_ramp("5F7A3A", 0.18)
DIRT = make_ramp("6E5038", 0.18)

def pat_weathered(face, P, c, r, w, h):
    """Big flat planes with a few long cracks (world-aligned) and rare chips; no speckle."""
    x, y, z = P
    a = (x if face in ("north", "south", "up", "down") else z)
    # long hairline cracks: a slanted line every ~23 px of the plane, broken by gaps
    k = (a * 0.7 + y) % 23.0
    if k < 1.0 and h3(round(a / 3), round(y / 3), 7) < 0.55: return -1
    if h3(x, y, z, 13) < 0.035: return -1
    return 0

def pat_moss(face, P, c, r, w, h):
    x, y, z = P
    return 1 if h3(x, y, z, 29) < 0.10 else (-1 if h3(x, y, z, 31) < 0.08 else 0)

def pat_dirt(face, P, c, r, w, h):
    x, y, z = P
    return -1 if h3(x, y, z, 17) < 0.12 else 0

STONE = Mat(GSTONE, 2, pat_weathered)
MOSSY = Mat(MOSS, 2, pat_moss, top=0)
EARTH = Mat(DIRT, 2, pat_dirt)


def build():
    m = {"up": MOSSY}          # moss settles on the tops of the old stone
    base = [
        # churned earth and the stone's own rubble around the wrist
        E((-12, 0, 4), (28, 2, 30), EARTH, skip="down", faces={"up": MOSSY}, name="earth mound"),
        E((-6, 2, 8), (24, 4, 28), EARTH, skip="down", faces={"up": MOSSY}, name="earth mound top"),
        E((-14, 0, 22), (-8, 4, 28), STONE, skip="down", faces=m, name="rubble"),
        E((25, 0, 6), (31, 3, 11), STONE, skip="down", faces=m, name="rubble"),
        E((22, 0, 26), (27, 5, 31), STONE, skip="down", faces=m, name="rubble"),
        E((-10, 0, 3), (-6, 2, 7), STONE, skip="down", name="pebble"),
        # the wrist comes up out of the ground, leaning back a little
        E((0, 2, 14), (18, 14, 28), STONE, skip="down", faces=m, name="wrist"),
        E((-2, 10, 12), (20, 16, 27), STONE, faces=m, name="wrist cuff"),
        # palm, faced to the front (north)
        E((-4, 16, 12), (21, 32, 26), STONE, skip="up", name="palm"),
        E((-4, 16, 10), (21, 22, 12), STONE, faces=m, name="heel of the palm"),
        # thumb out to the west, lifted
        E((-10, 18, 14), (-4, 24, 22), STONE, faces=m, name="thumb base"),
        E((-14, 22, 14), (-9, 30, 21), STONE, faces=m, rot=("z", 22.5, (-9, 22, 14)), name="thumb tip"),
    ]
    # fingers: modelled at y 32..64, stored shifted down 32 px by the builder
    Y = 32
    fingers = [E((-4, Y, 12), (21, Y + 4, 26), STONE, skip="down", faces=m, name="knuckle ridge")]
    # (x0, height of first joint, curled tip?) east to west: little, ring, middle, index
    for x0, h1, curl in ((16, 10, True), (10, 14, True), (4, 16, True), (-3, 7, False)):
        fingers.append(E((x0, Y + 4, 14), (x0 + 5, Y + 4 + h1, 21), STONE, faces=m, name="finger"))
        if curl:   # the top joint bends forward, toward the front
            y = Y + 4 + h1
            fingers.append(E((x0, y, 14), (x0 + 5, min(y + 9, 64), 20), STONE, faces=m, rot=("x", -45, (x0, y, 14)), name="finger tip"))
            # knuckle: fills the wedge behind the bent joint so its underside never shows from the back
            fingers.append(E((x0, y, 14), (x0 + 5, y + 5, 20), STONE, skip="down", faces=m, name="knuckle"))
        else:      # the index finger is broken off: a chipped stump
            fingers.append(E((x0 + 1, Y + 4 + h1, 15), (x0 + 4, Y + 5 + h1, 19), STONE, name="broken stump"))
            fingers.append(E((x0 + 2, Y + 5 + h1, 16), (x0 + 4, Y + 7 + h1, 18), STONE, name="broken stump spur"))
    return Prop("stone_hand", {"stone_hand": base, "stone_hand_fingers": fingers}, shift={"stone_hand_fingers": (0, 32, 0)})
