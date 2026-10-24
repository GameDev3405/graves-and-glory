"""The preflight rules of the supply houses (Phase 3 Task 8b), each with a negative test: the clean sheet passes, and a sheet with one thing wrong with a house is refused with the rule's message.
preflight.py finds the sheets and the structure files from its own place (root = the folder above tools/), so each case runs a copy of it in a temporary tree with its own design.json; the real Spec.java and sheets/preflight.json are never written."""
import json, pathlib, shutil, subprocess, sys, tempfile, unittest

REAL = pathlib.Path(__file__).resolve().parents[2]


class PreflightHouses(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = pathlib.Path(tempfile.mkdtemp(prefix='preflight-houses-'))
        (cls.tmp / 'tools').mkdir()
        shutil.copy(REAL / 'tools' / 'preflight.py', cls.tmp / 'tools' / 'preflight.py')
        shutil.copytree(REAL / 'sheets', cls.tmp / 'sheets')
        shutil.copytree(REAL / 'src' / 'main' / 'resources', cls.tmp / 'src' / 'main' / 'resources')
        shutil.copytree(REAL / 'test-tools' / 'resources', cls.tmp / 'test-tools' / 'resources')
        cls.design = json.loads((REAL / 'sheets' / 'design.json').read_text())

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.tmp, ignore_errors=True)

    def run_with(self, mutate):
        data = json.loads(json.dumps(self.design))
        mutate(data)
        (self.tmp / 'sheets' / 'design.json').write_text(json.dumps(data))
        r = subprocess.run([sys.executable, str(self.tmp / 'tools' / 'preflight.py')], capture_output=True, text=True)
        return r.returncode, r.stdout + r.stderr

    @staticmethod
    def land(data, id):
        return next(r for r in data['landmarks'] if r['id'] == id)

    @staticmethod
    def house(data, id):
        return next(r for r in data['supplyHouses'] if r['id'] == id)

    def refused(self, mutate, *words):
        code, out = self.run_with(mutate)
        self.assertEqual(code, 1, 'the sheet should be refused, preflight said: ' + out[-400:])
        for w in words:
            self.assertIn(w, out)

    def test_clean_sheet_passes(self):
        code, out = self.run_with(lambda d: None)
        self.assertEqual(code, 0, out[-600:])
        self.assertIn('Preflight clean', out)

    def test_house_replaces_nothing(self):
        # the review's mutation: a house row that replaces house_0 was accepted
        self.refused(lambda d: self.land(d, 'rg-supply-nw').update(replaces='house_0'), 'landmarks/rg-supply-nw', 'replaces')

    def test_house_encloses_nothing(self):
        self.refused(lambda d: self.land(d, 'ih-supply-sw').update(encloses='dawn_point'), 'landmarks/ih-supply-sw', 'encloses')

    def test_partner_has_the_same_elevation(self):
        def m(d):
            self.land(d, 'ih-supply-nw').update(elevation=76)
            self.house(d, 'ih-supply-nw').update(elevation=76)
        self.refused(m, 'partner')

    def test_yard_is_the_local_15_24(self):
        self.refused(lambda d: self.house(d, 'fm-supply-ne').update(yardX=self.house(d, 'fm-supply-ne')['yardX'] + 1), 'yard centre')

    def test_another_landmark_must_keep_off_the_house(self):
        def m(d):
            h = self.land(d, 'hw-supply-sw')
            o = self.land(d, 'hw-market')
            o.update(x=h['x'] + 31 // 2 + 19 // 2 + 1 + 5, z=h['z'])  # 5 blocks from the footprint, under the 12 the spacing rule asks for
        self.refused(m, 'blocks from this house')


if __name__ == '__main__':
    unittest.main()
