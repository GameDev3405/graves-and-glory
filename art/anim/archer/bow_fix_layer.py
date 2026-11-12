"""Bow troops' late fix layer (bow batch review, fix round 1, 2026-11-12): per-troop corrections to clips this family does not
build alone, applied to the troop's own copy only. The shared modules (art/anim/common/reactions.py, charge.py) and the knight
builder stay as they are for every other family.

python art/anim/archer/bow_fix_layer.py <troop> [--src FILE] [--out FILE]
python art/anim/common/export.py <troop>

Build order for archer, longbow, bonebow, wm_hunter: build_bow_anims.py (spliced per the batch record) -> reactions.py -> charge.py
-> THIS LAYER -> export.py. Re-running reactions.py or charge.py puts the shared clip back; run this layer again after it.

Safe to run twice: each clip the layer writes is fingerprinted in out/<troop>.fixlayer.json; a clip that still matches its
fingerprint is left alone, a clip that changed (a fresh build put the base back) gets the fix again. Every other clip in the
file stays byte for byte as it was (clips are replaced in place, the file is written as the builders write it).

Operations, sampled four a tick on the clip's own timeline and baked back with stances.bake (the same thinning as the builders):
  rot   (clip, bone, (x, y, z), window)          adds degrees to a bone's rotation over a smooth window
  pos   (clip, bone, (x, y, z), window)          adds px to a bone's position (hips y+ lifts the body)
  breath(clip, {bone: (x, y, z)}, cycles)        a closed sine over a loop: a held pose that still breathes
  soft  (upper, legs, mode)                      fullbody.soft_motion over the pair, so every piece that hangs swings
                                                 mode "add": the clips are additive (hits): judged over stance_ready and
                                                 written back as deltas; "stun": absolute, with the 4..24 tick loop closed
window = (t0, t1, t2, t3) in ticks: eases in over t0..t1, holds, eases out over t2..t3.
"""
import argparse, hashlib, json, math, os, sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
ASSET = ROOT / "src/main/resources/assets/gravesandglory/animations/troop"
SUB, TPS = 4, 20


def _smooth(x):
    x = min(1.0, max(0.0, x))
    return x * x * (3 - 2 * x)


def window(t, w):
    t0, t1, t2, t3 = w
    if t < t0 or t > t3 or (t == t0 and t1 > t0) or (t == t3 and t3 > t2):
        return 0.0
    if t < t1:
        return _smooth((t - t0) / (t1 - t0))
    if t <= t2:
        return 1.0
    return 1.0 - _smooth((t - t2) / (t3 - t2))


# ---------------------------------------------------------------------------------------------- the fixes, per troop
# The hips lift (A1, B5, L5, W2): the shared pulled, stun and knockback footing bends the knees and drops the hips while the toes
# drag, which puts this rig's feet (and the hunter's longer hooves) under the ground. The hips come up by the depth, only while the
# knees are bent. Gear (A2, B6, L6, W3): soft_motion on the four shared reaction clips. Head (A3, L4): the knockback whips the head
# forward into the string-side quiver at tick 3; it pitches less there.
def _common(lift_pulled, lift_stun, lift_kb, head_kb=(8.0, 0.0, 0.0), kb_bow_arm=(0.0, 0.0, 0.0), kb_quiver=None,
            kb_tune=None, out_tune=None, stun_tune=None, jolt=(), stun_jolt=6.0, out_jolt=()):
    """lift_*: px the hips come up (lift_stun: (snap, dazed loop)); head_kb, kb_bow_arm: degrees added in the knockback;
    kb_quiver: (bone, degrees) added to a quiver after its swing; *_tune: soft_motion overrides per piece; jolt: the hip gear
    the stun's snap shakes (the legs pieces the shake-off plays over: the shake-off clip may not key them itself); out_jolt:
    (piece, degrees) the shake-off's shrug throws, on top of their swing."""
    ops = [
        ("pos", "hit_pulled_legs", "hips", (0.0, lift_pulled, 0.0), (0.0, 0.75, 10.5, 13.5)),
        ("pos", "stun_legs", "hips", (0.0, lift_stun[0], 0.0), (0.0, 0.0, 2.5, 4.0)),      # the snap
        ("pos", "stun_legs", "hips", (0.0, lift_stun[1], 0.0), (0.0, 0.0, 24.0, 24.0)),    # the dazed loop (closed: constant)
        ("pos", "hit_knockback_legs", "hips", (0.0, lift_kb, 0.0), (0.0, 1.0, 10.0, 16.0)),
        ("rot", "hit_knockback", "head", head_kb, (1.5, 2.5, 3.5, 5.5)),
        ("rot", "hit_knockback", "left_arm", kb_bow_arm, (0.0, 1.0, 7.0, 10.0)),
        ("soft", "hit_pulled", "hit_pulled_legs", "add"),
        ("soft", "hit_knockback", "hit_knockback_legs", "add", {"tune": kb_tune or {}}),
        ("soft", "stun_out_upper", None, "add", {"legs_base": "stun_legs", "tune": out_tune or {}}),
        ("soft", "stun_upper", "stun_legs", "stun", {"tune": stun_tune or {}}),
    ]
    if kb_quiver:      # after the swing: the quiver kept off the stumbling leg
        ops.append(("rot", "hit_knockback", kb_quiver[0], kb_quiver[1], (0.5, 2.5, 11.0, 15.0)))
    ops += [("rot", "stun_legs", b, (stun_jolt, 0.0, 0.0), (0.25, 1.25, 2.5, 4.0)) for b in jolt]
    ops += [("rot", "stun_out_upper", b, d, (0.25, 1.5, 2.5, 5.0)) for b, d in out_jolt]
    return ops


def _run_skirt(side, deg, w=(0.5, 1.5, 2.5, 3.5)):
    """run_upper / charge_run_upper armour_clip: the run legs swing the thigh's skirt up into the jerkin's own skirt at tick 2
    (the left thigh forward) and 5.5 (the right); the skirt trails the thigh by a few degrees there (all three tiers, one hinge)."""
    return [("rot", "run_legs", f"{side}_skirt_t{n}", (deg, 0.0, 0.0), w) for n in (1, 2, 3)]


_SKIRT_OUT = {"right_skirt_t1": {"lim": ((0.0, 6.0), (0.0, 0.0), (0.0, 10.0))}}   # swings back and out, never into the jerkin skirt
CHARGE_OUT = 24.0     # wm_hunter charge_run_upper: the string arm hangs out from the jerkin all through the stride
FIXES = {
    "archer": _run_skirt("left", 9.0) + _run_skirt("right", 9.0, (4.0, 5.0, 6.0, 7.0))
    + _common(1.9, (1.3, 0.6), 1.7, kb_bow_arm=(0.0, 0.0, -9.0), kb_quiver=("quiver", (0.0, 0.0, -8.0)),
              kb_tune={"quiver": {"plumb": 0.0, "max": 6.0}}, out_tune=_SKIRT_OUT, stun_tune=_SKIRT_OUT,
              jolt=[f"{s}_skirt_t{n}" for s in ("left", "right") for n in (1, 2, 3)] + ["pouch", "signal_horn"]),
    # W1: the rally call. The string arm went up past the snout and the bracer hid the face (2.22 into the snout): the arm opens
    # out and a little lower, the forearm unfolds, so the fist cups beside the snout, and the head lifts and turns off the arm,
    # calling over the bow shoulder. W4: the charge run's string forearm swings into the bulky jerkin at the stride (2.50); the
    # arm swings out there.
    "wm_hunter": [("rot", "stance_rally_upper", "right_arm", (10.0, 0.0, 20.0), (3.0, 6.0, 13.0, 16.0)),
                  ("rot", "stance_rally_upper", "right_forearm", (15.0, 0.0, 0.0), (3.0, 6.0, 13.0, 16.0)),
                  ("rot", "stance_rally_upper", "head", (-10.0, -12.0, 0.0), (3.0, 6.0, 13.0, 16.0)),
                  ("rot", "charge_run_upper", "right_arm", (0.0, 0.0, CHARGE_OUT), (0.0, 0.0, 7.0, 7.0)),
                  # death_forward: the bow arm ended raised 108 deg behind the back, sticking up off the corpse; it lies lower
                  ("rot", "death_forward", "left_arm", (-25.0, 0.0, 0.0), (14.0, 17.0, 20.0, 20.0))]
    + _common(2.9, (1.2, 0.6), 2.0, head_kb=(0.0, 0.0, 0.0),
                         out_jolt=[("cloak", (5.0, 0.0, 0.0)), ("pauldron", (5.0, 0.0, 4.0)), ("tusk_necklace", (5.0, 0.0, 0.0))]),
    # L2/L3: the knight's hit legs dip the hips and the tall stave (4.3 px longer each way than the archer's) puts its lower
    # tip under the ground: the bow arm lifts through the dip. Run: the padded jack rides the thigh with its skirt.
    "longbow": _run_skirt("left", 9.0) + _run_skirt("right", 9.0, (4.0, 5.0, 6.0, 7.0))
    + [("rot", "run_legs", "left_jack", (9.0, 0.0, 0.0), (0.5, 1.5, 2.5, 3.5)),
       ("rot", "run_legs", "right_jack", (9.0, 0.0, 0.0), (4.0, 5.0, 6.0, 7.0)),
       ("rot", "hit_medium_right", "left_arm", (-8.0, 0.0, 0.0), (0.0, 1.0, 4.0, 9.0))]
    + _common(2.0, (1.3, 0.6), 1.7, kb_bow_arm=(-12.0, 0.0, -4.0), out_tune=_SKIRT_OUT, stun_tune=_SKIRT_OUT,
              jolt=[f"{s}_skirt_t{n}" for s in ("left", "right") for n in (1, 2, 3)] + ["pouch"]),
}


# ---------------------------------------------------------------------------------------------- sampling and baking
def _load_kit(troop):
    os.environ["GG_ANIM_TROOP"] = troop
    sys.path.insert(0, str(HERE.parent / "common"))
    import stances as S
    import fullbody as FB
    return S, FB


def frames(S, clip):
    ticks = round(clip["animation_length"] * TPS)
    out = []
    for i in range(ticks * SUB + 1):
        p = S.pose_at(clip, i / (SUB * TPS))
        out.append({b: [np.array(r, float), None if q is None else np.array(q, float)] for b, (r, q) in p.items()})
    return out, ticks


def rebake(S, clip, fr, ticks):
    baked = S.bake([{b: (tuple(r), None if q is None else tuple(q)) for b, (r, q) in f.items()} for f in fr], ticks, clip["loop"])
    out = {}
    for k, v in clip.items():
        out[k] = baked["bones"] if k == "bones" else v
    return out


def op_add(fr, bone, d, w, chan):
    for i, f in enumerate(fr):
        k = window(i / SUB, w)
        r, q = f.setdefault(bone, [np.zeros(3), None])
        if k == 0.0:
            continue
        if chan == 0:
            f[bone][0] = r + k * np.array(d, float)
        else:
            f[bone][1] = (np.zeros(3) if q is None else q) + k * np.array(d, float)


def op_breath(fr, amp, cycles):
    n = len(fr) - 1
    for i, f in enumerate(fr):
        s = math.sin(2 * math.pi * cycles * i / n)
        for b, d in amp.items():
            f[b][0] = f[b][0] + s * np.array(d, float)


def _lowpass(seq, bones):
    for b in bones:
        rots = np.array([x[b][0] for x in seq], dtype=float)
        for _ in range(2):
            pad = np.vstack([rots[:1], rots[:1], rots, rots[-1:], rots[-1:]])
            sm = (pad[:-4] + 2 * pad[1:-3] + 3 * pad[2:-2] + 2 * pad[3:-1] + pad[4:]) / 9.0
            sm[0], sm[-1] = rots[0], rots[-1]
            rots = sm
        for x, r in zip(seq, rots):
            x[b] = (tuple(float(v) for v in r), x[b][1])


def op_soft(S, FB, anims, up_fr, lg_fr, mode, opts=None):
    """soft_motion over the pair; returns the new (upper, legs) frames. Additive clips swing over the stance and go back as
    deltas; the stun's loop (ticks 4..24) is closed by spreading the seam's gap over the loop."""
    opts = opts or {}
    n = len(up_fr)
    alone = lg_fr is None
    if alone:
        lg_fr = [{} for _ in range(n)]
    if mode == "add":
        ru = S.pose_at(anims["stance_ready_upper"], 0.0)
        rl = S.pose_at(anims["stance_ready_legs"], 0.0)
    else:
        ru, rl = {}, {}
    parts = FB.soft_parts_for()
    for piece, over in opts.get("tune", {}).items():
        parts[piece].update(over)
    soft_names = {b for c in parts.values() for b in c["bones"]} | set(FB.plates_for())

    def absolute(fr, ready):
        out = []
        for f in fr:
            x = {}
            for b in set(f) | set(ready):
                r0, q0 = ready.get(b, (np.zeros(3), None))
                r1, q1 = f.get(b, (np.zeros(3), None))
                q = None if (q0 is None and q1 is None) else (np.zeros(3) if q0 is None else np.array(q0)) + (np.zeros(3) if q1 is None else np.array(q1))
                x[b] = (tuple(float(v) for v in np.array(r0) + np.array(r1)), None if q is None else tuple(float(v) for v in q))
            out.append(x)
        return out

    al = absolute(lg_fr, rl)
    if opts.get("legs_base"):           # an upper-only clip that the game adds over another legs clip (the shake-off over the stun)
        lb = anims[opts["legs_base"]]
        for i, x in enumerate(al):
            for b, (r, q) in S.pose_at(lb, i / (SUB * TPS)).items():
                x[b] = (tuple(float(v) for v in r), None if q is None else tuple(float(v) for v in q))
    su, sl = FB.soft_motion(absolute(up_fr, ru), al, parts=parts)
    out_u, out_l = [dict(f) for f in up_fr], [dict(f) for f in lg_fr]
    moved = set()
    # An upper clip alone (the stun shake-off) keeps its upper pieces only: its legs-side pieces belong to the legs clip it plays
    # over (scorecard: clips played together animate disjoint bones), which carries their swing.
    for seq, ready, out in ((su, ru, out_u),) + (() if alone else ((sl, rl, out_l),)):
        pieces = {b for x in seq for b in x} & soft_names
        _lowpass(seq, pieces)
        moved |= pieces
        for i, x in enumerate(seq):
            for b in pieces:
                r, q = x[b]
                r0, q0 = ready.get(b, (np.zeros(3), None))
                r = np.array(r, float) - np.array(r0, float)
                q = None if q is None else np.array(q, float) - (np.zeros(3) if q0 is None else np.array(q0, float))
                out[i][b] = [r, q]
    if mode == "stun":
        a = 4 * SUB
        for out in (out_u, out_l):
            for b in moved & set(out[0]):
                for ch in (0, 1):
                    if out[a][b][ch] is None or out[-1][b][ch] is None:
                        continue
                    gap = np.array(out[a][b][ch]) - np.array(out[-1][b][ch])
                    for i in range(a, n):
                        out[i][b][ch] = np.array(out[i][b][ch]) + gap * (i - a) / (n - 1 - a)
    return out_u, (None if alone else out_l)


def apply(troop, anims):
    S, FB = _load_kit(troop)
    work, ticks = {}, {}

    def fr(name):
        if name not in work:
            work[name], ticks[name] = frames(S, anims[name])
        return work[name]

    for op in FIXES.get(troop, []):
        kind = op[0]
        if kind == "rot":
            op_add(fr(op[1]), op[2], op[3], op[4], 0)
        elif kind == "pos":
            op_add(fr(op[1]), op[2], op[3], op[4], 1)
        elif kind == "breath":
            op_breath(fr(op[1]), op[2], op[3])
        elif kind == "soft":
            u = fr(op[1])
            lg = fr(op[2]) if op[2] else None
            nu, nl = op_soft(S, FB, anims, u, lg, op[3], op[4] if len(op) > 4 else None)
            work[op[1]] = nu
            if op[2]:
                work[op[2]] = nl
    return {n: rebake(S, anims[n], f, ticks[n]) for n, f in work.items()}


def _sha(clip):
    return hashlib.sha256(json.dumps(clip, sort_keys=True).encode("utf-8")).hexdigest()[:16]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("troop")
    ap.add_argument("--src", help="animation file to read (default: the troop's out/ file)")
    ap.add_argument("--out", help="write here instead of out/ (a scratch copy; the fingerprints are not updated)")
    ap.add_argument("--force", action="store_true", help="apply even where the fingerprint matches (scratch use only)")
    a = ap.parse_args()
    t = a.troop
    out_dir = HERE.parent / t / "out"
    src = Path(a.src) if a.src else out_dir / f"{t}.animation.json"
    doc = json.loads(src.read_text(encoding="utf-8"))
    anims = doc["animations"]
    lock_path = out_dir / f"{t}.fixlayer.json"
    lock = json.loads(lock_path.read_text(encoding="utf-8")) if lock_path.is_file() else {}
    names = {op[i] for op in FIXES.get(t, []) for i in ((1, 2) if op[0] == "soft" else (1,)) if op[i]}
    done = {n for n in names if not a.force and lock.get(n) == _sha(anims[n])}
    if done == names:
        print(t, "fix layer already applied to", len(names), "clips; nothing to do")
        return
    if done:
        sys.exit(f"{t}: {', '.join(sorted(done))} already fixed but not the rest: rebuild the base clips (builder, reactions.py, "
                 "charge.py) and run the layer again")
    base = anims
    new = apply(t, base)
    for n, c in new.items():
        anims[n] = c                       # in place: the clip order of the file is kept
    text = json.dumps(doc, indent=1)
    dst = Path(a.out) if a.out else src
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(text.encode("utf-8"))
    if not a.out:
        lock.update({n: _sha(c) for n, c in new.items()})
        lock_path.write_text(json.dumps(lock, indent=1, sort_keys=True) + "\n", encoding="utf-8")
    print(t, "fix layer:", ", ".join(sorted(new)), "->", dst)


if __name__ == "__main__":
    main()
