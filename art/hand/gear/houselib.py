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
  a box inflated by 0.1 or more grows by one whole pixel (two from 0.4) and is painted at that size by its own painters;
  smaller inflates (seam separators) are dropped. A box grown through the ground sits on it. The left leg's boxes stand
  0.04 px forward so the two legs' armour never share a plane where it overlaps at the crotch.
- Rarity: the source's own ladder (Common, Uncommon green trim, Rare blued, Epic violet-black, Legendary ember with its
  own geometry). A source box of the Common-to-Epic tier shows at rarities 1 to 4, a Legendary one at 5.
- Side colour: a box with any team texel draws in the side's cloth sheet; it is split by rarity where its other texels
  change with rarity. The source's four team colours keep their hexes; company and linen take handlib's ramps' base.
- Glow: texels in a glowing material (ember) of skin boxes go to the glow sheet. Ember inside a team box stays painted
  but is not lit (the cloth pass has no glow sheet); cloth_glow() counts them.
"""
import copy, functools, math, os, sys
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
LEFT_LEG_NUDGE = 0.04
TEAMS = dict(C.TEAMS)
for _s in ("company", "linen"): TEAMS[_s] = "#%02X%02X%02X" % HAND_TEAM[_s][0][2][:3]

def tier_of(r): return "legendary" if r == 5 else "base"
def rarities_of(b): return {"all": (1, 2, 3, 4, 5), "base": (1, 2, 3, 4), "legendary": (5,)}[b.tier]

def grow(g):
    """Whole pixels a source inflate becomes: 0 for a seam separator, 1 for a layer, 2 for a layer over a layer."""
    return 0 if g < 0.1 else 1 if g < 0.4 else 2

def converted_boxes(source):
    """The source boxes at whole-pixel geometry (no inflate), as source-space engine Boxes."""
    out = []
    for b in source.boxes:
        n = copy.copy(b); k = grow(b.inflate)
        n.frm = [v - k / 2 for v in b.frm]; n.size = [v + k for v in b.size]; n.origin = list(b.origin); n.inflate = 0.0
        n.paint, n.hide = list(b.paint), set(b.hide)
        if k and n.frm[1] < 0 <= b.frm[1] + 1e-6: n.frm[1] = 0.0     # grown through the ground: stand on it
        if n.bone == "left_leg": n.frm[2] -= LEFT_LEG_NUDGE; n.origin[2] -= LEFT_LEG_NUDGE
        out.append(n)
    return out

@functools.lru_cache(maxsize=None)
def armour_set(name):
    """The set as the gear frame draws it: the source ArmourSet rebuilt on the converted boxes."""
    import importlib
    src = importlib.import_module("house." + name).build()
    def pal(rarity, team):   # the source palette with any of the six sides
        if team in C.TEAMS: return src.palette_fn(rarity, team)
        p = src.palette_fn(rarity, "crimson"); p.add("team", TEAMS[team], 0.20, clamp=False); return p
    return E.ArmourSet(src.id, src.title, src.house, pal, converted_boxes(src), src.notes)

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
    return pivot, rot, Box(origin, list(box.size), paint, sheet=sheet)

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
