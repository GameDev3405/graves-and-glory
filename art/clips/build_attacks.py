"""Hand-keyed attack clips for the families that had none (anim review 2026-11-04 section 0): writes one Blockbench project
per family, art/clips/<rig>/<family>.bbmodel, which tools/clips.py converts to the clip JSON the game loads.

  python art/clips/build_attacks.py            write the projects (then run python tools/clips.py)
  python art/clips/build_attacks.py --check    rebuild in memory and compare with the committed projects

Same bookkeeping as build_clips.py (whose Anim and project it borrows, swapping in each family's rig); it never touches
sword_shield, bow, caster or common. These families have no stance clips, so every key is an offset from the model's rest
pose and each attack starts and ends at zero. None of their troop models has a forearm bone, so the swing is carried by the
shoulder (right_arm) and the wrist (right_hand); no forearm is keyed, so no elbow can bend backwards.

Every attack has the same beats: a wind-up into a cocked pose, a held beat of 150-250 ms there (two keys a few degrees
apart, so the pose creeps rather than freezes), the strike (linear into the impact marker), a follow-through past it, and
the recovery to rest. The `windup` marker sits on the cocked pose for the contact sheet.

Sign reminders (ModelPart, y down, -z forward): a negative shoulder rx swings the arm forward and up (-90 is level ahead,
-180 straight up); a positive wrist rx turns the weapon back toward the forearm; body ry positive turns the chest toward the
sword side's back (the coil), negative unwinds it; root ty positive sinks, root tz negative steps forward.

Blade direction: a sword-type grip ([90, 45, 90]) holds the blade 135 degrees ahead of the arm in the side plane, so with
the arm at angle a (0 hanging, 90 ahead, 180 up) and wrist w the blade points at a + 135 - w. An upright grip (spear,
trident, scythe, harpoon) holds the shaft 164 degrees ahead: a + 164 - w. The keys below are set so the blade or point
leads the move at every key (Kort, 2026-11-03).
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'art/clips'))
import build_clips as B  # noqa: E402

Z = (0, 0, 0)


def use_rig(name):
    """Point build_clips' Anim and project at art/rigs/<name>.json."""
    rig = json.loads((ROOT / 'art/rigs' / f'{name}.json').read_text(encoding='utf-8'))
    pivot = {}
    for b in rig['bones']:
        p = pivot.get(b['parent'], (0, 0, 0)) if b['parent'] else (0, 0, 0)
        pivot[b['name']] = tuple(a + c for a, c in zip(p, b['pivot']))
    B.RIG, B.PIVOT = rig, pivot


def clip(name, length, impact, windup, keys, lag=None):
    """keys: [(t, interp, {bone: six})]; the first and last keys are rest for every bone keyed anywhere."""
    a = B.Anim(name, length, impact=impact, windup=windup)
    bones = sorted({b for _, _, k in keys for b in k})
    a.k(0.0, **{b: Z for b in bones})
    for t, i, k in keys:
        a.k(t, i, **k)
    a.k(length, **{b: Z for b in bones})
    if lag:
        a.lag(**lag)
    return a


L = 'linear'
C = 'catmullrom'


# ---- biped: sword (revenant, tc_raider; one-handed, no shield)

def sword():
    out = []
    # Thrust (impact tick 7): the sword hand draws back to the hip with the point already level at the foe, the body coiled
    # behind it and the free arm out in front; a held beat, then the body unwinds, the shoulder drives the arm forward and
    # the wrist turns over so the point stays level and leads the whole way; a short overreach and the arm reels back.
    out.append(clip('sword_thrust', 1.1, 0.35, 0.1, [
        (0.1, C, dict(root=(0, 0, 0, 0, 0.4, 0.8), body=(-2, 16, 0), head=(0, -10, 0), right_arm=(30, 0, 6), right_hand=(18, 0, 0),
                      left_arm=(-30, 0, -10), left_hand=(0, 0, 0), right_leg=(8, 0, 0), left_leg=(-12, 0, 0), left_shin=(8, 0, 0))),
        (0.26, L, dict(root=(0, 0, 0, 0, 0.5, 1.0), body=(-3, 19, 0), head=(0, -12, 0), right_arm=(34, 0, 7), right_hand=(20, 0, 0),
                       left_arm=(-34, 0, -12))),
        (0.35, L, dict(root=(0, 0, 0, 0, 0.8, -2.2), body=(6, -24, 0), head=(-4, 18, 0), right_arm=(-96, 0, -8), right_hand=(120, 0, 0),
                       left_arm=(12, 0, 8), right_leg=(14, 0, 0), left_leg=(-26, 0, 0), left_shin=(18, 0, 0))),
        (0.45, C, dict(root=(0, 0, 0, 0, 0.85, -2.5), body=(7, -27, 0), head=(-4, 20, 0), right_arm=(-98, 0, -10), right_hand=(116, 0, 0),
                       left_arm=(16, 0, 10))),
        (0.7, C, dict(root=(0, 0, 0, 0, 0.3, -0.8), body=(2, -8, 0), head=(-1, 6, 0), right_arm=(-30, 0, -3), right_hand=(55, 0, 0),
                      left_arm=(4, 0, 2), right_leg=(5, 0, 0), left_leg=(-8, 0, 0), left_shin=(5, 0, 0))),
        (0.92, C, dict(root=(0, 0, 0, 0, 0.05, -0.1), body=(0, -1, 0), head=(0, 1, 0), right_arm=(-4, 0, 0), right_hand=(6, 0, 0))),
    ], lag=dict(head=0.02, left_arm=0.03)))

    # Overhead cut (impact tick 10): a small sink, then the sword climbs over the head, the blade laid back behind the
    # shoulders, the free arm up across the face; a held beat at the top, then the body drops and turns and the arm comes
    # over, the wrist snapping the blade out in line with the arm by the impact at head height; it follows through low and
    # forward, then climbs back.
    out.append(clip('sword_overhead', 1.2, 0.5, 0.24, [
        (0.1, C, dict(root=(0, 0, 0, 0, 0.5, 0.2), body=(3, 4, 0), right_arm=(8, 0, 2), right_hand=(0, 0, 0), left_arm=(-8, 0, 0))),
        (0.24, C, dict(root=(0, 0, 0, 0, -0.2, 0.8), body=(-8, 10, 0), head=(-6, -6, 0), right_arm=(-160, 0, -12), right_hand=(46, 0, 0),
                       left_arm=(-50, 0, -18), left_hand=(0, 0, 0), right_leg=(6, 0, 0), left_leg=(-8, 0, 0))),
        (0.4, L, dict(root=(0, 0, 0, 0, -0.25, 0.9), body=(-9, 11, 0), head=(-7, -7, 0), right_arm=(-168, 0, -12), right_hand=(40, 0, 0),
                      left_arm=(-54, 0, -20))),
        (0.5, L, dict(root=(0, 0, 0, 0, 1.2, -1.8), body=(14, -18, 0), head=(6, 14, 0), right_arm=(-84, 0, -10), right_hand=(128, 0, 0),
                      left_arm=(6, 0, 6), right_leg=(12, 0, 0), left_leg=(-22, 0, 0), left_shin=(16, 0, 0))),
        (0.62, C, dict(root=(0, 0, 0, 0, 1.4, -2.0), body=(16, -24, 0), head=(7, 17, 0), right_arm=(-56, 0, -14), right_hand=(100, 0, 0),
                       left_arm=(10, 0, 8))),
        (0.86, C, dict(root=(0, 0, 0, 0, 0.4, -0.5), body=(4, -6, 0), head=(2, 4, 0), right_arm=(-12, 0, -4), right_hand=(40, 0, 0),
                       left_arm=(2, 0, 2), right_leg=(3, 0, 0), left_leg=(-5, 0, 0), left_shin=(4, 0, 0))),
        (1.04, C, dict(root=(0, 0, 0, 0, 0.05, 0), body=(0.5, -1, 0), right_arm=(-2, 0, 0), right_hand=(4, 0, 0))),
    ], lag=dict(right_hand=0.025, head=0.02, left_arm=0.04)))

    # Low cut at the legs (impact tick 8): the body coils toward the sword side and the arm lifts out and back beside the
    # hip, the blade raised behind it; a held beat, then the body unwinds and sinks and the arm sweeps forward and across,
    # the blade falling to point forward and down at the shins by the impact; it carries across the front and recovers.
    out.append(clip('sword_low_cut', 1.1, 0.4, 0.14, [
        (0.14, C, dict(root=(0, 0, 0, 0, 0.4, 0.6), body=(-2, 26, 0), head=(0, -16, 0), right_arm=(-110, 0, -40), right_hand=(25, 0, 0),
                       left_arm=(-36, 0, -10), right_leg=(6, 0, 0), left_leg=(-10, 0, 0), left_shin=(8, 0, 0))),
        (0.3, L, dict(root=(0, 0, 0, 0, 0.5, 0.7), body=(-3, 30, 0), head=(0, -18, 0), right_arm=(-116, 0, -42), right_hand=(22, 0, 0),
                      left_arm=(-40, 0, -12))),
        (0.4, L, dict(root=(0, 0, 0, 0, 1.8, -1.4), body=(16, -16, 0), head=(-8, 10, 0), right_arm=(-56, 0, -14), right_hand=(112, 0, 0),
                      left_arm=(4, 0, 14), right_leg=(12, 0, 0), left_leg=(-28, 0, 0), left_shin=(24, 0, 0), right_shin=(10, 0, 0))),
        (0.52, C, dict(root=(0, 0, 0, 0, 1.9, -1.6), body=(18, -28, 0), head=(-8, 18, 0), right_arm=(-62, 0, -34), right_hand=(100, 0, 0),
                       left_arm=(8, 0, 16))),
        (0.8, C, dict(root=(0, 0, 0, 0, 0.5, -0.4), body=(4, -8, 0), head=(-2, 5, 0), right_arm=(-12, 0, -8), right_hand=(36, 0, 0),
                      left_arm=(2, 0, 4), right_leg=(3, 0, 0), left_leg=(-6, 0, 0), left_shin=(5, 0, 0), right_shin=(2, 0, 0))),
        (0.96, C, dict(root=(0, 0, 0, 0, 0.05, 0), body=(0.5, -1, 0), right_arm=(-2, 0, 0), right_hand=(4, 0, 0))),
    ], lag=dict(right_hand=0.025, head=0.02, left_arm=0.04)))
    return out


FAMILIES = {
    ('biped', 'sword'): sword,
}


def projects():
    out = {}
    for (rig, family), make in FAMILIES.items():
        use_rig(rig)
        out[(rig, family)] = B.dump(B.project(family, make()))
    use_rig('biped')
    return out


def main(args):
    check = args == ['--check']
    if args not in ([], ['--check']):
        print(__doc__)
        return 2
    bad = 0
    for (rig, family), text in projects().items():
        path = ROOT / 'art/clips' / rig / f'{family}.bbmodel'
        if check:
            if not path.is_file() or path.read_text(encoding='utf-8') != text:
                print(f'build_attacks: {path.relative_to(ROOT).as_posix()} is stale; run python art/clips/build_attacks.py')
                bad = 1
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8', newline='\n')
            print(f'build_attacks: wrote {path.relative_to(ROOT).as_posix()}')
    return bad


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
