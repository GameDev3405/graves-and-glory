"""Review booth: per-troop render config (art/anim/<family>/booth.<troop>.json).

See docs/superpowers/specs/2026-11-09-review-booth-design.md. All paths in a config are repo-relative.
"""
from __future__ import annotations

import json
import re
from dataclasses import dataclass, field


class BoothError(Exception):
    pass


NAME_RE = re.compile(r"^[A-Za-z0-9_.]+$")
TOP_KEYS = {"troop", "family", "geo", "textures", "anim", "foot_bones", "reference", "clips"}
REF_KEYS = {"geo", "textures", "anim"}
CLIP_KEYS = {"name", "base", "speed", "impact_tick", "reference", "reference_base"}


@dataclass
class ClipSpec:
    name: str
    base: list = field(default_factory=list)
    speed: float = 0.0
    impact_tick: int | None = None
    reference: str | None = None
    reference_base: list = field(default_factory=list)


@dataclass
class SourceSpec:
    geo: str
    textures: list
    anim: str


@dataclass
class BoothConfig:
    troop: str
    family: str
    source: SourceSpec
    reference: SourceSpec | None
    foot_bones: list | None
    clips: list

    def input_paths(self):
        out = []
        for p in [self.source.geo, *self.source.textures, self.source.anim]:
            if p not in out:
                out.append(p)
        return out


def _keys(obj, allowed, what):
    if not isinstance(obj, dict):
        raise BoothError("%s must be an object" % what)
    extra = sorted(set(obj) - allowed)
    if extra:
        raise BoothError("%s has unknown key(s): %s" % (what, ", ".join(extra)))


def _name(v, what):
    if not isinstance(v, str) or not NAME_RE.match(v):
        raise BoothError("%s must be a name of letters, digits, '_' or '.', got %r" % (what, v))
    return v


def _path(v, what):
    if not isinstance(v, str) or not v:
        raise BoothError("%s must be a repo-relative path" % what)
    norm = v.replace("\\", "/")
    if norm.startswith("/") or re.match(r"^[A-Za-z]:", norm) or ".." in norm.split("/"):
        raise BoothError("%s must be a repo-relative path without '..', got %r" % (what, v))
    return norm


def _names(v, what):
    if not isinstance(v, list):
        raise BoothError("%s must be a list of names" % what)
    return [_name(x, what) for x in v]


def _source(obj, what):
    for k in ("geo", "textures", "anim"):
        if k not in obj:
            raise BoothError("%s is missing %r" % (what, k))
    if not isinstance(obj["textures"], list):
        raise BoothError("%s textures must be a list of paths" % what)
    return SourceSpec(_path(obj["geo"], what + " geo"),
                      [_path(t, what + " textures") for t in obj["textures"]],
                      _path(obj["anim"], what + " anim"))


def load_config(path):
    try:
        with open(path, encoding="utf-8") as f:
            raw = json.load(f)
    except OSError as e:
        raise BoothError("cannot read config %s: %s" % (path, e))
    except ValueError as e:
        raise BoothError("config %s is not valid JSON: %s" % (path, e))
    _keys(raw, TOP_KEYS, "config")
    for k in ("troop", "family", "clips"):
        if k not in raw:
            raise BoothError("config is missing %r" % k)
    troop = _name(raw["troop"], "troop")
    family = _name(raw["family"], "family")
    source = _source(raw, "config")
    reference = None
    if raw.get("reference") is not None:
        _keys(raw["reference"], REF_KEYS, "reference")
        reference = _source(raw["reference"], "reference")
    foot_bones = None
    if raw.get("foot_bones") is not None:
        foot_bones = _names(raw["foot_bones"], "foot_bones")
    if not isinstance(raw["clips"], list) or not raw["clips"]:
        raise BoothError("clips must be a non-empty list")
    clips, seen = [], set()
    for c in raw["clips"]:
        _keys(c, CLIP_KEYS, "clip")
        if "name" not in c:
            raise BoothError("a clip is missing its name")
        name = _name(c["name"], "clip name")
        if name in seen:
            raise BoothError("clip %r is listed twice" % name)
        seen.add(name)
        what = "clip %r" % name
        impact = c.get("impact_tick")
        if impact is not None and (isinstance(impact, bool) or not isinstance(impact, int) or impact < 0):
            raise BoothError("%s impact_tick must be a whole number of ticks, 0 or more" % what)
        speed = c.get("speed", 0.0)
        if isinstance(speed, bool) or not isinstance(speed, (int, float)):
            raise BoothError("%s speed must be a number" % what)
        ref = c.get("reference")
        if ref is not None:
            if reference is None:
                raise BoothError("%s names a reference clip but the config has no top-level reference" % what)
            ref = _name(ref, what + " reference")
        clips.append(ClipSpec(name, _names(c.get("base", []), what + " base"), float(speed), impact, ref,
                              _names(c.get("reference_base", []), what + " reference_base")))
    return BoothConfig(troop, family, source, reference, foot_bones, clips)
