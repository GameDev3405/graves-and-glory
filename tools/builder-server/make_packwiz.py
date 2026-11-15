"""Make the self-updating builder pack: a packwiz pack folder the builder server serves at /pack/ (gg.packwiz.dir).

Every client-side mod of the full builder pack, as packwiz metafiles with the pinned download URL and hash:
  - the game pack's files (dependencies/platform-lock.json: Fabric API, Sodium, Iris ..., shaders, the resource pack),
  - the client/both builder mods (tools/builder-server/builder-mods.json: Axiom, Distant Horizons, Simple Voice Chat, Xaero's maps).
Server-only builder jars (WorldEdit, Ledger, Fabric Language Kotlin) are left out. The Graves and Glory jar is a plain file in the folder
(mods/graves-and-glory-<ver>.jar), so it is served from the builder server itself; so are the game pack's overrides (docs) when a game .mrpack
is given. It refuses when any pin disagrees (platform lock against dependencies/manifest.json, a local copy against its pin, a game pack entry
against the lock). Output is deterministic for the same inputs and --date. Local files only; it downloads and publishes nothing.

    python tools/builder-server/make_packwiz.py [--jar JAR] [--game-pack PACK.mrpack] [--out dist/builder-packwiz] [--date YYYYMMDD]

Publish: copy the output folder's contents to the server's gg.packwiz.dir folder (replace it whole). The next launch of every builder
instance pulls the change.
"""
import argparse, datetime, hashlib, json, pathlib, re, shutil, sys, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
HERE = pathlib.Path(__file__).resolve().parent
BUILDER_MODS = HERE / 'builder-mods.json'
LOCK = ROOT / 'dependencies' / 'platform-lock.json'
MANIFEST = ROOT / 'dependencies' / 'manifest.json'
DEFAULT_BASE_URL = 'http://usc-tx-b5-i1-s.kineticpanel.net:25589/pack/'
DEFAULT_OUT = ROOT / 'dist' / 'builder-packwiz'
MINECRAFT = '1.21.1'
FABRIC_LOADER = '0.19.5'
FOLDER = {'mod': 'mods', 'shader': 'shaderpacks', 'resourcepack': 'resourcepacks'}
SAFE_SEGMENT = re.compile(r'^[A-Za-z0-9_+-][A-Za-z0-9._+-]*$')
MODRINTH = re.compile(r'^https://cdn\.modrinth\.com/data/([A-Za-z0-9]{8})/versions/([A-Za-z0-9]{8})/[^/]+$')


class PinError(Exception):
    pass


def q(text):
    """A TOML basic string."""
    return json.dumps(str(text), ensure_ascii=False)


def digest(data, algo):
    return hashlib.new(algo, data).hexdigest()


def safe_path(path):
    """The served-path rule the mod's HTTP guard applies (no dot names, no colon or backslash): refuse anything it would not serve."""
    parts = path.split('/')
    if not parts or not all(SAFE_SEGMENT.match(p) for p in parts):
        raise PinError(f'Unsafe or unservable pack path: {path!r} (letters, digits, . _ + - only; no leading dot)')
    return path


def check_hashes(name, h, size, url):
    if not re.fullmatch('[0-9a-f]{40}', h.get('sha1', '')) or not re.fullmatch('[0-9a-f]{128}', h.get('sha512', '')):
        raise PinError(f'{name}: malformed sha1/sha512 pin')
    if not isinstance(size, int) or size <= 0:
        raise PinError(f'{name}: bad size pin')
    if not MODRINTH.match(url):
        raise PinError(f'{name}: download must be a cdn.modrinth.com version file, got {url}')


def check_local(name, path, h, size):
    """When a local copy exists, it must match its pin exactly."""
    if path and path.is_file():
        data = path.read_bytes()
        if len(data) != size or digest(data, 'sha1') != h['sha1'] or digest(data, 'sha512') != h['sha512']:
            raise PinError(f'{name}: local file {path} does not match its pin')


def game_mods(lock_rows, manifest_rows, root=ROOT):
    """Packwiz entries for the game pack's client files, cross-checked against dependencies/manifest.json."""
    by_file = {pathlib.PurePosixPath(m['path']).name: m for m in manifest_rows}
    out = []
    for row in lock_rows:
        name = row['fileName']
        check_hashes(name, row['hashes'], row['fileSize'], row['downloads'][0])
        pin = by_file.get(name) or {}  # Fabric API is pinned in the lock only; the rest are in both
        if pin and (pin['sha512'] != row['hashes']['sha512'] or pin['size'] != row['fileSize']):
            raise PinError(f'{name}: platform-lock.json and dependencies/manifest.json disagree')
        if row.get('sha256') and pin.get('sha256') and row['sha256'] != pin['sha256']:
            raise PinError(f'{name}: sha256 pins disagree')
        check_local(name, root / row['localPath'] if row.get('localPath') else None, row['hashes'], row['fileSize'])
        if row['env']['client'] == 'unsupported':
            continue
        m = MODRINTH.match(row['downloads'][0])
        out.append({'slug': row['slug'], 'name': pin.get('title') or row['slug'], 'folder': FOLDER[row['kind']],
                    'filename': name, 'url': row['downloads'][0], 'sha512': row['hashes']['sha512'],
                    'side': 'client' if row['env']['server'] == 'unsupported' else 'both',
                    'mod_id': row.get('projectId') or m.group(1), 'version_id': row.get('versionId') or m.group(2)})
    return out


def builder_mods(rows, cache=None):
    """Packwiz entries for the client and both-sided builder mods; server-only ones are left out."""
    out = []
    for row in rows:
        name = row['filename']
        if row['side'] not in ('server', 'client', 'both'):
            raise PinError(f'{name}: unknown side {row["side"]}')
        check_hashes(name, row, row['size'], row['url'])
        check_local(name, cache / name if cache else None, row, row['size'])
        if row['side'] == 'server':
            continue
        m = MODRINTH.match(row['url'])
        out.append({'slug': row['project'], 'name': row['project'], 'folder': 'mods', 'filename': name, 'url': row['url'],
                    'sha512': row['sha512'], 'side': row['side'], 'mod_id': m.group(1), 'version_id': m.group(2)})
    return out


def metafile(e):
    return '\n'.join([
        f'name = {q(e["name"])}',
        f'filename = {q(e["filename"])}',
        f'side = {q(e["side"])}',
        '',
        '[download]',
        f'url = {q(e["url"])}',
        'hash-format = "sha512"',
        f'hash = {q(e["sha512"])}',
        '',
        '[update]',
        '[update.modrinth]',
        f'mod-id = {q(e["mod_id"])}',
        f'version = {q(e["version_id"])}',
        ''])


def jar_version(data):
    """The Graves and Glory jar's version from its fabric.mod.json; refuses anything else."""
    import io
    try:
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            meta = json.loads(z.read('fabric.mod.json'))
    except (zipfile.BadZipFile, KeyError, ValueError) as e:
        raise PinError(f'not a Fabric mod jar: {e}')
    if meta.get('id') != 'gravesandglory':
        raise PinError(f'jar is mod {meta.get("id")!r}, not gravesandglory')
    return str(meta['version'])


def game_pack_files(pack, lock_rows):
    """The game .mrpack's jar and overrides (as {path: bytes}); its download entries must equal the lock."""
    lock = {FOLDER[r['kind']] + '/' + r['fileName']: r['hashes']['sha512'] for r in lock_rows}
    files, jar = {}, None
    with zipfile.ZipFile(pack) as z:
        index = json.loads(z.read('modrinth.index.json'))
        have = {f['path']: f['hashes']['sha512'] for f in index['files']}
        if have != lock:
            raise PinError(f'{pack.name}: download entries differ from dependencies/platform-lock.json; rebuild the game pack')
        for info in z.infolist():
            n = info.filename
            if info.is_dir() or not n.startswith('overrides/'):
                continue
            rel = n[len('overrides/'):]
            if re.fullmatch(r'mods/graves-and-glory-[^/]+\.jar', rel):
                if jar is not None:
                    raise PinError(f'{pack.name}: more than one Graves and Glory jar')
                jar = z.read(n)
            else:
                files[safe_path(rel)] = z.read(n)
    if jar is None:
        raise PinError(f'{pack.name}: no overrides/mods/graves-and-glory-*.jar')
    return jar, files


def build(out, jar_bytes, extra_files, date, lock_rows, manifest_rows, builder_rows, cache=None):
    """Writes the pack folder; returns (version, {path: bytes}) of everything written."""
    entries = game_mods(lock_rows, manifest_rows) + builder_mods(builder_rows, cache)
    gg = jar_version(jar_bytes)
    files = {}
    for e in entries:
        path = safe_path(f'{e["folder"]}/{e["slug"]}.pw.toml')
        if path in files:
            raise PinError(f'duplicate metafile {path}')
        files[path] = metafile(e).encode('utf8')
    names = [e['filename'] for e in entries]
    if len(names) != len(set(names)):
        raise PinError('a mod file is listed twice')
    files[safe_path(f'mods/graves-and-glory-{gg}.jar')] = jar_bytes
    for path, data in extra_files.items():
        if path in files:
            raise PinError(f'override {path} collides with a pack file')
        files[safe_path(path)] = data
    index = ['hash-format = "sha256"', '']
    for path in sorted(files):
        index += ['[[files]]', f'file = {q(path)}', f'hash = {q(digest(files[path], "sha256"))}']
        if path.endswith('.pw.toml'):
            index.append('metafile = true')
        index.append('')
    index_bytes = '\n'.join(index).encode('utf8')
    version = f'{gg}-{digest(jar_bytes, "sha256")[:8]}-{date}'
    pack = '\n'.join([
        'name = "Graves and Glory Builder"',
        'author = "Graves and Glory"',
        f'version = {q(version)}',
        'pack-format = "packwiz:1.1.0"',
        '',
        '[index]',
        'file = "index.toml"',
        'hash-format = "sha256"',
        f'hash = {q(digest(index_bytes, "sha256"))}',
        '',
        '[versions]',
        f'fabric = {q(FABRIC_LOADER)}',
        f'minecraft = {q(MINECRAFT)}',
        '']).encode('utf8')
    files['index.toml'] = index_bytes
    files['pack.toml'] = pack

    out = pathlib.Path(out)
    if out.exists() and any(out.iterdir()) and not (out / 'pack.toml').is_file():
        raise PinError(f'{out} exists and is not a packwiz folder; refusing to replace it')
    tmp = out.with_name(out.name + '.tmp')
    if tmp.exists():
        shutil.rmtree(tmp)
    for path, data in files.items():
        dest = tmp / path
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(data)
    if out.exists():
        shutil.rmtree(out)
    tmp.replace(out)
    return version, files


def load_json(path):
    return json.loads(pathlib.Path(path).read_text(encoding='utf8'))


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('--jar', type=pathlib.Path, help='Graves and Glory jar (default build/libs/graves-and-glory-<mod_version>.jar, or the game pack\'s)')
    ap.add_argument('--game-pack', type=pathlib.Path, help='game .mrpack: its jar and overrides go in, its entries must match the lock (default dist/graves-and-glory-<mod_version>.mrpack when present)')
    ap.add_argument('--out', type=pathlib.Path, default=DEFAULT_OUT, help='pack folder to (re)write (default dist/builder-packwiz)')
    ap.add_argument('--base-url', default=DEFAULT_BASE_URL, help='where the server serves the folder (only printed here; make_builder_pack.py --prism bakes it into the instance)')
    ap.add_argument('--date', default=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%d'), help='version stamp date, YYYYMMDD (default today, UTC)')
    ap.add_argument('--cache', type=pathlib.Path, help='folder of builder mod jars to verify against their pins, when present')
    args = ap.parse_args(argv)
    if not re.fullmatch(r'\d{8}', args.date):
        sys.exit('--date must be YYYYMMDD')
    mod_version = re.search(r'^mod_version=(.+)$', (ROOT / 'gradle.properties').read_text(encoding='utf8'), re.M).group(1).strip()
    lock_rows, manifest_rows = load_json(LOCK), load_json(MANIFEST)
    builder_rows = load_json(BUILDER_MODS)['mods']
    try:
        game = args.game_pack or ROOT / 'dist' / f'graves-and-glory-{mod_version}.mrpack'
        extra, jar_bytes = {}, None
        if args.game_pack or (game.is_file() and not args.jar):
            if not game.is_file():
                sys.exit(f'Game pack not found: {game}')
            jar_bytes, extra = game_pack_files(game, lock_rows)
        if args.jar:
            data = args.jar.read_bytes()
            if jar_bytes is not None and data != jar_bytes:
                raise PinError(f'{args.jar} differs from the jar inside {game.name}')
            jar_bytes = data
        if jar_bytes is None:
            jar = ROOT / 'build' / 'libs' / f'graves-and-glory-{mod_version}.jar'
            if not jar.is_file():
                sys.exit(f'Jar not found: {jar} (run ./gradlew build, or pass --jar or --game-pack)')
            jar_bytes = jar.read_bytes()
        version, files = build(args.out, jar_bytes, extra, args.date, lock_rows, manifest_rows, builder_rows, args.cache)
    except PinError as e:
        sys.exit('Refused: ' + str(e))
    metas = sorted(p for p in files if p.endswith('.pw.toml'))
    print(f'{args.out} version {version}: {len(metas)} metafiles, {len(files) - len(metas) - 2} plain files')
    print(f'Serve it with -Dgg.packwiz.dir=<copy of this folder>; builders read {args.base_url.rstrip("/")}/pack.toml')


if __name__ == '__main__':
    main()
