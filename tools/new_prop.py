"""Scaffolds a new prop (docs/props/WORKFLOW.md, step 1). Stdlib only.

    python tools/new_prop.py <id> --use dressing|lobby|gameplay --size SX SY SZ --name "Display Name"
                             [--category <menu category>] [--setting "<House, map or place>"] [--parts a,b] [--glow b]
                             [--block N] [--split [--split-glow]] [--effects-only]
    python tools/new_prop.py <id> --sync      (a split prop: rewrite the def's parts from model.py after modelling)

Writes, and refuses to overwrite:
  art/props/<id>/brief.md     the brief to fill in BEFORE modelling (purpose, scale, setting, look, budget, collision)
  art/props/<id>/model.py     the builder: build() -> proplib.Prop; starts as one placeholder box per part
  src/main/resources/data/gravesandglory/gg_props/<id>.json
                              the def: the next free custom_model_data block (7310001 + 10*n + part), solid = every cell
                              of the footprint's bottom layer, no lights or effects yet

Big props (--split, automatic when the footprint is over 3 cells on any axis): model.py models every element in the real
prop frame and returns Prop.split(id, elements, size); proplib cuts it into 3-cell regions, parts <id>_<n>. The def gets
consecutive CMD blocks for every region (10 parts a block; --block is the first), "split": {"block", "blocks"}, and the
parts that exist with their "offset" (the game moves each part). After modelling run --sync: it rebuilds the model and
rewrites the parts list (same CMD for the same region, offsets refreshed). --split-glow reserves a second set for glow regions.

Effect-only props (--effects-only, e.g. smoke_vent): one part with no elements, no solid cells, a sample smoke effect.

Then: fill the brief, model in model.py, and run  python tools/prop_card.py <id>
"""
import argparse
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
ART = ROOT / 'art' / 'props'
DEFS = ROOT / 'src' / 'main' / 'resources' / 'data' / 'gravesandglory' / 'gg_props'
CMD_MIN, CMD_MAX = 7310001, 7319999
USES = {
    'dressing': 'map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident',
    'lobby': 'lobby / Old Hold hub: seen up close (2-6 m) and lingered on, so detail and story carry',
    'gameplay': 'gameplay object (cover, barricade, siege piece, objective): collision IS the design; any rule change needs Kort',
}

BRIEF = """# Prop brief: {name} (`{id}`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** {use} ({use_text}).
- **What it is, in one line:** TODO
- **Where it goes:** TODO (map(s) and spot, or lobby room; chapter or battle if it is story dressing).
- **Setting / House:** {setting}
- **Footprint (cells, x east * y up * z south):** {sx} x {sy} x {sz}. Front faces north. Anchor cell ({px}, 0, {pz}).
- **Scale against a troop:** TODO (a troop is 2 blocks tall, 32 px. Say how tall the prop stands next to one and why).
- **Silhouette at 15 m:** TODO (the one shape a player must read from far away).
- **Close-up story at 3 m:** TODO (two or three details that reward a close look; no more).
- **Materials and ramps (proplib):** TODO (e.g. `stone`, `wood_y`, `iron`; new ramps only via `make_ramp`, named here).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** TODO (which cells are solid and why; what a troop or an arrow should do at this prop).
- **Light and effects:** TODO (none, or which cells glow and which vanilla particles/sounds, within "alive but not noisy").
- **Gameplay effect (gameplay use only, design only):** TODO (what it should do in a battle; Kort decides before any code).
- **References:** TODO (existing props or troops it must sit beside; docs/visual-references files; never a copyrighted game's asset).
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
"""

MODEL = '''"""{name} (`{id}`): {sx}x{sy}x{sz} cells, anchor cell ({px},0,{pz}). Brief: art/props/{id}/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x {x0}..{x1}, y 0..{y1}, z {z0}..{z1} px. Elements stay inside -16..32 per part;
a part that cannot is stored shifted (Prop(shift={{part: (dx,dy,dz)}})), like the forge hood.

Rules that the card checks: whole-px faces (1 texel per model px), every colour from a proplib ramp or make_ramp,
elements within the budget in brief.md, model inside the footprint (2 px overhang at most).
Build:  python art/props/build_props.py {id}      Card:  python tools/prop_card.py {id}
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE   # noqa: F401  (materials: proplib.MAT keys)


def build():
{parts_code}
    return Prop("{id}", {{{parts_dict}}}{glow})
'''


MODEL_SPLIT = '''"""{name} (`{id}`): {sx}x{sy}x{sz} cells, anchor cell ({px},0,{pz}). Brief: art/props/{id}/brief.md.

A BIG prop: model every element in the real prop frame (art/props/README.md: model px, 16 = 1 block, origin = min corner
of the anchor cell, x east, y up, z south, FRONT FACES NORTH (-z)). The footprint runs x {x0}..{x1}, y 0..{y1}, z {z0}..{z1} px.
Prop.split cuts the elements into 3-cell regions (parts {id}_<n>); after changing which regions hold elements run
python tools/new_prop.py {id} --sync  so the def lists the same parts. Shape helpers: wheel, cylinder, blob, curve.
Build:  python art/props/build_props.py {id}      Card:  python tools/prop_card.py {id}
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE, wheel, cylinder, blob, curve   # noqa: F401

SIZE = ({sx}, {sy}, {sz})


def build():
    els = [  # placeholder: replace with the real build
        E(({x0p}, 0, {z0p}), ({x1p}, 16, {z1p}), "plank_x", name="placeholder"),
    ]
    glow = []   # full-bright elements (embers, lantern glass): parts after the plain ones (needs --split-glow)
    return Prop.split("{id}", els, SIZE, glow=glow)
'''

MODEL_FX = '''"""{name} (`{id}`): an effect-only prop ({sx}x{sy}x{sz} cells, anchor cell ({px},0,{pz})): nothing to see, no solid cell,
only the def's `effects` (vanilla particles and sounds). Brief: art/props/{id}/brief.md. Card:  python tools/prop_card.py {id}
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import Prop   # noqa: F401


def build():
    return Prop("{id}", {{"{id}": []}})
'''


def used_blocks(defs, skip=None):
    used = set()
    for d in defs:
        if d.get('id') == skip: continue
        for p in d.get('parts', []):
            used.add((p['cmd'] - CMD_MIN) // 10)
        s = d.get('split')
        if s: used.update(range(s['block'], s['block'] + s['blocks']))
    return used


def next_block(defs, count=1):
    used = used_blocks(defs)
    return (max(used) + 1) if used else 0


def write_def(deff, d):
    """The def in the house style: one solid cell a line, one part a line."""
    text = json.dumps(d, indent=2)
    text = re.sub(r'\[\s+(-?\d+),\s+(-?\d+),\s+(-?\d+)\s+\]', r'[\1, \2, \3]', text)
    text = re.sub(r'\{\s+"model": ("[^"]+"),\s+"cmd": (\d+),\s+"glow": (true|false)(,\s+"offset": \[[^\]]*\])?\s+\}',
                  lambda m: '{"model": %s, "cmd": %s, "glow": %s%s}' % (m.group(1), m.group(2), m.group(3),
                                                                     re.sub(r'\s+', ' ', m.group(4)) if m.group(4) else ''), text)
    text = re.sub(r'"size": \[\s+(\d+),\s+(\d+),\s+(\d+)\s+\]', r'"size": [\1, \2, \3]', text)
    text = re.sub(r'\{\s+("kind"[^{}\[\]]*?)\s+\}', lambda m: '{' + re.sub(r',\s+', ', ', m.group(1)) + '}', text)
    deff.write_text(text + '\n', encoding='utf8', newline='\n')


def split_parts(pid, d):
    """Build art/props/<id>/model.py and return the def parts of a split prop: cmd = first block + region index."""
    import importlib.util
    sys.path.insert(0, str(ART))
    spec = importlib.util.spec_from_file_location('prop_model_' + pid, ART / pid / 'model.py')
    mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
    prop = mod.build()
    if not getattr(prop, 'offsets', False): sys.exit('%s: model.py does not return Prop.split(...); --sync is for split props' % pid)
    s = d['split']; room = 10 * s['blocks']
    out = []
    for name, shift in prop.shift.items():
        n = int(name.rsplit('_', 1)[1])
        if n >= room: sys.exit('%s: part %s needs region %d but the def reserved %d (--split-glow at scaffold time reserves glow regions)' % (pid, name, n, room))
        out.append({'model': name, 'cmd': CMD_MIN + 10 * s['block'] + n, 'glow': name in prop.glow, 'offset': list(shift)})
    return out


def sync(pid):
    deff = DEFS / (pid + '.json')
    if not deff.exists(): sys.exit('no def %s' % deff)
    d = json.loads(deff.read_text(encoding='utf8'))
    if 'split' not in d: sys.exit('%s is not a split prop (no "split" in the def)' % pid)
    d['parts'] = split_parts(pid, d)
    write_def(deff, d)
    print('synced %s: %d parts, cmd %d..%d' % (pid, len(d['parts']), d['parts'][0]['cmd'], d['parts'][-1]['cmd']))
    return 0


def main(argv=None):
    a = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    a.add_argument('id')
    a.add_argument('--sync', action='store_true', help='split prop: rewrite the def parts from model.py')
    a.add_argument('--use', choices=sorted(USES))
    a.add_argument('--size', nargs=3, type=int, metavar=('SX', 'SY', 'SZ'))
    a.add_argument('--name')
    a.add_argument('--split', action='store_true', help='big prop: Prop.split into 3-cell regions (automatic over 3 cells on an axis)')
    a.add_argument('--split-glow', action='store_true', help='split prop: also reserve CMDs for full-bright regions')
    a.add_argument('--effects-only', action='store_true', help='no visible part, no solid cell, only effects (smoke vent)')
    a.add_argument('--category', default=None, help='prop menu category (default: the use)')
    a.add_argument('--setting', default='TODO (House, map or place; or "any")')
    a.add_argument('--parts', default=None, help='comma list of part models (default: the id). One part unless it must exceed -16..32')
    a.add_argument('--block', type=int, default=None, help='CMD block the lead assigned; required when builders work in parallel worktrees')
    a.add_argument('--glow', default='', help='comma list of parts drawn full-bright (coals, lanterns)')
    o = a.parse_args(argv)
    if o.sync: return sync(o.id)
    if not (o.use and o.size and o.name): a.error('--use, --size and --name are required (or --sync)')

    pid = o.id
    if not re.fullmatch(r'[a-z0-9_]+', pid): sys.exit('id must be [a-z0-9_]+')
    sx, sy, sz = o.size
    if min(o.size) < 1: sys.exit('size must be positive')
    parts = [p for p in (o.parts or pid).split(',') if p]
    glow = [g for g in o.glow.split(',') if g]
    for p in parts + glow:
        if not re.fullmatch(r'[a-z0-9_]+', p): sys.exit('part %r must be [a-z0-9_]+' % p)
    if set(glow) - set(parts): sys.exit('glow parts %s are not parts' % sorted(set(glow) - set(parts)))

    defs = [json.loads(f.read_text(encoding='utf8')) for f in sorted(DEFS.glob('*.json'))]
    models = {p['model'] for d in defs for p in d['parts']}
    clash = [p for p in parts if p in models]
    if clash: sys.exit('part model names already used by another prop: %s (pick unique part names)' % clash)
    folder, deff = ART / pid, DEFS / (pid + '.json')
    if folder.exists() or deff.exists(): sys.exit('%s exists already; refusing to overwrite' % (folder if folder.exists() else deff))

    sys.path.insert(0, str(ART))
    import proplib
    split = (o.split or max(sx, sz, sy) > 3) and not o.effects_only and o.parts is None
    if o.split_glow and not split: sys.exit('--split-glow is for split props')
    if split and glow: sys.exit('--glow names parts; a split prop passes glow elements to Prop.split and scaffolds with --split-glow')
    if split:
        regions = proplib.region_count((sx, sy, sz)) * (2 if o.split_glow else 1)
        parts = []   # filled from the placeholder model below
    nblocks = -(-regions // 10) if split else 1
    if not split and len(parts) > 10: sys.exit('at most 10 parts in one CMD block; a bigger prop is a split prop (--split)')
    block = next_block(defs) if o.block is None else o.block
    taken = used_blocks(defs) & set(range(block, block + nblocks))
    if taken: sys.exit('CMD block(s) %s already used on this branch' % sorted(taken))
    if CMD_MIN + 10 * (block + nblocks) - 1 > CMD_MAX: sys.exit('no custom_model_data room (block %d, %d blocks)' % (block, nblocks))
    cmds = [CMD_MIN + 10 * block + i for i in range(len(parts))]

    px, pz = (sx - 1) // 2, (sz - 1) // 2
    x0, x1, z0, z1 = -16 * px, 16 * (sx - px), -16 * pz, 16 * (sz - pz)
    d = {
        'id': pid, 'category': o.category or o.use, 'name': o.name, 'size': [sx, sy, sz],
        'parts': [{'model': p, 'cmd': c, 'glow': p in glow} for p, c in zip(parts, cmds)],
        'solid': [[x, 0, z] for z in range(sz) for x in range(sx)],
        'lights': [], 'effects': [],
    }
    if split: d['split'] = {'block': block, 'blocks': nblocks}
    if o.effects_only:
        d['solid'] = []
        d['effects'] = [{'kind': 'particle', 'id': 'minecraft:campfire_cosy_smoke', 'x': px + 0.5, 'y': 0.2, 'z': pz + 0.5,
                         'spread': 0.25, 'count': 1, 'every': 12, 'chance': 0.7}]
    code = []
    for p in parts:
        code.append('    %s = [  # placeholder: replace with the real build\n'
                    '        E((%d, 0, %d), (%d, %d, %d), "plank_x", name="placeholder"),\n    ]' % (p, x0 + 2, z0 + 2, x1 - 2, min(16 * sy, 32), z1 - 2))
    folder.mkdir(parents=True)
    (folder / 'brief.md').write_text(BRIEF.format(name=o.name, id=pid, use=o.use, use_text=USES[o.use], setting=o.setting,
                                                  sx=sx, sy=sy, sz=sz, px=px, pz=pz), encoding='utf8', newline='\n')
    fmt = dict(name=o.name, id=pid, sx=sx, sy=sy, sz=sz, px=px, pz=pz, x0=x0, x1=x1, y1=16 * sy, z0=z0, z1=z1)
    if split: src = MODEL_SPLIT.format(x0p=x0 + 2, x1p=x1 - 2, z0p=z0 + 2, z1p=z1 - 2, **fmt)
    elif o.effects_only: src = MODEL_FX.format(**fmt)
    else: src = MODEL.format(parts_code='\n'.join(code), parts_dict=', '.join('"%s": %s' % (p, p) for p in parts),
                             glow=(', glow=(%s,)' % ', '.join('"%s"' % g for g in glow)) if glow else '', **fmt)
    (folder / 'model.py').write_text(src, encoding='utf8', newline='\n')
    if split: d['parts'] = split_parts(pid, d); cmds = [p['cmd'] for p in d['parts']]
    write_def(deff, d)
    print('scaffolded %s: block %d%s, cmd %s' % (pid, block, ' (+%d more for the split regions)' % (nblocks - 1) if nblocks > 1 else '',
                                                 ', '.join(map(str, cmds))))
    print('  %s\n  %s\n  %s' % (folder / 'brief.md', folder / 'model.py', deff))
    print('next: fill brief.md, model in model.py, then  python tools/prop_card.py %s' % pid)
    return 0


if __name__ == '__main__':
    sys.exit(main())
