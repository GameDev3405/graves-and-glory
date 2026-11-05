"""What every set shares: the rarity ladder, team colours, the shell boxes and the palette builder.

Rarity reads the way the first set (the crimson knight on Kort's sheet) reads it: Common is the set's own metal; Uncommon
adds green on the trim bands; Rare blues the metal; Epic blackens it with violet trim; Legendary is the set's own
darkened or precious metal with ember-orange trim, a few glowing texels and extra geometry. Only the `plate` and `accent`
materials change with rarity, so every House keeps its bone, fur, shell, brass and cloth at every tier.
"""
from .engine import Box, Palette, shift

RARITIES = ('common', 'uncommon', 'rare', 'epic', 'legendary')
RARITY_BAR = {'common': '#8E9298', 'uncommon': '#3FA843', 'rare': '#3A7BD5', 'epic': '#9442D0', 'legendary': '#E8892A'}
ACCENT = {'uncommon': '#3E8F3A', 'rare': '#6E93C8', 'epic': '#8C3CCB', 'legendary': '#D9782A'}
TEAMS = {'crimson': '#8E1F25', 'teal': '#17786E', 'gold': '#A88A2C', 'violet': '#5C3792'}

VOID = '#17161C'
EMBER = '#E2702A'


def palette(spec, rarity, team):
    """spec: plate {rarity: hex}, accent (Common's trim), mats {name: hex or (hex, step)}, legendary_accent optional."""
    p = Palette()
    p.add('plate', spec['plate'][rarity], spec.get('plate_step', 0.22), clamp=True)
    if rarity == 'common':
        p.add('accent', spec['accent'], 0.20)
    else:
        p.add('accent', spec.get('accent_' + rarity, ACCENT[rarity]), 0.20, clamp=False)
    for name, v in spec['mats'].items():
        hexv, step = (v, 0.22) if isinstance(v, str) else v
        p.add(name, hexv, step)
    p.add('team', TEAMS[team], 0.20, clamp=False)
    p.add('void', VOID, 0.25)
    p.add('ember', EMBER, 0.16, clamp=False, glow=True)
    return p


def rare_of(hexv, dv=0.95):
    return shift(hexv, hue=212, ds=+0.20, dv=dv)


def epic_of(hexv, dv=0.55):
    return shift(hexv, hue=262, ds=+0.06, dv=dv)


def R(*boxes, leg_nudge=0.03):
    """A right-side box and its mirror. Leg boxes' mirrors grow a hair so the two legs' faces never share a plane."""
    out = []
    for b in boxes:
        out.append(b)
        out.append(b.mirrored(leg_nudge if b.bone in ('right_leg',) else 0.0))
    return out


def row(top, y):
    """The texel row of a side face whose top edge is at model height `top`, for model height y (rounded down)."""
    return int(round(top - y))
