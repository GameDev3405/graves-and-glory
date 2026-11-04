"""Anim review 2026-11-04 section 0: every troop resolves an attack. For each troop in sheets/design.json, every attackVariants
row of its family must have a committed clip, clips/<rig>/<family>/<id>.json, on the troop model's rig (TroopMotion.build
indexes a family's attacks from that folder; a row without one leaves attackIndex -1 and MotionEngine starts no swing).
A forearm or shin key must also keep the joint bending the right way (forearm rx never above 0, shin rx never below 0) on
the pose the game shows: an action clip plays as an offset on the stance (MotionStack.action), so an action clip of a family
with stance clips is checked as stance + action against the stance it plays on (STANCE_OF), and a stance clip on its own."""
import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/gravesandglory'
import sys
sys.path.insert(0, str(ROOT / 'tools'))
import clips as C  # noqa: E402
STANCES = {'idle', 'carry', 'guard', 'brace', 'wall', 'shield', 'aim', 'rest'}
# the stance an action clip of these families plays on in a fight: a wall attack on the wall, a bow attack on the aim
STANCE_OF = {'sword_shield': lambda clip: 'wall' if clip.endswith('_wall') else 'guard', 'bow': lambda clip: 'aim'}
TOL = 0.05


class AttackClips(unittest.TestCase):
    def setUp(self):
        self.sheet = json.loads((ROOT / 'sheets/design.json').read_text(encoding='utf-8'))

    def test_every_troop_resolves_every_attack(self):
        rows = {}
        for r in self.sheet['attackVariants']:
            rows.setdefault(r['family'], []).append(r['id'])
        missing = []
        for t in self.sheet['troops']:
            model = ASSETS / 'models/troop' / f"{t['id']}.json"
            if not model.is_file():
                continue
            rig = json.loads(model.read_text(encoding='utf-8'))['rig']
            fam = t['family']
            self.assertIn(fam, rows, f"{t['id']}: family {fam} has no attackVariants row")
            for v in rows[fam]:
                if not (ASSETS / 'clips' / rig / fam / f'{v}.json').is_file():
                    missing.append(f"{t['id']} ({rig}/{fam}/{v})")
        self.assertEqual(missing, [], 'troops with an attack variant that has no clip: ' + ', '.join(missing))

    def test_elbows_and_knees_bend_forward(self):
        bad = []
        for path in sorted((ASSETS / 'clips').glob('*/*/*.json')):
            fam, name = path.parent.name, path.stem
            clip = json.loads(path.read_text(encoding='utf-8'))
            stance = None
            if fam in STANCE_OF and name not in STANCES:
                stance = C.parse(json.loads((path.parent / f'{STANCE_OF[fam](name)}.json').read_text(encoding='utf-8')))
            for bone, ch in clip['bones'].items():
                if not bone.endswith(('forearm', 'shin')):
                    continue
                for k in ch.get('rotation', []):
                    rx = k['v'][0] + (C.sample(stance, k['t'], wrap=True).get(bone, [0])[0] if stance else 0)
                    if bone.endswith('forearm') and rx > TOL or bone.endswith('shin') and rx < -TOL:
                        bad.append(f"{fam}/{name} {bone} t{k['t']} rx {round(rx, 2)}")
        self.assertEqual(bad, [], 'backward elbow or knee keys: ' + '; '.join(bad))

if __name__ == '__main__':
    unittest.main()
