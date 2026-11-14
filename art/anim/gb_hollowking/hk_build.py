"""The Hollow King's whole clip chain, in its order (2026-11-14). Run this, not the steps by hand:

    python art/anim/gb_hollowking/hk_build.py

  1. GG_ANIM_TROOP=gb_hollowking art/anim/knight/build_anims.py   -> out/gb_hollowking.animation.json (54 clips, polished
                                                                       BUILT, hk_polish.py)
  2. art/anim/common/export.py gb_hollowking                       -> copies out/ over the exported asset (geo, rig, textures,
                                                                       clips): the asset now has only the 54 built clips
  3. art/anim/common/stances.py gb_hollowking                      -> adds stance_rally_upper, stance_hold_*, stance_fallback_*
  4. art/anim/common/reactions.py gb_hollowking                    -> adds stun_*, stun_out_upper, hit_knockback*, hit_pulled*
  5. art/anim/common/charge.py gb_hollowking                       -> adds charge_run_upper
  6. art/anim/gb_hollowking/hk_polish.py                           -> polishes those 13 LATE clips of the asset in place

Steps 3-5 build from the exported asset and merge only their own clips into it; they never write out/. So out/ holds the
54 built clips and the asset 67: the 54 equal to out/ byte for byte, plus the 13 late ones. Running export.py alone (step 2
without 3-6) drops the 13 late clips from the asset; that is the loss the model hand-back of 2026-11-13 saw. This script
runs the whole chain and then checks that every shared clip is equal and no clip is missing.
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
TROOP = "gb_hollowking"
OUT = HERE / "out" / f"{TROOP}.animation.json"
ASSET = ROOT / "src/main/resources/assets/gravesandglory/animations/troop" / f"{TROOP}.animation.json"
LATE = {"stance_rally_upper", "stance_hold_upper", "stance_hold_legs", "stance_fallback_upper", "stance_fallback_legs",
        "stun_upper", "stun_legs", "stun_out_upper", "hit_knockback", "hit_knockback_legs", "hit_pulled",
        "hit_pulled_legs", "charge_run_upper"}
STEPS = [["art/anim/knight/build_anims.py"], ["art/anim/common/export.py", TROOP], ["art/anim/common/stances.py", TROOP],
         ["art/anim/common/reactions.py", TROOP], ["art/anim/common/charge.py", TROOP], ["art/anim/gb_hollowking/hk_polish.py"]]


def check():
    out = json.loads(OUT.read_text(encoding="utf-8"))["animations"]
    asset = json.loads(ASSET.read_text(encoding="utf-8"))["animations"]
    bad = [c for c in out if c not in asset or json.dumps(out[c], sort_keys=True) != json.dumps(asset[c], sort_keys=True)]
    missing = sorted((set(out) | LATE) - set(asset))
    extra = sorted(set(asset) - set(out) - LATE)
    print("out/ %d clips, asset %d clips; shared clips that differ: %s; missing from the asset: %s; unexpected: %s"
          % (len(out), len(asset), bad or "none", missing or "none", extra or "none"))
    return not bad and not missing and not extra


def main():
    env = dict(os.environ, GG_ANIM_TROOP=TROOP, PYTHONUNBUFFERED="1")
    for step in STEPS:
        print(">>", " ".join(step), flush=True)
        subprocess.run([sys.executable] + step, cwd=ROOT, env=env, check=True)
    sys.exit(0 if check() else 1)


if __name__ == "__main__":
    if sys.argv[1:] == ["--check"]:
        sys.exit(0 if check() else 1)
    main()
