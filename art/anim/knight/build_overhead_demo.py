"""The knight's overhead cut, authored whole-body with art/anim/common/fullbody.py (the troop-animation skill's demo).

python art/anim/knight/build_overhead_demo.py [--out file]
    merges attack_overhead and attack_overhead_legs into the knight's exported animation file (the way
    art/anim/common/reactions.py --only merges its clips), or into --out. Every other clip in the file stays as it is.
    Rebuilding the knight from build_anims.py and its later layers puts the old cut back: run this last.

Gameplay is untouched: 22 ticks, impact marker on tick 10, starts and ends on the guard. Only the knight's own two clips
change; build_anims.py and every other troop are as they were.

The move, in the reference reel's phases (docs/anim-rebuild/STYLE.md):
  0-2.2    anticipation  weight goes back onto the rear foot, the blade starts up, the shield comes in to the chest
  2.0-4.6  gather        the lead foot draws back a pixel under the body as the pelvis coils and sinks
  4.8-7.6  load          the blade stands high over the sword shoulder and keeps drifting up; the chest and pelvis
                         rise a quarter pixel (a breath in) and drop into the step, the shield trails: no frozen tick
  7.0-9.6  step          the lead foot lunges 5.4 px and plants just before the blow
  7.6-10   strike        the pelvis drives forward and drops, the torso uncoils, the blade comes down edge first
  10-12.5  follow        the cut carries on through, low and across the body; the shield swings out as the counterweight
  11.8-21  recover       the rear foot catches up, the blade lifts, both feet walk back
  18.5-22  settle        eases onto the exact guard
"""
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "common"))
import fullbody  # noqa: E402

B = fullbody.B
G_HAND, G_DIR, _, _ = fullbody.guard_targets()
G_BLADE = (-39.116, 3.059, 0.0)      # the guard's blade as (pitch, yaw, lean); checked against G_DIR below
T = B.ATTACK_TICKS

OVERHEAD = {
    "ticks": T, "impact": B.IMPACT["overhead"],
    "hips": [(0, *B.READY_HIPS),
             (2.2, (-3, 13, 1), (0.6, -1.8, 1.2)),
             (4.8, (-6, 19, 2), (1.0, -2.6, 2.3)),
             (6.2, (-7, 20.5, 2), (1.0, -2.3, 2.45)),      # a breath in at the top of the hold, then the drop
             (7.6, (-7, 21, 2), (1.0, -3.0, 2.5)),
             (10, (7, -8, -1), (-0.5, -2.9, -2.4)),
             (12.5, (8, -12, -2), (-0.6, -3.1, -2.9)),
             (14.8, (5, -6, 0), (-0.3, -2.5, -2.2)),
             (18.5, (1, 3, 0), (0.1, -1.6, -0.8)),
             (T, *B.READY_HIPS)],
    "steps": [("right", 2.0, 4.6, (-3.6, 2.0, 1.0), -11.0, 0.9),
              ("right", 7.0, 9.6, (-4.0, 2.0, 6.4), -8.0),
              ("left", 11.8, 14.2, (3.2, 2.0, 1.0), 6.0, 1.0),
              ("right", 15.0, 18.4, (-3.4, 2.0, 2.0), -12.0),
              ("left", 17.8, 20.8, (3.2, 2.0, -2.0), 7.0)],
    "torso": [(0, B.GUARD["torso"]),
              (2.2, (0, 20, 1)),
              (4.8, (-9, 30, 3)),
              (6.2, (-12.5, 32.5, 3.5)),
              (7.6, (-12, 34, 3)),
              (8.8, (0, 16, 0)),
              (10, (15, -10, -2)),
              (12.5, (19, -22, -3)),
              (14.8, (13, -14, -2)),
              (18.5, (7, 8, 0)),
              (T, B.GUARD["torso"])],
    "head": [(0, (0, 0, 0)), (4.8, (-5, 0, 0)), (7.6, (-5, 0, 0)), (10, (-4, 0, 0)), (12.5, (-6, 3, 0)), (14.8, (-4, 0, 0)), (18.5, (0, 0, 0)),
             (T, (0, 0, 0))],
    "reach": [(0, None),
              (2.2, (-5.6, 0.4, 4.6)),
              (4.8, (-5.6, 3.6, 1.6)),
              (7.6, (-5.6, 3.9, 0.9)),
              (9.0, (-5.8, 3.6, 3.9)),
              (10, (-2.6, 1.2, 6.6)),
              (11.0, (-2.2, -2.8, 5.8)),
              (12.5, (-2.4, -4.4, 4.2)),
              (14.8, (-2.8, -3.2, 4.8)),
              (18.5, (-2.9, -3.0, 5.9)),
              (T, None)],
    "blade": [(0, G_BLADE),
              (2.2, (-62, 0, -6)),
              (4.8, (-118, 0, -20)),
              (7.6, (-130, 0, -24)),
              (9.0, (-78, 0, -8)),
              (10, (8, 3, 0)),
              (11.0, (32, 6, 0)),
              (12.5, (42, 8, 0)),
              (14.8, (22, 6, 0)),
              (18.5, (-24, 5, -5)),
              (T, G_BLADE)],
    "fist": [(0, B.FIST_GUARD),
             (2.2, (4.7, 19.7, 5.5)),
             (4.8, (4.3, 20.3, 5.3)),
             (6.2, (4.4, 20.7, 5.2)),
             (7.6, (4.4, 20.8, 5.0)),
             (10, (7.4, 18.0, 3.8)),
             (12.5, (8.4, 16.6, 3.2)),
             (14.8, (7.2, 17.9, 3.8)),
             (18.5, (5.4, 18.8, 5.2)),
             (T, B.FIST_GUARD)],
    "throw": [(0, (0, 0, 0)), (4.8, (-6, 4, 4)), (7.6, (-6, 4, 4)), (10, (0, -7, -4)), (12.5, (4, -10, -5)),
              (18.5, (0, 0, 0)), (T, (0, 0, 0))],
}


def main():
    import numpy as np
    assert np.allclose(fullbody.blade_dir(*G_BLADE), G_DIR, atol=2e-3), "G_BLADE no longer matches the guard"
    out = HERE.parents[2] / "src/main/resources/assets/gravesandglory/animations/troop/knight.animation.json"
    if "--out" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--out") + 1])
    data = json.loads(out.read_text(encoding="utf-8"))
    new = fullbody.clips("attack_overhead", OVERHEAD)
    for name, clip in new.items():
        assert name in data["animations"], name
        data["animations"][name] = clip
    out.write_text(json.dumps(data, indent=1), encoding="utf-8")
    print("replaced", ", ".join(new), "in", out)


if __name__ == "__main__":
    main()
