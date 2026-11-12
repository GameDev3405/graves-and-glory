"""Review booth: render the three viewers of one clip (reference, previous, new) with one shared camera."""
from __future__ import annotations

import contextlib
import hashlib
import io
import json
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
# Foot slide is the renderer's planted-foot drift, in blocks. It is only measured on clips that travel (root speed
# above 0), and it is flagged when it is this much worse than the reference clip's own figure: the approved Knight
# walk measures about 0.19 by this metric (the foot rolls heel to toe), so a fixed 0.05 limit would flag everything.
FOOT_SLIDE_MARGIN = 0.05


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


def _shown_ticks(clip):
    """The ticks a sheet shows of a clip, one per game tick. A play-once clip has stopped AT its length (Clip.effective_tick
    returns None there and the bones go back to the model pose), so its last frame is taken a hair before the end: the
    sheet ends on the clip's last pose, as the game's last drawn frame does, not on the bind pose (sweep T02-F1)."""
    end = clip.length - 1e-4 if clip.loop == "once" else float("inf")
    return [min(float(t), end) if t >= end else t for t in range(_ticks_of(clip))]


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
        path, name = source.clips[-1]
        with open(path, encoding="utf-8") as f:
            under = rg.additive_under(name, set(json.load(f).get("animations", {})))
        if under:       # a hit is added onto the ready stance in game: show it that way, not over the rest pose
            hold, add, hand = rg.additive_split(name, [n for _, n in source.clips[:-1]], under)
            where = dict((n, q) for q, n in source.clips)       # the shake-off starts from the stun, see additive_split
            scene = rg.Scene(model, list(source.textures), [rg.load_clip(where.get(u, path), u) for u in hold], 0.0, None,
                             [rg.load_clip(where[a], a) for a in add],
                             (rg.load_clips_if_present(path, hand), rg.STUN_OUT_TICKS, rg.STUN_ENTRY_TICKS))
        else:
            scene = rg.Scene(model, list(source.textures), clips, source.speed)
    except (rg.GeoError, OSError, ValueError, KeyError) as e:
        raise BoothError("%s viewer: %s" % (viewer, e))
    return scene, _shown_ticks(clips[-1])


def _slide(scene_ticks, source, foot_bones, fallback=False):
    """Planted-foot drift in blocks, or None when the clip does not travel or no foot bone is found.
    fallback: when none of foot_bones exist in this model (a reference troop), use the renderer's name match."""
    if source.speed <= 0:
        return None
    scene, ticks = scene_ticks
    names = foot_bones
    if fallback and foot_bones and not any(n in scene.model.bones for n in foot_bones):
        names = None
    with contextlib.redirect_stderr(io.StringIO()):       # the renderer warns on stderr when no foot bone matches
        rep = rg.build_report(scene, ticks, foot_names=names, ground=0.0)
    drift = rep["foot_slide"].get("max_drift")
    return None if drift is None else float(drift)


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
    slide = _slide(scenes["new"], sources["new"], foot_bones)
    ref = _slide(scenes["reference"], sources["reference"], foot_bones, fallback=True) if "reference" in scenes else None
    numbers = {"foot_slide": slide, "foot_slide_reference": ref if slide is not None else None}
    flags = ["foot_slide"] if slide is not None and ref is not None and slide > ref + FOOT_SLIDE_MARGIN else []
    return ClipRender(frames, len(scenes["new"][1]), _hash(frames["new"]), numbers, flags)
