"""Forward kinematics and leg IK for the Moot Hound, mirroring tools/anim/render_geo.py (itself GeckoLib 4.8.4's bone transform).

Coordinates are render_geo's world at root offset 0, in px: +x the hound's left, +y up, +z forward. A pose is
{bone: {"rotation": [x, y, z] deg, "position": [x, y, z] px}} in clip-key units, exactly as the animation file stores them.
"""
import math, os, sys
import numpy as np
from scipy.optimize import least_squares

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.normpath(os.path.join(HERE, "..", "..", "..", "tools", "anim")))
import render_geo as R                                       # noqa: E402

GEO = os.path.join(HERE, "out", "wm_hound.geo.json")
LEGS = ["front_right", "front_left", "back_right", "back_left"]
SOLE = np.array([0.0, -1.0, -0.5])   # the paw's sole centre from the foot pivot, internal px (front is -z inside)


class Hound:
    def __init__(s, geo=GEO):
        s.m = R.load_geo(geo)
        s.order = [n for n in s.m.order]

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
        """World matrices (blocks) of every bone (or of `only` and its ancestors)."""
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

    def point(s, M, bone, local_px):
        b = s.m.bones[bone]
        p = M[bone] @ np.append((b.pivot + local_px) / 16.0, 1.0)
        return p[:3] * 16.0

    def sole(s, M, leg):
        return s.point(M, leg + "_foot", SOLE)

    def paw_pitch(s, M, leg):
        """World pitch of the paw, deg: + toe up (its forward axis turned toward +y)."""
        f = M[leg + "_foot"][:3, :3] @ np.array([0.0, 0.0, -1.0])   # internal forward
        return math.degrees(math.atan2(f[1], f[2]))

    def solve_leg(s, pose, leg, target, pitch=0.0, guess=None, base=None):
        """Leg, shin and foot rotations putting the sole centre on `target` (world px) with the paw at `pitch` (deg, + toe up).
        Front legs fold with the joint forward (paw tucked back), back legs with the hock backward."""
        par = s.m.bones[leg + "_leg"].parent
        Mp = base if base is not None else s.fk(pose, [par])[par]
        front = leg.startswith("front")
        names = [leg + "_leg", leg + "_shin", leg + "_foot"]

        def mats(v):
            lx, lz, sx, fx = v
            M = {par: Mp}
            M[names[0]] = Mp @ s.local(names[0], {"rotation": [lx, 0, lz]})
            M[names[1]] = M[names[0]] @ s.local(names[1], {"rotation": [sx, 0, 0]})
            M[names[2]] = M[names[1]] @ s.local(names[2], {"rotation": [fx, 0, 0]})
            return M

        def res(v):
            M = mats(v)
            e = (s.sole(M, leg) - target) * 4.0
            pe = math.radians(s.paw_pitch(M, leg) - pitch) * 3.0
            return np.concatenate([e, [pe, v[1] * 0.01]])
        x0 = np.array(guess if guess is not None else [0.0, 0.0, 0.0, 0.0], float)
        lo = [-120, -40, (0 if front else -150), -150]
        hi = [120, 40, (150 if front else 0), 150]
        x0 = np.clip(x0, np.array(lo) + 1e-6, np.array(hi) - 1e-6)
        r = least_squares(res, x0, bounds=(lo, hi), xtol=1e-8, ftol=1e-8)
        M = mats(r.x)
        err = float(np.linalg.norm(s.sole(M, leg) - target))
        return list(r.x), err
