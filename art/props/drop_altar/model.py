"""Drop Altar (`drop_altar`): 11x14x2 cells, anchor cell (5,0,0). Brief: art/props/drop_altar/brief.md.

A BIG prop: model every element in the real prop frame (art/props/README.md: model px, 16 = 1 block, origin = min corner
of the anchor cell, x east, y up, z south, FRONT FACES NORTH (-z)). The footprint runs x -80..96, y 0..224, z 0..32 px.
Prop.split cuts the elements into 3-cell regions (parts drop_altar_<n>); after changing which regions hold elements run
python tools/new_prop.py drop_altar --sync  so the def lists the same parts.
Build:  python art/props/build_props.py drop_altar      Card:  python tools/prop_card.py drop_altar

A giant-relic peg board (plinko) from the age of the giants: dark weathered stone, giant bronze, bone pegs, a glimpur
crystal in the crown. The geometry matches the drop-board physics exactly.

BOARD FRAME -> PROP FRAME (px). The board frame has x 0 (left inner wall) .. 9 (right inner wall), seen from the front,
and y 0 (top rail) down to 11 (slot floor). The front (open face) looks north, so the viewer's right is west (-x):
    prop x = 80 - 16 * board x          prop y = 16 * (11 - board y)          board x 4..5 / y 10..11 = the anchor cell
The coin channel is z 0..12 (0.75 block, open to the north); the back plate's face is z = 12; the coin's centre plane is
z = 6 (0.375 block behind the front face). Nothing but pegs, buttresses, slot fins and the rail's front lip is in the channel
below board y 0; the rail (z 8..12) stays behind a coin centred on z 6 (coin z 4.5..7.5).
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, slab, pat_iron   # noqa: F401

SIZE = (11, 14, 2)


def PX(bx): return int(round(80 - 16 * bx))        # board x -> prop x (px)
def PY(by): return int(round(16 * (11 - by)))      # board y -> prop y (px)


ZF, ZP = 1, 12            # pegs, buttresses and fins run from 1 px behind the front face back to the plate
XL, XR = PX(0), PX(9)      # inner wall faces: 80 (board left wall) and -64 (board right wall)

# ---- ramps ---------------------------------------------------------------------------------------------------
DSTONE = make_ramp("514c56")       # dark weathered giant stone, a cool violet grey
BRONZE = make_ramp("8a6a3e")       # giant bronze, dulled
BONE = make_ramp("c4b896")         # old bone pegs
GLIM = fixed("3a2a66", "6a52b0", "a08ae6", "d6ccf8", "f6f2ff")   # glimpur crystal, deep violet to violet-white

# ---- patterns (world-aligned) ----------------------------------------------------------------------------------
GLYPHS = [("###", "#..", "##.", "#.."), ("#.#", ".#.", ".#.", "#.#"), (".#.", "###", ".#.", ".#."),
          ("##.", "#.#", "##.", "#.."), ("#..", "#.#", "###", "..#"), (".##", "#..", ".#.", "##."),
          ("#.#", "###", "..#", "..#"), ("###", ".#.", ".#.", "###")]


def ashlar(face, P, bw=32, bh=16, yoff=8):
    """Giant ashlar: courses bh tall with the bed joints at y = yoff mod bh, blocks bw wide offset half a block a
    course; the top-left texels of a block catch the light (no speckle pits: the faces are large)."""
    x, y, z = P
    if face in ("up", "down"):
        u, v = x, z
        if (v % bh) < 1 or ((u + (bw // 2) * (math.floor(v / bh) % 2)) % bw) < 1: return -1
        return 0
    along = x if face in ("north", "south") else z
    row = math.floor((y - yoff) / bh); ly = (y - yoff) % bh
    lx = (along + (bw // 2) * (row % 2) + (5 if face in ("east", "west") else 0)) % bw
    if ly < 1 or lx < 1: return -1
    if ly >= bh - 1 and lx < 3: return 1
    return 0


def pat_stone(face, P, c, r, w, h): return ashlar(face, P)


def pat_plate(face, P, c, r, w, h):
    """The back plate's open face: giant ashlar with faint lines of giant script between the peg rows."""
    x, y, z = P
    if face == "north":
        k = (146 - y) / 16.0                                   # bands y 142..146 - 16k, k 0..7 (between peg rows)
        kk = math.floor(k)
        if 0 <= kk <= 7 and (146 - 16 * kk) - 4 <= y < 146 - 16 * kk:
            gy = int(math.floor(146 - 16 * kk - y))            # 0 = top row of the glyph
            u = int(math.floor(x)) + 64; slot, gx = u // 5, u % 5
            if gx < 3 and h3(slot, kk, 61) >= 0.3:
                g = GLYPHS[int(h3(slot, kk, 67) * len(GLYPHS))]
                if g[gy][gx] == "#": return -1
            if gx < 3 and h3(slot, kk, 61) >= 0.3: return 0       # glyph cells stay clean of mortar
    return ashlar(face, P)


def pat_pillar(face, P, c, r, w, h):
    """Carved pillars: two vertical flutes on the front and back, courses every 32 px; ashlar on the sides."""
    x, y, z = P
    if face in ("north", "south"):
        if (x % 16) in (3, 4) or (x % 16) in (11, 12): return -1
        if y % 32 < 1: return -1
        return 0
    if face in ("east", "west"): return ashlar(face, P, bw=16, bh=32, yoff=0)
    return 0


def pat_crystal(face, P, c, r, w, h):
    """Glimpur facets: diagonal facet lines a tone down, a bright first column and top on the lit faces."""
    x, y, z = P
    if face == "up": return 1
    if face == "down": return -1
    if (c + r) % 5 == 0: return -1
    if (r == 0 or c == 0) and face in ("north", "east"): return 1
    return 0


DIRON = make_ramp("3e3c46")       # blackened giant iron (slot fins), darker than the stone so the fins recede


def pat_peg(face, P, c, r, w, h):
    """Bone peg with form: lit top half, shaded underside, and a bronze collar where it meets the plate (z >= 10)."""
    x, y, z = P
    if z >= 10 and face not in ("north", "south"):
        return BRONZE[{"up": 3, "down": 0, "east": 2, "west": 1}[face]]
    if face == "north":
        if h >= 4: return 1 if r == 0 else (-1 if r >= h - 1 else 0)
        return 0 if r == 0 else -1
    return 0


def pat_fin(face, P, c, r, w, h):
    """Slot fin: dark iron, a thin bronze cap on its top 2 px."""
    x, y, z = P
    if face == "up": return BRONZE[3]
    if y >= PY(9.5) - 2: return BRONZE[{"north": 3, "east": 3, "west": 1, "down": 0}.get(face, 2)]
    return 0


class RampMat(Mat):
    def __init__(s, ramp, base, pattern, extra, **kw):
        Mat.__init__(s, ramp, base, pattern, **kw); s.ramps = extra


STONE = Mat(DSTONE, 2, pat_stone, top=0)
PLATE = Mat(DSTONE, 2, pat_plate, top=0)
PILLAR = Mat(DSTONE, 2, pat_pillar, top=0)
BRZ = Mat(BRONZE, 2, pat_iron)
PEG = RampMat(BONE, 2, pat_peg, (BRONZE,), edges=False)
FIN = RampMat(DIRON, 1, pat_fin, (BRONZE,))
CRYS = Mat(GLIM, 3, pat_crystal, edges=False, lit=False)
VEIN = Mat(GLIM, 3, lambda f, P, c, r, w, h: (1 if (r % 6 == 0 and f == "north") else 0), edges=False, lit=False)

# ---- rune plaques ------------------------------------------------------------------------------------------------
MULT = (10, 5, 3, 2, 1, 2, 3, 5, 10)                          # slot 0..8, left to right seen from the front
COLOUR = {1: "dcdce4", 2: "78bee6", 3: "5adcc8", 5: "b078ff", 10: "ffecaa"}


def plaque_ramp(hexc):
    r, g, b = (int(hexc[i:i + 2], 16) for i in (0, 2, 4))
    sc = lambda k: (round(r * k), round(g * k), round(b * k), 255)
    hi = (round(r + (255 - r) * 0.55), round(g + (255 - g) * 0.55), round(b + (255 - b) * 0.55), 255)
    return [sc(0.16), sc(0.34), sc(0.62), (r, g, b, 255), hi]


FH = 7                                                          # numeral height (px)
FONT = {"x": ("...", "...", "...", "#.#", ".#.", ".#.", "#.#"),
        "0": (".##.", "#..#", "#..#", "#..#", "#..#", "#..#", ".##."),
        "1": (".#", "##", ".#", ".#", ".#", ".#", ".#"),
        "2": (".##.", "#..#", "...#", "..#.", ".#..", "#...", "####"),
        "3": ("###.", "...#", "...#", ".##.", "...#", "...#", "###."),
        "5": ("####", "#...", "###.", "...#", "...#", "#..#", ".##.")}
FLOURISH_TOP = ("..............", "....#.##.#....")
FLOURISH_BOT = ("....#.##.#....", "..............")
PW, PH = 14, 20                                                 # plaque size (px): the whole slot width between fins


def text_grid(s):
    cols = []
    for i, ch in enumerate(s):
        g = FONT[ch]
        if i and not (s[i - 1] == "x" and ch == "1"): cols.append("." * FH)   # "1" tucks against the "x" (fits x10)
        for k in range(len(g[0])): cols.append("".join(g[row][k] for row in range(FH)))
    return cols                                                  # list of columns, each FH rows


class PlaquePaint:
    """A glowing rune plaque on the back plate: a dark field in a coloured frame, a faint rune flourish above and below,
    the multiplier in large bright pixel numerals (body in the multiplier colour, a highlight on each stroke's top).
    Painted as seen from the front (north): column 0 is at the plaque's max x (the viewer's left)."""
    def __init__(s, mult, xmax, ytop):
        s.ramp = plaque_ramp(COLOUR[mult]); s.ramps = (s.ramp,)
        s.xmax, s.ytop = xmax, ytop
        cols = text_grid("x%d" % mult); x0 = (PW - len(cols) + 1) // 2; y0 = 6
        s.grid = [["."] * PW for _ in range(PH)]
        for i, col in enumerate(cols):
            for row in range(FH):
                if col[row] == "#":
                    s.grid[y0 + row][x0 + i] = "H" if row == 0 or col[row - 1] != "#" else "D"
        for k, line in enumerate(FLOURISH_TOP):
            for i, ch in enumerate(line):
                if ch == "#": s.grid[2 + k][i] = "F"
        for k, line in enumerate(FLOURISH_BOT):
            for i, ch in enumerate(line):
                if ch == "#": s.grid[15 + k][i] = "F"
        for i in range(PW): s.grid[0][i] = s.grid[PH - 1][i] = "B"
        for j in range(PH): s.grid[j][0] = s.grid[j][PW - 1] = "B"

    def __call__(s, face, P, c, r, w, h):
        x, y, z = P
        if face != "north": return s.ramp[1]
        gx, gy = int(math.floor(s.xmax - x)), int(math.floor(s.ytop - y))
        ch = s.grid[gy][gx] if 0 <= gy < PH and 0 <= gx < PW else "B"
        return {"H": s.ramp[4], "D": s.ramp[3], "F": s.ramp[1], "B": s.ramp[2]}.get(ch, s.ramp[0])


# ---- the build ---------------------------------------------------------------------------------------------------
def peg(bx, by):
    """A round bone peg (4 px across: a 2x4 and a 4x2 bar, the 4x2 set back 1 px so the faces never share a plane);
    the pegs at board x 0 and 9 are half sunk in the walls (only the board half is modelled)."""
    cx, cy = PX(bx), PY(by)
    v = [cx - 1, cx + 1]; hb = [cx - 2, cx + 2]; sk = "south"
    if bx == 0: v[1] = hb[1] = XL; sk = "south east"
    if bx == 9: v[0] = hb[0] = XR; sk = "south west"
    return [E((v[0], cy - 2, ZF), (v[1], cy + 2, ZP), PEG, skip=sk, name="peg"),
            E((hb[0], cy - 1, ZF + 1), (hb[1], cy + 1, ZP), PEG, skip=sk, name="peg")]


def buttress(row_y, left):
    """Stone wedge on a wall beside an odd peg row: the ramp from the wall at (0, rowY - 0.6) down to the outer peg at
    (0.5, rowY), stepped in 2 px columns (tops at the ramp line, rounded), its foot at rowY + 0.125."""
    cy = PY(row_y); cells = set()
    for d0, top in ((0, 8), (2, 6), (4, 4)):           # columns from the wall outward: ramp height above the peg centre
        for d in (d0, d0 + 1):
            x = XL - 1 - d if left else XR + d
            for y in range(cy - 2, cy + top): cells.add((x, y))
    return slab(cells, "z", ZF, ZP, STONE, name="buttress", skip="south")


def build():
    els, glow = [], []
    # back plate: dark stone behind the channel, between the walls, its open face (z 12) carved with giant script
    els += [E((XR, 0, ZP), (XL, 192, 32), STONE, faces={"north": PLATE}, skip="down up east west", name="back plate")]
    # side walls: carved pillars, inner faces at board x 0 and 9, 1 block thick, base, two bronze bands, a capital
    for left in (True, False):
        x0, x1 = (XL, XL + 16) if left else (XR - 16, XR)
        inner = "west" if left else "east"
        ox0, ox1 = (x0, x1 + 2) if left else (x0 - 2, x1)       # outer overhang of base and bands (2 px)
        els += [E((x0, 0, 0), (x1, 192, 32), PILLAR, skip="down up", name="wall pillar"),
                E((ox0, 0, -1), (ox1, 10, 33), STONE, skip="down " + inner, name="pillar base"),
                E((ox0, 56, -1), (ox1, 60, 33), BRZ, skip=inner, name="pillar band"),
                E((ox0, 120, -1), (ox1, 124, 33), BRZ, skip=inner, name="pillar band"),
                E((x0 - 2, 192, -1), (x1 + 2, 206, 33), STONE, name="pillar capital"),
                E((x0 + 2, 206, 4), (x1 - 2, 212, 28), BRZ, skip="down", name="capital finial")]
        cx = (x0 + x1) // 2
        glow += [E((cx - 1, 64, -1), (cx + 1, 116, 0), VEIN, skip="south", name="glimpur vein"),
                 E((cx - 3, 212, 13), (cx + 3, 216, 19), CRYS, skip="down", name="glimpur stud")]
    # crown: lintel with a bronze band, stepped shoulders, a bronze cradle with claws holding the great glimpur crystal
    els += [E((XR + 2, 192, 0), (XL - 2, 202, 32), STONE, skip="east west", name="crown lintel"),
            E((XR + 2, 196, -1), (XL - 2, 199, 0), BRZ, skip="south east west", name="lintel band"),
            E((-40, 202, 2), (-4, 210, 30), STONE, skip="down", name="crown shoulder"),
            E((20, 202, 2), (56, 210, 30), STONE, skip="down", name="crown shoulder"),
            E((-6, 202, 4), (22, 208, 28), BRZ, skip="down", name="crystal cradle"),
            E((-2, 206, 12), (1, 218, 20), BRZ, skip="down", name="cradle claw"),
            E((15, 206, 12), (18, 218, 20), BRZ, skip="down", name="cradle claw")]
    glow += [E((3, 208, 10), (13, 212, 22), CRYS, skip="down up", name="glimpur crystal"),
             E((1, 212, 8), (15, 220, 24), CRYS, name="glimpur crystal"),
             E((4, 220, 11), (12, 224, 21), CRYS, skip="down", name="glimpur crystal")]
    # top rail (board y 0, x 0.5..8.5): bronze, its top at y 0, set back behind the coin plane, on three brackets
    els += [E((PX(8.5), PY(0) - 4, 8), (PX(0.5), PY(0), ZP), BRZ, skip="south", name="top rail")]
    for bx in (1.5, 4.5, 7.5):
        els += [E((PX(bx) - 2, PY(0) - 10, 9), (PX(bx) + 2, PY(0) - 4, ZP), BRZ, skip="south up", name="rail bracket")]
    # pegs: 8 rows at board y 1.5 + r; even rows at x 0..9, odd rows at x 0.5..8.5
    for r in range(8):
        xs = [float(i) for i in range(10)] if r % 2 == 0 else [i + 0.5 for i in range(9)]
        for bx in xs: els += peg(bx, 1.5 + r)
    # wall buttresses beside every odd row, both walls
    for r in (1, 3, 5, 7):
        els += buttress(1.5 + r, True) + buttress(1.5 + r, False)
    # slot fins at board x 1..8 from y 9.5 to the floor (y 11)
    for k in range(1, 9):
        els += [E((PX(k) - 1, 0, ZF), (PX(k) + 1, PY(9.5), ZP), FIN, skip="down south", name="slot fin")]
    # rune plaques on the back plate inside each slot (board y about 9.94..10.88), glowing in the multiplier's colour
    for i, m in enumerate(MULT):
        xmax = PX(i) - 1; xmin = xmax - PW
        glow += [E((xmin, 2, ZP - 1), (xmax, 2 + PH, ZP), PlaquePaint(m, xmax, 2 + PH), skip="south east west",
                   name="rune plaque x%d" % m)]
    return Prop.split("drop_altar", els, SIZE, glow=glow)
