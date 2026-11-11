"""The knight's thrust, authored whole-body with art/anim/common/fullbody.py (skill test: written from the skill alone).

python art/anim/knight/build_thrust_sonnet_test.py [--out file] [--report]
    Reads the knight's exported animation file (never writes it) and writes a copy to work/sonnet-test/knight.animation.json
    (or --out) with attack_thrust and attack_thrust_legs replaced. Every other clip is as exported.

Gameplay is untouched: 22 ticks (B.ATTACK_TICKS), impact marker on tick 7 (B.IMPACT["thrust"], sheets/design.json
sword_shield_thrust impactTicks 7), starts and ends on the guard.

The move, in the reference reel's phases (docs/anim-rebuild/STYLE.md). The thrust is the light, fast blow: a short load
and the fastest strike of the three, so the impact lands at tick 7.
  0-2.4    anticipation  weight goes back onto the rear foot, the sword hand draws back to the ribs, the shield comes in
  2.4-5.0  load          pelvis coils and sinks, shoulders turn away, the blade lies level aimed at the foe; the hand keeps
                         drifting back a little so the held pose never freezes
  4.6-6.8  step          the lead foot lunges and plants just before the blow
  5.0-7    strike        the pelvis drives forward and drops, the torso uncoils through the target, the hand shoots out
                         along the blade's own line (point leading, level)
  7-9.5    follow        full extension, the point carries on a little past the target, the shield flings out behind
  9.5-13   recover       the rear foot catches up, the arm draws back along the same line
  13-22    settle        both feet walk back, the blade lifts back to the guard and eases onto the exact guard
"""
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "common"))
import fullbody  # noqa: E402

B = fullbody.B
ROOT = HERE.parents[2]
SRC = ROOT / "src/main/resources/assets/gravesandglory/animations/troop/knight.animation.json"
DEFAULT_OUT = ROOT / "work/sonnet-test/knight.animation.json"
G_HAND, G_DIR, _, _ = fullbody.guard_targets()
G_BLADE = (-39.116, 3.059, 0.0)      # the guard's blade as (pitch, yaw, lean); checked against G_DIR below
T = B.ATTACK_TICKS

THRUST = {
    "ticks": T, "impact": B.IMPACT["thrust"],
    "hips": [(0, *B.READY_HIPS),
             (2.4, (-2, 11, 1), (0.4, -1.4, 0.8)),
             (4.4, (-4, 17, 2), (0.7, -1.6, 1.4)),
             (5.2, (-4, 17, 2), (0.7, -1.7, 1.4)),
             (7, (6, -7, -1), (-0.5, -2.7, -1.6)),
             (9.5, (7, -9, -1), (-0.6, -2.9, -2.0)),
             (12.5, (4, -4, 0), (-0.3, -2.4, -1.6)),
             (16.5, (1, 3, 0), (0.1, -1.6, -0.9)),
             (T, *B.READY_HIPS)],
    "steps": [("right", 4.4, 6.8, (-3.8, 2.0, 5.4), -9.0),
              ("left", 8.6, 11.4, (3.2, 2.0, 0.6), 6.0, 1.0),
              ("right", 12.6, 16.4, (-3.4, 2.0, 2.0), -12.0),
              ("left", 16.0, 19.6, (3.2, 2.0, -2.0), 7.0)],
    "torso": [(0, B.GUARD["torso"]),
              (2.4, (0, 16, 1)),
              (5.0, (-4, 26, 2)),
              (5.6, (-3, 24, 2)),
              (7, (14, -14, -1)),
              (9.5, (18, -18, -2)),
              (12.5, (12, -8, -1)),
              (16.5, (7, 6, 0)),
              (T, B.GUARD["torso"])],
    "head": [(0, (0, 0, 0)), (5.0, (-3, 0, 0)), (7, (0, 0, 0)), (9.5, (2, 3, 0)), (16.5, (0, 0, 0)), (T, (0, 0, 0))],
    "reach": [(0, None),
              (1.2, (-5.8, -2.0, 1.8)),
              (2.4, (-5.6, -1.4, 0.8)),
              (5.0, (-6.0, -2.4, -0.6)),
              (5.6, (-6.0, -2.4, -1.0)),
              (7, (-2.6, 0.1, 7.0)),
              (8.2, (-2.4, 0.9, 7.1)),
              (9.5, (-2.2, 0.9, 7.3)),
              (12.5, (-4.2, -0.8, 4.2)),
              (16.0, (-4.2, -1.8, 4.8)),
              (18.6, (-4.9, -2.7, 5.5)),
              (T, None)],
    "blade": [(0, G_BLADE),
              (1.2, (-23, -1, -3)),
              (2.4, (-14, 2, -3)),
              (5.0, (-3, 3, 0)),
              (5.6, (-3, 3, 0)),
              (7, (2, 2, 0)),
              (8.2, (3, 1, 0)),
              (9.5, (5, 0, 0)),
              (12.5, (-8, 2, -2)),
              (16.0, (-24, 4, -2)),
              (18.6, (-33, 3, -1)),
              (T, G_BLADE)],
    "fist": [(0, B.FIST_GUARD),
             (2.4, (5.0, 19.4, 5.2)),
             (5.0, (4.6, 19.8, 5.0)),
             (5.6, (4.6, 19.8, 5.0)),
             (7, (7.0, 17.6, 3.4)),
             (9.5, (8.0, 16.8, 3.0)),
             (12.5, (7.0, 17.8, 3.8)),
             (16.5, (5.6, 18.8, 5.2)),
             (T, B.FIST_GUARD)],
    "throw": [(0, (0, 0, 0)), (5.0, (-5, 4, 3)), (7, (0, -6, -3)), (9.5, (3, -8, -4)), (16.5, (0, 0, 0)), (T, (0, 0, 0))],
}


def main():
    import numpy as np
    assert np.allclose(fullbody.blade_dir(*G_BLADE), G_DIR, atol=2e-3), "G_BLADE no longer matches the guard"
    out = DEFAULT_OUT
    if "--out" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--out") + 1]).resolve()
    assert "src" not in out.relative_to(ROOT).parts[:1] if out.is_relative_to(ROOT) else True, "never write to src/"
    if "--report" in sys.argv:
        fullbody.reach_report(THRUST)
    data = json.loads(SRC.read_text(encoding="utf-8"))
    new = fullbody.clips("attack_thrust", THRUST)
    for name, clip in new.items():
        assert name in data["animations"], name
        data["animations"][name] = clip
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(data, indent=1), encoding="utf-8")
    print("replaced", ", ".join(new), "in", out)


if __name__ == "__main__":
    main()
