import sys, json
import numpy as np
from PIL import Image
from troop_render import *
from pose_solve import root_of

def view(k, kp, a, ap, out, cams=((0, -16, -115),), W=900, H=640):
    rk = root_of(k, kp, 12, 0, 50); ra = root_of(a, ap, -12, 0, -50)
    items = [(c, uv, t_, n, 0) for c, uv, t_, n, g in k.quads(kp, rk)] + [(c, uv, t_, n, 1) for c, uv, t_, n, g in a.quads(ap, ra)]
    tx = {**troop_textures(k), **troop_textures(a)}
    panels = []
    for eye in cams:
        cam = Camera(eye, (0, -26, 0), 40, W, H)
        gb = rasterize(items, cam, tx)
        L = unit = np.array([0.3, -0.5, 0.8]); L = L / np.linalg.norm(L)
        F = np.array([-0.4, -0.6, -0.7]); F /= np.linalg.norm(F)
        nl = gb['normal']
        sh = np.clip((nl * L).sum(2), 0, 1) * 0.9 + np.clip((nl * F).sum(2), 0, 1) * 0.45 + 0.2
        img = gb['albedo'] * sh[..., None] + gb['glow']
        img[~gb['mask']] = (0.2, 0.18, 0.22)
        panels.append((np.clip(img, 0, 1) * 255).astype(np.uint8))
    Image.fromarray(np.concatenate(panels, 1)).save(out)

if __name__ == '__main__':
    k = Troop('gb_hollowking', 3, 'violet'); a = Troop('tc_admiral', 3, 'teal')
    P = json.load(open(sys.argv[1]))
    view(k, P['king'], a, P['adm'], sys.argv[2], cams=((0, -16, -115), (-110, -20, 0), (0, -140, -40)))
