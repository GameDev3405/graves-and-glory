#!/usr/bin/env python3
"""Old Horn, the Antlered Alpha (Wildmoot house, troop id wm_oldhorn; Legendary; a polar bear entity on the hound rig with
its own bear boxes), hand-authored at one texel per model pixel.

A huge, low bear with a humped back under a wide antler rack, the widest silhouette in the House: a shaggy bark-brown
coat, a grey scarred muzzle, clay warpaint slashed across the shoulders and brow, clay-glow eyes. A bone-and-feather
mantle sits over the shoulder hump, plaques and quills bound with ochre sinew, and a hide mantle in the team colour is
draped under it over the hump and shoulders (clothing, not a flag; the spec's war-banner between the tines is an open
question for Kort and is NOT built). Tier I only. No back flag.

  python build_wm_oldhorn.py preview <dir> [side] | contact <png> [side] | stub
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from wildmoot import *   # noqa: F401,F403
import house_stub

SKIN = (128, 128)
CLOTH = (128, 64)
PITCH = 10   # the head is carried low, the rack presented

FEATHER = ramp("2a2620", "5a544a", "8a8476", "b8b2a2", "dcd6c4")

# ---- skin painters ----
def shag(seed, R=BARK, under=3, wear=3, lift=1, streak=True):
    """A shaggy bark-brown coat: long vertical strands, paler hide streaks, a darker underside."""
    def paint(face, w, h):
        g = blank(w, h); r = rng(seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + lift + LIT[face] + r.choice((-1, 0, 0, 0, 0, 0, 0, 1))
                if (x * 3 + y // 3) % 5 == 0 and y % 3 != 2: v -= 1
                g[y][x] = clamp(R, v)
        if streak and face not in ("top", "bottom") and h > 3:
            for _ in range(max(2, w * h // 10)):
                x, y = r.randrange(w), r.randrange(h - 2)
                dk = r.random() < 0.55
                g[y][x] = clamp(R, 1 + LIT[face] if dk else 4); g[y + 1][x] = clamp(R, 2 + LIT[face] if dk else 4)
        if face == "bottom":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(R, 0 + (x + y) % 2)
        elif face != "top" and under > 0 and h > under + 1:
            for x in range(w):
                d = under + (1 if r.random() < 0.4 else 0)
                for y in range(h - d, h): g[y][x] = clamp(R, 1 + LIT[face] + (1 if y == h - d else 0))
        return scuff(g, (seed, "w", face, w, h), wear, R)
    return paint

def stamp(g, face, w, cells, R=CLAY, tone_=3):
    """Clay texels at (distance from the front, y) on a side face; 'left' counts from column 0, 'right' from the last."""
    for zf, y in cells:
        x = zf if face == "left" else (w - 1 - zf if face == "right" else zf)
        if 0 <= y < len(g) and 0 <= x < w and g[y][x] is not None: g[y][x] = R[tone_]

def slash(zf, y0, n, step=2, tone_=3):
    """A diagonal warpaint slash: n rows, one column back every `step` rows."""
    return [(zf + i // step, y0 + i) for i in range(n)]

def chest_painter():
    base = shag("oc", under=3)
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("left", "right"):
            for k, zf in enumerate((1, 4, 7)):                                        # three clay slashes down the shoulder
                stamp(g, face, w, slash(zf, 7 + (k % 2), 5, 3, 3), CLAY, 3)
        if face == "front":
            for x in (3, 4, 9, 10): g[2][x] = CLAY[3]                                 # warpaint on the brow of the chest
        return g
    return paint

def body_painter():
    base = shag("ob", under=3)
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("left", "right"):
            for zf0, y0 in ((9, 1), (7, 2)):                                          # the claw-rakes of a rival, long healed
                stamp(g, face, w, slash(zf0, y0, 6, 2, 5), GREYFUR, 5)
        return g
    return paint

def head_painter():
    base = shag("oh", under=1, wear=2)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front":
            for x in range(w): g[1][x] = BARK[1]                                      # the heavy brow
            for x in (1, 2, 7, 8): g[2][x] = BARK[0]                                  # sunk sockets
            g[2][1] = g[2][2] = CLAY[3]; g[2][7] = g[2][8] = CLAY[3]                  # clay-lit eyes (the glow sheet lights them)
            for y in (3, 4, 5): g[y][1] = CLAY[3]; g[y][8] = CLAY[3]                  # warpaint tear-streaks
        if face == "top":
            for y in (1, 2): g[y][3] = CLAY[3]; g[y][6] = CLAY[3]                     # two clay marks between the horns
        if face in ("left", "right"):
            ex = 1 if face == "left" else w - 2
            for x in range(w): g[1][x] = BARK[1]
            g[2][ex] = CLAY[3]
            for y, off in ((5, 3), (6, 4)):                                           # a long pale scar raking the cheek
                xx = ex + (off if face == "left" else -off)
                if 0 <= xx < w: g[y][xx] = GREYFUR[5]
        return g
    return paint

def muzzle_painter():
    """A grey, scarred muzzle: a dark nose, a pale scar cut across the bridge, a split lip."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(GREYFUR, 3 + LIT[face] + ((x + y) % 3 == 0 and -1 or 0))
        if face == "front":
            for x in range(1, w - 1): g[0][x] = HORN[1]
            g[0][w // 2] = HORN[0]                                                    # the nose
            g[h - 1][w // 2] = GREYFUR[1]                                             # the split lip
            g[1][1] = GREYFUR[5]; g[2][2] = GREYFUR[5]                                # the scar goes through the lip
        if face == "top":
            for i in range(min(w, h) + 1):
                if i < h and i < w: g[i][i] = GREYFUR[5]                              # the scar across the bridge
        if face in ("left", "right"):
            g[1][1] = GREYFUR[5]; g[2][2] = GREYFUR[5]
        return g
    return paint

def jaw_painter():
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = clamp(GREYFUR, 2 + LIT[face])
        if face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = BONE[1] if (x + y) % 2 == 0 else GREYFUR[0]
        return g
    return paint

def ear_painter():
    def paint(face, w, h):
        g = flat(BARK, 3, "oe", 1)(face, w, h)
        if face in ("front", "back"):
            for y in range(h):
                for x in range(1, w - 1): g[y][x] = GREYFUR[2] if face == "front" else BARK[1]
        return g
    return paint

def fang():
    return bone_paint(5)

def antler_paint(seed, base=3):
    return antler(base=base, seed=seed)

def burr():
    def paint(face, w, h):
        g = lashing(OCHRE, 3)(face, w, h)
        for x in range(w): g[h - 1][x] = OCHRE[1]
        return scuff(g, ("burr", face, w, h), 2, OCHRE)
    return paint

def paw():
    def paint(face, w, h):
        g = shag("paw", under=0, wear=2, streak=False)(face, w, h)
        for y in range(h):
            for x in range(w):
                if face != "top": g[y][x] = clamp(BARK, 1 + LIT[face] + (1 if y == 0 else 0))
        return g
    return paint

def claws():
    def paint(face, w, h):
        g = blank(w, h)
        if face == "front":
            for x in range(w):
                if x % 2 == 0: g[0][x] = BONE[3]
        elif face in ("top", "bottom"):
            for y in range(h):
                for x in range(w): g[y][x] = BONE[2] if x % 2 == 0 else None
        else:
            g[0][0] = BONE[3]
        return g
    return paint

def leg_painter(seed):
    base = shag(seed, under=0, wear=2)
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("front", "back", "left", "right"):
            for x in range(w):
                if (x + (0 if face == "front" else 1)) % 2 == 0 or face != "front": g[3][x] = CLAY[3]   # a broken clay band round the shank
        return g
    return paint

def wrist():
    def paint(face, w, h):
        g = lashing(OCHRE, 3)(face, w, h)
        return scuff(g, ("wr", face, w, h), 2, OCHRE)
    return paint

def tuft():
    return shag("otf", under=0, wear=1, lift=0, streak=False)

def beard():
    return shag("ob2", under=0, wear=2, lift=-1)

def tailp():
    return shag("ot", under=0, wear=1, streak=False)

def plaque():
    """A lashed bone plaque: pale slab, a dark seam, a worn edge."""
    def paint(face, w, h):
        g = bone_paint(4, "oq")(face, w, h)
        if face == "top":
            for y in range(h): g[y][w // 2] = BONE[2]
        return g
    return paint

def lash():
    return lashing(OCHRE, 3)

def feather():
    """A flat quill: an ochre-bound root, a dark shaft down the middle, a pale vane that darkens to a worn tip."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = 3 - (1 if y >= h - 2 else 0) + (LIT[face] if face not in ("top", "bottom") else 0)
                g[y][x] = clamp(FEATHER, v + (1 if (x + y) % 3 == 0 else 0))
        if face in ("front", "back", "left", "right"):
            g[0] = [OCHRE[3] if (x + 0) % 2 == 0 else OCHRE[1] for x in range(w)]       # bound with ochre sinew
            if h > 3: g[1] = [OCHRE[2] for x in range(w)]
            if w >= 2 and face in ("left", "right"):
                for y in range(2, h): g[y][w // 2] = FEATHER[0]
        return g
    return paint

# ---- cloth ----
def hide_mantle(T, M, seed="wm-oh-mantle"):
    """The team hide mantle: a woven field, a darker fold above the hem, a ragged lower edge."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng(seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0) + (0 if (x // 2) % 2 == 0 else -1)
                if face not in ("top", "bottom") and y == 0: v += 1
                g[y][x] = clamp(T, v)
        if face not in ("top", "bottom"):
            if h > 3:
                for x in range(w): g[h - 2][x] = clamp(T, 1)
            for x in range(w):
                q = r.random()
                if q < 0.45: g[h - 1][x] = None
                elif q < 0.75: g[h - 1][x] = clamp(T, 0)
        return g
    return paint

# ---- assembling ----
def antler_chain(side):
    """One antler of the rack, c = +1 on the wearer's left. Head-local coordinates, tips lashed in ochre."""
    c = 1 if side == "left" else -1
    n = f"antler_{side}"
    B = [Bone(n + "_a", "head", (c * 2.5, -3.5, -3.5), [Box((-1, -5, -1), (2, 5, 2), antler_paint(n + "a", 2))], rot=(0, 0, c * 8)),
         Bone(n + "_b", n + "_a", (0, -5, 0), [Box((-1, -5, -1), (2, 5, 2), antler_paint(n + "b", 3))], rot=(0, 0, c * 14)),
         Bone(n + "_c", n + "_b", (0, -5, 0), [Box((-1, -4, -1), (2, 4, 2), antler_paint(n + "c", 3))], rot=(0, 0, -c * 26)),
         Bone(n + "_brow", n + "_a", (0, -2, 0), [Box((-0.5, -4, -0.5), (1, 4, 1), antler_paint(n + "br", 3))], rot=(46, 0, c * -6)),
         Bone(n + "_mid", n + "_b", (0, -2.5, 0), [Box((-0.5, -4, -0.5), (1, 4, 1), antler_paint(n + "m", 3))], rot=(0, 0, c * 36)),
         Bone(n + "_back", n + "_b", (0, -3.5, 0), [Box((-0.5, -3, -0.5), (1, 3, 1), antler_paint(n + "bk", 3))], rot=(-44, 0, c * -6)),
         Bone(n + "_tip1", n + "_c", (0, -4, 0), [Box((-0.5, -3, -0.5), (1, 3, 1), antler_paint(n + "t1", 4))], rot=(0, 0, c * 24)),
         Bone(n + "_tip2", n + "_c", (0, -4, 0), [Box((-0.5, -3, -0.5), (1, 3, 1), antler_paint(n + "t2", 4))], rot=(0, 0, -c * 22))]
    return B

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 11, 12), [Box((-7, -5, -12), (14, 11, 12), body_painter())]))
    B.append(Bone("chest", "body", (0, 0, -12), [Box((-7, -8, -10), (14, 14, 10), chest_painter())]))
    B.append(Bone("head", "chest", (0, 3, -9.5), [Box((-5, -4, -7), (10, 8, 7), head_painter())], rot=(PITCH, 0, 0)))
    B.append(Bone("muzzle", "head", boxes=[Box((-3, -1, -11), (6, 4, 4), muzzle_painter())]))
    B.append(Bone("jaw", "head", (0, 3, -7), [Box((-2, 0, -4), (4, 1, 4), jaw_painter())]))
    B.append(Bone("fangs", "head", boxes=[Box((-3, 3, -10.5), (1, 2, 1), fang()), Box((2, 3, -10.5), (1, 2, 1), fang())]))
    B.append(Bone("beard", "head", boxes=[Box((-4, 4, -6), (8, 2, 5), beard(), dil=0.1)]))
    B.append(Bone("ear_right", "head", (-4.5, -3.5, -1), [Box((-1, -2, -0.5), (2, 2, 1), ear_painter())], rot=(0, 0, -12)))
    B.append(Bone("ear_left", "head", (4.5, -3.5, -1), [Box((-1, -2, -0.5), (2, 2, 1), ear_painter())], rot=(0, 0, 12)))
    B.append(Bone("burrs", "head", boxes=[Box((1, -5, -5), (3, 2, 3), burr(), dil=0.2), Box((-4, -5, -5), (3, 2, 3), burr(), dil=0.2)]))
    for side in ("right", "left"):
        B.extend(antler_chain(side))
    B.append(Bone("fringe", "chest", boxes=[Box((-7, 3, -10), (14, 3, 10), pelt(R=BARK, seed="oh-fr"), dil=0.4)]))
    B.append(Bone("fringe_body", "body", boxes=[Box((-7, 3, -12), (14, 3, 12), pelt(R=BARK, seed="oh-fb"), dil=0.4)]))
    B.append(Bone("spine_tufts", "body", boxes=[
        Box((-1.5, -7, -10), (3, 2, 3), tuft()), Box((-1.5, -6.5, -6), (3, 2, 3), tuft()), Box((-1.5, -6.5, -2), (3, 2, 3), tuft())]))
    B.append(Bone("tail", "body", (0, -3, 0), [Box((-1.5, 0, 0), (3, 3, 2), tailp())]))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"front_{side}_leg", "chest", (c * 4.5, 3, -5), [
            Box((-2.5, 0, -2.5), (5, 8, 5), leg_painter("fl" + side)), Box((-3, 8, -4), (6, 2, 6), paw())]))
        B.append(Bone(f"{side}_wrist", f"front_{side}_leg", boxes=[Box((-2.5, 5, -2.5), (5, 1, 5), wrist(), dil=0.3)]))
        B.append(Bone(f"{side}_claws", f"front_{side}_leg", boxes=[Box((-2.5, 9, -5), (5, 1, 1), claws())]))
        B.append(Bone(f"back_{side}_leg", "root", (c * 4.5, 14, 8), [
            Box((-2.5, 0, -2.5), (5, 8, 5), leg_painter("bl" + side)), Box((-3, 8, -4), (6, 2, 6), paw())]))
        B.append(Bone(f"back_{side}_claws", f"back_{side}_leg", boxes=[Box((-2.5, 9, -5), (5, 1, 1), claws())]))
    # the hide mantle: team colour, draped over the hump and the shoulders under the bone-and-feather mantle
    B.append(Bone("hump", "chest", boxes=[Box((-5, -10.5, -9), (10, 3, 8), shag("ohump", under=0, wear=2))]))
    B.append(Bone("mantle", "chest", team=True, boxes=[
        Box((-7, -8, -9), (14, 3, 8), "top", dil=0.3, sheet="cloth"),
        Box((4, -5, -8), (3, 4, 8), "drape", dil=0.2, sheet="cloth"),
        Box((-7, -5, -8), (3, 4, 8), "drape", dil=0.2, sheet="cloth")]))
    # the bone-and-feather mantle: plaques lashed in ochre across the hump, quills hanging over the shoulders
    B.append(Bone("plaques", "chest", boxes=[
        Box((-4, -11.4, -7.5), (2, 1, 4), plaque()), Box((-1, -11.4, -7.5), (2, 1, 4), plaque()), Box((2, -11.4, -7.5), (2, 1, 4), plaque()),
        Box((-2, -11.4, -7.5), (1, 1, 4), lash(), dil=0.05), Box((1, -11.4, -7.5), (1, 1, 4), lash(), dil=0.05),
        Box((-4, -11.4, -4), (8, 1, 1), lash(), dil=0.05)]))
    for side, c in (("right", -1), ("left", 1)):
        for i, (zq, ang, ln) in enumerate(((-6, 14, 6), (-4, 26, 7), (-2, 38, 6))):
            B.append(Bone(f"quill_{side}_{i}", "chest", (c * 5.3, -9.8, zq + 2.5), [Box((-0.5, 0, -1), (1, ln, 2), feather())], rot=(0, 0, -c * ang)))
    for i, x in enumerate((-3.5, -1.2, 1.2, 3.5)):
        B.append(Bone(f"quill_back_{i}", "chest", (x, -10, -1.5), [Box((-1, 0, -0.5), (2, 6, 1), feather())], rot=(28 + 6 * (i % 2), 0, 0)))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    return hide_mantle(T, M, "wm-oh-" + spec)

def glow(bone, box):
    """Clay-glow eyes and a few warpaint marks, 14 texels in all, on the head only."""
    if bone.name != "head": return None
    def paint(face, w, h):
        g = blank(w, h)
        if face == "front":
            for x in (1, 2, 7, 8): g[2][x] = GLOWCLAY[2]                              # the eyes
            for y in (3, 4, 5): g[y][1] = GLOWCLAY[1]; g[y][8] = GLOWCLAY[1]           # tear-streaks
        if face == "top":
            for y in (1, 2): g[y][3] = GLOWCLAY[1]; g[y][6] = GLOWCLAY[1]
        return g
    return paint

NOTE = ("Old Horn, the Antlered Alpha (unit identity, Wildmoot; Legendary; polar bear entity on the hound rig, own bear boxes): a huge low "
        "bear with a hump under a 16 px antler rack of branching tines, a shaggy bark coat, a grey scarred muzzle, clay warpaint and clay-glow "
        "eyes, a bone-and-feather mantle bound in ochre over the shoulder hump on a team-colour hide mantle. No war-banner (an open question "
        "for Kort). Tier I only. No back flag.")
WM_OLDHORN = Troop("wm_oldhorn", SKIN, CLOTH, bones, cloth_painter,
                   {"rig": "hound", "weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "beast 24"}, tiers=(1,), glow=glow)
def build(): return WM_OLDHORN.build()
def outputs(): return WM_OLDHORN.outputs()

if __name__ == "__main__":
    house_stub.main(WM_OLDHORN, NOTE, sys.argv)
