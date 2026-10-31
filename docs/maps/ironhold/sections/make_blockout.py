"""Ironhold blockout pass generator (signed layout of 2026-10-30, docs/maps/ironhold/layout-draft-2026-10-30.md).

  python docs/maps/ironhold/sections/make_blockout.py base    # stand-in base world in work/mapforge/ironhold/
  python docs/maps/ironhold/sections/make_blockout.py plans   # E3/E4/F3/F4 blockout plans beside this file

Blockout is massing only: ground heights and surfaces, the fosse and its causeway, the river and the B deck, the new
front-door curtain stretch and its two cracked towers, the yards, and the Great Keep tower volume. Every column is
described once by `column(x, z)`; each section's plan is that description clipped to the section's bounds and
grouped into terrain ops by material. Edges that the layout draws straight (fosse lip, river banks, road) are made
ragged with a deterministic value noise, because Map Forge has no noise or erode op yet (terrain-helper item).

The base world stands in for today's live map until the 2A.2 export exists: flat grass with its top at y 63 over the
four sections plus a margin, with today's ironhold/gate, ironhold/keep and the four west curtain pieces placed at their
Spec.java LANDMARKS rows (ih-gate, ih-keep, ih-curtain-w*)."""
import hashlib
import json
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[3]
sys.path.insert(0, str(ROOT / 'tools'))
from mapforge import manifest as mf  # noqa: E402
from mapforge.plan import PLAN_FORMAT, apply, validate, write_undo  # noqa: E402
from mapforge.structure import normalize  # noqa: E402
from mapforge.world import Cell, World  # noqa: E402

MAP = HERE.parent / 'manifest.json'
WORK = ROOT / 'work/mapforge/ironhold'
WORLD = WORK / 'ironhold.json'
SECTIONS = ('E3', 'E4', 'F3', 'F4')
BASE_LO, BASE_HI = (-101, -52), (34, 83)  # E3..F4 plus a 4-block render margin
FLOOR, GROUND = 57, 63  # terrain ops rebuild each column from y 57; field and fortress floor top y 63 (stand at 64)


# ---------------------------------------------------------------- noise

def _hash(*k):
    return int.from_bytes(hashlib.sha256(('ironhold:' + ':'.join(map(str, k))).encode()).digest()[:4], 'big') / 2 ** 32


def wobble(channel, t, period=5, amp=1.0):
    """1D value noise in [-amp, amp], smooth over `period` blocks; deterministic per channel."""
    i, f = divmod(t, period)
    a, b = _hash(channel, i), _hash(channel, i + 1)
    f = f / period
    f = f * f * (3 - 2 * f)
    return (a + (b - a) * f) * 2 * amp - amp


def blob(channel, x, z, period=12):
    """2D value noise in [0, 1), smooth over `period` blocks."""
    (i, fx), (j, fz) = divmod(x, period), divmod(z, period)
    fx, fz = fx / period, fz / period
    fx, fz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
    a, b = _hash(channel, i, j), _hash(channel, i + 1, j)
    c, d = _hash(channel, i, j + 1), _hash(channel, i + 1, j + 1)
    return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz


def edge(channel, t, amp=1.0, period=5):
    return round(wobble(channel, t, period, amp))


# ---------------------------------------------------------------- columns

GRASS = ('minecraft:grass_block', 'minecraft:dirt', 3, 'minecraft:stone')
WALL = ('minecraft:stone_bricks', 'minecraft:stone_bricks', 0, 'minecraft:stone_bricks')
TOWER = ('minecraft:deepslate_tiles', 'minecraft:deepslate_tiles', 0, 'minecraft:deepslate_tiles')
CROWN = ('minecraft:deepslate_bricks', 'minecraft:deepslate_bricks', 0, 'minecraft:deepslate_tiles')


def keep_tower(x, z):
    """Great Keep tower (pick D2a): x 4..20, z 24..44, chamfered corners, shaft to y 99, setback (2 in) to 114,
    crown 115..118 with merlons on the setback edge. Stands in the river at x 4..5 on the gravel bed."""
    if not (4 <= x <= 20 and 24 <= z <= 44):
        return None
    dx, dz = min(x - 4, 20 - x), min(z - 24, 44 - z)
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


def column(x, z):
    """(top y, (surface, under, underDepth, core), clearTo) for the column, or None to leave it as it is."""
    az = abs(z)
    t = keep_tower(x, z)
    if t:
        return t
    # front-door towers: x -80..-70, z +-(9..17), from the fosse floor; cracked tops 73..76
    if -80 <= x <= -70 and 9 <= az <= 17:
        crack = 76 - int(_hash('crack', x, z) * 4) if (x in (-80, -70) or az in (9, 17)) else 76
        return (crack, WALL, None)
    # new curtain stretch x -74..-70 between the towers and today's curtain pieces (z +-18..27); walk at 72
    if -74 <= x <= -70 and 18 <= az <= 27:
        return (71, WALL, None)
    # front-door threshold through the curtain line
    if -74 <= x <= -70 and az <= 8:
        return (GROUND, ('minecraft:cobblestone', 'minecraft:gravel', 2, 'minecraft:stone'), None)
    if -74 <= x <= -70:
        return None  # today's curtain pieces stand here (z +-28..58, 66..96); the sally gaps keep their ground
    # fosse: x -80..-75 with a ragged outer lip; water to y 61 (bed y 58); causeway z -8..8
    if -81 + edge('fosse-w', z) <= x <= -75 and az <= 96:
        if az <= 8:
            return (GROUND, ('minecraft:cobblestone', 'minecraft:cobblestone', 3, 'minecraft:stone'), None)
        return (61, ('minecraft:water', 'minecraft:water', 2, 'minecraft:gravel'), GROUND)
    if x < -80:
        # killing field: the King's Road (dirt path, ragged) and gently rolling grass, 63..64
        if az <= 3 + edge('road', x, 1.4):
            return (GROUND, ('minecraft:dirt_path', 'minecraft:dirt', 3, 'minecraft:stone'), None)
        rise = 1 if x <= -85 and blob('field', x, z) > 0.62 else 0  # low swells, not single bumps
        return (GROUND + rise, GRASS, None)
    if -69 <= x <= -41 and az <= 93:
        return (GROUND, ('minecraft:gravel', 'minecraft:gravel', 2, 'minecraft:stone'), None)  # Outer Yard
    if -40 <= x <= -34 and az <= 23:
        return None  # today's ironhold/gate stands here
    # river at x 0, banks ragged by a block; B deck x -5..5, z -12..12 (spruce planks at y 63)
    west, east = -5 + edge('bank-w', z, 1.5, 7), 5 + edge('bank-e', z, 1.5, 7)
    if az <= 12 and -5 <= x <= 5:
        return (GROUND, ('minecraft:spruce_planks', 'minecraft:water', 3, 'minecraft:gravel'), None)
    if west <= x <= east and az > 12:
        return (62, ('minecraft:water', 'minecraft:water', 2, 'minecraft:gravel'), GROUND)
    if -7 <= x <= 7 and not (-23 <= x <= 23 and 21 <= z <= 47):
        return (GROUND, ('minecraft:mud', 'minecraft:mud', 2, 'minecraft:stone'), None)  # river bank where it narrows
    if -33 <= x <= -6 and -24 <= z <= 20:
        return (GROUND, ('minecraft:packed_mud', 'minecraft:dirt', 3, 'minecraft:stone'), None)  # inner yard
    if -23 <= x <= 23 and 21 <= z <= 47:
        return None  # today's ironhold/keep court and walls stand here
    if x >= 6:
        return (GROUND, ('minecraft:andesite', 'minecraft:gravel', 2, 'minecraft:stone'), None)  # east bailey
    if -40 <= x <= -6:
        return (GROUND, ('minecraft:coarse_dirt', 'minecraft:dirt', 3, 'minecraft:stone'), None)  # bailey ground
    return (GROUND, GRASS, None)


# ---------------------------------------------------------------- plans

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
    return {
        'format': PLAN_FORMAT, 'id': f'ironhold-{sid.lower()}-blockout', 'world': 'ironhold',
        'author': 'map lane agent (Opus 5.5), 2026-10-30',
        'purpose': f'Blockout pass of section {sid} ({s["name"]}) from the signed master layout: ground heights and '
                   'surfaces, fosse, causeway, river and B deck, front-door curtain and towers, yards, Great Keep '
                   'tower volume. Generated by docs/maps/ironhold/sections/make_blockout.py.',
        'section': sid, 'pass': 'blockout',
        'expectSection': mf.section_digests(m, world, {sid})[sid],
        'limits': {'maxChangedBlocks': 120000},
        'operations': ops,
    }


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
        rest = json.dumps({k: v for k, v in op.items() if k != 'heights'})
        rows = ',\n'.join('   ' + json.dumps(r, separators=(',', ':')) for r in op['heights'])
        ops.append('  ' + rest[:-1] + ', "heights": [\n' + rows + ']}')
    return head[:-2] + ',\n "operations": [\n' + ',\n'.join(ops) + '\n ]\n}\n'


def plans(only=SECTIONS):
    m, w = mf.load_manifest(MAP), World.load(WORLD)
    for sid in only:
        p = section_plan(m, sid, w)
        errors = validate(p)
        if errors:
            raise SystemExit('\n'.join(errors))
        path = HERE / f'{sid}-blockout.json'
        path.write_text(dump(p), encoding='utf-8')
        print(f'{path.name}: {len(p["operations"])} terrain ops, expectSection {p["expectSection"][:12]}')


if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else ''
    if cmd == 'base':
        base()
    elif cmd == 'plans':
        plans(sys.argv[2:] or SECTIONS)
    else:
        raise SystemExit(__doc__)
