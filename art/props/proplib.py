"""Shared ground for the hand-authored props (art/props/build_props.py): 5-tone ramps made the style guide's way,
world-aligned material painters, one texture sheet per prop at 1 texel per model px, and the exports (Java item model per
part, a Blockbench .bbmodel per prop, the sheet PNG).

Frame (fixed with SYSTEMS, art/props/README.md): model px, origin = min corner of the prop's anchor (pivot) cell,
x east, y up, z south, the prop's front faces north (-z). A part stays inside -16..32; when it can't, it is stored
shifted and its display.fixed translation shifts it back.

Light: the paint carries the troop light (top, front, the viewer's left). Seen from the front (from the north), the
viewer's left is east, so north and east are the lit faces, south and west the shade faces (handlib LIT).
"""
import base64, colorsys, io, json, math, os, uuid
from PIL import Image

# ---- ramps ---------------------------------------------------------------------------------------------------
def hx(s):
    s = s.lstrip("#"); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)

def _toward(h, target, step):
    d = ((target - h + 180) % 360) - 180
    return (h + max(-abs(step), min(abs(step), d))) % 360

def make_ramp(base, vstep=0.22):
    """Style guide section 1: 5 tones, 0 shadow .. 4 highlight, tone 2 the base. Value +-vstep a step, shadows turn
    toward 268 deg, lights toward 52 deg, 6 deg a step; darks gain 0.04 sat a step, lights lose 0.07."""
    r, g, b, _ = hx(base)
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255); h *= 360
    out = []
    for k in range(-2, 3):
        hh, ss = h, s
        for _ in range(abs(k)):
            hh = _toward(hh, 268 if k < 0 else 52, 6)
        ss = max(0.0, min(1.0, s + (0.04 * -k if k < 0 else -0.07 * k)))
        vv = max(0.06, min(1.0, v * (1 + vstep * k)))
        if vv > 0.60: ss = min(ss, 0.44)
        rr, gg, bb = colorsys.hsv_to_rgb(hh / 360, ss, vv)
        out.append((round(rr * 255), round(gg * 255), round(bb * 255), 255))
    return out

def fixed(*cs): return [hx(c) for c in cs]

# style guide table ramps (Gilded) and props' own
STEEL = fixed("3A3E4B", "5A616E", "7C8591", "A6ADB4", "D6D6D7")
GILT = fixed("482E16", "6F5026", "96783A", "BDAA6A", "E4D780")
WOOD = fixed("3E261F", "563C2E", "6E5440", "867057", "9E8E72")
SWOOD = fixed("301B16", "3E261F", "563C2E", "6E5440", "867057")
LINEN = fixed("82715E", "A0937B", "BFB59A", "DED9C2", "FCFAEF")
LEATHER = fixed("321A15", "462B20", "5A3E2C", "6E553D", "826E52")
IRON = make_ramp("5E636E")            # dull blacksmith iron: hood bands, tools, racks' fittings
STONE = make_ramp("7D7A76")           # hearth stone, close to vanilla stone bricks
SOOT = make_ramp("403B3C")            # blackened firebox stone
BARK = make_ramp("6E523C")            # stump bark
ENDWOOD = make_ramp("9A7A55", 0.16)   # cut end of the stump (rings)
COAL = fixed("151317", "221E23", "312B30", "443B3E", "5A4E4E")
HOT = fixed("5A1608", "9C2A0A", "D2520F", "F2902A", "FFD25E")   # glowing coals (full-bright part)
WATER = fixed("13283F", "1E3D5E", "2C5A84", "4378A6", "6C9EC6")
GRIT = make_ramp("7A7468", 0.18)      # grindstone wheel (sandstone grey)

# House shield fields, from art/house/<house>/banner.png and mark16.png
HOUSE = {
    "gilded":      {"field": fixed("6F5026", "96783A", "B8852A", "DCB145", "F2D877"), "charge": fixed("3A3024", "675641", "BFAC86", "DDCFAA", "F5EDD8")},
    "graveborn":   {"field": fixed("120C18", "1E1528", "362645", "54406A", "6E5888"), "charge": fixed("6E6A5E", "A49C86", "C6BEA6", "E2DCC8", "F4F0E2")},
    "ironclad":    {"field": fixed("121316", "1B1D21", "30343A", "4C5158", "6A7078"), "charge": fixed("4E3A1C", "6E5228", "8E6C36", "B3905A", "D2B27C")},
    "tidecallers": {"field": fixed("0A1426", "11264A", "1A3F6A", "2A5E8A", "4C7EA6"), "charge": fixed("3E5E72", "5E88A6", "9DB8C4", "C6DCE2", "E6F2F4")},
    "wildmoot":    {"field": fixed("24170E", "43291A", "673F25", "885836", "A87048"), "charge": fixed("6E6A5E", "A49C86", "C6BEA6", "E2DCC8", "F4F0E2")},
}

# ---- faces ---------------------------------------------------------------------------------------------------
FACES = ("north", "south", "east", "west", "up", "down")
LIT = {"up": 1, "north": 0, "east": 0, "south": -1, "west": -1, "down": -2}

def face_dims(face, frm, to):
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    return {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}[face]

def texel_point(face, frm, to, c, r):
    """Model point at the centre of texel (c, r) of a face painted as seen from outside (row 0 = top)."""
    x1, y1, z1 = frm; x2, y2, z2 = to
    cc, rr = c + 0.5, r + 0.5
    return {"north": (x2 - cc, y2 - rr, z1), "south": (x1 + cc, y2 - rr, z2),
            "west": (x1, y2 - rr, z1 + cc), "east": (x2, y2 - rr, z2 - cc),
            "up": (x1 + cc, y2, z1 + rr), "down": (x1 + cc, y1, z2 - rr)}[face]

# ---- materials -----------------------------------------------------------------------------------------------
def h3(*a):
    """Small deterministic hash -> [0,1) for hand-placed-looking marks (sparse, never a grid)."""
    n = 0
    for v in a: n = (n * 1000003 + int(math.floor(v * 4)) * 7919 + 17) & 0xFFFFFFFF
    n ^= n >> 13; n = (n * 0x5BD1E995) & 0xFFFFFFFF; n ^= n >> 15
    return (n & 0xFFFF) / 65536.0

class Mat:
    """A material: ramp, base tone and a pattern(face, P, c, r, w, h) -> tone delta (or a colour tuple)."""
    def __init__(s, ramp, base=2, pattern=None, edges=True, lit=True, top=1):
        s.ramp, s.base, s.pattern, s.edges, s.lit, s.top = ramp, base, pattern, edges, lit, top
    def __call__(s, face, P, c, r, w, h):
        t = s.base + ((s.top if face == "up" else LIT[face]) if s.lit else 0)
        if s.pattern:
            d = s.pattern(face, P, c, r, w, h)
            if isinstance(d, tuple): return d
            t += d
        if s.edges and face not in ("up", "down") and h >= 3:
            if r == 0 and face in ("north", "east"): t += 1
            elif r == h - 1: t -= 1
        if s.edges and face == "up" and min(w, h) >= 3 and (r == 0 or c == w - 1):
            t += 0   # top faces stay flat: the game already lights them
        return s.ramp[max(0, min(4, t))]

def pat_brick(face, P, c, r, w, h, bw=8, bh=4):
    """World-aligned stone brick courses: bh-texel courses, bw-texel bricks offset half a brick each course,
    1-texel mortar one tone down, a light texel at each brick's top-left, a rare pit."""
    x, y, z = P
    if face in ("up", "down"):
        u, v = x, z
        row = math.floor(v / bh); off = (bw // 2) * (row % 2)
        if (v % bh) < 1 or ((u + off) % bw) < 1: return -1
        return -1 if h3(u, v, 3) < 0.04 else 0
    along = x if face in ("north", "south") else z
    row = math.floor(y / bh); off = (bw // 2) * (row % 2) + (3 if face in ("east", "west") else 0)
    ly = y % bh; lx = (along + off) % bw
    if ly < 1: return -1                      # bed joint (bottom of a course)
    if lx < 1: return -1                      # head joint
    if ly >= bh - 1 and lx < 2: return 1      # top-left of a brick catches the light
    if h3(along, y, 7) < 0.05: return -1      # a pit
    return 0

def pat_grain(axis):
    """Wood: one grain line along the long axis every few texels, a tone down; flecks rare."""
    def p(face, P, c, r, w, h):
        x, y, z = P
        if face in ("up", "down") and axis == "y": return -1 if h3(x, z, 1) < 0.2 else 0
        a = {"x": (y, z), "y": (x, z), "z": (x, y)}[axis]
        k = a[0] if face not in ("up", "down") or axis == "y" else a[1]
        if face in ("north", "south") and axis == "y": k = x
        if face in ("east", "west") and axis == "y": k = z
        if face in ("up", "down") and axis == "x": k = z
        if face in ("up", "down") and axis == "z": k = x
        if face in ("north", "south") and axis == "x": k = y
        if face in ("east", "west") and axis == "z": k = y
        line = (math.floor(k) * 5 + 2) % 7 == 0
        if line: return -1
        return 1 if h3(x, y, z) < 0.03 else 0
    return p

def pat_plank(axis, width=4):
    """Planks: grain along `axis`, a seam (tone -1) every `width` texels across it."""
    g = pat_grain(axis)
    def p(face, P, c, r, w, h):
        x, y, z = P
        across = {"x": y if face not in ("up", "down") else z, "z": y if face not in ("up", "down") else x, "y": x if face in ("north", "south", "up", "down") else z}[axis]
        if (across % width) < 1: return -1
        return g(face, P, c, r, w, h)
    return p

def pat_iron(face, P, c, r, w, h):
    """Dull iron: a hammer mark now and then; the column nearest the light (east on the front) a tone up."""
    x, y, z = P
    if face == "north" and c == 0 and w >= 3: return 1
    if face == "east" and c == 0 and w >= 3: return 1
    return -1 if h3(x, y, z, 5) < 0.06 else 0

def pat_steel(face, P, c, r, w, h):
    x, y, z = P
    if face in ("north", "east") and c == 0 and w >= 2: return 1
    return -1 if h3(x, y, z, 9) < 0.05 else 0

def pat_gilt(face, P, c, r, w, h):
    if r == 0 and c == 0: return 1
    if r == h - 1 and c == w - 1 and w * h > 2: return -1
    return 0

def pat_leather(face, P, c, r, w, h):
    x, y, z = P
    return -1 if h3(x, y, z, 11) < 0.08 else 0

def pat_coal(face, P, c, r, w, h):
    """Coal bed: lumps of coal with hot glowing gaps; coals' faces carry the HOT ramp where a gap opens."""
    x, y, z = P
    k = h3(math.floor(x / 2), math.floor(z / 2), math.floor(y / 2), 13)
    if face in ("up",):
        if k < 0.38: return HOT[4] if h3(x, z, 2) < 0.35 else HOT[3]
        if k < 0.55: return HOT[2]
        return COAL[3] if (x + z) % 3 == 0 else COAL[2]
    if k < 0.45: return HOT[2] if r < h - 1 else HOT[1]
    return COAL[2] if r == 0 else COAL[1]

def pat_hot(face, P, c, r, w, h):
    """The firebox glow on the inner walls: hot near the bottom, darker up."""
    if r >= h - 2: return HOT[2]
    if r >= h - 4: return HOT[1]
    if r >= h - 6: return HOT[0]
    return SOOT[0]

def pat_water(face, P, c, r, w, h):
    x, y, z = P
    if face == "up":
        if h3(math.floor(x / 3), z, 21) < 0.12: return 1      # a short ripple glint
        return 0
    return -1

def pat_rings(face, P, c, r, w, h):
    x, y, z = P
    if face in ("up", "down"):
        d = max(abs(x - 8), abs(z - 8))
        if d > 6.5: return -2                      # bark rim
        return -1 if math.floor(d) % 3 == 1 else 0
    return 0

def pat_bark(face, P, c, r, w, h):
    x, y, z = P
    a = x if face in ("north", "south") else z
    if (math.floor(a) * 3 + 1) % 4 == 0: return -1
    return 1 if h3(a, y, 4) < 0.06 else 0

def pat_grit(face, P, c, r, w, h):
    x, y, z = P
    if face in ("east", "west"):
        # wheel side: a rim ring and the iron axle hub come from separate elements; here a stone speckle
        return -1 if h3(y, z, 31) < 0.10 else 0
    return -1 if h3(x, y, z, 33) < 0.12 else 0

def flat(ramp, tone):
    return Mat(ramp, tone, edges=False, lit=False)

MAT = {
    "stone": Mat(STONE, 2, pat_brick, top=0),
    "stone_top": Mat(STONE, 2, lambda f, P, c, r, w, h: (-1 if h3(*P, 41) < 0.05 else 0), top=0),
    "soot": Mat(SOOT, 2, pat_brick, top=0),
    "anvil": Mat(STEEL, 2, pat_iron),
    "hotwall": Mat(SOOT, 2, pat_hot, edges=False),
    "coal": Mat(COAL, 2, pat_coal, edges=False, lit=False),
    "iron": Mat(IRON, 2, pat_iron),
    "steel": Mat(STEEL, 2, pat_steel),
    "gilt": Mat(GILT, 3, pat_gilt, edges=False),
    "wood_y": Mat(WOOD, 2, pat_grain("y")), "wood_x": Mat(WOOD, 2, pat_grain("x")), "wood_z": Mat(WOOD, 2, pat_grain("z")),
    "swood_y": Mat(SWOOD, 2, pat_grain("y")), "swood_x": Mat(SWOOD, 2, pat_grain("x")), "swood_z": Mat(SWOOD, 2, pat_grain("z")),
    "plank_x": Mat(WOOD, 2, pat_plank("x")), "plank_z": Mat(WOOD, 2, pat_plank("z")),
    "leather": Mat(LEATHER, 2, pat_leather),
    "linen": Mat(LINEN, 2),
    "water": Mat(WATER, 2, pat_water, edges=False),
    "stump": Mat(BARK, 2, pat_bark),
    "endgrain": Mat(ENDWOOD, 2, pat_rings, edges=False),
    "grit": Mat(GRIT, 2, pat_grit),
}

# ---- elements and props ----------------------------------------------------------------------------------------
class E:
    """One cuboid: from/to in the prop frame (px), a material per face (str or {face: str/callable}),
    `skip` faces left out (hidden), optional single-axis rotation (axis, angle, origin) and shade.
    `whole` (set by the big-prop splitter) is the box of the element it was cut from: paint edges and columns follow it."""
    def __init__(s, frm, to, mat, skip="", rot=None, shade=True, name=None, faces=None, whole=None):
        s.frm, s.to = list(frm), list(to)
        s.mat, s.skip, s.rot, s.shade, s.name = mat, set(skip.split()) if skip else set(), rot, shade, name
        s.faces = faces or {}
        s.whole = whole
        for i in range(3): assert s.to[i] >= s.frm[i], (frm, to)

    def mat_for(s, face):
        m = s.faces.get(face, s.mat)
        return MAT[m] if isinstance(m, str) else m

class Prop:
    """A prop: {part: [E]}. `shift` {part: (dx,dy,dz)} stores a part moved by -shift (inside -16..32). A split prop
    (Prop.split) carries `offsets`: its shifts go to the def's part `offset` (the game moves the part entity) instead of
    the model's display translation, which Minecraft clamps to +-80 px."""
    def __init__(s, pid, parts, glow=(), shift=None, note=""):
        s.id, s.parts, s.glow, s.shift, s.note = pid, parts, set(glow), shift or {}, note
        s.offsets = False
        s.regions = None

    @classmethod
    def split(cls, pid, els, size, glow=(), note=""):
        """Big props: `els` (and `glow`, drawn full-bright) modelled in the real prop frame for a footprint `size`
        (sx, sy, sz cells) are cut into 3-cell regions; each non-empty region is a part `<id>_<n>` stored inside -16..32.
        n = region index (glow regions after all plain ones), so part names and CMDs stay put while the model changes."""
        parts, shift = split_elements(pid, els, size)
        total = region_count(size)
        gnames = []
        if glow:
            gp, gs = split_elements(pid, glow, size, first=total)
            parts.update(gp); shift.update(gs); gnames = list(gp)
        p = cls(pid, parts, glow=gnames, shift=shift, note=note)
        p.offsets, p.regions = True, total
        return p

# ---- shape helpers (each returns a list of E) -------------------------------------------------------------------
# All coordinates must come out whole px (the density check); shapes are stepped like everything else at 16 px a block.
PLANE = {"x": (2, 1), "y": (0, 2), "z": (0, 1)}   # axis -> (u, v): the cross-section plane's axes (indices)
FNEG = {0: "west", 1: "down", 2: "north"}; FPOS = {0: "east", 1: "up", 2: "south"}

def _whole(v, what="coordinate"):
    assert abs(v - round(v)) < 1e-9, "%s %g is not a whole px (shape helpers need whole-px results)" % (what, v)
    return int(round(v))

def _rect_rows(cells):
    rows = {}
    for u, v in cells: rows.setdefault(v, []).append(u)
    runs = {}
    for v, us in rows.items():
        us.sort(); r = []; s0 = p = us[0]
        for u in us[1:]:
            if u != p + 1: r.append((s0, p + 1)); s0 = u
            p = u
        r.append((s0, p + 1)); runs[v] = r
    out, grow, last = [], {}, None
    for v in sorted(runs):
        cur = set(runs[v])
        for k in list(grow):
            if k not in cur or last != v - 1: out.append((k[0], grow.pop(k), k[1], last + 1))
        for k in cur:
            if k not in grow: grow[k] = v
        last = v
    for k, v0 in grow.items(): out.append((k[0], v0, k[1], last + 1))
    return out

def rects(cells):
    """Few rectangles covering a set of (u, v) px cells exactly: [(u0, v0, u1, v1)], half-open. Tries runs along u
    and along v and keeps the fewer."""
    cells = set(cells)
    if not cells: return []
    a = _rect_rows(cells)
    b = [(u0, v0, u1, v1) for v0, u0, v1, u1 in _rect_rows({(v, u) for u, v in cells})]
    return sorted(a if len(a) <= len(b) else b)

def slab(cells, axis, a0, a1, mat, name=None, skip="", below=None, above=None, **kw):
    """Extrude a set of (u, v) px cells (PLANE[axis] order) from a0 to a1 along `axis`. Faces fully against another
    rectangle of the same set are skipped; so are end faces fully covered by the `below` / `above` cell sets."""
    ai = "xyz".index(axis); ui, vi = PLANE[axis]; cells = set(cells)
    a0, a1 = _whole(a0), _whole(a1); out = []
    for u0, v0, u1, v1 in rects(cells):
        sk = set(skip.split())
        if all((u0 - 1, v) in cells for v in range(v0, v1)): sk.add(FNEG[ui])
        if all((u1, v) in cells for v in range(v0, v1)): sk.add(FPOS[ui])
        if all((u, v0 - 1) in cells for u in range(u0, u1)): sk.add(FNEG[vi])
        if all((u, v1) in cells for u in range(u0, u1)): sk.add(FPOS[vi])
        inside = [(u, v) for u in range(u0, u1) for v in range(v0, v1)]
        if below is not None and all(c in below for c in inside): sk.add(FNEG[ai])
        if above is not None and all(c in above for c in inside): sk.add(FPOS[ai])
        frm, to = [0, 0, 0], [0, 0, 0]
        frm[ui], frm[vi], frm[ai] = u0, v0, a0; to[ui], to[vi], to[ai] = u1, v1, a1
        out.append(E(frm, to, mat, skip=" ".join(sorted(sk)), name=name, **kw))
    return out

def disc_cells(cu, cv, r, r_in=0.0, step=1):
    """px cells of a stepped disc (or ring when r_in > 0) centred on (cu, cv); `step` > 1 makes coarser steps
    (blocks of step px laid from the centre: needs a whole-px centre)."""
    out = set(); n = int(math.ceil(r / step)) + 1
    ou, ov = (cu, cv) if step > 1 else (math.floor(cu), math.floor(cv))
    for i in range(-n - 1, n + 1):
        for j in range(-n - 1, n + 1):
            u, v = ou + i * step, ov + j * step
            d = math.hypot(u + step / 2 - cu, v + step / 2 - cv)
            if d <= r + 0.2 and d > r_in + 0.2 - (0 if r_in > 0 else 1e9):
                for a in range(step):
                    for b in range(step): out.add((_whole(u + a), _whole(v + b)))
    return out

def cylinder(base, radius, length, axis, mat, name=None, step=1, hollow=0, skip="", **kw):
    """Stepped round post, barrel or drum along `axis`: `base` = centre of the start cap (min end on the axis),
    `radius` px, `length` px along the axis. A whole-px centre gives an even diameter, a .5 centre an odd one.
    `hollow` > 0 leaves a hole of that radius (a ring or a tube); `step` coarsens the steps (fewer elements)."""
    ai = "xyz".index(axis); ui, vi = PLANE[axis]
    cells = disc_cells(base[ui], base[vi], radius, hollow, step)
    return slab(cells, axis, base[ai], base[ai] + length, mat, name or "cylinder", skip=skip, **kw)

def _octagon_wheel(cu, cv, ui, vi, ai, axis, a0, a1, radius, thickness, mat, spokes, rim, hub, hub_mat, spoke_w, name):
    def box(u0, u1, v0, v1, b0, b1, m, nm, rot=None):
        frm, to = [0, 0, 0], [0, 0, 0]
        frm[ui], to[ui], frm[vi], to[vi], frm[ai], to[ai] = u0, u1, v0, v1, b0, b1
        return E([_whole(v) for v in frm], [_whole(v) for v in to], m, name=nm, rot=rot)
    org = [0, 0, 0]; org[ui], org[vi], org[ai] = cu, cv, (a0 + a1) / 2; org = tuple(org)
    L = 2 * int(round(radius * math.tan(math.radians(22.5))))   # octagon side, even so it sits on a whole-px centre
    h = L / 2; R, r_in = radius, radius - rim
    out = [box(cu - h, cu + h, cv + r_in, cv + R, a0, a1, mat, name + " rim"), box(cu - h, cu + h, cv - R, cv - r_in, a0, a1, mat, name + " rim"),
           box(cu + r_in, cu + R, cv - h, cv + h, a0, a1, mat, name + " rim"), box(cu - R, cu - r_in, cv - h, cv + h, a0, a1, mat, name + " rim")]
    d0, d1 = (a0 + 1, a1 - 1) if thickness >= 3 else (a0 - 1, a1 + 1)   # diagonals inset (or proud on a thin wheel): no shared face plane
    for ang in (45, -45):
        out.append(box(cu - h, cu + h, cv + r_in, cv + R, d0, d1, mat, name + " rim", rot=(axis, ang, org)))
        out.append(box(cu - h, cu + h, cv - R, cv - r_in, d0, d1, mat, name + " rim", rot=(axis, ang, org)))
    w = spoke_w / 2
    out.append(box(cu - r_in, cu - hub, cv - w, cv + w, a0, a1, mat, name + " spoke"))
    out.append(box(cu + hub, cu + r_in, cv - w, cv + w, a0, a1, mat, name + " spoke"))
    out.append(box(cu - w, cu + w, cv - r_in, cv - hub, a0, a1, mat, name + " spoke"))
    out.append(box(cu - w, cu + w, cv + hub, cv + r_in, a0, a1, mat, name + " spoke"))
    if spokes == 8:   # the diagonals cross the cross bars only inside the hub (hub 3+); inset where the wheel is thick enough
        s0, s1 = (a0 + 1, a1 - 1) if thickness >= 3 else (a0, a1)
        for ang in (45, -45): out.append(box(cu - r_in, cu + r_in, cv - w, cv + w, s0, s1, mat, name + " spoke", rot=(axis, ang, org)))
    out.append(box(cu - hub, cu + hub, cv - hub, cv + hub, a0 - 1, a1 + 1, hub_mat, name + " hub"))
    return out

def wheel(center, radius, thickness, axis, mat, spokes=4, rim=None, hub=None, hub_mat=None, spoke_w=2, name="wheel", step=1, octagon=None):
    """Spoked wheel turning about `axis` (the axle direction): a rim, `spokes` (0 = a solid plank disc, 4 = a cross,
    8 = a cross plus two 45-degree bars) and a hub that stands 1 px proud on both faces.
    `center` is the wheel's centre; on the axle axis it must be whole px for an even thickness, .5 for an odd one.
    Rim: `octagon` (default from radius 5 with spokes) = 4 straight felloes + 4 at 45 degrees, about 11-13 elements a wheel;
    octagon=False = a stepped ring of boxes (no rotations, more elements; `step` 2-3 makes it coarser and cheaper)."""
    assert spokes in (0, 4, 8), "spokes must be 0, 4 or 8 (models rotate only in 22.5-degree steps up to 45)"
    ai = "xyz".index(axis); ui, vi = PLANE[axis]; cu, cv = center[ui], center[vi]
    rim = rim or max(2, radius // 4); hub = hub or max(3 if spokes == 8 else 2, radius // 4); hub_mat = hub_mat or mat
    a0 = center[ai] - thickness / 2; a1 = a0 + thickness
    if octagon is None: octagon = radius >= 5 and spokes > 0
    if octagon: return _octagon_wheel(cu, cv, ui, vi, ai, axis, a0, a1, radius, thickness, mat, spokes, rim, hub, hub_mat, spoke_w, name)
    hubc = disc_cells(cu, cv, hub)
    if spokes == 0: cells = disc_cells(cu, cv, radius, 0, step)
    else:
        cells = disc_cells(cu, cv, radius, radius - rim, step)
        h = spoke_w / 2; reach = radius - rim / 2
        for i in range(int(math.floor(cu - reach)), int(math.ceil(cu + reach))):
            for j in range(_whole(cv - h, "spoke edge"), _whole(cv + h, "spoke edge")): cells.add((i, j))
        for j in range(int(math.floor(cv - reach)), int(math.ceil(cv + reach))):
            for i in range(_whole(cu - h, "spoke edge"), _whole(cu + h, "spoke edge")): cells.add((i, j))
    cells -= hubc
    out = slab(cells, axis, a0, a1, mat, name=name)
    if spokes == 8:   # diagonal bars, 1 px thinner on each face where the wheel is thick enough, ends buried in the rim
        L = _whole(radius - rim) if radius - rim == int(radius - rim) else int(radius - rim)
        b0, b1 = (a0 + 1, a1 - 1) if thickness >= 4 else (a0, a1)
        for ang in (45, -45):
            frm, to = [0, 0, 0], [0, 0, 0]
            frm[ui], to[ui] = cu - L, cu + L; frm[vi], to[vi] = cv - spoke_w / 2, cv + spoke_w / 2
            frm[ai], to[ai] = b0, b1
            frm = [_whole(v) for v in frm]; to = [_whole(v) for v in to]
            org = [0, 0, 0]; org[ui], org[vi], org[ai] = cu, cv, (b0 + b1) / 2
            out.append(E(frm, to, mat, rot=(axis, ang, tuple(org)), name=name + " spoke"))
    out += slab(hubc, axis, a0 - 1, a1 + 1, hub_mat, name=name + " hub")
    return out

def blob(frm, to, mat, layers=3, step=None, seed=0, name="rock", skip="down"):
    """Stepped rounded rock filling the box frm..to: `layers` horizontal slabs, each a stepped ellipse (widest low,
    narrower on top), steps of `step` px (default about a quarter of the smaller radius). `seed` nudges the upper layers
    a step so a set of boulders does not repeat; `skip` is applied to the bottom layer (it sits on the ground)."""
    x0, y0, z0 = (_whole(v) for v in frm); x1, y1, z1 = (_whole(v) for v in to)
    rx, rz = (x1 - x0) / 2, (z1 - z0) / 2; cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
    step = step or max(1, int(round(min(rx, rz) / 4)))
    bounds = [y0 + int(round((y1 - y0) * i / layers)) for i in range(layers + 1)]
    sc = []
    for i in range(layers):
        f = ((bounds[i] + bounds[i + 1]) / 2 - y0) / (y1 - y0)
        sc.append(math.sqrt(max(0.05, 1 - ((f - 0.38) / 0.64) ** 2)) if layers > 1 else 1.0)
    top = max(sc); sc = [s / top for s in sc]
    sets = []
    for i, s in enumerate(sc):
        jx = jz = 0
        if seed and i > 0:
            jx = step * (int(h3(seed, i, 1) * 3) - 1); jz = step * (int(h3(seed, i, 2) * 3) - 1)
        ex, ez = max(step / 2, rx * s), max(step / 2, rz * s); cells = set()
        for u in range(x0, x1, step):
            for v in range(z0, z1, step):
                du = (u + step / 2 - cx - jx) / ex; dv = (v + step / 2 - cz - jz) / ez
                if du * du + dv * dv <= 1.0 + 0.15:
                    for a in range(step):
                        for b in range(step):
                            if u + a < x1 and v + b < z1: cells.add((u + a, v + b))
        sets.append(cells)
    out = []
    for i, cells in enumerate(sets):
        out += slab(cells, "y", bounds[i], bounds[i + 1], mat, name=name, skip=skip if i == 0 else "",
                    below=sets[i - 1] if i else None, above=sets[i + 1] if i + 1 < len(sets) else None)
    return out

def _catmull(P, a, s):
    """Cross coordinates at `s` along axis index `a` of a Catmull-Rom curve through points P (sorted by P[k][a])."""
    k = 0
    while k < len(P) - 2 and s > P[k + 1][a]: k += 1
    p1, p2 = P[k], P[k + 1]; p0 = P[k - 1] if k else p1; p3 = P[k + 2] if k + 2 < len(P) else p2
    span = p2[a] - p1[a]; t = 0.0 if span == 0 else max(0.0, min(1.0, (s - p1[a]) / span))
    return [0.5 * (2 * p1[i] + (-p0[i] + p2[i]) * t + (2 * p0[i] - 5 * p1[i] + 4 * p2[i] - p3[i]) * t * t
                   + (-p0[i] + 3 * p1[i] - 3 * p2[i] + p3[i]) * t ** 3) for i in range(3)]

def curve(points, size, mat, name="curve", along=None, step=1, skip=""):
    """A stepped curve (canvas sag, a bow, an arch rib, a rope) through `points`, a smooth Catmull-Rom line.
    Each point is the min corner of the cross-section; the curve runs along `along` ("x"/"y"/"z", default the axis
    with the largest travel), and `size` is the cross-section (int, or (a, b) for the two other axes in xyz order).
    Offsets snap to `step` px; neighbouring steps always overlap 1 px, so a thin sheet never shows a gap."""
    P = [tuple(float(c) for c in p) for p in points]
    a = "xyz".index(along) if along else max(range(3), key=lambda i: abs(P[-1][i] - P[0][i]))
    P.sort(key=lambda p: p[a]); cross = [i for i in range(3) if i != a]
    sz = (size, size) if isinstance(size, (int, float)) else tuple(size)
    s0, s1 = _whole(P[0][a]), _whole(P[-1][a])
    runs = []
    for s in range(s0, s1):
        c = _catmull(P, a, s + 0.5); o = tuple(int(round(c[i] / step)) * step for i in cross)
        if runs and runs[-1][2] == o: runs[-1][1] = s + 1
        else: runs.append([s, s + 1, o])
    lo = [[r[2][k] for k in range(2)] for r in runs]; hi = [[r[2][k] + sz[k] for k in range(2)] for r in runs]
    for j in range(len(runs) - 1):
        for k in range(2):
            A, B = runs[j][2][k], runs[j + 1][2][k]
            d = B - A
            if abs(d) >= sz[k]:   # the two steps would only touch at an edge: grow both toward each other
                e = abs(d) - sz[k] + 1; ea, eb = (e + 1) // 2, e // 2
                if d > 0: hi[j][k] = max(hi[j][k], A + sz[k] + ea); lo[j + 1][k] = min(lo[j + 1][k], B - eb)
                else: lo[j][k] = min(lo[j][k], A - ea); hi[j + 1][k] = max(hi[j + 1][k], B + sz[k] + eb)
    out = []
    for j, (sa, sb, _) in enumerate(runs):
        frm, to = [0, 0, 0], [0, 0, 0]; frm[a], to[a] = sa, sb
        for k, i in enumerate(cross): frm[i], to[i] = lo[j][k], hi[j][k]
        out.append(E(frm, to, mat, name=name, skip=skip))
    return out

# ---- big props: 3-cell regions -----------------------------------------------------------------------------------
REGION = 48   # one part covers -16..32: 3 cells

def part_grid(size):
    """(start, n) per axis of the region grid over a footprint: n = ceil(cells / 3) regions, x and z centred on the
    footprint (slack split evenly), y from the floor. Region i spans start + 48 i .. + 48 and is stored shifted by
    start + 48 i + 16."""
    sx, sy, sz = size; px, pz = (sx - 1) // 2, (sz - 1) // 2
    lo, ext = (-16 * px, 0, -16 * pz), (16 * sx, 16 * sy, 16 * sz)
    n = [max(1, -(-e // REGION)) for e in ext]
    start = [lo[i] - (n[i] * REGION - ext[i]) // 2 if i != 1 else 0 for i in range(3)]
    return start, n

def region_count(size):
    _, n = part_grid(size); return n[0] * n[1] * n[2]

def split_elements(pid, els, size, first=0):
    """Assign elements (prop frame) to 3-cell regions, cutting any that cross a region boundary; returns
    ({'<id>_<n>': [E]}, {'<id>_<n>': shift}). A rotated element may be cut only across its rotation axis."""
    start, n = part_grid(size)
    parts, shift = {}, {}
    for e in els:
        rng = []
        for i in range(3):
            lo = (e.frm[i] - start[i]) // REGION
            hi = (e.to[i] - start[i] - (1e-9 if e.to[i] > e.frm[i] else 0)) // REGION
            lo, hi = int(lo), int(hi)
            if lo < 0 or hi >= n[i]:
                raise SystemExit("%s: element %r %s..%s leaves the part grid on %s (grid %g..%g for size %s); keep it inside "
                                 "the footprint there" % (pid, e.name, e.frm, e.to, "xyz"[i], start[i], start[i] + n[i] * REGION, list(size)))
            if hi > lo and e.rot and e.rot[0] != "xyz"[i]:
                raise SystemExit("%s: rotated element %r %s..%s crosses a part boundary on %s (cut only across its rotation "
                                 "axis %s); move it inside one region (boundaries at %s)" % (pid, e.name, e.frm, e.to, "xyz"[i], e.rot[0],
                                 [start[i] + REGION * k for k in range(1, n[i])]))
            rng.append(range(lo, hi + 1))
        for ix in rng[0]:
            for iy in rng[1]:
                for iz in rng[2]:
                    idx = (ix, iy, iz); frm, to, sk = [], [], set(e.skip)
                    for i in range(3):
                        b0 = start[i] + REGION * idx[i]; b1 = b0 + REGION
                        f, t = max(e.frm[i], b0), min(e.to[i], b1)
                        if f > e.frm[i]: sk.add(FNEG[i])
                        if t < e.to[i]: sk.add(FPOS[i])
                        frm.append(f); to.append(t)
                    piece = e if frm == e.frm and to == e.to else E(frm, to, e.mat, skip=" ".join(sorted(sk)), rot=e.rot, shade=e.shade,
                                                                  name=e.name, faces=e.faces, whole=e.whole or (list(e.frm), list(e.to)))
                    k = first + (iy * n[2] + iz) * n[0] + ix
                    pname = "%s_%d" % (pid, k)
                    parts.setdefault(pname, []).append(piece)
                    shift[pname] = tuple(start[i] + REGION * idx[i] + 16 for i in range(3))
    order = sorted(parts, key=lambda p: int(p.rsplit("_", 1)[1]))
    return {p: parts[p] for p in order}, {p: shift[p] for p in order}

def visible_faces(e):
    out = []
    for f in FACES:
        if f in e.skip: continue
        w, h = face_dims(f, e.frm, e.to)
        if w > 0 and h > 0: out.append(f)
    return out

def pack(rects, size):
    """Shelf pack [(key, w, h)] tallest first into size x size; returns {key: (x, y)} or None."""
    order = sorted(rects, key=lambda t: (-t[2], -t[1]))
    x = y = shelf = 0; pos = {}
    for k, w, h in order:
        if x + w > size: x, y, shelf = 0, y + shelf, 0
        if y + h > size or w > size: return None
        pos[k] = (x, y); x += w; shelf = max(shelf, h)
    return pos

def build_sheet(prop):
    rects = []
    for pname, els in prop.parts.items():
        for i, e in enumerate(els):
            for f in visible_faces(e):
                w, h = face_dims(f, e.frm, e.to)
                assert w == int(w) and h == int(h), (prop.id, pname, i, f, w, h)
                rects.append(((pname, i, f), int(w), int(h)))
    for size in SHEETS:
        pos = pack(rects, size)
        if pos is not None: break
    else: raise SystemExit("%s: faces don't fit a %d sheet" % (prop.id, SHEETS[-1]))
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0)); px = img.load()
    for (pname, i, f), w, h in rects:
        e = prop.parts[pname][i]; x0, y0 = pos[(pname, i, f)]; m = e.mat_for(f)
        W, H = (face_dims(f, *e.whole) if e.whole else (w, h))
        for r in range(h):
            for c in range(w):
                P = texel_point(f, e.frm, e.to, c, r)
                C, R = whole_texel(f, e.whole, P) if e.whole else (c, r)
                px[x0 + c, y0 + r] = m(f, P, C, R, W, H) if not callable(getattr(m, "paint", None)) else m.paint(f, P, C, R, W, H)
    return img, size, pos

SHEETS = (32, 64, 128, 256, 512, 1024)   # 1024: the most a prop sheet may take in the block atlas (prop_card caps it)

def whole_texel(face, whole, P):
    """(column, row) of model point P on `face` of the uncut box `whole` (inverse of texel_point)."""
    (x1, y1, z1), (x2, y2, z2) = whole; x, y, z = P
    c, r = {"north": (x2 - x, y2 - y), "south": (x - x1, y2 - y), "west": (z - z1, y2 - y), "east": (z2 - z, y2 - y),
            "up": (x - x1, z - z1), "down": (x - x1, z2 - z)}[face]
    return int(math.floor(c)), int(math.floor(r))

# ---- exports ---------------------------------------------------------------------------------------------------
def _rotXYZ(rx, ry, rz):
    a, b, c = (math.radians(v) for v in (rx, ry, rz))
    Rx = [[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]]
    Ry = [[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]]
    Rz = [[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]]
    mul = lambda A, B: [[sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mul(Rx, mul(Ry, Rz))

def gui_display(els, shift):
    """display.gui that fits the part in a slot: 3/4 view, scaled to its largest extent, centred."""
    if not els: return {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]}
    lo = [min(e.frm[i] for e in els) - shift[i] for i in range(3)]
    hi = [max(e.to[i] for e in els) - shift[i] for i in range(3)]
    ext = max(hi[i] - lo[i] for i in range(3))
    sc = round(min(4.0, 0.62 * 16 / ext * 1.6), 4) if ext else 0.625
    rot = [30, 225, 0]
    R = _rotXYZ(*rot)
    c = [((lo[i] + hi[i]) / 2) / 16 - 0.5 for i in range(3)]
    rc = [sum(R[i][k] * c[k] * sc for k in range(3)) for i in range(3)]
    t = [max(-80, min(80, round(-16 * v, 3))) for v in rc]
    return {"rotation": rot, "translation": t, "scale": [sc, sc, sc]}

def item_model(prop, pname, size, pos):
    shift = prop.shift.get(pname, (0, 0, 0))
    k = 16.0 / size; els = []
    for i, e in enumerate(prop.parts[pname]):
        frm = [e.frm[j] - shift[j] for j in range(3)]; to = [e.to[j] - shift[j] for j in range(3)]
        for v in frm + to: assert -16 <= v <= 32, (prop.id, pname, i, frm, to)
        faces = {}
        for f in visible_faces(e):
            w, h = face_dims(f, e.frm, e.to); x0, y0 = pos[(pname, i, f)]
            faces[f] = {"uv": [round(x0 * k, 6), round(y0 * k, 6), round((x0 + w) * k, 6), round((y0 + h) * k, 6)], "texture": "#0"}
        el = {"from": frm, "to": to, "faces": faces}
        if e.name: el["name"] = e.name
        if e.rot:
            axis, ang, org = e.rot
            el["rotation"] = {"angle": ang, "axis": axis, "origin": [org[j] - shift[j] for j in range(3)]}
        if not e.shade or pname in prop.glow: el["shade"] = False
        els.append(el)
    return {"credit": "Graves and Glory prop kit, art/props/build_props.py",
            "texture_size": [size, size],
            "textures": {"0": "gravesandglory:prop/%s" % prop.id, "particle": "gravesandglory:prop/%s" % prop.id},
            "elements": els,
            "display": {"fixed": {"translation": [0, 0, 0] if prop.offsets else list(shift)}, "gui": gui_display(prop.parts[pname], shift)}}

def part_offsets(prop):
    """{part: [dx, dy, dz]} px the game moves each part entity by (split props; the def's part `offset`)."""
    return {p: list(prop.shift.get(p, (0, 0, 0))) for p in prop.parts} if prop.offsets else {}

def bbmodel(prop, img, size, pos):
    buf = io.BytesIO(); img.save(buf, "PNG")
    u = lambda *a: str(uuid.uuid5(uuid.NAMESPACE_URL, "gg-prop/" + "/".join(map(str, a))))
    elements, outliner = [], []
    for pname, els in prop.parts.items():
        kids = []
        for i, e in enumerate(els):
            faces = {}
            for f in FACES:
                if (pname, i, f) in pos:
                    w, h = face_dims(f, e.frm, e.to); x0, y0 = pos[(pname, i, f)]
                    faces[f] = {"uv": [x0, y0, x0 + w, y0 + h], "texture": 0}
                else:
                    faces[f] = {"uv": [0, 0, 0, 0], "texture": None}
            rot = [0, 0, 0]; org = [(e.frm[j] + e.to[j]) / 2 for j in range(3)]
            if e.rot:
                axis, ang, org = e.rot; rot["xyz".index(axis)] = ang
            uid = u(prop.id, pname, i)
            elements.append({"name": e.name or "%s_%d" % (pname, i), "box_uv": False, "rescale": False, "locked": False,
                             "shade": e.shade and pname not in prop.glow, "from": e.frm, "to": e.to, "autouv": 0, "color": 0,
                             "origin": list(org), "rotation": rot, "faces": faces, "type": "cube", "uuid": uid})
            kids.append(uid)
        outliner.append({"name": pname, "origin": [0, 0, 0], "uuid": u(prop.id, pname), "export": True,
                         "isOpen": True, "visibility": True, "children": kids})
    return {"meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
            "name": prop.id, "parent": "", "ambientocclusion": False, "front_gui_light": False,
            "visible_box": [1, 1, 0], "resolution": {"width": size, "height": size},
            "elements": elements, "outliner": outliner,
            "textures": [{"path": "", "name": prop.id + ".png", "folder": "prop", "namespace": "gravesandglory", "id": "0",
                          "width": size, "height": size, "uv_width": size, "uv_height": size, "particle": True,
                          "mode": "bitmap", "saved": True, "uuid": u(prop.id, "tex"),
                          "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()}]}

def write(prop, root):
    d = os.path.join(root, prop.id); os.makedirs(d, exist_ok=True)
    img, size, pos = build_sheet(prop)
    img.save(os.path.join(d, prop.id + ".png"))
    for f in os.listdir(d):   # part models of an earlier build that the prop no longer has (a split prop's emptied region)
        if f.endswith(".json") and f[:-5] not in prop.parts:
            try: old = json.load(open(os.path.join(d, f), encoding="utf8"))
            except (OSError, ValueError): continue
            if str(old.get("credit", "")).startswith("Graves and Glory prop kit"): os.remove(os.path.join(d, f))
    for pname in prop.parts:
        with open(os.path.join(d, pname + ".json"), "w", newline="\n") as f:
            json.dump(item_model(prop, pname, size, pos), f, indent=1)
    with open(os.path.join(d, prop.id + ".bbmodel"), "w", newline="\n") as f:
        json.dump(bbmodel(prop, img, size, pos), f)
    n = sum(len(v) for v in prop.parts.values())
    print("%-17s sheet %3dx%-3d parts %-40s elements %d" % (prop.id, size, size, ",".join(prop.parts), n))
