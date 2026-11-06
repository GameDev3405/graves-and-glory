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

GEO = HERE / "out" / "A" / "preview" / "t1.geo.json"
OUT = HERE / "out" / "knight.animation.json"
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


def _fist(arm, fore):
    M = world({"left_arm": (arm, (0, 0, 0)), "left_forearm": ((fore, 0, 0), (0, 0, 0))}, "left_hand")
    return (M @ np.r_[MODEL.bones["left_hand"].pivot / 16.0, 1.0])[:3] * 16.0


def fist_arm(target, roll=-4.0, guess=None):
    """left_arm (x, y, roll) and left_forearm x putting the fist at target; the upper arm's roll (- out) is given."""
    x = np.array(guess if guess is not None else (-20.0, 20.0, -80.0), dtype=float)
    t = np.asarray(target, dtype=float)
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
    sh = hold((fx + 0.45 * math.sin(tw * (u - 0.05)), fy + 0.45 * bob, fz - c.shield * sw),
              (-0.65 * (hx + tx) - c.sway * trail + 1.2 * bob, -0.35 * net_yaw - 0.6 * c.sway * trail,
               -0.4 * net_roll + 0.4 * c.sway * math.sin(tw * (u - 0.1))))
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
        shd = hold((3.0 + 0.25 * sh, 17.5 + 0.35 * br, 4.5 + 0.2 * lag),
                   (-0.6 * tor[0] + 1.5 * lag, -0.4 * (hips[0][1] + tor[1]) + 1.2 * sh,
                    -0.4 * (hips[0][2] + tor[2]) - 1.0 * sh))
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


# Shield fist targets (fist_arm): the guard holds it before the left chest; a cut swings it out to the left.
FIST_GUARD = (2.5, 19.0, 5.5)
GUARD = key((2, 0, 0), (-6, 0, 6), -36, 0, 30, FIST_GUARD)

# (tick, pose). Anticipation draws the weapon away from the target, the drive accelerates into the impact
# key, follow-through carries past it, recovery settles back to the guard.
ATTACKS = {
    "thrust": [   # impact 7: body. Chamber the blade at the hip, then drive straight out with the shoulder.
        (0, GUARD),
        (4, key((-3, 20, 0), (22, 0, 14), -96, -4, 0, (2.0, 19.5, 6.0), throw=(4, 6, 0))),
        (7, key((13, -20, 0), (-88, 0, 2), -4, 4, -4, (6.5, 18.0, 3.5), throw=(-8, -14, -8))),
        (9, key((14, -22, 0), (-90, 0, 0), -2, 6, -6, (6.5, 18.0, 3.5), throw=(-10, -16, -10))),
        (12, key((10, -14, 0), (-55, 0, 4), -40, 0, 4, (5.0, 18.0, 4.0), throw=(-4, -8, -4))),   # draw the arm back: no held lunge
        (15, key((5, -4, 0), (-30, 0, 8), -50, -4, 14, (3.5, 18.5, 5.0))),
        (ATTACK_TICKS, GUARD),
    ],
    "overhead": [   # impact 10: head. Raise the blade behind the head, come over the top, cut down through.
        (0, GUARD),
        (6, key((-8, 12, 0), (-165, 0, 18), -70, -150, 0, (2.0, 20.0, 6.0), throw=(-6, 4, 4))),
        (8, key((2, 4, 0), (-150, 0, 14), -28, -80, 0, (2.5, 19.5, 6.0))),
        (10, key((12, -8, 0), (-100, 0, 6), -10, 22, 0, (5.0, 18.0, 4.0), throw=(-6, -10, -6))),
        (12, key((18, -14, 0), (-58, 0, 2), -12, 50, 0, (6.0, 17.5, 3.5), throw=(-10, -14, -10))),
        (14, key((18, -14, 0), (-44, 0, 2), -12, 58, 0, (6.0, 17.5, 3.5), throw=(-8, -12, -8))),
        (18, key((8, -4, 0), (-18, 0, 6), -34, 25, 18, (4.0, 18.0, 4.5))),
        (ATTACK_TICKS, GUARD),
    ],
    "low_cut": [   # impact 8: legs. Cock the blade high on the right, sweep down and across at the shins.
        (0, GUARD),
        (5, key((-2, 30, 4), (-30, 0, 55), -60, -40, 60, (2.0, 19.0, 6.0), throw=(4, 8, 2))),
        (8, key((20, -20, -4), (-55, 0, 0), -10, 40, -30, (6.0, 17.0, 3.5), throw=(-8, -14, -8))),
        (11, key((22, -32, -4), (-45, 0, -18), -15, 50, -45, (6.5, 17.0, 3.0), throw=(-10, -16, -12))),
        (16, key((8, -8, 0), (-20, 0, 4), -35, 20, 10, (4.0, 18.0, 4.5))),
        (ATTACK_TICKS, GUARD),
    ],
}


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


def sample_keys(keys, t):
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
    return regrip(out) if "_face" in out else out


# Each swing's impact tick: its attackVariants row's impactTicks (sheets), where the server lands the blow.
# Written into the clip as a "timeline" marker so the in-game check (GeoKnightChecks) can hold the two together.
IMPACT = {"thrust": 7, "overhead": 10, "low_cut": 8}


def attack_clips():
    n = ATTACK_TICKS * SUB
    out = {}
    for name, keys in ATTACKS.items():
        clip = bake([sample_keys(keys, i / SUB) for i in range(n + 1)], ATTACK_TICKS, loop=False)
        clip["timeline"] = {fmt_t(IMPACT[name] / TPS): "impact"}
        out["attack_" + name] = clip
    return out


# ------------------------------------------------------------------------------------------------
# Stances (unit animation rebuild, PLAN.md "Shield"): what the knight holds while it obeys an order.
# Played by GeoTroop's stance layer (StanceController), which crossfades to the order's pose within
# 2 ticks so the change reads at range. The upper poses go over any gait; the legs poses only while the
# knight stands (the layer scales them by how still it is), so a planted foot never slides.
#   ready      no order stance, but fighting: sword cocked, shield forward, left shoulder leading.
#   brace      Hold, out of formation: feet set wide, knees bent, the same guard over them.
#   wall       Hold, formed: a deep set, shoulder behind the shield, sword laid over its rim to stab.
#   *_up       the same with the shield raised (the server's raised shield: it blocks counters).
#   shield_up  shield raised while moving (shield_move, a raised shield on fall_back).
#   ward       fall_back with the shield down: backing off, point held at the enemy.
# Each loops 40 ticks with a slow breath so a held line is never a frozen statue.
# ------------------------------------------------------------------------------------------------

STANCE_TICKS = 40


def skey(torso, arm, fore, blade, blade_out, fist, hips=Z3, tilt=0.0, head=None):
    """A stance key: like key(), with the shield set upright over the stance's hips (STANCE_LEGS), tilted by tilt
    (+ top forward), and an optional head override."""
    k = key(torso, arm, fore, blade, blade_out, fist, upright(torso, hips, keep=0.85, turn=0.65, tilt=tilt))
    if head is not None:
        k["head"] = head
    return k


STANCE_UPPER = {
    "ready": skey((5, 14, 0), (-22, 0, 12), -62, -28, -4, FIST_GUARD),
    # brace: the sword rides low by the right hip, point forward and a little down, under the shield's edge.
    "brace": skey((8, 16, 0), (-10, 0, 14), -48, 18, -4, (2.5, 18.0, 5.5), hips=(0, 10, 0)),
    # *_up: the fist rises to the shoulder so the rim covers the eyes.
    "brace_up": skey((9, 18, 0), (-14, 0, 16), -52, 12, -6, (2.0, 22.5, 6.0), hips=(0, 10, 0)),
    # wall: torso pitched well forward over the deep set; the arm rises to keep the sword level over the rim.
    "wall": skey((22, 24, 0), (-72, -30, 6), -40, -6, -6, (2.5, 18.5, 6.0), hips=(4, 16, 0)),
    "wall_up": skey((24, 26, 0), (-88, -32, 4), -30, -2, -8, (2.0, 22.0, 6.0), hips=(4, 16, 0)),
    "shield_up": skey((6, 16, 0), (-30, 0, 16), -76, -40, -8, (2.0, 22.0, 6.0)),
    # ward: leaning back as it gives ground, the sword held across the belly, point angled down at the enemy.
    "ward": skey((-6, 8, 0), (-30, 0, -6), -62, 42, 50, (3.0, 17.5, 5.0)),
}

# Feet (ankle targets, px) and hips (rot, pos) for the two set stances; yaw is each foot's turn (right, left).
STANCE_LEGS = {
    "brace": {"hips": ((0.0, 10.0, 0.0), (0.0, -1.3, 0.2)), "right": (-3.0, 2.0, 2.6), "left": (2.8, 2.0, -2.6),
              "yaw": (-14.0, 8.0)},
    "wall": {"hips": ((4.0, 16.0, 0.0), (0.0, -3.6, 0.6)), "right": (-3.4, 2.0, 4.0), "left": (3.2, 2.0, -3.8),
             "yaw": (-20.0, 10.0)},
}


def stance_upper(pose):
    n = STANCE_TICKS * SUB
    out = []
    for i in range(n + 1):
        u = (i % n) / n
        br = math.sin(2 * math.pi * u)
        f = {b: (tuple(v), None) for b, v in pose.items()}
        tx, ty, tz = pose["torso"]
        f["torso"] = ((tx + 0.8 * br, ty, tz), None)
        hx, hy, hz = pose["head"]
        f["head"] = ((hx - 0.5 * br, hy, hz), None)
        ax, ay, az = pose["right_arm"]
        f["right_arm"] = ((ax - 0.6 * br, ay, az), None)
        sx, sy, sz = pose["left_arm"]
        f["left_arm"] = ((sx - 0.8 * br, sy, sz), None)
        fx_, fy_, fz_ = f["_face"][0]
        lag = math.sin(2 * math.pi * u - 0.6)              # the shield settles a beat after the breath
        f["_face"] = ((fx_ + 1.5 * lag, fy_ + 0.8 * lag, fz_ - 0.6 * lag), None)
        out.append(regrip(f))
    return bake(out, STANCE_TICKS)


def stance_legs(spec):
    n = STANCE_TICKS * SUB
    out, guesses = [], {}
    for i in range(n + 1):
        u = (i % n) / n
        br = math.sin(2 * math.pi * u)
        (rx, ry, rz), (px, py, pz) = spec["hips"]
        hips = ((rx, ry, rz), (px, py - 0.1 * br, pz))
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
    out = {"stance_%s_upper" % k: stance_upper(v) for k, v in STANCE_UPPER.items()}
    out.update({"stance_%s_legs" % k: stance_legs(v) for k, v in STANCE_LEGS.items()})
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
HIT_PEAK = {
    "front": shield_keep(off(torso=(-9, 0, 0), head=(-8, 0, 0), rarm=(-6, 0, 6), larm=(-4, 0, -4)), tilt=-4),
    "back": shield_keep(off(torso=(7, 0, 0), head=(3, 0, 0), rarm=(6, 0, 4), larm=(6, 0, -4)), tilt=3),
    "right": shield_keep(off(torso=(-3, 8, 5), head=(0, 4, 2), rarm=(5, 0, 4), larm=(-3, 0, -3))),
    "left": shield_keep(off(torso=(-3, -8, -5), head=(0, -4, -2), rarm=(-3, 0, 3), larm=(5, 0, -4))),
}
HIT_TICKS = {"light": 9, "medium": 16}
LIGHT = 0.5   # a light flinch reaches half the stagger's first key


def hit_keys(peak, weight):
    if weight == "light":    # a flinch: snap to the peak in 1.5 ticks, a small rebound, settled by t9
        p = scaled(peak, LIGHT)
        return [(0, ZERO), (1.5, p), (4, scaled(p, -0.15)), (6.5, scaled(p, 0.05)), (9, ZERO)]
    # a stagger: driven further and held a beat, then the body recovers past centre and settles
    return [(0, ZERO), (2, scaled(peak, 2.2)), (4.5, scaled(peak, 2.0)), (9, scaled(peak, -0.35)),
            (12.5, scaled(peak, 0.1)), (16, ZERO)]


# the shield arm is thrown up and forward into the blow, then driven back by it as the body gives
BLOCK_PEAK = shield_keep(off(torso=(-6, -10, 0), head=(-4, 6, 0), rarm=(-2, 0, 4), larm=(-14, -10, -8), lfore=26),
                         tilt=-8)
BLOCK_TICKS = 10


def hit_clips():
    out = {}
    for weight, ticks in HIT_TICKS.items():
        for d, peak in HIT_PEAK.items():
            keys = hit_keys(peak, weight)
            out["hit_%s_%s" % (weight, d)] = bake([sample_keys(keys, i / SUB) for i in range(ticks * SUB + 1)],
                                                  ticks, loop=False)
    keys = [(0, ZERO), (1.5, BLOCK_PEAK), (4, scaled(BLOCK_PEAK, 0.55)), (7, scaled(BLOCK_PEAK, -0.08)), (BLOCK_TICKS, ZERO)]
    out["hit_block"] = bake([sample_keys(keys, i / SUB) for i in range(BLOCK_TICKS * SUB + 1)], BLOCK_TICKS, loop=False)
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
        ch["rotation"] = {fmt_t(i / SUB / TPS): [round(cols[0][i], 3), round(cols[1][i], 3), round(cols[2][i], 3)]
                          for i in range(len(samples))}
        if any(s[b][1] is not None for s in samples):
            ch["position"] = {fmt_t(i / SUB / TPS): [round(v, 4) for v in (s[b][1] or (0, 0, 0))]
                              for i, s in enumerate(samples)}
        out[b] = ch
    return {"loop": loop, "animation_length": round(ticks / TPS, 6), "bones": out}


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
             "idle_legs": idle_legs, "idle_upper": idle_upper, **attack_clips(), **stance_clips(), **hit_clips()}
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1), encoding="utf-8")
    print("wrote", OUT, "clips:", ", ".join(anims))
    if a.check:
        for g in (WALK, RUN):
            print(g.name, "stride %.1f px, stance travel %.1f px" % (g.stride, g.travel), json.dumps(report(g)))


if __name__ == "__main__":
    main()
