"""The z-fighting check (art/hand/common/zfight.py, tools/zfight_check.py) on small hand-made models."""
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'art/hand/common'))
sys.path.insert(0, str(ROOT / 'tools'))
import zfight as Z  # noqa: E402
import zfight_check as ZC  # noqa: E402


def bone(name, parent='', pivot=(0, 0, 0), boxes=(), rot=(0, 0, 0), stages=(), tag='', limit=None):
    b = {'name': name, 'parent': parent, 'pivot': list(pivot), 'rotation': list(rot), 'stages': list(stages), 'tag': tag,
         'boxes': [{'origin': list(o), 'size': list(s), 'dilation': [d] * 3} for o, s, d in
                   ((x + (0.0,)) if len(x) == 2 else x for x in boxes)]}
    if limit:
        b['limit'] = list(limit)
    return b


def old(*bones):
    return Z.Model.old({'troop': 't', 'skins': {'1': '', '2': ''}, 'bones': [bone('root')] + list(bones)})


def pairs(model, **kw):
    return {(h.a, h.fa, h.b, h.fb) for h in Z.find(model, **kw)}


class Coplanar(unittest.TestCase):
    def test_same_way_overlap_fails(self):
        m = old(bone('a', 'root', boxes=[((0, 0, 0), (4, 4, 4))]), bone('b', 'root', boxes=[((2, 0, 0), (4, 4, 4))]))
        p = pairs(m)
        self.assertIn(('a#0', '-y', 'b#0', '-y'), p)   # the shared top
        self.assertIn(('a#0', '-z', 'b#0', '-z'), p)
        self.assertNotIn(('a#0', '+x', 'b#0', '-x'), p)

    def test_back_to_back_passes(self):
        m = old(bone('a', 'root', boxes=[((0, 0, 0), (4, 4, 4))]), bone('b', 'root', boxes=[((4, 1, 1), (2, 2, 2))]))
        self.assertEqual(pairs(m), set())

    def test_plane_tolerance(self):
        near = old(bone('a', 'root', boxes=[((0, 0, 0), (4, 4, 4))]), bone('b', 'root', boxes=[((2, 0.004, 0), (4, 4, 4))]))
        far = old(bone('a', 'root', boxes=[((0, 0, 0), (4, 4, 4))]), bone('b', 'root', boxes=[((2, 0.01, 0), (4, 4, 4))]))
        self.assertIn(('a#0', '-y', 'b#0', '-y'), pairs(near))
        self.assertNotIn(('a#0', '-y', 'b#0', '-y'), pairs(far))

    def test_inflate_parts_planes(self):
        m = old(bone('a', 'root', boxes=[((0, 0, 0), (4, 4, 4))]), bone('b', 'root', boxes=[((2, 0, 0), (4, 4, 4), 0.03)]))
        self.assertEqual(pairs(m), set())

    def test_small_overlap_passes(self):
        # 0.2 x 1 = 0.2 px2 of shared front: under the 0.25 px2 floor
        m = old(bone('a', 'root', boxes=[((0, 0, 0), (4, 1, 4))]), bone('b', 'root', boxes=[((3.8, 0, 0), (4, 1, 4))]))
        self.assertNotIn(('a#0', '-z', 'b#0', '-z'), pairs(m))

    def test_hidden_inside_a_third_cube(self):
        m = old(bone('a', 'root', boxes=[((0, 0, 0), (2, 2, 2)), ((2, 0, 0), (2, 2, 2)), ((-1, -1, -3), (6, 4, 4))]))
        # a#0 and a#1 share planes but the big third box swallows their -z faces; their +z faces sit inside too (z 2 < 1+?)
        self.assertNotIn(('a#0', '-z', 'a#1', '-z'), pairs(m))

    def test_flat_panels_both_ways(self):
        m = old(bone('a', 'root', boxes=[((0, 0, 0), (4, 4, 0)), ((2, 0, 0), (4, 4, 0))]))
        p = pairs(m)
        self.assertIn(('a#0', '-z', 'a#1', '-z'), p)
        self.assertIn(('a#0', '+z', 'a#1', '+z'), p)

    def test_rotated_bone(self):
        m = old(bone('a', 'root', rot=(20, 30, 10), boxes=[((0, 0, 0), (4, 4, 4)), ((2, 0, 0), (4, 4, 4))]))
        self.assertIn(('a#0', '-y', 'a#1', '-y'), pairs(m))

    def test_stages_never_together(self):
        m = old(bone('a', 'root', stages=[1], boxes=[((0, 0, 0), (4, 4, 4))]),
                bone('b', 'root', stages=[2], boxes=[((2, 0, 0), (4, 4, 4))]))
        self.assertEqual(pairs(m), set())

    def test_weapon_bones_left_out(self):
        m = old(bone('a', 'root', boxes=[((0, 0, 0), (4, 4, 4))]), bone('w', 'root', tag='weapon', boxes=[((2, 0, 0), (4, 4, 4))]))
        self.assertEqual(pairs(m), set())


class Joints(unittest.TestCase):
    """A jointed limb as handlib.limb builds it: a 4x6x4 upper box and a 4x8x4 lower box reaching 2 px back into it."""

    def leg(self, inset=0.0):
        return old(bone('right_leg', 'root', boxes=[((-2, 0, -2), (4, 6, 4))]),
                   bone('right_shin', 'right_leg', pivot=(0, 6, 0), limit=(0, 150), boxes=[((-2, -2, -2), (4, 8, 4), -inset)]))

    def test_bent_joint_fails_on_its_sides(self):
        bent = [p for p in Z.poses(self.leg()) if p[0] != 'rest']
        self.assertTrue(bent)
        p = pairs(self.leg(), pose_list=bent)
        self.assertIn(('right_leg#0', '-x', 'right_shin#0', '-x'), p)
        self.assertIn(('right_leg#0', '+x', 'right_shin#0', '+x'), p)

    def test_rest_fails_front_and_back(self):
        p = pairs(self.leg(), pose_list=[('rest', {})])
        self.assertIn(('right_leg#0', '-z', 'right_shin#0', '-z'), p)

    def test_inset_passes_every_pose(self):
        self.assertEqual(pairs(self.leg(0.04)), set())

    def test_bend_follows_the_limit(self):
        m = self.leg()
        self.assertEqual(m.bones['right_shin']['bend'], 1)
        f = Z.Model.old({'bones': [bone('root'), bone('right_forearm', 'root', limit=(-150, 0))]})
        self.assertEqual(f.bones['right_forearm']['bend'], -1)


class Geo(unittest.TestCase):
    def test_geo_model(self):
        g = {'minecraft:geometry': [{'bones': [
            {'name': 'root', 'pivot': [0, 0, 0]},
            {'name': 'hips', 'parent': 'root', 'pivot': [0, 12, 0], 'cubes': [{'origin': [-4, 12, -2], 'size': [8, 4, 4]}]},
            {'name': 'knee', 'parent': 'hips', 'pivot': [0, 12, 0], 'cubes': [{'origin': [-4, 10, -2], 'size': [3, 3, 1]}]},
            {'name': 'sword', 'parent': 'hips', 'pivot': [0, 12, 0], 'cubes': [{'origin': [-4, 12, -2], 'size': [1, 1, 1]}]}]}]}
        rig = {'tiers': [1], 'bones': {'sword': {'tag': 'weapon'}}}
        p = pairs(Z.Model.geo(g, rig, 'x'))
        self.assertIn(('hips#0', '-z', 'knee#0', '-z'), p)
        self.assertFalse(any('sword' in a or 'sword' in b for a, _, b, _ in p))


class Exemptions(unittest.TestCase):
    def test_exempt_and_stale(self):
        m = old(bone('a', 'root', boxes=[((0, 0, 0), (4, 4, 4))]), bone('b', 'root', boxes=[((2, 0, 0), (4, 4, 4))]))
        rows = [{'kind': 'old', 'troop': 't', 'a': 'b#0', 'b': 'a#0', 'reason': 'test'},
                {'kind': 'old', 'troop': 't', 'a': 'x#0', 'b': 'y#0', 'reason': 'gone'}]
        fails, counts, stale = ZC.check([('old', 't', m)], rows)
        self.assertEqual(fails, [])
        self.assertEqual([r['a'] for r in stale], ['x#0'])

    def test_shipped_exemptions_are_well_formed(self):
        for r in ZC.load_exempt():
            self.assertIn(r['kind'], ('old', 'geo'))
            self.assertGreater(len(r['reason']), 20, r)


class Settle(unittest.TestCase):
    class Box:
        def __init__(s, o, sz, dil=0.0):
            s.origin, s.size, s.dil = list(o), list(sz), dil

    class Bone:
        def __init__(s, name, parent, boxes):
            s.name, s.parent, s.boxes = name, parent, boxes

    def emit(self, B):
        return lambda: old(*[bone(b.name, b.parent, boxes=[(x.origin, x.size, x.dil) for x in b.boxes]) for b in B])

    def test_parts_a_shared_plane(self):
        B = [self.Bone('a', 'root', [self.Box((0, 0, 0), (4, 4, 4))]),
             self.Bone('b', 'root', [self.Box((2, 0, 0), (2, 2, 2)), self.Box((0, 0, 0), (2, 1, 2))])]
        left = Z.settle(B, self.emit(B))
        self.assertEqual(left, [])
        self.assertEqual(Z.find(self.emit(B)(), eps=Z.SOLVE_EPS), [])
        dils = [x.dil for b in B for x in b.boxes]
        self.assertTrue(all(abs(d) < 0.05 for d in dils), dils)
        self.assertEqual(dils[0], 0.0)   # the big box keeps its place; the small ones move

    def test_protect(self):
        B = [self.Bone('a', 'root', [self.Box((0, 0, 0), (1, 1, 1))]), self.Bone('b', 'root', [self.Box((0, 0, 0), (4, 4, 4))])]
        Z.settle(B, self.emit(B), protect={'a#0'})
        self.assertEqual(B[0].boxes[0].dil, 0.0)
        self.assertNotEqual(B[1].boxes[0].dil, 0.0)


if __name__ == '__main__':
    unittest.main()
