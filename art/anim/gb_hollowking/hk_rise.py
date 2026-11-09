"""The Hollow King's Undying King clips (anim spec C12, tier III rise): fake_death and rise.

Built by art/anim/knight/build_anims.py (GG_ANIM_TROOP=gb_hollowking) after its deaths are configured, from the same v4 death
system (art/anim/common/deaths.py):

  fake_death  30 ticks, holds its last frame: the forward fall (death_forward: the knees give, then he goes face down) with
              the grave sword kept gripped (never dropped, unlike the _drop deaths) and kept off the floor, then held 10 ticks
  rise        40 ticks, once: raised from the dead (Kort 2026-11-09: "his body starts to float, his arms and legs droop down and
              then whenever he's back to life his arms and legs snap back in place"). From that face-down pose the body is drawn
              up by the chest as if on strings, swinging upright as it lifts (t0-18), the arms dangling, the legs hanging with the
              toes down, the head lolling and the sword hanging loose in the hand; it hovers there, the limbs swaying (t18-27);
              then the limbs whip back into the guard in about three ticks, past it and back (t27-33), the head snapping up; he
              drops the last bit (t32-35.5) and lands into the guard, the knees taking it (t35.5-40)

The server keeps him down (stunned and still) for both clips, 70 ticks (Houses.UNDYING_DOWN), so the rise ends as he is free.
The fall is keyed (eased per bone) and grounded on its own corners. The rise's root is solved: the chest follows its path while
he floats, the lowest body corner comes down onto the floor from t32 and stays on it, and no body corner goes below the floor at
any tick. Its drift is walked back to the troop's spot, so the clip ends where the guard starts. d3_clear keeps the gripped sword
off the floor and out of the body (clear_rise: the float and the snap apart, easing back onto the stance's guard at the end); it
sags from the slack wrist about 35 degrees below level while he floats (its grip cannot hang plumb through the vambrace).
"""
from __future__ import annotations

import numpy as np

import deaths as D

HOLD = 10
RISE_TICKS = 40
Z3 = (0.0, 0.0, 0.0)

# The resurrection's beats (ticks of the rise clip).
LIFT = 18.0            # off the ground and upright, hanging from the chest
SNAP = 27.0            # the hover ends: the limbs whip back
SNAP_OVER = 29.5       # their overshoot past the guard
SNAP_UNDER = 31.0      # the small swing back
SNAP_SET = 33.0        # on the guard
DROP = 32.0            # the last bit to the ground starts (still floating through the snap)
TOUCH = 35.5           # the soles meet the floor
ABSORB = 37.0          # the knees take the landing
SETTLE = 40.0          # the guard (every death bone) is the last sample
FLOAT_PX = 13.0        # how high the hover holds the chest over its guard height (px): the soles hang about this far off the floor
CHEST = (0.0, 24.0, 0.0)   # the strings' point on the torso, model px (between the torso's pivot, 17, and the neck, 28)
OVER, UNDER = 0.3, 0.1     # the snap's overshoot past the guard and its swing back, as shares of the droop it leaves

# The limp hang (absolute animation degrees), upright and floating. Signs as the falls' (deaths.D3_FORWARD): root x+ pitches the
# body forward, torso x- arches the chest back, head x+ nods down, arm x- raises it forward (x+ back), z+ out for the right arm,
# forearm x- bends the elbow, shin x+ bends the knee, foot x+ points the toe down.
DROOP = {"root": (-5, 0, 0), "hips": Z3, "torso": (-12, 0, 0), "head": (38, 8, 12), "cape": (8, 0, 0),
         "right_arm": (10, 0, 24), "right_forearm": (-12, 0, 0), "right_hand": (180, 0, -90),
         "left_arm": (10, 0, -8), "left_forearm": (-10, 0, 0), "left_hand": (28, 0, 0),
         "right_leg": (4, 0, 3), "right_shin": (14, 0, 0), "right_foot": (45, 0, 0),
         "left_leg": (-3, 0, -3), "left_shin": (18, 0, 0), "left_foot": (50, 0, 0)}

LIFT_KEYS = [
    # the chest is drawn up first: the body peels off the floor still pitched forward, the arms and legs trailing under it
    (8, "in", {"root": (52, -6, 0), "torso": (-4, 0, 0), "head": (46, 16, 6), "cape": (20, 0, 0),
               "right_arm": (-30, 0, 12), "right_forearm": (-8, 0, 0), "right_hand": (0, 0, 30),
               "left_arm": (-28, 0, -12), "left_forearm": (-6, 0, 0), "left_hand": (24, 0, 0),
               "right_leg": (-20, 0, 4), "right_shin": (22, 0, 0), "right_foot": (40, 0, 0),
               "left_leg": (-14, 0, -4), "left_shin": (26, 0, 0), "left_foot": (44, 0, 0)}),
    # swinging upright under the strings, the limbs dangling and lagging
    (14, "io", {"root": (12, -2, 0), "torso": (-10, 0, 0), "head": (44, 12, 10), "cape": (14, 0, 0),
                "right_arm": (-4, 0, 18), "right_forearm": (-10, 0, 0), "right_hand": (120, 0, -45),
                "left_arm": (-2, 0, -10), "left_forearm": (-8, 0, 0), "left_hand": (28, 0, 0),
                "right_leg": (-6, 0, 3), "right_shin": (16, 0, 0), "right_foot": (44, 0, 0),
                "left_leg": (-8, 0, -3), "left_shin": (20, 0, 0), "left_foot": (48, 0, 0)}),
    (LIFT, "io", DROOP),
]

# the hover: the limp limbs and the lolling head sway on their own, a little behind one another
HOVER_KEYS = [
    (21.0, "io", {"head": (33, 3, 15), "right_arm": (13, 0, 27), "left_arm": (7, 0, -5), "right_leg": (7, 0, 3), "left_leg": (-6, 0, -3),
                  "right_hand": (183, 0, -90), "cape": (4, 0, 0)}),
    (24.0, "io", {"head": (41, 11, 8), "right_arm": (8, 0, 22), "left_arm": (12, 0, -10), "right_leg": (2, 0, 3), "left_leg": (0, 0, -3),
                  "right_hand": (177, 0, -90), "cape": (10, 0, 0)}),
    (SNAP, "io", DROOP),
]


def _guard(ready):
    return {b: tuple(map(float, ready[b][0])) if b in ready else Z3 for b in D.DEATH_BONES}


def _toward(g, d, share):
    """g pushed past itself, away from d, by share of (g - d): the snap's overshoot (share > 0) or its swing back (share < 0)."""
    return {b: tuple(g[b][c] + share * (g[b][c] - d[b][c]) for c in range(3)) for b in g}


def rise_keys(ready):
    """Every key of the rise after its first pose: the lift, the hover, the snap and the landing's give."""
    G = _guard(ready)
    D_ = {b: tuple(map(float, DROOP.get(b, G[b]))) for b in D.DEATH_BONES}
    absorb = dict(G)
    for b, add in {"right_leg": (-9, 0, 0), "left_leg": (-7, 0, 0), "right_shin": (16, 0, 0), "left_shin": (14, 0, 0),
                   "right_foot": (-7, 0, 0), "left_foot": (-7, 0, 0), "torso": (6, 0, 0), "head": (-5, 0, 0),
                   "right_arm": (-5, 0, 0), "left_arm": (-4, 0, 0)}.items():
        absorb[b] = tuple(G[b][c] + add[c] for c in range(3))
    return (list(LIFT_KEYS) + list(HOVER_KEYS)
            + [(SNAP_OVER, "out", _toward(G, D_, OVER)), (SNAP_UNDER, "io", _toward(G, D_, -UNDER)), (SNAP_SET, "io", G),
               (TOUCH, "io", G), (ABSORB, "out", absorb), (SETTLE, "io", G)])


def _pt(M, bone, p):
    """World point (px) of model point p (px) carried by bone."""
    v = M[bone] @ np.array([-p[0] / 16.0, p[1] / 16.0, p[2] / 16.0, 1.0])
    return v[:3] * 16.0


def rise_samples(last, ready, ground, geo):
    """From the fall's last sample (face down) to the guard: lifted by the chest, hung limp, snapped back and landed.

    The root's position is solved, not keyed: while floating, so the chest follows its path (up off the floor, over the troop's
    spot, holding FLOAT_PX over its guard height with a slow bob); from DROP, so the lowest body corner comes down onto the
    floor by TOUCH and stays on it; never below the floor (the body's own corners). The horizontal is walked back to the
    troop's spot, so the clip ends where the guard starts."""
    start = {b: (last[b][0], last[b][1]) for b in D.DEATH_BONES}
    frames = D._d3_tracks(rise_keys(ready), start)
    hips0 = np.array(last["hips"][1] if last["hips"][1] is not None else Z3, float)
    hips1 = np.array(ready["hips"][1] if ready.get("hips") and ready["hips"][1] is not None else Z3, float)
    r0 = np.array(last["root"][1] if last["root"][1] is not None else Z3, float)
    body = geo.cubes["body"]

    def pose_at(rot, hips):
        p = {b: (rot[b], None) for b in D.DEATH_BONES}
        p["hips"] = (rot["hips"], tuple(hips))
        return p

    # the chest where the fall left it, and where the guard holds it (root at the origin)
    M0 = geo.fk(dict(pose_at({b: last[b][0] for b in D.DEATH_BONES}, hips0), root=(last["root"][0], tuple(r0))))
    c_start = _pt(M0, "torso", CHEST)
    G = _guard(ready)
    Mg = geo.fk(dict(pose_at(G, hips1), root=(G["root"], Z3)))
    c_guard = _pt(Mg, "torso", CHEST)
    top = c_guard[1] + FLOAT_PX
    floor = min(ground, geo.corners(Mg, body)[:, 1].min())   # the landing: onto the guard's own lowest corner, so the last sample meets the guard

    samples, prot, drop_h, drop_xz = [], None, None, None
    n = RISE_TICKS * D.SUB
    for i in range(n + 1):
        t = i / D.SUB
        rot = D._d3_rot(frames, t, prot)
        prot = rot
        w = D._ease("io", min(1.0, t / LIFT))
        pose = pose_at(rot, hips0 + (hips1 - hips0) * w)
        pose["root"] = (rot["root"], Z3)
        M = geo.fk(pose)
        c0 = _pt(M, "torso", CHEST)                       # the chest with the root's position at zero
        low0 = geo.corners(M, body)[:, 1].min()           # the lowest body corner, likewise
        if t <= DROP:
            s = D._ease("io", min(1.0, t / LIFT))
            want = c_start + (np.array([c_guard[0], top, c_guard[2]]) - c_start) * s
            if t > LIFT:   # the hover's slow bob, and a jolt up as the limbs whip in
                want[1] += 0.6 * np.sin(2 * np.pi * (t - LIFT) / 9.0) * min(1.0, (t - LIFT) / 3.0)
                want[1] += 0.8 * np.exp(-((t - SNAP_OVER) / 1.2) ** 2)
            off = want - c0
            y = max(off[1], ground - low0)                # never into the floor
            pos = np.array([off[0], y, off[2]])
            if t == DROP or drop_h is None and t >= DROP:
                drop_h, drop_xz = low0 + y - floor, pos[[0, 2]].copy()
        else:
            h = drop_h * (1.0 - D._ease("in", min(1.0, (t - DROP) / (TOUCH - DROP))))   # falls the last bit, gathering speed
            back = D._ease("io", min(1.0, (t - DROP) / (SETTLE - 2.0 - DROP)))
            xz = drop_xz * (1.0 - back)
            pos = np.array([xz[0], floor + h - low0, xz[1]])
        # world (x, y, z) px -> the root's animation position (x, y, -z) (deaths._bone_mat under WORLD_FLIP)
        pose["root"] = (rot["root"], (float(pos[0]), float(pos[1]), float(-pos[2])))
        samples.append(pose)
    samples[0] = {b: (last[b][0], last[b][1]) for b in D.DEATH_BONES}
    samples[-1] = {b: (tuple(map(float, ready[b][0])), ready[b][1]) if b in ready else (Z3, None) for b in D.DEATH_BONES}
    return samples


def fall(start):
    """The forward fall with the sword kept: samples, ground, geometry (as deaths.d3_death_clips' death_forward)."""
    geo = D.D3Geo(D.HEAVY)
    sm, ground = D.d3_body(D.d3_keys("forward"), start, geo)
    for part in D.GEAR:
        D.d3_clear(sm, geo, part, ground, None)
        D.d3_clear(sm, geo, part, ground, None)
    return sm, ground, geo


def clear_rise(rs, geo, ground):
    """The held gear kept off the floor and out of the body (deaths.d3_clear), the float and the snap apart: the sword hangs
    loose till SNAP, then whips straight into the guard, never held back by the turn the hang needed. The snap is solved from
    the guard back (its samples reversed); from TOUCH its turns ease out, so the last sample is the stance's guard exactly."""
    cut, i0, n = int(SNAP * D.SUB), int(TOUCH * D.SUB), len(rs) - 1
    for part in D.GEAR:
        bones = (D.GEAR[part]["arm"], D.GEAR[part]["hand"])
        guard = [{b: rs[i][b] for b in bones} for i in range(i0, n + 1)]
        D.d3_clear(rs[:cut + 1], geo, part, ground, None)
        D.d3_clear(rs[cut:][::-1], geo, part, ground, None)
        for i in range(i0, n + 1):
            u = D._ease("io", (i - i0) / float(n - i0))
            for b in bones:
                (r1, pos), r0 = rs[i][b], guard[i - i0][b][0]
                rs[i][b] = (tuple(a + ((g - a + 180.0) % 360.0 - 180.0) * u for a, g in zip(r1, r0)), pos)


def clips(start, ready, bake):
    """{"fake_death": clip, "rise": clip}. start: idle frame 0 per death bone; ready: the guard (stance_ready frame 0)."""
    sm, ground, geo = fall(start)
    fake = sm + [sm[-1]] * (HOLD * D.SUB)
    c = bake(fake, D.DEATH_TICKS + HOLD, loop=False)
    c["loop"] = "hold_on_last_frame"
    rs = rise_samples(sm[-1], ready, ground, geo)
    clear_rise(rs, geo, ground)
    r = bake(rs, RISE_TICKS, loop=False)
    return {"fake_death": c, "rise": r}
