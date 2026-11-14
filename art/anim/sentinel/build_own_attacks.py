"""The Templar's (sentinel) own three attacks, authored whole-body with art/anim/common/fullbody.py.

Kort, 2026-11-13: "Own moves for all three". Until now the Templar swung the knight's cuts refitted to its rig. The Templar
is the slow, heavy shield-line troop (a long sword and a tall shield, a slower walk and a longer cooldown than the
knight). The knight opens his body and swings wide; the Templar fights from behind the shield and moves its weight, not
its feet. Everything here keeps the gameplay of the family's clips (docs/anim-rebuild/sentinel-REVIEW.md, "Own moves"):

  22 ticks, not looping, the "impact" marker at 7 (thrust), 10 (overhead) and 8 (low cut); the first and last keys are
  the ready guard the stances hand over (fullbody's ends); the point or edge at the impact is where the family's clip
  put it along the facing axis (within 1.5 px) at the height of the zone it hits (body, head, legs).

GG_ANIM_TROOP=sentinel python art/anim/knight/rebuild_out.py   step 8 swaps these six clips into out/ (both modes)
python art/anim/sentinel/build_own_attacks.py [--out file]     alone: merges them into a COPY,
                                                               work/ownmoves/sentinel.animation.json by default

attack_thrust (impact 7, body): a covered stab.
  0-1.8    tell         the weight settles a pixel, the shield fist closes up to cover the chest
  1.8-5.0  draw         the sword hand draws back level beside the shield's right edge (no coil behind the hip); the hips
                        sink and sit back over the rear foot, square to the foe; the hold keeps drifting back
  4.6-6.8  step         a short lead step (2.4 px) as the shield advances: the shield goes first
  5.6-7    stab         the point drives out past the shield's rim, hips square, chest over the lead knee
  7-8      carry        a pixel more on the arm, the chest still going
  8-10.5   snap back    the point is pulled straight back behind the shield, the blade rising to the guard's line
  9-11.5   drag         the rear foot drags up behind the step (the reel's "back foot drags")
  12-22    recover      the lead foot then the rear foot walk home under the guard; a breath in the tail

attack_overhead (impact 10, head): a hammer chop.
  0-2.6    brace        the rear foot steps out wide, the shield sinks low and forward as the anchor
  1.5-6.0  raise        the sword goes straight up beside the helm, upright, not laid back over the shoulder; the chest
                        rises a little behind the shield
  6.0-8.0  hold         the raised blade keeps creeping up (a breath in), everything else braced
  8.0-10   drop         the whole weight drops through both knees (no step): the pelvis sinks 3 px, the chest folds
                        forward and the blade falls in its own vertical plane, edge first
  10-13.8  stop         the cut carries down to waist height in front of the body and stops there, controlled: not swung
                        across the body (the arm decelerates over three ticks: the wrist stays under the scorecard's speed)
  13.8-15  settle       the stop drifts (a pixel and a few degrees), the knees still giving
  15-22    recover      the body rises, the blade lifts to the guard, the wide feet walk back in

attack_low_cut (impact 8, legs): a shield check and reap.
  0-1.5    tell         the hips sink, the shield fist starts forward
  1.5-5.4  check        the shield presses forward and up to open the line (its lower edge lifts off the shins), the body
                        behind it; the sword hand drops low beside the right hip with the blade held upright there
  5.4-8    reap         the lead knee dips (pelvis down 3 px) on a long reaching step and the blade falls forward and
                        down off the hip into a low cut under the shield's lower edge
  8-10.5   carry        the reap carries on across and up, still under the raised shield
  10.5-13  cover        the blade comes back up behind the shield, the shield comes down onto it
  13-22    recover      the feet walk home, the guard settles
"""
import json
import math
import os
import sys
from pathlib import Path

os.environ.setdefault("GG_ANIM_TROOP", "sentinel")
HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(HERE.parent / "common"))
import fullbody  # noqa: E402

B = fullbody.B
assert B.TROOP == "sentinel", "build_own_attacks.py is the Templar's (GG_ANIM_TROOP=sentinel), not %s" % B.TROOP
T = B.ATTACK_TICKS
G_HAND, G_DIR, _, _ = fullbody.guard_targets()
G_BLADE = (-math.degrees(math.asin(G_DIR[1])), math.degrees(math.atan2(G_DIR[0], G_DIR[2])), 0.0)
RH = B.READY_HIPS
FG = B.FIST_GUARD
TG = B.GUARD["torso"]

THRUST = {
    "ticks": T, "impact": B.IMPACT["thrust"],
    "hips": [(0, *RH),
             (1.8, (1, 4, 0), (0.1, -1.9, 0.6)),
             (5.0, (2, 3, 0), (0.0, -2.4, 1.0)),
             (6.0, (4, 2, 0), (-0.1, -2.6, 0.0)),
             (7, (7, 0, 0), (-0.2, -2.9, -2.4)),
             (8, (8, 0, 0), (-0.2, -3.0, -3.8)),
             (10.5, (5, 2, 0), (-0.1, -2.6, -2.2)),
             (12.0, (4, 3, 0), (0.0, -2.4, -1.9)),
             (16.0, (2, 5, 0), (0.0, -1.8, -0.8)),
             (19.0, (0.5, 6, 0), (0.0, -1.3, -0.1)),
             (T, *RH)],
    "steps": [("right", 4.6, 6.8, (-3.8, 2.0, 4.4), -9.0),
              ("left", 9.0, 11.5, (3.4, 2.0, 0.2), 6.0, 0.8),
              ("right", 12.5, 16.0, (-3.4, 2.0, 2.0), -12.0),
              ("left", 16.0, 19.5, (3.2, 2.0, -2.0), 7.0)],
    "torso": [(0, TG),
              (1.8, (4, 14, 0)),
              (5.0, (1, 15, 0)),
              (6.0, (5, 12, 0)),
              (7, (12, 6, 0)),
              (8, (16, 5, 0)),
              (10.5, (9, 10, 0)),
              (12.0, (8, 12, 0)),
              (16.0, (6, 14, 0)),
              (19.0, (5.5, 14, 0)),
              (T, TG)],
    "head": [(0, (0, 0, 0)), (7, (-3, 0, 0)), (8, (-4, 0, 0)), (12, (0, 0, 0)), (T, (0, 0, 0))],
    "reach": [(0, None),
              (1.8, (-3.4, -3.6, 3.2)),
              (5.0, (-3.0, -3.4, -0.6)),
              (6.0, (-2.8, -3.2, 2.2)),
              (7, (-1.8, -3.2, 7.0)),
              (8, (-1.8, -3.1, 6.9)),
              (10.5, (-3.0, -3.5, 3.4)),
              (14.0, None),
              (T, None)],
    "blade": [(0, G_BLADE),
              (1.8, (-8, 2, 0)),
              (5.0, (-4, 3, 0)),
              (6.0, (-2, 4, 0)),
              (7, (4, 5, 0)),
              (8, (3, 5, 0)),
              (10.5, (-22, 4, 0)),
              (14.0, G_BLADE),
              (T, G_BLADE)],
    "fist": [(0, FG),
             (1.8, (4.4, 19.6, 6.0)),
             (5.0, (4.4, 19.8, 6.4)),
             (6.0, (4.6, 19.6, 7.0)),
             (7, (4.8, 19.4, 7.2)),
             (8, (4.8, 19.3, 7.1)),
             (10.5, (4.6, 19.5, 6.6)),
             (16.0, (4.8, 19.2, 5.9)),
             (T, FG)],
    "throw": [(0, (0, 0, 0)), (5.0, (2, 2, 1)), (7, (-3, -2, -1)), (10.5, (1, 1, 0)), (16, (0, 0, 0)), (T, (0, 0, 0))],
}

OVERHEAD = {
    "ticks": T, "impact": B.IMPACT["overhead"],
    "hips": [(0, *RH),
             (2.6, (-1, 4, 0), (-0.2, -1.9, 0.4)),
             (6.0, (-3, 3, 0), (-0.2, -1.7, 0.6)),
             (8.0, (-3, 2, 0), (-0.2, -1.6, 0.5)),
             (10, (7, 0, 0), (-0.3, -3.4, -0.6)),
             (13.8, (8, -1, 0), (-0.3, -3.4, -0.5)),
             (14.8, (7, -1, 0), (-0.3, -3.3, -0.5)),
             (17.0, (5, 2, 0), (-0.2, -2.6, -0.3)),
             (19.5, (1, 5, 0), (-0.1, -1.5, 0.0)),
             (T, *RH)],
    "steps": [("left", 0.6, 2.6, (4.6, 2.0, -2.6), 12.0, 0.9),
              ("right", 1.6, 3.6, (-4.6, 2.0, 2.3), -16.0, 0.7),
              ("left", 15.0, 18.0, (3.2, 2.0, -2.0), 7.0),
              ("right", 18.0, 21.0, (-3.4, 2.0, 2.0), -12.0)],
    "torso": [(0, TG),
              (2.6, (2, 12, 0)),
              (6.0, (-6, 10, 0)),
              (8.0, (-7, 10, 0)),
              (10, (10, 6, 0)),
              (13.8, (15, 4, 0)),
              (14.8, (14, 4, 0)),
              (17.0, (9, 10, 0)),
              (T, TG)],
    "head": [(0, (0, 0, 0)), (6.0, (-3, 0, 0)), (10, (-4, 0, 0)), (13.8, (-6, 0, 0)), (17, (-2, 0, 0)), (T, (0, 0, 0))],
    "reach": [(0, None),
              (1.5, (-4.6, -0.8, 4.4)),
              (4.0, (-4.8, 3.4, 2.0)),
              (6.0, (-4.4, 5.4, 0.8)),
              (8.0, (-4.2, 5.8, 0.6)),
              (9.0, (-4.4, 3.0, 3.6)),
              (10, (-1.8, -0.6, 7.1)),
              (11.5, (-1.9, -2.8, 7.0)),
              (12.8, (-1.7, -4.2, 6.2)),
              (13.8, (-1.6, -5.6, 4.6)),
              (14.8, (-1.6, -5.4, 4.3)),
              (17.0, (-2.8, -3.4, 5.2)),
              (T, None)],
    "blade": [(0, G_BLADE),
              (1.5, (-60, 0, -4)),
              (4.0, (-86, 0, -6)),
              (6.0, (-94, 0, -6)),
              (8.0, (-98, 0, -6)),
              (9.0, (-60, 0, -3)),
              (10, (0, 2, 0)),
              (12.8, (14, 3, 0)),
              (13.8, (17, 3, 0)),
              (14.8, (14, 3, 0)),
              (17.0, (-10, 3, 0)),
              (T, G_BLADE)],
    "fist": [(0, FG),
             (2.6, (4.8, 17.8, 6.2)),
             (6.0, (4.6, 17.5, 6.0)),
             (8.0, (4.6, 17.4, 6.0)),
             (10, (5.0, 18.6, 6.0)),
             (13.8, (5.2, 18.8, 5.9)),
             (14.8, (5.2, 18.8, 5.9)),
             (17.0, (5.0, 18.4, 5.8)),
             (T, FG)],
    "throw": [(0, (0, 0, 0)), (2.6, (4, 2, 0)), (8.0, (5, 2, 0)), (10, (-2, -2, -1)), (13.8, (-3, -2, -1)),
              (17, (0, 0, 0)), (T, (0, 0, 0))],
}

LOW_CUT = {
    "ticks": T, "impact": B.IMPACT["low_cut"],
    "hips": [(0, *RH),
             (1.5, (1, 5, 0), (0.0, -1.8, 0.3)),
             (5.4, (2, 8, 0), (0.0, -2.0, 0.4)),
             (6.4, (3, 6, 0), (-0.1, -2.6, -0.4)),
             (8, (6, -2, 0), (-0.3, -3.5, -3.6)),
             (10.5, (5, -4, 0), (-0.3, -3.2, -2.6)),
             (13.0, (6, 0, 0), (-0.2, -3.4, -2.0)),
             (17.0, (2, 4, 0), (-0.1, -1.9, -0.7)),
             (T, *RH)],
    "steps": [("right", 5.6, 7.8, (-4.0, 2.0, 8.0), -10.0),
              ("left", 8.6, 11.2, (3.4, 2.0, 0.6), 5.0, 0.8),
              ("right", 13.5, 18.0, (-3.4, 2.0, 2.0), -12.0),
              ("left", 17.0, 20.5, (3.2, 2.0, -2.0), 7.0)],
    "torso": [(0, TG),
              (1.5, (4, 16, 0)),
              (5.4, (6, 18, 0)),
              (6.4, (8, 14, 0)),
              (8, (18, 2, 0)),
              (10.5, (12, 2, 0)),
              (13.0, (12, 8, 0)),
              (17.0, (6, 13, 0)),
              (T, TG)],
    "head": [(0, (0, 0, 0)), (5.4, (-2, 0, 0)), (8, (-5, 0, 0)), (10.5, (-6, 0, 0)), (13, (-2, 0, 0)), (T, (0, 0, 0))],
    "reach": [(0, None),
              (1.5, (-5.2, -4.0, 1.2)),
              (3.5, (-5.2, -4.6, -0.8)),
              (5.4, (-5.0, -4.6, -1.8)),
              (6.1, (-4.8, -5.0, 0.2)),
              (8, (-1.6, -5.4, 5.4)),
              (9.2, (-1.0, -5.4, 4.9)),
              (10.5, (-0.4, -5.2, 4.8)),
              (13.0, (-2.4, -3.8, 4.6)),
              (17.0, None),
              (T, None)],
    # The Templar's hilt is long (a 5 px pommel 4-9 px behind the grip): any blade laid level and pointing outward beside
    # the body puts the pommel in the hip. So the sword is held upright at the hip through the check (the pommel hangs
    # down, clear of the body) and the reap comes down and round from there, not round from a blade laid back.
    "blade": [(0, G_BLADE),
              (1.5, (-55, -40, 0)),
              (3.5, (-85, -120, 0)),
              (5.4, (-90, -150, 0)),
              (6.1, (-85, -140, 0)),
              (7.0, (-50, -40, 0)),
              (8, (7, 6, 0)),
              (9.2, (14, 38, 0)),
              (10.5, (8, 56, 0)),
              (13.0, (-20, 18, 0)),
              (17.0, G_BLADE),
              (T, G_BLADE)],
    "fist": [(0, FG),
             (1.5, (4.8, 19.8, 6.6)),
             (5.4, (4.6, 21.0, 7.4)),
             (6.4, (4.6, 21.2, 7.4)),
             (8, (4.8, 20.6, 7.0)),
             (10.5, (5.0, 20.2, 6.8)),
             (13.0, (4.8, 19.2, 6.2)),
             (17.0, (5.0, 19.0, 5.7)),
             (T, FG)],
    "throw": [(0, (0, 0, 0)), (1.5, (-2, 0, 0)), (5.4, (-6, 2, 0)), (8, (-4, -2, 0)), (10.5, (-3, -2, 0)),
              (13, (0, 0, 0)), (T, (0, 0, 0))],
}

PLANS = {"attack_thrust": THRUST, "attack_overhead": OVERHEAD, "attack_low_cut": LOW_CUT}


def clips(report=False):
    """{clip name: clip} for the six clips (each attack and its _legs clip)."""
    out = {}
    for name, spec in PLANS.items():
        out.update(fullbody.clips(name, spec, report=report))
    return out


def main():
    import numpy as np
    assert np.allclose(fullbody.blade_dir(*G_BLADE), G_DIR, atol=2e-3), "G_BLADE no longer matches the guard"
    out = ROOT / "work" / "ownmoves" / "sentinel.animation.json"
    if "--out" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--out") + 1])
    if "--reach" in sys.argv:
        for name, spec in PLANS.items():
            print(name)
            fullbody.reach_report(spec)
        return
    if not out.exists():
        src = ROOT / "src/main/resources/assets/gravesandglory/animations/troop/sentinel.animation.json"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(src.read_text(encoding="utf-8"), encoding="utf-8")
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
