"""The art/troops/<troop>.json stub and tier I skin that tools/build_skins.py wants beside a hand-authored troop.

The hand builder's outputs() replace everything build_skins would generate for the troop; the catalogue still reads
art/troops/<troop>.json (troop, rig, size, density, weapon, hand, portrait) and checks that its listed skins exist.
"""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", "..", ".."))

def write_stub(troop, note):
    """Write art/troops/<tid>.json and <tid>_s1.png from the troop's own build; call before tools/build_skins.py."""
    B, skins, _ = troop.build()
    meta = troop.meta
    stub = {"troop": troop.tid, "note": note, "rig": "biped", "size": list(troop.skin), "density": 1, "bones": [],
            "idleArms": meta.get("idleArms", "hang"), "parts": [],
            "skins": {str(t): "%s_s%d.png" % (troop.tid, t) for t in troop.tiers},
            "weapon": meta["weapon"], "hand": meta.get("hand", "right"), "portrait": meta.get("portrait", "standard")}
    if "hunch" in meta: stub["hunch"] = meta["hunch"]
    folder = os.path.join(REPO, "art", "troops")
    with open(os.path.join(folder, troop.tid + ".json"), "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(stub, indent=1) + "\n")
    for t in troop.tiers:
        skins[t].save(os.path.join(folder, "%s_s%d.png" % (troop.tid, t)))

def main(troop, note, argv):
    if len(argv) > 1 and argv[1] == "install": write_stub(troop, note)
    troop.main(argv)
