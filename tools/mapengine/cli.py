"""Command line for the map engine: new, check, build, serve."""
import argparse
import pathlib

import hashlib
import json

from mapengine import build as mbuild
from mapengine import check as mcheck
from mapengine import design

ROOT = pathlib.Path(__file__).resolve().parents[2]


def main(argv=None, root=None):
    root = pathlib.Path(root) if root else ROOT
    ap = argparse.ArgumentParser(prog='mapengine')
    sub = ap.add_subparsers(dest='cmd', required=True)
    p = sub.add_parser('new', help='create maps/<id>/design.json')
    p.add_argument('id')
    p.add_argument('name')
    for name in ('check', 'build'):
        sub.add_parser(name).add_argument('id')
    p = sub.add_parser('serve')
    p.add_argument('--port', type=int, default=8765)
    p.add_argument('--no-open', action='store_true')
    args = ap.parse_args(argv)
    if args.cmd == 'new':
        path = root / 'maps' / args.id / 'design.json'
        if path.exists():
            print(f'{path} already exists')
            return 1
        design.save(path, design.new_design(args.id, args.name))
        print(f'wrote {path}')
        return 0
    if args.cmd == 'build':
        d = design.load(root / 'maps' / args.id / 'design.json')
        out = root / 'work' / 'mapengine' / args.id
        try:
            m = mbuild.build(d, out)
        except mbuild.BuildError as e:
            print('cannot build: ' + '; '.join(e.problems))
            return 1
        print(f"built {len(m['tiles'])} tiles into {out}")
        return 0
    if args.cmd == 'check':
        d = design.load(root / 'maps' / args.id / 'design.json')
        out = root / 'work' / 'mapengine' / args.id
        try:
            stale = True
            if (out / 'map.json').exists():
                stale = json.loads((out / 'map.json').read_text(encoding='utf-8')).get('design_sha256') !=                     hashlib.sha256(mbuild.canonical_bytes(d)).hexdigest()
            if stale:
                mbuild.build(d, out)
        except mbuild.BuildError as e:
            print('cannot build: ' + '; '.join(e.problems))
            return 1
        issues = mcheck.check(d, out)
        for i in issues:
            print(f'{i.code} {i.object_id}: {i.message}')
        if issues:
            return 1
        print('OK: matches the design')
        return 0
    if args.cmd == 'serve':
        from mapengine import server
        srv = server.make_server(root / 'maps', root / 'work' / 'mapengine', args.port)
        url = f'http://127.0.0.1:{srv.server_address[1]}/'
        print(f'map engine at {url} (Ctrl+C to stop)')
        if not args.no_open:
            import webbrowser
            webbrowser.open(url)
        try:
            srv.serve_forever()
        except KeyboardInterrupt:
            pass
        return 0
    return 2
