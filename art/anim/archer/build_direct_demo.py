"""Thin runner for the archer's direct shot (art/anim/archer/direct_shot.py, Kort's approved round 3 on the 22-tick loose).

python art/anim/archer/build_direct_demo.py [--out file]
    writes attack_direct and attack_direct_legs, as the bow builder builds them for the archer, into a COPY of the
    archer's exported animation file (default work/archer_direct/archer.animation.json, made from the exported file when
    missing), to look at the pair without a full rebuild. It never writes the exported file: the normal build
    (python art/anim/archer/build_bow_anims.py) takes the pair from direct_shot.py itself.
"""
import json
import shutil
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(HERE))
import build_bow_anims as BW  # noqa: E402,F401  (GG_BOW_TROOP unset: the archer)
import direct_shot  # noqa: E402


def main():
    exported = ROOT / "src/main/resources/assets/gravesandglory/animations/troop/archer.animation.json"
    out = ROOT / "work/archer_direct/archer.animation.json"
    if "--out" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--out") + 1])
    assert out.resolve() != exported.resolve(), "this runner never writes the exported file; build_bow_anims.py does"
    if not out.is_file():
        out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(exported, out)
    data = json.loads(out.read_text(encoding="utf-8"))
    new = BW.only_rig(direct_shot.clips())
    for name, clip in new.items():
        assert name in data["animations"], name
        data["animations"][name] = clip
    out.write_text(json.dumps(data, indent=1), encoding="utf-8")
    print("replaced", ", ".join(new), "in", out)


if __name__ == "__main__":
    main()
