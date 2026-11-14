"""Plain-block comparison fixtures for the prop shots (docs/props/WORKFLOW.md, "Compare against a structure").

    python tools/compare_templates.py [--maps PATH] [--out DIR] [--sheet PNG | --no-sheet] [--check] [--only a,b]

Runs the maps lead's Python generators READ-ONLY from a maps checkout (default C:/Users/kortt/Melty/bb-maps, branch
maps/redesign; override with --maps or GG_MAPS_ROOT) and writes one small standalone structure template per plain-block
build, each on a small ground patch. Nothing of the generators is copied into this repo and nothing is written to the maps
checkout (bytecode writing is off while they import).

The fixtures are TEST FIXTURES: they live in test-tools/resources/data/gravesandglory/structures/compare/, which only the
unshipped local-integration jar carries (build.gradle localTestToolsJar), never the release jar. tools/release_config.py
refuses a jar that holds `structures/compare/`. The template id of a fixture is `compare/<name>`.

`fixture_for(prop_id)` maps a prop id to its fixture (variants to the base build); tools/prop_shot.py --compare-all uses it.
The sheet (default work/compare/fixtures.png) shows every fixture top-down and isometric, rendered offline by
tools/mapforge/render.py, so an empty or mis-cut fixture is visible at a glance. Needs Pillow only for the sheet.
"""
import argparse, os, pathlib, random, sys, types

ROOT = pathlib.Path(__file__).resolve().parents[1]
GROUP = 'compare'
OUT = ROOT / 'test-tools' / 'resources' / 'data' / 'gravesandglory' / 'structures' / GROUP
SHEET = ROOT / 'work' / 'compare' / 'fixtures.png'
DEFAULT_MAPS = r'C:\Users\kortt\Melty\bb-maps'

FIXTURES = ('trebuchet', 'supply_wagon_hay', 'supply_wagon_shot', 'tent_soldier', 'tent_pavilion', 'boulder', 'weapon_rack',
            'brazier', 'portcullis', 'smoke_vent', 'giant_rib', 'market_stall')
# Prop ids that have no plain-block version on the maps: shot alone even when their name starts with a fixture's name.
NO_FIXTURE = ('handcart', 'brazier_wall', 'portcullis_raised')
# Words in a prop id that mean the giant bones (the rivergate ribs).
BONE_WORDS = {'bone', 'bones', 'rib', 'ribs', 'giant'}
SHOT_WORDS = {'shot', 'stone', 'stones'}


def fixture_for(prop_id):
    """The fixture name (without the `compare/` group) for a prop id, or None when the prop is shot alone.
    Variants go to the base build: boulder_* to boulder, supply_wagon_shot (stone) to the shot wagon and every other
    supply_wagon_* to the hay wagon, market_stall_* to market_stall, smoke_vent_* to smoke_vent, weapon_rack_* to
    weapon_rack, the giant bones to giant_rib."""
    pid = prop_id.strip().lower()
    if any(pid == n or pid.startswith(n + '_') for n in NO_FIXTURE):
        return None
    if pid in FIXTURES:
        return pid
    if pid == 'supply_wagon' or pid.startswith('supply_wagon_'):
        return 'supply_wagon_shot' if SHOT_WORDS & set(pid.split('_')[2:]) else 'supply_wagon_hay'
    if BONE_WORDS & set(pid.split('_')):
        return 'giant_rib'
    for base in ('boulder', 'market_stall', 'smoke_vent', 'weapon_rack', 'tent_soldier', 'tent_pavilion', 'trebuchet', 'brazier', 'portcullis'):
        if pid.startswith(base + '_'):
            return base
    return None


def pairs_for(ids):
    """([(prop id, 'compare/<fixture>')], [ids shot alone]) for a list of prop ids."""
    paired, alone = [], []
    for i in ids:
        f = fixture_for(i)
        if f: paired.append((i, '%s/%s' % (GROUP, f)))
        else: alone.append(i)
    return paired, alone


# ------------------------------------------------------------------ the generators, read-only

def load_generators(maps):
    """Imports the maps lead's generator modules from a maps checkout without writing anything there. Returns a namespace."""
    maps = pathlib.Path(os.environ.get('GG_MAPS_ROOT') or maps)
    gen_ih, gen_rg = maps / 'docs' / 'maps' / 'ironhold' / 'gen', maps / 'docs' / 'maps' / 'rivergate' / 'gen'
    for p in (gen_ih, gen_rg):
        if not p.is_dir(): sys.exit('maps generators not found at %s (use --maps or GG_MAPS_ROOT)' % p)
    sys.dont_write_bytecode = True                      # never leave a __pycache__ in the maps checkout
    before = set(sys.modules)
    added = [str(gen_ih), str(gen_rg)]
    sys.path[:0] = added
    try:
        import ihbuild, works, gate, houses, ribs        # works first in ihbuild: it puts the rivergate kit on the path
        from kit import Build
    finally:
        for p in added:
            if p in sys.path: sys.path.remove(p)
    # The generators pull their own copy of tools/mapforge from the maps checkout; this script writes and renders with
    # this checkout's, so drop theirs from the module table (the generators keep working references to what they imported).
    for name in [m for m in sys.modules if (m == 'mapforge' or m.startswith('mapforge.')) and m not in before]:
        del sys.modules[name]
    tools = str(ROOT / 'tools')
    if tools not in sys.path: sys.path.insert(0, tools)
    return types.SimpleNamespace(ihbuild=ihbuild, works=works, gate=gate, houses=houses, ribs=ribs, Build=Build)


def cut(cells, x0, z0, x1, z1, y0, y1):
    return {k: v for k, v in cells.items() if x0 <= k[0] <= x1 and z0 <= k[2] <= z1 and y0 <= k[1] <= y1}


def boulder_cells(g):
    """ihbuild.Map.dress()'s nested boulder(), run as written (its own code object, on flat ground). The generator makes 2x2 to 3x3
    lumps 2 to 4 high; the fixture is the first valley spot that gives a big one whose middle column is its tallest, so it reads
    as a mound and not a flat-topped block."""
    ih = g.ihbuild
    code = next(c for c in ih.Map.dress.__code__.co_consts if isinstance(c, types.CodeType) and c.co_name == 'boulder')
    for x in range(-200, -110):
        cells = {}
        env = {'E': cells, 'top': lambda a, z: g.works.G, 'use': lambda a, z, r=1: None}
        types.FunctionType(code, vars(ih), 'boulder', None, tuple(types.CellType(env[n]) for n in code.co_freevars))(x, 0, 0)
        high = {}
        for (a, y, z), v in cells.items(): high[(a - x, z)] = max(high.get((a - x, z), 0), y - g.works.G)
        centre = high.get((0, 0), 0)
        if len(high) == 9 and centre >= 4 and all(h < centre for k, h in high.items() if k != (0, 0)):
            return cells
    raise RuntimeError('no boulder spot found')


def builders(g):
    """name -> (cells in map coordinates, ground layer y, ground mix). Every cell is a block state string."""
    w, G = g.works, g.works.G
    IH = [('gravel', 40), ('coarse_dirt', 30), ('cobbled_deepslate', 20), ('tuff', 10)]
    RG = [('cobblestone', 50), ('gravel', 30), ('stone', 20)]
    out = {}
    out['trebuchet'] = (w.trebuchet(), G, IH)
    out['supply_wagon_hay'] = (w.wagon(0), G, IH)
    out['supply_wagon_shot'] = (w.wagon(1), G, IH)
    out['tent_soldier'] = (w.tent(), G, IH)
    out['tent_pavilion'] = (w.pavilion(), G, IH)
    out['boulder'] = (boulder_cells(g), G, IH)
    # weapon_rack: the east side of the barracks yard (rack posts at x 21, z 11..18 of the yard; the map's x 267)
    out['weapon_rack'] = (cut(w.barracks_yard(), 20, 10, 21, 19, G, G + 4), G, IH)
    # brazier and portcullis: the main gatehouse built into an empty grid (site() lays its ground first)
    b = g.Build(seed=91); g.gate.site(b); g.gate.build(b)
    # the brazier column on the field side at z -6 (wall, block, lantern); gate.py sets it over the tower's wall cells there
    out['brazier'] = ({(-78, y, -6): b.c[(-78, y, -6)] for y in (70, 71, 72)}, 70, IH)
    # the drawn-up portcullis in its arch: bars x -74, z -4..4, y 74..76, with the jambs, lintel and lanterns round them
    out['portcullis'] = (cut(b.c, -75, -5, -73, 5, 70, 78), 70, IH)
    # smoke_vent: the forge chimney stack with its hidden smoking fire (works.stack), as the forge builds it
    chimney = w.C()
    w.stack(chimney, 0, 0, 2, 2, G + 13)
    out['smoke_vent'] = (chimney, G, IH)
    # giant_rib: one standing rib of rivergate's ribs.py (the second one of the north-east quarter), foot rubble included
    ribs_cells = {}
    g.ribs.rib(ribs_cells, random.Random(12), 1, -1, 1)
    out['giant_rib'] = (ribs_cells, g.ribs.BED, [('gravel', 50), ('mossy_cobblestone', 20), ('stone', 30)])
    # market_stall: the open shop front of a rivergate shop house (s2a): counters, awning trapdoors, posts, display
    shop = g.houses.kind('s2a', seed=3)
    out['market_stall'] = (cut(shop.c, 0, 7, 10, 12, g.houses.G, g.houses.G + 3), g.houses.G, RG)
    return out


# ------------------------------------------------------------------ cells -> template

def to_structure(cells, gy, mix, margin=2):
    """The cells on a ground patch: local y 0 is the ground layer, x and z start `margin` blocks of ground in."""
    from mapforge.structure import Structure, normalize
    cells = {k: v for k, v in cells.items() if k[1] >= gy and v != 'air'}
    if not cells:
        raise RuntimeError('empty fixture')
    xs, ys, zs = zip(*cells)
    x0, x1, z0, z1, ymax = min(xs) - margin, max(xs) + margin, min(zs) - margin, max(zs) + margin, max(ys)
    total = sum(wt for _, wt in mix)
    def ground(x, z):
        r = ((x * 73856093) ^ (z * 19349663) ^ 0x2545f491) % total
        for b, wt in mix:
            r -= wt
            if r < 0: return b
        return mix[-1][0]
    palette, index, blocks = [], {}, []
    for y in range(gy, ymax + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                v = cells.get((x, y, z))
                if v is None and y == gy: v = ground(x, z)
                if v is None: continue
                v = normalize(v)
                if v not in index: index[v] = len(palette); palette.append(v)
                blocks.append(((x - x0, y - gy, z - z0), index[v], None))
    return Structure((x1 - x0 + 1, ymax - gy + 1, z1 - z0 + 1), palette, blocks)


def build_all(maps, only=None):
    g = load_generators(maps)
    return {name: to_structure(cells, gy, mix) for name, (cells, gy, mix) in builders(g).items() if not only or name in only}


# ------------------------------------------------------------------ the sheet

def contact_sheet(made, path):
    from mapforge import render
    from mapforge.world import World
    rows, row = [], []
    for name, s in made.items():
        world = World.from_structure(s, name=name)
        hi = tuple(c - 1 for c in s.size)
        tag = '%s %dx%dx%d, %d blocks' % (name, *s.size, len(s.blocks))
        row += [render.render(world, 'top', (0, 0), (hi[0], hi[2]), title=tag + ' (top)'), render.render(world, 'iso', (0, 0, 0), hi, title=tag + ' (iso)')]
        if len(row) == 4: rows.append(row); row = []
    if row: rows.append(row)
    sheet = render.contact_sheet(rows, title="compare fixtures: the maps' plain-block builds, top-down and isometric")
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)


def main(argv=None):
    a = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    a.add_argument('--maps', default=DEFAULT_MAPS, help='maps checkout holding docs/maps/{ironhold,rivergate}/gen (read-only)')
    a.add_argument('--out', default=str(OUT)); a.add_argument('--sheet', default=str(SHEET)); a.add_argument('--no-sheet', action='store_true')
    a.add_argument('--check', action='store_true', help='write nothing; fail when a fixture file differs from what the generators make now')
    a.add_argument('--only', default='', help='comma list of fixture names')
    o = a.parse_args(argv)
    made = build_all(o.maps, set(o.only.split(',')) - {''})
    out = pathlib.Path(o.out)
    from mapforge.structure import Structure
    stale = []
    for name, s in made.items():
        path = out / (name + '.nbt')
        if o.check:
            if not path.is_file() or Structure.load(path).to_tag() != s.to_tag(): stale.append(name)
            continue
        out.mkdir(parents=True, exist_ok=True)
        s.save(path)
        print('%-18s %2dx%2dx%2d %5d blocks  %s' % (name, *s.size, len(s.blocks), path))
    if o.check:
        print('stale: ' + ', '.join(stale) if stale else 'fixtures match the generators')
        return 1 if stale else 0
    if not o.no_sheet:
        contact_sheet(made, pathlib.Path(o.sheet)); print('sheet', o.sheet)
    return 0


if __name__ == '__main__':
    sys.exit(main())
