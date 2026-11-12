"""Batch 1 stance clips for the finished biped troops: rally, hold, fall back, ranged rest and aim.

python art/anim/common/stances.py <troop|all> [--out DIR]

Reads the troop's exported clip file (assets/gravesandglory/animations/troop/<troop>.animation.json), builds the new stance clips from
the troop's own ready pose (and, for the aim, from the full-draw pose of its own attack clip, so the aim blends into the shot), and
merges ONLY the new clips back (after the last stance_ clip), leaving every other clip, deaths included, byte for byte as it was.
--out writes the merged file under DIR instead (scratch previews).

Clips (absolute poses, like the other stance_ clips; see docs/anim-rebuild/STYLE.md):
  stance_rally_upper      20 ticks, 5 up / 8 hold / 7 down, plays once (the client holds its last pose); every troop.
  stance_hold_upper/legs  48 ticks loop; troops that get the hold order (no shield, no spear).
  stance_fallback_upper   8 ticks loop, a guard bobbing with the step; stance_fallback_legs the backward walk (walk_legs reversed,
                          0.7 amplitude, same 8 tick cycle so the two stay in phase); every troop.
  stance_rest_upper       48 ticks loop; ranged troops.
  stance_aim_upper        40 ticks loop, held at full draw, pitch 0; stance_aim_up_upper (-45) and stance_aim_down_upper (+30) are
                          the same pose pitched about the chest's lateral axis; StanceController blends the three by aimPitch.
"""
import argparse
import json
import math
import re
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ASSETS = HERE.parents[2] / "src/main/resources/assets/gravesandglory/animations/troop"
TPS = 20
SUB = 4          # samples per tick
ROT_TOL, POS_TOL = 0.02, 0.002

GEAR = {}
for g, names in {
    "shield": "knight sentinel ic_hearthguard gl_marshal",
    "onehand": "revenant tc_raider gb_hollowking",
    "spear": "wm_tusk tc_tideguard tc_admiral",
    "axe": "ic_breaker",
    "bow": "archer longbow wm_hunter bonebow stalker",
    "xbow": "ic_arbalist",
    "thrower": "tc_harpooner",
    "staff": "mage",
    "focus": "hexer tc_mist wm_seer",
}.items():
    for n in names.split():
        GEAR[n] = g
HOLD = {"onehand", "axe", "bow", "xbow", "thrower", "staff", "focus"}   # gears that receive the "hold" order (BattleMath)
RANGED = {"bow", "xbow", "thrower", "staff", "focus"}
# the tick, in the troop's own attack clip, at which it stands at full aim (the aim pose is that frame)
AIM_AT = {"ic_arbalist": ("attack_loose", 0.2), "mage": ("attack_cast", 0.35), "hexer": ("attack_cast", 0.2),
          "wm_seer": ("attack_cast", 0.35), "tc_mist": ("attack_cast", 0.35), "tc_harpooner": ("attack_throw", 0.3)}
LEG_NAMES = re.compile(r"^(hips|.*_leg|.*_shin|.*_foot|.*_boot|skirt_.*)$")


# ---------------------------------------------------------------------------------------------- clip reading / writing
def _keys(m):
    ks = sorted(((float(t), v) for t, v in m.items()), key=lambda kv: kv[0])
    return [k for k, _ in ks], np.array([v for _, v in ks], float)


def sample(clip, bone, chan, t):
    """Linear sample of one channel of a baked clip at t seconds (None when the bone has no such channel)."""
    m = clip["bones"].get(bone, {}).get(chan)
    if not m:
        return None
    ts, vs = _keys(m)
    t = min(max(t, ts[0]), ts[-1])
    return np.array([np.interp(t, ts, vs[:, i]) for i in range(3)])


def pose_at(clip, t, upper_only=False, legs_only=False):
    """{bone: [rot, pos|None]} at t seconds, all bones of the clip (optionally only upper-body or leg bones)."""
    out = {}
    for b in clip["bones"]:
        leg = bool(LEG_NAMES.match(b))
        if (upper_only and leg) or (legs_only and not leg):
            continue
        r, p = sample(clip, b, "rotation", t), sample(clip, b, "position", t)
        out[b] = [r if r is not None else np.zeros(3), p]
    return out


# ---------------------------------------------------------------------------------------------- rotation maths
def _rx(a): c, s = math.cos(a), math.sin(a); return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
def _ry(a): c, s = math.cos(a), math.sin(a); return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
def _rz(a): c, s = math.cos(a), math.sin(a); return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def to_mat(r):
    """A bone's JSON Euler degrees (x, y, z; Blockbench signs) as the rotation matrix Rz Ry Rx of the geometry (x and y negated)."""
    x, y, z = (math.radians(v) for v in r)
    return _rz(z) @ _ry(-y) @ _rx(-x)


def from_mat(m):
    """The inverse of to_mat: the JSON Euler degrees of a rotation matrix (Rz Ry Rx order)."""
    b = -math.asin(max(-1.0, min(1.0, m[2, 0])))
    if abs(math.cos(b)) > 1e-6:
        a = math.atan2(m[2, 1], m[2, 2]); c = math.atan2(m[1, 0], m[0, 0])
    else:
        a = math.atan2(-m[1, 2], m[1, 1]); c = 0.0
    return np.array([-math.degrees(a), -math.degrees(b), math.degrees(c)])


def pitch(r, deg):
    """The bone rotation r turned by deg about the PARENT's lateral axis, + toward down (the arm's JSON x sign); stays correct
    when the bone is yawed (a plain +x would tip it sideways)."""
    return from_mat(_rx(-math.radians(deg)) @ to_mat(r))


def slerp(r0, r1, k):
    """The Euler rotation k of the way (geodesic, k may exceed 1 for overshoot) from the Euler rotation r0 to r1."""
    R0, R1 = to_mat(r0), to_mat(r1)
    d = R0.T @ R1
    ang = math.acos(max(-1.0, min(1.0, (np.trace(d) - 1) / 2)))
    if ang < 1e-6:
        return np.array(r0, float) + (np.array(r1, float) - np.array(r0, float)) * k
    if math.pi - ang < 1e-4:
        w, v = np.linalg.eigh((d + d.T) / 2)
        ax = v[:, int(np.argmax(w))]
    else:
        ax = np.array([d[2, 1] - d[1, 2], d[0, 2] - d[2, 0], d[1, 0] - d[0, 1]]) / (2 * math.sin(ang))
    a = ang * k
    K = np.array([[0, -ax[2], ax[1]], [ax[2], 0, -ax[0]], [-ax[1], ax[0], 0]])
    Rk = np.eye(3) + math.sin(a) * K + (1 - math.cos(a)) * (K @ K)
    return from_mat(R0 @ Rk)


def unwrap(vs):
    """Each row of vs (Euler degrees over time) moved onto the equivalent triple nearest the row before it, so a continuous motion never flips branch."""
    out = np.array(vs, float)
    for i in range(1, len(out)):
        prev, cur = out[i - 1], out[i]
        best = None
        for alt in (cur, np.array([cur[0] + 180, 180 - cur[1], cur[2] + 180])):
            c = alt + 360 * np.round((prev - alt) / 360)
            d = float(np.abs(c - prev).sum())
            if best is None or d < best[0]:
                best = (d, c)
        out[i] = best[1]
    return out


# ---------------------------------------------------------------------------------------------- envelopes
def smooth(x): x = min(1.0, max(0.0, x)); return x * x * (3 - 2 * x)
def ease_out(x): x = min(1.0, max(0.0, x)); return 1 - (1 - x) ** 3
def ease_in(x): x = min(1.0, max(0.0, x)); return x ** 3


def snap_up(x, over):
    """0 to 1 in one fast beat with an overshoot of `over` that settles back to 1 (x in 0..1)."""
    x = min(1.0, max(0.0, x))
    if x < 0.8:
        return (1 + over) * ease_out(x / 0.8)
    return (1 + over) - over * smooth((x - 0.8) / 0.2)


def rally_env(t, lead=0.0, rise=5.0):
    """5 ticks up with a little overshoot, 8 held, 7 back to ready. `lead` ticks early on the way up, late on the way down.
    `rise`: ticks up (RALLY_RISE), still held until tick 13."""
    if t < 13:
        return snap_up((t + lead) / rise, 0.07)
    return 1 - smooth((t - 13 - lead * 0.6) / 7.0)


def rally_env_late(t, d):
    """rally_env whose way down starts `d` ticks late but still ends at tick 20."""
    return snap_up(t / 5.0, 0.07) if t < 13 else 1 - smooth((t - 13 - d) / (7 - d))


# ---------------------------------------------------------------------------------------------- baking
def thin(ts, vs, tol):
    keep = [0]
    for i in range(1, len(ts) - 1):
        a = keep[-1]
        # drop i when the straight line a -> i+1 stays within tol at every skipped key
        ok = True
        for j in range(a + 1, i + 1):
            f = (ts[j] - ts[a]) / (ts[i + 1] - ts[a])
            if np.abs(vs[a] + (vs[i + 1] - vs[a]) * f - vs[j]).max() > tol:
                ok = False; break
        if not ok:
            keep.append(i)
    keep.append(len(ts) - 1)
    return keep


def bake(frames, ticks, loop):
    """frames: list (SUB samples per tick, inclusive of the end) of {bone: (rot, pos|None)}. Returns the clip dict."""
    n = len(frames)
    ts = np.array([i / (SUB * TPS) for i in range(n)])
    bones = {}
    for b in sorted({b for f in frames for b in f}):
        ent = {}
        for chan, idx, tol in (("rotation", 0, ROT_TOL), ("position", 1, POS_TOL)):
            if all(f.get(b, (None, None))[idx] is None for f in frames):
                continue
            vs = np.array([f[b][idx] if f.get(b, (None, None))[idx] is not None else (frames[0][b][idx] if frames[0][b][idx] is not None else np.zeros(3))
                           for f in frames], float)
            if idx == 0:
                vs = unwrap(vs)
            keep = thin(ts, vs, tol)
            ent[chan] = {(f"{ts[i]:.4f}".rstrip("0").rstrip(".") if ts[i] else "0.0"): [round(float(x), 3) for x in vs[i]] for i in keep}
        bones[b] = ent
    return {"loop": loop, "animation_length": round(ticks / TPS, 4), "bones": bones}


def frames_of(ticks, fn, loop=False):
    """fn(t_ticks) -> {bone: (rot, pos|None)}; sampled SUB per tick, the end inclusive (a loop closes on its first sample)."""
    n = ticks * SUB
    out = []
    for i in range(n + 1):
        t = i / SUB
        out.append(fn(0.0 if (loop and i == n) else t))
    return out


# ---------------------------------------------------------------------------------------------- the pose bank
class Troop:
    def __init__(self, tid, path=None):
        self.tid, self.gear = tid, GEAR[tid]
        self.path = Path(path) if path else ASSETS / f"{tid}.animation.json"
        self.doc = json.loads(self.path.read_text())
        self.anims = self.doc["animations"]
        self.ready = pose_at(self.anims["stance_ready_upper"], 0.0)
        self.ready_legs = pose_at(self.anims["stance_ready_legs"], 0.0)
        self.brace_legs = pose_at(self.anims["stance_brace_legs"], 0.0)
        self.arm = "left" if self.gear == "bow" else "right"     # the weapon arm (the bow is in the left fist)
        self.off = "right" if self.arm == "left" else "left"

    def rot(self, pose, bone):
        return np.array(pose[bone][0], float) if bone in pose else np.zeros(3)


def copy_pose(p):
    return {b: [np.array(r, float), None if q is None else np.array(q, float)] for b, (r, q) in p.items()}


def lift_arm(P, side, deg, keep=1.0, tilt=0.0, fore=None):
    """Raises the arm (+deg up, about the chest's lateral axis) and counter-pitches the hand by keep*deg so the weapon keeps its
    orientation; tilt tips the weapon further forward (-) or back (+)."""
    a, h = f"{side}_arm", f"{side}_hand"
    P[a][0] = pitch(P[a][0], -deg)
    if fore is not None:
        P[f"{side}_forearm"][0][0] = fore
    P[h][0] = pitch(P[h][0], keep * deg + tilt)


def chest(P, torso_x=0.0, torso_y=0.0, head_x=0.0, head_y=0.0, off=None, off_keep=1.0):
    """Moves the torso and head; the off arm is counter-pitched by the torso's pitch change so it stays in its pose."""
    P["torso"][0] = P["torso"][0] + np.array([torso_x, torso_y, 0.0])
    P["head"][0] = P["head"][0] + np.array([head_x, head_y, 0.0])
    if off and torso_x:
        P[f"{off}_arm"][0] = pitch(P[f"{off}_arm"][0], -torso_x * off_keep)


# ---------------------------------------------------------------------------------------------- rally (A10)
# per gear: the arm that thrusts up, by how many degrees, how much of the weapon's orientation is kept, a final tilt
RALLY = {
    "shield":  dict(arm="right", lift=105, keep=0.55, tilt=-0, fore=-22),
    "onehand": dict(arm="right", lift=105, keep=0.55, tilt=-0, fore=-22),
    "spear":   dict(arm="both", lift=48, keep=0.0, tilt=0, fore=None),   # was 62 (see RALLY_RISE)
    "axe":     dict(arm="both", lift=40, keep=0.0, tilt=-10, fore=None, out=-10),   # from the wide port guard a 48 deg heave put the haft into the breastplate: a lower heave, the head tipped on and the arms rolled off the chest
    "bow":     dict(arm="left", lift=84, keep=1.0, tilt=0, fore=-12, out=-11, chest=(-2, -1)),   # quiver on the back: no lean, the head stays out of the arrows
    "xbow":    dict(arm="right", lift=125, keep=0.45, tilt=0, fore=-10, out=-25),   # the crossbow overhead beside the head, the face clear
    "thrower": dict(arm="right", lift=14, keep=0.0, tilt=0, fore=-70),
    "staff":   dict(arm="right", lift=70, keep=1.0, tilt=0, fore=-35),
    "focus":   dict(arm="right", lift=80, keep=0.5, tilt=0, fore=-25),
}


# per troop: the full rally pose of the thrusting arm (Euler degrees, solved offline against the geometry so the weapon reads and
# nothing passes through the head or chest); the arm goes there from ready along a straight Euler line scaled by the envelope.
RALLY_POSE = {
    # harpoon thrust straight up over the head, tip leaning a hair forward
    "tc_harpooner": dict(right_arm=[-178.9, -24.5, -11.1], right_forearm=[-144.7, -1.6, -4.1], right_hand=[-111.2, 6.8, 5.5]),
    # lantern raised high beside the head
    "hexer": dict(right_arm=[-56.8, 53.7, 92.5], right_forearm=[-9.5, -8.9, 12.9], right_hand=[28.1, -18.7, -6.1]),
}


# per troop: the off hand's small glow pose (arm lift, forearm bend), scaled by the envelope
RALLY_OFF = {"hexer": dict(lift=30, fore=-40)}


RALLY_WAY = {
    # the way down, solved tick by tick so the spear sweeps overhead -> forward in the sagittal plane (no side swing, clear of the head); tick 20 is the ready pose
    "tc_harpooner": {
        13: dict(right_arm=[-178.9, -24.5, -11.1], right_forearm=[-144.7, -1.6, -4.1], right_hand=[-111.2, 6.8, 5.5]),
        14: dict(right_arm=[-177.5, -24.1, -10.9], right_forearm=[-143.3, -2.0, -3.6], right_hand=[-109.7, 6.7, 4.9]),
        15: dict(right_arm=[-173.9, -23.1, -10.4], right_forearm=[-140.1, -2.8, -2.4], right_hand=[-105.7, 6.3, 3.5]),
        16: dict(right_arm=[-169.2, -21.7, -9.5], right_forearm=[-135.3, -3.9, -0.8], right_hand=[-100.8, 6.1, 1.9]),
        17: dict(right_arm=[-164.1, -20.6, -8.8], right_forearm=[-130.0, -4.8, 0.7], right_hand=[-95.7, 5.8, 0.3]),
        18: dict(right_arm=[-159.5, -19.1, -7.6], right_forearm=[-124.9, -5.4, 2.3], right_hand=[-91.2, 5.8, -0.5]),
        19: dict(right_arm=[-156.6, -18.9, -6.9], right_forearm=[-121.5, -5.7, 3.1], right_hand=[-87.4, 5.8, -1.5]),
    },
}


# per troop, per bone: ticks that bone waits before coming down (it still arrives at ready by tick 20), so the weapon sweeps back clear of the head
RALLY_LAG = {}
# per troop, per bone: the way up eases in (envelope ** power) so the weapon rises clear of the head instead of cutting through it
RALLY_UP = {"tc_harpooner": dict(right_arm=2.0, right_forearm=1.5)}


def rally_pose(tr, e, c, t=None):
    """The ready pose with the rally at arm envelope e and chest envelope c (0..1.07)."""
    cfg = RALLY[tr.gear]
    P = copy_pose(tr.ready)
    if tr.tid in RALLY_POSE:
        off = "left" if cfg["arm"] == "right" else "right"
        chest(P, torso_x=-7 * c, head_x=-10 * c, off=off)
        way = RALLY_WAY.get(tr.tid)
        for b, tgt in RALLY_POSE[tr.tid].items():
            if way and t is not None and t >= 13:
                # on the solved way down: geodesic between the whole-tick waypoints, the last one being the ready pose
                i = min(int(t), 19); k = t - i
                a = np.array(way[i][b], float)
                z = np.array(way[i + 1][b], float) if i + 1 in way else np.array(tr.ready[b][0], float)
                P[b][0] = slerp(a, z, k)
                continue
            eb = e if t is None or b not in RALLY_LAG.get(tr.tid, {}) else rally_env_late(t, RALLY_LAG[tr.tid][b])
            if t is not None and t < 13 and b in RALLY_UP.get(tr.tid, {}):
                eb = snap_up(min(t / 5.0, 1.0) ** RALLY_UP[tr.tid][b], 0.07)
            P[b][0] = slerp(P[b][0], np.array(tgt, float), eb)     # geodesic, so the weapon sweeps a clean arc instead of an Euler tangle
        if tr.tid in RALLY_OFF:  # the free hand lifts a little, open, as if gathering the glow
            o = RALLY_OFF[tr.tid]
            P[f"{off}_arm"][0] = pitch(P[f"{off}_arm"][0], -o['lift'] * e)
            P[f"{off}_forearm"][0][0] += o['fore'] * e
        return P
    sides = ("left", "right") if cfg["arm"] == "both" else (cfg["arm"],)
    off = None if cfg["arm"] == "both" else ("left" if cfg["arm"] == "right" else "right")
    tx, hx = cfg.get("chest", (-7, -10))
    chest(P, torso_x=tx * c, head_x=hx * c, off=off)
    for s in sides:
        base_fore = P[f"{s}_forearm"][0][0]
        lift_arm(P, s, cfg["lift"] * e, cfg["keep"], cfg["tilt"] * e)
        P[f"{s}_arm"][0][2] += cfg.get("out", 0) * e
        if cfg["fore"] is not None:
            P[f"{s}_forearm"][0][0] = base_fore + (cfg["fore"] - base_fore) * min(e, 1.0)
    # small hold tremble on the thrust arm, windowed so it is zero at both ends
    return P


END_ON_HOLD = {"tc_harpooner", "archer", "ic_arbalist", "mage"}


# per gear: ticks the rally takes to rise (default 5). A two-handed shaft heaved up by both arms swung the whole weapon
# 62 deg in the first two ticks (scorecard ang_speed_weapon 60 deg/100ms, limit 43.5): spears and the greataxe heave it up
# 48 deg over 7 ticks (about 40 deg/100ms at the snap), still overshooting and held to tick 13.
RALLY_RISE = {"spear": 7.0, "axe": 7.0}


def build_rally(tr):
    rise = RALLY_RISE.get(tr.gear, 5.0)

    def fn(t):
        e = rally_env(t, rise=rise)
        c = rally_env(t, lead=1.2, rise=rise)
        P = rally_pose(tr, e, c, t)
        if tr.tid in END_ON_HOLD:         # the last 7 ticks also settle into the hold pose, so the clip ends exactly on it
            H = hold_pose(tr, 0.0); kh = smooth((t - 13) / 7.0)
            for b in P:
                if b in H and kh > 0:
                    P[b][0] = slerp(P[b][0], H[b][0], kh)
        w = smooth((t - 5) / 2.0) * (1 - smooth((t - 12) / 1.0))
        P["head"][0][1] += 0.8 * w * math.sin((t - 5) * math.pi * 0.5)
        P["torso"][0][0] += 0.5 * w * math.sin((t - 5) * math.pi * 0.5)
        return {b: (r, q) for b, (r, q) in P.items()}
    return bake(frames_of(20, fn), 20, False)


# ---------------------------------------------------------------------------------------------- hold (A1)
XH = [30, -15, 10]      # xbow hold: right arm lift, right arm tuck, left arm out (the crossbow diagonal across the chest)


# Per troop clearance fit of the hold: degrees added to an arm bone's JSON Euler. wm_hunter: the tucked string arm ran its forearm into
# the hide jerkin's flank (pen_limb 1.97 px, limit 1.79); the forearm turns 8 deg out from the ribs (1.65 px).
HOLD_FIT = {"wm_hunter": {"right_forearm": (0.0, -8.0, 0.0)}}


def hold_pose(tr, phase):
    """Weight low and centred, the weapon up and pointed at the threat; phase 0..1 round the 48 tick breath."""
    P = copy_pose(tr.ready)
    g = tr.gear
    br = math.sin(2 * math.pi * phase)
    if g in ("onehand",):
        chest(P, torso_x=2, head_x=-3, off="left")
        lift_arm(P, "right", 12, keep=0.55)
        P["right_forearm"][0][0] -= 8
    elif g == "axe":
        chest(P, torso_x=2, head_x=-3)
        for s in ("left", "right"):
            lift_arm(P, s, 14, keep=0.0)
    elif tr.tid == "archer":
        # the bow fist at the chest (a 38 lift put it at the mouth); the free arm hangs by the ribs with the fist at the belt
        # front (solved by IK so the elbow stays in at the side and the forearm clears the jerkin)
        chest(P, torso_x=1, head_x=-3)
        lift_arm(P, "left", 12, keep=1.0)
        P["right_arm"][0] = np.array([-41.7, -34.8, 12.9])
        P["right_forearm"][0] = np.array([-42.5, -2.3, -5.8])
    elif g == "bow":
        chest(P, torso_x=1, head_x=-3)
        lift_arm(P, "left", 38, keep=1.0)        # the bow comes up on the threat at chest height, the string hand to the chest
        P["right_arm"][0] = pitch(P["right_arm"][0], -6)
        P["right_arm"][0][2] -= 28               # the free arm tucked in, not flared
    elif g == "xbow":
        chest(P, torso_x=1, head_x=-3)
        lift_arm(P, "right", XH[0], keep=1.0)
        P["right_arm"][0][2] += XH[1]
        P["left_arm"][0][2] += XH[2]
    elif g == "thrower":
        chest(P, torso_x=1, head_x=-2)
        lift_arm(P, "left", 25, keep=0.0)        # the off hand points at the target over the cocked arm
    elif g == "staff":
        chest(P, torso_x=1, head_x=-3)
        lift_arm(P, "right", 16, keep=0.8)       # the staff upright and forward
        lift_arm(P, "left", 48, keep=0.0, fore=-38)    # the off hand held forward, palm out
    elif g == "focus":
        chest(P, torso_x=1, head_x=-3)
        lift_arm(P, "left", 52, keep=0.0)        # the off hand holds the focus forward
        lift_arm(P, "right", 10, keep=0.6)
    for b, d in HOLD_FIT.get(tr.tid, {}).items():
        P[b][0] = P[b][0] + np.array(d, float)
    # breath and a slow weight shift (period 48 ticks, so the loop closes)
    P["torso"][0][0] += 0.7 * br
    P["head"][0][0] += -0.4 * br
    P["torso"][0][1] += 1.0 * math.sin(2 * math.pi * phase + 0.8)
    for s in ("left", "right"):
        P[f"{s}_arm"][0][2] += (0.5 if s == "right" else -0.5) * br
    return P


def lerp_pose(a, b, k):
    out = {}
    for bone in a:
        if bone in b:
            r = a[bone][0] + (b[bone][0] - a[bone][0]) * k
            pa, pb = a[bone][1], b[bone][1]
            p = None if (pa is None and pb is None) else (pa if pb is None else pb if pa is None else pa + (pb - pa) * k)
            out[bone] = [r, p]
        else:
            out[bone] = [np.array(a[bone][0], float), a[bone][1]]
    return out


def hold_legs_pose(tr, phase):
    """Feet shoulder-width, one foot forward, knees bent: between the ready and the brace legs, a touch lower."""
    P = lerp_pose(tr.ready_legs, tr.brace_legs, 0.55)
    hp = P["hips"][1] if P["hips"][1] is not None else np.zeros(3)
    P["hips"][1] = hp + np.array([0.25 * math.sin(2 * math.pi * phase), -0.35 + 0.12 * math.sin(2 * math.pi * phase + 1.2), 0.0])
    return P


def build_hold(tr):
    N = 48
    up = bake(frames_of(N, lambda t: {b: tuple(v) for b, v in hold_pose(tr, t / N).items()}, loop=True), N, True)
    lg = bake(frames_of(N, lambda t: {b: tuple(v) for b, v in hold_legs_pose(tr, t / N).items()}, loop=True), N, True)
    return up, lg


# ---------------------------------------------------------------------------------------------- fall back (A5)
# Per troop clearance fit of the fall-back guard: degrees added to the JSON Euler (x, y, z) of an arm bone. The weapon hand is then
# re-solved so the weapon keeps its world orientation (the guard reads the same). Found by a small grid search on the scorecard's own
# entry depth. Bows: the string arm, pitched back 12 deg off the ready guard, ran its forearm (and the bonebow's and stalker's
# bracers) into the jerkin's flank (pen_limb 0.75-1.39 px, limit 0.6); the arm swings a few degrees out from the ribs instead.
BOW_FALLBACK_FIT = {"right_arm": (0.0, 0.0, 9.0), "right_forearm": (0.0, -12.0, 0.0)}
FALLBACK_FIT = {t: BOW_FALLBACK_FIT for t in ("archer", "longbow", "bonebow", "stalker", "wm_hunter")}


def fallback_fit(tr, base):
    fit = FALLBACK_FIT.get(tr.tid)
    if not fit:
        return
    arm, fore, hand = (f"{tr.arm}_{b}" for b in ("arm", "forearm", "hand"))
    world = to_mat(base[arm][0]) @ to_mat(base[fore][0]) @ to_mat(base[hand][0])
    for b, d in fit.items():
        base[b][0] = np.array(base[b][0], float) + np.array(d, float)
    base[hand][0] = from_mat((to_mat(base[arm][0]) @ to_mat(base[fore][0])).T @ world)


def build_fallback(tr):
    """Upper: a guard (shield up, a ward, the weapon low across the body for ranged, the focus raised for casters) bobbing with the
    8 tick step. Legs: walk_legs backwards at 0.7 of its amplitude, same cycle."""
    g = tr.gear
    walk = tr.anims["walk_legs"]
    L = walk["animation_length"]
    N = int(round(L * TPS))
    if g == "shield":
        base = pose_at(tr.anims["stance_shield_up_upper"], 0.0)
    elif g in ("spear", "onehand", "axe"):
        base = pose_at(tr.anims["stance_ward_upper"], 0.0)
    else:
        base = copy_pose(tr.ready)
        if g == "thrower":
            # one-handed low carry (solved in the render frame with the chest(-3, -1) below applied): the right fist at the right hip
            # front, the harpoon through it pointing forward-down (17 deg) toward the troop's left, across the body; the left fist a
            # guard at the belly. Minecraft arms are too short for a two-handed low grip. Static: no arm keys move, so no swing.
            base["right_arm"][0] = np.array([-39.65, 6.52, -13.67])
            base["right_forearm"][0] = np.array([-20.02, 2.34, 13.19])
            base["right_hand"][0] = np.array([-98.54, 21.65, 114.96])
            base["left_arm"][0] = np.array([-19.82, 2.98, -5.2])
            base["left_forearm"][0] = np.array([-45.72, -4.14, -9.8])
            base["left_hand"][0] = np.array([0.0, 0.0, 0.0])
        elif g == "bow":
            lift_arm(base, "left", -22, keep=1.0)             # the bow hand at the waist (forearm bent up to the belt line), the bow low
            base["left_forearm"][0][0] -= 45
            base["left_hand"][0][0] += 32
            base["right_arm"][0] = pitch(base["right_arm"][0], 12)
        elif g == "xbow":
            lift_arm(base, "right", -25, keep=1.0)
        elif g in ("staff", "focus"):
            lift_arm(base, "right", 42, keep=0.6)              # the focus raised as a ward
            if g == "staff":
                lift_arm(base, "left", 20, keep=0.0)
    base = {b: [np.array(r, float), None if q is None else np.array(q, float)] for b, (r, q) in base.items()}
    fallback_fit(tr, base)
    chest(base, torso_x=-3, head_x=-1)
    if g in ("shield", "spear", "onehand", "axe", "thrower") and "head" in base:
        pass

    def up(t):
        ph = 2 * math.pi * t / N
        P = copy_pose(base)
        P["torso"][0] += np.array([0.9 * math.cos(2 * ph), 1.6 * math.sin(ph), 0.0])
        P["head"][0] += np.array([-0.5 * math.cos(2 * ph), -1.2 * math.sin(ph), 0.0])
        return {b: (r, q) for b, (r, q) in P.items()}

    up_clip = bake(frames_of(N, up, loop=True), N, True)

    # legs: walk_legs reversed in time, amplitude 0.7 about its mean
    rots = {}
    for b in walk["bones"]:
        pr = {"rotation": [], "position": []}
        for chan in ("rotation", "position"):
            if chan not in walk["bones"][b]:
                continue
            ts, vs = _keys(walk["bones"][b][chan])
            mean = np.mean([np.interp(np.linspace(0, ts[-1], 64, endpoint=False), ts, vs[:, i]) for i in range(3)], axis=1)
            rots[(b, chan)] = (ts, vs, mean)

    def legs(t):
        out = {}
        for (b, chan), (ts, vs, mean) in rots.items():
            tt = (N - t) / TPS % L if t > 0 else 0.0
            v = np.array([np.interp(tt, ts, vs[:, i]) for i in range(3)])
            v = mean + 0.7 * (v - mean)
            r, q = out.get(b, (None, None))
            out[b] = (v, q) if chan == "rotation" else (r, v)
        for b, (r, q) in list(out.items()):
            if b == "hips":
                q = (q if q is not None else np.zeros(3)) + np.array([0, -0.5, 0.0])
                out[b] = (r if r is not None else np.zeros(3), q)
            if r is None:
                out[b] = (np.zeros(3), q)
        return out

    leg_clip = bake(frames_of(N, legs, loop=True), N, True)
    return up_clip, leg_clip


# ---------------------------------------------------------------------------------------------- ranged rest (A8) and aim (A9)
def build_rest(tr):
    g = tr.gear
    N = 48

    def fn(t):
        ph = t / N
        P = copy_pose(tr.ready)
        chest(P, torso_x=-3, torso_y=-6, head_x=1, head_y=8)
        if g == "bow":
            lift_arm(P, "left", -26, keep=1.0)
            P["right_arm"][0] = pitch(P["right_arm"][0], 22)
            P["right_forearm"][0] = np.array([-8, 0, 0.0])
        elif g == "xbow":
            lift_arm(P, "right", -28, keep=1.0)
            P["left_arm"][0] = pitch(P["left_arm"][0], 14)
        elif g == "thrower":
            lift_arm(P, "right", -70, keep=0.0)      # the harpoon points down and across the body, not out like a bar
            P["left_arm"][0] = pitch(P["left_arm"][0], 28)
        elif g == "staff":
            lift_arm(P, "right", -6, keep=1.0)        # gripped 40% up the shaft: a deeper drop plants the butt in the floor
            P["left_arm"][0] = pitch(P["left_arm"][0], 14)
        else:
            lift_arm(P, "right", -24, keep=0.8)
            P["left_arm"][0] = pitch(P["left_arm"][0], 12)
        br = math.sin(2 * math.pi * ph)
        P["torso"][0] += np.array([0.9 * br, 1.5 * math.sin(2 * math.pi * ph + 0.9), 0.0])
        P["head"][0] += np.array([-0.6 * br, 5.0 * math.sin(2 * math.pi * ph), 0.0])   # a slow look round
        return {b: (r, q) for b, (r, q) in P.items()}

    return bake(frames_of(N, fn, loop=True), N, True)


def aim_base(tr):
    """The full-aim pose of the troop's own attack clip (bows: the full draw of attack_direct)."""
    if tr.gear == "bow":
        c = tr.anims["attack_direct"]
        dr = c["bones"]["draw"]["position"]
        t = float(max(dr, key=lambda k: dr[k][0]))
    else:
        name, t = AIM_AT[tr.tid]
        c = tr.anims[name]
    P = pose_at(c, t, upper_only=True)
    base = copy_pose(tr.ready)
    for b, (r, q) in P.items():
        base[b] = [np.array(r, float), None if q is None else np.array(q, float)]
    return base


# per troop: how much of the aim pitch the chest, the arms and the head take (the head looks along it)
# stalker: aiming up, the upper bow limb ran into the fur cowl (pen_weapon 2.35 px, limit 1.49); its chest takes half the pitch and
# the head looks along the aim, which carries the cowl back off the limb (1.12 px).
AIM_GAIN = {"tc_harpooner": (0.35, 0.9, -0.2), "ic_arbalist": (0.35, 0.8, -0.2), "stalker": (0.5, 0.5, 0.2)}
AIM_ARM_Z = {"mage": {"right_arm": (32.0, 0.0)}}
# per troop and aim pitch: rows set after the pitch gains. Hexer up (-45): the gains lifted the lantern (held across the chest) into
# the face line; the upper arm swings out to the troop's right instead (IK, forearm and wrist kept), so the lantern rides out and
# up beside the shoulder and the face stays clear at 000 and 045 through the whole level-to-up blend.
AIM_ROWS = {"hexer": {-45.0: {"right_arm": [9.51, -20.14, 96.02]}}}
AIM_TWIST = {"tc_harpooner": -16}     # per troop: extra chest yaw (degrees) so the aim reads side-on
AIM_WORLD = {"tc_harpooner"}
# per troop and aim pitch (0 level, -45 up, 30 down): whole-pose presets solved by IK against the in-game head look (the game adds
# its clamped head pitch to the head's JSON x). Archer: chest side-on, the nock at the right jaw, the arrow on the aim line, the bow
# arm straight, the draw elbow behind the arrow, the face clear at 000 and 045; the quiver tipped so its arrows clear the hood.
# Refit (scorecard): at 000 the bow arm lifts 2.5 deg so the bow's lower limb clears the front shin (pen_weapon 1.72 -> 0.92 px); at
# +30 the draw arm is re-solved with the fist held on the string (elbow no longer reads hyperextended: -27 -> +3 deg) and the bow
# hand turns 2-3 deg so the lower limb clears the front thigh (1.59 -> 0.91 px). Nock moves under 0.35 px, the arrow line under 0.2 px.
AIM_POSE = {"archer": {
    0.0: {"head": [-143.45, -86.64, 143.5], "left_arm": [-51.62, -1.06, 9.45], "left_forearm": [-47.96, -13.66, -30.12], "left_hand": [1.97, 18.95, -59.54], "right_arm": [-72.26, -10.6, -4.34], "right_forearm": [-28.82, -9.73, -36.65], "torso": [-178.0, 87.3, 180.0]},
    -45.0: {"head": [-131.0, -78.23, 170.35], "left_arm": [-59.48, 1.53, 38.82], "left_forearm": [-76.33, -23.75, -29.95], "left_hand": [-45.44, 54.97, -73.17], "right_arm": [-84.15, -21.12, 29.73], "right_forearm": [-22.61, -2.65, -13.19], "torso": [-106.78, 57.48, 248.04]},
    30.0: {"head": [-150.31, -87.03, 137.75], "left_arm": [-51.18, 20.15, -3.58], "left_forearm": [-0.52, -0.41, -14.51], "left_hand": [33.1, -25.17, -94.87], "right_arm": [-91.15, 11.59, -8.12], "right_forearm": [-1.52, 8.03, -59.65], "torso": [-258.01, 77.51, 100.22]},
}, "tc_harpooner": {
    # javelin cock: the throwing arm raised out and back so the fist sits above and behind the shoulder, the spear lying in the fist
    # (wrist under 20 degrees) on the aim line raised by a cock (spear 69 / 45 / 26 degrees up at -45 / level / +30) so the shaft
    # passes above the face from the throwing side; the off arm straight at the target; scored for a clear face at 000, 045 and 090
    # and no spear contact with the head, body or arms at every pitch between
    # (these presets replace AIM_TWIST / AIM_GAIN / AIM_WORLD for the harpooner)
    0.0: {"head": [6.7, -11.56, -1.35], "left_arm": [-60.76, -17.89, 4.06], "left_forearm": [-26.63, -0.97, -4.09], "left_hand": [0.0, 0.0, 0.0], "right_arm": [-25.67, 34.45, 60.48], "right_forearm": [-14.28, 9.9, 69.31], "right_hand": [12.56, -12.99, -1.44], "torso": [-6.56, 11.64, 0.0]},
    -45.0: {"head": [6.51, -5.0, 0.02], "left_arm": [-98.24, -42.21, -22.78], "left_forearm": [-27.64, -1.83, -7.45], "left_hand": [0.0, 0.0, 0.0], "right_arm": [-41.34, 37.93, 49.53], "right_forearm": [-8.95, 6.2, 69.33], "right_hand": [16.03, -7.47, -1.05], "torso": [-13.31, 4.86, -1.17]},
    30.0: {"head": [-10.5, -5.05, 0.53], "left_arm": [-66.37, -30.93, 21.59], "left_forearm": [-25.46, -0.33, -1.47], "left_hand": [0.0, 0.0, 0.0], "right_arm": [-8.39, 31.58, 66.58], "right_forearm": [-2.55, 2.21, 81.93], "right_hand": [15.07, -6.57, -0.87], "torso": [15.01, 5.02, 0.8]},
}}
AIM_POSE_COMMON = {"archer": {"right_hand": ([0, 0, 0], None), "draw": ([0, 0, 0], [2, 0, 0]), "quiver": ([0, 0, 15], [0, -2, 1]),
                              "hood_cape": ([-0.87, 0, 0], None), "tippet": ([-0.58, 0, 0], None)}}


def aim_pitched(tr, base, deg, phase):
    """The aim pose pitched `deg` (+ down): the chest takes 0.35 of it, the arms the rest, the head looks along it."""
    P = copy_pose(base)
    if tr.tid in AIM_POSE:
        rows = dict(AIM_POSE_COMMON.get(tr.tid, {}))
        rows.update({b: (r, None) for b, r in AIM_POSE[tr.tid][deg].items()})
        for b, (r, q) in rows.items():
            P[b] = [np.array(r, float), None if q is None else np.array(q, float)]
    elif tr.gear == "bow":
        # side-on views hid the bow behind the chest: open the chest toward the camera side and push the bow arm out
        P["torso"][0][1] -= 12
        P["left_arm"][0][2] -= 21        # the bow arm 15 degrees further off the face line
        P["head"][0][1] += 8
        if "quiver" in P:   # the arrows stab the head and shoulder when the chest turns and leans: drop the quiver back and down
            P["quiver"] = [np.array([0.0, 0.0, -16.0]), np.array([0.0, -5.0, 2.0])]
    for b, dz in AIM_ARM_Z.get(tr.tid, {}).items():    # per troop arm roll offsets (e.g. the mage's staff arm tucked in)
        P[b][0][2] += dz[0] * max(0.0, deg / 30.0) + dz[1] * max(0.0, -deg / 45.0)     # (down, up) offsets, in degrees
    if tr.tid not in AIM_POSE:     # presets are already pitched
        P["torso"][0][1] += AIM_TWIST.get(tr.tid, 0.0)
        gt, ga, gh = AIM_GAIN.get(tr.tid, (0.35, 0.65, -0.2))
        P["torso"][0] = pitch(P["torso"][0], gt * deg)
        for s in ("left", "right"):
            if tr.tid in AIM_WORLD:    # the chest is turned side-on, so pitch the arms about the world's lateral axis (through the shoulder), not the chest's
                Rt = to_mat(P["torso"][0])
                P[f"{s}_arm"][0] = from_mat(Rt.T @ _rx(-math.radians(ga * deg)) @ Rt @ to_mat(P[f"{s}_arm"][0]))
            else:
                P[f"{s}_arm"][0] = pitch(P[f"{s}_arm"][0], ga * deg)
        P["head"][0] = pitch(P["head"][0], gh * deg)
    for b, r in AIM_ROWS.get(tr.tid, {}).get(deg, {}).items():
        P[b][0] = np.array(r, float)
    # the held draw trembles a little
    tr_ = math.sin(2 * math.pi * phase * 3)
    P["torso"][0][0] += 0.25 * tr_
    for s in ("left", "right"):
        P[f"{s}_arm"][0][0] += 0.3 * tr_ * (1 if s == tr.arm else -1)
    return P


def build_aim(tr):
    base = aim_base(tr)
    N = 40
    out = {}
    for name, deg in (("stance_aim_upper", 0.0), ("stance_aim_up_upper", -45.0), ("stance_aim_down_upper", 30.0)):
        out[name] = bake(frames_of(N, lambda t, d=deg: {b: tuple(v) for b, v in aim_pitched(tr, base, d, t / N).items()}, loop=True), N, True)
    return out


# ---------------------------------------------------------------------------------------------- merge
# Per troop: the clips its own builder authors, which this pass leaves as the build wrote them. The bow family's four (Kort's
# rule 6, 2026-11-10: each troop's own bowman version, the bow across the forearm): art/anim/archer/build_bow_anims.py owns
# rally, hold (upper and legs), fall back (upper; the walk-backwards legs stay here), rest and the aims. Stalker is not listed.
_BOW_OWNS = {"stance_rally_upper", "stance_hold_upper", "stance_hold_legs", "stance_fallback_upper", "stance_rest_upper",
             "stance_aim_upper", "stance_aim_up_upper", "stance_aim_down_upper"}
BUILDER_OWNS = {t: _BOW_OWNS for t in ("archer", "longbow", "bonebow", "wm_hunter")}


def build_all(tr):
    clips = _build_all(tr)
    own = BUILDER_OWNS.get(tr.tid, set())
    return {k: v for k, v in clips.items() if k not in own}


def _build_all(tr):
    clips = {"stance_rally_upper": build_rally(tr)}
    if tr.gear in HOLD:
        clips["stance_hold_upper"], clips["stance_hold_legs"] = build_hold(tr)
    clips["stance_fallback_upper"], clips["stance_fallback_legs"] = build_fallback(tr)
    if tr.gear in RANGED:
        clips["stance_rest_upper"] = build_rest(tr)
        clips.update(build_aim(tr))
    return clips


def merge(tr, clips, out_path):
    old = tr.anims
    new = {}
    for k, v in old.items():
        if k in clips:
            continue                       # replaced below, in place of the old copy (re-runs are idempotent)
        new[k] = v
    # insert after the last stance_ clip, before the hit_ clips
    ordered, placed = {}, False
    last_stance = max((i for i, k in enumerate(new) if k.startswith("stance_")), default=-1)
    for i, (k, v) in enumerate(new.items()):
        ordered[k] = v
        if i == last_stance:
            ordered.update(clips); placed = True
    if not placed:
        ordered.update(clips)
    doc = dict(tr.doc); doc["animations"] = ordered
    import hang_swing   # a troop with a hanging part (troop_rig.SWING: the hexer's lantern): its swing re-keyed over every clip
    hang_swing.key_doc(tr.tid, doc, log=False)
    import limb_clear   # a troop whose free forearm's gear sinks into its body (limb_clear.CLEAR): those clips cleared again
    limb_clear.key_doc(tr.tid, doc, ASSETS.parents[1] / "geo/troop" / f"{tr.tid}.geo.json", ASSETS.parents[1] / "troop_rig" / f"{tr.tid}.json",
                       log=lambda *_: None)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", newline="\n") as fh:
        fh.write(json.dumps(doc, indent=1))


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
