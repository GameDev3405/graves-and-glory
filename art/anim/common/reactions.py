"""Batch 2 reaction clips for the finished biped troops: the stun loop and the knockback hit.

python art/anim/common/reactions.py <troop|all> [--out DIR]

Reads the troop's exported clip file, builds the new clips from the troop's own ready pose (the stun) or as additive deltas (the
knockback), and merges ONLY the new clips back with stances.merge, so every other clip stays byte for byte as it was.

Clips (see work/anim-clip-spec B2 and B3):
  stun_upper / stun_legs  24 ticks: a 4 tick hit-snap entry (head thrown back, chest arched, arms flared, knees buckled) settling into
                          a 20 tick dazed loop. StanceController plays the first pass from tick 0, then loops ticks 4..24 (the pose
                          at tick 24 is the pose at tick 4, so the loop is cut safe at any frame). The head lolls in a slow circle, the
                          chest slumps and sways, the knees dip, the weapon arm hangs low with the weapon still in the hand, the
                          off hand (shield) drops to the side. ABSOLUTE poses, like the other stance_ clips.
  stun_out_upper          6 ticks, ADDITIVE: the shake-off (the head shakes, the chest straightens, a shrug). StanceController adds it
                          for 6 ticks after the stun ends while the stun pose fades out into the next stance.
  hit_knockback           20 ticks, ADDITIVE like every hit clip (starts and ends at 0, no root travel; the server moves the body): the
                          torso thrown back and the head whipped, the arms flung out; hit_knockback_legs the footwork: the hips drop and
                          lean back, two stumbling steps (back foot first), then the recovery. Bigger than hit_medium on every joint.
"""
import argparse
import math
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import stances as S                      # noqa: E402
from stances import (Troop, GEAR, bake, frames_of, lift_arm, pitch, copy_pose, lerp_pose, smooth, ease_out, snap_up, merge)  # noqa: E402

CORE_UP = {"head", "torso", "left_arm", "right_arm", "left_forearm", "right_forearm", "left_hand", "right_hand"}
STUN_ENTRY, STUN_LOOP = 4, 20
STUN_TICKS = STUN_ENTRY + STUN_LOOP
KB_TICKS, OUT_TICKS = 20, 6
TAU = 2 * math.pi


def v3(x=0.0, y=0.0, z=0.0):
    return np.array([x, y, z], float)


def set_fore(P, side, x, keep=0.0):
    """The forearm's x set to `x`; the hand takes `keep` of the change back, so the held item keeps `keep` of its old orientation."""
    f, h = f"{side}_forearm", f"{side}_hand"
    d = x - P[f][0][0]
    P[f][0][0] = x
    P[h][0][0] += -keep * d


# ---------------------------------------------------------------------------------------------- stun: the pose bank
# per gear, how the arms hang in the daze: ("arm", side, lift_down_deg, hand_keep, forearm_x|None, forearm_keep, out_z)
# lift_down lowers the arm about the chest's lateral axis; hand_keep is how much of the weapon's orientation the wrist keeps.
STUN_ARMS = {
    "shield":  [("right", 34, 0.00, -25, 0.00, 0), ("left", 10, 1.00, -32, 0.80, -12)],      # the sword sags, the shield drops to the side
    "onehand": [("right", 34, 0.00, -25, 0.00, 0), ("left", 20, 0.00, -22, 0.00, -6)],
    "spear":   [("right", 26, 0.50, None, 0, 0), ("left", 26, 0.50, None, 0, 0)],             # the spear drops in both hands, the tip low
    "axe":     [("right", 62, 0.10, -14, 0.0, 2), ("left", 58, 0.10, -20, 0.0, -2)],               # the axe hangs loose from the hands at the hips, the haft sagging forward
    "bow":     [("left", 26, 0.60, None, 0, -4), ("right", 30, 0.00, -10, 0.0, 0)],            # the bow hangs from the fist, the string hand drops
    "xbow":    [("right", 32, 0.60, None, 0, 0), ("left", 18, 0.00, -15, 0.0, 0)],
    "thrower": [],                                                                              # explicit low-carry pose below
    "staff":   [("right", 18, 0.90, None, 0, 0), ("left", 14, 0.00, -20, 0.0, -4)],           # the staff stays up, planted for support
    "focus":   [("right", 26, 0.50, None, 0, 0), ("left", 14, 0.00, -20, 0.0, -4)],
}
# the harpooner's one-handed low carry (the same solved pose as its fall-back guard): the harpoon hangs forward-down, clear of the legs
THROWER_LOW = {"right_arm": [-39.65, 6.52, -13.67], "right_forearm": [-20.02, 2.34, 13.19], "right_hand": [-98.54, 21.65, 114.96],
               "left_arm": [-19.82, 2.98, -5.2], "left_forearm": [-45.72, -4.14, -9.8], "left_hand": [0.0, 0.0, 0.0]}


def extras_up(tr):
    return [b for b in tr.ready if b not in CORE_UP]


def stun_up_pose(tr, ph, s):
    """The dazed upper body at loop phase ph (0..1) with the hit-snap envelope s (1 at the strike, 0 from tick 4 on)."""
    P = copy_pose(tr.ready)
    g, a = tr.gear, TAU * ph
    if g == "thrower":
        for b, r in THROWER_LOW.items():
            P[b][0] = v3(*r)
    for side, down, keep, fx, fkeep, out in STUN_ARMS[g]:
        lift_arm(P, side, -down, keep)
        if fx is not None:
            set_fore(P, side, fx, fkeep)
        P[f"{side}_arm"][0][2] += out
    slump = 9.0 + 1.6 * math.sin(2 * a + 0.6)
    P["torso"][0] += v3(slump - 17 * s, 5 * math.sin(a + 0.5), 3.5 * math.sin(a))
    # the head lolls in a slow circle (nod, then roll, round the 20 ticks), snapped back at the strike
    P["head"][0] += v3(12 + 8 * math.cos(a) - 24 * s, 6 * math.sin(a + 1.0), 8 * math.sin(a))
    for side, sgn in (("left", -1), ("right", 1)):       # the arms swing a little against the sway of the chest; the snap flings them out
        P[f"{side}_arm"][0][2] += sgn * (2.5 * math.sin(a + math.pi) + 22 * s)
        P[f"{side}_arm"][0][0] += 2.0 * math.sin(2 * a + 0.4)
        P[f"{side}_forearm"][0][0] += 4.0 * math.sin(a + 0.8) + 10 * s
    for b in extras_up(tr):                               # cloth, quivers, cords hang plumb: they counter the chest and swing a beat late
        P[b][0] += v3(-0.7 * (slump - 17 * s) + 2.0 * math.sin(a - 1.0), 0.0, 1.2 * math.sin(a - 0.5))
    return P


def stun_legs_pose(tr, ph, s):
    """The knees dip, the weight sways from foot to foot, the feet splay; the snap buckles the knees."""
    P = lerp_pose(tr.ready_legs, tr.brace_legs, 0.45)
    a = TAU * ph
    dip = 0.5 + 0.5 * math.sin(2 * a - 0.9)
    add = 9.0 + 5.0 * dip + 11.0 * s                     # extra knee bend, degrees
    for side, sgn in (("left", -1), ("right", 1)):
        w = 1.0 + 0.25 * sgn * math.sin(a)               # the weight rides the leaning side
        P[f"{side}_shin"][0][0] += add * w
        P[f"{side}_leg"][0][0] += -0.5 * add * w
        P[f"{side}_foot"][0][0] += -(-0.5 + 1.0) * add * w
        P[f"{side}_leg"][0][2] += sgn * 3.0              # splay
        boot = f"{side}_boot"
        if boot in P:
            P[boot][0] = P[f"{side}_foot"][0].copy()
    hp = P["hips"][1] if P["hips"][1] is not None else v3()
    P["hips"][1] = hp + v3(0.8 * math.sin(a), -(0.9 + 0.35 * dip + 1.2 * s), 0.0)
    P["hips"][0] = P["hips"][0] + v3(0.0, 0.0, 2.5 * math.sin(a))
    for b in P:
        if b.startswith("skirt_"):
            P[b][0] = P[b][0] + v3(0.4 * add * (1 if b.endswith(("front", "back")) else 0.3), 0.0, 0.0) * (0.6 + 0.4 * math.sin(a + 1.0))
    return P


def snap_env(t):
    """The hit-snap: 0.55 at the strike, 1 a tick later, gone by tick 4."""
    if t >= STUN_ENTRY:
        return 0.0
    if t < 1.0:
        return 0.55 + 0.45 * ease_out(t / 1.0)
    return 1.0 - smooth((t - 1.0) / (STUN_ENTRY - 1.0))


def build_stun(tr):
    def phase(t):
        return ((t - STUN_ENTRY) / STUN_LOOP) % 1.0

    def up(t):
        return {b: (r, q) for b, (r, q) in stun_up_pose(tr, phase(t), snap_env(t)).items()}

    def legs(t):
        return {b: (r, q) for b, (r, q) in stun_legs_pose(tr, phase(t), snap_env(t)).items()}
    return bake(frames_of(STUN_TICKS, up), STUN_TICKS, True), bake(frames_of(STUN_TICKS, legs), STUN_TICKS, True)


def build_stun_out(tr):
    """The shake-off, additive: head shakes twice and lifts, the chest straightens, the shoulders shrug. Zero at both ends."""
    def fn(t):
        u = t / OUT_TICKS
        env, bump = math.sin(math.pi * u), 1 - u
        out = {b: (v3(), None) for b in ("head", "torso", "left_arm", "right_arm", "left_forearm", "right_forearm")}
        out["head"] = (v3(-7 * env, 16 * math.sin(TAU * 2 * u) * bump, 4 * math.sin(TAU * 2 * u + 1.0) * bump), None)
        out["torso"] = (v3(-5 * env, 6 * math.sin(TAU * 2 * u + 0.5) * bump, 0.0), None)
        out["left_arm"] = (v3(0.0, 0.0, -5 * env), None)
        out["right_arm"] = (v3(0.0, 0.0, 5 * env), None)
        for b in extras_up(tr):
            out[b] = (v3(3 * math.sin(TAU * 2 * u) * bump, 0.0, 0.0), None)
        return out
    return bake(frames_of(OUT_TICKS, fn), OUT_TICKS, False)


# ---------------------------------------------------------------------------------------------- knockback
# per gear and side: (arm x, outward z magnitude, forearm x, hand x), the fling at full strength (additive, degrees; arm x negative = up/forward)
KB_ARMS = {
    "shield":  dict(left=(-8, 52, 24, -8), right=(-6, 46, 20, -28)),
    "onehand": dict(left=(-10, 52, 16, 0), right=(-6, 46, 20, -28)),
    "spear":   dict(left=(-30, 12, 10, 0), right=(-30, 12, 10, -12)),       # two hands on the shaft: both thrown up and back together
    "axe":     dict(left=(-34, 10, 8, 0), right=(-34, 10, 10, -12)),
    "bow":     dict(left=(-12, 48, 20, -15), right=(-10, 50, 25, 0)),
    "xbow":    dict(left=(-10, 52, 25, 0), right=(-14, 42, 14, -20)),
    "thrower": dict(left=(-10, 52, 25, 0), right=(-10, 42, 20, -20)),
    "staff":   dict(left=(-12, 54, 28, 0), right=(-16, 36, 10, -10)),
    "focus":   dict(left=(-12, 54, 28, 0), right=(-12, 42, 10, -15)),
}


def kb_env(t):
    """The body's throw: in over 3 ticks, a slight sag, out over 11 ticks with a small forward overshoot. 1 at the peak, 0 at 0 and 20."""
    if t < 3:
        return ease_out(t / 3.0)
    if t < 9:
        return 1.0 - 0.2 * smooth((t - 3) / 6.0)
    r = min(1.0, (t - 9) / 11.0)
    return 0.8 * (1 - smooth(r)) - 0.13 * math.sin(math.pi * r) ** 2 * (1 if r > 0.4 else 0.0)


def kb_env_arms(t):
    """The arms fly faster than the body and flail down with a wobble: 0 to 1.15 over 2.5 ticks, hanging on, then 0 by tick 18."""
    if t < 2.5:
        return snap_up(t / 2.5, 0.15)
    r = min(1.0, (t - 2.5) / 15.5)
    return (1 - smooth(r)) * (1 + 0.12 * math.sin(TAU * 1.5 * r) * (1 - r))


def kb_env_head(t):
    """The head whips back a tick after the chest, then nods forward and settles."""
    if t < 1:
        return 0.0
    if t < 4:
        return snap_up((t - 1) / 3.0, 0.2)
    r = min(1.0, (t - 4) / 14.0)
    return (1 - smooth(r)) * 1.0 - 0.18 * math.sin(math.pi * r) * (1 if r > 0.3 else 0)


def lag(f, t, d):
    return f(max(0.0, t - d))


def build_knockback(tr):
    cfg = KB_ARMS[tr.gear]
    ex = extras_up(tr)

    def fn(t):
        e, ea, eh = kb_env(t), kb_env_arms(t), kb_env_head(t)
        out = {}
        out["torso"] = (v3(-32 * e, 7 * e, 0.0), None)
        out["head"] = (v3(-24 * eh + 6 * (e - eh), -5 * e, 3 * e), None)
        for side, sgn in (("left", -1), ("right", 1)):
            ax, az, fx, hx = cfg[side]
            out[f"{side}_arm"] = (v3(ax * ea, sgn * az * ea, 0.0), None)
            out[f"{side}_forearm"] = (v3(fx * ea, 0.0, 0.0), None)
            out[f"{side}_hand"] = (v3(hx * ea, 0.0, 0.0), None)
        for b in ex:             # cloth and quivers hang plumb against the thrown chest, then swing out a beat late
            out[b] = (v3(0.8 * 32 * lag(kb_env, t, 1.5) - 6 * math.sin(math.pi * min(1.0, t / 14.0)) * (1 - e * 0.5), 0.0, 0.0), None)
        return out

    up = bake(frames_of(KB_TICKS, fn), KB_TICKS, False)
    return up, build_knockback_legs(tr)


def pulse(t, a, b, c=None):
    """0 before a, a smooth rise to 1 at b, a smooth fall to 0 at c (default b + (b - a))."""
    c = c if c is not None else b + (b - a)
    if t <= a or t >= c:
        return 0.0
    return smooth((t - a) / (b - a)) if t < b else 1 - smooth((t - b) / (c - b))


def build_knockback_legs(tr):
    ex = [b for b in tr.ready_legs if b not in ("hips", "left_leg", "right_leg", "left_shin", "right_shin", "left_foot", "right_foot")]

    def fn(t):
        e = kb_env(t)
        hold = smooth(t / 2.0) * (1 - smooth((t - 12) / 8.0))           # the stagger lasts about 14 ticks
        stepL = pulse(t, 2.5, 5.5, 9.0)                                  # step one: the back foot, driven back
        stepR = pulse(t, 7.0, 10.0, 13.5)                                # step two: the front foot
        out = {}
        for side, st in (("left", stepL), ("right", stepR)):
            other = stepR if side == "left" else stepL
            leg = 20 * hold + 26 * st - 6 * other                        # the stepping leg swings back, the planted one braces
            shin = 24 * hold + 38 * st                                   # knees bent, the stepper's knee lifts
            foot = -(0.55 * leg + 0.9 * shin) + 6 * st * 0
            out[f"{side}_leg"] = (v3(leg, 0.0, (-1 if side == "left" else 1) * 6 * hold), None)
            out[f"{side}_shin"] = (v3(shin, 0.0, 0.0), None)
            out[f"{side}_foot"] = (v3(foot, 0.0, 0.0), None)
            if f"{side}_boot" in ex:
                out[f"{side}_boot"] = (v3(foot, 0.0, 0.0), None)
        avg = (out["left_leg"][0][0] + out["right_leg"][0][0]) / 2
        for b in ex:
            if b.startswith("skirt_"):
                k = 0.4 if b.endswith(("front", "back")) else 0.15
                out[b] = (v3(k * avg, 0.0, 0.0), None)
        out["hips"] = (v3(-7 * e, 3 * math.sin(math.pi * min(1.0, t / 14.0)) * (1 if t < 14 else 0), 0.0),
                       v3(0.5 * (stepR - stepL), -1.3 * hold - 0.5 * (stepL + stepR), 2.2 * e))
        return out
    return bake(frames_of(KB_TICKS, fn), KB_TICKS, False)


# ---------------------------------------------------------------------------------------------- build / merge
def build_all(tr):
    clips = {}
    clips["stun_upper"], clips["stun_legs"] = build_stun(tr)
    clips["stun_out_upper"] = build_stun_out(tr)
    clips["hit_knockback"], clips["hit_knockback_legs"] = build_knockback(tr)
    return clips


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("troop"); ap.add_argument("--out")
    a = ap.parse_args()
    for tid in (list(GEAR) if a.troop == "all" else [a.troop]):
        tr = Troop(tid)
        clips = build_all(tr)
        dst = Path(a.out) / f"{tid}.animation.json" if a.out else tr.path
        merge(tr, clips, dst)
        print(f"{tid}: {', '.join(clips)} -> {dst}")


if __name__ == "__main__":
    main()
