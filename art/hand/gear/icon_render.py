"""The gear UI pictures (Kort approved 2026-11-13): every item is a smooth baked render of its real 3D model; rarity is told
by a shared frame and glow drawn round it in game (WarTableGear.pieceIcon and the Forge reveal card). The worn models do
not change.

  picture   per item and rarity, 144 x 144 RGBA: the model fitted to 128, a soft contact shadow and a light halo, no frame
            (drawn into a 36 canvas-px box with linear filtering)
  glow(r)   per rarity, 144 x 144: the backdrop glow (grey for Common), drawn into 40 canvas px (96 on the card)
  frame(r)  per rarity, 44 x 44 canvas px drawn 1:1 nearest: 2 px border; Rare up: light corner caps and a 1 px inner rim;
            Epic up: corner diamond studs; Legendary: crests top and bottom, a gem in the top one. frame(r, 48) is the
            Forge card's 96 px box, drawn at 2x.

Per slot: the helm turned and raised; the chest without its back cloth (cape, cloak, drape, tabard back); the legs with
both legs spread 12 degrees; the boots as the bootpair low ankle-boot pair; weapons by icons.cube_render.

  python art/hand/gear/icon_render.py sheet <out.png> [k]   every item at every rarity in its Arsenal slot, at k x (2)
  python art/hand/gear/icon_render.py card <out.png> <item> <rarity 1-5> [name kind]   a Forge card mock at 2x"""
import importlib, os, re, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont
HERE = os.path.dirname(os.path.abspath(__file__))
for _d in ("weapons", "sets", os.path.join("..", "common"), ""):
    _p = os.path.normpath(os.path.join(HERE, _d))
    if _p not in sys.path: sys.path.insert(0, _p)
import gearlib, icons, render, bootpair

INK = ["9d9d9d", "3fae49", "3d7fd6", "9a4fd6", "e8892b"]   # gearlib.BORDERS, index rarity - 1
hx = lambda s: tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))
TAB = hx("252f34"); CHAR = hx("1a2125"); DAWN = hx("5fb3ad")   # DarkUi.TAB_IDLE, CHARCOAL, DAWN
PIC = 144; HI = bootpair.HI   # texture side; render scale before the smooth downscale
FRAME, FRAME_CARD = 44, 48
VIEWS = {"helm": (-38, 18), "chest": (-22, 10), "legs": (-15, 10)}
LEG_SPREAD = 12
BACK = re.compile(r"\.(cape|cloak|drape|tabard_back)")


# ---- the model renders --------------------------------------------------------------------------------------------------
def _armour(p, r, built):
    bones, vis, sheets = gearlib.worn([p], r, icons.ICON_SIDE, built)
    if p.slot == "legs":
        for bn in bones:   # the mannequin's legs (fresh copies each call), turned apart so both read
            if bn.name in ("right_leg", "left_leg"):
                s = LEG_SPREAD if bn.name == "right_leg" else -LEG_SPREAD; bn.rot = (bn.rot[0], bn.rot[1], bn.rot[2] + s)
    hide = BACK.search if p.slot == "chest" else (lambda n: False)
    own = lambda bn: bn.name.startswith(p.item_id + ".") and vis(bn) and not hide(bn.name)
    return render.render(bones, own, sheets, *VIEWS[p.slot], px=HI, bg=(0, 0, 0, 0))


def model_render(p, r, built):
    """An armour piece's raw RGBA render at rarity r (built: {item id: p.build()})."""
    if p.slot == "boots": return bootpair.boot_pair(p, r, built)
    return _armour(p, r, built)


def weapon_render(w, r):
    return icons.cube_render(w.cubes(r, icons.ICON_SIDE), *icons.POSES.get(w.item_id, (-30, 15, -45)), px=HI)


# ---- the textures -------------------------------------------------------------------------------------------------------
def picture(src):
    """The item fitted to 128 of 144, a soft dark contact shadow so it lifts off any glow, a light halo round it."""
    src = src.crop(src.getbbox()); k = 128 / max(src.size)
    o = src.resize((max(1, round(src.width * k)), max(1, round(src.height * k))), Image.LANCZOS)
    pic = Image.new("RGBA", (PIC, PIC), (0, 0, 0, 0)); at = ((PIC - o.width) // 2, (PIC - o.height) // 2)
    sh = Image.new("RGBA", (PIC, PIC), (0, 0, 0, 0))
    sh.paste((10, 12, 14, 170), (at[0] + 3, at[1] + 4, at[0] + 3 + o.width, at[1] + 4 + o.height), o.split()[3])
    sh = sh.filter(ImageFilter.GaussianBlur(4))
    halo = Image.new("RGBA", (PIC, PIC), (0, 0, 0, 0))
    halo.paste((235, 225, 200, 165), (at[0], at[1], at[0] + o.width, at[1] + o.height), o.split()[3])
    halo = halo.filter(ImageFilter.MaxFilter(7)).filter(ImageFilter.GaussianBlur(2.5))
    pic.alpha_composite(sh); pic.alpha_composite(halo); pic.alpha_composite(o, at); return pic


def glow(r, side=PIC):
    """The shared backdrop glow of rarity r (1-5): grey for Common, the rarity ink above, stronger with rarity."""
    i0 = r - 1
    g = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    a = (60, 70, 120, 160, 200)[i0]; c = (150, 156, 160) if i0 == 0 else hx(INK[i0]); d = ImageDraw.Draw(g)
    for i in range(40, 0, -1):
        rr = side * 0.5 * i / 40; t = (1 - i / 40) ** 1.6
        d.ellipse([side / 2 - rr, side / 2 - rr, side / 2 + rr, side / 2 + rr], fill=c + (int(a * t),))
    return g.filter(ImageFilter.GaussianBlur(side / 24))


def frame(r, s=FRAME):
    """The frame overlay of rarity r (1-5), s x s canvas px. Border 2 px; the ornaments grow with rarity."""
    i0 = r - 1
    f = Image.new("RGBA", (s, s), (0, 0, 0, 0)); d = ImageDraw.Draw(f); c = hx(INK[i0]) + (255,)
    lite = tuple(min(255, int(v * 1.35 + 30)) for v in hx(INK[i0])) + (255,)
    d.rectangle([0, 0, s - 1, s - 1], outline=c, width=2)
    if i0 >= 2:   # rare: corner caps in the light tone over the border, an inner rim
        L = {2: 9, 3: 10, 4: 11}[i0]
        d.rectangle([2, 2, s - 3, s - 3], outline=lite[:3] + (150,), width=1)
        for (x, y, sx, sy) in ((0, 0, 1, 1), (s - 1, 0, -1, 1), (0, s - 1, 1, -1), (s - 1, s - 1, -1, -1)):
            for t in range(2):
                d.line([(x + sx * t, y + sy * t), (x + sx * (L - 1), y + sy * t)], fill=lite)
                d.line([(x + sx * t, y + sy * t), (x + sx * t, y + sy * (L - 1))], fill=lite)
    if i0 >= 3:   # epic: a diamond stud at each corner, dark with a light core
        for (x, y) in ((2, 2), (s - 3, 2), (2, s - 3), (s - 3, s - 3)):
            d.polygon([(x, y - 2), (x + 2, y), (x, y + 2), (x - 2, y)], fill=CHAR + (255,)); d.point((x, y), fill=lite)
    if i0 >= 4:   # legendary: crests on the top and bottom edge, a gem in the top one
        cx = s // 2
        d.polygon([(cx - 6, 0), (cx + 5, 0), (cx + 3, 3), (cx - 4, 3)], fill=c); d.line([(cx - 5, 1), (cx + 4, 1)], fill=lite)
        d.rectangle([cx - 2, 0, cx + 1, 2], fill=CHAR + (255,)); d.rectangle([cx - 1, 0, cx, 1], fill=(255, 236, 170, 255))
        d.polygon([(cx - 6, s - 1), (cx + 5, s - 1), (cx + 3, s - 4), (cx - 4, s - 4)], fill=c)
    return f


# ---- the items ----------------------------------------------------------------------------------------------------------
_items, _built = {}, {}


def items():
    """{item id: GearPiece or weapon} for every set module in sets/ and every weapon."""
    if not _items:
        import weaponlib
        for f in sorted(os.listdir(os.path.join(HERE, "sets"))):
            if f.endswith(".py") and not f.startswith("_"):
                for p in importlib.import_module(f[:-3]).pieces(): _items[p.item_id] = p
        for w in weaponlib.items(): _items[w.item_id] = w
    return _items


def item_render(iid, r):
    """The raw RGBA model render of item iid at rarity r (1-5), before picture() fits and softens it."""
    it = items()[iid]
    if not isinstance(it, gearlib.GearPiece): return weapon_render(it, r)
    if iid not in _built: _built[iid] = it.build()
    return model_render(it, r, {iid: _built[iid]})


def item_picture(iid, r):
    """The 144 x 144 picture of item iid at rarity r (1-5)."""
    return picture(item_render(iid, r))


# ---- previews: the in-game look, emulated -------------------------------------------------------------------------------
def slot(pic, r, k=2, s=FRAME, dura=0.7):
    """WarTableGear.pieceIcon at canvas scale k: fill, tint, glow, the picture in a 36 px box, frame, durability bar."""
    i0 = r - 1
    S = s * k; im = Image.new("RGBA", (S, S), TAB + (255,)); d = ImageDraw.Draw(im)
    tint = 0x22 if i0 >= 2 else 0x40 if i0 == 1 else 0x30
    im.alpha_composite(Image.new("RGBA", (S - 4 * k, S - 4 * k), hx(INK[i0]) + (tint,)), (2 * k, 2 * k))
    box = 40 * k; im.alpha_composite(glow(r).resize((box, box), Image.LANCZOS), ((S - box) // 2, (S - box) // 2 - k))
    box = 36 * k; im.alpha_composite(pic.resize((box, box), Image.LANCZOS), ((S - box) // 2, (S - box) // 2 - 2 * k))
    im.alpha_composite(frame(r, s).resize((S, S), Image.NEAREST))
    bar = s - 8
    d.rectangle([4 * k, (s - 6) * k, (4 + bar) * k - 1, (s - 4) * k - 1], fill=CHAR)
    d.rectangle([4 * k, (s - 6) * k, (4 + int(bar * dura)) * k - 1, (s - 4) * k - 1], fill=DAWN)
    return im


def card(pic, r, name, kind, k=2):
    """The Forge reveal card, 300 x 150 canvas px, the picture on the left in a 96 px framed box."""
    W, H = 300 * k, 150 * k; im = Image.new("RGBA", (W, H), TAB + (255,)); d = ImageDraw.Draw(im)
    c = hx(INK[r - 1]); d.rectangle([0, 0, W - 1, H - 1], outline=c, width=3 * k)
    bx, by, bs = 14 * k, 27 * k, 96 * k
    im.alpha_composite(Image.new("RGBA", (bs, bs), c + (0x22,)), (bx, by))
    im.alpha_composite(glow(r).resize((bs, bs), Image.LANCZOS), (bx, by))
    im.alpha_composite(pic.resize((bs - 8 * k, bs - 8 * k), Image.LANCZOS), (bx + 4 * k, by + 3 * k))
    im.alpha_composite(frame(r, FRAME_CARD).resize((bs, bs), Image.NEAREST), (bx, by))
    try:
        fb = ImageFont.truetype("C:/Windows/Fonts/georgiab.ttf", 13 * k); fs = ImageFont.truetype("C:/Windows/Fonts/georgia.ttf", 10 * k)
    except Exception:
        fb = fs = ImageFont.load_default()
    tx = bx + bs + 14 * k
    d.text((tx, 40 * k), name, font=fb, fill=c); d.text((tx, 64 * k), kind, font=fs, fill=(225, 215, 190))
    d.text((tx, 86 * k), "New", font=fs, fill=DAWN); d.text((tx, 126 * k), "Click to skip", font=fs, fill=(120, 125, 128))
    return im


def sheet(out, k=2, pics=None):
    """Every item in ids.json at every rarity in its Arsenal slot, one row per item. pics: {(id, r): picture} to reuse."""
    ids = [i["id"] for i in gearlib.ids()["items"]]
    S = 44 * k; g = 6 * k; lw = 150
    im = Image.new("RGB", (lw + 5 * (S + g) + g, len(ids) * (S + g) + g), (24, 22, 28)); d = ImageDraw.Draw(im)
    for n, iid in enumerate(ids):
        y = g + n * (S + g); d.text((g, y + S // 2 - 6), iid, fill=(230, 226, 214))
        for r in gearlib.RARITIES:
            pic = pics[(iid, r)] if pics else item_picture(iid, r)
            im.paste(slot(pic, r, k).convert("RGB"), (lw + g + (r - 1) * (S + g), y))
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True); im.save(out); print("gear icon sheet", out)


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else ""
    if cmd == "sheet": sheet(sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 2)
    elif cmd == "card":
        iid, r = sys.argv[3], int(sys.argv[4])
        card(item_picture(iid, r), r, *(sys.argv[5:7] if len(sys.argv) > 6 else (iid, "Rarity %d" % r))).save(sys.argv[2])
    else: raise SystemExit(__doc__)
