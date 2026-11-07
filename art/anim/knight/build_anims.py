"""Builds the knight's GeckoLib animations (unit animation rebuild, docs/anim-rebuild/PLAN.md).

python art/anim/knight/build_anims.py            -> out/knight.animation.json
python art/anim/knight/build_anims.py --check    -> also prints the foot-contact drift of every gait clip

Locomotion is generated, not hand keyed: a gait plan (where each foot is, how it is pitched, where the
hips are) is turned into bone rotations by inverse kinematics on the renderer's own bone maths
(tools/anim/render_geo.py, which mirrors GeckoLib 4.8.4), so a planted foot stays where it was put.

Clips come in pairs that animate disjoint bones and play on two controllers (GeoTroop):
  <gait>_legs   root, hips, legs, shins, feet           (the legs controller)
  <gait>_upper  torso, head, arms, forearms, cape, ...  (the upper controller)
A gait clip is one stride long at its reference speed; Java advances its phase by ground speed, so the
feet keep pace at any speed near it (GaitController).

World frame here: blocks, +Z forward, +Y up, the troop's right is -X. Pixel figures are 1/16 block.
Signs (checked numerically against render_geo, see signs()):
  leg  x+  swings the foot back          leg  z+  swings the right foot out (left foot in)
  shin x+  bends the knee                foot x+  points the toe down
  hips y+  turns toward its right        hips z+  tips its top toward its left
  hips pos x+ moves it to its left       hips pos z+ moves it back
  arm  x-  raises the hand forward       arm  z+  swings the right hand out
  head y+  turns toward its right
"""
from __future__ import annotations

import argparse
import json
import math
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(ROOT / "tools" / "anim"))
import render_geo as rg  # noqa: E402

# Another biped troop on the knight's skeleton (art/anim/common/troop_rig.py) builds the same clips against its own
# geometry (the falls ground its own corners): GG_ANIM_TROOP=sentinel python art/anim/knight/build_anims.py
import os  # noqa: E402
TROOP = os.environ.get("GG_ANIM_TROOP", "knight")
# Shieldless one-handed troops (revenant: notched sword, tc_raider: cutlass) keep the same body mechanics, but the empty left
# hand is a free fist: a lead/balance hand in the guard, a counter-pull to the hip on every blow, a natural arm swing in the
# gait, hanging loose at ease. Every shield-hold solve below then aims the fist only, with the wrist straight (FREE_WRIST).
SHIELD = TROOP in ("knight", "sentinel", "ic_hearthguard", "gl_marshal")
PREVIEW = HERE / "out" / "A" / "preview" if TROOP == "knight" else HERE.parent / TROOP / "out" / "preview"
GEO = PREVIEW / "t1.geo.json"
OUT = (HERE if TROOP == "knight" else HERE.parent / TROOP) / "out" / f"{TROOP}.animation.json"
# V2 (Kort approved 2026-11-06) is the default: attack drive and sword lane, graded hits, deaths, distinct stances, and front
# hits that open the sword arm outward. GG_V2=0 builds the old style. GG_V2_OUT, if set, redirects the output file.
V2 = os.environ.get("GG_V2") != "0"
if os.environ.get("GG_V2_OUT"):
    OUT = Path(os.environ["GG_V2_OUT"])
SUB = 4                     # samples per tick in generated clips
TPS = 20.0

MODEL = rg.load_geo(GEO)
FLIP3 = rg.WORLD_FLIP[:3, :3]


# ------------------------------------------------------------------------------------------------
# Forward kinematics (identical to render_geo.Scene.evaluate, bone matrices only)
# ------------------------------------------------------------------------------------------------

def local(name, rot=(0.0, 0.0, 0.0), pos=(0.0, 0.0, 0.0)):
    """A bone's matrix relative to its parent for animation rotation (degrees) and position (px)."""
    b = MODEL.bones[name]
    r = b.rot + np.radians([-rot[0], -rot[1], rot[2]])
    pv = b.pivot / 16.0
    return (rg._T(-pos[0] / 16.0, pos[1] / 16.0, pos[2] / 16.0) @ rg._T(*pv)
            @ rg._Rz(r[2]) @ rg._Ry(r[1]) @ rg._Rx(r[0]) @ rg._T(*(-pv)))


def chain(pose, names):
    """World matrices of `names` (each listed after its parent) for a pose {bone: (rot, pos)}."""
    M = {}
    for n in names:
        b = MODEL.bones[n]
        r, p = pose.get(n, ((0, 0, 0), (0, 0, 0)))
        par = M[b.parent] if b.parent in M else (rg.WORLD_FLIP if b.parent is None else world(pose, b.parent))
        M[n] = par @ local(n, r, p)
    return M


def world(pose, name):
    path = []
    n = name
    while n is not None:
        path.append(n)
        n = MODEL.bones[n].parent
    return chain(pose, path[::-1])[name]


def point(M, bedrock_px):
    """World position (px) of a point given in the geo file's own (Bedrock) pixel coordinates."""
    v = np.array([-bedrock_px[0], bedrock_px[1], bedrock_px[2], 16.0]) / 16.0
    return (M @ v)[:3] * 16.0


SIDES = {"right": -1.9, "left": 1.9}


def ankle_px(side):
    return (SIDES[side], 2.0, 0.0)


# ------------------------------------------------------------------------------------------------
# Leg IK
# ------------------------------------------------------------------------------------------------

LEG_CHAIN = ("root", "hips")


def leg_pose(pose, side, ankle_target, pitch, foot_yaw=0.0, guess=None):
    """Solves <side>_leg x/z and <side>_shin x so the ankle reaches ankle_target (world px), keeping the
    leg's y twist already in `pose`; then the foot so its sole is pitched `pitch` degrees toe-up and
    turned foot_yaw degrees toward the troop's left in the world. Returns (leg_rot, shin_rot, foot_rot)."""
    leg, shin, foot = side + "_leg", side + "_shin", side + "_foot"
    hipsM = chain(pose, LEG_CHAIN)["hips"]
    legY = pose.get(leg, ((0, 0, 0), (0, 0, 0)))[0][1]
    x = np.array(guess if guess is not None else (-20.0, 0.0, 30.0), dtype=float)
    target = np.asarray(ankle_target, dtype=float)

    def ankle(v):
        Ml = hipsM @ local(leg, (v[0], legY, v[1]))
        Ms = Ml @ local(shin, (v[2], 0, 0))
        return point(Ms @ local(foot), ankle_px(side))

    for _ in range(120):
        a = ankle(x)
        err = target - a
        if np.linalg.norm(err) < 1e-5:
            break
        J = np.zeros((3, 3))
        h = 1e-3
        for i in range(3):
            d = np.zeros(3)
            d[i] = h
            J[:, i] = (ankle(x + d) - a) / h
        # Damped (Levenberg-Marquardt) so a nearly straight leg does not throw the solve off.
        step = np.linalg.solve(J.T @ J + 0.02 * np.eye(3), J.T @ err)
        n = np.linalg.norm(step)
        if n > 10:
            step *= 10 / n
        x += step
        if x[2] < 0:          # the knee never bends backward
            x[2] = 0.0
    a = ankle(x)
    if np.linalg.norm(target - a) > 0.02:
        raise ValueError("%s ankle unreachable: target %s got %s (err %.3f px)"
                         % (side, np.round(target, 2), np.round(a, 2), np.linalg.norm(target - a)))
    Ms = hipsM @ local(leg, (x[0], legY, x[1])) @ local(shin, (x[2], 0, 0))
    R_target = Ry3(foot_yaw) @ Rx3(-pitch) @ FLIP3
    R_local = Ms[:3, :3].T @ R_target
    ax, ay, az = decompose_zyx(R_local)
    return (x[0], legY, x[1]), (x[2], 0.0, 0.0), (ax, ay, az)


def Rx3(deg):
    return rg._Rx(math.radians(deg))[:3, :3]


def Ry3(deg):
    return rg._Ry(math.radians(deg))[:3, :3]


def decompose_zyx(R):
    """Animation degrees (x, y, z) whose render rotation Rz(c) Ry(b) Rx(a) equals R (rest rotation 0)."""
    b = math.asin(max(-1.0, min(1.0, -R[2, 0])))
    a = math.atan2(R[2, 1], R[2, 2])
    c = math.atan2(R[1, 0], R[0, 0])
    return -math.degrees(a), -math.degrees(b), math.degrees(c)


# ------------------------------------------------------------------------------------------------
# Shield hold: the kite shield is gripped in the left fist (build_rig parents it to left_hand). The arm
# places the fist (fist_arm), the wrist keeps the shield upright and facing the front whatever the arm
# does (grip): its face is set relative to the torso, so it leans and turns only as much as asked.
# Fist targets are world px with the torso at rest: x+ the troop's left, y up from the ground, z+ forward.
# ------------------------------------------------------------------------------------------------

SHIELD_ARM = ("left_arm", "left_forearm", "left_hand")
Z3 = (0.0, 0.0, 0.0)
FREE_WRIST = (0.0, 0.0, 0.0)   # a free fist: the wrist in line with the forearm


# The targets below are set for the knight's shoulder (left_arm pivot 5, 22, 0); a troop whose shoulder sits elsewhere (the
# skeletons' at y 23) carries them with it, so a fist the knight reaches is reached the same from that shoulder. (The loaded
# pivots are in world axes: the knight's left shoulder is at x -5.)
SHOULDER_D = np.asarray(MODEL.bones["left_arm"].pivot, dtype=float) - np.array([-5.0, 22.0, 0.0])


def _fist(arm, fore):
    M = world({"left_arm": (arm, (0, 0, 0)), "left_forearm": ((fore, 0, 0), (0, 0, 0))}, "left_hand")
    return (M @ np.r_[MODEL.bones["left_hand"].pivot / 16.0, 1.0])[:3] * 16.0


def fist_arm(target, roll=-4.0, guess=None):
    """left_arm (x, y, roll) and left_forearm x putting the fist at target; the upper arm's roll (- out) is given."""
    x = np.array(guess if guess is not None else (-20.0, 20.0, -80.0), dtype=float)
    t = np.asarray(target, dtype=float) + SHOULDER_D
    f = lambda v: _fist((v[0], v[1], roll), v[2])
    for _ in range(80):
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
        x[2] = min(0.0, max(-150.0, x[2]))        # the elbow only flexes
    if np.linalg.norm(t - f(x)) > 0.25:     # a quarter pixel: the arm is near straight there
        raise ValueError("fist unreachable: target %s got %s" % (np.round(t, 2), np.round(f(x), 2)))
    return (x[0], x[1], roll), x[2]


def grip(arm, fore, face):
    """left_hand rotation holding the shield at `face`, animation degrees relative to the torso with the torso's own
    signs (x+ leans its top forward, y+ turns its face toward the troop's right), for this arm pose."""
    if not SHIELD:
        return FREE_WRIST
    Mf = world({"left_arm": (arm, (0, 0, 0)), "left_forearm": ((fore, 0, 0), (0, 0, 0))}, "left_forearm")[:3, :3]
    Mt = world({}, "torso")[:3, :3]
    Rd = local("left_hand", face)[:3, :3]
    return decompose_zyx(Mf.T @ Mt @ Rd)


def hold(fist, face, roll=-4.0, guess=None):
    """The shield arm's three bones for a fist target and shield face."""
    arm, fore = fist_arm(fist, roll, guess)
    return {"left_arm": arm, "left_forearm": (fore, 0.0, 0.0), "left_hand": grip(arm, fore, face)}


def regrip(frame):
    """Replaces a frame's "_face" entry (interpolated with the arm) by the left_hand angles that hold it."""
    face = frame.pop("_face")[0]
    frame["left_hand"] = (grip(frame["left_arm"][0], frame["left_forearm"][0][0], face), None)
    return frame


# ------------------------------------------------------------------------------------------------
# Gait plan
# ------------------------------------------------------------------------------------------------

HEEL = np.array([-2.0, -2.0])     # (forward, up) of the heel's contact edge from the ankle, px
TOE = np.array([3.5, -2.0])       # toe contact: between the bare foot's front (3) and the sabaton's (4)


def rot2(p, deg):
    """Rotates (forward, up) by deg toe-up about the ankle."""
    t = math.radians(deg)
    c, s = math.cos(t), math.sin(t)
    return np.array([p[0] * c - p[1] * s, p[0] * s + p[1] * c])


def smooth(t):
    t = min(1.0, max(0.0, t))
    return t * t * (3 - 2 * t)


def hermite(p0, p1, v0, v1, t):
    t2, t3 = t * t, t * t * t
    return ((2 * t3 - 3 * t2 + 1) * p0 + (t3 - 2 * t2 + t) * v0
            + (-2 * t3 + 3 * t2) * p1 + (t3 - t2) * v1)


class Gait:
    """One stride of a walk or run, sampled at phase u in [0, 1) (right heel strike at 0)."""

    def __init__(self, name, ticks, speed, duty, heel, toe_off, front, lift, lift_peak, hip_drop, bob,
                 bob_phase, sway, yaw, roll, pitch_hips=0.0, lane=1.9, flight=0.0):
        self.name, self.ticks, self.speed, self.duty = name, ticks, speed, duty
        self.heel, self.toe_off, self.front, self.lift, self.lift_peak = heel, toe_off, front, lift, lift_peak
        self.hip_drop, self.bob, self.bob_phase, self.sway, self.yaw, self.roll = hip_drop, bob, bob_phase, sway, yaw, roll
        self.pitch_hips, self.lane, self.flight = pitch_hips, lane, flight
        self.v = speed * 16.0                       # px a tick
        self.stance_ticks = duty * ticks
        self.travel = self.v * self.stance_ticks    # px the planted foot slides back relative to the hips
        self.stride = self.v * ticks

    # The stance foot: flat-ankle position F(s) = front - travel * s, pitched by heel roll then toe roll.
    HEEL_END, TOE_START = 0.16, 0.58

    def stance(self, s):
        """(forward, up) of the ankle and the sole pitch (toe-up degrees) at stance fraction s."""
        F = self.front - self.travel * s
        if s < self.HEEL_END:
            th = self.heel * (1 - smooth(s / self.HEEL_END))
            contact = np.array([F + HEEL[0], 0.0])
            return contact - rot2(HEEL, th), th
        if s > self.TOE_START:
            th = -self.toe_off * smooth((s - self.TOE_START) / (1 - self.TOE_START)) ** 1.15
            contact = np.array([F + TOE[0], 0.0])
            return contact - rot2(TOE, th), th
        return np.array([F, 2.0]), 0.0

    def swing(self, s):
        a0, p0 = self.stance(1.0)
        a1, p1 = self.stance(0.0)
        T = (1 - self.duty) * self.ticks
        # The foot leaves and lands moving back with the ground (zero slip at both ends), then a little
        # swing-leg retraction just before the heel lands.
        vz = -self.v * T
        z = hermite(a0[0], a1[0], vz, vz * 1.4, s)
        base = a0[1] + (a1[1] - a0[1]) * smooth(s)
        k = math.log(0.5) / math.log(self.lift_peak)
        y = base + self.lift * math.sin(math.pi * s ** k) ** 1.4
        # Pitch: toe comes up fast after toe-off, then sets for the heel strike.
        if s < 0.5:
            th = p0 + (self.heel * 0.2 - p0) * smooth(s / 0.5)
        else:
            th = self.heel * 0.2 + (p1 - self.heel * 0.2) * smooth((s - 0.5) / 0.5)
        return np.array([z, y]), th

    def foot(self, u):
        u %= 1.0
        if u < self.duty:
            return self.stance(u / self.duty)
        return self.swing((u - self.duty) / (1 - self.duty))

    def hips(self, u):
        """(rot, pos) of the hips bone at phase u."""
        tw = 2 * math.pi
        y = -self.hip_drop - self.bob * math.cos(tw * 2 * (u - self.bob_phase))
        x = -self.sway * math.sin(tw * (u - 0.03))
        yaw = -self.yaw * math.cos(tw * u)
        roll = self.roll * math.sin(tw * (u - 0.03))
        return (self.pitch_hips, yaw, roll), (x, y, 0.0)

    def pose(self, u, guesses):
        hr, hp = self.hips(u)
        pose = {"hips": (hr, hp)}
        out = {"hips": (hr, hp)}
        for side, off in (("right", 0.0), ("left", 0.5)):
            (fz, fy), th = self.foot(u + off)
            lx = -self.lane if side == "right" else self.lane
            pose[side + "_leg"] = ((0, -hr[1], 0), (0, 0, 0))
            lr, sr, fr = leg_pose(pose, side, (lx, fy, fz), th, guess=guesses.get(side))
            guesses[side] = (lr[0], lr[2], sr[0])
            out[side + "_leg"] = (lr, None)
            out[side + "_shin"] = (sr, None)
            out[side + "_foot"] = (fr, None)
        return out


WALK = Gait("walk", ticks=8, speed=0.16, duty=0.58, heel=15, toe_off=26, front=4.8, lift=2.6, lift_peak=0.4,
            hip_drop=1.1, bob=0.55, bob_phase=0.06, sway=0.3, yaw=4.0, roll=1.5)
# A run: shorter contact, a flight phase, the hips lowest at mid stance, knees more bent.
RUN = Gait("run", ticks=7, speed=0.26, duty=0.38, heel=6, toe_off=34, front=4.4, lift=3.6, lift_peak=0.45,
           hip_drop=2.1, bob=0.7, bob_phase=0.19, sway=0.2, yaw=6.0, roll=2.0)


# ------------------------------------------------------------------------------------------------
# Upper body: driven by the same phase as the legs, so it stays in step at any playback speed
# ------------------------------------------------------------------------------------------------

class Carry:
    """How the knight holds itself on the move. Angles in degrees; see the sign table at the top."""

    def __init__(self, lean, twist, arm, arm_lag, shield, fore, blade, follow, fist, head_keep, cape, arm_out=2.0,
                 arm_base=0.0, blade_out=0.0, roll=0.0, sway=0.0):
        self.lean, self.twist, self.arm, self.arm_lag = lean, twist, arm, arm_lag
        self.shield, self.fore, self.fist, self.sway = shield, fore, fist, sway
        # blade: the sword's pitch in the world (+ tip down from level); follow: how far it rocks with the arm, a beat late
        self.blade, self.follow = blade, follow
        self.arm_out = arm_out   # the sword arm's roll out from the side (+ out), keeping a raised blade off the face
        self.arm_base = arm_base  # the sword arm's mean pitch (- forward), lifting the hand off the hip
        self.blade_out = blade_out  # the wrist's yaw turning the point out (+), away from the near leg
        self.head_keep, self.cape = head_keep, cape
        # the shield: fist is where it is carried (see fist_arm), shield how far (px) it rocks fore and aft against the stride,
        # sway how far (deg) its top rocks with it
        self.roll = roll  # the shoulders' roll down toward the leg taking the weight, so the upper body reads the step


# Walking, the blade rides low and forward like the idle's (point down 22 deg); running, it is carried up at the ready
# (point 45 deg up), clear of the ground and of the knight's own legs. The wrist holds that angle while the arm swings:
# a blade that pitched with the arm read as a jab on every stride (review of walk3/run2, 2026-11-05).
# The sword hand is carried up off the hip and rolled out (fore, arm_out) so the cuff clears the thigh; the shield forearm
# bends less and swings less, because the shield pitches with it and a deep bend kicked its bottom edge out behind the
# knight (review of walk4/run4, 2026-11-05).
# Fix round 1 (review of run5, 2026-11-05): the walking blade drooped to foot height and crossed the near thigh, so the
# hand now rides higher (arm_base, fore) with the point near level and turned out; the shield arm counter-swings and the
# shoulders twist and roll against the stepping leg so the upper body is not a mannequin; the run blade sits lower and
# further out (it crossed the helm in front views), its rock is gentler, and the run leans and drives harder to read at range.
# Fix round 2 (re-review, 2026-11-05): in three-quarter view the walking point still crossed the near shin and the running point
# reached the helm, so both turn further out and the run blade sits lower; the shields are carried forward (shield_base) since
# both still trailed; the walking blade rocks a little more so it reads as carried, not fixed.
# Shield hold (Kort, 2026-11-05): the shield is carried upright in the fist in front of the left chest, rocking a little
# against the stride instead of swinging flat on the forearm.
WALK_CARRY = Carry(lean=3, twist=4.0, arm=11, arm_lag=0.04, shield=1.6, fore=-42, blade=0, follow=5, fist=(3.0, 17.5, 4.5),
                   head_keep=0.85, cape=(5, 2.0), arm_out=7.0, arm_base=-6.0, blade_out=34.0, roll=1.6, sway=5.0)
RUN_CARRY = Carry(lean=14, twist=4.0, arm=24, arm_lag=0.05, shield=2.0, fore=-62, blade=-12, follow=2, fist=(2.5, 18.5, 4.6),
                  head_keep=0.8, cape=(16, 3.5), arm_out=16.0, arm_base=-14.0, blade_out=20.0, roll=1.2, sway=8.0)


def upper_pose(g, c, u):
    """Upper-body bones at phase u. The torso carries the shoulders against the pelvis (counter-rotation),
    the head holds the gaze level and forward, the sword arm swings against the legs, the shield arm stays
    near the body, the cape trails and settles a beat behind the bob."""
    tw = 2 * math.pi
    (hx, hy, hz), (px, py, pz) = g.hips(u)
    # Shoulders turn against the pelvis: net shoulder yaw = twist * cos, opposite the hips' -yaw * cos.
    ty = (g.yaw + c.twist) * math.cos(tw * u)
    # Shoulders stay nearly level over the rolling pelvis, dipping a touch toward the leg in stance (right at u = 0.25).
    tz = -0.8 * hz - c.roll * math.sin(tw * (u - 0.03))
    # A little extra forward lean each time the body drops onto a leg, so the weight reads.
    tx = c.lean + 0.6 * math.cos(tw * 2 * (u - g.bob_phase))
    # The head undoes most of the body's turn, roll and pitch: eyes forward and level.
    net_yaw, net_roll = hy + ty, hz + tz
    head = (-(hx + tx) * c.head_keep, -net_yaw * c.head_keep, -net_roll * c.head_keep)
    # Arms swing against their own side's leg: the right arm is forward when the left foot is (u = 0.5).
    sw = math.cos(tw * (u - c.arm_lag))
    ra = (c.arm_base + c.arm * sw, 0.0, c.arm_out)
    # The elbow bends a little more as the arm comes forward, opens as it goes back.
    rf = (c.fore - 0.35 * c.arm * max(0.0, -sw), 0.0, 0.0)
    # The wrist keeps the blade at its carry pitch in the world, rocking a little a beat behind the arm.
    blade = c.blade + c.follow * math.cos(tw * (u - c.arm_lag - 0.1))
    rh = (blade - (hx + tx + ra[0] + rf[0]), c.blade_out, 0.0)
    # The shield fist rocks against the stride, bobs with each footfall and drifts toward the leg in stance; the
    # shield undoes only part of the body's lean, turn and roll (it rides the body), and its weight makes the top
    # trail the fist a beat late and swing out with the shoulder's turn.
    fx, fy, fz = c.fist
    bob = math.cos(tw * 2 * (u - g.bob_phase - 0.06))
    trail = math.cos(tw * (u - c.arm_lag - 0.12))
    if SHIELD:
        sh = hold((fx + 0.45 * math.sin(tw * (u - 0.05)), fy + 0.45 * bob, fz - c.shield * sw),
                  (-0.65 * (hx + tx) - c.sway * trail + 1.2 * bob, -0.35 * net_yaw - 0.6 * c.sway * trail,
                   -0.4 * net_roll + 0.4 * c.sway * math.sin(tw * (u - 0.1))))
    else:   # the empty hand swings against the sword arm (forward with the right foot), the elbow bending as it comes forward
        sh = {"left_arm": (0.6 * c.arm_base + 0.95 * c.arm * sw, 0.0, -0.4 * c.arm_out),
              "left_forearm": (0.5 * c.fore - 0.35 * c.arm * max(0.0, sw), 0.0, 0.0), "left_hand": FREE_WRIST}
    # Cape: blown back by the forward speed, flapping at the bob's rate, a quarter cycle late.
    flare, flap = c.cape
    cape = (flare + flap * math.sin(tw * 2 * (u - g.bob_phase - 0.12)), -0.5 * ty, -0.6 * tz)
    z = (0.0, 0.0, 0.0)
    return {"torso": ((tx, ty, tz), None), "head": (head, None), "right_arm": (ra, None),
            "right_forearm": (rf, None), "right_hand": (rh, None), "left_arm": (sh["left_arm"], None),
            "left_forearm": (sh["left_forearm"], None), "left_hand": (sh["left_hand"], None), "cape": (cape, None)}


def gait_upper(g, c):
    n = g.ticks * SUB
    samples = [upper_pose(g, c, i / n) for i in range(n)]
    samples.append(samples[0])
    return bake(samples, g.ticks)


# ------------------------------------------------------------------------------------------------
# Idle: the knight stands at ease, feet a little apart, breathing. 3 s loop.
# ------------------------------------------------------------------------------------------------

IDLE_TICKS = 60


def idle_clips():
    n = IDLE_TICKS * SUB
    legs, upper = [], []
    guesses = {}
    for i in range(n + 1):
        u = (i % n) / n
        br = math.sin(2 * math.pi * u)                    # one breath per loop
        sh = math.sin(2 * math.pi * u + 0.6)              # a slow weight shift, slightly out of step
        hips = ((0.0, 0.6 * sh, -0.5 * sh), (0.25 * sh, -0.55 - 0.12 * br, 0.0))
        pose = {"hips": hips}
        out = {"hips": hips}
        for side, foot in (("right", (-2.4, 2.0, 0.6)), ("left", (2.4, 2.0, -0.4))):
            pose[side + "_leg"] = ((0, -hips[0][1], 0), (0, 0, 0))
            lr, sr, fr = leg_pose(pose, side, foot, 0.0, foot_yaw=(-6 if side == "right" else 6),
                                  guess=guesses.get(side))
            guesses[side] = (lr[0], lr[2], sr[0])
            out[side + "_leg"], out[side + "_shin"], out[side + "_foot"] = (lr, None), (sr, None), (fr, None)
        legs.append(out)
        tor = (1.0 + 0.7 * br, -0.6 * sh, 0.5 * sh)
        lag = math.sin(2 * math.pi * u - 0.5)               # the shield's weight follows the breath a beat late
        if SHIELD:
            shd = hold((3.0 + 0.25 * sh, 17.5 + 0.35 * br, 4.5 + 0.2 * lag),
                       (-0.6 * tor[0] + 1.5 * lag, -0.4 * (hips[0][1] + tor[1]) + 1.2 * sh,
                        -0.4 * (hips[0][2] + tor[2]) - 1.0 * sh))
        else:   # at ease the empty hand hangs loose by the thigh, drifting with the breath
            shd = {"left_arm": (-2.0 - 0.6 * br + 0.4 * lag, 0.0, -3.0 - 0.5 * br), "left_forearm": (-14.0 - 1.0 * lag, 0.0, 0.0),
                   "left_hand": FREE_WRIST}
        upper.append({
            "torso": (tor, None),
            "head": ((-1.0 - 0.4 * br, 1.5 * math.sin(2 * math.pi * u * 2 - 1.0) * 0.5, -0.3 * sh), None),
            "right_arm": ((-2.0 - 0.6 * br, 0.0, 3.0 + 0.5 * br), None),
            "right_forearm": ((-22.0, 0.0, 0.0), None),
            "right_hand": ((40.0, 0.0, 0.0), None),
            "left_arm": (shd["left_arm"], None),
            "left_forearm": (shd["left_forearm"], None),
            "left_hand": (shd["left_hand"], None),
            "cape": ((1.0 + 0.6 * br, 0.0, 0.0), None),
        })
    return bake(legs, IDLE_TICKS), bake(upper, IDLE_TICKS)


# ------------------------------------------------------------------------------------------------
# Attacks: upper body only, played over the legs' gait or idle by GeoTroop's attack layer, which blends
# them in and out by weight. Tick 0 is the server's swing (Tactics.swing); each clip's impact key sits on
# its attackVariants row's impactTicks, the tick the server lands the blow. Shorter than the knight's
# 25-tick cooldown so repeated swings never overlap.
# ------------------------------------------------------------------------------------------------

ATTACK_TICKS = 22
UPPER_BONES = ("torso", "head", "right_arm", "right_forearm", "right_hand", "left_arm", "left_forearm", "left_hand")


def upright(torso, hips=Z3, keep=0.9, turn=0.7, tilt=0.0):
    """The shield face (see grip) that undoes `keep` of the body's lean and `turn` of its yaw and roll, plus `tilt`
    (+ top forward), so it stands near upright and faces the front."""
    return (tilt - keep * (torso[0] + hips[0]), -turn * (torso[1] + hips[1]), -turn * (torso[2] + hips[2]))


def key(torso, arm, fore, blade, blade_out, fist, face=None, throw=Z3):
    """One attack key pose. blade is the sword's world pitch (+ tip down, 0 level forward), turned into the
    wrist angle that holds it; blade_out swings the tip outward (+) or across the body (-). The head
    counters most of the torso so the eyes stay on the target. fist places the shield fist (fist_arm), face
    sets the shield (grip; default upright); the wrist is solved per sample after the keys are blended."""
    tx, ty, tz = torso
    hand = (blade - (tx + arm[0] + fore), blade_out, 0.0)
    face = upright(torso, keep=0.7, turn=0.45) if face is None else face
    face = tuple(f + t for f, t in zip(face, throw))    # the swing throws the shield's weight
    sh = hold(fist, face)
    return {"torso": torso, "head": (-0.7 * tx, -0.85 * ty, -0.8 * tz), "right_arm": arm,
            "right_forearm": (fore, 0.0, 0.0), "right_hand": hand, "left_arm": sh["left_arm"],
            "left_forearm": sh["left_forearm"], "left_hand": sh["left_hand"], "_face": face}


def front(fist, yaw):
    """A shield-fist target turned about the torso's vertical axis so it lands where `fist` would after the body
    yaws by `yaw` (animation degrees, the torso's signs): passing minus a body yaw keeps the fist in front."""
    c = TORSO_PIVOT
    a = math.radians(yaw)
    x, z = fist[0] - c[0], fist[2] - c[2]
    return (c[0] + x * math.cos(a) - z * math.sin(a), fist[1], c[2] + x * math.sin(a) + z * math.cos(a))


TORSO_PIVOT = tuple((world({}, "torso") @ np.r_[MODEL.bones["torso"].pivot / 16.0, 1.0])[:3] * 16.0)


# Shield fist targets (fist_arm): the guard holds it before the chest; a cut swings it out to the left.
FIST_GUARD = (2.0, 19.0, 6.2)
# The free fist (shieldless troops), torso-local px (x+ left, y up, z+ forward; left shoulder at (5, 22, 0), reach 8): the lead
# hand rides forward of the left hip, elbow soft, balancing the sword arm and ready to grab.
# Review 2026-11-06: carried higher and further forward so it reads as a working hand, not one hanging at the hip; in the
# brace and the wall it reaches out at the enemy, fending.
FREE_GUARD = (4.2, 17.8, 5.8)
FREE_STANCE = {"brace": (4.4, 17.8, 6.4), "brace_up": (4.4, 17.8, 6.4), "wall": (4.0, 18.5, 6.8), "wall_up": (4.0, 18.5, 6.8),
               "shield_up": (5.0, 16.0, 4.0), "ward": (8.0, 16.5, 2.5)}


def sf(name, fist):
    """The stance's fist target: the shield fist, or the free fist on a shieldless troop."""
    return fist if SHIELD else FREE_STANCE.get(name, FREE_GUARD)


def skey(torso, arm, fore, blade, blade_out, fist, hips=Z3, tilt=0.0, head=None):
    """A stance key: like key(), with the shield set upright over the stance's hips (STANCE_LEGS), tilted by tilt
    (+ top forward), and an optional head override."""
    k = key(torso, arm, fore, blade, blade_out, fist, upright(torso, hips, keep=0.85, turn=0.65, tilt=tilt))
    if head is not None:
        k["head"] = head
    return k


# The guard (docs/anim-rebuild/STYLE.md rule 1, Kort's reference reel): a ready pose, not a rest pose. The sword hand rides
# forward at the chest with the blade angled up and a little across the body, the shield is held out covering the torso, the
# left shoulder leads over the staggered feet of the ready footing (STANCE_LEGS "ready"). Every swing starts and ends on it.
READY = skey((5, 14, 0), (-44, 0, 14), -50, -44, -12, FIST_GUARD if SHIELD else FREE_GUARD, hips=(0, 6, 0))
if V2 and SHIELD:   # V2: the shield fist sits 3 px further left and the blade rides straight up, outside the rim, not laid over it
    FIST_GUARD = (5.0, 19.0, 5.6)
    READY = skey((5, 14, 0), (-44, 0, 20), -50, -44, -4, FIST_GUARD, hips=(0, 6, 0))
GUARD = READY
LONG = TROOP in ("sentinel", "gl_marshal")   # the Templar's 25-31 px longsword (and the Marshal's 24-32 px broadsword) follows through flatter, so its tip clears the ground
# The Marshal's broadsword reaches lower still: its cuts carry flatter by this much more (review 2026-11-06: the low cut's tip
# and the overhead's follow-through dipped under the soles).
FLAT = float(os.environ.get("GG_FLAT", 18.0 if TROOP == "gl_marshal" else 0.0))

# The standing footwork under each swing (STYLE.md rule 4): the hips coil back with the load, the lead (right) foot steps
# in and plants by the impact tick while the hips drop and drive forward into a wide, deep stance, then the foot steps
# back to the ready footing and the hips rise. hips: (tick, rot, pos) keys (pos z+ is backward in this rig) from and to STANCE_LEGS "ready"; steps:
# (side, lift tick, plant tick, ankle target px, foot yaw). A foot not stepping stays planted where it is.
READY_HIPS = ((0.0, 6.0, 0.0), (0.0, -1.1, 0.1))
ATTACK_LEGS = {
    "thrust": {"hips": [(0, *READY_HIPS), (3, (-2, 12, 0), (0.2, -1.3, 0.4)), (5.2, (-3, 14, 0), (0.2, -1.4, 0.5)),
                        (7, (5, -4, 0), (-0.1, -2.3, -1.3)), (9, (6, -6, 0), (-0.2, -2.5, -1.5)),
                        (12.5, (3, 0, 0), (0.0, -1.9, -0.9)), (17, (0, 6, 0), (0.0, -1.2, -0.2)), (ATTACK_TICKS, *READY_HIPS)],
               "steps": [("right", 4.0, 6.6, (-3.6, 2.0, 4.8), -10.0), ("right", 12.5, 16.5, (-3.4, 2.0, 2.0), -12.0)]},
    "overhead": {"hips": [(0, *READY_HIPS), (4, (-5, 11, 0), (0.1, -1.5, 0.8)), (7.5, (-7, 13, 0), (0.1, -1.7, 1.2)),
                          (10, (6, -6, 0), (-0.1, -2.6, -1.2)), (13, (7, -8, 0), (-0.1, -2.8, -1.3)),
                          (17, (2, 2, 0), (0.0, -1.6, -0.6)), (ATTACK_TICKS, *READY_HIPS)],
                 "steps": [("right", 6.8, 9.6, (-3.8, 2.0, 4.6), -10.0), ("right", 14.5, 18.5, (-3.4, 2.0, 2.0), -12.0)]},
    "low_cut": {"hips": [(0, *READY_HIPS), (3.5, (-2, 14, 0), (0.3, -1.3, 0.3)), (6.2, (-2, 16, 0), (0.3, -1.4, 0.3)),
                         (8, (6, -8, -2), (-0.4, -2.8, -0.9)), (11, (7, -12, -2), (-0.5, -3.0, -1)),
                         (15, (3, 0, 0), (0.0, -1.8, -0.5)), (ATTACK_TICKS, *READY_HIPS)],
                "steps": [("right", 5.0, 7.6, (-4.8, 2.0, 3.6), -18.0), ("right", 13.0, 17.5, (-3.4, 2.0, 2.0), -12.0)]},
}
if not SHIELD:   # the shieldless overhead load rocks back and drops the hips from the first ticks (review 2026-11-06)
    ATTACK_LEGS["overhead"]["hips"].insert(1, (2, (-3.5, 9.5, 0), (0.08, -1.45, 0.55)))
    # Review round 2 (2026-11-06): the load must read in the side view, so lean back further and sit deeper into it.
    ATTACK_LEGS["overhead"]["hips"][2] = (4, (-8, 11, 0), (0.1, -1.9, 0.9))
    ATTACK_LEGS["overhead"]["hips"][3] = (7.5, (-11, 13, 0), (0.1, -2.3, 1.3))
READY_FEET = {"right": ((-3.4, 2.0, 2.0), -12.0), "left": ((3.2, 2.0, -2.0), 7.0)}
if V2 and SHIELD:
    # V2 (review round 2): each blow is driven from the pelvis. The hips load first, back and over the rear (left) foot, a beat
    # before the torso coils; the drive carries them forward over the lead foot as it plants on the impact; then the rear foot
    # follows a short catch step so the follow-through lands on a supported base, and both feet walk back to the ready footing.
    ATTACK_LEGS = {
        "thrust": {"hips": [(0, *READY_HIPS), (2, (-1, 10, 1), (0.4, -1.3, 0.5)), (4.5, (-3, 15, 1.5), (0.6, -1.5, 0.8)),
                            (5.6, (-3, 15, 1.5), (0.5, -1.6, 0.7)), (7, (4, -6, -1), (-0.4, -2.4, -1.4)),
                            (9, (5, -8, -1), (-0.5, -2.6, -1.7)), (12, (3, -3, 0), (-0.2, -2.2, -2.2)),
                            (16.5, (1, 4, 0), (0.1, -1.6, -1.0)), (ATTACK_TICKS, *READY_HIPS)],
                   "steps": [("right", 4.6, 6.8, (-3.7, 2.0, 5.2), -10.0), ("left", 8.5, 11.0, (3.2, 2.0, 0.4), 6.0, 1.0),
                             ("right", 13.0, 16.5, (-3.4, 2.0, 2.0), -12.0), ("left", 16.0, 19.5, (3.2, 2.0, -2.0), 7.0)]},
        "overhead": {"hips": [(0, *READY_HIPS), (2.5, (-3, 9, 1), (0.4, -1.3, 0.6)), (5, (-6, 12, 2), (0.7, -1.6, 1.1)),
                              (7.8, (-7, 13, 2), (0.7, -1.7, 1.2)), (10, (5, -5, -1), (-0.4, -2.6, -1.4)),
                              (12.5, (6, -7, -1), (-0.5, -2.8, -1.7)), (15, (3, -2, 0), (-0.2, -2.3, -2.3)),
                              (18.5, (1, 3, 0), (0.1, -1.6, -1.0)), (ATTACK_TICKS, *READY_HIPS)],
                     "steps": [("right", 7.0, 9.6, (-3.9, 2.0, 5.0), -10.0), ("left", 11.5, 14.0, (3.2, 2.0, 0.4), 6.0, 1.0),
                               ("right", 15.0, 18.5, (-3.4, 2.0, 2.0), -12.0), ("left", 18.0, 21.0, (3.2, 2.0, -2.0), 7.0)]},
        "low_cut": {"hips": [(0, *READY_HIPS), (2, (-1, 10, 1), (0.4, -1.3, 0.4)), (4.5, (-2, 15, 2), (0.6, -1.5, 0.6)),
                             (6.4, (-2, 16, 2), (0.6, -1.6, 0.6)), (8, (6, -8, -3), (-0.6, -3.0, -1.2)),
                             (10.5, (7, -11, -3), (-0.7, -3.1, -1.4)), (13, (3, -3, 0), (-0.4, -2.5, -2.0)),
                             (17.5, (1, 3, 0), (0.1, -1.7, -1.0)), (ATTACK_TICKS, *READY_HIPS)],
                    "steps": [("right", 5.0, 7.6, (-5.0, 2.0, 4.0), -18.0), ("left", 10.5, 13.0, (3.4, 2.0, 0.0), 4.0, 1.0),
                              ("right", 14.0, 17.5, (-3.4, 2.0, 2.0), -12.0), ("left", 17.0, 20.5, (3.2, 2.0, -2.0), 7.0)]},
    }


def hips_yaw(name, t):
    """The attack_<name>_legs hips yaw at tick t (linear between its keys)."""
    ks = [(k[0], k[1][1]) for k in ATTACK_LEGS[name]["hips"]]
    for (t0, y0), (t1, y1) in zip(ks, ks[1:]):
        if t0 <= t <= t1:
            return y0 + (y1 - y0) * (t - t0) / (t1 - t0)
    return ks[-1][1]


def swing(name, t, torso, arm, fore, blade, blade_out, fist, throw=Z3, trail=0.8):
    """A (tick, key) of attack `name` whose shield fist and face stay in front of the body: the target (given as if
    the body faced front) is turned back against `trail` of the torso's and the legs clip's hips yaw, so the twist into
    a blow carries the shield across only a little (STYLE.md rule 9: it trails, it is never glued) instead of
    swinging it behind the back."""
    if not SHIELD:   # the free fist rides the body: its target is FREE_FIST's, torso-local
        return (t, key(torso, arm, fore, blade, blade_out, FREE_FIST[name][t], None, throw))
    yaw = min(0.0, torso[1] + hips_yaw(name, t))     # only the twist into the blow; the load keeps its shield pull-in
    face = upright(torso, (0.0, hips_yaw(name, t), 0.0), keep=0.7, turn=0.8)
    for k in range(int(round(trail * 10)), -1, -1):     # as much of `trail` as the arm can reach
        try:
            return (t, key(torso, arm, fore, blade, blade_out, front(fist, -0.1 * k * yaw), face, throw))
        except ValueError:
            pass
    raise ValueError("shield fist unreachable in %s at %s" % (name, t))


# The free fist through each blow (shieldless troops, STYLE.md rule 3's counter-pull): on the load it reaches out at the target,
# aiming and balancing the coil; the drive snaps it back past the left hip, the pull that turns the shoulders into the blow;
# the gather brings it forward again to the lead-hand guard.
FREE_FIST = {
    "thrust": {3: (4.0, 18.5, 6.5), 5.2: (3.8, 18.8, 6.8), 7: (6.5, 15.0, -2.0), 9: (6.8, 15.0, -2.6),
               12.5: (6.0, 15.5, 1.5), 17: (4.8, 16.6, 4.4)},
    # The free hand reaches high and forward at the foe through the load (round 2), then snaps to the hip as the blade falls.
    "overhead": {2: (2.4, 19.4, 6.6), 4: (1.8, 20.4, 6.4), 7.5: (1.2, 20.8, 6.0), 8.8: (4.6, 17.5, 4.5), 10: (6.4, 15.0, -1.6),
                 12: (6.8, 15.0, -2.4), 14: (6.8, 15.0, -2.4), 18: (5.4, 16.0, 2.8)},
    "low_cut": {3.5: (4.0, 17.8, 6.0), 6.2: (3.8, 17.8, 6.4), 8: (7.0, 15.0, -1.8), 11: (7.4, 15.2, -2.6),
                15: (6.0, 15.6, 1.6), 18.5: (4.8, 16.6, 4.4)},
}


# (tick, pose). Each swing has the reel's phases (STYLE.md rule 5): a load held long enough to read, with the shield pulled
# in to the chest (rule 7), a fast drive into the impact key, a follow-through that overshoots past it, a gather, then a
# settle that eases back onto the exact guard. The torso twists and leans into every blow (rule 3); the legs, hips and feet
# play the matching attack_<name>_legs clip while the knight stands (ATTACK_LEGS).
ATTACKS = {
    "thrust": [   # impact 7: body. Coil the blade back at the hip, hold, then drive straight out behind the shoulder.
        (0, GUARD),
        swing("thrust", 3, (-4, 26, 0), (18, 0, 16), -100, -4, 4, (1.6, 19.4, 4.8), throw=(4, 8, 2)),
        swing("thrust", 5.2, (-6, 30, 2), (22, 0, 16), -104, -6, 4, (1.4, 19.5, 4.6), throw=(5, 9, 2)),
        swing("thrust", 7, (16, -24, 0), (-88, 0, 2), -2, 4, -4, (3.4, 18.4, 6.4), throw=(-3, -6, -3)),
        swing("thrust", 9, (20, -28, 0), (-92, 0, -2), 0, 8, -8, (3.6, 18.2, 6.2), throw=(-4, -7, -4)),
        swing("thrust", 12.5, (12, -12, 0), (-58, 0, 6), -38, -12, 0, (2.8, 18.6, 6.0), throw=(-2, -4, -2)),
        swing("thrust", 17, (6, 12, 0), (-46, 0, 13), -50, -42, -10, (2.2, 18.9, 6.0)),
        (ATTACK_TICKS, GUARD),
    ],
    "overhead": [   # impact 10: head. Raise the blade over the back, hold it high, then cut down through and across.
        (0, GUARD),
    ] + ([   # shieldless (review 2026-11-06): the load starts at once, the body rocking back and the free hand drawn across
        swing("overhead", 2, (-4, 14, 0), (-108, 0, 16), -60, -98, -5, None, throw=(-3, 2, 2)),
    ] if not SHIELD else []) + [
        swing("overhead", 4, (-10, 13, 0), (-160, 0, 18), -68, -140, 0, (1.6, 20.0, 5.0), throw=(-6, 4, 4)),
        swing("overhead", 7.5, (-15, 17, 2), (-170, 0, 20), -74, -158, 0, (1.4, 20.2, 4.8), throw=(-6, 4, 4)),
        swing("overhead", 8.8, (2, 4, 0), (-150, 0, 14), -28, -80, 0, (2.4, 19.5, 5.6)),
        swing("overhead", 10, (16, -10, 0), (-100, 0, 6), -8, 22, 0, (3.6, 18.2, 6.2), throw=(-3, -5, -3)),
        swing("overhead", 12, (22, -16, 0), (-58, 0, 2), -12, (26 if LONG else 32) - FLAT, -6, (4.0, 17.9, 5.8), throw=(-4, -6, -4)),
        swing("overhead", 14, (22, -18, 0), (-46, 0, 2), -12, (30 if LONG else 36) - FLAT, -10, (4.0, 17.9, 5.8), throw=(-4, -6, -4)),
        swing("overhead", 18, (8, 8, 0), (-40, 0, 12), -52, -34, -8, (2.6, 18.8, 5.8)),
        (ATTACK_TICKS, GUARD),
    ],
    "low_cut": [   # impact 8: legs. Cock the blade high on the right, hold, sweep down and across at the shins, low.
        (0, GUARD),
        swing("low_cut", 3.5, (-4, 32, 6), (-32, 0, 58), -62, -42, 62, (1.6, 19.2, 4.8), throw=(4, 8, 2)),
        swing("low_cut", 6.2, (-6, 36, 8), (-34, 0, 62), -64, -46, 66, (1.4, 19.3, 4.6), throw=(5, 9, 2)),
        swing("low_cut", 8, (22, -22, -6), (-55, 0, 0), -10, 40 - FLAT, -30, (3.8, 17.8, 6.0), throw=(-3, -6, -4)),
        swing("low_cut", 11, (26, -36, -6), (-45, 0, -18), -15, (38 if LONG else 46) - FLAT, -48, (4.2, 17.6, 5.6), throw=(-4, -7, -5)),
        swing("low_cut", 15, (12, -10, 0), (-30, 0, 4), -40, 10, 6, (3.0, 18.4, 5.8)),
        swing("low_cut", 18.5, (6, 12, 0), (-44, 0, 13), -52, -40, -10, (2.2, 18.9, 6.0)),
        (ATTACK_TICKS, GUARD),
    ],
}

if V2 and SHIELD:
    # V2: the same phases on the sword's own lane. The shield fist rides 3 px further left (FIST_GUARD); the cuts come down and
    # finish on the sword side, tip out (blade_out +), instead of dragging the blade across the shield; the torso leans less and
    # turns less, the pelvis and feet (ATTACK_LEGS) carry the drive; the low cut lifts the shield over the head as the body drops.
    ATTACKS = {
        "thrust": [
            (0, GUARD),
            swing("thrust", 3, (-3, 22, 0), (16, 0, 18), -98, -4, 4, (4.8, 19.4, 5.0), throw=(4, 8, 2)),
            swing("thrust", 5.4, (-5, 26, 2), (20, 0, 18), -102, -6, 4, (4.6, 19.5, 4.8), throw=(5, 9, 2)),
            swing("thrust", 7, (12, -18, 0), (-88, 0, 8), -2, 4, -2, (5.4, 18.4, 6.2), throw=(-3, -6, -3)),
            swing("thrust", 9, (15, -21, 0), (-92, 0, 6), 0, 8, -4, (5.6, 18.2, 6.0), throw=(-4, -7, -4)),
            swing("thrust", 12.5, (8, -8, 0), (-58, 0, 10), -38, -12, -2, (5.2, 18.6, 5.8), throw=(-2, -4, -2)),
            swing("thrust", 17, (5, 12, 0), (-46, 0, 18), -50, -42, -4, (5.0, 18.9, 5.6)),
            (ATTACK_TICKS, GUARD),
        ],
        "overhead": [
            (0, GUARD),
            swing("overhead", 2.5, (-3, 12, 0), (-110, 0, 22), -60, -100, 4, (4.8, 19.2, 5.2), throw=(-3, 2, 2)),
            swing("overhead", 5, (-8, 14, 0), (-160, 0, 26), -68, -140, 4, (4.8, 19.0, 5.0), throw=(-6, 4, 4)),
            swing("overhead", 7.8, (-11, 17, 2), (-170, 0, 28), -74, -158, 4, (4.8, 19.0, 4.8), throw=(-6, 4, 4)),
            swing("overhead", 8.9, (1, 4, 0), (-150, 0, 22), -28, -80, 4, (5.0, 19.2, 5.6)),
            swing("overhead", 10, (11, -4, 0), (-100, 0, 16), -8, 22, 6, (5.4, 18.2, 6.2), throw=(-3, -5, -3)),
            swing("overhead", 12.5, (14, -7, 0), (-54, 0, 20), -12, 30, 10, (5.6, 17.9, 5.8), throw=(-4, -6, -4)),
            swing("overhead", 15, (12, -8, 0), (-46, 0, 16), -14, 32, 10, (5.6, 17.9, 5.8), throw=(-3, -5, -3)),
            swing("overhead", 18.5, (6, 8, 0), (-40, 0, 16), -52, -34, -4, (5.2, 18.8, 5.6)),
            (ATTACK_TICKS, GUARD),
        ],
        "low_cut": [
            (0, GUARD),
            swing("low_cut", 3.5, (-3, 28, 6), (-32, 0, 58), -62, -42, 62, (4.6, 20.4, 4.8), throw=(4, 8, 2)),
            swing("low_cut", 6.4, (-5, 32, 8), (-34, 0, 62), -64, -46, 66, (4.4, 20.6, 4.6), throw=(5, 9, 2)),
            swing("low_cut", 8, (15, -8, -4), (-58, 0, 16), -10, 28, 8, (5.4, 21.0, 6.0), throw=(-3, -6, -4)),
            swing("low_cut", 10.5, (16, -12, -4), (-50, 0, 14), -14, 24, 2, (5.8, 21.2, 5.6), throw=(-4, -7, -5)),
            swing("low_cut", 13, (10, -4, 0), (-34, 0, 16), -40, 10, 6, (5.4, 19.8, 5.8)),
            swing("low_cut", 17.5, (5, 12, 0), (-44, 0, 18), -52, -40, -4, (5.0, 18.9, 5.6)),
            (ATTACK_TICKS, GUARD),
        ],
    }



# ---- V2: the sword lane (review round 2). The sword and the sword forearm stay outside the shield's rim on every sample:
# never behind or through it, and over its face only a clear pixel in front. Each swing's keys are laid on the sword side of
# the rim; where the arm still has to pass the rim, the shield gives way (its fist eases left and back, its face opens) by
# just the amount the lane needs, a tick early, and comes back as the blade clears.
LANE_BONES = ("sword_t3", "right_forearm", "right_cuff", "right_couter")
LANE_PAD = 0.5          # px: lateral clearance past the rim
LANE_FRONT = -5.2       # shield px: a point at least a pixel in front of the boss (face plane at -3.6, boss front -4.2)
LANE_BACK = 3.5         # shield px: deeper behind the shield than this (its handle ends at 0.6) the torso hides the blade
# The give at full strength: fist target (torso px, x+ left, z+ forward) and shield face (animation degrees).
LANE_GIVE = ((3.0, -0.8, -1.6), (0.0, 22.0, 0.0))


def _lane_lattice(cube, step=1.0):
    lo, hi = cube.corners.min(0), cube.corners.max(0)
    ax = [np.linspace(lo[i], hi[i], max(2, int(math.ceil((hi[i] - lo[i]) * 16 / step)) + 1)) for i in range(3)]
    g = np.stack(np.meshgrid(*ax, indexing="ij"), -1).reshape(-1, 3)
    on = np.zeros(len(g), bool)
    for i in range(3):
        on |= np.isclose(g[:, i], lo[i]) | np.isclose(g[:, i], hi[i])
    return g[on]


_LANE = {}


def _lane_setup(model):
    if _LANE:
        return _LANE
    pts = []
    for ci, b in enumerate(model.cube_bone):
        if b in LANE_BONES:
            cube = model.cube_list[ci]
            cp = cube.pivot / 16.0
            Cr = rg._T(*cp) @ rg._Rz(cube.rot[2]) @ rg._Ry(cube.rot[1]) @ rg._Rx(cube.rot[0]) @ rg._T(*(-cp))
            L = _lane_lattice(cube)
            pts.append((b, L @ Cr[:3, :3].T + Cr[:3, 3]))
    rects = [(c.corners.min(0)[:2] * 16, c.corners.max(0)[:2] * 16)
             for ci, c in enumerate(model.cube_list) if model.cube_bone[ci] == "shield"]
    _LANE.update(pts=pts, rects=rects)
    return _LANE


def lane_margin(frame, model=None):
    """Smallest lane margin (px, >= 0 is clear) of the sword and sword forearm against the shield for an upper frame
    {bone: (rot, pos)}; also how many of the points pass over the shield's face (in front of it)."""
    model = model or HEAVY
    L = _lane_setup(model)
    M = {}
    for n in model.order:
        b = model.bones[n]
        rot, pos = frame.get(n, (Z3, Z3))
        M[n] = _bone_mat(b, rot, pos, M[b.parent] if b.parent is not None else rg.WORLD_FLIP)
    Si = np.linalg.inv(M["shield"])
    worst, front = 1e9, 0
    for b, P in L["pts"]:
        W = P @ M[b][:3, :3].T + M[b][:3, 3]
        q = (W @ Si[:3, :3].T + Si[:3, 3]) * 16.0
        lat = np.full(len(q), 1e9)
        for lo, hi in L["rects"]:
            d = np.maximum(lo - q[:, :2], q[:, :2] - hi)
            out = np.linalg.norm(np.maximum(d, 0), axis=1)
            lat = np.minimum(lat, np.where(d.max(1) < 0, d.max(1), out))
        m = np.maximum(np.maximum(lat - LANE_PAD, LANE_FRONT - q[:, 2]), q[:, 2] - LANE_BACK)
        worst = min(worst, float(m.min()))
        front += int(((lat < LANE_PAD) & (q[:, 2] < LANE_FRONT)).sum())
    return worst, front


def _give(frame, s, face):
    """frame with the shield given way by s (0..1) of LANE_GIVE, face being the frame's shield face before the give."""
    if s <= 0:
        return frame
    arm, fore = frame["left_arm"][0], frame["left_forearm"][0][0]
    fist = _fist(arm, fore) - SHOULDER_D
    tgt = fist + s * np.array(LANE_GIVE[0])
    a2, f2 = fist_arm(tgt, arm[2], guess=(arm[0], arm[1], fore))
    fc = tuple(f + s * g for f, g in zip(face, LANE_GIVE[1]))
    out = dict(frame)
    out["left_arm"], out["left_forearm"] = (a2, None), ((f2, 0.0, 0.0), None)
    out["left_hand"] = (grip(a2, f2, fc), None)
    return out


def lane_fix(keys, n, lead=2.0, hold_after=1.0):
    """Samples keys (n + 1 samples), giving the shield way wherever the sword lane needs it: the least give per sample that
    clears it (to LANE_GIVE), widened lead ticks before and hold_after ticks after, eased, then checked again."""
    raw = [sample_keys(keys, i / SUB, keep_face=True) for i in range(n + 1)]
    faces = [r["_face"][0] for r in raw]
    base = [regrip(dict(r)) for r in raw]
    need = np.zeros(n + 1)
    for i, f in enumerate(base):
        if lane_margin(f)[0] >= 0:
            continue
        for s in np.linspace(0.1, 1.0, 10):
            if lane_margin(_give(f, s, faces[i]))[0] >= 0:
                need[i] = s
                break
        else:
            need[i] = 1.0
    a, b = int(lead * SUB), int(hold_after * SUB)
    wide = np.array([need[max(0, i - b):i + a + 1].max() for i in range(n + 1)])
    k = np.array([1, 2, 3, 4, 3, 2, 1], float)
    k /= k.sum()
    soft = np.convolve(np.r_[[wide[0]] * 3, wide, [wide[-1]] * 3], k, mode="valid")
    s = np.maximum(soft, need)
    s[0] = s[-1] = 0.0 if need[0] == need[-1] == 0 else s[0]
    # the eased give can itself close the lane where the raw pose was clear (the shield turns into an arm that was beside
    # it): those samples take the nearest give that clears, pinned, and the rest are eased around them again
    pinned = np.zeros(n + 1, bool)
    grid = np.linspace(0.0, 1.0, 21)
    for _ in range(3):
        bad = [i for i in range(n + 1) if lane_margin(_give(base[i], float(s[i]), faces[i]))[0] < 0]
        if not bad:
            break
        for i in bad:
            ms = np.array([lane_margin(_give(base[i], float(g), faces[i]))[0] for g in grid])
            ok = grid[ms >= 0]
            s[i] = ok[np.argmin(abs(ok - s[i]))] if len(ok) else grid[np.argmax(ms)]
            pinned[i] = True
        sm = np.convolve(np.r_[[s[0]], s, [s[-1]]], np.array([1, 2, 1], float) / 4, mode="valid")
        s = np.where(pinned, s, sm)
        if need[0] == need[-1] == 0:
            s[0] = s[-1] = 0.0
    out = [_give(f, float(v), faces[i]) for i, (f, v) in enumerate(zip(base, s))]
    return out, s, [lane_margin(f) for f in out]


def monotone(ts, vs):
    """Fritsch-Carlson slopes: a smooth curve through the keys that never overshoots them, so extremes
    (the wind-up, the end of the follow-through) settle instead of bouncing."""
    n = len(ts)
    d = [(vs[i + 1] - vs[i]) / (ts[i + 1] - ts[i]) for i in range(n - 1)]
    m = [0.0] * n
    for i in range(1, n - 1):
        if d[i - 1] * d[i] > 0:
            w1 = 2 * (ts[i + 1] - ts[i]) + (ts[i] - ts[i - 1])
            w2 = (ts[i + 1] - ts[i]) + 2 * (ts[i] - ts[i - 1])
            m[i] = (w1 + w2) / (w1 / d[i - 1] + w2 / d[i])
    return m


def sample_keys(keys, t, keep_face=False):
    ts = [k[0] for k in keys]
    out = {}
    for b in UPPER_BONES + (("_face",) if "_face" in keys[0][1] else ()):
        rot = []
        for c in range(3):
            vs = [k[1][b][c] for k in keys]
            m = monotone(ts, vs)
            i = max(0, min(len(ts) - 2, next((j for j in range(len(ts) - 1) if t < ts[j + 1]), len(ts) - 2)))
            h = ts[i + 1] - ts[i]
            s = min(1.0, max(0.0, (t - ts[i]) / h))
            rot.append(hermite(vs[i], vs[i + 1], m[i] * h, m[i + 1] * h, s))
        out[b] = (tuple(rot), None)
    return regrip(out) if "_face" in out and not keep_face else out


# Each swing's impact tick: its attackVariants row's impactTicks (sheets), where the server lands the blow.
# Written into the clip as a "timeline" marker so the in-game check (GeoKnightChecks) can hold the two together.
IMPACT = {"thrust": 7, "overhead": 10, "low_cut": 8}


def curve(keys, t):
    """keys: (tick, (a, b, c)) pairs; the monotone curve through them at tick t, per component."""
    ts = [k[0] for k in keys]
    i = max(0, min(len(ts) - 2, next((j for j in range(len(ts) - 1) if t < ts[j + 1]), len(ts) - 2)))
    h = ts[i + 1] - ts[i]
    s = min(1.0, max(0.0, (t - ts[i]) / h))
    out = []
    for c in range(len(keys[0][1])):
        vs = [k[1][c] for k in keys]
        m = monotone(ts, vs)
        out.append(hermite(vs[i], vs[i + 1], m[i] * h, m[i + 1] * h, s))
    return tuple(out)


def feet_at(steps, t):
    """Each foot's (ankle px, yaw, sole pitch) at tick t: planted between steps; while stepping it travels on an eased
    line, lifted up to 1.4 px, the toe raised mid-swing and the heel striking first."""
    feet = {side: (np.array(p), y, 0.0) for side, (p, y) in READY_FEET.items()}
    for side, t0, t1, to, yaw, *lift in steps:
        lift = lift[0] if lift else 1.4     # 0: a skid, the sole dragged flat along the ground
        p0, y0, _ = feet[side]
        if t <= t0:
            continue
        u = min(1.0, (t - t0) / (t1 - t0))
        e = smooth(u)
        pos = p0 + (np.array(to) - p0) * e
        pos[1] += lift * math.sin(math.pi * u)
        feet[side] = (pos, y0 + (yaw - y0) * e, 10.0 * lift / 1.4 * math.sin(math.pi * u))
    return feet


def attack_legs(spec, ticks=ATTACK_TICKS):
    hips_rot = [(k[0], k[1]) for k in spec["hips"]]
    hips_pos = [(k[0], k[2]) for k in spec["hips"]]
    n = ticks * SUB
    out, guesses = [], {}
    for i in range(n + 1):
        t = i / SUB
        hips = (curve(hips_rot, t), curve(hips_pos, t))
        pose = {"hips": hips}
        f = {"hips": hips}
        feet = feet_at(spec["steps"], t)
        for side in ("right", "left"):
            pose[side + "_leg"] = ((0, -hips[0][1], 0), (0, 0, 0))
            at, yaw, pitch = feet[side]
            lr, sr, fr = leg_pose(pose, side, at, pitch, foot_yaw=yaw, guess=guesses.get(side))
            guesses[side] = (lr[0], lr[2], sr[0])
            f[side + "_leg"], f[side + "_shin"], f[side + "_foot"] = (lr, None), (sr, None), (fr, None)
        out.append(f)
    return out


def cape_lag(samples, legs=None):
    """Secondary motion (STYLE.md rule 9): the cape trails the body. It swings out behind by how fast the torso (and hips,
    standing) turned and pitched over the last two ticks, and sways against the turn, so it lags every drive and settles."""
    lag = 2 * SUB
    def body(i):
        tr = samples[max(0, i)]["torso"][0]
        hr = legs[max(0, i)]["hips"][0] if legs else Z3
        return np.array(tr) + np.array(hr)
    for i, s in enumerate(samples):
        d = body(i) - body(i - lag)
        swing = min(15.0, 0.45 * abs(d[0]) + 0.3 * abs(d[1]))
        sway = max(-5.0, min(5.0, -0.15 * d[1]))
        hang = 0.7 * max(0.0, body(i)[0])     # cloth hangs plumb: a forward lean lifts the back away under it
        s["cape"] = ((2.0 + swing - hang, 0.0, sway), None)
    return samples


def attack_clips():
    n = ATTACK_TICKS * SUB
    out = {}
    for name, keys in ATTACKS.items():
        legs = attack_legs(ATTACK_LEGS[name])
        if V2 and SHIELD:
            upper, give, lanes = lane_fix(keys, n)
            print("lane %-9s give max %.2f  margin min %.2f px  over-face samples %d" % (
                name, give.max(), min(m for m, _ in lanes), sum(1 for _, f in lanes if f)))
            upper = cape_lag(upper, legs)
        else:
            upper = cape_lag([sample_keys(keys, i / SUB) for i in range(n + 1)], legs)
        clip = bake(upper, ATTACK_TICKS, loop=False)
        clip["timeline"] = {fmt_t(IMPACT[name] / TPS): "impact"}
        out["attack_" + name] = clip
        out["attack_%s_legs" % name] = bake(legs, ATTACK_TICKS, loop=False)
    return out


# ------------------------------------------------------------------------------------------------
# Stances (unit animation rebuild, PLAN.md "Shield"): what the knight holds while it obeys an order.
# Played by GeoTroop's stance layer (StanceController), which crossfades to the order's pose within
# 2 ticks so the change reads at range. The upper poses go over any gait; the legs poses only while the
# knight stands (the layer scales them by how still it is), so a planted foot never slides.
#   ready      no order stance, but fighting: sword cocked, shield forward, left shoulder leading, feet set apart.
#   brace      Hold, out of formation: feet set wide, knees bent, the same guard over them.
#   wall       Hold, formed: a deep set, shoulder behind the shield, sword laid over its rim to stab.
#   *_up       the same with the shield raised (the server's raised shield: it blocks counters).
#   shield_up  shield raised while moving (shield_move, a raised shield on fall_back).
#   ward       fall_back with the shield down: backing off, point held at the enemy.
# Each loops 40 ticks with a slow breath so a held line is never a frozen statue.
# ------------------------------------------------------------------------------------------------

STANCE_TICKS = 40


STANCE_UPPER = {
    # ready stands on its own footing (STANCE_LEGS "ready"), so the shield is set upright over those hips.
    "ready": READY,
    # brace: the sword held low by the right hip but angled up and across, point at the enemy's chest past the shield's edge.
    "brace": skey((8, 16, 0), (-24, 0, 14), -52, -30, -10, sf("brace", (2.5, 18.0, 5.5)), hips=(0, 10, 0)),
    # *_up: the fist rises to the shoulder so the rim covers the eyes.
    "brace_up": skey((9, 18, 0), (-28, 0, 16), -56, -26, -10, sf("brace_up", (2.0, 22.5, 6.0)), hips=(0, 10, 0)),
    # wall: torso pitched well forward over the deep set; the arm rises to keep the sword level over the rim.
    "wall": skey((22, 24, 0), (-72, -30, 6), -40, -6, -6, sf("wall", (2.5, 18.5, 6.0)), hips=(4, 16, 0)),
    "wall_up": skey((24, 26, 0), (-88, -32, 4), -30, -2, -8, sf("wall_up", (2.0, 22.0, 6.0)), hips=(4, 16, 0)),
    "shield_up": skey((6, 16, 0), (-30, 0, 16), -76, -40, -8, sf("shield_up", (2.0, 22.0, 6.0))),
    # ward: leaning back as it gives ground, the sword held across the belly, point angled down at the enemy.
    "ward": skey((-6, 8, 0), (-30, 0, -6), -62, 42, 50, sf("ward", (3.0, 17.5, 5.0))),
}
if not SHIELD:   # no rim to hide behind (review 2026-11-06): the wall keeps chest and head up, blade angled up at the chest
    STANCE_UPPER["wall"] = STANCE_UPPER["wall_up"] = skey((12, 22, 0), (-62, -24, 8), -46, -28, -8, sf("wall", None),
                                                          hips=(4, 16, 0), head=(-14, -18, 0))

if V2 and SHIELD:
    # V2 (review round 2): the commands read apart at gameplay scale. ready stands tallest, shield upright; brace sinks a
    # step lower and wider, the shield's top tipped forward to meet the blow, the sword low on the right past the rim; the
    # wall is the deepest and most closed, chest down behind a shield tipped further forward, the sword level past the
    # shield's right edge for the next man's line; the ward leans back as it gives ground, the shield's top tipped back,
    # the point low and out on the sword side. The sword stays on its own lane, outside the rim, in every one.
    STANCE_UPPER.update({
        "brace": skey((12, 16, 0), (-24, 0, 30), -52, -20, 4, (5.5, 18.0, 6.0), hips=(2, 10, 0), tilt=5),
        "brace_up": skey((13, 18, 0), (-28, 0, 30), -56, -20, 4, (5.5, 22.0, 6.0), hips=(2, 10, 0), tilt=2),
        "wall": skey((22, 14, 0), (-52, 0, 26), -40, -6, 0, (6.0, 17.0, 6.0), hips=(4, 16, 0), tilt=9),
        "wall_up": skey((24, 14, 0), (-60, 0, 26), -36, -4, 0, (6.0, 21.0, 6.0), hips=(4, 16, 0), tilt=4),
        "shield_up": skey((6, 16, 0), (-30, 0, 26), -76, -40, 4, (5.5, 22.0, 6.0)),
        "ward": skey((-6, 8, 0), (-30, 0, 30), -50, 20, 12, (5.0, 18.0, 5.6), tilt=-6),
    })

# Feet (ankle targets, px) and hips (rot, pos) for the two set stances; yaw is each foot's turn (right, left).
STANCE_LEGS = {
    # ready: the fighting footing every standing swing plays over (Kort, 2026-11-05: not upright with the feet together).
    # Set a little wider and split fore and aft like the brace, knees soft, but shallower: a guard, not a hold.
    "ready": {"hips": ((0.0, 6.0, 0.0), (0.0, -1.1, 0.1)), "right": (-3.4, 2.0, 2.0), "left": (3.2, 2.0, -2.0),
              "yaw": (-12.0, 7.0)},
    "brace": {"hips": ((0.0, 10.0, 0.0), (0.0, -1.3, 0.2)), "right": (-3.0, 2.0, 2.6), "left": (2.8, 2.0, -2.6),
              "yaw": (-14.0, 8.0)},
    "wall": {"hips": ((4.0, 16.0, 0.0), (0.0, -3.6, 0.6)), "right": (-3.4, 2.0, 4.0), "left": (3.2, 2.0, -3.8),
             "yaw": (-20.0, 10.0)},
}


# How alive each stance's loop is (STYLE.md rule 2): the ready guard breathes visibly and rocks its weight centre, forward,
# centre, back (WEIGHT, a quarter cycle behind the breath); the brace and the wall hold still behind their shields.
STANCE_LIFE = {"ready": 3.0 if SHIELD else 4.2}
if V2 and SHIELD:
    STANCE_LEGS["brace"] = {"hips": ((2.0, 10.0, 0.0), (0.0, -2.2, 0.5)), "right": (-3.8, 2.0, 2.8), "left": (3.6, 2.0, -2.8),
                            "yaw": (-16.0, 9.0)}   # no shield to hold still: the shieldless guard rocks more
WEIGHT = 0.5 * math.pi


def stance_upper(pose, life=1.0):
    n = STANCE_TICKS * SUB
    out = []
    for i in range(n + 1):
        u = (i % n) / n
        br = life * math.sin(2 * math.pi * u)
        wt = (life - 1.0) * math.sin(2 * math.pi * u - WEIGHT)   # + weight forward
        wl = (life - 1.0) * math.sin(2 * math.pi * u - WEIGHT - 0.7)   # the arms trail the torso by a few ticks
        f = {b: (tuple(v), None) for b, v in pose.items()}
        tx, ty, tz = pose["torso"]
        f["torso"] = ((tx + 0.8 * br + 1.8 * wt, ty - 1.4 * wt, tz - 1.2 * wt), None)
        hx, hy, hz = pose["head"]
        f["head"] = ((hx - 0.5 * br - 1.3 * wt, hy + 0.9 * wt, hz + 1.0 * wt), None)
        ax, ay, az = pose["right_arm"]
        f["right_arm"] = ((ax - 0.6 * br - 1.3 * wl, ay, az), None)
        sx, sy, sz = pose["left_arm"]
        f["left_arm"] = ((sx - 0.8 * br - 1.0 * wl, sy, sz), None)
        if not SHIELD:   # the free hand counter-swings the weight and bobs with the breath, ahead of the hip
            f["left_arm"] = ((sx - 1.2 * br + 2.2 * wl, sy, sz - 1.3 * wl), None)
            f["left_forearm"] = ((pose["left_forearm"][0] - 1.6 * br, 0.0, 0.0), None)
        fx_, fy_, fz_ = f["_face"][0]
        lag = life * math.sin(2 * math.pi * u - 0.6)       # the shield settles a beat after the breath
        f["_face"] = ((fx_ + 1.5 * lag, fy_ + 0.8 * lag, fz_ - 0.6 * lag), None)
        out.append(regrip(f))
    return bake(out, STANCE_TICKS)


def stance_legs(spec, life=1.0):
    n = STANCE_TICKS * SUB
    out, guesses = [], {}
    for i in range(n + 1):
        u = (i % n) / n
        br = life * math.sin(2 * math.pi * u)
        wt = (life - 1.0) * math.sin(2 * math.pi * u - WEIGHT)
        (rx, ry, rz), (px, py, pz) = spec["hips"]
        # the weight rides forward over the lead foot (hips forward, a touch lower, tipped in) and back over the rear one
        # and sideways onto it (readable from the front): the hips slide and roll toward the lead side, the lead knee sinks
        hips = ((rx + 1.5 * wt, ry - 2.2 * wt, rz + 1.6 * wt),
                (px - 0.35 * wt, py - 0.1 * br - 0.12 * abs(wt) - 0.22 * max(0.0, wt), pz - 0.6 * wt))
        pose = {"hips": hips}
        f = {"hips": hips}
        for side in ("right", "left"):
            pose[side + "_leg"] = ((0, -ry, 0), (0, 0, 0))
            yaw = spec["yaw"][0 if side == "right" else 1]
            lr, sr, fr = leg_pose(pose, side, spec[side], 0.0, foot_yaw=yaw, guess=guesses.get(side))
            guesses[side] = (lr[0], lr[2], sr[0])
            f[side + "_leg"], f[side + "_shin"], f[side + "_foot"] = (lr, None), (sr, None), (fr, None)
        out.append(f)
    return bake(out, STANCE_TICKS)


def stance_clips():
    out = {"stance_%s_upper" % k: stance_upper(v, STANCE_LIFE.get(k, 1.0)) for k, v in STANCE_UPPER.items()}
    out.update({"stance_%s_legs" % k: stance_legs(v, STANCE_LIFE.get(k, 1.0)) for k, v in STANCE_LEGS.items()})
    return out


# ------------------------------------------------------------------------------------------------
# Hit reactions (PLAN.md "Damage"): additive. HitController adds these offsets to whatever the other
# layers wrote, so a knight flinches mid-swing or mid-stride without losing it. Each starts and ends at
# zero. Four directions per weight (where the blow came from); the layer mixes them by the hit's
# bearing, so a blow from the front-right gives a little of both. A blocked blow plays block instead:
# the shield arm is driven back and the body absorbs it, scaled by the blow's weight.
# ------------------------------------------------------------------------------------------------

def off(torso=Z3, head=Z3, rarm=Z3, rfore=0.0, larm=Z3, lfore=0.0):
    return {"torso": torso, "head": head, "right_arm": rarm, "right_forearm": (rfore, 0.0, 0.0), "right_hand": Z3,
            "left_arm": larm, "left_forearm": (lfore, 0.0, 0.0), "left_hand": Z3}


ZERO = off()


def wrap(a):
    return (a + 180.0) % 360.0 - 180.0


def shield_keep(o, keep=0.35, tilt=0.0):
    """o with a left_hand offset that keeps the shield nearer upright under o's torso and arm offsets (from the ready
    stance), so a blow rocks it without laying it flat; tilt (+ top forward) adds a rock of its own."""
    r = STANCE_UPPER["ready"]
    arm = tuple(a + b for a, b in zip(r["left_arm"], o["left_arm"]))
    fore = r["left_forearm"][0] + o["left_forearm"][0]
    face = tuple(f - keep * t for f, t in zip(r["_face"], o["torso"]))
    face = (face[0] + tilt, face[1], face[2])
    base = grip(r["left_arm"], r["left_forearm"][0], r["_face"])
    o = dict(o)
    o["left_hand"] = tuple(wrap(a - b) for a, b in zip(grip(arm, fore, face), base))
    return o


def scaled(o, k):
    return {b: tuple(k * c for c in v) for b, v in o.items()}


# The peak offset of a light blow from each direction, degrees. Front: the chest is driven back and the
# head snaps back; back: the body is pitched forward; right: the right shoulder is driven back and the
# body bends away to the left; left mirrors it.
# Big and readable (STYLE.md rule 8): a medium blow (2.2x these) throws the torso about 30 degrees and flings the sword arm wide.
HIT_PEAK = {
    "front": shield_keep(off(torso=(-13, 5, 0), head=(-10, -2, 0), rarm=(-14, 0, 30), larm=(-4, 0, -6)), tilt=-5),
    "back": shield_keep(off(torso=(10, 0, 0), head=(4, 0, 0), rarm=(8, 0, 14), larm=(8, 0, -6)), tilt=4),
    "right": shield_keep(off(torso=(-4, 12, 7), head=(0, 5, 3), rarm=(8, 0, 22), larm=(-3, 0, -4))),
    "left": shield_keep(off(torso=(-4, -12, -7), head=(0, -5, -3), rarm=(-4, 0, 12), larm=(8, 0, -8))),
}
if not SHIELD:   # no shield to take it (review 2026-11-06): thrown harder, twisting back, both arms flung wide
    HIT_PEAK = {
        "front": shield_keep(off(torso=(-17, 10, 0), head=(-12, -3, 0), rarm=(-20, 0, 42), larm=(-10, 0, -18), lfore=-12)),
        "back": shield_keep(off(torso=(12, 0, 0), head=(5, 0, 0), rarm=(10, 0, 20), larm=(10, 0, -14))),
        "right": shield_keep(off(torso=(-5, 14, 8), head=(0, 6, 3), rarm=(10, 0, 28), larm=(-6, 0, -10))),
        "left": shield_keep(off(torso=(-5, -14, -8), head=(0, -6, -3), rarm=(-6, 0, 16), larm=(10, 0, -16))),
    }
HIT_TICKS = {"light": 9, "medium": 16}
LIGHT = 0.5   # a light flinch reaches half the stagger's first key


def hit_keys(peak, weight):
    if weight == "light":    # a flinch: snap to the peak in 1.5 ticks, a small rebound, settled by t9
        p = scaled(peak, LIGHT)
        return [(0, ZERO), (1.5, p), (4, scaled(p, -0.15)), (6.5, scaled(p, 0.05)), (9, ZERO)]
    # a stagger: thrown and held a beat, a quick catch, a gather that overshoots centre a little, then planted in guard
    return [(0, ZERO), (2, scaled(peak, 2.2)), (4, scaled(peak, 2.05)), (7, scaled(peak, 0.35)), (10, scaled(peak, -0.4)),
            (13, scaled(peak, 0.08)), (16, ZERO)]


# A blocked blow (STYLE.md rule 7): the shield is driven back into the chest and the body gives (absorb), holds, then shoves
# the shield out past the guard before settling.
BLOCK_GUARD = shield_keep(off(torso=(3, -9, 0), head=(-2, 5, 0), rarm=(-2, 0, 4), larm=(-16, -6, -4), lfore=14), tilt=2)
BLOCK_ABSORB = shield_keep(off(torso=(-16, -14, 0), head=(-8, 7, 0), rarm=(-6, 0, 14), larm=(18, -7, -7), lfore=-30), tilt=-13)
BLOCK_SHOVE = shield_keep(off(torso=(11, -8, 0), head=(-3, 5, 0), rarm=(4, 0, 2), larm=(-50, -10, -7), lfore=52), tilt=11)
BLOCK_TICKS = 11


def hit_clips():
    out = {}
    for weight, ticks in HIT_TICKS.items():
        for d, peak in HIT_PEAK.items():
            keys = hit_keys(peak, weight)
            if V2 and SHIELD:
                samples = hit_samples(keys, weight, d, ticks)
            else:
                samples = [sample_keys(keys, i / SUB) for i in range(ticks * SUB + 1)]
            out["hit_%s_%s" % (weight, d)] = bake(samples, ticks, loop=False)
    keys = [(0, ZERO), (0.8, BLOCK_GUARD), (2.2, BLOCK_ABSORB), (3.8, scaled(BLOCK_ABSORB, 0.85)), (5.6, BLOCK_SHOVE),
            (6.8, scaled(BLOCK_SHOVE, 0.9)), (9, scaled(BLOCK_SHOVE, 0.15)), (BLOCK_TICKS, ZERO)]
    if V2 and SHIELD:
        keys = BLOCK_KEYS
    out["hit_block"] = bake([sample_keys(keys, i / SUB) for i in range(BLOCK_TICKS * SUB + 1)], BLOCK_TICKS, loop=False)
    out.update(hit_legs_clips())
    return out


# Footwork under the hit reactions (STYLE.md rules 4 and 8; the reel's hit "catch, guard, gather, plant" and block "skid, guard,
# absorb, shove, plant, settle"). Absolute hips keys and foot steps like ATTACK_LEGS, from and to the ready footing; baked as
# offsets from it, because HitController adds them (times how still the troop stands) on top of the stance legs. A stagger
# skids the struck-side foot (lift 0: dragged flat), then lifts and replants it on the gather; a flinch only gives at the knees.
HIT_LEGS = {
    "front": {"hips": [(0, *READY_HIPS), (2, (-5, 9, 0), (0.0, -1.7, 0.9)), (4, (-4, 9, 0), (0.0, -1.8, 1.0)),
                       (7, (2, 4, 0), (0.0, -1.6, 0.3)), (10, (-1, 7, 0), (0.0, -1.2, 0.0)), (16, *READY_HIPS)],
              "steps": [("right", 0.4, 2.6, (-3.4, 2.0, 0.9), -14.0, 0.0), ("right", 7.5, 11.5, (-3.4, 2.0, 2.0), -12.0, 1.0)]},
    "back": {"hips": [(0, *READY_HIPS), (2, (5, 4, 0), (0.0, -1.6, -0.7)), (4, (4, 4, 0), (0.0, -1.7, -0.8)),
                      (7, (-1, 7, 0), (0.0, -1.4, -0.2)), (10, (0.5, 6, 0), (0.0, -1.15, 0.15)), (16, *READY_HIPS)],
             "steps": [("right", 0.4, 2.6, (-3.4, 2.0, 3.0), -12.0, 0.0), ("right", 7.5, 11.5, (-3.4, 2.0, 2.0), -12.0, 1.0)]},
    "right": {"hips": [(0, *READY_HIPS), (2, (-2, 14, 3), (0.0, -1.6, 0.5)), (4, (-2, 13, 3), (0.0, -1.7, 0.5)),
                       (7, (1, 4, 0), (0.0, -1.4, 0.1)), (10, (0, 6.5, 0), (0.0, -1.15, 0.1)), (16, *READY_HIPS)],
              "steps": [("right", 0.4, 2.6, (-3.6, 2.0, 1.0), -20.0, 0.0), ("right", 7.5, 11.5, (-3.4, 2.0, 2.0), -12.0, 1.0)]},
    "left": {"hips": [(0, *READY_HIPS), (2, (-2, -2, -3), (0.0, -1.6, 0.5)), (4, (-2, -1, -3), (0.0, -1.7, 0.5)),
                      (7, (1, 8, 0), (0.0, -1.4, 0.1)), (10, (0, 5.5, 0), (0.0, -1.15, 0.1)), (16, *READY_HIPS)],
             "steps": [("left", 0.4, 2.6, (3.4, 2.0, -2.9), 10.0, 0.0), ("left", 7.5, 11.5, (3.2, 2.0, -2.0), 7.0, 1.0)]},
}
BLOCK_LEGS = {"hips": [(0, *READY_HIPS), (1.2, (-4.5, 9, 0), (0.0, -2.0, 1.15)), (3.2, (-3, 9, 0), (0.0, -2.75, 1.3)),
                       (5.8, (8, 0, 0), (0.0, -1.85, -0.95)), (7, (7, 1, 0), (0.0, -1.75, -0.85)), (9, (1, 5, 0), (0.0, -1.2, 0.0)),
                       (BLOCK_TICKS, *READY_HIPS)],
              "steps": [("right", 0.2, 1.6, (-3.4, 2.0, 0.7), -13.0, 0.0), ("left", 0.2, 1.6, (3.2, 2.0, -3.1), 7.0, 0.0),
                        ("right", 5.0, 8.0, (-3.4, 2.0, 2.0), -12.0, 1.0), ("left", 6.5, 10.0, (3.2, 2.0, -2.0), 7.0, 0.3)]}
if V2 and SHIELD:
    # V2 (review round 2): graded. A light blow is a flinch at the knees. A medium blow shifts the pelvis well off the footing
    # and the knight takes a short lifted recovery step to catch it (rear foot back for a blow from the front, lead foot
    # forward from behind, the far foot out sideways), then walks the foot home. A blocked blow is taken at the shield
    # elbow first; the body only follows a beat later and gives less, and the feet barely skid.
    LIGHT = 0.4

    def hit_keys(peak, weight):
        if weight == "light":
            p = scaled(peak, LIGHT)
            return [(0, ZERO), (1.5, p), (4, scaled(p, -0.12)), (6.5, scaled(p, 0.04)), (9, ZERO)]
        return [(0, ZERO), (2, scaled(peak, 2.3)), (4.5, scaled(peak, 2.15)), (7.5, scaled(peak, 0.55)), (10.5, scaled(peak, -0.35)),
                (13, scaled(peak, 0.08)), (16, ZERO)]

    # V2 (Kort, 2026-11-06): the sword arm opens up under a blow, it never folds in. A blow from the front knocks it a little
    # wide (the elbow lifts off the body, the point swings out to the right and a touch back) and it is back on guard in about
    # 5 ticks; from behind or the sides it keeps its pitch but turns out, not in, and never overshoots back across the body.
    # The fist and the blade stay on the sword side and outside the shield. The torso, head and shield arm are unchanged.
    HIT_ARM = {d: dict(ZERO, right_arm=v) for d, v in (("front", (-8, 14, -8)), ("back", (8, 4, 0)), ("right", (8, 4, 0)),
                                                         ("left", (-4, 8, 0)))}

    def hit_arm_keys(d, weight):
        a = HIT_ARM[d]
        if d == "front":
            if weight == "light":
                return [(0, ZERO), (1.5, scaled(a, 0.6)), (3.5, scaled(a, 0.2)), (5.5, ZERO), (9, ZERO)]
            return [(0, ZERO), (2, a), (3.5, scaled(a, 0.6)), (5.5, scaled(a, 0.15)), (7, ZERO), (16, ZERO)]
        if weight == "light":
            return [(0, ZERO), (1.5, scaled(a, LIGHT)), (4, scaled(a, 0.05)), (6.5, ZERO), (9, ZERO)]
        return [(0, ZERO), (2, scaled(a, 2.3)), (4.5, scaled(a, 2.15)), (7.5, scaled(a, 0.55)), (10.5, ZERO), (16, ZERO)]

    def hit_samples(keys, weight, d, ticks):
        ak = hit_arm_keys(d, weight)
        out = []
        for i in range(ticks * SUB + 1):
            s, a = sample_keys(keys, i / SUB), sample_keys(ak, i / SUB)
            s.update({b: a[b] for b in ("right_arm", "right_forearm", "right_hand")})
            out.append(s)
        return out

    HIT_LEGS = {
        "front": {"hips": [(0, *READY_HIPS), (2, (-6, 9, 0), (0.0, -1.9, 1.6)), (4.5, (-5, 9, 0), (0.0, -2.1, 1.9)),
                           (7.5, (1, 5, 0), (0.0, -1.8, 1.3)), (10.5, (-1, 7, 0), (0.0, -1.3, 0.5)), (16, *READY_HIPS)],
                  "steps": [("left", 1.0, 3.8, (3.3, 2.0, -4.0), 9.0, 0.8), ("right", 3.5, 6.5, (-3.4, 2.0, 0.6), -13.0, 0.6),
                            ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8), ("right", 11.5, 15.0, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
        "back": {"hips": [(0, *READY_HIPS), (2, (6, 4, 0), (0.0, -1.8, -1.2)), (4.5, (5, 4, 0), (0.0, -1.9, -1.4)),
                          (7.5, (-1, 7, 0), (0.0, -1.6, -0.8)), (10.5, (0.5, 6, 0), (0.0, -1.2, -0.2)), (16, *READY_HIPS)],
                 "steps": [("right", 1.0, 3.8, (-3.5, 2.0, 3.5), -12.0, 0.8), ("left", 3.5, 6.5, (3.2, 2.0, -0.6), 7.0, 0.6),
                           ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8), ("right", 11.5, 15.0, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
        "right": {"hips": [(0, *READY_HIPS), (2, (-2, 14, 4), (1.0, -1.8, 0.5)), (4.5, (-2, 13, 4), (1.2, -1.9, 0.5)),
                           (7.5, (1, 4, 0), (0.6, -1.6, 0.2)), (10.5, (0, 6.5, 0), (0.1, -1.2, 0.1)), (16, *READY_HIPS)],
                  "steps": [("left", 1.0, 3.8, (4.9, 2.0, -2.2), 12.0, 0.8), ("left", 9.0, 12.5, (3.2, 2.0, -2.0), 7.0, 0.8)]},
        "left": {"hips": [(0, *READY_HIPS), (2, (-2, -2, -4), (-1.0, -1.8, 0.5)), (4.5, (-2, -1, -4), (-1.2, -1.9, 0.5)),
                          (7.5, (1, 8, 0), (-0.6, -1.6, 0.2)), (10.5, (0, 5.5, 0), (-0.1, -1.2, 0.1)), (16, *READY_HIPS)],
                 "steps": [("right", 1.0, 3.8, (-5.0, 2.0, 1.6), -18.0, 0.8), ("right", 9.0, 12.5, (-3.4, 2.0, 2.0), -12.0, 0.8)]},
    }
    BLOCK_ELBOW = shield_keep(off(torso=(-1, -2, 0), head=(-1, 1, 0), rarm=(-1, 0, 2), larm=(14, -5, -6), lfore=-26), tilt=-6)
    BLOCK_ABSORB = shield_keep(off(torso=(-8, -8, 0), head=(-5, 4, 0), rarm=(-4, 0, 8), larm=(18, -7, -7), lfore=-30), tilt=-10)
    BLOCK_SHOVE = shield_keep(off(torso=(7, -6, 0), head=(-2, 4, 0), rarm=(3, 0, 2), larm=(-40, -8, -6), lfore=44), tilt=9)
    BLOCK_KEYS = [(0, ZERO), (1.0, BLOCK_ELBOW), (2.6, BLOCK_ABSORB), (4.2, scaled(BLOCK_ABSORB, 0.85)), (6.0, BLOCK_SHOVE),
                  (7.2, scaled(BLOCK_SHOVE, 0.9)), (9, scaled(BLOCK_SHOVE, 0.15)), (BLOCK_TICKS, ZERO)]
    BLOCK_LEGS = {"hips": [(0, *READY_HIPS), (1.2, (-1, 7, 0), (0.0, -1.3, 0.3)), (3.0, (-3, 8, 0), (0.0, -1.9, 0.75)),
                           (5.8, (4, 2, 0), (0.0, -1.6, -0.4)), (8, (1, 5, 0), (0.0, -1.2, 0.0)), (BLOCK_TICKS, *READY_HIPS)],
                  "steps": [("right", 1.4, 3.0, (-3.4, 2.0, 1.3), -13.0, 0.0), ("right", 6.0, 9.0, (-3.4, 2.0, 2.0), -12.0, 0.6)]}


def legs_offset(samples):
    """Absolute leg samples minus the ready footing: rotation and hips position offsets for an additive layer."""
    ready = attack_legs({"hips": [(0, *READY_HIPS), (1, *READY_HIPS)], "steps": []}, ticks=1)[0]
    out = []
    for s in samples:
        f = {}
        for b, (rot, pos) in s.items():
            r0, p0 = ready[b]
            f[b] = (tuple(wrap(a - c) for a, c in zip(rot, r0)),
                    tuple(a - c for a, c in zip(pos, p0)) if pos is not None else None)
        out.append(f)
    return out


def hit_legs_clips():
    out = {}
    r_rot, r_pos = READY_HIPS
    for d, spec in HIT_LEGS.items():
        med = HIT_TICKS["medium"]
        out["hit_medium_%s_legs" % d] = bake(legs_offset(attack_legs(spec, ticks=med)), med, loop=False)
        k2 = spec["hips"][1]   # a flinch: the knees give a little toward the stagger's first key, no step
        mid = lambda k: (tuple(a + k * (b - a) for a, b in zip(r_rot, k2[1])), tuple(a + k * (b - a) for a, b in zip(r_pos, k2[2])))
        lt = HIT_TICKS["light"]
        light = {"hips": [(0, *READY_HIPS), (1.5, *mid(0.45)), (4, *mid(-0.08)), (lt, *READY_HIPS)], "steps": []}
        out["hit_light_%s_legs" % d] = bake(legs_offset(attack_legs(light, ticks=lt)), lt, loop=False)
    out["hit_block_legs"] = bake(legs_offset(attack_legs(BLOCK_LEGS, ticks=BLOCK_TICKS)), BLOCK_TICKS, loop=False)
    return out


# ------------------------------------------------------------------------------------------------
# Clip assembly
# ------------------------------------------------------------------------------------------------

def unwrap(seq):
    """Removes 360-degree jumps from a sequence of angles."""
    out = [seq[0]]
    for a in seq[1:]:
        while a - out[-1] > 180:
            a -= 360
        while a - out[-1] < -180:
            a += 360
        out.append(a)
    return out


def bake(samples, ticks, loop=True):
    """samples: list over time of {bone: (rot, pos|None)} at SUB per tick, the last equal to the first
    for a loop. Returns a GeckoLib animation dict with one linear keyframe per sample."""
    bones = sorted({b for s in samples for b in s})
    out = {}
    for b in bones:
        ch = {}
        rots = [s[b][0] for s in samples]
        cols = [unwrap([r[i] for r in rots]) for i in range(3)]
        rv = [[round(cols[0][i], 3), round(cols[1][i], 3), round(cols[2][i], 3)] for i in range(len(samples))]
        ch["rotation"] = {fmt_t(i / SUB / TPS): rv[i] for i in thin(rv, ROT_TOL)}
        if any(s[b][1] is not None for s in samples):
            pv = [[round(v, 4) for v in (s[b][1] or (0, 0, 0))] for s in samples]
            ch["position"] = {fmt_t(i / SUB / TPS): pv[i] for i in thin(pv, POS_TOL)}
        out[b] = ch
    return {"loop": loop, "animation_length": round(ticks / TPS, 6), "bones": out}


ROT_TOL = 0.02   # degrees: a dropped key is never further than this from the line through its kept neighbours
POS_TOL = 0.002  # pixels


def thin(vals, tol):
    """Indices of the keys to keep from evenly spaced samples: first, last, and every key that linear interpolation
    between the kept keys around it would miss by more than tol on any axis. GeckoLib (and AttackController.sample)
    scan keys linearly every frame, so a dense bake of a still channel costs draw time for nothing."""
    keep, a, n = [0], 0, len(vals)
    while a < n - 1:
        b = a + 1
        while b + 1 < n and all(abs(vals[a][k] + (vals[b + 1][k] - vals[a][k]) * (m - a) / (b + 1 - a) - vals[m][k]) <= tol
                                for m in range(a + 1, b + 1) for k in range(3)):
            b += 1
        keep.append(b)
        a = b
    return keep


def fmt_t(sec):
    return ("%.4f" % sec).rstrip("0").rstrip(".") if sec else "0.0"


def gait_legs(g):
    n = g.ticks * SUB
    guesses = {}
    for i in range(n):            # warm the IK guesses on one silent lap
        g.pose(i / n, guesses)
    samples = [g.pose(i / n, guesses) for i in range(n)]
    samples.append(samples[0])
    return bake(samples, g.ticks)


# ------------------------------------------------------------------------------------------------
# Death falls (PLAN.md "Death"). One clip per side the killing blow came from; DeathController picks it
# from the lethal hit's bearing and plays it over every bone, so while dead it is the only writer. The
# fall lands by BattleMath.DEATH_FALL_TICKS (20) and the last frame holds while the corpse lies and fades.
#   death_back     struck from the front: thrown back, knees give, lands on its back
#   death_forward  struck from behind: lurches, drops to its knees, pitches face down
#   death_left     struck from its right: the left knee buckles, it falls on its left side, rolling face down
#   death_right    struck from its left: the mirror
# Poses are keyed; between keys each bone eases ("in" speeds into an impact like a fall under gravity,
# "out" slows out of a bounce). The root is not keyed: it is solved per sample against the heaviest armour
# (t3) so the lowest cube corner stays on the ground and the body rolls about whatever touches it (a
# planted foot, then a knee, then a shoulder) without sliding.
# ------------------------------------------------------------------------------------------------

DEATH_TICKS = 20
HEAVY = rg.load_geo(PREVIEW / "t3.geo.json")
DEATH_BONES = ("root", "hips", "torso", "head", "cape", "right_arm", "right_forearm", "right_hand",
               "left_arm", "left_forearm", "left_hand", "right_leg", "right_shin", "right_foot",
               "left_leg", "left_shin", "left_foot")


def body_corners(pose, model=HEAVY):
    """World corners (px) of every cube of `model` and the bone each belongs to, for pose {bone: (rot, pos)}."""
    M = {}
    for n in model.order:
        b = model.bones[n]
        rot, pos = pose.get(n, (Z3, Z3))
        pos = pos or Z3
        r = b.rot + np.radians([-rot[0], -rot[1], rot[2]])
        pv = b.pivot / 16.0
        par = M[b.parent] if b.parent is not None else rg.WORLD_FLIP
        M[n] = (par @ rg._T(-pos[0] / 16.0, pos[1] / 16.0, pos[2] / 16.0) @ rg._T(*pv)
                @ rg._Rz(r[2]) @ rg._Ry(r[1]) @ rg._Rx(r[0]) @ rg._T(*(-pv)))
    pts, owner = [], []
    for ci, cube in enumerate(model.cube_list):
        cp = cube.pivot / 16.0
        Mc = (M[model.cube_bone[ci]] @ rg._T(*cp) @ rg._Rz(cube.rot[2]) @ rg._Ry(cube.rot[1])
              @ rg._Rx(cube.rot[0]) @ rg._T(*(-cp)))
        pts.append(cube.corners @ Mc[:3, :3].T + Mc[:3, 3])
        owner += [model.cube_bone[ci]] * len(cube.corners)
    return np.vstack(pts) * 16.0, owner


def _ease(kind, s):
    if kind == "in":
        return s * s
    if kind == "out":
        return 1.0 - (1.0 - s) ** 2
    if kind == "lin":
        return s
    return s * s * (3.0 - 2.0 * s)


def idle_start():
    """Idle frame 0, every death bone, so the fall blends in from where the knight stood."""
    legs, upper = idle_clips()
    out = {}
    for clip in (legs, upper):
        for b, ch in clip["bones"].items():
            pos = ch.get("position", {}).get("0.0")
            out[b] = (tuple(ch["rotation"]["0.0"]), tuple(pos) if pos else None)
    out["root"] = (Z3, None)
    return out


def mirror(keys):
    """The same fall to the other side: left and right bones swap and y/z rotations flip. Arms are left as
    given (the sword and shield are not mirror images), so a mirrored variant re-keys them."""
    sw = lambda b: ("left_" + b[6:] if b.startswith("right_") else "right_" + b[5:] if b.startswith("left_") else b)
    out = []
    for t, ease, k in keys:
        out.append((t, ease, {sw(b): (r[0], -r[1], -r[2]) for b, r in k.items()
                              if "arm" not in b and "hand" not in b}))
    return out


HELD = ("sword", "shield")   # bone-name parts of carried gear: it drags on the ground, the body never pivots on it
SOFT = ()   # bone-name prefixes of soft hanging gear (hair, kelp, tails): it never holds the body up or pins its roll


def death_clip(keys, start, name):
    """keys: [(tick, ease into this key, {bone: rot})]; each key changes only the bones it names."""
    frames = [(0, None, {b: start[b][0] for b in DEATH_BONES})]
    for t, ease, k in keys:
        f = dict(frames[-1][2])
        f.update({b: tuple(map(float, r)) for b, r in k.items()})
        frames.append((t, ease, f))
    hips_pos = start["hips"][1]
    samples = []
    base_y = None
    shift = np.zeros(3)
    prev = held = None
    settled = {}
    for i in range(DEATH_TICKS * SUB + 1):
        tick = i / SUB
        j = next(j for j in range(1, len(frames)) if tick <= frames[j][0] or j == len(frames) - 1)
        t0, _, a = frames[j - 1]
        t1, ease, b = frames[j]
        e = _ease(ease, min(1.0, max(0.0, (tick - t0) / (t1 - t0))))
        rot = {n: tuple(a[n][c] + (b[n][c] - a[n][c]) * e for c in range(3)) for n in DEATH_BONES}
        pose = {n: (rot[n], None) for n in DEATH_BONES}
        pose["hips"] = (rot["hips"], hips_pos)
        P, owner = body_corners(pose)
        if held is None:
            soft = np.array([any(o.startswith(h) for h in SOFT) for o in owner], dtype=bool)
            held = np.array([any(h in o for h in HELD) for o in owner]) | soft
        low = int(np.argmin(np.where(soft, np.inf, P[:, 1])))
        pivot = int(np.argmin(np.where(held, np.inf, P[:, 1])))
        if prev is None:
            base_y = P[low, 1]
        else:                       # roll about the body's lowest corner: it does not slide. The sword and shield
            d = prev[pivot] - P[pivot]  # drag on the ground instead, so a blade sweeping round never shoves the body.
            shift += (d[0], 0.0, d[2])
        lift = base_y - P[low, 1]
        prev = P
        pose["root"] = (rot["root"], (shift[0], lift, -shift[2]))
        if SOFT:
            settle_soft(pose, base_y, settled)
        samples.append(pose)
    clip = bake(samples, DEATH_TICKS, loop=False)
    clip["loop"] = "hold_on_last_frame"
    death_report(name, samples)
    return clip


SETTLE_TURN = 45.0   # deg per tick a soft strand may swing to stay out of the ground
SETTLE_GRID = [(x, z) for x in range(-90, 91, 6) for z in range(-90, 91, 6)]


def _bone_mat(b, rot, pos, par):
    pos = pos or Z3
    r = b.rot + np.radians([-rot[0], -rot[1], rot[2]])
    pv = b.pivot / 16.0
    return (par @ rg._T(-pos[0] / 16.0, pos[1] / 16.0, pos[2] / 16.0) @ rg._T(*pv)
            @ rg._Rz(r[2]) @ rg._Ry(r[1]) @ rg._Rx(r[0]) @ rg._T(*(-pv)))


def settle_soft(pose, ground, settled, model=None):
    """Hanging hair and kelp (SOFT) swing about their own pivot by the smallest turn that keeps them out of the
    ground, at most SETTLE_TURN per tick from the last sample, easing back as the body lifts off them. Writes
    each strand's turn into pose; settled carries the last turn per strand."""
    model = model or HEAVY
    soft = lambda n: any(n.startswith(h) for h in SOFT)
    M = {}
    for n in model.order:
        b = model.bones[n]
        rot, pos = pose.get(n, (Z3, None))
        M[n] = _bone_mat(b, rot, pos, M[b.parent] if b.parent is not None else rg.WORLD_FLIP)
    cap = SETTLE_TURN / SUB
    for r in model.order:
        b = model.bones[r]
        if not soft(r) or (b.parent is not None and soft(b.parent)):
            continue
        sub = [r]
        for n in model.order:
            if model.bones[n].parent in sub:
                sub.append(n)
        cubes = [ci for ci, cb in enumerate(model.cube_bone) if cb in sub]
        if not cubes:
            continue

        def low(d):
            L = {r: _bone_mat(b, (d[0], 0.0, d[1]), None, M[b.parent])}
            for n in sub[1:]:
                bn = model.bones[n]
                L[n] = _bone_mat(bn, pose.get(n, (Z3, None))[0], None, L[bn.parent])
            y = np.inf
            for ci in cubes:
                cube = model.cube_list[ci]
                cp = cube.pivot / 16.0
                Mc = (L[model.cube_bone[ci]] @ rg._T(*cp) @ rg._Rz(cube.rot[2]) @ rg._Ry(cube.rot[1])
                      @ rg._Rx(cube.rot[0]) @ rg._T(*(-cp)))
                y = min(y, float(((cube.corners @ Mc[:3, :3].T)[:, 1] + Mc[1, 3]).min() * 16.0))
            return y

        p = settled.get(r, (0.0, 0.0))
        near = [d for d in SETTLE_GRID if max(abs(d[0] - p[0]), abs(d[1] - p[1])) <= cap + 1e-6] + [p]
        near.sort(key=lambda d: d[0] ** 2 + d[1] ** 2 + 4.0 * ((d[0] - p[0]) ** 2 + (d[1] - p[1]) ** 2))
        best = None
        for d in near:
            if low(d) >= ground - 0.01:
                best = d
                break
        if best is None:
            best = max(near, key=low)
        settled[r] = best
        pose[r] = ((float(best[0]), 0.0, float(best[1])), None)


def death_report(name, samples):
    """Per bone, the lowest point at rest (px above ground): what the body lies on and whatever sinks."""
    P, owner = body_corners(samples[-1])
    root = samples[-1]["root"][1]
    low = {}
    for p, b in zip(P, owner):
        low[b] = min(low.get(b, 99.0), p[1])
    groups = {"head": ("head", "helm"), "torso": ("torso", "gorget", "mantle", "surcoat", "pauldron", "cape"),
              "hips": ("hips", "belt", "skirt"), "arms": ("arm", "couter", "cuff", "hand"), "shield": ("shield",),
              "sword": ("sword",), "legs": ("leg", "shin", "knee"), "feet": ("foot", "toe")}
    rep = {g: round(float(min([v for b, v in low.items() if any(k in b for k in ks)] or [99.0])), 2)
           for g, ks in groups.items()}
    print("  %-14s rest min-y %s  root %s" % (name, rep, tuple(round(float(v), 2) for v in root)))


# Struck from the front: the head snaps back, the arms fly up, the knees give; it tips over its heels,
# lands flat on its back, bounces once and lies with the head rolled aside and the sword arm flung out.
DEATH_BACK = [
    (3, "out", {"root": (-6, 0, 0), "torso": (-14, 0, 0), "head": (-22, 0, 0),
                "right_arm": (-60, 0, 25), "right_forearm": (-30, 0, 0), "left_arm": (-30, 30, -10),
                "right_leg": (-25, 0, 4), "left_leg": (-22, 0, -3), "right_shin": (50, 0, 0), "left_shin": (48, 0, 0)}),
    (11, "in", {"root": (-86, 0, 0), "hips": (0, 0, 0), "torso": (-6, 0, 0), "head": (10, 0, 0),
                "right_arm": (-20, 0, 80), "right_forearm": (-20, 0, 0), "left_arm": (-10, 10, -60),
                "right_leg": (-20, 0, 4), "right_shin": (25, 0, 0), "left_leg": (-40, 0, -6), "left_shin": (70, 0, 0),
                "right_foot": (20, 0, 0), "left_foot": (20, 0, 0), "cape": (8, 0, 0)}),
    (13, "out", {"root": (-78, 0, 0), "head": (20, 0, 0), "right_arm": (-30, 0, 75), "left_shin": (64, 0, 0),
                 "right_hand": (-45, -40, 50), "left_arm": (5, 5, -50), "left_forearm": (-20, 0, 0)}),
    # settled: the helm rolls aside, the sword hand drops open with the blade flat, the shield lies on the ground
    (16, "io", {"root": (-83, 0, 0), "head": (40, 22, 0), "right_arm": (18, 0, 80), "right_forearm": (0, 0, 0),
                "right_hand": (-90, -75, 105), "left_arm": (20, 0, -40), "left_forearm": (-30, 0, 0),
                "left_hand": (-180, 0, -90), "left_shin": (60, 0, 0)}),
    (20, "io", {"root": (-82, 0, 0), "head": (45, 28, 0), "right_arm": (20, 0, 80), "left_shin": (58, 0, 0)}),
]

# Struck from behind: the hips are driven forward and the head whips back, both knees drop to the ground,
# it sways there a beat and pitches face down, the shield arm thrown out to the side.
DEATH_FORWARD = [
    (3, "out", {"root": (8, 0, 0), "torso": (16, 0, 0), "head": (-15, 0, 0),
                "right_arm": (25, 0, 20), "left_arm": (0, 30, -15),
                "right_leg": (-20, 0, 4), "left_leg": (-20, 0, -4), "right_shin": (55, 0, 0), "left_shin": (55, 0, 0)}),
    # down on both knees, thighs upright and the body still straight, so the kneel reads from the side
    (7, "in", {"root": (-2, 0, 0), "hips": (0, 0, 0), "torso": (4, 0, 0), "head": (24, 0, 0),
               "right_arm": (-5, 0, 12), "right_forearm": (-15, 0, 0), "left_arm": (5, 25, -12),
               "right_leg": (0, 0, 4), "left_leg": (0, 0, -4), "right_shin": (92, 0, 0), "left_shin": (92, 0, 0),
               "right_foot": (30, 0, 0), "left_foot": (30, 0, 0)}),
    # it sways back a beat on its knees, the head lolling, then sags forward past balance
    (9, "out", {"root": (-7, 0, 0), "torso": (-2, 0, 0), "head": (12, 0, 0)}),
    (11, "io", {"root": (6, 0, 0), "torso": (12, 0, 0), "head": (30, 0, 0)}),
    (13, "in", {"root": (34, 0, 0), "torso": (10, 0, 0), "head": (25, 20, 0), "left_arm": (-10, 20, -40),
                "right_shin": (70, 0, 0), "left_shin": (70, 0, 0)}),
    (16, "in", {"root": (84, -18, 0), "torso": (8, 0, 0), "head": (-15, 0, 0),
                "right_arm": (0, 0, -25), "right_forearm": (-40, 0, 0), "right_hand": (0, -40, -80),
                "left_arm": (25, 0, -85), "left_forearm": (-40, 0, 0), "left_hand": (0, 0, 0),
                "right_leg": (0, 0, 4), "left_leg": (0, 0, -4), "right_shin": (10, 0, 0), "left_shin": (12, 0, 0),
                "cape": (10, 0, 0)}),
    (18, "out", {"root": (76, -19, 0), "head": (-25, 0, 0), "right_shin": (20, 0, 0), "left_shin": (22, 0, 0)}),
    (20, "io", {"root": (80, -20, 0), "torso": (10, 0, 0), "head": (-20, 0, 0), "right_arm": (0, 0, -30),
                "right_hand": (0, -45, -90), "left_arm": (30, 0, -90), "right_shin": (12, 0, 0), "left_shin": (14, 0, 0)}),
]

# Struck from its right: the head and chest are knocked to the left, the left knee buckles first, it goes
# down on its left side rolling toward face down, the sword arm draped forward over the body.
DEATH_LEFT = [
    (3, "out", {"root": (0, 0, 6), "torso": (0, 0, 12), "head": (0, -10, 15),
                "right_arm": (-30, 0, 40), "left_leg": (-25, 0, -4), "left_shin": (60, 0, 0)}),
    (11, "in", {"root": (5, 0, 82), "hips": (0, 0, 0), "torso": (6, 0, 4), "head": (0, 0, -20),
                "right_arm": (-40, 0, 10), "right_forearm": (-20, 0, 0), "left_arm": (-80, 20, 0),
                "left_leg": (-30, 0, -5), "left_shin": (70, 0, 0), "right_leg": (-50, 0, 0), "right_shin": (60, 0, 0)}),
    (13, "out", {"root": (6, -15, 78), "head": (0, 0, -10)}),
    # it rolls on over the shield arm to lie face down, the sword arm flung out with the blade flat
    (17, "io", {"root": (10, -70, 88), "torso": (0, 0, -8), "head": (-15, 0, 0),
                "right_arm": (-60, 0, 55), "right_forearm": (0, 0, 0), "right_hand": (-45, 45, 90),
                "left_arm": (25, 0, -30), "left_forearm": (-40, 0, 0), "left_hand": (0, 0, 0)}),
    (20, "io", {"root": (10, -80, 88), "torso": (0, 0, -10), "head": (-20, 0, 0),
                "right_arm": (-60, 0, 60), "left_arm": (30, 0, -30)}),
]

# Struck from its left: the mirror, with the shield arm draped and the sword arm under, reaching forward.
DEATH_RIGHT = mirror(DEATH_LEFT)
for _t, _e, _k in DEATH_RIGHT:
    if _t == 3:
        _k.update({"left_arm": (-20, 20, -30)})
    if _t == 11:
        _k.update({"right_arm": (-80, -20, 0), "right_forearm": (-20, 0, 0), "left_arm": (-40, 0, -10)})
    if _t == 17:
        _k.update({"left_arm": (-55, 0, -55), "left_forearm": (0, 0, 0), "left_hand": (0, 135, 90),
                   "right_arm": (25, 0, 25), "right_forearm": (-40, 0, 0), "right_hand": (-40, 70, -30)})
    if _t == 20:
        _k.update({"left_arm": (-60, 0, -60), "right_arm": (30, 0, 30), "right_hand": (-40, 80, -30)})

if V2 and SHIELD:
    # V2 (review round 2): support goes first (the knees buckle before the body is thrown), the pelvis drops, then the body
    # meets the ground in order (seat or knees, back or chest, then the head), the sword arm and the shield arm land on
    # beats of their own, and the body gives a small secondary settle before it lies still.
    DEATH_BACK = [
        (2, "out", {"root": (-3, 0, 0), "torso": (-6, 0, 0), "head": (-10, 0, 0), "right_arm": (-20, 0, 12),
                    "right_leg": (-14, 0, 4), "left_leg": (-16, 0, -3), "right_shin": (36, 0, 0), "left_shin": (44, 0, 0)}),
        (5, "in", {"root": (-18, 0, 0), "torso": (-16, 0, 0), "head": (-22, 0, 0),
                   "right_arm": (-60, 0, 28), "right_forearm": (-30, 0, 0), "left_arm": (-30, 30, -10),
                   "right_leg": (-42, 0, 4), "left_leg": (-46, 0, -3), "right_shin": (78, 0, 0), "left_shin": (84, 0, 0)}),
        # the seat lands first, the back still up
        (8.5, "in", {"root": (-52, 0, 0), "torso": (-12, 0, 0), "head": (-14, 0, 0), "right_arm": (-48, 0, 50),
                     "left_arm": (-22, 20, -30), "right_leg": (-36, 0, 4), "left_leg": (-46, 0, -6),
                     "right_shin": (60, 0, 0), "left_shin": (80, 0, 0)}),
        # then the back, the head still lifted a beat behind it
        (11, "in", {"root": (-86, 0, 0), "hips": (0, 0, 0), "torso": (-6, 0, 0), "head": (-8, 0, 0),
                    "right_arm": (-30, 0, 72), "right_forearm": (-20, 0, 0), "left_arm": (-10, 10, -55),
                    "right_leg": (-20, 0, 4), "right_shin": (25, 0, 0), "left_leg": (-40, 0, -6), "left_shin": (70, 0, 0),
                    "right_foot": (20, 0, 0), "left_foot": (20, 0, 0), "cape": (8, 0, 0)}),
        (12.5, "in", {"root": (-80, 0, 0), "head": (20, 0, 0), "left_shin": (64, 0, 0)}),
        # the sword arm drops and the blade lies flat
        (14.5, "io", {"right_arm": (18, 0, 80), "right_forearm": (0, 0, 0), "right_hand": (-90, -75, 105), "head": (34, 16, 0)}),
        # the shield arm lands on its own beat
        (16.5, "in", {"left_arm": (20, 0, -40), "left_forearm": (-30, 0, 0), "left_hand": (-180, 0, -90)}),
        # a small secondary settle: the body rocks back onto the ground and the knee sinks
        (18, "out", {"root": (-84, 0, 0), "head": (42, 26, 0), "left_shin": (56, 0, 0)}),
        (20, "io", {"root": (-82, 0, 0), "head": (45, 28, 0), "right_arm": (20, 0, 80), "left_shin": (58, 0, 0)}),
    ]
    DEATH_FORWARD = [
        (2, "out", {"root": (4, 0, 0), "torso": (6, 0, 0), "head": (-8, 0, 0), "right_arm": (12, 0, 14),
                    "right_leg": (-14, 0, 4), "left_leg": (-14, 0, -4), "right_shin": (42, 0, 0), "left_shin": (46, 0, 0)}),
        # the pelvis drops straight down onto both knees
        (5.5, "in", {"root": (-2, 0, 0), "hips": (0, 0, 0), "torso": (6, 0, 0), "head": (22, 0, 0),
                     "right_arm": (-5, 0, 12), "right_forearm": (-15, 0, 0), "left_arm": (5, 25, -12),
                     "right_leg": (0, 0, 4), "left_leg": (0, 0, -4), "right_shin": (92, 0, 0), "left_shin": (92, 0, 0),
                     "right_foot": (30, 0, 0), "left_foot": (30, 0, 0)}),
        (7.5, "out", {"root": (-7, 0, 0), "torso": (-2, 0, 0), "head": (12, 0, 0), "left_arm": (-20, 20, -40)}),
        # the arms come up and forward as it tips, so the chest, not the blade or the shield rim, meets the ground
        (10, "io", {"root": (6, 0, 0), "torso": (12, 0, 0), "head": (30, 0, 0), "right_arm": (-40, 0, 40),
                    "left_arm": (-50, 20, -50)}),
        (12.5, "in", {"root": (34, 0, 0), "torso": (10, 0, 0), "head": (25, 20, 0), "left_arm": (-130, 20, -70),
                      "right_arm": (-90, 0, 40), "right_forearm": (-20, 0, 0), "right_shin": (70, 0, 0), "left_shin": (70, 0, 0)}),
        # the chest lands, the head still up, both arms flung out past the head
        (14.5, "in", {"root": (84, -18, 0), "torso": (8, 0, 0), "head": (5, 10, 0), "right_arm": (-160, 0, 25),
                      "right_forearm": (-70, 0, 0), "left_arm": (-170, 0, -90), "left_forearm": (-80, 0, 0),
                      "right_leg": (0, 0, 4), "left_leg": (0, 0, -4), "right_shin": (10, 0, 0),
                      "left_shin": (12, 0, 0), "cape": (10, 0, 0)}),
        (15.5, "in", {"head": (-15, 0, 0)}),
        # the sword arm settles and the blade turns flat, then the shield arm
        (16.5, "io", {"right_arm": (-170, 0, 25), "right_forearm": (-70, 0, 0), "right_hand": (0, -42, -85)}),
        (18, "in", {"left_arm": (-175, 0, -95), "left_forearm": (-86, 0, 0), "left_hand": (0, 0, 0)}),
        (19, "out", {"root": (78, -20, 0), "head": (-25, 0, 0), "right_shin": (18, 0, 0), "left_shin": (20, 0, 0)}),
        (20, "io", {"root": (80, -20, 0), "torso": (10, 0, 0), "head": (-20, 0, 0), "right_arm": (-170, 0, 25),
                    "right_hand": (0, -45, -90), "left_arm": (-175, 0, -95), "right_shin": (12, 0, 0), "left_shin": (14, 0, 0)}),
    ]
    # the side falls: the far knee and the near knee give a beat before the body is knocked over, and a secondary settle
    DEATH_LEFT.insert(0, (1.6, "out", {"left_leg": (-12, 0, -3), "left_shin": (34, 0, 0), "right_shin": (18, 0, 0)}))
    DEATH_RIGHT.insert(0, (1.6, "out", {"right_leg": (-12, 0, 3), "right_shin": (34, 0, 0), "left_shin": (18, 0, 0)}))
    DEATH_LEFT.insert(-1, (18.5, "out", {"root": (10, -84, 90), "torso": (0, 0, -12)}))
    DEATH_RIGHT.insert(-1, (18.5, "out", {"root": (10, 84, -90), "torso": (0, 0, 12)}))


def pose_at(keys, start, tick):
    """Every death bone's rotation at `tick`, eased between the keys as death_clip does."""
    f, prev_t, prev = {b: start[b][0] for b in DEATH_BONES}, 0, None
    for kt, ease, k in keys:
        nxt = dict(f)
        nxt.update({b: tuple(map(float, r)) for b, r in k.items()})
        if tick <= kt:
            e = _ease(ease, (tick - prev_t) / (kt - prev_t)) if kt > prev_t else 1.0
            return {b: tuple(f[b][c] + (nxt[b][c] - f[b][c]) * e for c in range(3)) for b in DEATH_BONES}
        f, prev_t = nxt, kt
    return f


def roll_keys(keys, start, t0, overs):
    """Re-key from key t0 to the last key once per tick, keeping the motion, with per-tick bone overrides."""
    end = keys[-1][0]
    out = [k for k in keys if k[0] <= t0]
    for t in range(t0 + 1, end + 1):
        f = pose_at(keys, start, t)
        f.update(overs.get(t, {}))
        out.append((t, "lin", f))
    return out


# The roll: the arm the body rolls over sweeps up past the head so the body, not the shield or the sword,
# meets the ground (per-tick search; without it the gear props the body ~10px off the ground for 4 ticks),
# and it ends flung out above the head.
def _sweep(arm, path):
    return {t: {arm + "_arm": a, arm + "_forearm": (f, 0, 0), arm + "_hand": h} for t, (a, f, h) in path.items()}


ROLL_LEFT = _sweep("left", {14: ((-120, 0, -30), -60, (90, 0, 0)), 15: ((-150, 0, -30), -60, (90, 0, 0)),
                            16: ((-180, 0, -30), -60, (90, 0, 0)), 17: ((-180, 0, -30), -60, (90, 0, 0)),
                            18: ((-180, 0, -30), -60, (90, 0, 0)), 19: ((-180, 0, -30), -60, (90, 0, 0)),
                            20: ((-180, 0, -30), -60, (90, 0, 0))})
ROLL_RIGHT = _sweep("right", {14: ((-120, 0, 0), -30, (0, 0, 0)), 15: ((-150, 0, 0), -30, (0, 0, 0)),
                              16: ((-150, 0, 0), -60, (0, 0, 0)), 17: ((-180, 0, 0), -60, (0, 0, 0)),
                              18: ((-165, -15, -15), -45, (-25, 25, -25)),
                              # the sword arm settles out past the head, the blade turned flat on the ground
                              19: ((-150, -30, -30), -30, (-45, 45, -45)), 20: ((-150, -30, -30), -30, (-45, 45, -45))})
if V2 and SHIELD:   # V2: the shield arm swings wide as the body rolls onto it, so the rim does not prop the torso (4.1 -> 1.4 px)
    for _t, _a in ((15, (-48, 0, -72)), (16, (-53, 0, -88)), (17, (-55, 0, -70))):
        ROLL_RIGHT.setdefault(_t, {})["left_arm"] = _a


# ------------------------------------------------------------------------------------------------
# Deaths v3/v4 (Knight, docs/anim-rebuild/KNIGHT-REVIEW.md): twelve falls per troop (four directions, each keeping the gear,
# dropping the weapon, or dropping every piece). The system lives in art/anim/common/deaths.py, shared by every biped builder;
# this builder configures it with its own geometry, bake function and gear.
# ------------------------------------------------------------------------------------------------

D3_TROOPS = ("knight", "sentinel", "gl_marshal", "ic_hearthguard")   # one shield troop at a time (2026-11-07)
D3 = TROOP in D3_TROOPS and V2
D3_WPN = "sword" if TROOP == "knight" else "weapon"   # the held weapon's bone prefix (<prefix>_t1..t3)
sys.path.insert(0, str(HERE.parent / "common"))
import deaths  # noqa: E402


def d3_death_clips(start):
    deaths.configure(heavy=HEAVY, bake=bake, sub=SUB, ticks=DEATH_TICKS, bones=DEATH_BONES,
                     gear=deaths.gear(D3_WPN, shield=SHIELD))
    return deaths.d3_death_clips(start)


def death_clips():
    start = idle_start()
    if D3:
        return d3_death_clips(start)
    return {n: death_clip(k, start, n) for n, k in (("death_back", DEATH_BACK), ("death_forward", DEATH_FORWARD),
                                                     ("death_left", roll_keys(DEATH_LEFT, start, 13, ROLL_LEFT)),
                                                     ("death_right", roll_keys(DEATH_RIGHT, start, 13, ROLL_RIGHT)))}


# ------------------------------------------------------------------------------------------------
# Checks
# ------------------------------------------------------------------------------------------------

def report(g):
    """Contact drift per stance phase (px, world) and the lowest sole point during swing."""
    n = g.ticks * SUB
    guesses = {}
    for i in range(n):
        g.pose(i / n, guesses)
    res = {}
    for side, off in (("right", 0.0), ("left", 0.5)):
        x = SIDES[side]
        phases = {"heel": [], "flat": [], "toe": []}
        swing_min = 99.0
        stance_min = 99.0
        for i in range(n):
            u = off + i / n               # unwrapped time, so the ground offset never jumps a stride
            pose = g.pose(u % 1.0, guesses)
            pr = {b: (r, p if p is not None else (0, 0, 0)) for b, (r, p) in pose.items()}
            M = chain(pr, ("root", "hips", side + "_leg", side + "_shin", side + "_foot"))[side + "_foot"]
            heel = point(M, (x, 0, -HEEL[0])) + [0, 0, g.v * u * g.ticks]
            toe = point(M, (x, 0, -TOE[0])) + [0, 0, g.v * u * g.ticks]
            uu = i / n                    # this foot's own phase
            lowest = min(heel[1], toe[1])
            if uu < g.duty:
                s = uu / g.duty
                key = "heel" if s < Gait.HEEL_END else "toe" if s > Gait.TOE_START else "flat"
                phases[key].append(heel if key == "heel" else toe if key == "toe" else (heel + toe) / 2)
                stance_min = min(stance_min, lowest)
            elif 0.15 < (uu - g.duty) / (1 - g.duty) < 0.85:   # mid swing: the scuff zone
                swing_min = min(swing_min, lowest)
        drift = {}
        for k, ps in phases.items():
            if len(ps) > 1:
                p0 = ps[0]
                drift[k] = round(max(math.hypot(p[0] - p0[0], p[2] - p0[2]) for p in ps), 3)
        res[side] = {"drift_px": drift, "stance_min_y": round(stance_min, 3), "midswing_min_y": round(swing_min, 3)}
    return res


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    a = ap.parse_args()
    idle_legs, idle_upper = idle_clips()
    anims = {"walk_legs": gait_legs(WALK), "walk_upper": gait_upper(WALK, WALK_CARRY),
             "run_legs": gait_legs(RUN), "run_upper": gait_upper(RUN, RUN_CARRY),
             "idle_legs": idle_legs, "idle_upper": idle_upper, **attack_clips(), **stance_clips(), **hit_clips(),
             **death_clips()}
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1), encoding="utf-8")
    print("wrote", OUT, "clips:", ", ".join(anims))
    if a.check:
        for g in (WALK, RUN):
            print(g.name, "stride %.1f px, stance travel %.1f px" % (g.stride, g.travel), json.dumps(report(g)))


if __name__ == "__main__":
    main()
