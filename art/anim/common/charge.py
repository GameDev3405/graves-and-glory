"""Batch 3 charge clips for the finished biped troops: charge_run_upper.

python art/anim/common/charge.py <troop|all> [--out DIR]

The Charge order sends the stance "charge"; StanceController plays charge_run_upper over the run legs (GaitController's run_legs, which
the stride clock drives by ground distance). The clip is an UPPER-BODY loop exactly as long as the troop's run_legs / run_upper cycle
(7 ticks, 5 for the mist caller and the seer, 4 for the mage and the hexer) and it is timed by the same stride clock, so the arms stay
in step with the feet at any speed. Only the core upper bones are keyed (torso, head, arms, forearms, hands): the cloth, quivers and
cords keep the run clip's own swing.

Melee gear (shield, onehand, spear, axe): a committed charge. The torso leans well into the run (about 22 degrees), the head stays up
and forward, the weapon is cocked or couched and the free arm pumps hard against the stride (A7 in work/anim-clip-spec-2026-11-07.md).
Ranged and casters (bow, xbow, thrower, staff, focus): a SUPPORT ADVANCE. They do not sprint into melee on a Charge; they follow the
line and shoot or cast once in range, so their clip is a brisk, upright carry of the weapon at the ready (the troop's own hold pose,
a light lean, a short bob), clearly not a melee charge. When one comes in reach of an enemy the stance becomes ranged_aim and the
aim and attack clips play instead.

All poses are absolute, built from the troop's own ready / shield-up / hold poses with stances.lift_arm and pitch, so every weapon
keeps its grip. Convention: arm x negative is forward, torso x positive leans forward, torso y positive brings the left shoulder
forward. The left leg is forward at LP ticks into run_legs (the minimum of left_leg x); the arm opposite a forward leg is forward.
"""
import argparse
import math
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import stances as S                      # noqa: E402
from stances import (Troop, GEAR, bake, frames_of, lift_arm, pitch, copy_pose, pose_at, smooth, merge)  # noqa: E402

CORE = ("torso", "head", "left_arm", "right_arm", "left_forearm", "right_forearm", "left_hand", "right_hand")
TAU = 2 * math.pi
MELEE = {"shield", "onehand", "spear", "axe"}
SHIELD_LEAN, ONEHAND_LEAN = 22.0, 22.0   # the torso's forward lean in the charge (degrees; A7 asks 15-20 from upright, the guard already carries about 4)
AIM_UP = -6.0                            # the blade's pitch in the sword gear, as the sum of the chain's x rotations: 6 degrees above level


def v3(x=0.0, y=0.0, z=0.0):
    return np.array([x, y, z], float)


def core_of(P):
    return {b: [np.array(P[b][0], float), None] for b in CORE if b in P}


def cycle(tr):
    """(N ticks, LP): the stride cycle length in ticks and the tick at which the left leg is farthest forward."""
    legs = tr.anims["run_legs"]
    n = int(round(legs["animation_length"] * S.TPS))
    best = min(range(n * 4), key=lambda i: S.sample(legs, "left_leg", "rotation", i / (4 * S.TPS))[0])
    return n, best / 4.0


def lean(P, d, keep_l=1.0, keep_r=1.0, head=-1.0):
    """The torso leans d degrees further forward; each arm is counter-pitched by keep * d so its weapon keeps (keep 1) or follows (keep 0) the lean."""
    P["torso"][0][0] += d
    P["head"][0][0] += head * d
    P["left_arm"][0] = pitch(P["left_arm"][0], -keep_l * d)
    P["right_arm"][0] = pitch(P["right_arm"][0], -keep_r * d)


def set_fore(P, side, x, keep=0.0):
    f, h = f"{side}_forearm", f"{side}_hand"
    d = x - P[f][0][0]
    P[f][0][0] = x
    P[h][0][0] += -keep * d


# ---------------------------------------------------------------------------------------------- the melee carries
# Each returns (pose, cfg): the static charge carry, and the dynamic amounts: yaw (torso yaw swing), pump (left, right arm x swing),
# roll (torso roll swing), bob (torso pitch bob), both degrees. The pose is built with the torso already leaned.
def couch_sword(P, side, arm_x, fore_x, aim, arm_z, hand_y=None):
    """The sword arm set so the blade lies along the line of the stride: the weapon bone is the hand's blade-forward at rest (rotation [90, 0, -90]),
    so the blade's pitch above level is minus the sum of the x rotations down the chain (torso, arm, forearm, hand) and `aim` is that sum.
    Chest-high hand (the elbow bent, the forearm close to level) with the point forward and a touch up."""
    a, f, h = f"{side}_arm", f"{side}_forearm", f"{side}_hand"
    P[a][0] = np.array([arm_x, 0.0, arm_z], float)
    P[f][0] = np.array([fore_x, 0.0, 0.0], float)
    hy = P[h][0][1] if hand_y is None else hand_y
    P[h][0] = np.array([aim - (P["torso"][0][0] + arm_x + fore_x), hy, 0.0], float)


def carry_shield(tr):
    """The shield thrust forward and high, the sword levelled forward at chest height, point on the enemy (A7)."""
    U = pose_at(tr.anims["stance_shield_up_upper"], 0.0, upper_only=True)
    P = copy_pose(tr.ready)
    for b in CORE:
        if b in U:
            P[b] = [np.array(U[b][0], float), None]
    lean(P, SHIELD_LEAN - P["torso"][0][0], keep_l=0.85, keep_r=0.0)
    P["torso"][0][1] = 8.0
    couch_sword(P, "right", -58, -60, AIM_UP, 12)
    return P, dict(yaw=4.0, pump=(3.5, 5.0), roll=2.0, bob=1.5)


def carry_onehand(tr):
    """No shield: the sword levelled forward at chest height, point on the enemy, the free arm pumping hard (A7)."""
    P = copy_pose(tr.ready)
    lean(P, ONEHAND_LEAN - P["torso"][0][0], keep_l=0.0, keep_r=0.0)
    P["left_arm"][0][2] -= 6
    P["torso"][0][1] = 6.0
    couch_sword(P, "right", -58, -62, AIM_UP, 10)
    return P, dict(yaw=6.0, pump=(30.0, 4.0), roll=2.5, bob=1.8)


def carry_spear(tr):
    """Both hands on the shaft, the spear couched: levelled forward under the arm, the point on the enemy."""
    P = copy_pose(tr.ready)
    lift_arm(P, "left", 14, keep=0.0)
    lift_arm(P, "right", 6, keep=0.0)
    lean(P, 10, keep_l=0.5, keep_r=0.5)
    P["torso"][0][1] = 8.0
    return P, dict(yaw=3.0, pump=(2.5, 2.5), roll=1.5, bob=1.8, together=True)


def carry_axe(tr):
    """Both hands on the haft, the axe cocked high at the shoulder, ready to fall."""
    P = copy_pose(tr.ready)
    # both arms lifted together (the same lift, so the left fist stays on the haft) from the wide port guard: the axe tips on from
    # the shoulder, its head clear of the helmet (the old 42/34 lift and roll drove the haft into the breastplate from that guard)
    lift_arm(P, "right", 30, keep=0.0)
    lift_arm(P, "left", 30, keep=0.0)
    lean(P, 10, keep_l=0.5, keep_r=0.4)
    P["torso"][0][1] = 6.0
    return P, dict(yaw=5.0, pump=(4.0, 4.0), roll=2.0, bob=2.0, together=True)


# ---------------------------------------------------------------------------------------------- the support advance
def carry_ranged(tr):
    """The weapon at the ready as in the troop's own hold, held steady while the body runs: upright, a light lean."""
    P = S.hold_pose(tr, 0.0)
    P = {b: [np.array(r, float), None] for b, (r, q) in P.items() if b in CORE}
    for b in CORE:
        if b not in P:
            P[b] = [np.array(tr.ready[b][0], float), None]
    if tr.tid in S.AIM_POSE and tr.gear == "thrower":
        # the javelin cocked at the shoulder: the troop's own level-aim preset
        for b, r in S.AIM_POSE[tr.tid][0.0].items():
            if b in CORE:
                P[b] = [np.array(r, float), None]
    caster = tr.gear in ("staff", "focus")
    if tr.gear == "staff":
        lift_arm(P, "right", 30, keep=0.4)      # the staff carried angled well forward ahead of the body, the head of it up and out, not upright at the side
        P["left_forearm"][0][0] = min(P["left_forearm"][0][0], -50)   # the off hand open and forward, ready to cast
    elif tr.gear == "focus":
        lift_arm(P, "left", 26, keep=0.0)       # the focus held out ahead at chest height, ready to cast
        P["left_forearm"][0][0] = min(P["left_forearm"][0][0], -30)
    lean(P, 2 if caster else 6, keep_l=1.0, keep_r=1.0)   # casters stay upright; the rest lean a little
    return P, dict(yaw=3.0 if caster else 2.0, pump=(0.0, 0.0), roll=1.0, bob=2.8 if caster else 1.2, free=ranged_free(tr))


def ranged_free(tr):
    """The arm that holds no weapon swings a little with the stride: (side, degrees)."""
    if tr.tid in FREE_TROOP:
        return FREE_TROOP[tr.tid]
    return {"bow": ("right", 5.0), "xbow": ("left", 5.0), "thrower": ("left", 6.0), "staff": ("left", 3.0), "focus": ("right", 12.0)}[tr.gear]


# per troop overrides of ranged_free: the mist caller's long driftwood rod swings less with the stride, so its conch does not
# whip at the head of each step
FREE_TROOP = {"tc_mist": ("right", 10.0)}


CARRY = {"shield": carry_shield, "onehand": carry_onehand, "spear": carry_spear, "axe": carry_axe}


# ---------------------------------------------------------------------------------------------- the clip
def build_charge(tr):
    n, lp = cycle(tr)
    base, cfg = (CARRY[tr.gear] if tr.gear in MELEE else carry_ranged)(tr)
    yaw0 = base["torso"][0][1]

    def fn(t):
        th = TAU * (t - lp) / n
        c, s = math.cos(th), math.sin(th)
        P = copy_pose(base)
        P["torso"][0][1] = yaw0 - cfg["yaw"] * c                       # the shoulders counter-twist against the leading leg
        P["torso"][0][2] += cfg["roll"] * s
        P["torso"][0][0] += cfg["bob"] * math.cos(2 * th - 0.6)        # a step bob twice a cycle
        P["head"][0][1] += 0.9 * cfg["yaw"] * c                        # the head holds its heading against the twist
        P["head"][0][0] += -0.8 * cfg["bob"] * math.cos(2 * th - 0.6)
        pl, pr = cfg["pump"]
        if "free" in cfg:
            side, deg = cfg["free"]
            P[f"{side}_arm"][0][0] += (deg * c if side == "left" else -deg * c)
        elif cfg.get("together"):
            for side in ("left", "right"):                             # both hands on the shaft move together
                P[f"{side}_arm"][0][0] += pl * math.cos(2 * th - 0.6)
        else:
            P["left_arm"][0][0] += pl * c                              # forward when the right leg is forward
            P["right_arm"][0][0] += -pr * c
            P["left_forearm"][0][0] += -0.35 * pl * c
            P["right_forearm"][0][0] += 0.35 * pr * c
        return {b: (P[b][0], None) for b in CORE if b in P}
    return bake(frames_of(n, fn, loop=True), n, True)


def build_all(tr):
    return {"charge_run_upper": build_charge(tr)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("troop"); ap.add_argument("--out")
    a = ap.parse_args()
    # ic_arbalist is left out of "all": its crossbow builder merges and finishes these clips itself (run it, not this)
    for tid in ([t for t in GEAR if t != "ic_arbalist"] if a.troop == "all" else [a.troop]):
        tr = Troop(tid)
        clips = build_all(tr)
        dst = Path(a.out) / f"{tid}.animation.json" if a.out else tr.path
        merge(tr, clips, dst)
        print(f"{tid}: {', '.join(clips)} -> {dst}")


if __name__ == "__main__":
    main()
