"""The Web Stalker's GeckoLib rig (animation rebuild, v2): the hand art (art/hand/wm_stalker) re-hung for the spider's clips.

    root > body > thorax > head > mask, palps, fangs (by tier), mandibles, crest
                  thorax > abdomen > sacs, cords, team cloth, plates, beads, trophy;  thorax > carapace, ridge plates
           body > <side>_leg_<i> (femur, its guard and spur) > <side>_leg_<i>_low (tibia and claw)

`body` is a bare bone at the thorax pivot: the *_legs clips key it (the body's height, pitch and roll) with the sixteen leg
bones, so every leg is solved against it; the *_upper, attack and hit clips key the thorax (a rear or a twist on top), head,
fangs, palps, mandibles and abdomen. The legs hang from `body`, not the thorax, so an upper-body swing played over the walk
never drags a planted claw. Every source box keeps its absolute place, so the painted texture is the hand art's unchanged.

    python art/anim/wm_stalker/build_rig.py [out dir]
"""
import json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ART = os.path.normpath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ART, "anim", "common"))
sys.path.insert(0, os.path.join(ART, "hand", "wm_stalker"))
import troop_rig as TR                                      # noqa: E402
from handlib import Bone, SIDES                             # noqa: E402
from build_wm_stalker import WM_STALKER                     # noqa: E402

LEGS = [f"{s}_leg_{i}" for s in ("left", "right") for i in (1, 2, 3, 4)]


def rigify(src):
    for bn in src:
        if getattr(bn, "weapon_tiers", None) and not bn.stages:   # the fangs and mandibles show at the troop's tier
            bn.stages = list(bn.weapon_tiers)
        # the hand art's rest pitch and roll are GeckoLib's opposite (the hound's pitch was too): Rig.geo writes -x and -z, so
        # they go in negated to come out as the hand art draws them (the legs splayed down to the floor, the abdomen tipped up)
        bn.rot = [-bn.rot[0], bn.rot[1], -bn.rot[2]]
    ab = TR.absolute(src)
    out, at = [], {}

    def add(bn, parent, apiv):
        at[bn.name] = list(apiv)
        bn.parent = parent
        bn.pivot = [apiv[i] - at[parent][i] for i in range(3)] if parent else list(apiv)
        out.append(bn)

    add(Bone("root", ""), "", (0, 24, 0))
    add(Bone("body", "root"), "root", ab["thorax"])
    for bn in src:
        if bn.name == "root":
            continue
        par = bn.parent
        if bn.name == "thorax" or bn.name in LEGS:
            par = "body"
        add(bn, par, ab[bn.name])
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


class StalkerRig(TR.Rig):
    def __init__(s):
        s.kind, s.tid, s.troop, s.weapon = "held", "wm_stalker", WM_STALKER, None
        s.skin, s.cloth = WM_STALKER.skin, WM_STALKER.cloth
        s.wy = s.skin[1] + s.cloth[1]
        s.B = rigify(WM_STALKER.bones())
        s.ground = 24
        s.tiers = tuple(WM_STALKER.tiers)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "out")
    os.makedirs(os.path.join(out, "preview"), exist_ok=True)
    R = StalkerRig(); R.textures()
    json.dump(R.geo(), open(os.path.join(out, "wm_stalker.geo.json"), "w"), indent=1)
    json.dump(R.settle_sidecar, open(os.path.join(out, "wm_stalker.settle.json"), "w"), indent=1)
    json.dump(R.meta(), open(os.path.join(out, "wm_stalker.rig.json"), "w"), indent=1)
    for t in R.tiers:
        for side in SIDES:
            R.texture(t, side).save(os.path.join(out, f"wm_stalker_t{t}_{side}.png"))
        json.dump(R.geo(lambda bn: TR.shows(bn, t)), open(os.path.join(out, "preview", f"t{t}.geo.json"), "w"))
    print(f"wm_stalker: {len(R.B)} bones, texture {R.skin[0]}x{R.height}, {os.path.normpath(out)}")


if __name__ == "__main__":
    main()
