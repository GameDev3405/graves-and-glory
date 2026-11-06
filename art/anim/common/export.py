"""Copies a troop rig built by troop_rig.py, and its clips, into the mod's assets for the GeckoLib renderer.

python art/anim/common/export.py <troop>

geo/troop/<troop>.geo.json, troop_rig/<troop>.json, textures/entity/troop/<troop>_geo/t<tier>_<side>.png,
animations/troop/<troop>.animation.json  (as art/anim/knight/export.py does for the knight)
"""
import shutil, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ASSETS = HERE.parents[2] / "src/main/resources/assets/gravesandglory"


def main():
    tid = sys.argv[1]
    src = HERE.parent / tid / "out"
    for need in (f"{tid}.geo.json", f"{tid}.rig.json", f"{tid}.animation.json"):
        if not (src / need).exists():
            sys.exit(f"missing {src / need}: run troop_rig.py {tid} and build_anims first")
    tex = ASSETS / f"textures/entity/troop/{tid}_geo"
    for d in (ASSETS / "geo/troop", ASSETS / "troop_rig", ASSETS / "animations/troop", tex):
        d.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src / f"{tid}.geo.json", ASSETS / f"geo/troop/{tid}.geo.json")
    shutil.copyfile(src / f"{tid}.rig.json", ASSETS / f"troop_rig/{tid}.json")
    shutil.copyfile(src / f"{tid}.animation.json", ASSETS / f"animations/troop/{tid}.animation.json")
    n = 0
    for png in sorted(src.glob(f"{tid}_t*_*.png")):
        shutil.copyfile(png, tex / f"{png.stem[len(tid) + 1:]}.png"); n += 1
    print(f"exported {tid}: geo, rig, {n} textures, clips")


if __name__ == "__main__":
    main()
