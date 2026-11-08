"""Make the builder-only client pack: the game .mrpack plus the client/both builder mods.

Reads dist/graves-and-glory-<version>.mrpack (never rebuilds it) and writes dist/graves-and-glory-<version>-builder.mrpack.
The extra mods are Modrinth download entries listed in tools/builder-server/builder-mods.json (side client or both).
Local file only; it publishes nothing. Not part of the game release.

    python tools/builder-server/make_builder_pack.py [--source PACK.mrpack] [--out PACK-builder.mrpack]
    python tools/builder-server/make_builder_pack.py --server-jars   (prints the jars that belong in the server's mods/)
"""
import argparse, json, pathlib, re, sys, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
MODS = pathlib.Path(__file__).resolve().with_name('builder-mods.json')
ENV = {'client': {'client': 'required', 'server': 'unsupported'},
       'both': {'client': 'required', 'server': 'required'}}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--source', type=pathlib.Path, help='game pack to start from (default dist/graves-and-glory-<mod_version>.mrpack)')
    ap.add_argument('--out', type=pathlib.Path, help='output (default: source name with -builder before .mrpack)')
    ap.add_argument('--server-jars', action='store_true', help="print the builder jars (side server or both) for the server's mods/ and exit")
    args = ap.parse_args()
    if args.server_jars:
        for m in json.loads(MODS.read_text(encoding='utf8'))['mods']:
            if m['side'] in ('server', 'both'):
                print(m['filename'])
        return
    version = re.search(r'^mod_version=(.+)$', (ROOT / 'gradle.properties').read_text(encoding='utf8'), re.M).group(1).strip()
    source = args.source or ROOT / 'dist' / f'graves-and-glory-{version}.mrpack'
    out = args.out or source.with_name(source.name[:-len('.mrpack')] + '-builder.mrpack')
    if not source.is_file():
        sys.exit(f'Game pack not found: {source} (build it first; this script never rebuilds it)')
    mods = json.loads(MODS.read_text(encoding='utf8'))['mods']
    extra = [m for m in mods if m['side'] in ENV]
    unknown = [m['project'] for m in mods if m['side'] not in ('server', 'client', 'both')]
    if unknown:
        sys.exit('Unknown side for: ' + ', '.join(unknown))

    with zipfile.ZipFile(source) as src:
        index = json.loads(src.read('modrinth.index.json'))
        have = {f['path'] for f in index['files']}
        for m in extra:
            path = 'mods/' + m['filename']
            if not m['url'].startswith('https://cdn.modrinth.com/'):
                sys.exit('Download must come from cdn.modrinth.com: ' + m['url'])
            if path in have:
                sys.exit('Already in the game pack: ' + path)
            index['files'].append({'path': path, 'hashes': {'sha1': m['sha1'], 'sha512': m['sha512']},
                                   'env': ENV[m['side']], 'downloads': [m['url']], 'fileSize': m['size']})
        index['name'] = index['name'] + ' (Builder)'
        index['versionId'] = str(index['versionId']) + '-builder'
        index['summary'] = 'Builder pack: the game pack plus builder tools. Not the game release.'
        tmp = out.with_name(out.name + '.tmp')
        with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as dst:
            dst.writestr('modrinth.index.json', json.dumps(index, indent=2))
            for item in src.infolist():
                if item.filename != 'modrinth.index.json':
                    dst.writestr(item, src.read(item.filename))
    tmp.replace(out)

    with zipfile.ZipFile(out) as check:
        assert check.testzip() is None
        done = json.loads(check.read('modrinth.index.json'))
    assert done['formatVersion'] == 1 and len({f['path'] for f in done['files']}) == len(done['files'])
    print(f'{out} ({len(done["files"])} files; added {", ".join(m["filename"] for m in extra) or "nothing"})')


if __name__ == '__main__':
    main()
