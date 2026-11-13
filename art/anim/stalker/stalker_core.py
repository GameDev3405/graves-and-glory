"""The barrow stalker's own animation solver (art/anim/stalker/build_stalker_anims.py builds the clips with it).

Every clip is authored as GOALS, not joint angles, and solved per sample (4 a tick) into bone rotations:

  hips_r, hips_p   the hips bone's rotation and position (px)
  feet             ankle plants in the world, moved by steps (side, t0, t1, to, yaw[, lift]); the legs are solved onto them
  torso, head      rotations
  grip             the bow hand's wrist, in CHEST space (the torso bone's own geo pixels: +x the troop's left, +y up, -z forward),
                   so the hand rides the chest as it leans and turns
  bow              (elevation, yaw, cant): where the arrow points in the WORLD and how far the limbs lean off upright. The wrist is
                   solved from it every sample, so the bow's facing is always explicit and never an accident of arm Eulers.
  string           0..1: how far the string hand has left its free target (rh, chest space) for the arrow's nock
  draw             0 bow at rest, 1 an arrow on the string, 3 full draw: the nock's distance behind the grip and the frame shown
  lhint, rhint     (arm x, y, z, forearm x) the arm solves stay near: they choose the elbow's side

Goals are interpolated with monotone curves (no overshoot), so far-apart poses never interpolate arm Eulers.

The rig maths (forward kinematics, leg solve, bake, grounded falls) is the knight builder's and the bow geometry (aim frame, wrist
onto the aim line, nock positions, flat bow) the bow family builder's; both are imported as libraries, neither is changed.
"""
import json, math, os, sys, tempfile
from pathlib import Path

os.environ["GG_BOW_TROOP"] = "stalker"
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "archer"))
sys.path.insert(0, str(HERE.parent / "knight"))
import build_bow_anims as B  # noqa: E402

K = B.K
np = K.np
rg = K.rg
SUB, TPS, Z3 = K.SUB, K.TPS, K.Z3

UPPER = ("torso", "head", "left_arm", "left_forearm", "left_hand", "right_arm", "right_forearm", "right_hand")
LEGS = ("hips", "right_leg", "right_shin", "right_foot", "left_leg", "left_shin", "left_foot")
CLOTH = ("mantle_back", "mantle_back_l", "mantle_back_r", "mantle_front_l", "mantle_front_r", "pelt_tail")
WRIST_L = (5.5, 15.0, 0.0)      # left_forearm-local: the bow hand's pivot
FIST_R = B.FIST                  # right_forearm-local: the fist on the string
GRIP_X, NOCK_Y = 6.0, B.NOCK_Y   # bow-local: the grip and the nock's height
STRING_TIP = (0.8, 2.6)           # bow-local: where the lower string meets the bottom limb

# One footing under everything that stands: the bow side leads, the back foot is turned out, the weight sits low between them.
FEET = {"right": ((-3.3, 2.0, -3.1), -40.0), "left": ((2.5, 2.0, 3.3), 6.0)}


def lerp(a, b, k):
    return tuple(x + (y - x) * k for x, y in zip(a, b))


def add(a, b):
    return tuple(x + y for x, y in zip(a, b))


def ramp(t, a, b):
    return B.ramp(t, a, b)


def wrap(a):
    return (a + 180.0) % 360.0 - 180.0


def bow_frame(elev, yaw, cant):
    """The arrow's direction d and the limbs' axis u (upright about d, leaned `cant` degrees: + tips the top limb to the right)."""
    d, u = B.aim(elev, yaw)
    c, s = math.cos(math.radians(cant)), math.sin(math.radians(cant))
    side = np.cross(d, u)
    return d, u * c + side * s


def chest(pose, p):
    """World px of a chest-space point."""
    return K.point(K.world(pose, "torso"), p)


def to_chest(pose, w):
    """Chest-space coordinates of a world point."""
    M = K.world(pose, "torso")
    v = np.linalg.inv(M) @ np.r_[np.asarray(w, float) / 16.0, 1.0]
    return (-v[0] * 16.0, v[1] * 16.0, v[2] * 16.0)


ELBOW = {"right": (-5.5, 19.0, 0.0), "left": (5.5, 19.0, 0.0)}     # the forearm pivots, model space


def arm_ik(Mt, side, target, hint, x0=None, elbow=None):
    """elbow: optional (x, y, z, weight), a chest-space point the elbow is drawn toward (fix round 2: the ready stance's
    string elbow is set out from the ribs; a weak Euler hint alone let it fold through the chest)."""
    arm, fore = side + "_arm", side + "_forearm"
    pt = FIST_R if side == "right" else WRIST_L
    hint = np.array(hint, float)
    ew = None if elbow is None else (K.point(Mt, elbow[:3]), float(elbow[3]))

    def tip(x):
        return K.point(Mt @ K.local(arm, (x[0], x[1], x[2])) @ K.local(fore, (x[3], 0.0, 0.0)), pt)

    def f(x):
        r = [4.0 * (tip(x) - target), 0.03 * (x - hint)]
        if ew is not None:
            r.append(ew[1] * (K.point(Mt @ K.local(arm, (x[0], x[1], x[2])), ELBOW[side]) - ew[0]))
        return np.concatenate(r)

    x = B.gn(f, hint if x0 is None else x0, iters=30, clamp=lambda x: np.r_[x[:3], min(-2.0, max(-142.0, x[3]))])
    return x, float(np.linalg.norm(tip(x) - target))


def feet_at(steps, t, base=FEET):
    """Each foot's (ankle px, yaw, sole pitch) at tick t: planted between steps, lifted on an eased line while stepping."""
    feet = {side: (np.array(p, float), y, 0.0) for side, (p, y) in base.items()}
    for side, t0, t1, to, yaw, *lift in steps:
        lift = lift[0] if lift else 1.4
        p0, y0, _ = feet[side]
        if t <= t0:
            continue
        u = min(1.0, (t - t0) / (t1 - t0))
        e = K.smooth(u)
        pos = p0 + (np.array(to, float) - p0) * e
        pos[1] += lift * math.sin(math.pi * u)
        feet[side] = (pos, y0 + (yaw - y0) * e, 9.0 * lift / 1.4 * math.sin(math.pi * u) * (1.0 if u < 0.6 else -0.4))
    return feet


class Solver:
    """Solves goals into a full pose, warm-started from the last sample."""

    def __init__(self):
        self.w = {}
        self.err = {"left": 0.0, "right": 0.0}

    def legs(self, g, feet):
        """The legs under the hips. A leg that cannot reach (too straight, or folded past its limit) sinks or lifts the hips a
        little at a time until it can, so a hard stumble never stops the build; the worst such shift is kept in err["hips"]."""
        last = None
        for k in range(-1, 25):
            dy = 0.0 if k < 1 else (0.0, -1.0, 1.0)[k % 3] * 0.12 * ((k + 2) // 3)
            try:
                out = self._legs(g, feet, dy, k >= 0)
            except ValueError as e:
                last = e
                continue
            self.err["hips"] = max(self.err.get("hips", 0.0), abs(dy))
            return out
        raise last

    def _legs(self, g, feet, dy, fresh):
        hp = tuple(g["hips_p"])
        hips = (tuple(g["hips_r"]), (hp[0], hp[1] + dy, hp[2]))
        pose, out = {"hips": hips}, {"hips": hips}
        w = {}
        for side in ("right", "left"):
            at, yaw, pitch = feet[side]
            pose[side + "_leg"] = ((0.0, -hips[0][1] - 0.6 * yaw, 0.0), Z3)
            lr, sr, fr = K.leg_pose(pose, side, at, pitch, foot_yaw=yaw, guess=None if fresh else self.w.get(side))
            w[side] = (lr[0], lr[2], sr[0])
            out[side + "_leg"], out[side + "_shin"], out[side + "_foot"] = (lr, None), (sr, None), (fr, None)
        self.w.update(w)
        return out

    def upper(self, g, legs):
        pose = {b: (v[0], v[1] or Z3) for b, v in legs.items()}
        pose["torso"] = (tuple(g["torso"]), Z3)
        pose["head"] = (tuple(g["head"]), Z3)
        Mt = K.world(pose, "torso")
        la, e = arm_ik(Mt, "left", K.point(Mt, g["grip"]), g["lhint"], self.w.get("la"))
        self.w["la"] = la
        self.err["left"] = max(self.err["left"], e)
        pose["left_arm"] = (tuple(la[:3]), Z3)
        pose["left_forearm"] = ((la[3], 0.0, 0.0), Z3)
        d, u = bow_frame(*g["bow"])
        pose["left_hand"] = (B.bow_wrist(pose, d, u), Z3)
        target = K.point(Mt, g["rh"])
        ws = g["string"][0]
        if ws > 0:
            nx = B.nock_x(max(1.0, min(3.0, g["draw"][0])))
            sl = float(g["slide"][0]) if "slide" in g else 0.0
            # slide (fix round 2): how far the string hand rests down the lower string from the nock, toward the bottom tip
            nock = K.point(K.world(pose, B.BOW), (nx + (STRING_TIP[0] - nx) * sl, NOCK_Y + (STRING_TIP[1] - NOCK_Y) * sl, 0.0))
            target = target + (nock - target) * ws
        ra, e = arm_ik(Mt, "right", target, g["rhint"], self.w.get("ra"), g.get("relbow"))
        self.w["ra"] = ra
        if ws >= 1.0:
            self.err["right"] = max(self.err["right"], e)
        pose["right_arm"] = (tuple(ra[:3]), Z3)
        pose["right_forearm"] = ((ra[3], 0.0, 0.0), Z3)
        pose["right_hand"] = (tuple(g["rhand"]), Z3)
        out = {b: (pose[b][0], None) for b in UPPER}
        out["draw"] = (Z3, (float(g["draw"][0]), 0.0, 0.0))
        return out

    def solve(self, g, feet):
        legs = self.legs(g, feet)
        f = dict(legs)
        f.update(self.upper(g, legs))
        return f


def fill(keys):
    """[(tick, {goal: value})] with each key changing only what it names -> every key holding every goal."""
    out, cur = [], {}
    for t, k in keys:
        cur = dict(cur)
        for n, v in k.items():
            cur[n] = tuple(float(c) for c in (v if isinstance(v, (tuple, list)) else (v,)))
        out.append((t, cur))
    return out


def goals_at(keys, t):
    return {n: K.curve([(kt, k[n]) for kt, k in keys], t) for n in keys[0][1]}


def run(keys, ticks, steps=(), mod=None, loop=False, feet=FEET, report=None):
    """Samples the keyed goals over `ticks` and solves each. mod(t, g) may adjust the goals (breath, sway, tremble).
    A loop is solved twice round so its warm starts close."""
    fn = keys if callable(keys) else None
    keys = None if fn else fill(keys)
    sv = Solver()
    out = []
    for lap in ((0, 1) if loop else (1,)):
        out = []
        for i in range(ticks * SUB + 1):
            t = i / SUB
            g = fill([(0, fn(t))])[0][1] if fn else goals_at(keys, t)
            if mod:
                mod(t, g)
            out.append(sv.solve(g, feet_at(steps, t, feet)))
        if lap == 0:
            sv.err = {"left": 0.0, "right": 0.0}
    if loop:
        out[-1] = out[0]
    if report:
        print("  %-22s reach miss: bow hand %.2f px, string hand %.2f px; hips shifted %.2f px" % (report, sv.err["left"], sv.err["right"], sv.err.get("hips", 0.0)))
    return out


def static(g, feet=FEET):
    """One goal set solved to a pose (the goals given as a single key dict)."""
    return Solver().solve(fill([(0, g)])[0][1], feet_at((), 0, feet))


# ------------------------------------------------------------------------------------------------
# The mantle: five long panels hang from the shoulders to the knees, and the pelt's tail down the back. They hang toward the
# ground when the chest leans away from them, are pushed out by a thigh that lifts under them, trail when he moves, and lag
# every change on a damped spring.
# ------------------------------------------------------------------------------------------------

def cloth(samples, loop=False, wind=0.0, flutter=0.0, period=None):
    def targets(i):
        s = samples[i]
        tx = s["torso"][0][0] + s["hips"][0][0] if "hips" in s else s["torso"][0][0]
        tz = s["torso"][0][2] + (s["hips"][0][2] if "hips" in s else 0.0)
        ph = 2 * math.pi * i / (period * SUB) if period else 0.0
        out = {}
        for b in CLOTH:
            back = "front" not in b
            hang = -0.75 * (min(tx, 0.0) if back else max(tx, 0.0))
            push = 0.0
            side = "left" if b.endswith("_l") else "right" if b.endswith("_r") else None
            if side and side + "_leg" in s:
                lx = s[side + "_leg"][0][0]
                push = 0.5 * max(0.0, lx - 8.0) if back else -0.55 * max(0.0, -lx - 12.0)
            if b in ("mantle_back", "pelt_tail") and "left_leg" in s:
                push = 0.25 * max(0.0, max(s["left_leg"][0][0], s["right_leg"][0][0]) - 8.0)
            k = 1.0 if back else -0.35
            sw = math.sin(ph * 2 + (0.6 if side == "left" else 0.0)) if period else 0.0
            x = hang + push + k * (wind + flutter * sw)
            z = -0.5 * tz
            if b == "pelt_tail":
                x *= 1.25
            out[b] = (x, z)
        return out

    n = len(samples)
    pos = {b: list(targets(0)[b]) for b in CLOTH}
    vel = {b: [0.0, 0.0] for b in CLOTH}
    dt, kk, dd = 1.0 / SUB, 0.55, 0.75      # per tick: stiffness and damping of the lag
    res = None
    for lap in ((0, 1) if loop else (1,)):
        res = []
        for i in range(n):
            tg = targets(i)
            fr = {}
            for b in CLOTH:
                for c in (0, 1):
                    vel[b][c] += (kk * (tg[b][c] - pos[b][c]) - dd * vel[b][c]) * dt
                    pos[b][c] += vel[b][c] * dt
                x, z = pos[b]
                x = max(x, 0.0) if "front" not in b else min(x, 0.0)     # cloth never passes through the body it lies on
                fr[b] = ((x, 0.0, z), None)
            res.append(fr)
    if loop:
        res[-1] = res[0]
    for s, fr in zip(samples, res):
        s.update(fr)
    return samples


def part(samples, bones):
    return [{b: s[b] for b in bones if b in s} for s in samples]


def upper_of(samples):
    return part(samples, UPPER + CLOTH + SOFT + ("draw",))


def legs_of(samples):
    return part(samples, LEGS + GEAR)


# ------------------------------------------------------------------------------------------------
# Review pass (2026-11-13). Neither runs on the two shots (Kort approved them as they are).
#   gear_follow  the boot (<side>_foot_gear) hangs off the shin, so with the foot turned flat under a leaning shin its toe
#                sank 1.7 px into the floor in every standing clip. It now takes the foot's turn about the foot's own
#                pivot, the knight builder's boot_follow for this rig's name.
#   soften       the hood's fur, the hip wraps and the shoulder plates swing (fullbody.soft_motion) in hits and the stun;
#                the mantle and the pelt keep this file's own cloth.
# ------------------------------------------------------------------------------------------------

GEAR = ("right_foot_gear", "left_foot_gear")
SOFT = ("fur_cowl", "wrap_tails", "right_pauldron", "left_pauldron")


def _gear_pos(side, rot, pos=None):
    M = K.MODEL
    gear, foot = side + "_foot_gear", side + "_foot"
    r = M.bones[foot].rot + np.radians([-rot[0], -rot[1], rot[2]])
    R = (rg._Rz(r[2]) @ rg._Ry(r[1]) @ rg._Rx(r[0]))[:3, :3]
    d = (np.eye(3) - R) @ (M.bones[foot].pivot - M.bones[gear].pivot)
    p0 = pos or (0.0, 0.0, 0.0)
    return (float(p0[0] - d[0]), float(p0[1] + d[1]), float(p0[2] + d[2]))


def gear_follow(samples):
    for s in samples:
        for side in ("left", "right"):
            if side + "_foot" in s and side + "_foot_gear" in K.MODEL.bones:
                rot, pos = s[side + "_foot"]
                s[side + "_foot_gear"] = (tuple(rot), _gear_pos(side, rot, pos))
    return samples


def gear_delta(frames, base):
    """The boot's keys in an additive legs clip: the game adds the clip onto the stance, so the boot's offset is solved
    for the stance's foot turned by the clip's foot delta (base: the stance pose under the hit), not taken from the hit's
    own reference pose."""
    for f in frames:
        for side in ("left", "right"):
            foot, gear = side + "_foot", side + "_foot_gear"
            if foot not in f or foot not in base:
                continue
            rf = add(base[foot][0], f[foot][0])
            p = _gear_pos(side, rf, base[foot][1])
            b0 = _gear_pos(side, base[foot][0], base[foot][1])
            f[gear] = (tuple(f[foot][0]), tuple(a - c for a, c in zip(p, b0)))
    return frames


def soften(samples, seam=None, tune=None):
    """fullbody.soft_motion over full-body samples (in place) for the hood fur, the hip wraps and the shoulder plates,
    low-passed as the bow family's fix layer does. seam: a loop's first sample index (the stun's tick 4); the gap between
    it and the last sample is spread over the loop so it closes."""
    sys.path.insert(0, str(HERE.parent / "common"))
    import fullbody as FB
    parts = {n: c for n, c in FB.soft_parts_for().items() if not set(c["bones"]) & set(CLOTH)}
    for n, over in (tune or {}).items():
        if n in parts:
            parts[n].update(over)
    up = part(samples, UPPER)
    lg = part(samples, LEGS)
    up2, lg2 = FB.soft_motion(up, lg, parts=parts)
    got = {}
    for u, l in zip(up2, lg2):
        for src in (u, l):
            for b in SOFT:
                if b in src:
                    got.setdefault(b, []).append(np.array(src[b][0], float))
    for b, rots in got.items():
        rots = np.array(rots)
        for _ in range(2):
            pad = np.vstack([rots[:1], rots[:1], rots, rots[-1:], rots[-1:]])
            sm = (pad[:-4] + 2 * pad[1:-3] + 3 * pad[2:-2] + 2 * pad[3:-1] + pad[4:]) / 9.0
            sm[0], sm[-1] = rots[0], rots[-1]
            rots = sm
        if seam is not None:
            n = len(rots) - 1
            gap = rots[seam] - rots[n]
            for i in range(seam, n + 1):
                rots[i] = rots[i] + gap * (i - seam) / float(n - seam)
        for s, r in zip(samples, rots):
            s[b] = (tuple(float(v) for v in r), None)
    return samples


def close_end(frames, ticks=3.0):
    """An additive clip ends exactly on zero: whatever it still holds at its end is taken out over its last ticks."""
    n = len(frames) - 1
    w0 = n - int(ticks * SUB)
    last = frames[-1]
    for i, f in enumerate(frames):
        if i <= w0:
            continue
        k = K.smooth((i - w0) / float(n - w0))
        for b, (rot, pos) in f.items():
            lr, lp = last[b]
            f[b] = (tuple(a - k * c for a, c in zip(rot, lr)),
                    None if pos is None else tuple(a - k * c for a, c in zip(pos, lp)))
    return frames


def delta(samples, ref, bones):
    """Additive offsets of the samples from a reference pose (rotations wrapped; the hips' position too)."""
    out = []
    for s in samples:
        f = {}
        for b in bones:
            if b not in s or (b not in ref and b not in SOFT):
                continue
            rot = tuple(wrap(a - c) for a, c in zip(s[b][0], ref[b][0] if b in ref else Z3))
            pos = tuple(a - c for a, c in zip(s[b][1], ref[b][1])) if b == "hips" or b in GEAR else None
            f[b] = (rot, pos)
        out.append(f)
    return out


# ------------------------------------------------------------------------------------------------
# Review sheets: big cells, the bow frame the draw channel picks, several clips or composed samples.
# ------------------------------------------------------------------------------------------------

rg.VIEWS["left34"] = (tuple(rg._norm((1.0, 0.45, 1.0))), (0.0, 1.0, 0.0))
rg.VIEWS["left"] = (tuple(rg._norm((1.0, 0.2, 0.0))), (0.0, 1.0, 0.0))
_SCENES = {}
FRAMES = ("rest", "pull_0", "pull_1", "pull_2")
TEX = str(HERE / "out" / "stalker_t3_crimson.png")


def _scene(frame, clip_path):
    model = _SCENES.get(frame)
    if model is None:
        model = _SCENES[frame] = rg.load_geo(str(K.PREVIEW / ("t3_%s.geo.json" % frame)))
    return rg.Scene(model, [TEX], [rg.load_clip(clip_path, "x")], 0.0)


def sheet(samples, ticks, out, views=("front", "left34", "left", "three_quarter"), scale=110, distance="near", label="", wide=False, cols=7):
    """Renders the composed samples (every bone in one pose list) at the given ticks: one column a tick, one row a view."""
    from PIL import Image, ImageDraw
    if isinstance(samples, dict):       # an already baked clip (the deaths): no draw channel, the bow at rest
        clip, n = samples, int(round(samples["animation_length"] * TPS))
        samples = [{}] * (n * SUB + 1)
    else:
        n = (len(samples) - 1) // SUB
        clip = K.bake([{b: v for b, v in s.items()} for s in samples], max(n, 1), loop=False)
    tmp = Path(tempfile.gettempdir()) / ("stalker_sheet_%d.json" % os.getpid())
    tmp.write_text(json.dumps({"format_version": "1.8.0", "animations": {"x": clip}}))
    scenes = {f: _scene(f, str(tmp)) for f in FRAMES}
    bounds = ((-1.15, 0.0, -1.15), (1.15, 2.75, 1.15))
    cams = rg.make_cameras(list(views), bounds, distance, scale if distance == "near" else 1080)
    cells = []
    for t in ticks:
        s = samples[min(len(samples) - 1, int(round(t * SUB)))]
        dr = s.get("draw", (Z3, Z3))[1] or Z3
        sc = scenes[FRAMES[max(0, min(3, int(round(abs(dr[0])))))]]
        fr = sc.evaluate(min(t, n - 0.01))
        cells.append([Image.fromarray(rg.render_view(sc, fr, c.bind(fr.root_off), ss=2, marker=False)) for c in cams])
    if distance != "near":
        cells = [[im.resize((im.width * 3, im.height * 3), Image.NEAREST) for im in col] for col in cells]
    cw = max(im.width for col in cells for im in col)
    rh = [max(col[v].height for col in cells) for v in range(len(views))]
    cols = len(cells) if wide else min(len(cells), cols)
    rows = (len(cells) + cols - 1) // cols
    bh = sum(rh) + 14
    img = Image.new("RGB", (cw * cols, bh * rows), (255, 255, 255))
    d = ImageDraw.Draw(img)
    for ci, col in enumerate(cells):
        x, y = (ci % cols) * cw, (ci // cols) * bh
        d.text((x + 3, y + 1), "%s t=%g" % (label, ticks[ci]), fill=(0, 0, 0))
        y += 14
        for v, im in enumerate(col):
            img.paste(im, (x, y))
            y += rh[v]
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    return out
