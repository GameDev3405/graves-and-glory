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

# The standing footwork (rule 4). An axe is all head: the chop sits deeper than the knight's overhead and the hips stay
# down through the bite, then rise as the axe is gathered; the hew coils right, steps in wide and drives left through it,
# then sits back on the hook pull.
K.ATTACK_LEGS = {
    "overhead_chop": {"hips": [(0, *READY_HIPS), (4.5, (-6, 12, 0), (0.1, -1.6, 0.9)), (8, (-9, 14, 0), (0.1, -1.9, 1.3)),
                               (10, (7, -7, 0), (-0.1, -3.0, -1.3)), (13, (8, -9, 0), (-0.1, -3.3, -1.4)),
                               (16, (7, -8, 0), (-0.1, -3.1, -1.3)), (20, (2, 2, 0), (0.0, -1.6, -0.6)),
                               (ATTACK_TICKS, *READY_HIPS)],
                      "steps": [("right", 7.0, 9.8, (-3.8, 2.0, 4.8), -10.0), ("right", 16.5, 20.5, (-3.4, 2.0, 2.0), -12.0)]},
    "hook_hew": {"hips": [(0, *READY_HIPS), (3, (-2, 14, 0), (0.3, -1.3, 0.3)), (6, (-3, 17, 0), (0.3, -1.5, 0.4)),
                          (8, (6, -8, -2), (-0.4, -2.8, -0.9)), (10.5, (7, -13, -2), (-0.5, -3.0, -1.0)),
                          (13, (2, -4, 0), (-0.2, -2.3, -0.3)), (17, (1, 4, 0), (0.0, -1.6, -0.3)),
                          (ATTACK_TICKS, *READY_HIPS)],
                 "steps": [("right", 4.5, 7.4, (-4.8, 2.0, 3.6), -18.0), ("right", 17.0, 21.5, (-3.4, 2.0, 2.0), -12.0)]},
}
sw, GUARD = K.swing, K.GUARD

# (tick, pose): key(torso, arm, fore, blade, blade_out, shield fist, throw). Each has the reel's phases (rule 5): a load held
# long enough to read with the shield pulled in to the chest (rule 7), a fast drive, an overshoot, a gather, an exact settle.
ATTACKS = {
    "overhead_chop": [   # impact 10: head. The axe climbs over the head and back, hangs there, then falls bit first.
        (0, GUARD),
        sw("overhead_chop", 4.5, (-12, 14, 0), (-165, 0, 18), -70, -150, 0, (1.6, 20.0, 5.0), throw=(-6, 4, 4)),
        sw("overhead_chop", 8, (-17, 18, 2), (-172, 0, 20), -76, -165, 0, (1.4, 20.2, 4.8), throw=(-6, 4, 4)),
        # Review 1: the drop reads over two frames (9 is mid-arc), the bit lands at chest height on the impact tick, the
        # follow-through carries it down to the bite, and the gather lifts it rather than leaving it to dangle.
        sw("overhead_chop", 9, (6, 0, 0), (-150, 0, 12), -26, -40, 0, (2.4, 19.5, 5.6)),
        sw("overhead_chop", 10, (14, -10, 0), (-118, 0, 6), -8, 10, 0, (3.4, 18.4, 6.2), throw=(-3, -5, -3)),
        sw("overhead_chop", 11.5, (24, -16, 0), (-84, 0, 2), -12, 30, -4, (4.0, 17.9, 5.8), throw=(-4, -6, -4)),
        sw("overhead_chop", 13, (25, -18, 0), (-76, 0, 2), -14, 26, -6, (4.0, 17.9, 5.8), throw=(-4, -6, -4)),
        sw("overhead_chop", 15.5, (16, 2, 0), (-62, 0, 8), -52, -20, -6, (3.2, 18.4, 5.8), throw=(-2, -3, -2)),
        sw("overhead_chop", 19, (8, 8, 0), (-40, 0, 12), -52, -34, -8, (2.6, 18.8, 5.8)),
        (ATTACK_TICKS, GUARD),
    ],
    "hook_hew": [   # impact 8: body. Cock high right, hold, hew down and across, then hook the bit back in.
        (0, GUARD),
        sw("hook_hew", 3, (-4, 30, 6), (-60, 0, 50), -60, -70, 60, (1.6, 19.2, 4.8), throw=(4, 8, 2)),
        sw("hook_hew", 6, (-6, 34, 8), (-64, 0, 54), -62, -76, 64, (1.4, 19.3, 4.6), throw=(5, 9, 2)),
        # Review 1: the hew comes across from high right (7) so the bit is already crossing the body on the impact tick,
        # carries on past the left hip (10), then the elbow folds and the bit is hooked back in toward the belly (13).
        sw("hook_hew", 7, (4, 8, -2), (-112, 0, 34), -24, -16, 30, (2.6, 18.8, 5.4), throw=(-1, -2, -1)),
        sw("hook_hew", 8, (16, -22, -6), (-100, 0, -16), -10, 18, -40, (3.8, 17.8, 6.0), throw=(-3, -6, -4)),
        sw("hook_hew", 10, (22, -32, -6), (-90, 0, -30), -16, 26, -62, (4.2, 17.6, 5.6), throw=(-4, -7, -5)),
        sw("hook_hew", 13, (12, -18, 0), (-46, 0, -24), -72, 6, -96, (3.0, 18.4, 5.8), throw=(-2, -3, -2)),
        sw("hook_hew", 17, (6, 12, 0), (-44, 0, 13), -52, -40, -10, (2.2, 18.9, 6.0)),
        (ATTACK_TICKS, GUARD),
    ],
}


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
        upper = cloth_lag([K.sample_keys(keys, i / SUB) for i in range(n + 1)], legs)
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
