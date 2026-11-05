"""The troop biped rig for animation previews, matching the game's clips (TroopClips/BipedClips) exactly.

Bones and pivots are art/rigs/biped.json's, written in Blockbench space (y up, feet at 0, front -z, the troop's right
at +x): root (at the neck, 24 px up), body (the hips) carrying the head, the arms and the hands, and the legs from the
root. Two follow bones hang from them for secondary motion: the cape (from the body) and the plume (from the head).

Angles are the clips' convention (Minecraft's model space, degrees): an arm pitch of -90 points the arm forward, a
positive body pitch leans forward, a positive head pitch looks down; a right arm's positive yaw swings it out to the
troop's right, a left arm's positive yaw swings it across to the right; a positive body yaw turns the troop to its right.
Offsets are model pixels with y up and x the model's (the troop's left), as Blockbench writes them for the game.
Rotations apply as Minecraft's ModelPart does (roll, then yaw, then pitch, outermost first), and keyframes interpolate
with Minecraft's own Catmull-Rom (Transformation.Interpolations.CUBIC), so the preview moves as the game would.
"""
import math

import numpy as np

# name -> (parent, pivot in Blockbench space)
BONES = {
    'root': (None, (0, 24, 0)),
    'body': ('root', (0, 12, 0)),
    'head': ('body', (0, 24, 0)),
    'right_arm': ('body', (5, 22, 0)),
    'right_hand': ('right_arm', (6, 14, 0)),
    'left_arm': ('body', (-5, 22, 0)),
    'left_hand': ('left_arm', (-6, 14, 0)),
    'right_leg': ('root', (1.9, 12, 0)),
    'left_leg': ('root', (-1.9, 12, 0)),
    'cape': ('body', (0, 24.4, 3.6)),
    'plume': ('head', (0, 34, 1)),
}
ORDER = list(BONES)
GAME_BONES = ['root', 'body', 'head', 'right_arm', 'right_hand', 'left_arm', 'left_hand', 'right_leg', 'left_leg']


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def mc_rotation(pitch, yaw, roll):
    """A clip rotation (degrees, Minecraft model space) as a Blockbench-space matrix. Model space is Blockbench's with x
    and y negated, so S R S with S = diag(-1, -1, 1): pitch and yaw change sign, roll keeps it."""
    r = math.radians
    return rz(r(roll)) @ ry(-r(yaw)) @ rx(-r(pitch))


def catmull(d, p0, p1, p2, p3):
    """Minecraft's MathHelper.catmullRom."""
    return 0.5 * (2 * p1 + (p2 - p0) * d + (2 * p0 - 5 * p1 + 4 * p2 - p3) * d * d + (3 * p1 - p0 - 3 * p2 + p3) * d * d * d)


def sample(keys, t):
    """keys: [(time, a, b, c)] sorted. Minecraft's AnimationHelper: the last key at or before t starts the span, the next
    ends it, the curve is Catmull-Rom over the neighbours (clamped at the ends); past the last key, the last value."""
    if not keys:
        return (0.0, 0.0, 0.0)
    start = 0
    for i, k in enumerate(keys):
        if k[0] <= t + 1e-9:
            start = i
    end = min(len(keys) - 1, start + 1)
    if end == start:
        return tuple(keys[start][1:])
    t0, t1 = keys[start][0], keys[end][0]
    d = max(0.0, min(1.0, (t - t0) / (t1 - t0))) if t1 > t0 else 0.0
    p0 = keys[max(0, start - 1)]
    p3 = keys[min(len(keys) - 1, end + 1)]
    return tuple(catmull(d, p0[i], keys[start][i], keys[end][i], p3[i]) for i in (1, 2, 3))


class Clip:
    """rot: bone -> [(t, pitch, yaw, roll)]; pos: bone -> [(t, x, y, z)] (model x, y up, z)."""

    def __init__(self, name, length, loop):
        self.name, self.length, self.loop = name, length, loop
        self.rot, self.pos = {}, {}

    def at(self, t):
        if self.loop:
            t = t % self.length
        rot = {b: sample(k, t) for b, k in self.rot.items()}
        pos = {b: sample(k, t) for b, k in self.pos.items()}
        return rot, pos


def world(rot, pos):
    """Bone -> 4x4 Blockbench-space matrix for a pose (rot, pos as Clip.at returns them)."""
    out = {}
    for b in ORDER:
        parent, piv = BONES[b]
        piv = np.array(piv, float)
        p = pos.get(b, (0, 0, 0))
        off = np.array([-p[0], p[1], p[2]])          # model x is Blockbench -x
        R = mc_rotation(*rot.get(b, (0, 0, 0)))
        L = np.eye(4)
        L[:3, :3] = R
        L[:3, 3] = piv + off - R @ piv
        out[b] = (out[parent] @ L) if parent else L
    return out


def apply(M, p):
    return M[:3, :3] @ np.asarray(p, float) + M[:3, 3]


# ---- the solvers that bake keys --------------------------------------------------------

FOOT_CORNERS = [(x, 0.0, z) for x in (-0.85, 4.65) for z in (-4.5, 2.75)]   # a sabatoned foot, toe cap included


def ground_root_y(rot, pos):
    """How far the root must drop (negative) or rise so the lowest point of either foot touches y 0."""
    pose = world(rot, dict(pos, root=(pos.get('root', (0, 0, 0))[0], 0, pos.get('root', (0, 0, 0))[2])))
    low = 1e9
    for leg, sx in (('right_leg', 1), ('left_leg', -1)):
        for x, y, z in FOOT_CORNERS:
            low = min(low, apply(pose[leg], (x * sx if sx > 0 else -x, y, z))[1])
    return -low


def spring_follow(clip, bone, parent_bone, axis_weights, stiffness=90.0, damping=11.0, rest=(0, 0, 0),
                  clamp=None, dt=1 / 240, step=0.05, passes=2):
    """Bakes a hanging part's swing (the cape from the body, the plume from the head) as rotation keys every `step` s.

    The part is a damped pendulum: it wants to hang straight down in the world, so its target pitch and roll cancel its
    parent's lean, and the parent's angular velocity and the root's acceleration kick it. axis_weights scales the
    reaction on pitch and roll; clamp(t, pitch, roll) can stop it passing through the body or legs."""
    def parent_angles(t):
        pose = world(*clip.at(t))
        Rp = pose[parent_bone][:3, :3]
        down = Rp.T @ np.array([0, -1.0, 0])                 # world down in the parent's frame
        pitch = math.degrees(math.atan2(down[2], -down[1]))   # positive: the world's down lies behind the parent
        roll = math.degrees(math.atan2(down[0], -down[1]))     # Rz(roll) swings the hanging end to +x
        root = pose['root'][:3, 3]
        return pitch, roll, root
    th = np.array(rest[:2], float)
    om = np.zeros(2)
    n = int(round(clip.length / dt))
    keys = []
    prev_root = None
    prev_vel = np.zeros(3)
    for p in range(passes):
        for i in range(n + 1):
            t = i * dt
            pp, pr, root = parent_angles(t)
            vel = np.zeros(3) if prev_root is None else (root - prev_root) / dt
            acc = (vel - prev_vel) / dt
            prev_root, prev_vel = root, vel
            target = np.array([pp + rest[0], pr + rest[1]])
            # the root's forward acceleration (-z) throws a cape back (+pitch); sideways throws it the other way
            kick = np.array([acc[2] * -0.35, acc[0] * -0.35]) * np.array(axis_weights)
            a = stiffness * (target - th) - damping * om + kick
            om += a * dt
            th += om * dt
            if clamp:
                th[0], th[1] = clamp(t, th[0], th[1])
            if p == passes - 1 and abs((t / step) - round(t / step)) < 1e-6:
                keys.append((round(t, 4), float(th[0]), 0.0, float(th[1])))
    clip.rot[bone] = keys
    return keys
