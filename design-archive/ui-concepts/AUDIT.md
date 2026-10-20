# Audit of the nine v1 mockups

Method: every v1 PNG opened at full size, then zoom-checked in crops (2x to 4x, nearest neighbour).
Coordinates are output pixels (1920x1080); "1x" means GUI pixels (960x540), the unit the rules use.
The layout lint (`check.js`, run by `python render.py --check`) was run on copies of the v1 pages for
the numbers in each header. v1 pages carry no panel markup, so for them the lint only counts edge hits,
crossings and type sizes; the defect lists below come from looking, not from the lint.
Contrast ratios were measured on the PNGs (text colour against the median background next to it).

## Systematic defects (they show up on several screens)

- **SYS-1 Colour fringes.** Chrome rendered every Lato and Marcellus run with LCD subpixel antialiasing.
  Doubled with nearest neighbour, every letter gets red and blue edges (1163 coloured fringe pixels
  in one sample label; 0 with greyscale antialiasing). v2 renders with `--disable-lcd-text`.
- **SYS-2 Glyphs pushed off their marks.** `art.js` forced `position:relative` on every glyph, so
  a glyph placed by its class slid right and down: House glyphs jam into tab corners (A, C), pennant
  glyphs sit off centre and the Skirmish swords are cut by the pennant edge (C home), map tokens float
  off their bases (A home). Fixed for v2 pages (`<html class="v2">`); v1 output is unchanged.
- **SYS-3 Background leftovers.** `fight-clean.png` still has the old score strip's bottom edge
  (y 120-133, x 435-1490), the vanilla chat backing as dark bars (y 859-899) and a patch seam at
  x 1572 that cuts a lantern in half. `camp-clean.png` has a brown and a moss smear (x 578-1350,
  y 937-975). v2 uses cleaned copies (`bg/fight-v2.png`, `bg/camp-v2.png`); the originals are untouched.
- **SYS-4 Too many type sizes.** 5 to 9 distinct sizes on each menu screen; Minecraft and Lato both
  used for body text, sometimes inside one card.
- **SYS-5 Data that disagrees with the spec or the code.**
  Templar shown as Epic, 2 slots (spec: Rare, 1 slot). Sunweaver shown as Rare (spec: Epic, 2 slots).
  Pool 60/28/10/2 with Legendary rolls and "Epic within 7" (spec: 86/12/2, Legendaries never rolled,
  the 40th roll without an Epic gives one). Star 2 = +4% (spec: +3% for Common and Rare).
  "Evolve to Knight / Longbow: 12 shards" (spec: the Archer evolves to Order Bowman; stage 2 costs
  10 shards and 100 gold for a Common). "Mastery 2: +2% mend" (mastery gives titles and skins only).
  Rivergate points named Old Village / Shattered Keep / Hilltop Shrine (code: Mill Hamlet,
  Trade Crossing, River Shrine) while the notices say Trade Crossing. Room code "K7F2" (codes are
  8 hex characters). (Correction: this list first called "H Rally Horn" an invented key. Kort has since confirmed
  "commander active on H" in docs/QUESTIONS-FOR-KORT.md, so H is the real ability key; what stays wrong is a hint
  that is always on.)
  Contracts "Hold Old Village 3 minutes", "Win with 8 Gilded" (code: Complete 1 battle,
  Help capture 1 objective, Issue 10 tactical orders, weekly Complete 6 battles).
- **SYS-6 Key-hint rows** sit 2 to 9 px (1x) from the bottom edge and repeat hints printed elsewhere
  on the same screen.

## hud-B-iron-ledger (v1: 90 words, 7 type sizes, lint: 17 edge hits)

1. Score strip is 805 x 130 px and reaches y 130: it hides the treeline across 42% of the width.
2. "first to 300" is light brown on vellum, 2.55:1.
3. Point labels sit on the strip's bottom border; the descenders of "Village" and "contested" touch it.
4. Point discs touch the first letter of their labels ("Old", "Shattered", "River").
5. The Dusk crescent is drawn as a "C" and reads as point C.
6. Timer is a dark box with its own font: three faces inside one strip.
7. Gold plate and queue notes 4 px (1x) from the top and left edges; queue text 2 px from its box.
8. Reinforcement shown twice: two queue notes top left and the toast at the bottom.
9. Panel header "SUPPORT ROUTS horn sounded, regroup 9 s" runs into the frame's corner; the horn icon overlaps the frame.
10. Blood-splat icon overlaps the "9" of "9 / 12".
11. "ATTACK - SHIELDWALL FIGHTING": order, formation and state spelled out in one line; red used for FIGHTING and for ROUTS.
12. ROUTED stamp runs over "FOLLOW - COMPACT" and into the card padding.
13. Group panel 4 px from the left edge and 5 px from the bottom (1x).
14. Toast seal hangs over the toast's left border; toast text 3 px from the box bottom.
15. "MARSHAL" is tan on tan, 2.03:1.
16. Ability label repeats the key strip ("H - RALLY HORN" and "H Rally Horn").
17. Key strip 5 px from the bottom edge, drawn over the vanilla hotbar (its outline shows underneath).
18. Key hints always on (H itself is the confirmed ability key; see the SYS-5 correction).
19. Minimap title "BATTLEFIELD" runs past its tab and is cut by the frame.
20. Minimap repeats the point states already in the strip; it sits 5 px from the right edge.
21. Point names do not match Rivergate (SYS-5).
22. Chat-bar leftovers behind the toast and the cards (SYS-3).
23. Colour fringes on every Lato run (SYS-1).
24. Seven type sizes, three faces (SYS-4).

## ui-A-war-table-recruit (v1: 274 words, 9 type sizes, lint: 7 edge hits)

1. House tabs run to 6 px (1x) from the top edge.
2. House glyphs jammed into each tab's top-right corner (SYS-2).
3. "the oath that holds the line" fills the Gilded tab with 2 px to spare.
4. "300 / gold / 14 Gilded shards": three sizes in one stack, crowded against the Tidecallers tab.
5. Muster roll opens with a heading and two dense lines (flavour, then every Bulwark number in red).
6. "Common - shield infantry" touches the Order Squire card's right border.
7. "Lord Marshal Aldric" runs 6 px (1x) past its card border.
8. Each card carries five lines (name, rarity, count, stars, footer): 30 lines in the grid.
9. One silhouette in six colours stands in for every troop; nothing is recognisable.
10. Stars are 3 px tan marks on cream, 2.42:1.
11. "x1 rank 2" and "Just ranked up: duplicate" repeat the reveal card.
12. Two focal points: the letter's wax seal and the reveal card compete, and the reveal is the smaller.
13. "DUPLICATE - RANK UP" ribbon hangs 5 px outside its panel.
14. Reveal portrait is cream on cream.
15. Reveal card text: flavour, Bulwark, counters and a hint in three sizes (six lines).
16. "Duplicates become rank stars... Enter: enlist - Esc: back" spans the letter panel with 3 px padding and repeats the footer keys.
17. Three dark buttons ("OPEN BANNER BUILDER B", "GRAVEBORN ROLL 2", "ALL HOUSES 1") compete with Enlist; key letters jammed into the labels.
18. Pool line wraps and leaves "letters." alone 9 px above the panel border.
19. Footer keycaps 2.5 px from the bottom edge; "Enter" overflows its keycap.
20. Data drift: Templar, Sunweaver, pool, +4%, evolve lines, mastery stat (SYS-5).
21. Colour fringes (SYS-1).
22. Nine type sizes (SYS-4).

## ui-B-field-ledger-recruit (v1: 268 words, 8 type sizes, lint: 6 crossed, 8 edge hits)

1. The Tidecallers tab runs under the Recruit panel.
2. "Order Longbow" crosses its card border; "Lord Marshal Aldric" spills out of the card.
3. "Common - shield infantry" touches the card border.
4. "Counters arrows. Weak to shield-/breakers." hyphen-wraps and sits 4 px from the card bottom.
5. Six cards of five or six lines: 34 lines of card text.
6. Header carries flavour ("Recruiter tents / half-ring below the ridge") and a currency row with 3 px icons.
7. "300", "14", "3", "Collection 11 / 30": 2x and 1x Minecraft sizes mixed in one row.
8. Bulwark ruler spells every tier across 950 px above a tick bar.
9. Recruit panel flavour ("One letter, one Gilded recruit. Dawn tent, east of the ridge.").
10. "Enter: recruit / Tab: skip" repeats the footer; sidebar "Esc Return to camp" repeats footer "Esc Camp".
11. The reveal card (meant to be the focal point) is a narrow column under the Recruit panel; the card grid is bigger and brighter.
12. Reveal portrait cream on cream; "Space: next letter" 2.72:1.
13. Pool chips wrong, "Epic+ guaranteed in 7" (SYS-5).
14. Footnote "Duplicates give a rank star..." floats over the world without a panel.
15. Footer keycaps 9 px from the bottom edge.
16. Body text switches between Minecraft and Lato inside one card.
17. "Room K7F2" and the troop data (SYS-5).
18. Colour fringes (SYS-1).
19. Eight type sizes (SYS-4).
20. Card name and rarity lines have 4 px side padding.

## ui-C-hall-of-banners-home (v1: 145 words, 5 type sizes, lint: 6 crossed, 4 edge hits)

1. The focal point is the player's banner; the fight choice (Practice, Skirmish, Campaign) is three small pennants at the bottom.
2. House tabs 9 px from the top edge; 1-5 numbers in tab corners; glyphs jammed top right (SYS-2).
3. Header row "300 gold - 14 - 3 - Founder, tier 3 - contracts 2": 3 px icons, the coin overlaps "300".
4. Instruction sentence as body text ("Walk under a House to recruit. Your banner hangs centre.").
5. "GILDED II soon: 2 more" and "RISE AGAIN I" chips: text touches the chip borders.
6. "Templar just ranked: 2 stars" repeats the recruit screen; "B banner builder" repeats the footer.
7. Invite banner: "F friends" repeats the footer; the dashed frame leaves a stray "..." below it.
8. Ansel's "READY" is teal on teal, 2.50:1; "Wildmoot 10" is unexplained.
9. Room banner "K7F2" (SYS-5).
10. "Normal bots" in the Field box repeats "2 bots, Normal" on the Dusk banner.
11. Dispatches: four lines ending 10 px (1x) from the right edge; "Founders tier 3: claim" repeats the header.
12. Footer key hints: "Camp" runs under the Practice pennant.
13. Skirmish swords clipped by the pennant edge; pennant glyphs off centre (SYS-2).
14. Mode labels 9 px from the bottom edge.
15. Background smear behind the Practice pennant (SYS-3).
16. Colour fringes (SYS-1).
17. Five type sizes (SYS-4).

## ui-C-hall-of-banners-recruit (v1: 276 words, 8 type sizes, lint: 4 crossed, 9 edge hits)

1. The Gilded banner holds 18 lines: flavour paragraph, Bulwark tiers, shards, pool, button, hint.
2. The banner's stripe texture runs through every line of its text.
3. Banner and tabs 9 px from the top edge.
4. Reveal portrait cream on cream; "+4% health, +4% damage, +6 shards" is wrong (SYS-5).
5. "Counters arrows and charges. Weak to shield-/breakers." hyphen-wraps.
6. "Space: next letter - Tab: skip" red on cream, and the footer repeats the keys.
7. "Opened this sitting: 3 / Archer, Squire, Templar" floats over the world.
8. Roll shields: "LORD MARSHAL" cut short with no full name anywhere; "not yet" 3.68:1.
9. Templar shield crams "Epic - shield - 2 slots / x1 rank 2" (wrong, and repeats the reveal).
10. Top-right "1-5: House / Esc: hall" repeats the footer.
11. "You field 8: Bulwark I. Two more for II." and "Bulwark I / ... / 2 more Gilded for Bulwark II": the same fact twice.
12. Instruction line "Click a shield to read it. Drag it onto your banner: B."
13. Footer keycaps 4 px from the bottom edge.
14. "Enter - ten letters: Shift+Enter" is ambiguous.
15. Pool and "Epic or better within 7 letters" (SYS-5).
16. Glyphs jammed in tab corners (SYS-2).
17. Colour fringes (SYS-1).
18. Eight type sizes (SYS-4).

## ui-A-war-table-home (v1: 286 words, 7 type sizes, lint: 3 covered, 27 crossed, 4 text overlaps, 4 edge hits)

1. Invalid UTF-8 in the source: the separators render as "?/" in the currency and chapter lines.
2. "Founder" under the title runs into the map frame.
3. Campaign string lines cross nine labels (Trade Crossing, Rivergate Valley, Hollowwood Reach, Kingsfield Crossroads, Campaign, the Ironhold subtitle...).
4. Tooltip wraps mid-phrase ("Normal bots - / 2 v 2", "B: / mode") and sits on the strings.
5. Tooltip keys "B: mode, N: bots" collide with B = Banner in the footer; N is not bound.
6. Mode token glyphs float off their bases (SYS-2); a string runs through "CAMPAIGN".
7. Point names (Trade Crossing, Hilltop Shrine) sit among the field names at the same weight.
8. The back letter's text is cut off under the front letter.
9. Orphan words: "invitation" (Dispatches), "fill" (Room), "gold" (Last battle).
10. The party is listed twice (Dispatches and Room).
11. Contracts invented (SYS-5); the seal tokens overlap each other.
12. Founders chapter line ends 5 px from the right edge (1x).
13. "Open the banner builder: B" repeats the footer.
14. Room code K7F2 (SYS-5).
15. Three competing focal points: the map, the right column and the Ride button.
16. Footer keycaps 7 px from the bottom edge; the title 11 px from the top.
17. Two fonts for numbers (2x Minecraft gold, Lato counts).
18. Colour fringes (SYS-1).
19. Seven type sizes (SYS-4).

## ui-B-field-ledger-home (v1: 322 words, 8 type sizes, lint: 4 crossed, 9 edge hits)

1. Each fight card has a three-line description: nine lines before any choice.
2. Skirmish chips wrap to two rows; Practice says "Easy bots", Skirmish "Normal".
3. Field names wrap to two lines; descriptions repeat the type ("river town, 3 points").
4. Domination / Normal / 2 v 2 printed three times (chips, Room panel, summary line).
5. Contracts invented (SYS-5); "CLAIM +12" squeezed into a progress row.
6. "CLAIM TIER 3" and "BANNER BUILDER B" buttons compete with the primary action.
7. Banner panel on the home screen: grid plus seven lines of traits and roles.
8. "2 more Gilded: Bulwark II" ends 3 px from the panel edge.
9. Party: the "Invite" chip touches its text and sits 2 px from the panel bottom.
10. Room: "Code copied to clipboard" 4 px from the panel bottom; code K7F2 (SYS-5).
11. Sidebar "Esc Return to camp" repeats the footer "Esc Camp".
12. Footer keycaps 9 px from the bottom edge.
13. Breadcrumb flavour in the header ("Banner camp / Rivergate ridge").
14. Body text alternates between Minecraft and Lato.
15. Colour fringes (SYS-1).
16. Eight type sizes (SYS-4).

## hud-A-marshals-glass (v1: 75 words, 2 type sizes, lint: 9 edge hits)

1. Compass text has no backing: "DAWN 148" and the point names are teal on sky, 1.58:1.
2. Point markers sit on the compass ticks; the B marker covers the N tick.
3. "contested" orange on sky, 1.21:1.
4. Gold and queue lines on sky and trees with no backing, 6 px from the edges.
5. Reinforcement shown twice (queue lines and toast).
6. Group rows sit on bare stone; the world lantern shows through the pips.
7. Group list 10 px from the left edge.
8. "Attack Shieldwall FIGHTING" spelled out; orange for the order word and the state.
9. The rout is shown only by red text and a dashed red box (colour alone).
10. "MARSHAL" orange on stone, 1.82:1; the ability block sits where vanilla hearts and food draw.
11. "H Rally Horn" printed beside the tile at all times (H is the confirmed key; the defect is the always-on hint, see SYS-5).
12. Key-hint row always on, 5 px from the bottom edge.
13. Toast floats on stone with no backing.
14. Point names wrong (SYS-5).
15. Background leftovers (SYS-3).

## hud-C-banner-line (v1: 76 words, 2 type sizes, lint: 10 edge hits)

1. Point labels float on sky and trees ("Old Village" teal on sky, 1.58:1).
2. "contested 55%" sits on the background strip leftover (SYS-3).
3. "to 300" printed twice.
4. Gold and queue lines on sky, 6 px from the edges.
5. The toast pill sits mid-screen over the horizon; reinforcement shown twice.
6. Standard poles run through the INFANTRY and RANGED labels.
7. Group labels on bare stone ("INFANTRY 9/12" 4.40:1, worse on the lighter bricks).
8. Formation boxes are 3 px dots.
9. A dashed ghost box under the Infantry flag has no meaning.
10. The fallen Support pole crosses the lantern and its own label: the rout reads as clutter.
11. "horn: the line breaks" red on stone, 1.02:1.
12. "MARSHAL" over a world lantern; the H hint always on (H is the confirmed key, see SYS-5).
13. Minimap ring 10 px from the right edge; repeats the strip's point states.
14. Key-hint row 5 px from the bottom, touching the minimap ring.
15. Point names wrong (SYS-5).

Total v1 defects: 24 + 22 + 20 + 17 + 18 + 19 + 16 + 15 + 15 = 166.

## Second pass (v2) results

Same method as above: every v2 PNG opened at full size and in 2x to 4x crops, and the lint
(`python render.py --all-v2`, results in `v2-check.json`) run on all nine. The lint reports 0 issues and
0 text runs under 4.5:1 on every v2 screen, and the crops showed no defect of the kinds listed above.

| Screen | Defects v1 to v2 | Words v1 to v2 | Type sizes v1 to v2 |
|---|---|---|---|
| hud-B-iron-ledger | 24 to 0 | 90 to 23 (26%) | 7 to 2 |
| ui-A-war-table-recruit | 22 to 0 | 274 to 40 (15%) | 9 to 3 |
| ui-B-field-ledger-recruit | 20 to 0 | 268 to 45 (17%) | 8 to 3 |
| ui-C-hall-of-banners-home | 17 to 0 | 145 to 33 (23%) | 5 to 3 |
| ui-C-hall-of-banners-recruit | 18 to 0 | 276 to 39 (14%) | 8 to 3 |
| ui-A-war-table-home | 19 to 0 | 286 to 36 (13%) | 7 to 3 |
| ui-B-field-ledger-home | 16 to 0 | 322 to 37 (11%) | 8 to 3 |
| hud-A-marshals-glass | 15 to 0 | 75 to 24 (32%) | 2 to 2 |
| hud-C-banner-line | 15 to 0 | 76 to 23 (30%) | 2 to 2 |
| **All nine** | **166 to 0** | **1812 to 300 (17%)** | menus 3, HUDs 2 |

How the systematic defects were closed:
- SYS-1: v2 renders use greyscale antialiasing; no colour fringe in any crop.
- SYS-2: v2 pages keep the positions their stylesheet gives; every glyph sits on its mark.
- SYS-3: v2 uses the cleaned backgrounds.
- SYS-4: menus use 16 (one focal name), 11 and 8; HUDs use 11 and 8.
- SYS-5: Templar Rare, Sunweaver Epic and not owned, Lord Marshal Aldric locked, odds one click away,
  "Epic within 28 letters", +3% for the second star, Mill Hamlet; no room code; contracts are a count badge.
- SYS-6: no key strip; keys sit inside the buttons they trigger (Esc, Enter, B). In battle H shows only
  with Alt or in the first minute, so no HUD mock shows it.

What changed on each screen:
- hud-B: one 24 px iron score bar with the seals hung below; cards carry name, state word, ink bar, two
  glyphs and count; loss = red frame, selection = brass frame, rout = stamp inside a darker card; gold,
  queue, minimap and key strip removed; one notice.
- ui-A recruit: roll (portrait, name, stars), then the letter as the focal point (portrait, name, rarity,
  the stamped second star, one effect line), then the purse (gold, Enlist with cost, Epic counter, Odds).
- ui-B recruit: brass spine for the sections, House tabs with colour underlines, six portrait cards,
  a cream reveal card with Enlist under it.
- ui-C home: beam pennants for the sections, party flags, three wall shields with Skirmish lit, the field
  banner with Change, one Ride button.
- ui-C recruit: House tabs on the beam, roster shields, the House banner unfurled around the recruit, purse.
- ui-A home: rail plaques, lead party tokens, three orders with the sealed Skirmish lifted, the inked field
  card, one Ride button.
- ui-B home: spine, three fight plates with Skirmish cream, field panel, party tokens, Ride.
- hud-A: one smoked-glass strip (the score race, then a bearing line carrying ring, hatched-square and
  diamond seals) and one glass band for the line (standard, name, one pip per troop with its health as
  height, count, glyphs, state). Rout = fallen standard and ROUTING; loss = red row and a white pip;
  cooldown = grey horn and seconds.
- hud-C: a rail with two great banners dyed toward 300 and the seals between; standards with one stripe
  per troop, width for health, the order as finial and the formation as base tile; one label band under
  them so no pole crosses a word. Rout = leaning pole, limp cloth and a sounding horn; cooldown = the
  commander's standard, its cloth dyed as the horn recharges.

Known limits, not counted as defects:
- Marshal's Glass is translucent by design; a world lantern shows faintly through the line band
  (every text run on it still measures 4.5:1 or better).
- Banner Line standards are drawn over the world, so world objects can pass behind them.
- The Alt state (key hints) and the select state (point names, order words) are described, not mocked.
