"""The spear trio's focus pass (fix round 2, 2026-11-12): clears focus_check's armour_clip rows by measuring exactly what
focus_check measures (tools/anim/focus_check.py: the clip played over its base clips, an additive clip added onto the
ready stance, every cube pair on different limbs against its rest-model fit) and moving one piece the least amount.

The builder's own passes (plate_clear, head_clear, cloth_clear) judge with a coarser lattice and over the guard's legs
instead of the clip's own, so they read 1.6-1.7 px where focus_check reads 2.0-2.9 px and left those clips alone. This
pass is used for what they missed. Each fix names a clip, a bone and one channel axis:

  const   one offset for the whole clip: a held stance's pose change (the sash end swung off the lead greave, the
          pauldron set off the helm); the smallest that clears every tick
  eased   per sample, the smallest offset that clears that sample (tried in steps, both ways unless a sign is given),
          widened a tick each side, smoothed, and (a clip that plays once) faded in over a tick and out over two, so it
          starts and ends where its stance has it; a loop wraps

The offset is added to that bone's channel in the clip (re-keyed at quarter ticks); every other bone and channel of the
clip, and every other clip, stays byte for byte as it was."""
import json
import math
import os
import sys
from pathlib import Path

import numpy as np

_REPO = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(_REPO / "tools" / "anim"))
sys.path.insert(0, str(_REPO / "art" / "anim" / "common"))
import focus_check as fc  # noqa: E402
import render_geo as rg  # noqa: E402
import scorecard as sc  # noqa: E402
import stances as STN  # noqa: E402

TARGET = 1.75       # px past the rest fit a pair may keep (focus_check armour_clip warns at 1.7, fails at 1.9)
SUBS = 4            # samples per tick (focus_check STEP 0.25)
_CTX = {}


def _ctx(troop):
    if troop not in _CTX:
        geo = "%s/geo/troop/%s.geo.json" % (sc.ASSETS, troop)
        model = rg.load_geo(os.path.join(fc.REPO, geo))
        rig = sc.Rig(model, sc.rig_rules_for(fc.REPO, geo), troop)
        P = fc.Parts(rig)
        bo = rig.model.cube_bone
        rest = fc._fits(rg.Scene(rig.model, None, [], 0.0).evaluate(0.0), P, bo)
        _CTX[troop] = (rig, P, bo, rest, sc.booth_config_for(fc.REPO, troop))
    return _CTX[troop]


def _scene(doc, troop, clip):
    """The clip as focus_check plays it (check_clip): over its base clips, an additive one onto the ready stance."""
    rig, _, _, _, config = _ctx(troop)
    units = {n: (b, s) for n, b, s in sc.units(list(doc["animations"]), config.clips if config else None)}
    base, speed = units.get(clip, ((), 0.0))
    under = fc.additive_under(clip, set(doc["animations"]))
    clips = [rg.load_clip(doc, b) for b in base] + [rg.load_clip(doc, clip)]
    top = clips[-1]
    if under:
        hold, add, hand = rg.additive_split(clip, list(base), under)
        held = rg.Scene(rig.model, None, [rg.load_clip(doc, u) for u in hold], 0.0)
        scene = rg.Scene(rig.model, None, held.clips, 0.0, None, [rg.load_clip(doc, x) for x in add],
                         (rg.load_clips_if_present(doc, hand), rg.STUN_OUT_TICKS, rg.STUN_ENTRY_TICKS))
    else:
        scene = rg.Scene(rig.model, None, clips, (speed or 0.0) if sc.clip_kind(clip) == "gait" else 0.0)
    n = max(1, int(math.floor(top.length / fc.STEP + 1e-6)))
    ts = [min(i * fc.STEP, top.length - (1e-4 if top.loop == "once" else 0.0)) for i in range(n + 1)]
    return scene, ts, top


def _under(rig, root):
    out = set()
    for n in rig.model.order:
        q = n
        while q is not None and q != root:
            q = rig.model.bones[q].parent
        if q == root:
            out.add(n)
    return out


def _gap(troop, fr, involve):
    """The deepest pair touching `involve` (bones) past its rest fit, pairs on one limb excluded (focus_check)."""
    rig, P, bo, rest, _ = _ctx(troop)
    best = (0.0, None)
    for pair, v in fc._fits(fr, P, bo).items():
        if pair[0] not in involve and pair[1] not in involve:
            continue
        la, lb = fc._limb_root(rig.full, pair[0]), fc._limb_root(rig.full, pair[1])
        if la is not None and la == lb:
            continue
        g = v - rest.get(pair, 0.0)
        if g > best[0]:
            best = (g, pair)
    return best


def _limb(troop, fr, involve=None):
    """focus_check limb_in_body: the deepest forearm or hand inside the torso, hips, head or the other arm's upper arm and
    forearm (`involve`, if given, a side: "left" or "right")."""
    P = _ctx(troop)[1]
    best = (0.0, None)
    for side in ((involve,) if involve in ("left", "right") else ("right", "left")):
        o = "left" if side == "right" else "right"
        d, pair = fc._depth(fr, P, P.idx([side + "_forearm", side + "_hand"]),
                            P.idx(["torso", "hips", "head", o + "_arm", o + "_forearm"]))
        if d > best[0]:
            best = (d, tuple(_ctx(troop)[2][i] for i in pair) if pair else None)
    return best


def _units(chan, v):
    v = np.asarray(v, float)
    return np.array([math.radians(-v[0]), math.radians(-v[1]), math.radians(v[2])]) if chan == "rotation" else v


def measure(doc, troop, clip, bone, chan="rotation", vec=None, idx=None, involve=None, metric="armour"):
    """{sample: (px, pair)} of the deepest pair touching the bone's subtree (or `involve`), with `vec` (JSON units)
    added to the bone's channel."""
    rig = _ctx(troop)[0]
    scene, ts, top = _scene(doc, troop, clip)
    inv = involve if involve and metric != "limb" else _under(rig, bone)
    if vec is not None:
        orig, add = scene._pose, _units(chan, vec)

        def pose(t):
            p = orig(t)
            d = p.setdefault(bone, {})
            d[chan] = np.asarray(d.get(chan, np.ones(3) if chan == "scale" else np.zeros(3)), float) + add
            return p
        scene._pose = pose
    gap = (lambda fr: _limb(troop, fr, involve)) if metric == "limb" else (lambda fr: _gap(troop, fr, inv))
    return {i: gap(scene.evaluate(ts[i])) for i in (idx if idx is not None else range(len(ts)))}, ts, top


def _need(doc, troop, clip, bone, chan, axis, signs, step, reach, target, const, involve, metric="armour"):
    g0, ts, top = measure(doc, troop, clip, bone, chan, involve=involve, metric=metric)
    bad = [i for i, g in g0.items() if g[0] > target]
    W = np.zeros(len(ts))
    if not bad:
        return W, g0, ts, top
    left, best = set(bad), {i: (g0[i][0], 0.0) for i in bad}
    k = 1
    while left and k * step <= reach + 1e-9:
        for s in signs:
            vec = np.zeros(3)
            vec[axis] = s * k * step
            gs = measure(doc, troop, clip, bone, chan, vec, idx=bad if const else sorted(left), involve=involve,
                         metric=metric)[0]
            if const:
                if max(g[0] for g in gs.values()) <= target:
                    W[:] = vec[axis]
                    return W, g0, ts, top
                worst = max(g[0] for g in gs.values())
                if worst < best[bad[0]][0] - 0.05:
                    best = {i: (worst, vec[axis]) for i in bad}
                continue
            for i, g in gs.items():
                if g[0] <= target and i in left:
                    W[i] = vec[axis]
                    left.discard(i)
                elif i in left and g[0] < best[i][0] - 0.05:
                    best[i] = (g[0], vec[axis])
        k += 1
    if const:
        W[:] = best[bad[0]][1]
        return W, g0, ts, top
    for i in left:
        W[i] = best[i][1]
    return W, g0, ts, top


def _ease(W, loop):
    n = len(W)
    idx = (lambda j: j % (n - 1)) if loop else (lambda j: min(max(j, 0), n - 1))
    wide = np.array([max([W[idx(j)] for j in range(i - SUBS, i + SUBS + 1)], key=abs) for i in range(n)])
    out = np.array([np.mean([wide[idx(j)] for j in range(i - SUBS // 2, i + SUBS // 2 + 1)]) for i in range(n)])
    if not loop:
        sm = lambda x: x * x * (3.0 - 2.0 * x)  # noqa: E731
        out *= np.array([sm(max(0.0, min(1.0, i / float(SUBS), (n - 1 - i) / (2.0 * SUBS)))) for i in range(n)])
    return out


def _write(clip, bone, chan, axis, W):
    n = len(W) - 1
    frames = []
    for i in range(n + 1):
        v = STN.sample(clip, bone, chan, i / (SUBS * 20.0)) if bone in clip["bones"] else None
        v = np.array(v if v is not None else (np.ones(3) if chan == "scale" else np.zeros(3)), float)
        v[axis] += W[i]
        frames.append({bone: (v, None) if chan != "position" else (None, v)})
    ent = STN.bake(frames, n // SUBS, clip.get("loop", False))["bones"][bone]
    clip["bones"].setdefault(bone, {})[chan] = ent["rotation" if chan == "scale" else chan]


def fix(doc, troop, clip, bone, chan="rotation", axis=0, signs=(1, -1), step=2.0, reach=20.0, const=False,
        target=TARGET, involve=None, metric="armour", always=False):
    """One fix (see the module doc). Returns (before px, after px) of the bone's deepest pair, or None if it did not need one."""
    A = doc["animations"]
    if clip not in A:
        return None
    W, g0, ts, top = _need(doc, troop, clip, bone, chan, axis, signs, step, reach, target, const, involve, metric)
    if not W.any():
        return None
    loop = top.loop == "loop"
    if not const:
        W = _ease(W, loop)
    old = json.loads(json.dumps(A[clip]["bones"].get(bone, {}).get(chan)))
    _write(A[clip], bone, chan, axis, W)
    g1 = measure(doc, troop, clip, bone, chan, involve=involve, metric=metric)[0]
    b0, b1 = max(g0.values(), key=lambda g: g[0]), max(g1.values(), key=lambda g: g[0])
    if b1[0] > b0[0] - 0.02 and not always:     # it did not help: the clip keeps what it had (the least amount is none)
        if old is None:
            A[clip]["bones"][bone].pop(chan, None)
        else:
            A[clip]["bones"][bone][chan] = old
        print("  focus %s: %s %s[%d] no help (%.2f -> %.2f px), left as it was" % (clip, bone, chan, axis, b0[0], b1[0]))
        return None
    k = int(np.argmax(np.abs(W)))
    print("  focus %s: %s %s[%d] %+.1f%s (most at t%.2f): %.2f -> %.2f px (%s)" % (
        clip, bone, chan, axis, W[k], " held" if const else "", ts[k], b0[0], b1[0], "/".join(b1[1] or ("-",))))
    return b0[0], b1[0]


def run(doc, troop, fixes, only=None):
    """Applies the troop's fixes in order: each is a dict of fix()'s arguments with "clips" (a list of clip names)."""
    for f in fixes:
        kw = {k: v for k, v in f.items() if k != "clips"}
        for c in f["clips"]:
            if only is None or c in only:
                fix(doc, troop, c, **kw)


def swing(doc, clip, bone, axis=0, amp=6.0, cycles=1.5):
    """A hanging piece that stands still in an additive clip (focus_check secondary_life): it swings amp deg either way,
    `cycles` times over the clip, from rest and back to rest (sin envelope), on top of what it had."""
    A = doc["animations"]
    if clip not in A:
        return
    c = A[clip]
    n = int(round(float(c["animation_length"]) * 20.0)) * SUBS
    W = np.array([amp * math.sin(math.pi * i / n) * math.sin(2.0 * math.pi * cycles * i / n) for i in range(n + 1)])
    _write(c, bone, "rotation", axis, W)
    print("  focus %s: %s swings %.0f deg (secondary life)" % (clip, bone, amp))


def smooth(doc, clip, bone, passes=2):
    """A spring piece that overshoots in one sample (focus_check pop): its rotation is smoothed [1 2 1]/4 over the quarter
    ticks, the clip's first and last samples kept."""
    A = doc["animations"]
    if clip not in A or bone not in A[clip]["bones"] or "rotation" not in A[clip]["bones"][bone]:
        return
    c = A[clip]
    n = int(round(float(c["animation_length"]) * 20.0)) * SUBS
    v = np.array([STN.sample(c, bone, "rotation", i / (SUBS * 20.0)) for i in range(n + 1)], float)
    s = v.copy()
    for _ in range(passes):
        s[1:-1] = 0.25 * s[:-2] + 0.5 * s[1:-1] + 0.25 * s[2:]
    _write_abs(c, bone, s)
    print("  focus %s: %s smoothed (pop)" % (clip, bone))


def _write_abs(clip, bone, vals):
    n = len(vals) - 1
    frames = [{bone: (np.array(vals[i], float), None)} for i in range(n + 1)]
    clip["bones"][bone]["rotation"] = STN.bake(frames, n // SUBS, clip.get("loop", False))["bones"][bone]["rotation"]
