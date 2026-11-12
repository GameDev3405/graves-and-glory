"""tc_raider batch review (2026-11-12): a late layer over the troop's finished clip file, for Kort's four focus points.

python art/anim/knight/raider_review.py [--anim FILE] [--out FILE]

Build order for tc_raider: build_anims.py (GG_ANIM_TROOP=tc_raider), export.py, stances.py, reactions.py, charge.py, then
THIS. It reads the exported clip file (or --anim), changes only what is listed below, and writes it back (or to --out). Every
clip it does not name stays byte for byte as it was, and in the clips it names every bone channel it does not name keeps its
keys: the layer adds keys, it does not re-author a clip. Running it twice gives the same file as running it once (each pass
starts from the clip's own bones; the bones it adds are dropped and made again).

  LIFE      every hanging piece (kelp on the helm and shoulder, scarf, skirt, the hip sash `tail`) and both shoulder plates
            move in the attacks, the medium hits, the block, the pulled and knockback hits and the stun: fullbody.soft_motion,
            the kit's own spring, measured from the clip's first sample and faded in and out, so each clip still starts and
            ends where the stance has those pieces (the stun's dazed loop is run to its steady swing so it still closes).
            Kort 2026-11-11: "i want the whole unit to be animated including pieces of its armor".
  SASH      the hip sash hangs over the left thigh: it follows the thigh's swing (SASH_FOLLOW of the left leg's pitch), so the
            greave never passes through it in the walk, the run, the fall back and the knockback (focus check armour_clip).
  ROLL      the cutlass's single edge has to turn about half round from the guard to lead the overhead and the low cut. The
            builder turned it from the load on; now the hand keeps the guard's grip through the wind-up and the edge turns
            onto the cut in the drive (ROLL_WINDOW), so the weapon never turns in the hand while it is drawn back. Kort
            2026-11-11: "weapons spins around in hand" (focus check grip_roll). The pointing of the blade is unchanged: only
            its turn about its own length is re-timed, and it reaches the builder's edge before the impact.
  SETTLE    the attacks end exactly on their first pose (the free forearm drifted 3 to 12 degrees off it).
  HANDOFF   (fix round 1) the attacks' first and last pose ARE stance_ready's (the phase both halves are nearest): the free arm,
            torso, head, legs and hips started about 15 degrees off it. Eased out over the first 2 ticks and back in over the
            last 3; the right arm, the impact marker and the length are untouched.
  HELM      (fix round 1) the helm stays off the shoulder shells in the medium hits, the block, the knockback and the stun, as
            the game plays them (hits added onto stance_ready): the shell slides down its arm off the helm, then the head
            rolls away, then (hits only) the arm gives back part of its flinch, each only where and as much as a sample needs.
  FLOOR     (fix round 1) the hips rise where the pulled hit, the knockback or the stun put a foot under the ground.
  STANCE    (fix round 1) stance_ready's free upper arm turns elbow-out until its bracer is out of the chest (HANDOFF lands on it).
HELM and FLOOR measure with focus_check's own geometry (tools/anim), so they need the repo's tools on the path. Test:
tools/anim/test_raider_review.py (the shipped file is this layer's output, and a second pass changes nothing).
"""
import argparse
import json
import math
import os
import sys
from pathlib import Path

import numpy as np

os.environ["GG_ANIM_TROOP"] = "tc_raider"
HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parent / "common"))
import build_anims as B  # noqa: E402
import fullbody  # noqa: E402

TID = "tc_raider"
ASSET = ROOT / "src/main/resources/assets/gravesandglory/animations/troop" / f"{TID}.animation.json"
OUT = HERE.parent / TID / "out" / f"{TID}.animation.json"   # kept equal to ASSET (main)
SUB, TPS = B.SUB, B.TPS

LIFE = ("attack_thrust", "attack_overhead", "attack_low_cut", "hit_medium_front", "hit_medium_back", "hit_medium_right",
        "hit_medium_left", "hit_block", "hit_pulled", "hit_knockback", "stun_upper", "stun_out_upper")
LOOP_FROM = {"stun_upper": 4.0}           # the dazed loop: ticks 4 to the end repeat
# stun_out_upper plays over the fading stun_legs, which owns the hip pieces (stun_upper writes them): read it, never write it
READ_LEGS = {"stun_out_upper": "stun_legs"}
SOFTEN_PASSES = 3
SASH = "tail"
SASH_FOLLOW = 0.9
SASH_CLIPS = ("walk_legs", "run_legs", "stance_fallback_legs")   # plus every LIFE clip's legs
# the edge's half turn onto the cut: from the ready grip (tick 0) it may creep at ROLL_CREEP deg per tick (under focus_check's grip_roll
# warning) until the last sample grip_roll judges (its trailing tick ends 1.25 ticks before the arc), then it finishes by the arc's first fast
# sample so the edge leads the cut (edge_lead). (from, arc start - 1.25 ticks, arc start), ticks: a 2-tick window then turns the hand under the scorecard's ang_speed_arms; the arc from focus_check.
ROLL_WINDOW = {"attack_overhead": (0.0, 7.0, 8.25), "attack_low_cut": (0.0, 5.0, 6.25)}
ROLL_CREEP = 11.5
SETTLE = ("attack_thrust", "attack_overhead", "attack_low_cut")
SETTLE_TICKS = 3.0
# after the cut the builder's hand wobbles through the Euler pole (overhead: a 23 deg kick at tick 12, focus_check pop; low cut:
# the z angle ticking 24, 0, -14, 0, 18 round tick 10, jitter): over these ticks the hand turns on one axis from the pose it
# has at the start to the one at the end, at the speeds it carries in and out (a Hermite ease, so no new kick at either end)
HAND_EASE = {"attack_overhead": (11.25, 13.25), "attack_low_cut": (8.75, 11.5)}
HANDOFF_IN, HANDOFF_OUT, HANDOFF_TOL = 2.0, 3.0, 0.05
HANDOFF_SHARE = {}
STANCE_FREE, STANCE_TURN_MAX = 1.27, 24
# the clips the helm rule judges, each over the base the game (and focus_check) plays it on
HELM = (("hit_medium_front", ["hit_medium_front_legs"]), ("hit_medium_back", ["hit_medium_back_legs"]),
        ("hit_medium_right", ["hit_medium_right_legs"]), ("hit_medium_left", ["hit_medium_left_legs"]),
        ("hit_block", ["hit_block_legs"]), ("hit_knockback", ["hit_knockback_legs"]), ("stun_upper", ["stun_legs"]))
HELM_FIT, HELM_SLIDE, HELM_ROLL, HELM_GAIN, HELM_ARM, HELM_ARM_GAIN, HELM_ITER = 1.5, 1.5, 12.0, 5.0, 0.7, 0.4, 16
HELM_TOL = 0.1
# legs clip -> the clips judged over it
FLOOR = (("hit_pulled_legs", [("hit_pulled", ["hit_pulled_legs"])]),
         ("hit_knockback_legs", [("hit_knockback", ["hit_knockback_legs"])]),
         ("stun_legs", [("stun_upper", ["stun_legs"])]))
FLOOR_KEEP, FLOOR_TOL = 0.4, 0.05


# ---------------------------------------------------------------------------------------------- clip sampling
def sample(clip, t):
    """{bone: (rot, pos|None)} of a clip at t ticks (linear, as the keys are baked)."""
    out = {}
    for b, ch in clip["bones"].items():
        r = B.curve_at(ch["rotation"], t / TPS) if "rotation" in ch else (0.0, 0.0, 0.0)
        p = B.curve_at(ch["position"], t / TPS) if "position" in ch else None
        out[b] = (r, p)
    return out


def ticks_of(clip):
    return int(round(clip["animation_length"] * TPS))


def bake_bones(samples, bones):
    """Channels for `bones` from samples at SUB per tick, thinned as the builder thins."""
    out = {}
    for b in bones:
        rots = [s[b][0] for s in samples]
        cols = [B.unwrap([r[i] for r in rots]) for i in range(3)]
        rv = [[round(cols[0][i], 3), round(cols[1][i], 3), round(cols[2][i], 3)] for i in range(len(samples))]
        ch = {"rotation": {B.fmt_t(i / SUB / TPS): rv[i] for i in B.thin(rv, B.ROT_TOL)}}
        if any(s[b][1] is not None for s in samples):
            pv = [[round(v, 4) for v in (s[b][1] or (0, 0, 0))] for s in samples]
            ch["position"] = {B.fmt_t(i / SUB / TPS): pv[i] for i in B.thin(pv, B.POS_TOL)}
        out[b] = ch
    return out


def set_bones(clip, chans):
    clip["bones"] = {**clip["bones"], **chans}
    clip["bones"] = {b: clip["bones"][b] for b in sorted(clip["bones"])}


# ---------------------------------------------------------------------------------------------- LIFE
def soft_bones():
    parts = fullbody.soft_parts_for()
    return {b for c in parts.values() for b in c["bones"]} | set(fullbody.plates_for())


def life(anims, name):
    up = anims[name]
    added = soft_bones()
    read_only = name in READ_LEGS
    legs_name = READ_LEGS.get(name) or (name.replace("_upper", "_legs") if name.endswith("_upper") else name + "_legs")
    lg = anims.get(legs_name)
    if read_only:                 # a copy without the pieces stun_upper gave it
        lg = {**lg, "bones": {b: v for b, v in lg["bones"].items() if b not in added}}
    for c in (up, lg):            # idempotent: drop what an earlier pass added
        if c:
            c["bones"] = {b: v for b, v in c["bones"].items() if b not in added}
    n = ticks_of(up) * SUB
    times = [i / SUB for i in range(n + 1)]
    lf = LOOP_FROM.get(name)
    if lf is not None:            # run the loop to its steady swing: entry, then the loop three more times, keep the last
        k0 = int(lf * SUB)
        seq = times + [t for _ in range(3) for t in times[k0 + 1:]]
    else:
        seq = times
    us = [sample(up, t) for t in seq]
    ls = [sample(lg, t) if lg else {} for t in seq]
    us, ls = fullbody.soft_motion(us, ls, report=True)
    if lf is not None:
        k0 = int(lf * SUB)
        steady_u = us[-(len(times) - k0):]
        steady_l = ls[-(len(times) - k0):]
        # the entry is the first pass's jolt, rising out of the steady loop's start and falling back into it by lf, so the
        # clip's first frame is its last too (the scorecard's wrap) and the loop picks up where the entry ends
        new_u, new_l = [], []
        for i in range(len(times)):
            if i < k0:
                w = math.sin(math.pi * i / k0)
                new_u.append(_blend(steady_u[0], us[i], w, added) if i else dict(us[0], **{b: steady_u[0][b] for b in added if b in steady_u[0]}))
                new_l.append(_blend(steady_l[0], ls[i], w, added) if i else dict(ls[0], **{b: steady_l[0][b] for b in added if b in steady_l[0]}))
            else:
                new_u.append(steady_u[i - k0]); new_l.append(steady_l[i - k0])
        # the loop closes exactly: its last sample is its first
        new_u[-1] = {**new_u[-1], **{b: new_u[k0][b] for b in added if b in new_u[k0]}}
        new_l[-1] = {**new_l[-1], **{b: new_l[k0][b] for b in added if b in new_l[k0]}}
        us, ls = new_u, new_l
    else:                         # a hit's jolt reaches the cloth a little softened: no one-sample kick (focus_check pop)
        us, ls = _soften(us, added), _soften(ls, added)
    ub = sorted(b for b in added if b in us[0])
    lb = sorted(b for b in added if b in ls[0])
    if lg is None:                # no footwork clip: the hip pieces ride in the upper clip
        for u, l in zip(us, ls):
            u.update({b: l[b] for b in lb})
        ub, lb = sorted(set(ub) | set(lb)), []
    set_bones(up, bake_bones(us, ub))
    if read_only:
        return None
    if lg is not None and lb:
        set_bones(lg, bake_bones(ls, lb))
    return legs_name if lg is not None else None


def _soften(seq, bones, passes=SOFTEN_PASSES):
    """[1 2 1]/4 over the samples of the soft bones' rotations, end samples kept."""
    seq = [dict(s) for s in seq]
    for _ in range(passes):
        prev = [dict(s) for s in seq]
        for i in range(1, len(seq) - 1):
            for b in bones:
                if b in seq[i] and b in prev[i - 1] and b in prev[i + 1]:
                    r = tuple(float((x + 2 * y + z) / 4.0) for x, y, z in zip(prev[i - 1][b][0], prev[i][b][0], prev[i + 1][b][0]))
                    seq[i][b] = (r, prev[i][b][1])
    return seq


def _blend(a, b, w, bones):
    out = dict(a)
    for k in bones:
        if k in a and k in b:
            ra, pa = a[k]; rb, pb = b[k]
            r = tuple(float(x + (y - x) * w) for x, y in zip(ra, rb))
            p = None if pa is None and pb is None else tuple(float(x + (y - x) * w) for x, y in zip(pa or (0, 0, 0), pb or (0, 0, 0)))
            out[k] = (r, p)
    return out


# ---------------------------------------------------------------------------------------------- SASH
def sash(clip, loop, fresh=False):
    """Adds the left thigh's pitch (from the first sample, or the loop's mean) to the sash's own keys (or its rest).
    fresh: the clip has no sash keys of its own (a gait), so any found are this layer's from an earlier pass: dropped."""
    if "left_leg" not in clip["bones"]:
        return
    if fresh:
        clip["bones"].pop(SASH, None)
    n = ticks_of(clip) * SUB
    ts = [i / SUB for i in range(n + 1)]
    pitch = np.array([sample(clip, t)["left_leg"][0][0] for t in ts])
    ref = float(np.mean(pitch[:-1])) if loop else float(pitch[0])
    own = clip["bones"].get(SASH)
    samples = []
    for t, p in zip(ts, pitch):
        r, q = (B.curve_at(own["rotation"], t / TPS), B.curve_at(own["position"], t / TPS) if "position" in own else None) \
            if own else ((0.0, 0.0, 0.0), None)
        samples.append({SASH: ((r[0] + SASH_FOLLOW * (p - ref), r[1], r[2]), q)})
    set_bones(clip, bake_bones(samples, [SASH]))


# ---------------------------------------------------------------------------------------------- ROLL
def _blade_axis():
    w = B.MODEL.bones[B.EDGE_BONE]
    Rw = B.rg._Rz(w.rot[2])[:3, :3] @ B.rg._Ry(w.rot[1])[:3, :3] @ B.rg._Rx(w.rot[0])[:3, :3]
    return Rw @ np.array([0.0, 1.0, 0.0])


def _twist(R, a):
    """Angle (deg) of R's twist about the unit axis a (R = swing * twist)."""
    w = math.sqrt(max(0.0, 1.0 + np.trace(R))) / 2.0
    v = np.array([R[2, 1] - R[1, 2], R[0, 2] - R[2, 0], R[1, 0] - R[0, 1]]) / (4.0 * w if w > 1e-6 else 1.0)
    return math.degrees(2.0 * math.atan2(float(v @ a), w))


def roll(clip, name):
    hold, ta, tb = ROLL_WINDOW[name]
    a = _blade_axis()
    n = ticks_of(clip) * SUB
    ts = [i / SUB for i in range(n + 1)]
    rots = [sample(clip, t)["right_hand"][0] for t in ts]
    # the roll summed sample to sample (the hand's turn about the blade against the forearm, as grip_roll measures it): a
    # twist taken against one far pose would mix in the wind-up's swing of the wrist
    L = [B.local("right_hand", r)[:3, :3] for r in rots]
    phi = [0.0]
    for i in range(1, len(L)):
        phi.append(phi[-1] + _twist(L[i - 1].T @ L[i], a))
    # up to ta the hand turns steadily toward the roll the cut needs (phi at tb) at ROLL_CREEP deg per tick, whatever the
    # builder's wrist does on the way; from ta the gap left closes evenly by tb
    k_b = int(round(tb * SUB))
    total = phi[k_b] - phi[0]
    ds, k_a = [], None
    for i, t in enumerate(ts):
        if hold < t <= ta:
            want = phi[0] + math.copysign(min(abs(total), ROLL_CREEP * (t - hold)), total)
            ds.append(want - phi[i])
            k_a = i
        else:
            ds.append(0.0)
    for i, t in enumerate(ts):
        if ta < t < tb and k_a is not None:
            ds[i] = ds[k_a] * (tb - t) / (tb - ta)
    if max(abs(d) for d in ds) < 0.5:
        return                    # the roll already keeps to the schedule (an earlier pass): nothing to do
    out = []
    for i, d in enumerate(ds):
        # outside the window, and wherever nothing changes, the builder's own Euler keys stand
        r = B.roll_hand(rots[i], d, rots[i]) if abs(d) > 1e-6 else tuple(rots[i])
        out.append({"right_hand": (r, None)})
    set_bones(clip, bake_bones(out, ["right_hand"]))


# ---------------------------------------------------------------------------------------------- SETTLE
def _m(rot):
    """Rotation matrix of animation degrees (rest rotation excluded), the same convention as B.local."""
    r = np.radians([-rot[0], -rot[1], rot[2]])
    return B.rg._Rz(r[2])[:3, :3] @ B.rg._Ry(r[1])[:3, :3] @ B.rg._Rx(r[0])[:3, :3]


def _axis_angle(R):
    ang = math.acos(max(-1.0, min(1.0, (np.trace(R) - 1.0) / 2.0)))
    if ang < 1e-9:
        return np.array([1.0, 0.0, 0.0]), 0.0
    v = np.array([R[2, 1] - R[1, 2], R[0, 2] - R[2, 0], R[1, 0] - R[0, 1]])
    if np.linalg.norm(v) < 1e-9:   # a half turn: take the axis from the symmetric part
        v = np.sqrt(np.maximum(np.diag(R) + 1.0, 0.0) / 2.0)
    return v / np.linalg.norm(v), ang


def _rodrigues(a, t):
    K = np.array([[0, -a[2], a[1]], [a[2], 0, -a[0]], [-a[1], a[0], 0]])
    return np.eye(3) + math.sin(t) * K + (1 - math.cos(t)) * K @ K


def _euler_near(R, ref):
    """Animation degrees for R on the Euler branch nearest ref (both ZYX solutions, 360-wrapped)."""
    e = np.array(B.decompose_zyx(R))
    ref = np.array(ref, dtype=float)
    best = None
    for c in (e, np.array([e[0] - 180.0, -180.0 - e[1], e[2] + 180.0])):
        c = ref + (c - ref + 180.0) % 360.0 - 180.0
        if best is None or np.abs(c - ref).sum() < np.abs(best - ref).sum():
            best = c
    return tuple(float(v) for v in best)


def hand_ease(clip, name):
    t0, t1 = HAND_EASE[name]
    ch = clip["bones"]["right_hand"]
    n = ticks_of(clip) * SUB
    ts = [i / SUB for i in range(n + 1)]
    rots = [B.curve_at(ch["rotation"], t / TPS) for t in ts]
    i0, i1 = int(t0 * SUB), int(t1 * SUB)
    R0, R1 = _m(rots[i0]), _m(rots[i1])
    ax, th = _axis_angle(R1 @ R0.T)
    if th < 1e-6:
        return

    def along(a, b):              # per-sample turn from pose a to pose b, along ax (deg->rad as the axis-angle gives)
        v, g = _axis_angle(_m(rots[b]) @ _m(rots[a]).T)
        return float(v @ ax) * g
    m0 = along(i0 - 1, i0) * (i1 - i0) / th
    m1 = along(i1, i1 + 1) * (i1 - i0) / th
    out, prev = [], None
    for i, r in enumerate(rots):
        if i0 < i < i1:
            u = (i - i0) / float(i1 - i0)
            s = (u ** 3 - 2 * u ** 2 + u) * m0 + (-2 * u ** 3 + 3 * u ** 2) + (u ** 3 - u ** 2) * m1
            r = _euler_near(_rodrigues(ax, s * th) @ R0, prev)
        prev = r
        out.append({"right_hand": (tuple(float(v) for v in r), None)})
    if max(abs(x - y) for o, r in zip(out, rots) for x, y in zip(o["right_hand"][0], r)) < 2 * B.ROT_TOL:
        return                    # already eased (an earlier pass): re-baking would only re-thin the keys
    set_bones(clip, bake_bones(out, ["right_hand"]))


def settle(clip):
    n = ticks_of(clip) * SUB
    ts = [i / SUB for i in range(n + 1)]
    end = ts[-1]
    chans = {}
    for b, ch in clip["bones"].items():
        if "rotation" not in ch or b in soft_bones() or b not in B.MODEL.bones:
            continue
        first, last = np.array(B.curve_at(ch["rotation"], 0.0)), np.array(B.curve_at(ch["rotation"], end / TPS))
        if np.abs(last - first).max() < 0.05:
            continue
        # the closing ease is a rotation (shortest turn from the last pose to the first), not an Euler blend: the cutlass
        # hand sits near the Euler pole (y ~ -170), where blending the three angles spins the blade about itself
        ax, ang = _axis_angle(_m(first) @ _m(last).T)
        samples = []
        for t in ts:
            r = np.array(B.curve_at(ch["rotation"], t / TPS))
            w = B.smooth((t - (end - SETTLE_TICKS)) / SETTLE_TICKS)
            if w > 0:
                r = np.array(r if ang < 1e-9 else _euler_near(_rodrigues(ax, w * ang) @ _m(r), first if t == end else r))
            p = B.curve_at(ch["position"], t / TPS) if "position" in ch else None
            samples.append({b: (tuple(float(v) for v in r), p)})
        chans.update(bake_bones(samples, [b]))
    set_bones(clip, chans)


# ---------------------------------------------------------------------------------------------- HANDOFF
def handoff(anims, name):
    """The attack's first and last pose onto stance_ready (one phase of it for the upper and the legs, the one they are
    nearest): the difference is added in full at the ends and eased out over HANDOFF_IN ticks from the start and in over
    HANDOFF_OUT ticks before the end, so the swing itself (the right arm, the impact, the length) is untouched."""
    pairs = [(anims[name], anims["stance_ready_upper"])]
    if name + "_legs" in anims:
        pairs.append((anims[name + "_legs"], anims["stance_ready_legs"]))
    loop = ticks_of(anims["stance_ready_upper"])
    hard = set(B.MODEL.bones) - soft_bones()
    firsts = [(c, s, sample(c, 0.0)) for c, s in pairs]

    def cost(ph):
        return sum(math.degrees(_axis_angle(_m(a[b][0]) @ _m(sp[b][0]).T)[1])
                   for c, s, a in firsts for sp in [sample(s, ph)] for b in sp if b in a and b in hard)
    ph = min((i / SUB for i in range(loop * SUB)), key=cost)
    for c, s, a in firsts:
        sp = sample(s, ph)
        n = ticks_of(c) * SUB
        ts = [i / SUB for i in range(n + 1)]
        end = ts[-1]
        chans = {}
        for b in sorted(sp):
            if b not in a or b not in hard:
                continue
            dr = HANDOFF_SHARE.get(b, 1.0) * (np.array(_euler_near(_m(sp[b][0]), a[b][0])) - np.array(a[b][0]))
            dp = np.array(sp[b][1]) - np.array(a[b][1]) if sp[b][1] is not None and a[b][1] is not None else np.zeros(3)
            if max(np.abs(dr).max(), 10.0 * np.abs(dp).max()) < HANDOFF_TOL:
                continue          # already on the stance (an earlier pass)
            samples = []
            for t in ts:
                w = 1.0 - B.smooth(t / HANDOFF_IN) if t < HANDOFF_IN else B.smooth((t - (end - HANDOFF_OUT)) / HANDOFF_OUT) if t > end - HANDOFF_OUT else 0.0
                r, p = sample(c, t)[b]
                r = tuple(float(v) for v in np.array(r) + w * dr)
                p = None if p is None else tuple(float(v) for v in np.array(p) + w * dp)
                samples.append({b: (r, p)})
            chans.update(bake_bones(samples, [b]))
        set_bones(c, chans)


# ---------------------------------------------------------------------------------------------- HELM and FLOOR (measured)
# Both rules measure the clip the way focus_check judges it (its own geometry: hits ADDED onto stance_ready, the stun over
# its legs) and add only what a sample needs, so a second pass finds nothing to add.
_GEO = {}


def _geo():
    if not _GEO:
        sys.path.insert(0, str(ROOT / "tools/anim"))
        import focus_check as F   # noqa: E402
        rel = "%s/geo/troop/%s.geo.json" % (F.sc.ASSETS, TID)
        model = F.rg.load_geo(str(ROOT / rel))
        rig = F.sc.Rig(model, F.sc.rig_rules_for(str(ROOT), rel), TID)
        P = F.Parts(rig)
        rest = F.rg.Scene(model, None, [], 0.0).evaluate(0.0)
        hard = P.hard()
        _GEO.update(F=F, model=model, P=P, hard=hard, rest=rest,
                    sole=max(0.0, -float(rest.corners[hard][:, :, 1].min()) * 16.0))
    return _GEO


def _frames(doc, name, base, n):
    """Frames of clip `name` over its base clips at samples 0..n (SUB per tick), composed as focus_check composes them."""
    g = _geo()
    F, rg = g["F"], g["F"].rg
    under = F.additive_under(name, set(doc["animations"]))
    clips = [rg.load_clip(doc, b) for b in base] + [rg.load_clip(doc, name)]
    if under:
        held = [rg.load_clip(doc, u) for u in under if u not in base]
        scene = rg.Scene(g["model"], None, held, 0.0, None, clips)
    else:
        scene = rg.Scene(g["model"], None, clips, 0.0)
    top = clips[-1]
    return [scene.evaluate(min(i / SUB, top.length - (1e-4 if top.loop == "once" else 0.0))) for i in range(n + 1)]


def _pair_fit(fr, a, b):
    """focus_check's fit of bone a's cubes and bone b's (px, the deepest lattice point of either inside the other)."""
    g = _geo()
    F, P, bone_of = g["F"], g["P"], g["model"].cube_bone
    A = [i for i, x in enumerate(bone_of) if x == a]
    Bc = [i for i, x in enumerate(bone_of) if x == b]
    best = 0.0
    for mv, bx in ((A, Bc), (Bc, A)):
        for i in mv:
            pts = F.sc.cube_points(fr.corners[i], P.lat[i])
            for j in bx:
                R = F.sc._rot(fr.cube_M[j])
                C = fr.corners[j]
                c = C.mean(axis=0)
                half = np.abs((C - c) @ R).max(axis=0)
                q = (pts - c) @ R
                best = max(best, float((half - np.abs(q)).min(axis=1).max()) * 16.0)
    return best


def _rest_pair(a, b):
    key = ("rest", a, b)
    if key not in _GEO:
        _GEO[key] = _pair_fit(_geo()["rest"], a, b)
    return _GEO[key]


def _floor_depth(fr):
    g = _geo()
    return -float(fr.corners[g["hard"]][:, :, 1].min()) * 16.0 - g["sole"]


def _spread(need):
    """A curve at or above `need` everywhere, without one-sample spikes: widened one sample each way, then [1 2 1]/4."""
    need = np.asarray(need, dtype=float)
    wide = np.maximum.reduce([need, np.roll(need, 1), np.roll(need, -1)])
    wide[0], wide[-1] = max(need[0], need[1]), max(need[-1], need[-2])
    sm = wide.copy()
    sm[1:-1] = (wide[:-2] + 2 * wide[1:-1] + wide[2:]) / 4.0
    return np.maximum(sm, need)


def _loop_safe(curve, k0):
    """For the stun (looped from sample k0, first sample = last): a constant over the loop, plus an entry bump that is
    0 at the first sample, so the loop and the clip's wrap stay closed."""
    curve = np.asarray(curve, dtype=float)
    c = float(curve[k0:].max())
    out = np.full(len(curve), c)
    out[1:k0] = np.maximum(curve[1:k0], c)
    return out


def _loop_safe_signed(curve, k0):
    """The same for a signed curve (the head's roll): over the loop the value of largest size the loop or its first
    sample asks for, free over the entry."""
    curve = np.asarray(curve, dtype=float)
    tail = np.concatenate([curve[:1], curve[k0:]])
    c = float(tail[int(np.argmax(np.abs(tail)))])
    out = np.full(len(curve), c)
    out[1:k0] = curve[1:k0]
    return out


def _ends_zero(curve):
    curve = np.array(curve, dtype=float)
    curve[0] = curve[-1] = 0.0
    return curve


def _add_channel(clip, bone, n, rot=None, pos=None):
    """Re-bake `bone` with per-sample additions: rot (n+1, 3) degrees, pos (n+1, 3) px."""
    samples = []
    for i in range(n + 1):
        r, p = sample(clip, i / SUB).get(bone, ((0.0, 0.0, 0.0), None))
        if rot is not None:
            r = tuple(float(v) for v in np.array(r) + rot[i])
        if pos is not None:
            p = tuple(float(v) for v in np.array(p if p is not None else (0.0, 0.0, 0.0)) + pos[i])
        samples.append({bone: (r, p)})
    set_bones(clip, bake_bones(samples, [bone]))


def clear_helm(anims, doc, name, base):
    """HELM: the helm stays off the shoulder shells. Where a sample sinks it more than HELM_FIT px past their rest fit, the
    shell first slides down its arm off the helm (at most HELM_SLIDE px, as the kit's shoulder plates do), then the head
    rolls away from it (at most HELM_ROLL deg), and in a hit, where the arm keys are the hit's own additions, the shell's
    arm last gives back part of its flinch (at most HELM_ARM of it)."""
    clip = anims[name]
    n = ticks_of(clip) * SUB
    additive = _geo()["F"].additive_under(name, set(anims)) is not None
    k0 = int(LOOP_FROM[name] * SUB) if name in LOOP_FROM else None
    shells = {s: s + "_shell_barn" for s in ("left", "right")}
    keep = ("head", "left_arm", "right_arm") + tuple(shells.values())
    orig = json.loads(json.dumps({b: clip["bones"].get(b) for b in keep}))
    roll = np.zeros(n + 1)
    slide = {s: np.zeros(n + 1) for s in shells}
    cut = {s: np.zeros(n + 1) for s in shells}
    for _ in range(HELM_ITER):
        frames = _frames(doc, name, base, n)
        over = {s: np.array([_pair_fit(fr, "head", sh) - _rest_pair("head", sh) - HELM_FIT for fr in frames])
                for s, sh in shells.items()}
        if max(o.max() for o in over.values()) <= HELM_TOL:
            break                 # clear (on a second pass: at once)
        if os.environ.get("RR_DEBUG"):
            print("  helm", name, "over", round(max(o.max() for o in over.values()), 2),
                  "slide", round(max(v.max() for v in slide.values()), 2), "roll", round(float(np.abs(roll).max()), 1),
                  "cut", round(max(c.max() for c in cut.values()), 2))
        for s, sign in (("left", -1.0), ("right", 1.0)):
            o = np.maximum(over[s], 0.0)
            if not o.any():
                continue
            ds = np.minimum(o, HELM_SLIDE - slide[s])
            slide[s] = slide[s] + _spread(ds)
            o = np.maximum(o - ds, 0.0)
            dr = np.minimum(HELM_GAIN * o, HELM_ROLL - np.abs(roll))
            roll += sign * _spread(dr)
            o = np.maximum(o - dr / HELM_GAIN, 0.0)
            if additive:
                cut[s] = np.minimum(cut[s] + _spread(HELM_ARM_GAIN * o), HELM_ARM)
            slide[s] = np.minimum(slide[s], HELM_SLIDE)
            slide[s] = _loop_safe(slide[s], k0) if k0 else _ends_zero(slide[s])
        roll = np.clip(roll, -HELM_ROLL, HELM_ROLL)
        roll = _loop_safe_signed(roll, k0) if k0 else _ends_zero(roll)
        for b, ch in orig.items():   # rebuild from the clip's own keys plus the curves so far
            if ch is None:
                clip["bones"].pop(b, None)
            else:
                clip["bones"][b] = json.loads(json.dumps(ch))
        z = np.zeros(n + 1)
        if roll.any():
            _add_channel(clip, "head", n, rot=np.stack([z, z, roll], axis=1))
        for s, sh in shells.items():
            if slide[s].any():
                _add_channel(clip, sh, n, pos=np.stack([z, -slide[s], z], axis=1))
            if cut[s].any():
                c = _ends_zero(cut[s])
                rots = np.array([sample(clip, i / SUB)[s + "_arm"][0] for i in range(n + 1)])
                _add_channel(clip, s + "_arm", n, rot=-c[:, None] * rots)
    return clip


def stance_clear(anims, doc):
    """STANCE: the ready stance's free bracer sat 1.69 px in the chest on every tick (focus_check limb_in_body); the attacks
    start and end on this pose (HANDOFF), where the scorecard's attack limit is 1.39 px. The free upper arm turns about its
    own length (elbow out) by the least whole degrees that bring it to STANCE_FREE px; every key keeps its time."""
    g = _geo()
    F, P = g["F"], g["P"]
    clip = anims["stance_ready_upper"]
    n = ticks_of(clip) * SUB
    fore = P.idx(["left_forearm", "left_hand"])
    body = P.idx(["torso", "hips", "head", "right_arm", "right_forearm"])

    def depth():
        return max(F._depth(fr, P, fore, body)[0] for fr in _frames(doc, "stance_ready_upper", ["stance_ready_legs"], n)[::2])
    keys = clip["bones"]["left_arm"]["rotation"]
    orig = {t: list(v) for t, v in keys.items()}
    turn = 0
    while depth() > STANCE_FREE and turn > -STANCE_TURN_MAX:
        turn -= 1
        for t, v in orig.items():
            keys[t] = [v[0], round(v[1] + turn, 3), v[2]]


def lift_floor(anims, doc, legs, comps):
    """FLOOR: where a sample puts a hard piece more than FLOOR_KEEP px under the ground (judged on every clip in comps,
    (upper, base) pairs that play over `legs`), the hips rise by what it needs."""
    clip = anims[legs]
    n = ticks_of(clip) * SUB
    k0 = int(LOOP_FROM["stun_upper"] * SUB) if legs == "stun_legs" else None
    need = np.zeros(n + 1)
    for name, base in comps:
        m = ticks_of(anims[name]) * SUB
        d = np.array([_floor_depth(fr) - FLOOR_KEEP for fr in _frames(doc, name, base, m)])
        need[:m + 1] = np.maximum(need[:m + 1], d)
    need = np.maximum(need, 0.0)
    if need.max() < FLOOR_TOL:
        return                    # nothing under the ground (an earlier pass lifted it)
    lift = _spread(need)
    lift = _loop_safe(lift, k0) if k0 else _ends_zero(lift)
    _add_channel(clip, "hips", n, pos=np.stack([np.zeros(n + 1), lift, np.zeros(n + 1)], axis=1))


# ---------------------------------------------------------------------------------------------- main
def apply(doc):
    anims = doc["animations"]
    stance_clear(anims, doc)
    for name in SETTLE:
        settle(anims[name])
    for name in SETTLE:
        handoff(anims, name)
    for name in ROLL_WINDOW:
        roll(anims[name], name)
    for name in HAND_EASE:
        hand_ease(anims[name], name)
    for name, base in HELM:
        clear_helm(anims, doc, name, base)
    for legs, comps in FLOOR:
        lift_floor(anims, doc, legs, comps)
    legs = []
    for name in LIFE:
        ln = life(anims, name)
        if ln:
            legs.append(ln)
    for name in SASH_CLIPS:
        sash(anims[name], loop=True, fresh=True)
    for name in legs:
        sash(anims[name], loop=False)
    return doc


def dumps(doc):
    return json.dumps(doc, indent=1)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--anim", default=str(ASSET))
    ap.add_argument("--out")
    a = ap.parse_args()
    src = Path(a.anim)
    doc = json.loads(src.read_text(encoding="utf-8"))
    apply(doc)
    dst = Path(a.out) if a.out else src
    # the shipped file is the troop's finished clip file: out/ keeps the same bytes, so export.py ships it unchanged
    for d in [dst] + ([OUT] if not a.out and src.resolve() == ASSET.resolve() else []):
        with open(d, "w", newline="\n", encoding="utf-8") as fh:
            fh.write(dumps(doc))
        print("wrote", d)


if __name__ == "__main__":
    main()
