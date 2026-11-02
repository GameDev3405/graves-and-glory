#!/usr/bin/env python3
"""The Moot Hound (Wildmoot house, troop id wm_hound; Common; a wolf entity), hand-authored at one texel per model pixel.

A low, long, head-down cur of the moot: a russet fur coat over a bark-dark belly, a dark muzzle with small bone teeth,
and a pale clay stripe run from the nose along the spine to the tail tip. An ochre-dyed sinew collar with bone toggles
sits at the neck; a ragged team saddle-cloth drapes both shoulders and the withers, split along the spine so the clay
stripe stays whole, with hide spikes rising through it. Hide, bone and clay throughout; the team colour is only the
cloth. No back flag.

Tier I is the moot cur. Tier II (Dire Hound) lashes a bone skull-cap over the brow, hangs a tooth collar under the jaw,
wraps the forelegs in ochre sinew and raises taller spikes. Tier III (Packleader) grows antler tufts from the head,
lays bone plates down the spine and paints ochre bars across the flanks.

  python build_wm_hound.py preview <dir> [side] | contact <png> [side] | stub
"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "common"))
from wildmoot import *   # noqa: F401,F403
import house_stub

SKIN = (128, 64)
CLOTH = (128, 64)
PITCH = 11   # degrees the head is carried down (positive x lowers the nose)

SPIKE_R = HORN[2:] + BONE[1:5]   # a hide spike: dark horn at the root to pale bone at the tip

def hcoat(seed, R=FUR, stripe=None, stripe_faces=("top",), stripe_w=2, under=2, wear=2, lift=1):
    """A russet fur coat in broken strands, bark-dark along the belly and underside, with an optional clay stripe on the top."""
    def paint(face, w, h):
        g = blank(w, h); r = rng(seed, face, w, h)
        for y in range(h):
            for x in range(w):
                v = 3 + lift + LIT[face] + r.choice((-1, 0, 0, 0, 0, 0, 1))
                if (x * 3 + y // 3) % 7 == 0 and y % 3 != 2: v -= 1
                g[y][x] = clamp(R, v)
        if face not in ("top", "bottom") and h > 3:
            for _ in range(max(1, w * h // 14)):                                      # pale strand tufts
                x, y = r.randrange(w), r.randrange(h - 2)
                dk = r.random() < 0.6                                                    # mostly dark strands, a few pale
                g[y][x] = clamp(R, 2 + LIT[face] if dk else 5); g[y + 1][x] = clamp(R, 2 + LIT[face] if dk else 4)
        if stripe and face in stripe_faces:
            c0 = (w - stripe_w) // 2
            for y in range(h):
                for x in range(c0, c0 + stripe_w): g[y][x] = clamp(stripe, 3 + (1 if (y % 3 == 0) else 0))
        if face == "bottom":
            for y in range(h):
                for x in range(w): g[y][x] = clamp(BARK, 1 + (x + y) % 2)
        elif face != "top" and under > 0 and h > under + 1:
            r = rng(seed, "under", face, w, h)
            for x in range(w):
                d = under + (1 if r.random() < 0.4 else 0)
                for y in range(h - d, h): g[y][x] = clamp(BARK, 2 + LIT[face] + (1 if y == h - d else 0))
        return scuff(g, (seed, "w", face, w, h), wear, R)
    return paint

def head_painter():
    base = hcoat("hh", stripe=CLAY, under=1)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "front":
            for x in (1, 2): g[0][x] = FUR[0]                                       # a low, angry brow
            for x in (3, 4): g[0][x] = FUR[0]
            g[1][1] = OCHRE[4]; g[1][4] = OCHRE[4]                                   # small amber eyes
            g[1][2] = FUR[1]; g[1][3] = FUR[1]
            for x in (0, 5): g[2][x] = BARK[2]; g[3][x] = BARK[2]
        if face in ("left", "right") and h >= 4 and w >= 3:
            ex = 1 if face == "left" else w - 2
            g[1][ex] = OCHRE[3]                                                      # the eye seen from the side
        return g
    return paint

def muzzle_painter():
    """A dark muzzle: sooty snout, a nose pad, a pale clay stripe up the bridge."""
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                g[y][x] = clamp(SNOUT, 1 + LIT[face] + (1 if (face in ("left", "right") and y == 0) else 0) + (x * 3 + y) % 2 * 0)
        if face == "front":
            for x in range(w): g[0][x] = HORN[2]
            g[0][0] = g[0][w - 1] = HORN[0]                                          # the nostrils
            for x in range(w): g[h - 1][x] = SNOUT[0]
        if face == "top":
            c0 = (w - 2) // 2
            for y in range(h):
                for x in range(c0, c0 + 2): g[y][x] = CLAY[3 if y % 3 else 4]       # the clay stripe runs to the nose
        if face in ("left", "right") and w >= 3:
            g[1][1] = SNOUT[3]; g[1][w - 2] = SNOUT[3]                               # whisker spots
        return g
    return paint

def jaw_painter():
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w): g[y][x] = clamp(SNOUT, 1 + LIT[face])
        if face == "front":
            for x in range(w): g[0][x] = BONE[3]                                     # the lower teeth
        if face == "top":
            for y in range(h):
                for x in range(w):
                    g[y][x] = BONE[2] if (x + y) % 2 == 0 else SNOUT[0]
        return g
    return paint

def ear_painter(notch=False):
    def paint(face, w, h):
        g = flat(FUR, 2, "ear", 1)(face, w, h)
        if face in ("front", "back"):
            for y in range(1, h):
                for x in range(w): g[y][x] = BARK[1 if face == "front" else 2]
            for x in range(w): g[0][x] = FUR[2]
            if notch and face == "front": g[0][w - 1] = None                          # a torn tip
        return g
    return paint

def fang():
    return bone_paint(5)

def collar():
    def paint(face, w, h):
        g = lashing(OCHRE, 3)(face, w, h)
        if face not in ("top", "bottom"):
            for x in range(w): g[h - 1][x] = clamp(OCHRE, 1)
            for y in range(h):
                if y % 3 == 1: g[y][0] = clamp(OCHRE, 4)
        return scuff(g, ("col", face, w, h), 3, OCHRE)
    return paint

def toggle():
    return bone_paint(4, "tg")

def spike():
    """A hide spike: dark horn at the root, pale bone at the tip."""
    rampS = [HORN[2], HORN[3], HORN[4], BONE[2], BONE[3], BONE[4]]
    def paint(face, w, h):
        g = blank(w, h)
        for y in range(h):
            for x in range(w):
                i = max(0, len(rampS) - 1 - y) if face not in ("top", "bottom") else (len(rampS) - 1 if face == "top" else 0)
                if face in ("left", "back") and i > 0: i -= 1
                g[y][x] = rampS[i]
        return g
    return paint

def paw():
    def paint(face, w, h):
        g = hcoat("paw", R=FUR, under=0, wear=1)(face, w, h)
        for y in range(h):
            for x in range(w):
                if face != "top": g[y][x] = clamp(BARK, 2 + LIT[face])
        if face == "front":
            for x in range(w): g[h - 1][x] = BONE[3] if x % 2 == 0 else BARK[1]       # pale claws
        return g
    return paint

def tail_painter():
    base = hcoat("tail", stripe=CLAY, stripe_faces=("back",), stripe_w=1, under=1)
    def paint(face, w, h):
        g = base(face, w, h)
        if face == "bottom":
            for x in range(w): g[0][x] = CLAY[3]
        return g
    return paint

def warpaint(base, tier):
    """At III the Packleader wears ochre bars across the flanks, over the coat."""
    def paint(face, w, h):
        g = base(face, w, h)
        if tier == 3 and face in ("left", "right") and h >= 4:
            for x in range(1, w - 1, 4):
                for y in range(1, h - 2):
                    for xx in (x, x + 1): g[y][xx] = OCHRE[4] if y % 2 else OCHRE[3]
        return g
    return paint

def wrap():
    """An ochre sinew leg wrap, a bone toggle on the front."""
    return wm_band()

# ---- cloth ----
def saddle(T, M):
    """Team saddle-cloth: a woven barry field, a darker fold above the hem, a ragged lower edge."""
    def paint(face, w, h):
        g = blank(w, h)
        r = rng("wm-hound-saddle", face, w, h)
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
                if q < 0.4: g[h - 1][x] = None
                elif q < 0.7: g[h - 1][x] = clamp(T, 0)
        return g
    return paint

# ---- assembling ----
def bones():
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 15, 8), [Box((-3, -3, -8), (6, 6, 10), {t: warpaint(hcoat("hb", stripe=CLAY), t) for t in (1, 2, 3)})]))
    B.append(Bone("chest", "body", (0, 0, -8), [Box((-4, -5, -7), (8, 8, 7), hcoat("hc", stripe=CLAY))]))
    B.append(Bone("head", "chest", (0, 1, -6.5), [Box((-3, -3, -5), (6, 5, 5), head_painter())], rot=(PITCH, 0, 0)))
    B.append(Bone("muzzle", "head", boxes=[Box((-2, -1, -9), (4, 2, 4), muzzle_painter())]))
    B.append(Bone("jaw", "head", (0, 1, -5), [Box((-1, 0, -4), (2, 1, 4), jaw_painter())]))
    B.append(Bone("fangs", "head", boxes=[Box((-2, 1, -8.5), (1, 1, 1), fang()), Box((1, 1, -8.5), (1, 1, 1), fang())]))
    B.append(Bone("ear_right", "head", (-2, -3, -1.5), [Box((-1, -3, -0.5), (2, 3, 1), ear_painter(False))], rot=(0, 0, -16)))
    B.append(Bone("ear_left", "head", (2, -3, -1.5), [Box((-1, -3, -0.5), (2, 3, 1), ear_painter(True))], rot=(0, 0, 16)))
    B.append(Bone("tail", "body", (0, -2, 2), [Box((-1.5, 0, -1.5), (3, 8, 3), tail_painter())], rot=(38, 0, 0)))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"front_{side}_leg", "chest", (c * 2.5, 2, -3.5), [
            Box((-1.5, 0, -1.5), (3, 3, 3), hcoat("fs" + side, under=0, wear=1)),
            Box((-1, 3, -1), (2, 3, 2), hcoat("fl" + side, R=FUR, under=0, wear=1)),
            Box((-1.5, 6, -2.5), (3, 1, 4), paw())]))
        B.append(Bone(f"back_{side}_leg", "root", (c * 2, 17, 8), [
            Box((-1.5, 0, -2), (3, 3, 4), hcoat("bh" + side, under=1, wear=1)),
            Box((-1, 3, -1), (2, 3, 2), hcoat("bl" + side, R=FUR, under=0, wear=1)),
            Box((-1.5, 6, -2.5), (3, 1, 4), paw())]))
    # the collar: ochre sinew about the neck, bone toggles through it
    B.append(Bone("collar", "head", boxes=[Box((-3, -3, -2), (6, 5, 2), collar(), dil=0.5)]))
    B.append(Bone("toggles", "head", boxes=[
        Box((-4.5, -1.5, -1.5), (1, 2, 1), toggle()), Box((3.5, -1.5, -1.5), (1, 2, 1), toggle()),
        Box((-0.5, -4.5, -1.5), (1, 1, 1), toggle())]))
    # hide spikes rising along the spine and through the cloth
    B.append(Bone("spikes", "chest", boxes=[
        Box((-0.5, -8.5, -3), (1, 4, 1), spike()), Box((2, -7.5, -2), (1, 3, 1), spike()), Box((-3, -7.5, -2), (1, 3, 1), spike())]))
    B.append(Bone("back_spikes", "body", boxes=[Box((-0.5, -6.5, 2.5), (1, 4, 1), spike())]))
    # the saddle-cloth: a flap over each shoulder and the withers, a gap along the spine for the stripe
    B.append(Bone("saddle", "chest", team=True, boxes=[
        Box((1, -5, -4), (3, 4, 4), "sad", dil=0.3, sheet="cloth"),
        Box((-4, -5, -4), (3, 4, 4), "sad", dil=0.3, sheet="cloth")]))
    B.append(Bone("saddle_rump", "body", team=True, boxes=[
        Box((1, -3, 0), (2, 3, 3), "sadr", dil=0.3, sheet="cloth"),
        Box((-3, -3, 0), (2, 3, 3), "sadr", dil=0.3, sheet="cloth")]))
    # tier II, the Dire Hound: a bone skull-cap lashed over the brow, ochre wraps on the forelegs, a tooth collar, taller spikes
    B.append(Bone("skullcap", "head", stages=[2, 3], boxes=[
        Box((-2.5, -4, -4.5), (5, 1, 4), per_tier(wm_plate, (2, 3))), Box((-0.5, -4.5, -5.2), (1, 1, 2), wm_tusk())]))
    B.append(Bone("tooth_collar", "head", stages=[2, 3], boxes=[
        Box((-3, 2, -2.4), (6, 1, 1), lashing(OCHRE, 2), dil=0.05),
        Box((-2.5, 3, -2.4), (1, 2, 1), wm_tusk()), Box((-0.5, 3, -2.6), (1, 2, 1), wm_tusk()), Box((1.5, 3, -2.4), (1, 2, 1), wm_tusk())]))
    B.append(Bone("spikes_ii", "chest", stages=[2, 3], boxes=[
        Box((-0.5, -10.5, -6), (1, 6, 1), spike()), Box((-2, -8.5, -5), (1, 4, 1), spike()), Box((1, -8.5, -5), (1, 4, 1), spike())]))
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"{side}_wrap", f"front_{side}_leg", stages=[2, 3], boxes=[Box((-1, 3.5, -1), (2, 1, 2), wrap(), dil=0.3)]))
    # tier III, the Packleader: antler tufts on the head, bone plates down the spine, ochre bars on the flanks
    for side, c in (("right", -1), ("left", 1)):
        B.append(Bone(f"antler_tuft_{side}", "head", (c * 2, -3.5, -3), stages=[3], rot=(-25, 0, -c * 22), boxes=[
            Box((-0.5, -6, -0.5), (1, 6, 1), antler(4, "hat" + side)), Box((-0.5 + c * 1, -5, -0.5), (1, 1, 1), antler(4, "hat2" + side)),
            Box((-0.5, -3, -1.5), (1, 1, 1), antler(4, "hat3" + side)), Box((-0.5 + c * 1, -6, -0.5), (1, 1, 1), antler(4, "hat4" + side))]))
    B.append(Bone("spine_plates", "body", stages=[3], boxes=[
        Box((-1.5, -4, -7), (3, 1, 3), wm_plate(3, "hsp1")), Box((-1.5, -4, -3), (3, 1, 3), wm_plate(3, "hsp2")),
        Box((-1, -3.8, 0.5), (2, 1, 2), wm_plate(3, "hsp3"))]))
    return B

def cloth_painter(spec, side):
    T, M = TEAM[side]
    return saddle(T, M)

NOTE = ("Wildmoot Moot Hound (unit identity, Wildmoot; Common; wolf entity): a low, long, head-down cur on the hound rig, a russet fur "
        "coat over a bark-dark belly, a dark muzzle with small bone teeth, a pale clay stripe from nose to tail tip, an ochre sinew "
        "collar with bone toggles and a ragged team saddle-cloth over the shoulders with hide spikes rising through it. Tier II: bone skull-cap, tooth collar, leg wraps, taller "
        "spikes; tier III: antler tufts, spine plates, ochre flank bars. No back flag.")
def veteran(bone, box):
    """The Mastery II trim: ochre cords about the shoulders and each upper leg, and two rings on the tail."""
    n, sz = bone.name, tuple(box.size)
    if n == "chest" and sz == (8, 8, 7): return vet_rows((5,), every=1, bead=True)
    if n.endswith("_leg") and sz in ((3, 3, 3), (3, 3, 4)): return vet_rows((1,))
    if n == "tail": return vet_rows((3, 6))
    return None

WM_HOUND = Troop("wm_hound", SKIN, CLOTH, bones, cloth_painter,
                 {"rig": "hound", "weapon": "none", "hand": "right", "idleArms": "hang", "portrait": "beast 16"}, veteran=veteran)
def build(): return WM_HOUND.build()
def outputs(): return WM_HOUND.outputs()

if __name__ == "__main__":
    house_stub.main(WM_HOUND, NOTE, sys.argv)
