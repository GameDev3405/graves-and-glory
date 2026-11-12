"""Rebuilds ic_hearthguard's whole clip file in out/, so export.py ships exactly what the troop's review approved.

python art/anim/axe/rebuild_hearthguard.py                 # writes art/anim/ic_hearthguard/out/ic_hearthguard.animation.json
python art/anim/common/export.py ic_hearthguard            # copies it (with the geo, rig and textures) into the mod's assets
python art/anim/axe/rebuild_hearthguard.py --anim FILE     # the same build into a scratch file (a check)

Why (review fix round 2, 2026-11-12; the shield family's art/anim/knight/rebuild_out.py pattern): build_axe_anims.py writes
out/, but the shipped file has more in it. stances.py, reactions.py and charge.py merged their clips into the EXPORTED file,
the deaths were spliced in by the death polish, and the review's hearthguard_hits.py refitted the hits and reactions there.
out/ never saw any of it, so export.py would have dropped or reverted those clips. This runs every step, the same code, in the
order the shipped file was made:

  1 build_axe_anims.py  every clip but the deaths (the same calls as its main(), in the same order)
  2 splice              the twelve death_* clips, unchanged, from DEATHS_AT (the death polish's commit) of the exported file
  3 stances.py          stance_rally_upper, stance_fallback_upper, stance_fallback_legs
  4 reactions.py        stun_upper, stun_legs, stun_out_upper, hit_knockback, hit_knockback_legs
  5 charge.py           charge_run_upper
  6 reactions.py        hit_pulled, hit_pulled_legs
  7 hearthguard_hits.py the hits, hit_block, stun, stun_out, knockback and pulled clips refitted to the dwarf (in place)
  8 FINISH              this troop's last fixes on run_upper and charge_run_upper (in place)

Steps 3 to 6 merge after the last stance_ clip, which reproduces the shipped clip order; steps 7 and 8 replace clips where
they stand. Every run starts from step 1, so running it twice gives the same bytes.
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
TROOP = "ic_hearthguard"
os.environ["GG_AXE_TROOP"] = TROOP
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parent / "common"))
import hearthguard_hits as H  # noqa: E402  (imports build_axe_anims as A, stances as S, reactions as R)
import hearthguard_finish as F  # noqa: E402

A, S, K = H.A, H.S, H.K
OUT = HERE.parent / TROOP / "out" / f"{TROOP}.animation.json"
ASSET = "src/main/resources/assets/gravesandglory/animations/troop/ic_hearthguard.animation.json"
DEATHS_AT = "8cb35120"      # "Polish v2 death clips": the commit that wrote the shipped death clips
LATE = (
    ("stances", ("stance_rally_upper", "stance_fallback_upper", "stance_fallback_legs")),
    ("reactions", ("stun_upper", "stun_legs", "stun_out_upper", "hit_knockback", "hit_knockback_legs")),
    ("charge", ("charge_run_upper",)),
    ("reactions", ("hit_pulled", "hit_pulled_legs")),
)


def write(doc, path):
    import hang_swing
    hang_swing.key_doc(TROOP, doc, log=False)       # as stances.merge does (no hanging part on this troop: no change)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", newline="\n") as fh:       # as stances.merge writes it
        fh.write(json.dumps(doc, indent=1))


def base(path):
    """Steps 1 and 2."""
    old = json.loads(subprocess.run(["git", "show", f"{DEATHS_AT}:{ASSET}"], cwd=ROOT, check=True, capture_output=True,
                                    text=True, encoding="utf-8").stdout)["animations"]
    deaths = {k: v for k, v in old.items() if k.startswith("death_")}
    if len(deaths) != 12:
        sys.exit(f"rebuild_hearthguard.py: {DEATHS_AT}:{ASSET} has {len(deaths)} death clips, expected 12")
    idle_legs, idle_upper = K.idle_clips()
    anims = {"walk_legs": K.gait_legs(K.WALK), "walk_upper": K.gait_upper(K.WALK, K.WALK_CARRY),
             "run_legs": K.gait_legs(K.RUN), "run_upper": K.gait_upper(K.RUN, K.RUN_CARRY),
             "idle_legs": idle_legs, "idle_upper": idle_upper, **A.attack_clips(), **K.stance_clips(), **K.hit_clips()}
    A.only_rig(anims)
    anims.update(deaths)
    write({"format_version": "1.8.0", "animations": anims}, path)
    print(f"base: {len(anims) - len(deaths)} built clips + {len(deaths)} deaths from {DEATHS_AT}", flush=True)


def late(path):
    """Steps 3 to 6."""
    for modname, names in LATE:
        mod = __import__(modname)
        tr = S.Troop(TROOP, path)
        clips = mod.build_all(tr)
        S.merge(tr, {n: clips[n] for n in names}, path)
        print(f"{modname}: {', '.join(names)}", flush=True)


def in_place(path, clips):
    doc = json.loads(path.read_text())
    missing = [k for k in clips if k not in doc["animations"]]
    if missing:
        sys.exit(f"rebuild_hearthguard.py: no {missing} to replace")
    doc["animations"] = {k: clips.get(k, v) for k, v in doc["animations"].items()}
    write(doc, path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--anim", default=str(OUT), help="the file to write (default: the troop's out/ file)")
    path = Path(ap.parse_args().anim)
    base(path)
    late(path)
    clips, _ = H.build(path=path)                   # step 7
    in_place(path, clips)
    print(f"hearthguard_hits: {', '.join(clips)}", flush=True)
    clips = F.build(path)                           # step 8
    in_place(path, clips)
    print(f"finish: {', '.join(clips)} -> {path}", flush=True)


if __name__ == "__main__":
    main()
