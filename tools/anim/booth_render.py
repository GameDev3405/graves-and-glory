"""Review booth: render the three viewers of one clip (reference, previous, new) with one shared camera."""
from __future__ import annotations

import contextlib
import hashlib
import io
import math
import os
import sys
from dataclasses import dataclass

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)
import render_geo as rg  # noqa: E402
from booth_config import BoothError  # noqa: E402

VIEWERS = ("reference", "previous", "new")
DISTANCES = ("near", "game16")
VIEW = "three_quarter"
FOOT_SLIDE_LIMIT = 0.05      # blocks; docs/anim-rebuild/PLAN.md


@dataclass
class Source:
    geo: str
    textures: list
    clips: list          # (animation file, clip name) pairs: base clips first, the reviewed clip last
    speed: float = 0.0


@dataclass
class ClipRender:
    frames: dict         # viewer -> distance -> [RGB uint8 array per tick]
    ticks: int           # New's length in frames
    frame_hash: str
    numbers: dict
    flags: list


def _ticks_of(clip):
    return int(math.ceil(clip.length - 1e-9)) + 1


def clip_ticks(anim_path, clip_name):
    """Frames needed to show the clip once: one per game tick, both ends included."""
    try:
        return _ticks_of(rg.load_clip(anim_path, clip_name))
    except (rg.GeoError, OSError, ValueError, KeyError) as e:
        raise BoothError("cannot read clip %r from %s: %s" % (clip_name, anim_path, e))


def _scene(source, viewer):
    if not source.clips:
        raise BoothError("%s viewer has no clip to play" % viewer)
    try:
        model = rg.load_geo(source.geo)
        clips = [rg.load_clip(path, name) for path, name in source.clips]
        scene = rg.Scene(model, list(source.textures), clips, source.speed)
    except (rg.GeoError, OSError, ValueError, KeyError) as e:
        raise BoothError("%s viewer: %s" % (viewer, e))
    return scene, list(range(_ticks_of(clips[-1])))


def _hash(frames_by_distance):
    h = hashlib.sha256()
    for dist in DISTANCES:
        for f in frames_by_distance[dist]:
            h.update(("%s:%dx%dx%d;" % ((dist,) + f.shape)).encode("ascii"))
            h.update(np.ascontiguousarray(f).tobytes())
    return h.hexdigest()


def render_clip(sources, foot_bones=None, ss=2):
    """sources: {"reference": Source|None, "previous": Source|None, "new": Source}."""
    if set(sources) != set(VIEWERS):
        raise BoothError("sources must have exactly the keys %s" % ", ".join(VIEWERS))
    if sources["new"] is None:
        raise BoothError("the new viewer needs a source")
    scenes = {v: _scene(sources[v], v) for v in VIEWERS if sources[v] is not None}
    lo, hi = rg.union_bounds([rg.compute_bounds(sc, ticks) for sc, ticks in scenes.values()])
    bounds = (lo.tolist(), hi.tolist())
    frames = {v: {} for v in scenes}
    for dist in DISTANCES:
        cam = rg.make_cameras([VIEW], bounds, dist, None)[0]
        for v, (sc, ticks) in scenes.items():
            out = []
            for t in ticks:
                fr = sc.evaluate(t)
                img = rg.render_view(sc, fr, cam.bind(fr.root_off), ss=ss, bones=False, label=None,
                                     marker_top=float(hi[1]))
                out.append(np.ascontiguousarray(np.asarray(img)[..., :3], dtype=np.uint8))
            frames[v][dist] = out
    sc, ticks = scenes["new"]
    with contextlib.redirect_stderr(io.StringIO()):       # the renderer warns on stderr when no foot bone matches
        rep = rg.build_report(sc, ticks, foot_names=foot_bones, ground=0.0)
    drift = rep["foot_slide"].get("max_drift")
    numbers = {"foot_slide": None if drift is None else float(drift)}
    flags = ["foot_slide"] if drift is not None and drift > FOOT_SLIDE_LIMIT else []
    return ClipRender(frames, len(ticks), _hash(frames["new"]), numbers, flags)
