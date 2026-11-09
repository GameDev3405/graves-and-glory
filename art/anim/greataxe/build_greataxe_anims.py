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
                 and shoulders unwind and drive it flat across from right to left, carrying on round past the left side
                 (v2: the recovery then raises the axe up in front on the left, rolling it evenly back to the guard).
  haft_jab       impact 7, body, light: the haft brought level across the chest, hands wide, and shoved straight out into the
                 foe (a cross-check), then pulled back to the guard.
Each clip lasts ATTACK_TICKS (28) inside the variant's 32-tick cooldown, carries the timeline "impact" marker on the server's
impact tick (AttackController syncs it) and has a matching attack_<name>_legs clip (STYLE.md rule 4).

The bit: the right fist turns the axe onto (d, u), d the shaft from the fist toward the head, and the bit points along
cross(u, d). Every key names the bit (or u) explicitly, so the bit leads each swing: the chop's swing plane is sagittal (u ~ +x),
the hew's level (u ~ +y).
"""
from __future__ import annotations

import math
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

if SP.V2C:
    # v2 feel (the Knight's): the pelvis loads first and fires first. Every swing loads the hips back over the rear (right)
    # foot during the wind-up, the hips start forward a beat before the arms leave the top, drive over the planted lead on
    # the impact, a rear catch step follows, then both feet come home. The chop's settle no longer lifts at atk 20.
    ATTACKS["overhead_chop"][-3] = (21, AKEY((18, 6, 0), (8, 0, 0), (-3.4, 15.4, 7.2), aim(48, 0), bit=Zf, Ls=4.6))
    # The mid-arc keys carry the butt clear of the chest now that the pelvis leads the drop.
    _ci = [t for t, _ in ATTACKS["overhead_chop"]].index(11.5)
    ATTACKS["overhead_chop"][_ci] = (11.5, AKEY((12, 2, 0), (9, 0, 0), (-2.8, 19.2, 8.6), aim(40, 14), u=UP_CHOP, Ls=3.5,
                                               rp=(-115, 0, 8, -25), lp=(-110, -10, 0, -25)))
    _hi = [t for t, _ in ATTACKS["side_hew"]].index(11.5)
    ATTACKS["side_hew"].insert(_hi, (11.25, AKEY((10.5, 16, -1.5), (6.5, 0, 0), (-4.6, 15.8, 9.0), aim(11, -76), u=(0.08, 1, 0),
                                                 Ls=3.5, rp=(-65, 0, 22, -25), lp=(-71, -25, -15, -32))))
    # The hew's recovery raises the axe up in front on the left instead of carrying it on round behind the back: the old
    # path left the right hand a full turn off its start, so the clip spun the arm round while it faded back to the stance.
    _ri = [t for t, _ in ATTACKS["side_hew"]].index(21)
    ATTACKS["side_hew"][_ri] = (21, AKEY((14, -4, 0), (6, 0, 0), (-3.2, 15.0, 7.5), aim(68, 30), bit=Zf, Ls=4.5))
    # The axe rolls a long way between the follow-through (t17) and that key, and a straight blend of d and u passes u
    # through the shaft (the hand flips over in a quarter tick). A t19 key on the shortest turn between the two keeps it
    # rolling evenly; its other channels are the curve's own values there.
    def _turn_key(keys, t, ta, tb):
        ka, kb = dict(keys)[ta], dict(keys)[tb]

        def F(k):
            d, u = np.asarray(k["d"]), np.asarray(k["u"])
            return np.column_stack([d, u, np.cross(d, u)])
        Fa = F(ka)
        Rr = F(kb) @ Fa.T
        th = math.acos(max(-1.0, min(1.0, (np.trace(Rr) - 1) / 2)))
        ax = unit(np.array([Rr[2, 1] - Rr[1, 2], Rr[0, 2] - Rr[2, 0], Rr[1, 0] - Rr[0, 1]]))
        h, Kx = th * (t - ta) / (tb - ta), np.array([[0, -ax[2], ax[1]], [ax[2], 0, -ax[0]], [-ax[1], ax[0], 0]])
        Fm = (np.eye(3) + math.sin(h) * Kx + (1 - math.cos(h)) * Kx @ Kx) @ Fa
        k = {n: tuple(float(v) for v in np.ravel(SP.interp(keys, t, n))) for n in ka}
        k["d"], k["u"] = tuple(Fm[:, 0]), tuple(Fm[:, 1])
        return k
    ATTACKS["side_hew"].insert(_ri, (19, _turn_key(ATTACKS["side_hew"], 19, 17, 21)))
    _HOME =[("left", 19.0, 22.5, (3.4, 2.0, 2.4), 14.0, 1.0), ("right", 22.0, 25.5, (-3.2, 2.0, -2.2), -6.0, 0.8)]
    ATTACK_LEGS = {
        "overhead_chop": {"hips": [(0, *READY_HIPS), (3, (-3, 25, -0.5), (-0.3, -1.5, 0.7)), (6.5, (-7, 30, -1.5), (-0.6, -1.9, 1.3)),
                                   (9.3, (-8, 30, -1.5), (-0.6, -2.0, 1.3)), (10.5, (-3, 25, 0), (-0.3, -2.3, 0.6)),
                                   (12, (9, 8, 1), (0.4, -3.2, -1.8)), (14, (11, 5, 1), (0.5, -3.6, -2.1)),
                                   (17, (9, 8, 0), (0.3, -3.2, -1.7)), (22, (3, 16, 0), (0.0, -1.9, -0.6)), (T, *READY_HIPS)],
                          "steps": [("left", 9.8, 11.8, (3.6, 2.0, 5.6), 12.0, 1.4), ("right", 14.5, 17.5, (-3.1, 2.0, -0.2), -8.0, 1.0)]
                          + _HOME},
        "side_hew": {"hips": [(0, *READY_HIPS), (3, (-1, 28, 0), (-0.3, -1.6, 0.6)), (6.5, (-3, 38, -1), (-0.6, -1.9, 1.0)),
                              (9.3, (-3, 41, -1), (-0.6, -2.0, 1.0)), (10.5, (-1, 32, -1), (-0.4, -2.2, 0.5)),
                              (12, (6, 2, -2), (0.3, -3.1, -1.2)), (14, (8, -10, -3), (0.4, -3.3, -1.3)),
                              (17, (6, -8, -2), (0.3, -3.0, -1.1)), (22, (2, 12, 0), (0.0, -1.8, -0.4)), (T, *READY_HIPS)],
                     "steps": ATTACK_LEGS["side_hew"]["steps"]},
        "haft_jab": {"hips": [(0, *READY_HIPS), (2.5, (-3, 24, -0.5), (-0.4, -1.6, 0.9)), (5.0, (-5, 27, -1), (-0.6, -1.8, 1.2)),
                              (6.0, (-1, 20, 0), (-0.2, -2.2, 0.2)), (7, (7, 10, 1), (0.4, -2.9, -2.2)),
                              (9, (8, 8, 1), (0.5, -3.1, -2.5)), (13, (4, 14, 0), (0.2, -2.3, -1.6)), (19, (0, 19, 0), (0.0, -1.5, -0.4)),
                              (T, *READY_HIPS)],
                     "steps": [("left", 4.8, 6.8, (3.6, 2.0, 5.5), 12.0, 1.3), ("right", 9.0, 12.0, (-3.1, 2.0, 0.0), -8.0, 1.0),
                               ("left", 14.0, 18.5, (3.4, 2.0, 2.4), 14.0, 1.0), ("right", 17.0, 20.5, (-3.2, 2.0, -2.2), -6.0, 0.8)]},
    }
    # Stances that read apart at gameplay scale: brace sunk with the haft upright before the face, wall crouched forward with
    # the head levelled at the foe, ward leaning back with the haft high and level across the chest.
    STANCE_KEYS["brace"] = AKEY((14, 12, 0), (2, 0, 0), (-1.5, 10.8, 5.2), (-3.0, 17.0, 1.6), bit=Zf, Ls=7.0)
    STANCE_KEYS["wall"] = AKEY((26, 16, 0), (8, 0, 0), (-2.6, 12.4, 6.2), aim(18, 4), bit=Zf, Ls=5.0)
    STANCE_KEYS["ward"] = AKEY((-4, 12, 0), (-2, 0, 0), (-3.5, 16.8, 1.6), aim(16, 80), bit=Y, Ls=9.0)


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


# Slash rollout 3 (Kort's shoulder-driven slash recipe, after the Knight's): the overhead chop's load sits clear of the helm
# (fists to the right of the head, hands closer on the haft), then the drive is shared out along the chain instead of
# flicked from the wrists: hips lead (peak turn t11.0), torso, shoulder and an extending elbow (forearm -11 at contact
# instead of -82), and the bit is rolled to lead its own motion around contact. Keys designed in arm space at 4 per tick
# (each key's right-arm prior is the designed arm), then baked here; the impact stays on t12.
if SP.V2C:
    _SLASH3 = {'overhead_chop': {'drop': [],
                       'keys': [(8.0,
                                 {'torso': (-14.0, 10.0, 0.0),
                                  'head': (-2.0, 0.0, 0.0),
                                  'O': (-4.14, 22.41, 0.18),
                                  'd': (0.0882, 0.829, -0.5522),
                                  'u': (-0.9939, 0.0367, -0.1036),
                                  'Ls': (2.5,),
                                  'rp': (-165.0, 0.0, 15.0, -70.0),
                                  'lp': (-160.0, -10.0, 0.0, -60.0)}),
                                (10.0,
                                 {'torso': (-16.0, 11.0, 0.0),
                                  'head': (-2.0, 0.0, 0.0),
                                  'O': (-4.04, 22.61, -0.02),
                                  'd': (0.1013, 0.766, -0.6348),
                                  'u': (-0.9924, 0.0334, -0.1181),
                                  'Ls': (2.5,),
                                  'rp': (-168.0, 0.0, 15.0, -72.0),
                                  'lp': (-162.0, -10.0, 0.0, -60.0)}),
                                (10.25,
                                 {'torso': (-13.2674, 10.5148, 0.0),
                                  'head': (-1.0664, 0.0, 0.0),
                                  'O': (-3.1476, 20.9521, 1.5559),
                                  'd': (0.0234, 0.8547, -0.5187),
                                  'u': (-0.9975, 0.0542, 0.0443),
                                  'Ls': (2.8125,),
                                  'rp': (-96.2452, -32.9984, 55.9934, -43.4881),
                                  'lp': (-160.1536, -10.0, 0.0, -58.0256)}),
                                (10.5,
                                 {'torso': (-6.8797, 9.2895, 0.0),
                                  'head': (1.1562, 0.0, 0.0),
                                  'O': (-3.512, 19.8578, 2.9946),
                                  'd': (-0.0131, 0.9272, -0.3744),
                                  'u': (-0.9797, -0.0869, -0.1809),
                                  'Ls': (3.5,),
                                  'rp': (-94.6607, -36.5037, 53.846, -40.0),
                                  'lp': (-155.2429, -10.0, 0.0, -53.0682)}),
                                (10.75,
                                 {'torso': (0.4478, 7.6694, 0.0),
                                  'head': (3.8008, 0.0, 0.0),
                                  'O': (-3.9356, 18.2273, 4.5653),
                                  'd': (-0.0028, 0.989, -0.1479),
                                  'u': (-0.9977, -0.0127, -0.0662),
                                  'Ls': (4.1875,),
                                  'rp': (-89.5628, -39.4731, 49.8209, -36.0),
                                  'lp': (-148.2107, -10.0, 0.0, -46.5767)}),
                                (11.0,
                                 {'torso': (6.0, 6.0, 0.0),
                                  'head': (6.0, 0.0, 0.0),
                                  'O': (-3.8784, 16.6445, 6.0351),
                                  'd': (0.0091, 0.9962, 0.0867),
                                  'u': (-0.9248, -0.0246, 0.3797),
                                  'Ls': (4.5,),
                                  'rp': (-84.441, -41.4683, 44.4836, -32.0),
                                  'lp': (-140.0, -10.0, 0.0, -40.0)}),
                                (11.25,
                                 {'torso': (9.1899, 4.1053, 0.0),
                                  'head': (7.5469, 0.0, 0.0),
                                  'O': (-3.5663, 14.8714, 7.0613),
                                  'd': (0.1239, 0.8953, 0.4279),
                                  'u': (-0.8774, -0.1026, 0.4687),
                                  'Ls': (4.125,),
                                  'rp': (-75.9661, -43.8297, 37.6857, -27.0),
                                  'lp': (-126.6286, -10.0, 0.0, -32.8409)}),
                                (11.5,
                                 {'torso': (12.0, 2.0, 0.0),
                                  'head': (9.0, 0.0, 0.0),
                                  'O': (-2.6065, 13.2863, 7.5206),
                                  'd': (0.2048, 0.6691, 0.7144),
                                  'u': (-0.8144, -0.2884, 0.5036),
                                  'Ls': (3.5,),
                                  'rp': (-67.654, -48.7866, 29.05, -22.0),
                                  'lp': (-110.0, -10.0, 0.0, -25.0)}),
                                (11.75,
                                 {'torso': (15.4091, -0.3684, 0.0),
                                  'head': (10.6667, 0.0, 0.0),
                                  'O': (-1.7661, 12.1241, 7.7041),
                                  'd': (0.3574, 0.3775, 0.8543),
                                  'u': (-0.7875, -0.3699, 0.4929),
                                  'Ls': (2.8961,),
                                  'rp': (-60.358, -51.565, 20.8582, -18.0),
                                  'lp': (-92.5, -10.0, 0.0, -15.625)}),
                                (12.0,
                                 {'torso': (18.0, -2.0, 0.0),
                                  'head': (12.0, 0.0, 0.0),
                                  'O': (-1.0495, 11.4586, 7.3443),
                                  'd': (0.4537, 0.0349, 0.8905),
                                  'u': (-0.8587, -0.2503, 0.4473),
                                  'Ls': (2.5,),
                                  'rp': (-53.2399, -54.8898, 12.289, -14.0),
                                  'lp': (-80.0, -10.0, 0.0, -10.0)}),
                                (12.25,
                                 {'torso': (19.3018, -2.4889, 0.0),
                                  'head': (12.8099, 0.0, 0.0),
                                  'O': (-0.7335, 11.4935, 7.911),
                                  'd': (0.4687, -0.0523, 0.8818),
                                  'u': (-0.8781, -0.1361, 0.4587),
                                  'Ls': (2.4226,),
                                  'rp': (-57.0195, -53.4765, 11.215, -13.0),
                                  'lp': (-75.3125, -10.0, 0.0, -10.0)}),
                                (12.5,
                                 {'torso': (20.4716, -2.9046, 0.0),
                                  'head': (13.5625, 0.0, 0.0),
                                  'O': (-0.4538, 11.5033, 8.3578),
                                  'd': (0.4801, -0.1392, 0.8661),
                                  'u': (-0.8757, -0.1348, 0.4637),
                                  'Ls': (2.3583,),
                                  'rp': (-60.9452, -52.9178, 11.3998, -12.0),
                                  'lp': (-71.25, -10.0, 0.0, -10.0)}),
                                (12.75,
                                 {'torso': (21.4964, -3.2496, 0.0),
                                  'head': (14.2422, 0.0, 0.0),
                                  'O': (-0.2515, 11.4861, 8.7523),
                                  'd': (0.4841, -0.1822, 0.8558),
                                  'u': (-0.6647, 0.5595, 0.4951),
                                  'Ls': (2.3063,),
                                  'rp': (-64.8699, -52.352, 12.4032, -11.0),
                                  'lp': (-67.8125, -10.0, 0.0, -10.0)}),
                                (13.0,
                                 {'torso': (22.3636, -3.5263, 0.0),
                                  'head': (14.8333, 0.0, 0.0),
                                  'O': (-0.0673, 11.4953, 9.1128),
                                  'd': (0.4872, -0.225, 0.8438),
                                  'u': (-0.6424, 0.5623, 0.5208),
                                  'Ls': (2.2657,),
                                  'rp': (-68.8137, -51.8886, 13.5959, -10.0),
                                  'lp': (-65.0, -10.0, 0.0, -10.0)}),
                                (13.25,
                                 {'torso': (23.0604, -3.7373, 0.0),
                                  'head': (15.3203, 0.0, 0.0),
                                  'O': (0.0588, 11.4648, 9.336),
                                  'd': (0.4841, -0.2504, 0.8384),
                                  'u': (-0.7869, 0.2944, 0.5422),
                                  'Ls': (2.2357,),
                                  'rp': (-70.7936, -51.6527, 14.5454, -10.5),
                                  'lp': (-62.8125, -10.0, 0.0, -10.0)}),
                                (13.5,
                                 {'torso': (23.5739, -3.8849, 0.0),
                                  'head': (15.6875, 0.0, 0.0),
                                  'O': (0.1666, 11.4792, 9.5445),
                                  'd': (0.4806, -0.2756, 0.8325),
                                  'u': (-0.8623, 0.0243, 0.5059),
                                  'Ls': (2.2153,),
                                  'rp': (-72.7615, -51.4054, 15.4416, -11.0),
                                  'lp': (-61.25, -10.0, 0.0, -10.0)}),
                                (13.75,
                                 {'torso': (23.8913, -3.9716, 0.0),
                                  'head': (15.9193, 0.0, 0.0),
                                  'O': (0.1989, 11.4029, 9.5639),
                                  'd': (0.48, -0.2798, 0.8314),
                                  'u': (-0.8623, 0.024, 0.5059),
                                  'Ls': (2.2037,),
                                  'rp': (-72.7776, -51.4625, 15.8444, -11.5),
                                  'lp': (-60.3125, -10.0, 0.0, -10.0)}),
                                (14.0,
                                 {'torso': (24.0, -4.0, 0.0),
                                  'head': (16.0, 0.0, 0.0),
                                  'O': (0.2123, 11.391, 9.5874),
                                  'd': (0.4794, -0.284, 0.8304),
                                  'u': (-0.8622, 0.0238, 0.506),
                                  'Ls': (2.2,),
                                  'rp': (-72.7935, -51.438, 16.0115, -12.0),
                                  'lp': (-60.0, -10.0, 0.0, -10.0)}),
                                (14.25,
                                 {'torso': (23.9758, -3.991, 0.0),
                                  'head': (15.978, 0.0, 0.0),
                                  'O': (0.2084, 11.4782, 9.6684),
                                  'd': (0.4788, -0.2882, 0.8293),
                                  'u': (-0.8622, 0.0236, 0.506),
                                  'Ls': (2.2017,),
                                  'rp': (-72.7557, -51.2169, 16.4444, -14.25),
                                  'lp': (-60.0, -10.0, 0.0, -10.017)}),
                                (14.5,
                                 {'torso': (23.9069, -3.9649, 0.0),
                                  'head': (15.9148, 0.0, 0.0),
                                  'O': (0.1833, 11.5913, 9.741),
                                  'd': (0.4782, -0.2924, 0.8282),
                                  'u': (-0.8622, 0.0234, 0.506),
                                  'Ls': (2.2069,),
                                  'rp': (-72.7683, -50.9663, 16.8964, -16.5),
                                  'lp': (-60.0, -10.0, 0.0, -10.0668)}),
                                (14.75,
                                 {'torso': (23.7989, -3.9226, 0.0),
                                  'head': (15.815, 0.0, 0.0),
                                  'O': (0.1054, 11.6325, 9.6942),
                                  'd': (0.4798, -0.2812, 0.8311),
                                  'u': (-0.8623, 0.024, 0.5059),
                                  'Ls': (2.2151,),
                                  'rp': (-71.5731, -50.9064, 17.1018, -18.75),
                                  'lp': (-60.0, -10.0, 0.0, -10.1478)}),
                                (15.0,
                                 {'torso': (23.6576, -3.8654, 0.0),
                                  'head': (15.6829, 0.0, 0.0),
                                  'O': (0.0135, 11.6972, 9.6468),
                                  'd': (0.4814, -0.27, 0.8339),
                                  'u': (-0.8623, 0.0246, 0.5058),
                                  'Ls': (2.2264,),
                                  'rp': (-70.4023, -50.803, 17.226, -21.0),
                                  'lp': (-60.0, -10.0, 0.0, -10.2582)}),
                                (15.25,
                                 {'torso': (23.4884, -3.7942, 0.0),
                                  'head': (15.5231, 0.0, 0.0),
                                  'O': (-0.0909, 11.7782, 9.593),
                                  'd': (0.4829, -0.2588, 0.8365),
                                  'u': (-0.8624, 0.0252, 0.5056),
                                  'Ls': (2.2404,),
                                  'rp': (-69.23, -50.6824, 17.3162, -23.25),
                                  'lp': (-60.0, -10.0, 0.0, -10.3964)}),
                                (15.5,
                                 {'torso': (23.2972, -3.7103, 0.0),
                                  'head': (15.3399, 0.0, 0.0),
                                  'O': (-0.2061, 11.8716, 9.5323),
                                  'd': (0.4844, -0.2476, 0.8391),
                                  'u': (-0.8624, 0.0257, 0.5055),
                                  'Ls': (2.257,),
                                  'rp': (-68.0569, -50.5503, 17.3939, -25.5),
                                  'lp': (-60.0, -10.0, 0.0, -10.5608)}),
                                (15.75,
                                 {'torso': (23.0894, -3.6147, 0.0),
                                  'head': (15.1379, 0.0, 0.0),
                                  'O': (-0.33, 11.9746, 9.4652),
                                  'd': (0.4858, -0.2363, 0.8415),
                                  'u': (-0.8625, 0.0263, 0.5053),
                                  'Ls': (2.276,),
                                  'rp': (-66.8832, -50.4081, 17.4685, -27.75),
                                  'lp': (-60.0, -10.0, 0.0, -10.7495)})]}}

    for _n, _e in _SLASH3.items():
        _ks = {t: k for t, k in ATTACKS[_n] if t not in _e["drop"]}
        _ks.update({t: dict(k) for t, k in _e["keys"]})
        ATTACKS[_n] = sorted(_ks.items(), key=lambda kv: kv[0])


SP.GUARD, SP.ATTACKS, SP.IMPACT, SP.ATTACK_LEGS, SP.ATTACK_TICKS = GUARD, ATTACKS, IMPACT, ATTACK_LEGS, ATTACK_TICKS
SP.STANCE_KEYS, SP.attack_clip = STANCE_KEYS, attack_clip


def _rot(e):
    x, y, z = np.radians(e)
    cx, sx, cy, sy, cz, sz = np.cos(x), np.sin(x), np.cos(y), np.sin(y), np.cos(z), np.sin(z)
    return (np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]) @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
            @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]))


def _ang(A, B):
    return float(np.degrees(np.arccos(np.clip((np.trace(A.T @ B) - 1) / 2, -1.0, 1.0))))


def _seg_cost(p, q):
    A, B, M = _rot(p), _rot(q), _rot([(p[i] + q[i]) / 2 for i in range(3)])
    return _ang(M, A) + _ang(M, B) - _ang(A, B) + 1e-4 * sum((p[i] - q[i]) ** 2 for i in range(3))


def rebranch(vals):
    """Per key, the equivalent euler triple that keeps linear euler blending on (near) the shortest rotation, the first key
    fixed and the last key within 180 deg of it on every axis (the game blends the clip's eulers with the stance's, so a
    last key on another branch spins the bone a full turn during the fade-out). None if the end has no such triple."""
    def cands(v):
        x, y, z = v
        return [(f[0] + 360 * a, f[1], f[2] + 360 * c) for f in ((x, y, z), (x + 180, 180 - y, z + 180))
                for a in range(-2, 3) for c in range(-2, 3)]
    first = tuple(vals[0])
    C = [[first]] + [cands(v) for v in vals[1:]]
    C[-1] = [c for c in C[-1] if all(abs(c[i] - first[i]) <= 180 for i in range(3))]
    if not C[-1]:
        return None
    cost, back = [0.0], [[0]]
    for k in range(1, len(C)):
        nc, nb = [], []
        for c in C[k]:
            best = min(range(len(C[k - 1])), key=lambda j: cost[j] + _seg_cost(C[k - 1][j], c))
            nc.append(cost[best] + _seg_cost(C[k - 1][best], c)); nb.append(best)
        cost, back = nc, back + [nb]
    j, out = min(range(len(cost)), key=lambda q: cost[q]), []
    for k in range(len(C) - 1, -1, -1):
        out.append(C[k][j]); j = back[k][j]
    out = [[round(v, 3) for v in t] for t in out[::-1]]
    assert all(_ang(_rot(a), _rot(b)) < 1e-2 for a, b in zip(vals, out))
    return out


_attack_clips = SP.attack_clips


def attack_clips():
    """The spear builder's attack clips, every rotation channel that ends more than 180 deg off its start re-branched."""
    out = _attack_clips()
    for name, clip in out.items():
        for bone, ch in clip["bones"].items():
            r = ch.get("rotation")
            if not isinstance(r, dict) or len(r) < 2:
                continue
            ks = sorted(r, key=float)
            if all(abs(r[ks[-1]][i] - r[ks[0]][i]) <= 180 for i in range(3)):
                continue
            new = rebranch([r[k] for k in ks])
            print("%s %s: end %s off its start; re-branched %s" % (name, bone, r[ks[-1]], "to %s" % new[-1] if new else "FAILED"))
            if new:
                ch["rotation"] = {k: v for k, v in zip(ks, new)}
    return out


if SP.V2C:
    SP.attack_clips = attack_clips

if __name__ == "__main__":
    if os.environ.get("GREATAXE_PROBE"):
        probe()
    else:
        SP.main()
