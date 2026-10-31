"""Command line: python tools/mapforge <command> ...  (see README.md). Offline only: works on mapforge world JSON files,
never on a Minecraft save or the builder server."""
import argparse
import json
import pathlib
import sys

from . import nbt
from .plan import PlanError, apply, dry_run, format_summary, load_plan, rollback, validate, write_undo
from .structure import Structure, normalize
from .world import Cell, World

PROTECT_FORMAT = 'gg-mapforge-protect/1'


def _refuse_live(path):
    """Refuses a path that looks like a Minecraft save or the builder server folder."""
    p = pathlib.Path(path).resolve()
    for d in (p.parent, *p.parents):
        if (d / 'level.dat').exists() or d.name in ('builder-server', 'GGBuild'):
            raise SystemExit(f'refused: {path} is inside a Minecraft save or the builder server ({d}); mapforge is offline only')
    if p.suffix != '.json':
        raise SystemExit(f'refused: {path} is not a mapforge world .json file')


def _protect(path):
    if not path:
        return ()
    data = json.loads(pathlib.Path(path).read_text(encoding='utf-8'))
    if data.get('format') != PROTECT_FORMAT:
        raise SystemExit(f'{path}: format must be {PROTECT_FORMAT}')
    return tuple(data['locks'])


def _report(summary, as_json):
    print(json.dumps(summary, indent=1) if as_json else format_summary(summary))


def main(argv=None):
    ap = argparse.ArgumentParser(prog='mapforge', description=__doc__)
    sub = ap.add_subparsers(dest='cmd', required=True)
    p = sub.add_parser('validate', help='check a plan')
    p.add_argument('plan')
    for name, help_ in (('dry-run', 'print the diff a plan would make'), ('apply', 'apply a plan and write an undo record')):
        p = sub.add_parser(name, help=help_)
        p.add_argument('plan')
        p.add_argument('--world', required=True)
        p.add_argument('--protect', help=f'{PROTECT_FORMAT} file of extra deny regions')
        p.add_argument('--json', action='store_true')
        if name == 'apply':
            p.add_argument('--undo', help='undo record path (default <world stem>.<plan id>.undo.json)')
    p = sub.add_parser('rollback', help='undo an applied plan')
    p.add_argument('undo')
    p.add_argument('--world', required=True)
    p = sub.add_parser('flat', help='write a flat offline world')
    p.add_argument('--out', required=True)
    p.add_argument('--min', type=int, nargs=2, required=True, metavar=('X', 'Z'))
    p.add_argument('--max', type=int, nargs=2, required=True, metavar=('X', 'Z'))
    p.add_argument('--y', type=int, required=True, help='top layer y')
    p.add_argument('--depth', type=int, default=1)
    p.add_argument('--block', default='minecraft:grass_block')
    p.add_argument('--name', default='offline')
    p = sub.add_parser('import', help='world from a structure .nbt')
    p.add_argument('nbt')
    p.add_argument('--out', required=True)
    p.add_argument('--origin', type=int, nargs=3, default=(0, 0, 0))
    p.add_argument('--name', default='offline')
    p = sub.add_parser('export', help='box of a world to a structure .nbt')
    p.add_argument('--world', required=True)
    p.add_argument('--min', type=int, nargs=3, required=True)
    p.add_argument('--max', type=int, nargs=3, required=True)
    p.add_argument('--out', required=True)
    p.add_argument('--skip-air', action='store_true')
    p = sub.add_parser('info', help='print a structure .nbt')
    p.add_argument('nbt')
    p.add_argument('--raw', action='store_true', help='dump every tag')
    a = ap.parse_args(argv)

    try:
        if a.cmd == 'validate':
            errors = validate(load_plan(a.plan), pathlib.Path(a.plan).parent)
            print('\n'.join(errors) or 'plan ok')
            return 1 if errors else 0
        if a.cmd in ('dry-run', 'apply'):
            _refuse_live(a.world)
            plan, world = load_plan(a.plan), World.load(a.world)
            if a.cmd == 'dry-run':
                s = dry_run(plan, world, pathlib.Path(a.plan).parent, _protect(a.protect))
                _report(s, a.json)
                return 0 if s['ok'] else 2
            summary, undo = apply(plan, world, pathlib.Path(a.plan).parent, _protect(a.protect))
            undo_path = pathlib.Path(a.undo or pathlib.Path(a.world).with_name(f'{pathlib.Path(a.world).stem}.{plan["id"]}.undo.json'))
            write_undo(undo_path, undo)  # the undo record exists before the world is replaced
            world.save(a.world)
            _report(summary, a.json)
            print(f'applied; undo record {undo_path}')
            return 0
        if a.cmd == 'rollback':
            _refuse_live(a.world)
            world = World.load(a.world)
            n = rollback(json.loads(pathlib.Path(a.undo).read_text(encoding='utf-8')), world)
            world.save(a.world)
            print(f'rolled back {n} block(s); world {world.digest()[:12]}')
            return 0
        if a.cmd == 'flat':
            _refuse_live(a.out)
            w = World(a.name)
            cell = Cell(normalize(a.block))
            for x in range(a.min[0], a.max[0] + 1):
                for z in range(a.min[1], a.max[1] + 1):
                    for y in range(a.y - a.depth + 1, a.y + 1):
                        w.set((x, y, z), cell)
            w.save(a.out)
            print(f'{a.out}: {len(w.blocks)} blocks, digest {w.digest()}')
            return 0
        if a.cmd == 'import':
            _refuse_live(a.out)
            w = World.from_structure(Structure.load(a.nbt), tuple(a.origin), a.name)
            w.save(a.out)
            print(f'{a.out}: {len(w.blocks)} blocks, digest {w.digest()}')
            return 0
        if a.cmd == 'export':
            s = World.load(a.world).export_structure(tuple(a.min), tuple(a.max), include_air=not a.skip_air)
            s.save(a.out)
            print(f'{a.out}: size {s.size}, {len(s.blocks)} blocks, {len(s.palette)} states')
            return 0
        if a.cmd == 'info':
            if a.raw:
                print(json.dumps(nbt.to_plain(nbt.read_file(a.nbt)[1]), indent=1))
                return 0
            s = Structure.load(a.nbt)
            print(f'size {s.size}, DataVersion {s.data_version}, {len(s.blocks)} blocks, {len(s.palette)} states, '
                  f'{len(s.entities.value)} entities, {sum(1 for b in s.blocks if b[2] is not None)} block entities')
            return 0
    except PlanError as e:
        print('REFUSED:\n  ' + '\n  '.join(e.errors), file=sys.stderr)
        return 2
    return 1
