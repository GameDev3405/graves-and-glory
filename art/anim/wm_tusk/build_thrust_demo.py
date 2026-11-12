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

What the checks say (focus_check, 2026-11-11, after Kort's round 2 grades): 0 FAIL, 4 WARN (weapon_in_body 0.79
t1.25, limb_in_body 1.86 t1.75, floor 0.68 t10, armour_clip 1.87 t9); phases: peak 15 px/tick at 6.5, lands at 86
percent; grip err max 1.09 px. Against round 2: the point reaches z 48.8 (was 46.6) while the pelvis goes 3.0 px
forward of the guard (was 5.2); the rear wrist reaches z 11.3 (was 10.3); strides 3.0 and 2.0 px (were 5.8 and 3.9).
Round 2 against round 1: the rear wrist z 10.3 (was 9.0), the fists 7.6 px apart in the guard (was 5.9). Scorecard
still fails pen_limb (2.0, limit 1.39): the guard stance's left arm in the chest (1.6 on tick 0), the spear builder's
to fix.

Fix round 1 (work/review/wm_tusk-fix-r1.md): a sink-and-sit-back tell, an in-between key at 6.0 (the point halfway to
the contact on tick 6, the rear arm moving one way through 5-7), a 2 px carry past the contact, a moving hold, a faster
rip, a breath in the tail, and the left-arm prior LP_OUT (TUSK_LP to experiment) that keeps the left elbow out of the
chest between the guard ends.

Phases:
  0-1.2     tell          the hips sink 1.6 px and sit back over the rear foot, the point dips at the foe
  1.2-5.0   load          the pelvis stays low over the rear foot and opens; the shaft is drawn back, the
                          front hand riding back along it; eyes stay on the foe
  5.0-6.0   start         the load held; the hips begin to square up, the lead foot is already in the air
  6.0-7     drive         most of the travel: the tip gains rise to tick 6.5 and land at 86 percent of the peak
                          (fix round 2); the contact is 5.3 px short of full reach; the lead foot lands as the point arrives
  7-8.8     carry         the point keeps going 5.3 px past the contact (fast to 7.5, then creeping) on the arms:
                          the rear arm straightens, the chest goes over the lead knee
  8.8-10.3  hold          the weight keeps sinking and the chest lifts back off the overshoot; the rear foot catches up
                          (a 2 px step; the lead foot's was 3)
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
# Kort, tusk round 2: "a little bit less body movement and more arms / thrusting the spear. right now he takes a couple
# steps forward and then a couple back, i like the steps but i feel like the strides are too long, shorten them up but
# extend the weapons reach."
#   strides  the lead foot steps 3.0 px (was 5.8), the rear foot 2.0 (was 3.9); the pelvis travels 4.2 px from the load
#            to full reach (was 7.0).
#   reach    the point goes 2.2 px FURTHER than round 2 with the body 2.2 px less forward: 45.8 px ahead of the pelvis
#            at full reach (was 41.4). It comes from the arms, and round 2 measured where they lost it: the front arm
#            was straight but spent 6 of its 8 px crossing the chest to a shaft out at the right side (the shoulders are
#            11 px apart and the fists 6, so one arm always crosses). Here:
#              the shoulders  stay 35 degrees side-on at full reach (round 2 squared up to 26), the leading one at the
#                             foe, so the front arm crosses less and the rear shoulder still comes through
#              the rear arm   ends nearly straight (7.9 of 8.1 px; round 2 folded it beside the ribs), its fist 1 px
#                             further out than round 2 and 0.6 px nearer the centre line
#              the rear elbow is carried OUT and level through the drive (the rp prior on keys 7 to 11.3). Without it
#                             the same reach laid the upper arm 2.0 px into the chest (armour_clip FAIL); with it 1.87,
#                             where the guard itself stands at 1.8
#              the draw       is 0.8 px deeper, and two in-between keys (6.5, 11.3) take the fist past the flank a
#                             pixel outside it, both ways
#            Ruled out by measurement: more side-on than this (the rear arm then lies across the chest, FAIL 1.97 to
#            2.0 at 18-21 degrees of chest yaw without the elbow prior; fully side-on loses the rear shoulder), a
#            chest-height thrust (the shaft goes 1.8 px into the torso), and sliding the shaft in the REAR hand (the
#            weapon's grip section is thicker than the shaft there).
LEAD = 5.4         # the lead foot's plant (the guard has it at 2.4)
REAR = -0.2        # where the rear foot catches up to (the guard: -2.2)
G = nudge(SP.GUARD, Ls=GRIP)
RH = SP.READY_HIPS

# torso, head, O (right fist, troop px: -x the troop's right, +z forward), shaft aim(elevation, yaw to the left),
# Ls = px between the fists along the shaft.
LP_OUT = tuple(float(v) for v in os.environ.get("TUSK_LP", "-60,-30,-30,-40").split(","))   # left-arm prior (the solver's null direction picks the elbow); see F1 in the fix-r1 notes
KEYS = [
    (0, G),
    (1.2, KEY((12, 13, 0), (5, 0, 0), (-4.4, 13.6, 2.2), aim(10, 17), Ls=8.3, lp=LP_OUT)),
    (3.0, KEY((9, 25, 0), (1, 3, 0), (-5.0, 15.0, -2.8), aim(6, 20), Ls=11.4, lp=LP_OUT)),
    (5.0, KEY((6, 31, 0), (-1, 5, 0), (-5.5, 16.0, -5.6), aim(3, 22), Ls=13.2, lp=LP_OUT)),
    (5.5, KEY((7, 31, 0), (0, 4.5, 0), (-5.45, 15.85, -5.2), aim(3, 21.5), Ls=13.1, lp=LP_OUT)),
    (6.0, KEY((9, 31, 0), (2, 3.5, 0), (-6.0, 15.6, -4.2), aim(3, 20), Ls=12.3, lp=LP_OUT)),
    (6.5, KEY((13, 29, 0), (5, -1, 0), (-6.1, 15.1, 0.8), aim(2.5, 15), Ls=10.2, lp=LP_OUT)),      # past the flank, a pixel outside it
    (7, KEY((20, 25, 0), (8, -4, 0), (-5.0, 14.3, 6.6), aim(2, 9), Ls=7.4, rp=(-45, 0, 40, -70), lp=LP_OUT)),
    (7.5, KEY((28, 23, 0), (11, -5, 0), (-4.2, 14.2, 10.1), aim(1, 7), Ls=6.5, rp=(-65, 0, 40, -40), lp=LP_OUT)),
    (8.8, KEY((33, 21, 0), (13, -5, 0), (-4.0, 14.0, 12.3), aim(0.5, 6), Ls=6.0, rp=(-75, 0, 40, -25), lp=LP_OUT)),
    (9.7, KEY((31, 21.5, 0), (12, -5, 0), (-4.0, 14.1, 11.8), aim(1, 6.5), Ls=6.1, rp=(-75, 0, 40, -25), lp=LP_OUT)),
    (10.3, KEY((27, 22, 0), (9, -4, 0), (-4.1, 14.2, 10.4), aim(2.5, 8), Ls=6.5, rp=(-65, 0, 40, -40), lp=LP_OUT)),
    (11.3, KEY((22, 20, 0), (7, -3, 0), (-5.1, 14.3, 7.2), aim(6, 11), Ls=7.2, rp=(-45, 0, 40, -70), lp=LP_OUT)),     # the fist comes back OUTSIDE the ribs
    (12.5, KEY((16, 20, 0), (4, -2, 0), (-4.2, 14.2, 3.4), aim(12, 15), Ls=8.0, lp=LP_OUT)),
    (16.0, nudge(G, O=(0.0, 0.3, 0.4), torso=(-1.5, 1.5, 0))),
    (19.3, nudge(G, O=(0.0, 0.25, 0.0), torso=(1.0, -0.5, 0))),
    (T, G)]

# (tick, (pitch, yaw, roll), (x, y, z)): position +x the troop's left, +z BACK.
LEGS = {
    "hips": [(0, *RH),
             (1.2, (-2, 18, 0), (0.1, -2.7, 0.5)),
             (3.0, (-4, 28, -1), (-0.3, -2.9, 1.0)),
             (5.0, (-4, 34, -1.5), (-0.4, -3.0, 1.2)),
             (5.5, (-3.5, 32, -1.2), (-0.35, -2.95, 1.0)),
             (6.0, (-2.5, 29, -1), (-0.3, -2.9, 0.6)),
             (7, (5, 17, 0.5), (0.3, -2.5, -1.5)),
             (8.8, (9, 14, 0.8), (0.4, -2.7, -3.0)),
             (9.7, (8.5, 14.5, 0.6), (0.4, -2.85, -2.8)),
             (10.3, (6.5, 15, 0.4), (0.3, -2.8, -2.4)),
             (12.5, (2.5, 17, 0), (0.1, -2.2, -1.2)),
             (16.5, (0, 21, 0), (0.0, -1.4, 0.3)),
             (19.3, (0.5, 19.5, 0), (0.0, -1.45, 0.15)),
             (T, *RH)],
    "steps": [("left", 4.4, 6.9, (3.6, 2.0, LEAD), 12.0, 1.2),
              ("right", 7.2, 10.0, (-3.1, 2.0, REAR), -8.0, 0.8),
              ("left", 12.6, 15.6, (3.4, 2.0, 2.4), 14.0, 0.9),
              ("right", 15.8, 19.0, (-3.2, 2.0, -2.2), -6.0, 0.7)],
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
