"""Forward kinematics and leg IK for the Web Stalker, mirroring tools/anim/render_geo.py (itself GeckoLib 4.8.4's bone transform).

Coordinates are render_geo's world at root offset 0, in px: +x the spider's left, +y up, +z forward. A pose is
{bone: {"rotation": [x, y, z] deg, "position": [x, y, z] px}} in clip-key units, exactly as the animation file stores them.

Each leg is two bones: the femur (<side>_leg_<i>, from the body) and the tibia (<side>_leg_<i>_low, with the claw). The claw's
tip is the foot: solve_leg turns the femur (yaw: the sweep fore and aft; roll: the lift) and bends the tibia so the tip lands on a
target, the knee always up (a spider's leg arches over its foot).
"""
import math, os, sys
import numpy as np
from scipy.optimize import least_squares

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.normpath(os.path.join(HERE, "..", "..", "..", "tools", "anim")))
import render_geo as R                                       # noqa: E402

GEO = os.path.join(HERE, "out", "wm_stalker.geo.json")
LEGS = [f"{s}_leg_{i}" for s in ("left", "right") for i in (1, 2, 3, 4)]
# the alternating tetrapod: set A steps together, then set B
SET_A = ["left_leg_1", "right_leg_2", "left_leg_3", "right_leg_4"]
SET_B = ["right_leg_1", "left_leg_2", "right_leg_3", "left_leg_4"]


def side(leg):
    return 1 if leg.startswith("left") else -1


class Spider:
    def __init__(s, geo=GEO):
        s.m = R.load_geo(geo)
        s.order = list(s.m.order)
        s.tip_rest = {}
        for leg in LEGS:   # the claw's far end, rest model space px
            low = leg + "_low"
            pv = s.m.bones[low].pivot
            best = None
            for ci, cube in enumerate(s.m.cube_list):
                if s.m.cube_bone[ci] != low or tuple(cube.size) != (3, 1, 1):
                    continue
                c = cube.corners * 16.0
                d = np.linalg.norm(c - pv, axis=1)
                far = c[np.argsort(d)[-4:]].mean(axis=0)
                best = far
            s.tip_rest[leg] = best

    def local(s, name, p):
        b = s.m.bones[name]
        rot = b.rot.copy()
        pos = (0.0, 0.0, 0.0)
        if p:
            if "rotation" in p:
                x, y, z = p["rotation"]
                rot = rot + np.radians([-x, -y, z])
            if "position" in p:
                pos = p["position"]
        pv = b.pivot / 16.0
        M = R._T(-pos[0] / 16.0, pos[1] / 16.0, pos[2] / 16.0) @ R._T(*pv)
        return M @ R._Rz(rot[2]) @ R._Ry(rot[1]) @ R._Rx(rot[0]) @ R._T(*(-pv))

    def fk(s, pose, only=None):
        out = {}
        need = None
        if only:
            need = set()
            for n in only:
                while n:
                    need.add(n); n = s.m.bones[n].parent
        for n in s.order:
            if need is not None and n not in need:
                continue
            b = s.m.bones[n]
            par = out[b.parent] if b.parent else R.WORLD_FLIP
            out[n] = par @ s.local(n, pose.get(n))
        return out

    def point(s, M, bone, rest_px):
        p = M[bone] @ np.append(np.asarray(rest_px, float) / 16.0, 1.0)
        return p[:3] * 16.0

    def tip(s, M, leg):
        return s.point(M, leg + "_low", s.tip_rest[leg])

    def knee(s, M, leg):
        return s.point(M, leg + "_low", s.m.bones[leg + "_low"].pivot)

    def solve_leg(s, leg, target, base, guess=None):
        """Femur [pitch, yaw, roll] and tibia bend (deg, clip keys) putting the claw tip on `target` (world px). `base` is the world
        matrix of the femur's parent (body). The femur's roll lifts about the body's forward axis, which goes slack once a fore or
        hind leg swings toward the front or back (gimbal), so its pitch is free too, but costs more than a yaw or a roll. Soft
        terms keep the knee above the claw and, on the fore and hind pairs, out ahead of (behind) the hip."""
        sg = side(leg)
        i = int(leg[-1])
        names = [leg, leg + "_low"]
        hip = None

        def mats(v):
            M = {}
            M[names[0]] = base @ s.local(names[0], {"rotation": [v[0], v[1], v[2]]})
            M[names[1]] = M[names[0]] @ s.local(names[1], {"rotation": [0, 0, v[3]]})
            return M

        def res(v):
            M = mats(v)
            tp, kn = s.tip(M, leg), s.knee(M, leg)
            e = (tp - target) * 4.0
            soft = [v[0] * 0.03, v[1] * 0.002, v[2] * 0.002, max(0.0, tp[1] + 1.0 - kn[1])]
            if i in (1, 4):
                fwd = 1 if i == 1 else -1
                soft.append(max(0.0, (hip[2] - kn[2]) * fwd) * 0.5)
            return np.concatenate([e, soft])
        hip = (base @ np.append(s.m.bones[leg].pivot / 16.0, 1.0))[:3] * 16.0
        # the tibia only folds downward (the knee stays up): its bend sign by side
        lo = [-80, -90, -125, -10 if sg > 0 else -140]
        hi = [80, 90, 125, 140 if sg > 0 else 10]
        x0 = np.array(guess if guess is not None else [0.0, 0.0, -60.0 * sg, 60.0 * sg], float)
        x0 = np.clip(x0, np.array(lo) + 1e-6, np.array(hi) - 1e-6)
        r = least_squares(res, x0, bounds=(lo, hi), xtol=1e-9, ftol=1e-9)
        M = mats(r.x)
        err = float(np.linalg.norm(s.tip(M, leg) - target))
        return list(r.x), err, M
