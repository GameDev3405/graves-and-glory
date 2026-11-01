"""Ironhold blockout pass generator (signed layout of 2026-10-30, docs/maps/ironhold/layout-draft-2026-10-30.md).

  python docs/maps/ironhold/sections/make_blockout.py base             # stand-in base world in work/mapforge/ironhold/
  python docs/maps/ironhold/sections/make_blockout.py plans [GROUP|ID]  # blockout plans beside this file (default core)
  python docs/maps/ironhold/sections/make_blockout.py rulings [ID]      # Kort's 2026-10-31 changes as small reopen plans

Blockout is massing only. Every column is described once by `column(x, z)` (fortress columns E and F, field columns
A..D) and each section's plan is that description clipped to the section and grouped into terrain ops by material.
Over it go the seeded terrain helpers from `features()`, in one fixed order and clipped to the section: field swells
and Mire hummocks (noise), settled slopes (erode), bog pools, the flooded trench, the fosse and the river (river),
the King's Road and its causeway, the supply spurs and trench berms (path), the curtain stretch, inner curtain
closures and supply-yard walls (wall), and surface blends (breakup). Noise is a function of world x, z and seed, so
sections planned one at a time meet without seams.

The base world stands in for today's live map until the 2A.2 export exists: flat grass with its top at y 63 over the
four core sections plus a margin, with today's ironhold/gate, ironhold/keep and the four west curtain pieces placed
at their Spec.java LANDMARKS rows (ih-gate, ih-keep, ih-curtain-w*). Today's east curtain pieces (ih-curtain-e*,
x 72) are left out on purpose: Kort ruled 2026-10-30 that the layout's east curtain at x 98 wins and the x 72 pieces
go. Field sections outside the base build their columns whole from y 57.

Kort's rulings of 2026-10-30 on the first blockout: the inner ward is closed by inner curtain runs from the gate's ends
to the river cliffs (the gate and the D4a postern are the only ways in); the river runs in a cut channel (water y 59,
banks y 63) so it cannot be climbed out of; the Great Keep tower stands on the east bank, clear of the river. Ruling of
2026-10-31: the river bends east around the keep court instead of running under it.

Surface-only rule (Kort, 2026-10-31: "We only need the top layer for maps, blocks aren't breakable so what's underneath
doesn't matter"): plans build only what can be seen. A column is its top block and SHELL - 1 blocks under it
(`shell_floor`), reaching down further only where a lower neighbour shows a cliff face; the `seal` helper, run after
the water and roads have reshaped the ground, closes cut banks and steps the shaping opened. No solid interiors. The
blockout sections already built were not rebuilt just to hollow them; the first section plans (cap 150000 each, written
before this rule, solid from y 57) stay as signed history."""
import hashlib
import math
import json
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[3]
sys.path.insert(0, str(ROOT / 'tools'))
from mapforge import manifest as mf  # noqa: E402
from mapforge import terrain  # noqa: E402
from mapforge.plan import PLAN_FORMAT, apply, validate, write_undo  # noqa: E402
from mapforge.structure import normalize  # noqa: E402
from mapforge.world import Cell, World  # noqa: E402

MAP = HERE.parent / 'manifest.json'
WORK = ROOT / 'work/mapforge/ironhold'
WORLD = WORK / 'ironhold.json'
BASE_LO, BASE_HI = (-101, -52), (34, 83)  # E3..F4 plus a 4-block render margin
FLOOR, GROUND = 57, 63  # lowest y a column is built from; field and fortress floor top y 63 (stand at 64)
SHELL = 4  # surface-only maps (Kort 2026-10-31): a column is its top block and SHELL - 1 blocks under it, no solid interior
MAX_CHANGED = 150000  # per-plan change cap; surface-only section plans stay far under it


# ---------------------------------------------------------------- noise

def _hash(*k):
    return int.from_bytes(hashlib.sha256(('ironhold:' + ':'.join(map(str, k))).encode()).digest()[:4], 'big') / 2 ** 32


# ---------------------------------------------------------------- columns

GRASS = ('minecraft:grass_block', 'minecraft:dirt', 3, 'minecraft:stone')
WALL = ('minecraft:stone_bricks', 'minecraft:stone_bricks', 0, 'minecraft:stone_bricks')
TOWER = ('minecraft:deepslate_tiles', 'minecraft:deepslate_tiles', 0, 'minecraft:deepslate_tiles')
CROWN = ('minecraft:deepslate_bricks', 'minecraft:deepslate_bricks', 0, 'minecraft:deepslate_tiles')
BANK = ('minecraft:mud', 'minecraft:mud', 2, 'minecraft:stone')
MUD = ('minecraft:mud', 'minecraft:mud', 2, 'minecraft:dirt')
YARD = ('minecraft:coarse_dirt', 'minecraft:dirt', 3, 'minecraft:stone')

KX0, KX1, KZ0, KZ1 = 9, 23, 24, 44  # Great Keep tower footprint, east of the river leg (bank at x 24)
COURT = (-23, 21, 23, 47)  # today's ironhold/keep court and walls (x0, z0, x1, z1)
RIVER_PTS = [[0, -250, 11], [0, -120, 11], [0, 12, 11], [6, 16, 7], [27.5, 16, 7], [27.5, 52, 6], [6, 52, 7], [0, 56, 11], [0, 100, 11],
             [-4, 148, 11], [-1, 175, 12], [4, 212, 11], [1, 250, 11]]  # centre line and width: the bend around the keep court (2026-10-31)
INNER = 71  # inner curtain closures: walk at 72 like the outer curtain
MIRE = (-185, -125, -110, 110)  # x0, x1, z0, z1 (layout zone table)
SUPPLY_YARDS = ((-155, -125, -90, -54), (-140, -110, 54, 90))  # west supply yards (x0, x1, z0, z1), dry pads
SUPPLY_HOUSES = ((-146, -134, -80, -68), (-131, -119, 64, 76))


def keep_tower(x, z):
    """Great Keep tower (pick D2a): x 9..23, z 24..44 on the east bank (Kort 2026-10-30: out of the river),
    chamfered corners, shaft to y 99, setback (2 in) to 114, crown 115..118 with merlons on the setback edge. It
    replaces today's keep blocks in its footprint, including the court's east wall."""
    if not (KX0 <= x <= KX1 and KZ0 <= z <= KZ1):
        return None
    dx, dz = min(x - KX0, KX1 - x), min(z - KZ0, KZ1 - z)
    if dx + dz < 3:
        return None  # chamfer
    top = 99
    if dx >= 2 and dz >= 2 and dx + dz >= 5:
        top = 114
        if (dx == 2 or dz == 2 or dx + dz == 5) and (x + z) % 2 == 0:
            return (118, CROWN, None)  # merlon
        if dx == 2 or dz == 2 or dx + dz == 5:
            return (115, CROWN, None)  # crown walk
    return (top, TOWER, None)


def fortress(x, z):
    """Columns E and F (x -97..30): (top y, (surface, under, underDepth, core), clearTo) or None to leave the column.
    The fosse, river, walls and road are helper ops laid over this; here they are plain ground for the ops to cut."""
    az = abs(z)
    t = keep_tower(x, z)
    if t:
        return t
    if -78 <= x <= -66 and 92 <= az <= 100:  # west corner towers of the north and south curtains, cracked tops
        return (76 - int(_hash('corner', x, z) * 3) if (x in (-78, -66) or az in (92, 100)) else 76, WALL, None)
    if az > 98 or (z >= 80 and x < -78):  # rows 5 and 6 west of the corner tower belong to the south flank
        return flank(x, z, (GROUND, GRASS, None))
    if -69 <= x <= 102 and 94 <= az <= 98:
        return (71, WALL, None)  # north and south curtains; the river cuts the water-gate arch, a lintel spans it
    # front-door towers: x -80..-70, z +-(9..17), from the fosse floor; cracked tops 73..76
    if -80 <= x <= -70 and 9 <= az <= 17:
        crack = 76 - int(_hash('crack', x, z) * 4) if (x in (-80, -70) or az in (9, 17)) else 76
        return (crack, WALL, None)
    if -74 <= x <= -70 and az <= 8:
        return (GROUND, ('minecraft:cobblestone', 'minecraft:gravel', 2, 'minecraft:stone'), None)  # threshold
    if -74 <= x <= -70:
        return None  # today's curtain pieces (z +-28..58, 66..96) and the new stretch (a wall op) stand here
    if -82 <= x <= -75 and az <= 8:
        return (GROUND, ('minecraft:cobblestone', 'minecraft:cobblestone', 3, 'minecraft:stone'), None)  # causeway
    if x < -74:
        return (GROUND, GRASS, None)  # killing field; the fosse, the road and the swells are ops
    if -69 <= x <= -41 and az <= 93:
        return (GROUND, ('minecraft:gravel', 'minecraft:gravel', 2, 'minecraft:stone'), None)  # Outer Yard
    if -40 <= x <= -34 and az <= 23:
        return None  # today's ironhold/gate stands here
    if COURT[0] <= x <= COURT[2] and COURT[1] <= z <= COURT[3]:
        return None  # today's ironhold/keep court and walls stand here
    d, wd = terrain.nearest(RIVER_PTS, x, z)
    if d <= wd / 2 + 2.5 + 1.5 * terrain.fbm(terrain.seed_of('bank'), x, z, 8, 2):
        return (GROUND, BANK, None)  # river banks, 1..4 blocks of mud; the river op cuts the channel out of this
    if -33 <= x <= -9 and -24 <= z <= 20:
        return (GROUND, ('minecraft:packed_mud', 'minecraft:dirt', 3, 'minecraft:stone'), None)  # inner yard
    if x >= 9 and az <= 93:
        return (GROUND, ('minecraft:andesite', 'minecraft:gravel', 2, 'minecraft:stone'), None)  # east bailey
    if -40 <= x <= -9:
        return (GROUND, ('minecraft:coarse_dirt', 'minecraft:dirt', 3, 'minecraft:stone'), None)  # bailey ground
    return (GROUND, GRASS, None)


def _smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def _box_dist(x, z, box):
    """Signed distance from the box edge: positive inside (x0, x1, z0, z1)."""
    x0, x1, z0, z1 = box
    return min(x - x0, x1 - x, z - z0, z1 - z)


def mire_depth(x, z):
    """How far (x, z) lies inside the Mire, in blocks, with a ragged edge; negative outside. The Mire keeps 3 blocks
    off the supply yards so they stand on dry ground."""
    d = _box_dist(x, z, MIRE) + 10 * terrain.fbm(terrain.seed_of('mire-edge'), x, z, 22, 3)
    west = max(0.0, min(1.0, 1 - (x - MIRE[0]) / 36))  # the west edge: broad lobes and small bays, not a ruled line
    if west:
        d += west * (26 * terrain.fbm(terrain.seed_of('mire-west-lobe'), x, z, 34, 2)
                     + 7 * terrain.fbm(terrain.seed_of('mire-west-bay'), x, z, 7, 2))
    for y in SUPPLY_YARDS:
        d = min(d, -_box_dist(x, z, y) - 3 + 1.5 * terrain.fbm(terrain.seed_of('yard-edge'), x, z, 6, 2))
    return d


def field(x, z):
    """Columns A..D (x <= -98): the attacker field rising to the west upland (y 63 at x -186, 66 at the C/B seam,
    67 on the upland), the Mire (mud at 61 with a ragged, graded edge) and the dry supply-yard pads."""
    for y in SUPPLY_YARDS:
        if _box_dist(x, z, y) >= 0:
            return (GROUND, YARD, None)
    warp = 14 * terrain.fbm(terrain.seed_of('upland-warp'), x, z, 40, 2)  # contours wander, not N-S rulings
    top = GROUND + int(round(4 * _smooth((-186 - x + warp) / 50)))
    if x < -226:
        w = west(x, z, top)
        if w:
            return w
        top += upland_rise(x, z)
    d = mire_depth(x, z)
    if d > 0.8:
        return (61 if d >= 2.5 else 62, MUD, None)
    if d > -2.2:
        return (min(top, 62 if d > -0.8 else 63), GRASS, None)
    if abs(z) > 98 or z >= 80:
        return flank(x, z, (top, GRASS, None))
    return (top, GRASS, None)


# ---------------------------------------------------------------- west column B (camp edge, west upland)

CAMP = (-331, -276, -72, 72)  # siege camp palisade line (x0, x1, z0, z1); the shelf inside stands at 72
CAMP_TOP, UPLAND = 72, 67
CAMP_FLOOR = ('minecraft:coarse_dirt', 'minecraft:dirt', 3, 'minecraft:stone')  # the trodden shelf
RAMPART = ('minecraft:rooted_dirt', 'minecraft:dirt', 3, 'minecraft:stone')  # the shelf's earth slope
CAMP_GATES = ((0, 8), (-64, 4), (64, 4))  # east palisade openings (z centre, half width): King's Road, both lanes
LANE_N_PTS = [[-300, -38], [-294, -46], [-293, -58], [-288, -64], [-276, -64], [-267, -69], [-248, -85], [-232, -99],
              [-223, -104], [-209, -112], [-195, -120], [-161, -142], [-151, -142], [-76, -136], [-43, -132], [-31, -128]]
LANE_S_PTS = [[-300, 38], [-294, 46], [-293, 58], [-288, 64], [-276, 64], [-247, 82], [-228, 95], [-219, 101],
              [-205, 109], [-192, 117], [-178, 125], [-160, 134], [-138, 138], [-112, 142], [-92, 140], [-75, 137],
              [-59, 134], [-20, 130]]
# The lanes leave the camp between the tent rows and through their own palisade gates at z +-64: the layout's straight
# run from (-300,+-38) to (-277,+-62) went through the NE and SE tents.
ROAD_PTS = [[-290, 0], [-188, 0]]
MANTLET_X = -235
MANTLET_Z = (-150, -128, -106, -84, -62, -40, -18, 26, 48, 70, 92, 114, 136)  # layout rows, each 6 long


def camp_height(x, z):
    """The siege camp shelf and its earth slope: (height, material) or None away from it. The shelf (72) reaches 2..7
    blocks past the palisade, its lip warped by noise; the slope falls to the upland (67) over 6 blocks where rock
    shows (a short crag) and 9 where it is earth, and over 16 at the three gates, so the road and the lanes ramp
    down instead of stepping."""
    if abs(z) > 100:  # the west half (x -331..-300, column A) since Kort 2026-10-31
        return None
    lip = 2 + 2.5 * (1 + terrain.fbm(terrain.seed_of('camp-lip'), x, z, 12, 3))
    b = _box_dist(x, z, (-400, CAMP[1] + 2, CAMP[2] - 2, CAMP[3] + 2)) + lip
    ramp = max(_smooth(1 - (abs(z - gz) - hw) / 10) for gz, hw in CAMP_GATES) if x > CAMP[1] - 2 else 0.0
    rock = terrain.fbm(terrain.seed_of('camp-rock'), x, z, 7, 2)
    span = (6 if rock > 0.15 else 9) * (1 - ramp) + 16 * ramp
    if b <= -span:
        return None
    if b >= 0:
        return CAMP_TOP, CAMP_FLOOR
    h = UPLAND + int(round((CAMP_TOP - UPLAND) * _smooth((b + span) / span)))
    return h, (CRAG if rock > 0.15 and ramp < 0.3 and b > -span + 2 else RAMPART)


RIM_X = -333  # the west rim's foot, just behind the camp's west palisade run (x -331)
RIM = ('minecraft:andesite', 'minecraft:stone', 2, 'minecraft:stone')  # skinned with grass by a breakup
# (andesite since 2026-10-31: the flank erodes settle grass, mud and stone with clamps at 72 and 76, which had
# planed a stone rim flat in A2's north strip and A5)


def rim_height(x, z):
    """Column A's west rim (layout: rim 82-88): from its wandering foot behind the camp it climbs 15-21 blocks to
    the map edge. Massed in andesite so the field noise and the flank settles (grass, mud and stone) leave it alone; a
    breakup skins it with grass, earth and rock. None east of the foot."""
    warp = 3 * terrain.fbm(terrain.seed_of('rim-warp'), x, z, 14, 2)
    t = _smooth((RIM_X - x + warp) / 14)
    if t <= 0:
        return None
    peak = 85 + 3 * terrain.fbm(terrain.seed_of('rim-peak'), x, z, 18, 2)
    return UPLAND + int(round((peak - UPLAND) * t))


def _lane_dist(x, z):
    return min(terrain.nearest(p, x, z)[0] for p in (LANE_N_PTS, LANE_S_PTS, ROAD_PTS))


def upland_rise(x, z):
    """The west upland is not a table: it lifts a block or two toward the North Mountains and the South Ridge
    (rows 1 and 6), fading out before the C seam so the field sections keep their edge."""
    fade = _smooth((-226 - x) / 24)
    north = 2.4 * _smooth((-146 - z) / 30)
    south = 2.4 * _smooth((z - 172) / 30)
    roll = 0.8 * terrain.fbm(terrain.seed_of('upland-roll'), x, z, 26, 2)
    return int(round(fade * (north + south + roll * (north + south > 0.3))))


def west(x, z, top):
    """West column B over the field: the camp shelf and slope, then outcrops of grey rock on the upland, kept off
    the road, the lanes and the mantlet row. None leaves the column to the field."""
    c = camp_height(x, z)
    r = rim_height(x, z)
    if r and r > max(top, c[0] if c else 0):
        return (r, RIM, None)
    if c and c[0] > top:
        return (c[0], c[1], None)
    if x > -232 or abs(x - MANTLET_X) < 6:
        return None
    o = terrain.fbm(terrain.seed_of('outcrop'), x, z, 9, 2) + 0.25 * terrain.fbm(terrain.seed_of('outcrop2'), x, z, 3, 1)
    if o > 0.42 and _lane_dist(x, z) > 9:
        return (top + upland_rise(x, z) + 1 + (o > 0.56), CRAG, None)
    return None



# ---------------------------------------------------------------- flanks (layout D6a: north low and wet, south high)

POINT_A = (-29, -130)
BLUFF = (-170, -70, -260, -160)  # north river bluff, top 70 (x0, x1, z0, z1)
CRAG = ('minecraft:stone', 'minecraft:andesite', 2, 'minecraft:stone')
WET = ('minecraft:mud', 'minecraft:dirt', 2, 'minecraft:dirt')
ASH = ('minecraft:coarse_dirt', 'minecraft:dirt', 3, 'minecraft:stone')
VILLAGE = ((-70, -60, -150, -142), (-52, -42, -148, -140), (-62, -52, -116, -108), (-40, -30, -112, -104),
           (-80, -70, -128, -120))  # five roofless houses (x0, x1, z0, z1), layout-blockout.json
WATCHTOWERS = ((-185, -175, -90, -80), (-185, -175, 80, 90))


def _tower(x, z, boxes, top, tag):
    for x0, x1, z0, z1 in boxes:
        if x0 <= x <= x1 and z0 <= z <= z1:
            edge = x in (x0, x1) or z in (z0, z1)
            return (top - int(_hash(tag, x, z) * 3) if edge else top, WALL, None)
    return None


def bluff_height(x, z):
    """North river bluff: rises from the flank floor over a warped, uneven foot to about 70; outcrops of bare rock
    where the slope is steep. Returns (height, crag) or None away from it."""
    b = _box_dist(x, z, BLUFF) + 9 * terrain.fbm(terrain.seed_of('bluff-edge'), x, z, 16, 3)
    if b <= -10:
        return None
    rock = terrain.fbm(terrain.seed_of('bluff-rock'), x, z, 6, 2)
    span = 6 if rock > 0.2 else 10  # where rock shows the face is steeper
    h = GROUND + round(7 * _smooth((b + span) / span)) + round(1.5 * terrain.fbm(terrain.seed_of('bluff-top'), x, z, 9, 2))
    return h, (rock > 0.2 and -span + 2 < b < 1)


def north_wet(x, z):
    """How wet the north flank is at (x, z): above about 0.15 the ground lies at 62 in mud."""
    return terrain.fbm(terrain.seed_of('north-wet'), x, z, 15, 3) + 0.25 * _smooth((-104 - z) / 14) - 0.1


def flank(x, z, base):
    """Ground outside the north and south curtains (|z| > 98) and on the field's flanks. North: a low, wet meadow at
    62..63 with the bluff behind, the burned village on trampled ash, the A supply yard pad, the river rising into
    its gorge mouth. South: see south_flank."""
    t = _tower(x, z, WATCHTOWERS, 77, 'watch')
    if t:
        return t
    if z > 0:
        return south_flank(x, z, base)
    top, mat, clear = base
    dA = math.hypot(x - POINT_A[0], z - POINT_A[1])
    if dA <= 16 + 2 * terrain.fbm(terrain.seed_of('yard-a'), x, z, 5, 2):
        return (GROUND, YARD, None)  # A supply yard: dry, level, at 63 under the capture ring
    d, wd = terrain.nearest(RIVER_PTS, x, z)
    if z < -140 and d > wd / 2 + 1:  # the river runs north into its gorge: the banks rise toward the rim
        rise = _smooth((-140 - z) / 36) * _smooth(1 - (d - wd / 2) / 70)
        top = max(top, GROUND + round(8 * rise + 1.5 * terrain.fbm(terrain.seed_of('gorge'), x, z, 7, 2)))
    if dA <= 19.5:
        top = min(top, GROUND)  # nothing stands into the A capture ring (y 64..67 within 17)
    if d <= wd / 2 + 2.5 + 1.5 * terrain.fbm(terrain.seed_of('bank'), x, z, 8, 2):
        return (max(top, GROUND), BANK, None)
    bl = bluff_height(x, z)
    if bl and bl[0] > top:
        return (bl[0], CRAG if bl[1] else GRASS, None)
    if top > GROUND:
        return (top, mat, clear)
    for x0, x1, z0, z1 in VILLAGE:
        if _box_dist(x, z, (x0 - 4, x1 + 4, z0 - 4, z1 + 4)) + 2 * terrain.fbm(terrain.seed_of('ash'), x, z, 4, 2) > 0:
            return (GROUND, ASH, None)  # burned and trampled ground round each house
    if north_wet(x, z) > 0.15:
        return (62, WET, None)
    return (top, mat, clear)



POINT_C = (29, 130)
GALLOWS_RIDGE = (-215, -108, 112, 200)  # top about 72 (x0, x1, z0, z1)
WATCH_HILL = (45, 100, 140, 200)  # Watch Hill shoulder, rising SE to about 72
CHAPEL = (-165, -145, 140, 156)
GALLOWS = ((-190, 130), (-120, 175))
CHAPEL_FLOOR = ('minecraft:cobblestone', 'minecraft:stone', 2, 'minecraft:stone')
CPAD = ('minecraft:coarse_dirt', 'minecraft:gravel', 2, 'minecraft:stone')
LOG = ('minecraft:dark_oak_log', 'minecraft:dark_oak_log', 0, 'minecraft:dark_oak_log')


def landform(x, z, box, rise, tag):
    """A hill on a box: the edge warped by 10 blocks, a long slope (12) where the ground is soft and a short one (6)
    with bare rock where it is not, a crest that rolls by a block or two. (height, crag) or None away from it."""
    b = _box_dist(x, z, box) + 10 * terrain.fbm(terrain.seed_of(tag + '-edge'), x, z, 18, 3)
    if b <= -12:
        return None
    rock = terrain.fbm(terrain.seed_of(tag + '-rock'), x, z, 7, 2)
    span = 6 if rock > 0.18 else 12
    h = GROUND + round(rise * _smooth((b + span) / span)) + round(1.6 * terrain.fbm(terrain.seed_of(tag + '-top'), x, z, 11, 2))
    return h, (rock > 0.18 and -span + 2 < b < 1)


def _south_ground(x, z, top):
    for box, rise, tag in ((GALLOWS_RIDGE, 9, 'gallows'), (WATCH_HILL, 9, 'watch-hill')):
        lf = landform(x, z, box, rise, tag)
        if lf and lf[0] > top:
            return lf
    return top, False


def south_flank(x, z, base):
    """South flank (layout D6a: high): dry ground at 63..64 rising to Gallows Ridge (72, the chapel shell and two
    gallows on it) and the Watch Hill shoulder (72); point C on a level, trodden pad at 63 under its ring; the river
    in its cut channel, the banks rising toward the South Ridge rim."""
    top, mat, clear = base
    for gx, gz in GALLOWS:
        if (x, z) == (gx, gz):
            return (_south_ground(x, z, top)[0] + 5, LOG, None)  # gallows post, the beam is a fill op
    if CHAPEL[0] - 1 <= x <= CHAPEL[1] + 1 and CHAPEL[2] - 1 <= z <= CHAPEL[3] + 1:
        return (72, CHAPEL_FLOOR, None)  # the chapel's floor, levelled into the ridge
    dC = math.hypot(x - POINT_C[0], z - POINT_C[1])
    if dC <= 14 + 4 * terrain.fbm(terrain.seed_of('yard-c'), x, z, 7, 2) + 2 * terrain.fbm(terrain.seed_of('yard-c2'), x, z, 3, 1):
        return (GROUND, CPAD, None)
    d, wd = terrain.nearest(RIVER_PTS, x, z)
    if z > 170 and d > wd / 2 + 1:
        rise = _smooth((z - 170) / 36) * _smooth(1 - (d - wd / 2) / 70)
        top = max(top, GROUND + round(7 * rise + 1.5 * terrain.fbm(terrain.seed_of('gorge-s'), x, z, 7, 2)))
    if d <= wd / 2 + 2.5 + 1.5 * terrain.fbm(terrain.seed_of('bank'), x, z, 8, 2):
        return (max(top, GROUND), BANK, None)
    h, crag = _south_ground(x, z, top)
    if x <= -98 and h > top:  # the ridge foot grades down into the Mire instead of standing on its edge
        cap = 63 + max(0.0, -mire_depth(x, z) - 2.2) * 0.7
        if h > cap:
            h, crag = max(top, round(cap)), False
    if dC <= 19.5:
        return (min(h, GROUND), mat, clear)  # nothing stands into the C capture ring (y 64..67 within 17)
    return (h, CRAG if crag else mat, clear)



EAST_CORNERS = ((96, 106, -100, -92), (96, 106, 92, 100))  # NE and SE corner towers of the curtain (x0, x1, z0, z1)
EAST_WATCH = ((145, 155, -155, -145), (145, 155, 145, 155))  # east watchtowers, top 79
DONJON = (103, 158, -48, 48)  # Donjon Court floor, polished blackstone at 63
NE_YARD, SE_YARD = (110, 140, -90, -54), (125, 155, 54, 90)  # east supply yards (x0, x1, z0, z1)
EAST_HOUSES = ((119, 131, -80, -68), (134, 146, 64, 76))
HALL = (110, 150, -40, -20)  # barracks hall in the court
DUSK = (126, 130, -2, 2)  # spawn-dusk protect lock (y 64..90 since Kort 2026-10-31): a plain floor to 63, nothing above
DUSK_EX = {'min': [125, -3], 'max': [131, 3]}
FLAGS = ('minecraft:polished_blackstone', 'minecraft:cobbled_deepslate', 2, 'minecraft:stone')


def east(x, z):
    """Column H (x 95..158): the east curtain at x 98..102 with its corner towers, Donjon Court behind it, the two
    east supply yards, the flanks beyond the curtain lines and the east watchtowers. Kort 2026-10-31: the Dusk spawn
    box gets a real floor (plain polished blackstone to 63; its lock now refuses blocks y 64..90 only), and the north
    and south curtains run on past the corner towers to the gorge parapet, so the court is walled like an inner ward."""
    az = abs(z)
    if DUSK[0] <= x <= DUSK[1] and DUSK[2] <= z <= DUSK[3]:
        return (GROUND, ('minecraft:polished_blackstone', 'minecraft:polished_blackstone', 2, 'minecraft:stone'), None)
    t = _tower(x, z, EAST_WATCH, 79, 'ewatch')
    if t:
        return t
    for x0, x1, z0, z1 in EAST_CORNERS:
        if x0 <= x <= x1 and z0 <= z <= z1:
            edge = x in (x0, x1) or z in (z0, z1)
            return (76 - int(_hash('ecorner', x, z) * 3) if edge else 76, WALL, None)
    if az > 98:
        return flank(x, z, (GROUND, GRASS, None))
    if 98 <= x <= 102 or az >= 94:
        return (71, WALL, None)  # east curtain, and the north and south curtains on east to the gorge parapet
    if x <= 97:
        return (GROUND, ('minecraft:andesite', 'minecraft:gravel', 2, 'minecraft:stone'), None)  # east bailey
    if DONJON[0] <= x <= DONJON[1] and DONJON[2] <= z <= DONJON[3]:
        return (GROUND, FLAGS, None)
    for x0, x1, z0, z1 in (NE_YARD, SE_YARD):
        if x0 <= x <= x1 and z0 <= z <= z1:
            return (GROUND, YARD, None)
    return (GROUND, GRASS, None)


GORGE_X0, PARAPET, GORGE_FLOOR = 159, (166, 168), 44  # column I: ground to the parapet, then the drop to the gorge floor
GORGE_BED = ('minecraft:gravel', 'minecraft:stone', 2, 'minecraft:stone')
GORGE_BASE = 40  # gorge columns build from here (the rest of the map from FLOOR)


def gorge(x, z):
    """Column I (x 159..222), the gorge parapet (layout zone table): the ward ground and the flanks run on to a low
    stone parapet at x 166..168 (walk 64, top 65, merlons to 66); the north and south curtains close on it, so the
    Donjon Court's ward is walled on every side (Kort 2026-10-31). Past it the rim breaks off along a ragged edge
    2..6 blocks out and drops in steep crag steps to the gorge floor at 44, with scree banked at the foot and a few
    fallen boulders."""
    az = abs(z)
    if x <= PARAPET[1]:
        if 94 <= az <= 98:
            return (71, WALL, None)  # north and south curtains run on to the parapet
        if x >= PARAPET[0]:
            return (66 if (z + x) % 2 == 0 else 65, WALL, None)
        if az > 98:
            return flank(x, z, (GROUND, GRASS, None))
        return (GROUND, GRASS, None)
    edge = PARAPET[1] + 2.5 + 2 * terrain.fbm(terrain.seed_of('gorge-edge'), x, z, 10, 3)
    out = x - edge
    if out < 0:
        return (GROUND, CRAG if out > -1.2 else GRASS, None)
    step = 0.6 * terrain.fbm(terrain.seed_of('gorge-ledge'), x, z, 4, 2)
    face = GROUND - 1 - int(round(4.5 * out + 3 * step))
    scree = GORGE_FLOOR + int(round(5 * _smooth(1 - (out - 3) / 9) + 1.5 * terrain.fbm(terrain.seed_of('scree'), x, z, 5, 2)))
    boulder = terrain.fbm(terrain.seed_of('gorge-boulder'), x, z, 3, 1)
    top = max(face, scree, GORGE_FLOOR)
    if out > 12 and boulder > 0.55:
        top = max(top, GORGE_FLOOR + 2 + (boulder > 0.7))
        return (top, CRAG, None, GORGE_BASE)
    return (top, CRAG if top == face and top > scree else GORGE_BED, None, GORGE_BASE)


EDGE_N, EDGE_S, EDGE_E = -182, 212, 300  # feet of the North Mountains, the South Ridge and the gorge's far wall


def edge_rise(x, z):
    """Rows 0 and 7 and columns J..K's far side (layout: rims and backdrop, rim 82-88): 0 inside the play rows,
    climbing to 1 at the map edge, the feet wandering a few blocks. The river keeps a valley through both rims
    (its gorge mouth), so t is pressed down within about 30 blocks of its line."""
    warp = 3 * terrain.fbm(terrain.seed_of('edge-warp'), x, z, 16, 2)
    t = max(_smooth((EDGE_N - z + warp) / 58), _smooth((z - EDGE_S + warp) / 36), _smooth((x - EDGE_E + warp) / 44))
    if t <= 0:
        return 0.0
    rx = 0 if z < 0 else 2
    return t * (1 - 0.85 * _smooth(1 - (abs(x - rx) - 9) / 24))


def column(x, z):
    """The ground column at (x, z); the edge rims (rows 0 and 7, the gorge's far wall) are laid over it."""
    c = _column(x, z)
    t = edge_rise(x, z)
    if t <= 0.02:
        return c
    top = c[0]
    peak = 85 + 3 * terrain.fbm(terrain.seed_of('edge-peak'), x, z, 18, 2)
    h = int(round(top + (peak - top) * t))
    if h <= top:
        return c
    mat = c[1] if t < 0.12 else CRAG if x >= GORGE_X0 else RIM
    return (h, mat, None) + tuple(c[3:])


def _column(x, z):
    if x >= GORGE_X0:
        return gorge(x, z)
    if x >= 95:
        return east(x, z)
    t = _tower(x, z, WATCHTOWERS, 77, 'watch')
    if t:
        return t
    return fortress(x, z) if x >= -97 else field(x, z)


# ---------------------------------------------------------------- helper ops

W = 'minecraft:'


def _mix(*pairs):
    return [[W + n, w] for n, w in pairs]


def _pools():
    """Bog pools across the Mire: one short wandering channel per 18-block cell where the hash allows, kept off the
    causeway. Water at 61 over a 1-2 deep bed; `only` mud, so each pool stops where the mud ends."""
    ops = []
    for cz in range(MIRE[2] // 18 - 1, MIRE[3] // 18 + 2):
        for cx in range(MIRE[0] // 18 - 1, MIRE[1] // 18 + 2):
            if _hash('pool', cx, cz) > 0.62:
                continue
            x, z = cx * 18 + 4 + int(_hash('px', cx, cz) * 10), cz * 18 + 4 + int(_hash('pz', cx, cz) * 10)
            if abs(z) < 9 or mire_depth(x, z) < 4:
                continue
            pts = [[x, z, 3 + round(_hash('pw', cx, cz) * 3)]]
            for k in range(1 + int(_hash('pn', cx, cz) * 3)):
                ang = _hash('pa', cx, cz, k) * 6.283
                step = 3 + _hash('ps', cx, cz, k) * 5
                x, z = round(x + step * math.cos(ang)), round(z + step * math.sin(ang))
                pts.append([x, z, 2 + round(_hash('pw', cx, cz, k) * 3)])
            ops.append({'op': 'river', 'note': f'bog pool ({pts[0][0]},{pts[0][1]})', 'points': pts, 'width': 4,
                        'water': 61, 'depth': 2, 'floor': 57, 'bed': _mix(('mud', 3), ('clay', 1)), 'wobble': 0.8,
                        'wobbleScale': 4, 'only': [W + 'mud'], 'exclude': [{'min': [-200, -7], 'max': [-110, 7]}],
                        'seed': f'pool{cx},{cz}'})
    return ops


ROAD_GAP = {'min': [-400, -6], 'max': [-60, 6]}
TRENCH_GAPS = [ROAD_GAP, {'min': [-400, 39], 'max': [-60, 43]}, {'min': [-400, -43], 'max': [-60, -39]}]


TRENCH_RUNS = ((-60, -46), (-36, -9), (9, 36), (46, 60))  # each trench is dug in four runs between the crossings


def _trench_line(x0, z0, z1, tag):
    """A hand-dug line near x0: a point every 4..6 blocks, each off the line by up to a block, so the trench keeps
    its position but never runs on a ruler. Third value: a per-point hash in 0..1 for width and depth."""
    pts, z, k = [], float(z0), 0
    while True:
        dx = 0 if k == 0 and z0 in (-60, 60) else round((_hash(tag, 'dx', z0, k) - 0.5) * 2.0, 1)
        pts.append([x0 + dx, round(z, 1), _hash(tag, 'w', z0, k)])
        if z >= z1:
            break
        z = min(z1, z + 4 + _hash(tag, 'step', z0, k) * 2)
        k += 1
    return pts


def _trench150():
    """Mire trench x -150, flooded: four runs with rounded ends short of the crossings, the line wandering by a
    block, 3..4 wide and 2..3 deep run by run; the east berm follows the line, uneven at 62..63, and the lips are
    slumped by a thermal erode afterwards (only earth, so the bed and the water stay)."""
    ops = []
    for z0, z1 in TRENCH_RUNS:
        line = _trench_line(-150, z0, z1, 't150')
        depth = 2 + (_hash('t150', 'depth', z0) > 0.45)
        ops.append(({'op': 'river', 'note': f'Mire trench x -150, run z {z0}..{z1}, flooded, {depth} deep',
                     'points': [[x, z, 3 + (h > 0.6)] for x, z, h in line], 'width': 3, 'water': 61,
                     'depth': depth, 'floor': 57, 'bank': 1, 'bed': _mix(('mud', 3), ('clay', 1), ('gravel', 1)),
                     'wobble': 0.7, 'wobbleScale': 4, 'exclude': TRENCH_GAPS, 'seed': f'trench150:{z0}'}, None))
        ops.append(({'op': 'path', 'note': f'Mire trench berm (east lip), run z {z0}..{z1}',
                     'points': [[x + 3.5, z, 62 + (h > 0.35)] for x, z, h in line], 'width': 2, 'falloff': 1.5,
                     'surface': _mix(('coarse_dirt', 3), ('dirt', 1), ('rooted_dirt', 1)), 'under': W + 'dirt',
                     'wobble': 0.8, 'wobbleScale': 4, 'exclude': TRENCH_GAPS, 'seed': f'berm150:{z0}'}, None))
    ops.append(({'op': 'erode', 'note': 'trench x -150: slump the lips and the berm', 'mode': 'thermal',
                 'iterations': 3, 'talus': 0.8, 'rate': 0.5, 'jitter': 0.4, 'clamp': [61, 66],
                 'only': [W + n for n in ('coarse_dirt', 'dirt', 'rooted_dirt', 'grass_block')],
                 'seed': 'slump150'}, (-155, -62, -143, 62)))
    return ops


def _trench105():
    """Field trench x -105, dry: four runs, floor 60..61 along the run with sloped sides (falloff), a floor of
    trodden mud and gravel; the east berm at 64..65 follows the line; the lips are slumped afterwards."""
    ops = []
    for z0, z1 in TRENCH_RUNS:
        line = _trench_line(-105, z0, z1, 't105')
        ops.append(({'op': 'path', 'note': f'field trench x -105, run z {z0}..{z1}, dry',
                     'points': [[x, z, 60 + (h > 0.4)] for x, z, h in line], 'width': 3, 'falloff': 1.2,
                     'surface': _mix(('coarse_dirt', 3), ('packed_mud', 2), ('mud', 1), ('gravel', 1)),
                     'wobble': 0.7, 'wobbleScale': 4, 'exclude': TRENCH_GAPS, 'seed': f't105:{z0}'}, None))
        ops.append(({'op': 'path', 'note': f'field trench berm (east lip), run z {z0}..{z1}',
                     'points': [[x + 3.5, z, 64 + (h > 0.5)] for x, z, h in line], 'width': 2, 'falloff': 1.5,
                     'surface': _mix(('coarse_dirt', 3), ('dirt', 1), ('rooted_dirt', 1)), 'under': W + 'dirt',
                     'wobble': 0.8, 'wobbleScale': 4, 'exclude': TRENCH_GAPS, 'seed': f'berm105:{z0}'}, None))
    ops.append(({'op': 'erode', 'note': 'trench x -105: slump the lips and the berm', 'mode': 'thermal',
                 'iterations': 3, 'talus': 0.8, 'rate': 0.5, 'jitter': 0.4, 'clamp': [60, 67],
                 'only': [W + n for n in ('coarse_dirt', 'dirt', 'rooted_dirt', 'grass_block')],
                 'seed': 'slump105'}, (-110, -62, -98, 62)))
    return ops



A_BOX = {'min': [-48, -149], 'max': [-10, -111]}  # round the A capture ring: no raising ops
NORTH = (-350, -250, 158, -101)  # the north flank outside the curtain line (column H since 2026-10-31)
NORTH_FIELD = (-350, -250, -82, -49)


def _north_shape():
    ops = []
    for region in (NORTH_FIELD, (-81, -250, 158, -101)):
        ops.append(({'op': 'noise', 'note': 'north flank swells: low ground, the bluff and the gorge banks',
                     'amplitude': 1.1, 'scale': 20, 'octaves': 3, 'offset': 0.15, 'clamp': [62, 72],
                     'only': [W + 'grass_block'], 'exclude': [A_BOX], 'seed': 'field'}, region))
    ops.append(({'op': 'erode', 'note': 'north flank: settle the bluff, the gorge banks and the meadow',
                 'mode': 'thermal', 'iterations': 4, 'talus': 1.0, 'rate': 0.5, 'jitter': 0.35, 'clamp': [61, 72],
                 'only': [W + n for n in ('grass_block', 'mud', 'stone')], 'exclude': [A_BOX], 'seed': 'settle-n'},
                (-350, -250, 158, -101)))
    return ops


def _north_water():
    """Puddles and shallow pools on the wet meadow (water at 62, flush with the mud), then the north ford: the river
    shoaled to a gravel bed 1 under the water across z -136..-124, banks eased down on both sides."""
    ops = []
    for cz in range(-250 // 14, -100 // 14):
        for cx in range(-230 // 14, 158 // 14 + 1):
            if _hash('npool', cx, cz) > 0.55:
                continue
            x, z = cx * 14 + 3 + int(_hash('npx', cx, cz) * 8), cz * 14 + 3 + int(_hash('npz', cx, cz) * 8)
            if z > -101 or north_wet(x, z) < 0.3 or math.hypot(x - POINT_A[0], z - POINT_A[1]) < 21                     or x >= GORGE_X0:  # Kort 2026-10-31: no pool on the gorge rim (I1), dry ground there
                continue
            pts = [[x, z, 2 + round(_hash('npw', cx, cz) * 3)]]
            for k in range(1 + int(_hash('npn', cx, cz) * 3)):
                ang = _hash('npa', cx, cz, k) * 6.283
                step = 2 + _hash('nps', cx, cz, k) * 4
                x, z = round(x + step * math.cos(ang)), round(z + step * math.sin(ang))
                pts.append([x, z, 2 + round(_hash('npw', cx, cz, k) * 2)])
            ops.append(({'op': 'river', 'note': f'north meadow pool ({pts[0][0]},{pts[0][1]})', 'points': pts,
                         'width': 3, 'water': 62, 'depth': 1 + (_hash('npd', cx, cz) > 0.6), 'floor': 57,
                         'bed': _mix(('mud', 3), ('clay', 1)), 'wobble': 0.7, 'wobbleScale': 3, 'only': [W + 'mud'],
                         'seed': f'npool{cx},{cz}'}, None))
    # the north ford (tidied 2026-10-31 like the south one): a gravel bar on a slant from the A side (west, z -127)
    # to the reach track (east, z -134), 1 under the water; the channel keeps its width; ramps in _north_paths
    ops.append(({'op': 'river', 'note': 'north ford: slanting gravel bar 1 under the water, channel kept its width',
                 'points': [[-7, -126.5, 5], [-2, -128.5, 6], [3, -131, 6], [7, -133.5, 5]], 'width': 6,
                 'water': 59, 'depth': 1, 'floor': 55, 'bank': 2,
                 'bed': _mix(('gravel', 4), ('cobblestone', 1), ('clay', 1)),
                 'bankSurface': _mix(('gravel', 3), ('coarse_dirt', 2), ('mud', 1)), 'wobble': 1.4,
                 'wobbleScale': 3, 'seed': 'ford-n2'}, None))
    return ops


def _north_paths():
    lane = {'op': 'path', 'width': 5, 'falloff': 2, 'surface': _mix(('dirt_path', 3), ('coarse_dirt', 2)),
            'edge': _mix(('coarse_dirt', 2), ('gravel', 1)), 'edgeDensity': 0.5, 'wobble': 2.0, 'wobbleScale': 6}
    ops = [(dict(lane, note='north lane: camp, bluff foot, burned village, A', seed='lane-n',
                 points=LANE_N_PTS), None)]
    track = {'op': 'path', 'width': 3, 'falloff': 1.5, 'surface': _mix(('dirt_path', 2), ('coarse_dirt', 2),
             ('grass_block', 1)), 'wobble': 1.2, 'wobbleScale': 5}
    ops.append((dict(track, note='A to the north sally gap, round the fosse end (outside the curtain)',
                     seed='sally-n', points=[[-31, -128], [-46, -119], [-66, -112], [-84, -105], [-88, -92],
                                             [-87, -76], [-84, -62]]), None))
    ops.append(({'op': 'path', 'note': 'north sally causeway over the fosse at 63', 'points': [[-85, -62], [-74, -62]],
                 'width': 3, 'y': 63, 'surface': _mix(('cobblestone', 2), ('coarse_dirt', 1)),
                 'under': _mix(('cobblestone', 2), ('dirt', 1)), 'seed': 'sallyc-n'}, None))
    ops.append((dict(track, note='north reach: worn track along the river bank to the ford', seed='reach-n',
                     points=[[92, -106], [64, -110], [36, -118], [18, -127]]), None))
    ramp = {'op': 'path', 'width': 4, 'falloff': 2.5, 'surface': _mix(('gravel', 3), ('coarse_dirt', 2), ('dirt_path', 1)),
            'under': W + 'gravel', 'edge': _mix(('gravel', 2), ('coarse_dirt', 1), ('andesite', 1)), 'edgeDensity': 0.6,
            'wobble': 1.3, 'wobbleScale': 3}
    ops.append((dict(ramp, note='north ford: west ramp, A side down the cliff to the bar', seed='fordramp-nw',
                     points=[[-21, -130, 63], [-14, -128.5, 63], [-9, -127, 61], [-6, -126.5, 59]]), None))
    ops.append((dict(ramp, note='north ford: east ramp, bar up the cliff to the reach track', seed='fordramp-ne',
                     points=[[6, -133.5, 59], [9, -134.5, 61], [13, -132, 63], [18, -128, 63]]), None))
    return ops


def _house(i, x0, x1, z0, z1):
    """A roofless burned house: a closed 1-thick shell whose top breaks between 64 and 68 along each side, a doorway
    gap on one side, any part within 19 of A broken down to the ground (the capture ring)."""
    pts = []
    for (ax, az), (bx, bz) in (((x0, z0), (x1, z0)), ((x1, z0), (x1, z1)), ((x1, z1), (x0, z1)), ((x0, z1), (x0, z0))):
        for k in range(3):
            px, pz = ax + (bx - ax) * k / 3, az + (bz - az) * k / 3
            top = 64 + round(_hash('house', i, k, ax, az) * 4)
            if math.hypot(px - POINT_A[0], pz - POINT_A[1]) < 19:
                top = GROUND
            pts.append([round(px, 1), round(pz, 1), top])
    doors = (((x0 + x1) // 2, z0), (x1, (z0 + z1) // 2), ((x0 + x1) // 2, z1), (x0, (z0 + z1) // 2))
    door = doors[int(_hash('door', i) * 4)]
    return ({'op': 'wall', 'note': f'burned house {i + 1} ({x0}..{x1}, {z0}..{z1}), roofless shell', 'points': pts,
             'closed': True, 'thickness': 1, 'floor': 57,
             'block': _mix(('blackstone', 4), ('cobbled_deepslate', 2), ('polished_blackstone_bricks', 1),
                           ('cobblestone', 1)),
             'exclude': [{'min': [door[0] - 1, door[1] - 1], 'max': [door[0] + 1, door[1] + 1]}],
             'seed': f'house{i}'}, None)


def _north_built():
    ops = [_house(i, *b) for i, b in enumerate(VILLAGE)]
    for a0, a1 in ((30, 70), (95, 128), (215, 248), (288, 322)):  # stubs of the A yard wall, clear of the lanes
        pts = []
        for k in range(4):
            ang = math.radians(a0 + (a1 - a0) * k / 3)
            r = 21 + _hash('ayard', a0, k) * 1.5
            pts.append([round(POINT_A[0] + r * math.cos(ang), 1), round(POINT_A[1] + r * math.sin(ang), 1),
                        64 + round(_hash('ayard-top', a0, k) * 2)])
        ops.append(({'op': 'wall', 'note': f'A supply yard wall stub ({a0}..{a1} deg), broken', 'points': pts,
                     'thickness': 1.4, 'floor': 57, 'block': _mix(('cobblestone', 3), ('mossy_cobblestone', 1)),
                     'seed': f'ayard{a0}'}, None))
    ops.append(({'op': 'fill', 'note': 'north water gate: arch lintel over the river, y 68..71', 'min': [-6, 68, -98],
                 'max': [6, 71, -94], 'block': W + 'stone_bricks'}, (-6, -98, 6, -94)))
    return ops


def _north_surface():
    return [
        ({'op': 'breakup', 'note': 'burned village: ash, gravel and charred earth', 'replace': [W + 'coarse_dirt'],
          'mix': _mix(('gravel', 2), ('podzol', 2), ('dirt', 1), ('tuff', 1)), 'density': 0.45, 'scale': 2,
          'seed': 'ash'}, (-90, -160, -49, -100)),
        ({'op': 'breakup', 'note': 'north meadow: grass going to mud and moss at the wet',
          'replace': [W + 'grass_block'], 'mix': _mix(('mud', 2), ('moss_block', 2), ('coarse_dirt', 1)),
          'near': [W + 'mud', W + 'water'], 'width': 2.5, 'density': 0.6, 'scale': 2, 'seed': 'nmeadow'}, NORTH),
        ({'op': 'breakup', 'note': 'north wet: moss, clay and rooted mud', 'replace': [W + 'mud'],
          'mix': _mix(('moss_block', 3), ('mud', 3), ('clay', 1), ('rooted_dirt', 1)), 'density': 0.4, 'scale': 2,
          'seed': 'nwet'}, NORTH),
        ({'op': 'breakup', 'note': 'bluff: rock faces broken with andesite, cobble and gravel',
          'replace': [W + 'stone'], 'mix': _mix(('andesite', 2), ('cobblestone', 1), ('gravel', 1), ('tuff', 1)),
          'density': 0.5, 'scale': 2, 'seed': 'crag'}, NORTH),
        ({'op': 'breakup', 'note': 'A supply yard: cart ruts', 'replace': [W + 'coarse_dirt'],
          'mix': _mix(('gravel', 2), ('dirt_path', 1), ('dirt', 1)), 'density': 0.3, 'scale': 2, 'seed': 'ayard'},
         (-47, -148, -11, -112)),
    ]


C_BOX = {'min': [8, 109], 'max': [50, 151]}
SOUTH_REGIONS = ((-350, 80, -82, 250), (-81, 99, 158, 250))


def _stubs(center, arcs, r0, tag, block):
    ops = []
    for a0, a1 in arcs:
        pts = []
        for k in range(4):
            ang = math.radians(a0 + (a1 - a0) * k / 3)
            r = r0 + _hash(tag, a0, k) * 1.5
            pts.append([round(center[0] + r * math.cos(ang), 1), round(center[1] + r * math.sin(ang), 1),
                        64 + round(_hash(tag + '-top', a0, k) * 2)])
        ops.append(({'op': 'wall', 'note': f'{tag} wall stub ({a0}..{a1} deg), broken', 'points': pts,
                     'thickness': 1.4, 'floor': 57, 'block': block, 'seed': f'{tag}{a0}'}, None))
    return ops


def _south_shape():
    ops = []
    for region in SOUTH_REGIONS:
        ops.append(({'op': 'noise', 'note': 'south flank swells: dry high ground, the ridge and the shoulder',
                     'amplitude': 1.3, 'scale': 18, 'octaves': 3, 'offset': 0.2, 'clamp': [63, 75],
                     'only': [W + 'grass_block'], 'exclude': [C_BOX], 'seed': 'field-s'}, region))
        ops.append(({'op': 'erode', 'note': 'south flank: settle the ridge and shoulder slopes', 'mode': 'thermal',
                     'iterations': 4, 'talus': 1.1, 'rate': 0.5, 'jitter': 0.35, 'clamp': [62, 76],
                     'only': [W + n for n in ('grass_block', 'stone')], 'exclude': [C_BOX], 'seed': 'settle-s'},
                    region))
    return ops


def _south_water():
    """The south ford (tidied 2026-10-31: the old one widened the river into a round pool). A gravel bar crosses
    the channel on a slant from the lane's end (west, z 127) to the C side (east, z 134), 1 under the water, its
    edges ragged; the channel keeps its width, with only a slight easing of the cliff where the bar meets it.
    The ramps down the banks are paths (see _south_paths)."""
    return [({'op': 'river', 'note': 'south ford: slanting gravel bar 1 under the water, channel kept its width',
              'points': [[-7, 126.5, 5], [-2, 128.5, 6], [3, 131, 6], [7, 133.5, 5]], 'width': 6, 'water': 59,
              'depth': 1, 'floor': 55, 'bank': 2, 'bed': _mix(('gravel', 4), ('cobblestone', 1), ('clay', 1)),
              'bankSurface': _mix(('gravel', 3), ('coarse_dirt', 2), ('andesite', 1)), 'wobble': 1.4,
              'wobbleScale': 3, 'seed': 'ford-s2'}, None)]


def _south_paths():
    lane = {'op': 'path', 'width': 5, 'falloff': 2, 'surface': _mix(('dirt_path', 3), ('coarse_dirt', 2), ('gravel', 1)),
            'edge': _mix(('coarse_dirt', 2), ('gravel', 1)), 'edgeDensity': 0.5, 'wobble': 2.0, 'wobbleScale': 6}
    track = {'op': 'path', 'width': 3, 'falloff': 1.5, 'surface': _mix(('dirt_path', 2), ('coarse_dirt', 2),
             ('grass_block', 1)), 'wobble': 1.2, 'wobbleScale': 5}
    return [
        (dict(lane, note='south lane: camp, over Gallows Ridge past the chapel, down to the south ford and C',
              seed='lane-s', points=LANE_S_PTS), None),
        (dict(track, note='ridge foot to the south sally gap, round the fosse end (outside the curtain)',
              seed='sally-s', points=[[-60, 136], [-66, 113], [-84, 105], [-88, 92], [-87, 76], [-84, 62]]), None),
        ({'op': 'path', 'note': 'south sally causeway over the fosse at 63', 'points': [[-85, 62], [-74, 62]],
          'width': 3, 'y': 63, 'surface': _mix(('cobblestone', 2), ('coarse_dirt', 1)),
          'under': _mix(('cobblestone', 2), ('dirt', 1)), 'seed': 'sallyc-s'}, None),
        ({'op': 'path', 'note': 'south ford: west ramp, lane end down the cliff to the bar',
          'points': [[-21, 130, 63], [-14, 128.5, 63], [-9, 127, 61], [-6, 126.5, 59]], 'width': 4, 'falloff': 2.5,
          'surface': _mix(('gravel', 3), ('coarse_dirt', 2), ('dirt_path', 1)), 'under': W + 'gravel',
          'edge': _mix(('gravel', 2), ('coarse_dirt', 1), ('andesite', 1)), 'edgeDensity': 0.6, 'wobble': 1.3,
          'wobbleScale': 3, 'seed': 'fordramp-w'}, None),
        ({'op': 'path', 'note': 'south ford: east ramp, bar up the cliff toward C',
          'points': [[6, 133.5, 59], [9, 134.5, 61], [13, 133, 63], [18, 131, 63]], 'width': 4, 'falloff': 2.5,
          'surface': _mix(('gravel', 3), ('coarse_dirt', 2), ('dirt_path', 1)), 'under': W + 'gravel',
          'edge': _mix(('gravel', 2), ('coarse_dirt', 1), ('andesite', 1)), 'edgeDensity': 0.6, 'wobble': 1.3,
          'wobbleScale': 3, 'seed': 'fordramp-e'}, None),
        (dict(track, note='C to the south postern', seed='postern-s',
              points=[[44, 99], [46, 108], [40, 118], [33, 126]]), None),
    ]


def _chapel():
    """The ridge chapel: a roofless stone shell, walls broken between 75 and 82, the west gable standing tallest,
    a west door, a chancel gap in the east wall."""
    x0, x1, z0, z1 = CHAPEL
    corners = ((x0, z0), (x1, z0), (x1, z1), (x0, z1))
    pts = []
    for i, ((ax, az), (bx, bz)) in enumerate(zip(corners, corners[1:] + corners[:1])):
        n = 4 if ax != bx else 3
        for k in range(n):
            px, pz = ax + (bx - ax) * k / n, az + (bz - az) * k / n
            top = 75 + round(_hash('chapel', i, k) * 5)
            if px == x0:
                top = 82 if k in (1, 2) or n == 3 and k == 0 else 79  # west gable
            pts.append([round(px, 1), round(pz, 1), top])
    mid = (z0 + z1) // 2
    return [({'op': 'wall', 'note': 'Gallows Ridge chapel, roofless shell', 'points': pts, 'closed': True,
              'thickness': 1, 'floor': 57,
              'block': _mix(('stone_bricks', 3), ('mossy_stone_bricks', 2), ('cracked_stone_bricks', 2),
                            ('cobblestone', 1)),
              'exclude': [{'min': [x0, mid - 1], 'max': [x0, mid + 1]}, {'min': [x1, mid - 2], 'max': [x1, mid + 3]}],
              'seed': 'chapel'}, None)]


def _south_built():
    ops = _chapel()
    for gx, gz in GALLOWS:
        gy = _south_ground(gx, gz, GROUND)[0] + 5
        ops.append(({'op': 'fill', 'note': f'gallows beam ({gx},{gz})', 'min': [gx + 1, gy, gz],
                     'max': [gx + 2, gy, gz], 'block': W + 'dark_oak_log[axis=x]'}, (gx + 1, gz, gx + 2, gz)))
    ops += _stubs(POINT_C, ((95, 135), (205, 245), (320, 350)), 21, 'cyard',
                  _mix(('cobblestone', 3), ('mossy_cobblestone', 1), ('andesite', 1)))
    ops.append(({'op': 'fill', 'note': 'south water gate: arch lintel over the river, y 68..71', 'min': [-6, 68, 94],
                 'max': [6, 71, 98], 'block': W + 'stone_bricks'}, (-6, 94, 6, 98)))
    ops.append(({'op': 'clear', 'note': 'south postern through the curtain, clear y 64..70', 'min': [40, 64, 94],
                 'max': [48, 70, 98]}, (40, 94, 48, 98)))
    return ops


def _south_surface():
    return [
        ({'op': 'breakup', 'note': 'ridge and shoulder: rock broken with andesite, cobble and gravel',
          'replace': [W + 'stone'], 'mix': _mix(('andesite', 2), ('cobblestone', 1), ('gravel', 1), ('tuff', 1)),
          'density': 0.5, 'scale': 2, 'seed': 'crag-s'}, (-350, 80, 158, 250)),
        ({'op': 'breakup', 'note': 'dry high ground: thin grass, coarse dirt and stone showing',
          'replace': [W + 'grass_block'], 'mix': _mix(('coarse_dirt', 3), ('rooted_dirt', 1), ('stone', 1)),
          'density': 0.12, 'scale': 4, 'seed': 'dry-s'}, (-81, 99, 158, 250)),
        ({'op': 'breakup', 'note': 'chapel floor: broken flags', 'replace': [W + 'cobblestone'],
          'mix': _mix(('mossy_cobblestone', 2), ('gravel', 1), ('stone_bricks', 1), ('grass_block', 1)),
          'density': 0.4, 'scale': 2, 'seed': 'chapelfloor'}, (CHAPEL[0], CHAPEL[2], CHAPEL[1], CHAPEL[3])),
        ({'op': 'breakup', 'note': 'C pad: trodden ground', 'replace': [W + 'coarse_dirt'],
          'mix': _mix(('gravel', 2), ('dirt_path', 1), ('grass_block', 1)), 'density': 0.3, 'scale': 2,
          'seed': 'cpad'}, (10, 111, 48, 149)),
    ]


# ---------------------------------------------------------------- west column B helper ops

WEST = (-289, -176, -226, 207)  # column B, rows 1..6 (x0, z0, x1, z1)
CAMP_SLOPE = (-300, -100, -250, 100)


def _west_shape():
    """The camp slope settles (thermal, earth and rock only, the shelf and palisade line held)."""
    shelf = {'min': [-400, CAMP[2] - 1], 'max': [CAMP[1] + 1, CAMP[3] + 1]}
    return [({'op': 'erode', 'note': 'camp slope: settle the earth and break the crag', 'mode': 'thermal',
              'iterations': 3, 'talus': 1.2, 'rate': 0.5, 'jitter': 0.4, 'clamp': [UPLAND - 1, CAMP_TOP],
              'only': [W + n for n in ('rooted_dirt', 'stone', 'grass_block')], 'exclude': [shelf],
              'seed': 'settle-camp'}, CAMP_SLOPE)]


def _mantlets():
    """Mantlet row x -235 (layout rows of 6), each moved along z until it stands 7 clear of the road and the lanes.
    (x0, z0, x1, z1, ground y) with a lean of up to a block either way so the row is not ruled."""
    out = []
    for z0 in MANTLET_Z:
        z = z0
        for _ in range(12):
            if min(terrain.nearest(p, MANTLET_X, z + k)[0] for p in (LANE_N_PTS, LANE_S_PTS, ROAD_PTS)
                   for k in (0, 5)) >= 7:
                break
            z += 2 if z0 > 0 else -2
        lean = round((_hash('mantlet', z0) - 0.5) * 2)
        y = column(MANTLET_X, z + 2)[0]
        out.append((MANTLET_X - lean, z, MANTLET_X + lean, z + 5, y))
    return out


def _west_paths():
    """Each mantlet stands on a graded earth pad with a low bank thrown up in front of it (east, toward the
    fortress); the bank is a raised path, so it eases back to the ground."""
    ops = []
    for x0, z0, x1, z1, y in _mantlets():
        ops.append(({'op': 'path', 'note': f'mantlet pad ({MANTLET_X},{z0}..{z1}) at {y}',
                     'points': [[x0, z0 - 1.5], [x1, z1 + 1.5]], 'width': 4, 'falloff': 2, 'y': y,
                     'surface': _mix(('coarse_dirt', 3), ('dirt', 1), ('grass_block', 1)), 'under': W + 'dirt',
                     'wobble': 0.6, 'wobbleScale': 3, 'seed': f'mpad{z0}'}, None))
        ops.append(({'op': 'path', 'note': f'mantlet bank ({MANTLET_X + 2},{z0}..{z1})',
                     'points': [[x0 + 2, z0], [x1 + 2, z1]], 'width': 1.5, 'falloff': 1.5, 'y': y + 1,
                     'surface': _mix(('coarse_dirt', 2), ('rooted_dirt', 1), ('dirt', 1)), 'under': W + 'dirt',
                     'wobble': 0.4, 'wobbleScale': 3, 'seed': f'mbank{z0}'}, None))
    return ops


def _tent(x0, x1, z0, z1, block):
    """A ridge tent, ridge along x: four courses stepping in a block a side from the shelf (73) to the ridge (76)."""
    return [({'op': 'fill', 'note': f'tent ({x0}..{x1}, {z0}..{z1}), course {k}', 'min': [x0, 73 + k, z0 + k],
              'max': [x1, 73 + k, z1 - k], 'block': W + block}, (x0, z0 + k, x1, z1 - k)) for k in range(4)]


def _trebuchet(x0, z0, sign):
    """Trebuchet massing on a 7x7 footprint (x0.., z0 and 6 more toward sign): log base frame, two uprights to an
    axle at 80, the arm cocked low to the west and the counterweight hung under its short east end (82)."""
    zs = [z0 + k * sign for k in range(7)]
    zlo, zhi = min(zs), max(zs)
    za, zb, zm = z0 + sign, z0 + 5 * sign, z0 + 3 * sign
    log = W + 'dark_oak_log'
    ops = [({'op': 'fill', 'note': 'trebuchet base rail', 'min': [x0, 73, zlo], 'max': [x0 + 6, 73, zlo],
             'block': log + '[axis=x]'}, (x0, zlo, x0 + 6, zlo)),
           ({'op': 'fill', 'note': 'trebuchet base rail', 'min': [x0, 73, zhi], 'max': [x0 + 6, 73, zhi],
             'block': log + '[axis=x]'}, (x0, zhi, x0 + 6, zhi)),
           ({'op': 'fill', 'note': 'trebuchet cross beam', 'min': [x0, 73, zlo + 1], 'max': [x0, 73, zhi - 1],
             'block': log + '[axis=z]'}, (x0, zlo + 1, x0, zhi - 1)),
           ({'op': 'fill', 'note': 'trebuchet cross beam', 'min': [x0 + 6, 73, zlo + 1],
             'max': [x0 + 6, 73, zhi - 1], 'block': log + '[axis=z]'}, (x0 + 6, zlo + 1, x0 + 6, zhi - 1))]
    for zu in (za, zb):
        ops.append(({'op': 'fill', 'note': 'trebuchet upright', 'min': [x0 + 3, 74, zu], 'max': [x0 + 3, 79, zu],
                     'block': log}, (x0 + 3, zu, x0 + 3, zu)))
        for dx, y in ((1, 74), (2, 76), (5, 74), (4, 76)):  # A-frame braces
            ops.append(({'op': 'fill', 'note': 'trebuchet brace', 'min': [x0 + dx, y, zu], 'max': [x0 + dx, y, zu],
                         'block': W + 'spruce_planks'}, (x0 + dx, zu, x0 + dx, zu)))
    zlo2, zhi2 = min(za, zb), max(za, zb)
    ops.append(({'op': 'fill', 'note': 'trebuchet axle', 'min': [x0 + 3, 80, zlo2], 'max': [x0 + 3, 80, zhi2],
                 'block': log + '[axis=z]'}, (x0 + 3, zlo2, x0 + 3, zhi2)))
    for dx, y in ((-1, 75), (0, 76), (1, 78), (2, 79), (4, 81), (5, 82)):  # the arm
        ops.append(({'op': 'fill', 'note': 'trebuchet arm', 'min': [x0 + dx, y, zm], 'max': [x0 + dx, y, zm],
                     'block': log + '[axis=x]'},
                    (x0 + dx, zm, x0 + dx, zm)))
    ops.append(({'op': 'fill', 'note': 'trebuchet counterweight', 'min': [x0 + 5, 78, zm - 1],
                 'max': [x0 + 6, 80, zm + 1], 'block': W + 'cobblestone'}, (x0 + 5, zm - 1, x0 + 6, zm + 1)))
    return ops


TENTS = (((-290, -282, -60, -53), 'white_wool'), ((-290, -282, -30, -23), 'light_gray_wool'),
         ((-290, -282, 22, 29), 'white_wool'), ((-290, -282, 53, 60), 'brown_wool'))
# the camp's west half (column A, Kort 2026-10-31): two more tents either side of the Dawn spawn's street
TENTS_W = (((-325, -317, -44, -37), 'brown_wool'), ((-325, -317, 37, 44), 'light_gray_wool'))
DAWN = (-316, -312, -2, 2)  # spawn-dawn protect lock, y 73..90 (Kort 2026-10-31): stands on the camp shelf at 72


def _west_built():
    """Camp palisade (east run and the east ends of the north and south runs), its gates, the tents and trebuchets
    on the shelf edge, and the mantlet row."""
    ops = []
    stakes = _mix(('dark_oak_log', 3), ('spruce_log', 2), ('stripped_dark_oak_log', 1))
    gates = [{'min': [CAMP[1] - 1, gz - hw], 'max': [CAMP[1] + 1, gz + hw]} for gz, hw in CAMP_GATES]
    east = [[CAMP[1], z, 74 + round(_hash('stake', z))] for z in range(CAMP[2], CAMP[3] + 1, 6)]
    ops.append(({'op': 'wall', 'note': 'camp palisade, east run, three gates', 'points': east, 'thickness': 1,
                 'floor': 69, 'block': stakes, 'merlons': True, 'exclude': gates, 'seed': 'palisade-e'}, None))
    for zc in (CAMP[2], CAMP[3]):
        run = [[x, zc, 74 + round(_hash('stake', x, zc))] for x in range(CAMP[1], CAMP[0] - 1, -5)]
        ops.append(({'op': 'wall', 'note': f'camp palisade, run z {zc}', 'points': run, 'thickness': 1,
                     'floor': 69, 'block': stakes, 'merlons': True, 'seed': f'palisade{zc}'}, None))
    for gz, hw in CAMP_GATES:
        tall, w = (79, 1) if gz == 0 else (77, 0)
        for pz in (gz - hw - 1 - w, gz + hw + 1 + w):
            ops.append(({'op': 'fill', 'note': f'camp gate post z {pz}', 'min': [CAMP[1] - w, 69, pz - w],
                         'max': [CAMP[1] + w, tall, pz + w], 'block': W + 'dark_oak_log'},
                        (CAMP[1] - w, pz - w, CAMP[1] + w, pz + w)))
    ops.append(({'op': 'fill', 'note': "King's Road gate lintel", 'min': [CAMP[1], 78, -9], 'max': [CAMP[1], 78, 9],
                 'block': W + 'dark_oak_log[axis=z]'}, (CAMP[1], -9, CAMP[1], 9)))
    for (x0, x1, z0, z1), block in TENTS + TENTS_W:
        ops += _tent(x0, x1, z0, z1, block)
    west_run = [[CAMP[0], z, 74 + round(_hash('stake', CAMP[0], z))] for z in range(CAMP[2], CAMP[3] + 1, 6)]
    ops.append(({'op': 'wall', 'note': 'camp palisade, west run under the rim', 'points': west_run, 'thickness': 1,
                 'floor': 69, 'block': stakes, 'merlons': True, 'seed': 'palisade-w'}, None))
    ops += _trebuchet(-286, -50, 1)
    ops += _trebuchet(-286, 50, -1)
    for x0, z0, x1, z1, y in _mantlets():
        ops.append(({'op': 'wall', 'note': f'siege mantlet ({MANTLET_X},{z0}..{z1})', 'points': [[x0, z0], [x1, z1]],
                     'thickness': 1.2, 'floor': y + 1, 'top': y + 2,
                     'block': _mix(('spruce_planks', 3), ('spruce_log', 1), ('stripped_spruce_log', 1)),
                     'seed': f'mantlet{z0}'}, None))
    return ops


def _west_surface():
    return [
        ({'op': 'breakup', 'note': 'west rim: grass and earth over the stone, rock left on the steep',
          'replace': [W + 'stone', W + 'andesite'],
          'mix': _mix(('grass_block', 6), ('coarse_dirt', 2), ('stone', 2), ('mossy_cobblestone', 1), ('gravel', 1)),
          'density': 0.75, 'scale': 3, 'seed': 'rim-skin'}, (-350, -176, -300, 207)),
        ({'op': 'breakup', 'note': 'camp shelf: trodden earth, gravel, paths and tufts',
          'replace': [W + 'coarse_dirt'],
          'mix': _mix(('dirt_path', 2), ('gravel', 1), ('dirt', 2), ('grass_block', 2), ('packed_mud', 1)),
          'density': 0.4, 'scale': 2, 'seed': 'campfloor'}, (-350, CAMP[2] - 7, CAMP[1] + 7, CAMP[3] + 7)),
        ({'op': 'breakup', 'note': 'camp slope: grass over the thrown earth', 'replace': [W + 'rooted_dirt'],
          'mix': _mix(('grass_block', 5), ('coarse_dirt', 2), ('dirt', 1)), 'density': 0.7, 'scale': 2,
          'seed': 'rampart'}, CAMP_SLOPE),
        ({'op': 'breakup', 'note': 'west upland rock: andesite, cobble, gravel and moss', 'replace': [W + 'stone'],
          'mix': _mix(('andesite', 3), ('cobblestone', 1), ('gravel', 1), ('tuff', 1), ('mossy_cobblestone', 1)),
          'density': 0.5, 'scale': 2, 'seed': 'crag-w'}, WEST),
        ({'op': 'breakup', 'note': 'west upland: grass worn round the rock', 'replace': [W + 'grass_block'],
          'mix': _mix(('coarse_dirt', 2), ('rooted_dirt', 1), ('gravel', 1)), 'near': [W + 'stone', W + 'andesite'],
          'width': 1.5, 'density': 0.5, 'scale': 1, 'seed': 'rockfoot-w'}, WEST),
    ]


# ---------------------------------------------------------------- east bailey, column G (G2..G4)

BAILEY = (31, -93, 97, 93)  # inside the curtains, east of the river's bend (x0, z0, x1, z1); the yard is andesite at 63
BAILEY_ROAD = [[6, 0], [24, 1], [44, -1], [62, 1], [80, -1], [97, 0]]  # defenders' road: B deck to the east postern
POSTERN_S_TRACK = [[60, 1], [57, 18], [55, 40], [50, 62], [46, 80], [44, 93]]  # road to the south postern (40..48)
NORTH_WALL_TRACK = [[70, -1], [72, -30], [76, -60], [80, -84], [90, -90]]  # road to the NE corner tower stair
SHEDS = ((38, 50, -93, -87, 'n1'), (58, 70, -93, -88, 'n2'), (64, 76, 87, 93, 's1'))  # lean-tos on the curtain
STORES = ((80, 88, 30, 36, 'yard store'), (36, 44, -70, -64, 'north store'))  # stacked stores, low cover
BAILEY_KEEP_OFF = 6  # cart and crate cover keeps this far off the road and tracks


def _bailey_track_dist(x, z):
    return min(terrain.nearest(p, x, z)[0] for p in (BAILEY_ROAD, POSTERN_S_TRACK, NORTH_WALL_TRACK))


def _bailey_cover():
    """Scattered carts and crate stacks across the yard, one at most per 12-block cell where the hash allows,
    each kept off the road, the tracks, the river bank, the sheds and stores. (x0, z0, x1, z1, top, kind)."""
    out = []
    for cz in range(BAILEY[1] // 12, BAILEY[3] // 12 + 1):
        for cx in range(BAILEY[0] // 12, BAILEY[2] // 12 + 1):
            if _hash('bcover', cx, cz) > 0.62:
                continue
            x = cx * 12 + 2 + int(_hash('bcx', cx, cz) * 7)
            z = cz * 12 + 2 + int(_hash('bcz', cx, cz) * 7)
            along = _hash('bcdir', cx, cz) > 0.5
            big = _hash('bcbig', cx, cz) > 0.7
            w, d = ((4, 3) if big else (3, 2)) if along else ((3, 4) if big else (2, 3))
            if not (BAILEY[0] + 6 <= x and x + w <= BAILEY[2] - 4 and BAILEY[1] + 3 <= z and z + d <= BAILEY[3] - 3):
                continue
            if min(_bailey_track_dist(x + i, z + k) for i in (0, w) for k in (0, d)) < BAILEY_KEEP_OFF:
                continue
            if terrain.nearest(RIVER_PTS, x, z)[0] < 14:
                continue
            if any(_box_dist(x, z, (a - 3 - w, b + 3, c - 3 - d, e + 3)) >= 0 for a, b, c, e, _ in SHEDS + STORES):
                continue
            if any(_box_dist(x, z, (o[0] - 3 - w, o[2] + 3, o[1] - 3 - d, o[3] + 3)) >= 0 for o in out):
                continue
            cart = _hash('bckind', cx, cz) > 0.45
            out.append((x, z, x + w - 1, z + d - 1, 64 if cart else 64 + (_hash('bch', cx, cz) > 0.5),
                        'cart' if cart else 'crates'))
    return out


def _bailey_shape():
    """The bailey yard is not a table: a low swell of a block where carts have not packed it flat."""
    cover = [{'min': [o[0] - 1, o[1] - 1], 'max': [o[2] + 1, o[3] + 1]} for o in _bailey_cover()]
    cover += [{'min': [a - 1, c - 1], 'max': [b + 1, e + 1]} for a, b, c, e, _ in SHEDS + STORES]
    return [({'op': 'noise', 'note': 'east bailey: low swells in the andesite yard', 'amplitude': 1.3, 'scale': 14,
              'octaves': 2, 'offset': -0.1, 'clamp': [63, 64], 'only': [W + 'andesite'], 'exclude': cover,
              'seed': 'bailey-swell'}, BAILEY)]


def _bailey_paths():
    road = {'op': 'path', 'width': 5, 'falloff': 2, 'y': 63, 'surface': _mix(('dirt_path', 3), ('gravel', 2),
            ('coarse_dirt', 1)), 'under': W + 'gravel', 'edge': _mix(('gravel', 2), ('coarse_dirt', 1)),
            'edgeDensity': 0.5, 'wobble': 1.5, 'wobbleScale': 6}
    track = {'op': 'path', 'width': 3, 'falloff': 1.5, 'y': 63, 'surface': _mix(('gravel', 2), ('coarse_dirt', 2),
             ('dirt_path', 1)), 'under': W + 'gravel', 'wobble': 1.0, 'wobbleScale': 5}
    return [
        (dict(road, note="defenders' road: B deck through the east bailey to the east postern", seed='bailey-road',
              points=BAILEY_ROAD), None),
        (dict(track, note='bailey track: road to the south postern', seed='postern-in-s', points=POSTERN_S_TRACK),
         None),
        (dict(track, note='bailey track: road to the north-east corner tower', seed='nwall-track',
              points=NORTH_WALL_TRACK), None),
    ]


def _bailey_built():
    """Rough massing only (the structure pass makes them sheds, carts and stores): lean-to sheds against the north
    and south curtains, two stacked stores and scattered carts and crates for low cover."""
    ops = []
    for x0, x1, z0, z1, tag in SHEDS:
        wall_z, inner, step = (z0, z1, 1) if z0 < 0 else (z1, z0, -1)  # roof falls from the curtain into the yard
        for k, y in enumerate((68, 67, 66, 66, 65, 65)):
            z = wall_z + k * step
            if (z - inner) * step > 0:
                break
            ops.append(({'op': 'fill', 'note': f'lean-to shed {tag}, course {k}', 'min': [x0, 64, z],
                         'max': [x1, y, z], 'block': W + ('spruce_planks' if k else 'stripped_spruce_log[axis=x]')},
                        (x0, z, x1, z)))
        ops.append(({'op': 'clear', 'note': f'lean-to shed {tag}: open front', 'min': [x0 + 1, 64, inner],
                     'max': [x1 - 1, 65, inner]}, (x0 + 1, inner, x1 - 1, inner)))
    for x0, x1, z0, z1, tag in STORES:
        ops.append(({'op': 'fill', 'note': f'{tag}: stacked barrels and crates, low cover', 'min': [x0, 64, z0],
                     'max': [x1, 65, z1], 'block': W + 'barrel[facing=up]'}, (x0, z0, x1, z1)))
        ops.append(({'op': 'fill', 'note': f'{tag}: upper stack', 'min': [x0 + 2, 66, z0 + 1],
                     'max': [x1 - 3, 66, z1 - 2], 'block': W + 'spruce_planks'}, (x0 + 2, z0 + 1, x1 - 3, z1 - 2)))
    for x0, z0, x1, z1, top, kind in _bailey_cover():
        if kind == 'cart':
            ops.append(({'op': 'fill', 'note': f'cart ({x0},{z0})', 'min': [x0, 64, z0], 'max': [x1, 64, z1],
                         'block': W + 'spruce_planks'}, (x0, z0, x1, z1)))
            ops.append(({'op': 'fill', 'note': f'cart ({x0},{z0}) load', 'min': [x0, 65, z0], 'max': [x0, 65, z0],
                         'block': W + 'hay_block'}, (x0, z0, x0, z0)))
        else:
            ops.append(({'op': 'fill', 'note': f'crate stack ({x0},{z0})', 'min': [x0, 64, z0], 'max': [x1, top, z1],
                         'block': W + 'barrel[facing=up]'}, (x0, z0, x1, z1)))
    return ops


def _bailey_surface():
    return [
        ({'op': 'breakup', 'note': 'bailey: wall foot, gravel and moss where carts do not reach',
          'replace': [W + 'andesite'],
          'mix': _mix(('gravel', 2), ('mossy_cobblestone', 1), ('coarse_dirt', 2), ('grass_block', 1)),
          'near': [W + 'stone_bricks'], 'width': 2.5, 'density': 0.55, 'scale': 2, 'seed': 'bailey-foot'}, BAILEY),
        ({'op': 'breakup', 'note': 'bailey: churned ground along the road and tracks', 'replace': [W + 'andesite'],
          'mix': _mix(('coarse_dirt', 3), ('packed_mud', 2), ('gravel', 2), ('mud', 1)), 'near': [W + 'dirt_path'],
          'width': 4, 'density': 0.6, 'scale': 2, 'seed': 'bailey-churn'}, BAILEY),
        ({'op': 'breakup', 'note': 'bailey: old flagging and mud in broad patches', 'replace': [W + 'andesite'],
          'mix': _mix(('stone', 2), ('cobblestone', 2), ('packed_mud', 2), ('coarse_dirt', 1), ('polished_andesite', 1)),
          'density': 0.3, 'scale': 7, 'seed': 'bailey-patch'}, BAILEY),
    ]


# ---------------------------------------------------------------- east column H (H1..H6)

EAST = (95, -98, 158, 98)  # between the curtain lines (x0, z0, x1, z1); the flanks beyond use the north/south ops
EAST_ROAD = [[103, 0], [110, 0.5], [118, -0.5], [124, 0]]  # defenders' road on from the postern to the Dusk spawn (starts
# at x 103 so the op stays clear of column G; the bailey road's end runs through the postern itself)
NE_SPUR = [[105, -1], [106, -24], [107, -44], [116, -50], [126, -54], [125, -66]]  # west of the hall to the NE yard
SE_SPUR = [[118, 2], [124, 14], [132, 34], [138, 52], [140, 62]]  # across the court to the SE yard gate
YARD_GATES = ((NE_YARD, (126, -54)), (SE_YARD, (138, 54)))  # gate centre on each yard's court-side wall


def _east_shape():
    return [({'op': 'noise', 'note': 'east ground: low swells between the court and the curtain lines',
              'amplitude': 1.1, 'scale': 16, 'octaves': 2, 'offset': -0.05, 'clamp': [63, 64],
              'only': [W + 'grass_block'], 'exclude': [DUSK_EX], 'seed': 'east-swell'}, (103, -98, 158, 98))]


def _east_paths():
    road = {'op': 'path', 'width': 5, 'falloff': 2, 'y': 63, 'surface': _mix(('dirt_path', 3), ('gravel', 2),
            ('coarse_dirt', 1)), 'under': W + 'gravel', 'edge': _mix(('gravel', 2), ('coarse_dirt', 1)),
            'edgeDensity': 0.5, 'wobble': 1.5, 'wobbleScale': 6, 'exclude': [DUSK_EX]}
    spur = {'op': 'path', 'width': 4, 'falloff': 1.5, 'surface': _mix(('dirt_path', 3), ('coarse_dirt', 1)),
            'under': W + 'dirt', 'wobble': 0.5, 'exclude': [DUSK_EX]}
    return [
        (dict(road, note="defenders' road: east postern across Donjon Court to the Dusk spawn", seed='east-road',
              points=EAST_ROAD), None),
        (dict(spur, note='supply spur NE: west of the barracks hall to the NE yard gate and house', seed='spur-ne',
              points=NE_SPUR), None),
        (dict(spur, note='supply spur SE: across the court to the SE yard gate and house', seed='spur-se',
              points=SE_SPUR), None),
    ]


def _east_built():
    """Rough massing: the east postern through the curtain (the roads cut the wall near z 0, so the curtain is made
    good either side and a lintel spans the gate), the barracks hall with a stepped roof, the two supply houses and
    their yard walls."""
    ops = [
        ({'op': 'fill', 'note': 'east curtain made good north of the postern, y 57..71', 'min': [98, 57, -14],
          'max': [102, 71, -7], 'block': W + 'stone_bricks'}, (98, -14, 102, -7)),
        ({'op': 'fill', 'note': 'east curtain made good south of the postern, y 57..71', 'min': [98, 57, 7],
          'max': [102, 71, 14], 'block': W + 'stone_bricks'}, (98, 7, 102, 14)),
        ({'op': 'clear', 'note': 'east postern through the curtain, clear y 64..70', 'min': [98, 64, -6],
          'max': [102, 70, 6]}, (98, -6, 102, 6)),
        ({'op': 'fill', 'note': 'east postern lintel, y 71', 'min': [98, 71, -6], 'max': [102, 71, 6],
          'block': W + 'stone_bricks'}, (98, -6, 102, 6)),
    ]
    x0, x1, z0, z1 = HALL
    ops.append(({'op': 'fill', 'note': 'barracks hall volume, y 57..74', 'min': [x0, 57, z0], 'max': [x1, 74, z1],
                 'block': W + 'stone_bricks'}, (x0, z0, x1, z1)))
    for k, y in enumerate(range(75, 80)):  # roof courses step in from both long sides to the ridge at z -30
        a, b = z0 + 1 + 2 * k, z1 - 1 - 2 * k
        ops.append(({'op': 'fill', 'note': f'barracks hall roof course {k}, y {y}', 'min': [x0, y, a],
                     'max': [x1, y, b], 'block': W + 'deepslate_tiles'}, (x0, a, x1, b)))
    for x0, x1, z0, z1 in EAST_HOUSES:
        ops.append(({'op': 'fill', 'note': 'east supply house volume', 'min': [x0, 57, z0], 'max': [x1, 70, z1],
                     'block': W + 'stone_bricks'}, (x0, z0, x1, z1)))
    for (x0, x1, z0, z1), (gx, gz) in YARD_GATES:
        ops.append(({'op': 'wall', 'note': 'east supply yard wall, gate on the court side',
                     'points': [[x0, z0], [x1, z0], [x1, z1], [x0, z1]], 'closed': True, 'thickness': 1,
                     'floor': 57, 'top': 66, 'block': W + 'cobblestone',
                     'exclude': [{'min': [gx - 2, gz - 1], 'max': [gx + 2, gz + 1]}]}, None))
    return ops


def _east_surface():
    court = (DONJON[0], DONJON[2], DONJON[1], DONJON[3])
    return [
        ({'op': 'breakup', 'note': 'Donjon Court: worn flags, deepslate and gravel',
          'replace': [W + 'polished_blackstone'],
          'mix': _mix(('polished_blackstone_bricks', 2), ('cobbled_deepslate', 2), ('blackstone', 1), ('gravel', 1)),
          'density': 0.35, 'scale': 2, 'exclude': [DUSK_EX], 'seed': 'donjon'}, court),
        ({'op': 'breakup', 'note': 'Donjon Court: wear along the road and spurs',
          'replace': [W + 'polished_blackstone'],
          'mix': _mix(('gravel', 2), ('coarse_dirt', 1), ('cobbled_deepslate', 1)), 'near': [W + 'dirt_path'],
          'width': 3, 'density': 0.55, 'scale': 2, 'exclude': [DUSK_EX], 'seed': 'donjon-wear'}, court),
        ({'op': 'breakup', 'note': 'east yards: cart ruts', 'replace': [W + 'coarse_dirt'],
          'mix': _mix(('gravel', 2), ('dirt_path', 1), ('dirt', 1)), 'density': 0.3, 'scale': 2, 'seed': 'eyards'},
         EAST),
        ({'op': 'breakup', 'note': 'east ground: worn grass by the walls and yards', 'replace': [W + 'grass_block'],
          'mix': _mix(('coarse_dirt', 2), ('rooted_dirt', 1), ('gravel', 1)),
          'near': [W + 'stone_bricks', W + 'cobblestone', W + 'polished_blackstone'], 'width': 2.5,
          'density': 0.5, 'scale': 2, 'seed': 'eastfoot'}, EAST),
        ({'op': 'breakup', 'note': 'east ground: worn patches', 'replace': [W + 'grass_block'],
          'mix': _mix(('coarse_dirt', 2), ('rooted_dirt', 1)), 'density': 0.1, 'scale': 4, 'seed': 'eastwear'}, EAST),
    ]


GORGE = (159, -176, 222, 207)  # column I, rows 1..6 (x0, z0, x1, z1)


EDGE_ROWS = ((-350, -250, 158, -177), (-350, 208, 158, 249))  # rows 0 and 7 west of the gorge
GORGE_FAR = ((159, -250, 349, -177), (159, 208, 349, 249), (223, -176, 349, 207))  # gorge in rows 0, 7 and J..K


def _edge_surface():
    """Rows 0 and 7 (North Mountains, South Ridge): the andesite skinned like the west rim; the gorge's far
    reaches (rows 0 and 7 east of the parapet line, columns J..K) get the gorge's own face and bed breakups."""
    ops = [({'op': 'breakup', 'note': 'mountains and ridge: grass and earth over the rock, rock left on the steep',
             'replace': [W + 'andesite'],
             'mix': _mix(('grass_block', 6), ('coarse_dirt', 2), ('stone', 2), ('mossy_cobblestone', 1),
                         ('gravel', 1)),
             'density': 0.75, 'scale': 3, 'seed': 'edge-skin'}, r) for r in EDGE_ROWS]
    for r in GORGE_FAR:
        ops += [(dict(op), r) for op, _ in _gorge_surface()[:2]]
    return ops


def _gorge_surface():
    return [
        ({'op': 'breakup', 'note': 'gorge faces: andesite, tuff and gravel in the crag', 'replace': [W + 'stone'],
          'mix': _mix(('andesite', 3), ('tuff', 2), ('cobblestone', 1), ('gravel', 1), ('stone', 2)),
          'density': 0.6, 'scale': 2, 'depth': 3, 'seed': 'gorge-face'}, GORGE),
        ({'op': 'breakup', 'note': 'gorge floor: scree, cobble and coarse dirt', 'replace': [W + 'gravel'],
          'mix': _mix(('cobblestone', 2), ('andesite', 2), ('coarse_dirt', 1), ('tuff', 1)),
          'density': 0.35, 'scale': 2, 'seed': 'gorge-bed'}, GORGE),
        ({'op': 'breakup', 'note': 'rim: grass worn along the parapet and curtain foot', 'replace': [W + 'grass_block'],
          'mix': _mix(('coarse_dirt', 2), ('rooted_dirt', 1), ('gravel', 1)),
          'near': [W + 'stone_bricks', W + 'stone'], 'width': 2, 'density': 0.5, 'scale': 2, 'seed': 'rimfoot'},
         GORGE),
    ]


def features():
    """Every helper op on the map, in apply order. Each is clipped to the section it is planned for; noise, erode
    and breakup carry a region (x0, z0, x1, z1) that is intersected with the section."""
    f = []
    # -- shape: swells on the field and upland, hummocks in the Mire, then slopes settled by thermal erosion
    f.append(({'op': 'noise', 'note': 'field and upland swells', 'amplitude': 1.1, 'scale': 20, 'octaves': 3,
               'offset': 0.15, 'clamp': [63, 68], 'only': [W + 'grass_block'], 'seed': 'field'}, (-350, -48, -82, 79)))
    f.append(({'op': 'noise', 'note': 'Mire hummocks', 'amplitude': 0.9, 'scale': 6, 'offset': -0.25,
               'clamp': [61, 62], 'only': [W + 'mud'], 'seed': 'hummock'}, (-350, -250, -98, 280)))
    f.append(({'op': 'erode', 'note': 'settle the Mire edge and the swells', 'mode': 'thermal', 'iterations': 3,
               'talus': 1.0, 'rate': 0.5, 'jitter': 0.3, 'only': [W + 'grass_block', W + 'mud'], 'seed': 'settle'},
              (-350, -250, -98, 280)))
    f += _north_shape()
    f += _south_shape()
    f += _west_shape()
    f += _bailey_shape()
    f += _east_shape()
    # -- water: bog pools, the flooded trench in the Mire, the fosse, the river
    f += [(op, None) for op in _pools()]
    f += _trench150()
    face = _mix(('stone', 3), ('cobblestone', 2), ('coarse_dirt', 2), ('dirt', 1))
    fosse = []  # inner edge on the curtain foot (x -75); the outer lip wanders between x -80 and -83
    for z in range(-100, 101, 9):
        wd = 6 + round(_hash('fosse-w', z) * 3)
        fosse.append([-74.5 - wd / 2, z, wd])
    f.append(({'op': 'river', 'note': 'fosse x -80..-75 (outer lip ragged), water 61, causeway z -8..8',
               'points': fosse, 'width': 6, 'water': 61, 'depth': 3, 'floor': 56, 'wobble': 2.2, 'wobbleScale': 5,
               'bankSurface': face, 'faces': True, 'seed': 'fosse',
               'exclude': [{'min': [-78, -100], 'max': [-66, -92]}, {'min': [-78, 92], 'max': [-66, 100]}, {'min': [-83, -8], 'max': [-74, 8]}, {'min': [-80, 9], 'max': [-70, 17]},
                           {'min': [-80, -17], 'max': [-70, -9]}, {'min': [-74, -200], 'max': [-60, 200]}]}, None))
    cliff = _mix(('stone', 3), ('andesite', 2), ('mud', 2), ('packed_mud', 1), ('cobblestone', 1))
    f.append(({'op': 'river', 'note': 'river: cut channel, water 59, bends east around the keep court',
               'points': RIVER_PTS,
               'width': 11, 'water': 59, 'depth': 3, 'floor': 55, 'wobble': 1.8, 'wobbleScale': 7,
               'bankSurface': cliff, 'faces': True, 'seed': 'river',
               'exclude': [{'min': [COURT[0], 20], 'max': [24, 48]}]}, None))
    f += _north_water()
    f += _south_water()
    # -- roads: the King's Road on the ground, across the Mire on a causeway at 63, on to the front door
    road = {'width': 7, 'surface': W + 'dirt_path', 'edge': _mix(('coarse_dirt', 2), ('gravel', 1)), 'falloff': 2,
            'edgeDensity': 0.5, 'wobble': 3.0, 'wobbleScale': 5}
    f.append((dict(road, op='path', note="King's Road, upland to the Mire", points=ROAD_PTS,
                   seed='road-w'), None))
    f.append((dict(road, op='path', note="King's Road causeway over the Mire at 63", points=[[-194, 0], [-116, 0]],
                   y=63, falloff=4, under=_mix(('cobblestone', 1), ('dirt', 2)), seed='causeway'), None))
    f.append((dict(road, op='path', note="King's Road, killing field to the fosse causeway",
                   points=[[-120, 0], [-83, 0]], seed='road-e'), None))
    f += _north_paths()
    f += _south_paths()
    f += _west_paths()
    f.append((dict(road, op='path', note="camp street: the Dawn spawn (x -314) out to the King's Road gate",
                   points=[[-322, 0], [-290, 0]], width=5, wobble=1.0, seed='camp-street'), (-350, -20, -290, 20)))
    f += _bailey_paths()
    spur = {'op': 'path', 'width': 4, 'falloff': 1.5, 'surface': _mix(('dirt_path', 3), ('coarse_dirt', 1)),
            'under': W + 'dirt', 'wobble': 0.5}
    f.append((dict(spur, note='supply spur SW, raised track over the Mire', seed='spur-sw',
                   points=[[-199, 2, 63], [-190, 10, 63], [-183, 17, 62], [-170, 30, 62], [-155, 45, 62],
                           [-146, 54, 62], [-141, 60, 63], [-135, 66, 63], [-133, 70, 63]]), None))
    f.append((dict(spur, note='supply spur NW, raised track over the Mire', seed='spur-nw',
                   points=[[-199, -2, 63], [-190, -10, 63], [-183, -17, 62], [-170, -30, 62], [-161, -44, 62],
                           [-158, -52, 62], [-155, -60, 63], [-149, -66, 63], [-147, -72, 63]]), None))
    f += _trench105()
    f += _east_paths()
    # -- surface-only: close the cliff faces, cut banks and steps the shaping opened in the hollow columns
    f.append(({'op': 'seal', 'note': 'fill the gaps a cliff, cut bank or step would show (surface-only maps)'},
              (-350, -250, 349, 249)))
    # the B deck goes down after the roads so the defenders' road meets its planks instead of paving them
    f.append(({'op': 'fill', 'note': 'B deck: spruce planks at 63 over the channel', 'min': [-5, 63, -12],
               'max': [5, 63, 12], 'block': W + 'spruce_planks'}, (-5, -12, 5, 12)))
    # -- built massing: supply houses and yard walls, the curtain stretch, the inner curtain closures
    for i, (x0, x1, z0, z1) in enumerate(SUPPLY_HOUSES):
        f.append(({'op': 'fill', 'note': 'supply house volume', 'min': [x0, 57, z0], 'max': [x1, 70, z1],
                   'block': W + 'stone_bricks'}, (x0, z0, x1, z1)))
    for i, (x0, x1, z0, z1) in enumerate(SUPPLY_YARDS):
        gz = 60 if z0 > 0 else -60  # gate where the spur comes in through the west wall
        f.append(({'op': 'wall', 'note': 'supply yard wall', 'points': [[x0, z0], [x1, z0], [x1, z1], [x0, z1]],
                   'closed': True, 'thickness': 1, 'floor': 57, 'top': 66, 'block': W + 'cobblestone',
                   'exclude': [{'min': [x0 - 1, gz - 2], 'max': [x0 + 1, gz + 2]}]}, None))
    f.append(({'op': 'wall', 'note': 'front-door curtain stretch z -27..-18', 'points': [[-72, -19.5], [-72, -26]],
               'thickness': 5, 'floor': 57, 'top': 71, 'block': W + 'stone_bricks',
               'exclude': [{'min': [-74, -96], 'max': [-70, -28]}, {'min': [-80, -17], 'max': [-70, -9]}]}, None))
    f.append(({'op': 'wall', 'note': 'front-door curtain stretch z 18..27', 'points': [[-72, 19.5], [-72, 26]],
               'thickness': 5, 'floor': 57, 'top': 71, 'block': W + 'stone_bricks',
               'exclude': [{'min': [-74, 28], 'max': [-70, 96]}, {'min': [-80, 9], 'max': [-70, 17]}]}, None))
    gate = {'min': [-40, -23], 'max': [-34, 23]}
    court = {'min': [COURT[0], COURT[1]], 'max': [COURT[2], COURT[3]]}
    ground = [W + n for n in ('packed_mud', 'coarse_dirt', 'mud', 'grass_block', 'andesite', 'stone', 'cobblestone', 'stone_bricks')]
    f.append(({'op': 'wall', 'note': 'inner curtain closure north: gate end to z -30, east to the river cliff',
               'points': [[-38, -23.5], [-38, -28.5]], 'thickness': 5, 'floor': 57, 'top': INNER,
               'block': W + 'stone_bricks', 'exclude': [gate]}, None))
    f.append(({'op': 'wall', 'note': 'inner curtain closure north, east run z -30..-28', 'points': [[-38, -29],
               [4, -29]], 'thickness': 3, 'floor': 57, 'top': INNER, 'block': W + 'stone_bricks', 'only': ground,
               'exclude': [gate]}, None))
    f.append(({'op': 'wall', 'note': 'inner curtain closure south: gate end to z 30', 'points': [[-38, 23.5],
               [-38, 28.5]], 'thickness': 5, 'floor': 57, 'top': INNER, 'block': W + 'stone_bricks',
               'exclude': [gate]}, None))
    f.append(({'op': 'wall', 'note': 'inner curtain closure south, east run z 28..30 to the court wall',
               'points': [[-38, 29], [-24, 29]], 'thickness': 3, 'floor': 57, 'top': INNER,
               'block': W + 'stone_bricks', 'exclude': [gate, court]}, None))
    f += _north_built()
    f += _south_built()
    f += _west_built()
    f += _bailey_built()
    f += _east_built()
    # -- surfaces: blend the boundaries so no material ends on a ruler line
    f.append(({'op': 'breakup', 'note': 'river banks: mud, gravel and moss near the water',
               'replace': [W + n for n in ('mud', 'packed_mud', 'andesite', 'coarse_dirt', 'grass_block')],
               'mix': _mix(('mud', 4), ('coarse_dirt', 2), ('gravel', 2), ('moss_block', 1)), 'near': [W + 'water'],
               'width': 3, 'density': 0.6, 'scale': 2, 'seed': 'riverbank'}, (-34, -250, 97, 280)))
    f.append(({'op': 'breakup', 'note': 'fosse lip: trampled earth', 'replace': [W + 'grass_block'],
               'mix': _mix(('coarse_dirt', 3), ('dirt', 1), ('mud', 1)), 'near': [W + 'water'], 'width': 2.5,
               'density': 0.65, 'scale': 2, 'seed': 'fosselip'}, (-97, -250, -74, 280)))
    f.append(({'op': 'breakup', 'note': 'Outer Yard: worn gravel', 'replace': [W + 'gravel'],
               'mix': _mix(('coarse_dirt', 2), ('cobblestone', 1), ('gravel', 2)), 'density': 0.3, 'scale': 2,
               'seed': 'outeryard'}, (-69, -93, -41, 93)))
    f.append(({'op': 'breakup', 'note': 'inner yard: packed mud worn through', 'replace': [W + 'packed_mud'],
               'mix': _mix(('coarse_dirt', 2), ('mud', 1), ('gravel', 1)), 'density': 0.25, 'scale': 3,
               'seed': 'inneryard'}, (-33, -24, -9, 20)))
    f.append(({'op': 'breakup', 'note': 'east bailey: andesite yard with gravel and cobble',
               'replace': [W + 'andesite'], 'mix': _mix(('gravel', 2), ('cobblestone', 1), ('stone', 1)),
               'density': 0.28, 'scale': 2, 'seed': 'bailey'}, (9, -96, 97, 96)))
    f.append(({'op': 'breakup', 'note': 'Mire margin: grass going to mud and moss', 'replace': [W + 'grass_block'],
               'mix': _mix(('coarse_dirt', 2), ('mud', 2), ('moss_block', 1)), 'near': [W + 'mud', W + 'water'],
               'width': 3, 'density': 0.7, 'scale': 2, 'seed': 'miremargin'}, (-350, -250, -98, 280)))
    f.append(({'op': 'breakup', 'note': 'Mire surface: moss, podzol and peat', 'replace': [W + 'mud'],
               'mix': _mix(('moss_block', 3), ('podzol', 2), ('coarse_dirt', 1), ('mud', 1)), 'density': 0.3,
               'scale': 3, 'seed': 'miresurface'}, (-350, -250, -98, 280)))
    f.append(({'op': 'breakup', 'note': 'field: worn patches', 'replace': [W + 'grass_block'],
               'mix': _mix(('coarse_dirt', 2), ('rooted_dirt', 1)), 'density': 0.1, 'scale': 4, 'seed': 'fieldwear'},
              (-350, -250, -81, 280)))
    f.append(({'op': 'breakup', 'note': 'supply yards: cart ruts', 'replace': [W + 'coarse_dirt'],
               'mix': _mix(('gravel', 2), ('dirt_path', 1), ('dirt', 1)), 'density': 0.3, 'scale': 2,
               'seed': 'yards'}, (-160, -95, -105, 95)))
    f += _north_surface()
    f += _south_surface()
    f += _west_surface()
    f += _bailey_surface()
    f += _east_surface()
    f += _gorge_surface()
    f += _edge_surface()
    return f


def _clip(op, region, box):
    """The op clipped to the section box (x0, z0, x1, z1), or None when it cannot touch the section."""
    x0, z0, x1, z1 = box
    if op['op'] in ('fill', 'clear'):
        (a, _, b), (c, _, d) = op['min'], op['max']
        lo, hi = (max(a, x0), max(b, z0)), (min(c, x1), min(d, z1))
        if lo[0] > hi[0] or lo[1] > hi[1]:
            return None
        return dict(op, min=[lo[0], op['min'][1], lo[1]], max=[hi[0], op['max'][1], hi[1]])
    if 'points' in op:
        pts = op['points']
        reach = max([op.get('width', 0), op.get('thickness', 0)] + [p[2] for p in pts if len(p) > 2 and op['op'] == 'river'])
        reach = reach / 2 + op.get('falloff', 0) + op.get('bank', 0) + op.get('wobble', 0) + 2
        px0, px1 = min(p[0] for p in pts) - reach, max(p[0] for p in pts) + reach
        pz0, pz1 = min(p[1] for p in pts) - reach, max(p[1] for p in pts) + reach
        if px1 < x0 or px0 > x1 or pz1 < z0 or pz0 > z1:
            return None
        return dict(op, min=[x0, z0], max=[x1, z1])
    rx0, rz0, rx1, rz1 = region
    lo, hi = (max(rx0, x0), max(rz0, z0)), (min(rx1, x1), min(rz1, z1))
    if lo[0] > hi[0] or lo[1] > hi[1]:
        return None
    return dict(op, min=list(lo), max=list(hi))


# ---------------------------------------------------------------- plans

CORE = ('E3', 'E4', 'F3', 'F4')
GROUPS = {'core': CORE, 'mire': ('C3', 'C4', 'D3', 'D4'),
          'north': ('C1', 'D1', 'E1', 'F1', 'G1', 'C2', 'D2', 'E2', 'F2'),
          'south': ('C5', 'D5', 'E5', 'F5', 'G5', 'C6', 'D6', 'E6', 'F6', 'G6'),
          'west': ('B1', 'B2', 'B3', 'B4', 'B5', 'B6'),
          'bailey': ('G2', 'G3', 'G4'),
          'east': ('H1', 'H2', 'H3', 'H4', 'H5', 'H6'),
          'gorge': ('I1', 'I2', 'I3', 'I4', 'I5', 'I6')}
REOPEN = ("Kort's rulings of 2026-10-30: east curtain to x 98 (x 72 pieces dropped), inner ward closed to the river "
          'cliffs, Great Keep tower out of the river, stations awaiting the modes lane; 2026-10-31: the river bends '
          'around the keep court; 2026-10-31 milestone 2A.3: river, banks and walls rebuilt with the terrain helpers')


REOPEN_MIRE = ('2026-10-31 milestone 2A.3 flank pass: the trenches at x -150 and -105 redug by hand (wandering line, '
               'uneven width and depth, slumped lips, run ends short of the crossings) and the Mire west edge '
               'roughened into lobes and bays; positions unchanged')


REOPEN_SOUTH = ('2026-10-31 milestone 2A.3 south flank pass: the ridge-foot track to the south sally gap and its '
                'causeway over the fosse at z 62 (the mirror of the north one), routed outside the curtain')


REOPEN_FORD = ('2026-10-31 F5 tidy: the south ford no longer widens the river into a round pool; a slanting gravel '
               'bar crosses the kept channel, with gravel ramps down each cliff from the lane end and toward C')


REOPEN_G5 = ('2026-10-31 east bailey pass: the inside of the south postern gets the end of the bailey track from '
             "the defenders' road; the rest of the section is unchanged")


REOPEN_FORD_N = ('2026-10-31 F1 tidy (Kort): the north ford made like the south one, a slanting gravel bar across '
                 'the kept channel with gravel ramps down each cliff from the A side and up to the reach track')


REOPEN_F3 = ("2026-10-31 (Kort): the defenders' road joined from the B deck (x 6) to the bailey (x 30); the deck is "
             'laid after the roads so its planks stay planks')


REOPEN_RIM = ('2026-10-31 rim fix: the flank erodes (grass, mud and stone, clamped at 72 and 76) had planed the '
              'stone west rim flat in this section; the rim is now massed in andesite and climbs to 82-88 as signed')


def shell_floor(x, z, top, low=FLOOR):
    """Lowest y the column at (x, z) is built from (surface-only maps): SHELL blocks counting the top, deeper where a
    lower neighbour (by the column description) shows a cliff face, never below low."""
    nbrs = [c[0] for c in (column(x + 1, z), column(x - 1, z), column(x, z + 1), column(x, z - 1)) if c is not None]
    return max(low, min(top - SHELL + 1, min(nbrs, default=top) - 1))


def massing_ops(cols, what, extra=''):
    """Terrain ops for columns {(x, z): (top, mat, clear, low)}, grouped by material and clearing, each column built
    from its shell_floor up (a `floors` grid beside `heights`)."""
    groups = {}
    for (x, z), (top, mat, clear, low) in cols.items():
        groups.setdefault((mat, clear), {})[(x, z)] = (top, low)
    ops = []
    for (mat, clear), g in sorted(groups.items(), key=lambda kv: (-len(kv[1]), kv[0][0][0])):
        surface, under, depth, core = mat
        xs, zs = [p[0] for p in g], [p[1] for p in g]
        gx0, gx1, gz0, gz1 = min(xs), max(xs), min(zs), max(zs)
        heights = [[(g[(x, z)][0] - GROUND) if (x, z) in g else None for x in range(gx0, gx1 + 1)]
                   for z in range(gz0, gz1 + 1)]
        floors = [[shell_floor(x, z, g[(x, z)][0], g[(x, z)][1]) if (x, z) in g else None
                   for x in range(gx0, gx1 + 1)] for z in range(gz0, gz1 + 1)]
        op = {'op': 'terrain', 'note': f'{surface.split(":")[1]} {what} ({len(g)} columns){extra}',
              'origin': [gx0, gz0], 'base': GROUND, 'floor': FLOOR, 'surface': surface, 'under': under,
              'underDepth': depth, 'core': core, 'heights': heights, 'floors': floors}
        if clear is not None:
            op['clearTo'] = clear
        ops.append(op)
    return ops


def section_plan(m, sid, world):
    s = mf.section(m, sid)
    (x0, _, z0), (x1, _, z1) = mf.section_box(s)
    cols = {}
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            c = column(x, z)
            if c is not None:
                cols[(x, z)] = (c[0], c[1], c[2], c[3] if len(c) > 3 else FLOOR)  # the gorge builds from lower down
    ops = massing_ops(cols, 'massing')
    for op, region in features():
        c = _clip(op, region, (x0, z0, x1, z1))
        if c is not None:
            ops.append(c)
    done = s['status'] != 'todo'
    p = {
        'format': PLAN_FORMAT, 'id': f'ironhold-{sid.lower()}-blockout', 'world': 'ironhold',
        'author': 'map lane agent (Opus 5.5), 2026-10-31',
        'purpose': f'Blockout pass of section {sid} ({s["name"]}) from the signed master layout: column massing, then '
                   'the seeded terrain helpers (swells, erosion, river and pools, roads and causeway, walls, surface '
                   'breakup) clipped to the section. Generated by docs/maps/ironhold/sections/make_blockout.py.',
        'section': sid, 'pass': 'blockout',
        'expectSection': mf.section_digests(m, world, {sid})[sid],
        'limits': {'maxChangedBlocks': MAX_CHANGED},
        'operations': ops,
    }
    if done:
        p['reopen'] = (REOPEN_MIRE if sid in GROUPS['mire'] else REOPEN_SOUTH if sid == 'E4' else
                       REOPEN_FORD if sid == 'F5' else REOPEN_G5 if sid == 'G5' else
                       REOPEN_FORD_N if sid == 'F1' else REOPEN_F3 if sid == 'F3' else
                       REOPEN_RIM if sid in ('A2', 'A5') else REOPEN)
    return p


RULINGS_1031 = {  # Kort 2026-10-31: section -> (plan suffix, reason); only the ruled change, the rest stays as signed
    'H3': ('dusk-floor', 'spawn-dusk lock narrowed to y 64..90; a real floor under the Dusk spawn'),
    'H2': ('ward-north', 'Donjon Court enclosed: the north curtain runs on past the NE corner tower to the gorge parapet'),
    'H5': ('ward-south', 'Donjon Court enclosed: the south curtain runs on past the SE corner tower to the gorge parapet'),
    'I1': ('dry-pool', 'the north meadow pool on the gorge rim removed and filled to dry ground'),
}


def rulings_plan(m, sid, world):
    """A reopen plan holding only the 2026-10-31 ruled change in sid. Regenerating the whole section would also
    redo seam wear that neighbouring passes have since settled, so the signed rest of the section is left alone."""
    suffix, why = RULINGS_1031[sid]
    s = mf.section(m, sid)
    (x0, _, z0), (x1, _, z1) = mf.section_box(s)
    if sid == 'H3':
        cols = [(x, z) for x in range(DUSK[0], DUSK[1] + 1) for z in range(DUSK[2], DUSK[3] + 1)]
    elif sid == 'I1':  # every column of the section holding water, filled to grass at the meadow's 62
        cols = sorted({(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1) for y in range(56, 64)
                       if 'water' in world.get((x, y, z)).state})
    else:
        cols = [(x, z) for x in range(EAST_CORNERS[0][1] + 1, x1 + 1) for z in range(z0, z1 + 1) if 94 <= abs(z) <= 98]
    ruled = {}
    for x, z in cols:
        top, mat, _ = (62, GRASS, None) if sid == 'I1' else column(x, z)[:3]
        ruled[(x, z)] = (top, mat, None, FLOOR)
    ops = massing_ops(ruled, 'ruling', f': {why}')
    if sid in ('H2', 'H5'):
        zc = -96 if sid == 'H2' else 96
        ops.append({'op': 'breakup', 'note': 'grass worn along the new curtain foot', 'replace': [W + 'grass_block'],
                    'mix': _mix(('coarse_dirt', 2), ('rooted_dirt', 1), ('gravel', 1)), 'near': [W + 'stone_bricks'],
                    'width': 2.5, 'density': 0.5, 'scale': 2, 'seed': f'curtainfoot{zc}',
                    'min': [EAST_CORNERS[0][1] + 1, max(z0, zc - 6)], 'max': [x1, min(z1, zc + 6)]})
    return {
        'format': PLAN_FORMAT, 'id': f'ironhold-{sid.lower()}-{suffix}', 'world': 'ironhold',
        'author': 'map lane agent (Opus 5.5), 2026-10-31',
        'purpose': f"Kort's ruling of 2026-10-31 in section {sid}: {why}. Generated by "
                   'docs/maps/ironhold/sections/make_blockout.py rulings.',
        'section': sid, 'pass': 'blockout', 'reopen': f"Kort's ruling of 2026-10-31: {why}",
        'expectSection': mf.section_digests(m, world, {sid})[sid],
        'limits': {'maxChangedBlocks': 20000},
        'operations': ops,
    }


def rulings(only):
    m, w = mf.load_manifest(MAP), World.load(WORLD)
    for sid in only:
        p = rulings_plan(m, sid, w)
        errors = validate(p)
        if errors:
            raise SystemExit('\n'.join(errors))
        path = HERE / f'{sid}-{RULINGS_1031[sid][0]}.json'
        path.write_text(dump(p), encoding='utf-8')
        print(f'{path.name}: {len(p["operations"])} ops, expectSection {p["expectSection"][:12]}')


def base():
    WORK.mkdir(parents=True, exist_ok=True)
    if WORLD.exists():
        raise SystemExit(f'{WORLD} exists; delete it to rebuild the stand-in base world')
    w = World('ironhold')
    grass, dirt = Cell(normalize('minecraft:grass_block')), Cell(normalize('minecraft:dirt'))
    for x in range(BASE_LO[0], BASE_HI[0] + 1):
        for z in range(BASE_LO[1], BASE_HI[1] + 1):
            for y in range(GROUND - 3, GROUND + 1):
                w.set((x, y, z), grass if y == GROUND else dirt)
    pieces = [('ironhold/gate', [-37, 64, 0]), ('ironhold/keep', [0, 64, 34])] + \
             [('ironhold/curtain', [-72, 64, z]) for z in (-81, -43, 43, 81)]
    plan = {'format': PLAN_FORMAT, 'id': 'ironhold-base-today', 'world': 'ironhold', 'author': 'map lane agent',
            'purpose': "Stand-in for today's Ironhold until the 2A.2 export: today's gate, keep and west curtain "
                       'pieces at their Spec.java LANDMARKS rows. Sectionless; applied without --map because the '
                       'keep and gate stand on Siege and station protect locks by design.',
            'limits': {'maxChangedBlocks': 200000},
            'locks': [{'id': 'base', 'mode': 'allow', 'min': [-110, -64, -110], 'max': [40, 319, 110]}],
            'operations': [{'op': 'structure', 'note': f'today\'s {p} ({at[0]},{at[2]})', 'piece': p, 'at': at,
                            'rotation': 0} for p, at in pieces]}
    errors = validate(plan)
    if errors:
        raise SystemExit('\n'.join(errors))
    (HERE / 'base-today.json').write_text(json.dumps(plan, indent=1) + '\n', encoding='utf-8')
    summary, undo = apply(plan, w)
    write_undo(WORK / 'ironhold.base-today.undo.json', undo)
    w.save(WORLD)
    print(f'{WORLD}: {len(w.blocks)} blocks; {summary["changed"]} placed by the base plan; digest {w.digest()[:12]}')


def dump(p):
    """Plan JSON with one heights row per line, so the files stay readable and diff by row."""
    head = json.dumps({k: v for k, v in p.items() if k != 'operations'}, indent=1)
    ops = []
    for op in p['operations']:
        if 'heights' not in op:
            ops.append('  ' + json.dumps(op))
            continue
        rest = json.dumps({k: v for k, v in op.items() if k != 'heights'})
        rows = ',\n'.join('   ' + json.dumps(r, separators=(',', ':')) for r in op['heights'])
        ops.append('  ' + rest[:-1] + ', "heights": [\n' + rows + ']}')
    return head[:-2] + ',\n "operations": [\n' + ',\n'.join(ops) + '\n ]\n}\n'


def plans(only):
    m, w = mf.load_manifest(MAP), World.load(WORLD)
    for sid in only:
        p = section_plan(m, sid, w)
        errors = validate(p)
        if errors:
            raise SystemExit('\n'.join(errors))
        path = HERE / f'{sid}-blockout.json'
        path.write_text(dump(p), encoding='utf-8')
        print(f'{path.name}: {len(p["operations"])} ops, expectSection {p["expectSection"][:12]}')


if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else ''
    if cmd == 'base':
        base()
    elif cmd == 'rulings':
        rulings(sys.argv[2:] or list(RULINGS_1031))
    elif cmd == 'plans':
        ids = []
        for a in sys.argv[2:] or ['core']:
            ids += GROUPS.get(a, [a])
        plans(ids)
    else:
        raise SystemExit(__doc__)
