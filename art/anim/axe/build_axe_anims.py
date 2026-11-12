"""Axe troops (unit animation rebuild): the knight's body mechanics, with the axe family's own swings.

    GG_AXE_TROOP=ic_hearthguard python art/anim/axe/build_axe_anims.py

ic_hearthguard (axe_shield, sheets/design.json) carries a hand axe and a round shield, so it reuses the knight's shield
guard, gait, stances, hits and falls, and replaces only the attacks with its two variants:
  overhead_chop  impact 10, head: the axe raised high behind the head, held, then driven straight down, bit first, and
                 left to bite low before it is gathered back.
  hook_hew       impact 8, body: the axe cocked high on the right, held, hewn down and across, then the bit hooked back
                 toward the body (the pull that drags a shield or a leg).
Both clips last ATTACK_TICKS, inside the variant's 28-tick cooldown, and carry the timeline "impact" marker on the server's
impactTicks (AttackController syncs it), with a matching attack_<name>_legs clip (STYLE.md rule 4).

The axe's rest rotation (troop_rig.py) puts the bit toward the foe in the guard, so every swing that pitches the tip down
leads with the bit. Cloth secondary motion (rule 9): the knight's cape lag is turned onto the tabard and the beard braid.
"""
from __future__ import annotations

import json
import math
import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
TROOP = os.environ.get("GG_AXE_TROOP", "ic_hearthguard")
os.environ["GG_ANIM_TROOP"] = TROOP
sys.path.insert(0, str(HERE.parent / "knight"))
import build_anims as K  # noqa: E402
sys.path.insert(0, str(HERE.parent / "common"))
import fullbody  # noqa: E402

np, Z3, SUB = K.np, K.Z3, K.SUB
K.HELD = K.HELD + ("weapon",)
K.DEATH_BONES = tuple(b for b in K.DEATH_BONES if b != "cape")
_GEO = json.loads((HERE.parent / TROOP / "out" / f"{TROOP}.geo.json").read_text())["minecraft:geometry"][0]["bones"]
GEO_BONES = {b["name"] for b in _GEO}

# Hanging cloth on the torso: (bone, share of the cape's lag). A front piece trails the same way a cape does about its top
# edge, so the knight's signed lag drives it; only the flare (which pushes a cape out behind) is dropped, or the tabard would
# swing into the thighs.
CLOTH = {"ic_hearthguard": (("tabard", 1.0), ("beard_braid", 0.6))}.get(TROOP, ())


def cloth_lag(samples, legs=None):
    """The cape's trailing lag, turned onto the troop's hanging cloth: it trails each pitch and turn of the body and settles
    a beat later; leaning forward, it hangs plumb, away from the legs."""
    lag = 2 * SUB

    def body(i):
        tr = samples[max(0, i)]["torso"][0]
        hr = legs[max(0, i)]["hips"][0] if legs else Z3
        return np.array(tr) + np.array(hr)
    for i, s in enumerate(samples):
        d = body(i) - body(i - lag)
        hang = 0.7 * max(0.0, body(i)[0])
        x = max(-28.0, min(4.0, -hang - 0.35 * d[0]))
        z = max(-6.0, min(6.0, -0.15 * d[1]))
        s.pop("cape", None)
        for bone, k in CLOTH:
            s[bone] = ((k * x, 0.0, k * z), None)
    return samples


K.cape_lag = cloth_lag   # the knight's attacks, hits and falls call it through the module

# ------------------------------------------------------------------------------------------------
# Attacks
# ------------------------------------------------------------------------------------------------

ATTACK_TICKS = 24
READY_HIPS = K.READY_HIPS
IMPACT = {"overhead_chop": 10, "hook_hew": 8}

# The standing footwork (rule 4): the knight's slash footwork (the hips lead the torso by about a tick), its recovery
# stretched over the axe's two extra ticks.
def _late(t, imp):
    return t if t <= imp + 2 else imp + 2 + (t - imp - 2) * (ATTACK_TICKS - imp - 2) / (K.ATTACK_TICKS - imp - 2)


SLASH_OF = {"overhead_chop": "overhead", "hook_hew": "low_cut"}   # the knight's slash each axe swing is cut from
K.ATTACK_LEGS = {}
for _name, _src in SLASH_OF.items():
    _sl = K.SLASH_LEGS[_src]
    K.ATTACK_LEGS[_name] = {"hips": [(_late(h[0], IMPACT[_name]),) + tuple(h[1:]) for h in _sl["hips"]],
                            "steps": [(st[0], _late(st[1], IMPACT[_name]), _late(st[2], IMPACT[_name])) + tuple(st[3:]) for st in _sl["steps"]]}
sw, GUARD = K.swing, K.GUARD

# (tick, pose): key(torso, arm, fore, blade, blade_out, shield fist, throw). Kort's shoulder-driven slash (the knight's
# SLASH_ROWS): a high lift and draw-back in the load, the shoulder carrying about 40% of the arc, the forearm nearly straight
# at contact with a wrist snap, the shoulder carrying on about 35 deg past the impact, and mid-arc keys so the drive never
# eases to a stop. The chop is the knight's overhead, the hew its low cut; the round shield keeps the dwarf's own fist path
# (it sits closer in than the knight's heater), and the recovery is stretched over the axe's two extra ticks.
SHIELD_FIST = {
    "overhead_chop": ((0, None), (4.5, (1.6, 20.0, 5.0)), (8, (1.4, 20.2, 4.8)), (9, (2.4, 19.5, 5.6)), (10, (3.4, 18.4, 6.2)),
                      (11.5, (4.0, 17.9, 5.8)), (13, (4.0, 17.9, 5.8)), (15.5, (3.2, 18.4, 5.8)), (19, (2.6, 18.8, 5.8))),
    "hook_hew": ((0, None), (3, (1.6, 19.2, 4.8)), (6, (1.4, 19.3, 4.6)), (7, (2.6, 18.8, 5.4)), (8, (3.8, 17.8, 6.0)),
                 (10, (4.2, 17.6, 5.6)), (13, (3.0, 18.4, 5.8)), (17, (2.2, 18.9, 6.0))),
}


def _fist(name, t):
    ks = [k for k in SHIELD_FIST[name] if k[1] is not None]
    return tuple(float(np.interp(t, [k[0] for k in ks], [k[1][c] for k in ks])) for c in range(3))


ATTACKS = {}
for _name, _src in SLASH_OF.items():
    ATTACKS[_name] = [(0, GUARD)] + [sw(_name, _late(r[0], IMPACT[_name]), *r[1:6], _fist(_name, r[0]), **r[7])
                                     for r in K.SLASH_ROWS[_src]] + [(ATTACK_TICKS, GUARD)]
# Clearance (the dwarf's breastplate is deeper than the knight's): the hew's arm swings a little wider across the chest,
# and the chop's follow-through lifts its bit off the breastplate.
AXE_FIT = {"hook_hew": (6.4, 9.4, (0, 0, 10, 0, 0, 0)), "overhead_chop": (11.0, 17.0, (0, 0, 0, 0, -15, 0))}
for _name, (_t0, _t1, _d) in AXE_FIT.items():
    ATTACKS[_name] = K.slash_fit(ATTACKS[_name], _t0, _t1, _d)
K.IMPACT.update(IMPACT)


# Review 1: struck from its right, the dwarf rolls face down over the shield arm; the round shield, turned the knight's way,
# stood up off the back like a lid. Its wrist turns the other way through the roll so the shield lies flat over the back.
K.ROLL_LEFT = K._sweep("left", {14: ((-120, 0, -30), -60, (90, 0, 0)), 15: ((-150, 0, -30), -60, (90, 0, 0)),
                                16: ((-180, 0, -30), -40, (45, 0, -45)),
                                **{t: ((-180, 0, -30), -20, (0, 0, -90)) for t in range(17, 21)}})

# Per-swing edge roll pacing (the knight builder's EDGE_RATE, deg per sample, and EDGE_IN, ticks of turn before the drive).
# overhead_chop (scorecard 2026-11-09): the bit's half turn ran at the knight's 25 deg/sample in two ticks of the load
# (right_hand 179.5 deg/100ms, limit 136.2, a flick). It now starts as the axe is lifted (6 ticks ahead of the drive) and turns
# at 15 deg/sample, so the head is turned over by the top of the load; the hold, the drive and the impact are unchanged.
EDGE_PACE = {"overhead_chop": (15.0, 6.0)}

# Batch review 2026-11-11. The bit is on the axe bone's local -x (the exported geo: haft at x 0.34-0.41, bit 0-0.31, back
# spike 0.44-0.75, as ic_breaker's), so the knight table's +1 rolled the head half round in the load and drove the SPIKE
# into the chop (render cmp_head.png, t11) and spun the axe in the hand on the way up (focus check grip_roll 59.6 / 41.6).
# With the edge on -x the roll is a quarter turn at most, so both swings turn at 15 deg/sample from a tick before the drive.
if TROOP == "ic_hearthguard":
    K.EDGE_SIGN = -1.0
    EDGE_PACE = {"overhead_chop": (15.0, 1.0), "hook_hew": (15.0, 1.0)}
# The deep cut pushed the rear knee guard and the rear boot under the ground (1.6 px, t11 / t9): the hips ride 1.2 px higher
# round the impact (a smooth bump, 3.5 ticks each side), so the lunge stays as deep to the eye. The pieces that hung still
# (pauldrons, tasset plates, the braid) get fullbody.soft_motion; the tabard keeps cloth_lag's lag, and in both
# follow-throughs it swings forward off the leading thigh (it was 1.9 px in it at t15). In the hew's cocked load the arm's
# raise turned the right pauldron's boss 2.5 px into the breastplate (t5.5): the plate slides 2 px out along the arm's own
# x from t3 to t9 and comes back (turning it, or sliding it down the arm, cleared nothing).
HIPS_BUMP = 1.2
TABARD_LIFT = 14.0                                   # deg, forward about its top edge, t12-18.5
HEW_PAULDRON = {"slide": (-2.0, 0.0, 0.0)}           # px, bone-local, t3-9
HEW_TABARD, HEW_TABARD_LIFT = (6.0, 11.5), 8.0       # the hew's drive throws the lagging tabard back into the coat (t8.5)


def _bump_hips(name, at=None, width=3.5, lift=HIPS_BUMP):
    at, hs = IMPACT[name] + 0.8 if at is None else at, []
    for h in K.ATTACK_LEGS[name]["hips"]:
        w = max(0.0, 1.0 - abs(h[0] - at) / width)
        w = 0.5 - 0.5 * math.cos(math.pi * w)
        hs.append((h[0], h[1], (h[2][0], h[2][1] + lift * w, h[2][2])) + tuple(h[3:]))
    K.ATTACK_LEGS[name]["hips"] = hs


def _push(samples, bone, t0, t1, rot=(0.0, 0.0, 0.0), pivot=None, slide=(0.0, 0.0, 0.0)):
    """Adds a hann-weighted turn (about `pivot`, model px) and slide to one bone's samples between t0 and t1."""
    b = fullbody._full_model(TROOP).bones[bone]
    for i, s in enumerate(samples):
        t = i / SUB
        if not t0 < t < t1:
            continue
        w = 0.5 - 0.5 * math.cos(2 * math.pi * (t - t0) / (t1 - t0))
        r0, p0 = s.get(bone, (Z3, None))
        e = np.array(rot, dtype=float) * w
        off = np.zeros(3)
        if pivot is not None and np.any(e):
            q = np.array([-pivot[0], pivot[1], pivot[2]]) / 16.0
            off = (np.eye(3) - fullbody._rot_of(e)) @ (q - b.pivot / 16.0) * 16.0
            off = np.array([-off[0], off[1], off[2]])
        s[bone] = (tuple(float(a + c) for a, c in zip(r0, e)),
                   tuple(float(a + c + w * d) for a, c, d in zip(p0 or Z3, off, slide)))


if TROOP == "ic_hearthguard":
    for _name in ATTACKS:
        _bump_hips(_name)
    # fix round 1: from the ready footing the hew's rear knee guard dipped 1.0 px under the ground at t11; the hips ride
    # 0.6 px higher there too
    _bump_hips("hook_hew", at=11.0, width=3.0, lift=0.6)


# ------------------------------------------------------------------------------------------------
# Stances (Kort's rule 6, 2026-11-10: every clip suits this unit and its weapon). The knight's stance set held a sword's
# guard: the blade forward and angled up past the rim. The hearthguard's are an axe-bearer's, built on the same skey()
# (blade = the haft's world pitch, + head down; the bit leads where the head pitches down, troop_rig.py's rest):
#   ready      the axe cocked at the shoulder, haft near upright, the bit to the foe, ready to chop; the round shield up to
#              the beard, over a lower, wider dwarf's footing.
#   brace      sunk behind the shield, the axe low past its right rim with the head dropped forward: the hook set to drag a
#              shield or a leg (hook_hew's pull).
#   wall       chest down behind the shield, the axe raised over the rim, poised to chop down over the front rank.
#   shield_up  the shield to the eyes, the axe drawn back high behind it, cocked to answer.
#   ward       backing off, the axe low and across, the bit hooked at the foe.
# The *_up forms raise the shield fist as the knight's do. The ready legs, the brace and the wall footing are set lower and
# wider than the knight's (a short, broad troop under a heavy shield).
# Batch review 2026-11-11 (focus check): the brace and brace_up axe arm was abducted 30 deg, which pushed the right pauldron's
# boss into the helm on the breath (armour_clip 2.56 px); it hangs at 14. The ward's arm 26 -> 18 for the same boss. The wall's
# shield fist rides 1.8 px higher (its rim went 2.7 px under the ground on the breath) and its footing sinks 2.5 px, not 3.9, with
# the rear foot 1.8 px nearer (the rear knee guard went 2.1 px under the ground); it still sits lower than the brace, chest further down.
# ------------------------------------------------------------------------------------------------
if TROOP == "ic_hearthguard":
    skey = K.skey
    K.STANCE_UPPER.update({
        "ready": skey((6, 18, 0), (-58, 0, 26), -70, -82, 16, (4.8, 19.8, 5.6), hips=(0, 8, 0)),
        "brace": skey((14, 18, 0), (-30, 0, 14), -40, 20, 6, (5.5, 18.5, 6.0), hips=(2, 12, 0), tilt=6),
        "brace_up": skey((15, 20, 0), (-52, 0, 14), -42, 18, 6, (5.5, 22.5, 6.0), hips=(2, 12, 0), tilt=2),
        "wall": skey((22, 16, 0), (-100, 0, 26), -50, -70, 14, (6.0, 18.8, 6.0), hips=(4, 16, 0), tilt=9),
        "wall_up": skey((24, 16, 0), (-108, 0, 26), -44, -66, 14, (6.0, 21.0, 6.0), hips=(4, 16, 0), tilt=4),
        "shield_up": skey((6, 18, 0), (-50, 0, 40), -90, -84, 30, (5.5, 22.0, 6.0)),
        "ward": skey((-6, 10, 0), (-26, 0, 18), -56, 0, 14, (5.0, 21.0, 5.6), tilt=-6),
    })
    K.STANCE_LEGS.update({
        "ready": {"hips": ((0.0, 8.0, 0.0), (0.0, -1.5, 0.2)), "right": (-3.6, 2.0, 2.4), "left": (3.4, 2.0, -2.4),
                  "yaw": (-14.0, 9.0)},
        "brace": {"hips": ((2.0, 12.0, 0.0), (0.0, -2.0, 0.4)), "right": (-3.4, 2.0, 3.0), "left": (3.2, 2.0, -3.0),
                  "yaw": (-18.0, 10.0)},
        "wall": {"hips": ((4.0, 16.0, 0.0), (0.0, -2.5, 0.6)), "right": (-3.6, 2.0, 4.0), "left": (3.4, 2.0, -2.2),
                 "yaw": (-22.0, 12.0)},
    })


# Review fix round 1 (2026-11-12): both swings started and ended on the knight's guard, not on this dwarf's own stance_ready
# (at t0 the forearm was 20 deg, the hand 20 deg and the arm 13.5 deg off it, so the axe jumped ~40 deg as the attack handed
# back to the stance). The first and last keys are now the ready stance's own first sample (its breath and weight shift at
# u = 0 included, as K.stance_upper / K.stance_legs compute it), the knight-guard recovery key is eased halfway onto it, and
# the feet start from and walk back to the ready footing. The follow-through carries on past the impact (STYLE rule 5,
# focus WARN phases): FOLLOW's (right_arm, torso) turn is added on a smooth bump peaking FOLLOW_AT ticks after the impact
# (the chop drives on down and the chest goes further over; the hew carries on across the body), then the recovery runs as
# before. Length and impact are unchanged.
FOLLOW = {"overhead_chop": ((8.0, 0.0, 0.0), (8.0, 0.0, 0.0)), "hook_hew": ((0.0, -12.0, 0.0), (0.0, -8.0, 0.0))}
FOLLOW_AT, FOLLOW_SPAN = 2.5, (0.6, 6.0)


def _ready_t0():
    """(upper key, hips (rot, pos), feet {side: (ankle px, yaw)}) of stance_ready at its first sample."""
    pose, spec = K.STANCE_UPPER["ready"], K.STANCE_LEGS["ready"]
    a_br, a_wt, a_lag = K.feel.stance_amps(K.STANCE_LIFE.get("ready", 1.0), True)
    wt, wl, lag = a_wt * math.sin(-K.WEIGHT), a_wt * math.sin(-K.WEIGHT - 0.7), a_lag * math.sin(-0.6)
    k = dict(pose)
    tx, ty, tz = pose["torso"]
    k["torso"] = (tx + 1.8 * wt, ty - 1.4 * wt, tz - 1.2 * wt)
    hx, hy, hz = pose["head"]
    k["head"] = (hx - 1.3 * wt, hy + 0.9 * wt, hz + 1.0 * wt)
    ax, ay, az = pose["right_arm"]
    k["right_arm"] = (ax - 1.3 * wl, ay, az)
    sx, sy, sz = pose["left_arm"]
    k["left_arm"] = (sx - 1.0 * wl, sy, sz)
    fx_, fy_, fz_ = pose["_face"]
    k["_face"] = (fx_ + 1.5 * lag, fy_ + 0.8 * lag, fz_ - 0.6 * lag)
    (rx, ry, rz), (px, py, pz) = spec["hips"]
    hips = ((rx + 1.5 * wt, ry - 2.2 * wt, rz + 1.6 * wt),
            (px - 0.35 * wt, py - 0.12 * abs(wt) - 0.22 * max(0.0, wt), pz - 0.6 * wt))
    feet = {"right": (spec["right"], spec["yaw"][0]), "left": (spec["left"], spec["yaw"][1])}
    return k, hips, feet


def _blend(a, b, f):
    return {n: tuple(x + f * (y - x) for x, y in zip(a[n], b[n])) for n in a}


if TROOP == "ic_hearthguard":
    READY_KEY, READY_HIPS_T0, READY_FEET_T0 = _ready_t0()
    for _name in ATTACKS:
        _ks = ATTACKS[_name]
        imp = IMPACT[_name]
        mid = []
        for _t, _k in _ks[1:-2]:
            a, b = imp + FOLLOW_SPAN[0], imp + FOLLOW_SPAN[1]
            if a < _t < b:
                u = (_t - a) / (FOLLOW_AT + imp - a) if _t <= imp + FOLLOW_AT else 1.0 - (_t - imp - FOLLOW_AT) / (b - imp - FOLLOW_AT)
                w = 0.5 - 0.5 * math.cos(math.pi * max(0.0, min(1.0, u)))
                _k = dict(_k)
                _k["right_arm"] = tuple(a + w * d for a, d in zip(_k["right_arm"], FOLLOW[_name][0]))
                _k["torso"] = tuple(a + w * d for a, d in zip(_k["torso"], FOLLOW[_name][1]))
            mid.append((_t, _k))
        ATTACKS[_name] = [(0, READY_KEY)] + mid + [(_ks[-2][0], _blend(_ks[-2][1], READY_KEY, 0.5)), (ATTACK_TICKS, READY_KEY)]
        _hs = K.ATTACK_LEGS[_name]["hips"]
        K.ATTACK_LEGS[_name]["hips"] = [(0,) + READY_HIPS_T0] + _hs[1:-1] + [(ATTACK_TICKS,) + READY_HIPS_T0]
        _st = []
        for s in K.ATTACK_LEGS[_name]["steps"]:      # the steps home land on the ready footing
            if tuple(s[3]) == tuple(K.READY_FEET[s[0]][0]):
                s = (s[0], s[1], s[2], READY_FEET_T0[s[0]][0], READY_FEET_T0[s[0]][1]) + tuple(s[5:])
            _st.append(s)
        K.ATTACK_LEGS[_name]["steps"] = _st


def attack_clips():
    n = ATTACK_TICKS * SUB
    out = {}
    soft = {k: v for k, v in fullbody.soft_parts_for().items() if k != "tabard"}
    for name, keys in ATTACKS.items():
        feet = K.READY_FEET
        if TROOP == "ic_hearthguard":
            K.READY_FEET = READY_FEET_T0               # the attack's feet start from the ready footing
        legs = K.attack_legs(K.ATTACK_LEGS[name], ticks=ATTACK_TICKS)
        K.READY_FEET = feet
        pace = (K.EDGE_RATE, K.EDGE_IN)
        K.EDGE_RATE, K.EDGE_IN = EDGE_PACE.get(name, pace)
        roll = K.edge_roll(name, keys, legs, n, report=True)
        K.EDGE_RATE, K.EDGE_IN = pace
        upper = cloth_lag(K.rolled(keys, n, roll), legs)   # the bit leads (EDGE_SIGN)
        if TROOP == "ic_hearthguard":
            upper, legs = fullbody.soft_motion(upper, legs, parts=soft, report=True)
            _push(upper, "tabard", 12.0, 18.5, rot=(TABARD_LIFT, 0.0, 0.0), pivot=(0.0, 18.0, 0.0))
            if name == "hook_hew":
                _push(upper, "tabard", *HEW_TABARD, rot=(HEW_TABARD_LIFT, 0.0, 0.0), pivot=(0.0, 18.0, 0.0))
                _push(upper, "right_pauldron", 3.0, 9.0, pivot=(-6.0, 24.5, 0.0), **HEW_PAULDRON)
        clip = K.bake(upper, ATTACK_TICKS, loop=False)
        clip["timeline"] = {K.fmt_t(IMPACT[name] / K.TPS): "impact"}
        out["attack_" + name] = clip
        out["attack_%s_legs" % name] = K.bake(legs, ATTACK_TICKS, loop=False)
    return out


def probe():
    """Where the axe goes in each swing: its lowest corner above the body's lowest (the soles), and the samples where it nears the ground."""
    for name, keys in ATTACKS.items():
        legs = K.attack_legs(K.ATTACK_LEGS[name], ticks=ATTACK_TICKS)
        low, below = 99.0, []
        for i in range(ATTACK_TICKS * SUB + 1):
            s = K.sample_keys(keys, i / SUB)
            pose = {b: (v[0], v[1] or Z3) for b, v in s.items() if not b.startswith("_")}
            pose.update({b: (v[0], v[1] or Z3) for b, v in legs[i].items()})
            P, owner = K.body_corners(pose)
            w = np.array(["weapon" in o for o in owner])
            h = P[w, 1].min() - P[~w, 1].min()
            low = min(low, h)
            if h < 0.5:
                below.append((i / SUB, round(float(h), 1)))
        print("%s: axe lowest %.1f px above the soles; under 0.5 px at %s" % (name, low, below))


def only_rig(anims):
    for clip in anims.values():
        clip["bones"] = {b: ch for b, ch in clip["bones"].items() if b in GEO_BONES}
    return anims


def main():
    if os.environ.get("AXE_PROBE"):
        probe()
        return
    idle_legs, idle_upper = K.idle_clips()
    anims = {"walk_legs": K.gait_legs(K.WALK), "walk_upper": K.gait_upper(K.WALK, K.WALK_CARRY),
             "run_legs": K.gait_legs(K.RUN), "run_upper": K.gait_upper(K.RUN, K.RUN_CARRY),
             "idle_legs": idle_legs, "idle_upper": idle_upper, **attack_clips(), **K.stance_clips(), **K.hit_clips(),
             **K.death_clips()}
    only_rig(anims)
    K.OUT.parent.mkdir(parents=True, exist_ok=True)
    K.OUT.write_text(json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1), encoding="utf-8")
    print("wrote", K.OUT, "clips:", ", ".join(anims))


if __name__ == "__main__":
    main()
