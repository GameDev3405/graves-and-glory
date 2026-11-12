"""Rebuilds a knight-builder troop's whole clip file in out/, so export.py ships exactly what the troop's review approved.

GG_ANIM_TROOP=<troop> python art/anim/knight/rebuild_out.py                      # the whole file (sentinel, gl_marshal)
GG_ANIM_TROOP=<troop> python art/anim/knight/rebuild_out.py --only CLIP[,CLIP]   # only these late-layer clips, over out/
then: python art/anim/common/export.py <troop>   (knight: python art/anim/knight/export.py A)

Why (shield family review, 2026-11-12): build_anims.py writes out/<troop>.animation.json, but the shipped file has more in it.
stances.py, charge.py and reactions.py merge their clips into the EXPORTED file, soft_layers.py post-processes those in place,
and the D3 deaths were spliced in by the death polish (docs/anim-rebuild/POLISH-DEATHS.md). out/ never saw any of it, so
export.py would have dropped or reverted those clips. This runs every step, the same code, in the order the shipped files were
made, and writes the result to out/ (export.py then copies it forward, as for every troop):

  1 build_anims.py  every clip but the deaths (the same calls as its main(), in the same order)
  2 splice          the twelve death_* clips, unchanged, from DEATHS_AT:art/anim/<troop>/out/<troop>.animation.json. The
                    builder no longer reproduces them (the death polish built them over rounds of deaths.py constants, then
                    heldprop.py re-keyed the wrists; a fresh build plus heldprop.py differs, sentinel measured 2026-11-12).
  3 stances.py      stance_rally_upper, stance_fallback_upper, stance_fallback_legs
  4 reactions.py    stun_upper, stun_legs, stun_out_upper, hit_knockback, hit_knockback_legs
  5 charge.py       charge_run_upper
  6 reactions.py    hit_pulled, hit_pulled_legs
  7 soft_layers.py  (troops in its LAYERS) the soft pieces and footing fixes on the step 4 and 6 clips

Each merge puts its clips after the last stance_ clip, so this order also reproduces the shipped clip order. soft_layers.py
adds to what reactions.py built, so steps 3-7 never run twice over one file: the whole-file mode always starts from step 1.
--only runs just the steps that build the named clips (and step 7) over the existing out/ file; it is how the knight, whose
out/ predates the batch clips, carries hit_pulled (KNIGHT-REVIEW.md, "Batch review 2026-11-11: hit_pulled").
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
sys.path.insert(0, str(HERE.parent / "common"))
TROOP = os.environ.get("GG_ANIM_TROOP", "knight")
OUT = (HERE if TROOP == "knight" else HERE.parent / TROOP) / "out" / f"{TROOP}.animation.json"
DEATHS_AT = "8cb35120"      # "Polish v2 death clips": the commit that wrote the shipped death clips
LATE = (
    ("stances", ("stance_rally_upper", "stance_fallback_upper", "stance_fallback_legs")),
    ("reactions", ("stun_upper", "stun_legs", "stun_out_upper", "hit_knockback", "hit_knockback_legs")),
    ("charge", ("charge_run_upper",)),
    ("reactions", ("hit_pulled", "hit_pulled_legs")),
)
SOFT = {"stun_upper", "stun_out_upper", "hit_knockback", "hit_knockback_legs", "hit_pulled", "hit_pulled_legs"}


def write(doc, path):
    with open(path, "w", newline="\n") as fh:      # as stances.merge and soft_layers.py write it
        fh.write(json.dumps(doc, indent=1))


def base(path):
    """Steps 1 and 2: the builder's clips with the committed deaths spliced in, written to path."""
    sys.path.insert(0, str(HERE))
    import build_anims as B
    if B.HK:
        sys.exit("rebuild_out.py: the Undying King adds hk_rise clips; not covered here")
    rel = OUT.relative_to(ROOT).as_posix()
    old = json.loads(subprocess.run(["git", "show", f"{DEATHS_AT}:{rel}"], cwd=ROOT, check=True, capture_output=True,
                                    text=True, encoding="utf-8").stdout)["animations"]
    deaths = {k: v for k, v in old.items() if k.startswith("death_")}
    if len(deaths) != 12:
        sys.exit(f"rebuild_out.py: {DEATHS_AT}:{rel} has {len(deaths)} death clips, expected 12")
    idle_legs, idle_upper = B.idle_clips()
    anims = {"walk_legs": B.gait_legs(B.WALK), "walk_upper": B.gait_upper(B.WALK, B.WALK_CARRY),
             "run_legs": B.gait_legs(B.RUN), "run_upper": B.gait_upper(B.RUN, B.RUN_CARRY),
             "idle_legs": idle_legs, "idle_upper": idle_upper, **B.attack_clips(), **B.stance_clips(), **B.hit_clips(),
             **deaths}
    path.parent.mkdir(parents=True, exist_ok=True)
    write({"format_version": "1.8.0", "animations": anims}, path)
    print(f"base: {len(anims) - len(deaths)} built clips + {len(deaths)} deaths from {DEATHS_AT} -> {path}", flush=True)


def late(path, only):
    """Steps 3 to 7 over path, in place."""
    import stances
    for modname, names in LATE:
        want = [n for n in names if only is None or n in only]
        if not want:
            continue
        mod = __import__(modname)
        tr = stances.Troop(TROOP, path)
        clips = mod.build_all(tr)
        missing = [n for n in want if n not in clips]
        if missing:
            sys.exit(f"rebuild_out.py: {modname}.py builds no {missing} for {TROOP}")
        stances.merge(tr, {n: clips[n] for n in want}, path)
        print(f"{modname}: {', '.join(want)} -> {path}", flush=True)
    if only is None or only & SOFT:
        subprocess.run([sys.executable, str(HERE / "soft_layers.py"), "--anim", str(path)],
                       env=dict(os.environ, GG_ANIM_TROOP=TROOP), check=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", help="comma list of late-layer clips: rebuild only their steps over the existing out/ file")
    ap.add_argument("--anim", default=str(OUT), help="the file to write (default: the troop's out/ file; another path for a "
                                                     "scratch check, which --only then extends in place)")
    a = ap.parse_args()
    path = Path(a.anim)
    only = set(a.only.split(",")) if a.only else None
    if only is not None:
        unknown = only - {n for _, names in LATE for n in names}
        if unknown:
            sys.exit(f"rebuild_out.py: --only takes late-layer clips; not {sorted(unknown)}")
    else:
        base(path)
    late(path, only)


if __name__ == "__main__":
    main()
