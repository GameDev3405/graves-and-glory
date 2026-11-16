"""Forward kinematics and a small least-squares IK for camp NPC rigs, mirroring tools/anim/render_geo.py (itself GeckoLib
4.8.4's bone transform), as art/anim/wm_hound/hound_ik.py does for the hound.

Points and directions are given in the geo file's own frame (px; y up, front -z, the NPC's right -x) and come back in the
same frame, so a builder can reason in the coordinates its model is written in. A pose is
{bone: {"rotation": [x, y, z] deg, "position": [x, y, z] px}} in clip-key units, exactly as the animation file stores them.
"""
import os, sys
import numpy as np
from scipy.optimize import least_squares

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.normpath(os.path.join(HERE, "..", "..", "tools", "anim")))
import render_geo as R                                       # noqa: E402

_F = np.array([1.0, 1.0, -1.0])       # file frame <-> render_geo's world: z flips (the world looks down +z)


class Rig:
    def __init__(s, geo):
        s.m = R.load_geo(geo)
        s.order = list(s.m.order)

    def local(s, name, p):
        b = s.m.bones[name]
        rot, pos = b.rot.copy(), (0.0, 0.0, 0.0)
        if p:
            if "rotation" in p:
                x, y, z = p["rotation"]
                rot = rot + np.radians([-x, -y, z])
            if "position" in p: pos = p["position"]
        pv = b.pivot / 16.0
        M = R._T(-pos[0] / 16.0, pos[1] / 16.0, pos[2] / 16.0) @ R._T(*pv)
        return M @ R._Rz(rot[2]) @ R._Ry(rot[1]) @ R._Rx(rot[0]) @ R._T(*(-pv))

    def chain(s, name):
        out = []
        while name: out.append(name); name = s.m.bones[name].parent
        return out[::-1]

    def fk(s, pose, only=None, base=None):
        """World matrices of every bone, or of `only` and their ancestors. base: {bone: matrix} already known (kept as is)."""
        out = dict(base) if base else {}
        need = None
        if only:
            need = set()
            for n in only: need.update(s.chain(n))
        for n in s.order:
            if n in out or (need is not None and n not in need): continue
            b = s.m.bones[n]
            out[n] = (out[b.parent] if b.parent else R.WORLD_FLIP) @ s.local(n, pose.get(n))
        return out

    def pt(s, M, bone, p):
        """Where the rest-pose file point p (px), carried by `bone`, is now (file frame, px)."""
        g = np.array([-p[0], p[1], p[2], 16.0]) / 16.0
        return (M[bone] @ g)[:3] * 16.0 * _F

    def vec(s, M, bone, v):
        """Where the rest-pose file direction v, carried by `bone`, points now (file frame)."""
        g = np.array([-v[0], v[1], v[2]])
        return (M[bone][:3, :3] @ g) * _F

    def solve(s, pose, free, res, bounds=None, reg=0.02, guess=None):
        """Least-squares IK. free: [(bone, axis)] rotation components to solve, starting from `guess` (or the pose's own
        values); res(M) -> residuals, M holding the matrices of every bone below the first free bone's parent. Returns
        ({bone: rotation}, residual norm) and writes the rotations into pose. reg pulls toward the start (deg weight)."""
        bones = []
        for b, _ in free:
            if b not in bones: bones.append(b)
        first = min(bones, key=lambda b: len(s.chain(b)))
        par = s.m.bones[first].parent
        base = s.fk(pose, [par]) if par else {}
        below = [n for n in s.order if first in s.chain(n)]
        x0 = np.array([pose.get(b, {}).get("rotation", [0, 0, 0])[a] for b, a in free], float) if guess is None else np.array(guess, float)

        def apply(v):
            for (b, a), val in zip(free, v):
                r = list(pose.setdefault(b, {}).get("rotation", [0.0, 0.0, 0.0])); r[a] = float(val); pose[b]["rotation"] = r

        def mats(v):
            apply(v)
            return s.fk(pose, below, base)

        def f(v):
            return np.concatenate([np.asarray(res(mats(v)), float), (v - x0) * reg])
        if bounds is None: lo, hi = [-180.0] * len(free), [180.0] * len(free)
        else: lo, hi = [b[0] for b in bounds], [b[1] for b in bounds]
        start = np.clip(x0, np.array(lo) + 1e-6, np.array(hi) - 1e-6)
        r = least_squares(f, start, bounds=(lo, hi), xtol=1e-9, ftol=1e-9)
        M = mats(r.x)
        return M, float(np.linalg.norm(np.asarray(res(M), float)))
