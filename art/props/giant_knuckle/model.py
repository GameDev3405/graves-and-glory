"""Giant's Knuckle (`giant_knuckle`): 3x2x2 cells, anchor cell (1,0,0). Brief: art/props/giant_knuckle/brief.md.

Frame (art/props/README.md): model px, 16 = 1 block, origin = min corner of the anchor cell, x east, y up, z south,
FRONT FACES NORTH (-z). The footprint runs x -16..32, y 0..32, z 0..32 px. One part (nothing above y 32).

The knuckle end of a giant's finger bone breaking the ground: one fat round condyle knob (an octagonal drum with a boss
on each face) half sunk in the earth, and the bone's shaft snapped off short, a thick octagonal stub rising east out of
the knob with a long splinter running on past the break along its top. The rest of the shaft lies in the moss.
Build:  python art/props/build_props.py giant_knuckle      Card:  python tools/prop_card.py giant_knuckle
"""
import os, sys, math
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import E, Prop, Mat, MAT, make_ramp, fixed, h3, HOUSE   # noqa: F401
from giant_bones import bone, bone_chain, octa_chain, mound, octagon, relight, MARROW_FACE, BREAK_FACE

K, KR = (0, 13), 12     # the condyle knob's centre (x, y) and inradius: its lowest edge sits on the floor
ZC = 16                 # the bone's mid-plane (z)


def build():
    B = bone(moss_h=7, seed=12)
    els = []
    # the knob: one round condyle, a boss on each face
    els += octagon(K, KR, "z", ZC - 12, ZC + 12, B, name="condyle")
    els += octagon(K, 7, "z", ZC - 15, ZC - 12, B, name="condyle boss")
    els += octagon(K, 7, "z", ZC + 12, ZC + 15, B, name="condyle boss")
    # the shaft: a broken octagonal stub leaving the knob 67.5 degrees off upright, its end the marrow of the break,
    # and a thin splinter of its upper wall running on past the break
    stub, _ = octa_chain((2, 13), [(67.5, 22, 12)], ZC, B, name="shaft stub", end_faces={"east": MARROW_FACE})
    els += stub
    sp, _ = bone_chain((0.5, 16.7), [(67.5, 28)], 4, ZC - 3, ZC + 3, B, name="splinter", end_faces={"east": BREAK_FACE})
    els += sp
    # the rest of the shaft, lying in the moss with its break toward the stub
    els.append(E((14, 0, 20), (28, 7, 26), bone(moss_h=2, seed=13), skip="down", name="fallen shaft",
                 rot=("y", -22.5, (14, 0, 20)), faces={"west": BREAK_FACE}))
    # earth heaped round the knob
    els += mound((-16, 0, 0), (32, 6, 32), layers=2, seed=15, name="earth mound")
    return Prop("giant_knuckle", {"giant_knuckle": relight(els)})
