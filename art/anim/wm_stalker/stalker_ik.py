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

    def cubes_of(s, bones):
        """[(bone, centre, half extents, lattice)] of every cube on `bones`, rest model space blocks; the lattice (N x 4,
        homogeneous) is focus_check's: a grid at most 1 px apart, corners included. The rig's cubes are unrotated."""
        out = []
        for ci, cube in enumerate(s.m.cube_list):
            b = s.m.cube_bone[ci]
            if b not in bones:
                continue
            c = cube.corners
            lo, hi = c.min(axis=0), c.max(axis=0)
            n = [max(2, min(25, int(math.ceil((hi[k] - lo[k]) * 16.0 - 1e-6)) + 1)) for k in range(3)]
            g = np.stack(np.meshgrid(*[np.linspace(lo[k], hi[k], n[k]) for k in range(3)], indexing="ij"), -1).reshape(-1, 3)
            out.append((b, (lo + hi) / 2, (hi - lo) / 2, np.hstack([g, np.ones((len(g), 1))])))
        return out

    def leg_cubes(s, leg):
        key = ("legcubes", leg)
        if key not in s.__dict__:
            s.__dict__[key] = s.cubes_of({leg, leg + "_low", leg + "_guard", leg + "_spur"})
        return s.__dict__[key]

    @staticmethod
    def pair_depth(Ma, ca, ha, Pa, Mb, cb, hb, Pb):
        """How deep (px) two posed boxes sink into each other: the deepest lattice point of either inside the other, as
        focus_check measures it. M: bone world matrices; c, h: box centre and half extents in the bone's rest frame."""
        Ab = np.linalg.inv(Mb) @ Ma          # a's rest frame to b's
        qa = (Pa @ Ab.T)[:, :3] - cb
        d1 = float((hb - np.abs(qa)).min(axis=1).max())
        qb = (Pb @ np.linalg.inv(Ab).T)[:, :3] - ca
        d2 = float((ha - np.abs(qb)).min(axis=1).max())
        return max(d1, d2) * 16.0

    def solve_leg(s, leg, target, base, guess=None, keep=0.0, avoid=None):
        """Femur [pitch, yaw, roll] and tibia bend (deg, clip keys) putting the claw tip on `target` (world px). `base` is the world
        matrix of the femur's parent (body). The femur's roll lifts about the body's forward axis, which goes slack once a fore or
        hind leg swings toward the front or back (gimbal), so its pitch is free too, but costs more than a yaw or a roll. Soft
        terms keep the knee above the claw and, on the fore and hind pairs, out ahead of (behind) the hip.

        keep (per deg): pulls the answer toward `guess` (the previous sample's), so the leg's spare degree of freedom does not
        flick from one sample to the next. avoid: ([(body bone, world matrix, centre, half, lattice)], {(leg bone, body bone):
        allowed px}): the leg's cubes stay out of those body cubes (beyond what the rest pose already overlaps, per pair of
        bones, as focus_check's armour_clip judges it), using the same spare freedom first, then the tip."""
        sg = side(leg)
        i = int(leg[-1])
        names = [leg, leg + "_low"]
        hip = None
        g0 = np.array(guess, float) if (guess is not None and keep > 0) else None
        lc = s.leg_cubes(leg) if avoid else None

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
            if g0 is not None:
                soft.extend((np.asarray(v) - g0) * keep)
            if avoid:
                body, allow = avoid
                worst = {}
                for lb, lcen, lh, lp in lc:
                    # guard and spur ride the femur unkeyed (their own transform is the identity about the femur's pivot)
                    Ml = M[names[1]] if lb == names[1] else M[names[0]]
                    lw = (Ml @ np.append(lcen, 1.0))[:3]
                    lr = float(np.linalg.norm(lh))
                    for bb, Mb, bc, bh, bp, bw, br in body:
                        if np.linalg.norm(lw - bw) > lr + br:      # bounding spheres apart: nothing to measure
                            continue
                        k = (lb, bb)
                        worst[k] = max(worst.get(k, 0.0), s.pair_depth(Ml, lcen, lh, lp, Mb, bc, bh, bp))
                over = max([0.0] + [d - allow.get(k, allow["_"]) for k, d in worst.items()])
                soft.append(over * 3.0)
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
