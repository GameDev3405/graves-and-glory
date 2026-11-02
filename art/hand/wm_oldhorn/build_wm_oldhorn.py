#!/usr/bin/env python3
"""Old Horn, the Antlered Alpha (Wildmoot house, troop id wm_oldhorn; Legendary; a polar bear entity on the hound rig with
its own bear boxes), hand-authored at one texel per model pixel.

A huge, low bear with a high shoulder hump, a heavy neck ruff and thick forelegs, under a branching antler rack some
30 px across, the widest silhouette in the House: a shaggy bark-brown coat, a grey scarred muzzle, clay warpaint slashed across the shoulders and brow, clay-glow eyes. A bone-and-feather
mantle sits over the shoulder hump, plaques and quills bound with ochre sinew, and a hide mantle in the team colour is
draped under it over the hump and shoulders (clothing, not a flag; the spec's war-banner between the tines is an open
question for Kort and is NOT built). No back flag.

Tier I is the bear in his coat and mantle. Tier II straps bone to him: a bone plate over the brow, bone greaves on
the forelegs and ochre bands with bone beads round the rack's beams. Bone pauldrons are lashed over the mantle's drapes. Tier III adds the
trophies: a crest of boar tusks rising out of the hump, a skull hung from each pauldron, tusks on the greaves, and every
tine's tip dipped in ochre.

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

def cheek(seed):
    """A jowl ruff: shag combed down and back, a grey fleck of age on the front face."""
    base = shag("oc" + seed, under=1, wear=2, lift=0)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front":
            for y in range(1, h - 1, 2): g[y][w // 2] = GREYFUR[3]
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

def antler_paint(seed, base=3, dip=False):
    """Antler; dip=True (the tines at tier III) dips the top two rows of each tine in ochre."""
    a = antler(base=base, seed=seed)
    if not dip: return a
    def paint(face, w, h):
        g = a(face, w, h)
        if face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = OCHRE[4]
        elif face != "bottom":
            for x in range(w):
                g[0][x] = OCHRE[4] if face in ("front", "right") else OCHRE[3]
                if h > 4: g[1][x] = OCHRE[2]
        return g
    return paint

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
                v = 3 - (1 if y >= h - 2 else 0) + (max(0, LIT[face]) if face not in ("top", "bottom") else 0)   # pale vanes on every side
                g[y][x] = clamp(FEATHER, v + (1 if (x + y) % 3 == 0 else 0))
        if face in ("front", "back", "left", "right"):
            g[0] = [OCHRE[3] if (x + 0) % 2 == 0 else OCHRE[1] for x in range(w)]       # bound with ochre sinew
            if h > 3: g[1] = [OCHRE[2] for x in range(w)]
            if w >= 2 and face in ("left", "right"):
                for y in range(2, h, 2): g[y][w // 2] = FEATHER[1]                     # a broken shaft line, not a dark slab
        return g
    return paint

# ---- tiers II and III: bone greaves, a brow plate, rack bands, then a tusk collar, greave tusks and ochre-dipped tines ----
def brow_plate(tier):
    """A bone plate lashed over the brow: a pale slab with a dark seam; at III its edge is painted ochre and clay."""
    def paint(face, w, h):
        g = bone_paint(4, "obp")(face, w, h)
        if face == "front":
            g[h - 1] = [BONE[2] if x % 2 else BONE[3] for x in range(w)]
            g[0][w // 2] = g[0][w // 2 - 1] = BONE[5]
            if tier == 3:
                g[0] = [OCHRE[3] if x % 2 == 0 else BONE[5] for x in range(w)]
                g[h - 1][1] = g[h - 1][w - 2] = CLAY[4]
        if face in ("left", "right"): g[0][0] = OCHRE[3]
        return g
    return paint

def greave(tier):
    """A bone greave strapped to the foreleg's front: two plates, an ochre strap between."""
    def paint(face, w, h):
        g = bone_paint(3, "ogr")(face, w, h)
        if face not in ("top", "bottom") and h >= 3:
            g[h // 2] = [OCHRE[3] if x % 2 == 0 else OCHRE[1] for x in range(w)]
        if face == "front":
            g[0] = [BONE[5]] * w
            if tier == 3:
                for y in range(h): g[y][0] = g[y][w - 1] = BONE[2]
        return g
    return paint

def pauldron(tier):
    """A bone shoulder slab: two lashed plates, a dark seam between, an ochre strap; at III a row of ochre-painted dots."""
    def paint(face, w, h):
        g = bone_paint(3, "opd")(face, w, h)
        if face in ("left", "right"):
            for y in range(h): g[y][w // 2] = BONE[1]
            for x in range(w): g[1][x] = OCHRE[3] if x % 2 == 0 else OCHRE[1]
            g[0] = [BONE[5]] * w
            if tier == 3:
                for x in range(1, w, 2): g[h - 2][x] = OCHRE[4]
        return g
    return paint

def band():
    """An ochre sinew band round an antler beam, a bone bead set in it."""
    def paint(face, w, h):
        g = lashing(OCHRE, 3)(face, w, h)
        if face == "front": g[h // 2][w // 2] = BONE[5]
        return g
    return paint

def bead():
    return bone_paint(4, "obd")

def tusk():
    """A boar tusk, pale at the root, darker at the tip (the bottom row)."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = clamp(BONE, 4 + (LIT[face] if face not in ("top", "bottom") else 1) - (y * 2 // max(1, h)))
        if face not in ("top", "bottom"): g[h - 1] = [BONE[2]] * w
        return g
    return paint

def skull_t():
    """A small trophy skull: bone, two dark sockets and a nasal notch on the front."""
    def paint(face, w, h):
        g = bone_paint(4, "osk")(face, w, h)
        if face == "front" and w >= 3 and h >= 3:
            g[1][0] = g[1][w - 1] = HORN[0]; g[2][w // 2] = HORN[1]
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
    """One antler of the rack, c = +1 on the wearer's left. Head-local coordinates, rotations relative to the parent tine.

    A heavy 2 x 2 beam leaves the burr low and wide, sweeps out and back, then climbs; brow and bez tines stab forward
    over the brow, two long tines rise off the outer beam, and the crown forks into three. About 30 px across, the beams
    rising some 18 px over the head: the widest silhouette in the House."""
    c = 1 if side == "left" else -1
    n = f"antler_{side}"
    def paints(nm, base):   # the beams stay bone every tier; at III each tine's tip is dipped in ochre
        if nm in ("_a", "_b", "_c"): return antler_paint(n + nm, base)
        return {t: antler_paint(n + nm, base, dip=(t == 3)) for t in (1, 2, 3)}
    A = lambda nm, par, piv, size, base, rot: Bone(n + nm, par, piv, [Box((-size[0] / 2, -size[1], -size[2] / 2), size, paints(nm, base))], rot=rot)
    return [
        A("_a", "head", (c * 3.5, -3.5, -2.5), (2, 6, 2), 2, (-14, 0, c * 68)),        # the beam leaves the burr wide and low
        A("_b", n + "_a", (0, -6, 0), (2, 7, 2), 2, (-6, 0, -c * 20)),                  # sweeps out and back
        A("_c", n + "_b", (0, -7, 0), (2, 6, 2), 3, (4, 0, -c * 28)),                   # then climbs
        A("_d", n + "_c", (0, -6, 0), (1, 5, 1), 4, (0, 0, -c * 6)),                    # the crown's middle tip
        A("_brow", n + "_a", (0, -1.5, 0), (1, 6, 1), 3, (34, 0, -c * 62)),             # brow tine, forward over the eyes
        A("_bez", n + "_a", (0, -5, 0), (1, 6, 1), 3, (22, 0, -c * 66)),                # bez tine, forward and up
        A("_trez", n + "_b", (0, -2.5, 0), (1, 7, 1), 3, (6, 0, -c * 50)),              # a long tine rising off the outer beam
        A("_royal", n + "_b", (0, -5.5, 0), (1, 6, 1), 3, (12, 0, -c * 34)),            # a second, shorter
        A("_back", n + "_c", (0, -2, 0), (1, 4, 1), 3, (-42, 0, c * 10)),               # a back tine off the climb
        A("_inner", n + "_c", (0, -4, 0), (1, 5, 1), 4, (8, 0, -c * 44)),              # an inner tine toward the other side
        A("_crown1", n + "_c", (0, -6, 0), (1, 5, 1), 4, (0, 0, c * 40)),               # the crown forks outward
        A("_crown2", n + "_c", (0, -6, 0), (1, 4, 1), 4, (-12, 0, -c * 42))]            # and inward

def bones():
    """At most 64 bones (TroopModel.MAX_BONES): static parts ride on their parent's bone as extra boxes."""
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 11, 12), [
        Box((-7, -5, -12), (14, 11, 12), body_painter()),
        Box((-7, 3, -12), (14, 4, 12), pelt(R=BARK, seed="oh-fb"), dil=0.4),                              # the belly fringe
        # the back steps down from the hump to a lower rump: a ridge of shag, then the tufts along the spine
        Box((-6, -7, -12), (12, 2, 7), shag("oridge", under=0, wear=2)),
        Box((-1.5, -9, -11), (3, 2, 3), tuft()), Box((-1.5, -8, -7), (3, 2, 3), tuft()), Box((-1.5, -6.5, -3), (3, 2, 3), tuft())]))
    Y = -12.9
    B.append(Bone("chest", "body", (0, 0, -12), [
        Box((-7, -8, -10), (14, 14, 10), chest_painter()),
        # the mane: a heavy ruff round the neck, wider than the shoulders, and shag hanging off the shoulders
        Box((-8, -7, -11.5), (16, 12, 3), pelt(R=BARK, seed="oh-ruff"), dil=0.3),
        Box((-9, -1, -9), (2, 8, 7), pelt(R=BARK, seed="oh-shr"), dil=0.2), Box((7, -1, -9), (2, 8, 7), pelt(R=BARK, seed="oh-shl"), dil=0.2),
        Box((-7, 3, -10), (14, 4, 10), pelt(R=BARK, seed="oh-fr"), dil=0.4),
        # the hump rises well over the shoulders; the team hide mantle drapes over it, the bone plaques on top
        Box((-6, -12, -9.5), (12, 4, 9), shag("ohump", under=0, wear=2)),
        Box((-4, Y, -7.5), (2, 1, 4), plaque()), Box((-1, Y, -7.5), (2, 1, 4), plaque()), Box((2, Y, -7.5), (2, 1, 4), plaque()),
        Box((-2, Y, -7.5), (1, 1, 4), lash(), dil=0.05), Box((1, Y, -7.5), (1, 1, 4), lash(), dil=0.05),
        Box((-4, Y, -4), (8, 1, 1), lash(), dil=0.05)]))
    B.append(Bone("head", "chest", (0, 3, -9.5), [
        Box((-5, -4, -7), (10, 8, 7), head_painter()),
        Box((-3, -1, -11), (6, 4, 4), muzzle_painter()),
        Box((-3, 3, -10.5), (1, 2, 1), fang()), Box((2, 3, -10.5), (1, 2, 1), fang()),
        Box((-4, 4, -6), (8, 3, 5), beard(), dil=0.1),
        # cheek ruffs widen the head past the brow: the bear's jowls and mane
        Box((-7, -2, -5), (2, 6, 5), cheek("ckr"), dil=0.1), Box((5, -2, -5), (2, 6, 5), cheek("ckl"), dil=0.1),
        Box((2, -5, -4), (3, 2, 3), burr(), dil=0.2), Box((-5, -5, -4), (3, 2, 3), burr(), dil=0.2)], rot=(PITCH, 0, 0)))
    B.append(Bone("jaw", "head", (0, 3, -7), [Box((-2, 0, -4), (4, 1, 4), jaw_painter())]))
    B.append(Bone("ear_right", "head", (-5, -3.5, -1), [Box((-1, -2, -0.5), (2, 2, 1), ear_painter())], rot=(0, 0, -20)))
    B.append(Bone("ear_left", "head", (5, -3.5, -1), [Box((-1, -2, -0.5), (2, 2, 1), ear_painter())], rot=(0, 0, 20)))
    for side in ("right", "left"):
        B.extend(antler_chain(side))
    B.append(Bone("tail", "body", (0, -3, 0), [Box((-1.5, 0, 0), (3, 3, 2), tailp())]))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"front_{side}_leg", "chest", (c * 4.5, 3, -5), [
            Box((-3, 0, -3), (6, 8, 6), leg_painter("fl" + side)), Box((-3.5, 8, -4.5), (7, 2, 7), paw()),
            Box((-3, 5, -3), (6, 1, 6), wrist(), dil=0.3), Box((-3, 9, -5.5), (6, 1, 1), claws())]))
        B.append(Bone(f"back_{side}_leg", "root", (c * 4.5, 14, 8), [
            Box((-3, 0, -3), (6, 8, 6), leg_painter("bl" + side)), Box((-3.5, 8, -4.5), (7, 2, 7), paw()),
            Box((-3, 9, -5.5), (6, 1, 1), claws())]))
    B.append(Bone("mantle", "chest", team=True, boxes=[
        Box((-7, -11, -9.5), (14, 4, 9), "top", dil=0.3, sheet="cloth"),
        Box((4, -7, -8), (3, 5, 8), "drape", dil=0.2, sheet="cloth"),
        Box((-7, -7, -8), (3, 5, 8), "drape", dil=0.2, sheet="cloth")]))
    for side, c in (("right", -1), ("left", 1)):
        for i, (zq, ang, ln) in enumerate(((-6, 14, 7), (-4, 26, 8), (-2, 38, 7))):
            B.append(Bone(f"quill_{side}_{i}", "chest", (c * 6, -11, zq + 2.5), [Box((-0.5, 0, -1), (1, ln, 2), feather())], rot=(0, 0, -c * ang)))
    for i, x in enumerate((-3.5, -1.2, 1.2, 3.5)):
        B.append(Bone(f"quill_back_{i}", "chest", (x, -11.5, -1), [Box((-1, 0, -0.5), (2, 7, 1), feather())], rot=(28 + 6 * (i % 2), 0, 0)))
    # tier II: a bone brow plate, bone greaves on the forelegs, ochre bands with bone beads round the rack's beams
    B.append(Bone("brow_plate", "head", stages=[2, 3], boxes=[
        Box((-3.5, -5, -7.6), (7, 2, 1), per_tier(brow_plate, (2, 3))), Box((-1, -6, -7.2), (2, 1, 3), lash(), dil=0.05)]))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"{side}_greave", f"front_{side}_leg", stages=[2, 3], boxes=[
            Box((-2.5, 0.5, -3.6), (5, 4, 1), per_tier(greave, (2, 3)), dil=0.1)]))
        B.append(Bone(f"{side}_rack_band", f"antler_{side}_b", stages=[2, 3], boxes=[
            Box((-1, -2, -1), (2, 1, 2), band(), dil=0.3),
            Box((-0.5, -1, -1.6), (1, 1, 1), bead()), Box((-0.5, 0, -1.6), (1, 1, 1), bead())]))
    # tier II: bone pauldrons lashed over the mantle's drapes
    B.append(Bone("pauldrons", "chest", stages=[2, 3], boxes=[
        Box((7.4, -8.5, -9), (1, 6, 8), per_tier(pauldron, (2, 3)), dil=0.1), Box((-8.4, -8.5, -9), (1, 6, 8), per_tier(pauldron, (2, 3)), dil=0.1)]))
    # tier III: a crest of boar tusks rising out of the hump, trophy skulls hung from the pauldrons, tusks on the greaves
    B.append(Bone("hump_tusks", "chest", (0, -12.5, -5), rot=(-14, 0, 0), stages=[3], boxes=[
        Box((-0.5, -6, -3), (1, 6, 2), tusk()), Box((-3, -4, -2.5), (1, 4, 1), tusk()), Box((2, -4, -2.5), (1, 4, 1), tusk()),
        Box((-5, -3, -1), (1, 3, 1), tusk()), Box((4, -3, -1), (1, 3, 1), tusk()),
        Box((-0.5, -5, 0.5), (1, 5, 1), tusk()), Box((-3, -1, -3), (6, 1, 4), lash(), dil=0.05)]))
    B.append(Bone("trophies", "chest", stages=[3], boxes=[
        Box((8, -3, -7), (2, 2, 2), skull_t()), Box((-10, -3, -7), (2, 2, 2), skull_t()),
        Box((8.5, -4, -6.5), (1, 1, 1), lash()), Box((-9.5, -4, -6.5), (1, 1, 1), lash())]))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"{side}_greave_tusks", f"front_{side}_leg", stages=[3], boxes=[
            Box((-2, 0.5, -4.6), (1, 2, 1), tusk()), Box((1, 0.5, -4.6), (1, 2, 1), tusk())]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    return hide_mantle(T, M, "wm-oh-" + spec)

def glow(bone, box):
    """Clay-glow eyes and a few warpaint marks, 14 texels in all, on the head only."""
    if bone.name != "head" or box.size != [10, 8, 7]: return None
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
        "bear with a high hump and neck ruff under a 30 px antler rack of branching tines, a shaggy bark coat, a grey scarred muzzle, clay warpaint and clay-glow "
        "eyes, a bone-and-feather mantle bound in ochre over the shoulder hump on a team-colour hide mantle. No war-banner (an open question "
        "for Kort). Tier II: bone brow plate, pauldrons, greaves, rack bands; tier III: a tusk crest on the hump, skull trophies, greave tusks, ochre-dipped tines. No back flag.")
def veteran(bone, box):
    """The Mastery II trim: ochre cords at the antlers' bases, on each flank and on each upper leg, bone beads in them."""
    n, sz = bone.name, tuple(box.size)
    if n.startswith("antler_") and n.endswith("_a"): return vet_rows((1,))
    if n == "body" and sz == (14, 11, 12): return vet_rows((3,), every=2, bead=True)
    if n == "chest" and sz == (14, 14, 10): return vet_rows((9,), every=2, bead=True)
    if n.endswith("_leg") and sz in ((6, 8, 6),): return vet_rows((4,), bead=True)
    return None

WM_OLDHORN = Troop("wm_oldhorn", SKIN, CLOTH, bones, cloth_painter,
                   {"rig": "hound", "weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "beast 24"}, glow=glow, veteran=veteran)
def build(): return WM_OLDHORN.build()
def outputs(): return WM_OLDHORN.outputs()

if __name__ == "__main__":
    house_stub.main(WM_OLDHORN, NOTE, sys.argv)
