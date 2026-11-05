"""Fixing poses at the source by search.

A clip's beats are written by hand; at Minecraft proportions a hand-written beat often puts a held item or a forearm in
the body. This searches each failing beat for the nearest pose that is clear: small random steps on the beat's own
fields (hand and bow positions, aims, elbow poles, the torso's turn), kept when they lower a score of

    items inside the body (px^3) + forearms deep in the trunk (px^3) + a penalty if a hand cannot reach
    + a cost for straying from the written beat (so each beat keeps its intent)

Each beat is judged as a still pose (fast). The results are stored as data (`<unit>_poses.json`: per clip, per beat
time, the changed fields) that the clip builder applies over the written timeline, so they are reviewable, versioned and
reproducible (fixed seeds).

  python tools/unit_anim/pose_search.py archer [clip ...]
"""
import json
import pathlib
import random
import sys

import numpy as np

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))

# fields a search may move, with step sizes (px for positions, degrees for angles, unitless for poles)
FIELDS = {'hand_at': 0.6, 'bow_at': 0.6, 'aim': 4.0, 'arrow_dir': 6.0, 'r_pole': 0.25, 'l_pole': 0.25,
          'body': 3.0, 'chest': 3.0}
DEVIATION = {'hand_at': 0.6, 'bow_at': 0.6, 'aim': 0.05, 'arrow_dir': 0.03, 'r_pole': 0.5, 'l_pole': 0.5,
             'body': 0.08, 'chest': 0.08}


def load(path):
    try:
        return json.loads(pathlib.Path(path).read_text(encoding='utf-8'))
    except FileNotFoundError:
        return {}


def apply(timeline, overrides):
    """The timeline with a clip's stored overrides laid over its beats ({'%.3f' % time: {field: value}})."""
    out = []
    for t, label, pose in timeline:
        ch = overrides.get('%.3f' % t)
        if ch:
            pose = dict(pose)
            pose.update({k: tuple(v) for k, v in ch.items()})
        out.append((t, label, pose))
    return out


class Judge:
    def __init__(self, unit, still, clipper, limbs, reach_limit=0.08):
        self.unit, self.still, self.clipper, self.limbs, self.reach_limit = unit, still, clipper, limbs, reach_limit

    def __call__(self, pose, written=None):
        c = self.still(pose)
        from animate_combat import clip_pose
        p = clip_pose(c, 0.0)
        items = sum(self.clipper.volumes(p).values())
        limbs = self.limbs.volume(p)
        cost = items + limbs + 300.0 * max(0.0, c.reach_error - self.reach_limit)     # never trade reach away
        if written is not None:
            for k, w in DEVIATION.items():
                if k in pose and k in written:
                    cost += w * float(np.linalg.norm(np.subtract(pose[k], written[k])))
        return cost, items, limbs, c.reach_error


def search_beat(judge, pose, fields, steps=60, seed=0):
    rnd = random.Random(seed)
    written = dict(pose)
    best = (judge(pose, written), dict(pose))
    for _ in range(steps):
        cand = dict(best[1])
        k = rnd.choice(fields)
        v = list(cand[k])
        i = rnd.randrange(len(v))
        v[i] += rnd.gauss(0, FIELDS[k])
        cand[k] = tuple(v)
        r = judge(cand, written)
        if r[0] < best[0][0]:
            best = (r, cand)
    changed = {k: [round(float(x), 3) for x in best[1][k]] for k in fields if tuple(best[1][k]) != tuple(written[k])}
    return best[0], judge(pose), changed


def run(unit_name, clip_names=None, steps=110, gate_items=1.5, gate_limbs=8.0):
    if unit_name != 'archer':
        raise SystemExit('only the archer stores searched poses so far')
    import animate_archer as AA
    from unit_anim import archer_clips as XC
    from unit_anim import clearance as CL
    unit = AA.ArcherUnit()
    judge = Judge(unit, XC.still, unit.clipper(),
                  CL.LimbClipper([(b, bone) for b, f, bone, l in unit.parts if l is None], bones=XC.BONES))
    store = load(XC.POSES_FILE)
    for name, timeline_fn in XC.TIMELINES.items():
        if clip_names and name not in clip_names:
            continue
        overrides = store.setdefault(name, {})
        tl = apply(timeline_fn(), overrides)
        loop_len = XC.LOOPS.get(name)
        for k, (t, label, pose) in enumerate(tl):
            if loop_len and abs(t - loop_len) < 1e-6:
                continue            # a loop's last beat is its first: it takes the first beat's fix below
            before = judge(pose)
            if before[1] <= gate_items and before[2] <= gate_limbs and before[3] <= judge.reach_limit:
                continue
            fields = [f for f in FIELDS if f in pose]
            after, _, changed = search_beat(judge, pose, fields, steps=steps, seed=k)
            if changed and after[0] < before[0]:
                overrides.setdefault('%.3f' % t, {}).update(changed)
            if loop_len and t == 0 and '%.3f' % t in overrides:
                overrides['%.3f' % loop_len] = overrides['%.3f' % t]
            print('%s %.3f %-18s items %.1f -> %.1f  limbs %.1f -> %.1f  reach %.2f -> %.2f' %
                  (name, t, label, before[1], after[1], before[2], after[2], before[3], after[3]), flush=True)
    pathlib.Path(XC.POSES_FILE).write_text(json.dumps(store, indent=1, sort_keys=True) + '\n', encoding='utf-8')


if __name__ == '__main__':
    run(sys.argv[1], sys.argv[2:] or None)
