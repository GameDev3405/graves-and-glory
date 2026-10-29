"""Generate the two Gilded flagships (Knight, Templar) at 3 texels a px.

The plan's first wave: reconstruct the two most important Gilded troops as layered miniatures instead of painted
vanilla-proportioned soldiers. It reuses the House lane's proven 3x material kit (`tools/paint_house_troops.py`) and
overrides only its data: the new Gilded parts, the two troop definitions, the materials and the faces. Run this, then
`python tools/build_skins.py` and `python tools/build_skins.py --verify`.

What the kit adds:
  - a kettle helm and an open face for the Knight, a flat-topped great helm for the Templar;
  - layered pauldrons hung from the arms, a breastplate with a fauld, tassets, vambraces, greaves and sabatons;
  - a sculpted cape (Knight stage 3, Templar stage 2) and a crest for the Knight-Captain;
  - heroic bone scales (wider body and shoulders, taller Templar) on the existing biped rig;
  - 3x skins, four faces each, and the existing team chest panel / long tabard so player colour still reads.

No back flags: the Knight and Templar lose the old pennant/standard. The shield stays the game's offhand item in this
wave; the custom shield model is the next technical spike.
"""
import sys
import pathlib

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import paint_house_troops as H
from PIL import Image

ART = H.ART
D = H.D
FD = H.FD
# The two flagships carry more plate than a House troop, so they paint on a 256x128 layout (768x384 at 3x). The rig
# and the face slots keep their old coordinates; only the parts' packed UVs move.
H.SIZE = (256, 128)
SIZE = H.SIZE
SLOTS = H.SLOTS
box = H.box
mirror = H.mirror

# ---------------------------------------------------------------- the Gilded ramps (art style guide, section 1)
STEEL = H.ramp('3A3E4B', '5A616E', '7C8591', 'A6ADB4', 'D6D6D7')
GILT = H.ramp('482E16', '6F5026', '96783A', 'BDAA6A', 'E4D780')
LINEN = H.ramp('82715E', 'A0937B', 'BFB59A', 'DED9C2', 'FCFAEF')
LEATHER = H.ramp('321A15', '462B20', '5A3E2C', '6E553D', '826E52')
IRON = H.ramp('1B1D21', '30343A', '4C5158', '6F757D', 'A3A9B0')
NAVY = H.ramp('0E1320', '18202F', '232E42', '33425A', '4E6078')

# ---------------------------------------------------------------- parts
# A part's boxes are relative to its bone's pivot. The left-side parts are their own ids with mirrored boxes and the
# left bone, because the build refuses a negative scale and the part library names a part's bone.

R_PAULDRON = [
    box([-4.5, -1, -3], [6, 3, 6], 'right cap over the shoulder'),
    box([-5, -1.5, -3.5], [7, 1, 6], 'right rolled rim'),
    box([-4.5, 2, -3], [5, 1, 5], 'right first lame'),
    box([-4, 3, -2.5], [4, 1, 4], 'right second lame'),
    box([-2, -1.5, -3.75], [2, 1, 1], 'right gilt boss'),
]

# A dark cloth sleeve proud of the mail, and a gauntlet that closes the arm at the wrist. Both are geometry, not paint:
# at card size a colour change on a flat arm disappears, but a 0.3 px shoulder step and a 3 px fist do not.
R_SLEEVE = [
    box([-3.3, -2.2, -2.4], [5, 5, 5], 'right sleeve'),
]

R_GAUNTLET = [
    box([-1.6, 0, -1.6], [3, 3, 3], 'right fist'),
    box([-2, 0.6, -2], [4, 1, 4], 'right knuckle plate'),
]

PARTS = {
    'g_kettle_helm': ('head', True, 'The Gilded kettle helm: a stepped dome under a wide brim, a comb along the crown, '
                                    'a rear neck flare, a nasal bar and two cheek plates that sit on the skull (not 0.5 px off it, '
                                    'which read as bare skin at 45 degrees); the face stays open under the brim.', [
        box([-5, -10.5, -5], [10, 2, 10], 'crown band'),
        box([-4.5, -8.8, -4.5], [9, 1, 9], 'dome lower'),
        box([-3.5, -7.6, -3.5], [7, 1, 7], 'dome upper'),
        box([-6.5, -6.9, -5.4], [13, 1, 10], 'wide brim'),
        box([-4, -5.9, 5.0], [8, 2, 1], 'rear neck flare'),
        box([-0.5, -6.4, -5.9], [1, 2, 1], 'nasal bar'),
        box([-4.6, -6.6, -4.4], [1, 3, 8], 'right cheek plate'),
        box([3.6, -6.6, -4.4], [1, 3, 8], 'left cheek plate'),
        box([-0.6, -11, -3.5], [1, 1, 7], 'comb'),
    ]),
    'g_great_helm': ('head', True, 'The Gilded great helm: a flat stepped crown, a raised comb set into it, a brow band over a '
                                   'recessed sight slit split by a nose bar, a stepped chin plate, a gilt cross proud of the chin, '
                                   'side vents and a neck guard. It closes the face completely.', [
        box([-5, -9, -5], [10, 8, 10], 'shell'),
        box([-4.5, -10, -4.5], [9, 1, 9], 'top step'),
        box([-3.5, -11, -3.5], [7, 1, 7], 'flat crown'),
        box([-1, -11.6, -4], [2, 1, 8], 'comb ridge'),
        box([-4.5, -8.6, -5.5], [9, 1, 1], 'brow band'),
        box([-3, -7.4, -5.3], [6, 1, 1], 'visor slit'),
        box([-0.5, -7.4, -6.0], [1, 3, 1], 'nose bar'),
        box([-2.5, -4.2, -5.7], [5, 2, 1], 'chin plate'),
        box([-0.5, -4.4, -6.2], [1, 2, 1], 'cross upright'),
        box([-1.5, -3.8, -6.2], [3, 1, 1], 'cross arm'),
        box([-5.5, -0.8, -4.5], [11, 2, 9], 'neck guard'),
        box([-5.6, -5.6, -2.4], [1, 2, 2], 'right vent'),
        box([4.6, -5.6, -2.4], [1, 2, 2], 'left vent'),
    ]),
    'g_mail_coif': ('head', False, 'The mail coif under the helm: a dark ring at the neck and the jaw, so the head sits in the '
                                   'armour instead of on it.', [
        box([-4.6, -0.4, -4.6], [9, 1, 9], 'neck ring'),
    ]),
    'g_pauldron': ('right_arm', False, 'The Gilded layered pauldron, right: a plate over the shoulder with a rolled rim and two '
                                       'lames below it. It widens the armoured silhouette and moves with the arm.', R_PAULDRON),
    'g_pauldron_left': ('left_arm', False, 'The Gilded layered pauldron, left.', mirror(R_PAULDRON)),
    'g_breastplate': ('body', False, 'The Gilded breastplate over the mail: a chest plate, a fauld at the waist and a gorget '
                                     'collar, each a little proud of the body.', [
        box([-4.5, -12, -3], [9, 7, 5], 'breastplate'),
        box([-4.5, -5, -3], [9, 1, 5], 'fauld'),
        box([-3.5, -12.5, -2.5], [7, 1, 5], 'gorget'),
    ]),
    'g_tasset_right': ('right_leg', False, 'A short plate tasset over the right thigh; the thigh and knee stay visible below it.', [
        box([-2, -1, -2.5], [4, 2, 5], 'tasset'),
    ]),
    'g_tasset_left': ('left_leg', False, 'A short plate tasset over the left thigh.', [
        box([-2, -1, -2.5], [4, 2, 5], 'tasset'),
    ]),
    'g_belt': ('body', False, 'The Gilded sword belt: a broad leather belt, a gilt buckle and a pouch.', [
        box([-4.5, -1.5, -3], [9, 2, 6], 'belt'),
        box([-1, -2, -3.5], [2, 3, 1], 'buckle'),
        box([2, -1, -3.5], [2, 2, 1], 'pouch'),
    ]),
    'g_vambrace': ('right_arm', False, 'The Gilded steel vambrace and cuff on the right forearm.', [
        box([-3.5, 6, -2.5], [5, 3, 5], 'vambrace'),
        box([-4, 5.5, -3], [6, 1, 6], 'cuff'),
    ]),
    'g_vambrace_left': ('left_arm', False, 'The Gilded steel vambrace and cuff on the left forearm.', [
        box([-1.5, 6, -2.5], [5, 3, 5], 'vambrace'),
        box([-2, 5.5, -3], [6, 1, 6], 'cuff'),
    ]),
    'g_sleeve': ('right_arm', False, 'A dark cloth sleeve over the right upper arm, proud of the torso so the arm reads as a separate '
                                     'shape even before the pauldron.', R_SLEEVE),
    'g_sleeve_left': ('left_arm', False, 'A dark cloth sleeve over the left upper arm.', mirror(R_SLEEVE)),
    'g_gauntlet': ('right_hand', False, 'The right gauntlet fist and knuckle plate at the wrist.', R_GAUNTLET),
    'g_gauntlet_left': ('left_hand', False, 'The left gauntlet fist and knuckle plate at the wrist.', mirror(R_GAUNTLET)),
    'g_greave': ('right_leg', False, 'The Gilded greave and poleyn on the right leg; slimmer than the thigh so the legs part in '
                                      'silhouette.', [
        box([-2, 5, -2], [4, 5, 4], 'greave'),
        box([-2, 3.5, -2.5], [4, 2, 1], 'poleyn'),
    ]),
    'g_greave_left': ('left_leg', False, 'The Gilded greave and poleyn on the left leg.', [
        box([-2, 5, -2], [4, 5, 4], 'greave'),
        box([-2, 3.5, -2.5], [4, 2, 1], 'poleyn'),
    ]),
    'g_sabaton': ('right_leg', False, 'The Gilded sabaton toe on the right foot.', [
        box([-2, 10, -3], [4, 2, 2], 'sabaton'),
        box([-2, 11, -3.5], [4, 1, 1], 'toe'),
    ]),
    'g_sabaton_left': ('left_leg', False, 'The Gilded sabaton toe on the left foot.', [
        box([-2, 10, -3], [4, 2, 2], 'sabaton'),
        box([-2, 11, -3.5], [4, 1, 1], 'toe'),
    ]),
    'g_surcoat': ('body', False, 'The Gilded surcoat skirt: two white panels from the belt to the mid thigh, butted against '
                                 'the team pale so the pale reads as a stripe down the cloth, and cut high so thigh, knee and '
                                 'greave all read below it.', [
        box([-4.85, -2.6, -2.55], [3, 7, 5], 'surcoat skirt right'),
        box([1.85, -2.6, -2.55], [3, 7, 5], 'surcoat skirt left'),
    ]),
    'g_cape': ('body', False, 'The Gilded mantle: a yoke across the shoulder blades over two split tails, each a two-step '
                              'fold with a raised outer edge, a dark hem and gilt trim, the centre open over the team tabard so the '
                              'leg split reads from behind. It hangs to the knee. Static, no pole, no flag.', [
        box([-5, -12.6, 2.3], [10, 2, 1], 'shoulder yoke'),
        box([-4.6, -10.8, 2.3], [3, 8, 1], 'left upper tail'),
        box([-4.8, -2.8, 2.5], [3, 6, 1], 'left lower tail'),
        box([1.6, -10.8, 2.3], [3, 8, 1], 'right upper tail'),
        box([1.8, -2.8, 2.5], [3, 6, 1], 'right lower tail'),
        box([-4.6, -10.8, 3.1], [1, 14, 1], 'left trim'),
        box([3.6, -10.8, 3.1], [1, 14, 1], 'right trim'),
        box([-2.6, -10.8, 2.2], [1, 15, 1], 'left inner fold'),
        box([1.6, -10.8, 2.2], [1, 15, 1], 'right inner fold'),
        box([-4.6, 3.2, 2.5], [3, 1, 2], 'left hem'),
        box([1.6, 3.2, 2.5], [3, 1, 2], 'right hem'),
    ]),
    'g_crest': ('head', True, 'The Knight-Captain crest: a gilt socket and a white plume stepping back in three tapering blocks, '
                              'so the profile reads as a swept plume rather than a post.', [
        box([-1.5, -11.4, -2.5], [3, 1, 3], 'socket'),
        box([-1, -12.6, -2], [2, 1, 5], 'plume base'),
        box([-0.5, -13.6, 1], [2, 1, 4], 'plume mid'),
        box([0, -14.6, 3], [1, 1, 4], 'plume tip'),
    ]),
}

# The troop json parts list: (part, [stages]) or a bare id for every stage.
TROOPS = {
    'knight': {
        'note': 'Gilded Order Squire > Knight > Knight-Captain (unit identity, Gilded): husk, biped, heroic scale; the kettle '
                'helm over a mail coif, a short split surcoat under a team chest panel, dark sleeves, steel vambraces and '
                'gauntlets, greaves and sabatons; stage 2 pauldrons, breastplate and short tassets; stage 3 crest and folded '
                'cape. Arming sword, the shield on the left arm. No back flag.',
        'bones': {'body': {'scale': [1.12, 1.0, 1.12]},
                  'right_arm': {'pivot': [-6.3, -10.6, 0], 'scale': [1.08, 1.0, 1.08]},
                  'left_arm': {'pivot': [6.3, -10.6, 0], 'scale': [1.08, 1.0, 1.08]},
                  'right_leg': {'pivot': [-3.2, 12, 0], 'scale': [1.05, 1.08, 1.05]},
                  'left_leg': {'pivot': [3.2, 12, 0], 'scale': [1.05, 1.08, 1.05]}},
        'idleArms': 'hang',
        'parts': ['g_kettle_helm', 'g_mail_coif', 'g_belt', 'g_surcoat', 'g_sleeve', 'g_sleeve_left',
                  'g_gauntlet', 'g_gauntlet_left', 'g_vambrace', 'g_vambrace_left', 'g_greave', 'g_greave_left',
                  'g_sabaton', 'g_sabaton_left',
                  ('g_pauldron', [2, 3]), ('g_pauldron_left', [2, 3]), ('g_breastplate', [2, 3]),
                  ('g_tasset_right', [2, 3]), ('g_tasset_left', [2, 3]), ('g_crest', [3]), ('g_cape', [3])],
        'team': [{'part': 'chest_panel'}],
        'weapon': 'arming_sword', 'portrait': 'standard',
    },
    'sentinel': {
        'note': 'Gilded Templar > Templar Knight > Templar Commander (unit identity, Gilded): husk, biped, tall and broad; the '
                'stepped great helm with a comb, brow, visor snout and gilt cross, a team long tabard over heroic plate, '
                'pauldrons, short tassets, dark sleeves, steel vambraces and gauntlets, greaves and sabatons; stage 2 cape; '
                'stage 3 gold filigree and crest. Longsword, the shield on the left arm. No back flag.',
        'bones': {'body': {'scale': [1.18, 1.05, 1.18]},
                  'right_arm': {'pivot': [-6.4, -10.6, 0], 'scale': [1.12, 1.05, 1.12]},
                  'left_arm': {'pivot': [6.4, -10.6, 0], 'scale': [1.12, 1.05, 1.12]},
                  'right_leg': {'pivot': [-3.3, 12, 0], 'scale': [1.08, 1.08, 1.08]},
                  'left_leg': {'pivot': [3.3, 12, 0], 'scale': [1.08, 1.08, 1.08]}},
        'idleArms': 'hang',
        'parts': ['g_great_helm', 'g_mail_coif', 'g_breastplate', 'g_belt', 'g_sleeve', 'g_sleeve_left',
                  'g_gauntlet', 'g_gauntlet_left', 'g_pauldron', 'g_pauldron_left', 'g_tasset_right', 'g_tasset_left',
                  'g_vambrace', 'g_vambrace_left', 'g_greave', 'g_greave_left', 'g_sabaton', 'g_sabaton_left',
                  ('g_cape', [2, 3]), ('g_crest', [3])],
        'team': [{'part': 'long_tabard'}],
        'weapon': 'longsword', 'portrait': 'tall 4',
    },
}

# ---------------------------------------------------------------- materials
def steel_mats(stage, templar):
    # Gilt rivets only on the third tier: at stage 2 a gilt stud on every five texels of every plate read as gold
    # measles in the live client, and the plan keeps House gold under a fifth of the surface.
    plate = H.rivet_metal(STEEL, GILT if stage >= 3 else IRON, every=7,
                          rim=GILT if stage >= 3 else None, base=0.58)
    smooth = H.metal(STEEL, rim=GILT if stage >= 3 else None, seam=4, base=0.58)
    mail = H.mail(IRON)
    gold = H.metal(GILT, base=0.6)
    leather = H.leather(LEATHER, stitch=H.tone(GILT, 0.7))
    ivory = H.cloth_skin(LINEN, 6, 0.55)
    cape_center = H.cloth_skin(LINEN, 2, 0.55)
    cape_side = H.cloth_skin(LINEN, 2, 0.28)
    cape_hem = H.solid(LINEN, 0.22)
    navy = H.cloth_skin(NAVY, 4, 0.42)
    deep = H.solid(IRON, 0.28)
    # The sleeve is the value break at the shoulder: near-black navy against the bright breastplate, then a steel
    # vambrace and fist. A mail upper arm on a mail torso reads as one slab at card size.
    arm = H.limb(navy, mail, 0.55)
    leg = H.limb(navy, mail, 0.40)
    return {
        'body': H.mail(IRON) if not templar else plate,
        'right_arm': arm, 'left_arm': arm,
        'right_leg': leg, 'left_leg': leg,
        'g_kettle_helm': H.by_note({'nasal': gold}, plate),
        'g_great_helm': H.by_note({'cross': gold, 'ridge': gold, 'visor': deep, 'vent': deep}, plate),
        'g_mail_coif': mail,
        'g_pauldron': H.by_note({'boss': gold}, plate),
        'g_pauldron_left': H.by_note({'boss': gold}, plate),
        'g_breastplate': H.by_note({'gorget': gold, 'fauld': smooth}, plate),
        'g_tasset_right': smooth,
        'g_tasset_left': smooth,
        'g_belt': H.by_note({'buckle': gold, 'pouch': leather}, leather),
        'g_vambrace': smooth,
        'g_vambrace_left': smooth,
        'g_sleeve': navy,
        'g_sleeve_left': navy,
        'g_gauntlet': H.by_note({'knuckle': smooth}, leather),
        'g_gauntlet_left': H.by_note({'knuckle': smooth}, leather),
        'g_greave': smooth,
        'g_greave_left': smooth,
        'g_sabaton': smooth,
        'g_sabaton_left': smooth,
        'g_cape': H.by_note({'trim': gold, 'fold': cape_side, 'tail': cape_center, 'hem': cape_hem}, cape_center),
        'g_crest': H.by_note({'socket': gold}, H.solid(LINEN, 0.7)),
        'g_surcoat': ivory,
    }


def knight_mats(stage):
    return steel_mats(stage, False)


def templar_mats(stage):
    return steel_mats(stage, True)


# ---------------------------------------------------------------- faces
FACE_SETS = {
    'knight': [
        (H.hexc('B9856A'), H.hexc('6E4A32'), H.hexc('2A3440')),
        (H.hexc('A06E54'), H.hexc('8A5A3A'), H.hexc('34402A')),
        (H.hexc('C49478'), H.hexc('3E2A20'), H.hexc('2A3440')),
        (H.hexc('9C6A50'), H.hexc('B0A79A'), H.hexc('3A2A20')),
    ],
    'sentinel': [
        (H.hexc('A87A60'), H.hexc('6E4A32'), H.hexc('2A3440')),
        (H.hexc('9C6A50'), H.hexc('8E8A84'), H.hexc('3A2A20')),
        (H.hexc('B88E74'), H.hexc('3E2A20'), H.hexc('34402A')),
        (H.hexc('8E6048'), H.hexc('B0A79A'), H.hexc('2A3440')),
    ],
}


# ---------------------------------------------------------------- faces
# paint_house_troops' face painter has House-specific faces and a generic fallback that paints a four-texel dark eye
# band. The Knight's kettle helm is open, so that fallback read as a blindfold in the live client. These two faces are
# painted here instead: one-texel eyes with skin between them, a brow, a nose and a mouth.
def gilded_face_px(troop, n, x, y, skin, hair, eye):
    if y == 0:
        return H.mul(hair, 0.9) if x not in (0, 7) else skin
    if y == 1:
        return H.mix(hair, skin, 0.45) if 1 <= x <= 6 else skin
    if y == 2:
        return H.mul(skin, 0.92)
    if y == 3:
        if x in (2, 5):
            return eye
        if x in (1, 6):
            return H.mul(skin, 0.72)
        return skin
    if y == 4 and x in (3, 4):
        return H.mul(skin, 0.86)
    if y == 5 and x in (1, 6):
        return H.mul(skin, 0.9)
    if y == 6 and x in (3, 4):
        return H.mul(skin, 0.55)
    if y == 7:
        return H.mul(skin, 0.94)
    return skin


H.face_px = gilded_face_px

# ---------------------------------------------------------------- run
H.PARTS = PARTS
H.TEAM = {}
H.TROOPS = TROOPS
H.FACE_SETS = FACE_SETS


def build_sources():
    """Write the part library and the two troop jsons. A part is packed once and shared by both troops: the part json
    holds absolute sheet UVs and every troop entry points at [0, 0], so the second troop cannot overwrite the first's UVs."""
    reserve = H.rig_rects() + [(0, 0, 32, 16)] + [(x, y, x + 22, y + 11) for x, y in H.SLOTS]
    pk = H.Packer(reserve)
    names = []
    for t in TROOPS.values():
        for p in t['parts']:
            n = p if isinstance(p, str) else p[0]
            if n not in names:
                names.append(n)
    boxes_uv = {}
    items = sorted(((n, i, b) for n in names for i, b in enumerate(PARTS[n][3])),
                   key=lambda e: -(2 * (e[2]['size'][0] + e[2]['size'][2])) * (e[2]['size'][2] + e[2]['size'][1]))
    for n, i, b in items:
        boxes_uv[(n, i)] = pk.place(b['size'])
    for n in names:
        bone, helm, note, bxs = PARTS[n]
        H.write_json(ART / 'parts' / (n + '.json'), {
            'part': n, 'note': note, 'bone': bone, 'team': False, 'tag': 'helm' if helm else '',
            'boxes': [{'origin': b['origin'], 'size': b['size'], 'uv': list(boxes_uv[(n, i)]), 'note': b['note']}
                      for i, b in enumerate(bxs)]})
    for troop, t in TROOPS.items():
        entries = []
        for p in t['parts']:
            n, stages = (p, None) if isinstance(p, str) else p
            e = {'part': n, 'uv': [0, 0]}
            if stages:
                e['stages'] = stages
            entries.append(e)
        src = {'troop': troop, 'note': t['note'], 'rig': 'biped', 'size': list(SIZE), 'density': D}
        if t.get('bones'):
            src['bones'] = t['bones']
        if 'hunch' in t:
            src['hunch'] = t['hunch']
        src.update({'idleArms': t['idleArms'], 'parts': entries + t['team'],
                    'skins': {str(s): '%s_s%d.png' % (troop, s) for s in (1, 2, 3)},
                    'weapon': t['weapon'], 'hand': 'right', 'portrait': t['portrait'],
                    'faces': {'density': FD, 'count': 4, 'slots': H.SLOTS}})
        H.write_json(ART / 'troops' / (troop + '.json'), src)


def main():
    build_sources()
    for troop in TROOPS:
        for s in (1, 2, 3):
            path = ART / 'troops' / ('%s_s%d.png' % (troop, s))
            if not path.is_file():
                Image.new('RGBA', (SIZE[0] * D, SIZE[1] * D), (0, 0, 0, 0)).save(path)
        path = ART / 'troops' / ('%s_faces.png' % troop)
        if not path.is_file():
            Image.new('RGBA', (64, 128), (0, 0, 0, 0)).save(path)
        H.paint_skins(troop)
        H.paint_faces(troop)
    print('paint_gilded_flagships: wrote knight and sentinel at %d texels a px' % D)


H.MATS = {'knight': knight_mats, 'sentinel': templar_mats}

if __name__ == '__main__':
    main()
