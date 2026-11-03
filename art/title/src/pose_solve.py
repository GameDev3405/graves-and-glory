"""Solves both heroes' weapon arms together so the King's blade and the Admiral's trident cross between them (key art pose).
Cost: the gap between the two weapon axes at their crossing, the blade and shaft directions against the wanted ones, the
crossing's place, and how far each grip turns from its in-game grip (hands keep holding their weapons the game's way)."""
import numpy as np
from scipy.optimize import minimize
from troop_render import Troop, rot


def root_of(t, pose, x, z, yaw):
    ys = np.concatenate([c[:, 1] for c, *_ in t.quads(pose)])
    R = np.eye(4); R[:3, :3] = rot((0, yaw, 0)); R[:3, 3] = (x, 0, z)
    T = np.eye(4); T[1, 3] = -ys.max()
    return R @ T


def seg_closest(p0, p1, q0, q1):
    u, v, w = p1 - p0, q1 - q0, p0 - q0
    a, b, c, d, e = u @ u, u @ v, v @ v, u @ w, v @ w
    den = a * c - b * b
    s = np.clip((b * e - c * d) / den, 0, 1) if den > 1e-9 else 0
    t = np.clip((a * e - b * d) / (b * b - a * c) * -1 if False else (e + b * s) / c, 0, 1)
    s = np.clip((t * b - d) / a, 0, 1)
    return p0 + u * s, q0 + v * t, s, t


def unit(v):
    v = np.asarray(v, float); return v / np.linalg.norm(v)


def solve_pair(k, KP, rk, a, AP, ra, want):
    def poses(v):
        kp = dict(KP); ap = dict(AP)
        kp['right_arm'] = tuple(v[0:3]); kp['_grip'] = tuple(v[3:6])
        ap['right_arm'] = tuple(v[6:9]); ap['_grip'] = tuple(v[9:12])
        return kp, ap

    def cost(v):
        kp, ap = poses(v)
        kt, kb = k.weapon_ends(kp, rk)
        at, ab = a.weapon_ends(ap, ra)
        pk, pa, s, t = seg_closest(kb, kt, ab, at)
        c = 20 * np.sum((pk - pa) ** 2)
        c += 300 * np.sum((unit(kt - kb) - unit(want['king_dir'])) ** 2)
        c += 300 * np.sum((unit(at - ab) - unit(want['adm_dir'])) ** 2)
        c += 2 * np.sum(((pk + pa) / 2 - want['contact']) ** 2)
        c += 40 * (s - want['king_frac']) ** 2 + 40 * (t - want['adm_frac']) ** 2
        c += 0.01 * (np.sum(np.square(v[3:6])) + np.sum(np.square(v[9:12])))
        return c
    rng = np.random.default_rng(1)
    x0 = np.array([-150, 0, 20, 0, 0, 0, -100, 0, 0, 170, 0, 0], float)
    b = [(-200, -40), (-60, 60), (-40, 60), (-60, 60), (-60, 60), (-60, 60),
         (-200, -40), (-60, 60), (-60, 40), (100, 240), (-60, 60), (-60, 60)]
    best = None
    for i in range(30):
        st = x0 if i == 0 else np.clip(x0 + rng.normal(0, 35, 12), [l for l, _ in b], [h for _, h in b])
        r = minimize(cost, st, method='Powell', bounds=b, options=dict(maxiter=8000, xtol=1e-3, ftol=1e-7))
        if best is None or r.fun < best.fun:
            best = r
    kp, ap = poses(best.x)
    kt, kb = k.weapon_ends(kp, rk); at, ab = a.weapon_ends(ap, ra)
    pk, pa, s, t = seg_closest(kb, kt, ab, at)
    return kp, ap, best.fun, (pk + pa) / 2, np.linalg.norm(pk - pa), s, t


KP = {'body': (12, 18, 0), 'head': (-10, -10, 0), 'left_arm': (-70, -30, -15), 'right_leg': (-26, 0, 0), 'left_leg': (20, 0, 0)}
AP = {'body': (8, -15, 0), 'head': (-8, 10, 0), 'left_arm': (-95, 25, -10), 'right_leg': (-28, 0, 0), 'left_leg': (26, 0, 0)}
WANT = dict(king_dir=(-1, 0.5, -0.15), adm_dir=(0.6, -1, -0.2), contact=np.array([0, -38, -2.0]), king_frac=0.6, adm_frac=0.85)

if __name__ == '__main__':
    k = Troop('gb_hollowking', 3, 'violet'); a = Troop('tc_admiral', 3, 'teal')
    rk = root_of(k, KP, 12, 0, 50); ra = root_of(a, AP, -12, 0, -50)
    kp, ap, f, c, gap, s, t = solve_pair(k, KP, rk, a, AP, ra, WANT)
    print('cost', round(f, 2), 'contact', np.round(c, 1), 'gap', round(gap, 2), 's,t', round(s, 2), round(t, 2))
    print('KING', {k_: np.round(v, 1).tolist() for k_, v in kp.items()})
    print('ADM', {k_: np.round(v, 1).tolist() for k_, v in ap.items()})
