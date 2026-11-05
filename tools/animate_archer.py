"""Renders and exports the Thornhide archer: the Antlerbow, an arrow and a hip quiver on the articulated rig.

  python tools/animate_archer.py                 everything: the reel, the GIF, the beat sheets and the exports
  python tools/animate_archer.py --check         feet, smoothness, clipping and hand-on-string report
  python tools/animate_archer.py --sheet shoot 0.6 --out DIR    three large views per time
  python tools/animate_archer.py --strip shoot 0.9 1.2 --out DIR   consecutive 60 fps frames, tiled

Writes art/animations/thornhide_archer/ (see its README). The driver is animate_combat's, with this unit and show.
"""
import pathlib
import sys

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import animate_combat as AC                                                  # noqa: E402
import animate_unit as AU                                                    # noqa: E402
from player_armor import thornhide as ARMOUR                                 # noqa: E402
from player_weapons import thornhide as WEAPONS                              # noqa: E402
from unit_anim import accessories as ACC                                     # noqa: E402
from unit_anim import archer_clips as XC                                     # noqa: E402
from unit_anim import clearance as CL                                        # noqa: E402
from unit_anim import combat_rig as CR                                       # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / 'art' / 'animations' / 'thornhide_archer'

FOLLOW = {'cloak': 'cape', 'feather': 'plume', 'loincloth': 'loin'}


def _bone_of(box):
    for prefix, bone in FOLLOW.items():
        if box.name.startswith(prefix):
            return bone
    return box.bone


class ArcherUnit:
    """The Thornhide harness cut at the joints, the placeholder body, the quiver on the hips; the bow and the arrow
    are drawn per frame (the bow bends with the draw, the arrow comes and goes)."""

    def __init__(self, rarity='common', team='crimson'):
        self.rarity = rarity
        aset = ARMOUR.build()
        img, glow = aset.texture(rarity, team)
        _, _, pos = aset.pack()
        tex, gl = np.array(img), np.array(glow)[..., 3] > 0
        tier = 'legendary' if rarity == 'legendary' else 'base'
        parts = [(b, {f: (tex, gl, pos[(b.name, f)], AU.SHADE[f]) for f in AU.FACES if f not in b.hide}, _bone_of(b), None)
                 for b in aset.boxes_for(tier)]
        parts += AU.body_parts()
        self.parts = CR.split_parts(parts)
        self.quiver = AU.item_parts(ACC.QUIVER, rarity, 'body', XC.QUIVER_LOCAL)
        self.arrow = AU.item_parts(ACC.ARROW, rarity, 'right_hand', XC.ARROW_LOCAL)
        self.bow_rest = AU.item_parts({w.id: w for w in WEAPONS.weapons()}['antlerbow'], rarity, 'left_hand',
                                      XC.BOW_LOCAL, skip=('arrow',))

    def drawables_at(self, pose, clip, t):
        out = [(b, f, pose[bone]) for b, f, bone, _ in self.parts]
        out += [(b, f, pose[bone] @ local) for b, f, bone, local in self.quiver]
        d, P, D, _, _ = XC.item_state(clip, t)
        bow_M = pose['left_hand'] @ XC.BOW_LOCAL
        out += [(b, f, bow_M) for b, f in ACC.bow_drawn(d, self.rarity)]
        if XC.arrow_visible(clip, t):
            out += [(b, f, pose[bone] @ local) for b, f, bone, local in self.arrow]
        M = XC.flight(clip, t)
        if M is not None:
            out += [(b, f, M) for b, f, _, _ in self.arrow]
        return out

    def drawables(self, pose):
        raise TypeError('the archer is drawn per frame: use drawables_at')

    def export_parts(self):
        out = [(b, f, bone, None, 'armour') for b, f, bone, _ in self.parts if not b.name.startswith('p_')]
        for name, parts in (('hip_quiver', self.quiver), ('antlerbow', self.bow_rest), ('arrow', self.arrow)):
            out += [(b, f, bone, local, name) for b, f, bone, local in parts]
        return out

    def clipper(self):
        parts = [(b, bone, None, None) for b, f, bone, _ in self.parts]
        parts += [(b, 'quiver', local, 'quiver') for b, f, bone, local in self.quiver]
        parts += [(b, 'bow', local, 'bow') for b, f, bone, local in self.bow_rest]
        parts += [(b, 'arrow', local, 'arrow') for b, f, bone, local in self.arrow]
        return CL.Clipper(parts, held={'bow': {'grip'}, 'arrow': {'nock', 'vane_l', 'vane_r', 'vane_c'}, 'quiver': set()},
                          exclude=(('bow', 'limb_0', 'left_forearm'),       # the riser passes through the closed fist
                                   ('bow', 'string', 'left_forearm'),     # the string brushes the bracer: its job
                                   ('bow', 'string', 'right_forearm'),    # the drawing fingers hold the string
                                   ('quiver', 'fletch', 'right_forearm'),  # the hand among the fletchings, choosing one
                                   ('quiver', 'shaft', 'right_forearm'),
                                   ('quiver', 'mouth', 'right_forearm'),   # ... and at the quiver's mouth
                                   ('arrow', 'shaft', 'left_forearm')),    # the nocked arrow rests on the bow hand
                          soft=CL.SOFT + ('cloak', 'loincloth', 'feather'))


def reel(clips, fps=60):
    c = {x.name: x for x in clips}
    plan = []
    AC.run_into(plan, c['archer_idle'])
    AC.run_into(plan, c['archer_kite'], times=2)
    AC.run_into(plan, c['archer_shoot'])
    AC.run_into(plan, c['archer_shoot'])
    AC.run_into(plan, c['archer_shoot'], 0.35)
    AC.run_into(plan, c['archer_hit'])
    AC.run_into(plan, c['archer_hit'], 0.5)
    AC.run_into(plan, c['archer_idle'])
    return plan


def data_extra(c):
    return {'string_draw': [[round(t, 4), round(d, 3), ACC.frame_for(d)] for t, d in c.draws],
            'arrow_in_hand': [[t, v] for t, v in c.arrow_keys], 'loose': c.loose}


SHOW = AC.Show(
    prefix='archer', out=OUT, title='Thornhide archer  ·  Antlerbow', model='archer_thornhide',
    unit=ArcherUnit, clips=lambda: [f() for f in XC.CLIPS],
    titles={'archer_idle': 'low ready', 'archer_shoot': 'draw, loose and reload', 'archer_kite': 'backpedal at ready',
            'archer_hit': 'hit reaction'},
    sheet_caption='bow side above, draw side below', idle='archer_idle',
    java_header=['// The Thornhide archer, written by tools/animate_archer.py in the TroopClips builder BipedClips uses. These',
                 '// clips need the articulated troop model (chest, forearms, shins and feet; pivots in archer_clips.json).',
                 '// The bow swaps its rest/pull_0..2 frames by the string draw in archer_clips.json, as a vanilla bow does.',
                 '// Not wired: gameplay-visible, it waits on Kort\'s call.'],
    follow_note='// The follow parts the preview simulates: cape (the cloak), plume (the feather fan), loin (the loincloth):',
    reel=reel, data_extra=data_extra)


if __name__ == '__main__':
    sys.exit(AC.main(show=SHOW))
