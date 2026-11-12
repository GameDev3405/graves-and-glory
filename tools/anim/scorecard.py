#!/usr/bin/env python3
"""Animation scorecard: scores every GeckoLib troop clip against limits derived from the approved Knight v2 clips.

  python tools/anim/scorecard.py --troop knight [--json out.json]
  python tools/anim/scorecard.py --all [--json out.json]
  python tools/anim/scorecard.py --derive            # recompute tools/anim/scorecard_limits.json from the knight

Exit status is 1 when any scored clip fails a metric, 0 otherwise (2 on a usage error).

A scored clip is one clip of the animation file played with its legs clip (booth config `base`, else the paired
`<name>_legs`), the way the game plays it. Every metric has a value, a limit and a status (pass, fail or n/a); a metric
that does not apply (no weapon, not a slash, not looping, no feet...) is n/a, never pass. Posing is render_geo's own
forward kinematics (Scene.evaluate) on the troop's tier-3 visible rig (troop_rig/<troop>.json), sampled every half tick.

Metrics (units in brackets; how each limit is derived is written into scorecard_limits.json):
  elbow_bend, knee_bend  [deg]   forearm (shin) direction in the upper arm's (thigh's) frame, projected on the limb's
                                 front/back plane: 0 = straight, + = normal flexion (elbow forward, knee back),
                                 - = hyperextension. Value is the clip's [min, max]; it must lie inside the limit range.
                                 Biped rigs only (n/a otherwise), as are the arms/legs/torso speeds and foot slide.
  ang_speed_<group> [deg/100ms]  largest rotation of any bone of the group over any 2-tick window. arms, legs and torso
                                 use the bone's rotation relative to its parent (the joint), weapon its world rotation.
  foot_slide     [blocks]        render_geo.foot_slide on planted feet. Gait clips move the root at the troop's booth
                                 config speed; with no booth config a gait's foot slide is n/a.
  pen_weapon     [px]            how deep a weapon or held-prop cube reaches inside a torso, head or leg cube: the
                                 deepest point of a lattice over the cube (<= 1 px apart) measured to the body cube's
                                 nearest face (oriented boxes; 1 px = 1/16 block). A blade run through the body reads
                                 as deep as its middle, not just its thickness.
  pen_limb       [px]            the same for forearm/hand and shin/foot cubes inside a torso/hips cube, except that
                                 along the axis the limb comes in on (the side its elbow/knee is on) depth counts from
                                 that entry face, so a forearm through a 4 px torso reads up to 4 px, not 2.
  edge_lead      [deg]           slash attacks: angle between the blade's edge (weapon bone local x, either side unless
                                 the weapon is single-edged) and the blade tip's travel across the blade, at the design
                                 impact tick (the builder's EDGE_LEAD definition, art/anim/knight/build_anims.py).
  impact_tick    [ticks]         melee attacks with a design.json attackVariants entry: the clip's contact is the
                                 2-tick window in which the weapon tip drives furthest toward the foe (the model's
                                 front), so a wind-up never counts; its centre must land within the tolerance of the
                                 variant's impactTicks.
  loop_pose [deg], loop_pos [px], loop_vel [deg/tick]
                                 looping clips: rotation and position gap between the last and first frame, and the jump
                                 in a bone's rotation speed across the wrap (1-tick finite differences), judged against
                                 that bone's own peak speed in the clip (a ratio from the knight) with a deg/tick floor.

Limits are per clip kind; scorecard_limits.json 'families' widens some for a troop's design.json family, with reasons.
"""
from __future__ import annotations

import argparse
import contextlib
import datetime
import glob
import io
import json
import math
import os
import re
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)
import render_geo as rg  # noqa: E402

REPO = os.path.dirname(os.path.dirname(HERE))
LIMITS_PATH = os.path.join(HERE, "scorecard_limits.json")
APPROVED_PATH = os.path.join(HERE, "scorecard_approved.json")
ASSETS = "src/main/resources/assets/gravesandglory"
RENDERER = "src/main/java/gg/gravesandglory/GeoTroops.java"  # the APPROVED and PREVIEW sets (GeoTroopRenderer reads them)
DESIGN = "sheets/design.json"
KNIGHT = "knight"

STEP = 0.5            # ticks between samples
WINDOW = 2.0          # ticks: 100 ms
TIER = 3              # rig stage scored (the full kit)
KINDS = ("attack", "gait", "fallback", "stance", "hit", "death")
GROUPS = ("arms", "legs", "torso", "weapon")
TORSO_BONES = ("torso", "hips", "body", "chest", "waist")
MELEE = ("stab", "slash", "blunt")
GAIT = ("walk", "run", "charge_run")

# How each limit is made from the knight's own numbers. "add" widens a maximum (or a range on both sides) by a fixed
# amount, "scale" multiplies a maximum. Recorded in the limits file next to every limit.
HYPER = -5.0          # deg: anatomical cap, no elbow or knee bends backwards further than this, whatever the family
SPEED_FLOOR = 8.0     # deg/100ms: no angular speed limit is tighter than this (a near-still knight stance reads ~1)
LOOP_VEL_FLOOR = 8.0  # deg/tick: a wrap kink this size is a pop of ~4 deg, under 1 px at the end of a 12 px limb
_SPEED_HOW = "knight per-kind max x 1.25, never under %g deg/100ms" % SPEED_FLOOR
_BEND_HOW = ("maximum: knight per-kind max + 10 deg. Minimum: the anatomical cap %g deg (no hyperextension), or the "
             "knight's own minimum where it goes further (its hits); a limb straighter than the knight's is a style, "
             "not a fault" % HYPER)
MARGINS = {
    "elbow_bend": {"add": 10.0, "floor_lo": HYPER, "how": _BEND_HOW},
    "knee_bend": {"add": 10.0, "floor_lo": HYPER, "how": _BEND_HOW},
    "ang_speed_arms": {"scale": 1.25, "floor": SPEED_FLOOR, "kind_scale": {"death": 2.0},
                       "how": _SPEED_HOW + "; death x 2.0 (a falling body flings its arms: the knight's deaths are "
                                           "its one style, other troops fall harder)"},
    "ang_speed_legs": {"scale": 1.25, "floor": SPEED_FLOOR, "how": _SPEED_HOW},
    "ang_speed_torso": {"scale": 1.25, "floor": SPEED_FLOOR, "how": _SPEED_HOW},
    "ang_speed_weapon": {"scale": 1.25, "floor": SPEED_FLOOR, "how": _SPEED_HOW},
    "foot_slide": {"add": 0.05, "how": "knight per-kind max + 0.05 blocks (the booth's foot slide margin)"},
    "pen_weapon": {"add": 0.5, "how": "knight per-kind max + 0.5 px"},
    "pen_limb": {"add": 0.5, "how": "knight per-kind max + 0.5 px"},
    "edge_lead": {"add": 10.0, "how": "knight max over its slash attacks + 10 deg"},
    "impact_tick": {"add": 1.0, "how": "tolerance = knight's largest |contact - impactTicks| + 1 tick"},
    "loop_pose": {"add": 1.0, "how": "knight per-kind max over its looping clips + 1 deg"},
    "loop_pos": {"add": 0.25, "how": "knight per-kind max over its looping clips + 0.25 px"},
    "loop_vel": {"scale": 1.25, "floor": LOOP_VEL_FLOOR,
                 "how": "the limit is a ratio: a bone's wrap kink may be at most (knight per-kind max of kink / that "
                        "bone's peak 1-tick speed in the clip) x 1.25 times its own peak speed, and never less than "
                        "%g deg/tick (a ~4 deg pop: under 1 px at the end of a 12 px limb)" % LOOP_VEL_FLOOR},
}
UNITS = {"elbow_bend": "deg", "knee_bend": "deg", "foot_slide": "blocks", "pen_weapon": "px", "pen_limb": "px",
         "edge_lead": "deg", "impact_tick": "tick", "loop_pose": "deg", "loop_pos": "px", "loop_vel": "deg/tick"}
for _g in GROUPS:
    UNITS["ang_speed_" + _g] = "deg/100ms"
METRICS = tuple(MARGINS)
RANGE_METRICS = ("elbow_bend", "knee_bend")
BIPED_METRICS = ("elbow_bend", "knee_bend", "ang_speed_arms", "ang_speed_legs", "ang_speed_torso", "foot_slide")
GLOBAL_METRICS = ("edge_lead", "impact_tick")   # attacks only: one limit
# Single-edged weapons: the side of the blade bone's local x the edge is on (knight builder EDGE_SIGN). ic_breaker's
# greataxe is not in the builder's table (art/anim/greataxe aims the bit by hand); its geo puts the bit, the wide head
# cubes, on the weapon bone's local -x of the haft (x -0.12..0.31 against the haft at 0.31..0.44 blocks) and only a
# small back spike on +x, so its edge is -1. ic_hearthguard's bearded axe is the same way round (bit cubes at file
# x +1..+6 of the haft, which is the bone's local -x; back spike on +x) and art/anim/axe aims it with -1; the +1 here
# until 2026-11-12 was copied from the knight builder's table and failed every clip that led with the real bit.
# Every other weapon is double-edged and either edge may lead.
EDGE_SIGN = {"tc_raider": -1.0, "ic_hearthguard": -1.0, "ic_breaker": -1.0}
# Family allowances (design.json troops[].family). The knight is sword_shield: its numbers are that family's limits.
# Another family's limit is widened only where its fighting style needs the motion; each entry says why. Values are
# limits in the metric's unit, per clip kind ("all" = every kind); they widen, never narrow.
FAMILIES = {
    "bow": {
        "elbow_bend": {"all": [HYPER, 145.0], "reason": "the draw (volley, and the aim stances that hold it) folds "
                                                        "the string arm until the hand is at the cheek (~130-145 "
                                                        "deg), which a sword never does"},
        "ang_speed_arms": {"attack": 160.0, "reason": "the volley's draw and release snap the string arm back and "
                                                      "out faster than the knight's cuts"},
    },
    "crossbow": {
        "elbow_bend": {"attack": [HYPER, 125.0], "reason": "spanning the crossbow folds the arm to haul the string "
                                                           "back to the nut"},
    },
    "spear": {
        "elbow_bend": {"stance": [HYPER, 100.0], "reason": "the spear wall couches the shaft high at the shoulder, "
                                                          "the rear elbow folded behind it"},
        "ang_speed_legs": {"attack": 65.0, "reason": "the high thrust is a lunge: the lead leg drives the point "
                                                     "forward, which the knight's planted cuts never do"},
        "pen_limb": {"attack": 2.05, "stance": 2.0,
                     "reason": "Kort-approved wm_tusk attack_thrust r3 measures 2.0: a two-hand "
                               "spear's front forearm crosses the chest to the shaft in the load and "
                               "the rip (ticks 2-3, 12.5-13); stance: Kort-approved wm_tusk r3 guard measures 1.93-1.98"},
    },
    "thrower": {
        "elbow_bend": {"all": [HYPER, 130.0],
                       "reason": "the harpoon is carried cocked above the shoulder, the throwing elbow folded"},
        "ang_speed_arms": {"attack": 180.0, "reason": "an overarm throw whips the arm faster than any melee swing"},
    },
}


class ScoreError(Exception):
    pass


# ----------------------------------------------------------------------------------------------
# Clip naming
# ----------------------------------------------------------------------------------------------

def stem(name):
    return re.sub(r"_(upper|legs)$", "", name)


def clip_kind(name):
    s = stem(name)
    if s.startswith("attack"):
        return "attack"
    if s in GAIT:
        return "gait"
    if s == "stance_fallback":
        return "fallback"     # a stepping retreat with no root speed: its feet travel, unlike a held stance
    if s.startswith("stance") or s == "idle":
        return "stance"
    if s.startswith(("hit", "stun")):
        return "hit"
    if s.startswith("death") or s in ("fake_death", "rise"):
        return "death"
    return "other"


def legs_for(name, names):
    """The legs clip the game plays under clip `name`, by name: <stem>_legs, then shorter stems (dropping words from
    the end, then the front), then stance_ready_legs for a stance. None when nothing fits."""
    if name.endswith("_legs"):
        return None
    words = stem(name).split("_")
    cands = ["_".join(words[:i]) for i in range(len(words), 0, -1)] + ["_".join(words[i:]) for i in range(1, len(words))]
    for c in cands:
        if c + "_legs" in names and c + "_legs" != name:
            return c + "_legs"
    if words[0] == "stance" and "stance_ready_legs" in names:
        return "stance_ready_legs"
    return None


def units(names, config_clips=None):
    """[(clip, [base clips], speed or None)] for every clip of an animation file; a legs clip another clip plays on top
    of is scored with that clip, not on its own. config_clips: booth ClipSpecs (their base and speed win)."""
    names = list(names)
    have = set(names)
    cfg = {c.name: c for c in (config_clips or [])}
    out, used = [], set()
    for n in names:
        if n.endswith("_legs"):
            continue
        if n in cfg:
            base = [b for b in cfg[n].base if b in have]
            speed = cfg[n].speed if cfg[n].speed > 0 else None
        else:
            leg = legs_for(n, have)
            base = [leg] if leg else []
            speed = None
        used.update(base)
        out.append((n, base, speed))
    out += [(n, [], None) for n in names if n.endswith("_legs") and n not in used]
    order = {n: i for i, n in enumerate(names)}
    return sorted(out, key=lambda u: order[u[0]])


# ----------------------------------------------------------------------------------------------
# Rig: which bones and cubes each metric looks at
# ----------------------------------------------------------------------------------------------

def _tokens(name):
    return set(name.split("_"))


def visible_bones(model, rules):
    """Bones shown at stage TIER, face 0, from troop_rig rules (all bones when there are none). Pull states of a bow
    are swapped in by the game while drawing; the rest pose is scored."""
    if not rules:
        return set(model.bones)
    spec = rules.get("bones", {})
    shown = set()
    for n in model.order:
        r = spec.get(n, {})
        ok = TIER in r.get("stages", [TIER]) and 0 in r.get("variants", [0]) and "_pull_" not in n
        p = model.bones[n].parent
        if ok and (p is None or p in shown):
            shown.add(n)
    return shown


class _View:
    def __init__(self, **kw):
        self.__dict__.update(kw)


class Rig:
    def __init__(self, model, rules=None, troop=None):
        self.full = model
        self.troop = troop
        vis = visible_bones(model, rules)
        tags = {n: r.get("tag") for n, r in (rules or {}).get("bones", {}).items()}
        held_roots = [n for n in model.order if n in vis and tags.get(n) in ("weapon", "draw")]
        if not rules:
            held_roots = [n for n in model.order if n.startswith(("weapon", "sword")) and
                          model.bones[n].parent in ("right_hand", "left_hand")]
        self.weapon = self._subtree(held_roots, vis)
        props = [n for n in model.order if n in vis and tags.get(n) == "shield"]
        self.props = self._subtree(props, vis) - self.weapon
        # a lean copy of the model: visible cubes only and no faces, so Scene.evaluate only poses bones and corners
        lean = rg.Model()
        lean.bones, lean.order, lean.tex_w, lean.tex_h = model.bones, model.order, model.tex_w, model.tex_h
        keep = [i for i, b in enumerate(model.cube_bone) if b in vis]
        lean.cube_list = [model.cube_list[i] for i in keep]
        lean.cube_bone = [model.cube_bone[i] for i in keep]
        lean.cube_face_range = [(0, 0)] * len(keep)
        lean.face_verts = np.zeros((0, 4, 3))
        lean.face_uv = np.zeros((0, 4, 2))
        lean.face_norm = np.zeros((0, 3))
        self.model = lean
        cb = lean.cube_bone

        def cubes(bones):
            return [i for i, b in enumerate(cb) if b in bones]

        names = list(model.order)
        self.groups = {
            "arms": [n for n in names if _tokens(n) & {"arm", "forearm", "hand"} and n not in self.weapon | self.props],
            "legs": [n for n in names if _tokens(n) & {"leg", "shin", "foot"}],
            "torso": [n for n in names if n in TORSO_BONES],
        }
        self.biped = all(n in model.bones for n in ("right_arm", "right_forearm", "left_arm", "left_forearm"))
        self.elbows = [(model.bones[n].parent, n) for n in names if "forearm" in _tokens(n)
                       and model.bones[n].parent and "arm" in _tokens(model.bones[n].parent)]
        self.knees = [(model.bones[n].parent, n) for n in names if "shin" in _tokens(n)
                      and model.bones[n].parent and "leg" in _tokens(model.bones[n].parent)]
        core = {n for n in names if n in TORSO_BONES}
        head = self._subtree(["head"], vis) if "head" in model.bones else set()
        legs = {n for n in names if _tokens(n) & {"leg", "shin", "foot"}}
        limb_roots = [n for n in names if _tokens(n) & {"forearm", "shin"} and n in vis]
        limbs = self._subtree(limb_roots, vis) - self.weapon - self.props
        held = cubes(self.weapon | self.props)
        body = cubes(core | head | legs)
        lat = lambda idx: [(i, lattice(lean.cube_list[i])) for i in idx]  # noqa: E731
        self.pen_weapon = (lat(held), body) if held and body else None
        core_idx = cubes(core)
        # each limb cube remembers its limb's root joint (the forearm's elbow, the shin's knee): the side of a body
        # box that joint is on is the side the limb comes in through
        root_of = {}
        for r in limb_roots:
            for d in self.full.descendants(r):
                root_of.setdefault(d, r)
        self.pen_limb = ([(i, lattice(lean.cube_list[i]), root_of[cb[i]]) for i in cubes(limbs)], core_idx)             if cubes(limbs) and core_idx else None
        self.feet = [n for n in names if rg.is_foot(n) and n in vis]
        # foot slide follows each foot bone's own cubes: with a toe cube as a child, render_geo's "lowest cube" flips
        # between foot and toe on a flat foot and reads as 3 px of slide on a knight standing still
        self.foot_idx = [i for i, b in enumerate(cb) if b in self.feet]
        fm = rg.Model()
        fm.bones, fm.order = model.bones, model.order
        fm.cube_list = [lean.cube_list[i] for i in self.foot_idx]
        fm.cube_bone = [cb[i] for i in self.foot_idx]
        self.feet_view = _View(model=fm)
        self._blade(cubes)

    def _subtree(self, roots, vis):
        out = set()
        for r in roots:
            out.update(d for d in self.full.descendants(r) if d in vis)
        return out

    def _blade(self, cubes):
        """Blade bone, tip (in model space, before the bone's pose) and edge sign, as the builder defines them."""
        self.blade_bone = None
        idx = cubes(self.weapon)
        if not idx:
            return
        cl = self.model.cube_list
        blade = max(idx, key=lambda i: np.ptp(cl[i].corners[:, 1]))
        bone = self.model.cube_bone[blade]
        own = [i for i in idx if self.model.cube_bone[i] == bone]
        c = cl[blade].corners
        sign = EDGE_SIGN.get(self.troop)
        if sign:
            top = max(own, key=lambda i: cl[i].corners[:, 1].max())
            t = cl[top].corners
            tip = (t[:, 0].mean(), t[:, 1].max(), t[:, 2].mean())
        else:
            tip = (c[:, 0].mean(), max(cl[i].corners[:, 1].max() for i in own), c[:, 2].mean())
        self.blade_bone, self.tip, self.edge_sign = bone, np.array(tip + (1.0,)), sign


# ----------------------------------------------------------------------------------------------
# Measuring
# ----------------------------------------------------------------------------------------------

def _rot(M):
    R = np.array(M[:3, :3], dtype=np.float64)
    n = np.linalg.norm(R, axis=0)
    return R / np.where(n < 1e-12, 1.0, n)


def _angle(R1, R2):
    c = (np.trace(R1.T @ R2) - 1.0) / 2.0
    return math.degrees(math.acos(max(-1.0, min(1.0, c))))


def _times(length, loop, extra=0.0):
    if length <= 0:
        return [0.0]
    end = length + (extra if loop == "loop" else 0.0)
    ts = [round(i * STEP, 6) for i in range(int(math.floor(end / STEP + 1e-9)) + 1)]
    if loop == "once" and ts[-1] >= length:
        ts[-1] = length - 1e-4      # a play-once clip has stopped at its length; take its last pose instead
    return ts


def lattice(cube, step=1.0, cap=25):
    """(u, v, w) fractions of a cube's x, y, z edges: a grid at most `step` px apart (corners included)."""
    n = [max(2, min(cap, int(math.ceil(abs(float(sz)) / step)) + 1)) for sz in cube.size]
    g = np.stack(np.meshgrid(*[np.linspace(0.0, 1.0, k) for k in n], indexing="ij"), axis=-1)
    return g.reshape(-1, 3)


def cube_points(corners, uvw):
    """Points of posed cubes: corners (8 x 3) in render_geo's order BLB, BRB, TLB, TRB, TLF, TRF, BLF, BRF."""
    c0 = corners[0]
    E = np.stack([corners[6] - c0, corners[2] - c0, corners[1] - c0])     # x, y, z edges
    return c0 + uvw @ E


def point_depth(points, corners, axes):
    """Deepest of `points` (N x 3) inside any box (corners B x 8 x 3, axes B x 3 x 3 columns): the distance from the
    point to the box's nearest face, 0 when no point is inside. Same units as the input."""
    if len(points) == 0 or len(corners) == 0:
        return 0.0
    c = corners.mean(axis=1)                                            # B x 3
    proj = np.einsum("bvc,bca->bva", corners - c[:, None], axes)       # B x 8 x 3
    half = np.abs(proj).max(axis=1)                                     # B x 3
    q = np.einsum("bpc,bca->bpa", points[None] - c[:, None], axes)     # B x N x 3
    d = (half[:, None, :] - np.abs(q)).min(axis=2)
    return float(max(0.0, d.max()))


def entry_depth(points, roots, corners, axes):
    """Deepest of `points` (N x 3) inside any box. Like point_depth (distance to the nearest face) except along the
    entry axis, where depth counts from the face the limb comes in through: the face on the side of the point's limb
    root (`roots`, N x 3: the elbow or knee, outside the body). So it does not stop at the box's centre plane: a
    forearm run front to back through a 4 px torso reads up to 4 px, while a hand grazing the torso's side reads
    only as deep as it is past that side."""
    if len(points) == 0 or len(corners) == 0:
        return 0.0
    c = corners.mean(axis=1)                                            # B x 3
    proj = np.einsum("bvc,bca->bva", corners - c[:, None], axes)
    half = np.abs(proj).max(axis=1)                                     # B x 3
    q = np.einsum("bpc,bca->bpa", points[None] - c[:, None], axes)     # B x N x 3
    qr = np.einsum("bpc,bca->bpa", roots[None] - c[:, None], axes)
    a = np.argmax(np.abs(qr) - half[:, None, :], axis=2)               # B x N: the axis the root is most outside on
    qa = np.take_along_axis(q, a[..., None], axis=2)[..., 0]
    sa = np.sign(np.take_along_axis(qr, a[..., None], axis=2)[..., 0])
    sa = np.where(sa == 0, 1.0, sa)
    ha = np.take_along_axis(np.broadcast_to(half[:, None, :], q.shape), a[..., None], axis=2)[..., 0]
    side = half[:, None, :] - np.abs(q)                                 # distance to each axis's nearer face
    np.put_along_axis(side, a[..., None], (ha - sa * qa)[..., None], axis=2)   # entry axis: from the entry face
    d = side.min(axis=2)
    return float(max(0.0, d.max()))


def _flex(Ru, Rl, knee):
    v = Ru.T @ Rl @ np.array([0.0, -1.0, 0.0])
    a = math.degrees(math.atan2(-v[2], -v[1]))       # gecko space: the model's front is -z
    return -a if knee else a


def _wrap(d):
    return (np.asarray(d) + math.pi) % (2.0 * math.pi) - math.pi


def loop_numbers(clip):
    """Across the wrap of a looping clip: (rotation gap deg, position gap px, kinks). kinks: per rotated bone
    (bone, kink, peak) in deg/tick at 1-tick finite differences (the game's update step): kink is the jump in rotation
    speed across the wrap, peak the bone's own largest 1-tick speed anywhere in the clip."""
    L = clip.length
    h = min(1.0, L / 2.0)
    rot = pos = 0.0
    kinks = []
    for b, chans in clip.bones.items():
        for ch in ("rotation", "position"):
            if not chans.get(ch):
                continue
            s = lambda t: np.asarray(clip.sample(b, ch, t), dtype=np.float64)  # noqa: E731
            gap = s(L) - s(0.0)
            if ch == "rotation":
                rot = max(rot, float(np.degrees(np.abs(_wrap(gap))).max()))
                v_end = _wrap(s(L) - s(L - h)) / h
                v_start = _wrap(s(h) - s(0.0)) / h
                kink = float(np.degrees(np.abs(v_end - v_start)).max())
                ts = np.arange(0.0, L - h + 1e-9, h)
                peak = max(float(np.degrees(np.abs(_wrap(s(t + h) - s(t)))).max()) / h for t in ts)
                kinks.append((b, kink, peak))
            else:
                pos = max(pos, float(np.abs(gap).max()))
    return rot, pos, kinks


def measure(rig, anim, name, base=(), speed=None, kind=None, variant=None, family=None):
    """Raw numbers of one clip played over its base clips. anim: parsed animation json (dict). A gait clip's root
    speed comes from the troop's booth config (`speed`); without one its foot slide is n/a."""
    kind = kind or clip_kind(name)
    try:
        clips = [rg.load_clip(anim, b) for b in base] + [rg.load_clip(anim, name)]
    except rg.GeoError as e:
        raise ScoreError("%s: %s" % (name, e))
    top = clips[-1]
    speed = float(speed or 0.0)
    scene = rg.Scene(rig.model, None, clips, speed if kind == "gait" else 0.0)
    ts = _times(top.length, top.loop, WINDOW)
    frames = [scene.evaluate(t) for t in ts]
    span = [i for i, t in enumerate(ts) if t <= top.length]
    raw, notes = {}, {}

    # 1. elbow and knee bend
    for metric, pairs, knee in (("elbow_bend", rig.elbows, False), ("knee_bend", rig.knees, True)):
        if not rig.biped or not pairs:
            notes[metric] = "no biped arms/legs" if not rig.biped else "no forearm/shin bones"
            continue
        vals = [_flex(_rot(frames[i].bone_M[u]), _rot(frames[i].bone_M[l]), knee) for i in span for u, l in pairs]
        raw[metric] = [round(min(vals), 2), round(max(vals), 2)]

    # 2. angular speed per group, over 2-tick windows (windows past the end of a loop wrap into its start)
    k = int(round(WINDOW / STEP))
    touched = set().union(*[c.touched() for c in clips])
    for g in GROUPS:
        metric = "ang_speed_" + g
        if g == "weapon":
            bones = [rig.blade_bone] if rig.blade_bone else []
        else:
            bones = [b for b in rig.groups[g] if b in touched]
        if not bones:
            notes[metric] = "no weapon" if g == "weapon" else "group not animated"
            continue
        best = 0.0
        for b in bones:
            p = rig.full.bones[b].parent
            Rs = []
            for fr in frames:
                R = _rot(fr.bone_M[b])
                if g != "weapon" and p is not None:
                    R = _rot(fr.bone_M[p]).T @ R
                Rs.append(R)
            for i in range(len(Rs) - k):
                best = max(best, _angle(Rs[i], Rs[i + k]))
        raw[metric] = round(best, 2)

    # 3. foot slide (render_geo's planted-foot drift)
    if kind == "gait" and speed <= 0:
        notes["foot_slide"] = "no booth gait speed for this troop"
    elif rig.feet:
        series = {f: [] for f in rig.feet}
        for i in span:
            for f in rig.feet:
                smp = rg.foot_sample(rig.feet_view, _View(corners=frames[i].corners[rig.foot_idx]), f)
                if smp is not None:
                    series[f].append((ts[i], smp[0], smp[1]))
        with contextlib.redirect_stderr(io.StringIO()):
            slide = rg.foot_slide(series, ground=0.0, step=STEP)
        if slide.get("max_drift") is None:
            notes["foot_slide"] = "no foot cubes"
        else:
            raw["foot_slide"] = round(float(slide["max_drift"]), 4)
    else:
        notes["foot_slide"] = "no foot bones"

    # 4. penetration
    for metric, sets, why in (("pen_weapon", rig.pen_weapon, "no weapon or held prop"),
                              ("pen_limb", rig.pen_limb, "no forearm/shin or torso cubes")):
        if not sets:
            notes[metric] = why
            continue
        movers, boxes = sets
        J = np.array(boxes)
        worst = 0.0
        for i in span:
            fr = frames[i]
            pts = np.concatenate([cube_points(fr.corners[mv[0]], mv[1]) for mv in movers])
            R = np.array([_rot(fr.cube_M[j]) for j in boxes])
            if metric == "pen_limb":
                piv = {r: scene.bone_pivot_world(fr, r) for r in {mv[2] for mv in movers}}
                roots = np.concatenate([np.repeat(piv[mv[2]][None], len(mv[1]), axis=0) for mv in movers])
                d = entry_depth(pts, roots, fr.corners[J], R)
            else:
                d = point_depth(pts, fr.corners[J], R)
            worst = max(worst, d * 16.0)
        raw[metric] = round(worst, 3)

    # 5 and 6. edge lead and impact tick (melee attacks with a design variant)
    is_attack = kind == "attack"
    vkind = variant.get("kind") if variant else None
    if not is_attack:
        notes["edge_lead"] = notes["impact_tick"] = "not an attack"
    elif not rig.blade_bone:
        notes["edge_lead"] = notes["impact_tick"] = "no weapon"
    elif variant is None:
        notes["edge_lead"] = notes["impact_tick"] = "no design.json attack variant"
    else:
        tip = lambda fr: (fr.bone_M[rig.blade_bone] @ rig.tip)[:3] * 16.0  # noqa: E731
        imp = float(variant["impactTicks"])
        if vkind == "slash" and imp <= top.length:
            a, b = scene.evaluate(imp - 0.25), scene.evaluate(min(imp, top.length - 1e-4))
            v = tip(b) - tip(a)
            Rm = _rot(b.bone_M[rig.blade_bone])
            Lx, E = Rm @ np.array([0.0, 1.0, 0.0]), Rm @ np.array([rig.edge_sign or 1.0, 0.0, 0.0])
            vp = v - (v @ Lx) * Lx
            if np.linalg.norm(vp) < 1e-9:
                raw["edge_lead"] = 90.0
            else:
                th = abs(math.degrees(math.atan2(float(np.cross(E, vp) @ Lx), float(E @ vp))))
                raw["edge_lead"] = round(th if rig.edge_sign else min(th, 180.0 - th), 2)
        else:
            notes["edge_lead"] = "not a slash" if vkind != "slash" else "impact after the clip ends"
        if vkind in MELEE:
            # contact, not wind-up: the 2-tick window in which the tip drives furthest toward the foe (world +z,
            # the model's front); a wind-up or a haft drawn back moves the tip away from the foe and never wins
            P = np.array([tip(frames[i]) for i in span])
            T = np.array([ts[i] for i in span])
            if len(P) > k:
                drive = P[k:, 2] - P[:-k, 2]
                j = int(np.argmax(drive))
                raw["impact_tick"] = {"peak": round(float((T[j] + T[j + k]) / 2.0), 2), "design": imp}
            else:
                notes["impact_tick"] = "clip shorter than 2 ticks"
        else:
            notes["impact_tick"] = "ranged/magic attack: no melee contact"

    # 7. loop continuity
    loops = [c for c in clips if c.loop == "loop" and c.length > 0]
    if top.loop != "loop" or not loops:
        for m in ("loop_pose", "loop_pos", "loop_vel"):
            notes[m] = "not looping"
    else:
        nums = [loop_numbers(c) for c in loops]
        raw["loop_pose"] = round(max(n[0] for n in nums), 3)
        raw["loop_pos"] = round(max(n[1] for n in nums), 3)
        kinks = [kk for n in nums for kk in n[2]]
        if kinks:
            # the knight's ratios come from every bone; a bone can only fail with a kink over the floor
            ratio = max((kk[1] / kk[2] for kk in kinks if kk[1] >= 0.01 and kk[2] > 1e-6), default=0.0)
            big = sorted((kk for kk in kinks if kk[1] >= 1.0), key=lambda kk: -kk[1])
            raw["loop_vel"] = {"kink": round(max(kk[1] for kk in kinks), 3), "ratio": round(ratio, 4),
                               "bones": [[b, round(kv, 3), round(pv, 3)] for b, kv, pv in big]}
        else:
            notes["loop_vel"] = "no rotation channels"

    if not rig.biped:
        for m in BIPED_METRICS:
            if m in raw:
                del raw[m]
                notes[m] = "not a biped: the knight's limits do not apply"
    return {"name": name, "base": list(base), "kind": kind, "family": family,
            "speed": speed if kind == "gait" else 0.0,
            "length": top.length, "loop": top.loop, "raw": raw, "notes": notes}


# ----------------------------------------------------------------------------------------------
# Judging
# ----------------------------------------------------------------------------------------------

def limit_for(limits, metric, kind, family=None):
    """The knight-derived limit for a clip kind, widened by the troop family's stated allowance when there is one
    (family allowances only ever widen; a range's minimum keeps the anatomical cap)."""
    m = limits.get("metrics", {}).get(metric)
    if not m:
        return None
    lim = m.get("limit", {})
    if metric in GLOBAL_METRICS:
        return lim.get("all")
    base = lim.get(kind, lim.get("other"))
    fam = limits.get("families", {}).get(family or "", {}).get(metric, {})
    wide = fam.get(kind, fam.get("all"))
    if base is None or wide is None:
        return base
    if metric in RANGE_METRICS:
        cap = m.get("floor_lo", HYPER)
        return [round(min(base[0], max(wide[0], cap)), 2), round(max(base[1], wide[1]), 2)]
    return round(max(base, wide), 4)


def judge(meas, limits):
    out = []
    for metric in METRICS:
        unit = UNITS[metric]
        row = {"id": metric, "unit": unit, "status": "n/a", "value": None, "limit": None}
        if metric not in meas["raw"]:
            row["note"] = meas["notes"].get(metric, "")
            out.append(row)
            continue
        val = meas["raw"][metric]
        lim = limit_for(limits, metric, meas["kind"], meas.get("family"))
        if lim is None:
            row.update(value=val, note="no knight limit for this metric")
            out.append(row)
            continue
        if metric in RANGE_METRICS:
            ok = lim[0] - 1e-9 <= val[0] and val[1] <= lim[1] + 1e-9
        elif metric == "loop_vel":
            floor = limits["metrics"][metric].get("floor", LOOP_VEL_FLOOR)
            worst, kv, lv, at = -1.0, val["kink"], floor, None
            for b, kink, peak in val["bones"]:
                lb = max(floor, lim * peak)
                if kink / lb > worst:
                    worst, kv, lv, at = kink / lb, kink, lb, (b, peak)
            ok = kv <= lv + 1e-9
            row["note"] = "limit = max(%s, %s x the bone's peak speed)" % (_fmt(float(floor)), _fmt(float(lim)))
            if at:
                row["note"] = "%s, peak %s deg/tick; %s" % (at[0], _fmt(float(at[1])), row["note"])
            val, lim = kv, round(lv, 3)
        elif metric == "impact_tick":
            off = val["peak"] - val["design"]
            ok = abs(off) <= lim + 1e-9
            row["design"] = val["design"]
            val = val["peak"]
            lim = [round(row["design"] - lim, 2), round(row["design"] + lim, 2)]
        else:
            ok = val <= lim + 1e-9
        row.update(status="pass" if ok else "fail", value=val, limit=lim)
        out.append(row)
    return out


def _within(val, cap):
    """An approved clip keeps its approval only while it is no worse than the value that was approved."""
    if isinstance(val, (int, float)) and isinstance(cap, (int, float)):
        return val <= cap + 1e-3
    if isinstance(val, (list, tuple)) and isinstance(cap, (list, tuple)) and len(val) == len(cap):
        return all(isinstance(a, (int, float)) and isinstance(b, (int, float)) and abs(a - b) <= 1e-3 for a, b in zip(val, cap))
    return False


def score(meas, limits, approved=None):
    """approved: {metric id: entry} for this clip (scorecard_approved.json). A metric over its limit that the owner approved
    as it is stays "fail" with the entry's reason under "approved", and does not fail the clip while its value is no worse."""
    metrics = judge(meas, limits)
    for m in metrics:
        e = (approved or {}).get(m["id"])
        if e and m["status"] == "fail" and _within(m["value"], e.get("value")):
            m["approved"] = e.get("reason", "")
    return {"name": meas["name"], "base": meas["base"], "kind": meas["kind"], "speed": meas["speed"],
            "pass": all(m["status"] != "fail" or "approved" in m for m in metrics), "metrics": metrics}


def failures(scored):
    """Readable lines for every failing metric: clip, metric, value, limit."""
    out = []
    for c in scored:
        for m in c["metrics"]:
            if m["status"] == "fail" and "approved" not in m:
                out.append("%s: %s = %s %s, limit %s" % (c["name"], m["id"], _fmt(m["value"]), m["unit"], _fmt(m["limit"])))
    return out


def approvals(scored):
    """Readable lines for every metric over its limit that the owner approved as it is."""
    return ["%s: %s = %s %s, limit %s (%s)" % (c["name"], m["id"], _fmt(m["value"]), m["unit"], _fmt(m["limit"]), m["approved"])
            for c in scored for m in c["metrics"] if m["status"] == "fail" and "approved" in m]


def _fmt(v):
    if isinstance(v, list):
        return "[%s]" % ", ".join(_fmt(x) for x in v)
    if isinstance(v, float):
        return ("%.3f" % v).rstrip("0").rstrip(".")
    return str(v)


# ----------------------------------------------------------------------------------------------
# Troops
# ----------------------------------------------------------------------------------------------

def _read(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def load_limits(path=LIMITS_PATH):
    if not os.path.isfile(path):
        raise ScoreError("no limits file %s; run scorecard.py --derive" % path)
    return _read(path)


def load_approved(path=None):
    """{(troop, clip): {metric id: entry}} from scorecard_approved.json: clips the owner approved exactly as they are."""
    path = path or APPROVED_PATH
    out = {}
    if os.path.isfile(path):
        for e in _read(path).get("approved", []):
            out.setdefault((e["troop"], e["clip"]), {})[e["metric"]] = e
    return out


def renderer_troops(repo=REPO):
    with open(os.path.join(repo, RENDERER), encoding="utf-8") as f:
        src = f.read()
    # every GeckoLib troop: the approved ones and the previews (TROOPS before the per-troop approval switch)
    sets = re.findall(r"Set<String>\s+(?:TROOPS|APPROVED|PREVIEW)\s*=\s*Set\.of\(([^)]*)\)", src)
    if not sets:
        raise ScoreError("no TROOPS, APPROVED or PREVIEW set in %s" % RENDERER)
    names = list(dict.fromkeys(n for body in sets for n in re.findall(r'"([A-Za-z0-9_]+)"', body)))
    return [t for t in names if os.path.isfile(os.path.join(repo, ASSETS, "animations", "troop", t + ".animation.json"))]


def rig_rules_for(repo, geo_rel):
    """troop_rig/<troop>.json beside the geo folder (assets/<ns>/geo/troop/x.geo.json -> assets/<ns>/troop_rig/x.json)."""
    parts = geo_rel.replace("\\", "/").split("/")
    if "geo" not in parts:
        return None
    i = len(parts) - 1 - parts[::-1].index("geo")
    troop = parts[-1].split(".")[0]
    p = os.path.join(repo, *parts[:i], "troop_rig", troop + ".json")
    return _read(p) if os.path.isfile(p) else None


class Design:
    def __init__(self, repo):
        p = os.path.join(repo, DESIGN)
        d = _read(p) if os.path.isfile(p) else {}
        self.family = {t["id"]: t.get("family") for t in d.get("troops", []) if isinstance(t, dict) and "id" in t}
        self.variants = {v["id"]: v for v in d.get("attackVariants", []) if isinstance(v, dict) and "id" in v}

    def variant(self, troop, clip):
        s = stem(clip)
        fam = self.family.get(troop)
        if not fam or not s.startswith("attack_"):
            return None
        return self.variants.get(fam + "_" + s[len("attack_"):])


def booth_config_for(repo, troop):
    from booth_config import load_config
    hits = sorted(glob.glob(os.path.join(repo, "art", "anim", "*", "booth.%s.json" % troop)))
    return load_config(hits[0]) if hits else None


def measure_troop(repo, troop, geo=None, anim=None, config=None, only=None):
    geo = geo or "%s/geo/troop/%s.geo.json" % (ASSETS, troop)
    anim = anim or "%s/animations/troop/%s.animation.json" % (ASSETS, troop)
    model = rg.load_geo(os.path.join(repo, geo))
    data = _read(os.path.join(repo, anim))
    rig = Rig(model, rig_rules_for(repo, geo), troop)
    design = Design(repo)
    if config is None:
        config = booth_config_for(repo, troop)
    names = list(data.get("animations", {}))
    out = []
    for name, base, speed in units(names, config.clips if config else None):
        if only is not None and name not in only:
            continue
        out.append(measure(rig, data, name, base, speed, variant=design.variant(troop, name),
                           family=design.family.get(troop)))
    return out


def score_troop(repo, troop, limits, approved=None, **kw):
    approved = load_approved() if approved is None else approved
    return [score(m, limits, approved.get((troop, m["name"]))) for m in measure_troop(repo, troop, **kw)]


def score_booth(repo, cfg, names, limits=None, approved=None):
    """Scorecards of the named clips of a booth config, keyed by clip name (the booth build gate)."""
    limits = limits or load_limits()
    approved = load_approved() if approved is None else approved
    model = rg.load_geo(os.path.join(repo, *cfg.source.geo.split("/")))
    data = _read(os.path.join(repo, *cfg.source.anim.split("/")))
    rig = Rig(model, rig_rules_for(repo, cfg.source.geo), cfg.troop)
    design = Design(repo)
    by = {c.name: c for c in cfg.clips}
    out = {}
    for n in names:
        c = by[n]
        meas = measure(rig, data, n, c.base, c.speed if c.speed > 0 else None, variant=design.variant(cfg.troop, n),
                       family=design.family.get(cfg.troop))
        out[n] = score(meas, limits, approved.get((cfg.troop, n)))
    return out


# ----------------------------------------------------------------------------------------------
# Deriving limits from the knight
# ----------------------------------------------------------------------------------------------

def derive(repo=REPO, measured=None):
    meas = measured if measured is not None else measure_troop(repo, KNIGHT)
    metrics = {}
    for metric in METRICS:
        mg = MARGINS[metric]
        per = {}
        for m in meas:
            if metric not in m["raw"]:
                continue
            v = m["raw"][metric]
            key = "all" if metric in GLOBAL_METRICS else m["kind"]
            if metric == "impact_tick":
                v = abs(v["peak"] - v["design"])
            elif metric == "loop_vel":
                v = v["ratio"]
            if metric in RANGE_METRICS:
                lo, hi = per.get(key, [v[0], v[1]])
                per[key] = [min(lo, v[0]), max(hi, v[1])]
            else:
                per[key] = max(per.get(key, v), v)
        if not per:
            continue
        if metric not in GLOBAL_METRICS:
            vals = list(per.values())
            per["other"] = ([min(v[0] for v in vals), max(v[1] for v in vals)] if metric in RANGE_METRICS
                            else max(vals))

        def widen(k, v):
            if metric in RANGE_METRICS:
                return [round(min(v[0], mg["floor_lo"]), 2), round(v[1] + mg["add"], 2)]
            if metric == "loop_vel":
                return round(v * mg["scale"], 4)       # a ratio; the deg/tick floor is applied when judging
            out = v * mg.get("kind_scale", {}).get(k, mg.get("scale", 1.0)) + mg.get("add", 0.0)
            return round(max(out, mg.get("floor", 0.0)), 4)
        metrics[metric] = {"unit": "ratio" if metric == "loop_vel" else UNITS[metric], "how": mg["how"],
                           "knight": {k: (v if metric in RANGE_METRICS else round(v, 4)) for k, v in per.items()},
                           "limit": {k: widen(k, v) for k, v in per.items()}}
        for key in ("floor", "floor_lo"):
            if key in mg:
                metrics[metric][key] = mg[key]
    return {
        "_comment": "Animation scorecard limits (tools/anim/scorecard.py). Every limit is the approved Knight v2 clips' "
                    "own extreme for that metric and clip kind plus the stated margin; 'other' is the envelope over the "
                    "knight's kinds and applies to clip kinds the knight does not have. The knight is the sword_shield "
                    "family; 'families' widens a limit for another design.json family only where its style needs the "
                    "motion, each with its reason (it never narrows one, and never lifts the anatomical caps). "
                    "Non-biped rigs are n/a for bend, limb speed and foot slide; a gait's foot slide needs the troop's "
                    "booth gait speed. Regenerate with "
                    "python tools/anim/scorecard.py --derive.",
        "derived_from": {"animation": "%s/animations/troop/knight.animation.json" % ASSETS,
                         "geo": "%s/geo/troop/knight.geo.json" % ASSETS, "clips": len(meas)},
        "kinds": {"attack": "attack_*", "gait": "walk, run, charge_run", "fallback": "stance_fallback",
                  "stance": "stance_*, idle",
                  "hit": "hit_*, stun*", "death": "death_*, fake_death, rise", "other": "anything else"},
        "edge_sign": EDGE_SIGN,
        "metrics": metrics,
        "families": FAMILIES,
    }


# ----------------------------------------------------------------------------------------------
# CLI
# ----------------------------------------------------------------------------------------------

def summary(results):
    lines = ["%-15s %5s %5s  %s" % ("troop", "clips", "fail", "failures by metric")]
    for troop, clips in results.items():
        by = {}
        for c in clips:
            for m in c["metrics"]:
                if m["status"] == "fail" and "approved" not in m:
                    by[m["id"]] = by.get(m["id"], 0) + 1
        lines.append("%-15s %5d %5d  %s" % (troop, len(clips), sum(not c["pass"] for c in clips),
                                            ", ".join("%s %d" % kv for kv in sorted(by.items())) or "-"))
    return "\n".join(lines)


def main(argv=None):
    p = argparse.ArgumentParser(description="Score GeckoLib troop clips against limits derived from the approved knight.")
    p.add_argument("--repo", default=REPO)
    g = p.add_mutually_exclusive_group(required=True)
    g.add_argument("--troop", help="troop id, e.g. knight")
    g.add_argument("--all", action="store_true", help="every GeoTroopRenderer troop with an animation json")
    g.add_argument("--derive", action="store_true", help="recompute %s from the knight" % os.path.relpath(LIMITS_PATH, REPO))
    p.add_argument("--json", help="write the full results here")
    p.add_argument("--limits", default=LIMITS_PATH)
    a = p.parse_args(argv)
    try:
        if a.derive:
            lim = derive(a.repo)
            with open(a.limits, "w", encoding="utf-8", newline="\n") as f:
                json.dump(lim, f, indent=1)
                f.write("\n")
            print("wrote %s from %d knight clips" % (a.limits, lim["derived_from"]["clips"]))
            return 0
        limits = load_limits(a.limits)
        troops = renderer_troops(a.repo) if a.all else [a.troop]
        results = {t: score_troop(a.repo, t, limits) for t in troops}
    except (ScoreError, rg.GeoError, OSError, ValueError) as e:
        print("error: %s" % e, file=sys.stderr)
        return 2
    if a.json:
        os.makedirs(os.path.dirname(os.path.abspath(a.json)), exist_ok=True)
        with open(a.json, "w", encoding="utf-8", newline="\n") as f:
            json.dump({"date": datetime.date.today().isoformat(), "limits": a.limits,
                       "troops": results}, f, indent=1)
            f.write("\n")
    print(summary(results))
    bad = [line for t, cl in results.items() for line in failures(cl)]
    if not a.all:
        for line in bad:
            print("FAIL " + line)
        for line in [line for cl in results.values() for line in approvals(cl)]:
            print("APPROVED " + line)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
