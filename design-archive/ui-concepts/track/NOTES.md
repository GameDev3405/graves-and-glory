# The Chapter plaque, round 2, approved (the Founders reward track)

Three states of one screen, 1920x1080, rendered from their own HTML through `render.py --check` (lint clean,
no text run under 4.5:1). Same rail, plaques, plates and letter as the accepted War Table home (v4.2) and
recruit (v4.1), on this page's own stretch of the table. Kort approved round 2 with one change: no table
props on this page. After that he ruled brass fittings off table wood (both under "On approval" below).

| PNG | State |
|---|---|
| `track-01-sworn.png` | Sworn to the Gilded Order at tier 11 (2,870 XP): tier 11 to claim on both rows, the hero is tier 20's Rare (Order Longbow). |
| `track-02-unsworn.png` | The same player sworn to no House: the Oath row in sepia under "not sworn", and "Swear to a House at the recruit table". |
| `track-03-claim.png` | The moment after Claim: tier 11 stamped wet on both rows, the Claim plate dull, the rod slid one plaque, the hero moved on to tier 21's Cloth of Gold. |

## On approval

Kort approved round 2 with one change: the candle and the ink pot and quill come off this page (they are kept
for other pages). The three PNGs are re-rendered without them and without the candle's light on the oak; the
page keeps its own wood.

**Rule (Kort, after approval): no brass fittings on table wood.** Table wood is plank layout, knots and nail
heads only, on every War Table page. Kort first banned the brass butterfly key (the bowtie-shaped key let in
across a plank seam), then the brass mending plate (the small plate screwed across a seam). Round 2 had one
of each on this page: the key in the seam between the hero and the rod, the plate across the seam under the
track. Both are gone from this page's wood and from the code (`butterfly()`, `mending_plate()` and `seams()`
deleted from `trackart4.py`), and the three PNGs are re-rendered without them. The wood itself is unchanged:
the kit's oak with this page's seed, its planks, knots, butt joints and nail heads.

The track stays where it is. The props stood in the margins and in the band under the track, outside the
56..904 grid, so they freed no width inside it: the row names start at 56, the plaques run 136..888 and the
rod's bosses end at 904, the right edge the chapter block and the Claim plate share. Wider plaques would carry
the rows past 904 (ten 72 px plaques with 8 px gaps end at 928), and any width between 68 and 72 leaves the
4 px grid. Ten tiers at 68 px with 8 px gaps already hold, and the slide after a claim needs ten. The Oath
item names stay as proposed.

## Round 2: what changed

Round 1 was graded 8.5 by the controller. The fixes:

1. **The pin carries no number.** The current tier is marked on the rod by a plain drop of red wax (`pin-tier`),
   pressed over the rod and run down toward the plaques. The chapter's seal is the one place the number shows.
2. **Plaques never touch.** 68x80 on a 76 pitch: 8 px between every plaque, the design rules' gap within a
   group. The current pair keeps that width and stands out by the lift (the Roll's 8 px, the Oath's 4), 8 px
   more height (68x88), polished brass and a deeper shadow, so it clears its neighbours by the same 8 px. Ten
   tiers still fit, which the hero rule's slide needs (track-03 shows 11 to 20, the hero is 21).
3. **The Oath row fills its length.** A plaque under every tier on screen; both rows end at 888, under the
   rod's right boss.
4. **The Oath row's rewards are the House's, for the player's own commander and banner**, never a troop's
   (veteran livery and brass frames per troop stay with mastery, Houses spec 4.7). Table below.

Rulings applied: hero rule H1; the Oath beside mastery, and swearing late makes Oath tiers at or below the
current tier claimable at once; "livery"; the Hollow's mark as the claimed stamp; the Founders Chevron is
brass and ivory (`pennant-160` recoloured: ivory linen, brass chevrons, gold thread); shards go to the sworn
House and an unsworn player picks the House at claim, so the Roll's shards read "10" for both (gold crystals
when sworn, steel when not) and the Oath adds "+10"; the Rare is picked at claim and "Change" stays as the
preview.

Kort's two table rules (DESIGN-RULES, 2026-10-18 late):

- **Props at the kit's pixel scale, 2.5 times round 4**, painted at that size, not scaled up, and shown in
  round 2 at the right end of the track and under the row names. Kort took them off this page on approval;
  the sprites stay in `trackart5.py` and `track/art` for the pages that keep props (`prop-candle-2x`,
  `prop-glow-2x`, `prop-inkwell-2x`). A brass chamberstick, 112x154 GUI px against the kit's 44x54: finger
  ring and thumb rest, the rolled rim lit at the back left and catching the flame at the front, the floor
  turned in rings, the stick's shadow across it, the socket and its lip, the candle with a melted crater, wax
  runs, a run down the socket to a puddle, the wick bent in the flame, the flame (orange tip, yellow body,
  white core, blue at the wick) and its halo. Its light is a stepped warm pool (353x281) to lay on the oak
  round it; its shadow falls under the dish, down and right like every shadow on the table. An ink pot of
  dark green glass with a goose quill, 72x150: the thick foot, the ink line seen through the glass, a cold
  window sliver on the left, a candle caught small and warm on its right, a dribble of ink down the
  shoulder, the quill bowed, its vane split in two places, the shaft stained where it dips.
- **This page's own table.** The kit's `oak_table` with its own seed (53; home and recruit use 11), so its
  planks, knots, joints and nail heads are its own. Nothing brass is let into it. (Round 2 had a butterfly key
  in the seam between the hero and the rod and a mending plate across the seam under the track; Kort ruled
  brass fittings off table wood, see "On approval".)

## Layout

- **Rail.** "War table" back plaque with its Esc key, then Recruit, Banner, Contracts (2), Chapter (the open
  page, its brass polished a step, its label pale brass), Friends (1), in home's places.
- **Hero, top left.** The next big reward on a letter, as recruit shows a troop: the live window in the ornate
  frame, a wax seal for its kind on the letter's top edge (a lozenge for a Rare, the crowned sword for a House
  item), a Minecraft kicker ("Tier 20"), the name in the one 16 px line, the kind in sepia, one line of copy.
- **Chapter, top right.** "Chapter 1" / "The Founders"; no end date (the Founders never close). The tier on a
  wax seal, the XP as brass poured along a rail engraved with the 30 tier marks, the numbers under it, the
  Claim plate where recruit keeps Enlist (664..904).
- **Track, below.** Ten tiers hang on chains from a brass rod with scroll bosses at both ends. Numbers on brass
  sleeves, the current tier under the red wax pin. The Roll (everyone) above, the Oath (sworn) hung under the
  Roll plaque of the same tier. Row names on the left; one label line across each row.

## The Oath row (here for the Gilded Order)

| Tier | Oath reward | Label |
|---|---|---|
| 2, 4, 6, 8, 12, 14, 16, 18, 22, 24, 26, 28 | Shards: another 10 on the Roll's 10 | Shards / +10 |
| 10 | Portrait frame: the House's gilt frame, crowned, for the player's own portrait | Portrait / frame |
| 11 | Tabard: the commander's livery, ivory with a gilt border and the crowned sword | Tabard / livery |
| 13, 17, 19 | Pennant patterns: Pale, Bend, Cross (ivory on the House's gold) | Pennant / pattern |
| 15 | Banner cloth: Order white (ivory, the sword and border in gold) | Banner / cloth |
| 20 | Cloak: the commander's livery, the Order's gold, hooded | Cloak / livery |
| 21 | Banner cloth: Cloth of Gold (gold, the sword and border in ivory) | Banner / cloth |
| 30 | Title "Oathsworn" | Oathsworn / title |
| 1, 3, 5, 7, 9, 23, 27 | Pennant patterns (names proposed): Fess, Chief, Bordure, Saltire, Quarterly, Gyronny, Pily | Pennant / pattern |
| 25, 29 | Banner cloths (names proposed): Crowned field, Night cloth | Banner / cloth |

Every House gets the same slots in its own colours and device. The milestone tiers (10, 20, 30) carry the
big pieces, so the Oath never sits empty under a Roll milestone.

## Decisions

1. **Hero rule H1 (accepted).** The hero shows the next big reward just past the track's right edge, so it
   never repeats a plaque, and the track slides one plaque after a claim. When a tier has two big rewards the
   hero takes, in order: a Rare troop, the Oath's House item (sworn players), the Roll's cosmetic, a title.
   Tier 20 shows the Longbow over the cloak; tier 21 shows the Cloth of Gold to a sworn player and the
   Founders Chevron (`pennant-160`, `seal-chevron`) to an unsworn one.
2. **States are shapes.** Claimed: the Hollow's mark pressed in red wax on the plaque's corner, wet with a drip
   in the moment after Claim. Current: the pair hangs lifted and taller, polished, under the rod's pin. Not
   reached: tarnished plaque and chains under an iron hasp. Not sworn: the Oath row in sepia, no hasps, no
   stamps, the row named "not sworn".
3. **Numbers in Minecraft.** Marcellus has old-style numerals, so every number is Minecraft 8 on its own line.
4. **Swearing late.** Oath plaques at or below the current tier turn from sepia to lit and wait to be claimed;
   one Claim takes them all with the current tier.

## Copy (ASCII, as rendered)

- Hero, tier 20: "Tier 20" / "Order Longbow" / "Rare ranged, your pick" / "One arrow, two men. The Order wastes
  nothing." / "Change".
- Hero, tier 21: "Tier 21" / "Cloth of Gold" / "Banner cloth, Gilded Order" / "Your banner in the Order's gold.
  Only the sworn fly it."
- Chapter: "Chapter 1" / "The Founders" / "11" / "2,870 / 7,500 XP" / "130 XP to tier 12" / "Enter" "Claim".
- Track: "Tier"; "The Roll" / "for all"; "The Oath" / "Gilded Order" (sworn) or "not sworn". Roll plaques:
  "Gold" / "30", "Shards" / "10", "Pathfinder" / "title", "Captain" / "title", "Rare troop" / "your pick".
  Oath plaques: "Portrait" / "frame", "Tabard" / "livery", "Shards" / "+10", "Pennant" / "pattern", "Banner" /
  "cloth", "Cloak" / "livery".
- Unsworn: "Swear to a House at the recruit table".
- Rail: "War table" "Esc", "Recruit", "Banner", "Contracts" "2", "Chapter", "Friends" "1".

## Grades (Kort's rubric, one point each)

Round 1: self 9.5 on each screen; controller 8.5 (tier twice, plaques touching, the Oath row empty on the
right, Oath rewards duplicating mastery). Round 2, self:

| Criterion | 01 sworn | 02 unsworn | 03 claim | Note |
|---|---|---|---|---|
| 1 Legibility | 1 | 1 | 1 | Lint clean; every run 4.5:1 or better; smallest text Minecraft 8 (16 px on the PNG). |
| 2 Information budget | 1 | 1 | 1 | The tier number once (the seal); the pin only marks; one reward per plaque in two lines. |
| 3 Focal point | 1 | 1 | 1 | The hero letter first, then the lifted pair and the Claim plate. Second weakest: the 2.5x candle's flame is now the brightest spot on the page and pulls a little at the corner. |
| 4 Hierarchy | 1 | 1 | 1 | Sizes 8, 11, 16; brass headings, ivory names, muted secondary lines. |
| 5 Spacing and alignment | 0.5 | 0.5 | 0.5 | 8 px between all plaques, edges at 56 and 904, both rows end at 888, props 12 px inside the screen. Weakest: at 2.5x the props crowd the grid at the bottom corners (the candle's stick stands 12 px and its socket lip 7 px from the tier 19 Oath plaque, the quill 8 px from the row names). |
| 6 Colour discipline | 1 | 1 | 1 | Red only as wax; the House's gold only on what the House gives; every state has a shape. |
| 7 Readable drama | 1 | 1 | 1 | Stamp, lifted pair and pin, hasp and sepia read without text. |
| 8 Feel | 1 | 1 | 1 | The accepted table, brass and vellum, every sprite painted at GUI scale 1. |
| 9 Honesty | 1 | 1 | 1 | Spec numbers (4.8, 250 XP a tier); 9-slice plaques, sprites, a clipped fill, a live render window, Minecraft text. |
| 10 Flow | 1 | 1 | 1 | One primary action (Enter), the way back (Esc), the scroll bosses; 02 says how to open the row. |
| **Total** | **9.5** | **9.5** | **9.5** | |

Round 2 was approved by Kort. The two weakest points above (the props crowding the bottom corners, the
flame pulling at the corner) left the page with the props.

## Rebuild

From `work/ui-concepts`:

```
mkdir -p track/.tmp && export TMPDIR="$PWD/track/.tmp"
python track/build.py                 # every sprite into track/art (or name some: python track/build.py pin-tier)
python track/pages.py                 # the three pages from one description
python render.py --check track/v4-track-01-sworn.html bg/camp-v2.png track/track-01-sworn.png
python render.py --check track/v4-track-02-unsworn.html bg/camp-v2.png track/track-02-unsworn.png
python render.py --check track/v4-track-03-claim.html bg/camp-v2.png track/track-03-claim.png
rm -rf track/.tmp
```

- The pages are named `v4-...` because render.py gives greyscale text anti-aliasing only to `v2-`/`v3-`/`v4-` names.
- `TMPDIR` points at a short private folder: the session's temp path is longer than Chrome's path limit, and the
  folder keeps render.py's overlay apart from the storybook artist's renders.
- Sources, all importing the v4 kit (`v4lib`, `v4pal`, `v4a_*`, `v4art.oak_table`) and changing nothing in it:
  `trackart.py` (plaques, chains, rod, tags, arrows), `trackart2.py` (the Hollow's mark, the chapter seal, hasp,
  the Roll's icons), `trackart3.py` (hero letter, Longbow window, seals, Founders Chevron, XP rail, dull plate,
  open-page plaque), `trackart4.py` (this page's table, the pin, the Oath's icons), `trackart5.py`
  (the 2.5x candle, its light, the ink pot and quill for other pages; Cloth of Gold; the crowned-sword seal).
- `track/art` holds every sprite `build.py` makes. Eight are not on these three screens: `prop-candle-2x`,
  `prop-glow-2x` and `prop-inkwell-2x` (the table props, kept for other pages), `plaque-tier` and
  `stamp-hollow` (the odd-tier variants), `ic-o-cloak-sepia` (tier 20 for an unsworn player), `pennant-160` and
  `seal-chevron` (the unsworn hero at tier 21).

## Kort's answers (round 2)

1. The Oath item names stay as proposed.
2. No table props on this page: the candle and the ink pot and quill stand on other pages.
3. No brass butterfly key on any table page.
4. No brass fittings on table wood at all, so the mending plate goes too: plank layout, knots and nail heads
   only.
