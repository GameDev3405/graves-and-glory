"""Builds the smithy prop kit: art/props/<id>/<part>.json (Java item models), <id>.png (one sheet a prop) and
<id>.bbmodel (Blockbench project of all parts). Frame and rules: art/props/README.md.

    python art/props/build_props.py [id ...]
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from proplib import E, Prop, Mat, MAT, HOUSE, IRON, STEEL, GILT, LEATHER, WOOD, GRIT, h3, write

HERE = os.path.dirname(os.path.abspath(__file__))

# ---- forge: 3x4x2 cells, pivot cell (1,0,0) -> frame x -16..32, y 0..64, z 0..32 ---------------------------------
def bellows_pleats(face, P, c, r, w, h):
    """Leather bellows: pleat lines across the long axis (z), one tone down, the ridge between a tone up."""
    x, y, z = P
    if face in ("east", "west", "up", "down"):
        k = math.floor(z) % 3
        return -1 if k == 0 else (1 if k == 1 and face != "down" else 0)
    return -1 if r % 2 else 0

def forge():
    hearth = [
        E((-8, 0, 2), (24, 14, 32), "stone", faces={"north": "soot"}, name="hearth base"),
        E((-8, 0, 0), (2, 14, 2), "stone", skip="south", name="front left"),
        E((14, 0, 0), (24, 14, 2), "stone", skip="south", name="front right"),
        E((2, 6, 0), (14, 14, 2), "stone", skip="south", name="ash pit lintel"),
        E((-9, 14, -1), (25, 16, 32), "stone_top", name="hearth coping"),
        E((-8, 16, 0), (-4, 30, 20), "stone", faces={"east": "hotwall"}, name="west cheek"),
        E((20, 16, 0), (24, 30, 20), "stone", faces={"west": "hotwall"}, name="east cheek"),
        E((-8, 16, 20), (24, 32, 32), "stone", faces={"north": "hotwall"}, name="chimney breast"),
        E((-4, 16, 0), (20, 17, 3), "stone_top", name="coal curb"),
        E((25, 4, 4), (26, 27, 27), "swood_z", name="tool board"),
        # bellows on the west, its nozzle into the cheek
        E((-15, 0, 7), (-13, 9, 9), "wood_y", name="bellows leg"), E((-11, 0, 7), (-9, 9, 9), "wood_y", name="bellows leg"),
        E((-15, 0, 23), (-13, 9, 25), "wood_y", name="bellows leg"), E((-11, 0, 23), (-9, 9, 25), "wood_y", name="bellows leg"),
        E((-16, 9, 5), (-8, 10, 27), "plank_z", name="bellows board low"),
        E((-15, 10, 7), (-9, 14, 25), Mat(LEATHER, 2, bellows_pleats), name="bellows leather"),
        E((-16, 14, 5), (-8, 15, 27), "plank_z", name="bellows board top"),
        E((-15, 10, 3), (-9, 14, 7), Mat(LEATHER, 1, bellows_pleats), name="bellows snout"),
        E((-13, 11, 1), (-10, 13, 3), "iron", name="nozzle"),
        E((-10, 11, 0), (-8, 13, 2), "iron", name="tuyere"),
        E((-13, 15, 25), (-11, 28, 27), "wood_y", name="bellows lever post"),
        E((-15, 26, 18), (-9, 28, 30), "wood_z", name="bellows lever"),
        E((-14, 15, 19), (-13, 26, 20), "linen", name="bellows chain"),
        # tool rack on the east
        E((26, 0, 1), (30, 30, 4), "wood_y", name="rack post front"),
        E((26, 0, 27), (30, 30, 30), "wood_y", name="rack post back"),
        E((26, 26, 4), (30, 28, 27), "wood_z", name="rack rail"),
        E((27, 25, 9), (29, 26, 10), "iron", name="hook"), E((27, 25, 15), (29, 26, 16), "iron", name="hook"),
        E((27, 25, 21), (29, 26, 22), "iron", name="hook"),
        # tongs
        E((27, 7, 8), (29, 25, 9), "iron", name="tongs arm"), E((27, 7, 10), (29, 25, 11), "iron", name="tongs arm"),
        E((27, 4, 7), (29, 7, 9), "iron", name="tongs jaw"), E((27, 4, 10), (29, 7, 12), "iron", name="tongs jaw"),
        E((26, 14, 8), (29, 15, 11), "iron", name="tongs rivet"),
        # hammer
        E((27, 11, 15), (29, 25, 17), "wood_y", name="hammer haft"),
        E((26, 8, 13), (29, 11, 19), "anvil", name="hammer head"),
        # fire rake
        E((27, 5, 21), (29, 25, 22), "iron", name="rake rod"),
        E((26, 3, 20), (29, 5, 25), "iron", name="rake head"),
    ]
    lumps = [(-3, 4, 4), (1, 5, 3), (5, 4, 5), (10, 6, 3), (14, 4, 4), (17, 5, 2), (-1, 10, 5), (4, 11, 4), (9, 10, 6),
             (15, 11, 3), (0, 15, 4), (7, 15, 3), (12, 15, 5)]
    coals = [E((-4, 16, 3), (20, 19, 20), "coal", skip="down", name="coal bed"),
             E((-3, 17, 1), (19, 19, 3), "coal", skip="down", name="coal bank front")]
    for i, (x, z, w) in enumerate(lumps):
        hgt = 1 + (i % 3 == 0)
        coals.append(E((x, 19, z), (x + w, 19 + hgt, z + min(w, 4)), "coal", skip="down", name="coal lump"))
    hood = [
        E((-9, 30, -1), (25, 32, 20), "iron", name="hood lintel"),
        E((-8, 32, 0), (24, 38, 32), "stone", faces={"down": "soot"}, name="hood tier 1"),
        E((-5, 38, 3), (21, 39, 32), "iron", name="hood band"),
        E((-4, 39, 4), (20, 44, 32), "stone", name="hood tier 2"),
        E((-1, 44, 9), (17, 45, 32), "iron", name="hood band 2"),
        E((0, 45, 10), (16, 49, 32), "stone", name="hood tier 3"),
        E((2, 49, 18), (14, 60, 30), "stone", name="chimney"),
        E((1, 60, 17), (15, 63, 31), "stone_top", faces={"up": "soot"}, name="chimney cap"),
    ]
    return Prop("forge", {"hearth": hearth, "hood": hood, "coals": coals}, glow=("coals",),
                shift={"hood": (0, 32, 0)})

# ---- anvil on a stump: 1x1x1, may overhang 2 px ------------------------------------------------------------------
def anvil():
    a = [
        E((2, 0, 2), (14, 7, 14), "stump", faces={"up": "endgrain"}, name="stump"),
        E((1, 0, 4), (2, 2, 9), "stump", skip="east", name="root"), E((14, 0, 7), (15, 2, 12), "stump", skip="west", name="root"),
        E((5, 0, 1), (10, 2, 2), "stump", skip="south", name="root"), E((7, 0, 14), (12, 1, 15), "stump", skip="north", name="root"),
        E((3, 7, 4), (13, 9, 12), "iron", name="anvil foot"),
        E((5, 9, 5), (11, 11, 11), "iron", name="anvil waist"),
        E((3, 11, 5), (13, 12, 11), "anvil", name="anvil shoulder"),
        E((0, 12, 4), (14, 16, 12), "anvil", faces={"up": "steel"}, name="anvil face"),
        E((14, 13, 5), (16, 16, 11), "anvil", faces={"up": "steel"}, name="horn"),
        E((16, 14, 6), (18, 16, 10), "anvil", faces={"up": "steel"}, name="horn tip"),
        E((-2, 14, 5), (0, 16, 11), "anvil", faces={"up": "steel"}, name="heel"),
        E((3, 16, 7), (7, 18, 10), "iron", name="hammer head"),
        E((7, 16, 8), (14, 17, 9), "wood_x", name="hammer haft"),
    ]
    return Prop("anvil", {"anvil": a})

# ---- grindstone bench: 2x2x1, pivot cell 0 -> frame x 0..32, z 0..16 ----------------------------------------------
def wheel_face(face, P, c, r, w, h):
    """Grindstone: on the flats a darker worn ring at the rim and a ring round the hub; the tread a tone up."""
    x, y, z = P
    if face in ("north", "south"):
        d = math.hypot(x - 21, y - 23)
        if d > 6.0: return -1
        if 3.0 < d < 4.0: return -1
        return 1 if (d < 6.0 and d > 5.0 and y > 23 and x > 21) else 0
    if face == "up": return 1
    return -1 if h3(x, y, z, 33) < 0.10 else 0

WHEEL = Mat(GRIT, 2, wheel_face)

def grindstone_bench():
    g = [
        E((1, 0, 3), (3, 12, 5), "wood_y", name="leg"), E((29, 0, 3), (31, 12, 5), "wood_y", name="leg"),
        E((1, 0, 11), (3, 12, 13), "wood_y", name="leg"), E((29, 0, 11), (31, 12, 13), "wood_y", name="leg"),
        E((1, 4, 5), (3, 6, 11), "wood_z", name="stretcher"), E((29, 4, 5), (31, 6, 11), "wood_z", name="stretcher"),
        E((3, 4, 7), (29, 6, 9), "wood_x", name="stretcher long"),
        E((0, 12, 2), (32, 15, 14), "plank_x", name="bench top"),
        E((13, 15, 3), (29, 18, 13), "plank_x", faces={"up": "water"}, name="wheel tub"),
        E((20, 15, 1), (22, 24, 3), "wood_y", name="upright front"), E((20, 15, 13), (22, 24, 15), "wood_y", name="upright back"),
        E((20, 22, 3), (22, 24, 13), "iron", name="axle"),
        E((21, 22, 0), (22, 23, 1), "iron", name="crank pin"),
        E((21, 22, -1), (27, 23, 0), "iron", name="crank arm"),
        E((26, 18, -2), (27, 23, -1), "wood_y", name="crank handle"),
        # a sword waiting its turn
        E((3, 15, 7), (12, 16, 9), "steel", name="blade"),
        E((2, 15, 6), (3, 17, 10), "iron", name="guard"),
        E((0, 15, 7), (2, 16, 9), "leather", name="grip"),
    ]
    rows = [(16, 18, 24), (17, 16, 26), (18, 15, 27), (20, 14, 28), (26, 15, 27), (28, 16, 26), (29, 18, 24)]
    tops = [17, 18, 20, 26, 28, 29, 30]
    for (y0, x0, x1), y1 in zip(rows, tops):
        if y1 <= 18: continue
        g.append(E((x0, max(y0, 18), 6), (x1, y1, 10), WHEEL, name="wheel"))
    g.append(E((19, 21, 5), (23, 25, 11), "iron", name="hub"))
    return Prop("grindstone_bench", {"grindstone_bench": g})

# ---- weapon rack: 2x2x1 ---------------------------------------------------------------------------------------
def weapon_rack():
    r = [
        E((0, 0, 3), (32, 2, 13), "plank_x", name="base"),
        E((1, 2, 7), (4, 30, 10), "wood_y", name="post"), E((28, 2, 7), (31, 30, 10), "wood_y", name="post"),
        E((0, 24, 6), (32, 27, 10), "swood_x", name="top rail"),
        E((4, 10, 7), (28, 12, 9), "swood_x", name="mid rail"),
        E((4, 2, 4), (28, 3, 6), "swood_x", name="foot rail"),
    ]
    # arming sword, point down (Gilded squire): steel blade, iron bar, leather grip
    r += [E((6, 3, 4), (8, 20, 5), "steel", name="arming sword blade"), E((6, 2, 4), (7, 3, 5), "steel", name="point"),
          E((3, 20, 3), (11, 21, 6), "iron", name="arming sword bar"), E((6, 21, 4), (8, 25, 5), "leather", name="grip"),
          E((5, 25, 3), (9, 27, 6), "iron", name="pommel")]
    # boar spear, head up
    r += [E((12, 2, 4), (13, 26, 5), "wood_y", name="boar spear shaft"),
          E((10, 25, 3), (15, 26, 6), "iron", name="spear lugs"),
          E((11, 26, 4), (14, 30, 5), "steel", name="spear head"), E((12, 30, 4), (13, 32, 5), "steel", name="spear tip")]
    # Ironclad hearth axe, head up
    r += [E((18, 2, 4), (19, 28, 5), "wood_y", name="hearth axe haft"),
          E((19, 21, 3), (21, 27, 6), "iron", name="axe cheek"), E((21, 18, 4), (24, 29, 5), "steel", name="axe blade"),
          E((16, 23, 4), (18, 25, 5), "iron", name="axe spike"), E((18, 28, 4), (19, 29, 5), "iron", name="cap")]
    # Gilded longsword, point down, gilt guard
    r += [E((24, 3, 4), (26, 22, 5), "steel", name="longsword blade"), E((24, 2, 4), (25, 3, 5), "steel", name="point"),
          E((21, 22, 3), (29, 23, 6), "gilt", name="longsword guard"), E((24, 23, 4), (26, 28, 5), "leather", name="grip"),
          E((23, 28, 3), (27, 30, 6), "gilt", name="pommel")]
    return Prop("weapon_rack", {"weapon_rack": r})

# ---- shield rack: 2x2x1, one shield a House ----------------------------------------------------------------------
SHIELD_W = [10, 10, 10, 10, 10, 10, 10, 8, 8, 6, 4, 2]
CHARGES = {
    "gilded": ["..........", "....Cc....", "....cc....", "..Cccccd..", "....cc....", "....cc....", "....cc....",
               "....cd....", "....cd....", "..........", "..........", ".........."],
    "graveborn": ["..........", "....dd....", "...cccd...", "...cCCd...", "...cCCd...", "...cccd...", "....dd....",
                  "..........", "..........", "..........", "..........", ".........."],
    "ironclad": ["..........", "..........", ".CCccccd..", "...cccd...", "....cd....", "...cccd...", "..Cccccd..",
                 "..........", "..........", "..........", "..........", ".........."],
    "tidecallers": ["..........", "..C.Cc.d..", "..c.cc.d..", "..Cccccd..", "....cc....", "....cc....", "....cc....",
                    "....cd....", "....dd....", "..........", "..........", ".........."],
    "wildmoot": ["..........", ".C......d.", ".cC....cd.", "..cc..cd..", "...cCcd...", "...cddc...", "...cccc...",
                 "....cc....", "..........", "..........", "..........", ".........."],
}

class ShieldPaint:
    """A heater shield face: field in the House ramp, a 1-texel rim (gilt for Gilded, iron otherwise), the charge."""
    def __init__(s, house, xmin, ytop):
        s.h, s.xmin, s.ytop = HOUSE[house], xmin, ytop
        s.grid = CHARGES[house]; s.rim = GILT if house == "gilded" else IRON
    def inside(s, gx, gy):
        if gy < 0 or gy >= len(SHIELD_W): return False
        w = SHIELD_W[gy]; a = (10 - w) // 2
        return a <= gx < a + w
    def __call__(s, face, P, c, r, w, h):
        x, y, z = P
        gx = int(math.floor(s.xmin + 10 - x)); gy = int(math.floor(s.ytop - y))   # seen from the front: col 0 at max x
        field = s.h["field"]
        if face == "north":
            edge = not all(s.inside(gx + dx, gy + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge: return s.rim[3] if (gy == 0 or gx == 0) else s.rim[2 if gy < 7 else 1]
            ch = s.grid[gy][gx] if 0 <= gy < 12 and 0 <= gx < 10 else "."
            if ch != ".": return s.h["charge"][{"C": 4, "c": 3, "d": 1}[ch]]
            tone = 2 if (gx + gy) % 5 else 3 if gx < 5 else 2
            return field[tone if gy < 8 else 1 if gx >= 6 else 2]
        if face == "south": return WOOD[1]
        if face == "up": return s.rim[3]
        if face == "down": return s.rim[0]
        return s.rim[2] if face == "east" else s.rim[1]

def shield(house, xmin, ybot, z, name, lean=None):
    ytop = ybot + 12; els = []
    p = ShieldPaint(house, xmin, ytop)
    for gy, w in enumerate(SHIELD_W):
        a = (10 - w) // 2
        els.append(E((xmin + a, ytop - gy - 1, z), (xmin + a + w, ytop - gy, z + 2), p, rot=lean, name="%s shield" % name))
    # merge equal rows into taller boxes (fewer elements, same look)
    merged = []
    for e in els:
        if merged and merged[-1].frm[0] == e.frm[0] and merged[-1].to[0] == e.to[0] and merged[-1].frm[1] == e.to[1]:
            merged[-1].frm[1] = e.frm[1]
        else: merged.append(e)
    return merged

def shield_rack():
    r = [
        E((0, 0, 4), (32, 2, 13), "plank_x", name="base"),
        E((0, 2, 8), (3, 31, 11), "wood_y", name="post"), E((29, 2, 8), (32, 31, 11), "wood_y", name="post"),
        E((0, 28, 7), (32, 31, 11), "wood_x", name="top beam"),
        E((3, 13, 8), (29, 15, 10), "wood_x", name="mid rail"),
        E((6, 27, 5), (8, 28, 7), "iron", name="peg"), E((15, 27, 5), (17, 28, 7), "iron", name="peg"),
        E((24, 27, 5), (26, 28, 7), "iron", name="peg"),
    ]
    r += shield("gilded", 2, 15, 5, "gilded")
    r += shield("ironclad", 11, 15, 3, "ironclad")
    r += shield("tidecallers", 20, 15, 5, "tidecallers")
    # two more leaning on the base, tilted back against the mid rail
    r += shield("graveborn", 5, 2, 4, "graveborn", lean=("x", -22.5, (10, 2, 5)))
    r += shield("wildmoot", 17, 2, 4, "wildmoot", lean=("x", -22.5, (22, 2, 5)))
    return Prop("shield_rack", {"shield_rack": r})

# ---- quench trough: 2x1x1 --------------------------------------------------------------------------------------
def quench_trough():
    t = [
        E((2, 0, 3), (6, 1, 13), "swood_z", name="foot"), E((26, 0, 3), (30, 1, 13), "swood_z", name="foot"),
        E((1, 1, 2), (31, 3, 14), "plank_x", name="floor"),
        E((1, 3, 2), (31, 12, 4), "plank_x", name="north wall"), E((1, 3, 12), (31, 12, 14), "plank_x", name="south wall"),
        E((1, 3, 4), (3, 12, 12), "plank_z", name="west wall"), E((29, 3, 4), (31, 12, 12), "plank_z", name="east wall"),
        E((3, 3, 4), (29, 11, 12), "water", skip="north south east west down", name="water"),
        E((6, 1, 1), (8, 12, 2), "iron", name="band"), E((6, 1, 14), (8, 12, 15), "iron", name="band"),
        E((24, 1, 1), (26, 12, 2), "iron", name="band"), E((24, 1, 14), (26, 12, 15), "iron", name="band"),
        # tongs left in the water, leaning on the rim
        E((19, 9, 6), (20, 15, 7), "iron", rot=("z", 22.5, (19, 9, 6)), name="tongs"),
        E((21, 9, 7), (22, 15, 8), "iron", rot=("z", 22.5, (21, 9, 7)), name="tongs"),
    ]
    return Prop("quench_trough", {"quench_trough": t})

ALL = [forge, anvil, weapon_rack, shield_rack, quench_trough, grindstone_bench]

def main(argv):
    want = set(argv[1:])
    for f in ALL:
        p = f()
        if not want or p.id in want: write(p, HERE)

if __name__ == "__main__":
    main(sys.argv)
