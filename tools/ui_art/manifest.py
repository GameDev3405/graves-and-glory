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
import kit_icons
import kit_props
import kit_track
import kit_maps
import hud_laststand
import hud_veterans

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
for _n in ('boss-dawn', 'boss-dusk', 'chain', 'hook', 'hook-brass', 'horn', 'ability-slot',
           'key-brass-plate', 'hp-channel', 'hp-channel-grey'):
    mock('hud', _n)
# the class actives' own pictures for the ability slot (kit_icons.ABILITY), 22 x 20 as the horn is, drawn at (5, 6) of the slot by
# BattleHud.ability by the active's id: hud/ability-stand_fast, -bloodrush, -mark_prey, -rally_horn (Muster), -grave_bloom, -forward_supply
for _a, _paint in kit_icons.ABILITY.items():
    add('hud/ability-' + _a, _paint)
mock('hud', 'score-card', ox=1, oy=1)
mock('hud', 'feed-slip', ox=1, oy=3)
for _g in ('g-attack', 'g-hold', 'g-follow', 'g-line', 'g-compact', 'g-fight', 'g-cycle'):
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
# the selected group's ledger card, 62 px tall with its key row (the mock's card-sel, and its loss flash and fallen states from
# the same ledger_card arguments); the mock's 48 px cards and the plain iron card went with UI Task 8c, where only the
# selected group keeps a card and every other group hangs as a chip. A group at its last stand keeps the live card-sel
mock('hud', 'card-sel', ox=1, oy=1)
add('hud/card-flash-62', lambda: ledger_card(176, 62, seed=12, flash=True), ox=2, oy=2)
add('hud/card-fallen-62', lambda: ledger_card(176, 62, kind='grey', seed=14, rust=0.35), ox=1, oy=1)
# the lighter battle HUD (UI Task 8c, kit_extra): a group's chip in its four looks (iron, the loss flash, brass while the
# army is selected, grey and rusted when fallen), its strength channel, the army's key strip,
# the tutorial's lesson note, the narrower hint slip and the Compact size's feed leaf of three entries
add('hud/chip-plain', lambda: kit_extra.chip(seed=41), ox=1, oy=1)
add('hud/chip-flash', lambda: kit_extra.chip(seed=42, flash=True), ox=2, oy=2)
add('hud/chip-sel', lambda: kit_extra.chip(metal=BRASSR, seed=43), ox=1, oy=1)
add('hud/chip-fallen', lambda: kit_extra.chip('grey', seed=44, rust=0.35), ox=1, oy=1)
add('hud/chip-channel', kit_extra.chip_channel)
add('hud/keys-strip', kit_extra.keys_strip, ox=1, oy=1)
add('hud/lesson-note', kit_extra.lesson_note, kind='three-v', t=kit_extra.LESSON_T, b=kit_extra.LESSON_B, ox=1, oy=1)
add('hud/notice-slip', kit_extra.hint_slip)
add('hud/feed-slip-3', kit_extra.feed_leaf_3, ox=1, oy=3)
# bar fills, 102 x 3, cut to length in game (UiTex.bar)
add('hud/hp-full-dawn', lambda: hp_fill(102))
add('hud/hp-lost', lambda: hp_fill(102, 'lost'))
add('hud/hp-full-dusk', kit_extra.hp_full_dusk)
mock('hud', 'key-chip', kind='three-h', l=3, r=3)
# feed glyphs inked on the vellum slip: seal ours, struck seal theirs, in each team's hue
add('hud/fgv-seal-dawn', MOCK['fgv-seal'])
add('hud/fgv-seal-x-dusk', MOCK['fgv-seal-x'])
add('hud/fgv-seal-dusk', lambda: feed_glyph_ink('fg-seal', 'them'))
add('hud/fgv-seal-x-dawn', lambda: feed_glyph_ink('fg-seal-x', 'us'))
for _n in ('fgv-skull', 'fgv-skull-grey', 'fgv-banner-down', 'fgv-banner-up', 'fgv-sun'):
    mock('hud', _n)
add('hud/fgv-moon', kit_extra.fgv_moon)
# LAST STAND (War Table port Task 6, hud_laststand): the stamp a group at its last stand carries in a chip's state slot and on the
# selected card, and the feed's glyph for its line. The plates stay as they are: it fights on, so its plate keeps the live look
add('hud/stamp-last-chip', hud_laststand.chip_stamp)
add('hud/stamp-last-card', hud_laststand.card_stamp)
add('hud/fgv-laststand', hud_laststand.fgv_laststand)
# the Iron Ledger's veteran pieces (War Table port Task 7, hud_veterans): a level-2 and a level-3 pip, the glint of an open Armoury
# buy on 4 frames, and the level-3 feed line's glyph in each hue, solid for our own line and struck for an enemy's
add('hud/pip-l2', hud_veterans.pip_l2)
add('hud/pip-l3', hud_veterans.pip_l3)
add('hud/pip-more', hud_veterans.pip_more)
add('hud/glint', hud_veterans.glint, kind='sheet', frames=hud_veterans.GLINT_N)
for _side, _hue in (('dawn', 'us'), ('dusk', 'them')):
    add('hud/fgv-levelup-' + _side, (lambda h: lambda: hud_veterans.fgv_levelup(h))(_hue))
    add('hud/fgv-levelup-x-' + _side, (lambda h: lambda: hud_veterans.fgv_levelup(h, True))(_hue))
# cards that carry text
add('hud/respawn-card', lambda: ledger_card(200, 28, 'lo', IRON, seed=16), ox=1, oy=1)

# ================================================================ table/
# the 1280 x 720 tables, one stretch of oak per War Table page from the page's own seed (kit_props.TABLE_SEEDS; Kort,
# 2026-10-18 late): home's is the round 4 mock's (seed 11), recruit's has its reserved seed; plank layout, knots and
# nail heads only, never a brass fitting (no butterfly key, no mending plate)
# UI Task 8b-2: each is exported on the 1920 x 1080 big-canvas table (kit_props.table_big), its 1280 x 720 middle the page's
# oak pixel for pixel, so the planks run to the window's edge on a GUI 1 canvas at 1080p
add('table/oak-table', lambda: kit_props.table_big('home'))
add('table/oak-table-recruit', lambda: kit_props.table_big('recruit'))
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
           'ink-skirmish', 'ink-campaign', 'field-card', 'tack', 'map-seal', 'quill',
           'btn-brass-240x40', 'prop-maproll'):
    mock('table', _n)
# the props at the kit's pixel scale, about 2.5 times the mock's (Kort, 2026-10-18 late): the candle's flame flickers
# on 4 frames, the quill twitches on 3, the glow is the candle's light on the oak, the glint crosses the Ride plate
add('table/prop-candle', kit_props.candle, kind='sheet', frames=4)
add('table/prop-inkwell', kit_props.inkwell, kind='sheet', frames=3)
add('table/prop-glow', kit_props.glow)
add('table/plate-glint', kit_props.plate_glint)
add('table/btn-iron-240x40', lambda: kit_extra.btn_iron(MOCK['btn-brass-240x40']()))

# the War Table pages of UI plan Task 7: each page's own oak (kit_props.TABLE_SEEDS), the sheets and notes they pin to
# it (kit_extra.SHEETS), the commander classes' icons for the Banner page's plaques, the Chapter's reward track as
# Kort approved it (kit_track, from work/ui-concepts/track), the open page's rail plaques, and a painted card for every
# map (kit_maps: Rivergate's is the mock's card with its seals lettered, the others painted from their layouts)
for _p in ('banner', 'contracts', 'chapter', 'friends', 'field'):
    add('table/oak-table-' + _p, (lambda p: lambda: kit_props.table_big(p))(_p))
for _n, (_w, _h, _s) in kit_extra.SHEETS.items():
    add(_n, (lambda w, h, s: lambda: kit_extra.pinned(w, h, s))(_w, _h, _s))
for _c, _paint in kit_extra.CLASS_ICONS.items():
    add('table/icon-class-' + _c, _paint)
# the six Phase 2 classes' icons (kit_icons): the plaque's icon slot was bare for them (WarTablePages.paintClass skips a missing icon)
for _c, _paint in kit_icons.CLASS_ICONS.items():
    add('table/icon-class-' + _c, _paint)
for _n, _paint in kit_track.TRACK.items():
    if _n.startswith('table/plaque-'):        # the open page's plaque slices as the plaque it stands in for
        add(_n, _paint, kind='nine', l=6, t=6, r=7, b=8)
    else:
        add(_n, _paint)
for _n, _paint in kit_maps.MAPS.items():
    add(_n, _paint)

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
# the Odds note over the letter: light parchment and two tacks (kit_extra; UI plan Task 6)
add('recruit/odds-note', kit_extra.odds_note)

# portraits are live models, never exported
assert not any('pt-' in n for n in MANIFEST)
