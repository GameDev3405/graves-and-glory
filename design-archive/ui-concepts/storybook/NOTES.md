# Storybook: notes

Five screens of the campaign's front end (story spec section 7.1 to 7.4), at the War Table v4 pixel detail.
Each PNG is 1920x1080, rendered from its own HTML at 960x540 GUI px and doubled nearest-neighbour; 03, 04 and 05
are composited over in-game stills:

    cd work/ui-concepts/storybook
    python sb_render.py            # all five; python sb_render.py 02 for one. Lint and contrast in sb-check.json

Rebuilt on 2026-10-19 over the in-game stills, with every ruling so far applied in one pass (listed under
"Rulings"). All five pass the kit lint (no clipped, covered, crossed, tight, edge or box-overlap issues), and
every text run measures 4.5:1 or better on the final PNG, the world under it included.

| Page | Words | Type sizes | Under it |
|---|---|---|---|
| storybook-01-closed | 14 | 13, 16, 24 | its own table stretch |
| storybook-02-spread | 101 | 13, 16, 24 | its own table stretch |
| storybook-03-panel-scene | 32 | 13, 16 | `stills/fall-03-mound-night.png` |
| storybook-04-stage-scene | 22 | 13, 16 | `stills/north-crossing-dusk.png` |
| storybook-05-march-card | 26 | 13, 16, 24 | `stills/training-field-day.png` |

Type: Marcellus 24 (the one focal title), Marcellus 16 (names and headings), 13 (Lato body and key caps,
Marcellus capitals for kinds and running heads). Nothing under 13 GUI px. Colour: oak, morocco, brass, vellum,
one blood red (wax and ribbon, and 03's rubricated initial by ruling 4).

## Scene images and portraits come from the game (Kort, 2026-10-18)
Every scene image in the storybook is real in-game footage: a set built and lit in the world and shot through
the game camera, set up however the moment needs (time, weather, fog, lanterns, posed actors, silhouettes,
props). Nothing a scene shows may be painted or traced from my own block scenes. The book, its leaves, the
vellum cards, the plates and the chrome stay UI kit work.

The staging agent's stills are in `stills/`: 1920x1080, HUD hidden, Iris with Complementary Reimagined at its
defaults, no third-party resource packs (`stills/STAGING.md` says how each set was built and shot). The
rebuild uses them as they are, with nothing painted on any frame:
- 03: `fall-03-mound-night.png`, the main frame (fov 40, looking up 7 degrees), whole. Of the three framings it
  has the climber largest against the moon, and the guard stands outside the caption strip (only the Order
  standard's pole comes near it). The frame has no mist (it could not be staged), and none is painted in
  (coordinator). The moon is the game's own square one; the round-moon option in `stills/options/` is not the
  shipped look and is not used.
- 04: `north-crossing-dusk.png`, the main frame (fov 50, time 12750). The fire sits above the card, and Maud and
  the fire clear the bars. Chosen over the later dusk (alt2), where the smoke hangs over the two soldiers, and
  over the wider alt1, where Maud and the fire are smaller.
- 05: `training-field-day.png`, the main frame (fov 60), framed for the card by the staging: the lantern posts
  in the left third, the soldiers in the right third (the nearest just clear of the card's right edge), the
  empty road under the card in the centre third. Alt1 (fov 65) is the same camera wider; alt2 is the first
  take, with a post in the centre third.
- 02, the read plate (Order on the Crossings): `plate-order-crossings-alt1.png` (fov 50, the soldiers larger),
  the 16:9 crop from (120, 60) to (1800, 1005), so the two soldiers frame the bridge deck at plate size.
- 02, the sealed plate (The North Crossing): `north-crossing-dusk.png`, the 16:9 crop from (420, 250) to
  (1500, 858), round the fire, so the standard shows left of the seal and Maud right of it under the tissue.
- Each plate picture is its crop brought down to the plate's 128 x 72 (Lanczos, then an unsharp mask of radius
  0.8, 60 percent, threshold 2, so the figures keep their edges), then given the plate's state: the read
  plate's sepia, the sealed plate's tissue and seal (`th_still` in sb_ui.py).
- 02, the battle plate keeps the War Table's map sheet with the mode glyph: the spec has a battle's thumbnail
  show its map and mode glyph, and the map is kit art, not a scene image (coordinator, 2026-10-18).

Over the world, the page is transparent where the world shows, and `sb_render.py` (its `BG` table) lays the
still under it at its own 1920x1080, under the kit's chrome doubled to scale 2.

Portraits are rendered from the live character model in the game, not hand-drawn. The pixel busts in the
mockups (Maud's miniature on 02 and 05, her bust on 04's card) are placeholders for those renders. The frames
round them, the moon and a resting companion's grey are kit work; the grey is applied to the live render:
v = 34 + 0.72 x (0.30 R + 0.59 G + 0.11 B), drawn as (v, v + 3, v + 8).

### What the stills stand in for (for the builder)
The stills were shot with stand-ins for looks the game does not have yet (`stills/STAGING.md`):
- Maud is the Dawn Sentinel model in iron with the House's gold `host` trim, helm off, and no tabard. Kort's
  approved company look puts her in the navy tabard (below), and her placeholder busts wear it, so on 04 the
  card's bust and the Maud by the fire disagree. Once her model wears the look Kort settles on, the north
  crossing is shot again (04's frame and 02's sealed plate) and her live render fills the frames.
- The company's soldiers are the Dawn Knight model in leather dyed navy (#242444) with iron helmets; there is
  no Hollow model yet.
- The Hollow standard is a vanilla blue banner with a red roundel (vanilla has no navy dye). The mark (below)
  shows the standard's cloth with a round hollow through it, and the company's colour is the tabard's navy; a
  standard that matches needs the mod's own banner art.
- The actors' poses come from a harness mixin that is not shipped (`StoryStillsPoseMixin`); Phase 4 needs its
  own way to pose actors.

## What each screen decides

### 01: the book, closed (`storybook-01-closed.png`)
- The March plaque opens the War Table with the book lying closed on its own stretch of the table. The book is
  the only thing the candle lights: a morocco binding near black, brass corners, the title in gilt, the Hollow
  mark (its pole centred) pressed into a red seal at the heart of a gilt medallion, the part's numeral and name
  in a cartouche.
- The cover reads "The Long March" and the cartouche "I / The Road North" (the copy's own title line).
- The wind is already at work in the still: the board's lower corner lifted with the first leaf under it, the
  candle flame laid over, dry leaves blown in, dust off the fore-edge, the ribbon lifting.
- One action, Enter "Open the book" (the wind opens it after a beat anyway); the rail keeps one plaque, War
  Table on Esc.
- Re-rendered at the rebuild for two things only: the centred mark on the seal, and the table stretch without
  its brass keys (planks, knots and nail heads only).

### 02: the open spread (`storybook-02-spread.png`)
- The book settles open at the current page, page 2 "The River Road", on its own stretch of the table, with
  the candle and the inkwell to its right so the light pools on the right leaf, where the next thing to do is.
- Left leaf: running head (numeral and chapter title), the page title as the one 24 px line, the passage under
  an illuminated drop capital (gilt T in a ruled diaper box), then the company: framed miniature, name, want.
  Page 2 has Maud alone (Wren joins on page 3), so the rows under hers are not drawn: nothing stands in for a
  companion who has not joined. The four-row layout is below, under "The company rows".
- The passage and the company row share one left edge (x 66), 6 px inside the leaf's ruled frame. The frame
  was widened at the rebuild (x 60 to 400 on the left leaf, mirrored on the right) so the longest want clears
  it.
- Right leaf: running head "Page 2", then the page's scenes in play order as plates tipped into the leaf:
  - read, Order on the Crossings: the in-game crossing by day, two of the company framing the bridge deck,
    faded toward sepia like an old print, a quill tick and "Read" (a click replays it);
  - next, The Training Field: a gilt frame, a gold glow round it that breathes, the battle's map at full
    strength with the mode glyph on a small vellum tab, and the one primary action, a riveted brass tab
    "Enter | Play";
  - sealed, The North Crossing: the in-game camp at dusk under a tissue guard with the Hollow seal pressed on
    it, name and line faded.
- Turning: the left leaf's lower corner is lifted (mouse), the rail's second plaque is "Back a page" on the
  left arrow key; the wheel turns too. The right leaf's corner lies flat and the deep block of leaves behind it
  is tied with a cord and a small seal: everything past the current page is sealed, as the spec's "thick sealed
  section at the back". Turning is back only: the current page is the last that opens (ruling 6).
- The ribbon lies sunk in the fold and comes out over the tail of the spine onto the oak.

### 03: a book panel playing (`storybook-03-panel-scene.png`)
- `c1_fall`, panel 3 of 5, over the in-game frame: the first king's mound under the full moon, the climber on
  its top against the moon with the horn at her mouth, the Tide below with their pale eyes, and in the near
  field the Order's guard with raised lanterns and the Order's white and gold standard. Title "The Fall" top
  left with five pips (panel 3 lit).
- The caption is typed on a strip of the book's own vellum laid over the foot of the panel, in the title face,
  with its first letter in wax red as a rubricated initial (ruling 4: rubrication is a book convention, kept as
  an accent; 6.96:1 on the vellum).
- The panel is at night: "Day three" counts the days of the Fall, not daylight (ruling 2).
- First reading: the skip key drawn greyed under a closed clasp, "First reading" (spec); on replay the clasp
  opens and Esc skips. Continue sits on the use key (the mouse glyph).

### 04: a stage scene playing (`storybook-04-stage-scene.png`)
- `c1_b1_after`, "The North Crossing", over the in-game frame: dusk on the east bank of the north crossing, the
  company's fire, Maud seated on a log reading the pact-copy, two of the company across the fire, the Hollow
  standard, the lit lanterns along the bridge and a sentry on it. Letterbox bars in morocco with a gilt line
  carry the title and the skip.
- The dialog card is the kit's vellum card: Maud's miniature at 60 GUI px (ruling 3; the spec's portrait
  texture is 32 x 32; the bust is a placeholder for the live model's render), name in the title face, a voice
  mark after it for a voiced line, the line typed in the body face, Continue on the use key when typed.
- Card line: `c1_m10`, set one sentence to a line ("Leave that with me. / It is nothing you need tonight. /
  Eat something, Third.") because it lands harder that way and still keeps to three lines (ruling 3).
- Maud holds the turncoat's pact-copy (the story spec's object and word).

### 05: the march card (`storybook-05-march-card.png`)
- After `c1_b1_before` the scene hands straight to its battle (spec: the player never returns to the table
  between them), so the card rises over the held last frame, between the letterbox bars: the in-game east road
  into the crossings in the morning, the company's soldiers marching in with swords drawn in the right third,
  the lantern posts in the left, the centre third left to the road under the card.
- The card: battle name as the 24 px title, the reason (spec verbatim), the map with map, mode and difficulty
  beside it, the company row (Maud alone, as page 2 has it; Wren joins on page 3), the brass "Enter | March"
  and "Esc | Back to the book".
- The card stays over the held world frame (ruling 5); the `table-sb-march` stretch built for the other option
  is unused.

### The company rows (for the builder)
The spread's left leaf has room for four rows. Page 2 draws one (Maud); page 7 onward fills all four. In GUI px
on the spread:
- Row n (0 to 3) is a 328x46 box at x 66 (the passage's left edge, 6 px inside the leaf's ruled frame), top
  272 + 52n (272, 324, 376, 428); the fourth ends at 474, inside the leaf's rule.
- In a row: the 46 px miniature frame at (0, 0) with the 40 px portrait (the live model's render) inset 3 px;
  the name in Marcellus 16, ink, at (52, 5); the want in Lato 13, sepia, at (52, 25), one line, never wrapped
  (276 px of measure; Maud's, the longest so far, is 277 px wide and ends 5 px short of the rule).
- A resting companion: the grey frame and the render greyed (the formula above), the small moon on the
  frame's top right corner at (37, -3), the name in faded ink (#6b5a3a) with "Resting" in Marcellus 13
  capitals after it at (100, 7), the want faded.
- Rows fill in the order the companions join, with no gap left for anyone not yet on the road.

On the march card (05) the company is one centred row: 46 px frames 70 px apart, the first at
x = 200 - (46n + 24(n - 1)) / 2 on the 400 px card (177 for one, 72 for four), top 216; each name in Lato 13
centred under its frame at top 266; a resting companion grey, with the moon at (38, -5) on its frame and the
name faded.

## The company's look (approved by Kort, 2026-10-18)
Kort has made the Hollow's navy tabard and the Hollow mark on its seal the company's look everywhere. Both stay
exactly as drawn.

### The navy tabard
- The company's tabard is the Templar's knee-length tabard with its white cross (unit identity), in the
  Hollow's navy, with the gilt belt as the unit has it. Maud wears it over her white plate; the Order squires of
  the company wear the same navy pair in their surcoat panel.
- Colours: navy #2a3747; navy shade #1d2633 (folds, the edges turned from the light, the lower skirt); the
  cross #f3efe4 on the model (#eeeadf in the retired block scenes); the belt #96783a, its buckle #c09a52, its ends
  #6e5527.
- As drawn on the model, the bust's coat, front, 8 x 12. It is the unit's own coat with its linen panel
  (P #cfc6b0, Q #b5ac96) swapped for the navy pair; on the model that panel is the unit's team panel (unit
  identity rule 2):

```
eFEEEEFe    F #eeeadf  E #d9d3c1  e #bcb5a2  f #9d9683   white plate
EPPWWPPE    P #2a3747  Q #1d2633                         the navy
EWWWWWWE    W #f3efe4                                    the cross
EPPWWPPE
EPPWWPPE
EQPWWPQE
eQPWWPQe
eQPWWPQe
JGGHGGGJ    J #6e5527  G #96783a  H #c09a52              the belt
eQPPPPQe
eQPPPPQe
fQQPPQQf
```

  Its right side (the model's own), top to bottom:
  `eEEF eEEP eEEW eEEP eEEP eEEQ eeEQ eeEQ JGGG eeEQ eeEQ feeQ`; the left side mirrors it; the back is plain
  plate.
- The retired block scenes drew the same tabard on Maud's block figure, the skirt falling past the knee
  (`TABARD_FRONT`, `LAP_TOP` and `LAP_FRONT` in sb_river.py, `TABARD_BACK`, `TABARD_SIDE` and `SKIRT_BACK` in
  sb_scenes.py), and the squires' panel (`sq_coat("mn")` in sb_river.py), for a modeller who wants the side
  and back views. The stills do not show the tabard yet ("What the stills stand in for").

### The Hollow mark and its seal
The mark is the company standard as the game hangs a banner: a finial over a crossbar, the cloth hanging from
the bar with a round hollow through it (the empty place where a crown goes), a swallowtail, the pole below.
The pole is centred (coordinator, 2026-10-18), so the mark is symmetric about its middle column. 15 x 20 px,
`x` is the mark (`HOLLOW` in sb_book.py):

```
.......x.......
......xxx......
xxxxxxxxxxxxxxx
.xxxxxxxxxxxxx.
.xxxxxxxxxxxxx.
.xxxxx...xxxxx.
.xxxx.....xxxx.
.xxxx.....xxxx.
.xxxx.....xxxx.
.xxxxx...xxxxx.
.xxxxxxxxxxxxx.
.xxxxxxxxxxxxx.
.xxxxxxxxxxxxx.
.xxxxxx.xxxxxx.
.xxxxx.x.xxxxx.
.xxxx..x..xxxx.
.xxx...x...xxx.
.xx....x....xx.
.......x.......
......xxx......
```

On the small seal (radius 14) the mark is cut down to 11 x 11 (`HOLLOW_SMALL` in sb_book.py; coordinator,
2026-10-18), so that all of it and its cast shadow sit inside the sunk field and nothing reaches the beaded
border: the finial one pixel, the crossbar, one row of cloth over the hollow, the hollow three rows, one row
under it, the notch, two rows of tails, the pole's foot:

```
.....x.....
xxxxxxxxxxx
.xxxxxxxxx.
.xxx...xxx.
.xx.....xx.
.xxx...xxx.
.xxxxxxxxx.
.xxxx.xxxx.
.xxx.x.xxx.
.xx..x..xx.
....xxx....
```

The mark is pressed into a red wax seal (`wax_seal` in work/ui-concepts/v4a_paper.py), lit from the top left.
Sizes are sprite pixels at the kit's scale (GUI px, doubled on screen).
- Wax, dark to light: #2e0b08 outline; #4a120c deep shadow; #6a1b12 shadow; #8a2b1f the sunk field;
  #a83c2a the lit wax and the mark's face; #c45a42 lit edges; #e08d70 glints.
- Outline: a disc of radius r0 with a wobbling edge, R(a) = r0 + 0.7 sin(3a + p1) + 0.5 sin(5a + p2) (p1 and
  p2 from the seed), drips running out from the edge (an angle in radians and a length each: 3 px wide for the
  length, then 1 px for two more), a 1 px outline in #2e0b08, and a soft shadow #0a0505 at (+1, +2) at 45% and
  (+2, +3) at 20%.
- From the outside in, with ri = r0 - 5.2:
  - the poured lip, the outer 1.5 px: #a83c2a where it faces the light, #8a2b1f, #6a1b12, then #4a120c where
    it faces away;
  - the torus the stamp squeezed out, from ri + 2.3 to the lip: its slopes #c45a42, #a83c2a, #8a2b1f and
    #6a1b12 by how they face the light, with a little noise, its crest #e08d70 where the light catches it;
  - the stamp's beaded border, from ri + 1.1 to ri + 2.3: beads and gaps alternating every 2.6 px round the
    circle at ri + 1.7, the beads #c45a42 (#a83c2a on the side away from the light), the gaps #6a1b12;
  - the field's inner wall, ri to ri + 1.1: #4a120c on the top left side, #a83c2a on the bottom right, #6a1b12
    between;
  - the field, inside ri: #8a2b1f, with #6a1b12 where (5x + 3y) mod 13 = 0.
- The mark stands proud of the field, centred on the seal: its face #a83c2a; its edges #c45a42 where open to
  the left or above, #8a2b1f where open to the right or below; a 1 px cast shadow #4a120c right, below and
  diagonal; one glint #e08d70 on its top left pixel.
- Finish: a five-pixel glint #e08d70 on the crest at (-0.6 r0, -0.6 r0) from the centre; up to four bubbles
  in the lip (a #4a120c pit under a #c45a42 rim); a #c45a42 pixel up and left of each drip's tip.
- Where it is used:
  - 01, the cover: r0 20, seed 11, drips (1.9, 4), (2.5, 2), (0.9, 2), with `HOLLOW`; `art/seal-hollow.png`,
    51 x 52 with its shadow. The whole mark sits inside the field: 10.6 px from the seal's centre at most, 11.3
    with its cast shadow, inside ri = 14.8. The seal sits at the heart of the cover's gilt medallion: gilt rings
    at radius 40 and 36, 36 dots at 45, and rays from 49 to 52 on every other step with single dots at 49
    between.
  - 02, the sealed plate: r0 14, seed 13, drips (1.9, 3), (0.8, 2), with `HOLLOW_SMALL`; 39 x 40 with its
    shadow, pressed on the tissue over the plate (in `art/plate-sealed.png`). The mark reaches 6.4 px from the
    centre, 7.2 with its cast shadow, inside ri = 8.8; the beaded border starts at 9.9.
  - The small seal on the cord round 02's back block is not the mark: r0 5 with a plain cross.
- For the exact pixels: `wax_seal(20, sb_book.HOLLOW, seed=11, drips=...)` and
  `wax_seal(14, sb_book.HOLLOW_SMALL, seed=13, drips=...)` give them again, or take `art/seal-hollow.png` and
  the seal in `art/plate-sealed.png` as they are.

## Rulings on round 1's questions (controller, 2026-10-18)
1. Page 2 is Maud alone (Wren joins on page 3): 02 and 05 are redrawn so. The four-row layout stays above for
   the builder; page 7 onward fills it.
2. 03's night panel stays: "Day three" counts the days of the Fall, not daylight.
3. 04's 60 px miniature and its one sentence a line (within three lines) both stand.
4. 03's red initial stays: rubrication is a book convention, and it stays an accent.
5. 05 stays over the held world frame, not on the table.
6. Turning is back only: the current page is the last that opens; the forward block stays tied and sealed.
7. All UI labels are approved as written, and "Sealed until the battle is won." is approved as UI copy.
8. The game's own renderer is the target, not my light and water. The stage shots in this round (04's set,
   05's held frame, 02's plate stills) are not promises about lighting or water. From the next round, any
   stage mockup uses a harness screenshot of the real map where one exists. Kort has since widened this (the
   rule at the top): every scene image is in-game footage, shot for the moment, with no placeholder kept.
   Since the rebuild of 2026-10-19 the shots are the game's own frames.
9. Ashka's portrait matches her unit model as the story spec's cast table gives it (Hide Hunter, on the
   boar-kin build); corrected this round. The Hollow's navy tabard and the seal mark went to Kort.
- The recruit paper is the Pact in every label and note; the turncoat's paper in 04's set is the pact-copy
  (the story spec's word).

Later the same day (all applied in the rebuild of 2026-10-19):
- Kort: every scene image is in-game footage, and portraits are rendered from the live character model (the
  rule at the top).
- Kort: the Hollow's navy tabard and the Hollow mark are the company's look everywhere, exactly as drawn (see
  "The company's look").
- Coordinator: 02's battle plate keeps the War Table's map sheet.
- Coordinator: the mark's pole is centred so the mark is symmetric (art and notes), and the plate's radius-14
  seal keeps the whole mark inside its field, with nothing in the beaded border, by an 11-row version of the
  mark. 01 is re-rendered for its seal.
- Kort: no brass butterfly keys on any table background. The coordinator adds no brass fittings of any kind
  set into the plank surface (no plates, straps or tacks either), so plank layout, knots and nail heads only.
  The kit's chrome (the rail with its inlay and rosettes, plaques, frames, seals) stays. See "Table rules".

2026-10-19, coordinator: the stills are in; rebuild everything ruled in one pass. 03 goes over the Fall frame
with no mist painted in, the caption strip and the title as before; 04 over the north crossing with the
letterbox and Maud's card; 05 over the training field with the card in the centre third; 02's two stage plates
are cropped from the north crossing and the order-on-the-crossings stills, and the battle plate keeps the map.

## Self-grades (rubric of 2026-10-17, half points)

Graded on the rebuild of 2026-10-19: the game's own frames under 03 to 05 and in 02's two stage plates, the
portraits still placeholders.

| Criterion | 01 | 02 | 03 | 04 | 05 |
|---|---|---|---|---|---|
| 1 Legibility | 1 | 1 | 1 | 1 | 1 |
| 2 Information budget | 1 | 1 | 1 | 1 | 1 |
| 3 Focal point | 1 | 1 | 1 | 1 | 1 |
| 4 Hierarchy | 1 | 1 | 1 | 1 | 1 |
| 5 Spacing and alignment | 1 | 1 | 1 | 1 | 1 |
| 6 Colour discipline | 1 | 1 | 1 | 1 | 1 |
| 7 Readable drama | 0.5 | 1 | 1 | 1 | 0.75 |
| 8 Feel | 1 | 1 | 1 | 1 | 1 |
| 9 Honesty | 1 | 0.75 | 1 | 0.5 | 0.75 |
| 10 Flow | 1 | 1 | 1 | 1 | 1 |
| **Total** | **9.5** | **9.75** | **10** | **9.5** | **9.5** |
| Mine, before the stills | 9.5 | 9.5 | 9.5 | 9.5 | 9.5 |
| Controller, round 1 | 9.5 | 8.5 | 9.5 | 9 | 8.5 |

What moved since my last grades:
- Honesty. 03's panel, 04's set, 05's held frame and 02's two stage plates are the game's own frames now, so 03
  goes to 1. What is left is the portraits: they are placeholders for the live render, and they wear the
  approved navy tabard while the Maud in the game's frames wears iron and gold trim with no tabard. 02 and 05
  stay at 0.75; 04 goes down to 0.5, because there the card's bust and the Maud by the fire share the screen
  and plainly differ.
- Spacing on 02. The company row stood 2 px inside the left leaf's ruled frame where the passage stood 6 px
  in, and the want ran to 1.5 px short of the right rule. The leaf's ruled frame is widened (x 60 to 400,
  mirrored on the right leaf) and the passage moved to the row's edge (x 66), so both stand 6 px inside the
  rule and the want ends 5 px short of it: 1.
- Colour on 03. The red initial is the one accent ruling 4 keeps, and it measures 6.96:1 on the vellum: 1.

Weakest criteria across the set:
- **Honesty** (02 0.75, 04 0.5, 05 0.75). The portraits show a Maud the game does not render yet (see "What the
  stills stand in for"). It is settled by Kort's word on Maud's look, then her live render in the frames, and a
  re-shot north crossing if her model changes.
- **Readable drama** (01 0.5, 05 0.75). Both are still moments whose drama is mostly in motion: the wind opening
  the book, the card rising over the held frame. 05's frame helps now (the company marching in, swords drawn),
  but the stakes still come from the card's words.

## Copy

Verbatim from the copy (`docs/copy/chapter-1.md`) and the specs:
- 01: "The Long March", "I", "The Road North" (the copy's title line).
- 02: page title "The River Road"; the leaf passage (52 words; the copy's heading says 54); Maud's want from
  the company table; scene names "Order on the Crossings", "The Training Field", "The North Crossing".
- 03: "The Fall"; caption `c1_r03`.
- 04: "The North Crossing"; Maud's `c1_m10`.
- 05: "The Training Field"; the reason "The road west must open before the march begins." (spec); "Rivergate
  Valley", "Domination", "Easy" from the copy's battle 1 facts.
- "first reading" is the spec's phrase for the greyed skip.

Approved as UI copy (it was a placeholder in the Record's voice; ruling 7):
- 02: "Sealed until the battle is won." (under a sealed scene).

UI labels, approved as written (ruling 7): "Open the book" (01); "Back a page", "Page 2", "Read", "Play" (02),
and "Resting" for a resting companion in the company rows (not drawn on page 2); "Continue", "Skip" (03, 04);
"The company", "March", "Back to the book" (05).

## Motion (for the builder)
- 01: a beat, then the wind lifts the cover and three or four leaves turn (spec), flame leaning, leaves
  tumbling in, the ribbon lifting; Enter opens at once.
- 02: the book settles on the current page; the next plate's glow breathes (two steps of the stepped glow, 2 s
  up, 2 s down); the candle flame flickers in still air; the lifted corner rises a pixel under the mouse;
  a turn is three or four frames of a leaf.
- 03: a slow drift across the panel (a camera move shot in the game, or a slow pan over the frame); the
  caption types two characters a tick with a soft quill sound; the continue glyph appears when the line is
  typed; the use key hurries typing.
- 04: the bars slide in, the card rises; typing as 03; the voice mark only on voiced lines.
- 05: the scene's last frame holds; the card rises over it.

## Table rules (coordinator, 2026-10-18)
- Props are drawn at the kit's 2x pixel scale (about 2.5 times the home screen's): candle and chamberstick,
  inkwell and quill, dry leaves. The candle lights the oak (a stepped warm pool) and the book (a warm wash that
  falls off from the flame), and throws each object's shadow away from the flame in dithered steps; the
  candle's own shadow comes from the window light.
- Each screen on the table sits on its own stretch: 01 `table-sb-closed`, 02 `table-sb-open` (its own planks,
  knots and nails); `table-sb-march` is unused (05 stays over the world, ruling 5).
- No brass on the table wood (Kort, 2026-10-18, with the coordinator's addition). The brass butterfly key, the
  bowtie-shaped plank joint, is banned from every table background, and so is every other brass fitting set
  into the plank surface: keys, plates, straps, tacks. Plank layout, knots and nail heads (the kit's iron heads,
  rust bleeding into the oak) carry the variety, and nothing else.
  - Applied at the rebuild: the keys (two on each stretch) and the domed tacks (on 02's stretch and the march
    stretch) are gone, with nothing brass in their place. `bowtie`, `brass_tacks`, `mending_plate` and
    `seam_rows` are deleted from sb_table.py; a stretch is now the kit's `oak_table` with its own seed and the
    rectangles its knots and joints keep clear of (type on bare oak, the props' feet). 01 and 02 are
    re-rendered, and no pixel of the three stretches is in the brass palette.
  - The rule is about the planks, not the kit's chrome (coordinator, 2026-10-18). The back rail with its
    cable inlay and rosette studs (the War Table v4 kit's `rail`), the plaques, frames and seals all stay as
    they are.
- 02's light pass takes the inkwell where the page puts it, (798, 338). It had been worked out at (812, 338),
  which set the bottle's shadow on the oak 14 px to the right of the bottle; fixed at the rebuild.

## Where the art comes from
- `stills/`: the staging agent's in-game frames, with `STAGING.md` (how each set was built and shot). They are
  the backgrounds of 03, 04 and 05 and the sources of 02's two stage plates; nothing is painted on them.
- `sb_render.py`: the render, with `BG`, the table of what lies under each page.
- `sb_table.py`: table stretches, rail, candle (wind and calm), inkwell, glow, the light and shadow passes.
- `sb_book.py`: the closed book and its seal, the open book (vellum leaves and their ruled frames, page blocks,
  fold, curl, sealed cord), and the two Hollow marks (`HOLLOW`, `HOLLOW_SMALL`).
- `sb_props.py`: plaques, key caps, ribbons, leaves, motes.
- `sb_ui.py`: caption strip, tags, cards, miniature frames, bars, plates and their glow (the stage plates'
  pictures by `th_still` from the stills, the battle plate's from the War Table's map), drop capital, tick,
  brass tab, glyphs.
- `sb_portraits.py`: placeholder busts for the mockups only (the game renders each portrait from the live
  character model). They are drawn from block models in the troop types' looks from the unit identity spec, so
  the placeholders do not mislead: Ashka is the Hide Hunter on its boar-kin build (`ArmyBoarkin`), with snout
  and tusks, ears hanging from the crown, russet fur, leather cap, clay stripes on the brow and the
  three-feather fan on the left of her head, framed Tall +4. Wren's, Dunmar's and Ashka's wait for the pages
  they join.
- Retired: `sb_river.py` with `sb_world.py` and `sb_scenes.py` drew my block-scene placeholders. No page uses
  them, each says so at its top, and their outputs (`panel-kings`, `stage-crossing`, `stage-crossings-day`,
  `th-day`, `th-dusk`) are moved to `art/retired/`.
