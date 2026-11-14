"""The Moot Hound's GeckoLib rig (animation rebuild, v2): the hand art (art/hand/wm_hound) re-hung on a quadruped skeleton.

    root > body (rump) > chest > head > jaw, ears, fangs and head gear
    body > tail, rump gear;  chest > shoulder gear, saddle
    chest > front_<side>_leg > front_<side>_shin > front_<side>_foot     (shoulder, forearm, paw)
    body  > back_<side>_leg  > back_<side>_shin  > back_<side>_foot      (thigh, hock, paw)

Every source box keeps its absolute place, so the painted texture is the hand art's unchanged: each leg's three boxes (upper,
lower, paw) go one to a bone, and the tier II wraps ride the shin. troop_rig.Rig does the textures and the geo.

    python art/anim/wm_hound/build_rig.py [out dir]
"""
import copy, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.normpath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ART, "anim", "common"))
sys.path.insert(0, os.path.join(ART, "hand", "wm_hound"))
import troop_rig as TR                                      # noqa: E402
from handlib import Bone, SIDES                             # noqa: E402
from build_wm_hound import WM_HOUND                         # noqa: E402

LEGS = [f"{e}_{s}" for e in ("front", "back") for s in ("right", "left")]
SHIN, FOOT = 3, 6          # the lower box and the paw sit 3 and 6 px down the leg (handlib y-down)


def rigify(src):
    for bn in src:   # the fangs show at the troop's tier
        if getattr(bn, "weapon_tiers", None) and not bn.stages:
            bn.stages = list(bn.weapon_tiers)
        # the hand art's rest pitch is GeckoLib's opposite (its head carried nose down, its tail swept back): Rig.geo writes
        # -x, so the pitch goes in negated to come out as the hand art draws it
        bn.rot = [-bn.rot[0], bn.rot[1], bn.rot[2]]
    ab = TR.absolute(src)
    by = {bn.name: bn for bn in src}
    out, at = [], {}

    def add(bn, parent, apiv):
        at[bn.name] = list(apiv)
        bn.parent = parent
        bn.pivot = [apiv[i] - at[parent][i] for i in range(3)] if parent else list(apiv)
        out.append(bn)
        return bn

    def moved(box, frm, to):   # the same box, its origin taken from one pivot to another (absolute place unchanged)
        b = copy.copy(box)
        b.origin = [box.origin[i] + frm[i] - to[i] for i in range(3)]
        return b

    add(Bone("root", ""), "", (0, 24, 0))
    PARENT = {"back_right_leg": "body", "back_left_leg": "body"}
    legs = {}
    for bn in src:
        if bn.name == "root":
            continue
        if bn.name.endswith("_wrap"):
            continue   # placed on the shin below
        par = PARENT.get(bn.name, bn.parent)
        if par == "root" and bn.name != "body":
            par = "body"
        if bn.name in [l + "_leg" for l in LEGS]:
            hip = ab[bn.name]
            up, lo, paw = bn.boxes
            bn.boxes = [up]
            add(bn, par, hip)
            stem = bn.name[:-4]
            sh = [hip[0], hip[1] + SHIN, hip[2]]
            ft = [hip[0], hip[1] + FOOT, hip[2]]
            add(Bone(stem + "_shin", bn.name, boxes=[moved(lo, hip, sh)]), bn.name, sh)
            add(Bone(stem + "_foot", stem + "_shin", boxes=[moved(paw, hip, ft)]), stem + "_shin", ft)
            legs[stem] = (hip, sh)
            continue
        add(bn, par, ab[bn.name])
    for side in ("right", "left"):
        for w, stem in ((f"{side}_wrap", f"front_{side}"), (f"{side}_back_wrap", f"back_{side}")):
            bn = by[w]
            hip, sh = legs[stem]
            bn.boxes = [moved(b, ab[w], sh) for b in bn.boxes]
            add(bn, stem + "_shin", sh)
    order, seen, kids = [], set(), {}
    for bn in out:
        kids.setdefault(bn.parent, []).append(bn)

    def walk(p):
        for bn in kids.get(p, []):
            if bn.name in seen:
                continue
            seen.add(bn.name); order.append(bn); walk(bn.name)
    walk("")
    assert len(order) == len(out), "orphans"
    return order


class HoundRig(TR.Rig):
    def __init__(s):
        s.kind, s.tid, s.troop, s.weapon = "held", "wm_hound", WM_HOUND, None
        s.skin, s.cloth = WM_HOUND.skin, WM_HOUND.cloth
        s.wy = s.skin[1] + s.cloth[1]
        s.B = rigify(WM_HOUND.bones())
        s.ground = 24   # the floor's handlib y, as troop_rig.Rig (geo y is ground - y)
        s.tiers = tuple(WM_HOUND.tiers)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "out")
    os.makedirs(os.path.join(out, "preview"), exist_ok=True)
    R = HoundRig(); R.textures()
    json.dump(R.geo(), open(os.path.join(out, "wm_hound.geo.json"), "w"), indent=1)
    json.dump(R.meta(), open(os.path.join(out, "wm_hound.rig.json"), "w"), indent=1)
    for t in R.tiers:
        for side in SIDES:
            R.texture(t, side).save(os.path.join(out, f"wm_hound_t{t}_{side}.png"))
        json.dump(R.geo(lambda bn: TR.shows(bn, t)), open(os.path.join(out, "preview", f"t{t}.geo.json"), "w"))
    print(f"wm_hound: {len(R.B)} bones, texture {R.skin[0]}x{R.height}, {os.path.normpath(out)}")


if __name__ == "__main__":
    main()
