"""Warmaw tusk `attack_high_thrust`, re-authored as a skill trial (Sonnet, 2026-11-11). Writes ONLY to
work/sonnet_high_thrust/wm_tusk.animation.json (a copy of the exported file); refuses to write the exported file.

python art/anim/wm_tusk/build_high_thrust_sonnet_test.py [--out file]

Stands on art/anim/spear/build_spear_anims.py (shaft frame, two-hand solver, key format) and art/anim/common/fullbody.py
(soft_motion). Length and impact tick are read from the exported clip, never invented.

What makes it a HIGH thrust (the level thrust keeps the pelvis driving forward and the point flat at 2-3 degrees):
  the load is LOW      the pelvis sinks deeper and the rear fist drops to the hip, the point dips toward the ground
  the drive RISES      the legs spring up as the lead foot lands: the hips go up (not down) through the impact, the rear fist
                       climbs to chest/shoulder height and the shaft tilts up ~30 degrees, so the point arrives at a head
  the body stays tall  the chest leans less than in the level thrust, the head tips back to look up the shaft
  then the point is pulled back down out of the high line before the feet walk home.

Phases (22 ticks, impact on tick 7):
  0-1.2    tell      weight presses onto the lead foot, the point dips
  1.2-5.0  load      pelvis sinks back over the rear foot and opens, rear fist to the hip, front hand rides back along the shaft
  5.0-5.8  start     the hips begin to rise and square up; the lead foot is already in the air
  5.8-7    drive     legs spring, the rear fist climbs to the shoulder, the point lifts onto the high line; the lead foot lands
  7-8.6    carry     hips and chest keep going up and over the lead knee, the head tips back
  8.6-10.5 hold      the weight settles, the rear foot catches up, a small drift
  10.5-14  rip       the point comes back down out of the high line, the chest drops
  14-22    recover   both feet walk home, the guard settles a touch past itself and back
"""
import json
import os
import shutil
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
os.environ["GG_SPEAR_TROOP"] = "wm_tusk"
sys.path.insert(0, str(HERE.parent / "spear"))
sys.path.insert(0, str(HERE.parent / "common"))
import build_spear_anims as SP  # noqa: E402
import fullbody  # noqa: E402

K, np = SP.K, SP.np
SUB, TPS = SP.SUB, SP.TPS
KEY, aim, nudge = SP.KEY, SP.aim, SP.nudge
G = SP.GUARD
RH = SP.READY_HIPS

EXPORTED = ROOT / "src/main/resources/assets/gravesandglory/animations/troop/wm_tusk.animation.json"
DEFAULT_OUT = ROOT / "work/sonnet_high_thrust/wm_tusk.animation.json"
NAME = "attack_high_thrust"

_ex = json.loads(EXPORTED.read_text(encoding="utf-8"))["animations"][NAME]
T = int(round(_ex["animation_length"] * TPS))                       # 22
IMPACT = int(round(float(next(k for k, v in _ex["timeline"].items() if v == "impact")) * TPS))   # 7
assert (T, IMPACT) == (22, 7), (T, IMPACT)

# torso, head, O (right fist, troop px: -x the troop's right, +z forward), shaft aim(elevation, yaw to the left),
# Ls = px between the fists along the shaft.
KEYS = [
    (0, G),
    (1.2, KEY((15, 13, 0), (5, 0, 0), (-4.5, 13.4, 2.4), aim(8, 17), Ls=6.2)),
    (3.0, KEY((9, 25, 0), (1, 3, 0), (-6.0, 12.8, -2.6), aim(0, 20), Ls=10.5)),
    (5.0, KEY((6, 31, 0), (-2, 5, 0), (-6.2, 12.0, -4.6), aim(-5, 22), Ls=12.0)),
    (5.8, KEY((8, 27, 0), (-3, 3, 0), (-5.8, 13.0, -3.4), aim(4, 21), Ls=11.2)),
    (7, KEY((15, 13, 0), (-9, 0, 0), (-4.9, 17.0, 6.6), aim(22, 20), Ls=5.6)),
    (8.6, KEY((19, 11, 0), (-11, 0, 0), (-5.0, 17.8, 8.6), aim(22, 20), Ls=5.6)),
    (10.5, KEY((17, 13, 0), (-9, 0, 0), (-5.0, 17.2, 7.8), aim(21, 20), Ls=5.6)),
    (13.5, KEY((14, 12, 0), (-1, 0, 0), (-4.4, 15.0, 4.2), aim(14, 16), Ls=6.4)),
    (17.5, nudge(G, O=(0.0, 0.3, 0.4), torso=(-1.5, 1.5, 0))),
    (T, G)]

# (tick, (pitch, yaw, roll), (x, y, z)): position +x the troop's left, +z BACK.
LEGS = {
    "hips": [(0, *RH),
             (1.2, (1, 18, 0), (0.1, -1.7, -0.3)),
             (3.0, (-3, 28, -1), (-0.4, -2.8, 0.9)),
             (5.0, (-5, 34, -1.5), (-0.6, -3.6, 1.5)),
             (5.8, (-3, 28, -1), (-0.3, -3.3, 0.8)),
             (7, (3, 7, 1), (0.3, -1.9, -2.1)),
             (8.6, (5, 4, 1), (0.4, -1.8, -2.5)),
             (10.5, (4, 6, 0.5), (0.3, -1.8, -2.5)),
             (13.5, (2, 12, 0), (0.1, -2.0, -1.4)),
             (17.5, (0, 21, 0), (0.0, -1.4, 0.3)),
             (T, *RH)],
    "steps": [("left", 4.4, 6.9, (3.6, 2.0, 6.6), 12.0, 1.5),
              ("right", 7.8, 10.8, (-3.1, 2.0, 0.6), -8.0, 0.9),
              ("left", 13.2, 16.6, (3.4, 2.0, 2.4), 14.0, 1.0),
              ("right", 16.2, 19.8, (-3.2, 2.0, -2.2), -6.0, 0.8)],
}
SETTLE = 3.0   # ticks at the end over which the arms ease onto the first sample's own solution


def thrust():
    legs = K.attack_legs(LEGS)
    sol = SP.Solver()
    out, hits = [], 0
    for i in range(T * SUB + 1):
        s = SP.sample(KEYS, legs[i], sol, i / SUB)
        hits += bool(SP.shaft_hits(SP.fk_pose(s, legs[i])))
        out.append(s)
    n = T * SUB
    for i in range(n - int(SETTLE * SUB), n + 1):
        k = K.smooth((i - (n - SETTLE * SUB)) / (SETTLE * SUB))
        for b in ("right_arm", "right_forearm", "right_hand", "left_arm", "left_forearm", "left_hand"):
            a, e = np.array(out[i][b][0], float), np.array(out[0][b][0], float)
            e = a + (e - a + 180.0) % 360.0 - 180.0
            out[i][b] = (tuple(float(v) for v in a + (e - a) * k), None)
    print("high thrust: grip err max %.3f px, left fist err max %.3f px, samples with the shaft in the body %d"
          % (sol.worst["right"], sol.worst["left"], hits))
    return out, legs


def clips():
    upper, legs = thrust()
    upper, legs = fullbody.soft_motion(upper, legs, report=True)
    clip = K.bake(upper, T, loop=False)
    clip["timeline"] = {K.fmt_t(IMPACT / TPS): "impact"}
    return {NAME: clip, NAME + "_legs": K.bake(legs, T, loop=False)}


def main():
    out = DEFAULT_OUT
    if "--out" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--out") + 1])
    assert out.resolve() != EXPORTED.resolve(), "this test builder never writes the exported file"
    assert out.resolve() == DEFAULT_OUT.resolve(), "this test builder writes only work/sonnet_high_thrust/wm_tusk.animation.json"
    if not out.is_file():
        out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(EXPORTED, out)
    data = json.loads(out.read_text(encoding="utf-8"))
    new = clips()
    for name, clip in new.items():
        assert name in data["animations"], name
        data["animations"][name] = clip
    out.write_text(json.dumps(data, indent=1), encoding="utf-8")
    print("replaced", ", ".join(new), "in", out)


if __name__ == "__main__":
    main()
