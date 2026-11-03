import numpy as np
from troop_render import Troop
from pose_solve import root_of, seg_closest, unit
k = Troop('gb_hollowking', 3, 'violet'); a = Troop('tc_admiral', 3, 'teal')
KP = {'body': (12, 18, 0), 'head': (-10, -10, 0), 'left_arm': (-70, -30, -15), 'right_leg': (-26, 0, 0), 'left_leg': (20, 0, 0)}
AP = {'body': (8, -15, 0), 'head': (-8, 10, 0), 'left_arm': (-95, 25, -10), 'right_leg': (-28, 0, 0), 'left_leg': (26, 0, 0)}
for kg in (150, 165, 180, 195):
  for ka in (-130, -145, -160):
    kp = dict(KP, right_arm=(ka, 0, 10), _grip=(kg, 0, 0))
    rk = root_of(k, kp, 12, 0, 50); t, b = k.weapon_ends(kp, rk)
    print('K arm', ka, 'grip', kg, 'butt', np.round(b, 1), 'tip', np.round(t, 1), 'dir', np.round(unit(t - b), 2))
for ag in (180, 200, 215, 230):
  for aa in (-100, -115):
    ap = dict(AP, right_arm=(aa, 0, 0), _grip=(ag, 0, 0))
    ra = root_of(a, ap, -12, 0, -50); t, b = a.weapon_ends(ap, ra)
    print('A arm', aa, 'grip', ag, 'butt', np.round(b, 1), 'tip', np.round(t, 1), 'dir', np.round(unit(t - b), 2))
