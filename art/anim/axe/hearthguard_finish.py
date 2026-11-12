"""ic_hearthguard only: the last small fixes on two looping clips, made on the built clips (rebuild_hearthguard.py step 8).

Review fix round 2 (2026-11-12), measured with the focus check over the clip's own base (run_legs):
  run_upper         the shield forearm, bent across the chest, touched the breastplate (2.03 px, t0.25): the shield arm
                    rides 4 deg further out all through the loop (1.88). More puts the shoulder boss in the helm (6 deg:
                    2.02); unbending the forearm instead puts it in the coat (1.99).
  charge_run_upper  charge.py keys the core bones only, so the beard braid hung straight down from the leaning head into the
                    lifted right thigh (1.93 px, t5.5): the braid is hair, so it swings 8 deg forward about its root around
                    that stride only. With the braid clear, the tabard showed on the other thigh (1.92, t1.5): it swings
                    14 deg forward around that stride (1.86), as it does in the attacks' follow-through.
Each row adds a rotation (degrees) to one bone, weighted by a smooth pulse that wraps around the loop (or all through it),
so the loops still close exactly and the clip lengths, loop flags and every other bone stay as they were.
"""
from __future__ import annotations

import json
import math
from pathlib import Path


TPS = 20.0
ALL = None
# clip: [(bone, (dx, dy, dz) deg, (peak tick, half width ticks) or ALL)]
ROWS = {
    "run_upper": [("left_arm", (0.0, 0.0, -4.0), ALL)],
    "charge_run_upper": [("beard_braid", (8.0, 0.0, 0.0), (5.5, 2.5)), ("tabard", (14.0, 0.0, 0.0), (1.5, 3.0))],
}


def _w(t, length, span):
    """The row's weight at t ticks: 1 all through (ALL), or a hann pulse at peak, half width w, wrapped around the loop."""
    if span is None:
        return 1.0
    peak, half = span
    d = abs(t - peak) % length
    d = min(d, length - d)
    return 0.0 if d >= half else 0.5 * (1.0 + math.cos(math.pi * d / half))


def _fmt(x):
    return round(x, 3)


def fix_clip(clip, rows):
    clip = json.loads(json.dumps(clip))
    length = clip["animation_length"] * TPS

    for bone, rot, span in rows:
        ch = clip["bones"].setdefault(bone, {})
        keys = ch.get("rotation")
        if keys is None:                         # an unkeyed bone (the braid): keyed from its rest at the head's key times
            keys = {k: [0.0, 0.0, 0.0] for k in clip["bones"]["head"]["rotation"]}
        out = {}
        for k, v in keys.items():
            w = _w(float(k) * TPS, length, span)
            out[k] = [_fmt(a + w * d) for a, d in zip(v, rot)] if w > 0.0 else v
        ch["rotation"] = out
    return clip


def build(path):
    anims = json.loads(Path(path).read_text())["animations"]
    return {name: fix_clip(anims[name], rows) for name, rows in ROWS.items()}
