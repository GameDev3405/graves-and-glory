"""Sweep of every block that is not a full cube in a piece (lanterns, fences, walls, chains, ladders, trapdoors,
buttons, bars, slabs and stairs outside roofs): prints each kind with a count and flags placements that make no sense.
Usage: python audit.py PIECE.nbt [...]"""
import sys, os, collections
sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..', '..', 'tools')))
from mapforge.structure import Structure, parse_state

THIN = ('lantern', 'fence', 'wall', 'chain', 'ladder', 'trapdoor', 'button', 'iron_bars', 'campfire', 'grindstone',
        'cauldron', 'composter', 'torch', 'fire', 'dead_bush', 'grass', 'fern', 'poppy')
SOFT = THIN + ('slab', 'stairs', 'water', 'leaves')


def kind(state):
    n = parse_state(state)[0].replace('minecraft:', '')
    if n in ('air',): return None, n
    for t in THIN:
        if n == t or n.endswith('_' + t) or n.endswith(t): return t, n
    return None, n


def full(state):
    if state is None: return False
    n = parse_state(state)[0].replace('minecraft:', '')
    return n != 'air' and not any(n == t or n.endswith(t) for t in SOFT)


for path in sys.argv[1:]:
    s = Structure.load(path)
    c = {pos: st for pos, st, be in s.cells() if st != 'minecraft:air'}
    count, flags = collections.Counter(), []
    g = lambda x, y, z: c.get((x, y, z))
    for (x, y, z), st in sorted(c.items()):
        k, n = kind(st)
        if not k: continue
        count[n] += 1
        props = dict(parse_state(st)[1])
        up, dn = g(x, y + 1, z), g(x, y - 1, z)
        side = [g(x + 1, y, z), g(x - 1, y, z), g(x, y, z + 1), g(x, y, z - 1)]
        why = None
        if k == 'lantern':
            if props.get('hanging') == 'true':
                if not (full(up) or (up and 'chain' in up) or (up and ('fence' in up or 'slab' in up or 'wall' in up))): why = 'hangs from nothing'
                elif dn: why = 'hangs with a block right under it'
            else:
                if not dn: why = 'stands on nothing'
                elif up: why = 'stands with a block right over it (squeezed)'
            if any(full(v) for v in side) and sum(1 for v in side if full(v)) >= 2: why = (why or '') + ' boxed in by blocks'
        elif k in ('fence', 'wall'):
            if not dn and not up and sum(1 for v in side if v) == 0: why = 'floats alone'
            elif not dn and not any(v for v in side): why = 'hangs with nothing under or beside'
            elif not dn and not (up and ('fence' in up or 'wall' in up or full(up))) and sum(1 for v in side if v and ('fence' in v or 'wall' in v or full(v))) < 1: why = 'no support'
        elif k == 'chain':
            if not up: why = 'chain from nothing'
        elif k == 'ladder':
            f = props.get('facing'); bx, bz = {'north': (0, 1), 'south': (0, -1), 'east': (-1, 0), 'west': (1, 0)}[f]
            if not full(g(x + bx, y, z + bz)): why = 'ladder with no wall behind'
        elif k == 'button':
            why = 'button (decorative): check'
        elif k == 'trapdoor':
            if not any(full(v) for v in side + [up, dn]): why = 'trapdoor on nothing'
        if why: flags.append(((x, y, z), n, why.strip()))
    print('==', os.path.basename(path), dict(count))
    for f in flags: print('   ', *f)
