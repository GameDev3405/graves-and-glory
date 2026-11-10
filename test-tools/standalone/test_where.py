"""tools/where.py smoke test: the overlay answers rules keys and troop ids, the rg fallback answers words nothing else knows."""
import pathlib, re, subprocess, sys, time, unittest
ROOT = pathlib.Path(__file__).resolve().parents[2]
TOOLS = ROOT/'tools'

def where(*args):
    t = time.time()
    r = subprocess.run([sys.executable, str(TOOLS/'where.py'), *args], cwd=ROOT, capture_output=True, encoding='utf-8', errors='replace')
    return r.stdout, time.time()-t

class Where(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not (ROOT/'work'/'where'/'overlay.json').exists():
            subprocess.run([sys.executable, str(TOOLS/'where_extract.py')], cwd=ROOT, check=True, capture_output=True)
        where('warmup')   # builds the AST index cache if it is missing

    def test_rules_key_returns_spec_line_and_java_reader(self):
        out, secs = where('squadSize')
        self.assertRegex(out, r'Spec\.java:\d+')
        self.assertRegex(out, r'sheets/design\.json:\d+')
        self.assertRegex(out, r'src/main/java/\S+\.java:\d+\s+reads rules\.squadSize')
        self.assertLess(secs, 3)

    def test_troop_id_returns_design_row_and_animation_file(self):
        out, secs = where('tc_raider')
        self.assertRegex(out, r'sheets/design\.json:\d+\s+design\.json troops row tc_raider')
        self.assertRegex(out, r'\S+tc_raider\.animation\.json')
        self.assertLess(secs, 3)

    def test_second_troop_and_max_cap(self):
        out, _ = where('ic_hearthguard', '--max', '5')
        lines = [l for l in out.splitlines() if re.match(r'\s+\S+:\d+\s', l) or re.match(r'\s+\S+/\s', l)]
        self.assertTrue(0 < len(lines) <= 5, lines)
        self.assertIn('design.json', out)

    def test_unknown_word_falls_back_to_rg(self):
        out, _ = where('Slowness')
        self.assertIn('rg fallback', out)
        self.assertRegex(out, r'sheets/design\.json:\d+')

    def test_nothing_anywhere_says_so(self):
        out, _ = where('qzx' + 'wvy' + 'plugh')
        self.assertIn('no match', out)

if __name__ == '__main__':
    unittest.main()
