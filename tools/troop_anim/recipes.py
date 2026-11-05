"""Recipes: a clip as a few chained poses (see author.py for the pose fields), timed from sheets/design.json.

A recipe is {'clip', 'length', 'impact', 'loop', 'stance', 'keys', 'push', 'lag'}. The functions here build them from
a troop and the clip's design timing so one recipe serves every troop of a family (and, mirrored, a left-handed one).
Times are in seconds and land on the 0.05 s tick; aims are (elevation, azimuth) in the chest's frame (the body's until
the rig has a chest), azimuth + outward on that arm's side. An elevation past 90 goes over the top: (120, 0) is up and
30 degrees back.

The proven recipe is RECIPES['knight']['sword_shield_overhead'] (docs/review/anim-redo/tools-proof). The family presets
are starting points: bake one with `--check`, look at its `--sheet`, then tune the key poses for that troop.
"""
from . import model as M
from . import pose as P

TICK = 0.05


def snap(t):
    return round(round(t / TICK) * TICK, 2)


def timing(troop, clip, length=None):
    """(impact s or None, length s) from design.json's attackVariants and the clip's own length."""
    row = P.attacks(troop.family).get(clip[:-5] if clip.endswith('_wall') else clip) or {}
    lib = P.library(troop.rig, troop.family)
    have = lib.get(clip)
    L = length or (have['length'] if have else None)
    imp = row.get('impactTicks')
    return (imp / 20.0 if imp is not None else None), L


def _base(troop, clip, keys, length, **kw):
    r = {'clip': clip, 'length': length, 'loop': False, 'stance': 'default', 'keys': keys, 'push': True,
         'lag': {'off': 0.05}}
    r.update(kw)
    return r


# ---- one-handed strikes (sword, axe, with or without a shield)

def overhead(troop, clip, impact=None, length=None, shield=None):
    """High behind the head, a held top, a straight linear strike edge first, a low follow-through, a pause, home.

      0 .. T-0.25    wind-up (eases out of the stance; the body coils back); 0.25 s keeps the shoulder under 60 deg/100 ms
      T-0.25 .. T-0.15 the held top (a moving hold: it creeps a few degrees further back)
      T-0.15 .. T    the strike, linear (the arm unloads; the hips and chest drive it)
      T .. T+0.1     follow-through to a low blade (root down, knees bent, root forward); the wrist stays cocked so a
                     long hilt clears the upper arm and the tip clears the ground
      T+0.1 .. T+0.3 the pause on the low blade
      T+0.3 .. L     recovery to the stance
    """
    T0, L0 = timing(troop, clip, length)
    T = impact if impact is not None else T0
    L = length or L0
    if T is None or L is None:
        raise ValueError(f'{clip}: no impact tick in design.json and none given')
    shield = troop.shield_arm is not None if shield is None else shield
    top, strike, follow, pause = snap(T - 0.25), snap(T - 0.15), snap(T + 0.1), snap(T + 0.3)
    off_guard = {'upper': (-80.0, 4.0), 'fore': (-28.0, -24.0)} if shield else {'upper': (-62.0, 18.0), 'fore': (-30.0, 5.0)}
    off_tuck = {'upper': (-72.0, 2.0), 'fore': (-20.0, -28.0)} if shield else {'upper': (-70.0, 25.0), 'fore': (-45.0, 10.0)}
    keys = [
        {'t': 0.0, 'stance': True},
        {'t': top, 'pose': {
            'root': (0.0, 0.4, -0.6), 'hips': (-2.0, -6.0, 0.0), 'body': (-5.0, -14.0, -3.0),
            'weapon': {'upper': (45.0, 30.0), 'fore': (125.0, -10.0), 'item': (160.0, 0.0), 'twist': 0.0},
            'off': off_guard}},
        {'t': strike, 'interp': 'linear', 'pose': {
            'root': (0.0, 0.5, -0.8), 'hips': (-2.5, -7.0, 0.0), 'body': (-6.0, -16.0, -3.0),
            'weapon': {'upper': (50.0, 30.0), 'fore': (128.0, -10.0), 'item': (163.0, 0.0), 'twist': 0.0}}},
        {'t': snap(T), 'pose': {
            'root': (0.0, 1.6, 2.2), 'hips': (4.0, 5.0, 0.0), 'body': (10.0, 12.0, 2.0),
            'weapon': {'upper': (-25.0, 6.0), 'fore': (-8.0, 2.0), 'item': (25.0, -6.0), 'twist': 0.0},
            'off': off_tuck}},
        {'t': follow, 'pose': {
            'root': (0.0, 2.0, 2.8), 'hips': (6.0, 7.0, 0.0), 'body': (15.0, 14.0, 3.0),
            'weapon': {'upper': (-50.0, 4.0), 'fore': (-36.0, 0.0), 'item': (4.0, -10.0), 'twist': 0.0}}},
        {'t': pause, 'pose': {
            'root': (0.0, 1.9, 2.7), 'hips': (5.5, 6.5, 0.0), 'body': (14.0, 13.0, 3.0),
            'weapon': {'upper': (-53.0, 4.0), 'fore': (-39.0, 0.0), 'item': (2.0, -10.0), 'twist': 0.0}}},
        {'t': snap(L), 'stance': True},
    ]
    return _base(troop, clip, keys, L, impact=T)


# ---- two-handed weapons: the off hand takes the haft (grab) for the attack and lets go after

def two_handed_overhead(troop, clip, impact=None, length=None, on=(-5.0, 0.0, 0.0)):
    """A greatsword, axe or polearm cleave with both hands: like overhead(), with the off hand on the grip `on`
    (along the item, px; negative is toward the pommel) from the wind-up to the pause."""
    r = overhead(troop, clip, impact, length, shield=False)
    grab = {'grab': 1.0, 'on': on}
    for k in r['keys'][1:-1]:
        k['pose']['off'] = dict(grab)
    r['lag'] = {}
    return r


def thrust(troop, clip, impact=None, length=None, two_handed=False, on=(-5.0, 0.0, 0.0)):
    """Draw the weapon back at the hip, a linear lunge to full reach at the impact, recover."""
    T0, L0 = timing(troop, clip, length)
    T = impact if impact is not None else T0
    L = length or L0
    back, go, hold = snap(max(T - 0.2, 0.1)), snap(max(T - 0.1, 0.15)), snap(T + 0.15)
    keys = [
        {'t': 0.0, 'stance': True},
        {'t': back, 'pose': {'root': (0.0, 0.5, -0.8), 'body': (-3.0, -16.0, 0.0), 'hips': (0.0, -6.0, 0.0),
                             'weapon': {'upper': (-60.0, 30.0), 'fore': (-10.0, 10.0), 'item': (0.0, 0.0), 'twist': 0.0}}},
        {'t': go, 'interp': 'linear', 'pose': {'root': (0.0, 0.6, -0.9), 'body': (-3.0, -18.0, 0.0),
                                               'weapon': {'upper': (-62.0, 32.0), 'fore': (-12.0, 10.0), 'item': (-2.0, 0.0)}}},
        {'t': snap(T), 'pose': {'root': (0.0, 1.6, 2.8), 'body': (10.0, 10.0, 0.0), 'hips': (4.0, 6.0, 0.0),
                                'weapon': {'upper': (-6.0, 0.0), 'fore': (-2.0, -2.0), 'item': (-4.0, -2.0)},
                                'feet': {'weapon': (0.0, 0.0, 0.0, 0.0)}}},
        {'t': hold, 'pose': {'root': (0.0, 1.5, 2.6), 'body': (9.0, 9.0, 0.0)}},
        {'t': snap(L), 'stance': True},
    ]
    if two_handed:
        for k in keys[1:-1]:
            k['pose']['off'] = {'grab': 1.0, 'on': on}
    return _base(troop, clip, keys, L, impact=T)


def side_cut(troop, clip, impact=None, length=None, two_handed=False, on=(-5.0, 0.0, 0.0)):
    """A flat cut from the weapon side across the front: the blade cocked out and back at shoulder height, a linear
    sweep through the front at the impact, follow-through across the body, recover."""
    T0, L0 = timing(troop, clip, length)
    T = impact if impact is not None else T0
    L = length or L0
    top, strike, follow, pause = snap(T - 0.25), snap(T - 0.15), snap(T + 0.1), snap(T + 0.3)
    keys = [
        {'t': 0.0, 'stance': True},
        {'t': top, 'pose': {'root': (0.0, 0.5, -0.4), 'hips': (0.0, -10.0, 0.0), 'body': (0.0, -22.0, 0.0),
                            'weapon': {'upper': (-20.0, 70.0), 'fore': (10.0, 110.0), 'item': (20.0, 150.0), 'twist': 90.0}}},
        {'t': strike, 'interp': 'linear', 'pose': {'body': (0.0, -24.0, 0.0),
                                                   'weapon': {'upper': (-18.0, 74.0), 'fore': (12.0, 116.0), 'item': (22.0, 156.0)}}},
        {'t': snap(T), 'pose': {'root': (0.0, 1.4, 1.8), 'hips': (2.0, 8.0, 0.0), 'body': (6.0, 16.0, 0.0),
                                'weapon': {'upper': (-10.0, 0.0), 'fore': (0.0, -10.0), 'item': (5.0, -20.0)}}},
        {'t': follow, 'pose': {'body': (8.0, 22.0, 0.0),
                               'weapon': {'upper': (-25.0, -30.0), 'fore': (-20.0, -60.0), 'item': (-15.0, -80.0)}}},
        {'t': pause, 'pose': {'body': (7.0, 21.0, 0.0),
                              'weapon': {'upper': (-27.0, -30.0), 'fore': (-22.0, -60.0), 'item': (-18.0, -82.0)}}},
        {'t': snap(L), 'stance': True},
    ]
    if two_handed:
        for k in keys[1:-1]:
            k['pose']['off'] = {'grab': 1.0, 'on': on}
    return _base(troop, clip, keys, L, impact=T)


# ---- ranged and the rest

def bow_shot(troop, clip, impact=None, length=None, string=(0.0, 0.0, 3.0)):
    """Raise the bow (the weapon hand: archers are left-handed, so it is mirrored), the draw hand on the string, draw
    to the cheek over the held top, loose on the impact tick, settle. The draw hand is the off hand; `string` is its
    point on the bow (along, side, back) px."""
    T0, L0 = timing(troop, clip, length)
    T = impact if impact is not None else T0
    L = length or L0
    raise_, nock, loose = snap(T * 0.35), snap(T * 0.55), snap(T)
    keys = [
        {'t': 0.0, 'stance': True},
        {'t': raise_, 'pose': {'body': (0.0, -30.0, 0.0), 'weapon': {'upper': (-5.0, 60.0), 'fore': (0.0, 60.0)},
                               'off': {'grab': 1.0, 'on': string}, 'look': (0.0, 0.0)}},
        {'t': nock, 'pose': {'body': (0.0, -40.0, 0.0), 'weapon': {'upper': (5.0, 75.0), 'fore': (6.0, 78.0)}}},
        {'t': snap(T - 0.05), 'interp': 'linear', 'pose': {'off': {'grab': 0.0, 'upper': (2.0, -95.0), 'fore': (10.0, -150.0)}}},
        {'t': loose, 'pose': {'off': {'upper': (4.0, -100.0), 'fore': (6.0, -170.0)}}},
        {'t': snap(T + 0.25), 'pose': {'body': (0.0, -36.0, 0.0)}},
        {'t': snap(L), 'stance': True},
    ]
    return _base(troop, clip, keys, L, impact=T, lag={})


def crossbow_shot(troop, clip, impact=None, length=None, on=(-4.0, 0.0, 0.0)):
    """Shoulder the crossbow with both hands, a small recoil on the impact tick, lower it."""
    T0, L0 = timing(troop, clip, length)
    T = impact if impact is not None else T0
    L = length or L0
    keys = [
        {'t': 0.0, 'stance': True},
        {'t': snap(max(T - 0.1, 0.05)), 'pose': {'body': (0.0, -10.0, 0.0),
                                                  'weapon': {'upper': (-30.0, 10.0), 'fore': (5.0, -10.0), 'item': (0.0, 0.0)},
                                                  'off': {'grab': 1.0, 'on': on}}},
        {'t': snap(T), 'interp': 'linear', 'pose': {'root': (0.0, 0.0, -1.0), 'body': (-6.0, -12.0, 0.0),
                                                    'weapon': {'item': (8.0, 0.0)}}},
        {'t': snap(T + 0.2), 'pose': {'root': (0.0, 0.0, 0.0), 'body': (0.0, -10.0, 0.0), 'weapon': {'item': (0.0, 0.0)}}},
        {'t': snap(L), 'stance': True},
    ]
    return _base(troop, clip, keys, L, impact=T, lag={})


def throw(troop, clip, impact=None, length=None):
    """Draw the throwing arm back over the shoulder, a held top, a linear whip forward with the release on the impact
    tick, follow-through down across the body."""
    T0, L0 = timing(troop, clip, length)
    T = impact if impact is not None else T0
    L = length or L0
    top, go = snap(T - 0.25), snap(T - 0.1)
    keys = [
        {'t': 0.0, 'stance': True},
        {'t': top, 'pose': {'root': (0.0, 0.4, -0.8), 'body': (-6.0, -25.0, 0.0), 'hips': (0.0, -8.0, 0.0),
                            'weapon': {'upper': (35.0, 60.0), 'fore': (110.0, 30.0), 'item': (150.0, 10.0)},
                            'off': {'upper': (-10.0, -20.0), 'fore': (0.0, -20.0)}}},
        {'t': go, 'interp': 'linear', 'pose': {'weapon': {'upper': (40.0, 62.0), 'fore': (118.0, 30.0), 'item': (158.0, 10.0)}}},
        {'t': snap(T), 'pose': {'root': (0.0, 1.2, 2.0), 'body': (12.0, 15.0, 0.0), 'hips': (3.0, 6.0, 0.0),
                                'weapon': {'upper': (10.0, 5.0), 'fore': (5.0, 0.0), 'item': (0.0, 0.0)},
                                'off': {'upper': (-60.0, 20.0), 'fore': (-40.0, 10.0)}}},
        {'t': snap(T + 0.15), 'pose': {'body': (16.0, 20.0, 0.0), 'weapon': {'upper': (-50.0, -20.0), 'fore': (-60.0, -30.0)}}},
        {'t': snap(L), 'stance': True},
    ]
    return _base(troop, clip, keys, L, impact=T)


def cast(troop, clip, impact=None, length=None):
    """Gather (both hands drawn in and up), a held charge, a linear push of the casting hand on the impact tick."""
    T0, L0 = timing(troop, clip, length)
    T = impact if impact is not None else T0
    L = length or L0
    keys = [
        {'t': 0.0, 'stance': True},
        {'t': snap(T * 0.5), 'pose': {'body': (-6.0, -8.0, 0.0),
                                      'weapon': {'upper': (-40.0, -20.0), 'fore': (40.0, -60.0)},
                                      'off': {'upper': (-40.0, -20.0), 'fore': (40.0, -60.0)}}},
        {'t': snap(T - 0.1), 'interp': 'linear', 'pose': {'body': (-7.0, -9.0, 0.0)}},
        {'t': snap(T), 'pose': {'root': (0.0, 0.8, 1.6), 'body': (8.0, 6.0, 0.0),
                                'weapon': {'upper': (5.0, 0.0), 'fore': (8.0, 0.0)},
                                'off': {'upper': (-55.0, 20.0), 'fore': (-20.0, 10.0)}}},
        {'t': snap(T + 0.25), 'pose': {'body': (6.0, 5.0, 0.0)}},
        {'t': snap(L), 'stance': True},
    ]
    return _base(troop, clip, keys, L, impact=T)


def crew_fire(troop, clip, impact=None, length=None):
    """A crew's fire: lean in to the piece, a held beat, a linear pull on the impact tick, step back from the recoil."""
    T0, L0 = timing(troop, clip, length)
    T = impact if impact is not None else T0
    L = length or L0
    keys = [
        {'t': 0.0, 'stance': True},
        {'t': snap(T - 0.2), 'pose': {'root': (0.0, 1.0, 1.0), 'body': (14.0, 0.0, 0.0),
                                      'weapon': {'upper': (-40.0, 0.0), 'fore': (-10.0, -10.0)},
                                      'off': {'upper': (-40.0, 0.0), 'fore': (-10.0, -10.0)}}},
        {'t': snap(T - 0.1), 'interp': 'linear', 'pose': {'body': (15.0, 0.0, 0.0)}},
        {'t': snap(T), 'pose': {'root': (0.0, 0.6, -1.2), 'body': (2.0, 0.0, 0.0),
                                'weapon': {'upper': (-55.0, 10.0), 'fore': (-30.0, 0.0)},
                                'off': {'upper': (-55.0, 10.0), 'fore': (-30.0, 0.0)}}},
        {'t': snap(T + 0.3), 'pose': {'root': (0.0, 0.3, -0.6)}},
        {'t': snap(L), 'stance': True},
    ]
    return _base(troop, clip, keys, L, impact=T)


# ---- which preset a family's attack takes

SHAPES = {
    'overhead': overhead, 'cleave': two_handed_overhead, 'chop': overhead, 'hook': two_handed_overhead,
    'thrust': thrust, 'jab': thrust, 'cut': side_cut, 'hew': side_cut, 'reap': side_cut, 'sweep': side_cut,
    'volley': bow_shot, 'direct': bow_shot, 'loose': crossbow_shot, 'throw': throw, 'cast': cast, 'fire': crew_fire,
}
TWO_HANDED = ('greatsword', 'scythe', 'spear')


def preset(troop, clip):
    """The family preset for a clip, by the last word of its name; None when there is none."""
    base = clip[:-5] if clip.endswith('_wall') else clip
    for word in reversed(base.split('_')):
        fn = SHAPES.get(word)
        if fn is None:
            continue
        if fn in (thrust, side_cut) and troop.family in TWO_HANDED:
            return fn(troop, clip, two_handed=True)
        if fn is overhead and troop.family in TWO_HANDED:
            return two_handed_overhead(troop, clip)
        return fn(troop, clip)
    return None


# ---- tuned recipes (troop id -> clip -> builder); these are what --write commits

RECIPES = {
    'knight': {'sword_shield_overhead': lambda t: overhead(t, 'sword_shield_overhead')},
}


def recipe(troop, clip):
    tuned = RECIPES.get(troop.id, {}).get(clip)
    return tuned(troop) if tuned else preset(troop, clip)
