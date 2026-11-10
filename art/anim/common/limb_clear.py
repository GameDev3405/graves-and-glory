"""Per-troop free-arm clearance: keeps a shieldless troop's empty left forearm (and the bracer, wrap or vambrace on it) out of
its own torso and hips, on the clips listed for that troop.

The shieldless one-hand troops share the revenant's free-fist motion (art/anim/knight/build_anims.py: the lead hand in the guard,
the counter-pull to the hip on every blow; art/anim/common/stances.py; art/anim/common/deaths.py). A troop whose forearm gear is
bulkier than the revenant's sinks that gear into the chest or hip where the revenant's clears it (tc_raider's bracer and wrap,
the Hollow King's vambrace and couter). For each listed clip this pass swings the left upper arm (left_arm) out from the body,
and a little forward or back where that clears it sooner, by the least angle that brings the forearm's cubes within `px` of the
body boxes' faces (the scorecard's own entry depth, tools/anim/scorecard.py pen_limb), at every quarter tick of the clip. The
needed angle is held a tick either side and eased (raised cosine, 3 ticks), so the arm drifts out ahead of the contact and back
after it, never in a jerk. Nothing else in the clip changes: the fist's path keeps its timing, the other bones keep their keys.

CLEAR[troop][clip] = px: the clearance target of that clip (at or under the clip's scorecard limit, with a margin).
Called by build_anims.py (its own clips, before writing out/) and stances.py merge() (rally, hold, fall back) through key_doc.
"""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(ROOT / "tools" / "anim"))
import render_geo as rg  # noqa: E402

CLEAR = {
    "tc_raider": {"idle_upper": 1.7, "attack_thrust": 1.3, "attack_overhead": 1.3, "attack_low_cut": 1.3,
                  "stance_ready_upper": 1.7, "stance_shield_up_upper": 1.7, "stance_rally_upper": 1.7,
                  "stance_hold_upper": 1.7, "stance_fallback_upper": 0.5, "death_right": 2.75, "death_right_drop1": 2.75,
                  "death_right_drop2": 2.75},
    "gb_hollowking": {"attack_overhead_cleave": 1.3, "stance_fallback_upper": 0.5, "death_right": 2.75,
                      "death_right_drop1": 2.75, "death_right_drop2": 2.75},
}
# The sword arm's bracer too, where it alone sinks (tc_raider's fall-back guard: right_bracer 0.76 px in the torso).
CLEAR_RIGHT = {"tc_raider": {"stance_fallback_upper": 0.5}}

STEP = 0.25          # ticks between solved samples
HOLD = 1.0           # ticks the needed angle is held either side
EASE = 3.0           # ticks: width of the raised-cosine ease
# Fix directions, (left_arm x, left_arm z, left_forearm y) animation degrees at full scale: x- raises the arm forward, z- swings it
# out, forearm y- rolls the fist knuckles-out (the bracer's inner edge turns off the body). Each clip takes ONE direction (the one
# whose largest fix is smallest, the roll weighted half: on a square forearm it barely changes the silhouette) and only its scale
# varies over the clip, so the fix reads as one gesture (the elbow opening, the fist rolling out) rather than a wobble.
DIRS = ((0.0, -20.0, 0.0), (-10.0, 0.0, 0.0), (-10.0, -10.0, 0.0), (0.0, 0.0, -45.0), (0.0, -10.0, -45.0),
        (0.0, -10.0, -30.0), (0.0, -10.0, -15.0), (0.0, -20.0, -15.0), (-5.0, -10.0, -15.0), (-5.0, -10.0, -45.0),
        (-10.0, 0.0, -30.0), (-10.0, -5.0, -15.0))
# The sword arm (CLEAR_RIGHT) never rolls its forearm (that would turn the blade) and swings out with z+; its hand is re-solved
# on every key so the blade keeps its world orientation (the guard reads the same, as stances.py's fall-back fits do).
DIRS_RIGHT = ((-10.0, 0.0, 0.0), (0.0, 20.0, 0.0), (-10.0, 10.0, 0.0))
SCALES = np.arange(0.0, 1.5001, 0.025)


def _weight(d):
    return abs(d[0]) + abs(d[1]) + 0.5 * abs(d[2])


def _rig(tid, geo_path, rules_path, side="left"):
    import scorecard as sc
    model = rg.load_geo(str(geo_path))
    rules = json.loads(Path(rules_path).read_text(encoding="utf-8")) if rules_path and Path(rules_path).is_file() else None
    rig = sc.Rig(model, rules, tid)
    movers, boxes = rig.pen_limb
    arm = side + "_arm"
    sub = set(rig.full.descendants(arm)) | {arm}
    mine = [mv for mv in movers if rig.model.cube_bone[mv[0]] in sub]
    order = [n for n in rig.model.order if n in sub]
    return sc, rig, mine, boxes, order, side


def _local(b, rot_rad, pos, scl):
    pv = b.pivot / 16.0
    rot = b.rot + rot_rad
    return (rg._T(-pos[0] / 16.0, pos[1] / 16.0, pos[2] / 16.0) @ rg._T(*pv) @ rg._Rz(rot[2]) @ rg._Ry(rot[1])
            @ rg._Rx(rot[0]) @ rg._S(*scl) @ rg._T(*(-pv)))


class _Probe:
    """Depth of the left forearm's cubes in the body boxes at one tick, for a trial (x, z) delta on left_arm."""

    def __init__(self, ctx, scene, t):
        self.sc, self.rig, self.left, self.boxes, self.order, side = ctx
        self.arm, self.fore = side + "_arm", side + "_forearm"
        self.fr = scene.evaluate(t)
        self.pose = scene._pose(t)
        J = np.array(self.boxes)
        self.J_corners = self.fr.corners[J]
        self.J_axes = np.array([self.sc._rot(self.fr.cube_M[j]) for j in self.boxes])

    def depth(self, dx=0.0, dz=0.0, fy=0.0):
        m, fr = self.rig.model, self.fr
        M = dict(fr.bone_M)
        for n in self.order:
            b = m.bones[n]
            p = self.pose.get(n, {})
            r = np.array(p.get("rotation", (0.0, 0.0, 0.0)), dtype=float)
            if n == self.arm:
                r = r + np.array([math.radians(-dx), 0.0, math.radians(dz)])
            elif n == self.fore:
                r = r + np.array([0.0, math.radians(-fy), 0.0])
            M[n] = M[b.parent] @ _local(b, r, p.get("position", (0.0, 0.0, 0.0)), p.get("scale", (1.0, 1.0, 1.0)))
        pts, roots = [], []
        for ci, uvw, root in self.left:
            cube = m.cube_list[ci]
            cp = cube.pivot / 16.0
            Mc = M[m.cube_bone[ci]] @ rg._T(*cp) @ rg._Rz(cube.rot[2]) @ rg._Ry(cube.rot[1]) @ rg._Rx(cube.rot[0]) @ rg._T(*(-cp))
            corners = cube.corners @ Mc[:3, :3].T + Mc[:3, 3]
            pts.append(self.sc.cube_points(corners, uvw))
            piv = (M[root] @ np.append(m.bones[root].pivot / 16.0, 1.0))[:3]
            roots.append(np.repeat(piv[None], len(uvw), axis=0))
        return self.sc.entry_depth(np.concatenate(pts), np.concatenate(roots), self.J_corners, self.J_axes) * 16.0

    def scale(self, target, d, start=0.0):
        """Least scale >= start of direction d keeping the depth at or under target (the largest scale if none does)."""
        for v in SCALES:
            if v >= start - 1e-9 and self.depth(d[0] * v, d[1] * v, d[2] * v) <= target:
                return float(v)
        return float(SCALES[-1])


def _ease(ts, need, loop, length):
    """Hold each sample's need HOLD ticks either side (largest magnitude wins), then smooth with a raised cosine EASE wide."""
    n = len(ts)
    held = np.zeros_like(need)
    k = int(round(HOLD / STEP))
    for i in range(n):
        best = need[i]
        for j in range(i - k, i + k + 1):
            jj = (j % (n - 1)) if loop else min(max(j, 0), n - 1)
            if np.linalg.norm(need[jj]) > np.linalg.norm(best):
                best = need[jj]
        held[i] = best
    w = int(round(EASE / STEP))
    ker = np.array([0.5 + 0.5 * math.cos(math.pi * j / (w + 1)) for j in range(-w, w + 1)])
    out = np.zeros_like(need)
    for i in range(n):
        acc, tot = np.zeros(need.shape[1]), 0.0
        for j, kv in zip(range(i - w, i + w + 1), ker):
            jj = (j % (n - 1)) if loop else min(max(j, 0), n - 1)
            acc += kv * held[jj]
            tot += kv
        out[i] = acc / tot
    return out


def _sample_keys(keys, t_sec):
    """Linear value of a rotation channel ({time: [x, y, z]}) at t_sec."""
    ts = sorted(keys, key=float)
    tf = [float(t) for t in ts]
    vs = [keys[t] for t in ts]
    if t_sec <= tf[0]:
        return list(vs[0])
    if t_sec >= tf[-1]:
        return list(vs[-1])
    for i in range(len(tf) - 1):
        if tf[i] <= t_sec <= tf[i + 1]:
            u = (t_sec - tf[i]) / (tf[i + 1] - tf[i]) if tf[i + 1] > tf[i] else 0.0
            return [vs[i][c] + u * (vs[i + 1][c] - vs[i][c]) for c in range(3)]
    return list(vs[-1])


def clear_clip(ctx, doc, name, target, log=None):
    sc, side = ctx[0], ctx[5]
    arm, fore, hand = side + "_arm", side + "_forearm", side + "_hand"
    anims = doc["animations"]
    a = anims[name]
    for b in (arm, fore, hand):
        if b in a["bones"] and not all(isinstance(v, list) for v in a["bones"][b]["rotation"].values()):
            raise ValueError("%s/%s: only plain linear keys are supported" % (name, b))
    base = [b for n, b, _s in sc.units(list(anims)) if n == name][0]
    length = float(a["animation_length"]) * 20.0
    loop = a.get("loop") is True
    ts = np.arange(0.0, length + 1e-9, STEP)

    def probes():
        clips = [rg.load_clip(doc, b) for b in base] + [rg.load_clip(doc, name)]
        scene = rg.Scene(ctx[1].model, None, clips, 0.0)
        return [_Probe(ctx, scene, min(t, length - 1e-4)) for t in ts]

    ps = probes()
    worst0 = max(p.depth() for p in ps)
    if worst0 <= target:
        return ts, np.zeros(len(ts))
    best = None
    for d in (DIRS if side == "left" else DIRS_RIGHT):   # of the directions whose eased fix clears every sample, the one
        total = _fit(ps, d, target, ts, loop, length)       # whose largest fix is smallest
        if total is not None and (best is None or total.max() * _weight(d) < best[0]):
            best = (total.max() * _weight(d), d, total)
    if best is None:
        raise ValueError("%s: no fix direction clears the %s forearm to %.2f px" % (name, side, target))
    _c, d, total = best
    delta = np.outer(total, d)
    bones = ctx[1].model.bones
    old = {b: dict(a["bones"][b]["rotation"]) for b in (arm, fore, hand) if b in a["bones"]}
    _apply(a, arm, ts, delta[:, :2], lambda v, e: [v[0] + e[0], v[1], v[2] + e[1]])
    _apply(a, fore, ts, delta[:, 2:], lambda v, e: [v[0], v[1] + e[0], v[2]])
    if side == "right":   # the hand keeps the blade's orientation relative to the torso
        def resolve(v, e, t):
            R = (_mat(bones[arm], _sample_keys(old[arm], t)) @ _mat(bones[fore], _sample_keys(old[fore], t))
                 @ _mat(bones[hand], v))
            An = _mat(bones[arm], _sample_keys(a["bones"][arm]["rotation"], t))
            Fn = _mat(bones[fore], _sample_keys(a["bones"][fore]["rotation"], t))
            return _json(bones[hand], (An @ Fn).T @ R)
        _apply(a, hand, ts, delta[:, :1], resolve, with_time=True)
    worst = max(p.depth() for p in probes())
    if log:
        k = float(total.max())
        log("%s: %s forearm %.2f -> %.2f px (target %.2f); %s x %.1f z %.1f, %s y %.1f deg at most"
            % (name, side, worst0, worst, target, arm, d[0] * k, d[1] * k, fore, d[2] * k))
    return ts, total


def _mat(b, deg):
    r = b.rot + np.array([math.radians(-deg[0]), math.radians(-deg[1]), math.radians(deg[2])])
    return rg._Rz(r[2])[:3, :3] @ rg._Ry(r[1])[:3, :3] @ rg._Rx(r[0])[:3, :3]


def _json(b, R):
    """Animation degrees (x, y, z) of a bone whose local rotation (rest included) is R = Rz Ry Rx."""
    ry = math.asin(max(-1.0, min(1.0, -R[2, 0])))
    rx = math.atan2(R[2, 1], R[2, 2])
    rz = math.atan2(R[1, 0], R[0, 0])
    r = np.array([rx, ry, rz]) - b.rot
    return [-math.degrees(r[0]), -math.degrees(r[1]), math.degrees(r[2])]


def _fit(ps, d, target, ts, loop, length):
    """Eased scale of direction d over the clip clearing every sample, or None. The probes pose the original clip and the fix
    is linear in the Euler channels, so a scale is judged exactly without re-keying: ease the needs, and where the eased scale
    still fails (it sits in a bump), raise that sample's need from there."""
    need = np.array([p.scale(target, d) for p in ps], dtype=float)
    if need.max() >= SCALES[-1]:
        return None
    for _round in range(30):
        total = _ease(ts, need[:, None], loop, length)[:, 0]
        bad = [i for i, p in enumerate(ps) if p.depth(*(np.array(d) * total[i])) > target]
        if not bad:
            return total
        for i in bad:   # raise the need by what the eased scale still lacks there (the ease spreads it, so it takes rounds)
            req = ps[i].scale(target, d, total[i] + 0.025)
            if req >= SCALES[-1]:
                return None
            need[i] += req - total[i]
    return None


def _apply(a, bone, ts, add, fn, with_time=False):
    live = [i for i in range(len(ts)) if np.abs(add[i]).max() > 1e-4]
    if not live:
        return
    lo, hi = max(0, live[0] - 1), min(len(ts) - 1, live[-1] + 1)
    old = dict(a["bones"][bone]["rotation"])
    times = sorted({float(k) for k in old} | {round(float(ts[i]) / 20.0, 6) for i in range(lo, hi + 1)})
    new = {}
    for t in times:
        v = _sample_keys(old, t)
        tick = t * 20.0
        i = int(round(tick / STEP))
        if lo <= i <= hi and abs(ts[i] - tick) < 1e-6:
            v = fn(v, [float(c) for c in add[i]], t) if with_time else fn(v, [float(c) for c in add[i]])
        new[_fmt(t)] = [round(c, 3) for c in v]
    a["bones"][bone]["rotation"] = _thin(new)


def _thin(keys, tol=0.02):
    """Drops keys that linear interpolation between their kept neighbours reproduces within tol degrees."""
    ts = list(keys)
    keep = [ts[0]]
    for i in range(1, len(ts) - 1):
        t0, t2 = float(keep[-1]), float(ts[i + 1])
        v0, v2 = keys[keep[-1]], keys[ts[i + 1]]
        # the key is dropped only if every key between the last kept one and the next is still reproduced
        ok = all(abs(v0[c] + (float(tt) - t0) / (t2 - t0) * (v2[c] - v0[c]) - keys[tt][c]) <= tol
                 for tt in ts[ts.index(keep[-1]) + 1:i + 1] for c in range(3))
        if not ok:
            keep.append(ts[i])
    keep.append(ts[-1])
    return {t: keys[t] for t in keep}


def _fmt(t):
    s = ("%.6f" % t).rstrip("0")
    return s + "0" if s.endswith(".") else s


def key_doc(tid, doc, geo_path, rules_path, log=print):
    """Clears the listed clips of `tid` in doc (an animation json dict), in place. Other troops and clips are untouched."""
    for side, table in (("left", CLEAR.get(tid)), ("right", CLEAR_RIGHT.get(tid))):
        if not table:
            continue
        ctx = _rig(tid, geo_path, rules_path, side)
        for name, px in table.items():
            if name in doc["animations"]:
                clear_clip(ctx, doc, name, px, log)


def main():
    import argparse
    ap = argparse.ArgumentParser(description="Clear the listed clips of a troop's deployed animation json (in place).")
    ap.add_argument("troop")
    ap.add_argument("--out", help="write here instead of over the deployed json")
    a = ap.parse_args()
    assets = ROOT / "src/main/resources/assets/gravesandglory"
    src = assets / "animations/troop" / f"{a.troop}.animation.json"
    doc = json.loads(src.read_text(encoding="utf-8"))
    key_doc(a.troop, doc, assets / "geo/troop" / f"{a.troop}.geo.json", assets / "troop_rig" / f"{a.troop}.json")
    with open(a.out or src, "w", newline="\n") as fh:
        fh.write(json.dumps(doc, indent=1))


if __name__ == "__main__":
    main()
