"""The Warmaw tusk's spear thrust, re-authored for the troop-animation skill's third demo (two hands on one shaft).

python art/anim/wm_tusk/build_thrust_demo.py [--out file]
    merges attack_thrust and attack_thrust_legs into a COPY of the tusk's animation file (default
    work/wm_tusk_thrust/wm_tusk.animation.json, made from the exported file when missing). It never writes the exported
    file by itself: the tusk is the animation lead's troop and this clip is a proposal until Kort grades it.

It stands on art/anim/spear/build_spear_anims.py (imported, not edited): the shaft frame, the two-hand solver and the
keys' format are that builder's. What changes, and why (Kort's 2026-11-11 grades of the knight demo, and focus_check on
the tusk's current thrust: 4 FAIL rows):

  the whole unit  the current clip scales the knight's cape lag onto the kilt and trophy; the tooth necklace, the hip
                  skull and all four shoulder plates never move (secondary_life FAIL). Here every one of them swings
                  on fullbody.soft_motion. "i want the whole unit to be animated including pieces of its armor".
  the load        the current load barely shows from the side: the fist goes back but the point stays put, because the
                  shaft only slides through the front hand. Here the pelvis sits back and down over the rear foot, the
                  chest coils, the front hand rides back with the shaft and the point drops onto the target line.
  drive and carry the current clip stops dead on the impact (phases WARN). Here the hips keep driving for a tick past
                  the contact, the head goes down behind the tusks, and the point is ripped back out before the feet
                  walk home.
  the front arm   at full reach the current clip brings the two fists 2 px apart, which folds the front forearm into the
                  chest and lays it along the shaft (limb_in_body 2.0, weapon_in_body FAIL, armour_clip FAIL). Here the
                  fists never close under 5.9 px wrist to wrist (the two wrist wraps meet at 5.6) and the rear fist
                  stops short of the front one. The rear fist's target at full reach is 1.5 px past what the FRONT arm
                  allows ("grip err" in the build line): the front arm is simply straight there, the shaft stays in
                  both fists.
  the end         the solver's warm start left the arms 3 degrees off the guard on the last sample (settle FAIL). The
                  last ticks ease onto the first sample's own solution.

Gameplay is untouched: 22 ticks, "impact" on tick 7.

What the checks say (focus_check, 2026-11-11, after Kort's round 1 grades): 0 FAIL, 4 WARN (weapon_in_body 0.92
t7.75, limb_in_body 1.79 t7.5, floor 0.67 t10.5, armour_clip 1.81 t3); phases: peak 14 px/tick at 6.5, lands at 84
percent; grip err max 1.48 px. Against round 1: the rear wrist reaches z 10.3 (was 9.0), the fists are 7.6 px apart
in the guard (was 5.9) and 5.9 at full reach (was 5.6). Scorecard still fails pen_limb (limit 1.39): the guard
stance's left arm in the chest, the spear builder's to fix.

Fix round 1 (work/review/wm_tusk-fix-r1.md): a sink-and-sit-back tell, an in-between key at 6.0 (the point halfway to
the contact on tick 6, the rear arm moving one way through 5-7), a 2 px carry past the contact, a moving hold, a faster
rip, a breath in the tail, and the left-arm prior LP_OUT (TUSK_LP to experiment) that keeps the left elbow out of the
chest between the guard ends.

Phases:
  0-1.2     tell          the hips sink 1.6 px and sit back over the rear foot, the point dips at the foe
  1.2-5.0   load          the pelvis stays low over the rear foot and opens; the shaft is drawn back, the
                          front hand riding back along it; eyes stay on the foe
  5.0-6.0   start         the load held; the hips begin to square up, the lead foot is already in the air
  6.0-7     drive         most of the travel: the tip gains rise to tick 6.75 and land at 83 percent of the peak
                          (fix round 2); the contact is 4.5 px short of full reach; the lead foot lands as the point arrives
  7-8.8     carry         the point keeps going 4.5 px past the contact (fast to 7.5, then creeping), the hips and
                          chest go over the lead knee
  8.8-10.3  hold          the weight keeps sinking and the chest lifts back off the overshoot; the rear foot catches up
  10.3-12.5 rip           the point is pulled back out to guard height, the chest comes up
  12.5-22   recover       both feet walk home, the guard settles a touch past itself (16), breathes in (19.3), back
"""
import json
import math
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
T, IMPACT = SP.ATTACK_TICKS, 7
# Kort, tusk round 1: "hand placement could be spaced out a tad bit more" (tick 0, the guard) and "the hand placement
# and lunge could do better. have the unit poke out the spear a tad bit more, not a lot just a little bit."
#   hands   the spear builder's guard holds the fists 6.0 px apart (the wrist wraps meet at 5.6). GRIP is the guard's
#           spacing here: 8.0 along the shaft (7.6 wrist to wrist, round 1 was 5.9).
#   lunge   measured, not assumed (work/tusk_arms.py): at full reach the FRONT arm is the straight one (8.0 of 8.1 px),
#           the rear fist sits bent beside the ribs. So the point goes no further than the front fist lets it, and a
#           wide grip at full reach costs reach px for px (grip 7.4 there: the point gained 0.5 px and the right arm
#           went 2.0 px into the torso, FAIL). What carries the point 1.3 px deeper than round 1:
#             the body      the lead foot lands 1.2 px further out (8.2, the leg's limit) and the pelvis follows 1.1 px
#             the shoulder  the leading (left) shoulder stays 4 degrees further forward and the chest 2 degrees lower
#             the rear fist drives the shaft through the front hand: 8.0 apart in the guard, 6.3 at full reach (5.9 wrist
#                           to wrist; round 1 was 5.6), which is the push of a two-hand thrust and reads as one
# The stance clips still hold the fists 6.0 apart: see the note for the animation lead.
GRIP = 8.0
REACH = 1.1        # px the pelvis and the rear fist's target go deeper than round 1
LEAD = 8.2         # the lead foot's plant; 8.8 is out of the leg's reach
G = nudge(SP.GUARD, Ls=GRIP)
RH = SP.READY_HIPS

# torso, head, O (right fist, troop px: -x the troop's right, +z forward), shaft aim(elevation, yaw to the left),
# Ls = px between the fists along the shaft.
LP_OUT = tuple(float(v) for v in os.environ.get("TUSK_LP", "-60,-30,-30,-40").split(","))   # left-arm prior (the solver's null direction picks the elbow); see F1 in the fix-r1 notes
KEYS = [
    (0, G),
    (1.2, KEY((12, 13, 0), (5, 0, 0), (-4.4, 13.6, 2.2), aim(10, 17), Ls=8.3, lp=LP_OUT)),
    (3.0, KEY((9, 25, 0), (1, 3, 0), (-5.0, 15.0, -2.6), aim(6, 20), Ls=11.2, lp=LP_OUT)),
    (5.0, KEY((6, 31, 0), (-1, 5, 0), (-5.5, 16.0, -4.8), aim(3, 22), Ls=12.4, lp=LP_OUT)),
    (5.5, KEY((7, 30, 0), (0, 4.5, 0), (-5.45, 15.85, -4.4), aim(3, 22), Ls=12.3, lp=LP_OUT)),
    (6.0, KEY((9, 26, 0), (2, 3.5, 0), (-5.42, 15.7, -3.6), aim(3, 21.5), Ls=11.6, lp=LP_OUT)),
    (7, KEY((21.8, 11.6, 0), (8, 0, 0), (-5.0, 13.9, 5.0 + 0.55 * REACH), aim(2, 11.5), Ls=7.56, lp=LP_OUT)),
    (7.5, KEY((31.6, 10.2, 0), (11, 0, 0), (-5.0, 13.5, 8.5 + 0.68 * REACH), aim(1.5, 10.5), Ls=6.72, lp=LP_OUT)),
    (8.8, KEY((35, 8, 0), (13, 0, 0), (-5.0, 13.2, 9.85 + REACH), aim(1, 10), Ls=6.3, lp=LP_OUT)),
    (9.7, KEY((32.5, 10, 0), (12, 0, 0), (-5.0, 13.3, 9.3 + REACH), aim(1.5, 10.5), Ls=6.4, lp=LP_OUT)),
    (10.3, KEY((28.6, 12.2, 0), (9, 0, 0), (-4.9, 13.7, 8.2 + 0.9 * REACH), aim(3, 11), Ls=6.72, lp=LP_OUT)),
    (12.5, KEY((17, 13, 0), (4, 0, 0), (-4.4, 14.0, 3.2 + 0.4 * REACH), aim(12, 16), Ls=8.0, lp=LP_OUT)),
    (16.0, nudge(G, O=(0.0, 0.3, 0.4), torso=(-1.5, 1.5, 0))),
    (19.3, nudge(G, O=(0.0, 0.25, 0.0), torso=(1.0, -0.5, 0))),
    (T, G)]

# (tick, (pitch, yaw, roll), (x, y, z)): position +x the troop's left, +z BACK.
LEGS = {
    "hips": [(0, *RH),
             (1.2, (-2, 18, 0), (0.1, -2.9, 0.7)),
             (3.0, (-4, 28, -1), (-0.4, -3.1, 1.4)),
             (5.0, (-4, 34, -1.5), (-0.6, -3.2, 1.8)),
             (5.5, (-3.5, 32, -1.2), (-0.5, -3.15, 1.5)),
             (6.0, (-2.5, 29, -1), (-0.4, -3.05, 1.0)),
             (7, (6, 8, 1), (0.4, -2.55, -2.4 - 0.55 * REACH)),
             (8.8, (10, 3, 1), (0.5, -2.8, -4.1 - REACH)),
             (9.7, (9, 4, 0.8), (0.5, -3.0, -3.8 - REACH)),
             (10.3, (7, 6, 0.5), (0.4, -2.9, -3.2 - 0.9 * REACH)),
             (12.5, (3, 12, 0), (0.1, -2.3, -1.6 - 0.4 * REACH)),
             (16.5, (0, 21, 0), (0.0, -1.4, 0.3)),
             (19.3, (0.5, 19.5, 0), (0.0, -1.45, 0.15)),
             (T, *RH)],
    "steps": [("left", 4.4, 6.9, (3.6, 2.0, LEAD), 12.0, 1.5),
              ("right", 7.2, 10.4, (-3.1, 2.0, 0.8 + 0.9), -8.0, 0.9),
              ("left", 12.6, 16.2, (3.4, 2.0, 2.4), 14.0, 1.0),
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
    for i in range(n - int(SETTLE * SUB), n + 1):        # same keys, same legs: only the solver's branch differs
        k = K.smooth((i - (n - SETTLE * SUB)) / (SETTLE * SUB))
        for b in ("right_arm", "right_forearm", "right_hand", "left_arm", "left_forearm", "left_hand"):
            a, e = np.array(out[i][b][0], float), np.array(out[0][b][0], float)
            e = a + (e - a + 180.0) % 360.0 - 180.0
            out[i][b] = (tuple(float(v) for v in a + (e - a) * k), None)
    print("thrust: grip err max %.3f px, left fist err max %.3f px, samples with the shaft in the body %d"
          % (sol.worst["right"], sol.worst["left"], hits))
    return out, legs


def clips():
    upper, legs = thrust()
    upper, legs = fullbody.soft_motion(upper, legs, report=True)
    clip = K.bake(upper, T, loop=False)
    clip["timeline"] = {K.fmt_t(IMPACT / TPS): "impact"}
    return {"attack_thrust": clip, "attack_thrust_legs": K.bake(legs, T, loop=False)}


def main():
    exported = ROOT / "src/main/resources/assets/gravesandglory/animations/troop/wm_tusk.animation.json"
    out = ROOT / "work/wm_tusk_thrust/wm_tusk.animation.json"
    if "--out" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--out") + 1])
    assert out.resolve() != exported.resolve(), "this demo never writes the exported file; copy its clips by hand after approval"
    if not out.is_file():
        out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(exported, out)
    data = json.loads(out.read_text(encoding="utf-8"))
    new = clips()
    for name, clip in new.items():
        assert name in data["animations"], name
        data["animations"][name] = clip
    out.write_text(json.dumps(data, indent=1), encoding="utf-8")
    print("replaced", ", ".join(new), "in", out)


if __name__ == "__main__":
    main()
