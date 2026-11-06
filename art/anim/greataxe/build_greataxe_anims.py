"""Greataxe troops (unit animation rebuild): the spear builder's two-handed solve, footing, carry, hits and falls, with the
greataxe's own guard, stances and swings.

    python art/anim/greataxe/build_greataxe_anims.py            (writes art/anim/ic_breaker/out/ic_breaker.animation.json)
    GREATAXE_PROBE=1 ...                                         (each swing's axe lowest point above the soles, bit at impact)
    SPEAR_DUMP=<pose.json> SPEAR_ATTACK=<name|guard> SPEAR_TICKS=0,8,12 ...   (key poses for still_sp.py)

ic_breaker (axe family, sheets/design.json) swings a two-handed greataxe. Its three variants (Spec attackVariants):
  overhead_chop  impact 12, head, heavy: the axe heaved up over the right shoulder until its head hangs behind the back, held,
                 then brought over the top and down, bit first, the upper hand sliding down to the lower; it bites low and is
                 wrenched back up.
  side_hew       impact 12, body, heavy: the body coils right with the axe cocked back behind the right hip, held, then the hips
                 and shoulders unwind and drive it flat across from right to left, carrying on round past the left side.
  haft_jab       impact 7, body, light: the haft brought level across the chest, hands wide, and shoved straight out into the
                 foe (a cross-check), then pulled back to the guard.
Each clip lasts ATTACK_TICKS (28) inside the variant's 32-tick cooldown, carries the timeline "impact" marker on the server's
impact tick (AttackController syncs it) and has a matching attack_<name>_legs clip (STYLE.md rule 4).

The bit: the right fist turns the axe onto (d, u), d the shaft from the fist toward the head, and the bit points along
cross(u, d). Every key names the bit (or u) explicitly, so the bit leads each swing: the chop's swing plane is sagittal (u ~ +x),
the hew's level (u ~ +y).
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
os.environ["GG_SPEAR_TROOP"] = os.environ.get("GG_GREATAXE_TROOP", "ic_breaker")
sys.path.insert(0, str(HERE.parent / "spear"))
import build_spear_anims as SP  # noqa: E402

K, np, unit, aim = SP.K, SP.np, SP.unit, SP.aim
SUB = K.SUB
READY_HIPS = SP.READY_HIPS


def AKEY(torso, gaze, O, d, bit=None, u=None, Ls=6.0, rp=(-20, 0, 10, -60), lp=(-60, -30, 0, -40)):
    """A key with the bit named: bit (a world direction, squared to the shaft) or u directly (bit = cross(u, d))."""
    d = unit(d)
    if bit is not None:
        b = np.asarray(bit, float)
        b = unit(b - (b @ d) * d)
        u = np.cross(d, b)
    # The hand's turn (hold_wrist) lays the bit (weapon-local +x) along cross(d, u'), so the solver's u' is minus the swing
    # plane's u named here (checked with bit_dir: the guard's bit points at the foe).
    u = -np.asarray(u, float)
    u = unit(u - (u @ d) * d)
    return {"torso": tuple(map(float, torso)), "head": tuple(map(float, gaze)), "O": tuple(map(float, O)), "d": tuple(d),
            "u": tuple(u), "Ls": (float(Ls),), "rp": tuple(map(float, rp)), "lp": tuple(map(float, lp))}


X, Y, Zf = (1.0, 0.0, 0.0), (0.0, 1.0, 0.0), (0.0, 0.0, 1.0)

# The guard: the axe held across the body at the high port, the right fist low before the belly, the head up by the right
# shoulder and the bit toward the foe, the left fist up the haft. From here the chop needs only a heave, the hew a coil.
GUARD = AKEY((12, 12, 0), (4, 0, 0), (-2.2, 14.6, 4.0), (-7.5, 17.0, -2.0), bit=Zf, Ls=6.0, rp=(-35, 0, -10, -70),
             lp=(-50, -20, -20, -60))
ATTACK_TICKS = 28
IMPACT = {"overhead_chop": 12, "side_hew": 12, "haft_jab": 7}

UP_CHOP = X     # the chop's swing plane is the body's own: its normal points to the troop's left

ATTACKS = {
    "overhead_chop": [   # impact 12: head. Heave, hang, then over the top and down.
        (0, GUARD),
        (4, AKEY((-4, 6, 0), (2, 0, 0), (-3.8, 20.0, 3.0), aim(75, -8), u=UP_CHOP, Ls=6.5, rp=(-120, 0, 10, -70), lp=(-110, -10, 0, -70))),
        (8, AKEY((-14, 10, 0), (-2, 0, 0), (-1.0, 23.4, 0.2), aim(150, -10), u=UP_CHOP, Ls=7.0, rp=(-165, 0, 15, -70), lp=(-160, -10, 0, -60))),
        (10, AKEY((-16, 11, 0), (-2, 0, 0), (-0.9, 23.6, 0.0), aim(156, -10), u=UP_CHOP, Ls=7.0, rp=(-168, 0, 15, -72), lp=(-162, -10, 0, -60))),
        (11, AKEY((6, 6, 0), (6, 0, 0), (-2.0, 22.0, 4.5), aim(75, -4), u=UP_CHOP, Ls=4.5, rp=(-150, 0, 10, -40), lp=(-140, -10, 0, -40))),
        # Review 1: a mid-arc key so the drop reads over two frames rather than snapping.
        (11.5, AKEY((12, 2, 0), (9, 0, 0), (-2.8, 19.0, 7.6), aim(40, 14), u=UP_CHOP, Ls=3.5, rp=(-115, 0, 8, -25), lp=(-110, -10, 0, -25))),
        (12, AKEY((18, -2, 0), (12, 0, 0), (-3.5, 15.5, 9.5), aim(4, 28), u=UP_CHOP, Ls=2.5, rp=(-80, 0, 5, -10), lp=(-80, -10, 0, -10))),
        (14, AKEY((24, -4, 0), (16, 0, 0), (-4.0, 13.5, 8.8), aim(-13, 30), u=UP_CHOP, Ls=2.2, rp=(-60, 0, 5, -10), lp=(-60, -10, 0, -10))),
        (17, AKEY((22, -3, 0), (14, 0, 0), (-4.0, 13.8, 8.8), aim(-11, 30), u=UP_CHOP, Ls=2.4, rp=(-60, 0, 5, -12), lp=(-60, -10, 0, -12))),
        (21, AKEY((18, 6, 0), (8, 0, 0), (-3.5, 15.8, 7.2), aim(52, 0), bit=Zf, Ls=4.5)),
        (25, SP.nudge(GUARD, O=(0.2, 0.3, 0.4), torso=(2, -1, 0))),
        (ATTACK_TICKS, GUARD),
    ],
    "side_hew": [   # impact 12: body. Coil right, hold, unwind flat across from right to left, carry on round.
        (0, GUARD),
        (4, AKEY((6, 16, -3), (4, 0, 0), (-3.2, 17.8, 2.8), aim(30, -110), u=(0.15, 1, 0), Ls=6.5, rp=(-40, 0, 40, -60), lp=(-70, -30, -30, -60))),
        (8, AKEY((4, 22, -6), (4, 0, 0), (-3.0, 18.2, 2.4), aim(30, -150), u=(0.2, 1, 0), Ls=7.0, rp=(-30, 0, 55, -55), lp=(-75, -40, -40, -70))),
        (10, AKEY((3, 23, -6), (4, 0, 0), (-3.0, 18.3, 2.3), aim(30, -155), u=(0.2, 1, 0), Ls=7.0, rp=(-30, 0, 56, -55), lp=(-75, -40, -40, -70))),
        (11, AKEY((10, 14, -2), (6, 0, 0), (-4.5, 16.5, 6.5), aim(14, -100), u=(0.1, 1, 0), Ls=4.5, rp=(-60, 0, 30, -30), lp=(-70, -30, -20, -40))),
        (11.5, AKEY((11, 2, -1), (7, 0, 0), (-5.5, 15.0, 8.0), aim(8, 12), u=(0.05, 1, 0), Ls=3.5, rp=(-70, 0, 15, -20), lp=(-72, -20, -10, -25))),
        (12, AKEY((12, -10, 0), (8, 0, 0), (-2.0, 14.5, 9.0), aim(4, 42), u=Y, Ls=2.5, rp=(-75, 0, 5, -10), lp=(-75, -10, 0, -10))),
        (14, AKEY((22, -30, 4), (8, 0, 0), (3.0, 13.5, 7.5), aim(-2, 85), u=Y, Ls=2.2, rp=(-70, 0, -20, -20), lp=(-60, 0, 20, -20))),
        (17, AKEY((20, -34, 4), (8, 0, 0), (4.0, 13.0, 6.5), aim(-5, 110), u=Y, Ls=2.4, rp=(-65, 0, -30, -25), lp=(-55, 0, 25, -25))),
        (21, AKEY((14, -4, 0), (6, 0, 0), (-3.2, 15.0, 7.5), aim(68, -80), bit=Zf, Ls=4.5)),
        (25, SP.nudge(GUARD, O=(0.2, 0.2, 0.4), torso=(1, -1, 0))),
        (ATTACK_TICKS, GUARD),
    ],
    "haft_jab": [   # impact 7: body. The haft level across the chest, hands wide, shoved straight out.
        (0, GUARD),
        (3, AKEY((-2, 8, 0), (2, 0, 0), (-4.5, 16.0, 3.2), aim(10, 84), bit=Y, Ls=9.0, rp=(-60, 0, 20, -90), lp=(-60, -10, -10, -80))),
        (5.5, AKEY((-5, 9, 0), (2, 0, 0), (-4.6, 16.2, 3.0), aim(12, 86), bit=Y, Ls=9.0, rp=(-60, 0, 20, -95), lp=(-60, -10, -10, -85))),
        (7, AKEY((22, 2, 0), (8, 0, 0), (-2.5, 16.0, 9.0), aim(10, 76), bit=Y, Ls=9.0, rp=(-85, 0, 10, -20), lp=(-85, -10, 0, -20))),
        (9, AKEY((25, 0, 0), (8, 0, 0), (-2.5, 16.2, 9.8), aim(9, 74), bit=Y, Ls=9.0, rp=(-88, 0, 10, -15), lp=(-88, -10, 0, -15))),
        # Review 1: pulled back in and lifted (not swung out forward, which read as a second, spear-like thrust).
        (13, AKEY((14, 8, 0), (5, 0, 0), (-3.0, 15.8, 4.8), aim(55, 70), bit=(0.0, 0.6, 1.0), Ls=7.5)),
        (19, SP.nudge(GUARD, O=(0.2, 0.3, 0.5), torso=(1, -1, 0))),
        (ATTACK_TICKS, GUARD),
    ],
}

T = ATTACK_TICKS
ATTACK_LEGS = {
    "overhead_chop": {"hips": [(0, *READY_HIPS), (4, (-4, 24, 0), (0.2, -1.4, 0.6)), (8, (-7, 27, 0), (0.2, -1.5, 1.0)),
                               (10, (-8, 28, 0), (0.2, -1.6, 1.1)), (12, (8, 10, 0), (-0.2, -3.0, -1.6)),
                               (14, (10, 6, 0), (-0.3, -3.5, -1.9)), (17, (9, 8, 0), (-0.3, -3.3, -1.8)),
                               (22, (2, 16, 0), (0.0, -1.8, -0.5)), (T, *READY_HIPS)],
                      "steps": [("left", 9.8, 11.8, (3.6, 2.0, 5.6), 12.0, 1.4), ("left", 18.0, 23.0, (3.4, 2.0, 2.4), 14.0, 1.0)]},
    "side_hew": {"hips": [(0, *READY_HIPS), (4, (-2, 30, 0), (0.3, -1.6, 0.6)), (8, (-3, 38, 0), (0.3, -1.8, 0.8)),
                          (10, (-3, 40, 0), (0.3, -1.9, 0.9)), (11, (2, 20, -1), (0.0, -2.5, -0.4)),
                          (12, (6, 2, -2), (-0.3, -3.0, -1.2)), (14, (8, -10, -3), (-0.4, -3.2, -1.3)),
                          (17, (6, -8, -2), (-0.3, -3.0, -1.1)), (22, (2, 12, 0), (0.0, -1.8, -0.4)), (T, *READY_HIPS)],
                 "steps": [("left", 9.5, 11.6, (4.6, 2.0, 5.0), 6.0, 1.4), ("right", 11.5, 14.5, (-2.6, 2.0, -1.6), -28.0, 0.0),
                           ("left", 18.0, 23.0, (3.4, 2.0, 2.4), 14.0, 1.0), ("right", 20.0, 25.0, (-3.2, 2.0, -2.2), -6.0, 0.6)]},
    "haft_jab": {"hips": [(0, *READY_HIPS), (3, (-4, 24, 0), (0.2, -1.6, 0.8)), (5.5, (-5, 26, 0), (0.2, -1.7, 1.0)),
                          (7, (6, 12, 0), (-0.2, -2.8, -2.2)), (9, (7, 10, 0), (-0.3, -3.0, -2.5)),
                          (13, (4, 14, 0), (0.0, -2.3, -1.4)), (19, (0, 19, 0), (0.0, -1.5, -0.3)), (T, *READY_HIPS)],
                 "steps": [("left", 4.8, 6.8, (3.6, 2.0, 6.4), 12.0, 1.4), ("left", 14.0, 19.0, (3.4, 2.0, 2.4), 14.0, 1.0)]},
}

STANCE_KEYS = {
    "ready": GUARD,
    # brace: sunk over the braced legs, the axe head held up before the face to take a charge on the haft
    "brace": AKEY((18, 12, 0), (2, 0, 0), (-1.5, 11.5, 5.0), (-4.0, 17.0, 2.0), bit=Zf, Ls=6.5),
    # wall: the formed line, crouched, the head forward at chest height, bit out
    "wall": AKEY((20, 14, 0), (6, 0, 0), (-2.5, 13.0, 5.0), aim(35, 4), bit=Zf, Ls=5.5),
    # ward: giving ground, upright, the haft raised level across the chest to fend
    "ward": AKEY((4, 12, 0), (0, 0, 0), (-3.5, 16.0, 2.0), aim(14, 80), bit=Y, Ls=9.0),
}
STANCE_KEYS["shield_up"] = STANCE_KEYS["ready"]


def attack_clip(name):
    keys, legs = ATTACKS[name], K.attack_legs(ATTACK_LEGS[name], ticks=ATTACK_TICKS)
    sol = SP.Solver()
    out, hits = [], 0
    for i in range(ATTACK_TICKS * SUB + 1):
        s = SP.sample(keys, legs[i], sol, i / SUB)
        h = SP.shaft_hits(SP.fk_pose(s, legs[i]))
        if h and os.environ.get("GREATAXE_PROBE"):
            print("  shaft in body at t%.2f: %s" % (i / SUB, h[:2]))
        hits += bool(h)
        out.append(s)
    print("attack_%s: grip err max %.3f px, left fist err max %.3f px, samples with the shaft in the body %d"
          % (name, sol.worst["right"], sol.worst["left"], hits))
    return out, legs


BIT_EDGE, BIT_BACK = (2.0, 29.0, 0.0), (-6.0, 29.0, 0.0)   # weapon-local: the bit's edge and the shaft behind it


def bit_dir(pose):
    return unit(SP.wpoint(pose, BIT_EDGE) - SP.wpoint(pose, BIT_BACK))


def probe():
    """Each swing: the axe's lowest corner above the body's lowest (the soles), the samples under 0.5 px, and where the bit
    points at the impact tick (it must lead: down in the chop, toward the troop's left in the hew)."""
    for name in ATTACKS:
        upper, legs = attack_clip(name)
        low, below = 99.0, []
        for i, (s, lg) in enumerate(zip(upper, legs)):
            pose = SP.fk_pose(s, lg)
            P, owner = SP._CORNERS(pose)
            w = np.array(["weapon" in o for o in owner])
            h = P[w, 1].min() - P[~w, 1].min()
            low = min(low, h)
            if h < 0.5:
                below.append((i / SUB, round(float(h), 1)))
        bits = {t: np.round(bit_dir(SP.fk_pose(upper[t * SUB], legs[t * SUB])), 2).tolist()
                for t in sorted({0, IMPACT[name] - 1, IMPACT[name], IMPACT[name] + 2})}
        print("  %s: axe lowest %.1f px above the soles; under 0.5 px at %s; bit by tick %s" % (name, low, below, bits))


SP.GUARD, SP.ATTACKS, SP.IMPACT, SP.ATTACK_LEGS, SP.ATTACK_TICKS = GUARD, ATTACKS, IMPACT, ATTACK_LEGS, ATTACK_TICKS
SP.STANCE_KEYS, SP.attack_clip = STANCE_KEYS, attack_clip

if __name__ == "__main__":
    if os.environ.get("GREATAXE_PROBE"):
        probe()
    else:
        SP.main()
