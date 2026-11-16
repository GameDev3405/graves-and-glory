#!/usr/bin/env python3
"""The forge Blacksmith's clips (lobby NPC spec 4.1): `idle`, his work loop, and `interact`, played once when a forge opens.

    python art/camp_npc/blacksmith/build_blacksmith_anims.py [--report] [out dir]

Writes assets/gravesandglory/animations/camp_npc/blacksmith.animation.json (generated: change this builder, never the file).
Run build_blacksmith.py first: the poses are solved on its geo.

idle (7 s loop): a two-beat strike on the billet (a heavy blow with a full-body hip drop, then a light tap) at 1.2 s and
again at 4.2 s; after the second he rests the hammer on the anvil and wipes his brow with the graft forearm.
interact (3 s, once): the hammer goes up onto his right shoulder, he squares up to the player and leans on the anvil with
his left fist, nods, and returns to the loop's first pose (GeoCampNpc plays idle from tick 0 after it).

How it is made. The body (hips, torso, head) is keyed by hand below. Everything that must touch something is solved on
the rig every tick (npc_fk): the feet stay planted while the hips drop and turn, the hammer is driven by its grip
position and haft direction so its face lands flat on the billet, and the tongs hold the billet's cold end until the brow
wipe. The beard, braid and apron trail the body. Keys are baked one per tick, linear.
The root is never keyed: the anvil hangs from it and must not move. No effects (sparks, glow, steam) and no head
tracking: those need renderer code that does not exist yet.
"""
import json, math, os, sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
sys.path.insert(0, os.path.join(HERE, ".."))
import npc_fk                                           # noqa: E402

ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "gravesandglory")
GEO = os.path.join(ASSETS, "geo", "camp_npc", "blacksmith.geo.json")
OUT = os.path.join(ASSETS, "animations", "camp_npc", "blacksmith.animation.json")
RIG = npc_fk.Rig(GEO)

V = lambda *a: np.array(a, float)
def unit(v): v = np.asarray(v, float); return v / np.linalg.norm(v)

# ------------------------------------------------------------------------------------------------ the set (file frame, px)
STRIKE = V(-1.0, 13.0, -15.0)            # the billet's hot end, where the hammer's face lands
BILLET_COLD = V(3.5, 12.5, -15.0)        # where the tongs' jaws hold it
ANVIL_TOP = 12.0
HAMMER_GRIP, HAMMER_FACE = V(-12.5, 7, 0), V(-12.5, 3.5, -8.5)     # on the hammer bone, at rest
TONGS_GRIP, TONGS_TIP = V(12.5, 7, 0), V(12.5, 7, -14.0)           # on the tongs bone, at rest
FEET = {"right": (V(-6.5, 0, 4.5), 20.0), "left": (V(6.0, 0, -1.0), -10.0)}   # planted sole centres and toe-out (deg, + to his right)
SOLE = {"right": V(-4, 0, -1), "left": V(4, 0, -1)}

# ------------------------------------------------------------------------------------------------ interpolation
EASE = {"io": lambda f: f * f * (3 - 2 * f), "in": lambda f: f ** 2.2, "out": lambda f: 1 - (1 - f) ** 2.2, "lin": lambda f: f}


def seg(keys, t):
    """(key before, key after, eased fraction) on a sorted key list [(tick, value, ease into this key)]."""
    if t <= keys[0][0]: return keys[0], keys[0], 0.0
    for a, b in zip(keys, keys[1:]):
        if t <= b[0]: return a, b, EASE[b[2]]((t - a[0]) / (b[0] - a[0]))
    return keys[-1], keys[-1], 0.0


def slerp(a, b, f):
    a, b = unit(a), unit(b)
    w = math.acos(max(-1.0, min(1.0, float(a @ b))))
    if w < 1e-4: return a
    return (math.sin((1 - f) * w) * a + math.sin(f * w) * b) / math.sin(w)


# ------------------------------------------------------------------------------------------------ keys
# body: hips position (x to his left, y up, z back), hips yaw, torso pitch (+ forward), yaw (+ to his right), roll (+ to his
# left), head pitch (+ down), yaw
def body(hx=0.0, hy=-1.5, hz=3.0, hyaw=10.0, tp=12.0, ty=6.0, tr=0.0, np_=6.0, ny=-14.0, nr=0.0):
    return V(hx, hy, hz, hyaw, tp, ty, tr, np_, ny, nr)


READY = body()
def tool(grip, d, s=(1, 0, 0), up=None, frame=None, world_aim=False, aim=1.0, fk=False, guess=None):
    """An arm target: the grip (fist centre), the tool's direction out of the fist, and its side axis s (the tool's own up is
    s x d) or an up hint; in the world, or carried by `frame` (a bone, the point and directions given as at rest).
    world_aim: only the grip rides the frame, the directions stay in the world. aim: how hard the directions are held
    (1 = a tool at work; low = the hand matters, the tool just hangs in it). fk: the arm swings to this key from the key
    before by its joints (a big free move, where a target dragged through the air would flip the elbow); guess: where the
    solve of this key starts, as arm angles or as a tick of the clip whose solved arm it starts from."""
    d = unit(d)
    if up is not None: s = np.cross(d, unit(up))
    return {"grip": V(*grip), "d": d, "s": unit(s), "frame": frame, "world_aim": world_aim, "aim": aim, "fk": fk, "guess": guess}


def strike_at(d, lift=0.0):
    """The hammer target with its face flat on the billet (lift px above it), the haft pointing along d (level)."""
    d = unit(d)
    face = STRIKE + V(0, lift, 0)
    centre = face + V(0, 3.5, 0)                      # the head's centre, on the haft's axis
    return tool(centre - 8.5 * d, d)


def tongs_on_billet(d=(-0.62, -0.05, -0.78)):
    d = unit(d)
    return tool(BILLET_COLD - 14.0 * d, d, up=(0, 1, 0))


H_READY = tool((-11.4, 15.6, -6.4), (0.52, 0.12, -0.85))            # held low over the anvil at his right, clear of his face
T_GRIP = tongs_on_billet()
T_LIFT = dict(T_GRIP, grip=T_GRIP["grip"] + V(0.5, 3.0, 1.0))        # the tongs lifted clear of the anvil
WIPE_ARM = [7.0, -22.0, -157.0, -106.0, 6.0, 26.0, 2.0, 18.0, 40.0]   # where the brow wipe's solve starts: elbow lifted high, out to his left
BLOW, TAP = (0.82, 0, -0.57), (0.84, 0, -0.54)                       # the haft comes in from his right, so the fist clears his beard
H_REST = tool((-10.6, 15.5, -8.0), (0.66, 0.02, -0.75))             # the hammer's head left standing on the anvil at his right


def strike_body(c):
    """Body keys of one two-beat strike starting at tick c (back at READY by c + 47)."""
    return [
        (c + 6, body(hy=-2.1, tp=15, np_=9), "io"),                                     # anticipation: a small dip
        (c + 17, body(hx=-0.8, hy=-0.9, hz=3.6, hyaw=16, tp=1, ty=18, tr=-4, np_=8, ny=-26), "io"),   # up: legs straighten, chest opens right
        (c + 20, body(hx=-0.9, hy=-0.8, hz=3.7, hyaw=17, tp=-1, ty=20, tr=-4, np_=9, ny=-28), "io"),  # the hang at the top
        (c + 24, body(hx=0.6, hy=-3.3, hz=3.0, hyaw=5, tp=21, ty=-9, tr=3, np_=6, ny=-4), "in"),     # the blow: hips drop, chest drives down and through
        (c + 25.5, body(hx=0.7, hy=-3.8, hz=2.9, hyaw=4, tp=23.5, ty=-10, tr=3, np_=9, ny=-4), "out"),  # the squash
        (c + 30, body(hx=0.1, hy=-1.3, hz=3.1, hyaw=9, tp=10, ty=3, tr=-1, np_=5, ny=-12), "io"),    # the rebound lifts him nearly upright
        (c + 34, body(hx=0.5, hy=-2.7, hz=2.9, hyaw=6, tp=19, ty=-5, tr=2, np_=8, ny=-7), "in"),     # the tap, lighter
        (c + 35.5, body(hx=0.5, hy=-2.9, hz=2.9, hyaw=6, tp=20, ty=-5, tr=2, np_=9, ny=-7), "out"),
        (c + 47, READY, "io"),
    ]


def strike_hammer(c):
    return [
        (c + 6, tool((-9.4, 16.3, -8.2), (0.80, 0.05, -0.60)), "io"),                    # he lays the face near the mark
        (c + 12, tool((-13.2, 23.0, -1.5), (0.30, 0.80, -0.52)), "in"),                  # up past the shoulder
        (c + 17, tool((-14.0, 28.0, 5.0), (-0.10, 0.60, 0.79)), "out"),                  # over and behind it
        (c + 20, tool((-14.0, 28.5, 5.6), (-0.12, 0.53, 0.84)), "io"),                   # it hangs
        (c + 22.6, tool((-12.6, 25.0, 0.0), (0.34, 0.80, -0.50)), "in"),                 # the arc down, late and fast
        (c + 24, strike_at(BLOW), "lin"),                                                # the blow lands
        (c + 25.5, strike_at(BLOW), "lin"),
        (c + 30, tool((-11.5, 21.5, -7.0), (0.45, 0.66, -0.60)), "out"),                 # it bounces high off the iron, beside his face
        (c + 34, strike_at(TAP), "in"),                                                  # the tap
        (c + 35.5, strike_at(TAP), "lin"),
    ]


def idle_keys():
    B = [(0, READY, "io")] + strike_body(0) + [(60, READY, "io")] + strike_body(60)
    B += [(109, body(hx=-1.5, hy=-1.2, hz=3.1, hyaw=8, tp=10, ty=2, tr=-3, np_=10, ny=-16, nr=3), "io"),      # his weight goes right as the left arm comes up
          (113, body(hx=-1.4, hy=-1.0, hz=3.2, hyaw=7, tp=9, ty=-2, tr=0, np_=17, ny=-20, nr=8), "io"),      # he straightens and dips his brow to the glove
          (119, body(hx=-1.5, hy=-0.9, hz=3.2, hyaw=7, tp=8, ty=-1, tr=-1, np_=15, ny=-9, nr=5), "io"),       # the head rolls against the wipe
          (125, body(hx=-1.4, hy=-1.0, hz=3.2, hyaw=7, tp=9, ty=-2, tr=0, np_=17, ny=-19, nr=8), "io"),
          (130, body(hx=-1.5, hy=-1.2, hz=3.1, hyaw=8, tp=10, ty=2, tr=-3, np_=9, ny=-15, nr=3), "io"),
          (135, body(hx=-0.4, hy=-1.4, hz=3.0, hyaw=9, tp=11, ty=5, tr=0, np_=6, ny=-13), "io"),
          (140, READY, "io")]
    H = [(0, H_READY, "io")] + strike_hammer(0) + [(47, H_READY, "io"), (60, H_READY, "io")] + strike_hammer(60)
    H += [(105, H_REST, "io"), (131, H_REST, "io"), (140, H_READY, "io")]                # the hammer stands on the anvil while he wipes
    # His arms are short: the back of the graft's glove reaches the brow because the head dips to meet it. The elbow stays
    # out to his left and the tongs hang down and forward, so neither covers his face.
    wipe = lambda x, y, z, **k: tool((x, y, z), (0.74, -0.52, -0.42), up=(0.47, -0.05, 0.88), frame="head", world_aim=True, aim=0.2, **k)
    T = [(0, T_GRIP, "io"), (102, T_GRIP, "io"), (105, T_LIFT, "io"),      # the tongs come up off the billet first
         (113, wipe(8.2, 31.4, -9.0, fk=True, guess=WIPE_ARM), "io"),    # the arm swings up: the glove comes to the sweat rag,
         (119, wipe(2.2, 32.6, -9.0), "io"),             # drags across it, above his eyes,
         (125, wipe(7.6, 31.4, -9.0), "io"),             # and back
         (133, dict(T_LIFT, fk=True, guess=105), "io"), (137, T_GRIP, "io"), (140, T_GRIP, "io")]
    return B, H, T


def interact_keys():
    # the lean: his weight goes onto the left fist, so the shoulders tip and the head comes forward and up to the player
    lean = body(hx=1.2, hy=-2.8, hz=0.8, hyaw=-3, tp=27, ty=-5, tr=5, np_=-17, ny=3, nr=-3)
    B = [(0, READY, "io"),
         (4, body(hy=-2.0, tp=14, np_=7, ny=-8), "io"),
         (10, body(hy=-0.5, hz=2.8, hyaw=3, tp=5, ty=2, tr=-2, np_=3, ny=0), "io"),                  # up and square to the player
         (18, lean, "io"),                                                                          # onto the anvil
         (26, body(hx=1.2, hy=-2.7, hz=0.8, hyaw=-3, tp=26, ty=-5, tr=5, np_=-17, ny=3, nr=-3), "io"),
         (30, body(hx=1.2, hy=-2.8, hz=0.8, hyaw=-3, tp=27, ty=-5, tr=5, np_=-6, ny=3, nr=-3), "io"),  # a nod
         (35, lean, "io"),
         (44, body(hx=1.2, hy=-2.7, hz=0.9, hyaw=-3, tp=26, ty=-5, tr=5, np_=-16, ny=3, nr=-3), "io"),
         (60, READY, "io")]
    shoulder = tool((-10.6, 25.4, -8.6), (-0.12, 0.33, 0.94), frame="torso")                        # the haft across his right shoulder
    swing = tool((-14.0, 20.5, -7.0), (0.10, 0.80, -0.59), frame="torso")                           # the way up and down, wide of his head
    H = [(0, H_READY, "io"), (3, tool((-11.2, 15.2, -6.6), (0.50, 0.10, -0.86)), "io"), (7, swing, "io"),
         (11, shoulder, "io"), (46, shoulder, "io"), (52, swing, "io"), (60, H_READY, "io")]
    plant = tool((10.2, ANVIL_TOP + 2.4, -14.2), (0.68, -0.73, 0.0), up=(0.73, 0.68, 0.0))           # the fist on the anvil's horn, the tongs hanging from it
    T = [(0, T_GRIP, "io"), (4, T_GRIP, "io"), (7, T_LIFT, "io"), (18, dict(plant, fk=True), "io"), (45, plant, "io"),
         (55, dict(T_LIFT, fk=True, guess=7), "io"), (58, T_GRIP, "io"), (60, T_GRIP, "io")]
    return B, H, T


# ------------------------------------------------------------------------------------------------ solving one tick
# shoulder x3, elbow, wrist x3, and the tool turning in the fist (its side axis and its yaw): a smith's haft is seldom
# square to his forearm, and a cube fist hides the difference
ARM_BOUNDS = {"right": [(-250, 90), (-110, 110), (-40, 175), (-150, -2), (-45, 45), (-45, 45), (-45, 45), (-80, 80), (-40, 40)],
              "left": [(-250, 90), (-110, 110), (-175, 40), (-150, -2), (-45, 45), (-45, 45), (-45, 45), (-80, 80), (-40, 40)]}


def target_now(M, key):
    if key["frame"] is None: return key["grip"], key["d"], key["s"]
    f = key["frame"]
    if key["world_aim"]: return RIG.pt(M, f, key["grip"]), key["d"], key["s"]
    return RIG.pt(M, f, key["grip"]), RIG.vec(M, f, key["d"]), RIG.vec(M, f, key["s"])


def aim_now(keys, t):
    a, b, f = seg(keys, t)
    return a[1]["aim"] + (b[1]["aim"] - a[1]["aim"]) * f


def arm_target(M, keys, t):
    a, b, f = seg(keys, t)
    ga, da, sa = target_now(M, a[1]); gb, db, sb = target_now(M, b[1])
    d = slerp(da, db, f); s = slerp(sa, sb, f)
    return ga + (gb - ga) * f, d, unit(np.cross(s, d))


def arm_free(side, tool_bone):
    return [(side + "_arm", 0), (side + "_arm", 1), (side + "_arm", 2), (side + "_forearm", 0),
            (side + "_hand", 0), (side + "_hand", 1), (side + "_hand", 2), (tool_bone, 0), (tool_bone, 1)]


def get_arm(pose, side, tool_bone):
    return [pose[b]["rotation"][a] for b, a in arm_free(side, tool_bone)]


def set_arm(pose, side, tool_bone, vals):
    for (b, a), val in zip(arm_free(side, tool_bone), vals):
        r = list(pose.setdefault(b, {}).get("rotation", [0.0, 0.0, 0.0])); r[a] = float(val); pose[b]["rotation"] = r


def swinging(keys, t):
    """Is the arm between two keys on a joint swing at tick t (so it has no target to be measured against)?"""
    a, b, f = seg(keys, t)
    return bool(b[1]["fk"]) and a is not b and t < b[0]


def solve_arm(pose, side, tool_bone, grip_pt, grip, d, u, guess, aim=1.0):
    free = arm_free(side, tool_bone)

    def res(M):
        return np.concatenate([(RIG.pt(M, tool_bone, grip_pt) - grip) * 3.0,
                               (RIG.vec(M, tool_bone, (0, 0, -1)) - d) * 14.0 * aim,
                               (RIG.vec(M, tool_bone, (0, 1, 0)) - u) * 7.0 * aim,
                               np.array(pose[side + "_hand"]["rotation"]) * 0.03,          # a quiet wrist first,
                               np.array(pose[tool_bone]["rotation"]) * 0.012])             # then the tool in the fist
    if guess is not None: guess = list(guess); guess[3] = min(guess[3], -20.0)      # never start from a locked elbow
    M, err = RIG.solve(pose, free, res, ARM_BOUNDS[side], reg=0.012, guess=guess)
    return [pose[b]["rotation"][a] for b, a in free], M


def solve_leg(pose, side, guess):
    free = [(side + "_leg", 0), (side + "_leg", 1), (side + "_leg", 2), (side + "_shin", 0),
            (side + "_foot", 0), (side + "_foot", 1), (side + "_foot", 2)]
    target, toe = FEET[side]
    a = math.radians(toe)
    fwd = V(-math.sin(a), 0, -math.cos(a))

    def res(M):
        f = side + "_foot"
        return np.concatenate([(RIG.pt(M, f, SOLE[side]) - target) * 4.0,
                               (RIG.vec(M, f, (0, 1, 0)) - V(0, 1, 0)) * 20.0,
                               (RIG.vec(M, f, (0, 0, -1)) - fwd) * 12.0])
    bounds = [(-110, 60), (-60, 60), (-50, 50), (4, 140), (-80, 80), (-70, 70), (-50, 50)]
    if guess is not None: guess = list(guess); guess[3] = max(guess[3], 14.0)       # never start from a locked knee
    else: guess = [-30.0, 0.0, 0.0, 50.0, -20.0, 0.0, 0.0]
    RIG.solve(pose, free, res, bounds, reg=0.01, guess=guess)
    return [pose[b]["rotation"][a] for b, a in free]


def breath(t, period):
    return math.sin(2 * math.pi * t / period)


def body_pose(B, t, loop):
    a, b, f = seg(B, t)
    v = a[1] + (b[1] - a[1]) * f
    br = breath(t, 70.0) if loop else breath(t, 60.0)
    return {"hips": {"position": [float(v[0]), float(v[1] + 0.12 * br), float(v[2])], "rotation": [0.0, float(v[3]), 0.0]},
            "torso": {"rotation": [float(v[4] + 0.7 * br), float(v[5]), float(v[6])]},
            "head": {"rotation": [float(v[7] - 0.4 * br), float(v[8]), float(v[9])]}}


def bake(keys, length, loop):
    B, H, T = keys
    for K in keys: assert all(x[0] < y[0] for x, y in zip(K, K[1:])), "keys out of order"
    frames, warm = [], {}
    for sweep in range(2 if loop else 1):                 # a loop is solved twice so tick 0 starts from tick length-1
        frames, ends = [], {}
        for t in range(length + 1):
            pose = body_pose(B, t, loop)
            for side in ("right", "left"):
                warm["leg" + side] = solve_leg(pose, side, warm.get("leg" + side))
            M = RIG.fk(pose, ["head"])
            for side, bone, gp, K in (("right", "hammer", HAMMER_GRIP, H), ("left", "tongs", TONGS_GRIP, T)):
                a, b, f = seg(K, t)
                if b[1]["fk"] and a is not b:                 # a joint swing to key b: solve b once, at its own tick, then blend
                    ta, tb = a[0], b[0]
                    assert ta == int(ta) and tb == int(tb), "a swing runs between whole ticks"
                    if (side, tb) not in ends:
                        end = body_pose(B, tb, loop)
                        g, d, u = arm_target(RIG.fk(end, ["head"]), K, tb)
                        gs = b[1]["guess"]
                        if isinstance(gs, int): gs = get_arm(frames[gs], side, bone)
                        ends[(side, tb)], _ = solve_arm(end, side, bone, gp, g, d, u, gs if gs is not None else warm.get("arm" + side), b[1]["aim"])
                    if t < tb:
                        a0 = np.array(get_arm(frames[int(ta)], side, bone))
                        vals = a0 + (np.array(ends[(side, tb)]) - a0) * f
                        set_arm(pose, side, bone, vals); warm["arm" + side] = [float(x) for x in vals]
                        continue
                    warm["arm" + side] = ends[(side, tb)]
                g, d, u = arm_target(M, K, t)
                warm["arm" + side], _ = solve_arm(pose, side, bone, gp, g, d, u, warm.get("arm" + side), aim_now(K, t))
            frames.append(pose)
        if loop: frames[length] = frames[0] if sweep else frames[length]
    # secondary: the beard and braid hang back toward plumb and trail the head; the apron hangs from the hips and rides the thighs
    n = length
    def pitch(i):
        i = i % n if loop else max(0, min(n, i))
        p = frames[i]
        return p["torso"]["rotation"][0] + p["head"]["rotation"][0]
    for t, p in enumerate(frames):
        p["beard"] = {"rotation": [round(-0.50 * pitch(t - 2) + 0.35 * (pitch(t - 1) - pitch(t - 3)), 3), 0.0, round(-0.5 * p["head"]["rotation"][2], 3)]}
        p["braid"] = {"rotation": [round(-0.22 * pitch(t - 4) + 0.5 * (pitch(t - 3) - pitch(t - 5)), 3), 0.0, 0.0]}
        thigh = min(p["right_leg"]["rotation"][0], p["left_leg"]["rotation"][0], 0.0)
        p["apron"] = {"rotation": [round(0.75 * thigh, 3), 0.0, 0.0]}
    if loop: frames[length] = frames[0]
    return frames


def report(name, frames, keys, loop):
    B, H, T = keys
    worst = {"foot": 0.0, "hammer": 0.0, "tongs": 0.0, "step": 0.0}
    lo, hi, who = V(99, 99, 99), V(-99, -99, -99), {}
    for t, p in enumerate(frames):
        M = RIG.fk(p)
        for side in ("right", "left"):
            worst["foot"] = max(worst["foot"], float(np.linalg.norm(RIG.pt(M, side + "_foot", SOLE[side]) - FEET[side][0])))
        for bone, gp, K in (("hammer", HAMMER_GRIP, H), ("tongs", TONGS_GRIP, T)):
            if swinging(K, t): continue
            g, d, u = arm_target(M, K, t)
            worst[bone] = max(worst[bone], float(np.linalg.norm(RIG.pt(M, bone, gp) - g)))
        for bone, pts in (("hammer", [(-15, 0, -10.5), (-10, 10.5, -6.5), (-13.5, 7, 2.5)]), ("tongs", [(12.5, 7, -15)]), ("head", [(-6, 32, -6), (6, 32, 5), (0, 26, -8)]),
                          ("right_forearm", [(-16.5, 9, -4), (-8.5, 17, 4)]), ("left_forearm", [(16.5, 9, -4), (8.5, 17, 4)])):
            for q in pts:
                w = RIG.pt(M, bone, q)
                for i in range(3):
                    if w[i] < lo[i]: lo[i] = w[i]; who["lo" + "xyz"[i]] = "%s t%d" % (bone, t)
                    if w[i] > hi[i]: hi[i] = w[i]; who["hi" + "xyz"[i]] = "%s t%d" % (bone, t)
        if t:
            for b in p:
                if "rotation" in p[b] and b in frames[t - 1]:
                    worst["step"] = max(worst["step"], float(np.max(np.abs(np.array(p[b]["rotation"]) - np.array(frames[t - 1][b]["rotation"])))))
    print("%s: worst foot slip %.2f px, hammer grip off target %.2f px, tongs grip off target %.2f px, largest one-tick turn %.1f deg" % (name, worst["foot"], worst["hammer"], worst["tongs"], worst["step"]))
    print("  reach (px): x %.1f..%.1f, y ..%.1f, z %.1f..%.1f  (cull box at scale 1.3: |x|,|z| <= 19.7, y <= 40)" % (lo[0], hi[0], hi[1], lo[2], hi[2]))
    print("  reached by: " + ", ".join("%s %s" % (k, who[k]) for k in ("lox", "hix", "hiy", "loz", "hiz")))
    for t in [k[0] for k in H if k[1] is not H_READY]:
        if abs(t - round(t)) > 1e-6: continue
        M = RIG.fk(frames[int(t)])
        face = RIG.pt(M, "hammer", HAMMER_FACE)
        if abs(face[1] - STRIKE[1]) < 1.0 and np.linalg.norm(face - STRIKE) < 3:
            print("  tick %d: hammer face %.2f px from the mark, tilt %.1f deg" % (t, np.linalg.norm(face - STRIKE), math.degrees(math.acos(min(1.0, float(RIG.vec(M, "hammer", (0, 1, 0))[1]))))))


def clip(frames, loop):
    bones = {}
    for t, p in enumerate(frames):
        for b, ch in p.items():
            for k, val in ch.items():
                if k == "position" and b != "hips": continue
                bones.setdefault(b, {}).setdefault(k, {})[repr(round(t / 20.0, 4))] = [round(float(x), 3) for x in val]
    out = {"loop": True} if loop else {}
    out["animation_length"] = round((len(frames) - 1) / 20.0, 4)
    out["bones"] = bones
    return out


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    idle_k, inter_k = idle_keys(), interact_keys()
    idle, inter = bake(idle_k, 140, True), bake(inter_k, 60, False)
    inter[60] = idle[0]                                   # GeoCampNpc restarts idle at tick 0 after the interact
    if "--report" in sys.argv:
        report("idle", idle, idle_k, True); report("interact", inter, inter_k, False)
    out = os.path.join(args[0], "blacksmith.animation.json") if args else OUT
    os.makedirs(os.path.dirname(out), exist_ok=True)
    data = {"format_version": "1.8.0", "animations": {"idle": clip(idle, True), "interact": clip(inter, False)}}
    with open(out, "w", newline="\n") as f: f.write(json.dumps(data, separators=(",", ":")) + "\n")
    print("wrote", out)


if __name__ == "__main__":
    main()
