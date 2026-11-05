"""A small software rasteriser for review images: orthographic views of a posed troop, textured as the game draws it
(nearest texel, alpha cut-out under 0.1, entity-style shading), supersampled 2x.

View(theta, pitch): theta 0 looks at the troop's front, 90 at its right side, -90 at its left; pitch looks down.
Screen right is r = (cos t, 0, -sin t), the view direction f = (sin t, 0, cos t), up is -y, all in model space.
"""
import math

import numpy as np

from .model import GROUND

FLAT = {'skin': (0.62, 0.6, 0.58), 'cloth': (0.7, 0.18, 0.16), 'weapon': (0.75, 0.77, 0.8), 'item_shield': (0.55, 0.38, 0.2)}
BG = (0.93, 0.93, 0.9)
GROUND_RGB = (0.8, 0.78, 0.72)
SS = 2


class View:
    def __init__(self, theta, pitch=0.0, name=''):
        self.theta, self.pitch, self.name = theta, pitch, name
        t, p = math.radians(theta), math.radians(pitch)
        f = np.array([math.sin(t), 0.0, math.cos(t)])
        u = np.array([0.0, -1.0, 0.0])
        self.r = np.array([math.cos(t), 0.0, -math.sin(t)])
        self.f = f * math.cos(p) + np.array([0.0, 1.0, 0.0]) * math.sin(p)
        self.u = u * math.cos(p) + f * math.sin(p)


def views_for(troop):
    """The review views: 3/4 from the weapon side's front, the weapon side's profile, the front."""
    s = 1.0 if troop.hand == 'right' else -1.0
    return [View(35 * s, 8, '3/4'), View(90 * s, 0, 'profile'), View(0, 4, 'front')]


def _shade(n):
    return 0.70 * n[..., 0] ** 2 + 0.86 * n[..., 2] ** 2 + np.where(n[..., 1] < 0, 1.0, 0.52) * n[..., 1] ** 2


def triangles(troop, W, hide=()):
    """(world corners T x 3 x 3, uv T x 3 x 2, texture key per triangle, item per triangle)."""
    P, UV, keys, items = [], [], [], []
    for q in troop.quads:
        if q.bone not in W or q.bone in hide:
            continue
        M = W[q.bone]
        c = (M[:3, :3] @ q.p.T).T + M[:3, 3]
        for a, b, d in ((0, 1, 2), (0, 2, 3)):
            P.append(c[[a, b, d]])
            UV.append(q.uv[[a, b, d]])
            keys.append(q.tex)
            items.append(q.item)
    return np.array(P), np.array(UV), keys, items


def render(troop, W, view, size=(220, 300), scale=6.0, center=(0.0, 12.0), ground=True, marks=None):
    """An RGB float image of the troop posed by W. center: the model point (x, y) at the image centre, in the view's
    horizontal axis and model y. marks: optional world points (N x 3) drawn as red dots (clipping)."""
    w, h = size[0] * SS, size[1] * SS
    s = scale * SS
    img = np.empty((h, w, 3), np.float32)
    img[:] = BG
    zb = np.full((h, w), np.inf, np.float32)
    cx = w / 2 - center[0] * s
    cy = h / 2 - center[1] * s

    def project(p):
        p = np.asarray(p, float)
        return np.stack([cx + p @ view.r * s, cy - p @ view.u * s, p @ view.f], -1)

    if ground:
        _ground(img, zb, project, troop)
    P, UV, keys, items = triangles(troop, W)
    if len(P):
        S = project(P.reshape(-1, 3)).reshape(-1, 3, 3)
        n = np.cross(P[:, 1] - P[:, 0], P[:, 2] - P[:, 0])
        nn = np.linalg.norm(n, axis=1)
        ok = nn > 1e-9
        n[ok] /= nn[ok][:, None]
        flip = (n @ view.f) > 0
        n[flip] *= -1
        light = _shade(n)
        for i in np.nonzero(ok)[0]:
            tex = troop.textures.get(keys[i])
            _tri(img, zb, S[i], UV[i], tex, FLAT.get(keys[i], (0.6, 0.6, 0.6)), light[i])
    if marks is not None and len(marks):
        m = project(np.asarray(marks, float))
        for x, y, z in m:
            xi, yi = int(x), int(y)
            if 0 <= xi < w and 0 <= yi < h:
                img[max(0, yi - 1):yi + 2, max(0, xi - 1):xi + 2] = (0.9, 0.05, 0.05)
    return img.reshape(h // SS, SS, w // SS, SS, 3).mean((1, 3))


def _ground(img, zb, project, troop):
    h, w, _ = img.shape
    g = np.array([[x, GROUND, z] for x in (-40, 40) for z in (-40, 40)], float)
    s = project(g)
    tri = [(0, 1, 3), (0, 3, 2)]
    for a, b, c in tri:
        _tri(img, zb, s[[a, b, c]], None, None, GROUND_RGB, 1.0, depth_bias=0.0)
    # the ground line through the feet, for the profile and front views
    y = int(round(project(np.array([[0, GROUND, 0]]))[0, 1]))
    if 0 <= y < h:
        img[y:y + 2, :] = np.minimum(img[y:y + 2, :], 0.55)


def _tri(img, zb, s, uv, tex, flat, light, depth_bias=0.0):
    h, w, _ = img.shape
    x0, x1 = max(int(math.floor(s[:, 0].min())), 0), min(int(math.ceil(s[:, 0].max())), w - 1)
    y0, y1 = max(int(math.floor(s[:, 1].min())), 0), min(int(math.ceil(s[:, 1].max())), h - 1)
    if x0 > x1 or y0 > y1:
        return
    a, b, c = s[0, :2], s[1, :2], s[2, :2]
    area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
    if abs(area) < 1e-9:
        return
    ys, xs = np.mgrid[y0:y1 + 1, x0:x1 + 1]
    px, py = xs + 0.5, ys + 0.5
    w0 = ((b[0] - px) * (c[1] - py) - (b[1] - py) * (c[0] - px)) / area
    w1 = ((c[0] - px) * (a[1] - py) - (c[1] - py) * (a[0] - px)) / area
    w2 = 1 - w0 - w1
    eps = -1e-6
    inside = (w0 >= eps) & (w1 >= eps) & (w2 >= eps)
    if not inside.any():
        return
    z = w0 * s[0, 2] + w1 * s[1, 2] + w2 * s[2, 2] + depth_bias
    sub = zb[y0:y1 + 1, x0:x1 + 1]
    m = inside & (z < sub)
    if not m.any():
        return
    if tex is not None and uv is not None:
        th, tw = tex.shape[:2]
        u = w0 * uv[0, 0] + w1 * uv[1, 0] + w2 * uv[2, 0]
        v = w0 * uv[0, 1] + w1 * uv[1, 1] + w2 * uv[2, 1]
        ti = np.clip((u * tw).astype(int), 0, tw - 1)
        tj = np.clip((v * th).astype(int), 0, th - 1)
        col = tex[tj, ti]
        m &= col[..., 3] >= 0.1
        rgb = col[..., :3] * light
    else:
        rgb = np.broadcast_to(np.array(flat, np.float32) * light, m.shape + (3,))
    if not m.any():
        return
    sub[m] = z[m]
    img[y0:y1 + 1, x0:x1 + 1][m] = rgb[m]


def to_pil(img):
    from PIL import Image
    return Image.fromarray(np.clip(img * 255 + 0.5, 0, 255).astype(np.uint8))


def bounds(troop, Ws, view):
    """Screen extents (in model px along r and -u) of the troop over a list of poses, for framing."""
    lo, hi = np.array([1e9, 1e9]), np.array([-1e9, -1e9])
    for W in Ws:
        P, _, _, _ = triangles(troop, W)
        if not len(P):
            continue
        p = P.reshape(-1, 3)
        xy = np.stack([p @ view.r, -(p @ view.u)], 1)
        lo, hi = np.minimum(lo, xy.min(0)), np.maximum(hi, xy.max(0))
    return lo, hi
