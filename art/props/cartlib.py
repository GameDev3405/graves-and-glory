"""Shared code for the cart props (supply_wagon*, handcart*): the base model and the loads. Each prop's model.py
calls wagon_prop(id, load) or cart_prop(id, load). All coordinates are the prop frame (px, x east, y up, z south,
FRONT FACES NORTH). Nothing here is a prop of its own (the build only looks at art/props/<id>/model.py).

Supply wagon, 3x3x6 cells, anchor cell (1,0,2): footprint x -16..32, y 0..48, z -32..64.
  body z 0..64, shafts (thills) run north to z -32. Bed floor y 13..16, side boards to y 28, wheels outside the bed.
  Wheels turn about x: rear radius 12 at z 50, front radius 10 at z 6 (rotated rim pieces must not cross the z=16 part
  boundary: the front wheel's rim ends exactly at z=16).
Handcart, 2x2x3 cells, anchor cell (0,0,1): footprint x 0..32, y 0..32, z -16..32. Handles run north.
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, wheel, cylinder, blob, WOOD, LINEN, LEATHER, IRON, STONE, STEEL  # noqa: F401
from proplib import pat_grain

WAGON_SIZE = (3, 3, 6)
CART_SIZE = (2, 2, 3)

# ---- shared materials ------------------------------------------------------------------------------------------
CANVAS = make_ramp("B5A88A", 0.14)        # weathered, undyed tilt canvas


def pat_canvas(face, P, c, r, w, h):
    """Plain canvas: a seam stripe (a tone down) every 4 px along the tilt's length, nothing else."""
    x, y, z = P
    a = y if face in ("east", "west") else x
    return -1 if int(math.floor(a)) % 4 == 0 else 0


MAT["canvas"] = Mat(CANVAS, 2, pat_canvas, edges=False)
MAT["dark_iron"] = Mat(make_ramp("34343C", 0.2), 2, lambda f, P, c, r, w, h: 0)
MAT["wheel_wood"] = MAT["wood_z"]


def rope_band(face):
    return LINEN[0] if face in ("south", "west", "down") else LINEN[1]


# ---- the wagon ------------------------------------------------------------------------------------------------------
def wagon_base():
    els = []
    # wheels: rear radius 12 (8 spokes), front radius 10 (4 spokes), iron hubs; left and right of the bed
    for x, side in ((-12.5, "left"), (28.5, "right")):
        els += wheel((x, 12, 50), 12, 3, "x", "wood_z", spokes=8, hub_mat="iron", name="rear wheel " + side)
        els += wheel((x, 10, 6), 10, 3, "x", "wood_z", spokes=4, hub_mat="iron", name="front wheel " + side)
    els += [
        # axles and the front bolster
        E((-10, 10, 48), (26, 13, 52), "wood_x", skip="up", name="rear axle"),
        E((-10, 8, 4), (26, 13, 8), "wood_x", skip="up", name="front axle"),
        # bed: dark floor rail, then the planked side boards, front and tail boards
        E((-10, 13, 0), (26, 16, 64), "swood_z", skip="down", name="bed floor"),
        E((-10, 16, 0), (-7, 28, 64), "plank_z", name="left side board"),
        E((23, 16, 0), (26, 28, 64), "plank_z", name="right side board"),
        E((-7, 16, 0), (23, 28, 3), "plank_x", name="front board"),
        E((-7, 16, 61), (23, 28, 64), "plank_x", name="tail board"),
        E((-11, 28, 0), (-6, 30, 64), "wood_z", name="left cap rail"),
        E((22, 28, 0), (27, 30, 64), "wood_z", name="right cap rail"),
        # iron straps round the side boards
        E((-11, 13, 10), (-10, 28, 12), "iron", name="strap"), E((-11, 13, 30), (-10, 28, 32), "iron", name="strap"),
        E((-11, 13, 50), (-10, 28, 52), "iron", name="strap"),
        E((26, 13, 10), (27, 28, 12), "iron", name="strap"), E((26, 13, 30), (27, 28, 32), "iron", name="strap"),
        E((26, 13, 50), (27, 28, 52), "iron", name="strap"),
        # driver's bench across the cap rails
        E((-11, 30, 4), (27, 33, 12), "swood_x", name="seat"),
        # shafts running north from the front board, a dark iron ferrule on each end, and two legs to hold them up
        E((0, 17, -30), (3, 20, 3), "wood_z", name="shaft"), E((13, 17, -30), (16, 20, 3), "wood_z", name="shaft"),
        E((0, 17, -32), (3, 20, -30), "dark_iron", name="shaft ferrule"), E((13, 17, -32), (16, 20, -30), "dark_iron", name="shaft ferrule"),
        E((0, 0, -22), (3, 17, -19), "wood_y", skip="down", name="shaft leg"), E((13, 0, -22), (16, 17, -19), "wood_y", skip="down", name="shaft leg"),
    ]
    return els


def wagon_prop(pid, load):
    return Prop.split(pid, wagon_base() + list(load), WAGON_SIZE)


# ---- wagon loads (bed interior x -7..23, z 3..61, floor top y 16, side boards to y 28) -------------------------------
HAY = make_ramp("C9A84C", 0.18)


def hay_mat(z0):
    """Hay bale with straw lines along its length and two rope bands per bale (bales 15 px long starting at z0)."""
    g = pat_grain("z")
    def p(face, P, c, r, w, h):
        x, y, z = P
        if face in ("up", "east", "west", "down") and int((math.floor(z) - z0) % 15) in (4, 5, 10, 11):
            return rope_band(face)
        if face in ("north", "south"):
            return -1 if h3(x, y, 3) < 0.2 else 0
        return g(face, P, c, r, w, h)
    return Mat(HAY, 2, p)


def load_hay():
    els = []
    # layer 1: six bales, two across and three along (a 1 px gap at the ends)
    for ix, x in enumerate((-7, 8)):
        for z in (15, 30, 45):
            els.append(E((x, 16, z), (x + 15, 26, z + 15), hay_mat(z), skip="" , name="hay bale"))
    # layer 2: four bales, shifted half a bale along the bed
    for x in (-7, 8):
        for z in (22, 37):
            els.append(E((x, 26, z), (x + 15, 36, z + 15), hay_mat(z), name="hay bale"))
    # layer 3: two bales on top
    for x in (-7, 8):
        els.append(E((x, 36, 29), (x + 15, 45, 44), hay_mat(29), name="hay bale"))
    return els


def barrel_mat(cx, cz, y0, hoops=((2, 3), (12, 13))):
    """Standing barrel painted by the cylinder it is cut from: stave lines, two iron hoops, an iron-rimmed lid."""
    def p(face, P, c, r, w, h):
        x, y, z = P
        if face == "up":
            d = math.hypot(x - cx, z - cz)
            if d >= 5.8: return IRON[2]
            return 1 if int(math.floor(x)) % 4 == 0 else 0
        a = x if face in ("north", "south") else z
        ly = int(math.floor(y)) - y0
        if ly in hoops[0] or ly in hoops[1]: return IRON[1] if face in ("north", "east") else IRON[0]
        if ly in (0, 15): return -1
        return -1 if int(math.floor(a * 1.0)) % 3 == 0 else 0
    return Mat(WOOD, 2, p)


def load_barrels():
    els = []
    rows = (22.5, 37.5, 52.5)
    for cx in (0.5, 15.5):
        for cz in rows:
            els += cylinder((cx, 16, cz), 7, 16, "y", barrel_mat(cx, cz, 16), name="barrel", skip="down")
    # one keg lying along the bed on top of the four middle barrels' gap
    els += cylinder((8, 38, 24), 6, 24, "z", keg_mat(), name="keg", skip="")
    return els


def keg_mat():
    def p(face, P, c, r, w, h):
        x, y, z = P
        if face in ("north", "south"):
            d = math.hypot(x - 8, y - 38)
            return IRON[2] if d >= 4.8 else (1 if int(math.floor(y)) % 4 == 0 else 0)
        zz = int(math.floor(z)) - 24
        if zz in (2, 3) or zz in (20, 21): return IRON[1] if face in ("up", "east") else IRON[0]
        a = x if face in ("up", "down") else y
        return -1 if int(math.floor(a)) % 3 == 0 else 0
    return Mat(WOOD, 2, p)


GSHOT = make_ramp("767370", 0.2)
BALL = make_ramp("4A4D56", 0.2)


def pat_boulder(face, P, c, r, w, h):
    x, y, z = P
    if h3(x, y, z, 7) < 0.05: return -1
    return 1 if h3(x, z, 3) < 0.05 else 0


STONEBALL = make_ramp("A9A5A0", 0.2)


def ball(x, y, z, mat):
    """A 6 px octagonal ball in stepped slabs: a 4x2x4 cap above and below, and a belly whose footprint is an octagon
    (a 6x4 bar plus two 4x1 ends), so no face or corner reads as a cube from any side."""
    return [E((x + 1, y, z + 1), (x + 5, y + 2, z + 5), mat, name="cannonball"),
            E((x, y + 2, z + 1), (x + 6, y + 4, z + 5), mat, name="cannonball"),
            E((x + 1, y + 2, z), (x + 5, y + 4, z + 1), mat, name="cannonball"),
            E((x + 1, y + 2, z + 5), (x + 5, y + 4, z + 6), mat, name="cannonball"),
            E((x + 1, y + 4, z + 1), (x + 5, y + 6, z + 5), mat, name="cannonball")]


def load_shot():
    els = []
    stone = Mat(GSHOT, 2, pat_boulder, top=1)
    light = Mat(STONEBALL, 2, None)
    dark = Mat(BALL, 2, None)
    # a ballast bed of rubble lifts the shot clear of the side boards
    els.append(E((-7, 16, 15), (23, 24, 61), stone, skip="down", name="rubble bed"))
    # three big trebuchet stones in a row down the left side
    for i, z in enumerate((15, 30, 45)):
        els += blob((-7, 24, z), (5, 38 - 3 * (i % 2), z + 14), stone, layers=2, step=3, seed=i + 1, name="trebuchet stone")
    # cannonball piles on the right, light stone and dark iron balls alternating: 3x2 + 2x1 stacked, and 2 + 1
    def ball_at(i, j, lvl, x0, z0, off):
        return ball(x0 + off + i * 6, 24 + lvl * 6, z0 + off + j * 6, light if (i + j + lvl) % 2 == 0 else dark)
    for i in range(3):
        for j in range(2): els += ball_at(i, j, 0, 5, 15, 0)
    for i in range(2): els += ball_at(i, 0, 1, 5, 15, 3)
    for i in range(2): els += ball_at(i, 0, 0, 5, 34, 0)
    els += ball_at(0, 0, 1, 5, 34, 3)
    return els


def load_covered():
    els = []
    # stepped canvas tilt over the bed from z 14 to 62, with a wooden hoop (rib) round it at four places
    tiers = [(30, 37, -9, 25), (37, 42, -6, 22), (42, 45, -2, 18), (45, 47, 3, 13)]
    for y0, y1, xl, xr in tiers:
        els.append(E((xl, y0, 14), (xr, y1, 62), "canvas", name="canvas tilt"))
    rib = [(30, 38, -10, 26), (37, 43, -7, 23), (42, 46, -3, 19), (45, 48, 2, 14)]
    for z in (15, 29, 43, 57):
        for y0, y1, xl, xr in rib:
            els.append(E((xl, y0, z), (xr, y1, z + 2), "swood_z", name="hoop"))
    return els


# ---- the handcart ---------------------------------------------------------------------------------------------------
def cart_base():
    els = []
    els += wheel((2.5, 10, 12), 10, 3, "x", "wood_z", spokes=4, hub_mat="iron", name="wheel left")
    els += wheel((29.5, 10, 12), 10, 3, "x", "wood_z", spokes=4, hub_mat="iron", name="wheel right")
    els += [
        E((5, 8, 10), (27, 12, 14), "wood_x", skip="up", name="axle"),
        E((5, 12, 0), (27, 14, 30), "swood_z", skip="down", name="floor"),
        E((5, 14, 0), (7, 22, 30), "plank_z", name="left side board"),
        E((25, 14, 0), (27, 22, 30), "plank_z", name="right side board"),
        E((7, 14, 0), (25, 22, 2), "plank_x", name="front board"),
        E((7, 14, 28), (25, 22, 30), "plank_x", name="tail board"),
        E((4, 22, 0), (8, 23, 30), "wood_z", name="left cap rail"),
        E((24, 22, 0), (28, 23, 30), "wood_z", name="right cap rail"),
        # handles run north from the front board; a pull bar joins their ends
        E((8, 14, -13), (11, 17, 2), "wood_z", name="handle"), E((21, 14, -13), (24, 17, 2), "wood_z", name="handle"),
        E((6, 14, -16), (26, 17, -13), "wood_x", name="grip bar"),
        # two rear legs hold it level when it stands
        E((8, 0, 24), (11, 12, 27), "wood_y", skip="down", name="leg"), E((21, 0, 24), (24, 12, 27), "wood_y", skip="down", name="leg"),
        # iron straps round the boards
        E((4, 12, 6), (5, 22, 8), "iron", name="strap"), E((4, 12, 22), (5, 22, 24), "iron", name="strap"),
        E((27, 12, 6), (28, 22, 8), "iron", name="strap"), E((27, 12, 22), (28, 22, 24), "iron", name="strap"),
    ]
    return els


def cart_prop(pid, load):
    return Prop(pid, {pid: cart_base() + list(load)})


ORE = make_ramp("6D7280", 0.2)
COPPER = make_ramp("C47A3C", 0.2)


def ore_mat():
    def p(face, P, c, r, w, h):
        x, y, z = P
        k = h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 9)
        if k < 0.12: return COPPER[3 if face != "up" else 4]
        if k < 0.2: return COPPER[2]
        return -1 if h3(x, y, z, 5) < 0.1 else 0
    m = Mat(ORE, 2, p)
    m.ramps = (COPPER,)
    return m


def load_ore():
    m = ore_mat()
    els = blob((7, 14, 3), (25, 28, 27), m, layers=3, step=3, seed=2, name="ore heap")
    els += blob((9, 25, 6), (19, 32, 16), m, layers=2, step=3, seed=5, name="ore lump")
    els += blob((13, 24, 17), (23, 31, 26), m, layers=2, step=3, seed=8, name="ore lump")
    return els


BURLAP = make_ramp("B79B6B", 0.16)


def pat_burlap(face, P, c, r, w, h):
    x, y, z = P
    return -1 if (int(math.floor(x)) + int(math.floor(y)) + int(math.floor(z))) % 3 == 0 else 0


def load_sacks():
    els = []
    sack = Mat(BURLAP, 2, pat_burlap)

    def one(x, z, tall):
        top = 14 + tall
        return [E((x, 14, z), (x + 9, top, z + 8), sack, name="sack"),
                E((x + 1, top, z + 1), (x + 8, top + 3, z + 7), sack, name="sack shoulder"),
                E((x + 3, top + 3, z + 3), (x + 6, top + 5, z + 5), "leather", name="sack tie")]
    # six upright sacks, two across and three along, standing a hand above the side boards
    for i, x in enumerate((7, 16)):
        for j, z in enumerate((3, 11, 19)):
            els += one(x, z, 11 if (i + j) % 2 else 10)
    return els
