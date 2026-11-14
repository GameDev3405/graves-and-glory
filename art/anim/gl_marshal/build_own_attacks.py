"""Lord Marshal Aldric's own three attacks, authored whole-body with art/anim/common/fullbody.py (Kort 2026-11-13: "Own
moves for all three"). Until now the Marshal swung the knight's choreography refitted to his rig.

python art/anim/gl_marshal/build_own_attacks.py [--out file]
    merges the six clips into a COPY of the Marshal's exported animation file (default
    work/ownmoves/gl_marshal.animation.json, made from the exported file when missing). It never writes the exported file.
    The shipped clips come from art/anim/knight/rebuild_out.py, step 8, which imports clips() from here.

Gameplay is untouched: 22 ticks each, "impact" on ticks 7 (thrust), 10 (overhead) and 8 (low cut), as the exported clips
have them; every clip starts and ends on the Marshal's guard; at the impact the point or edge is where the old clip put it
along the facing axis (within 1.5 px), at the height of the zone the blow hits.

Character (rule 6). The knight is a drilled soldier: he coils, drops and drives. The Marshal is a veteran commander and a
master swordsman: upright, economical, long in the reach, the shortest cooldown of the family. His moves are a
fencer's: the body stays tall, the feet do the travelling, the arm finishes the line, and the cloak and plates show the
speed.

attack_thrust: a passing lunge (impact 7, body).
  0-1.5    tell        the point drops level onto the foe, the weight eases back
  0.9-2.8  set         the rear foot draws back half a step and turns out, the chest turns side-on (sword shoulder to
                       the foe), the shield comes in; the hand draws back along the line, point on the foe
  2.8-5.9  hold        the line is held (the load, three ticks); a breath in, the pelvis sits back over the rear foot
  5.9-7.0  lunge       the arm leads, then the lead foot shoots 7 px forward; the rear leg straightens, the pelvis drops,
                       the shield arm sweeps out wide to the side as the counterweight (well clear of the chest); the
                       point arrives level at the body on 7
  7.0-8.0  carry       the arm locks out, 2 px past the contact
  8.0-9.6  hold        the lunge is held, the pelvis still sinking a little
  9.6-12.2 recover     the lead foot pushes him back, the point comes up to guard height, the chest squares
  12.5-22  settle      the rear foot and the lead foot each walk home, the guard breathes in and settles

attack_overhead: a high guard at the ear, then a descending diagonal cleave with a pass (impact 10, head).
  (Fix round 1, 2026-11-13. The brief asked for a roof guard: hilt above and in front of the helm, blade back over the
  shield shoulder. On this rig it cannot be done cleanly: the sword shoulder is at y 21 and the arm reaches 8.1 px, so
  the hand tops out near y 29 while the helm and crown reach y 33-37; any blade back over the shield shoulder then
  passes behind the head, which is the family's own wind-up. The nearest clean thing is below.)
  0-2      anticipate  the hand rises forward and up to the sword-side cheek, the blade turning up with the wrist (no
                       roll); the rear foot settles back and out, the weight starts back
  2-4.5    raise       high guard at the ear: hand beside the sword-side cheek, blade standing upright beside the helm;
                       the shield drawn in to the chest; the chest and pelvis coil toward the sword side
  4.5-7.8  hold        the weight sits back and down over the rear foot (pelvis 2 px over toward it), the coil keeps
                       growing: the load
  7.0-9.9  pass        the rear foot passes forward 9.5 px and plants just before the blow
  7.8-10   cut         the blade tips out over the sword side and comes down on the diagonal, edge first, from high on
                       the sword side; at 10 it is 36 degrees below level, still falling and travelling to the shield side
  10-13.5  follow      on through and across: the chest turns through (40 degrees past square), the hand crosses to the
                       centre and the blade finishes low OUTSIDE the lead (shield-side) knee; the shield swings up and
                       out of its way
  13.5-15  finish      he straightens tall over the passed foot
  14-22    recover     the passed foot walks back, the blade lifts, the guard settles

attack_low_cut: an offline backhand (impact 8, legs).
  0-1.5    tell        the hips start to turn toward the shield side, the shield tucks to the chest
  0.8-4.0  offline     the shield-side foot steps out and forward, off the line; the hips turn with it
  1.2-4.6  chamber     the sword crosses over the tucked shield and stands on the shield side, blade up
  4.6-5.6  hold        the chamber is held and keeps winding
  5.6-8    cut         the body drops onto the stepped foot and the blade comes down and out from the shield side,
                       low, travelling outward across the target's legs
  8-10.5   follow      on out to the sword side, the hand low and carried wide of the hip (the long grip clears the
                       chest); the shield stays tucked; he rises out of the cut (an upright commander, not a crouch)
  10.5-22  recover     the sword-side foot comes in, the shield-side foot steps home, the blade lifts to guard
"""
import json
import os
import shutil
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
os.environ.setdefault("GG_ANIM_TROOP", "gl_marshal")
assert os.environ["GG_ANIM_TROOP"] == "gl_marshal", "build_own_attacks.py builds the Marshal only"
sys.path.insert(0, str(HERE.parent / "common"))
import fullbody  # noqa: E402

B = fullbody.B
T = B.ATTACK_TICKS
G_BLADE = (-39.116, 3.059, 0.0)      # the guard's blade as (pitch, yaw, lean); checked against the guard in clips()
G_HIPS = B.READY_HIPS
GT = B.GUARD["torso"]                # (5, 14, 0)
FG = B.FIST_GUARD                    # (5, 19, 5.6)

THRUST = {
    "ticks": T, "impact": B.IMPACT["thrust"],
    "hips": [(0, *G_HIPS),
             (1.5, (0, 2, 0), (0.2, -1.4, 0.4)),
             (2.8, (0, -8, 0), (0.6, -1.8, 1.0)),
             (5.0, (0, -10, 0), (0.6, -2.0, 1.2)),
             (5.9, (0, -11, 0), (0.6, -2.1, 1.3)),
             (7.0, (2, -18, 0), (-0.2, -3.4, -2.6)),
             (8.0, (2, -20, 0), (-0.3, -3.6, -3.0)),
             (9.6, (2, -19, 0), (-0.3, -3.8, -3.1)),
             (12.2, (1, -6, 0), (0.0, -2.0, 0.0)),
             (15.5, (0, 2, 0), (0.1, -1.4, 0.4)),
             (18.5, (0, 5, 0), (0.0, -1.0, 0.0)),
             (T, *G_HIPS)],
    "steps": [("left", 0.9, 2.8, (3.4, 2.0, -4.2), 22.0, 1.0),
              ("right", 5.6, 7.0, (-3.8, 2.0, 9.0), -6.0, 1.6),
              ("right", 9.6, 12.2, (-3.6, 2.0, 3.2), -10.0, 1.2),
              ("left", 12.6, 15.4, (3.2, 2.0, -2.0), 7.0, 1.0),
              ("right", 15.8, 18.6, (-3.4, 2.0, 2.0), -12.0, 0.8)],
    "torso": [(0, GT),
              (1.5, (3, 6, 0)),
              (2.8, (2, -14, 0)),
              (5.0, (1, -16, 0)),
              (7.0, (1, -24, 0)),
              (8.0, (0, -27, 0)),
              (9.6, (1, -25, 0)),
              (12.2, (3, -6, 0)),
              (15.5, (4, 10, 0)),
              (18.5, (5, 15, 0)),
              (T, GT)],
    "head": [(0, (0, 0, 0)), (1.5, (0, -2, 0)), (2.8, (0, 8, 0)), (5.0, (0, 10, 0)), (7.0, (2, 18, 0)), (8.0, (2, 20, 0)),
             (9.6, (2, 19, 0)), (12.2, (0, 6, 0)), (15.5, (0, -2, 0)), (T, (0, 0, 0))],
    "reach": [(0, None),
              (1.5, (-3.0, -3.2, 4.4)),
              (2.8, (-1.0, -3.8, 2.6)),
              (5.0, (-0.8, -4.0, 2.4)),
              (5.9, (-0.7, -3.9, 2.7)),
              (6.5, (0.3, -2.6, 4.2)),
              (7.0, (0.6, -0.4, 5.2)),
              (8.0, (0.7, -0.4, 7.7)),
              (9.6, (0.6, -0.6, 7.4)),
              (12.2, (-2.6, -2.2, 5.6)),
              (15.5, (-3.6, -2.6, 5.4)),
              (T, None)],
    "blade": [(0, G_BLADE),
              (1.5, (-19, 4, 0)),
              (2.8, (-6, 5, 0)),
              (5.0, (-1, 5, 0)),
              (5.9, (0, 5, 0)),
              (7.0, (9, 6, 0)),
              (8.0, (10, 5, 0)),
              (9.6, (8, 5, 0)),
              (12.2, (-16, 4, 0)),
              (15.5, (-32, 3, 0)),
              (T, G_BLADE)],
    "fist": [(0, FG),
             (1.5, (4.6, 19.4, 5.0)),
             (2.8, (4.2, 19.8, 4.4)),
             (5.0, (4.6, 19.6, 4.0)),
             (6.5, (8.2, 18.4, 2.0)),
             (7.0, (10.4, 18.4, 1.2)),
             (8.0, (11.2, 17.8, 0.8)),
             (9.6, (10.8, 18.0, 1.0)),
             (11.2, (9.6, 19.0, 1.6)),
             (12.6, (7.4, 19.0, 4.0)),
             (15.5, (5.2, 19.0, 5.2)),
             (T, FG)],
    "throw": [(0, (0, 0, 0)), (2.8, (-3, 2, 2)), (5.0, (-3, 2, 2)), (7.0, (2, -8, -6)), (8.5, (3, -10, -6)),
              (12.2, (0, -2, -1)), (16, (0, 0, 0)), (T, (0, 0, 0))],
}

OVERHEAD = {
    "ticks": T, "impact": B.IMPACT["overhead"],
    "elbow": [(0, (0,)), (10.0, (0,)), (11.0, (-14,)), (12.5, (-14,)), (16, (0,)), (T, (0,))],
    "hips": [(0, *G_HIPS),
             (2.0, (-1, 13, 0), (0.6, -1.8, 1.2)),
             (4.5, (-2, 18, 1), (2.2, -3.0, 1.8)),
             (6.5, (-2, 19, 1), (2.3, -3.0, 1.9)),
             (7.8, (-2, 20, 1), (2.3, -3.1, 1.9)),
             (10, (3, 12, 0), (-0.4, -1.4, -4.4)),
             (11.0, (3, 10, 0), (-0.5, -1.4, -4.9)),
             (12.5, (2, 8, 0), (-0.6, -1.4, -5.0)),
             (15.0, (1, 8, 0), (-0.4, -1.0, -4.0)),
             (18.0, (0, 6, 0), (-0.1, -1.0, -1.0)),
             (T, *G_HIPS)],
    "steps": [("left", 1.2, 3.8, (3.8, 2.0, -2.9), 10.0, 0.8),
              ("left", 6.8, 9.9, (2.4, 2.0, 6.8), 10.0, 0.8),
              ("left", 14.2, 17.6, (3.2, 2.0, -2.0), 7.0, 1.2),
              ("right", 17.4, 20.2, (-3.4, 2.0, 2.0), -12.0, 0.6)],
    "torso": [(0, GT),
              (2.0, (2, 18, 0)),
              (4.5, (-2, 21, -2)),
              (6.5, (-3, 23, -2)),
              (8.0, (-3, 24, -2)),
              (9.0, (2, 6, 0)),
              (10, (8, -30, 1)),
              (11.2, (8, -44, 2)),
              (12.5, (7, -48, 2)),
              (14.5, (3, -36, 1)),
              (17.5, (3, 2, 0)),
              (T, GT)],
    "head": [(0, (0, 0, 0)), (4.5, (-2, -4, 0)), (8.0, (-2, -4, 0)), (10, (-2, 4, 0)), (12.5, (-4, 6, 0)),
             (15.0, (-2, 2, 0)), (T, (0, 0, 0))],
    "reach": [(0, None),
              (2.0, (-4.8, 3.0, 4.6)),
              (4.5, (-5.6, 4.0, 3.0)),
              (6.5, (-5.6, 4.2, 2.9)),
              (8.0, (-5.6, 4.3, 2.8)),
              (8.7, (-5.2, 4.4, 3.8)),
              (9.3, (-4.3, 4.2, 5.3)),
              (10, (-1.6, 4.2, 6.2)),
              (10.7, (0.2, 0.2, 6.4)),
              (11.5, (1.8, -2.2, 5.6)),
              (12.5, (2.8, -3.6, 4.6)),
              (13.5, (2.8, -3.8, 4.4)),
              (15.0, (1.0, -3.4, 4.6)),
              (18.0, (-3.0, -2.8, 5.8)),
              (T, None)],
    "blade": [(0, G_BLADE),
              (2.0, (-70, 2, -4)),
              (4.5, (-96, 0, -6)),
              (6.5, (-100, 0, -6)),
              (8.0, (-102, 0, -6)),
              (8.7, (-86, 0, -18)),
              (9.3, (-50, 2, -24)),
              (10, (36, 12, 0)),
              (10.7, (47, 26, 0)),
              (11.5, (43, 38, 0)),
              (12.5, (38, 48, 0)),
              (13.5, (36, 54, 0)),
              (15.0, (20, 36, 0)),
              (18.0, (-20, 12, -4)),
              (T, G_BLADE)],
    "fist": [(0, FG),
             (2.0, (4.4, 19.4, 4.6)),
             (4.5, (4.8, 19.8, 4.4)),
             (6.5, (4.7, 20.0, 4.3)),
             (8.0, (4.7, 20.1, 4.2)),
             (9.0, (5.8, 19.9, 4.2)),
             (10, (7.6, 19.6, 2.8)),
             (12.5, (9.6, 19.8, 2.0)),
             (15.0, (8.0, 19.6, 3.4)),
             (18.0, (5.6, 19.4, 5.0)),
             (T, FG)],
    "throw": [(0, (0, 0, 0)), (4.5, (-4, 3, 3)), (8.0, (-4, 3, 3)), (10, (-2, -6, -6)), (12.5, (-2, -10, -8)),
              (15.0, (-2, -6, -4)), (18.5, (0, 0, 0)), (T, (0, 0, 0))],
}

LOW_CUT = {
    "ticks": T, "impact": B.IMPACT["low_cut"],
    "hips": [(0, *G_HIPS),
             (1.5, (2, -4, 0), (0.4, -1.4, 0.0)),
             (4.0, (6, -22, 0), (1.6, -2.2, -1.8)),
             (5.6, (7, -26, 0), (1.8, -2.4, -2.2)),
             (8, (8, -30, -2), (2.4, -2.8, -4.6)),
             (10.5, (4, -24, -2), (2.2, -2.4, -4.8)),
             (13.5, (3, -14, 0), (1.4, -1.8, -2.6)),
             (17.0, (2, 0, 0), (0.4, -1.4, -0.6)),
             (T, *G_HIPS)],
    "steps": [("left", 0.8, 4.0, (6.4, 2.0, 4.6), 13.0, 1.0),
              ("right", 9.6, 12.4, (-2.2, 2.0, 2.8), -14.0, 0.8),
              ("left", 12.8, 16.0, (3.2, 2.0, -2.0), 7.0, 1.2),
              ("right", 16.4, 19.4, (-3.4, 2.0, 2.0), -12.0, 0.6)],
    "torso": [(0, GT),
              (1.5, (4, -6, 0)),
              (4.0, (6, -24, 0)),
              (5.6, (6, -28, 0)),
              (8, (7, -4, 0)),
              (10.5, (3, 24, 0)),
              (13.5, (4, 4, 0)),
              (17.0, (5, 10, 0)),
              (T, GT)],
    "head": [(0, (0, 0, 0)), (4.0, (2, 14, 0)), (5.6, (2, 16, 0)), (8, (6, 20, 0)), (10.5, (4, 16, 0)), (13.5, (2, 10, 0)),
             (17.0, (0, 2, 0)), (T, (0, 0, 0))],
    "reach": [(0, None),
              (1.5, (-3.1, 0.0, 7.0)),
              (2.75, (0.4, -0.2, 7.8)),
              (4.0, (2.6, 1.6, 6.8)),
              (5.6, (3.0, 1.6, 6.8)),
              (7.0, (1.6, -2.4, 7.2)),
              (8, (-0.8, -5.0, 5.8)),
              (9.0, (-5.0, -4.2, 4.0)),
              (10.5, (-6.0, -3.0, 3.4)),
              (13.5, (-4.6, -2.8, 5.6)),
              (17.0, (-4.0, -2.8, 5.6)),
              (T, None)],
    "blade": [(0, G_BLADE),
              (1.5, (-62, 8, 2)),
              (2.75, (-72, 30, 12)),
              (4.0, (-80, 56, 22)),
              (5.6, (-84, 70, 30)),
              (7.0, (-20, 30, 10)),
              (8, (21, 12, 0)),
              (9.0, (22, -22, 0)),
              (10.5, (20, -38, 0)),
              (13.5, (0, -24, 0)),
              (17.0, (-28, -6, -2)),
              (T, G_BLADE)],
    "fist": [(0, FG),
             (1.5, (4.4, 18.6, 4.8)),
             (4.0, (3.8, 18.0, 4.2)),
             (5.6, (3.8, 17.9, 4.1)),
             (8, (4.0, 18.0, 4.0)),
             (10.5, (4.2, 18.2, 4.2)),
             (13.5, (4.6, 18.6, 4.8)),
             (17.0, (5.0, 19.0, 5.4)),
             (T, FG)],
    "throw": [(0, (0, 0, 0)), (4.0, (-2, 2, 2)), (5.6, (-2, 2, 2)), (8, (2, -3, -2)), (10.5, (2, -4, -3)),
              (14, (0, -1, 0)), (T, (0, 0, 0))],
}

PLANS = {"attack_thrust": THRUST, "attack_overhead": OVERHEAD, "attack_low_cut": LOW_CUT}


def soft_parts():
    """The Marshal's hanging pieces with his own tuning (build_anims.SOFT_TUNE), as his family clips get them."""
    parts = fullbody.soft_parts_for()
    for name, tune in B.SOFT_TUNE.get(B.TROOP, {}).items():
        if name in parts:
            parts[name].update(tune)
    return parts


def clips(report=False):
    """{clip name: clip} for the six clips."""
    import numpy as np
    _, g_dir, _, _ = fullbody.guard_targets()
    assert np.allclose(fullbody.blade_dir(*G_BLADE), g_dir, atol=2e-3), "G_BLADE no longer matches the guard"
    out = {}
    for name, plan in PLANS.items():
        spec = dict(plan, soft=soft_parts())
        out.update(fullbody.clips(name, spec, report=report))
    return out


def main():
    out = ROOT / "work" / "ownmoves" / "gl_marshal.animation.json"
    if "--out" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--out") + 1])
    if not out.exists():
        out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(ROOT / "src/main/resources/assets/gravesandglory/animations/troop/gl_marshal.animation.json", out)
    data = json.loads(out.read_text(encoding="utf-8"))
    new = clips(report=True)
    for name, clip in new.items():
        assert name in data["animations"], name
        data["animations"][name] = clip
    with open(out, "w", newline="\n") as fh:
        fh.write(json.dumps(data, indent=1))
    print("replaced", ", ".join(new), "in", out)


if __name__ == "__main__":
    main()
