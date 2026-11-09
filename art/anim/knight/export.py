"""Copies a built knight rig (out/<option>) into the mod's assets for the GeckoLib renderer.

python art/anim/knight/export.py A|B

geo/troop/knight.geo.json            GeckoLib geometry (scanned by GeckoLib's "geo" loader)
troop_rig/knight.json                bone visibility rules (GeoTroopRenderer reads it; kept out of geo/,
                                     which GeckoLib parses as geometry)
textures/entity/troop/knight_geo/t<tier>_<side>.png
animations/troop/knight.animation.json   clips built by build_anims.py (shared by both rig options)
"""
import shutil, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ASSETS = HERE.parents[2] / "src/main/resources/assets/gravesandglory"


def main():
    opt = sys.argv[1] if len(sys.argv) > 1 else "A"
    src = HERE / "out" / opt
    if not (src / "knight.geo.json").exists():
        sys.exit(f"no build at {src}; run build_rig.py {opt} first")
    (ASSETS / "geo/troop").mkdir(parents=True, exist_ok=True)
    (ASSETS / "troop_rig").mkdir(parents=True, exist_ok=True)
    tex = ASSETS / "textures/entity/troop/knight_geo"
    tex.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src / "knight.geo.json", ASSETS / "geo/troop/knight.geo.json")
    shutil.copyfile(src / "knight.rig.json", ASSETS / "troop_rig/knight.json")
    n = 0
    for png in sorted(src.glob("knight_t*_*.png")):
        tier_side = png.stem[len("knight_"):]
        shutil.copyfile(png, tex / f"{tier_side}.png")
        n += 1
    anim = HERE / "out" / "knight.animation.json"
    if not anim.exists():
        sys.exit("no clips at out/knight.animation.json; run build_anims.py first")
    (ASSETS / "animations/troop").mkdir(parents=True, exist_ok=True)
    shutil.copyfile(anim, ASSETS / "animations/troop/knight.animation.json")
    print(f"exported option {opt}: geo, rig, {n} textures, clips")


if __name__ == "__main__":
    main()
