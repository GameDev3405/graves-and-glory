"""Heraldic units: the skins for the six Gilded Order troops.

My own version of the project's work/p2b/skins.py. The baseline skins are flat:
a body is one value of cream, a leg is one value of mail, and where forms meet
(under the chin, in the armpit, where the skirt falls over the leg) there is no
shadow at all, so the unit reads as a cut-out rather than a body.

This adds, for every troop:
  - form: a lit edge, a graded field and a shadowed edge, so the limb is round;
  - ambient occlusion where forms meet, so the unit sits on itself;
  - material separation: mail reads as rings, cloth as a weave with folds, plate
    as a gradient with a specular, leather and wood as grain;
  - a face with a brow, a nose shadow, a mouth and a lit cheek;
  - edge lines, dark on the shadow side and light on the lit side.

Light comes from the top front left, which is the way the renderer shades, so
the painted highlight and the lit face agree.

Every face is painted through a size-aware spec (a function of the face's width
and height), so a pattern can never miss its face.

  python work/p2b/hx_skins.py [troop ...]
"""
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))

from hx_art import (P, Painter, cylinder, darker, grid, hgrad, limb, mail, mirror, vgrad)  # noqa: E402

ART = ROOT / 'art/troops'


# ---------------------------------------------------------------------------------------
# The face: an 8x8 head. Rows 0 and 1 sit under the helm and are hair in shadow; the
# brow is a shadow line, the eyes carry a lit sclera and a dark pupil, the nose is a
# shadow and a lit bridge, the mouth is a line.
# ---------------------------------------------------------------------------------------
FACE = {
    'young': grid(['HHHHHHHH',
                   'HHHHHHHH',
                   'hKKKKKKh',
                   'KbbbbbbK',
                   'KwpKKpwK',
                   'KKKnnKKK',
                   'KKKmmKKK',
                   'kKKKKKKk'],
                  {'H': 'H1', 'h': 'H2', 'K': 'K1', 'b': 'K2', 'w': 'EW', 'p': 'EP', 'n': 'K2', 'm': 'MO',
                   'k': 'K2'}),
    'weathered': grid(['HHHHHHHH',
                       'HHHHHHHH',
                       'hWWWWWWh',
                       'WbbbbbbW',
                       'WwpWWpwW',
                       'WWWnnWWW',
                       'WWWmmWWW',
                       'wWWWWWWw'],
                      {'H': 'H1', 'h': 'H2', 'W': 'W1', 'b': 'W2', 'w': 'EW', 'p': 'EP', 'n': 'W2', 'm': 'MO',
                       'w': 'W2'}),
    'old': grid(['RRRRRRRR',
                 'RRRRRRRR',
                 'rWWWWWWr',
                 'WbbbbbbW',
                 'WwpWWpwW',
                 'WWWnnWWW',
                 'WWWmmWWW',
                 'wWWWWWWw'],
                {'R': 'R1', 'r': 'R2', 'W': 'W1', 'b': 'W2', 'w': 'EW', 'p': 'EP', 'n': 'W2', 'm': 'MO', 'w': 'W2'}),
}
# The side of the head: the face is toward the right (the viewer's left when seen from
# the west), the back of the skull toward the left.
FACE_SIDE = {
    'young': grid(['HHHHHHHH',
                   'HHHHHHHH',
                   'HHHHHHKK',
                   'HHHHKKKK',
                   'HHHkKKKK',
                   'HHHkKKKK',
                   'HHHKKKKK',
                   'hhKKKKKk'],
                  {'H': 'H1', 'h': 'H2', 'K': 'K1', 'k': 'K2'}),
    'weathered': grid(['HHHHHHHH',
                       'HHHHHHHH',
                       'HHHHHHWW',
                       'HHHHWWWW',
                       'HHHWWWWW',
                       'HHHWWWWW',
                       'HHWWWWWW',
                       'hhWWWWWW'],
                      {'H': 'H1', 'h': 'H2', 'W': 'W1'}),
    'old': grid(['RRRRRRRR',
                 'RRRRRRRR',
                 'RRRRRRWW',
                 'RRRRWWWW',
                 'RRRWWWWW',
                 'RRRWWWWW',
                 'RRWWWWWW',
                 'rrWWWWWW'],
                {'R': 'R1', 'r': 'R2', 'W': 'W1'}),
}
# The back of the head: hair, with the nape in shadow.
FACE_BACK = {
    'young': grid(['HHHHHHHH'] * 6 + ['hHHHHHHh', 'hhhhhhhh'], {'H': 'H1', 'h': 'H2'}),
    'weathered': grid(['HHHHHHHH'] * 6 + ['hHHHHHHh', 'hhhhhhhh'], {'H': 'H1', 'h': 'H2'}),
    'old': grid(['RRRRRRRR'] * 6 + ['rRRRRRRr', 'rrrrrrrr'], {'R': 'R1', 'r': 'R2'}),
}


def head(p, kind, hair):
    """The head: a face, its two sides, the back and the crown."""
    p.put('head', 'north', FACE[kind])
    p.put('head', 'west', FACE_SIDE[kind])
    p.put('head', 'east', mirror(FACE_SIDE[kind]))
    p.put('head', 'south', FACE_BACK[kind])
    p.fill('head', 'top', hair)
    p.fill('head', 'bottom', darker(hair))


# ---------------------------------------------------------------------------------------
# A metal helm shell: a lit crown band, a graded band and a shadowed rim.
# ---------------------------------------------------------------------------------------
def helm_shell(p, part, box=0, base='S1', lit='S0', dark='S3', deep='S4', crown=True):
    for f in ('north', 'west', 'east', 'south'):
        p.paint(part, f, lambda w, h, b=base, l=lit, d=dark: vgrad(w, h, [l, b, d], mid_at=max(1, h // 3)),
                box=box)
    if crown:
        p.paint(part, 'top', lambda w, h, b=base, l=lit, d=dark: hgrad(w, h, l, b, d), box=box)
    else:
        p.paint(part, 'top', lambda w, h, b=base: vgrad(w, h, b), box=box)
    p.paint(part, 'bottom', lambda w, h, c=deep: vgrad(w, h, c), box=box)


# ---------------------------------------------------------------------------------------
# The body: a garment over the frame. The front is lit on the left and shadowed on the
# right; the shoulder line and the foot are in shadow (the collar above, the skirt
# below); the back is one step down all over.
# ---------------------------------------------------------------------------------------
def garment(p, bone, top, field_code, lit, dark, foot=None, back=None, folds=True):
    """Paints a garment on a biped body.

    The front is lit on the left and shadowed on the right, with a dark shadow band
    down the back edge where the body turns away; vertical fold lines break the
    field so it reads as hanging cloth; the shoulder line is in shadow under the
    collar and the hem is in shadow where the skirt falls over it.
    """
    deep = darker(dark)
    tones = [lit, field_code, field_code, dark, deep]

    def front(w, h):
        """The lit side of the form, falling away to the shadowed side, with the
        collar in shadow above and the hem in shadow below."""
        base = cylinder(w, h, tones, fold=0.62 if folds else None, fold_col=darker(field_code))
        out = []
        for j, r in enumerate(base):
            row = r.split()
            if j == 0:
                row = [top] * w                       # the collar, in shadow
            elif j == h - 1:
                row = [(foot or deep)] * w            # the hem, under the skirt
            out.append(' '.join(row))
        return out

    def side(w, h):
        """The narrow side of the form: lit at the front edge, dark at the back."""
        base = cylinder(w, h, [lit, field_code, dark, deep])
        out = []
        for j, r in enumerate(base):
            row = r.split()
            if j == 0:
                row = [top] * w
            elif j == h - 1:
                row = [(foot or deep)] * w
            out.append(' '.join(row))
        return out

    p.paint(bone, 'north', front)
    p.paint(bone, 'south', lambda w, h, b=back or darker(field_code): vgrad(w, h, b))
    p.paint(bone, 'west', side)
    p.paint(bone, 'east', mirror(side))
    p.paint(bone, 'top', lambda w, h: vgrad(w, h, top))
    p.paint(bone, 'bottom', lambda w, h, c=foot or deep: vgrad(w, h, c))


def pauldron(p, metal='S'):
    """Paints the pauldron lames. The upper lame is the big one over the shoulder, the
    second hangs below it, and the third is the rolled rim on top in gilt. It takes the
    troop's metal, so the Templar's is white plate and the Marshal's is blackened."""
    lit, mid, dark, deep = {
        'S': ('S0', 'S1', 'S2', 'S3'),
        'B': ('B0', 'B1', 'B2', 'B3'),
    }[metal]
    for name in ('pauldron_right', 'pauldron_left'):
        # the cap, box 0: a lit top edge, a graded field, a shadowed foot
        p.paint(name, 'north', lambda w, h, l=lit, m=mid, d=dark, c=deep: cylinder(w, h, [l, m, d, c]))
        p.paint(name, 'south', lambda w, h, m=mid, d=dark, c=deep: cylinder(w, h, [m, d, c, c]))
        p.paint(name, 'west', lambda w, h, l=lit, m=mid, d=dark, c=deep: cylinder(w, h, [l, m, d, c]))
        p.paint(name, 'east', lambda w, h, m=mid, d=dark, c=deep: cylinder(w, h, [m, d, c, c]))
        p.paint(name, 'top', lambda w, h, l=lit, m=mid: cylinder(w, h, [l, m]))
        p.paint(name, 'bottom', lambda w, h, c=deep: cylinder(w, h, [c]))
        # the rolled rim, box 1: gilt, a lit roll
        p.paint(name, 'north', lambda w, h: vgrad(w, h, ['G0', 'G1', 'G3'], mid_at=max(1, h // 3)), box=1)
        p.paint(name, 'south', lambda w, h: vgrad(w, h, ['G1', 'G2', 'G4'], mid_at=max(1, h // 3)), box=1)
        p.paint(name, 'west', lambda w, h: vgrad(w, h, ['G0', 'G1', 'G3'], mid_at=max(1, h // 3)), box=1)
        p.paint(name, 'east', lambda w, h: vgrad(w, h, ['G1', 'G2', 'G4'], mid_at=max(1, h // 3)), box=1)
        p.paint(name, 'top', lambda w, h: vgrad(w, h, ['G0', 'G1']), box=1)
        p.paint(name, 'bottom', lambda w, h: vgrad(w, h, ['G3', 'G4']), box=1)


def skirt_front(hem='E4', field='E1', edge='E3'):
    """A skirt front: a lit field, a fold every other column and a shadowed hem."""
    def spec(w, h):
        out = []
        for j in range(h):
            row = []
            for i in range(w):
                if j == h - 1:
                    c = hem
                elif i == 0 or i == w - 1:
                    c = edge
                elif i % 3 == 1 and j > 0:
                    c = edge
                else:
                    c = field
                row.append(c)
            out.append(' '.join(row))
        return out
    return spec


# =======================================================================================
# The Order Squire (knight): the cheap shield line. Iron cap, white surcoat over mail,
# a team pale, an arming sword. Young, clean-shaven.
# =======================================================================================
def knight(stage):
    p = Painter('knight')
    head(p, 'young', 'H1')
    # ---- the iron cap: a lit crown, a band, a shadowed rim over the brow ----
    helm_shell(p, 'iron_cap', 0, base='S1', lit='S0', dark='S3', deep='S4')
    p.paint('iron_cap', 'top', lambda w, h: vgrad(w, h, ['S1', 'S0', 'S2'], mid_at=max(1, h // 2)), box=0)
    # the crown block on top of the cap
    p.paint('iron_cap', 'top', lambda w, h: vgrad(w, h, ['S1', 'S0', 'S2'], mid_at=max(1, h // 2)), box=1)
    for f in ('north', 'west', 'east', 'south'):
        p.paint('iron_cap', f, lambda w, h: vgrad(w, h, ['S1', 'S3'], mid_at=1), box=1)
    p.paint('iron_cap', 'bottom', lambda w, h: vgrad(w, h, 'S3'), box=1)

    # ---- the body: the white surcoat over mail, the mail showing at the collar ----
    garment(p, 'body', top='M2', field_code='E2', lit='E1', dark='E3', foot='E4', back='E3')

    # ---- the surcoat skirt: a hem, a fold and a shadow where it meets the leg ----
    p.paint('surcoat_skirt', 'north', skirt_front())
    p.paint('surcoat_skirt', 'south', lambda w, h: vgrad(w, h, ['E2', 'E4'], mid_at=max(1, h - 1)))
    p.paint('surcoat_skirt', 'west', lambda w, h: vgrad(w, h, ['E3', 'E1', 'E4'], mid_at=max(1, h - 1)))
    p.paint('surcoat_skirt', 'east', lambda w, h: vgrad(w, h, ['E3', 'E2', 'E4'], mid_at=max(1, h - 1)))
    p.paint('surcoat_skirt', 'top', lambda w, h: vgrad(w, h, 'E3'))
    p.paint('surcoat_skirt', 'bottom', lambda w, h: vgrad(w, h, 'E4'))
    if stage >= 2:   # the Knight's surcoat is hemmed in gilt
        for f, code in (('north', 'G2'), ('south', 'G2'), ('west', 'G3'), ('east', 'G3')):
            x0, y0, x1, y1 = p.rect('surcoat_skirt', f)
            for x in range(x0, x1):
                p.px[x, y1 - 1] = P[code]

    if stage == 1:
        # ---- the Squire's arms: a mail sleeve on a rounded arm, a leather glove ----
        for arm in ('right_arm', 'left_arm'):
            sleeve = lambda w, h: (cylinder(w, h - 4, ['M0', 'M1', 'M2', 'M3']) +
                                    cylinder(w, 4, ['L0', 'L1', 'L2', 'L3']))
            p.paint(arm, 'north', sleeve)
            p.paint(arm, 'west', sleeve)
            p.paint(arm, 'east', lambda w, h: sleeve(w, h))
            p.paint(arm, 'south', lambda w, h: sleeve(w, h))
            p.paint(arm, 'top', lambda w, h: cylinder(w, h, ['M0', 'M1', 'M2', 'M3']))
            p.paint(arm, 'bottom', lambda w, h: vgrad(w, h, 'L2'))
        # ---- the Squire's legs: mail chausses, leather boots ----
        for leg in ('right_leg', 'left_leg'):
            legwear = lambda w, h: (cylinder(w, h - 5, ['M0', 'M1', 'M2', 'M3']) +
                                     cylinder(w, 5, ['L0', 'L0', 'L1', 'L2']))
            p.paint(leg, 'north', legwear)
            p.paint(leg, 'west', legwear)
            p.paint(leg, 'east', lambda w, h: legwear(w, h))
            p.paint(leg, 'south', lambda w, h: legwear(w, h))
            p.paint(leg, 'top', lambda w, h: vgrad(w, h, 'M2'))
            p.paint(leg, 'bottom', lambda w, h: vgrad(w, h, 'L3'))
    else:
        # ---- the pauldron: the shoulders that tell an armoured troop from a cloth one ----
        pauldron(p, 'S')
        # ---- the Knight's plate arms: a polished pauldron rimmed in gilt, mail at the
        # gap, a gilt couter, the vambrace, a gilt cuff and the steel gauntlet ----
        def arm_plate(w, h):
            out = []
            for j in range(h):
                row = []
                for i in range(w):
                    if j in (3, 9):
                        c = 'G1' if i < w - 1 else 'G2'   # the couter and the cuff
                    elif j == 6:
                        c = 'G0' if i == 1 else ('G1' if i == 2 else 'S1')
                    elif i == 0:
                        c = 'S0'
                    elif i == w - 1:
                        c = 'S2' if j < 8 else 'S3'
                    else:
                        c = 'S1' if j < 8 else 'S2'
                    row.append(c)
                out.append(' '.join(row))
            return out
        for arm in ('right_arm', 'left_arm'):
            limb(p, arm, arm_plate)
            p.paint(arm, 'top', lambda w, h: hgrad(w, h, 'S0', 'S1', 'S2'))
            p.paint(arm, 'bottom', lambda w, h: vgrad(w, h, 'S3'))
        # ---- the Knight's plate legs: cuisses, the poleyn with its gilt boss, the
        # greave, the sabaton ----
        def leg_plate(w, h):
            out = []
            for j in range(h):
                row = []
                for i in range(w):
                    if j == 5:
                        c = 'G0' if i == 1 else ('G1' if i == 2 else 'S1')
                    elif j == 6:
                        c = 'S1' if i < w - 1 else 'S2'
                    elif i == 0:
                        c = 'S0'
                    elif i == w - 1:
                        c = 'S2' if j < 9 else 'S3'
                    else:
                        c = 'S1' if j < 9 else 'S2'
                    row.append(c)
                out.append(' '.join(row))
            return out
        for leg in ('right_leg', 'left_leg'):
            limb(p, leg, leg_plate)
            p.paint(leg, 'top', lambda w, h: vgrad(w, h, 'S2'))
            p.paint(leg, 'bottom', lambda w, h: vgrad(w, h, 'S4'))
    # ---- the Knight-Captain's crest plume (stage 3): a gilt socket, the white plume
    # sweeping back ----
    p.boxfill('crest_plume', {'side': 'G2', 'top': 'G1', 'bottom': 'G3'}, box=0)
    for b in (1, 2, 3):
        p.boxfill('crest_plume', {'side': 'U1', 'top': 'U0', 'bottom': 'U3', 'west': 'U2', 'south': 'U2'}, box=b)
    p.paint('crest_plume', 'north', lambda w, h: vgrad(w, h, ['U0', 'U2']), box=1)
    p.save('knight_s%d.png' % stage)


# =======================================================================================
# The Order Archer (archer): the volley line. Slight, hooded, bow upright, a white
# fletched quiver. The face is in the hood's shadow.
# =======================================================================================
def archer(stage):
    p = Painter('archer')
    head(p, 'young', 'H1')
    # ---- the linen hood: the face opening cut out of the front, the point rising back ----
    def hood_front(w, h):
        """The front of the hood: cloth with the face opening cut out of it, so the
        face shows through in shadow. The opening is the shape the head fills: the
        brow across the top, the cheeks at the sides, the chin below."""
        base = hood_face(w, h)
        out = []
        for j, r in enumerate(base):
            row = r.split()
            if j < 3:
                out.append(' '.join(row))
                continue
            # the opening: two columns of cloth at the sides, the face between
            left, right = 2, w - 3
            for i in range(left, right + 1):
                row[i] = '__'
            out.append(' '.join(row))
        return out

    def hood_face(w, h):
        out = []
        for j in range(h):
            row = []
            for i in range(w):
                if j < 2:
                    c = 'N1'
                elif j < 4:
                    c = 'N2'
                elif j < h - 2:
                    c = 'N3'
                else:
                    c = 'N4'
                row.append(c)
            out.append(' '.join(row))
        return out
    for f in ('west', 'east', 'south'):
        p.paint('linen_hood', f, hood_face, box=0)
    # the front: the cloth with the face opening cut out of it, so the face shows
    p.paint('linen_hood', 'north', lambda w, h: hood_front(w, h), box=0)
    p.paint('linen_hood', 'top', lambda w, h: vgrad(w, h, ['N1', 'N0', 'N2'], mid_at=max(1, h // 2)), box=0)
    p.paint('linen_hood', 'bottom', lambda w, h: vgrad(w, h, 'N4'), box=0)
    for f in ('north', 'west', 'east', 'south'):
        p.paint('linen_hood', f, lambda w, h: vgrad(w, h, ['N1', 'N3'], mid_at=1), box=1)
    p.paint('linen_hood', 'top', lambda w, h: vgrad(w, h, ['N1', 'N0', 'N2'], mid_at=max(1, h // 2)), box=1)
    p.paint('linen_hood', 'bottom', lambda w, h: vgrad(w, h, 'N4'), box=1)
    for f in ('north', 'west', 'east', 'south'):
        p.paint('linen_hood', f, lambda w, h: vgrad(w, h, ['N1', 'N3'], mid_at=1), box=2)
    p.paint('linen_hood', 'top', lambda w, h: vgrad(w, h, ['N1', 'N0', 'N2'], mid_at=max(1, h // 2)), box=2)
    p.paint('linen_hood', 'bottom', lambda w, h: vgrad(w, h, 'N4'), box=2)
    for f in ('north', 'west', 'east', 'south'):
        p.paint('linen_hood', f, lambda w, h: vgrad(w, h, ['N1', 'N3'], mid_at=1), box=3)
    p.paint('linen_hood', 'top', lambda w, h: vgrad(w, h, 'N1'), box=3)
    p.paint('linen_hood', 'bottom', lambda w, h: vgrad(w, h, 'N4'), box=3)

    # ---- the body: the gambeson under the team jerkin ----
    garment(p, 'body', top='N2', field_code='N3', lit='N2', dark='N4', foot='N4', back='N4')

    # ---- the white fletched quiver across the back ----
    for f in ('north', 'west', 'east', 'south'):
        p.paint('white_quiver', f, lambda w, h: vgrad(w, h, ['R0', 'R1', 'R2', 'R3'], mid_at=max(1, h // 6)), box=0)
    p.paint('white_quiver', 'top', lambda w, h: vgrad(w, h, ['R0', 'R1', 'R2'], mid_at=max(1, h // 3)), box=0)
    p.paint('white_quiver', 'bottom', lambda w, h: vgrad(w, h, 'R3'), box=0)
    # the white fletching on top: the brightest value on the archer, so the mark
    # reads over the shoulder the way the spec says it must
    for f in ('north', 'west', 'east', 'south'):
        p.paint('white_quiver', f, lambda w, h: vgrad(w, h, ['U0', 'U1', 'U2'], mid_at=max(1, h // 2)), box=1)
    p.paint('white_quiver', 'top', lambda w, h: vgrad(w, h, ['U0', 'U1', 'U2'], mid_at=max(1, h // 2)), box=1)
    p.paint('white_quiver', 'bottom', lambda w, h: vgrad(w, h, 'U2'), box=1)

    # ---- arms: a gambeson sleeve, a bracer and a glove ----
    for arm in ('right_arm', 'left_arm'):
        p.paint(arm, 'north', lambda w, h: vgrad(w, h, ['N1', 'N3', 'L1', 'L2'], mid_at=max(1, h // 3)))
        p.paint(arm, 'west', lambda w, h: vgrad(w, h, ['N1', 'N3', 'L1', 'L2'], mid_at=max(1, h // 3)))
        p.paint(arm, 'east', lambda w, h: vgrad(w, h, ['N2', 'N4', 'L2', 'L3'], mid_at=max(1, h // 3)))
        p.paint(arm, 'south', lambda w, h: vgrad(w, h, ['N2', 'N4', 'L2', 'L3'], mid_at=max(1, h // 3)))
        p.paint(arm, 'top', lambda w, h: vgrad(w, h, 'N2'))
        p.paint(arm, 'bottom', lambda w, h: vgrad(w, h, 'L2'))
    # ---- legs: leggings and boots ----
    for leg in ('right_leg', 'left_leg'):
        p.paint(leg, 'north', lambda w, h: vgrad(w, h, ['N1', 'N3', 'L0', 'L2'], mid_at=max(1, h // 3)))
        p.paint(leg, 'west', lambda w, h: vgrad(w, h, ['N1', 'N3', 'L0', 'L2'], mid_at=max(1, h // 3)))
        p.paint(leg, 'east', lambda w, h: vgrad(w, h, ['N2', 'N4', 'L1', 'L3'], mid_at=max(1, h // 3)))
        p.paint(leg, 'south', lambda w, h: vgrad(w, h, ['N2', 'N4', 'L1', 'L3'], mid_at=max(1, h // 3)))
        p.paint(leg, 'top', lambda w, h: vgrad(w, h, 'N2'))
        p.paint(leg, 'bottom', lambda w, h: vgrad(w, h, 'L3'))

    # ---- the Sergeant-Archer's signal horn (stage 3) ----
    p.paint('signal_horn', 'north', lambda w, h: vgrad(w, h, 'G1'), box=0)
    p.paint('signal_horn', 'south', lambda w, h: vgrad(w, h, 'G1'), box=0)
    p.paint('signal_horn', 'west', lambda w, h: vgrad(w, h, 'G1'), box=0)
    p.paint('signal_horn', 'east', lambda w, h: vgrad(w, h, 'G1'), box=0)
    p.paint('signal_horn', 'top', lambda w, h: vgrad(w, h, 'G0'), box=0)
    p.paint('signal_horn', 'bottom', lambda w, h: vgrad(w, h, 'G3'), box=0)
    for b in (1, 2, 3, 4):
        p.paint('signal_horn', 'north', lambda w, h: vgrad(w, h, ['U1', 'U2', 'U3'], mid_at=max(1, h // 2)), box=b)
        p.paint('signal_horn', 'south', lambda w, h: vgrad(w, h, ['U1', 'U2', 'U3'], mid_at=max(1, h // 2)), box=b)
        p.paint('signal_horn', 'west', lambda w, h: vgrad(w, h, ['U2', 'U3'], mid_at=1), box=b)
        p.paint('signal_horn', 'east', lambda w, h: vgrad(w, h, ['U1', 'U2'], mid_at=1), box=b)
        p.paint('signal_horn', 'top', lambda w, h: vgrad(w, h, 'U0'), box=b)
        p.paint('signal_horn', 'bottom', lambda w, h: vgrad(w, h, 'U3'), box=b)
    p.save('archer_s%d.png' % stage)


# =======================================================================================
# The Templar (sentinel): the arrow wall. Tall, white plate, the flat-topped great helm,
# the team long tabard with the white cross.
# =======================================================================================
def sentinel(stage):
    p = Painter('sentinel')
    # the closed helm: the face is a dark slit, so no face is painted
    helm_shell(p, 'great_helm', 0, base='S1', lit='S0', dark='S3', deep='S4', crown=True)
    # the painted eye slit and the breath hole
    def helm_front(w, h):
        out = []
        for j in range(h):
            row = []
            for i in range(w):
                if j == 3 and 4 <= i <= 5:
                    c = 'S4'                      # the eye slit
                elif j == 0:
                    c = 'S0'
                elif j < 4:
                    c = 'S1'
                elif j < 8:
                    c = 'S2'
                else:
                    c = 'S3'
                row.append(c)
            out.append(' '.join(row))
        return out
    p.paint('great_helm', 'north', helm_front)
    p.paint('great_helm', 'south', lambda w, h: vgrad(w, h, ['S1', 'S2', 'S4'], mid_at=max(1, h // 3)))
    p.paint('great_helm', 'top', lambda w, h: vgrad(w, h, ['S0', 'S1', 'S2'], mid_at=max(1, h // 3)))

    # ---- the body: white plate over mail, the long tabard over it ----
    garment(p, 'body', top='M2', field_code='S1', lit='S0', dark='S2', foot='S3', back='S2')

    # ---- the Templar's white mantle (stage 3) ----
    def cape_back(w, h):
        out = []
        for j in range(h):
            row = []
            for i in range(w):
                if j == 0:
                    c = 'E0'
                elif j < 3:
                    c = 'E1'
                elif j < h - 2:
                    c = 'E3'
                else:
                    c = 'E4'
                row.append(c)
            out.append(' '.join(row))
        return out
    p.paint('cape', 'north', cape_back, box=0)
    p.paint('cape', 'south', lambda w, h: vgrad(w, h, ['E1', 'E3', 'E4'], mid_at=max(1, h // 3)), box=0)
    p.paint('cape', 'west', lambda w, h: vgrad(w, h, ['E1', 'E3', 'E4'], mid_at=max(1, h // 3)), box=0)
    p.paint('cape', 'east', lambda w, h: vgrad(w, h, ['E2', 'E4'], mid_at=max(1, h // 3)), box=0)
    p.paint('cape', 'top', lambda w, h: vgrad(w, h, ['E0', 'E1', 'E3'], mid_at=max(1, h // 3)), box=0)
    p.paint('cape', 'bottom', lambda w, h: vgrad(w, h, 'E4'), box=0)
    for b in (1, 2):
        p.paint('cape', 'north', lambda w, h: vgrad(w, h, ['E0', 'E1', 'E3', 'E4'], mid_at=max(1, h // 4)), box=b)
        p.paint('cape', 'south', lambda w, h: vgrad(w, h, ['E1', 'E3', 'E4'], mid_at=max(1, h // 3)), box=b)
        p.paint('cape', 'west', lambda w, h: vgrad(w, h, ['E0', 'E3', 'E4'], mid_at=max(1, h // 4)), box=b)
        p.paint('cape', 'east', lambda w, h: vgrad(w, h, ['E2', 'E4'], mid_at=max(1, h // 3)), box=b)
        p.paint('cape', 'top', lambda w, h: vgrad(w, h, ['E0', 'E1', 'E3', 'E4'], mid_at=max(1, h // 4)), box=b)
        p.paint('cape', 'bottom', lambda w, h: vgrad(w, h, 'E4'), box=b)

    # ---- the tabard skirt flaps: the long tabard over the legs ----
    for side in ('tabard_skirt_right', 'tabard_skirt_left'):
        p.paint(side, 'north', lambda w, h: vgrad(w, h, ['E1', 'E3', 'E4'], mid_at=max(1, h // 3)))
        p.paint(side, 'south', lambda w, h: vgrad(w, h, ['E2', 'E3', 'E4'], mid_at=max(1, h // 3)))
        p.paint(side, 'west', lambda w, h: vgrad(w, h, ['E2', 'E3', 'E4'], mid_at=max(1, h // 3)))
        p.paint(side, 'east', lambda w, h: vgrad(w, h, ['E3', 'E4'], mid_at=max(1, h // 3)))
        p.paint(side, 'top', lambda w, h: vgrad(w, h, 'E3'))
        p.paint(side, 'bottom', lambda w, h: vgrad(w, h, 'E4'))

    # ---- the pauldron: the shoulders that tell an armoured troop from a cloth one ----
    pauldron(p, 'S')
    # ---- arms: plate with a great pauldron, a couter, a vambrace and a gauntlet ----
    def arm_plate(w, h):
        out = []
        for j in range(h):
            row = []
            for i in range(w):
                if j in (3, 9):
                    c = 'G1' if i < w - 1 else 'G2'
                elif i == 0:
                    c = 'S0'
                elif i == w - 1:
                    c = 'S2' if j < 8 else 'S3'
                else:
                    c = 'S1' if j < 8 else 'S2'
                row.append(c)
            out.append(' '.join(row))
        return out
    for arm in ('right_arm', 'left_arm'):
        limb(p, arm, arm_plate)
        p.paint(arm, 'top', lambda w, h: hgrad(w, h, 'S0', 'S1', 'S2'))
        p.paint(arm, 'bottom', lambda w, h: vgrad(w, h, 'S4'))
    # ---- legs: cuisses, poleyn, greave, sabaton ----
    def leg_plate(w, h):
        out = []
        for j in range(h):
            row = []
            for i in range(w):
                if j == 5:
                    c = 'G0' if i == 1 else ('G1' if i == 2 else 'S1')
                elif j == 6:
                    c = 'S1' if i < w - 1 else 'S2'
                elif i == 0:
                    c = 'S0'
                elif i == w - 1:
                    c = 'S2' if j < 9 else 'S3'
                else:
                    c = 'S1' if j < 9 else 'S2'
                row.append(c)
            out.append(' '.join(row))
        return out
    for leg in ('right_leg', 'left_leg'):
        limb(p, leg, leg_plate)
        p.paint(leg, 'top', lambda w, h: vgrad(w, h, 'S2'))
        p.paint(leg, 'bottom', lambda w, h: vgrad(w, h, 'S4'))
    p.save('sentinel_s%d.png' % stage)


# =======================================================================================
# The Order Longbow (longbow): the mobile line-breaker. Narrow, peaked felt cap, a
# weathered bearded face, a gilt sash, the tall quiver at the hip.
# =======================================================================================
def longbow(stage):
    p = Painter('longbow')
    head(p, 'weathered', 'H1')
    # ---- the peaked felt cap: a soft crown, a lit top, the bill over the brow ----
    helm_shell(p, 'felt_cap', 0, base='Y1', lit='Y0', dark='Y3', deep='Y3', crown=True)
    p.paint('felt_cap', 'top', lambda w, h: vgrad(w, h, ['Y1', 'Y0', 'Y2'], mid_at=max(1, h // 2)), box=0)
    for f in ('north', 'west', 'east', 'south'):
        p.paint('felt_cap', f, lambda w, h: vgrad(w, h, ['Y1', 'Y3'], mid_at=1), box=1)
    p.paint('felt_cap', 'top', lambda w, h: vgrad(w, h, ['Y1', 'Y0', 'Y2'], mid_at=max(1, h // 2)), box=1)
    p.paint('felt_cap', 'bottom', lambda w, h: vgrad(w, h, 'Y3'), box=1)
    # the bill, cut to a point
    for f in ('north', 'west', 'east', 'south'):
        p.paint('felt_cap', f, lambda w, h: vgrad(w, h, ['Y2', 'Y3'], mid_at=1), box=2)
    p.paint('felt_cap', 'top', lambda w, h: vgrad(w, h, ['Y3', 'Y1', 'Y2'], mid_at=max(1, h // 2)), box=2)
    p.paint('felt_cap', 'bottom', lambda w, h: vgrad(w, h, 'Y3'), box=2)

    # ---- the body: the gambeson under the team jerkin ----
    garment(p, 'body', top='N2', field_code='N3', lit='N2', dark='N4', foot='N4', back='N4')

    # ---- the gilt sash round the waist ----
    for f in ('north', 'south'):
        p.paint('gilt_sash', f, lambda w, h: vgrad(w, h, ['G0', 'G2', 'G3'], mid_at=max(1, h // 3)), box=0)
    for f in ('west', 'east'):
        p.paint('gilt_sash', f, lambda w, h: vgrad(w, h, ['G1', 'G3'], mid_at=1), box=0)
    p.paint('gilt_sash', 'top', lambda w, h: vgrad(w, h, ['G0', 'G1', 'G2'], mid_at=max(1, h // 3)), box=0)
    p.paint('gilt_sash', 'bottom', lambda w, h: vgrad(w, h, 'G3'), box=0)
    for f in ('north', 'south'):
        p.paint('gilt_sash', f, lambda w, h: vgrad(w, h, ['G1', 'G3'], mid_at=max(1, h // 2)), box=1)
    for f in ('west', 'east'):
        p.paint('gilt_sash', f, lambda w, h: vgrad(w, h, ['G2', 'G3'], mid_at=1), box=1)
    p.paint('gilt_sash', 'top', lambda w, h: vgrad(w, h, 'G0'), box=1)
    p.paint('gilt_sash', 'bottom', lambda w, h: vgrad(w, h, 'G3'), box=1)

    # ---- the tall quiver at the hip ----
    for f in ('north', 'west', 'east', 'south'):
        p.paint('tall_quiver', f, lambda w, h: vgrad(w, h, ['L1', 'L2', 'L3', 'L4'], mid_at=max(1, h // 6)), box=0)
    p.paint('tall_quiver', 'top', lambda w, h: vgrad(w, h, ['L1', 'L0', 'L2'], mid_at=max(1, h // 3)), box=0)
    p.paint('tall_quiver', 'bottom', lambda w, h: vgrad(w, h, 'L4'), box=0)
    for f in ('north', 'west', 'east', 'south'):
        p.paint('tall_quiver', f, lambda w, h: vgrad(w, h, ['L2', 'L3'], mid_at=1), box=1)
    p.paint('tall_quiver', 'top', lambda w, h: vgrad(w, h, 'L1'), box=1)
    p.paint('tall_quiver', 'bottom', lambda w, h: vgrad(w, h, 'L3'), box=1)
    # the grey goose fletching
    for f in ('north', 'west', 'east', 'south'):
        p.paint('tall_quiver', f, lambda w, h: vgrad(w, h, ['R2', 'R3'], mid_at=1), box=2)
    p.paint('tall_quiver', 'top', lambda w, h: vgrad(w, h, ['R1', 'R2', 'R3'], mid_at=max(1, h // 3)), box=2)
    p.paint('tall_quiver', 'bottom', lambda w, h: vgrad(w, h, 'R3'), box=2)

    # ---- arms and legs ----
    for arm in ('right_arm', 'left_arm'):
        p.paint(arm, 'north', lambda w, h: vgrad(w, h, ['N1', 'N3', 'L1', 'L2'], mid_at=max(1, h // 3)))
        p.paint(arm, 'west', lambda w, h: vgrad(w, h, ['N1', 'N3', 'L1', 'L2'], mid_at=max(1, h // 3)))
        p.paint(arm, 'east', lambda w, h: vgrad(w, h, ['N2', 'N4', 'L2', 'L3'], mid_at=max(1, h // 3)))
        p.paint(arm, 'south', lambda w, h: vgrad(w, h, ['N2', 'N4', 'L2', 'L3'], mid_at=max(1, h // 3)))
        p.paint(arm, 'top', lambda w, h: vgrad(w, h, 'N2'))
        p.paint(arm, 'bottom', lambda w, h: vgrad(w, h, 'L2'))
    for leg in ('right_leg', 'left_leg'):
        p.paint(leg, 'north', lambda w, h: vgrad(w, h, ['N1', 'N3', 'L0', 'L2'], mid_at=max(1, h // 3)))
        p.paint(leg, 'west', lambda w, h: vgrad(w, h, ['N1', 'N3', 'L0', 'L2'], mid_at=max(1, h // 3)))
        p.paint(leg, 'east', lambda w, h: vgrad(w, h, ['N2', 'N4', 'L1', 'L3'], mid_at=max(1, h // 3)))
        p.paint(leg, 'south', lambda w, h: vgrad(w, h, ['N2', 'N4', 'L1', 'L3'], mid_at=max(1, h // 3)))
        p.paint(leg, 'top', lambda w, h: vgrad(w, h, 'N2'))
        p.paint(leg, 'bottom', lambda w, h: vgrad(w, h, 'L3'))

    # ---- the Master Longbow's long white feather (stage 3) ----
    for f in ('north', 'west', 'east', 'south'):
        p.paint('long_feather', f, lambda w, h: vgrad(w, h, ['U0', 'U2', 'U3'], mid_at=max(1, h // 2)), box=0)
    p.paint('long_feather', 'top', lambda w, h: vgrad(w, h, 'U0'), box=0)
    p.paint('long_feather', 'bottom', lambda w, h: vgrad(w, h, 'U3'), box=0)
    for f in ('north', 'west', 'east', 'south'):
        p.paint('long_feather', f, lambda w, h: vgrad(w, h, ['U0', 'U1', 'U2', 'U3'], mid_at=max(1, h // 4)), box=1)
    p.paint('long_feather', 'top', lambda w, h: vgrad(w, h, 'U0'), box=1)
    p.paint('long_feather', 'bottom', lambda w, h: vgrad(w, h, 'U3'), box=1)
    for f in ('north', 'west', 'east', 'south'):
        p.paint('long_feather', f, lambda w, h: vgrad(w, h, ['U0', 'U3'], mid_at=1), box=2)
    p.paint('long_feather', 'top', lambda w, h: vgrad(w, h, 'U0'), box=2)
    p.paint('long_feather', 'bottom', lambda w, h: vgrad(w, h, 'U3'), box=2)
    p.save('longbow_s%d.png' % stage)


# =======================================================================================
# The Sunweaver (mage): the healer that wins long fights. A white robe in three tiers,
# a deep cowl, the sun staff. He never strikes.
# =======================================================================================
def mage():
    p = Painter('mage')
    head(p, 'old', 'R1')
    # ---- the deep white cowl: the old face set back in it ----
    helm_shell(p, 'white_hood', 0, base='U2', lit='U1', dark='U4', deep='U4', crown=True)
    # the front: the cowl with the face opening cut out of it, so the old face shows
    def cowl_front(w, h):
        out = []
        for j, r in enumerate(vgrad(w, h, ['U1', 'U2', 'U4'], mid_at=max(1, h // 3))):
            row = r.split()
            if j < 3:
                out.append(' '.join(row))
                continue
            for i in range(2, w - 3):
                row[i] = '__'
            out.append(' '.join(row))
        return out
    p.paint('white_hood', 'north', cowl_front, box=0)
    p.paint('white_hood', 'top', lambda w, h: vgrad(w, h, ['U1', 'U0', 'U2'], mid_at=max(1, h // 2)), box=0)

    # ---- the body: the robe, with a shadow under the cowl ----
    garment(p, 'body', top='U2', field_code='U3', lit='U1', dark='U4', foot='U4', back='U4')

    # ---- the robe skirt in three tiers ----
    for b in (0, 1, 2):
        p.paint('robe_skirt', 'north', skirt_front(hem='U4', field='U1', edge='U3'), box=b)
        p.paint('robe_skirt', 'south', lambda w, h: vgrad(w, h, ['U2', 'U4'], mid_at=max(1, h - 1)), box=b)
        p.paint('robe_skirt', 'west', lambda w, h: vgrad(w, h, ['U3', 'U1', 'U4'], mid_at=max(1, h - 1)), box=b)
        p.paint('robe_skirt', 'east', lambda w, h: vgrad(w, h, ['U3', 'U2', 'U4'], mid_at=max(1, h - 1)), box=b)
        p.paint('robe_skirt', 'top', lambda w, h: vgrad(w, h, 'U3'), box=b)
        p.paint('robe_skirt', 'bottom', lambda w, h: vgrad(w, h, 'U4'), box=b)

    # ---- the sunburst brooch ----
    p.paint('sun_brooch', 'north', lambda w, h: vgrad(w, h, ['G0', 'G1', 'G3'], mid_at=max(1, h // 3)))
    p.paint('sun_brooch', 'south', lambda w, h: vgrad(w, h, ['G2', 'G3'], mid_at=1))
    p.paint('sun_brooch', 'west', lambda w, h: vgrad(w, h, ['G2', 'G3'], mid_at=1))
    p.paint('sun_brooch', 'east', lambda w, h: vgrad(w, h, ['G1', 'G2'], mid_at=1))
    p.paint('sun_brooch', 'top', lambda w, h: vgrad(w, h, 'G0'))
    p.paint('sun_brooch', 'bottom', lambda w, h: vgrad(w, h, 'G3'))
    p.save('mage_s1.png')


# =======================================================================================
# Lord Marshal Aldric (gl_marshal): the rally point. Broad, blackened plate under a
# white surcoat, an open helm with a gilt laurel, a grey beard, his banner.
# =======================================================================================
def gl_marshal():
    p = Painter('gl_marshal')
    head(p, 'weathered', 'R1')
    # ---- the open helm: blackened steel, the gilt laurel, the cheek plates ----
    helm_shell(p, 'laurel_helm', 0, base='B1', lit='B0', dark='B3', deep='B4', crown=True)
    p.paint('laurel_helm', 'top', lambda w, h: vgrad(w, h, ['B1', 'B0', 'B2'], mid_at=max(1, h // 2)), box=0)
    for f in ('north', 'west', 'east', 'south'):
        p.paint('laurel_helm', f, lambda w, h: vgrad(w, h, ['B1', 'B3'], mid_at=1), box=1)
    p.paint('laurel_helm', 'top', lambda w, h: vgrad(w, h, ['B1', 'B0', 'B2'], mid_at=max(1, h // 2)), box=1)
    p.paint('laurel_helm', 'bottom', lambda w, h: vgrad(w, h, 'B4'), box=1)
    # the laurel wreath round it, cut into leaves
    def laurel(w, h):
        out = []
        for j in range(h):
            row = []
            for i in range(w):
                row.append(('G0', 'G1', 'G2', 'G3')[(i + j) % 4])
            out.append(' '.join(row))
        return out
    p.paint('laurel_helm', 'north', laurel, box=2)
    p.paint('laurel_helm', 'south', lambda w, h: vgrad(w, h, ['G2', 'G3'], mid_at=1), box=2)
    p.paint('laurel_helm', 'west', lambda w, h: vgrad(w, h, ['G1', 'G3'], mid_at=1), box=2)
    p.paint('laurel_helm', 'east', lambda w, h: vgrad(w, h, ['G0', 'G2'], mid_at=1), box=2)
    p.paint('laurel_helm', 'top', laurel, box=2)
    p.paint('laurel_helm', 'bottom', lambda w, h: vgrad(w, h, 'G3'), box=2)
    # the cheek plates
    for b in (3, 4):
        p.paint('laurel_helm', 'north', lambda w, h: vgrad(w, h, ['B2', 'B3'], mid_at=1), box=b)
        p.paint('laurel_helm', 'south', lambda w, h: vgrad(w, h, ['B3', 'B4'], mid_at=1), box=b)
        p.paint('laurel_helm', 'west', lambda w, h: vgrad(w, h, ['B2', 'B3'], mid_at=1), box=b)
        p.paint('laurel_helm', 'east', lambda w, h: vgrad(w, h, ['B3', 'B4'], mid_at=1), box=b)
        p.paint('laurel_helm', 'top', lambda w, h: vgrad(w, h, 'B2'), box=b)
        p.paint('laurel_helm', 'bottom', lambda w, h: vgrad(w, h, 'B4'), box=b)
    # the neck guard
    p.paint('laurel_helm', 'north', lambda w, h: vgrad(w, h, ['B0', 'B1', 'B3'], mid_at=max(1, h // 3)), box=5)
    p.paint('laurel_helm', 'south', lambda w, h: vgrad(w, h, ['B2', 'B3', 'B4'], mid_at=max(1, h // 3)), box=5)
    p.paint('laurel_helm', 'west', lambda w, h: vgrad(w, h, ['B1', 'B3'], mid_at=1), box=5)
    p.paint('laurel_helm', 'east', lambda w, h: vgrad(w, h, ['B3', 'B4'], mid_at=1), box=5)
    p.paint('laurel_helm', 'top', lambda w, h: vgrad(w, h, ['B0', 'B2'], mid_at=max(1, h // 2)), box=5)
    p.paint('laurel_helm', 'bottom', lambda w, h: vgrad(w, h, 'B4'), box=5)

    # ---- the body: blackened plate under the white surcoat, the mail at the collar ----
    garment(p, 'body', top='M2', field_code='E2', lit='E1', dark='E3', foot='E4', back='E3')

    # ---- the surcoat skirt, wider for the Marshal ----
    p.paint('surcoat_skirt', 'north', skirt_front())
    p.paint('surcoat_skirt', 'south', lambda w, h: vgrad(w, h, ['E2', 'E4'], mid_at=max(1, h - 1)))
    p.paint('surcoat_skirt', 'west', lambda w, h: vgrad(w, h, ['E3', 'E1', 'E4'], mid_at=max(1, h - 1)))
    p.paint('surcoat_skirt', 'east', lambda w, h: vgrad(w, h, ['E3', 'E2', 'E4'], mid_at=max(1, h - 1)))
    p.paint('surcoat_skirt', 'top', lambda w, h: vgrad(w, h, 'E3'))
    p.paint('surcoat_skirt', 'bottom', lambda w, h: vgrad(w, h, 'E4'))

    # ---- the pauldron: the shoulders that tell an armoured troop from a cloth one ----
    pauldron(p, 'B')
    # ---- arms: blackened plate, a great pauldron, a couter, a vambrace ----
    def arm_plate(w, h):
        out = []
        for j in range(h):
            row = []
            for i in range(w):
                if j in (3, 9):
                    c = 'G1' if i < w - 1 else 'G2'
                elif i == 0:
                    c = 'B0'
                elif i == w - 1:
                    c = 'B2' if j < 8 else 'B3'
                else:
                    c = 'B1' if j < 8 else 'B2'
                row.append(c)
            out.append(' '.join(row))
        return out
    for arm in ('right_arm', 'left_arm'):
        limb(p, arm, arm_plate)
        p.paint(arm, 'top', lambda w, h: hgrad(w, h, 'B0', 'B1', 'B2'))
        p.paint(arm, 'bottom', lambda w, h: vgrad(w, h, 'B4'))
    # ---- legs: blackened cuisses and sabatons ----
    def leg_plate(w, h):
        out = []
        for j in range(h):
            row = []
            for i in range(w):
                if j == 5:
                    c = 'G0' if i == 1 else ('G1' if i == 2 else 'B1')
                elif j == 6:
                    c = 'B1' if i < w - 1 else 'B2'
                elif i == 0:
                    c = 'B0'
                elif i == w - 1:
                    c = 'B2' if j < 9 else 'B3'
                else:
                    c = 'B1' if j < 9 else 'B2'
                row.append(c)
            out.append(' '.join(row))
        return out
    for leg in ('right_leg', 'left_leg'):
        limb(p, leg, leg_plate)
        p.paint(leg, 'top', lambda w, h: vgrad(w, h, 'B2'))
        p.paint(leg, 'bottom', lambda w, h: vgrad(w, h, 'B4'))
    p.save('gl_marshal_s1.png')


TROOPS = {
    'knight': lambda: [knight(s) for s in (1, 2, 3)],
    'archer': lambda: [archer(s) for s in (1, 2, 3)],
    'sentinel': lambda: [sentinel(s) for s in (1, 2, 3)],
    'longbow': lambda: [longbow(s) for s in (1, 2, 3)],
    'mage': lambda: [mage()],
    'gl_marshal': lambda: [gl_marshal()],
}

if __name__ == '__main__':
    names = sys.argv[1:] or list(TROOPS)
    for n in names:
        for fn in TROOPS[n]():
            pass
        print('%s: skins written' % n)
