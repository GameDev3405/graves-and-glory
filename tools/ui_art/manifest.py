"""The UI kit export list (UI plan Task 1, pinned): each texture's folder/name, its painter call, kind, slice
insets and box offset. "Mock" is the mockups' own ASSETS[name] call, merged in v4art's order (later painters
override earlier ones of the same name). Insets are measured from the PNG's edges; a slice's right and bottom
insets include the shadow the painter puts outside the plate, so the screws and rules stay in the corners."""
from collections import namedtuple

import v4art
import v4a_table, v4a_paper, v4a_hud, v4a_recruit, v4a_ink, v4a_map, v4a_furn, v4a_portrait, v4a_recruit2
from v4pal import IRON
from v4a_hud import seal, track, spine, ledger_card, hp_fill, feed_glyph_ink, BRASSR
import kit_extra

MOCK = dict(v4art.ASSETS)
for _mod in (v4a_table, v4a_paper, v4a_hud, v4a_recruit, v4a_ink, v4a_map, v4a_furn, v4a_portrait, v4a_recruit2):
    MOCK.update(_mod.ASSETS)

Entry = namedtuple('Entry', 'paint kind l t r b ox oy frames')
MANIFEST = {}


def add(name, paint, kind='fixed', l=0, t=0, r=0, b=0, ox=0, oy=0, frames=1):
    assert name not in MANIFEST, name
    MANIFEST[name] = Entry(paint, kind, l, t, r, b, ox, oy, frames)


def mock(folder, name, **kw):
    add(folder + '/' + name, MOCK[name], **kw)


def crop(paint, x, y, w, h):
    return lambda: paint().crop(x, y, w, h)


# ================================================================ hud/
for _n in ('boss-dawn', 'boss-dusk', 'chain', 'hook', 'hook-brass', 'stamp-rout', 'horn', 'ability-slot',
           'key-brass-plate', 'hp-channel', 'hp-channel-grey', 'notice-slip'):
    mock('hud', _n)
mock('hud', 'score-card', ox=1, oy=1)
mock('hud', 'feed-slip', ox=1, oy=3)
for _g in ('g-attack', 'g-hold', 'g-follow', 'g-shieldwall', 'g-line', 'g-compact', 'g-fight', 'g-cycle'):
    mock('hud', _g)
    mock('hud', _g + '-faded')
mock('hud', 'g-fight-red')
for _g in kit_extra.GLYPHS:
    add('hud/' + _g, (lambda g: lambda: kit_extra.glyph_ink(g))(_g))
    add('hud/' + _g + '-faded', (lambda g: lambda: kit_extra.glyph_faded(g))(_g))
# point seals: Dawn round (A), contested square with swords (B), Dusk diamond (C), neutral bare square
add('hud/seal-dawn', lambda: seal('A'))
add('hud/seal-contested', lambda: seal('B'))
add('hud/seal-dusk', lambda: seal('C'))
add('hud/seal-neutral', kit_extra.seal_neutral)
# score tracks: the empty groove is the same for both teams (ticks at x 18 and 37); the fills are rows 2-5,
# columns 1-54 of a full track, with the dark edge at the inner end (Dawn's last column, Dusk's first)
add('hud/track-empty', lambda: track(56, 0, 'dawn'))
add('hud/track-dawn-fill', crop(lambda: track(56, 1.0, 'dawn'), 1, 2, 54, 4))
add('hud/track-dusk-fill', crop(lambda: track(56, 1.0, 'dusk'), 1, 2, 54, 4))
# the spine, 9 x 58: its 24 px middle holds one rivet, so tiling keeps the painter's 24 px rivet pitch
add('hud/spine', lambda: spine(56), kind='three-v', t=18, b=16)
# ledger cards, and the heights the mock did not paint, from the same ledger_card arguments
mock('hud', 'card-plain', ox=1, oy=1)
mock('hud', 'card-sel', ox=1, oy=1)
mock('hud', 'card-rout', ox=1, oy=1)
mock('hud', 'card-flash', ox=2, oy=2)
add('hud/card-plain-62', lambda: ledger_card(176, 62, seed=11), ox=1, oy=1)
add('hud/card-flash-62', lambda: ledger_card(176, 62, seed=12, flash=True), ox=2, oy=2)
add('hud/card-rout-62', lambda: ledger_card(176, 62, kind='grey', seed=14, rust=0.35), ox=1, oy=1)
add('hud/card-sel-48', lambda: ledger_card(176, 48, metal=BRASSR, seed=13), ox=1, oy=1)
# bar fills, 102 x 3, cut to length in game (UiTex.bar)
add('hud/hp-full-dawn', lambda: hp_fill(102))
add('hud/hp-lost', lambda: hp_fill(102, 'lost'))
add('hud/hp-rout', lambda: hp_fill(102, 'rout'))
add('hud/hp-full-dusk', kit_extra.hp_full_dusk)
mock('hud', 'key-chip', kind='three-h', l=3, r=3)
# feed glyphs inked on the vellum slip: seal ours, struck seal theirs, in each team's hue
add('hud/fgv-seal-dawn', MOCK['fgv-seal'])
add('hud/fgv-seal-x-dusk', MOCK['fgv-seal-x'])
add('hud/fgv-seal-dusk', lambda: feed_glyph_ink('fg-seal', 'them'))
add('hud/fgv-seal-x-dawn', lambda: feed_glyph_ink('fg-seal-x', 'us'))
for _n in ('fgv-skull', 'fgv-skull-grey', 'fgv-banner-down', 'fgv-banner-up', 'fgv-rout', 'fgv-sun'):
    mock('hud', _n)
add('hud/fgv-moon', kit_extra.fgv_moon)
# cards that carry text
add('hud/lesson-card', lambda: ledger_card(400, 40, 'lo', IRON, seed=15), ox=1, oy=1)
add('hud/respawn-card', lambda: ledger_card(200, 28, 'lo', IRON, seed=16), ox=1, oy=1)

# ================================================================ table/
# the 1280 x 720 tables, the mock's avoid boxes shifted by (160, 90) as the 960 x 540 page is centred on them
add('table/oak-table', lambda: v4art.oak_table(W=1280, H=720, avoid=[(200, 270, 520, 462)]))
add('table/oak-table-recruit', lambda: v4art.oak_table(W=1280, H=720, avoid=[(824, 434, 1064, 586)]))
mock('table', 'rail', kind='three-h', l=0, r=0)
for _w in (96, 104, 112, 120):     # plates 6 px in from each edge; the PNG adds 1 px of shadow right, 2 below
    mock('table', 'plaque-%d' % _w, kind='nine', l=6, t=6, r=7, b=8)
for _w in (64, 80, 88):            # tags 4 px in from each edge; the PNG adds 1 px of shadow right, 2 below
    mock('table', 'btn-vellum-%dx24' % _w, kind='nine', l=4, t=4, r=5, b=6)
for _n in ('camp', 'recruit', 'banner', 'contracts', 'chapter', 'friends', 'table'):
    mock('table', 'icon-' + _n)
mock('table', 'wax-badge')
for _w in range(13, 49):
    mock('table', 'key-brass-%d' % _w)
for _n in ('frame-back-48', 'frame-back-empty-48', 'frame-rim-48', 'frame-back-160', 'frame-ornate-184', 'invite-plus',
           'ready-check', 'order-lo', 'order-lo2', 'order-hi', 'ribbon-teal', 'wax-crown', 'ink-practice',
           'ink-skirmish', 'ink-campaign', 'field-card', 'tack', 'map-rivergate', 'map-seal', 'quill',
           'btn-brass-240x40', 'prop-maproll', 'prop-glow', 'prop-candle', 'prop-inkwell'):
    mock('table', _n)
add('table/btn-iron-240x40', lambda: kit_extra.btn_iron(MOCK['btn-brass-240x40']()))

# ================================================================ recruit/
mock('recruit', 'banner-rod', kind='three-h', l=9, r=9)
for _h in ('gilded', 'graveborn', 'wildmoot', 'ironclad', 'tidecallers'):
    mock('recruit', 'banner-' + _h)
    mock('recruit', 'banner-' + _h + '-on')
mock('recruit', 'muster-roll', ox=15)
mock('recruit', 'row-mark', ox=4)
for _n in ('rule-208', 'star-s', 'star-s-empty', 'star-l', 'star-l-empty', 'sparkle', 'padlock-brass', 'letter-hi',
           'wax-star', 'wax-broken', 'letter-bundle', 'coin-m', 'pouch', 'die'):
    mock('recruit', _n)

# portraits are live models, never exported
assert not any('pt-' in n for n in MANIFEST)
