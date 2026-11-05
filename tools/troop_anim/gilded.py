"""Group 1, the Gilded Order: disciplined, drilled, proud. One builder per troop returns its clips (kit.Clip); the IDs in the
comments are the approved list's (KN-1 ...)."""
import numpy as np

from troop_anim.kit import Kit, Clip, P, add, mix, v

L, CR, BZ = 'linear', 'catmullrom', 'bezier'


def arm(k, pose, side, hand, aim=None, flat=None, hint=None, **kw):
    """The pose with an arm solved: hand at (right, up, fwd) px from that shoulder's pivot, the weapon along aim (right, up, fwd)."""
    sh = k.at(pose, side + '_arm')
    return k.reach(pose, side, pos=sh + v(*hand), aim=None if aim is None else v(*aim), flat=None if flat is None else v(*flat), hint=hint, **kw)


def legs(stance=(0, 0), bend=(0, 0), out=(0, 0), toe=(0, 0)):
    """Leg pose: stance (right, left) thigh pitch (negative forward), knee bend, thigh roll out, foot toe-out (yaw)."""
    r, l = stance
    return {'right_leg': [r, toe[0], out[0]], 'right_shin': [bend[0], 0, 0], 'right_foot': [-(r + bend[0]) * 0.5, 0, 0],
            'left_leg': [l, -toe[1], -out[1]], 'left_shin': [bend[1], 0, 0], 'left_foot': [-(l + bend[1]) * 0.5, 0, 0]}


def torso(root=(0, 0, 0), drop=0.0, fwd=0.0, side=0.0, body=(0, 0, 0), chest=(0, 0, 0), head=(0, 0, 0)):
    """root rotation (pitch, yaw, roll) and translation (drop is +y, fwd is -z, side is -x), body, chest and head rotations."""
    return {'root': [*root, -side, drop, -fwd], 'body': list(body), 'chest': list(chest), 'head': list(head)}


def live(clip, amp=1.0, arms=1.0, shift=1.0):
    """A stance loop rebuilt from its first pose with visible life: two breaths (chest and shoulders rise, head settles), a slow
    weight shift onto one hip and back, a settle of the arms. amp scales it (a heavy Templar breathes slower and smaller)."""
    p0 = clip.keys[0][1]
    L = clip.length
    def at(breath, sway):
        return add(p0, chest=[-1.6 * amp * breath, 0, 0], head=[0.8 * amp * breath, 0, 0], body=[0, 0, 1.2 * amp * sway * shift],
                   root=[0, 0, -1.0 * amp * sway * shift, -0.4 * amp * sway * shift, -0.2 * amp * breath + 0.25 * abs(sway) * shift, 0],
                   right_arm=[-2.0 * amp * arms * breath, 0, 1.0 * amp * arms * breath], left_arm=[-1.5 * amp * arms * breath, 0, -1.0 * amp * arms * breath])
    tk = lambda u: round(L * u / 0.05) * 0.05
    keys = [(0, p0, CR), (tk(0.25), at(1, 0.5), CR), (tk(0.5), at(0, 1), CR), (tk(0.75), at(1, 0.5), CR), (L, p0, CR)]
    return Clip(clip.name, L, keys, mode=clip.mode, base=clip.base, loop=True, markers=clip.markers)


# ---------------------------------------------------------------- Order Squire (knight): young, eager, drilled

def fk(arm_r=None, fore_r=None, arm_l=None, fore_l=None):
    """Arm keys straight: (rx, ry, rz) for each arm, a pitch for each forearm."""
    out = {}
    if arm_r is not None:
        out['right_arm'] = list(arm_r)
    if fore_r is not None:
        out['right_forearm'] = [fore_r, 0, 0]
    if arm_l is not None:
        out['left_arm'] = list(arm_l)
    if fore_l is not None:
        out['left_forearm'] = [fore_l, 0, 0]
    return out


def sword(k, pose, aim, flat=None, **kw):
    """The wrist turned so the blade points along aim (right, up, fwd)."""
    return k.point(pose, 'right', v(*aim), None if flat is None else v(*flat), **kw)


# a point-down hold (hands stacked on the pommel, the blade planted): outside a strike the wrist may turn past the ±40 strike bound
PLANT = dict(wrist0=(150.0, 0.0, 0.0))


def knight():
    k = Kit('knight')
    # ---- stances
    # KN-1 idle: textbook guard, sword raised, breathing steady (the engine breathes; the loop keeps the point alive)
    idle = P(torso(drop=0.3, body=(3, -6, 0), chest=(2, -5, 0), head=(-2, 9, 0)), legs((8, -10), (6, 8), (2, -2), (8, 4)),
             fk((-28, -6, 6), -50, (-25, 25, 0), -20))
    idle = sword(k, idle, (0.15, 0.75, 0.64))                   # point up and forward, turned out clear of the face
    idle_b = sword(k, add(idle, right_arm=[-3, 0, 1], body=[1, 0, 0], left_arm=[-2, 0, 0]), (0.15, 0.78, 0.61))
    # guard: tighter and lower, the point on the enemy, the shield up over the chest
    guard = P(torso(drop=0.9, body=(6, -10, 0), chest=(3, -6, 0), head=(-5, 15, 0)), legs((14, -18), (12, 14), (2, -3), (12, 6)),
              fk((-38, -8, 6), -52, (-45, 25, 0), -20))
    guard = sword(k, guard, (-0.12, 0.45, 0.88))
    guard_b = sword(k, add(guard, right_arm=[-3, 0, 0], root=[0, 0, 0, 0, 0.25, 0]), (-0.12, 0.47, 0.87))
    # carry (KN-4): marching with the sword shouldered, blade up and back over the right shoulder, shield at the side
    carry = P(torso(body=(1, 0, 0)), legs(), fk((-8, -14, 8), -96, (-6, 10, 0), -10))
    carry = sword(k, carry, (0.3, 0.38, -0.87))              # sloped back over the shoulder, the hand low at the chest
    stances = [
        Clip('idle', 4.0, [(0, idle, CR), (2.0, idle_b, CR), (4.0, idle, CR)], mode='stance', loop=True),
        Clip('guard', 2.4, [(0, guard, CR), (1.2, guard_b, CR), (2.4, guard, CR)], mode='stance', loop=True),
        Clip('carry', 2.0, [(0, carry, CR), (1.0, add(carry, right_arm=[-1.5, 0, 0]), CR), (2.0, carry, CR)], mode='stance', loop=True),
    ]
    stances = [live(c) for c in stances]

    # ---- KN-7: a lunging thrust that steps in (thrust slot: impact tick 7 = 0.35 s)
    # chamber: elbow drawn back to the hip, right shoulder turned back, weight on the back leg; strike 0.1 s; the lunge carries the reach
    ch = P(guard, torso(drop=1.5, fwd=-1.0, body=(0, 14, 0), chest=(-2, 22, 0), head=(-4, -18, 0)), legs((22, -14), (20, 10), (2, -3), (12, 6)),
           fk((-27, 0, 10), -70, (-52, 25, 0), -22))
    ch = sword(k, ch, (-0.1, 0.3, 0.95))
    ch2 = sword(k, add(ch, right_arm=[-2, 0, 0], root=[0, 0, 0, 0, 0.15, 0]), (-0.1, 0.28, 0.96))
    hit = P(guard, torso(drop=1.9, fwd=3.2, body=(12, -8, 0), chest=(5, -10, 0), head=(-12, 14, 0)), legs((30, -40), (6, 36), (2, -3), (12, 6)),
            fk((-82, 4, 4), -14, (-30, 30, 0), -30))
    hit = sword(k, hit, (-0.05, 0.04, 1.0))
    hold = sword(k, add(hit, root=[0, 0, 0, 0, 0.2, -0.3], right_arm=[2, 0, 0]), (-0.05, 0.02, 1.0))
    back = mix(hold, guard, 0.55)
    back = sword(k, back, (-0.1, 0.35, 0.93))
    lunge = Clip('kn_lunge', 1.3, [(0, guard, L), (0.1, ch, BZ), (0.25, ch2, L), (0.35, hit, L), (0.5, hold, BZ), (0.9, back, BZ), (1.3, guard, L)],
                 markers={'impact': 0.35}, slot='sword_shield_thrust')

    # ---- KN-6: the drill cut, a clean diagonal from high right to low left (overhead slot: impact tick 10 = 0.5 s)
    up = P(guard, torso(drop=0.7, fwd=-0.3, body=(-2, 10, 0), chest=(-4, 14, 0), head=(-8, -10, 0)), legs((16, -16), (10, 12), (2, -3), (12, 6)),
           fk((-128, 18, -14), -96, (-40, 30, 0), -24))
    up = sword(k, up, (0.35, 0.55, -0.76))                      # cocked over the right shoulder, the point up and back
    up2 = sword(k, add(up, right_arm=[-6, 0, 0], chest=[-1, 1, 0]), (0.35, 0.6, -0.72))
    cut = P(guard, torso(drop=1.7, fwd=1.6, body=(12, -16, 0), chest=(6, -16, 0), head=(-10, 18, 0)), legs((22, -28), (6, 26), (2, -3), (12, 6)),
            fk((-74, -22, 4), -40, (-30, 30, 0), -30))
    cut = sword(k, cut, (-0.45, -0.25, 0.86), flat=(0.3, 0.9, 0.1))   # edge leading, down and across to the left
    thru = P(cut, torso(drop=1.9, fwd=1.8, body=(16, -24, 0), chest=(8, -22, 0), head=(-10, 22, 0)), legs((22, -28), (6, 26), (2, -3), (12, 6)),
             fk((-46, -34, 10), -24, (-26, 30, 0), -30))
    thru = sword(k, thru, (-0.6, -0.6, 0.5))
    rec = sword(k, mix(thru, guard, 0.6), (-0.2, 0.3, 0.92))
    drill = Clip('kn_drill_cut', 1.4, [(0, guard, L), (0.2, up, BZ), (0.4, up2, L), (0.5, cut, L), (0.6, thru, BZ), (0.95, rec, BZ), (1.4, guard, L)],
                 markers={'impact': 0.5}, slot='sword_shield_overhead')

    # ---- KN-8: a two-hit combination, a forehand cut then a low backhand (low_cut slot: impact tick 8 = 0.4 s)
    c1 = P(guard, torso(drop=1.0, body=(4, 8, 0), chest=(0, 16, 0), head=(-6, -8, 0)), legs((16, -18), (12, 14), (2, -3), (12, 6)),
           fk((-82, 22, -6), -70, (-42, 25, 0), -20))
    c1 = sword(k, c1, (0.35, 0.75, 0.56))                       # cocked up and out to the right
    c2 = P(guard, torso(drop=1.2, fwd=0.8, body=(8, -14, 0), chest=(4, -18, 0), head=(-6, 16, 0)), legs((18, -22), (10, 18), (2, -3), (12, 6)),
           fk((-74, -30, 4), -46, (-40, 25, 0), -20), {'right_hand': c1['right_hand']})
    c2 = sword(k, c2, (-0.7, 0.3, 0.65))                        # first cut lands across to the left
    c3 = P(c2, fk((-62, -36, 8), -64, (-38, 25, 0), -22))
    c3 = sword(k, c3, (-0.55, 0.05, 0.83))                      # wrist rolled over for the backhand
    b1 = P(guard, torso(drop=2.0, fwd=2.0, body=(16, 14, 0), chest=(6, 16, 0), head=(-12, -12, 0)), legs((26, -32), (8, 30), (2, -3), (12, 6)),
           fk((-44, 18, -10), -30, (-34, 25, 0), -24))
    b1 = sword(k, b1, (0.7, -0.35, 0.62), flat=(0, 1, 0))        # backhand low to the right, at the legs
    b2 = sword(k, P(b1, fk((-34, 30, -16), -24, None, None), torso(drop=2.1, fwd=2.1, body=(18, 20, 0), chest=(7, 22, 0), head=(-12, -14, 0))), (0.85, -0.4, 0.33))
    b3 = sword(k, mix(b2, guard, 0.6), (0.1, 0.35, 0.93))
    combo = Clip('kn_combination', 1.4, [(0, guard, L), (0.1, c1, BZ), (0.2, c2, L), (0.3, c3, L), (0.4, b1, L), (0.5, b2, BZ), (0.95, b3, BZ), (1.4, guard, L)],
                 markers={'impact': 0.4}, slot='sword_shield_low_cut')

    # ---- KN-9: a shoulder barge to break a guard, eager and a little reckless (thrust slot: impact 0.35 s)
    ld = P(guard, torso(drop=2.0, fwd=-1.0, body=(12, -14, 0), chest=(4, -22, 0), head=(-8, 26, 0)), legs((20, -14), (22, 16), (2, -3), (12, 6)),
           fk((-20, 6, 14), -62, (-24, 42, 0), -96))
    ld = sword(k, ld, (0.2, 0.7, 0.68))
    ld2 = add(ld, root=[0, 0, 0, 0, 0.15, 0.1])
    bash = P(guard, torso(drop=1.3, fwd=4.0, body=(18, -20, 0), chest=(6, -24, 0), head=(4, 24, 0)), legs((30, -42), (8, 34), (2, -3), (12, 6)),
             fk((8, 10, 22), -40, (-30, 44, 0), -100))
    bash = sword(k, bash, (0.4, 0.75, 0.5))
    over = sword(k, P(bash, torso(drop=1.9, fwd=4.6, body=(22, -16, 0), chest=(7, -18, 0), head=(-2, 18, 0)), fk((14, 12, 30), -36, None, None)), (0.5, 0.7, 0.4))
    catch = sword(k, mix(over, guard, 0.45), (0.1, 0.5, 0.86))
    barge = Clip('kn_barge', 1.5, [(0, guard, L), (0.15, ld, BZ), (0.25, ld2, L), (0.35, bash, L), (0.55, over, BZ), (1.0, catch, BZ), (1.5, guard, L)],
                 markers={'impact': 0.35}, slot='sword_shield_thrust')

    # ---- KN-2: checks his grip and tugs his gauntlet, then back to guard (an idle variant over idle)
    low = P(idle, torso(drop=0.2, body=(5, -2, 0), chest=(6, 4, 0), head=(26, 14, 0)), fk((-34, -24, 4), -64, (-36, 48, 0), -84))
    low = sword(k, low, (-0.2, -0.75, 0.62))                    # the point lowered, the wrist up where he can see it
    tug = add(low, left_forearm=[8, 0, 0], left_arm=[2, 0, 0], head=[2, 0, 0])
    flex = sword(k, add(low, right_arm=[-4, 0, 2]), (-0.35, -0.7, 0.62))
    flex2 = sword(k, add(low, right_arm=[-2, 0, -2]), (0.0, -0.75, 0.66))
    nod = add(idle, head=[10, 0, 0], chest=[2, 0, 0])
    alt1 = Clip('idle_alt_1', 3.2, [(0, idle, CR), (0.5, low, CR), (0.9, tug, CR), (1.15, low, CR), (1.4, tug, CR), (1.75, flex, CR), (2.05, flex2, CR),
                                     (2.5, nod, CR), (3.2, idle, CR)], base='idle')
    # ---- KN-3: glances back for orders, a quick nod, eyes front again
    look = add(idle, head=[-4, -58, 0], chest=[0, -12, 0], body=[0, -4, 0])
    look_n = add(look, head=[12, 0, 0])
    alt2 = Clip('idle_alt_2', 2.6, [(0, idle, CR), (0.45, look, CR), (1.05, look, CR), (1.3, look_n, CR), (1.5, look, CR), (2.0, add(idle, head=[0, 6, 0]), CR),
                                     (2.6, idle, CR)], base='idle')

    # ---- KN-4 walk: marching in step (rhythm over the stance: the hips dip at each heel strike, the shield arm swings, the
    # shouldered sword stays put, the shoulders counter the hips). Time 0 is the right heel striking.
    z6 = [0.0] * 6
    def wk(dip, sway, chest, shield, head):
        return {'root': [0, 0, 0, 0, dip, 0], 'body': [2, 0, sway], 'chest': [0, chest, 0], 'left_arm': [shield, 0, 0], 'head': [0, -chest * 0.6, 0]}
    walk = Clip('walk', 1.0, [(0, wk(0.55, 1.5, -5, -9, 0), L), (0.15, wk(0.2, 1.0, -3, -5, 0), BZ), (0.3, wk(-0.3, 0, 0, 0, 0), BZ), (0.5, wk(0.55, -1.5, 5, 9, 0), L),
                              (0.65, wk(0.2, -1.0, 3, 5, 0), BZ), (0.8, wk(-0.3, 0, 0, 0, 0), BZ), (1.0, wk(0.55, 1.5, -5, -9, 0), L)],
                mode='raw', base='carry', loop=True, markers={'stride_14': 0})
    # ---- KN-5 run: a charge at the ready (forward lean, shield tucked, the sword arm pumping a little, head up)
    def rn(dip, chest, pump):
        return {'root': [0, 0, 0, 0, dip, 0], 'body': [9, 0, 0], 'chest': [2, chest, 0], 'head': [-8, -chest * 0.5, 0],
                'left_arm': [-14, 0, 0], 'left_forearm': [-12, 0, 0], 'right_arm': [pump, 0, 0]}
    run = Clip('run', 1.0, [(0, rn(1.0, -6, -5), L), (0.1, rn(0.2, -3, -2), BZ), (0.3, rn(-0.4, 0, 2), BZ), (0.5, rn(1.0, 6, -5), L), (0.6, rn(0.2, 3, -2), BZ),
                            (0.8, rn(-0.4, 0, 2), BZ), (1.0, rn(1.0, -6, -5), L)], mode='raw', base='carry', loop=True, markers={'stride_19': 0})

    # ---- KN-10: staggers, recovers into guard fast and shakes it off (body, head and legs)
    def hit(name, jolt, buckle=0.0, snap=0.0):
        a = add(guard, root=[0, 0, 0, 0, 0.4 + buckle, 1.0 * jolt], body=[-10 * jolt, 4, 0], chest=[-4 * jolt, 6, 0], head=[-12 * jolt - snap, -6, 4],
                right_arm=[14 * jolt, 0, 6], left_arm=[10 * jolt, 0, 0], right_shin=[buckle * 10, 0, 0], left_shin=[buckle * 10, 0, 0])
        b = add(guard, root=[0, 0, 0, 0, 0.6 + buckle * 0.6, 0.6 * jolt], body=[-3, 2, 0], head=[-3, -2, 0], right_arm=[4, 0, 2])
        sh1, sh2 = add(guard, head=[2, 12, 0]), add(guard, head=[2, -10, 0])
        return Clip(name, 1.0, [(0, guard, L), (0.1, a, BZ), (0.25, b, CR), (0.4, guard, CR), (0.55, sh1, CR), (0.7, sh2, CR), (0.8, guard, CR), (1.0, guard, L)])
    hits = [hit('hit_body', 1.0), hit('hit_head', 0.8, snap=14), hit('hit_legs', 0.5, buckle=1.4)]

    # ---- KN-11: drops to his knees, the sword planted, slumps forward
    rest = P(torso(), legs(), fk((0, 0, 0), 0, (0, 0, 0), 0))
    reel = P(rest, torso(drop=0.4, fwd=-0.6, body=(-10, 6, 0), head=(-14, 0, 0)), fk((10, 0, 10), -20, (-20, 20, 0), -20), legs((6, -4), (6, 4)))
    kneel = P(torso(drop=6.2, fwd=0.6, body=(14, 0, 0), chest=(6, 0, 0), head=(20, 0, 0)),
              {'right_leg': [-6, 6, 2], 'right_shin': [96, 0, 0], 'right_foot': [-30, 0, 0], 'left_leg': [-4, -6, -2], 'left_shin': [94, 0, 0], 'left_foot': [-30, 0, 0]},
              fk((-42, -10, 4), -34, (-12, 20, 0), -16))
    kneel = sword(k, kneel, (0.05, -0.97, 0.2))                  # the sword planted point down before him
    sag = add(kneel, body=[10, 0, 4], head=[12, 0, 0], chest=[4, 0, 0], right_arm=[10, 0, 0], root=[0, 0, 0, 0, 0.3, 0])
    tip = P(sag, torso(root=(40, 0, 6), drop=7.0, fwd=1.0, body=(10, 0, 4), chest=(4, 0, 0), head=(10, 20, 0)), fk((-30, 0, 20), -20, (-20, 10, -20), -10))
    down = P(torso(root=(86, 0, 4), drop=9.5, fwd=2.0, body=(2, 0, 2), chest=(0, 0, 0), head=(-20, 70, 0)),
             {'right_leg': [-8, 4, 2], 'right_shin': [34, 0, 0], 'right_foot': [-20, 0, 0], 'left_leg': [-4, -4, -2], 'left_shin': [40, 0, 0], 'left_foot': [-20, 0, 0]},
             fk((-150, 0, 34), -20, (-10, 0, -40), -30))
    down = k.reach(down, 'right', aim=v(0.5, 0, 0.87), hand_only=True)     # the sword lies flat beside him, not standing up
    down = k.grounded(down)
    death = Clip('death', 2.4, [(0, rest, L), (0.2, reel, BZ), (0.6, kneel, BZ), (1.1, sag, CR), (1.6, tip, BZ), (2.0, down, BZ), (2.4, down, L)], mode='whole')

    # ---- KN-12: raises his sword overhead in salute (a scene pose; it holds its last key)
    stand = P(torso(), legs((0, 0), (0, 0), (2, -2), (6, 6)), fk((-10, 0, 4), -10, (-20, 20, 0), -16))
    raise_ = P(torso(body=(-4, 0, 0), chest=(-6, 0, 0), head=(-18, 0, 0)), legs((0, 0), (0, 0), (2, -2), (6, 6)), fk((-172, -4, -8), -8, (-22, 22, 0), -20))
    raise_ = sword(k, raise_, (0, 1, 0.08))
    pump = sword(k, add(raise_, right_arm=[8, 0, 0], right_forearm=[-12, 0, 0], root=[0, 0, 0, 0, 0.4, 0]), (0, 1, 0.15))
    victory = Clip('victory', 2.4, [(0, stand, L), (0.35, add(stand, root=[0, 0, 0, 0, 0.6, 0]), BZ), (0.8, raise_, BZ), (1.1, pump, BZ), (1.35, raise_, BZ),
                                    (1.6, pump, BZ), (1.85, raise_, BZ), (2.4, raise_, L)], mode='whole')

    for nm, p in (('guard', guard), ('lunge chamber', ch), ('lunge impact', hit_), ('drill up', up), ('drill cut', cut), ('combo c2', c2), ('combo b1', b1)) if False else ():
        print(nm, k.report(p))
    return k, stances + [lunge, drill, combo, barge, alt1, alt2, walk, run] + hits + [death, victory]


# ---------------------------------------------------------------- bows

def bow_arm(k, pose, aim, draw, hand='reach'):
    """The bow (left) arm solved so the arrow line (the bow's +x, the hand twisted by draw as TroopMotion does) points along aim
    (right, up, fwd), its fist at hand (right, up, fwd from the left shoulder) when given. The bow hand itself stays unkeyed."""
    sh = k.at(pose, 'left_arm')
    if hand == 'reach':   # drawn: the bow fist out along the line, 7 px from the shoulder; else let the arm settle
        hand = tuple(7.0 * np.array([aim[0], aim[1], aim[2]]) / np.linalg.norm(aim)) if draw > 0.5 else None
    # the limbs (the bow's -y) upright, canted 15 degrees out to the left so the bow clears the face
    up = v(-0.26, 0.97, 0) if draw > 0.5 else v(-0.2, 0.3, 0.93)
    return k.reach(pose, 'left', pos=None if hand is None else sh + v(*hand), aim=v(*aim), axis=(1, 0, 0), free=range(4),
                   twist=k.twist(draw), wpos=6.0, axis2=(0, -1, 0), aim2=up)


def string_hand(k, pose, draw, frame, off=(0, 0, 0)):
    """The draw (right) hand on the string's nock (frame 0 rest .. 3 full draw), plus off (right, up, fwd) px."""
    return k.reach(pose, 'right', pos=k.nock(pose, draw, frame) + v(*off), free=range(4), wpos=8.0)


def quiver_hand(k, pose, up=0.0):
    """The draw hand at the quiver's mouth over the right shoulder (the archer's quiver bone, its top end)."""
    M = k.mats(pose)
    name = 'quiver' if 'quiver' in M else 'tall_quiver'
    boxes = next(b_['boxes'] for b_ in k.json['bones'] if b_['name'] == name)
    y0 = min(x['origin'][1] for x in boxes)
    top = (M[name] @ np.array([0, y0 - 0.5 - up, 1.5, 1.0]))[:3]
    return k.reach(pose, 'right', pos=top, free=range(4), wpos=8.0)


def archer():
    """Order Archer: light, quick, hooded skirmisher."""
    k = Kit('archer')
    F = (0, 0.03, 1)                     # the arrow's line: level, at the enemy
    # AR-1 idle: bow low, an arrow nocked loosely, light on the feet
    idle = P(torso(drop=0.5, body=(4, 8, 0), chest=(2, 10, 0), head=(-2, -14, 0)), legs((6, -12), (8, 10), (3, -3), (14, 6)),
             fk((-6, 0, 4), -30, (-14, -10, -6), -24))
    idle = string_hand(k, idle, 0, 0, off=(0.5, 0, 0))
    idle_b = add(idle, root=[0, 0, 0, 0, 0.35, 0], body=[1, 0, 0], left_arm=[-2, 0, 0], right_arm=[-2, 0, 0])
    # carry: the bow carried low at the side, the draw hand free
    carry = P(torso(body=(3, 0, 0)), legs(), fk((-8, 0, 4), -18, (-10, -6, -8), -20))
    # guard: ready, crouched a little, bow half raised across the body
    guard = P(torso(drop=0.9, body=(8, 20, 0), chest=(2, 18, 0), head=(-4, -32, 0)), legs((10, -16), (12, 14), (3, -3), (20, 4)), fk((-20, 0, 0), -40, None, None))
    guard = bow_arm(k, guard, (0.3, -0.4, 0.86), 0.0, hand=(0.5, -6, 3.5))
    guard = string_hand(k, guard, 0, 0, off=(0.5, 0, 0))
    # aim: side-on, the bow up on the line, an arrow nocked (the code turns the bow upright with the aim stance)
    aim = P(torso(drop=0.7, body=(2, 34, 0), chest=(0, 38, 0), head=(-2, -66, 0)), legs((12, -14), (8, 12), (3, -3), (40, 6)), fk((-80, 0, 0), -60, (-80, -20, 0), -10))
    aim = bow_arm(k, aim, F, 1.0, hand=(0.0, 0.0, 7.0))
    aim = string_hand(k, aim, 1.0, 0)
    stances = [
        Clip('idle', 3.0, [(0, idle, CR), (1.5, idle_b, CR), (3.0, idle, CR)], mode='stance', loop=True),
        Clip('carry', 2.0, [(0, carry, CR), (1.0, add(carry, left_arm=[-2, 0, 0]), CR), (2.0, carry, CR)], mode='stance', loop=True),
        Clip('guard', 2.4, [(0, guard, CR), (1.2, add(guard, root=[0, 0, 0, 0, 0.25, 0]), CR), (2.4, guard, CR)], mode='stance', loop=True),
        Clip('aim', 3.0, [(0, aim, CR), (1.5, add(aim, chest=[0, 1, 0], root=[0, 0, 0, 0, 0.2, 0]), CR), (3.0, aim, CR)], mode='stance', loop=True),
    ]
    stances = [live(c, arms=0 if c.name == 'aim' else 1) for c in stances]

    def shot(name, slot, line, timeline):
        return Clip(name, 2.0, timeline, base='aim', slot=slot, markers=line)

    # every shot ends with a reload: the hand goes over the shoulder to the quiver and nocks a new arrow (AR-9)
    def reload_keys(t0, pose):
        reach = quiver_hand(k, add(pose, head=[0, 8, 0]))
        pull = quiver_hand(k, add(pose, head=[0, 8, 0]), up=4)
        return [(t0, reach, BZ), (t0 + 0.2, pull, BZ), (t0 + 0.45, string_hand(k, aim, 1.0, 0, off=(1, 0.5, 2)), BZ)]

    # AR-6: quick shot, a snap up from low, a fast draw, a short hold, loose (bow_direct: the loose on tick 16)
    LOAD = legs((14, -16), (16, 20), (3, -3), (40, 6))          # weight down into the knees at full draw
    q0 = bow_arm(k, P(aim, torso(drop=1.8, body=(10, 30, 0), chest=(2, 22, 0), head=(-4, -50, 0)), legs((16, -18), (22, 26), (3, -3), (40, 6))), (0.2, -0.45, 0.87), 1.0)
    q0 = string_hand(k, q0, 1.0, 0)
    q1 = string_hand(k, bow_arm(k, P(add(aim, root=[0, 0, 0, 0, 1.2, 0]), LOAD), (0, 0.06, 1), 1.0), 1.0, 1)
    qf = string_hand(k, bow_arm(k, P(add(aim, chest=[0, 3, 0], root=[0, 0, 0, 0, 1.3, 0]), LOAD), F, 1.0), 1.0, 3)
    # the loose: a hop back off the shot, the bow arm kicking up
    ql = string_hand(k, bow_arm(k, P(add(aim, chest=[0, 5, 0], head=[0, -2, 0], body=[-4, 0, 0], root=[0, 0, 0, 0, 0.2, 1.2]), legs((4, -6), (8, 10), (3, -3), (40, 6))),
                                (0, 0.15, 0.99), 1.0), 1.0, 3, off=(1.0, 0.3, -1.2))
    qs = string_hand(k, bow_arm(k, P(add(aim, root=[0, 0, 0, 0, 1.0, 1.4]), LOAD), (0, 0.05, 1), 1.0), 1.0, 3, off=(1.0, 0.3, -1.2))
    quick = shot('ar_quick_shot', 'bow_direct', {'draw': 0.45, 'full': 0.65, 'impact': 0.8},
                 [(0, aim, L), (0.25, q0, BZ), (0.45, q1, BZ), (0.65, qf, L), (0.8, qf, L), (0.9, ql, BZ), (1.1, qs, BZ)]
                 + reload_keys(1.3, aim) + [(2.0, aim, BZ)])
    # AR-7: aimed shot, standing tall, leaning into the line, the head down to the string, a long still hold, a clean loose
    TALL = legs((12, -14), (12, 16), (3, -3), (40, 6))
    a1 = string_hand(k, bow_arm(k, P(add(aim, root=[0, 0, 0, 0, 0.6, 0], head=[3, 0, 0]), TALL), F, 1.0), 1.0, 0)
    a2 = string_hand(k, bow_arm(k, P(add(aim, chest=[0, 3, 0], head=[6, -4, 4], body=[4, 0, 0], root=[0, 0, 0, 0, 0.9, -0.3]), TALL), F, 1.0), 1.0, 3)
    a3 = string_hand(k, bow_arm(k, P(add(aim, chest=[0, 3, 0], head=[6, -4, 4], body=[4, 0, 0], root=[0, 0, 0, 0, 1.0, -0.3]), TALL), (0, 0.035, 1), 1.0), 1.0, 3)
    al = string_hand(k, bow_arm(k, P(add(aim, chest=[0, 4, 0], head=[2, -3, 2], body=[-2, 0, 0], root=[0, 0, 0, 0, 0.5, 0.5]), TALL), (0, 0.1, 1), 1.0), 1.0, 3,
                     off=(1.2, 0, -1.4))
    aimed = shot('ar_aimed_shot', 'bow_direct', {'draw': 0.2, 'full': 0.55, 'impact': 0.8},
                 [(0, aim, L), (0.2, a1, BZ), (0.55, a2, L), (0.7, a3, CR), (0.8, a2, L), (0.95, al, BZ), (1.15, al, BZ)] + reload_keys(1.35, aim)[:2] + [(2.0, aim, BZ)])
    # AR-8: retreating shot, two steps back, a turn, loosed mid-step (bow_volley: aimed up into an arc)
    UP = (0, 0.42, 0.91)
    r1 = P(add(aim, root=[0, 0, 0, 0, 0.8, 1.6], body=[-4, -10, 0]), legs((-10, 18), (16, 4), (3, -3), (30, 6)))
    r1 = string_hand(k, bow_arm(k, r1, (0.3, -0.3, 0.9), 1.0), 1.0, 0)
    r2 = P(add(aim, root=[0, 0, 0, 0, 0.5, 3.2], body=[-6, 4, 0], chest=[-4, 4, 0]), legs((18, -8), (6, 20), (3, -3), (30, 6)))
    r2f = string_hand(k, bow_arm(k, r2, UP, 1.0), 1.0, 3)
    r3 = string_hand(k, bow_arm(k, P(add(aim, root=[0, 0, 0, 0, 0.6, 4.0], body=[-6, 6, 0], chest=[-4, 6, 0]), legs((8, 6), (14, 10), (3, -3), (30, 6))), UP, 1.0), 1.0, 3,
                     off=(2.0, 0, -2.0))
    back = shot('ar_retreat_shot', 'bow_volley', {'draw': 0.4, 'full': 0.65, 'impact': 0.8},
                [(0, aim, L), (0.3, r1, BZ), (0.5, mix(r1, r2, 0.6), BZ), (0.65, r2f, L), (0.8, r2f, L), (0.95, r3, BZ), (1.2, mix(r3, aim, 0.5), BZ)]
                + reload_keys(1.35, aim)[:2] + [(2.0, aim, BZ)])
    # AR-9 reload, on its own (a named clip the game can play by scene pose; every shot above ends with one)
    rl = [(0, aim, L)] + [(t + 0.1, p_, i) for t, p_, i in reload_keys(0.15, aim)] + [(0.9, aim, BZ)]
    reload = Clip('reload', 0.9, rl, mode='whole')
    # AR-2: tests the bowstring with two fingers, head cocked to listen
    pl = P(idle, torso(drop=0.5, body=(6, 8, 0), chest=(4, 14, 0), head=(14, -4, 10)), fk(None, None, (-34, -12, -4), -64))
    pl = k.reach(pl, 'right', pos=k.nock(pl, 0, 0) + v(0.5, 2.5, 0), free=range(4))
    pl2 = add(pl, right_arm=[3, 0, -2], right_forearm=[-6, 0, 0])
    alt1 = Clip('idle_alt_1', 2.8, [(0, idle, CR), (0.5, pl, CR), (0.8, pl2, CR), (1.0, pl, CR), (1.35, pl2, CR), (1.55, pl, CR), (2.1, add(idle, head=[4, 0, 0]), CR), (2.8, idle, CR)], base='idle')
    # AR-3: scans the horizon, a hand shading the eyes
    sh = P(idle, torso(drop=0.3, body=(0, 4, 0), chest=(-2, 4, 0), head=(-8, -30, 0)), fk((-150, 30, -10), -110, None, None))
    sh_r = add(sh, head=[0, 50, 0], chest=[0, 10, 0])
    alt2 = Clip('idle_alt_2', 3.6, [(0, idle, CR), (0.5, sh, CR), (1.4, add(sh, head=[0, 18, 0]), CR), (2.3, sh_r, CR), (2.8, sh_r, CR), (3.6, idle, CR)], base='idle')
    # AR-4 walk: crouched and alert (hips low, a soft bob, the head steady and level, the bow low)
    def wk(dip, sway, chest):
        return {'root': [0, 0, 0, 0, 1.3 + dip, 0], 'body': [9, 0, sway], 'chest': [0, chest, 0], 'head': [-8, -chest, 0], 'left_arm': [-6, 0, 0]}
    walk = Clip('walk', 1.0, [(0, wk(0.3, 1, -4), L), (0.25, wk(-0.2, 0, 0), BZ), (0.5, wk(0.3, -1, 4), L), (0.75, wk(-0.2, 0, 0), BZ), (1.0, wk(0.3, 1, -4), L)],
                mode='raw', base='carry', loop=True, markers={'stride_12': 0})
    # AR-5 run: quick and light, the bow carried at the side, the draw arm pumping
    def rn(dip, chest, pump):
        return {'root': [0, 0, 0, 0, dip, 0], 'body': [7, 0, 0], 'chest': [0, chest, 0], 'head': [-6, -chest * 0.6, 0],
                'right_arm': [pump, 0, 0], 'right_forearm': [-50, 0, 0], 'left_arm': [-pump * 0.4, 0, 0]}
    run = Clip('run', 1.0, [(0, rn(0.7, -7, -30), L), (0.25, rn(-0.3, 0, 0), BZ), (0.5, rn(0.7, 7, 30), L), (0.75, rn(-0.3, 0, 0), BZ), (1.0, rn(0.7, -7, -30), L)],
               mode='raw', base='carry', loop=True, markers={'stride_18': 0})
    # AR-10: flinches and ducks, the bow raised as cover (raw: over whichever stance is up)
    def hit(name, k_):
        duck = {'root': [0, 0, 0, 0, 1.8 * k_, 0.8], 'body': [14 * k_, 0, 0], 'head': [16 * k_, 0, 0], 'left_arm': [-30 * k_, 10, 0], 'left_forearm': [-30 * k_, 0, 0],
                'right_arm': [-20 * k_, 0, 10]}
        z = {b: [0, 0, 0] for b in duck}
        z['root'] = [0, 0, 0, 0, 0, 0]
        return Clip(name, 0.9, [(0, z, L), (0.1, duck, BZ), (0.35, duck, CR), (0.6, mix(z, duck, 0.3), CR), (0.9, z, L)], mode='raw')
    hits = [hit('hit_body', 1.0), hit('hit_head', 1.2), hit('hit_legs', 0.7)]
    # AR-11: spins with the hit, falls to the side, the bow flung out of reach
    rest = P(torso(), legs(), fk((0, 0, 0), 0, (0, 0, 0), 0))
    spin = P(torso(root=(0, 50, 0), drop=0.6, body=(-6, 20, 8), head=(-10, 20, 10)), legs((-10, 10), (10, 20)), fk((-30, 0, 40), -30, (-40, 0, -70), -10))
    fall = P(torso(root=(0, 100, 50), drop=4.0, side=-2, body=(4, 10, 10), head=(0, 10, 20)), legs((-20, 10), (30, 40)), fk((-60, 0, 60), -20, (-100, 0, -80), 0))
    down = k.grounded(P(torso(root=(0, 120, 88), drop=10, side=-4, body=(2, 0, 4), head=(0, 0, 30)), legs((-30, 10), (40, 20)), fk((-90, 0, 40), -30, (-170, 0, -20), 0)))
    death = Clip('death', 1.8, [(0, rest, L), (0.25, spin, BZ), (0.7, fall, BZ), (1.1, down, BZ), (1.8, down, L)], mode='whole')
    return k, stances + [quick, aimed, back, reload, alt1, alt2, walk, run] + hits + [death]


# ---------------------------------------------------------------- two hands on one hilt

def both(k, pose, along=2.5):
    """The off (left) hand on the grip, along px from the sword hand toward the pommel."""
    M = k.mats(pose)['right_hand'] @ k.grip
    at = (M @ np.array([0, along, 0, 1.0]))[:3]
    return k.reach(pose, 'left', pos=at, free=range(4), wpos=8.0)


def swing(k, pose, arm_r, fore_r, aim, flat=None, along=2.5, two=True, **kw):
    """A two-handed sword pose: the sword arm keyed, the wrist aimed, the off hand on the grip."""
    p = P(pose, fk(arm_r, fore_r))
    p = sword(k, p, aim, flat, **kw)
    return both(k, p, along) if two else p


def felled(theta, extra=None):
    """A body tipping backward stiff about its heels by theta degrees: root pitch back, the hips swung up and back round the feet."""
    r = np.radians(theta)
    return P(torso(root=(-theta, 0, 0), drop=12 - 12 * np.cos(r), fwd=-12 * np.sin(r)), extra or {})


# ---------------------------------------------------------------- Templar (sentinel): heavy, solemn, devout

def sentinel():
    k = Kit('sentinel')
    T = dict(two=True)
    # SE-1 idle: two-handed, the point planted by the boots, the hands stacked on the pommel
    idle = P(torso(drop=0.2, body=(2, 0, 0), chest=(-2, 0, 0), head=(4, 0, 0)), legs((2, -4), (2, 2), (3, -3), (10, 10)))
    idle = swing(k, idle, (-24, -14, 10), -62, (-0.05, -1, 0.12), along=-0.5, **PLANT)
    idle_b = swing(k, add(idle, chest=[-1, 0, 0], root=[0, 0, 0, 0, -0.15, 0]), (-25, -14, 10), -62, (-0.05, -1, 0.12), along=-0.5, **PLANT)
    # guard: two-handed at the hip, the point up at the enemy's face (a plough guard), feet wide and rooted
    g = P(torso(drop=1.2, body=(6, -14, 0), chest=(2, -10, 0), head=(-6, 22, 0)), legs((16, -18), (12, 14), (4, -4), (16, 8)))
    guard = swing(k, g, (-30, -20, 6), -58, (-0.15, 0.55, 0.82))
    # carry: the sword held upright before the chest, as in a procession
    c = P(torso(body=(0, 0, 0), chest=(-2, 0, 0), head=(-2, 0, 0)), legs())
    carry = swing(k, c, (-40, -6, 14), -86, (0.3, 1, 0.1))     # upright, outboard of the visor
    # SE-9: braced, the sword planted and one knee down, holding the line (the brace stance)
    # (a full kneel as a stance fights the engine's foot planting, which twists the kneeling thigh to keep its foot: a deep braced
    # lunge instead, the back knee low, the front knee over the foot)
    br = P(torso(drop=2.6, fwd=0.4, body=(10, -8, 0), chest=(2, -6, 0), head=(-8, 10, 0)),
           {'right_leg': [30, 6, 3], 'right_shin': [36, 0, 0], 'right_foot': [-30, 0, 0], 'left_leg': [-40, -4, -3], 'left_shin': [52, 0, 0], 'left_foot': [-10, 0, 0]})
    brace = swing(k, br, (-50, -16, 8), -46, (0.0, -0.78, 0.62), along=-0.5, **PLANT)
    stances = [
        Clip('idle', 4.5, [(0, idle, CR), (2.25, idle_b, CR), (4.5, idle, CR)], mode='stance', loop=True),
        Clip('guard', 3.0, [(0, guard, CR), (1.5, add(guard, root=[0, 0, 0, 0, 0.2, 0]), CR), (3.0, guard, CR)], mode='stance', loop=True),
        Clip('carry', 2.0, [(0, carry, CR), (1.0, add(carry, chest=[-1, 0, 0]), CR), (2.0, carry, CR)], mode='stance', loop=True),
        Clip('brace', 3.0, [(0, brace, CR), (1.5, add(brace, chest=[-1, 0, 0]), CR), (3.0, brace, CR)], mode='stance', loop=True),
    ]
    stances = [live(c, amp=0.8, arms=0 if c.name in ('idle', 'brace', 'guard', 'carry') else 1, shift=0.6) for c in stances]
    # SE-6: overhead cleave, two-handed, all the body's weight behind it (overhead slot: impact 0.5 s)
    # the shield rides the off forearm, so the wind-up is one-handed with the shield held up in front (two hands overhead would lift
    # the board onto the helm); the weight comes from the whole body dropping through
    SH = fk(None, None, (-62, 30, 0), -30)
    w1 = swing(k, P(guard, torso(drop=0.6, fwd=-0.8, body=(-8, 6, 0), chest=(-8, 6, 0), head=(-10, -4, 0)), SH), (-132, -10, -6), -94, (0.1, 0.62, -0.78), two=False)
    w2 = swing(k, P(guard, torso(drop=0.5, fwd=-1.0, body=(-10, 6, 0), chest=(-10, 6, 0), head=(-12, -4, 0)), SH), (-136, -8, -6), -96, (0.1, 0.55, -0.83), two=False)
    c1 = swing(k, P(guard, torso(drop=2.4, fwd=1.6, body=(16, -6, 0), chest=(8, -6, 0), head=(-8, 6, 0)), legs((24, -28), (14, 26), (4, -4), (16, 8))),
               (-90, -12, 4), -36, (-0.05, 0.1, 1.0), two=False)
    c1 = P(c1, fk(None, None, (-50, 30, 0), -34))
    c2 = swing(k, P(guard, torso(drop=3.0, fwd=2.0, body=(26, -6, 0), chest=(12, -6, 0), head=(-14, 6, 0)), legs((28, -32), (16, 30), (4, -4), (16, 8)), fk(None, None, (-46, 30, 0), -34)),
               (-48, -10, 6), -30, (-0.05, -0.42, 0.9), two=False)
    cleave = Clip('se_cleave', 1.9, [(0, guard, L), (0.25, w1, BZ), (0.4, w2, L), (0.5, c1, L), (0.6, c2, BZ), (0.95, c2, CR), (1.4, mix(c2, guard, 0.6), BZ), (1.9, guard, L)],
                  markers={'impact': 0.5}, slot='sword_shield_overhead')
    # SE-7: a horizontal sweep that turns the hips through (low_cut slot: impact 0.4 s)
    s1 = swing(k, P(guard, torso(drop=1.6, body=(6, 26, 0), chest=(2, 22, 0), head=(-6, -24, 0))), (-70, 34, -10), -40, (0.95, 0.2, 0.2))
    s2 = swing(k, P(guard, torso(drop=1.8, body=(8, 30, 0), chest=(4, 26, 0), head=(-6, -28, 0))), (-72, 36, -10), -38, (0.97, 0.1, 0.2))
    s3 = swing(k, P(guard, torso(drop=2.2, fwd=1.0, body=(10, -10, 0), chest=(6, -12, 0), head=(-8, 14, 0))), (-68, -10, 4), -18, (-0.15, -0.15, 0.97), flat=(0, 1, 0))
    s4 = swing(k, P(guard, torso(drop=2.2, fwd=1.2, body=(10, -34, 0), chest=(6, -30, 0), head=(-8, 30, 0))), (-62, -40, 10), -24, (-0.95, -0.1, 0.3))
    sweep = Clip('se_sweep', 1.8, [(0, guard, L), (0.2, s1, BZ), (0.3, s2, L), (0.4, s3, L), (0.55, s4, BZ), (0.9, s4, CR), (1.3, mix(s4, guard, 0.6), BZ), (1.8, guard, L)],
                 markers={'impact': 0.4}, slot='sword_shield_low_cut')
    # SE-8: the pommel driven into the face, then a short cut (thrust slot: the pommel lands on 0.35 s)
    p1 = swing(k, P(guard, torso(drop=1.2, body=(4, 18, 0), chest=(0, 16, 0), head=(-4, -16, 0))), (-50, 10, 6), -100, (0.3, -0.4, -0.86))
    p2 = swing(k, P(guard, torso(drop=1.6, fwd=2.4, body=(12, -10, 0), chest=(4, -12, 0), head=(-8, 12, 0)), legs((20, -26), (10, 22), (4, -4), (16, 8))),
               (-96, -6, 4), -40, (0.1, -0.35, -0.93))                 # pommel first, the blade back past the shoulder
    p3 = swing(k, P(p2, torso(drop=1.8, fwd=2.6, body=(16, 10, 0), chest=(8, 14, 0), head=(-8, -10, 0)), fk(None, None, (-60, 30, 0), -30)), (-120, 6, -8), -90, (0.3, 0.6, -0.74), two=False)
    p4 = swing(k, P(p2, torso(drop=2.2, fwd=2.8, body=(20, -16, 0), chest=(10, -18, 0), head=(-10, 16, 0)), fk(None, None, (-48, 30, 0), -34)), (-60, -24, 8), -30, (-0.5, -0.6, 0.62), two=False)
    pommel = Clip('se_pommel', 1.9, [(0, guard, L), (0.2, p1, BZ), (0.25, p1, L), (0.35, p2, L), (0.55, p2, CR), (0.75, p3, BZ), (0.9, p4, L), (1.4, mix(p4, guard, 0.6), BZ),
                                     (1.9, guard, L)], markers={'impact': 0.35}, slot='sword_shield_thrust')
    # SE-2: a short prayer, the head bowed over the crossguard
    pr = add(idle, head=[30, 0, 0], chest=[8, 0, 0], body=[2, 0, 0], root=[0, 0, 0, 0, 0.3, 0])
    pr = both(k, swing(k, pr, (-30, -14, 10), -74, (-0.05, -1, 0.1), along=-0.5, two=False, **PLANT), along=-0.5)
    alt1 = Clip('idle_alt_1', 4.0, [(0, idle, CR), (0.8, pr, CR), (2.6, add(pr, head=[4, 0, 0]), CR), (3.3, add(idle, head=[-4, 0, 0]), CR), (4.0, idle, CR)], base='idle')
    # SE-3: rolls his shoulders under the weight of the plate
    def roll(rx, rz, hd):
        return add(idle, chest=[rx, 0, rz], head=[hd, 0, -rz * 0.6], right_arm=[0, 0, rz * 0.4], left_arm=[0, 0, rz * 0.4])
    alt2 = Clip('idle_alt_2', 2.8, [(0, idle, CR), (0.5, roll(-6, 6, -6), CR), (0.95, roll(4, 0, 6), CR), (1.4, roll(-6, -6, -6), CR), (1.9, roll(3, 0, 4), CR),
                                     (2.8, idle, CR)], base='idle')
    # SE-4 walk: slow, heavy, measured: a deep settle on each heel, a roll of the shoulders over the planted leg
    def wk(dip, roll_, chest):
        return {'root': [0, 0, roll_ * 0.5, 0, dip, 0], 'body': [3, 0, roll_], 'chest': [0, chest, 0], 'head': [0, -chest * 0.7, -roll_ * 0.5]}
    walk = Clip('walk', 1.0, [(0, wk(0.9, 2.5, -3), L), (0.2, wk(0.5, 1.5, -2), BZ), (0.35, wk(-0.3, 0, 0), BZ), (0.5, wk(0.9, -2.5, 3), L), (0.7, wk(0.5, -1.5, 2), BZ),
                              (0.85, wk(-0.3, 0, 0), BZ), (1.0, wk(0.9, 2.5, -3), L)], mode='raw', base='carry', loop=True, markers={'stride_13': 0})
    # SE-5 run: a lumbering charge, the sword held high
    def rn(dip, chest):
        return {'root': [0, 0, 0, 0, dip, 0], 'body': [8, 0, 0], 'chest': [-2, chest, 0], 'head': [-6, -chest * 0.6, 0],
                'right_arm': [-34, 0, 0], 'left_arm': [-34, 0, 0]}
    run = Clip('run', 1.0, [(0, rn(1.4, -6), L), (0.25, rn(-0.2, 0), BZ), (0.5, rn(1.4, 6), L), (0.75, rn(-0.2, 0), BZ), (1.0, rn(1.4, -6), L)],
               mode='raw', base='carry', loop=True, markers={'stride_15': 0})
    # SE-10: barely moves: a shudder in the plate, rights himself
    def hit(name, s_):
        z = {'root': [0] * 6, 'body': [0, 0, 0], 'chest': [0, 0, 0], 'head': [0, 0, 0]}
        a = {'root': [0, 0, 0, 0, 0.3 * s_, 0.3 * s_], 'body': [-3 * s_, 0, 1], 'chest': [-2 * s_, 2, 0], 'head': [-4 * s_, 0, 2]}
        b = {'root': [0, 0, 0, 0, 0.1, 0.1], 'body': [1, 0, -1], 'chest': [1, -1, 0], 'head': [1, 0, -1]}
        return Clip(name, 0.7, [(0, z, L), (0.05, a, L), (0.15, b, CR), (0.25, mix(z, a, 0.4), CR), (0.4, z, CR), (0.7, z, L)], mode='raw')
    hits = [hit('hit_body', 1.0), hit('hit_head', 1.4), hit('hit_legs', 0.8)]
    # SE-11: falls backward like a felled tree, arms flung wide
    arms = fk((-40, 0, 60), -10, (-40, 0, -60), -10)
    stand = P(torso(), legs(), fk((-10, 0, 10), -20, (-10, 0, -10), -20))
    death = Clip('death', 2.2, [(0, stand, L), (0.3, P(felled(4), {'head': [-14, 0, 0]}, fk((0, 0, 20), -20, (0, 0, -20), -20)), BZ),
                                (0.8, P(felled(18), {'head': [-10, 0, 0]}, arms), BZ), (1.2, P(felled(50), arms), L), (1.45, P(felled(86), arms), L),
                                (1.6, k.grounded(P(felled(90), {'head': [-8, 0, 0]}, fk((-10, 0, 70), -10, (-10, 0, -70), -10))), BZ),
                                (2.2, k.grounded(P(felled(90), {'head': [-6, 20, 0]}, fk((0, 0, 78), -6, (0, 0, -78), -6))), L)], mode='whole')
    return k, stances + [cleave, sweep, pommel, alt1, alt2, walk, run] + hits + [death]


# ---------------------------------------------------------------- Lord Marshal Aldric (gl_marshal): broad, regal, theatrical

def gl_marshal():
    k = Kit('gl_marshal')
    # GM-1 idle: a commanding stance, the sword point down before him, both hands on the pommel, chest out, chin up
    idle = P(torso(body=(-2, 0, 0), chest=(-5, 0, 0), head=(-6, 0, 0)), legs((0, 0), (0, 0), (5, -5), (14, 14)))
    idle = swing(k, idle, (-30, -16, 12), -58, (0, -1, 0.18), along=-0.5, **PLANT)
    idle_b = swing(k, add(idle, chest=[-1.5, 0, 0], head=[-1, 0, 0]), (-30, -16, 12), -58, (0, -1, 0.18), along=-0.5, **PLANT)
    # guard: the shield forward, the sword cocked high behind it, ready to come down
    g = P(torso(drop=0.8, body=(4, -8, 0), chest=(0, -10, 0), head=(-6, 16, 0)), legs((12, -16), (10, 12), (4, -4), (14, 8)),
          fk((-126, 14, -16), -70, (-46, 25, 0), -20))
    guard = sword(k, g, (0.35, 0.75, 0.55))
    # carry: the sword at the slope, upright against the shoulder, the shield at his side
    c = P(torso(chest=(-4, 0, 0), head=(-4, 0, 0)), legs(), fk((-30, -2, 16), -92, (-14, 14, 0), -14))
    carry = sword(k, c, (0.32, 0.94, 0.1))                    # at the slope, outboard of the shoulder, clear of the face
    stances = [
        Clip('idle', 4.0, [(0, idle, CR), (2.0, idle_b, CR), (4.0, idle, CR)], mode='stance', loop=True),
        Clip('guard', 2.6, [(0, guard, CR), (1.3, add(guard, root=[0, 0, 0, 0, 0.2, 0], right_arm=[-2, 0, 0]), CR), (2.6, guard, CR)], mode='stance', loop=True),
        Clip('carry', 2.0, [(0, carry, CR), (1.0, add(carry, chest=[-1, 0, 0]), CR), (2.0, carry, CR)], mode='stance', loop=True),
    ]
    stances = [live(c, arms=0 if c.name == 'idle' else 1) for c in stances]
    # GM-6: a sweeping executioner's cut: the sword wheels up behind, then down in one long arc (overhead slot: impact 0.5 s)
    e1 = sword(k, P(guard, torso(drop=0.6, fwd=-0.6, body=(-8, 16, 0), chest=(-8, 18, 0), head=(-10, -10, 0)), fk((-150, 20, -20), -96, None, None)), (0.4, 0.3, -0.86))
    e2 = sword(k, add(e1, chest=[-2, 2, 0]), (0.4, 0.28, -0.87))
    e3 = sword(k, P(guard, torso(drop=2.2, fwd=2.0, body=(18, -10, 0), chest=(10, -14, 0), head=(-12, 14, 0)), legs((24, -30), (12, 28), (4, -4), (14, 8)),
                    fk((-96, -6, 4), -42, (-30, 25, 0), -24)), (-0.1, 0.25, 0.96))
    e4 = sword(k, P(e3, torso(drop=2.6, fwd=2.2, body=(26, -14, 0), chest=(12, -18, 0), head=(-14, 18, 0)), fk((-50, -18, 8), -24, None, None)), (-0.35, -0.5, 0.79))
    execu = Clip('gm_executioner', 1.8, [(0, guard, L), (0.25, e1, BZ), (0.4, e2, L), (0.5, e3, L), (0.6, e4, BZ), (1.0, e4, CR), (1.35, mix(e4, guard, 0.6), BZ), (1.8, guard, L)],
                 markers={'impact': 0.5}, slot='sword_shield_overhead')
    # GM-7: a riposte: the blade turns a blow aside, then the counter-thrust (thrust slot: impact 0.35 s)
    r1 = sword(k, P(guard, torso(drop=1.0, fwd=-0.6, body=(0, 12, 0), chest=(-2, 14, 0), head=(-6, -10, 0)), fk((-80, 30, -16), -64, (-40, 25, 0), -20)), (0.5, 0.85, 0.15))
    r2 = sword(k, add(r1, right_arm=[0, 6, -4], root=[0, 0, 0, 0, 0.1, 0.2]), (0.6, 0.78, 0.1))
    r3 = sword(k, P(guard, torso(drop=1.8, fwd=3.0, body=(12, -10, 0), chest=(6, -12, 0), head=(-10, 14, 0)), legs((28, -36), (8, 32), (4, -4), (14, 8)),
                    fk((-84, 2, 4), -10, (-30, 25, 0), -26)), (-0.04, 0.03, 1.0))
    r4 = sword(k, add(r3, root=[0, 0, 0, 0, 0.2, -0.2]), (-0.04, 0.0, 1.0))
    riposte = Clip('gm_riposte', 1.5, [(0, guard, L), (0.1, r1, BZ), (0.25, r2, L), (0.35, r3, L), (0.5, r4, BZ), (1.0, mix(r4, guard, 0.6), BZ), (1.5, guard, L)],
                   markers={'impact': 0.35}, slot='sword_shield_thrust')
    # GM-8: a three-hit flourish that ends in a pose (low_cut slot: the third lands on 0.4 s)
    f1 = sword(k, P(guard, torso(drop=1.2, body=(6, -14, 0), chest=(2, -16, 0), head=(-6, 14, 0)), fk((-80, -26, 4), -40, None, None)), (-0.8, 0.2, 0.56))
    f2 = sword(k, P(guard, torso(drop=1.4, body=(8, 14, 0), chest=(4, 16, 0), head=(-6, -12, 0)), fk((-78, 26, -8), -44, None, None)), (0.8, 0.15, 0.58))
    f3 = sword(k, P(guard, torso(drop=2.2, fwd=1.6, body=(14, -14, 0), chest=(6, -16, 0), head=(-10, 14, 0)), legs((22, -28), (10, 26), (4, -4), (14, 8)),
                    fk((-50, -24, 8), -26, (-30, 25, 0), -24)), (-0.55, -0.5, 0.67))
    pose = sword(k, P(torso(drop=0.6, body=(-2, -6, 0), chest=(-6, -8, 0), head=(-10, 10, 0)), legs((10, -12), (6, 8), (4, -4), (14, 8)),
                      fk((-30, 20, 30), -10, (-36, 25, 0), -20)), (0.55, -0.2, 0.8))            # the blade flicked out low to the side, chin up
    flourish = Clip('gm_flourish', 1.9, [(0, guard, L), (0.15, f1, L), (0.3, f2, L), (0.4, f3, L), (0.6, mix(f3, pose, 0.5), BZ), (0.85, pose, BZ), (1.3, pose, CR),
                                         (1.9, guard, BZ)], markers={'impact': 0.4}, slot='sword_shield_low_cut')
    # GM-2: points the sword forward, issuing an order
    pt = sword(k, P(idle, torso(body=(0, -8, 0), chest=(-4, -10, 0), head=(-8, 10, 0)), fk((-86, -4, 4), -6, (-14, 14, 0), -14)), (0, 0.08, 1.0))
    alt1 = Clip('idle_alt_1', 3.0, [(0, idle, CR), (0.6, pt, CR), (1.1, add(pt, right_arm=[-3, 0, 0], head=[-4, 0, 0]), CR), (1.9, pt, CR), (2.4, add(idle, head=[-3, 0, 0]), CR),
                                     (3.0, idle, CR)], base='idle')
    # GM-3: raises the sword high, rallying the line
    hi = sword(k, P(idle, torso(chest=(-8, 0, 0), head=(-18, 0, 0)), fk((-168, -4, -10), -10, (-30, 20, 0), -20)), (0, 1, 0.1))
    alt2 = Clip('idle_alt_2', 3.0, [(0, idle, CR), (0.6, hi, CR), (0.9, add(hi, right_arm=[8, 0, 0]), CR), (1.2, hi, CR), (1.5, add(hi, right_arm=[8, 0, 0]), CR), (1.9, hi, CR),
                                     (3.0, idle, CR)], base='idle')
    # GM-4 walk: a stately stride: chest out, a slight roll, little bob
    def wk(dip, sway, chest):
        return {'root': [0, 0, 0, 0, dip, 0], 'body': [-1, 0, sway], 'chest': [-3, chest, 0], 'head': [-3, -chest * 0.8, 0], 'left_arm': [chest * 1.5, 0, 0]}
    walk = Clip('walk', 1.0, [(0, wk(0.35, 2, -4), L), (0.25, wk(-0.1, 0, 0), BZ), (0.5, wk(0.35, -2, 4), L), (0.75, wk(-0.1, 0, 0), BZ), (1.0, wk(0.35, 2, -4), L)],
                mode='raw', base='carry', loop=True, markers={'stride_15': 0})
    # GM-5 run: a commanding charge, the sword raised
    def rn(dip, chest):
        return {'root': [0, 0, 0, 0, dip, 0], 'body': [8, 0, 0], 'chest': [-2, chest, 0], 'head': [-10, -chest * 0.6, 0], 'right_arm': [-70, 0, -6],
                'right_forearm': [30, 0, 0], 'left_arm': [-20, 0, 0]}
    run = Clip('run', 1.0, [(0, rn(1.0, -6), L), (0.25, rn(-0.3, 0), BZ), (0.5, rn(1.0, 6), L), (0.75, rn(-0.3, 0), BZ), (1.0, rn(1.0, -6), L)],
               mode='raw', base='carry', loop=True, markers={'stride_18': 0})
    # GM-10: unimpressed: a step back, then he straightens his cape
    def hit(name, s_):
        z = {'root': [0] * 6, 'body': [0, 0, 0], 'chest': [0, 0, 0], 'head': [0, 0, 0], 'left_arm': [0, 0, 0], 'left_forearm': [0, 0, 0]}
        a = {'root': [0, 0, 0, 0, 0.3, 1.2 * s_], 'body': [-5 * s_, 0, 0], 'chest': [-3 * s_, 0, 0], 'head': [-6 * s_, 0, 0], 'left_arm': [0, 0, 0], 'left_forearm': [0, 0, 0]}
        tug = {'root': [0, 0, 0, 0, 0, 0.6], 'body': [0, 0, 0], 'chest': [-4, -6, 0], 'head': [-4, 10, 0], 'left_arm': [-20, 30, -20], 'left_forearm': [-60, 0, 0]}
        return Clip(name, 1.2, [(0, z, L), (0.1, a, BZ), (0.35, mix(a, z, 0.5), CR), (0.6, tug, CR), (0.8, add(tug, left_forearm=[8, 0, 0]), CR), (1.2, z, CR)], mode='raw')
    hits = [hit('hit_body', 1.0), hit('hit_head', 1.2), hit('hit_legs', 0.7)]
    # GM-11: falls to one knee, raises the sword one last time, then falls
    stand = P(torso(), legs(), fk((-10, 0, 6), -14, (-14, 14, 0), -14))
    knee = P(torso(drop=4.6, body=(10, 0, 0), chest=(4, 0, 0), head=(14, 0, 0)),
             {'right_leg': [24, 6, 3], 'right_shin': [86, 0, 0], 'right_foot': [-40, 0, 0], 'left_leg': [-60, -4, -3], 'left_shin': [70, 0, 0], 'left_foot': [-8, 0, 0]},
             fk((-30, 0, 10), -20, (-20, 14, 0), -30))
    last = sword(k, P(knee, torso(drop=4.6, body=(-2, 0, 0), chest=(-8, 0, 0), head=(-20, 0, 0)), fk((-170, 0, -6), -10, None, None)), (0, 1, 0.05))
    slump = P(knee, torso(drop=5.0, body=(18, 0, 6), chest=(8, 0, 0), head=(20, 0, 0)), fk((-10, 0, 20), -10, None, None))
    down = k.grounded(P(torso(root=(84, 10, 10), drop=9.5, fwd=2.0, head=(-10, -60, 0)),
                        {'right_leg': [-4, 6, 3], 'right_shin': [30, 0, 0], 'left_leg': [-10, -4, -3], 'left_shin': [24, 0, 0]}, fk((-150, 0, 30), -16, (-10, 0, -40), -20)))
    down = k.reach(down, 'right', aim=v(0.5, 0, 0.87), hand_only=True)
    down = k.grounded(down)
    death = Clip('death', 3.0, [(0, stand, L), (0.4, knee, BZ), (0.9, knee, CR), (1.5, last, BZ), (1.9, last, CR), (2.2, slump, BZ), (2.6, down, BZ), (3.0, down, L)], mode='whole')
    # GM-9: war cry, the sword raised, the head thrown back (a rally tell; a scene pose)
    stand2 = P(torso(chest=(-4, 0, 0)), legs((0, 0), (0, 0), (5, -5), (14, 14)), fk((-30, -10, 6), -40, (-20, 20, 0), -20))
    cry = sword(k, P(torso(drop=0.8, body=(-6, 0, 0), chest=(-12, 0, 0), head=(-30, 0, 0)), legs((6, -8), (8, 8), (6, -6), (14, 14)), fk((-176, -6, -12), -6, (-30, 30, -20), -30)),
                (0, 1, 0))
    # the cry held with breath: the chest heaving, the head shaking with the shout, the sword hand trembling
    heave = lambda c_, h_, r_: add(cry, chest=[c_, 0, 0], head=[h_, r_, 0], right_arm=[c_ * 0.5, 0, 0], left_arm=[0, 0, -c_ * 0.5])
    war_cry = Clip('war_cry', 3.0, [(0, stand2, L), (0.3, add(stand2, root=[0, 0, 0, 0, 1.0, 0], chest=[8, 0, 0]), BZ), (0.7, cry, BZ), (1.1, heave(-3, -4, 4), CR),
                                    (1.5, heave(2, 2, -4), CR), (1.9, heave(-3, -5, 3), CR), (2.3, heave(2, 1, -2), CR), (3.0, cry, CR)], mode='whole')
    return k, stances + [execu, riposte, flourish, alt1, alt2, walk, run] + hits + [death, war_cry]


# ---------------------------------------------------------------- Order Longbow (longbow): tall, patient marksman

def longbow():
    k = Kit('longbow')
    F = (0, 0.03, 1)
    # LB-1 idle: the longbow's lower tip resting on the ground at his left side, leaning on it a little
    idle = P(torso(drop=0.2, root=(0, 0, -2), body=(2, 0, -3), chest=(0, 4, -2), head=(2, -6, 3)), legs((0, -4), (0, 2), (3, -5), (10, 6)),
             fk((-6, 0, 6), -12, (-30, -10, -20), -44))
    idle = bow_arm(k, idle, (0.1, -0.15, 0.98), 0.0, hand=(-1.5, -4.0, 3.0))
    # carry (LB-4): the bow over the shoulder, held at the grip by the left hand up by the collarbone
    carry = P(torso(body=(2, 0, 0), head=(0, 0, 0)), legs(), fk((-6, 0, 4), -16, (-10, -30, -10), -140))
    # aim: tall and side-on, the bow arm long, the draw to the jaw
    aim = P(torso(drop=0.5, body=(0, 36, 0), chest=(-2, 40, 0), head=(-4, -70, 0)), legs((12, -14), (6, 10), (4, -4), (44, 6)), fk((-84, 0, 0), -70, (-80, -20, 0), -10))
    aim = bow_arm(k, aim, F, 1.0)
    aim = string_hand(k, aim, 1.0, 0)
    guard = P(torso(drop=0.6, body=(4, 18, 0), chest=(0, 18, 0), head=(-2, -30, 0)), legs((8, -12), (8, 10), (3, -3), (20, 4)), fk((-16, 0, 6), -40, None, None))
    guard = bow_arm(k, guard, (0.25, -0.3, 0.92), 0.0, hand=(0.5, -5, 4))
    stances = [
        Clip('idle', 4.0, [(0, idle, CR), (2.0, add(idle, body=[0, 0, -1], root=[0, 0, -0.5, 0, 0.1, 0]), CR), (4.0, idle, CR)], mode='stance', loop=True),
        Clip('carry', 2.0, [(0, carry, CR), (1.0, add(carry, right_arm=[-2, 0, 0]), CR), (2.0, carry, CR)], mode='stance', loop=True),
        Clip('guard', 2.6, [(0, guard, CR), (1.3, add(guard, root=[0, 0, 0, 0, 0.2, 0]), CR), (2.6, guard, CR)], mode='stance', loop=True),
        Clip('aim', 3.0, [(0, aim, CR), (1.5, add(aim, chest=[0, 1, 0], root=[0, 0, 0, 0, 0.15, 0]), CR), (3.0, aim, CR)], mode='stance', loop=True),
    ]
    stances = [live(c, arms=0 if c.name == 'aim' else 1) for c in stances]

    def reload_keys(t0, pose):    # LB-9: up over the shoulder to the tall back quiver
        reach = quiver_hand(k, add(pose, head=[0, 10, 0]))
        pull = quiver_hand(k, add(pose, head=[0, 10, 0]), up=5)
        return [(t0, reach, BZ), (t0 + 0.2, pull, BZ), (t0 + 0.45, string_hand(k, aim, 1.0, 0, off=(1, 0.5, 2)), BZ)]

    # LB-6: a high arcing volley, the whole body tilted back into the shot (bow_volley: the loose on tick 16)
    UP = (0, 0.64, 0.77)
    v1 = string_hand(k, bow_arm(k, add(aim, body=[-6, 0, 0], chest=[-8, 0, 0], head=[-10, 0, 0]), (0, 0.4, 0.92), 1.0), 1.0, 1)
    v2 = string_hand(k, bow_arm(k, add(aim, body=[-10, 0, 0], chest=[-12, 2, 0], head=[-16, 0, 0], root=[0, 0, 0, 0, 0.3, 0]), UP, 1.0), 1.0, 3)
    vl = string_hand(k, bow_arm(k, add(aim, body=[-10, 0, 0], chest=[-12, 4, 0], head=[-16, 0, 0]), UP, 1.0), 1.0, 3, off=(1.2, 0.4, -1.4))
    volley = Clip('lb_volley', 2.0, [(0, aim, L), (0.3, v1, BZ), (0.6, v2, L), (0.8, v2, L), (0.95, vl, BZ), (1.15, vl, BZ)] + reload_keys(1.3, aim)[:2] + [(2.0, aim, BZ)],
                  base='aim', slot='bow_volley', markers={'draw': 0.25, 'full': 0.6, 'impact': 0.8})
    # LB-7: a direct heavy shot: a deep draw leaning into the bow, a hard release that rocks him back
    HEAVY = legs((16, -20), (18, 22), (4, -4), (44, 6))
    d1 = string_hand(k, bow_arm(k, P(add(aim, root=[0, 0, 0, 0, 0.9, -0.4], body=[4, 0, 0]), HEAVY), F, 1.0), 1.0, 1)
    d2 = string_hand(k, bow_arm(k, P(add(aim, root=[0, 0, 0, 0, 1.5, -0.6], body=[6, 0, 0], chest=[0, 4, 0]), HEAVY), F, 1.0), 1.0, 3, off=(0.8, 0, -0.6))
    dl = string_hand(k, bow_arm(k, P(add(aim, root=[0, 0, 0, 0, 0.6, 1.2], body=[-6, 0, 0], chest=[-2, 6, 0], head=[-4, 0, 0]), legs((6, -10), (8, 12), (4, -4), (44, 6))), (0, 0.14, 0.99), 1.0), 1.0, 3,
                     off=(1.4, 0.4, -1.6))
    heavy = Clip('lb_heavy_shot', 2.0, [(0, aim, L), (0.25, d1, BZ), (0.6, d2, L), (0.8, d2, L), (0.95, dl, BZ), (1.2, dl, CR)] + reload_keys(1.35, aim)[:2] + [(2.0, aim, BZ)],
                 base='aim', slot='bow_direct', markers={'draw': 0.25, 'full': 0.6, 'impact': 0.8})
    # LB-8: a kneeling shot: down on the right knee, steadier, the loose from low
    kn = P(aim, torso(drop=5.8, body=(4, 36, 0), chest=(-2, 40, 0), head=(-4, -70, 0)),
           {'right_leg': [14, 30, 4], 'right_shin': [100, 0, 0], 'right_foot': [-42, 0, 0], 'left_leg': [-66, -10, -4], 'left_shin': [80, 0, 0], 'left_foot': [-12, 0, 0]})
    k1 = string_hand(k, bow_arm(k, kn, (0, -0.1, 1), 1.0), 1.0, 1)
    k2 = string_hand(k, bow_arm(k, kn, (0, 0.06, 1), 1.0), 1.0, 3)
    kl = string_hand(k, bow_arm(k, add(kn, chest=[0, 4, 0]), (0, 0.06, 1), 1.0), 1.0, 3, off=(1.2, 0.2, -1.4))
    kneel = Clip('lb_kneel_shot', 2.0, [(0, aim, L), (0.3, k1, BZ), (0.6, k2, L), (0.8, k2, L), (0.95, kl, BZ), (1.25, kl, CR), (1.6, mix(kl, aim, 0.5), BZ), (2.0, aim, BZ)],
                  base='aim', slot='bow_direct', markers={'draw': 0.3, 'full': 0.6, 'impact': 0.8})
    reload = Clip('reload', 1.0, [(0, aim, L)] + [(t + 0.1, p_, i) for t, p_, i in reload_keys(0.15, aim)] + [(1.0, aim, BZ)], mode='whole')
    # LB-2: licks a finger and holds it up to the wind
    lick = k.reach(P(idle, torso(head=(-6, -6, 0))), 'right', pos=k.at(idle, 'head') + v(0, -3.5, 4.5), free=range(4))
    upf = P(idle, torso(head=(-10, -24, 0), chest=(-2, -6, 0)), fk((-160, 10, -6), -20, None, None))
    alt1 = Clip('idle_alt_1', 3.4, [(0, idle, CR), (0.5, lick, CR), (0.8, lick, CR), (1.3, upf, CR), (2.0, add(upf, head=[0, 30, 0]), CR), (2.6, upf, CR), (3.4, idle, CR)],
                base='idle')
    # LB-3: counts the arrows in the tall quiver, a touch for each
    cnt = quiver_hand(k, add(idle, head=[10, 30, 0], chest=[0, 10, 0]))
    taps = [(0.6 + 0.35 * i, add(cnt, right_forearm=[-8 if i % 2 else 0, 0, 0], right_arm=[0, 0, 3 * (i % 3)]), CR) for i in range(5)]
    alt2 = Clip('idle_alt_2', 3.4, [(0, idle, CR)] + taps + [(2.6, add(idle, head=[4, 0, 0]), CR), (3.4, idle, CR)], base='idle')
    # LB-4 walk: long, unhurried strides
    def wk(dip, sway, chest):
        return {'root': [0, 0, 0, 0, dip, 0], 'body': [1, 0, sway], 'chest': [0, chest, 0], 'head': [0, -chest * 0.6, 0], 'right_arm': [-chest * 3, 0, 0]}
    walk = Clip('walk', 1.0, [(0, wk(0.4, 1.2, -5), L), (0.25, wk(-0.3, 0, 0), BZ), (0.5, wk(0.4, -1.2, 5), L), (0.75, wk(-0.3, 0, 0), BZ), (1.0, wk(0.4, 1.2, -5), L)],
                mode='raw', base='carry', loop=True, markers={'stride_16': 0})
    # LB-5 run: an awkward jog, the long bow lifted clear of the ground
    def rn(dip, chest, bob):
        return {'root': [0, 0, 0, 0, dip, 0], 'body': [6, 0, 2 * bob], 'chest': [0, chest, 0], 'head': [-4, -chest * 0.4, -2 * bob],
                'left_arm': [-16 + 6 * bob, 0, 0], 'right_arm': [-chest * 4, 0, 0], 'right_forearm': [-40, 0, 0]}
    run = Clip('run', 1.0, [(0, rn(1.1, -6, 1), L), (0.25, rn(-0.2, 0, 0), BZ), (0.5, rn(1.1, 6, -1), L), (0.75, rn(-0.2, 0, 0), BZ), (1.0, rn(1.1, -6, 1), L)],
               mode='raw', base='carry', loop=True, markers={'stride_17': 0})
    # LB-10: steps back and draws the bow in to protect it
    def hit(name, s_):
        z = {'root': [0] * 6, 'body': [0, 0, 0], 'head': [0, 0, 0], 'left_arm': [0, 0, 0], 'left_forearm': [0, 0, 0]}
        a = {'root': [0, 0, 0, 0, 0.6, 1.4 * s_], 'body': [-6 * s_, 0, 0], 'head': [-10 * s_, 0, 0], 'left_arm': [20, 20, 0], 'left_forearm': [-40, 0, 0]}
        return Clip(name, 1.0, [(0, z, L), (0.1, a, BZ), (0.4, a, CR), (0.7, mix(z, a, 0.3), CR), (1.0, z, L)], mode='raw')
    hits = [hit('hit_body', 1.0), hit('hit_head', 1.2), hit('hit_legs', 0.7)]
    # LB-11: the bow arm drops first and the bow falls away, then he crumples beside it
    rest = P(torso(), legs(), fk((0, 0, 0), 0, (0, 0, 0), 0))
    drop = P(torso(drop=0.4, body=(6, 0, 4), head=(10, 0, 0)), legs(), fk((10, 0, 6), -10, (30, 0, -40), 0))
    buckle = P(torso(drop=3.6, root=(10, 0, 10), body=(16, 0, 8), head=(16, 0, 0)), legs((-30, -20), (60, 50)), fk((0, 0, 10), -10, (20, 0, -60), 0))
    down = k.grounded(P(torso(root=(10, 30, 86), drop=10, side=-1, body=(4, 0, 4), head=(0, 0, 10)), legs((-30, -40), (50, 60)), fk((-30, 0, 20), -20, (-20, 0, -100), 0)))
    death = Clip('death', 2.2, [(0, rest, L), (0.35, drop, BZ), (0.8, buckle, BZ), (1.4, down, BZ), (2.2, down, L)], mode='whole')
    return k, stances + [volley, heavy, kneel, reload, alt1, alt2, walk, run] + hits + [death]


# ---------------------------------------------------------------- Sunweaver (mage): graceful, flowing, radiant

def mage():
    k = Kit('mage')
    # MA-1 idle: the staff upright, the robe swaying, a gentle hover-like float in the posture
    def held(pose, arm, fore, aim, **kw):
        return sword(k, P(pose, fk(arm, fore)), aim, **kw)
    STRIKE = dict(wrist0=(120.0, 0.0, 0.0))   # the staff turned over in the fist so its head leads when the arm comes forward
    base = P(torso(body=(-1, 0, 0), chest=(-3, 0, 0), head=(-3, 0, 0)), legs((0, 0), (0, 0), (2, -2), (6, 6)), fk(None, None, (-10, 10, -6), -30))
    idle = held(base, (-24, -6, 8), -34, (0.05, 1, 0.1))
    idle_up = held(add(base, root=[0, 0, 0, 0, -0.5, 0], chest=[-1, 0, 0], left_arm=[-4, 0, -2]), (-25, -6, 8), -34, (0.05, 1, 0.12))
    idle_sway = held(add(base, root=[0, 0, 1.5, 0, 0.1, 0], body=[0, 0, -1.5], head=[0, 0, 2]), (-24, -6, 8), -34, (0.04, 1, 0.1))
    guard = held(P(torso(drop=0.6, body=(3, -10, 0), chest=(0, -10, 0), head=(-4, 14, 0)), legs((6, -10), (2, 4), (3, -3), (10, 6)), fk(None, None, (-50, 20, -20), -50)),
                 (-46, -10, 6), -40, (0.15, 0.85, 0.5))
    carry = held(P(torso(body=(0, 0, 0), chest=(-2, 0, 0)), legs(), fk(None, None, (-8, 10, -6), -20)), (-30, -6, 6), -30, (0.05, 1, 0.15))
    stances = [
        Clip('idle', 4.0, [(0, idle, CR), (1.0, idle_up, CR), (2.0, idle_sway, CR), (3.0, idle_up, CR), (4.0, idle, CR)], mode='stance', loop=True),
        Clip('guard', 2.6, [(0, guard, CR), (1.3, add(guard, root=[0, 0, 0, 0, -0.3, 0]), CR), (2.6, guard, CR)], mode='stance', loop=True),
        Clip('carry', 2.0, [(0, carry, CR), (1.0, add(carry, root=[0, 0, 0, 0, -0.2, 0]), CR), (2.0, carry, CR)], mode='stance', loop=True),
    ]
    stances = [live(c) for c in stances]
    S = dict(base='guard', slot='caster_cast')
    # MA-6: sunbolt, the staff thrust forward (caster_cast: the spell leaves on tick 10 = 0.5 s)
    b1 = held(P(guard, torso(drop=0.8, fwd=-0.6, body=(-4, 16, 0), chest=(-4, 18, 0), head=(-6, -16, 0)), fk(None, None, (-60, 30, -30), -70)), (-34, 20, 10), -62, (0.3, 0.8, -0.5), **STRIKE)
    b2 = held(add(b1, root=[0, 0, 0, 0, 0.1, 0.1], chest=[-1, 2, 0]), (-34, 20, 10), -64, (0.3, 0.82, -0.48), **STRIKE)
    b3 = held(P(guard, torso(drop=1.2, fwd=2.4, body=(10, -12, 0), chest=(4, -14, 0), head=(-8, 14, 0)), legs((20, -26), (4, 10), (3, -3), (10, 6)),
               fk(None, None, (-30, 20, -40), -20)), (-84, -4, 4), -12, (-0.02, 0.3, 0.95), **STRIKE)
    b4 = add(b3, root=[0, 0, 0, 0, 0.2, -0.2])
    bolt = Clip('ma_sunbolt', 1.6, [(0, guard, L), (0.2, b1, BZ), (0.35, b2, L), (0.5, b3, L), (0.65, b4, BZ), (1.1, mix(b4, guard, 0.6), BZ), (1.6, guard, L)],
                markers={'impact': 0.5}, **S)
    # MA-7: sunburst, the staff raised high overhead, then slammed down
    u1 = held(P(guard, torso(drop=-0.6, body=(-8, 0, 0), chest=(-8, 0, 0), head=(-20, 0, 0)), fk(None, None, (-150, 0, -20), -20)), (-160, -6, -6), -10, (0, 1, -0.1), **STRIKE)
    u2 = held(add(u1, root=[0, 0, 0, 0, -0.3, 0]), (-164, -6, -6), -8, (0, 1, -0.12), **STRIKE)
    s1 = held(P(guard, torso(drop=2.4, fwd=1.0, body=(18, 0, 0), chest=(8, 0, 0), head=(-6, 0, 0)), legs((16, -20), (6, 10), (3, -3), (10, 6)), fk(None, None, (-96, 20, -30), -40)),
              (-104, -8, 4), -20, (0, -0.6, 0.8), **STRIKE)
    s2 = add(s1, root=[0, 0, 0, 0, 0.4, 0], body=[2, 0, 0])
    # the burst: off the slam the staff springs up and both arms open wide, the light thrown out
    bu = held(P(s2, torso(drop=1.2, fwd=0.6, body=(2, 0, 0), chest=(-8, 0, 0), head=(-16, 0, 0)), fk(None, None, (-60, 0, -70), -10)), (-138, -6, 16), -20, (0.1, 0.8, 0.6), **STRIKE)
    burst = Clip('ma_sunburst', 1.9, [(0, guard, L), (0.3, u1, BZ), (0.4, u2, L), (0.5, s1, L), (0.6, s2, BZ), (0.8, bu, BZ), (1.1, bu, CR), (1.5, mix(bu, guard, 0.6), BZ), (1.9, guard, L)],
                 markers={'impact': 0.5}, **S)
    # MA-8: a staff swing when an enemy gets close: one hand, the staff's head swept across at chest height, the hips and shoulders
    # carrying the arc, the free arm flung out for balance
    w1 = held(P(guard, torso(drop=1.0, body=(4, 24, 0), chest=(2, 26, 0), head=(-4, -20, 0)), fk(None, None, (-20, 0, -24), -30)), (-66, 8, -6), -40, (0.6, 0.45, 0.66), **STRIKE)
    w2 = held(P(guard, torso(drop=1.4, fwd=1.4, body=(8, -28, 0), chest=(6, -28, 0), head=(-6, 26, 0)), legs((16, -20), (6, 10), (3, -3), (10, 6)), fk(None, None, (-28, 0, -44), -20)),
              (-84, -40, 14), -24, (-0.85, 0.05, 0.52), **STRIKE)
    w3 = held(P(w2, torso(drop=1.6, fwd=1.4, body=(10, -40, 0), chest=(6, -40, 0), head=(-6, 34, 0)), fk(None, None, (-20, 0, -50), -12)), (-66, -54, 18), -22, (-0.9, -0.35, 0.25), **STRIKE)
    staffswing = Clip('ma_staff_swing', 1.4, [(0, guard, L), (0.25, w1, BZ), (0.4, w1, L), (0.5, w2, L), (0.65, w3, BZ), (1.0, mix(w3, guard, 0.5), BZ), (1.4, guard, L)],
                      markers={'impact': 0.5}, **S)
    # MA-2: traces a sun sigil in the air with her free hand (a circle, then the rays)
    sh = k.at(idle, 'left_arm')
    ring = [k.reach(add(idle, head=[8, 0, 0]), 'left', pos=sh + v(-1 + 2.5 * np.cos(a), -1 + 2.5 * np.sin(a), 6), free=range(4)) for a in np.linspace(0, 2 * np.pi, 7)]
    sig = [(0.5 + 0.25 * i, p_, CR) for i, p_ in enumerate(ring)]
    alt1 = Clip('idle_alt_1', 3.6, [(0, idle, CR)] + sig + [(2.4, add(ring[0], left_arm=[-20, 0, 0]), CR), (2.8, ring[0], CR), (3.6, idle, CR)], base='idle')
    # MA-3: the staff spun slowly in the hand, a light flourish (half a turn each way)
    spin = [(0.4 + 0.3 * i, add(idle, right_hand=[0, a_, 0], head=[4, 0, 0]), CR) for i, a_ in enumerate((0, 60, 120, 180, 120, 60, 0))]
    alt2 = Clip('idle_alt_2', 3.0, [(0, idle, CR)] + spin + [(3.0, idle, CR)], base='idle')
    # MA-4 walk: a smooth glide: almost no bob, the robe hiding the steps, the staff steady
    def wk(dip, sway):
        return {'root': [0, 0, 0, 0, dip, 0], 'body': [0, 0, sway], 'chest': [0, -sway, 0], 'head': [0, sway * 0.5, 0]}
    walk = Clip('walk', 1.0, [(0, wk(0.1, 1), L), (0.25, wk(-0.05, 0), BZ), (0.5, wk(0.1, -1), L), (0.75, wk(-0.05, 0), BZ), (1.0, wk(0.1, 1), L)],
                mode='raw', base='carry', loop=True, markers={'stride_11': 0})
    # MA-5 run: the robe gathered up in the free hand, the staff carried forward
    def rn(dip, chest):
        return {'root': [0, 0, 0, 0, dip, 0], 'body': [8, 0, 0], 'chest': [0, chest, 0], 'head': [-6, -chest * 0.5, 0], 'right_arm': [-20, 0, 0],
                'left_arm': [-10, 10, -10], 'left_forearm': [-60, 0, 0]}
    run = Clip('run', 1.0, [(0, rn(0.6, -4), L), (0.25, rn(-0.2, 0), BZ), (0.5, rn(0.6, 4), L), (0.75, rn(-0.2, 0), BZ), (1.0, rn(0.6, -4), L)],
               mode='raw', base='carry', loop=True, markers={'stride_15': 0})
    # MA-9: heal or bless: a hand raised over an ally, the staff lifted, the light pooling (a scene pose)
    stand = P(base, fk((-24, -6, 8), -34, None, None))
    stand = sword(k, stand, (0.05, 1, 0.1))
    bl = held(P(torso(body=(-4, 0, 0), chest=(-6, 0, 0), head=(-10, 0, 0)), legs((0, 0), (0, 0), (2, -2), (6, 6)), fk(None, None, (-130, 10, -20), -20)), (-40, -6, 8), -40, (0.05, 1, 0.2))
    bless = Clip('bless', 2.6, [(0, stand, L), (0.5, bl, BZ), (1.2, add(bl, left_arm=[-6, 0, 0], root=[0, 0, 0, 0, -0.4, 0]), CR), (1.9, add(bl, left_arm=[4, 0, 0]), CR),
                                (2.6, bl, CR)], mode='whole')
    # MA-10: reels, the staff swung up across her as a ward
    def hit(name, s_):
        z = {'root': [0] * 6, 'body': [0, 0, 0], 'head': [0, 0, 0], 'right_arm': [0, 0, 0], 'right_forearm': [0, 0, 0]}
        a = {'root': [0, 0, 0, 0, 0.4, 1.0 * s_], 'body': [-8 * s_, 0, 0], 'head': [-10 * s_, 0, 0], 'right_arm': [-30, -20, 0], 'right_forearm': [-30, 0, 0]}
        return Clip(name, 1.0, [(0, z, L), (0.1, a, BZ), (0.4, a, CR), (0.7, mix(z, a, 0.3), CR), (1.0, z, L)], mode='raw')
    hits = [hit('hit_body', 1.0), hit('hit_head', 1.2), hit('hit_legs', 0.7)]
    # MA-11: sinks slowly (the robe folds at the hips, her knees held by it), the staff light fading, settling on her side
    rest = P(torso(), legs(), fk((-20, 0, 6), -20, (0, 0, 0), 0))
    sag = P(torso(drop=1.0, body=(10, 0, 0), chest=(6, 0, 0), head=(16, 0, 0)), legs((-6, -6)), fk((-10, 0, 10), -10, (0, 0, -6), -10))
    sit = P(torso(drop=6.5, fwd=-1.0, body=(30, 0, 4), chest=(10, 0, 0), head=(20, 0, 0)), legs((-80, -76), (10, 10), (6, -6)), fk((10, 0, 20), -10, (10, 0, -10), -10))
    down = k.grounded(P(torso(root=(0, 0, -84), drop=10, side=1, body=(10, 0, 0), chest=(6, 0, 0), head=(0, 0, -20)), legs((-40, -30), (10, 10)),
                        fk((-60, 0, 30), -20, (-20, 0, -20), -30)))
    death = Clip('death', 3.0, [(0, rest, L), (0.5, sag, BZ), (1.4, sit, BZ), (2.2, down, BZ), (3.0, down, L)], mode='whole')
    return k, stances + [bolt, burst, staffswing, alt1, alt2, walk, run] + hits + [bless, death]
