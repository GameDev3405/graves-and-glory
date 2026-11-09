"""The Hollow King's Undying King clips (anim spec C12, tier III rise): fake_death and rise.

Built by art/anim/knight/build_anims.py (GG_ANIM_TROOP=gb_hollowking) after its deaths are configured, from the same v4 death
system (art/anim/common/deaths.py):

  fake_death  30 ticks, holds its last frame: the forward fall (death_forward: the knees give, then he goes face down) with
              the grave sword kept gripped (never dropped, unlike the _drop deaths) and kept off the floor, then held 10 ticks
  rise        40 ticks, once: from that face-down pose the hands press into the ground (t6), he pushes the chest up (t13),
              draws a leg under him onto one knee (t20), regrips and lifts the sword (t27), stands to full height with the
              chest out and the head tilting up (t34), and settles onto the exact guard (stance_ready, t40)

Like the falls, the body is keyed (eased per bone) and grounded on its own corners: the lowest body corner stays on the
ground and the body rolls about it without sliding. The fall's ground drift is walked back to the troop's spot over the
stand, so the clip ends where the guard starts. d3_clear keeps the gripped sword off the floor and out of the body.
"""
from __future__ import annotations

import numpy as np

import deaths as D

HOLD = 10
RISE_TICKS = 40
Z3 = (0.0, 0.0, 0.0)

# (tick, ease into it, {bone: animation degrees}). Signs as the falls' (deaths.D3_FORWARD): root x+ pitches the whole body
# forward about the feet (84 face down), arm x- raises the hand forward, shin x+ bends the knee, head x- tips the face up.
RISE_KEYS = [
    # the hands come under the shoulders and press into the ground; the head lifts off it
    (6, "io", {"right_arm": (-78, 0, 18), "right_forearm": (-40, 0, 0), "right_hand": (-30, 0, -50),
               "left_arm": (-78, 0, -16), "left_forearm": (-40, 0, 0), "left_hand": (20, 0, 0),
               "head": (-22, 10, 0), "torso": (4, 0, 0)}),
    # the arms straighten and push the chest up off the ground, the knees start to draw in
    (13, "io", {"root": (52, -6, 0), "torso": (12, 0, 0), "head": (-30, 4, 0),
                "right_arm": (-62, 0, 16), "right_forearm": (-14, 0, 0), "left_arm": (-62, 0, -14), "left_forearm": (-12, 0, 0),
                "right_leg": (-30, 0, 4), "left_leg": (-14, 0, -4), "right_shin": (60, 0, 0), "left_shin": (48, 0, 0),
                "right_foot": (20, 0, 0), "left_foot": (40, 0, 0)}),
    # onto one knee: the right foot planted forward, the left knee down, the body over the knee, the free hand on the thigh
    (20, "io", {"root": (6, -4, 0), "hips": Z3, "torso": (24, 0, 0), "head": (-6, 0, 0),
                "right_leg": (-80, 0, 6), "right_shin": (84, 0, 0), "right_foot": (2, 0, 0),
                "left_leg": (-4, 0, -4), "left_shin": (100, 0, 0), "left_foot": (40, 0, 0),
                "right_arm": (-26, 0, 26), "right_forearm": (-24, 0, 0), "right_hand": (-40, 0, -40),
                "left_arm": (-46, 0, -12), "left_forearm": (-48, 0, 0), "left_hand": (10, 0, 0)}),
    # the regrip: the sword drawn up off the ground to the side, the head coming up
    (27, "io", {"torso": (14, 0, 0), "head": (-14, 0, 0),
                "right_arm": (-58, 0, 24), "right_forearm": (-54, 0, 0), "right_hand": (-46, 0, -6),
                "left_arm": (-36, 0, -10), "left_forearm": (-40, 0, 0)}),
    # stands to full height: chest out, the head tilted up, menacing
    (34, "in", {"root": Z3, "torso": (-10, 0, 0), "head": (-18, 0, 0),
                "right_leg": (-8, 0, 4), "right_shin": (8, 0, 0), "right_foot": Z3,
                "left_leg": (6, 0, -4), "left_shin": (6, 0, 0), "left_foot": Z3,
                "right_arm": (-52, 0, 22), "right_forearm": (-48, 0, 0)}),
]
SETTLE = 40.0          # the guard pose (every death bone) is the last key


def _guard(ready):
    return {b: tuple(map(float, ready[b][0])) for b in D.DEATH_BONES if b in ready}


def fall(start):
    """The forward fall with the sword kept: samples, ground, geometry (as deaths.d3_death_clips' death_forward)."""
    geo = D.D3Geo(D.HEAVY)
    sm, ground = D.d3_body(D.d3_keys("forward"), start, geo)
    for part in D.GEAR:
        D.d3_clear(sm, geo, part, ground, None)
        D.d3_clear(sm, geo, part, ground, None)
    return sm, ground, geo


def rise_samples(last, ready, ground, geo):
    """From the fall's last sample to the guard, grounded on the body's own corners."""
    start = {b: (last[b][0], last[b][1]) for b in D.DEATH_BONES}
    keys = list(RISE_KEYS) + [(SETTLE, "io", _guard(ready))]
    frames = D._d3_tracks(keys, start)
    hips0 = np.array(last["hips"][1] if last["hips"][1] is not None else Z3, float)
    hips1 = np.array(ready["hips"][1] if ready.get("hips") and ready["hips"][1] is not None else Z3, float)
    r0 = np.array(last["root"][1] if last["root"][1] is not None else Z3, float)
    body = geo.cubes["body"]
    samples, prev, prot, shift = [], None, None, np.array([r0[0], 0.0, -r0[2]])
    n = RISE_TICKS * D.SUB
    for i in range(n + 1):
        t = i / D.SUB
        rot = D._d3_rot(frames, t, prot)
        prot = rot
        w = D._ease("io", min(1.0, max(0.0, (t - 6.0) / (SETTLE - 8.0))))
        pose = {b: (rot[b], None) for b in D.DEATH_BONES}
        pose["hips"] = (rot["hips"], tuple(hips0 + (hips1 - hips0) * w))
        P = geo.corners(geo.fk(pose), body)
        low = int(np.argmin(P[:, 1]))
        if prev is not None:
            d = prev[low] - P[low]
            shift += (d[0], 0.0, d[2])
        prev = P
        # walk the drift back to the troop's spot over the stand (t20..t38): the guard starts at the root's origin
        back = D._ease("io", min(1.0, max(0.0, (t - 20.0) / 18.0)))
        y = ground - P[low, 1]
        if t >= 34.0:   # standing: the soles on the floor, the root eased onto the guard's
            y *= 1.0 - D._ease("io", min(1.0, (t - 34.0) / 6.0))
        pose["root"] = (rot["root"], (shift[0] * (1 - back), y, -shift[2] * (1 - back)))
        samples.append(pose)
    samples[-1] = {b: (tuple(map(float, ready[b][0])), ready[b][1]) if b in ready else (Z3, None) for b in D.DEATH_BONES}
    return samples


def clips(start, ready, bake):
    """{"fake_death": clip, "rise": clip}. start: idle frame 0 per death bone; ready: the guard (stance_ready frame 0)."""
    sm, ground, geo = fall(start)
    fake = sm + [sm[-1]] * (HOLD * D.SUB)
    c = bake(fake, D.DEATH_TICKS + HOLD, loop=False)
    c["loop"] = "hold_on_last_frame"
    rs = rise_samples(sm[-1], ready, ground, geo)
    for part in D.GEAR:
        D.d3_clear(rs, geo, part, ground, None)
    r = bake(rs, RISE_TICKS, loop=False)
    return {"fake_death": c, "rise": r}
