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

def attack_clips():
    n = ATTACK_TICKS * SUB
    out = {}
    for name, keys in ATTACKS.items():
        legs = K.attack_legs(K.ATTACK_LEGS[name], ticks=ATTACK_TICKS)
        upper = cloth_lag(K.rolled(keys, n, K.edge_roll(name, keys, legs, n, report=True)), legs)   # the bit leads (EDGE_SIGN)
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
