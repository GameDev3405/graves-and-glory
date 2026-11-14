"""Prop contact sheet: renders each prop's exported part models (art/props/<id>/<part>.json + <id>.png), assembled in
the shared prop frame (display.fixed translation added back), front and 3/4 views at 1x (16 px a block, nearest) and
4x, next to the Knight's front view at the same scale, on grass and stone-brick swatches.

It also checks texel density: every face of every element must map exactly 1 texel per model px. Any face that does
not is printed and the tool exits 1.

    python tools/prop_sheet.py [--props forge,anvil] [--art art/props] --out work/props/art/sheet.png
    python tools/prop_sheet.py --check-only [--art DIR]
"""
import argparse, glob, json, math, os, sys
import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ORDER = ["forge", "anvil", "weapon_rack", "shield_rack", "quench_trough", "grindstone_bench"]
FACE_OF = {(0, 0): "west", (0, 1): "east", (1, 0): "down", (1, 1): "up", (2, 0): "north", (2, 1): "south"}
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}

def face_dims(face, frm, to):
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    return {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}[face]

def load_prop(art, pid):
    d = os.path.join(art, pid)
    tex = Image.open(os.path.join(d, pid + ".png")).convert("RGBA")
    parts = []
    for path in sorted(glob.glob(os.path.join(d, "*.json"))):
        m = json.load(open(path))
        if "elements" not in m: continue
        parts.append((os.path.splitext(os.path.basename(path))[0], m))
    dpath = os.path.join(ROOT, "src", "main", "resources", "data", "gravesandglory", "gg_props", pid + ".json")
    if os.path.exists(dpath):   # a big prop's parts are moved by the def's part offset (the game moves the entity)
        off = {q["model"]: q["offset"] for q in json.load(open(dpath, encoding="utf8")).get("parts", []) if "offset" in q}
        for n, m in parts:
            if n in off: m["_offset"] = off[n]
    return tex, parts

def check_density(name, model, tex_size):
    """Return a list of 'model element N face F: ...' for every face not at 1 texel per model px (+-0)."""
    bad = []
    S = model.get("texture_size", [tex_size[0], tex_size[1]])
    if list(S) != list(tex_size): bad.append("%s: texture_size %s but the sheet is %s" % (name, S, list(tex_size)))
    for i, e in enumerate(model["elements"]):
        for f, fd in e.get("faces", {}).items():
            w, h = face_dims(f, e["from"], e["to"])
            u1, v1, u2, v2 = fd["uv"]
            tw, th = abs(u2 - u1) * tex_size[0] / 16.0, abs(v2 - v1) * tex_size[1] / 16.0
            if fd.get("rotation", 0) in (90, 270): tw, th = th, tw
            if abs(tw - w) > 1e-6 or abs(th - h) > 1e-6:
                bad.append("%s element %d face %s: %gx%g px face gets %gx%g texels" % (name, i, f, w, h, tw, th))
    return bad

def rot_matrix(axis, ang):
    a = math.radians(ang); c, s = math.cos(a), math.sin(a)
    if axis == "x": return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == "y": return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])

def elements_of(parts):
    """Flatten parts into elements in the prop frame (shift added back): (from, to, rot, faces, shade, part)."""
    out = []
    for pname, m in parts:
        sh = np.array(m.get("display", {}).get("fixed", {}).get("translation", [0, 0, 0]), float) + m.get("_offset", [0, 0, 0])
        for e in m["elements"]:
            frm = np.array(e["from"], float) + sh; to = np.array(e["to"], float) + sh
            r = e.get("rotation")
            rot = (r["axis"], r["angle"], np.array(r["origin"], float) + sh) if r and r.get("angle") else None
            out.append((frm, to, rot, e.get("faces", {}), e.get("shade", True), pname))
    return out

def render(els, tex, yaw=0.0, pitch=0.0, k=1, bg=None):
    """Orthographic ray cast. yaw 0 looks south at the front (north face); +yaw swings the camera toward the east."""
    T = np.asarray(tex, dtype=np.float32) / 255.0; TS = tex.size
    a, p = math.radians(yaw), math.radians(pitch)
    d = np.array([-math.sin(a) * math.cos(p), -math.sin(p), math.cos(a) * math.cos(p)])
    rgt = np.cross(d, [0, 1, 0]); rgt /= np.linalg.norm(rgt); up = np.cross(rgt, d)
    if not els: return Image.new("RGBA", (16 * k, 16 * k), (0, 0, 0, 0))   # an effect-only prop: nothing to draw
    corners = []; spans = []
    for frm, to, rot, *_ in els:
        mine = []
        for cx in (frm[0], to[0]):
            for cy in (frm[1], to[1]):
                for cz in (frm[2], to[2]):
                    q = np.array([cx, cy, cz])
                    if rot: q = rot_matrix(rot[0], rot[1]) @ (q - rot[2]) + rot[2]
                    mine.append(q)
        corners += mine; M = np.array(mine); spans.append(((M @ rgt).min(), (M @ rgt).max(), (M @ up).min(), (M @ up).max()))
    C = np.array(corners); sx, sy = C @ rgt, C @ up
    x0, x1 = math.floor(sx.min()) - 1, math.ceil(sx.max()) + 1
    y0, y1 = math.floor(sy.min()) - 1, math.ceil(sy.max()) + 1
    W, H = int((x1 - x0) * k), int((y1 - y0) * k)
    jj, ii = np.mgrid[0:H, 0:W]
    U = x0 + (ii + 0.5) / k; V = y1 - (jj + 0.5) / k
    O = (U[..., None] * rgt + V[..., None] * up - d * 1000.0).reshape(-1, 3)
    N = O.shape[0]
    best = np.full(N, np.inf); col = np.zeros((N, 4), np.float32)
    for (frm, to, rot, faces, shade, _), (smin, smax, tmin_, tmax_) in zip(els, spans):
        # only the rays inside the element's screen box can hit it (same picture, far less work on big props)
        i0, i1 = max(0, int(math.floor((smin - x0) * k)) - 1), min(W, int(math.ceil((smax - x0) * k)) + 1)
        j0, j1 = max(0, int(math.floor((y1 - tmax_) * k)) - 1), min(H, int(math.ceil((y1 - tmin_) * k)) + 1)
        if i0 >= i1 or j0 >= j1: continue
        sub = (np.arange(j0, j1)[:, None] * W + np.arange(i0, i1)[None, :]).ravel()
        if rot:
            Ri = rot_matrix(rot[0], rot[1]).T
            Ol = (O[sub] - rot[2]) @ Ri.T + rot[2]; Dl = Ri @ d
        else:
            Ol, Dl = O[sub], d
        with np.errstate(divide="ignore", invalid="ignore"):
            inv = np.where(np.abs(Dl) > 1e-12, 1.0 / Dl, np.inf)
            t1 = (frm - Ol) * inv; t2 = (to - Ol) * inv
        t1 = np.where(np.isnan(t1), -np.inf, t1); t2 = np.where(np.isnan(t2), np.inf, t2)
        tmin = np.minimum(t1, t2); tmax = np.maximum(t1, t2)
        tn = tmin.max(1); tf = tmax.min(1); ax = tmin.argmax(1)
        hit = (tn <= tf + 1e-9) & (tf > 0) & (tn < best[sub])
        if not hit.any(): continue
        loc = np.nonzero(hit)[0]; idx = sub[loc]
        P = Ol[loc] + np.outer(tn[loc], Dl)
        for axis in range(3):
            side = 0 if Dl[axis] > 0 else 1
            f = FACE_OF[(axis, side)]
            if f not in faces: continue
            sel = idx[ax[loc] == axis]
            if not len(sel): continue
            Ps = P[ax[loc] == axis]
            x, y, z = Ps[:, 0], Ps[:, 1], Ps[:, 2]
            fx = lambda v, lo, hi: np.clip((v - lo) / max(hi - lo, 1e-9), 0, 0.999999)
            if f == "north": s, t = fx(-x, -to[0], -frm[0]), fx(-y, -to[1], -frm[1])
            elif f == "south": s, t = fx(x, frm[0], to[0]), fx(-y, -to[1], -frm[1])
            elif f == "west": s, t = fx(z, frm[2], to[2]), fx(-y, -to[1], -frm[1])
            elif f == "east": s, t = fx(-z, -to[2], -frm[2]), fx(-y, -to[1], -frm[1])
            elif f == "up": s, t = fx(x, frm[0], to[0]), fx(z, frm[2], to[2])
            else: s, t = fx(x, frm[0], to[0]), fx(-z, -to[2], -frm[2])
            u1, v1, u2, v2 = faces[f]["uv"]
            tu = ((u1 + s * (u2 - u1)) * TS[0] / 16.0).astype(int).clip(0, TS[0] - 1)
            tv = ((v1 + t * (v2 - v1)) * TS[1] / 16.0).astype(int).clip(0, TS[1] - 1)
            c = T[tv, tu]
            ok = c[:, 3] > 0.5
            sel, c = sel[ok], c[ok]
            sh = SHADE[f] if shade else 1.0
            col[sel, :3] = c[:, :3] * sh; col[sel, 3] = 1.0
            best[sel] = tn[loc[ax[loc] == axis][ok]]
    img = Image.fromarray((col.reshape(H, W, 4) * 255).astype(np.uint8), "RGBA")
    return img

# ---- swatches and knight ------------------------------------------------------------------------------------
def swatch(kind, w, h, k):
    t = Image.new("RGBA", (16, 16)); p = t.load()
    for y in range(16):
        for x in range(16):
            n = ((x * 7 + y * 13) * 2654435761 >> 7) & 15
            if kind == "grass":
                p[x, y] = (88, 130, 56, 255) if n > 4 else ((72, 112, 46, 255) if n > 1 else (104, 148, 66, 255))
            else:
                mortar = y % 8 == 7 or (x + (8 if y // 8 % 2 else 0)) % 16 == 15
                v = 100 if mortar else (128 if n > 3 else 118)
                p[x, y] = (v, v - 2, v - 4, 255)
    t = t.resize((16 * k, 16 * k), Image.NEAREST)
    img = Image.new("RGBA", (w, h))
    for y in range(0, h, 16 * k):
        for x in range(0, w, 16 * k): img.paste(t, (x, y))
    return img

def knight_front(k):
    sys.path[:0] = [os.path.join(ROOT, "art", "hand", "common"), os.path.join(ROOT, "art", "hand", "knight")]
    try:
        import build_knight, render as trender
        B, skins, cloths = build_knight.build()
        img = trender.render(B, build_knight.KNIGHT.visible(1), {"skin": skins[1], "cloth": cloths["gold"]}, 0, 0, px=k * 4, bg=(0, 0, 0, 0))
        bb = img.getbbox(); img = img.crop(bb)
        return img.resize((max(1, img.width // 4), max(1, img.height // 4)), Image.NEAREST)
    except Exception as ex:   # the sheet still renders without the reference
        print("knight reference unavailable:", ex)
        return None

def on_swatch(img, kind, k, pad=None):
    pad = pad if pad is not None else 4 * k
    W, H = img.width + 2 * pad, img.height + 2 * pad
    bg = swatch(kind, W, H, max(1, k))
    bg.alpha_composite(img, (pad, pad)); return bg

def compose(tiles, gap=6, bg=(36, 38, 46, 255), label=None):
    W = sum(t.width for t in tiles) + gap * (len(tiles) + 1)
    H = max(t.height for t in tiles) + 2 * gap + (12 if label else 0)
    out = Image.new("RGBA", (W, H), bg); x = gap; y0 = gap + (12 if label else 0)
    if label: ImageDraw.Draw(out).text((gap, 1), label, fill=(220, 220, 220, 255))
    for t in tiles:
        out.alpha_composite(t, (x, y0 + (H - 2 * gap - (12 if label else 0)) - t.height)); x += t.width + gap
    return out

def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--art", default=os.path.join(ROOT, "art", "props"))
    ap.add_argument("--props", default="")
    ap.add_argument("--out", default="")
    ap.add_argument("--check-only", action="store_true")
    ap.add_argument("--no-knight", action="store_true")
    a = ap.parse_args(argv)
    ids = [p for p in a.props.split(",") if p] or [p for p in ORDER if os.path.isdir(os.path.join(a.art, p))] + \
          sorted(p for p in os.listdir(a.art) if os.path.isdir(os.path.join(a.art, p)) and p not in ORDER)
    bad, props = [], []
    for pid in ids:
        tex, parts = load_prop(a.art, pid)
        if not parts: bad.append("%s: no part models" % pid); continue
        for pname, m in parts:
            bad += check_density("%s/%s" % (pid, pname), m, tex.size)
            n = sum(len(e.get("faces", {})) for e in m["elements"])
            print("%-28s elements %3d faces %4d  1 texel/px %s" % (pid + "/" + pname, len(m["elements"]), n,
                  "ok" if not [b for b in bad if b.startswith(pid + "/" + pname + " ")] else "FAIL"))
        props.append((pid, tex, parts))
    for b in bad: print("DENSITY", b)
    if bad: return 1
    if a.check_only or not a.out: return 0
    kn1 = None if a.no_knight else knight_front(1)
    kn4 = None if a.no_knight else knight_front(4)
    rows = []
    for pid, tex, parts in props:
        els = elements_of(parts)
        r1 = [render(els, tex, 0, 0, 1), render(els, tex, 35, 25, 1)]
        r4 = [render(els, tex, 0, 0, 4), render(els, tex, 35, 25, 4)]
        t1 = ([on_swatch(kn1, "grass", 1)] if kn1 else []) + [on_swatch(r1[0], "grass", 1), on_swatch(r1[1], "stone", 1)]
        t4 = ([on_swatch(kn4, "grass", 4)] if kn4 else []) + [on_swatch(r4[0], "grass", 4), on_swatch(r4[1], "stone", 4),
              on_swatch(render(els, tex, -35, 25, 4), "stone", 4), on_swatch(render(els, tex, 180, 20, 4), "stone", 4)]
        rows.append(compose([compose(t1, label="1x"), compose(t4, label="4x: front, 3/4 front-east, 3/4 front-west, back")], label=pid))
    W = max(r.width for r in rows); H = sum(r.height for r in rows)
    sheet = Image.new("RGBA", (W, H), (36, 38, 46, 255)); y = 0
    for r in rows: sheet.alpha_composite(r, (0, y)); y += r.height
    os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
    sheet.save(a.out); print("sheet", a.out, sheet.size)
    return 0

if __name__ == "__main__":
    sys.exit(main())
