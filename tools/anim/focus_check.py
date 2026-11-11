#!/usr/bin/env python3
"""Focus check: Kort's four animation complaints, measured on every tick of every clip.

  python tools/anim/focus_check.py --troop knight [--clip attack_overhead] [--json out.json] [--ticks]
  python tools/anim/focus_check.py --troop stalker --anim path/to/other.animation.json     # score another build
  python tools/anim/focus_check.py --all

Exit 1 when any clip has a FAIL, 0 otherwise (2 on a usage error). It reads the exported assets, like the scorecard,
and poses them with render_geo's own maths. The scorecard asks "is this clip inside the approved knight's envelope";
this tool asks Kort's four questions with absolute limits and says the tick and the two parts involved, so the fix is
aimed. Both must pass before a clip is shown to anyone. A check that does not apply is n/a, never pass.

CLIP  clipping (px, 1 px = 1/16 block; depth = how far the deepest point is inside the other box)
  weapon_in_body    weapon cubes inside torso, hips, head, legs or the other arm
  weapon_in_arm     weapon cubes inside the weapon arm's own upper arm
  weapon_in_prop    weapon cubes inside a held prop (sword through the shield)
  prop_in_body      held prop (shield) inside the body or the other arm
  limb_in_body      forearm/hand inside torso, hips, head or the other arm; shin/foot inside the other leg
  floor             lowest hard cube corner under the ground
STALE stagnant or lifeless motion
  dead_hold         action clips: longest run of ticks in which nothing visible moves 0.25 px (a frozen frame)
  loop_life         looping stance/idle: the furthest any visible point travels over the loop, and how many body
                    parts move at least half a pixel (a loop under one texel is a statue at game scale)
  whole_body        attacks and hits with a legs clip: hips travel, torso turn, a real step, the off hand moving
  phases            melee attacks: a held load before the strike, a strike much faster than the rest, a follow-through
                    that carries on past the impact
FACE  weapon facing
  edge_lead         slashes: the edge leads the tip's travel on every fast sample of the arc, not only at the impact
  point_lead        thrusts: the blade lies along its own travel while it drives
  at_foe            at the impact tick the blade is in front of the hand, toward the foe
  guard_up          the ready stance: the blade does not hang below the hand
  roll_spin         the weapon spinning about its own length in the hand
  wrist             the blade folded back along the forearm, past what a wrist allows
LIMB  limbs glitching or bending the wrong way
  elbow             an elbow bending a way the shoulder could not turn it to (backward, or far out to the side)
  knee              a knee bending backward; either joint folded shut
  knee_aim          a bent knee pointing away from its own toes, or behind the hips
  shoulder          the upper arm forced up behind the back
  elbow_cross       an elbow carried across the body's midline
  feet_cross        the feet swapping sides
  head_turn         the head turned further from the chest than a neck goes
  pop               a one-sample jump in a bone's rotation (a glitch frame)
  jitter            a bone reversing direction and back within a tick
  settle            action clips that do not end on the pose they started from (snaps when the layer blends out)

Limits are absolute and written in LIMITS below with the reason for each. They were set on the approved knight (v2,
2026-11-06/07): its clips pass, apart from the baseline findings listed in tools/anim/focus_exemptions.json. Never
loosen a limit to make a clip pass; a real exception is recorded there (troop, clip, check, reason).

What it does not do: it measures geometry, not taste, and it is calibrated for the knight skeleton with a one-handed
blade. Run on the stalker Kort rejected and the one he kept (2026-11-11) it failed most clips of both and did not
tell them apart. On another rig, run it on that troop's kept clips first and read which rows are rig differences.
"""
from __future__ import annotations

import argparse
import json
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)
import render_geo as rg  # noqa: E402
import scorecard as sc  # noqa: E402

REPO = sc.REPO
EXEMPT_PATH = os.path.join(HERE, "focus_exemptions.json")
STEP = 0.25                 # ticks between samples (the builders bake four keys per tick)
PER = int(round(1 / STEP))
FRONT = np.array([0.0, 0.0, 1.0])
UP = np.array([0.0, 1.0, 0.0])
SOFT = {"cape", "cloak", "skirt", "tail", "tails", "mantle", "surcoat", "tippet", "tabard", "hair", "plume", "crest",
        "pelt", "wrap", "sash", "scarf", "kelp", "fur", "tassel", "ribbon", "banner", "hood", "cowl", "beard", "string"}
SKELETON = ("hips", "torso", "head", "right_arm", "right_forearm", "right_hand", "left_arm", "left_forearm", "left_hand",
            "right_leg", "right_shin", "right_foot", "left_leg", "left_shin", "left_foot")

# (warn, fail, reason). A value past `fail` fails the clip; past `warn` it is listed for the reviewer.
LIMITS = {
    "weapon_in_body": (0.75, 1.5, "px. Under a pixel is a graze nobody sees; 1.5 px is a blade visibly in the body"),
    "weapon_in_arm": (2.0, 3.0, "px into the weapon arm's own upper arm: the pommel passes it when the elbow folds, so more room"),
    "weapon_in_prop": (0.5, 1.0, "px. Kort 2026-11-06: 'the sword and shield clipping' was why he rejected the first falls"),
    "prop_in_body": (1.25, 2.0, "px. A shield is carried against the body; its rim brushes the thigh in a stride"),
    "limb_in_body": (1.0, 2.0, "px. Armour is modelled a pixel proud of the limb, so a pixel of overlap is contact"),
    "floor": (0.5, 1.0, "px under the ground"),
    "dead_hold": (2.0, 3.0, "ticks. A held pose still breathes; three frozen ticks read as a stuck frame"),
    "loop_life": (0.6, 0.4, "px, a minimum. Kort cut the sway to 25-40% on 2026-11-07 ('way too much sway'), so a loop is small on purpose; under 0.4 px it is a statue"),
    "edge_lead": (25.0, 40.0, "deg off the cut's line while the tip is fast"),
    "point_lead": (25.0, 40.0, "deg between the blade and the hand's travel while the hand drives forward"),
    "roll_spin": (100.0, 140.0, "deg per tick about the blade's length (the edge roll may turn 100 a tick)"),
    "wrist": (135.0, 150.0, "deg between the blade and the forearm's line (0 is a thrust in line with the arm, 90 a hammer grip, 180 folded back on the elbow)"),
    "elbow": (105.0, 130.0, "deg the upper arm would have to twist at the shoulder for the elbow to bend the way it does (180 is an elbow bending backward)"),
    "knee": (-2.0, -5.0, "deg, a minimum: negative is a knee bending backward"),
    "fold": (150.0, 160.0, "deg of elbow or knee flexion: the forearm would pass through the upper arm"),
    "knee_aim": (50.0, 70.0, "deg between a bent knee and its toes, seen from above"),
    "shoulder": (60.0, 80.0, "deg the upper arm is raised behind the back"),
    "elbow_cross": (0.5, 2.0, "px the elbow sits across the chest's midline"),
    "feet_cross": (0.0, 1.0, "px the feet have swapped sides"),
    "head_turn": (70.0, 95.0, "deg between where the head and the chest face"),
    "pop": (8.0, 14.0, "deg: one sample turning this much more than both of its neighbours"),
    "jitter": (4.0, 8.0, "deg there and back within half a tick"),
    "settle": (0.5, 2.0, "deg between the first and last pose of an action clip"),
}
MIN_FAST = 6.0      # px/tick: under this the blade is being carried, not swung, and its edge is not judged
DEAD = 0.25         # px/tick of the fastest visible point: under this the frame is frozen


class Result:
    def __init__(self, check, group):
        self.check, self.group = check, group
        self.status, self.value, self.tick, self.detail = "n/a", None, None, ""

    def set(self, value, tick=None, detail="", lower=False, limit=None):
        warn, fail, _ = limit or LIMITS[self.check]
        bad = (value < fail) if lower else (value > fail)
        soso = (value < warn) if lower else (value > warn)
        self.status = "FAIL" if bad else "WARN" if soso else "pass"
        self.value, self.tick, self.detail = round(float(value), 2), tick, detail
        return self

    def note(self, text):
        self.detail = text
        return self

    def row(self):
        return {"check": self.check, "group": self.group, "status": self.status, "value": self.value,
                "tick": self.tick, "detail": self.detail}


def _tok(n):
    return set(n.split("_"))


class Parts:
    """Which body part every visible cube belongs to, and lattices of points to test with."""

    def __init__(self, rig):
        self.rig = rig
        m = rig.model
        self.part, self.soft = [], []
        for b in m.cube_bone:
            n, soft, part = b, False, None
            while n is not None:
                if n in rig.weapon:
                    part = "weapon"
                    break
                if n in rig.props:
                    part = "prop"
                    break
                if n in SKELETON or n in sc.TORSO_BONES:
                    part = "torso" if n in sc.TORSO_BONES and n != "hips" else n
                    break
                if _tok(n) & SOFT:
                    soft = True
                n = rig.full.bones[n].parent
            self.part.append(part)
            self.soft.append(soft or part is None)
        self.lat = [sc.lattice(c) for c in m.cube_list]
        self.holder = {}        # "weapon"/"prop" -> side of the hand that holds it
        for kind, bones in (("weapon", rig.weapon), ("prop", rig.props)):
            for b in bones:
                n = b
                while n is not None and not n.endswith("_hand"):
                    n = rig.full.bones[n].parent
                if n:
                    self.holder[kind] = n.split("_")[0]
                    break

    def idx(self, parts):
        return [i for i, p in enumerate(self.part) if p in parts and not self.soft[i]]

    def hard(self):
        return [i for i, s in enumerate(self.soft) if not s]


def _depth(fr, P, movers, boxes, roots=None):
    """Deepest mover lattice point inside any box (px), and the (mover, box) cube pair it belongs to."""
    if not movers or not boxes:
        return 0.0, None
    best, pair = 0.0, None
    J = np.array(boxes)
    R = np.array([sc._rot(fr.cube_M[j]) for j in boxes])
    C = fr.corners[J]
    for i in movers:
        pts = sc.cube_points(fr.corners[i], P.lat[i])
        c = C.mean(axis=1)
        proj = np.einsum("bvc,bca->bva", C - c[:, None], R)
        half = np.abs(proj).max(axis=1)
        q = np.einsum("bpc,bca->bpa", pts[None] - c[:, None], R)
        d = (half[:, None, :] - np.abs(q)).min(axis=2)          # B x N
        k = np.unravel_index(int(np.argmax(d)), d.shape)
        if d[k] * 16.0 > best:
            best, pair = float(d[k] * 16.0), (i, boxes[k[0]])
    return best, pair


def _ang(a, b):
    na, nb = np.linalg.norm(a), np.linalg.norm(b)
    if na < 1e-9 or nb < 1e-9:
        return 0.0
    return math.degrees(math.acos(max(-1.0, min(1.0, float(a @ b) / (na * nb)))))


def _swing_twist(Rm):
    """A rotation split into a bend of the bone's long (y) axis and a turn about it: (bend deg, turn deg signed)."""
    w = math.sqrt(max(0.0, 1.0 + Rm[0, 0] + Rm[1, 1] + Rm[2, 2])) / 2.0
    y = (Rm[0, 2] - Rm[2, 0]) / (4.0 * w) if w > 1e-6 else 1.0
    turn = math.degrees(2.0 * math.atan2(y, w))
    bend = _ang(Rm @ np.array([0.0, 1.0, 0.0]), np.array([0.0, 1.0, 0.0]))
    return bend, (turn + 180.0) % 360.0 - 180.0


def _pivot(rig, fr, bone):
    b = rig.full.bones[bone]
    return (fr.bone_M[bone] @ np.append(b.pivot / 16.0, 1.0))[:3] * 16.0


def check_clip(rig, anim, name, base=(), kind=None, variant=None, speed=0.0, exempt=()):
    """All checks of one clip played over its base clips. Returns {"name", "kind", "results": [...], "ticks": {...}}."""
    kind = kind or sc.clip_kind(name)
    clips = [rg.load_clip(anim, b) for b in base] + [rg.load_clip(anim, name)]
    top = clips[-1]
    scene = rg.Scene(rig.model, None, clips, speed if kind == "gait" else 0.0)
    n = max(1, int(math.floor(top.length / STEP + 1e-6)))
    ts = [min(i * STEP, top.length - (1e-4 if top.loop == "once" else 0.0)) for i in range(n + 1)]
    frames = [scene.evaluate(t) for t in ts]
    P = Parts(rig)
    bone_of = rig.model.cube_bone
    res = {}

    def R(check, group):
        res[check] = Result(check, group)
        return res[check]

    def name_pair(pair):
        return "" if pair is None else "%s into %s" % (bone_of[pair[0]], bone_of[pair[1]])

    def worst(check, group, movers, boxes, why):
        r = R(check, group)
        if not movers or not boxes:
            return r.note(why)
        vals = [_depth(fr, P, movers, boxes) for fr in frames]
        series[check] = [v[0] for v in vals]
        k = int(np.argmax(series[check]))
        return r.set(vals[k][0], ts[k], name_pair(vals[k][1]))

    series = {}
    # ---------------------------------------------------------------- CLIP
    body = ["torso", "hips", "head", "right_leg", "right_shin", "right_foot", "left_leg", "left_shin", "left_foot"]
    arm = lambda s: [s + "_arm", s + "_forearm", s + "_hand"]  # noqa: E731
    other = lambda s: "left" if s == "right" else "right"  # noqa: E731
    w, p = P.idx(["weapon"]), P.idx(["prop"])
    ws, ps = P.holder.get("weapon"), P.holder.get("prop")
    worst("weapon_in_body", "CLIP", w, P.idx(body + (arm(other(ws)) if ws else [])), "no weapon")
    worst("weapon_in_arm", "CLIP", w, P.idx([ws + "_arm"]) if ws else [], "no weapon")
    worst("weapon_in_prop", "CLIP", w, p, "no weapon and held prop together")
    worst("prop_in_body", "CLIP", p, P.idx(body + (arm(other(ps)) + [ps + "_arm"] if ps else [])), "no held prop")
    r = R("limb_in_body", "CLIP")
    if rig.biped:
        best = (0.0, None, 0.0)
        ser = np.zeros(len(frames))
        for side in ("right", "left"):
            fore = P.idx([side + "_forearm", side + "_hand"])
            tgt = P.idx(["torso", "hips", "head"] + arm(other(side))[:2])
            leg = P.idx([side + "_shin", side + "_foot"])
            ltgt = P.idx([other(side) + "_leg", other(side) + "_shin", other(side) + "_foot"]) if side == "right" else []
            for k, fr in enumerate(frames):
                for mv, bx in ((fore, tgt), (leg, ltgt)):
                    d, pair = _depth(fr, P, mv, bx)
                    ser[k] = max(ser[k], d)
                    if d > best[0]:
                        best = (d, pair, ts[k])
        series["limb_in_body"] = ser.tolist()
        r.set(best[0], best[2], name_pair(best[1]))
    else:
        r.note("not a biped")
    r = R("floor", "CLIP")
    hard = P.hard()
    if hard:
        # A rig whose boots or foot gear hang under the sole at rest starts below the ground: only what a clip adds counts.
        rest = rg.Scene(rig.model, None, [], 0.0).evaluate(0.0)
        sole = max(0.0, -float(rest.corners[hard][:, :, 1].min()) * 16.0)
        lows = [(-float(fr.corners[hard][:, :, 1].min()) * 16.0 - sole, bone_of[hard[int(np.argmin(fr.corners[hard][:, :, 1].min(axis=1)))]])
                for fr in frames]
        series["floor"] = [max(0.0, v[0]) for v in lows]
        k = int(np.argmax(series["floor"]))
        r.set(series["floor"][k], ts[k], lows[k][1] + " under the ground" if series["floor"][k] > 0 else "")

    # ---------------------------------------------------------------- shared signals
    pts = np.array([fr.corners[hard].reshape(-1, 3) for fr in frames]) * 16.0 if hard else np.zeros((len(frames), 0, 3))
    move = np.zeros(len(frames))            # px/tick of the fastest visible point, per sample
    if len(frames) > 1 and pts.shape[1]:
        step = np.linalg.norm(pts[1:] - pts[:-1], axis=2).max(axis=1) / STEP
        move[1:] = step
        move[0] = step[0]
    series["motion"] = move.tolist()
    has_blade = rig.blade_bone is not None
    tip = grip = L = E = None
    if has_blade:
        tip = np.array([(fr.bone_M[rig.blade_bone] @ rig.tip)[:3] * 16.0 for fr in frames])
        Rb = [sc._rot(fr.bone_M[rig.blade_bone]) for fr in frames]
        L = np.array([Rm @ np.array([0.0, 1.0, 0.0]) for Rm in Rb])
        E = np.array([Rm @ np.array([rig.edge_sign or 1.0, 0.0, 0.0]) for Rm in Rb])
        hand = rig.full.bones[rig.blade_bone].parent
        grip = np.array([_pivot(rig, fr, hand) for fr in frames])
        tv = np.zeros_like(tip)
        tv[1:-1] = (tip[2:] - tip[:-2]) / (2 * STEP)
        tv[0], tv[-1] = (tip[1] - tip[0]) / STEP, (tip[-1] - tip[-2]) / STEP
        tspeed = np.linalg.norm(tv, axis=1)
        series["tip_speed"] = tspeed.tolist()
    vkind = variant.get("kind") if variant else None
    impact = float(variant["impactTicks"]) if variant else None
    ii = None if impact is None or impact > top.length else int(round(impact / STEP))

    # ---------------------------------------------------------------- STALE
    r = R("dead_hold", "STALE")
    if kind in ("attack", "hit") and len(frames) > PER:
        last = len(frames) - 1
        while last > 0 and move[last] < DEAD:       # the settle at the end is judged on its own, below
            last -= 1
        run = best = 0
        at = 0.0
        for k in range(1, last + 1):
            run = run + 1 if move[k] < DEAD else 0
            if run > best:
                best, at = run, ts[k]
        r.set(best * STEP, round(at - best * STEP, 2), "nothing moves" if best else "")
        tail = (len(frames) - 1 - last) * STEP
        if tail > 0.35 * top.length and r.status == "pass":
            r.status, r.value, r.tick = "WARN", round(tail, 2), ts[last]
            r.detail = "the clip is over by tick %.2f and stands still for its last %.2f ticks" % (ts[last], tail)
    else:
        r.note("only action clips (attack, hit)")
    r = R("loop_life", "STALE")
    if kind in ("stance", "fallback") and top.loop == "loop" and pts.shape[1]:
        ext = np.linalg.norm(pts.max(axis=0) - pts.min(axis=0), axis=1)      # per point, px
        parts = {}
        k0 = 0
        for i in hard:
            m = len(frames[0].corners[i])
            parts[P.part[i]] = max(parts.get(P.part[i], 0.0), float(ext[k0:k0 + m].max()))
            k0 += m
        alive = sorted(pp for pp, v in parts.items() if v >= 0.5)
        r.set(float(ext.max()), None, "%d of %d parts move 0.5 px or more: %s" % (len(alive), len(parts), ", ".join(alive)),
              lower=True)
    else:
        r.note("only looping stances and idle")
    r = R("whole_body", "STALE")
    legs_played = any(c.name.endswith("_legs") for c in clips[:-1])
    if kind in ("attack", "hit") and rig.biped and legs_played:
        hips = np.array([_pivot(rig, fr, "hips") for fr in frames])
        hip_go = float(np.linalg.norm(hips.max(axis=0) - hips.min(axis=0)))
        turn = max(sc._angle(sc._rot(frames[0].bone_M["torso"]), sc._rot(fr.bone_M["torso"])) for fr in frames)
        feet = [np.array([_pivot(rig, fr, s + "_foot") for fr in frames]) for s in ("right", "left")]
        step_go = max(float(np.linalg.norm(f.max(axis=0) - f.min(axis=0))) for f in feet)
        off = other(ws) if ws else "left"
        oh = np.array([np.linalg.inv(fr.bone_M["torso"])[:3, :] @ np.append(_pivot(rig, fr, off + "_hand") / 16.0, 1.0)
                       for fr in frames]) * 16.0
        off_go = float(np.linalg.norm(oh.max(axis=0) - oh.min(axis=0)))
        heavy = kind == "attack"
        need = {"hips travel": (hip_go, 2.0 if heavy else 0.5 if "light" in name else 0.75, "px"), "torso turn": (turn, 12.0 if heavy else 5.0, "deg"),
                "step": (step_go, 2.0 if heavy else 0.0, "px"), "off hand": (off_go, 1.5 if heavy else 0.0, "px")}
        miss = [k for k, (v, lim, _) in need.items() if v < lim]
        r.status = "FAIL" if len(miss) >= 2 or "hips travel" in miss else "WARN" if miss else "pass"
        r.value = round(hip_go, 2)
        r.detail = "; ".join("%s %.1f %s%s" % (k, v, u, " (needs %.1f)" % lim if v < lim else "") for k, (v, lim, u) in need.items())
    else:
        r.note("only attacks and hits played with a legs clip")
    r = R("phases", "STALE")
    if kind == "attack" and has_blade and vkind in sc.MELEE and ii is not None:
        peak = float(tspeed.max())
        kp = int(np.argmax(tspeed))
        before = tspeed[:max(1, ii - 2 * PER)]
        slow = (before < 0.3 * peak) & (np.arange(len(before)) >= PER)
        run = best = 0
        for v in slow:
            run = run + 1 if v else 0
            best = max(best, run)
        load = best * STEP
        ratio = peak / max(1e-6, float(np.median(tspeed)))
        after = min(len(frames) - 1, ii + PER)
        carry = float(tv[after] @ tv[ii]) / max(1e-6, float(tv[ii] @ tv[ii]))
        faults = []
        if load < 2.0:
            faults.append("load held %.2f ticks (needs 2)" % load)
        if ratio < 2.5:
            faults.append("strike only %.1fx the clip's usual tip speed (needs 2.5x)" % ratio)
        if abs(ts[kp] - impact) > 1.5:
            faults.append("fastest at tick %.2f, impact is %g" % (ts[kp], impact))
        if carry < 0.15:
            faults.append("stops at the impact, no follow-through")
        r.status = "FAIL" if len(faults) >= 2 else "WARN" if faults else "pass"
        r.value, r.tick = round(ratio, 2), ts[kp]
        r.detail = "; ".join(faults) or "load %.2f ticks, strike %.1fx, peak %.0f px/tick at tick %.2f" % (load, ratio, peak, ts[kp])
    else:
        r.note("only melee attacks with a design.json variant")

    # ---------------------------------------------------------------- FACE
    arc = None
    if has_blade and ii is not None:
        lo, hi = max(0, ii - 3 * PER), min(len(frames) - 1, ii + 2 * PER)
        pk = float(tspeed[lo:hi + 1].max())
        arc = [k for k in range(lo, hi + 1) if tspeed[k] >= max(MIN_FAST, 0.5 * pk)]
    r = R("edge_lead", "FACE")
    if has_blade and vkind == "slash" and arc:
        errs = []
        for k in arc:
            vp = tv[k] - (tv[k] @ L[k]) * L[k]
            th = abs(math.degrees(math.atan2(float(np.cross(E[k], vp) @ L[k]), float(E[k] @ vp))))
            errs.append(th if rig.edge_sign else min(th, 180.0 - th))
        series["edge_lead"] = dict(zip([ts[k] for k in arc], [round(e, 1) for e in errs]))
        k = int(np.argmax(errs))
        r.set(errs[k], ts[arc[k]], "judged on ticks %.2f-%.2f" % (ts[arc[0]], ts[arc[-1]]))
    else:
        r.note("only slashes with a design.json variant")
    r = R("point_lead", "FACE")
    if has_blade and vkind == "stab" and arc:
        gv = np.gradient(grip, STEP, axis=0)
        gs = np.linalg.norm(gv, axis=1)
        lo = max(0, ii - 4 * PER)
        drive = [k for k in range(lo, ii + 1) if gs[k] >= max(2.0, 0.5 * float(gs[lo:ii + 1].max())) and gv[k] @ FRONT > 0]
        if drive:
            errs = [_ang(L[k], gv[k]) for k in drive]
            series["point_lead"] = dict(zip([ts[k] for k in drive], [round(e, 1) for e in errs]))
            k = int(np.argmax(errs))
            r.set(errs[k], ts[drive[k]], "judged on ticks %.2f-%.2f" % (ts[drive[0]], ts[drive[-1]]))
        else:
            r.status, r.detail = "FAIL", "the point never drives forward before the impact"
    else:
        r.note("only thrusts with a design.json variant")
    r = R("at_foe", "FACE")
    if has_blade and vkind in sc.MELEE and ii is not None:
        ahead = float((tip[ii] - grip[ii]) @ FRONT)
        ok = ahead > 2.0 and (vkind != "stab" or float(L[ii] @ FRONT) > 0.7)
        r.status, r.value, r.tick = ("pass" if ok else "FAIL"), round(ahead, 2), impact
        r.detail = "tip %.1f px in front of the hand at the impact%s" % (ahead, "" if ok else ": the blade is not toward the foe")
    else:
        r.note("only melee attacks with a design.json variant")
    r = R("guard_up", "FACE")
    if has_blade and sc.stem(name) == "stance_ready":
        hang = (grip[:, 1] - tip[:, 1])
        k = int(np.argmax(hang))
        r.set(float(hang[k]), ts[k], "tip under the hand" if hang[k] > 0 else "", limit=(0.0, 2.0, ""))
    else:
        r.note("only the ready stance of a troop with a blade")
    r = R("roll_spin", "FACE")
    if has_blade and len(frames) > 2:
        spins = [0.0]
        for k in range(1, len(frames)):
            spins.append(abs(_swing_twist(Rb[k - 1].T @ Rb[k])[1]) / STEP)
        k = int(np.argmax(spins))
        r.set(spins[k], ts[k])
    else:
        r.note("no weapon")
    r = R("wrist", "FACE")
    if has_blade:
        hand = rig.full.bones[rig.blade_bone].parent
        fore = rig.full.bones[hand].parent
        # What the eye sees of a wrist is the blade against the forearm: square to it in a hammer grip, in line with it
        # in a thrust, never folded back along it toward the elbow.
        angs = []
        for k, fr in enumerate(frames):
            f = _pivot(rig, fr, hand) - _pivot(rig, fr, fore)
            angs.append(_ang(L[k], f))
        k = int(np.argmax(angs))
        r.set(angs[k], ts[k], "blade folded back along the forearm" if angs[k] > LIMITS["wrist"][0] else "")
    else:
        r.note("no weapon")

    # ---------------------------------------------------------------- LIMB
    r = R("elbow", "LIMB")
    if rig.biped and rig.elbows and "torso" in rig.full.bones:
        best, fold = (0.0, 0.0, ""), (0.0, 0.0, "")
        for k, fr in enumerate(frames):
            Rt = sc._rot(fr.bone_M["torso"])
            for u, lo_ in rig.elbows:
                Ru = sc._rot(fr.bone_M[u])
                v = Ru.T @ sc._rot(fr.bone_M[lo_]) @ np.array([0.0, -1.0, 0.0])
                bend = _ang(v, np.array([0.0, -1.0, 0.0]))
                if bend > fold[0]:
                    fold = (bend, ts[k], lo_ + " folded shut")
                swing, twist = _swing_twist(Rt.T @ Ru)
                if bend < 15.0 or swing > 150.0:    # a straight arm has no elbow direction; nor has one straight up
                    continue
                way = math.degrees(math.atan2(-v[0], -v[2]))        # which way the forearm folds, 0 = to the front
                eff = abs((twist + way + 180.0) % 360.0 - 180.0)
                if eff > best[0]:
                    best = (eff, ts[k], "%s bends %s" % (lo_, "backward" if eff > 150 else "out of the shoulder's reach"))
        r.set(best[0], best[1], best[2] if best[0] > LIMITS["elbow"][0] else "")
        if r.status == "pass" and fold[0] > LIMITS["fold"][0]:
            r.set(fold[0], fold[1], fold[2], limit=LIMITS["fold"])
    else:
        r.note("not a biped")
    r = R("knee", "LIMB")
    if rig.biped and rig.knees:
        vals = [(sc._flex(sc._rot(fr.bone_M[u]), sc._rot(fr.bone_M[lo_]), True), lo_, ts[k])
                for k, fr in enumerate(frames) for u, lo_ in rig.knees]
        lo_v, hi_v = min(vals), max(vals)
        r.set(lo_v[0], lo_v[2], lo_v[1] + " bends backward" if lo_v[0] < 0 else "", lower=True)
        if hi_v[0] > LIMITS["fold"][0] and r.status == "pass":
            r.set(hi_v[0], hi_v[2], hi_v[1] + " folded shut", limit=LIMITS["fold"])
    else:
        r.note("not a biped")
    biped_legs = rig.biped and all(s + "_" + b in rig.full.bones for s in ("right", "left") for b in ("leg", "shin", "foot"))
    r = R("knee_aim", "LIMB")
    if biped_legs:
        best = (0.0, 0.0, "")
        for k, fr in enumerate(frames):
            fwd = sc._rot(fr.bone_M["hips"]) @ np.array([0.0, 0.0, -1.0])
            for s in ("right", "left"):
                hip, kn, an = (_pivot(rig, fr, s + b) for b in ("_leg", "_shin", "_foot"))
                ax = an - hip
                kd = (kn - hip) - ax * float((kn - hip) @ ax) / max(1e-9, float(ax @ ax))
                if np.linalg.norm(kd) < 0.6:        # a near-straight leg has no knee direction
                    continue
                toe = sc._rot(fr.bone_M[s + "_foot"]) @ np.array([0.0, 0.0, -1.0])
                kh, th = kd * [1, 0, 1], toe * [1, 0, 1]
                if np.linalg.norm(kh) < 0.3 or np.linalg.norm(th) < 0.3:
                    continue
                a = _ang(kh, th)
                if float(kh @ (fwd * [1, 0, 1])) < -0.5 * np.linalg.norm(kh):
                    a = max(a, 90.0)                # the knee points behind the hips
                if a > best[0]:
                    best = (a, ts[k], s + " knee")
        r.set(best[0], best[1], best[2])
    else:
        r.note("not a biped")
    r = R("shoulder", "LIMB")
    r2 = R("elbow_cross", "LIMB")
    if rig.biped:
        best, cross = (0.0, 0.0, ""), (0.0, 0.0, "")
        for k, fr in enumerate(frames):
            Ti = np.linalg.inv(fr.bone_M["torso"])
            Rt = sc._rot(fr.bone_M["torso"])
            for s in ("right", "left"):
                sh, el = _pivot(rig, fr, s + "_arm"), _pivot(rig, fr, s + "_forearm")
                d = Rt.T @ (el - sh)                 # torso frame: gecko axes, front is -z, up +y
                d = d / max(1e-9, np.linalg.norm(d))
                if d[2] > 0.2 and abs(d[0]) < 0.75:  # behind the shoulder and not out to the side
                    a = math.degrees(math.atan2(d[2], -d[1]))
                    if a > best[0]:
                        best = (a, ts[k], s + " upper arm")
                ex = (Ti @ np.append(el / 16.0, 1.0))[0] * 16.0
                sx = (Ti @ np.append(sh / 16.0, 1.0))[0] * 16.0
                over = -ex * np.sign(sx)             # px past the midline, on the other side from its own shoulder
                if over > cross[0]:
                    cross = (float(over), ts[k], s + " elbow")
        r.set(best[0], best[1], best[2])
        r2.set(cross[0], cross[1], cross[2])
    else:
        r.note("not a biped")
        r2.note("not a biped")
    r = R("feet_cross", "LIMB")
    if biped_legs and kind != "death":
        best = (0.0, 0.0)
        for k, fr in enumerate(frames):
            Ti = np.linalg.inv(fr.bone_M["hips"])
            x = {s: (Ti @ np.append(_pivot(rig, fr, s + "_foot") / 16.0, 1.0))[0] * 16.0 for s in ("right", "left")}
            rest = np.sign(rig.full.bones["right_foot"].pivot[0] - rig.full.bones["left_foot"].pivot[0]) or 1.0
            over = -(x["right"] - x["left"]) * rest
            if over > best[0]:
                best = (float(over), ts[k])
        r.set(best[0], best[1])
    else:
        r.note("not a biped, or a fall")
    r = R("head_turn", "LIMB")
    if "head" in rig.full.bones and "torso" in rig.full.bones and kind != "death":
        f = np.array([0.0, 0.0, -1.0])
        vals = [_ang(sc._rot(fr.bone_M["head"]) @ f, sc._rot(fr.bone_M["torso"]) @ f) for fr in frames]
        k = int(np.argmax(vals))
        r.set(vals[k], ts[k])
    else:
        r.note("no head and torso bones, or a fall")
    # pops and jitter: local rotation steps of every animated bone
    touched = sorted(set().union(*[c.touched() for c in clips]) & set(rig.full.bones))
    rp, rj = R("pop", "LIMB"), R("jitter", "LIMB")
    pop, jit = (0.0, 0.0, ""), (0.0, 0.0, "")
    for b in touched:
        par = rig.full.bones[b].parent
        Rs = [(sc._rot(fr.bone_M[par]).T if par else np.eye(3)) @ sc._rot(fr.bone_M[b]) for fr in frames]
        s = [sc._angle(Rs[k], Rs[k + 1]) for k in range(len(Rs) - 1)]
        for k in range(1, len(s) - 1):
            spike = s[k] - max(s[k - 1], s[k + 1])
            if spike > pop[0] and s[k] > 2.5 * max(s[k - 1], s[k + 1], 0.5):
                pop = (spike, ts[k], b)
        for k in range(len(s) - 1):
            back = min(s[k], s[k + 1]) - sc._angle(Rs[k], Rs[k + 2])
            if back > jit[0] and sc._angle(Rs[k], Rs[k + 2]) < 0.35 * (s[k] + s[k + 1]):
                jit = (back, ts[k + 1], b)
    rp.set(pop[0], pop[1], pop[2])
    rj.set(jit[0], jit[1], jit[2])
    r = R("settle", "LIMB")
    if sc.stem(name).startswith(("attack", "hit_")) and top.loop != "loop":
        worst_b = (0.0, "")
        for b in sorted(top.touched() & set(rig.full.bones)):
            a = sc._angle(sc._rot(frames[0].bone_M[b]), sc._rot(frames[-1].bone_M[b]))
            if a > worst_b[0]:
                worst_b = (a, b)
        r.set(worst_b[0], ts[-1], worst_b[1])
        if r.status == "FAIL" and vkind not in sc.MELEE and kind == "attack":
            r.status, r.detail = "WARN", r.detail + "; a shot may end on its own pose"
    else:
        r.note("only action clips (attack, hit)")

    if kind == "death":       # a fall ends with the body lying on its gear: these are for the reviewer, not a gate
        for key, rr in res.items():
            if rr.status == "FAIL" and (rr.group == "CLIP" or key in ("wrist", "knee_aim", "elbow")):
                rr.status, rr.detail = "WARN", (rr.detail + "; a fall, so not gated").strip("; ")
    for key in exempt:
        if key in res and res[key].status == "FAIL":
            res[key].status = "exempt"
    per_tick = {}
    for key, vals in series.items():
        if isinstance(vals, list) and len(vals) == len(ts):
            per_tick[key] = [round(max(vals[i:i + PER]) if key != "tip_speed" else vals[i], 2)
                             for i in range(0, len(ts), PER)]
        elif isinstance(vals, dict):
            per_tick[key] = vals
    return {"name": name, "base": list(base), "kind": kind, "length": top.length,
            "results": [r.row() for r in res.values()], "ticks": per_tick}


def load_exemptions(path=EXEMPT_PATH):
    if not os.path.isfile(path):
        return {}
    out = {}
    for e in sc._read(path).get("exemptions", []):
        out.setdefault((e["troop"], e["clip"]), set()).add(e["check"])
    return out


def check_troop(repo, troop, anim=None, geo=None, only=None):
    geo = geo or "%s/geo/troop/%s.geo.json" % (sc.ASSETS, troop)
    anim_path = anim or os.path.join(repo, "%s/animations/troop/%s.animation.json" % (sc.ASSETS, troop))
    model = rg.load_geo(os.path.join(repo, geo))
    data = sc._read(anim_path)
    rig = sc.Rig(model, sc.rig_rules_for(repo, geo), troop)
    design = sc.Design(repo)
    config = sc.booth_config_for(repo, troop)
    exempt = load_exemptions()
    out = []
    for name, base, speed in sc.units(list(data.get("animations", {})), config.clips if config else None):
        if only and name not in only:
            continue
        out.append(check_clip(rig, data, name, base, variant=design.variant(troop, name), speed=float(speed or 0.0),
                              exempt=exempt.get((troop, name), ())))
    return out


def print_report(troop, clips, show_pass=False, ticks=False):
    fails = 0
    for c in clips:
        bad = [r for r in c["results"] if r["status"] in ("FAIL", "WARN", "exempt")]
        nfail = sum(1 for r in bad if r["status"] == "FAIL")
        fails += nfail
        head = "FAIL" if nfail else "warn" if bad else "pass"
        print("%-5s %s %s" % (head, troop, c["name"]))
        for r in (c["results"] if show_pass else bad):
            if r["status"] == "n/a" and not show_pass:
                continue
            print("      %-6s %-5s %-15s %8s  tick %-6s %s" % (r["status"], r["group"], r["check"],
                  "" if r["value"] is None else r["value"], "" if r["tick"] is None else r["tick"], r["detail"]))
        if ticks:
            keys = [k for k, v in c["ticks"].items() if isinstance(v, list)]
            print("      tick  " + "  ".join("%14s" % k for k in keys))
            for i in range(len(c["ticks"][keys[0]])):
                print("      %4d  " % i + "  ".join("%14.2f" % c["ticks"][k][i] for k in keys))
    return fails


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    g = ap.add_mutually_exclusive_group(required=True)
    g.add_argument("--troop")
    g.add_argument("--all", action="store_true")
    ap.add_argument("--clip", action="append", help="only this clip (repeatable)")
    ap.add_argument("--anim", help="score this animation.json in place of the troop's exported one")
    ap.add_argument("--json")
    ap.add_argument("--ticks", action="store_true", help="print the per-tick numbers")
    ap.add_argument("--verbose", action="store_true", help="print passing and n/a checks too")
    try:
        a = ap.parse_args(argv)
    except SystemExit:
        return 2
    troops = sc.renderer_troops(REPO) if a.all else [a.troop]
    report, fails = {}, 0
    for t in troops:
        try:
            clips = check_troop(REPO, t, anim=a.anim, only=a.clip)
        except (OSError, rg.GeoError, sc.ScoreError) as e:
            print("error %s: %s" % (t, e), file=sys.stderr)
            return 2
        report[t] = clips
        fails += print_report(t, clips, a.verbose, a.ticks)
    total = sum(len(v) for v in report.values())
    bad = sum(1 for v in report.values() for c in v if any(r["status"] == "FAIL" for r in c["results"]))
    print("focus check: %d clips, %d with a FAIL (%d FAIL rows)" % (total, bad, fails))
    if a.json:
        with open(a.json, "w", encoding="utf-8", newline="\n") as f:
            json.dump({"limits": {k: {"warn": v[0], "fail": v[1], "why": v[2]} for k, v in LIMITS.items()},
                       "troops": report}, f, indent=1)
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
