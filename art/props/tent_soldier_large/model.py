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

Palette: `build(pid, pal)` takes a `Palette` (canvas ramps, hemp, optional team trim and weathering). `ORIGINAL` is this prop; the
variants (`tent_soldier_large_weathered|dawn|dusk`) are thin model.py wrappers around `build`, so geometry, collision and solid
cells are identical. ORIGINAL's output must stay byte-identical.
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, h3, WOOD, pat_grain, cylinder, LIT   # noqa: F401

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


def _clamp(v): return max(0, min(4, v))


class Palette:
    """Colours of one tent. `canvas` (walls, gables), `roof` (defaults to canvas), `hemp`, `team` (a ramp: roof stripes on
    panels `stripes`, the eave skirt and the rolled flap) and `wear`, a dict: mud (ramp), mud_lo / mud_span (ragged mud line
    px), dirt_lo / dirt_span (grimed lower wall), wall_patches [(face, a0, a1, y0, y1, ramp)] on WALL (a along the wall),
    front_patches (same, on the front wall), roof_patches [(side, s0, s1, z0, z1, ramp)] (side +1 east / -1 west, s px down
    the slope from the ridge), roof_stain (chance a 16 px roof panel is a tone darker)."""
    def __init__(s, canvas=CANVAS, roof=None, hemp=HEMP, team=None, stripes=(), wear=None):
        s.canvas, s.roof, s.hemp, s.team, s.stripes, s.wear = canvas, roof or canvas, hemp, team, tuple(stripes), wear

    def extra(s):
        w = s.wear or {}
        out = [w["mud"]] if w.get("mud") else []
        for key in ("wall_patches", "front_patches", "roof_patches"):
            out += [q[-1] for q in w.get(key, ())]
        if s.team: out.append(s.team)
        return tuple(out)


ORIGINAL = Palette()


def _col(ramp, face, d, base):
    """A texel colour by hand (a pattern returning a colour skips Mat's own lighting): base tone + light + delta."""
    return ramp[_clamp(base + (1 if face == "up" else LIT[face]) + d)]


def _wall_pattern(pal, patches_key, base=3):
    """The wall canvas pattern: a seam every 16 px; weathered: mud line at the hem, grimed lower wall, patches."""
    wr = pal.wear

    def p(f, P, c, r, w, h):
        along = P[2] if f in ("east", "west") else P[0]
        d = -1 if math.floor(along) % 16 == 0 else 0
        if not wr or f in ("up", "down"): return d
        y = P[1]
        for fc, a0, a1, y0, y1, pr in wr.get(patches_key, ()):
            if fc == f and a0 <= along < a1 and y0 <= y < y1:
                edge = along < a0 + 1 or along >= a1 - 1 or y < y0 + 1 or y >= y1 - 1
                return _col(pr, f, -1 if edge else 0, base)
        key = math.floor(along / 3.0)
        if y < wr["mud_lo"] + int(h3(key, 5, 3) * wr["mud_span"]):
            return _col(wr["mud"], f, -1 if y < 2 else 0, base)
        if y < wr["dirt_lo"] + int(h3(math.floor(along / 5.0), 9, 3) * wr["dirt_span"]): d -= 1
        return d
    return p


def _roof_pattern(pal, side, skirt=False):
    """Roof canvas: a seam every 16 px; team stripes on `pal.stripes` panels; weathered: stained panels and patches."""
    wr = pal.wear

    def p(f, P, c, r, w, h):
        panel = math.floor(P[2] / 16)
        d = -1 if math.floor(P[2]) % 16 == 0 else 0
        if pal.team and (skirt or panel in pal.stripes): return _col(pal.team, f, d, 2)
        if not wr: return d
        if f == "up" and not skirt:
            sl = abs(P[0] - 8) + 10
            for sd, s0, s1, z0, z1, pr in wr.get("roof_patches", ()):
                if sd == side and s0 <= sl < s1 and z0 <= P[2] < z1:
                    edge = sl < s0 + 1 or sl >= s1 - 1 or P[2] < z0 + 1 or P[2] >= z1 - 1
                    return _col(pr, f, -1 if edge else 0, 2)
        if wr.get("roof_stain") and h3(panel, 3, side) < wr["roof_stain"]: d -= 1
        return d
    return p


class Kit:
    """The materials of one palette."""
    def __init__(s, pal):
        wr = pal.wear
        s.roof = {sd: Mat(pal.roof, 2, _roof_pattern(pal, sd)) for sd in (1, -1)}
        s.skirt = {sd: Mat(pal.roof, 2, _roof_pattern(pal, sd, skirt=True)) for sd in (1, -1)} if pal.team else s.roof
        s.wall = Mat(pal.canvas, 3, _wall_pattern(pal, "wall_patches"))
        s.gable = Mat(pal.canvas, 3, _wall_pattern(pal, "none"), edges=False)
        s.front = Mat(pal.canvas, 3, _wall_pattern(pal, "front_patches"), edges=False)   # the front wall and lintel: no top edge, or it draws a pale seam across the gable
        s.pole = POLE
        s.inside = Mat(pal.canvas, 1, lambda f, P, c, r, w, h: 0, edges=False, lit=False)
        s.fold = Mat(pal.team or pal.canvas, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[0]) % 5 == 0 else 0)   # the rolled flap
        s.rope = Mat(pal.hemp, 2, edges=False)
        for m in (s.wall, s.gable, s.front) + tuple(s.roof.values()) + tuple(s.skirt.values()):
            m.ramps = (pal.canvas, pal.roof) + pal.extra()
        s.floor, s.blanket, s.bale = FLOOR, BLANKET, BALE


POLE = Mat(make_ramp("9C805A"), 2, pat_grain("z"))
FLOOR = Mat(GROUND, 2, pat_weave, edges=False)
BLANKET = Mat(WOOL, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[0]) % 5 == 0 else 0)
BALE = Mat(HAY, 2, lambda f, P, c, r, w, h: -1 if math.floor(P[2] if f in ("east", "west") else P[0]) % 3 == 0 else 0, edges=False)


def piece_start(side, ox, oy, length, angle, z0, z1, name, mat, thick=2):
    """A roof slab turning about its outer top START corner (ox, oy); it pitches down away from the ridge."""
    if side > 0:
        return E((ox, oy - thick, z0), (ox + length, oy, z1), mat, rot=("z", -angle, (ox, oy, z0)), name=name)
    return E((ox - length, oy - thick, z0), (ox, oy, z1), mat, rot=("z", angle, (ox, oy, z0)), name=name)


def piece_end(side, ex, ey, length, angle, z0, z1, name, mat, thick=2):
    """A roof slab turning about its outer top END corner (ex, ey): its unrotated box lies back toward the ridge."""
    if side > 0:
        return E((ex - length, ey - thick, z0), (ex, ey, z1), mat, rot=("z", -angle, (ex, ey, z0)), name=name)
    return E((ex, ey - thick, z0), (ex + length, ey, z1), mat, rot=("z", angle, (ex, ey, z0)), name=name)


def roof_lower(dx):
    """Underside height of the roof at distance dx from the ridge (steep run, then the flared eave skirt)."""
    if dx <= 40: return R - dx - 2.83
    return (R - 40) - (dx - 40) * math.tan(math.radians(22.5)) - 2.17


def gable_half(y0):
    """Half width of a gable row whose bottom is y0: as wide as the roof underside allows, tucked 1 px under it."""
    dx = 0.0
    while roof_lower(dx + 0.1) >= y0 and dx < 60: dx += 0.1
    return int(dx) - 1


def build(pid="tent_soldier_large", pal=ORIGINAL):
    k = Kit(pal)
    ROPE, WALL, GABLE, FRONT, INSIDE, FOLD, POLE = k.rope, k.wall, k.gable, k.front, k.inside, k.fold, k.pole
    els = []
    # ---- roof: three bays along z (cuts at z=-16, 32), the middle one sags 1 px --------------------------------------
    for z0, z1, sag in [(-50, -16, 0), (-16, 32, -1), (32, 66, 0)]:
        e = R + sag
        # east of the ridge: steep slab ending on the x=32 boundary (turns about its end), then the rest, then the skirt
        els.append(piece_end(1, 32, e - 24, 34, 45, z0, z1, "roof pitch", k.roof[1]))
        els.append(piece_start(1, 32, e - 24, 23, 45, z0, z1, "roof pitch", k.roof[1]))
        els.append(piece_start(1, 48, e - 40, 17, 22.5, z0, z1, "roof eave skirt", k.skirt[1]))
        # west: mirrored about x=8, boundary x=-16
        els.append(piece_end(-1, -16, e - 24, 34, 45, z0, z1, "roof pitch", k.roof[-1]))
        els.append(piece_start(-1, -16, e - 24, 23, 45, z0, z1, "roof pitch", k.roof[-1]))
        els.append(piece_start(-1, -32, e - 40, 17, 22.5, z0, z1, "roof eave skirt", k.skirt[-1]))
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
    return Prop.split(pid, els, SIZE, glow=glow)
