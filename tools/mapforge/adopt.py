"""python tools/mapforge adopt <command> ...: the offline half of map adoption (plan task 2A.5), on gg-mapforge-runs/1
files (runs.py). The game half is /mapforge (test-tools MapForge.java) and the shipped MapAdoption.java.

  batch WORLD.json OUT --map ID [--manifest M] [--protects P] [--top Y] [--area X0,Z0,X1,Z1] [--keep-protected] [--keep-column X,Z,R ...]
        compile an offline world into a batch: each column from its lowest cell up to --top (air between and above
        its cells), allow region = the world's bounds up to --top, deny locks = the manifest's protect locks plus the
        kept columns. --keep-protected leaves out every cell on a deny lock, so the live world keeps them.
  validate FILE [--base CAPTURE]      check a runs file (and an overlay against its base capture)
  info FILE                           print the header
  diff BASE FINAL OUT                 overlay of two captures (the offline twin of /mapforge diff)
  apply PATCH CAPTURE OUT             a capture with an overlay, batch or undo written over it (round-trip tests)
  bake OVERLAY --base CAPTURE --target dev|release [--approved-by NAME]
        validate the overlay against the capture it was made from, stamp it with the mod version and copy it into the
        mod: dev = test-tools/resources/data/gravesandglory/adoption-dev/ (unshipped, read with -PadoptionDev),
        release = src/main/resources/data/gravesandglory/adoption/ (ships; needs Kort's approval, --approved-by)."""
import argparse
import json
import pathlib
import sys

from . import manifest as mf
from .runs import Runs, RunsError, apply as apply_runs, diff as diff_runs, from_world, validate as validate_file
from .world import World

ROOT = pathlib.Path(__file__).resolve().parents[2]
TARGETS = {'dev': ROOT / 'test-tools/resources/data/gravesandglory/adoption-dev',
           'release': ROOT / 'src/main/resources/data/gravesandglory/adoption'}


def _inside(x, y, z, lock):
    if 'center' in lock:
        (cx, cz), r, (y0, y1) = lock['center'], lock['radius'], lock['y']
        return y0 <= y <= y1 and (x - cx) ** 2 + (z - cz) ** 2 <= r * r
    return all(lock['min'][i] <= (x, y, z)[i] <= lock['max'][i] for i in range(3))


def _column_locks(deny):
    """keep(x, y, z) for a list of locks, testing only the locks whose footprint holds the column."""
    cache = {}

    def keep(x, y, z):
        here = cache.get((x, z))
        if here is None:
            here = cache[(x, z)] = [l for l in deny if _inside(x, l['y'][0] if 'center' in l else l['min'][1], z, l)]
        return any(_inside(x, y, z, l) for l in here)
    return keep


def compile_batch(world, map_id, manifest=None, top=None, keep_protected=False, keep_columns=(), purpose='adopt', protects=None, area=None):
    lo, hi = world.bounds()
    if area:
        lo, hi = (max(lo[0], area[0]), lo[1], max(lo[2], area[1])), (min(hi[0], area[2]), hi[1], min(hi[2], area[3]))
    top = hi[1] if top is None else top
    deny = [{k: v for k, v in lock.items() if k in ('id', 'center', 'radius', 'y', 'min', 'max')}
            for lock in (manifest or {}).get('protect', []) + (protects or {}).get('protect', [])]
    for x, z, r in keep_columns:
        deny.append({'id': f'keep-{x}-{z}', 'min': [x - r, -64, z - r], 'max': [x + r, 319, z + r]})
    keep = _column_locks(deny) if keep_protected else None
    header = {'map': map_id, 'stamp': (protects or {}).get('stamp', 'unbound'), 'purpose': purpose, 'author': 'tools/mapforge adopt batch',
              'locks': {'allow': [{'id': 'world', 'min': [lo[0], lo[1], lo[2]], 'max': [hi[0], top, hi[2]]}], 'deny': deny},
              'source': world.name}
    box = ((lo[0], -64, lo[2]), (hi[0], 319, hi[2])) if area else None
    return from_world(world, 'batch', header, box=box, top=top, keep=keep)


def mod_version():
    for line in (ROOT / 'gradle.properties').read_text().splitlines():
        if line.startswith('mod_version='):
            return line.split('=', 1)[1].strip()
    return 'unknown'


def check_overlay(overlay, base):
    """Problems with an overlay against the capture it was made from."""
    errors = []
    if overlay.kind != 'overlay':
        errors.append(f'{overlay.kind} is not an overlay')
    if base.kind != 'capture':
        errors.append('the base is not a capture')
    for key in ('map', 'stamp'):
        if overlay.header.get(key) != base.header.get(key):
            errors.append(f'{key} {overlay.header.get(key)!r} differs from the base capture\'s {base.header.get(key)!r}')
    if overlay.header.get('baseDigest') != base.digest_over(overlay):
        errors.append('baseDigest does not match the base capture: the overlay was made against another base')
    return errors


def main(argv):
    ap = argparse.ArgumentParser(prog='mapforge adopt', description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest='cmd', required=True)
    p = sub.add_parser('batch')
    p.add_argument('world'); p.add_argument('out'); p.add_argument('--map', required=True)
    p.add_argument('--manifest'); p.add_argument('--top', type=int)
    p.add_argument('--keep-protected', action='store_true')
    p.add_argument('--keep-column', action='append', default=[], help='X,Z,R: a square of columns the batch leaves alone')
    p.add_argument('--purpose', default='adopt')
    p.add_argument('--area', help='X0,Z0,X1,Z1: only these columns (the allow lock)')
    p.add_argument('--protects', help="the game's own protects (/mapforge protects OUT): capture rings and spawn columns")
    p = sub.add_parser('validate'); p.add_argument('file'); p.add_argument('--base')
    p = sub.add_parser('info'); p.add_argument('file')
    p = sub.add_parser('diff'); p.add_argument('base'); p.add_argument('final'); p.add_argument('out')
    p = sub.add_parser('apply'); p.add_argument('patch'); p.add_argument('capture'); p.add_argument('out')
    p = sub.add_parser('bake'); p.add_argument('overlay'); p.add_argument('--base', required=True)
    p.add_argument('--target', choices=sorted(TARGETS), required=True); p.add_argument('--approved-by')
    a = ap.parse_args(argv)
    try:
        if a.cmd == 'batch':
            keep = [tuple(int(v) for v in s.split(',')) for s in a.keep_column]
            world = World.load(a.world)
            b = compile_batch(world, a.map, mf.load_manifest(a.manifest) if a.manifest else None, a.top, a.keep_protected, keep, a.purpose,
                              area=tuple(int(v) for v in a.area.split(',')) if a.area else None,
                              protects=json.loads(pathlib.Path(a.protects).read_text()) if a.protects else None)
            size = b.write(a.out)
            print(f'batch {a.out}: {len(b.columns)} columns, {b.cells()} cells, {size} bytes, digest {b.header["digest"][:12]}')
            return 0
        if a.cmd == 'validate':
            errors, r = validate_file(a.file)
            if r is not None and a.base:
                errors += check_overlay(r, Runs.read(a.base))
            print('\n'.join(errors) or f'{a.file}: sound {r.kind}, {r.cells()} cells')
            return 1 if errors else 0
        if a.cmd == 'info':
            print(json.dumps(Runs.read(a.file).header, indent=1, sort_keys=True))
            return 0
        if a.cmd == 'diff':
            base, final = Runs.read(a.base), Runs.read(a.final)
            if base.header.get('stamp') != final.header.get('stamp'):
                print('REFUSED: the generator stamp changed between the base and the final capture', file=sys.stderr)
                return 2
            out = diff_runs(base, final, {'map': final.header.get('map'), 'stamp': final.header.get('stamp'),
                                          'source': f'mapforge adopt diff', 'finalDigest': final.digest()})
            size = out.write(a.out)
            print(f'overlay {a.out}: {out.cells()} cells, {len(out.be)} block entities, {size} bytes')
            return 0
        if a.cmd == 'apply':
            out = apply_runs(Runs.read(a.patch), Runs.read(a.capture))
            out.write(a.out)
            print(f'capture {a.out}: digest {out.digest()[:12]}')
            return 0
        if a.cmd == 'bake':
            overlay, base = Runs.read(a.overlay), Runs.read(a.base)
            errors = check_overlay(overlay, base)
            if a.target == 'release' and not a.approved_by:
                errors.append('a release bake ships in the player JAR and needs Kort\'s approval: pass --approved-by')
            if errors:
                print('REFUSED:\n  ' + '\n  '.join(errors), file=sys.stderr)
                return 2
            overlay.header['modVersion'] = mod_version()
            overlay.header['bakedFrom'] = pathlib.Path(a.overlay).name
            if a.approved_by:
                overlay.header['approvedBy'] = a.approved_by
            target = TARGETS[a.target]
            target.mkdir(parents=True, exist_ok=True)
            out = target / f'{overlay.header["map"]}.runs.gz'
            size = overlay.write(out)
            print(f'baked {out}: {overlay.cells()} cells, {size} bytes, stamp {overlay.header["stamp"][:12]}, mod {overlay.header["modVersion"]}')
            return 0
    except (RunsError, OSError) as e:
        print(f'adopt: {e}', file=sys.stderr)
        return 1
    return 1
