"""Batch review 2026-11-11: a late layer over the reaction clips of the troops built on the knight builder.

GG_ANIM_TROOP=sentinel python art/anim/knight/soft_layers.py [--anim <file>]

The stun, the knockback and the pulled hit are built by art/anim/common/reactions.py for every biped (it merges them into the
exported animation file). That shared module is not this family's to change, so the knight family post-processes its own
troops' copies here, after reactions.py and the export, the way a full-body layer does (references/pipeline.md 4b):

  * the whole unit is alive (Kort 2026-11-11: "i want the whole unit to be animated including pieces of its armor"): every
    hanging piece (cape, tabard, skirt plates) swings on a spring behind its parent and the shoulder plates trail their arms
    (art/anim/common/fullbody.soft_motion). All of it goes in the upper clip; the legs clips are not touched. The stun's loop
    (ticks 4 to 24) stays cut safe: the swing is carried so tick 24 equals tick 4.
  * hit_pulled, as the game plays it (HitController adds it on top of the ready stance): the arms' forward fling carried the
    blade across the visor (1.98 px into the helm at t3.5, 3.7 px at the heavy scale 1.55) and the dragged toes went 2.5 px
    under the ground (t1). The sword arm carries the fist wider of the helm through the fling (arm z -16, x +6 at full
    fling), the body is yanked up 2.6 px off its dragging feet and they point their toes 10 degrees less.
  * hit_knockback: the shove and the stumble put the planted right boot 1.5 px under the ground (t2, t5.5, t14.75) as the
    hips dropped: the hips sink 1.7 px less over the shove and each stumbling step, and the braced foot's toe lifts 10
    degrees through the recovery.
  * hit_pulled at the heavy scale (1.55): the skirt plates' swing is capped at 5 degrees so the front plate does not climb
    into the tabard skirt. Values were swept over the composed clip at k 1 and 1.55 (work/review/try_layer.sh).

Only the troops in LAYERS change; any other troop exits without writing. Re-running reactions.py puts the shared clips back,
so run this after it (and after export.py). Rebuilding with build_anims.py does not touch these clips.
"""
from __future__ import annotations

import argparse
import json
import math
import os
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parent / "common"))
TROOP = os.environ.get("GG_ANIM_TROOP", "knight")
import build_anims as B  # noqa: E402
import fullbody  # noqa: E402
import stances as S  # noqa: E402
import reactions as RX  # noqa: E402

ASSET = ROOT / "src/main/resources/assets/gravesandglory/animations/troop" / f"{TROOP}.animation.json"

# per troop: which clips get the soft pieces, and the fixes (amounts at full strength, degrees)
LAYERS = {
    "sentinel": dict(soft=("stun_upper", "stun_out_upper", "hit_knockback", "hit_pulled"),
                     pull_arm=(6.0, 0.0, -16.0), pull_toe=10.0, pull_lift=2.6, pull_step_lift=2.0,
                     pull_tune={n: dict(max=5.0) for n in ("right_skirt_t1", "left_skirt_t1")},
                     kb_lift=1.7, kb_heel=-10.0, kb_early=1.0,
                     kb_tune={"cape": dict(max=14.0)}),
    # the marshal: the same reactions with the same faults as played (blade 2.4 px into the head at t2, 3.6 at 1.55; toe
    # 2.5 px under the ground; the knockback's planted boot 1.6 px under), so the same fixes; its skirt plates are one piece a side
    "gl_marshal": dict(soft=("stun_upper", "stun_out_upper", "hit_knockback", "hit_pulled"),
                       pull_arm=(6.0, 0.0, -16.0), pull_toe=10.0, pull_lift=2.6, pull_step_lift=2.0,
                       pull_tune=dict({n: dict(max=5.0) for n in ("right_skirt", "left_skirt")},
                                      cape=dict(inward=0.0, max=8.0), cape_t2=dict(inward=0.0, max=8.0),
                                      fur_collar=dict(gain=1.0, max=6.0, zeta=0.5)),
                       kb_lift=1.7, kb_heel=-10.0, kb_early=1.0,
                       kb_tune={"cape": dict(max=14.0), "fur_collar": dict(gain=1.0, max=6.0, zeta=0.5)},
                       stun_tune={"fur_collar": dict(gain=1.0, max=6.0, zeta=0.5)},
                       stun_out_tune={n: dict(gain=2.0) for n in ("cape", "cape_t2", "left_skirt", "right_skirt")}),
    # the knight (APPROVED): hit_pulled only, the clip added after the approval. As the game plays it the fling put the
    # blade 1.7 px into the great helm at t3.5 (3.5 px at the heavy scale 1.55) and the dragged toe 2.5 px under the ground;
    # the same arm, lift and toe fix as the sentinel clears both, and the cape and skirt plates swing. Every other knight
    # clip stays byte-identical (no other key here).
    "knight": dict(soft=("hit_pulled",), pull_arm=(6.0, 0.0, -16.0), pull_toe=10.0, pull_lift=2.6, pull_step_lift=2.0,
                   pull_tune={"cape": dict(gain=2.5)}),
}


def samples_of(clip):
    n = int(round(clip["animation_length"] * S.TPS)) * S.SUB
    return [{b: (tuple(float(v) for v in r), None if p is None else tuple(float(v) for v in p))
             for b, (r, p) in S.pose_at(clip, i / (S.SUB * S.TPS)).items()} for i in range(n + 1)]


def bump(f, b, d_rot):
    r, p = f.get(b, ((0.0, 0.0, 0.0), None))
    f[b] = (tuple(a + c for a, c in zip(r, d_rot)), p)


def add_legs(base, delta):
    """Absolute legs for the spring: the ready footing plus the clip's additive footwork."""
    out = []
    for f in delta:
        g = {b: (tuple(r), p) for b, (r, p) in base.items()}
        for b, (r, p) in f.items():
            r0, p0 = g.get(b, ((0.0, 0.0, 0.0), None))
            pp = None if p is None and p0 is None else tuple(a + c for a, c in zip(p0 or (0, 0, 0), p or (0, 0, 0)))
            g[b] = (tuple(a + c for a, c in zip(r0, r)), pp)
        out.append(g)
    return out


PLATE_EASE = 0.45    # a shoulder plate takes this share of its remaining lag each sample (no one-sample flick on an arm's snap)


def soft_upper(upper, legs, tune=None):
    parts = fullbody.soft_parts_for()
    for name, c in parts.items():
        c.pop("clip", None)
        c.update((tune or {}).get(name, {}))
    up, _ = fullbody.soft_motion(upper, [dict(s) for s in legs], parts=parts, report=True)
    # the kit's plate lag (fullbody.PLATE_LAG) reaches its 10 degrees in one sample when an arm is flung (the pulled hit's
    # fling turns the arm 14 degrees in a quarter tick): ease it in so the plate is never a one-sample pop
    for plate, (arm, _f) in fullbody.plates_for().items():
        if plate not in up[0]:
            continue
        prev = np.array(up[0][plate][0], dtype=float)
        for i in range(1, len(up)):
            goal = np.array(up[i][plate][0], dtype=float)
            prev = goal if i == len(up) - 1 else prev + PLATE_EASE * (goal - prev)
            up[i] = dict(up[i], **{plate: (tuple(float(v) for v in prev), up[i][plate][1])})
    return up, sorted({b for c in parts.values() for b in c["bones"]} | set(fullbody.plates_for()))


def loop_safe(upper, bones, start):
    """Carries the pieces' swing so the last sample equals sample `start` (a loop from there is cut safe)."""
    n = len(upper) - 1
    for b in bones:
        if b not in upper[start]:
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


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--anim", default=str(ASSET), help="animation file to rewrite in place (default: the exported one)")
    a = ap.parse_args()
    cfg = LAYERS.get(TROOP)
    if cfg is None:
        print("soft_layers: nothing for", TROOP)
        return
    path = Path(a.anim)
    doc = json.loads(path.read_text(encoding="utf-8"))
    A = doc["animations"]
    ready_legs = {b: (tuple(float(v) for v in r), None if p is None else tuple(float(v) for v in p))
                  for b, (r, p) in S.pose_at(A["stance_ready_legs"], 0.0).items()}
    new, legs_changed = {}, {}

    # hit_pulled: the fixes first, then the pieces
    if "pull_arm" in cfg:
        up, lg = samples_of(A["hit_pulled"]), samples_of(A["hit_pulled_legs"])
        for i, (u, l) in enumerate(zip(up, lg)):
            t = i / S.SUB
            bump(u, "right_arm", tuple(RX.pull_env_arms(t) * v for v in cfg["pull_arm"]))
            drag = RX.smooth(t / 1.5) * (1 - RX.smooth((t - 3.5) / 4.0))     # reactions.build_pulled_legs's drag
            steps = max(RX.pulse(t, 3.5, 6.5, 10.0), RX.pulse(t, 7.0, 9.5, 12.5))     # the catching steps
            r, p = l["hips"]; p = p or (0.0, 0.0, 0.0)
            l["hips"] = (r, (p[0], p[1] + cfg["pull_lift"] * drag + cfg["pull_step_lift"] * steps, p[2]))
            for side in ("left", "right"):
                bump(l, side + "_foot", (-cfg["pull_toe"] * drag, 0.0, 0.0))
                if side + "_boot" in l:
                    bump(l, side + "_boot", (-cfg["pull_toe"] * drag, 0.0, 0.0))
        legs_changed["hit_pulled_legs"] = lg
        if "hit_pulled" in cfg["soft"]:
            up, _ = soft_upper(up, add_legs(ready_legs, lg), tune=cfg.get("pull_tune"))
        new["hit_pulled"] = up

    # hit_knockback: the footing fixes, then the pieces
    if "kb_lift" in cfg:
        up, lg = samples_of(A["hit_knockback"]), samples_of(A["hit_knockback_legs"])
        for i, l in enumerate(lg):
            t = i / S.SUB
            # the shove's first sink and both stumbling steps: the planted boot sank as the hips dropped
            st = max(cfg["kb_early"] * RX.pulse(t, 0.5, 2.0, 4.0), RX.pulse(t, 2.5, 5.5, 9.0), RX.pulse(t, 7.0, 10.0, 18.0))
            r, p = l["hips"]; p = p or (0.0, 0.0, 0.0)
            l["hips"] = (r, (p[0], p[1] + cfg["kb_lift"] * st, p[2]))
            hold = RX.smooth(t / 2.0) * (1 - RX.smooth((t - 12) / 8.0))      # reactions.build_knockback_legs's stagger
            heel = cfg["kb_heel"] * hold * RX.smooth((t - 10.0) / 3.0)        # the recovery: the braced foot's toe lifts
            bump(l, "right_foot", (heel, 0.0, 0.0))                             # (foot x- is toe up) so it stays above the ground
            if "right_boot" in l:
                bump(l, "right_boot", (heel, 0.0, 0.0))
        legs_changed["hit_knockback_legs"] = lg
        if "hit_knockback" in cfg["soft"]:
            up, _ = soft_upper(up, add_legs(ready_legs, lg), tune=cfg.get("kb_tune"))
        new["hit_knockback"] = up

    # the stun (absolute, over stun_legs) and its shake-off (additive, over the stun's last pose)
    if "stun_upper" in cfg["soft"]:
        up, sl = samples_of(A["stun_upper"]), samples_of(A["stun_legs"])
        up, bones = soft_upper(up, sl, tune=cfg.get("stun_tune"))
        new["stun_upper"] = loop_safe(up, bones, RX.STUN_ENTRY * S.SUB)
    if "stun_out_upper" in cfg["soft"]:
        up = samples_of(A["stun_out_upper"])
        last = samples_of(A["stun_legs"])[-1]
        new["stun_out_upper"], _ = soft_upper(up, [last] * len(up), tune=cfg.get("stun_out_tune"))

    for name, frames in list(new.items()) + list(legs_changed.items()):
        old = A[name]
        clip = S.bake(frames, int(round(old["animation_length"] * S.TPS)), old.get("loop", False))
        for k, v in old.items():
            if k not in ("bones", "animation_length", "loop"):
                clip[k] = v
        A[name] = clip
    with open(path, "w", newline="\n") as fh:      # as stances.merge writes it
        fh.write(json.dumps(doc, indent=1))
    print("soft_layers %s: %s -> %s" % (TROOP, ", ".join(list(new) + list(legs_changed)), path))


if __name__ == "__main__":
    main()
