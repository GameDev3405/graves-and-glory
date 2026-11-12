#!/usr/bin/env python3
"""Every pair of parts that sinks into each other in a clip, not only the worst one focus_check's row names.

  python tools/anim/fit_pairs.py --troop knight --clip attack_overhead [--part pauldron] [--from 9 --to 16]
      [--anim work/copy.animation.json] [--top 12] [--ticks] [--same-limb]

For each pair of bones whose cubes overlap: the most the clip adds over the model's own rest fit (px), the tick, and
the depth there. The same measure as focus_check's armour_clip row (focus_check._fits less the rest fit), so a pair
over that row's WARN line (1.7) is the next one the row will name once the worst is fixed. Like the row, it leaves out
pieces of one limb (a shin and its own guard fold over the knee); `--same-limb` lists those too. `--part` keeps the pairs
that name a bone containing that text; `--ticks` prints the pairs per half tick instead of the summary.
"""
from __future__ import annotations

import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)
import focus_check as fc  # noqa: E402
import render_geo as rg  # noqa: E402
import scorecard as sc  # noqa: E402


def pairs(troop, clip, anim_path=None, part="", t0=0.0, t1=1e9, step=0.5, same_limb=False):
    """({pair: (gain px, tick, depth px)}, {tick: {pair: gain px}}) for one clip played over its _legs clip. Pieces of one
    limb (a shin and its own guard) are left out as the armour_clip row leaves them out, unless `same_limb`."""
    geo = "%s/geo/troop/%s.geo.json" % (sc.ASSETS, troop)
    rig = sc.Rig(rg.load_geo(os.path.join(sc.REPO, geo)), sc.rig_rules_for(sc.REPO, geo), troop)
    anim = sc._read(anim_path or os.path.join(sc.REPO, "%s/animations/troop/%s.animation.json" % (sc.ASSETS, troop)))
    top = rg.load_clip(anim, clip)
    layers = [rg.load_clip(anim, clip + "_legs")] if clip + "_legs" in anim.get("animations", {}) else []
    scene = rg.Scene(rig.model, None, layers + [top], 0.0)
    bone_of = rig.model.cube_bone
    P = fc.Parts(rig)
    rest = fc._fits(rg.Scene(rig.model, None, [], 0.0).evaluate(0.0), P, bone_of)
    worst, per_tick = {}, {}
    t = max(0.0, t0)
    while t <= min(t1, top.length - 1e-4):
        row = {}
        for pr, v in fc._fits(scene.evaluate(t), P, bone_of).items():
            if part and part not in pr[0] and part not in pr[1]:
                continue
            la, lb = fc._limb_root(rig.full, pr[0]), fc._limb_root(rig.full, pr[1])
            if not same_limb and la is not None and la == lb:
                continue
            g = v - rest.get(pr, 0.0)
            row[pr] = g
            if g > worst.get(pr, (-1e9, 0.0, 0.0))[0]:
                worst[pr] = (g, t, v)
        per_tick[t] = row
        t += step
    return worst, per_tick


def main(argv=None):
    p = argparse.ArgumentParser(description="Every overlapping pair of parts in a clip, with what the clip adds over the rest fit.")
    p.add_argument("--troop", required=True)
    p.add_argument("--clip", required=True)
    p.add_argument("--anim")
    p.add_argument("--part", default="")
    p.add_argument("--from", dest="t0", type=float, default=0.0)
    p.add_argument("--to", dest="t1", type=float, default=1e9)
    p.add_argument("--top", type=int, default=12)
    p.add_argument("--ticks", action="store_true")
    p.add_argument("--same-limb", action="store_true", help="also list pieces of one limb, which armour_clip does not judge")
    a = p.parse_args(argv)
    worst, per_tick = pairs(a.troop, a.clip, a.anim, a.part, a.t0, a.t1, same_limb=a.same_limb)
    if a.ticks:
        for t, row in per_tick.items():
            best = sorted(row.items(), key=lambda kv: -kv[1])[:4]
            print("t%5.1f  %s" % (t, "; ".join("%s/%s %+.2f" % (pr[0], pr[1], g) for pr, g in best if g > 0.05)))
        return 0
    for pr, (g, t, v) in sorted(worst.items(), key=lambda kv: -kv[1][0])[:a.top]:
        if g > 0.05:
            print("%-20s %-20s adds %5.2f px (depth %.2f) at tick %.1f" % (pr[0], pr[1], g, v, t))
    return 0


if __name__ == "__main__":
    sys.exit(main())
