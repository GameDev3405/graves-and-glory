"""Make the builder-only client pack: a self-updating Prism instance zip (--prism), or the game .mrpack plus the client/both builder mods.

Reads dist/graves-and-glory-<version>.mrpack (never rebuilds it) and writes dist/graves-and-glory-<version>-builder.mrpack.
The extra mods are Modrinth download entries listed in tools/builder-server/builder-mods.json (side client or both).
Local file only; it publishes nothing. Not part of the game release.

--prism writes dist/graves-and-glory-<version>-builder-prism.zip, a Prism Launcher instance (import once). Its pre-launch command runs the
pinned packwiz installer (tools/builder-server/packwiz-pins.json, jars from work/packwiz-dl/) against the pack.toml the builder server serves
(make_packwiz.py output on gg.packwiz.dir), so every launch pulls the current builder pack; the installer never updates itself from GitHub.

    python tools/builder-server/make_builder_pack.py --prism [--base-url URL] [--packwiz-jars DIR] [--out PACK-builder-prism.zip]
    python tools/builder-server/make_builder_pack.py [--source PACK.mrpack] [--out PACK-builder.mrpack]
    python tools/builder-server/make_builder_pack.py --server-jars   (prints the jars that belong in the server's mods/)
"""
import argparse, hashlib, json, pathlib, re, sys, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
MODS = pathlib.Path(__file__).resolve().with_name('builder-mods.json')
PINS = pathlib.Path(__file__).resolve().with_name('packwiz-pins.json')
DEFAULT_BASE_URL = 'http://usc-tx-b5-i1-s.kineticpanel.net:25589/pack/'  # same as make_packwiz.py
BOOTSTRAP, INSTALLER = 'packwiz-installer-bootstrap.jar', 'packwiz-installer.jar'
ENV = {'client': {'client': 'required', 'server': 'unsupported'},
       'both': {'client': 'required', 'server': 'required'}}


def pre_launch(base_url):
    """Prism runs this in the instance's game folder (.minecraft) before every launch; $INST_JAVA is the instance's Java."""
    url = base_url.rstrip('/') + '/pack.toml'
    if not re.fullmatch(r'https?://[A-Za-z0-9.:\[\]-]+(/[A-Za-z0-9._~%/+-]*)?', url):
        raise ValueError('base URL must be a plain http(s) URL: ' + base_url)
    return f'"$INST_JAVA" -jar {BOOTSTRAP} --bootstrap-no-update --bootstrap-main-jar {INSTALLER} {url}'


def ini_value(text):
    """A QSettings ini string value: quoted, with backslash and quote escaped (unquoted, Qt drops the quotes around $INST_JAVA)."""
    return '"' + text.replace('\\', '\\\\').replace('"', '\\"') + '"'


def pinned_jars(folder):
    """The two packwiz jars, each checked against its size and sha256 pin."""
    out = {}
    for pin in json.loads(PINS.read_text(encoding='utf8'))['files']:
        path = pathlib.Path(folder) / pin['name']
        if not path.is_file():
            sys.exit(f'{path} missing; download the approved {pin["url"]} into {folder}')
        data = path.read_bytes()
        if len(data) != pin['size'] or hashlib.sha256(data).hexdigest() != pin['sha256']:
            sys.exit(f'Refused: {path} does not match its pin in {PINS.name} (size {pin["size"]}, sha256 {pin["sha256"]})')
        out[pin['name']] = data
    return out


def prism_zip(out, base_url, jars, version):
    """Writes the Prism instance zip (fixed timestamps, so the same inputs give the same bytes)."""
    cfg = '\n'.join(['[General]', 'ConfigVersion=1.2', 'InstanceType=OneSix', f'name=Graves and Glory Builder {version}',
                     'iconKey=default', 'OverrideCommands=true', 'PreLaunchCommand=' + ini_value(pre_launch(base_url)),
                     'WrapperCommand=', 'PostExitCommand=', ''])
    pack = {'formatVersion': 1, 'components': [
        {'uid': 'net.minecraft', 'version': '1.21.1', 'important': True, 'cachedName': 'Minecraft'},
        {'uid': 'net.fabricmc.intermediary', 'version': '1.21.1', 'dependencyOnly': True, 'cachedName': 'Intermediary Mappings'},
        {'uid': 'net.fabricmc.fabric-loader', 'version': '0.19.5', 'cachedName': 'Fabric Loader'}]}
    items = [('instance.cfg', cfg.encode('utf8')), ('mmc-pack.json', (json.dumps(pack, indent=2) + '\n').encode('utf8')),
             ('.minecraft/' + BOOTSTRAP, jars[BOOTSTRAP]), ('.minecraft/' + INSTALLER, jars[INSTALLER])]
    tmp = out.with_name(out.name + '.tmp')
    with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as z:
        for name, data in items:
            info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            z.writestr(info, data)
    tmp.replace(out)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--source', type=pathlib.Path, help='game pack to start from (default dist/graves-and-glory-<mod_version>.mrpack)')
    ap.add_argument('--out', type=pathlib.Path, help='output (default: source name with -builder before .mrpack)')
    ap.add_argument('--server-jars', action='store_true', help="print the builder jars (side server or both) for the server's mods/ and exit")
    ap.add_argument('--prism', action='store_true', help='write the self-updating Prism instance zip instead of the .mrpack')
    ap.add_argument('--base-url', default=DEFAULT_BASE_URL, help='folder URL the server serves the packwiz pack at (ends in /pack/)')
    ap.add_argument('--packwiz-jars', type=pathlib.Path, default=ROOT / 'work' / 'packwiz-dl', help='folder holding the two pinned packwiz jars')
    args = ap.parse_args()
    if args.server_jars:
        for m in json.loads(MODS.read_text(encoding='utf8'))['mods']:
            if m['side'] in ('server', 'both'):
                print(m['filename'])
        return
    version = re.search(r'^mod_version=(.+)$', (ROOT / 'gradle.properties').read_text(encoding='utf8'), re.M).group(1).strip()
    if args.prism:
        out = args.out or ROOT / 'dist' / f'graves-and-glory-{version}-builder-prism.zip'
        out.parent.mkdir(parents=True, exist_ok=True)
        try:
            cmd = pre_launch(args.base_url)
        except ValueError as e:
            sys.exit(str(e))
        prism_zip(out, args.base_url, pinned_jars(args.packwiz_jars), version)
        print(f'{out}\n  pre-launch: {cmd}')
        return
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
