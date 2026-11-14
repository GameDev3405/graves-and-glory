"""Admiral Vael's own three attacks (Kort, 2026-11-13: "Own moves for all three"). Until now the admiral played the Trident
Guard's thrusts and sweep refitted to his rig; these are his, built for his character: a large hero with a trident in two
hands whose ability is the undertow (he drags enemies in). Where the Trident Guard is a braced rank-and-file spearman, the
admiral moves like the tide: long and flowing, the weight rolling forward and drawing back, the cape and coat tails
following.

python art/anim/tc_admiral/build_own_attacks.py [--only thrust,high_thrust,low_sweep] [--out file]
    merges the six clips into a COPY of the admiral's animation file (default work/tc_admiral_own/tc_admiral.animation.json,
    made from the exported file when missing). Run alone it never writes the exported file or out/.
The normal build (GG_SPEAR_TROOP=tc_admiral python art/anim/spear/build_spear_anims.py) takes clips() as its last step, so
out/ and, after `--deploy=<the six clips>`, the exported file carry these clips and a rebuild keeps them.

It stands on art/anim/spear/build_spear_anims.py (imported, not edited for this): its shaft frame, two-hand solver, key
format (KEY: torso, gaze, O the rear fist in troop px, d the shaft, Ls the front fist up the shaft, arm priors), the
chest-frame sweep planner, and the passes every spear attack goes through (despike, the cloth lag, soft motion, the
armour, head and cloth clearances, the pose trims, the focus pass's admiral fixes). Gameplay is untouched: 22 ticks,
"impact" on 7, 7 and 8, as the clips these replace; every clip starts and ends on the admiral's guard.

attack_thrust: crouch, surge and haul (zone: body)
  0-1.5     tell      the tide draws back: the weight rocks onto the rear foot, the chest rises, the fists ease back
  1.0-3.0   gather    the lead foot draws back under the body
  1.5-6.0   crouch    a low rolling crouch: the pelvis drops 2 px and sits back over the rear foot, the chest pitches in
                      and coils, the trident is drawn far back along the right hip with the tines low by the lead knee
  5.0-7.0   surge     the lead foot reaches out long, the rear leg drives straight, the pelvis surges 6 px forward and
                      pitches in, the torso leans into it; the shaft rises from under level to the contact (tick 7)
  7-8.2     crest     the point carries on 2.6 px; the rear foot rolls up behind: the weight is all forward
  8.2-12.2  haul      the undertow: the tines dip and hook, both fists haul the trident back to the right hip, the weight
                      rocks back onto the rear foot, the chest comes upright and coils, the lead foot drags back
  12.2-22   recover   the rear foot then the lead foot walk home, the trident rises onto the guard, a breath, settle
attack_high_thrust: overhand harpoon stab, coming down (zone: head)
  0-1.5     tell      the weight goes up and back, the point starts to rise
  1.5-5.6   lift      the rear fist climbs past the right shoulder and stands above it, elbow high; the tines rise over
                      the target's head (31 degrees up); the chest turns side-on and leans back, the body tall on the rear foot
  5.0-6.8   step      the lead foot steps out long
  5.6-7     fall      the body drops onto the lead foot (the pelvis falls 1.5 px and pitches 6 degrees, the chest pitches
                      in 12), the tines come down onto the head (31 up to 10 up at the contact; the rig's arms cannot hold
                      the fist high enough for the tines to point down at head height)
  7-8.6     follow    the fall carries on, the point drives on and down to 7 degrees below level
  8.6-12.5  rise      the body rises off the lead foot, the rear foot catches up, the fist comes down off the shoulder
  12.5-22   recover   the feet walk home, the trident onto the guard
attack_low_sweep: the returning tide, a backhand sweep from the left (zone: legs, the shaft and the tines low)
  0-2.0     tell      the hips start to turn left, the point drifts across the body
  1.6-4.0   draw      the lead foot steps out wide to the left and forward, the pelvis sinks, the chest coils round to
                      the LEFT (the family sweep coils right), the trident is carried out past the left hip, tines low
  4.2-6.0   held      the wave stands, the coil drifting deeper
  6.0-8     reap      the hips lead the turn back to the right, the chest follows, the blade rolls its edge on and the
                      shaft sweeps flat and low from left to right through the shins (tick 8)
  7.4-11.2  carry     the sweep carries on round to the right with no stop; the rear foot steps back and out under the
                      turn and the lead foot drags in behind it
  10.6-17   ebb       the trident flows back from the right to the front, rising, the blade rolls back; the feet walk home
  17-22     settle    onto the guard
"""
import json
import os
import shutil
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
os.environ["GG_SPEAR_TROOP"] = "tc_admiral"
sys.path.insert(0, str(HERE.parent / "spear"))
import build_spear_anims as SP  # noqa: E402

assert SP.TROOP == "tc_admiral", SP.TROOP
K, np = SP.K, SP.np
SUB, TPS = SP.SUB, SP.TPS
KEY, aim, nudge = SP.KEY, SP.aim, SP.nudge
T = SP.ATTACK_TICKS                                      # 22, as the clips these replace
IMPACT = {"thrust": 7, "high_thrust": 7, "low_sweep": 8}  # the exported clips' "impact" markers
G = SP.GUARD
RH = SP.READY_HIPS
SETTLE = 3.0     # ticks at the end over which the arms ease onto the first sample's own solution

# ------------------------------------------------------------------------------------------------
# Keys. KEY(torso, gaze, O, d, Ls, rp, lp): torso (pitch + forward, yaw + to the troop's right, roll), gaze (the head's
# look in the troop frame), O the rear (right) fist in troop px (x - the troop's right, z + forward), d = aim(elevation,
# yaw to the left), Ls px between the fists along the shaft (never under 5.6), rp / lp the arm priors of the solve.
# Hips keys: (tick, (pitch, yaw, roll), (x, y, z)) with z + BACK; steps: (foot, lift, plant, (x, y, z) z + FORWARD, yaw, lift).
# ------------------------------------------------------------------------------------------------

THRUST = [
    (0, G),
    (1.5, KEY((10, 18, 0), (3, 0, 0), (-7.6, 13.3, -2.0), aim(15, 19), Ls=10.8, rp=(-20.0, -18.0, 14.0, -20.0), lp=(-60.0, 10.0, -40.0, -40.0))),
    (2.8, KEY((12, 21, 0), (4, 0, 0), (-5.9, 12.7, -5.3), aim(-10, 14), Ls=13.0, rp=(-8.48, -15.86, 13.86, -45.32), lp=(-19.68, 0, 0, -68.95))),
    (5.2, KEY((16, 22, 0), (5, 0, 0), (-6.0, 12.5, -5.6), aim(-12, 14), Ls=13.2, rp=(-8.48, -15.86, 13.86, -45.32), lp=(-19.68, 0, 0, -68.95))),
    (6.0, KEY((15, 21, 0), (5, 0, 0), (-6.2, 12.7, -4.8), aim(-9, 12), Ls=12.8, rp=(1.87, -17.51, 16.65, -53.64), lp=(-20.14, 0.01, 8.5, -67.79))),
    (6.5, KEY((16, 20, 0), (6, 0, 0), (-6.4, 13.1, 1.6), aim(2, 8), Ls=9.6, rp=(1.09, -28.02, 19.49, -65.98), lp=(-49.05, 25.42, 0, -42.25))),
    (7, KEY((18, 18, 0), (6, 0, 0), (-4.9, 13.6, 7.8), aim(8, 8), Ls=7.0, rp=(-47.3, -47.6, 7.2, -29.4), lp=(-77.9, 11.9, -4.2, -2.0))),
    (8.2, KEY((20, 18, 0), (7, 0, 0), (-5.5, 13.6, 10.4), aim(4, 8), Ls=6.4, rp=(-47.3, -47.6, 7.2, -29.4), lp=(-77.9, 11.9, -4.2, -2.0))),
    (9.2, KEY((14, 22, 0), (6, 0, 0), (-5.4, 13.0, 7.4), aim(-4, 5), Ls=7.0, rp=(-40.0, -40.0, 8.0, -40.0), lp=(-75.0, 15.0, -4.0, -10.0))),
    (11.0, KEY((3, 28, 0), (4, 0, 0), (-5.5, 15.6, -1.5), aim(-6, 4), Ls=10.0, rp=(-8.48, -15.86, 13.86, -45.32), lp=(-30.0, 0, 0, -60.0))),
    (12.2, KEY((2, 29, 0), (3, 0, 0), (-5.2, 14.7, -3.4), aim(-5, 6), Ls=10.5, rp=(-8.48, -15.86, 13.86, -45.32), lp=(-30.0, 0, 0, -60.0))),
    (15.0, KEY((9, 22, 0), (4, 0, 0), (-7.4, 12.7, -1.6), aim(10, 15), Ls=10.8, rp=(-20.0, -18.0, 14.0, -20.0), lp=(-60.0, 10.0, -40.0, -40.0))),
    (18.0, nudge(G, O=(0.1, 0.25, 0.3), torso=(-1.0, 1.0, 0))),
    (20.0, nudge(G, O=(0.0, 0.1, 0.0), torso=(0.5, -0.5, 0))),
    (T, G)]
THRUST_LEGS = {
    "hips": [(0, *RH),
             (1.5, (-3, 22, 0), (-0.2, -1.1, 1.0)),
             (2.8, (2, 28, -1), (-0.3, -3.0, 1.6)),
             (5.2, (4, 30, -1), (-0.3, -3.4, 1.6)),
             (6.0, (3, 28, -0.5), (-0.1, -3.4, 0.6)),
             (7, (10, 22, 0.5), (0.3, -3.3, -4.4)),
             (8.2, (11, 21, 0.5), (0.3, -3.0, -5.0)),
             (9.2, (7, 22, 0), (0.2, -2.8, -4.2)),
             (11.0, (-2, 28, -1), (-0.2, -2.2, -0.6)),
             (12.2, (-2.5, 29, -1), (-0.3, -2.3, -0.2)),
             (15.0, (-1, 24, 0), (-0.1, -1.8, 0.3)),
             (18.5, (0.5, 19.5, 0), (0.0, -1.35, 0.2)),
             (T, *RH)],
    "steps": [("left", 1.0, 3.0, (3.3, 2.0, 0.6), 14.0, 0.8),
              ("left", 5.0, 7.0, (4.9, 2.0, 8.2), 12.0, 1.3),
              ("right", 7.6, 9.6, (-3.0, 2.0, 1.0), -8.0, 0.8),
              ("left", 10.0, 12.4, (3.6, 2.0, 4.8), 13.0, 0.6),
              ("right", 12.6, 15.4, (-3.2, 2.0, -2.2), -6.0, 0.8),
              ("left", 15.6, 18.4, (3.4, 2.0, 2.4), 14.0, 0.9)],
}

HIGH = [
    (0, G),
    (1.5, KEY((10, 22, 0), (2, 0, 0), (-8.0, 15.0, -1.6), aim(22, 19), Ls=9.6, rp=(-30.0, -20.0, 25.0, -20.0), lp=(-80.0, 20.0, -50.0, -30.0))),
    (3.5, KEY((2, 36, 0), (-2, 0, 0), (-9.2, 20.5, -3.0), aim(30, 17), Ls=7.6, rp=(-60.0, -10.0, 40.0, -60.0), lp=(-110.0, 30.0, -20.0, -30.0))),
    (5.6, KEY((-2, 40, 0), (-14, 16, 0), (-9.5, 22.8, -3.6), aim(31, 16), Ls=5.5, rp=(-80.0, -5.0, 30.0, -80.0), lp=(-115.0, 35.0, -10.0, -25.0))),
    (6.3, KEY((4, 40, 0), (-12, 13, 0), (-8.2, 23.4, 1.6), aim(18, 16), Ls=6.8, rp=(-93.4, 2.6, 5.4, -98.5), lp=(-114.9, 37.7, -13.1, -40.1))),
    (7, KEY((10, 38, 0), (-6, 6, 0), (-8.6, 23.0, 8.0), aim(10, 16), Ls=8.5, rp=(-93.4, 2.6, 5.4, -98.5), lp=(-114.9, 37.7, -13.1, -40.1))),
    (8.6, KEY((14, 36, 0), (2, -6, 0), (-7.6, 21.0, 8.4), aim(-7, 15), Ls=8.3, rp=(-93.4, 2.6, 5.4, -98.5), lp=(-114.9, 37.7, -13.1, -40.1))),
    (10.4, KEY((12, 34, 0), (1, 0, 0), (-8.6, 18.0, 5.0), aim(2, 16), Ls=9.0, rp=(-70.0, -5.0, 20.0, -80.0), lp=(-110.0, 30.0, -20.0, -30.0))),
    (12.5, KEY((12, 26, 0), (3, 0, 0), (-8.0, 15.6, 1.6), aim(14, 18), Ls=8.8, rp=(-30.0, -20.0, 25.0, -20.0), lp=(-80.0, 20.0, -50.0, -30.0))),
    (17.0, nudge(G, O=(0.2, 0.3, 0.6), torso=(0, -1, 0))),
    (19.5, nudge(G, O=(0.0, 0.1, 0.1), torso=(0.5, -0.5, 0))),
    (T, G)]
HIGH_LEGS = {
    "hips": [(0, *RH),
             (1.5, (-2, 24, 0), (-0.2, -1.15, 0.6)),
             (3.5, (-4, 29, -0.5), (-0.3, -1.0, 1.1)),
             (5.6, (-4.5, 30, -1), (-0.3, -1.05, 1.25)),
             (6.3, (0, 24, 0), (0.0, -1.4, -0.6)),
             (7, (6, 12, 0.5), (0.3, -2.6, -4.2)),
             (8.6, (11, 10, 0.5), (0.3, -3.6, -4.8)),
             (10.4, (6, 13, 0), (0.2, -2.5, -4.0)),
             (12.5, (2, 17, 0), (0.1, -1.8, -2.2)),
             (16.5, (0, 21, 0), (0.0, -1.35, 0.0)),
             (T, *RH)],
    "steps": [("left", 5.0, 6.8, (3.7, 2.0, 7.2), 12.0, 1.3),
              ("right", 7.2, 9.4, (-3.0, 2.0, 0.8), -8.0, 0.8),
              ("left", 12.6, 15.4, (3.4, 2.0, 2.4), 14.0, 0.9),
              ("right", 15.6, 18.6, (-3.2, 2.0, -2.2), -6.0, 0.8)],
}

# The sweep is planned in the chest's frame (SP._sweep_keys): torso, head, O (the rear fist, chest px), ev (world
# elevation), r (the shaft's yaw left of the chest's forward), phi (the blade's roll; its own track below), Ls, priors.
_RP_L, _LP_L = (-24.29, -45.72, 46.33, -71.88), (-83.79, 35.1, -13.57, -4.24)       # arms with the shaft out left
_RP_R, _LP_R = (60.95, -11.99, 10.79, -103.49), (-84.35, 35.11, -17.07, -3.68)      # arms with the shaft out right
SWEEP_PLAN = [
    (2.0, {"torso": (12, 4, 0), "head": (8, -2, 0), "O": (-6.2, 16.8, 3.0), "ev": 6, "r": 45, "Ls": 9.0, "rp": _RP_L, "lp": _LP_L}),
    (4.2, {"torso": (16, -22, 0), "head": (14, -8, 0), "O": (-5.0, 17.2, 4.6), "ev": -5, "r": 45, "Ls": 7.6, "rp": _RP_L, "lp": _LP_L}),
    (6.0, {"torso": (16, -24, 0), "head": (15, -9, 0), "O": (-5.0, 17.0, 4.6), "ev": -8, "r": 45, "Ls": 7.6, "rp": _RP_L, "lp": _LP_L}),
    (7.0, {"torso": (18, -8, 0), "head": (17, -5, 0), "O": (-5.4, 17.0, 4.0), "ev": -11, "r": 46, "Ls": 8.4, "rp": _RP_L, "lp": _LP_L}),
    (8.0, {"torso": (20, 12, 0), "head": (18, 0, 0), "O": (-6.6, 16.8, 4.8), "ev": -10, "r": 39, "Ls": 9.6, "rp": (7.39, -27.79, 21.74, -87.41), "lp": (-86.58, 35.62, -8.1, -2)}),
    (9.2, {"torso": (22, 30, 0), "head": (17, 6, 0), "O": (-7.2, 15.8, 2.2), "ev": -9, "r": 34, "Ls": 11.0, "rp": _RP_R, "lp": _LP_R}),
    (10.6, {"torso": (18, 36, 0), "head": (15, 8, 0), "O": (-7.4, 15.2, 2.4), "ev": -3, "r": 32, "Ls": 12.0, "rp": _RP_R, "lp": _LP_R}),
    (12.5, {"torso": (16, 28, 0), "head": (11, 5, 0), "O": (-7.0, 15.8, 2.8), "ev": 4, "r": 40, "Ls": 11.0, "rp": _RP_R, "lp": _LP_R}),
    (15.0, {"torso": (14, 18, 0), "head": (7, 1, 0), "O": (-5.6, 17.4, 3.4), "ev": 11, "r": 48, "Ls": 9.0, "rp": (-27.12, -48.83, 49.3, -63.58), "lp": (-83.84, 34.63, -17.91, -3.68)}),
    (17.5, {"torso": (14, 13, 0), "head": (5, 0, 0), "O": (-4.38, 18.25, 5.59), "ev": 16, "r": 52, "Ls": 7.2, "rp": (-39.85, -39.24, 50.08, -66.67), "lp": (-87.69, 29.12, -16.3, -6.35)}),
]
# The blade's roll (never in the wind-up, Kort: "weapons spins around in hand"): the tines turn their edge onto the sweep
# only as the strike starts, and back to the guard's hold on the way round.
SWEEP_PHI = [(0.0, (0.0,)), (5.6, (0.0,)), (7.4, (90.0,)), (9.4, (90.0,)), (10.6, (80.0,)), (14.2, (35.0,)),
             (17.5, (5.0,)), (22.0, (0.0,))]
SWEEP_LEGS = {
    "hips": [(0, *RH),
             (2.0, (3, 12, 0.5), (0.4, -1.7, -0.2)),
             (4.2, (8, -4, 1.5), (0.9, -2.7, -0.8)),
             (6.0, (8, -8, 1.5), (1.0, -3.0, -1.0)),
             (7.0, (9, 8, 0.5), (0.5, -3.1, -0.2)),
             (8, (10, 24, -0.5), (-0.1, -3.2, 0.5)),
             (9.2, (9, 32, -1), (-0.4, -3.1, 1.0)),
             (10.6, (7, 34, -1), (-0.45, -2.9, 1.1)),
             (12.5, (4, 28, -0.5), (-0.4, -2.3, 1.0)),
             (15.0, (2, 24, 0), (-0.1, -1.8, 0.4)),
             (18, (0.5, 21, 0), (0.0, -1.45, 0.1)),
             (T, *RH)],
    "steps": [("left", 1.6, 4.0, (6.2, 2.0, 4.8), 2.0, 0.9),
              ("right", 7.4, 9.4, (-3.4, 2.0, -2.6), 12.0, 0.2),
              ("left", 8.8, 11.2, (4.6, 2.0, 2.2), 20.0, 0.5),
              ("right", 12.0, 15.0, (-3.2, 2.0, -2.2), -6.0, 0.6),
              ("left", 15.2, 18.0, (3.4, 2.0, 2.4), 14.0, 0.8)],
}


def _sweep_keys():
    keep = SP.SWEEP_PHI
    SP.SWEEP_PHI = SWEEP_PHI
    try:
        return SP._sweep_keys(SWEEP_PLAN, SWEEP_LEGS)
    finally:
        SP.SWEEP_PHI = keep


PLANS = {"thrust": (lambda: THRUST, THRUST_LEGS), "high_thrust": (lambda: HIGH, HIGH_LEGS),
         "low_sweep": (_sweep_keys, SWEEP_LEGS)}


def solve(name):
    keys_fn, legs_spec = PLANS[name]
    keys, legs = keys_fn(), K.attack_legs(legs_spec)
    sol = SP.Solver()
    out, hits, bad = [], 0, []
    for i in range(T * SUB + 1):
        w = dict(sol.worst)
        sol.worst = {"right": 0.0, "left": 0.0}
        s = SP.sample(keys, legs[i], sol, i / SUB)
        if sol.worst["right"] > 1.5 or sol.worst["left"] > 0.5:
            bad.append("%.2f:%.1f/%.1f" % (i / SUB, sol.worst["right"], sol.worst["left"]))
        sol.worst = {k: max(w[k], sol.worst[k]) for k in w}
        if SP.shaft_hits(SP.fk_pose(s, legs[i])):
            hits += 1
            bad.append("hit %.2f" % (i / SUB))
        out.append(s)
    if bad and os.environ.get("OWN_DEBUG"):
        print("  grip err over 1.5 px (tick:right/left):", " ".join(bad))
    n = T * SUB
    for i in range(n - int(SETTLE * SUB), n + 1):        # same keys, same legs: only the solver's branch differs
        k = K.smooth((i - (n - SETTLE * SUB)) / (SETTLE * SUB))
        for b in ("right_arm", "right_forearm", "right_hand", "left_arm", "left_forearm", "left_hand"):
            a, e = np.array(out[i][b][0], float), np.array(out[0][b][0], float)
            e = a + (e - a + 180.0) % 360.0 - 180.0
            out[i][b] = (tuple(float(v) for v in a + (e - a) * k), None)
    print("attack_%s (own): grip err max %.3f px, left fist err max %.3f px, samples with the shaft in the body %d"
          % (name, sol.worst["right"], sol.worst["left"], hits))
    return out, legs


def attack(name):
    """The clip pair, through the spear builder's attack passes in its own order (attack_clips)."""
    label = "attack_" + name
    upper, legs = solve(name)
    if os.environ.get("OWN_FAST"):     # authoring only: the solve alone, no passes
        upper = SP.trim(SP.gear(upper, legs))
        clip = K.bake(upper, T, loop=False)
        clip["timeline"] = {K.fmt_t(IMPACT[name] / TPS): "impact"}
        return {label: clip, label + "_legs": K.bake(legs, T, loop=False)}
    upper = SP.despike(upper, label, legs)
    upper = SP.gear(upper, legs)
    upper, legs = SP.soft(upper, legs, label)
    upper, legs = SP.armour_pass(upper, legs, label)
    upper = SP.trim(upper)
    clip = K.bake(upper, T, loop=False)
    clip["timeline"] = {K.fmt_t(IMPACT[name] / TPS): "impact"}
    return {label: clip, label + "_legs": K.bake(legs, T, loop=False)}


# The own attacks' own focus fixes, after the builder's (same pass, FP.run): the sweep's load turns the head the way
# the thrust's does, so the back hair hangs a pixel back too; the overhand's lead forearm passes the face at the strike,
# so the head turns off it.
OWN_FIXES = [
    dict(clips=["attack_low_sweep"], bone="hair_back", chan="position", axis=2, signs=(-1,), step=0.5, reach=1.5),
    dict(clips=["attack_high_thrust"], bone="head", chan="rotation", axis=1),
    dict(clips=["attack_high_thrust"], bone="head", chan="rotation", axis=1, metric="limb", involve="left"),
    dict(clips=["attack_high_thrust"], bone="head", chan="rotation", axis=1, metric="limb", involve="right"),
    dict(clips=["attack_high_thrust"], bone="head", chan="rotation", axis=2, metric="limb", involve="right"),
    dict(clips=["attack_high_thrust"], bone="right_pauldron", chan="rotation", axis=2, step=1.0, reach=6.0),
    dict(clips=["attack_thrust"], bone="sash_end", chan="rotation", axis=2),
    dict(clips=["attack_thrust"], bone="sash_end", chan="rotation", axis=0),
    # the crouch draws the rear arm back under the back hair (1.97 at t2): the hair rides forward and up off it
    dict(clips=["attack_thrust"], bone="hair_back", chan="position", axis=2, signs=(1,), step=0.5, reach=1.5),
    dict(clips=["attack_thrust"], bone="hair_back", chan="position", axis=1, signs=(1,), step=0.5, reach=1.5),
    dict(clips=["attack_thrust"], bone="hair_back", chan="rotation", axis=0),
    # the sweep carries the rear arm across the chest (the right arm's shoulder block 1.95-1.98 into the torso at t9, t11.5,
    # t15, the back hair on it at t13.5): the arm sits up to 1 px out from the chest there, eased; then the right
    # epaulette's inner end, dipped into the chest's top by the raised arm (2.0, t2-15), slides fore or aft along the
    # shoulder, the least either way
    dict(clips=["attack_low_sweep"], bone="right_arm", chan="position", axis=0, signs=(-1,), step=0.5, reach=1.0,
         involve={"right_arm"}),
    dict(clips=["attack_low_sweep"], bone="right_epaulette", chan="position", axis=2, step=0.5, reach=2.0),
]


# (clip, bone) a spring piece that overshoots in one sample at the strike (focus_check pop), smoothed as the builder does
OWN_SMOOTHS = [("attack_thrust", "right_fringe"), ("attack_thrust", "left_fringe"), ("attack_high_thrust", "cape"),
               ("attack_high_thrust", "left_forearm")]   # the lead forearm turned back 8.2 deg in half a tick (jitter)


def clips(only=None):
    names = [n for n in PLANS if not only or n in only]
    out = {}
    for n in names:
        out.update(attack(n))
    SP.only_rig(out)
    fixes = [dict(f, clips=[c for c in f["clips"] if c in out]) for f in SP.FIXES + OWN_FIXES]
    fixes = [f for f in fixes if f["clips"]]
    if fixes and not os.environ.get("OWN_FAST"):   # the builder's focus pass, as its own attacks get it (main)
        print("own attacks: focus pass")
        SP.FP.run({"animations": out}, SP.TROOP, fixes, only=set(out))
        for clip, bone in OWN_SMOOTHS:
            SP.FP.smooth({"animations": out}, clip, bone)
    pin_ends(out)
    return out


def pin_ends(out):
    """The family clip's own first and last key on every channel it keyed (its last key is not quite its first: the
    family's passes moved the arms a fraction of a degree at the end), so the own attack hands over exactly where the
    family attack did. Read from the fixed values the unit test holds (tools/anim/own_attacks_tc_admiral_fixed.json)."""
    fixed = json.loads((ROOT / "tools/anim/own_attacks_tc_admiral_fixed.json").read_text(encoding="utf-8"))["clips"]
    for name, clip in out.items():
        for key in fixed[name]["first"]:
            bone, ch = key.rsplit(".", 1)
            chan = clip["bones"].get(bone, {}).get(ch)
            if not isinstance(chan, dict) or "vector" in chan:
                continue
            ts = sorted(chan, key=float)
            for t, end in ((ts[0], "first"), (ts[-1], "last")):
                v = [float(x) for x in fixed[name][end][key]]
                if isinstance(chan[t], dict):
                    chan[t] = dict(chan[t], vector=v)
                else:
                    chan[t] = v


def main():
    exported = ROOT / "src/main/resources/assets/gravesandglory/animations/troop/tc_admiral.animation.json"
    out = ROOT / "work/tc_admiral_own/tc_admiral.animation.json"
    if "--out" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--out") + 1])
    only = None
    if "--only" in sys.argv:
        only = set(sys.argv[sys.argv.index("--only") + 1].split(","))
    assert out.resolve() != exported.resolve(), "run alone this never writes the exported file"
    if not out.is_file():
        out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(exported, out)
    data = json.loads(out.read_text(encoding="utf-8"))
    new = clips(only)
    for name, clip in new.items():
        assert name in data["animations"], name
        data["animations"][name] = clip
    out.write_text(json.dumps(data, indent=1), encoding="utf-8")
    print("replaced", ", ".join(new), "in", out)


if __name__ == "__main__":
    main()
