"""The v4 death system (Knight deaths v3/v4, docs/anim-rebuild/KNIGHT-REVIEW.md), shared by every biped builder.

Twelve falls: four directions, each with the gear kept, the first piece dropped, or every piece dropped:
  death_<dir>        keeps the gear         death_<dir>_drop1   lets the first piece (the weapon) go
  death_<dir>_drop2  lets every piece go    (a troop with one piece has the same drop in both)
The body is keyed (eased between keys) and grounded on its own corners only, so the gear never props it up; it lands and
stops dead (no rebound key, no slide). A wrist eases by quaternion slerp and is decomposed back to euler continuously.
A held piece is kept off the floor and out of the body by a small shoulder-plus-wrist correction (d3_clear). A dropped
piece is animated on its own bones: each sample's wanted world transform is baked into the hand's frame, so no rig change.
It leaves with the hand's velocity and spin, flies under gravity, lands tilted onto one end, tips flat, gives one small
clatter and lies still beside the hand that held it (_d3_beside).

A builder calls configure() once with its own geometry, bake function and gear list, then d3_death_clips(start):

    deaths.configure(heavy=<t3 geo>, bake=bake, sub=SUB, ticks=20, bones=DEATH_BONES,
                     gear=deaths.gear("weapon", shield=False))
    clips = deaths.d3_death_clips(idle_start())     # {"death_back": clip, "death_back_drop1": clip, ...}

gear() names the troop's pieces: the weapon (bones <prefix>_t1..t3, right hand) and, with shield=True, the shield (left hand).
d3_death_clips takes `drops` (per piece and direction: release tick and landing spec) and `arms` (per direction and side:
the resting arm, forearm and wrist) to override the knight's defaults.
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "tools" / "anim"))
import render_geo as rg  # noqa: E402

Z3 = (0.0, 0.0, 0.0)

# Set by configure(): the builder's geometry and clip baker.
HEAVY = None          # the heaviest tier's geo (t3): the falls ground and clear against it
SUB = 4               # samples per tick
DEATH_TICKS = 20
DEATH_BONES = ()
GEAR = {}
bake = None
POSE_FIX = None       # optional: a builder's pose -> pose (a rig's follower bones, e.g. a boot that follows its foot), applied before every fk


def configure(heavy, bake, sub, ticks, bones, gear):
    """heavy: the troop's heaviest geo; bake(samples, ticks, loop): the builder's clip baker; sub: samples per tick;
    ticks: clip length; bones: the bones a fall keys (a rig without a cape leaves it out); gear: see gear()."""
    g = globals()
    g["HEAVY"], g["bake"], g["SUB"], g["DEATH_TICKS"], g["DEATH_BONES"] = heavy, bake, sub, ticks, tuple(bones)
    g["GEAR"] = {p: dict(v, fist=tuple(b for b in v["fist"] if b in heavy.bones)) for p, v in gear.items()}


def gear(weapon="sword", shield=True):
    """The pieces a troop carries and drops: the weapon in the right hand and, if shield, the shield in the left.
    Each: match (bone-name prefix of the piece), hand and arm (the bones that hold it), ref (the bone its transform is
    solved on), bones (the piece's own bones, animated when dropped), fist (the forearm pieces whose overlap with
    the piece is allowed), centroid_bones (the cubes that give its centre; None: all of them)."""
    out = {"sword": dict(match=weapon, hand="right_hand", arm="right_arm", ref=weapon + "_t3",
                         bones=tuple(weapon + "_t%d" % i for i in (1, 2, 3)),
                         fist=("right_forearm", "right_cuff", "right_glove"), centroid_bones=(weapon + "_t3",))}
    if shield:
        out["shield"] = dict(match="shield", hand="left_hand", arm="left_arm", ref="shield", bones=("shield",),
                             fist=("left_forearm", "left_cuff", "left_glove"), centroid_bones=None)
    return out


def _ease(kind, s):
    if kind == "in":
        return s * s
    if kind == "out":
        return 1.0 - (1.0 - s) ** 2
    if kind == "lin":
        return s
    return s * s * (3.0 - 2.0 * s)


def _bone_mat(b, rot, pos, par):
    pos = pos or Z3
    r = b.rot + np.radians([-rot[0], -rot[1], rot[2]])
    pv = b.pivot / 16.0
    return (par @ rg._T(-pos[0] / 16.0, pos[1] / 16.0, pos[2] / 16.0) @ rg._T(*pv)
            @ rg._Rz(r[2]) @ rg._Ry(r[1]) @ rg._Rx(r[0]) @ rg._T(*(-pv)))


def mirror(keys):
    """The same fall to the other side: left and right bones swap and y/z rotations flip. Arms are left as
    given (the sword and shield are not mirror images), so a mirrored variant re-keys them."""
    sw = lambda b: ("left_" + b[6:] if b.startswith("right_") else "right_" + b[5:] if b.startswith("left_") else b)
    out = []
    for t, ease, k in keys:
        out.append((t, ease, {sw(b): (r[0], -r[1], -r[2]) for b, r in k.items()
                              if "arm" not in b and "hand" not in b}))
    return out


D3_SLERP = ("right_hand", "left_hand")   # wrists eased by slerp in every fall, gear or not
D3_G = 1.0          # px per tick^2: the falls are stylised fast, so dropped gear falls as fast to match them
D3_SLIP = 3.5      # px/tick, the most sideways speed a dropped piece leaves the hand with
D3_LAT = 0.75       # px: surface lattice step of the gear for the clearance checks


def _d3_part(bone):
    for part, g in GEAR.items():
        if bone.startswith(g["match"]):
            return part
    return "body"


class D3Geo:
    """Cube boxes of a model, split into body and each gear piece, with a surface lattice on the gear."""

    def __init__(self, model):
        self.model = model
        self.bone = list(model.cube_bone)
        self.part = [_d3_part(b) for b in self.bone]
        self.loc, self.lo, self.hi, self.lat = [], [], [], {}
        for ci, cube in enumerate(model.cube_list):
            cp = cube.pivot / 16.0
            self.loc.append(rg._T(*cp) @ rg._Rz(cube.rot[2]) @ rg._Ry(cube.rot[1]) @ rg._Rx(cube.rot[0]) @ rg._T(*(-cp)))
            lo, hi = cube.corners.min(0), cube.corners.max(0)
            self.lo.append(lo)
            self.hi.append(hi)
            if self.part[ci] != "body":
                ax = [np.linspace(lo[k], hi[k], max(2, int(math.ceil((hi[k] - lo[k]) * 16.0 / D3_LAT)) + 1)) for k in range(3)]
                g = np.stack(np.meshgrid(*ax, indexing="ij"), -1).reshape(-1, 3)
                on = np.zeros(len(g), bool)
                for k in range(3):
                    on |= np.isclose(g[:, k], lo[k]) | np.isclose(g[:, k], hi[k])
                self.lat[ci] = g[on]
        self.cubes = {p: [ci for ci in range(len(self.bone)) if self.part[ci] == p] for p in ("body",) + tuple(GEAR)}
        ref = {}
        for p, g in GEAR.items():
            only = g.get("centroid_bones")       # None: every cube of the piece
            P = np.vstack([model.cube_list[ci].corners for ci in self.cubes[p] if only is None or self.bone[ci] in only])
            ref[p] = P.mean(0)
        self.centroid = ref        # bone-local (blocks) centroid of each piece, for its flight

    def fk(self, pose):
        if POSE_FIX is not None:
            pose = POSE_FIX(pose)
        M = {}
        for n in self.model.order:
            b = self.model.bones[n]
            rot, pos = pose.get(n, (Z3, None))
            M[n] = _bone_mat(b, rot, pos, M[b.parent] if b.parent is not None else rg.WORLD_FLIP)
        return M

    def box(self, M, ci):
        return M[self.bone[ci]] @ self.loc[ci]

    def corners(self, M, cis):
        out = []
        for ci in cis:
            A = self.box(M, ci)
            out.append(self.model.cube_list[ci].corners @ A[:3, :3].T + A[:3, 3])
        return np.vstack(out) * 16.0 if out else np.zeros((0, 3))

    def lattice(self, M, part):
        out = []
        for ci in self.cubes[part]:
            A = self.box(M, ci)
            out.append(self.lat[ci] @ A[:3, :3].T + A[:3, 3])
        return np.vstack(out) * 16.0

    def depth(self, M, pts, cis):
        """Deepest (px) any of pts (world px) sits inside the boxes cis, and the bone of that box."""
        best, who = 0.0, None
        q0 = pts / 16.0
        for ci in cis:
            A = self.box(M, ci)
            R, t = A[:3, :3], A[:3, 3]
            q = (q0 - t) @ R
            d = np.minimum(q - self.lo[ci], self.hi[ci] - q).min(1) * 16.0
            m = float(d.max())
            if m > best:
                best, who = m, self.bone[ci]
        return best, who


def _d3_q(R):
    """Unit quaternion (w, x, y, z) of a rotation matrix."""
    w = math.sqrt(max(0.0, 1.0 + R[0, 0] + R[1, 1] + R[2, 2])) / 2.0
    x = math.copysign(math.sqrt(max(0.0, 1.0 + R[0, 0] - R[1, 1] - R[2, 2])) / 2.0, R[2, 1] - R[1, 2])
    y = math.copysign(math.sqrt(max(0.0, 1.0 - R[0, 0] + R[1, 1] - R[2, 2])) / 2.0, R[0, 2] - R[2, 0])
    z = math.copysign(math.sqrt(max(0.0, 1.0 - R[0, 0] - R[1, 1] + R[2, 2])) / 2.0, R[1, 0] - R[0, 1])
    q = np.array([w, x, y, z])
    return q / np.linalg.norm(q)


def _d3_R(q):
    w, x, y, z = q
    return np.array([[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                     [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                     [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]])


def _d3_slerp(q0, q1, s):
    d = float(np.dot(q0, q1))
    if d < 0.0:
        q1, d = -q1, -d
    if d > 0.9995:
        q = q0 + (q1 - q0) * s
        return q / np.linalg.norm(q)
    a = math.acos(d)
    return (math.sin((1 - s) * a) * q0 + math.sin(s * a) * q1) / math.sin(a)


def _d3_exp(r):
    """Rotation matrix of a rotation vector (radians)."""
    a = float(np.linalg.norm(r))
    if a < 1e-12:
        return np.eye(3)
    k = r / a
    K_ = np.array([[0, -k[2], k[1]], [k[2], 0, -k[0]], [-k[1], k[0], 0]])
    return np.eye(3) + math.sin(a) * K_ + (1 - math.cos(a)) * K_ @ K_


def _d3_log(R):
    """Rotation vector (radians, angle <= pi) of a rotation matrix."""
    q = _d3_q(R)
    if q[0] < 0:
        q = -q
    s = float(np.linalg.norm(q[1:]))
    if s < 1e-12:
        return np.zeros(3)
    return q[1:] / s * 2.0 * math.atan2(s, q[0])


def _d3_local_R(bone, rot):
    b = HEAVY.bones[bone]
    r = b.rot + np.radians([-rot[0], -rot[1], rot[2]])
    return (rg._Rz(r[2]) @ rg._Ry(r[1]) @ rg._Rx(r[0]))[:3, :3]


def _d3_anim(bone, R, near):
    """Animation degrees of `bone` whose local rotation (with its rest) is R, on the euler branch nearest `near`."""
    b = HEAVY.bones[bone]
    r1 = math.asin(max(-1.0, min(1.0, -R[2, 0])))
    if abs(math.cos(r1)) < 1e-6:     # gimbal: put it all on x
        r0, r2 = math.atan2(-R[1, 2], R[1, 1]), 0.0
        sols = [(r0, r1, r2)]
    else:
        r0 = math.atan2(R[2, 1], R[2, 2])
        r2 = math.atan2(R[1, 0], R[0, 0])
        sols = [(r0, r1, r2), (r0 + math.pi, math.pi - r1, r2 + math.pi)]
    best = None
    for s in sols:
        a = [-math.degrees(s[0] - b.rot[0]), -math.degrees(s[1] - b.rot[1]), math.degrees(s[2] - b.rot[2])]
        for k in range(3):
            a[k] = near[k] + ((a[k] - near[k] + 180.0) % 360.0 - 180.0)
        d = sum(abs(a[k] - near[k]) for k in range(3))
        if best is None or d < best[0]:
            best = (d, tuple(a))
    return best[1]


def _d3_tracks(keys, start):
    """Per bone, its own keys [(tick, ease into it, rot)] from frame 0: a bone eases only between the keys that
    name it, so the root can accelerate through one long "in" while the arms and head key their own beats."""
    tr = {b: [(0.0, None, tuple(map(float, start[b][0])))] for b in DEATH_BONES}
    for t, ease, k in sorted(keys, key=lambda x: x[0]):
        for b, r in k.items():
            if b in tr:                   # a rig without a cape (ic_hearthguard) skips the cape keys
                tr[b].append((float(t), ease, tuple(map(float, r))))
    return tr


def _d3_rot(tracks, tick, prev):
    rot = {}
    for n in DEATH_BONES:
        tr = tracks[n]
        j = next((j for j in range(1, len(tr)) if tick <= tr[j][0]), None)
        if j is None:
            t0 = t1 = tr[-1][0]
            a = b = tr[-1][2]
            e = 1.0
        else:
            t0, _, a = tr[j - 1]
            t1, ease, b = tr[j]
            e = _ease(ease, min(1.0, max(0.0, (tick - t0) / (t1 - t0))))
        if n in D3_SLERP:
            q = _d3_slerp(_d3_q(_d3_local_R(n, a)), _d3_q(_d3_local_R(n, b)), e)
            rot[n] = _d3_anim(n, _d3_R(q), prev[n] if prev else a)
        else:
            rot[n] = tuple(a[c] + (b[c] - a[c]) * e for c in range(3))
    return rot


def d3_body(keys, start, geo):
    """Samples {bone: (rot, pos)} of a fall: keyed bones eased, the root solved on the body's own corners (never
    the gear's) so the lowest stays on the ground and the body rolls about it without sliding. Returns the
    samples and the ground height (px)."""
    frames = _d3_tracks(keys, start)
    hips_pos = start["hips"][1]
    body = geo.cubes["body"]
    samples, base_y, shift, prev, prot = [], None, np.zeros(3), None, None
    for i in range(DEATH_TICKS * SUB + 1):
        rot = _d3_rot(frames, i / SUB, prot)
        prot = rot
        pose = {n: (rot[n], None) for n in DEATH_BONES}
        pose["hips"] = (rot["hips"], hips_pos)
        P = geo.corners(geo.fk(pose), body)
        low = int(np.argmin(P[:, 1]))
        if prev is None:
            base_y = P[low, 1]
            if -0.5 < base_y < -0.05:      # a sole cube inflated past the foot (the Revenant's boot, -0.3): the floor is y 0
                base_y = 0.0
        else:
            d = prev[low] - P[low]
            shift += (d[0], 0.0, d[2])
        prev = P
        pose["root"] = (rot["root"], (shift[0], base_y - P[low, 1], -shift[2]))
        samples.append(pose)
    return samples, float(base_y)


def _d3_gear_local(M_hand, W, bone):
    """Animation rotation (deg, unwrapped later) and position (px) of gear `bone` under a hand at M_hand so its
    matrix is W."""
    b = HEAVY.bones[bone]
    L = np.linalg.inv(M_hand) @ W
    R = L[:3, :3]
    pv = b.pivot / 16.0
    p = L[:3, 3] - pv + R @ pv
    return R, (-p[0] * 16.0, p[1] * 16.0, p[2] * 16.0)


def d3_drop(samples, geo, part, spec, ground):
    """Writes the dropped piece's own channels into samples. spec: release tick, landing yaw (deg, of the
    piece's long axis in world xz), flip (+1 face up / -1), tilt (deg the leading end is lower at landing),
    land (x, z) world px of the piece's centroid at rest, bounce (deg). Returns a report dict."""
    hand, ref = GEAR[part]["hand"], GEAR[part]["ref"]
    n = len(samples)
    Ms = [geo.fk(s) for s in samples]
    held = [M[ref] for M in Ms]
    ir = int(round(spec["release"] * SUB))
    c_loc = np.r_[geo.centroid[part], 1.0]
    cen = lambda W: (W @ c_loc)[:3] * 16.0
    v = (cen(held[ir]) - cen(held[ir - 1])) * SUB                      # px per tick
    # a dead hand lets go, it does not throw: the slip keeps the hand's heading but not a flailing arm's speed
    hs = float(np.linalg.norm(v[[0, 2]]))
    if hs > D3_SLIP:
        v[[0, 2]] *= D3_SLIP / hs
    v[1] = min(v[1], 1.0)
    Rr = held[ir][:3, :3]
    w = _d3_log(Rr @ held[ir - 1][:3, :3].T) * SUB                       # rad per tick, world
    # the rest: long axis (local y) along the yaw, face normal (local z) up or down, lying on the ground
    yaw = math.radians(spec.get("yaw", 0.0))                           # "beside" specs solve their own rest below
    ay = np.array([math.sin(yaw), 0.0, math.cos(yaw)])
    az = np.array([0.0, float(spec.get("flip", 1)), 0.0])
    R_rest = np.column_stack([np.cross(ay, az), ay, az])
    cis = geo.cubes[part]

    def place(R, anchor_world, anchor_local):
        W = np.eye(4)
        W[:3, :3] = R
        W[:3, 3] = anchor_world / 16.0 - R @ anchor_local
        return W

    rel = {b: np.linalg.inv(Ms[0][ref]) @ Ms[0][b] for b in set(geo.bone[ci] for ci in cis)}

    def gc(W):
        return geo.corners({b: W @ r for b, r in rel.items()}, cis)
    # rest: centroid at land (x, z), the lowest face on the ground
    def rest_at(x, z):
        W = place(R_rest, np.array([x, 0.0, z]), geo.centroid[part])
        W[1, 3] += (ground - gc(W)[:, 1].min()) / 16.0
        return W
    if spec.get("beside"):
        # v4: it falls flat right beside the hand that let it go, on that hand's side of the body
        c0 = cen(held[ir])
        T0 = (v[1] + math.sqrt(max(0.0, v[1] ** 2 + 2.0 * D3_G * max(0.0, c0[1] - ground - 1.0)))) / D3_G
        obst = [ci for ci in range(len(geo.bone)) if geo.part[ci] != part]
        i_land = max(ir + 1, int(round((spec["release"] + T0) * SUB)) - 6)
        zone = [_d3_boxes(geo, Ms[i], obst) for i in sorted(set(list(range(i_land, n, 2)) + [n - 1]))]
        bes = _d3_beside(geo, Ms[ir], Ms[-1], held[ir], hand, part, rel, gc, ground, zone)
        W_rest, R_rest, ay = bes["W"], bes["R"], bes["ay"]
        spec = dict(spec, lead=bes["lead"])
    elif "land" in spec:
        W_rest = rest_at(*spec["land"])
    else:
        # where it would fall with no push but gravity, moved straight out from under the body (and any other
        # piece) until it lies clear beside it
        c0 = cen(held[ir])
        W_rest = rest_at(c0[0], c0[2])
        dy = c0[1] - cen(W_rest)[1]
        T0 = (v[1] + math.sqrt(max(0.0, v[1] ** 2 + 2.0 * D3_G * dy))) / D3_G
        bx = c0[[0, 2]] + v[[0, 2]] * T0
        Mf = Ms[-1]
        obst = [ci for ci in range(len(geo.bone)) if geo.part[ci] != part]
        # clear of everything that lies or comes down there from a little before it lands to the end
        i_land = max(ir + 1, int(round((spec["release"] + T0) * SUB)) - 6)
        zone = [_d3_boxes(geo, Ms[i], obst) for i in sorted(set(list(range(i_land, n, 2)) + [n - 1]))]
        foot = geo.corners(Mf, geo.cubes["body"])[:, [0, 2]].mean(0)
        out = bx - foot
        if np.linalg.norm(out) < 1e-3:
            out = c0[[0, 2]] - foot
        out /= np.linalg.norm(out)
        for step in np.arange(0.0, 60.0, 0.5):
            x, z = bx + out * step
            W_rest = rest_at(x, z)
            Mg = {b: W_rest @ r for b, r in rel.items()}
            G = geo.lattice(Mg, part)
            if all(_d3_pen(G + o, bx) == 0.0 for bx in zone
                   for o in ((0, 0.0, 0), (1.5, 0, 0), (-1.5, 0, 0), (0, 0, 1.5), (0, 0, -1.5), (0, 1.0, 0))):
                break
    # if nothing clears it in the air it lands a little further out and tries again
    land_out = 0.0
    for _try in range(4):
        if _try:
            if "land" in spec:
                break
            land_out += 3.0
            if spec.get("beside"):
                bes = _d3_beside(geo, Ms[ir], Ms[-1], held[ir], hand, part, rel, gc, ground, zone, land_out)
                W_rest, R_rest, ay = bes["W"], bes["R"], bes["ay"]
                spec = dict(spec, lead=bes["lead"])
            else:
                W_rest = rest_at(*(bx + out * (step + land_out)))
        ok = True
        # landing: tilted about the horizontal axis across the long axis, one end down, that end's corner on the ground
        ax_t = np.cross(ay, np.array([0.0, 1.0, 0.0]))
        ax_t /= np.linalg.norm(ax_t)
        tilt = math.radians(spec["tilt"])
        Pr = gc(W_rest)
        bot = Pr[(Pr[:, 1] < ground + 1e-3)]
        ends = (bot[np.argmax(bot @ ay)], bot[np.argmin(bot @ ay)])

        def about(pivot, theta):
            """W_rest rocked by theta about the bottom edge through pivot, then set down so its lowest corner is on
            the ground: it rolls on whatever part of it touches, never into the floor."""
            Rt = _d3_exp(ax_t * theta)
            W = np.eye(4)
            W[:3, :3] = Rt
            W[:3, 3] = pivot / 16.0 - Rt @ (pivot / 16.0)
            W = W @ W_rest
            W[1, 3] += (ground - gc(W)[:, 1].min()) / 16.0
            return W
        # landing: one end down, the other lifted by tilt (lead > 0: the end along +yaw lands first)
        tilt = math.radians(spec["tilt"])
        k_lead = 0 if spec.get("lead", 1) > 0 else 1
        lead, trail = ends[k_lead], ends[1 - k_lead]
        # the trailing end must be the raised one
        iw = np.linalg.inv(W_rest)
        pl, pt = iw @ np.r_[lead / 16.0, 1.0], iw @ np.r_[trail / 16.0, 1.0]
        rise = lambda W: (W @ pt)[1] - (W @ pl)[1]
        sg = 1.0 if rise(about(lead, tilt)) >= rise(about(lead, -tilt)) else -1.0
        tipped = lambda theta: about(lead, theta * sg)
        # the clatter: the down end kicks back up off the ground, rocking about the other end
        bounced = lambda theta: about(trail, -theta * sg)
        W_land = tipped(tilt)
        # flight: the centroid leaves with the hand's velocity under a constant acceleration that lands it at W_land;
        # its vertical part is gravity, which sets the flight time
        c0, c1 = cen(held[ir]), cen(W_land)
        dy = c1[1] - c0[1]
        T = (v[1] + math.sqrt(max(0.0, v[1] ** 2 - 2.0 * D3_G * dy))) / D3_G
        acc = 2.0 * (c1 - c0 - v * T) / T ** 2
        rT = _d3_log(W_land[:3, :3] @ Rr.T)
        tc = spec["release"] + T
        t_tip, t_b1, t_b2 = spec.get("tip", 1.6), spec.get("b1", 0.9), spec.get("b2", 1.0)
        rep = {"release": spec["release"], "flight": round(T, 2), "touch": round(tc, 2), "still": round(tc + t_tip + t_b1 + t_b2, 2),
               "v": tuple(np.round(v, 2)), "acc": tuple(np.round(acc, 2)), "spin": round(math.degrees(np.linalg.norm(rT)), 1)}
        def fly(tau, off):
            u = tau / T
            h10, h01, h11 = u ** 3 - 2 * u ** 2 + u, -2 * u ** 3 + 3 * u ** 2, u ** 3 - u ** 2
            r = h10 * T * w + h01 * rT + h11 * rT
            R = _d3_exp(r) @ Rr
            W = np.eye(4)
            W[:3, :3] = R
            c = c0 + v * tau + 0.5 * acc * tau ** 2 + off * math.sin(math.pi * u)
            W[:3, 3] = c / 16.0 - R @ geo.centroid[part]
            W[1, 3] += max(0.0, ground - gc(W)[:, 1].min()) / 16.0     # an end swung low just before touch rides up
            return W
        # in the air it clears the body and the other piece: a sideways swerve that grows and dies away over the
        # flight (zero at the hand and at touch), the smallest that keeps it out
        hand_arm = _d3_desc(GEAR[part]["arm"].replace("_arm", "_forearm"))
        fl = [i for i in range(ir + 1, n) if i / SUB - spec["release"] < T]
        obst_all = [ci for ci in range(len(geo.bone)) if geo.part[ci] != part]
        obst_far = [ci for ci in obst_all if geo.bone[ci] not in hand_arm]
        fboxes = {i: _d3_boxes(geo, Ms[i], obst_far if i / SUB - spec["release"] < 1.5 else obst_all) for i in fl}

        def fpen_of(off):
            worst = (0.0, None)
            for i in fl:
                W = fly(i / SUB - spec["release"], off)
                G = geo.lattice({b: W @ r for b, r in rel.items()}, part)[::2]
                pz = _d3_pen(G, fboxes[i])
                if pz > worst[0]:
                    worst = (pz, i)
            return worst
        off = np.zeros(3)
        base = fpen_of(off)
        if base[0] > 0.15:
            i = base[1]
            Wb = fly(i / SUB - spec["release"], off)
            away = cen(Wb) - geo.corners(Ms[i], geo.cubes["body"]).mean(0)
            away[1] = 0.0
            away /= max(1e-6, np.linalg.norm(away))
            side = np.cross(np.array([0.0, 1.0, 0.0]), v / max(1e-6, np.linalg.norm(v)))
            side[1] = 0.0
            side /= max(1e-6, np.linalg.norm(side))
            up = np.array([0.0, 1.0, 0.0])
            obst_i = obst_far if i / SUB - spec["release"] < 1.5 else obst_all
            _, who = geo.depth(Ms[i], geo.lattice({b: Wb @ r for b, r in rel.items()}, part), obst_i)
            hit = None if who is None else cen(Wb) - geo.corners(Ms[i], [ci for ci in obst_i if geo.bone[ci] == who]).mean(0)
            hit = away if who is None else hit / max(1e-6, np.linalg.norm(hit))
            back = -v / max(1e-6, np.linalg.norm(v))
            best = None
            for d in (hit, away, up, (away + up) / math.sqrt(2.0), side, -side, back):
                for k in np.arange(0.5, 16.01, 0.5):
                    if best is not None and k >= best[0]:
                        break
                    if fpen_of(d * k)[0] <= 0.15:
                        if best is None or k < best[0]:
                            best = (k, d)
                        break
            if best is not None:
                off = best[1] * best[0]
            else:
                ok = False
        if ok:
            break
    hw = _d3_pivot(Ms[ir], hand)
    gp = (W_rest @ np.linalg.inv(held[ir]) @ np.r_[hw / 16.0, 1.0])[:3] * 16.0
    rep["grip_hand"] = round(float(np.linalg.norm((gp - hw)[[0, 2]])), 1)
    rep["grip_hand_end"] = round(float(np.linalg.norm((gp - _d3_pivot(Ms[-1], hand))[[0, 2]])), 1)
    rep["hand_h"] = round(float(hw[1] - ground), 1)
    rep["land_out"], rep["swerve"] = land_out, round(float(np.linalg.norm(off)), 1)
    prev_anim = None
    for i in range(n):
        tick = i / SUB
        if i <= ir:
            continue
        tau = tick - spec["release"]
        if tau < T:
            W = fly(tau, off)
        else:
            s = tau - T
            if s < t_tip:
                W = tipped(tilt * (1.0 - _ease("in", s / t_tip)))
            elif s < t_tip + t_b1:
                W = bounced(math.radians(spec.get("bounce", 3.0)) * _ease("out", (s - t_tip) / t_b1))
            elif s < t_tip + t_b1 + t_b2:
                W = bounced(math.radians(spec.get("bounce", 3.0)) * (1.0 - _ease("in", (s - t_tip - t_b1) / t_b2)))
            else:
                W = W_rest
        for bone in GEAR[part]["bones"]:
            R, pos = _d3_gear_local(Ms[i][hand], W, bone)
            near = prev_anim if prev_anim is not None else (0.0, 0.0, 0.0)
            a = _d3_anim(bone, R, near)
            samples[i][bone] = (a, pos)
        prev_anim = samples[i][GEAR[part]["bones"][0]][0]
    for i in range(ir + 1):
        for bone in GEAR[part]["bones"]:
            samples[i][bone] = (Z3, Z3)
    return rep


def _d3_desc(root):
    out, kids = {root}, True
    while kids:
        kids = False
        for n, b in HEAVY.bones.items():
            if n not in out and b.parent in out:
                out.add(n)
                kids = True
    return out


def _d3_pivot(M, bone):
    return (M[bone] @ np.r_[HEAVY.bones[bone].pivot / 16.0, 1.0])[:3] * 16.0


def _d3_boxes(geo, M, cis):
    """Boxes cis in world px, stacked: rotations, origins, low and high corners."""
    if not cis:
        return None
    A = [geo.box(M, ci) for ci in cis]
    return (np.stack([a[:3, :3] for a in A]), np.stack([a[:3, 3] for a in A]) * 16.0,
            np.stack([geo.lo[ci] for ci in cis]) * 16.0, np.stack([geo.hi[ci] for ci in cis]) * 16.0)


def _d3_pen(P, boxes):
    """Deepest (px) any point of P sits inside any of the boxes."""
    if boxes is None:
        return 0.0
    R, t, lo, hi = boxes
    q = np.einsum("nk,bkj->bnj", P, R) - np.einsum("bk,bkj->bj", t, R)[:, None, :]
    d = np.minimum(q - lo[:, None, :], hi[:, None, :] - q).min(2)
    return max(0.0, float(d.max()))


D3_GAP = ((0, 0.0, 0), (1.0, 0, 0), (-1.0, 0, 0), (0, 0, 1.0), (0, 0, -1.0), (0, 0.75, 0))
# where a let-go piece may lie, nearest first: k px straight out from the hand to its side of the body, j px along it
D3_BESIDE_STEPS = sorted(((float(k), float(j)) for k in np.arange(0.0, 28.01, 0.5) for j in range(-10, 11)),
                         key=lambda kj: math.hypot(*kj))


def _d3_beside(geo, Mr, Mf, Wh, hand, part, rel, gc, ground, zone, extra=0.0):
    """Where a let-go piece lies (v4, Kort 2026-11-06: "drop the sword on the same side he was holding it"): the
    spot the hand let go of it, on the floor, the piece turned as little as it can from how it was held, then moved
    out to the hand's side of the body's midline (hips to head at rest), or along the body, the nearest spot where every
    corner is on that side and it is a clear gap (1 px) off the body and the other piece. Returns its rest matrix, rotation, long axis,
    which end lands first, and how far it was moved out."""
    hw = _d3_pivot(Mr, hand)
    g_loc = (np.linalg.inv(Wh) @ np.r_[hw / 16.0, 1.0])[:3]          # the grip, in the piece's own frame
    hips, head = _d3_pivot(Mf, "hips"), _d3_pivot(Mf, "head")
    a = hips[[0, 2]]
    u = (head - hips)[[0, 2]]
    u /= max(1e-6, np.linalg.norm(u))
    hx = hw[[0, 2]]
    w = hx - a
    sg = 1.0 if u[0] * w[1] - u[1] * w[0] >= 0.0 else -1.0
    nrm = sg * np.array([-u[1], u[0]])                                  # out from the midline, the hand's way
    Rh = Wh[:3, :3]
    aw, zw = Rh[:, 1], Rh[:, 2]
    yaw0 = math.atan2(aw[0], aw[2]) if math.hypot(aw[0], aw[2]) > 0.3 else math.atan2(u[0], u[1])
    f0 = 1.0 if zw[1] >= 0.0 else -1.0
    best = None
    for flip in (f0, -f0):
        for dyaw in (0, 20, -20, 40, -40, 60, -60, 90, -90, 120, -120, 150, -150, 180):
            ay = np.array([math.sin(yaw0 + math.radians(dyaw)), 0.0, math.cos(yaw0 + math.radians(dyaw))])
            R = np.column_stack([np.cross(ay, np.array([0.0, flip, 0.0])), ay, np.array([0.0, flip, 0.0])])
            base = 0.06 * math.degrees(float(np.linalg.norm(_d3_log(R @ Rh.T))))
            if best is not None and base >= best[0]:
                continue
            for k, j in D3_BESIDE_STEPS:
                if best is not None and base + math.hypot(k, j) >= best[0]:
                    break
                t = hx + nrm * (k + extra) + u * j
                W = np.eye(4)
                W[:3, :3] = R
                W[:3, 3] = np.array([t[0], 0.0, t[1]]) / 16.0 - R @ g_loc
                W[1, 3] += (ground - gc(W)[:, 1].min()) / 16.0
                if float(((gc(W)[:, [0, 2]] - a) @ nrm).min()) < 0.0:
                    continue
                G = geo.lattice({b: W @ r for b, r in rel.items()}, part)
                if _d3_pen(G, zone[-1]) > 0.0:                          # cheap first: the body as it ends
                    continue
                if all(_d3_pen(G + np.array(o), z) == 0.0 for z in zone for o in D3_GAP):
                    best = (base + math.hypot(k, j), W, R, ay, k)
                    break
    if best is None:
        raise RuntimeError("no clear rest beside the %s hand" % part)
    _, W, R, ay, k = best
    return {"W": W, "R": R, "ay": ay, "lead": 1 if aw[1] < 0.0 else -1, "out": k + extra}


def d3_clear(samples, geo, part, ground, upto=None):
    """Held gear kept off the ground and out of the body: per sample, the smallest shoulder turn plus wrist turn
    (rotation vectors, solved by pattern search from the previous sample's answer) that lifts the gear and arm
    to the ground and clears the gear out of the body and the other piece, then smoothed over time and written
    into the arm and hand keys. Returns the largest shoulder and wrist turns (deg)."""
    arm, hand = GEAR[part]["arm"], GEAR[part]["hand"]
    others = [p for p in GEAR if p != part]
    chain = _d3_desc(arm)
    n = len(samples)
    stop = n if upto is None else upto + 1
    xs = np.zeros((n, 6))
    x = np.zeros(6)
    cen = []
    for i in range(stop):
        cen.append(geo.corners(geo.fk(samples[i]), geo.cubes[part]).mean(0))
    # after a drop the empty arm is still kept off the floor, easing out of the turns it held the piece with
    for i in range(n):
        held = i < stop
        sp = float(np.linalg.norm(cen[min(i + 1, stop - 1)] - cen[max(i - 1, 0)])) * SUB / 2.0 if held else 0.0
        mg = 0.4 * min(1.0, sp / 1.5) if held else 0.1
        M = geo.fk(samples[i])
        s, w = _d3_pivot(M, arm), _d3_pivot(M, hand)
        G = geo.lattice(M, part)[::3] if held else np.zeros((0, 3))
        K = geo.corners(M, [ci for ci in geo.cubes["body"] if geo.bone[ci] in chain])
        own = [ci for ci in geo.cubes["body"] if geo.bone[ci] in chain and geo.bone[ci] not in GEAR[part]["fist"]]
        rest = [ci for ci in geo.cubes["body"] if geo.bone[ci] not in chain] + [ci for p in others for ci in geo.cubes[p]]
        GK = np.vstack([G, K])
        lo_g, hi_g = GK.min(0) - 40, GK.max(0) + 40
        rest = [ci for ci in rest if np.all(geo.box(M, ci)[:3, 3] * 16.0 > lo_g) and np.all(geo.box(M, ci)[:3, 3] * 16.0 < hi_g)]
        rest = _d3_boxes(geo, M, rest)
        own = _d3_boxes(geo, M, own)

        def cost(v):
            Ra, Rh = _d3_exp(v[:3]), _d3_exp(v[3:])
            Gh = (G - w) @ Rh.T + w
            Gw = (Gh - s) @ Ra.T + s
            Kw = (K - s) @ Ra.T + s
            fl = np.maximum(0.0, ground + mg - np.concatenate([Gw[:, 1], Kw[:, 1]]))
            p1 = max(0.0, _d3_pen(Gw, rest) - 0.25) if len(Gw) else 0.0
            p2 = max(0.0, _d3_pen(Gh, own) - 0.25) if len(Gh) else 0.0
            return (60.0 * (fl.max() ** 2 + 0.2 * (fl ** 2).sum() / 20.0) + 60.0 * (p1 ** 2 + p2 ** 2)
                    + 30.0 * float(v[:3] @ v[:3]) + 12.0 * float(v[3:] @ v[3:]) + 25.0 * float((v - x) @ (v - x)))
        z = np.zeros(6)
        if cost(z) - 25.0 * float(x @ x) < 1e-9 and float(x @ x) < 1e-6:
            x = z
            xs[i] = x
            continue
        v, c = x.copy(), cost(x)
        if cost(z) < c:
            v, c = z, cost(z)
        step = 0.12
        while step > 0.004:
            moved = False
            for k in range(6):
                for sg in (1.0, -1.0):
                    u = v.copy()
                    u[k] += sg * step
                    cu = cost(u)
                    if cu < c:
                        v, c, moved = u, cu, True
            if not moved:
                step *= 0.5
        x = v
        xs[i] = x
    # smooth over time (a short gaussian) so the turns ease in and out
    k = np.exp(-0.5 * (np.arange(-6, 7) / 2.0) ** 2)
    k /= k.sum()

    def smooth(xs):
        pad = np.vstack([np.repeat(xs[:1], 6, 0), xs, np.repeat(xs[-1:], 6, 0)])
        return np.stack([np.convolve(pad[:, j], k, "valid") for j in range(6)], 1)
    # while it still holds the piece the turns see nothing of the empty arm's after the drop; that arm eases over
    held = xs.copy()
    held[stop:] = xs[stop - 1]
    A, B = smooth(held), smooth(xs)
    blend = np.clip((np.arange(n) - stop + 1) / 6.0, 0.0, 1.0)[:, None]
    xs = A * (1.0 - blend) + B * blend
    for i in range(n):
        v = xs[i]
        if float(v @ v) < 1e-10:
            continue
        M = geo.fk(samples[i])
        s, w = _d3_pivot(M, arm) / 16.0, _d3_pivot(M, hand) / 16.0
        Ra, Rh = _d3_exp(v[:3]), _d3_exp(v[3:])
        Ga = np.eye(4); Ga[:3, :3] = Ra; Ga[:3, 3] = s - Ra @ s
        w2 = Ra @ (w - s) + s
        Gh = np.eye(4); Gh[:3, :3] = Rh; Gh[:3, 3] = w2 - Rh @ w2
        Mn_arm = Ga @ M[arm]
        L = np.linalg.inv(M[HEAVY.bones[arm].parent]) @ Mn_arm
        rot, pos = samples[i][arm]
        samples[i][arm] = (_d3_anim(arm, L[:3, :3], rot), pos)
        Mn_hand = Gh @ Ga @ M[hand]
        L = np.linalg.inv(Ga @ M[HEAVY.bones[hand].parent]) @ Mn_hand
        rot, pos = samples[i][hand]
        samples[i][hand] = (_d3_anim(hand, L[:3, :3], rot), pos)
    return (round(math.degrees(np.linalg.norm(xs[:, :3], axis=1).max()), 1),
            round(math.degrees(np.linalg.norm(xs[:, 3:], axis=1).max()), 1))


# Struck from the front: it reels a step back onto the right foot, catches there a beat, then goes over
# backwards stiff as a plank about that heel, the arms trailing up; they slap down after the back lands.
D3_BACK = [
    (3, "out", {"root": (-5, 0, 0), "torso": (-12, 0, 0), "head": (-20, 0, 0),
                "right_leg": (16, 0, 4), "right_shin": (22, 0, 0), "left_leg": (-8, 0, -3), "left_shin": (12, 0, 0),
                "right_arm": (-40, 0, 28), "right_forearm": (-30, 0, 0), "left_arm": (-30, 30, -14)}),
    (6, "io", {"root": (-12, 0, 0), "torso": (-8, 0, 0), "head": (-12, 0, 0),
               "right_leg": (20, 0, 4), "right_shin": (4, 0, 0), "left_leg": (-16, 0, -3), "left_shin": (8, 0, 0)}),
    (9, "io", {"right_arm": (-80, 0, 40), "left_arm": (-60, 20, -30), "right_forearm": (-20, 0, 0)}),
    (13, "in", {"root": (-88, 0, 0), "torso": (-4, 0, 0), "head": (6, 0, 0),
                "right_leg": (-2, 0, 5), "right_shin": (6, 0, 0), "left_leg": (-12, 0, -5), "left_shin": (14, 0, 0),
                "right_foot": (18, 0, 0), "left_foot": (14, 0, 0), "cape": (6, 0, 0)}),
    (15.5, "out", {"head": (14, 22, 0)}),
]
D3_BACK_ARMS = {"right": (14.5, (33.3, 47.8, 72.3), (-4.6, 0, 0), (6.9, 42.7, -21.5)),
                "left": (15.5, (22.5, -50.8, -61.6), (-4.7, 0, 0), (128.9, 23.7, -57.8))}

# Struck from behind: the knees buckle and it drops onto them, sags forward and folds onto its face, the legs
# laid out behind as the hips go down; the arms land a beat after the chest.
D3_FORWARD = [
    (2, "out", {"root": (5, 0, 0), "torso": (10, 0, 0), "head": (-12, 0, 0), "right_arm": (12, 0, 14), "left_arm": (-6, 34, -10),
                "right_leg": (-12, 0, 4), "left_leg": (-12, 0, -4), "right_shin": (40, 0, 0), "left_shin": (44, 0, 0)}),
    (6, "in", {"root": (0, 0, 0), "hips": (0, 0, 0), "torso": (6, 0, 0), "head": (16, 0, 0),
               "right_leg": (0, 0, 4), "left_leg": (0, 0, -4), "right_shin": (92, 0, 0), "left_shin": (92, 0, 0),
               "right_foot": (30, 0, 0), "left_foot": (30, 0, 0)}),
    (8.5, "io", {"root": (8, 0, 0), "torso": (14, 0, 0), "head": (24, 0, 0), "right_shin": (90, 0, 0), "left_shin": (90, 0, 0),
                 "right_arm": (-15, 0, 14), "left_arm": (-10, 25, -14)}),
    (10.5, "io", {"right_arm": (-20, -20, 60), "left_arm": (-15, 10, -60), "right_forearm": (-20, 0, 0), "left_forearm": (-25, 0, 0)}),
    (13, "in", {"root": (84, -10, 0), "torso": (8, 0, 0), "head": (4, 0, 0),
                "right_leg": (-2, 0, 4), "left_leg": (-2, 0, -4), "right_shin": (10, 0, 0), "left_shin": (10, 0, 0),
                "right_foot": (60, 0, 0), "left_foot": (60, 0, 0), "cape": (10, 0, 0)}),
    (15.5, "out", {"head": (-8, 30, 0)}),
]
D3_FORWARD_ARMS = {"right": (14, (-28.1, -40.3, 65.6), (-4.7, 0, 0), (-60, 15, -60)),     # blade out along his right
                   "left": (15, (-5.8, 7.8, -64.7), (-29.9, 0, 0), (35.8, -6.1, -7.3))}

# Struck from its right: the blow twists the chest round, both knees give, and it crumples onto its left side,
# turned a little toward face down, the legs folded.
D3_LEFT = [
    (2, "out", {"root": (0, -8, 6), "torso": (0, -12, 12), "head": (0, -14, 16),
                "left_leg": (-10, 0, -4), "left_shin": (30, 0, 0), "right_leg": (-4, 0, 2), "right_shin": (16, 0, 0),
                "right_arm": (-30, 0, 35)}),
    (6, "io", {"root": (2, -10, 14), "torso": (0, -10, 8), "head": (-6, -8, 10),
               "left_leg": (-28, 0, -5), "left_shin": (60, 0, 0), "right_leg": (-20, 0, 2), "right_shin": (50, 0, 0)}),
    (12.5, "in", {"root": (8, -20, 86), "hips": (0, 0, 0), "torso": (4, 0, 2), "head": (4, 0, -12),
                  "left_leg": (-46, 0, -6), "left_shin": (70, 0, 0), "right_leg": (-38, 0, 4), "right_shin": (55, 0, 0),
                  "right_foot": (20, 0, 0), "left_foot": (20, 0, 0)}),
    (15, "out", {"head": (10, -14, -18)}),
]
D3_LEFT_ARMS = {"right": (14, (-46.8, 32.2, -52.7), (-10.8, 0, 0), (21.5, -81.2, 14.2)),
                "left": (13.5, (-55.5, -1.7, 28.6), (-33.7, 0, 0), (58.2, -23, -51.8))}

D3_RIGHT = mirror(D3_LEFT)
D3_RIGHT_ARMS = {"right": (13.5, (-96.4, 1.4, -4), (-8.9, 0, 0), (2.4, 6.1, -21.8)),
                 "left": (14, (-41.5, 9.2, 67.7), (-12.3, 0, 0), (34.4, -36.7, -54.3))}

D3_DIRS = {"back": (D3_BACK, D3_BACK_ARMS), "forward": (D3_FORWARD, D3_FORWARD_ARMS),
           "left": (D3_LEFT, D3_LEFT_ARMS), "right": (D3_RIGHT, D3_RIGHT_ARMS)}


def d3_keys(direction, arms=None, keys=None, rest=None):
    if keys is None:
        keys, rest = D3_DIRS[direction]
    rest = dict(rest, **(arms or {}))
    out = list(keys)
    for side, (t, arm, fore, hand) in rest.items():
        out.append((t, "in", {side + "_arm": arm, side + "_forearm": fore}))
        out.append((t - 1.0, "io", {side + "_hand": hand}))
    return out


# Dropped gear: the tick it leaves the hand and how it lands (tilt deg, bounce, tip, clatter b1/b2); "beside" lays it down
# flat on the side of the hips-head midline the releasing hand is on.
SWORD_DROPS = {
    "back": {"release": 13.0, "beside": True, "tilt": 12, "bounce": 3.0, "tip": 1.2, "b1": 0.6, "b2": 0.7},
    "forward": {"release": 12.0, "beside": True, "tilt": 10, "bounce": 3.0, "tip": 1.2, "b1": 0.6, "b2": 0.7},
    "left": {"release": 12.75, "beside": True, "tilt": 12, "bounce": 3.0, "tip": 0.9, "b1": 0.45, "b2": 0.55},
    "right": {"release": 11.75, "beside": True, "tilt": 10, "bounce": 3.0, "tip": 1.2, "b1": 0.6, "b2": 0.7},
}
SHIELD_DROPS = {
    "back": {"release": 14.0, "beside": True, "tilt": 10, "bounce": 2.5, "tip": 1.0, "b1": 0.5, "b2": 0.6},
    "forward": {"release": 12.25, "beside": True, "tilt": 10, "bounce": 2.5, "tip": 1.0, "b1": 0.5, "b2": 0.6},
    "left": {"release": 13.0, "beside": True, "tilt": 10, "bounce": 2.5, "tip": 1.0, "b1": 0.5, "b2": 0.6},
    "right": {"release": 13.5, "beside": True, "tilt": 10, "bounce": 2.5, "tip": 0.8, "b1": 0.45, "b2": 0.5},
}
DIRECTIONS = ("back", "forward", "left", "right")
D3_REPORT = {}


def d3_death_clips(start, drops=None, arms=None, dirs=None):
    """The twelve falls, baked; each holds its last frame. start: idle frame 0 per death bone.
    drops: {piece: {direction: spec}} (default SWORD_DROPS / SHIELD_DROPS); arms: {direction: {side: (tick, arm,
    forearm, wrist)}} replaces that side's resting arm; dirs: {direction: (keys, arms)} replaces the body keys."""
    drops = drops or {p: (SWORD_DROPS if p == "sword" else SHIELD_DROPS) for p in GEAR}
    dirs = dirs or D3_DIRS
    pieces = list(GEAR)
    geo = D3Geo(HEAVY)
    out = {}
    for d in DIRECTIONS:
        variants = (("", {}), ("_drop1", {pieces[0]: drops[pieces[0]][d]}),
                    ("_drop2", {p: drops[p][d] for p in pieces}))
        for suf, dr in variants:
            keys, rest = dirs[d]
            sm, ground = d3_body(d3_keys(d, (arms or {}).get(d), keys, rest), start, geo)
            for part in pieces:
                up = int(round(dr[part]["release"] * SUB)) if part in dr else None
                d3_clear(sm, geo, part, ground, up)
                d3_clear(sm, geo, part, ground, up)
            for part, spec in dr.items():
                D3_REPORT[(d + suf, part)] = d3_drop(sm, geo, part, spec, ground)
            c = bake(sm, DEATH_TICKS, loop=False)
            c["loop"] = "hold_on_last_frame"
            out["death_" + d + suf] = c
    return out
