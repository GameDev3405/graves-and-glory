"""Anim review 2026-11-04 section 0: every troop resolves an attack. For each troop in sheets/design.json, every attackVariants
row of its family must have a committed clip, clips/<rig>/<family>/<id>.json, on the troop model's rig (TroopMotion.build
indexes a family's attacks from that folder; a row without one leaves attackIndex -1 and MotionEngine starts no swing).
A forearm or shin key must also keep the joint bending the right way on the new families: forearm rx never above 0."""
import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/gravesandglory'
# families whose clips were keyed as offsets on a stance clip (sword_shield, bow): their forearm keys add onto a bent stance
STANCE_OFFSET = {'sword_shield', 'bow', 'caster', 'common'}


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

    def test_new_family_elbows_and_knees_bend_forward(self):
        bad = []
        for path in sorted((ASSETS / 'clips').glob('*/*/*.json')):
            if path.parent.name in STANCE_OFFSET:
                continue
            clip = json.loads(path.read_text(encoding='utf-8'))
            for bone, ch in clip['bones'].items():
                for k in ch.get('rotation', []):
                    if bone.endswith('forearm') and k['v'][0] > 1e-6:
                        bad.append(f"{path.parent.name}/{path.stem} {bone} t{k['t']} rx {k['v'][0]}")
                    if bone.endswith('shin') and k['v'][0] < -1e-6:
                        bad.append(f"{path.parent.name}/{path.stem} {bone} t{k['t']} rx {k['v'][0]}")
        self.assertEqual(bad, [], 'backward elbow or knee keys: ' + '; '.join(bad))


if __name__ == '__main__':
    unittest.main()
