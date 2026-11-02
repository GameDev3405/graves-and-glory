"""Map engine terrain: turns a design's shapes into a heightfield. Pure numpy, no Minecraft.

This is the one implementation shared by the browser editor preview and the block builder, so the
algorithm is pinned. Coordinates are block units: cell (x, z) has its centre at (x + 0.5, z + 0.5).
Every object is drawn, whether its state is accepted or proposed.
"""
import math
from dataclasses import dataclass, field

import numpy as np

NO_WATER = -32768
_BIG = 1 << 20


@dataclass
class Field:
    height: np.ndarray  # int16 [z, x], ground surface relative to baseY
    water: np.ndarray   # int16 [z, x], water surface relative to baseY, or NO_WATER
    road: np.ndarray    # bool  [z, x]
    zone: np.ndarray    # int16 [z, x], index into zones, or -1
    zones: list = field(default_factory=list)


def _polygon_mask(outline, x0, z0, nx, nz):
    """Even-odd fill at cell centres over the window x0..x0+nx, z0..z0+nz (cell indices)."""
    inside = np.zeros((nz, nx), dtype=bool)
    xc = x0 + 0.5 + np.arange(nx)
    zc = z0 + 0.5 + np.arange(nz)
    n = len(outline)
    for i in range(n):
        ax, az = float(outline[i][0]), float(outline[i][1])
        bx, bz = float(outline[(i + 1) % n][0]), float(outline[(i + 1) % n][1])
        if az == bz:
            continue
        lo, hi = (az, bz) if az < bz else (bz, az)
        # rows whose centre lies in the half-open span [lo, hi)
        r0 = max(0, int(math.ceil(lo - 0.5 - z0)))
        r1 = min(nz, int(math.ceil(hi - 0.5 - z0)))
        if r0 >= r1:
            continue
        t = (zc[r0:r1] - az) / (bz - az)
        xint = ax + t * (bx - ax)
        inside[r0:r1] ^= xc[None, :] < xint[:, None]
    return inside


def _outline_window(outline, width, depth, pad):
    """Cell window covering the outline's bounding box, clipped to the canvas grown by pad. None if empty."""
    xs = [p[0] for p in outline]
    zs = [p[1] for p in outline]
    x0 = max(int(math.floor(min(xs))), -pad)
    z0 = max(int(math.floor(min(zs))), -pad)
    x1 = min(int(math.ceil(max(xs))), width + pad)
    z1 = min(int(math.ceil(max(zs))), depth + pad)
    if x1 <= x0 or z1 <= z0:
        return None
    return x0, z0, x1 - x0, z1 - z0


def _chamfer_inside(mask, limit):
    """Chamfer (3-4) distance in units of 1/3 block from each inside cell to the nearest outside cell.

    Cells beyond `limit` iterations of propagation keep a large value; callers clip at the ramp anyway.
    The border of the array counts as outside, so callers pad the mask by one cell.
    """
    dist = np.where(mask, _BIG, 0).astype(np.int32)
    for _ in range(limit):
        new = dist.copy()
        for dz, dx, c in ((-1, 0, 3), (1, 0, 3), (0, -1, 3), (0, 1, 3), (-1, -1, 4), (-1, 1, 4), (1, -1, 4), (1, 1, 4)):
            src = dist[max(0, -dz):dist.shape[0] - max(0, dz), max(0, -dx):dist.shape[1] - max(0, dx)] + c
            dst = new[max(0, dz):dist.shape[0] - max(0, -dz), max(0, dx):dist.shape[1] - max(0, -dx)]
            np.minimum(dst, src, out=dst)
        # Propagating from the outside: min over neighbours + cost, only for inside cells
        new = np.where(mask, new, 0)
        if np.array_equal(new, dist):
            break
        dist = new
    return dist


def _smoothstep(t):
    return t * t * (3.0 - 2.0 * t)


def _apply_hill(h, o, width, depth):
    height = int(round(o.get('height', 0)))
    if height == 0:
        return
    ramp = 2 + int(round((1 - float(o.get('steepness', 0.5))) * 30))
    pad = ramp + 2
    win = _outline_window(o['outline'], width, depth, pad)
    if win is None:
        return
    x0, z0, nx, nz = win
    inner = _polygon_mask(o['outline'], x0, z0, nx, nz)
    if not inner.any():
        return
    mask = np.pad(inner, 1)  # a 1-cell outside border, so the window edge counts as outside
    dist = _chamfer_inside(mask, ramp + 2)[1:-1, 1:-1]
    t = np.clip(dist / (3.0 * ramp), 0.0, 1.0)
    val = np.rint(height * _smoothstep(t)).astype(np.int32)
    val = np.where(inner, val, 0)
    # Clip the window to the canvas.
    cx0, cz0 = max(x0, 0), max(z0, 0)
    cx1, cz1 = min(x0 + nx, width), min(z0 + nz, depth)
    if cx1 <= cx0 or cz1 <= cz0:
        return
    sub = val[cz0 - z0:cz1 - z0, cx0 - x0:cx1 - x0]
    cur = h[cz0:cz1, cx0:cx1]
    if height > 0:
        np.copyto(cur, np.maximum(cur, sub), where=sub > 0)
    else:
        np.copyto(cur, np.minimum(cur, sub), where=sub < 0)


def _path_dist(path, reach, width, depth):
    """Distance from each cell centre to the polyline, exact within `reach`, inf elsewhere."""
    dist = np.full((depth, width), np.inf)
    pts = [(float(p[0]), float(p[1])) for p in path]
    for (ax, az), (bx, bz) in zip(pts, pts[1:]):
        x0 = max(0, int(math.floor(min(ax, bx) - reach - 0.5)))
        x1 = min(width, int(math.ceil(max(ax, bx) + reach + 0.5)) + 1)
        z0 = max(0, int(math.floor(min(az, bz) - reach - 0.5)))
        z1 = min(depth, int(math.ceil(max(az, bz) + reach + 0.5)) + 1)
        if x1 <= x0 or z1 <= z0:
            continue
        px = (np.arange(x0, x1) + 0.5)[None, :]
        pz = (np.arange(z0, z1) + 0.5)[:, None]
        dx, dz = bx - ax, bz - az
        len2 = dx * dx + dz * dz
        if len2 == 0:
            t = 0.0
        else:
            t = np.clip(((px - ax) * dx + (pz - az) * dz) / len2, 0.0, 1.0)
        d = np.hypot(px - (ax + t * dx), pz - (az + t * dz))
        view = dist[z0:z1, x0:x1]
        np.minimum(view, d, out=view)
    return dist


def heightfield(d):
    width, depth = d['width'], d['depth']
    objs = d.get('objects', [])
    h = np.zeros((depth, width), dtype=np.int16)
    water = np.full((depth, width), NO_WATER, dtype=np.int16)
    road = np.zeros((depth, width), dtype=bool)
    zone = np.full((depth, width), -1, dtype=np.int16)
    zones = []

    for o in objs:
        if o.get('kind') == 'hill':
            _apply_hill(h, o, width, depth)

    for o in objs:
        if o.get('kind') != 'river':
            continue
        half = float(o['width']) / 2.0
        dist = _path_dist(o['path'], half + 3.0, width, depth)
        np.minimum(h, np.where(dist <= half, -int(round(o['depth'])), 32767).astype(np.int16), out=h)
        bank = (dist > half) & (dist <= half + 3.0)
        cap = np.floor(np.where(bank, dist - half, 0.0) + 0.5).astype(np.int16)  # round half up
        np.minimum(h, np.where(bank, cap, 32767).astype(np.int16), out=h)
        water[dist <= half] = 0

    for o in objs:
        if o.get('kind') == 'road':
            half = float(o['width']) / 2.0
            road |= _path_dist(o['path'], half, width, depth) <= half

    for o in objs:
        if o.get('kind') != 'zone':
            continue
        win = _outline_window(o['outline'], width, depth, 0)
        if win is None:
            continue
        x0, z0, nx, nz = win
        if o['palette'] not in zones:
            zones.append(o['palette'])
        zone[z0:z0 + nz, x0:x0 + nx][_polygon_mask(o['outline'], x0, z0, nx, nz)] = zones.index(o['palette'])

    return Field(h, water, road, zone, zones)
