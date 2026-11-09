"""Builder bundle pins, the builder pack (plan task B1), the packwiz pack and the self-updating Prism instance zip."""
import hashlib, importlib.util, io, json, pathlib, re, subprocess, sys, tempfile, tomllib, unittest, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
TOOL = ROOT / 'tools' / 'builder-server' / 'make_builder_pack.py'
PACKWIZ = ROOT / 'tools' / 'builder-server' / 'make_packwiz.py'
MODS = json.loads((ROOT / 'tools' / 'builder-server' / 'builder-mods.json').read_text(encoding='utf8'))['mods']
PINS = json.loads((ROOT / 'tools' / 'builder-server' / 'packwiz-pins.json').read_text(encoding='utf8'))['files']
LOCK = json.loads((ROOT / 'dependencies' / 'platform-lock.json').read_text(encoding='utf8'))
BUNDLE = {'voicechat-fabric-1.20.1-2.6.22.jar': 'both',
          'xaerominimap-fabric-1.20.1-26.6.0.jar': 'client',
          'xaeroworldmap-fabric-1.20.1-1.47.0.jar': 'client'}


def run(*args):
    return subprocess.run([sys.executable, str(TOOL), *args], capture_output=True, text=True, cwd=ROOT)


def load(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def fake_jar(version='9.9.9'):
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w') as z:
        # a fixed entry time: writestr(name) stamps the clock, so two calls a 2 s tick apart made test_deterministic flaky
        z.writestr(zipfile.ZipInfo('fabric.mod.json', (2026, 1, 1, 0, 0, 0)), json.dumps({'id': 'gravesandglory', 'version': version}))
    return buf.getvalue()


class BuilderPackChecks(unittest.TestCase):
    def test_every_pin_complete(self):
        for m in MODS:
            with self.subTest(m.get('filename')):
                self.assertTrue(m['filename'].endswith('.jar'))
                self.assertTrue(m['url'].startswith('https://cdn.modrinth.com/'))
                self.assertGreater(m['size'], 0)
                self.assertRegex(m['sha1'], r'^[0-9a-f]{40}$')
                self.assertRegex(m['sha512'], r'^[0-9a-f]{128}$')
                self.assertIn(m['side'], ('server', 'client', 'both'))

    def test_bundle_pinned_and_approved(self):
        by = {m['filename']: m for m in MODS}
        for name, side in BUNDLE.items():
            with self.subTest(name):
                self.assertIn(name, by)
                self.assertEqual(by[name]['side'], side)
                self.assertEqual(by[name].get('approvedBy'), 'Kort 2026-11-08')

    def test_pack_adds_bundle(self):
        with tempfile.TemporaryDirectory() as tmp:
            src = pathlib.Path(tmp) / 'game.mrpack'
            with zipfile.ZipFile(src, 'w') as z:
                z.writestr('modrinth.index.json', json.dumps({'formatVersion': 1, 'game': 'minecraft', 'versionId': '0.5.1',
                                                               'name': 'Graves and Glory', 'files': [],
                                                               'dependencies': {'minecraft': '1.20.1'}}))
                z.writestr('overrides/config/x.txt', 'x')
            out = pathlib.Path(tmp) / 'builder.mrpack'
            r = run('--source', str(src), '--out', str(out))
            self.assertEqual(r.returncode, 0, r.stderr)
            with zipfile.ZipFile(out) as z:
                files = {f['path']: f for f in json.loads(z.read('modrinth.index.json'))['files']}
        self.assertEqual(files['mods/voicechat-fabric-1.20.1-2.6.22.jar']['env'], {'client': 'required', 'server': 'required'})
        for x in ('xaerominimap-fabric-1.20.1-26.6.0.jar', 'xaeroworldmap-fabric-1.20.1-1.47.0.jar'):
            self.assertEqual(files['mods/' + x]['env'], {'client': 'required', 'server': 'unsupported'})
        self.assertIn('mods/DistantHorizons-3.3.3-1.20.1-fabric-forge.jar', files)
        self.assertFalse([p for p in files if re.search('litematica', p, re.I)])

    def test_server_jars_listed(self):
        r = run('--server-jars')
        self.assertEqual(r.returncode, 0, r.stderr)
        lines = r.stdout.split()
        for m in MODS:
            if m['side'] in ('server', 'both'):
                self.assertIn(m['filename'], lines)
            else:
                self.assertNotIn(m['filename'], lines)
        self.assertIn('voicechat-fabric-1.20.1-2.6.22.jar', lines)


class PackwizChecks(unittest.TestCase):
    """make_packwiz.py: pins, sides, every builder client mod, server-only absent, index hashes match the files."""
    def setUp(self):
        self.pw = load(PACKWIZ, 'make_packwiz')
        self.tmp = tempfile.TemporaryDirectory()
        self.out = pathlib.Path(self.tmp.name) / 'pack'
        self.manifest = json.loads((ROOT / 'dependencies' / 'manifest.json').read_text(encoding='utf8'))

    def tearDown(self):
        self.tmp.cleanup()

    def make(self, lock=None, builder=None, extra=None):
        return self.pw.build(self.out, fake_jar(), extra or {}, '20261108', lock or LOCK, self.manifest, builder or MODS)

    def test_pack_contents(self):
        version, files = self.make()
        self.assertTrue(version.startswith('9.9.9-') and version.endswith('-20261108'))
        pack = tomllib.loads((self.out / 'pack.toml').read_text(encoding='utf8'))
        self.assertEqual(pack['versions'], {'minecraft': '1.20.1', 'fabric': '0.16.14'})
        index_bytes = (self.out / 'index.toml').read_bytes()
        self.assertEqual(pack['index']['hash'], hashlib.sha256(index_bytes).hexdigest())
        index = tomllib.loads(index_bytes.decode('utf8'))
        on_disk = sorted(p.relative_to(self.out).as_posix() for p in self.out.rglob('*') if p.is_file())
        self.assertEqual(sorted([f['file'] for f in index['files']] + ['index.toml', 'pack.toml']), on_disk)
        metas = {}
        for f in index['files']:
            data = (self.out / f['file']).read_bytes()
            self.assertEqual(f['hash'], hashlib.sha256(data).hexdigest(), f['file'])
            self.assertEqual(f.get('metafile', False), f['file'].endswith('.pw.toml'))
            if f.get('metafile'):
                metas[f['file']] = tomllib.loads(data.decode('utf8'))
        by_name = {m['filename']: (path, m) for path, m in metas.items()}
        for m in MODS:
            if m['side'] == 'server':
                self.assertNotIn(m['filename'], by_name)
                continue
            path, meta = by_name[m['filename']]
            self.assertEqual(meta['side'], m['side'])
            self.assertEqual(meta['download'], {'url': m['url'], 'hash-format': 'sha512', 'hash': m['sha512']})
            self.assertRegex(meta['update']['modrinth']['mod-id'], '^[A-Za-z0-9]{8}$')
        for row in LOCK:
            path, meta = by_name[row['fileName']]
            self.assertTrue(path.startswith({'mod': 'mods/', 'shader': 'shaderpacks/', 'resourcepack': 'resourcepacks/'}[row['kind']]))
            self.assertEqual(meta['download']['hash'], row['hashes']['sha512'])
            self.assertEqual(meta['update']['modrinth'], {'mod-id': row['projectId'], 'version': row['versionId']})
            self.assertEqual(meta['side'], 'client' if row['env']['server'] == 'unsupported' else 'both')
        for name in ('Axiom-6.1.5-for-MC1.20.1.jar', 'DistantHorizons-3.3.3-1.20.1-fabric-forge.jar', 'fabric-api-0.92.2+1.20.1.jar', *BUNDLE):
            self.assertIn(name, by_name)
        for name in ('worldedit-mod-7.2.15.jar', 'ledger-1.2.8.jar', 'fabric-language-kotlin-1.10.20+kotlin.1.9.24.jar'):
            self.assertNotIn(name, by_name)
        self.assertIn('mods/graves-and-glory-9.9.9.jar', on_disk)
        self.assertFalse([p for p in on_disk if re.search('worldedit|ledger|kotlin', p, re.I)])

    def test_deterministic(self):
        _, a = self.make()
        _, b = self.make()
        self.assertEqual(a, b)

    def test_refuses_bad_pins(self):
        lock = json.loads(json.dumps(LOCK))
        lock[0]['hashes']['sha512'] = '0' * 128
        with self.assertRaises(self.pw.PinError):
            self.make(lock=lock)
        builder = json.loads(json.dumps(MODS))
        builder[0]['url'] = 'https://example.com/x.jar'
        with self.assertRaises(self.pw.PinError):
            self.make(builder=builder)
        with self.assertRaises(self.pw.PinError):
            self.make(extra={'../evil.txt': b'x'})
        with self.assertRaises(self.pw.PinError):
            self.pw.build(self.out, b'not a jar', {}, '20261108', LOCK, self.manifest, MODS)

    def test_refuses_local_copy_off_pin(self):
        cache = pathlib.Path(self.tmp.name) / 'cache'
        cache.mkdir()
        client = next(m for m in MODS if m['side'] == 'client')
        (cache / client['filename']).write_bytes(b'tampered')
        with self.assertRaises(self.pw.PinError):
            self.pw.build(self.out, fake_jar(), {}, '20261108', LOCK, self.manifest, MODS, cache)

    def test_overrides_and_game_pack(self):
        src = pathlib.Path(self.tmp.name) / 'game.mrpack'
        lock_files = [{'path': self.pw.FOLDER[r['kind']] + '/' + r['fileName'], 'hashes': r['hashes']} for r in LOCK]
        with zipfile.ZipFile(src, 'w') as z:
            z.writestr('modrinth.index.json', json.dumps({'formatVersion': 1, 'files': lock_files}))
            z.writestr('overrides/mods/graves-and-glory-9.9.9.jar', fake_jar())
            z.writestr('overrides/docs/README.md', 'hi')
        jar, extra = self.pw.game_pack_files(src, LOCK)
        self.assertEqual(extra, {'docs/README.md': b'hi'})
        self.pw.build(self.out, jar, extra, '20261108', LOCK, self.manifest, MODS)
        self.assertEqual((self.out / 'docs' / 'README.md').read_bytes(), b'hi')
        with zipfile.ZipFile(src, 'w') as z:
            z.writestr('modrinth.index.json', json.dumps({'formatVersion': 1, 'files': lock_files[1:]}))
        with self.assertRaises(self.pw.PinError):
            self.pw.game_pack_files(src, LOCK)

    def test_refuses_to_replace_other_folder(self):
        self.out.mkdir()
        (self.out / 'keep.txt').write_text('mine')
        with self.assertRaises(self.pw.PinError):
            self.make()
        self.assertTrue((self.out / 'keep.txt').is_file())

    def test_default_base_url(self):
        self.assertEqual(self.pw.DEFAULT_BASE_URL, 'http://usc-tx-b5-i1-s.kineticpanel.net:25589/pack/')
        self.assertEqual(load(TOOL, 'make_builder_pack').DEFAULT_BASE_URL, self.pw.DEFAULT_BASE_URL)


class PrismZipChecks(unittest.TestCase):
    def setUp(self):
        self.bp = load(TOOL, 'make_builder_pack')
        self.tmp = tempfile.TemporaryDirectory()
        self.dir = pathlib.Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def test_pins_recorded(self):
        self.assertEqual({p['name']: p['size'] for p in PINS}, {'packwiz-installer-bootstrap.jar': 98989, 'packwiz-installer.jar': 4378828})
        for p in PINS:
            self.assertRegex(p['sha256'], '^[0-9a-f]{64}$')
            self.assertTrue(p['url'].startswith('https://github.com/packwiz/'))

    def test_pre_launch(self):
        cmd = self.bp.pre_launch('http://example.test:25589/pack/')
        self.assertEqual(cmd, '"$INST_JAVA" -jar packwiz-installer-bootstrap.jar --bootstrap-no-update '
                              '--bootstrap-main-jar packwiz-installer.jar http://example.test:25589/pack/pack.toml')
        self.assertEqual(self.bp.ini_value('"a" b'), '"\\"a\\" b"')
        with self.assertRaises(ValueError):
            self.bp.pre_launch('http://x/pack/ ; rm -rf')

    def test_zip(self):
        jars = {'packwiz-installer-bootstrap.jar': b'boot', 'packwiz-installer.jar': b'inst'}
        out = self.bp.prism_zip(self.dir / 'p.zip', 'http://h.test:1/pack/', jars, '0.5.1')
        with zipfile.ZipFile(out) as z:
            self.assertEqual(sorted(z.namelist()), ['.minecraft/packwiz-installer-bootstrap.jar', '.minecraft/packwiz-installer.jar',
                                                    'instance.cfg', 'mmc-pack.json'])
            cfg = z.read('instance.cfg').decode('utf8')
            pack = json.loads(z.read('mmc-pack.json'))
        self.assertIn('OverrideCommands=true', cfg)
        line = re.search('^PreLaunchCommand=(.*)$', cfg, re.M).group(1)
        self.assertEqual(re.sub(r'\\(.)', r'\1', line[1:-1]), self.bp.pre_launch('http://h.test:1/pack/'))
        comps = {c['uid']: c['version'] for c in pack['components']}
        self.assertEqual(comps, {'net.minecraft': '1.20.1', 'net.fabricmc.intermediary': '1.20.1', 'net.fabricmc.fabric-loader': '0.16.14'})
        again = self.bp.prism_zip(self.dir / 'q.zip', 'http://h.test:1/pack/', jars, '0.5.1')
        self.assertEqual(out.read_bytes(), again.read_bytes())

    def test_refuses_unpinned_jars(self):
        for p in PINS:
            (self.dir / p['name']).write_bytes(b'wrong')
        r = run('--prism', '--packwiz-jars', str(self.dir), '--out', str(self.dir / 'x.zip'))
        self.assertNotEqual(r.returncode, 0)
        self.assertIn('does not match its pin', r.stderr)
        self.assertFalse((self.dir / 'x.zip').exists())


if __name__ == '__main__':
    unittest.main()
