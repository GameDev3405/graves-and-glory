# Design rules, second pass

960x540 at GUI scale 2, rendered 1920x1080 nearest neighbour. Tokens: `common.css`, v2 block.

## Type: three sizes
- **16** Marcellus: one per screen, the focal name.
- **11** Lato body (two lines at most); Marcellus 11 for headings and names.
- **8** Minecraft 1x: numbers, labels, chips, keycaps.

Colour and weight carry hierarchy, not extra sizes or boxes.

## Grid
- 4 px unit; panels pad 8, chips and plates pad 4; gap 8 within a group, 16 between groups.
- Nothing within 12 px of the screen edge.
- Containers fit measured text; long names truncate with "...".

## Colour roles
- Surfaces: charcoal, iron, vellum, oak, smoked glass.
- On dark: ivory primary, muted `#afb9bc` secondary, brass `#c8a56b` headings and selection.
- On vellum: ink `#2a2219`, sepia `#5c4a32`.
- House and team colours are accents only.
- Alarm red always comes with a shape or a word.
- Text contrast 4.5:1 or better, measured on the PNG.

## Information budget
**HUD.** Always: score, timer, point seals, groups (name, count, health, one state word, order and
formation glyphs), ability, one notice. On select: order and formation words, point names. One click
away: map (M), commands and gold (R).

**Home.** Always: three fight choices, chosen field and rules, party, one primary action. On select:
field list. One click away: recruit, banner, contracts, chapter, friends (badges are counts).

**Recruit.** Always: House tabs, reveal card, roster (portrait, name, stars), enlist with cost, gold,
Epic counter. On select: detail pane. One click away: odds, banner builder.

Keys sit inside the button they trigger. In battle, key hints (H for the ability too) show only
while Alt is held or in the first minute. No fact appears twice on a screen.

## Third pass amendments (v3)
- HUD: order and formation words always sit beside their glyphs (in v2 they appeared only on select).
- HUD: the selected group card carries its order keys (G Attack, V Follow, B Formation). Other battle key
  hints still show only while Alt is held or in the first minute. The ability's key sits on its slot.
- HUD: no hunger row, and nothing replaces it yet. Vanilla hearts, XP bar and hotbar stay as they are.
- Portraits: every frame is a window for a live model render (drawEntity, scissored; the rim overlaps the
  window by 1 px). The pixel busts inside are placeholders until the unit identity pass.
- Textures are pixel art painted at GUI scale 1 (`v3art.py`). Light comes from the top left, ramps are
  hue-shifted, and there is no pillow shading. Text stays live.

## Kort, 2026-10-18 late: table props and wood variety

- The candle and the ink pot read too small against everything else on every table screen. Scale table props (candle, ink pot and quill, and any others) to the kit's 2x pixel scale of the plaques and cards, about 2.5 times their round 4 size, and let them cast light and shadow on the wood as the mockup's candle does.
- Wood placement varies per screen: the plank layout, knots, nail heads and brass fittings of the table background are not the same image on every page. Each War Table page (home, recruit, banner, contracts, chapter, friends, field, the storybook) gets its own plank arrangement and prop placement from the same kit, so flipping pages feels like moving along the table, not reloading a backdrop.

## Kort, 2026-10-18 late: scenes are in-game footage

- Every image a storybook scene shows (the Fall's panels, stage scenes, the thumbnails on the page, the held frame under a march card) is real in-game footage: a set built in the world, timed, lit and dressed however the moment needs (time of day, weather, fog, lanterns, posed actors, silhouettes, props), shot through the game camera with the HUD hidden. Nothing a scene shows is painted or traced from a block scene outside the game.
- The book, leaves, vellum cards, plates, plaques and chrome stay UI kit pixel art. Portraits are rendered from the live model (controller's call), as the War Table frame renders the commander.
- A mockup's world shot is a harness screenshot of a staged set; where no map or set exists yet the mockup says so and is a placeholder until a frame replaces it.

## Kort, 2026-10-18 late: the Hollow's look

- The company's colours are navy (the tabard) and its mark is the seal from the storybook mockups (red wax on the cover and cards). Use them wherever the Hollow is shown: the standard, the book, the cards, the War Table.
- Portraits are rendered from the live character model everywhere; no hand-drawn portraits.

## Kort, 2026-10-18 late: no butterfly keys

- The brass butterfly key (the bowtie-shaped plank joint) is banned from every table background. Wood variety comes from plank layout, knots and nail heads only; see the no-brass rule below.

## Kort, 2026-10-18 late: no brass mending plates

- The brass mending plate (the small screwed plate across a seam) is banned with the butterfly key. Table wood varies by plank layout, knots and nail heads only; no brass fittings of any kind.
