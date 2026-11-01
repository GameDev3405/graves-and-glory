"""Ironhold surface fix pass (Kort's rulings of 2026-10-31), over the built blockout world.

  python docs/maps/ironhold/sections/make_surface.py plans          # <SID>-surface.json for every section it changes
  python docs/maps/ironhold/sections/make_surface.py apply [SID...] # dry-run (locks), apply, undo, renders, review

Kort's top-layer rule: "We only need the top layer for maps, blocks aren't breakable so what's underneath doesn't
matter, just make sure the top layer looks good and go from there." This pass only re-skins what shows:

1. Floors (fields, yards, the east bailey, the Donjon Court): one main block per area, a light clumped accent, and
   paths whose cores are one block (each path op's own main block) so they read clearly. The old edge speckle and
   breakup patches go. Cliff steps (a drop of 2 or more), banks within 2 of water and non-ground tops are kept.
2. Gorge: crag faces banded stone and tuff, the floor and scree gravel with a few tuff clumps. No cobble or dirt speckle.
3. Great Keep tower: rebuilt in stone brick like the court walls: plinth, buttresses, arrow slits, a corbelled gallery
   with a crenellated parapet at 99..101, the upper stage with tall windows and a band, and a machicolated crown
   (corbels at 113, deck 114, merlons to 117, corner pinnacles 118). Footprint and heights as signed (D2a).
4. South postern (G5): the y 71 span over x 40..48, z 94..98 is the postern lintel, kept as a bridge: its two tops a
   grass-wear breakup had turned to grass and coarse dirt are stone brick again, the walk notch over the passage is
   filled and upside-down stairs at y 70 make arch shoulders.
5. Support: every gravel or sand block left over air (river and fosse beds under water) gets a stone under it, so it
   cannot fall in game.
"""
import json
import math
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import make_blockout as mb  # noqa: E402  (puts tools/ on sys.path)
from mapforge import manifest as mf  # noqa: E402
from mapforge import terrain  # noqa: E402
from mapforge.cli import _section_renders  # noqa: E402
from mapforge.plan import PLAN_FORMAT, apply, dry_run, format_summary, validate, write_undo  # noqa: E402
from mapforge.structure import normalize  # noqa: E402
from mapforge.world import Cell, World  # noqa: E402

W = 'minecraft:'
AUTHOR = 'map lane agent (Opus 5.5), 2026-10-31'
SUFFIX = 'surface'
REOPEN = ("Kort's rulings of 2026-10-31, surface fix pass: calm floors with readable paths, banded gorge, the Great "
          'Keep tower detailed in stone brick, the south postern lintel made good, falling blocks supported')
GROUND = {W + b for b in ('grass_block', 'coarse_dirt', 'dirt', 'rooted_dirt', 'gravel', 'stone', 'andesite', 'cobblestone',
                          'mossy_cobblestone', 'tuff', 'packed_mud', 'moss_block', 'podzol', 'polished_blackstone',
                          'cobbled_deepslate', 'blackstone', 'polished_blackstone_bricks', 'dirt_path', 'polished_andesite')}
FALLING = {W + b for b in ('gravel', 'sand', 'red_sand', 'suspicious_gravel', 'suspicious_sand')}
NATURAL = GROUND | {W + 'deepslate', W + 'calcite'}


# ---------------------------------------------------------------- accents

def _threshold(seed, density, scale=3.0):
    """fbm value above which about `density` of the cells lie (sampled once)."""
    s = terrain.seed_of(seed)
    vals = sorted(terrain.fbm(s, x * 7 + 3, z * 11 + 5, scale, 2) for x in range(70) for z in range(70))
    return vals[int(len(vals) * (1 - density))]


ZONES = {  # name: (main, accent, density)
    'field': (W + 'grass_block', W + 'coarse_dirt', 0.03),
    'yard': (W + 'coarse_dirt', W + 'packed_mud', 0.05),
    'bailey': (W + 'andesite', W + 'gravel', 0.04),
    'court': (W + 'polished_blackstone', W + 'polished_blackstone_bricks', 0.05),
    'outer': (W + 'gravel', W + 'cobblestone', 0.03),
    'inner': (W + 'packed_mud', W + 'coarse_dirt', 0.04),
    'scree': (W + 'gravel', W + 'tuff', 0.06),
}
_TH = {k: _threshold('acc-' + k, d) for k, (_, _, d) in ZONES.items()}


def zone_block(zone, x, z):
    main, acc, _ = ZONES[zone]
    return acc if terrain.fbm(terrain.seed_of('acc-' + zone), x, z, 3.0, 2) > _TH[zone] else main


def zone_of(x, z):
    """Floor zone of a column by its signed massing, or None (structures, rims, Mire, gorge, river)."""
    if mb.KX0 <= x <= mb.KX1 and mb.KZ0 <= z <= mb.KZ1:
        return None
    c = mb.column(x, z)
    if c is None:
        return None
    mat = c[1][0]
    if x >= 159:
        return 'scree' if c[1] == mb.GORGE_BED else ('field' if mat == W + 'grass_block' and x < 169 else None)
    if mat == W + 'grass_block':
        return 'field'
    if mat == W + 'coarse_dirt':
        return 'yard'
    if mat == W + 'andesite' and 9 <= x <= 97 and abs(z) <= 93:
        return 'bailey'
    if mat == W + 'polished_blackstone':
        return 'court'
    if mat == W + 'gravel' and -69 <= x <= -41:
        return 'outer'
    if mat == W + 'packed_mud':
        return 'inner'
    return None


# ---------------------------------------------------------------- paths

def _seg_dist(px, pz, ax, az, bx, bz):
    dx, dz = bx - ax, bz - az
    L = dx * dx + dz * dz
    t = 0.0 if L == 0 else max(0.0, min(1.0, ((px - ax) * dx + (pz - az) * dz) / L))
    return math.hypot(px - ax - t * dx, pz - az - t * dz)


def path_masks():
    """{(x, z): (core, main)}: core True within the path's width, False in its wobble/falloff band."""
    out = {}
    for op, _ in mb.features():
        if op['op'] != 'path':
            continue
        s = op['surface']
        main = s if isinstance(s, str) else s[0][0]
        main = normalize(main)
        if main not in GROUND:
            continue
        hw, band = op['width'] / 2, op.get('wobble', 0) + op.get('falloff', 0) + 0.5
        pts = op['points']
        for a, b in zip(pts, pts[1:]):
            r = hw + band + 1
            for x in range(math.floor(min(a[0], b[0]) - r), math.ceil(max(a[0], b[0]) + r) + 1):
                for z in range(math.floor(min(a[1], b[1]) - r), math.ceil(max(a[1], b[1]) + r) + 1):
                    d = _seg_dist(x, z, a[0], a[1], b[0], b[1])
                    if d <= hw - 0.5:
                        out[(x, z)] = (True, main)
                    elif d <= hw + band and (x, z) not in out:
                        out[(x, z)] = (False, main)
    return out


# ---------------------------------------------------------------- keep tower

def _stair(facing, half):
    return f'{W}stone_brick_stairs[facing={facing},half={half},shape=straight,waterlogged=false]'


def keep_cells():
    """{(x, y, z): state} for the Great Keep tower footprint (chamfer cells excluded), y 64..120, air included."""
    X0, X1, Z0, Z1 = mb.KX0, mb.KX1, mb.KZ0, mb.KZ1
    out = {}
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            dx, dz = min(x - X0, X1 - x), min(z - Z0, Z1 - z)
            if dx + dz < 3:
                continue
            s = min(dx, dz, dx + dz - 3)  # 0 = outer face, chamfers included
            d = {x - X0: 'east', X1 - x: 'west', z - Z0: 'south', Z1 - z: 'north'}
            inward = d[min(d)]
            diag = dx + dz - 3 < min(dx, dz)
            u = x + z
            # buttresses: corner piers on the chamfers, plus face piers (long faces z 27..28/33..35/40..41,
            # short faces x 12/16/20)
            pier = s == 0 and (diag or (dx == 0 and z in (27, 28, 33, 34, 35, 40, 41)) or (dz == 0 and x in (12, 16, 20)))
            slit = s == 1 and ((dx == 1 and z in (30, 38)) or (dz == 1 and x in (14, 18)))
            upper_win = s == 2 and ((dx == 2 and z in (29, 34, 39)) or (dz == 2 and x == 16))
            for y in range(64, 121):
                if (s >= 3 and y <= 113 and not (s == 3 and upper_win_back(x, z, dx, dz) and 103 <= y <= 107))                         or (s == 2 and y < 97):
                    continue  # hidden core: what's inside doesn't show
                b = None
                if y <= 98:
                    if s == 0 and (pier or y <= 65):
                        b = 'wall'
                    elif s == 0 and y == 66:
                        b = _stair(inward, 'bottom')  # drip course over the plinth
                    elif s == 0 and y == 98:
                        b = _stair(inward, 'top')  # gallery corbel
                    elif s == 1:
                        b = None if slit and y in (70, 71, 72, 79, 80, 81, 88, 89, 90) else 'wall'
                    elif s == 2 and slit_back(x, z, dx, dz) and y in (70, 71, 72, 79, 80, 81, 88, 89, 90):
                        b = W + 'deepslate_tiles'
                    elif s == 2 and y >= 97:
                        b = 'wall'  # foot of the upper stage
                elif y == 99:
                    b = 'wall' if s <= 2 else None  # gallery floor
                elif y <= 101:
                    if s == 0:
                        b = 'wall' if y == 100 or u % 3 != 2 else None  # gallery parapet, merlons 2 on 1 off
                    elif s == 2:
                        b = 'wall'
                elif y <= 113:
                    if s == 2:
                        if upper_win and 103 <= y <= 107 and not (dz == 2 and y == 103):
                            b = None
                        elif y == 109:
                            b = W + 'chiseled_stone_bricks'  # band
                        else:
                            b = 'wall'
                    elif s == 3 and upper_win_back(x, z, dx, dz) and 103 <= y <= 107:
                        b = W + 'deepslate_tiles'
                    elif s == 1 and y == 113:
                        b = _stair(inward, 'top')  # crown corbels
                elif y == 114:
                    b = 'wall' if s >= 1 else None  # crown deck
                elif y <= 117:
                    if s == 1:
                        b = 'wall' if y == 115 or u % 3 != 2 else None  # crown merlons 2 on 1 off, top 117
                elif y == 118:
                    if s == 1 and diag:
                        b = 'wall'  # corner pinnacles
                if b == 'wall':
                    h = mb._hash('keepwall', x, y, z)
                    b = (W + 'cracked_stone_bricks' if h < 0.07 else
                         W + 'mossy_stone_bricks' if h < (0.13 if y < 72 else 0.10) else W + 'stone_bricks')
                out[(x, y, z)] = b or W + 'air'
    # corner pinnacles need their merlon under them
    for (x, y, z), b in list(out.items()):
        if y == 118 and b != W + 'air':
            out[(x, 116, z)] = out[(x, 117, z)] = W + 'stone_bricks'
    return out


def slit_back(x, z, dx, dz):
    return (dx == 2 and z in (30, 38) and dz >= 2) or (dz == 2 and x in (14, 18) and dx >= 2)


def upper_win_back(x, z, dx, dz):
    return (dx == 3 and z in (29, 34, 39)) or (dz == 3 and x == 16)


# ---------------------------------------------------------------- the pass

def postern_cells():
    out = {(44, 71, 98): W + 'stone_bricks', (45, 71, 98): W + 'stone_bricks'}
    for x in range(41, 48):
        for z in (94, 95):
            out[(x, 71, z)] = W + 'stone_bricks'
    for z in range(94, 99):
        out[(40, 70, z)] = _stair('west', 'top')
        out[(48, 70, z)] = _stair('east', 'top')
    return out


def band_block(x, y, z):
    b = math.floor((y + 1.5 * terrain.fbm(terrain.seed_of('gorge-band'), x, z, 40, 2)) / 2)
    return W + ('tuff' if b % 5 in (3, 4) else 'stone')


def desired(world):
    """{pos: state} the pass wants (only cells that differ from world)."""
    blocks = world.blocks
    cols = {}
    for (x, y, z), c in blocks.items():
        if c.state != W + 'air':
            cols.setdefault((x, z), []).append(y)
    tops = {k: max(v) for k, v in cols.items()}

    def st(p):
        c = blocks.get(p)
        return c.state if c is not None else W + 'air'

    water = {k for k, y in tops.items() if 'water' in st((k[0], y, k[1]))}
    near_water = {(x + i, z + j) for x, z in water for i in range(-2, 3) for j in range(-2, 3)}
    paths = path_masks()
    want = {}
    for (x, z), y in tops.items():
        s = st((x, y, z))
        if x >= 159:
            c = mb.column(x, z)
            if c is not None and c[1] == mb.CRAG and (x >= 169 or mb.edge_rise(x, z) > 0.02):
                for yy in cols[(x, z)]:
                    if st((x, yy, z)) in NATURAL and any(st(q) == W + 'air' for q in (
                            (x + 1, yy, z), (x - 1, yy, z), (x, yy, z + 1), (x, yy, z - 1), (x, yy + 1, z))):
                        want[(x, yy, z)] = band_block(x, yy, z)
                continue
        zone = zone_of(x, z)
        if zone is None or s not in GROUND or (x, z) in near_water:
            continue
        nb = [tops.get(k) for k in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))]
        steep = any(n is None or y - n >= 2 for n in nb)
        p = paths.get((x, z)) if zone != 'scree' else None
        if p and p[0]:
            want[(x, y, z)] = p[1]  # path core: the path's main block
            continue
        if steep:
            continue
        if p and s == W + 'dirt_path':
            continue  # ragged path edge stays path
        want[(x, y, z)] = zone_block(zone, x, z)
    want.update(keep_cells())
    want.update(postern_cells())
    # support: no gravel or sand over air
    final = lambda p: want.get(p, st(p))  # noqa: E731
    for p, b in list(want.items()) + [(p, c.state) for p, c in blocks.items() if c.state in FALLING]:
        if b in FALLING and final(p) in FALLING:
            below = (p[0], p[1] - 1, p[2])
            if final(below) == W + 'air':
                want[below] = W + ('tuff' if p[0] >= 159 else 'stone')
    return {p: b for p, b in want.items() if normalize(b) != st(p)}


def _ops(changes, label):
    """Row-run fill ops (x runs at one y, z and state), clears for air."""
    rows = {}
    for (x, y, z), b in changes.items():
        rows.setdefault((y, z, b), []).append(x)
    ops = []
    for (y, z, b), xs in sorted(rows.items()):
        xs.sort()
        start = prev = xs[0]
        for x in xs[1:] + [None]:
            if x is not None and x == prev + 1:
                prev = x
                continue
            if b == W + 'air':
                ops.append({'op': 'clear', 'note': label(start, z), 'min': [start, y, z], 'max': [prev, y, z]})
            elif start == prev:
                ops.append({'op': 'set', 'note': label(start, z), 'pos': [start, y, z], 'block': b})
            else:
                ops.append({'op': 'fill', 'note': label(start, z), 'min': [start, y, z], 'max': [prev, y, z], 'block': b})
            if x is not None:
                start = prev = x
    return ops


def _label(x, z):
    if mb.KX0 <= x <= mb.KX1 and mb.KZ0 <= z <= mb.KZ1:
        return 'Great Keep tower in stone brick'
    if 40 <= x <= 48 and 94 <= z <= 98:
        return 'south postern lintel made good'
    if x >= 159:
        return 'gorge: banded crag, gravel scree'
    return 'floor: one main block, light accent, path cores'


def section_plans(m, world):
    changes = desired(world)
    by = {}
    for s in m['sections']:
        (x0, _, z0), (x1, _, z1) = mf.section_box(s)
        mine = {p: b for p, b in changes.items() if x0 <= p[0] <= x1 and z0 <= p[2] <= z1}
        if mine:
            by[s['id']] = mine
    digests = mf.section_digests(m, world, set(by))
    plans = {}
    for sid, ch in by.items():
        plans[sid] = {
            'format': PLAN_FORMAT, 'id': f'ironhold-{sid.lower()}-{SUFFIX}', 'world': 'ironhold', 'author': AUTHOR,
            'purpose': f'Surface fix pass of section {sid}: {REOPEN}. Top layer only. Generated by '
                       'docs/maps/ironhold/sections/make_surface.py.',
            'section': sid, 'pass': 'blockout', 'reopen': REOPEN, 'expectSection': digests[sid],
            'limits': {'maxChangedBlocks': mb.MAX_CHANGED}, 'operations': _ops(ch, _label)}
    return plans


def write_plans():
    m, w = mf.load_manifest(mb.MAP), World.load(mb.WORLD)
    plans = section_plans(m, w)
    for sid, p in sorted(plans.items()):
        errors = validate(p)
        if errors:
            raise SystemExit('\n'.join(errors[:5]))
        (HERE / f'{sid}-{SUFFIX}.json').write_text(json.dumps(p, indent=0) + '\n', encoding='utf-8')
    print(f'{len(plans)} plans: ' + ' '.join(sorted(plans)))


def apply_all(only):
    m, w = mf.load_manifest(mb.MAP), World.load(mb.WORLD)
    log = mb.WORK / 'surface-pass.log'
    sids = only or sorted(p.name.split('-')[0] for p in HERE.glob(f'*-{SUFFIX}.json'))
    with open(log, 'a', encoding='utf-8') as f:
        for sid in sids:
            path = HERE / f'{sid}-{SUFFIX}.json'
            plan = json.loads(path.read_text(encoding='utf-8'))
            s = dry_run(plan, w, HERE, m)
            text = format_summary(s)
            f.write(text + '\n\n')
            if not s['ok']:
                raise SystemExit(f'{sid}: refused\n' + '\n'.join(text.splitlines()[-6:]))
            summary, undo = apply(plan, w, HERE, m)
            write_undo(mb.WORK / f'ironhold.{plan["id"]}.undo.json', undo)
            imgs = _section_renders(m, sid, w, HERE)
            rel = [str(pathlib.Path(i).resolve().relative_to(mb.ROOT)).replace('\\', '/') for i in imgs]
            mf.sign(m, sid, 'blockout', w, 'map lane agent (Opus 5.5)', 'pass',
                    'Surface fix pass (Kort 2026-10-31): ' + _note(plan), rel,
                    [f'docs/maps/ironhold/sections/{path.name}'])
            print(f'{sid}: {summary["changed"]} changed, {text.splitlines()[-1]}', flush=True)
    w.save(mb.WORLD)
    mf.save_manifest(mb.MAP, m)
    print(f'world saved; log {log}')


def _note(plan):
    kinds = sorted({op['note'] for op in plan['operations']})
    return '; '.join(kinds) + '. Top layer only.'


if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else ''
    if cmd == 'plans':
        write_plans()
    elif cmd == 'apply':
        apply_all(sys.argv[2:])
    else:
        raise SystemExit(__doc__)
