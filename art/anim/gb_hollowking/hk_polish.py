"""The Hollow King's polish pass (batch review 2026-11-11): what his clips needed after the four-focus-point check, applied to
the finished animation document (doc level, his clips only; every other troop's clips are never touched).

  rags       The rag skirt hangs in three groups (model fix 2026-11-13; it was one rigid board across both thighs):
             rags_r over the right thigh, rags_c between the thighs, rags_l over the left, each hinged on the belt line at
             its strip's top. Each group is a damped spring on its hinge pulled toward plumb, pushed by what it lies on:
             per sample the range of turns that keeps it clear of the legs, the body and the floor is found, and the spring
             is held inside it. A thigh coming forward lifts its own group at once; when it goes back the cloth falls late,
             on its own swing, a beat behind the leg. A tipping or turning body leaves the cloth behind, so it sways and
             settles after attacks and hits and hangs to gravity in the deaths. Loops run to their steady swing and close.
             Keyed on the groups in the _legs clip and the whole-body clips (additive hit legs: only what the stance under
             them does not already give, back to it by the end). No scale: the strips keep their length.
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
  plate      The knockback's folded right knee sat under its hip plate (hip_r into right_shin 1.94 px at t10): the plate
             flares off the shin by the least angle, eased in and out (the knee, the boot and the hit keep their pose).

Each fix measures the clip it is given, so a pass is meant for fresh clips. polish() therefore records what each stage
wrote (art/anim/gb_hollowking/out/hk_polish.state.json: per stage, a hash of every clip it changed) and does nothing when
the document already holds exactly that output, so running it again changes no byte. A document that holds only part of
a stage's output is refused (re-run the chain from the build). Timing, length, the impact marker and the timeline
markers are kept; channels it does not fix keep their keys.

Built clips: art/anim/knight/build_anims.py (GG_ANIM_TROOP=gb_hollowking) calls polish(doc, BUILT) before writing out/.
That stage ends with the same limb_clear pass (on the deployed geo and rig) that stances.py runs over the exported file,
so out/ equals the exported asset on every clip they share. Late clips (made in the exported asset by
art/anim/common/stances.py, reactions.py and charge.py): after that chain run
    python art/anim/gb_hollowking/hk_polish.py
which polishes the LATE clips of the exported animation file in place.
"""
from __future__ import annotations

import hashlib
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
ASSETS = ROOT / "src/main/resources/assets/gravesandglory"
ASSET = ASSETS / "animations/troop" / f"{TROOP}.animation.json"
STATE = HERE / "out" / "hk_polish.state.json"

SUB, TPS = 4, 20.0
Z3 = (0.0, 0.0, 0.0)
RAG_CLEAR = 1.0      # px a rag may lie in a leg beyond its rest fit (focus_check armour_clip warns at 1.7)
RAGS = ("rags_r", "rags_c", "rags_l")   # the rag skirt's three groups (model fix 2026-11-13), each on its own belt hinge
RAG_MAX = 80.0       # degrees a group may turn forward at most
RAG_BACK = 25.0      # degrees a group may swing back past its rest at most
RAG_STEP = 5.0       # degrees: the scan for the clear range, then bisected
RAG_PERIOD = 0.5     # s: the cloth's swing on its hinge (a damped spring toward plumb)
RAG_ZETA = 0.45
RAG_FLOOR = 1.0      # px a strip may lie on (in) the ground beyond FLOOR_PAD: cloth lands and folds there (a rigid strip
                     # cannot fold, so where it meets the floor it is let in this far instead of being flung up)
RAG_CARRY = 0.5      # of a pushing thigh's speed the cloth keeps when the thigh stops (heavy, rotted cloth)
RAG_LAND = 6.0       # degrees per sample at most a strip is pushed in a one-shot clip: where the body lands on it or a
                     # thigh meets it, it starts to move a few samples early instead of flicking in one sample
PLATE_CLEAR = 1.2    # px a plate or the head may lie in the body beyond its rest fit (armour_clip warns at 1.7)
CUISSE_SLIDE = 2.5   # px a thigh plate slides down its thigh at most
SLIDE_RATE = 0.15    # px per sample
HEAD_MAX = 25.0
PLATE_MAX = 30.0     # degrees a hip plate may flare at most
HIP_CLEAR = 1.6      # px a hip plate may lie in the leg beyond its rest fit (it sits on the thigh at 1.1-1.3 all through)
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
DEATHS = ["death_%s%s" % (d, x) for d in ("back", "forward", "left", "right") for x in ("", "_drop1", "_drop2")] + [
    "fake_death", "rise"]     # whole-body clips: the rags hang to gravity as he falls (and as he is drawn up again)
BUILT = {
    "floor": [("run_legs", "run_upper", None), ("stance_wall_legs", "stance_wall_upper", None)],
    "rags": [("idle_legs", "idle_upper", None), ("walk_legs", "walk_upper", None), ("run_legs", "run_upper", None),
             ("attack_overhead_cleave_legs", "attack_overhead_cleave", None),
             ("attack_side_cut_legs", "attack_side_cut", None), ("attack_thrust_legs", "attack_thrust", None),
             ("stance_ready_legs", "stance_ready_upper", None), ("stance_brace_legs", "stance_brace_upper", None),
             ("stance_wall_legs", "stance_wall_upper", None)] + [(h + "_legs", h, READY) for h in HITS]
            + [(d, None, None) for d in DEATHS],
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
    "plate": [("hit_knockback_legs", "hit_knockback", READY)],
    "seam": [("stun_legs", "stun_upper", None), ("stun_upper", "stun_legs", None)],
    "soft": [("hit_pulled", "hit_pulled_legs", READY), ("hit_knockback", "hit_knockback_legs", READY),
             ("stun_upper", "stun_legs", None), ("stun_out_upper", "stun_legs", STUN)],
}
STUN_LOOP = (4.0, 24.0)     # reactions.py: the stun clip plays its entry once, then loops ticks 4-24
START = {}     # group -> {clip: forward turn its first sample starts from} (none yet: plumb)
HIP_PLATES = ("hip_r", "hip_l")


def _seam(v, n):
    """The stun's legs loop ticks 4-24 in game: the last STUN_LOOP[0] ticks of a per-sample curve blend onto the curve's
    own run into tick 4, so tick 24 lands on tick 4 with the same speed (no pop at each wrap)."""
    a, z = int(STUN_LOOP[0] * SUB), int(STUN_LOOP[1] * SUB)
    if z > n:
        return v
    v = np.array(v, dtype=float)
    src = v.copy()
    for i in range(z - a, z + 1):
        f = _B().smooth((i - (z - a)) / float(a))
        v[i] = src[i] * (1.0 - f) + src[i - (z - a)] * f
    d0 = v[z] - v[0]        # the clip's own wrap too (it is marked loop): the entry eases from tick 24's value
    for i in range(0, a + 1):
        v[i] += d0 * (1.0 - _B().smooth(i / float(a)))
    return v


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
        self.rag_idx = {g: [i for i in keep if self.bone_of[i] == g] for g in RAGS}
        self.rags = [i for i in keep if self.bone_of[i] in RAGS]
        self.others = [i for i in keep if self.bone_of[i] not in RAGS]

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
        self._tip_local, self._tip_order = {}, {}
        for g, ix in self.rag_idx.items():      # the hem: the strip's four lowest corners at rest (model space, blocks)
            c = self.model.cube_list[ix[0]].corners
            self._tip_order[g] = np.argsort(c[:, 1])[:4]
            self._tip_local[g] = c[self._tip_order[g]].mean(axis=0)
        self.rest_fit = {g: self._rag_fits(rest[1], rest[2], rest[1][ix], rest[2][ix], ix) for g, ix in self.rag_idx.items()}
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
    def rag_boxes(self, M, g, theta):
        """The cubes of rag group g turned theta degrees (the channel's own sign) on its hinge, in a pose's matrices."""
        Mr = dict(M)
        Mr[g] = M[self.model.bones[g].parent] @ self.local(g, (theta, 0.0, 0.0), None)
        return self._cubes(Mr, self.rag_idx[g])

    def rag_tip(self, M, g, theta):
        """The middle of group g's hem (its lowest edge at rest), in blocks, for a turn theta."""
        C, _ = self.rag_boxes(M, g, theta)
        c = C[0]
        return c[self._tip_order[g]].mean(axis=0)

    def _rag_fits(self, C, R, RC, RR, idx):
        sc = self.sc
        out = {}
        lo, hi = C.min(axis=1), C.max(axis=1)
        for k in range(len(idx)):
            a = RC[k]
            alo, ahi = a.min(axis=0), a.max(axis=0)
            pa = sc.cube_points(a, self.lat[idx[k]])
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

    def piece(self, root, skip_limb=None, cloth=True):
        """(cubes under `root`, the other cubes it may clip, its rest fits) for a gap solve. cloth=False leaves the rag
        strips out of the others: the cloth yields to a plate, never the plate to the cloth."""
        import focus_check as fc
        key = (root, skip_limb, cloth)
        if key not in self._pieces:
            under = self.subtree(root)
            pool = self.rags + self.others if cloth else self.others
            mov = [i for i in pool if self.bone_of[i] in under]
            oth = [i for i in pool if self.bone_of[i] not in under
                   and (skip_limb is None or fc._limb_root(self.full, self.bone_of[i]) != skip_limb)]
            M, C, R = self.fk({})
            rest = self.fits_of(C, R, mov, C[mov], R[mov], oth)
            self._pieces[key] = (mov, oth, rest)
        return self._pieces[key]

    def gap(self, fr, root, M2, skip_limb=None, cloth=True):
        """px the piece under `root` (posed by the bone matrices M2) lies in the rest of the body beyond its rest fit."""
        mov, oth, rest = self.piece(root, skip_limb, cloth)
        MC, MR = self._cubes(M2, mov)
        fits = self.fits_of(fr[1], fr[2], mov, MC, MR, oth)
        return max([v - rest.get(b, 0.0) for b, v in fits.items()] + [0.0])

    def rag_gap(self, fr, g, theta):
        """(px group g lies in the body beyond its rest fit, px its lowest point is under the floor line) at a turn."""
        M, C, R = fr
        RC, RR = self.rag_boxes(M, g, theta)
        fits = self._rag_fits(C, R, RC, RR, self.rag_idx[g])
        rf = self.rest_fit[g]
        gap = max([v - rf.get(b, 0.0) for b, v in fits.items()] + [0.0])
        low = self.ground + FLOOR_PAD - float(RC[:, :, 1].min()) * 16.0
        return gap, low

    def _rag_sign(self, rest):
        RC, _ = self.rag_boxes(rest[0], "rags_c", 15.0)
        RC0, _ = self.rag_boxes(rest[0], "rags_c", -15.0)
        return 1.0 if RC[:, :, 2].mean() > RC0[:, :, 2].mean() else -1.0     # world +z is his front

    def rag_ok(self, fr, g, a, floor=0.0):
        gp, low = self.rag_gap(fr, g, self.sign * a)
        return gp <= RAG_CLEAR and low <= floor

    def rag_range(self, fr, g, floor=0.0):
        """(least, greatest) forward turn in degrees, in [-RAG_BACK, RAG_MAX], at which group g lies clear of the legs, the
        body and the floor. The clear turns are taken as one range: scanned in from both ends, then bisected."""
        grid = np.arange(-RAG_BACK, RAG_MAX + 1e-6, RAG_STEP)
        ok = None
        for j, a in enumerate(grid):
            if self.rag_ok(fr, g, float(a), floor):
                ok = j
                break
        if ok is None:      # nothing clears (pinned between the legs and the floor): the spring alone decides
            return -RAG_BACK, RAG_MAX
        lo = float(grid[ok])
        if ok > 0:
            a, b = float(grid[ok - 1]), lo
            for _ in range(5):
                m = (a + b) / 2.0
                a, b = (a, m) if self.rag_ok(fr, g, m, floor) else (m, b)
            lo = b
        hi = float(grid[-1])
        if not self.rag_ok(fr, g, hi, floor):
            j = len(grid) - 1
            while j > ok and not self.rag_ok(fr, g, float(grid[j]), floor):
                j -= 1
            a, b = float(grid[j]), float(grid[j + 1])
            for _ in range(5):
                m = (a + b) / 2.0
                a, b = (m, b) if self.rag_ok(fr, g, m, floor) else (a, m)
            hi = a
        return lo, hi

    def rag_plumb(self, M, g):
        """The forward turn (degrees) that hangs group g straight down under its hinge: gravity's pull, in its parent."""
        Mp = M[self.model.bones[g].parent]
        down = Mp[:3, :3].T @ np.array([0.0, -1.0, 0.0])
        a0 = self.rag_dir(M, g, 0.0)
        a1 = self.rag_dir(M, g, self.sign * 10.0)
        # the strip's direction turns about the hinge's x axis: the angle from down in the plane of that turn
        n = np.cross(a0, a1)
        n /= np.linalg.norm(n)
        d = down - n * float(down @ n)
        m = float(np.linalg.norm(d))
        if m < 1e-6:
            return 0.0
        d /= m
        ang = math.degrees(math.atan2(float(np.cross(a0, d) @ n), float(a0 @ d)))
        return ang * m * m        # lying on his side, down is along the hinge: the strips have no pull either way

    def rag_dir(self, M, g, theta):
        """Unit direction from group g's hinge to its hem, in its parent's frame, at a turn."""
        L = self.local(g, (theta, 0.0, 0.0), None)
        pv = self.model.bones[g].pivot / 16.0
        tip = (L @ np.append(self._tip_local[g], 1.0))[:3]
        v = tip - pv
        return v / np.linalg.norm(v)

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

def rag_curve(rig, frames, g, x0, loop, seam=False, floor=0.0):
    """Group g's forward turn per sample (degrees): a damped spring on its hinge toward plumb (gravity, so a turning or
    tipping body leaves the cloth behind and it swings and settles after), pushed by whatever it lies on: a thigh coming
    forward lifts it at once (the clear range's floor), and when the thigh goes back the strip falls late, on its own
    swing, a beat behind the leg. Loops run to their steady swing and close; x0 (or None: plumb) starts the rest."""
    n = len(frames) - 1
    rng = [rig.rag_range(fr, g, floor) for fr in frames]
    lo = np.array([r[0] for r in rng])
    hi = np.array([r[1] for r in rng])
    goal = np.clip([rig.rag_plumb(fr[0], g) for fr in frames], -RAG_BACK, RAG_MAX)
    w = 2.0 * math.pi / RAG_PERIOD
    k, damp = w * w, 2.0 * RAG_ZETA * w
    dt = 1.0 / (SUB * TPS)
    x = float(np.clip(goal[0] if x0 is None else x0, lo[0], hi[0]))
    v = 0.0
    out = np.zeros(n + 1)
    touch = 0     # which bound held it the sample before (-1 lo, +1 hi): only a bound already holding it carries it on
    for _ in range(4 if loop else 1):       # a loop runs round until its swing repeats
        for i in range(n + 1):
            if i:
                h = dt / 4.0
                for _ in range(4):
                    v += (k * (goal[i] - x) - damp * v) * h
                    x += v * h
            if x < lo[i]:            # pushed: it moves with what pushes it (a bound that only now meets it just stops it)
                x = lo[i]
                v = max(v, RAG_CARRY * (lo[i] - lo[i - 1]) / dt if i and touch < 0 else 0.0)
                touch = -1
            elif x > hi[i]:
                x = hi[i]
                v = min(v, RAG_CARRY * (hi[i] - hi[i - 1]) / dt if i and touch > 0 else 0.0)
                touch = 1
            else:
                touch = 0
            out[i] = x
    if loop:      # the residue of the wrap is spread over the cycle, so the last sample is the first
        out -= (out[n] - out[0]) * np.arange(n + 1) / float(n)
    if seam:
        out = _seam(out, n)
    out = np.clip(out, lo, np.maximum(lo, hi))
    if not loop:     # a one-shot (death, hit, attack): no push faster than RAG_LAND a sample, it starts early instead
        for i in range(n - 1, -1, -1):
            out[i] = min(max(out[i], out[i + 1] - RAG_LAND, lo[i]), out[i + 1] + RAG_LAND, max(lo[i], hi[i]))
    return out, lo


def fix_rags(rig, B, anims, name, partner, base, log):
    ctx = Ctx(anims, name, partner, base)
    for b in ("rags",) + RAGS:
        ctx.clip["bones"].pop(b, None)
    ts, n = _times(ctx)
    frames = [rig.fk(ctx.pose(t, strip=RAGS)) for t in ts]
    seam = (int(STUN_LOOP[0] * SUB), int(STUN_LOOP[1] * SUB)) if name == "stun_legs" else ()
    msg = []
    for g in RAGS:
        b0 = rig.sign * ctx.base_of(g)[0][0] if base is not None else None    # the stance an additive hit plays over
        x0 = b0 if b0 is not None else START.get(g, {}).get(name)
        curve, lo = rag_curve(rig, frames, g, x0, ctx.loop, seam=bool(seam), floor=RAG_FLOOR if name in DEATHS else 0.0)
        if b0 is not None:        # HitController adds a hit's turns: only what the stance does not already give,
            curve = curve - b0    # and back to the stance by the end
            m = min(n, 4 * SUB)
            for i in range(n - m, n + 1):
                f = B.smooth((i - (n - m)) / float(m))
                curve[i] = max(curve[i] * (1.0 - f), lo[i] - b0)
        if np.abs(curve).max() < 0.05:
            msg.append("%s still" % g)
            continue
        vals = [[round(rig.sign * float(v), 3), 0.0, 0.0] for v in curve]
        keys = sorted(set(B.thin(vals, B.ROT_TOL)) | set(seam))
        ctx.clip["bones"][g] = {"rotation": {_ft(i / SUB / TPS): vals[i] for i in keys}}
        msg.append("%s %+.1f..%+.1f" % (g, float(curve.min()), float(curve.max())))
    log("  rags %-28s %s" % (name, ", ".join(msg)))


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
            if rig.gap(fr, bone, fr[0], skip_limb=side + "_leg", cloth=False) <= PLATE_CLEAR:
                continue
            d = CUISSE_SLIDE
            for x in np.arange(0.25, CUISSE_SLIDE + 1e-6, 0.25):
                M2 = _posed(rig, fr[0], bone, pos=(0.0, -float(x), 0.0))
                if rig.gap(fr, bone, M2, skip_limb=side + "_leg", cloth=False) <= PLATE_CLEAR:
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


def fix_plate(rig, B, anims, name, partner, base, log):
    """A hip plate (hip_r / hip_l, hung from the hips) that a folded knee comes up under: the plate flares off the leg by
    the least angle about the axis that clears it soonest, eased in and out. The leg, the boot and the hit keep their
    pose; an additive clip starts and ends on the stance."""
    ctx = Ctx(anims, name, partner, base)
    ts, n = _times(ctx)
    did = []
    for bone in HIP_PLATES:
        def g(fr, pose, ax, d):
            r, q = pose.get(bone, (Z3, None))
            r = list(r)
            r[ax] += d
            return rig.gap(fr, bone, _posed(rig, fr[0], bone, rot=tuple(r), pos=q), cloth=False)

        frames = [(i, ctx.pose(t)) for i, t in enumerate(ts)]
        deep = []
        for i, pose in frames:
            fr = rig.fk(pose)
            v = g(fr, pose, 0, 0.0)
            if v > HIP_CLEAR:
                deep.append((v, i, pose, fr))
        if not deep:
            continue
        _, k, pk, fk = max(deep, key=lambda e: e[0])
        best = None
        for ax in (0, 2):             # forward/back or outward: the least flare that clears the worst sample
            for s in (1.0, -1.0):
                for x in np.arange(1.0, PLATE_MAX + 1e-6, 1.0):
                    if g(fk, pk, ax, s * x) <= HIP_CLEAR:
                        if best is None or x < best[2]:
                            best = (ax, s, float(x))
                        break
        if best is None:
            log("  plate %s %s: no flare within %.0f deg clears it" % (name, bone, PLATE_MAX))
            continue
        ax, s, _ = best
        need = np.zeros(n + 1)
        for _, i, pose, fr in deep:
            lo, hi = 0.0, PLATE_MAX
            for _ in range(10):
                mid = (lo + hi) / 2.0
                lo, hi = (lo, mid) if g(fr, pose, ax, s * mid) <= HIP_CLEAR else (mid, hi)
            need[i] = hi
        E = envelope(need, ARM_RATE, ARM_RATE, ctx.loop)
        if base is not None:
            E = np.minimum(E, np.array([ARM_RATE * min(i, n - i) for i in range(n + 1)]))
        live = [i for i in range(n + 1) if E[i] > 1e-3]
        lo_i, hi_i = max(0, live[0] - 1), min(n, live[-1] + 1)
        ch = ctx.clip["bones"].get(bone, {}).get("rotation")
        ks = _keys(ch) if ch is not None else [(0.0, list(Z3))]
        vals = {}
        for i in range(lo_i, hi_i + 1):
            v = _at(ks, ts[i])
            v[ax] += s * float(E[i])
            vals[ts[i]] = v
        write_channel(ctx.clip, bone, "rotation", vals)
        did.append("%s %s %+.1f deg at most (t%s), t%s-%s" % (bone, "xyz"[ax], s * float(E.max()), B.fmt_t(k / SUB),
                                                            B.fmt_t(lo_i / SUB), B.fmt_t(hi_i / SUB)))
    log("  plate %-27s %s" % (name, ", ".join(did) or "clear"))


def fix_seam(rig, B, anims, name, partner, base, log):
    """The stun loops ticks 4-24 in game: every channel's value at tick 24 is set to its value at tick 4 (the rags are
    already blended onto it; what is left is the made clip's own rounding)."""
    clip = anims[name]
    a, z = STUN_LOOP[0] / TPS, STUN_LOOP[1] / TPS
    worst = (0.0, "")
    for b, chans in clip["bones"].items():
        for c, ch in chans.items():
            if not isinstance(ch, dict):
                continue
            ks = _keys(ch)
            va, vz = _at(ks, a), _at(ks, z)
            d = max(abs(x - y) for x, y in zip(va, vz))
            if d > 1e-4:     # one rounded value keyed at both ends of the loop, so they are equal to the last digit
                va = [round(float(x), 4 if c == "position" else 3) for x in va]
                write_channel(clip, b, c, {a: va, z: va})
                worst = max(worst, (d, "%s %s" % (b, c)))
    log("  seam %-28s tick 24 = tick 4 on every channel (largest change %.3f, %s)" % (name, worst[0], worst[1] or "none"))


FIXES = (("floor", fix_floor), ("cuisse", fix_cuisse), ("thigh", fix_thigh), ("rags", fix_rags), ("weapon", fix_weapon),
         ("edge", fix_edge), ("head", fix_head), ("plate", fix_plate), ("soft", fix_soft), ("seam", fix_seam))


def _hash(clip):
    """A clip's hash as it reads back from its file (key times are strings there, numbers plain floats)."""
    flat = json.loads(json.dumps(clip))
    return hashlib.sha1(json.dumps(flat, sort_keys=True).encode("utf-8")).hexdigest()


def _state():
    try:
        return json.loads(STATE.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def polished(doc, stage):
    """True when doc already holds this stage's recorded output, False when it holds none of it. A document holding only
    part of it (some clips polished, some fresh) raises: a second pass over polished clips would move them again."""
    rec = _state().get(stage) or {}
    anims = doc["animations"]
    same = [c for c, h in rec.items() if c in anims and _hash(anims[c]) == h]
    if rec and len(same) == len(rec):
        return True
    if same:
        raise RuntimeError("hk_polish %s: %d of %d clips are already polished (%s); re-run the chain from the build"
                           % (stage, len(same), len(rec), ", ".join(sorted(same))))
    return False


def polish(doc, plan, log=print):
    """Applies the plan (BUILT or LATE) to an animation document in place, once: a document that already holds the
    stage's recorded output is left as it is (see the module note)."""
    stage = "built" if plan is BUILT else "late" if plan is LATE else None
    anims = doc["animations"]
    if stage and polished(doc, stage):
        log("hk_polish %s: already polished, nothing to do" % stage)
        return doc
    before = {c: _hash(v) for c, v in anims.items()}
    B = _B()
    rig = Rig()
    log("hk_polish: ground %.2f px, rags turn %+d" % (rig.ground, int(rig.sign)))
    for kind, fn in FIXES:
        for name, partner, base in plan.get(kind, ()):
            if name not in anims:
                log("  %s %s: no such clip" % (kind, name))
                continue
            fn(rig, B, anims, name, partner, base, log)
    if stage == "built":     # as stances.py does over the exported file, so out/ equals the asset on the shared clips
        import limb_clear
        limb_clear.key_doc(TROOP, doc, ASSETS / "geo/troop" / f"{TROOP}.geo.json", ASSETS / "troop_rig" / f"{TROOP}.json",
                           log=log)
    if stage:
        st = _state()
        st[stage] = {c: _hash(v) for c, v in anims.items() if before.get(c) != _hash(v)}
        STATE.parent.mkdir(parents=True, exist_ok=True)
        with open(STATE, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(json.dumps(st, indent=1, sort_keys=True) + "\n")
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
