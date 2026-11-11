"""Full-body clip kit: author an action clip by saying where things are in the world, not by guessing joint angles.

A clip is a small spec of tracks keyed by tick. The kit turns it into the two GeckoLib clips the game plays
(`<name>` for the upper body, `<name>_legs` for hips, legs and feet) on the knight skeleton:

  hips    [(tick, rot, pos)]            the pelvis: this is what drives a blow. rot/pos as in ATTACK_LEGS (pos z+ is back)
  steps   [(side, lift, plant, ankle px, yaw[, lift px])]   feet stay planted between steps (build_anims.feet_at)
  torso   [(tick, (x, y, z))]           lean (x+ forward), twist (y+ toward its right), side bend
  head    [(tick, (x, y, z))]           added to the automatic counter-turn that keeps the eyes on the foe
  reach   [(tick, (x, y, z) | None)]    the sword wrist, in px FROM THE SWORD SHOULDER along the world axes: x+ the troop's
                                        LEFT, y up, z+ forward. The arm is 8.1 px long, so keep the length under 7.9.
                                        None = where the guard holds it. (Or `hand`: the wrist in world px from the ground.)
  blade   [(tick, (pitch, yaw, lean))]  where the blade points: pitch 0 level forward, -90 straight up, -150 up and back
                                        over the shoulder, +60 forward and down; yaw + swings a forward blade to the left;
                                        lean + tips an upright blade to the left
  elbow   [(tick, (twist,))]            optional: upper-arm twist that swings the elbow out (default 0)
  fist    [(tick, (x, y, z))]           the shield fist, world px with the torso at rest (it rides the torso)
  face    [(tick, (x, y, z))]           the shield face relative to the torso (see build_anims.grip); None = upright
  throw   [(tick, (x, y, z))]           added to the face: the swing throws the shield's weight (it is never glued)
  ticks, impact, guard                  length, impact tick (written as the timeline marker), the pose it starts and ends on

Everything between keys is a monotone curve, so a key is never overshot. Per sample (four a tick) the kit solves the
sword arm so the wrist is exactly at `hand`, turns the hand so the blade points exactly along `blade`, and rolls it
about the blade so the edge leads whenever the tip is fast. The first and last sample are the guard pose exactly.

  import fullbody
  upper, legs = fullbody.solve(SPEC)      # samples
  clips = fullbody.clips("attack_overhead", SPEC)   # {"attack_overhead": ..., "attack_overhead_legs": ...}

The kit raises (with the tick) when a hand or fist target is out of reach. It does not judge the result:
tools/anim/focus_check.py and tools/anim/scorecard.py do, on the exported files.
"""
from __future__ import annotations

import math
import os
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(HERE.parent / "knight"))
sys.path.insert(0, str(ROOT / "tools" / "anim"))
import build_anims as B  # noqa: E402
import render_geo as rg  # noqa: E402
import scorecard as sc  # noqa: E402

SUB = B.SUB
SWORD_ARM = ("right_arm", "right_forearm", "right_hand")
EDGE_FAST = 12.0     # px per tick of the tip: at this speed the edge is fully on the cut's line
EDGE_SLOW = 4.0      # under this the hand keeps its natural grip
EDGE_RELEASE = 2.5   # ticks the roll takes to let go after a cut
ROLL_RATE = 14.0     # degrees per sample (a quarter tick): the fastest the hand turns about the blade


def _unit(v):
    n = np.linalg.norm(v)
    return v / n if n > 1e-9 else v


def _rig():
    geo = "%s/geo/troop/%s.geo.json" % (sc.ASSETS, B.TROOP)
    return sc.Rig(rg.load_geo(os.path.join(str(ROOT), geo)), sc.rig_rules_for(str(ROOT), geo), B.TROOP)


class Blade:
    """The weapon's rest geometry, read from the exported rig: its axis, its edge and its length from the wrist."""

    def __init__(self):
        rig = _rig()
        self.bone = rig.blade_bone
        M = B.world({}, self.bone)
        Mh = B.world({}, "right_hand")
        self.hand_rest = Mh[:3, :3]
        wrist = (Mh @ np.r_[B.MODEL.bones["right_hand"].pivot / 16.0, 1.0])[:3] * 16.0
        tip = (M @ rig.tip)[:3] * 16.0
        axis = M[:3, :3] @ np.array([0.0, 1.0, 0.0])
        self.axis = _unit(axis if axis @ (tip - wrist) > 0 else -axis)
        edge = M[:3, :3] @ np.array([1.0, 0.0, 0.0])
        self.edge = _unit(edge - (edge @ self.axis) * self.axis)
        self.length = float(np.linalg.norm(tip - wrist))
        self.rest = np.column_stack([self.axis, self.edge, np.cross(self.axis, self.edge)])

    def hand_world(self, d, e):
        """The hand's world rotation that points the blade along d with its edge along e (e square to d)."""
        return np.column_stack([d, e, np.cross(d, e)]) @ self.rest.T @ self.hand_rest


def blade_dir(pitch, yaw=0.0, lean=0.0):
    p, y, a = math.radians(pitch), math.radians(yaw), math.radians(lean)
    d = np.array([0.0, -math.sin(p), math.cos(p)])
    d = np.array([d[2] * math.sin(y), d[1], d[2] * math.cos(y)])
    return _unit(np.array([d[0] * math.cos(a) + d[1] * math.sin(a), -d[0] * math.sin(a) + d[1] * math.cos(a), d[2]]))


def _arc(a, b):
    """The shortest rotation taking unit vector a onto unit vector b."""
    v, c = np.cross(a, b), float(a @ b)
    if c < -0.9999:
        return -np.eye(3)
    K = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    return np.eye(3) + K + K @ K / (1.0 + c)


def _wrist(pose):
    M = B.world(pose, "right_hand")
    return (M @ np.r_[B.MODEL.bones["right_hand"].pivot / 16.0, 1.0])[:3] * 16.0


def arm_to(pose, target, twist=0.0, guess=None, tick=None):
    """right_arm (x, twist, z) and right_forearm x that put the wrist at target (world px) under the body in `pose`."""
    x = np.array(guess if guess is not None else (-44.0, 20.0, -50.0), dtype=float)
    t = np.asarray(target, dtype=float)

    def f(v):
        p = dict(pose)
        p["right_arm"] = ((v[0], twist, v[1]), (0, 0, 0))
        p["right_forearm"] = ((v[2], 0, 0), (0, 0, 0))
        return _wrist(p)

    for _ in range(100):
        a = f(x)
        err = t - a
        if np.linalg.norm(err) < 1e-4:
            break
        J = np.zeros((3, 3))
        for i in range(3):
            d = np.zeros(3)
            d[i] = 1e-3
            J[:, i] = (f(x + d) - a) / 1e-3
        step = np.linalg.solve(J.T @ J + 0.02 * np.eye(3), J.T @ err)
        n = np.linalg.norm(step)
        if n > 10:
            step *= 10 / n
        x += step
        x[2] = min(-2.0, max(-150.0, x[2]))       # the elbow only flexes, and never locks dead straight
    miss = float(np.linalg.norm(t - f(x)))
    if miss > 0.2:
        raise ValueError("tick %s: sword hand target %s is %.2f px out of reach" % (tick, np.round(t, 2), miss))
    return (x[0], twist, x[1]), x[2]


def _track(keys, t, width):
    if not keys:
        return (0.0,) * width
    if len(keys) == 1:
        return tuple(keys[0][1])
    return B.curve([(k[0], tuple(k[1])) for k in keys], t)


def _shoulder(pose):
    M = B.world(pose, "right_arm")
    return (M @ np.r_[B.MODEL.bones["right_arm"].pivot / 16.0, 1.0])[:3] * 16.0


def guard_reach(guard=None):
    """The guard's sword wrist as a reach from the sword shoulder (world axes, px)."""
    guard = guard or B.GUARD
    return guard_targets(guard)[0] - _shoulder({"hips": B.READY_HIPS, "torso": (guard["torso"], (0, 0, 0))})


def _hand(spec, pose, t, g_reach):
    if "reach" in spec:
        keys = [(k[0], tuple(g_reach) if k[1] is None else tuple(k[1])) for k in spec["reach"]]
        return _shoulder(pose) + np.array(_track(keys, t, 3))
    return np.array(_track(spec["hand"], t, 3))


def guard_targets(guard=None):
    """The guard pose as this kit's targets: (hand px, blade direction, edge direction, torso rot)."""
    guard = guard or B.GUARD
    pose = {"hips": B.READY_HIPS, "torso": (guard["torso"], (0, 0, 0)), "right_arm": (guard["right_arm"], (0, 0, 0)),
            "right_forearm": (guard["right_forearm"], (0, 0, 0)), "right_hand": (guard["right_hand"], (0, 0, 0))}
    bl = Blade()
    Mh = B.world(pose, "right_hand")[:3, :3]
    R = Mh @ bl.hand_rest.T
    return _wrist(pose), R @ bl.axis, R @ bl.edge, guard["torso"]


def solve(spec, report=False):
    """(upper samples, legs samples) of a spec, SUB per tick, ready for build_anims.bake."""
    ticks = spec.get("ticks", B.ATTACK_TICKS)
    guard = spec.get("guard", B.GUARD)
    n = ticks * SUB
    bl = Blade()
    legs = B.attack_legs({"hips": spec["hips"], "steps": spec["steps"]}, ticks)
    g_hand, g_dir, g_edge, _ = guard_targets(guard)
    g_reach = guard_reach(guard)

    # Pass 1: body, sword arm and blade direction.
    rows, guess = [], None
    for i in range(n + 1):
        t = i / SUB
        torso = _track(spec["torso"], t, 3)
        pose = {"hips": legs[i]["hips"], "torso": (torso, (0, 0, 0))}
        twist = _track(spec.get("elbow", []), t, 1)[0]
        hand = _hand(spec, pose, t, g_reach)
        d = blade_dir(*_track(spec["blade"], t, 3))
        arm, fore = arm_to(pose, hand, twist, guess, tick=t)
        guess = (arm[0], arm[2], fore)
        pose["right_arm"], pose["right_forearm"] = (arm, (0, 0, 0)), ((fore, 0, 0), (0, 0, 0))
        Mf = B.world(pose, "right_forearm")
        elbow = (Mf @ np.r_[B.MODEL.bones["right_forearm"].pivot / 16.0, 1.0])[:3] * 16.0
        rows.append({"t": t, "torso": torso, "pose": pose, "hand": hand, "d": d, "arm": arm, "fore": fore,
                     "Mf": Mf[:3, :3], "forearm": _unit(hand - elbow), "tip": hand + d * bl.length})

    # Pass 2: the edge. A hammer grip keeps the edge in the plane of forearm and blade; when the tip is fast the edge
    # turns onto the tip's own line of travel (Kort: the edge leads the cut on every tick, not only at the impact).
    # The natural grip is the guard's own wrist, bent the shortest way to the new blade direction with no turn about the
    # blade: it has no flip anywhere a wrist can reach, and it is the guard itself at both ends.
    a0, e0 = rows[0]["Mf"].T @ g_dir, rows[0]["Mf"].T @ g_edge
    rolls, phi, w_prev, env = [], 0.0, 0.0, 0.0
    for i, r in enumerate(rows):
        a, b = rows[max(0, i - 1)], rows[min(n, i + 1)]
        v = (b["tip"] - a["tip"]) / max(1e-9, b["t"] - a["t"])
        vp = v - (v @ r["d"]) * r["d"]
        speed = float(np.linalg.norm(vp))
        nat = r["Mf"] @ _arc(a0, r["Mf"].T @ r["d"]) @ e0
        nat = _unit(nat - (nat @ r["d"]) * r["d"])
        w = min(1.0, max(0.0, (speed - EDGE_SLOW) / (EDGE_FAST - EDGE_SLOW)))
        if speed >= EDGE_SLOW:      # a slow tip has no line of travel worth turning the edge to: the roll is kept
            raw = math.degrees(math.atan2(float(np.cross(nat, vp) @ r["d"]), float(nat @ vp)))    # nat -> travel, about d
            # A double-edged blade: either edge may lead (the roll is free by half turns). The nearer edge is picked as a
            # swing starts and then followed, so the hand never snaps half a turn in the middle of a cut.
            ref = 0.0 if w_prev < 0.05 else phi
            phi = ref + ((raw - ref + 90.0) % 180.0 - 90.0)
        # The edge stays on the cut's line through the follow-through and lets go slowly (Kort 2026-11-08: "late in the
        # slash the blade turned flat against the target").
        env = max(w, env - 1.0 / (EDGE_RELEASE * SUB))
        r["nat"], r["speed"], r["w"] = nat, speed, w
        rolls.append(env * phi)
        w_prev = env
    # Smooth the roll over half a tick each way, but never where the edge must be on the line (fast samples); then cap
    # how fast the hand may turn about the blade, so the roll in and out of a cut is a turn of the wrist, not a spin.
    rolls = [rolls[i] if r["w"] >= 1.0 else float(np.mean(rolls[max(0, i - 2):min(n, i + 2) + 1]))
             for i, r in enumerate(rows)]
    for order in (range(1, n + 1), range(n - 1, -1, -1)):
        for i in order:
            j = i - 1 if order.start == 1 else i + 1
            rolls[i] = rolls[j] + max(-ROLL_RATE, min(ROLL_RATE, rolls[i] - rolls[j]))
    smooth = []
    for i, r in enumerate(rows):
        psi = rolls[i]
        c, sn = math.cos(math.radians(psi)), math.sin(math.radians(psi))
        smooth.append(_unit(c * r["nat"] + sn * np.cross(r["d"], r["nat"])))

    # Pass 3: hands, shield arm, head.
    upper, sguess = [], None
    for i, r in enumerate(rows):
        t = r["t"]
        Rh = bl.hand_world(r["d"], smooth[i])
        hand_rot = B.decompose_zyx(r["Mf"].T @ Rh)
        torso = r["torso"]
        hips_rot = legs[i]["hips"][0]
        face = _track(spec["face"], t, 3) if spec.get("face") else B.upright(torso, hips_rot, keep=0.85, turn=0.65)
        face = tuple(f + w for f, w in zip(face, _track(spec.get("throw", []), t, 3)))
        fist = _track(spec["fist"], t, 3)
        try:
            sarm, sfore = B.fist_arm(fist, guess=sguess)
        except ValueError as e:
            raise ValueError("tick %s: shield %s" % (t, e))
        sguess = (sarm[0], sarm[1], sfore)
        look = _track(spec.get("head", []), t, 3)
        head = (-0.7 * torso[0] + look[0], -0.85 * torso[1] + look[1], -0.8 * torso[2] + look[2])
        upper.append({"torso": (torso, None), "head": (head, None), "right_arm": (r["arm"], None),
                      "right_forearm": ((r["fore"], 0.0, 0.0), None), "right_hand": (hand_rot, None),
                      "left_arm": (sarm, None), "left_forearm": ((sfore, 0.0, 0.0), None),
                      "left_hand": (B.grip(sarm, sfore, face), None)})
    # The ends are the guard itself, to the last digit: ease the solved angles onto it over the outer tick.
    for i in list(range(0, SUB + 1)) + list(range(n - SUB, n + 1)):
        s = B.smooth(min(i, n - i) / float(SUB))
        for b in B.UPPER_BONES:
            cur = np.array(upper[i][b][0], dtype=float)
            g = np.array(guard[b], dtype=float)
            cur = g + ((cur - g + 180.0) % 360.0 - 180.0)
            upper[i][b] = (tuple(g + (cur - g) * s), None)
    upper = B.cape_lag(upper, legs)
    for i in range(n - 2 * SUB, n + 1):      # the cape comes to rest on the pose it started from
        s = B.smooth((n - i) / (2.0 * SUB))
        c, c0 = np.array(upper[i]["cape"][0]), np.array(upper[0]["cape"][0])
        upper[i]["cape"] = (tuple(c0 + (c - c0) * s), None)
    if report:
        worst = max(_euler_gap(upper, b) for b in B.UPPER_BONES)
        print("fullbody: %d samples, peak tip speed %.1f px/tick, worst between-key error %.2f deg"
              % (n + 1, max(r["speed"] for r in rows), worst))
    return upper, legs


def _R(rot):
    r = np.radians([-rot[0], -rot[1], rot[2]])
    return (rg._Rz(r[2]) @ rg._Ry(r[1]) @ rg._Rx(r[0]))[:3, :3]


def _euler_gap(samples, bone):
    """Largest angle between the pose GeckoLib shows halfway between two keys (it blends the three angles) and the pose
    halfway along the true turn. Large means a glitch frame between keys that no on-key check can see."""
    worst = 0.0
    for a, b in zip(samples, samples[1:]):
        ra, rb = np.array(a[bone][0]), np.array(b[bone][0])
        rb = ra + ((rb - ra + 180.0) % 360.0 - 180.0)
        Ra, Rb, Rm = _R(ra), _R(rb), _R((ra + rb) / 2.0)
        full = sc._angle(Ra, Rb)
        worst = max(worst, abs(sc._angle(Ra, Rm) - full / 2.0), abs(sc._angle(Rm, Rb) - full / 2.0))
    return worst


def clips(name, spec, report=True):
    """{name: upper clip, name + "_legs": legs clip}, baked like every other knight clip."""
    ticks = spec.get("ticks", B.ATTACK_TICKS)
    upper, legs = solve(spec, report)
    clip = B.bake(upper, ticks, loop=False)
    if spec.get("impact") is not None:
        clip["timeline"] = {B.fmt_t(spec["impact"] / B.TPS): "impact"}
    return {name: clip, name + "_legs": B.bake(legs, ticks, loop=False)}


def reach_report(spec, step=0.5):
    """Prints, every `step` ticks, the sword shoulder, the hand target and how far apart they are against the arm's
    reach, and the same for the shield fist. Run it before solve() when a target is out of reach."""
    ticks = spec.get("ticks", B.ATTACK_TICKS)
    legs = B.attack_legs({"hips": spec["hips"], "steps": spec["steps"]}, ticks)
    arm = sum(float(np.linalg.norm(B.MODEL.bones[a].pivot - B.MODEL.bones[b].pivot))
              for a, b in (("right_arm", "right_forearm"), ("right_forearm", "right_hand")))
    ls = np.array([5.0, 22.0, 0.0])
    print("sword arm reach %.2f px" % arm)
    for i in range(0, ticks * SUB + 1, max(1, int(round(step * SUB)))):
        t = i / SUB
        pose = {"hips": legs[i]["hips"], "torso": (_track(spec["torso"], t, 3), (0, 0, 0))}
        M = B.world(pose, "right_arm")
        sh = (M @ np.r_[B.MODEL.bones["right_arm"].pivot / 16.0, 1.0])[:3] * 16.0
        hand = _hand(spec, pose, t, guard_reach(spec.get("guard")))
        fist = np.array(_track(spec["fist"], t, 3))
        d, df = float(np.linalg.norm(hand - sh)), float(np.linalg.norm(fist - ls))
        print("tick %5.2f  shoulder %-22s hand %-22s %5.2f%s   fist %5.2f%s" % (
            t, np.round(sh, 1), np.round(hand, 1), d, " OUT" if d > arm - 0.05 else "    ", df, " OUT" if df > 7.9 else ""))
