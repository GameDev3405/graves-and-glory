# Battle map redo and bot plans: design (2026-11-13)

Owner decisions: Kort, 2026-11-13 (`status/DECISIONS-2026-11-09.md`, "2026-11-13 In-game map redo"). They are settled; this spec only says how.

**Approved by Kort, 2026-11-13** (AskUserQuestion, "Yes, build it"), after the review page https://claude.ai/artifact/TifyovtbuX8f1MMvsegwbq with both looks rendered from New Kingsfield's real blocks by `tools/battlemap_preview.py`.

## 0. Revision 2 (Kort, 2026-11-13, after in-game screenshots; AskUserQuestion)

Kort: "i only want this map ... the other one doesnt look good"; the points and icons clutter, so the map cannot be read at a glance. Approved mockup: `work/map-declutter-mockup.png`. This replaces the conflicting parts of sections 2-4 and 6:
- **Real look only.** The parchment look, the toggle, the T key and the look option are removed.
- **Terrain toned down** (desaturated and darkened) so the markers stand out.
- **Points:** large discs filled with the owner's colour (neutral light grey) with a dark rim and a big letter. The capture ring is in the challenger's colour. The name sits under the disc as outlined text, with no box. The plan flag is a small pennant on the disc.
- **Troops:** a small square icon per group at its real position, showing the troop type glyph (shield for infantry, bow for ranged, and so on) and a count beside it. Own groups are filled; allies are outlined, with the same glyph. No text plaques and no leader lines. Selected groups get a light edge.
- **Allies:** icon only. The name, count and job show on hover.
- **Order lines** are drawn only for selected own groups and for whatever is hovered.
- Enemies (triangles), fights (crossed swords), pings and the "you" arrow stay, drawn in the same outlined style.

## 1. Goal

1. The player sees at a glance where the troops are, how the field is laid out and where the points of interest are.
2. The player gives allied bots a shared plan, so bots stop doing their own thing and losing games.

## 2. The screen

- **Full screen, the game keeps running** (`shouldPause` false, as now). The M key opens and closes it. Esc closes it.
- **Opens on the whole field**, fitted to the screen with a small margin. **Scroll zooms** about the cursor, from the fitted view up to 8 screen pixels per block. **Drag pans** with the left mouse button held past a 4 px threshold, or with the middle button. A click without a drag is still a click. The view stays clamped to the field. Home (or a "Fit" button) returns to the whole field.
- **Two looks, one toggle** (button "Parchment | Real" top right, key T). Parchment is the default. The choice is remembered in the client options.
- **Top bar:** mode, both team scores, the round clock, and in Domination the points each side holds.
- **Bottom bar:** the ping buttons (from `Spec.PINGS`, as now), the "Fit" button and a one-line legend using the map's own glyphs.
- **Hover tooltips** on every marker: point name, owner and capture state, plan and who set it; a group's type, count, health and order; an ally army's commander, bot or person, count and current job; a sighting's age.

## 3. The two looks (generated per map, no hand drawing)

Both looks come from one **field grid**: for every column of the playable field, the top visible block and its height.

- **Where the grid comes from.** The server scans the built field once after each map build. It has every chunk, so it works for old and engine maps alike, and it shows the real blocks. It sends the grid to each player in a match once, compressed, by a new packet `battlemap_grid_v1` (map id, build hash, width, depth, then deflated per-column bytes: a block-look index into a palette list sent with it, and a height). The client caches it by map id and build hash for the session. Size: Kingsfield is 541 by 381 columns, about 412 KB raw, a few tens of KB deflated.
- **Real view.** Each column takes its block's map colour, with a light north-west slope shade, so buildings and hills read. One texel per block.
- **Parchment.** Each top block is sorted into a few kinds by a table (`assets/gravesandglory/battlemap/kinds.json`): water, road, building, foliage, crops, rock and ground. The client draws, at 3 texels per block: an aged parchment base with paper grain and a dark edge; water as a pale wash with an ink shore; roads as light bands with thin ink edges; buildings as warm filled shapes with crisp ink outlines; trees as small inked blobs; crops as faint hatching; contour lines every 4 blocks; hachures on cliffs. Terrain stays low in contrast so markers sit cleanly on top. It is the War Table's parchment, dark wood and brass-frame direction.
- Both textures build off the render thread when the grid arrives, so opening the map costs nothing. Until the grid arrives, the map shows today's `Arena.mapColor` texture (old maps), or a plain parchment (engine maps).

## 4. What the map shows

Shapes carry the meaning; colour only adds the state (UI spec 6.1).

| Thing | Glyph |
|---|---|
| Capture point | Lettered disc in the owner's colour (neutral: parchment grey). A progress ring in the challenger's colour while being taken; a hatched ring while contested. Name under it. |
| Team score | In the top bar. In Domination, each held point shows a small "+" pip for its owner. |
| Own group | Filled plaque: type and count ("INF 10"), with a health bar under it. A dashed line to its order target, with the order's glyph at the end. Selected groups get a gold edge. |
| Ally army (bot or person) | Outlined plaque with the commander's name, "(bot)" for bots, and the army's count. A dotted line to where it is heading, ending in the job glyph if it follows a plan. |
| Enemy | Red triangle while seen. After that, a faded triangle that fades out over 20 s, as the sightings do now. Never shown unless seen. |
| Ping | Ring with its kind's label (as now), fading out. |
| Fight | Crossed swords where an own or allied group is in combat. |
| You | White arrow pointing the way you face. |
| Plan | A flag over the point: sword for Attack, shield for Defend, banner for Hold, with the setter's name in the tooltip. |

**Data the client lacks today.** Ally groups, their destinations, health and jobs are not sent. They come in a second new packet, `battlemap_v1`, sent once a second to each player in a match. It carries, for every allied army, the commander's name, a bot flag and a stable army id, and per group the centre, count, average health, order and target. It also carries the team's plans: per point the job, who set it and when, and the direct orders. Fights come from the existing in-combat flags. The `state_v10` packet is not changed.

## 5. Bot plans

### 5.1 Team plan per point

- Click a point: a small popup offers **Attack**, **Defend**, **Hold** and **Clear**.
  - **Attack:** take the point. Bots go there and fight for it. Once it is ours and not contested, the plan becomes Defend by itself.
  - **Defend:** guard the point. Bots stay near it and engage enemies that come within its radius plus 16 blocks.
  - **Hold:** stand on the point. Bots take the hold order there and keep their ground.
  - **Clear:** remove the plan; bots choose for themselves again.
- **Any teammate may plan. The latest order per point wins.** Plans last until cleared, replaced or the round ends.
- **How the bots split up.** A pure function `BattlePlan.assign` runs at each bot decision. It gives each planned point at least one ally bot army, newest plan first, nearest army first. The remaining armies go to Attack points first, then Defend, then Hold, nearest first. With more plans than bots, the newest plans are staffed first. Bots without a planned point choose as they do now.
- **Modes.** Point plans are offered where bots choose among capture points: Domination and Siege. In Convoy and Survival the mode already sets the bots' goal (`BattleMode.botGoal`). There, the point popup shows only "Ping here", and direct orders (5.2) still work.

### 5.2 Direct order to one ally bot army

Click an ally bot's plaque: a popup offers **Attack** (then click a point or a place), **Defend here**, **Hold here**, **Follow me** and **Clear**. A direct order beats the team plan for that army. It lasts until cleared, replaced or the round ends. Any teammate may give one; the latest wins. A human ally's plaque has no popup: people are not ordered.

### 5.3 Bots follow, but defend themselves

The plan is applied in `Campaign.botDecide` after the retreat check and before the point choice turns into an order. So a bot that is nearly dead still retreats (`retreatShare`), and a bot in contact still fights back (`inContact`, and troops engage by themselves in `Tactics`). Only the goal changes; pathing, movement and routing are the AI lead's code and do not change.

Agreed with the AI lead (2026-11-13), the hook keeps the bot behaviour around it:
- Retreat, no retreat while in contact, and self-defence keep priority.
- Fresh reinforcements still gather at the rally (`musterRefill` and `musterRelease`) before heading to the planned point.
- Bots without a plan still spread over different points (`pickPoint` claims). Several bots planned to one point is the player's call.
- A Defend or Hold plan replaces the "guard only under threat" rule for that point, and never clears other points' `botGuard` claims.
- When a player takes a bot's seat, that army's direct order drops.
- `BattlePlan.jobFor(squad)` is a plain query with no side effects, so the later human-bot brain can read player plans through it.
- Bot quality is measured before and after with `tools/bot_quality.py` (idle, Hold idle, trickle, objective share) on a 4-match Domination sim.

### 5.4 Outside the map

- **Markers in the world:** each planned point shows a floating flag with its job glyph and the point's letter above the point, drawn for the planning team only (an `AFTER_ENTITIES` world-render listener, as order outlines do). A direct order's target shows a smaller flag to the player who gave it.
- **A chat line from the bot:** when an army's job changes because of a plan or order, that bot says one short team line in the event feed, sounding like a normal player (Kort, 2026-11-11), such as "on it, taking B", "holding C", "defending A", "following you", "back to my own plan". At most one line per bot every 5 s; when several change at once, the lines are spread over 1 to 3 s.

## 6. Controls summary

| Input | Does |
|---|---|
| Scroll | Zoom about the cursor |
| Left drag, middle drag | Pan |
| Left click a point | Plan popup |
| Left click an ally bot plaque | Direct order popup |
| Left click an own group | Select it (Shift adds), as now |
| Right click | Move the selected own groups there (hold order), as now |
| Ping button, then left click | Ping, as now |
| T, or the toggle | Parchment or real view |
| Home, or Fit | Whole field |
| M, Esc | Close |

## 7. Network and server (new protocol, no save change)

- Client to server: `battleplan_v1` = (kind: point plan or direct order, point index or army id, job, optional x/z). The server checks: a seated player in a running round, the point index or ally bot army is valid and on the player's team, the job is known, and a cooldown of 0.5 s per player. A refusal reaches the player as a feed line.
- Server to client: `battlemap_grid_v1` once per match per player, and `battlemap_v1` once a second, as in sections 3 and 4.
- Plans live on the `Battle` and are cleared with it. Nothing is saved.

## 8. Code layout

- `BattlePlan` (new, pure, no Minecraft): plan table, `assign`, Attack-to-Defend switch, ack-line choice. Added to the fast `javac` set with a `BattlePlanCheck`.
- `BattleMapGrid` (new): server scan, encode and decode (the codec is pure and checked fast).
- `BattleMapArt` (new, client): builds the real and parchment textures from the grid.
- `BattleMapScreen` (new, client): replaces `MapScreen`, with view transforms (pure `MapView`: zoom, pan, clamp, screen-to-world; checked fast), drawing, popups and tooltips. `MapScreen` stays as a name the M key and checks use, or the checks move to the new class.
- `Campaign.botDecide`: one hook calling `BattlePlan`. Owned by the battle map lead; agreed with the AI lead, who owns routing.
- `PlanMarkers` (new, client): world flags.

## 9. Proof

- `tools/verify.py fast` green, including `BattlePlanCheck` (assignment, latest wins, Attack becoming Defend, direct order beats team plan, retreat beats both), `MapViewCheck` and the grid codec check.
- In-game checks: extend `p1-ui-map-glyphs` with the new legend and glyphs, plus new checks. `p1-ui-map-view`: opens fitted, zoom and drag, the toggle switches texture. `p1-ui-map-plan`: click a point, choose Attack; the plan shows on the map and as a world flag. Each check saves a screenshot. Run on an old map and on New Kingsfield.
- Bot proof from `BotSimulation`: with an Attack plan on one point, the allied bots' decisions (`decisions.csv`) choose that point within one decision cycle and the team takes it. A direct Hold order keeps one bot army on its spot, except while it retreats. Run at 1v1, 2v2 and 4v4.
- Screenshots of both looks in game for Kort.

## 10. My calls (small gaps ruled by the map lead)

1. The field grid comes from a server scan sent by packet. The client cannot scan the whole field itself, because chunks past view distance are not loaded.
2. New packets (`battlemap_grid_v1`, `battlemap_v1`, `battleplan_v1`) rather than changing `state_v10`. This is a protocol addition Kort's feature needs; there is no save change.
3. Attack turns into Defend once the point is ours, so bots do not wander off a point they were sent to take.
4. Defend means guarding within radius plus 16 blocks; Hold means standing on the point.
5. Plans and direct orders last until cleared, replaced or the round ends. They do not time out.
6. Human allies cannot be ordered; they see the plan flags and the map like everyone else.
7. In Convoy and Survival there are no point plans, because the mode sets the bots' goal; direct orders still work there.
8. Zoom runs from the fitted view up to 8 screen pixels per block; parchment is drawn at 3 texels per block.
9. Bot lines are rate-limited to one per bot every 5 s.
10. No corner minimap (Kort).
11. Deviation from 5.1, 5.2 and call 7 (found in build, 2026-11-13): Siege's `botTarget` and Convoy/Survival's `botGoal` override any plan. So the server refuses a direct order or point plan for a bot the mode aims ("Bots follow the mode's own plan here."), and the map offers no order popup for those bots. Direct orders work only for bots the mode leaves free.
12. Bot lines name the point ("on it, taking Trade Crossing") rather than its letter, and each bot is named "<tier> bot <n>" so lines from different bots can be told apart.
