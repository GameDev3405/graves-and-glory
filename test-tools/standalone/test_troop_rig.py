"""The GeckoLib troop rig (art/anim/common/troop_rig.py) keeps every part on the piece it belongs to.

rigify re-hangs a troop's handlib bones on the knight's skeleton. A bone that moves (the shield, from the forearm to the left
fist) must take its children with it: Kort 2026-11-12, "why are their shields have a wierd red overhang on them?" The shield's
cloth face had stayed at the old place, 2.7 px across and 2 px up from the shield it is painted on.

Two checks:
  - the rig: for every troop, a bone and its child that were parent and child in the troop's own art move by the same amount;
  - the shipped models: on every troop geo with a shield, each shield_face cube lies on the shield (inside its outline).
"""
import importlib
import json
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
GEO = ROOT / 'src/main/resources/assets/gravesandglory/geo/troop'
sys.path.insert(0, str(ROOT / 'art/anim/common'))
TOL = 1e-6
# rigify renames these source bones (troop_rig.rigify): a troop's own foot rides the rig's foot, a plain "cape" hangs under the container
RENAMED = {'right_foot': 'right_foot_gear', 'left_foot': 'left_foot_gear', 'cape': 'cape_cloth'}


def place(bone, pivots):
    """where a bone's art sits: its absolute pivot plus its first box's offset (a bone with no box: the pivot)"""
    p = pivots[bone.name]
    o = bone.boxes[0].origin if bone.boxes else (0, 0, 0)
    return [p[i] + o[i] for i in range(3)]


def moved(TR, rig):
    """{rigged bone name: (how far its art moved from the troop's own art, its source parent's rigged name)}"""
    src = rig.troop.bones()                      # a fresh copy: rigify changed the one it was given
    src_at, out_at = TR.absolute(src), TR.absolute(rig.B)
    out = {bn.name: bn for bn in rig.B}
    res = {}
    for s in src:
        o = out.get(RENAMED.get(s.name, s.name)) or out.get(s.name)
        if o is None:
            continue
        if bool(s.boxes) != bool(o.boxes):       # the art was rebuilt elsewhere (a robed troop's legs): compare the pivots
            a, b = src_at[s.name], out_at[o.name]
        else:
            a, b = place(s, src_at), place(o, out_at)
        res[o.name] = ([b[i] - a[i] for i in range(3)], RENAMED.get(s.parent, s.parent))
    return res, out


class ChildrenFollowTheirParent(unittest.TestCase):
    def test_every_troop(self):
        TR = importlib.import_module('troop_rig')
        bad = []
        for tid in TR.TROOPS:
            rig = TR.Rig(tid)
            shifts, out = moved(TR, rig)
            for name, (shift, src_parent) in shifts.items():
                bone = out[name]
                if bone.parent != src_parent or src_parent not in shifts:
                    continue                     # re-hung on another bone (boot to the foot, gear to a robe panel): not a pair any more
                want = shifts[src_parent][0]
                if any(abs(shift[i] - want[i]) > TOL for i in range(3)):
                    bad.append('%s: %s moved %s, its parent %s moved %s' % (
                        tid, name, [round(v, 3) for v in shift], src_parent, [round(v, 3) for v in want]))
        self.assertEqual(bad, [], '\n'.join(bad))


class ShieldFaceOnTheShield(unittest.TestCase):
    def test_shipped_geos(self):
        seen, bad = [], []
        for path in sorted(GEO.glob('*.geo.json')):
            bones = json.loads(path.read_text(encoding='utf-8'))['minecraft:geometry'][0]['bones']
            shield = next((b for b in bones if b['name'] == 'shield'), None)
            if shield is None:
                continue
            troop = path.name.split('.')[0]
            seen.append(troop)
            x0 = min(c['origin'][0] for c in shield['cubes'])
            x1 = max(c['origin'][0] + c['size'][0] for c in shield['cubes'])
            y0 = min(c['origin'][1] for c in shield['cubes'])
            y1 = max(c['origin'][1] + c['size'][1] for c in shield['cubes'])
            faces = [b for b in bones if b.get('parent') == 'shield' and b['name'].startswith('shield_face')]
            self.assertTrue(faces, troop + ': a shield with no shield_face bone')
            for f in faces:
                if any(abs(f['pivot'][i] - shield['pivot'][i]) > TOL for i in range(3)):
                    bad.append('%s: %s pivot %s, the shield %s' % (troop, f['name'], f['pivot'], shield['pivot']))
                for c in f['cubes']:
                    o, s = c['origin'], c['size']
                    if o[0] < x0 - TOL or o[0] + s[0] > x1 + TOL or o[1] < y0 - TOL or o[1] + s[1] > y1 + TOL:
                        bad.append('%s: %s spans x %s..%s y %s..%s, the shield x %s..%s y %s..%s' % (
                            troop, f['name'], o[0], o[0] + s[0], o[1], o[1] + s[1], x0, x1, y0, y1))
        self.assertIn('knight', seen)
        self.assertGreaterEqual(len(seen), 4, seen)   # knight, sentinel, gl_marshal, ic_hearthguard
        self.assertEqual(bad, [], '\n'.join(bad))


if __name__ == '__main__':
    unittest.main()
