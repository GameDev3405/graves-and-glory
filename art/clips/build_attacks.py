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


# ---- biped: spear (wm_tusk, tc_tideguard, tc_admiral; upright grip, the free hand out along the shaft)

def spear():
    out = []
    # Thrust (impact tick 7): the spear hand draws back past the hip with the point level at the foe and the free hand
    # forward as a guide; a held beat, then a step and a straight drive, the wrist turning so the point stays level.
    out.append(clip('spear_thrust', 1.1, 0.35, 0.1, [
        (0.1, C, dict(root=(0, 0, 0, 0, 0.4, 0.8), body=(-2, 18, 0), head=(0, -12, 0), right_arm=(24, 0, 4), right_hand=(54, 0, 0),
                      left_arm=(-46, 0, -12), right_leg=(8, 0, 0), left_leg=(-12, 0, 0), left_shin=(8, 0, 0))),
        (0.26, L, dict(root=(0, 0, 0, 0, 0.5, 1.0), body=(-3, 21, 0), head=(0, -13, 0), right_arm=(28, 0, 5), right_hand=(58, 0, 0),
                       left_arm=(-50, 0, -14))),
        (0.35, L, dict(root=(0, 0, 0, 0, 0.8, -2.4), body=(6, -20, 0), head=(-4, 14, 0), right_arm=(-74, 0, -6), right_hand=(148, 0, 0),
                       left_arm=(-60, 0, -6), right_leg=(14, 0, 0), left_leg=(-26, 0, 0), left_shin=(18, 0, 0))),
        (0.46, C, dict(root=(0, 0, 0, 0, 0.85, -2.6), body=(7, -23, 0), head=(-4, 16, 0), right_arm=(-78, 0, -8), right_hand=(150, 0, 0),
                       left_arm=(-62, 0, -6))),
        (0.72, C, dict(root=(0, 0, 0, 0, 0.3, -0.8), body=(2, -6, 0), head=(-1, 5, 0), right_arm=(-20, 0, -2), right_hand=(60, 0, 0),
                       left_arm=(-16, 0, -2), right_leg=(5, 0, 0), left_leg=(-8, 0, 0), left_shin=(5, 0, 0))),
        (0.94, C, dict(root=(0, 0, 0, 0, 0.05, -0.1), body=(0, -1, 0), right_arm=(-3, 0, 0), right_hand=(6, 0, 0), left_arm=(-2, 0, 0))),
    ], lag=dict(head=0.02, left_arm=0.03)))

    # High thrust (impact tick 7): the spear comes up over the shoulder, the point aimed down the line at the face; a held
    # beat cocked there, then the arm drives forward and down over the top, the point leading.
    out.append(clip('spear_high_thrust', 1.1, 0.35, 0.12, [
        (0.12, C, dict(root=(0, 0, 0, 0, 0.2, 0.7), body=(-6, 16, 0), head=(-2, -10, 0), right_arm=(-160, 0, -8), right_hand=(228, 0, 0),
                       left_arm=(-60, 0, -10), right_leg=(6, 0, 0), left_leg=(-10, 0, 0), left_shin=(6, 0, 0))),
        (0.27, L, dict(root=(0, 0, 0, 0, 0.25, 0.9), body=(-7, 19, 0), head=(-2, -12, 0), right_arm=(-166, 0, -9), right_hand=(232, 0, 0),
                       left_arm=(-64, 0, -12))),
        (0.35, L, dict(root=(0, 0, 0, 0, 0.7, -2.2), body=(8, -20, 0), head=(-2, 14, 0), right_arm=(-118, 0, -8), right_hand=(182, 0, 0),
                       left_arm=(-30, 0, 4), right_leg=(12, 0, 0), left_leg=(-24, 0, 0), left_shin=(16, 0, 0))),
        (0.46, C, dict(root=(0, 0, 0, 0, 0.8, -2.5), body=(10, -24, 0), head=(-2, 16, 0), right_arm=(-112, 0, -10), right_hand=(180, 0, 0),
                       left_arm=(-24, 0, 6))),
        (0.72, C, dict(root=(0, 0, 0, 0, 0.3, -0.7), body=(2, -6, 0), head=(0, 4, 0), right_arm=(-40, 0, -3), right_hand=(80, 0, 0),
                       left_arm=(-6, 0, 2), right_leg=(4, 0, 0), left_leg=(-7, 0, 0), left_shin=(4, 0, 0))),
        (0.94, C, dict(root=(0, 0, 0, 0, 0.05, -0.1), body=(0, -1, 0), right_arm=(-4, 0, 0), right_hand=(8, 0, 0), left_arm=(-1, 0, 0))),
    ], lag=dict(head=0.02, left_arm=0.03)))

    # Low sweep (impact tick 8, blunt): the body coils toward the spear side, the shaft laid low and out to that side;
    # a held beat, then the body unwinds and sinks and the shaft sweeps across at shin height, carrying on past the front.
    out.append(clip('spear_low_sweep', 1.1, 0.4, 0.14, [
        (0.14, C, dict(root=(0, 0, 0, 0, 0.6, 0.5), body=(0, 30, 0), head=(0, -18, 0), right_arm=(-30, 0, -36), right_hand=(120, 0, 0),
                       left_arm=(-44, 0, -16), right_leg=(6, 0, 0), left_leg=(-10, 0, 0), left_shin=(8, 0, 0))),
        (0.3, L, dict(root=(0, 0, 0, 0, 0.7, 0.6), body=(0, 34, 0), head=(0, -20, 0), right_arm=(-32, 0, -40), right_hand=(122, 0, 0),
                      left_arm=(-46, 0, -18))),
        (0.4, L, dict(root=(0, 0, 0, 0, 1.8, -1.2), body=(14, -18, 0), head=(-8, 12, 0), right_arm=(-46, 0, -10), right_hand=(126, 0, 0),
                      left_arm=(-20, 0, 10), right_leg=(12, 0, 0), left_leg=(-26, 0, 0), left_shin=(22, 0, 0), right_shin=(10, 0, 0))),
        (0.52, C, dict(root=(0, 0, 0, 0, 1.9, -1.4), body=(16, -32, 0), head=(-8, 20, 0), right_arm=(-50, 0, 6), right_hand=(120, 0, 0),
                       left_arm=(-12, 0, 14))),
        (0.8, C, dict(root=(0, 0, 0, 0, 0.5, -0.4), body=(4, -8, 0), head=(-2, 5, 0), right_arm=(-14, 0, 0), right_hand=(40, 0, 0),
                      left_arm=(-4, 0, 4), right_leg=(3, 0, 0), left_leg=(-6, 0, 0), left_shin=(5, 0, 0), right_shin=(2, 0, 0))),
        (0.96, C, dict(root=(0, 0, 0, 0, 0.05, 0), body=(0.5, -1, 0), right_arm=(-2, 0, 0), right_hand=(4, 0, 0))),
    ], lag=dict(head=0.02, left_arm=0.04)))
    return out


def beats(name, length, impact, cocked_at, hold, poses, lag=None, two=False, follow_interp=None):
    """The shared beat layout: cocked pose at cocked_at, held (creeping to the second cocked pose) for `hold` seconds, a
    linear strike into the impact pose, follow-through 0.12 s later, a settle at about the clip's two-thirds and a near-rest
    key 0.16 s before the end. poses: (cocked, cocked2, hit, follow, settle, near)."""
    if two:
        poses = [both_hands(p, two if isinstance(two, tuple) else (0, 2.1, 2.1)) for p in poses]
    cocked, cocked2, hit, follow, settle, near = poses
    # follow_interp=L holds a ground strike's follow-through on the line from the impact (no catmull overshoot into the floor)
    keys = [(cocked_at, C, cocked), (cocked_at + hold, L, cocked2), (impact, L, hit), (impact + 0.12, follow_interp or C, follow),
            (impact + (length - impact) * 0.48, C, settle), (length - 0.16, C, near)]
    lag = dict(lag or dict(head=0.02, left_arm=0.04))
    if two:
        lag.pop('left_arm', None)   # the off hand stays on the grip
    return clip(name, length, impact, cocked_at, keys, lag=lag)


# ---- biped: axe (ic_breaker) and axe_shield (ic_hearthguard); the bit (+x of the weapon) leads every cut

def axe_cuts(prefix, chop_at, hew_name, hew_at, chop_name='overhead_chop', two=False, follow_wrist=108):
    out = []
    # Overhead chop: the axe climbs over the head and lays back, bit up and the haft behind the shoulders; a held beat, then
    # the body drops onto bent knees and the arm brings the bit down through the line, the haft in line with the arm at
    # impact; it follows through low and climbs back.
    cock = 0.1 + (chop_at - 0.2 - 0.1) * 0.5
    out.append(beats(f'{prefix}_{chop_name}', chop_at + 0.7, chop_at, cock, min(0.22, chop_at - 0.11 - cock), (
        dict(root=(0, 0, 0, 0, -0.2, 0.8), body=(-9, 10, 0), head=(-6, -6, 0), right_arm=(-162, 0, -10), right_hand=(48, 0, 0),
             left_arm=(-50, 0, -16), right_leg=(6, 0, 0), left_leg=(-8, 0, 0), left_shin=(4, 0, 0), right_shin=(2, 0, 0)),
        dict(root=(0, 0, 0, 0, -0.3, 0.9), body=(-10, 11, 0), head=(-7, -7, 0), right_arm=(-170, 0, -10), right_hand=(42, 0, 0),
             left_arm=(-54, 0, -18)),
        dict(root=(0, 0, 0, 0, 1.6, -1.8), body=(16, -16, 0), head=(6, 12, 0), right_arm=(-82, 0, -8), right_hand=(122, 0, 0),
             left_arm=(4, 0, 8), right_leg=(14, 0, 0), left_leg=(-26, 0, 0), left_shin=(22, 0, 0), right_shin=(12, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.8, -2.0), body=(18, -20, 0), head=(7, 15, 0), right_arm=(-58, 0, -10), right_hand=(follow_wrist, 0, 0),
             left_arm=(8, 0, 10)),
        dict(root=(0, 0, 0, 0, 0.5, -0.5), body=(4, -5, 0), head=(2, 4, 0), right_arm=(-14, 0, -4), right_hand=(40, 0, 0),
             left_arm=(2, 0, 2), right_leg=(3, 0, 0), left_leg=(-5, 0, 0), left_shin=(4, 0, 0), right_shin=(2, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, 0), body=(0.5, -1, 0), right_arm=(-2, 0, 0), right_hand=(4, 0, 0))),
        lag=dict(head=0.02, left_arm=0.04), two=two))
    # Side hew (or hook hew): the body coils and the arm swings out and back on the axe side at chest height, the bit
    # turned out; a held beat, then the body unwinds and the arm sweeps the bit across the front, following through to the
    # far side.
    cock = 0.1 + (hew_at - 0.2 - 0.1) * 0.4
    out.append(beats(f'{prefix}_{hew_name}', hew_at + 0.65, hew_at, cock, min(0.22, hew_at - 0.1 - cock), (
        dict(root=(0, 0, 0, 0, 0.5, 0.6), body=(-2, 32, 0), head=(0, -20, 0), right_arm=(-80, 0, -70), right_hand=(60, 0, 0),
             left_arm=(-40, 0, -14), right_leg=(6, 0, 0), left_leg=(-10, 0, 0), left_shin=(8, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.6, 0.7), body=(-3, 36, 0), head=(0, -22, 0), right_arm=(-84, 0, -74), right_hand=(56, 0, 0),
             left_arm=(-42, 0, -16)),
        dict(root=(0, 0, 0, 0, 1.2, -1.4), body=(8, -14, 0), head=(-2, 8, 0), right_arm=(-88, 0, -6), right_hand=(120, 0, 0),
             left_arm=(4, 0, 14), right_leg=(12, 0, 0), left_leg=(-24, 0, 0), left_shin=(18, 0, 0), right_shin=(8, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.3, -1.6), body=(10, -34, 0), head=(-2, 20, 0), right_arm=(-80, 0, 30), right_hand=(118, 0, 0),
             left_arm=(10, 0, 16)),
        dict(root=(0, 0, 0, 0, 0.4, -0.4), body=(3, -8, 0), head=(0, 5, 0), right_arm=(-20, 0, 4), right_hand=(40, 0, 0),
             left_arm=(2, 0, 4), right_leg=(3, 0, 0), left_leg=(-5, 0, 0), left_shin=(4, 0, 0), right_shin=(2, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, 0), body=(0.5, -1, 0), right_arm=(-2, 0, 0), right_hand=(4, 0, 0))), two=two))
    return out


def both_hands(pose, grip=(0, 2.1, 2.1)):
    """A two-handed grip: the free arm's shoulder is solved (a small grid search over the rig's forward kinematics) so its
    hand closes on the hilt or haft 3 px below the weapon fist, toward the pommel; a slight cost on the side turns keeps it
    from reaching round the far side. The forearm-less troop models share the rig's 8 px shoulder-to-hand length."""
    pose = dict(pose)
    if 'right_arm' not in pose:
        return pose
    six = {b: B.six(v) for b, v in pose.items()}
    w = B.world(six)
    rm, ro = w['right_hand']
    target = [ro[i] + B.mat_vec(rm, list(grip))[i] for i in range(3)]
    rx0 = pose['right_arm'][0]

    def cost(a):
        six['left_arm'] = [a[0], a[1], a[2], 0, 0, 0]
        lo = B.world(six)['left_hand'][1]
        return sum((lo[i] - target[i]) ** 2 for i in range(3)) ** 0.5 + 0.01 * (abs(a[1]) + abs(a[2]))

    best = min(((rx, ry, rz) for rx in range(int(rx0) - 60, int(rx0) + 61, 10) for ry in range(-70, 71, 10)
                for rz in range(-70, 21, 10)), key=cost)
    for step in (4, 2, 1):
        best = min(((best[0] + dx, best[1] + dy, best[2] + dz) for dx in (-step, 0, step) for dy in (-step, 0, step)
                    for dz in (-step, 0, step)), key=cost)
    pose['left_arm'] = tuple(float(x) for x in best)
    return pose


def axe():
    out = axe_cuts('axe', 0.6, 'side_hew', 0.6)
    # Haft jab (impact tick 7, blunt): the axe draws back to the hip with the head forward, then a short straight punch of
    # the head into the foe's body and back.
    out.append(beats('axe_haft_jab', 1.0, 0.35, 0.1, 0.16, (
        dict(root=(0, 0, 0, 0, 0.4, 0.7), body=(-2, 16, 0), head=(0, -10, 0), right_arm=(30, 0, 6), right_hand=(18, 0, 0),
             left_arm=(-30, 0, -10), right_leg=(8, 0, 0), left_leg=(-12, 0, 0), left_shin=(8, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.5, 0.9), body=(-3, 19, 0), head=(0, -12, 0), right_arm=(34, 0, 7), right_hand=(20, 0, 0),
             left_arm=(-34, 0, -12)),
        dict(root=(0, 0, 0, 0, 0.7, -1.8), body=(6, -20, 0), head=(-4, 14, 0), right_arm=(-80, 0, -8), right_hand=(112, 0, 0),
             left_arm=(10, 0, 8), right_leg=(12, 0, 0), left_leg=(-22, 0, 0), left_shin=(14, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.7, -1.9), body=(6, -21, 0), head=(-4, 15, 0), right_arm=(-76, 0, -8), right_hand=(108, 0, 0),
             left_arm=(12, 0, 8)),
        dict(root=(0, 0, 0, 0, 0.25, -0.6), body=(2, -6, 0), head=(-1, 4, 0), right_arm=(-24, 0, -2), right_hand=(40, 0, 0),
             left_arm=(4, 0, 2), right_leg=(4, 0, 0), left_leg=(-7, 0, 0), left_shin=(4, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, -0.1), body=(0, -1, 0), right_arm=(-4, 0, 0), right_hand=(6, 0, 0)))))
    return out


def axe_shield():
    return axe_cuts('axe_shield', 0.5, 'hook_hew', 0.4)


# ---- biped: greatsword (gb_hollowking), two hands on the hilt

def greatsword():
    out = axe_cuts('greatsword', 0.6, 'side_cut', 0.6, chop_name='overhead_cleave', two=True, follow_wrist=86)
    # Thrust (impact tick 7): both hands draw the hilt back to the hip with the point level, then drive it straight in.
    out.append(beats('greatsword_thrust', 1.1, 0.35, 0.1, 0.16, (
        dict(root=(0, 0, 0, 0, 0.4, 0.8), body=(-2, 18, 0), head=(0, -12, 0), right_arm=(28, 0, 6), right_hand=(18, 0, 0),
             right_leg=(8, 0, 0), left_leg=(-12, 0, 0), left_shin=(8, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.5, 1.0), body=(-3, 21, 0), head=(0, -13, 0), right_arm=(32, 0, 7), right_hand=(20, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.8, -2.2), body=(6, -20, 0), head=(-4, 14, 0), right_arm=(-90, 0, -8), right_hand=(122, 0, 0),
             right_leg=(14, 0, 0), left_leg=(-26, 0, 0), left_shin=(18, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.85, -2.5), body=(7, -23, 0), head=(-4, 16, 0), right_arm=(-92, 0, -10), right_hand=(120, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.3, -0.8), body=(2, -6, 0), head=(-1, 5, 0), right_arm=(-28, 0, -3), right_hand=(50, 0, 0),
             right_leg=(5, 0, 0), left_leg=(-8, 0, 0), left_shin=(5, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, -0.1), body=(0, -1, 0), right_arm=(-4, 0, 0), right_hand=(6, 0, 0))), two=True))
    return out


# ---- biped: scythe (reaper), both hands on the snath; the blade (+x of the weapon, at the snath's top) leads each cut

SNATH = (0, 5, -3)   # the lower hand 5 px down the snath (the upright grip sits 3 px ahead of the fist)


def scythe(P=None):
    P = dict(SCY, **(P or {}))
    out = []
    # Reap (impact tick 12): a backswing. The body coils toward the right and the fists come across the chest, so the snath
    # runs back over the right shoulder and the blade is drawn up and out behind on the right at head height; a held beat,
    # then the body unwinds and sinks and the blade sweeps right to left across the front at shin height, point first (the
    # blade's +x lies along the sweep), 4 px clear of the grass, and carries on past the left hip. (Art QA 2026-11 D1: the
    # old cocked pose held the blade forward and low, out of view; D2: the impact tip sat 3 px into the ground.)
    a = P['reap_arm']
    out.append(beats('scythe_reap', 1.3, 0.6, 0.26, 0.22, (
        dict(root=(0, 0, 0, 0, 0.6, 0.6), body=(-2, P['reap_coil'], 0), head=(0, -30, 0), right_arm=(-50, 0, -70),
             right_hand=(50, 0, 0), right_leg=(6, 0, 0), left_leg=(-10, 0, 0), left_shin=(8, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.7, 0.7), body=(-3, P['reap_coil'] + 4, 0), head=(0, -32, 0), right_arm=(-52, 0, -72),
             right_hand=(48, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.2, -1.2), body=(P['reap_lean'], -4, 0), head=(-6, 4, 0), right_arm=(a - 4, 0, -4),
             right_hand=(P['reap_wrist'] + P['reap_hit'], 0, 0),
             right_leg=(12, 0, 0), left_leg=(-24, 0, 0), left_shin=(20, 0, 0), right_shin=(10, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.3, -1.4), body=(P['reap_lean'] + 2, -44, 0), head=(-8, 24, 0), right_arm=(a - 6, 0, 8),
             right_hand=(P['reap_wrist'] + P['reap_hit'] - 14, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.5, -0.4), body=(4, -10, 0), head=(-2, 6, 0), right_arm=(-10, 0, 0), right_hand=(30, 0, 0),
             right_leg=(3, 0, 0), left_leg=(-6, 0, 0), left_shin=(5, 0, 0), right_shin=(2, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, 0), body=(0.5, -1, 0), right_arm=(-2, 0, 0), right_hand=(4, 0, 0))), two=SNATH))
    # Overhead hook (impact tick 12): the scythe climbs high behind the head with the blade twisted to hang down; a held
    # beat, then the body drops and the arms pull the snath down and forward so the blade hooks down onto the foe point
    # first, dragging back toward the reaper through the follow-through.
    tw = P['hook_twist']
    out.append(beats('scythe_overhead_hook', 1.3, 0.6, 0.26, 0.22, (
        dict(root=(0, 0, 0, 0, -0.2, 0.8), body=(-9, P['hook_coil'], 0), head=(-6, -P['hook_coil'] // 2, 0), right_arm=(-150, 0, P['hook_out']), right_hand=(P['hook_cock'], tw, 0),
             right_leg=(6, 0, 0), left_leg=(-8, 0, 0), left_shin=(4, 0, 0), right_shin=(2, 0, 0)),
        dict(root=(0, 0, 0, 0, -0.3, 0.9), body=(-10, P['hook_coil'] + 3, 0), head=(-7, -P['hook_coil'] // 2, 0), right_arm=(-156, 0, P['hook_out'] - 2), right_hand=(P['hook_cock'] - 4, tw, 0)),
        dict(root=(0, 0, 0, 0, 1.6, -1.6), body=(16, P['hook_unw'], 0), head=(6, 10, 0), right_arm=(P['hook_arm'], 0, 0), right_hand=(P['hook_hit'], tw, 0),
             right_leg=(14, 0, 0), left_leg=(-26, 0, 0), left_shin=(22, 0, 0), right_shin=(12, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.7, -1.4), body=(18, P['hook_unw'] - 14, 0), head=(7, 14, 0), right_arm=(P['hook_arm'] + 12, 0, 6),
             right_hand=(P['hook_hit'] - 14, tw, 0)),
        dict(root=(0, 0, 0, 0, 0.5, -0.5), body=(4, -5, 0), head=(2, 4, 0), right_arm=(-14, 0, -4), right_hand=(40, tw * 0.3, 0),
             right_leg=(3, 0, 0), left_leg=(-5, 0, 0), left_shin=(4, 0, 0), right_shin=(2, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, 0), body=(0.5, -1, 0), right_arm=(-2, 0, 0), right_hand=(4, 0, 0))), two=SNATH))
    return out


SCY = dict(reap_arm=-34, reap_wrist=124, reap_coil=55, reap_lean=12, reap_hit=-14,
           hook_twist=0, hook_cock=40, hook_arm=-80, hook_hit=134, hook_coil=65, hook_out=-50, hook_unw=-8)

# ---- biped: crossbow (ic_arbalist; ic_ballista's crewman runs the same clip at his engine), thrower (tc_harpooner) and
# crew (ic_mortar). Ranged rows: impact marks the loose, release or shot.

STOCK = (0, -5, -5)   # the off hand under the stock, 7 px ahead of the trigger hand (sword grip: the stock runs (0,-.71,-.71))


def crossbow():
    # Loose (loose at 0.35 s, tick 7): the stock comes up to the shoulder over 240 ms, the crossbow rolling level (prod across, as it is
    # aimed: the sword grip holds it prod-upright, so the wrist turns it a quarter round, right_hand ry ~90) and settles on
    # the aim to tick 4; the bolt looses, the prod kicks the arms up and the body rocks back and sinks; the crossbow comes
    # down to chest height, nose low, and back to rest. (Review r1 F1 and r2: the prod read vertical; the raise stays under 60 deg per 100 ms.)
    return [beats('crossbow_loose', 1.55, 0.35, 0.24, 0.08, (
        dict(root=(0, 0, 0, 0, 0.3, 0), body=(2, 12, 0), head=(2, -12, 0), right_arm=(-84, 0, 0), right_hand=(142, 88, -6),
             right_leg=(4, 0, 0), left_leg=(-6, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.35, 0), body=(2, 12, 0), head=(2, -12, 0), right_arm=(-85, 0, 0), right_hand=(140, 89, -6)),
        dict(root=(0, 0, 0, 0, 1.0, 0.6), body=(-10, 12, 0), head=(-4, -12, 0), right_arm=(-95, 0, 0), right_hand=(140, 92, -6)),
        dict(root=(0, 0, 0, 0, 0.6, 0.4), body=(-6, 12, 0), head=(-2, -12, 0), right_arm=(-90, 0, 0), right_hand=(140, 91, -6)),
        dict(root=(0, 0, 0, 0, 0.8, -0.2), body=(10, 6, 0), head=(8, -6, 0), right_arm=(-55, 0, 0), right_hand=(115, 40, -3),
             right_leg=(-4, 0, 0), right_shin=(5, 0, 0), left_leg=(-8, 0, 0), left_shin=(8, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.1, 0), body=(1, 1, 0), right_arm=(-4, 0, 0), right_hand=(4, 2, 0))),
        lag=dict(head=0.02), two=STOCK)]


def thrower():
    # Throw (impact tick 8 = release): the harpoon comes up over the shoulder, point forward, the body coiled back and the
    # free arm pointing at the target; a held beat, then the hips and shoulders unwind, the weight goes onto the front
    # foot and the arm whips the harpoon forward and out; the arm follows through low across the body.
    return [beats('thrower_throw', 1.2, 0.4, 0.12, 0.16, (
        dict(root=(0, 0, 0, 0, 0.4, 0.8), body=(-8, 14, 0), head=(0, -12, 0), right_arm=(-150, 0, -6), right_hand=(219, 0, 0),
             left_arm=(-80, 20, 0), right_leg=(10, 0, 0), left_leg=(-14, 0, 0), left_shin=(4, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.5, 0.9), body=(-9, 16, 0), head=(0, -14, 0), right_arm=(-156, 0, -6), right_hand=(224, 0, 0),
             left_arm=(-82, 20, 0)),
        dict(root=(0, 0, 0, 0, 1.2, -1.4), body=(12, -14, 0), head=(4, 10, 0), right_arm=(-100, 0, -4), right_hand=(170, 0, 0),
             left_arm=(-20, 0, 6), right_leg=(4, 0, 0), left_leg=(-18, 0, 0), left_shin=(10, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.3, -1.6), body=(16, -22, 0), head=(6, 14, 0), right_arm=(-50, 0, 6), right_hand=(122, 0, 0),
             left_arm=(6, 0, 8)),
        dict(root=(0, 0, 0, 0, 0.4, -0.4), body=(4, -6, 0), head=(2, 4, 0), right_arm=(-12, 0, 0), right_hand=(20, 0, 0),
             left_arm=(0, 0, 2), right_leg=(2, 0, 0), left_leg=(-5, 0, 0), left_shin=(3, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, 0), body=(0.5, -1, 0), right_arm=(-2, 0, 0), right_hand=(2, 0, 0))))]


def crew():
    # Fire (impact tick 8 = the shot; the tube rides on his back, muzzle past the right shoulder): he drops into a braced
    # crouch, the right hand reaching back up to the firing lanyard at the tube, the left forearm up before his face and
    # his head turned away from the muzzle; a held beat while he takes up the slack, then he yanks the lanyard and the
    # mortar fires: the recoil slams him down into a deep squat, body pitched over, head ducked, the right arm thrown out
    # and down; he rises back to rest. (Art QA 2026-11 D3: the old clip barely left the idle pose at game distance.)
    return [beats('crew_fire', 1.3, 0.4, 0.12, 0.2, (
        dict(root=(0, 0, 0, 0, 1.0, 0.4), body=(16, -10, 0), head=(10, -30, 0), right_arm=(30, 0, -14), right_hand=(0, 0, 0),
             left_arm=(-66, 0, 18), right_leg=(-22, 0, 0), right_shin=(26, 0, 0), left_leg=(-16, 0, 0), left_shin=(20, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.3, 0.5), body=(18, -12, 0), head=(12, -34, 0), right_arm=(36, 0, -16), right_hand=(0, 0, 0),
             left_arm=(-72, 0, 20), right_leg=(-28, 0, 0), right_shin=(32, 0, 0), left_leg=(-20, 0, 0), left_shin=(24, 0, 0)),
        dict(root=(0, 0, 0, 0, 2.8, 1.6), body=(30, -4, 0), head=(26, -20, 0), right_arm=(-8, 0, -30), right_hand=(0, 0, 0),
             left_arm=(-100, 0, 26), right_leg=(-56, 0, 0), right_shin=(64, 0, 0), left_leg=(-46, 0, 0), left_shin=(56, 0, 0)),
        dict(root=(0, 0, 0, 0, 3.0, 1.8), body=(32, -2, 0), head=(30, -16, 0), right_arm=(-20, 0, -40), right_hand=(0, 0, 0),
             left_arm=(-110, 0, 26)),
        dict(root=(0, 0, 0, 0, 0.8, 0.4), body=(8, -2, 0), head=(4, -6, 0), right_arm=(-6, 0, -8), right_hand=(0, 0, 0),
             left_arm=(-30, 0, 6), right_leg=(-12, 0, 0), right_shin=(14, 0, 0), left_leg=(-8, 0, 0), left_shin=(10, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, 0), body=(1, 0, 0), left_arm=(-2, 0, 0), right_leg=(-1, 0, 0), right_shin=(1, 0, 0))))]


# ---- creature rigs. y is down and the beast faces -z, as on the biped: rx > 0 dips a forward-pointing bone (head, jaw,
# chest) and swings a hanging leg back; rx < 0 rears the front up or reaches a leg forward. Root ty > 0 sinks, tz < 0 is
# forward. The muzzle (or fangs, or fists) points at the target at every key from the cocked pose to the follow-through.

BEAST_LAG = dict(head=0.02, tail=0.05)


def bite(name, impact, s=1.0, low=0.0):
    """Head drawn back and low with the jaw opening through the held beat; the head lunges and the jaw snaps shut on the
    impact, then a short worrying shake. s scales the travel, low dips the strike (a leg bite)."""
    cock = 0.12 + (impact - 0.4) * 0.5
    return beats(name, impact + 0.8, impact, cock, 0.16, (
        dict(root=(0, 0, 0, 0, 0.8 * s, 1.0 * s), chest=(4, 0, 0), head=(-8, 0, 0), jaw=(16, 0, 0),
             front_left_leg=(-6, 0, 0), front_right_leg=(4, 0, 0), tail=(-10, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.0 * s, 1.2 * s), chest=(5, 0, 0), head=(-10, 0, 0), jaw=(34, 0, 0), tail=(-12, 0, 0)),
        dict(root=(0, 0, 0, 0, max(0.6 * s + low, 1.5 * s), -2.6 * s), chest=(8 + 10 * low, 0, 0), head=(10 + 12 * low, 0, 0), jaw=(2, 0, 0),
             front_left_leg=(-14, 0, 0), front_right_leg=(10, 0, 0), back_left_leg=(8, 0, 0), back_right_leg=(6, 0, 0),
             tail=(6, 0, 0)),
        dict(root=(0, 0, 0, 0, max(0.7 * s + low, 1.25 * s), -2.4 * s), chest=(8 + 10 * low, 6, 0), head=(12 + 12 * low, 14, 10), jaw=(4, 0, 0),
             tail=(2, 14, 0)),
        dict(root=(0, 0, 0, 0, 0.3 * s, -0.6 * s), chest=(2, -2, 0), head=(4, -6, -4), jaw=(3, 0, 0),
             front_left_leg=(-3, 0, 0), front_right_leg=(2, 0, 0), back_left_leg=(2, 0, 0), back_right_leg=(1, 0, 0),
             tail=(-2, -4, 0)),
        dict(root=(0, 0, 0, 0, 0.05, 0), head=(0.5, 0, 0), jaw=(0.5, 0, 0))), lag=BEAST_LAG)


def lunge(name, impact, s=1.0):
    """Crouch on bent haunches, held; spring forward and up, forelegs reaching and jaws open, body striking at impact; land."""
    cock = 0.12 + (impact - 0.4) * 0.5
    return beats(name, impact + 0.9, impact, cock, 0.18, (
        dict(root=(0, 0, 0, 0, 2.0 * s, 1.6 * s), body=(4, 0, 0), chest=(4, 0, 0), head=(-12, 0, 0), jaw=(6, 0, 0),
             front_left_leg=(16, 0, 0), front_right_leg=(16, 0, 0), back_left_leg=(-22, 0, 0), back_right_leg=(-22, 0, 0),
             tail=(-14, 0, 0)),
        dict(root=(0, 0, 0, 0, 2.4 * s, 1.9 * s), body=(5, 0, 0), chest=(5, 0, 0), head=(-14, 0, 0), jaw=(8, 0, 0),
             front_left_leg=(18, 0, 0), front_right_leg=(18, 0, 0), back_left_leg=(-26, 0, 0), back_right_leg=(-26, 0, 0)),
        dict(root=(0, 0, 0, 0, -2.6 * s, -5.0 * s), body=(-12, 0, 0), chest=(-4, 0, 0), head=(14, 0, 0), jaw=(26, 0, 0),
             front_left_leg=(-56, 0, 0), front_right_leg=(-50, 0, 0), back_left_leg=(38, 0, 0), back_right_leg=(42, 0, 0),
             tail=(14, 0, 0)),
        dict(root=(0, 0, 0, 0, -1.6 * s, -5.6 * s), body=(-6, 0, 0), chest=(0, 0, 0), head=(10, 0, 0), jaw=(10, 0, 0),
             front_left_leg=(-30, 0, 0), front_right_leg=(-26, 0, 0), back_left_leg=(30, 0, 0), back_right_leg=(32, 0, 0),
             tail=(8, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.8 * s, -2.4 * s), body=(4, 0, 0), chest=(3, 0, 0), head=(-2, 0, 0), jaw=(2, 0, 0),
             front_left_leg=(6, 0, 0), front_right_leg=(4, 0, 0), back_left_leg=(-6, 0, 0), back_right_leg=(-4, 0, 0),
             tail=(-4, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.1, -0.1), body=(0.5, 0, 0), head=(-0.5, 0, 0))), lag=BEAST_LAG)


def hound():
    return [bite('hound_bite', 0.4, low=1.0), lunge('hound_lunge', 0.4)]


def bear():
    # Maul (impact tick 10, head zone): the bear rears onto its haunches, the right forepaw raised high and forward, head
    # tucked; a held beat, then the body drops forward and the paw rakes down through head height, claws first, and on
    # down past the chest.
    maul = beats('bear_maul', 1.3, 0.5, 0.16, 0.2, (
        dict(root=(0, 0, 0, 0, -1.2, 1.0), body=(-34, 0, 0), chest=(-4, 0, 0), head=(26, 0, 0), jaw=(14, 0, 0),
             front_right_leg=(-118, 0, -10), front_left_leg=(-40, 0, 0), back_left_leg=(-14, 0, 0), back_right_leg=(-10, 0, 0),
             tail=(-16, 0, 0)),
        dict(root=(0, 0, 0, 0, -1.4, 1.2), body=(-38, 0, 0), chest=(-5, 0, 0), head=(28, 0, 0), jaw=(20, 0, 0),
             front_right_leg=(-126, 0, -10), front_left_leg=(-42, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.8, -2.2), body=(-6, 0, 0), chest=(6, 0, 0), head=(-2, 0, 0), jaw=(28, 0, 0),
             front_right_leg=(-44, 0, 6), front_left_leg=(-10, 0, 0), back_left_leg=(6, 0, 0), back_right_leg=(8, 0, 0),
             tail=(4, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.2, -2.4), body=(2, 0, 0), chest=(8, 0, 0), head=(-4, 0, 0), jaw=(22, 0, 0),
             front_right_leg=(6, 0, 8), front_left_leg=(-2, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.4, -0.8), body=(1, 0, 0), chest=(2, 0, 0), head=(0, 0, 0), jaw=(4, 0, 0),
             front_right_leg=(2, 0, 2), front_left_leg=(0, 0, 0), back_left_leg=(2, 0, 0), back_right_leg=(2, 0, 0),
             tail=(-2, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, 0), body=(0.3, 0, 0), front_right_leg=(0.5, 0, 0))), lag=BEAST_LAG)
    return [maul, bite('bear_bite', 0.4, s=0.8)]


def ravager():
    # Charge (impact tick 10): the brute backs up a step and lowers its head, a forefoot pawing; held; then it drives
    # forward head-down, tusks first, and tosses its head up through the follow-through.
    charge = beats('ravager_charge', 1.4, 0.5, 0.16, 0.2, (
        dict(root=(0, 0, 0, 0, 1.0, 2.0), body=(4, 0, 0), chest=(6, 0, 0), head=(24, 0, 0), jaw=(4, 0, 0),
             front_right_leg=(-30, 0, 0), front_left_leg=(6, 0, 0), back_left_leg=(-10, 0, 0), back_right_leg=(-12, 0, 0),
             tail=(-14, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.2, 2.2), body=(5, 0, 0), chest=(7, 0, 0), head=(28, 0, 0), jaw=(4, 0, 0),
             front_right_leg=(12, 0, 0), front_left_leg=(8, 0, 0), back_left_leg=(-14, 0, 0), back_right_leg=(-16, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.4, -5.0), body=(6, 0, 0), chest=(8, 0, 0), head=(20, 0, 0), jaw=(6, 0, 0),
             front_right_leg=(-28, 0, 0), front_left_leg=(20, 0, 0), back_left_leg=(26, 0, 0), back_right_leg=(18, 0, 0),
             tail=(10, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.4, -5.4), body=(-6, 0, 0), chest=(-4, 0, 0), head=(-30, 0, 0), jaw=(6, 0, 0),
             front_right_leg=(-20, 0, 0), front_left_leg=(-8, 0, 0), back_left_leg=(16, 0, 0), back_right_leg=(12, 0, 0),
             tail=(16, 0, 0)),
        # the head stays up and the jaw shut through the recovery so the jaw tentacles clear the grass (Art QA 2026-11 D6)
        dict(root=(0, 0, 0, 0, 0.1, -2.0), body=(1, 0, 0), chest=(1, 0, 0), head=(-24, 0, 0), jaw=(0, 0, 0),
             front_right_leg=(-2, 0, 0), front_left_leg=(2, 0, 0), back_left_leg=(3, 0, 0), back_right_leg=(2, 0, 0),
             tail=(2, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, -0.1), head=(-0.5, 0, 0))), lag=BEAST_LAG)
    return [charge, bite('ravager_bite', 0.4, s=0.9)]


def spider():
    # Bite (impact tick 8): the front rears, forelegs lift and the fangs cock forward; held; then the thorax plunges, the
    # head drives down and the fangs stab in on the impact.
    leg = lambda l, r: dict(left_leg_1=(0, 0, l[0]), right_leg_1=(0, 0, -l[0]), left_leg_2=(0, 0, l[1]), right_leg_2=(0, 0, -l[1]),
                            left_leg_3=(0, 0, l[2]), right_leg_3=(0, 0, -l[2]), left_leg_4=(0, 0, l[3]), right_leg_4=(0, 0, -l[3]))
    bite_ = beats('spider_bite', 1.2, 0.4, 0.12, 0.16, (
        dict(root=(0, 0, 0, 0, 0.5, 1.0), thorax=(-16, 0, 0), head=(-6, 0, 0), fangs=(-30, 0, 0), abdomen=(12, 0, 0),
             **leg((-30, -12, 4, 6), None)),
        dict(root=(0, 0, 0, 0, 0.5, 1.2), thorax=(-19, 0, 0), head=(-8, 0, 0), fangs=(-38, 0, 0), abdomen=(14, 0, 0),
             **leg((-34, -14, 4, 7), None)),
        dict(root=(0, 0, 0, 0, 0.6, -2.4), thorax=(8, 0, 0), head=(12, 0, 0), fangs=(28, 0, 0), abdomen=(-6, 0, 0),
             **leg((6, 2, -2, -4), None)),
        dict(root=(0, 0, 0, 0, 0.8, -2.6), thorax=(10, 0, 0), head=(14, 0, 0), fangs=(34, 0, 0), abdomen=(-8, 0, 0),
             **leg((8, 3, -2, -4), None)),
        dict(root=(0, 0, 0, 0, 0.3, -0.8), thorax=(2, 0, 0), head=(3, 0, 0), fangs=(6, 0, 0), abdomen=(-2, 0, 0),
             **leg((2, 1, 0, -1), None)),
        dict(root=(0, 0, 0, 0, 0.05, 0), thorax=(0.3, 0, 0), fangs=(0.5, 0, 0))), lag=dict(head=0.02, abdomen=0.05))
    # Lunge (impact tick 8): every leg folds and the body sinks low, held; it springs forward and up, forelegs thrown out,
    # and the body hits at impact; it lands spread and settles.
    lunge_ = beats('spider_lunge', 1.3, 0.4, 0.12, 0.18, (
        dict(root=(0, 0, 0, 0, 2.6, 1.4), thorax=(4, 0, 0), head=(-4, 0, 0), fangs=(-10, 0, 0), abdomen=(-6, 0, 0),
             **leg((-14, -14, -14, -14), None)),
        dict(root=(0, 0, 0, 0, 3.0, 1.6), thorax=(5, 0, 0), head=(-5, 0, 0), fangs=(-12, 0, 0), abdomen=(-7, 0, 0),
             **leg((-16, -16, -16, -16), None)),
        dict(root=(0, 0, 0, 0, -3.0, -6.0), thorax=(-10, 0, 0), head=(6, 0, 0), fangs=(20, 0, 0), abdomen=(10, 0, 0),
             **leg((-36, -20, 14, 22), None)),
        dict(root=(0, 0, 0, 0, -1.6, -6.4), thorax=(-4, 0, 0), head=(6, 0, 0), fangs=(24, 0, 0), abdomen=(6, 0, 0),
             **leg((-20, -10, 10, 14), None)),
        dict(root=(0, 0, 0, 0, 0.8, -2.6), thorax=(2, 0, 0), head=(1, 0, 0), fangs=(4, 0, 0), abdomen=(-2, 0, 0),
             **leg((2, 2, 2, 2), None)),
        dict(root=(0, 0, 0, 0, 0.1, -0.1), thorax=(0.3, 0, 0))), lag=dict(head=0.02, abdomen=0.05))
    return [bite_, lunge_]


COL = dict(hit_ty=0.3, hit_wrist=234, follow_wrist=236)


def colossus(P=None):
    P = dict(COL, **(P or {}))
    # Slam (impact tick 12, head zone): both fists climb overhead and the body leans back, the maul upright and laid back
    # behind the head, held; then the body folds forward onto bent hips, the arms come down level in front, fists together,
    # and the wrist brings the maul over the top so its head leads and lands on the ground ahead (the maul hangs from
    # right_hand, so the wrist keys turn it; review r1 F3).
    slam = beats('colossus_slam', 1.5, 0.6, 0.30, 0.18, (
        dict(root=(0, 0, 0, 0, -0.4, 0.8), body=(-10, 0, 0), head=(-6, 0, 0), right_arm=(-166, 0, 8), left_arm=(-166, 0, -8),
             right_hand=(170, 0, 0), right_leg=(6, 0, 0), left_leg=(-6, 0, 0)),
        dict(root=(0, 0, 0, 0, -0.6, 1.0), body=(-12, 0, 0), head=(-8, 0, 0), right_arm=(-172, 0, 8), left_arm=(-172, 0, -8),
             right_hand=(164, 0, 0)),
        # the slam is a ground strike: the maul head lands on the grass, not in it (Art QA 2026-11 D5); the kneeless legs
        # take little root drop, so the weight goes into the body fold
        dict(root=(0, 0, 0, 0, P['hit_ty'], -1.6), body=(22, 0, 0), head=(-10, 0, 0), right_arm=(-104, -10, 0), left_arm=(-104, 10, 0),
             right_hand=(P['hit_wrist'], 0, 0), right_leg=(-18, 0, 0), left_leg=(10, 0, 0)),
        dict(root=(0, 0, 0, 0, P['hit_ty'] + 0.3, -1.8), body=(26, 0, 0), head=(-12, 0, 0), right_arm=(-112, -10, 0), left_arm=(-112, 10, 0),
             right_hand=(P['follow_wrist'], 0, 0)),
        # the recovery lifts the maul upright before the chest and the fists off the grass, rather than reaching both long
        # arms forward to the ground (D4: they read as a splayed leg)
        dict(root=(0, 0, 0, 0, 0.3, -0.4), body=(4, 0, 0), head=(-2, 0, 0), right_arm=(-56, 0, 4), left_arm=(-44, 0, -4),
             right_hand=(100, 0, 0), right_leg=(-2, 0, 0), left_leg=(1, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, 0), body=(0.5, 0, 0), right_arm=(-1, 0, 0), left_arm=(-1, 0, 0), right_hand=(1, 0, 0))),
        lag=dict(head=0.03), follow_interp=L)
    # Backhand (impact tick 8): the right arm swings across the chest to the left with the body coiled left, held; then the
    # body unwinds and the arm sweeps out to the right at chest height, back of the fist leading, and carries past.
    back = beats('colossus_backhand', 1.2, 0.4, 0.12, 0.16, (
        dict(root=(0, 0, 0, 0, 0.6, 0.4), body=(4, -30, 0), head=(0, 22, 0), right_arm=(-84, -56, 0), right_hand=(-10, 0, 0),
             left_arm=(10, 0, -10), right_leg=(-4, 0, 0), left_leg=(6, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.7, 0.5), body=(5, -34, 0), head=(0, 24, 0), right_arm=(-86, -62, 0), right_hand=(-12, 0, 0),
             left_arm=(12, 0, -10)),
        dict(root=(0, 0, 0, 0, 1.2, -0.8), body=(6, 18, 0), head=(0, -6, 0), right_arm=(-82, 26, 0), right_hand=(-6, 0, 0),
             left_arm=(-6, 0, -6), right_leg=(6, 0, 0), left_leg=(-8, 0, 0)),
        dict(root=(0, 0, 0, 0, 1.3, -0.9), body=(6, 32, 0), head=(0, -12, 0), right_arm=(-74, 56, 0), right_hand=(-4, 0, 0),
             left_arm=(-8, 0, -6)),
        dict(root=(0, 0, 0, 0, 0.4, -0.3), body=(2, 6, 0), head=(0, -2, 0), right_arm=(-14, 10, 0), right_hand=(-1, 0, 0),
             left_arm=(-2, 0, -1), right_leg=(1, 0, 0), left_leg=(-2, 0, 0)),
        dict(root=(0, 0, 0, 0, 0.05, 0), body=(0, 0.5, 0), right_arm=(-1, 0, 0))), lag=dict(head=0.03, left_arm=0.04))
    return [slam, back]


FAMILIES = {
    ('biped', 'sword'): sword,
    ('biped', 'spear'): spear,
    ('biped', 'axe'): axe,
    ('biped', 'axe_shield'): axe_shield,
    ('biped', 'greatsword'): greatsword,
    ('biped', 'scythe'): scythe,
    ('biped', 'crossbow'): crossbow,
    ('biped', 'thrower'): thrower,
    ('biped', 'crew'): crew,
    ('hound', 'hound'): hound,
    ('hound', 'bear'): bear,
    ('hound', 'ravager'): ravager,
    ('spider', 'spider'): spider,
    ('colossus', 'colossus'): colossus,
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
