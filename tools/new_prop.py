"""Scaffolds a new prop (docs/props/WORKFLOW.md, step 1). Stdlib only.

    python tools/new_prop.py <id> --use dressing|lobby|gameplay --size SX SY SZ --name "Display Name"
                             [--category <menu category>] [--setting "<House, map or place>"] [--parts a,b] [--glow b]

Writes, and refuses to overwrite:
  art/props/<id>/brief.md     the brief to fill in BEFORE modelling (purpose, scale, setting, look, budget, collision)
  art/props/<id>/model.py     the builder: build() -> proplib.Prop; starts as one placeholder box per part
  src/main/resources/data/gravesandglory/gg_props/<id>.json
                              the def: the next free custom_model_data block (7310001 + 10*n + part), solid = every cell
                              of the footprint's bottom layer, no lights or effects yet

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
CMD_MIN, CMD_MAX = 7310001, 7310999
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


def next_block(defs):
    used = set()
    for d in defs:
        for p in d.get('parts', []):
            used.add((p['cmd'] - CMD_MIN) // 10)
    return (max(used) + 1) if used else 0


def main(argv=None):
    a = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    a.add_argument('id')
    a.add_argument('--use', required=True, choices=sorted(USES))
    a.add_argument('--size', required=True, nargs=3, type=int, metavar=('SX', 'SY', 'SZ'))
    a.add_argument('--name', required=True)
    a.add_argument('--category', default=None, help='prop menu category (default: the use)')
    a.add_argument('--setting', default='TODO (House, map or place; or "any")')
    a.add_argument('--parts', default=None, help='comma list of part models (default: the id). One part unless it must exceed -16..32')
    a.add_argument('--block', type=int, default=None, help='CMD block the lead assigned; required when builders work in parallel worktrees')
    a.add_argument('--glow', default='', help='comma list of parts drawn full-bright (coals, lanterns)')
    o = a.parse_args(argv)

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

    block = next_block(defs) if o.block is None else o.block
    used = {(p['cmd'] - CMD_MIN) // 10 for d in defs for p in d.get('parts', [])} if o.block is not None else set()
    if block in used: sys.exit('CMD block %d is already used on this branch' % block)
    cmds = [CMD_MIN + 10 * block + i for i in range(len(parts))]
    if cmds[-1] > CMD_MAX or len(parts) > 10: sys.exit('no custom_model_data room (block %d, %d parts)' % (block, len(parts)))

    px, pz = (sx - 1) // 2, (sz - 1) // 2
    x0, x1, z0, z1 = -16 * px, 16 * (sx - px), -16 * pz, 16 * (sz - pz)
    d = {
        'id': pid, 'category': o.category or o.use, 'name': o.name, 'size': [sx, sy, sz],
        'parts': [{'model': p, 'cmd': c, 'glow': p in glow} for p, c in zip(parts, cmds)],
        'solid': [[x, 0, z] for z in range(sz) for x in range(sx)],
        'lights': [], 'effects': [],
    }
    code = []
    for p in parts:
        code.append('    %s = [  # placeholder: replace with the real build\n'
                    '        E((%d, 0, %d), (%d, %d, %d), "plank_x", name="placeholder"),\n    ]' % (p, x0 + 2, z0 + 2, x1 - 2, min(16 * sy, 32), z1 - 2))
    folder.mkdir(parents=True)
    (folder / 'brief.md').write_text(BRIEF.format(name=o.name, id=pid, use=o.use, use_text=USES[o.use], setting=o.setting,
                                                  sx=sx, sy=sy, sz=sz, px=px, pz=pz), encoding='utf8', newline='\n')
    (folder / 'model.py').write_text(MODEL.format(name=o.name, id=pid, sx=sx, sy=sy, sz=sz, px=px, pz=pz, x0=x0, x1=x1, y1=16 * sy,
                                                  z0=z0, z1=z1, parts_code='\n'.join(code),
                                                  parts_dict=', '.join('"%s": %s' % (p, p) for p in parts),
                                                  glow=(', glow=(%s,)' % ', '.join('"%s"' % g for g in glow)) if glow else ''),
                                     encoding='utf8', newline='\n')
    # the def in the house style: one solid cell a line, like the smithy defs
    text = json.dumps(d, indent=2)
    text = re.sub(r'\[\s+(-?\d+),\s+(-?\d+),\s+(-?\d+)\s+\]', r'[\1, \2, \3]', text)
    text = re.sub(r'\{\s+"model": ("[^"]+"),\s+"cmd": (\d+),\s+"glow": (true|false)\s+\}', r'{"model": \1, "cmd": \2, "glow": \3}', text)
    text = re.sub(r'"size": \[\s+(\d+),\s+(\d+),\s+(\d+)\s+\]', r'"size": [\1, \2, \3]', text)
    deff.write_text(text + '\n', encoding='utf8', newline='\n')
    print('scaffolded %s: block %d, cmd %s' % (pid, block, ', '.join(map(str, cmds))))
    print('  %s\n  %s\n  %s' % (folder / 'brief.md', folder / 'model.py', deff))
    print('next: fill brief.md, model in model.py, then  python tools/prop_card.py %s' % pid)
    return 0


if __name__ == '__main__':
    sys.exit(main())
