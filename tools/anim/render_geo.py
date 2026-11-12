#!/usr/bin/env python3
"""Offline numpy+PIL previewer for GeckoLib 4.8.4 geo.json models and animations.

Mirrors how GeckoLib 4.8.4 (format 1.12.0) bakes and renders on Minecraft 1.20.1.
Every convention below cites GeckoLib source (software/bernie/geckolib/...):

* Bone rotation  (-rx, -ry, +rz) rad, pivot (-px, py, pz)   BakedModelFactory.constructBone   ~L139-140
* Cube origin    (-(ox+sx)/16, oy/16, oz/16), pivot x flipped, rotation (-rx,-ry,+rz)
                                                             BakedModelFactory.constructCube ~L157-166
* Vertex layout / quad vertex order / mirror swaps           BakedModelFactory.VertexSet     ~L179-246
* Box UV regions (sizes floored), DOWN has -sz height        BakedModelFactory.buildQuad     ~L65-103
* UV u-swap when not mirrored, mirror flips normal x         GeoQuad.build                   ~L25-49
* Bone matrix: T(-pos.x,pos.y,pos.z) T(pivot) Rz Ry Rx S T(-pivot)   RenderUtils.prepMatrixForBone ~L94
* Cube matrix: T(pivot) Rz Ry Rx T(-pivot)                   GeoRenderer.renderCube ~L292 / RenderUtils.rotateMatrixAroundCube ~L58
* fixInvertedFlatCube                                        RenderUtils ~L211
* Keyframe sampling, past-end holds end value                AnimationController.getCurrentKeyFrameLocation ~L745
* Rotation degrees -> rad, x/y negated                       AnimationController.getAnimationPointAtTick ~L715
* Catmull-rom spline args / quirks                           BakedAnimationsAdapter.addSplineArgs ~L223,
                                                             EasingType.CatmullRomEasing ~L388-423
* Easings (note easeInQuint is pow(4) in 4.8.4)              EasingType ~L28-60, L134-383
* Loop types                                                 Animation.LoopType ~L39-97

World placement: GeoEntityRenderer rotates 180-yaw about Y, so for yaw 0 the world position of
a model point is (-gx, gy, -gz) (= Blockbench x, y, -z).  The model front (Blockbench -Z) faces
world +Z, which is also the direction --speed moves the root.

Dependencies: python3, numpy, Pillow.
"""
from __future__ import annotations

import argparse
import json
import math
import os
import sys
import time

import numpy as np
from PIL import Image, ImageDraw, ImageFont


class GeoError(Exception):
    pass


# ----------------------------------------------------------------------------------------------
# Small math helpers
# ----------------------------------------------------------------------------------------------

def _T(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def _Rx(a):
    c, s = math.cos(a), math.sin(a)
    m = np.eye(4)
    m[1, 1], m[1, 2], m[2, 1], m[2, 2] = c, -s, s, c
    return m


def _Ry(a):
    c, s = math.cos(a), math.sin(a)
    m = np.eye(4)
    m[0, 0], m[0, 2], m[2, 0], m[2, 2] = c, s, -s, c
    return m


def _Rz(a):
    c, s = math.cos(a), math.sin(a)
    m = np.eye(4)
    m[0, 0], m[0, 1], m[1, 0], m[1, 1] = c, -s, s, c
    return m


def _S(x, y, z):
    return np.diag([x, y, z, 1.0])


def _norm(v):
    v = np.asarray(v, dtype=np.float64)
    return v / np.linalg.norm(v)


# World placement for yaw 0: GeoEntityRenderer rotates by (180 - yaw) degrees about Y.
WORLD_FLIP = np.diag([-1.0, 1.0, -1.0, 1.0])


# ----------------------------------------------------------------------------------------------
# Model loading / baking (BakedModelFactory)
# ----------------------------------------------------------------------------------------------

DIRS = ("west", "east", "north", "south", "up", "down")  # quad build order in buildQuads
DIR_NORMAL = {
    "west": (-1.0, 0.0, 0.0), "east": (1.0, 0.0, 0.0),
    "north": (0.0, 0.0, -1.0), "south": (0.0, 0.0, 1.0),
    "up": (0.0, 1.0, 0.0), "down": (0.0, -1.0, 0.0),
}
UV_ROT = {0: 0, 90: 1, 180: 2, 270: 3}


def _rotate_uvs(rot, u, v, uw, vh):
    # FaceUV.Rotation.rotateUvs
    if rot == 0:
        return [u, v, uw, v, uw, vh, u, vh]
    if rot == 1:
        return [uw, v, uw, vh, u, vh, u, v]
    if rot == 2:
        return [uw, vh, u, vh, u, v, uw, v]
    return [u, vh, u, v, uw, v, uw, vh]


def _vertex_set(origin, vs, infl):
    ox, oy, oz = origin
    sx, sy, sz = vs
    x0, x1 = ox - infl, ox + sx + infl
    y0, y1 = oy - infl, oy + sy + infl
    z0, z1 = oz - infl, oz + sz + infl
    return {
        "BLB": (x0, y0, z0), "BRB": (x0, y0, z1), "TLB": (x0, y1, z0), "TRB": (x0, y1, z1),
        "TLF": (x1, y1, z0), "TRF": (x1, y1, z1), "BLF": (x1, y0, z0), "BRF": (x1, y0, z1),
    }


_QUADS = {
    "west": ("TRB", "TLB", "BLB", "BRB"),
    "east": ("TLF", "TRF", "BRF", "BLF"),
    "north": ("TLB", "TLF", "BLF", "BLB"),
    "south": ("TRF", "TRB", "BRB", "BRF"),
    "up": ("TRB", "TRF", "TLF", "TLB"),
    "down": ("BLB", "BLF", "BRF", "BRB"),
}


def _vertices_for_quad(vset, direction, box_uv, mirror):
    if direction == "west":
        key = "east" if mirror else "west"
    elif direction == "east":
        key = "west" if mirror else "east"
    elif direction == "up":
        key = "down" if (mirror and not box_uv) else "up"
    elif direction == "down":
        key = "up" if (mirror and not box_uv) else "down"
    else:
        key = direction
    return [vset[n] for n in _QUADS[key]]


class Face:
    __slots__ = ("name", "verts", "uv", "normal")


class Cube:
    def __init__(self):
        self.pivot = np.zeros(3)      # gecko-space pixels
        self.rot = np.zeros(3)        # radians
        self.size = np.zeros(3)       # pixels (original)
        self.faces = []
        self.corners = np.zeros((8, 3))  # unrotated corners, blocks, gecko space
        self.mirror = False


class Bone:
    def __init__(self, name):
        self.name = name
        self.parent = None
        self.children = []
        self.pivot = np.zeros(3)      # gecko-space pixels (-px, py, pz)
        self.rot = np.zeros(3)        # radians
        self.cubes = []
        self.inflate = None


class Model:
    def __init__(self):
        self.bones = {}
        self.order = []       # parents before children
        self.tex_w = 16.0
        self.tex_h = 16.0
        self.identifier = ""
        # flattened faces
        self.face_verts = None
        self.face_uv = None
        self.face_norm = None
        self.cube_list = []
        self.cube_bone = []
        self.cube_face_range = []

    def descendants(self, name):
        out = [name]
        for c in self.bones[name].children:
            out.extend(self.descendants(c))
        return out


def _num3(v, what, default=0.0):
    if v is None:
        return [default] * 3
    if len(v) != 3:
        raise GeoError("%s must have 3 numbers, got %r" % (what, v))
    out = []
    for x in v:
        if isinstance(x, str):
            raise GeoError("%s: Molang/string values are not supported (%r)" % (what, x))
        out.append(float(x))
    return out


def _build_cube(cj, bone, tw, th):
    cube = Cube()
    mirror = cj.get("mirror") is True
    infl_pix = cj.get("inflate")
    if infl_pix is not None:
        infl = float(infl_pix) / 16.0
    else:
        infl = (float(bone.inflate) / 16.0) if bone.inflate else 0.0
    size = _num3(cj.get("size"), "cube.size")
    origin = _num3(cj.get("origin"), "cube.origin")
    rot = _num3(cj.get("rotation"), "cube.rotation")
    piv = _num3(cj.get("pivot"), "cube.pivot")
    org = (-(origin[0] + size[0]) / 16.0, origin[1] / 16.0, origin[2] / 16.0)
    vs = (size[0] / 16.0, size[1] / 16.0, size[2] / 16.0)
    cube.pivot = np.array([-piv[0], piv[1], piv[2]])
    cube.rot = np.array([math.radians(-rot[0]), math.radians(-rot[1]), math.radians(rot[2])])
    cube.size = np.array(size)
    cube.mirror = mirror
    vset = _vertex_set(org, vs, infl)
    cube.corners = np.array([vset[k] for k in ("BLB", "BRB", "TLB", "TRB", "TLF", "TRF", "BLF", "BRF")])

    uvj = cj.get("uv")
    if uvj is None:
        uvj = [0, 0]
    box = isinstance(uvj, (list, tuple))
    fsz = [math.floor(s) for s in size]  # (sx, sy, sz)
    for d in DIRS:
        if box:
            u, v = float(uvj[0]), float(uvj[1])
            sx, sy, sz = fsz
            if d == "west":
                uvd = ((u + sz + sx, v + sz), (sz, sy))
            elif d == "east":
                uvd = ((u, v + sz), (sz, sy))
            elif d == "north":
                uvd = ((u + sz, v + sz), (sx, sy))
            elif d == "south":
                uvd = ((u + sz + sx + sz, v + sz), (sx, sy))
            elif d == "up":
                uvd = ((u + sz, v), (sx, sz))
            else:
                uvd = ((u + sz + sx, v + sz), (sx, -sz))
            rot_id = 0
            verts = _vertices_for_quad(vset, d, True, mirror)
        else:
            fj = uvj.get(d)
            if fj is None:
                continue
            uvd = (tuple(float(x) for x in fj.get("uv", [0, 0])),
                   tuple(float(x) for x in fj.get("uv_size", [0, 0])))
            rr = int(fj.get("uv_rotation", 0)) % 360
            if rr not in UV_ROT:
                raise GeoError("uv_rotation must be 0/90/180/270, got %r" % rr)
            rot_id = UV_ROT[rr]
            verts = _vertices_for_quad(vset, d, False, mirror)
        # GeoQuad.build
        (u, v), (usz, vsz) = uvd
        uw = (u + usz) / tw
        vh = (v + vsz) / th
        u = u / tw
        v = v / th
        nrm = np.array(DIR_NORMAL[d])
        if not mirror:
            u, uw = uw, u
        else:
            nrm = nrm * np.array([-1.0, 1.0, 1.0])
        uvs = _rotate_uvs(rot_id, u, v, uw, vh)
        f = Face()
        f.name = d
        f.verts = np.array(verts, dtype=np.float64)
        f.uv = np.array(uvs, dtype=np.float64).reshape(4, 2)
        f.normal = nrm
        cube.faces.append(f)
    return cube


def load_geo(src):
    """Load a geo.json from a path, JSON string-less dict, or file object."""
    if isinstance(src, (str, os.PathLike)):
        with open(src, "r", encoding="utf-8") as fh:
            data = json.load(fh)
    else:
        data = src
    geos = data.get("minecraft:geometry")
    if not geos:
        raise GeoError("no minecraft:geometry entry")
    g = geos[0]
    desc = g.get("description", {})
    model = Model()
    model.tex_w = float(desc.get("texture_width", 16))
    model.tex_h = float(desc.get("texture_height", 16))
    model.identifier = desc.get("identifier", "")
    for bj in g.get("bones", []):
        name = bj["name"]
        if name in model.bones:
            raise GeoError("duplicate bone name %r" % name)
        b = Bone(name)
        b.parent = bj.get("parent")
        piv = _num3(bj.get("pivot"), "bone.pivot")
        rot = _num3(bj.get("rotation"), "bone.rotation")
        b.pivot = np.array([-piv[0], piv[1], piv[2]])
        b.rot = np.array([math.radians(-rot[0]), math.radians(-rot[1]), math.radians(rot[2])])
        b.inflate = bj.get("inflate")
        model.bones[name] = b
        b._json = bj
    for b in model.bones.values():
        if b.parent is not None:
            if b.parent not in model.bones:
                raise GeoError("bone %r has missing parent %r" % (b.name, b.parent))
            model.bones[b.parent].children.append(b.name)
    # order parents first, detect cycles
    seen = set()

    def visit(n, stack):
        if n in stack:
            raise GeoError("bone hierarchy cycle at %r" % n)
        if n in seen:
            return
        stack = stack | {n}
        p = model.bones[n].parent
        if p is not None:
            visit(p, stack)
        seen.add(n)
        model.order.append(n)

    for n in list(model.bones):
        visit(n, frozenset())
    # cubes
    fv, fuv, fn = [], [], []
    for n in model.bones:
        b = model.bones[n]
        for cj in b._json.get("cubes", []):
            cube = _build_cube(cj, b, model.tex_w, model.tex_h)
            b.cubes.append(cube)
    for n in model.order:
        b = model.bones[n]
        for cube in b.cubes:
            start = len(fv)
            for f in cube.faces:
                fv.append(f.verts)
                fuv.append(f.uv)
                fn.append(f.normal)
            model.cube_list.append(cube)
            model.cube_bone.append(n)
            model.cube_face_range.append((start, len(fv)))
    model.face_verts = np.array(fv).reshape(-1, 4, 3)
    model.face_uv = np.array(fuv).reshape(-1, 4, 2)
    model.face_norm = np.array(fn).reshape(-1, 3)
    return model


# ----------------------------------------------------------------------------------------------
# Easing (EasingType.java)
# ----------------------------------------------------------------------------------------------

def _ease_in(f):
    return f


def _ease_out(f):
    return lambda t: 1.0 - f(1.0 - t)


def _ease_in_out(f):
    def g(t):
        if t < 0.5:
            return f(t * 2.0) / 2.0
        return 1.0 - f((1.0 - t) * 2.0) / 2.0
    return g


def _sine(n):
    return 1.0 - math.cos(n * math.pi / 2.0)


def _circle(n):
    return 1.0 - math.sqrt(max(0.0, 1.0 - n * n))


def _expo(n):
    return 2.0 ** (10.0 * (n - 1.0))


def _back(n):
    n2 = 1.70158 if n is None else n * 1.70158
    return lambda t: t * t * ((n2 + 1.0) * t - n2)


def _elastic(n):
    n2 = 1.0 if n is None else n
    return lambda t: 1.0 - math.cos(t * math.pi / 2.0) ** 3 * math.cos(t * n2 * math.pi)


def _bounce(n):
    n2 = 0.5 if n is None else n

    def f(t):
        one = 121.0 / 16.0 * t * t
        two = 121.0 / 4.0 * n2 * (t - 6.0 / 11.0) ** 2 + 1 - n2
        three = 121.0 * n2 * n2 * (t - 9.0 / 11.0) ** 2 + 1 - n2 * n2
        four = 484.0 * n2 ** 3 * (t - 10.5 / 11.0) ** 2 + 1 - n2 ** 3
        return min(min(one, two), min(three, four))
    return f


def _step(n):
    n2 = 2 if n is None else n
    if n2 < 2:
        raise GeoError("easing step: steps must be >= 2, got %r" % n2)
    steps = int(n2)

    def f(t):
        if t < 0:
            return 0.0
        length = 1.0 / steps
        top = (steps - 1) * length
        if t > top:
            return top
        left, right = 0, steps - 1
        while right - left != 1:
            test = left + (right - left) // 2
            if t >= test * length:
                left = test
            else:
                right = test
        return left * length
    return f


def _catmull_n(n):
    # EasingType.catmullRom(double) - the transformer used only when <2 easingArgs
    return 0.5 * (2.0 * (n + 1.0) + 2.0
                  + (2.0 * n - 5.0 * (n + 1.0) + 4.0 * (n + 2.0) - (n + 3.0))
                  + (3.0 * (n + 1.0) - n - 3.0 * (n + 2.0) + (n + 3.0)))


def _linear(n):
    return n


_Q = lambda n: n * n
_C = lambda n: n * n * n

# name -> transformer builder(value) (EasingType.java L28-60). easeInQuint really is pow(4) in 4.8.4.
EASINGS = {
    "linear": lambda v: _ease_in(_linear),
    "none": lambda v: _ease_in(_linear),
    "step": lambda v: _ease_in(_step(v)),
    "easeinsine": lambda v: _ease_in(_sine),
    "easeoutsine": lambda v: _ease_out(_sine),
    "easeinoutsine": lambda v: _ease_in_out(_sine),
    "easeinquad": lambda v: _ease_in(_Q),
    "easeoutquad": lambda v: _ease_out(_Q),
    "easeinoutquad": lambda v: _ease_in_out(_Q),
    "easeincubic": lambda v: _ease_in(_C),
    "easeoutcubic": lambda v: _ease_out(_C),
    "easeinoutcubic": lambda v: _ease_in_out(_C),
    "easeinquart": lambda v: _ease_in(lambda t: t ** 4),
    "easeoutquart": lambda v: _ease_out(lambda t: t ** 4),
    "easeinoutquart": lambda v: _ease_in_out(lambda t: t ** 4),
    "easeinquint": lambda v: _ease_in(lambda t: t ** 4),   # GeckoLib bug: pow(4)
    "easeoutquint": lambda v: _ease_out(lambda t: t ** 5),
    "easeinoutquint": lambda v: _ease_in_out(lambda t: t ** 5),
    "easeinexpo": lambda v: _ease_in(_expo),
    "easeoutexpo": lambda v: _ease_out(_expo),
    "easeinoutexpo": lambda v: _ease_in_out(_expo),
    "easeincirc": lambda v: _ease_in(_circle),
    "easeoutcirc": lambda v: _ease_out(_circle),
    "easeinoutcirc": lambda v: _ease_in_out(_circle),
    "easeinback": lambda v: _ease_in(_back(v)),
    "easeoutback": lambda v: _ease_out(_back(v)),
    "easeinoutback": lambda v: _ease_in_out(_back(v)),
    "easeinelastic": lambda v: _ease_in(_elastic(v)),
    "easeoutelastic": lambda v: _ease_out(_elastic(v)),
    "easeinoutelastic": lambda v: _ease_in_out(_elastic(v)),
    "easeinbounce": lambda v: _ease_in(_bounce(v)),
    "easeoutbounce": lambda v: _ease_out(_bounce(v)),
    "easeinoutbounce": lambda v: _ease_in_out(_bounce(v)),
    "catmullrom": lambda v: _ease_in_out(_catmull_n),
}


def spline_point(delta, p0, p1, p2, p3):
    """CatmullRomEasing.getPointOnSpline (EasingType.java ~L400)."""
    return 0.5 * (2.0 * p1 + (p2 - p0) * delta
                  + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * delta * delta
                  + (3.0 * p1 - p0 - 3.0 * p2 + p3) * delta * delta * delta)


class Keyframe:
    """One segment: `length` ticks, from `start` to `end` (vec3 each), with easing."""

    def __init__(self, length, start, end, easing="linear", args=None):
        self.length = float(length)
        self.start = np.asarray(start, dtype=np.float64)
        self.end = np.asarray(end, dtype=np.float64)
        self.easing = easing
        self.args = args or []

    def apply(self, cur):
        """EasingType.apply for this keyframe at local tick `cur`."""
        if cur >= self.length:
            return self.end.copy()
        t = cur / self.length
        if self.easing == "catmullrom":
            if len(self.args) < 2:
                # quirk: Interpolations.lerp(transformed, start, end) has swapped arguments
                tr = _ease_in_out(_catmull_n)(t)
                return tr + (self.start - tr) * self.end
            p0, p3 = self.args[0], self.args[1]
            return spline_point(t, p0, self.start, self.end, p3)
        value = None
        if self.args:
            a0 = self.args[0]
            value = float(a0 if not hasattr(a0, "__len__") else a0[0])
        f = EASINGS.get(self.easing, EASINGS["linear"])(value)
        return self.start + (self.end - self.start) * f(t)


# ----------------------------------------------------------------------------------------------
# Animation file parsing (BakedAnimationsAdapter)
# ----------------------------------------------------------------------------------------------

def _vec3(v, what):
    if isinstance(v, (int, float)):
        return [float(v)] * 3
    if not isinstance(v, (list, tuple)) or len(v) != 3:
        raise GeoError("%s: expected 3 numbers, got %r" % (what, v))
    out = []
    for x in v:
        if isinstance(x, (int, float)):
            out.append(float(x))
        elif isinstance(x, str):
            try:
                out.append(float(x))
            except ValueError:
                raise GeoError("%s: Molang expressions are not supported (%r)" % (what, x))
        else:
            raise GeoError("%s: bad value %r" % (what, x))
    return out


def _entry_from_obj(ts, obj, what):
    easing = "linear"
    args = []
    if "easing" in obj:
        easing = str(obj["easing"]).lower()
    if "easingArgs" in obj:
        for a in obj["easingArgs"]:
            if isinstance(a, str):
                try:
                    a = float(a)
                except ValueError:
                    raise GeoError("%s: Molang easingArgs unsupported (%r)" % (what, a))
            args.append(float(a))
    return easing, args


def _unwrap_vector(x):
    """pre/post may be {"vector": [...]} objects."""
    if isinstance(x, dict) and "vector" in x:
        return x["vector"]
    return x


def _get_keyframes(spec, what):
    """BakedAnimationsAdapter.getKeyframes -> list of (ts_seconds, vec3, easing, args)."""
    if isinstance(spec, (int, float)):
        return [(0.0, [float(spec)] * 3, "linear", [])]
    if isinstance(spec, list):
        return [(0.0, _vec3(spec, what), "linear", [])]
    if not isinstance(spec, dict):
        raise GeoError("%s: unsupported keyframe spec %r" % (what, spec))
    if "vector" in spec:
        easing, args = _entry_from_obj(0.0, spec, what)
        return [(0.0, _vec3(spec["vector"], what), easing, args)]
    out = []
    for k, v in spec.items():
        try:
            ts = float(k)
        except ValueError:
            raise GeoError("%s: bad keyframe timestamp %r" % (what, k))
        if isinstance(v, (list, int, float)):
            out.append((ts, _vec3(v, what), "linear", []))
        elif isinstance(v, dict):
            if "vector" in v:
                easing, args = _entry_from_obj(ts, v, what)
                out.append((ts, _vec3(v["vector"], what), easing, args))
            else:
                if "pre" in v:
                    pts = ts - 0.001 if ts != 0 else 0.0
                    out.append((pts, _vec3(_unwrap_vector(v["pre"]), what), "linear", []))
                if "post" in v:
                    easing = str(v.get("lerp_mode", "linear")).lower()
                    out.append((ts, _vec3(_unwrap_vector(v["post"]), what), easing, []))
        else:
            raise GeoError("%s: bad keyframe value %r" % (what, v))
    # GeckoLib keeps JSON insertion order; do not sort, but warn when it is not ascending.
    if any(out[i][0] > out[i + 1][0] for i in range(len(out) - 1)):
        print("warning: %s: keyframe timestamps are not ascending (GeckoLib keeps file order)" % what,
              file=sys.stderr)
    return out


def _build_stack(entries, is_rot):
    frames = []
    prev_t = 0.0
    prev_val = None
    for i, (ts, val, easing, args) in enumerate(entries):
        v = np.array(val, dtype=np.float64)
        if is_rot:
            v = np.array([math.radians(-v[0]), math.radians(-v[1]), math.radians(v[2])])
        length = (ts - prev_t) * 20.0
        start = v if prev_val is None else prev_val
        frames.append(Keyframe(length, start, v, easing, list(args)))
        prev_t = ts
        prev_val = v
    # addSplineArgs
    if len(frames) == 1 and frames[0].easing != "linear":
        f = frames[0]
        frames[0] = Keyframe(f.length, f.start, f.end)
        return frames
    for i, f in enumerate(frames):
        if f.easing == "catmullrom":
            a0 = f.start if i == 0 else frames[i - 1].end
            a1 = f.end if i + 1 >= len(frames) else frames[i + 1].end
            frames[i] = Keyframe(f.length, f.start, f.end, f.easing, [a0, a1])
    return frames


class Clip:
    CHANNELS = ("rotation", "position", "scale")

    def __init__(self, name, length_ticks, loop, bones):
        self.name = name
        self.length = float(length_ticks)
        self.loop = loop            # 'loop' | 'once' | 'hold'
        self.bones = bones          # bone -> channel -> [Keyframe]

    def touched(self):
        return {b for b, ch in self.bones.items() if any(ch.get(c) for c in self.CHANNELS)}

    def sample(self, bone, channel, tick):
        """Raw keyframe sampling (getCurrentKeyFrameLocation + EasingType.apply)."""
        frames = self.bones.get(bone, {}).get(channel)
        if not frames:
            return None
        total = 0.0
        for fr in frames:
            total += fr.length
            if total > tick:
                return fr.apply(tick - (total - fr.length))
        return frames[-1].apply(tick)

    def effective_tick(self, t):
        """Clip-local tick for global tick t, or None when the clip is no longer playing."""
        if self.loop == "loop":
            if self.length <= 0:
                return 0.0
            return math.fmod(t, self.length)
        if self.loop == "hold":
            return t
        if t >= self.length:
            return None      # PLAY_ONCE -> controller STOPPED -> bones return to model pose
        return t


def load_clip(src, name):
    if isinstance(src, (str, os.PathLike)):
        with open(src, "r", encoding="utf-8") as fh:
            data = json.load(fh)
    else:
        data = src
    anims = data.get("animations", {})
    if name not in anims:
        raise GeoError("clip %r not found; available: %s" % (name, ", ".join(sorted(anims)) or "(none)"))
    a = anims[name]
    loop_j = a.get("loop", False)
    if loop_j is True:
        loop = "loop"
    elif isinstance(loop_j, str):
        s = loop_j.lower()
        loop = "loop" if s in ("true", "loop") else "hold" if s == "hold_on_last_frame" else "once"
    else:
        loop = "once"
    bones = {}
    longest = 0.0
    for bname, bj in a.get("bones", {}).items():
        ch = {}
        for cname in Clip.CHANNELS:
            if cname in bj:
                what = "%s/%s/%s" % (name, bname, cname)
                stack = _build_stack(_get_keyframes(bj[cname], what), cname == "rotation")
                ch[cname] = stack
                longest = max(longest, sum(f.length for f in stack))
        bones[bname] = ch
    if "animation_length" in a:
        length = float(a["animation_length"]) * 20.0
    else:
        length = longest
    return Clip(name, length, loop, bones)


# ----------------------------------------------------------------------------------------------
# Textures
# ----------------------------------------------------------------------------------------------

ALPHA_CUTOFF = 0.1


def compose_textures(layers):
    """Composite layers in order (cutout replace, alpha >= 0.1). Different sizes are sampled by
    normalised UV, i.e. smaller layers are nearest-upscaled to the largest layer's size."""
    arrs = []
    for L in layers or []:
        if isinstance(L, (str, os.PathLike)):
            L = Image.open(L)
        if isinstance(L, Image.Image):
            L = np.array(L.convert("RGBA"))
        arrs.append(np.asarray(L, dtype=np.uint8))
    if not arrs:
        return np.full((1, 1, 4), (170, 170, 170, 255), dtype=np.uint8)
    W = max(a.shape[1] for a in arrs)
    H = max(a.shape[0] for a in arrs)
    out = np.zeros((H, W, 4), dtype=np.uint8)
    thr = int(math.ceil(ALPHA_CUTOFF * 255.0))   # 0.1*255=25.5 -> alpha>=26 survives
    for a in arrs:
        h, w = a.shape[:2]
        ys = np.minimum(((np.arange(H) + 0.5) * h / H).astype(int), h - 1)
        xs = np.minimum(((np.arange(W) + 0.5) * w / W).astype(int), w - 1)
        up = a[ys][:, xs]
        vis = up[..., 3] >= thr
        out[vis, :3] = up[vis, :3]
        out[vis, 3] = 255
    return out


# ----------------------------------------------------------------------------------------------
# Scene evaluation (poses -> world geometry)
# ----------------------------------------------------------------------------------------------

L0 = _norm((0.2, 1.0, -0.7))
L1 = _norm((-0.2, 1.0, 0.7))


def shade_of(normals):
    n = np.asarray(normals, dtype=np.float64)
    acc = np.maximum(0.0, n @ L0) + np.maximum(0.0, n @ L1)
    return np.minimum(1.0, acc * 0.6 + 0.4)


class Frame:
    pass


def additive_under(name, names):
    """The stance clips an ADDITIVE clip is shown over, or None for a clip that is a pose of its own. The game adds
    every hit clip (HitController.add) and the stun shake-off (StanceController, stun_out_upper) onto whatever the
    other layers wrote; a troop taking a blow in a fight stands in its ready stance. Played over the rest pose
    instead, a hit shows arms and feet the game never draws (false floor, elbow and knee findings) and hides real
    clipping against the stance (wm_hound, 2026-11-12: chest 2.6 px into the skullcap)."""
    if name.endswith("_legs") or not name.startswith(("hit_", "stun_out")):
        return None
    return [c for c in ("stance_ready_upper", "stance_ready_legs") if c in names]


def additive_split(name, base, under):
    """(held, added) clip names for an additive clip `name` shown with its base clips over the stance clips `under`.
    A base that is additive itself (a hit's legs) is added with the clip. A base that is a pose of its own (stun_legs
    under stun_out_upper: the game still holds the stun's legs when the shake-off starts, StanceController's handover)
    takes the place of the stance clip of the same half; adding it onto stance_ready_legs bent the knees twice
    (ic_hearthguard stun_out_upper, a false 1.87 px floor row, 2026-11-12)."""
    added = [b for b in base if b.startswith(("hit_", "stun_out"))] + [name]
    poses = [b for b in base if b not in added]
    half = lambda c: "legs" if c.endswith("_legs") else "upper"
    held = [u for u in under if u not in base and half(u) not in {half(q) for q in poses}] + poses
    return held, added


class Scene:
    DRAW_FRAMES = ("rest", "pull_0", "pull_1", "pull_2")

    def __init__(self, model, textures=None, clips=None, speed=0.0, look=None, additive=None):
        self.model = model
        # additive: clips ADDED onto the pose the other clips make, as HitController and the stun shake-off do in game
        # (rotation and position offsets; they may key bones the other clips key). They do not change the root motion.
        self.additive = list(additive or [])
        # look = (troop_rig rules, armour/weapon tier, face): draw only what the game shows (GeoTroopRenderer.preRender),
        # and of a bow's draw frames only the one the clips key on the "draw" bone's x position. None draws every bone.
        self.look = look
        self.tex = compose_textures(textures)
        self.clips = list(clips or [])
        self.speed = float(speed)
        owner = {}
        for c in self.clips:
            for b in sorted(c.touched()):
                if b in owner:
                    raise GeoError("bone %r is animated by both clip %r and clip %r; "
                                   "clips played together must animate disjoint bones"
                                   % (b, owner[b], c.name))
                owner[b] = c.name

    def root_offset(self, tick):
        return np.array([0.0, 0.0, self.speed * tick])

    def clip_label(self):
        return "+".join([c.name for c in self.clips] + ["(+)" + c.name for c in self.additive]) or "static"

    def _pose(self, tick):
        pose = {}
        for c in self.clips:
            et = c.effective_tick(tick)
            if et is None:
                continue
            for b in c.touched():
                if b not in self.model.bones:
                    continue
                d = {}
                for ch in Clip.CHANNELS:
                    v = c.sample(b, ch, et)
                    if v is not None:
                        d[ch] = v
                pose[b] = d
        for c in self.additive:
            et = c.effective_tick(tick)
            if et is None:
                continue
            for b in c.touched():
                if b not in self.model.bones:
                    continue
                d = pose.setdefault(b, {})
                for ch in ("rotation", "position"):
                    v = c.sample(b, ch, et)
                    if v is not None:
                        d[ch] = np.asarray(d[ch], dtype=float) + np.asarray(v, dtype=float) if ch in d else v
        return pose

    def evaluate(self, tick):
        m = self.model
        pose = self._pose(tick)
        root_off = self.root_offset(tick)
        base = _T(*root_off) @ WORLD_FLIP
        bone_M = {}
        for n in m.order:
            b = m.bones[n]
            p = pose.get(n, {})
            rot = b.rot + p["rotation"] if "rotation" in p else b.rot
            pos = p.get("position", (0.0, 0.0, 0.0))
            sc = p.get("scale", (1.0, 1.0, 1.0))
            parent = bone_M[b.parent] if b.parent is not None else base
            pv = b.pivot / 16.0
            M = parent @ _T(-pos[0] / 16.0, pos[1] / 16.0, pos[2] / 16.0) @ _T(*pv)
            M = M @ _Rz(rot[2]) @ _Ry(rot[1]) @ _Rx(rot[0]) @ _S(*sc) @ _T(*(-pv))
            bone_M[n] = M
        hidden = self.hidden_bones(pose)
        nF = len(m.face_verts)
        fv = np.zeros((nF, 4, 3))
        fn = np.zeros((nF, 3))
        corners = []
        cube_M = []
        for ci, cube in enumerate(m.cube_list):
            M = bone_M[m.cube_bone[ci]]
            if m.cube_bone[ci] in hidden:       # zero-area faces: nothing is drawn; the pivot keeps the bounds honest
                cube_M.append(M)
                corners.append(np.tile((M @ np.append(m.bones[m.cube_bone[ci]].pivot / 16.0, 1.0))[:3], (8, 1)))
                continue
            cp = cube.pivot / 16.0
            Mc = M @ _T(*cp) @ _Rz(cube.rot[2]) @ _Ry(cube.rot[1]) @ _Rx(cube.rot[0]) @ _T(*(-cp))
            cube_M.append(Mc)
            a, b = m.cube_face_range[ci]
            R3 = Mc[:3, :3]
            if b > a:
                fv[a:b] = m.face_verts[a:b] @ R3.T + Mc[:3, 3]
                try:
                    NM = np.linalg.inv(R3).T
                except np.linalg.LinAlgError:
                    NM = R3
                nn = m.face_norm[a:b] @ NM.T
                ln = np.linalg.norm(nn, axis=1, keepdims=True)
                nn = nn / np.where(ln < 1e-12, 1.0, ln)
                s = cube.size
                for i in range(len(nn)):   # fixInvertedFlatCube
                    if nn[i, 0] < 0 and (s[1] == 0 or s[2] == 0):
                        nn[i, 0] = -nn[i, 0]
                    if nn[i, 1] < 0 and (s[0] == 0 or s[2] == 0):
                        nn[i, 1] = -nn[i, 1]
                    if nn[i, 2] < 0 and (s[0] == 0 or s[1] == 0):
                        nn[i, 2] = -nn[i, 2]
                fn[a:b] = nn
            corners.append(cube.corners @ R3.T + Mc[:3, 3])
        fr = Frame()
        fr.tick = tick
        fr.root_off = root_off
        fr.bone_M = bone_M
        fr.cube_M = cube_M
        fr.face_verts = fv
        fr.face_norm = fn
        fr.shade = shade_of(fn) if nF else np.zeros(0)
        fr.corners = np.array(corners) if corners else np.zeros((0, 8, 3))
        return fr

    def hidden_bones(self, pose):
        if not self.look:
            return set()
        rules, tier, face = self.look
        spec = rules.get("bones", {})
        frame = 0
        d = pose.get("draw", {}).get("position")
        if d is not None:
            frame = int(max(0, min(len(self.DRAW_FRAMES) - 1, round(abs(float(d[0]))))))
        hidden = set()
        for n in self.model.order:
            r = spec.get(n, {})
            show = tier in r.get("stages", [tier]) and face in r.get("variants", [face])
            if r.get("tag") == "draw":
                for i, f in enumerate(self.DRAW_FRAMES):
                    if n.endswith("_" + f) and i != frame:
                        show = False
            par = self.model.bones[n].parent
            if not show or (par is not None and par in hidden):
                hidden.add(n)
        return hidden

    def aabb(self, fr):
        if len(fr.corners) == 0:
            return np.zeros(3), np.zeros(3)
        c = fr.corners.reshape(-1, 3)
        return c.min(axis=0), c.max(axis=0)

    def bone_pivot_world(self, fr, name):
        b = self.model.bones[name]
        p = fr.bone_M[name] @ np.append(b.pivot / 16.0, 1.0)
        return p[:3]


# ----------------------------------------------------------------------------------------------
# Cameras
# ----------------------------------------------------------------------------------------------

def _view_defs():
    e = math.radians(12.0)
    se, ce = math.sin(e), math.cos(e)
    up = (0.0, 1.0, 0.0)
    return {
        # name: (direction from target to the camera, up hint)
        "front": ((0.0, se, ce), up),
        "back": ((0.0, se, -ce), up),
        "side": ((-ce, se, 0.0), up),
        "three_quarter": (tuple(_norm((-1.0, 0.55, 1.0))), up),
        "top": ((0.0, 1.0, 0.0), (0.0, 0.0, 1.0)),
        "low": (tuple(_norm((-0.6, 0.07, 0.8))), up),
        # exact axis-aligned views (used by tests)
        "axis_pz": ((0.0, 0.0, 1.0), up),
        "axis_nz": ((0.0, 0.0, -1.0), up),
        "axis_px": ((1.0, 0.0, 0.0), up),
        "axis_nx": ((-1.0, 0.0, 0.0), up),
        "axis_py": ((0.0, 1.0, 0.0), (0.0, 0.0, 1.0)),
        "axis_ny": ((0.0, -1.0, 0.0), (0.0, 0.0, 1.0)),
    }


VIEWS = _view_defs()
PUBLIC_VIEWS = ("front", "side", "back", "three_quarter", "top", "low")


class Camera:
    def __init__(self, name, eye_dir, up_hint, mode, scale, rel_target, W, H, cx, cy, focal=None, dist=16.0):
        self.name = name
        self.mode = mode            # 'ortho' | 'persp'
        self.scale = float(scale)   # ortho: px per block
        self.focal = focal          # persp: focal length in px
        self.dist = dist
        self.F = -_norm(eye_dir)
        self.R = _norm(np.cross(self.F, np.asarray(up_hint, dtype=np.float64)))
        self.U = np.cross(self.R, self.F)
        self.rel_target = np.asarray(rel_target, dtype=np.float64)
        self.W, self.H, self.cx, self.cy = int(W), int(H), float(cx), float(cy)
        self.target = self.rel_target.copy()
        self.eye = self.target.copy()

    def bind(self, root_off):
        c = Camera.__new__(Camera)
        c.__dict__.update(self.__dict__)
        c.target = self.rel_target + np.asarray(root_off)
        c.eye = c.target - self.F * self.dist if self.mode == "persp" else c.target.copy()
        return c

    def to_cam(self, pts):
        rel = np.asarray(pts, dtype=np.float64) - self.eye
        return np.stack([rel @ self.R, rel @ self.U, rel @ self.F], axis=-1)

    def project(self, pts, ss=1):
        pc = self.to_cam(pts)
        s = self.px_scale(ss)
        if self.mode == "ortho":
            return self.cx * ss + pc[..., 0] * s, self.cy * ss - pc[..., 1] * s, pc[..., 2]
        z = np.where(pc[..., 2] > 1e-6, pc[..., 2], 1e-6)
        return self.cx * ss + s * pc[..., 0] / z, self.cy * ss - s * pc[..., 1] / z, pc[..., 2]

    def px_scale(self, ss=1):
        return (self.scale if self.mode == "ortho" else self.focal) * ss


def make_cameras(view_names, rel_bounds, distance="near", scale=None, margin=14):
    """rel_bounds: (min, max) of the model AABB relative to the root over all rendered ticks
    (already extended to include the ground marker)."""
    lo, hi = (np.asarray(b, dtype=np.float64) for b in rel_bounds)
    centre = (lo + hi) / 2.0
    corners = np.array([[x, y, z] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])])
    cams = []
    for name in view_names:
        if name not in VIEWS:
            raise GeoError("unknown view %r (choose from %s)" % (name, ", ".join(PUBLIC_VIEWS)))
        eye_dir, up_hint = VIEWS[name]
        if distance == "near":
            sc = 160.0 if scale is None else float(scale)
            probe = Camera(name, eye_dir, up_hint, "ortho", sc, centre, 1, 1, 0, 0)
            pc = probe.bind((0, 0, 0)).to_cam(corners)
            ext_x = pc[:, 0].max() - pc[:, 0].min()
            ext_y = pc[:, 1].max() - pc[:, 1].min()
            W = max(64, int(math.ceil(ext_x * sc + 2 * margin)))
            H = max(64, int(math.ceil(ext_y * sc + 2 * margin)))
            # centre of the projected box
            mx = (pc[:, 0].max() + pc[:, 0].min()) / 2.0
            my = (pc[:, 1].max() + pc[:, 1].min()) / 2.0
            cams.append(Camera(name, eye_dir, up_hint, "ortho", sc, centre, W, H,
                               W / 2.0 - mx * sc, H / 2.0 + my * sc))
        else:
            win_h = 1080.0 if scale is None else float(scale)
            f = (win_h / 2.0) / math.tan(math.radians(35.0))    # 70 degree vertical FOV
            probe = Camera(name, eye_dir, up_hint, "persp", 0.0, centre, 1, 1, 0, 0, focal=f, dist=16.0)
            pb = probe.bind((0, 0, 0))
            px, py, pz = pb.project(corners)
            W = max(96, int(math.ceil(px.max() - px.min() + 2 * margin)))
            H = max(96, int(math.ceil(py.max() - py.min() + 2 * margin)))
            cx = W / 2.0 - (px.max() + px.min()) / 2.0
            cy = H / 2.0 - (py.max() + py.min()) / 2.0
            cams.append(Camera(name, eye_dir, up_hint, "persp", 0.0, centre, W, H, cx, cy, focal=f, dist=16.0))
    return cams


# ----------------------------------------------------------------------------------------------
# Rendering
# ----------------------------------------------------------------------------------------------

BG_SKY = np.array([226.0, 226.0, 226.0])
BG_GROUND = np.array([212.0, 212.0, 212.0])
GRID_FINE = np.array([192.0, 192.0, 192.0])
GRID_BLOCK = np.array([140.0, 140.0, 140.0])
ALPHA_MIN = int(math.ceil(ALPHA_CUTOFF * 255.0))


def render_background(cam, ss):
    Ws, Hs = cam.W * ss, cam.H * ss
    sc = cam.px_scale(ss)
    xs = (np.arange(Ws) + 0.5 - cam.cx * ss) / sc
    ys = (cam.cy * ss - (np.arange(Hs) + 0.5)) / sc
    X = xs[None, :]
    Y = ys[:, None]
    img = np.empty((Hs, Ws, 3), dtype=np.float32)
    img[:] = BG_SKY
    if cam.mode == "ortho":
        oy = cam.target[1] + Y * cam.U[1] + X * cam.R[1]
        dy = cam.F[1]
        if abs(dy) < 1e-9:
            return img
        lam = -oy / dy
        valid = np.ones((Hs, Ws), dtype=bool)
        hx = cam.target[0] + X * cam.R[0] + Y * cam.U[0] + lam * cam.F[0]
        hz = cam.target[2] + X * cam.R[2] + Y * cam.U[2] + lam * cam.F[2]
    else:
        dvec_y = X * cam.R[1] + Y * cam.U[1] + cam.F[1]
        with np.errstate(all="ignore"):
            lam = -cam.eye[1] / dvec_y
        valid = (lam > 0) & np.isfinite(lam)
        lam = np.where(valid, lam, 0.0)
        hx = cam.eye[0] + lam * (X * cam.R[0] + Y * cam.U[0] + cam.F[0])
        hz = cam.eye[2] + lam * (X * cam.R[2] + Y * cam.U[2] + cam.F[2])
    hx = np.broadcast_to(hx, (Hs, Ws))
    hz = np.broadcast_to(hz, (Hs, Ws))
    fpx = np.abs(np.gradient(hx, axis=1)) + np.abs(np.gradient(hx, axis=0))
    fpz = np.abs(np.gradient(hz, axis=1)) + np.abs(np.gradient(hz, axis=0))
    img[valid] = BG_GROUND

    def lines(c, fp, step, k):
        d = np.abs(c / step - np.round(c / step)) * step
        return d < fp * k

    fine_ok = (fpx < 1.0 / 16.0 * 0.6) & (fpz < 1.0 / 16.0 * 0.6)
    fine = (lines(hx, fpx, 1.0 / 16.0, 0.5) | lines(hz, fpz, 1.0 / 16.0, 0.5)) & valid & fine_ok
    block_ok = (fpx < 0.5) & (fpz < 0.5)
    block = (lines(hx, fpx, 1.0, 1.0) | lines(hz, fpz, 1.0, 1.0)) & valid & block_ok
    img[fine] = GRID_FINE
    img[block] = GRID_BLOCK
    return img


DEPTH_TIE = 1e-7   # blocks: two faces closer than this are in one plane


def rasterize(cam, fr, tex, ss):
    """Z-buffered flat-face rasterisation at ss x resolution. Returns (colour float32, hit bool)."""
    Ws, Hs = cam.W * ss, cam.H * ss
    zbuf = np.full((Hs, Ws), np.inf)
    col = np.zeros((Hs, Ws, 3), dtype=np.float32)
    hit = np.zeros((Hs, Ws), dtype=bool)
    nF = len(fr.face_verts)
    if nF == 0:
        return col, hit
    persp = cam.mode == "persp"
    pc = cam.to_cam(fr.face_verts.reshape(-1, 3)).reshape(nF, 4, 3)
    A = pc[:, 0]
    E1 = pc[:, 1] - A
    E2 = pc[:, 3] - A
    n = np.cross(E1, E2)
    sc = cam.px_scale(ss)
    cxs, cys = cam.cx * ss, cam.cy * ss
    if persp:
        cs = -np.cross(A, E2)
        ct = -np.cross(E1, A)
        cd = n
        cl = np.zeros((nF, 3))
        cl[:, 2] = np.einsum("ij,ij->i", A, n)
        zok = (pc[..., 2] > 0.05).all(axis=1)
        z = np.where(pc[..., 2] > 0.05, pc[..., 2], 0.05)
        px = cxs + sc * pc[..., 0] / z
        py = cys - sc * pc[..., 1] / z
        okn = np.linalg.norm(n, axis=1) > 1e-14
    else:
        cs = np.stack([E2[:, 1], -E2[:, 0], -(A[:, 0] * E2[:, 1] - A[:, 1] * E2[:, 0])], axis=1)
        ct = np.stack([-E1[:, 1], E1[:, 0], -(E1[:, 0] * A[:, 1] - E1[:, 1] * A[:, 0])], axis=1)
        cd = np.zeros((nF, 3))
        cd[:, 2] = n[:, 2]
        cl = np.stack([-n[:, 0], -n[:, 1], np.einsum("ij,ij->i", A, n)], axis=1)
        zok = np.ones(nF, dtype=bool)
        px = cxs + pc[..., 0] * sc
        py = cys - pc[..., 1] * sc
        okn = np.abs(n[:, 2]) > 1e-14
    bx0 = np.maximum(0, np.floor(px.min(axis=1)).astype(int))
    bx1 = np.minimum(Ws, np.ceil(px.max(axis=1)).astype(int) + 1)
    by0 = np.maximum(0, np.floor(py.min(axis=1)).astype(int))
    by1 = np.minimum(Hs, np.ceil(py.max(axis=1)).astype(int) + 1)
    ok = zok & okn & (bx1 > bx0) & (by1 > by0)
    Th, Tw = tex.shape[:2]
    uv = fr.face_uv
    for i in np.nonzero(ok)[0]:
        x0, x1, y0, y1 = bx0[i], bx1[i], by0[i], by1[i]
        X = (np.arange(x0, x1) + 0.5 - cxs) / sc
        Y = (cys - (np.arange(y0, y1) + 0.5)) / sc
        Xr = X[None, :]
        Yc = Y[:, None]
        den = cd[i, 0] * Xr + cd[i, 1] * Yc + cd[i, 2]
        with np.errstate(all="ignore"):
            s = (cs[i, 0] * Xr + cs[i, 1] * Yc + cs[i, 2]) / den
            t = (ct[i, 0] * Xr + ct[i, 1] * Yc + ct[i, 2]) / den
            inside = (s >= 0) & (s <= 1) & (t >= 0) & (t <= 1)
        yy, xx = np.nonzero(inside)
        if yy.size == 0:
            continue
        dd = den[yy, xx] if den.ndim == 2 and den.shape == inside.shape else np.broadcast_to(den, inside.shape)[yy, xx]
        d = (cl[i, 0] * X[xx] + cl[i, 1] * Y[yy] + cl[i, 2]) / dd
        gy, gx = y0 + yy, x0 + xx
        # LEQUAL, as the game's depth test: of two faces in one plane (the two sides of a zero-depth cube) the later
        # one wins everywhere. A strict "<" let float noise pick per pixel and drew a hatch that is not in the game.
        sel = d <= zbuf[gy, gx] + DEPTH_TIE
        if persp:
            sel &= d > 0.01
        if not sel.any():
            continue
        yy, xx, gy, gx, d = yy[sel], xx[sel], gy[sel], gx[sel], d[sel]
        s_ = s[yy, xx]
        t_ = t[yy, xx]
        uv0, uv1, uv3 = uv[i, 0], uv[i, 1], uv[i, 3]
        u = uv0[0] + s_ * (uv1[0] - uv0[0]) + t_ * (uv3[0] - uv0[0])
        v = uv0[1] + s_ * (uv1[1] - uv0[1]) + t_ * (uv3[1] - uv0[1])
        tx = np.clip(np.floor(u * Tw).astype(int), 0, Tw - 1)
        ty = np.clip(np.floor(v * Th).astype(int), 0, Th - 1)
        texel = tex[ty, tx]
        vis = texel[:, 3] >= ALPHA_MIN
        if not vis.any():
            continue
        gy, gx, d, texel = gy[vis], gx[vis], d[vis], texel[vis]
        zbuf[gy, gx] = d
        col[gy, gx] = texel[:, :3].astype(np.float32) * np.float32(fr.shade[i])
        hit[gy, gx] = True
    return col, hit


_BG_CACHE = {}


def render_view(scene, fr, cam, ss=2, bones=False, label=None, marker=True, bg_cache=True,
                marker_top=None):
    """Render one view of an evaluated frame. cam must already be bound (cam.bind(root_off))."""
    if not hasattr(fr, "face_uv"):
        fr.face_uv = scene.model.face_uv
    key = (cam.name, cam.W, cam.H, cam.mode, round(float(cam.scale), 6), tuple(np.round(cam.target, 9)),
           ss, tuple(np.round(cam.R, 9)), tuple(np.round(cam.U, 9)))
    bg = _BG_CACHE.get(key) if bg_cache else None
    if bg is None:
        bg = render_background(cam, ss)
        if bg_cache:
            if len(_BG_CACHE) > 8:
                _BG_CACHE.clear()
            _BG_CACHE[key] = bg
    col, hit = rasterize(cam, fr, scene.tex, ss)
    out = np.where(hit[..., None], col, bg)
    if ss > 1:
        out = out.reshape(cam.H, ss, cam.W, ss, 3).mean(axis=(1, 3))
    img = Image.fromarray(np.clip(out + 0.5, 0, 255).astype(np.uint8), "RGB")
    d = ImageDraw.Draw(img)
    if marker:
        o = fr.root_off
        top = marker_top if marker_top is not None else 1.0
        px, py, _ = cam.project(np.array([[o[0], 0.0, o[2]], [o[0], top, o[2]]]))
        d.line([(px[0], py[0]), (px[1], py[1])], fill=(220, 40, 40), width=1)
        d.ellipse([px[0] - 2, py[0] - 2, px[0] + 2, py[0] + 2], outline=(220, 40, 40))
    if bones:
        font = _font()
        for n in scene.model.bones:
            p = scene.bone_pivot_world(fr, n)
            px, py, _ = cam.project(p[None, :])
            d.ellipse([px[0] - 2, py[0] - 2, px[0] + 2, py[0] + 2], fill=(20, 60, 220))
            d.text((px[0] + 4, py[0] - 5), n, fill=(20, 40, 160), font=font)
    if label:
        d.text((3, 2), label, fill=(30, 30, 30), font=_font())
    return np.array(img)


_FONT = None


def _font():
    global _FONT
    if _FONT is None:
        try:
            _FONT = ImageFont.load_default(size=11)
        except TypeError:
            _FONT = ImageFont.load_default()
    return _FONT


# ----------------------------------------------------------------------------------------------
# Report (AABB, feet, foot slide)
# ----------------------------------------------------------------------------------------------

FOOT_PLANT_TOL = 0.02     # foot is "planted" when its lowest point is within this of the ground minimum


def is_foot(name):
    return name.startswith("foot") or name.endswith("_foot")


def foot_bones(model, names=None):
    if names:
        return [n for n in model.bones if n in set(names)]
    return [n for n in model.bones if is_foot(n)]


def foot_sample(scene, fr, bone):
    """(fixed point, min y) of the bone's cubes and its descendants' cubes.

    The fixed point is the centroid of the bottom face (4 corners) of the lowest cube, so heel/toe
    rolls do not jump between corners."""
    m = scene.model
    names = set(m.descendants(bone))
    idx = [i for i, bn in enumerate(m.cube_bone) if bn in names]
    if not idx:
        return None
    best = min(idx, key=lambda i: fr.corners[i][:, 1].min())
    ymin = min(float(fr.corners[i][:, 1].min()) for i in idx)
    bottom = fr.corners[best][[0, 1, 6, 7]]      # BLB, BRB, BLF, BRF
    return bottom.mean(axis=0), ymin


def build_report(scene, ticks, foot_names=None, ground=0.0):
    feet = foot_bones(scene.model, foot_names)
    per_tick = []
    series = {f: [] for f in feet}
    for t in ticks:
        fr = scene.evaluate(t)
        lo, hi = scene.aabb(fr)
        entry = {"tick": t, "aabb": {"min": [round(float(x), 5) for x in lo],
                                     "max": [round(float(x), 5) for x in hi]}, "feet": {}}
        for f in feet:
            smp = foot_sample(scene, fr, f)
            if smp is None:
                continue
            p, ymin = smp
            entry["feet"][f] = {"lowest_vertex": [round(float(x), 5) for x in p], "min_y": round(ymin, 5)}
            series[f].append((t, p, ymin))
        per_tick.append(entry)
    step = max([b - a for a, b in zip(ticks, ticks[1:])], default=1.0)
    slide = foot_slide(series, ground=ground, step=step)
    return {"ticks": per_tick, "foot_slide": slide}


def foot_slide(series, ground=0.0, step=1.0):
    out = {"ground_y": ground, "tolerance": FOOT_PLANT_TOL, "feet": {}}
    warnings = []
    if step > 1.0:
        warnings.append("tick step %g is above 1: planted spans and slide may be under-reported" % step)
    if not any(series.values()):
        warnings.append("no foot bones matched (names starting with 'foot' or ending in '_foot'); "
                        "use --foot-bones a,b to name them. Slide not measured.")
        out["max_drift"] = None
        out["warning"] = " ".join(warnings)
        print("warning: " + out["warning"], file=sys.stderr)
        return out
    worst = 0.0
    for f, s in series.items():
        spans = []
        cur = []
        for (t, p, y) in s:
            if y - ground <= FOOT_PLANT_TOL:
                cur.append((t, p))
            else:
                if cur:
                    spans.append(cur)
                cur = []
        if cur:
            spans.append(cur)
        res = []
        for sp in spans:
            if len(sp) < 2:
                continue
            p0 = sp[0][1]
            drift = max(math.hypot(p[0] - p0[0], p[2] - p0[2]) for _, p in sp)
            res.append({"start": sp[0][0], "end": sp[-1][0], "samples": len(sp), "drift": round(drift, 5)})
            worst = max(worst, drift)
        out["feet"][f] = {"spans": res,
                          "max_drift": round(max([r["drift"] for r in res], default=0.0), 5)}
    out["max_drift"] = round(worst, 5)
    if warnings:
        out["warning"] = " ".join(warnings)
        print("warning: " + out["warning"], file=sys.stderr)
    return out


# ----------------------------------------------------------------------------------------------
# Frame/sheet/gif composition and CLI
# ----------------------------------------------------------------------------------------------

def parse_ticks(spec):
    parts = spec.split(":")
    if len(parts) not in (2, 3):
        raise GeoError("--ticks expects a:b[:step], got %r" % spec)
    a, b = float(parts[0]), float(parts[1])
    step = float(parts[2]) if len(parts) == 3 else 1.0
    if step <= 0:
        raise GeoError("tick step must be > 0")
    out = []
    i = 0
    while a + i * step <= b + 1e-9:
        out.append(round(a + i * step, 9))
        i += 1
    return out


def compute_bounds(scene, ticks):
    lo = np.full(3, np.inf)
    hi = np.full(3, -np.inf)
    for t in ticks:
        fr = scene.evaluate(t)
        a, b = scene.aabb(fr)
        a = a - fr.root_off
        b = b - fr.root_off
        lo = np.minimum(lo, a)
        hi = np.maximum(hi, b)
    lo = np.minimum(lo, [0, 0, 0])
    hi = np.maximum(hi, [0, 0.5, 0])
    return lo, hi


def union_bounds(bounds):
    """Smallest (min, max) box holding every (min, max) in bounds, so several scenes can share one camera."""
    bounds = list(bounds)
    if not bounds:
        raise GeoError("union_bounds needs at least one bounds pair")
    lo = np.min([np.asarray(b[0], dtype=np.float64) for b in bounds], axis=0)
    hi = np.max([np.asarray(b[1], dtype=np.float64) for b in bounds], axis=0)
    return lo, hi


def compose_row(imgs, names, tick, clip_label):
    H = max(i.shape[0] for i in imgs)
    gap = 4
    W = sum(i.shape[1] for i in imgs) + gap * (len(imgs) - 1)
    canvas = np.empty((H, W, 3), dtype=np.uint8)
    canvas[:] = (200, 200, 200)
    x = 0
    for i in imgs:
        h = i.shape[0]
        canvas[0:h, x:x + i.shape[1]] = i
        # pad rest with sky colour
        if h < H:
            canvas[h:H, x:x + i.shape[1]] = (226, 226, 226)
        x += i.shape[1] + gap
    return canvas


def label_image(arr, text):
    im = Image.fromarray(arr)
    ImageDraw.Draw(im).text((3, 2), text, fill=(30, 30, 30), font=_font())
    return np.array(im)


_W = {}


def _worker_init(cfg):
    scene, cams = build_scene_from_cfg(cfg)
    _W["scene"], _W["cams"], _W["cfg"] = scene, cams, cfg


def _worker_render(tick):
    cfg = _W["cfg"]
    return render_tick(_W["scene"], _W["cams"], tick, cfg)


def render_tick(scene, cams, tick, cfg):
    fr = scene.evaluate(tick)
    imgs = []
    for cam in cams:
        bc = cam.bind(fr.root_off)
        imgs.append(render_view(scene, fr, bc, ss=cfg["ss"], bones=cfg["bones"],
                                label=None, marker_top=cfg["marker_top"]))
    return imgs


def _look(cfg):
    if not cfg.get("rig"):
        return None
    with open(cfg["rig"], encoding="utf-8") as f:
        return json.load(f), int(cfg.get("tier", 3)), int(cfg.get("face", 0))


def build_scene_from_cfg(cfg):
    model = load_geo(cfg["geo"])
    clips = [load_clip(p, n) for p, n in cfg["clips"]]
    add = [load_clip(p, n) for p, n in cfg.get("additive", [])]
    scene = Scene(model, cfg["textures"], clips, cfg["speed"], _look(cfg), add)
    cams = make_cameras(cfg["views"], cfg["bounds"], cfg["distance"], cfg["scale"])
    return scene, cams


def build_parser():
    p = argparse.ArgumentParser(description="Preview GeckoLib 4.8.4 geo.json models/animations offline.")
    p.add_argument("--geo", required=True, help="geo.json model")
    p.add_argument("--texture", action="append", default=[], help="texture layer (repeatable, composited in order)")
    p.add_argument("--anim", action="append", default=[], help="animation.json (repeatable, pairs with --clip)")
    p.add_argument("--clip", action="append", default=[], help="clip name (repeatable)")
    p.add_argument("--add-clip", action="append", default=[],
                   help="clip ADDED on top of the --clip pose, from the first --anim file (repeatable): a hit clip and "
                        "its _legs over the stance, as the game plays them")
    g = p.add_mutually_exclusive_group()
    g.add_argument("--ticks", help="a:b[:step] inclusive tick range")
    g.add_argument("--tick", type=float, help="single (possibly fractional) tick")
    p.add_argument("--speed", type=float, default=0.0, help="root motion, blocks per tick along facing (+Z)")
    p.add_argument("--views", default="front,side,three_quarter", help="comma list of %s" % ",".join(PUBLIC_VIEWS))
    p.add_argument("--distance", choices=["near", "game16"], default="near")
    p.add_argument("--scale", type=float, default=None,
                   help="near: pixels per block (default 160); game16: virtual window height px (default 1080)")
    p.add_argument("--out", default="render_out", help="output directory")
    p.add_argument("--report", action="store_true", help="print JSON report (AABBs, feet, foot slide)")
    p.add_argument("--bones", action="store_true", help="draw pivot dots with bone names")
    p.add_argument("--rig", help="troop_rig/<troop>.json: draw only the bones the game shows for --tier and --face, and "
                                 "only the bow draw frame the clip keys (without it every tier and frame is drawn at once)")
    p.add_argument("--tier", type=int, default=3, help="armour and weapon tier with --rig (default 3)")
    p.add_argument("--face", type=int, default=0, help="face variant with --rig (default 0)")
    p.add_argument("--ss", type=int, default=2, help="supersampling factor (default 2)")
    p.add_argument("--jobs", type=int, default=0, help="worker processes (default: auto)")
    p.add_argument("--foot-bones", default=None, help="comma list of bone names to treat as feet "
                   "(overrides the foot*/ *_foot name match)")
    p.add_argument("--ground", type=float, default=0.0,
                   help="ground height in model units (blocks); default 0 = rest ground")
    p.add_argument("--no-gif", action="store_true")
    p.add_argument("--no-png", action="store_true", help="skip per-tick PNG frames")
    return p


def main(argv=None):
    args = build_parser().parse_args(argv)
    t0 = time.time()
    try:
        if args.add_clip and not args.anim:
            raise GeoError("--add-clip needs an --anim file")
        add_anim = args.anim[0] if args.anim else None
        if len(args.anim) != len(args.clip):
            if len(args.anim) == 1 and args.clip:
                args.anim = args.anim * len(args.clip)
            else:
                raise GeoError("--anim and --clip must be given in matching pairs")
        if args.ticks:
            ticks = parse_ticks(args.ticks)
        elif args.tick is not None:
            ticks = [args.tick]
        else:
            ticks = [0.0]
        views = [v.strip() for v in args.views.split(",") if v.strip()]
        cfg = {"geo": args.geo, "textures": args.texture, "clips": list(zip(args.anim, args.clip)),
               "speed": args.speed, "views": views, "distance": args.distance, "scale": args.scale,
               "ss": args.ss, "bones": args.bones, "rig": args.rig, "tier": args.tier, "face": args.face,
               "additive": [(add_anim, n) for n in args.add_clip]}
        model = load_geo(args.geo)
        clips = [load_clip(p, n) for p, n in cfg["clips"]]
        add = [load_clip(p, n) for p, n in cfg["additive"]]
        scene = Scene(model, args.texture, clips, args.speed, _look(cfg), add)   # raises on overlapping bones
        lo, hi = compute_bounds(scene, ticks)
        cfg["bounds"] = (lo.tolist(), hi.tolist())
        cfg["marker_top"] = float(hi[1])
        cams = make_cameras(views, cfg["bounds"], args.distance, args.scale)
    except (GeoError, OSError, ValueError, KeyError) as e:
        print("error: %s" % e, file=sys.stderr)
        return 2

    if args.report:
        fb = [x.strip() for x in args.foot_bones.split(",") if x.strip()] if args.foot_bones else None
        rep = build_report(scene, ticks, foot_names=fb, ground=args.ground)
        print(json.dumps(rep, indent=1))

    os.makedirs(args.out, exist_ok=True)
    jobs = args.jobs or min(os.cpu_count() or 1, 8)
    jobs = max(1, min(jobs, len(ticks)))
    results = []
    if jobs > 1 and len(ticks) >= 4:
        import multiprocessing as mp
        with mp.Pool(jobs, initializer=_worker_init, initargs=(cfg,)) as pool:
            results = pool.map(_worker_render, ticks, chunksize=max(1, len(ticks) // (jobs * 4)))
    else:
        results = [render_tick(scene, cams, t, cfg) for t in ticks]

    clip_label = scene.clip_label()
    step = (ticks[1] - ticks[0]) if len(ticks) > 1 else 1.0
    gif_frames = []
    sheet_cols = list(range(len(ticks)))
    note = ""
    if len(sheet_cols) > 8:
        idx = np.linspace(0, len(ticks) - 1, 8).round().astype(int)
        sheet_cols = sorted(set(int(i) for i in idx))
        note = "showing %d of %d ticks" % (len(sheet_cols), len(ticks))
    cells = {}
    for k, (t, imgs) in enumerate(zip(ticks, results)):
        row = compose_row(imgs, views, t, clip_label)
        row = label_image(row, "t=%g  %s" % (t, clip_label)) if False else row
        if not args.no_png:
            Image.fromarray(row).save(os.path.join(args.out, "frame_%04d_t%g.png" % (k, t)))
        if not args.no_gif and len(ticks) > 1:
            gif_frames.append(Image.fromarray(row).quantize(colors=256, dither=Image.Dither.NONE))
        if k in sheet_cols:
            cells[k] = imgs
    # sheet: columns = ticks, rows = views
    CELL = 220
    font = _font()
    lab_h = 14
    scaled = {}
    for k, imgs in cells.items():
        for vi, im in enumerate(imgs):
            pil = Image.fromarray(im)
            f = min(1.0, CELL / max(pil.size))
            if f < 1.0:
                pil = pil.resize((max(1, int(pil.width * f)), max(1, int(pil.height * f))), Image.LANCZOS)
            scaled[(k, vi)] = pil
    colw = max(p.width for p in scaled.values())
    rowh = [max(scaled[(k, vi)].height for k in cells) for vi in range(len(views))]
    sheet = Image.new("RGB", (colw * len(cells), sum(h + lab_h for h in rowh) + (lab_h if note else 0)), (255, 255, 255))
    d = ImageDraw.Draw(sheet)
    y = 0
    if note:
        d.text((3, 1), note, fill=(0, 0, 0), font=font)
        y = lab_h
    for vi, vname in enumerate(views):
        for ci, k in enumerate(sorted(cells)):
            x = ci * colw
            d.text((x + 3, y + 1), "%s t=%g %s" % (vname, ticks[k], clip_label[:34]), fill=(0, 0, 0), font=font)
            sheet.paste(scaled[(k, vi)], (x, y + lab_h))
        y += rowh[vi] + lab_h
    sheet.save(os.path.join(args.out, "sheet.png"))
    if gif_frames:
        dur = max(20, int(round(step * 50)))
        gif_frames[0].save(os.path.join(args.out, "anim.gif"), save_all=True, append_images=gif_frames[1:],
                           duration=dur, loop=0, disposal=1)
    print("rendered %d tick(s) x %d view(s) -> %s in %.1fs" % (len(ticks), len(views), args.out, time.time() - t0),
          file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
