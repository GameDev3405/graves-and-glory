"""Ironhold blockout pass generator (signed layout of 2026-10-30, docs/maps/ironhold/layout-draft-2026-10-30.md).

  python docs/maps/ironhold/sections/make_blockout.py base             # stand-in base world in work/mapforge/ironhold/
  python docs/maps/ironhold/sections/make_blockout.py plans [GROUP|ID]  # blockout plans beside this file (default core)

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
2026-10-31: the river bends east around the keep court instead of running under it."""
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
FLOOR, GROUND = 57, 63  # terrain ops rebuild each column from y 57; field and fortress floor top y 63 (stand at 64)


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
RIVER_PTS = [[0, -120, 11], [0, 12, 11], [6, 16, 7], [27.5, 16, 7], [27.5, 52, 6], [6, 52, 7], [0, 56, 11],
             [0, 200, 11]]  # centre line and width: the bend around the keep court (2026-10-31)
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
    if x >= 9:
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
    d = mire_depth(x, z)
    if d > 0.8:
        return (61 if d >= 2.5 else 62, MUD, None)
    if d > -2.2:
        return (min(top, 62 if d > -0.8 else 63), GRASS, None)
    return (top, GRASS, None)


def column(x, z):
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


def features():
    """Every helper op on the map, in apply order. Each is clipped to the section it is planned for; noise, erode
    and breakup carry a region (x0, z0, x1, z1) that is intersected with the section."""
    f = []
    # -- shape: swells on the field and upland, hummocks in the Mire, then slopes settled by thermal erosion
    f.append(({'op': 'noise', 'note': 'field and upland swells', 'amplitude': 1.1, 'scale': 20, 'octaves': 3,
               'offset': 0.15, 'clamp': [63, 68], 'only': [W + 'grass_block'], 'seed': 'field'}, (-350, -250, -82, 280)))
    f.append(({'op': 'noise', 'note': 'Mire hummocks', 'amplitude': 0.9, 'scale': 6, 'offset': -0.25,
               'clamp': [61, 62], 'only': [W + 'mud'], 'seed': 'hummock'}, (-350, -250, -98, 280)))
    f.append(({'op': 'erode', 'note': 'settle the Mire edge and the swells', 'mode': 'thermal', 'iterations': 3,
               'talus': 1.0, 'rate': 0.5, 'jitter': 0.3, 'only': [W + 'grass_block', W + 'mud'], 'seed': 'settle'},
              (-350, -250, -98, 280)))
    # -- water: bog pools, the flooded trench in the Mire, the fosse, the river
    f += [(op, None) for op in _pools()]
    f.append(({'op': 'river', 'note': 'Mire trench x -150, flooded', 'points': [[-150, -60], [-150, 60]], 'width': 3,
               'water': 61, 'depth': 2, 'floor': 57, 'bed': W + 'mud', 'wobble': 0.6, 'exclude': TRENCH_GAPS,
               'seed': 'trench150'}, None))
    face = _mix(('stone', 3), ('cobblestone', 2), ('coarse_dirt', 2), ('dirt', 1))
    fosse = []  # inner edge on the curtain foot (x -75); the outer lip wanders between x -80 and -83
    for z in range(-100, 101, 9):
        wd = 6 + round(_hash('fosse-w', z) * 3)
        fosse.append([-74.5 - wd / 2, z, wd])
    f.append(({'op': 'river', 'note': 'fosse x -80..-75 (outer lip ragged), water 61, causeway z -8..8',
               'points': fosse, 'width': 6, 'water': 61, 'depth': 3, 'floor': 56, 'wobble': 2.2, 'wobbleScale': 5,
               'bankSurface': face, 'faces': True, 'seed': 'fosse',
               'exclude': [{'min': [-83, -8], 'max': [-74, 8]}, {'min': [-80, 9], 'max': [-70, 17]},
                           {'min': [-80, -17], 'max': [-70, -9]}, {'min': [-74, -200], 'max': [-60, 200]}]}, None))
    cliff = _mix(('stone', 3), ('andesite', 2), ('mud', 2), ('packed_mud', 1), ('cobblestone', 1))
    f.append(({'op': 'river', 'note': 'river: cut channel, water 59, bends east around the keep court',
               'points': RIVER_PTS,
               'width': 11, 'water': 59, 'depth': 3, 'floor': 55, 'wobble': 1.8, 'wobbleScale': 7,
               'bankSurface': cliff, 'faces': True, 'seed': 'river',
               'exclude': [{'min': [COURT[0], 20], 'max': [24, 48]}]}, None))
    f.append(({'op': 'fill', 'note': 'B deck: spruce planks at 63 over the channel', 'min': [-5, 63, -12],
               'max': [5, 63, 12], 'block': W + 'spruce_planks'}, (-5, -12, 5, 12)))
    # -- roads: the King's Road on the ground, across the Mire on a causeway at 63, on to the front door
    road = {'width': 7, 'surface': W + 'dirt_path', 'edge': _mix(('coarse_dirt', 2), ('gravel', 1)), 'falloff': 2,
            'edgeDensity': 0.5, 'wobble': 3.0, 'wobbleScale': 5}
    f.append((dict(road, op='path', note="King's Road, upland to the Mire", points=[[-290, 0], [-188, 0]],
                   seed='road-w'), None))
    f.append((dict(road, op='path', note="King's Road causeway over the Mire at 63", points=[[-194, 0], [-116, 0]],
                   y=63, falloff=4, under=_mix(('cobblestone', 1), ('dirt', 2)), seed='causeway'), None))
    f.append((dict(road, op='path', note="King's Road, killing field to the fosse causeway",
                   points=[[-120, 0], [-83, 0]], seed='road-e'), None))
    spur = {'op': 'path', 'width': 4, 'falloff': 1.5, 'surface': _mix(('dirt_path', 3), ('coarse_dirt', 1)),
            'under': W + 'dirt', 'wobble': 0.5}
    f.append((dict(spur, note='supply spur SW, raised track over the Mire', seed='spur-sw',
                   points=[[-199, 2, 63], [-190, 10, 63], [-183, 17, 62], [-170, 30, 62], [-155, 45, 62],
                           [-146, 54, 62], [-141, 60, 63], [-135, 66, 63], [-133, 70, 63]]), None))
    f.append((dict(spur, note='supply spur NW, raised track over the Mire', seed='spur-nw',
                   points=[[-199, -2, 63], [-190, -10, 63], [-183, -17, 62], [-170, -30, 62], [-161, -44, 62],
                           [-158, -52, 62], [-155, -60, 63], [-149, -66, 63], [-147, -72, 63]]), None))
    berm = {'op': 'path', 'width': 2, 'falloff': 1.5, 'surface': _mix(('coarse_dirt', 3), ('dirt', 1)),
            'under': W + 'dirt', 'exclude': TRENCH_GAPS}
    f.append((dict(berm, note='Mire trench berm (east lip)', points=[[-146.5, -60], [-146.5, 60]], y=63,
                   seed='berm150'), None))
    f.append(({'op': 'path', 'note': 'field trench x -105 (dry, 2 deep)', 'points': [[-105, -60], [-105, 60]],
               'width': 3, 'y': 61, 'surface': W + 'coarse_dirt', 'exclude': TRENCH_GAPS, 'wobble': 0.4,
               'seed': 't105'}, None))
    f.append((dict(berm, note='field trench berm (east lip)', points=[[-101.5, -60], [-101.5, 60]], y=64,
                   seed='berm105'), None))
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
    return f


def _clip(op, region, box):
    """The op clipped to the section box (x0, z0, x1, z1), or None when it cannot touch the section."""
    x0, z0, x1, z1 = box
    if op['op'] == 'fill':
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
GROUPS = {'core': CORE, 'mire': ('C3', 'C4', 'D3', 'D4')}
REOPEN = ("Kort's rulings of 2026-10-30: east curtain to x 98 (x 72 pieces dropped), inner ward closed to the river "
          'cliffs, Great Keep tower out of the river, stations awaiting the modes lane; 2026-10-31: the river bends '
          'around the keep court; 2026-10-31 milestone 2A.3: river, banks and walls rebuilt with the terrain helpers')


def section_plan(m, sid, world):
    s = mf.section(m, sid)
    (x0, _, z0), (x1, _, z1) = mf.section_box(s)
    groups = {}
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            c = column(x, z)
            if c is None:
                continue
            top, (surface, under, depth, core), clear = c
            groups.setdefault((surface, under, depth, core, clear), {})[(x, z)] = top
    ops = []
    for (surface, under, depth, core, clear), cols in sorted(groups.items(), key=lambda kv: (-len(kv[1]), kv[0][0])):
        xs, zs = [p[0] for p in cols], [p[1] for p in cols]
        gx0, gx1, gz0, gz1 = min(xs), max(xs), min(zs), max(zs)
        heights = [[(cols[(x, z)] - GROUND) if (x, z) in cols else None for x in range(gx0, gx1 + 1)]
                   for z in range(gz0, gz1 + 1)]
        op = {'op': 'terrain', 'note': f'{surface.split(":")[1]} massing ({len(cols)} columns)',
              'origin': [gx0, gz0], 'base': GROUND, 'floor': FLOOR, 'surface': surface, 'under': under,
              'underDepth': depth, 'core': core, 'heights': heights}
        if clear is not None:
            op['clearTo'] = clear
        ops.append(op)
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
        'limits': {'maxChangedBlocks': 150000},
        'operations': ops,
    }
    if done:
        p['reopen'] = REOPEN
    return p


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
    elif cmd == 'plans':
        ids = []
        for a in sys.argv[2:] or ['core']:
            ids += GROUPS.get(a, [a])
        plans(ids)
    else:
        raise SystemExit(__doc__)
