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
def tool(grip, d, s=(1, 0, 0), up=None, frame=None, world_aim=False):
    """An arm target: the grip (fist centre), the tool's direction out of the fist, and its side axis s (the tool's own up is
    s x d) or an up hint; in the world, or carried by `frame` (a bone, the point and directions given as at rest).
    world_aim: only the grip rides the frame, the directions stay in the world."""
    d = unit(d)
    if up is not None: s = np.cross(d, unit(up))
    return {"grip": V(*grip), "d": d, "s": unit(s), "frame": frame, "world_aim": world_aim}


def strike_at(d, lift=0.0):
    """The hammer target with its face flat on the billet (lift px above it), the haft pointing along d (level)."""
    d = unit(d)
    face = STRIKE + V(0, lift, 0)
    centre = face + V(0, 3.5, 0)                      # the head's centre, on the haft's axis
    return tool(centre - 8.5 * d, d)


def tongs_on_billet(d=(-0.62, -0.05, -0.78)):
    d = unit(d)
    return tool(BILLET_COLD - 14.0 * d, d, up=(0, 1, 0))


H_READY = tool((-9.6, 15.6, -5.6), (0.40, 0.12, -0.91))             # held low over the anvil, clear of his face
H_REST = tool((-9.5, 15.6, -7.6), (0.50, 0.02, -0.86))              # the face resting on the anvil at his right
T_GRIP = tongs_on_billet()


def strike_body(c):
    """Body keys of one two-beat strike starting at tick c (ends at READY by c + 44)."""
    return [
        (c + 6, body(hy=-2.1, tp=15, np_=9), "io"),                                     # anticipation: a small dip
        (c + 17, body(hx=-0.8, hy=-0.9, hz=3.6, hyaw=16, tp=1, ty=18, tr=-4, np_=8, ny=-26), "io"),   # up: legs straighten, chest opens right
        (c + 20, body(hx=-0.9, hy=-0.8, hz=3.7, hyaw=17, tp=-1, ty=20, tr=-4, np_=9, ny=-28), "io"),  # the hang at the top
        (c + 24, body(hx=0.6, hy=-3.3, hz=2.7, hyaw=5, tp=23, ty=-9, tr=3, np_=8, ny=-4), "in"),     # the blow: hips drop, chest drives down and through
        (c + 25.5, body(hx=0.7, hy=-3.8, hz=2.6, hyaw=4, tp=25.5, ty=-10, tr=3, np_=11, ny=-4), "out"), # the squash
        (c + 29.5, body(hx=0.3, hy=-2.3, hz=2.8, hyaw=7, tp=17, ty=-2, tr=1, np_=7, ny=-9), "io"),   # the rebound
        (c + 33, body(hx=0.5, hy=-2.9, hz=2.7, hyaw=6, tp=21, ty=-5, tr=2, np_=9, ny=-7), "in"),     # the tap
        (c + 34.5, body(hx=0.5, hy=-3.0, hz=2.7, hyaw=6, tp=21.5, ty=-5, tr=2, np_=9.5, ny=-7), "out"),
        (c + 46, READY, "io"),
    ]


def strike_hammer(c):
    return [
        (c + 6, tool((-7.6, 16.4, -6.2), (0.50, 0.06, -0.86)), "io"),                    # he lays the face near the mark
        (c + 12, tool((-12.6, 23.0, -1.5), (0.22, 0.80, -0.56)), "in"),                  # up past the shoulder
        (c + 17, tool((-14.0, 28.0, 5.0), (-0.10, 0.60, 0.79)), "out"),                  # over and behind it
        (c + 20, tool((-14.0, 28.5, 5.6), (-0.12, 0.53, 0.84)), "io"),                   # it hangs
        (c + 22.6, tool((-12.0, 25.0, 0.4), (0.24, 0.80, -0.55)), "in"),                 # the arc down, late and fast
        (c + 24, strike_at((0.50, 0, -0.87)), "lin"),                                    # the blow lands
        (c + 25.5, strike_at((0.50, 0, -0.87)), "lin"),
        (c + 29.5, tool((-5.6, 19.2, -6.2), (0.50, 0.40, -0.77)), "out"),                # it bounces off the iron
        (c + 33, strike_at((0.52, 0, -0.85)), "in"),                                     # the tap
        (c + 34.5, strike_at((0.52, 0, -0.85)), "lin"),
    ]


def idle_keys():
    B = [(0, READY, "io")] + strike_body(0) + [(60, READY, "io")] + strike_body(60)
    B += [(100, READY, "io"),
          (111, body(hy=-1.0, hz=3.2, hyaw=7, tp=9, ty=-4, tr=3, np_=11, ny=-26, nr=6), "io"),      # he straightens and dips his head into the arm
          (117, body(hy=-0.9, hz=3.2, hyaw=7, tp=8, ty=-3, tr=2, np_=8, ny=-12, nr=3), "io"),       # the head rolls against the wipe
          (123, body(hy=-1.0, hz=3.2, hyaw=7, tp=9, ty=-4, tr=3, np_=10, ny=-24, nr=6), "io"),
          (133, body(hy=-1.3, hz=3.1, hyaw=9, tp=11, ty=4, tr=0, np_=6, ny=-12), "io"),
          (140, READY, "io")]
    H = [(0, H_READY, "io")] + strike_hammer(0) + [(46, H_READY, "io"), (60, H_READY, "io")] + strike_hammer(60)
    H += [(104, H_REST, "io"), (130, H_REST, "io"), (140, H_READY, "io")]                # the hammer rests while he wipes
    # his arms are short: the graft's wrist reaches the brow only because the head turns and dips to meet it
    wipe = lambda x, y, z: tool((x, y, z), (0.62, 0.62, -0.48), up=(0.5, -0.8, 0.3), frame="head", world_aim=True)   # tongs out to his left, clear of his face
    T = [(0, T_GRIP, "io"), (101, T_GRIP, "io"),
         (111, wipe(6.4, 29.0, -9.7), "io"),             # the graft's wrist comes up to the brow
         (117, wipe(0.8, 29.6, -9.7), "io"),             # drags across it
         (123, wipe(5.6, 29.0, -9.7), "io"),             # and back
         (134, T_GRIP, "io"), (140, T_GRIP, "io")]
    return B, H, T


def interact_keys():
    lean = body(hx=0.4, hy=-2.6, hz=1.2, hyaw=0, tp=24, ty=-3, tr=2, np_=-13, ny=2)
    B = [(0, READY, "io"),
         (4, body(hy=-2.0, tp=14, np_=7, ny=-8), "io"),
         (10, body(hy=-0.5, hz=2.8, hyaw=3, tp=5, ty=2, tr=-2, np_=4, ny=0), "io"),                  # up and square to the player
         (18, lean, "io"),                                                                          # onto the anvil
         (26, body(hx=0.4, hy=-2.5, hz=1.2, hyaw=0, tp=23, ty=-3, tr=2, np_=-13, ny=2), "io"),
         (30, body(hx=0.4, hy=-2.6, hz=1.2, hyaw=0, tp=24, ty=-3, tr=2, np_=-3, ny=2), "io"),        # a nod
         (35, lean, "io"),
         (44, body(hx=0.4, hy=-2.5, hz=1.3, hyaw=0, tp=23, ty=-3, tr=2, np_=-12, ny=2), "io"),
         (60, READY, "io")]
    shoulder = tool((-10.6, 24.6, -8.6), (-0.12, 0.33, 0.94), frame="torso")                        # the haft across his right shoulder
    H = [(0, H_READY, "io"), (4, tool((-9.2, 15.2, -5.8), (0.42, 0.1, -0.9)), "io"),
         (10, shoulder, "io"), (46, shoulder, "io"), (60, H_READY, "io")]
    plant = tool((6.8, ANVIL_TOP + 2.1, -13.6), (0.42, 0.86, -0.28), up=(-0.9, 0.4, 0))             # the fist on the anvil, tongs upright
    T = [(0, T_GRIP, "io"), (5, T_GRIP, "io"), (18, plant, "io"), (45, plant, "io"), (58, T_GRIP, "io"), (60, T_GRIP, "io")]
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


def arm_target(M, keys, t):
    a, b, f = seg(keys, t)
    ga, da, sa = target_now(M, a[1]); gb, db, sb = target_now(M, b[1])
    d = slerp(da, db, f); s = slerp(sa, sb, f)
    return ga + (gb - ga) * f, d, unit(np.cross(s, d))


def solve_arm(pose, side, tool_bone, grip_pt, grip, d, u, guess):
    free = [(side + "_arm", 0), (side + "_arm", 1), (side + "_arm", 2), (side + "_forearm", 0),
            (side + "_hand", 0), (side + "_hand", 1), (side + "_hand", 2), (tool_bone, 0), (tool_bone, 1)]

    def res(M):
        return np.concatenate([(RIG.pt(M, tool_bone, grip_pt) - grip) * 3.0,
                               (RIG.vec(M, tool_bone, (0, 0, -1)) - d) * 14.0,
                               (RIG.vec(M, tool_bone, (0, 1, 0)) - u) * 7.0,
                               np.array(pose[side + "_hand"]["rotation"]) * 0.03,          # a quiet wrist first,
                               np.array(pose[tool_bone]["rotation"]) * 0.012])             # then the tool in the fist
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


def bake(keys, length, loop):
    B, H, T = keys
    frames, warm = [], {}
    for sweep in range(2 if loop else 1):                 # a loop is solved twice so tick 0 starts from tick length-1
        frames = []
        for t in range(length + 1):
            a, b, f = seg(B, t)
            v = a[1] + (b[1] - a[1]) * f
            br = breath(t, 70.0) if loop else breath(t, 60.0)
            pose = {"hips": {"position": [float(v[0]), float(v[1] + 0.12 * br), float(v[2])], "rotation": [0.0, float(v[3]), 0.0]},
                    "torso": {"rotation": [float(v[4] + 0.7 * br), float(v[5]), float(v[6])]},
                    "head": {"rotation": [float(v[7] - 0.4 * br), float(v[8]), float(v[9])]}}
            for side in ("right", "left"):
                warm["leg" + side] = solve_leg(pose, side, warm.get("leg" + side))
            M = RIG.fk(pose, ["head"])
            g, d, u = arm_target(M, H, t)
            warm["armR"], _ = solve_arm(pose, "right", "hammer", HAMMER_GRIP, g, d, u, warm.get("armR"))
            g, d, u = arm_target(M, T, t)
            warm["armL"], _ = solve_arm(pose, "left", "tongs", TONGS_GRIP, g, d, u, warm.get("armL"))
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
        g, d, u = arm_target(M, H, t)
        worst["hammer"] = max(worst["hammer"], float(np.linalg.norm(RIG.pt(M, "hammer", HAMMER_GRIP) - g)))
        g, d, u = arm_target(M, T, t)
        worst["tongs"] = max(worst["tongs"], float(np.linalg.norm(RIG.pt(M, "tongs", TONGS_GRIP) - g)))
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
