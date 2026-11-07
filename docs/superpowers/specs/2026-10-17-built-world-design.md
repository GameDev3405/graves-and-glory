> Historical (superseded 2026-11-07): see PLAN-TO-1.0.md / WORKFLOW.md

# Phase 3 design: the built world

Version 1, 2026-10-17. **Proposal for Kort's sign-off**; nothing is built.
Sources: `docs/VISION-AND-ROADMAP.md` sections 3, 9, 11, 12 and 13, and the
current code. Plan: `docs/superpowers/plans/2026-10-17-phase-3-built-world.md`.

## 1. Intent and why

The camp is a moss courtyard with four pavilions, and every battlefield
building is a box drawn by `Arena.structure`. Pillar 3 asks for authored
structures with a reason to exist. The Houses (Phase 2) need a camp where they
are seen, and Phase 4's modes need landmarks built for fights. Known road
blockers stall troops today, so they are fixed in the same pass, first.

## 2. Goals, non-goals, anti-goals

Goals:
1. A hand-built war camp: ridge, commander's tent and war table, House tents in
   a half-ring with banners and live troop displays, the Graveborn graveyard
   shrine, a training yard that is the tutorial, War Gates facing the map.
2. Every generic building except bases and bridges becomes an authored
   landmark with cover and sight lines for its fights.
3. No road blockers: 20/20 knights over all 15 crossings of the five maps, and
   zero blocked road cells.
4. Map switches stay inside the budget.

Non-goals: pixel UI icons and frames (separate UI effort); new maps; modes
(Phase 4); in-world banner cosmetics (parked; the command tent's standard pole
is the anchor); moving points, bases, rivers or the arena; authored terrain or
forests (procedural fill stays, as pillar 3 allows).

Anti-goals: a landmark on a through-road, a deck or a ring it does not enclose;
authored detail written as Java `box()` calls; lecterns in camp; camp mobs with
AI; worldgen structure registration; new dependencies.

## 3. Constraints

- Minecraft 1.20.1, Fabric Loader 0.16.14, Fabric API 0.92.2; no new
  dependencies.
- Pieces are vanilla structure files
  (`data/gravesandglory/structures/<group>/<name>.nbt`, DataVersion 3465),
  read as vanilla `StructureTemplate`s and placed by the mod's placer (vanilla
  `place`, rotated about the centre block). Structure void keeps the ground.
- The 700x500 arena, Y=64 and every point, base, bridge, river and road
  endpoint keep their coordinates; point rings stay flat to radius+2.
- Map switch budget: one landmark per tick after the column build, each at
  most 100 ms, at most 1.5 s per map. Camp pieces at most 1.5 s per start;
  camp terrain at most 6 s, only when its sheets change.
- Placement numbers live in sheets; the harness stays in `test-tools/`.

## 4. The camp

Origin O = (0, 64, -310); dx east, dz south toward the map. The bowl is flat
at y 64 south of dz -38; a bank climbs to the ridge plateau (y 76) by dz -51;
a rim and mountains close the other sides. The terrain island (dx -96..96,
dz -108..40) stops 3 blocks short of the arena.

    dz -60            [COMMAND TENT + war table]
    dz -51  ===================== ridge edge =====================
    dz -44       [stair W, dx -18]            [stair E, dx 18]
    dz -39                 [GRAVEYARD SHRINE]
    dz -24         [WILDMOOT]                [IRONCLAD]
    dz  -6   [GILDED]         parade standard        [TIDECALLERS]
    dz   6  [TRAINING YARD, dx -46]  parade  [ARCHIVE, dx 46]
    dz  18                     spawn plinth
    dz  26       [west pad, dx -11..-8]  arch  [east pad, dx 8..11]
    dz  30  ============ WAR GATES, dx -18..18 ===================
    dz  35  apron; the arena lies south

- Spawn faces north at dz 18.5, toward the House ring and ridge; a commander
  who has not cleared battle 1 arrives at the training yard.
- Command tent: the Quartermaster and a 15x9 carpet war table of the current
  map, a candle per point.
- House tents face the parade with banner, palette and six live troop
  displays; the Graveborn recruit at the shrine. A tent or troop opens that
  House's recruit page.
- Training yard: four lesson boards naming the player's keys, six dummies, and
  the Drillmaster (battle 1 or practice).
- Archive: Archivist, Founders' Stone. War Gates: arch, both ready pads,
  Steward.

## 5. Landmarks

A shared kit replaces the hamlet and plain buildings on every map: two houses,
market, farm, customs, warehouse, shrine and four watchtowers. Signature
pieces carry the fights; each has a cover line (must be blocked) and a sight
line (must be clear), checked by raycast.

| Map | Landmark | The fight it serves |
|---|---|---|
| Rivergate | mill at the north crossing | Loft archers see the bridge deck; the yard wall covers defenders from the west approach. |
| Kingsfield | outposts enclosing B and D, plus the mill | Each flank point becomes a stockade with two gates and three lookouts; a lookout sees the outer road. The mill repeats its Rivergate job. |
| Ironhold | gate (replaces the breach) | An 11-wide passage between towers; a tower top sees the west field. `ironhold/gate_intact` is reserved for Siege. |
| Ironhold | keep (replaces the ruin) | Straddles the river south of B; its north wall covers defenders, the wall-walk sees B. |
| Ironhold | curtains, supply barn, garrison | Curtains keep the 7-wide sally gaps; the barn takes in the hay line. |
| Frostmarch | lodge, refuge, signal tower | The lodge yard wall covers the north spur road, its window sees it; the tower's fire is in the piece. |
| Hollowwood | chapel, sawmill, logging camp, moon shrine | The ruined chapel covers and sees the south lane; the logging camp holds the logs that blocked the road. |

## 6. Road-blocker fixes

1. Bridges: wood posts and railings, and stone walls and lanterns, move to the
   rows beside the deck; railings stand only over water.
2. `Arena.clearDecks` empties every deck cell above the deck after the
   structures pass (this removes the Ironhold ruin wall).
3. The Ironhold breach gap widens from 7 to 11; the gate piece then replaces it.
4. The Hollowwood log pile goes; its logs return inside the logging camp.
5. Hamlet houses, the market, the Ironhold garrison, the Frostmarch signal
   tower and the Kingsfield customs and warehouse move off the approach roads.
6. The Ironhold hay line moves into the supply barn.
7. A new layout marker rebuilds existing maps; routes now cover Kingsfield.

The bases' barracks and stores still clip their own road (Question 6).

## 7. Authoring, versioning, verification

Authoring options:
- Hand-built with a structure block: the best feel, but every change needs a
  person and leaves no readable diff.
- Java builders, as today: reviewable, but this is the generated look pillar 3
  retires.
- **Chosen: command scripts exported to structure files.** Each piece is a
  `.mcfunction` of `fill` and `setblock` lines in the harness. A harness-only
  StructureStudio builds it in a test world, photographs it and saves it with
  vanilla `saveFromWorld`. Agents write and diff scripts; the game ships plain
  structure files; a verify mode rebuilds every script and compares NBT, so
  files never drift from scripts. Kort can still edit any piece in a structure
  block; `/studio dump` writes the script back.

Versioning: script and `.nbt` are committed together. Worlds record layout
`campaign-3` plus a hash of the map's landmark rows and files, so a changed
piece rebuilds that map on next load. The camp records a hash of its sheets;
its entity tag becomes `gravesandglory-camp-v4`. A piece whose footprint
changes gets a new name.

Verification:
- Fixed-camera screenshots of every piece in the studio, every signature
  landmark from ground and above, and named camp views.
- Route harness 20/20 on all 15 crossings after every landmark task; the walk
  scan stays at zero blocked road cells.
- No new blockers: nothing solid on a through-road, deck or unenclosed ring,
  and every cover and sight line holds.
- Kort's gate: contact sheets of the camp and each map, judged for tone.

## 8. Builder's calls

- Piece dimensions, palettes and exact camp layout (vision section 13).
- One kit for all maps, so effort goes into signature pieces.
- Moves of at most 20 blocks off the approach roads.
- Displays are real troop mobs with no AI, roofed, named like battle troops so
  skins match.
- Carpets on the war table, not a map item.
- Procedural camp terrain; only the pieces are authored.
- Pixel UI hook: labels stay text until the UI pass gives `CampDisplays` the
  House glyphs, with no world change.

## 9. Questions for Kort

1. Graveborn recruit at the graveyard shrine, not a fifth tent?
   **Recommended: yes**; the shrine reads as their House.
2. All six troops of each House as live displays, showing your own LOCKED or
   stars? **Recommended: yes.**
3. The training yard teaches with lesson boards and a START button for battle
   1, not an in-camp drill? **Recommended: yes**; battle 1 teaches live.
4. Replace every generic building except bases and bridges, with a kit plus
   signature pieces? **Recommended: yes.**
5. Existing worlds rebuild the camp and maps by themselves after the update?
   **Recommended: yes**; profiles are untouched.
6. Leave the bases' barracks and stores road clips for Phase 4?
   **Recommended: yes**, recorded in `docs/PARKED.md`.
