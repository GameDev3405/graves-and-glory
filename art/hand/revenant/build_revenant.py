#!/usr/bin/env python3
"""The Graveborn Risen (troop id revenant), hand-authored at one texel per model pixel.

A shambling dead man in what the grave left him: stooped (the torso leans 15 degrees), arms forward, a rusted coif framing a
grey-green face with sunk soul-lit eyes and a slack jaw hanging open, a rust-eaten hauberk torn open over bare ribs, a ragged
mail skirt in flaring strips, one great torn pauldron with a snapped bone shard, and over it all a shredded team tabard hanging
in strips (the mark: the stoop under the shredded tabard). Violet cord binds the brow, waist, wrists and ankles. He carries the
notched sword (art/hand/weapons/build_gb_notched_sword.py) in the right hand. No back flag.

Tier I is the Risen. Tier II (Revenant) wears salvaged grave-iron over the rags: a plate gorget, a plate over the right
epaulet, vambraces, tassets, knee cops and a heavy brow plate, all piped in violet. Tier III (Grave Knight) goes to black
plate chased in funeral gilt, with a slitted rusted visor over the face, a ridge along the coif, spiked pauldrons and a team
half-cape hanging from the left shoulder.

  python build_revenant.py preview <dir> [side] | contact <png> [side] [all] | stub
"""
import os, random, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from handlib import *   # noqa: F401,F403
import house_stub
from graveborn import GSKIN, RUST, MOSS, BONE, ROT, GIRON, VIOLET, SOUL, GILT, gb_plate, gb_spike, gb_band, gb_gilt

SKIN = (128, 128)
CLOTH = (128, 64)
HUNCH = 15

def rng(*k): return random.Random(repr(k))

def lit(face, x, w, base):
    """The base tone for a texel: face light plus the lit-edge shift on the side faces."""
    return base + LIT[face] + (lightx(face, x, w) if face not in ("top", "bottom") else 0)

def surf(R, base, seed, wear=0.0):
    """A worn flat surface: face light, a little noise, `wear` the share of darker pits."""
    def paint(face, w, h):
        r = rng(seed, face, w, h); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                v = lit(face, x, w, base) + r.choice((-1, 0, 0, 0, 1))
                if r.random() < wear: v -= 2
                g[y][x] = clamp(R, v)
        return g
    return paint

def rust_mail(base=3, seed="rm", moss=0.05, flake=0.06, patch=0.38):
    """Grave-iron mail eaten by rust: a ring lattice in dark iron, blooms of rust in 2x2 patches, flaked and pitted, a few moss stains."""
    def paint(face, w, h):
        r = rng(seed, face, w, h); g = blank(w, h)
        bloom = {(x // 2, y // 2): r.random() for x in range(w) for y in range(h)}
        for y in range(h):
            for x in range(w):
                v = lit(face, x, w, base + 1) + ((1 if x % 2 == 0 else 0) if y % 2 == 0 else (-1 if x % 2 else 0))
                if face not in ("top", "bottom") and y == 0: v += 1
                roll = r.random()
                if roll < flake: v += 1
                elif roll < flake + 0.10: v -= 2
                if bloom[(x // 2, y // 2)] < patch: g[y][x] = clamp(RUST, v - 1)
                else: g[y][x] = clamp(GIRON, v + 1)
                if r.random() < moss: g[y][x] = MOSS[1 + r.randrange(3)]
        return g
    return paint

def jag(g, bottom, seed, depth=2, p=0.5):
    """Cut a ragged lower edge: each column loses 0..depth rows from the bottom."""
    r = rng(seed); w = len(g[0]); h = len(g)
    for x in range(w):
        n = r.randrange(depth + 1) if r.random() < p else 0
        for y in range(h - n, h): g[y][x] = None
    return g

# ---- head ----
def cranium():
    """The sunk face under the coif: soul-socket pits (the glow sheet lights them), a flat nose, bared upper teeth."""
    G = GSKIN
    def paint(face, w, h):
        r = rng("cr", face); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(G, lit(face, x, w, 5) + r.choice((-1, 0, 0, 1)))
        if face == "front":
            for x in range(w): g[0][x] = g[1][x] = G[1]
            for x in (1, 2, 5, 6): g[2][x] = G[2]                       # the heavy brow
            g[2][3] = g[2][4] = G[5]
            g[3][1] = g[3][6] = G[3]
            g[3][2] = g[3][5] = G[0]                                    # the sunk sockets (the glow sheet lights them)
            g[2][2] = g[2][5] = G[1]; g[4][2] = g[4][5] = G[1]
            g[4][1] = g[4][6] = BONE[3]                                 # cheekbone showing through
            g[4][3] = g[4][4] = G[1]                                    # no nose, two slits
            g[5][1] = g[5][6] = G[2]
            for x in (2, 3, 4, 5): g[5][x] = BONE[4] if x % 2 == 0 else BONE[2]   # the upper teeth, bared
        elif face in ("left", "right"):
            for y in range(h):
                g[y][0 if face == "left" else w - 1] = G[2]
            g[3][w // 2] = g[3][w // 2 + 1] = BONE[2]                   # bone at the temple
            g[2][w // 2] = BONE[3]
        elif face == "back":
            for y in range(h): g[y][3] = G[1] if y % 2 else G[2]        # a stitched seam down the skull
        elif face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(G, 1 + (x + y) % 2)
        return g
    return paint

def jaw():
    """The slack lower jaw, dropped open: grey flesh with bone along the chin and the lower teeth on its top face."""
    G = GSKIN
    def paint(face, w, h):
        r = rng("jw", face); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(G, lit(face, x, w, 4) + r.choice((-1, 0, 0, 1)))
        if face == "top":
            for y in range(h):
                for x in range(w): g[y][x] = G[0] if (x + y) % 2 else G[1]   # the dark mouth
            for x in range(w):
                g[0][x] = BONE[3 if x % 2 else 4]                     # lower teeth along the front
        if face == "front":
            for x in range(w): g[h - 1][x] = BONE[2]                  # the chin bone
            g[h - 1][0] = g[h - 1][w - 1] = None
        return g
    return paint

def coif():
    """The rusted coif: mail over the skull, a violet cord tied across the brow, the face window cut open."""
    def paint(face, w, h):
        g = rust_mail(3, "cf", 0.06, 0.08)(face, w, h)
        if face in ("front", "back", "left", "right"):
            for x in range(w): g[1][x] = VIOLET[2 + (x % 2)]          # the brow cord
            for x in range(w):
                if face != "front" or x in (0, w - 1): g[h - 1][x] = clamp(RUST, 1 + x % 2)
        if face == "front":
            for y in range(2, h):
                for x in range(1, w - 1): g[y][x] = None               # the face window
        if face in ("left", "right"):
            for y in range(h - 2, h):
                for x in range(w):
                    if (x + y) % 3 == 0: g[y][x] = None                # ragged cheek-flaps
        if face == "back":
            jag(g, 0, "cfb", 2, 0.7)
        return g
    return paint

def glow_fn(bone, box):
    if bone.name == "head" and box.size == [8, 6, 8]:
        def p(face, w, h):
            g = blank(w, h)
            if face == "front": g[3][2] = g[3][5] = SOUL[1]
            return g
        return p
    return None


def visor():
    """The rusted visor: a plate over the lower face with vertical breathing slits, rust at its rim, the eyes left open above."""
    base = gb_plate(3, "vi", 0.2)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front":
            for y in range(1, h):
                for x in range(1, w - 1, 2): g[y][x] = GIRON[0]
        return g
    return paint

# ---- body ----
def torso():
    """A rust-eaten hauberk: torn open on the belly over bare ribs, the spine showing through the back, a violet rope belt."""
    G = GSKIN
    base = rust_mail(3, "to", 0.05, 0.07)
    def paint(face, w, h):
        g = base(face, w, h)
        if face in ("top", "bottom"): return g
        if face == "front":
            for y in range(6, 11):
                for x in (1, 2, 3):
                    g[y][x] = BONE[3 if y % 2 == 0 else 1] if y in (6, 8, 10) else G[0]   # ribs over dark flesh
            g[5][1] = g[5][3] = G[1]
        if face == "left":
            for y in range(2, 10):
                for x in (1, 2):
                    g[y][x] = BONE[2 + (y % 2)] if y % 2 == 0 else G[0]
        if face == "back":
            for y in range(2, 10):
                g[y][3] = BONE[3 if y % 2 else 2]; g[y][4] = G[0]                         # the spine
            for y in range(2, 10):
                g[y][2] = g[y][5] = G[1] if y % 3 else G[2]
        for y in (h - 2, h - 1):
            for x in range(w): g[y][x] = VIOLET[2 + ((x + y) % 2) + (1 if y == h - 2 and x % 2 == 0 else 0)]   # the belt rope
        return g
    return paint

def strip_mail(seed, depth=2):
    """One strip of the ragged hauberk skirt: rusted mail, a violet-bound tatter at the foot."""
    def paint(face, w, h):
        g = rust_mail(3, seed, 0.07, 0.05)(face, w, h)
        if face in ("top", "bottom"): return g
        for x in range(w):
            if h > 3: g[h - 2][x] = VIOLET[2 + x % 2] if face in ("front", "back") else g[h - 2][x]
        return jag(g, 0, seed, depth, 0.9)
    return paint

def pauldron_plate():
    """The torn plate pauldron on the left shoulder: pitted dark rust over grave-iron, a rolled edge, a gouged rivet."""
    def paint(face, w, h):
        g = surf(RUST, 2, "pp", 0.14)(face, w, h)
        r = rng("ppr", face)
        if face not in ("top", "bottom"):
            for x in range(w): g[0][x] = clamp(RUST, 3 + LIT[face] + 0)
            for x in range(w): g[h - 1][x] = RUST[1]
            if face == "front":
                for y in range(h): g[y][0] = RUST[3]
                g[1][w // 2] = RUST[5]
            for _ in range(max(1, w * h // 8)): g[r.randrange(h)][r.randrange(w)] = GIRON[2]
            if face in ("front", "back"):
                g[h - 1][r.randrange(w)] = None; g[h - 1][r.randrange(w)] = None            # a bitten lower edge
        else:
            for _ in range(w * h // 4): g[r.randrange(h)][r.randrange(w)] = RUST[3]
        return g
    return paint

def epaulet():
    def paint(face, w, h):
        g = rust_mail(3, "ep", 0.07, 0.07)(face, w, h)
        if face not in ("top", "bottom"): jag(g, 0, "ep" + face, 2, 0.8)
        return g
    return paint

def shard():
    """A snapped bone shard: dirty white, a darker root, a splintered tip."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = clamp(BONE, 4 - (y * 3) // max(1, h) + LIT[face] // 2 + (1 if face == "top" else 0))
        if face not in ("top", "bottom") and h > 2: g[h - 1][0] = BONE[1]
        return g
    return paint

def arm():
    """A rusted mail sleeve ending jagged at the elbow, then bare grey-green forearm with the bone showing along it."""
    sleeve = rust_mail(3, "as", 0.06, 0.06)
    def paint(face, w, h):
        if face == "top": return sleeve(face, w, h)
        r = rng("ar", face, w); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(GSKIN, lit(face, x, w, 3) + r.choice((-1, 0, 0, 1)))
        if face == "bottom": return g
        s = sleeve(face, w, h)
        edge = [5 + r.randrange(3) for _ in range(w)]                   # a ragged sleeve end
        for y in range(h):
            for x in range(w):
                if y < edge[x]: g[y][x] = s[y][x]
        for y in range(edge[0] + 2, h - 2):                              # the radius, bare through the flesh
            xx = 1 if face in ("front", "right") else w - 2
            if face != "back": g[y][xx] = BONE[2 + (y % 2)] if y % 3 else BONE[4]
        for x in range(w): g[h - 1][x] = GSKIN[1]; g[h - 2][x] = clamp(GSKIN, lit(face, x, w, 2))
        return g
    return paint

def wrist():
    """A rag binding round the wrist: dark rot cloth, a violet cord, a trailing tatter."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(ROT, lit(face, x, w, 3)) if y < h - 1 else clamp(VIOLET, 2 + (x % 2))
        if face in ("front", "right"): g[0][1] = VIOLET[3]
        return g
    return paint

def leg():
    """Rotted moss-rag breeches ragged at the knee, then the bare shin, the bone showing through."""
    cloth = surf(MOSS, 2, "lg", 0.15)
    def paint(face, w, h):
        if face == "top": return cloth(face, w, h)
        r = rng("lg", face); g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(GSKIN, lit(face, x, w, 2) + r.choice((-1, 0, 0, 1)))
        if face == "bottom": return g
        c = cloth(face, w, h)
        edge = [5 + r.randrange(4) for _ in range(w)]
        for y in range(h):
            for x in range(w):
                if y < edge[x]: g[y][x] = c[y][x]
        if face == "front":
            for y in range(edge[1] + 1, h - 3): g[y][2] = BONE[3 if y % 2 else 2]      # the shin bone
        for x in range(w):
            if r.random() < 0.5: g[edge[x] - 1][x] = ROT[1]                             # a frayed seam
        return g
    return paint

def boot():
    """A rotted grave-boot: split dark leather bound with violet cord across the instep."""
    def paint(face, w, h):
        g = surf(ROT, 3, "bt", 0.12)(face, w, h)
        if face not in ("top", "bottom"):
            for x in range(w): g[0][x] = VIOLET[2 + x % 2]
            for x in range(w): g[h - 1][x] = ROT[0]
        return g
    return paint

def chain():
    return surf(GIRON, 4, "ch", 0.2)

# ---- the team tabard, in shreds (cloth sheet) ----
def shred(T, seed, taper):
    """A tabard strip: a dingy team field, a fold line, holes, a fraying vertical edge and a diagonal-cut foot."""
    def paint(face, w, h):
        g = blank(w, h)
        if face not in ("front", "back"): return g
        r = rng("td", seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 2 + LIT[face] + (1 if y < 1 else 0) + (1 if (x + y // 3) % 3 == 0 else 0) - (1 if x == w - 1 else 0)
                g[y][x] = clamp(T, v)
                if y > 3 and r.random() < 0.05: g[y][x] = None
        for x in range(w):                                              # the diagonal-cut, uneven foot
            cut = (x * taper) % 3 + (1 if x == w - 1 else 0)
            for y in range(h - cut, h): g[y][x] = None
        for y in range(h):
            if r.random() < 0.3: g[y][0 if r.random() < 0.5 else w - 1] = None
        return g
    return paint

def cloth_painter(spec, side):
    T, M = TEAM[side]
    kind, seed, taper = spec
    return shred(T, seed, taper)

# ---- assembling ----
FRONT_STRIPS = [(-4, 2, 9, -8), (-1, 2, 13, -4), (2, 2, 7, -8)]    # x0, w, length, rotation x

def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), torso())], rot=(HUNCH, 0, 0)))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 6, 8), cranium())], rot=(-6, 0, 0)))
    B.append(Bone("jaw", "head", (0, -2, 2), [Box((-3, 0, -5.5), (6, 3, 6), jaw())], rot=(24, 0, 0)))
    B.append(Bone("coif", "head", tag="helm", boxes=[Box((-4, -8, -4), (8, 6, 8), coif(), dil=0.6)]))
    for side, x, ox in (("right", -5, -3), ("left", 5, -1)):   # jointed: elbow and hand as handlib.arms()
        B += limb(f"{side}_arm", "body", (x, -10, 0), (ox, -2, -2), arm(), f"{side}_forearm", ELBOW[side])
        B.append(Bone(f"{side}_hand", f"{side}_forearm", HAND))
    B.append(Bone("arms_folded", "body", (0, -9, -1), rot=(-43, 0, 0)))
    for side, x in (("right", -1.9), ("left", 1.9)):   # jointed: the knee 6 px down the leg
        B += limb(f"{side}_leg", "root", (x, 12, 0), (-2, 0, -2), leg(), f"{side}_shin", KNEE)
    # the ragged hauberk skirt: strips of mail flaring from the waist, each its own length
    skirt = [("sk_f0", (-4.5, -2.6), 3, 6, -9), ("sk_f1", (-1.5, -2.6), 3, 4, -6), ("sk_f2", (1.5, -2.6), 3, 7, -9),
             ("sk_b0", (-4.5, 1.6), 3, 5, 9), ("sk_b1", (-1.5, 1.6), 3, 7, 6), ("sk_b2", (1.5, 1.6), 3, 4, 9)]
    for name, (x, z), w, ln, rx in skirt:
        B.append(Bone(name, "body", (0, -2, 0), rot=(rx, 0, 0), boxes=[Box((x, 0, z), (w, ln, 1), strip_mail(name))]))
    B.append(Bone("sk_r", "body", (-4.5, -2, 0), rot=(0, 0, 9), boxes=[Box((-0.5, 0, -2), (1, 5, 4), strip_mail("skr"))]))
    B.append(Bone("sk_l", "body", (4.5, -2, 0), rot=(0, 0, -9), boxes=[Box((-0.5, 0, -2), (1, 6, 4), strip_mail("skl"))]))
    # shoulders: a torn mail epaulet on the right, the great gouged plate on the left with a snapped bone shard
    B.append(Bone("right_epaulet", "right_arm", (-1, -2.5, 0), rot=(0, 0, 8), boxes=[Box((-3, -1, -3), (6, 3, 6), epaulet(), dil=0.1)]))
    B.append(Bone("left_pauldron", "left_arm", (1, -2.5, 0), rot=(0, 0, -12), boxes=[Box((-3.5, -2, -3.5), (7, 3, 7), pauldron_plate(), dil=0.2)]))
    B.append(Bone("shard", "left_pauldron", (1, -1, 1), rot=(0, 0, 18), boxes=[Box((-0.5, -5, -0.5), (1, 5, 1), shard()), Box((0.5, -3, -0.5), (1, 2, 1), shard())]))
    # bindings: wrists and ankles, a broken shackle chain on the left wrist
    for side in ("right", "left"):
        ox = -1 if side == "right" else -2   # the forearm's frame: the arm frame less the elbow
        B.append(Bone(f"{side}_wrist", f"{side}_forearm", boxes=[Box((ox, 2, -2), (4, 3, 4), wrist(), dil=0.3)]))
        B.append(Bone(f"{side}_boot", f"{side}_shin", boxes=[Box((-2, 2.5, -3), (4, 3.5, 5), boot(), dil=0.3)]))
    B.append(Bone("chain", "left_forearm", (0, 4, 0), rot=(0, 0, -6), boxes=[Box((-0.5, 0, -0.5), (1, 4, 1), chain())]))
    # the tabard: a chest panel and strips front and back, each at its own length and lean
    B.append(Bone("tabard_chest", "body", (0, -10, 0), team=True, boxes=[
        Box((-3, 0, -2.95), (6, 3, 0), ("t", "chest", 1), sheet="cloth"), Box((-3, 0, 2.95), (6, 4, 0), ("t", "chestb", 2), sheet="cloth")]))
    for i, (x0, w, ln, rx) in enumerate(FRONT_STRIPS):
        B.append(Bone(f"tabard_f{i}", "body", (0, -7.5, 0), team=True, rot=(rx, 0, 0), boxes=[
            Box((x0, 0, -3.0 - 0.1 * i), (w, ln, 0), ("t", "f%d" % i, i + 1), sheet="cloth")]))
    for i, (x0, w, ln, rx) in enumerate([(-3, 2, 9, 7), (0, 2, 12, 5), (-1, 1, 6, 8)]):
        B.append(Bone(f"tabard_b{i}", "body", (0, -7, 0), team=True, rot=(rx, 0, 0), boxes=[
            Box((x0, 0, 3.0 + 0.1 * i), (w, ln, 0), ("t", "b%d" % i, i + 2), sheet="cloth")]))
    # tier II, the Revenant: salvaged grave-iron plate over the rags, piped in violet
    B.append(Bone("plate_gorget", "body", stages=[2, 3], boxes=[Box((-4.5, -13, -2.7), (9, 3, 5.4), per_tier(gb_plate, (2, 3)), dil=0.1)]))
    B.append(Bone("brow_plate", "head", stages=[2, 3], boxes=[Box((-4, -6.6, -4.9), (8, 1.4, 1), per_tier(gb_plate, (2, 3)), dil=0.1)]))
    B.append(Bone("right_plate", "right_arm", (-1, -2.5, 0), rot=(0, 0, 8), stages=[2, 3], boxes=[
        Box((-2.5, -2.4, -2.5), (5, 2, 5), per_tier(lambda t: gb_plate(t, "rpl"), (2, 3)), dil=0.35),
        Box((-0.5, -5.4, -0.5), (1, 3, 1), per_tier(lambda t: gb_spike(GIRON, 3, 3), (2, 3)))]))
    for side, ox in (("right", -2.5), ("left", -0.5)):
        B.append(Bone(f"{side}_vambrace", f"{side}_forearm", stages=[2, 3], boxes=[
            Box((ox - 0.5 - ELBOW[side][0], -1, -2), (5, 3, 4), per_tier(lambda t, s=side: gb_plate(t, "vb" + s, 0.18), (2, 3)), dil=0.35)]))
        B.append(Bone(f"{side}_knee_cop", f"{side}_shin", stages=[2, 3], boxes=[
            Box((-2, -1.6, -3.4), (4, 3, 1.5), per_tier(lambda t, s=side: gb_plate(t, "kc" + s), (2, 3)), dil=0.1)]))
    B.append(Bone("tasset_r", "body", (-4.5, -2, 0), rot=(0, 0, 6), stages=[2, 3], boxes=[
        Box((-1.6, 0, -2.4), (1, 6, 4.8), per_tier(lambda t: gb_plate(t, "tr"), (2, 3)), dil=0.1)]))
    B.append(Bone("tasset_l", "body", (4.5, -2, 0), rot=(0, 0, -6), stages=[2, 3], boxes=[
        Box((0.6, 0, -2.4), (1, 7, 4.8), per_tier(lambda t: gb_plate(t, "tl"), (2, 3)), dil=0.1)]))
    # pushed further so each tier reads at game distance: a breastplate round the ribs, a violet belt, cuisses, cheek guards,
    # a crest fin and a spiked cap on each pauldron; tier III adds gilt collar, buckle and rim bands
    P2 = lambda seed, worn=0.12: per_tier(lambda t: gb_plate(t, seed, worn), (2, 3))
    # one bone each for the breastplate and the helm plates: the jointed limbs took four bones and a model holds 64 (TroopModel.MAX_BONES)
    B.append(Bone("breastplate", "body", stages=[2, 3], boxes=[
        Box((-4, -10.5, -2.4), (3.3, 6, 0.8), P2("bl"), dil=0.25), Box((0.7, -10.5, -2.4), (3.3, 6, 0.8), P2("br"), dil=0.25),
        Box((-4, -4.4, -2.3), (8, 1.4, 4.6), gb_band(VIOLET, 2), dil=0.3)]))
    B.append(Bone("helm_plates", "head", stages=[2, 3], boxes=[Box((-0.5, -10.6, -3.5), (1, 3, 7), P2("cf"), dil=0.05)]
        + [Box((ox, -5.2, -3.2), (1, 3.4, 5), P2("ck" + side), dil=0.15) for side, ox in (("r", -5.2), ("l", 4.2))]))
    for side in ("right", "left"):
        B.append(Bone(f"{side}_cuisse", f"{side}_leg", stages=[2, 3], boxes=[Box((-2, 0.8, -2), (4, 4, 4), P2("cs" + side), dil=0.4)]))
    B.append(Bone("left_cap2", "left_arm", (1, -2.5, 0), rot=(0, 0, -12), stages=[2], boxes=[
        Box((-3.5, -3.2, -3.5), (7, 2, 7), P2("lc2"), dil=0.3), Box((0, -7, 0), (1, 4, 1), per_tier(lambda t: gb_spike(GIRON, 3, 3), (2, 3)))]))
    B.append(Bone("right_spike2", "right_arm", (-1, -2.5, 0), rot=(0, 0, 8), stages=[2, 3], boxes=[
        Box((-3.2, -4.6, -1), (1, 3, 1), per_tier(lambda t: gb_spike(GIRON, 3, 3), (2, 3))), Box((1.2, -4.6, -1), (1, 3, 1), per_tier(lambda t: gb_spike(GIRON, 3, 3), (2, 3)))]))
    B.append(Bone("gilt_collar", "body", stages=[3], boxes=[Box((-4.5, -13.5, -2.8), (9, 1, 5.6), gb_gilt("gc"), dil=0.1)]))
    B.append(Bone("gilt_buckle", "body", stages=[3], boxes=[Box((-1, -4.6, -2.9), (2, 2, 0.8), gb_gilt("gk"), dil=0.1)]))
    B.append(Bone("gilt_rim_r", "right_arm", (-1, -2.5, 0), rot=(0, 0, 8), stages=[3], boxes=[Box((-3, -1, -3), (6, 1, 6), gb_gilt("grr"), dil=0.5)]))
    B.append(Bone("gilt_rim_l", "left_arm", (1, -2.5, 0), rot=(0, 0, -12), stages=[3], boxes=[Box((-3, -0.6, -3), (6, 1, 6), gb_gilt("grl"), dil=0.45)]))
    # tier III, the Grave Knight: a slitted rusted visor, a coif ridge, a plate over the left pauldron, a team half-cape
    B.append(Bone("visor", "head", stages=[3], boxes=[Box((-4, -3.9, -4.9), (8, 2, 1), visor(), dil=0.1)]))
    B.append(Bone("pauldron_cap", "left_arm", (1, -2.5, 0), rot=(0, 0, -12), stages=[3], boxes=[
        Box((-3, -3.4, -3), (6, 2, 6), gb_plate(3, "pc"), dil=0.3),
        Box((0.5, -7.4, 0), (1, 4, 1), gb_spike(GIRON, 3, 3)), Box((-2.5, -5.4, -1), (1, 2, 1), gb_spike(GIRON, 3, 3))]))
    B.append(Bone("half_cape", "body", (3.2, -11.2, 2.3), rot=(6, 0, -3), stages=[3], team=True, boxes=[
        Box((-3.2, 0, 0.5), (7, 18, 0), ("t", "hc0", 1), sheet="cloth")]))
    B.append(Bone("cape_clasp", "body", stages=[3], boxes=[Box((2.2, -12.2, 1.7), (2, 2, 1), gb_gilt("cc"))]))
    return B

NOTE = ("Graveborn Risen (unit identity, Graveborn): a stooped, shambling dead man, torso leaning 15 degrees and arms forward: a rusted "
        "coif with a violet brow cord over a grey-green face with soul-lit sunk eyes and a slack jaw, a rust-eaten hauberk torn open over "
        "bare ribs, a ragged mail skirt in strips, a torn plate pauldron with a snapped bone shard, violet-bound wrists and ankles and a "
        "shredded team tabard hanging in strips. He carries the notched sword in the right hand. Tier II: salvaged plate with violet piping; "
        "tier III: black gilt-chased plate, a slitted visor and a team half-cape. No back flag.")
REVENANT = Troop("revenant", SKIN, CLOTH, bones, cloth_painter,
                 {"weapon": "notched_sword", "hand": "right", "idleArms": "forward", "portrait": "standard"}, glow=glow_fn,
                 merge={"right_boot": "right_foot", "left_boot": "left_foot", "brow_plate": "helm_plates", "cape_clasp": "gilt_collar"})
def build(): return REVENANT.build()
def outputs(): return REVENANT.outputs()

if __name__ == "__main__":
    house_stub.main(REVENANT, NOTE, sys.argv)
