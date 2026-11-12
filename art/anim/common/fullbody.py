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
import zlib
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
ROLL_RATE = 14.0     # degrees per sample (a quarter tick): the fastest the hand turns about the blade inside the strike
ROLL_CALM = 2.5      # degrees per sample outside the strike: 10 a tick, under focus_check's grip_roll limit
EDGE_BEFORE = 3.0    # ticks before the impact from which the edge may turn onto the cut's line. Never in the wind-up:
#                      Kort 2026-11-11, tick 5 of the raise: "weapons spins around in hand"
EDGE_AFTER = 2.0     # ticks after the impact the fast roll rate still applies

# Secondary motion (Kort 2026-11-11: "i want the whole unit to be animated including pieces of its armor that way it
# feels alive"; he named the cape and the helmet's red crest as stale, then took the crest back after seeing it rock:
# "remove the wobble of the red strip on his head", so a crest rides its helm and gets no keys). Each soft piece is a
# weight on a spring that trails its parent bone. soft_parts_for() finds the pieces of any rig by name (the same name tokens focus_check's
# secondary_life check judges) and measures each one from the geo; SOFT_TUNED overrides a piece per troop.
#   pivot/tip  where it hangs from and its free end, in the geo file's pixels
#   period     ticks of one swing; zeta = damping; gain scales the angle; max caps it (degrees)
#   plumb      how much it hangs straight down rather than with its parent (0 to 1)
#   inward     degrees it may swing toward the body it hangs on (cloth does not pass through the body)
#   lim        optional per-axis (lo, hi) degrees;  clip "legs" puts the keys in the <name>_legs clip
#   sway       degrees of slow swing of its own, so a piece is not a statue while the body holds a pose (reviews of
#              2026-11-11: quiver, skirts and trophies "pinned" through a full draw and a held lunge). Each piece gets
#              its own phase and a period of its own (_name_frac), so two pieces on one body never move as a pair.
SOFT_KINDS = {
    "cloth": dict(zeta=0.30, gain=1.2, max=30.0, plumb=0.40, inward=3.0, sway=1.2),
    "plume": dict(zeta=0.22, gain=2.4, max=16.0, plumb=0.0, inward=6.0, sway=0.6),
    "gear": dict(zeta=0.40, gain=0.8, max=8.0, plumb=0.5, inward=2.0, sway=1.6),
}
SAME_HINGE = 1.5     # px between two sibling bones' hanging points for them to count as one piece
SWAY_PERIOD = 1.5    # the slow swing's period, in the piece's own spring periods
SOFT_TUNED = {
    "knight": {
        "cape": dict(pivot=(0, 24, 3.6), tip=(0, 7, 4.1), period=7.0, zeta=0.28, gain=1.25, max=34.0, plumb=0.45,
                     lim=((0.0, 34.0), (-3.0, 3.0), (-14.0, 14.0))),
        "right_skirt": dict(lim=((-11.0, 11.0), (0.0, 0.0), (-6.0, 6.0)), max=11.0, gain=0.9, plumb=0.25, period=4.0),
        "left_skirt": dict(lim=((-11.0, 11.0), (0.0, 0.0), (-6.0, 6.0)), max=11.0, gain=0.9, plumb=0.25, period=4.0),
    },
}
# A shoulder plate is strapped to its upper arm. It trails the arm's swing by PLATE_FOLLOW of the turn, but never by
# more than PLATE_LAG degrees: in a small motion it visibly moves against the arm, in a big swing it stays ON the arm.
# Kort, knight overhead round 2, tick 10 (the plate then stayed 70% behind a raised arm): "why is wrist/arm above
# shoulder?", "sholder kind of twists out of place". Armour never leaves its limb.
PLATE_FOLLOW = 0.7
PLATE_LAG = 10.0
# ... and it gives way to the helm: when the head turns or tips onto a shoulder, the plate slides down its own arm
# (along the arm only, so it stays wrapped round it) rather than sinking into the helm (Kort marked the pauldron in the helm, tick 14 of the first overhead cut;
# the reviewer of round 2 still saw it "on the helm's lower edge"). Measured against the stance's own fit.
PLATE_YIELD = 0.25   # px a plate may sink deeper into the helm than the first sample has it
PLATE_SLIDE = 2.5    # px it slides at most
_LEG_BONES = ("hips", "right_leg", "left_leg", "right_shin", "left_shin", "right_foot", "left_foot")


_FULL = {}


def _name_frac(name, salt=""):
    """A fixed number in [0, 1) for a piece's name: its own phase and period, the same on every build."""
    return (zlib.crc32((salt + name).encode("utf-8")) & 0xFFFFFFFF) / 4294967296.0


def _full_model(troop):
    """The exported rig with every tier's cubes (B.MODEL is the builder's own lighter model)."""
    if troop not in _FULL:
        path = ROOT / "src/main/resources/assets/gravesandglory/geo/troop" / (troop + ".geo.json")
        _FULL[troop] = rg.load_geo(str(path)) if path.is_file() else B.MODEL
    return _FULL[troop]


def soft_parts_for(model=None, troop=None, hidden=()):
    """The trailing pieces of a rig, found by name and measured from its geo: {name: settings} for soft_motion.
    A piece is a bone whose name has a token of focus_check.ALIVE_KINDS; siblings that share a pivot and parent and hang
    from the same place (a skirt, its plate and its gilt, or the same cloth at three armour tiers) swing as one. Two
    things that only share a default bone pivot (the tusk's hip skull and tooth necklace) are two pieces."""
    import focus_check as fc
    troop = troop or B.TROOP
    model = model or _full_model(troop)
    tuned = SOFT_TUNED.get(troop, {})
    out, by_key = {}, {}
    for n, (kind, att, tip, length) in fc.alive_pieces(model).items():
        bone = model.bones[n]
        if kind == "plate" or n in hidden:
            continue
        key = (bone.parent, tuple(np.round(bone.pivot, 2)), kind)
        twin = next((m for m, a0 in by_key.get(key, ()) if np.linalg.norm(np.array(a0) - np.array(att)) <= SAME_HINGE), None)
        if twin is not None:                   # a sibling on the same hinge: one piece
            out[twin]["bones"] += (n,)
            continue
        par = bone.parent
        pbox = fc.rest_box(model, par)
        axis = (pbox.min(axis=0) + pbox.max(axis=0)) / 2.0 if len(pbox) else np.array(model.bones[par].pivot, dtype=float)
        chain, q = [], par
        while q is not None:
            chain.append(q)
            q = model.bones[q].parent
        legs = next((c in _LEG_BONES for c in chain if c in _LEG_BONES or c == "torso"), False)
        c = dict(SOFT_KINDS[kind])
        c.update(bones=(n,), parent=par, pivot=(-att[0], att[1], att[2]), tip=(-tip[0], tip[1], tip[2]),
                 axis=(-axis[0], axis[1], axis[2]), kind=kind,
                 period=(3.0 + 0.25 * length) * (0.88 + 0.24 * _name_frac(n, "period")))
        if length < 8.0:
            c["max"] = min(c["max"], 12.0)
        if legs:
            c["clip"] = "legs"
        out[n] = c
        by_key.setdefault(key, []).append((n, att))
    for name, over in tuned.items():
        if name in out:
            out[name].update(over)
    return out


def plates_for(model=None):
    model = model or _full_model(B.TROOP)
    return {n: (model.bones[n].parent, PLATE_FOLLOW) for n in model.order
            if "pauldron" in n.split("_") and str(model.bones[n].parent).endswith("_arm")}


def _rest_local(bone):
    pv = bone.pivot / 16.0
    return rg._T(*pv) @ rg._Rz(bone.rot[2]) @ rg._Ry(bone.rot[1]) @ rg._Rx(bone.rot[0]) @ rg._T(*(-pv))


def _bone_world(pose, name, model):
    """World matrix of any bone of the exported rig: the builder's bones posed, a tier's own pieces rigid on them."""
    extra, n = np.eye(4), name
    while n is not None and n not in B.MODEL.bones:
        extra = _rest_local(model.bones[n]) @ extra
        n = model.bones[n].parent
    return (B.world(pose, n) if n is not None else np.eye(4)) @ extra


def _cube_world(M, cube):
    cp = cube.pivot / 16.0
    Mc = M @ rg._T(*cp) @ rg._Rz(cube.rot[2]) @ rg._Ry(cube.rot[1]) @ rg._Rx(cube.rot[0]) @ rg._T(*(-cp))
    return cube.corners @ Mc[:3, :3].T + Mc[:3, 3]


_LATTICE = np.array([[u, v, w] for u in np.linspace(0, 1, 5) for v in np.linspace(0, 1, 5) for w in np.linspace(0, 1, 5)])


def _sink(boxes_a, boxes_b):
    """px the deepest point of either set of posed cubes (8 x 3 corners, blocks) lies inside a cube of the other."""
    best = 0.0
    for A, Bx in ((boxes_a, boxes_b), (boxes_b, boxes_a)):
        for a in A:
            pts = sc.cube_points(a, _LATTICE)
            for b in Bx:
                c = b.mean(axis=0)
                E = np.stack([b[6] - b[0], b[2] - b[0], b[1] - b[0]])
                half = np.linalg.norm(E, axis=1) / 2.0
                if half.min() < 1e-6:
                    continue
                q = (pts - c) @ (E / (2.0 * half[:, None])).T
                best = max(best, float((half - np.abs(q)).min(axis=1).max()))
    return best * 16.0


def _under(model, root):
    out = []
    for n in model.order:
        q = n
        while q is not None and q != root:
            q = model.bones[q].parent
        if q == root:
            out.append(n)
    return out


def _plates_yield(upper, legs, plates, fade, report=False):
    """Slides each shoulder plate away from the helm wherever the pose would sink it deeper than the stance has it."""
    import focus_check as fc
    model = _full_model(B.TROOP)
    if "head" not in B.MODEL.bones:
        return
    soft = set(fc.alive_pieces(model))

    def cubes(names):
        return [(model.cube_bone[ci], cube) for ci, cube in enumerate(model.cube_list) if model.cube_bone[ci] in names]

    helm = cubes([n for n in _under(model, "head") if n not in soft])
    n = len(upper)
    poses = []
    for u, l in zip(upper, legs):
        pose = {}
        for src in (l, u):
            for b, (rot, pos) in src.items():
                if b in B.MODEL.bones:
                    pose[b] = (rot, pos if pos is not None else (0.0, 0.0, 0.0))
        poses.append(pose)
    for plate, (arm, _) in plates.items():
        mine = cubes(_under(model, plate))
        if plate not in B.MODEL.bones or plate not in upper[0] or not mine or not helm:
            continue
        H = [[_cube_world(_bone_world(p, bn, model), c) for bn, c in helm] for p in poses]
        Pl = [[_cube_world(_bone_world(p, bn, model), c) for bn, c in mine] for p in poses]
        d0 = _sink(Pl[0], H[0])
        W = np.zeros(n)                      # px each sample slides the plate down its arm
        for i in range(n):
            if _sink(Pl[i], H[i]) - d0 <= PLATE_YIELD:
                continue
            away = _unit(np.concatenate(Pl[i]).mean(axis=0) - np.concatenate(H[i]).mean(axis=0))
            down = _unit(B.world(poses[i], arm)[:3, :3] @ np.array([0.0, -1.0, 0.0]))   # shoulder to elbow
            if float(away @ down) <= 0.1:
                continue                     # the helm is not above the plate along the arm: sliding would not help
            lo, hi = 0.0, PLATE_SLIDE
            for _ in range(7):
                mid = (lo + hi) / 2.0
                if _sink([b + down * mid / 16.0 for b in Pl[i]], H[i]) - d0 > PLATE_YIELD:
                    lo = mid
                else:
                    hi = mid
            W[i] = hi
        if not W.any():
            continue
        wide = np.array([W[max(0, i - SUB):i + SUB + 1].max() for i in range(n)])
        h = max(1, SUB // 2)
        soft_w = np.array([wide[max(0, i - h):i + h + 1].mean() for i in range(n)]) * np.array(fade)
        for i in range(n):
            if soft_w[i] < 1e-3:
                continue
            upper[i][plate] = (upper[i][plate][0], (0.0, -float(soft_w[i]), 0.0))     # arm-local: down the arm only
        if report and soft_w.max() > 0.05:
            k = int(np.argmax(soft_w))
            print("  plate %-21s slides %.1f px down its arm, off the helm (most at tick %.1f)"
                  % (plate, float(soft_w[k]), k / float(SUB)))


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


def _rot_of(rot, rest=(0.0, 0.0, 0.0)):
    r = np.asarray(rest, dtype=float) + np.radians([-rot[0], -rot[1], rot[2]])
    return (rg._Rz(r[2]) @ rg._Ry(r[1]) @ rg._Rx(r[0]))[:3, :3]


def _rot_pow(R, f):
    """R turned by f of its own angle about its own axis (f may be negative)."""
    ang = math.acos(max(-1.0, min(1.0, (np.trace(R) - 1.0) / 2.0)))
    if ang < 1e-6:
        return np.eye(3)
    ax = _unit(np.array([R[2, 1] - R[1, 2], R[0, 2] - R[2, 0], R[1, 0] - R[0, 1]]))
    K = np.array([[0, -ax[2], ax[1]], [ax[2], 0, -ax[0]], [-ax[1], ax[0], 0]])
    a = ang * f
    return np.eye(3) + math.sin(a) * K + (1.0 - math.cos(a)) * K @ K


def soft_motion(upper, legs, parts=None, plates=None, report=False):
    """Adds the trailing pieces to the samples: every piece of soft_parts_for() (or `parts`) swings on a damped spring
    behind its parent bone, and the shoulder plates follow only part of the arm. All of it is measured from the first
    sample and faded in and out, so the clip starts and ends with every piece where the stance has it.
    Returns (upper, legs). A piece that the clip already keys keeps its first sample's pose as the rest it swings from."""
    parts = soft_parts_for() if parts is None else parts
    plates = plates_for() if plates is None else plates
    n = len(upper) - 1
    poses = []
    for u, l in zip(upper, legs):
        pose = {}
        for src in (l, u):
            for b, (rot, pos) in src.items():
                if b in B.MODEL.bones:
                    pose[b] = (rot, pos if pos is not None else (0.0, 0.0, 0.0))
        poses.append(pose)
    down = np.array([0.0, -1.0, 0.0])
    fade = [B.smooth(max(0.0, min(1.0, i / float(SUB), (n - i) / (2.0 * SUB)))) for i in range(n + 1)]
    for name, c in parts.items():
        bones = [b for b in c["bones"] if b in B.MODEL.bones]
        if not bones or c["parent"] not in B.MODEL.bones:
            continue
        w = 2.0 * math.pi / c["period"]
        k, damp = w * w, 2.0 * c["zeta"] * w
        rest0 = {b: (upper[0].get(b) or legs[0].get(b) or ((0.0, 0.0, 0.0), None))[0] for b in bones}
        for p in poses:                       # the spring sees the parent only: the piece's own keys are its rest pose
            for b in bones:
                p.pop(b, None)
        Ms = [B.world(p, c["parent"]) for p in poses]
        P = [B.point(M, c["pivot"]) for M in Ms]
        length = float(np.linalg.norm(np.array(c["tip"], dtype=float) - np.array(c["pivot"], dtype=float)))
        out_l = None                           # the way out from the body it hangs on, in the parent's frame
        if c.get("axis") is not None and c.get("inward") is not None:
            o = np.array(c["tip"], dtype=float) - np.array(c["axis"], dtype=float)
            o = np.array([-o[0], 0.0, o[2]])
            if np.linalg.norm(o) > 1.0:
                out_l = _unit(o)
        tip_l = _unit(np.array([-(c["tip"][0] - c["pivot"][0]), c["tip"][1] - c["pivot"][1], c["tip"][2] - c["pivot"][2]]))

        # the slow swing of its own: a small sideways push on the spring's goal, square to the way the piece hangs
        e1 = _unit(np.cross(tip_l, [0.0, 0.0, 1.0]) if abs(tip_l[2]) < 0.9 else np.cross(tip_l, [1.0, 0.0, 0.0]))
        e2 = np.cross(tip_l, e1)
        turn = 2.0 * math.pi * _name_frac(name, "turn")
        side_l = math.cos(turn) * e1 + math.sin(turn) * e2
        sway = math.sin(math.radians(c.get("sway", 0.0)))
        sway_w = 2.0 * math.pi / (c["period"] * SWAY_PERIOD * SUB)
        sway_0 = 2.0 * math.pi * _name_frac(name, "phase")

        def target(i):
            d = _unit(B.point(Ms[i], c["tip"]) - P[i])
            d = _unit((1.0 - c["plumb"]) * d + c["plumb"] * down)
            return P[i] + _unit(d + Ms[i][:3, :3] @ side_l * sway * math.sin(sway_w * i + sway_0)) * length

        T, V = target(0), np.zeros(3)
        Sl = []
        for i in range(n + 1):
            goal = target(i)
            for _ in range(4):
                h = 1.0 / (SUB * 4)
                V = V + (k * (goal - T) - damp * V) * h
                T = T + V * h
            T = P[i] + _unit(T - P[i]) * length
            rigid = _unit(B.point(Ms[i], c["tip"]) - P[i])
            S = _arc(rigid, _unit(T - P[i]))
            Rp = Ms[i][:3, :3]
            Sl.append(Rp.T @ S @ Rp)
        base = Sl[0]
        peak = 0.0
        for i in range(n + 1):
            S = base.T @ Sl[i]
            ang = math.degrees(math.acos(max(-1.0, min(1.0, (np.trace(S) - 1.0) / 2.0))))
            f = fade[i] * min(c["gain"], c["max"] / max(ang, 1e-6))
            if out_l is not None and ang > 1e-3:      # cloth does not pass into the body it hangs on
                lim_in = math.sin(math.radians(c["inward"]))
                if -float((_rot_pow(S, f) @ tip_l - tip_l) @ out_l) > lim_in:
                    lo, hi = 0.0, f
                    for _ in range(10):
                        mid = (lo + hi) / 2.0
                        if -float((_rot_pow(S, mid) @ tip_l - tip_l) @ out_l) > lim_in:
                            hi = mid
                        else:
                            lo = mid
                    f = lo
            e = np.array(B.decompose_zyx(_rot_pow(S, f)))
            if c.get("lim"):
                e = np.array([min(hi_, max(lo_, v)) for v, (lo_, hi_) in zip(e, c["lim"])])
            peak = max(peak, float(np.linalg.norm(e)))
            for b in bones:
                bone = B.MODEL.bones[b]
                rot = tuple(float(v) for v in (np.array(rest0[b]) + e))
                q = np.array([-c["pivot"][0], c["pivot"][1], c["pivot"][2]]) / 16.0
                off = (np.eye(3) - _rot_of(e)) @ (q - bone.pivot / 16.0) * 16.0
                pos = (float(-off[0]), float(off[1]), float(off[2])) if np.linalg.norm(off) > 1e-4 else None
                (legs if c.get("clip") == "legs" else upper)[i][b] = (rot, pos)
                (upper if c.get("clip") == "legs" else legs)[i].pop(b, None)
        if report:
            print("  soft %-22s %-5s swings %4.1f deg (period %.1f, on %s%s)"
                  % (name, c.get("kind", ""), peak, c["period"], c["parent"], ", legs clip" if c.get("clip") == "legs" else ""))
    for plate, (arm, f) in plates.items():
        if plate not in B.MODEL.bones or arm not in upper[0]:
            continue
        rest = B.MODEL.bones[arm].rot
        R0 = _rot_of(upper[0][arm][0], rest)
        for i in range(n + 1):
            Ra = _rot_of(upper[i][arm][0], rest)
            D = Ra @ R0.T          # the arm's turn since the first sample, in its parent's frame
            turn = math.degrees(math.acos(max(-1.0, min(1.0, (np.trace(D) - 1.0) / 2.0))))
            X = Ra.T @ _rot_pow(D, -min(f * turn, PLATE_LAG) / max(turn, 1e-6)) @ Ra
            upper[i][plate] = (tuple(float(v) for v in B.decompose_zyx(X)), None)
    _plates_yield(upper, legs, plates, fade, report)
    return upper, legs


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
    impact = spec.get("impact")
    edge_from = spec.get("edge_from", (impact - EDGE_BEFORE) if impact is not None else 0.0)
    edge_to = (impact + EDGE_AFTER) if impact is not None else float(ticks)
    # The reference edge is the guard's edge carried along the blade's own path with no turn about the blade's length
    # (parallel transport in the world): the blade swings like a clock hand and never spins. The roll below is measured
    # from it, so a roll of 0 is "no spin", whatever the arm does underneath.
    carried = [_unit(g_edge - (g_edge @ rows[0]["d"]) * rows[0]["d"])]
    for i in range(1, n + 1):
        e = _arc(rows[i - 1]["d"], rows[i]["d"]) @ carried[-1]
        carried.append(_unit(e - (e @ rows[i]["d"]) * rows[i]["d"]))
    # Carried round a closed path the edge comes back turned by the path's own twist: the recovery pays it back slowly.
    e_end = _unit(g_edge - (g_edge @ rows[n]["d"]) * rows[n]["d"])
    home = math.degrees(math.atan2(float(np.cross(carried[n], e_end) @ rows[n]["d"]), float(carried[n] @ e_end)))
    rolls, phi, w_prev, env = [], 0.0, 0.0, 0.0
    for i, r in enumerate(rows):
        a, b = rows[max(0, i - 1)], rows[min(n, i + 1)]
        v = (b["tip"] - a["tip"]) / max(1e-9, b["t"] - a["t"])
        vp = v - (v @ r["d"]) * r["d"]
        speed = float(np.linalg.norm(vp))
        nat = carried[i]
        w = min(1.0, max(0.0, (speed - EDGE_SLOW) / (EDGE_FAST - EDGE_SLOW)))
        if r["t"] < edge_from:      # the wind-up: the hand keeps its grip, the blade does not turn in it
            w = 0.0
        if speed >= EDGE_SLOW and w > 0.0:      # a slow tip has no line of travel worth turning the edge to: the roll is kept
            raw = math.degrees(math.atan2(float(np.cross(nat, vp) @ r["d"]), float(nat @ vp)))    # nat -> travel, about d
            # A double-edged blade: either edge may lead (the roll is free by half turns). The nearer edge is picked as a
            # swing starts and then followed, so the hand never snaps half a turn in the middle of a cut.
            ref = 0.0 if w_prev < 0.05 else phi
            phi = ref + ((raw - ref + 90.0) % 180.0 - 90.0)
        # The edge stays on the cut's line through the follow-through and lets go slowly (Kort 2026-11-08: "late in the
        # slash the blade turned flat against the target").
        env = max(w, env - 1.0 / (EDGE_RELEASE * SUB))
        r["nat"], r["speed"], r["w"] = nat, speed, w
        base = home if (impact is not None and r["t"] > impact) else 0.0
        rolls.append(base + env * (phi - base))
        w_prev = env
    # Smooth the roll over half a tick each way, but never where the edge must be on the line (fast samples); then cap
    # how fast the hand may turn about the blade, so the roll in and out of a cut is a turn of the wrist, not a spin.
    rolls = [rolls[i] if r["w"] >= 1.0 else float(np.mean(rolls[max(0, i - 2):min(n, i + 2) + 1]))
             for i, r in enumerate(rows)]
    for order in (range(1, n + 1), range(n - 1, -1, -1)):
        if order.start != 1:
            rolls[n] = home          # the clip ends on the guard's own grip
        for i in order:
            j = i - 1 if order.start == 1 else i + 1
            a_, b_ = sorted((rows[i]["t"], rows[j]["t"]))
            rate = ROLL_RATE if (a_ >= edge_from - 1.0 and b_ <= edge_to) else ROLL_CALM
            rolls[i] = rolls[j] + max(-rate, min(rate, rolls[i] - rolls[j]))
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
    c0 = upper[0]["cape"]
    for u in upper:                          # the stance's own cape pose; soft_motion swings it from there
        u["cape"] = c0
    upper, legs = soft_motion(upper, legs, spec.get("soft"), spec.get("plates"), report=report)
    if report:
        worst = max(_euler_gap(upper, b) for b in B.UPPER_BONES)
        print("fullbody: %d samples, peak tip speed %.1f px/tick, worst between-key error %.2f deg, path twist %.1f deg"
              % (n + 1, max(r["speed"] for r in rows), worst, home))
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
