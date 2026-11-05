"""Builds the troops' own models and skins (Phase 2 Task 5) from art/ into src/main/resources/assets/gravesandglory/:
models/troop/<troop>.json, rig/<rig>.json, weapon/<id>.json and index.json (every troop, rig, weapon and output), and
textures/entity/troop/<troop>_s<stage>.png (the source skin with its relief layer pasted over it), _veteran (the Mastery II
trim overlay), _e (glow),
_o (outer layer), weapon/<id>.png (the frames packed left to right, and _e), and one team_cloth_<side>.png per factions row
plus team_cloth_linen.png (no side) and team_cloth_company.png (the company's navy, which portraits wear: unit identity
rule 8 as Kort ruled it, Needs Kort 11).

  python tools/build_skins.py                     export everything, and remove the outputs the previous index.json
                                                  listed that are no longer built (never a file no index listed)
  python tools/build_skins.py --verify            rebuild into a temp dir and compare: a PNG by its size and RGBA
                                                  pixels (another Pillow or zlib may encode the same pixels in other
                                                  bytes), anything else byte for byte; exit 1 naming the first
                                                  difference
  python tools/build_skins.py --to-bbmodel <troop>  write art/troops/<troop>.bbmodel: one display group turned 180 about
                                                  x (Blockbench y is up, the rig y down), a group per rig bone and
                                                  part, box-UV skin cubes. From then on the build takes the troop's
                                                  skin geometry from the project; the json keeps scales and the
                                                  parts. It refuses to overwrite: the project is the source after
                                                  the export, so the json's geometry keys are deleted in the same
                                                  commit (art pipeline v2 section 2.3)
  python tools/build_skins.py --tiers             print per troop its armour and weapon tiers I to III and the tier each
                                                  draws (III->II: tier III's art is missing, it draws tier II's), then the
                                                  stopgap list, which tools/package.py refuses while it is not empty
  python tools/build_skins.py --template <troop>  write work/troops/<troop>_template.png: every skin box face of the
                                                  troop in a flat colour, the sheet to paint over, and
                                                  <troop>_relief_template.png: where relief may stand (design 7)
  python tools/build_skins.py --turn <shots> <out>  build the turnaround sheets turn-<troop>.png and turn-all.png from
                                                  the suite turn's shots p2-turn-<troop>-<pose>-<angle>.png
  python tools/build_skins.py --contact <shots> <out.png>  build the tell contact sheet from the suite tells' shots
                                                  p2-tells-<troop>.png: a row a House and a column a troop, in the houses
                                                  order of sheets/design.json, a missing shot a grey cell

Sources (sizes in model pixels, y down, the ground at y 24, angles in degrees; a "note" key is allowed anywhere):
  art/rigs/<rig>.json      rig, size [w,h], outer (the outer twin's dilation), hands {right, left} (bone names), bones[]
                           of {name, parent, pivot, rotation?, boxes[{origin, size, uv, mirror?}]}, parents first. A pivot
                           is relative to the parent's pivot, a box origin to its own bone's pivot (the ModelPart way).
  art/parts/<part>.json    part, bone (the default anchor), team, tag ("" or "helm"), pivot? and rotation? (in the
                           anchor's frame), scale? (the part's own, times the troop's for it), boxes[]. Box UVs are
                           relative to the troop's uv for the part, except that a team part's UVs are absolute on the
                           cloth sheet.
  art/weapons/<id>.json    weapon, density? (a .bbmodel weapon: texels a px, 2 unless it says; a sprite takes none), thickness (1 to 3 px), grip {pixel [x, y], offset?, rotation?} (the grip pixel's centre
                           sits on the hand bone's pivot; offset and rotation place the sprite in its frame), pull? (three
                           times in seconds of bow_draw) with nocks (the string's middle in sprite px, a pair a frame).
                           The art is <id>.png (rest), <id>_pull_0..2.png with pull, and for a lit weapon <id>_e.png: the
                           build extrudes the sprites (design 2 of the troop depth design).
                           A weapon's tiers II and III (in-match progression design 4.4) are weapons of their own,
                           art/weapons/<id>_t2 and <id>_t3 under the same rules; a troop names its tier I <id>, and a
                           missing tier draws the highest tier below it.
  art/troops/<troop>.json  troop, rig, size?, density? (1 to 4, 1 unless it says), bones {<name>: {pivot?, boxes?, scale?}}, hunch? (biped), idleArms?,
                           parts[] of {part, name?, bone?, uv, offset?, rotation?, scale?, stages?, weaponTiers?},
                           skins {<armour tier>: file}, veteran?, glow?, glowFrom?, outer?, weapon (an id or none), hand, portrait,
                           faces? {count (1 to 8), slots [[x, y] x count-1], card?, fixed?, beard? {slots [[x, y] x count-1]}, note?}: the
                           troop's face variants (below). A skin and a part's stages name armour tiers (1 to 3, the gearTiers rows); a part's
                           weaponTiers name the weapon tiers it shows at (the weapon parts of a troop with no held weapon,
                           and the Ballista Crew's frame). A tier with no skin of its own draws the highest own skin below.
  art/troops/<troop>_faces.png  the face patches of a faces troop: patches stacked, each the head box's rectangle of the
                           sheet (32x16 px for the 8x8x8 head), the six faces opaque and nothing else painted, drawn at the
                           troop's density or at a whole multiple of it (the build reduces them); the first `count` are used and
                           any more are banked.
  art/troops/<troop>_beards.png  the same for the beard volume (the head's second box, a faces block with "beard"): the six
                           faces of the box painted or clear (a clear texel is the beard's silhouette, the head patch of the same
                           variant shows through it), the rest of its rectangle clear.
  art/marks/chevron.png    the rank chevron (in-match progression design 7), copied to textures/entity/troop/chevron.png:
                           at most 32 px a side, alpha 0 or 255, every opaque pixel grey (the client tints it).
  art/troops/<troop>_s<stage>_r.png  the relief layer of that skin: the troop's size, alpha 0 or 255, an opaque texel
                           stands one texel proud of its face (design 3).
  art/troops/<troop>_veteran.png  the veteran trim (in-match progression design 4.4): an overlay drawn over whichever
                           armour tier the troop wears, never a fourth full skin. The troop's size, alpha 0 or 255, every
                           opaque texel on a texel every own skin paints, at most TRIM_SHARE of the stage 1 skin's opaque
                           texels, team tint rule 1; it takes no relief layer (a _veteran_r.png is refused).
  art/team/team_cloth.png  the greyscale cloth template; its three greys take the side colour x1.0, x0.8 and x0.6, every
                           other pixel keeps its paint. art/team/glyph_<glyph>.png: full-sheet masks, stamped in white.
  art/team/team_cloth.json optional {"density": N}: the template and the glyph masks are N texels a px of the cloth layout.

Texel density (the texel-3 trial, docs of .superpowers/sdd/2026-10-17-phase-2-houses/texel-3-plan.md): a troop's "density" is the
texels a model px of its PNGs carry, a whole number from 1 to 4, 1 unless it says (its json); its size, boxes' uv and bones' sheet
stay in layout units (px of the model), so the model json needs no change in the client's loader, and every PNG it owns (skins,
_r relief, _veteran, _e, _o) is density times the layout in each direction; the model says "density" right after "size" when
it is not 1, and a build of a density 1 troop is byte for byte what it was. A weapon's "density" (cube weapons only, 2 unless it
says) is the texels a px of its sheet: the sheet is its real size and the quads read real texels, as before. The team cloth's
density is art/team/team_cloth.json {"density": N} (1 unless it exists); the bones' cloth sheet is the template's size divided
by it. A troop of density above 1 also gets a one-texel-a-px twin of each of its PNGs, <name>_far.png (a D x D block takes its
most common colour and is clear unless at least half of it is painted), which the client binds past TroopDepth.NEAR_BLOCKS and
on the portrait cards drawn under 48 px a block. The relief mesher voxels are a texel wide in the plane and one old cell
(a px, times the box's scale) proud along the normal, whatever the density; the glow caps count a troop's lit texels over D squared and a cube weapon's over (D / 2) squared (a 2x weapon counts as ever).

Face variants (texel-3 faces design F2, .superpowers/sdd/2026-10-17-phase-2-houses/task-5d-faces-design.md): a troop with a "faces"
block gets its head box split into count sibling bones under `head`, face_v0 (the rig's head box where it was) and face_v1..
(the same box with its uv at slot 1..), each carrying "variants": [k]; the client shows only the bone of the variant a troop
wears (TroopFaces: a hash of its UUID; a card and a pinned troop wear card and fixed). The patches of <troop>_faces.png are
pasted into every own skin (patch 0 over the head's faces, patch k at slot k), so the far twin, the glow and later the _n and _s
follow the sheet with no draw call and no texture added; a slot's 32x16 rectangle must be free in every sheet of the troop (no
box uv, no skin paint, no trim, no glow), and a face bone takes no relief. Variant 0 renders as the head always did. The model
says {"faces": {count, card, fixed?}} after "portrait"; card is the face a card portrait wears (the pinned face unless it says).
Face art has its own density, the block's "density" (texels a px of the patch files; the troop's own unless it says): the six
troops of the faces round are drawn at 2 (64x32 texels for the head, 28x10 for the mage's beard). On a troop of density 1 the patches
are reduced to the sheet by the far twin's majority rule (2 to 1: the 8x8 head of the default look); on a sheet of a higher density
the face bones keep the patches' own texels: a face bone has its own "sheet", the model's PNG in face px (the sheet's texels over the
face density; 192x288 texels at 2 is 96x144 on the 3x knight), and its boxes' uv in face px, so the client's one unit is the
face's texel size and no UV lies inside a texel. A slot is then the layout px its patch needs rounded up (22x11 for the 3x knight's
head), a slot's texel origin (and the old head's) must be a whole number of face px, the old head rectangle is cleared but for patch
0, and the far twin judges the block that straddles a patch's edge by the patch's texels in it. Without the key the face art may
still be drawn at the troop's density or a whole multiple of it, as before. With a "beard" block the head's second box (the mage's and the Marshal's beard volume)
goes into the face bones too, at its own rectangle in face_v0 and at a beard slot in the others, so a variant has its own beard
(beard patch k is pasted with head patch k). A faces block of count 1 splits nothing and adds no slot: it pastes patch 0 over the
head (the Templar's great helm hides the face; the other patches of its file are banked).

Proportions are baked here, never scaled at runtime: a bone scale k scales the bone's boxes about its pivot (origin
k*origin + size*(k-1)/2, dilation size*(k-1)/2, the UV size stays the integer size) and moves its children's pivots by
k (a jointed limb's lower bone takes its upper bone's scale unless it has its own, JOINTS); a part takes its anchor bone's scale times its own; root then shifts so the lowest leg box bottom sits at y 24.

Deterministic: every collection is walked in file or sorted order. Like build_ui_art.py it re-executes itself with
PYTHONHASHSEED=0 and runs under Python 3.12. Pillow is a dev tool only."""
import base64
import io
import json
import math
import os
import re
import subprocess
import sys
import tempfile
import uuid
import colorsys
from collections import Counter
from pathlib import Path

sys.dont_write_bytecode = True
from PIL import Image
import bbmodel

ROOT = Path(__file__).resolve().parents[1]
ASSETS = 'src/main/resources/assets/gravesandglory'
MODELS = 'models/troop'
TEXTURES = 'textures/entity/troop'
NAMESPACE = 'gravesandglory'
FORMAT = 3
# The rig of each sheet entity (TroopModels.RIGS in the client).
RIGS = {'husk': 'biped', 'zombie': 'biped', 'skeleton': 'biped', 'stray': 'biped', 'wither_skeleton': 'biped',
        'drowned': 'biped', 'zombified_piglin': 'biped', 'vindicator': 'biped', 'pillager': 'biped', 'evoker': 'biped',
        'wolf': 'hound', 'polar_bear': 'hound', 'ravager': 'hound', 'spider': 'spider', 'iron_golem': 'colossus'}
# Each skeleton's bones as (bone, parent), parents first: the trees of the brief's source formats. The renderer finds
# the hand and team bones by their paths from the root, and Task 5b's clips turn bones by name, so a rig keeps the tree.
# The biped is jointed (animation rework Task 9, the plan's rig contract): a forearm under each arm at the elbow, a shin under
# each leg at the knee, and the hands on the forearms, where the one-piece arm held them (arm-frame [-1, 8, 0] and [1, 8, 0]).
BIPED_BONES = (('root', ''), ('body', 'root'), ('head', 'body'), ('right_arm', 'body'), ('right_forearm', 'right_arm'),
               ('right_hand', 'right_forearm'), ('left_arm', 'body'), ('left_forearm', 'left_arm'), ('left_hand', 'left_forearm'),
               ('arms_folded', 'body'), ('right_leg', 'root'), ('right_shin', 'right_leg'), ('left_leg', 'root'),
               ('left_shin', 'left_leg'))
# The colossus keeps the one-piece biped tree: an iron golem's limbs do not bend.
COLOSSUS_BONES = (('root', ''), ('body', 'root'), ('head', 'body'), ('right_arm', 'body'), ('right_hand', 'right_arm'),
                  ('left_arm', 'body'), ('left_hand', 'left_arm'), ('arms_folded', 'body'), ('right_leg', 'root'),
                  ('left_leg', 'root'))
RIG_BONES = {
    'biped': BIPED_BONES,
    'hound': (('root', ''), ('body', 'root'), ('chest', 'body'), ('head', 'chest'), ('jaw', 'head'),
              ('front_left_leg', 'chest'), ('front_right_leg', 'chest'), ('tail', 'body'), ('back_left_leg', 'root'),
              ('back_right_leg', 'root')),
    'spider': (('root', ''), ('thorax', 'root'), ('head', 'thorax'), ('fangs', 'head'), ('abdomen', 'thorax'))
              + tuple(('%s_leg_%d' % (s, n), 'thorax') for s in ('left', 'right') for n in (1, 2, 3, 4)),
    'colossus': COLOSSUS_BONES,
}
# The bones whose boxes the rig stands on (bake_bones sets their lowest point on the ground): a jointed leg's foot is on its shin.
LEGS = {'right_leg', 'left_leg', 'right_shin', 'left_shin', 'front_left_leg', 'front_right_leg', 'back_left_leg', 'back_right_leg'} | \
       {'%s_leg_%d' % (s, n) for s in ('left', 'right') for n in (1, 2, 3, 4)}
# A jointed limb's (upper, lower) bones: a troop's scale for the upper bone also scales the lower one unless it sets its own, so
# a scaled arm or leg keeps its proportions and its hand or foot where the one-piece limb had it.
JOINTS = (('right_arm', 'right_forearm'), ('left_arm', 'left_forearm'), ('right_leg', 'right_shin'), ('left_leg', 'left_shin'))
MODEL_KEYS = ('format', 'troop', 'rig', 'size', 'bones', 'skins', 'veteran', 'glow', 'glowFrom', 'outer', 'weapon',
              'hand', 'idleArms', 'portrait')


def model_keys(density, faces=False):
    """The keys of a troop model in order: MODEL_KEYS, with "density" right after "size" when the troop's is not 1 and "faces" last
    for a troop with face variants."""
    keys = MODEL_KEYS + ('faces',) if faces else MODEL_KEYS
    if density == TROOP_DENSITY:
        return keys
    at = keys.index('size') + 1
    return keys[:at] + ('density',) + keys[at:]
VANILLA_KEYS = ('parent', 'elements', 'textures', 'display', 'overrides', 'gui_light', 'ambientocclusion')
TROOP_KEYS = {'troop', 'rig', 'size', 'density', 'bones', 'hunch', 'idleArms', 'parts', 'skins', 'veteran', 'glow', 'glowFrom',
              'outer', 'weapon', 'hand', 'portrait', 'faces', 'note'}
FACES_KEYS = {'count', 'slots', 'card', 'fixed', 'beard', 'density', 'note'}
BEARD_KEYS = {'slots', 'note'}
FACES_MIN, FACES_MAX = 1, 8   # the faces a troop may have, the canonical one included (1: the canonical face only, no variants)
FACES_SUFFIX = '_faces'       # art/troops/<troop>_faces.png
BEARDS_SUFFIX = '_beards'     # art/troops/<troop>_beards.png
ENTRY_KEYS = {'part', 'name', 'bone', 'uv', 'offset', 'rotation', 'scale', 'stages', 'weaponTiers', 'note'}
# The gear tiers (in-match progression design 4.4) when a sheet has no gearTiers rows; a tier's roman numeral for --tiers.
TIERS = (1, 2, 3)
ROMAN = {1: 'I', 2: 'II', 3: 'III'}
# A weapon tier's model: art/weapons/<weapon>_t2 and _t3; tier I keeps <weapon> (in-match progression plan, spec gaps 7).
WEAPON_TIER = re.compile(r'(.+)_t(\d+)')
# The rank chevron (in-match progression design 7): art/marks/chevron.png, grey so the client tints it the side colour.
CHEVRON_SRC, CHEVRON_MAX = 'art/marks/chevron.png', 32
PART_KEYS = {'part', 'bone', 'team', 'tag', 'pivot', 'rotation', 'scale', 'boxes', 'note'}
WEAPON_KEYS = {'weapon', 'density', 'thickness', 'grip', 'pull', 'nocks', 'note'}
RIG_KEYS = {'rig', 'size', 'outer', 'hands', 'bones', 'note'}
IDLE_ARMS = ('hang', 'forward', 'folded')
PORTRAIT = re.compile(r'(standard|tall \d+|wide \d+|beast \d+( yaw -?\d+)?)')
GREYS = {(160, 160, 160): 1.0, (128, 128, 128): 0.8, (96, 96, 96): 0.6}   # the cloth template's field greys
LINEN = (0xCF, 0xC6, 0xB0)
COMPANY = (0x2A, 0x37, 0x47)   # the Hollow's navy (work/ui-concepts/storybook/NOTES.md), the company's cloth
GLYPH = (0xF3, 0xEF, 0xE4)   # the pennant glyph's white, the white of the charges
GLOW_CAP = {'epic': 16, 'legendary': 16}   # lit area, in layout px for a troop's glow and in texels of a 2x weapon for a cube weapon's (its texels over (D / 2) squared); any other rarity: 6
TINT_SATURATION, TINT_VALUE, TINT_HUE = 0.45, 0.60, 45.0
ONE = (1.0, 1.0, 1.0)
# The bones a model may have: TroopModel records the bones a draw shows as the bits of one long (TroopModel.MAX_BONES),
# and the client refuses a bigger model, which then draws vanilla.
MAX_BONES = 64
# Troop depth (Task 5d): the caps are presentation constants, kept here and not in sheets.
RELIEF_CAP = 256     # relief quads in one stage, all bones together
RELIEF_SHARE = 0.20  # the most of one face's texels a relief may raise
WEAPON_CAP = 128     # quads in one weapon frame
FRAME_MAX = 48       # the longest side of a weapon frame, px
PULL_FRAMES = 3      # pull_0 to pull_2 of a draw weapon, after the rest frame
PULL_MAX = 1.2       # a pull time lies between 0 and the longest bow_draw clip, in seconds
SLIVER = 0.25        # a side face reads its texel through a UV band 2 x SLIVER across, at the texel's centre
THICKNESS = (1, 2, 3)
# Cube weapons (art pipeline v2): made in Blockbench, by default 2 texels to a model px on a 64 x 128 sheet (a weapon json may say
# density); the build adds a far version under the sheet and the _n and _s maps beside it.
WEAPON_FRAMES = ('rest', 'pull_0', 'pull_1', 'pull_2')
WEAPON_DENSITY = 2     # texels a px of a cube weapon's sheet unless its json says density: a face's UV is exactly twice its size
TROOP_DENSITY = 1      # texels a px of a troop's PNGs unless its json says density
DENSITY_MAX = 4        # the most texels a px a source may name
FAR_SUFFIX = '_far'    # <name>_far.png: the one-texel-a-px twin of a density D troop PNG (TroopModels.twin in the client derives the same name)
TIMES = {1: 'once', 2: 'twice', 3: 'three times', 4: 'four times'}
THICKNESS_SLACK = 1.5  # px a json thickness may differ from the model's depth across z (it is informational)
FAR_CAP = 32           # quads of the far version, which a weapon on a troop outside TroopDepth.near draws
FAR_SAMPLES = 4        # the front-view raster takes this many samples a side of a px
FAR_COVER = 0.5        # the share of a px's samples a face must cover for the far sprite to be solid there
NOCK_REACH = 0.5       # px a nock may lie from the string cube
GLOW_EMISSION = 200    # _s alpha on a texel lit in _e (255 is none)
TONE_LUMA = 0.12       # luma (0 to 1) of one tone step of a ramp
TONE_SMOOTH = 12       # smoothness a tone step above the patch's face tone adds
TONE_MAX = 2           # steps either side of the face tone (a ramp has five tones)
HEIGHT_BASE, HEIGHT_TONE, EDGE_TILT = 0.5, 0.05, 1.2   # _n alpha: a texel is 0.5 high, 0.05 a tone step; a patch edge tilts by the drop
# The material map's key colours (art pipeline v2 section 6): name, _s smoothness (R), F0 (G, never 238 to 254), porosity (B).
MATERIALS = {
    (255, 0, 0): ('steel', 185, 230, 0), (170, 0, 0): ('iron', 110, 230, 0), (255, 170, 0): ('gilt', 200, 231, 0),
    (0x8B, 0x45, 0x13): ('wood', 70, 10, 40), (255, 255, 255): ('linen', 30, 8, 60), (0x55, 0x33, 0x22): ('leather', 60, 10, 30),
    (0xEE, 0xEE, 0xDD): ('bone', 90, 12, 20), (0, 255, 0): ('glass', 240, 20, 200)}
# Minecraft's Direction order in model space (0 down is -y, y runs down the model), the axis of each, and the quad rule.
NORMALS = ((0, -1, 0), (0, 1, 0), (0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0))
AXIS = (1, 1, 2, 2, 0, 0)
RELIEF_TINT = 0.4              # the relief template shows an allowed face's hue at this strength over white
RELIEF_RED = (214, 48, 48)     # and hatches a forbidden texel in this


def fail(why):
    print('build_skins: FAIL ' + why)
    sys.exit(1)


def num(v):
    """A number as the JSON keeps it: four decimals, an integer when it is one, never -0."""
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


def is_num(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool) and math.isfinite(v)


def vec(v, n, what, ints=False, minimum=None):
    if not (isinstance(v, list) and len(v) == n and all(is_num(c) and (not ints or float(c).is_integer()) for c in v)):
        fail('%s must be a list of %d %s' % (what, n, 'whole numbers' if ints else 'numbers'))
    if minimum is not None and any(c < minimum for c in v):
        fail('%s must not go below %s' % (what, minimum))
    return [int(c) for c in v] if ints else [float(c) for c in v]


def scale3(v, what):
    if is_num(v):
        v = [v, v, v]
    k = vec(v, 3, what + ' scale')
    if any(c <= 0 for c in k):
        fail(what + ' scale must be above 0')
    return tuple(k)


def keys(d, allowed, what):
    if not isinstance(d, dict):
        fail(what + ' must be a JSON object')
    extra = sorted(set(d) - allowed)
    if extra:
        fail('%s has unknown key %s' % (what, extra[0]))


def density_of(data, what, default):
    """The "density" of a source json (texels a px): a whole number from 1 to DENSITY_MAX, `default` when it is absent."""
    value = data.get('density', default)
    if isinstance(value, bool) or not isinstance(value, int) or not 1 <= value <= DENSITY_MAX:
        fail('%s: density must be a whole number from 1 to %d (texels a px), not %s' % (what, DENSITY_MAX, json.dumps(value)))
    return value


def read_json(path):
    try:
        return json.loads(path.read_text(encoding='utf-8'))
    except (OSError, ValueError) as e:
        fail('%s: %s' % (path.as_posix(), e))


def read_png(path, what):
    try:
        with Image.open(path) as im:
            return im.convert('RGBA')
    except OSError as e:
        fail('%s: %s (%s)' % (what, path.name, e))


class Catalog:
    """Every source under art/ plus the sheet rows the build reads (troops, factions, levelNames)."""

    def __init__(self, root):
        self.root = root
        self.rigs, self.parts, self.weapons, self.troops = {}, {}, {}, {}
        self.bbmodels = {}   # weapon id -> art/weapons/<id>.bbmodel
        self.sheet, self.factions, self.stages = {}, {}, {}
        self.template, self.glyphs = None, {}
        self.tiers = list(TIERS)
        self.cloth_density = TROOP_DENSITY   # texels a px of the cloth template and glyph masks (art/team/team_cloth.json)
        self.cloth_layout = None             # the cloth sheet in px of the model: the team bones' sheet and the boxes' uv are in it


def sources(folder, key):
    out = {}
    for path in sorted(folder.glob('*.json')):
        data = read_json(path)
        if not isinstance(data, dict) or data.get(key) != path.stem:
            fail('%s: "%s" must be %s, the file name' % (path.as_posix(), key, path.stem))
        out[path.stem] = data
    return out


def load(root: Path) -> Catalog:
    root = Path(root)
    cat = Catalog(root)
    design = read_json(root / 'sheets/design.json')
    cat.sheet = {r['id']: r for r in design.get('troops', [])}
    cat.factions = {f['id']: f for f in design.get('factions', [])}
    for reserved in ('linen', 'company'):
        if reserved in cat.factions:
            fail('factions row %s: linen and company name the cloths with no side' % reserved)
    # Every troop has armour tiers I to III, the gearTiers rows (in-match progression design 4.4): a skin _s<n> and the
    # parts' stages key name armour tiers. A sheet without gearTiers rows (the test fixtures) has the three.
    cat.tiers = sorted({int(r['tier']) for r in design.get('gearTiers', [])}) or list(TIERS)
    for tid in cat.sheet:
        cat.stages[tid] = list(cat.tiers)
    art = root / 'art'
    cat.rigs = sources(art / 'rigs', 'rig')
    cat.parts = sources(art / 'parts', 'part')
    cat.weapons = sources(art / 'weapons', 'weapon')
    for path in sorted((art / 'weapons').glob('*.bbmodel')):
        if path.stem not in cat.weapons:
            fail('art/weapons/%s has no art/weapons/%s.json (grip, thickness, pull and nocks)' % (path.name, path.stem))
        cat.bbmodels[path.stem] = path
    cat.troops = sources(art / 'troops', 'troop')
    if (art / 'team/team_cloth.png').is_file():
        cat.template = read_png(art / 'team/team_cloth.png', 'team cloth template')
        if (art / 'team/team_cloth.json').is_file():
            side = read_json(art / 'team/team_cloth.json')
            keys(side, {'density', 'note'}, 'art/team/team_cloth.json')
            cat.cloth_density = density_of(side, 'art/team/team_cloth.json', TROOP_DENSITY)
        k = cat.cloth_density
        if cat.template.width % k or cat.template.height % k:
            fail('art/team/team_cloth.json: art/team/team_cloth.png is %dx%d, which density %d does not divide (a cloth drawn at '
                 '%d texels a px is %d times its layout in each direction)' % (cat.template.width, cat.template.height, k, k, k))
        cat.cloth_layout = (cat.template.width // k, cat.template.height // k)
        for path in sorted((art / 'team').glob('glyph_*.png')):
            cat.glyphs[path.stem[len('glyph_'):]] = read_png(path, 'glyph')
    for name in cat.rigs:
        check_rig(cat, name)
    for name in cat.parts:
        check_part(cat, name)
    team = [(pid, b['uv'], b['size']) for pid in sorted(cat.parts) if cat.parts[pid]['team']
            for b in check_boxes(cat.parts[pid]['boxes'], 'part ' + pid, faceuv=False)]
    if team:
        check_layout(team, cat.cloth_layout, 'the cloth sheet (every team part)')
    return cat


# The six faces of a box by the side of the model they show (right is the model's right, -x; front is -z), in the order a
# box's "faceuv" lists them.
FACEUV = ('top', 'bottom', 'right', 'front', 'left', 'back')


def check_boxes(boxes, what, sheet=None, faceuv=True):
    """A box list as the build keeps it. "faceuv" (a rig or a bone override, not a part: faceuv=False) moves named faces of the
    box UV layout to another top-left texel, each {face: [u, v]} of FACEUV, so the jointed rig's two 4x6x4 limb boxes read
    the halves of the vanilla 4x12x4 layout; a box with it is unmirrored."""
    if not isinstance(boxes, list):
        fail(what + ' boxes must be a list')
    out = []
    for i, b in enumerate(boxes):
        keys(b, {'origin', 'size', 'uv', 'faceuv', 'mirror', 'note'} if faceuv else {'origin', 'size', 'uv', 'mirror', 'note'},
             '%s box %d' % (what, i))
        mirror = b.get('mirror', False)
        if not isinstance(mirror, bool):
            fail('%s box %d mirror must be true or false' % (what, i))
        box = {'origin': vec(b.get('origin'), 3, '%s box %d origin' % (what, i)),
               'size': vec(b.get('size'), 3, '%s box %d size' % (what, i), ints=True, minimum=0),
               'uv': vec(b.get('uv'), 2, '%s box %d uv' % (what, i), ints=True, minimum=0),
               'mirror': mirror, 'dilation': [0.0, 0.0, 0.0]}
        if 'faceuv' in b:
            moved = b['faceuv']
            if not isinstance(moved, dict) or not moved or set(moved) - set(FACEUV):
                fail('%s box %d faceuv must map some of %s to a [u, v]' % (what, i, ', '.join(FACEUV)))
            if mirror:
                fail('%s box %d has a faceuv and is mirrored; a box with per-face UVs is unmirrored' % (what, i))
            box['faceuv'] = {k: vec(moved[k], 2, '%s box %d faceuv %s' % (what, i, k), ints=True, minimum=0)
                             for k in FACEUV if k in moved}
        out.append(box)
    return out


def face_layout(uv, size, faceuv=None):
    """Each face's texel rectangle (x0, y0, x1, y1) by FACEUV name: ModelPart.Cuboid's box UV layout at uv, a face that
    `faceuv` names at its own top-left texel."""
    (u, v), (w, h, d) = uv, size
    at = {'top': (u + d, v), 'bottom': (u + d + w, v), 'right': (u, v + d), 'front': (u + d, v + d),
          'left': (u + d + w, v + d), 'back': (u + 2 * d + w, v + d)}
    for k, c in (faceuv or {}).items():
        at[k] = (c[0], c[1])
    dims = {'top': (w, d), 'bottom': (w, d), 'right': (d, h), 'front': (w, h), 'left': (d, h), 'back': (w, h)}
    return {k: (at[k][0], at[k][1], at[k][0] + dims[k][0], at[k][1] + dims[k][1]) for k in FACEUV}


def footprint(uv, size, faceuv=None):
    """The texture rectangles a box's six faces read, as (x0, y0, x1, y1): the top row holds the top and bottom faces,
    the second row the four sides (ModelPart.Cuboid's layout); a box with a faceuv, each face's own rectangle."""
    if faceuv:
        return [r for r in face_layout(uv, size, faceuv).values() if r[0] < r[2] and r[1] < r[3]]
    (u, v), (w, h, d) = uv, size
    return [r for r in ((u + d, v, u + d + 2 * w, v + d), (u, v + d, u + 2 * d + 2 * w, v + d + h)) if r[0] < r[2] and r[1] < r[3]]


def check_layout(owned, sheet, what):
    """owned: (owner, uv, size) or (owner, uv, size, faceuv) per box. Every face inside the sheet, no two boxes on one texel.
    A faceuv face may lie wholly inside another face of its own box (a joint cap reads the rows of its box's front)."""
    w, h = sheet
    rects = []
    for n, item in enumerate(owned):
        owner, uv, size = item[:3]
        moved = item[3] if len(item) > 3 else None
        for r in footprint(uv, size, moved):
            if r[0] < 0 or r[1] < 0 or r[2] > w or r[3] > h:
                fail('%s: %s has a box outside the %dx%d sheet (uv %d, %d)' % (what, owner, w, h, uv[0], uv[1]))
            rects.append((owner, n, bool(moved), r))

    def inside(a, b):
        return b[0] <= a[0] and b[1] <= a[1] and a[2] <= b[2] and a[3] <= b[3]
    for i, (a, na, fa, ra) in enumerate(rects):
        for b, nb, _, rb in rects[i + 1:]:
            if ra[0] < rb[2] and rb[0] < ra[2] and ra[1] < rb[3] and rb[1] < ra[3]:
                if na == nb and fa and (inside(ra, rb) or inside(rb, ra)):
                    continue
                if a == b:
                    fail('%s: two boxes of %s overlap in UV at (%d, %d)' % (what, a, max(ra[0], rb[0]), max(ra[1], rb[1])))
                fail('%s: %s and %s overlap in UV at (%d, %d)' % (what, a, b, max(ra[0], rb[0]), max(ra[1], rb[1])))


def check_rig(cat, name):
    rig = cat.rigs[name]
    what = 'rig ' + name
    keys(rig, RIG_KEYS, what)
    if name not in RIG_BONES:
        fail('%s: no such skeleton; the rigs are %s' % (what, ', '.join(sorted(RIG_BONES))))
    vec(rig.get('size'), 2, what + ' size', ints=True, minimum=1)
    if not is_num(rig.get('outer')):
        fail(what + ': outer must be a number')
    bones = rig.get('bones')
    if not isinstance(bones, list):
        fail(what + ': bones must be a list')
    seen = []
    for b in bones:
        keys(b, {'name', 'parent', 'pivot', 'rotation', 'boxes', 'note'}, what + ' bone')
        n = b.get('name')
        if n in seen:
            fail('%s: duplicate bone %s' % (what, n))
        parent = b.get('parent')
        if not seen and parent != '':
            fail('%s: %s: the first bone is root, with parent ""' % (what, n))
        if seen and parent not in seen:
            fail('%s: %s has parent %s, which is not a bone listed before it' % (what, n, parent))
        vec(b.get('pivot'), 3, '%s %s pivot' % (what, n))
        if 'rotation' in b:
            vec(b['rotation'], 3, '%s %s rotation' % (what, n))
        check_boxes(b.get('boxes', []), '%s %s' % (what, n))
        seen.append(n)
    tree = dict(RIG_BONES[name])
    if sorted(seen) != sorted(tree) or seen[0] != 'root':
        fail('%s: the bones must be %s, root first' % (what, ', '.join(tree)))
    for b in bones:
        if b['parent'] != tree[b['name']]:
            fail('%s: %s has parent %s, but the %s skeleton hangs it from %s' % (what, b['name'], b['parent'] or '""', name,
                                                                                 tree[b['name']] or '""'))
    hands = rig.get('hands')
    if not isinstance(hands, dict) or set(hands) != {'right', 'left'} or any(hands[k] not in seen for k in hands):
        fail(what + ': hands must name a bone for right and for left')
    check_layout([(b['name'], x['uv'], x['size'], x.get('faceuv')) for b in bones for x in check_boxes(b.get('boxes', []), what)],
                 rig['size'], what)


def check_part(cat, name):
    p = cat.parts[name]
    what = 'part ' + name
    keys(p, PART_KEYS, what)
    if not isinstance(p.get('bone'), str) or not isinstance(p.get('team'), bool) or p.get('tag') not in ('', 'helm'):
        fail(what + ': needs bone (a bone name), team (true or false) and tag ("" or "helm")')
    for k in ('pivot', 'rotation'):
        if k in p:
            vec(p[k], 3, '%s %s' % (what, k))
    scale3(p.get('scale', 1), what)
    boxes = check_boxes(p.get('boxes'), what, faceuv=False)
    if p['team']:
        if cat.template is None:
            fail(what + ': a team part needs art/team/team_cloth.png')
        check_layout([(name, b['uv'], b['size']) for b in boxes], cat.cloth_layout, what + ' on the cloth sheet')


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def rotation(deg):
    """The ModelPart rotation (pitch, yaw, roll in degrees) as a matrix: Z then Y then X, as Quaternionf.rotationZYX."""
    x, y, z = (math.radians(a) for a in deg)
    rx = [[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]]
    ry = [[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]]
    rz = [[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]]
    return mat_mul(mat_mul(rz, ry), rx)


def world(bones):
    """Each bone's rest transform in model space: (rotation matrix, translation)."""
    out = {}
    for b in bones:
        r, t = out.get(b['parent'], ([[1, 0, 0], [0, 1, 0], [0, 0, 1]], [0.0, 0.0, 0.0]))
        p = b['pivot']
        t2 = [t[i] + sum(r[i][k] * p[k] for k in range(3)) for i in range(3)]
        out[b['name']] = (mat_mul(r, rotation(b['rotation'])), t2)
    return out


def corners(box):
    lo = [box['origin'][i] - box['dilation'][i] for i in range(3)]
    hi = [box['origin'][i] + box['size'][i] + box['dilation'][i] for i in range(3)]
    return [(x, y, z) for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])]


def bounds(bones, include=lambda b: True):
    """(min, max) corners of the chosen bones' boxes in model space, rest pose."""
    w = world(bones)
    pts = []
    for b in bones:
        if not include(b):
            continue
        r, t = w[b['name']]
        for box in b['boxes']:
            for c in corners(box):
                pts.append([t[i] + sum(r[i][k] * c[k] for k in range(3)) for i in range(3)])
    if not pts:
        return None
    return [min(p[i] for p in pts) for i in range(3)], [max(p[i] for p in pts) for i in range(3)]


def merge_bones(rig, what, overrides=None, parts=(), folded=True, stages=(1,), source=None):
    """The rig's bones with the troop's overrides and parts, before scale, hunch and the stand-on-the-ground shift.
    parts: (entry, part) pairs. `source` is a troop .bbmodel's rig bones (bbmodel.troop_bones): the bone groups own the
    skin geometry then, the troop json keeps only each bone's scale, and the parts are still built here from the library.
    Returns (bones by name, scale by name), parents first."""
    bones, scale = {}, {}
    if source is not None:
        # Geometry lives in the .bbmodel: a pivot or boxes left in the troop json would be a second, silent copy.
        bones = {name: dict(b, pivot=list(b['pivot']), rotation=list(b['rotation']),
                            boxes=[dict(x, origin=list(x['origin']), dilation=list(x['dilation'])) for x in b['boxes']])
                 for name, b in source.items()}
        for name, ov in (overrides or {}).items():
            if name not in bones:
                fail('%s: unknown bone %s (bones names a bone the %s rig lacks)' % (what, name, rig['rig']))
            if set(ov) & {'pivot', 'boxes'}:
                fail('%s: bones %s still sets %s: a troop with a .bbmodel keeps its geometry there, not in the json'
                     % (what, name, ', '.join(sorted(set(ov) & {'pivot', 'boxes'}))))
            keys(ov, {'scale', 'note'}, '%s bones %s' % (what, name))
            if 'scale' in ov:
                scale[name] = scale3(ov['scale'], '%s bones %s' % (what, name))
    else:
        for b in rig['bones']:
            bones[b['name']] = {'name': b['name'], 'parent': b['parent'], 'part': '', 'team': False, 'tag': '', 'stages': [], 'weaponTiers': [],
                                'pivot': [float(c) for c in b['pivot']], 'rotation': [float(c) for c in b.get('rotation', [0, 0, 0])],
                                'boxes': check_boxes(b.get('boxes', []), what)}
        for name, ov in (overrides or {}).items():
            if name not in bones:
                fail('%s: unknown bone %s (bones names a bone the %s rig lacks)' % (what, name, rig['rig']))
            keys(ov, {'pivot', 'boxes', 'scale', 'note'}, '%s bones %s' % (what, name))
            if 'pivot' in ov:
                bones[name]['pivot'] = vec(ov['pivot'], 3, '%s bones %s pivot' % (what, name))
            if 'boxes' in ov:
                bones[name]['boxes'] = check_boxes(ov['boxes'], '%s bones %s' % (what, name))
            if 'scale' in ov:
                scale[name] = scale3(ov['scale'], '%s bones %s' % (what, name))
    for upper, lower in JOINTS:
        if upper in scale and lower in bones and lower not in scale:
            scale[lower] = scale[upper]
    if not folded and 'arms_folded' in bones:
        bones['arms_folded']['boxes'] = []
    for entry, part in parts:
        pid = part['part']
        name = entry.get('name', pid)
        if not isinstance(name, str) or not re.fullmatch(r'[a-z0-9_]+', name):
            fail('%s: part %s needs a lower-case name' % (what, pid))
        if name in bones:
            fail('%s: duplicate bone %s (a part\'s bone is its name, which defaults to the part id)' % (what, name))
        anchor = entry.get('bone', part['bone'])
        if anchor not in bones:
            fail('%s: part %s hangs from unknown bone %s' % (what, name, anchor))
        if bones[anchor]['team']:
            fail('%s: part %s hangs from the team part %s, which the renderer cannot draw: the skin pass hides a team part '
                 'with everything under it, and the cloth pass skips a part whose parent is hidden; hang it from %s'
                 % (what, name, anchor, bones[anchor]['parent']))
        if part['team']:
            origin_uv = [0, 0]
        else:
            if 'uv' not in entry:
                fail('%s: part %s needs a uv on the troop sheet' % (what, name))
            origin_uv = vec(entry['uv'], 2, '%s part %s uv' % (what, name), ints=True, minimum=0)
        want = entry.get('stages', [])
        if not isinstance(want, list) or any(s not in stages for s in want):
            fail('%s: part %s shows at armour tiers %s, but the armour tiers are %s' % (what, name, want, list(stages)))
        # weaponTiers: the weapon tiers a weapon part shows at (design 4.4: a troop with no held weapon, or the Ballista
        # Crew's frame, shows its weapon tier as parts on the rig); without the key a part shows at every weapon tier.
        arms = entry.get('weaponTiers')
        if arms is not None and (not isinstance(arms, list) or not arms):
            fail('%s: part %s weaponTiers must list the weapon tiers it shows at, not %s' % (what, name, arms))
        if arms is not None and any(isinstance(t, bool) or t not in stages for t in arms):
            fail('%s: part %s shows at weapon tiers %s, but the weapon tiers are %s' % (what, name, arms, list(stages)))
        pivot = [a + b for a, b in zip(vec(part.get('pivot', [0, 0, 0]), 3, 'pivot'),
                                       vec(entry.get('offset', [0, 0, 0]), 3, '%s part %s offset' % (what, name)))]
        rot = vec(entry.get('rotation', part.get('rotation', [0, 0, 0])), 3, '%s part %s rotation' % (what, name))
        boxes = check_boxes(part['boxes'], 'part ' + pid, faceuv=False)
        for b in boxes:
            b['uv'] = [origin_uv[0] + b['uv'][0], origin_uv[1] + b['uv'][1]]
        bones[name] = {'name': name, 'parent': anchor, 'part': pid, 'team': part['team'], 'tag': part['tag'],
                       'stages': sorted(want), 'weaponTiers': sorted(arms or []), 'pivot': pivot, 'rotation': rot, 'boxes': boxes}
        own = scale3(entry.get('scale', 1), '%s part %s' % (what, name))
        base = scale3(part.get('scale', 1), 'part ' + pid)   # the part's own scale, then the troop's for it
        k = scale.get(anchor, ONE)
        scale[name] = tuple(k[i] * base[i] * own[i] for i in range(3))
    return bones, scale


def bake_bones(rig, what, overrides=None, parts=(), hunch=0.0, folded=True, stages=(1,), source=None):
    """The rig's bones with the troop's overrides and parts, scaled, hunched and set on the ground; parts first checked.
    parts: (entry, part) pairs; `source` is a troop .bbmodel's rig bones (see merge_bones). Returns the bone dicts, parents first."""
    bones, scale = merge_bones(rig, what, overrides, parts, folded, stages, source)
    for b in bones.values():
        k = scale.get(b['name'], ONE)
        if k == ONE:
            continue
        for box in b['boxes']:
            for i in range(3):
                grow = box['size'][i] * (k[i] - 1) / 2
                box['origin'][i] = k[i] * box['origin'][i] + grow
                box['dilation'][i] = grow
        for child in bones.values():
            if child['parent'] == b['name']:
                child['pivot'] = [child['pivot'][i] * k[i] for i in range(3)]
    if hunch:
        bones['body']['rotation'][0] += hunch
        for n in ('head', 'right_arm', 'left_arm'):
            bones[n]['rotation'][0] -= hunch
    order = list(bones.values())
    feet = bounds(order, lambda b: b['name'] in LEGS)
    if feet is None:
        fail('%s: no leg box to stand on' % what)
    bones['root']['pivot'][1] += 24 - feet[1][1]
    return order


def clean_bone(b, sheet, density=TROOP_DENSITY):
    """The bone as the model file keeps it. A face bone drawn at its own density below the troop's (b['face_density']) has the PNG's
    texels over that density for its sheet and its boxes' uv in that unit (face px), the unit the client's cuboids use."""
    out = {'name': b['name'], 'parent': b['parent'], 'part': b['part'], 'team': b['team'], 'tag': b['tag'], 'stages': b['stages']}
    if b.get('weaponTiers'):   # a weapon part of a troop with no held weapon: only such a bone says it
        out['weaponTiers'] = b['weaponTiers']
    if b.get('variants'):   # a face bone of a faces troop: only the bones of a troop with faces say it
        out['variants'] = b['variants']
    df = b.get('face_density')
    def unit(uv):
        return [c * density // df for c in uv] if df else uv
    if df:
        sheet = [c * density // df for c in sheet]

    def box(x):
        kept = {'origin': [num(c) for c in x['origin']], 'size': x['size'], 'uv': unit(x['uv'])}
        if x.get('faceuv'):   # only a jointed limb's box moves faces (check_boxes)
            kept['faceuv'] = {k: unit(c) for k, c in x['faceuv'].items()}
        kept.update({'mirror': x['mirror'], 'dilation': [num(c) for c in x['dilation']]})
        return kept
    out.update({'sheet': list(sheet), 'pivot': [num(c) for c in b['pivot']], 'rotation': [num(c) for c in b['rotation']],
                'boxes': [box(x) for x in b['boxes']]})
    return out


def bone_density(model, b):
    """The texels a px the uv of a baked bone's boxes are in: the troop's, or for a face bone drawn at its own density the density
    its sheet implies (the layout size over the bone's sheet, times the troop's)."""
    density = model.get('density', TROOP_DENSITY)
    if b.get('variants') and b['sheet'] != model['size']:
        return density * model['size'][0] // b['sheet'][0]
    return density


def texture_id(name):
    return '%s:%s/%s' % (NAMESPACE, TEXTURES, name)


def troop_files(cat, troop):
    """The troop's skins by armour tier, and its veteran, glow and outer files ('' for none), checked against the naming rules."""
    src = cat.troops[troop]
    folder = cat.root / 'art/troops'
    if (folder / (troop + '.png')).exists():
        fail('%s: art/troops/%s.png: skins end _s<armour tier> or _veteran, never <troop>.png (ETF, when a player has it, '
             'pairs <name>_e.png with <name>.png)' % (troop, troop))
    stages = cat.stages[troop]
    skins = src.get('skins')
    if not isinstance(skins, dict) or '1' not in skins:
        fail('%s: skins must map armour tiers to files, with a tier 1 skin' % troop)
    own = {}
    for key, f in skins.items():
        if not key.isdigit() or int(key) not in stages:
            fail('%s: a skin for armour tier %s, which no gear tier has (armour tiers %s)' % (troop, key, stages))
        if f != '%s_s%s.png' % (troop, key):
            fail('%s: the armour tier %s skin must be %s_s%s.png, not %s' % (troop, key, troop, key, f))
        if not (folder / f).is_file():
            fail('%s: %s is missing from art/troops' % (troop, f))
        own[int(key)] = f
    extra = {}
    for kind, suffix in (('veteran', '_veteran'), ('glow', '_e'), ('outer', '_o')):
        want = troop + suffix + '.png'
        f = src.get(kind, want if (folder / want).is_file() else '')
        if f and f != want:
            fail('%s: the %s file must be %s, not %s' % (troop, kind, want, f))
        if f and not (folder / f).is_file():
            fail('%s: %s is missing from art/troops' % (troop, f))
        extra[kind] = f
    return own, extra


def tint(im, what, factions):
    """Team tint rule 1: no opaque pixel with saturation over 0.45 and value over 0.60 within 45 degrees of hue of a side."""
    sides = []
    for f in factions.values():
        c = f['color']
        h, s, v = colorsys.rgb_to_hsv(((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255)
        sides.append((f['id'], h * 360))
    seen = {}
    w, h = im.size
    data = im.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = data[x, y]
            if a == 0 or (r, g, b) in seen:
                continue
            hue, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            bad = None
            if s > TINT_SATURATION and v > TINT_VALUE:
                for side, sh in sides:
                    if min(abs(hue * 360 - sh), 360 - abs(hue * 360 - sh)) < TINT_HUE:
                        bad = side
                        break
            seen[(r, g, b)] = bad
            if bad:
                fail('%s pixel (%d, %d) #%02X%02X%02X is within %d degrees of the %s side colour (saturation %.2f, value '
                     '%.2f); team colour belongs on the cloth sheet' % (what, x, y, r, g, b, TINT_HUE, bad, s, v))


def lit(im):
    data = im.convert('RGBA').tobytes()
    return sum(1 for i in range(3, len(data), 4) if data[i])


def png_bytes(im):
    """The PNG of an image as RGBA, every transparent texel (0, 0, 0, 0): it carries no colour, as the glow pass adds RGB."""
    data = bytearray(im.convert('RGBA').tobytes())
    for i in range(0, len(data), 4):
        if not data[i + 3]:
            data[i:i + 3] = b'\x00\x00\x00'
    buf = io.BytesIO()
    Image.frombytes('RGBA', im.size, bytes(data)).save(buf, 'PNG')
    return buf.getvalue()


def dumps(obj):
    """Fixed formatting: one top-level key per line and one list element per line for lists of objects; LF."""
    def one(v):
        return json.dumps(v, separators=(',', ':'), ensure_ascii=True)
    lines = []
    for k, v in obj.items():
        if isinstance(v, list) and v and all(isinstance(x, (dict, str)) for x in v):
            lines.append('%s:[\n%s\n]' % (one(k), ',\n'.join(one(x) for x in v)))
        else:
            lines.append('%s:%s' % (one(k), one(v)))
    return ('{\n' + ',\n'.join(lines) + '\n}\n').encode('utf-8')


def quad_corners(d, lo, hi):
    """The four corners of an axis-aligned quad, counter-clockwise seen from the side its normal points to: corner 2 is
    corner 1 + corner 3 - corner 0, and the cross product of (corner 1 - corner 0) and (corner 3 - corner 0) points along
    the normal. lo and hi are equal on the direction's axis. TroopMesh.corners in the client is the same rule."""
    a = AXIS[d]
    p, q = (a + 1) % 3, (a + 2) % 3

    def at(pp, qq):
        c = [0.0, 0.0, 0.0]
        c[a], c[p], c[q] = lo[a], pp, qq
        return c
    if NORMALS[d][a] > 0:
        return [at(lo[p], lo[q]), at(hi[p], lo[q]), at(hi[p], hi[q]), at(lo[p], hi[q])]
    return [at(lo[p], lo[q]), at(lo[p], hi[q]), at(hi[p], hi[q]), at(hi[p], lo[q])]


def make_quad(d, lo, hi, tex):
    """One quad as the model JSON keeps it (13 numbers): the direction, the min and max corners and the texels of corners
    0, 1 and 3. tex maps a corner (x, y, z) to its texel (u, v)."""
    c = quad_corners(d, lo, hi)
    out = [d] + [num(v) for v in lo] + [num(v) for v in hi]
    for i in (0, 1, 3):
        out += [num(v) for v in tex(c[i])]
    return out


def runs(flags):
    """The (start, end) of every run of true flags, end exclusive."""
    out, start = [], None
    for i, f in enumerate(list(flags) + [False]):
        if f and start is None:
            start = i
        elif not f and start is not None:
            out.append((start, i))
            start = None
    return out


def sprite_mesh(frame: Image.Image, thickness: int, grip: tuple, column: int, v0: float = 0) -> list:
    """The quads of one weapon frame (design 2): its two big faces first (north, then south), each the whole frame, then a
    side face along every run of edges between an opaque pixel and transparency or the border, thickness deep and centred
    on z = 0. The grip pixel's centre is (0, 0, 0); x runs right, y down, one sprite pixel is one model pixel. The UVs
    are those of the frame's column in the packed sheet; a side face reads its edge pixel through a band half a texel
    across at the pixel's centre and the full run along. v0 is the row of the sheet the frame's top row sits on (0 for a
    sprite weapon's packed sheet; a cube weapon's far version stands under its colour sheet)."""
    w, h = frame.size
    px = frame.load()
    gx, gy, t = grip[0] + 0.5, grip[1] + 0.5, thickness / 2
    x0, x1, y0, y1 = -gx, w - gx, -gy, h - gy
    u0 = column * w

    def solid(x, y):
        return 0 <= x < w and 0 <= y < h and px[x, y][3] > 0

    def whole(c):
        return u0 + c[0] + gx, v0 + c[1] + gy
    quads = [make_quad(2, (x0, y0, -t), (x1, y1, -t), whole), make_quad(3, (x0, y0, t), (x1, y1, t), whole)]
    for d, dy in ((0, -1), (1, 1)):   # the edges that face up (-y) and down (+y), row by row
        for y in range(h):
            for a, b in runs([solid(x, y) and not solid(x, y + dy) for x in range(w)]):
                plane = (y if d == 0 else y + 1) - gy
                quads.append(make_quad(d, (a - gx, plane, -t), (b - gx, plane, t),
                                       lambda c, y=y: (u0 + c[0] + gx, v0 + y + 0.5 + (SLIVER if c[2] > 0 else -SLIVER))))
    for d, dx in ((4, -1), (5, 1)):   # the edges that face left (-x) and right (+x), column by column
        for x in range(w):
            for a, b in runs([solid(x, y) and not solid(x + dx, y) for y in range(h)]):
                plane = (x if d == 4 else x + 1) - gx
                quads.append(make_quad(d, (plane, a - gy, -t), (plane, b - gy, t),
                                       lambda c, x=x: (u0 + x + 0.5 + (SLIVER if c[2] > 0 else -SLIVER), v0 + c[1] + gy)))
    return quads


def weapon_meta(cat, wid, cube):
    """The checked json of a weapon: thickness, grip (a pixel for a sprite), offset, rotation and pull; the same rules for a
    sprite and a cube weapon. Returns a dict."""
    w = cat.weapons[wid]
    what = 'weapon ' + wid
    tiered = WEAPON_TIER.fullmatch(wid)
    if tiered:
        base, tier = tiered.group(1), int(tiered.group(2))
        if tier not in cat.tiers or tier == 1:
            fail('%s: tier %d is no weapon tier (a tier model is <weapon>_t2 to _t%d; tier I is <weapon>)' % (what, tier, max(cat.tiers)))
        if base not in cat.weapons:
            fail('%s: a weapon tier of %s, which art/weapons lacks (tier I is %s.json)' % (what, base, base))
    if 'boxes' in w or 'size' in w:
        fail('%s: boxes and size are gone (format 2): a weapon is a Blockbench model, art/weapons/%s.bbmodel, or a sprite, '
             'art/weapons/%s.png, that the build extrudes' % (what, wid, wid))
    keys(w, WEAPON_KEYS, what)
    if not cube and 'density' in w:
        fail('%s: density belongs to a .bbmodel weapon; a sprite weapon is one texel to a px' % what)
    density = density_of(w, what, WEAPON_DENSITY)
    thickness = w.get('thickness')
    if isinstance(thickness, bool) or thickness not in THICKNESS:
        fail('%s: thickness must be 1, 2 or 3 (px of depth), not %s' % (what, thickness))
    grip = w.get('grip')
    keys(grip, {'pixel', 'offset', 'rotation', 'note'}, what + ' grip')
    if cube and 'pixel' in grip:
        fail('%s: grip pixel is gone from a .bbmodel weapon: the origin of the model is the grip (art pipeline v2 section 2.1)' % what)
    pixel = None if cube else vec(grip.get('pixel'), 2, what + ' grip pixel', ints=True, minimum=0)
    offset = vec(grip.get('offset', [0, 0, 0]), 3, what + ' grip offset')
    rotation = vec(grip.get('rotation', [0, 0, 0]), 3, what + ' grip rotation')
    pull = w.get('pull')
    if pull is not None:
        pull = vec(pull, PULL_FRAMES, what + ' pull')
        if any(b <= a for a, b in zip(pull, pull[1:])):
            fail('%s: pull %s does not rise strictly' % (what, [num(p) for p in pull]))
        if pull[0] < 0 or pull[-1] > PULL_MAX:
            fail('%s: pull %s must lie between 0 and %s seconds' % (what, [num(p) for p in pull], num(PULL_MAX)))
    return {'what': what, 'thickness': thickness, 'pixel': pixel, 'offset': offset, 'rotation': rotation, 'pull': pull,
            'nocks': w.get('nocks'), 'density': density}


def weapon_model(wid, kind, size, meta, frames, far, texture, normal, specular, glow):
    """A weapon's model json (format 3): the keys of format 2 plus its kind ('sprite' or 'cube'), the far version, and the
    ids of the normal and specular maps (informational: Iris finds them by suffix). A cube weapon whose density is not
    WEAPON_DENSITY says so right after its size (the sheet and the quads are in real texels either way)."""
    out = {'format': FORMAT, 'weapon': wid, 'kind': kind, 'size': list(size)}
    if kind == 'cube' and meta['density'] != WEAPON_DENSITY:
        out['density'] = meta['density']
    out.update({'thickness': meta['thickness'],
                'grip': {'offset': [num(c) for c in meta['offset']], 'rotation': [num(c) for c in meta['rotation']]},
                'pull': [num(p) for p in meta['pull']] if meta['pull'] is not None else [], 'frames': frames, 'far': far,
                'texture': texture, 'normal': normal, 'specular': specular, 'glow': glow})
    return out


def sprite_weapon_outputs(cat, wid):
    """The files of a weapon that has only a PNG (design 2 of the troop depth design): its model with the quads of the
    extruded sprite frames, the packed texture and the packed glow. Returns (files, lit glow pixels, [(weapon, frame, quads)])."""
    meta = weapon_meta(cat, wid, False)
    what, thickness, pixel, pull, nocks = meta['what'], meta['thickness'], meta['pixel'], meta['pull'], meta['nocks']
    folder = cat.root / 'art/weapons'
    if pull is None:
        names, files = ['rest'], [wid + '.png']
        stray = sorted(p.name for p in folder.glob('%s_pull_*.png' % wid))
        if stray:
            fail('%s: %s without a pull key (the three times of bow_draw)' % (what, stray[0]))
    else:
        names = ['rest'] + ['pull_%d' % i for i in range(PULL_FRAMES)]
        files = [wid + '.png'] + ['%s_pull_%d.png' % (wid, i) for i in range(PULL_FRAMES)]
    frames = []
    for name, f in zip(names, files):
        if not (folder / f).is_file():
            fail('%s: %s is missing from art/weapons' % (what, f))
        im = read_png(folder / f, what)
        if im.width > FRAME_MAX or im.height > FRAME_MAX:
            fail('%s: %s is %dx%d, over the %d px a frame may be' % (what, f, im.width, im.height, FRAME_MAX))
        if frames and im.size != frames[0].size:
            fail('%s: %s is %dx%d, not the rest frame size %dx%d' % (what, f, im.width, im.height, frames[0].width, frames[0].height))
        tint(im, '%s: %s' % (what, f), cat.factions)
        frames.append(im)
    fw, fh = frames[0].size
    if pixel[0] >= fw or pixel[1] >= fh:
        fail('%s: grip pixel %s is outside the frame, %dx%d' % (what, pixel, fw, fh))
    for name, im in zip(names, frames):
        if im.getpixel(tuple(pixel))[3] == 0:
            fail('%s: grip pixel %s is on a transparent pixel of frame %s' % (what, pixel, name))
    if pull is None:
        if nocks is not None:
            fail('%s: nocks without a pull key: nothing to nock' % what)
    else:
        if not isinstance(nocks, list) or len(nocks) != len(frames):
            fail('%s: nocks must hold one [x, y] a frame (%d), the middle of the string in sprite px' % (what, len(frames)))
        nocks = [vec(n, 2, '%s nock' % what) for n in nocks]
    glow = None
    if (folder / (wid + '_e.png')).is_file():
        if pull is not None:
            fail('%s: %s_e.png beside pull: a draw weapon has no glow' % (what, wid))
        glow = read_png(folder / (wid + '_e.png'), what)
        if glow.size != frames[0].size:
            fail('%s: %s_e.png is not the frame size' % (what, wid))
    gx, gy = pixel[0] + 0.5, pixel[1] + 0.5
    out_frames, stats = [], []
    for i, (name, im) in enumerate(zip(names, frames)):
        quads = sprite_mesh(im, thickness, pixel, i)
        if len(quads) > WEAPON_CAP:
            fail('%s: frame %s has %d quads, over the %d quads a frame may have' % (what, name, len(quads), WEAPON_CAP))
        nock = None if nocks is None else [num(nocks[i][0] - gx), num(nocks[i][1] - gy), 0]
        out_frames.append({'name': name, 'nock': nock, 'quads': quads})
        stats.append((wid, name, len(quads)))
    sheet = Image.new('RGBA', (fw * len(frames), fh), (0, 0, 0, 0))
    for i, im in enumerate(frames):
        sheet.paste(im, (i * fw, 0))
    model = weapon_model(wid, 'sprite', sheet.size, meta, out_frames, [], texture_id('weapon/%s.png' % wid), '', '',
                         texture_id('weapon/%s_e.png' % wid) if glow else '')
    out = {'%s/weapon/%s.json' % (MODELS, wid): dumps(model), '%s/weapon/%s.png' % (TEXTURES, wid): png_bytes(sheet)}
    if glow:
        lit_sheet = Image.new('RGBA', sheet.size, (0, 0, 0, 0))
        lit_sheet.paste(glow, (0, 0))
        out['%s/weapon/%s_e.png' % (TEXTURES, wid)] = png_bytes(lit_sheet)
    return out, (lit(glow) if glow else 0), stats


# ---- cube weapons: the .bbmodel, the far version and the LabPBR maps (art pipeline v2) ----

def glow_source(cat, wid):
    """The _e.png a cube weapon is lit by: its own, or for a tier model (<weapon>_t2, _t3) the base weapon's, as the heart is
    the same cube at the same sheet spot in every tier. Returns the path or None."""
    folder = cat.root / 'art/weapons'
    if (folder / (wid + '_e.png')).is_file():
        return folder / (wid + '_e.png')
    tier = re.fullmatch(r'(.+)_t[23]', wid)
    if tier and tier.group(1) in cat.weapons and (folder / (tier.group(1) + '_e.png')).is_file():
        return folder / (tier.group(1) + '_e.png')
    return None


def far_view(quads, colour, keys, lit_sheet):
    """The far version's sprite (the fourth cut): the rest frame seen from the front (the weapon frame's -z side), one px to
    a model px, a px solid where at least half of its samples hit a face; its colour, key colour and glow are the ones most
    of its samples read, and a texel the sheet leaves transparent is a cutout. quads are the frame's 20-number quads (the
    caller leaves a string's out: it is under a px).
    Returns the colour, key and lit images of the sprite and its grip pixel (the one whose centre is the model's origin)."""
    cpx, kpx = colour.load(), keys.load()
    lpx = lit_sheet.load() if lit_sheet is not None else None
    n = FAR_SAMPLES
    xs = [q[i] for q in quads for i in (0, 3, 6, 9)]
    ys = [q[i] for q in quads for i in (1, 4, 7, 10)]
    ix0, ix1 = math.floor(min(xs) + 0.5), math.floor(max(xs) + 0.5)
    iy0, iy1 = math.floor(min(ys) + 0.5), math.floor(max(ys) + 0.5)
    w, h = ix1 - ix0 + 1, iy1 - iy0 + 1
    depth = {}   # (sample column, sample row) -> (z, the texel it reads); sample i lies at x = (i + 0.5) / n
    for q in quads:
        c0, c1, c3 = q[0:3], q[3:6], q[9:12]
        e1, e3 = [c1[k] - c0[k] for k in range(3)], [c3[k] - c0[k] for k in range(3)]
        det = e1[0] * e3[1] - e1[1] * e3[0]
        if det >= -1e-9:   # faces away from the viewer at -z, or stands on edge
            continue
        t0, t1, t3 = q[12:14], q[14:16], q[16:18]
        t2 = (t1[0] + t3[0] - t0[0], t1[1] + t3[1] - t0[1])
        umin, umax = min(t0[0], t1[0], t3[0], t2[0]), max(t0[0], t1[0], t3[0], t2[0])
        vmin, vmax = min(t0[1], t1[1], t3[1], t2[1]), max(t0[1], t1[1], t3[1], t2[1])
        px = [c0[0], c1[0], c1[0] + c3[0] - c0[0], c3[0]]
        py = [c0[1], c1[1], c1[1] + c3[1] - c0[1], c3[1]]
        for sj in range(math.floor(min(py) * n), math.ceil(max(py) * n)):
            y = (sj + 0.5) / n
            for si in range(math.floor(min(px) * n), math.ceil(max(px) * n)):
                x = (si + 0.5) / n
                a = ((x - c0[0]) * e3[1] - (y - c0[1]) * e3[0]) / det
                b = (e1[0] * (y - c0[1]) - e1[1] * (x - c0[0])) / det
                if a < -1e-9 or a > 1 + 1e-9 or b < -1e-9 or b > 1 + 1e-9:
                    continue
                z = c0[2] + a * e1[2] + b * e3[2]
                if (si, sj) in depth and depth[(si, sj)][0] <= z:
                    continue
                u = min(max(t0[0] + a * (t1[0] - t0[0]) + b * (t3[0] - t0[0]), umin), umax - 1e-6)
                v = min(max(t0[1] + a * (t1[1] - t0[1]) + b * (t3[1] - t0[1]), vmin), vmax - 1e-6)
                tx = min(max(int(math.floor(u)), 0), colour.width - 1)
                ty = min(max(int(math.floor(v)), 0), colour.height - 1)
                if cpx[tx, ty][3] == 0:
                    continue   # an unpainted texel of a patch rounded up to whole texels: a cutout
                depth[(si, sj)] = (z, (cpx[tx, ty], kpx[tx, ty], bool(lpx and lpx[tx, ty][3])))
    far_c, far_k, far_l = (Image.new('RGBA', (w, h), (0, 0, 0, 0)) for _ in range(3))
    for row in range(h):
        for col in range(w):
            first_i, first_j = (col + ix0) * n - n // 2, (row + iy0) * n - n // 2
            hits = [depth[(first_i + a, first_j + b)][1] for b in range(n) for a in range(n) if (first_i + a, first_j + b) in depth]
            if len(hits) < n * n * FAR_COVER:
                continue
            record = max(Counter(hits).items(), key=lambda kv: kv[1])[0]
            far_c.putpixel((col, row), record[0])
            far_k.putpixel((col, row), record[1])
            if record[2]:
                far_l.putpixel((col, row), (255, 255, 255, 255))
    return far_c, far_k, far_l, (-ix0, -iy0)


def far_strings(far_c, far_k, grip, quads, colour, keys):
    """Draws each string of the rest frame into the far sprite as a one px line along its middle, on px the bow leaves
    clear (task 16 r10: a troop past TroopDepth.NEAR_BLOCKS draws the far version, and Kort asked that the bowstring read at
    30 blocks). A string's line runs between the middles of the short edges of its broad face seen from the front (-z);
    its colour and key are the texels at that face's centre."""
    cpx, kpx = colour.load(), keys.load()
    for q in quads:
        if q.face != 'north':
            continue
        c = q.corners
        e01 = math.dist(c[0][:2], c[1][:2]); e03 = math.dist(c[0][:2], c[3][:2])
        c2 = [c[1][k] + c[3][k] - c[0][k] for k in range(3)]
        if e01 < e03:
            a, b = [(c[0][k] + c[1][k]) / 2 for k in (0, 1)], [(c[3][k] + c2[k]) / 2 for k in (0, 1)]
        else:
            a, b = [(c[0][k] + c[3][k]) / 2 for k in (0, 1)], [(c[1][k] + c2[k]) / 2 for k in (0, 1)]
        t = q.texels
        u = (t[0][0] + t[1][0] + t[2][0] + t[3][0]) / 4 if len(t) == 4 else (t[1][0] + t[2][0]) / 2
        v = (t[0][1] + t[1][1] + t[2][1] + t[3][1]) / 4 if len(t) == 4 else (t[1][1] + t[2][1]) / 2
        tx, ty = min(max(int(u), 0), colour.width - 1), min(max(int(v), 0), colour.height - 1)
        if cpx[tx, ty][3] == 0:
            continue
        # r11: two px wide, the second line one px further from the grip, so at 30 blocks the string is a 2 px pale line
        # with the bow's own gap left between it and the stave (a line, not a rod)
        ln = math.hypot(b[0] - a[0], b[1] - a[1]) or 1.0
        nx, ny = -(b[1] - a[1]) / ln, (b[0] - a[0]) / ln
        if nx * (a[0] + b[0]) + ny * (a[1] + b[1]) < 0:
            nx, ny = -nx, -ny
        steps = max(1, int(math.ceil(max(abs(b[0] - a[0]), abs(b[1] - a[1])) * 2)))
        for off in (0.0, 1.0):
            for i in range(steps + 1):
                x, y = a[0] + (b[0] - a[0]) * i / steps + nx * off, a[1] + (b[1] - a[1]) * i / steps + ny * off
                col, row = math.floor(x + 0.5) + grip[0], math.floor(y + 0.5) + grip[1]
                if 0 <= col < far_c.width and 0 <= row < far_c.height and far_c.getpixel((col, row))[3] == 0:
                    far_c.putpixel((col, row), cpx[tx, ty])
                    far_k.putpixel((col, row), kpx[tx, ty])


def far_quads(sprite, thickness, grip, v0):
    """The far sprite extruded, cut to FAR_CAP quads: the two big faces, then the longest side runs, in their own order."""
    quads = sprite_mesh(sprite, thickness, grip, 0, v0)
    if len(quads) <= FAR_CAP:
        return quads
    longest = sorted(range(2, len(quads)), key=lambda i: (-max(quads[i][4] - quads[i][1], quads[i][5] - quads[i][2]), i))
    keep = set(longest[:FAR_CAP - 2])
    return quads[:2] + [q for i, q in enumerate(quads) if i in keep]


def luma(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255


def material_maps(colour, keys, rects, lit_sheet=None, density=WEAPON_DENSITY):
    """The _n and _s maps of a weapon sheet from its colour sheet and its key colours (art pipeline v2 section 6). _s: R the
    material's smoothness, +12 a tone step above the patch's face tone (the most common colour of its patch), G its F0, B its
    porosity, A 255 or 200 on a texel lit in lit_sheet; a texel with no material is (0, 0, 0, 255). _n: R and G the tilt of
    the height field across the texel (DirectX, green down), B 255, A the height (0.5 flat, 0.05 a tone step, never 0); every
    patch edge drops to nothing, so its edge texels tilt outward, by EDGE_TILT at the weapons' 2 texels a px and less on a finer
    sheet (a texel is 1/density px, so the same bevel drops by a smaller step: EDGE_TILT x 2 / density). rects are the
    patches, as (x0, y0, x1, y1) texels."""
    tilt = EDGE_TILT * WEAPON_DENSITY / density
    w, h = colour.size
    cpx, kpx = colour.load(), keys.load()
    lpx = lit_sheet.load() if lit_sheet is not None else None
    patch = [[None] * w for _ in range(h)]
    for i, (x0, y0, x1, y1) in enumerate(rects):
        for y in range(max(0, y0), min(h, y1)):
            for x in range(max(0, x0), min(w, x1)):
                if patch[y][x] is None and cpx[x, y][3]:
                    patch[y][x] = i
    for y in range(h):
        for x in range(w):
            if patch[y][x] is None and cpx[x, y][3]:
                patch[y][x] = -1   # a texel no patch claims: one patch of its own
    colours = {}
    for y in range(h):
        for x in range(w):
            if patch[y][x] is not None:
                colours.setdefault(patch[y][x], Counter())[tuple(cpx[x, y][:3])] += 1
    face_luma = {p: luma(max(c.items(), key=lambda kv: kv[1])[0]) for p, c in colours.items()}
    tone = [[0] * w for _ in range(h)]
    height = [[0.0] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            if patch[y][x] is not None:
                step = round((luma(cpx[x, y]) - face_luma[patch[y][x]]) / TONE_LUMA)
                tone[y][x] = max(-TONE_MAX, min(TONE_MAX, step))
                height[y][x] = HEIGHT_BASE + HEIGHT_TONE * tone[y][x]
    normal = Image.new('RGBA', (w, h), (128, 128, 255, 255))
    spec = Image.new('RGBA', (w, h), (0, 0, 0, 255))
    for y in range(h):
        for x in range(w):
            pid = patch[y][x]
            if pid is None:
                continue

            def at(xx, yy):
                return height[yy][xx] if 0 <= xx < w and 0 <= yy < h and patch[yy][xx] == pid else 0.0
            nx = -(at(x + 1, y) - at(x - 1, y)) * tilt
            ny = -(at(x, y + 1) - at(x, y - 1)) * tilt
            ln = math.sqrt(nx * nx + ny * ny + 1.0)
            normal.putpixel((x, y), (int(round(128 + nx / ln * 127)), int(round(128 + ny / ln * 127)), 255,
                                     max(1, min(255, int(height[y][x] * 255 + 0.5)))))
            material = MATERIALS.get(tuple(kpx[x, y][:3]))
            if material is None:
                continue
            name, smooth, f0, porosity = material
            if 238 <= f0 <= 254:
                fail('material %s has F0 %d, which LabPBR leaves undefined (238 to 254)' % (name, f0))
            spec.putpixel((x, y), (max(0, min(255, smooth + TONE_SMOOTH * tone[y][x])), f0, porosity,
                                   GLOW_EMISSION if lpx and lpx[x, y][3] else 255))
    return normal, spec


def embedded_png(url):
    """The image a base64 PNG data URL carries, or None when the URL is anything else."""
    head = 'data:image/png;base64,'
    if not url.startswith(head):
        return None
    try:
        with Image.open(io.BytesIO(base64.b64decode(url[len(head):], validate=True))) as im:
            return im.convert('RGBA')
    except Exception:
        return None


def key_sheet(cat, wid, colour):
    """art/weapons/<wid>_m.png, the material map, checked against the colour sheet: its size, a key colour on every opaque
    texel and none on a transparent one, every key colour in MATERIALS."""
    what = 'weapon ' + wid
    path = cat.root / 'art/weapons' / (wid + '_m.png')
    if not path.is_file():
        fail('%s: art/weapons/%s_m.png is missing (the material map: a key colour on every opaque texel, art pipeline v2 section 6)' % (what, wid))
    keys = read_png(path, what)
    if keys.size != colour.size:
        fail('%s: art/weapons/%s_m.png is %dx%d, not the colour sheet\'s %dx%d' % (what, wid, keys.width, keys.height, colour.width, colour.height))
    cpx, kpx = colour.load(), keys.load()
    for y in range(colour.height):
        for x in range(colour.width):
            opaque, keyed = cpx[x, y][3] > 0, kpx[x, y][3] > 0
            if opaque and not keyed:
                fail('%s: art/weapons/%s_m.png texel (%d, %d) is opaque in the colour sheet but has no key colour' % (what, wid, x, y))
            if keyed and not opaque:
                fail('%s: art/weapons/%s_m.png texel (%d, %d) is transparent in the colour sheet but has the key colour #%02X%02X%02X'
                     % ((what, wid, x, y) + tuple(kpx[x, y][:3])))
            if keyed and tuple(kpx[x, y][:3]) not in MATERIALS:
                fail('%s: art/weapons/%s_m.png key colour #%02X%02X%02X at texel (%d, %d) is not in the material table'
                     % ((what, wid) + tuple(kpx[x, y][:3]) + (x, y)))
    return keys


def cube_weapon_outputs(cat, wid):
    """The files of a weapon made in Blockbench (art pipeline v2): its model (the quads of each frame group, 20 numbers
    each, and the far version), the colour sheet with the far version's strip under it, the _n and _s maps, and the glow.
    Returns (files, lit glow pixels, [(weapon, frame, quads)])."""
    meta = weapon_meta(cat, wid, True)
    what, thickness, pull, nocks, density = meta['what'], meta['thickness'], meta['pull'], meta['nocks'], meta['density']
    folder = cat.root / 'art/weapons'
    shown = 'art/weapons/%s.bbmodel' % wid

    def bad(why):
        fail('%s: %s %s' % (what, shown, why))
    try:
        model = bbmodel.read(cat.bbmodels[wid])
        quads_of = bbmodel.weapon_frames(model)
    except bbmodel.BBError as e:
        fail('%s: %s' % (what, e))
    if 'rest' not in model.frames:
        bad('has no frame group rest (its top-level groups are %s)' % (', '.join(model.frames) or 'none'))
    for name in model.frames:
        if name not in WEAPON_FRAMES:
            bad('has a top-level group %s, which is none of rest, pull_0, pull_1 or pull_2 (the frame groups)' % name)
    names = [n for n in WEAPON_FRAMES if n in model.frames]
    if pull is None:
        if len(names) > 1:
            bad('has frame group %s without a pull key in %s.json (the three times of bow_draw)' % (names[1], wid))
    else:
        for n in WEAPON_FRAMES:
            if n not in model.frames:
                bad('frame group %s is missing: a weapon with a pull key has rest, pull_0, pull_1 and pull_2' % n)
    for problem in bbmodel.check_textures(model, kind='weapon', density=density):
        bad(problem)
    texture = model.textures[0]
    png = folder / (wid + '.png')
    if not png.is_file():
        bad('is missing its texture art/weapons/%s.png' % wid)
    colour = read_png(png, what)
    if colour.size != (texture.width, texture.height):
        bad('texture is %dx%d in the project but art/weapons/%s.png is %dx%d' % (texture.width, texture.height, wid, colour.width, colour.height))
    embedded = embedded_png(texture.source)
    if embedded is None:
        bad('has no embedded PNG texture (its source is %s); save the project with the texture embedded in Blockbench' %
            (texture.source[:24] + '...' if texture.source else 'empty'))
    if embedded.size != colour.size or embedded.tobytes() != colour.tobytes():
        a, b = embedded.tobytes(), colour.tobytes()
        at = next((i for i in range(min(len(a), len(b))) if a[i] != b[i]), 0) // 4
        bad('the embedded texture differs from art/weapons/%s.png at pixel (%d, %d); save the texture in Blockbench' %
            (wid, at % colour.width, at // colour.width))
    sheet_w, sheet_h = colour.size
    depth = max(c.hi[2] - c.lo[2] for c in model.frames['rest'])
    if abs(depth - thickness) > THICKNESS_SLACK:
        bad('thickness %d in %s.json but the rest frame is %s px deep across z (a thickness is the depth, to %s px)' %
            (thickness, wid, num(depth), num(THICKNESS_SLACK)))
    for name in names:
        cubes = model.frames[name]
        for c in cubes:
            for face, (_, uv) in sorted(c.faces.items()):
                if (min(uv[0], uv[2]) < -1e-6 or min(uv[1], uv[3]) < -1e-6 or max(uv[0], uv[2]) > sheet_w + 1e-6
                        or max(uv[1], uv[3]) > sheet_h + 1e-6):
                    bad('frame %s: cube %s face %s reads texels off the %dx%d sheet (UV %s)' %
                        (name, c.name, face, sheet_w, sheet_h, [num(v) for v in uv]))
        for cube, face, want_w, want_h, got_w, got_h in bbmodel.off_density(cubes, density):
            bad('frame %s: cube %s face %s is %s x %s texels, but %s its size is %s x %s (the sheet of this weapon has %d texels a px; '
                'no face is stretched)' % (name, cube, face, num(got_w), num(got_h), TIMES[density], num(want_w), num(want_h), density))
        for a, fa, b, fb, dist, overlap in bbmodel.coplanar_faces(cubes):
            bad('frame %s: cube %s face %s and cube %s face %s are coplanar (%s px apart) and overlap by %s px squared, so '
                'they z-fight; move one off or put them back to back' % (name, a, fa, b, fb, num(dist), num(overlap)))
        pieces, free = bbmodel.joined(cubes)
        if len(pieces) > 1:
            bad('frame %s has an air gap: it is not one piece (%s); every cube must touch or overlap the rest' %
                (name, ' | '.join(', '.join(p) for p in pieces)))
        if free:
            bad('frame %s: the string %s hangs %s px from every other cube, over the 0.2 px a string may hang from its nocks' %
                (name, free[0][0], num(free[0][1])))
        if len(quads_of[name]) > WEAPON_CAP:
            bad('frame %s has %d quads, over the %d quads a frame may have' % (name, len(quads_of[name]), WEAPON_CAP))
    if pull is None:
        if nocks is not None:
            fail('%s: nocks without a pull key: nothing to nock' % what)
    else:
        if not isinstance(nocks, list) or len(nocks) != len(names):
            fail('%s: nocks must hold one [x, y] a frame (%d), the middle of the string in the weapon frame\'s px' % (what, len(names)))
        nocks = [vec(n, 2, '%s nock' % what) for n in nocks]
        for name, nock in zip(names, nocks):
            strings = [c for c in model.frames[name] if bbmodel.is_string(c)]
            reach = min((bbmodel.distance(c, [nock[0], -nock[1], 0.0]) for c in strings), default=None)
            if reach is None or reach > NOCK_REACH:
                bad('frame %s: nock [%s, %s] is not on the string (%s)' % (
                    name, num(nock[0]), num(nock[1]), 'the frame has no cube called string' if reach is None else
                    'the nearest string cube is %s px away, over the %s px a nock may be' % (num(reach), num(NOCK_REACH))))
    glow_path = glow_source(cat, wid)
    glow = None
    if glow_path is not None:
        if pull is not None:
            fail('%s: %s beside pull: a draw weapon has no glow' % (what, glow_path.name))
        glow = read_png(glow_path, what)
        if glow.size != colour.size:
            fail('%s: art/weapons/%s is %dx%d, not the colour sheet\'s %dx%d' % (what, glow_path.name, glow.width, glow.height, sheet_w, sheet_h))
        for y in range(sheet_h):
            for x in range(sheet_w):
                if glow.getpixel((x, y))[3] and not colour.getpixel((x, y))[3]:
                    fail('%s: art/weapons/%s lights texel (%d, %d), which is transparent in the colour sheet' % (what, glow_path.name, x, y))
    keys = key_sheet(cat, wid, colour)
    tint(colour, '%s: art/weapons/%s.png' % (what, wid), cat.factions)
    frames_out, stats = [], []
    for i, name in enumerate(names):
        nock = None if nocks is None else [num(nocks[i][0]), num(nocks[i][1]), 0]
        frames_out.append({'name': name, 'nock': nock, 'quads': quads_of[name]})
        stats.append((wid, name, len(quads_of[name])))
    # a string is under a px: at one px to a model px it would vanish or become a plank by the luck of its alignment, so it is
    # left out of the sampled view and drawn after as a one px line along its middle (far_strings)
    rest_quads = bbmodel.frame_quads(model)['rest']
    solid = [q.numbers() for q in rest_quads if not bbmodel.is_string(q.cube)]
    far_c, far_k, far_l, grip = far_view(solid or quads_of['rest'], colour, keys, glow)
    far_strings(far_c, far_k, grip, [q for q in rest_quads if bbmodel.is_string(q.cube)], colour, keys)
    if far_c.width > sheet_w:
        fail('%s: its far version is %d px wide, over the %d px of the sheet' % (what, far_c.width, sheet_w))
    far = far_quads(far_c, thickness, grip, sheet_h)
    total = (sheet_w, sheet_h + far_c.height)

    def stacked(top, strip):
        out = Image.new('RGBA', total, (0, 0, 0, 0))
        if top is not None:
            out.paste(top, (0, 0))
        out.paste(strip, (0, sheet_h))
        return out
    colour_full, keys_full, lit_full = stacked(colour, far_c), stacked(keys, far_k), stacked(glow, far_l)
    rects = bbmodel.face_rects(model) + [(0, sheet_h, far_c.width, sheet_h + far_c.height)]
    normal, spec = material_maps(colour_full, keys_full, rects, lit_full if glow else None, density)
    base = 'weapon/%s' % wid
    out_model = weapon_model(wid, 'cube', total, meta, frames_out, far, texture_id(base + '.png'), texture_id(base + '_n.png'),
                             texture_id(base + '_s.png'), texture_id(base + '_e.png') if glow else '')
    out = {'%s/%s.json' % (MODELS, base): dumps(out_model), '%s/%s.png' % (TEXTURES, base): png_bytes(colour_full),
           '%s/%s_n.png' % (TEXTURES, base): png_bytes(normal), '%s/%s_s.png' % (TEXTURES, base): png_bytes(spec)}
    if glow:
        out['%s/%s_e.png' % (TEXTURES, base)] = png_bytes(lit_full)
    # a weapon's lit texels keep the unit they always had, a texel of its default density 2 counting 1 (so a 2x weapon's cap is no looser
    # than it was before the texel-3 work); at another density the same lit area counts the same: texels / (density / 2) squared
    return out, (lit(glow) * WEAPON_DENSITY ** 2 / density ** 2 if glow else 0), stats


def weapon_outputs(cat, wid):
    """The files of one weapon: its model, the packed texture, the glow and for a cube weapon the maps. A weapon with a
    .bbmodel is built from its cubes; one with only a PNG is extruded as a sprite. Returns (files, lit glow pixels,
    [(weapon, frame, quads)])."""
    if wid in cat.bbmodels:
        return cube_weapon_outputs(cat, wid)
    return sprite_weapon_outputs(cat, wid)


# ---- the relief layer (Task 5d, design 3 and 7) ----

DIRECTION_NAMES = ('top', 'bottom', 'north', 'south', 'west', 'east')   # by Direction index: 0 down is -y, the model's top
OVERLAP_EPS = 1e-3   # px of penetration an overlap must exceed: boxes that only touch do not overlap


def dot3(a, b):
    return sum(a[i] * b[i] for i in range(3))


def cross3(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]


class Face:
    """One face of a ModelPart.Cuboid as the 1.20.1 constructor lays it out: its geometric normal d, the texel rectangle it
    reads, and the affine map between its plane and the texels. The position p0 + a * e1 + b * e3 reads the texel
    (u2 + a * du, v1 + b * dv), the vanilla vertices 0, 1 and 3 and their UVs. The texels are those of the troop's relief layer
    (and skin), `density` times the layout, so a face of a density 3 troop reads three times as many texels a px."""

    def __init__(self, d, rect, verts, uvs, density=1):
        self.d, self.rect, self.density = d, rect, density
        self.p0 = list(verts[0])
        self.e1 = [verts[1][i] - verts[0][i] for i in range(3)]
        self.e3 = [verts[3][i] - verts[0][i] for i in range(3)]
        self.u2, self.v1 = uvs[0]
        self.du, self.dv = uvs[1][0] - uvs[0][0], uvs[3][1] - uvs[0][1]
        self.ax = tuple(max(range(3), key=lambda i: abs(e[i])) for e in (self.e1, self.e3))   # the model axes u and v run along

    def tex(self, pos):
        """The texel coordinates of a position, projected on the face's plane (the normal's coordinate does not matter)."""
        rel = [pos[i] - self.p0[i] for i in range(3)]
        a, b = dot3(rel, self.e1) / dot3(self.e1, self.e1), dot3(rel, self.e3) / dot3(self.e3, self.e3)
        return self.u2 + a * self.du, self.v1 + b * self.dv

    def at(self, u, v):
        """The position on the face's plane that reads texel coordinates (u, v)."""
        a, b = (u - self.u2) / self.du, (v - self.v1) / self.dv
        return [self.p0[i] + a * self.e1[i] + b * self.e3[i] for i in range(3)]


CUBOID_FACE = {0: 'top', 1: 'bottom', 2: 'front', 3: 'back', 4: 'right', 5: 'left'}   # Direction id -> FACEUV name


def cuboid_faces(box, density=1):
    """The faces of a baked box by geometric normal, transcribed from ModelPart.Cuboid (1.20.1 yarn): the eight vertices, the
    UV rectangles and the quad lists, a mirrored box swapping its x vertices and flipping its normals' x. The rectangles and
    UVs are in the texels of a layer of this density (the layout's units times it)."""
    (u, v), (sx, sy, sz) = box['uv'], box['size']
    o, dl = box['origin'], box['dilation']
    x, y, z = o[0] - dl[0], o[1] - dl[1], o[2] - dl[2]
    j, k, l = o[0] + sx + dl[0], o[1] + sy + dl[1], o[2] + sz + dl[2]
    if box['mirror']:
        x, j = j, x
    vert = {1: (x, y, z), 2: (j, y, z), 3: (j, k, z), 4: (x, k, z), 5: (x, y, l), 6: (j, y, l), 7: (j, k, l), 8: (x, k, l)}
    n, o2, p, q, r = u + sz, u + sz + sx, u + sz + 2 * sx, u + 2 * sz + sx, u + 2 * sz + 2 * sx
    t, w = v + sz, v + sz + sy
    out = {}
    for d, names, u1, v1, u2, v2 in ((0, (6, 5, 1, 2), n, v, o2, t), (1, (3, 4, 8, 7), o2, t, p, v),
                                     (4, (1, 5, 8, 4), u, t, n, w), (2, (2, 1, 4, 3), n, t, o2, w),
                                     (5, (6, 2, 3, 7), o2, t, q, w), (3, (5, 6, 7, 8), q, t, r, w)):
        moved = box.get('faceuv', {}).get(CUBOID_FACE[d])
        if moved:   # a faceuv face: the same rectangle at its own top-left texel (check_boxes: such a box is unmirrored)
            du, dv = moved[0] - min(u1, u2), moved[1] - min(v1, v2)
            u1, u2, v1, v2 = u1 + du, u2 + du, v1 + dv, v2 + dv
        if box['mirror'] and d in (4, 5):
            d ^= 1
        k = density
        out[d] = Face(d, (k * min(u1, u2), k * min(v1, v2), k * max(u1, u2), k * max(v1, v2)), [vert[i] for i in names],
                      [(k * u2, k * v1), (k * u1, k * v1), (k * u1, k * v2), (k * u2, k * v2)], density)
    return out


def face_rects(box, density=1):
    """The texel rectangle of each of the box's faces that has an area, by geometric normal."""
    return {d: f.rect for d, f in cuboid_faces(box, density).items() if f.rect[2] > f.rect[0] and f.rect[3] > f.rect[1]}


def box_extent(box):
    return ([box['origin'][i] - box['dilation'][i] for i in range(3)],
            [box['origin'][i] + box['size'][i] + box['dilation'][i] for i in range(3)])


def obb(rt, lo, hi):
    """An axis-aligned box of a bone's frame as an oriented box in model space: (centre, half sizes, axes, radius)."""
    r, t = rt
    c = [(lo[i] + hi[i]) / 2 for i in range(3)]
    half = [(hi[i] - lo[i]) / 2 for i in range(3)]
    return ([t[i] + sum(r[i][k] * c[k] for k in range(3)) for i in range(3)], half,
            [[r[i][k] for i in range(3)] for k in range(3)], math.sqrt(sum(h * h for h in half)))


def obb_overlap(a, b, eps=OVERLAP_EPS):
    """Whether two oriented boxes penetrate each other by more than eps along every axis (separating axis test)."""
    ca, ha, aa, ra = a
    cb, hb, ab, rb = b
    d = [cb[i] - ca[i] for i in range(3)]
    if dot3(d, d) >= (ra + rb) ** 2:
        return False
    axes = list(aa) + list(ab)
    for u in aa:
        for v in ab:
            c = cross3(u, v)
            n = math.sqrt(dot3(c, c))
            if n > 1e-6:
                axes.append([x / n for x in c])
    for axis in axes:
        reach = sum(ha[i] * abs(dot3(axis, aa[i])) for i in range(3)) + sum(hb[i] * abs(dot3(axis, ab[i])) for i in range(3))
        if abs(dot3(axis, d)) >= reach - eps:
            return False
    return True


class Boxes:
    """Every box of a model as an oriented box in model space at rest, for the voxel overlap test. A box of a part that can
    hide (a helm part, or one limited to some stages) is no obstacle: the relief shows when the part hides."""

    def __init__(self, model):
        self.rt = world(model['bones'])
        self.items = []
        for b in model['bones']:
            hide = b['tag'] == 'helm' or bool(b['stages']) or bool(b.get('weaponTiers'))
            for i, box in enumerate(b['boxes']):
                self.items.append((b['name'], i, hide, obb(self.rt[b['name']], *box_extent(box))))

    def hit(self, bone, index, lo, hi):
        """The (bone, box index) that a cell box of box `index` of `bone` overlaps at rest, or None."""
        me = obb(self.rt[bone], lo, hi)
        for name, i, hide, other in self.items:
            if not hide and (name, i) != (bone, index) and obb_overlap(me, other):
                return name, i
        return None


class Grid:
    """The occupancy grid of one box (design 3, steps 2 and 3): (w+2) x (h+2) x (d+2) cells in texel units, the box inside
    and the voxels of its raised texels in the one-cell shell around it. In the plane of a face a cell is as big as one texel
    on the box, so on a scaled box it is as many px as the scale divided by the density; across it (the shell cells' depth
    along their normal) it is one layout px, 1 px times the scale, whatever the density, so a voxel of a density 3 troop
    stands a full px proud of its face and not a third of one (texel-3 plan Q1a): `cell` is that px and `step` the texel."""

    def __init__(self, box, density=1):
        self.box, self.density = box, density
        self.n = [s * density for s in box['size']]
        self.lo = box_extent(box)[0]
        self.cell = [(box['size'][i] + 2 * box['dilation'][i]) / box['size'][i] for i in range(3)]
        self.step = [c / density for c in self.cell]
        self.faces = cuboid_faces(box, density)
        self.voxels = {}   # shell cell -> (face d, tx, ty): the raised texel standing there
        self.edges, self.corners = {}, set()

    def cell_of(self, face, tx, ty):
        """The shell cell the voxel of texel (tx, ty) of a face stands in."""
        pos = face.at(tx + 0.5, ty + 0.5)
        c = [1 + math.floor((pos[i] - self.lo[i]) / self.step[i]) for i in range(3)]
        a = AXIS[face.d]
        c[a] = 0 if NORMALS[face.d][a] < 0 else self.n[a] + 1
        return tuple(c)

    def cell_box(self, c):
        shell = self.shell_axes(c)
        lo = [self.lo[i] + (c[i] - 1) * (self.cell[i] if c[i] == 0 else self.step[i]) for i in range(3)]
        return lo, [lo[i] + (self.cell[i] if i in shell else self.step[i]) for i in range(3)]

    def shell_axes(self, c):
        return [i for i in range(3) if c[i] in (0, self.n[i] + 1)]

    def inward(self, c, axis):
        c = list(c)
        c[axis] = 1 if c[axis] == 0 else self.n[axis]
        return tuple(c)

    def inside(self, c):
        return all(1 <= c[i] <= self.n[i] for i in range(3))

    def filled(self, c):
        return self.inside(c) or c in self.voxels or c in self.edges or c in self.corners

    def fill(self):
        """Edge and corner cells (design 3, step 3): an edge cell fills when the two face cells beside it are raised, a
        corner cell when its three edge cells are filled. A band that wraps a box edge is then one solid."""
        n = self.n
        for e in range(3):
            a, b = (e + 1) % 3, (e + 2) % 3
            for sa in (0, n[a] + 1):
                for sb in (0, n[b] + 1):
                    for i in range(1, n[e] + 1):
                        c = [0, 0, 0]
                        c[e], c[a], c[b] = i, sa, sb
                        c = tuple(c)
                        if self.inward(c, a) in self.voxels and self.inward(c, b) in self.voxels:
                            self.edges[c] = True
        for sx in (0, n[0] + 1):
            for sy in (0, n[1] + 1):
                for sz in (0, n[2] + 1):
                    c = (sx, sy, sz)
                    if all(self.inward(c, ax) in self.edges for ax in range(3)):
                        self.corners.add(c)

    def source(self, c, d):
        """The face voxel whose texel a free face (direction d) of shell cell c shows: the cell itself for a face voxel,
        else the voxel beside it on the face the free face continues, with every other shell coordinate moved inward."""
        if c in self.voxels:
            return c
        shell = self.shell_axes(c)
        keep = AXIS[d] if AXIS[d] in shell else shell[0]
        s = c
        for ax in shell:
            if ax != keep:
                s = self.inward(s, ax)
        return s

    def anchor(self, c):
        """The raised texel (face d, tx, ty) a shell cell is named after: its own, or that of a voxel beside an edge or corner cell."""
        if c in self.voxels:
            return self.voxels[c]
        s = c
        for ax in self.shell_axes(c)[1:]:
            s = self.inward(s, ax)
        return self.voxels[s]

    def cells(self):
        return sorted(set(self.voxels) | set(self.edges) | self.corners)


def sliver_tex(face, texel, axes, run, lo, hi):
    """The texel map of a side or corner quad: along its run axis (an in-plane axis of the face it reads, or None) the
    texels run straight; across, half a texel at the centre of the source texel, SLIVER either side. Every axis of the
    quad takes one texture axis, so no quad has a UV of zero area (the tangent Iris builds from it stays finite). The texels
    are those of the layer; the map returns layout units (the layer's texels over the face's density), which is what the
    model json keeps and what the client divides by the bone's sheet."""
    tex_axis = {}
    for m in axes:
        if m in face.ax:
            tex_axis[m] = face.ax.index(m)
    for m in axes:
        if m not in tex_axis:
            tex_axis[m] = ({0, 1} - set(tex_axis.values())).pop()
    centre = (texel[0] + 0.5, texel[1] + 0.5)

    def tex(pos):
        uv = [0.0, 0.0]
        for m in axes:
            if m == run:
                uv[tex_axis[m]] = face.tex(pos)[tex_axis[m]]
            else:
                uv[tex_axis[m]] = centre[tex_axis[m]] + (SLIVER if pos[m] > (lo[m] + hi[m]) / 2 else -SLIVER)
        return uv[0] / face.density, uv[1] / face.density
    return tex


def top_quads(g):
    """The faces on top of the voxels, merged per face into maximal rectangles of raised texels (greedy, along u then v);
    each quad's UV is the straight continuation of its texels."""
    quads = []
    for d in sorted(g.faces):
        f = g.faces[d]
        texels = {(x, y) for dd, x, y in g.voxels.values() if dd == d}
        left = set(texels)
        a = AXIS[d]
        plane = g.lo[a] - g.cell[a] if NORMALS[d][a] < 0 else g.lo[a] + g.n[a] * g.step[a] + g.cell[a]
        for y, x in sorted((y, x) for x, y in texels):
            if (x, y) not in left:
                continue
            x1 = x
            while (x1 + 1, y) in left:
                x1 += 1
            y1 = y
            while all((xx, y1 + 1) in left for xx in range(x, x1 + 1)):
                y1 += 1
            left -= {(xx, yy) for xx in range(x, x1 + 1) for yy in range(y, y1 + 1)}
            p, q = f.at(x, y), f.at(x1 + 1, y1 + 1)
            lo, hi = [min(p[i], q[i]) for i in range(3)], [max(p[i], q[i]) for i in range(3)]
            lo[a] = hi[a] = plane
            quads.append(make_quad(d, lo, hi, lambda pos, f=f: tuple(c / f.density for c in f.tex(pos))))
    return quads


def side_quads(g):
    """The free faces of the shell cells that are not the top of a face voxel: the sides of voxels, and the outer faces
    and caps of edge and corner cells. Faces against the box or another filled cell are never drawn. Faces on one line
    that continue each other's texels merge into runs (along a voxel's face, along an edge)."""
    groups = {}
    for c in g.cells():
        for d in range(6):
            a = AXIS[d]
            nb = list(c)
            nb[a] += NORMALS[d][a]
            if g.filled(tuple(nb)) or (c in g.voxels and g.voxels[c][0] == d):
                continue
            s = g.source(c, d)
            ds, tx, ty = g.voxels[s]
            face = g.faces[ds]
            shell = g.shell_axes(c)
            if c in g.voxels:
                run = next(i for i in range(3) if i not in (a, AXIS[ds]))
            elif a in shell:
                run = next((i for i in range(3) if i not in shell), None)
            else:
                run = None
            lo_a, hi_a = g.cell_box(c)
            plane = hi_a[a] if NORMALS[d][a] > 0 else lo_a[a]
            if run is None:
                key = (d, c)
            else:
                across = (tx, ty)[1 - face.ax.index(run)]
                key = (d, tuple(None if i == run else c[i] for i in range(3)), ds, run, across)
            groups.setdefault(key, []).append((c, run, plane, ds, (tx, ty)))
    quads = []
    for key, members in groups.items():
        members.sort(key=lambda m: m[0])
        # split a group into stretches of consecutive cells along the run axis
        stretches = [[members[0]]]
        for m in members[1:]:
            run = m[1]
            if run is not None and m[0][run] == stretches[-1][-1][0][run] + 1:
                stretches[-1].append(m)
            else:
                stretches.append([m])
        for st in stretches:
            first, last = st[0], st[-1]
            c, run, plane, ds, texel = first
            d = key[0]
            a = AXIS[d]
            lo, hi = [0.0] * 3, [0.0] * 3
            lo_f, hi_f = g.cell_box(c)
            lo_l, hi_l = g.cell_box(last[0])
            for i in range(3):
                lo[i], hi[i] = lo_f[i], hi_f[i]
            if run is not None:
                lo[run], hi[run] = lo_f[run], hi_l[run]
            lo[a] = hi[a] = plane
            axes = [(a + 1) % 3, (a + 2) % 3]
            quads.append(make_quad(d, lo, hi, sliver_tex(g.faces[ds], texel, axes, run, lo, hi)))
    return quads


def relief_name(troop, key):
    return '%s_%s_r.png' % (troop, 'veteran' if key == 'veteran' else 's' + key)


def size_words(size, density):
    """A troop's size as the messages say it: 64x64, or 64x64 at 3 texels a px (192x192) for a density above 1."""
    if density == TROOP_DENSITY:
        return '%dx%d' % (size[0], size[1])
    return '%dx%d at %d texels a px (%dx%d)' % (size[0], size[1], density, size[0] * density, size[1] * density)


def side_image(cat, troop, file, size, density=1):
    """The glow or outer file of a troop, which its relief must not cover, checked for the troop's size (at its density)."""
    im = read_png(cat.root / 'art/troops' / file, troop)
    if list(im.size) != [size[0] * density, size[1] * density]:
        fail('%s: %s is %dx%d, not the troop size %s' % (troop, file, im.width, im.height, size_words(size, density)))
    return im.load()


def relief_mesh(cat: Catalog, model: dict, relief: Image.Image, troop: str, key: str) -> dict:
    """The relief quads of one skin of a troop by bone name (design 3), every rule of the design checked: the relief is the
    troop's size with alpha 0 or 255 only; no raised texel has a team hue, lies on a lit glow texel or under an opaque
    outer texel, lies on no skin box face or on a box with no depth; at most RELIEF_SHARE of a face is raised; and no
    voxel overlaps another box at rest (a box of a part that can hide excepted)."""
    what = '%s: %s' % (troop, relief_name(troop, key))
    size = model['size']
    density = model.get('density', TROOP_DENSITY)
    if list(relief.size) != [size[0] * density, size[1] * density]:
        fail('%s is %dx%d, not the troop size %s' % (what, relief.width, relief.height, size_words(size, density)))
    data = relief.load()
    raised = []
    for y in range(relief.height):
        for x in range(relief.width):
            a = data[x, y][3]
            if a not in (0, 255):
                fail('%s pixel (%d, %d) has alpha %d: a relief texel is opaque or empty' % (what, x, y, a))
            if a:
                raised.append((x, y))
    tint(relief, what, cat.factions)
    own, extra = troop_files(cat, troop)
    glow = side_image(cat, troop, extra['glow'], size, density) if extra['glow'] else None
    outer = side_image(cat, troop, extra['outer'], size, density) if extra['outer'] else None
    owner = {}
    for b in model['bones']:
        if b['team']:
            continue
        for i, box in enumerate(b['boxes']):
            for d, (x0, y0, x1, y1) in face_rects(box, bone_density(model, b)).items():
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        owner[(x, y)] = (b, i, d)
    grids, count = {}, {}
    for x, y in raised:
        if glow is not None and glow[x, y][3]:
            fail('%s pixel (%d, %d) sits on a lit glow texel: the voxel would hide it' % (what, x, y))
        if outer is not None and outer[x, y][3]:
            fail('%s pixel (%d, %d) is under an opaque texel of the outer layer' % (what, x, y))
        hit = owner.get((x, y))
        if hit is None:
            fail('%s pixel (%d, %d) lies on no skin box face' % (what, x, y))
        b, i, d = hit
        if b.get('variants'):
            fail('%s pixel (%d, %d) lies on the face bone %s: faces are flat paint (the beard volume of a beard block is part of '
                 'the face too), and relief belongs on the helm or the hair' % (what, x, y, b['name']))
        if 0 in b['boxes'][i]['size']:
            fail('%s pixel (%d, %d) lies on box %d of bone %s, which has no depth' % (what, x, y, i, b['name']))
        g = grids.setdefault((b['name'], i), Grid(b['boxes'][i], density))
        g.voxels[g.cell_of(g.faces[d], x, y)] = (d, x, y)
        count[(b['name'], i, d)] = count.get((b['name'], i, d), 0) + 1
    for (name, i, d), n in sorted(count.items()):
        x0, y0, x1, y1 = grids[(name, i)].faces[d].rect
        if n > RELIEF_SHARE * (x1 - x0) * (y1 - y0):
            fail('%s: %d of the %d texels of the %s face of box %d of bone %s are raised, more than %d%% of the face'
                 % (what, n, (x1 - x0) * (y1 - y0), DIRECTION_NAMES[d], i, name, round(RELIEF_SHARE * 100)))
    boxes = Boxes(model)
    out = {}
    for b in model['bones']:
        quads = []
        for i in range(len(b['boxes'])):
            g = grids.get((b['name'], i))
            if g is None:
                continue
            g.fill()
            for c in g.cells():
                over = boxes.hit(b['name'], i, *g.cell_box(c))
                if over:
                    _, tx, ty = g.anchor(c)
                    fail('%s pixel (%d, %d) makes a voxel that overlaps box %d of bone %s at rest'
                         % (what, tx, ty, over[1], over[0]))
            quads += top_quads(g) + side_quads(g)
        if quads:
            out[b['name']] = quads
    return out


def composite(skin: Image.Image, relief: Image.Image) -> Image.Image:
    """The shipped stage skin: the source skin with every opaque relief texel pasted over it."""
    out = skin.convert('RGBA').copy()
    px, rp = out.load(), relief.convert('RGBA').load()
    for y in range(out.height):
        for x in range(out.width):
            if rp[x, y][3] == 255:
                px[x, y] = rp[x, y]
    return out


TRIM_SHARE = 0.20   # the veteran trim covers at most this share of the stage 1 skin's opaque texels (gilt kept rare)


def trim_check(trim, skins, troop, name):
    """The veteran trim rules: alpha 0 or 255, every opaque texel on a texel every own skin paints (so it lies on the armour
    whichever tier is worn), and at most TRIM_SHARE of the stage 1 skin's opaque texels."""
    tp = trim.load()
    w, h = trim.size
    sp = [s.load() for s in skins]
    n = 0
    for y in range(h):
        for x in range(w):
            a = tp[x, y][3]
            if a not in (0, 255):
                fail('%s: %s: texel (%d, %d) has alpha %d; a trim is alpha 0 or 255' % (troop, name, x, y, a))
            if a == 0:
                continue
            n += 1
            for s in sp:
                if s[x, y][3] == 0:
                    fail('%s: %s: texel (%d, %d) lies on a texel a skin leaves clear' % (troop, name, x, y))
    base = sum(1 for y in range(h) for x in range(w) if sp[0][x, y][3] > 0)
    if n == 0:
        fail('%s: %s is empty' % (troop, name))
    if n > TRIM_SHARE * base:
        fail('%s: %s covers %d texels, over %d%% of the %d the stage 1 skin paints' % (troop, name, n, TRIM_SHARE * 100, base))


def trim_under_relief(trim, reliefs):
    """{key: [(x, y)]} of the trim's opaque texels that lie under an opaque texel of each relief layer (key: the skin's stage).
    Within TroopDepth.near a relief voxel stands one texel proud and covers the trim on the flat face there, so up close those
    trim texels do not show (Task 12 review minor 3). Keys with none are left out."""
    tp = trim.load()
    w, h = trim.size
    out = {}
    for key in sorted(reliefs):
        rp = reliefs[key].load()
        under = [(x, y) for y in range(h) for x in range(w) if tp[x, y][3] and rp[x, y][3]]
        if under:
            out[key] = under
    return out


def relief_sources(cat, troop, own, extra):
    """{key: file} of the troop's relief files: none, or one for every own skin (the stages' keys). The veteran trim is an
    overlay and takes none."""
    folder = cat.root / 'art/troops'
    want = {str(s): relief_name(troop, str(s)) for s in sorted(own)}
    if (folder / relief_name(troop, 'veteran')).is_file():
        fail('%s: %s: the veteran trim is an overlay drawn over the worn tier and takes no relief layer'
             % (troop, relief_name(troop, 'veteran')))
    stray = re.compile(r'%s_(s\d+|veteran)_r\.png' % re.escape(troop))
    for path in sorted(folder.glob('*_r.png')):
        if stray.fullmatch(path.name) and path.name not in want.values():
            fail('%s: %s belongs to no own skin of the troop' % (troop, path.name))
    present = {k: f for k, f in want.items() if (folder / f).is_file()}
    if present and len(present) != len(want):
        fail('%s: %s is missing: once one skin has a relief layer, every own skin needs one'
             % (troop, next(f for k, f in want.items() if k not in present)))
    return present


def relief_template(cat: Catalog, troop: str) -> Image.Image:
    """Where relief may stand (design 7): every skin box face in its bone's hue at RELIEF_TINT over white, and a texel whose
    voxel would overlap another box at rest (a joint, an inner face, a face under a team part or a part that never hides)
    or lies on a box with no depth hatched in RELIEF_RED. Joint faces that only touch their parent at rest (a limb's top and
    bottom faces) overlap nothing, so they are NOT hatched: keeping relief off them is still a by-eye rule (design 7). The sheet
    is the layer of a density D troop (D times the layout), a texel judged on its own voxel, and the hatch runs by layout px
    (so it is the density 1 hatch enlarged)."""
    model = bake(cat, troop)
    boxes = Boxes(model)
    density = model.get('density', TROOP_DENSITY)
    im = Image.new('RGBA', (model['size'][0] * density, model['size'][1] * density), (0, 0, 0, 0))
    px = im.load()
    names = {'top': 0, 'bottom': 1, 'north': 2, 'south': 3, 'west': 4, 'east': 5}
    for n, b in enumerate(x for x in model['bones'] if not x['team'] and x['boxes']):
        base = HUES[n % len(HUES)]
        for i, box in enumerate(b['boxes']):
            flat = 0 in box['size']
            g = None if flat else Grid(box, bone_density(model, b))
            for name, (x0, y0, x1, y1) in faces(box['uv'], box['size'], bone_density(model, b), box.get('faceuv')).items():
                d = names[name] ^ (1 if box['mirror'] and name in ('west', 'east') else 0)
                shaded = [max(0, min(255, int(c * FACE_SHADE[name]))) for c in base]
                colour = tuple(int(c * RELIEF_TINT + 255 * (1 - RELIEF_TINT)) for c in shaded) + (255,)
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        no = flat or bool(b.get('variants')) or boxes.hit(b['name'], i, *g.cell_box(g.cell_of(g.faces[d], x, y)))
                        px[x, y] = RELIEF_RED + (255,) if no and (x // density + y // density) % 3 == 0 else colour
    return im


# ---- the turnaround sheets (--turn) ----

TURN_SHOT = re.compile(r'p2-turn-(?P<troop>[a-z0-9_]+)-(?P<pose>[a-z0-9_]+)-(?P<angle>\d+)\.png')
TURN_ANGLES = (0, 45, 90, 135, 180, 225, 270, 315)
TURN_POSES = ('idle', 'draw', 'loose', 'attack', 'walk25', 'flat', 'bare')   # the order the suite takes them in
TURN_NAMES = {'idle': 'idle (rest)', 'draw': 'full draw', 'loose': 'loose', 'attack': 'attack peak', 'walk25': 'walk 1/4',
              'flat': 'flat (relief off)', 'bare': 'NO WEAPON'}
TURN_CROP = (430, 20, 850, 650)   # a 2:3 window round the troop of a 1280x720 shot, feet near the lower edge
TURN_CELL = (120, 180)            # a cell's picture; the sheet is 8 of them, 960 px, and a label strip below each
TURN_LABEL = 26
TURN_BG, TURN_GAP = (24, 22, 28), (72, 72, 80)


def turn_font(name, size):
    from PIL import ImageFont
    try:
        return ImageFont.truetype('C:/Windows/Fonts/%s.ttf' % name, size)
    except OSError:
        return ImageFont.load_default()


def turn_cell(path, line1, line2):
    from PIL import ImageDraw
    cw, ch = TURN_CELL
    cell = Image.new('RGB', (cw, ch + TURN_LABEL), TURN_BG)
    draw = ImageDraw.Draw(cell)
    if path is None:
        cell.paste(Image.new('RGB', TURN_CELL, TURN_GAP), (0, 0))
        draw.text((4, 4), 'no shot', fill=(210, 150, 150), font=turn_font('consola', 11))
    else:
        with Image.open(path) as shot:
            cell.paste(shot.convert('RGB').crop(TURN_CROP).resize(TURN_CELL, Image.LANCZOS), (0, 0))
    draw.text((3, ch + 1), line1, fill=(235, 235, 235), font=turn_font('consolab', 11))
    draw.text((3, ch + 13), line2, fill=(200, 190, 140), font=turn_font('consola', 11))
    return cell


def turn_sheet(rows, path):
    from PIL import ImageDraw
    cw, ch = TURN_CELL[0], TURN_CELL[1] + TURN_LABEL
    sheet = Image.new('RGB', (len(TURN_ANGLES) * cw, len(rows) * ch), TURN_BG)
    draw = ImageDraw.Draw(sheet)
    for r, row in enumerate(rows):
        for c, cell in enumerate(row):
            sheet.paste(cell, (c * cw, r * ch))
            draw.rectangle([c * cw, r * ch, (c + 1) * cw - 1, (r + 1) * ch - 1], outline=(60, 58, 70))
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    print('build_skins: %s %dx%d' % (path.name, sheet.width, sheet.height))
    return path


def turn_sheets(shots: Path, out: Path) -> list:
    """The turnaround sheets of the suite turn's shots p2-turn-<troop>-<pose>-<angle>.png: turn-<troop>.png for each
    troop (a row a pose in the order of TURN_POSES, a column an angle 0 to 315, a missing shot a grey cell) and turn-all.png
    (a row a troop, its idle at the eight angles). Every cell is labelled with the troop, the pose and the angle."""
    shots, out = Path(shots), Path(out)
    found = {}
    for path in sorted(shots.glob('p2-turn-*.png')):
        m = TURN_SHOT.fullmatch(path.name)
        if m:
            found.setdefault(m['troop'], {}).setdefault(m['pose'], {})[int(m['angle'])] = path
    if not found:
        fail('%s holds no p2-turn-<troop>-<pose>-<angle>.png shot' % shots)
    made = []
    for troop in sorted(found):
        poses = sorted(found[troop], key=lambda p: (TURN_POSES.index(p) if p in TURN_POSES else len(TURN_POSES), p))
        rows = [[turn_cell(found[troop][pose].get(a), '%s %s' % (troop, TURN_NAMES.get(pose, pose)), 'angle %d' % a)
                 for a in TURN_ANGLES] for pose in poses]
        made.append(turn_sheet(rows, out / ('turn-%s.png' % troop)))
    rows = [[turn_cell(found[t].get('idle', {}).get(a), '%s idle' % t, 'angle %d' % a) for a in TURN_ANGLES] for t in sorted(found)]
    made.append(turn_sheet(rows, out / 'turn-all.png'))
    return made


# ---- the tell contact sheet (--contact) ----
# The grading rubric's layout rules apply to it: a House named at the head of each row, the troop's name above its picture (never flush against
# the picture of the row above), nothing within 12 px of the edge or touching its neighbour, and text at 4.5:1 or better on its ground.

CONTACT_CELL = 200                    # a picture, a square, px
CONTACT_LABEL = 22                    # the troop's name above its picture
CONTACT_HEADER = 30                   # a House's name band at the head of its row
CONTACT_MARGIN = 16                   # the sheet's edge to the first band or picture
CONTACT_GAP = 12                      # between the pictures of a row
CONTACT_ROW_GAP = 18                  # between a row's pictures and the next House's header
CONTACT_BAND = CONTACT_HEADER + CONTACT_LABEL + CONTACT_CELL   # a row: header, labels, pictures
CONTACT_CROP = 600                    # the square cut from the centre of a 1280x720 shot: the troop with its overhead gesture, and no more sky and grass than that
CONTACT_GREY = (46, 44, 54)           # a missing shot's picture: quieter than a shot, so the shots carry the sheet
CONTACT_NO_SHOT = (200, 200, 208)     # its "no shot" text: about 8:1 on the grey
CONTACT_TEXT = (235, 235, 235)        # the House and troop names on the sheet's ground (TURN_BG): about 15:1


def contrast(a: tuple, b: tuple) -> float:
    """The WCAG contrast ratio of two RGB colours."""
    def luminance(c):
        lin = [v / 255 / 12.92 if v / 255 <= 0.04045 else ((v / 255 + 0.055) / 1.055) ** 2.4 for v in c[:3]]
        return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]
    hi, lo = sorted((luminance(a), luminance(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


def contact_size(cols: int, rows: int) -> tuple:
    """The sheet's width and height."""
    return (2 * CONTACT_MARGIN + cols * CONTACT_CELL + (cols - 1) * CONTACT_GAP,
            2 * CONTACT_MARGIN + rows * CONTACT_BAND + (rows - 1) * CONTACT_ROW_GAP)


def contact_header_box(row: int, cols: int) -> tuple:
    """(left, top, right, bottom) of a row's House band: the whole width inside the margins."""
    top = CONTACT_MARGIN + row * (CONTACT_BAND + CONTACT_ROW_GAP)
    return (CONTACT_MARGIN, top, contact_size(cols, 1)[0] - CONTACT_MARGIN, top + CONTACT_HEADER)


def contact_label_box(col: int, row: int) -> tuple:
    """(left, top, right, bottom) of the strip above a picture, where the troop's name is."""
    left = CONTACT_MARGIN + col * (CONTACT_CELL + CONTACT_GAP)
    top = CONTACT_MARGIN + row * (CONTACT_BAND + CONTACT_ROW_GAP) + CONTACT_HEADER
    return (left, top, left + CONTACT_CELL, top + CONTACT_LABEL)


def contact_picture_box(col: int, row: int) -> tuple:
    """(left, top, right, bottom) of a picture, below its label."""
    left, top, right, _ = contact_label_box(col, row)
    return (left, top + CONTACT_LABEL, right, top + CONTACT_LABEL + CONTACT_CELL)


def contact_houses() -> list:
    """The sheet's rows, the Houses in the houses order: {'name', 'accent' (RGB), 'troops' (the row's troops in order)}."""
    return [{'name': h['name'], 'accent': ((h['accent'] >> 16) & 255, (h['accent'] >> 8) & 255, h['accent'] & 255),
             'troops': [t.strip() for t in h['troops'].split(',')]}
            for h in read_json(ROOT / 'sheets/design.json').get('houses', [])]


def contact_grid() -> list:
    """The troops of the sheet's houses: a row a House in the houses order, a troop a column in the row's troops order."""
    return [h['troops'] for h in contact_houses()]


def contact_picture(path):
    """One troop's picture: its shot cropped to the CONTACT_CROP square at the centre, or grey with a centred "no shot" when there is none."""
    from PIL import ImageDraw
    size = CONTACT_CELL
    if path is None:
        pic = Image.new('RGB', (size, size), CONTACT_GREY)
        draw, font = ImageDraw.Draw(pic), turn_font('consola', 13)
        box = draw.textbbox((0, 0), 'no shot', font=font)
        draw.text(((size - (box[2] - box[0])) // 2 - box[0], (size - (box[3] - box[1])) // 2 - box[1]), 'no shot', fill=CONTACT_NO_SHOT, font=font)
        return pic
    with Image.open(path) as shot:
        shot = shot.convert('RGB')
        side = min(CONTACT_CROP, *shot.size)
        left, top = (shot.width - side) // 2, (shot.height - side) // 2
        return shot.crop((left, top, left + side, top + side)).resize((size, size), Image.LANCZOS)


def contact(shots: Path, out: Path) -> None:
    """The tell contact sheet of the suite tells' shots p2-tells-<troop>.png: the Houses are the rows, in the houses order, each headed by
    its name (the House's accent a small swatch beside it); the troops are the columns, in each row's troops order, a picture the troop's
    shot cropped to the CONTACT_CROP square at the centre with the troop's name above it, and a missing shot leaves a quiet grey picture
    saying "no shot". The pictures are all the same size, a gap apart, and the margins are 16 px (contact_size, contact_header_box,
    contact_label_box and contact_picture_box say where everything is)."""
    from PIL import ImageDraw
    shots, out = Path(shots), Path(out)
    houses = contact_houses()
    if not houses:
        fail('sheets/design.json has no houses row')
    cols = max(len(h['troops']) for h in houses)
    sheet = Image.new('RGB', contact_size(cols, len(houses)), TURN_BG)
    draw = ImageDraw.Draw(sheet)
    head, name = turn_font('consolab', 16), turn_font('consolab', 13)
    ref_head, ref_name = draw.textbbox((0, 0), 'Hgy', font=head), draw.textbbox((0, 0), 'Hgy', font=name)   # one height for every name, so the baselines agree
    found = 0
    for r, house in enumerate(houses):
        left, top, _, bottom = contact_header_box(r, cols)
        draw.rectangle([left, top + 5, left + 4, bottom - 6], fill=house['accent'])
        draw.text((left + 14, top + (CONTACT_HEADER - (ref_head[3] - ref_head[1])) // 2 - ref_head[1]), house['name'], fill=CONTACT_TEXT, font=head)
        for c, troop in enumerate(house['troops']):
            path = shots / ('p2-tells-%s.png' % troop)
            found += path.is_file()
            lx, ly, _, _ = contact_label_box(c, r)
            draw.text((lx, ly + (CONTACT_LABEL - (ref_name[3] - ref_name[1])) // 2 - ref_name[1]), troop, fill=CONTACT_TEXT, font=name)
            sheet.paste(contact_picture(path if path.is_file() else None), contact_picture_box(c, r)[:2])
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print('build_skins: %s %dx%d, %d of %d shots' % (out.name, sheet.width, sheet.height, found, sum(len(h['troops']) for h in houses)))


def parse_faces(src, troop, size):
    """The "faces" block of a troop source checked for its shape: {'count', 'slots', 'card', 'fixed', 'beard', 'density'} ('fixed' is
    None for none, 'beard' is None or {'slots'}, 'density' None for the troop's own), or None for a troop without one. A block of count 1 has no slots (it is the canonical face
    alone). Where the slots lie on the sheet is split_faces' check."""
    if 'faces' not in src:
        return None
    block = src['faces']
    what = troop + ' faces'
    if not isinstance(block, dict):
        fail('%s must be an object {count, slots, ...}, not %s' % (what, json.dumps(block)))
    keys(block, FACES_KEYS, what)
    fdensity = density_of(block, what + ' (the density of the patch files)', None) if 'density' in block else None
    count = block.get('count')
    if isinstance(count, bool) or not isinstance(count, int) or not FACES_MIN <= count <= FACES_MAX:
        fail('%s: count must be a whole number from %d to %d (the canonical face counts), not %s'
             % (what, FACES_MIN, FACES_MAX, json.dumps(count)))

    def slot_list(value, where):
        if count == 1:
            if value not in (None, []):
                fail('%s: count 1 is the canonical face alone and takes no slots, not %s' % (where, json.dumps(value)))
            return []
        if not isinstance(value, list) or len(value) != count - 1:
            fail('%s: slots must list %d slots [x, y] (patches 1 to %d: the top left of each, in px of the sheet), not %s'
                 % (where, count - 1, count - 1, json.dumps(value)))
        return [vec(slot, 2, '%s slot %d' % (where, n), ints=True, minimum=0) for n, slot in enumerate(value, 1)]
    slots = slot_list(block.get('slots'), what)
    beard = None
    if 'beard' in block:
        if not isinstance(block['beard'], dict):
            fail('%s beard must be an object {slots: [[x, y] x count-1]}, not %s' % (what, json.dumps(block['beard'])))
        keys(block['beard'], BEARD_KEYS, what + ' beard')
        beard = {'slots': slot_list(block['beard'].get('slots'), what + ' beard')}
    out = {}
    for key, default in (('fixed', None), ('card', None)):
        value = block.get(key, default)
        if key == 'card' and value is None:
            value = out['fixed'] if out['fixed'] is not None else 0
        if value is not None and (isinstance(value, bool) or not isinstance(value, int) or not 0 <= value < count):
            fail('%s: %s must be a face from 0 to %d, not %s' % (what, key, count - 1, json.dumps(value)))
        out[key] = value
    return {'count': count, 'slots': slots, 'card': out['card'], 'fixed': out['fixed'], 'beard': beard, 'density': fdensity}


def face_density(faces, density, troop):
    """(df, file): df the texels a px the face bones are drawn at on the sheet, file the density the patch files must be drawn at
    (None: any whole multiple of the troop's, as before the block had a density). A block's density above the troop's is reduced to it
    (it must be a whole multiple); below it the faces are drawn at their own density, which needs face bones, so not count 1."""
    f = faces['density']
    if f is None:
        return density, None
    if f > density:
        if f % density:
            fail('%s: faces density %d is above the troop\'s density %d and not a whole multiple of it (the patches are reduced by the quotient)'
                 % (troop, f, density))
        return density, f
    if f < density and faces['count'] == 1:
        fail('%s: faces count 1 has no face bone to draw at density %d, below the troop\'s %d: its patch is pasted over the head, '
             'so its density is the troop\'s or a multiple of it' % (troop, f, density))
    return f, f


def reserve(box, density, df):
    """(width, height) in layout px of the sheet a face box's patch needs: the box's rectangle in px, its texels at the face density
    rounded up to whole layout px of the troop's density (a 64x32 texel head patch is 22x11 px on a 3x sheet)."""
    w, h = box_rect(box)
    return -(-w * df // density), -(-h * df // density)


def rects_overlap(a, b):
    return a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]


def box_rect(box):
    """(width, height) in px of the rectangle a box's six faces unfold in on the sheet: 32x16 for the 8x8x8 head, 14x5 for a 6x4x1 beard."""
    sx, sy, sz = box['size']
    return 2 * (sx + sz), sz + sy


def split_faces(bones, faces, troop, size, density=TROOP_DENSITY, df=None):
    """The bones of a faces troop: the first box of `head` (the rig's head box, which keeps its place and its scale) moves into
    a child face_v0, and face_v1.. are the same box with its uv at slot 1.., each bone "variants": [k]; they follow `head` and `head`
    stays the pivot the clips, the helm bones and helm_off hang from. With a beard block the second box of `head` (the beard volume)
    goes with the first, its uv at beard slot k in face_vk; without one any other box of `head` stays on it and shows in every
    variant. Every slot is a free rectangle of its box's size (32x16 px for 8x8x8): inside the sheet, clear of the boxes it is
    cut from, of the slots before it and of every other skin box. A count of 1 splits nothing (the head is the canonical face).
    With faces drawn below the troop's density (df < density) a slot is the rectangle its texels need (`reserve`), the sheet and every
    texel origin must be a whole number of face px, and the face bones are marked "face_density" for clean_bone."""
    df = density if df is None else df
    at = next((i for i, b in enumerate(bones) if b['name'] == 'head'), None)
    if at is None or not bones[at]['boxes']:
        fail('%s: faces need a head bone with a box to split' % troop)
    if any(b['name'].startswith('face_v') for b in bones):
        fail('%s: a bone is already called face_v..., the name of a face bone' % troop)
    head = bones[at]
    wide = 2 if faces['beard'] is not None else 1   # the boxes of `head` the face bones take
    if len(head['boxes']) < wide:
        fail('%s: faces beard wants the beard volume as the second box of head, and head has %d' % (troop, len(head['boxes'])))
    mine = head['boxes'][:wide]
    width, height = size
    taken = [('the %s (box at uv (%d, %d))' % (('head', 'beard')[n], box['uv'][0], box['uv'][1]),
              (box['uv'][0], box['uv'][1], box['uv'][0] + box_rect(box)[0], box['uv'][1] + box_rect(box)[1])) for n, box in enumerate(mine)]
    others = [('the %s box at uv (%d, %d)' % (b['name'], box['uv'][0], box['uv'][1]), rect)
              for b in bones if not b['team'] for i, box in enumerate(b['boxes']) if not (b is head and i < wide)
              for rect in footprint(box['uv'], box['size'], box.get('faceuv'))]
    slot_lists = (faces['slots'], faces['beard']['slots'] if wide == 2 else [])
    if faces['count'] > 1 and df != density:
        if (width * density) % df or (height * density) % df:
            fail('%s: the %dx%d sheet at %d texels a px is %dx%d texels, not a multiple of %d: the face bones (density %d) need a sheet of a '
                 'whole number of face px' % (troop, width, height, density, width * density, height * density, df, df))
        for n, box in enumerate(mine):
            if (box['uv'][0] * density) % df or (box['uv'][1] * density) % df:
                fail('%s: the %s box at uv (%d, %d) starts at texel (%d, %d), not a multiple of %d, the face density'
                     % (troop, ('head', 'beard')[n], box['uv'][0], box['uv'][1], box['uv'][0] * density, box['uv'][1] * density, df))
    if faces['count'] > 1:
        for n, label in enumerate(('slot', 'beard slot')):
            fw, fh = reserve(mine[n], density, df) if n < wide else (0, 0)
            for k, (x, y) in enumerate(slot_lists[n], 1):
                r = (x, y, x + fw, y + fh)
                if df != density and ((x * density) % df or (y * density) % df):
                    fail('%s: faces %s %d at (%d, %d) starts at texel (%d, %d), not a multiple of %d, the face density (use a multiple of %d '
                         'for each coordinate)' % (troop, label, k, x, y, x * density, y * density, df, df // math.gcd(df, density)))
                if r[2] > width or r[3] > height:
                    fail('%s: faces %s %d at (%d, %d) leaves the %dx%d sheet (a %s is %dx%d px)' % (troop, label, k, x, y, width, height, label, fw, fh))
                for other, rect in taken + others:
                    if rects_overlap(r, rect):
                        fail('%s: faces %s %d at (%d, %d) overlaps %s' % (troop, label, k, x, y, other))
                taken.append(('%s %d at (%d, %d)' % (label, k, x, y), r))
    if faces['count'] == 1:
        return bones
    kids = []
    for k in range(faces['count']):
        boxes = []
        for n, box0 in enumerate(mine):
            box = {key: list(v) if isinstance(v, list) else v for key, v in box0.items()}
            if k:
                box['uv'] = list(slot_lists[n][k - 1])
            boxes.append(box)
        kid = {'name': 'face_v%d' % k, 'parent': 'head', 'part': '', 'team': False, 'tag': '', 'stages': [], 'variants': [k],
               'pivot': [0.0, 0.0, 0.0], 'rotation': [0.0, 0.0, 0.0], 'boxes': boxes}
        if df != density:
            kid['face_density'] = df
        kids.append(kid)
    head['boxes'] = head['boxes'][wide:]
    return bones[:at + 1] + kids + bones[at + 1:]


def bake(cat: Catalog, troop: str) -> dict:
    """The generated model of one troop (models/troop/<troop>.json), every rule checked."""
    src = cat.troops.get(troop)
    if src is None:
        fail('%s: no art/troops/%s.json' % (troop, troop))
    keys(src, TROOP_KEYS, troop)
    row = cat.sheet.get(troop)
    if row is None:
        fail('%s: no troops row in sheets/design.json has this id' % troop)
    rig_name = src.get('rig')
    rig = cat.rigs.get(rig_name)
    if rig is None:
        fail('%s: unknown rig %s' % (troop, rig_name))
    want = RIGS.get(row['entity'])
    if rig_name != want:
        fail('%s: rig %s is not the %s rig (%s)' % (troop, rig_name, row['entity'], want))
    size = vec(src.get('size', rig['size']), 2, troop + ' size', ints=True, minimum=1)
    density = density_of(src, troop, TROOP_DENSITY)
    faces = parse_faces(src, troop, size)
    for suffix, wanted, block in ((FACES_SUFFIX, faces is not None, 'faces block'), (BEARDS_SUFFIX, faces is not None and faces['beard'] is not None, 'faces beard block')):
        if not wanted and (cat.root / 'art/troops' / (troop + suffix + '.png')).is_file():
            fail('%s: art/troops/%s%s.png has no %s in %s.json to use it' % (troop, troop, suffix, block, troop))
    idle = src.get('idleArms', 'hang')
    if idle not in IDLE_ARMS:
        fail('%s: idleArms %s is not one of %s' % (troop, idle, ', '.join(IDLE_ARMS)))
    hunch = src.get('hunch', 0)
    if not is_num(hunch) or (hunch and rig_name != 'biped'):
        fail('%s: hunch is a number of degrees, and only a biped hunches' % troop)
    parts = []
    entries = src.get('parts', [])
    if not isinstance(entries, list):
        fail(troop + ': parts must be a list')
    for e in entries:
        keys(e, ENTRY_KEYS, troop + ' part entry')
        p = cat.parts.get(e.get('part'))
        if p is None:
            fail('%s: unknown part %s' % (troop, e.get('part')))
        parts.append((e, p))
    stages = cat.stages[troop]
    # A troop with a .bbmodel (art pipeline v2 section 2.2) takes its skin geometry from the bone groups; the json keeps
    # each bone's scale, and the parts are still the library's. The export writes the pre-scale rig, so the build's bake
    # is unchanged and the output must be byte-identical to the json-only build's (--to-bbmodel's gate).
    model_source = None
    project = cat.root / 'art/troops' / (troop + '.bbmodel')
    if project.is_file():
        try:
            model_source = bbmodel.troop_bones(bbmodel.read(project), rig, parts)
        except bbmodel.BBError as e:
            fail('%s: %s' % (troop, e))
        folded_in_project = bool(model_source['arms_folded']['boxes'])
        if (idle == 'folded') != folded_in_project:
            fail('%s: art/troops/%s.bbmodel has %s arms_folded boxes but idleArms is %s: re-export it with --to-bbmodel'
                 % (troop, troop, 'folded' if folded_in_project else 'no', idle))
    bones = bake_bones(rig, troop, src.get('bones'), parts, float(hunch), idle == 'folded', stages, source=model_source)
    if faces and faces['count'] > 1 and troop_files(cat, troop)[1]['outer']:
        fail('%s: faces and an outer layer together: the outer twin of the head would lose its box to the face bones' % troop)
    df = face_density(faces, density, troop)[0] if faces else density
    if faces:
        bones = split_faces(bones, faces, troop, size, density, df)
    if len(bones) > MAX_BONES:
        fail('%s: %d bones, more than %d (the client records the bones a draw shows as the bits of one long and '
             'refuses a bigger model)' % (troop, len(bones), MAX_BONES))
    skin_boxes = [(b['name'], x['uv'], x['size'], x.get('faceuv')) for b in bones if not b['team'] and not (b.get('face_density') and b['variants'][0])
                  for x in b['boxes']]   # a slot of a dense face bone is not a full rectangle: split_faces checked it at its own size
    check_layout(skin_boxes, size, troop)
    team_boxes = [(b['part'], x['uv'], x['size']) for b in bones if b['team'] for x in b['boxes']]
    if team_boxes:
        check_layout(sorted(set((o, tuple(uv), tuple(s)) for o, uv, s in team_boxes), key=str), cat.cloth_layout,
                     troop + ' on the cloth sheet')
    weapon = src.get('weapon')
    if weapon != 'none' and weapon not in cat.weapons:
        fail('%s: unknown weapon %s' % (troop, weapon))
    tiered = WEAPON_TIER.fullmatch(weapon or '')
    if tiered and tiered.group(1) in cat.weapons:
        fail('%s: weapon %s is a weapon tier; name its tier I weapon %s (the client picks the tier the soldier wears)'
             % (troop, weapon, tiered.group(1)))
    hand = src.get('hand')
    if hand not in ('right', 'left'):
        fail('%s: hand must be right or left' % troop)
    portrait = src.get('portrait')
    if not isinstance(portrait, str) or not PORTRAIT.fullmatch(portrait):
        fail('%s: portrait must be standard, tall N, wide N or beast N [yaw D], not %s' % (troop, portrait))
    glow_from = src.get('glowFrom', 1)
    if glow_from not in stages:
        fail('%s: glowFrom %s is not one of the stages %s' % (troop, glow_from, stages))
    own, extra = troop_files(cat, troop)
    # Only the troop's own skins: a tier without one draws the highest own tier below it on the client (the stopgap,
    # in-match progression design 4.4), and --tiers lists it.
    skins = {str(s): texture_id(own[s]) for s in sorted(own)}
    model = {'format': FORMAT, 'troop': troop, 'rig': rig_name, 'size': size,
             'bones': [clean_bone(b, cat.cloth_layout if b['team'] else size, density) for b in bones], 'skins': skins,
             'veteran': texture_id(extra['veteran']) if extra['veteran'] else '',
             'glow': texture_id(extra['glow']) if extra['glow'] else '', 'glowFrom': glow_from,
             'outer': texture_id(extra['outer']) if extra['outer'] else '',
             'weapon': weapon, 'hand': hand, 'idleArms': idle, 'portrait': portrait}
    varied = faces is not None and faces['count'] > 1   # a count of 1 has no variants to say
    order = model_keys(density, varied)
    info = dict({'count': faces['count'], 'card': faces['card']}, **({} if faces['fixed'] is None else {'fixed': faces['fixed']})) if varied else None
    model = {k: dict(model, density=density, faces=info)[k] for k in order}
    assert tuple(model) == order and not set(model) & set(VANILLA_KEYS)
    return model


def px_words(n):
    """An area in px for a message: whole when it is, else two decimals."""
    return str(int(n)) if n == int(n) else '%.2f' % n


def far_name(name):
    """The file name of the one-texel-a-px twin of a density D troop PNG: knight_s1.png -> knight_s1_far.png."""
    assert name.endswith('.png')
    return name[:-len('.png')] + FAR_SUFFIX + '.png'


def far_twin(im, density, islands=()):
    """The twin of a density D image at one texel a px (its size over D in each direction), made as the weapons' far version
    is: a block of D x D texels takes the colour most of its painted texels share (a tie goes to the colour met first in
    reading order) and is clear unless at least half of it is painted. The client binds it past TroopDepth.NEAR_BLOCKS and on
    the cards drawn under 48 px a block, where a 3x skin would crawl in the mip chain (texel-3 plan Q2a). Density 1 is the image.
    `islands` are texel rectangles (x0, y0, x1, y1) of faces drawn below the density: a block that straddles the edge of one is judged by
    the island's texels in it (its patch's last column of 64 texels is one third of a 3 texel block, and would be a hole otherwise)."""
    if density == TROOP_DENSITY:
        return im
    src = im.convert('RGBA')
    px = src.load()
    w, h = src.width // density, src.height // density
    out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    op = out.load()
    for y in range(h):
        for x in range(w):
            seen = {}
            for j in range(density):
                for i in range(density):
                    c = px[x * density + i, y * density + j]
                    if c[3]:
                        seen[c] = seen.get(c, 0) + 1
            area = density * density
            if islands:
                inside = 0
                for ix0, iy0, ix1, iy1 in islands:
                    across = min(x * density + density, ix1) - max(x * density, ix0)
                    down = min(y * density + density, iy1) - max(y * density, iy0)
                    if across > 0 and down > 0:
                        inside += across * down
                area = inside or area
            if 2 * sum(seen.values()) >= area:
                op[x, y] = max(seen.items(), key=lambda kv: kv[1])[0]
    return out


def face_targets(bones, faces):
    """What the patches of a faces troop are pasted over, one entry a face: {'k', 'head': the head box, 'beard': the beard box or None},
    from the bones of the baked model: each face bone's first (and with a beard block second) box, or for a count of 1 the head's."""
    wide = 2 if faces['beard'] is not None else 1
    if faces['count'] == 1:
        head = next(b for b in bones if b['name'] == 'head')
        return [{'k': 0, 'head': head['boxes'][0], 'beard': head['boxes'][1] if wide == 2 else None}]
    return [{'k': b['variants'][0], 'head': b['boxes'][0], 'beard': b['boxes'][1] if wide == 2 else None} for b in bones if b.get('variants')]


def read_patch_file(cat, troop, name, box, count, density, solid, df=None, file_density=None):
    """art/troops/<name> of a faces troop, checked, and reduced to the density the face bones are drawn at (df, the troop's unless the
    block says): patches of `box`'s rectangle stacked, at the block's density (file_density) or, without one, at the troop's density
    or a whole multiple of it, at least `count` of them (the first `count` are used, more are banked). Texels outside the
    box's six faces are clear. Inside them a `solid` patch (a head) is opaque, any other (a beard) is opaque or clear. The tint
    rule holds. A file drawn at more texels than df is reduced by the far twin's majority rule. Returns the `count` patches stacked at df."""
    df = density if df is None else df
    fw, fh = box_rect(box)
    path = cat.root / 'art/troops' / name
    if not path.is_file():
        fail('%s: %s is missing from art/troops (the faces block wants %d patches of %dx%d px stacked)' % (troop, name, count, fw, fh))
    im = read_png(path, troop)
    scale = im.width // fw if im.width % fw == 0 else 0
    patch = fh * scale
    if file_density is None:
        wrong = scale < density or scale % density or im.height % patch
        want = density
    else:
        wrong = scale != file_density or im.height % patch
        want = file_density
    if wrong:
        fail('%s: %s is %dx%d, not %dx%d (%d patches of %dx%d stacked%s%s)' % (
            troop, name, im.width, im.height, fw * want, fh * want * count, count, fw * want, fh * want,
            '' if want == TROOP_DENSITY else ', at %d texels a px' % want,
            '; or at a whole multiple of that density, with more patches banked after them' if file_density is None
            else '; more patches may be banked after them'))
    if im.height // patch < count:
        fail('%s: %s is %dx%d, not %dx%d: %d patches of %dx%d where the faces block wants %d' % (
            troop, name, im.width, im.height, im.width, patch * count, im.height // patch, im.width, patch, count))
    inside = footprint((0, 0), box['size'])
    px = im.load()
    for k in range(im.height // patch):
        for y in range(patch):
            for x in range(im.width):
                a = px[x, k * patch + y][3]
                on = any(x0 * scale <= x < x1 * scale and y0 * scale <= y < y1 * scale for x0, y0, x1, y1 in inside)
                where = '%s texel (%d, %d) (patch %d, texel (%d, %d))' % (name, x, k * patch + y, k, x, y)
                if on and a != 255 and (solid or a != 0):
                    fail('%s: %s has alpha %d: a texel of the %s is %s' % (troop, where, a, 'faces of the head' if solid else 'beard box',
                                                                          'opaque' if solid else 'opaque or clear'))
                if not on and a != 0:
                    fail('%s: %s is outside the faces of the %s, where nothing is painted' % (troop, where, 'head' if solid else 'beard box'))
    tint(im, '%s: %s' % (troop, name), cat.factions)
    return far_twin(im.crop((0, 0, im.width, patch * count)), scale // df)


def read_face_patches(cat, troop, faces, targets, density, df=None):
    """(the head patches, the beard patches or None) of a faces troop, at the density its face bones are drawn at, `count` of each stacked."""
    head = read_patch_file(cat, troop, troop + FACES_SUFFIX + '.png', targets[0]['head'], faces['count'], density, True, df, faces['density'])
    beard = None
    if faces['beard'] is not None:
        beard = read_patch_file(cat, troop, troop + BEARDS_SUFFIX + '.png', targets[0]['beard'], faces['count'], density, False, df, faces['density'])
    return head, beard


def face_slot_rects(targets, density=TROOP_DENSITY, df=None, overlay=False):
    """The texel rectangles (label, x0, y0, x1, y1) that belong to the faces. Of the skins nothing may paint the slots of variants
    1 and up (a slot's patch texels rounded up to whole layout px of the troop's density); variant 0's own rectangles (the head's and
    the beard's, at the troop's density) are the skin's, replaced by patch 0. An overlay (the veteran trim, the
    glow) is drawn over whichever face shows, so it may touch none of them: variant 0's rectangles count too, or a trim texel there would
    mark a quarter of the faces only (a count of 1 has one face and nothing to be uneven)."""
    df = density if df is None else df
    out = []
    for t in targets:
        for label, box in (('face slot', t['head']), ('beard slot', t['beard'])):
            if box is None or (t['k'] == 0 and not (overlay and len(targets) > 1)):
                continue
            x0, y0 = box['uv'][0] * df, box['uv'][1] * df
            (fw, fh), (rw, rh) = box_rect(box), reserve(box, density, df)
            if t['k'] == 0:
                x1, y1 = x0 + fw * density, y0 + fh * density
            else:
                x1, y1 = x0 + rw * density if df == density else -(-(x0 + fw * df) // density) * density, \
                    y0 + rh * density if df == density else -(-(y0 + fh * df) // density) * density
            name = ('the canonical ' + ('face' if box is t['head'] else 'beard')) if t['k'] == 0 else '%s %d' % (label, t['k'])
            out.append((name, x0, y0, x1, y1))
    return out


def check_face_slots(im, troop, name, verb, targets, density, overlay=False, df=None):
    """No texel of this sheet (a skin, the trim or the glow) may lie in a slot: the slots are free rows for the face patches. A trim or
    glow sheet (overlay) may not lie on variant 0's head or beard either."""
    px = im.load()
    for label, x0, y0, x1, y1 in face_slot_rects(targets, density, df, overlay):
        for y in range(y0, y1):
            for x in range(x0, x1):
                if px[x, y][3]:
                    fail('%s: %s %s texel (%d, %d), inside %s, which belongs to the faces' % (troop, name, verb, x, y, label))


def face_islands(targets, df):
    """The texel rectangles the patches are pasted in: (x0, y0, x1, y1) a head and a beard patch each, face by face."""
    out = []
    for t in targets:
        for box in (t['head'], t['beard']):
            if box is not None:
                (fw, fh), (u, v) = box_rect(box), box['uv']
                out.append((u * df, v * df, (u + fw) * df, (v + fh) * df))
    return out


def paste_faces(im, heads, beards, targets, density, df=None):
    """The skin with every face's patches pasted over the texels of the six faces of its boxes (a beard's clear texels too: the cut-out is
    the beard's silhouette): head patch 0 over the head's, patch k at slot k, and likewise beard patch k at the beard box's rectangle
    or at beard slot k. The patches are at the face density df (the troop's unless the faces block says), whose px is df texels from
    the box's texel origin (its uv times df). Where df is below the troop's density the old rectangle of variant 0, at the
    troop's density, is cleared first: what the old head painted outside the patch is unused. Nothing else of the skin changes."""
    df = density if df is None else df
    out = im.convert('RGBA').copy()
    px = out.load()
    for patches, key in ((heads, 'head'), (beards, 'beard')):
        if patches is None:
            continue
        src = patches.load()
        for t in targets:
            box = t[key]
            (u, v), (fw, fh), k = box['uv'], box_rect(box), t['k']
            if df != density and k == 0:
                for y in range(v * df, v * df + fh * density):
                    for x in range(u * df, u * df + fw * density):
                        px[x, y] = (0, 0, 0, 0)
            for x0, y0, x1, y1 in footprint((0, 0), box['size']):
                for y in range(y0 * df, y1 * df):
                    for x in range(x0 * df, x1 * df):
                        px[u * df + x, v * df + y] = src[x, k * fh * df + y]
    return out


def troop_outputs(cat, troop, weapon_lit):
    """The files of one troop: its model, with the relief meshes of its skins, and the skins, each the source skin with its
    relief layer pasted over it. Returns (files, [(troop, key, relief quads)])."""
    model = bake(cat, troop)
    own, extra = troop_files(cat, troop)
    folder = cat.root / 'art/troops'
    size = model['size']
    density = model.get('density', TROOP_DENSITY)
    out = {}
    reliefs = relief_sources(cat, troop, own, extra)
    key_of = {own[s]: str(s) for s in own}
    faces = parse_faces(cat.troops[troop], troop, size)
    df = face_density(faces, density, troop)[0] if faces else density
    targets = face_targets(model['bones'], faces) if faces else []
    heads, beards = read_face_patches(cat, troop, faces, targets, density, df) if faces else (None, None)
    islands = face_islands(targets, df) if df != density else ()
    by_bone, stats = {}, []
    for f in [own[s] for s in sorted(own)] + [extra['veteran'], extra['outer'], extra['glow']]:
        if not f:
            continue
        im = read_png(folder / f, troop)
        if list(im.size) != [size[0] * density, size[1] * density]:
            fail('%s: %s is %dx%d, not the troop size %s' % (troop, f, im.size[0], im.size[1], size_words(size, density)))
        if targets:
            check_face_slots(im, troop, f, 'lights' if f == extra['glow'] else 'covers' if f == extra['veteran'] else 'paints', targets, density,
                             overlay=f in (extra['glow'], extra['veteran']), df=df)
        if f != extra['glow']:
            tint(im, '%s: %s' % (troop, f), cat.factions)
        if f == extra['veteran']:
            trim_check(im, [read_png(folder / own[s], troop) for s in sorted(own)], troop, f)
            n = sum(1 for a in im.tobytes()[3::4] if a)
            for key, under in trim_under_relief(im, {k: read_png(folder / reliefs[k], troop) for k in reliefs}).items():
                print('build_skins: note: %s: %s: %d of %d trim texels lie under the s%s relief (hidden within TroopDepth.near)'
                      % (troop, f, len(under), n, key))
        key = key_of.get(f)
        if key is not None and targets:
            im = paste_faces(im, heads, beards, targets, density, df)
        if key in reliefs:
            relief = read_png(folder / reliefs[key], troop)
            meshes = relief_mesh(cat, model, relief, troop, key)
            total = sum(len(q) for q in meshes.values())
            if total > RELIEF_CAP:
                fail('%s: %s has %d relief quads, over the %d quads a stage may have' % (troop, reliefs[key], total, RELIEF_CAP))
            for bone, quads in meshes.items():
                by_bone.setdefault(bone, {})[key] = quads
            stats.append((troop, key, total))
            im = composite(im, relief)
        out['%s/%s' % (TEXTURES, f)] = png_bytes(im)
        if density != TROOP_DENSITY:
            out['%s/%s' % (TEXTURES, far_name(f))] = png_bytes(far_twin(im, density, islands))
    for b in model['bones']:
        if b['name'] in by_bone:
            b['relief'] = {k: by_bone[b['name']][k] for k in sorted(by_bone[b['name']], key=lambda k: (len(k), k))}
    out['%s/%s.json' % (MODELS, troop)] = dumps(model)
    cap = GLOW_CAP.get(cat.sheet[troop]['rarity'], 6)
    held = max([weapon_lit.get(w, 0) for w in weapon_tiers(cat, model['weapon']).values()] or [0])   # the brightest tier it may hold
    n = (lit(read_png(folder / extra['glow'], troop)) / density ** 2 if extra['glow'] else 0) + held
    if n > cap:
        fail("%s: %s lit area in its glow and its weapon's glow together, over the %s cap of %d"
             % (troop, px_words(n), cat.sheet[troop]['rarity'], cap))
    return out, stats


def cloth(cat: Catalog, side) -> Image.Image:
    """The team cloth sheet of a side (a factions id), linen for None, or the company's navy for 'company': the
    template's three greys take the colour x1.0, x0.8 and x0.6, every other pixel keeps its paint, and a side's glyph mask
    goes on in white (linen and the company carry none)."""
    if cat.template is None:
        fail('art/team/team_cloth.png is missing')
    if side is None:
        rgb, glyph = LINEN, None
    elif side == 'company':
        rgb, glyph = COMPANY, None
    else:
        row = cat.factions.get(side)
        if row is None:
            fail('cloth: no factions row %s' % side)
        c = row['color']
        rgb = ((c >> 16) & 255, (c >> 8) & 255, c & 255)
        glyph = cat.glyphs.get(row['glyph'])
        if glyph is None:
            fail('cloth %s: art/team/glyph_%s.png is missing' % (side, row['glyph']))
    out = cat.template.copy()
    px = out.load()
    w, h = out.size
    if glyph is not None and glyph.size != out.size:
        fail('glyph_%s.png is %dx%d, not the cloth template size %dx%d' % (row['glyph'], glyph.size[0], glyph.size[1], w, h))
    mask = glyph.load() if glyph is not None else None
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            k = GREYS.get((r, g, b)) if a else None
            if mask is not None and mask[x, y][3]:
                if k is None:
                    fail('glyph_%s.png: pixel (%d, %d) is not on a field grey of team_cloth.png' % (row['glyph'], x, y))
                px[x, y] = GLYPH + (255,)
            elif k is not None:
                px[x, y] = tuple(int(c * k + 0.5) for c in rgb) + (a,)
    return out


def chevron(cat: Catalog) -> Image.Image:
    """The rank chevron (in-match progression design 7), checked: at most CHEVRON_MAX px a side, alpha 0 or 255, every
    opaque pixel grey (r = g = b), since the client multiplies it by the side colour."""
    im = read_png(cat.root / CHEVRON_SRC, 'chevron')
    what = CHEVRON_SRC
    if im.width > CHEVRON_MAX or im.height > CHEVRON_MAX:
        fail('%s is %dx%d, over the %d px a side a chevron may be' % (what, im.width, im.height, CHEVRON_MAX))
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a not in (0, 255):
                fail('%s: pixel (%d, %d) has alpha %d; a chevron pixel is opaque or clear' % (what, x, y, a))
            if a and not r == g == b:
                fail('%s: pixel (%d, %d) #%02X%02X%02X is not grey; the client tints the chevron the side colour' % (what, x, y, r, g, b))
    if not any(a for a in im.tobytes()[3::4]):
        fail('%s is empty' % what)
    return im


# ---- in-match progression Task 9: armour and weapon tiers, the stopgap list and the release check ----

def weapon_tiers(cat, wid):
    """{tier: weapon id} of the models a troop holding wid has: tier I is wid, tier n is <wid>_t<n> where art/weapons has
    one; {} for no held weapon."""
    if not wid or wid == 'none':
        return {}
    out = {1: wid}
    for t in cat.tiers:
        if t > 1 and '%s_t%d' % (wid, t) in cat.weapons:
            out[t] = '%s_t%d' % (wid, t)
    return out


def drawn_tiers(own, tiers):
    """{tier: the tier it draws}: its own art when it has some, else the highest tier below it that has (the stopgap,
    in-match progression design 4.4); tier I always has its own."""
    out, last = {}, 1
    for t in sorted(tiers):
        if t == 1 or t in own:
            last = t
        out[t] = last
    return out


def part_tiers(cat, src) -> set:
    """The weapon tiers a troop's weaponTiers parts have whole: a tier every bone that carries a tier I weapon part also
    carries a part of (Task 9 review M7: a tier II cap without its tier II guard is no tier II); TroopModel.partTiers is the
    client's twin. Empty for a troop without weapon parts."""
    by_bone = {}
    for e in src.get('parts', []) or []:
        if e.get('weaponTiers'):
            part = cat.parts.get(e.get('part'), {})
            by_bone.setdefault(e.get('bone', part.get('bone')), set()).update(e['weaponTiers'])
    if not by_bone:   # a hand-authored troop: its weaponTiers bones, by parent bone, as TroopModel groups them
        f = Path(cat.root) / 'src/main/resources/assets' / NAMESPACE / MODELS / ('%s.json' % src.get('troop', ''))
        model = json.loads(f.read_text(encoding='utf-8')) if (Path(cat.root) / 'art/hand' / src.get('troop', '-')).is_dir() and f.is_file() else {}
        for b in model.get('bones', []):
            if b.get('weaponTiers'):
                by_bone.setdefault(b['parent'], set()).update(b['weaponTiers'])
    if not by_bone:
        return set()
    every = set().union(*by_bone.values())
    return {t for t in every if all(t in tiers for tiers in by_bone.values() if 1 in tiers)}


def tier_table(cat: Catalog) -> list:
    """Per sheet troop, by id: {troop, armour, weapon, kind}. armour and weapon map each tier to the tier it draws (None for a
    troop with no model, which draws its vanilla mob); kind names the held weapon, 'parts' (its weapon tier is a weaponTiers
    part set) or both."""
    rows = []
    for tid in sorted(cat.sheet):
        src = cat.troops.get(tid)
        if src is None:
            rows.append({'troop': tid, 'armour': None, 'weapon': None, 'kind': None})
            continue
        own, _ = troop_files(cat, tid)
        parts = part_tiers(cat, src)
        held = weapon_tiers(cat, src.get('weapon'))
        if held:
            have, kind = set(held), src['weapon'] + (' and parts' if parts else '')
            if parts:
                have &= parts | {1}
        else:
            have, kind = parts | {1}, 'parts'
        rows.append({'troop': tid, 'armour': drawn_tiers(set(own), cat.tiers), 'weapon': drawn_tiers(have, cat.tiers), 'kind': kind})
    return rows


def stopgaps(cat: Catalog) -> list:
    """[(troop, why)] of every troop that draws a stopgap tier or has no model, by id: the 0.5.0 art gate."""
    out = []
    for r in tier_table(cat):
        if r['armour'] is None:
            out.append((r['troop'], 'no model'))
            continue
        gaps = ['%s %s draws %s' % (part, ROMAN.get(t, t), ROMAN.get(d, d)) for part in ('armour', 'weapon')
                for t, d in r[part].items() if t != d]
        if gaps:
            out.append((r['troop'], ', '.join(gaps)))
    return out


def tier_line(drawn):
    return ' '.join(ROMAN.get(t, str(t)) if t == d else '%s->%s' % (ROMAN.get(t, t), ROMAN.get(d, d)) for t, d in drawn.items())


def stopgap_list(gaps):
    return ', '.join('%s (%s)' % g for g in gaps)


def tiers(root: Path) -> int:
    """--tiers: one line per sheet troop with its armour and weapon tiers (III->II: tier III draws tier II's art), then the
    stopgap list."""
    cat = load(root)
    for r in tier_table(cat):
        if r['armour'] is None:
            print('build_skins: tiers %s: no model (draws its vanilla mob)' % r['troop'])
        else:
            print('build_skins: tiers %s: armour %s; weapon %s %s' % (r['troop'], tier_line(r['armour']), r['kind'], tier_line(r['weapon'])))
    gaps = stopgaps(cat)
    print('build_skins: stopgap %s' % ('%d troops: %s' % (len(gaps), stopgap_list(gaps)) if gaps else 'none'))
    return 0


def release_gate(root: Path, argv, tool='package') -> str:
    """The release files' tier check (in-match progression design 4.4, round 34 question 6), run first by tools/package.py,
    export_modrinth.py and export_server.py: '' when no troop draws a stopgap tier; with one, '-stopgap' (the file names'
    mark, release_name) under --allow-stopgap, else it prints the list and exits 1."""
    gaps = stopgaps(load(root))
    if not gaps:
        return ''
    if '--allow-stopgap' in argv:
        print('%s: --allow-stopgap: %d troops draw a stopgap tier, so the file name carries -stopgap: %s' % (tool, len(gaps), stopgap_list(gaps)))
        return '-stopgap'
    print('%s: refused: %d troops still draw a stopgap tier (0.5.0 waits for every troop\'s own weapon and armour '
          'tiers I to III): %s' % (tool, len(gaps), stopgap_list(gaps)))
    print('%s: for a marked local build run python tools/%s.py --allow-stopgap' % (tool, tool))
    sys.exit(1)


def release_name(release, mark, suffix) -> str:
    """A release file's name: graves-and-glory-<version><mark><suffix> (suffix .zip, .mrpack or -server.zip)."""
    return 'graves-and-glory-%s%s%s' % (release, mark, suffix)


def release_archive(manifest, release, mark, tool='export') -> str:
    """The player archive dist/package-manifest.json names when it is this version's, with this gate's mark; else the tool
    refuses (exit 1), so a -stopgap package never feeds an unmarked .mrpack or server kit, nor a stale one a new kit."""
    want = release_name(release, mark, '.zip')
    if manifest.get('fileName') != want:
        print('%s: refused: dist/package-manifest.json names %s, not %s; run python tools/package.py%s first'
              % (tool, manifest.get('fileName'), want, ' --allow-stopgap' if mark else ''))
        sys.exit(1)
    return want


def build(cat):
    """Every output as {path under the assets root: bytes}, plus the index, the number of cloths and the relief stages and
    weapon frames built as (owner, name, quads) rows."""
    files = {}
    weapon_lit = {}
    stats = {'relief': [], 'weapons': []}
    for wid in sorted(cat.weapons):
        out, n, frames = weapon_outputs(cat, wid)
        files.update(out)
        weapon_lit[wid] = n
        stats['weapons'] += frames
    for rid in sorted(cat.rigs):
        rig = cat.rigs[rid]
        bones = bake_bones(rig, 'rig ' + rid)
        files['%s/rig/%s.json' % (MODELS, rid)] = dumps(
            {'format': FORMAT, 'rig': rid, 'size': rig['size'], 'outer': num(rig['outer']), 'hands': rig['hands'],
             'bones': [clean_bone(b, rig['size']) for b in bones]})
    for tid in sorted(cat.troops):
        hand = hand_outputs(cat.root, tid)
        if hand is not None:
            files.update(hand)
            continue
        out, relief = troop_outputs(cat, tid, weapon_lit)
        files.update(out)
        stats['relief'] += relief
    sides = list(cat.factions)
    for side in sides + [None, 'company']:
        files['%s/team_cloth_%s.png' % (TEXTURES, side or 'linen')] = png_bytes(cloth(cat, side))
    if (cat.root / CHEVRON_SRC).is_file():
        files['%s/chevron.png' % TEXTURES] = png_bytes(chevron(cat))
    index = {'format': FORMAT, 'troops': sorted(cat.troops), 'rigs': sorted(cat.rigs), 'weapons': sorted(cat.weapons),
             'outputs': sorted(files)}
    files['%s/index.json' % MODELS] = dumps(index)
    return files, index, len(sides) + 2, stats


def hand_outputs(root, tid):
    """The outputs of a hand-authored troop (art/hand/<troop>/build_<troop>.py, its outputs(): {path: image or JSON object}),
    or None when the troop has no such script and is generated here."""
    script = Path(root) / 'art' / 'hand' / tid / ('build_%s.py' % tid)
    if not script.is_file():
        return None
    import importlib.util
    sys.path.insert(0, str(script.parent))
    try:
        spec = importlib.util.spec_from_file_location('hand_' + tid, script)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        made = mod.outputs()
    finally:
        sys.path.remove(str(script.parent))
    return {rel: dumps(v) if isinstance(v, dict) else png_bytes(v) for rel, v in made.items()}


def most(rows):
    """(quads, 'owner name') of the biggest of (owner, name, quads) rows, the first of equals; (0, '-') for none."""
    top = max(rows, key=lambda r: r[2], default=None)
    return (0, '-') if top is None else (top[2], '%s %s' % (top[0], top[1]))


def listed(index_file):
    try:
        return set(json.loads(index_file.read_text(encoding='utf-8')).get('outputs', []))
    except (OSError, ValueError, AttributeError):
        return set()


def owned_files(out):
    found = []
    for folder in (MODELS, TEXTURES):
        if (out / folder).is_dir():
            found += [p.relative_to(out).as_posix() for p in (out / folder).rglob('*') if p.is_file()]
    return sorted(found)


def export(root: Path, out: Path) -> dict:
    """Builds every output into out (the assets root), removes what the previous index listed and this build no longer
    makes, and returns the new index."""
    root, out = Path(root), Path(out)
    cat = load(root)
    files, index, cloths, stats = build(cat)
    before = listed(out / MODELS / 'index.json')
    for rel, data in files.items():
        path = out / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.is_file() or differs(rel, path.read_bytes(), data):
            path.write_bytes(data)
    for gone in sorted(before - set(index['outputs'])):
        path = out / gone
        if path.is_file():
            path.unlink()
            print('build_skins: removed ' + gone)
    strays = sorted(set(owned_files(out)) - set(files))
    if strays:
        print('build_skins: left %d file(s) that no index listed, first %s; --verify fails until they go' % (len(strays), strays[0]))
    print('build_skins: %d troops, %d rigs, %d weapons, %d cloths' % (len(cat.troops), len(cat.rigs), len(cat.weapons), cloths))
    print('build_skins: relief %d stages, %d quads (most %d, %s); weapons %d frames, %d quads (most %d, %s)' % (
        (len(stats['relief']), sum(n for _, _, n in stats['relief'])) + most(stats['relief']) +
        (len(stats['weapons']), sum(n for _, _, n in stats['weapons'])) + most(stats['weapons'])))
    try:
        shown = out.resolve().relative_to(root.resolve()).as_posix()
    except ValueError:
        shown = out.as_posix()
    print('build_skins: %d outputs in %s' % (len(files), shown))
    return index


def pixels(data):
    """A PNG's size and its pixels as RGBA bytes."""
    with Image.open(io.BytesIO(data)) as im:
        return im.size, im.convert('RGBA').tobytes()


def differs(rel, a, b):
    """How a committed output differs from its rebuild, or None when they match: a PNG by its size and RGBA pixels (the
    same pixels may be encoded in other bytes by another Pillow or zlib build, and that is no change), anything else byte
    for byte."""
    if a == b:
        return None
    if rel.endswith('.png'):
        try:
            (sa, pa), (sb, pb) = pixels(a), pixels(b)
        except Exception as e:  # Pillow raises OSError, SyntaxError or zlib.error on a broken file
            return 'is not a PNG its rebuild can be compared with (%s)' % e
        if sa != sb:
            return 'is %dx%d, its rebuild %dx%d' % (sa + sb)
        if pa == pb:
            return None
        at = next(i for i in range(len(pa)) if pa[i] != pb[i]) // 4
        return 'differs from its rebuild at pixel (%d, %d): #%s, rebuilt #%s' % (
            at % sa[0], at // sa[0], pa[at * 4:at * 4 + 4].hex().upper(), pb[at * 4:at * 4 + 4].hex().upper())
    at = next((i for i in range(min(len(a), len(b))) if a[i] != b[i]), min(len(a), len(b)))
    return 'differs from its rebuild at byte %d' % at


def verify(root: Path) -> int:
    """Rebuilds into a temp dir and compares every output with the committed one (differs: PNGs by pixels, the rest byte
    for byte); exits 1 naming the first difference, a missing output or a file the index does not list."""
    root = Path(root)
    out = root / ASSETS
    if not (out / MODELS / 'index.json').is_file():
        fail('%s/index.json is missing; run python tools/build_skins.py' % MODELS)
    with tempfile.TemporaryDirectory() as tmp:
        quiet = io.StringIO()
        real, sys.stdout = sys.stdout, quiet
        try:
            index = export(root, Path(tmp))
        except SystemExit:
            sys.stdout = real
            print(quiet.getvalue().rstrip())
            raise
        finally:
            sys.stdout = real
        fresh = sorted(index['outputs'] + ['%s/index.json' % MODELS])
        for rel in fresh:
            a, b = out / rel, Path(tmp) / rel
            if not a.is_file():
                fail('%s is missing; run python tools/build_skins.py' % rel)
            why = differs(rel, a.read_bytes(), b.read_bytes())
            if why:
                fail('%s %s; run python tools/build_skins.py' % (rel, why))
        extra = sorted(set(owned_files(out)) - set(fresh))
        if extra:
            fail('%s is not in %s/index.json' % (extra[0], MODELS))
    print('build_skins: %d outputs verified' % len(fresh))
    return 0


FACE_SHADE = {'top': 1.25, 'bottom': 0.55, 'north': 1.0, 'south': 0.7, 'west': 0.85, 'east': 0.85}
HUES = [(196, 92, 84), (84, 140, 196), (104, 180, 96), (200, 164, 72), (150, 100, 190), (70, 170, 170), (210, 120, 50),
        (120, 120, 200), (180, 90, 150), (110, 160, 60), (90, 110, 130), (200, 200, 110)]


def faces(uv, size, density=1, faceuv=None):
    """The six face rectangles of a box on its sheet, by the side they show (north is the front, west the model's right), in
    the texels of a sheet of this density; a face that faceuv moves at its own texels."""
    k = density
    rect = face_layout(uv, size, faceuv)
    return {name: tuple(k * c for c in rect[f]) for name, f in
            (('top', 'top'), ('bottom', 'bottom'), ('west', 'right'), ('north', 'front'), ('east', 'left'), ('south', 'back'))}


def template(cat: Catalog, troop: str) -> Image.Image:
    """Every skin box face of the troop in a flat colour (one hue per bone; top light, bottom dark, the front plain)."""
    model = bake(cat, troop)
    density = model.get('density', TROOP_DENSITY)
    im = Image.new('RGBA', (model['size'][0] * density, model['size'][1] * density), (0, 0, 0, 0))
    px = im.load()
    for i, b in enumerate(x for x in model['bones'] if not x['team'] and x['boxes']):
        base = HUES[i % len(HUES)]
        for box in b['boxes']:
            for face, (x0, y0, x1, y1) in faces(box['uv'], box['size'], bone_density(model, b), box.get('faceuv')).items():
                k = FACE_SHADE[face]
                colour = tuple(max(0, min(255, int(c * k))) for c in base) + (255,)
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        px[x, y] = colour
    return im


def box_face_rects(uv, size, faceuv=None):
    """The six faces of a box's ModelPart layout as (x0, y0, x1, y1) on the sheet, one rect each (footprint's two rows
    split), by Blockbench's names (the display group's 180 turn swaps east and west): up, down, east, north, west, south;
    a face that faceuv moves at its own texels."""
    rect = face_layout(uv, size, faceuv)
    return {bb: rect[f] for bb, f in (('up', 'top'), ('down', 'bottom'), ('east', 'right'), ('north', 'front'),
                                       ('west', 'left'), ('south', 'back'))}


def troop_project(cat, troop, dest=None):
    """Writes art/troops/<troop>.bbmodel from the troop json: one display group turned 180 about x (Blockbench's y is up,
    the rig's is down) around groups named for the rig bones and parts, with the skin cubes box UV on the troop sheet.
    The rig's own coordinates, rotations and UVs are kept as they are, so the build reads the file back unchanged.
    With dest, writes there instead and does not refuse an existing art/troops file (tools/anim_edit.py's scratch copy)."""
    src = cat.troops.get(troop)
    if src is None:
        fail('%s: no art/troops/%s.json' % (troop, troop))
    scratch = dest is not None
    dest = Path(dest) if scratch else cat.root / 'art/troops' / (troop + '.bbmodel')
    if dest.is_file() and not scratch:
        fail('%s: art/troops/%s.bbmodel already exists; it is the source of the troop\'s skin geometry now. Delete it '
             'first only if the json still holds that geometry, or the boxes move out of the build.' % (troop, troop))
    rig = cat.rigs.get(src.get('rig'))
    if rig is None:
        fail('%s: unknown rig %s' % (troop, src.get('rig')))
    size = vec(src.get('size', rig['size']), 2, troop + ' size', ints=True, minimum=1)
    idle = src.get('idleArms', 'hang')
    if idle not in IDLE_ARMS:
        fail('%s: idleArms %s is not one of %s' % (troop, idle, ', '.join(IDLE_ARMS)))
    parts = []
    for e in src.get('parts', []):
        p = cat.parts.get(e.get('part'))
        if p is None:
            fail('%s: unknown part %s' % (troop, e.get('part')))
        parts.append((e, p))
    stages = cat.stages[troop]
    bones, _ = merge_bones(rig, troop, src.get('bones'), parts, idle == 'folded', stages)
    order = list(bones.values())
    flat = {}
    for b in order:
        p = flat.get(b['parent'], [0.0, 0.0, 0.0])
        flat[b['name']] = [p[i] + b['pivot'][i] for i in range(3)]

    def name_of(b):
        if not b['part']:
            return b['name']
        if b['team']:
            return 'team:' + b['part'] + ('' if b['name'] == b['part'] else ':' + b['name'])
        return 'part:' + b['part'] + ('' if b['name'] == b['part'] else ':' + b['name'])

    def uid(kind, key):
        return str(uuid.uuid5(uuid.NAMESPACE_URL, 'gravesandglory/troop/%s/%s/%s' % (troop, kind, key)))

    group_name = {b['name']: name_of(b) for b in order}
    skins = src.get('skins') or {}
    if not skins.get('1'):
        fail('%s: skins needs a tier 1 skin to show in Blockbench' % troop)
    textures, skin_tex = [], {}
    for tier in sorted(stages):
        f = skins.get(str(tier))
        if not f:
            continue
        path = cat.root / 'art/troops' / f
        if not path.is_file():
            fail('%s: %s is missing from art/troops' % (troop, f))
        with Image.open(path) as im:
            w, h = im.size
        skin_tex[tier] = len(textures)
        textures.append({'name': f, 'relative_path': f, 'width': w, 'height': h,
                         'uv_width': size[0], 'uv_height': size[1], 'particle': False, 'render_mode': 'default'})
    team = any(b['team'] for b in order)
    cloth_tex = None
    if team:
        path = cat.root / 'art/team/team_cloth.png'
        if not path.is_file():
            fail('%s: team parts need art/team/team_cloth.png' % troop)
        with Image.open(path) as im:
            w, h = im.size
        cloth_tex = len(textures)
        textures.append({'name': 'team_cloth.png', 'relative_path': '../team/team_cloth.png', 'width': w, 'height': h,
                         'uv_width': cat.cloth_layout[0], 'uv_height': cat.cloth_layout[1], 'particle': False, 'render_mode': 'default'})

    groups, elements, child_groups, child_cubes = [], [], {}, {}
    for b in order:
        name = group_name[b['name']]
        groups.append({'uuid': uid('group', name), 'name': name, 'origin': [num(c) for c in flat[b['name']]],
                       'rotation': [num(c) for c in b['rotation']], 'color': 0, 'visibility': True, 'isOpen': True,
                       'autouv': 0, 'export': True, 'locked': False, 'selected': False})
        child_groups.setdefault(name, [])
        child_cubes[name] = []
    for b in order:
        name = group_name[b['name']]
        if b['parent']:
            child_groups[group_name[b['parent']]].append(name)
    for b in order:
        name = group_name[b['name']]
        base = flat[b['name']]
        for i, box in enumerate(b['boxes']):
            if box['mirror']:
                fail('%s: box %d of %s is mirrored; a troop .bbmodel round-trip does not carry a mirrored box yet '
                     '(the rig has one in arms_folded): export this troop when its idleArms is hang, or add mirror support'
                     % (troop, i, name))
            frm = [base[j] + box['origin'][j] for j in range(3)]
            to = [frm[j] + box['size'][j] for j in range(3)]
            eid = uid('cube', '%s#%d' % (name, i))
            if b['team']:
                if cloth_tex is None:
                    fail('%s: team part %s has no cloth texture' % (troop, name))
                faces = {f: {'uv': [float(c) for c in r], 'texture': cloth_tex} for f, r in box_face_rects(box['uv'], box['size']).items()}
                element = {'uuid': eid, 'name': '%s_%d' % (name, i), 'type': 'cube', 'box_uv': False, 'rescale': False,
                           'locked': False, 'light_emission': 0, 'render_order': 'default', 'allow_mirror_modeling': True,
                           'from': [num(c) for c in frm], 'to': [num(c) for c in to], 'autouv': 0, 'color': 0,
                           'origin': [num(c) for c in base], 'faces': faces}
            else:
                # a jointed limb's box (faceuv) is per-face UV in Blockbench, its uv_offset still the box's uv (bbmodel reads it back)
                moved = box.get('faceuv')
                element = {'uuid': eid, 'name': '%s_%d' % (name, i), 'type': 'cube', 'box_uv': not moved, 'rescale': False,
                           'locked': False, 'light_emission': 0, 'render_order': 'default', 'allow_mirror_modeling': True,
                           'from': [num(c) for c in frm], 'to': [num(c) for c in to], 'autouv': 0, 'color': 0,
                           'origin': [num(c) for c in base], 'uv_offset': [int(box['uv'][0]), int(box['uv'][1])],
                           'faces': ({f: {'uv': [float(c) for c in r], 'texture': 0}
                                      for f, r in box_face_rects(box['uv'], box['size'], moved).items()} if moved
                                     else {f: {'texture': 0} for f in bbmodel.FACES})}
            elements.append(element)
            child_cubes[name].append(eid)

    def node(name):
        return {'name': name, 'uuid': uid('group', name), 'export': True, 'isOpen': True,
                'children': [node(c) for c in child_groups[name]] + child_cubes[name]}

    display = {'uuid': uid('group', 'model'), 'name': 'model', 'origin': [0, 0, 0], 'rotation': [180, 0, 0], 'color': 0,
               'visibility': True, 'isOpen': True, 'autouv': 0, 'export': True, 'locked': False, 'selected': False}
    project = {'meta': {'format_version': '5.0', 'model_format': 'free', 'box_uv': True},
               'name': troop, 'model_identifier': '', 'visible_box': [1, 1, 0], 'variable_placeholders': '',
               'variable_placeholder_buttons': [], 'timeline_setups': [], 'unhandled_root_fields': {},
               'resolution': {'width': size[0], 'height': size[1]}, 'elements': elements,
               'groups': [display] + groups,
               'outliner': [{'name': 'model', 'uuid': display['uuid'], 'export': True, 'isOpen': True,
                             'children': [node('root')]}],
               'textures': textures}
    return bbmodel.write(project, dest)


def main(args):
    if args == ['--verify']:
        return verify(ROOT)
    if args == ['--tiers']:
        return tiers(ROOT)
    if len(args) == 2 and args[0] == '--to-bbmodel':
        cat = load(ROOT)
        path = troop_project(cat, args[1])
        print('build_skins: wrote %s' % path.relative_to(ROOT).as_posix())
        return 0
    if len(args) == 2 and args[0] == '--template':
        cat = load(ROOT)
        for im, name in ((template(cat, args[1]), '_template.png'), (relief_template(cat, args[1]), '_relief_template.png')):
            dest = ROOT / 'work/troops' / (args[1] + name)
            dest.parent.mkdir(parents=True, exist_ok=True)
            im.save(dest)
            print('build_skins: template %s' % dest.relative_to(ROOT).as_posix())
        return 0
    if len(args) == 3 and args[0] == '--turn':
        turn_sheets(Path(args[1]), Path(args[2]))
        return 0
    if len(args) == 3 and args[0] == '--contact':
        contact(Path(args[1]), Path(args[2]))
        return 0
    if not args:
        export(ROOT, ROOT / ASSETS)
        return 0
    print(__doc__)
    return 2


if __name__ == '__main__':
    if sys.version_info[:2] != (3, 12):
        sys.exit('build_skins: needs Python 3.12, not %d.%d' % sys.version_info[:2])
    if os.environ.get('PYTHONHASHSEED') != '0':
        os.environ['PYTHONHASHSEED'] = '0'
        sys.exit(subprocess.call([sys.executable] + sys.argv))
    sys.exit(main(sys.argv[1:]))
