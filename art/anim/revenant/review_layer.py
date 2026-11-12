"""Batch review 2026-11-11: the revenant's late layer over clips the knight builder and the shared reaction module make for it.

python art/anim/revenant/review_layer.py [--anim <file>]      (default: the exported revenant.animation.json, rewritten in place)

The attacks and hits come from art/anim/knight/build_anims.py (shared with knight, sentinel, gl_marshal, tc_raider and
gb_hollowking: a plain rebuild of it also drifts reviewed clips, so those clips are spliced, never rebuilt); the stun, the
knockback and the pulled hit come from art/anim/common/reactions.py, which every biped shares. Neither is this troop's to change,
so the revenant post-processes its own copies here, after the builder, stances.py, reactions.py and the export, the way a
full-body layer does (references/pipeline.md 4b). Re-running any of those puts the old clip back: run this last, and once
(it adds to what it finds: a second run on its own output doubles the offsets).

  * The whole unit is alive (Kort 2026-11-11: "i want the whole unit to be animated including pieces of its armor"): every
    hanging piece (half cape, tabards, tassets) swings on a spring behind its parent and the shoulder plates trail their arms
    (art/anim/common/fullbody.soft_motion). Attacks: the pieces go where the kit puts them (upper or _legs clip). Hits are
    additive (HitController adds them to the stance the troop stands in): every piece goes in the upper clip as an offset, and
    the spring sees the ready legs plus the hit's footwork. The stun's loop (ticks 4 to 24) stays cut safe.
  * The hung, cocked head (the revenant's stance character) is thrown into its own left pauldron and shoulder shard by the
    heavier hits: through the hit the head lifts and straightens a little, in step with the blow (HEAD_COUNTER).
  * hit_knockback and hit_pulled, as the game plays them over the ready stance, sank the planted boot up to 2.2 px under the
    ground as the hips dropped: the hips ride higher over those ticks (LIFT, px by tick).
  * The shoulder-driven slash's edge roll snaps on and off in one sample (attack_overhead t11.5, attack_low_cut t8.75: a pop of
    19 and 20 degrees on the sword hand). The roll eases over the window instead (EASE).
  * The front tabard flap gives way to the lifting right thigh (PUSH); the cape and clasp spring slower (TUNE).
  * The stun (shared) sank the right boot 1.8 px and sat the coif in the right shoulder spikes: hips lift, head offset
    (STUN_LIFT, STUN_HEAD), handed back over stun_out_upper.

Only the revenant: any other troop exits without writing.
"""
from __future__ import annotations

import argparse
import json
import math
import os
import sys
from pathlib import Path

import numpy as np

os.environ.setdefault("GG_ANIM_TROOP", "revenant")
HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(ROOT / "art/anim/knight"))
sys.path.insert(0, str(ROOT / "art/anim/common"))
import build_anims as B  # noqa: E402
import fullbody  # noqa: E402
import stances as S  # noqa: E402

ASSET = ROOT / "src/main/resources/assets/gravesandglory/animations/troop/revenant.animation.json"

ATTACKS = ("attack_thrust", "attack_overhead", "attack_low_cut")
HITS = ("hit_medium_front", "hit_medium_back", "hit_medium_right", "hit_medium_left", "hit_block", "hit_knockback", "hit_pulled")
# head offset at the hit's peak (pitch, yaw, roll; - pitch lifts, - roll tips it off the left shoulder), scaled by the hit's
# own torso envelope (0 at rest, 1 at the clip's largest torso offset)
HEAD_COUNTER = {"hit_medium_back": (-8.0, 0.0, -8.0), "hit_medium_left": (-6.0, 4.0, -12.0), "hit_knockback": (-6.0, 0.0, -8.0),
                "hit_block": (-4.0, 0.0, -6.0)}
# hips lift (px) by tick, linear between keys, over the additive footwork
LIFT = {"hit_knockback": [(0, 0.0), (1, 1.2), (3, 1.4), (5, 1.6), (7, 1.7), (8, 2.0), (10, 2.3), (12, 1.9), (14, 1.5),
                          (16, 1.0), (18, 0.4), (20, 0.0)],
        "hit_pulled": [(0, 0.0), (1, 1.4), (2, 1.2), (4, 1.7), (5, 1.8), (6, 0.9), (7, 1.0), (9, 2.0), (10, 1.9), (11, 1.3), (12, 0.6),
                       (13, 0.2), (14, 0.0)]}
# (clip, bone, from tick, to tick): the bone's rotation eases from its pose at `from` to its pose at `to` (geodesic, smooth)
EASE = [("attack_overhead", "right_hand", 11.25, 12.25), ("attack_low_cut", "right_hand", 8.5, 9.5)]
# per-piece spring settings over fullbody's measured ones (fullbody.SOFT_TUNED is shared: these stay here). The long front
# tabard flap may swing 30 degrees, toward the right cuisse on every attack; the short cape and its clasp snapped on the
# hits' first sharp ticks (a 15 to 21 degree pop): slower, better damped. The half cape swung onto the left boot at the
# overhead's deep step (2.0 px, t7): it swings at most 8 degrees.
TUNE = {"tabard_f1": dict(max=12.0, gain=0.9), "tabard_f0": dict(max=16.0), "half_cape": dict(max=8.0),
        "cape": dict(period=4.5, zeta=0.5, gain=0.9), "cape_clasp": dict(period=4.5, zeta=0.5, gain=0.9)}
# the front flap hangs plumb while the right thigh swings up through it (right_cuisse into tabard_f1, 1.9 px before any
# spring): the flap gives way to the thigh, PUSH degrees (pitch) of flap per degree the leg lifts past PUSH_FROM.
PUSH, PUSH_FROM = -0.35, 25.0
PUSH_HITS = ("hit_medium_right",)    # the one hit that lifts the right thigh into the flap (a push in the others sends it into the left)
# stun_upper (shared reactions.py) plays the stun over this troop's hung, cocked head: the coif sat in the right shoulder
# spikes the whole stun. A steady head offset (pitch, yaw, roll) over the whole clip; it keeps the loop cut safe.
STUN_HEAD = (-6.0, 0.0, 8.0)
# stun_legs (shared) sinks the right boot up to 1.8 px under the ground: the hips ride STUN_LIFT px higher the whole clip
STUN_LIFT = 1.0
# the stun loops from STUN_ENTRY in game (StanceController), but the scorecard measures the wrap from its last tick to tick
# 0, where the pieces are still at rest: the cape and clasp swing a little less through the stun so that gap stays in limit
STUN_TUNE = {"cape": dict(gain=0.75), "cape_clasp": dict(gain=0.75)}


def samples_of(clip):
    n = int(round(clip["animation_length"] * S.TPS)) * S.SUB
    return [{b: (tuple(float(v) for v in r), None if p is None else tuple(float(v) for v in p))
             for b, (r, p) in S.pose_at(clip, i / (S.SUB * S.TPS)).items()} for i in range(n + 1)]


def bump(f, b, d_rot=(0.0, 0.0, 0.0), d_pos=None):
    r, p = f.get(b, ((0.0, 0.0, 0.0), None))
    if d_pos is not None:
        p = tuple(a + c for a, c in zip(p or (0.0, 0.0, 0.0), d_pos))
    f[b] = (tuple(a + c for a, c in zip(r, d_rot)), p)


def add_pose(base, delta):
    """Absolute frames: a pose plus the clip's additive offsets."""
    out = []
    for f in delta:
        g = {b: (tuple(r), p) for b, (r, p) in base.items()}
        for b, (r, p) in f.items():
            r0, p0 = g.get(b, ((0.0, 0.0, 0.0), None))
            pp = None if p is None and p0 is None else tuple(a + c for a, c in zip(p0 or (0, 0, 0), p or (0, 0, 0)))
            g[b] = (tuple(a + c for a, c in zip(r0, r)), pp)
        out.append(g)
    return out


def soft(upper, legs, all_upper=False, extra=None):
    parts = fullbody.soft_parts_for()
    for tune in (TUNE, extra or {}):
        for k, over in tune.items():
            if k in parts:
                parts[k].update(over)
    if all_upper:
        for c in parts.values():
            c.pop("clip", None)
    return fullbody.soft_motion(upper, [dict(s) for s in legs], parts=parts, report=True)


def push_flap(upper, legs, frm=None):
    """legs: the right leg's pitch source; frm: the lift it pushes from (default: PUSH_FROM, or past the first sample's
    lift, so the clip starts and ends with the flap where the stance has it)."""
    pitch = [-float(l.get("right_leg", ((0.0, 0.0, 0.0), None))[0][0]) for l in legs]
    frm = max(PUSH_FROM, pitch[0], pitch[-1]) if frm is None else frm
    for u, p in zip(upper, pitch):
        lift = max(0.0, p - frm)
        if lift > 0.0:
            bump(u, "tabard_f1", (-PUSH * lift, 0.0, 0.0))
    return upper


def loop_safe(upper, bones, start):
    """Carries the pieces' swing so the last sample equals sample `start` (a loop from there is cut safe)."""
    n = len(upper) - 1
    for b in bones:
        if b not in upper[start] or b not in upper[n]:
            continue
        r0, p0 = upper[start][b]
        r1, p1 = upper[n][b]
        for i in range(start, n + 1):
            w = (i - start) / float(n - start)
            r, p = upper[i][b]
            r = tuple(a + w * (c - e) for a, c, e in zip(r, r0, r1))
            if p is not None or p0 is not None or p1 is not None:
                z = (0.0, 0.0, 0.0)
                p = tuple(a + w * (c - e) for a, c, e in zip(p or z, p0 or z, p1 or z))
            upper[i] = dict(upper[i], **{b: (r, p)})
    return upper


def ease(frames, bone, t0, t1):
    i0, i1 = int(round(t0 * S.SUB)), int(round(t1 * S.SUB))
    a, b = np.array(frames[i0][bone][0], float), np.array(frames[i1][bone][0], float)
    for i in range(i0 + 1, i1):
        k = B.smooth((i - i0) / float(i1 - i0))
        frames[i] = dict(frames[i], **{bone: (tuple(float(v) for v in S.slerp(a, b, k)), frames[i][bone][1])})


def lerp_keys(keys, t):
    ts, vs = [k for k, _ in keys], [v for _, v in keys]
    return float(np.interp(t, ts, vs))


def rebake(old, frames):
    clip = S.bake(frames, int(round(old["animation_length"] * S.TPS)), old.get("loop", False))
    for k, v in old.items():
        if k not in ("bones", "animation_length", "loop"):
            clip[k] = v
    return clip


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--anim", default=str(ASSET), help="animation file to rewrite in place (default: the exported one)")
    a = ap.parse_args()
    if B.TROOP != "revenant":
        print("review_layer: revenant only, not", B.TROOP)
        return
    path = Path(a.anim)
    doc = json.loads(path.read_text(encoding="utf-8"))
    A = doc["animations"]
    ready_legs = {b: (tuple(float(v) for v in r), None if p is None else tuple(float(v) for v in p))
                  for b, (r, p) in S.pose_at(A["stance_ready_legs"], 0.0).items()}
    new = {}

    for name in ATTACKS:
        up, lg = samples_of(A[name]), samples_of(A[name + "_legs"])
        for c, bone, t0, t1 in EASE:
            if c == name:
                ease(up, bone, t0, t1)
        print(name)
        up, lg = soft(up, lg)
        new[name], new[name + "_legs"] = push_flap(up, lg), lg

    for name in HITS:
        up, lg = samples_of(A[name]), samples_of(A[name + "_legs"])
        if name in LIFT:
            for i, f in enumerate(lg):
                bump(f, "hips", d_pos=(0.0, lerp_keys(LIFT[name], i / S.SUB), 0.0))
            new[name + "_legs"] = lg
        if name in HEAD_COUNTER:
            env = [float(np.linalg.norm(f.get("torso", ((0, 0, 0), None))[0])) for f in up]
            peak = max(env) or 1.0
            for f, e in zip(up, env):
                bump(f, "head", tuple(v * e / peak for v in HEAD_COUNTER[name]))
        print(name)
        up, _ = soft(up, add_pose(ready_legs, lg), all_upper=True)
        new[name] = push_flap(up, lg, frm=0.0) if name in PUSH_HITS else up     # additive: the leg's own lift over the stance

    up, sl = samples_of(A["stun_upper"]), samples_of(A["stun_legs"])
    for f in sl:
        bump(f, "hips", d_pos=(0.0, STUN_LIFT, 0.0))
    new["stun_legs"] = sl
    for f in up:
        bump(f, "head", STUN_HEAD)
    print("stun_upper")
    up, _ = soft(up, sl, all_upper=True, extra=STUN_TUNE)
    bones = sorted({b for c in fullbody.soft_parts_for().values() for b in c["bones"]} | set(fullbody.plates_for()))
    import reactions as RX  # noqa: E402
    new["stun_upper"] = loop_safe(up, bones, RX.STUN_ENTRY * S.SUB)
    up = samples_of(A["stun_out_upper"])
    for i, f in enumerate(up):                # the stun's head offset hands back to the stance over the clip
        bump(f, "head", tuple(v * (1.0 - B.smooth(i / float(len(up) - 1))) for v in STUN_HEAD))
    last = sl[-1]
    print("stun_out_upper")
    new["stun_out_upper"], _ = soft(up, [last] * len(up), all_upper=True)

    for name, frames in new.items():
        A[name] = rebake(A[name], frames)
    with open(path, "w", newline="\n") as fh:      # as stances.merge writes it
        fh.write(json.dumps(doc, indent=1))
    print("review_layer revenant: %s -> %s" % (", ".join(new), path))


if __name__ == "__main__":
    main()
