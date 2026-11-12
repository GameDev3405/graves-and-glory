"""ic_hearthguard only: its hit and stun clips, built by the shared builders and then fitted to this dwarf.

    python art/anim/axe/hearthguard_hits.py [--out FILE] [--only CLIP,CLIP]

Review fix round 1 (2026-11-12). The hits (hit_light_*, hit_medium_*, hit_block: the knight builder's, run through
build_axe_anims exactly as the troop's export builds them) and the reactions (stun_upper / stun_legs, stun_out_upper,
hit_knockback, hit_pulled: common/reactions.py) are rebuilt from their own builders, sample for sample, and then, for this
troop only:
  1. FIXES: small hann-weighted offsets where the focus check found the axe in the helm, a boot or knee guard under the
     ground, or the head in a shoulder boss (played over stance_ready, as the game plays a hit);
  2. fullbody.soft_motion over the clip ADDED onto stance_ready (the stance the dwarf stands in when a blow lands): the braid,
     the tabard, the tasset plates and both pauldrons trail the body, as they do in the attacks. The stance keys none of
     them, so the trailing pieces are written into the additive clip as they are. stun_upper loops ticks 4..24, so its
     trailing pieces are run over three passes of the loop and the middle pass is used (the loop closes exactly).
Only these clips (and their _legs) are merged back into the exported file (stances.merge): every other clip stays byte for
byte as it was, and the knight's, the sentinel's and every other troop's files are not read or written.
"""
from __future__ import annotations

import argparse
import math
import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
os.environ["GG_AXE_TROOP"] = "ic_hearthguard"
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parent / "common"))
import build_axe_anims as A  # noqa: E402
import stances as S  # noqa: E402
import reactions as R  # noqa: E402

K, fullbody, np = A.K, A.fullbody, A.np
TROOP = "ic_hearthguard"
SUB = K.SUB
assert A.TROOP == TROOP and S.SUB == SUB

K_HITS = ["hit_light_front", "hit_light_back", "hit_light_right", "hit_light_left", "hit_medium_front", "hit_medium_back",
          "hit_medium_right", "hit_medium_left", "hit_block"]
REACTIONS = ["stun_upper", "stun_out_upper", "hit_knockback", "hit_pulled"]
LEGS_OF = {"stun_upper": "stun_legs", "stun_out_upper": None}

# (bone, rot deg, pos px or None, (rise from, peak, fall to) in ticks): added to the clip's samples with a smooth pulse
# ((None, None, None): all through the clip).
# Additive clips: offsets on top of the shared clip; stun_upper / stun_legs: offsets on the absolute pose.
ALL = (None, None, None)
FIXES = {
    # The yank flung the axe arm up and forward and the haft swung through the helm (2.41 px, t2.25): the arm is flung
    # 25 deg less. The dragged feet put the right boot 2.3 px under the ground at t1 and again on the catch step (t4, t9.75):
    # the hips ride up to 2.5 px higher through the drag and 1.4 px through the catch.
    "hit_pulled": [("right_arm", (25.0, 0.0, 0.0), None, (0.5, 3.0, 9.0))],
    "hit_pulled_legs": [("hips", None, (0.0, 2.5, 0.0), (0.0, 1.5, 7.5)), ("hips", None, (0.0, 1.4, 0.0), (5.0, 9.5, 13.5))],
    # The snap threw the head back into the right pauldron's boss (3.6 px, t1): the axe arm flares 25 deg less, the head is
    # thrown back 14 deg less and turned 10 deg off the boss; through the daze the arm hangs 6 deg further in and the head
    # turns 4 deg further off it, and the shield arm rides 8 deg higher (its rim touched the ground at t8). The buckled knees
    # put the rear knee guard 1.9 px under the ground (t0.75) and 1.4 px through the dazed dip: the hips ride 2.2 px higher
    # through the snap and 0.6 px higher all through the loop.
    "stun_upper": [("right_arm", (0.0, 0.0, -25.0), None, (-1.0, 0.75, 3.5)), ("head", (14.0, -10.0, 0.0), None, (-1.0, 0.75, 3.5)),
                   ("left_arm", (-8.0, 0.0, 0.0), None, ALL), ("right_arm", (0.0, 0.0, -6.0), None, ALL),
                   ("head", (0.0, -4.0, 0.0), None, ALL)],
    "stun_legs": [("hips", None, (0.0, 2.2, 0.0), (-1.0, 0.75, 3.5)), ("hips", None, (0.0, 0.6, 0.0), ALL)],
    # The flung axe hand flicked the haft back onto the helm (1.64 px, t1.5): the wrist flicks 18 deg less. The two stumbling
    # steps put the boots and the right knee guard up to 1.9 px under the ground (t1-3, t5.5, t10, t12-15): the hips ride
    # higher through each.
    "hit_knockback": [("right_hand", (18.0, 0.0, 0.0), None, (0.0, 1.5, 5.0))],
    "hit_knockback_legs": [("hips", None, (0.0, 1.0, 0.0), (0.0, 2.0, 4.5)), ("hips", None, (0.0, 2.0, 0.0), (3.0, 5.5, 8.0)),
                           ("hips", None, (0.0, 2.0, 0.0), (7.5, 10.0, 12.5)), ("hips", None, (0.0, 0.6, 0.0), (1.0, 6.0, 17.0)),
                           ("hips", None, (0.0, 0.8, 0.0), (10.5, 13.5, 17.5))],
}
# Pieces left out of the soft pass per clip. None: the medium hits keep the tabard (left still, the check fails it as
# lifeless), and swing it off the thigh instead (TABARD).
SOFT_SKIP = {}
# (t0, t1, deg): the tabard swung forward about its top edge off the leading thigh (as the attacks' follow-through).
TABARD = {
    "hit_knockback": [(0.0, 3.5, 12.0), (6.0, 10.0, 8.0)],
    "hit_medium_right": [(0.0, 3.0, 10.0)],
    "stun_upper": [(7.0, 13.0, 8.0), (15.5, 20.5, 6.0)],      # inside the loop (4..24), so it still closes
}


def _pulse(t, a, b, c):
    return R.pulse(t, a, b, c)


def _capture(mod, fn):
    """Runs fn() with mod.bake recording what it was handed: ({id(clip): (samples, ticks, loop)}, fn's result)."""
    got, orig = {}, mod.bake

    def bake(samples, ticks, loop=True):
        clip = orig(samples, ticks, loop)
        got[id(clip)] = (samples, ticks, loop)
        return clip
    mod.bake = bake
    try:
        res = fn()
    finally:
        mod.bake = orig
    return got, res


def _plain(frames):
    """[{bone: (rot tuple, pos tuple|None)}] without the builder's helper keys."""
    return [{b: (tuple(float(v) for v in r), None if p is None else tuple(float(v) for v in p))
             for b, (r, p) in f.items() if not b.startswith("_")} for f in frames]


def raw_clips():
    """{clip: (samples, ticks, loop, bake)} for every clip this module rebuilds, as their own builders build them."""
    out = {}
    got, clips = _capture(K, K.hit_clips)
    for n in K_HITS:
        for m in (n, n + "_legs"):
            s, ticks, loop = got[id(clips[m])]
            out[m] = (_plain(s), ticks, loop, K.bake)
    tr = S.Troop(TROOP)
    got, clips = _capture(R, lambda: R.build_all(tr))
    for m, c in clips.items():
        s, ticks, loop = got[id(c)]
        out[m] = (_plain(s), ticks, loop, S.bake)
    return out, tr


def apply_fixes(frames, rows):
    for bone, rot, pos, (a, b, c) in rows:
        for i, f in enumerate(frames):
            w = 1.0 if a is None else _pulse(i / SUB, a, b, c)
            if w <= 0.0:
                continue
            r0, p0 = f.get(bone, ((0.0, 0.0, 0.0), None))
            r1 = tuple(x + w * d for x, d in zip(r0, rot or (0.0, 0.0, 0.0)))
            p1 = p0 if pos is None else tuple(x + w * d for x, d in zip(p0 or (0.0, 0.0, 0.0), pos))
            f[bone] = (r1, p1)


def _stance(tr):
    up, lg = tr.anims["stance_ready_upper"], tr.anims["stance_ready_legs"]

    def at(clip, t):
        t = (t / K.TPS) % clip["animation_length"]
        return {b: (tuple(float(v) for v in r), None if p is None else tuple(float(v) for v in p))
                for b, (r, p) in S.pose_at(clip, t).items()}
    return (lambda t: at(up, t)), (lambda t: at(lg, t))


def _over(frames, stance_at):
    """The additive frames added onto the stance (rotation and position offsets summed), sample by sample."""
    out = []
    for i, f in enumerate(frames):
        s = dict(stance_at(i / SUB))
        for b, (r, p) in f.items():
            r0, p0 = s.get(b, ((0.0, 0.0, 0.0), None))
            pp = None if (p is None and p0 is None) else tuple(x + y for x, y in zip(p0 or (0, 0, 0), p or (0, 0, 0)))
            s[b] = (tuple(x + y for x, y in zip(r0, r)), pp)
        out.append(s)
    return out


def _soft_parts(name):
    skip = SOFT_SKIP.get(name, ())
    return {k: v for k, v in fullbody.soft_parts_for().items() if k not in skip}


def _soft_bones(parts, plates):
    return {b for c in parts.values() for b in c["bones"]} | set(plates)


def soft(name, upper, legs, absolute, tr, loop_from=None):
    """Runs fullbody.soft_motion over the clip as the game shows it and writes the trailing pieces into upper (and legs).
    absolute: the clip is a pose (stun); else it is added onto stance_ready. loop_from: the loop's first tick (stun)."""
    st_up, st_lg = _stance(tr)
    parts, plates = _soft_parts(name), fullbody.plates_for()
    bones = _soft_bones(parts, plates)
    U = [dict(f) for f in upper] if absolute else _over(upper, st_up)
    L = [dict(f) for f in legs] if absolute else _over(legs, st_lg)
    n = len(U) - 1
    if loop_from is None:
        U, L = fullbody.soft_motion(U, L, parts=parts, plates=plates, report=True)
        pick = list(range(n + 1))
    else:
        j0 = int(round(loop_from * SUB))
        loop = list(range(j0 + 1, n + 1))
        idx = list(range(n + 1)) + loop * 3
        U, L = fullbody.soft_motion([dict(U[i]) for i in idx], [dict(L[i]) for i in idx], parts=parts, plates=plates,
                                    report=True)
        a = n + len(loop)                      # the middle pass: ext[a] is the loop's start, ext[a + len(loop)] its end
        pick = list(range(j0)) + list(range(a, a + len(loop) + 1))
    for dst, src in ((upper, U), (legs, L)):
        for f in dst:
            for b in bones:
                f.pop(b, None)
    for i, k in enumerate(pick):
        for b in bones:
            for src, dst in ((U, upper), (L, legs)):
                if b in src[k]:
                    (upper if legs is None else dst)[i][b] = src[k][b]
    if loop_from is not None:
        _close_loop(upper, legs, bones, j0)
    return upper, legs


def _close_loop(upper, legs, bones, j0):
    """The trailing pieces' loop made exact: the end eased onto the loop's first sample, the entry eased into it."""
    n = len(upper) - 1
    for clip in (upper, legs):
        for b in bones:
            if b not in clip[j0]:
                continue
            r0, p0 = (np.array(v if v is not None else (0, 0, 0), float) for v in clip[j0][b])
            rn, pn = (np.array(v if v is not None else (0, 0, 0), float) for v in clip[n][b])
            haspos = any(clip[i][b][1] is not None for i in range(n + 1))
            for i in range(j0, n + 1):         # the drift over the pass, taken out linearly
                u = (i - j0) / float(n - j0)
                r, p = (np.array(v if v is not None else (0, 0, 0), float) for v in clip[i][b])
                clip[i][b] = (tuple(r + u * (r0 - rn)), tuple(p + u * (p0 - pn)) if haspos else None)


def build(only=None):
    raw, tr = raw_clips()
    names = [n for n in K_HITS + REACTIONS if not only or n in only]
    out = {}
    for name in names:
        legs_name = LEGS_OF.get(name, name + "_legs")
        up, ticks, loop, bake = raw[name]
        lg = raw[legs_name][0] if legs_name else None
        apply_fixes(up, FIXES.get(name, ()))
        if lg is not None:
            apply_fixes(lg, FIXES.get(legs_name, ()))
        legs_for_soft = lg if lg is not None else [{} for _ in up]
        absolute = name == "stun_upper"
        if lg is None:                          # stun_out_upper: the stance's legs under it, the pieces in the upper clip
            st_up, st_lg = _stance(tr)
            legs_for_soft = [{b: ((0.0, 0.0, 0.0), None) for b in st_lg(0.0)} for _ in up]
        up, legs_for_soft = soft(name, up, legs_for_soft, absolute, tr, loop_from=R.STUN_ENTRY if name == "stun_upper" else None)
        if lg is None:                          # no legs clip: whatever soft_motion put on the legs side goes in the upper clip
            for f, g in zip(up, legs_for_soft):
                for b, v in g.items():
                    if b not in ("hips",) and not b.endswith(("_leg", "_shin", "_foot")):
                        f[b] = v
        for t0, t1, deg in TABARD.get(name, ()):
            A._push(up, "tabard", t0, t1, rot=(deg, 0.0, 0.0), pivot=(0.0, 18.0, 0.0))
        out[name] = bake(up, ticks, loop)
        if lg is not None:
            out[legs_name] = bake(legs_for_soft, ticks, raw[legs_name][2])
    return A.only_rig(out), tr


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", help="write the merged file here instead of the exported one")
    ap.add_argument("--only", help="comma list of clips (each with its _legs)")
    a = ap.parse_args()
    clips, tr = build(set(a.only.split(",")) if a.only else None)
    dst = Path(a.out) if a.out else tr.path
    S.merge(tr, clips, dst)
    print("%s: %s -> %s" % (TROOP, ", ".join(clips), dst))


if __name__ == "__main__":
    main()
