"""The five House armour sets Kort approved on 2026-11-04 (Oathbound, Barrow Warden, Thornhide, Forgewall, Tidewrought),
brought into the commander gear frame. ART ONLY: no stats, catalogue rows or drops until Kort answers question #113
(docs/drafts/house-gear-sets-2026-11-04.md on main); their ids sit in ids.json's art_only list.

The sets are authored in house/ (copied unchanged from claude/gifted-ptolemy-j2rv7i at 5ff7ed4c, tools/player_armor/):
each box with its painters, in Blockbench space (y up, feet at 0, the wearer's right at +x), each rarity and team from
one palette. This module turns one into four GearPieces (helm, chest, legs, boots):

- Space: a source point (x, y, z) is (-x, 24 - y, z) on the mannequin (y down, the wearer's right at -x); a source
  rotation (rx, ry, rz) is (-rx, -ry, rz). Each source box is its own bone on the biped part it names.
- Texels: every face texel is looked up from the source canvas through both frames, so each face reads as the source
  painted it, at one texel per model pixel.
- One texel per pixel: the source separated layers with `inflate` (a fraction of a pixel that stretches the texels). Here
  each box grows by its inflate rounded to whole pixels and is painted at that size by its own painters; a box grown
  through the ground sits on it. Faces left sharing a plane are parted by the source's own layer order: a box that grew
  past where the source had it gives the pixel back, else the outer box is lifted 0.02 px at a time (dilation, at most
  0.1 px), so texels stay one per pixel to within a few hundredths (converted_boxes).
- Rarity: the source's own ladder (Common, Uncommon green trim, Rare blued, Epic violet-black, Legendary ember with its
  own geometry). A source box of the Common-to-Epic tier shows at rarities 1 to 4, a Legendary one at 5.
- Side colour: a box with any team texel draws in the side's cloth sheet; it is split by rarity where its other texels
  change with rarity. The source's four team colours keep their hexes; company and linen take handlib's ramps' base.
- Glow: texels in a glowing material (ember) of skin boxes go to the glow sheet. Ember inside a team box stays painted
  but is not lit (the cloth pass has no glow sheet); cloth_glow() counts them.
"""
import copy, functools, itertools, math, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE); sys.path.insert(0, os.path.join(HERE, "..", "common"))
from gearlib import GearPiece, RARITIES
from handlib import Box, Bone, pack, TEAM as HAND_TEAM
from house import engine as E, common as C

ORDER = ("oathbound", "barrow_warden", "thornhide", "forgewall", "tidewrought")
SLOT = {"helmet": "helm", "chest": "chest", "legs": "legs", "boots": "boots"}
RAR = dict(zip(RARITIES, C.RARITIES))                      # 1 -> 'common' ... 5 -> 'legendary'
WORLD = {"head": (0, 0, 0), "body": (0, 12, 0), "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0),
         "right_leg": (-1.9, 12, 0), "left_leg": (1.9, 12, 0)}   # each biped part's frame on the mannequin
FACE = {"front": "north", "back": "south", "right": "east", "left": "west", "top": "up", "bottom": "down"}
TEAMS = dict(C.TEAMS)
for _s in ("company", "linen"): TEAMS[_s] = "#%02X%02X%02X" % HAND_TEAM[_s][0][2][:3]

def tier_of(r): return "legendary" if r == 5 else "base"
def rarities_of(b): return {"all": (1, 2, 3, 4, 5), "base": (1, 2, 3, 4), "legendary": (5,)}[b.tier]

def grow(g):
    """Whole pixels a source inflate starts as: the nearest whole growth (0.25 and up is one pixel)."""
    return int(math.floor(2 * g + 0.5)) if g >= 0.1 else 0

EPS, MAX_SEP = 0.02, 0.1
OUTWARD = {"north": (2, -1), "south": (2, 1), "west": (0, -1), "east": (0, 1), "down": (1, -1), "up": (1, 1)}

def _shape(b, k, e):
    n = copy.copy(b)
    n.frm = [v - k / 2 for v in b.frm]; n.size = [v + k for v in b.size]; n.origin = list(b.origin); n.inflate = e
    n.paint, n.hide = list(b.paint), set(b.hide)
    if k and n.frm[1] < 0 <= b.frm[1] + 1e-6: n.frm[1] = 0.0         # grown through the ground: stand on it
    return n

def _face(b, f):
    ax, sign = OUTWARD[f]; lo, hi = b.lo_hi()
    return sign * (hi[ax] if sign > 0 else lo[ax])                     # larger is further out

# Head clearance (brief: nothing clips the head). Source boxes whose tops rose above the shoulder band (y 25) into the
# helm at rest are lowered, in px, before conversion; both sides move alike, so mirrored _l boxes take the same shift.
LOWER = {
    "forgewall": {"slab_r": 1.2, "slab_l": 1.2, "gorget": 1.0},
    "tidewrought": {"shell_hump_r": 0.2, "shell_hump_l": 0.2},
    "barrow_warden": {"skull_pauldron": 1.0, "skull_lame": 1.0, "skull_horn_back": 1.0, "skull_horn_tip": 1.0,
                      "skull_horn_front": 1.0},
}

# Head clearance with the head turned (houseclear.YAWS, up to 50 degrees): a turned helm sweeps a circle of its corner
# radius (about 7.1 px, 7.8 under Barrow Warden's hood), so shoulder gear over the band moves out past it by (dx, dz) px
# in source space; a _r entry mirrors onto its _l box. Forgewall's smokestacks also drop 2 px (LOWER) so their mouths
# sit under the helm's top, and stand 2 px further back, clear of the turned helm.
SHIFT = {
    "barrow_warden": {"skull_horn_back": (1.5, 0), "skull_horn_tip": (1.5, 0), "skull_horn_front": (1.5, 0),
                      "spaulder_spike_0": (-1.0, 0), "spaulder_spike_1": (-1.0, 0), "spaulder_spike_2": (-1.0, 0)},
    "thornhide": {"pad_spike_a": (2.0, 0)},
    "forgewall": {"stack_r": (0, 2.0)},
    "tidewrought": {"coral_stem_r": (2.0, 0), "coral_out_r": (2.0, 0), "coral_in_r": (2.0, 0), "coral_back_r": (2.0, 0)},
}
LOWER["forgewall"].update({"stack_r": 2.0, "stack_l": 2.0})

# Legendary silhouette (the 1.06 rule, test_legendary_silhouette_grows: Legendary covers over 6% more of the front and
# side views than Common). Legendary-only boxes added to the source, each a copy of a source box (its painters, tier,
# slot and bone) at a new (from, size); a _r box gets its mirror.
EXTRA = {
    "oathbound": [("wing_out_r", "wing1_r", (10, 25.5, 0.5), (1, 3, 2)),      # an outer feather row on each wing
                  ("wing4_r", "wing3_r", (9, 29.5, 1.5), (1, 1, 1))],         # and one more step at its tip
    "barrow_warden": [("crown_spike_side_r", "crown_spike_0", (5, 34.2, -2), (1, 3, 1)),   # spikes on the crown's sides
                      ("drape_flare", "drape_long", (-3.5, 3, 4.1), (7, 8, 1))],             # the torn drape lifts at the hem
    "thornhide": [("cloak_tail", "cloak_long", (-1.5, 1, 4.1), (3, 12, 1))],                 # a wolf's tail down the cloak
    "forgewall": [("drop_low_r", "drop_r", (9, 16, -4), (2, 3, 8))],                    # the slab's outer plate hangs lower
    "tidewrought": [("cloak_billow", "cloak_long", (-3.5, 2, 4.1), (7, 12, 1))],           # the sea-cloak billows out
}

def _extras(name, boxes):
    by, out = {b.name: b for b in boxes}, []
    for new, src, frm, size in EXTRA.get(name, ()):
        b = copy.copy(by[src]); b.name = new; b.tier = "legendary"
        b.frm = [float(v) for v in frm]; b.size = [float(v) for v in size]
        b.origin = [b.frm[i] + b.size[i] / 2 for i in range(3)]; b.paint = list(b.paint); b.hide = set()
        out.append(b)
        if new.endswith("_r"): out.append(b.mirrored())
    return out

# Right-side boxes narrowed to a new outer-x span (x0, x1) so a turned helm clears them; _l boxes mirror it.
SPAN = {"oathbound": {"pauldron_dome_r": (7.2, 10.2)}}

def _lowered(name, boxes):
    out = []
    for b in boxes:
        d = LOWER.get(name, {}).get(b.name)
        if d:
            b = copy.copy(b); b.frm = list(b.frm); b.origin = list(b.origin)
            b.frm[1] -= d; b.origin[1] -= d
        sh = SHIFT.get(name, {}).get(b.name)
        if sh is None and b.name.endswith("_l"):
            sh = SHIFT.get(name, {}).get(b.name[:-2] + "_r"); sh = None if sh is None else (-sh[0], sh[1])
        if sh:
            b = copy.copy(b); b.frm = list(b.frm); b.origin = list(b.origin)
            b.frm[0] += sh[0]; b.origin[0] += sh[0]; b.frm[2] += sh[1]; b.origin[2] += sh[1]
        sp = SPAN.get(name, {}).get(b.name) or SPAN.get(name, {}).get(b.name[:-2] + "_r" if b.name.endswith("_l") else "")
        if sp:
            x0, x1 = sp if b.name.endswith("_r") else (-sp[1], -sp[0])
            b = copy.copy(b); b.frm = list(b.frm); b.size = list(b.size); b.origin = list(b.origin)
            b.frm[0], b.size[0], b.origin[0] = x0, round(x1 - x0, 6), (x0 + x1) / 2
        out.append(b)
    return out

def touching(boxes, eps=1e-4):
    """Pairs of unrotated boxes whose opposite faces meet in one plane over some area: armour renders without back-face
    culling, so these z-fight too (the engine's coplanar() only pairs faces that point the same way)."""
    flat, out = [(b, b.lo_hi()) for b in boxes if not any(b.rot)], []
    for (a, (la, ha)), (b, (lb, hb)) in itertools.combinations(flat, 2):
        for ax in range(3):
            if abs(ha[ax] - lb[ax]) > eps and abs(hb[ax] - la[ax]) > eps: continue
            if all(min(ha[i], hb[i]) - max(la[i], lb[i]) > 0.02 for i in range(3) if i != ax): out.append((a.name, b.name))
    return out

def converted_boxes(source, name=None):
    """The source boxes at whole-pixel geometry, as source-space engine Boxes whose `inflate` is only a separator.

    Each box grows by its source inflate rounded to whole pixels. Where two faces then share a plane (they would
    z-fight), the one the source drew further out wins: if the inner one grew past where the source had it, it gives the
    pixel back; otherwise the outer one is lifted by 0.02 px (at most 0.1 px), which keeps the texels one per pixel to
    within a few hundredths."""
    src = {b.name: b for b in _lowered(name, list(source.boxes) + _extras(name, source.boxes))}
    k = {n: grow(b.inflate) for n, b in src.items()}
    e = {n: 0.0 for n in src}
    for _ in range(200):
        boxes = [_shape(src[n], k[n], e[n]) for n in src]
        by = {b.name: b for b in boxes}
        clash = []
        for tier in ("base", "legendary"):
            for a, b, f in E.coplanar([x for x in boxes if x.in_tier(tier)] + BODY):
                if not (a.startswith("p_") and b.startswith("p_")): clash.append((a, b, f))
        if not clash:
            touch = [t for tier in ("base", "legendary")
                     for t in touching([x for x in boxes if x.in_tier(tier)])]
            if not touch: return boxes
            a, b = touch[0]; lift = a if e[a] <= e[b] else b   # either one rising a hair makes them overlap instead
            if e[lift] + EPS > MAX_SEP + 1e-9: raise SystemExit("cannot separate %s and %s (touching)" % (a, b))
            e[lift] = round(e[lift] + EPS, 4); continue
        a, b, f = clash[0]
        sa = _face(src.get(a) or BODY_BY[a], f); sb = _face(src.get(b) or BODY_BY[b], f)
        if sa == sb: outer, inner = (a, b) if b.startswith("p_") else (b, a)
        else: outer, inner = (a, b) if sa > sb else (b, a)
        if outer.startswith("p_"): raise SystemExit("%s sits inside the body at %s's %s face" % (inner, outer, f))
        if not inner.startswith("p_") and k[inner] and _face(by[inner], f) > _face(src[inner], f) + 1e-6: k[inner] -= 1
        elif e[outer] + EPS <= MAX_SEP + 1e-9: e[outer] = round(e[outer] + EPS, 4)
        else: raise SystemExit("cannot separate %s and %s (%s)" % (a, b, f))
    raise SystemExit("separation did not settle")

@functools.lru_cache(maxsize=None)
def armour_set(name):
    """The set as the gear frame draws it: the source ArmourSet rebuilt on the converted boxes."""
    import importlib
    src = importlib.import_module("house." + name).build()
    def pal(rarity, team):   # the source palette with any of the six sides
        if team in C.TEAMS: return src.palette_fn(rarity, team)
        p = src.palette_fn(rarity, "crimson"); p.add("team", TEAMS[team], 0.20, clamp=False); return p
    return E.ArmourSet(src.id, src.title, src.house, pal, converted_boxes(src, name), src.notes)

@functools.lru_cache(maxsize=None)
def palette(name, r, team): return armour_set(name).palette_fn(RAR[r], team)

def canvases(name, r): return armour_set(name).paint(tier_of(r))

def colour(name, r, team, mat, tone):
    t = palette(name, r, team).tones[mat][int(tone)]
    return (int(round(t[0])), int(round(t[1])), int(round(t[2])), 255)

def to_source(face, s, t):
    """The source face and its (u, v) in 0..1 for a mannequin box face's point (s, t) (render.faces_of)."""
    p = {"front": (s, t, 0), "back": (1 - s, t, 1), "right": (0, t, 1 - s), "left": (1, t, s),
         "top": (s, 0, 1 - t), "bottom": (s, 1, t)}[face]
    q = (1 - p[0], 1 - p[1], p[2])                                       # mannequin -> source, box-normalised
    f = FACE[face]
    o, u, v = {"north": ((1, 1, 0), (-1, 0, 0), (0, -1, 0)), "south": ((0, 1, 1), (1, 0, 0), (0, -1, 0)),
               "east": ((1, 1, 1), (0, 0, -1), (0, -1, 0)), "west": ((0, 1, 0), (0, 0, 1), (0, -1, 0)),
               "up": ((0, 1, 0), (1, 0, 0), (0, 0, 1)), "down": ((0, 0, 1), (1, 0, 0), (0, 0, -1))}[f]
    d = [q[i] - o[i] for i in range(3)]
    return f, sum(d[i] * u[i] for i in range(3)), sum(d[i] * v[i] for i in range(3))

def face_grid(name, box, r, team, w, h, face, keep=None):
    """A mannequin face's texel grid from the source canvas (None where the source leaves it clear or hides the face).
    keep(mat) -> bool filters the texels (the glow sheet keeps only lit ones)."""
    g = [[None] * w for _ in range(h)]
    cv = canvases(name, r)
    for y in range(h):
        for x in range(w):
            f, a, b = to_source(face, (x + 0.5) / w, (y + 0.5) / h)
            c = cv.get((box.name, f))
            if c is None: continue
            u, v = min(c.w - 1, int(a * c.w)), min(c.h - 1, int(b * c.h))
            m = c.mat[v, u]
            if m is None or (keep and not keep(m)): continue
            g[y][x] = colour(name, r, team, m, c.tone[v, u])
    return g

def painter(name, box, r, team="crimson", keep=None):
    return lambda face, w, h: face_grid(name, box, r, team, w, h, face, keep)

def has_team(name, box):
    for r in rarities_of(box):
        for f in E.FACES:
            c = canvases(name, r).get((box.name, f))
            if c is not None and any(m == "team" for m in c.mat.flat): return True
    return False

def team_groups(name, box):
    """The rarities of a team box, grouped where its painted faces are the same (one cloth bone per group)."""
    groups = {}
    for r in rarities_of(box):
        key = []
        for f in E.FACES:
            c = canvases(name, r).get((box.name, f))
            if c is None: continue
            key.append(tuple(None if m is None else (m, int(t)) if m == "team" else colour(name, r, "crimson", m, t)
                             for m, t in zip(c.mat.flat, c.tone.flat)))
        groups.setdefault(tuple(key), []).append(r)
    return list(groups.values())

def mc_point(p): return (-p[0], 24 - p[1], p[2])

def gear_box(box, paint, sheet="skin"):
    """(pivot, rotation, origin) in the parent part's frame, and the handlib Box."""
    lo = mc_point((box.frm[0] + box.size[0], box.frm[1] + box.size[1], box.frm[2]))
    w = WORLD[box.bone]
    if any(box.rot):
        o = mc_point(box.origin)
        pivot, rot, origin = [o[i] - w[i] for i in range(3)], [-box.rot[0], -box.rot[1], box.rot[2]], [lo[i] - o[i] for i in range(3)]
    else:
        pivot, rot, origin = [0, 0, 0], [0, 0, 0], [lo[i] - w[i] for i in range(3)]
    return pivot, rot, Box(origin, list(box.size), paint, dil=box.inflate, sheet=sheet)

def stages(rs): return [] if tuple(rs) == RARITIES else list(rs)

def bones_of(name, slot):
    out = []
    for b in armour_set(name).boxes:
        if SLOT[b.slot] != slot: continue
        if not has_team(name, b):
            pivot, rot, hb = gear_box(b, {r: painter(name, b, r) for r in rarities_of(b)})
            out.append(Bone(b.name, b.bone, pivot, [hb], stages=stages(rarities_of(b)), tag=slot, rot=rot))
            continue
        groups = team_groups(name, b)
        for i, rs in enumerate(groups):
            pivot, rot, hb = gear_box(b, (name, b.name, rs[0]), sheet="cloth")
            out.append(Bone(b.name + ("" if len(groups) == 1 else "_%d" % i), b.bone, pivot, [hb], stages=stages(rs), tag=slot, team=True, rot=rot))
    return out

def source_box(name, box_name):
    for b in armour_set(name).boxes:
        if b.name == box_name: return b
    raise KeyError(box_name)

def cloth_painter(name):
    def make(spec, side):
        _, box_name, r = spec
        return painter(name, source_box(name, box_name), r, side)
    return make

def glow_cb(name):
    def glow(bone, b, r):
        src = source_box(name, bone.name)
        lit = palette(name, r, "crimson").glow
        if not any(m in lit for c in (canvases(name, r).get((src.name, f)) for f in E.FACES) if c is not None for m in c.mat.flat): return None
        return painter(name, src, r, keep=lambda m: m in lit)
    return glow

def fit(boxes, sizes=((32, 16), (32, 32), (64, 32), (64, 64), (128, 64), (128, 128), (256, 128), (256, 256))):
    """The smallest sheet the boxes pack onto."""
    from handlib import footprint
    for s in sizes:
        if any(footprint(b)[0] > s[0] for b in boxes): continue      # pack does not catch a box wider than the sheet
        try: pack(boxes, s); return s
        except SystemExit: continue
    raise SystemExit("no sheet fits")

def pieces(name):
    out = []
    for slot in ("helm", "chest", "legs", "boots"):
        mk = functools.partial(bones_of, name, slot)
        B = mk()
        sk = [b for bone in B for b in bone.boxes if b.sheet == "skin"]
        cl = [b for bone in B for b in bone.boxes if b.sheet == "cloth"]
        out.append(GearPiece("%s_%s" % (name, slot), slot, mk, cloth_painter=cloth_painter(name),
                             skin=fit(sk), cloth=fit(cl) if cl else (64, 32), glow=glow_cb(name)))
    return out

# ---- checks the source build ran, on the converted geometry ------------------------------------------
BODY = [E.Box("p_head", "skin", "head", (-4, 24, -4), (8, 8, 8), []), E.Box("p_body", "skin", "body", (-4, 12, -2), (8, 12, 4), []),
        E.Box("p_arm_r", "skin", "right_arm", (4, 12, -2), (4, 12, 4), []), E.Box("p_arm_l", "skin", "left_arm", (-8, 12, -2), (4, 12, 4), []),
        E.Box("p_leg_r", "skin", "right_leg", (-0.1, 0, -2), (4, 12, 4), []), E.Box("p_leg_l", "skin", "left_leg", (-3.9, 0, -2), (4, 12, 4), [])]
BODY_BY = {b.name: b for b in BODY}

def coplanar(name):
    """Faces that share a plane and would z-fight, in each tier, the armour against itself and the wearer's body."""
    s = armour_set(name); out = []
    for tier in ("base", "legendary"):
        for a, b, f in E.coplanar(s.boxes_for(tier) + BODY):
            if not (a.startswith("p_") and b.startswith("p_")): out.append((tier, a, b, f))
    return out

def cloth_glow(name):
    """Lit texels painted inside team boxes (drawn unlit in game)."""
    n = 0
    for b in armour_set(name).boxes:
        if not has_team(name, b): continue
        for r in rarities_of(b):
            lit = palette(name, r, "crimson").glow
            n += sum(1 for f in E.FACES if canvases(name, r).get((b.name, f)) is not None
                     for m in canvases(name, r)[(b.name, f)].mat.flat if m in lit)
    return n
