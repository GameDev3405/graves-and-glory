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


# ---------------------------------------------------------------------------------------------- main
def apply(doc):
    anims = doc["animations"]
    for name in SETTLE:
        settle(anims[name])
    for name in ROLL_WINDOW:
        roll(anims[name], name)
    for name in HAND_EASE:
        hand_ease(anims[name], name)
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


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--anim", default=str(ASSET))
    ap.add_argument("--out")
    a = ap.parse_args()
    src = Path(a.anim)
    doc = json.loads(src.read_text(encoding="utf-8"))
    apply(doc)
    dst = Path(a.out) if a.out else src
    with open(dst, "w", newline="\n", encoding="utf-8") as fh:
        fh.write(json.dumps(doc, indent=1))
    print("wrote", dst)


if __name__ == "__main__":
    main()
