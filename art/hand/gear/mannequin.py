"""The bare commander the gear is shown on: a vanilla-sized biped in a dark padded under-tunic, so every piece reads
against the body it will sit on in game (plan decision D1: gear is drawn over the player's own skin)."""
import os, sys
from PIL import Image
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "common"))
from handlib import Box, Bone, ramp, quilt, leather, rows, face_painter, pack, paint_box, SKINS

TUNIC = ramp("16151a", "24222a", "35313a", "48434c", "5e5862", "77707a")   # dyed-dark wool, house-neutral
HOSE = ramp("121116", "1d1b21", "2a272e", "3a363e", "4c4750")

def skin_hand(face, w, h):
    S = SKINS["fair"]
    return [[S[3 if face in ("front", "right") else 2] for _ in range(w)] for _ in range(h)]

def bones():
    tunic = quilt(TUNIC, base=2, every=2)
    arm = rows([(0, tunic), (9, skin_hand)])
    leg = rows([(0, quilt(HOSE, base=2, every=4)), (9, leather(base=1))])
    B = [Bone("root", "")]
    B.append(Bone("body", "root", (0, 12, 0), [Box((-4, -12, -2), (8, 12, 4), tunic)]))
    B.append(Bone("head", "body", (0, -12, 0), [Box((-4, -8, -4), (8, 8, 8), face_painter(0))]))
    B.append(Bone("right_arm", "body", (-5, -10, 0), [Box((-3, -2, -2), (4, 12, 4), arm)]))
    B.append(Bone("right_hand", "right_arm", (-1, 8, 0)))
    B.append(Bone("left_arm", "body", (5, -10, 0), [Box((-1, -2, -2), (4, 12, 4), arm)]))
    B.append(Bone("left_hand", "left_arm", (1, 8, 0)))
    B.append(Bone("right_leg", "root", (-1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg)]))
    B.append(Bone("left_leg", "root", (1.9, 12, 0), [Box((-2, 0, -2), (4, 12, 4), leg)]))
    pack([b for bn in B for b in bn.boxes], (64, 64))
    return B

def skin():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for bn in bones():
        for b in bn.boxes: paint_box(img, b, b.paint[0])
    return img
