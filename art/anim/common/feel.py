"""The v2 feel scales, in ONE place, for every biped builder (knight, spear, axe, greataxe, archer, crossbow, thrower, caster).

Kort, 2026-11-07, after the sway trial: "way too much sway", "heavy hits break their back", and the ready stance "has too much
sway, bring it down to like 25%". He picked trial variant B. 1.0 for any scale rebuilds the previous v2 motion exactly.

  V2_SWAY_SCALE        0.4   the idle and walk/run sway, and the non-ready stances' weight rock (torso/head/pelvis/arm trail,
                             shield lag, pelvis roll, hip shift, shoulder roll). Breathing, gait yaw and arm swing are not sway.
  V2_HIT_BACK_SCALE    0.35  how far a blow throws the spine, head and pelvis BACKWARD (negative pitch on the hit clips).
                             Forward pitch, twist, the stagger step and the knee give are unchanged.
  V2_READY_SWAY_SCALE  0.25  EVERYTHING the ready stance and the combat ready idle do over their loop: breath, weight rock,
                             fore-aft lean, yaw drift, shield lag, head travel, the weapon's drift. (Not the 0.4 above.)

Env overrides for trials: GG_V2_SWAY, GG_V2_HIT_BACK, GG_V2_READY_SWAY (all ignored under GG_V2=0, which builds the v1 style).

The scales are read through the accessors sway() / hit_back() / ready_sway() AT CALL TIME, so `with unscaled():` builds the
approved v2 motion exactly (the death falls start from that idle pose, which is not part of this polish).
"""
import contextlib
import os

V2 = os.environ.get("GG_V2") != "0"


def _env(name, default):
    return float(os.environ.get(name, default)) if V2 else 1.0


V2_SWAY_SCALE = _env("GG_V2_SWAY", 0.4)
V2_HIT_BACK_SCALE = _env("GG_V2_HIT_BACK", 0.35)
V2_READY_SWAY_SCALE = _env("GG_V2_READY_SWAY", 0.25)

_unscaled = False


def sway():
    return 1.0 if _unscaled else V2_SWAY_SCALE


def hit_back():
    return 1.0 if _unscaled else V2_HIT_BACK_SCALE


def ready_sway():
    return 1.0 if _unscaled else V2_READY_SWAY_SCALE


@contextlib.contextmanager
def unscaled():
    """All three scales read 1.0 inside: the previous v2 motion, byte for byte."""
    global _unscaled
    prev, _unscaled = _unscaled, True
    try:
        yield
    finally:
        _unscaled = prev


def hb(x):
    """A pitch (degrees, - is back) with its backward part scaled by V2_HIT_BACK_SCALE."""
    return x * hit_back() if x < 0 else x


HEAVY_BACK_MAX_DEG = 17.5   # the most a heavy front blow may throw the torso bone back (offset), HitController's x1.55 included
MEDIUM_PEAK_GAIN = 2.3      # the v2 medium clip reaches 2.3x its HIT_PEAK
HEAVY_GAIN = 1.55           # HitController: heavy = medium x 1.55


def hb_front(x):
    """hb() for the FRONT blow's torso pitch, also capped so the heavy blow (peak x 2.3 x 1.55) stays within HEAVY_BACK_MAX_DEG."""
    v = hb(x)
    return v if _unscaled else max(v, -HEAVY_BACK_MAX_DEG / (MEDIUM_PEAK_GAIN * HEAVY_GAIN))


def stance_amps(life, ready):
    """(breath, weight rock, lag) amplitudes of a stance loop whose life is `life` (the old br = life, wt = life - 1, lag = life).
    The ready stance scales ALL three by V2_READY_SWAY_SCALE; any other stance keeps its breath and scales the excess by
    V2_SWAY_SCALE."""
    if ready:
        r = ready_sway()
        return r * life, r * (life - 1.0), r * life
    s = sway()
    return life, s * (life - 1.0), s * life
