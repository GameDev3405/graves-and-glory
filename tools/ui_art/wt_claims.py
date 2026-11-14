"""War Table port Task 1 (claims with a House): the House chip of the Contracts and Chapter pages' "for <House>" row.

A chip is a cut parchment tag like the War Table's vellum tags (v4a_table.vellum_button), 24 tall, drawn at any width from the
64 px nine-slice; the House's own seal (textures/gui/house/<id>.png) and its name are drawn over it by WarTablePages.
  table/chip-house     the idle chip: the vellum tag as it is, raised one pixel off the oak, its shadow right and below.
  table/chip-house-on  the chosen chip, set into the table instead of raised: a darker face, an inked edge all round, no
                       shadow, and a light rule under it (the PNG is 5 px taller than the 24 px box; the rule sits 2 px under
                       the tag), so the chosen House is told by its depth, its rule and its word, never by colour alone.
Registered through manifest.add when build_ui_art imports this module, after the manifest itself (one import line there).
The Roll's tier-reward icons (chapter/ic-shards, ic-pennant, ic-frame) are the Oath row's own painters, drawn in colour."""
from v4lib import Spr, rgba
from v4pal import VEL, INK
import v4a_table
import kit_track
import kit_gearshard
import manifest

W, H = 64, 24
RULE_GAP, RULE_ROWS = 2, 2   # the chosen chip's rule: 2 px under the tag, 2 px thick


def chip():
    """the idle chip: v4a_table's vellum tag, 64 x 24 (the PNG adds 1 px of shadow right and 2 below)"""
    return v4a_table.vellum_button(W, H)


def chip_on():
    """the chosen chip: inked edge, a face one step darker than the vellum tag with its light inside the top and left edges
    turned to shade, no drop shadow, and a 2 px light parchment rule under it, as wide as the tag less its corners, which reads on the dark oak where an ink rule would not"""
    s = Spr(W + 1, H + RULE_GAP + RULE_ROWS + 1)
    s.rect(0, 0, W, H, INK[2])                      # the inked edge, all round
    s.rect(1, 1, W - 2, H - 2, VEL[4])              # the face, darker than the idle tag's VEL[7]
    s.hline(1, W - 2, 1, VEL[2]); s.vline(1, 1, H - 2, VEL[2])          # set into the oak: shade inside the top and left
    s.hline(2, W - 2, 2, VEL[3]); s.vline(2, 2, H - 2, VEL[3])
    s.hline(1, W - 2, H - 2, VEL[5]); s.vline(W - 2, 2, H - 2, VEL[5])  # the light on the lower and right lip
    for (x, y) in ((0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)):
        s.set(x, y, None)
    s.hline(2, W - 3, H + RULE_GAP, VEL[8])          # the rule, under the tag: light parchment, so it reads on the dark oak
    s.hline(2, W - 3, H + RULE_GAP + 1, VEL[5])
    s.hline(3, W - 2, H + RULE_GAP + RULE_ROWS, rgba('#0b0605', 140))  # its shadow
    return s


manifest.add('table/chip-house', chip, kind='nine', l=4, t=4, r=5, b=6)
# the bottom band holds the tag's lower edge, the gap and the rule, so the rule runs the chip's whole width at any width
manifest.add('table/chip-house-on', chip_on, kind='nine', l=4, t=4, r=5, b=H + RULE_GAP + RULE_ROWS + 1 - (H - 4))


# The Chapter's Roll plaques carry the reward of the tier (Progression.reward), so the three rewards the Roll showed as coins
# get their own pictures: the Oath row's own art (kit_track.OATH_ART, bright where the Oath shows it in sepia).
manifest.add('chapter/ic-gear-shards', kit_gearshard.gear_shards)                                  # a gear tier: gear shards (review item 15)
manifest.add('chapter/ic-shards', lambda: kit_track.shard_cluster(kit_track.SHARD_GILDED))      # a shard tier: House shards
manifest.add('chapter/ic-pennant', lambda: kit_track.pennant('pale'))                            # tier 21: the pennant pattern
manifest.add('chapter/ic-frame', kit_track.house_frame)                                          # tier 29: the banner frame
