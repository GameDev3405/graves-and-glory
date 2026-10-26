# Phase 3 Task 4: the House ring

Status: **DONE_WITH_CONCERNS.** The ring is built, checked and committed. The House displays, labels and display survival need Phase 2 code that is not on this branch. By my own grade the four tents score 8, under the 9 the rubric asks for.

## Commit

`46bae7a` on `phase-3-built-world`, on top of `9532931`. It was committed by path with the message file `work/p3/commit-t4.txt`. Nothing was pushed.

The commit holds:
- `sheets/design.json`, `sheets/preflight.json` and `Spec.java` (generated)
- `CampWorld.java`, `CampScreen.java`, `CampTerrain.java`
- `Phase3Checks.java`, `CampTerrainCheck.java`
- five `.mcfunction` scripts and five `.nbt` files under `camp/`

The generator `work/p3/housegen.py` is git-ignored, like `campgen.py`.

## What was built

**Four tents**, each 13x12x11 and authored facing south:
- **Body:** a closed canvas body on a timber frame.
- **Porch:** a porch under an awning, and a plinth at local y 1 along z 9 with six display slots at x 1..11, step 2.
- **Banners:** the House wall banner hangs on the gable at (6,9,9). The standing banner sits on a dark oak pole at (0,7,10).
- **Gilded Order:** soiled linen with a dull gilt (yellow terracotta) hem. Birch frame, sandstone dais, gold finial and plinth ends. Bell, smithing table and map table.
- **Wildmoot:** stitched hides (brown wool, brown terracotta, packed mud). Spruce poles, bone tusks over the gable, a bone totem with a skull, hay and moss.
- **Ironclad:** black and grey canvas, dark oak and iron. A smoking forge chimney of cobbled deepslate, blast furnace (unlit), anvil, a fenced fire, and iron-bar windows.
- **Tidecallers:** sea-faded canvas (cyan terracotta, salt-grey patches, cyan stripes). Dark prismarine hem, driftwood deck, a mast with a furled sail, and an enclosed tide pool with sea pickles. Sea lanterns sit in the awning.

**The Graveborn graveyard**, 23x16x23, in the notch between the ridge stairs:
- **Court:** a court behind a blackstone colonnade, which shades the six slots at z 19. The colonnade's back is iron grilles between piers, with an open arch at x 10..12.
- **Terraces:** three terraces of graves (headstones, mounds and candles) climb to a blackstone shrine with an altar, skull and soul lanterns.
- **Side walls:** they are cut to the bank beside them. The back corners mound up to the plateau.
- **Dressing:** two dead dark oak trees, ferns, dead bush and moss.
- **Paths:** the central path and the cross way at z 15..17 stay open, so campwalk legs 4a2 and 4a3 still walk through.

**Code:**
- `CampWorld.slot(row,k)` and `CampWorld.yaw(rotation)` are added.
- `CampScreen.open("recruit:<house>")` now keeps the House in `CampScreen.house`.
- The recruiter campStations row is gone. Tents open recruitment through `CampWorld.station` on their footprint.

## Deviations and rulings

1. **The display, survival and label checks are not in this task.** `p3-houses-displays`, `-survive` and `-labels` need ArmyMobs, the houses sheet and CampDisplays from phase-2-houses. The display-mob half of `-opens` waits for the same merge. Ruling: build what stands alone and report these as pending; nothing was merged.
2. **I added one check, `p3-houses-slots`.** It checks:
   - slot 0 is the plan's first slot;
   - slot 1 lies two blocks on, in the direction the plan gives;
   - all 30 slots stand on solid ground, with air at feet and head.
3. **The house pictures do not all follow the plan's camera rule.** The plan puts the camera max(w,d)+8 from the piece toward (0.5,y,-315.5). For the four tents that spot lies in the smoke of the parade's two lit braziers (Task 3, world -4.5/5.5, -315.5), and the frames came out grey. When the spot is within 4 blocks of a brazier, the camera now swings 30 degrees north. The graveyard camera follows the plan exactly.
4. **Campwalk lane `4a2-recruiter` is now `4a2-graveyard`.**
   - Its pictures are `graveyard-gate` and `graveyard-court`.
   - The `label:recruiter` probe was dropped, because the recruiter is gone and the House labels come with Phase 2.
   - The campwalk now has 123 checks instead of 124. The only differences are those ids.
5. **`CampTerrain.REVISION` is now terrain-6.** Deleting the recruiter row changes `clearOfFoot`, so the bank foot moves at |dx| < 7. All of that lies inside the graveyard footprint and its apron. No layout string was bumped.
6. **The `CampTerrainCheck` recruiter-floor guard (dx -3..4, dz -37..-30) is replaced by a graveyard guard.** The new guard checks three things:
   - the graveborn row is in the sheet;
   - its footprint lies at the floor;
   - the bank at dx ±12, dz -50..-43 matches the heights the graveyard's side walls were cut to (within 1).
   It was red first (1 of 31 failing, 243 columns off) and green after the rows went in (31 of 31).
7. **Two changes came from my own look at the pictures:**
   - The colonnade's back wall became grilles between piers, so the graves show from the camp. The colonnade roof also lost its slab ridge.
   - The tent palettes were muted over two rounds: no white or yellow wool glare, no toy cyan and blue stripes, no prismarine glare.
8. **Harness changes:**
   - `launch-t4.sh` and `launch-cw-t4.sh` are copies of the launchers with a 20-minute gate wait.
   - `run-t4.sh` and `run-cw-t4.sh` add a watchdog that kills only my own root's java tree.
   - The camp-checks client hung after `final-suite`. I killed only `b-earlier-064025-t4cc`, which is why that run shows `gradle exit 1`.

## Verification

| Check | Command / root | Result |
|---|---|---|
| Compile | `work/p3/compile-b.sh t4e` | exit 0 |
| Build | `gradle build --offline -x test` | exit 0 |
| Preflight | `python tools/preflight.py` | Preflight clean, 1581 cells |
| CampTerrainCheck | javac Spec+CampTerrain+check | red 1/31 before the rows, PASS 31 of 31 after |
| Other standalone checks | Admission, BattleMath, CampRules, Compatibility, LayoutMarker, LegacyDataMigration; 2 py tests | 746 / 41 / 41 / pass / 17 / 13 passed; py OK |
| Studio export | `b-studio-061108-t4exp3` | p3-export and p3-dump pass for all 5, studio suite pass |
| Houses, red | `b-studio-055518-t4red` (before the rows) | 6 houses checks fail, "0 rows with a recruit:<house> station" |
| studio,houses,camp (verify, box dump) | `b-studio-061253-t4v2` | 46 pass, 0 fail. Fresh exports and dumps pass for all 5. Houses 7/7. Camp suite 23. |
| boxdiff | `boxdiff.py boxdump/strict-before boxdump/t4b` | 0 differing cells on maps 0 to 4 (the `t4` dump also 0) |
| p3-camp-rim-seam | same run | unread 0, ok on all six seams |
| Houses (final cameras) | `b-houses-062321-t4v5` | 7 pass |
| Arena | `b-arena-...-t4` (`q-t4-arena.out`) | 51 pass |
| Routes | `q-t4-routes.out` | 49 pass |
| Camp checks | `b-earlier-064025-t4cc` (`q-t4-campchecks.out`) | 33 pass, including station-network-route via `where("recruit")` |
| Campwalk, shaders on (Iris, Sodium, Complementary) | `walk-camp-065054-t4cw`, all legs | 123 pass, 0 fail: floating, lantern, entities, labels, leg-4a2-graveyard |
| graphify update | `graphify update .` | 2033 nodes, 5945 edges |

## Self-grade (10 rubric lines, 0 to 10)

The ten lines are:
1. Readability
2. Silhouette
3. Detail density
4. Purpose
5. No clipping or floating
6. No seams with ground
7. No stale or foreign blocks
8. Rails and posts
9. Walkability
10. Feel

| Piece | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | Grade |
|---|---|---|---|---|---|---|---|---|---|---|---|
| tent_gilded | 9 | 7 | 8 | 9 | 9 | 9 | 9 | 9 | 9 | 7 | 8 |
| tent_wildmoot | 9 | 8 | 8 | 9 | 9 | 9 | 9 | 9 | 9 | 8 | 8 |
| tent_ironclad | 9 | 8 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 8.5 |
| tent_tidecallers | 9 | 8 | 8 | 9 | 9 | 9 | 9 | 9 | 9 | 8 | 8 |
| graveyard | 9 | 9 | 9 | 8 | 9 | 9 | 9 | 9 | 9 | 9 | 9 |

**What holds the tents back:**
- All four share one skeleton: a box body, a 45-degree roof of full wool steps, and a flat awning. Only the dressing tells them apart: the tusks, the chimney, the mast.
- The Gilded tent is still the cleanest and least grim piece in the camp.

Lifting them to 9 needs a different roof form per House, which a redesign of the shared skeleton would allow. The 12-block height in the sheet limits spires.

**The graveyard** is strongly grim and Souls-like with shaders (`graveyard-court`), and the north bank now reads as a terraced hillside with a shrine. From the parade, though, its front reads as a mausoleum: the graves show only through the grilles and from the plateau or the air.

## Contact sheet

`C:\Users\kortt\Melty\bannerbound-p3\work\p3\task4-contact.png` (960x540). The tiles, row by row:
- camp-ring, camp-house-graveborn, fly-aerial-front (shaders)
- camp-house-gilded, -wildmoot, -ironclad
- camp-house-tidecallers, graveyard-court (shaders), graveyard-gate (shaders)

## Needs an independent grade

- **The four tents**, which I graded under 9: is the shared stepped-wool skeleton acceptable, or does each House need its own roof form?
- **The graveyard's front seen from the parade**: does it read as a graveyard, or should the colonnade be opened further?
- **The swung camera** for the tent pictures: accept it, or move or unlight the parade braziers instead?

## Still needs an in-game check

- After the Phase 2 merge: `p3-houses-displays`, `-survive` and `-labels`; the display-mob half of `-opens`; the recruit page's House tab reading `CampScreen.house`.
- Walking into each tent and the shrine at night, and the Wildmoot tusks and the Ironclad chimney smoke close up.
