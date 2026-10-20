# v2 grades (controller, 2026-10-17)

Rubric: work/ui-concepts/GRADING-RUBRIC.md. 1 point per line, half points allowed. Below 8 = rework.
Designer self-score was 9.0-9.5 on every screen.

| Screen | Score | Lost points |
|---|---|---|
| HUD A Marshal's Glass | 8.5 | Info budget 0.5 (unlabelled icon pairs per group, stray "N" on the point rail). Drama 0.5 (rout is a red word only; ability cooldown is a bare number, no sweep). Feel 0.5 (most generic dark overlay of the three). |
| HUD B Iron Ledger | 8.5 | Info budget 0.5 (same icon pairs). Drama 0.5 (cooldown bare number; contested point reads only as a square frame). Feel 0.5 (top bar lost the iron/vellum character; only the cards carry it). |
| HUD C Banner Line | 8.5 | Focal 0.5 (standards are tiny; the label band draws the eye instead). Spacing 0.5 (standards float at different heights and do not line up with the band). Drama 0.5 (fallen standard for rout is good; cooldown still a bare number). |
| UI A War Table home | 9 | Feel 0.5 (nav tabs float over a sky band that does not meet the table). Hierarchy 0.5 (black "Change" button breaks the parchment button family). |
| UI A War Table recruit | 9 | Spacing 0.5 (right column: envelope pile, gold, button, pity text, Odds are not on one grid). Legibility 0.5 (wax seal sits where the second star should be, so the tier reads as star-seal-plus). |
| UI B Field Ledger home | 8.5 | Spacing 0.5 (map card, party row and Enter button are three blocks that do not share a baseline). Feel 1.0 (flat dark glass over a dark screenshot; least like this game). |
| UI B Field Ledger recruit | 9 | Feel 1.0 (clean but generic gacha grid; gold total sits top-right, far from the Enlist cost). |
| UI C Hall of Banners home | 9 | Spacing 1.0 (pennant tops at three different heights; party tiles hang through the rail line; Enter button far below the pennants). |
| UI C Hall of Banners recruit | 9 | Spacing 1.0 (Enlist, gold and Odds float in the right third; name lengths push the star rows out of line). |

Direction averages: A 8.8, B 8.7, C 8.8.

Shared HUD notes (all three): vanilla hearts, hunger, XP and hotbar are still drawn under the custom HUD, so health shows twice; the notice pill sits in the middle of the battle view; no key hints anywhere for group orders.
Shared UI notes: every recruit screen shows the same Templar reveal; a locked/unknown state and a "not enough gold" state are not mocked.

# v3 grades (controller, 2026-10-18)

| Screen | Score | Lost points |
|---|---|---|
| HUD Iron Ledger v3 | 9 | Info budget 0.5 (order and formation words now always on every card, plus a key row: 36 words on screen in a fight). Spacing 0.5 (notice slip plus three cards make a tall stack that owns the lower-left quarter; the slip reads as a fourth card). Cooldown sweep in the horn slot is hard to see at this size. |
| War Table home v3 | 9.5 | Spacing 0.5 (content sits in one horizontal band with empty oak above and below; the practice and campaign ink drawings are faint next to the skirmish sheet). |
| War Table recruit v3 | 9.5 | Spacing 0.5 (right column: envelope pile is large and high, then pity text, Odds, gold and Enlist step down with uneven visual gaps; Enlist ends up in the far corner away from the letter). |

Everything else scores full: the iron-and-vellum material is now one object top to bottom, rout/loss/contested read without text, key hints appear on the selected card, hunger row gone, portrait frame is a window for a live render.

# v4 grades (controller, 2026-10-18)

| Screen | Score | Lost points |
|---|---|---|
| HUD Iron Ledger v4 | 9 | Feel 0.5 (the event feed is a plain dark translucent box with vanilla-chat styling; it needs the ledger material, an iron edge or a vellum strip, to belong to the same object as the cards). Info budget 0.5 (seven lines in the mock; five in play). |
| War Table home v4 | 9.5 | Focal 0.5 (the map corner under the rail is a large flat grey rectangle that reads as unfinished; make it a rolled map end or a smaller, warmer sheet). |
| War Table recruit v4 | 9.5 | Feel 0.5 (the 160 px live-model reveal is chunky next to the table; honest to the game, and the unit identity pass decides skin resolution). |

Detail ask met: oak grain and nails, torn parchment, relief seals, riveted iron, ink illustrations, full-density map. Subtitles in place. Portraits are live-model renders in the spec's crop.
v4.1 (2026-10-18): feed as vellum slip with iron edge, rolled map; controller grades 9.5 / 9.5 / 9.5. Sent to Kort for his grading.

# Title "The Hill" v2 grades (controller, 2026-10-18)

| Screen | Score | Lost points |
|---|---|---|
| Title day v2 | 9 | Hierarchy 0.5 (the dead tree and left tent cluster pull the eye off the commander; the sun flare on the right is stronger than the lockup). Feel 0.5 (the plateau grass meets the rendered battlefield in a hard pixel edge; a thin shadow or haze band would seat the cliff). |
| Title night v2 | 9.5 | Legibility 0.5 (battlefield troops dissolve into the dark; the lantern pools read, the armies barely do). |

Detail ask met: bedded rock strata, three tents with lit doorways, dead tree, worn path, half-sunk shield, broken spear, heather, hand-placed embers, banner with sun sigil. Menu column and version tag unchanged from v1 (fine). Both above 8; sent to Kort for his grades.

# Kort's grades and feedback (2026-10-18, decision board)

| Screen | Verdict |
|---|---|
| HUD Iron Ledger v4.1 | no objection (not graded in words) |
| War Table home v4.1 | rework: the map is too cluttered since the detail pass, "you can't tell what's what"; keep the detail, zoom it out |
| War Table recruit v4.1 | approved ("looks good") |
| Title The Hill v2 day + night | rework: the pixel art is not Minecraft; make it blocky, Steve at the top with a diamond sword |

# v4.2 War Table home map (controller, 2026-10-18)

| Screen | Score | Lost points |
|---|---|---|
| War Table home v4.2 | 9.5 | Colour 0.5 (the river is olive-grey and reads as a second road until the bridges give it away; a cooler grey within the sepia family would separate it). |

Rework ask met: whole real Rivergate field zoomed out, forts, three lanes, river with plank bridge, stone bridge and ford, seals A/B/C, mill hamlet and shrine read first with open ground between; woods, hills and farm demoted to pale marks; compass and scale dropped. Only the map pixels differ from v4.1. Sent to Kort for his grade.

# Title "The Hill" v3, blocky (controller, 2026-10-18)

| Screen | Score | Lost points |
|---|---|---|
| Title day v3 | 8.5 | Feel 0.5 (the crag face under the menu is a flat dark slab with faint block edges; it reads as a darkened rectangle, not as stone courses). Hierarchy 0.5 (the panorama sun flare is still the brightest spot and sits off the lockup). Focal 0.5 (the two wool tents and the sign post are toy-sized next to the Steve commander; the plateau is a thin strip). |
| Title night v3 | 9 | Feel 0.5 (same crag slab under the menu). Focal 0.5 (same tents). |

Rework ask met: the hill is hand-placed blocks (grass, dirt, gravel path, stone/andesite/cobble courses, wool tents with lanterns, log dead oak), game-style lighting, and the commander is a Steve-proportioned player model in Dawn armour with a raised diamond sword and the banner. Sword and face read at a glance. Lint clean on all three pages. Both above 8; sent to Kort for his grade.

# Kort's grades, round 2 (2026-10-18, afternoon)

| Screen | Verdict |
|---|---|
| War Table home v4.2 | approved. Clarified: the enlarged sealed sheet is the focused one; hovering Practice shrinks Skirmish to normal (no seal) and enlarges Practice with the seal. Wants animation and effects so the menu feels alive. |
| Title The Hill v3 blocky | rejected ("really bad"). Wants: Steve on a hill facing the battle, overlooking it, sword in the air; one unified hill, no split between a close-up foreground and a far-away panorama. Before any long build: quick image demos to choose from, so the vision is understood first. |

## Reward track, round 1 (controller grade 8.5, fix round dispatched)

- Strong: War Table family, live portrait in the frame, wax stamps and iron hasps read at a glance.
- Under 9: tier shown twice (seal and pin); plaques touch (no 8 px gap); the Oath row leaves the bottom right empty; Oath rewards duplicated mastery 4.7.
- Rulings: hero shows the next big reward and the track slides one plaque after a claim; Oath row rewards are House-wide and the player's own (commander livery, pennants, banner cloth, portrait frame, double shards, the Oathsworn title), distinct from mastery; late swearing unlocks Oath tiers at or below the current tier; shards go to the sworn House, unsworn players choose at claim; the Rare is picked at claim with Change as preview; the Founders Chevron is brass and ivory, not a team colour.

## Reward track, round 2 (controller grade 9, sent to Kort)

- Fixed: numberless pin, 8 px plaque gaps, full Oath row, House-and-player Oath rewards, props at 2.5x with candle light, own wood seed and fittings.
- Open for Kort: Oath item names (Fess, Chief, Bordure, Saltire, Quarterly, Gyronny, Pily; Crowned field, Night cloth) or names from the Houses spec; prop size on this page (they sit close to the track).

## Reward track, approved (Kort, 2026-10-18 late)

- Kort: approved at the controller's 9; the candle and ink pot come off this page (kept at 2.5x for other pages); the artist's Oath item names stay.
- Re-rendered without props: track-01-sworn, track-02-unsworn, track-03-claim, lint clean. The track stays at 136 to 888 (the props sat outside the content area, so nothing widens). Into the UI plan as Task 7's Chapter page.

## Storybook, round 1 (controller grades 01 9.5, 02 8.5, 03 9.5, 04 9, 05 8.5; fix round dispatched)

- Strong: the closed book on its own stretch of table with the 2.5x candle and ink; the spread's leaf layout (running head, drop capital, wants, scene plates in three states); the Fall panel with its vellum caption strip; the march card over the held frame.
- Under 9: 02 and 05 show four companions on page 2 where the copy has Maud alone (honesty). Fix: Maud alone, empty rows not drawn, four-row layout kept in NOTES for page 7 onward.
- Rulings: night under "Day three" stays; the 60 px portrait equals the spec's 32 px texture at scale 2; red initial kept (rubrication); 05 stays over the world; turning back only; all labels and "Sealed until the battle is won." approved; the renderer is the target, stage mockups use harness screenshots of real maps from the next round; Ashka's portrait follows her Hide Hunter model.
- Queued for Kort: the Hollow's navy tabard and the seal mark are the artist's designs.

## Storybook, round 2 rebuild (controller, 2026-10-19)

- Contact sheet `work/ui-concepts/storybook/round2-sheet.png` (five pages, 960 wide). Every ruling is in: centred seal mark, 11-row mark inside the small seals, no brass on the wood, Maud alone on page 2, the battle plate keeps its map, every world shot a staged in-game still with the shader on and no packs.
- Grades: 01 closed 9.5 (the still book carries its drama in the seal and the ribbon alone); 02 spread 9.5 (the three plates now read as three different places; the leaf rules and Maud's row share an edge); 03 Fall panel 9.5 (the climber's silhouette against the moon over the Tide is the set's whole point and it lands); 04 North Crossing 9 (the dusk fire and the smoke are right; the placeholder Maud in Gilded kit beside a navy-tabard bust is the honesty gap the artist named, closed by the own characters); 05 march card 9.5 (lantern posts left, the column marching in right, the card in the centre third).
- Set passes at 9 or better. Sent to Kort as the one contact sheet. Honesty gaps are placeholder gaps: the final shoot waits on Phase 2 Task 5 (own characters) and Phase 3 (maps), as ruled on 2026-10-19.
- Rulings: Maud's in-game look is not a question: companions wear the company's navy tabard on the own rigs (unit identity rule 8; Phase 4 Task 19 the named cast), so 04 and page 2's sealed plate are reshot once the cast exists. The Hollow standard is a vanilla blue banner for now; the navy cloth with the hollow needs the mod's own banner art, queued on Phase 4 Task 19 as a prop.
