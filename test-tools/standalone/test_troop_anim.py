"""Checks tools/troop_anim (the /animate-troop workflow, docs/ANIMATING-TROOPS.md): sampling parity with Clip.java on the
committed clips, the arm and leg solvers, the bbmodel writer's round trip, and the quality gates. Pure: no Minecraft, no
Blockbench. The Java parity test needs a JDK 17 and Gson 2.10.1 (the verify.py setup) and is skipped without them."""
import copy
import glob
import json
import math
import os
import pathlib
import shutil
import subprocess
import sys
import tempfile
import unittest

import numpy as np

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
import clips as CL  # noqa: E402
from troop_anim import author as A  # noqa: E402
from troop_anim import checks as K  # noqa: E402
from troop_anim import model as M  # noqa: E402
from troop_anim import pose as P  # noqa: E402
from troop_anim import recipes as R  # noqa: E402
from troop_anim import solve as S  # noqa: E402

CLIPS = ROOT / CL.OUT
OVERHEAD = 'sword_shield_overhead'


def _jdk():
    homes = [os.environ.get('JAVA_HOME')] + sorted(glob.glob('C:/Program Files/Eclipse Adoptium/jdk-17*'), reverse=True)
    for h in homes:
        if h and (pathlib.Path(h) / 'bin' / ('javac.exe' if os.name == 'nt' else 'javac')).is_file():
            return pathlib.Path(h) / 'bin'
    javac = shutil.which('javac')
    return pathlib.Path(javac).parent if javac else None


def _gson():
    jars = sorted((pathlib.Path.home() / '.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.10.1').glob('*/*.jar'))
    return jars[0] if jars else None


HARNESS = r'''package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/** Samples clips with Clip.sample: each input line is "path<TAB>t1,t2,..."; each output line "path t bone v0..v5". */
public final class ClipParityDump {
    public static void main(String[] a) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String line : Files.readAllLines(Path.of(a[0]))) {
            String[] parts = line.split("\t");
            Clip clip = Clip.parse(Files.readString(Path.of(parts[0])));
            for (String ts : parts[1].split(",")) {
                Map<String, double[]> out = new TreeMap<>();
                clip.sample(Double.parseDouble(ts), out);
                for (Map.Entry<String, double[]> e : out.entrySet()) {
                    sb.append(parts[0]).append('\t').append(ts).append('\t').append(e.getKey());
                    for (double v : e.getValue()) sb.append('\t').append(v);
                    sb.append('\n');
                }
            }
        }
        Files.writeString(Path.of(a[1]), sb.toString());
    }
}
'''


def _times(length):
    ts = [round(i * 0.025, 4) for i in range(int(round(length / 0.025)) + 1)]
    return ts + [0.0123, round(length / 3, 6), round(length * 0.7071, 6), -0.05, round(length + 0.1, 4)]


@unittest.skipUnless(_jdk() and _gson(), 'no JDK 17 or Gson 2.10.1 (see docs/SETUP.md)')
class ClipJavaParity(unittest.TestCase):
    """pose.channels samples with tools/clips.py; this pins that sampler to Clip.sample on every committed clip."""

    def test_every_committed_clip_samples_like_clip_java(self):
        files = sorted(CLIPS.rglob('*.json'))
        self.assertGreater(len(files), 50)
        with tempfile.TemporaryDirectory() as tmp:
            tmp = pathlib.Path(tmp)
            src = tmp / 'src' / 'gg' / 'gravesandglory'
            src.mkdir(parents=True)
            (src / 'ClipParityDump.java').write_text(HARNESS, encoding='utf-8')
            jdk, gson = _jdk(), _gson()
            classes = tmp / 'classes'
            r = subprocess.run([str(jdk / 'javac'), '--release', '17', '-nowarn', '-cp', str(gson), '-sourcepath',
                                str(ROOT / 'src/main/java'), '-d', str(classes), str(src / 'ClipParityDump.java')],
                               capture_output=True, text=True)
            self.assertEqual(r.returncode, 0, r.stderr[-2000:])
            want = {}
            lines = []
            for f in files:
                clip = CL.parse(json.loads(f.read_text(encoding='utf-8')))
                ts = _times(clip['length'])
                lines.append(f'{f}\t' + ','.join(repr(t) for t in ts))
                for t in ts:
                    want[(str(f), repr(t))] = CL.sample(clip, t)
            (tmp / 'in.txt').write_text('\n'.join(lines) + '\n', encoding='utf-8')
            r = subprocess.run([str(jdk / 'java'), '-cp', str(classes) + os.pathsep + str(gson), 'gg.gravesandglory.ClipParityDump',
                                str(tmp / 'in.txt'), str(tmp / 'out.txt')], capture_output=True, text=True)
            self.assertEqual(r.returncode, 0, r.stderr[-2000:])
            got = {}
            for line in (tmp / 'out.txt').read_text(encoding='utf-8').splitlines():
                path, t, bone, *v = line.split('\t')
                got.setdefault((path, t), {})[bone] = [float(x) for x in v]
        worst = (0.0, None)
        for k, py in want.items():
            java = got.get(k, {})
            for bone in set(py) | set(java):
                d = float(np.max(np.abs(np.array(py.get(bone, [0.0] * 6)) - np.array(java.get(bone, [0.0] * 6)))))
                if d > worst[0]:
                    worst = (d, (pathlib.Path(k[0]).name, k[1], bone))
        self.assertLess(worst[0], 1e-6, f'Clip.java and clips.sample differ by {worst[0]} at {worst[1]}')


class Aims(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.troop = M.Troop('knight')

    def arm(self):
        tr = self.troop
        arm = S.Arm(tr, tr.hand)
        arm.seed({b: tr.rest[b][0] for b in (arm.arm, arm.fore, arm.hand)})
        return arm

    def test_sph_round_trips_over_the_top(self):
        for out in (1.0, -1.0):
            for el, az in ((0, 0), (30, 20), (-60, -45), (120, 10), (160, -30), (200, 15)):
                v = A.sph(el, az, out)
                self.assertAlmostEqual(float(np.linalg.norm(v)), 1.0, places=9)
                self.assertLess(S.angle(A.sph(*A.unsph(v, out), out), v), 1e-6)

    def test_arm_lays_upper_fore_and_item_on_their_aims(self):
        out = 1.0 if self.troop.hand == 'left' else -1.0
        for el in ((-58, -5, 35), (30, 110, 160), (60, 125, 160), (-25, -12, 40), (-50, -42, -58)):
            arm = self.arm()
            u, f, it = (A.sph(e, a, out) for e, a in zip(el, (10, 0, -5)))
            net = arm.solve(u, f, it, twist=0.0)
            got = arm.aims(net)
            self.assertLess(S.angle(got['upper'], u), 0.5, el)
            self.assertLess(S.angle(got['fore'], f), 0.5, el)
            self.assertLess(S.angle(got['item'], it), 0.5, el)
            self.assertLessEqual(net[arm.fore][0], 1e-9, 'the elbow bends one way only')

    def test_twist_turns_the_edge_about_the_item(self):
        out = -1.0 if self.troop.hand == 'right' else 1.0
        arm = self.arm()
        net = arm.solve(A.sph(-30, 10, out), A.sph(-10, 0, out), A.sph(30, -10, out), twist=25.0)
        self.assertAlmostEqual(arm.aims(net)['twist'], 25.0, delta=0.5)

    def test_euler_stays_continuous_over_the_top(self):
        out = -1.0 if self.troop.hand == 'right' else 1.0
        arm = self.arm()
        prev = None
        for el in range(-60, 181, 5):
            net = arm.solve(A.sph(el, 15, out), A.sph(min(el + 40, 200), 5, out), A.sph(min(el + 70, 220), 0, out), twist=0.0)
            if prev is not None:
                for b in (arm.arm, arm.hand):
                    self.assertLess(float(np.max(np.abs(np.array(net[b]) - np.array(prev[b])))), 40.0, (el, b, net[b], prev[b]))
            prev = net

    def test_reach_puts_the_fist_on_a_target(self):
        arm = self.arm()
        rest = {b: arm.prev[b] for b in (arm.arm, arm.fore, arm.hand)}
        target = arm.hand_point(rest) + np.array([2.0, -3.0, -4.0])
        u, f, res = arm.reach(target, np.array([-1.0, 0.4, 0.6]))
        self.assertLess(res, 1e-6)
        net = arm.solve(u, f)
        self.assertLess(float(np.linalg.norm(arm.hand_point(net) - target)), 0.05)

    def test_leg_ik_plants_the_sole(self):
        tr = self.troop
        W = tr.fk(P.local(tr, {}, None, None, False, False))
        for side in ('right', 'left'):
            leg = S.Leg(tr, side)
            leg.seed({b: tr.rest[b][0] for b in (leg.leg, leg.shin)})
            here = (W[leg.shin] @ np.array([0.0, S.SOLE, 0.0, 1.0]))[:3]
            target = here + np.array([0.5, -1.5, -2.0])
            (lp, ly, lr), knee, res = leg.solve(W['root'], target)
            self.assertLess(res, 1e-3, side)
            self.assertGreaterEqual(knee, 0.0, 'knees bend one way only')

    def test_pchip_keeps_monotone_keys_monotone(self):
        x = np.array([0.0, 0.2, 0.35, 0.5, 0.6])
        y = np.array([0.0, 50.0, 60.0, 61.0, 120.0])
        vals = [S.pchip(x, y, t) for t in np.linspace(0, 0.6, 121)]
        self.assertTrue(all(b >= a - 1e-9 for a, b in zip(vals, vals[1:])))
        for xi, yi in zip(x, y):
            self.assertAlmostEqual(S.pchip(x, y, xi), yi, places=9)


class Export(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.troop = M.Troop('knight')
        cls.path = P.source(cls.troop.rig, cls.troop.family)
        cls.raw = cls.path.read_text(encoding='utf-8')
        cls.rec = R.recipe(cls.troop, OVERHEAD)
        cls.baker, cls.clip, cls.times, cls.text, cls.parsed = A.author(cls.troop, cls.rec)

    def test_an_unchanged_project_writes_byte_identical(self):
        for p in sorted((ROOT / CL.SRC).rglob('*.bbmodel')):
            raw = p.read_text(encoding='utf-8')
            self.assertEqual(A._dump_like(raw, json.loads(raw)), raw, p.name)

    def test_write_touches_only_its_animation(self):
        old, new = json.loads(self.raw), json.loads(self.text)
        self.assertEqual({k: v for k, v in old.items() if k != 'animations'}, {k: v for k, v in new.items() if k != 'animations'})
        self.assertEqual([a['name'] for a in old['animations']], [a['name'] for a in new['animations']])
        for a, b in zip(old['animations'], new['animations']):
            if a['name'] == OVERHEAD:
                for k in ('uuid', 'length', 'loop', 'snapping'):
                    self.assertEqual(a.get(k), b.get(k), k)
                self.assertEqual({u: x for u, x in a['animators'].items() if x.get('type') == 'effect'},
                                 {u: x for u, x in b['animators'].items() if x.get('type') == 'effect'}, 'markers kept')
            else:
                self.assertEqual(json.dumps(a, sort_keys=True), json.dumps(b, sort_keys=True), a['name'])
        rig = json.loads((ROOT / CL.RIGS / f'{self.troop.rig}.json').read_text(encoding='utf-8'))
        before = CL.convert(old, rig, 'old')
        after = CL.convert(new, rig, 'new')
        self.assertEqual(sorted(before), sorted(after))
        for name in before:
            if name != OVERHEAD:
                self.assertEqual(CL.dump(before[name]), CL.dump(after[name]), name)

    def test_the_written_clip_keeps_design_timing(self):
        p = self.parsed
        self.assertEqual(p['length'], 1.2)
        self.assertEqual(p['impact'], 10)
        self.assertFalse(p['loop'])
        self.assertEqual(K.ticks(p), [])
        self.assertEqual(K.timing(self.troop, p)['want'], 10)

    def test_the_bake_starts_and_ends_on_the_stance(self):
        for b, v in self.clip.items():
            self.assertLess(float(np.max(np.abs(v[0]))), 1e-6, b)
            self.assertLess(float(np.max(np.abs(v[-1]))), 1e-6, b)

    def test_write_refuses_to_change_timing(self):
        with self.assertRaises(ValueError):
            A.write(self.troop, OVERHEAD, self.clip, self.times, impact=0.5, length=1.0, dry=True)
        with self.assertRaises(ValueError):
            A.write(self.troop, OVERHEAD, self.clip, self.times, impact=0.45, length=1.2, dry=True)
        with self.assertRaises(KeyError):
            A.write(self.troop, 'no_such_clip', self.clip, self.times, dry=True)
        self.assertEqual(self.path.read_text(encoding='utf-8'), self.raw, 'dry runs never write')


def _synthetic(bones, length=1.0, impact=-1, name=OVERHEAD):
    return {'clip': name, 'length': length, 'loop': False, 'impact': impact, 'markers': {},
            'bones': {b: {'rotation': [{'t': t, 'v': list(v)} for t, v in keys]} for b, keys in bones.items()}}


class Gates(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.troop = M.Troop('knight')

    def test_gates_are_the_guides(self):
        self.assertEqual(K.GATES, {'reach': 0.3, 'sink': 0.15, 'hover': 0.15, 'skate': 8.0, 'pop': 50.0, 'jerk': 30.0,
                                   'clip': 1.5, 'loop': 0.5, 'loop_root': 0.01, 'ends': 1.0, 'rate': 60.0, 'wrist': 40.0})

    def test_rate_flags_a_snap_and_passes_a_swing(self):
        snap = _synthetic({'right_arm': [(0.0, (0, 0, 0)), (0.05, (-80, 0, 0)), (1.0, (-80, 0, 0))]})
        swing = _synthetic({'right_arm': [(0.0, (0, 0, 0)), (0.2, (-80, 0, 0)), (1.0, (-80, 0, 0))]})
        self.assertGreater(K.rate(snap, self.troop)['right_arm'][0], K.GATES['rate'])
        self.assertAlmostEqual(K.rate(swing, self.troop)['right_arm'][0], 40.0, delta=0.5)

    def test_ticks_flags_keys_off_the_tick(self):
        clip = _synthetic({'right_arm': [(0.0, (0, 0, 0)), (0.26, (-10, 0, 0)), (0.3, (0, 0, 0))]})
        self.assertEqual(K.ticks(clip), [('right_arm', 'rotation', 0.26)])

    def test_backward_elbow_and_open_ends_fail(self):
        good = R.recipe(self.troop, OVERHEAD)
        _, _, _, _, clip = A.author(self.troop, good)
        bad = copy.deepcopy(clip)
        keys = bad['bones']['right_forearm']['rotation']
        self.assertTrue(keys)
        for k in keys:
            k['v'] = [k['v'][0] + 150.0] + list(k['v'][1:])
            k['post'] = list(k['v'])
        res = K.measure(self.troop, bad)
        self.assertIn('right_forearm', res['signs'])
        self.assertGreater(res['ends'], K.GATES['ends'])
        self.assertTrue(any(f.startswith('sign right_forearm') for f in res['fails']))

    def test_measure_reports_every_gate_on_a_committed_clip(self):
        res = K.measure(self.troop, OVERHEAD)
        for k in ('sink', 'hover', 'skate', 'pop', 'jerk', 'clipping', 'ends', 'signs', 'rate', 'wrist', 'ticks_off',
                  'timing', 'fails', 'stance_gap'):
            self.assertIn(k, res)
        self.assertEqual(res['timing']['want'], 10)
        self.assertEqual(res['timing']['impact'], 10)
        self.assertIn('PASS' if not res['fails'] else 'FAIL', K.report(res))


if __name__ == '__main__':
    unittest.main()
