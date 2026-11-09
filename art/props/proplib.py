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
    `skip` faces left out (hidden), optional single-axis rotation (axis, angle, origin) and shade."""
    def __init__(s, frm, to, mat, skip="", rot=None, shade=True, name=None, faces=None):
        s.frm, s.to = list(frm), list(to)
        s.mat, s.skip, s.rot, s.shade, s.name = mat, set(skip.split()) if skip else set(), rot, shade, name
        s.faces = faces or {}
        for i in range(3): assert s.to[i] >= s.frm[i], (frm, to)

    def mat_for(s, face):
        m = s.faces.get(face, s.mat)
        return MAT[m] if isinstance(m, str) else m

class Prop:
    def __init__(s, pid, parts, glow=(), shift=None, note=""):
        s.id, s.parts, s.glow, s.shift, s.note = pid, parts, set(glow), shift or {}, note

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
    for size in (32, 64, 128, 256):
        pos = pack(rects, size)
        if pos: break
    else: raise SystemExit(prop.id + ": faces don't fit a 256 sheet")
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0)); px = img.load()
    for (pname, i, f), w, h in rects:
        e = prop.parts[pname][i]; x0, y0 = pos[(pname, i, f)]; m = e.mat_for(f)
        for r in range(h):
            for c in range(w):
                P = texel_point(f, e.frm, e.to, c, r)
                px[x0 + c, y0 + r] = m(f, P, c, r, w, h) if not callable(getattr(m, "paint", None)) else m.paint(f, P, c, r, w, h)
    return img, size, pos

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
            faces[f] = {"uv": [round(x0 * k, 4), round(y0 * k, 4), round((x0 + w) * k, 4), round((y0 + h) * k, 4)], "texture": "#0"}
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
            "display": {"fixed": {"translation": list(shift)}, "gui": gui_display(prop.parts[pname], shift)}}

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
    for pname in prop.parts:
        with open(os.path.join(d, pname + ".json"), "w", newline="\n") as f:
            json.dump(item_model(prop, pname, size, pos), f, indent=1)
    with open(os.path.join(d, prop.id + ".bbmodel"), "w", newline="\n") as f:
        json.dump(bbmodel(prop, img, size, pos), f)
    n = sum(len(v) for v in prop.parts.values())
    print("%-17s sheet %3dx%-3d parts %-40s elements %d" % (prop.id, size, size, ",".join(prop.parts), n))
