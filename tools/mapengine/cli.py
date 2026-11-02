"""Command line for the map engine: new, check, build, serve."""
import argparse
import pathlib

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
    print(f'{args.cmd}: not implemented yet')
    return 2
