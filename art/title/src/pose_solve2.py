import json
import numpy as np
from scipy.optimize import minimize
from troop_render import Troop
from pose_solve import root_of, seg_closest, unit
from pose_probe import k, a, KP, AP

def poses(v):
    kp = dict(KP, right_arm=tuple(v[0:3]), _grip=(v[3], 0, 0))
    ap = dict(AP, right_arm=tuple(v[4:7]), _grip=(v[7], 0, 0))
    return kp, ap

RK = root_of(k, KP, 12, 0, 50); RA = root_of(a, AP, -12, 0, -50)

def evalv(v):
    kp, ap = poses(v)
    kt, kb = k.weapon_ends(kp, RK); at, ab = a.weapon_ends(ap, RA)
    pk, pa, s, t = seg_closest(kb, kt, ab, at)
    return pk, pa, s, t, unit(kt - kb), unit(at - ab)

def cost(v):
    pk, pa, s, t, dk, da = evalv(v)
    c = 30 * np.sum((pk - pa) ** 2) + 30 * (s - 0.6) ** 2 * 100 + 30 * (t - 0.78) ** 2 * 100
    m = (pk + pa) / 2
    c += 2 * ((m[0] + 1) ** 2 + (m[1] + 42) ** 2)
    c += 0.002 * np.sum(np.square(v[[1, 2, 5, 6]]))
    return c

x0 = np.array([-160, 0, 10, 150, -125, 0, 0, 150], float)
b = [(-185, -110), (-50, 50), (-30, 50), (120, 200), (-175, -90), (-50, 50), (-50, 30), (100, 200)]
best = None
rng = np.random.default_rng(2)
for i in range(6):
    st = x0 if i == 0 else np.clip(x0 + rng.normal(0, 15, 8), [l for l, _ in b], [h for _, h in b])
    r = minimize(cost, st, method='Powell', bounds=b, options=dict(maxiter=6000))
    if best is None or r.fun < best.fun: best = r
pk, pa, s, t, dk, da = evalv(best.x)
print(round(best.fun, 2), np.round(best.x, 1), 'gap', round(np.linalg.norm(pk - pa), 2), 'at', np.round((pk + pa) / 2, 1), round(s, 2), round(t, 2), np.round(dk, 2), np.round(da, 2))
kp, ap = poses(best.x)
json.dump({'king': {k_: list(map(float, v)) for k_, v in kp.items()}, 'adm': {k_: list(map(float, v)) for k_, v in ap.items()}}, open('pose.json', 'w'))
