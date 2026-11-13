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

import json
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
# ic_breaker's guard (forearm clearance, 2026-11-10): the same high port, the axe upright beside the right shoulder, but the
# elbows held out from the breastplate (the forearms had sunk 2.5 px into the chest). Designed in arm space so both fists sit
# exactly on the haft: the right fist before the right breast, the left close above it, both forearms clear of the torso.
BREAKER = SP.TROOP == "ic_breaker"
if BREAKER:
    GUARD = dict(torso=(12.0, 12.0, 0.0), head=(4.0, 0.0, 0.0), O=(-3.655, 16.081, 4.397), d=(-0.44692, 0.8839, 0.13781),
                 u=(-0.89241, -0.45122, 0.0), Ls=(3.719,), rp=(-58.21, -53.9, 24.47, -42.12), lp=(-115.91, 7.61, -91.91, -42.17))
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

if BREAKER:
    # ic_breaker's stances on the wide guard (forearm clearance, 2026-11-10), same reads, the elbows out from the breastplate:
    # brace the haft near upright before the right breast, hands close; wall the head levelled forward at chest height; ward
    # the haft high across the chest to the left.
    STANCE_KEYS["ready"] = STANCE_KEYS["shield_up"] = GUARD
    STANCE_KEYS["brace"] = dict(torso=(14.0, 12.0, 0.0), head=(2.0, 0.0, 0.0), O=(-5.041, 13.797, 3.854), d=(-0.25721, 0.95477, 0.14921),
                                u=(-0.96558, -0.26012, 0.0), Ls=(2.756,), rp=(-61.44, -47.37, 24.37, -22.79), lp=(-120.44, 11.35, -100.04, -35.13))
    STANCE_KEYS["wall"] = dict(torso=(26.0, 16.0, 0.0), head=(8.0, 0.0, 0.0), O=(-7.211, 12.4, 4.151), d=(0.06635, 0.30914, 0.9487),
                               u=(-0.97774, 0.20984, 0.0), Ls=(5.474,), rp=(-73.34, -36.03, 43.19, -44.94), lp=(-93.51, 15.55, -41.45, -28.3))
    STANCE_KEYS["ward"] = dict(torso=(-4.0, 12.0, 0.0), head=(-2.0, 0.0, 0.0), O=(-4.5, 20.449, 4.185), d=(0.9094, 0.33797, 0.24242),
                               u=(0.25758, 0.0, -0.96626), Ls=(12.831,), rp=(-91.11, -22.93, 73.27, -48.2), lp=(-72.33, -47.38, -0.01, -85.67))


def attack_clip(name):
    return attack_solve(name, ATTACKS[name], ATTACK_LEGS[name])


def attack_solve(name, keys, legs_spec):
    """One attack's samples solved from its keys and its legs plan (attack_clip; its own step so the solve cache keys it
    on this attack's keys alone)."""
    legs = K.attack_legs(legs_spec, ticks=ATTACK_TICKS)
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


# ic_breaker on the wide guard (forearm clearance, 2026-11-10): keys refitted in arm space so both forearms stay out of the
# breastplate and hips, the fists and haft within a few px and degrees of the old keys; timing, torso and head unchanged.
if BREAKER:
    _BREAKER_FIT = {
        "haft_jab": {
            2.0: dict(torso=(2.46, 9.04, 0), head=(2.52, 0, 0), O=(-6.215, 17.166, 3.561), d=(0.6877, 0.7016, 0.1865), u=(0.2591, 0.0027, -0.9658), Ls=(6.632,), rp=(-69.1, -35.28, 28.54, -17.19), lp=(-62.23, 8.55, -68.95, -98.11)),
            3.0: dict(torso=(-2, 8, 0), head=(2, 0, 0), O=(-6.334, 17.585, 2.835), d=(0.979, 0.1756, 0.1038), u=(0.1054, 0.0002, -0.9944), Ls=(4.647,), rp=(-70.21, -35.05, 30.64, -14.59), lp=(-66.64, 36.66, -89.58, -94.93)),
            5.5: dict(torso=(-5, 9, 0), head=(2, 0, 0), O=(-6.191, 17.063, 2.501), d=(0.9756, 0.2085, 0.0695), u=(0.071, 0.0003, -0.9975), Ls=(4.768,), rp=(-68.35, -37.65, 31, -11.84), lp=(-68.01, 39.15, -93.17, -98.01)),
            7.0: dict(torso=(22, 2, 0), head=(8, 0, 0), O=(-2.698, 13.888, 11.082), d=(0.9688, 0.1526, 0.1951), u=(0.1985, -0.0074, -0.9801), Ls=(7.457,), rp=(-85.67, -36.04, 21.32, -13.17), lp=(-21.03, 9.86, -93.62, -98.13)),
            8.25: dict(torso=(24.56, 0.33, 0), head=(8, 0, 0), O=(-2.438, 14.907, 11.085), d=(0.9669, 0.1549, 0.2027), u=(0.2068, -0.0105, -0.9783), Ls=(9.327,), rp=(-66.07, -36.95, 22.91, -72.19), lp=(-27.79, -20.35, -32.22, -104.27)),
            9.0: dict(torso=(25, 0, 0), head=(8, 0, 0), O=(-2.628, 15.451, 10.704), d=(0.9583, 0.1467, 0.2452), u=(0.2485, -0.0042, -0.9686), Ls=(9.293,), rp=(-63.49, -36.1, 29.62, -90.04), lp=(-30.9, -20.68, -33.86, -108)),
            11.0: dict(torso=(19.73, 3.49, 0), head=(6.68, 0, 0), O=(-2.978, 14.717, 9.479), d=(0.8331, 0.4035, 0.3783), u=(0.0728, 0.598, -0.7982), Ls=(9.242,), rp=(-56.3, -41.37, 20.47, -64.92), lp=(-36.49, -20.46, -44, -100.1)),
            13.0: dict(torso=(14, 8, 0), head=(5, 0, 0), O=(-4.179, 15.826, 6.245), d=(0.5356, 0.7755, 0.3342), u=(-0.7091, 0.6279, -0.3208), Ls=(8.150,), rp=(-53.23, -46.99, 55.37, -91.31), lp=(-70.31, -18.94, -58.45, -108.07)),
            14.0: dict(torso=(13.71, 8.77, 0), head=(4.73, 0, 0), O=(-3.832, 15.418, 6.83), d=(0.4658, 0.8195, 0.3337), u=(-0.7944, 0.5535, -0.2503), Ls=(7.779,), rp=(-51.54, -46.77, 24.07, -59.78), lp=(-67.48, -10.46, -55.81, -101.99)),
            16.0: dict(torso=(13.37, 9.9, 0), head=(4.33, 0, 0), O=(-3.301, 15.972, 6.15), d=(0.0806, 0.9555, 0.2838), u=(-0.9818, 0.1251, -0.1426), Ls=(5.502,), rp=(-55.52, -51.77, 18.35, -49.33), lp=(-86.68, 0.28, -70.6, -82.53)),
            19.0: dict(torso=(13, 11, 0), head=(4, 0, 0), O=(-2.951, 16.549, 5.235), d=(-0.3887, 0.9104, 0.1418), u=(-0.9191, -0.3939, 0.0091), Ls=(3.288,), rp=(-44.08, -53.57, -0.01, -47.6), lp=(-99.05, 6.2, -90.46, -68.93)),
        },
        "side_hew": {
            4.0: dict(torso=(6, 16, -3), head=(4, 0, 0), O=(-3.97, 18.01, 3.392), d=(-0.787, 0.5355, -0.3064), u=(-0.5218, -0.8427, -0.1325), Ls=(4.018,), rp=(-73.82, -57.68, 6.91, -16.18), lp=(-126.79, -2.77, -95.63, -18.43)),
            8.0: dict(torso=(4, 22, -6), head=(4, 0, 0), O=(-4.973, 18.228, 1.84), d=(-0.4216, 0.5156, -0.7459), u=(-0.4148, -0.8412, -0.347), Ls=(4.965,), rp=(-80.79, -59.52, 8.28, -9.26), lp=(-129.97, -0.92, -86.68, -31.74)),
            10.0: dict(torso=(3, 23, -6), head=(4, 0, 0), O=(-4.853, 18.263, 2.249), d=(-0.3786, 0.5174, -0.7675), u=(-0.3908, -0.841, -0.3742), Ls=(4.949,), rp=(-91.28, -56.97, 21.57, -8.48), lp=(-127.96, -0.96, -85.77, -38.25)),
            11.0: dict(torso=(10, 14, -2), head=(6, 0, 0), O=(-4.003, 17.005, 5.643), d=(-0.9505, 0.256, -0.1764), u=(-0.2552, -0.9665, -0.0274), Ls=(2.650,), rp=(-85.76, -51.67, 20.35, -19.63), lp=(-122.62, 6.9, -93.69, -23.53)),
            11.25: dict(torso=(10.5, 16, -1.5), head=(6.5, 0, 0), O=(-4.352, 16.629, 6.755), d=(-0.9513, 0.1976, 0.2365), u=(-0.1964, -0.9801, 0.029), Ls=(1.590,), rp=(-100.39, -41.32, 46.73, -21.9), lp=(-121.03, 7.59, -93.98, -16.5)),
            11.5: dict(torso=(11, 2, -1), head=(7, 0, 0), O=(-3.151, 15.378, 7.936), d=(0.2473, 0.1599, 0.9556), u=(-0.0137, -0.9856, 0.1685), Ls=(1.363,), rp=(-98, -19.5, 100.89, -47.71), lp=(-120.34, 13.69, -95.41, -14.22)),
            12.0: dict(torso=(12, -10, 0), head=(8, 0, 0), O=(-1.009, 14.642, 8.713), d=(0.6881, 0.0836, 0.7208), u=(0.0563, -0.9965, 0.0619), Ls=(2.028,), rp=(-88.32, -13.92, 123.36, -72.07), lp=(-120.81, 15.67, -95.29, -14.55)),
            14.0: dict(torso=(22, -30, 4), head=(8, 0, 0), O=(3.047, 12.783, 8.416), d=(0.992, -0.0537, 0.1144), u=(-0.0534, -0.9986, -0.0052), Ls=(2.119,), rp=(-41.37, -41.67, 18.76, -69.82), lp=(-112.07, 17.73, -98.42, -32.58)),
            17.0: dict(torso=(20, -34, 4), head=(8, 0, 0), O=(3.439, 12.928, 7.163), d=(0.9391, -0.0975, -0.3296), u=(-0.0917, -0.9952, 0.0333), Ls=(1.397,), rp=(-44.7, -47.14, 15.41, -53.66), lp=(-102.05, 24.4, -106.98, -55.37)),
            19.0: dict(torso=(16.76, -21.5, 2), head=(7.25, 0, 0), O=(1.1, 13.605, 6.969), d=(0.7823, 0.1781, 0.5969), u=(-0.507, -0.3747, 0.7762), Ls=(3.003,), rp=(-41.05, -50.12, 13.72, -51.49), lp=(-103.63, 18.12, -102.33, -38.45)),
            21.0: dict(torso=(14, -4, 0), head=(6, 0, 0), O=(-3.019, 14.908, 7.29), d=(0.1966, 0.9261, 0.3221), u=(-0.9784, 0.2067, 0.003), Ls=(4.130,), rp=(-45.01, -37.22, 15.4, -50.91), lp=(-113.2, 3.87, -88.04, -40.83)),
            25.0: dict(torso=(13, 11, 0), head=(4, 0, 0), O=(-3.526, 16.239, 4.928), d=(-0.4443, 0.885, 0.1391), u=(-0.8937, -0.4487, 0.0004), Ls=(3.699,), rp=(-47.02, -50.88, 11.12, -51.72), lp=(-117.47, 5.17, -90.15, -41.97)),
        },
        "overhead_chop": {
            17.0: dict(torso=(22, -3, 0), head=(14, 0, 0), O=(-3.28, 13.646, 8.759), d=(0.516, -0.1865, 0.8361), u=(-0.8566, -0.113, 0.5035), Ls=(2.344,), rp=(-30.45, -37.03, 13.16, -92.03), lp=(-71.92, 50.14, 1.53, -16.09)),
            21.0: dict(torso=(18, 6, 0), head=(8, 0, 0), O=(-3.853, 15.628, 6.888), d=(-0.0167, 0.7322, 0.6809), u=(-0.9999, -0.0122, -0.0114), Ls=(4.938,), rp=(-38.81, -42.12, 0, -68.83), lp=(-85.78, 45.24, 3.97, -31.91)),
        },
    }  # _BREAKER_FIT
    for _n, _ks in _BREAKER_FIT.items():
        ATTACKS[_n] = sorted({**dict(ATTACKS[_n]), **_ks}.items(), key=lambda kv: kv[0])


def _un(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def _arc(a, b):
    """The shortest rotation taking direction a onto direction b."""
    a, b = _un(a), _un(b)
    v, c = np.cross(a, b), float(a @ b)
    Kx = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    return np.eye(3) + Kx + Kx @ Kx / (1 + c)


def _roll(u, d, deg):
    """u turned deg about the shaft d."""
    d, u = _un(d), np.asarray(u, float)
    a = math.radians(deg)
    return u * math.cos(a) + np.cross(d, u) * math.sin(a) + d * (d @ u) * (1 - math.cos(a))


def _sq(u, d):
    d = _un(d)
    u = np.asarray(u, float)
    return tuple(float(x) for x in _un(u - (u @ d) * d))


def _edit(name, t, **kw):
    """Replaces fields of the key at tick t (O/d/u given as vectors; nothing else of the key changes)."""
    ks = dict(ATTACKS[name])
    k = dict(ks[t])
    for f, v in kw.items():
        k[f] = tuple(float(x) for x in np.ravel(v))
    k["u"] = _sq(k["u"], k["d"])
    ks[t] = k
    ATTACKS[name] = sorted(ks.items(), key=lambda kv: kv[0])


def _key(name, t):
    return dict(ATTACKS[name])[t]


REVIEW_FIX = BREAKER and os.environ.get("GG_BREAKER_REVIEW") != "0"
if REVIEW_FIX:
    # ---- haft_jab. grip_roll 83 at t1.5: the wind-up rolled the axe 114 deg about its haft in two ticks (the bit went from
    # toward the foe to straight up). Now every wind-up key carries the guard's roll along the swing (the shortest turn of
    # the haft, no roll about it), so the bit stays toward the foe through the load and the shove; the recovery closes the
    # small rest of the roll over ten ticks after the strike. phases (fastest at the wind-up, no follow-through, landing at
    # 42% of the fastest): the shove now drives on past the impact (the haft keeps going out to t8.25) and the haft's
    # far end swings forward through the strike, so the blow is still accelerating when it lands.
    _jab = "haft_jab"
    _d0, _u0 = _key(_jab, 0)["d"], _key(_jab, 0)["u"]
    _pd, _pu = _d0, _u0
    for _t, _k in list(ATTACKS[_jab]):
        if 0 < _t <= 9.0:
            _pu = _sq(_arc(_pd, _k["d"]) @ np.asarray(_pu), _k["d"])
            _pd = _k["d"]
            _edit(_jab, _t, u=_pu)
    _post = [t for t, _ in ATTACKS[_jab] if 9.0 < t <= 19.0]
    _trans = {}
    for _t in _post:
        _k = _key(_jab, _t)
        _pu = _sq(_arc(_pd, _k["d"]) @ np.asarray(_pu), _k["d"])
        _pd = _k["d"]
        _trans[_t] = _pu
    _end = _key(_jab, _post[-1])
    _dn = _un(_end["d"])
    _err = math.degrees(math.atan2(float(np.cross(_trans[_post[-1]], _end["u"]) @ _dn), float(np.dot(_trans[_post[-1]], _end["u"]))))
    for _t in _post:
        _edit(_jab, _t, u=_roll(_trans[_t], _key(_jab, _t)["d"], _err * (_t - 9.0) / (_post[-1] - 9.0)))
    # the shove: loaded with the far end of the haft drawn back, driven out with it swinging forward, and carried on
    _k55, _k7, _k825, _k9 = (_key(_jab, t) for t in (5.5, 7.0, 8.25, 9.0))
    _edit(_jab, 5.5, d=_un(np.asarray(_k55["d"]) + (0, 0, -0.22)), u=_arc(_k55["d"], _un(np.asarray(_k55["d"]) + (0, 0, -0.22))) @ np.asarray(_k55["u"]),
          O=np.asarray(_k55["O"]) + (0, 0, -0.8))
    _edit(_jab, 7.0, d=_un(np.asarray(_k7["d"]) + (0, 0, 0.12)), u=_arc(_k7["d"], _un(np.asarray(_k7["d"]) + (0, 0, 0.12))) @ np.asarray(_k7["u"]))
    _edit(_jab, 8.25, d=_un(np.asarray(_k825["d"]) + (0, 0, 0.2)), u=_arc(_k825["d"], _un(np.asarray(_k825["d"]) + (0, 0, 0.2))) @ np.asarray(_k825["u"]),
          O=np.asarray(_k825["O"]) + (0, 0, 1.2))
    _edit(_jab, 9.0, O=np.asarray(_k9["O"]) + (0, 0, 0.9))
    # helm into left_spikes 3.98 at t7.5 / left_gauntlet into helm at t13.25: the left fist rode the haft far out, so the
    # left arm rose with its plate into the helm. The hands sit a little closer on the haft through the recovery.
    for _t, _ls in ((11.0, 7.8), (13.0, 7.0), (14.0, 6.8)):
        _edit(_jab, _t, Ls=(_ls,))

    # ---- overhead_chop. roll_spin 170 at t12.75 and the right_hand pop at t12.5: the bit's roll jumped 50 deg in a quarter
    # tick and back (keys 12.75-13.25). Those keys now roll evenly from t12.5 to t13.5.
    _ch = "overhead_chop"
    _ua, _ub = np.asarray(_key(_ch, 12.5)["u"]), np.asarray(_key(_ch, 13.5)["u"])
    for _t in (12.75, 13.0, 13.25):
        _f = (_t - 12.5) / 1.0
        _edit(_ch, _t, u=_ua * (1 - _f) + _ub * _f)
    # floor 2.7 at t14: the follow-through drove the bit into the ground. The haft is levelled a few degrees from the
    # impact on (the impact key itself is unchanged).
    for _t, _deg in ((12.75, 2.0), (13.0, 4.0), (13.25, 6.0), (13.5, 8.0), (13.75, 9.0), (14.0, 9.0), (14.25, 9.0),
                     (14.5, 9.0), (14.75, 8.5), (15.0, 8.0), (15.25, 7.0), (15.5, 6.0), (15.75, 5.0), (17.0, 2.0)):
        _k = _key(_ch, _t)
        _d = _un(_k["d"])
        _side = _un(np.cross(_d, (0.0, 1.0, 0.0)))
        _R = _arc(_d, _roll(_d, _side, -_deg))
        _nd = _R @ _d
        if _nd[1] < _d[1]:
            _R = _arc(_d, _roll(_d, _side, _deg))
            _nd = _R @ _d
        _edit(_ch, _t, d=_nd, u=_R @ np.asarray(_k["u"]))
    # weapon_t3 into the helm at t10.75 and left_cuff / left_gauntlet into the helm at t5-10.75 (the heave): the haft was
    # held straight over the crown (O z 0), so both forearms crossed the helm. It is now heaved up a hand in front of the
    # brow (the axe head still hangs behind the back); clear_helm re-solves the left arm where any cuff is left.
    for _t, _dy, _dz in ((4.0, 0.0, 1.2), (8.0, 0.0, 3.4), (10.0, 0.0, 3.4), (10.25, 1.0, 2.8), (10.5, 1.6, 2.2), (10.75, 1.4, 1.4)):
        _edit(_ch, _t, O=np.asarray(_key(_ch, _t)["O"]) + (0.0, _dy, _dz))

    # ---- side_hew. left_cuff into the helm at t10 and the left elbow (133 at t3.75): see clear_helm.
    # floor: right_knee_wing under the ground at the deepest drive (hew t13, jab t9, chop t14): the pelvis sinks a little less there.
    def _raise(name, lift):
        # the whole man rides higher, the axe with him: the haft's keys rise by the pelvis's lift at their tick, so the arms
        # keep the pose they had (lifting only the pelvis bent the arms up and tipped the right plate's spikes into the helm)
        hk = ATTACK_LEGS[name]["hips"]
        ATTACK_LEGS[name] = dict(ATTACK_LEGS[name], hips=[(t, r, (p[0], p[1] + lift.get(t, 0.0), p[2])) for t, r, p in hk])
        ht = [t for t, _, _ in hk]
        hl = [lift.get(t, 0.0) for t in ht]
        for t, k in list(ATTACKS[name]):
            dy = float(np.interp(t, ht, hl))
            if dy:
                _edit(name, t, O=np.asarray(k["O"]) + (0.0, dy, 0.0))
    _raise("side_hew", {10.5: 0.4, 12: 1.7, 14: 1.5, 17: 1.0})
    _raise("haft_jab", {6.0: 0.6, 7: 1.3, 9: 1.3, 13: 0.5})
    _raise("overhead_chop", {10.5: 0.4, 12: 1.9, 14: 1.8, 17: 1.2})
    # floor: right_knee_wing under the ground in brace (1.34) and wall (2.05). This rig's knee plate hangs 3 px down the
    # front of the shin, so the rear knee cannot fold as deep as the spearman's: the rear foot is set further back and the
    # pelvis sits a little higher (wall stays the deepest and longest stance, brace a step under the guard).
    def _stance_legs(k, lift, back):
        s = dict(SP.STANCE_LEGS[k])
        (r, (px, py, pz)) = s["hips"]
        s["hips"] = (r, (px, py + lift, pz))
        x, y, z = s["right"]
        s["right"] = (x, y, z - back)
        SP.STANCE_LEGS[k] = s
    _stance_legs("brace", 0.9, 0.7)
    _stance_legs("wall", 2.2, 1.5)
    # hit_medium_back: floor 1.6 at t2.25, the axe head into the ground as the chest is thrown forward; the arms lift a
    # little more against the blow so the haft stays nearer level.
    SP.HIT_PEAK["back"] = K.off(torso=(12, 0, 0), head=(5, 0, 0), rarm=(-12, 0, 4), larm=(-12, 0, -4))
    if os.environ.get("GG_BREAKER_FIX1") != "0":   # fix round 1: the axe head into the helm 1.84 at t2: the head is thrown back
        SP.HIT_PEAK["back"] = K.off(torso=(12, 0, 0), head=(-6, 0, 0), rarm=(-12, 0, 4), larm=(-12, 0, -4))

FIX1 = REVIEW_FIX and os.environ.get("GG_BREAKER_FIX1") != "0"   # fix round 1 (2026-11-12); =0 rebuilds the reviewed clips
FIX2 = FIX1 and os.environ.get("GG_BREAKER_FIX2") != "0"   # fix round 2 (2026-11-12): round2_fixes; =0 leaves it out
if FIX1:
    # ---- overhead_chop, the heave (t4-10.75). The fists were held before the brow at shoulder height: the left gauntlet sank
    # into the helm (2.27 px at t8.5) and the haft stood straight up before the helm's face as the drop began (weapon into
    # helm 1.66 at t10.5). The fists now ride a little lower, under the chin, and the haft leans out over the right shoulder
    # as it comes over the top, so it passes beside the helm instead of before its face. The axe head still hangs behind
    # the back; the drop's keys from t11 on are unchanged.
    for _t, _dy, _yaw in ((4.0, -0.8, 0.0), (8.0, -2.4, 0.0), (10.0, -2.4, 0.0), (10.25, -2.2, 0.05), (10.5, -1.6, 0.1),
                          (10.75, -0.8, 0.06)):
        _k = _key("overhead_chop", _t)
        _nd = _un(np.asarray(_k["d"]) + (-_yaw, 0.0, 0.0))
        _edit("overhead_chop", _t, O=np.asarray(_k["O"]) + (0.0, _dy, 0.0), d=_nd, u=_arc(_k["d"], _nd) @ np.asarray(_k["u"]))
    # ---- side_hew, the coil (t4-10) and the carry (t14): pen_limb 1.49 (the left forearm across the breastplate as both
    # fists wind to the right). The fists are held a little further out before the body there (the swing's path and the
    # impact key are unchanged).
    for _t, _dz in ((4.0, 0.5), (8.0, 1.0), (10.0, 1.0), (14.0, 0.8)):
        _edit("side_hew", _t, O=np.asarray(_key("side_hew", _t)["O"]) + (0.0, 0.0, _dz))


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


def _rebranch_clips(out):
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


def attack_clips():
    """The spear builder's attack clips, every rotation channel that ends more than 180 deg off its start re-branched."""
    if REVIEW:
        return _rebranch_clips(review_attack_clips())
    return _rebranch_clips(_attack_clips())


# =============================================================================================================================
# Batch review 2026-11-11 (Kort's four focus points: no clipping, nothing stale, the weapon facing the right way, no limb
# glitches; docs/anim-rebuild/IC_BREAKER-REVIEW.md "Batch review 2026-11-11"). ic_breaker only (REVIEW):
#   soft motion   every piece that hangs (cape cloth, fur collar, skirts, tassets, tabard) swings on fullbody.soft_motion and the
#                 pauldrons trail their arms, in the attacks, the hits, the stun, the shake-off, the knockback and the pull. The
#                 helm's plume is a crest combed along the helm's ridge: it rides the helm and gets no keys (Kort, knight round 3:
#                 "remove the wobble of the red strip on his head").
#   layered clips the batch 1-3 clips (rally, hold, fall back, charge, stun, shake-off, knockback, pull) are built here from the
#                 shared modules (art/anim/common stances, charge, reactions: imported, not edited) on this build's own clips,
#                 then fixed for this rig, so one run writes the whole clip file (out/ = the exported file; export.py copies it).
# =============================================================================================================================
REVIEW = BREAKER and os.environ.get("GG_BREAKER_REVIEW") != "0"
COMMON = HERE.parent / "common"
sys.path.insert(0, str(COMMON))
import fullbody  # noqa: E402

FIXED_PIECES = ("helm_plume",)   # rides the helm: a crest, not a plume that hangs
_SOFT = {}


def soft_parts():
    if "parts" not in _SOFT:
        _SOFT["parts"] = {n: c for n, c in fullbody.soft_parts_for().items() if n not in FIXED_PIECES}
        _SOFT["plates"] = fullbody.plates_for()
        _SOFT["bones"] = ({b for c in _SOFT["parts"].values() for b in c["bones"]}, set(_SOFT["plates"]))
    return _SOFT["parts"], _SOFT["plates"]


def soften(upper, legs, report=False):
    parts, plates = soft_parts()
    upper, legs = fullbody.soft_motion(upper, legs, parts=parts, plates=plates, report=report)
    return plate_clear(upper, legs, report=report), legs


PLATE_TRAIL = 10.0   # deg: how far a plate may turn on its arm (the kit's own lag limit)
_PLATE_GRID = [(x, z) for x in np.arange(-10.0, 10.01, 2.5) for z in np.arange(-10.0, 10.01, 2.5) if x * x + z * z <= PLATE_TRAIL ** 2 + 1e-6]


def plate_clear(upper, legs, report=False):
    """The spiked pauldrons stand 3 px from the helm's rim at rest: wherever a plate's spikes would sink into the helm, the
    plate uses its 10 deg of play on the arm to turn them out of it (eased over neighbouring samples). Nothing else moves."""
    n = len(upper)
    moved = 0
    for side in ("right", "left"):
        plate = side + "_pauldron"
        if not any(plate in s for s in upper):
            continue
        for i in range(n):
            if plate not in upper[i]:
                upper[i] = dict(upper[i])
                upper[i][plate] = (Z3, None)
        keys = [np.array(upper[i][plate][0], float) for i in range(n)]
        best = list(keys)
        for i in range(n):
            p = SP.fk_pose(upper[i], legs[i])
            if _spike_pen(p, side) <= 0.3:
                continue
            cost0, pick = None, keys[i]
            for x, z in _PLATE_GRID:
                cand = np.array([x, keys[i][1], z])
                q = dict(p)
                q[plate] = (tuple(cand), p[plate][1])
                c = _spike_pen(q, side) + 0.004 * float(((cand - keys[i]) ** 2).sum())
                if cost0 is None or c < cost0:
                    cost0, pick = c, cand
            best[i] = pick
            moved += 1
        # ease: each changed sample's change spread over a tick with a smoothstep falloff (never past the trail limit)
        delta = [b - k for b, k in zip(best, keys)]
        out = [np.zeros(3) for _ in range(n)]
        for i, d in enumerate(delta):
            if not d.any():
                continue
            for j in range(max(0, i - SUB), min(n, i + SUB + 1)):
                w = fullbody.B.smooth(1.0 - abs(j - i) / float(SUB + 1))
                if np.linalg.norm(d * w) > np.linalg.norm(out[j]):
                    out[j] = d * w
        for i in range(n):
            if out[i].any():
                v = keys[i] + out[i]
                m = float(np.linalg.norm(v))
                if m > PLATE_TRAIL:
                    v = v * PLATE_TRAIL / m
                upper[i] = dict(upper[i])
                upper[i][plate] = (tuple(float(a) for a in v), upper[i][plate][1])
    if report and moved:
        print("  plates turned off the helm on %d samples" % moved)
    return upper


def piece_bones():
    soft_parts()
    return _SOFT["bones"][0] | _SOFT["bones"][1]


# ----------------------------------------------------------------------------------- the helm's clearance (this rig only)
# The breaker's guard holds the left fist high on the haft by the right side of the helm, so a left forearm that reaches
# across to it passes the helm (left_cuff / left_gauntlet into helm), and the plates' spikes stand 2 px above the
# pauldrons, close under the helm's rim. Two per-sample fixes, applied where the fault is and nowhere else:
#   left_clear  the left arm is solved again with the fist kept on the haft (it may slide along it), its cuff and gauntlet
#               kept out of the helm and the upper arm's twist inside the shoulder's reach (the elbow check);
#   head_clear  the head rolls away from a spiked plate that would sink into the helm.
HELM_BOX = (np.array([-5.0, 23.0, -5.0]), np.array([5.0, 34.0, 5.0]))   # head-local px (the helm's main cube)
ARM_PTS = (((6.0, 16.0, 0.0), 2.9), ((6.0, 14.0, 0.0), 2.7), ((6.0, 18.5, 0.0), 2.4))   # left_forearm-local: cuff, gauntlet, wrist
SPIKES = {"right": [(-8.5, y, z) for y in (26.5, 28.5) for z in (-2.5, 0.0, 2.5)],
          "left": [(8.5, y, z) for y in (26.5, 28.5) for z in (-2.5, 0.0, 2.5)]}
ELBOW_MAX = 125.0   # the elbow check fails at 130; the guard itself stands at 115
sys.path.insert(0, str(HERE.parents[2] / "tools" / "anim"))


def _local_px(M, w):
    v = np.linalg.inv(M) @ np.r_[np.asarray(w, float) / 16.0, 1.0]
    return np.array([-v[0], v[1], v[2]]) * 16.0


def _box_pen(q, r, box=HELM_BOX):
    """How far a ball of radius r at head-local q sinks into the helm (px, 0 when clear)."""
    lo, hi = box
    out = np.maximum(lo - q, 0.0) + np.maximum(q - hi, 0.0)
    d = float(np.linalg.norm(out))
    if d > 0:
        return max(0.0, r - d)
    return r + float(min((q - lo).min(), (hi - q).min()))


def _elbow(p, side="left"):
    import focus_check as FC   # noqa: E402  (read only: the same twist measure as the check)
    Rt = K.world(p, "torso")[:3, :3]
    Ru = K.world(p, side + "_arm")[:3, :3]
    Rf = K.world(p, side + "_forearm")[:3, :3]
    v = Ru.T @ Rf @ np.array([0.0, -1.0, 0.0])
    bend = math.degrees(math.acos(max(-1.0, min(1.0, -v[1]))))
    swing, twist = FC._swing_twist(Rt.T @ Ru)
    if bend < 15.0 or swing > 150.0:
        return 0.0
    way = math.degrees(math.atan2(-v[0], -v[2]))
    return abs((twist + way + 180.0) % 360.0 - 180.0)


def _helm_pen(p):
    Mh = K.world(p, "head")
    Mf = K.world(p, "left_forearm")
    return max(_box_pen(_local_px(Mh, K.point(Mf, c)), r) for c, r in ARM_PTS)


def _slide(p):
    """Where along the haft (px from the right fist) the left fist sits."""
    f = K.point(K.world(p, "left_forearm"), SP.FIST_L)
    a, b = SP.wpoint(p, SP.S(0.0)), SP.wpoint(p, SP.S(1.0))
    return float((f - a) @ (b - a))


def left_clear(p, warm=None, s_range=(1.0, 14.0), margin=0.6, chest=False):
    """The left arm solved again so its fist stays on the haft while the cuff and gauntlet stay off the helm and the elbow
    bends within the shoulder's reach (chest: and the forearm out of the breastplate, fix round 1). Returns (left_arm rot,
    left_forearm x, slide) or None when the pose needs nothing."""
    pen, elb = _helm_pen(p), _elbow(p)
    if chest == "only":
        pen = 0.0
    if pen <= 0.0 and elb <= ELBOW_MAX and (not chest or chest_pen(p, "left") <= CHEST_MAX):
        return None
    a0 = np.asarray(p["left_arm"][0], float)
    f0 = float(p["left_forearm"][0][0])
    s0 = _slide(p)
    prior = np.r_[a0, f0, s0]
    Mh = None

    def f(x):
        q = SP._arm("left", p, x[:4])
        fl = K.point(K.world(q, "left_forearm"), SP.FIST_L)
        res = [4.0 * (fl - SP.wpoint(q, SP.S(x[4])))]
        Mhq = K.world(q, "head")
        Mf = K.world(q, "left_forearm")
        res.append([(0.0 if chest == "only" else 3.0) * max(0.0, _box_pen(_local_px(Mhq, K.point(Mf, c)), r + margin)) for c, r in ARM_PTS])
        res.append([0.25 * max(0.0, _elbow(q) - (ELBOW_MAX - 7.0))])
        if chest:
            res.append([3.0 * max(0.0, chest_pen(q, "left") - (CHEST_MAX - CHEST_MARGIN))])
            res.append([2.0 * max(0.0, _spike_pen(q, "left") - 0.2)])
            res.append([2.0 * haft_in_left_arm(q)])
        res.append(0.03 * (x[:4] - prior[:4]))
        res.append([0.15 * (x[4] - prior[4])])
        return np.concatenate([np.ravel(r_) for r_ in res])

    clamp = lambda x: np.r_[x[:3], min(-2.0, max(-150.0, x[3])), min(s_range[1], max(s_range[0], x[4]))]  # noqa: E731
    x = SP.gn(f, prior, iters=40, clamp=clamp)
    return tuple(float(v) for v in x[:3]), float(x[3]), float(x[4])


# Fix round 1 (2026-11-12): the forearms' clearance of the breastplate and the belt (scorecard pen_limb, measured as it does:
# each forearm cube's points inside the torso or hips box, the depth counted from the face the elbow is outside of).
_GEO_BONES = {b["name"]: b for b in SP._GEO}
_FLIP = np.diag([-1.0, 1.0, 1.0])


def _grid(c, step=1.0):
    inf = float(c.get("inflate", 0.0) or 0.0)
    lo = np.asarray(c["origin"], float) - inf
    sz = np.asarray(c["size"], float) + 2 * inf
    axes = [np.linspace(lo[k], lo[k] + sz[k], max(2, int(math.ceil(abs(sz[k]) / step)) + 1)) for k in range(3)]
    return np.stack(np.meshgrid(*axes, indexing="ij"), axis=-1).reshape(-1, 3)


FORE_CUBES = {s: [(b, np.vstack([_grid(c) for c in _GEO_BONES[b].get("cubes", [])]))
                  for b in (s + "_forearm", s + "_cuff", s + "_gauntlet") if _GEO_BONES.get(b, {}).get("cubes")]
              for s in ("right", "left")}
CHEST_BOXES = [(b, np.asarray(c["origin"], float) - float(c.get("inflate", 0.0) or 0.0),
                np.asarray(c["origin"], float) + np.asarray(c["size"], float) + float(c.get("inflate", 0.0) or 0.0))
               for b in ("torso", "hips") for c in _GEO_BONES[b].get("cubes", [])]


def _to_local(Mb, Mf):
    """Bedrock px in bone f's frame -> bedrock px in bone b's frame."""
    T = np.linalg.inv(Mb) @ Mf
    R, t = _FLIP @ T[:3, :3] @ _FLIP, _FLIP @ T[:3, 3] * 16.0
    return R, t


RIGHT_TRIGGER = 1.0   # px: the guard itself holds the right forearm 0.9 px into the breastplate; right_clear leaves it be
CHEST_MAX = 0.85      # px: the scorecard's pen_limb limit is 1.391; the solves aim under it with room for the bake
CHEST_MARGIN = 0.3


def right_clear(upper, legs, report=""):
    """Fix round 1: where the right forearm sinks into the breastplate (the chop's heave: the arm straight out before the
    face, its 4 px forearm across the chest), the right arm is solved again with the forearm held out of the torso; the
    fist may move (the axe goes with it, its turn in the world unchanged). The change is eased over a tick either side,
    then the left fist is put back on the haft at the same place along it."""
    n = len(upper)
    sols = [None] * n
    for i in range(n):
        p = SP.fk_pose(upper[i], legs[i])
        if chest_pen(p, "right") <= RIGHT_TRIGGER:
            continue
        x0 = np.r_[np.asarray(upper[i]["right_arm"][0], float), float(upper[i]["right_forearm"][0][0])]
        g0 = K.point(K.world(p, "right_hand"), SP.GRIP)

        def f(x, p=p, x0=x0, g0=g0):
            q = SP._arm("right", p, x)
            return np.concatenate([1.0 * (K.point(K.world(q, "right_hand"), SP.GRIP) - g0),
                                   [4.0 * max(0.0, chest_pen(q, "right") - (CHEST_MAX - 1.5 * CHEST_MARGIN))],
                                   0.03 * (x - x0)])
        clamp = lambda x: np.r_[x[:3], min(-2.0, max(-150.0, x[3]))]  # noqa: E731
        sols[i] = SP.gn(f, x0.copy(), iters=40, clamp=clamp) - x0
    need = [i for i in range(n) if sols[i] is not None]
    if not need:
        return upper
    D = np.zeros((n, 4))
    for i in need:
        D[i] = sols[i]
    for k in range(4):
        D[:, k] = _smooth_series(list(D[:, k]), SUB)
    for _ in range(4):
        D[1:-1] = 0.25 * D[:-2] + 0.5 * D[1:-1] + 0.25 * D[2:]
    env = edge_env(n)
    for i in range(n):
        d = D[i] * env[i]
        if not np.abs(d).max() > 1e-6:
            continue
        p = SP.fk_pose(upper[i], legs[i])
        Rh = K.world(p, "right_hand")[:3, :3]
        s = _slide(p)
        x = np.r_[np.asarray(upper[i]["right_arm"][0], float), float(upper[i]["right_forearm"][0][0])] + d
        q = SP._arm("right", p, x)
        hand = K.decompose_zyx(K.world(q, "right_forearm")[:3, :3].T @ Rh)
        u = dict(upper[i], right_arm=(tuple(float(v) for v in x[:3]), upper[i]["right_arm"][1]),
                 right_forearm=((float(x[3]), upper[i]["right_forearm"][0][1], upper[i]["right_forearm"][0][2]), upper[i]["right_forearm"][1]),
                 right_hand=(tuple(float(v) for v in hand), upper[i]["right_hand"][1]))
        q = SP.fk_pose(u, legs[i])
        y0 = np.r_[np.asarray(u["left_arm"][0], float), float(u["left_forearm"][0][0])]

        def fl(y, q=q, s=s, y0=y0):
            qq = SP._arm("left", q, y)
            return np.concatenate([4.0 * (K.point(K.world(qq, "left_forearm"), SP.FIST_L) - SP.wpoint(qq, SP.S(s))),
                                   0.05 * (y - y0)])
        y = SP.gn(fl, y0.copy(), iters=30)
        u["left_arm"] = (tuple(float(v) for v in y[:3]), u["left_arm"][1])
        u["left_forearm"] = ((float(y[3]), u["left_forearm"][0][1], u["left_forearm"][0][2]), u["left_forearm"][1])
        upper[i] = u
    if report:
        print("  %s right forearm off the chest: %d samples solved; after: %.2f px" % (
            report, len(need), max(chest_pen(SP.fk_pose(s_, l_), "right") for s_, l_ in zip(upper, legs))))
    return upper


def chest_pen(p, side):
    """How deep (px) the side's forearm, cuff and gauntlet sink into the torso or hips box, from the elbow's side."""
    Mf = K.world(p, side + "_forearm")
    elbow = K.point(Mf, _GEO_BONES[side + "_forearm"]["pivot"])
    worst = 0.0
    for body, lo, hi in CHEST_BOXES:
        Mb = K.world(p, body)
        r = _local_px(Mb, elbow)
        half, c = (hi - lo) / 2.0, (hi + lo) / 2.0
        qr = r - c
        a = int(np.argmax(np.abs(qr) - half))
        sa = 1.0 if qr[a] >= 0 else -1.0
        for bone, pts in FORE_CUBES[side]:
            R, t = _to_local(Mb, K.world(p, bone))
            q = pts @ R.T + t - c
            side_d = half - np.abs(q)
            side_d[:, a] = half[a] - sa * q[:, a]
            d = side_d.min(axis=1)
            worst = max(worst, float(d.max()))
    return max(0.0, worst)


LARM_BOX = (np.array([4.0, 18.0, -2.0]), np.array([8.0, 24.0, 2.0]))   # left_arm-local px (the upper arm's cube)
_HAFT_S = np.arange(-6.0, 22.01, 1.0)


def haft_in_left_arm(p, r=0.7):
    """How far the haft (a 1 px rod) sinks into the left upper arm (px), the part near the left fist (its grip) skipped."""
    Ma = K.world(p, "left_arm")
    fist = K.point(K.world(p, "left_forearm"), SP.FIST_L)
    worst = 0.0
    for s_ in _HAFT_S:
        w = SP.wpoint(p, SP.S(float(s_)))
        if np.linalg.norm(w - fist) < 3.5:
            continue
        worst = max(worst, _box_pen(_local_px(Ma, w), r, LARM_BOX))
    return worst


def _spike_pen(p, side):
    Mh = K.world(p, "head")
    Ms = K.world(p, side + "_spikes")
    return max(_box_pen(_local_px(Mh, K.point(Ms, c)), 0.6) for c in SPIKES[side])


def head_roll_for(p):
    """The head's roll (deg, its own z) that takes the helm off a sinking spike, 0 when none sinks."""
    pens = {s: _spike_pen(p, s) for s in ("right", "left")}
    if max(pens.values()) <= 0.0:
        return 0.0
    best = (sum(pens.values()), 0.0)
    h = p["head"]
    for a in np.arange(-16.0, 16.01, 1.0):
        q = dict(p)
        q["head"] = ((h[0][0], h[0][1], h[0][2] + a), h[1])
        c = _spike_pen(q, "right") + _spike_pen(q, "left") + 0.004 * a * a
        if c < best[0] - 1e-6:
            best = (c, float(a))
    return best[1]


_TURNS = [(x, y, z) for x in (-10.0, -5.0, 0.0, 5.0, 10.0) for y in np.arange(-15.0, 15.01, 3.0) for z in np.arange(-16.0, 16.01, 4.0)]
TURN_LIM = (12.0, 16.0, 18.0)
TURN_REG = 0.002
if FIX1:   # fix round 1: the hew's impact (helm into right_spikes 3.1 at t13) needs a little more tuck than the grid reached
    _TURNS = [(x, y, z) for x in (-10.0, -5.0, 0.0, 5.0, 10.0) for y in np.arange(-21.0, 21.01, 3.0) for z in np.arange(-24.0, 24.01, 4.0)]
    TURN_LIM = (12.0, 22.0, 24.0)
    TURN_REG = 0.0012   # and a deep spike is worth a bigger tuck


def _head_cost(q):
    return _spike_pen(q, "right") + _spike_pen(q, "left") + _helm_pen(q)


def head_turn_for(p):
    """The head's turn (deg on its own x, y, z) that takes the helm off a sinking spike or a crossing forearm: the man tucks
    his head away from his own arm. (0, 0, 0) when nothing sinks. A coarse grid, then a fine one round the best."""
    c0 = _head_cost(p)
    if c0 <= 0.05:
        return (0.0, 0.0, 0.0)
    h = p["head"]
    best = (c0, (0.0, 0.0, 0.0))

    def tryt(t):
        nonlocal best
        q = dict(p)
        q["head"] = ((h[0][0] + t[0], h[0][1] + t[1], h[0][2] + t[2]), h[1])
        c = _head_cost(q) + TURN_REG * (t[0] ** 2 + t[1] ** 2 + t[2] ** 2)
        if c < best[0] - 1e-6:
            best = (c, tuple(float(a) for a in t))
    for t in _TURNS:
        tryt(t)
    b = best[1]
    for dx in (-2.5, 0.0, 2.5):
        for dy in (-1.5, 0.0, 1.5):
            for dz in (-2.0, 0.0, 2.0):
                tryt(tuple(max(-L, min(L, v)) for L, v in zip(TURN_LIM, (b[0] + dx, b[1] + dy, b[2] + dz))))
    return best[1]


def _smooth_series(vals, width):
    """A centred moving maximum-preserving smooth: each value spread to its neighbours with a smoothstep falloff."""
    n = len(vals)
    out = list(vals)
    for i, v in enumerate(vals):
        if v == 0.0:
            continue
        for j in range(max(0, i - width), min(n, i + width + 1)):
            w = fullbody.B.smooth(1.0 - abs(j - i) / float(width + 1))
            if abs(v * w) > abs(out[j]) and (out[j] == 0.0 or (out[j] > 0) == (v > 0)):
                out[j] = v * w
    return out


def edge_env(n):
    """1 inside the clip, easing to 0 over the first and last half tick: the first and last sample stay the stance's."""
    h = SUB // 4
    return [fullbody.B.smooth(min(1.0, max(0.0, (i - h) / float(SUB // 2)))) * fullbody.B.smooth(min(1.0, max(0.0, (n - 1 - h - i) / float(SUB // 2))))
            for i in range(n)]


def head_clear(upper, legs, env=None):
    """The head tucks away from a spike or a forearm that would sink into the helm (eased; env scales it per sample)."""
    if env is None:
        env = [1.0] * len(upper)
    turns = [head_turn_for(SP.fk_pose(s, l)) for s, l in zip(upper, legs)]
    comp = []
    for k in range(3):
        v = np.array(_smooth_series([t[k] for t in turns], SUB), float)
        for _ in range(6):        # eased: a turn chosen per sample would step from one grid cell to the next (head pops)
            v[1:-1] = 0.25 * v[:-2] + 0.5 * v[1:-1] + 0.25 * v[2:]
        comp.append([float(a) * e for a, e in zip(v, env)])
    rolls = comp[2]
    for i in range(len(upper)):
        d = (comp[0][i], comp[1][i], comp[2][i])
        if any(d):
            h = upper[i]["head"]
            upper[i] = dict(upper[i], head=((h[0][0] + d[0], h[0][1] + d[1], h[0][2] + d[2]), h[1]))
    return upper, rolls


def head_clear_loop(upper, legs, passes=24):
    """head_clear for a looping clip (fix round 1, charge_run_upper: loop_pose 3.6 deg at the seam and a 15 deg head dodge
    at t4.5-6.5): the turns are smoothed round the loop (the last sample is the first), so the seam closes and a short
    dodge is spread into a gentler tuck."""
    n = len(upper)
    m = n - 1
    turns = [head_turn_for(SP.fk_pose(s, l)) for s, l in zip(upper[:m], legs[:m])]
    for k in range(3):
        v = np.array([t[k] for t in turns], float)
        for _ in range(passes):
            v = 0.25 * np.roll(v, 1) + 0.5 * v + 0.25 * np.roll(v, -1)
        for i in range(n):
            a = float(v[i % m])
            h = upper[i]["head"]
            r = list(h[0])
            r[k] += a
            upper[i] = dict(upper[i], head=(tuple(r), h[1]))
    return upper


def _ease_left_arm(upper, legs, touched, passes=8):
    """Per-sample solves can land a few degrees apart (a pop on the left arm): the re-solved samples' left arm and forearm
    angles are eased with a [1 2 1] filter (unwrapped, the untouched samples either side held), then the fist is put back on
    the haft from the eased angles with a short solve that stays near them."""
    if not touched:
        return
    n = len(upper)
    lo, hi = max(0, min(touched) - 1), min(n - 1, max(touched) + 1)
    A = np.array([list(upper[i]["left_arm"][0]) + [upper[i]["left_forearm"][0][0]] for i in range(lo, hi + 1)], float)
    A = np.degrees(np.unwrap(np.radians(A), axis=0))
    for _ in range(passes):
        A[1:-1] = 0.25 * A[:-2] + 0.5 * A[1:-1] + 0.25 * A[2:]
    for k, i in enumerate(range(lo + 1, hi)):
        p = SP.fk_pose(upper[i], legs[i])
        s = _slide(p)
        x0 = A[k + 1]

        def f(x, p=p, s=s, x0=x0):
            qq = SP._arm("left", p, x)
            return np.concatenate([4.0 * (K.point(K.world(qq, "left_forearm"), SP.FIST_L) - SP.wpoint(qq, SP.S(s))),
                                   0.05 * (x - x0)])
        x = SP.gn(f, x0.copy(), iters=30)
        upper[i] = dict(upper[i], left_arm=(tuple(float(v) for v in x[:3]), upper[i]["left_arm"][1]),
                        left_forearm=((float(x[3]), upper[i]["left_forearm"][0][1], upper[i]["left_forearm"][0][2]),
                                      upper[i]["left_forearm"][1]))


def depop_left_arm(upper, legs, step=6.0):
    """A left arm that jumps more than `step` degrees in one sample (an IK branch change at a key: chop t3.75-4) is eased
    over a tick either side of the jump and its fist put back on the haft. The first and last half tick stay as they are."""
    n = len(upper)
    A = np.array([list(upper[i]["left_arm"][0]) + [upper[i]["left_forearm"][0][0]] for i in range(n)], float)
    A = np.degrees(np.unwrap(np.radians(A), axis=0))
    jumps = [i for i in range(1, n) if np.max(np.abs(A[i] - A[i - 1])) > step]
    keep = SUB // 2
    for i in jumps:
        touched = [j for j in range(i - SUB, i + SUB) if keep <= j < n - keep]
        _ease_left_arm(upper, legs, touched, passes=16)
    return upper


def despike_left(upper, legs, step=5.0, rounds=3, protect=()):
    """Fix round 1: a left arm that jumps on one sample and back on the next (the hew's t11.5: the forearm -38, -22, -30) is
    put back between its neighbours, the fist's place along the haft too, and the fist re-put on the haft from there."""
    n = len(upper)
    for _ in range(rounds):
        A = np.array([list(upper[i]["left_arm"][0]) + [upper[i]["left_forearm"][0][0]] for i in range(n)], float)
        A = np.degrees(np.unwrap(np.radians(A), axis=0))
        S = np.array([_slide(SP.fk_pose(upper[i], legs[i])) for i in range(n)])
        mid = 0.5 * (A[:-2] + A[2:])
        bad = [i + 1 for i in range(n - 2) if np.abs(A[i + 1] - mid[i]).max() > step and SUB // 2 <= i + 1 < n - SUB // 2
               and i + 1 not in protect]
        if not bad:
            break
        for i in bad:
            y0 = 0.5 * (A[i - 1] + A[i + 1])
            s = 0.5 * (S[i - 1] + S[i + 1])
            p = SP.fk_pose(upper[i], legs[i])

            def fl(y, p=p, s=s, y0=y0):
                qq = SP._arm("left", p, y)
                return np.concatenate([4.0 * (K.point(K.world(qq, "left_forearm"), SP.FIST_L) - SP.wpoint(qq, SP.S(s))),
                                       0.05 * (y - y0)])
            y = SP.gn(fl, y0.copy(), iters=30)
            upper[i] = dict(upper[i], left_arm=(tuple(float(v) for v in y[:3]), upper[i]["left_arm"][1]),
                            left_forearm=((float(y[3]), upper[i]["left_forearm"][0][1], upper[i]["left_forearm"][0][2]),
                                          upper[i]["left_forearm"][1]))
    return upper


def left_chest_final(upper, legs, report="", target=0.9, passes=5, prior=0.12):
    """Fix round 1: the last word on the left forearm's clearance of the breastplate and belt (scorecard pen_limb). The
    helm clearance's blends and eases put the fist back on the haft without the chest term, so a forearm cleared there can
    sink again. Here each sample still over `target` is solved again (fist on the haft, its place along the haft free within
    a few px; the helm, elbow, spike and haft terms kept), the angle changes are spread and eased like right_clear's, and the
    fist is put back on the haft from the eased angles. A few passes, each on the samples still over."""
    n = len(upper)
    env = edge_env(n)
    upper = despike_left(upper, legs)
    raw = set()
    for pss in range(passes + 1):
        last = pss == passes   # the last pass: a sample still over (the hew's strike, where the haft whips through a
        D = np.zeros((n, 5))   # quarter tick and a blend of its neighbours' changes misses it) takes its own solve, held
        need = 0               # near the middle of its neighbours (no pop)
        if last:
            A = np.array([list(u["left_arm"][0]) + [u["left_forearm"][0][0]] for u in upper], float)
            A = np.degrees(np.unwrap(np.radians(A), axis=0))
        for i in range(n):
            p = SP.fk_pose(upper[i], legs[i])
            if env[i] <= 0.0 or (chest_pen(p, "left") <= (target + 0.15 if last else target)
                                 and (last or haft_in_left_arm(p) <= 0.4)):   # (the jab's load: the haft through the upper arm)
                continue
            a0 = np.asarray(upper[i]["left_arm"][0], float)
            x0 = np.r_[a0, float(upper[i]["left_forearm"][0][0]), _slide(p)]
            h0, sp0 = _helm_pen(p), _spike_pen(p, "left")
            pr = x0[:4].copy()
            if last and 0 < i < n - 1:
                pr = pr + 0.5 * (A[i - 1] + A[i + 1]) - A[i]

            def f(x, p=p, x0=x0, h0=h0, sp0=sp0, pr=pr, w=(0.3 if last else prior)):
                q = SP._arm("left", p, x[:4])
                fl = K.point(K.world(q, "left_forearm"), SP.FIST_L)
                return np.concatenate([4.0 * (fl - SP.wpoint(q, SP.S(x[4]))),
                                       [4.0 * max(0.0, chest_pen(q, "left") - (target - CHEST_MARGIN))],
                                       [3.0 * max(0.0, _helm_pen(q) - h0)],
                                       [2.0 * max(0.0, _spike_pen(q, "left") - max(0.2, sp0))],
                                       [4.0 * haft_in_left_arm(q)],
                                       [0.25 * max(0.0, _elbow(q) - ELBOW_MAX + 7.0)],
                                       w * (x[:4] - pr), [0.1 * (x[4] - x0[4])]])
            clamp = lambda x, s0=x0[4]: np.r_[x[:3], min(-2.0, max(-150.0, x[3])), min(s0 + 3.0, max(s0 - 3.0, x[4]))]  # noqa: E731
            D[i] = SP.gn(f, x0.copy(), iters=40, clamp=clamp) - x0
            need += 1
            if last:
                x = x0 + D[i]
                upper[i] = dict(upper[i], left_arm=(tuple(float(v) for v in x[:3]), upper[i]["left_arm"][1]),
                                left_forearm=((float(x[3]), upper[i]["left_forearm"][0][1], upper[i]["left_forearm"][0][2]),
                                              upper[i]["left_forearm"][1]))
                raw.add(i)
        if not need or last:
            break
        for k in range(5):
            D[:, k] = _smooth_series(list(D[:, k]), SUB)
        for _ in range(4):
            D[1:-1] = 0.25 * D[:-2] + 0.5 * D[1:-1] + 0.25 * D[2:]
        for i in range(n):
            d = D[i] * env[i]
            if not np.abs(d).max() > 1e-6:
                continue
            p = SP.fk_pose(upper[i], legs[i])
            y0 = np.r_[np.asarray(upper[i]["left_arm"][0], float), float(upper[i]["left_forearm"][0][0])] + d[:4]
            s = _slide(p) + d[4]

            def fl(y, p=p, s=s, y0=y0):
                qq = SP._arm("left", p, y)
                return np.concatenate([4.0 * (K.point(K.world(qq, "left_forearm"), SP.FIST_L) - SP.wpoint(qq, SP.S(s))),
                                       0.05 * (y - y0)])
            y = SP.gn(fl, y0.copy(), iters=30)
            upper[i] = dict(upper[i], left_arm=(tuple(float(v) for v in y[:3]), upper[i]["left_arm"][1]),
                            left_forearm=((float(y[3]), upper[i]["left_forearm"][0][1], upper[i]["left_forearm"][0][2]),
                                          upper[i]["left_forearm"][1]))
    upper = despike_left(upper, legs, protect=raw)
    if report:
        print("  %s left forearm off the chest (final): after %.2f px, helm %.2f px, elbow %.0f deg" % (
            report, max(chest_pen(SP.fk_pose(s_, l_), "left") for s_, l_ in zip(upper, legs)),
            max(_helm_pen(SP.fk_pose(s_, l_)) for s_, l_ in zip(upper, legs)),
            max(_elbow(SP.fk_pose(s_, l_)) for s_, l_ in zip(upper, legs))))
    return upper


CLEAR_WINDOW = {"haft_jab": (10.0, 28.0)}   # the jab's wind-up and shove keep their own left arm (a re-solve there popped)
CHEST_WINDOW = {}   # fix round 1: a window for the chest term alone (tried on the jab's load: it popped the left arm; left_chest_final does it)


def clear_helm(upper, legs, report="", edges=False, head=True, window=None, chest=False):
    """Both helm fixes over a clip's samples (absolute upper and legs), each eased in and out over a tick around the samples
    that need it. The left fist stays on the haft on every sample. edges: the first and last tick are left exactly as they
    are (an attack starts and ends on the stance) and the fixes ease in over the tick after and out over the tick before."""
    n = len(upper)
    env = [1.0] * n
    if edges:
        env = edge_env(n)
    env_head = list(env)
    if window:
        env = [e * fullbody.B.smooth(min(1.0, max(0.0, (i / float(SUB) - window[0]) / 1.0))) for i, e in enumerate(env)]
        if len(window) > 2:   # (start, end, ...): eased out over the tick before the end too
            env = [e * fullbody.B.smooth(min(1.0, max(0.0, (window[1] - i / float(SUB)) / 1.0))) for i, e in enumerate(env)]
    sols, warm = [None] * n, None
    for i in range(n):
        if env[i] <= 0.0:
            warm = None
            continue
        r = left_clear(SP.fk_pose(upper[i], legs[i]), warm, chest=chest)
        sols[i] = r
        warm = None if r is None else (*r[0], r[1], r[2])
    need = [i for i, r in enumerate(sols) if r is not None]
    if need:
        # ease each fixed run in and out over a tick: the arm angles blend from the clip's own to the solved ones, then the
        # fist is put back on the haft by a short solve from the blend (no clearance terms needed there)
        w = [0.0] * n
        for i in need:
            for j in range(max(0, i - SUB), min(n, i + SUB + 1)):
                w[j] = max(w[j], 1.0 if j == i else fullbody.B.smooth(1.0 - abs(j - i) / float(SUB + 1)))
        last = None
        for i in range(n):
            if sols[i] is not None:
                last = sols[i]
            w[i] *= env[i]
            if w[i] <= 0.0:
                continue
            tgt = sols[i]
            if tgt is None:   # a ramp sample: blend toward the nearest solved sample
                k = min(need, key=lambda j: abs(j - i))
                tgt = sols[k]
            a0 = np.asarray(upper[i]["left_arm"][0], float)
            f0 = float(upper[i]["left_forearm"][0][0])
            a = a0 + w[i] * (np.asarray(tgt[0]) - a0)
            fx = f0 + w[i] * (tgt[1] - f0)
            p = SP.fk_pose(upper[i], legs[i])
            s = _slide(p) + w[i] * (tgt[2] - _slide(p))
            q = SP._arm("left", p, np.r_[a, fx])

            def f(x):
                qq = SP._arm("left", p, x)
                return np.concatenate([4.0 * (K.point(K.world(qq, "left_forearm"), SP.FIST_L) - SP.wpoint(qq, SP.S(s))),
                                       0.05 * (x - np.r_[a, fx])])
            x = SP.gn(f, np.r_[a, fx], iters=30)
            upper[i] = dict(upper[i], left_arm=(tuple(float(v) for v in x[:3]), upper[i]["left_arm"][1]),
                            left_forearm=((float(x[3]), upper[i]["left_forearm"][0][1], upper[i]["left_forearm"][0][2]),
                                          upper[i]["left_forearm"][1]))
        _ease_left_arm(upper, legs, [i for i in range(n) if w[i] > 0.0])
    rolls = [0.0]
    if head:
        upper, rolls = head_clear(upper, legs, env_head)
    if report:
        pen = max(_helm_pen(SP.fk_pose(s, l)) for s, l in zip(upper, legs))
        elb = max(_elbow(SP.fk_pose(s, l)) for s, l in zip(upper, legs))
        print("  %s helm clearance: %d samples re-solved, head roll max %.1f deg; after: cuff in helm %.2f px, elbow %.0f deg"
              % (report, len(need), max((abs(a) for a in rolls), default=0.0), pen, elb))
    return upper


ATTACK_CAPE = 0.45 if FIX1 else 0.6   # (fix round 1: cape_cloth into the tabard 2.06 at the chop's t15) the cape swings a little less in the attacks: it hung into the tabard's back at the chop's follow-through (t14.5)


APRON_LIFT = {"overhead_chop": (-5.0, (2.0, 8.0, 20.0, 27.0))}   # deg on x, eased in t2-8 and out t20-27
ATTACK_APRON = 0.7 if FIX1 else None   # fix round 1: apron into left_leg 1.96 at the chop's t13.5 (the lunge's front thigh)


def calm_cape(upper, scale, bones=None):
    for b in bones or HIT_CAPE:
        if b not in upper[0]:
            continue
        r0 = np.asarray(upper[0][b][0], float)
        for i in range(len(upper)):
            r = r0 + scale * (np.asarray(upper[i][b][0], float) - r0)
            upper[i] = dict(upper[i])
            upper[i][b] = (tuple(float(a) for a in r), upper[i][b][1])
    return upper


def review_attack_clips(names=None):
    out = {}
    for name in names or ATTACKS:
        upper, legs = attack_clip(name)
        if FIX1:
            upper = right_clear(upper, legs, report="attack_" + name)
        if FIX1 and name in CHEST_WINDOW:   # a window clear_helm leaves alone still keeps the left forearm off the chest
            upper = clear_helm(upper, legs, report="attack_%s (chest)" % name, edges=True, window=CHEST_WINDOW[name], head=False,
                               chest="only")
        upper = clear_helm(upper, legs, report="attack_" + name, edges=True, window=CLEAR_WINDOW.get(name), chest=FIX1)
        upper = depop_left_arm(upper, legs)
        if FIX1:
            upper = left_chest_final(upper, legs, report="attack_" + name)
        upper = SP.gear(upper, legs)
        print("attack_%s soft motion:" % name)
        upper, legs = soften(upper, legs, report=True)
        upper = calm_cape(upper, ATTACK_CAPE)
        if ATTACK_APRON:
            upper = calm_cape(upper, ATTACK_APRON, bones=("apron",))
        if FIX1 and name in APRON_LIFT:   # the apron rides a little forward off the lunging thigh, eased in and out
            dx, (a, b, c, d) = APRON_LIFT[name]
            for i in range(len(upper)):
                t = i / float(SUB)
                w = fullbody.B.smooth(min(1.0, max(0.0, (t - a) / (b - a)))) * fullbody.B.smooth(min(1.0, max(0.0, (d - t) / (d - c))))
                if w > 0.0 and "apron" in upper[i]:
                    r, pv = upper[i]["apron"]
                    upper[i] = dict(upper[i], apron=((r[0] + dx * w, r[1], r[2]), pv))
        clip = K.bake(upper, ATTACK_TICKS, loop=False)
        clip["timeline"] = {K.fmt_t(IMPACT[name] / SP.TPS): "impact"}
        out["attack_" + name] = clip
        out["attack_%s_legs" % name] = K.bake(legs, ATTACK_TICKS, loop=False)
    return out


def guard_pose():
    """The guard as the hit layer sits on it: the solved upper body (absolute) and the ready legs."""
    legs = K.attack_legs({"hips": [(0, *READY_HIPS), (1, *READY_HIPS)], "steps": []}, ticks=1)[0]
    s = SP.sample([(0, GUARD), (1, GUARD)], legs, SP.Solver(), 0.0)
    return s, legs


def soft_additive(offs, legs_abs, ticks):
    """Soft motion for an additive clip: its offsets are laid on the guard, the pieces swing on the result, and their keys
    (rest 0 in every stance) go back into the offsets; pieces on the legs go into the returned legs-offset keys."""
    base, _ = guard_pose()
    comp = []
    for o in offs:
        s = {b: (tuple(float(a) for a in v[0]), v[1]) for b, v in base.items()}
        for b, (r, p) in o.items():
            r0 = s.get(b, ((0.0, 0.0, 0.0), None))[0]
            s[b] = (tuple(float(x + y) for x, y in zip(r0, r)), p)
        comp.append(s)
    lg = [dict(l) for l in legs_abs]
    up, lg = soften(comp, lg)
    up = hit_pieces(up, lg)
    bones = piece_bones()
    up_keys = [{b: v for b, v in s.items() if b in bones} for s in up]
    leg_keys = [{b: v for b, v in s.items() if b in bones} for s in lg]
    return up_keys, leg_keys


HIT_CAPE = ("cape", "cape_cloth")
HIT_CAPE_SCALE = 0.45   # the cape hangs 1 px off the tabard's back: in a hit it swings a little less (hit_medium_back t5)
if FIX1:   # fix round 1: cape_cloth into the tabard 1.96 (hit_medium_back t3.5) and 2.29 (hit_pulled t4)
    HIT_CAPE_SCALE = 0.3
PLATE_JOLT = 5.0        # deg: a pauldron knocked on its strap by the blow, where the arm itself barely moves (hit_block)


def hit_pieces(up, lg):
    """The hits' pieces, after soft motion: each piece's keys eased with a [1 2 1] filter twice (a 9-tick flinch is short for
    the kit's one-tick fade-in: hit_light_back's collar kinked 15.6 deg at t1.5), the cape a little calmer, and a
    pauldron that would stay still on an arm that hardly moves knocked on its strap by the blow. The plates then turn off
    the helm as in the attacks."""
    n = len(up)
    parts, plates = soft_parts()
    pb = {b for c in parts.values() for b in c["bones"]} | set(plates)
    for frames in (up, lg):
        n = len(frames)
        for b in pb:
            if b not in frames[0]:
                continue
            r = np.array([np.asarray(s[b][0], float) for s in frames])
            r0 = r[0].copy()
            if b in HIT_CAPE and float(np.ptp(r, axis=0).max()) > 8.0:   # a short flinch keeps its whole swing
                r = r0 + HIT_CAPE_SCALE * (r - r0)
            for _ in range(2):
                m = r.copy()
                m[1:-1] = 0.25 * r[:-2] + 0.5 * r[1:-1] + 0.25 * r[2:]
                r = m
            if float(np.ptp(r[:, 0])) + float(np.ptp(r[:, 2])) < 8.0:   # knocked by the blow where its limb hardly moves
                for i in range(n):
                    t = i / float(SUB)
                    fade = fullbody.B.smooth(min(1.0, t / 0.75)) * fullbody.B.smooth(min(1.0, (n - 1 - i) / float(2 * SUB)))
                    r[i, 0] += PLATE_JOLT * math.exp(-t / 3.0) * math.sin(2 * math.pi * t / 4.0) * fade
            for i in range(n):
                frames[i] = dict(frames[i])
                frames[i][b] = (tuple(float(a) for a in r[i]), frames[i][b][1])
    return plate_clear(up, lg)


def haft_offsets(offs, loffs, report="", slide=(-2.5, 4.0), settle=False):
    """Fix round 1 (2026-11-12): an additive hit over the guard with both fists on the haft. The shared reaction tables add
    the same arm offsets to both arms, and on this guard (the left arm reaching across, turned 92 deg on its z) that folds
    the left elbow backward with the fist into the helm (hit_pulled 151 deg / 2.7 px, hit_knockback 160 / 3.1). Here the
    blow moves the body, the head and the right arm (the axe goes with the right fist); the left arm is solved each sample
    so its fist stays on the haft near its guard place, the cuff and gauntlet off the helm and the breastplate and the elbow
    within the shoulder's reach, warm-started from the sample before (no branch change), then eased. Returns the offsets
    with new left_arm / left_forearm channels (0 where the clip's own offsets are 0)."""
    g_up, g_legs = guard_pose()
    n = len(offs)
    gp = SP.fk_pose(g_up, g_legs)
    s0 = _slide(gp)
    a_g = np.r_[np.asarray(g_up["left_arm"][0], float), float(g_up["left_forearm"][0][0])]
    comp = []
    for i in range(n):
        u = {b: (tuple(float(x) for x in v[0]), v[1]) for b, v in g_up.items()}
        for b, (r, _p) in offs[i].items():
            if b in SP.GEO_BONES and b not in piece_bones():
                r0, p0 = u.get(b, (Z3, None))
                u[b] = (_add(r0, r), p0)
        lg = {b: (tuple(float(x) for x in v[0]), v[1]) for b, v in g_legs.items()}
        for b, (r, p) in (loffs[i].items() if loffs else ()):
            if b in ("hips", "right_leg", "left_leg", "right_shin", "left_shin", "right_foot", "left_foot"):
                r0, p0 = lg.get(b, (Z3, None))
                lg[b] = (_add(r0, r), p0 if p is None else _add(p0 or Z3, p))
        comp.append((u, lg))
    quiet = [max([abs(float(a)) for b, (r, _p) in offs[i].items() for a in r] + [0.0]) < 1e-6 for i in range(n)]
    X = np.tile(np.r_[a_g, s0], (n, 1))
    warm = np.r_[a_g, s0]
    for i in range(n):
        if quiet[i]:
            warm = np.r_[a_g, s0]
            continue
        p = SP.fk_pose(*comp[i])

        def f(x, p=p):
            q = SP._arm("left", p, x[:4])
            Mh, Mf = K.world(q, "head"), K.world(q, "left_forearm")
            res = [4.0 * (K.point(Mf, SP.FIST_L) - SP.wpoint(q, SP.S(x[4])))]
            res.append([6.0 * max(0.0, _box_pen(_local_px(Mh, K.point(Mf, c)), r + 0.9)) for c, r in ARM_PTS])
            res.append([3.0 * max(0.0, chest_pen(q, "left") - (CHEST_MAX - CHEST_MARGIN))])
            res.append([0.25 * max(0.0, _elbow(q) - (ELBOW_MAX - 7.0))])
            res.append(0.08 * (x[:4] - a_g))   # (the elbow's swing about the shoulder-fist line held near the guard's)
            res.append([0.15 * (x[4] - s0)])
            return np.concatenate([np.ravel(r_) for r_ in res])
        clamp = lambda x: np.r_[x[:3], min(-2.0, max(-150.0, x[3])), min(s0 + slide[1], max(s0 + slide[0], x[4]))]  # noqa: E731
        X[i] = warm = SP.gn(f, warm.copy(), iters=40, clamp=clamp)
    X[:, :4] = np.degrees(np.unwrap(np.radians(X[:, :4]), axis=0))
    for _ in range(6):
        X[1:-1] = 0.25 * X[:-2] + 0.5 * X[1:-1] + 0.25 * X[2:]
    g = np.r_[a_g, s0]
    for i in range(n if settle else 0):   # a layered clip's last two ticks settle on the guard's own left arm (the pull and
        e = fullbody.B.smooth(min(1.0, (n - 1 - i) / float(2 * SUB)))   # the knockback ended 20 deg off it: settle)
        X[i] = g + e * (X[i] - g)
    out = [dict(o) for o in offs]
    worst = [0.0, 0.0, 0.0]
    for i in range(n):
        if quiet[i]:
            out[i]["left_arm"] = (Z3, None)
            out[i]["left_forearm"] = (Z3, None)
            continue
        p = SP.fk_pose(*comp[i])
        x0, s = X[i, :4], float(X[i, 4])

        def fl(x, p=p, s=s, x0=x0):
            q = SP._arm("left", p, x)
            return np.concatenate([4.0 * (K.point(K.world(q, "left_forearm"), SP.FIST_L) - SP.wpoint(q, SP.S(s))), 0.05 * (x - x0)])
        x = SP.gn(fl, x0.copy(), iters=30)
        q = SP._arm("left", p, x)
        worst = [max(worst[0], _helm_pen(q)), max(worst[1], chest_pen(q, "left")), max(worst[2], _elbow(q))]
        lf0 = offs[i].get("left_forearm", (Z3, None))[0]
        out[i]["left_arm"] = (tuple(float(a - b) for a, b in zip(x[:3], a_g[:3])), None)
        out[i]["left_forearm"] = ((float(x[3] - a_g[3]), float(lf0[1]), float(lf0[2])), None)
    if report:
        print("  %s both fists on the haft: left cuff in helm %.2f px, in chest %.2f px, elbow %.0f deg" % (report, *worst))
    return out


def _hit_legs_abs():
    """K.hit_legs_clips' leg samples before they are made offsets: {clip: (absolute samples, ticks)}."""
    out = {}
    r_rot, r_pos = READY_HIPS
    for d, spec in K.HIT_LEGS.items():
        med = K.HIT_TICKS["medium"]
        out["hit_medium_%s" % d] = (K.attack_legs(spec, ticks=med), med)
        k2 = spec["hips"][1]
        mid = lambda k: (tuple(a + k * (b - a) for a, b in zip(r_rot, k2[1])), tuple(a + k * (b - a) for a, b in zip(r_pos, k2[2])))
        lt = K.HIT_TICKS["light"]
        light = {"hips": [(0, *READY_HIPS), (1.5, *mid(0.45)), (4, *mid(-0.08)), (lt, *READY_HIPS)], "steps": []}
        out["hit_light_%s" % d] = (K.attack_legs(light, ticks=lt), lt)
    out["hit_block"] = (K.attack_legs(K.BLOCK_LEGS, ticks=K.BLOCK_TICKS), K.BLOCK_TICKS)
    return out


HAFT_HITS = ("hit_medium_front", "hit_medium_back", "hit_medium_left")   # fix round 1: the left fist kept on the haft


def review_hit_clips():
    """SP.hit_clips with the pieces swinging: the same offsets and footwork, each piece's keys added to both."""
    out = _hit_clips()
    legs_abs = _hit_legs_abs()
    for name, (labs, lt) in legs_abs.items():
        weight, d = ("light", "front") if name == "hit_block" else name.split("_")[1:]
        clip = out["hit_%s_%s" % (weight, d)]
        ut = K.HIT_TICKS[weight]
        keys = K.front_onset(SP.hit_keys(SP.HIT_PEAK[d], weight), d, weight)
        offs = [K.sample_keys(keys, min(i, ut * SUB) / SUB) for i in range(lt * SUB + 1)]
        haft = FIX1 and name in HAFT_HITS
        if haft:
            lo0 = K.legs_offset([dict(f) for f in labs])
            offs = haft_offsets(offs, lo0, report=name)
        up_keys, leg_keys = soft_additive(offs, labs, lt)
        if lt != ut:   # the block: its upper (the light flinch, 9 ticks) settles on its own length, the legs on theirs
            up_keys = soft_additive(offs[:ut * SUB + 1], labs[:ut * SUB + 1], ut)[0]
        print("%s soft motion: %d upper, %d legs pieces" % (name, len(up_keys[0]), len(leg_keys[0])))
        new_up = dict(clip)
        new_up["bones"] = dict(clip["bones"])
        new_up["bones"].update(K.bake(up_keys[:ut * SUB + 1], ut, loop=False)["bones"])
        if haft:
            new_up["bones"].update(K.bake([{b: f[b] for b in ("left_arm", "left_forearm")} for f in offs[:ut * SUB + 1]], ut,
                                          loop=False)["bones"])
        out[name] = new_up
        # The legs layer again from its own samples: a shin offset that would bend the knee backward is held at -1 deg
        # (hit_medium_back's left shin, -9 at t11.5) and a boot that dips under the ground lifts the hips (hit_medium_right)
        loffs = K.legs_offset(labs)
        for f in loffs:
            for side in ("right", "left"):
                (sx, sy, sz), sp = f[side + "_shin"]
                if sx < -1.0:
                    f[side + "_shin"] = ((-1.0, sy, sz), sp)
        loffs = hips_lift(offs, loffs, report=name)
        for f, k in zip(loffs, leg_keys):
            f.update(k)
        out[name + "_legs"] = K.bake(loffs, lt, loop=False)
    return out


_hit_clips = SP.hit_clips
if REVIEW:
    SP.attack_clips = attack_clips
    SP.hit_clips = review_hit_clips
elif SP.V2C:
    SP.attack_clips = attack_clips


# ----------------------------------------------------------------------------------------------- the layered clips
import stances as LS   # noqa: E402
import charge as LC    # noqa: E402
import reactions as LR  # noqa: E402

ASSET = HERE.parents[2] / "src/main/resources/assets/gravesandglory/animations/troop" / ("%s.animation.json" % SP.TROOP)
LAYERED_NAMES = ("stance_rally_upper", "stance_hold_upper", "stance_hold_legs", "stance_fallback_upper", "stance_fallback_legs",
                 "hit_pulled", "hit_pulled_legs", "charge_run_upper", "stun_upper", "stun_legs", "stun_out_upper", "hit_knockback",
                 "hit_knockback_legs")


def layered_clips(path):
    """The shared modules' clips for this troop, built on the clip file at path (this build's own clips)."""
    tr = LS.Troop(SP.TROOP, path)
    clips = {}
    clips.update(LS.build_all(tr))
    clips.update(LC.build_all(tr))
    clips.update(LR.build_all(tr))
    if REVIEW:
        fix_layered(tr, clips)
    return clips


def _unbake(clip):
    ticks = int(round(clip["animation_length"] * SP.TPS))
    frames = []
    for i in range(ticks * SUB + 1):
        P = LS.pose_at(clip, i / (SUB * SP.TPS))
        frames.append({b: (tuple(float(x) for x in r), None if q is None else tuple(float(x) for x in q)) for b, (r, q) in P.items()})
    return frames, ticks


def _rebake(frames, ticks, loop):
    return LS.bake([{b: (np.array(r, float), None if q is None else np.array(q, float)) for b, (r, q) in f.items()} for f in frames],
                   ticks, loop)


def _at(frames, i):
    return frames[i % (len(frames) - 1)] if len(frames) > 1 else frames[0]


KNEE_PARTS = ("right_knee_wing", "left_knee_wing", "right_knee", "left_knee", "right_greave", "left_greave")
FLOOR_PARTS = KNEE_PARTS + ("right_boot", "left_boot")
FLOOR_TOL = 0.2   # px: what the check passes (0.5) less a margin


def floor_depth(pose, parts=FLOOR_PARTS):
    """How far the lowest of `parts` sits under the ground (px, 0 when on or over it), and which part."""
    P, owner = K.body_corners({b: (v[0], v[1] or Z3) for b, v in pose.items()})
    best = (0.0, None)
    for y, o in zip(P[:, 1], owner):
        if o in parts and -y > best[0]:
            best = (float(-y), o)
    return best


Z3 = (0.0, 0.0, 0.0)


def legs_raise(legs, upper, margin=0.35, report=""):
    """Absolute legs: where a knee plate or boot dips under the ground, the pelvis is raised just enough (eased over a tick
    either side) and both legs are solved again to the same ankles and the same foot turn: the knees bend less, the feet
    stay where they were."""
    n = len(legs)
    for _ in range(3):
        dep = [floor_depth(SP.fk_pose(_at(upper, i), legs[i]), FLOOR_PARTS)[0] for i in range(n)]
        need = [d + margin if d > FLOOR_TOL else 0.0 for d in dep]
        if not any(need):
            break
        lift = _smooth_series(need, SUB)
        for i, dh in enumerate(lift):
            if dh <= 0.0:
                continue
            pose = SP.fk_pose(_at(upper, i), legs[i])
            targets = {}
            for side in ("right", "left"):
                Mf = K.world(pose, side + "_foot")
                targets[side] = (K.point(Mf, K.ankle_px(side)), Mf[:3, :3])
            hr, hp = legs[i]["hips"]
            hp = hp or Z3

            def attempt(h):
                nw = dict(legs[i])
                nw["hips"] = (hr, (hp[0], hp[1] + h, hp[2]))
                qq = SP.fk_pose(_at(upper, i), nw)
                try:
                    return nw, qq, {side: K.leg_pose(qq, side, targets[side][0], 0.0,
                                                     guess=(legs[i][side + "_leg"][0][0], legs[i][side + "_leg"][0][2],
                                                            legs[i][side + "_shin"][0][0])) for side in ("right", "left")}
                except ValueError:
                    return None
            got = attempt(dh)
            if got is None:         # the front leg straightens first: raise as far as both legs still reach
                lo, hi = 0.0, dh
                for _ in range(6):
                    mid = 0.5 * (lo + hi)
                    g = attempt(mid)
                    if g is None:
                        hi = mid
                    else:
                        lo, got = mid, g
                if got is None:
                    continue
            new, q, sol = got
            for side in ("right", "left"):
                lr, sr, _ = sol[side]
                q[side + "_leg"], q[side + "_shin"] = (lr, Z3), (sr, Z3)
                Ms = K.world(q, side + "_shin")
                fr = K.decompose_zyx(Ms[:3, :3].T @ targets[side][1])
                new[side + "_leg"], new[side + "_shin"], new[side + "_foot"] = (lr, None), (sr, None), (fr, None)
                if side + "_boot" in new:
                    new[side + "_boot"] = (fr, new[side + "_boot"][1])
            legs[i] = new
    if report:
        print("  %s floor: lowest plate now %.2f px under" % (report, max(floor_depth(SP.fk_pose(_at(upper, i), legs[i]))[0] for i in range(n))))
    return legs


def _over_guard(up_off, leg_off):
    """An additive sample laid on the guard (fix round 1: the hits are judged over the ready stance)."""
    g_up, g_legs = guard_pose()
    u = {b: (tuple(float(x) for x in v[0]), v[1]) for b, v in g_up.items()}
    for b, (r, _p) in up_off.items():
        r0, p0 = u.get(b, (Z3, None))
        u[b] = (_add(r0, r), p0)
    lg = {b: (tuple(float(x) for x in v[0]), v[1]) for b, v in g_legs.items()}
    for b, (r, p) in leg_off.items():
        r0, p0 = lg.get(b, (Z3, None))
        lg[b] = (_add(r0, r), p0 if p is None else _add(p0 or Z3, p))
    return u, lg


def hips_lift(upper, legs, margin=0.3, report="", guard=False):
    """Additive legs (a hit layer, judged and played as offsets): where a boot or knee plate dips under the ground, the
    hips' offset is raised by that much, eased over a tick either side. guard: measured over the guard (fix round 1)."""
    n = len(legs)
    for _ in range(3):
        if guard:
            dep = [floor_depth(SP.fk_pose(*_over_guard(_at(upper, i), legs[i])))[0] for i in range(n)]
        else:
            dep = [floor_depth(SP.fk_pose(_at(upper, i), legs[i]))[0] for i in range(n)]
        need = [d + margin if d > FLOOR_TOL else 0.0 for d in dep]
        if not any(need):
            break
        lift = _smooth_series(need, SUB)
        for i, dh in enumerate(lift):
            if dh > 0.0:
                hr, hp = legs[i].get("hips", (Z3, None))
                hp = hp or Z3
                legs[i] = dict(legs[i], hips=(hr, (hp[0], hp[1] + dh, hp[2])))
    if report:
        print("  %s floor: lowest plate now %.2f px under" % (report, max(floor_depth(SP.fk_pose(_at(upper, i), legs[i]))[0] for i in range(n))))
    return legs


LOOP_SWAY = 5.0 if FIX2 else 3.0   # deg (fix round 2: 5, so the right tasset swings past 4 deg in the stun)


def loop_sway(frames, entry, loop_ticks):
    """A piece that would hang still in a loop (the stun: the hips hardly move, so the tassets and skirts never swing)
    sways gently on its own, twice per loop, so the loop still closes (0 at the loop's start and end)."""
    parts, plates = soft_parts()
    pb = {b for c in parts.values() for b in c["bones"]} | set(plates)
    n0 = entry * SUB
    out = [dict(f) for f in frames]
    for k, b in enumerate(sorted(pb)):
        if b not in frames[0]:
            continue
        r = np.array([np.asarray(f[b][0], float) for f in frames[n0:]])
        if float(np.ptp(r[:, 0])) + float(np.ptp(r[:, 2])) >= 8.0:
            continue
        for i in range(n0, len(frames)):
            t = (i - n0) / float(SUB)
            a = LOOP_SWAY * math.sin(2 * math.pi * t / (loop_ticks / 2.0) + 0.0)
            v = out[i][b]
            out[i][b] = ((v[0][0] + a, v[0][1], v[0][2]), v[1])
    return out


def soft_loop(upper, legs, entry, loop_ticks):
    """Soft motion on a clip whose ticks entry..end loop (the stun): the springs run over the entry and four passes of the
    loop; the loop takes the settled third pass (so its last sample is its first), the entry blends into it."""
    n0, nl = entry * SUB, loop_ticks * SUB
    ext_u = upper[:n0] + [upper[n0 + (k % nl)] for k in range(4 * nl + 1)]
    ext_l = legs[:n0] + [legs[n0 + (k % nl)] for k in range(4 * nl + 1)]
    su, sl = soften([dict(s) for s in ext_u], [dict(s) for s in ext_l])
    bones = piece_bones()
    cyc = n0 + 2 * nl
    out_u, out_l = [dict(s) for s in upper], [dict(s) for s in legs]
    for i in range(len(upper)):
        if i >= n0:
            src = cyc + (i - n0)
            pu, pl = su[src], sl[src]
        else:
            w = fullbody.B.smooth(i / float(n0))
            pu, pl = {}, {}
            for dst, a, b in ((pu, su[i], su[cyc + nl - n0 + i]), (pl, sl[i], sl[cyc + nl - n0 + i])):
                for k in set(a) | set(b):
                    if k in bones and k in a and k in b:
                        ra, rb = np.array(a[k][0]), np.array(b[k][0])
                        pa, pb = a[k][1], b[k][1]
                        pos = None if pa is None and pb is None else tuple(np.array(pa or Z3) * (1 - w) + np.array(pb or Z3) * w)
                        dst[k] = (tuple(ra * (1 - w) + rb * w), pos)
        for b in bones:
            for dst, src_ in ((out_u[i], pu), (out_l[i], pl)):
                dst.pop(b, None)
                if b in src_:
                    dst[b] = src_[b]
    return out_u, out_l


def _add(a, b):
    return tuple(float(x + y) for x, y in zip(a, b))


def soft_offsets(up_offs, leg_offs, base_up=None, base_legs=None):
    """Soft motion for an additive clip and its additive legs: both laid on the guard (or the given absolute frames), the
    pieces swung, and their keys put back into the offsets (rest 0 in every stance)."""
    g_up, g_legs = guard_pose()
    n = len(up_offs)
    comp_u, comp_l = [], []
    for i in range(n):
        bu = base_up[i] if base_up else g_up
        bl = base_legs[i] if base_legs else g_legs
        u = {b: (tuple(float(x) for x in v[0]), v[1]) for b, v in bu.items()}
        for b, (r, p) in up_offs[i].items():
            r0, p0 = u.get(b, (Z3, None))
            u[b] = (_add(r0, r), p0 if p is None else _add(p0 or Z3, p))
        l = {b: (tuple(float(x) for x in v[0]), v[1]) for b, v in bl.items()}
        for b, (r, p) in leg_offs[i].items():
            r0, p0 = l.get(b, (Z3, None))
            l[b] = (_add(r0, r), p0 if p is None else _add(p0 or Z3, p))
        comp_u.append(u)
        comp_l.append(l)
    bones = piece_bones()
    for i in range(n):            # the pieces are measured from the clip's own rest: strip what the base gives them
        for s in (comp_u[i], comp_l[i]):
            for b in list(s):
                if b in bones:
                    s.pop(b)
    su, sl = soften(comp_u, comp_l)
    su = hit_pieces(su, sl)
    out_u = [dict(o) for o in up_offs]
    out_l = [dict(o) for o in leg_offs]
    for i in range(n):
        for dst, src in ((out_u[i], su[i]), (out_l[i], sl[i])):
            for b in bones:
                dst.pop(b, None)
        for b in bones:
            if b in su[i]:
                out_u[i][b] = su[i][b]
            if b in sl[i]:
                out_l[i][b] = sl[i][b]
    return out_u, out_l


def fix_layered(tr, clips):
    """The layered clips fixed for this rig (batch review 2026-11-11). Each clip is read back from the shared module's bake,
    fixed sample by sample, and baked again with the module's own baker."""
    ready_legs, _ = _unbake(tr.anims["stance_ready_legs"])
    rl0 = ready_legs[0]

    # stance_rally_upper: left_cuff into the helm and the left elbow bending backward (155) at the top of the heave
    up, t = _unbake(clips["stance_rally_upper"])
    up = clear_helm(up, [rl0] * len(up), report="stance_rally_upper")
    clips["stance_rally_upper"] = _rebake(up, t, clips["stance_rally_upper"]["loop"])

    # stance_hold: left_cuff into the helm (t35.75); right_knee_wing under the ground (t27.25)
    up, t = _unbake(clips["stance_hold_upper"])
    lg, tl = _unbake(clips["stance_hold_legs"])
    lg = legs_raise(lg, up, report="stance_hold")
    up = clear_helm(up, [_at(lg, i) for i in range(len(up))], report="stance_hold_upper")
    clips["stance_hold_upper"] = _rebake(up, t, clips["stance_hold_upper"]["loop"])
    clips["stance_hold_legs"] = _rebake(lg, tl, clips["stance_hold_legs"]["loop"])

    # charge_run_upper (on the run legs): left_cuff and the axe head into the helm, the left elbow out of reach
    up, t = _unbake(clips["charge_run_upper"])
    run, _ = _unbake(tr.anims["run_legs"])
    lr = [_at(run, i) for i in range(len(up))]
    up = clear_helm(up, lr, report="charge_run_upper", head=not FIX1)
    if FIX1:
        up = head_clear_loop(up, lr)
    clips["charge_run_upper"] = _rebake(up, t, clips["charge_run_upper"]["loop"])

    # stun: the axe and right forearm into the hips (the arms hung 4-8 deg too low for this rig's tassets), right_knee_wing
    # under the ground in the buckle, the left spikes into the helm in the snap; the pieces hang still
    up, t = _unbake(clips["stun_upper"])
    lg, tl = _unbake(clips["stun_legs"])
    for i, s in enumerate(up):
        P = {b: [np.array(r, float), None if q is None else np.array(q, float)] for b, (r, q) in s.items()}
        LS.lift_arm(P, "right", 18.0, 0.1)
        LS.lift_arm(P, "left", 16.0, 0.1)
        up[i] = {b: (tuple(float(x) for x in r), None if q is None else tuple(float(x) for x in q)) for b, (r, q) in P.items()}
    lg = legs_raise(lg, up, report="stun")
    # no clear_helm here: its left-arm re-solve popped the arm 36 deg at t1.75 and broke the loop seam (scorecard
    # loop_pose 33, pen_limb 2.73); the arm lift alone takes the left spikes from 3.45 to 2.16 px
    up, lg = soft_loop(up, lg, LR.STUN_ENTRY, LR.STUN_LOOP)
    up, lg = loop_sway(up, LR.STUN_ENTRY, LR.STUN_LOOP), loop_sway(lg, LR.STUN_ENTRY, LR.STUN_LOOP)
    clips["stun_upper"] = _rebake(up, t, clips["stun_upper"]["loop"])
    clips["stun_legs"] = _rebake(lg, tl, clips["stun_legs"]["loop"])
    stun_up, stun_lg = up, lg

    # stun_out_upper (additive, over the stun's last pose as it fades): the pieces shake with the head
    off, t = _unbake(clips["stun_out_upper"])
    base_u = [stun_up[LR.STUN_ENTRY * SUB]] * len(off)
    base_l = [stun_lg[LR.STUN_ENTRY * SUB]] * len(off)
    o_u, _ = soft_offsets(off, [{} for _ in off], base_u, base_l)
    clips["stun_out_upper"] = _rebake(o_u, t, clips["stun_out_upper"]["loop"])

    # hit_knockback / hit_pulled (additive, with additive legs): the pieces swing; a boot that dips under the ground is lifted
    for name in ("hit_knockback", "hit_pulled"):
        off, t = _unbake(clips[name])
        loff, tl = _unbake(clips[name + "_legs"])
        if FIX1:
            off = haft_offsets(off, [_at(loff, i) for i in range(len(off))], report=name, settle=True)
        o_u, o_l = soft_offsets(off, loff)
        o_l = hips_lift(o_u, o_l, report=name, guard=FIX1)
        clips[name] = _rebake(o_u, t, clips[name]["loop"])
        clips[name + "_legs"] = _rebake(o_l, tl, clips[name + "_legs"]["loop"])


# ------------------------------------------------------------------------------------------ fix round 2 (2026-11-12)
# Late, cheap fixes keyed on the finished clip file (the upstream solves stay cached). Each one was measured with
# tools/anim/focus_check.py on the exported clips before it went in (IC_BREAKER-REVIEW.md, fix round 2).
R2_SKIRT_K = 0.2   # the run's skirts trail the thighs: rotation x = -0.2 * the leg's x
R2_CHARGE_HAND = (12.0, 4.0)   # charge_run: the axe tipped forward off the helm (right_hand x, y deg); the left fist follows
R2_TABARD_K = 0.5   # hit_knockback: the tabard's forward swing halved (it swung 22 deg into the right forearm)
R2_OFFSETS = (   # (clip, bone, (dx, dy, dz) deg, (in from, in to, out from, out to) ticks): eased in and out with smoothstep
    # hit_pulled: the cape lifts off the legs as the body is yanked forward (cape into the legs, 2.28)
    ("hit_pulled", "cape", (6.0, 0.0, 0.0), (0.5, 2.5, 6.0, 9.0)),
    # hit_knockback: the head tips back with the blow, off the right gauntlet (limb_in_body)
    ("hit_knockback", "head", (-10.0, 0.0, 0.0), (0.0, 1.0, 2.5, 4.5)),
    # hit_medium_front: the cape cloth flicks with the blow (it hung still); the head leans off the apron
    ("hit_medium_front", "cape_cloth", (6.0, 0.0, 0.0), (0.5, 3.0, 5.0, 10.0)),
    ("hit_medium_front", "head", (0.0, 0.0, -3.0), (5.0, 7.5, 9.5, 12.0)),
    # stun: the right arm hangs wider of the tassets (axe and forearm into the hips), the head and left arm clear the
    # spikes in the snap, the head tips off the left plate and the cape cloth lifts off the legs for the loop
    ("stun_upper", "right_arm", (0.0, 0.0, 14.0), (1.5, 4.0, 1e9, 1e9)),
    ("stun_upper", "head", (0.0, 0.0, -5.0), (0.0, 0.75, 2.5, 4.0)),
    ("stun_upper", "left_arm", (0.0, 0.0, 8.0), (0.0, 0.5, 2.5, 4.0)),
    ("stun_upper", "head", (0.0, 0.0, -4.0), (2.0, 4.0, 1e9, 1e9)),
    ("stun_upper", "cape_cloth", (4.0, 0.0, 0.0), (2.0, 4.0, 1e9, 1e9)),
    # stun: the right tasset swings out and back once a loop (it moved 3.5 deg; loop_sway skips it, its x and z already
    # span 9 deg); both swings sit inside the loop (t4-t24), so the seam is untouched
    ("stun_legs", "right_tasset", (4.0, 0.0, 0.0), (5.0, 9.0, 11.0, 15.0)),
    ("stun_legs", "right_tasset", (-3.0, 0.0, 0.0), (15.0, 18.0, 20.0, 23.5)),
    # attack_side_hew: the plates and head ride clear of the haft and arms through the windup and the follow-through
    ("attack_side_hew", "head", (0.0, -6.0, -12.0), (8.5, 9.5, 10.5, 11.25)),
    ("attack_side_hew", "left_pauldron", (0.0, 0.0, 12.0), (8.5, 9.5, 10.5, 11.25)),
    ("attack_side_hew", "right_pauldron", (-6.0, 0.0, 16.0), (10.5, 11.5, 13.0, 14.0)),
    ("attack_side_hew", "head", (10.0, 0.0, 16.0), (10.5, 11.5, 13.0, 14.0)),
    ("attack_side_hew", "left_pauldron", (8.0, 0.0, 0.0), (14.5, 16.0, 18.0, 20.0)),
)


def _r2_smooth(x):
    x = min(1.0, max(0.0, x))
    return x * x * (3.0 - 2.0 * x)


def _r2_keys(doc, clip, bone):
    """The bone's rotation keys in a clip (keyed at every sample, all zero, when the clip did not key it)."""
    a = doc["animations"][clip]
    m = a["bones"].setdefault(bone, {}).setdefault("rotation", {})
    if not m:
        n = int(round(a["animation_length"] * 20 * SUB))
        m.update({"%g" % (i / (20.0 * SUB)): [0.0, 0.0, 0.0] for i in range(n + 1)})
    return m


def _r2_map(m, fn):
    for t in list(m):
        v = m[t]["post"] if isinstance(m[t], dict) else m[t]
        nv = [round(float(x), 3) for x in fn(float(t) * 20.0, v)]
        if isinstance(m[t], dict):
            m[t] = dict(m[t], post=nv) if "pre" not in m[t] else {"pre": nv, "post": nv}
        else:
            m[t] = nv


def _r2_offset(doc, clip, bone, off, win):
    a, b, c, d = win
    def f(tk, v):
        w = _r2_smooth((tk - a) / max(b - a, 1e-6)) * _r2_smooth((d - tk) / max(d - c, 1e-6))
        return [x + w * o for x, o in zip(v, off)]
    _r2_map(_r2_keys(doc, clip, bone), f)


def _r2_scale(doc, clip, bone, k):
    m = _r2_keys(doc, clip, bone)
    v0 = m[min(m, key=float)]
    r0 = list(v0["post"] if isinstance(v0, dict) else v0)
    _r2_map(m, lambda tk, v: [x0 + k * (x - x0) for x, x0 in zip(v, r0)])


def _r2_run_skirts(doc):
    B = doc["animations"]["run_legs"]["bones"]
    for side in ("left", "right"):
        leg = B[side + "_leg"]["rotation"]
        B[side + "_skirt"] = {"rotation": {t: [round(-R2_SKIRT_K * v[0], 3), 0.0, 0.0] for t, v in leg.items()}}


def _r2_charge_hand(doc):
    """charge_run_upper: right_hand turned by R2_CHARGE_HAND, the left fist solved back onto the haft at its slide."""
    A = doc["animations"]
    up, t = _unbake(A["charge_run_upper"])
    run, _ = _unbake(A["run_legs"])
    dx, dy = R2_CHARGE_HAND
    for i in range(len(up)):
        lr = _at(run, i)
        s = dict(up[i])
        h = s["right_hand"]
        sl = _slide(SP.fk_pose(s, lr))
        s["right_hand"] = ((h[0][0] + dx, h[0][1] + dy, h[0][2]), h[1])
        p = SP.fk_pose(s, lr)
        a0 = np.r_[np.asarray(s["left_arm"][0], float), s["left_forearm"][0][0]]

        def f(x, p=p, sl=sl, a0=a0):
            qq = SP._arm("left", p, x)
            return np.concatenate([4.0 * (K.point(K.world(qq, "left_forearm"), SP.FIST_L) - SP.wpoint(qq, SP.S(sl))),
                                   0.05 * (x - a0)])
        x = SP.gn(f, a0, iters=30)
        s["left_arm"] = (tuple(float(v) for v in x[:3]), s["left_arm"][1])
        s["left_forearm"] = ((float(x[3]), s["left_forearm"][0][1], s["left_forearm"][0][2]), s["left_forearm"][1])
        up[i] = s
    A["charge_run_upper"] = _rebake(up, t, A["charge_run_upper"]["loop"])


def _r2_foot_raise(doc, keep=0.8, over=0.92):
    """stun_legs: where a boot (or knee wing) dips more than `over` px under the ground (the check fails at 1), its ankle is
    lifted until it dips `keep` px, eased over a tick; the leg is solved again to the raised ankle (as high as it still
    reaches) and the foot keeps its world turn. A full lift to the ground bent the knee to 100 deg (scorecard knee_bend
    limit 93.9) and opened the loop seam (loop_pose 29.7): the stun's legs hang nearly straight."""
    A = doc["animations"]
    upper, _ = _unbake(A["stun_upper"])
    legs, tl = _unbake(A["stun_legs"])
    n = len(legs)
    z3 = (0.0, 0.0, 0.0)

    def depth(i, side):
        pose = SP.fk_pose(_at(upper, i), legs[i])
        return floor_depth(pose, tuple(b for b in FLOOR_PARTS if b.startswith(side)))[0]
    for _ in range(3):
        moved = False
        for side in ("right", "left"):
            d = [depth(i, side) for i in range(n)]
            need = [max(0.0, x - keep) if x > over else 0.0 for x in d]
            if not any(need):
                continue
            moved = True
            for i, dh in enumerate(_smooth_series(need, SUB)):
                if dh <= 0:
                    continue
                pose = SP.fk_pose(_at(upper, i), legs[i])
                Mf = K.world(pose, side + "_foot")
                tgt = K.point(Mf, K.ankle_px(side)) + np.array([0.0, dh, 0.0])
                g = legs[i]
                guess = (g[side + "_leg"][0][0], g[side + "_leg"][0][2], g[side + "_shin"][0][0])
                got, lo, hi = None, 0.0, dh
                try:
                    got = K.leg_pose(pose, side, tgt, 0.0, guess=guess)
                except ValueError:      # out of reach: raise as far as the leg still reaches
                    for _ in range(6):
                        mid = 0.5 * (lo + hi)
                        try:
                            got = K.leg_pose(pose, side, tgt - np.array([0.0, dh - mid, 0.0]), 0.0, guess=guess)
                            lo = mid
                        except ValueError:
                            hi = mid
                if got is None:
                    continue
                lr, sr, _ = got
                q = dict(pose)
                q[side + "_leg"], q[side + "_shin"] = (lr, z3), (sr, z3)
                fr = K.decompose_zyx(K.world(q, side + "_shin")[:3, :3].T @ Mf[:3, :3])
                new = dict(g)
                new[side + "_leg"], new[side + "_shin"], new[side + "_foot"] = (lr, None), (sr, None), (fr, None)
                if side + "_boot" in new:
                    new[side + "_boot"] = (fr, new[side + "_boot"][1])
                legs[i] = new
        if not moved:
            break
    A["stun_legs"] = _rebake(legs, tl, A["stun_legs"]["loop"])


def round2_fixes(doc):
    """The fix round 2 steps on the finished clip file (write_layers)."""
    _r2_run_skirts(doc)
    _r2_charge_hand(doc)
    for clip, bone, off, win in R2_OFFSETS:
        _r2_offset(doc, clip, bone, off, win)
    _r2_scale(doc, "hit_knockback", "tabard", R2_TABARD_K)
    _r2_foot_raise(doc)


def write_layers():
    """Adds the layered clips to this build's clip file, in the exported file's clip order, and keeps the exported file's
    death clips (the approved ones: heldprop.py's data-level polish of 2026-11-07, which no builder reproduces for this
    non-D3 troop; see docs/anim-rebuild/POLISH-DEATHS.md). Runs only on a fresh SP.main output, so the one recipe
    `python art/anim/greataxe/build_greataxe_anims.py && python art/anim/common/export.py ic_breaker` is idempotent."""
    doc = json.loads(K.OUT.read_text(encoding="utf-8"))
    done = [n for n in LAYERED_NAMES if n in doc["animations"]]
    if done:
        raise SystemExit("write_layers: %s already holds layered clips (%s); run the whole builder" % (K.OUT, done[0]))
    clips = layered_clips(K.OUT)
    shipped = json.loads(ASSET.read_text(encoding="utf-8"))["animations"] if ASSET.is_file() else {}
    order = list(shipped)
    anims = dict(doc["animations"])
    anims.update(clips)
    ordered = {k: anims[k] for k in order if k in anims}
    ordered.update({k: v for k, v in anims.items() if k not in ordered})
    doc["animations"] = ordered
    import hang_swing   # noqa: E402  (as stances.merge does; no hanging part on this troop)
    hang_swing.key_doc(SP.TROOP, doc, log=False)
    if FIX2:
        round2_fixes(doc)
    for n in doc["animations"]:
        if n.startswith("death_") and n in shipped:
            doc["animations"][n] = shipped[n]
    with open(K.OUT, "w", newline="\n") as fh:
        fh.write(json.dumps(doc, indent=1))
    print("layered clips:", ", ".join(clips), "->", K.OUT)


# ----------------------------------------------------------------------------------- the solve cache (fix round 2)
# A full build solved every sample of every clip again (about two hours). The slow, pure steps go through solve_cache.memo:
# a step called with the same inputs, read by the same code, as in an earlier build returns that build's result
# (work/anim-cache/<troop>/, gitignored; GG_BREAKER_CACHE=0 builds without it). It changes no output: see solve_cache.py.
import solve_cache   # noqa: E402

if REVIEW:
    def _memo(fn, step):
        return solve_cache.memo(fn, SP.TROOP, step)
    for _n in ("attack_solve", "right_clear", "clear_helm", "depop_left_arm", "left_chest_final", "soften", "soft_additive",
               "haft_offsets", "hips_lift", "legs_raise", "head_clear_loop", "soft_loop", "soft_offsets", "_hit_clips"):
        globals()[_n] = _memo(globals()[_n], _n)
    for _n in ("idle_clips", "stance_clips", "death_clips"):
        setattr(SP, _n, _memo(getattr(SP, _n), "SP." + _n))
    for _m, _tag in ((LS, "LS"), (LC, "LC"), (LR, "LR")):
        _m.build_all = _memo(_m.build_all, _tag + ".build_all")


if __name__ == "__main__":
    if os.environ.get("GREATAXE_PROBE"):
        probe()
    else:
        SP.main()
        if BREAKER and os.environ.get("GG_BREAKER_LAYERS") != "0":
            write_layers()
        if REVIEW and solve_cache.ENABLED:
            print("solve cache: %(hit)d steps reused, %(miss)d solved" % solve_cache.STATS)
