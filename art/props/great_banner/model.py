"""Great Banner (`great_banner`): 3x6x1 cells, anchor cell (1,0,0). Brief: art/props/great_banner/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..96, z 0..16 px (the cloth spreads along x, three cells wide).
Three parts, each stored shifted into -16..32 (a part is at most 3 cells tall):
  great_banner        y 0..48   the stone footing, the lower timber pole with iron bands        shift y +16
  great_banner_top    y 48..96  the upper pole, crossbar with gilt end caps, spear finial       shift y +64
  great_banner_cloth  y 32..80  the swallow-tailed cloth, 2 px thick, hanging against the pole   shift y +48
The cloth has its own part and its own painter (ClothPaint) so a team colour can replace it: it is the only thing
that uses RED. Deep red field, gold trim, a gold sun device (symmetrical, so front and back both read).
Build:  python art/props/build_props.py great_banner      Card:  python tools/prop_card.py great_banner
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, GILT   # noqa: F401

RED = make_ramp("8E1F26")          # neutral house colour for now: deep red cloth

CX = 8                             # pole and cloth centre line (x)
X0, X1 = -6, 22                    # the cloth: 28 px wide
Y0, Y1 = 32, 80                    # 48 px tall, hanging from the crossbar top
Z0, Z1 = 4, 6                      # 2 px thick, hung against the front of the pole (pole front is z 6)
# swallow tail, rows bottom up: (y from, y to, left piece x range, right piece x range)
TAILS = [(32, 36, (-6, 4), (12, 22)), (36, 40, (-6, 6), (10, 22)), (40, 42, (-6, 7), (9, 22))]


def cloth_inside(x, y):
    """Is whole-px cell (x, y) cloth?"""
    if not (X0 <= x < X1 and Y0 <= y < Y1): return False
    for ya, yb, (l0, l1), (r0, r1) in TAILS:
        if ya <= y < yb: return l0 <= x < l1 or r0 <= x < r1
    return True


class ClothPaint:
    """Cloth faces: deep red field with a 1 px gold border all round (the swallow tail included), a few fold lines, and
    a gold sun (disc, ring, rays) over a gold chevron. Painted from world coordinates and symmetrical about x = 8, so the
    back reads the same as the front. Edge faces are gold trim. `ramps` lists every ramp it uses (palette check)."""
    ramps = (RED, GILT)

    def __call__(s, face, P, c, r, w, h):
        x, y = P[0], P[1]
        ix, iy = int(math.floor(x)), int(math.floor(y))
        if face in ("east", "west", "down"):
            return GILT[2 if face == "east" else 1]
        if face == "up":
            return RED[1] if iy >= Y1 - 1 else GILT[2]
        front = face == "north"
        base = 2 if front else 1
        nb = ((ix + 1, iy), (ix - 1, iy), (ix, iy + 1), (ix, iy - 1))
        if any(not cloth_inside(a, b) for a, b in nb):
            return GILT[3 if front else 2]
        du, dv = abs(x - CX), y - 60.5
        d = math.hypot(du, dv)
        lit = (x - CX) + dv > 3                    # light from top left of the front view (higher x)
        # chevron below the sun, point down
        line = 45 + du
        if du <= 7 and line <= y < line + 3:
            return GILT[3 if front else 2]
        # sun: disc, ring, rays (north and sideways only, the chevron sits below)
        if d <= 3.3: return GILT[4 if (front and lit) else 3 if front else 2]
        if 5.2 <= d <= 6.6: return GILT[3 if (front and lit) else 2]
        if 7.8 <= d <= 11.2:
            if du < 1.2 and dv > 0: return GILT[3 if front else 2]            # north ray
            if dv > -1.2 and dv < 1.2 and du > 7: return GILT[3 if front else 2]   # east and west rays
            if abs(du - dv) < 1.2 and dv > 0: return GILT[3 if front else 2]       # north-east/west rays
        # fold lines in the field
        if (ix + 3) % 9 == 0 and 34 < iy < 76: return RED[base - 1]
        return RED[base]


def build():
    base = [
        # stepped stone footing, the lower pole with two iron bands
        E((2, 0, 2), (14, 4, 14), "stone", skip="down", name="footing"),
        E((4, 4, 4), (12, 8, 12), "stone", name="footing step"),
        E((6, 8, 6), (10, 48, 10), "wood_y", skip="up", name="pole low"),
        E((5, 14, 5), (11, 16, 11), "iron", name="band low"),
        E((5, 26, 5), (11, 28, 11), "iron", name="band mid"),
    ]
    top = [
        E((6, 48, 6), (10, 86, 10), "wood_y", skip="down", name="pole high"),
        # crossbar, an iron clamp where it crosses the pole, gilt end caps
        E((-12, 76, 6), (28, 80, 10), "wood_x", name="crossbar"),
        E((5, 75, 6), (11, 81, 10), "iron", name="clamp"),
        E((-14, 75, 5), (-12, 81, 11), "gilt", name="end cap west"), E((28, 75, 5), (30, 81, 11), "gilt", name="end cap east"),
        E((5, 82, 5), (11, 84, 11), "iron", name="band high"),
        # spear finial: socket and a leaf-shaped gilt head
        E((5, 86, 5), (11, 88, 11), "iron", name="socket"),
        E((5, 88, 7), (11, 92, 9), "gilt", name="spear head"),
        E((6, 92, 7), (10, 94, 9), "gilt", name="spear head"),
        E((7, 94, 7), (9, 96, 9), "gilt", name="spear point"),
    ]
    p = ClothPaint()
    cloth = [E((X0, 42, Z0), (X1, Y1, Z1), p, name="cloth body")]
    for ya, yb, (l0, l1), (r0, r1) in TAILS:
        cloth.append(E((l0, ya, Z0), (l1, yb, Z1), p, name="cloth tail"))
        cloth.append(E((r0, ya, Z0), (r1, yb, Z1), p, name="cloth tail"))
    return Prop("great_banner", {"great_banner": base, "great_banner_top": top, "great_banner_cloth": cloth},
                shift={"great_banner": (0, 16, 0), "great_banner_top": (0, 64, 0), "great_banner_cloth": (0, 48, 0)})
