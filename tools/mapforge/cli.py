"""Command line: python tools/mapforge <command> ...  (see README.md). Offline only: works on mapforge world JSON files,
never on a Minecraft save or the builder server."""
import argparse
import json
import pathlib
import sys

from . import colours, manifest as mf, nbt, render
from .colours import Palette
from .plan import PlanError, apply, dry_run, format_summary, load_plan, rollback, validate, write_undo
from .structure import Structure, normalize
from .world import Cell, World

def _refuse_live(path, suffix='.json'):
    """Refuses a path that looks like a Minecraft save or the builder server folder."""
    p = pathlib.Path(path).resolve()
    for d in (p.parent, *p.parents):
        if (d / 'level.dat').exists() or d.name in ('builder-server', 'GGBuild'):
            raise SystemExit(f'refused: {path} is inside a Minecraft save or the builder server ({d}); mapforge is offline only')
    if p.suffix != suffix:
        raise SystemExit(f'refused: {path} is not a mapforge {"world .json" if suffix == ".json" else suffix} file')


def _map(path):
    return mf.load_manifest(path) if path else None


def _section_renders(m, sid, world, out_dir, margin=4, before=None, label=None):
    """Top-down and isometric PNGs of one section (plus margin blocks around it), named <map>-<section>-top/iso.png."""
    s = mf.section(m, sid)
    lo, hi = mf.section_box(s)
    blo, bhi = world.bounds() or (lo, hi)
    lo = [lo[0] - margin, max(lo[1], blo[1]), lo[2] - margin]
    hi = [hi[0] + margin, min(hi[1], bhi[1]), hi[2] + margin]
    out_dir = pathlib.Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    palette, paths = Palette(), []
    title = f'{m["id"]} {sid} {s.get("name", "")} - {label or s["status"]}'.strip()
    for view in ('top', 'iso'):
        path = out_dir / f'{m["id"]}-{sid}-{view}.png'
        _refuse_live(path, '.png')
        img = render.render(world, view, lo, hi, None, True, before, palette, title)
        img.save(path)
        paths.append(path)
    return paths


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
        p.add_argument('--map', help='gg-mapforge-map/1 manifest: section bounds and protect locks (needed for sectioned plans)')
        p.add_argument('--json', action='store_true')
        if name == 'apply':
            p.add_argument('--undo', help='undo record path (default <world stem>.<plan id>.undo.json)')
            p.add_argument('--render', action='store_true', help='also write before, after and diff PNGs beside the undo record (Pillow)')
    p = sub.add_parser('status', help='section status, last review and drift of a map manifest')
    p.add_argument('map')
    p.add_argument('--world', help='world to check for drift since each section\'s last passing review')
    p.add_argument('--digests', action='store_true', help='print every section digest of --world (for expectSection)')
    p = sub.add_parser('sign', help='record an agent review of one section pass, or Kort\'s sign-off of the whole map')
    p.add_argument('map')
    p.add_argument('section', nargs='?')
    p.add_argument('--world', required=True)
    p.add_argument('--by', required=True)
    p.add_argument('--pass', dest='pass_', choices=mf.PASSES + ('done',))
    p.add_argument('--verdict', choices=mf.VERDICTS, default='pass')
    p.add_argument('--notes', default='')
    p.add_argument('--image', action='append', default=[])
    p.add_argument('--plan', action='append', default=[])
    p.add_argument('--whole', action='store_true', help='sign the whole finished map (every section done)')
    p = sub.add_parser('render-section', help='top-down and isometric review PNGs of one section')
    p.add_argument('map')
    p.add_argument('section')
    p.add_argument('--world', required=True)
    p.add_argument('--out-dir', required=True)
    p.add_argument('--margin', type=int, default=4)
    p.add_argument('--label', help='caption label (default the section status)')
    g = p.add_mutually_exclusive_group()
    g.add_argument('--diff', metavar='BEFORE', help='diff against this earlier world')
    g.add_argument('--undo', metavar='UNDO', help='diff of this undo record, rebuilt on WORLD')
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
    p = sub.add_parser('render', help='review PNG of a world: top-down, isometric or a diff (Pillow)')
    p.add_argument('world')
    p.add_argument('--out', required=True, help='output .png')
    p.add_argument('--view', choices=('top', 'iso'), default='top')
    p.add_argument('--min', type=int, nargs='+', metavar='N', help='crop box corner: X Z or X Y Z')
    p.add_argument('--max', type=int, nargs='+', metavar='N', help='crop box corner: X Z or X Y Z')
    p.add_argument('--scale', type=int, help='pixels per block (top) or per half tile (iso); default fits about 800-1400 px')
    p.add_argument('--no-grid', action='store_true', help='top-down without the 16-block grid and coordinate labels')
    g = p.add_mutually_exclusive_group()
    g.add_argument('--diff', metavar='BEFORE', help='diff against this earlier world (WORLD is the after side)')
    g.add_argument('--undo', metavar='UNDO', help='diff of the before and after of this undo record, rebuilt on WORLD')
    p.add_argument('--title', help='caption title (default the world name)')
    p.add_argument('--report-unknown', action='store_true', help='list blocks drawn in the magenta fallback')
    p = sub.add_parser('colours', help='regenerate colours.json from a Minecraft client jar (Pillow)')
    p.add_argument('--jar', required=True)
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
            if a.cmd == 'apply' and a.render:
                render.require_pillow()  # refuse before changing anything
            if a.cmd == 'dry-run':
                s = dry_run(plan, world, pathlib.Path(a.plan).parent, _map(a.map))
                _report(s, a.json)
                return 0 if s['ok'] else 2
            summary, undo = apply(plan, world, pathlib.Path(a.plan).parent, _map(a.map))
            undo_path = pathlib.Path(a.undo or pathlib.Path(a.world).with_name(f'{pathlib.Path(a.world).stem}.{plan["id"]}.undo.json'))
            write_undo(undo_path, undo)  # the undo record exists before the world is replaced
            world.save(a.world)
            _report(summary, a.json)
            print(f'applied; undo record {undo_path}')
            if a.render:
                for path in render.undo_renders(undo_path, undo, world):
                    print(f'render {path}')
            return 0
        if a.cmd == 'status':
            m = mf.load_manifest(a.map)
            world = World.load(a.world) if a.world else None
            print(mf.format_status(m, mf.status_rows(m, world)))
            if a.digests and world is not None:
                for sid, d in mf.section_digests(m, world).items():
                    print(f'  digest {sid} {d}')
            return 0
        if a.cmd == 'sign':
            _refuse_live(a.world)
            m, world = mf.load_manifest(a.map), World.load(a.world)
            if a.whole:
                signed = mf.sign_map(m, world, a.by)
                mf.save_manifest(a.map, m)
                print(f'map {m["id"]} signed by {signed["by"]}; world {signed["world"][:12]}')
                return 0
            if not a.section or not a.pass_:
                raise SystemExit('sign needs SECTION and --pass (or --whole)')
            r = mf.sign(m, a.section, a.pass_, world, a.by, a.verdict, a.notes, a.image, a.plan)
            mf.save_manifest(a.map, m)
            print(f'{a.section}: {r["pass"]} {r["verdict"]} by {r["by"]}; section digest {r["digest"][:12]}; '
                  f'status {mf.section(m, a.section)["status"]}')
            return 0
        if a.cmd == 'render-section':
            _refuse_live(a.world)
            m, world, before = mf.load_manifest(a.map), World.load(a.world), None
            if a.diff:
                _refuse_live(a.diff)
                before = World.load(a.diff)
            elif a.undo:
                before, world = render.worlds_from_undo(world, json.loads(pathlib.Path(a.undo).read_text(encoding='utf-8')))
            for path in _section_renders(m, a.section, world, a.out_dir, a.margin, before, a.label):
                print(path)
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
        if a.cmd == 'render':
            _refuse_live(a.world)
            _refuse_live(a.out, '.png')
            world, before = World.load(a.world), None
            if a.diff:
                _refuse_live(a.diff)
                before = World.load(a.diff)
            elif a.undo:
                before, world = render.worlds_from_undo(world, json.loads(pathlib.Path(a.undo).read_text(encoding='utf-8')))
            palette = Palette()
            img = render.render(world, a.view, a.min, a.max, a.scale, not a.no_grid, before, palette, a.title)
            img.save(a.out)
            print(f'{a.out}: {img.width}x{img.height}')
            if a.report_unknown:
                lo, hi = render.crop_box([world] + ([before] if before else []), a.min, a.max)
                unknown = render.unknown_blocks(world, lo, hi, palette)
                if before:
                    for k, v in render.unknown_blocks(before, lo, hi, palette).items():
                        unknown[k] = max(unknown.get(k, 0), v)
                print('unknown blocks (magenta): ' + (', '.join(f'{k} x{v}' for k, v in sorted(unknown.items())) or 'none'))
            return 0
        if a.cmd == 'colours':
            n, missing = colours.generate(a.jar)
            print(f'{colours.TABLE}: {n} blocks' + (f'; no texture for {", ".join(missing)}' if missing else ''))
            return 0
        if a.cmd == 'info':
            if a.raw:
                print(json.dumps(nbt.to_plain(nbt.read_file(a.nbt)[1]), indent=1))
                return 0
            s = Structure.load(a.nbt)
            print(f'size {s.size}, DataVersion {s.data_version}, {len(s.blocks)} blocks, {len(s.palette)} states, '
                  f'{len(s.entities.value)} entities, {sum(1 for b in s.blocks if b[2] is not None)} block entities')
            return 0
    except render.RenderError as e:
        print(f'render: {e}', file=sys.stderr)
        return 1
    except mf.ManifestError as e:
        print('manifest: ' + '\n  '.join(e.errors), file=sys.stderr)
        return 2
    except PlanError as e:
        print('REFUSED:\n  ' + '\n  '.join(e.errors), file=sys.stderr)
        return 2
    return 1
