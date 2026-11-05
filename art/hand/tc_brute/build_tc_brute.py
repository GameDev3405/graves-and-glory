#!/usr/bin/env python3
"""The Tidecaller Kraken-touched Brute (Tidecallers house, troop id tc_brute; Epic; a ravager entity on the hound rig with its
own boxes), hand-authored at one texel per model pixel.

A huge drowned beast, low in the quarters and towering in the shoulder: a mottled blue-black hide crusted with barnacle
clumps and hung with kelp, a heavy brow of sea-blue plate, a pale tusked maw and, from the jaw, four 8 px kraken tentacles
(the signature) ringed with suckers. A sea-blue girth strap, breast plate, pauldrons and muzzle strap are the fixed accent;
a team-colour saddle mantle drapes the shoulders (clothing, not a flag). Tier II (the Reef-Breaker): bronze plate rimmed in
sea blue over the pauldrons and brow, a bronze crest and greaves on the forelegs; the tusks are capped in steel. Tier III (the
Abyssal Brute): deep sea-blue barding chased in warm bronze over chest and back, barnacle clusters, longer tusks capped in warm
bronze. The tusks are weaponTiers parts (one head bone per tier). No back flag.

  python build_tc_brute.py preview <dir> [side] | contact <png> [side] | stub
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
sys.path.insert(0, os.path.join(HERE, "..", "tc_tideguard"))
from handlib import *   # noqa: F401,F403
import handlib as H
import house_stub
sys.path.insert(0, os.path.join(HERE, "..", "common"))
import tidecallers as TC
import build_tc_tideguard as TG   # noqa: E402
from build_tc_tideguard import VERD, BRONZE, BARN, plate_b, scuff, rng   # noqa: E402

SKIN = (256, 128)   # tier II and III plate and tusks (128 x 128 was full)
CLOTH = (128, 64)
PITCH = 9   # degrees the head is carried down (positive x lowers the nose)

HIDE = ramp("070f16", "11222c", "1f3d48", "33606a", "52848a", "82aaa8")        # blue-black drowned hide
TENT = ramp("0c1030", "182058", "2a3c88", "4262b0", "7a9ede", "bcd2f2")        # kraken flesh: saturated deep sea blue
KELP = ramp("0d140a", "1c2a12", "30431a", "4c6528", "788c3e")
GLOWSEA = ramp("7fc9b0", "cff3e4")                                              # sea-foam glow (guide 10.5)

# ---- helpers ----
def clump(g, r, w, h, n, R=BARN, patches=1):
    """n barnacle clumps in `patches` crusted patches: a pale cap, a lighter neighbour and a dark gap under it."""
    cs = [(r.randrange(w), r.randrange(max(1, h - 1))) for _ in range(patches)]
    for _ in range(n):
        cx, cy = cs[r.randrange(len(cs))]
        x = max(0, min(w - 1, cx + r.randrange(-2, 3))); y = max(0, min(h - 2 if h > 1 else 0, cy + r.randrange(-2, 3)))
        g[y][x] = R[4]
        if x + 1 < w and r.random() < 0.7: g[y][x + 1] = R[3]
        if y + 1 < h: g[y + 1][x] = R[0] if r.random() < 0.6 else R[1]
        if x > 0 and r.random() < 0.4: g[y][x - 1] = R[2]

def hide(seed, under=3, wear=2, crust=0.03, drips=True, lift=0, R=HIDE, blot=True):
    """Mottled blue-black hide: low blotches, wet drip streaks, a shadowed underside, barnacle clumps, a few raking scars."""
    def paint(face, w, h):
        g = blank(w, h); r = rng(seed, face, w, h)
        cells = {(cx, cy): r.choice((-1, 0, 0, 0, 0, 1)) for cx in range(w // 4 + 2) for cy in range(h // 4 + 2)}
        for y in range(h):
            for x in range(w):
                v = 2 + lift + (LIT[face] if face != 'back' else 0) + r.choice((-1, 0, 0, 0, 0, 0, 0, 1))
                if face not in ("top", "bottom"): v += lightx(face, x, w)
                if blot: v += cells[(x // 4, y // 4)]
                g[y][x] = clamp(R, v)
        if drips and face not in ("top", "bottom") and h > 5:
            for x in range(w):
                if r.random() < 0.16:
                    ln = 3 + r.randrange(4)
                    for y in range(min(h, ln)): g[y][x] = clamp(R, 1 + LIT[face] + (1 if y == ln - 1 else 0))
        if face == "bottom":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(R, 0 + (x + y) % 2)
        elif face != "top" and under > 0 and h > under + 1:
            for x in range(w):
                d = under + (1 if r.random() < 0.4 else 0)
                for y in range(h - d, h): g[y][x] = clamp(R, 1 + LIT[face] + (1 if y == h - d else 0))
        if crust and face != "bottom" and w * h > 12 and r.random() < 0.5: clump(g, r, w, h, max(2, int(w * h * crust * 0.6)), BARN, 1 + (w * h > 120))
        if face in ("left", "right") and h > 8 and w > 6 and r.random() < 0.6:      # an old rake of scars
            sx, sy = 2 + r.randrange(max(1, w - 6)), 1 + r.randrange(max(1, h - 8))
            for i in range(5):
                if sy + i < h and sx + i // 2 < w: g[sy + i][sx + i // 2] = clamp(R, 4)
        return scuff(g, (seed, "w", face, w, h), wear, R)
    return paint

def plate_sea(seed, base=3, rivets=True):
    def paint(face, w, h):
        g = plate_b(VERD, base, seed, rivets=rivets)(face, w, h)
        r = rng(seed, "b", face, w, h)
        if w * h > 20 and r.random() < 0.7: clump(g, r, w, h, 3, BARN, 1)                 # barnacles crusting one corner of the plate
        return g
    return paint

def strap(seed="st"):
    """Sea-blue webbing: a lit edge, a stitched centre, bronze studs; weathered."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)
                if (x + y) % 4 == 0: v -= 1
                g[y][x] = clamp(VERD, v)
        if face in ("left", "right") and h > 6:
            for y in range(1, h, 4): g[y][w // 2] = BRONZE[4]            # studs
            by = h // 2 - 1
            for y in range(by, by + 3):
                for x in range(w): g[y][x] = BRONZE[3 if y == by + 1 else 2]   # a bronze buckle
        if face == "top":
            for x in range(1, w, 4): g[0][x] = BRONZE[4]
        return scuff(g, (seed, face, w, h), max(1, w * h // 14), VERD)
    return paint

def barn_bump(seed):
    """A 3D barnacle bump: a cream cone, dark in the crater."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(BARN, 3 + LIT[face] + (1 if face == "top" else 0) + (1 if (x + y) % 3 == 0 else 0))
        if face == "top":
            g[h // 2][w // 2] = BARN[0]
        return g
    return paint

def tusk(down=False, seed="tk"):
    """Ivory tusk, a grey root and a pale point."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                t = (h - 1 - y) if not down else y                          # distance from the root
                g[y][x] = clamp(BARN, 1 + LIT[face] + (2 if t >= h - 1 else 1 if t >= h // 2 else 0) + 1)
        return g
    return paint

def tentacle(seg, seed):
    """Kraken flesh: dark ridged back, a pale sucker ring on each row of the front, a bright curling tip."""
    def paint(face, w, h):
        g = blank(w, h); r = rng(seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 2 + LIT[face] + (1 if face == "top" else 0) + (1 if seg == 1 else 0) + r.choice((0, 0, 1))
                if y == h - 1 and seg == 1: v += 1
                g[y][x] = clamp(TENT, v)
        if face in ("front", "back", "left", "right") and h > 1:
            for y in range(1, h, 2):
                if face in ("front", "back"):
                    g[y][(y // 2 + (1 if w > 2 else 0)) % w] = BARN[4]                       # a pale sucker ring
                    if w > 2: g[y][(y // 2 + 2) % w] = TENT[4]
                elif w > 1: g[y][w // 2] = TENT[4]
        return g
    return paint

def kelp(seed):
    """A hanging ribbon: dark frond, a bright vein, ragged edges and a split tip."""
    def paint(face, w, h):
        g = blank(w, h); r = rng(seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 - (y * 2) // max(1, h) + LIT[face] + r.choice((-1, 0, 0, 1))
                g[y][x] = clamp(KELP, v)
                if face in ("front", "back") and x == w // 2 and w > 1: g[y][x] = KELP[4] if y % 2 == 0 else KELP[3]
                if y > h // 2 and r.random() < 0.18 + 0.03 * (y - h // 2): g[y][x] = None
        if h > 3 and face in ("front", "back"): g[h - 1][0] = None
        return g
    return paint

def fin():
    """A frilled ear fin: dark root, a membrane that pales to a sea-blue edge."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(TENT, 1 + (h - 1 - y) // 2 + (1 if (x + y) % 2 == 0 else 0) + (LIT[face] > 0))
        return g
    return paint

def claws():
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                if x % 2 == 0 or face in ("left", "right"): g[y][x] = BARN[3 if face in ("front", "top") else 2]
        return g
    return paint

def paw(seed):
    def paint(face, w, h):
        g = hide(seed, under=0, wear=3, crust=0.08, drips=False)(face, w, h)
        if face not in ("top", "bottom"):
            for x in range(w): g[0][x] = clamp(HIDE, 1)
        return g
    return paint

def leg(seed):
    """A thick leg: hide above, a sea-blue wrap at the shin."""
    base = hide(seed, under=0, wear=3, crust=0.07)
    wrap = strap(seed + "w")
    def paint(face, w, h):
        g = base(face, w, h)
        if face not in ("top", "bottom") and h > 5:
            wr = wrap(face, w, 2)
            for i in range(2):
                for x in range(w): g[h - 3 + i][x] = wr[i][x]
        return g
    return paint

def head_painter():
    base = hide("hd", under=1, wear=3, crust=0.07)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front":
            for x in range(w): g[1][x] = HIDE[0]                      # the heavy brow
            for x in range(w): g[2][x] = HIDE[1]
            for x in (1, 2, 3, w - 4, w - 3, w - 2): g[3][x] = HIDE[0]   # sunk sockets
            g[3][2] = g[3][w - 3] = GLOWSEA[1]                        # the cold sea-foam stare
            g[3][3] = g[3][w - 4] = TENT[3]
            g[4][2] = g[4][w - 3] = HIDE[1]
        if face in ("left", "right"):
            ex = 1 if face == "left" else w - 2
            g[3][ex] = GLOWSEA[1]; g[3][ex + (1 if face == "left" else -1)] = TENT[3]
            for y in (4, 5, 6):                                         # gill slits on the cheek
                for dx in (4, 5):
                    xx = dx if face == "left" else w - 1 - dx
                    if 0 <= xx < w: g[y][xx] = HIDE[0] if y % 2 == 0 else HIDE[1]
        return g
    return paint

def muzzle_painter():
    base = hide("mz", under=1, wear=3, crust=0.05, lift=-1)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front":
            g[0][1] = g[0][w - 2] = HIDE[0]                           # flared nostrils
            for x in range(w): g[h - 1][x] = BARN[1]                  # the upper lip
        return g
    return paint

def jaw_painter():
    base = hide("jw", under=0, wear=3, crust=0.12, lift=-1)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = HIDE[0] if (x + y) % 2 == 0 else HIDE[1]     # the dark throat
        return g
    return paint

def body_painter(seed="bd"): return hide(seed, under=3, wear=5, crust=0.08)
def chest_painter():
    base = hide("ch", under=3, wear=5, crust=0.09)
    return base

# ---- cloth ----
def mantle(T, M, seed="tcb"):
    """The team saddle mantle: barry-wavy bars of the team colour, a darker fold, a ragged hem."""
    def paint(face, w, h):
        g = blank(w, h); r = rng(seed, face, w, h)
        for y in range(h):
            for x in range(w):
                wave = (y + (1 if (x // 2) % 2 else 0)) // 2 % 2 == 0
                v = (2 if wave else 1) + LIT[face] + (1 if y == 0 else 0)
                g[y][x] = clamp(T, v)
        if face not in ("top", "bottom") and h > 3:
            for x in range(w):
                q = r.random()
                if q < 0.4: g[h - 1][x] = None
                elif q < 0.7: g[h - 2][x] = clamp(T, 1)
        return g
    return paint

def cloth_painter(spec, side):
    T, M = TEAM[side]
    return mantle(T, M, "tcb-" + spec)

# ---- assembling ----
def tentacle_chain(i, x, splay, curl, fwd):
    """Tentacle i hangs from the jaw: root at jaw-local (x, 2, -8); splay = roll out (z), fwd = pitch forward, curl = bend of the tip."""
    n = f"tent_{i}"
    return [Bone(n + "_a", "jaw", (x, 2.5, -8), [Box((-1.5, 0, -1.5), (3, 4, 3), tentacle(0, n + "a"))], rot=(fwd, 0, splay)),
            Bone(n + "_b", n + "_a", (0, 4, 0), [Box((-1, 0, -1), (2, 4, 2), tentacle(1, n + "b"))], rot=(curl, 0, splay * 0.7))]

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 10, 13), [Box((-7, -6, -13), (14, 13, 13), body_painter())]))
    B.append(Bone("chest", "body", (0, 0, -13), [Box((-10, -11, -11), (20, 18, 11), chest_painter())]))
    B.append(Bone("hump", "chest", boxes=[Box((-7, -15, -9), (14, 4, 8), body_painter("hm"))]))   # the shoulder hump (Kort 2026-11-02: more bulk)
    B.append(Bone("head", "chest", (0, 3, -10), [Box((-6, -5, -8), (12, 10, 8), head_painter())], rot=(PITCH, 0, 0)))
    B.append(Bone("muzzle", "head", boxes=[Box((-4.5, 0, -13), (9, 5, 5), muzzle_painter())]))
    B.append(Bone("jaw", "head", (0, 5, -3), [Box((-4.5, 0, -10), (9, 3, 10), jaw_painter())]))
    B.append(Bone("tusks", "head", weapon_tiers=[1], boxes=[Box((-5, 4.5, -12), (1.5, 3, 1.5), tusk(down=True)), Box((3.5, 4.5, -12), (1.5, 3, 1.5), tusk(down=True))]))
    B.append(Bone("tusks_t2", "head", weapon_tiers=[2], boxes=[Box((-5, 4.5, -12), (1.5, 4, 1.5), tusk(down=True)), Box((3.5, 4.5, -12), (1.5, 4, 1.5), tusk(down=True)),
        Box((-5.2, 4.3, -12.2), (1.9, 1.2, 1.9), TC.tc_band(2, "btb")), Box((3.3, 4.3, -12.2), (1.9, 1.2, 1.9), TC.tc_band(2, "btb2"))]))
    B.append(Bone("tusks_t3", "head", weapon_tiers=[3], boxes=[Box((-5.5, 4.5, -12.5), (2, 5, 2), tusk(down=True)), Box((3.5, 4.5, -12.5), (2, 5, 2), tusk(down=True)),
        Box((-5.7, 4.3, -12.7), (2.4, 1.4, 2.4), TC.tc_band(3, "btc")), Box((3.3, 4.3, -12.7), (2.4, 1.4, 2.4), TC.tc_band(3, "btc2")),
        Box((-5, 9.5, -12), (1, 1.5, 1), TC.tc_spike(TC.WARM, 3, 2)), Box((4, 9.5, -12), (1, 1.5, 1), TC.tc_spike(TC.WARM, 3, 2))]))
    B.append(Bone("jaw_tusks", "jaw", boxes=[Box((-4.5, -3, -10), (1.5, 3, 1.5), tusk()), Box((3, -3, -10), (1.5, 3, 1.5), tusk())]))
    # the signature: four 8 px tentacles from the jaw, outer pair splayed wide, inner pair hanging forward
    for i, (x, splay, fwd, curl) in enumerate(((-3.5, 34, -28, -34), (-1.2, 12, -38, -40), (1.2, -12, -38, -40), (3.5, -34, -28, -34))):
        B.extend(tentacle_chain(i, x, splay, curl, fwd))
    B.append(Bone("muzzle_strap", "head", boxes=[Box((-4.5, 1, -10.5), (9, 4, 2), strap("ms"), dil=0.4)]))
    B.append(Bone("brow", "head", boxes=[Box((-6.5, -6.5, -8.5), (13, 2, 7), plate_sea("bw", 3), dil=0.2),
                                         Box((-1, -8.5, -7), (2, 2, 5), plate_sea("bc", 4, rivets=False))]))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"fin_{side}", "head", (c * 6, -2, -2), [Box((-0.5, -6, -1.5), (1, 6, 3), fin())], rot=(0, 0, c * 26)))
    B.append(Bone("tail", "body", (0, -3, 0), [Box((-2, 0, 0), (4, 4, 5), body_painter("tl")), Box((-1.5, 0.5, 5), (3, 3, 4), body_painter("tt"))], rot=(-8, 0, 0)))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"front_{side}_leg", "chest", (c * 6.5, 4, -4.5), [
            Box((-4, 0, -4), (8, 8, 8), leg("fl" + side)), Box((-4.5, 8, -4.5), (9, 2, 9), paw("fp" + side))]))
        B.append(Bone(f"{side}_claws", f"front_{side}_leg", boxes=[Box((-4, 8.5, -5.5), (8, 1.5, 1), claws())]))
        B.append(Bone(f"back_{side}_leg", "root", (c * 5, 14, 8), [
            Box((-3, 0, -3), (6, 8, 6), leg("bl" + side)), Box((-3.5, 8, -3.5), (7, 2, 7), paw("bp" + side))]))
        B.append(Bone(f"back_{side}_claws", f"back_{side}_leg", boxes=[Box((-3, 8.5, -4.5), (6, 1.5, 1), claws())]))
        # pauldron on the shoulder: sea-blue plate crusted with barnacles
        B.append(Bone(f"{side}_pauldron", "chest", (c * 8, -11, -5), [
            Box((-4.5, -3, -4.5), (9, 5, 9), plate_sea("pd" + side, 3))], rot=(0, 0, c * 16)))
        B.append(Bone(f"{side}_barn", "chest", boxes=[
            Box((c * 9.5 - 0.75, -16, -7), (1.5, 1.5, 1.5), barn_bump("b1" + side)), Box((c * 8 - 1, -15.5, -5.5), (2, 1, 1.5), barn_bump("b5" + side)),
            Box((c * 10 + (0 if c > 0 else -1.5), -6, -9), (1.5, 2, 2), barn_bump("b3" + side)), Box((c * 10 + (0 if c > 0 else -1.5), -2, -5), (1.5, 2, 2), barn_bump("b4" + side))]))
    # the girth strap round the chest and a breast plate
    B.append(Bone("girth", "chest", boxes=[Box((-10, -11, -4), (20, 18, 2), strap("gt"), dil=0.45)]))
    B.append(Bone("breast", "chest", boxes=[Box((-7, -7, -11.5), (14, 8, 1), plate_sea("br", 3), dil=0.1)]))
    B.append(Bone("girth_body", "body", boxes=[Box((-7, -6, -6), (14, 13, 2), strap("gb"), dil=0.45)]))
    # spine spikes
    for i, z in enumerate((-11, -7, -3)):
        B.append(Bone(f"spike_{i}", "body", boxes=[Box((-1, -9.5, z), (2, 3.5, 2), tusk(seed="sp" + str(i))), Box((-0.5, -11.5, z + 0.5), (1, 2, 1), tusk(seed="st" + str(i)))]))
    B.append(Bone("barn_back", "body", boxes=[Box((-4, -7.5, -10), (2, 2, 2), barn_bump("bb1")), Box((2, -7.5, -5), (2, 2, 2), barn_bump("bb2")),
                                              Box((5, 0, -4), (1.5, 2, 2), barn_bump("bb3"))]))
    # the saddle mantle (team colour) over the shoulders
    B.append(Bone("mantle", "chest", team=True, boxes=[
        Box((-7, -15.6, -9), (14, 1, 8), "top", dil=0.1, sheet="cloth"),
        Box((10, -11, -9), (1, 8, 9), "drape", dil=0.15, sheet="cloth"),
        Box((-11, -11, -9), (1, 8, 9), "drape", dil=0.15, sheet="cloth")]))
    B.append(Bone("rump", "body", team=True, boxes=[
        Box((-7, -6.6, -11), (14, 1, 10), "top", dil=0.1, sheet="cloth"),
        Box((7, -6, -11), (1, 6, 10), "drape", dil=0.15, sheet="cloth"),
        Box((-8, -6, -11), (1, 6, 10), "drape", dil=0.15, sheet="cloth")]))
    # kelp hanging off the shoulders, flanks, jaw and tail
    for k, (par, x, y, z, rz) in enumerate((("chest", -11.5, -10, -2, 6), ("chest", 11.5, -10, -2, -6), ("body", -7.6, -2, -9, 4),
                                            ("body", 7.6, -2, -4, -4), ("jaw", -3, 2, -6, 8), ("jaw", 3, 2, -2, -8), ("tail", 0, 3, 3, 0))):
        B.append(Bone(f"kelp_{k}", par, (x, y, z), [Box((-1, 0, -0.5), (2, 9 if par != "jaw" else 7, 1), kelp("kp%d" % k))], rot=(6, 0, rz)))
    # tier II, the Reef-Breaker: bronze plate over pauldrons and brow, a crest, foreleg greaves
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"{side}_tplate", "chest", (c * 8, -11, -5), rot=(0, 0, c * 22), stages=[2, 3], boxes=[
            Box((-5, -4.2, -5), (10, 2, 10), TC.P("btpd" + side), dil=0.2), Box((-2 + c, -6, -2), (2, 2, 2), TC.tc_spike(TC.BRONZE, 3, 2))]))
        B.append(Bone(f"{side}_greave", f"front_{side}_leg", stages=[2, 3], boxes=[Box((-4.3, 2, -4.3), (8.6, 4, 8.6), TC.P("btgv" + side), dil=0.1)]))
    B.append(Bone("brow_plate", "head", stages=[2, 3], boxes=[Box((-6.8, -7.2, -8.8), (13.6, 1.4, 7.4), TC.P("btbw", 0.03), dil=0.1),
                                                            Box((-1, -10.5, -7.5), (2, 3, 6), TC.P("btcr", 0.0))]))
    # tier III, the Abyssal Brute: deep barding over chest and back, barnacle clusters
    B.append(Bone("barding", "chest", stages=[3], boxes=[Box((-10.5, -11.5, -11.8), (21, 10, 1.2), TC.P("btbd", 0.04), dil=0.15)]))
    B.append(Bone("back_barding", "body", stages=[3], boxes=[Box((-7.5, -7, -12), (15, 1.2, 11), TC.P("btbb", 0.04), dil=0.2)]))
    B.append(Bone("barn_iii", "chest", stages=[3], boxes=[Box((-6, -16.5, -6), (2.5, 1.5, 2.5), TC.tc_barnacles("b3a")), Box((3, -16.2, -4), (2, 1.2, 2), TC.tc_barnacles("b3b")),
                                                       Box((-10.8, -4, -8), (1.5, 2.5, 2.5), TC.tc_barnacles("b3c")), Box((9.3, -3, -6), (1.5, 2.5, 2.5), TC.tc_barnacles("b3d"))]))
    return B

def glow(bone, box):
    """Sea-foam eyes, 6 texels in all, on the head only."""
    if bone.name != "head": return None
    def paint(face, w, h):
        g = blank(w, h)
        if face == "front":
            g[3][2] = g[3][w - 3] = GLOWSEA[1]
        if face in ("left", "right"):
            g[3][1 if face == "left" else w - 2] = GLOWSEA[1]
        return g
    return paint

NOTE = ("Kraken-touched Brute (unit identity, Tidecallers; Epic; ravager entity on the hound rig, own boxes): a huge drowned beast, low in the "
        "quarters and towering at the shoulder, a mottled blue-black hide crusted with barnacle clumps and hung with kelp, a pale tusked maw "
        "with four 8 px kraken tentacles from the jaw (the signature), sea-blue girth strap, breast plate, pauldrons, brow plate and muzzle "
        "strap as fixed accent, a team-colour saddle mantle over the shoulders. Tier II bronze plate and capped tusks; tier III deep sea-blue barding and longer bronze-capped tusks. No back flag.")
TC_BRUTE = Troop("tc_brute", SKIN, CLOTH, bones, cloth_painter,
                 {"rig": "hound", "weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "beast 28"}, glow=glow)
def build(): return TC_BRUTE.build()
def outputs(): return TC_BRUTE.outputs()

if __name__ == "__main__":
    house_stub.main(TC_BRUTE, NOTE, sys.argv)
