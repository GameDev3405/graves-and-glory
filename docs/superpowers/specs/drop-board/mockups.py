#!/usr/bin/env python3
"""Drop board mock-ups for the design page (concept pictures, not game art). Writes JPEGs beside this file.
python docs/superpowers/specs/drop-board/mockups.py"""
import math, random
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUT = Path(__file__).resolve().parent
B = 16  # px a block
MULT = [10, 5, 3, 2, 1, 2, 3, 5, 10]
MCOL = {1: (150, 150, 158), 2: (120, 190, 230), 3: (90, 220, 200), 5: (176, 120, 255), 10: (255, 236, 170)}
RARITY = {"common": (157, 157, 157), "rare": (61, 127, 214), "epic": (154, 79, 214), "legendary": (232, 137, 43)}
STONE = [(52, 54, 62), (62, 64, 73), (72, 75, 85), (84, 87, 98)]
BRONZE = [(92, 62, 30), (132, 92, 44), (176, 128, 62), (214, 170, 96)]
BONE = [(150, 140, 120), (196, 186, 162), (232, 224, 200)]
W, H = 11 * B, 13 * B


def font(size):
    for name in ("C:/Windows/Fonts/segoeuib.ttf", "C:/Windows/Fonts/arialbd.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            pass
    return ImageFont.load_default()


def h01(x, y, s=0):
    h = (x * 374761393 + y * 668265263 + s * 362437) & 0xffffffff
    h = ((h ^ (h >> 13)) * 1274126177) & 0xffffffff
    return ((h ^ (h >> 16)) & 0xffff) / 65536.0


def pegs():
    out = []
    for r in range(8):
        y = 2 * B + 8 + r * B
        if r % 2 == 0:
            xs = [B + B * i for i in range(10)]
        else:
            xs = [B + 8 + B * i for i in range(9)]
        out += [(x, y) for x in xs]
    return out


def board(lit=None, coin=None, trail=None, glow=1.0, item=None):
    """The board, 1 px = 1 model px. lit: slot index flaring. coin: (x, y). item: (slot, rarity, rise 0..1)."""
    im = Image.new("RGB", (W, H), (0, 0, 0))
    d = ImageDraw.Draw(im)
    # slab: big stone courses, 2 blocks wide, 1 high, staggered
    for y in range(H):
        for x in range(W):
            row = y // B
            col = (x + (B if row % 2 else 0)) // (2 * B)
            t = 1 + int(h01(col, row, 3) * 2)
            if y % B == 0 or (x + (B if row % 2 else 0)) % (2 * B) == 0:
                t = 0
            if h01(x // 2, y // 2, 9) > 0.93:
                t = max(0, t - 1)
            im.putpixel((x, y), STONE[t])
    # bronze frame: pillars, lintel, base
    def bronze(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                t = 1 + (1 if (x - x0) < 3 or (y - y0) < 2 else 0) - (1 if (x1 - x) <= 2 or (y1 - y) <= 2 else 0)
                im.putpixel((x, y), BRONZE[max(0, min(3, t))])
    bronze(0, 0, 10, H - 24); bronze(W - 10, 0, W, H - 24); bronze(0, 0, W, 12)
    for y in range(8, H - 28, 16):  # rivets
        for x in (4, W - 6):
            d.rectangle([x, y, x + 1, y + 1], fill=BRONZE[3])
    # crown: a giant's eye socket of glimpur crystal
    cx = W // 2
    d.polygon([(cx - 14, 12), (cx + 14, 12), (cx + 8, 22), (cx - 8, 22)], fill=BRONZE[1])
    d.polygon([(cx - 6, 13), (cx + 6, 13), (cx, 21)], fill=(200, 170, 255))
    d.point((cx - 1, 15), fill=(255, 255, 255))
    # rail
    d.rectangle([12, 26, W - 13, 28], fill=BRONZE[2]); d.rectangle([12, 29, W - 13, 29], fill=BRONZE[0])
    # pegs: bone studs
    for (x, y) in pegs():
        d.rectangle([x - 2, y - 2, x + 1, y + 1], fill=BONE[1])
        d.rectangle([x - 2, y - 2, x, y - 2], fill=BONE[2]); d.rectangle([x - 1, y + 1, x + 1, y + 1], fill=BONE[0])
    # slots
    sy0, sy1 = 2 * B + 8 * B + 8, 2 * B + 8 * B + 8 + 28
    d.rectangle([B, sy0, W - B - 1, sy1], fill=(22, 22, 28))
    for i, m in enumerate(MULT):
        x0 = B + i * B
        c = MCOL[m]
        k = glow * (1.0 if lit is None or lit == i else 0.35)
        if lit == i:
            k = 1.6
        col = tuple(min(255, int(v * (0.35 + 0.5 * k))) for v in c)
        d.rectangle([x0, sy0 - 4, x0, sy1], fill=BRONZE[1]); d.rectangle([x0 + B - 1, sy0 - 4, x0 + B - 1, sy1], fill=BRONZE[0])
        d.rectangle([x0 + 2, sy1 - 3, x0 + B - 3, sy1 - 1], fill=col)  # glowing floor
        # rune numerals: pips
        glyph(d, x0 + 2, sy0 + 5, m, col)
        if lit == i:
            for yy in range(sy0 - 40, sy0 + 2):
                a = (yy - (sy0 - 40)) / 42
                for xx in range(x0 + 2, x0 + B - 2):
                    p = im.getpixel((xx, yy))
                    im.putpixel((xx, yy), tuple(min(255, int(p[j] + c[j] * 0.55 * a * a)) for j in range(3)))
    # base plinth
    for y in range(H - 24, H):
        for x in range(W):
            t = 2 if y < H - 20 else 1 + int(h01(x // 8, y // 6, 5) * 2)
            im.putpixel((x, y), STONE[min(3, t)])
    d.rectangle([0, H - 24, W - 1, H - 23], fill=STONE[3])
    if trail:
        for i, (x, y) in enumerate(trail):
            a = (i + 1) / len(trail)
            p = im.getpixel((int(x), int(y)))
            im.putpixel((int(x), int(y)), tuple(min(255, int(p[j] + (220, 190, 110)[j] * 0.5 * a)) for j in range(3)))
    if item:
        slot, rar, rise = item
        x0 = B + slot * B + B // 2
        y = sy0 - int(rise * 46)
        c = RARITY[rar]
        for r in range(22, 0, -1):
            a = (1 - r / 22) ** 2
            for ang in range(0, 360, 6):
                xx = int(x0 + r * math.cos(math.radians(ang))); yy = int(y + r * math.sin(math.radians(ang)))
                if 0 <= xx < W and 0 <= yy < H:
                    p = im.getpixel((xx, yy))
                    im.putpixel((xx, yy), tuple(min(255, int(p[j] + c[j] * 0.10 * a)) for j in range(3)))
        # a helm silhouette as the stand-in for the piece
        d.rectangle([x0 - 5, y - 6, x0 + 4, y + 5], fill=(214, 214, 224)); d.rectangle([x0 - 5, y - 6, x0 + 4, y - 5], fill=(245, 245, 250))
        d.rectangle([x0 - 3, y - 1, x0 + 2, y], fill=(30, 30, 40)); d.rectangle([x0 - 1, y - 9, x0, y - 7], fill=c)
        d.rectangle([x0 - 6, y - 7, x0 + 5, y + 6], outline=c)
    if coin:
        x, y = coin
        d.ellipse([x - 5, y - 5, x + 4, y + 4], fill=BRONZE[2], outline=BRONZE[0])
        d.ellipse([x - 3, y - 3, x + 2, y + 2], outline=BRONZE[3])
        d.point((x - 1, y - 1), fill=(255, 240, 200)); d.point((x, y), fill=BRONZE[3])
    return im


def glyph(d, x, y, m, col):
    """Giant numerals: bars and a ring; with the plain number under them."""
    pat = {1: ["....", ".##.", ".##.", "...."], 2: ["#..#", "#..#", "#..#", "...."], 3: ["#.#.", "#.#.", "#.#.", "..##"],
           5: ["####", "#...", "###.", "####"], 10: ["#.##", "#.##", "#.##", "#.##"]}[m]
    for j, row in enumerate(pat):
        for i, ch in enumerate(row):
            if ch == "#":
                d.rectangle([x + i * 3, y + j * 3, x + i * 3 + 1, y + j * 3 + 1], fill=col)


def knight(scale=1.0):
    """A 2-block troop silhouette for scale."""
    w, h = int(14 * scale), int(32 * scale)
    im = Image.new("RGBA", (max(w, 4), max(h, 8)), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    s = scale
    d.rectangle([int(4 * s), 0, int(10 * s), int(8 * s)], fill=(200, 200, 210, 255))
    d.rectangle([int(2 * s), int(8 * s), int(12 * s), int(20 * s)], fill=(214, 170, 60, 255))
    d.rectangle([int(3 * s), int(20 * s), int(6 * s), int(32 * s)], fill=(70, 80, 120, 255))
    d.rectangle([int(8 * s), int(20 * s), int(11 * s), int(32 * s)], fill=(70, 80, 120, 255))
    return im


def scene(bd, scale, size, ground=0.86, label=None, dress=True, knight_at=None, bscale=1.0):
    """The board standing in a night court, scaled up with hard pixels."""
    cw, ch = size
    im = Image.new("RGB", (cw, ch), (0, 0, 0))
    d = ImageDraw.Draw(im)
    for y in range(ch):
        t = y / ch
        c = (int(10 + 14 * t), int(12 + 12 * t), int(26 + 22 * t))
        d.line([(0, y), (cw, y)], fill=c)
    random.seed(7)
    for _ in range(90):
        x, y = random.randrange(cw), random.randrange(int(ch * 0.6))
        d.point((x, y), fill=(200, 200, 230))
    gy = int(ch * ground)
    d.rectangle([0, gy, cw, ch], fill=(26, 30, 30))
    for x in range(0, cw, scale * 16):
        d.line([(x, gy), (x - 60, ch)], fill=(34, 38, 38))
    for y in range(gy, ch, scale * 6):
        d.line([(0, y), (cw, y)], fill=(34, 38, 38))
    bw, bh = int(bd.width * scale * bscale), int(bd.height * scale * bscale)
    big = bd.resize((bw, bh), Image.NEAREST)
    bx, by = (cw - bw) // 2, gy - bh
    # glow behind
    halo = Image.new("RGB", (cw, ch), (0, 0, 0))
    hd = ImageDraw.Draw(halo)
    hd.ellipse([bx - bw // 3, by + bh // 3, bx + bw + bw // 3, gy + bh // 6], fill=(46, 30, 78))
    halo = halo.filter(ImageFilter.GaussianBlur(scale * 14))
    im = Image.blend(im, Image.eval(Image.merge("RGB", [Image.eval(a, lambda v: v) for a in im.split()]), lambda v: v), 0)
    im = Image.fromarray(__import__("numpy").clip(__import__("numpy").asarray(im).astype("int16") + __import__("numpy").asarray(halo).astype("int16"), 0, 255).astype("uint8"))
    im.paste(big, (bx, by))
    d = ImageDraw.Draw(im)
    if dress:
        # braziers either side, a broken giant finger of stone
        for sx in (bx - scale * 22, bx + bw + scale * 10):
            d.rectangle([sx, gy - scale * 18, sx + scale * 10, gy], fill=(48, 50, 58))
            d.rectangle([sx - scale * 2, gy - scale * 22, sx + scale * 12, gy - scale * 18], fill=BRONZE[1])
            d.polygon([(sx, gy - scale * 22), (sx + scale * 10, gy - scale * 22), (sx + scale * 5, gy - scale * 36)], fill=(200, 170, 255))
            d.polygon([(sx + scale * 3, gy - scale * 22), (sx + scale * 7, gy - scale * 22), (sx + scale * 5, gy - scale * 30)], fill=(250, 240, 255))
    if knight_at is not None:
        k = knight(scale)
        im.paste(k, (bx + knight_at, gy - k.height), k)
    if label:
        d.text((14, 10), label, font=font(22), fill=(240, 232, 214))
    return im, (bx, by)


def sim_path(start_x, target_slot, seed):
    """A quick stand-in bounce path for the picture (the game uses the real server sim)."""
    rnd = random.Random(seed)
    for _ in range(20000):
        x, y, vx, vy = start_x, 30.0, 0.0, 0.0
        pts = []
        ok = False
        for step in range(4000):
            vy += 0.012
            x += vx; y += vy
            for (px, py) in pegs():
                dx, dy = x - px, y - py
                dist = math.hypot(dx, dy)
                if dist < 7 and dist > 0:
                    nx, ny = dx / dist, dy / dist
                    dot = vx * nx + vy * ny
                    if dot < 0:
                        vx -= 1.55 * dot * nx; vy -= 1.55 * dot * ny
                        vx += rnd.uniform(-0.05, 0.05)
                    x, y = px + nx * 7, py + ny * 7
            if x < B + 5: x, vx = B + 5, abs(vx) * 0.5
            if x > W - B - 6: x, vx = W - B - 6, -abs(vx) * 0.5
            if step % 6 == 0: pts.append((x, y))
            if y > 2 * B + 8 * B + 10:
                ok = int((x - B) // B) == target_slot
                break
        if ok:
            return pts
    return pts


def save(im, name):
    im.convert("RGB").save(OUT / name, quality=88)
    print(name, im.size)


def main():
    S = 4
    # 1 the board, idle
    im, _ = scene(board(), S, (1100, 980), label="The altar board (giant size: 11 wide, 13 tall; a troop is 2 tall)", knight_at=-70)
    save(im, "board.jpg")
    # 2 size comparison
    a, _ = scene(board(), 3, (640, 760), label="Giant: 11 x 13 blocks", knight_at=-56)
    b, _ = scene(board(), 3, (640, 760), label="Large: 7 x 9 blocks", knight_at=-56, bscale=0.66)
    both = Image.new("RGB", (1280, 760)); both.paste(a, (0, 0)); both.paste(b, (640, 0))
    save(both, "size.jpg")
    # 3 storyboard
    path = sim_path(70.0, 7, 5)
    frames = []
    def panel(bd, title, sub):
        p, _ = scene(bd, 2, (440, 560), dress=True)
        d = ImageDraw.Draw(p)
        d.rectangle([0, 0, 440, 34], fill=(0, 0, 0)); d.rectangle([0, 526, 440, 560], fill=(0, 0, 0))
        d.text((10, 5), title, font=font(18), fill=(240, 232, 214))
        d.text((10, 533), sub, font=font(14), fill=(190, 184, 170))
        return p
    frames.append(panel(board(coin=(70, 22)), "1  Fly in, aim (0 - 3 s)", "Coin follows the mouse on the rail. Click drops."))
    mid = len(path) * 55 // 100
    frames.append(panel(board(coin=tuple(map(int, path[mid])), trail=path[:mid]), "2  Drop (about 5 s)", "Real bounces, each peg rings. Camera follows."))
    frames.append(panel(board(lit=7, coin=(B + 7 * B + 8, 2 * B + 8 * B + 20), glow=0.6), "3  Slot flares (1 s)", "x5 lights violet; the other runes dim."))
    frames.append(panel(board(lit=7, glow=0.6, item=(7, "epic", 1.0)), "4  Reveal (2.5 s)", "The piece rises in its rarity colour."))
    sb = Image.new("RGB", (440 * 4 + 30, 560), (0, 0, 0))
    for i, f in enumerate(frames):
        sb.paste(f, (i * 450, 0))
    save(sb, "storyboard.jpg")
    # 4 lobby plan
    lobby()


def lobby():
    s = 5  # px a block
    half = 100
    im = Image.new("RGB", (half * 2 * s, int(half * 2.1 * s)), (38, 54, 38))
    d = ImageDraw.Draw(im)
    def X(x): return (x + half) * s
    def Z(z): return (z + 115) * s
    areas = [("Hearth court", -18, -24, 18, 10, (120, 84, 40)), ("War Table", -5, -12, 5, -2, (170, 120, 60)), ("Spawn", -7, 14, 7, 24, (120, 120, 120)),
             ("House tents", -47, -58, 47, -40, (70, 70, 90)), ("Graveyard", -20, -90, 20, -66, (40, 40, 44)), ("Armoury", -44, -24, -26, -8, (110, 50, 50)),
             ("Training yard", -84, -30, -50, 10, (70, 100, 60)), ("War Gates", -12, 44, 12, 51, (120, 80, 100))]
    f = font(15)
    for name, x0, z0, x1, z1, c in areas:
        d.rectangle([X(x0), Z(z0), X(x1), Z(z1)], fill=c, outline=(20, 20, 20))
        d.text((X(x0) + 4, Z(z0) + 3), name, font=f, fill=(240, 232, 214))
    def spot(x, z, w, dep, tag, text, col):
        d.rectangle([X(x - w // 2), Z(z), X(x + w // 2), Z(z + dep)], fill=col, outline=(255, 255, 255), width=2)
        d.text((X(x - w // 2), Z(z) - 40), tag, font=font(20), fill=(255, 236, 170))
        d.text((X(x - w // 2), Z(z + dep) + 4), text, font=f, fill=(240, 232, 214))
    spot(34, -22, 15, 12, "A  East of the hearth court", "40 blocks from the table, faces west:\nseen from the table, walk-up watching", (150, 110, 230))
    spot(0, -106, 15, 10, "B  Beyond the graveyard", "End of the north axis, faces south:\na long reveal walk past the tents", (150, 110, 230))
    d.rectangle([X(60), Z(60), X(96), Z(84)], outline=(255, 255, 255), width=2)
    d.text((X(60) + 4, Z(60) + 4), "C  Its own stage,\noff the island beside\nthe podium (never\nwalked to)", font=f, fill=(255, 236, 170))
    d.text((10, 8), "Lobby plan (north up). Purple: where the altar could stand.", font=font(20), fill=(240, 232, 214))
    save(im, "lobby.jpg")


if __name__ == "__main__":
    main()
