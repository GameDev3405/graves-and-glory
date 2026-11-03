"""Title key art from the real troop models (board 4F.3, Kort's route "real models, painted over", 2026-11-03).

The Hollow King and Admiral Vael lock weapons in front; the Tidecallers press in from the left and the Graveborn from
the right; stormy dusk with the sun low behind the clash. Every figure is the in-game model and skin, rendered by
troop_render into G-buffers and lit here (backlit sun, cool sky fill, screen-space rim, ambient occlusion, depth haze).
The sky, plain, dust, rain and embers come from paint_title.Scene; there is no stroke filter.

Usage: python key_art.py OUT.png [--scale 0.25] [--seed N]
"""
import json
import math
import sys
from pathlib import Path

import cv2
import numpy as np

import paint_title as pt
from paint_title import hexc, mix, smooth, fbm
from pose_solve import root_of
from troop_render import Troop, Camera, rasterize, troop_textures

HERE = Path(__file__).parent
EYE, TARGET, FOV = (0, -13, -128), (0, -36.5, 0), 36
SUN_DIR = np.array([0.12, -0.13, 1.0]) / np.linalg.norm([0.12, -0.13, 1.0])   # toward the sun, low behind the clash

TIDE = ['tc_tideguard', 'tc_tideguard', 'tc_raider', 'tc_harpooner', 'tc_raider']
GRAVE = ['revenant', 'revenant', 'reaper', 'revenant', 'reaper']


def crowd_pose(rng):
    s = rng.uniform(-28, 28)
    return {'body': (rng.uniform(-4, 16), rng.uniform(-10, 10), 0), 'head': (rng.uniform(-12, 6), rng.uniform(-20, 20), 0),
            'right_arm': (rng.uniform(-160, -40), rng.uniform(-20, 20), rng.uniform(-5, 25)),
            'left_arm': (rng.uniform(-80, 5), rng.uniform(-15, 15), rng.uniform(-25, 0)),
            'right_leg': (s, 0, 0), 'left_leg': (-s, 0, 0)}


def crowd(rng):
    """(troop id, cloth, x, z, yaw) for both lines. They meet behind the heroes and thin out into the haze."""
    out = []
    for side, ids, cloth, yaw in ((-1, TIDE, 'teal', -90), (1, GRAVE, 'violet', 90)):
        for row in range(14):
            z = 75 + row * 34 + rng.uniform(-8, 8)
            n = 5 + row
            for i in range(n):
                front = side * rng.uniform(2, 12)          # the two lines overlap a little where they meet
                x = front + side * (i * rng.uniform(15, 24) + rng.uniform(0, 8)) + side * row * 3
                if abs(x) < 26 and z < 60:
                    continue                                 # keep the heroes clear
                out.append((ids[rng.integers(len(ids))], cloth, x, z, yaw + rng.uniform(-35, 35)))
    return out


def lit(gb, cam, fog_col, sun_col, fog_k, burst=None):
    """Shade one G-buffer: sun from behind, cool sky and front fill, rim on the sun side, AO, haze by depth."""
    n, alb, m = gb['normal'], gb['albedo'], gb['mask']
    ndl = lambda d: np.clip((n * (np.array(d) / np.linalg.norm(d))).sum(2), 0, 1)[..., None]
    light = (0.10 * hexc('#5A4E6A') +
             ndl(SUN_DIR) * sun_col * 1.5 +
             ndl((0, -1, -0.15)) * hexc('#5C5878') * 0.55 +
             ndl((-0.55, -0.35, -1)) * hexc('#6A7896') * 0.5 +
             ndl((0.8, -0.2, -0.6)) * hexc('#7A5A66') * 0.28)
    # ambient occlusion from the depth buffer
    d = np.where(m, gb['depth'], 0).astype(np.float32)
    w = cv2.GaussianBlur(m.astype(np.float32), (0, 0), 5) + 1e-4
    dm = cv2.GaussianBlur(d, (0, 0), 5) / w
    ao = 1 - 0.5 * np.clip((gb['depth'] - dm) / 6, 0, 1)
    col = alb * light * np.where(m, ao, 1)[..., None]
    # specular glints on metal (the skin's specular map), sun half-vector
    view = -np.array(cam.R[2])
    h = SUN_DIR + view; h /= np.linalg.norm(h)
    col += (gb['spec'] * np.clip((n * h).sum(2), 0, 1) ** 12)[..., None] * sun_col * 1.2
    # rim: pixels whose neighbour toward the sun (screen up and a little right) is background or much deeper
    sun_px = cam.project(np.array([cam.eye + SUN_DIR * 1e5]))[0]
    h_, w_ = m.shape
    for r, k in ((2, 1.0), (5, 0.45)):
        dx, dy = 0.25 * r, -r
        M = np.float32([[1, 0, -dx], [0, 1, -dy]])
        dd = cv2.warpAffine(np.where(m, gb['depth'], 1e6).astype(np.float32), M, (w_, h_), borderValue=1e6)
        edge = (m & (dd - gb['depth'] > 8)).astype(np.float32)
        col += cv2.GaussianBlur(edge, (0, 0), 0.6)[..., None] * sun_col * (0.35 + alb) * k * 0.9
    if burst is not None:   # the spark burst lights the heroes' faces and upper bodies from between them
        bx, by, rad = burst
        yy, xx = np.mgrid[0:h_, 0:w_].astype(np.float32)
        dx, dy = bx - xx, by - yy
        dd = np.sqrt(dx * dx + dy * dy) + 1e-3
        L = np.stack([dx / dd, -dy / dd * 0.8, np.full_like(dd, -0.6)], 2)
        L /= np.linalg.norm(L, axis=2, keepdims=True)
        k = np.clip((n * L).sum(2), 0, 1) * np.exp(-(dd / rad) ** 2)
        col += alb * (k * m)[..., None] * hexc('#FFC27A') * 2.2
    col += gb['glow'] * 1.6
    fog = 1 - np.exp(-np.maximum(gb['depth'] - 120, 0) / fog_k)
    col = mix(col, fog_col, fog[..., None] * 0.92)
    return col, sun_px


def rise(sc):
    """Dark ground rising bottom-left under the menu, with a warm lip from the sun; nothing bottom-right."""
    W_, H_ = 2560, 1440
    n = fbm(1, W_, 61, octaves=6, base=4)[0]
    xs = np.arange(0, 980, 4)
    top = 935 + (xs / 980) ** 1.8 * 560 - 40 * n[xs] + 10 * np.sin(xs / 37)
    m = np.zeros((H_, W_), np.float32)
    cv2.fillPoly(m, [np.int32(np.concatenate([np.stack([xs, top], 1), [[980, H_], [0, H_]]]))], 1.0, cv2.LINE_AA)
    m = cv2.GaussianBlur(m, (0, 0), 1.2)
    lip = np.clip(m - cv2.warpAffine(m, np.float32([[1, 0, 0], [0, 1, 5]]), (W_, H_)), 0, 1)
    sc.img[:] = mix(sc.img, hexc('#0B080B'), m[..., None]) + hexc('#8A4A30') * lip[..., None] * 0.6


def main(out, scale=1.0, seed=7):
    W, H = int(2560 * scale), int(1440 * scale)
    cam = Camera(EYE, TARGET, FOV, W, H)
    hor = cam.project(np.array([[0, 0, 1e6]]))[0, 1] / scale
    sun = cam.project(np.array([np.array(EYE) + SUN_DIR * 1e6]))[0] / scale
    print('horizon', round(hor), 'sun', np.round(sun[:2]))

    # ---- background: sky, plain, dust (paint_title, authored at 2560x1440)
    v = dict(sun=(float(sun[0]), float(sun[1])), hor=int(hor), clash=1280, storm=0.8, rain=True, warm=1.0)
    sc = pt.Scene(v)
    sc.sky(); sc.ground(); sc.dust()
    bg = cv2.resize(sc.img, (W, H), interpolation=cv2.INTER_AREA)

    # ---- crowds, back to front groups
    rng = np.random.default_rng(seed)
    cache = {}
    items, tx = [], {}
    for g, (tid, cloth, x, z, yaw) in enumerate(crowd(rng)):
        key = (tid, cloth)
        if key not in cache:
            cache[key] = Troop(tid, 3, cloth)
            tx.update(troop_textures(cache[key]))
        t = cache[key]
        p = crowd_pose(rng)
        r = root_of(t, p, x, z, yaw)
        items += [(c, uv, t_, nn, g + 2) for c, uv, t_, nn, _ in t.quads(p, r)]
    print('crowd quads', len(items))
    gc = rasterize(items, cam, tx)
    haze = cv2.resize(sc.haze_arr(np.mgrid[0:1440, 0:2560][1].astype(np.float32)), (W, H))
    haze = haze * 0.8 + hexc('#3A2A34') * 0.2
    sun_col = hexc('#FFB070')
    cc, sun_px = lit(gc, cam, haze, sun_col, 260)
    img = np.where(gc['mask'][..., None], cc, bg)

    sc.img = cv2.resize(img, (2560, 1440), interpolation=cv2.INTER_CUBIC) if scale != 1 else img
    rise(sc)
    img = cv2.resize(sc.img, (W, H), interpolation=cv2.INTER_AREA) if scale != 1 else sc.img

    # ---- heroes
    P = json.load(open(HERE / 'pose.json'))
    k = Troop('gb_hollowking', 3, 'violet'); a = Troop('tc_admiral', 3, 'teal')
    kp = {b: tuple(v_) for b, v_ in P['king'].items()}; ap = {b: tuple(v_) for b, v_ in P['adm'].items()}
    rk = root_of(k, kp, 12, 0, 50); ra = root_of(a, ap, -12, 0, -50)
    hi = [(c, uv, t_, nn, 0) for c, uv, t_, nn, _ in k.quads(kp, rk)] + [(c, uv, t_, nn, 1) for c, uv, t_, nn, _ in a.quads(ap, ra)]
    gh = rasterize(hi, cam, {**troop_textures(k), **troop_textures(a)})
    # contact shadows under the heroes
    sh = np.zeros((H, W), np.float32)
    for x in (12, -12):
        p = cam.project(np.array([[x, 0, 0]]))[0]
        cv2.ellipse(sh, (int(p[0]), int(p[1])), (int(170 * scale), int(26 * scale)), 0, 0, 360, 1.0, -1)
    sh = cv2.GaussianBlur(sh, (0, 0), 14 * scale)
    img *= (1 - 0.6 * sh)[..., None]
    contact = cam.project(np.array([[0.0, -42.9, -16.1]]))[0]
    hc, _ = lit(gh, cam, haze, sun_col, 1e9, burst=(contact[0], contact[1], 520 * scale))
    img = np.where(gh['mask'][..., None], hc, img)

    # ---- atmosphere over everything: rain, sparks at the contact, embers, grade
    sc.img = cv2.resize(img, (2560, 1440), interpolation=cv2.INTER_CUBIC) if scale != 1 else img
    contact = contact / scale
    sc.rain()
    sc.sparks((float(contact[0]), float(contact[1])))
    sc.embers()
    sc.grade()
    cv2.imwrite(out, cv2.cvtColor(sc.to8(), cv2.COLOR_RGB2BGR))
    print('contact', np.round(contact[:2]))


if __name__ == '__main__':
    a = sys.argv
    main(a[1], float(a[a.index('--scale') + 1]) if '--scale' in a else 1.0, int(a[a.index('--seed') + 1]) if '--seed' in a else 7)
