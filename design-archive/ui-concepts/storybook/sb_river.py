"""RETIRED (Kort, 2026-10-18): every scene image in the storybook is in-game footage (stills/, see NOTES.md),
and no page uses what this file draws; its old outputs are in art/retired/. Kept for the record only.

The river road, built from blocks (sb_world.py draws it): a river one block below the meadow running across
the view, three stone-brick crossings with lantern posts, oaks and a ridge on the far bank, the company's camp
on the near bank. Two lights over one world:

dusk  c1_b1_after, "The North Crossing": the fire at dusk by the north crossing. Maud sits on a log by the fire
      with her helm off beside her and the turncoat's pact-copy in her hands; two squires eat at the fire; the
      Hollow's standard stands planted behind her (its cloth carries the ring, the hollow the company is named
      for). The sun has gone down behind the far trees on the left; the sky runs ember to violet; the river holds
      it; the bridge lanterns are lit and the fire lights the faces. The in-game stage frame under 04's card.
day   c1_b1_before, "Order on the Crossings": the same river by day from the south bank; the turncoats' white
      and gilt banners on all three crossings, their squires holding the bridges; Maud and the commander on the
      near bank. Behind 05's march card, and the read scene's thumbnail on 02.

python sb_river.py dusk|day|thumbs [--half] [--cache=DIR]"""
import math, random, sys, time, os, pickle
import numpy as np
from PIL import Image
import sb_world as SW
from sb_world import B, lin, srgb, shade, quant, bayer, checker
from sb_common import ART
import sb_scenes as SC
from sb_scenes import Rig, faces, g, sol, PX, value_noise

B.ALBEDO.update({"h8": "#f3dcc6",                                   # a pale scar
                 "J0": "#4d3a1c", "J1": "#7a5e2a", "J2": "#a8843f", "J3": "#d0ae62"})   # the turncoats' gilt

# ================================================================================================ the world
RIVER = (20, 32)            # water from z 20 to 31, one block below the meadow
BRIDGES = (-23, 7, 36)      # the west edge of each crossing (4 blocks wide); 7 is the north crossing
FAR_RIDGE = 112


def oak(w, x, z, h=5, r=2, seed=0, y0=0):
    rnd = random.Random(seed)
    for y in range(y0, y0 + h):
        w.set(x, y, z, "oak_log")
    for y in range(y0 + h - 2, y0 + h + 2):
        rr = r if y < y0 + h + 1 else r - 1
        for dx in range(-rr, rr + 1):
            for dz in range(-rr, rr + 1):
                if abs(dx) == rr and abs(dz) == rr and (rnd.random() < 0.7 or y >= y0 + h):
                    continue
                if (dx, dz) == (0, 0) and y < y0 + h:
                    continue
                if rnd.random() < 0.07:
                    continue
                w.set(x + dx, y, z + dz, "oak_leaves")


def tent(w, x0, z0, depth=4, wool="white_wool"):
    """a camp tent as a player builds one: an A-frame of wool three blocks wide, open at the front"""
    for z in range(z0, z0 + depth):
        w.set(x0, 0, z, wool)
        w.set(x0 + 2, 0, z, wool)
        w.set(x0 + 1, 1, z, wool)
    w.set(x0 + 1, 0, z0 + depth - 1, wool)


def river_world(seed=31):
    rnd = random.Random(seed)
    w = B.World(-70, 92, -4, 16, -30, 132)
    z0, z1 = RIVER
    for x in range(-70, 92):
        for z in range(-30, 132):
            w.set(x, -4, z, "stone")
            if z0 <= z < z1:
                w.set(x, -3, z, "dirt")
                w.set(x, -2, z, rnd.choice(["gravel", "gravel", "dirt", "coarse_dirt", "gravel"]))
                w.set(x, -1, z, "water")
            else:
                w.set(x, -3, z, "dirt")
                w.set(x, -2, z, "dirt")
                w.set(x, -1, z, "grass")
    # the shore: mud and gravel at the water's edge, never a clean line
    for x in range(-70, 92):
        for z in (z0 - 1, z1):
            if rnd.random() < 0.45:
                w.set(x, -1, z, rnd.choice(["coarse_dirt", "gravel", "dirt", "coarse_dirt"]))
        for z in (z0 - 2, z1 + 1):
            if rnd.random() < 0.15:
                w.set(x, -1, z, "coarse_dirt")
    # the far ridge: a low wooded rise closing the view
    for x in range(-70, 92):
        hgt = 1 + int(5.5 * value_noise(np.array([x + 0.5]), np.array([0.5]), 9.0, 4)[0] +
                      2.0 * value_noise(np.array([x + 0.5]), np.array([3.5]), 3.0, 7)[0])
        for z in range(FAR_RIDGE, 132):
            hh = min(hgt, int((z - FAR_RIDGE) * 0.6) + 1)
            for y in range(0, hh):
                w.set(x, y, z, "dirt")
            if hh > 0:
                w.set(x, hh - 1, z, "grass")
    for i in range(70):
        x, z = rnd.randrange(-68, 90), rnd.randrange(FAR_RIDGE + 2, 128)
        y = 0
        while w.get(x, y, z) is not None and y < 12:
            y += 1
        oak(w, x, z, rnd.choice([4, 5, 5, 6]), 2, seed=i, y0=y)
    # roads to each crossing, trodden
    for bx in BRIDGES:
        for z in list(range(-30, z0 - 3)) + list(range(z1 + 3, FAR_RIDGE)):
            for x in range(bx, bx + 4):
                if rnd.random() < (0.85 if 0 < x - bx < 3 else 0.45):
                    w.set(x, -1, z, rnd.choice(["path", "path", "path", "gravel", "coarse_dirt"]))
        bridge(w, bx, rnd)
    # the far bank: oaks, a broken wall
    for (x, z, h) in ((-44, 27 + 7, 5), (-38, 40, 6), (-30, 34, 5), (-14, 37, 6), (-7, 33, 5), (2, 38, 6),
                      (16, 35, 6), (21, 42, 5), (27, 34, 5), (46, 36, 6), (54, 42, 5), (-56, 38, 6), (64, 34, 5),
                      (-3, 52, 6), (24, 57, 6), (-31, 61, 5), (41, 67, 6), (-62, 59, 6), (71, 56, 5), (-19, 73, 6),
                      (9, 79, 5), (33, 89, 6), (-45, 87, 5), (58, 83, 6), (-8, 97, 6), (18, 103, 5)):
        oak(w, x, z, h, 2, seed=x * 7 + z)
    for i in range(46):
        x, z = rnd.randrange(-60, 80), rnd.randrange(z1 + 1, z1 + 9)
        if w.get(x, 0, z) is None and not any(bx - 2 <= x <= bx + 5 for bx in BRIDGES):
            w.set(x, 0, z, "oak_leaves")
            if rnd.random() < 0.4 and w.get(x + 1, 0, z) is None:
                w.set(x + 1, 0, z, "oak_leaves")
    for x in range(-12, -4):
        for y in range(0, 1 + (x % 3 == 0)):
            if rnd.random() < 0.8:
                w.set(x, y, 47, "mossy_stone_bricks" if rnd.random() < 0.4 else "stone_bricks")
    # plants on both banks; reeds at the water's edge
    for i in range(2400):
        x, z = rnd.randrange(-68, 90), rnd.choice([rnd.randrange(-28, z0), rnd.randrange(z1, FAR_RIDGE)])
        b = w.get(x, -1, z)
        if z0 - 7 <= z < z0 and -30 < x < 40:
            continue
        if w.get(x, 0, z) is None and b and b["name"] == "grass":
            w.set(x, 0, z, rnd.choice(["short_grass"] * 6 + ["tall_grass"] * 2 + ["poppy"]))
    for x in range(-68, 90):
        for z in (z0 - 1, z1):
            b = w.get(x, -1, z)
            if rnd.random() < (0.07 if z < z0 else 0.2) and w.get(x, 0, z) is None and b and b["name"] != "water":
                w.set(x, 0, z, "tall_grass")
    return w


def bridge(w, bx, rnd):
    """a stone-brick crossing four blocks wide: the deck a block above the meadow on two abutments and a pier in
    midstream, so the river shows through two low arches; steps of slab at both ends; a parapet of stone brick
    along both edges with a crenel every third block; lantern posts at the four corners"""
    z0, z1 = RIVER
    sb = lambda: "stone_bricks" if rnd.random() < 0.72 else "mossy_stone_bricks"
    mid = (z0 + z1) // 2
    for x in range(bx, bx + 4):
        for z in range(z0 - 1, z1 + 1):
            w.set(x, 0, z, sb())                              # deck (top at 1)
        for z in range(z0, z1):
            if (z - z0) % 4 == 0 or z == z1 - 1:
                w.set(x, -1, z, sb())                         # abutments and piers stand in the water
        w.set(x, 0, z0 - 2, "brick_slab")
        w.set(x, 0, z1 + 1, "brick_slab")
    for z in range(z0 - 1, z1 + 1):
        for x in (bx, bx + 3):
            if (z - z0) % 3 != 2 or z in (z0 - 1, z1):
                w.set(x, 1, z, sb())
            else:
                w.set(x, 1, z, "stone_slab")
    for (x, z) in ((bx, z0 - 1), (bx + 3, z0 - 1), (bx, z1), (bx + 3, z1), (bx, (z0 + z1) // 2)):
        w.set(x, 2, z, "fence")
        w.set(x, 3, z, "lantern")


# ================================================================================================ figures
FIG_KEY = dict(SC.KN_KEY)
FIG_KEY.update({"s": "h4", "S": "h5", "D": "h6", "X": "h7", "R": "y0", "r": "y1", "q": "y3", "x": "h8",
                "W": "E3", "p": "C1", "u": "K0", "M": "T1", "N": "T0", "P": "L2", "Q": "L1", "B": "k1", "V": "k2",
                "J": "J1", "j": "J2", "I": "J3", "l": "L3"})
MAUD_FACE = g([
    "RRRRRRRR",
    "RqRRRRRR",
    "qssssxsR",
    "RuuusxuR",
    "RWpssWpR",
    "RssSSssR",
    "RsSXXSsR",
    "RSssssSR"], FIG_KEY)
MAUD_SIDE_R = g(["RRRRRRRR", "RRRRRRqR", "RRRRRqss", "RRRRqssu", "RRRSSssW", "RRRSDssS", "RRRRssss",
                 "RRRDDssD"], FIG_KEY)
MAUD_SIDE_L = g(["RRRRRRRR", "RqRRRRRR", "ssqRRRRR", "ussqRRRR", "WssSSRRR", "SssDSRRR", "ssssRRRR",
                 "DssDDRRR"], FIG_KEY)
MAUD_BACK = g(["RRRRRRRR", "RRRqRRRR", "RRRRRRRR", "RRRRRqRR", "RRRRRRRR", "RRRRRRRR", "RRRRRRRR",
               "DRRRRRRD"], FIG_KEY)
MAUD_TOP = g(["RRRRRRRR", "RRqRRRRR", "RRRqRRRR", "RRRRRRRR", "RRRRRqRR", "RRRRRRRR", "RRRRRRRR", "RRRRRRRR"],
             FIG_KEY)
TABARD_FRONT = g([
    "fggggggf",
    "mmmhhmmn",
    "mmmhhmmn",
    "hhhhhhhh",
    "hhhhhhhh",
    "mmmhhmmn",
    "mmmhhmmn",
    "mmmhhmmn",
    "nmmhhmnn",
    "GGGHHGGG",
    "nmmhhmnn",
    "nnmhhmnn"], FIG_KEY)
LAP_TOP = g(["nmmhhmmn", "mmmhhmmn", "mmmhhmmn", "nmmhhmmn", "nmmhhmnn", "nnmhhmnn"], FIG_KEY)
LAP_FRONT = g(["nmmhhmnn", "nnmhhmnn"], FIG_KEY)
PACT_COPY_BACK = g(["llllll", "lyyyyl", "llllll", "lyyyll", "llWWll"], dict(FIG_KEY, y="L1", W="W1"))


PLATE_ARM_C = g(["gggg", "ghhg", "gggf", "fggf", "efff", "cddc", "bccb", "gggf", "ghgf", "fggf", "bbbb", "abba"],
                FIG_KEY)
PLATE_LEG_C = g(["gggf", "ghgf", "fggf", "efgf", "cddc", "bGcb", "fggf", "ghgf", "fggf", "efff", "aaaa", "aaaa"],
                FIG_KEY)


def seated_legs(r, leg, boot):
    """legs as the game seats a player: thighs straight forward from the hip, shins straight down to the ground
    (the rig's origin is on the ground under the hips)"""
    for (x0, x1) in ((-2, 0), (0, 2)):
        r.limb((x0, 0, -2), (x1, 12, 2), leg, ((x0 + x1) / 2.0, 12, 0), pitch=90)
        r.add((x0 - 0.05, 0, -12), (x1 + 0.05, 12, -8), boot)


def maud_seated(at, yaw=-25.0, head_yaw=20.0):
    """Maud on a log by the fire, helm off: Templar plate, the company's navy tabard with its white cross; her
    tabard over her knees; both hands up in front of her holding the turncoat's pact-copy, its back toward us
    (a line of ink, the red seal); her head turned back to whoever she is talking to"""
    r = Rig(at, yaw, 0.0)
    plate = faces(all=PLATE_LEG_C)
    seated_legs(r, plate, plate)
    r.add((-4, 12, -2), (4, 24, 2), faces(front=TABARD_FRONT, back=SC.TABARD_BACK, left=SC.TABARD_SIDE,
                                          right=SC.TABARD_SIDE, top=sol("E2", 8, 4)))
    r.add((-4.2, 13.6, -11.2), (4.2, 14.4, -1.9), faces(top=LAP_TOP, front=LAP_FRONT, all=sol("C0", 8, 1)))
    r.add((-4.2, 7.6, -11.6), (4.2, 14.4, -11.0), faces(front=g(["nmmhhmnn"] * 3 + ["nnmhhmnn"] * 3, FIG_KEY),
                                                       all=sol("C0", 8, 6)))     # the tabard falls past the knee
    head = B.rot(yaw=head_yaw, pitch=3)
    r.add((-4, 24, -4), (4, 32, 4), faces(front=MAUD_FACE, right=MAUD_SIDE_R, left=MAUD_SIDE_L, back=MAUD_BACK,
                                          top=MAUD_TOP, bottom=sol("h7", 8, 8)), head, (0, 24, 0))
    r.add((-1.2, 23.0, 4), (1.2, 27.0, 5.4), faces(all=sol("y0")), head, (0, 24, 0))          # her hair, tied
    for sx in (-1, 1):                                                                        # pauldrons
        x0 = 4 if sx > 0 else -9
        r.add((x0, 21, -2.6), (x0 + 5, 25, 2.6), faces(top=g(["hhhhg", "hgggf", "ggggf", "gggff", "Gfffe"],
                                                              FIG_KEY), all=g(["hhhhh", "ggggg", "fffff", "GeeeG"],
                                                                              FIG_KEY)))
    for (x0, x1, rl) in ((-8, -4, 9), (4, 8, -9)):                   # arms up and in, to the paper
        r.limb((x0, 12, -2), (x1, 24, 2), faces(all=PLATE_ARM_C), ((x0 + x1) / 2.0, 22, 0), pitch=68, roll=rl)
    tilt = B.rot(pitch=-58)
    r.add((-3.2, 16.0, -12.6), (3.2, 20.6, -12.0), faces(front=PACT_COPY_BACK, all=sol("L2", 6, 1)), tilt,
          (0, 18, -12))
    return r.boxes


def helm_on_log(at, yaw=24.0):
    r = Rig(at, yaw, 0.0)
    r.add((-4, 0, -4), (4, 8, 4), faces(front=SC.HELM_FRONT, back=SC.HELM_BACK, left=SC.HELM_SIDE,
                                        right=SC.HELM_SIDE, top=SC.HELM_TOP))
    return r.boxes


SQ_FACE = g(["MMMMMMMM", "MddddddM", "NssssssN", "NrrssrrN", "NWpssWpN", "NsSDDSsN", "NssXXssN", "NMSssSMN"],
            FIG_KEY)
SQ_SIDE_R = g(["MMMMMMMM", "MddddddM", "NNNNNsss", "NNNNssss", "NNNSSssW", "NNNSDsss", "NNNNssss",
               "NNNMMssM"], FIG_KEY)
SQ_SIDE_L = g(["MMMMMMMM", "MddddddM", "sssNNNNN", "ssssNNNN", "WssSSNNN", "sssDSNNN", "ssssNNNN",
               "MssMMNNN"], FIG_KEY)


def sq_coat(panel):
    """the Order squire's hip-length white surcoat with its team panel down the front (unit identity)"""
    a, b = panel
    rows = ["NMNMNMNM", "gggggggg", "gg" + a * 4 + "gg", "gf" + a * 4 + "fg", "gf" + a * 4 + "fg",
            "gf" + a * 4 + "fg", "ff" + a + b + b + a + "ff", "ff" + b * 4 + "ff", "JGGHGGGJ", "ffggggff",
            "ffggggff", "efggggfe"]
    return g(rows, FIG_KEY)


SQ_COAT_HOLLOW = sq_coat("mn")
SQ_COAT_TURN = sq_coat("jJ")
MAIL = g(["cccc", "bbbb"] * 6, FIG_KEY)
HOSE = g(["kkkk"] * 10 + ["bbbb"] * 2, {"k": "k1", "b": "b1"})
CAP_SIDE = g(["dddddddd", "cccccccc", "bbbbbbbb"], FIG_KEY)


def squire_head(r, head, coat_pivot=(0, 24, 0)):
    r.add((-4, 24, -4), (4, 32, 4), faces(front=SQ_FACE, right=SQ_SIDE_R, left=SQ_SIDE_L, back=sol("T1", 8, 8),
                                          top=sol("T2", 8, 8)), head, coat_pivot)
    r.add((-4.5, 29, -4.5), (4.5, 32.5, 4.5), faces(top=sol("T3", 8, 8), all=CAP_SIDE), head, coat_pivot)


def squire_seated(at, yaw=0.0, head_yaw=0.0, head_pitch=-10.0, coat=None, arm=56.0):
    """an Order squire of the company sitting to eat: iron cap, mail, the white surcoat with the company's navy
    panel; a wooden bowl in both hands"""
    r = Rig(at, yaw, 0.0)
    seated_legs(r, faces(all=sol("k1")), faces(all=sol("b1")))
    r.add((-4, 12, -2), (4, 24, 2), faces(front=SQ_COAT_HOLLOW if coat is None else coat, back=sol("E1", 8, 12),
                                          all=sol("E1", 4, 12)))
    squire_head(r, B.rot(yaw=head_yaw, pitch=head_pitch))
    for (x0, x1, rl) in ((-8, -4, 10), (4, 8, -10)):
        r.limb((x0, 12, -2), (x1, 24, 2), faces(all=MAIL), ((x0 + x1) / 2.0, 22, 0), pitch=arm, roll=rl)
    r.add((-2.6, 14.2, -12.6), (2.6, 16.8, -8.4), faces(top=g(["kkkkk", "kSSSk", "kSDSk", "kkkkk"],
                                                              {"k": "k2", "S": "d2", "D": "d3"}),
                                                       all=g(["KKKKK", "kkkkk"], {"k": "k1", "K": "k2"})))
    return r.boxes


def squire_standing(at, yaw=0.0, coat=None, spear=True, shield=True, head_yaw=0.0, team="hollow"):
    """a squire standing: the same kit, a spear upright in the right hand, a shield on the left arm"""
    r = Rig(at, yaw, 0.0)
    r.add((-2, 0, -2), (0, 12, 2), faces(all=HOSE))
    r.add((0, 0, -2), (2, 12, 2), faces(all=HOSE))
    r.add((-4, 12, -2), (4, 24, 2), faces(front=SQ_COAT_HOLLOW if coat is None else coat, back=sol("E1", 8, 12),
                                          all=sol("E1", 4, 12)))
    squire_head(r, B.rot(yaw=head_yaw))
    r.limb((-8, 12, -2), (-4, 24, 2), faces(all=MAIL), (-6, 22, 0), pitch=24)
    r.limb((4, 12, -2), (8, 24, 2), faces(all=MAIL), (6, 22, 0), pitch=14)
    if shield:
        a, b = ("m", "h") if team == "hollow" else ("j", "h")
        pane = g(["g" * 8, "g" + a * 6 + "g", "g" + a + b * 4 + a + "g", "g" + a + b * 4 + a + "g",
                  "g" + a * 6 + "g", "g" * 8, "gg" + a * 4 + "gg"], FIG_KEY)
        r.add((-10.6, 7, -6), (-9.4, 18, 4), faces(left=pane, all=sol("E1", 8, 10)), B.rot(pitch=24), (-6, 22, 0))
    if spear:
        r.add((6.4, -2, -9), (7.6, 44, -7.8), faces(all=sol("S2")), B.rot(pitch=14), (6, 22, 0))
        r.add((6.0, 44, -9.3), (8.0, 50, -7.5), faces(all=g(["dd", "cc", "cc", "bb"], FIG_KEY)), B.rot(pitch=14),
              (6, 22, 0))
    return r.boxes


HOLLOW_CLOTH = g([
    "GGGGGGGGGGGG",
    "GmmmmmmmmmmG",
    "GmmmmmmmmmmG",
    "GmmmmhhmmmmG",
    "GmmmhnnhmmmG",
    "GmmhnnnnhmmG",
    "GmhnnnnnnhmG",
    "GmhnnnnnnhmG",
    "GmmhnnnnhmmG",
    "GmmmhnnhmmmG",
    "GmmmmhhmmmmG",
    "GmmmmmmmmmmG",
    "GmmmmmmmmmmG",
    "GmmmmmmmmmmG",
    "GmmmmG.GmmmG",
    "GmmmG...GmmG",
    "GmmG.....GmG",
    "GmG.......GG"], FIG_KEY)
TURN_CLOTH = g([
    "HHHHHHHHHHHH",
    "HhhhhhhhhhhH",
    "HhhhhjjhhhhH",
    "HhhhjIIjhhhH",
    "HhhjIjjIjhhH",
    "HhjIjhhjIjhH",
    "HhhjIjjIjhhH",
    "HhhhjIIjhhhH",
    "HhhhhjjhhhhH",
    "HhhhhjjhhhhH",
    "HhhhhjjhhhhH",
    "HhhhhhhhhhhH",
    "HhhhhhhhhhhH",
    "HhhhhH.HhhhH",
    "HhhhH...HhhH",
    "HhhH.....HhH"], FIG_KEY)


def banner_pole(at, cloth, h=3.4, w=0.85, yaw=0.0, sway=0.0, back="C0"):
    """a banner on a pole: the pole, a crossbar, a finial, the cloth hanging from the bar"""
    x, y, z = at
    out = [B.Box((x, y + h / 2, z), (0.07, h / 2, 0.07), {"all": g(["S", "s"] * 4, {"S": "S1", "s": "S2"})}),
           B.Box((x, y + h - 0.14, z - 0.08), (w / 2 + 0.08, 0.05, 0.05), {"all": sol("S2")}),
           B.Box((x, y + h + 0.08, z), (0.09, 0.09, 0.09), {"all": sol("G2"), (1, 1): sol("e3")})]
    rows, cols = cloth.shape[:2]
    ch = w * rows / float(cols)
    out.append(B.Box((x, y + h - 0.2 - ch / 2, z - 0.1), (w / 2, ch / 2, 0.03),
                     {(2, -1): cloth, (2, 1): cloth, "all": sol(back)}, B.rot(yaw=yaw, pitch=sway)))
    return out


def fire_logs(cx, cz):
    """the campfire's logs in a square, as the game lays them, round the flame"""
    log = g(["bbbbbbbbbbbbbbbb", "BbBbbBbbBbbBbbBb", "bbbbbbbbbbbbbbbb"], {"b": "b1", "B": "b2"})
    end = g(["rr", "rr"], {"r": "r2"})
    out = []
    for (dx, along, y) in ((-0.3, "z", 0.09), (0.3, "z", 0.09), (-0.3, "x", 0.27), (0.3, "x", 0.27)):
        if along == "z":
            out.append(B.Box((cx + 0.5 + dx, y, cz + 0.5), (0.1, 0.09, 0.5), {(2, 1): end, (2, -1): end,
                                                                               "all": log}))
        else:
            out.append(B.Box((cx + 0.5, y, cz + 0.5 + dx), (0.5, 0.09, 0.1), {(0, 1): end, (0, -1): end,
                                                                               "all": log}))
    out.append(B.Box((cx + 0.5, 0.02, cz + 0.5), (0.4, 0.02, 0.4),
                     {"all": g(["vvwv", "wvvv", "vvvw", "vwvv"], {"v": "v1", "w": "c0"})}))
    return out


def log_seat(x0, x1, z, yaw=0.0):
    bark = g(["bbbbbbbb", "bBbbBbbB", "bbbbbbbb", "BbbBbbbB"], {"b": "b1", "B": "b2"})
    rings = g(["rRr", "RrR", "rRr"], {"r": "r2", "R": "r1"})
    return [B.Box(((x0 + x1 + 1) / 2.0, 0.28, z + 0.5), ((x1 - x0 + 1) / 2.0, 0.28, 0.28),
                  {(0, 1): rings, (0, -1): rings, "all": bark}, B.rot(yaw=yaw))]


def kit_pile(at):
    """packs and a rolled blanket by the fire"""
    x, y, z = at
    return [B.Box((x, 0.2, z), (0.3, 0.2, 0.22), {"all": g(["kKkk", "kkKk"], {"k": "k1", "K": "k0"}),
                                                   (1, 1): sol("k2")}),
            B.Box((x + 0.45, 0.13, z + 0.1), (0.14, 0.13, 0.32), {"all": g(["CcC", "cCc"], {"C": "C1", "c": "C2"})})]


def commander(at, yaw=180.0):
    """Danta, seen from behind: the company's navy gambeson, a grey cloak, an arming sword on the hip, dark hair"""
    r = Rig(at, yaw, 0.0)
    for (x0, x1) in ((-2, 0), (0, 2)):
        r.add((x0, 0, -2), (x1, 12, 2), faces(all=g(["kkkk"] * 9 + ["bbbb"] * 3, {"k": "k1", "b": "b0"})))
    r.add((-4, 12, -2), (4, 24, 2), faces(all=g(["mmmm", "mnmm"] * 6, FIG_KEY)))
    r.add((-4.6, 9.5, 2.0), (4.6, 24.4, 2.9), faces(all=g(["aaaaaaaa", "aabaaaba", "aaaaaaaa"] * 4,
                                                          {"a": "y2", "b": "y1"}), top=sol("y2", 8, 1)))
    r.add((-4, 24, -4), (4, 32, 4), faces(all=g(["RRRRRRRR"] * 6 + ["RRRssRRR", "RRssssRR"], FIG_KEY),
                                          front=g(["RRRRRRRR", "RssssssR", "ssssssss", "ruusuurs", "sWpssWps",
                                                   "sssDDsss", "ssSXXSss", "sssssssS"], FIG_KEY)))
    r.limb((-8, 12, -2), (-4, 24, 2), faces(all=g(["mmmm"] * 10 + ["ssss"] * 2, FIG_KEY)), (-6, 22, 0), pitch=4)
    r.limb((4, 12, -2), (8, 24, 2), faces(all=g(["mmmm"] * 10 + ["ssss"] * 2, FIG_KEY)), (6, 22, 0), pitch=-6)
    r.add((4.4, 2, -1), (5.4, 13, 0.2), faces(all=sol("K1")), B.rot(roll=-16), (5, 13, 0))
    return r.boxes


# ================================================================================================ sky
def sky_at(cam, sx, sy, bands, horizon_deg, clouds=None, sun=None, gamma=1.0):
    """the sky for screen positions (any row, also above the frame or mirrored for the river): banded by the
    angle above the horizon, each band edge a checker; the game's square sun with stepped glow; a layer of the
    game's flat clouds in perspective, each cell lit along its near edge. Returns sRGB floats (n, 3)"""
    sx, sy = np.asarray(sx, float), np.asarray(sy, float)
    el = np.degrees(np.arctan2((cam.yv - sy) / cam.f, 1.0))
    n = len(bands)
    u = np.clip(el / horizon_deg, 0, 1) ** gamma * (n - 1)
    i = np.floor(u).astype(int)
    f = u - i
    th = ((np.floor(sx) + np.floor(sy)) % 2 == 0)
    up = (f > 0.82) & th
    cols = np.array([SC.hexv(c) for c in bands])
    out = cols[np.clip(i + up, 0, n - 1)]
    if clouds is not None:
        cy_, cell, seed, col, rim, far, cover = clouds
        Dy = (cam.yv - sy) / cam.f
        Dx = (sx - cam.xv) / cam.f
        m = Dy > 0.004
        t = np.where(m, (cy_ - cam.eye[1]) / np.maximum(Dy, 1e-4), 0)
        X = cam.eye[0] + Dx * t
        Z = cam.eye[2] + t
        cxi, czi = np.floor(X / cell), np.floor(Z / cell)
        v = value_noise(cxi + 0.5, czi + 0.5, 2.3, seed) * 0.7 + value_noise(cxi + 0.5, czi + 0.5, 0.9, seed + 3) * 0.3
        isc = m & (v > cover)
        front = value_noise(cxi + 0.5, czi - 0.5, 2.3, seed) * 0.7 + value_noise(cxi + 0.5, czi - 0.5, 0.9,
                                                                                 seed + 3) * 0.3
        edge = isc & (front <= cover) & ((Z / cell - czi) < 0.16)
        fade = np.clip((t - 60) / far, 0, 1)
        fq = np.floor(fade * 3 + 0.5) / 3.0
        base = np.where(edge[:, None], np.array(SC.hexv(rim)), np.array(SC.hexv(col)))
        cc = base * (1 - fq[:, None] * 0.8) + out * (fq[:, None] * 0.8)
        out = np.where(isc[:, None], cc, out)
    if sun is not None:
        (ux, uy), size, core, rim_c, glow = sun
        d = np.maximum(np.abs(sx - ux), np.abs(sy - uy))
        dr = np.hypot(sx - ux, (sy - uy) * 1.25)          # the glow is round (a little flattened), the sun square
        for (r, c, a) in glow:
            m = (dr < r - 1.5) | ((dr < r + 1.5) & th)
            out[m] = out[m] * (1 - a) + np.array(SC.hexv(c)) * a
        m = d < size / 2.0
        out[m] = np.array(SC.hexv(core))
        m2 = m & (d >= size / 2.0 - max(1.0, size / 8.0))
        out[m2] = np.array(SC.hexv(rim_c))
    return out


DUSK_BANDS = ["#eab46a", "#e29d55", "#d48447", "#c06d46", "#a85a4a", "#8e4a4e", "#753e52", "#5e3653", "#4a2f52",
              "#38294c", "#2a2544", "#20203c", "#191b33", "#141629"]
DAY_BANDS = ["#c9d7e0", "#bccde0", "#aec3df", "#a0b8dd", "#92addb", "#85a2d7", "#7a99d3", "#708fcf", "#6787ca"]


# ================================================================================================ the river's mirror
def mirror(world, figs, R, mask, cam, sky_fn, amb, warm, squash=0.22, reach=9.0):
    """the reflection in the river: each water pixel traces its reflected ray from the water, through the
    world and the figures; what it meets is lit flat by its own sky and block light, what it misses is the sky
    seen along the mirrored ray (the frame row mirrored about the horizon). Returns sRGB floats for the mask"""
    idx = np.nonzero(mask.ravel())[0]
    H, W = mask.shape
    P = R["P"].reshape(-1, 3)[idx]
    D = R["D"].reshape(-1, 3)[idx].copy()
    D[:, 1] *= -1
    O = P + np.array([0.0, 0.02, 0.0])
    t, nrm, bid, cell = B.trace(world, O, D)
    hit = np.isfinite(t) & (t < reach)          # a painter's river: the bank, the reeds, the bridge; then sky
    tone = np.full(idx.size, "", dtype=object)
    Ph = np.zeros((idx.size, 3))
    if hit.any():
        Ph[hit] = O[hit] + D[hit] * t[hit, None]
        tn, pc = SW.sample(world, Ph[hit], nrm[hit], bid[hit], cell[hit])
        tone[hit] = tn
        Ph[hit] = pc
    boxes = [b for f_ in figs for b in f_.boxes]
    if boxes:
        te, ne, be, toe, _ = B.boxes_hit(O, D, boxes)
        closer = (be >= 0) & (te < np.where(hit, t, reach))
        tone[closer] = toe[closer]
        Ph[closer] = O[closer] + D[closer] * te[closer, None]
        nrm[closer] = ne[closer]
        hit = hit | closer
    sky = B.sky_light(world)
    blk = B.block_light(world)
    ys, xs = idx // W, idx % W
    out = sky_fn(xs + 0.5, cam.yv - ((ys + 0.5) - cam.yv) * squash)
    if hit.any():
        h = np.nonzero(hit)[0]
        lv = B.cell_sky(world, sky, Ph[h] + nrm[h] * 0.05)
        bl = B.cell_sky(world, blk, Ph[h] + nrm[h] * 0.05, outside=0)
        aq = quant(B.brightness(lv) * 0.8, 4)
        wq = quant(B.brightness(bl) ** 2.0, 4)
        for j, k in enumerate(h):
            tn = tone[k]
            alb = lin(B.ALBEDO[tn])
            c = alb if tn in SW.EMIT else alb * (np.asarray(amb) * aq[j] + np.asarray(warm) * wq[j])
            out[k] = srgb(c)
    return out, idx


def lin_arr(srgb_img):
    c = np.asarray(srgb_img, float) / 255.0
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def pools(R, lk):
    """the block light as round pools (a painter's falloff) in place of the game's diamond: each source gives
    15 less a fixed step per block of distance; the fire reaches further than a lantern"""
    P = R["P"]
    out = np.zeros(P.shape[:2])
    srcs = ([(R["fire"], lk.get("fire_fall", 1.35))] if R.get("fire") is not None else []) +            [(l, lk.get("lantern_fall", 1.9)) for l in R.get("lights", [])]
    for (c, fall) in srcs:
        d = np.linalg.norm(P - np.asarray(c)[None, None, :], axis=2)
        out = np.maximum(out, 15 - fall * d)
    return np.where(R["hit"], np.clip(out, 0, 15), 0)


def firelit(R, fire):
    """the fire as a painter would light the faces round it: a figure's faces turned to the flame take a little
    more of its light and those turned away a little less (the game's block light has no direction)"""
    P, n = R["P"], R["nrm"]
    d = np.asarray(fire)[None, None, :] - P
    d /= np.maximum(np.linalg.norm(d, axis=2, keepdims=True), 1e-6)
    dot = np.sum(d * n, axis=2)
    gain = np.where(R["ent"], 0.92 + 0.22 * np.clip(dot, -1, 1), 1.0)
    return np.clip(R["blk"] * gain, 0, 15)


# ================================================================================================ dusk
DUSK_CAM = dict(f=600, xv=470, yv=180, eye=(0.0, 3.17, -1.0))
DUSK_SUN_DIR = (-0.70, 0.07, 0.71)
FIRE = (0, 9)                                         # the campfire's cell (x, z)


def dusk_figures(w):
    figs = []
    fx, fz = FIRE
    w.set(fx, 0, fz, "fire")
    figs.append(SW.Figure(fire_logs(fx, fz)))
    figs.append(SW.Figure(log_seat(-3, -1, 8, yaw=-14.0)))
    figs.append(SW.Figure(maud_seated((-1.75, 0.0, 8.55), yaw=-34.0, head_yaw=26.0)))
    figs.append(SW.Figure(helm_on_log((-2.75, 0.56, 8.35), yaw=30.0)))
    figs.append(SW.Figure(log_seat(1, 2, 11, yaw=8.0)))
    figs.append(SW.Figure(squire_seated((1.05, 0.0, 11.5), yaw=14.0, head_yaw=-20.0, head_pitch=-8.0, arm=40.0)))
    figs.append(SW.Figure(log_seat(2, 2, 8, yaw=90.0)))
    figs.append(SW.Figure(squire_seated((2.3, 0.0, 9.45), yaw=-74.0, head_yaw=-16.0, head_pitch=-14.0)))
    figs.append(SW.Figure(kit_pile((-3.6, 0.0, 10.2))))
    figs.append(SW.Figure(banner_pole((-4.1, 0.0, 11.3), HOLLOW_CLOTH, h=3.5, w=0.9, yaw=6.0, sway=-4.0)))
    figs.append(SW.Figure(squire_standing((8.6, 1.0, 23.4), yaw=150.0, head_yaw=-20.0)))
    figs.append(SW.Figure(squire_standing((6.0, 0.0, 16.6), yaw=200.0)))
    return figs


def dusk_world():
    return river_world()


def dusk_sky_fn(cam, k):
    sun = ((132 * k, cam.yv - 4 * k), 22 * k, "#fff3c8", "#ffd88a",
           ((84 * k, "#f2b064", 0.10), (56 * k, "#f6bf6e", 0.16), (36 * k, "#fbd38a", 0.26)))
    clouds = (52.0, 9.0, 11, "#6c4058", "#e0905e", 260.0, 0.60)
    return lambda sx, sy: sky_at(cam, sx, sy, DUSK_BANDS, 13.5, clouds, sun, 0.85)


def trace_scene(world_fn, figs_fn, camd, sun_dir, sky_fn_of, k=1.0, cache=None, W=960, H=540, fire=None,
                mirror_light=((0.10, 0.07, 0.12), (1.2, 0.7, 0.32))):
    if cache and os.path.exists(cache):
        with open(cache, "rb") as fh:
            return pickle.load(fh)
    t0 = time.time()
    w = world_fn()
    figs = figs_fn(w)
    w.finish()
    cam = B.Camera(camd["f"] * k, camd["xv"] * k, camd["yv"] * k, camd["eye"])
    R = SW.render(w, cam, int(W * k), int(H * k), figures=figs, L=sun_dir)
    print("  traced in %.1fs" % (time.time() - t0))
    R["k"], R["cam"], R["fire"] = k, camd, fire
    lan = np.argwhere(w.id == B.BID["lantern"]) + w.lo
    R["lights"] = [(float(x) + 0.5, float(y) + 0.35, float(z) + 0.5) for (x, y, z) in lan]
    wmask = np.isin(R["tone"], ["o0", "o1", "o2", "o3"]) & (R["nrm"][..., 1] > 0.5) & R["hit"]
    R["wmask"] = wmask
    R["mirror"] = mirror(w, figs, R, wmask, cam, sky_fn_of(cam, k), *mirror_light)
    print("  mirror done at %.1fs (%d water px)" % (time.time() - t0, int(wmask.sum())))
    if cache:
        with open(cache, "wb") as fh:
            pickle.dump(R, fh)
    return R


DUSK_LOOK = dict(amb=(0.105, 0.080, 0.150), sun=(0.80, 0.42, 0.20), warm=(1.70, 0.86, 0.32), fog="#7a5068",
                 layers=[(16.0, 0.0), (34.0, 0.10), (70.0, 0.24), (1e9, 0.42)], d0=0.020, hs=0.9, warm_curve=1.6,
                 sky_mix=0.62, mirror_mix=(0.74, 0.56), warm_q=4000, warm_band=None, fire_fall=1.05,
                 lantern_fall=1.8, level_band=0.6)


def compose(R, sky_fn, lk, name, post=None):
    t0 = time.time()
    k = R["k"]
    Hd, Wd = R["hit"].shape
    yy, xx = np.mgrid[0:Hd, 0:Wd]
    sky = sky_fn(xx.ravel() + 0.5, yy.ravel() + 0.5).reshape(Hd, Wd, 3)
    R = dict(R)
    if R.get("fire") is not None or R.get("lights"):
        R["blk"] = pools(R, lk)
        if R.get("fire") is not None:
            R["blk"] = firelit(R, R["fire"])
        lv = R["blk"]
        lo = np.floor(lv)
        th = 0.5 + (checker(Hd, Wd) - 0.5) * lk.get("level_band", 0.6)
        R["blk"] = np.where(R["ent"], np.round(lv), lo + ((lv - lo) > th))
    fog = SC.stepped_fog(R, SC.ground_fog(R, lk["d0"], lk["hs"], seed=5), lk["layers"], 4, 0.3)
    fog_col = lin_arr(sky) * lk["sky_mix"] + lin(lk["fog"])[None, None, :] * (1 - lk["sky_mix"])
    out, hit, ncodes = shade(R, amb_col=lk["amb"], sun_col=lk["sun"], warm_col=lk["warm"], fog=fog,
                             fog_col=fog_col, warm_curve=lk["warm_curve"], fog_q=0, warm_q=lk["warm_q"],
                             warm_band=lk["warm_band"])
    img = np.where(hit[..., None], srgb(out), np.round(sky)).astype(float)
    mcol, idx = R["mirror"]
    flat = img.reshape(-1, 3)
    if idx.size:
        water = flat[idx]
        tone = R["tone"].ravel()[idx]
        ripple = np.isin(tone, ["o2", "o3"])
        a, b = lk["mirror_mix"]
        mix = np.where(ripple[:, None], b, a)
        # the mirror fogs as the water does
        fq = fog.ravel()[idx][:, None]
        fc = srgb(fog_col.reshape(-1, 3)[idx]).astype(float)
        m2 = mcol * (1 - fq) + fc * fq
        flat[idx] = m2 * mix + water * (1 - mix)
    img = flat.reshape(Hd, Wd, 3)
    if post:
        img = post(img)
    Image.fromarray(np.clip(np.round(img), 0, 255).astype(np.uint8), "RGB").save(ART + "/" + name + ".png")
    print("  art/%s.png %dx%d, %d codes, %.1fs" % (name, Wd, Hd, ncodes, time.time() - t0))
    return img


def sparkle(img, R, cam, sun_x, lights, k=1.0, seed=4):
    """the river catching the light as a painter marks it: a glitter path of short horizontal dashes under the
    low sun, widening toward us and thinning out, and a broken warm streak under each lantern"""
    rnd = random.Random(seed)
    wm = R["wmask"]
    H, W = wm.shape
    ys = np.nonzero(wm.any(axis=1))[0]
    if not len(ys):
        return img
    y0, y1 = ys.min(), ys.max()
    for y in range(y0, y1 + 1):
        f = (y - y0) / max(1.0, float(y1 - y0))
        half = (10 + 26 * f) * k
        x = int(sun_x - half)
        while x < sun_x + half:
            run = rnd.randint(2, 6)
            if rnd.random() < 0.55 - 0.3 * f:
                c = SC.hexv("#fff1c0") if rnd.random() < 0.4 else SC.hexv("#f6c477")
                for xx in range(x, min(int(sun_x + half), x + run)):
                    if 0 <= xx < W and wm[y, xx]:
                        img[y, xx] = c
            x += run + rnd.randint(1, 5)
    for (lx, ly, lz) in lights:
        mz = lz - cam.eye[2]
        if mz <= 0.5:
            continue
        sx = cam.xv + cam.f * (lx - cam.eye[0]) / mz
        sy = cam.yv - cam.f * ((-0.25 - ly) - cam.eye[1]) / mz
        X, Y = int(round(sx)), int(round(sy))
        for dy in range(-int(14 * k), int(10 * k)):
            yy = Y + dy
            if 0 <= yy < H and 0 <= X < W and wm[yy, X] and (dy % 2 == 0 or abs(dy) < 3):
                a = 1.0 - abs(dy) / (14.0 * k)
                c = SC.hexv("#ffd27a") if a > 0.55 else SC.hexv("#e0802c")
                for xx in (X - 1, X, X + 1) if a > 0.6 else (X,):
                    if 0 <= xx < W and wm[yy, xx]:
                        img[yy, xx] = img[yy, xx] * (1 - a) + c * a
    return img


def render_dusk(k=1.0, cache=None, name="stage-crossing", look=None):
    lk = dict(DUSK_LOOK, **(look or {}))
    R = trace_scene(dusk_world, dusk_figures, DUSK_CAM, DUSK_SUN_DIR, dusk_sky_fn, k, cache,
                    fire=(FIRE[0] + 0.5, 0.4, FIRE[1] + 0.5))
    c = R["cam"]
    cam = B.Camera(c["f"] * k, c["xv"] * k, c["yv"] * k, c["eye"])
    return compose(R, dusk_sky_fn(cam, k), lk, name,
                   post=lambda img: sparkle(img, R, cam, 132 * k, R.get("lights", []), k))


# ================================================================================================ day
def maud_standing(at, yaw=180.0, head_yaw=0.0):
    """Maud on her feet by day, helm off and carried under her left arm, seen from behind: plate legs, the
    company's navy tabard and its skirt, her dark hair tied at the nape, the sword at her right hip"""
    r = Rig(at, yaw, 0.0)
    r.add((-2, 0, -2), (0, 12, 2), faces(all=SC.PLATE_LEG))
    r.add((0, 0, -2), (2, 12, 2), faces(all=SC.PLATE_LEG))
    r.add((-4, 12, -2), (4, 24, 2), faces(back=SC.TABARD_BACK, front=TABARD_FRONT, left=SC.TABARD_SIDE,
                                          right=SC.TABARD_SIDE, top=sol("E2", 8, 4)))
    r.add((-4.3, 5, -2.3), (4.3, 13, 2.3), faces(back=SC.SKIRT_BACK, front=SC.SKIRT_BACK, all=sol("C1", 4, 8)))
    head = B.rot(yaw=head_yaw)
    r.add((-4, 24, -4), (4, 32, 4), faces(front=MAUD_FACE, right=MAUD_SIDE_R, left=MAUD_SIDE_L, back=MAUD_BACK,
                                          top=MAUD_TOP, bottom=sol("h7", 8, 8)), head, (0, 24, 0))
    r.add((-1.2, 23.0, 4), (1.2, 27.0, 5.4), faces(all=sol("y0")), head, (0, 24, 0))
    for sx in (-1, 1):
        x0 = 4 if sx > 0 else -9
        r.add((x0, 21, -2.6), (x0 + 5, 25, 2.6), faces(top=g(["hhhhg", "hgggf", "ggggf", "gggff", "Gfffe"],
                                                              FIG_KEY), all=g(["hhhhh", "ggggg", "fffff", "GeeeG"],
                                                                              FIG_KEY)))
    r.limb((-8, 12, -2), (-4, 24, 2), faces(all=PLATE_ARM_C), (-6, 22, 0), pitch=34, roll=10)
    r.add((-13.5, 11.5, -5.5), (-6.5, 18.5, 1.5), faces(front=SC.HELM_FRONT, back=SC.HELM_BACK,
                                                        left=SC.HELM_SIDE, right=SC.HELM_SIDE, top=SC.HELM_TOP),
          B.rot(yaw=-20, roll=8), (-10, 15, -2))
    r.limb((4, 12, -2), (8, 24, 2), faces(all=PLATE_ARM_C), (6, 22, 0), pitch=6, roll=-4)
    r.add((4.6, 1.5, -1.2), (5.6, 13, 0.0), faces(all=sol("K1")), B.rot(roll=-14, pitch=-10), (5, 13, 0))
    r.add((4.1, 12.5, -1.8), (6.1, 13.5, 0.6), faces(all=sol("G1")), B.rot(roll=-14, pitch=-10), (5, 13, 0))
    return r.boxes


DAY_CAM = dict(f=560, xv=480, yv=205, eye=(-6.4, 5.0, 0.8))
DAY_SUN_DIR = tuple(np.array([0.55, 0.70, -0.45]) / np.linalg.norm([0.55, 0.70, -0.45]))


def day_figures(w):
    """the turncoats hold every crossing: a white-and-gilt banner on the west parapet of each, two of their
    squires at its near end facing the south bank; Maud and the commander on the south bank watching"""
    figs = []
    z0, z1 = RIVER
    for i, bx in enumerate(BRIDGES):
        figs.append(SW.Figure(banner_pole((bx + 0.5, 2.0, z0 + 1.5), TURN_CLOTH, h=3.2, w=0.9, yaw=4.0 - 3 * i,
                                          sway=-3.0)))
        figs.append(SW.Figure(squire_standing((bx + 1.25, 1.0, z0 - 0.45), yaw=8.0 - 4 * i, coat=SQ_COAT_TURN,
                                              team="turn", head_yaw=-10.0 + 9 * i)))
        figs.append(SW.Figure(squire_standing((bx + 2.75, 1.0, z0 + 0.35), yaw=-14.0 + 6 * i, coat=SQ_COAT_TURN,
                                              team="turn", spear=(i != 1), head_yaw=12.0)))
    figs.append(SW.Figure(maud_standing((-0.4, 0.0, 13.4), yaw=200.0, head_yaw=-26.0)))
    figs.append(SW.Figure(commander((1.3, 0.0, 12.6), yaw=194.0)))
    return figs


def day_sky_fn(cam, k):
    clouds = (52.0, 9.0, 13, "#f4f6f8", "#d3dae3", 260.0, 0.60)
    return lambda sx, sy: sky_at(cam, sx, sy, DAY_BANDS, 30.0, clouds, None, 0.8)


DAY_LOOK = dict(amb=(0.46, 0.52, 0.64), sun=(1.02, 0.95, 0.82), warm=(0.30, 0.20, 0.08), fog="#c6d3df",
                layers=[(30.0, 0.0), (60.0, 0.08), (100.0, 0.18), (1e9, 0.30)], d0=0.004, hs=0.9, warm_curve=2.0,
                sky_mix=0.70, mirror_mix=(0.58, 0.42), warm_q=4000, warm_band=None, fire_fall=1.2,
                lantern_fall=2.2, level_band=0.6)


def render_day(k=1.0, cache=None, name="stage-crossings-day", look=None):
    lk = dict(DAY_LOOK, **(look or {}))
    R = trace_scene(river_world, day_figures, DAY_CAM, DAY_SUN_DIR, day_sky_fn, k, cache, fire=None,
                    mirror_light=((0.46, 0.52, 0.64), (0.30, 0.20, 0.08)))
    c = R["cam"]
    cam = B.Camera(c["f"] * k, c["xv"] * k, c["yv"] * k, c["eye"])
    return compose(R, day_sky_fn(cam, k), lk, name)


# ================================================================================================ thumbnails
# the storybook's plates: a small still of each stage scene, traced natively at the plate's size (128 x 72 GUI px)
# from a camera set closer in than the scene's own, so the figures stay readable on the page
THUMB_DAY_CAM = dict(f=90, xv=52, yv=30, eye=(1.0, 2.6, 7.0))
THUMB_DUSK_CAM = dict(f=100, xv=64, yv=30, eye=(-0.9, 1.9, 3.6))


def thumb_dusk_sky_fn(cam, k):
    sun = ((18.0, cam.yv - 1.0), 4, "#fff3c8", "#ffd88a",
           ((15.0, "#f2b064", 0.10), (10.0, "#f6bf6e", 0.16), (6.5, "#fbd38a", 0.26)))
    clouds = (52.0, 9.0, 11, "#6c4058", "#e0905e", 260.0, 0.60)
    return lambda sx, sy: sky_at(cam, sx, sy, DUSK_BANDS, 13.5, clouds, sun, 0.85)


def render_thumbs(which=("day", "dusk"), day_cam=None, dusk_cam=None):
    out = {}
    if "day" in which:
        cd = day_cam or THUMB_DAY_CAM
        R = trace_scene(river_world, day_figures, cd, DAY_SUN_DIR, day_sky_fn, 1.0, None, W=128, H=72,
                        fire=None, mirror_light=((0.46, 0.52, 0.64), (0.30, 0.20, 0.08)))
        cam = B.Camera(cd["f"], cd["xv"], cd["yv"], cd["eye"])
        out["day"] = compose(R, day_sky_fn(cam, 1.0), DAY_LOOK, "th-day")
    if "dusk" in which:
        cd = dusk_cam or THUMB_DUSK_CAM
        R = trace_scene(dusk_world, dusk_figures, cd, DUSK_SUN_DIR, thumb_dusk_sky_fn, 1.0, None, W=128, H=72,
                        fire=(FIRE[0] + 0.5, 0.4, FIRE[1] + 0.5))
        cam = B.Camera(cd["f"], cd["xv"], cd["yv"], cd["eye"])
        out["dusk"] = compose(R, thumb_dusk_sky_fn(cam, 1.0), DUSK_LOOK, "th-dusk")
    return out


if __name__ == "__main__":
    args = sys.argv[1:]
    k = 0.5 if "--half" in args else 1.0
    cache = None
    for a_ in args:
        if a_.startswith("--cache="):
            cache = a_.split("=", 1)[1]
    if "dusk" in args:
        render_dusk(k, cache and cache + "/dusk%s.pkl" % ("-half" if k < 1 else ""),
                    "stage-crossing" + ("-half" if k < 1 else ""))
    if "thumbs" in args:
        render_thumbs()
    if "day" in args:
        render_day(k, cache and cache + "/day%s.pkl" % ("-half" if k < 1 else ""),
                   "stage-crossings-day" + ("-half" if k < 1 else ""))
