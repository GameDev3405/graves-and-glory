"""The Hollow King's polish pass (batch review 2026-11-11): what his clips needed after the four-focus-point check, applied to
the finished animation document (doc level, his clips only; every other troop's clips are never touched).

  rags       The three rag strips hang from the hips in front of the thighs as rigid planes. A thigh or knee that comes
             forward passed straight through them (armour_clip 2.2-2.5 px in every gait, stance, attack and hit). They are
             cloth: every sample turns them forward about their hip hinge by the least angle that keeps them on the front of
             the legs and off the ground, and that need is spread into a soft rise and a slower fall (a cloth lag, never a
             snap), periodic in the loops. Keyed on "rags" in the _legs clip (additive hit legs: only what the stance under
             them does not already give).
  soft       cloak_a/b/c and both pauldrons stood still in his attacks, heavy hits and stuns: fullbody.soft_motion (damped
             spring per cloak, pauldrons following 70% of the arm, at most 10 degrees behind) on the pose the game shows
             (an additive hit over the guard). tine_r/tine_l share their pauldron's hinge and take its keys.
  weapon     The greatsword tip went into the ground in the cleave's follow-through (5 px, t14.5) and in the stun (4 px):
             the sword arm is lifted by the least angle that keeps every blade cube on the ground line, eased in and out.
  floor      A knee spike or boot under the ground (run t1, stance_wall t9.5, stance_fallback t4.25, knockback t11, pulled
             t1): the hips are raised by the least amount, the planted legs re-solved to keep the ankle and the foot's turn
             (a boot that was under the ground is set on it), eased in and out. Never the whole clip. A leg's re-solve fades
             over PLANT_EASE samples as its boot leaves the ground (a hard switch snapped the shin 18 degrees in a sample).
  cuisse     A thigh lifted past ~60 degrees sank its plate into the hip block (run, side cut, cleave, pulled; 1.9-2.3 px):
             the plate slides down its own thigh by the least amount (at most 2.5 px).
  thigh      The run lifted each knee to 81 degrees and drove the thigh into the hip block (2.1 px): the thigh lifts by the
             least angle less at the peak (15.5 degrees, so about 65) with the shin keeping its angle to the world. A
             heavier stride, same timing.
  edge       The cleave landed 14 degrees off its edge at the impact (scorecard edge_lead, limit 13.5): the sword hand rolls
             2 degrees about its y over three ticks each side of the impact.
  head       The knockback threw the head back into the spine fan (2.0 px): tipped back less, at most 14 degrees, eased.

Every fix is a constraint (re-running it on its own output changes nothing). Timing, length, the impact marker and the
timeline markers are kept; channels it does not fix keep their keys.

Built clips: art/anim/knight/build_anims.py (GG_ANIM_TROOP=gb_hollowking) calls polish(doc, BUILT) before writing out/.
Late clips (made in the exported asset by art/anim/common/stances.py, reactions.py and charge.py): after that chain run
    python art/anim/gb_hollowking/hk_polish.py
which polishes the LATE clips of the exported animation file in place.
"""
from __future__ import annotations

import json
import math
import os
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
TROOP = "gb_hollowking"
for _p in (ROOT / "tools" / "anim", ROOT / "art" / "anim" / "common", ROOT / "art" / "anim" / "knight"):
    if str(_p) not in sys.path:
        sys.path.insert(0, str(_p))
os.environ.setdefault("GG_ANIM_TROOP", TROOP)
ASSET = ROOT / "src/main/resources/assets/gravesandglory/animations/troop" / f"{TROOP}.animation.json"

SUB, TPS = 4, 20.0
Z3 = (0.0, 0.0, 0.0)
RAG_CLEAR = 1.0      # px a rag may lie in a leg beyond its rest fit (focus_check armour_clip warns at 1.7)
RAG_RATE = 1.6       # degrees per sample (a quarter tick) the rags rise at most; they fall at RAG_FALL
RAG_FALL = 0.9
RAG_MAX = 70.0
RAG_FOLDS = (1.0, 0.85, 0.7, 0.55)   # how short the strips may bunch, tried in turn
FOLD_RATE = 0.03     # per sample
PLATE_CLEAR = 1.2    # px a plate or the head may lie in the body beyond its rest fit (armour_clip warns at 1.7)
CUISSE_SLIDE = 2.5   # px a thigh plate slides down its thigh at most
SLIDE_RATE = 0.15    # px per sample
HEAD_MAX = 25.0
THIGH_CLEAR = 1.5    # px a thigh may lie in the hip block beyond its rest fit (armour_clip warns at 1.7)
THIGH_MAX = 20.0     # degrees a thigh lifts less at most
EDGE_LEAD = 12.5     # degrees the edge may lie off the blade's motion at the impact (scorecard limit 13.53)
EDGE_MAX = 10.0      # degrees the sword hand may roll at most
EDGE_WINDOW = 3.0    # ticks each side of the impact the roll eases over
FLOOR_PAD = 0.3      # px over the ground line a fixed knee, boot or blade is left at
LIFT_RATE = 0.25     # px per sample the hips rise or settle at most (a tick: 1 px)
RAISE_RATE = 0.6     # px per sample a boot rises off the ground at most where an additive clip starts
PLANT_EASE = 6      # samples each side over which a leg's re-solve fades as its boot leaves or meets the ground
ARM_RATE = 1.2       # degrees per sample the sword arm lifts or settles at most
SOFT_BONES = ("cloak_a", "cloak_b", "cloak_c", "left_pauldron", "right_pauldron", "tine_l", "tine_r")
TINES = {"right_pauldron": "tine_r", "left_pauldron": "tine_l"}

# clip -> (paired upper or legs clip, additive base clips frozen at a tick or None). The additive clips (HitController.add
# sums their euler onto the stance) are judged and fixed on the pose the game shows.
HITS = ["hit_light_front", "hit_light_back", "hit_light_right", "hit_light_left", "hit_medium_front", "hit_medium_back",
        "hit_medium_right", "hit_medium_left", "hit_block"]
READY = ("stance_ready_upper", "stance_ready_legs", 0.0)
BUILT = {
    "floor": [("run_legs", "run_upper", None), ("stance_wall_legs", "stance_wall_upper", None)],
    "rags": [("walk_legs", "walk_upper", None), ("run_legs", "run_upper", None),
             ("attack_overhead_cleave_legs", "attack_overhead_cleave", None),
             ("attack_side_cut_legs", "attack_side_cut", None), ("attack_thrust_legs", "attack_thrust", None),
             ("stance_ready_legs", "stance_ready_upper", None), ("stance_brace_legs", "stance_brace_upper", None),
             ("stance_wall_legs", "stance_wall_upper", None)] + [(h + "_legs", h, READY) for h in HITS],
    "weapon": [("attack_overhead_cleave", "attack_overhead_cleave_legs", None)],
    "cuisse": [("run_legs", "run_upper", None), ("attack_side_cut_legs", "attack_side_cut", None),
               ("attack_overhead_cleave_legs", "attack_overhead_cleave", None)],
    "thigh": [("run_legs", "run_upper", None)],
    "edge": [("attack_overhead_cleave", "attack_overhead_cleave_legs", None)],
    "soft": [("attack_overhead_cleave", "attack_overhead_cleave_legs", None),
             ("attack_side_cut", "attack_side_cut_legs", None), ("attack_thrust", "attack_thrust_legs", None)]
            + [(h, h + "_legs", READY) for h in HITS if "light" not in h],
}
STUN = ("stun_upper", None, 4.0 / TPS)    # stun_out adds onto the stun's upper body where its loop starts; legs play on
LATE = {
    "floor": [("stance_fallback_legs", "stance_fallback_upper", None), ("hit_knockback_legs", "hit_knockback", READY),
              ("hit_pulled_legs", "hit_pulled", READY)],
    "rags": [("stance_hold_legs", "stance_hold_upper", None), ("stance_fallback_legs", "stance_fallback_upper", None),
             ("stun_legs", "stun_upper", None), ("hit_knockback_legs", "hit_knockback", READY),
             ("hit_pulled_legs", "hit_pulled", READY)],
    "weapon": [("stun_upper", "stun_legs", None), ("stun_out_upper", "stun_legs", STUN)],
    "cuisse": [("hit_pulled_legs", "hit_pulled", READY)],
    "head": [("hit_knockback", "hit_knockback_legs", READY)],
    "soft": [("hit_pulled", "hit_pulled_legs", READY), ("hit_knockback", "hit_knockback_legs", READY),
             ("stun_upper", "stun_legs", None), ("stun_out_upper", "stun_legs", STUN)],
}
STUN_LOOP = (4.0, 24.0)     # reactions.py: the stun clip plays its entry once, then loops ticks 4-24


def _B():
    """The knight builder module (its forward kinematics and bake): the running builder when it calls us, else imported."""
    m = sys.modules.get("build_anims")
    if m is None:
        main = sys.modules.get("__main__")
        if getattr(main, "HK", False) and hasattr(main, "bake"):
            sys.modules["build_anims"] = main
            m = main
        else:
            import build_anims as m  # noqa: E402
    return m


# ------------------------------------------------------------------------------------------------ document channels

def _keys(ch):
    if not isinstance(ch, dict):
        return [(0.0, [float(x) for x in ch])]
    return sorted((float(t), [float(x) for x in v]) for t, v in ch.items())


def _at(ks, t):
    if t <= ks[0][0]:
        return list(ks[0][1])
    for (t0, v0), (t1, v1) in zip(ks, ks[1:]):
        if t <= t1:
            f = (t - t0) / (t1 - t0) if t1 > t0 else 1.0
            return [a + (b - a) * f for a, b in zip(v0, v1)]
    return list(ks[-1][1])


def clip_pose(clip, sec):
    """{bone: (rot, pos|None)} of a clip at a time (seconds), in the builder's own sample form."""
    pose = {}
    for b, chans in clip["bones"].items():
        rot = _at(_keys(chans["rotation"]), sec) if "rotation" in chans else list(Z3)
        pos = _at(_keys(chans["position"]), sec) if "position" in chans else None
        pose[b] = (tuple(rot), tuple(pos) if pos is not None else None)
    return pose


def add_pose(base, top):
    out = dict(base)
    for b, (r, p) in top.items():
        if b in out:
            r0, p0 = out[b]
            pos = None if p is None and p0 is None else tuple(a + c for a, c in zip(p0 or Z3, p or Z3))
            out[b] = (tuple(a + c for a, c in zip(r0, r)), pos)
        else:
            out[b] = (r, p)
    return out


def _ticks(clip):
    return float(clip["animation_length"]) * TPS


def _n(clip):
    return int(round(_ticks(clip) * SUB))


def _ft(sec):
    return ("%.4f" % sec).rstrip("0").rstrip(".") if sec else "0.0"


class Ctx:
    """One clip and its partner, with the additive base under them (frozen at a tick)."""

    def __init__(self, anims, name, partner, base):
        self.anims, self.name, self.partner = anims, name, partner
        self.clip, self.pair = anims[name], anims.get(partner)
        self.base = None
        if base is not None:
            bu, bl, t0 = base
            self.base = clip_pose(anims[bu], t0)
            if bl is not None:
                self.base = add_pose(clip_pose(anims[bl], t0), self.base)
        self.loop = self.clip.get("loop") is True

    def pose(self, sec, strip=()):
        p = clip_pose(self.clip, sec)
        if self.pair is not None:
            p = add_pose(p, clip_pose(self.pair, min(sec, float(self.pair["animation_length"]))))
        if self.base is not None:
            p = add_pose(self.base, p)
        for b in strip:
            p.pop(b, None)
        return p

    def base_of(self, bone):
        if self.base is None or bone not in self.base:
            return (Z3, None)
        return self.base[bone]


def write_channel(clip, bone, chan, vals, keep_outside=True):
    """Sets a channel's values at the given times (seconds -> list), keeping every key outside them."""
    ch = clip["bones"].setdefault(bone, {}).get(chan)
    out = {} if ch is None or not keep_outside else {float(t): list(v) for t, v in ch.items()}
    if ch is None and keep_outside:
        out[0.0] = [0.0, 0.0, 0.0]
        out[float(clip["animation_length"])] = [0.0, 0.0, 0.0]
    for t, v in vals.items():
        out = {k: w for k, w in out.items() if abs(k - t) > 1e-6}
        out[t] = v
    clip["bones"][bone][chan] = {_ft(t): [round(float(x), 4 if chan == "position" else 3) for x in out[t]]
                                 for t in sorted(out)}


# ------------------------------------------------------------------------------------------------ the full rig

class Rig:
    def __init__(self):
        import render_geo as rg
        import scorecard as sc
        import focus_check as fc
        self.rg, self.sc = rg, sc
        geo = "%s/geo/troop/%s.geo.json" % (sc.ASSETS, TROOP)
        rig = sc.Rig(rg.load_geo(str(ROOT / geo)), sc.rig_rules_for(str(ROOT), geo), TROOP)
        self.model = m = rig.model        # focus_check's lean copy: the cubes the game can show
        P = fc.Parts(rig)
        self.bone_of = list(m.cube_bone)
        self.lat = P.lat
        self.hard = P.hard()
        self.weapon = P.idx(["weapon"])
        keep = [i for i in range(len(self.bone_of)) if P.part[i] not in ("weapon", "prop")]
        self.rags = [i for i in keep if self.bone_of[i] == "rags"]
        self.others = [i for i in keep if self.bone_of[i] != "rags"]

        def under(root):
            out = set()
            for n in m.order:
                q = n
                while q is not None and q != root:
                    q = m.bones[q].parent
                if q == root:
                    out.add(n)
            return out
        hard = set(self.hard)
        self.foot = {s: [i for i in hard if self.bone_of[i] in under(s + "_foot")] for s in ("right", "left")}
        self.leg = {s: [i for i in hard if self.bone_of[i] in under(s + "_leg") and i not in self.foot[s]]
                    for s in ("right", "left")}
        rest = self.fk({})
        self.ground = min(0.0, float(rest[1][self.hard][:, :, 1].min()) * 16.0)     # px: focus_check's floor line
        self.rest_fit = self._rag_fits(rest[1], rest[2], rest[1][self.rags], rest[2][self.rags])
        self.full = rig.full
        self._pieces = {}
        self.sign = self._rag_sign(rest)

    def local(self, name, rot, pos):
        rg = self.rg
        b = self.model.bones[name]
        r = b.rot + np.radians([-rot[0], -rot[1], rot[2]])
        pv = b.pivot / 16.0
        pos = pos or Z3
        return (rg._T(-pos[0] / 16.0, pos[1] / 16.0, pos[2] / 16.0) @ rg._T(*pv)
                @ rg._Rz(r[2]) @ rg._Ry(r[1]) @ rg._Rx(r[0]) @ rg._T(*(-pv)))

    def _cubes(self, M, idx):
        rg = self.rg
        C, R = [], []
        for ci in idx:
            cube = self.model.cube_list[ci]
            cp = cube.pivot / 16.0
            Mc = M[self.bone_of[ci]] @ rg._T(*cp) @ rg._Rz(cube.rot[2]) @ rg._Ry(cube.rot[1]) @ rg._Rx(cube.rot[0]) @ rg._T(*(-cp))
            C.append(cube.corners @ Mc[:3, :3].T + Mc[:3, 3])
            R.append(self.sc._rot(Mc))
        return np.array(C), np.array(R)

    def fk(self, pose):
        """(bone matrices, cube corners n x 8 x 3 in blocks, cube axes n x 3 x 3) of the full rig in a builder pose."""
        M = {}
        for n in self.model.order:
            b = self.model.bones[n]
            r, p = pose.get(n, (Z3, None))
            M[n] = (M[b.parent] if b.parent is not None else self.rg.WORLD_FLIP) @ self.local(n, r, p)
        C, R = self._cubes(M, range(len(self.bone_of)))
        return M, C, R

    # rags ---------------------------------------------------------------------------------------
    def rag_boxes(self, M, theta, fold=1.0):
        rg = self.rg
        Mr = dict(M)
        L = self.local("rags", (theta, 0.0, 0.0), None)
        if fold != 1.0:      # scaled along the strips about their hinge (the render applies scale after the turn)
            pv = self.model.bones["rags"].pivot / 16.0
            L = L @ rg._T(*pv) @ rg._S(1.0, fold, 1.0) @ rg._T(*(-pv))
        Mr["rags"] = M[self.model.bones["rags"].parent] @ L
        return self._cubes(Mr, self.rags)

    def _rag_fits(self, C, R, RC, RR):
        sc = self.sc
        out = {}
        lo, hi = C.min(axis=1), C.max(axis=1)
        for k in range(len(self.rags)):
            a = RC[k]
            alo, ahi = a.min(axis=0), a.max(axis=0)
            pa = sc.cube_points(a, self.lat[self.rags[k]])
            for j in self.others:
                if not (np.all(alo <= hi[j]) and np.all(lo[j] <= ahi)):
                    continue
                d = 0.0
                for pts, box, Rm in ((pa, C[j], R[j]), (sc.cube_points(C[j], self.lat[j]), a, RR[k])):
                    c = box.mean(axis=0)
                    half = np.abs((box - c) @ Rm).max(axis=0)
                    d = max(d, float((half - np.abs((pts - c) @ Rm)).min(axis=1).max()) * 16.0)
                if d > 0:
                    b = self.bone_of[j]
                    out[b] = max(out.get(b, 0.0), d)
        return out

    def fits_of(self, C, R, mov, MC, MR, oth):
        """{other bone: px} the moving cubes (posed as MC/MR) and the other cubes lie in each other."""
        sc = self.sc
        out = {}
        lo, hi = C.min(axis=1), C.max(axis=1)
        for k, i in enumerate(mov):
            a = MC[k]
            alo, ahi = a.min(axis=0), a.max(axis=0)
            pa = sc.cube_points(a, self.lat[i])
            for j in oth:
                if not (np.all(alo <= hi[j]) and np.all(lo[j] <= ahi)):
                    continue
                d = 0.0
                for pts, box, Rm in ((pa, C[j], R[j]), (sc.cube_points(C[j], self.lat[j]), a, MR[k])):
                    c = box.mean(axis=0)
                    half = np.abs((box - c) @ Rm).max(axis=0)
                    d = max(d, float((half - np.abs((pts - c) @ Rm)).min(axis=1).max()) * 16.0)
                if d > 0:
                    b = self.bone_of[j]
                    out[b] = max(out.get(b, 0.0), d)
        return out

    def subtree(self, root):
        out = set()
        for n in self.model.order:
            q = n
            while q is not None and q != root:
                q = self.model.bones[q].parent
            if q == root:
                out.add(n)
        return out

    def piece(self, root, skip_limb=None):
        """(cubes under `root`, the other cubes it may clip, its rest fits) for a gap solve."""
        import focus_check as fc
        if root not in self._pieces:
            under = self.subtree(root)
            mov = [i for i in self.rags + self.others if self.bone_of[i] in under]
            oth = [i for i in self.rags + self.others if self.bone_of[i] not in under
                   and (skip_limb is None or fc._limb_root(self.full, self.bone_of[i]) != skip_limb)]
            M, C, R = self.fk({})
            rest = self.fits_of(C, R, mov, C[mov], R[mov], oth)
            self._pieces[root] = (mov, oth, rest)
        return self._pieces[root]

    def gap(self, fr, root, M2, skip_limb=None):
        """px the piece under `root` (posed by the bone matrices M2) lies in the rest of the body beyond its rest fit."""
        mov, oth, rest = self.piece(root, skip_limb)
        MC, MR = self._cubes(M2, mov)
        fits = self.fits_of(fr[1], fr[2], mov, MC, MR, oth)
        return max([v - rest.get(b, 0.0) for b, v in fits.items()] + [0.0])

    def rag_gap(self, fr, theta, fold=1.0):
        M, C, R = fr
        RC, RR = self.rag_boxes(M, theta, fold)
        fits = self._rag_fits(C, R, RC, RR)
        gap = max([v - self.rest_fit.get(b, 0.0) for b, v in fits.items()] + [0.0])
        low = self.ground + FLOOR_PAD - float(RC[:, :, 1].min()) * 16.0
        return gap, low

    def _rag_sign(self, rest):
        z = []
        for s in (1.0, -1.0):
            RC, _ = self.rag_boxes(rest[0], 15.0 * s)
            z.append(float(RC[:, :, 2].mean()))
        return 1.0 if z[0] > z[1] else -1.0          # world +z is his front

    def rag_need(self, fr, folds=RAG_FOLDS):
        """(least forward turn in degrees >= 0, fold) that clears the legs and the ground. The strips turn first; when a
        knee comes up higher than a turn can clear, they also bunch (fold < 1 shortens them about the hinge)."""
        def ok(a, f):
            g = self.rag_gap(fr, self.sign * a, f)
            return g[0] <= RAG_CLEAR and g[1] <= 0.0
        if ok(0.0, folds[0]):
            return 0.0, folds[0]
        for f in folds:
            prev = None
            for a in np.arange(0.0, RAG_MAX + 1e-6, 5.0):
                if ok(a, f):
                    if prev is None:
                        return 0.0, f
                    lo, hi = prev, a
                    for _ in range(4):
                        mid = (lo + hi) / 2.0
                        lo, hi = (lo, mid) if ok(mid, f) else (mid, hi)
                    return hi, f
                prev = a
        best = min(((self.rag_gap(fr, self.sign * a, f)[0], a, f) for f in folds[-1:]
                    for a in np.arange(0.0, RAG_MAX + 1e-6, 5.0)))
        return best[1], best[2]

    # floor --------------------------------------------------------------------------------------
    def low(self, C, idx):
        return float(C[idx][:, :, 1].min()) * 16.0 if idx else 1e9


# ------------------------------------------------------------------------------------------------ easing of a need

def envelope(need, rise, fall, loop):
    """The least curve over `need` (one value per sample) that rises at most `rise` and falls at most `fall` per sample,
    then softened (and kept over the need). Loops wrap."""
    s = np.asarray(need, dtype=float)
    n = len(s)
    if not s.any():
        return s
    if loop:
        m = n - 1
        x = np.concatenate([s[:m]] * 3)
    else:
        x = s.copy()

    def tent(v):
        v = v.copy()
        for i in range(1, len(v)):           # going forward a value can only fall at `fall`
            v[i] = max(v[i], v[i - 1] - fall)
        for i in range(len(v) - 2, -1, -1):  # going back it can only rise at `rise`
            v[i] = max(v[i], v[i + 1] - rise)
        return v

    def blur(v):
        k = np.array([1.0, 4.0, 6.0, 4.0, 1.0]) / 16.0
        p = np.pad(v, 2, mode="edge")
        return np.convolve(p, k, mode="valid")

    e = tent(x)
    e = tent(np.maximum(blur(e), x))
    if loop:
        e = e[m:2 * m]
        e = np.append(e, e[0])
    return e


def _times(ctx):
    n = _n(ctx.clip)
    return [i / SUB / TPS for i in range(n + 1)], n


# ------------------------------------------------------------------------------------------------ the fixes

def fix_rags(rig, B, anims, name, partner, base, log):
    ctx = Ctx(anims, name, partner, base)
    ctx.clip["bones"].pop("rags", None)
    ts, n = _times(ctx)
    folds = RAG_FOLDS
    if base is not None:     # HitController adds a hit's turns only: the strips keep the stance's length under it
        sc0 = anims[base[1]]["bones"].get("rags", {}).get("scale")
        folds = (_at(_keys(sc0), base[2])[1] if sc0 else 1.0,)
    res = [rig.rag_need(rig.fk(ctx.pose(t, strip=("rags",))), folds) for t in ts]
    need = [a for a, _ in res]
    bunch = [folds[0] - f for _, f in res]
    if ctx.loop:
        need[-1] = need[0] = max(need[0], need[-1])
        bunch[-1] = bunch[0] = max(bunch[0], bunch[-1])
    env = envelope(need, RAG_RATE, RAG_FALL, ctx.loop)
    fold = envelope(bunch, FOLD_RATE, FOLD_RATE, ctx.loop)
    if base is not None:     # the stance under an additive hit already turns them this far
        r0 = rig.sign * ctx.base_of("rags")[0][0]
        env = np.maximum(env - r0, 0.0)
        env[0] = env[-1] = 0.0
        fold[0] = fold[-1] = 0.0
    if max(env) < 0.05 and max(fold) < 0.005:
        log("  rags %-28s clear (no key)" % name)
        return
    ch = {}
    vals = [[round(rig.sign * float(v), 3), 0.0, 0.0] for v in env]
    ch["rotation"] = {_ft(i / SUB / TPS): vals[i] for i in B.thin(vals, B.ROT_TOL)}
    if max(fold) >= 0.005 and base is None:
        sv = [[1.0, round(1.0 - float(v), 4), 1.0] for v in fold]
        ch["scale"] = {_ft(i / SUB / TPS): sv[i] for i in B.thin(sv, 0.002)}
    ctx.clip["bones"]["rags"] = ch
    k = int(np.argmax(need))
    left = max(rig.rag_gap(rig.fk(ctx.pose(t, strip=("rags",))), rig.sign * float(env[i]) + (ctx.base_of("rags")[0][0] if base is not None else 0.0), folds[0] - float(fold[i]))[0]
               for i, t in enumerate(ts) if i % 4 == 0)
    log("  rags %-28s turn max %.1f deg at t%s (keyed %.1f), bunch max %.2f; worst gap left %.2f px"
        % (name, max(need), B.fmt_t(k / SUB) if k else "0", max(env), max(fold), left))


def _bpose(pose):
    return {b: (r, p if p is not None else Z3) for b, (r, p) in pose.items()}


def _solve_leg(B, pose, side, target, foot_R, guess):
    leg, shin, foot = side + "_leg", side + "_shin", side + "_foot"
    hipsM = B.chain(_bpose(pose), B.LEG_CHAIN)["hips"]
    legY = pose.get(leg, (Z3, None))[0][1]
    x = np.array(guess, dtype=float)

    def ankle(v):
        Ms = hipsM @ B.local(leg, (v[0], legY, v[1])) @ B.local(shin, (v[2], 0, 0))
        return B.point(Ms @ B.local(foot), B.ankle_px(side))

    for _ in range(80):
        a = ankle(x)
        err = target - a
        if np.linalg.norm(err) < 1e-5:
            break
        J = np.zeros((3, 3))
        for i in range(3):
            d = np.zeros(3)
            d[i] = 1e-3
            J[:, i] = (ankle(x + d) - a) / 1e-3
        step = np.linalg.solve(J.T @ J + 0.02 * np.eye(3), J.T @ err)
        if np.linalg.norm(step) > 10:
            step *= 10 / np.linalg.norm(step)
        x += step
        x[2] = max(x[2], 0.0)
    if np.linalg.norm(target - ankle(x)) > 0.05:
        raise ValueError("%s ankle unreachable (%.3f px)" % (side, np.linalg.norm(target - ankle(x))))
    Ms = hipsM @ B.local(leg, (x[0], legY, x[1])) @ B.local(shin, (x[2], 0, 0))
    fr = B.decompose_zyx(Ms[:3, :3].T @ foot_R)
    return (x[0], legY, x[1]), (x[2], 0.0, 0.0), fr


def _lifted(rig, B, pose, lift, raise_foot, planted):
    """The pose with the hips raised `lift` px and each planted leg re-solved onto its ankle (raised `raise_foot`)."""
    p = dict(pose)
    hr, hp = p.get("hips", (Z3, None))
    hp = hp or Z3
    p["hips"] = (hr, (hp[0], hp[1] + lift, hp[2]))
    for side in ("right", "left"):
        if not planted[side] and raise_foot[side] <= 0:
            continue
        foot = side + "_foot"
        Mf = B.world(_bpose(pose), foot)
        tgt = B.point(Mf, B.ankle_px(side)) + np.array([0.0, raise_foot[side], 0.0])
        guess = (pose.get(side + "_leg", (Z3, None))[0][0], pose.get(side + "_leg", (Z3, None))[0][2],
                 pose.get(side + "_shin", (Z3, None))[0][0])
        lr, sr, fr = _solve_leg(B, p, side, tgt, Mf[:3, :3], guess)
        p[side + "_leg"] = (lr, p.get(side + "_leg", (Z3, None))[1])
        p[side + "_shin"] = (sr, p.get(side + "_shin", (Z3, None))[1])
        p[foot] = (fr, p.get(foot, (Z3, None))[1])
    return p


def fix_floor(rig, B, anims, name, partner, base, log):
    ctx = Ctx(anims, name, partner, base)
    ts, n = _times(ctx)
    g = rig.ground + FLOOR_PAD
    lift, rf = np.zeros(n + 1), {s: np.zeros(n + 1) for s in ("right", "left")}
    plant = []
    for i, t in enumerate(ts):
        pose = ctx.pose(t)
        _, C, _ = rig.fk(pose)
        foot_low = {s: rig.low(C, rig.foot[s]) for s in ("right", "left")}
        planted = {s: foot_low[s] - rig.ground < 0.75 for s in ("right", "left")}
        plant.append(planted)
        for s in ("right", "left"):
            rf[s][i] = max(0.0, rig.ground + 0.05 - foot_low[s]) if foot_low[s] < rig.ground - 0.05 else 0.0
        knee = min(rig.low(C, rig.leg[s]) for s in ("right", "left"))
        if knee >= g:
            continue
        best = (knee, 0.0)
        for d in np.arange(0.1, 4.01, 0.1):      # the least rise that clears, or the best a planted leg can reach
            try:
                p = _lifted(rig, B, pose, float(d), {s: rf[s][i] for s in rf}, planted)
            except ValueError:
                break
            _, C2, _ = rig.fk(p)
            k2 = min(rig.low(C2, rig.leg[s]) for s in ("right", "left"))
            if k2 > best[0]:
                best = (k2, float(d))
            if k2 >= g:
                break
        lift[i] = best[1]
        if best[0] < g:
            log("  floor %s t%s: knee still %.2f px under after %.1f px" % (name, B.fmt_t(i / SUB), g - best[0], best[1]))
    if not lift.any() and not any(rf[s].any() for s in rf):
        log("  floor %-27s clear" % name)
        return
    if ctx.loop:
        lift[-1] = lift[0] = max(lift[0], lift[-1])
    E = envelope(lift, LIFT_RATE, LIFT_RATE, ctx.loop)
    R = {s: envelope(rf[s], LIFT_RATE, LIFT_RATE, ctx.loop) for s in rf}
    if base is not None:     # an additive clip starts and ends on the stance: the fix ramps in and out (hips at LIFT_RATE, boots at RAISE_RATE)
        E = np.minimum(E, np.array([LIFT_RATE * min(i, n - i) for i in range(n + 1)]))
        for s in R:
            R[s] = np.minimum(R[s], np.array([RAISE_RATE * min(i, n - i) for i in range(n + 1)]))
    live = [i for i in range(n + 1) if E[i] > 1e-4 or R["right"][i] > 1e-4 or R["left"][i] > 1e-4]
    idx = sorted({j for i in live for j in (i - 1, i, i + 1) if 0 <= j <= n})
    # How much each leg keeps its boot where it was: 1 while the boot is down, eased off over PLANT_EASE samples as it
    # leaves the ground (a hard switch snaps the thigh and shin back in one sample).
    W = {}
    for s in rf:
        raw = np.array([1.0 if plant[i][s] or R[s][i] > 1e-4 else 0.0 for i in range(n + 1)])
        w = np.zeros(n + 1)
        for i in range(n + 1):
            acc = 0.0
            for k in range(-PLANT_EASE, PLANT_EASE + 1):
                j = (i + k) % n if ctx.loop else min(max(i + k, 0), n)      # a loop's last sample is its first
                acc += raw[j]
            w[i] = acc / (2 * PLANT_EASE + 1)
        W[s] = np.array([B.smooth(float(v)) for v in w])
    out = {b: {} for b in ("hips", "right_leg", "right_shin", "right_foot", "left_leg", "left_shin", "left_foot")}
    for i in idx:
        pose = ctx.pose(ts[i])
        planted = {s: bool(E[i] > 0 and W[s][i] > 1e-4) for s in rf}
        e = float(E[i])
        while True:
            try:
                p = _lifted(rig, B, pose, e, {s: float(R[s][i]) for s in R}, planted)
                break
            except ValueError:
                e -= 0.05
                if e < 0:
                    raise
        for s in rf:
            if not planted[s] and R[s][i] <= 1e-4:
                continue
            for part in ("_leg", "_shin", "_foot"):
                b = s + part
                r0, q = pose.get(b, (Z3, None))
                r1 = p[b][0]
                p[b] = (tuple(a + (c - a) * float(W[s][i]) for a, c in zip(r0, r1)), q)
        for b in out:
            r, q = p.get(b, (Z3, None))
            r0, q0 = ctx.base_of(b)
            if b == "hips":
                out[b][ts[i]] = [a - c for a, c in zip(q or Z3, q0 or Z3)]
            else:
                out[b][ts[i]] = [a - c for a, c in zip(r, r0)]
    for b, vals in out.items():
        write_channel(ctx.clip, b, "position" if b == "hips" else "rotation", vals)
    k = int(np.argmax(lift))
    log("  floor %-27s hips up max %.2f px at t%s, boot raise max %.2f px, keys %d-%d"
        % (name, float(E.max()), B.fmt_t(k / SUB) if k else "0", max(R["right"].max(), R["left"].max()),
           idx[0], idx[-1]))


def fix_weapon(rig, B, anims, name, partner, base, log):
    ctx = Ctx(anims, name, partner, base)
    ts, n = _times(ctx)
    g = rig.ground + FLOOR_PAD

    def low(pose, d):
        if d:
            r, q = pose["right_arm"]
            pose = dict(pose)
            pose["right_arm"] = ((r[0] + d, r[1], r[2]), q)
        return rig.low(rig.fk(pose)[1], rig.weapon)

    lows = [low(ctx.pose(t), 0.0) for t in ts]
    deep = [max(0.0, g - v) for v in lows]
    if max(deep) <= 0:
        log("  weapon %-26s clear" % name)
        return
    k = int(np.argmax(deep))
    pk = ctx.pose(ts[k])
    sign = -1.0 if low(pk, -6.0) > low(pk, 6.0) else 1.0      # arm x- lifts the hand forward
    need = np.zeros(n + 1)
    for i in range(n + 1):
        if deep[i] <= 0:
            continue
        pose = ctx.pose(ts[i])
        lo, hi = 0.0, 40.0
        for _ in range(12):
            mid = (lo + hi) / 2.0
            lo, hi = (lo, mid) if low(pose, sign * mid) >= g else (mid, hi)
        need[i] = hi
    if ctx.loop:
        need[-1] = need[0] = max(need[0], need[-1])
    E = envelope(need, ARM_RATE, ARM_RATE, ctx.loop)
    live = [i for i in range(n + 1) if E[i] > 1e-3]
    lo_i, hi_i = max(0, live[0] - 1), min(n, live[-1] + 1)
    ch = ctx.clip["bones"]["right_arm"]["rotation"]
    ks = _keys(ch)
    times = sorted({ts[i] for i in range(lo_i, hi_i + 1)} | {t for t, _ in ks if ts[lo_i] <= t <= ts[hi_i]})
    vals = {}
    for t in times:
        f = t * TPS * SUB
        a = int(math.floor(f))
        d = float(E[min(a, n)] + (E[min(a + 1, n)] - E[min(a, n)]) * (f - a))
        v = _at(ks, t)
        vals[t] = [v[0] + sign * d, v[1], v[2]]
    write_channel(ctx.clip, "right_arm", "rotation", vals)
    log("  weapon %-26s blade %.2f px under at t%s: arm lifted max %.1f deg over t%s-%s"
        % (name, max(deep), B.fmt_t(k / SUB), float(E.max()), B.fmt_t(lo_i / SUB), B.fmt_t(hi_i / SUB)))


def fix_soft(rig, B, anims, name, partner, base, log):
    import fullbody as FB
    ctx = Ctx(anims, name, partner, base)
    for b in SOFT_BONES:
        ctx.clip["bones"].pop(b, None)
    ts, n = _times(ctx)
    legs_bones = set(anims[partner]["bones"]) if partner in anims else set()
    if base is not None and base[1] is not None:
        legs_bones |= set(anims[base[1]]["bones"])
    upper, legs = [], []
    for t in ts:
        p = ctx.pose(t, strip=SOFT_BONES)
        legs.append({b: v for b, v in p.items() if b in legs_bones})
        upper.append({b: v for b, v in p.items() if b not in legs_bones})
    upper, legs = FB.soft_motion(upper, legs, report=False)
    bones = [b for b in SOFT_BONES if b in upper[0] and b not in TINES.values()]
    samples = []
    for i in range(n + 1):
        s = {}
        for b in bones:
            r, q = upper[i][b]
            r0, q0 = ctx.base_of(b)
            s[b] = (tuple(a - c for a, c in zip(r, r0)), None if q is None and q0 is None else
                    tuple(a - c for a, c in zip(q or Z3, q0 or Z3)))
            if b in TINES and TINES[b] in B.MODEL.bones:
                s[TINES[b]] = s[b]
        samples.append(s)
    if name == "stun_upper":         # the loop plays ticks 4-24: its end comes back to where tick 4 has it
        a, z = int(STUN_LOOP[0] * SUB), int(STUN_LOOP[1] * SUB)
        mid = (a + z) // 2
        for b in samples[0]:
            ra, rz = np.array(samples[a][b][0]), np.array(samples[z][b][0])
            pa = np.array(samples[a][b][1] or Z3)
            pz = np.array(samples[z][b][1] or Z3)
            for i in range(mid, z + 1):
                f = B.smooth((i - mid) / float(z - mid))
                r, q = samples[i][b]
                samples[i][b] = (tuple(np.array(r) + (ra - rz) * f),
                                 None if q is None and samples[a][b][1] is None else tuple(np.array(q or Z3) + (pa - pz) * f))
            if samples[z][b][1] is not None:     # a plate's slide also starts where the loop ends (the clip's own wrap)
                p0, pe = np.array(samples[0][b][1] or Z3), np.array(samples[z][b][1])
                for i in range(0, a + 1):
                    f = 1.0 - B.smooth(i / float(a))
                    r, q = samples[i][b]
                    samples[i][b] = (r, tuple(np.array(q or Z3) + (pe - p0) * f))
    baked = B.bake(samples, _ticks(ctx.clip), loop=ctx.loop)
    for b, ch in baked["bones"].items():
        ctx.clip["bones"][b] = ch
    swing = {}
    for b in bones:
        rs = [np.array(s[b][0]) for s in samples]
        swing[b] = max(float(np.linalg.norm(r - rs[0])) for r in rs)
    log("  soft %-28s %s" % (name, ", ".join("%s %.0f" % (b, v) for b, v in swing.items())))


def _posed(rig, M, bone, rot=None, pos=None):
    """Bone matrices with one bone re-posed (its subtree follows)."""
    M2 = dict(M)
    old = M[bone]
    M2[bone] = M[rig.model.bones[bone].parent] @ rig.local(bone, rot or Z3, pos)
    D = M2[bone] @ np.linalg.inv(old)
    for n in rig.subtree(bone):
        if n != bone:
            M2[n] = D @ M[n]
    return M2


def fix_cuisse(rig, B, anims, name, partner, base, log):
    """A thigh plate lifted with the thigh past ~60 degrees sinks its top into the hip block: it slides down its own thigh
    (at most CUISSE_SLIDE px), as a pauldron slides down its arm under the helm."""
    ctx = Ctx(anims, name, partner, base)
    ts, n = _times(ctx)
    did = []
    for side in ("right", "left"):
        bone = side + "_cuisse"
        ctx.clip["bones"].pop(bone, None)
        need = np.zeros(n + 1)
        for i, t in enumerate(ts):
            fr = rig.fk(ctx.pose(t))
            if rig.gap(fr, bone, fr[0], skip_limb=side + "_leg") <= PLATE_CLEAR:
                continue
            d = CUISSE_SLIDE
            for x in np.arange(0.25, CUISSE_SLIDE + 1e-6, 0.25):
                M2 = _posed(rig, fr[0], bone, pos=(0.0, -float(x), 0.0))
                if rig.gap(fr, bone, M2, skip_limb=side + "_leg") <= PLATE_CLEAR:
                    d = float(x)
                    break
            need[i] = d
        if not need.any():
            continue
        if ctx.loop:
            need[-1] = need[0] = max(need[0], need[-1])
        E = envelope(need, SLIDE_RATE, SLIDE_RATE, ctx.loop)
        if base is not None:
            E[0] = E[-1] = 0.0
        vals = [[0.0, round(-float(v), 4), 0.0] for v in E]
        ctx.clip["bones"][bone] = {"position": {_ft(i / SUB / TPS): vals[i] for i in B.thin(vals, B.POS_TOL)}}
        did.append("%s %.2f px" % (bone, E.max()))
    log("  cuisse %-26s %s" % (name, ", ".join(did) or "clear"))


def _add_x(ctx, B, bone, E, sign, ts, n):
    """Adds sign * E (degrees per sample) to a bone's x rotation over the samples where E is live, keys kept."""
    live = [i for i in range(n + 1) if E[i] > 1e-3]
    lo_i, hi_i = max(0, live[0] - 1), min(n, live[-1] + 1)
    if ctx.loop and (live[0] == 0 or live[-1] == n):
        lo_i, hi_i = 0, n
    ks = _keys(ctx.clip["bones"][bone]["rotation"])
    times = sorted({ts[i] for i in range(lo_i, hi_i + 1)} | {t for t, _ in ks if ts[lo_i] <= t <= ts[hi_i]})
    vals = {}
    for t in times:
        f = t * TPS * SUB
        a = int(math.floor(f))
        d = float(E[min(a, n)] + (E[min(a + 1, n)] - E[min(a, n)]) * (f - a))
        v = _at(ks, t)
        vals[t] = [v[0] + sign * d, v[1], v[2]]
    write_channel(ctx.clip, bone, "rotation", vals)
    return lo_i, hi_i


def fix_thigh(rig, B, anims, name, partner, base, log):
    """A thigh lifted high in the run drives its top into the hip block: the thigh lifts by the least angle less at the
    peak and the shin keeps its angle to the world (the boot is in the air there), so the stride reads the same, heavier."""
    ctx = Ctx(anims, name, partner, base)
    ts, n = _times(ctx)
    hips = [i for i in rig.others if rig.bone_of[i] == "hips"]
    did = []
    for side in ("right", "left"):
        leg, shin = side + "_leg", side + "_shin"
        mov = [i for i in rig.rags + rig.others if rig.bone_of[i] in rig.subtree(leg)]
        M0, C0, R0 = rig.fk({})
        rest = rig.fits_of(C0, R0, mov, C0[mov], R0[mov], hips).get("hips", 0.0)

        def g(fr, pose, d):
            (r, q), (rs, qs) = pose[leg], pose[shin]
            M2 = _posed(rig, fr[0], leg, rot=(r[0] + d, r[1], r[2]), pos=q)
            M2 = _posed(rig, M2, shin, rot=(rs[0] - d, rs[1], rs[2]), pos=qs)
            MC, MR = rig._cubes(M2, mov)
            return rig.fits_of(fr[1], fr[2], mov, MC, MR, hips).get("hips", 0.0) - rest

        need, sign = np.zeros(n + 1), None
        for i, t in enumerate(ts):
            pose = ctx.pose(t)
            fr = rig.fk(pose)
            if g(fr, pose, 0.0) <= THIGH_CLEAR:
                continue
            if sign is None:
                sign = 1.0 if g(fr, pose, 4.0) < g(fr, pose, -4.0) else -1.0
            d = THIGH_MAX
            for x in np.arange(0.5, THIGH_MAX + 1e-6, 0.5):
                if g(fr, pose, sign * x) <= THIGH_CLEAR:
                    d = float(x)
                    break
            need[i] = d
        if not need.any():
            continue
        if ctx.loop:
            need[-1] = need[0] = max(need[0], need[-1])
        E = envelope(need, ARM_RATE, ARM_RATE, ctx.loop)
        lo_i, hi_i = _add_x(ctx, B, leg, E, sign, ts, n)
        _add_x(ctx, B, shin, E, -sign, ts, n)
        did.append("%s %.1f deg less at most, t%s-%s" % (leg, E.max(), B.fmt_t(lo_i / SUB), B.fmt_t(hi_i / SUB)))
    log("  thigh %-27s %s" % (name, ", ".join(did) or "clear"))


def fix_edge(rig, B, anims, name, partner, base, log):
    """The blade lands a little flat at the impact: the sword hand rolls it onto its edge by the least angle, over a short
    window around the impact (the scorecard's edge_lead, the edge against the tip's motion at the impact tick)."""
    import render_geo as rg
    import scorecard as sc
    geo = "%s/geo/troop/%s.geo.json" % (sc.ASSETS, TROOP)
    srig = sc.Rig(rg.load_geo(str(ROOT / geo)), sc.rig_rules_for(str(ROOT), geo), TROOP)
    imp = float(sc.Design(str(ROOT)).variant(TROOP, name)["impactTicks"])
    clip = anims[name]
    ch = clip["bones"]["right_hand"]["rotation"]
    ks = _keys(ch)
    W = EDGE_WINDOW / TPS
    t0 = imp / TPS
    times = sorted({round(t0 + k / SUB / TPS, 6) for k in range(-int(EDGE_WINDOW * SUB), int(EDGE_WINDOW * SUB) + 1)}
                   | {t for t, _ in ks if t0 - W <= t <= t0 + W})

    def edge(axis, d):
        doc = {"animations": {partner: anims[partner], name: dict(clip, bones=dict(clip["bones"]))}}
        if d:
            vals = {}
            for t in times:
                v = list(_at(ks, t))
                v[axis] += d * B.smooth(1.0 - min(1.0, abs(t - t0) / W))
                vals[t] = v
            c = doc["animations"][name]
            c["bones"]["right_hand"] = dict(c["bones"]["right_hand"])
            write_channel(c, "right_hand", "rotation", vals)
        clips = [rg.load_clip(doc, partner), rg.load_clip(doc, name)]
        scene = rg.Scene(srig.model, None, clips, 0.0)
        a, b = scene.evaluate(imp - 0.25), scene.evaluate(imp)
        tip = lambda fr: (fr.bone_M[srig.blade_bone] @ srig.tip)[:3] * 16.0  # noqa: E731
        v = tip(b) - tip(a)
        Rm = sc._rot(b.bone_M[srig.blade_bone])
        Lx, E = Rm @ np.array([0.0, 1.0, 0.0]), Rm @ np.array([1.0, 0.0, 0.0])
        vp = v - (v @ Lx) * Lx
        th = abs(math.degrees(math.atan2(float(np.cross(E, vp) @ Lx), float(E @ vp))))
        return min(th, 180.0 - th), (doc["animations"][name]["bones"]["right_hand"] if d else None)

    e0, _ = edge(0, 0.0)
    if e0 <= EDGE_LEAD:
        log("  edge %-28s %.2f deg, clear" % (name, e0))
        return
    best = None
    for d in np.arange(0.5, EDGE_MAX + 1e-6, 0.5):
        for axis in (1, 0, 2):
            for s in (1.0, -1.0):
                e, ch2 = edge(axis, s * d)
                if e <= EDGE_LEAD and best is None:
                    best = (axis, s * d, e, ch2)
        if best:
            break
    if best is None:
        log("  edge %-28s %.2f deg, no roll within %.0f deg clears it" % (name, e0, EDGE_MAX))
        return
    clip["bones"]["right_hand"] = best[3]
    log("  edge %-28s %.2f -> %.2f deg: right_hand %s %+.1f deg at t%s, eased over %.0f ticks each side"
        % (name, e0, best[2], "xyz"[best[0]], best[1], B.fmt_t(imp), EDGE_WINDOW))


def fix_head(rig, B, anims, name, partner, base, log):
    """The head thrown back into the spine fan on his back: tipped back less, by the least angle, eased."""
    ctx = Ctx(anims, name, partner, base)
    ts, n = _times(ctx)

    def g(fr, pose, d):
        r, q = pose["head"]
        return rig.gap(fr, "head", _posed(rig, fr[0], "head", rot=(r[0] + d, r[1], r[2]), pos=q))

    need, sign = np.zeros(n + 1), None
    for i, t in enumerate(ts):
        pose = ctx.pose(t)
        fr = rig.fk(pose)
        if g(fr, pose, 0.0) <= PLATE_CLEAR:
            continue
        if sign is None:
            sign = 1.0 if g(fr, pose, 6.0) < g(fr, pose, -6.0) else -1.0
        d = HEAD_MAX
        for x in np.arange(1.0, HEAD_MAX + 1e-6, 1.0):
            if g(fr, pose, sign * x) <= PLATE_CLEAR:
                d = float(x)
                break
        need[i] = d
    if not need.any():
        log("  head %-28s clear" % name)
        return
    E = envelope(need, ARM_RATE, ARM_RATE, ctx.loop)
    if base is not None:
        E[0] = E[-1] = 0.0
    live = [i for i in range(n + 1) if E[i] > 1e-3]
    lo_i, hi_i = max(0, live[0] - 1), min(n, live[-1] + 1)
    ks = _keys(ctx.clip["bones"]["head"]["rotation"])
    times = sorted({ts[i] for i in range(lo_i, hi_i + 1)} | {t for t, _ in ks if ts[lo_i] <= t <= ts[hi_i]})
    vals = {}
    for t in times:
        f = t * TPS * SUB
        a = int(math.floor(f))
        d = float(E[min(a, n)] + (E[min(a + 1, n)] - E[min(a, n)]) * (f - a))
        v = _at(ks, t)
        vals[t] = [v[0] + sign * d, v[1], v[2]]
    write_channel(ctx.clip, "head", "rotation", vals)
    log("  head %-28s tipped back %.1f deg less at most, t%s-%s" % (name, E.max(), B.fmt_t(lo_i / SUB), B.fmt_t(hi_i / SUB)))


FIXES = (("floor", fix_floor), ("rags", fix_rags), ("cuisse", fix_cuisse), ("thigh", fix_thigh), ("weapon", fix_weapon),
         ("edge", fix_edge), ("head", fix_head), ("soft", fix_soft))


def polish(doc, plan, log=print):
    """Applies the plan (BUILT or LATE) to an animation document in place."""
    B = _B()
    rig = Rig()
    anims = doc["animations"]
    log("hk_polish: ground %.2f px, rags turn %+d" % (rig.ground, int(rig.sign)))
    for kind, fn in FIXES:
        for name, partner, base in plan.get(kind, ()):
            if name not in anims:
                log("  %s %s: no such clip" % (kind, name))
                continue
            fn(rig, B, anims, name, partner, base, log)
    return doc


def main():
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else ASSET
    doc = json.loads(path.read_text(encoding="utf-8"))
    polish(doc, LATE)
    with open(path, "w", newline="\n") as fh:
        fh.write(json.dumps(doc, indent=1))
    print("wrote", path)


if __name__ == "__main__":
    main()
