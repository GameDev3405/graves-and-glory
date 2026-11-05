"""The articulated combat rig: the troop biped with a waist, elbows, knees and ankles, in the style of Fresh Animations.

The game's troop rig (rig.py) has rigid limbs: an arm is one cube from shoulder to wrist, a leg one cube from hip to
foot, the torso one block. This rig keeps every one of its pivots and adds four kinds of joint:

  chest           the torso bends and twists at the waist (y 17.5); the head and both arms ride on it
  *_forearm       the elbows, hinged just behind the arm's centre (y 17.5, z +0.5), so a bent arm brings the fist
                  clear in front of the upper arm (the joint sleeve closes the point of the elbow)
  *_shin          the knees, hinged at the front of the leg (y 7.5, z -2.5) so the knee cop stays closed
  *_foot          the ankles (y 2.5), so a planted foot stays flat while the leg above it moves, or rolls onto its toes

Angles and offsets use the clips' convention (rig.py). A forearm's pitch is the elbow (0 straight, negative bends the
forearm forward), a shin's pitch is the knee (0 straight, positive bends the shin back); both are hinges, no yaw or roll.

The legs are not keyed by hand. Clips place the feet on the floor (where each foot stands, how far its toe turns out,
how high it lifts, how far its heel rises) and a solver bends the hip, knee and ankle to put it there every tick, so a
planted foot never slides while the body lunges, twists and crouches over it. The shield is aimed, not keyed: the
wrist is solved so the board faces where a clip says, whatever the arm does.

The armour is cut at the joints (`split_parts`): every box that crosses a joint becomes two boxes whose faces take the
matching rows of the original texture, so the set needs no repainting. The cut faces show the plate's edge row.
"""
import copy
import math

import numpy as np

from . import rig
from .rig import mc_euler, mc_rotation

WAIST_Y, ELBOW_Y, KNEE_Y, ANKLE_Y = 17.5, 17.5, 7.5, 2.5

# name -> (parent, pivot in Blockbench space); parents before children
BONES = {
    'root': (None, (0, 24, 0)),
    'body': ('root', (0, 12, 0)),                   # the hips
    'chest': ('body', (0, WAIST_Y, 0)),
    'head': ('chest', (0, 24, 0)),
    'right_arm': ('chest', (5, 22, 0)),
    'right_forearm': ('right_arm', (6, ELBOW_Y, 0.5)),
    'right_hand': ('right_forearm', (6, 14, 0)),
    'left_arm': ('chest', (-5, 22, 0)),
    'left_forearm': ('left_arm', (-6, ELBOW_Y, 0.5)),
    'left_hand': ('left_forearm', (-6, 14, 0)),
    'right_leg': ('root', (1.9, 12, 0)),
    'right_shin': ('right_leg', (1.9, KNEE_Y, -2.5)),
    'right_foot': ('right_shin', (1.9, ANKLE_Y, 0)),
    'left_leg': ('root', (-1.9, 12, 0)),
    'left_shin': ('left_leg', (-1.9, KNEE_Y, -2.5)),
    'left_foot': ('left_shin', (-1.9, ANKLE_Y, 0)),
    'cape': ('chest', (0, 24.4, 3.6)),
    'plume': ('head', (0, 34, 1)),
}
NEW_BONES = ['chest', 'right_forearm', 'left_forearm', 'right_shin', 'left_shin', 'right_foot', 'left_foot']
HINGES = {'right_forearm': (-150, 0), 'left_forearm': (-150, 0), 'right_shin': (0, 150), 'left_shin': (0, 150)}


def world(rot, pos, bones=None):
    return rig.world(rot, pos, bones or BONES)


def bones_with(follow):
    """The articulated rig with a unit's own follow bones (springs) instead of the knight's cape and plume:
    follow = {name: (parent, pivot)}."""
    out = {b: v for b, v in BONES.items() if b not in ('cape', 'plume')}
    out.update(follow)
    return out


# ---- cutting the armour at the joints ---------------------------------------------------

# a rigid bone's boxes are cut at these heights into these bones (top segment first)
SEGMENTS = {
    'body': ([WAIST_Y], ['chest', 'body']),
    'right_arm': ([ELBOW_Y], ['right_arm', 'right_forearm']),
    'left_arm': ([ELBOW_Y], ['left_arm', 'left_forearm']),
    'right_leg': ([KNEE_Y, ANKLE_Y], ['right_leg', 'right_shin', 'right_foot']),
    'left_leg': ([KNEE_Y, ANKLE_Y], ['left_leg', 'left_shin', 'left_foot']),
}
# pieces that ride one segment whole instead of being cut (name prefix -> segment index)
WHOLE = {'couter': 0, 'skirt': 0, 'poleyn': 1, 'sabaton': 2, 'toe': 2, 'pauldron': 0, 'wing': 0,
         'boot': 2, 'paw': 0}
SIDES = ('north', 'east', 'south', 'west')


def _cut(box, faces, c, density=1):
    """Cuts an unrotated box at height c (snapped to its texel rows): (upper box, faces), (lower box, faces), or None
    when the cut misses it. Side faces keep their rows; the new cut faces take the edge row of the front face."""
    y0, y1 = box.frm[1], box.frm[1] + box.size[1]
    r = round(y1 - c)                      # rows above the cut
    cy = y1 - r
    if r <= 0 or cy <= y0 + 1e-6:
        return None
    up, lo = copy.copy(box), copy.copy(box)
    up.name, lo.name = box.name + '~u', box.name + '~l'
    up.frm, up.size = [box.frm[0], cy, box.frm[2]], [box.size[0], y1 - cy, box.size[2]]
    lo.frm, lo.size = list(box.frm), [box.size[0], cy - y0, box.size[2]]
    up.origin = [up.frm[i] + up.size[i] / 2 for i in range(3)]
    lo.origin = [lo.frm[i] + lo.size[i] / 2 for i in range(3)]
    up.hide, lo.hide = set(box.hide) - {'down'}, set(box.hide) - {'up'}
    fu, fl = {}, {}
    for f, (tex, gl, (x, y, w, h), sh) in faces.items():
        if f in SIDES:
            fu[f] = (tex, gl, (x, y, w, r), sh)
            fl[f] = (tex, gl, (x, y + r, w, h - r), sh)
        elif f == 'up':
            fu[f] = faces[f]
        elif f == 'down':
            fl[f] = faces[f]
    edge = next((f for f in ('north', 'south', 'east', 'west') if f in faces), None)
    if edge:
        tex, gl, (x, y, w, h), sh = faces[edge]
        fu['down'] = (tex, gl, (x, y + r - 1, w, 1), sh)
        fl['up'] = (tex, gl, (x, y + r, w, 1), sh)
    else:
        up.hide.add('down')
        lo.hide.add('up')
    return (up, fu), (lo, fl)


def sleeve(box, faces, cy, reach=1.5, shrink=0.15):
    """A plate hidden inside a cut joint (a hair smaller than the box, `reach` px either side of the cut) that rides
    the lower bone and fills the gap the joint opens when it bends, so no seam or the body beneath shows. Its faces
    are the plate's row at the cut."""
    y0, y1 = box.frm[1], box.frm[1] + box.size[1]
    lo_y, hi_y = max(y0, cy - reach), min(y1, cy + reach)
    s = copy.copy(box)
    s.name = box.name + '~joint'
    s.frm, s.size = [box.frm[0], lo_y, box.frm[2]], [box.size[0], hi_y - lo_y, box.size[2]]
    s.origin = [s.frm[i] + s.size[i] / 2 for i in range(3)]
    s.inflate = box.inflate - shrink
    s.hide = set(box.hide) | {'up', 'down'}
    r = round(y1 - cy)
    sf = {}
    for f in SIDES:
        if f in faces:
            tex, gl, (x, y, w, h), sh = faces[f]
            sf[f] = (tex, gl, (x, y + min(r, h - 1), w, 1), sh)
    return s, sf


def split_parts(parts):
    """[(box, faces, bone, local)] on the rigid rig -> the same on the articulated rig (items and unlisted bones pass
    through)."""
    out = []
    for box, faces, bone, local in parts:
        if local is not None or bone not in SEGMENTS:
            out.append((box, faces, bone, local))
            continue
        cuts, segs = SEGMENTS[bone]
        whole = next((i for p, i in WHOLE.items() if box.name.startswith(p)), None)
        if whole is not None:
            out.append((box, faces, segs[whole], None))
            continue
        if any(box.rot):
            cyy = box.origin[1]
            out.append((box, faces, segs[sum(1 for c in cuts if cyy < c)], None))
            continue
        piece = (box, faces)
        for i, c in enumerate(cuts):
            res = _cut(piece[0], piece[1], c)
            if res is None:
                mid = piece[0].frm[1] + piece[0].size[1] / 2
                if mid > c:
                    out.append((piece[0], piece[1], segs[i], None))
                    piece = None
                    break
                continue
            (ub, uf), (lb, lf) = res
            out.append((ub, uf, segs[i], None))
            if c != ANKLE_Y:                     # the sabaton covers the ankle; every other joint gets a sleeve
                sb, sf = sleeve(piece[0], piece[1], ub.frm[1])
                out.append((sb, sf, segs[i + 1], None))
            piece = (lb, lf)
        if piece is not None:
            out.append((piece[0], piece[1], segs[sum(1 for c in cuts if piece[0].frm[1] + piece[0].size[1] / 2 < c)],
                        None))
    return out


# ---- interpolation for placed things (feet, shield aim) -----------------------------------

def pchip(keys, t):
    """Monotone cubic (Fritsch-Carlson) through [(t, v...)]: smooth through moving keys, exactly still between equal
    keys and never overshooting, so a planted foot holds without creeping."""
    if t <= keys[0][0]:
        return tuple(keys[0][1:])
    if t >= keys[-1][0]:
        return tuple(keys[-1][1:])
    i = max(j for j in range(len(keys) - 1) if keys[j][0] <= t)
    n = len(keys[0]) - 1
    out = []
    for c in range(1, n + 1):
        def slope(j):
            return (keys[j + 1][c] - keys[j][c]) / (keys[j + 1][0] - keys[j][0])

        def tangent(j):
            if j == 0 or j == len(keys) - 1:
                return 0.0
            a, b = slope(j - 1), slope(j)
            if a * b <= 0:
                return 0.0
            h0, h1 = keys[j][0] - keys[j - 1][0], keys[j + 1][0] - keys[j][0]
            w1, w2 = 2 * h1 + h0, h1 + 2 * h0
            return (w1 + w2) / (w1 / a + w2 / b)
        h = keys[i + 1][0] - keys[i][0]
        s = (t - keys[i][0]) / h
        p0, p1 = keys[i][c], keys[i + 1][c]
        m0, m1 = tangent(i) * h, tangent(i + 1) * h
        h00, h10, h01, h11 = 2 * s ** 3 - 3 * s ** 2 + 1, s ** 3 - 2 * s ** 2 + s, -2 * s ** 3 + 3 * s ** 2, s ** 3 - s ** 2
        out.append(h00 * p0 + h10 * m0 + h01 * p1 + h11 * m1)
    return tuple(out)


# ---- the feet ---------------------------------------------------------------------------

BALL = np.array([0.0, -ANKLE_Y, -4.5])        # the tip of the toe cap from the ankle (the sole is at y 0)
HEEL = np.array([0.0, -ANKLE_Y, 2.75])


def foot_target(side, foot):
    """A foot placement (out, z, lift, toe_out, heel) -> (ankle position, foot rotation) in the root's ground frame
    (Blockbench space, the entity's feet at y 0). out: distance of the ankle from the centre line, z: Blockbench z
    (negative is forward), lift: height of the sole off the floor, toe_out: degrees the toe turns away from the centre
    line, heel: degrees the heel rises with the toe cap kept down (negative lifts the toe on the heel)."""
    out, z, lift, toe_out, heel = foot
    sx = 1 if side == 'right' else -1
    Y = rig.ry(math.radians(-sx * toe_out))
    F = Y @ rig.rx(math.radians(-heel))
    ground = np.array([sx * out, ANKLE_Y, z])
    contact = BALL if heel >= 0 else HEEL
    pivot = ground + Y @ contact
    ankle = pivot - F @ contact + np.array([0.0, lift, 0.0])
    return ankle, F


def _local(piv, R):
    L = np.eye(4)
    L[:3, :3] = R
    L[:3, 3] = np.asarray(piv, float) - R @ np.asarray(piv, float)
    return L


def leg_chain(side, root_pos, hip, knee):
    """World matrices of a leg's thigh and shin (the root only translates)."""
    p = 'right_' if side == 'right' else 'left_'
    M0 = np.eye(4)
    M0[:3, 3] = (-root_pos[0], root_pos[1], root_pos[2])
    T = M0 @ _local(BONES[p + 'leg'][1], mc_rotation(*hip))
    S = T @ _local(BONES[p + 'shin'][1], mc_rotation(knee, 0, 0))
    return T, S


def solve_leg(side, root_pos, foot, guess, knee_follow=0.75):
    """Hip (pitch, yaw, roll), knee and foot angles that put the ankle and sole where `foot` says. The hip's yaw follows
    the toe so the knee tracks over the foot; pitch, roll and the knee are solved by damped Gauss-Newton from `guess`
    (last tick's answer, so the solution moves smoothly). Returns (hip, knee, foot angles, ankle error in px)."""
    p = 'right_' if side == 'right' else 'left_'
    sx = 1 if side == 'right' else -1
    target, F = foot_target(side, foot)
    yaw = sx * foot[3] * knee_follow           # the clips' yaw: a right leg's positive yaw turns its toe out
    ankle_local = np.array(BONES[p + 'foot'][1], float)
    x = np.array(guess, float)                 # pitch, roll, knee

    def f(v):
        T, S = leg_chain(side, root_pos, (v[0], yaw, v[1]), v[2])
        return rig.apply(S, ankle_local) - target
    lo, hi = np.array([-110.0, -40.0, 0.0]), np.array([80.0, 40.0, 150.0])
    for _ in range(30):
        e = f(x)
        if np.linalg.norm(e) < 1e-5:
            break
        J = np.zeros((3, 3))
        for k in range(3):
            d = np.zeros(3)
            d[k] = 1e-3
            J[:, k] = (f(x + d) - e) / 1e-3
        lam = 1e-3
        dx = np.linalg.solve(J.T @ J + lam * np.eye(3), -J.T @ e)
        x = np.clip(x + dx, lo, hi)
    err = float(np.linalg.norm(f(x)))
    T, S = leg_chain(side, root_pos, (x[0], yaw, x[1]), x[2])
    Fl = S[:3, :3].T @ F
    return (float(x[0]), float(yaw), float(x[1])), float(x[2]), mc_euler(Fl), err


def foot_points(pose, side):
    """The sole's corners and the toe cap's tip in the world for a posed rig (for the ground checks)."""
    p = 'right_' if side == 'right' else 'left_'
    sx = 1 if side == 'right' else -1
    pts = []
    for x, z in ((-0.85, -4.5), (4.65, -4.5), (-0.85, 2.75), (4.65, 2.75)):
        pts.append(rig.apply(pose[p + 'foot'], (x if sx > 0 else -x, 0.0, z)))
    return pts


# ---- the shield's wrist -----------------------------------------------------------------

def aim_hand(forearm_world, aim, item_local):
    """The wrist angles that make a held item sit as it does at rest turned by `aim` (pitch, yaw, roll in the clips'
    convention, relative to the root, which only translates): hand = forearm^-1 . aim . rest item . item^-1, so
    aim (0, 0, 0) holds a shield upright facing forward."""
    D = mc_rotation(*aim) @ item_local[:3, :3]
    return mc_euler(forearm_world[:3, :3].T @ D @ item_local[:3, :3].T)


# ---- aiming the arms ----------------------------------------------------------------------

def _unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


def frame_from(down, front):
    """The rotation that turns a hanging limb (pointing -y, its front -z) to point along `down` with its front toward
    `front` (made perpendicular)."""
    d = _unit(down)
    h = np.asarray(front, float)
    w = h - np.dot(h, d) * d
    if np.linalg.norm(w) < 1e-6:
        w = np.array([0.0, 0.0, -1.0]) - d[2] * d
    w = _unit(w)
    y, z = -d, -w
    return np.column_stack([np.cross(y, z), y, z])


def closest_euler(R, prev=None):
    """mc_euler with the branch (p, y, r) or (p + 180, 180 - y, r + 180) and the turn nearest `prev`, so keys never
    flip."""
    p, y, r = mc_euler(R)
    cands = [(p, y, r), (p + 180, 180 - y, r + 180)]
    if prev is None:
        return cands[0]
    best = None
    for c in cands:
        c = [v + 360 * round((pv - v) / 360) for v, pv in zip(c, prev)]
        dist = sum((v - pv) ** 2 for v, pv in zip(c, prev))
        if best is None or dist < best[0]:
            best = (dist, tuple(float(v) for v in c))
    return best[1]


def aim_arm(upper, fore, blade=None, twist=0.0, prev=None):
    """Arm, elbow and wrist angles from directions in the chest's frame (Blockbench axes: +x the troop's right, +y up,
    -z forward): `upper` from shoulder to elbow, `fore` from elbow to fist, `blade` along a held weapon. The grip is
    natural: the blade's flats stay square to the forearm's sides as a hand holds a sword (the wrist bends, it does not
    roll), turned `twist` degrees about the blade. That also keeps the wrist's angles far from gimbal lock. prev: the
    last pose's angles {bone: angles} for continuity."""
    prev = prev or {}
    u, f = _unit(upper), _unit(fore)
    # the elbow bends toward the forearm; as the arm straightens that direction fades, so a little of the blade (or
    # forward) steadies it and the hinge never spins
    hint = np.asarray(blade if blade is not None else (0, 0, -1), float)
    hint = hint - np.dot(hint, u) * u
    bend = f - np.dot(f, u) * u
    w = bend + 0.12 * hint
    if prev.get('bend') is not None:                       # nearly straight: keep bending the way it was
        pw = np.asarray(prev['bend'], float)
        pw = pw - np.dot(pw, u) * u
        w = w + max(0.0, 0.35 - np.linalg.norm(bend)) * 4 * pw
    Ra = frame_from(u, w)
    elbow = max(-145.0, -math.degrees(math.acos(max(-1.0, min(1.0, float(np.dot(u, f)))))))   # elbows stop at 145
    out = {'arm': closest_euler(Ra, prev.get('arm')), 'forearm': (elbow, 0.0, 0.0), 'bend': -Ra[:, 2]}
    if blade is not None:
        b = _unit(blade)
        Rf = Ra @ mc_rotation(elbow, 0, 0)
        side = Rf[:, 0]                                    # the forearm's own x: where the flats face at rest
        x = side - np.dot(side, b) * b
        if np.linalg.norm(x) < 0.2:                        # the blade lies along that axis: lean on the forearm's y
            x = x + 0.5 * (Rf[:, 1] - np.dot(Rf[:, 1], b) * b)
        x = _unit(x)
        if twist:
            c, s_ = math.cos(math.radians(twist)), math.sin(math.radians(twist))
            x = c * x + s_ * np.cross(-b, x)
        z = -b
        y = np.cross(z, x)
        H = np.column_stack([x, y, z])
        out['hand'] = closest_euler(Rf.T @ H, prev.get('hand'))
    return out


# ---- reaching: an arm that puts its hand somewhere ----------------------------------------

def _chain(side, chest_M, arm, elbow, bones=None):
    """World matrices of an arm and its forearm from the chest's world matrix."""
    bones = bones or BONES
    p = side + '_'
    A = chest_M @ _local(bones[p + 'arm'][1], mc_rotation(*arm))
    F = A @ _local(bones[p + 'forearm'][1], mc_rotation(elbow, 0, 0))
    return A, F


def reach_arm(side, chest_M, target, pole, guess, bones=None, pole_weight=0.35, fore=None, fore_weight=0.6,
              stay=0.3, prev_swivel=None, min_bend=15.0, twist=0.25):
    """Aims (upper, fore) that put the hand's pivot at `target` (world), with the elbow drawn toward `pole` (a world
    direction from the shoulder). Damped Gauss-Newton on small turns of last tick's two directions (`guess`), so the
    arm moves continuously and the solve has no poles (yaw and pitch angles would, with an arm hanging straight down). `fore` (a world direction, optional) pulls the forearm toward
    it with `fore_weight`: a bow arm's forearm points down the shot and a draw arm's lies along the arrow, so the wrist
    stays nearly straight. `stay` holds the solution near last tick's: near full stretch the two sides an elbow can
    bend score almost alike, and without it the solve hops between them tick to tick. `min_bend` (degrees) keeps the
    elbow off that singularity. Returns (upper, fore, error in px, None)."""
    bones = bones or BONES
    p = side + '_'
    hand_piv = np.array(bones[p + 'hand'][1], float)
    elbow_piv = np.array(bones[p + 'forearm'][1], float)
    shoulder = rig.apply(chest_M @ _local(bones[p + 'arm'][1], np.eye(3)), bones[p + 'arm'][1])
    Rc = chest_M[:3, :3]
    pole = _unit(Rc.T @ np.asarray(pole, float))          # in the chest's frame, like the aims

    def basis(v):                # two directions square to v: the plane a small turn of v moves in
        v = _unit(v)
        a = np.array([1.0, 0, 0]) if abs(v[0]) < 0.9 else np.array([0, 1.0, 0])
        t1 = _unit(np.cross(v, a))
        return v, t1, np.cross(v, t1)
    U0, F0 = basis(guess[0]), basis(guess[1])

    def to_vecs(v):              # small turns of last tick's directions: no poles anywhere, unlike yaw and pitch
        return (_unit(U0[0] + v[0] * U0[1] + v[1] * U0[2]), _unit(F0[0] + v[2] * F0[1] + v[3] * F0[2]))
    x = np.zeros(4)

    def bend_dir(u, f):          # which way the forearm leaves the upper arm's line: it sets the arm's twist
        b = f - np.dot(f, u) * u
        return b / np.linalg.norm(b) if np.linalg.norm(b) > 1e-6 else b
    b0 = bend_dir(U0[0], F0[0])

    def resid(v):
        u, f = to_vecs(v)
        a = aim_arm(u, f)
        A, F = _chain(side, chest_M, a['arm'], a['forearm'][0], bones)
        hand = rig.apply(F, hand_piv)
        elbow = rig.apply(A, elbow_piv)
        mid = (shoulder + hand) / 2
        want = Rc @ pole
        e = elbow - mid
        e = e - np.dot(e, _unit(hand - shoulder)) * _unit(hand - shoulder)
        side_err = (_unit(e) if np.linalg.norm(e) > 1e-6 else e) - want
        r = [hand - target, pole_weight * side_err]
        if fore is not None:
            r.append(fore_weight * (_unit(hand - elbow) - _unit(fore)))
        r.append(stay * v)
        r.append(twist * (bend_dir(u, f) - b0))            # the twist turns smoothly, even near a straight arm
        bend = math.degrees(math.acos(max(-1.0, min(1.0, float(np.dot(u, f))))))
        r.append([0.08 * max(0.0, min_bend - bend)])       # a soft floor: an elbow never locks straight
        return np.concatenate(r)
    for it in range(40):
        r = resid(x)
        J = np.zeros((len(r), 4))
        for k in range(4):
            d = np.zeros(4)
            d[k] = 1e-4
            J[:, k] = (resid(x + d) - r) / 1e-4
        dx = np.linalg.solve(J.T @ J + 1e-3 * np.eye(4), -J.T @ r)
        x = x + np.clip(dx, -0.3, 0.3)
        if np.linalg.norm(dx) < 1e-5:
            break
    r = resid(x)
    u, f = to_vecs(x)
    return u, f, float(np.linalg.norm(r[:3])), None
