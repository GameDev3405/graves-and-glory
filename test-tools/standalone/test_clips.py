"""Checks tools/clips.py (animation rework Task 11): Blockbench animations in art/clips/<rig>/<family>.bbmodel become clip JSON
under assets/gravesandglory/clips, with the bone, impact, death and loop checks. Pure: no Minecraft, no Blockbench."""
import contextlib
import io
import json
import pathlib
import shutil
import sys
import tempfile
import unittest
import uuid

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / 'tools'))
import clips

FIXTURES = HERE / 'fixtures'
RIG = json.loads((FIXTURES / 'clip_rig.json').read_text(encoding='utf-8'))
BLOCKBENCH = json.loads((FIXTURES / 'clip_bezier.json').read_text(encoding='utf-8'))['clips']   # [{clip, samples}] from Blockbench's own code
VARIANTS = [{'id': 'overhead', 'family': 'sword_shield', 'zone': 'head', 'kind': 'slash', 'impactTicks': 10, 'weight': 'medium'},
            {'id': 'thrust', 'family': 'sword_shield', 'zone': 'body', 'kind': 'stab', 'impactTicks': 7, 'weight': 'light'},
            {'id': 'loose', 'family': 'bow', 'zone': 'body', 'kind': 'arrow', 'impactTicks': 4, 'weight': 'light'}]


def gid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'group/' + name))


def key(channel, time, *points, interpolation='linear', **handles):
    """A saved Blockbench keyframe; points are [x, y, z] data points (two of them split the key into pre and post)."""
    k = {'channel': channel, 'data_points': [{'x': x, 'y': y, 'z': z} for x, y, z in points],
         'uuid': str(uuid.uuid4()), 'time': time, 'color': -1, 'interpolation': interpolation}
    if interpolation == 'bezier':
        k.update({'bezier_linked': False, 'bezier_left_time': [-0.1, -0.1, -0.1], 'bezier_left_value': [0, 0, 0],
                  'bezier_right_time': [0.1, 0.1, 0.1], 'bezier_right_value': [0, 0, 0]})
        k.update(handles)
    return k


def animation(name, length, bones, loop='once', markers=()):
    """bones: {bone: [keyframes]}; markers: [(seconds, script)] on the Effects animator's timeline channel."""
    animators = {gid(b): {'name': b, 'type': 'bone', 'keyframes': keys} for b, keys in bones.items()}
    if markers:
        animators['effects'] = {'name': 'Effects', 'type': 'effect', 'keyframes': [
            {'channel': 'timeline', 'data_points': [{'script': s}], 'uuid': str(uuid.uuid4()), 'time': t, 'color': -1, 'interpolation': 'linear'}
            for t, s in markers]}
    return {'uuid': str(uuid.uuid4()), 'name': name, 'loop': loop, 'override': False, 'length': length, 'snapping': 20,
            'selected': False, 'anim_time_update': '', 'blend_weight': '', 'start_delay': '', 'loop_delay': '', 'animators': animators}


def project(animations, extra_groups=()):
    groups = [{'name': 'model', 'origin': [0, 0, 0], 'rotation': [180, 0, 0], 'uuid': gid('model')}]
    groups += [{'name': b['name'], 'origin': [0, 0, 0], 'uuid': gid(b['name'])} for b in RIG['bones']]
    groups += [{'name': n, 'origin': [0, 0, 0], 'uuid': gid(n)} for n in extra_groups]
    return {'meta': {'format_version': '5.0', 'model_format': 'free', 'box_uv': True}, 'name': 'clips',
            'resolution': {'width': 64, 'height': 64}, 'elements': [], 'groups': groups, 'outliner': [], 'textures': [],
            'animations': list(animations)}


class ClipsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.tmp.name)
        (self.root / 'art/rigs').mkdir(parents=True)
        shutil.copy(FIXTURES / 'clip_rig.json', self.root / 'art/rigs/biped.json')
        self.sheet({'attackVariants': VARIANTS})

    def tearDown(self):
        self.tmp.cleanup()

    def sheet(self, data):
        (self.root / 'sheets').mkdir(exist_ok=True)
        (self.root / 'sheets/design.json').write_text(json.dumps(data), encoding='utf-8')

    def put(self, family, *animations, extra_groups=()):
        path = self.root / 'art/clips/biped' / (family + '.bbmodel')
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(project(animations, extra_groups)), encoding='utf-8')

    def run_clips(self, *args):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            code = clips.main(list(args), root=self.root)
        return code, out.getvalue()

    def out(self, family, clip):
        return json.loads((self.root / clips.OUT / 'biped' / family / (clip + '.json')).read_text(encoding='utf-8'))

    @staticmethod
    def saved(clip):
        """A fixture clip as the Blockbench animation that saves it."""
        def handles(k):
            return {'bezier_left_time': k['lt'], 'bezier_left_value': k['lv'], 'bezier_right_time': k['rt'], 'bezier_right_value': k['rv']} if k['i'] == 'bezier' else {}
        bones = {b: [key(ch, k['t'], k['v'], *([k['post']] if 'post' in k else []), interpolation=k['i'], **handles(k)) for ch, keys in chans.items() for k in keys]
                 for b, chans in clip['bones'].items()}
        return animation(clip['clip'], clip['length'], bones, loop='loop' if clip['loop'] else 'once')

    def test_clips_round_trip_and_sample_as_blockbench_does(self):
        self.put('bow', *(self.saved(c['clip']) for c in BLOCKBENCH))
        code, text = self.run_clips()
        self.assertEqual(code, 0, text)
        for c in BLOCKBENCH:
            clip = self.out('bow', c['clip']['clip'])
            self.assertEqual(clip, c['clip'])
            self.assertTrue(c['samples'])
            for s in c['samples']:
                got = clips.sample(clips.parse(clip), s['t'])[s['bone']]
                got = got[:3] if s['channel'] == 'rotation' else got[3:]
                for a, b in zip(got, s['value']):
                    self.assertAlmostEqual(a, b, delta=1e-6, msg=f"{clip['clip']} {s['bone']} {s['channel']} at {s['t']}")
        self.assertEqual(self.run_clips('--verify')[0], 0)
        clip = self.out('bow', 'reference')
        clip['bones']['right_arm']['rotation'][0]['rt'][2] = 0.2   # a stale handle in the shipped JSON fails --verify
        (self.root / clips.OUT / 'biped/bow/reference.json').write_text(json.dumps(clip), encoding='utf-8')
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 1)
        self.assertIn('reference', text)

    def test_unknown_bone_fails(self):
        self.put('bow', animation('swing', 0.5, {'tail': [key('rotation', 0, [0, 0, 0]), key('rotation', 0.5, [10, 0, 0])]}),
                 extra_groups=['tail'])
        code, text = self.run_clips()
        self.assertEqual(code, 1)
        self.assertIn("'tail'", text)
        self.assertIn('biped', text)
        self.assertFalse((self.root / clips.OUT).exists())

    def test_melee_impact_must_match_the_sheet(self):
        swing = [key('rotation', 0, [0, 0, 0]), key('rotation', 0.5, [-120, 0, 0]), key('rotation', 0.8, [20, 0, 0])]
        thrust = self.thrust()
        self.put('sword_shield', animation('overhead', 0.8, {'right_arm': swing}, markers=[(0.5, 'impact')]), thrust)
        code, text = self.run_clips()
        self.assertEqual(code, 0, text)
        self.assertEqual(self.out('sword_shield', 'overhead')['markers'], {'impact': 0.5})
        self.assertEqual(clips.parse(self.out('sword_shield', 'overhead'))['impact'], 10)

        self.put('sword_shield', animation('overhead', 0.8, {'right_arm': swing}, markers=[(0.45, 'impact')]), thrust)
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 1)
        self.assertIn('impact 9', text)
        self.assertIn('impactTicks 10', text)

        self.put('sword_shield', animation('overhead', 0.8, {'right_arm': swing}), thrust)
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 1)
        self.assertIn('no impact', text)
        self.assertIn('impactTicks 10', text)

        self.put('sword_shield', animation('overhead', 0.8, {'right_arm': swing}, markers=[(0.43, 'impact')]), thrust)
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 1)
        self.assertIn('not on a tick', text)

        self.sheet({'troops': []})   # the real sheet before Task 1 lands: skip with a note, never fail
        self.put('sword_shield', animation('overhead', 0.8, {'right_arm': swing}, markers=[(0.45, 'impact')]), thrust)
        self.assertEqual(self.run_clips()[0], 0)
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 0, text)
        self.assertIn('impact check skipped', text)
        self.assertEqual(len([l for l in text.splitlines() if 'skipped' in l]), 1)

    def test_attack_clip_is_no_longer_than_the_server_sends_a_swing(self):
        cap = clips.attack_cap()
        self.assertEqual(cap, 40)
        long_swing = [key('rotation', 0, [0, 0, 0]), key('rotation', 0.5, [-120, 0, 0]), key('rotation', 2.05, [0, 0, 0])]
        self.put('sword_shield', animation('overhead', 2.05, {'right_arm': long_swing}, markers=[(0.5, 'impact')]), self.thrust())
        code, text = self.run_clips()
        self.assertEqual(code, 1)
        self.assertIn('41 ticks', text)
        self.assertIn('ATTACK_CLIP_MAX_TICKS 40', text)
        fits = [key('rotation', 0, [0, 0, 0]), key('rotation', 0.5, [-120, 0, 0]), key('rotation', 2.0, [0, 0, 0])]
        self.put('sword_shield', animation('overhead', 2.0, {'right_arm': fits}, markers=[(0.5, 'impact')]), self.thrust())
        code, text = self.run_clips()
        self.assertEqual(code, 0, text)

    @staticmethod
    def thrust(name='thrust'):
        return animation(name, 0.6, {'right_arm': [key('rotation', 0, [0, 0, 0]), key('rotation', 0.35, [-90, 0, 0]), key('rotation', 0.6, [0, 0, 0])]},
                         markers=[(0.35, 'impact')])

    def test_every_melee_row_of_a_family_needs_its_clip(self):
        overhead = animation('overhead', 0.8, {'right_arm': [key('rotation', 0, [0, 0, 0]), key('rotation', 0.8, [-120, 0, 0])]}, markers=[(0.5, 'impact')])
        self.put('sword_shield', overhead)
        code, text = self.run_clips()
        self.assertEqual(code, 1)
        self.assertIn('sword_shield', text)
        self.assertIn("'thrust'", text)
        self.assertFalse((self.root / clips.OUT).exists())
        self.put('sword_shield', overhead, self.thrust('thrusst'))   # misspelt: the row still has no clip
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 1)
        self.assertIn("'thrust'", text)
        self.put('sword_shield', overhead, self.thrust())
        code, text = self.run_clips()
        self.assertEqual(code, 0, text)
        self.sheet({'troops': []})   # before the sheet has impactTicks the check is skipped with the same note
        self.put('sword_shield', overhead)
        self.assertEqual(self.run_clips()[0], 0)
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 0, text)
        self.assertIn('impact check skipped', text)

    def test_times_stay_inside_the_clip(self):
        def swing(*times, markers=()):
            return animation('swing', 0.8, {'right_arm': [key('rotation', t, [n * 10, 0, 0]) for n, t in enumerate(times)]}, markers=markers)
        for bad, value in (((0, 0.4, 0.9), '0.9'), ((-0.1, 0.4, 0.8), '-0.1')):
            self.put('bow', swing(*bad))
            code, text = self.run_clips()
            self.assertEqual(code, 1, bad)
            for part in ('swing', 'right_arm', value, '0.8'):
                self.assertIn(part, text)
        for at in (1.0, -0.05):
            self.put('bow', swing(0, 0.8, markers=[(at, 'release')]))
            code, text = self.run_clips()
            self.assertEqual(code, 1, at)
            for part in ('swing', 'release', str(at), '0.8'):
                self.assertIn(part, text)
        self.put('bow', swing(0, 0.4, 0.4, 0.8))
        code, text = self.run_clips()
        self.assertEqual(code, 1)
        for part in ('swing', 'right_arm', 'rotation', 'two keys at 0.4'):
            self.assertIn(part, text)
        both = swing(0, 0.4, 0.8)   # a rotation and a position key may share a time
        both['animators'][gid('right_arm')]['keyframes'].append(key('position', 0.4, [0, 1, 0]))
        self.put('bow', both)
        code, text = self.run_clips()
        self.assertEqual(code, 0, text)

    def lying(self, sink, loop='hold'):
        """Flat on the back: hips sunk `sink` px, torso and legs pitched level. At sink 8 the back of the head touches the ground."""
        return animation('death_back', 1, {
            'root': [key('position', 0, [0, 0, 0]), key('position', 1, [0, sink, 0])],
            'body': [key('rotation', 0, [0, 0, 0]), key('rotation', 1, [-90, 0, 0])],
            'right_leg': [key('rotation', 0, [0, 0, 0]), key('rotation', 1, [-90, 0, 0])],
            'left_leg': [key('rotation', 0, [0, 0, 0]), key('rotation', 1, [-90, 0, 0])]}, loop=loop)

    def test_death_clip_rests_on_the_ground(self):
        self.put('common', self.lying(8))
        code, text = self.run_clips()
        self.assertEqual(code, 0, text)
        self.put('common', self.lying(8.08))   # within the 0.1 px slack
        code, text = self.run_clips()
        self.assertEqual(code, 0, text)
        self.put('common', self.lying(8.5))
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 1)
        self.assertIn('death_back', text)
        self.assertIn('head', text)
        self.assertIn('0.5', text)
        bent = self.lying(8)   # a knee bent through the floor: the shin's own pivot and box count
        bent['animators'][gid('right_shin')] = {'name': 'right_shin', 'type': 'bone', 'keyframes': [key('rotation', 1, [60, 0, 0])]}
        self.put('common', bent)
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 1)
        self.assertIn('right_shin', text)
        self.put('common', self.lying(8, loop='loop'))   # a death clip plays once and holds its last frame
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 1)
        self.assertIn('death_back', text)
        self.assertIn('cannot loop', text)

    def test_looping_clip_ends_where_it_starts(self):
        def idle(end):
            sway = [key('rotation', 0, [0, 0, 2], interpolation='catmullrom'), key('rotation', 0.6, [3, 0, -2], interpolation='catmullrom'),
                    key('rotation', 1.2, [end, 0, 2], interpolation='catmullrom')]
            return animation('idle', 1.2, {'body': sway}, loop='loop')
        self.put('bow', idle(0))
        code, text = self.run_clips()
        self.assertEqual(code, 0, text)
        self.assertIs(self.out('bow', 'idle')['loop'], True)
        self.put('bow', idle(0.005))
        self.assertEqual(self.run_clips('--verify')[0], 1)   # stale output, but the loop itself is within 0.01
        self.assertEqual(self.run_clips()[0], 0)
        self.put('bow', idle(0.5))
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 1)
        self.assertIn('idle', text)
        self.assertIn('loop', text)

    def test_verify_passes_with_no_clips_and_removes_strays(self):
        self.assertEqual(self.run_clips('--verify')[0], 0)
        stray = self.root / clips.OUT / 'biped/sword_shield/old.json'
        stray.parent.mkdir(parents=True)
        stray.write_text('{}', encoding='utf-8')
        code, text = self.run_clips('--verify')
        self.assertEqual(code, 1)
        self.assertIn('old.json', text)
        self.assertEqual(self.run_clips()[0], 0)
        self.assertFalse(stray.exists())


if __name__ == '__main__':
    unittest.main()
