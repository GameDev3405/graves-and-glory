"""Builder bundle pins and the builder pack (plan task B1)."""
import json, pathlib, re, subprocess, sys, tempfile, unittest, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
TOOL = ROOT / 'tools' / 'builder-server' / 'make_builder_pack.py'
MODS = json.loads((ROOT / 'tools' / 'builder-server' / 'builder-mods.json').read_text(encoding='utf8'))['mods']
BUNDLE = {'voicechat-fabric-1.20.1-2.6.22.jar': 'both',
          'xaerominimap-fabric-1.20.1-26.6.0.jar': 'client',
          'xaeroworldmap-fabric-1.20.1-1.47.0.jar': 'client'}


def run(*args):
    return subprocess.run([sys.executable, str(TOOL), *args], capture_output=True, text=True, cwd=ROOT)


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


if __name__ == '__main__':
    unittest.main()
